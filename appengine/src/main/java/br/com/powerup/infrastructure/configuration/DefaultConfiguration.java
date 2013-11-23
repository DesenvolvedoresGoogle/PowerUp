package br.com.powerup.infrastructure.configuration;

import java.util.Iterator;

import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.SystemConfiguration;

import com.google.appengine.api.memcache.MemcacheServiceFactory;

/**
 * @author fuechi
 * 
 */
public class DefaultConfiguration extends AbstractConfiguration implements
		Configuration {

	private CompositeConfiguration delegate;

	public DefaultConfiguration() {
		delegate = new CompositeConfiguration();
		delegate.addConfiguration(new SystemConfiguration());
		delegate.addConfiguration(new MemcacheConfiguration(
				new DatastoreConfiguration(), MemcacheServiceFactory
						.getMemcacheService(getClass().getName())));
	}

	@Override
	public boolean isEmpty() {
		return delegate.isEmpty();
	}

	@Override
	public boolean containsKey(String key) {
		return delegate.containsKey(key);
	}

	@Override
	public Object getProperty(String key) {
		return delegate.getProperty(key);
	}

	@Override
	public Iterator<String> getKeys() {
		return delegate.getKeys();
	}

	@Override
	protected void addPropertyDirect(String key, Object value) {
		delegate.addProperty(key, value);
	}

}