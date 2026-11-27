package org.recompile.mobile;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

/**
 * Clean-room MIDlet suite loader for CoreJ2.
 *
 * <p>Observable behavior (MIDP suite bring-up + resource routing):
 * <ul>
 *   <li>Parse {@code META-INF/MANIFEST.MF} for {@code MIDlet-1} and app properties</li>
 *   <li>Instantiate the MIDlet, optionally invoke OEM {@code initApp()}, then {@code startApp()}</li>
 *   <li>Rewrite MIDlet bytecode so {@code Class.getResourceAsStream} resolves through
 *       the suite JAR (via {@link Mobile#getResourceAsStream})</li>
 * </ul>
 *
 * <p>Uses ObjectWeb ASM (BSD) for the rewrite. Not derived from FreeJ2ME source.
 */
public class MIDletLoader extends URLClassLoader {

    public String name;
    public String icon;
    public String suitename;

    private String className;
    private Class<?> mainClass;
    private MIDlet mainInst;
    private final HashMap<String, String> properties = new HashMap<String, String>(32);

    public MIDletLoader(URL[] urls) {
        super(urls);
        installCLDCProperties();
        try {
            loadManifest();
            properties.put("microedition.platform", "j2me");
            properties.put("microedition.profiles", "MIDP-2.0");
            properties.put("microedition.configuration", "CLDC-1.0");
            properties.put("microedition.locale", "en-US");
            properties.put("microedition.encoding", "file.encoding");
        } catch (Exception e) {
            System.out.println("Can't Read Manifest!");
        }
    }

    private static void installCLDCProperties() {
        try {
            System.setProperty("microedition.platform", "j2me");
            System.setProperty("microedition.profiles", "MIDP-2.0");
            System.setProperty("microedition.configuration", "CLDC-1.0");
            System.setProperty("microedition.locale", "en-US");
            System.setProperty("microedition.encoding", "file.encoding");
        } catch (Exception e) {
            System.out.println("Can't add CLDC System Properties");
        }
    }

    /** Stops the running MIDlet so another JAR can start on the same JVM. */
    public void destroyCurrent() {
        if (mainInst == null) {
            return;
        }
        System.out.println("corej2-midlet: destroyCurrent begin");
        System.out.flush();
        try {
            Method destroy = findDeclaredMethod(mainInst.getClass(), "destroyApp", boolean.class);
            if (destroy != null) {
                destroy.setAccessible(true);
                destroy.invoke(mainInst, Boolean.TRUE);
                System.out.println("corej2-midlet: destroyApp OK");
            }
        } catch (Throwable t) {
            System.out.println("corej2-midlet: destroyApp failed " + t);
            t.printStackTrace();
        }
        mainInst = null;
        System.out.println("corej2-midlet: destroyCurrent done");
        System.out.flush();
    }

    public void start() throws MIDletStateChangeException {
        System.out.println("corej2-midlet: start begin class=" + className);
        System.out.flush();

        try {
            mainClass = loadClass(className);
            if (mainClass == null) {
                throw new ClassNotFoundException("loadClass returned null for " + className);
            }
            System.out.println("corej2-midlet: class loaded " + mainClass.getName());
            System.out.flush();

            Constructor<?> constructor = mainClass.getConstructor();
            constructor.setAccessible(true);
            MIDlet.initAppProperties(properties);
            mainInst = (MIDlet) constructor.newInstance();
            System.out.println("corej2-midlet: constructed");
            System.out.flush();
        } catch (Exception e) {
            System.out.println("Problem Constructing " + name + " class: " + className);
            System.out.println("Reason: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Problem Constructing " + className, e);
        }

        // Some OEM/Gameloft MIDlets expose initApp() before startApp().
        try {
            Method init = findDeclaredMethodUntil(mainClass, MIDlet.class, "initApp");
            if (init != null) {
                init.setAccessible(true);
                System.out.println("corej2-midlet: invoking initApp");
                System.out.flush();
                init.invoke(mainInst);
                System.out.println("corej2-midlet: initApp OK");
                System.out.flush();
            }
        } catch (Exception e) {
            System.out.println("corej2-midlet: initApp failed");
            e.printStackTrace();
            throw new RuntimeException("initApp failed", e);
        }

        Method start;
        try {
            Class<?> walk = mainClass;
            start = null;
            while (start == null) {
                try {
                    start = walk.getDeclaredMethod("startApp");
                    start.setAccessible(true);
                } catch (NoSuchMethodException e) {
                    walk = walk.getSuperclass();
                    if (walk == null || walk == MIDlet.class) {
                        throw e;
                    }
                    walk = loadClass(walk.getName(), true);
                }
            }
        } catch (Exception e) {
            System.out.println("Can't Find startApp Method");
            e.printStackTrace();
            throw new RuntimeException("Can't Find startApp Method", e);
        }

        try {
            start.invoke(mainInst);
            System.out.println("START_APP_OK");
            System.out.flush();
        } catch (Exception e) {
            System.out.println("corej2-midlet: startApp failed");
            e.printStackTrace();
            throw new RuntimeException("startApp failed", e);
        }
    }

    private void loadManifest() {
        String[] candidates = {
            "META-INF/MANIFEST.MF",
            "meta-inf/MANIFEST.MF",
            "META-INF/manifest.fm",
            "meta-inf/manifest.fm"
        };
        URL url = null;
        for (int i = 0; i < candidates.length; i++) {
            url = findResource(candidates[i]);
            if (url != null) {
                break;
            }
        }
        if (url == null) {
            return;
        }

        try {
            InputStream is = url.openStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            ArrayList<String> lines = new ArrayList<String>();
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith(" ") && !lines.isEmpty()) {
                    line = lines.get(lines.size() - 1) + line.trim();
                    lines.remove(lines.size() - 1);
                }
                lines.add(line);
            }
            br.close();

            for (int i = 0; i < lines.size(); i++) {
                line = lines.get(i);
                if (line.startsWith("MIDlet-1:")) {
                    System.out.println(line);
                    String body = line.substring(9);
                    String[] parts = body.split(",");
                    if (parts.length == 3) {
                        name = parts[0].trim();
                        icon = parts[1].trim();
                        className = parts[2].trim();
                        suitename = name;
                    }
                }
                int split = line.indexOf(':');
                if (split > 0) {
                    String key = line.substring(0, split).trim();
                    String value = line.substring(split + 1).trim();
                    properties.put(key, value);
                }
            }
            if (suitename != null) {
                suitename = suitename.replace(":", "");
            }
        } catch (Exception e) {
            System.out.println("Can't Read Jar Manifest!");
            e.printStackTrace();
        }
    }

    public InputStream getResourceAsStream(String resource) {
        try {
            URL url = getResource(resource);
            if (url == null) {
                return super.getResourceAsStream(resource);
            }
            return readFully(url.openStream());
        } catch (Exception e) {
            System.out.println(resource + " Not Found");
            return super.getResourceAsStream(resource);
        }
    }

    public URL getResource(String resource) {
        if (resource != null && resource.startsWith("/")) {
            resource = resource.substring(1);
        }
        try {
            return findResource(resource);
        } catch (Exception e) {
            System.out.println(resource + " Not Found");
            return super.getResource(resource);
        }
    }

    public InputStream getMIDletResourceAsStream(String resource) {
        URL url = getResource(resource);
        if (url == null) {
            return super.getResourceAsStream(resource);
        }
        try {
            return readFully(url.openStream());
        } catch (Exception e) {
            return super.getResourceAsStream(resource);
        }
    }

    public byte[] getMIDletResourceAsByteArray(String resource) {
        URL url = getResource(resource);
        try {
            InputStream stream = url.openStream();
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] data = new byte[4096];
            int count;
            while ((count = stream.read(data)) != -1) {
                buffer.write(data, 0, count);
            }
            stream.close();
            return buffer.toByteArray();
        } catch (Exception e) {
            System.out.println(resource + " Not Found");
            return new byte[0];
        }
    }

    public Class<?> loadClass(String name) throws ClassNotFoundException {
        if (isPlatformClass(name)) {
            return loadClass(name, true);
        }
        try {
            String resource = name.replace('.', '/') + ".class";
            InputStream stream = super.getResourceAsStream(resource);
            if (stream == null) {
                throw new ClassNotFoundException(name);
            }
            byte[] code = instrument(stream);
            return defineClass(name, code, 0, code.length);
        } catch (ClassNotFoundException e) {
            throw e;
        } catch (Exception e) {
            System.out.println("Error Adapting Class " + name);
            return null;
        }
    }

    private static boolean isPlatformClass(String name) {
        return name.startsWith("java.")
            || name.startsWith("javax.")
            || name.startsWith("com.nokia")
            || name.startsWith("com.mascotcapsule")
            || name.startsWith("com.samsung")
            || name.startsWith("sun.")
            || name.startsWith("com.siemens")
            || name.startsWith("org.recompile")
            || name.startsWith("dev.abadx.corej2")
            || name.startsWith("org.objectweb");
    }

    public InputStream getMIDletResourceAsSiemensStream(String resource) {
        URL url = getResource(resource);
        try {
            InputStream stream = url.openStream();
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] data = new byte[4096];
            int count;
            while ((count = stream.read(data)) != -1) {
                buffer.write(data, 0, count);
            }
            stream.close();
            return new SiemensInputStream(buffer.toByteArray());
        } catch (Exception e) {
            return super.getResourceAsStream(resource);
        }
    }

    private static InputStream readFully(InputStream stream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[4096];
        int count;
        while ((count = stream.read(data)) != -1) {
            buffer.write(data, 0, count);
        }
        stream.close();
        return new ByteArrayInputStream(buffer.toByteArray());
    }

    private static Method findDeclaredMethod(Class<?> type, String name, Class<?>... params) {
        Class<?> walk = type;
        while (walk != null) {
            try {
                return walk.getDeclaredMethod(name, params);
            } catch (NoSuchMethodException e) {
                walk = walk.getSuperclass();
            }
        }
        return null;
    }

    private static Method findDeclaredMethodUntil(Class<?> start, Class<?> stopExclusive, String name) {
        Class<?> walk = start;
        while (walk != null && walk != stopExclusive) {
            try {
                return walk.getDeclaredMethod(name);
            } catch (NoSuchMethodException e) {
                walk = walk.getSuperclass();
            }
        }
        return null;
    }

    private byte[] instrument(InputStream stream) throws Exception {
        ClassReader reader = new ClassReader(stream);
        ClassWriter writer = new ClassWriter(0);
        reader.accept(new ResourceRewriteVisitor(writer), 0);
        return writer.toByteArray();
    }

    /**
     * Rewrites {@code INVOKEVIRTUAL Class.getResourceAsStream} to
     * {@code INVOKESTATIC Mobile.getResourceAsStream} so suite resources
     * resolve against the MIDlet JAR under CLDC-style class loading.
     */
    private static final class ResourceRewriteVisitor extends ClassAdapter {
        ResourceRewriteVisitor(ClassVisitor visitor) {
            super(visitor);
        }

        public MethodVisitor visitMethod(
            int access, String name, String desc, String signature, String[] exceptions
        ) {
            return new ResourceRewriteMethod(super.visitMethod(access, name, desc, signature, exceptions));
        }
    }

    private static final class ResourceRewriteMethod extends MethodAdapter implements Opcodes {
        ResourceRewriteMethod(MethodVisitor visitor) {
            super(visitor);
        }

        public void visitMethodInsn(int opcode, String owner, String name, String desc) {
            if (opcode == INVOKEVIRTUAL
                && "getResourceAsStream".equals(name)
                && "java/lang/Class".equals(owner)) {
                mv.visitMethodInsn(
                    INVOKESTATIC,
                    "org/recompile/mobile/Mobile",
                    "getResourceAsStream",
                    "(Ljava/lang/Class;Ljava/lang/String;)Ljava/io/InputStream;"
                );
            } else {
                mv.visitMethodInsn(opcode, owner, name, desc);
            }
        }
    }

    /** Siemens OEM streams treat EOF as 0 instead of -1 (observable quirk). */
    private static final class SiemensInputStream extends InputStream {
        private final ByteArrayInputStream iostream;

        SiemensInputStream(byte[] data) {
            iostream = new ByteArrayInputStream(data);
        }

        public int read() {
            int t = iostream.read();
            return t == -1 ? 0 : t;
        }

        public int read(byte[] b, int off, int len) {
            int t = iostream.read(b, off, len);
            return t == -1 ? 0 : t;
        }
    }
}
