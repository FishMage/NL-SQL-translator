package main.server.pojo;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

public class MapSelectNodeChoicesReq {
    @SerializedName("node_choices")
    private Map<String, NNode> nodeChoices;

    public Map<String, NNode> getNodeChoices() {
        return nodeChoices;
    }
}
