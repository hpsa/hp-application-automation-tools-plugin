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

package com.hp.application.automation.tools.octane.executor;

import com.hp.application.automation.tools.octane.actions.UFTTestUtil;
import com.hp.application.automation.tools.octane.actions.UftTestType;
import com.hp.application.automation.tools.octane.actions.dto.AutomatedTest;
import com.hp.application.automation.tools.octane.actions.dto.ScmResourceFile;
import hudson.ExtensionList;
import hudson.FilePath;
import hudson.model.*;
import hudson.plugins.git.GitChangeSet;
import hudson.scm.ChangeLogSet;
import hudson.scm.EditType;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class UFTTestDetectionService {
    private static final Logger logger = LogManager.getLogger(UFTTestDetectionService.class);
    private static final String INITIAL_DETECTION_FILE = "INITIAL_DETECTION_FILE.txt";
    private static final String DETECTION_RESULT_FILE = "detection_result.xml";
    private static final String STFileExtention = ".st";//api test
    private static final String QTPFileExtention = ".tsp";//gui test
    private static final String XLSXExtention = ".xlsx";//excel file
    private static final String XLSExtention = ".xls";//excel file


    public static UFTTestDetectionResult startScanning(AbstractBuild<?, ?> build, String workspaceId, String scmRepositoryId, BuildListener buildListener) {
        ChangeLogSet<? extends ChangeLogSet.Entry> changeSet = build.getChangeSet();
        Object[] changeSetItems = changeSet.getItems();
        UFTTestDetectionResult result = null;

        try {

            boolean fullScan = build.getId().equals("1") || !initialDetectionFileExist(build.getWorkspace()) || isFullScan((build));
            if (fullScan) {
                printToConsole(buildListener, "Executing initial detection");
                result = doInitialDetection(build.getWorkspace());
            } else {
                printToConsole(buildListener, "Executing changeSet detection");
                result = doChangeSetDetection(changeSetItems, build.getWorkspace());
                removeTestDuplicated(result.getUpdatedTests());
            }

            if (!result.getNewTests().isEmpty()) {
                printToConsole(buildListener, String.format("Found %s new tests", result.getNewTests().size()));
            }
            if (!result.getUpdatedTests().isEmpty()) {
                printToConsole(buildListener, String.format("Found %s updated tests", result.getUpdatedTests().size()));
            }
            if (!result.getDeletedTests().isEmpty()) {
                printToConsole(buildListener, String.format("Found %s deleted tests", result.getDeletedTests().size()));
            }
            if (!result.getNewScmResourceFiles().isEmpty()) {
                printToConsole(buildListener, String.format("Found %s new data tables", result.getNewScmResourceFiles().size()));
            }
            if (!result.getDeletedScmResourceFiles().isEmpty()) {
                printToConsole(buildListener, String.format("Found %s deleted data tables", result.getDeletedScmResourceFiles().size()));
            }

            result.setScmRepositoryId(scmRepositoryId);
            result.setWorkspaceId(workspaceId);
            result.setFullScan(fullScan);
            sortTests(result.getNewTests());
            sortTests(result.getUpdatedTests());
            publishDetectionResults(build, buildListener, result);

            if (result.hasChanges()) {
                UftTestDiscoveryDispatcher dispatcher = getExtension(UftTestDiscoveryDispatcher.class);
                dispatcher.enqueueResult(build.getProject().getName(), build.getNumber());
            }
            createInitialDetectionFile(build.getWorkspace());

        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    private static boolean isFullScan(AbstractBuild<?, ?> build) {
        ParametersAction parameters = build.getAction(ParametersAction.class);
        if (parameters != null) {
            ParameterValue parameterValue = parameters.getParameter(TestExecutionJobCreatorService.FULL_SCAN_PARAMETER_NAME);
            if (parameterValue != null) {
                return (Boolean) parameterValue.getValue();
            }
        }
        return false;
    }

    private static void sortTests(List<AutomatedTest> newTests) {
        Collections.sort(newTests, new Comparator<AutomatedTest>() {
            @Override
            public int compare(AutomatedTest o1, AutomatedTest o2) {
                int comparePackage = o1.getPackage().compareTo(o2.getPackage());
                if (comparePackage == 0) {
                    return o1.getName().compareTo(o2.getName());
                } else {
                    return comparePackage;
                }
            }
        });
    }

    private static <T> T getExtension(Class<T> clazz) {
        ExtensionList<T> items = Jenkins.getInstance().getExtensionList(clazz);
        return items.get(0);
    }

    private static void removeTestDuplicated(List<AutomatedTest> tests) {
        Set<String> keys = new HashSet<>();
        List<AutomatedTest> testsToRemove = new ArrayList<>();
        for (AutomatedTest test : tests) {
            String key = test.getPackage() + "_" + test.getName();
            if (keys.contains(key)) {
                testsToRemove.add(test);
            }
            keys.add(key);

        }
        tests.removeAll(testsToRemove);
    }

    private static void printToConsole(BuildListener buildListener, String msg) {
        if (buildListener != null) {
            buildListener.getLogger().println("UFTTestDetectionService : " + msg);
        }
    }

    private static UFTTestDetectionResult doChangeSetDetection(Object[] changeSetItems, FilePath workspace) throws IOException, InterruptedException {
        UFTTestDetectionResult result = new UFTTestDetectionResult();
        if (changeSetItems.length == 0) {
            return result;
        }

        boolean isGitChanges = changeSetItems[0] instanceof GitChangeSet;
        if (!isGitChanges) {
            return result;
        }

        for (int i = 0; i < changeSetItems.length; i++) {
            GitChangeSet changeSet = (GitChangeSet) changeSetItems[i];
            for (GitChangeSet.Path path : changeSet.getPaths()) {
                String fileFullPath = workspace + File.separator + path.getPath();
                if (isTestMainFilePath(path.getPath())) {

                    if (EditType.ADD.equals(path.getEditType())) {
                        if (isFileExist(fileFullPath)) {
                            FilePath testFolder = getTestFolderForTestMainFile(fileFullPath);
                            scanFileSystemRecursively(workspace, testFolder, result.getNewTests(), result.getNewScmResourceFiles());
                        }
                    } else if (EditType.DELETE.equals(path.getEditType())) {
                        if (!isFileExist(fileFullPath)) {
                            FilePath testFolder = getTestFolderForTestMainFile(fileFullPath);
                            AutomatedTest test = createAutomatedTest(workspace, testFolder, null);
                            result.getDeletedTests().add(test);
                        }
                    } else if (EditType.EDIT.equals(path.getEditType())) {
                        if (isFileExist(fileFullPath)) {
                            FilePath testFolder = getTestFolderForTestMainFile(fileFullPath);
                            scanFileSystemRecursively(workspace, testFolder, result.getUpdatedTests(), result.getUpdatedScmResourceFiles());
                        }
                    }
                } else if (isUftDataTableFile(path.getPath())) {
                    FilePath filePath = new FilePath(new File(fileFullPath));
                    UftTestType testType = isUftTestFolder(filePath.getParent().list());
                    if (testType.isNone()) {
                        if (EditType.ADD.equals(path.getEditType())) {
                            if (filePath.exists()) {
                                ScmResourceFile resourceFile = createDataTable(workspace, filePath);
                                result.getNewScmResourceFiles().add(resourceFile);
                            }
                        } else if (EditType.DELETE.equals(path.getEditType())) {
                            if (!filePath.exists()) {
                                ScmResourceFile resourceFile = createDataTable(workspace, filePath);
                                result.getDeletedScmResourceFiles().add(resourceFile);
                            }
                        }
                    }
                }
            }
        }

        return result;
    }

    private static AutomatedTest createAutomatedTest(FilePath root, FilePath dirPath, UftTestType testType) throws IOException, InterruptedException {
        AutomatedTest test = new AutomatedTest();
        test.setName(dirPath.getName());

        //set component - relative path from root
        String relativePath = getRelativePath(root, dirPath);
        String packageName = relativePath.length() != dirPath.getName().length() ? relativePath.substring(0, relativePath.length() - dirPath.getName().length() - 1) : "";
        test.setPackage(packageName);

        if (testType != null && !testType.isNone()) {
            test.setUftTestType(testType);
        }

        String description = UFTTestUtil.getTestDescription(dirPath);
        test.setDescription(description);

        return test;
    }

    private static String getRelativePath(FilePath root, FilePath path) throws IOException, InterruptedException {
        String testPath = path.getRemote();
        String rootPath = root.getRemote();
        String relativePath = testPath.replace(rootPath, "");
        relativePath = StringUtils.strip(relativePath, "\\/");
        relativePath.replaceAll("/", "\\");
        return relativePath;
    }

    private static boolean isFileExist(String path) {
        File file = new File(path);
        return file.exists();
    }

    private static boolean initialDetectionFileExist(FilePath workspace) {
        try {
            File rootFile = new File(workspace.toURI());
            File file = new File(rootFile, INITIAL_DETECTION_FILE);
            return file.exists();

        } catch (Exception e) {
            return false;
        }
    }

    private static void createInitialDetectionFile(FilePath workspace) {
        try {
            File rootFile = new File(workspace.toURI());
            File file = new File(rootFile, INITIAL_DETECTION_FILE);
            file.createNewFile();
        } catch (IOException | InterruptedException e) {
            logger.error("Failed to createInitialDetectionFile : " + e.getMessage());
        }
    }

    /*private static void removeInitialDetectionFlag(FilePath workspace) {
        try {
            File rootFile = new File(workspace.toURI());
            File file = new File(rootFile, INITIAL_DETECTION_FILE);
            file.delete();
        } catch (IOException | InterruptedException e) {
            logger.error("Failed to removeInitialDetectionFlag");
        }
    }*/

    private static UFTTestDetectionResult doInitialDetection(FilePath workspace) throws IOException, InterruptedException {
        UFTTestDetectionResult result = new UFTTestDetectionResult();
        scanFileSystemRecursively(workspace, workspace, result.getNewTests(), result.getNewScmResourceFiles());
        return result;
    }

    private static void scanFileSystemRecursively(FilePath root, FilePath dirPath, List<AutomatedTest> foundTests, List<ScmResourceFile> foundResources) throws IOException, InterruptedException {
        List<FilePath> paths = dirPath.isDirectory() ? dirPath.list() : Arrays.asList(dirPath);

        //if it test folder - create new test, else drill down to subFolders
        UftTestType testType = isUftTestFolder(paths);
        if (!testType.isNone()) {
            AutomatedTest test = createAutomatedTest(root, dirPath, testType);
            foundTests.add(test);

        } else {
            for (FilePath path : paths) {
                if (path.isDirectory()) {
                    scanFileSystemRecursively(root, path, foundTests, foundResources);
                } else if (isUftDataTableFile(path.getName())) {
                    ScmResourceFile dataTable = createDataTable(root, path);
                    foundResources.add(dataTable);
                }
            }
        }
    }

    private static ScmResourceFile createDataTable(FilePath root, FilePath path) throws IOException, InterruptedException {
        ScmResourceFile resourceFile = new ScmResourceFile();
        resourceFile.setName(path.getName());
        resourceFile.setRelativePath(getRelativePath(root, path));
        return resourceFile;

    }

    private static boolean isUftDataTableFile(String path) {
        return path.endsWith(XLSXExtention) || path.endsWith(XLSExtention);
    }

    private static UftTestType isUftTestFolder(List<FilePath> paths) {
        for (FilePath path : paths) {
            if (path.getName().endsWith(STFileExtention)) {
                return UftTestType.API;
            }
            if (path.getName().endsWith(QTPFileExtention)) {
                return UftTestType.GUI;
            }
        }

        return UftTestType.None;
    }

    private static boolean isTestMainFilePath(String path) {
        String lowerPath = path.toLowerCase();
        if (lowerPath.endsWith(STFileExtention)) {
            return true;
        } else if (lowerPath.endsWith(QTPFileExtention)) {
            return true;
        }

        return false;
    }

    private static FilePath getTestFolderForTestMainFile(String path) {
        if (isTestMainFilePath(path)) {
            File file = new File(path);
            File parent = file.getParentFile();
            return new FilePath(parent);
        }
        return null;
    }

    private static void publishDetectionResults(AbstractBuild<?, ?> build, TaskListener _logger, UFTTestDetectionResult detectionResult) {

        try {
            File file = getReportXmlFile(build);
            JAXBContext jaxbContext = JAXBContext.newInstance(UFTTestDetectionResult.class);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            jaxbMarshaller.marshal(detectionResult, file);
            //jaxbMarshaller.marshal(detectionResult, System.out);

        } catch (JAXBException e) {
            _logger.error("Failed to persist detection results: " + e);
        }
    }

    public static UFTTestDetectionResult readDetectionResults(Run run) {

        File file = getReportXmlFile(run);
        try {
            JAXBContext context = JAXBContext.newInstance(UFTTestDetectionResult.class);
            Unmarshaller m = context.createUnmarshaller();
            UFTTestDetectionResult result = (UFTTestDetectionResult) m.unmarshal(new FileReader(file));
            return result;
        } catch (JAXBException | FileNotFoundException e) {
            return null;
        }
    }

    private static File getReportXmlFile(Run run) {
        File reportXmlFile = new File(run.getRootDir(), DETECTION_RESULT_FILE);
        return reportXmlFile;
    }
}