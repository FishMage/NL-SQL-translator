package main.core;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


//May have many bugs...better than nothing still..
public class ImplicitNodeInserter {
	
	private static int endOfLeftSubtree (TreeNode[] tnodes) {
		for (int i = 2; i < tnodes.length; i ++) {
			if(tnodes[i].parent.id == tnodes[0].id) {return i - 1;} //Object equal, may need to be overwritten
		}                                                          //in TreeNode??
		return -1;
	}
	
	private static int nnToBeInserted (TreeNode[] tnodes) {
		for (int i = endOfLeftSubtree(tnodes); i > 0; i--) {
			if (tnodes[i].node.type==NodeType.NN) {return i;}
		}
		return -1;
	}
	
	private static int IndexToInsertCN (TreeNode[] tnodes) {
		for (int i = endOfLeftSubtree(tnodes) + 1; i < tnodes.length; i++) {
			if (tnodes[i].node.type==NodeType.NN) {return i;}
		}
		return -1;
	}
	
	private static int idxCoreNode (TreeNode[] tnodes, boolean left) {
		int startIndex = 1;
		int endIndex = endOfLeftSubtree(tnodes);

		if (!left) {
			startIndex = endOfLeftSubtree(tnodes) + 1;
			endIndex = tnodes.length - 1;
		}
		for (int i = startIndex; i <= endIndex; i ++) {
			if (tnodes[i].node.type==NodeType.NN) {return i;}
		}
		return -1;
	}
	
	public static void insertImplicitNode(ParseTree tree){
		List <TreeNode> rootChildren = tree.root.children;
		if(rootChildren.size()<=1){return;}
		
		//add from SELECT to left subtree
		
		//System.out.println("add nodes under SELECT to left subtree");
		int idxSN = 0; 
		for(int i = 0; i < rootChildren.size(); i++){
			if(rootChildren.get(i).node.type==NodeType.SN){ idxSN = i; break;} //one name node copied
		}
		
		TreeNode sn = rootChildren.get(idxSN);
		List<TreeNode> snChildren = sn.children;
		
		int idxSN_NN = 0;
		for(int i = 0; i < snChildren.size(); i++){
			if(snChildren.get(i).node.type==NodeType.NN){ idxSN_NN = i; break;}
		}
		
		TreeNode sn_nn = snChildren.get(idxSN_NN);
		int idxInsertedNode;
		TreeNode copy1;
		
		for(int i = 0; i<rootChildren.size(); i++){
			if(i != idxSN){
				TreeNode[] tnodesSN_NN = rootChildren.get(i).allChildrenArray();
				idxInsertedNode = nnToBeInserted(tnodesSN_NN);
				if(idxInsertedNode != -1){
					copy1 = sn_nn.clone();
					
					tnodesSN_NN[idxInsertedNode].children.add(copy1);
					copy1.parent = tnodesSN_NN[idxInsertedNode];
				}
			}
		}
		
		//System.out.println(tree.toString() + '\n');
		///////
		//System.out.println("core node insertion");
		int idxLeftCoreNode = -1;
		int idxRightCoreNode = -1;
		for(int i = 0; i<rootChildren.size(); i++){
			if(i != idxSN){
				TreeNode[] tnodes = rootChildren.get(i).allChildrenArray();
				int startOfRightBranch = endOfLeftSubtree(tnodes) + 1;
				int sizeOfRightTree = tnodes[startOfRightBranch].children.size() + 1;
				//if right tree contains only number, skip
				if(sizeOfRightTree!=1 || !tnodes[startOfRightBranch].word.matches("-?\\d+(\\.\\d+)?")){
					idxLeftCoreNode = idxCoreNode(tnodes, true);
					idxRightCoreNode = idxCoreNode(tnodes, false);
					if(idxLeftCoreNode!= -1){
						boolean insert = false;
						
						if(idxRightCoreNode==-1){
							insert = true;
						}
						else if(!tnodes[idxLeftCoreNode].node.val.equals(
								tnodes[idxRightCoreNode].node.val)){
							insert = true;
						}
						if(insert){
							TreeNode copy2 = tnodes[idxLeftCoreNode].clone();
							copy2.children = new ArrayList<TreeNode>();
							
							boolean insertFN = false;
							
							int idxNRightCoreNode = IndexToInsertCN(tnodes);
							if(idxNRightCoreNode!=-1){
								for(int j = tnodes.length-1; j>endOfLeftSubtree(tnodes); j--){
									if(tnodes[j].node.type==NodeType.FN){
										idxNRightCoreNode = j + 1;
										insertFN = true;
										break;
									}
								}
							}
							if(insertFN){
								//FN node has no child or 1 NN node
								List<TreeNode> fnChildren = tnodes[idxNRightCoreNode].children;
								for(TreeNode tnodeTemp: fnChildren){
									copy2.children.add(tnodeTemp);
									tnodeTemp.parent = copy2;
								}
								copy2.parent = tnodes[idxNRightCoreNode-1];
								tnodes[idxNRightCoreNode-1].children = new ArrayList<TreeNode>();
								tnodes[idxNRightCoreNode-1].children.add(copy2);
							}
							else{
								//only VN
								if(idxNRightCoreNode==-1){
									idxNRightCoreNode = endOfLeftSubtree(tnodes) + 1;
								}
								copy2.children.add(tnodes[idxNRightCoreNode]);
								copy2.parent = tnodes[idxNRightCoreNode].parent;
								TreeNode toDelete = tnodes[idxNRightCoreNode];
								for(int k = 0; k < tnodes[idxNRightCoreNode].parent.children.size();k++){
									TreeNode temp = tnodes[idxNRightCoreNode].parent.children.get(k);
									if(temp.id==toDelete.id && temp.word.equals(toDelete.word)
											&& temp.node.type==toDelete.node.type
											&& temp.node.val.equals(toDelete.node.val)){
										tnodes[idxNRightCoreNode].parent.children.remove(k);
									}
								}
								tnodes[idxNRightCoreNode].parent.children.add(copy2);
								tnodes[idxNRightCoreNode].parent = copy2;
								
							}
						}
						//System.out.println(tree.toString());
						//constraints nodes(NV in the paper)
						List<TreeNode> NV_children_left = tnodes[idxLeftCoreNode].children;
						for (int j = 0; j < NV_children_left.size(); j ++) {
							TreeNode[] tnodesNew = rootChildren.get(i).allChildrenArray();
							idxRightCoreNode = idxCoreNode(tnodesNew, false);
							List<TreeNode> NV_children_right = tnodesNew[idxRightCoreNode].children;
							
							boolean found_NV = false;
							
							TreeNode curr_left = NV_children_left.get(j);
							NodeType curr_left_type = curr_left.node.type;
							for (int k = 0; k < NV_children_right.size(); k ++) {
								TreeNode curr_right = NV_children_right.get(k);
								if(curr_left_type==NodeType.ON){
									//equal is object equal...
									if (curr_left.equals(curr_right)) {
										found_NV = true;
										break;
									}
									else{
										//!!!!!
										if (curr_left.node.sameSchema(curr_right.node)) {
											found_NV = true;
											break;
										}
									}
								}
							}
							if(!found_NV){
								TreeNode copy3 = curr_left.clone();
								tnodesNew[idxRightCoreNode].children.add(copy3);
								copy3.parent = tnodesNew[idxRightCoreNode];
							}
						}
						
						//insert FN
						TreeNode[] tnodes_final_temp = rootChildren.get(i).allChildrenArray();
						int indexOfLeftFN_Tail = -1;
						for (int j = idxLeftCoreNode; j > 0; j --) {
							if (tnodes_final_temp[j].node.type==NodeType.FN) {
								indexOfLeftFN_Tail = j;
								break;
							}
						}
						if(indexOfLeftFN_Tail!=-1){
							for (int k = 1; k < indexOfLeftFN_Tail + 1; k ++) {
								TreeNode[] tnodes_final = rootChildren.get(i).allChildrenArray();
								idxRightCoreNode = idxCoreNode(tnodes_final, false);
								boolean found_FN = false;
								for (int j = endOfLeftSubtree(tnodes_final) + 1; j < idxRightCoreNode; j ++) {
                                    //!!!!!!!
									if (tnodes_final[j].node.type==tnodes_final[k].node.type &&
											tnodes_final[j].node.val.equals(tnodes_final[k].node.val)) {
										found_FN = true;
									}
								}
								if(!found_FN) {
									TreeNode copy4 = tnodes_final[k].clone();
									copy4.children = new ArrayList<TreeNode>();
									
                                    TreeNode toDelete = tnodes_final[endOfLeftSubtree(tnodes_final) + 1];
                                    for(int toD=0; toD<tnodes[0].children.size(); toD++){
                                    	TreeNode temp = tnodes[0].children.get(toD);
                                    	if(temp.id==toDelete.id && temp.word.equals(toDelete.word)
											&& temp.node.type==toDelete.node.type
											&& temp.node.val.equals(toDelete.node.val)){
                                    		tnodes[0].children.remove(toD);
                                    	}
                                    }
									tnodes[0].children.add(copy4);

									copy4.parent = tnodes[0];
									copy4.children.add(tnodes[endOfLeftSubtree(tnodes_final) + 1]);
									tnodes[endOfLeftSubtree(tnodes_final) + 1].parent = copy4;
								}
							}
							
						}
						
						
						
					}
				}
			}
		}
	}
}
