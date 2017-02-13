package com.hp.octane.plugins.jenkins.tests.detection;

import hudson.ExtensionList;
import hudson.ExtensionPoint;
import hudson.model.AbstractBuild;
import hudson.model.Hudson;

import java.io.IOException;

public abstract class ResultFieldsDetectionExtension implements ExtensionPoint {

    public abstract ResultFields detect(AbstractBuild build) throws IOException, InterruptedException;

    public static ExtensionList<ResultFieldsDetectionExtension> all() {
        return Hudson.getInstance().getExtensionList(ResultFieldsDetectionExtension.class);
    }
}
