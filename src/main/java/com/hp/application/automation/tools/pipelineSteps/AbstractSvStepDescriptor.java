package com.hp.application.automation.tools.pipelineSteps;

import javax.annotation.Nonnull;

import com.hp.application.automation.tools.model.SvServerSettingsModel;
import com.hp.application.automation.tools.run.AbstractSvRunDescriptor;
import hudson.util.ListBoxModel;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.StepExecution;

public class AbstractSvStepDescriptor<T extends AbstractSvRunDescriptor> extends AbstractStepDescriptorImpl {

    final protected T builderDescriptor;
    final private String functionName;

    public AbstractSvStepDescriptor(Class<? extends StepExecution> executionType, String functionName, T builderDescriptor) {
        super(executionType);
        this.functionName = functionName;
        this.builderDescriptor = builderDescriptor;
    }

    @Override
    public String getFunctionName() {
        return functionName;
    }

    @Nonnull
    @Override
    public String getDisplayName() {
        return builderDescriptor.getDisplayName();
    }

    @Override
    public String getConfigPage() {
        return builderDescriptor.getConfigPage();
    }

    public SvServerSettingsModel[] getServers() {
        return builderDescriptor.getServers();
    }

    @SuppressWarnings("unused")
    public ListBoxModel doFillServerNameItems() {
        return builderDescriptor.doFillServerNameItems();
    }
}
