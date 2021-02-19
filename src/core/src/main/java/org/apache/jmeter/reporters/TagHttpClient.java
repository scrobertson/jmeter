/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.jmeter.reporters;

import java.io.IOException;
import java.net.URI;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.jmeter.util.JMeterUtils;

public class TagHttpClient {
    PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();
    CloseableHttpClient client;
    HttpPost httpPost;
    boolean isEnabled;

    TagHttpClient(int connctionTimeOut, int requestTimeOut, int socketTimeOut, URI influxURI) {
        this.connManager.setDefaultMaxPerRoute(2);
        this.connManager.setMaxTotal(2);
        RequestConfig config = RequestConfig.custom().setConnectTimeout(connctionTimeOut)
                .setConnectionRequestTimeout(requestTimeOut).setSocketTimeout(socketTimeOut).build();
        this.client = HttpClients.custom().setConnectionManager(this.connManager).setDefaultRequestConfig(config).build();
        this.httpPost = new HttpPost();
        this.httpPost.setURI(influxURI);
        this.httpPost.setHeader("Content-type", "application/x-www-form-urlencoded; charset=UTF-8");
        this.httpPost.setHeader("Authorization", "Token " + JMeterUtils.getProperty("influx.token"));
        this.isEnabled = true;
    }

    public CloseableHttpClient getClient() {
        return this.client;
    }

    public HttpPost getHTTPPost() {
        return this.httpPost;
    }

    public boolean isOpen() {
        return this.isEnabled;
    }

    public void close() throws IOException {
        this.client.close();
        this.connManager.close();
    }
}
