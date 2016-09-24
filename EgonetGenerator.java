/**
 * Version: 1.0
 * Author: Keith Henderson
 * Contact: keith@llnl.gov
 */

import java.util.*;



/**
 * Class for optimizing egonet counting. Calculates summary counts
 * once for each node, even though each node may be used in multiple
 * egonets. By supplying the Egonet object with these counts, it
 * lazily doesn't go calculate them itself. Summary counts are given
 * to the Egonet constructor (for non-attribute counts) and to the
 * getCounts call (for attribute counts).
 * 
 * 
 * @author henderson43
 *
 */
public class EgonetGenerator {
	Map<AttributedNode, Counter<AttributedNode>> inLinks, outLinks;
	Map<AttributedNode, Integer> totalIn, totalOut;
	Map<String, Map<AttributedNode, Counter<AttributedNode>>> inAttr, outAttr;
	Map<String, Map<AttributedNode, Double>> inAttrTotal, outAttrTotal;
	String[] edgeAttrNames, vertAttrNames;
	
	AttributedGraph filteredGraph;
	
	
	/**
	 * 
	 * @param graph AttributedGraph to generate from
	 * @param filterNames names of attributes to filter on
	 * @param filterVals corresponding values; any match will accept the edge
	 * @param edgeAttrNames names of edge attributes to count
	 * @param vertAttrNames names of vertex attributes to count
	 */
	public EgonetGenerator(AttributedGraph graph, 
			String[] filterNames, 
			Object[] filterVals, 
			String[] edgeAttrNames,
			String[] vertAttrNames) {
		if(edgeAttrNames == null) edgeAttrNames = new String[0];
		this.edgeAttrNames = edgeAttrNames;
		

		if(vertAttrNames == null) vertAttrNames = new String[0];
		this.vertAttrNames = vertAttrNames;
		
		if(filterNames == null) {
			filteredGraph = graph;
		}
		else {
			filteredGraph = new AttributedGraph();
			for(AttributedNode node : graph.getNodes()) {
				for(AttributedLink link : node.getOutLinks()) {
					for(int i = 0; i < filterNames.length; i++) {
						if(link.attrs.get(filterNames[i]) == null ? 
								filterVals[i] == null : 
									link.attrs.get(filterNames[i]).equals(
											filterVals[i])) {
							AttributedNode node2 = link.dst;
							Map<String, Object> attrs = link.attrs;
							filteredGraph.addLink(node.id, node2.id, attrs);
							break;
						}
					}
				}
			}
		}

		inLinks = new HashMap<AttributedNode, Counter<AttributedNode>>();
		outLinks = new HashMap<AttributedNode, Counter<AttributedNode>>();
		totalIn = new HashMap<AttributedNode, Integer>();
		totalOut = new HashMap<AttributedNode, Integer>();
		preprocess();
		
		inAttr = new HashMap<String, Map<AttributedNode, Counter<AttributedNode>>>();
		outAttr = new HashMap<String, Map<AttributedNode, Counter<AttributedNode>>>();
		inAttrTotal = new HashMap<String, Map<AttributedNode, Double>>();
		outAttrTotal = new HashMap<String, Map<AttributedNode, Double>>();
		countAttrs();
	}
	
	
	/**
	 * 
	 * @param graph AttributedGraph to generate from
	 * @param filterNames names of attributes to filter on
	 * @param filterVals corresponding values; any match will accept the edge
	 * @param edgeAttrNames names of edge attributes to count
	 */
	public EgonetGenerator(AttributedGraph graph, 
			String[] filterNames, 
			Object[] filterVals, 
			String[] edgeAttrNames) {
		
		this(graph, filterNames, filterVals, edgeAttrNames, null);
		
	}
	
	/**
	 * Assumes no filters and no attributes for counting
	 * 
	 * @param graph graph to generate from
	 */
	public EgonetGenerator(AttributedGraph graph) {
		this(graph, null, null, null, null);
	}
	
	
	
	/**
	 * Gets the counts for the (symmetric) egonet around these nodes (with
	 * specified level). Will use filter and count attributes from instantiation.
	 * 
	 * @param centers nodes at the center of the egonet
	 * @param level how far from centers to travel
	 * @return see Egonet.getCounts()
	 */
	public Map<String, Double> getCounts(Set<Long> centers, int level) {
		String[] attrNames = new String[edgeAttrNames.length + vertAttrNames.length];
		int i = 0;
		for (String attr : edgeAttrNames) attrNames[i++] = attr;
		for (String attr : vertAttrNames) attrNames[i++] = attr;
		
		
		
		Set<AttributedNode> centerNodes = new HashSet<AttributedNode>();
		for(long id : centers) centerNodes.add(filteredGraph.getNode(id));
		return new Egonet(centerNodes, 
				level, 
				inLinks, 
				outLinks, 
				totalIn, 
				totalOut).
				getCounts(attrNames, 
						inAttr, 
						outAttr, 
						inAttrTotal, 
						outAttrTotal);
	}
	
	public Map<String, Double> getCounts(Long center, int level) {
		Set<Long> centers = new HashSet<Long>();
		centers.add(center);
		return getCounts(centers, level);
		
	}
	
	/**
	 * Gets the counts for the left egonet around these nodes (with
	 * specified level). Will use filter and count attributes from instantiation.
	 * 
	 * @param centers nodes at the center of the egonet
	 * @param level how far from centers to travel
	 * @return see Egonet.getCounts()
	 */
	public Map<String, Double> getLeftCounts(Set<Long> centers, int level) {
		Set<AttributedNode> centerNodes = new HashSet<AttributedNode>();
		for(long id : centers) centerNodes.add(filteredGraph.getNode(id));
		return new LeftEgonet(centerNodes, 
				level, 
				inLinks, 
				outLinks, 
				totalIn, 
				totalOut).
				getCounts(edgeAttrNames, 
						inAttr, 
						outAttr, 
						inAttrTotal, 
						outAttrTotal);
	}
	
	public Map<String, Double> getLeftCounts(Long center, int level) {
		Set<Long> centers = new HashSet<Long>();
		centers.add(center);
		return getLeftCounts(centers, level);
	}
	
	/**
	 * Gets the counts for the right egonet around these nodes (with
	 * specified level). Will use filter and count attributes from instantiation.
	 * 
	 * @param centers nodes at the center of the egonet
	 * @param level how far from centers to travel
	 * @return see Egonet.getCounts()
	 */
	public Map<String, Double> getRightCounts(Set<Long> centers, int level) {
		Set<AttributedNode> centerNodes = new HashSet<AttributedNode>();
		for(long id : centers) centerNodes.add(filteredGraph.getNode(id));
		return new RightEgonet(centerNodes, 
				level, 
				inLinks, 
				outLinks, 
				totalIn, 
				totalOut).
				getCounts(edgeAttrNames, 
						inAttr, 
						outAttr, 
						inAttrTotal, 
						outAttrTotal);
	}
	
	public Map<String, Double> getRightCounts(Long center, int level) {
		Set<Long> centers = new HashSet<Long>();
		centers.add(center);
		return getRightCounts(centers, level);	
	}
	
	
	public int getExternalNodes(Set<Long> centers, int level) {
		Set<AttributedNode> centerNodes = new HashSet<AttributedNode>();
		for(long id : centers) centerNodes.add(filteredGraph.getNode(id));
		return new Egonet(centerNodes, 
				level, 
				inLinks, 
				outLinks, 
				totalIn, 
				totalOut).getNumExternalNodes();
	}
	
	public int getExternalNodes(Long center, int level) {
		Set<Long> centers = new HashSet<Long>();
		centers.add(center);
		return getExternalNodes(centers, level);
	}
	
	public int getLeftExternalNodes(Set<Long> centers, int level) {
		Set<AttributedNode> centerNodes = new HashSet<AttributedNode>();
		for(long id : centers) centerNodes.add(filteredGraph.getNode(id));
		return new LeftEgonet(centerNodes, 
				level, 
				inLinks, 
				outLinks, 
				totalIn, 
				totalOut).getNumExternalNodes();
	}
	
	public int getLeftExternalNodes(Long center, int level) {
		Set<Long> centers = new HashSet<Long>();
		centers.add(center);
		return getLeftExternalNodes(centers, level);
	}
	
	public int getRightExternalNodes(Set<Long> centers, int level) {
		Set<AttributedNode> centerNodes = new HashSet<AttributedNode>();
		for(long id : centers) centerNodes.add(filteredGraph.getNode(id));
		return new RightEgonet(centerNodes, 
				level, 
				inLinks, 
				outLinks, 
				totalIn, 
				totalOut).getNumExternalNodes();
	}
	
	public int getRightExternalNodes(Long center, int level) {
		Set<Long> centers = new HashSet<Long>();
		centers.add(center);
		return getRightExternalNodes(centers, level);
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
		for(AttributedNode node : filteredGraph.getNodes()) {
			preprocess(node);
		}
	}
	
	/*
	 * To speed up egonet calculation, we want to summarize all the edges between a pair
	 * of nodes by counting them. We also want the total count for each node (in and out).
	 */
	protected void countAttrs(AttributedNode node) {
		for(String attrName : edgeAttrNames) {
			if(!inAttr.containsKey(attrName)) {
				inAttr.put(attrName, 
						new HashMap<AttributedNode, 
						Counter<AttributedNode>>());
				outAttr.put(attrName, 
						new HashMap<AttributedNode, 
						Counter<AttributedNode>>());
				inAttrTotal.put(attrName,
						new HashMap<AttributedNode, Double>());
				outAttrTotal.put(attrName,
						new HashMap<AttributedNode, Double>());
			}
			Counter<AttributedNode> nodeInLinks = new Counter<AttributedNode>();
			Counter<AttributedNode> nodeOutLinks = new Counter<AttributedNode>();
			double nodeTotalIn = 0, nodeTotalOut = 0;
			
			for(AttributedLink link : node.getInLinks()) {
				Double x = link.attrs.containsKey(attrName) ? 
						(Double)link.attrs.get(attrName) : 0.0;
				nodeInLinks.increment(link.src, x);
				nodeTotalIn += x;
			}
			for(AttributedLink link : node.getOutLinks()) {
				Double x = link.attrs.containsKey(attrName) ? 
						(Double)link.attrs.get(attrName) : 0.0;
				nodeOutLinks.increment(link.dst, x);
				nodeTotalOut += x;
			}
			inAttr.get(attrName).put(node, nodeInLinks);
			outAttr.get(attrName).put(node, nodeOutLinks);
			inAttrTotal.get(attrName).put(node, nodeTotalIn);
			outAttrTotal.get(attrName).put(node, nodeTotalOut);
		}
		for(String attrName : vertAttrNames) {
			if(!inAttr.containsKey(attrName)) {
				inAttr.put(attrName, 
						new HashMap<AttributedNode, 
						Counter<AttributedNode>>());
				outAttr.put(attrName, 
						new HashMap<AttributedNode, 
						Counter<AttributedNode>>());
				inAttrTotal.put(attrName,
						new HashMap<AttributedNode, Double>());
				outAttrTotal.put(attrName,
						new HashMap<AttributedNode, Double>());
			}
			Counter<AttributedNode> nodeInLinks = new Counter<AttributedNode>();
			Counter<AttributedNode> nodeOutLinks = new Counter<AttributedNode>();
			double nodeTotalIn = 0, nodeTotalOut = 0;
			
			Set<Long> seen = new HashSet<Long>();
			for(AttributedLink link : node.getInLinks()) {
				if(seen.contains(link.src.id)) continue;
				seen.add(link.src.id);
				Double x = link.src.attrs.containsKey(attrName) ? 
						(Double)link.src.attrs.get(attrName) : 0.0;
				nodeInLinks.increment(link.src, x);
				nodeTotalIn += x;
			}
			seen.clear();
			for(AttributedLink link : node.getOutLinks()) {
				if(seen.contains(link.dst.id)) continue;
				seen.add(link.dst.id);
				Double x = link.dst.attrs.containsKey(attrName) ? 
						(Double)link.dst.attrs.get(attrName) : 0.0;
				nodeOutLinks.increment(link.dst, x);
				nodeTotalOut += x;
			}
			inAttr.get(attrName).put(node, nodeInLinks);
			outAttr.get(attrName).put(node, nodeOutLinks);
			inAttrTotal.get(attrName).put(node, nodeTotalIn);
			outAttrTotal.get(attrName).put(node, nodeTotalOut);
		}
	}
	

	// Preprocess any necessary nodes
	protected void countAttrs() {
		for(AttributedNode node : filteredGraph.getNodes()) {
			countAttrs(node);
		}
	}
	
}
