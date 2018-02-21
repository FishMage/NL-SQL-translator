
package main.server.pojo;

import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Generated;
import com.google.gson.annotations.SerializedName;
import main.core.ParseTree;

@Generated("net.hexar.json2pojo")
@SuppressWarnings("unused")
public class NTree {

    @SerializedName("edges")
    private List<List<Integer>> mEdges;
    @SerializedName("nodes")
    private List<NNode> mNNodes;

    public NTree() {
    }

    public NTree(ParseTree parseTree) {
        mEdges = parseTree.getEdges();
        mNNodes = parseTree.getSortedTreeNodeList().stream()
                .map(tn -> new NNode(tn))
                .collect(Collectors.toList());
    }

    public List<List<Integer>> getNEdges() {
        return mEdges;
    }

    public List<NNode> getNNodes() {
        return mNNodes;
    }

    public static class Builder {

        private List<List<Integer>> mNEdges;
        private List<NNode> mNNodes;

        public NTree.Builder withNEdges(List<List<Integer>> nEdges) {
            mNEdges = nEdges;
            return this;
        }

        public NTree.Builder withNNodes(List<NNode> nNodes) {
            mNNodes = nNodes;
            return this;
        }

        public NTree build() {
            NTree NTree = new NTree();
            NTree.mEdges = mNEdges;
            NTree.mNNodes = mNNodes;
            return NTree;
        }

    }

}
