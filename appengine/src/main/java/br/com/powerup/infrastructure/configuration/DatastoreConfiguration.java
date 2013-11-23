package br.com.powerup.infrastructure.configuration;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

import javax.inject.Inject;

import org.apache.commons.configuration.AbstractConfiguration;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entities;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Transaction;

public class DatastoreConfiguration extends AbstractConfiguration {

	public static final String DEFAULT_ENTITY_KIND = "Configuration";
	public static final String DEFAULT_PROPERTY_VALUE = "value";

	private final DatastoreService datastoreService;
	private final String entityKind;
	private final String propertyValue; 

	public DatastoreConfiguration() {
		this(DatastoreServiceFactory.getDatastoreService(), DEFAULT_ENTITY_KIND, DEFAULT_PROPERTY_VALUE);
	}
	
	@Inject
	public DatastoreConfiguration(final DatastoreService datastoreService) {
		this(datastoreService, DEFAULT_ENTITY_KIND, DEFAULT_PROPERTY_VALUE);
	}

	@Inject
	public DatastoreConfiguration(final DatastoreService datastoreService, final String entityKind, final String propertyValue) {
		super();
		this.datastoreService = datastoreService;
		this.entityKind = entityKind;
		this.propertyValue = propertyValue;
	}
	
	@Override
	public boolean containsKey(final String key) {
		try {
			datastoreService.get(newKey(key));
			return true;
		} catch (final EntityNotFoundException e) {
			return false;
		}
	}

	@Override
	public Iterator<String> getKeys() {
		final Iterable<Entity> iterable = datastoreService.prepare(new Query(entityKind).setKeysOnly()).asIterable();
		final List<String> list = new LinkedList<String>();
		for (final Entity entity : iterable) {
			list.add(entity.getKey().getName());
		}
		return list.iterator();
	}

	@Override
	public Object getProperty(final String key) {
		try {
			final Entity entity = getInsideTransaction(newKey(key));
			final Object value = entity.getProperty(propertyValue);
			return value;
		} catch (final EntityNotFoundException e) {
			if (isThrowExceptionOnMissing()) {
				throw new NoSuchElementException("key not found '" + key + "'");
			}
			return null;
		}
	}

	@Override
	public boolean isEmpty() {
		final Query q = new Query(Entities.KIND_METADATA_KIND);
		final Filter kinfFilter = new FilterPredicate(Entity.KEY_RESERVED_PROPERTY,
				FilterOperator.EQUAL, Entities.createKindKey(entityKind));
		q.setFilter(kinfFilter);
		return !datastoreService.prepare(q).asIterator().hasNext();
	}

	@Override
	protected void addPropertyDirect(final String key, final Object value) {
		final Entity entity = new Entity(newKey(key));
		entity.setProperty(propertyValue, value);
		putInsideTransaction(entity);
	}

	protected Key newKey(final String key) {
		return KeyFactory.createKey(entityKind, key);
	}
	
	private void putInsideTransaction(final Entity featureEntity) {
		final Transaction txn = datastoreService.beginTransaction();
		try {
			datastoreService.put(txn, featureEntity);
			txn.commit();
		} finally  {
			if(txn.isActive()) {
				txn.rollback();
			}
		}
	}

	private Entity getInsideTransaction(final Key key) throws EntityNotFoundException {
		final Transaction txn = datastoreService.beginTransaction();
		Entity featureEntity;
		try {
			featureEntity = datastoreService.get(txn, key);
			txn.commit();
		} finally  {
			if(txn.isActive()) {
				txn.rollback();
			}
		}
		return featureEntity;
	}

}