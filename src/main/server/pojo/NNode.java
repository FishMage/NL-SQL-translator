
package main.server.pojo;

import javax.annotation.Generated;
import com.google.gson.annotations.SerializedName;
import main.core.Node;
import main.core.TreeNode;

@Generated("net.hexar.json2pojo")
@SuppressWarnings("unused")
public class NNode {

    @SerializedName("id")
    private Integer mId;
    @SerializedName("word")
    private String mWord;
    @SerializedName("type")
    private String mType;
    @SerializedName("val")
    private String mVal;

    public NNode() {}

    public NNode(TreeNode treeNode) {
        mId = treeNode.id;
        mWord = treeNode.word;
        mVal = treeNode.node.val;
        mType = treeNode.node.type.name();
    }

    public Integer getId() {
        return mId;
    }

    public String getType() {
        return mType;
    }

    public String getVal() {
        return mVal;
    }

    public String getmWord() {
        return mWord;
    }

    public static class Builder {

        private Integer mId;
        private String mType;
        private String mVal;
        private String mWord;

        public NNode.Builder withId(Integer id) {
            mId = id;
            return this;
        }

        public NNode.Builder withWord(String word) {
            mWord = word;
            return this;
        }

        public NNode.Builder withType(String type) {
            mType = type;
            return this;
        }

        public NNode.Builder withVal(String val) {
            mVal = val;
            return this;
        }

        public NNode build() {
            NNode NNode = new NNode();
            NNode.mId = mId;
            NNode.mWord = mWord;
            NNode.mType = mType;
            NNode.mVal = mVal;
            return NNode;
        }

    }

}
