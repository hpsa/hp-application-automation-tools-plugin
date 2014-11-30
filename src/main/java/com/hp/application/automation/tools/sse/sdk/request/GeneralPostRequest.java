package com.hp.application.automation.tools.sse.sdk.request;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hp.application.automation.tools.common.Pair;
import com.hp.application.automation.tools.rest.HttpHeaders;
import com.hp.application.automation.tools.sse.common.RestXmlUtils;
import com.hp.application.automation.tools.sse.sdk.Client;
import com.hp.application.automation.tools.sse.sdk.ResourceAccessLevel;
import com.hp.application.automation.tools.sse.sdk.Response;

/**
 * Created by barush on 29/10/2014.
 */
public abstract class GeneralPostRequest extends GeneralRequest {
    
    protected GeneralPostRequest(Client client) {
        super(client);
    }
    
    @Override
    protected Map<String, String> getHeaders() {

        Map<String, String> ret = new HashMap<String, String>();
        ret.put(HttpHeaders.CONTENT_TYPE, RestXmlUtils.APP_XML);
        ret.put(HttpHeaders.ACCEPT, RestXmlUtils.APP_XML);

        return ret;
    }
    
    @Override
    public Response perform() {
        
        return client.httpPost(
                getUrl(),
                getDataBytes(),
                getHeaders(),
                ResourceAccessLevel.PROTECTED);
    }
    
    private byte[] getDataBytes() {
        
        StringBuilder builder = new StringBuilder("<Entity><Fields>");
        for (Pair<String, String> currPair : getDataFields()) {
            builder.append(RestXmlUtils.fieldXml(currPair.getFirst(), currPair.getSecond()));
        }
        
        return builder.append("</Fields></Entity>").toString().getBytes();
    }
    
    protected List<Pair<String, String>> getDataFields() {
        
        return new ArrayList<Pair<String, String>>(0);
    }
}
