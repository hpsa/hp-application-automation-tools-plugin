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
package com.hpe.application.automation.tools.common.sdk.request;

import com.hpe.application.automation.tools.common.Pair;
import com.hpe.application.automation.tools.common.rest.HttpHeaders;
import com.hpe.application.automation.tools.common.RestXmlUtils;
import com.hpe.application.automation.tools.common.sdk.Client;
import com.hpe.application.automation.tools.common.sdk.ResourceAccessLevel;
import com.hpe.application.automation.tools.common.sdk.Response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
