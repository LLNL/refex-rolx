/**
 * Version: 1.0
 * Author: Keith Henderson
 * Contact: keith@llnl.gov
 */

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * Map from K to double that defaults to zero.
 * 
 * @author hendersk
 *
 * @param <K> Key class.
 */
public class Counter <K> extends HashMap <K, Double>{
	static final long serialVersionUID = 1;
	public static final double TOLERANCE = 0.0001;
	
	public Double get(Object key) {
		if (super.get(key) == null) return 0.0;
		return super.get(key);
	}
	public Double put(K key, Double d) {
		Double old = super.put(key, d);
		if(Math.abs(d) < TOLERANCE && containsKey(key)) remove(key);
		return old;
	}
	public Double increment(K key, Double amount) {
		put(key, get(key) + amount);
		return get(key);
	}
	
	public Double increment(K key) {
		return increment(key, 1.0);
	}
	
	public Double decrement(K key, Double amount) {
		put(key, get(key) - amount);
		return get(key);
	}
	
	public Double decrement(K key) {
		return decrement(key, 1.0);
	}
	
	public boolean is(K key, double d) {
		return Math.abs(get(key) - d) < TOLERANCE;
	}
	
	public void normalize() {
		double sum = 0.0;
		for (K key : keySet()) sum += get(key);
		if(Math.abs(sum) < 0.001) return;
		List<K> l = new ArrayList<K>(keySet());
		for (K key : l) put(key, get(key) / sum);
	}

	public K argmax() {
		K r = null;
		double m = Double.NEGATIVE_INFINITY;
		for (K k : keySet()) {
			if (get(k) > m) r = k;
		}
		return r;
	}
	
	public K argmin() {
		K r = null;
		double m = Double.POSITIVE_INFINITY;
		for (K k : keySet()) {
			if (get(k) < m) r = k;
		}
		return r;
	}
}
