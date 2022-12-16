package io.github.jwharm.javagi.generator;

import io.github.jwharm.javagi.model.*;

public abstract class PatchSet {
    public static final PatchSet EMPTY = new Empty();

    public abstract void patch(Repository repo);

    protected static void removeConstant(Repository repo, String constant) {
        if (!repo.namespace.constantList.removeIf(f -> constant.equals(f.name))) System.err.println("Did not remove " + constant + ": Not found");
    }

    protected static void removeFunction(Repository repo, String function) {
        if (!repo.namespace.functionList.removeIf(f -> function.equals(f.name))) System.err.println("Did not remove " + function + ": Not found");
    }

    protected static void removeMethod(Repository repo, String type, String method) {
        Method m = findMethod(repo, type, method);
        if (m != null) m.parent.methodList.remove(m);
        else System.err.println("Did not remove " + type + "." + method + ": Not found");
    }

    protected static void renameMethod(Repository repo, String type, String oldName, String newName) {
        Method m = findMethod(repo, type, oldName);
        if (m != null) m.name = newName;
        else System.err.println("Did not rename " + type + "." + oldName + ": Not found");
    }

    protected static void removeType(Repository repo, String type) {
        if (repo.namespace.registeredTypeMap.remove(type) == null) System.err.println("Did not remove " + type + ": Not found");
    }

    protected static Method findMethod(Repository repo, String type, String method) {
        try {
            for (Method m : repo.namespace.registeredTypeMap.get(type).methodList) {
                if (method.equals(m.name)) return m;
            }
            return null;
        } catch (NullPointerException ignored) {
            return null;
        }
    }

    protected static void removeEnumMember(Repository repo, String type, String member) {
        RegisteredType rt = repo.namespace.registeredTypeMap.get(type);
        if (rt == null) {
            System.err.println("Did not remove " + member + " in " + type + ": Enumeration not found");
            return;
        }
        if (!(rt instanceof Enumeration e)) {
            System.err.println("Did not remove " + member + " in " + type + ": Not an enumeration");
            return;
        }
        Member found = null;
        for (Member m : e.memberList) {
            if (m.name.equals(member)) {
                found = m;
                break;
            }
        }
        if (found == null) System.err.println("Did not remove " + member + " in " + e + ": Member not found");
        else e.memberList.remove(found);
    }

    private static class Empty extends PatchSet {
        @Override
        public void patch(Repository repository) {
        }
    }
}
