package com.hp.octane.plugins.jenkins.model.processors.builders;

import com.hp.octane.plugins.jenkins.model.ModelFactory;
import com.tikal.jenkins.plugins.multijob.MultiJobBuilder;
import com.tikal.jenkins.plugins.multijob.PhaseJobsConfig;
import hudson.model.AbstractProject;
import hudson.tasks.Builder;
import jenkins.model.Jenkins;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: gullery
 * Date: 08/01/15
 * Time: 23:02
 * To change this template use File | Settings | File Templates.
 */

public class MultiJobBuilderProcessor extends AbstractBuilderProcessor {
	private static final Logger logger = LogManager.getLogger(MultiJobBuilderProcessor.class);

	public MultiJobBuilderProcessor(Builder builder) {
		MultiJobBuilder b = (MultiJobBuilder) builder;
		super.phases = new ArrayList<>();
		List<AbstractProject> items = new ArrayList<>();
		AbstractProject tmpProject;
		for (PhaseJobsConfig config : b.getPhaseJobs()) {
			tmpProject = (AbstractProject) Jenkins.getInstance().getItem(config.getJobName());
			if (tmpProject != null) {
				items.add(tmpProject);
			} else {
				logger.warn("project named '" + config.getJobName() + "' not found; considering this as corrupted configuration and skipping the project");
			}
		}
		super.phases.add(ModelFactory.createStructurePhase(b.getPhaseName(), true, items));
	}
}
