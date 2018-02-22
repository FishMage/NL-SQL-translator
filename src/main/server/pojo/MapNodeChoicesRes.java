package main.server.pojo;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

public class MapNodeChoicesRes {
    @SerializedName("node_choices")
    private Map<String, List<NNode>> nodeChoices;

    private MapNodeChoicesRes(Builder builder) {
        nodeChoices = builder.nodeChoices;
    }

    public Map<String, List<NNode>> getNodeChoices() {
        return nodeChoices;
    }


    public static final class Builder {
        private Map<String, List<NNode>> nodeChoices;

        public Builder() {
        }

        public Builder withNodeChoices(Map<String, List<NNode>> val) {
            nodeChoices = val;
            return this;
        }

        public MapNodeChoicesRes build() {
            return new MapNodeChoicesRes(this);
        }
    }
}
