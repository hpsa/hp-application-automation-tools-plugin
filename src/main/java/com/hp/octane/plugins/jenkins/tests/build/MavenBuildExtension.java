// (C) Copyright 2003-2015 Hewlett-Packard Development Company, L.P.

package com.hp.octane.plugins.jenkins.tests.build;

import hudson.Extension;
import hudson.model.AbstractBuild;

@Extension
public class MavenBuildExtension extends BuildHandlerExtension {

	@Override
	public boolean supports(AbstractBuild<?, ?> build) {
		return "hudson.maven.MavenBuild".equals(build.getClass().getName()) ||
				"hudson.maven.MavenModuleSetBuild".equals(build.getClass().getName());
	}

	@Override
	public BuildDescriptor getBuildType(AbstractBuild<?, ?> build) {
		return new BuildDescriptor(
				build.getRootBuild().getProject().getName(),
				build.getRootBuild().getProject().getName(),
				String.valueOf(build.getNumber()),
				String.valueOf(build.getNumber()),
				"");
	}

	@Override
	public String getProjectFullName(AbstractBuild<?, ?> build) {
		if ("hudson.maven.MavenBuild".equals(build.getClass().getName())) {
			// we don't push individual maven module results (although we create the file)
			return null;
		} else {
			return build.getProject().getName();
		}
	}
}
