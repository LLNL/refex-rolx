/**
 * Version: 1.0
 * Author: Keith Henderson
 * Contact: keith@llnl.gov
 */

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

/**
 * This class defines a graph whose nodes and links can have
 * attributes attached.
 */

public class AttributedGraph {

  private Map<String,Map<Object,Set<AttributedNode>>> nodeIndices =
    new HashMap<String,Map<Object,Set<AttributedNode>>>();
  
  private long nextNodeId = 0;  
  private Map<Long,AttributedNode> idToNode = new HashMap<Long,AttributedNode>();
  private Set<AttributedLink> links = new HashSet<AttributedLink>();
  private long startTime;
  
  /**
   * Default constructor.
   */
  public AttributedGraph() {  
  }  
  
  /**
   * Copy constructor.
   */
  public AttributedGraph(AttributedGraph graph) {
        
    this.nextNodeId = graph.nextNodeId;     
    this.startTime = graph.startTime;
        
    // copy nodes
    for (Map.Entry<Long,AttributedNode> nodeEntry : graph.idToNode.entrySet()) {
      Long id = nodeEntry.getKey();
      AttributedNode origNode = nodeEntry.getValue();     
      
      AttributedNode nodeCopy = new AttributedNode(origNode);
      idToNode.put(id, nodeCopy);
    }
    
    // copy links and update node/link connections
    for (AttributedLink origLink : graph.links) {
      
      // copy link      
      AttributedNode srcCopy = idToNode.get(origLink.src.id);
      AttributedNode dstCopy = idToNode.get(origLink.dst.id);
           
      addLink(srcCopy, dstCopy, origLink.attrs);     
    }
  }
  
  /**
   * Get the number of nodes in the graph.
   */
  public int getNumNodes() {
    return idToNode.size();
  }
  
  /**
   * Get the number of links in the graph.
   */
  public int getNumLinks() {
    return links.size();
  }
  
  /**
   * Get collection of node identifiers.
   */
  public Collection<AttributedNode> getNodes() {
    return idToNode.values();
  }      
  
  /**
   * Get collection of links.
   */
  public Collection<AttributedLink> getLinks() {
    return links;
  } 
  
  /**
   * Copy link from another graph. Nodes are copied as necessary.
   * 
   * We assume that node id's are the same between graphs.
   */
  public AttributedLink addLink(AttributedLink link) {
    // add nodes if necessary
    AttributedNode srcNode = idToNode.get(link.src.id);
    if (srcNode == null) {
      srcNode = new AttributedNode(link.src);
      idToNode.put(srcNode.id, srcNode);   
    }
    
    AttributedNode dstNode = idToNode.get(link.dst.id);
    if (dstNode == null) {
     dstNode = new AttributedNode(link.dst);
     idToNode.put(dstNode.id, dstNode);
    }
    
    // add link
    return addLink(srcNode, dstNode, link.attrs);
  }
    
  
  /**
   * Add a new link between the specified nodes. If nodes don't exist in graph, create them.
   */
  public AttributedLink addLink(long src, long dst, Map<String,Object> attrs){
    
    // make sure auto-assigned node ids don't conflict user-specified ids
    if (src > nextNodeId) {
      nextNodeId = src + 1;
    }
    if (dst > nextNodeId) {
      nextNodeId = dst + 1;
    }
    
    // don't allow null attrs object
    if (attrs == null) {
      attrs = new HashMap<String,Object>();
    }       
    
    // add link
    return addLink(getNode(src), getNode(dst), attrs); 
  }  
    
  protected AttributedLink addLink(
      AttributedNode srcNode, AttributedNode dstNode, Map<String,Object> attrs) {
    
    AttributedLink link = new AttributedLink(srcNode, dstNode, attrs); 
    
    links.add(link);
    
    // update node in/out links      
    if (srcNode == dstNode) {
      srcNode.addSelfLoop(link);      
    } else {
      srcNode.addOutLink(link);
      dstNode.addInLink(link);
    }   
    
    return link;
  }
  
  protected void removeLink(AttributedLink link) {
        
    links.remove(link);
       
    // update node in/out links      
    if (link.src == link.dst) {
      link.src.removeSelfLoop(link);           
    } else {
      link.src.removeOutLink(link);
      link.dst.removeInLink(link);
      
      // remove destination node if it has no links
      if (link.dst.numAdjacentLinks() < 1) {
        idToNode.remove(link.dst.id);
      }
    }           
    
    // remove source node if it has no links
    if (link.src.numAdjacentLinks() < 1) {  
      idToNode.remove(link.src.id);
    }  
  }
  
  /**
   * If a node with the specified id exists, it is returned. Otherwise, a new
   * node is created with this id and then returned.
   */
  protected AttributedNode getNode(Long id) {
    
    AttributedNode node = idToNode.get(id);
    if (node == null) {
      node = addNode(id);
    }
    
    return node;
  }
  
  private AttributedNode addNode(Long id) {
    AttributedNode node = new AttributedNode(id);      
    idToNode.put(id, node);
    return node;
  }
  
  public long getIntervalStartTime() {
    return startTime;
  }

  public void setIntervalStartTime(long startTime) {
    this.startTime = startTime;    
  }
  
  /**
   * Get node that unique matches the specified attribute value. If more than
   * one node matches, an RuntimeException is thrown.
   */
  public AttributedNode getNode(String attrName, Object attrVal) {
    Collection<AttributedNode> nodes = getNodes(attrName, attrVal);
    
    if (nodes.size() > 1) {
      throw new RuntimeException(
          "Multiple matching nodes found for attribute='" +
          attrName +
          "', value='" +
          attrVal +
          "'.");
    }
    
    if (nodes.isEmpty()) {
      return null;
    } else {
      return nodes.iterator().next();
    }  
  }
  
  /**
   * Add a new node to the graph.
   */
  public AttributedNode addNode() {        
    return addNode(nextNodeId++);
  }
  
  /**
   * Get collection of nodes that match the specified attribute value.
   */
  public Set<AttributedNode> getNodes(String attrName, Object attrVal) {

    // if this attribute is indexed, just lookup nodes in index
    Map<Object,Set<AttributedNode>> index = nodeIndices.get(attrName);    
    if (index != null) {
      Set<AttributedNode> nodes = index.get(attrVal);
      if (nodes == null) {
        return new HashSet<AttributedNode>();
      } else {
        return nodes;
      }      
    }    
    
    // otherwise, use sequential search
    Set<AttributedNode> nodes = new HashSet<AttributedNode>();
    for (AttributedNode node : getNodes()) {                  
      Object nodeAttrVal = node.getAttr(attrName);
      if (nodeAttrVal != null && nodeAttrVal.equals(attrVal)) {
        nodes.add(node);
      }
    }
    
    return nodes;
  }
  
  public void buildNodeIndex(String attrName) {
    
    // create index and add it to collection of indices
    Map<Object,Set<AttributedNode>> index =
      new HashMap<Object,Set<AttributedNode>>();
    
    nodeIndices.put(attrName, index);
    
    // build index
    for (AttributedNode node : getNodes()) {                  
      Object nodeAttrVal = node.getAttr(attrName);
      Set<AttributedNode> nodes = index.get(nodeAttrVal);
      if (nodes == null) {
        nodes = new HashSet<AttributedNode>();
        index.put(nodeAttrVal, nodes);
      }
      nodes.add(node);
    }    
  }
  
  public void updateIndex(AttributedNode node) {
    for (Entry<String,Object> attr : node.attrs.entrySet()) {
      String attrName = attr.getKey();
      Object attrVal = attr.getValue();
      if (attrVal != null) {
        Map<Object,Set<AttributedNode>> index = nodeIndices.get(attrName);
        if (index != null) {
          Set<AttributedNode> nodes = index.get(attrVal);
          if (nodes == null) {
            nodes = new HashSet<AttributedNode>();
            index.put(attrVal, nodes);
          }
          nodes.add(node);          
        }
      }
    }
  }
  
  public void removeNodeIndex(String attrName) {
    nodeIndices.put(attrName, null);    
  }
}
