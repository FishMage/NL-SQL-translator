package main.core;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class SchemaGraph {
	
	
        // table, column name, column type
       	private Map<String, Map<String, String>> tables;
	//table, column name, column values
	private Map<String, Map<String, Set<String>>> tableRows;
	

        //table, primary key: set of column names
	private Map<String, Set<String>> keys;
	

        //{table, set of table names connected with table}
        // two tables are connected if t1's primary key is not the same as t2's primary key
        // and either t1's primary key contains t2's primary key or vice versa
	private Map<String, Set<String>> connectivity;
	
        //constructing a SchemaGraph, with java.sql.Connection
	public SchemaGraph(Connection con) throws SQLException {
		System.out.println("Retrieving schema graph...");
		DatabaseMetaData meta = con.getMetaData();
		tables = new HashMap<>();
		tableRows = new HashMap<>();
		ResultSet rsTable = meta.getTables(null, null, "%", new String[]{"TABLE"});
		

	    Statement stmt = con.createStatement();
		while (rsTable.next()) {
			String tableName = rsTable.getString("TABLE_NAME");
			
			//System.out.println(tableName);
			
			tables.put(tableName, new HashMap<>());
			tableRows.put(tableName, new HashMap<>());
			
			Map<String, String> table = tables.get(tableName);
			Map<String, Set<String>> tableRow = tableRows.get(tableName);
			
			ResultSet rsColumn = meta.getColumns(null, null, tableName, null);
			while (rsColumn.next()){
				/*retrieve column info for each table, insert into tables*/
				String columnName = rsColumn.getString("COLUMN_NAME");
				String columnType = rsColumn.getString("TYPE_NAME");
				
				//System.out.println(columnName);
				//System.out.println(columnType+"\n");
				
				table.put(columnName, columnType); 
				/*draw random sample of size 2000 from each table, insert into tableRows*/
				String query = "SELECT " + columnName + " FROM " + tableName + " ORDER BY RANDOM() LIMIT 2000;";
				ResultSet rows = stmt.executeQuery(query);
				tableRow.put(columnName, new HashSet<String>());
				Set<String> columnValues = tableRow.get(columnName);
				while (rows.next()){
					String columnValue = rows.getString(1);
					//testing if the last column read has a SQL NULL
					if (!rows.wasNull())
						columnValues.add(columnValue);
				}
			}			
		}
		if (stmt != null) { stmt.close(); }
		//System.out.println(tables);
		readPrimaryKeys(meta);
		findConnectivity();
		System.out.println("Schema graph retrieved.");
	}


        //initialize keys
	private void readPrimaryKeys(DatabaseMetaData meta) throws SQLException {
		keys = new HashMap<>();
		for (String tableName : tables.keySet()) {
			
			ResultSet pk = meta.getPrimaryKeys(null, null, tableName);
			//primary keys have to be set for tables in the database
			if(!pk.next()) {
				System.out.println("The primary key is not set for 1 or more tables.\n");
	    		return;
			}
			keys.put(tableName, new HashSet<String>());
			while (true) {
		    	keys.get(tableName).add(pk.getString("COLUMN_NAME"));
		    	if(!pk.next()) {break;}
		    }
		}
		//System.out.println(keys);
	}
	
	//find all connected tables in the database, stored in connectivity
	// 2 tables are connected if they have joinKeys(defined below)
	private void findConnectivity() {
		connectivity = new HashMap<String, Set<String>>();
		for (String tableName : tables.keySet()) {
			connectivity.put(tableName, new HashSet<String>());
		}
		for (String table1 : tables.keySet()) {
			for (String table2 : tables.keySet()) {
				if (table1.equals(table2)) { continue; }
				if (!getJoinKeys(table1, table2).isEmpty()) {
					connectivity.get(table1).add(table2);
					connectivity.get(table2).add(table1);
				}
			}
		}
		System.out.println(connectivity);
	}

	//return a set of joinkeys for 2 tables
	//2 tables have joinkeys if their primary keys are not identical(otherwise not foreign key?) and 
	//either primary keys of t1 contains t2 or vice versa. 
	//the joinkeys is the smaller set of primary keys of t1 or t2(the set being contained by another set).
	public Set<String> getJoinKeys(String table1, String table2) {
		Set<String> table1Keys = keys.get(table1);
		Set<String> table2Keys = keys.get(table2);
		if (table1Keys.equals(table2Keys)) { return new HashSet<String>(); }

		if (table2Keys.containsAll(table1Keys)) { return new HashSet<String>(table1Keys); }
		if (table1Keys.containsAll(table2Keys)) { return new HashSet<String>(table2Keys); }
		
		return new HashSet<String>();
	}

    
	//returns a linkedlist of tables to join t1 and t2 based on connectivity
	public List<String> getJoinPath(String table1, String table2) {
		if (!tables.containsKey(table1) || !tables.containsKey(table2)) {
			System.out.println("In SchemaGraph::getJoinPath(t1, t2), one or both of them are not in the database.\n");
			return new ArrayList<String>();
		}
		// Assume table1 and table2 are different.
		// Find shortest path using BFS.
		//Here we should have a tree structure, so using BFS or DFS doesn't make a difference
		HashMap<String, Boolean> visited = new HashMap<>();
		for (String tableName : tables.keySet()) {
			visited.put(tableName, false);
		}
		HashMap<String, String> prev = new HashMap<>(); // the parent tableName
		LinkedList<String> queue = new LinkedList<>();
		queue.add(table1);
		visited.put(table1, true);
		boolean found = false;
		while (!queue.isEmpty() && !found) {
			String currTable = queue.removeFirst();
			for (String nextTable : connectivity.get(currTable)) {
				if (!visited.get(nextTable)) {
					visited.put(nextTable, true);
					queue.add(nextTable);
					prev.put(nextTable, currTable);
				}
				if (nextTable.equals(table2)) { found = true; }
			}
		}

		LinkedList<String> path = new LinkedList<>();
		if (visited.get(table2)) {
			String table_to_push = table2; 
			path.push(table_to_push);
			while (prev.containsKey(table_to_push)) {
				table_to_push = prev.get(table_to_push);
				path.push(table_to_push);
			}
		}
		return path;
	}
	
	//return set of tables in the database
	public Set<String> getTableNames() {
		return tables.keySet();
	}
	
	//return set of columns in a table
	public Set<String> getColumns(String table) {
		return tables.get(table).keySet();
	}
	
	//return (limited to 2000)set of rows of a column in a table 
	public Set<String> getValues(String table, String column){
		return tableRows.get(table).get(column);
	}
	
	//return set of primary keys of a table
	public Set<String> getKeys(String table){
		return keys.get(table);
	}
	
	//return the set of tables that are connected with the table
	public Set<String> getNeighbors(String table){
		return connectivity.get(table);
	}
    
	//print out: table: tableName
	//           {column1: type1,   column2: type2,   ...}
	@Override
	public String toString() {
		String s = "";
		for (String tableName : tables.keySet()) {
			s += "table: "+tableName+"\n";
			s += "{";
			Map<String, String> columns = tables.get(tableName);
			for (String colName : columns.keySet()) {
				s += colName+": "+columns.get(colName)+",   ";
			}
			s += "}\n";
		}
		return s;
	}
	
	public static void main(String[] args) throws Exception {
	        //jdbc:postgresql://127.0.0.1:5432/ should be the default ip and port, user: dblpuser, password: dblpuser
		Connection con = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:5432/dblp", "dblpuser", "dblpuser");
		//Connection con = DriverManager.getConnection("jdbc:postgresql://localhost:5432/dblp", "dblpuser", "dblpuser");
		SchemaGraph schema = new SchemaGraph(con);
		System.out.println(schema);
		
		for(String table : schema.getTableNames()){
			System.out.println(table+":");
			System.out.println("Columns: ");
			System.out.println(schema.getColumns(table));
			System.out.println("Primary keys: ");
			System.out.println(schema.getKeys(table));
			System.out.printf("Tables connected with %s : ", table);
			System.out.println(schema.getNeighbors(table));
			System.out.println("----------------------------------------");
		}

	}
}

//Here is my output of the main using dblp database:

//	Retrieving schema graph...
//	{authorship=[inproceedings, article], inproceedings=[authorship], article=[authorship]}
//	Schema graph retrieved.
//	table: authorship
//	{author: text,   pubkey: text,   }
//	table: inproceedings
//	{area: text,   year: text,   title: text,   booktitle: text,   pubkey: text,   }
//	table: article
//	{journal: text,   year: text,   title: text,   pubkey: text,   }
//
//	authorship:
//	Columns: 
//	[author, pubkey]
//	Primary keys: 
//	[author, pubkey]
//	Tables connected with authorship : [inproceedings, article]
//	----------------------------------------
//	inproceedings:
//	Columns: 
//	[area, year, title, booktitle, pubkey]
//	Primary keys: 
//	[pubkey]
//	Tables connected with inproceedings : [authorship]
//	----------------------------------------
//	article:
//	Columns: 
//	[journal, year, title, pubkey]
//	Primary keys: 
//	[pubkey]
//	Tables connected with article : [authorship]
//	----------------------------------------
