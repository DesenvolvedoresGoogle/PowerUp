package br.com.powerup.infrastructure.configuration;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.memcache.MemcacheService;

public class MemcacheDatastoreConfiguration extends DatastoreConfiguration {

	private final MemcacheService memcacheService;
	private final static String DEFAULT_MEMCACHE_KEY_PREFIX = MemcacheConfiguration.class.getName();
	private final String memcacheKeyPrefix;

	public MemcacheDatastoreConfiguration(final DatastoreService datastoreService, final MemcacheService memcacheService, final String memcacheKeyPrefix) {
		super(datastoreService);
		this.memcacheService = memcacheService;
		this.memcacheKeyPrefix = memcacheKeyPrefix;
	}

	public MemcacheDatastoreConfiguration(final DatastoreService datastoreService, final MemcacheService memcacheService) {
		this(datastoreService, memcacheService, DEFAULT_MEMCACHE_KEY_PREFIX);
	}

	/**
	 * @param key
	 * @return
	 */
	private String cacheKey(final String key) {
		return memcacheKeyPrefix + key;
	}

	@Override
	public Object getProperty(final String key) {
		Object o = memcacheService.get(cacheKey(key));
		if ( o == null ) {
			o = super.getProperty(key);
			if ( o != null ) {
				memcacheService.put(cacheKey(key), o);
			}
		}
		return o;
	}
}