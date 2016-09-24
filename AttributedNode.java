/**
 * Version: 1.0
 * Author: Keith Henderson
 * Contact: keith@llnl.gov
 */

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This class defines a node in an AttributedGraph. Each node has a
 * collection of attributes indexed by Strings, as well as outgoing
 * and incoming AttributedLinks.
 */
public class AttributedNode {  
  public final long id;
  
  public Map<String,Object> attrs = new HashMap<String,Object>();  
  private int selfLoopCount = 0;
  private Set<AttributedLink> inLinks = new HashSet<AttributedLink>();
  private Set<AttributedLink> outLinks = new HashSet<AttributedLink>();   

  /**
   * Default constructor.
   */
  public AttributedNode(long id) {
    this.id = id;
  }
  
  /**
   * Create new node and copy attribute values from existing node.
   * Links are not copied.
   */
  public AttributedNode(AttributedNode origNode) {
    this.id = origNode.id;
    
    for (Map.Entry<String,Object> entry : origNode.attrs.entrySet()) {
      attrs.put(entry.getKey(), entry.getValue());
    }          
  }

  public String toString() {
    StringBuffer sb = new StringBuffer();
    
    boolean first = true;
    for (Map.Entry<String,Object> attr : attrs.entrySet()) {
      if (!first) {
        sb.append(", ");       
      } else {
        first = false;
      }
      
      sb.append(attr.getValue());
    }
    
    //return sb.toString();
    return (String)getAttr("nodeID");
  }
  
  /*package*/ void addInLink(AttributedLink link) {      
    inLinks.add(link);      
  }
  
  /*package*/ void addOutLink(AttributedLink link) {      
    outLinks.add(link);
  }    
  
  /*package*/ void addSelfLoop(AttributedLink link) {  
    inLinks.add(link);
    outLinks.add(link);    
    ++selfLoopCount;
  }    
  
  /*package*/ void removeInLink(AttributedLink link) {      
    inLinks.remove(link);
  }
  
  /*package*/ void removeOutLink(AttributedLink link) {      
    outLinks.remove(link);
  }    
  
  /*package*/ void removeSelfLoop(AttributedLink link) {  
    inLinks.remove(link);
    outLinks.remove(link);    
    --selfLoopCount;
  }
  
  public int numAdjacentNodes() {
    return getUniqueNeighbors().size();
  }
  
  public int numInLinks() {
    return inLinks.size();
  }
  
  public int numOutLinks() {
    return outLinks.size();
  }
  
  public int numAdjacentLinks() {
    return inLinks.size() + outLinks.size() - selfLoopCount;
  }
  
  public Set<AttributedLink> getInLinks() {
    return inLinks;
  }
  
  public Set<AttributedLink> getOutLinks() {
    return outLinks;
  }
  
  /** 
   * Get all links (in and out).
   */
  public Set<AttributedLink> getLinks() {
    Set<AttributedLink> links = new HashSet<AttributedLink>(inLinks);
    links.addAll(outLinks);
    return links;
  }
  
  public Set<AttributedNode> getUniqueNeighbors() {
    Set<AttributedNode> neighbors = new HashSet<AttributedNode>();
    
    for (AttributedLink link : inLinks) {
      neighbors.add(link.src);
    }
    
    for (AttributedLink link : outLinks) {
      neighbors.add(link.dst);
    }    
    
    return neighbors;
  }  
  
  public Object getAttr(String name) {
    return attrs.get(name);
  }
  
  public void setAttr(String name, Object value) {
    attrs.put(name.intern(), value);
  }
}
