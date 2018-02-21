package main.core;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author sinkerine
 */
public class TreeAdjustor {
    static final int maxEdit = 5;
    static final double scoreThreshold = 0.8;

    public static List<ParseTree> genParseTrees(ParseTree parseTree, int limits) {
        List<ParseTree> parseTrees = genParseTrees(parseTree);
        return parseTrees.size() <= limits ? parseTrees : parseTrees.subList(0, limits);
    }

    private static List<ParseTree> genParseTrees(ParseTree parseTree) {
        List<ParseTree> res = new ArrayList<>();
        PriorityQueue<ParseTree> pq = new PriorityQueue<>((lhs, rhs) -> Double.compare(rhs.score, lhs.score));
        Set<ParseTree> H = new HashSet<>();

        pq.add(parseTree);
        parseTree.edit = 0;

        while (!pq.isEmpty()) {
            ParseTree curTree = pq.poll();
            double curScore = SyntaxEvaluator.evaluate(curTree);
            if(curTree.edit > maxEdit) continue;
            // Filter our visited tree using hash set
            List<ParseTree> trees = nextLevel(curTree).stream().filter(tree -> !H.contains(tree)).collect(Collectors.toList());
            trees.forEach(tree -> { tree.edit++; H.add(tree); });

            /**
             * Add trees whose score is greater than the current tree into the priority queue
             * Add trees whose score is greater than the threshold into the result
             * Evaluate tree scores by valid tree nodes percentage(See SyntaxEvaluator)
             * Decide validity of a tree by tree scores as well, i.e. whether the score of current tree is greater than threshold
             */

            trees.forEach(tree -> tree.score = SyntaxEvaluator.evaluate(tree));
            pq.addAll(trees.stream()
                    .filter(tree -> tree.score >= curScore)
                    .collect(Collectors.toList()));
            res.addAll(trees.stream()
                    .filter(tree -> tree.score >= scoreThreshold)
                    .collect(Collectors.toList()));
        }

        // Sort trees first by score(DESC), then by edit(ASC)
        res.sort((t1, t2) -> {
            int scoreDiff = Double.compare(t2.score, t1.score);
            return scoreDiff != 0 ? scoreDiff : t1.edit - t2.edit;
        });
        return res;
    }

    /**
     * get trees after one step adjustment of all tree nodes
     * @param parseTree
     * @return
     */
    private static List<ParseTree> nextLevel(ParseTree parseTree) {
        return parseTree.getSortedTreeNodeList().stream()
                .flatMap(tn -> adjust(parseTree, tn).stream())
                .filter(tree -> tree.getSortedTreeNodeList().size() > 1)
                .collect(Collectors.toList());
    }

    /**
     * one step adjustment for a tree node
     * @param tree
     * @param treeNode
     * @return
     */
    private static List<ParseTree> adjust(ParseTree tree, TreeNode treeNode) {
        List<ParseTree> res = new ArrayList<>();

        // Swap current tree node with every child
        if (treeNode.parent != null) {
            for (TreeNode child : treeNode.children) {
                ParseTree clone = tree.clone();
                swapTreeNode(findInTree(clone, treeNode), findInTree(clone, child));
                res.add(clone);
            }
        }

        // Move every child up a level, here let it be the rightmost sibling.
        if (treeNode.parent != null) {
            for (TreeNode child : treeNode.children) {
                ParseTree clone = tree.clone();
                promote(findInTree(clone, child), findInTree(clone, treeNode));
                res.add(clone);
            }
        }

        // Move every sibling down a level, here let it be the rightmost child.
        if (treeNode.parent != null) {
            for (TreeNode sibling : treeNode.parent.children) {
                ParseTree clone = tree.clone();
                demote(findInTree(clone, sibling), findInTree(clone, treeNode));
                res.add(clone);
            }
        }

        // Swap child for all permutations
        if (treeNode.children.size() >= 2) {
            List<TreeNode> children = treeNode.children;
            int n = children.size();
            for(int i = 0; i < n; i++) {
                for(int j = i + 1; j < n; j++) {
                    ParseTree clone = tree.clone();
                    swapTreeNode(findInTree(clone, children.get(i)), findInTree(clone, children.get(j)));
                    res.add(clone);
                }
            }
        }
        return res;
    }

    private static void promote(TreeNode child, TreeNode treeNode) {
        treeNode.children.removeIf(tn -> tn.id == child.id);
        treeNode.parent.children.add(child);
        child.parent = treeNode.parent;
    }

    private static void demote(TreeNode sibling, TreeNode treeNode) {
        treeNode.parent.children.removeIf(tn -> tn.id == sibling.id);
        treeNode.children.add(sibling);
        sibling.parent = treeNode;
    }

    /**
     * @param tn1
     * @param tn2
     */
    private static void swapTreeNode(TreeNode tn1, TreeNode tn2) {
        tn1.swap(tn2);
    }

    /**
     * findInTree a tree node in a tree by id
     * @param tree
     * @param treeNode
     * @return
     */
    private static TreeNode findInTree(ParseTree tree, TreeNode treeNode) {
        for (TreeNode tn : tree) {
            if (tn.id == treeNode.id) {
                return tn;
            }
        }
        System.out.println("Error: fail to find node " + treeNode.id);
        return null;
    }

    public static void main(String[] args) {
        List<List<Integer>> edges = new ArrayList<>();
        edges.add(new ArrayList<>(Arrays.asList(0, 1)));
        edges.add(new ArrayList<>(Arrays.asList(1, 3)));
        edges.add(new ArrayList<>(Arrays.asList(3, 2)));
        Map<Integer, TreeNode> treeNodes = new ArrayList<>(Arrays.asList(
                new TreeNode(0, "ROOT", new Node("ROOT", NodeType.ROOT)),
                new TreeNode(1, "return", new Node("SELECT", NodeType.SN)),
                new TreeNode(2, "all", new Node("ALL", NodeType.QN)),
                new TreeNode(3, "books", new Node("inproceedings.booktitle", NodeType.VN))
        )).stream().collect(Collectors.toMap(tn -> tn.getId(), Function.identity()));
        ParseTree parseTree = new ParseTree(treeNodes, edges);
        List<ParseTree> parseTrees = genParseTrees(parseTree);
        parseTrees.forEach(tree -> {
            System.out.println("-------------------------");
            System.out.println(tree);
            System.out.println("-------------------------");
        });
    }
}
