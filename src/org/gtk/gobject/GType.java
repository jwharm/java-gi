package org.gtk.gobject;

public class GType {

    public static GType FUNDAMENTAL_MAX      = new GType(org.gtk.interop.jextract.gtk_h.G_TYPE_FUNDAMENTAL_MAX());
    public static GType INVALID              = new GType(org.gtk.interop.jextract.gtk_h.G_TYPE_INVALID());
    public static GType NONE                 = new GType(org.gtk.interop.jextract.gtk_h.G_TYPE_NONE());
    public static GType INTERFACE            = new GType(org.gtk.interop.jextract.gtk_h.G_TYPE_INTERFACE());
    public static GType CHAR                 = new GType(org.gtk.interop.jextract.gtk_h.G_TYPE_CHAR());
    public static GType UCHAR                = new GType(org.gtk.interop.jextract.gtk_h.G_TYPE_UCHAR());
    public static GType BOOLEAN              = new GType(org.gtk.interop.jextract.gtk_h.G_TYPE_BOOLEAN());
    public static GType INT                  = new GType(org.gtk.interop.jextract.gtk_h.G_TYPE_INT());
    public static GType UINT                 = new GType(org.gtk.interop.jextract.gtk_h.G_TYPE_UINT());
    public static GType LONG                 = new GType(org.gtk.interop.jextract.gtk_h.G_TYPE_LONG());
    public static GType ULONG                = new GType(org.gtk.interop.jextract.gtk_h.G_TYPE_ULONG());
    public static GType INT64                = new GType(org.gtk.interop.jextract.gtk_h.G_TYPE_INT64());
    public static GType UINT64               = new GType(org.gtk.interop.jextract.gtk_h.G_TYPE_UINT64());
    public static GType ENUM                 = new GType(org.gtk.interop.jextract.gtk_h.G_TYPE_ENUM());
    public static GType FLAGS                = new GType(org.gtk.interop.jextract.gtk_h.G_TYPE_FLAGS());
    public static GType FLOAT                = new GType(org.gtk.interop.jextract.gtk_h.G_TYPE_FLOAT());
    public static GType DOUBLE               = new GType(org.gtk.interop.jextract.gtk_h.G_TYPE_DOUBLE());
    public static GType STRING               = new GType(org.gtk.interop.jextract.gtk_h.G_TYPE_STRING());
    public static GType POINTER              = new GType(org.gtk.interop.jextract.gtk_h.G_TYPE_POINTER());
    public static GType BOXED                = new GType(org.gtk.interop.jextract.gtk_h.G_TYPE_BOXED());
    public static GType PARAM                = new GType(org.gtk.interop.jextract.gtk_h.G_TYPE_PARAM());
    public static GType OBJECT               = new GType(org.gtk.interop.jextract.gtk_h.G_TYPE_OBJECT());
    public static GType VARIANT              = new GType(org.gtk.interop.jextract.gtk_h.G_TYPE_VARIANT());
    public static GType FUNDAMENTAL_SHIFT    = new GType(org.gtk.interop.jextract.gtk_h.G_TYPE_FUNDAMENTAL_SHIFT());
    public static GType RESERVED_GLIB_FIRST  = new GType(org.gtk.interop.jextract.gtk_h.G_TYPE_RESERVED_GLIB_FIRST());
    public static GType RESERVED_GLIB_LAST   = new GType(org.gtk.interop.jextract.gtk_h.G_TYPE_RESERVED_GLIB_LAST());
    public static GType RESERVED_BSE_FIRST   = new GType(org.gtk.interop.jextract.gtk_h.G_TYPE_RESERVED_BSE_FIRST());
    public static GType RESERVED_BSE_LAST    = new GType(org.gtk.interop.jextract.gtk_h.G_TYPE_RESERVED_BSE_LAST());
    public static GType RESERVED_USER_FIRST  = new GType(org.gtk.interop.jextract.gtk_h.G_TYPE_RESERVED_USER_FIRST());
    public static GType FLAG_RESERVED_ID_BIT = new GType(org.gtk.interop.jextract.gtk_h.G_TYPE_FLAG_RESERVED_ID_BIT());

    private long value;

    public GType(long value) {
        this.value = value;
    }

    public long getValue() {
        return value;
    }
}
