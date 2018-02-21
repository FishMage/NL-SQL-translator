
package main.server.pojo;

import javax.annotation.Generated;
import com.google.gson.annotations.SerializedName;

import java.util.List;

@Generated("net.hexar.json2pojo")
@SuppressWarnings("unused")
public class AdjustTreeChoicesRes {

    @SerializedName("trees")
    List<NTree> trees;

    private AdjustTreeChoicesRes(Builder builder) {
        trees = builder.trees;
    }

    public List<NTree> getTrees() {
        return trees;
    }


    public static final class Builder {
        private List<NTree> trees;

        public Builder() {
        }

        public Builder withTrees(List<NTree> val) {
            trees = val;
            return this;
        }

        public AdjustTreeChoicesRes build() {
            return new AdjustTreeChoicesRes(this);
        }
    }
}
