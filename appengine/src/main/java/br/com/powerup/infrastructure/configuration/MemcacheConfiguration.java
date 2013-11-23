package br.com.powerup.infrastructure.configuration;

import java.util.Iterator;

import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.configuration.Configuration;

import com.google.appengine.api.memcache.MemcacheService;

public class MemcacheConfiguration extends AbstractConfiguration {

	private Configuration delegate;
	private MemcacheService memcacheService;
	private final static String DEFAULT_MEMCACHE_KEY_PREFIX = MemcacheConfiguration.class.getName();
	private String memcacheKeyPrefix;

	public MemcacheConfiguration(final Configuration delegate, final MemcacheService memcacheService) {
		this(delegate, memcacheService, DEFAULT_MEMCACHE_KEY_PREFIX);
	}

	public MemcacheConfiguration(final Configuration delegate, final MemcacheService memcacheService, final String memcacheKeyPrefix) {
		super();
		this.delegate = delegate;
		this.memcacheService = memcacheService;
		this.memcacheKeyPrefix = memcacheKeyPrefix;
	}

	@Override
	public boolean isEmpty() {
		return delegate.isEmpty();
	}

	@Override
	public boolean containsKey(final String key) {
		if ( !memcacheService.contains(cacheKey(key)) ) {
			return delegate.containsKey(key);
		}
		return true;
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
		Object propertyValue = memcacheService.get(cacheKey(key));
		if ( propertyValue == null ) {
			propertyValue = delegate.getProperty(key);
			if ( propertyValue != null ) {
				memcacheService.put(cacheKey(key), propertyValue);
			}
		}
		return propertyValue;
	}

	@Override
	protected void addPropertyDirect(final String key, final Object value) {
		delegate.addProperty(key, value);
	}

	@Override
	public Iterator<String> getKeys() {
		return delegate.getKeys();
	}

}