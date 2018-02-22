package main.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;





public class SQLGenerator {
    /**
     * nested class SQLFormat
     */
    public class SQLFormat {
        private List<SQLFormat> blocks;//for subqueries. See sec 6.2 in paper
        private Map<String, Collection<String>> map;
        
        SQLFormat() {
            map = new HashMap<>();
            map.put("SELECT", new ArrayList<String>());
            map.put("FROM", new HashSet<String>());
            map.put("WHERE", new HashSet<String>());
            blocks = new ArrayList<SQLFormat>();
        }
        
        
        public void add(String key, String value){
            map.get(key).add(value);
        }
        
        //add subquery in ComplexCondition...
        public void addBlock(SQLFormat query){
            blocks.add(query);
            add("FROM", "BLOCK"+blocks.size());
        }
        
        //for addJoinPath
        Collection<String> getCollection(String keyWord) { return map.get(keyWord); }
        
        //helper for toString
        private StringBuilder SelectFrom_format(Collection<String> SelectFrom){
            StringBuilder sb = new StringBuilder();
            for(String query:SelectFrom){
                if(sb.length()==0){
                    sb.append(query);
                }
                else{
                    sb.append(", ").append(query);
                }
            }
            return sb;
        }
        //helper for toString
        private StringBuilder Where_format(Collection<String> Where){
            StringBuilder sb = new StringBuilder();
            for(String query:Where){
                if(sb.length()==0){
                    sb.append(query);
                }
                else{
                    //problem with Logic Node, no grammar for LN
                    //here choose just AND
                    sb.append(" AND ").append(query);
                }
            }
            return sb;
            
        }
        
        //Should give SQL query like Figure 8 in paper
        @Override
        public String toString() {
            if (map.get("SELECT").isEmpty() || map.get("FROM").isEmpty()) {
                return "The Query doesn't have (SELECT ... FROM ...) structure.\n";
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < blocks.size(); i++) {
                sb.append("Block"+(i+1)+":").append("\n");
                sb.append(blocks.get(i).toString()).append("\n");
                sb.append("\n");
            }
            sb.append("SELECT ").append(SelectFrom_format(map.get("SELECT"))).append("\n");
            sb.append(" FROM ").append(SelectFrom_format(map.get("FROM"))).append("\n");
            
            if (!map.get("WHERE").isEmpty()) {
                sb.append(" WHERE ").append(Where_format(map.get("WHERE"))).append("\n");
            }
            
            return sb.toString();
        }
        
    }
    
    SQLFormat query;
    private SchemaGraph schema;
    private int blockIndex = 1;//labeling subqueries
    
    public SQLGenerator(TreeNode root, SchemaGraph schema, boolean subQuery){
        if(!subQuery){
            this.schema = schema;
            query = new SQLFormat();
            
            translateSClause(root.children.get(0));
            if(root.children.size()>1){
                translateComplexCondition(root.children.get(1));
            }
            if (schema != null) addJoinPath();
        }
        else{
            this.schema = schema;
            query = new SQLFormat();
            translateGNP(root);
        }
        
    }
    
    public SQLGenerator(TreeNode root, SchemaGraph schema){
        this(root, schema, false);
    }
    
    /**
     * translate NN node
     */
    private void translateNN(TreeNode tnode, String valueFN){
        if(tnode.node.type != NodeType.NN){return;}
        if (!valueFN.equals("")) {
            query.add("SELECT", valueFN+"("+tnode.node.val+")"); //SELECT SUM(...)
        } else {
            query.add("SELECT", tnode.node.val);  //SELECT ...
        }
        query.add("FROM", tnode.node.val.split("\\.")[0]); // if node.val = "article.title" => FROM article
    }
    private void translateNN(TreeNode tnode) {
        translateNN(tnode, "");
    }
    
    private static boolean isNumber(String str) {
        return str.matches("-?\\d+(\\.\\d+)?");
    }
    
    /**
     * translate Condition according to grammar
     */
    private void translateCondition(TreeNode tnode) {
        String attribute = "ATTRIBUTE";
        String compareSymbol = "=";
        String value = "VALUE";
        if (tnode.node.type==NodeType.VN) {
            attribute = tnode.node.val;
            value = tnode.word;
        } else if (tnode.node.type==NodeType.ON) {
            compareSymbol = tnode.node.val;
            TreeNode VN = tnode.children.get(0);
            attribute = VN.node.val;
            value = VN.word;
        }
        if (!isNumber(value)) { value = "\""+value+"\""; }
        query.add("WHERE", attribute+" "+compareSymbol+" "+value);//WHERE article.title = ...
        query.add("FROM", attribute.split("\\.")[0]); //FROM article
    }
    
    /**
     * translate NP
     */
    private void translateNP(TreeNode tnode, String valueFN) {
        translateNN(tnode, valueFN);
        for (TreeNode child : tnode.children) {
            if (child.node.type==NodeType.NN) {
                translateNN(child);
            }
            else if (child.node.type==NodeType.ON ||child.node.type==NodeType.VN){
                /**/translateCondition(child);
            }
        }
    }
    private void translateNP(TreeNode tnode) {
        translateNP(tnode, "");
    }
    
    
    /**
     * translate GNP
     */
    private void translateGNP(TreeNode tnode) {
        if (tnode.node.type==NodeType.FN) {
            if (tnode.children.isEmpty()) { return; }
            translateNP(tnode.children.get(0), tnode.node.val);//GNP has no nested FN
        } else if (tnode.node.type==NodeType.NN) {
            translateNP(tnode);
        }
    }
    
    /**
     * translate SClause
     */
    private void translateSClause(TreeNode tnode) {
        if (tnode.node.type!=NodeType.SN) { return; }
        translateGNP(tnode.children.get(0));
    }
    
    /**
     * translate complex condition
     */
    private void translateComplexCondition(TreeNode tnode) {
        if (tnode.node.type!=NodeType.ON) { return; }
        if (tnode.children.size() != 2) { return; }
        SQLGenerator LeftSubTree = new SQLGenerator(tnode.children.get(0), schema, true);
        SQLGenerator RightSubTree= new SQLGenerator(tnode.children.get(1), schema, true);
        query.addBlock(LeftSubTree.query);
        query.addBlock(RightSubTree.query);
        query.add("WHERE", "BLOCK"+(blockIndex++)+" "+tnode.node.val+" "+"BLOCK"+(blockIndex++));
    }
    
    /////below are 3 methods for adding join query
    private void appendJoinKeys(String table1, String table2){
        Set<String> joinKeys = schema.getJoinKeys(table1, table2);
        for (String joinKey : joinKeys) {
            query.add("WHERE", table1+"."+joinKey+" = "+table2+"."+joinKey);
        }
    }
    
    private void addJoinPath(List<String> joinPath) {
        for (int i = 0; i < joinPath.size()-1; i++) {
            appendJoinKeys(joinPath.get(i), joinPath.get(i+1));
        }
    }
    
    private void addJoinPath() {
        List<String> fromTables = new ArrayList<String>(query.getCollection("FROM"));
        if (fromTables.size() <= 1) { return; }
        for (int i = 0; i < fromTables.size()-1; i++) {
            for (int j = i+1; j < fromTables.size(); j++) {
                List<String> joinPath = schema.getJoinPath(fromTables.get(i), fromTables.get(j));
                addJoinPath(joinPath);
            }
        }
    }
    /////
    
    public static void main(String[] args){
        ParseTree tree = new ParseTree();
        TreeNode[] tnodes = new TreeNode[6];
        
        tnodes[0] = new TreeNode(0, "ROOT", "ROOT");
        tnodes[0].node = new Node("ROOT",NodeType.ROOT);
        tnodes[1] = new TreeNode(1, "return", "--"); // posTag not useful
        tnodes[1].node = new Node("SELECT", NodeType.SN);
        tnodes[2] = new TreeNode(2, "titles", "--");
        tnodes[2].node = new Node( "inproceedings.title", NodeType.NN);
        tnodes[3] = new TreeNode(3, "theory", "--");
        tnodes[3].node = new Node( "inproceedings.area", NodeType.VN);
        tnodes[4] = new TreeNode(4, "before", "--");
        tnodes[4].node = new Node( "<", NodeType.ON);
        tnodes[5] = new TreeNode(5, "1970", "--");
        tnodes[5].node = new Node( "inproceedings.year", NodeType.VN);
        
        tree.root = tnodes[0];
        tree.root.children.add(tnodes[1]);
        tnodes[1].children.add(tnodes[2]);
        tnodes[2].parent = tnodes[1];
        tnodes[2].children.add(tnodes[3]);
        tnodes[2].children.add(tnodes[4]);
        tnodes[3].parent = tnodes[2];
        tnodes[4].parent = tnodes[2];
        tnodes[4].children.add(tnodes[5]);
        tnodes[5].parent = tnodes[4];
        
        System.out.println(tree);
        System.out.println(tree.translateToSQL(null));
    }
}




