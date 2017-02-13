package com.hp.octane.plugins.jenkins.tests.junit;

import com.google.inject.Inject;
import com.hp.octane.plugins.jenkins.actions.cucumber.CucumberTestResultsAction;
import com.hp.octane.plugins.jenkins.tests.HPRunnerType;
import com.hp.octane.plugins.jenkins.tests.MqmTestsExtension;
import com.hp.octane.plugins.jenkins.tests.TestResultContainer;
import com.hp.octane.plugins.jenkins.tests.detection.ResultFields;
import com.hp.octane.plugins.jenkins.tests.detection.ResultFieldsDetectionService;
import com.hp.octane.plugins.jenkins.tests.impl.ObjectStreamIterator;
import com.hp.octane.plugins.jenkins.tests.testResult.TestResult;
import hudson.Extension;
import hudson.FilePath;
import hudson.maven.MavenBuild;
import hudson.maven.MavenModule;
import hudson.maven.MavenModuleSetBuild;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.remoting.VirtualChannel;
import hudson.tasks.test.AbstractTestResultAction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jenkinsci.remoting.Role;
import org.jenkinsci.remoting.RoleChecker;

import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Extension
public class JUnitExtension extends MqmTestsExtension {
	private static Logger logger = LogManager.getLogger(JUnitExtension.class);

	public static final String STORM_RUNNER = "StormRunner";
	public static final String LOAD_RUNNER = "LoadRunner";

	private static final String JUNIT_RESULT_XML = "junitResult.xml"; // NON-NLS

	private static final String PREFORMANCE_REPORT = "PerformanceReport";
	private static final String TRANSACTION_SUMMARY = "TransactionSummary";
	@Inject
	ResultFieldsDetectionService resultFieldsDetectionService;

	public boolean supports(AbstractBuild<?, ?> build) throws IOException, InterruptedException {
		if (build.getAction(CucumberTestResultsAction.class) != null) {
			logger.debug("CucumberTestResultsAction found. Will not process JUnit results.");
			return false;
		} else if (build.getAction(AbstractTestResultAction.class) != null) {
			logger.debug("AbstractTestResultAction found, JUnit results expected");
			return true;
		} else {
			logger.debug("AbstractTestResultAction not found, no JUnit results expected");
			return false;
		}
	}

	@Override
	public TestResultContainer getTestResults(AbstractBuild<?, ?> build, HPRunnerType hpRunnerType, String jenkinsRootUrl) throws IOException, InterruptedException {
		logger.debug("Collecting JUnit results");

		boolean isLoadRunnerProject = isLoadRunnerProject(build);
		FilePath resultFile = new FilePath(build.getRootDir()).child(JUNIT_RESULT_XML);
		if (resultFile.exists()) {
			logger.debug("JUnit result report found");
			ResultFields detectedFields = null;
			if (hpRunnerType.equals(HPRunnerType.StormRunner)) {
				detectedFields = new ResultFields(null, STORM_RUNNER, null);
			} else if (isLoadRunnerProject) {
				detectedFields = new ResultFields(null, LOAD_RUNNER, null);
			} else {
				detectedFields = resultFieldsDetectionService.getDetectedFields(build);
			}
			FilePath filePath = build.getWorkspace().act(new GetJUnitTestResults(build, Arrays.asList(resultFile), shallStripPackageAndClass(detectedFields), hpRunnerType, jenkinsRootUrl));
			return new TestResultContainer(new ObjectStreamIterator<TestResult>(filePath, true), detectedFields);
		} else {
			//avoid java.lang.NoClassDefFoundError when maven plugin is not present
			if ("hudson.maven.MavenModuleSetBuild".equals(build.getClass().getName())) {
				logger.debug("MavenModuleSetBuild detected, looking for results in maven modules");

				List<FilePath> resultFiles = new LinkedList<FilePath>();
				Map<MavenModule, MavenBuild> moduleLastBuilds = ((MavenModuleSetBuild) build).getModuleLastBuilds();
				for (MavenBuild mavenBuild : moduleLastBuilds.values()) {
					AbstractTestResultAction action = mavenBuild.getAction(AbstractTestResultAction.class);
					if (action != null) {
						FilePath moduleResultFile = new FilePath(mavenBuild.getRootDir()).child(JUNIT_RESULT_XML);
						if (moduleResultFile.exists()) {
							logger.debug("Found results in " + mavenBuild.getFullDisplayName());
							resultFiles.add(moduleResultFile);
						}
					}
				}
				if (!resultFiles.isEmpty()) {
					ResultFields detectedFields = null;
					if (hpRunnerType.equals(HPRunnerType.StormRunner)) {
						detectedFields = new ResultFields(null, STORM_RUNNER, null);
					} else if (isLoadRunnerProject) {
						detectedFields = new ResultFields(null, LOAD_RUNNER, null);
					} else {
						detectedFields = resultFieldsDetectionService.getDetectedFields(build);
					}
					FilePath filePath = build.getWorkspace().act(new GetJUnitTestResults(build, resultFiles, shallStripPackageAndClass(detectedFields), hpRunnerType, jenkinsRootUrl));
					return new TestResultContainer(new ObjectStreamIterator<TestResult>(filePath, true), detectedFields);
				}
			}
			logger.debug("No JUnit result report found");
			return null;
		}
	}

	private boolean shallStripPackageAndClass(ResultFields resultFields) {
		if (resultFields == null) {
			return false;
		}
		return resultFields.equals(new ResultFields("UFT", "UFT", null));
	}

	private boolean isLoadRunnerProject(AbstractBuild<?, ?> build) throws IOException, InterruptedException {
		FilePath preformanceReportFolder = new FilePath(build.getRootDir()).child(PREFORMANCE_REPORT);
		FilePath transactionSummaryFolder = new FilePath(build.getRootDir()).child(TRANSACTION_SUMMARY);
		if ((preformanceReportFolder.exists() && preformanceReportFolder.isDirectory()) && (transactionSummaryFolder.exists() && transactionSummaryFolder.isDirectory())) {
			return true;
		}
		return false;
	}

	private static class GetJUnitTestResults implements FilePath.FileCallable<FilePath> {

		private final List<FilePath> reports;
		private final String jobName;
		private final String buildId;
		private final String jenkinsRootUrl;
		private final HPRunnerType hpRunnerType;
		private boolean isUFTProject = false;
		private FilePath filePath;
		private List<ModuleDetection> moduleDetection;
		private long buildStarted;
		private FilePath workspace;
		private boolean stripPackageAndClass;

		public GetJUnitTestResults(AbstractBuild<?, ?> build, List<FilePath> reports, boolean stripPackageAndClass, HPRunnerType hpRunnerType, String jenkinsRootUrl) throws IOException, InterruptedException {
			this.reports = reports;
			this.filePath = new FilePath(build.getRootDir()).createTempFile(getClass().getSimpleName(), null);
			this.buildStarted = build.getStartTimeInMillis();
			this.workspace = build.getWorkspace();
			this.stripPackageAndClass = stripPackageAndClass;
			this.hpRunnerType = hpRunnerType;
			this.jenkinsRootUrl = jenkinsRootUrl;
			AbstractProject project = build.getProject();
			this.jobName = project.getName();
			this.buildId = build.getProject().getBuilds().getLastBuild().getId();
			moduleDetection = Arrays.asList(
					new MavenBuilderModuleDetection(build),
					new MavenSetModuleDetection(build),
					new ModuleDetection.Default());
		}

		@Override
		public FilePath invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
			OutputStream os = filePath.write();
			BufferedOutputStream bos = new BufferedOutputStream(os);
			ObjectOutputStream oos = new ObjectOutputStream(bos);

			try {
				for (FilePath report : reports) {
					JUnitXmlIterator iterator = new JUnitXmlIterator(report.read(), moduleDetection, workspace, jobName, buildId, buildStarted, stripPackageAndClass, hpRunnerType, jenkinsRootUrl);
					while (iterator.hasNext()) {
						oos.writeObject(iterator.next());
					}
				}
			} catch (XMLStreamException e) {
				throw new IOException(e);
			}
			os.flush();

			oos.close();
			return filePath;
		}

		@Override
		public void checkRoles(RoleChecker roleChecker) throws SecurityException {
			roleChecker.check(this, Role.UNKNOWN);
		}
	}

	/*
	 * To be used in tests only.
	 */
	public void _setResultFieldsDetectionService(ResultFieldsDetectionService detectionService) {
		this.resultFieldsDetectionService = detectionService;
	}
}
