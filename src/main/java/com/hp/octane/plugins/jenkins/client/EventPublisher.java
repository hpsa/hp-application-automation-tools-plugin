// (C) Copyright 2003-2015 Hewlett-Packard Development Company, L.P.

package com.hp.octane.plugins.jenkins.client;

public interface EventPublisher {

    boolean isSuspended();

    void resume();

}