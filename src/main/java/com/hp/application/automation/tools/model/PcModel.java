package com.hp.application.automation.tools.model;

import java.util.Arrays;
import java.util.List;

import org.kohsuke.stapler.DataBoundConstructor;

public class PcModel {

    public static final String    COLLATE         = "Collate Results";
    public static final String    COLLATE_ANALYZE = "Collate And Analyze";
    public static final String    DO_NOTHING      = "Do Not Collate";

    private final String           pcServerName;
    private final String           almUserName;
    private final SecretContainer  almPassword;
    private final String           almDomain;
    private final String           almProject;
    private final String           testId;
    private final String           testInstanceId;
    private final TimeslotDuration timeslotDuration;
    private final PostRunAction    postRunAction;
    private final boolean          vudsMode;
    private final String           description;

    @DataBoundConstructor
    public PcModel(String pcServerName, String almUserName, String almPassword, String almDomain, String almProject,
            String testId, String testInstanceId, String timeslotDurationHours, String timeslotDurationMinutes,
            PostRunAction postRunAction, boolean vudsMode, String description) {

        this.pcServerName = pcServerName;
        this.almUserName = almUserName;
        this.almPassword = setPassword(almPassword);
        this.almDomain = almDomain;
        this.almProject = almProject;
        this.testId = testId;
        this.testInstanceId = testInstanceId;
        this.timeslotDuration = new TimeslotDuration(timeslotDurationHours, timeslotDurationMinutes);
        this.postRunAction = postRunAction;
        this.vudsMode = vudsMode;
        this.description = description;
    }

    protected SecretContainer setPassword(String almPassword) {

        SecretContainer secretContainer = new SecretContainerImpl();
        secretContainer.initialize(almPassword);
        return secretContainer;
    }

    public String getPcServerName() {

        return this.pcServerName;
    }

    public String getAlmUserName() {

        return this.almUserName;
    }

    public SecretContainer getAlmPassword() {

        return this.almPassword;
    }

    public String getAlmDomain() {

        return this.almDomain;
    }

    public String getAlmProject() {

        return this.almProject;
    }

    public String getTestId() {

        return this.testId;
    }

    public String getTestInstanceId() {

        return this.testInstanceId;
    }

    public TimeslotDuration getTimeslotDuration() {

        return this.timeslotDuration;
    }

    public boolean isVudsMode() {

        return this.vudsMode;
    }

    public PostRunAction getPostRunAction() {

        return this.postRunAction;
    }

    public String getDescription() {

        return this.description;
    }

    public static List<PostRunAction> getPostRunActions() {
        return Arrays.asList(PostRunAction.values());
    }
    
    @Override
    public String toString() {

        return String.format("[PCServer='%s', User='%s', %s", runParamsToString().substring(1));
    }
    
    public String runParamsToString() {
        
        String vudsModeString = (vudsMode) ? ", VUDsMode='true'" : "";
                
        return String.format("[Domain='%s', Project='%s', TestID='%s', " +
        		"TestInstanceID='%s', TimeslotDuration='%s', PostRunAction='%s'%s]",

        		almDomain, almProject, testId, testInstanceId,  
        		timeslotDuration, postRunAction.getValue(), vudsModeString);
    }

}
