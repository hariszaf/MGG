package be.kuleuven.mgG.internal.tasks;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Collections;
import java.util.ArrayList;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import be.kuleuven.mgG.internal.model.MGGManager;

public class ImportNetworkData extends AbstractTask {
	
	
    private final MGGManager mggManager;
	
    
    public ImportNetworkData (MGGManager mggManager) {
    	this.mggManager=mggManager;
    }

    
    
	@Override
	public void run(TaskMonitor taskMonitor) throws Exception {
				
		//get current network
		
		CyNetwork currentNetwork=mggManager.getCurrentNetwork();
		
		  
    	if(currentNetwork==null) {
    
		taskMonitor.showMessage(TaskMonitor.Level.ERROR, " No Network is currently loaded)");
		return;
		
	} 
				
		   
	    JSONArray networkData = new JSONArray();

	 
	    CyTable edgeTable = currentNetwork.getDefaultEdgeTable();
	    
	    
	    boolean weightColumnExists = edgeTable.getColumn("microbetag::weight") != null;
	    boolean hasWeights = false; 

	    //  are weights present?
	    if (weightColumnExists) {
	        for (CyEdge edge : currentNetwork.getEdgeList()) {
	            Double weight = edgeTable.getRow(edge.getSUID()).get("microbetag::weight", Double.class);
	            if (weight != null) {
	                hasWeights = true;
	                break; 
	            }
	        }
	    }
	    
	    // Add headers
	    JSONArray headers = new JSONArray();
	    
	    headers.add("Source Node");
	    headers.add("Target Node");
	    
	    if(hasWeights) {
	    	
	    	headers.add("weight");
	    }
	    networkData.add(headers);
	    

	   
	    for (CyEdge edge : currentNetwork.getEdgeList()) {
	        
	    	 JSONArray edgeArray = new JSONArray();

	    	    
	    	    String sourceNodeId = currentNetwork.getRow(edge.getSource()).get("name", String.class);
	    	    String targetNodeId = currentNetwork.getRow(edge.getTarget()).get("name", String.class);

	    	    
	    	    
	    	    Double weight = edgeTable.getRow(edge.getSUID()).get("microbetag::weight", Double.class);
	    	    
	       
	        if (hasWeights && weight!=null) {
	        	
	        	
	    	    		edgeArray.add(sourceNodeId);
	    	    		edgeArray.add(targetNodeId);
	    	    		edgeArray.add(weight);
	            
	    	    		networkData.add(edgeArray);
	            
	            } else if(!hasWeights) {
	                	
	            	
	            	
	            	taskMonitor.showMessage(TaskMonitor.Level.ERROR ,"Network data has no microbetag::weight value,"
	            			+ "network data will not import");
	            }
	        
	    }
	       

	    //  JSONObject for the network
	    JSONObject networkJsonObject = new JSONObject();
	    networkJsonObject.put("network", networkData);
	    
	    //  JSONObject for abundance data
	    JSONObject datajsonObject=new JSONObject();
	    datajsonObject=mggManager.getJsonObject();
	    
	    if (hasWeights && areNodesInDataJson(networkJsonObject,  datajsonObject)) {
	       //  If all are present
	        mggManager.setNetworkObject(networkJsonObject);
	        taskMonitor.setTitle("Uploading Network");
	        taskMonitor.showMessage(TaskMonitor.Level.INFO ,"Network data loaded correctly");
	        
	    
	        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, 
	            "Network data loaded correctly", "Information", JOptionPane.INFORMATION_MESSAGE));
	  
	      // taskMonitor.showMessage(TaskMonitor.Level.INFO ,"Network Object" +networkJsonObject.toString());
	      // taskMonitor.showMessage(TaskMonitor.Level.INFO ,"Data object" +datajsonObject.toString());
	    
	    } else {
    
    	// If not all  are present
	     mggManager.setNetworkObject(null);
	      List<String> unknown = unknownNodes(networkJsonObject, datajsonObject);
	      taskMonitor.showMessage(TaskMonitor.Level.ERROR, 
	        "Node ids of the network are not present with sequence identifiers of "
	        + " abudance table -> network data can't be imported"
	        + (unknown.isEmpty() ? "" : "; e.g. " + String.join(", ", unknown.subList(0, Math.min(5, unknown.size()))))
	        + ". Nodes of metadata variables are accepted once the metadata are imported (Import Metadata).");
	      
	     // SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, 
	          //    "Node ids of the network are not present with sequence identifiers of " +
	           //   "the abundance table -> network data can't be imported", "Error", JOptionPane.ERROR_MESSAGE));
	      
	    }	   
	}
	
		

		
	private boolean areNodesInDataJson(JSONObject networkJsonObject, JSONObject dataJsonObject) {
		// This function checks whether the node names of the loaded network, belong to the abundance table already loaded.
		// If not, then the network will not be loaded successfully, since microbetag would fail.
		// Nodes of the metadata variables (FlashWeave with metadata) are not in the abundance table: see unknownNodes.
		return unknownNodes(networkJsonObject, dataJsonObject).isEmpty();
	}

	/**
	 * The nodes of the network that are neither sequences of the abundance table nor metadata variables.
	 * A node of a metadata variable is named after it (e.g. ENV1, or ENV4_A for a value of a categorical
	 * variable): its name starts with the name of a variable of the imported metadata, as the microbetag
	 * server tells them apart.
	 */
	private List<String> unknownNodes(JSONObject networkJsonObject, JSONObject dataJsonObject) {
		
		 JSONArray networkData = (JSONArray) networkJsonObject.get("network");
		    Set<String> nodeNamesInNetwork = new HashSet<>();
		    for (int i = 1; i < networkData.size(); i++) { 
		        JSONArray edge = (JSONArray) networkData.get(i);
		        nodeNamesInNetwork.add((String) edge.get(0)); 
		        nodeNamesInNetwork.add((String) edge.get(1)); 
		    }

		    Set<String> nodeNamesInData = new HashSet<>();
		  
		    if (dataJsonObject.containsKey("data")) {
		        JSONArray data = (JSONArray) dataJsonObject.get("data");
		        
		        for (int i = 1; i < data.size(); i++) {
		            JSONArray row = (JSONArray) data.get(i);
		            
		            String nodeName = (String) row.get(0); 
		            nodeNamesInData.add(nodeName);
		        }
		    }		   
		    // The metadata variables: the first cell of each row of the metadata, as the server reads them
		    List<String> metavars = new ArrayList<>();
		    JSONObject metadataJsonObject = mggManager.getMetadataJsonObject();
		    if (metadataJsonObject != null && metadataJsonObject.containsKey("metadata")) {
		        for (Object row : (JSONArray) metadataJsonObject.get("metadata")) {
		            JSONArray cells = (JSONArray) row;
		            if (!cells.isEmpty() && cells.get(0) != null && !cells.get(0).toString().trim().isEmpty()) {
		                metavars.add(cells.get(0).toString().trim());
		            }
		        }
		    }

		    List<String> unknown = new ArrayList<>();
		    for (String node : nodeNamesInNetwork) {
		        if (nodeNamesInData.contains(node)) {
		            continue;
		        }
		        boolean isMetavar = false;
		        for (String metavar : metavars) {
		            if (node.startsWith(metavar)) {
		                isMetavar = true;
		                break;
		            }
		        }
		        if (!isMetavar) {
		            unknown.add(node);
		        }
		    }
		    Collections.sort(unknown);
		    return unknown;
		}
  
	}
	    
