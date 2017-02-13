package com.hp.octane.plugins.jenkins.tests;

import com.hp.octane.plugins.jenkins.ExtensionUtil;
import com.hp.octane.plugins.jenkins.ResultQueue;
import com.hp.octane.plugins.jenkins.tests.junit.TestResultStatus;
import hudson.matrix.*;
import hudson.maven.MavenModuleSet;
import hudson.model.AbstractBuild;
import hudson.model.FreeStyleProject;
import hudson.tasks.Maven;
import hudson.tasks.junit.JUnitResultArchiver;
import org.junit.*;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.ToolInstallations;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class JUnitResultsTest {

	private static Set<String> helloWorld2Tests = new HashSet<>();

	static {
		helloWorld2Tests.add(TestUtils.testSignature("helloWorld2", "hello", "HelloWorld2Test", "testOnce", TestResultStatus.PASSED));
		helloWorld2Tests.add(TestUtils.testSignature("helloWorld2", "hello", "HelloWorld2Test", "testDoce", TestResultStatus.PASSED));
	}

	private static Set<String> subFolderHelloWorldTests = new HashSet<>();

	static {
		subFolderHelloWorldTests.add(TestUtils.testSignature("subFolder/helloWorld", "hello", "HelloWorldTest", "testOne", TestResultStatus.PASSED));
		subFolderHelloWorldTests.add(TestUtils.testSignature("subFolder/helloWorld", "hello", "HelloWorldTest", "testTwo", TestResultStatus.FAILED));
		subFolderHelloWorldTests.add(TestUtils.testSignature("subFolder/helloWorld", "hello", "HelloWorldTest", "testThree", TestResultStatus.SKIPPED));
	}

	@ClassRule
	public static final JenkinsRule rule = new JenkinsRule();
	private static String mavenName;

	private TestQueue queue;

	@BeforeClass
	public static void prepareClass() throws Exception {
		rule.jenkins.setNumExecutors(10);
		Maven.MavenInstallation mavenInstallation = ToolInstallations.configureMaven3();
		mavenName = mavenInstallation.getName();
	}

	@Before
	public void prepareTest() {
		TestListener testListener = ExtensionUtil.getInstance(rule, TestListener.class);
		queue = new TestQueue();
		testListener._setTestResultQueue(queue);
	}

	@Test
	public void testJUnitResults() throws Exception {
		String projectName = "root-job-" + UUID.randomUUID().toString();
		FreeStyleProject project = rule.createFreeStyleProject(projectName);

		project.getBuildersList().add(new Maven("-s settings.xml clean test", mavenName, null, null, "-Dmaven.test.failure.ignore=true"));
		project.getPublishersList().add(new JUnitResultArchiver("**/target/surefire-reports/*.xml"));
		project.setScm(new CopyResourceSCM("/helloWorldRoot"));
		AbstractBuild build = TestUtils.runAndCheckBuild(project);

		matchTests(build, projectName, TestUtils.helloWorldTests, helloWorld2Tests);
		Assert.assertEquals(Collections.singleton(projectName + "#1"), getQueuedItems());
	}

	@Test
	public void testJUnitResultsPom() throws Exception {
		String projectName = "root-job-" + UUID.randomUUID().toString();
		FreeStyleProject project = rule.createFreeStyleProject(projectName);

		project.getBuildersList().add(new Maven("-s subFolder/settings.xml clean test", mavenName, "subFolder/helloWorld/pom.xml", null, "-Dmaven.test.failure.ignore=true"));
		project.getPublishersList().add(new JUnitResultArchiver("**/target/surefire-reports/*.xml"));
		project.setScm(new CopyResourceSCM("/helloWorldRoot", "subFolder"));
		AbstractBuild build = TestUtils.runAndCheckBuild(project);

		matchTests(build, projectName, subFolderHelloWorldTests);
		Assert.assertEquals(Collections.singleton(projectName + "#1"), getQueuedItems());
	}

	@Test
	public void testJUnitResultsTwoPoms() throws Exception {
		String projectName = "root-job-" + UUID.randomUUID().toString();
		FreeStyleProject project = rule.createFreeStyleProject(projectName);

		project.getBuildersList().add(new Maven("-s settings.xml clean test", mavenName, "helloWorld/pom.xml", null, "-Dmaven.test.failure.ignore=true"));
		project.getBuildersList().add(new Maven("-s settings.xml clean test", mavenName, "helloWorld2/pom.xml", null, "-Dmaven.test.failure.ignore=true"));
		project.getPublishersList().add(new JUnitResultArchiver("**/target/surefire-reports/*.xml"));
		project.setScm(new CopyResourceSCM("/helloWorldRoot"));
		AbstractBuild build = TestUtils.runAndCheckBuild(project);

		matchTests(build, projectName, TestUtils.helloWorldTests, helloWorld2Tests);
		Assert.assertEquals(Collections.singleton(projectName + "#1"), getQueuedItems());
	}

	@Test
	public void testJUnitResultsLegacy() throws Exception {
		String projectName = "root-job-" + UUID.randomUUID().toString();
		MavenModuleSet project = rule.createProject(MavenModuleSet.class, projectName);
		project.runHeadless();

		project.setMaven(mavenName);
		project.setGoals("-s settings.xml clean test -Dmaven.test.failure.ignore=true");
		project.getPublishersList().add(new JUnitResultArchiver("**/target/surefire-reports/*.xml"));
		project.setScm(new CopyResourceSCM("/helloWorldRoot"));
		AbstractBuild build = TestUtils.runAndCheckBuild(project);

		matchTests(build, projectName, TestUtils.helloWorldTests, helloWorld2Tests);
		Assert.assertEquals(Collections.singleton(projectName + "#1"), getQueuedItems());
	}

	@Test
	public void testJUnitResultsLegacyWithoutJUnitArchiver() throws Exception {
		String projectName = "root-job-" + UUID.randomUUID().toString();
		MavenModuleSet project = rule.createProject(MavenModuleSet.class, projectName);
		project.runHeadless();

		project.setMaven(mavenName);
		project.setGoals("-s settings.xml clean test -Dmaven.test.failure.ignore=true");
		project.setScm(new CopyResourceSCM("/helloWorldRoot"));
		AbstractBuild build = TestUtils.runAndCheckBuild(project);

		matchTests(build, projectName, TestUtils.helloWorldTests, helloWorld2Tests);
		Assert.assertEquals(Collections.singleton(projectName + "#1"), getQueuedItems());
	}

	@Test
	public void testJUnitResultsLegacySubfolder() throws Exception {
		String projectName = "root-job-" + UUID.randomUUID().toString();
		MavenModuleSet project = rule.createProject(MavenModuleSet.class, projectName);
		project.runHeadless();

		project.setMaven(mavenName);
		project.setRootPOM("subFolder/helloWorld/pom.xml");
		project.setGoals("-s settings.xml clean test -Dmaven.test.failure.ignore=true");
		project.getPublishersList().add(new JUnitResultArchiver("**/target/surefire-reports/*.xml"));
		project.setScm(new CopyResourceSCM("/helloWorldRoot", "subFolder"));
		AbstractBuild build = TestUtils.runAndCheckBuild(project);

		matchTests(build, projectName, subFolderHelloWorldTests);
		Assert.assertEquals(Collections.singleton(projectName + "#1"), getQueuedItems());
	}

	@Test
	public void testJUnitResultsWorkspaceStripping() throws Exception {
		Set<String> uftTests = new HashSet<>();
		uftTests.add(TestUtils.testSignature("", "All-Tests", "<None>", "subfolder" + File.separator + "CalculatorPlusNextGen", TestResultStatus.FAILED));

		String projectName = "root-job-" + UUID.randomUUID().toString();
		FreeStyleProject project = rule.createFreeStyleProject(projectName);
		project.getPublishersList().add(new TestCustomJUnitArchiver("UFT_results.xml"));
		project.setScm(new CopyResourceSCM("/UFT"));
		AbstractBuild build = TestUtils.runAndCheckBuild(project);

		matchTests(build, projectName, uftTests);
		Assert.assertEquals(Collections.singleton(projectName + "#1"), getQueuedItems());
	}

	@Test
	public void testJUnitResultsFreeStyleModule() throws Exception {
		// this scenario simulates FreeStyle project with maven executed via shell (by not using Maven builder directly)
		String projectName = "root-job-" + UUID.randomUUID().toString();
		FreeStyleProject project = rule.createFreeStyleProject(projectName);

		project.getBuildersList().add(new Maven("-s settings.xml clean test", mavenName, null, null, "-Dmaven.test.failure.ignore=true"));
		project.getPublishersList().add(new JUnitResultArchiver("**/target/surefire-reports/*.xml"));
		project.setScm(new CopyResourceSCM("/helloWorldRoot"));
		AbstractBuild build = TestUtils.runAndCheckBuild(project);

		matchTests(build, projectName, TestUtils.helloWorldTests, helloWorld2Tests);
		Assert.assertEquals(Collections.singleton(projectName + "#1"), getQueuedItems());
	}

	@Test
	public void testJUnitResultsMatrixProject() throws Exception {
		String projectName = "root-job-" + UUID.randomUUID().toString();
		MatrixProject matrixProject = rule.createProject(MatrixProject.class, projectName);
		matrixProject.setAxes(new AxisList(new Axis("OS", "Linux", "Windows")));

		matrixProject.getBuildersList().add(new Maven("-s settings.xml clean test", mavenName, null, null, "-Dmaven.test.failure.ignore=true"));
		matrixProject.getPublishersList().add(new JUnitResultArchiver("**/target/surefire-reports/*.xml"));
		matrixProject.setScm(new CopyResourceSCM("/helloWorldRoot"));
		MatrixBuild build = (MatrixBuild) TestUtils.runAndCheckBuild(matrixProject);

		for (MatrixRun run : build.getExactRuns()) {
			matchTests(run, projectName, TestUtils.helloWorldTests, helloWorld2Tests);
		}
		Assert.assertEquals(new HashSet<>(Arrays.asList(projectName + "/OS=Windows#1", projectName + "/OS=Linux#1")), getQueuedItems());
		Assert.assertFalse(new File(build.getRootDir(), "mqmTests.xml").exists());
	}

	private Set<String> getQueuedItems() {
		Set<String> ret = new HashSet<>();
		ResultQueue.QueueItem item;
		while ((item = queue.peekFirst()) != null) {
			ret.add(item.getProjectName() + "#" + item.getBuildNumber());
			queue.remove();
		}
		return ret;
	}

	private void matchTests(AbstractBuild build, String projectName, Set<String>... expectedTests) throws FileNotFoundException {
		File mqmTestsXml = new File(build.getRootDir(), "mqmTests.xml");
		TestUtils.matchTests(new TestResultIterable(mqmTestsXml), projectName, build.getStartTimeInMillis(), expectedTests);
	}
}
