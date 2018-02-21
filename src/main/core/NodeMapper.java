package main.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;


interface INodeMapper {
    List<Node> mapChoicesForNode(TreeNode treeNode, SchemaGraph schema, int k);

    Map<Integer, List<Node>> mapChoicesForTree(TreeNode root);
}

public class NodeMapper {
    /*
        Map specific words to Node
        Fill the mappings later
     */
    static final HashMap<String, Node> mappings = new HashMap<String, Node>() {{
        put("return", new Node("SELECT", NodeType.SN));
        put("give", new Node("SELECT", NodeType.SN));
    
        put("more", new Node(">", NodeType.ON));
        put("greater", new Node(">", NodeType.ON));
        put("larger", new Node(">", NodeType.ON));
        put("after", new Node(">", NodeType.ON));
        put("older", new Node(">", NodeType.ON));
        put("smaller", new Node("<", NodeType.ON));
        put("younger", new Node("<", NodeType.ON));
        put("before", new Node("<", NodeType.ON));
        put("less", new Node("<", NodeType.ON));
        put("equals", new Node("=", NodeType.ON));
        put("same", new Node("=", NodeType.ON));
        put("not", new Node("!=", NodeType.ON));
        put("no", new Node("!=", NodeType.ON));
    
        put("average", new Node("AVG", NodeType.FN));
        put("maximum", new Node("MAX", NodeType.FN));
        put("largest", new Node("MAX", NodeType.FN));
        put("most", new Node("MAX", NodeType.FN));
        put("smallest", new Node("MIN", NodeType.FN));
        put("least", new Node("MIN", NodeType.FN));
        put("total", new Node("SUM", NodeType.FN));
        put("sum", new Node("SUM", NodeType.FN));
        put("number", new Node("COUNT", NodeType.FN));
        put("count", new Node("COUNT", NodeType.FN));
    
        put("all", new Node("ALL", NodeType.QN));
        put("any", new Node("ANY", NodeType.QN));
        put("each", new Node("EACH", NodeType.QN));
    
        put("and", new Node("AND", NodeType.LN));
        put("or", new Node("OR", NodeType.LN));
    
    }};
    
    public List<Node> mapChoicesForNode(TreeNode treeNode, SchemaGraph db, int k){
    	List<Node> res = new ArrayList<Node>();
    	if(treeNode.word.equals("ROOT")){
    		res.add(new Node("ROOT", NodeType.ROOT));
    		return res;
    	}
    	String word = treeNode.word.toLowerCase();
    	if(mappings.containsKey(word)){
    		res.add(mappings.get(word));
    		return res;
    	}
    	Set<Node> recordNodes = new HashSet<Node>();
    	
    	for(String tableName: db.getTableNames()){
    		res.add(new Node(tableName, NodeType.NN, WordSimilarity.getSim(tableName, word)));
    		for(String colName: db.getColumns(tableName)){
    			res.add(new Node(tableName + "." +colName, NodeType.NN, WordSimilarity.getSim(colName, word)));
    			for(String record: db.getValues(tableName, colName)){
    				if (word == null || record == null) {
						System.out.println("Choosing between "+word+" and "+record);
						System.out.println("In table "+tableName+", column "+colName);
					}
                    recordNodes.add(new Node(tableName+"."+colName, NodeType.NN,
                            WordSimilarity.getSim(word, record)));
    				recordNodes.add(new Node(tableName+"."+colName, NodeType.VN,
    						WordSimilarity.getSim(word, record)));
    			}
    		}
    	}
    	res.addAll(recordNodes);
		res.add(new Node("meaningless", NodeType.UNKNOW, 1.0));
		Collections.sort(res, new Node.sortByScore());
		Set<String> tableCol = new HashSet<String>();
		List<Node> resNew = new ArrayList<Node>();
		for(Node nodeinfo: res){
			if(tableCol.contains(nodeinfo.val)){
				continue;
			}
			resNew.add(nodeinfo);
			tableCol.add(nodeinfo.val);
		}
		//Collections.sort(resNew, new Node.sortByScore());
		return resNew.subList(0, k);
    	
    }
    public static void main(String[] args) {
        NLParser parser = new NLParser();
        NodeMapper nodeMapper = new NodeMapper();
        try {
            Connection conn = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:5432/dblp", "dblpuser", "dblpuser");
            SchemaGraph schema = new SchemaGraph(conn);
            System.out.println("Schema:");
            System.out.println(schema.toString());
            ParseTree parseTree = new ParseTree("I will explain", parser);
            System.out.println(parseTree);
            System.out.println("Node choices:");
            for (TreeNode tn : parseTree) {
                System.out.println(nodeMapper.mapChoicesForNode(tn, schema, 7));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}

