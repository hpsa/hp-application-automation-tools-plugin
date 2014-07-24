package com.hp.application.automation.tools.sse.result;

import java.util.List;
import java.util.Map;

import com.hp.application.automation.tools.sse.common.StringUtils;
import com.hp.application.automation.tools.sse.common.XPathUtils;
import com.hp.application.automation.tools.sse.result.model.junit.Testsuites;
import com.hp.application.automation.tools.sse.sdk.Client;
import com.hp.application.automation.tools.sse.sdk.Logger;
import com.hp.application.automation.tools.sse.sdk.Response;
import com.hp.application.automation.tools.sse.sdk.handler.Handler;
import com.hp.application.automation.tools.sse.sdk.request.GetRequest;
import com.hp.application.automation.tools.sse.sdk.request.GetRunEntityNameRequest;

public abstract class Publisher extends Handler {
    
    public Publisher(Client client, String entityId, String runId) {
        
        super(client, entityId, runId);
    }
    
    public Testsuites publish(
            String nameSuffix,
            String url,
            String domain,
            String project,
            Logger logger) {
        
        Testsuites ret = null;
        Response response = getRunEntityTestSetRunsRequest(_client, _runId).execute();
        List<Map<String, String>> testInstanceRun = getTestInstanceRun(response, logger);
        String entityName = getEntityName(nameSuffix, logger);
        if (testInstanceRun != null) {
            ret =
                    new JUnitParser().toModel(
                            testInstanceRun,
                            entityName,
                            _runId,
                            url,
                            domain,
                            project);
        }
        
        return ret;
    }
    
    protected Response getEntityName(String nameSuffix) {
        
        return new GetRunEntityNameRequest(_client, nameSuffix, _entityId).execute();
    }
    
    protected List<Map<String, String>> getTestInstanceRun(Response response, Logger logger) {
        
        List<Map<String, String>> ret = null;
        try {
            if (!StringUtils.isNullOrEmpty(response.toString())) {
                ret = XPathUtils.toEntities(response.toString());
            }
        } catch (Throwable cause) {
            logger.log(String.format(
                    "Failed to parse TestInstanceRuns response XML. Exception: %s, XML: %s",
                    cause.getMessage(),
                    response.toString()));
        }
        
        return ret;
    }
    
    protected abstract GetRequest getRunEntityTestSetRunsRequest(Client client, String runId);
    
    protected abstract String getEntityName(String nameSuffix, Logger logger);
}
