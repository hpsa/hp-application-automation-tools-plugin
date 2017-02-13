// (C) Copyright 2003-2015 Hewlett-Packard Development Company, L.P.

package com.hp.octane.plugins.jenkins.configuration;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Action;
import jenkins.model.TransientActionFactory;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;

@Extension
public class ConfigurationActionFactory extends TransientActionFactory<AbstractProject> {

    @Override
    public Class<AbstractProject> type() {
        return AbstractProject.class;
    }

    @Nonnull
    @Override
    public Collection<? extends Action> createFor(@Nonnull AbstractProject project) {
        // not sure if we need proper extensibility mechanism here: let's start small and extend if needed
        if ("hudson.matrix.MatrixConfiguration".equals(project.getClass().getName())) {
            return Collections.emptyList();
        }
        return Collections.singleton(new ConfigurationAction(project));
    }
}
