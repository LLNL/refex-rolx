/**
 * Version: 1.0
 * Author: Keith Henderson
 * Contact: keith@llnl.gov
 */

import java.util.*;

/**
 * This class is allows Matlab to use Java's PriorityQueue
 * functionality correctly. It's kind of a kluge but Matlab doesn't
 * play very well with Java.
 */
public class HuffmanComparator implements Comparator<Object>
{
    public int compare(Object a, Object b) {
	Object[] aa = (Object[]) a;
	Object[] bb = (Object[]) b;
	return ((Double)aa[0]).compareTo((Double)bb[0]);
    }
}