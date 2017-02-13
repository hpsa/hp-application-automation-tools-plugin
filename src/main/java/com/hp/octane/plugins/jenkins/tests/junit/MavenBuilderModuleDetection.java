// (C) Copyright 2003-2015 Hewlett-Packard Development Company, L.P.

package com.hp.octane.plugins.jenkins.tests.junit;

import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Project;
import hudson.tasks.Builder;
import hudson.tasks.Maven;

import java.io.File;

public class MavenBuilderModuleDetection extends AbstractMavenModuleDetection {

    public MavenBuilderModuleDetection(AbstractBuild build) {
        super(build);
    }

    protected void addPomDirectories(AbstractBuild build) {
        if (build.getProject() instanceof FreeStyleProject ||
                "hudson.matrix.MatrixConfiguration".equals(build.getProject().getClass().getName())) {
            boolean unknownBuilder = false;
            for (Builder builder: ((Project<?, ?>) build.getProject()).getBuilders()) {
                if (builder instanceof Maven) {
                    Maven maven = (Maven) builder;
                    if (maven.pom != null) {
                        if (maven.pom.endsWith("/pom.xml") || maven.pom.endsWith("\\pom.xml")) {
                            addPomDirectory(new FilePath(rootDir, maven.pom.substring(0, maven.pom.length() - 8)));
                            continue;
                        } else {
                            int p = maven.pom.lastIndexOf(File.separatorChar);
                            if (p > 0) {
                                addPomDirectory(new FilePath(rootDir, maven.pom.substring(0, p)));
                                continue;
                            }
                        }
                    }
                    addPomDirectory(rootDir);
                } else {
                    unknownBuilder = true;
                }
            }
            if (unknownBuilder && !pomDirs.contains(rootDir)) {
                // attempt to support shell and batch executions too
                // simply assume there is top-level pom file for any non-maven builder
                addPomDirectory(rootDir);
            }
        }
    }
}
