/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.skywalking.apm.agent.core.logging.kafka;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.serialization.BytesSerializer;
import org.apache.kafka.common.utils.Bytes;
import org.apache.skywalking.apm.agent.core.boot.BootService;
import org.apache.skywalking.apm.agent.core.boot.DefaultImplementor;
import org.apache.skywalking.apm.agent.core.logging.api.ILog;
import org.apache.skywalking.apm.agent.core.logging.api.LogManager;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * Configuring, initializing and holding a KafkaProducer instance for reporters.
 */
@DefaultImplementor
public class KafkaProducerManager implements BootService, Runnable {

    private static final ILog logger = LogManager.getLogger(KafkaProducerManager.class);

    private KafkaProducer<Bytes, Bytes> producer;

    @Override
    public void prepare() throws Throwable {
        final Properties properties = new Properties();
        properties.setProperty(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaLoggingPluginConfig.Plugin.KafkaLogger.BOOTSTRAP_SERVERS);
        for (Map.Entry<String, String> entry : KafkaLoggingPluginConfig.Plugin.KafkaLogger.PRODUCER_CONFIG.entrySet()) {
            properties.setProperty(entry.getKey(), entry.getValue());
        }

        AdminClient adminClient = AdminClient.create(properties);
        DescribeTopicsResult topicsResult = adminClient.describeTopics(Arrays.asList(
                KafkaLoggingPluginConfig.Plugin.KafkaLogger.TOPIC_NAME
        ));
        Set<String> topics = new HashSet();
        for (Map.Entry<String, KafkaFuture<TopicDescription>> entry : topicsResult.values().entrySet()) {
            try {
                entry.getValue().get();
                continue;
            } catch (ExecutionException e) {
                logger.error("Describe topics failure.", e);
            } catch (InterruptedException e) {
                logger.error("Describe topics failure.", e);
            }
            topics.add(entry.getKey());
        }

        if (!topics.isEmpty()) {
            logger.warn("These topics" + topics + " don't exist.");
        }

        producer = new KafkaProducer(properties, new BytesSerializer(), new BytesSerializer());
    }

    @Override
    public void boot() {

    }

    @Override
    public void onComplete() {
    }

    @Override
    public void run() {

    }

    /**
     * Get the KafkaProducer instance to send data to Kafka broker.
     */
    public final KafkaProducer<Bytes, Bytes> getProducer() {
        return producer;
    }

    @Override
    public void shutdown() {
        producer.flush();
        producer.close();
    }
}
