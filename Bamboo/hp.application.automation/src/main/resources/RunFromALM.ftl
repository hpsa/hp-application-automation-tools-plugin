[@ww.select labelKey="Alm.almServerInputLbl" name="almServer" list=uiConfigBean.getExecutableLabels('hpAlmServer') extraUtility=addExecutableLink/]
[@ww.textfield labelKey="Alm.userNameInputLbl" name="userName" required='true'/]
[@ww.password labelKey="Alm.passwordInputLbl" name="password" showPassword="false"/]
[@ww.textfield labelKey="Alm.domainInputLbl" name="domain" required='true'/]
[@ww.textfield labelKey="Alm.projectInputLbl" name="projectName" required='true'/]
[@ww.textarea labelKey="Alm.testsPathInputLbl" name="testPathInput" required='true' rows="4"/]
[@ww.textfield labelKey="Alm.timelineInputLbl" name="timeoutInput"/]

[@ww.checkbox labelKey='Alm.advancedLbl' name='AdvancedOption' toggle='true' /]
[@ui.bambooSection dependsOn='AdvancedOption' showOn='true']
    [@ww.select labelKey="Alm.runModeInputLbl" name="runMode" list="runModeItems" emptyOption="false"/]
    [@ww.textfield labelKey="Alm.testingToolHostInputLbl" name="testingToolHost" required='false'/]
[/@ui.bambooSection]