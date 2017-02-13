// (C) Copyright 2003-2015 Hewlett-Packard Development Company, L.P.

package com.hp.octane.plugins.jenkins.client;

import com.hp.octane.plugins.jenkins.events.EventsClient;
import com.hp.octane.plugins.jenkins.events.EventsService;
import hudson.Extension;

@Extension
public class JenkinsInsightEventPublisher implements EventPublisher {

    @Override
    public boolean isSuspended() {
        EventsClient client = EventsService.getExtensionInstance().getClient();
        return client == null || client.isSuspended();
    }

    @Override
    public void resume() {
        EventsService.getExtensionInstance().wakeUpClient();
    }
}