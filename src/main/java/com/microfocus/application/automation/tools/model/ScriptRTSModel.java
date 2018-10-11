/*
 *
 *  Certain versions of software and/or documents (“Material”) accessible here may contain branding from
 *  Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.  As of September 1, 2017,
 *  the Material is now offered by Micro Focus, a separately owned and operated company.  Any reference to the HP
 *  and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE
 *  marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * © Copyright 2012-2018 Micro Focus or one of its affiliates.
 *
 * The only warranties for products and services of Micro Focus and its affiliates
 * and licensors (“Micro Focus”) are set forth in the express warranty statements
 * accompanying such products and services. Nothing herein should be construed as
 * constituting an additional warranty. Micro Focus shall not be liable for technical
 * or editorial errors or omissions contained herein.
 * The information contained herein is subject to change without notice.
 * ___________________________________________________________________
 *
 */

package com.microfocus.application.automation.tools.model;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import org.kohsuke.stapler.DataBoundConstructor;
import org.apache.commons.lang.StringUtils;
import javax.annotation.Nonnull;
import java.util.List;
import java.util.Properties;

public class ScriptRTSModel extends AbstractDescribableImpl<ScriptRTSModel> {
    private String scriptName;
    private List<AdditionalAttributeModel> additionalAttributes;
    static int additionalAttributeCounter = 1;

    @DataBoundConstructor
    public ScriptRTSModel(String scriptName, List<AdditionalAttributeModel> additionalAttributes) {
        this.scriptName = scriptName;
        this.additionalAttributes = additionalAttributes;
    }

    public List<AdditionalAttributeModel> getAdditionalAttributes() {
        return additionalAttributes;
    }

    public String getScriptName() {
        return scriptName;
    }


    /**
     * Adds additional attributes to the props file containing:
     * script name, additional attribute name, value and description
     *
     * @param props
     * @param scriptName
     */
    void addAdditionalAttributesToPropsFile(Properties props, String scriptName)
    {
        for (AdditionalAttributeModel additionalAttribute: this.additionalAttributes) {
            if (!StringUtils.isEmpty(additionalAttribute.getName())) {
                props.put("AdditionalAttribute" + additionalAttributeCounter,
                    scriptName + ";"
                    + additionalAttribute.getName() + ";"
                    + additionalAttribute.getValue() + ";"
                    + additionalAttribute.getDescription()
                    );
                additionalAttributeCounter++;
            }
        }
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<ScriptRTSModel> {
        @Nonnull
        public String getDisplayName() {return "Script RTS Model";}
    }
}
