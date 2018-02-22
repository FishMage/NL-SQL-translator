package main.server;

import com.google.gson.Gson;
import spark.ResponseTransformer;

import static spark.Spark.*;

public class JsonTransformer implements ResponseTransformer {
    private Gson gson = new Gson();

    @Override
    public String render(Object model) {
        return gson.toJson(model);
    }
}
