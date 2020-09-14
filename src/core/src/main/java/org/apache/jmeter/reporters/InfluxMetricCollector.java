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

import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.samplers.Sampler;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.util.JMeterUtils;

class InfluxMetricCollector {
    private final StringBuilder samples;
    private final String suite;
    private final String application;
    private static boolean ERRPRSTOINFLUX = JMeterUtils.getPropDefault("summariser.influx.errorBody.enabled", false);


    public InfluxMetricCollector(String project, String suite) {
        this.application = project;
        this.suite = suite;
        this.samples = new StringBuilder();
    }

    public void addSample(SampleResult result) {

        StringBuilder sb = new StringBuilder();
        sb.append("samples");
        sb.append(",application=").append(this.escapeTag(this.application));
        sb.append(",suite=").append(this.escapeTag(this.suite));
        sb.append(",label=").append(this.escapeTag(result.getSampleLabel()));
        sb.append(",status=");
        if (result.isSuccessful()) {
            sb.append("Success");
        } else {
            sb.append("Failure");
        }
        sb.append(",threadname=").append(this.escapeTag(result.getThreadName()));
        sb.append(",responsecode=").append(this.escapeResponceCode(result.getResponseCode()));
        sb.append(" ").append("ath=").append(JMeterContextService.getThreadCounts().activeThreads);
        sb.append(",duration=").append(result.getTime());
        sb.append(",latency=").append(result.getLatency());
        sb.append(",bytes=").append(result.getBytesAsLong());

        if(!result.isSuccessful()) {
            if(ERRPRSTOINFLUX) {
                Sampler sampler;
                sb.append(",errormessage=").append(this.influxStrConvertor(result.getResponseDataAsString()));
                sb.append(",errorresponsecode=").append(this.escapeResponceCode(result.getResponseCode()));
                sb.append(",requestbody=").append(this.influxStrConvertor(result.getSamplerData()));
            }
        }

        sb.append(" ").append(result.getTimeStamp()).append("000000");
        sb.append("\n");

        this.samples.append(sb.toString());
    }

    public String getLineProtocol() {
        String linePropertocol = this.samples.toString();
        this.samples.setLength(0);
        return linePropertocol;
    }

    public String influxStrConvertor(String influxString){
        influxString = influxString.replace("\"", "\\\"")
                .trim();

        return "\"" + influxString + "\"";
    }

    public String escapeTag(String tag) {
        tag = tag.replaceAll(",", "\\\\,")
                .replaceAll(" ", "\\\\ ")
                .replaceAll("=", "\\\\=")
                .trim();

        return tag;
    }

    public String escapeResponceCode(String code) {
        code = code.trim();
        if (code.length() > 5) {
            code = "0";
        }
        return code;
    }
}
