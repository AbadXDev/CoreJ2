package javax.microedition.m3g;

import java.util.Vector;

/** CoreJ2 overlay — Group with initialized child list. */
public class Group extends Node {
    private final Vector nodes = new Vector();

    public Group() { setRenderingEnable(true); }

    public void addChild(Node child) {
        if (child != null && !nodes.contains(child)) {
            nodes.addElement(child);
        }
    }

    public Node getChild(int index) { return (Node) nodes.elementAt(index); }
    public int getChildCount() { return nodes.size(); }
    public boolean pick(int scope, float x, float y, Camera camera, RayIntersection ri) { return false; }
    public boolean pick(int scope, float ox, float oy, float oz, float dx, float dy, float dz, RayIntersection ri) { return false; }
    public void removeChild(Node child) { nodes.removeElement(child); }
}
