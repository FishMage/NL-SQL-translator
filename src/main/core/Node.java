package main.core;

import java.util.Comparator;

public class Node {
    public String val;
    public NodeType type = NodeType.UNKNOW;

    double score = 1.0;//for comparing word similarity
    
    public Node() {
    }
    
    public Node(String val, NodeType type) {
        this.val = val;
        this.type = type;
    }
    
    public Node(String val, NodeType type, double score){
        this(val, type);
        this.score = score;
    }
    
    protected Node(Node node) {
        this.val = node.val;
        this.type = node.type;
        this.score = node.score;
    }

    public Node clone() {
        return new Node(this);
    }

    //for comparing scores in NodeMapper
    public static class sortByScore implements Comparator<Node> {
        @Override
        public int compare(Node a, Node b) {
            if (a.score < b.score) { return 1; }
            else if (a.score > b.score) { return -1; }
            else { return 0; }
        }
    }
    
    //for implicit node inserter
    public boolean sameSchema(Node rhs){
        if(val==null || rhs.val==null){
            return false;
        }
        int idxDot = val.indexOf('.');
        int idxDotRHS = rhs.val.indexOf('.');
        idxDot = idxDot==-1?val.length():idxDot;
        idxDotRHS = idxDotRHS==-1?rhs.val.length():idxDotRHS;
        if(val.substring(0, idxDot-1).equals(rhs.val.substring(0, idxDotRHS-1))){
            return true;
        }
        return false;
    }
    
    @Override
    public String toString() {
        return String.format("val: %s, type: %s, score: %f", val, type, score);
    }
}
