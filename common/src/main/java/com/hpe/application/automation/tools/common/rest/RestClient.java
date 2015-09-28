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

package com.hpe.application.automation.tools.common.rest;

import com.hpe.application.automation.tools.common.SSEException;
import com.hpe.application.automation.tools.common.RestXmlUtils;
import com.hpe.application.automation.tools.common.sdk.Client;
import com.hpe.application.automation.tools.common.sdk.HttpRequestDecorator;
import com.hpe.application.automation.tools.common.sdk.ResourceAccessLevel;
import com.hpe.application.automation.tools.common.sdk.Response;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/***
 * 
 * @author Effi Bar-She'an
 * @author Dani Schreiber
 * 
 */
public class RestClient implements Client {
    
    private final String _serverUrl;
    protected Map<String, String> _cookies = new HashMap<String, String>();
    private final String _restPrefix;
    private final String _webuiPrefix;
    private final String _username;
    
    public RestClient(String url, String domain, String project, String username) {
        
        if (!url.endsWith("/")) {
            url = String.format("%s/", url);
        }
        _serverUrl = url;
        _username = username;
        _restPrefix =
                getPrefixUrl(
                        "rest",
                        String.format("domains/%s", domain),
                        String.format("projects/%s", project));
        _webuiPrefix = getPrefixUrl("webui/alm", domain, project);
    }
    
    public String build(String suffix) {
        
        return String.format("%1$s%2$s", _serverUrl, suffix);
    }
    
    public String buildRestRequest(String suffix) {
        
        return String.format("%1$s/%2$s", _restPrefix, suffix);
    }
    
    public String buildWebUIRequest(String suffix) {
        
        return String.format("%1$s/%2$s", _webuiPrefix, suffix);
    }
    
    public Response httpGet(
            String url,
            String queryString,
            Map<String, String> headers,
            ResourceAccessLevel resourceAccessLevel) {
        
        Response ret = null;
        try {
            ret = doHttp(RestXmlUtils.GET, url, queryString, null, headers, resourceAccessLevel);
        } catch (Throwable cause) {
            throw new SSEException(cause);
        }
        
        return ret;
    }
    
    public Response httpPost(
            String url,
            byte[] data,
            Map<String, String> headers,
            ResourceAccessLevel resourceAccessLevel) {
        
        Response ret = null;
        try {
            ret = doHttp(RestXmlUtils.POST, url, null, data, headers, resourceAccessLevel);
        } catch (Throwable cause) {
            throw new SSEException(cause);
        }
        
        return ret;
    }
    
    public Response httpPut(
            String url,
            byte[] data,
            Map<String, String> headers,
            ResourceAccessLevel resourceAccessLevel) {
        
        Response ret = null;
        try {
            ret = doHttp(RestXmlUtils.PUT, url, null, data, headers, resourceAccessLevel);
        } catch (Throwable cause) {
            throw new SSEException(cause);
        }
        
        return ret;
    }
    
    public String getServerUrl() {
        
        return _serverUrl;
    }
    
    private String getPrefixUrl(String protocol, String domain, String project) {
        
        return String.format("%s%s/%s/%s", _serverUrl, protocol, domain, project);
    }
    
    /**
     * @param type
     *            http operation: get post put delete
     * @param url
     *            to work on
     * @param queryString
     * @param data
     *            to write, if a writable operation
     * @param headers
     *            to use in the request
     * @return http response
     */
    private Response doHttp(
            String type,
            String url,
            String queryString,
            byte[] data,
            Map<String, String> headers,
            ResourceAccessLevel resourceAccessLevel) {
        
        Response ret = null;
        if ((queryString != null) && !queryString.isEmpty()) {
            url += "?" + queryString;
        }
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod(type);
            
            Map<String, String> decoratedHeaders = new HashMap<String, String>();
            if (headers != null) {
                decoratedHeaders.putAll(headers);
            }
            
            HttpRequestDecorator.decorateHeaderWithUserInfo(
                    decoratedHeaders,
                    getUsername(),
                    resourceAccessLevel);
            
            prepareHttpRequest(connection, decoratedHeaders, data);
            connection.connect();
            ret = retrieveHtmlResponse(connection);
            updateCookies(ret);
        } catch (Throwable cause) {
            throw new SSEException(cause);
        }
        
        return ret;
    }
    
    /**
     * @param connnection
     *            connection to set the headers and bytes in
     * @param headers
     *            to use in the request, such as content-type
     * @param bytes
     *            the actual data to post in the connection.
     */
    private void prepareHttpRequest(
            HttpURLConnection connnection,
            Map<String, String> headers,
            byte[] bytes) {
        
        // set all cookies for request
        connnection.setRequestProperty(RestXmlUtils.COOKIE, getCookies());
        
        setConnectionHeaders(connnection, headers);
        
        setConnectionData(connnection, bytes);
    }
    
    private void setConnectionData(HttpURLConnection connnection, byte[] bytes) {
        
        if (bytes != null && bytes.length > 0) {
            connnection.setDoOutput(true);
            try {
                OutputStream out = connnection.getOutputStream();
                out.write(bytes);
                out.flush();
                out.close();
            } catch (Throwable cause) {
                throw new SSEException(cause);
            }
        }
    }
    
    private void setConnectionHeaders(HttpURLConnection connnection, Map<String, String> headers) {
        
        if (headers != null) {
            Iterator<Entry<String, String>> headersIterator = headers.entrySet().iterator();
            while (headersIterator.hasNext()) {
                Entry<String, String> header = headersIterator.next();
                connnection.setRequestProperty(header.getKey(), header.getValue());
            }
        }
    }
    
    /**
     * @param connection
     *            that is already connected to its url with an http request, and that should contain
     *            a response for us to retrieve
     * @return a response from the server to the previously submitted http request
     * @throws IOException
     */
    private Response retrieveHtmlResponse(HttpURLConnection connection) {
        
        Response ret = new Response();
        
        try {
            ret.setStatusCode(connection.getResponseCode());
            ret.setHeaders(connection.getHeaderFields());
        } catch (Throwable cause) {
            throw new SSEException(cause);
        }
        
        InputStream inputStream;
        // select the source of the input bytes, first try 'regular' input
        try {
            inputStream = connection.getInputStream();
        }
        // if the connection to the server somehow failed, for example 404 or 500,
        // con.getInputStream() will throw an exception, which we'll keep.
        // we'll also store the body of the exception page, in the response data. */
        catch (Throwable e) {
            inputStream = connection.getErrorStream();
            ret.setFailure(e);
        }
        
        // this takes data from the previously set stream (error or input) 
        // and stores it in a byte[] inside the response
        ByteArrayOutputStream container = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int read;
        try {
            while ((read = inputStream.read(buf, 0, 1024)) > 0) {
                container.write(buf, 0, read);
            }
            ret.setData(container.toByteArray());
        } catch (Exception ex) {
            throw new SSEException(ex);
        }
        
        return ret;
    }
    
    private void updateCookies(Response response) {
        
        Iterable<String> newCookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        if (newCookies != null) {
            for (String cookie : newCookies) {
                int equalIndex = cookie.indexOf('=');
                int semicolonIndex = cookie.indexOf(';');
                String cookieKey = cookie.substring(0, equalIndex);
                String cookieValue = cookie.substring(equalIndex + 1, semicolonIndex);
                _cookies.put(cookieKey, cookieValue);
            }
        }
    }
    
    private String getCookies() {
        
        StringBuilder ret = new StringBuilder();
        if (!_cookies.isEmpty()) {
            for (Entry<String, String> entry : _cookies.entrySet()) {
                ret.append(entry.getKey()).append("=").append(entry.getValue()).append(";");
            }
        }
        
        return ret.toString();
    }
    
    @Override
    public String getUsername() {
        
        return _username;
    }
}
