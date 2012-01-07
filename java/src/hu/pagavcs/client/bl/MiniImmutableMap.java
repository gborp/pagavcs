package hu.pagavcs.client.bl;

import java.util.AbstractMap;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MiniImmutableMap<K, V> implements Map<K, V> {

	Object[] keys;
	Object[] values;

	public MiniImmutableMap(Map<K, V> entries) {
		Set<Entry<K, V>> entrySet = entries.entrySet();
		keys = new Object[entrySet.size()];
		values = new Object[entrySet.size()];
		int i = 0;
		for (Entry<K, V> entry : entrySet) {
			keys[i] = entry.getKey();
			values[i] = entry.getValue();
			i++;
		}
	}

	public int size() {
		return keys.length;
	}

	public boolean isEmpty() {
		return size() == 0;
	}

	public boolean containsKey(Object key) {
		for (Object li : keys) {
			if ((key == null && li == null) || (key != null && key.equals(li))) {
				return true;
			}
		}
		return false;
	}

	public boolean containsValue(Object value) {
		for (Object li : values) {
			if ((value == null && li == null) || (value != null && value.equals(li))) {
				return true;
			}
		}
		return false;
	}

	public V get(Object key) {
		int size = size();
		for (int i = 0; i < size; i++) {
			Object li = keys[i];
			if ((key == null && li == null) || (key != null && key.equals(li))) {
				return (V) values[i];
			}
		}
		return null;
	}

	public V put(Object key, Object value) {
		throw new RuntimeException("Not implemented");
	}

	public V remove(Object key) {
		throw new RuntimeException("Not implemented");
	}

	public void putAll(Map m) {
		throw new RuntimeException("Not implemented");
	}

	public void clear() {
		throw new RuntimeException("Not implemented");
	}

	public Set<K> keySet() {
		return new HashSet(Arrays.asList(keys));
	}

	public Collection<V> values() {
		return (Collection<V>) Arrays.asList(values);
	}

	public Set<Entry<K, V>> entrySet() {
		HashSet<Entry<K, V>> result = new HashSet<Entry<K, V>>();
		int size = size();
		for (int i = 0; i < size; i++) {
			SimpleImmutableEntry<K, V> entry = new AbstractMap.SimpleImmutableEntry<K, V>((K) keys[i], (V) values[i]);
			result.add(entry);
		}
		return result;
	}

}
