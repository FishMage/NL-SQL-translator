package main.server;

import static spark.Spark.*;
import com.google.gson.Gson;

import javafx.util.Pair;
import main.core.*;
import main.server.pojo.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Server for YANLIDB
 * @author sinkerine
 */
public class Index {
    private static Gson gson = new Gson();
    private static JsonTransformer jsonTransformer = new JsonTransformer();
    private static ParseTree parseTree;
    private static NLParser parser;
    private static Connection conn;
    private static SchemaGraph schema;
    private static NodeMapper nodeMapper;

    public static void main(String[] args) {

//        onLaunch();

        port(4567);
        enableCORS("http://localhost:63342","POST","Access-Control-Allow-Origin");
        System.out.println("Listening on 4567...");


        /**
         * request: POST
         * {
         *      "text": ""
         * }
         * response:
         *     {
         *        "tree": {
         *            "nodes": [
         *                {
         *                    "id": 0,
         *                    "word": "",
         *                    "val": "",
         *                    "type": ""
         *                }
         *            ],
         *            "edges": [
         *                [0,0]
         *            ]
         *        }
         *    }
         */
        post("/parse/text", (req, res) -> {
           ParseTextReq parseTextReq = gson.fromJson(req.body(), ParseTextReq.class);
           String text = parseTextReq.getText();
           System.out.println("Request: " + text);
           parseTree = new ParseTree(text, new NLParser());
            return new ParseTextRes.Builder()
                    .withNTree(new NTree(parseTree))
                    .build();
        }, jsonTransformer);

        /**
         * request: GET
         * response:
         *
         *     {
         *        "node_choices": {
         *            "0": [
         *                {
         *                    "val: "",
         *                    "type": ""
         *                },
         *            ],
         *            "1": [
         *                {
         *                    "val: "",
         *                    "type": ""
         *                },
         *            ],
         *        }
         *    }
         */

        get("/map/node_choices", (req, res) -> {
            System.out.println("Request: Get-> map_node_choices");
            List<TreeNode> treeNodeList = parseTree.getSortedTreeNodeList();
            treeNodeList.sort(Comparator.comparing(TreeNode::getId));
            Map<String, List<NNode>> choices = treeNodeList.stream()
                    .map(tn -> {
                        List<Node> nodes = nodeMapper.mapChoicesForNode(tn, schema, 5);
                        return new Pair<>(String.valueOf(tn.id),
                                nodes.stream()
                                        .map(node -> new NNode.Builder()
                                                .withVal(node.val)
                                                .withType(node.type.name())
                                                .build())
                                        .collect(Collectors.toList()));
                    })
                    .collect(Collectors.toMap(pr -> pr.getKey(), pr -> pr.getValue()));
            return new MapNodeChoicesRes.Builder().withNodeChoices(choices).build();
        }, jsonTransformer);

        /**
         * request: POST
         * {
         *      "node_choices": {
         *          "id": {
         *              "val": "",
         *              "type": "",
         *          }
         *      }
         * }
         * response:
         */

        post("/map/select_node_choices", (req, res) -> {
            System.out.println("Request: Post->/map/select_node_choices");
            MapSelectNodeChoicesReq nodeChoicesReq = gson.fromJson(req.body(), MapSelectNodeChoicesReq.class);
            Map<String, NNode> nodeChoices = nodeChoicesReq.getNodeChoices();
            Map<Integer, Node> nodeMappings = nodeChoices.entrySet().stream()
                    .collect(Collectors.toMap(entry -> Integer.parseInt(entry.getKey()),
                            entry -> {
                                NNode nnode = entry.getValue();
                                return new Node(nnode.getVal(), NodeType.valueOf(nnode.getType()));
                            }
                    ));
            parseTree.updateNodeMappings(nodeMappings);
            parseTree.removeUnKnownNodes();
            return "OK";
        });

        /**
         * request: GET
         * response:
         *     {
         *        "trees": [
         *            {
         *                "nodes": [
         *                    {
         *                        "id": 0,
         *                        "word": "",
         *                        "val: "",
         *                        "type": ""
         *                    }
         *                ],
         *                "edges": [
         *                    [0,0],
         *                ]
         *            },
         *        ]
         *    }
         */

        get("/adjust/tree_choices", (req, res) -> {
            System.out.println("Request: Get->adjust/tree_choice");
            List<ParseTree> parseTrees = TreeAdjustor.genParseTrees(parseTree, 10);
            List<NTree> trees = parseTrees.stream()
                    .map(parseTree -> new NTree(parseTree))
                    .collect(Collectors.toList());
            return new AdjustTreeChoicesRes.Builder()
                    .withTrees(trees)
                    .build();
        }, jsonTransformer);

        /**
         * request: POST
         *     {
         *        "tree": {
         *            "nodes": [
         *                {
         *                    "id": 0,
         *                    "word": "",
         *                    "val": "",
         *                    "type": ""
         *                }
         *            ],
         *            "edges": [
         *                [0,0]
         *            ]
         *        }
         *    }
         */
        post("/adjust/select_tree_choice", (req, res) -> {
            System.out.println("Request: Post->adjust/select_tree_choice");
            AdjustSelectTreeChoice adjustSelectTreeChoice = gson.fromJson(req.body(), AdjustSelectTreeChoice.class);
            NTree treeChoice = adjustSelectTreeChoice.getTree();
            List<List<Integer>> edges = treeChoice.getNEdges();
            Map<Integer, TreeNode> treeNodes = treeChoice.getNNodes().stream()
                    .map(node -> new TreeNode(node.getId(), node.getmWord(),
                            new Node(node.getVal(), NodeType.valueOf(node.getType()))))
                    .collect(Collectors.toMap(tn -> tn.getId(), Function.identity()));
            parseTree = new ParseTree(treeNodes, edges);
            return "OK";
        });

        post("/process/insert_implicit_nodes", (req, res) -> {
            parseTree.insertImplicitNode();
            return "OK";
        });

        /**
         * GET
         * Response:
         * {
         *      "sql": ""
         * }
         */
        get("/process/translate_to_sql", (req, res) -> {
            SQLGenerator.SQLFormat sqlFormat = parseTree.translateToSQL(schema);
            return new ProcessTranslateToSQLRes.Builder()
                    .withSql(sqlFormat.toString())
                    .build();
        }, jsonTransformer);

        /**
         * request:
         * {}
         * response
         * 200
         * {}
         */

        get("/parse/clear", (req, res) -> {
            parseTree = null;
            return "OK";
        });

        /**
         *
         */
        get("/show/tree", (req, res) -> {
            if (parseTree != null) {
                return new ShowTreeRes.Builder().withNTree(new NTree(parseTree));
            } else {
                return "Parse tree not initilized!";
            }
        }, jsonTransformer);
    }
    private static void enableCORS(final String origin, final String methods, final String headers) {

        options("/*", (request, response) -> {

            String accessControlRequestHeaders = request.headers("Access-Control-Request-Headers");
            if (accessControlRequestHeaders != null) {
                response.header("Access-Control-Allow-Headers", accessControlRequestHeaders);
            }

            String accessControlRequestMethod = request.headers("Access-Control-Request-Method");
            if (accessControlRequestMethod != null) {
                response.header("Access-Control-Allow-Methods", accessControlRequestMethod);
            }

            return "OK";
        });

        before((request, response) -> {
            response.header("Access-Control-Allow-Origin", origin);
            response.header("Access-Control-Request-Method", methods);
            response.header("Access-Control-Allow-Headers", headers);
            // Note: this may or may not be necessary in your particular application
            response.type("application/json");
        });
    }

    private static void onLaunch() {
        parser = new NLParser();
        connDb();
        try {
            schema = new SchemaGraph(conn);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        System.out.println("Database schema: " + schema.toString());
        nodeMapper = new NodeMapper();
    }

    private static void connDb() {
        try { Class.forName("org.postgresql.Driver"); }
        catch (ClassNotFoundException e1) { }

        try {
            conn = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:5432/dblp", "dblpuser", "dblpuser");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        System.out.println("Connected to database.");
    }

    private static void disconnDb() {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
