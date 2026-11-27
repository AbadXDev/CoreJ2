#include "EmbeddedJVMNative.h"

#include <jni.h>

#include <dlfcn.h>
#include <errno.h>
#include <os/log.h>
#include <pthread.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>

static pthread_mutex_t g_lock = PTHREAD_MUTEX_INITIALIZER;
static JavaVM *g_vm = NULL;
static int g_created = 0;

enum {
    COREJ2_DESTROY_TIMEOUT_SEC = 3,
    COREJ2_DESTROY_TIMED_OUT = -301,
    COREJ2_JVM_UNAVAILABLE = -8
};

/* JNI_CreateJavaVM is resolved at runtime: on device builds libjvm.a is
 * force-loaded into the app image; on simulator builds (no libjvm) the
 * symbol is absent and create fails with COREJ2_JVM_UNAVAILABLE instead of
 * failing to link. */
typedef jint (*CoreJ2CreateJavaVMFn)(JavaVM **, void **, void *);

static CoreJ2CreateJavaVMFn corej2_resolve_create_jvm(void) {
    return (CoreJ2CreateJavaVMFn)dlsym(RTLD_DEFAULT, "JNI_CreateJavaVM");
}

static void corej2_clear_exception(JNIEnv *env) {
    if (env == NULL) {
        return;
    }
    if ((*env)->ExceptionCheck(env)) {
        (*env)->ExceptionDescribe(env);
        (*env)->ExceptionClear(env);
    }
}

int corej2_embedded_jvm_is_created(void) {
    int value;
    pthread_mutex_lock(&g_lock);
    value = g_created;
    pthread_mutex_unlock(&g_lock);
    return value;
}

void *corej2_embedded_jvm_javavm(void) {
    void *vm;
    pthread_mutex_lock(&g_lock);
    vm = (void *)g_vm;
    pthread_mutex_unlock(&g_lock);
    return vm;
}

int corej2_embedded_jvm_create(const char *java_home, const char *classpath) {
    if (java_home == NULL || classpath == NULL) {
        return -100;
    }

    pthread_mutex_lock(&g_lock);
    if (g_created || g_vm != NULL) {
        pthread_mutex_unlock(&g_lock);
        return -101; /* second JVM rejected */
    }

    char classpath_opt[4096];
    char java_home_opt[4096];
    int n = snprintf(classpath_opt, sizeof(classpath_opt), "-Djava.class.path=%s", classpath);
    if (n <= 0 || (size_t)n >= sizeof(classpath_opt)) {
        pthread_mutex_unlock(&g_lock);
        return -102;
    }
    n = snprintf(java_home_opt, sizeof(java_home_opt), "-Djava.home=%s", java_home);
    if (n <= 0 || (size_t)n >= sizeof(java_home_opt)) {
        pthread_mutex_unlock(&g_lock);
        return -103;
    }

    /* Minimal options: previous SIGKILL (CODESIGNING invalid-page, PC=0) happened
     * inside Arguments::parse — strip optional -XX flags until bring-up is stable.
     * -Xverify:none: Zero static link can miss libverify until force_load; skip
     * bytecode verification so MIDlet Class.getConstructor does not throw
     * VerifyError "Could not link verifier". */
    JavaVMOption options[5];
    memset(options, 0, sizeof(options));
    options[0].optionString = classpath_opt;
    options[1].optionString = java_home_opt;
    options[2].optionString = "-Xmx32m";
    options[3].optionString = "-Djava.awt.headless=true";
    options[4].optionString = "-Xverify:none";

    JavaVMInitArgs args;
    memset(&args, 0, sizeof(args));
    args.version = JNI_VERSION_10;
    args.nOptions = 5;
    args.options = options;
    args.ignoreUnrecognized = JNI_TRUE;

    CoreJ2CreateJavaVMFn create_jvm = corej2_resolve_create_jvm();
    if (create_jvm == NULL) {
        os_log_error(OS_LOG_DEFAULT,
                     "corej2_embedded_jvm_create: JNI_CreateJavaVM not found (dlsym)");
        pthread_mutex_unlock(&g_lock);
        return COREJ2_JVM_UNAVAILABLE;
    }

    os_log(OS_LOG_DEFAULT,
           "corej2_embedded_jvm_create: java.home=%{public}s classpath=%{public}s",
           java_home,
           classpath);
    fprintf(stderr,
            "corej2_embedded_jvm_create: java.home=%s classpath=%s\n",
            java_home,
            classpath);
    fflush(stderr);

    /* Preflight lib/modules. java.home is forced to $HOME/Documents; both
     * lookup_vm_options ($JAVA_HOME/lib/modules) and the legacy nested path
     * ($JAVA_HOME/Documents/lib/modules) may be consulted. */
    {
        const char *home_env = getenv("HOME");
        const char *candidates[2];
        char path_a[4096];
        char path_b[4096];
        int i;
        path_a[0] = '\0';
        path_b[0] = '\0';
        if (home_env != NULL && home_env[0] != '\0') {
            (void)snprintf(path_a, sizeof(path_a), "%s/Documents/lib/modules", home_env);
            (void)snprintf(path_b, sizeof(path_b),
                           "%s/Documents/Documents/lib/modules", home_env);
        } else {
            (void)snprintf(path_a, sizeof(path_a), "%s/lib/modules", java_home);
        }
        candidates[0] = path_a;
        candidates[1] = path_b;
        for (i = 0; i < 2; i++) {
            FILE *mf;
            if (candidates[i] == NULL || candidates[i][0] == '\0') {
                continue;
            }
            mf = fopen(candidates[i], "rb");
            if (mf == NULL) {
                fprintf(stderr,
                        "corej2_embedded_jvm_create: cannot open %s errno=%d\n",
                        candidates[i],
                        errno);
                fflush(stderr);
            } else {
                unsigned char magic[4] = {0};
                size_t nread = fread(magic, 1, 4, mf);
                fclose(mf);
                fprintf(stderr,
                        "corej2_embedded_jvm_create: modules magic=%02x%02x%02x%02x nread=%zu path=%s\n",
                        magic[0],
                        magic[1],
                        magic[2],
                        magic[3],
                        nread,
                        candidates[i]);
                fflush(stderr);
            }
        }
        setenv("JAVA_HOME", java_home, 1);
    }

    JNIEnv *env = NULL;
    jint rc = create_jvm(&g_vm, (void **)&env, &args);
    if (rc != JNI_OK || g_vm == NULL) {
        os_log_error(OS_LOG_DEFAULT,
                     "corej2_embedded_jvm_create: JNI_CreateJavaVM failed rc=%d",
                     (int)rc);
        fprintf(stderr, "corej2_embedded_jvm_create: FAILED rc=%d\n", (int)rc);
        fflush(stderr);
        g_vm = NULL;
        g_created = 0;
        pthread_mutex_unlock(&g_lock);
        return (int)rc;
    }

    os_log(OS_LOG_DEFAULT, "corej2_embedded_jvm_create: OK");
    fprintf(stderr, "corej2_embedded_jvm_create: OK\n");
    fflush(stderr);
    g_created = 1;
    pthread_mutex_unlock(&g_lock);
    return 0;
}

int corej2_embedded_jvm_run_hello_world(const char *main_class_jni) {
    if (main_class_jni == NULL) {
        return -200;
    }

    pthread_mutex_lock(&g_lock);
    if (!g_created || g_vm == NULL) {
        pthread_mutex_unlock(&g_lock);
        return -201;
    }
    JavaVM *vm = g_vm;
    pthread_mutex_unlock(&g_lock);

    JNIEnv *env = NULL;
    jint attach = (*vm)->GetEnv(vm, (void **)&env, JNI_VERSION_10);
    if (attach == JNI_EDETACHED) {
        attach = (*vm)->AttachCurrentThread(vm, (void **)&env, NULL);
    }
    if (attach != JNI_OK || env == NULL) {
        return -202;
    }

    jclass cls = (*env)->FindClass(env, main_class_jni);
    if (cls == NULL) {
        corej2_clear_exception(env);
        return -203;
    }

    jmethodID main_mid = (*env)->GetStaticMethodID(env, cls, "main", "([Ljava/lang/String;)V");
    if (main_mid == NULL) {
        corej2_clear_exception(env);
        return -204;
    }

    jclass string_cls = (*env)->FindClass(env, "java/lang/String");
    if (string_cls == NULL) {
        corej2_clear_exception(env);
        return -205;
    }
    jobjectArray empty_args = (*env)->NewObjectArray(env, 0, string_cls, NULL);
    if (empty_args == NULL) {
        corej2_clear_exception(env);
        return -206;
    }

    (*env)->CallStaticVoidMethod(env, cls, main_mid, empty_args);
    if ((*env)->ExceptionCheck(env)) {
        corej2_clear_exception(env);
        return -207;
    }

    jfieldID completed_fid = (*env)->GetStaticFieldID(env, cls, "completed", "Z");
    if (completed_fid == NULL) {
        corej2_clear_exception(env);
        return -208;
    }
    jboolean completed = (*env)->GetStaticBooleanField(env, cls, completed_fid);
    if (!completed) {
        return -209;
    }

    return 0;
}

typedef struct {
    JavaVM *vm;
    jint rc;
    int finished;
    pthread_mutex_t mu;
    pthread_cond_t cv;
} CoreJ2DestroyJob;

static void *corej2_destroy_thread_main(void *arg) {
    CoreJ2DestroyJob *job = (CoreJ2DestroyJob *)arg;
    jint rc = (*job->vm)->DestroyJavaVM(job->vm);
    pthread_mutex_lock(&job->mu);
    job->rc = rc;
    job->finished = 1;
    pthread_cond_signal(&job->cv);
    pthread_mutex_unlock(&job->mu);
    return NULL;
}

int corej2_embedded_jvm_destroy(void) {
    pthread_mutex_lock(&g_lock);
    if (!g_created || g_vm == NULL) {
        g_created = 0;
        g_vm = NULL;
        pthread_mutex_unlock(&g_lock);
        return 0;
    }
    JavaVM *vm = g_vm;
    g_vm = NULL;
    g_created = 0;
    pthread_mutex_unlock(&g_lock);

    /* Release the creating thread's JNI attachment before destroy when possible. */
    JNIEnv *env = NULL;
    if ((*vm)->GetEnv(vm, (void **)&env, JNI_VERSION_10) == JNI_OK && env != NULL) {
        (void)(*vm)->DetachCurrentThread(vm);
    }

    CoreJ2DestroyJob job;
    memset(&job, 0, sizeof(job));
    job.vm = vm;
    job.rc = JNI_ERR;
    job.finished = 0;
    pthread_mutex_init(&job.mu, NULL);
    pthread_cond_init(&job.cv, NULL);

    pthread_t thread;
    if (pthread_create(&thread, NULL, corej2_destroy_thread_main, &job) != 0) {
        pthread_mutex_destroy(&job.mu);
        pthread_cond_destroy(&job.cv);
        /* Last resort: block on calling thread. */
        return (int)(*vm)->DestroyJavaVM(vm);
    }

    struct timespec deadline;
    clock_gettime(CLOCK_REALTIME, &deadline);
    deadline.tv_sec += COREJ2_DESTROY_TIMEOUT_SEC;

    pthread_mutex_lock(&job.mu);
    while (!job.finished) {
        int wr = pthread_cond_timedwait(&job.cv, &job.mu, &deadline);
        if (wr == ETIMEDOUT) {
            break;
        }
    }
    int finished = job.finished;
    jint rc = job.rc;
    pthread_mutex_unlock(&job.mu);

    if (!finished) {
        /* HotSpot may block forever in Threads::destroy_vm; reclaim on process exit. */
        pthread_detach(thread);
        pthread_mutex_destroy(&job.mu);
        pthread_cond_destroy(&job.cv);
        return COREJ2_DESTROY_TIMED_OUT;
    }

    pthread_join(thread, NULL);
    pthread_mutex_destroy(&job.mu);
    pthread_cond_destroy(&job.cv);
    return (int)rc;
}
