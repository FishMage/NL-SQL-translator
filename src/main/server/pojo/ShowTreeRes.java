
package main.server.pojo;

import javax.annotation.Generated;
import com.google.gson.annotations.SerializedName;

@Generated("net.hexar.json2pojo")
@SuppressWarnings("unused")
public class ShowTreeRes {

    @SerializedName("tree")
    private NTree nTree;

    private ShowTreeRes(Builder builder) {
        nTree = builder.nTree;
    }

    public NTree getnTree() {
        return nTree;
    }

    public static final class Builder {
        private NTree nTree;

        public Builder() {
        }

        public Builder withNTree(NTree val) {
            nTree = val;
            return this;
        }

        public ShowTreeRes build() {
            return new ShowTreeRes(this);
        }
    }
}
