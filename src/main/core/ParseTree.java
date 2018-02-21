package main.core;

import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.trees.TypedDependency;
import javafx.util.Pair;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * @author sinkerine
 */
public class ParseTree implements Iterable<TreeNode>{
    TreeNode root;

    // For tree adjustor
    int edit = 0;
    double score = 0;

    public ParseTree() {
    }

    public ParseTree(String text, NLParser parser) {
        // Parse a sentence
        List<TaggedWord> taggedWords = parser.tag(text);
        Collection<TypedDependency> dependencies = parser.genDependencies(taggedWords);

        /*
         * now the sentence is a List of words in the NL,
         * for example "It is a dog." => [It, is, a, dog, .]
         */

        /*
         * tagged is a list of words with its POS tag
         * for example [It/PRP, is/VBZ, a/DT, dog/NN, ./.]
        */

        // Build ParseTree
        int n = taggedWords.size();
        Map<Integer, TreeNode> treeNodes = IntStream.range(0, n + 1).boxed()
                .map(i -> i == 0 ? new TreeNode(0, "ROOT", "ROOT", new Node("ROOT", NodeType.ROOT))
                            : new TreeNode(i, taggedWords.get(i - 1).word(), taggedWords.get(i - 1).tag()))
                .collect(Collectors.toMap(tn -> tn.getId(), Function.identity()));

        List<List<Integer>> edges = dependencies.stream()
                .map(dep -> new ArrayList<Integer>(Arrays.asList(dep.gov().index(), dep.dep().index())))
                .collect(Collectors.toList());
        root = buildTree(treeNodes, edges);
    }

    public ParseTree(Map<Integer, TreeNode> treeNodes, List<List<Integer>> edges) {
        root = buildTree(treeNodes, edges);
    }

    private TreeNode buildTree(Map<Integer, TreeNode> treeNodes, List<List<Integer>> edges) {
        edges.stream().forEach(edge -> {
            int from = edge.get(0), to = edge.get(1);
            treeNodes.get(to).parent = treeNodes.get(from);
            treeNodes.get(from).children.add(treeNodes.get(to));
        });
        return treeNodes.get(0);
    }

    protected ParseTree(ParseTree tree) {
        root = copyDfs(tree.root, null);
        edit = tree.edit;
        score = tree.score;
    }

    private TreeNode copyDfs(TreeNode tn, TreeNode newParent) {
        TreeNode tmp = tn.clone();
        tmp.parent = newParent;
        for (TreeNode child : tn.children) {
            tmp.children.add(copyDfs(child, tmp));
        }
        return tmp;
    }

    public ParseTree clone() {
        return new ParseTree(this);
    }

    // Serialization

    /**
     * generate hashcode by preorder traversal with delimiters
     * @return hash code of parse tree
     */

    @Override
    public int hashCode() {
        return serializeAsString().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == null) return this == obj;
        if (obj instanceof ParseTree) {
            return hashCode() == obj.hashCode();
        }
        return super.equals(obj);
    }

    /**
     * serialize parse tree as string using preorder traversal with delimiters
     * @return serialized string
     */
    public String serializeAsString() {
        StringBuilder sb = new StringBuilder();
        serializeAsString(root, sb);
        return sb.toString();
    }

    private void serializeAsString(TreeNode tn, StringBuilder sb) {
        sb.append(String.valueOf(tn.id));
        sb.append(',');
        for (TreeNode child : tn.children) {
            serializeAsString(child, sb);
        }
        sb.append(')');
    }

    // Reads

    /**
     * Iterator(preorder)
     */

    private class ParseTreeIterator implements Iterator<TreeNode> {
        Stack<TreeNode> st;
        ParseTreeIterator() {
            st = new Stack<TreeNode>();
            st.push(root);
        }

        @Override
        public boolean hasNext() {
            return !st.isEmpty();
        }

        @Override
        public TreeNode next() {
            TreeNode node = st.pop();
            List<TreeNode> children = node.children;
            for(int i = children.size() - 1; i >= 0; i--) {
                st.push(children.get(i));
            }
            return node;
        }
    }

    @Override
    public ParseTreeIterator iterator() {
        return new ParseTreeIterator();
    }

    /*
     * get sorted tree nodes as a list(preorder)
     */
    public List<TreeNode> getSortedTreeNodeList() {
        List<TreeNode> res = new ArrayList<>();
        for (TreeNode node : this) {
            res.add(node);
        }
        res.sort((tn1, tn2) -> tn1.id - tn2.id);
        return res;
    }

    /**
     * get edges by bfs
     */
    public List<List<Integer>> getEdges() {
        List<List<Integer>> edges = new ArrayList<>();
        Queue<TreeNode> q = new LinkedList<>();
        q.offer(root);

        while(!q.isEmpty()) {
            TreeNode treeNode = q.poll();
            for(TreeNode child: treeNode.children) {
                edges.add(Arrays.asList(treeNode.id, child.id));
                q.offer(child);
            }
        }

        return edges;
    }

    /**
     * convert parse tree to String by BFS
     * @return
     */
    public String toString() {
        int d = -1;
        Queue<TreeNode> q = new LinkedList<>();
        StringBuilder sb = new StringBuilder();

        q.offer(root);
        while (!q.isEmpty()) {
            d++;
            int sz = q.size();
            List<TreeNode> curLevel = new ArrayList<>();
            sb.append(String.format("level %d: { ", d));
            while (sz-- > 0) {
                TreeNode tn = q.poll();
                sb.append(String.format("%d: [", tn.id));
                for (TreeNode child : tn.children) {
                    q.offer(child);
                    sb.append(String.format("%d,", child.id));
                }
                sb.append("], ");
            }
            sb.append("}, ");
        }
        return sb.toString();
    }

    // Updates
    public void updateNodeMappings(Map<Integer, Node> treeNodeId2Node) {
        updateNodeMappings(root, treeNodeId2Node);
    }

    private void updateNodeMappings(TreeNode treeNode, Map<Integer, Node> treeNodeId2Node) {
        if(treeNode == null) return;
        treeNode.node = treeNodeId2Node.getOrDefault(treeNode.id, new Node(treeNode.node.val, NodeType.UNKNOW));
        treeNode.children.stream().forEach(child -> updateNodeMappings(child, treeNodeId2Node));
    }

    public void removeUnKnownNodes() {
        removeUnKnownNodes(root);
    }

    /*
    Remove treenodes with UNKNOWN Mappings
     */
    private void removeUnKnownNodes(TreeNode treeNode) {
        List<TreeNode> children = new ArrayList<>(treeNode.children);
        children.stream().forEach(tn -> removeUnKnownNodes(tn));

        if (treeNode != root && treeNode.node.type == NodeType.UNKNOW) {
            treeNode.parent.children.remove(treeNode);
            treeNode.children.stream().forEach(child -> {
                treeNode.parent.children.add(child);
                child.parent = treeNode.parent;
            });
        }
    }

    //implicit node
    public void insertImplicitNode(){
    	ImplicitNodeInserter.insertImplicitNode(this);
    }
    
    //translateToSQL
    public SQLGenerator.SQLFormat translateToSQL(SchemaGraph schema){
        SQLGenerator generator = new SQLGenerator(root, schema);
        return generator.query;
    }
    
    // Test
    public static void main(String[] args) {
        ParseTree parseTree = new ParseTree("I will explain ", new NLParser());
    }
}
