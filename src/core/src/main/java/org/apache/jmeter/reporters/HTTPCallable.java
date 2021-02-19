/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.jmeter.reporters;

import java.util.concurrent.Callable;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

class HTTPCallable implements Callable<Void> {
    final String metric;
    final HttpPost http;
    final CloseableHttpClient client;

    public HTTPCallable(CloseableHttpClient client, HttpPost http, String metric) {
        this.client = client;
        this.http = http;
        this.metric = metric;
    }

    public Void call() throws Exception {
        this.http.setEntity(new StringEntity(this.metric));
        CloseableHttpResponse response = this.client.execute(this.http);
        EntityUtils.consumeQuietly(response.getEntity());
        response.close();
        return null;
    }
}
