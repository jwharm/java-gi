package io.github.jwharm.javagi;

import org.objectweb.asm.*;

import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

public class CallbackGenerator implements Opcodes {

    private final static AtomicInteger count = new AtomicInteger();

    public static Class<?> generateClassWithStaticMethod(Object delegate, MethodType mt, Marshal[] marshals, String methodName) {
        String name = "JavaGIcb" + count.incrementAndGet();
        String internalName = "io/github/jwharm/javagi/" + name;
        String qualifiedName = "io.github.jwharm.javagi." + name;

        String delegateInternalName = delegate.getClass().getName().replace('.', '/');
        String delegateDescriptor = 'L' + delegateInternalName + ';';

        Method[] methods = delegate.getClass().getMethods();
        if (methods.length != 1) {
            return null;
        }
        String delegateMethodName = methods[0].getName();

        try {
            ClassWriter classWriter = new ClassWriter(0);
            FieldVisitor fieldVisitor;
            MethodVisitor methodVisitor;

            classWriter.visit(-65473, ACC_PUBLIC | ACC_SUPER, internalName, null, "java/lang/Object", null);

            classWriter.visitSource(name + ".java", null);

            {
                fieldVisitor = classWriter.visitField(ACC_PRIVATE | ACC_STATIC, "delegate", delegateDescriptor, null, null);
                fieldVisitor.visitEnd();
            }

            {
                methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
                methodVisitor.visitCode();
                Label label0 = new Label();
                methodVisitor.visitLabel(label0);
                methodVisitor.visitLineNumber(5, label0);
                methodVisitor.visitVarInsn(ALOAD, 0);
                methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
                methodVisitor.visitInsn(RETURN);
                Label label1 = new Label();
                methodVisitor.visitLabel(label1);
                methodVisitor.visitLocalVariable("this", 'L' + name + ';', null, label0, label1, 0);
                methodVisitor.visitMaxs(1, 1);
                methodVisitor.visitEnd();
            }

            {
                methodVisitor = classWriter.visitMethod(ACC_PUBLIC | ACC_STATIC, methodName, "(Ljava/lang/foreign/MemoryAddress;Ljava/lang/foreign/MemoryAddress;)I", null, null);
                methodVisitor.visitCode();
                Label label0 = new Label();
                methodVisitor.visitLabel(label0);
                methodVisitor.visitLineNumber(10, label0);
                methodVisitor.visitFieldInsn(GETSTATIC, name, "delegate", delegateDescriptor);
                methodVisitor.visitVarInsn(ALOAD, 0);
                methodVisitor.visitVarInsn(ALOAD, 1);
                methodVisitor.visitMethodInsn(INVOKEINTERFACE, delegateInternalName, delegateMethodName, "(Ljava/lang/foreign/MemoryAddress;Ljava/lang/foreign/MemoryAddress;)I", true);
                methodVisitor.visitInsn(IRETURN);
                Label label1 = new Label();
                methodVisitor.visitLabel(label1);
                methodVisitor.visitLocalVariable("a", "Ljava/lang/foreign/MemoryAddress;", null, label0, label1, 0);
                methodVisitor.visitLocalVariable("b", "Ljava/lang/foreign/MemoryAddress;", null, label0, label1, 1);
                methodVisitor.visitMaxs(3, 2);
                methodVisitor.visitEnd();
            }
            classWriter.visitEnd();

            byte[] bytes = classWriter.toByteArray();

            return new ClassLoader() {
                public Class<?> defineClass(byte[] bytes) {
                    return super.defineClass(qualifiedName, bytes, 0, bytes.length);
                }
            }.defineClass(bytes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
