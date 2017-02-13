package com.hp.octane.plugins.jenkins.bridge;

import com.google.inject.Inject;
import com.hp.octane.plugins.jenkins.client.JenkinsMqmRestClientFactory;
import com.hp.octane.plugins.jenkins.client.JenkinsMqmRestClientFactoryImpl;
import com.hp.octane.plugins.jenkins.configuration.ConfigurationListener;
import com.hp.octane.plugins.jenkins.configuration.ServerConfiguration;
import hudson.Extension;
import jenkins.model.Jenkins;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * Created by gullery on 05/08/2015.
 * <p>
 * Bridge Service meant to provide an abridged connectivity functionality
 * The only APIs to be exposed is the basic management of abridged clients
 */

@Extension
public class BridgesService implements ConfigurationListener {
	private static final Logger logger = LogManager.getLogger(BridgesService.class);

	private static BridgesService extensionInstance;
	private JenkinsMqmRestClientFactory clientFactory;
	private BridgeClient bridgeClient;

	public static BridgesService getExtensionInstance() {
		if (extensionInstance == null) {
			List<BridgesService> extensions = Jenkins.getInstance().getExtensionList(BridgesService.class);
			if (extensions.isEmpty()) {
				throw new RuntimeException("bridge service was not initialized properly");
			} else if (extensions.size() > 1) {
				throw new RuntimeException("bridge service expected to be singleton, found " + extensions.size() + " instances");
			} else {
				extensionInstance = extensions.get(0);
			}
		}
		return extensionInstance;
	}

	public void updateBridge(ServerConfiguration conf) {
		if (conf.isValid()) {
			if (bridgeClient != null) {
				bridgeClient.update(conf);
			} else {
				bridgeClient = new BridgeClient(conf, clientFactory);
			}
		} else {
			if (bridgeClient != null) {
				logger.info("empty / non-valid configuration submitted, disposing bridge client");
				bridgeClient.dispose();
				bridgeClient = null;
			}
		}
	}

	@Inject
	public void setMqmRestClientFactory(JenkinsMqmRestClientFactoryImpl clientFactory) {
		this.clientFactory = clientFactory;
	}

	@Override
	public void onChanged(ServerConfiguration conf, ServerConfiguration oldConf) {
		updateBridge(conf);
	}
}
