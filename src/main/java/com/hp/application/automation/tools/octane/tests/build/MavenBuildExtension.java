/*
 *     Copyright 2017 Hewlett-Packard Development Company, L.P.
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package com.hp.application.automation.tools.octane.tests.build;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.Run;

@Extension
public class MavenBuildExtension extends BuildHandlerExtension {

	@Override
	public boolean supports(Run<?, ?> build) {
		return "hudson.maven.MavenBuild".equals(build.getClass().getName()) ||
				"hudson.maven.MavenModuleSetBuild".equals(build.getClass().getName());
	}

	@Override
	public BuildDescriptor getBuildType(Run<?, ?> build) {
		return new BuildDescriptor(
				((AbstractBuild)build).getRootBuild().getProject().getName(),
				((AbstractBuild)build).getProject().getName(),
				String.valueOf(build.getNumber()),
				String.valueOf(build.getNumber()),
				"");
	}

	@Override
	public String getProjectFullName(Run<?, ?> build) {
		if ("hudson.maven.MavenBuild".equals(build.getClass().getName())) {
			// we don't push individual maven module results (although we create the file)
			return null;
		} else {
			return ((AbstractBuild)build).getProject().getName();
		}
	}
}
