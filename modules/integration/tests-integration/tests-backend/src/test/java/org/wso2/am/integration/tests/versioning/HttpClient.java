/*
*Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*WSO2 Inc. licenses this file to you under the Apache License,
*Version 2.0 (the "License"); you may not use this file except
*in compliance with the License.
*You may obtain a copy of the License at
*
*http://www.apache.org/licenses/LICENSE-2.0
*
*Unless required by applicable law or agreed to in writing,
*software distributed under the License is distributed on an
*"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*KIND, either express or implied.  See the License for the
*specific language governing permissions and limitations
*under the License.
*/
package org.wso2.am.integration.tests.versioning;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.wso2.am.integration.test.utils.http.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class HttpClient {
    private static Log log = LogFactory.getLog(HttpRequestUtil.class);

    public HttpClient() {
    }

    public static HttpResponse doPost(String url, Map<String, List<String>> headers, List<NameValuePair> urlParameters)
            throws IOException {
        CloseableHttpClient httpClient = getHttpsClient();
        org.apache.http.HttpResponse response = sendPOSTMessage(httpClient, url, headers, urlParameters);
        return constructResponse(response);
    }

    public static HttpResponse doPost(URL url, String urlParams, Map<String, List<String>> headers) throws IOException {
        List<NameValuePair> urlParameters = new ArrayList();
        if (urlParams != null && urlParams.contains("=")) {
            String[] paramList = urlParams.split("&");
            String[] arr = paramList;
            for (String newPair : arr) {
                String pair = newPair;
                if (pair.contains("=")) {
                    String[] pairList = pair.split("=");
                    String key = pairList[0];
                    String value = pairList.length > 1 ? pairList[1] : "";
                    urlParameters.add(new BasicNameValuePair(key, URLDecoder.decode(value, "UTF-8")));

                }
            }
        }

        return doPost((String) url.toString(), (Map) headers, (List) urlParameters);
    }

    public static CloseableHttpClient getHttpsClient() {
        int timeout = 7;
        RequestConfig config = RequestConfig.custom().setConnectTimeout(timeout * 10000).
                setConnectionRequestTimeout(timeout * 10000).setSocketTimeout(timeout * 10000).build();
        CloseableHttpClient httpClient = HttpClients.custom().disableRedirectHandling().
                setDefaultRequestConfig(config).setHostnameVerifier(SSLConnectionSocketFactory.
                ALLOW_ALL_HOSTNAME_VERIFIER).build();
        return httpClient;
    }

    private static org.apache.http.HttpResponse sendPOSTMessage(CloseableHttpClient httpClient,
                                                                String url, Map<String, List<String>> headers,
                                                                List<NameValuePair> urlParameters) throws IOException {
        HttpPost post = new HttpPost(url);
        if (headers != null) {
            Iterator i = headers.entrySet().iterator();

            while (i.hasNext()) {
                Map.Entry<String, List<String>> head = (Map.Entry) i.next();
                for (int itr = 0; head.getValue().size() > itr; itr++) {
                    post.addHeader((String) head.getKey(), (String) head.getValue().get(itr));
                }
            }
        }
        post.setEntity(new UrlEncodedFormEntity(urlParameters));
        return httpClient.execute(post);
    }

    private static HttpResponse constructResponse(org.apache.http.HttpResponse response) throws IOException {
        int code = response.getStatusLine().getStatusCode();
        String body = getResponseBody(response);
        Header[] headers = response.getAllHeaders();

        Map<String, List<String>> heads = new HashMap();
        Header[] arr = headers;
        for (Header newHeader : arr) {
            List<String> headerArray = new ArrayList<>();
            if (heads.get(newHeader.getName()) != null) {
                headerArray.add(headerArray.size(), newHeader.getValue());
                heads.put(newHeader.getName(), headerArray);
            } else {
                headerArray.add(0, newHeader.getValue());
                heads.put(newHeader.getName(), headerArray);
            }
        }

        HttpResponse res = new HttpResponse(body, code, heads);
        return res;
    }

    public static String getResponseBody(org.apache.http.HttpResponse response) throws IOException {
        BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent(),
                "UTF-8"));
        StringBuffer sb = new StringBuffer();

        String line;
        while ((line = rd.readLine()) != null) {
            sb.append(line);
        }

        rd.close();
        return sb.toString();
    }
}
