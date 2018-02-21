package main.core;

import java.util.List;

/**
 * find the number of invalid nodes in a tree, as a score of a tree(the higher the worse)
 * for tree adjustor
 * here "invalid" means violating the grammar rules defined in the paper
 */
public class SyntaxEvaluator {
	int invalid;
	
	public SyntaxEvaluator(){
		invalid = 0;
	}

	// Rules
    /**
     * Q -> (SClause)(ComplexCondition)*
     * SClause -> SELECT + GNP
     * valid Query:
     * 1. first child is SN;
     * 2. 2 - n children is ComplexCondition
     */
    private static boolean Q(TreeNode treeNode) {
        List<TreeNode> children = treeNode.children;

        if(children.isEmpty() || !SClause(children.get(0))) {
            return false;
        } else {
            for(int i = 1; i < children.size(); i++) {
                if (!ComplexCondition(children.get(i))) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * ComplexCondition -> ON + (LeftSubTree*RightSubTree)
     * LeftSubTree -> GNP
     * RightSubTree -> GNP | VN | FN
     */
    private static boolean ComplexCondition(TreeNode treeNode) {
        if (!ON(treeNode)) {
            return false;
        } else {
            List<TreeNode> children = treeNode.children;
            if(children.size() != 2) {
                return false;
            } else {
                TreeNode left = children.get(0);
                TreeNode right = children.get(1);
                return GNP(left) && (GNP(right) || VN(right) || FN(right));
            }
        }
    }

    /**
     * valid GNP
     * if tree node is FN:
     *   has only a child
     * @param treeNode
     * @return
     */
    private static boolean GNP(TreeNode treeNode) {
        List<TreeNode> children = treeNode.children;
        if (FN(treeNode)) {
            return children.isEmpty() || children.size() == 1 && (GNP(children.get(0)));
        } else {
            return NP(treeNode);
        }
    }

    /**
     * NP -> NN + (NN)*(Condition)*
     * @param treeNode
     * @return
     */
    private static boolean NP(TreeNode treeNode) {
        if (!NN(treeNode)) {
            return false;
        } else {
            return treeNode.children.stream().allMatch(tn -> NN(tn) || Condition(tn));
        }
    }

    /**
     * Condition -> VN | (ON + VN)
     * @param treeNode
     * @return
     */
    private static boolean Condition(TreeNode treeNode) {
        return VN(treeNode) || ON(treeNode) && treeNode.children.size() == 1 && VN(treeNode.children.get(0));
    }

    private static boolean SClause(TreeNode treeNode) {
        List<TreeNode> children = treeNode.children;
        return treeNode.node.type == NodeType.SN && (children.isEmpty() || children.size() == 1 && GNP(children.get(0)));
    }
    private static boolean FN(TreeNode treeNode) {
        return treeNode.node.type == NodeType.FN;
    }

    private static boolean NN(TreeNode treeNode) {
        return treeNode.node.type == NodeType.NN;
    }

    private static boolean VN(TreeNode treeNode) {
        return treeNode.node.type == NodeType.VN;
    }

    private static boolean ON(TreeNode treeNode) {
        return treeNode.node.type == NodeType.ON;
    }

    // Check nodes

    private static boolean checkRoot(TreeNode treeNode) {
        return Q(treeNode);
    }

    /**
     * valid SN
     *  only one GNP child
     * @param treeNode
     * @return
     */
    private static boolean checkSN(TreeNode treeNode) {
        return treeNode.children.size() == 1 && GNP(treeNode.children.get(0));
    }

    /**
     * valid ON
     * 1. For ComplexCondition
     *     parent is ROOT
     *     match ComplexCondition
     * 2. For Condition
     *     parent is NN
     *     has a VN child
     * @param treeNode
     * @return
     */
    private static boolean checkON(TreeNode treeNode) {
        TreeNode parent = treeNode.parent;
        List<TreeNode> children = treeNode.children;
        return parent.node.type == NodeType.ROOT && ComplexCondition(treeNode)
                || NN(parent) && children.size() == 1 && VN(children.get(0));
    }

    /**
     * valid NN
     *  1. For NP -> NN + (NN)*(Condition)*, as a child
     *      has a NN parent and has no child
     *  2. For GNP -> NP -> (NN) + (NN)*(Condition)*
     *      match NP
     * @param treeNode
     * @return
     */
    private static boolean checkNN(TreeNode treeNode) {
        TreeNode parent = treeNode.parent;
        List<TreeNode> children = treeNode.children;
        return NN(parent) && children.isEmpty()
                || NP(treeNode);
    }

    /**
     * valid VN
     *     has no child
     * @param treeNode
     * @return
     */
    private static boolean checkVN(TreeNode treeNode) {
        return treeNode.children.isEmpty();
    }

    /**
     *  valid FN
     *  1. ON + (leftSubtree -> GNP -> FN + GNP)
     *      parent is ON
     *      has no child or a GNP child
     *  2. SClause -> SELECT + (GNP -> FN + GNP)
     *      parent is SN
     *      has a GNP child
     *  3. GNP -> FN + (GNP -> FN + GNP)
     *      parent is FN
     *      has a GNP child
     * @param treeNode
     * @return
     */
    private static boolean checkFN(TreeNode treeNode) {
        TreeNode parent = treeNode.parent;
        List<TreeNode> children = treeNode.children;
        int childrenSize = children.size();
        if (ON(parent)) {
            return children.isEmpty() || childrenSize == 1 && GNP(children.get(0));
        } else if (FN(parent)) {
            return childrenSize == 1 && GNP(children.get(0));
        } else {
            return SClause(parent);
        }
    }

    /**
     * score the tree by valid tree percentage: (num of valid tree nodes) / (num of tree nodes)
     * @param tree
     * @return score of parse tree
     */
	static double evaluate(ParseTree tree) {
        int numTreeNode = 0, numValid = 0;
        for (TreeNode tn : tree) {
            NodeType type = tn.node.type;
            numTreeNode++;
            switch (type) {
                case ROOT: if(checkRoot(tn)) numValid++; else tn.isInvalid = true;
                break;
                case SN: if(checkSN(tn)) numValid++; else tn.isInvalid = true;
                break;
                case ON: if(checkON(tn)) numValid++; else tn.isInvalid = true;
                break;
                case NN: if(checkNN(tn)) numValid++; else tn.isInvalid = true;
                break;
                case VN: if(checkVN(tn)) numValid++; else tn.isInvalid = true;
                break;
                case FN: if(checkFN(tn)) numValid++; else tn.isInvalid = true;
                break;
                case QN: numValid++;
                break;
            }
        }
        return (double)numValid / numTreeNode;
    }
	
}
