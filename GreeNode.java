import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;

public class GreeNode implements Serializable {
    
    int identity;
    GreeNode parent;
    ArrayList<GreeNode> children;

    public GreeNode(int i) {
        identity = i;
        parent = null;
    }
    
    public GreeNode(int i, GreeNode j) {
        identity = i;
        parent = j;
    }

    public GreeNode getParent() {
        return parent;
    }
    
    public GreeNode commonAncestor(GreeNode other) {
        /*if (this.equals(other)) {
            return this;
        } 
        GreeNode pointer = this.parent;
        GreeNode otherPointer = other.parent;
        if (pointer.equals(otherPointer) || pointer.equals(other)) {
            return pointer;
        }
        if (otherPointer.equals(this)) {
            return this;
        }*/
        HashSet<GreeNode> myAncestors = new HashSet<GreeNode>();
        myAncestors.add(this);    
        HashSet<GreeNode> otherAncestors = new HashSet<GreeNode>();
        otherAncestors.add(other);
        GreeNode pointer = this.parent;
        if (otherAncestors.contains(pointer)) {
                    return pointer;
            } 
        myAncestors.add(pointer);
        GreeNode otherPointer = other.parent;
        if (myAncestors.contains(otherPointer)) {
            return otherPointer;
        }
        otherAncestors.add(otherPointer);
        
        while(true) {
            if (pointer != null) {
                pointer = pointer.parent;
                if (otherAncestors.contains(pointer)) {
                    return pointer;
                } 
                myAncestors.add(pointer);
            }
            if (otherPointer != null) {
                otherPointer = otherPointer.parent;
                if (myAncestors.contains(otherPointer)) {
                    return otherPointer;
                }
                otherAncestors.add(otherPointer);
            }
        }
    }
        
        
    
}
