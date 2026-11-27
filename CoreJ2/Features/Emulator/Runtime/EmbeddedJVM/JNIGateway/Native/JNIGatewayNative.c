#include "JNIGatewayNative.h"

#include "../../Native/EmbeddedJVMNative.h"

#include <jni.h>

#include <pthread.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

enum {
    COREJ2_JGW_OK = 0,
    COREJ2_JGW_NO_VM = -1,
    COREJ2_JGW_ATTACH = -2,
    COREJ2_JGW_LOCAL_FRAME = -3,
    COREJ2_JGW_CLASS = -4,
    COREJ2_JGW_METHOD = -5,
    COREJ2_JGW_EXCEPTION = -6,
    COREJ2_JGW_NOT_COMPLETED = -7,
    COREJ2_JGW_UNAVAILABLE = -8,
    COREJ2_JGW_FIELD = -9,
    COREJ2_JGW_OBJECT = -10,
    COREJ2_JGW_TYPE = -11,
    COREJ2_JGW_REG = -12
};

enum {
    COREJ2_JGW_MAX_CLASSES = 128,
    COREJ2_JGW_MAX_METHODS = 512,
    COREJ2_JGW_MAX_FIELDS = 256,
    COREJ2_JGW_MAX_OBJECTS = 256,
    COREJ2_JGW_NAME_MAX = 512
};

typedef struct {
    int in_use;
    uint64_t id;
    jclass global;
    char name[COREJ2_JGW_NAME_MAX];
} CoreJ2JgwClassSlot;

typedef struct {
    int in_use;
    uint64_t id;
    uint64_t class_id;
    jmethodID mid;
    char name[256];
    char signature[256];
    int is_static;
} CoreJ2JgwMethodSlot;

typedef struct {
    int in_use;
    uint64_t id;
    uint64_t class_id;
    jfieldID fid;
    char name[256];
    char signature[256];
    int is_static;
} CoreJ2JgwFieldSlot;

typedef struct {
    int in_use;
    uint64_t id;
    uint64_t class_id;
    jobject global;
} CoreJ2JgwObjectSlot;

static pthread_mutex_t g_cache_lock = PTHREAD_MUTEX_INITIALIZER;
static CoreJ2JgwClassSlot g_classes[COREJ2_JGW_MAX_CLASSES];
static CoreJ2JgwMethodSlot g_methods[COREJ2_JGW_MAX_METHODS];
static CoreJ2JgwFieldSlot g_fields[COREJ2_JGW_MAX_FIELDS];
static CoreJ2JgwObjectSlot g_objects[COREJ2_JGW_MAX_OBJECTS];
static uint64_t g_next_id = 1;
static int32_t g_find_class_count = 0;
static int32_t g_get_method_count = 0;
static int32_t g_get_field_count = 0;

static void jgw_clear_exception(JNIEnv *env) {
    if (env != NULL && (*env)->ExceptionCheck(env)) {
        (*env)->ExceptionClear(env);
    }
}

static void jgw_copy_exception(
    JNIEnv *env,
    char *out_type,
    int32_t out_type_len,
    char *out_message,
    int32_t out_message_len
) {
    if (out_type != NULL && out_type_len > 0) {
        out_type[0] = '\0';
    }
    if (out_message != NULL && out_message_len > 0) {
        out_message[0] = '\0';
    }
    if (env == NULL || !(*env)->ExceptionCheck(env)) {
        return;
    }

    jthrowable exc = (*env)->ExceptionOccurred(env);
    if (exc == NULL) {
        return;
    }
    /* Keep a global ref so we can still inspect after ExceptionDescribe clears. */
    jthrowable held = (jthrowable)(*env)->NewGlobalRef(env, exc);
    (*env)->ExceptionDescribe(env); /* prints stack and clears pending exception */
    if (held == NULL) {
        return;
    }

    jclass exc_class = (*env)->GetObjectClass(env, held);
    if (exc_class != NULL && out_type != NULL && out_type_len > 0) {
        jclass class_class = (*env)->FindClass(env, "java/lang/Class");
        if (class_class != NULL) {
            jmethodID get_name = (*env)->GetMethodID(env, class_class, "getName", "()Ljava/lang/String;");
            if (get_name != NULL) {
                jstring name = (jstring)(*env)->CallObjectMethod(env, exc_class, get_name);
                if (name != NULL) {
                    const char *utf = (*env)->GetStringUTFChars(env, name, NULL);
                    if (utf != NULL) {
                        snprintf(out_type, (size_t)out_type_len, "%s", utf);
                        (*env)->ReleaseStringUTFChars(env, name, utf);
                    }
                }
                jgw_clear_exception(env);
            }
            jgw_clear_exception(env);
        }
        jgw_clear_exception(env);
    }

    if (exc_class != NULL && out_message != NULL && out_message_len > 0) {
        jmethodID get_message = (*env)->GetMethodID(env, exc_class, "getMessage", "()Ljava/lang/String;");
        if (get_message != NULL) {
            jstring msg = (jstring)(*env)->CallObjectMethod(env, held, get_message);
            if (msg != NULL) {
                const char *utf = (*env)->GetStringUTFChars(env, msg, NULL);
                if (utf != NULL) {
                    snprintf(out_message, (size_t)out_message_len, "%s", utf);
                    (*env)->ReleaseStringUTFChars(env, msg, utf);
                }
            }
            jgw_clear_exception(env);
        } else {
            jgw_clear_exception(env);
        }
    }

    (*env)->DeleteGlobalRef(env, held);
}

static int jgw_attach(JavaVM *vm, JNIEnv **env_out, int *did_attach_out) {
    JNIEnv *env = NULL;
    jint attach = (*vm)->GetEnv(vm, (void **)&env, JNI_VERSION_10);
    *did_attach_out = 0;
    if (attach == JNI_EDETACHED) {
        if ((*vm)->AttachCurrentThread(vm, (void **)&env, NULL) != JNI_OK || env == NULL) {
            return COREJ2_JGW_ATTACH;
        }
        *did_attach_out = 1;
    } else if (attach != JNI_OK || env == NULL) {
        return COREJ2_JGW_ATTACH;
    }
    *env_out = env;
    return COREJ2_JGW_OK;
}

static CoreJ2JgwClassSlot *jgw_find_class_slot_by_name(const char *name) {
    for (int i = 0; i < COREJ2_JGW_MAX_CLASSES; i++) {
        if (g_classes[i].in_use && strcmp(g_classes[i].name, name) == 0) {
            return &g_classes[i];
        }
    }
    return NULL;
}

static CoreJ2JgwClassSlot *jgw_find_class_slot_by_id(uint64_t id) {
    for (int i = 0; i < COREJ2_JGW_MAX_CLASSES; i++) {
        if (g_classes[i].in_use && g_classes[i].id == id) {
            return &g_classes[i];
        }
    }
    return NULL;
}

static CoreJ2JgwClassSlot *jgw_alloc_class_slot(void) {
    for (int i = 0; i < COREJ2_JGW_MAX_CLASSES; i++) {
        if (!g_classes[i].in_use) {
            return &g_classes[i];
        }
    }
    return NULL;
}

int corej2_jni_gateway_cache_find_class(const char *binary_name, uint64_t *out_id) {
    if (binary_name == NULL || out_id == NULL) {
        return COREJ2_JGW_CLASS;
    }

    pthread_mutex_lock(&g_cache_lock);
    CoreJ2JgwClassSlot *existing = jgw_find_class_slot_by_name(binary_name);
    if (existing != NULL) {
        *out_id = existing->id;
        pthread_mutex_unlock(&g_cache_lock);
        return COREJ2_JGW_OK;
    }
    pthread_mutex_unlock(&g_cache_lock);

    JavaVM *vm = (JavaVM *)corej2_embedded_jvm_javavm();
    if (vm == NULL) {
        return COREJ2_JGW_NO_VM;
    }

    JNIEnv *env = NULL;
    int did_attach = 0;
    int attach_rc = jgw_attach(vm, &env, &did_attach);
    if (attach_rc != COREJ2_JGW_OK) {
        return attach_rc;
    }

    g_find_class_count += 1;
    jclass local = (*env)->FindClass(env, binary_name);
    if (local == NULL) {
        jgw_clear_exception(env);
        if (did_attach) {
            (*vm)->DetachCurrentThread(vm);
        }
        return COREJ2_JGW_CLASS;
    }

    jclass global = (jclass)(*env)->NewGlobalRef(env, local);
    (*env)->DeleteLocalRef(env, local);
    if (global == NULL) {
        jgw_clear_exception(env);
        if (did_attach) {
            (*vm)->DetachCurrentThread(vm);
        }
        return COREJ2_JGW_LOCAL_FRAME;
    }

    pthread_mutex_lock(&g_cache_lock);
    existing = jgw_find_class_slot_by_name(binary_name);
    if (existing != NULL) {
        *out_id = existing->id;
        pthread_mutex_unlock(&g_cache_lock);
        (*env)->DeleteGlobalRef(env, global);
        if (did_attach) {
            (*vm)->DetachCurrentThread(vm);
        }
        return COREJ2_JGW_OK;
    }

    CoreJ2JgwClassSlot *slot = jgw_alloc_class_slot();
    if (slot == NULL) {
        pthread_mutex_unlock(&g_cache_lock);
        (*env)->DeleteGlobalRef(env, global);
        if (did_attach) {
            (*vm)->DetachCurrentThread(vm);
        }
        return COREJ2_JGW_LOCAL_FRAME;
    }

    slot->in_use = 1;
    slot->id = g_next_id++;
    slot->global = global;
    snprintf(slot->name, sizeof(slot->name), "%s", binary_name);
    *out_id = slot->id;
    pthread_mutex_unlock(&g_cache_lock);

    if (did_attach) {
        (*vm)->DetachCurrentThread(vm);
    }
    return COREJ2_JGW_OK;
}

int corej2_jni_gateway_cache_get_method(
    uint64_t class_id,
    const char *name,
    const char *signature,
    int32_t is_static,
    uint64_t *out_id
) {
    if (name == NULL || signature == NULL || out_id == NULL) {
        return COREJ2_JGW_METHOD;
    }

    pthread_mutex_lock(&g_cache_lock);
    for (int i = 0; i < COREJ2_JGW_MAX_METHODS; i++) {
        if (g_methods[i].in_use
            && g_methods[i].class_id == class_id
            && g_methods[i].is_static == (is_static ? 1 : 0)
            && strcmp(g_methods[i].name, name) == 0
            && strcmp(g_methods[i].signature, signature) == 0) {
            *out_id = g_methods[i].id;
            pthread_mutex_unlock(&g_cache_lock);
            return COREJ2_JGW_OK;
        }
    }
    CoreJ2JgwClassSlot *class_slot = jgw_find_class_slot_by_id(class_id);
    jclass global = class_slot != NULL ? class_slot->global : NULL;
    pthread_mutex_unlock(&g_cache_lock);

    if (global == NULL) {
        return COREJ2_JGW_CLASS;
    }

    JavaVM *vm = (JavaVM *)corej2_embedded_jvm_javavm();
    if (vm == NULL) {
        return COREJ2_JGW_NO_VM;
    }
    JNIEnv *env = NULL;
    int did_attach = 0;
    int attach_rc = jgw_attach(vm, &env, &did_attach);
    if (attach_rc != COREJ2_JGW_OK) {
        return attach_rc;
    }

    g_get_method_count += 1;
    jmethodID mid = NULL;
    if (is_static) {
        mid = (*env)->GetStaticMethodID(env, global, name, signature);
    } else {
        mid = (*env)->GetMethodID(env, global, name, signature);
    }
    if (mid == NULL) {
        jgw_clear_exception(env);
        if (did_attach) {
            (*vm)->DetachCurrentThread(vm);
        }
        return COREJ2_JGW_METHOD;
    }

    pthread_mutex_lock(&g_cache_lock);
    for (int i = 0; i < COREJ2_JGW_MAX_METHODS; i++) {
        if (!g_methods[i].in_use) {
            g_methods[i].in_use = 1;
            g_methods[i].id = g_next_id++;
            g_methods[i].class_id = class_id;
            g_methods[i].mid = mid;
            g_methods[i].is_static = is_static ? 1 : 0;
            snprintf(g_methods[i].name, sizeof(g_methods[i].name), "%s", name);
            snprintf(g_methods[i].signature, sizeof(g_methods[i].signature), "%s", signature);
            *out_id = g_methods[i].id;
            pthread_mutex_unlock(&g_cache_lock);
            if (did_attach) {
                (*vm)->DetachCurrentThread(vm);
            }
            return COREJ2_JGW_OK;
        }
    }
    pthread_mutex_unlock(&g_cache_lock);
    if (did_attach) {
        (*vm)->DetachCurrentThread(vm);
    }
    return COREJ2_JGW_LOCAL_FRAME;
}

int corej2_jni_gateway_cache_get_field(
    uint64_t class_id,
    const char *name,
    const char *signature,
    int32_t is_static,
    uint64_t *out_id
) {
    if (name == NULL || signature == NULL || out_id == NULL) {
        return COREJ2_JGW_FIELD;
    }

    pthread_mutex_lock(&g_cache_lock);
    for (int i = 0; i < COREJ2_JGW_MAX_FIELDS; i++) {
        if (g_fields[i].in_use
            && g_fields[i].class_id == class_id
            && g_fields[i].is_static == (is_static ? 1 : 0)
            && strcmp(g_fields[i].name, name) == 0
            && strcmp(g_fields[i].signature, signature) == 0) {
            *out_id = g_fields[i].id;
            pthread_mutex_unlock(&g_cache_lock);
            return COREJ2_JGW_OK;
        }
    }
    CoreJ2JgwClassSlot *class_slot = jgw_find_class_slot_by_id(class_id);
    jclass global = class_slot != NULL ? class_slot->global : NULL;
    pthread_mutex_unlock(&g_cache_lock);

    if (global == NULL) {
        return COREJ2_JGW_CLASS;
    }

    JavaVM *vm = (JavaVM *)corej2_embedded_jvm_javavm();
    if (vm == NULL) {
        return COREJ2_JGW_NO_VM;
    }
    JNIEnv *env = NULL;
    int did_attach = 0;
    int attach_rc = jgw_attach(vm, &env, &did_attach);
    if (attach_rc != COREJ2_JGW_OK) {
        return attach_rc;
    }

    g_get_field_count += 1;
    jfieldID fid = NULL;
    if (is_static) {
        fid = (*env)->GetStaticFieldID(env, global, name, signature);
    } else {
        fid = (*env)->GetFieldID(env, global, name, signature);
    }
    if (fid == NULL) {
        jgw_clear_exception(env);
        if (did_attach) {
            (*vm)->DetachCurrentThread(vm);
        }
        return COREJ2_JGW_FIELD;
    }

    pthread_mutex_lock(&g_cache_lock);
    for (int i = 0; i < COREJ2_JGW_MAX_FIELDS; i++) {
        if (!g_fields[i].in_use) {
            g_fields[i].in_use = 1;
            g_fields[i].id = g_next_id++;
            g_fields[i].class_id = class_id;
            g_fields[i].fid = fid;
            g_fields[i].is_static = is_static ? 1 : 0;
            snprintf(g_fields[i].name, sizeof(g_fields[i].name), "%s", name);
            snprintf(g_fields[i].signature, sizeof(g_fields[i].signature), "%s", signature);
            *out_id = g_fields[i].id;
            pthread_mutex_unlock(&g_cache_lock);
            if (did_attach) {
                (*vm)->DetachCurrentThread(vm);
            }
            return COREJ2_JGW_OK;
        }
    }
    pthread_mutex_unlock(&g_cache_lock);
    if (did_attach) {
        (*vm)->DetachCurrentThread(vm);
    }
    return COREJ2_JGW_LOCAL_FRAME;
}

void corej2_jni_gateway_cache_clear_all(void) {
    JavaVM *vm = (JavaVM *)corej2_embedded_jvm_javavm();
    JNIEnv *env = NULL;
    int did_attach = 0;
    if (vm != NULL) {
        (void)jgw_attach(vm, &env, &did_attach);
    }

    pthread_mutex_lock(&g_cache_lock);
    for (int i = 0; i < COREJ2_JGW_MAX_OBJECTS; i++) {
        if (g_objects[i].in_use && g_objects[i].global != NULL && env != NULL) {
            (*env)->DeleteGlobalRef(env, g_objects[i].global);
        }
        memset(&g_objects[i], 0, sizeof(g_objects[i]));
    }
    for (int i = 0; i < COREJ2_JGW_MAX_CLASSES; i++) {
        if (g_classes[i].in_use && g_classes[i].global != NULL && env != NULL) {
            (*env)->DeleteGlobalRef(env, g_classes[i].global);
        }
        memset(&g_classes[i], 0, sizeof(g_classes[i]));
    }
    memset(g_methods, 0, sizeof(g_methods));
    memset(g_fields, 0, sizeof(g_fields));
    pthread_mutex_unlock(&g_cache_lock);

    if (did_attach && vm != NULL) {
        (*vm)->DetachCurrentThread(vm);
    }
}

int corej2_jni_gateway_object_create_global(uint64_t class_id, uint64_t *out_id) {
    if (out_id == NULL) {
        return COREJ2_JGW_OBJECT;
    }

    pthread_mutex_lock(&g_cache_lock);
    CoreJ2JgwClassSlot *class_slot = jgw_find_class_slot_by_id(class_id);
    jclass cls = class_slot != NULL ? class_slot->global : NULL;
    pthread_mutex_unlock(&g_cache_lock);
    if (cls == NULL) {
        return COREJ2_JGW_CLASS;
    }

    JavaVM *vm = (JavaVM *)corej2_embedded_jvm_javavm();
    if (vm == NULL) {
        return COREJ2_JGW_NO_VM;
    }
    JNIEnv *env = NULL;
    int did_attach = 0;
    int attach_rc = jgw_attach(vm, &env, &did_attach);
    if (attach_rc != COREJ2_JGW_OK) {
        return attach_rc;
    }

    jobject local = (*env)->AllocObject(env, cls);
    if (local == NULL) {
        jgw_clear_exception(env);
        if (did_attach) {
            (*vm)->DetachCurrentThread(vm);
        }
        return COREJ2_JGW_OBJECT;
    }

    jobject global = (*env)->NewGlobalRef(env, local);
    (*env)->DeleteLocalRef(env, local);
    if (global == NULL) {
        jgw_clear_exception(env);
        if (did_attach) {
            (*vm)->DetachCurrentThread(vm);
        }
        return COREJ2_JGW_LOCAL_FRAME;
    }

    pthread_mutex_lock(&g_cache_lock);
    for (int i = 0; i < COREJ2_JGW_MAX_OBJECTS; i++) {
        if (!g_objects[i].in_use) {
            g_objects[i].in_use = 1;
            g_objects[i].id = g_next_id++;
            g_objects[i].class_id = class_id;
            g_objects[i].global = global;
            *out_id = g_objects[i].id;
            pthread_mutex_unlock(&g_cache_lock);
            if (did_attach) {
                (*vm)->DetachCurrentThread(vm);
            }
            return COREJ2_JGW_OK;
        }
    }
    pthread_mutex_unlock(&g_cache_lock);
    (*env)->DeleteGlobalRef(env, global);
    if (did_attach) {
        (*vm)->DetachCurrentThread(vm);
    }
    return COREJ2_JGW_LOCAL_FRAME;
}

int corej2_jni_gateway_object_release_global(uint64_t object_id) {
    JavaVM *vm = (JavaVM *)corej2_embedded_jvm_javavm();
    JNIEnv *env = NULL;
    int did_attach = 0;
    jobject global = NULL;

    pthread_mutex_lock(&g_cache_lock);
    for (int i = 0; i < COREJ2_JGW_MAX_OBJECTS; i++) {
        if (g_objects[i].in_use && g_objects[i].id == object_id) {
            global = g_objects[i].global;
            memset(&g_objects[i], 0, sizeof(g_objects[i]));
            break;
        }
    }
    pthread_mutex_unlock(&g_cache_lock);

    if (global == NULL) {
        return 0;
    }

    if (vm != NULL && jgw_attach(vm, &env, &did_attach) == COREJ2_JGW_OK && env != NULL) {
        (*env)->DeleteGlobalRef(env, global);
        if (did_attach) {
            (*vm)->DetachCurrentThread(vm);
        }
    }
    return 1;
}

int corej2_jni_gateway_object_contains(uint64_t object_id) {
    int found = 0;
    pthread_mutex_lock(&g_cache_lock);
    for (int i = 0; i < COREJ2_JGW_MAX_OBJECTS; i++) {
        if (g_objects[i].in_use && g_objects[i].id == object_id) {
            found = 1;
            break;
        }
    }
    pthread_mutex_unlock(&g_cache_lock);
    return found;
}

int32_t corej2_jni_gateway_cache_find_class_count(void) {
    return g_find_class_count;
}

int32_t corej2_jni_gateway_cache_get_method_count(void) {
    return g_get_method_count;
}

int32_t corej2_jni_gateway_cache_get_field_count(void) {
    return g_get_field_count;
}

int32_t corej2_jni_gateway_cache_live_global_class_count(void) {
    int32_t count = 0;
    pthread_mutex_lock(&g_cache_lock);
    for (int i = 0; i < COREJ2_JGW_MAX_CLASSES; i++) {
        if (g_classes[i].in_use) {
            count += 1;
        }
    }
    pthread_mutex_unlock(&g_cache_lock);
    return count;
}

int32_t corej2_jni_gateway_cache_live_global_object_count(void) {
    int32_t count = 0;
    pthread_mutex_lock(&g_cache_lock);
    for (int i = 0; i < COREJ2_JGW_MAX_OBJECTS; i++) {
        if (g_objects[i].in_use) {
            count += 1;
        }
    }
    pthread_mutex_unlock(&g_cache_lock);
    return count;
}

int corej2_jni_gateway_invoke_hello_world_main_cached(
    uint64_t class_native_id,
    int32_t *out_frames_pushed,
    int32_t *out_frames_popped,
    char *out_exception_type,
    int32_t out_exception_type_len,
    char *out_exception_message,
    int32_t out_exception_message_len
) {
    if (out_frames_pushed != NULL) {
        *out_frames_pushed = 0;
    }
    if (out_frames_popped != NULL) {
        *out_frames_popped = 0;
    }
    if (out_exception_type != NULL && out_exception_type_len > 0) {
        out_exception_type[0] = '\0';
    }
    if (out_exception_message != NULL && out_exception_message_len > 0) {
        out_exception_message[0] = '\0';
    }

    pthread_mutex_lock(&g_cache_lock);
    CoreJ2JgwClassSlot *class_slot = jgw_find_class_slot_by_id(class_native_id);
    jclass cls = class_slot != NULL ? class_slot->global : NULL;
    pthread_mutex_unlock(&g_cache_lock);
    if (cls == NULL) {
        return COREJ2_JGW_CLASS;
    }

    JavaVM *vm = (JavaVM *)corej2_embedded_jvm_javavm();
    if (vm == NULL) {
        return COREJ2_JGW_NO_VM;
    }

    JNIEnv *env = NULL;
    int did_attach = 0;
    int attach_rc = jgw_attach(vm, &env, &did_attach);
    if (attach_rc != COREJ2_JGW_OK) {
        return attach_rc;
    }

    if ((*env)->PushLocalFrame(env, 16) < 0) {
        jgw_clear_exception(env);
        if (did_attach) {
            (*vm)->DetachCurrentThread(vm);
        }
        return COREJ2_JGW_LOCAL_FRAME;
    }
    if (out_frames_pushed != NULL) {
        *out_frames_pushed = 1;
    }

    int result = COREJ2_JGW_OK;
    jmethodID main_mid = (*env)->GetStaticMethodID(env, cls, "main", "([Ljava/lang/String;)V");
    if (main_mid == NULL) {
        jgw_copy_exception(
            env,
            out_exception_type,
            out_exception_type_len,
            out_exception_message,
            out_exception_message_len
        );
        jgw_clear_exception(env);
        result = COREJ2_JGW_METHOD;
        goto done;
    }

    jclass string_cls = (*env)->FindClass(env, "java/lang/String");
    if (string_cls == NULL) {
        jgw_clear_exception(env);
        result = COREJ2_JGW_CLASS;
        goto done;
    }
    jobjectArray args = (*env)->NewObjectArray(env, 0, string_cls, NULL);
    if (args == NULL) {
        jgw_clear_exception(env);
        result = COREJ2_JGW_LOCAL_FRAME;
        goto done;
    }

    (*env)->CallStaticVoidMethod(env, cls, main_mid, args);
    if ((*env)->ExceptionCheck(env)) {
        jgw_copy_exception(
            env,
            out_exception_type,
            out_exception_type_len,
            out_exception_message,
            out_exception_message_len
        );
        jgw_clear_exception(env);
        result = COREJ2_JGW_EXCEPTION;
        goto done;
    }

    jfieldID completed_fid = (*env)->GetStaticFieldID(env, cls, "completed", "Z");
    if (completed_fid == NULL) {
        jgw_clear_exception(env);
        result = COREJ2_JGW_METHOD;
        goto done;
    }
    if (!(*env)->GetStaticBooleanField(env, cls, completed_fid)) {
        result = COREJ2_JGW_NOT_COMPLETED;
        goto done;
    }

done:
    (void)(*env)->PopLocalFrame(env, NULL);
    if (out_frames_popped != NULL) {
        *out_frames_popped = 1;
    }
    if (did_attach) {
        (*vm)->DetachCurrentThread(vm);
    }
    return result;
}

static CoreJ2JgwMethodSlot *jgw_find_method_slot_by_id(uint64_t id) {
    for (int i = 0; i < COREJ2_JGW_MAX_METHODS; i++) {
        if (g_methods[i].in_use && g_methods[i].id == id) {
            return &g_methods[i];
        }
    }
    return NULL;
}

static CoreJ2JgwObjectSlot *jgw_find_object_slot_by_id(uint64_t id) {
    for (int i = 0; i < COREJ2_JGW_MAX_OBJECTS; i++) {
        if (g_objects[i].in_use && g_objects[i].id == id) {
            return &g_objects[i];
        }
    }
    return NULL;
}

static int jgw_store_object_from_local(
    JNIEnv *env,
    jobject local,
    uint64_t class_id,
    uint64_t *out_id
) {
    if (local == NULL) {
        *out_id = 0;
        return COREJ2_JGW_OK;
    }
    jobject global = (*env)->NewGlobalRef(env, local);
    (*env)->DeleteLocalRef(env, local);
    if (global == NULL) {
        jgw_clear_exception(env);
        return COREJ2_JGW_LOCAL_FRAME;
    }
    pthread_mutex_lock(&g_cache_lock);
    for (int i = 0; i < COREJ2_JGW_MAX_OBJECTS; i++) {
        if (!g_objects[i].in_use) {
            g_objects[i].in_use = 1;
            g_objects[i].id = g_next_id++;
            g_objects[i].class_id = class_id;
            g_objects[i].global = global;
            *out_id = g_objects[i].id;
            pthread_mutex_unlock(&g_cache_lock);
            return COREJ2_JGW_OK;
        }
    }
    pthread_mutex_unlock(&g_cache_lock);
    (*env)->DeleteGlobalRef(env, global);
    return COREJ2_JGW_LOCAL_FRAME;
}

int corej2_jni_gateway_read_byte_array(
    uint64_t object_id,
    uint8_t *out_buffer,
    int32_t buffer_len,
    int32_t *out_len
) {
    if (out_len != NULL) {
        *out_len = 0;
    }

    JavaVM *vm = (JavaVM *)corej2_embedded_jvm_javavm();
    if (vm == NULL) {
        return COREJ2_JGW_NO_VM;
    }

    jobject global = NULL;
    pthread_mutex_lock(&g_cache_lock);
    CoreJ2JgwObjectSlot *slot = jgw_find_object_slot_by_id(object_id);
    if (slot != NULL) {
        global = slot->global;
    }
    pthread_mutex_unlock(&g_cache_lock);
    if (global == NULL) {
        return COREJ2_JGW_OBJECT;
    }

    JNIEnv *env = NULL;
    int did_attach = 0;
    if (jgw_attach(vm, &env, &did_attach) != COREJ2_JGW_OK || env == NULL) {
        return COREJ2_JGW_ATTACH;
    }

    int result = COREJ2_JGW_OK;
    if ((*env)->PushLocalFrame(env, 4) != JNI_OK) {
        jgw_clear_exception(env);
        result = COREJ2_JGW_LOCAL_FRAME;
    } else {
        jsize length = (*env)->GetArrayLength(env, (jarray)global);
        if ((*env)->ExceptionCheck(env)) {
            jgw_clear_exception(env);
            result = COREJ2_JGW_TYPE;
        } else {
            if (out_len != NULL) {
                *out_len = (int32_t)length;
            }
            if (out_buffer == NULL || buffer_len < (int32_t)length) {
                result = COREJ2_JGW_TYPE;
            } else if (length > 0) {
                (*env)->GetByteArrayRegion(env, (jbyteArray)global, 0, length, (jbyte *)out_buffer);
                if ((*env)->ExceptionCheck(env)) {
                    jgw_clear_exception(env);
                    result = COREJ2_JGW_TYPE;
                }
            }
        }
        (*env)->PopLocalFrame(env, NULL);
    }

    if (did_attach) {
        (*vm)->DetachCurrentThread(vm);
    }
    return result;
}

static int jgw_fill_jvalues(
    JNIEnv *env,
    const corej2_jgw_value *args,
    int32_t argc,
    jvalue *out_jv
) {
    for (int32_t i = 0; i < argc; i++) {
        memset(&out_jv[i], 0, sizeof(jvalue));
        switch (args[i].kind) {
            case COREJ2_JGW_KIND_BOOLEAN:
                out_jv[i].z = args[i].i32 ? JNI_TRUE : JNI_FALSE;
                break;
            case COREJ2_JGW_KIND_INT:
                out_jv[i].i = args[i].i32;
                break;
            case COREJ2_JGW_KIND_LONG:
                out_jv[i].j = args[i].i64;
                break;
            case COREJ2_JGW_KIND_FLOAT:
                out_jv[i].f = args[i].f32;
                break;
            case COREJ2_JGW_KIND_DOUBLE:
                out_jv[i].d = args[i].f64;
                break;
            case COREJ2_JGW_KIND_STRING: {
                if (args[i].str == NULL) {
                    out_jv[i].l = NULL;
                } else {
                    jstring s = (*env)->NewStringUTF(env, args[i].str);
                    if (s == NULL) {
                        jgw_clear_exception(env);
                        return COREJ2_JGW_LOCAL_FRAME;
                    }
                    out_jv[i].l = s;
                }
                break;
            }
            case COREJ2_JGW_KIND_OBJECT: {
                pthread_mutex_lock(&g_cache_lock);
                CoreJ2JgwObjectSlot *slot = jgw_find_object_slot_by_id(args[i].object_id);
                jobject global = slot != NULL ? slot->global : NULL;
                pthread_mutex_unlock(&g_cache_lock);
                if (global == NULL) {
                    return COREJ2_JGW_OBJECT;
                }
                out_jv[i].l = global;
                break;
            }
            case COREJ2_JGW_KIND_NULL:
                out_jv[i].l = NULL;
                break;
            default:
                return COREJ2_JGW_TYPE;
        }
    }
    return COREJ2_JGW_OK;
}

int corej2_jni_gateway_object_new_global(
    uint64_t class_id,
    uint64_t constructor_method_id,
    const corej2_jgw_value *args,
    int32_t argc,
    uint64_t *out_id
) {
    if (out_id == NULL) {
        return COREJ2_JGW_OBJECT;
    }
    *out_id = 0;
    if (argc < 0 || argc > COREJ2_JGW_MAX_CALL_ARGS) {
        return COREJ2_JGW_TYPE;
    }
    if (argc > 0 && args == NULL) {
        return COREJ2_JGW_TYPE;
    }

    pthread_mutex_lock(&g_cache_lock);
    CoreJ2JgwClassSlot *class_slot = jgw_find_class_slot_by_id(class_id);
    jclass cls = class_slot != NULL ? class_slot->global : NULL;
    CoreJ2JgwMethodSlot *method_slot = jgw_find_method_slot_by_id(constructor_method_id);
    jmethodID mid = method_slot != NULL ? method_slot->mid : NULL;
    pthread_mutex_unlock(&g_cache_lock);
    if (cls == NULL) {
        return COREJ2_JGW_CLASS;
    }
    if (mid == NULL) {
        return COREJ2_JGW_METHOD;
    }

    JavaVM *vm = (JavaVM *)corej2_embedded_jvm_javavm();
    if (vm == NULL) {
        return COREJ2_JGW_NO_VM;
    }
    JNIEnv *env = NULL;
    int did_attach = 0;
    int attach_rc = jgw_attach(vm, &env, &did_attach);
    if (attach_rc != COREJ2_JGW_OK) {
        return attach_rc;
    }

    if ((*env)->PushLocalFrame(env, 16) != 0) {
        jgw_clear_exception(env);
        if (did_attach) {
            (*vm)->DetachCurrentThread(vm);
        }
        return COREJ2_JGW_LOCAL_FRAME;
    }

    jvalue jv[COREJ2_JGW_MAX_CALL_ARGS];
    int fill_rc = jgw_fill_jvalues(env, args, argc, jv);
    if (fill_rc != COREJ2_JGW_OK) {
        (void)(*env)->PopLocalFrame(env, NULL);
        if (did_attach) {
            (*vm)->DetachCurrentThread(vm);
        }
        return fill_rc;
    }

    jobject local = (*env)->NewObjectA(env, cls, mid, jv);
    if (local == NULL || (*env)->ExceptionCheck(env)) {
        /* Describe to stderr (object_new_global has no out_exception buffers). */
        jgw_copy_exception(env, NULL, 0, NULL, 0);
        jgw_clear_exception(env);
        (void)(*env)->PopLocalFrame(env, NULL);
        if (did_attach) {
            (*vm)->DetachCurrentThread(vm);
        }
        return COREJ2_JGW_EXCEPTION;
    }

    local = (*env)->PopLocalFrame(env, local);
    if (local == NULL) {
        jgw_clear_exception(env);
        if (did_attach) {
            (*vm)->DetachCurrentThread(vm);
        }
        return COREJ2_JGW_LOCAL_FRAME;
    }

    int store_rc = jgw_store_object_from_local(env, local, class_id, out_id);
    if (did_attach) {
        (*vm)->DetachCurrentThread(vm);
    }
    return store_rc;
}

int corej2_jni_gateway_call(
    uint64_t class_id,
    uint64_t method_id,
    int32_t is_static,
    uint64_t receiver_object_id,
    const corej2_jgw_value *args,
    int32_t argc,
    int32_t return_kind,
    corej2_jgw_value *out_result,
    char *out_string,
    int32_t out_string_len,
    int32_t *out_frames_pushed,
    int32_t *out_frames_popped,
    char *out_exception_type,
    int32_t out_exception_type_len,
    char *out_exception_message,
    int32_t out_exception_message_len
) {
    if (out_frames_pushed != NULL) {
        *out_frames_pushed = 0;
    }
    if (out_frames_popped != NULL) {
        *out_frames_popped = 0;
    }
    if (out_exception_type != NULL && out_exception_type_len > 0) {
        out_exception_type[0] = '\0';
    }
    if (out_exception_message != NULL && out_exception_message_len > 0) {
        out_exception_message[0] = '\0';
    }
    if (out_string != NULL && out_string_len > 0) {
        out_string[0] = '\0';
    }
    if (out_result != NULL) {
        memset(out_result, 0, sizeof(*out_result));
        out_result->kind = return_kind;
    }
    if (argc < 0 || argc > COREJ2_JGW_MAX_CALL_ARGS) {
        return COREJ2_JGW_TYPE;
    }
    if (argc > 0 && args == NULL) {
        return COREJ2_JGW_TYPE;
    }

    pthread_mutex_lock(&g_cache_lock);
    CoreJ2JgwClassSlot *class_slot = jgw_find_class_slot_by_id(class_id);
    jclass cls = class_slot != NULL ? class_slot->global : NULL;
    CoreJ2JgwMethodSlot *method_slot = jgw_find_method_slot_by_id(method_id);
    jmethodID mid = method_slot != NULL ? method_slot->mid : NULL;
    jobject receiver = NULL;
    if (!is_static) {
        CoreJ2JgwObjectSlot *obj_slot = jgw_find_object_slot_by_id(receiver_object_id);
        receiver = obj_slot != NULL ? obj_slot->global : NULL;
    }
    pthread_mutex_unlock(&g_cache_lock);

    if (cls == NULL) {
        return COREJ2_JGW_CLASS;
    }
    if (mid == NULL) {
        return COREJ2_JGW_METHOD;
    }
    if (!is_static && receiver == NULL) {
        return COREJ2_JGW_OBJECT;
    }

    JavaVM *vm = (JavaVM *)corej2_embedded_jvm_javavm();
    if (vm == NULL) {
        return COREJ2_JGW_NO_VM;
    }
    JNIEnv *env = NULL;
    int did_attach = 0;
    int attach_rc = jgw_attach(vm, &env, &did_attach);
    if (attach_rc != COREJ2_JGW_OK) {
        return attach_rc;
    }

    if ((*env)->PushLocalFrame(env, 64) < 0) {
        jgw_clear_exception(env);
        if (did_attach) {
            (*vm)->DetachCurrentThread(vm);
        }
        return COREJ2_JGW_LOCAL_FRAME;
    }
    if (out_frames_pushed != NULL) {
        *out_frames_pushed = 1;
    }

    int result = COREJ2_JGW_OK;
    jvalue jv[COREJ2_JGW_MAX_CALL_ARGS];
    memset(jv, 0, sizeof(jv));
    if (argc > 0) {
        result = jgw_fill_jvalues(env, args, argc, jv);
        if (result != COREJ2_JGW_OK) {
            goto done;
        }
    }

    switch (return_kind) {
        case COREJ2_JGW_KIND_VOID:
            if (is_static) {
                (*env)->CallStaticVoidMethodA(env, cls, mid, jv);
            } else {
                (*env)->CallVoidMethodA(env, receiver, mid, jv);
            }
            if ((*env)->ExceptionCheck(env)) {
                jgw_copy_exception(
                    env,
                    out_exception_type,
                    out_exception_type_len,
                    out_exception_message,
                    out_exception_message_len
                );
                jgw_clear_exception(env);
                result = COREJ2_JGW_EXCEPTION;
                goto done;
            }
            if (out_result != NULL) {
                out_result->kind = COREJ2_JGW_KIND_VOID;
            }
            break;

        case COREJ2_JGW_KIND_BOOLEAN: {
            jboolean v = is_static
                ? (*env)->CallStaticBooleanMethodA(env, cls, mid, jv)
                : (*env)->CallBooleanMethodA(env, receiver, mid, jv);
            if ((*env)->ExceptionCheck(env)) {
                jgw_copy_exception(
                    env,
                    out_exception_type,
                    out_exception_type_len,
                    out_exception_message,
                    out_exception_message_len
                );
                jgw_clear_exception(env);
                result = COREJ2_JGW_EXCEPTION;
                goto done;
            }
            if (out_result != NULL) {
                out_result->kind = COREJ2_JGW_KIND_BOOLEAN;
                out_result->i32 = v ? 1 : 0;
            }
            break;
        }

        case COREJ2_JGW_KIND_INT: {
            jint v = is_static
                ? (*env)->CallStaticIntMethodA(env, cls, mid, jv)
                : (*env)->CallIntMethodA(env, receiver, mid, jv);
            if ((*env)->ExceptionCheck(env)) {
                jgw_copy_exception(
                    env,
                    out_exception_type,
                    out_exception_type_len,
                    out_exception_message,
                    out_exception_message_len
                );
                jgw_clear_exception(env);
                result = COREJ2_JGW_EXCEPTION;
                goto done;
            }
            if (out_result != NULL) {
                out_result->kind = COREJ2_JGW_KIND_INT;
                out_result->i32 = (int32_t)v;
            }
            break;
        }

        case COREJ2_JGW_KIND_LONG: {
            jlong v = is_static
                ? (*env)->CallStaticLongMethodA(env, cls, mid, jv)
                : (*env)->CallLongMethodA(env, receiver, mid, jv);
            if ((*env)->ExceptionCheck(env)) {
                jgw_copy_exception(
                    env,
                    out_exception_type,
                    out_exception_type_len,
                    out_exception_message,
                    out_exception_message_len
                );
                jgw_clear_exception(env);
                result = COREJ2_JGW_EXCEPTION;
                goto done;
            }
            if (out_result != NULL) {
                out_result->kind = COREJ2_JGW_KIND_LONG;
                out_result->i64 = (int64_t)v;
            }
            break;
        }

        case COREJ2_JGW_KIND_FLOAT: {
            jfloat v = is_static
                ? (*env)->CallStaticFloatMethodA(env, cls, mid, jv)
                : (*env)->CallFloatMethodA(env, receiver, mid, jv);
            if ((*env)->ExceptionCheck(env)) {
                jgw_copy_exception(
                    env,
                    out_exception_type,
                    out_exception_type_len,
                    out_exception_message,
                    out_exception_message_len
                );
                jgw_clear_exception(env);
                result = COREJ2_JGW_EXCEPTION;
                goto done;
            }
            if (out_result != NULL) {
                out_result->kind = COREJ2_JGW_KIND_FLOAT;
                out_result->f32 = (float)v;
            }
            break;
        }

        case COREJ2_JGW_KIND_DOUBLE: {
            jdouble v = is_static
                ? (*env)->CallStaticDoubleMethodA(env, cls, mid, jv)
                : (*env)->CallDoubleMethodA(env, receiver, mid, jv);
            if ((*env)->ExceptionCheck(env)) {
                jgw_copy_exception(
                    env,
                    out_exception_type,
                    out_exception_type_len,
                    out_exception_message,
                    out_exception_message_len
                );
                jgw_clear_exception(env);
                result = COREJ2_JGW_EXCEPTION;
                goto done;
            }
            if (out_result != NULL) {
                out_result->kind = COREJ2_JGW_KIND_DOUBLE;
                out_result->f64 = (double)v;
            }
            break;
        }

        case COREJ2_JGW_KIND_STRING: {
            jobject local = is_static
                ? (*env)->CallStaticObjectMethodA(env, cls, mid, jv)
                : (*env)->CallObjectMethodA(env, receiver, mid, jv);
            if ((*env)->ExceptionCheck(env)) {
                jgw_copy_exception(
                    env,
                    out_exception_type,
                    out_exception_type_len,
                    out_exception_message,
                    out_exception_message_len
                );
                jgw_clear_exception(env);
                result = COREJ2_JGW_EXCEPTION;
                goto done;
            }
            if (local == NULL) {
                if (out_result != NULL) {
                    out_result->kind = COREJ2_JGW_KIND_NULL;
                }
                break;
            }
            const char *utf = (*env)->GetStringUTFChars(env, (jstring)local, NULL);
            if (utf == NULL) {
                jgw_clear_exception(env);
                result = COREJ2_JGW_LOCAL_FRAME;
                goto done;
            }
            if (out_string != NULL && out_string_len > 0) {
                snprintf(out_string, (size_t)out_string_len, "%s", utf);
            }
            (*env)->ReleaseStringUTFChars(env, (jstring)local, utf);
            if (out_result != NULL) {
                out_result->kind = COREJ2_JGW_KIND_STRING;
            }
            break;
        }

        case COREJ2_JGW_KIND_OBJECT: {
            jobject local = is_static
                ? (*env)->CallStaticObjectMethodA(env, cls, mid, jv)
                : (*env)->CallObjectMethodA(env, receiver, mid, jv);
            if ((*env)->ExceptionCheck(env)) {
                jgw_copy_exception(
                    env,
                    out_exception_type,
                    out_exception_type_len,
                    out_exception_message,
                    out_exception_message_len
                );
                jgw_clear_exception(env);
                result = COREJ2_JGW_EXCEPTION;
                goto done;
            }
            if (local == NULL) {
                if (out_result != NULL) {
                    out_result->kind = COREJ2_JGW_KIND_NULL;
                    out_result->object_id = 0;
                }
                break;
            }
            uint64_t obj_id = 0;
            result = jgw_store_object_from_local(env, local, class_id, &obj_id);
            if (result != COREJ2_JGW_OK) {
                goto done;
            }
            if (out_result != NULL) {
                out_result->kind = COREJ2_JGW_KIND_OBJECT;
                out_result->object_id = obj_id;
            }
            break;
        }

        default:
            result = COREJ2_JGW_TYPE;
            break;
    }

done:
    (void)(*env)->PopLocalFrame(env, NULL);
    if (out_frames_popped != NULL) {
        *out_frames_popped = 1;
    }
    if (did_attach) {
        (*vm)->DetachCurrentThread(vm);
    }
    return result;
}

enum { COREJ2_JGW_MAX_NATIVE_SLOTS = 16 };

typedef struct {
    int in_use;
    uint64_t token;
    uint64_t class_id;
    char name[256];
    char signature[256];
    int is_static;
    int slot;
} CoreJ2JgwNativeReg;

static CoreJ2JgwNativeReg g_native_regs[COREJ2_JGW_MAX_NATIVE_SLOTS];

extern void corej2_jni_gateway_trampoline_dispatch(
    uint64_t token,
    const corej2_jgw_value *args,
    int32_t argc,
    corej2_jgw_value *out_result
);

static void jgw_dispatch_void(uint64_t token) {
    corej2_jgw_value out;
    memset(&out, 0, sizeof(out));
    corej2_jni_gateway_trampoline_dispatch(token, NULL, 0, &out);
}

static void jgw_dispatch_int(uint64_t token, jint value) {
    corej2_jgw_value args[1];
    corej2_jgw_value out;
    memset(args, 0, sizeof(args));
    memset(&out, 0, sizeof(out));
    args[0].kind = COREJ2_JGW_KIND_INT;
    args[0].i32 = (int32_t)value;
    corej2_jni_gateway_trampoline_dispatch(token, args, 1, &out);
}

#define JGW_DEFINE_VOID_STATIC(N) \
    JNIEXPORT void JNICALL jgw_native_void_static_##N(JNIEnv *env, jclass cls) { \
        (void)env; (void)cls; \
        if (g_native_regs[N].in_use) { jgw_dispatch_void(g_native_regs[N].token); } \
    }

#define JGW_DEFINE_VOID_INSTANCE(N) \
    JNIEXPORT void JNICALL jgw_native_void_instance_##N(JNIEnv *env, jobject obj) { \
        (void)env; (void)obj; \
        if (g_native_regs[N].in_use) { jgw_dispatch_void(g_native_regs[N].token); } \
    }

#define JGW_DEFINE_INT_STATIC(N) \
    JNIEXPORT void JNICALL jgw_native_int_static_##N(JNIEnv *env, jclass cls, jint v) { \
        (void)env; (void)cls; \
        if (g_native_regs[N].in_use) { jgw_dispatch_int(g_native_regs[N].token, v); } \
    }

JGW_DEFINE_VOID_STATIC(0) JGW_DEFINE_VOID_STATIC(1) JGW_DEFINE_VOID_STATIC(2) JGW_DEFINE_VOID_STATIC(3)
JGW_DEFINE_VOID_STATIC(4) JGW_DEFINE_VOID_STATIC(5) JGW_DEFINE_VOID_STATIC(6) JGW_DEFINE_VOID_STATIC(7)
JGW_DEFINE_VOID_STATIC(8) JGW_DEFINE_VOID_STATIC(9) JGW_DEFINE_VOID_STATIC(10) JGW_DEFINE_VOID_STATIC(11)
JGW_DEFINE_VOID_STATIC(12) JGW_DEFINE_VOID_STATIC(13) JGW_DEFINE_VOID_STATIC(14) JGW_DEFINE_VOID_STATIC(15)

JGW_DEFINE_VOID_INSTANCE(0) JGW_DEFINE_VOID_INSTANCE(1) JGW_DEFINE_VOID_INSTANCE(2) JGW_DEFINE_VOID_INSTANCE(3)
JGW_DEFINE_VOID_INSTANCE(4) JGW_DEFINE_VOID_INSTANCE(5) JGW_DEFINE_VOID_INSTANCE(6) JGW_DEFINE_VOID_INSTANCE(7)
JGW_DEFINE_VOID_INSTANCE(8) JGW_DEFINE_VOID_INSTANCE(9) JGW_DEFINE_VOID_INSTANCE(10) JGW_DEFINE_VOID_INSTANCE(11)
JGW_DEFINE_VOID_INSTANCE(12) JGW_DEFINE_VOID_INSTANCE(13) JGW_DEFINE_VOID_INSTANCE(14) JGW_DEFINE_VOID_INSTANCE(15)

JGW_DEFINE_INT_STATIC(0) JGW_DEFINE_INT_STATIC(1) JGW_DEFINE_INT_STATIC(2) JGW_DEFINE_INT_STATIC(3)
JGW_DEFINE_INT_STATIC(4) JGW_DEFINE_INT_STATIC(5) JGW_DEFINE_INT_STATIC(6) JGW_DEFINE_INT_STATIC(7)
JGW_DEFINE_INT_STATIC(8) JGW_DEFINE_INT_STATIC(9) JGW_DEFINE_INT_STATIC(10) JGW_DEFINE_INT_STATIC(11)
JGW_DEFINE_INT_STATIC(12) JGW_DEFINE_INT_STATIC(13) JGW_DEFINE_INT_STATIC(14) JGW_DEFINE_INT_STATIC(15)

static void *jgw_void_static_fns[COREJ2_JGW_MAX_NATIVE_SLOTS] = {
    jgw_native_void_static_0, jgw_native_void_static_1, jgw_native_void_static_2, jgw_native_void_static_3,
    jgw_native_void_static_4, jgw_native_void_static_5, jgw_native_void_static_6, jgw_native_void_static_7,
    jgw_native_void_static_8, jgw_native_void_static_9, jgw_native_void_static_10, jgw_native_void_static_11,
    jgw_native_void_static_12, jgw_native_void_static_13, jgw_native_void_static_14, jgw_native_void_static_15
};

static void *jgw_void_instance_fns[COREJ2_JGW_MAX_NATIVE_SLOTS] = {
    jgw_native_void_instance_0, jgw_native_void_instance_1, jgw_native_void_instance_2, jgw_native_void_instance_3,
    jgw_native_void_instance_4, jgw_native_void_instance_5, jgw_native_void_instance_6, jgw_native_void_instance_7,
    jgw_native_void_instance_8, jgw_native_void_instance_9, jgw_native_void_instance_10, jgw_native_void_instance_11,
    jgw_native_void_instance_12, jgw_native_void_instance_13, jgw_native_void_instance_14, jgw_native_void_instance_15
};

static void *jgw_int_static_fns[COREJ2_JGW_MAX_NATIVE_SLOTS] = {
    jgw_native_int_static_0, jgw_native_int_static_1, jgw_native_int_static_2, jgw_native_int_static_3,
    jgw_native_int_static_4, jgw_native_int_static_5, jgw_native_int_static_6, jgw_native_int_static_7,
    jgw_native_int_static_8, jgw_native_int_static_9, jgw_native_int_static_10, jgw_native_int_static_11,
    jgw_native_int_static_12, jgw_native_int_static_13, jgw_native_int_static_14, jgw_native_int_static_15
};

static void *jgw_select_fn(const char *signature, int is_static, int slot) {
    if (signature == NULL) {
        return NULL;
    }
    if (strcmp(signature, "()V") == 0) {
        return is_static ? jgw_void_static_fns[slot] : jgw_void_instance_fns[slot];
    }
    if (is_static && strcmp(signature, "(I)V") == 0) {
        return jgw_int_static_fns[slot];
    }
    return NULL;
}

int corej2_jni_gateway_register_native(
    uint64_t class_id,
    const char *method_name,
    const char *signature,
    int32_t is_static,
    uint64_t token
) {
    if (method_name == NULL || signature == NULL) {
        return COREJ2_JGW_TYPE;
    }

    int slot = -1;
    pthread_mutex_lock(&g_cache_lock);
    for (int i = 0; i < COREJ2_JGW_MAX_NATIVE_SLOTS; i++) {
        if (!g_native_regs[i].in_use) {
            slot = i;
            break;
        }
    }
    CoreJ2JgwClassSlot *class_slot = jgw_find_class_slot_by_id(class_id);
    jclass cls = class_slot != NULL ? class_slot->global : NULL;
    pthread_mutex_unlock(&g_cache_lock);

    if (slot < 0) {
        return COREJ2_JGW_LOCAL_FRAME;
    }
    if (cls == NULL) {
        return COREJ2_JGW_CLASS;
    }

    void *fn = jgw_select_fn(signature, is_static ? 1 : 0, slot);
    if (fn == NULL) {
        return COREJ2_JGW_TYPE;
    }

    JavaVM *vm = (JavaVM *)corej2_embedded_jvm_javavm();
    if (vm == NULL) {
        return COREJ2_JGW_NO_VM;
    }
    JNIEnv *env = NULL;
    int did_attach = 0;
    int attach_rc = jgw_attach(vm, &env, &did_attach);
    if (attach_rc != COREJ2_JGW_OK) {
        return attach_rc;
    }

    JNINativeMethod method;
    memset(&method, 0, sizeof(method));
    method.name = (char *)method_name;
    method.signature = (char *)signature;
    method.fnPtr = fn;

    jint rc = (*env)->RegisterNatives(env, cls, &method, 1);
    if (rc != 0) {
        jgw_clear_exception(env);
        if (did_attach) {
            (*vm)->DetachCurrentThread(vm);
        }
        return COREJ2_JGW_METHOD;
    }

    pthread_mutex_lock(&g_cache_lock);
    g_native_regs[slot].in_use = 1;
    g_native_regs[slot].token = token;
    g_native_regs[slot].class_id = class_id;
    g_native_regs[slot].is_static = is_static ? 1 : 0;
    g_native_regs[slot].slot = slot;
    snprintf(g_native_regs[slot].name, sizeof(g_native_regs[slot].name), "%s", method_name);
    snprintf(g_native_regs[slot].signature, sizeof(g_native_regs[slot].signature), "%s", signature);
    pthread_mutex_unlock(&g_cache_lock);

    if (did_attach) {
        (*vm)->DetachCurrentThread(vm);
    }
    return COREJ2_JGW_OK;
}

int corej2_jni_gateway_unregister_native(uint64_t token) {
    uint64_t class_id = 0;
    int found = 0;

    pthread_mutex_lock(&g_cache_lock);
    for (int i = 0; i < COREJ2_JGW_MAX_NATIVE_SLOTS; i++) {
        if (g_native_regs[i].in_use && g_native_regs[i].token == token) {
            class_id = g_native_regs[i].class_id;
            memset(&g_native_regs[i], 0, sizeof(g_native_regs[i]));
            found = 1;
            break;
        }
    }
    CoreJ2JgwClassSlot *class_slot = found ? jgw_find_class_slot_by_id(class_id) : NULL;
    jclass cls = class_slot != NULL ? class_slot->global : NULL;
    pthread_mutex_unlock(&g_cache_lock);

    if (!found) {
        return COREJ2_JGW_REG;
    }

    JavaVM *vm = (JavaVM *)corej2_embedded_jvm_javavm();
    JNIEnv *env = NULL;
    int did_attach = 0;
    if (vm != NULL && cls != NULL && jgw_attach(vm, &env, &did_attach) == COREJ2_JGW_OK) {
        (void)(*env)->UnregisterNatives(env, cls);
        jgw_clear_exception(env);
        if (did_attach) {
            (*vm)->DetachCurrentThread(vm);
        }
    }
    return COREJ2_JGW_OK;
}

void corej2_jni_gateway_unregister_all_natives(void) {
    uint64_t class_ids[COREJ2_JGW_MAX_NATIVE_SLOTS];
    int class_count = 0;

    pthread_mutex_lock(&g_cache_lock);
    for (int i = 0; i < COREJ2_JGW_MAX_NATIVE_SLOTS; i++) {
        if (!g_native_regs[i].in_use) {
            continue;
        }
        int seen = 0;
        for (int j = 0; j < class_count; j++) {
            if (class_ids[j] == g_native_regs[i].class_id) {
                seen = 1;
                break;
            }
        }
        if (!seen && class_count < COREJ2_JGW_MAX_NATIVE_SLOTS) {
            class_ids[class_count++] = g_native_regs[i].class_id;
        }
        memset(&g_native_regs[i], 0, sizeof(g_native_regs[i]));
    }
    pthread_mutex_unlock(&g_cache_lock);

    JavaVM *vm = (JavaVM *)corej2_embedded_jvm_javavm();
    JNIEnv *env = NULL;
    int did_attach = 0;
    if (vm != NULL && jgw_attach(vm, &env, &did_attach) == COREJ2_JGW_OK) {
        for (int i = 0; i < class_count; i++) {
            pthread_mutex_lock(&g_cache_lock);
            CoreJ2JgwClassSlot *class_slot = jgw_find_class_slot_by_id(class_ids[i]);
            jclass cls = class_slot != NULL ? class_slot->global : NULL;
            pthread_mutex_unlock(&g_cache_lock);
            if (cls != NULL) {
                (void)(*env)->UnregisterNatives(env, cls);
                jgw_clear_exception(env);
            }
        }
        if (did_attach) {
            (*vm)->DetachCurrentThread(vm);
        }
    }
}
