/**
 * Version: 1.0
 * Author: Keith Henderson
 * Contact: keith@llnl.gov
 */

import java.util.*;

/**
 * This class defines an egonet, i.e. the subgraph defined by a node,
 * its neighbors, and any induced edges among these nodes. Carries
 * some bookkeeping information about links entering and exiting the
 * egonet. Egonet properties (feature names) are described in the
 * comments below.
 */
public class Egonet{
	protected Set<AttributedNode> nodes = new HashSet<AttributedNode>();
	protected Map<AttributedNode, Counter<AttributedNode>> inLinks, outLinks;
	protected Map<AttributedNode, Integer> totalIn, totalOut;
	
	Map<String, Map<AttributedNode, Counter<AttributedNode>>> inAttr, outAttr;
	Map<String, Map<AttributedNode, Double>> inAttrTotal, outAttrTotal;
	
	protected Egonet() {
		// for subclasses
	}
	
	
	
	/**
	 * Constructs egonet around a center node. The k-level egonet
	 * is the center node plus any node that can be reached from
	 * the center in k or fewer hops, travelling across edges in
	 * either direction.
	 * 
	 * This constructor can be used to optimize if many egonets
	 * are going to be constructed from the same graph. To do so,
	 * populate inLinks, outLinks, totalIn and totalOut for all
	 * nodes in the graph. This avoids recalculation.
	 * 
	 *  
	 * @param centers
	 * @param level
	 * @param inLinks
	 * @param outLinks
	 * @param totalIn
	 * @param totalOut
	 */
	public Egonet(Set<AttributedNode> centers, int level,
			Map<AttributedNode, Counter<AttributedNode>> inLinks,
			Map<AttributedNode, Counter<AttributedNode>> outLinks,
			Map<AttributedNode, Integer> totalIn,
			Map<AttributedNode, Integer> totalOut) {
		
		// null arguments -> empty maps
		if(inLinks == null) inLinks = 
			new HashMap<AttributedNode, Counter<AttributedNode>>();
		if(outLinks == null) outLinks = 
			new HashMap<AttributedNode, Counter<AttributedNode>>();
		if(totalIn == null) totalIn = 
			new HashMap<AttributedNode, Integer>();
		if(totalOut == null) totalOut = 
			new HashMap<AttributedNode, Integer>();

		this.inLinks = inLinks;
		this.outLinks = outLinks;
		this.totalIn = totalIn;
		this.totalOut = totalOut;
		
		inAttr = new HashMap<String, Map<AttributedNode, Counter<AttributedNode>>>();
		outAttr = new HashMap<String, Map<AttributedNode, Counter<AttributedNode>>>();
		inAttrTotal = new HashMap<String, Map<AttributedNode, Double>>();
		outAttrTotal = new HashMap<String, Map<AttributedNode, Double>>();
		
		// Add center nodes and do BFS iters to get egonet nodes.
		nodes.addAll(centers);
		AttributedNode neighbor;
		Set<AttributedNode> closedSet = new HashSet<AttributedNode>();
		for(int i = 0; i < level; i++) {
			Set<AttributedNode> oldNodes = new HashSet<AttributedNode>(nodes);
			for(AttributedNode node : oldNodes) {
				if(closedSet.contains(node)) continue;
				for(AttributedLink link : node.getLinks()) {
					neighbor = link.src.equals(node) ? link.dst : link.src;
					nodes.add(neighbor);
				}
				closedSet.add(node);
			}
		}
		
		// Preprocess any necessary nodes
		preprocess();
	}
	
	/**
	 * Constructs egonet around a center node. The k-level egonet is the center
	 * node plus any node that can be reached from the center in k or fewer hops,
	 * travelling across edges in either direction.
	 * 
	 * @param centers nodes at the center of the egonet.
	 * @param level how many hops are allowed
	 */
	public Egonet(Set<AttributedNode> centers, int level) {
		this(centers, level,
				new HashMap<AttributedNode, Counter<AttributedNode>>(),
				new HashMap<AttributedNode, Counter<AttributedNode>>(),
				new HashMap<AttributedNode, Integer>(),
				new HashMap<AttributedNode, Integer>());
	}
	
	/**
	 * Constructs egonet around a center node. The k-level egonet
	 * is the center node plus any node that can be reached from
	 * the center in k or fewer hops, travelling across edges in
	 * either direction.
	 * 
	 * @param center node at the center of the egonet.
	 * @param level how many hops are allowed
	 */
	public Egonet(AttributedNode center, int level) {
		this(makeSetFromInstance(center), level);
	}
	
	public Set<AttributedNode> getNodes() {
		return new HashSet<AttributedNode>(nodes);
	}
	
	public int size() {
		return nodes.size();
	}
	
	public Map<String, Double> getCounts() {
		
		return getCounts(new String[0]);
	}
	
	
	/**
	 * Counts several egonet properties. In particular:
	 * 
	 * wn - Within Node - # of nodes in egonet
	 * weu - Within Edge Unique - # of unique edges with both ends in egonet
	 * wet - Within Edge Total - total # of internal edges
	 * xesu - eXternal Edge Source Unique - # of unique edges exiting egonet
	 * xest - eXternal Edge Source Total - total # of edges exiting egonet
	 * xedu - eXternal Edge Destination Unique - # of unique edges entering egonet
	 * xedt - eXternal Edge Destination Total - total # of edges entering egonet
	 * 
	 * and three counts per attribute,
	 * 
	 * wea-ATTRNAME - Within Edge Attribute - sum of attribute for internal edges
	 * xesa-ATTRNAME - eXternal Edge Source Attribute - sum of attr for exiting edges
	 * xeda-ATTRNAME - eXternal Edge Destination Attribute - sum of attr for entering edges
	 * xea-ATTRNAME - sum of xeda and xesa
	 * 
	 * @param attrNames names of any attributes to be calculated. Values are converted using (Double)
	 * @param inAttr attribute name -> target node -> source node -> sum(attr) of edges
	 * @param outAttr attribute name -> source node -> target node -> sum(attr) of edges
	 * @param inAttrTotal attribute name -> target node -> sum(attr) of all edges
	 * @param outAttrTotal attribute name -> source node -> sum(attr) of all edges
	 * 
	 * @return Map from property abbreviations (e.g. "xedu" = eXternal Edge Destination Unique) to counts
	 */
	
	
	public Map<String, Double> getCounts(String[] attrNames, 
			Map<String, Map<AttributedNode, Counter<AttributedNode>>> inAttr,
			Map<String, Map<AttributedNode, Counter<AttributedNode>>> outAttr,
			Map<String, Map<AttributedNode, Double>> inAttrTotal,
			Map<String, Map<AttributedNode, Double>> outAttrTotal) {

		if(attrNames == null) attrNames = new String[0];
		
		Counter<String> counts = new Counter<String>();
		
		Map<String, Double> ret = new HashMap<String, Double>();
		ret.put("wn", 0.);
		ret.put("weu", 0.);
		ret.put("wet", 0.);
		ret.put("xesu", 0.);
		ret.put("xest", 0.);
		ret.put("xedu", 0.);
		ret.put("xedt", 0.);
		ret.put("xeu", 0.);
		ret.put("xet", 0.);
		for(String attr : attrNames) {
			ret.put("wea-" + attr, 0.);
			ret.put("xesa-" + attr, 0.);
			ret.put("xeda-" + attr, 0.);
			ret.put("xea-" + attr, 0.);
		}
		
		
		/*
		 * Look at each node, updating counts for Within (w*) and eXternal (x*) counts.
		 */
		
		for(AttributedNode n1 : nodes) {
			// wn (within nodes) is just this.size() after loop.
			counts.increment("wn");
			

			/*
			 * Update counts based on incoming links for this node. If the egonet
			 * is bigger than this nodes neighbor set, we will iterate over the node's
			 * neighbor set. Otherwise, we will cleverly iterate over the egonet instead.
			 * This results in an expected speedup, but makes it impossible to calculate
			 * xn (external nodes). xn should be explicitly calculated by generating the
			 * k+1 egonet and getting its size() or calling getNumExternalNodes().
			 */
			Counter<AttributedNode> nodeInLinks = inLinks.get(n1);
			Set<AttributedNode> inNeighbors = nodeInLinks.keySet();
			
			if(inNeighbors.size() < nodes.size()) { // Egonet is bigger than this node's neighbors
				for(AttributedNode n2 : inNeighbors) {
					if(nodes.contains(n2)) {
//						// Internal node -- update counts accordingly.
						counts.increment("weu");
						counts.increment("wet", nodeInLinks.get(n2));
						for(String attr : attrNames) {
							counts.increment("wea-"+attr, 
									inAttr.get(attr).get(n1).get(n2));
						}
					}
					else {
//						// External node -- update counts accordingly.
						counts.increment("xedu");
						counts.increment("xedt", nodeInLinks.get(n2));
						for(String attr : attrNames) {
							counts.increment("xeda-"+attr, 
									inAttr.get(attr).get(n1).get(n2));
						}
					}
				}
			}
			else {	// This node has more neighbors than the egonet has nodes.
				/* 
				 * Assume all neighbors are external, correct later 
				 * by iterating over internal nodes explicitly.
				 */
				counts.increment("xedu", 1.0*inNeighbors.size());
				counts.increment("xedt", 1.0*totalIn.get(n1));
				for(String attr : attrNames) {
					counts.increment("xeda-"+attr, 
							inAttrTotal.get(attr).get(n1));
				}
				for(AttributedNode n2 : nodes) {
					if(inNeighbors.contains(n2)) {
						// Internal node -- update counts accordingly.
						counts.increment("weu");
						counts.increment("wet", nodeInLinks.get(n2));
						for(String attr : attrNames) {
							counts.increment("wea-"+attr, 
									inAttr.get(attr).get(n1).get(n2));
						}
						
						// We overcounted this as an external node.
						counts.decrement("xedu");
						counts.decrement("xedt", nodeInLinks.get(n2));
						for(String attr : attrNames) {
							counts.decrement("xeda-"+attr, 
									inAttr.get(attr).get(n1).get(n2));
						}
					}
					else {
						// Do nothing. we already counted this external node.
					}
				}
			}
			
			/*
			 * Same for outgoing links -- only need to complete xesu and xest since
			 * internal (w*) counts were done above.
			 */
			Counter<AttributedNode> nodeOutLinks = outLinks.get(n1);
			Set<AttributedNode> outNeighbors = nodeOutLinks.keySet();
			
			// If Egonet is bigger than this node's neighbors
			if(outNeighbors.size() < nodes.size()) { 
				for(AttributedNode n2 : outNeighbors) {
					if(nodes.contains(n2)) {
						// Already counted these in the inLinks for some other egonet node
//						counts.increment("weu");
//						counts.increment("wet", outLinks.get(n2));
					}
					else {
						
						// External node -- update counts accordingly.
						counts.increment("xesu");
						counts.increment("xest", nodeOutLinks.get(n2));
						for(String attr : attrNames) {
							counts.increment("xesa-"+attr, 
									outAttr.get(attr).get(n1).get(n2));
						}
					}
				}
			}
			else {	// This node has more neighbors than the egonet has nodes.
				/* 
				 * Assume all neighbors are external, correct later 
				 * by iterating over internal nodes explicitly.
				 */
				counts.increment("xesu", 1.0*outNeighbors.size());
				counts.increment("xest", 1.0* totalOut.get(n1));
				for(String attr : attrNames) {
					counts.increment("xesa-"+attr, 
							outAttrTotal.get(attr).get(n1));
				}
				for(AttributedNode n2 : nodes) {
					if(outNeighbors.contains(n2)) {
						// Already counted these in the inLinks for some other egonet node
//						counts.increment("weu");
//						counts.increment("wet", outLinks.get(n2));
						
						// We overcounted this as an external node.
						counts.decrement("xesu");
						counts.decrement("xest", nodeOutLinks.get(n2));
						for(String attr : attrNames) {
							counts.decrement("xesa-"+attr, 
									outAttr.get(attr).get(n1).get(n2));
						}
					}
					else {
						// Do nothing. we already counted this external node.
					}
				}
			}
			
		}
		
		
		for(String key : counts.keySet()) {
			ret.put(key, counts.get(key).doubleValue());
		}
		ret.put("xeu", ret.get("xesu") + ret.get("xedu"));
		ret.put("xet", ret.get("xest") + ret.get("xedt"));
		for(String attr : attrNames) {
			ret.put("xea-" + attr, ret.get("xesa-" + attr) + ret.get("xeda-"+attr));
		}
		
		return ret;
	}
			
	
	
	/**
	 * Counts several egonet properties. In particular:
	 * 
	 * wn - Within Node - # of nodes in egonet
	 * weu - Within Edge Unique - # of unique edges with both ends in egonet
	 * wet - Within Edge Total - total # of internal edges
	 * xesu - eXternal Edge Source Unique - # of unique edges exiting egonet
	 * xest - eXternal Edge Source Total - total # of edges exiting egonet
	 * xedu - eXternal Edge Destination Unique - # of unique edges entering egonet
	 * xedt - eXternal Edge Destination Total - total # of edges entering egonet
	 * 
	 * and three counts per attribute,
	 * 
	 * wea-ATTRNAME - Within Edge Attribute - sum of attribute for internal edges
	 * xesa-ATTRNAME - eXternal Edge Source Attribute - sum of attr for exiting edges
	 * xeda-ATTRNAME - eXternal Edge Destination Attribute - sum of attr for entering edges
	 * 
	 * @param attrNames names of any attributes to be calculated. Values are converted using (Double)
	 * @return Map from property abbreviations (e.g. "xedu" = eXternal Edge Destination Unique) to counts
	 */
	public Map<String, Double> getCounts(String[] attrNames) {
		if(attrNames == null) attrNames = new String[0];
		countAttrs(attrNames);
		return getCounts(attrNames, inAttr, outAttr, inAttrTotal, outAttrTotal);

	}
	
	/**
	 * Slow method for calculating "xn" property, the number of external 
	 * nodes. Must inspect each neighbor of each egonet node, whereas
	 * getCounts takes a shortcut for nodes whose neighborhood is bigger than
	 * the egonet.
	 * 
	 * @return number of unique nodes that are one hop (in either direction) away from egonet
	 */
	public int getNumExternalNodes() {
		Set<AttributedNode> extNodes = new HashSet<AttributedNode>();
		for(AttributedNode node : nodes) {
			extNodes.addAll(node.getUniqueNeighbors());
		}
		extNodes.removeAll(nodes);
		return extNodes.size();
	}

	/*
	 * To speed up egonet calculation, we want to summarize all the edges between a pair
	 * of nodes by counting them. We also want the total count for each node (in and out).
	 */
	protected void preprocess(AttributedNode node) {
		Counter<AttributedNode> nodeInLinks = new Counter<AttributedNode>();
		Counter<AttributedNode> nodeOutLinks = new Counter<AttributedNode>();
		int nodeTotalIn = 0, nodeTotalOut = 0;

		for(AttributedLink link : node.getInLinks()) {
			nodeInLinks.increment(link.src);
				nodeTotalIn++;
		}
		for(AttributedLink link : node.getOutLinks()) {
			nodeOutLinks.increment(link.dst);
			nodeTotalOut++;
		}
		inLinks.put(node, nodeInLinks);
		outLinks.put(node, nodeOutLinks);
		totalIn.put(node, nodeTotalIn);
		totalOut.put(node, nodeTotalOut);
	}
	

	// Preprocess any necessary nodes
	protected void preprocess() {
		for(AttributedNode node : nodes) {
			if(!inLinks.containsKey(node) || !outLinks.containsKey(node) ||
					!totalIn.containsKey(node) || !totalOut.containsKey(node)) {
				preprocess(node);
			}
		}
	}
	
	protected void countAttrs(String[] attrNames, AttributedNode node) {
		for(String attrName : attrNames) {
			if(!inAttr.containsKey(attrName)) {
				inAttr.put(attrName.intern(), 
						new HashMap<AttributedNode, 
						Counter<AttributedNode>>());
				outAttr.put(attrName.intern(), 
						new HashMap<AttributedNode, 
						Counter<AttributedNode>>());
				inAttrTotal.put(attrName.intern(),
						new HashMap<AttributedNode, Double>());
				outAttrTotal.put(attrName.intern(),
						new HashMap<AttributedNode, Double>());
			}
			Counter<AttributedNode> nodeInLinks = new Counter<AttributedNode>();
			Counter<AttributedNode> nodeOutLinks = new Counter<AttributedNode>();
			double nodeTotalIn = 0, nodeTotalOut = 0;
			
			for(AttributedLink link : node.getInLinks()) {
				nodeInLinks.increment(link.src, (Double)link.attrs.get(attrName));
				nodeTotalIn+=(Double)link.attrs.get(attrName);
			}
			for(AttributedLink link : node.getOutLinks()) {
				nodeOutLinks.increment(link.dst, (Double)link.attrs.get(attrName));
				nodeTotalOut+=(Double)link.attrs.get(attrName);
			}
			inAttr.get(attrName).put(node, nodeInLinks);
			outAttr.get(attrName).put(node, nodeOutLinks);
			inAttrTotal.get(attrName).put(node, nodeTotalIn);
			outAttrTotal.get(attrName).put(node, nodeTotalOut);
		}
	}
	

	// Preprocess any necessary nodes
	protected void countAttrs(String[] attrNames) {
		for(AttributedNode node : nodes) {
			countAttrs(attrNames, node);
		}
	}
	
	
	protected static Set<AttributedNode> makeSetFromInstance(AttributedNode instance) {
		HashSet<AttributedNode> ret = new HashSet<AttributedNode>();
		ret.add(instance);
		return ret;
	}
}
