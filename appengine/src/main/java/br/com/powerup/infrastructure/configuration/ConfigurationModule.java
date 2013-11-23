package br.com.powerup.infrastructure.configuration;

import com.google.inject.AbstractModule;

public class ConfigurationModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(Configuration.class).to(DefaultConfiguration.class);
	}

}
