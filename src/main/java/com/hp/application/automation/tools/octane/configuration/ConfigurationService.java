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

package com.hp.application.automation.tools.octane.configuration;

import com.hp.application.automation.tools.model.OctaneServerSettingsModel;
import com.hp.application.automation.tools.settings.OctaneServerSettingsBuilder;
import hudson.Extension;
import hudson.Plugin;
import hudson.model.Hudson;
import jenkins.model.Jenkins;

@Extension
public class ConfigurationService {

	public static OctaneServerSettingsModel getModel() {
		return getOctaneDescriptor().getModel();
	}

	public static ServerConfiguration getServerConfiguration() {
		return getOctaneDescriptor().getServerConfiguration();
	}

	public static  void configurePlugin(OctaneServerSettingsModel newModel){
		getOctaneDescriptor().setModel(newModel);
	}

	private static OctaneServerSettingsBuilder.OctaneDescriptorImpl getOctaneDescriptor(){
		return  Hudson.getInstance().getDescriptorByType(OctaneServerSettingsBuilder.OctaneDescriptorImpl.class);
	}

	public static String getPluginVersion(){
		Plugin plugin = Jenkins.getInstance().getPlugin("hp-application-automation-tools-plugin");
		return plugin.getWrapper().getVersion();
	}

}
