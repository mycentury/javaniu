package xyz.javanew.util;

import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.springframework.util.CollectionUtils;

public class BinaryTree<K, V> extends AbstractMap<K, V> implements NavigableMap<K, V>, Cloneable, java.io.Serializable {
	private final Comparator<? super K> comparator;
	private transient Entry<K, V> root;
	private transient int size = 0;
	private transient int modCount = 0;
	private transient int balance = 0;// 平衡值=左level-右level

	public BinaryTree() {
		comparator = null;
	}

	public BinaryTree(Comparator<? super K> comparator) {
		this.comparator = comparator;
	}

	public BinaryTree(Map<? extends K, ? extends V> m) {
		comparator = null;
		putAll(m);
	}

	public BinaryTree(SortedMap<K, ? extends V> m) {
		comparator = m.comparator();
		try {
			buildFromSorted(m.size(), m.entrySet().iterator(), null, null);
		} catch (java.io.IOException cannotHappen) {
		} catch (ClassNotFoundException cannotHappen) {
		}
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public boolean containsKey(Object key) {
		return getEntry(key) != null;
	}

	@Override
	public boolean containsValue(Object value) {
		for (Entry<K, V> e = getFirstEntry(); e != null; e = successor(e))
			if (valEquals(value, e.value)) return true;
		return false;
	}

	@Override
	public V get(Object key) {
		Entry<K, V> p = getEntry(key);
		return (p == null ? null : p.value);
	}

	@Override
	public Comparator<? super K> comparator() {
		return comparator;
	}

	@Override
	public K firstKey() {
		return key(getFirstEntry());
	}

	@Override
	public K lastKey() {
		return key(getLastEntry());
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> map) {
		int mapSize = map.size();
		if (size == 0 && mapSize != 0 && map instanceof SortedMap) {
			Comparator<?> c = ((SortedMap<?, ?>) map).comparator();
			if (c == comparator || (c != null && c.equals(comparator))) {
				++modCount;
				try {
					buildFromSorted(mapSize, map.entrySet().iterator(), null, null);
				} catch (java.io.IOException cannotHappen) {
				} catch (ClassNotFoundException cannotHappen) {
				}
				return;
			}
		}
		super.putAll(map);
	}

	final Entry<K, V> getEntry(Object key) {
		if (comparator != null) return getEntryUsingComparator(key);
		if (key == null) throw new NullPointerException();
		@SuppressWarnings("unchecked")
		Comparable<? super K> k = (Comparable<? super K>) key;
		Entry<K, V> p = root;
		while (p != null) {
			int cmp = k.compareTo(p.key);
			if (cmp < 0) p = p.left;
			else if (cmp > 0) p = p.right;
			else
				return p;
		}
		return null;
	}

	final Entry<K, V> getEntryUsingComparator(Object key) {
		@SuppressWarnings("unchecked")
		K k = (K) key;
		Comparator<? super K> cpr = comparator;
		if (cpr != null) {
			Entry<K, V> p = root;
			while (p != null) {
				int cmp = cpr.compare(k, p.key);
				if (cmp < 0) p = p.left;
				else if (cmp > 0) p = p.right;
				else
					return p;
			}
		}
		return null;
	}

	final Entry<K, V> getCeilingEntry(K key) {
		Entry<K, V> p = root;
		while (p != null) {
			int cmp = compare(key, p.key);
			if (cmp < 0) {
				if (p.left != null) p = p.left;
				else
					return p;
			} else if (cmp > 0) {
				if (p.right != null) {
					p = p.right;
				} else {
					Entry<K, V> parent = p.parent;
					Entry<K, V> ch = p;
					while (parent != null && ch == parent.right) {
						ch = parent;
						parent = parent.parent;
					}
					return parent;
				}
			} else
				return p;
		}
		return null;
	}

	final Entry<K, V> getFloorEntry(K key) {
		Entry<K, V> p = root;
		while (p != null) {
			int cmp = compare(key, p.key);
			if (cmp > 0) {
				if (p.right != null) p = p.right;
				else
					return p;
			} else if (cmp < 0) {
				if (p.left != null) {
					p = p.left;
				} else {
					Entry<K, V> parent = p.parent;
					Entry<K, V> ch = p;
					while (parent != null && ch == parent.left) {
						ch = parent;
						parent = parent.parent;
					}
					return parent;
				}
			} else
				return p;

		}
		return null;
	}

	/**
	 * Gets the entry for the least key greater than the specified key; if no such entry exists, returns the entry for
	 * the least key greater than the specified key; if no such entry exists returns {@code null}.
	 */
	final Entry<K, V> getHigherEntry(K key) {
		Entry<K, V> p = root;
		while (p != null) {
			int cmp = compare(key, p.key);
			if (cmp < 0) {
				if (p.left != null) p = p.left;
				else
					return p;
			} else {
				if (p.right != null) {
					p = p.right;
				} else {
					Entry<K, V> parent = p.parent;
					Entry<K, V> ch = p;
					while (parent != null && ch == parent.right) {
						ch = parent;
						parent = parent.parent;
					}
					return parent;
				}
			}
		}
		return null;
	}

	/**
	 * Returns the entry for the greatest key less than the specified key; if no such entry exists (i.e., the least key
	 * in the Tree is greater than the specified key), returns {@code null}.
	 */
	final Entry<K, V> getLowerEntry(K key) {
		Entry<K, V> p = root;
		while (p != null) {
			int cmp = compare(key, p.key);
			if (cmp > 0) {
				if (p.right != null) p = p.right;
				else
					return p;
			} else {
				if (p.left != null) {
					p = p.left;
				} else {
					Entry<K, V> parent = p.parent;
					Entry<K, V> ch = p;
					while (parent != null && ch == parent.left) {
						ch = parent;
						parent = parent.parent;
					}
					return parent;
				}
			}
		}
		return null;
	}

	/**
	 * Associates the specified value with the specified key in this map. If the map previously contained a mapping for
	 * the key, the old value is replaced.
	 *
	 * @param key key with which the specified value is to be associated
	 * @param value value to be associated with the specified key
	 * @return the previous value associated with {@code key}, or {@code null} if there was no mapping for {@code key}.
	 *         (A {@code null} return can also indicate that the map previously associated {@code null} with {@code key}
	 *         .)
	 * @throws ClassCastException if the specified key cannot be compared with the keys currently in the map
	 * @throws NullPointerException if the specified key is null and this map uses natural ordering, or its comparator
	 *             does not permit null keys
	 */
	@Override
	public V put(K key, V value) {
		if (root == null) {
			root = new Entry<>(key, value, null);
			size = 1;
			modCount++;
			printTree(false);
			return null;
		}
		Entry<K, V> t = root;
		int cmp;
		Entry<K, V> parent;
		// 父辈级元素，不含父元素
		List<Entry<K, V>> parents = new ArrayList<BinaryTree.Entry<K, V>>();
		List<Entry<K, V>> currentParents = new ArrayList<BinaryTree.Entry<K, V>>();
		if (key == null) throw new NullPointerException();
		@SuppressWarnings("unchecked")
		Comparable<? super K> k = (Comparable<? super K>) key;
		do {
			parent = t;
			currentParents.clear();
			if (CollectionUtils.isEmpty(parents)) {
				currentParents.add(root);
			} else {
				for (Entry<K, V> entry : parents) {
					if (entry.left != null) {
						currentParents.add(entry.left);
					}
					if (entry.right != null) {
						currentParents.add(entry.right);
					}
				}
			}
			parents.clear();
			parents.addAll(currentParents);

			cmp = k.compareTo(t.key);
			if (cmp < 0) t = t.left;
			else if (cmp > 0) t = t.right;
			else {
				V setValue = t.setValue(value);
				printTree(false);
				return setValue;
			}
		} while (t != null);
		Entry<K, V> e = new Entry<>(key, value, parent);
		if (cmp < 0) parent.left = e;
		else
			parent.right = e;

		boolean balanceChanged = true;
		for (Entry<K, V> entry : parents) {
			if (entry != root && (entry.left != null && entry.left != e || entry.right != null && entry.right != e)) {
				balanceChanged = false;
			}
		}

		int balanceAdd = !balanceChanged ? 0 : (k.compareTo(root.key) < 0 ? -1 : 1);

		// 连续2代"独生子"，需要转换，转换后不改变层级
		if (parent.parent != null && (parent.left == null || parent.right == null) && (parent.parent.left == null || parent.parent.right == null)) {
			Entry<K, V> min = null;
			Entry<K, V> mid = null;
			Entry<K, V> max = null;
			if (parent.parent.left == null) {
				min = parent.parent;
				if (parent.left == null) {
					mid = parent;
					max = e;
				} else {
					mid = e;
					max = parent;
				}
			} else {
				max = parent.parent;
				if (parent.left == null) {
					mid = e;
					min = parent;
				} else {
					mid = parent;
					min = e;
				}
			}
			if (parent.parent.parent == null) {
				root = mid;
				balance = 0;
			} else {
				if (parent.parent.parent.left == parent.parent) {
					parent.parent.parent.left = mid;
				} else {
					parent.parent.parent.right = mid;
				}
			}
			mid.parent = parent.parent.parent;
			mid.left = min;
			mid.right = max;
			min.parent = mid;
			min.left = null;
			min.right = null;
			max.parent = mid;
			max.left = null;
			max.right = null;
			balanceAdd = 0;
		}

		// 连续三个节点左孩子都为null或者右孩子都为null
		// if (parent.parent != null && parent.left == null && parent.parent.left == null) {
		// rotateLeft(parent.parent);
		// balanceAdd--;
		// } else if (parent.parent != null && parent.right == null && parent.parent.right == null) {
		// rotateRight(parent.parent);
		// balanceAdd++;
		// }
		// 添加到左分支，左分支太重
		if (balance + balanceAdd < -1) {
			rotateRight(root);
			balance = 0;
			// 添加到右分支
		} else if (balance + balanceAdd > 1) {
			rotateLeft(root);
			balance = 0;
		} else {
			balance += balanceAdd;
		}

		size++;
		modCount++;
		printTree(false);
		return null;
	}

	/**
	 * 
	 */
	private void printTree(boolean printLeaf) {
		System.out.println("--------------------当前树形结构(" + balance + ")---------------------");
		boolean hasNode = true;
		List<List<Entry<K, V>>> allLevelNodes = new ArrayList<List<Entry<K, V>>>();
		List<Entry<K, V>> currentLevelNodes = new ArrayList<Entry<K, V>>();
		currentLevelNodes.add(root);
		allLevelNodes.add(currentLevelNodes);
		while (hasNode) {
			hasNode = false;
			List<Entry<K, V>> lastLevelNodes = currentLevelNodes;
			currentLevelNodes = new ArrayList<Entry<K, V>>();
			for (Entry<K, V> entry : lastLevelNodes) {
				if (entry != null) {
					currentLevelNodes.add(entry.left);
					currentLevelNodes.add(entry.right);
					if (!hasNode && (entry.left != null || entry.right != null)) {
						hasNode = true;
					}
				} else {
					currentLevelNodes.add(null);
					currentLevelNodes.add(null);
				}
			}
			allLevelNodes.add(currentLevelNodes);
		}

		String blankWidth = "  ";
		List<Entry<K, V>> currLevelNodes = null;
		List<Entry<K, V>> lastLevelNodes = null;
		for (int i = 0; i < allLevelNodes.size(); i++) {
			lastLevelNodes = currLevelNodes;
			currLevelNodes = allLevelNodes.get(i);
			StringBuilder nodeLine = new StringBuilder();
			StringBuilder lineLine = new StringBuilder();
			StringBuilder blank = new StringBuilder();
			// 空白
			for (int j = 0; j < Math.pow(2, allLevelNodes.size() - 1 - i) - 1; j++) {
				blank.append(blankWidth);
			}
			for (int k = 0; k < currLevelNodes.size(); k++) {
				Entry<K, V> entry = currLevelNodes.get(k);
				// if (entry == null) {
				// lineLine.append(blank).append(blankWidth).append(blank).append(blankWidth);
				// nodeLine.append(blank).append(blankWidth).append(blank).append(blankWidth);
				// } else {
				// lineLine.append(blank).append(k % 2 == 0 ? " /" : "\\ ").append(blank).append(blankWidth);
				// nodeLine.append(blank).append(entry == null?"NN":entry.getKey()).append(blank).append(blankWidth);
				// }
				String entryString = entry == null ? "NN" : entry.getKey().toString();
				String codeString = k % 2 == 0 ? " /" : "\\ ";
				if (entry == null && (lastLevelNodes != null && lastLevelNodes.get(k / 2) != null || !printLeaf)) {
					entryString = blankWidth;
					codeString = blankWidth;
				}
				lineLine.append(blank).append(codeString).append(blank).append(blankWidth);
				nodeLine.append(blank).append(entryString).append(blank).append(blankWidth);
			}
			if (i != 0) {
				System.out.println(lineLine.toString());
			}
			System.out.println(nodeLine.toString());
		}
	}

	public static void main(String[] args) {
		// fixTest();
		randomTest();
	}

	/**
	 * 
	 */
	private static void fixTest() {
		int[] keys = { 60, 67, 51 };
		BinaryTree<String, String> tree = new BinaryTree<String, String>();
		for (int i = 0; i < keys.length; i++) {
			String key = String.valueOf(keys[i]);
			System.out.println("--------------------新增：" + key + "---------------------");
			tree.put(key, key);
		}
	}

	/**
	 * 
	 */
	private static void randomTest() {
		BinaryTree<String, String> tree = new BinaryTree<String, String>();
		for (int i = 0; i < 20; i++) {
			String key = String.valueOf((int) (Math.random() * 90 + 10));
			System.out.println("--------------------新增：" + key + "---------------------");
			tree.put(key, key);
		}
	}

	int getChildrenValue(Entry<K, V> e) {
		int count = 0;
		if (e.left != null) {
			count += 1;
		}
		if (e.right != null) {
			count += 2;
		}
		return count;
	}

	/**
	 * Removes the mapping for this key from this BinaryTree if present.
	 *
	 * @param key key for which mapping should be removed
	 * @return the previous value associated with {@code key}, or {@code null} if there was no mapping for {@code key}.
	 *         (A {@code null} return can also indicate that the map previously associated {@code null} with {@code key}
	 *         .)
	 * @throws ClassCastException if the specified key cannot be compared with the keys currently in the map
	 * @throws NullPointerException if the specified key is null and this map uses natural ordering, or its comparator
	 *             does not permit null keys
	 */
	@Override
	public V remove(Object key) {
		Entry<K, V> p = getEntry(key);
		if (p == null) return null;

		V oldValue = p.value;
		deleteEntry(p);
		return oldValue;
	}

	/**
	 * Removes all of the mappings from this map. The map will be empty after this call returns.
	 */
	@Override
	public void clear() {
		modCount++;
		size = 0;
		root = null;
	}

	/**
	 * Returns a shallow copy of this {@code BinaryTree} instance. (The keys and values themselves are not cloned.)
	 *
	 * @return a shallow copy of this map
	 */
	@Override
	public Object clone() {
		BinaryTree<?, ?> clone;
		try {
			clone = (BinaryTree<?, ?>) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new InternalError(e);
		}

		// Put clone into "virgin" state (except for comparator)
		clone.root = null;
		clone.size = 0;
		clone.modCount = 0;
		clone.entrySet = null;
		clone.navigableKeySet = null;
		clone.descendingMap = null;

		// Initialize clone with our mappings
		try {
			clone.buildFromSorted(size, entrySet().iterator(), null, null);
		} catch (java.io.IOException cannotHappen) {
		} catch (ClassNotFoundException cannotHappen) {
		}

		return clone;
	}

	@Override
	public Map.Entry<K, V> firstEntry() {
		return exportEntry(getFirstEntry());
	}

	@Override
	public Map.Entry<K, V> lastEntry() {
		return exportEntry(getLastEntry());
	}

	/**
	 * @since 1.6
	 */
	@Override
	public Map.Entry<K, V> pollFirstEntry() {
		Entry<K, V> p = getFirstEntry();
		Map.Entry<K, V> result = exportEntry(p);
		if (p != null) deleteEntry(p);
		return result;
	}

	/**
	 * @since 1.6
	 */
	@Override
	public Map.Entry<K, V> pollLastEntry() {
		Entry<K, V> p = getLastEntry();
		Map.Entry<K, V> result = exportEntry(p);
		if (p != null) deleteEntry(p);
		return result;
	}

	/**
	 * @throws ClassCastException {@inheritDoc}
	 * @throws NullPointerException if the specified key is null and this map uses natural ordering, or its comparator
	 *             does not permit null keys
	 * @since 1.6
	 */
	@Override
	public Map.Entry<K, V> lowerEntry(K key) {
		return exportEntry(getLowerEntry(key));
	}

	/**
	 * @throws ClassCastException {@inheritDoc}
	 * @throws NullPointerException if the specified key is null and this map uses natural ordering, or its comparator
	 *             does not permit null keys
	 * @since 1.6
	 */
	@Override
	public K lowerKey(K key) {
		return keyOrNull(getLowerEntry(key));
	}

	/**
	 * @throws ClassCastException {@inheritDoc}
	 * @throws NullPointerException if the specified key is null and this map uses natural ordering, or its comparator
	 *             does not permit null keys
	 * @since 1.6
	 */
	@Override
	public Map.Entry<K, V> floorEntry(K key) {
		return exportEntry(getFloorEntry(key));
	}

	/**
	 * @throws ClassCastException {@inheritDoc}
	 * @throws NullPointerException if the specified key is null and this map uses natural ordering, or its comparator
	 *             does not permit null keys
	 * @since 1.6
	 */
	@Override
	public K floorKey(K key) {
		return keyOrNull(getFloorEntry(key));
	}

	/**
	 * @throws ClassCastException {@inheritDoc}
	 * @throws NullPointerException if the specified key is null and this map uses natural ordering, or its comparator
	 *             does not permit null keys
	 * @since 1.6
	 */
	@Override
	public Map.Entry<K, V> ceilingEntry(K key) {
		return exportEntry(getCeilingEntry(key));
	}

	/**
	 * @throws ClassCastException {@inheritDoc}
	 * @throws NullPointerException if the specified key is null and this map uses natural ordering, or its comparator
	 *             does not permit null keys
	 * @since 1.6
	 */
	@Override
	public K ceilingKey(K key) {
		return keyOrNull(getCeilingEntry(key));
	}

	/**
	 * @throws ClassCastException {@inheritDoc}
	 * @throws NullPointerException if the specified key is null and this map uses natural ordering, or its comparator
	 *             does not permit null keys
	 * @since 1.6
	 */
	@Override
	public Map.Entry<K, V> higherEntry(K key) {
		return exportEntry(getHigherEntry(key));
	}

	/**
	 * @throws ClassCastException {@inheritDoc}
	 * @throws NullPointerException if the specified key is null and this map uses natural ordering, or its comparator
	 *             does not permit null keys
	 * @since 1.6
	 */
	@Override
	public K higherKey(K key) {
		return keyOrNull(getHigherEntry(key));
	}

	// Views

	/**
	 * Fields initialized to contain an instance of the entry set view the first time this view is requested. Views are
	 * stateless, so there's no reason to create more than one.
	 */
	private transient EntrySet entrySet;
	private transient KeySet<K> navigableKeySet;
	private transient NavigableMap<K, V> descendingMap;

	/**
	 * Returns a {@link Set} view of the keys contained in this map.
	 * <p>
	 * The set's iterator returns the keys in ascending order. The set's spliterator is
	 * <em><a href="Spliterator.html#binding">late-binding</a></em>, <em>fail-fast</em>, and additionally reports
	 * {@link Spliterator#SORTED} and {@link Spliterator#ORDERED} with an encounter order that is ascending key order.
	 * The spliterator's comparator (see {@link java.util.Spliterator#getComparator()}) is {@code null} if the tree
	 * map's comparator (see {@link #comparator()}) is {@code null}. Otherwise, the spliterator's comparator is the same
	 * as or imposes the same total ordering as the tree map's comparator.
	 * <p>
	 * The set is backed by the map, so changes to the map are reflected in the set, and vice-versa. If the map is
	 * modified while an iteration over the set is in progress (except through the iterator's own {@code remove}
	 * operation), the results of the iteration are undefined. The set supports element removal, which removes the
	 * corresponding mapping from the map, via the {@code Iterator.remove}, {@code Set.remove}, {@code removeAll},
	 * {@code retainAll}, and {@code clear} operations. It does not support the {@code add} or {@code addAll}
	 * operations.
	 */
	@Override
	public Set<K> keySet() {
		return navigableKeySet();
	}

	/**
	 * @since 1.6
	 */
	@Override
	public NavigableSet<K> navigableKeySet() {
		KeySet<K> nks = navigableKeySet;
		return (nks != null) ? nks : (navigableKeySet = new KeySet<>(this));
	}

	/**
	 * @since 1.6
	 */
	@Override
	public NavigableSet<K> descendingKeySet() {
		return descendingMap().navigableKeySet();
	}

	@Override
	public Set<Map.Entry<K, V>> entrySet() {
		EntrySet es = entrySet;
		return (es != null) ? es : (entrySet = new EntrySet());
	}

	/**
	 * @since 1.6
	 */
	@Override
	public NavigableMap<K, V> descendingMap() {
		NavigableMap<K, V> km = descendingMap;
		return (km != null) ? km : (descendingMap = new DescendingSubMap<>(this, true, null, true, true, null, true));
	}

	@Override
	public NavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
		return new AscendingSubMap<>(this, false, fromKey, fromInclusive, false, toKey, toInclusive);
	}

	/**
	 * @throws ClassCastException {@inheritDoc}
	 * @throws NullPointerException if {@code toKey} is null and this map uses natural ordering, or its comparator does
	 *             not permit null keys
	 * @throws IllegalArgumentException {@inheritDoc}
	 * @since 1.6
	 */
	@Override
	public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
		return new AscendingSubMap<>(this, true, null, true, false, toKey, inclusive);
	}

	/**
	 * @throws ClassCastException {@inheritDoc}
	 * @throws NullPointerException if {@code fromKey} is null and this map uses natural ordering, or its comparator
	 *             does not permit null keys
	 * @throws IllegalArgumentException {@inheritDoc}
	 * @since 1.6
	 */
	@Override
	public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
		return new AscendingSubMap<>(this, false, fromKey, inclusive, true, null, true);
	}

	/**
	 * @throws ClassCastException {@inheritDoc}
	 * @throws NullPointerException if {@code fromKey} or {@code toKey} is null and this map uses natural ordering, or
	 *             its comparator does not permit null keys
	 * @throws IllegalArgumentException {@inheritDoc}
	 */
	@Override
	public SortedMap<K, V> subMap(K fromKey, K toKey) {
		return subMap(fromKey, true, toKey, false);
	}

	/**
	 * @throws ClassCastException {@inheritDoc}
	 * @throws NullPointerException if {@code toKey} is null and this map uses natural ordering, or its comparator does
	 *             not permit null keys
	 * @throws IllegalArgumentException {@inheritDoc}
	 */
	@Override
	public SortedMap<K, V> headMap(K toKey) {
		return headMap(toKey, false);
	}

	/**
	 * @throws ClassCastException {@inheritDoc}
	 * @throws NullPointerException if {@code fromKey} is null and this map uses natural ordering, or its comparator
	 *             does not permit null keys
	 * @throws IllegalArgumentException {@inheritDoc}
	 */
	@Override
	public SortedMap<K, V> tailMap(K fromKey) {
		return tailMap(fromKey, true);
	}

	@Override
	public boolean replace(K key, V oldValue, V newValue) {
		Entry<K, V> p = getEntry(key);
		if (p != null && Objects.equals(oldValue, p.value)) {
			p.value = newValue;
			return true;
		}
		return false;
	}

	@Override
	public V replace(K key, V value) {
		Entry<K, V> p = getEntry(key);
		if (p != null) {
			V oldValue = p.value;
			p.value = value;
			return oldValue;
		}
		return null;
	}

	@Override
	public void forEach(BiConsumer<? super K, ? super V> action) {
		Objects.requireNonNull(action);
		int expectedModCount = modCount;
		for (Entry<K, V> e = getFirstEntry(); e != null; e = successor(e)) {
			action.accept(e.key, e.value);

			if (expectedModCount != modCount) {
				throw new ConcurrentModificationException();
			}
		}
	}

	@Override
	public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
		Objects.requireNonNull(function);
		int expectedModCount = modCount;

		for (Entry<K, V> e = getFirstEntry(); e != null; e = successor(e)) {
			e.value = function.apply(e.key, e.value);

			if (expectedModCount != modCount) {
				throw new ConcurrentModificationException();
			}
		}
	}

	// View class support

	class Values extends AbstractCollection<V> {
		@Override
		public Iterator<V> iterator() {
			return new ValueIterator(getFirstEntry());
		}

		@Override
		public int size() {
			return BinaryTree.this.size();
		}

		@Override
		public boolean contains(Object o) {
			return BinaryTree.this.containsValue(o);
		}

		@Override
		public boolean remove(Object o) {
			for (Entry<K, V> e = getFirstEntry(); e != null; e = successor(e)) {
				if (valEquals(e.getValue(), o)) {
					deleteEntry(e);
					return true;
				}
			}
			return false;
		}

		@Override
		public void clear() {
			BinaryTree.this.clear();
		}

		@Override
		public Spliterator<V> spliterator() {
			return new ValueSpliterator<K, V>(BinaryTree.this, null, null, 0, -1, 0);
		}
	}

	class EntrySet extends AbstractSet<Map.Entry<K, V>> {
		@Override
		public Iterator<Map.Entry<K, V>> iterator() {
			return new EntryIterator(getFirstEntry());
		}

		@Override
		public boolean contains(Object o) {
			if (!(o instanceof Map.Entry)) return false;
			Map.Entry<?, ?> entry = (Map.Entry<?, ?>) o;
			Object value = entry.getValue();
			Entry<K, V> p = getEntry(entry.getKey());
			return p != null && valEquals(p.getValue(), value);
		}

		@Override
		public boolean remove(Object o) {
			if (!(o instanceof Map.Entry)) return false;
			Map.Entry<?, ?> entry = (Map.Entry<?, ?>) o;
			Object value = entry.getValue();
			Entry<K, V> p = getEntry(entry.getKey());
			if (p != null && valEquals(p.getValue(), value)) {
				deleteEntry(p);
				return true;
			}
			return false;
		}

		@Override
		public int size() {
			return BinaryTree.this.size();
		}

		@Override
		public void clear() {
			BinaryTree.this.clear();
		}

		@Override
		public Spliterator<Map.Entry<K, V>> spliterator() {
			return new EntrySpliterator<K, V>(BinaryTree.this, null, null, 0, -1, 0);
		}
	}

	/*
	 * Unlike Values and EntrySet, the KeySet class is static, delegating to a NavigableMap to allow use by SubMaps,
	 * which outweighs the ugliness of needing type-tests for the following Iterator methods that are defined
	 * appropriately in main versus submap classes.
	 */

	Iterator<K> keyIterator() {
		return new KeyIterator(getFirstEntry());
	}

	Iterator<K> descendingKeyIterator() {
		return new DescendingKeyIterator(getLastEntry());
	}

	static final class KeySet<E> extends AbstractSet<E> implements NavigableSet<E> {
		private final NavigableMap<E, ?> m;

		KeySet(NavigableMap<E, ?> map) {
			m = map;
		}

		@Override
		public Iterator<E> iterator() {
			if (m instanceof BinaryTree) return ((BinaryTree<E, ?>) m).keyIterator();
			else
				return ((BinaryTree.NavigableSubMap<E, ?>) m).keyIterator();
		}

		@Override
		public Iterator<E> descendingIterator() {
			if (m instanceof BinaryTree) return ((BinaryTree<E, ?>) m).descendingKeyIterator();
			else
				return ((BinaryTree.NavigableSubMap<E, ?>) m).descendingKeyIterator();
		}

		@Override
		public int size() {
			return m.size();
		}

		@Override
		public boolean isEmpty() {
			return m.isEmpty();
		}

		@Override
		public boolean contains(Object o) {
			return m.containsKey(o);
		}

		@Override
		public void clear() {
			m.clear();
		}

		@Override
		public E lower(E e) {
			return m.lowerKey(e);
		}

		@Override
		public E floor(E e) {
			return m.floorKey(e);
		}

		@Override
		public E ceiling(E e) {
			return m.ceilingKey(e);
		}

		@Override
		public E higher(E e) {
			return m.higherKey(e);
		}

		@Override
		public E first() {
			return m.firstKey();
		}

		@Override
		public E last() {
			return m.lastKey();
		}

		@Override
		public Comparator<? super E> comparator() {
			return m.comparator();
		}

		@Override
		public E pollFirst() {
			Map.Entry<E, ?> e = m.pollFirstEntry();
			return (e == null) ? null : e.getKey();
		}

		@Override
		public E pollLast() {
			Map.Entry<E, ?> e = m.pollLastEntry();
			return (e == null) ? null : e.getKey();
		}

		@Override
		public boolean remove(Object o) {
			int oldSize = size();
			m.remove(o);
			return size() != oldSize;
		}

		@Override
		public NavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
			return new KeySet<>(m.subMap(fromElement, fromInclusive, toElement, toInclusive));
		}

		@Override
		public NavigableSet<E> headSet(E toElement, boolean inclusive) {
			return new KeySet<>(m.headMap(toElement, inclusive));
		}

		@Override
		public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
			return new KeySet<>(m.tailMap(fromElement, inclusive));
		}

		@Override
		public SortedSet<E> subSet(E fromElement, E toElement) {
			return subSet(fromElement, true, toElement, false);
		}

		@Override
		public SortedSet<E> headSet(E toElement) {
			return headSet(toElement, false);
		}

		@Override
		public SortedSet<E> tailSet(E fromElement) {
			return tailSet(fromElement, true);
		}

		@Override
		public NavigableSet<E> descendingSet() {
			return new KeySet<>(m.descendingMap());
		}

		@Override
		public Spliterator<E> spliterator() {
			return keySpliteratorFor(m);
		}
	}

	/**
	 * Base class for BinaryTree Iterators
	 */
	abstract class PrivateEntryIterator<T> implements Iterator<T> {
		Entry<K, V> next;
		Entry<K, V> lastReturned;
		int expectedModCount;

		PrivateEntryIterator(Entry<K, V> first) {
			expectedModCount = modCount;
			lastReturned = null;
			next = first;
		}

		@Override
		public final boolean hasNext() {
			return next != null;
		}

		final Entry<K, V> nextEntry() {
			Entry<K, V> e = next;
			if (e == null) throw new NoSuchElementException();
			if (modCount != expectedModCount) throw new ConcurrentModificationException();
			next = successor(e);
			lastReturned = e;
			return e;
		}

		final Entry<K, V> prevEntry() {
			Entry<K, V> e = next;
			if (e == null) throw new NoSuchElementException();
			if (modCount != expectedModCount) throw new ConcurrentModificationException();
			next = predecessor(e);
			lastReturned = e;
			return e;
		}

		@Override
		public void remove() {
			if (lastReturned == null) throw new IllegalStateException();
			if (modCount != expectedModCount) throw new ConcurrentModificationException();
			// deleted entries are replaced by their successors
			if (lastReturned.left != null && lastReturned.right != null) next = lastReturned;
			deleteEntry(lastReturned);
			expectedModCount = modCount;
			lastReturned = null;
		}
	}

	final class EntryIterator extends PrivateEntryIterator<Map.Entry<K, V>> {
		EntryIterator(Entry<K, V> first) {
			super(first);
		}

		@Override
		public Map.Entry<K, V> next() {
			return nextEntry();
		}
	}

	final class ValueIterator extends PrivateEntryIterator<V> {
		ValueIterator(Entry<K, V> first) {
			super(first);
		}

		@Override
		public V next() {
			return nextEntry().value;
		}
	}

	final class KeyIterator extends PrivateEntryIterator<K> {
		KeyIterator(Entry<K, V> first) {
			super(first);
		}

		@Override
		public K next() {
			return nextEntry().key;
		}
	}

	final class DescendingKeyIterator extends PrivateEntryIterator<K> {
		DescendingKeyIterator(Entry<K, V> first) {
			super(first);
		}

		@Override
		public K next() {
			return prevEntry().key;
		}

		@Override
		public void remove() {
			if (lastReturned == null) throw new IllegalStateException();
			if (modCount != expectedModCount) throw new ConcurrentModificationException();
			deleteEntry(lastReturned);
			lastReturned = null;
			expectedModCount = modCount;
		}
	}

	// Little utilities

	/**
	 * Compares two keys using the correct comparison method for this BinaryTree.
	 */
	@SuppressWarnings("unchecked")
	final int compare(Object k1, Object k2) {
		return comparator == null ? ((Comparable<? super K>) k1).compareTo((K) k2) : comparator.compare((K) k1, (K) k2);
	}

	/**
	 * Test two values for equality. Differs from o1.equals(o2) only in that it copes with {@code null} o1 properly.
	 */
	static final boolean valEquals(Object o1, Object o2) {
		return (o1 == null ? o2 == null : o1.equals(o2));
	}

	/**
	 * Return SimpleImmutableEntry for entry, or null if null
	 */
	static <K, V> Map.Entry<K, V> exportEntry(BinaryTree.Entry<K, V> e) {
		return (e == null) ? null : new AbstractMap.SimpleImmutableEntry<>(e);
	}

	/**
	 * Return key for entry, or null if null
	 */
	static <K, V> K keyOrNull(BinaryTree.Entry<K, V> e) {
		return (e == null) ? null : e.key;
	}

	/**
	 * Returns the key corresponding to the specified Entry.
	 * 
	 * @throws NoSuchElementException if the Entry is null
	 */
	static <K> K key(Entry<K, ?> e) {
		if (e == null) throw new NoSuchElementException();
		return e.key;
	}

	// SubMaps

	/**
	 * Dummy value serving as unmatchable fence key for unbounded SubMapIterators
	 */
	private static final Object UNBOUNDED = new Object();

	/**
	 * @serial include
	 */
	abstract static class NavigableSubMap<K, V> extends AbstractMap<K, V> implements NavigableMap<K, V>, java.io.Serializable {
		private static final long serialVersionUID = -2102997345730753016L;
		/**
		 * The backing map.
		 */
		final BinaryTree<K, V> m;

		/**
		 * Endpoints are represented as triples (fromStart, lo, loInclusive) and (toEnd, hi, hiInclusive). If fromStart
		 * is true, then the low (absolute) bound is the start of the backing map, and the other values are ignored.
		 * Otherwise, if loInclusive is true, lo is the inclusive bound, else lo is the exclusive bound. Similarly for
		 * the upper bound.
		 */
		final K lo, hi;
		final boolean fromStart, toEnd;
		final boolean loInclusive, hiInclusive;

		NavigableSubMap(BinaryTree<K, V> m, boolean fromStart, K lo, boolean loInclusive, boolean toEnd, K hi, boolean hiInclusive) {
			if (!fromStart && !toEnd) {
				if (m.compare(lo, hi) > 0) throw new IllegalArgumentException("fromKey > toKey");
			} else {
				if (!fromStart) // type check
				m.compare(lo, lo);
				if (!toEnd) m.compare(hi, hi);
			}

			this.m = m;
			this.fromStart = fromStart;
			this.lo = lo;
			this.loInclusive = loInclusive;
			this.toEnd = toEnd;
			this.hi = hi;
			this.hiInclusive = hiInclusive;
		}

		// internal utilities

		final boolean tooLow(Object key) {
			if (!fromStart) {
				int c = m.compare(key, lo);
				if (c < 0 || (c == 0 && !loInclusive)) return true;
			}
			return false;
		}

		final boolean tooHigh(Object key) {
			if (!toEnd) {
				int c = m.compare(key, hi);
				if (c > 0 || (c == 0 && !hiInclusive)) return true;
			}
			return false;
		}

		final boolean inRange(Object key) {
			return !tooLow(key) && !tooHigh(key);
		}

		final boolean inClosedRange(Object key) {
			return (fromStart || m.compare(key, lo) >= 0) && (toEnd || m.compare(hi, key) >= 0);
		}

		final boolean inRange(Object key, boolean inclusive) {
			return inclusive ? inRange(key) : inClosedRange(key);
		}

		/*
		 * Absolute versions of relation operations. Subclasses map to these using like-named "sub" versions that invert
		 * senses for descending maps
		 */

		final BinaryTree.Entry<K, V> absLowest() {
			BinaryTree.Entry<K, V> e = (fromStart ? m.getFirstEntry() : (loInclusive ? m.getCeilingEntry(lo) : m.getHigherEntry(lo)));
			return (e == null || tooHigh(e.key)) ? null : e;
		}

		final BinaryTree.Entry<K, V> absHighest() {
			BinaryTree.Entry<K, V> e = (toEnd ? m.getLastEntry() : (hiInclusive ? m.getFloorEntry(hi) : m.getLowerEntry(hi)));
			return (e == null || tooLow(e.key)) ? null : e;
		}

		final BinaryTree.Entry<K, V> absCeiling(K key) {
			if (tooLow(key)) return absLowest();
			BinaryTree.Entry<K, V> e = m.getCeilingEntry(key);
			return (e == null || tooHigh(e.key)) ? null : e;
		}

		final BinaryTree.Entry<K, V> absHigher(K key) {
			if (tooLow(key)) return absLowest();
			BinaryTree.Entry<K, V> e = m.getHigherEntry(key);
			return (e == null || tooHigh(e.key)) ? null : e;
		}

		final BinaryTree.Entry<K, V> absFloor(K key) {
			if (tooHigh(key)) return absHighest();
			BinaryTree.Entry<K, V> e = m.getFloorEntry(key);
			return (e == null || tooLow(e.key)) ? null : e;
		}

		final BinaryTree.Entry<K, V> absLower(K key) {
			if (tooHigh(key)) return absHighest();
			BinaryTree.Entry<K, V> e = m.getLowerEntry(key);
			return (e == null || tooLow(e.key)) ? null : e;
		}

		/** Returns the absolute high fence for ascending traversal */
		final BinaryTree.Entry<K, V> absHighFence() {
			return (toEnd ? null : (hiInclusive ? m.getHigherEntry(hi) : m.getCeilingEntry(hi)));
		}

		/** Return the absolute low fence for descending traversal */
		final BinaryTree.Entry<K, V> absLowFence() {
			return (fromStart ? null : (loInclusive ? m.getLowerEntry(lo) : m.getFloorEntry(lo)));
		}

		// Abstract methods defined in ascending vs descending classes
		// These relay to the appropriate absolute versions

		abstract BinaryTree.Entry<K, V> subLowest();

		abstract BinaryTree.Entry<K, V> subHighest();

		abstract BinaryTree.Entry<K, V> subCeiling(K key);

		abstract BinaryTree.Entry<K, V> subHigher(K key);

		abstract BinaryTree.Entry<K, V> subFloor(K key);

		abstract BinaryTree.Entry<K, V> subLower(K key);

		/** Returns ascending iterator from the perspective of this submap */
		abstract Iterator<K> keyIterator();

		abstract Spliterator<K> keySpliterator();

		/** Returns descending iterator from the perspective of this submap */
		abstract Iterator<K> descendingKeyIterator();

		// public methods

		@Override
		public boolean isEmpty() {
			return (fromStart && toEnd) ? m.isEmpty() : entrySet().isEmpty();
		}

		@Override
		public int size() {
			return (fromStart && toEnd) ? m.size() : entrySet().size();
		}

		@Override
		public final boolean containsKey(Object key) {
			return inRange(key) && m.containsKey(key);
		}

		@Override
		public final V put(K key, V value) {
			if (!inRange(key)) throw new IllegalArgumentException("key out of range");
			return m.put(key, value);
		}

		@Override
		public final V get(Object key) {
			return !inRange(key) ? null : m.get(key);
		}

		@Override
		public final V remove(Object key) {
			return !inRange(key) ? null : m.remove(key);
		}

		@Override
		public final Map.Entry<K, V> ceilingEntry(K key) {
			return exportEntry(subCeiling(key));
		}

		@Override
		public final K ceilingKey(K key) {
			return keyOrNull(subCeiling(key));
		}

		@Override
		public final Map.Entry<K, V> higherEntry(K key) {
			return exportEntry(subHigher(key));
		}

		@Override
		public final K higherKey(K key) {
			return keyOrNull(subHigher(key));
		}

		@Override
		public final Map.Entry<K, V> floorEntry(K key) {
			return exportEntry(subFloor(key));
		}

		@Override
		public final K floorKey(K key) {
			return keyOrNull(subFloor(key));
		}

		@Override
		public final Map.Entry<K, V> lowerEntry(K key) {
			return exportEntry(subLower(key));
		}

		@Override
		public final K lowerKey(K key) {
			return keyOrNull(subLower(key));
		}

		@Override
		public final K firstKey() {
			return key(subLowest());
		}

		@Override
		public final K lastKey() {
			return key(subHighest());
		}

		@Override
		public final Map.Entry<K, V> firstEntry() {
			return exportEntry(subLowest());
		}

		@Override
		public final Map.Entry<K, V> lastEntry() {
			return exportEntry(subHighest());
		}

		@Override
		public final Map.Entry<K, V> pollFirstEntry() {
			BinaryTree.Entry<K, V> e = subLowest();
			Map.Entry<K, V> result = exportEntry(e);
			if (e != null) m.deleteEntry(e);
			return result;
		}

		@Override
		public final Map.Entry<K, V> pollLastEntry() {
			BinaryTree.Entry<K, V> e = subHighest();
			Map.Entry<K, V> result = exportEntry(e);
			if (e != null) m.deleteEntry(e);
			return result;
		}

		// Views
		transient NavigableMap<K, V> descendingMapView;
		transient EntrySetView entrySetView;
		transient KeySet<K> navigableKeySetView;

		@Override
		public final NavigableSet<K> navigableKeySet() {
			KeySet<K> nksv = navigableKeySetView;
			return (nksv != null) ? nksv : (navigableKeySetView = new BinaryTree.KeySet<>(this));
		}

		@Override
		public final Set<K> keySet() {
			return navigableKeySet();
		}

		@Override
		public NavigableSet<K> descendingKeySet() {
			return descendingMap().navigableKeySet();
		}

		@Override
		public final SortedMap<K, V> subMap(K fromKey, K toKey) {
			return subMap(fromKey, true, toKey, false);
		}

		@Override
		public final SortedMap<K, V> headMap(K toKey) {
			return headMap(toKey, false);
		}

		@Override
		public final SortedMap<K, V> tailMap(K fromKey) {
			return tailMap(fromKey, true);
		}

		// View classes

		abstract class EntrySetView extends AbstractSet<Map.Entry<K, V>> {
			private transient int size = -1, sizeModCount;

			@Override
			public int size() {
				if (fromStart && toEnd) return m.size();
				if (size == -1 || sizeModCount != m.modCount) {
					sizeModCount = m.modCount;
					size = 0;
					Iterator<?> i = iterator();
					while (i.hasNext()) {
						size++;
						i.next();
					}
				}
				return size;
			}

			@Override
			public boolean isEmpty() {
				BinaryTree.Entry<K, V> n = absLowest();
				return n == null || tooHigh(n.key);
			}

			@Override
			public boolean contains(Object o) {
				if (!(o instanceof Map.Entry)) return false;
				Map.Entry<?, ?> entry = (Map.Entry<?, ?>) o;
				Object key = entry.getKey();
				if (!inRange(key)) return false;
				BinaryTree.Entry<?, ?> node = m.getEntry(key);
				return node != null && valEquals(node.getValue(), entry.getValue());
			}

			@Override
			public boolean remove(Object o) {
				if (!(o instanceof Map.Entry)) return false;
				Map.Entry<?, ?> entry = (Map.Entry<?, ?>) o;
				Object key = entry.getKey();
				if (!inRange(key)) return false;
				BinaryTree.Entry<K, V> node = m.getEntry(key);
				if (node != null && valEquals(node.getValue(), entry.getValue())) {
					m.deleteEntry(node);
					return true;
				}
				return false;
			}
		}

		/**
		 * Iterators for SubMaps
		 */
		abstract class SubMapIterator<T> implements Iterator<T> {
			BinaryTree.Entry<K, V> lastReturned;
			BinaryTree.Entry<K, V> next;
			final Object fenceKey;
			int expectedModCount;

			SubMapIterator(BinaryTree.Entry<K, V> first, BinaryTree.Entry<K, V> fence) {
				expectedModCount = m.modCount;
				lastReturned = null;
				next = first;
				fenceKey = fence == null ? UNBOUNDED : fence.key;
			}

			@Override
			public final boolean hasNext() {
				return next != null && next.key != fenceKey;
			}

			final BinaryTree.Entry<K, V> nextEntry() {
				BinaryTree.Entry<K, V> e = next;
				if (e == null || e.key == fenceKey) throw new NoSuchElementException();
				if (m.modCount != expectedModCount) throw new ConcurrentModificationException();
				next = successor(e);
				lastReturned = e;
				return e;
			}

			final BinaryTree.Entry<K, V> prevEntry() {
				BinaryTree.Entry<K, V> e = next;
				if (e == null || e.key == fenceKey) throw new NoSuchElementException();
				if (m.modCount != expectedModCount) throw new ConcurrentModificationException();
				next = predecessor(e);
				lastReturned = e;
				return e;
			}

			final void removeAscending() {
				if (lastReturned == null) throw new IllegalStateException();
				if (m.modCount != expectedModCount) throw new ConcurrentModificationException();
				// deleted entries are replaced by their successors
				if (lastReturned.left != null && lastReturned.right != null) next = lastReturned;
				m.deleteEntry(lastReturned);
				lastReturned = null;
				expectedModCount = m.modCount;
			}

			final void removeDescending() {
				if (lastReturned == null) throw new IllegalStateException();
				if (m.modCount != expectedModCount) throw new ConcurrentModificationException();
				m.deleteEntry(lastReturned);
				lastReturned = null;
				expectedModCount = m.modCount;
			}

		}

		final class SubMapEntryIterator extends SubMapIterator<Map.Entry<K, V>> {
			SubMapEntryIterator(BinaryTree.Entry<K, V> first, BinaryTree.Entry<K, V> fence) {
				super(first, fence);
			}

			@Override
			public Map.Entry<K, V> next() {
				return nextEntry();
			}

			@Override
			public void remove() {
				removeAscending();
			}
		}

		final class DescendingSubMapEntryIterator extends SubMapIterator<Map.Entry<K, V>> {
			DescendingSubMapEntryIterator(BinaryTree.Entry<K, V> last, BinaryTree.Entry<K, V> fence) {
				super(last, fence);
			}

			@Override
			public Map.Entry<K, V> next() {
				return prevEntry();
			}

			@Override
			public void remove() {
				removeDescending();
			}
		}

		// Implement minimal Spliterator as KeySpliterator backup
		final class SubMapKeyIterator extends SubMapIterator<K> implements Spliterator<K> {
			SubMapKeyIterator(BinaryTree.Entry<K, V> first, BinaryTree.Entry<K, V> fence) {
				super(first, fence);
			}

			@Override
			public K next() {
				return nextEntry().key;
			}

			@Override
			public void remove() {
				removeAscending();
			}

			@Override
			public Spliterator<K> trySplit() {
				return null;
			}

			@Override
			public void forEachRemaining(Consumer<? super K> action) {
				while (hasNext())
					action.accept(next());
			}

			@Override
			public boolean tryAdvance(Consumer<? super K> action) {
				if (hasNext()) {
					action.accept(next());
					return true;
				}
				return false;
			}

			@Override
			public long estimateSize() {
				return Long.MAX_VALUE;
			}

			@Override
			public int characteristics() {
				return Spliterator.DISTINCT | Spliterator.ORDERED | Spliterator.SORTED;
			}

			@Override
			public final Comparator<? super K> getComparator() {
				return NavigableSubMap.this.comparator();
			}
		}

		final class DescendingSubMapKeyIterator extends SubMapIterator<K> implements Spliterator<K> {
			DescendingSubMapKeyIterator(BinaryTree.Entry<K, V> last, BinaryTree.Entry<K, V> fence) {
				super(last, fence);
			}

			@Override
			public K next() {
				return prevEntry().key;
			}

			@Override
			public void remove() {
				removeDescending();
			}

			@Override
			public Spliterator<K> trySplit() {
				return null;
			}

			@Override
			public void forEachRemaining(Consumer<? super K> action) {
				while (hasNext())
					action.accept(next());
			}

			@Override
			public boolean tryAdvance(Consumer<? super K> action) {
				if (hasNext()) {
					action.accept(next());
					return true;
				}
				return false;
			}

			@Override
			public long estimateSize() {
				return Long.MAX_VALUE;
			}

			@Override
			public int characteristics() {
				return Spliterator.DISTINCT | Spliterator.ORDERED;
			}
		}
	}

	/**
	 * @serial include
	 */
	static final class AscendingSubMap<K, V> extends NavigableSubMap<K, V> {
		private static final long serialVersionUID = 912986545866124060L;

		AscendingSubMap(BinaryTree<K, V> m, boolean fromStart, K lo, boolean loInclusive, boolean toEnd, K hi, boolean hiInclusive) {
			super(m, fromStart, lo, loInclusive, toEnd, hi, hiInclusive);
		}

		@Override
		public Comparator<? super K> comparator() {
			return m.comparator();
		}

		@Override
		public NavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
			if (!inRange(fromKey, fromInclusive)) throw new IllegalArgumentException("fromKey out of range");
			if (!inRange(toKey, toInclusive)) throw new IllegalArgumentException("toKey out of range");
			return new AscendingSubMap<>(m, false, fromKey, fromInclusive, false, toKey, toInclusive);
		}

		@Override
		public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
			if (!inRange(toKey, inclusive)) throw new IllegalArgumentException("toKey out of range");
			return new AscendingSubMap<>(m, fromStart, lo, loInclusive, false, toKey, inclusive);
		}

		@Override
		public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
			if (!inRange(fromKey, inclusive)) throw new IllegalArgumentException("fromKey out of range");
			return new AscendingSubMap<>(m, false, fromKey, inclusive, toEnd, hi, hiInclusive);
		}

		@Override
		public NavigableMap<K, V> descendingMap() {
			NavigableMap<K, V> mv = descendingMapView;
			return (mv != null) ? mv : (descendingMapView = new DescendingSubMap<>(m, fromStart, lo, loInclusive, toEnd, hi, hiInclusive));
		}

		@Override
		Iterator<K> keyIterator() {
			return new SubMapKeyIterator(absLowest(), absHighFence());
		}

		@Override
		Spliterator<K> keySpliterator() {
			return new SubMapKeyIterator(absLowest(), absHighFence());
		}

		@Override
		Iterator<K> descendingKeyIterator() {
			return new DescendingSubMapKeyIterator(absHighest(), absLowFence());
		}

		final class AscendingEntrySetView extends EntrySetView {
			@Override
			public Iterator<Map.Entry<K, V>> iterator() {
				return new SubMapEntryIterator(absLowest(), absHighFence());
			}
		}

		@Override
		public Set<Map.Entry<K, V>> entrySet() {
			EntrySetView es = entrySetView;
			return (es != null) ? es : (entrySetView = new AscendingEntrySetView());
		}

		@Override
		BinaryTree.Entry<K, V> subLowest() {
			return absLowest();
		}

		@Override
		BinaryTree.Entry<K, V> subHighest() {
			return absHighest();
		}

		@Override
		BinaryTree.Entry<K, V> subCeiling(K key) {
			return absCeiling(key);
		}

		@Override
		BinaryTree.Entry<K, V> subHigher(K key) {
			return absHigher(key);
		}

		@Override
		BinaryTree.Entry<K, V> subFloor(K key) {
			return absFloor(key);
		}

		@Override
		BinaryTree.Entry<K, V> subLower(K key) {
			return absLower(key);
		}
	}

	/**
	 * @serial include
	 */
	static final class DescendingSubMap<K, V> extends NavigableSubMap<K, V> {
		private static final long serialVersionUID = 912986545866120460L;

		DescendingSubMap(BinaryTree<K, V> m, boolean fromStart, K lo, boolean loInclusive, boolean toEnd, K hi, boolean hiInclusive) {
			super(m, fromStart, lo, loInclusive, toEnd, hi, hiInclusive);
		}

		private final Comparator<? super K> reverseComparator = Collections.reverseOrder(m.comparator);

		@Override
		public Comparator<? super K> comparator() {
			return reverseComparator;
		}

		@Override
		public NavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
			if (!inRange(fromKey, fromInclusive)) throw new IllegalArgumentException("fromKey out of range");
			if (!inRange(toKey, toInclusive)) throw new IllegalArgumentException("toKey out of range");
			return new DescendingSubMap<>(m, false, toKey, toInclusive, false, fromKey, fromInclusive);
		}

		@Override
		public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
			if (!inRange(toKey, inclusive)) throw new IllegalArgumentException("toKey out of range");
			return new DescendingSubMap<>(m, false, toKey, inclusive, toEnd, hi, hiInclusive);
		}

		@Override
		public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
			if (!inRange(fromKey, inclusive)) throw new IllegalArgumentException("fromKey out of range");
			return new DescendingSubMap<>(m, fromStart, lo, loInclusive, false, fromKey, inclusive);
		}

		@Override
		public NavigableMap<K, V> descendingMap() {
			NavigableMap<K, V> mv = descendingMapView;
			return (mv != null) ? mv : (descendingMapView = new AscendingSubMap<>(m, fromStart, lo, loInclusive, toEnd, hi, hiInclusive));
		}

		@Override
		Iterator<K> keyIterator() {
			return new DescendingSubMapKeyIterator(absHighest(), absLowFence());
		}

		@Override
		Spliterator<K> keySpliterator() {
			return new DescendingSubMapKeyIterator(absHighest(), absLowFence());
		}

		@Override
		Iterator<K> descendingKeyIterator() {
			return new SubMapKeyIterator(absLowest(), absHighFence());
		}

		final class DescendingEntrySetView extends EntrySetView {
			@Override
			public Iterator<Map.Entry<K, V>> iterator() {
				return new DescendingSubMapEntryIterator(absHighest(), absLowFence());
			}
		}

		@Override
		public Set<Map.Entry<K, V>> entrySet() {
			EntrySetView es = entrySetView;
			return (es != null) ? es : (entrySetView = new DescendingEntrySetView());
		}

		@Override
		BinaryTree.Entry<K, V> subLowest() {
			return absHighest();
		}

		@Override
		BinaryTree.Entry<K, V> subHighest() {
			return absLowest();
		}

		@Override
		BinaryTree.Entry<K, V> subCeiling(K key) {
			return absFloor(key);
		}

		@Override
		BinaryTree.Entry<K, V> subHigher(K key) {
			return absLower(key);
		}

		@Override
		BinaryTree.Entry<K, V> subFloor(K key) {
			return absCeiling(key);
		}

		@Override
		BinaryTree.Entry<K, V> subLower(K key) {
			return absHigher(key);
		}
	}

	/**
	 * This class exists solely for the sake of serialization compatibility with previous releases of BinaryTree that
	 * did not support NavigableMap. It translates an old-version SubMap into a new-version AscendingSubMap. This class
	 * is never otherwise used.
	 *
	 * @serial include
	 */
	private class SubMap extends AbstractMap<K, V> implements SortedMap<K, V>, java.io.Serializable {
		private static final long serialVersionUID = -6520786458950516097L;
		private boolean fromStart = false, toEnd = false;
		private K fromKey, toKey;

		private Object readResolve() {
			return new AscendingSubMap<>(BinaryTree.this, fromStart, fromKey, true, toEnd, toKey, false);
		}

		@Override
		public Set<Map.Entry<K, V>> entrySet() {
			throw new InternalError();
		}

		@Override
		public K lastKey() {
			throw new InternalError();
		}

		@Override
		public K firstKey() {
			throw new InternalError();
		}

		@Override
		public SortedMap<K, V> subMap(K fromKey, K toKey) {
			throw new InternalError();
		}

		@Override
		public SortedMap<K, V> headMap(K toKey) {
			throw new InternalError();
		}

		@Override
		public SortedMap<K, V> tailMap(K fromKey) {
			throw new InternalError();
		}

		@Override
		public Comparator<? super K> comparator() {
			throw new InternalError();
		}
	}

	// Red-black mechanics

	private static final boolean RED = false;
	private static final boolean BLACK = true;

	/**
	 * Node in the Tree. Doubles as a means to pass key-value pairs back to user (see Map.Entry).
	 */

	static final class Entry<K, V> implements Map.Entry<K, V> {
		K key;
		V value;
		Entry<K, V> left;
		Entry<K, V> right;
		Entry<K, V> parent;
		boolean color = BLACK;

		/**
		 * Make a new cell with given key, value, and parent, and with {@code null} child links, and BLACK color.
		 */
		Entry(K key, V value, Entry<K, V> parent) {
			this.key = key;
			this.value = value;
			this.parent = parent;
		}

		/**
		 * Returns the key.
		 *
		 * @return the key
		 */
		@Override
		public K getKey() {
			return key;
		}

		/**
		 * Returns the value associated with the key.
		 *
		 * @return the value associated with the key
		 */
		@Override
		public V getValue() {
			return value;
		}

		/**
		 * Replaces the value currently associated with the key with the given value.
		 *
		 * @return the value associated with the key before this method was called
		 */
		@Override
		public V setValue(V value) {
			V oldValue = this.value;
			this.value = value;
			return oldValue;
		}

		@Override
		public boolean equals(Object o) {
			if (!(o instanceof Map.Entry)) return false;
			Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;

			return valEquals(key, e.getKey()) && valEquals(value, e.getValue());
		}

		@Override
		public int hashCode() {
			int keyHash = (key == null ? 0 : key.hashCode());
			int valueHash = (value == null ? 0 : value.hashCode());
			return keyHash ^ valueHash;
		}

		@Override
		public String toString() {
			return key + "=" + value;
		}
	}

	/**
	 * Returns the first Entry in the BinaryTree (according to the BinaryTree's key-sort function). Returns null if the
	 * BinaryTree is empty.
	 */
	final Entry<K, V> getFirstEntry() {
		Entry<K, V> p = root;
		if (p != null) while (p.left != null)
			p = p.left;
		return p;
	}

	/**
	 * Returns the last Entry in the BinaryTree (according to the BinaryTree's key-sort function). Returns null if the
	 * BinaryTree is empty.
	 */
	final Entry<K, V> getLastEntry() {
		Entry<K, V> p = root;
		if (p != null) while (p.right != null)
			p = p.right;
		return p;
	}

	/**
	 * Returns the successor of the specified Entry, or null if no such.
	 */
	static <K, V> BinaryTree.Entry<K, V> successor(Entry<K, V> t) {
		if (t == null) return null;
		else if (t.right != null) {
			Entry<K, V> p = t.right;
			while (p.left != null)
				p = p.left;
			return p;
		} else {
			Entry<K, V> p = t.parent;
			Entry<K, V> ch = t;
			while (p != null && ch == p.right) {
				ch = p;
				p = p.parent;
			}
			return p;
		}
	}

	/**
	 * Returns the predecessor of the specified Entry, or null if no such.
	 */
	static <K, V> Entry<K, V> predecessor(Entry<K, V> t) {
		if (t == null) return null;
		else if (t.left != null) {
			Entry<K, V> p = t.left;
			while (p.right != null)
				p = p.right;
			return p;
		} else {
			Entry<K, V> p = t.parent;
			Entry<K, V> ch = t;
			while (p != null && ch == p.left) {
				ch = p;
				p = p.parent;
			}
			return p;
		}
	}

	/**
	 * Balancing operations. Implementations of rebalancings during insertion and deletion are slightly different than
	 * the CLR version. Rather than using dummy nilnodes, we use a set of accessors that deal properly with null. They
	 * are used to avoid messiness surrounding nullness checks in the main algorithms.
	 */

	private static <K, V> boolean colorOf(Entry<K, V> p) {
		return (p == null ? BLACK : p.color);
	}

	private static <K, V> Entry<K, V> parentOf(Entry<K, V> p) {
		return (p == null ? null : p.parent);
	}

	private static <K, V> void setColor(Entry<K, V> p, boolean c) {
		if (p != null) p.color = c;
	}

	private static <K, V> Entry<K, V> leftOf(Entry<K, V> p) {
		return (p == null) ? null : p.left;
	}

	private static <K, V> Entry<K, V> rightOf(Entry<K, V> p) {
		return (p == null) ? null : p.right;
	}

	/** From CLR */
	private void rotateLeft(Entry<K, V> p) {
		if (p != null) {
			Entry<K, V> r = p.right;
			p.right = r.left;
			if (r.left != null) r.left.parent = p;
			r.parent = p.parent;
			if (p.parent == null) root = r;
			else if (p.parent.left == p) p.parent.left = r;
			else
				p.parent.right = r;
			r.left = p;
			p.parent = r;
		}
	}

	/** From CLR */
	private void rotateRight(Entry<K, V> p) {
		if (p != null) {
			Entry<K, V> l = p.left;
			p.left = l.right;
			if (l.right != null) l.right.parent = p;
			l.parent = p.parent;
			if (p.parent == null) root = l;
			else if (p.parent.right == p) p.parent.right = l;
			else
				p.parent.left = l;
			l.right = p;
			p.parent = l;
		}
	}

	/** From CLR */
	private void fixAfterInsertion(Entry<K, V> x) {
		x.color = RED;

		while (x != null && x != root && x.parent.color == RED) {
			if (parentOf(x) == leftOf(parentOf(parentOf(x)))) {
				Entry<K, V> y = rightOf(parentOf(parentOf(x)));
				if (colorOf(y) == RED) {
					setColor(parentOf(x), BLACK);
					setColor(y, BLACK);
					setColor(parentOf(parentOf(x)), RED);
					x = parentOf(parentOf(x));
				} else {
					if (x == rightOf(parentOf(x))) {
						x = parentOf(x);
						rotateLeft(x);
					}
					setColor(parentOf(x), BLACK);
					setColor(parentOf(parentOf(x)), RED);
					rotateRight(parentOf(parentOf(x)));
				}
			} else {
				Entry<K, V> y = leftOf(parentOf(parentOf(x)));
				if (colorOf(y) == RED) {
					setColor(parentOf(x), BLACK);
					setColor(y, BLACK);
					setColor(parentOf(parentOf(x)), RED);
					x = parentOf(parentOf(x));
				} else {
					if (x == leftOf(parentOf(x))) {
						x = parentOf(x);
						rotateRight(x);
					}
					setColor(parentOf(x), BLACK);
					setColor(parentOf(parentOf(x)), RED);
					rotateLeft(parentOf(parentOf(x)));
				}
			}
		}
		root.color = BLACK;
	}

	/**
	 * Delete node p, and then rebalance the tree.
	 */
	private void deleteEntry(Entry<K, V> p) {
		modCount++;
		size--;

		// If strictly internal, copy successor's element to p and then make p
		// point to successor.
		if (p.left != null && p.right != null) {
			Entry<K, V> s = successor(p);
			p.key = s.key;
			p.value = s.value;
			p = s;
		} // p has 2 children

		// Start fixup at replacement node, if it exists.
		Entry<K, V> replacement = (p.left != null ? p.left : p.right);

		if (replacement != null) {
			// Link replacement to parent
			replacement.parent = p.parent;
			if (p.parent == null) root = replacement;
			else if (p == p.parent.left) p.parent.left = replacement;
			else
				p.parent.right = replacement;

			// Null out links so they are OK to use by fixAfterDeletion.
			p.left = p.right = p.parent = null;

			// Fix replacement
			if (p.color == BLACK) fixAfterDeletion(replacement);
		} else if (p.parent == null) { // return if we are the only node.
			root = null;
		} else { // No children. Use self as phantom replacement and unlink.
			if (p.color == BLACK) fixAfterDeletion(p);

			if (p.parent != null) {
				if (p == p.parent.left) p.parent.left = null;
				else if (p == p.parent.right) p.parent.right = null;
				p.parent = null;
			}
		}
	}

	/** From CLR */
	private void fixAfterDeletion(Entry<K, V> x) {
		while (x != root && colorOf(x) == BLACK) {
			if (x == leftOf(parentOf(x))) {
				Entry<K, V> sib = rightOf(parentOf(x));

				if (colorOf(sib) == RED) {
					setColor(sib, BLACK);
					setColor(parentOf(x), RED);
					rotateLeft(parentOf(x));
					sib = rightOf(parentOf(x));
				}

				if (colorOf(leftOf(sib)) == BLACK && colorOf(rightOf(sib)) == BLACK) {
					setColor(sib, RED);
					x = parentOf(x);
				} else {
					if (colorOf(rightOf(sib)) == BLACK) {
						setColor(leftOf(sib), BLACK);
						setColor(sib, RED);
						rotateRight(sib);
						sib = rightOf(parentOf(x));
					}
					setColor(sib, colorOf(parentOf(x)));
					setColor(parentOf(x), BLACK);
					setColor(rightOf(sib), BLACK);
					rotateLeft(parentOf(x));
					x = root;
				}
			} else { // symmetric
				Entry<K, V> sib = leftOf(parentOf(x));

				if (colorOf(sib) == RED) {
					setColor(sib, BLACK);
					setColor(parentOf(x), RED);
					rotateRight(parentOf(x));
					sib = leftOf(parentOf(x));
				}

				if (colorOf(rightOf(sib)) == BLACK && colorOf(leftOf(sib)) == BLACK) {
					setColor(sib, RED);
					x = parentOf(x);
				} else {
					if (colorOf(leftOf(sib)) == BLACK) {
						setColor(rightOf(sib), BLACK);
						setColor(sib, RED);
						rotateLeft(sib);
						sib = leftOf(parentOf(x));
					}
					setColor(sib, colorOf(parentOf(x)));
					setColor(parentOf(x), BLACK);
					setColor(leftOf(sib), BLACK);
					rotateRight(parentOf(x));
					x = root;
				}
			}
		}

		setColor(x, BLACK);
	}

	private static final long serialVersionUID = 919286545866124006L;

	/**
	 * Save the state of the {@code BinaryTree} instance to a stream (i.e., serialize it).
	 *
	 * @serialData The <em>size</em> of the BinaryTree (the number of key-value mappings) is emitted (int), followed by
	 *             the key (Object) and value (Object) for each key-value mapping represented by the BinaryTree. The
	 *             key-value mappings are emitted in key-order (as determined by the BinaryTree's Comparator, or by the
	 *             keys' natural ordering if the BinaryTree has no Comparator).
	 */
	private void writeObject(java.io.ObjectOutputStream s) throws java.io.IOException {
		// Write out the Comparator and any hidden stuff
		s.defaultWriteObject();

		// Write out size (number of Mappings)
		s.writeInt(size);

		// Write out keys and values (alternating)
		for (Iterator<Map.Entry<K, V>> i = entrySet().iterator(); i.hasNext();) {
			Map.Entry<K, V> e = i.next();
			s.writeObject(e.getKey());
			s.writeObject(e.getValue());
		}
	}

	/**
	 * Reconstitute the {@code BinaryTree} instance from a stream (i.e., deserialize it).
	 */
	private void readObject(final java.io.ObjectInputStream s) throws java.io.IOException, ClassNotFoundException {
		// Read in the Comparator and any hidden stuff
		s.defaultReadObject();

		// Read in size
		int size = s.readInt();

		buildFromSorted(size, null, s, null);
	}

	/** Intended to be called only from TreeSet.readObject */
	void readTreeSet(int size, java.io.ObjectInputStream s, V defaultVal) throws java.io.IOException, ClassNotFoundException {
		buildFromSorted(size, null, s, defaultVal);
	}

	/** Intended to be called only from TreeSet.addAll */
	void addAllForTreeSet(SortedSet<? extends K> set, V defaultVal) {
		try {
			buildFromSorted(set.size(), set.iterator(), null, defaultVal);
		} catch (java.io.IOException cannotHappen) {
		} catch (ClassNotFoundException cannotHappen) {
		}
	}

	/**
	 * Linear time tree building algorithm from sorted data. Can accept keys and/or values from iterator or stream. This
	 * leads to too many parameters, but seems better than alternatives. The four formats that this method accepts are:
	 * 1) An iterator of Map.Entries. (it != null, defaultVal == null). 2) An iterator of keys. (it != null, defaultVal
	 * != null). 3) A stream of alternating serialized keys and values. (it == null, defaultVal == null). 4) A stream of
	 * serialized keys. (it == null, defaultVal != null). It is assumed that the comparator of the BinaryTree is already
	 * set prior to calling this method.
	 *
	 * @param size the number of keys (or key-value pairs) to be read from the iterator or stream
	 * @param it If non-null, new entries are created from entries or keys read from this iterator.
	 * @param str If non-null, new entries are created from keys and possibly values read from this stream in serialized
	 *            form. Exactly one of it and str should be non-null.
	 * @param defaultVal if non-null, this default value is used for each value in the map. If null, each value is read
	 *            from iterator or stream, as described above.
	 * @throws java.io.IOException propagated from stream reads. This cannot occur if str is null.
	 * @throws ClassNotFoundException propagated from readObject. This cannot occur if str is null.
	 */
	private void buildFromSorted(int size, Iterator<?> it, java.io.ObjectInputStream str, V defaultVal) throws java.io.IOException,
			ClassNotFoundException {
		this.size = size;
		root = buildFromSorted(0, 0, size - 1, computeRedLevel(size), it, str, defaultVal);
	}

	/**
	 * Recursive "helper method" that does the real work of the previous method. Identically named parameters have
	 * identical definitions. Additional parameters are documented below. It is assumed that the comparator and size
	 * fields of the BinaryTree are already set prior to calling this method. (It ignores both fields.)
	 *
	 * @param level the current level of tree. Initial call should be 0.
	 * @param lo the first element index of this subtree. Initial should be 0.
	 * @param hi the last element index of this subtree. Initial should be size-1.
	 * @param redLevel the level at which nodes should be red. Must be equal to computeRedLevel for tree of this size.
	 */
	@SuppressWarnings("unchecked")
	private final Entry<K, V> buildFromSorted(int level, int lo, int hi, int redLevel, Iterator<?> it, java.io.ObjectInputStream str, V defaultVal)
			throws java.io.IOException, ClassNotFoundException {
		/*
		 * Strategy: The root is the middlemost element. To get to it, we have to first recursively construct the entire
		 * left subtree, so as to grab all of its elements. We can then proceed with right subtree. The lo and hi
		 * arguments are the minimum and maximum indices to pull out of the iterator or stream for current subtree. They
		 * are not actually indexed, we just proceed sequentially, ensuring that items are extracted in corresponding
		 * order.
		 */

		if (hi < lo) return null;

		int mid = (lo + hi) >>> 1;

		Entry<K, V> left = null;
		if (lo < mid) left = buildFromSorted(level + 1, lo, mid - 1, redLevel, it, str, defaultVal);

		// extract key and/or value from iterator or stream
		K key;
		V value;
		if (it != null) {
			if (defaultVal == null) {
				Map.Entry<?, ?> entry = (Map.Entry<?, ?>) it.next();
				key = (K) entry.getKey();
				value = (V) entry.getValue();
			} else {
				key = (K) it.next();
				value = defaultVal;
			}
		} else { // use stream
			key = (K) str.readObject();
			value = (defaultVal != null ? defaultVal : (V) str.readObject());
		}

		Entry<K, V> middle = new Entry<>(key, value, null);

		// color nodes in non-full bottommost level red
		if (level == redLevel) middle.color = RED;

		if (left != null) {
			middle.left = left;
			left.parent = middle;
		}

		if (mid < hi) {
			Entry<K, V> right = buildFromSorted(level + 1, mid + 1, hi, redLevel, it, str, defaultVal);
			middle.right = right;
			right.parent = middle;
		}

		return middle;
	}

	/**
	 * Find the level down to which to assign all nodes BLACK. This is the last `full' level of the complete binary tree
	 * produced by buildTree. The remaining nodes are colored RED. (This makes a `nice' set of color assignments wrt
	 * future insertions.) This level number is computed by finding the number of splits needed to reach the zeroeth
	 * node. (The answer is ~lg(N), but in any case must be computed by same quick O(lg(N)) loop.)
	 */
	private static int computeRedLevel(int sz) {
		int level = 0;
		for (int m = sz - 1; m >= 0; m = m / 2 - 1)
			level++;
		return level;
	}

	/**
	 * Currently, we support Spliterator-based versions only for the full map, in either plain of descending form,
	 * otherwise relying on defaults because size estimation for submaps would dominate costs. The type tests needed to
	 * check these for key views are not very nice but avoid disrupting existing class structures. Callers must use
	 * plain default spliterators if this returns null.
	 */
	static <K> Spliterator<K> keySpliteratorFor(NavigableMap<K, ?> m) {
		if (m instanceof BinaryTree) {
			@SuppressWarnings("unchecked")
			BinaryTree<K, Object> t = (BinaryTree<K, Object>) m;
			return t.keySpliterator();
		}
		if (m instanceof DescendingSubMap) {
			@SuppressWarnings("unchecked")
			DescendingSubMap<K, ?> dm = (DescendingSubMap<K, ?>) m;
			BinaryTree<K, ?> tm = dm.m;
			if (dm == tm.descendingMap) {
				@SuppressWarnings("unchecked")
				BinaryTree<K, Object> t = (BinaryTree<K, Object>) tm;
				return t.descendingKeySpliterator();
			}
		}
		@SuppressWarnings("unchecked")
		NavigableSubMap<K, ?> sm = (NavigableSubMap<K, ?>) m;
		return sm.keySpliterator();
	}

	final Spliterator<K> keySpliterator() {
		return new KeySpliterator<K, V>(this, null, null, 0, -1, 0);
	}

	final Spliterator<K> descendingKeySpliterator() {
		return new DescendingKeySpliterator<K, V>(this, null, null, 0, -2, 0);
	}

	/**
	 * Base class for spliterators. Iteration starts at a given origin and continues up to but not including a given
	 * fence (or null for end). At top-level, for ascending cases, the first split uses the root as
	 * left-fence/right-origin. From there, right-hand splits replace the current fence with its left child, also
	 * serving as origin for the split-off spliterator. Left-hands are symmetric. Descending versions place the origin
	 * at the end and invert ascending split rules. This base class is non-commital about directionality, or whether the
	 * top-level spliterator covers the whole tree. This means that the actual split mechanics are located in
	 * subclasses. Some of the subclass trySplit methods are identical (except for return types), but not nicely
	 * factorable. Currently, subclass versions exist only for the full map (including descending keys via its
	 * descendingMap). Others are possible but currently not worthwhile because submaps require O(n) computations to
	 * determine size, which substantially limits potential speed-ups of using custom Spliterators versus default
	 * mechanics. To boostrap initialization, external constructors use negative size estimates: -1 for ascend, -2 for
	 * descend.
	 */
	static class BinaryTreeSpliterator<K, V> {
		final BinaryTree<K, V> tree;
		BinaryTree.Entry<K, V> current; // traverser; initially first node in range
		BinaryTree.Entry<K, V> fence; // one past last, or null
		int side; // 0: top, -1: is a left split, +1: right
		int est; // size estimate (exact only for top-level)
		int expectedModCount; // for CME checks

		BinaryTreeSpliterator(BinaryTree<K, V> tree, BinaryTree.Entry<K, V> origin, BinaryTree.Entry<K, V> fence, int side, int est,
				int expectedModCount) {
			this.tree = tree;
			this.current = origin;
			this.fence = fence;
			this.side = side;
			this.est = est;
			this.expectedModCount = expectedModCount;
		}

		final int getEstimate() { // force initialization
			int s;
			BinaryTree<K, V> t;
			if ((s = est) < 0) {
				if ((t = tree) != null) {
					current = (s == -1) ? t.getFirstEntry() : t.getLastEntry();
					s = est = t.size;
					expectedModCount = t.modCount;
				} else
					s = est = 0;
			}
			return s;
		}

		public final long estimateSize() {
			return (long) getEstimate();
		}
	}

	static final class KeySpliterator<K, V> extends BinaryTreeSpliterator<K, V> implements Spliterator<K> {
		KeySpliterator(BinaryTree<K, V> tree, BinaryTree.Entry<K, V> origin, BinaryTree.Entry<K, V> fence, int side, int est, int expectedModCount) {
			super(tree, origin, fence, side, est, expectedModCount);
		}

		@Override
		public KeySpliterator<K, V> trySplit() {
			if (est < 0) getEstimate(); // force initialization
			int d = side;
			BinaryTree.Entry<K, V> e = current, f = fence, s = ((e == null || e == f) ? null : // empty
					(d == 0) ? tree.root : // was top
							(d > 0) ? e.right : // was right
									(d < 0 && f != null) ? f.left : // was left
											null);
			if (s != null && s != e && s != f && tree.compare(e.key, s.key) < 0) { // e not already past s
				side = 1;
				return new KeySpliterator<>(tree, e, current = s, -1, est >>>= 1, expectedModCount);
			}
			return null;
		}

		@Override
		public void forEachRemaining(Consumer<? super K> action) {
			if (action == null) throw new NullPointerException();
			if (est < 0) getEstimate(); // force initialization
			BinaryTree.Entry<K, V> f = fence, e, p, pl;
			if ((e = current) != null && e != f) {
				current = f; // exhaust
				do {
					action.accept(e.key);
					if ((p = e.right) != null) {
						while ((pl = p.left) != null)
							p = pl;
					} else {
						while ((p = e.parent) != null && e == p.right)
							e = p;
					}
				} while ((e = p) != null && e != f);
				if (tree.modCount != expectedModCount) throw new ConcurrentModificationException();
			}
		}

		@Override
		public boolean tryAdvance(Consumer<? super K> action) {
			BinaryTree.Entry<K, V> e;
			if (action == null) throw new NullPointerException();
			if (est < 0) getEstimate(); // force initialization
			if ((e = current) == null || e == fence) return false;
			current = successor(e);
			action.accept(e.key);
			if (tree.modCount != expectedModCount) throw new ConcurrentModificationException();
			return true;
		}

		@Override
		public int characteristics() {
			return (side == 0 ? Spliterator.SIZED : 0) | Spliterator.DISTINCT | Spliterator.SORTED | Spliterator.ORDERED;
		}

		@Override
		public final Comparator<? super K> getComparator() {
			return tree.comparator;
		}

	}

	static final class DescendingKeySpliterator<K, V> extends BinaryTreeSpliterator<K, V> implements Spliterator<K> {
		DescendingKeySpliterator(BinaryTree<K, V> tree, BinaryTree.Entry<K, V> origin, BinaryTree.Entry<K, V> fence, int side, int est,
				int expectedModCount) {
			super(tree, origin, fence, side, est, expectedModCount);
		}

		@Override
		public DescendingKeySpliterator<K, V> trySplit() {
			if (est < 0) getEstimate(); // force initialization
			int d = side;
			BinaryTree.Entry<K, V> e = current, f = fence, s = ((e == null || e == f) ? null : // empty
					(d == 0) ? tree.root : // was top
							(d < 0) ? e.left : // was left
									(d > 0 && f != null) ? f.right : // was right
											null);
			if (s != null && s != e && s != f && tree.compare(e.key, s.key) > 0) { // e not already past s
				side = 1;
				return new DescendingKeySpliterator<>(tree, e, current = s, -1, est >>>= 1, expectedModCount);
			}
			return null;
		}

		@Override
		public void forEachRemaining(Consumer<? super K> action) {
			if (action == null) throw new NullPointerException();
			if (est < 0) getEstimate(); // force initialization
			BinaryTree.Entry<K, V> f = fence, e, p, pr;
			if ((e = current) != null && e != f) {
				current = f; // exhaust
				do {
					action.accept(e.key);
					if ((p = e.left) != null) {
						while ((pr = p.right) != null)
							p = pr;
					} else {
						while ((p = e.parent) != null && e == p.left)
							e = p;
					}
				} while ((e = p) != null && e != f);
				if (tree.modCount != expectedModCount) throw new ConcurrentModificationException();
			}
		}

		@Override
		public boolean tryAdvance(Consumer<? super K> action) {
			BinaryTree.Entry<K, V> e;
			if (action == null) throw new NullPointerException();
			if (est < 0) getEstimate(); // force initialization
			if ((e = current) == null || e == fence) return false;
			current = predecessor(e);
			action.accept(e.key);
			if (tree.modCount != expectedModCount) throw new ConcurrentModificationException();
			return true;
		}

		@Override
		public int characteristics() {
			return (side == 0 ? Spliterator.SIZED : 0) | Spliterator.DISTINCT | Spliterator.ORDERED;
		}
	}

	static final class ValueSpliterator<K, V> extends BinaryTreeSpliterator<K, V> implements Spliterator<V> {
		ValueSpliterator(BinaryTree<K, V> tree, BinaryTree.Entry<K, V> origin, BinaryTree.Entry<K, V> fence, int side, int est, int expectedModCount) {
			super(tree, origin, fence, side, est, expectedModCount);
		}

		@Override
		public ValueSpliterator<K, V> trySplit() {
			if (est < 0) getEstimate(); // force initialization
			int d = side;
			BinaryTree.Entry<K, V> e = current, f = fence, s = ((e == null || e == f) ? null : // empty
					(d == 0) ? tree.root : // was top
							(d > 0) ? e.right : // was right
									(d < 0 && f != null) ? f.left : // was left
											null);
			if (s != null && s != e && s != f && tree.compare(e.key, s.key) < 0) { // e not already past s
				side = 1;
				return new ValueSpliterator<>(tree, e, current = s, -1, est >>>= 1, expectedModCount);
			}
			return null;
		}

		@Override
		public void forEachRemaining(Consumer<? super V> action) {
			if (action == null) throw new NullPointerException();
			if (est < 0) getEstimate(); // force initialization
			BinaryTree.Entry<K, V> f = fence, e, p, pl;
			if ((e = current) != null && e != f) {
				current = f; // exhaust
				do {
					action.accept(e.value);
					if ((p = e.right) != null) {
						while ((pl = p.left) != null)
							p = pl;
					} else {
						while ((p = e.parent) != null && e == p.right)
							e = p;
					}
				} while ((e = p) != null && e != f);
				if (tree.modCount != expectedModCount) throw new ConcurrentModificationException();
			}
		}

		@Override
		public boolean tryAdvance(Consumer<? super V> action) {
			BinaryTree.Entry<K, V> e;
			if (action == null) throw new NullPointerException();
			if (est < 0) getEstimate(); // force initialization
			if ((e = current) == null || e == fence) return false;
			current = successor(e);
			action.accept(e.value);
			if (tree.modCount != expectedModCount) throw new ConcurrentModificationException();
			return true;
		}

		@Override
		public int characteristics() {
			return (side == 0 ? Spliterator.SIZED : 0) | Spliterator.ORDERED;
		}
	}

	static final class EntrySpliterator<K, V> extends BinaryTreeSpliterator<K, V> implements Spliterator<Map.Entry<K, V>> {
		EntrySpliterator(BinaryTree<K, V> tree, BinaryTree.Entry<K, V> origin, BinaryTree.Entry<K, V> fence, int side, int est, int expectedModCount) {
			super(tree, origin, fence, side, est, expectedModCount);
		}

		@Override
		public EntrySpliterator<K, V> trySplit() {
			if (est < 0) getEstimate(); // force initialization
			int d = side;
			BinaryTree.Entry<K, V> e = current, f = fence, s = ((e == null || e == f) ? null : // empty
					(d == 0) ? tree.root : // was top
							(d > 0) ? e.right : // was right
									(d < 0 && f != null) ? f.left : // was left
											null);
			if (s != null && s != e && s != f && tree.compare(e.key, s.key) < 0) { // e not already past s
				side = 1;
				return new EntrySpliterator<>(tree, e, current = s, -1, est >>>= 1, expectedModCount);
			}
			return null;
		}

		@Override
		public void forEachRemaining(Consumer<? super Map.Entry<K, V>> action) {
			if (action == null) throw new NullPointerException();
			if (est < 0) getEstimate(); // force initialization
			BinaryTree.Entry<K, V> f = fence, e, p, pl;
			if ((e = current) != null && e != f) {
				current = f; // exhaust
				do {
					action.accept(e);
					if ((p = e.right) != null) {
						while ((pl = p.left) != null)
							p = pl;
					} else {
						while ((p = e.parent) != null && e == p.right)
							e = p;
					}
				} while ((e = p) != null && e != f);
				if (tree.modCount != expectedModCount) throw new ConcurrentModificationException();
			}
		}

		@Override
		public boolean tryAdvance(Consumer<? super Map.Entry<K, V>> action) {
			BinaryTree.Entry<K, V> e;
			if (action == null) throw new NullPointerException();
			if (est < 0) getEstimate(); // force initialization
			if ((e = current) == null || e == fence) return false;
			current = successor(e);
			action.accept(e);
			if (tree.modCount != expectedModCount) throw new ConcurrentModificationException();
			return true;
		}

		@Override
		public int characteristics() {
			return (side == 0 ? Spliterator.SIZED : 0) | Spliterator.DISTINCT | Spliterator.SORTED | Spliterator.ORDERED;
		}

		@Override
		public Comparator<Map.Entry<K, V>> getComparator() {
			// Adapt or create a key-based comparator
			if (tree.comparator != null) {
				return Map.Entry.comparingByKey(tree.comparator);
			} else {
				return (Comparator<Map.Entry<K, V>> & Serializable) (e1, e2) -> {
					@SuppressWarnings("unchecked")
					Comparable<? super K> k1 = (Comparable<? super K>) e1.getKey();
					return k1.compareTo(e2.getKey());
				};
			}
		}
	}
}
