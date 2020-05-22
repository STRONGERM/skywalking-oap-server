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

package org.apache.skywalking.oap.server.library.client.elasticsearch;

import com.google.common.base.Splitter;
import com.google.gson.*;
import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.*;
import javax.net.ssl.SSLContext;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.*;
import org.apache.http.auth.*;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.*;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.ssl.*;
import org.apache.skywalking.oap.server.library.client.Client;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.action.bulk.*;
import org.elasticsearch.action.get.*;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.*;
import org.elasticsearch.action.support.*;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.*;
import org.elasticsearch.client.indices.*;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.*;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.joda.time.LocalDate;
import org.slf4j.*;

/**
 * @author peng-yongsheng
 */
public class ElasticSearchClient implements Client {

    private static final Logger logger = LoggerFactory.getLogger(ElasticSearchClient.class);

    public static final String TYPE = "type";
    private final String clusterNodes;
    private final String protocol;
    private final String trustStorePath;
    private final String trustStorePass;
    private final String namespace;
    private final String user;
    private final String password;
    protected RestHighLevelClient client;

    public ElasticSearchClient(String clusterNodes, String protocol, String trustStorePath, String trustStorePass,
        String namespace, String user, String password) {
        this.clusterNodes = clusterNodes;
        this.protocol = protocol;
        this.namespace = namespace;
        this.user = user;
        this.password = password;
        this.trustStorePath = trustStorePath;
        this.trustStorePass = trustStorePass;
    }

    @Override
    public void connect() throws IOException, KeyStoreException, NoSuchAlgorithmException, KeyManagementException, CertificateException {
        List<HttpHost> pairsList = parseClusterNodes(clusterNodes);
        RestClientBuilder builder;
        if (StringUtils.isNotBlank(user) && StringUtils.isNotBlank(password)) {
            final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(user, password));

            if (StringUtils.isBlank(trustStorePath)) {
                builder = RestClient.builder(pairsList.toArray(new HttpHost[0]))
                    .setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider));
            } else {
                KeyStore truststore = KeyStore.getInstance("jks");
                try (InputStream is = Files.newInputStream(Paths.get(trustStorePath))) {
                    truststore.load(is, trustStorePass.toCharArray());
                }
                SSLContextBuilder sslBuilder = SSLContexts.custom()
                    .loadTrustMaterial(truststore, null);
                final SSLContext sslContext = sslBuilder.build();
                builder = RestClient.builder(pairsList.toArray(new HttpHost[0]))
                    .setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider).setSSLContext(sslContext));
            }
        } else {
            builder = RestClient.builder(pairsList.toArray(new HttpHost[0]));
        }

        client = new RestHighLevelClient(builder);
        client.ping();
    }

    @Override
    public void shutdown() throws IOException {
        client.close();
    }

    private List<HttpHost> parseClusterNodes(String nodes) {
        List<HttpHost> httpHosts = new LinkedList<>();
        logger.info("elasticsearch cluster nodes: {}", nodes);
        List<String> nodesSplit = Splitter.on(",").omitEmptyStrings().splitToList(nodes);

        for (String node : nodesSplit) {
            String host = node.split(":")[0];
            String port = node.split(":")[1];
            httpHosts.add(new HttpHost(host, Integer.parseInt(port), protocol));
        }

        return httpHosts;
    }

    public boolean createIndex(String indexName) throws IOException {
        indexName = formatIndexName(indexName);

        CreateIndexRequest request = new CreateIndexRequest(indexName);
        CreateIndexResponse response = client.indices().create(request, RequestOptions.DEFAULT);
        logger.debug("create {} index finished, isAcknowledged: {}", indexName, response.isAcknowledged());
        return response.isAcknowledged();
    }

    public boolean createIndex(String indexName, JsonObject settings, JsonObject mapping) throws IOException {
        indexName = formatIndexName(indexName);
        CreateIndexRequest request = new CreateIndexRequest(indexName);
        request.settings(settings.toString(), XContentType.JSON);
        request.mapping(mapping.toString(), XContentType.JSON);
        CreateIndexResponse response = client.indices().create(request, RequestOptions.DEFAULT);
        logger.debug("create {} index finished, isAcknowledged: {}", indexName, response.isAcknowledged());
        return response.isAcknowledged();
    }

    public List<String> retrievalIndexByAliases(String aliases) throws IOException {
        aliases = formatIndexName(aliases);
        Response response = client.getLowLevelClient().performRequest(HttpGet.METHOD_NAME, "/_alias/" + aliases);
        if (HttpStatus.SC_OK == response.getStatusLine().getStatusCode()) {
            Gson gson = new Gson();
            InputStreamReader reader = new InputStreamReader(response.getEntity().getContent());
            JsonObject responseJson = gson.fromJson(reader, JsonObject.class);
            logger.debug("retrieval indexes by aliases {}, response is {}", aliases, responseJson);
            return new ArrayList<>(responseJson.keySet());
        }
        return Collections.emptyList();
    }

    /**
     * If your indexName is retrieved from elasticsearch through {@link #retrievalIndexByAliases(String)} or some other
     * method and it already contains namespace. Then you should delete the index by this method, this method will no
     * longer concatenate namespace.
     * <p>
     * https://github.com/apache/skywalking/pull/3017
     */
    public boolean deleteByIndexName(String indexName) throws IOException {
        return deleteIndex(indexName, false);
    }

    /**
     * If your indexName is obtained from metadata or configuration and without namespace. Then you should delete the
     * index by this method, this method automatically concatenates namespace.
     * <p>
     * https://github.com/apache/skywalking/pull/3017
     */
    public boolean deleteByModelName(String modelName) throws IOException {
        return deleteIndex(modelName, true);
    }

    private boolean deleteIndex(String indexName, boolean formatIndexName) throws IOException {
        if (formatIndexName) {
            indexName = formatIndexName(indexName);
        }
        DeleteIndexRequest request = new DeleteIndexRequest(indexName);
        AcknowledgedResponse response = client.indices().delete(request, RequestOptions.DEFAULT);
        logger.debug("delete {} index finished, isAcknowledged: {}", indexName, response.isAcknowledged());
        return response.isAcknowledged();
    }

    public boolean isExistsIndex(String indexName) throws IOException {
        indexName = formatIndexName(indexName);
        GetIndexRequest request = new GetIndexRequest();
        request.indices(indexName);
        return client.indices().exists(request);
    }

    public boolean isExistsTemplate(String indexName) throws IOException {
        indexName = formatIndexName(indexName);

        Response response = client.getLowLevelClient().performRequest(HttpHead.METHOD_NAME, "/_template/" + indexName);

        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode == HttpStatus.SC_OK) {
            return true;
        } else if (statusCode == HttpStatus.SC_NOT_FOUND) {
            return false;
        } else {
            throw new IOException("The response status code of template exists request should be 200 or 404, but it is " + statusCode);
        }
    }

    public boolean createTemplate(String indexName, JsonObject settings, JsonObject mapping) throws IOException {
        indexName = formatIndexName(indexName);

        JsonArray patterns = new JsonArray();
        patterns.add(indexName + "-*");

        JsonObject aliases = new JsonObject();
        aliases.add(indexName, new JsonObject());

        JsonObject template = new JsonObject();
        template.add("index_patterns", patterns);
        template.add("aliases", aliases);
        template.add("settings", settings);
        template.add("mappings", mapping);

        HttpEntity entity = new NStringEntity(template.toString(), ContentType.APPLICATION_JSON);

        Response response = client.getLowLevelClient().performRequest(HttpPut.METHOD_NAME, "/_template/" + indexName, Collections.emptyMap(), entity);
        return response.getStatusLine().getStatusCode() == HttpStatus.SC_OK;
    }

    public boolean deleteTemplate(String indexName) throws IOException {
        indexName = formatIndexName(indexName);

        Response response = client.getLowLevelClient().performRequest(HttpDelete.METHOD_NAME, "/_template/" + indexName);
        return response.getStatusLine().getStatusCode() == HttpStatus.SC_OK;
    }

    public SearchResponse search(String indexName, SearchSourceBuilder searchSourceBuilder) throws IOException {
        indexName = formatIndexName(indexName);
        SearchRequest searchRequest = new SearchRequest(indexName);
        searchRequest.types(TYPE);
        searchRequest.source(searchSourceBuilder);
        return client.search(searchRequest);
    }

    public SearchResponse search(String indexName, SearchSourceBuilder searchSourceBuilder, String traceId) throws IOException {
        String[] fullIndexNames = formatIndexNames(indexName, 0, 0, traceId);
        if (fullIndexNames == null || fullIndexNames.length == 0) {
            return null;
        }
        SearchRequest searchRequest = new SearchRequest(fullIndexNames);
        searchRequest.types(TYPE);
        searchRequest.source(searchSourceBuilder);
        return client.search(searchRequest);
    }

    public SearchResponse search(String indexName, SearchSourceBuilder searchSourceBuilder, long startTimestamp, long endTimestamp) throws IOException {
        String[] fullIndexNames = formatIndexNames(indexName, startTimestamp, endTimestamp, null);
        SearchRequest searchRequest = new SearchRequest(fullIndexNames);
        searchRequest.types(TYPE);
        searchRequest.source(searchSourceBuilder);
        return client.search(searchRequest);
    }

    public GetResponse get(String indexName, String id) throws IOException {
        indexName = formatIndexName(indexName);
        GetRequest request = new GetRequest(indexName, TYPE, id);
        return client.get(request);
    }

    public SearchResponse ids(String indexName, String[] ids, long startTimestamp, long endTimestamp) throws IOException {
        String[] fullIndexNames = formatIndexNames(indexName, startTimestamp, endTimestamp, null);

        SearchRequest searchRequest = new SearchRequest(fullIndexNames);
        searchRequest.types(TYPE);
        searchRequest.source().query(QueryBuilders.idsQuery().addIds(ids)).size(ids.length);
        return client.search(searchRequest);
    }

    public SearchResponse ids(String indexName, String[] ids) throws IOException {
        indexName = formatIndexName(indexName);

        SearchRequest searchRequest = new SearchRequest(indexName);
        searchRequest.types(TYPE);
        searchRequest.source().query(QueryBuilders.idsQuery().addIds(ids)).size(ids.length);
        return client.search(searchRequest);
    }

    public void forceInsert(String indexName, String id, XContentBuilder source) throws IOException {
        IndexRequest request = prepareInsert(indexName, id, source);
        request.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
        client.index(request);
    }

    public void forceUpdate(String indexName, String id, XContentBuilder source, long version) throws IOException {
        UpdateRequest request = prepareUpdate(indexName, id, source);
        request.version(version);
        request.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
        client.update(request);
    }

    public void forceUpdate(String indexName, String id, XContentBuilder source) throws IOException {
        UpdateRequest request = prepareUpdate(indexName, id, source);
        request.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
        client.update(request);
    }

    public ElasticSearchInsertRequest prepareInsert(String indexName, String id, XContentBuilder source) {
        indexName = formatIndexName(indexName);
        return new ElasticSearchInsertRequest(indexName, TYPE, id).source(source);
    }

    public ElasticSearchUpdateRequest prepareUpdate(String indexName, String id, XContentBuilder source) {
        indexName = formatIndexName(indexName);
        return new ElasticSearchUpdateRequest(indexName, TYPE, id).doc(source);
    }

    public int delete(String indexName, String timeBucketColumnName, long endTimeBucket) throws IOException {
        indexName = formatIndexName(indexName);
        Map<String, String> params = Collections.singletonMap("conflicts", "proceed");
        String jsonString = "{" +
            "  \"query\": {" +
            "    \"range\": {" +
            "      \"" + timeBucketColumnName + "\": {" +
            "        \"lte\": " + endTimeBucket +
            "      }" +
            "    }" +
            "  }" +
            "}";
        HttpEntity entity = new NStringEntity(jsonString, ContentType.APPLICATION_JSON);
        Response response = client.getLowLevelClient().performRequest(HttpPost.METHOD_NAME, "/" + indexName + "/_delete_by_query", params, entity);
        logger.debug("delete indexName: {}, jsonString : {}", indexName, jsonString);
        return response.getStatusLine().getStatusCode();
    }

    public void synchronousBulk(BulkRequest request) {
        request.timeout(TimeValue.timeValueMinutes(2));
        request.setRefreshPolicy(WriteRequest.RefreshPolicy.WAIT_UNTIL);
        request.waitForActiveShards(ActiveShardCount.ONE);
        try {
            int size = request.requests().size();
            BulkResponse responses = client.bulk(request);
            logger.info("Synchronous bulk took time: {} millis, size: {}", responses.getTook().getMillis(), size);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    public BulkProcessor createBulkProcessor(int bulkActions, int flushInterval, int concurrentRequests) {
        BulkProcessor.Listener listener = new BulkProcessor.Listener() {
            @Override
            public void beforeBulk(long executionId, BulkRequest request) {
                int numberOfActions = request.numberOfActions();
                logger.debug("Executing bulk [{}] with {} requests", executionId, numberOfActions);
            }

            @Override
            public void afterBulk(long executionId, BulkRequest request, BulkResponse response) {
                if (response.hasFailures()) {
                    logger.warn("Bulk [{}] executed with failures", executionId);
                } else {
                    logger.info("Bulk execution id [{}] completed in {} milliseconds, size: {}", executionId, response.getTook().getMillis(), request.requests().size());
                }
            }

            @Override
            public void afterBulk(long executionId, BulkRequest request, Throwable failure) {
                logger.error("Failed to execute bulk", failure);
            }
        };

        return BulkProcessor.builder(client::bulkAsync, listener)
            .setBulkActions(bulkActions)
            .setFlushInterval(TimeValue.timeValueSeconds(flushInterval))
            .setConcurrentRequests(concurrentRequests)
            .setBackoffPolicy(BackoffPolicy.exponentialBackoff(TimeValue.timeValueMillis(100), 3))
            .build();
    }

    public String formatIndexName(String indexName) {
        if (StringUtils.isNotEmpty(namespace)) {
            return namespace + "_" + indexName;
        }
        return indexName;
    }

    private String[] formatIndexNames(String indexName, long startTimestamp, long endTimestamp, String traceId) throws IOException {
        ArrayList<String> indexList = new ArrayList<>();
        ArrayList<String> dateList = new ArrayList<>();
        String baseName = formatIndexName(indexName);

        if (startTimestamp != 0 && endTimestamp != 0) {
            //use dateTime limit
            LocalDate nowDate = LocalDate.now();
            LocalDate startDate = LocalDate.fromDateFields(new Date(startTimestamp));
            LocalDate endDate = LocalDate.fromDateFields(new Date(endTimestamp));
            if (indexName.endsWith("month")) {
                for (; !startDate.isAfter(endDate); startDate = startDate.plusMonths(1)) {
                    if (!startDate.isAfter(nowDate))
                        dateList.add(startDate.toString("yyyyMM"));
                }
            } else {
                for (; !startDate.isAfter(endDate); startDate = startDate.plusDays(1)) {
                    if (!startDate.isAfter(nowDate))
                        dateList.add(startDate.toString("yyyyMMdd"));
                }
            }
            //use traceId limit
            if (traceId != null) {
                long ts = Long.valueOf(traceId.split("\\.")[2]) / 10000;
                String traceDateStr = LocalDate.fromDateFields(new Date(ts)).toString("yyyyMMdd");
                for (String dtString : dateList) {
                    if (dtString.compareTo(traceDateStr) > 0) {
                        dateList.remove(dtString);
                    }
                }
            }
        } else {
            if (traceId != null) {
                long ts = Long.valueOf(traceId.split("\\.")[2]) / 10000;
                LocalDate traceDate = LocalDate.fromDateFields(new Date(ts));
                LocalDate nextDate = traceDate.plusDays(1);
                dateList.add(traceDate.toString("yyyyMMdd"));
                dateList.add(nextDate.toString("yyyyMMdd"));
            }
        }

        if (dateList.size() > 0) {
            for (String dateStr : dateList) {
                indexList.add(baseName + "-" + dateStr);
            }
        }

        //drop index if not exists

        if (indexList.size() > 0) {
            List<String> existsIndexes = retrievalIndexByAliases(indexName);
            indexList.removeIf(s -> !existsIndexes.contains(s));
        }

        if (indexList.size() > 0) {
            return indexList.toArray(new String[0]);
        } else {
            return null;
        }
    }

}
