
package main.server.pojo;

import javax.annotation.Generated;
import com.google.gson.annotations.SerializedName;

@Generated("net.hexar.json2pojo")
@SuppressWarnings("unused")
public class ParseTextRes {

    @SerializedName("tree")
    private NTree mNTree;

    public NTree getNTree() {
        return mNTree;
    }

    public static class Builder {

        private NTree mNTree;

        public ParseTextRes.Builder withNTree(NTree nTree) {
            mNTree = nTree;
            return this;
        }

        public ParseTextRes build() {
            ParseTextRes ParseTextRes = new ParseTextRes();
            ParseTextRes.mNTree = mNTree;
            return ParseTextRes;
        }

    }

}
