package hu.pagavcs.mug;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class HardWeakMap<K, V> implements Map<K, V> {

	private HashMap<K, WeakReference<V>> delegate;

	public HardWeakMap() {
		delegate = new HashMap<K, WeakReference<V>>();
	}

	public void clear() {
		delegate.clear();
	}

	public boolean containsKey(Object key) {
		return delegate.containsKey(key);
	}

	public boolean containsValue(Object value) {
		return delegate.containsValue(new WeakReference<V>((V) value));
	}

	public Set<Map.Entry<K, V>> entrySet() {
		throw new RuntimeException("Unsupported");
	}

	public V get(Object key) {
		WeakReference<V> result = delegate.get(key);
		if (result != null) {
			return result.get();
		} else {
			return null;
		}
	}

	public boolean isEmpty() {
		return delegate.isEmpty();
	}

	public Set<K> keySet() {
		return delegate.keySet();
	}

	public V put(K key, V value) {
		WeakReference<V> result = delegate.put(key, new WeakReference<V>(value));
		if (result != null) {
			return result.get();
		} else {
			return null;
		}
	}

	public void putAll(Map<? extends K, ? extends V> m) {
		throw new RuntimeException("Unsupported");
	}

	public V remove(Object key) {
		WeakReference<V> removed = delegate.remove(key);
		if (removed != null) {
			return removed.get();
		} else {
			return null;
		}
	}

	public int size() {
		return delegate.size();
	}

	public Collection<V> values() {
		throw new RuntimeException("Unsupported");
	}

}
