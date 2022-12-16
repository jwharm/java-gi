package io.github.jwharm.javagi;

import org.objectweb.asm.*;

import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

import static org.objectweb.asm.Opcodes.*;

public class CallbackGenerator {

    private static final AtomicInteger count = new AtomicInteger();

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

    public static Class<?> generateClassWithStaticMethod(Class<?> delegateClass, MethodType mt, String methodName) {
        String name = "JavaGIcb" + count.incrementAndGet();
        String internalName = "io/github/jwharm/javagi/" + name;
        String qualifiedName = "io.github.jwharm.javagi." + name;

        String delegateInternalName = internalName(delegateClass);
        String delegateDescriptor = delegateClass.descriptorString();

        Method[] methods = delegateClass.getDeclaredMethods();
        Method method = methods[0];
        String delegateMethodName = method.getName();

        int argc = mt.parameterCount();
        if (argc != method.getParameterCount()) {
            throw new RuntimeException("Number of delegate parameters and methodtype parameters do not match");
        }

        try {
            ClassWriter classWriter = new ClassWriter(0);
            FieldVisitor fieldVisitor;
            MethodVisitor methodVisitor;

            // Class
            classWriter.visit(-65473, ACC_PUBLIC | ACC_SUPER, internalName, null, "java/lang/Object", null);
            classWriter.visitSource(name + ".java", null);

            // Field `delegate`
            {
                fieldVisitor = classWriter.visitField(ACC_PUBLIC | ACC_STATIC, "delegate", delegateDescriptor, null, null);
                fieldVisitor.visitEnd();
            }

            // Field `Marshal[] marshallers`
            {
                fieldVisitor = classWriter.visitField(ACC_PUBLIC | ACC_STATIC, "marshallers", "[Lio/github/jwharm/javagi/Marshal;", null, null);
                fieldVisitor.visitEnd();
            }

            // Constructor
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

            // Method
            {
                methodVisitor = classWriter.visitMethod(ACC_PUBLIC | ACC_STATIC, methodName, mt.descriptorString(), null, null);
                methodVisitor.visitCode();

                Class<?>[] delegateParamTypes = method.getParameterTypes();
                for (int i = 0; i < argc; i++) {

                    Class<?> ptype = delegateParamTypes[i];
                    String ptypeInternal = internalName(ptype);

                    methodVisitor.visitFieldInsn(GETSTATIC, internalName, "marshallers", "[Lio/github/jwharm/javagi/Marshal;");
                    methodVisitor.visitInsn(ICONST_0 + i);
                    methodVisitor.visitInsn(AALOAD);
                    methodVisitor.visitVarInsn(ALOAD, i);
                    methodVisitor.visitFieldInsn(GETSTATIC, "io/github/jwharm/javagi/Ownership", "NONE", "Lio/github/jwharm/javagi/Ownership;");
                    methodVisitor.visitMethodInsn(INVOKEINTERFACE, "io/github/jwharm/javagi/Marshal", "marshal", "(Ljava/lang/Object;Lio/github/jwharm/javagi/Ownership;)Ljava/lang/Object;", true);
                    methodVisitor.visitTypeInsn(CHECKCAST, ptypeInternal);
                    methodVisitor.visitVarInsn(ASTORE, argc + i);
                }

                Label label = new Label();
                methodVisitor.visitLabel(label);
                methodVisitor.visitLineNumber(30, label);
                methodVisitor.visitFieldInsn(GETSTATIC, name, "delegate", delegateDescriptor);

                // Method parameters
                for (int i = 0; i < argc; i++) {
                    String descriptor = mt.parameterType(i).descriptorString();
                    methodVisitor.visitVarInsn(getLoadOpcode(descriptor), argc + i);
                }

                // Invoke the delegate
                methodVisitor.visitMethodInsn(INVOKEINTERFACE, delegateInternalName, delegateMethodName, mt.descriptorString(), true);

                methodVisitor.visitInsn(getReturnOpcode(mt.returnType().descriptorString()));

                // Local variables
                Label label1 = new Label();
                methodVisitor.visitLabel(label1);
                for (int i = 0; i < argc; i++) {
                    methodVisitor.visitLocalVariable("arg" + i, mt.parameterType(i).descriptorString(), null, label, label1, i);
                }
                for (int i = 0; i < argc; i++) {
                    methodVisitor.visitLocalVariable("m" + i, delegateParamTypes[i].descriptorString(), null, label, label1, argc + i);
                }
                methodVisitor.visitMaxs((argc * 2) + 2, argc * 2);

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
