package com.mystic.guilds.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.mutable.MutableBoolean;


/**
 * A Cache class that allows quick access to stored objects
 * @param <Key>
 * @param <Value>
 */
public class Cache <Key, Value> {
	public static final String version = "1.0.1";
	static final boolean DEBUG = true;

	/**
	 * ease of use cache to allows subclasses to define themselves dirty
	 * @param <K>
	 * @param <V>
	 */
	public static abstract class CacheObject<K,V> implements UniqueKey<K>{
		Cache<K, V> cache;
		public void setCache(Cache<K,V> cache){this.cache = cache;}
		@SuppressWarnings("unchecked")
		protected void setDirty(){if (cache != null) cache.setDirty(getKey());}
	}

	/**
	 * All cache classes must be able to return a Unique Key, (not necessarily just an int ala hashCode() ) 
	 * @param <K>
	 */
	public interface UniqueKey<K> {
		public K getKey();
	}

	/**
	 * Interface for the class that will Save and Load the objects in the cache 
	 *
	 * @param <K> Key Object
	 * @param <T> Value Object
	 */
	public interface CacheSerializer<K,T>{
		public T load(K key, MutableBoolean dirty, Object... varArgs);
		public void save(List<T> types);
	}

	/**
	 * A cache element along with when it was used 
	 */
	public class CacheElement{
		public UniqueKey<Key> v;
		public Long lastUsed;
		public CacheElement(UniqueKey<Key> value){
			this.v = value;
		}
		public void setUsed(){
			this.lastUsed = System.currentTimeMillis();
		}
	}
	CacheSerializer<Key,UniqueKey<Key>> serializer; /// Our serializer for the cache data
	Map<Key,CacheElement> map = new HashMap<Key,CacheElement>(); /// a mapping of the key to the cache objects
	Set<Key> dirty = new HashSet<Key>(); /// which keys have been modified
	Boolean autoFlush = false;
	Long autoFlushTime = null;
	Long lastCheckedTime = System.currentTimeMillis();

	/**
	 * Create a new cache with the object that will save/load
	 * @param serializer
	 */
	public Cache(CacheSerializer<Key,Value> serializer){
		this.setSerializer(serializer);
	}

	/**
	 * Specify that this cache should try to save and remove old records every specified time
	 * This is a "loose" time, and will only be checked on a get or setDirty call
	 * @param time
	 */
	public void setSaveEvery(long time){
		autoFlush = true;
		autoFlushTime = time;
	}

	@SuppressWarnings("unchecked")
	/**
	 * get a key.  if varArgs is not null these values will be passed to the serializer in the case that the 
	 * cache object does not exist
	 * @param key cache key
	 * @param varArgs arguments that will be passed to serializer when a key is not found
	 * @return
	 */
	public Value get(Key key, Object... varArgs) {
		if (DEBUG) System.out.println(" - getting key = " + key + " contains=" + map.containsKey(key));
		CacheElement o = map.get(key);
		//		UniqueKey<Key> t = map.get(key);
		if (o==null){
			MutableBoolean isdirty = new MutableBoolean(false);
			UniqueKey<Key> t = serializer.load(key, isdirty, varArgs);
			if (DEBUG) System.out.println("  - loaded element  = " + t + " ");
			if (t==null)
				return null;
			o = new CacheElement(t);
			key = t.getKey();
			synchronized(map){
				map.put(key, o);	
			}
			if (DEBUG) System.out.println("  - adding key = " + key + " contains=" + map.containsKey(key) +"  dirty="+dirty);
			if (isdirty.booleanValue()){ /// If its dirty, add to our dirty set
				synchronized(dirty){
					dirty.add(key);
				}
			}
		}
		o.setUsed();
		if (autoFlush && autoFlushTime != null){
			flushOld(autoFlushTime);}
		return (Value) o.v;
	}

	/**
	 * get a cache object using the key from the given param
	 * @param type
	 * @return
	 */
	public Value get(UniqueKey<Key> type){
		return get(type.getKey());
	}

	/**
	 * get a cache object using the key from the given param.  
	 * @param type
	 * @param varArgs
	 * @return
	 */
	public Value get(UniqueKey<Key> type,Object... varArgs) {
		return get(type.getKey(),varArgs);
	}

	/**
	 * remove a cache object using the type given
	 * @param type
	 * @return
	 */
	public Value remove(UniqueKey<Key> type){
		return remove(type.getKey());
	}

	@SuppressWarnings("unchecked")
	/**
	 * remove a cache object using the key from the given param
	 * @param key
	 * @return
	 */
	public Value remove(Key key){
		if (DEBUG) System.out.println(" - remove key = " + key + " contains=" + map.containsKey(key));
		CacheElement o = map.remove(key);
		if (o != null){
			if (dirty.contains(key)){
				List<UniqueKey<Key>> types = new ArrayList<UniqueKey<Key>>(1);
				types.add(o.v);
				serializer.save(types);
			}
			return (Value) o.v;
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public Collection<Value> values() {
		return (Collection<Value>) map.values();
	}

	/**
	 * Specify that a cache object is 'dirty' and needs to be saved to db
	 * @param key
	 */
	public void setDirty(Key... keys) {
		synchronized(dirty){
			for (Key key : keys){
				if (DEBUG) System.out.println(" - setting dirty key = " + key + " v=" + map.get(key));
				dirty.add(key);
			}
		}			
		if (autoFlush && autoFlushTime != null){
			flushOld(autoFlushTime);}

	}
	/**
	 * Specify that a list of cache objects are 'dirty' and need to be saved to db
	 * @param types
	 */
	public void setDirty(UniqueKey<Key> ... types) {
		synchronized(dirty){
			for (UniqueKey<Key> t: types){
				if (map.containsKey(t.getKey()))
					dirty.add(t.getKey());
			}			
		}
	}

	public void setClean(UniqueKey<Key> ... types) {
		synchronized(dirty){
			for (UniqueKey<Key> t: types){
				dirty.remove(t.getKey());
			}			
		}
	}


	/**
	 * Explicitly save
	 * @param element
	 */
	public void save(UniqueKey<Key> element) {
		List<UniqueKey<Key>> types = new ArrayList<UniqueKey<Key>>(1);
		types.add(element);
		serializer.save(types);
	}

	/**
	 * Save all dirty cache records
	 */
	public void save() {
		List<UniqueKey<Key>> types = new ArrayList<UniqueKey<Key>>(dirty.size());
		synchronized(dirty){ synchronized(map){
			dirty.remove(null);
			for (Key key: dirty){
				if (DEBUG) System.out.println(" - saving key = " + key + " v=" + map.get(key));
				types.add(map.get(key).v);
			}			
			dirty.clear();
		}}
		serializer.save(types);
	}

	@SuppressWarnings("unchecked")
	/**
	 * Specify the class to be used to serialize the data
	 * @param cachable
	 */
	public void setSerializer(CacheSerializer<Key, Value> serializer){
		this.serializer = (CacheSerializer<Key, UniqueKey<Key>>) serializer;
	}

	/**
	 * write out all dirty records.  and empty the cache
	 */
	public void flush() {
		save();
		synchronized(dirty){ synchronized(map){
			map.clear(); 
			dirty.clear();
		}}
	}

	/**
	 * write out all records ands clear any records older than the specified time (in milliseconds)
	 */
	public void flushOld(Long time) {
		final long now = System.currentTimeMillis();
		if (now - lastCheckedTime < time)
			return;
		lastCheckedTime = now;
		save();
		List<Key> old = new ArrayList<Key>();
		synchronized(dirty){ synchronized(map){
			for (CacheElement e: map.values()){
				if (now - e.lastUsed > time){
					if (DEBUG) System.out.println("  - flushing old cache element =" + e.v +"  lastUsed=" + (now - e.lastUsed));
					old.add(e.v.getKey());
				}
			}
			dirty.clear();
			for (Key k: old){
				map.remove(k);
			}
		}}
	}
}
