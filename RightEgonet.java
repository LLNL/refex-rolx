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
 * This class defines a Right Egonet around a center node. The k-level
 * right egonet is the center node plus any node that can be reached
 * from the center in k or fewer hops, only travelling in the
 * direction of the edges.
 */
public class RightEgonet extends Egonet {
	/**
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
	public RightEgonet(Set<AttributedNode> centers, int level,
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
		
		// Add center nodes and do BFS iters to get egonet nodes.
		nodes.addAll(centers);
		AttributedNode neighbor;
		Set<AttributedNode> closedSet = new HashSet<AttributedNode>();
		for(int i = 0; i < level; i++) {
			Set<AttributedNode> oldNodes = new HashSet<AttributedNode>(nodes);
			for(AttributedNode node : oldNodes) {
				if(closedSet.contains(node)) continue;
				for(AttributedLink link : node.getOutLinks()) {
					neighbor = link.dst;
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
	 * only travelling in the direction of the edges.
	 * 
	 * @param centers nodes at the center of the egonet.
	 * @param level how many hops are allowed
	 */
	public RightEgonet(Set<AttributedNode> centers, int level) {
		this(centers, level,
				new HashMap<AttributedNode, Counter<AttributedNode>>(),
				new HashMap<AttributedNode, Counter<AttributedNode>>(),
				new HashMap<AttributedNode, Integer>(),
				new HashMap<AttributedNode, Integer>());
	}
	
	/**
	 * Constructs egonet around a center node. The k-level egonet is the center
	 * node plus any node that can be reached from the center in k or fewer hops,
	 * only travelling in the direction of the edges.
	 * 
	 * @param center node at the center of the egonet.
	 * @param level how many hops are allowed
	 */
	public RightEgonet(AttributedNode center, int level) {
		this(makeSetFromInstance(center), level);
	}

}
