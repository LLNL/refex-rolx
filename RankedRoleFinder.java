/**
 * Version: 1.0
 * Author: Keith Henderson
 * Contact: keith@llnl.gov
 */

import java.io.*;
import java.text.*;
import java.util.*;


/**
 * This class contains the primary logic for generating feature values
 * fo a given graph.
 */
public class RankedRoleFinder {

	private static final int MAX_ITERATIONS = 100;
	private static final double TOLERANCE = 0.01;

	private static Map<String, Map<Double, Set<AttributedNode>>> sortedAttrSets = 
		new HashMap<String, Map<Double, Set<AttributedNode>>>();
	private static Map<String, Double> maxBins = new HashMap<String, Double>();
	private static Map<String, Map<String, Boolean>> memoizedMatches = 
		new HashMap<String, Map<String, Boolean>>();
	
	// to see how much time we save over correlation
	private static double checks = 0;
	private static double corrChecks = 0;
	
	static String[] firstIteration(AttributedGraph graph, 
				       Collection<AttributedNode> nodes) {
		if(nodes == null)
			nodes = graph.getNodes();

		String[] properties = {
				"wn",
				"weu",
				"wea-wgt",
				"wem",
				"xesu",
				"xesa-wgt",
				"xesm",
				"xedu",
				"xeda-wgt",
				"xedm",
				"xeu",
				"xea-wgt",
				"xem",
		};


		String[] ret = new String[properties.length*2];
		int k = 0;
		for(String property : properties) {
			ret[k] = property.replace("a-wgt", "t") + "0";
			ret[(k++) + properties.length] = property.replace("a-wgt", "t") + "1";
		}


		EgonetGenerator egoGen = new EgonetGenerator(graph, null, null, new String[]{"wgt"});
		for(AttributedNode node : nodes) {
			Map<String, Double> counts; 

			counts = egoGen.getCounts(node.id, 0);
			for(String base : new String[]{"we", "xes", "xed", "xe"}) {
				if(counts.get(base+"u") > 0) {
					counts.put(base+"m", counts.get(base+"a-wgt")/counts.get(base+"u"));
				}
				else {
					counts.put(base+"m", 0.0);
				}
			}
			for(String property : properties) {
				node.setAttr(property.replace("a-wgt", "t")+'0', counts.get(property));
			}

			counts = egoGen.getCounts(node.id, 1);
			for(String base : new String[]{"we", "xes", "xed", "xe"}) {
				if(counts.get(base+"u") > 0) {
					counts.put(base+"m", counts.get(base+"a-wgt")/counts.get(base+"u"));
				}
				else {
					counts.put(base+"m", 0.0);
				}
			}
			for(String property : properties) {
				node.setAttr(property.replace("a-wgt", "t")+'1', counts.get(property));
			}
			
		}

		return ret;
	}

	static String[] nextIteration(AttributedGraph graph, Collection<AttributedNode> nodes,
			String[] attrs) {
		if(nodes == null)
			nodes = graph.getNodes();

		String[] properties = {
				"xes",
				"xed",
				"xe",
				"wn",
				"wnm"
		};

		
		
		String[] ret = new String[properties.length*2*attrs.length];
		int k = 0;
		for(String attr : attrs) {
			for(String property : properties) {
				ret[k] = property + "0-" + attr.replace("wgt-", "");
				ret[(k++) + properties.length*attrs.length] = 
					property + "1-" + attr.replace("wgt-", "");
			}	
		}

		
		EgonetGenerator egoGen = new EgonetGenerator(graph, null, null, null, attrs);
		

		
		
		Map<String, Double> counts; 
		for(AttributedNode node : nodes) {

			counts = egoGen.getCounts(node.id, 0);
			for(String attr : attrs) {
				counts.put("wna-"+attr, (Double)node.getAttr(attr));

				for(String base : new String[]{"xe", "xes", "xed"}) {
					if(counts.get(base+"u") > 0) {
						counts.put(base+"m-"+attr, counts.get(base+"a-"+attr)/counts.get(base+"u"));
					}
					else {
						counts.put(base+"m-"+attr, 0.0);
					}
				}
				counts.put("wnm-"+attr, counts.get("wna-"+attr) / counts.get("wn"));
			}

			for(String attr : attrs) {
				for(String property : properties) {
					if(property.endsWith("m")) {
						node.setAttr(property+"0-" + attr.replace("wgt-", ""),
								counts.get(property + "-" + attr));
					}
					else {
						node.setAttr(property + "0-" + attr.replace("wgt-", ""),
								counts.get(property + "a-" + attr));
					}
				}
			}

			counts = egoGen.getCounts(node.id, 1);

			for(String attr : attrs) {
				double count = 0;
				for(AttributedLink link : node.getLinks()) {
					AttributedNode neighbor = link.src.equals(node) ? link.dst : link.src;
					count += (Double) neighbor.getAttr(attr);
				}
				counts.put("wna-"+attr, count);


				for(String base : new String[]{"xe", "xes", "xed"}) {
					if(counts.get(base+"u") > 0) {
						counts.put(base+"m-"+attr, counts.get(base+"a-"+attr)/counts.get(base+"u"));
					}
					else {
						counts.put(base+"m-"+attr, 0.0);
					}
				}
				counts.put("wnm-"+attr, counts.get("wna-"+attr) / counts.get("wn"));
			}


			for(String attr : attrs) {
				for(String property : properties) {
					if(property.endsWith("m")) {
						node.setAttr(property+"1-" + attr.replace("wgt-", ""),
								counts.get(property + "-" + attr));
					}
					else {
						node.setAttr(property + "1-" + attr.replace("wgt-", ""),
								counts.get(property + "a-" + attr));
					}
				}
			}
		}

		return ret;
	}

	static String[] calculateAttrs(AttributedGraph graph, 
			Collection<AttributedNode> nodes, 
			String[] attrs) {
		if(attrs == null) {
			return firstIteration(graph, nodes);

		}
		if(attrs.length == 0)
			return attrs;

		if(nodes == null)
			nodes = graph.getNodes();

		return nextIteration(graph, nodes, attrs);
	}


	static String verticalBin(AttributedGraph graph, String attr, double binSize) {

		int numNodes = graph.getNodes().size();
		int added = 0;

		class Pair implements Comparable<Pair> {
			double value;
			AttributedNode node;
			Pair(double value, AttributedNode node) {
				this.value = value;
				this.node = node;
			}
			public int compareTo(Pair o) {
				Pair p = (Pair)o;
				return value - p.value < 0?-1:1;
			}
			public String toString() { 
				return node.getAttr("nodeID") + ": " + value;
			}
		}
		List<Pair> values = new ArrayList<Pair>();
		for(AttributedNode n : graph.getNodes()) {
			if(Math.abs((Double) n.getAttr(attr) - 0) > 1E-5)
				values.add(new Pair((Double)n.getAttr(attr), n));
			else
				n.setAttr("wgt-" + attr, 0.0);
		}
		Collections.sort(values);
		numNodes = values.size();
		if(numNodes == 0) {
			return "wgt-" + attr;
		}
		
		double score = 1;
		int needed = (int)Math.ceil(binSize*(numNodes));
		int thisBin = 0;
		AttributedNode n = values.get(0).node;
		double oldVal = (Double) n.getAttr(attr), newVal;
		n.setAttr("wgt-" + attr, score);
		thisBin++;
		added++;
		while(added < numNodes) {
			n = values.get(added).node;
			newVal = (Double) n.getAttr(attr);
			if(absDiff(newVal,oldVal) > TOLERANCE && thisBin >= needed) {
				score += 1;
				thisBin = 0;
				needed = (int)Math.ceil(binSize*(numNodes-added));
			}
			oldVal = newVal;
			n.setAttr("wgt-" + attr, score);
			added++;
			thisBin++;
		}
		return "wgt-" + attr;
	}

	static Set<String> calculateReps(AttributedGraph graph, 
			int maxDist, Set<String> candidates,
			Map<String, Double> maxBins,
			Map<String, Map<Double, Set<AttributedNode>>> sortedAttrSets,
			Map<String, Map<String, Boolean>> memoizedMatches) {
		Set<String> reps = new HashSet<String>(); 
		Map<String, String> p = new HashMap<String, String>();

		
		Set<String> exactMatches = new HashSet<String>();

		for(String attr1 : new HashSet<String>(candidates)) {
			p.put(attr1, attr1);
		}
		
		
		
		for(String attr1 : candidates) {
			for(String attr2 : candidates) {
				if(attr1 == attr2) continue;
				if(smallerString(attr1, attr2).equals(attr2)) continue;
				if(memoizedMatches.containsKey(attr1) && 
						memoizedMatches.get(attr1).containsKey(attr2)) {
					if(memoizedMatches.get(attr1).get(attr2)) {
						union(attr1, attr2, p);
						continue;
					}
					
				}
				corrChecks += graph.getNumNodes();
				
				if(exactMatches.contains(attr2)) continue;
				
				boolean match = attrOrdersAgree(attr1,attr2,graph,maxDist, maxBins,
						sortedAttrSets, memoizedMatches);
				
				
				
				if(!memoizedMatches.containsKey(attr1))
					memoizedMatches.put(attr1, new HashMap<String, Boolean>());
				memoizedMatches.get(attr1).put(attr2, match);
				if(match) {
					union(attr1, attr2, p);
					if(maxDist == 0)
						exactMatches.add(attr2);
				}
			}
		}
		
		
		for(String attr : candidates) {
			if(find(attr, p) == attr) {
				reps.add(attr);
			}
		}
		return reps;
	}

	
	private static boolean attrOrdersAgree(String attr1, String attr2, 
			AttributedGraph graph, int maxAllowed, Map<String, Double> maxBins,
			Map<String, Map<Double, Set<AttributedNode>>> sortedAttrSets,
			Map<String, Map<String, Boolean>> memoizedMatches) {
		double index1 = maxBins.get(attr1), index2 = maxBins.get(attr2);
		
	
		while(index1 > index2 && index1 > maxAllowed) {
			for(AttributedNode n1 : sortedAttrSets.get(attr1).get(index1)) {
				checks += 1;
				double diff = absDiff((Double)n1.getAttr(attr1), (Double)n1.getAttr(attr2));
				if(diff > maxAllowed && diff - maxAllowed > TOLERANCE) {
					return false;
				}
			}
			index1--;
		}
		
		while (index2 > index1 && index2 > maxAllowed) {
			for(AttributedNode n1 : sortedAttrSets.get(attr2).get(index2)) {
				checks += 1;
				double diff = absDiff((Double)n1.getAttr(attr2), (Double)n1.getAttr(attr1));
				if(diff > maxAllowed && diff - maxAllowed > TOLERANCE) {
					return false;
				}
			}
			index2--;
		}
		
		double index = index1;
		while(index > maxAllowed) {
			for(AttributedNode n1 : sortedAttrSets.get(attr1).get(index)) {
				checks += 1;
				double diff = absDiff((Double)n1.getAttr(attr1), (Double)n1.getAttr(attr2));
				if(diff > maxAllowed && diff - maxAllowed > TOLERANCE) {
					return false;
				}
			}
			
			for(AttributedNode n1 : sortedAttrSets.get(attr2).get(index)) {
				checks += 1;
				double diff = absDiff((Double)n1.getAttr(attr2), (Double)n1.getAttr(attr1));
				if(diff > maxAllowed && diff - maxAllowed > TOLERANCE) {
					return false;
				}
			}
			index--;
		}
		
		
		return true;
	}
	
	
	
	// Utility functions
	
	private static double absDiff(double x, double y) {
		if (x > y) return x-y;
		return y-x;
	}
	
	private static void link(String x, String y, Map<String, String> p) {
		if(smallerString(x, y) == x) p.put(y, x);
		else p.put(x, y);
	}

	private static String find(String x, Map<String, String> p) {
		if (x != p.get(x))
			p.put(x, find(p.get(x), p));
		return p.get(x);
	}

	private static void union(String x, String y, Map<String, String> p) {
		link(find(x, p), find(y, p), p);
	}



	private static String smallerString(String s1, String s2) {
		if(s1.split("-").length < s2.split("-").length) return s1;
		if(s2.split("-").length < s1.split("-").length) return s2;
		if(s2.contains("-wn0-") && !s1.contains("-wn0-")) return s1;
		if(s1.contains("-wn0-") && !s2.contains("-wn0-")) return s2;
		if(s1.length() < s2.length()) return s1;
		if(s2.length() < s1.length()) return s2;
		for(int i = 0; i < s1.length(); i++) {
			if ((int)s1.charAt(i) < (int)s2.charAt(i)) return s1;
			if ((int)s2.charAt(i) < (int)s1.charAt(i)) return s2;
		}
		return s2;
	}


}

