package org.javagi.regress;

import org.gnome.gi.regress.AliasedTestBoxed;
import org.gnome.gi.regress.PtrArrayAlias;
import org.gnome.gi.regress.TestBoxed;
import org.gnome.gi.regress.VaListAlias;
import org.gnome.glib.PtrArray;
import org.junit.jupiter.api.Test;

import java.lang.foreign.MemorySegment;

import static org.gnome.gi.regress.Regress.*;

public class TestAliasedType {
    @Test
    void isIntrospectableViaAlias() {
        PtrArrayAlias data = PtrArrayAlias.fromPtrArray(new PtrArray());
        introspectableViaAlias(data);
    }

    @Test
    void isNotIntrospectableViaAlias() {
        // This actually needs a `va_list`, which we don't support.
        VaListAlias ok = new VaListAlias(MemorySegment.NULL);
        notIntrospectableViaAlias(ok);
    }

    @Test
    void callerAlloc() {
        AliasedTestBoxed boxed = AliasedTestBoxed.fromTestBoxed(new TestBoxed());
        aliasedCallerAlloc(boxed);
    }
}
