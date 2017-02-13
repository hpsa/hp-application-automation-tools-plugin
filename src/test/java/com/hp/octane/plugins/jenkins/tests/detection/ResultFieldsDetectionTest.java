package com.hp.octane.plugins.jenkins.tests.detection;

import com.hp.octane.plugins.jenkins.ExtensionUtil;
import com.hp.octane.plugins.jenkins.tests.CopyResourceSCM;
import com.hp.octane.plugins.jenkins.tests.TestUtils;
import com.hp.octane.plugins.jenkins.tests.junit.JUnitExtension;
import hudson.model.AbstractBuild;
import hudson.model.FreeStyleProject;
import hudson.tasks.Maven;
import hudson.tasks.junit.JUnitResultArchiver;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.ToolInstallations;
import org.mockito.Mockito;

import java.io.File;
import java.io.FileReader;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ResultFieldsDetectionTest {

	@Rule
	public final JenkinsRule rule = new JenkinsRule();

	private static FreeStyleProject project;

	private static ResultFieldsDetectionService detectionService;

	@Before
	public void setUp() throws Exception {
		project = rule.createFreeStyleProject("junit - job");
		JUnitExtension junitExtension = ExtensionUtil.getInstance(rule, JUnitExtension.class);
		detectionService = Mockito.mock(ResultFieldsDetectionService.class);
		junitExtension._setResultFieldsDetectionService(detectionService);

		Maven.MavenInstallation mavenInstallation = ToolInstallations.configureMaven3();

		project.getBuildersList().add(new Maven("-s settings.xml -U test", mavenInstallation.getName(), null, null, "-Dmaven.test.failure.ignore=true"));
		project.setScm(new CopyResourceSCM("/helloWorldRoot"));
	}

	@Test
	public void testDetectionNotRun() throws Exception {
		//there is no test publisher set up in this project, detection will not run
		AbstractBuild build = TestUtils.runAndCheckBuild(project);
		verify(detectionService, Mockito.never()).getDetectedFields(build);
	}

	@Test
	public void testDetectionRunOnce() throws Exception {
		project.getPublishersList().add(new JUnitResultArchiver("**/target/surefire-reports/*.xml"));
		AbstractBuild build = TestUtils.runAndCheckBuild(project);
		verify(detectionService, Mockito.times(1)).getDetectedFields(build);
	}

	@Test
	public void testDetectedFieldsInXml() throws Exception {
		when(detectionService.getDetectedFields(any(AbstractBuild.class))).thenReturn(new ResultFields("HOLA", "CIAO", "SALUT"));
		project.getPublishersList().add(new JUnitResultArchiver("**/target/surefire-reports/*.xml"));
		AbstractBuild build = TestUtils.runAndCheckBuild(project);

		File mqmTestsXml = new File(build.getRootDir(), "mqmTests.xml");
		ResultFieldsXmlReader xmlReader = new ResultFieldsXmlReader(new FileReader(mqmTestsXml));
		ResultFields resultFields = xmlReader.readXml().getResultFields();

		Assert.assertNotNull(resultFields);
		Assert.assertEquals("HOLA", resultFields.getFramework());
		Assert.assertEquals("CIAO", resultFields.getTestingTool());
		Assert.assertEquals("SALUT", resultFields.getTestLevel());
	}

	@Test
	public void testNoDetectedFieldsInXml() throws Exception {
		when(detectionService.getDetectedFields(any(AbstractBuild.class))).thenReturn(null);
		project.getPublishersList().add(new JUnitResultArchiver("**/target/surefire-reports/*.xml"));
		AbstractBuild build = TestUtils.runAndCheckBuild(project);

		File mqmTestsXml = new File(build.getRootDir(), "mqmTests.xml");
		ResultFieldsXmlReader xmlReader = new ResultFieldsXmlReader(new FileReader(mqmTestsXml));
		ResultFields resultFields = xmlReader.readXml().getResultFields();
		;

		Assert.assertNull(resultFields.getFramework());
		Assert.assertNull(resultFields.getTestingTool());
		Assert.assertNull(resultFields.getTestLevel());
	}

	@Test
	public void testEmptyDetectedFieldsInXml() throws Exception {
		when(detectionService.getDetectedFields(any(AbstractBuild.class))).thenReturn(new ResultFields(null, null, null));
		project.getPublishersList().add(new JUnitResultArchiver("**/target/surefire-reports/*.xml"));
		AbstractBuild build = TestUtils.runAndCheckBuild(project);

		File mqmTestsXml = new File(build.getRootDir(), "mqmTests.xml");
		ResultFieldsXmlReader xmlReader = new ResultFieldsXmlReader(new FileReader(mqmTestsXml));
		ResultFields resultFields = xmlReader.readXml().getResultFields();
		;

		Assert.assertNull(resultFields.getFramework());
		Assert.assertNull(resultFields.getTestingTool());
		Assert.assertNull(resultFields.getTestLevel());
	}

	/**
	 * We do not detect Junit yet.
	 */
	@Test
	public void testNotDetectableConfigurationInXml() throws Exception {
		project.getPublishersList().add(new JUnitResultArchiver("**/target/surefire-reports/*.xml"));
		AbstractBuild build = TestUtils.runAndCheckBuild(project);

		File mqmTestsXml = new File(build.getRootDir(), "mqmTests.xml");
		ResultFieldsXmlReader xmlReader = new ResultFieldsXmlReader(new FileReader(mqmTestsXml));
		ResultFields resultFields = xmlReader.readXml().getResultFields();

		Assert.assertNull(resultFields.getFramework());
		Assert.assertNull(resultFields.getTestingTool());
		Assert.assertNull(resultFields.getTestLevel());
	}
}
