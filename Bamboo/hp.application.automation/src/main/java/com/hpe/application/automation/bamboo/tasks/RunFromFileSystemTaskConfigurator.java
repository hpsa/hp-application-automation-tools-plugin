/**
 © Copyright 2015 Hewlett Packard Enterprise Development LP

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in
 all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NON INFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 THE SOFTWARE.
 */
package com.hpe.application.automation.bamboo.tasks;

import java.util.HashMap;
import java.util.Map;
import com.atlassian.bamboo.collections.ActionParametersMap;
import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.bamboo.utils.error.ErrorCollection;
import com.atlassian.bamboo.utils.i18n.I18nBean;
import com.atlassian.util.concurrent.NotNull;
import com.atlassian.util.concurrent.Nullable;
import org.apache.commons.lang.StringUtils;

public class RunFromFileSystemTaskConfigurator extends AbstractLauncherTaskConfigurator {

	public static final String TESTS_PATH = "testPathInput";
	public static final String TIMEOUT = "timeoutInput";
    public static final String MCSERVERURL = "mcServerURLInput";
    public static final String MCUSERNAME = "mcUserNameInput";
    public static final String MCPASSWORD = "mcPasswordInput";
    public static final String MCAPPLICATIONPATH = "mcApplicationPathInput";
    public static final String MCAPPLICATIONIDKEY = "mcApplicationIDKeyInput";

	public static final String PUBLISH_MODE_ALWAYS_STRING = "RunFromFileSystemTask.publishMode.always";
	public static final String PUBLISH_MODE_FAILED_STRING = "RunFromFileSystemTask.publishMode.failed";
	public static final String PUBLISH_MODE_NEVER_STRING = "RunFromFileSystemTask.publishMode.never";

	public static final String ARTIFACT_NAME_FORMAT_STRING = "RunFromFileSystemTask.artifactNameFormat";

	public static final String PUBLISH_MODE_PARAM = "publishMode";
	public static final String PUBLISH_MODE_ITEMS_PARAM = "publishModeItems";

	public static final String PUBLISH_MODE_ALWAYS_VALUE = "always";
	public static final String PUBLISH_MODE_FAILED_VALUE = "failed";
	public static final String PUBLISH_MODE_NEVER_VALUE = "never";
	public static final String TASK_NAME_VALUE = "RunFromFileSystemTask.taskName";
	private static final String TASK_ID_CONTROL = "RunFromFileSystemTask.taskId";
	private static final String TASK_ID_LBL = "CommonTask.taskIdLbl";

	public Map<String, String> generateTaskConfigMap(@NotNull final ActionParametersMap params, @Nullable final TaskDefinition previousTaskDefinition)
	{
		final Map<String, String> config = super.generateTaskConfigMap(params, previousTaskDefinition);

		config.put(TESTS_PATH, params.getString(TESTS_PATH));
		config.put(TIMEOUT, params.getString(TIMEOUT));
        config.put(MCSERVERURL, params.getString(MCSERVERURL));
        config.put(MCUSERNAME, params.getString(MCUSERNAME));
        config.put(MCPASSWORD, params.getString(MCPASSWORD));
        config.put(MCAPPLICATIONPATH, params.getString(MCAPPLICATIONPATH));
        config.put(MCAPPLICATIONIDKEY, params.getString(MCAPPLICATIONIDKEY));
		config.put(PUBLISH_MODE_PARAM, params.getString(PUBLISH_MODE_PARAM));
		config.put(CommonTaskConfigurationProperties.TASK_NAME, getI18nBean().getText(TASK_NAME_VALUE));

		return config;
	}

	public void validate(@NotNull final ActionParametersMap params, @NotNull final ErrorCollection errorCollection)
	{
		super.validate(params, errorCollection);

		final String pathParameter = params.getString(TESTS_PATH);
		final String timeoutParameter = params.getString(TIMEOUT);

		I18nBean textProvider = getI18nBean();

		if (StringUtils.isEmpty(pathParameter))
		{
			errorCollection.addError(TESTS_PATH, textProvider.getText("RunFromFileSystemTaskConfigurator.error.testsPathIsEmpty"));
		}

		if(!StringUtils.isEmpty(timeoutParameter))
		{   	 
			if (!StringUtils.isNumeric(timeoutParameter) || Integer.parseInt(timeoutParameter) < 0 | Integer.parseInt(timeoutParameter) > 30)
			{
				errorCollection.addError(TIMEOUT, textProvider.getText("RunFromFileSystemTaskConfigurator.error.timeoutIsNotCorrect"));
			} 	   
		}
	}

	@Override
	public void populateContextForCreate(@NotNull final Map<String, Object> context) {

		super.populateContextForCreate(context);

		context.put(PUBLISH_MODE_PARAM, PUBLISH_MODE_FAILED_VALUE);

		populateContextForLists(context);
	}

	@Override
	public void populateContextForEdit(@NotNull final Map<String, Object> context, @NotNull final TaskDefinition taskDefinition)
	{
		super.populateContextForEdit(context, taskDefinition);

		context.put(TESTS_PATH, taskDefinition.getConfiguration().get(TESTS_PATH));
		context.put(TIMEOUT, taskDefinition.getConfiguration().get(TIMEOUT));
		context.put(MCSERVERURL, taskDefinition.getConfiguration().get(MCSERVERURL));
		context.put(MCUSERNAME, taskDefinition.getConfiguration().get(MCUSERNAME));
		context.put(MCPASSWORD, taskDefinition.getConfiguration().get(MCPASSWORD));
		context.put(MCAPPLICATIONPATH, taskDefinition.getConfiguration().get(MCAPPLICATIONPATH));
		context.put(MCAPPLICATIONIDKEY, taskDefinition.getConfiguration().get(MCAPPLICATIONIDKEY));
		context.put(PUBLISH_MODE_PARAM, taskDefinition.getConfiguration().get(PUBLISH_MODE_PARAM));
		context.put(TASK_ID_CONTROL, getI18nBean().getText(TASK_ID_LBL) + String.format("%03d",taskDefinition.getId()));

		populateContextForLists(context);
	}

	private void populateContextForLists(@org.jetbrains.annotations.NotNull final Map<String, Object> context)
	{
		context.put(PUBLISH_MODE_ITEMS_PARAM, getPublishModes());
	}

	private Map<String, String> getPublishModes()
	{
		Map<String, String> publishModesMap = new HashMap<String, String>();

		I18nBean textProvider = getI18nBean();

		publishModesMap.put(PUBLISH_MODE_FAILED_VALUE, textProvider.getText(PUBLISH_MODE_FAILED_STRING));
		publishModesMap.put(PUBLISH_MODE_ALWAYS_VALUE, textProvider.getText(PUBLISH_MODE_ALWAYS_STRING));
		publishModesMap.put(PUBLISH_MODE_NEVER_VALUE, textProvider.getText(PUBLISH_MODE_NEVER_STRING));

		return publishModesMap;
	}
}
