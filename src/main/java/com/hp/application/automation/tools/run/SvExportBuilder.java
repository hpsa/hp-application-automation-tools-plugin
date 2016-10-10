// (c) Copyright 2016 Hewlett Packard Enterprise Development LP
// Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
// The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

package com.hp.application.automation.tools.run;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintStream;

import com.hp.application.automation.tools.model.SvExportModel;
import com.hp.application.automation.tools.model.SvServiceSelectionModel;
import com.hp.sv.jsvconfigurator.processor.ExportProcessor;
import com.hp.sv.jsvconfigurator.serverclient.ICommandExecutor;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.util.FormValidation;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

public class SvExportBuilder extends AbstractSvRunBuilder<SvExportModel> {

    @DataBoundConstructor
    public SvExportBuilder(String serverName, String targetDirectory, boolean cleanTargetDirectory, SvServiceSelectionModel serviceSelection) {
        super(new SvExportModel(serverName, targetDirectory, cleanTargetDirectory, serviceSelection));
    }

    @Override
    protected void logConfig(PrintStream logger, String prefix) {
        logger.println(prefix + "Target Directory: " + model.getTargetDirectory());
        super.logConfig(logger, prefix);
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Override
    public boolean performImpl(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws Exception {
        PrintStream logger = listener.getLogger();

        ExportProcessor processor = new ExportProcessor(null);
        ICommandExecutor exec = createCommandExecutor();

        verifyNotNull(model.getTargetDirectory(), "Target directory must be set");

        String targetDirectory = build.getWorkspace().child(model.getTargetDirectory()).getRemote();

        if (model.isCleanTargetDirectory()) {
            cleanTargetDirectory(logger, targetDirectory);
        }

        for (ServiceInfo service : getServiceList(false, logger, build)) {
            logger.printf("  Exporting service '%s' [%s] to %s %n", service.getName(), service.getId(), targetDirectory);
            processor.process(exec, targetDirectory, service.getId());
        }

        return true;
    }

    /**
     * Cleans all sub-folders containing *.vproj file.
     */
    private void cleanTargetDirectory(PrintStream logger, String targetDirectory) throws IOException {
        File target = new File(targetDirectory);
        if (target.exists()) {
            File[] subfolders = target.listFiles((FilenameFilter) DirectoryFileFilter.INSTANCE);
            if (subfolders.length > 0) {
                logger.println("  Cleaning target directory...");
            }
            for (File subfolder : subfolders) {
                if (subfolder.listFiles((FilenameFilter) new SuffixFileFilter(".vproj")).length > 0) {
                    logger.println("    Deleting subfolder of target directory: " + subfolder.getAbsolutePath());
                    FileUtils.deleteDirectory(subfolder);
                } else {
                    logger.println("    Skipping delete of directory '" + subfolder.getAbsolutePath() + "' because it does not contain any *.vproj file.");
                }
            }
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractSvRunDescriptor {

        public DescriptorImpl() {
            super("SV: Export Virtual Service");
        }

        @SuppressWarnings("unused")
        public FormValidation doCheckTargetDirectory(@QueryParameter String targetDirectory) {
            if (StringUtils.isBlank(targetDirectory)) {
                return FormValidation.error("Target directory cannot be empty");
            }
            return FormValidation.ok();
        }
    }
}
