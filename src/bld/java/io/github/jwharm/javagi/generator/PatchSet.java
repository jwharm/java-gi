package io.github.jwharm.javagi.generator;

import io.github.jwharm.javagi.model.*;

@FunctionalInterface
public interface PatchSet {
    PatchSet EMPTY = new Empty();

    void patch(Repository repo);

    default void removeConstant(Repository repo, String constant) {
        if (!repo.namespace.constantList.removeIf(f -> constant.equals(f.name))) System.err.println("Did not remove " + constant + ": Not found");
    }

    default void removeFunction(Repository repo, String function) {
        if (!repo.namespace.functionList.removeIf(f -> function.equals(f.name))) System.err.println("Did not remove " + function + ": Not found");
    }

    default void removeMethod(Repository repo, String type, String method) {
        Method m = findMethod(repo, type, method);
        if (m != null) m.parent.methodList.remove(m);
        else System.out.println("Did not remove " + type + "." + method + ": Not found");
    }

    default void renameMethod(Repository repo, String type, String oldName, String newName) {
        Method m = findMethod(repo, type, oldName);
        if (m != null) m.name = newName;
        else System.out.println("Did not rename " + type + "." + oldName + ": Not found");
    }

    default void removeVirtualMethod(Repository repo, String type, String virtualMethod) {
        VirtualMethod vm = findVirtualMethod(repo, type, virtualMethod);
        if (vm != null) vm.parent.virtualMethodList.remove(vm);
        else System.out.println("Did not remove " + type + "." + virtualMethod + ": Not found");
    }

    default void removeProperty(Repository repo, String type, String property) {
        Property p = findProperty(repo, type, property);
        if (p != null) p.parent.propertyList.remove(p);
        else System.out.println("Did not remove " + type + "." + property + ": Not found");
    }

    default void removeType(Repository repo, String type) {
        if (repo.namespace.registeredTypeMap.remove(type) == null) 
            System.out.println("Did not remove " + type + ": Not found");
    }

    default void setReturnType(Repository repo, String type, String name, String typeName, String typeCType, String defaultReturnValue, String doc) {
        Method m = findVirtualMethod(repo, type, name);
        if (m == null)
            m = findMethod(repo, type, name);
        if (m != null) {
            ReturnValue rv = m.returnValue;
            rv.type = new Type(rv, typeName, typeCType);
            rv.overrideReturnValue = defaultReturnValue;
            if (doc != null) {
                rv.doc = new Doc(rv, "1");
                rv.doc.contents = doc;
            }
        } else
            System.out.println("Did not change return type of " + type + "." + name + ": Not found");
    }

    default void setReturnFloating(CallableType ct) {
        try {
            ReturnValue rv = ct.getReturnValue();
            rv.returnsFloatingReference = true;
        } catch (NullPointerException ignored) {
            String name = (ct instanceof GirElement elem) ? elem.name : "[unknown]";
            System.out.println("Did not flag return type as floating reference in " + name + ": Not found");
        }
    }

    default Constructor findConstructor(Repository repo, String type, String constructor) {
        try {
            for (Constructor c : repo.namespace.registeredTypeMap.get(type).constructorList) {
                if (constructor.equals(c.name)) return c;
            }
            return null;
        } catch (NullPointerException ignored) {
            return null;
        }
    }

    default Function findFunction(Repository repo, String type, String function) {
        try {
            if (type == null)
                for (Function f : repo.namespace.functionList) {
                    if (function.equals(f.name)) return f;
                }
            else
                for (Function f : repo.namespace.registeredTypeMap.get(type).functionList) {
                    if (function.equals(f.name)) return f;
                }
            return null;
        } catch (NullPointerException ignored) {
            return null;
        }
    }

    default Method findMethod(Repository repo, String type, String method) {
        try {
            for (Method m : repo.namespace.registeredTypeMap.get(type).methodList) {
                if (method.equals(m.name)) return m;
            }
            return null;
        } catch (NullPointerException ignored) {
            return null;
        }
    }

    default VirtualMethod findVirtualMethod(Repository repo, String type, String virtualMethod) {
        try {
            for (VirtualMethod vm : repo.namespace.registeredTypeMap.get(type).virtualMethodList) {
                if (virtualMethod.equals(vm.name)) return vm;
            }
            return null;
        } catch (NullPointerException ignored) {
            return null;
        }
    }

    default Property findProperty(Repository repo, String type, String property) {
        try {
            for (Property p : repo.namespace.registeredTypeMap.get(type).propertyList) {
                if (property.equals(p.propertyName)) return p;
            }
            return null;
        } catch (NullPointerException ignored) {
            return null;
        }
    }

    default void removeEnumMember(Repository repo, String type, String member) {
        RegisteredType rt = repo.namespace.registeredTypeMap.get(type);
        if (rt == null) {
            System.out.println("Did not remove " + member + " in " + type + ": Enumeration not found");
            return;
        }
        if (!(rt instanceof Enumeration e)) {
            System.out.println("Did not remove " + member + " in " + type + ": Not an enumeration");
            return;
        }
        Member found = null;
        for (Member m : e.memberList) {
            if (m.name.equals(member)) {
                found = m;
                break;
            }
        }
        if (found == null) System.out.println("Did not remove " + member + " in " + e + ": Member not found");
        else e.memberList.remove(found);
    }

    default void makeGeneric(Repository repo, String type) {
        RegisteredType inst = repo.namespace.registeredTypeMap.get(type);
        if (inst != null) {
            inst.generic = true;
            for (Method m : inst.methodList) {
                if (m.parameters != null) {
                    for (Parameter p : m.parameters.parameterList) {
                        if (p.type != null && "org.gnome.gobject.GObject".equals(p.type.qualifiedJavaType)) {
                            p.type.qualifiedJavaType = "T";
                        }
                    }
                }
                if (m.returnValue != null) {
                    Type returnType = m.returnValue.type;
                    if (returnType != null && "org.gnome.gobject.GObject".equals(returnType.qualifiedJavaType)) {
                        returnType.qualifiedJavaType = "T";
                    }
                }
            }
        } else {
            System.out.println("Did not make " + type + " generic: Type not found");
        }
    }

    default void makeAutoCloseable(Repository repo, String type) {
        RegisteredType inst = repo.namespace.registeredTypeMap.get(type);
        if (inst == null) {
            System.out.println("Did not make " + type + " AutoCloseable: Type not found");
            return;
        }
        inst.autoCloseable = true;
    }

    default void inject(Repository repo, String type, String code) {
        RegisteredType inst = repo.namespace.registeredTypeMap.get(type);
        if (inst == null) {
            System.out.println("Did not inject code into " + type + ": Type not found");
            return;
        }
        if (inst.injected == null) inst.injected = code;
        else inst.injected += code;
    }

    class Empty implements PatchSet {
        @Override
        public void patch(Repository repository) {
        }
    }
}
