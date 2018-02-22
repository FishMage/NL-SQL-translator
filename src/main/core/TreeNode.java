package main.core;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/*
    Node of ParseTree
 */
public class TreeNode {
    // TreeNode info
    public int id;
    public String word;
    public String posTag;

    // Node info
    public Node node = new Node();

    //for syntaxChecker
    boolean isInvalid = false;
    

    // Links
    TreeNode parent;
    List<TreeNode> children = new ArrayList<>();

    public TreeNode(int id) {
        this.id = id;
    }

    public TreeNode(int id, String word, String posTag) {
        this.id = id;
        this.word = word;
        this.posTag = posTag;
    }

    public TreeNode(int id, String word, Node node) {
        this.id = id;
        this.word = word;
        this.posTag = "";
        this.node = node;
    }

    public TreeNode(int id, String word, String posTag, Node node) {
        this.id = id;
        this.word = word;
        this.posTag = posTag;
        this.node = node;
    }

    /**
     * parent and children need to be cloned manually in a tree
     * @param tn
     */
    protected TreeNode(TreeNode tn) {
        id = tn.id;
        word = tn.word;
        posTag = tn.posTag;
        node = tn.node.clone();
    }

    public TreeNode clone() {
        return new TreeNode(this);
    }

    void swap(TreeNode rhs) {
        int tid = rhs.id;
        String tword = rhs.word;
        String tpostag = rhs.posTag;
        Node tnode = rhs.node;
        rhs.id = id;
        rhs.word = word;
        rhs.posTag = posTag;
        rhs.node = node;
        id = tid;
        word = tword;
        posTag = tpostag;
        node = tnode;
    }

    public int getId() {
        return id;
    }

    //return all children in an array with current TreeNode as root
    //for implicit node inserter
    public TreeNode[] allChildrenArray(){
        List<TreeNode> tnodesList = new ArrayList<>();
        LinkedList<TreeNode> stack = new LinkedList<>();
        stack.push(this);
        while (!stack.isEmpty()) {
            TreeNode curr = stack.pop();
            tnodesList.add(curr);
            for(int i = curr.children.size()-1; i>=0; i--){//preserve order
                stack.push(curr.children.get(i));
            }
        }
        TreeNode[] tnodesArray = new TreeNode[tnodesList.size()];
        for(int i = 0; i<tnodesList.size(); i++){
            tnodesArray[i] = tnodesList.get(i);
        }
        return tnodesArray;
    }
    
    @Override
    public String toString() {
        return String.format("node %d: word: %s, posTag: %s", id, word, posTag);
    }
}
