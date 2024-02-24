package io.github.jwharm.javagi.gir;

import java.util.List;
import java.util.Map;

public interface Node {
    Namespace namespace();
    Node parent();
    void setParent(Node parent);

    List<Node> children();
    Map<String, String> attributes();

    String attr(String key);
    int attrInt(String key);
    boolean attrBool(String key, boolean defaultValue);

    <T extends Node> T withAttribute(String attrName, String newValue);
    <T extends Node> T withChildren(GirElement... newChildren);
    <T extends Node> T withChildren(List<Node> newChildren);
}
