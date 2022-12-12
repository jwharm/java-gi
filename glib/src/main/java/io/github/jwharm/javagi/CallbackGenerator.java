package io.github.jwharm.javagi;

import org.objectweb.asm.*;

import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;

public class CallbackGenerator implements Opcodes {

    private final static AtomicInteger count = new AtomicInteger();

    private static String internalName(Class<?> cls) {
        return cls.getName().replace('.', '/');
    }

    private static int getReturnOpcode(String typeDescriptor) {
        return switch (typeDescriptor) {
            case "Z", "B", "C", "S", "I" -> Opcodes.IRETURN;
            case "J" -> Opcodes.LRETURN;
            case "F" -> Opcodes.FRETURN;
            case "D" -> Opcodes.DRETURN;
            case "V" -> Opcodes.RETURN;
            default -> Opcodes.ARETURN;
        };
    }

    private static int getLoadOpcode(String typeDescriptor) {
        return switch (typeDescriptor) {
            case "Z", "B", "C", "S", "I" -> Opcodes.ILOAD;
            case "J" -> Opcodes.LLOAD;
            case "F" -> Opcodes.FLOAD;
            case "D" -> Opcodes.DLOAD;
            default -> Opcodes.ALOAD;
        };
    }

    public static Class<?> generateClassWithStaticMethod(Class<?> delegateClass, MethodType mt, Marshal[] marshals, String methodName) {
        String name = "JavaGIcb" + count.incrementAndGet();
        String internalName = "io/github/jwharm/javagi/" + name;
        String qualifiedName = "io.github.jwharm.javagi." + name;

        String delegateInternalName = internalName(delegateClass);
        String delegateDescriptor = delegateClass.descriptorString();

        Method[] methods = delegateClass.getDeclaredMethods();
        Method method = methods[0];
        String delegateMethodName = method.getName();

        try {
            ClassWriter classWriter = new ClassWriter(0);
            FieldVisitor fieldVisitor;
            MethodVisitor methodVisitor;

            classWriter.visit(-65473, ACC_PUBLIC | ACC_SUPER, internalName, null, "java/lang/Object", null);

            classWriter.visitSource(name + ".java", null);

            {
                fieldVisitor = classWriter.visitField(ACC_PUBLIC | ACC_STATIC, "delegate", delegateDescriptor, null, null);
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
                methodVisitor = classWriter.visitMethod(ACC_PUBLIC | ACC_STATIC, methodName, mt.descriptorString(), null, null);
                methodVisitor.visitCode();
                Label label0 = new Label();
                methodVisitor.visitLabel(label0);
                methodVisitor.visitLineNumber(10, label0);
                methodVisitor.visitFieldInsn(GETSTATIC, name, "delegate", delegateDescriptor);

                for (int i = 0; i < mt.parameterCount(); i++) {
                    String descriptor = mt.parameterType(i).descriptorString();
                    methodVisitor.visitVarInsn(getLoadOpcode(descriptor), i);
                }

                methodVisitor.visitMethodInsn(INVOKEINTERFACE, delegateInternalName, delegateMethodName, mt.descriptorString(), true);

                methodVisitor.visitInsn(getReturnOpcode(mt.returnType().descriptorString()));

                Label label1 = new Label();
                methodVisitor.visitLabel(label1);

                for (int i = 0; i < mt.parameterCount(); i++) {
                    methodVisitor.visitLocalVariable("arg" + i, mt.parameterType(i).descriptorString(), null, label0, label1, i);
                }

                methodVisitor.visitMaxs(mt.parameterCount() + 1, mt.parameterCount());
                methodVisitor.visitEnd();
            }
            classWriter.visitEnd();

            byte[] bytes = classWriter.toByteArray();

            Files.write(Paths.get("/home/jw/Documents/Generated.class"), bytes);

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
