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
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.threads.JMeterContextService.ThreadCounts;
import org.apache.jmeter.util.JMeterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class InfluxMetricSender {
    private static final Logger log = LoggerFactory.getLogger(InfluxMetricSender.class);
    private static final boolean TOINFLUX = JMeterUtils.getPropDefault("summariser.influx.out.enabled", false);
    private static final boolean INFLUX_LOG_ENABLED = JMeterUtils.getPropDefault("summariser.influx.log.enabled", false);
    private static final String INFLUX_URL = JMeterUtils.getPropDefault("report.influx.host", "http://localhost:8086");
    private static final String INFLUX_ORG = JMeterUtils.getPropDefault("report.influx.org", "jmeterOrg");
    private static final String INFLUX_BUCKET = JMeterUtils.getPropDefault("report.influx.bucket", "jmeterBucket");
    private static final int INFLUX_CONNECTION_TIMEOUT = JMeterUtils.getPropDefault("summariser.influx.connection.timeout", 5000);
    private static final int INFLUX_SOCKET_TIMEOUT = JMeterUtils.getPropDefault("summariser.influx.socket.timeout", 5000);
    private static final int INFLUX_REQUEST_TIMEOUT = JMeterUtils.getPropDefault("summariser.influx.request.timeout", 5000);
    private static final String INFLUX_APPLICATION = JMeterUtils.getPropDefault("summariser.influx.application", "myApp");
    private static final String INFLUX_TEST_SUITE = JMeterUtils.getPropDefault("summariser.influx.application.suite", "load-test");
    private static final String DELTA_MEASUREMENT = "delta,";
    private static final String TOTAL_MEASUREMENT = "total,";
    private static final String DECIMAL_FORMAT = "0.00";
    private final DecimalFormat DF;
    private static final int THREAD_POOL_SIZE = 5;
    private static TagHttpClient tagClient;
    private static ExecutorService executorService;

    public InfluxMetricSender() throws URISyntaxException {
        if (TOINFLUX) {
            String fullHost = INFLUX_URL + "/api/v2/write?org=" + INFLUX_ORG + "&" + "bucket=" + INFLUX_BUCKET;
            tagClient = new TagHttpClient(INFLUX_CONNECTION_TIMEOUT, INFLUX_REQUEST_TIMEOUT, INFLUX_SOCKET_TIMEOUT, new URI(fullHost));
        }

        executorService = Executors.newSingleThreadExecutor();
        this.DF = new DecimalFormat("0.00");
    }

    public String getProject() {
        return INFLUX_APPLICATION;
    }

    public String getSuite() {
        return INFLUX_TEST_SUITE;
    }


    public void sendIntervalMetric(SummariserRunningSample summariserRunningSample) {
        if (tagClient.isOpen()) {
            String metric = "delta,".concat(this.getMetric(summariserRunningSample));
            executorService.submit(new HTTPCallable(tagClient.getClient(), tagClient.getHTTPPost(), metric));
        }

    }

    public void sendTotalMetric(SummariserRunningSample summariserRunningSample) {
        if (tagClient.isOpen()) {
            String metric = "total,".concat(this.getMetric(summariserRunningSample));
            executorService.submit(new HTTPCallable(tagClient.getClient(), tagClient.getHTTPPost(), metric));
        }

    }

    public void sendSampleMetric(String lineProtocol) {
        if (tagClient.isOpen() && lineProtocol.length() > 0) {
            executorService.submit(new HTTPCallable(tagClient.getClient(), tagClient.getHTTPPost(), lineProtocol));
            if (INFLUX_LOG_ENABLED) {
                log.info(lineProtocol);
            }
        }

    }

    public void shutDown() {
        executorService.shutdown();

        try {
            executorService.awaitTermination(10000L, TimeUnit.MILLISECONDS);
        } catch (InterruptedException var3) {
            log.error(var3.getMessage());
        }

        try {
            tagClient.close();
        } catch (IOException var2) {
            log.error(var2.getMessage());
        }

    }

    private String getMetric(SummariserRunningSample summariserRunningSample) {
        StringBuilder sb = new StringBuilder(100);
        sb.append("application=" + INFLUX_APPLICATION + ",");
        sb.append("suite=" + INFLUX_TEST_SUITE + " ");
        ThreadCounts tc = JMeterContextService.getThreadCounts();
        sb.append("ath=" + tc.activeThreads + ",");
        sb.append("sth=" + tc.startedThreads + ",");
        sb.append("eth=" + tc.finishedThreads + ",");
        sb.append("count=" + summariserRunningSample.getNumSamples() + ",");
        sb.append("min=" + summariserRunningSample.getMin() + ",");
        sb.append("avg=" + summariserRunningSample.getAverage() + ",");
        sb.append("max=" + summariserRunningSample.getMax() + ",");
        sb.append("err=" + summariserRunningSample.getErrorCount() + ",");
        sb.append("errpct=" + this.DF.format(summariserRunningSample.getErrorPercentage() * 100.0D) + ",");
        sb.append("rate=" + this.DF.format(summariserRunningSample.getRate()));
        if (INFLUX_LOG_ENABLED) {
            log.info(sb.toString());
        }

        return sb.toString();
    }
}
