/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.client;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.Build;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionRequestValidationException;
import org.elasticsearch.action.admin.cluster.node.tasks.list.ListTasksResponse;
import org.elasticsearch.action.admin.cluster.storedscripts.DeleteStoredScriptRequest;
import org.elasticsearch.action.admin.cluster.storedscripts.GetStoredScriptRequest;
import org.elasticsearch.action.admin.cluster.storedscripts.GetStoredScriptResponse;
import org.elasticsearch.action.admin.cluster.storedscripts.PutStoredScriptRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.explain.ExplainRequest;
import org.elasticsearch.action.explain.ExplainResponse;
import org.elasticsearch.action.fieldcaps.FieldCapabilitiesRequest;
import org.elasticsearch.action.fieldcaps.FieldCapabilitiesResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.get.MultiGetRequest;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.ClearScrollRequest;
import org.elasticsearch.action.search.ClearScrollResponse;
import org.elasticsearch.action.search.ClosePointInTimeRequest;
import org.elasticsearch.action.search.ClosePointInTimeResponse;
import org.elasticsearch.action.search.MultiSearchRequest;
import org.elasticsearch.action.search.MultiSearchResponse;
import org.elasticsearch.action.search.OpenPointInTimeRequest;
import org.elasticsearch.action.search.OpenPointInTimeResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.analytics.InferencePipelineAggregationBuilder;
import org.elasticsearch.client.analytics.ParsedInference;
import org.elasticsearch.client.analytics.ParsedStringStats;
import org.elasticsearch.client.analytics.ParsedTopMetrics;
import org.elasticsearch.client.analytics.StringStatsAggregationBuilder;
import org.elasticsearch.client.analytics.TopMetricsAggregationBuilder;
import org.elasticsearch.client.core.CountRequest;
import org.elasticsearch.client.core.CountResponse;
import org.elasticsearch.client.core.GetSourceRequest;
import org.elasticsearch.client.core.GetSourceResponse;
import org.elasticsearch.client.core.MainRequest;
import org.elasticsearch.client.core.MainResponse;
import org.elasticsearch.client.core.MultiTermVectorsRequest;
import org.elasticsearch.client.core.MultiTermVectorsResponse;
import org.elasticsearch.client.core.TermVectorsRequest;
import org.elasticsearch.client.core.TermVectorsResponse;
import org.elasticsearch.client.tasks.TaskSubmissionResponse;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.util.concurrent.FutureUtils;
import org.elasticsearch.common.util.concurrent.ListenableFuture;
import org.elasticsearch.core.CheckedConsumer;
import org.elasticsearch.core.CheckedFunction;
import org.elasticsearch.index.rankeval.RankEvalRequest;
import org.elasticsearch.index.rankeval.RankEvalResponse;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.index.reindex.ReindexRequest;
import org.elasticsearch.index.reindex.UpdateByQueryRequest;
import org.elasticsearch.plugins.spi.NamedXContentProvider;
import org.elasticsearch.rest.BytesRestResponse;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.script.mustache.MultiSearchTemplateRequest;
import org.elasticsearch.script.mustache.MultiSearchTemplateResponse;
import org.elasticsearch.script.mustache.SearchTemplateRequest;
import org.elasticsearch.script.mustache.SearchTemplateResponse;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.bucket.adjacency.AdjacencyMatrixAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.adjacency.ParsedAdjacencyMatrix;
import org.elasticsearch.search.aggregations.bucket.composite.CompositeAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.composite.ParsedComposite;
import org.elasticsearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.filter.FiltersAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.filter.ParsedFilter;
import org.elasticsearch.search.aggregations.bucket.filter.ParsedFilters;
import org.elasticsearch.search.aggregations.bucket.geogrid.GeoHashGridAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.geogrid.GeoTileGridAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.geogrid.ParsedGeoHashGrid;
import org.elasticsearch.search.aggregations.bucket.geogrid.ParsedGeoTileGrid;
import org.elasticsearch.search.aggregations.bucket.global.GlobalAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.global.ParsedGlobal;
import org.elasticsearch.search.aggregations.bucket.histogram.AutoDateHistogramAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.histogram.HistogramAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.histogram.ParsedAutoDateHistogram;
import org.elasticsearch.search.aggregations.bucket.histogram.ParsedDateHistogram;
import org.elasticsearch.search.aggregations.bucket.histogram.ParsedHistogram;
import org.elasticsearch.search.aggregations.bucket.histogram.ParsedVariableWidthHistogram;
import org.elasticsearch.search.aggregations.bucket.histogram.VariableWidthHistogramAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.missing.MissingAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.missing.ParsedMissing;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedReverseNested;
import org.elasticsearch.search.aggregations.bucket.nested.ReverseNestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.range.DateRangeAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.range.GeoDistanceAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.range.IpRangeAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.range.ParsedBinaryRange;
import org.elasticsearch.search.aggregations.bucket.range.ParsedDateRange;
import org.elasticsearch.search.aggregations.bucket.range.ParsedGeoDistance;
import org.elasticsearch.search.aggregations.bucket.range.ParsedRange;
import org.elasticsearch.search.aggregations.bucket.range.RangeAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.sampler.InternalSampler;
import org.elasticsearch.search.aggregations.bucket.sampler.ParsedSampler;
import org.elasticsearch.search.aggregations.bucket.terms.DoubleTerms;
import org.elasticsearch.search.aggregations.bucket.terms.LongRareTerms;
import org.elasticsearch.search.aggregations.bucket.terms.LongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedDoubleTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongRareTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedSignificantLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedSignificantStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringRareTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.SignificantLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.SignificantStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.StringRareTerms;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.elasticsearch.search.aggregations.metrics.AvgAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.CardinalityAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.ExtendedStatsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.GeoBoundsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.GeoCentroidAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.InternalHDRPercentileRanks;
import org.elasticsearch.search.aggregations.metrics.InternalHDRPercentiles;
import org.elasticsearch.search.aggregations.metrics.InternalTDigestPercentileRanks;
import org.elasticsearch.search.aggregations.metrics.InternalTDigestPercentiles;
import org.elasticsearch.search.aggregations.metrics.MaxAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.MedianAbsoluteDeviationAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.MinAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.ParsedAvg;
import org.elasticsearch.search.aggregations.metrics.ParsedCardinality;
import org.elasticsearch.search.aggregations.metrics.ParsedExtendedStats;
import org.elasticsearch.search.aggregations.metrics.ParsedGeoBounds;
import org.elasticsearch.search.aggregations.metrics.ParsedGeoCentroid;
import org.elasticsearch.search.aggregations.metrics.ParsedHDRPercentileRanks;
import org.elasticsearch.search.aggregations.metrics.ParsedHDRPercentiles;
import org.elasticsearch.search.aggregations.metrics.ParsedMax;
import org.elasticsearch.search.aggregations.metrics.ParsedMedianAbsoluteDeviation;
import org.elasticsearch.search.aggregations.metrics.ParsedMin;
import org.elasticsearch.search.aggregations.metrics.ParsedScriptedMetric;
import org.elasticsearch.search.aggregations.metrics.ParsedStats;
import org.elasticsearch.search.aggregations.metrics.ParsedSum;
import org.elasticsearch.search.aggregations.metrics.ParsedTDigestPercentileRanks;
import org.elasticsearch.search.aggregations.metrics.ParsedTDigestPercentiles;
import org.elasticsearch.search.aggregations.metrics.ParsedTopHits;
import org.elasticsearch.search.aggregations.metrics.ParsedValueCount;
import org.elasticsearch.search.aggregations.metrics.ParsedWeightedAvg;
import org.elasticsearch.search.aggregations.metrics.ScriptedMetricAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.StatsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.SumAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.TopHitsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.ValueCountAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.WeightedAvgAggregationBuilder;
import org.elasticsearch.search.aggregations.pipeline.DerivativePipelineAggregationBuilder;
import org.elasticsearch.search.aggregations.pipeline.ExtendedStatsBucketPipelineAggregationBuilder;
import org.elasticsearch.search.aggregations.pipeline.InternalBucketMetricValue;
import org.elasticsearch.search.aggregations.pipeline.InternalSimpleValue;
import org.elasticsearch.search.aggregations.pipeline.ParsedBucketMetricValue;
import org.elasticsearch.search.aggregations.pipeline.ParsedDerivative;
import org.elasticsearch.search.aggregations.pipeline.ParsedExtendedStatsBucket;
import org.elasticsearch.search.aggregations.pipeline.ParsedPercentilesBucket;
import org.elasticsearch.search.aggregations.pipeline.ParsedSimpleValue;
import org.elasticsearch.search.aggregations.pipeline.ParsedStatsBucket;
import org.elasticsearch.search.aggregations.pipeline.PercentilesBucketPipelineAggregationBuilder;
import org.elasticsearch.search.aggregations.pipeline.StatsBucketPipelineAggregationBuilder;
import org.elasticsearch.search.aggregations.timeseries.ParsedTimeSeries;
import org.elasticsearch.search.aggregations.timeseries.TimeSeriesAggregationBuilder;
import org.elasticsearch.search.suggest.Suggest;
import org.elasticsearch.search.suggest.completion.CompletionSuggestion;
import org.elasticsearch.search.suggest.completion.CompletionSuggestionBuilder;
import org.elasticsearch.search.suggest.phrase.PhraseSuggestion;
import org.elasticsearch.search.suggest.phrase.PhraseSuggestionBuilder;
import org.elasticsearch.search.suggest.term.TermSuggestion;
import org.elasticsearch.search.suggest.term.TermSuggestionBuilder;
import org.elasticsearch.xcontent.ContextParser;
import org.elasticsearch.xcontent.DeprecationHandler;
import org.elasticsearch.xcontent.NamedXContentRegistry;
import org.elasticsearch.xcontent.ParseField;
import org.elasticsearch.xcontent.XContentParser;
import org.elasticsearch.xcontent.XContentType;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;

/**
 * High level REST client that wraps an instance of the low level {@link RestClient} and allows to build requests and read responses. The
 * {@link RestClient} instance is internally built based on the provided {@link RestClientBuilder} and it gets closed automatically when
 * closing the {@link RestHighLevelClient} instance that wraps it.
 * <p>
 *
 * In case an already existing instance of a low-level REST client needs to be provided, this class can be subclassed and the
 * {@link #RestHighLevelClient(RestClient, CheckedConsumer, List)} constructor can be used.
 * <p>
 *
 * This class can also be sub-classed to expose additional client methods that make use of endpoints added to Elasticsearch through plugins,
 * or to add support for custom response sections, again added to Elasticsearch through plugins.
 * <p>
 *
 * The majority of the methods in this class come in two flavors, a blocking and an asynchronous version (e.g.
 * {@link #search(SearchRequest, RequestOptions)} and {@link #searchAsync(SearchRequest, RequestOptions, ActionListener)}, where the later
 * takes an implementation of an {@link ActionListener} as an argument that needs to implement methods that handle successful responses and
 * failure scenarios. Most of the blocking calls can throw an {@link IOException} or an unchecked {@link ElasticsearchException} in the
 * following cases:
 *
 * <ul>
 * <li>an {@link IOException} is usually thrown in case of failing to parse the REST response in the high-level REST client, the request
 * times out or similar cases where there is no response coming back from the Elasticsearch server</li>
 * <li>an {@link ElasticsearchException} is usually thrown in case where the server returns a 4xx or 5xx error code. The high-level client
 * then tries to parse the response body error details into a generic ElasticsearchException and suppresses the original
 * {@link ResponseException}</li>
 * </ul>
 *
 * @deprecated The High Level Rest Client is deprecated in favor of the
 * <a href="https://www.elastic.co/guide/en/elasticsearch/client/java-api-client/current/introduction.html">
 * Elasticsearch Java API Client</a>
 */
@Deprecated(since = "7.16.0", forRemoval = true)
@SuppressWarnings("removal")
public class RestHighLevelClient implements Closeable {

    private static final Logger logger = LogManager.getLogger(RestHighLevelClient.class);
    /**
     * Environment variable determining whether to send the 7.x compatibility header
     */
    public static final String API_VERSIONING_ENV_VARIABLE = "ELASTIC_CLIENT_APIVERSIONING";

    // To be called using performClientRequest and performClientRequestAsync to ensure version compatibility check
    private final RestClient client;
    private final NamedXContentRegistry registry;
    private final CheckedConsumer<RestClient, IOException> doClose;
    private final boolean useAPICompatibility;

    /** Do not access directly but through getVersionValidationFuture() */
    private volatile ListenableFuture<Optional<String>> versionValidationFuture;

    private final IndicesClient indicesClient = new IndicesClient(this);
    private final IngestClient ingestClient = new IngestClient(this);
    private final SnapshotClient snapshotClient = new SnapshotClient(this);
    private final MachineLearningClient machineLearningClient = new MachineLearningClient(this);
    private final SecurityClient securityClient = new SecurityClient(this);
    private final TransformClient transformClient = new TransformClient(this);
    private final EqlClient eqlClient = new EqlClient(this);
    private final SearchableSnapshotsClient searchableSnapshotsClient = new SearchableSnapshotsClient(this);

    /**
     * Creates a {@link RestHighLevelClient} given the low level {@link RestClientBuilder} that allows to build the
     * {@link RestClient} to be used to perform requests.
     */
    public RestHighLevelClient(RestClientBuilder restClientBuilder) {
        this(restClientBuilder, Collections.emptyList());
    }

    /**
     * Creates a {@link RestHighLevelClient} given the low level {@link RestClientBuilder} that allows to build the
     * {@link RestClient} to be used to perform requests and parsers for custom response sections added to Elasticsearch through plugins.
     */
    protected RestHighLevelClient(RestClientBuilder restClientBuilder, List<NamedXContentRegistry.Entry> namedXContentEntries) {
        this(restClientBuilder.build(), RestClient::close, namedXContentEntries);
    }

    /**
     * Creates a {@link RestHighLevelClient} given the low level {@link RestClient} that it should use to perform requests and
     * a list of entries that allow to parse custom response sections added to Elasticsearch through plugins.
     * This constructor can be called by subclasses in case an externally created low-level REST client needs to be provided.
     * The consumer argument allows to control what needs to be done when the {@link #close()} method is called.
     * Also subclasses can provide parsers for custom response sections added to Elasticsearch through plugins.
     */
    protected RestHighLevelClient(
        RestClient restClient,
        CheckedConsumer<RestClient, IOException> doClose,
        List<NamedXContentRegistry.Entry> namedXContentEntries
    ) {
        this(restClient, doClose, namedXContentEntries, null);
    }

    /**
     * Creates a {@link RestHighLevelClient} given the low level {@link RestClient} that it should use to perform requests and
     * a list of entries that allow to parse custom response sections added to Elasticsearch through plugins.
     * This constructor can be called by subclasses in case an externally created low-level REST client needs to be provided.
     * The consumer argument allows to control what needs to be done when the {@link #close()} method is called.
     * Also subclasses can provide parsers for custom response sections added to Elasticsearch through plugins.
     */
    protected RestHighLevelClient(
        RestClient restClient,
        CheckedConsumer<RestClient, IOException> doClose,
        List<NamedXContentRegistry.Entry> namedXContentEntries,
        Boolean useAPICompatibility
    ) {
        this.client = Objects.requireNonNull(restClient, "restClient must not be null");
        this.doClose = Objects.requireNonNull(doClose, "doClose consumer must not be null");
        this.registry = new NamedXContentRegistry(
            Stream.of(getDefaultNamedXContents().stream(), getProvidedNamedXContents().stream(), namedXContentEntries.stream())
                .flatMap(Function.identity())
                .collect(toList())
        );
        if (useAPICompatibility == null && "true".equals(System.getenv(API_VERSIONING_ENV_VARIABLE))) {
            this.useAPICompatibility = true;
        } else {
            this.useAPICompatibility = Boolean.TRUE.equals(useAPICompatibility);
        }
    }

    /**
     * Returns the low-level client that the current high-level client instance is using to perform requests
     */
    public final RestClient getLowLevelClient() {
        return client;
    }

    @Override
    public final void close() throws IOException {
        doClose.accept(client);
    }

    /**
     * Provides an {@link IndicesClient} which can be used to access the Indices API.
     *
     * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/indices.html">Indices API on elastic.co</a>
     */
    public final IndicesClient indices() {
        return indicesClient;
    }

    /**
     * Provides a {@link IngestClient} which can be used to access the Ingest API.
     *
     * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/ingest.html">Ingest API on elastic.co</a>
     */
    public final IngestClient ingest() {
        return ingestClient;
    }

    /**
     * Provides a {@link SnapshotClient} which can be used to access the Snapshot API.
     *
     * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/modules-snapshots.html">Snapshot API on elastic.co</a>
     */
    public final SnapshotClient snapshot() {
        return snapshotClient;
    }

    /**
     * A wrapper for the {@link RestHighLevelClient} that provides methods for accessing the Searchable Snapshots APIs.
     * <p>
     * See the <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/searchable-snapshots-apis.html">Searchable Snapshots
     * APIs on elastic.co</a> for more information.
     */
    public SearchableSnapshotsClient searchableSnapshots() {
        return searchableSnapshotsClient;
    }

    /**
     * Provides methods for accessing the Elastic Licensed Machine Learning APIs that
     * are shipped with the Elastic Stack distribution of Elasticsearch. All of
     * these APIs will 404 if run against the OSS distribution of Elasticsearch.
     * <p>
     * See the <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/ml-apis.html">
     * Machine Learning APIs on elastic.co</a> for more information.
     *
     * @return the client wrapper for making Machine Learning API calls
     */
    public MachineLearningClient machineLearning() {
        return machineLearningClient;
    }

    /**
     * Provides methods for accessing the Elastic Licensed Security APIs that
     * are shipped with the Elastic Stack distribution of Elasticsearch. All of
     * these APIs will 404 if run against the OSS distribution of Elasticsearch.
     * <p>
     * See the <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/security-api.html">
     * Security APIs on elastic.co</a> for more information.
     *
     * @return the client wrapper for making Security API calls
     */
    public SecurityClient security() {
        return securityClient;
    }

    /**
     * Provides methods for accessing the Elastic Licensed Data Frame APIs that
     * are shipped with the Elastic Stack distribution of Elasticsearch. All of
     * these APIs will 404 if run against the OSS distribution of Elasticsearch.
     * <p>
     * See the <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/transform-apis.html">
     *     Transform APIs on elastic.co</a> for more information.
     *
     * @return the client wrapper for making Data Frame API calls
     */
    public TransformClient transform() {
        return transformClient;
    }

    /**
     * Provides methods for accessing the Elastic EQL APIs that
     * are shipped with the Elastic Stack distribution of Elasticsearch. All of
     * these APIs will 404 if run against the OSS distribution of Elasticsearch.
     * <p>
     * See the <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/eql.html">
     *     EQL APIs on elastic.co</a> for more information.
     *
     * @return the client wrapper for making Data Frame API calls
     */
    public final EqlClient eql() {
        return eqlClient;
    }

    /**
     * Executes a bulk request using the Bulk API.
     * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-bulk.html">Bulk API on elastic.co</a>
     * @param bulkRequest the request
     * @param options the request options (e.g. headers), use {@link RequestOptions#DEFAULT} if nothing needs to be customized
     * @return the response
     */
    public final BulkResponse bulk(BulkRequest bulkRequest, RequestOptions options) throws IOException {
        return performRequestAndParseEntity(bulkRequest, RequestConverters::bulk, options, BulkResponse::fromXContent, emptySet());
    }

    /**
     * Asynchronously executes a bulk request using the Bulk API.
     * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-bulk.html">Bulk API on elastic.co</a>
     * @param bulkRequest the request
     * @param options the request options (e.g. headers), use {@link RequestOptions#DEFAULT} if nothing needs to be customized
     * @param listener the listener to be notified upon request completion
     * @return cancellable that may be used to cancel the request
     */
    public final Cancellable bulkAsync(BulkRequest bulkRequest, RequestOptions options, ActionListener<BulkResponse> listener) {
        return performRequestAsyncAndParseEntity(
            bulkRequest,
            RequestConverters::bulk,
            options,
            BulkResponse::fromXContent,
            listener,
            emptySet()
        );
    }

    /**
     * Executes a reindex request.
     * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-reindex.html">Reindex API on elastic.co</a>
     * @param reindexRequest the request
     * @param options the request options (e.g. headers), use {@link RequestOptions#DEFAULT} if nothing needs to be customized
     * @return the response
     */
    public final BulkByScrollResponse reindex(ReindexRequest reindexRequest, RequestOptions options) throws IOException {
        return performRequestAndParseEntity(
            reindexRequest,
            RequestConverters::reindex,
            options,
            BulkByScrollResponse::fromXContent,
            singleton(409)
        );
    }

    /**
     * Submits a reindex task.
     * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-reindex.html">Reindex API on elastic.co</a>
     * @param reindexRequest the request
     * @param options the request options (e.g. headers), use {@link RequestOptions#DEFAULT} if nothing needs to be customized
     * @return the submission response
     */
    public final TaskSubmissionResponse submitReindexTask(ReindexRequest reindexRequest, RequestOptions options) throws IOException {
        return performRequestAndParseEntity(
            reindexRequest,
            RequestConverters::submitReindex,
            options,
            TaskSubmissionResponse::fromXContent,
            emptySet()
        );
    }

    /**
     * Asynchronously executes a reindex request.
     * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-reindex.html">Reindex API on elastic.co</a>
     * @param reindexRequest the request
     * @param options the request options (e.g. headers), use {@link RequestOptions#DEFAULT} if nothing needs to be customized
     * @param listener the listener to be notified upon request completion
     * @return cancellable that may be used to cancel the request
     */
    public final Cancellable reindexAsync(
        ReindexRequest reindexRequest,
        RequestOptions options,
        ActionListener<BulkByScrollResponse> listener
    ) {
        return performRequestAsyncAndParseEntity(
            reindexRequest,
            RequestConverters::reindex,
            options,
            BulkByScrollResponse::fromXContent,
            listener,
            singleton(409)
        );
    }

    /**
     * Executes a update by query request.
     * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-update-by-query.html">
     *     Update By Query API on elastic.co</a>
     * @param updateByQueryRequest the request
     * @param options the request options (e.g. headers), use {@link RequestOptions#DEFAULT} if nothing needs to be customized
     * @return the response
     */
    public final BulkByScrollResponse updateByQuery(UpdateByQueryRequest updateByQueryRequest, RequestOptions options) throws IOException {
        return performRequestAndParseEntity(
            updateByQueryRequest,
            RequestConverters::updateByQuery,
            options,
            BulkByScrollResponse::fromXContent,
            singleton(409)
        );
    }

    /**
     * Submits a update by query task.
     * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-update-by-query.html">
     *     Update By Query API on elastic.co</a>
     * @param updateByQueryRequest the request
     * @param options the request options (e.g. headers), use {@link RequestOptions#DEFAULT} if nothing needs to be customized
     * @return the submission response
     */
    public final TaskSubmissionResponse submitUpdateByQueryTask(UpdateByQueryRequest updateByQueryRequest, RequestOptions options)
        throws IOException {
        return performRequestAndParseEntity(
            updateByQueryRequest,
            RequestConverters::submitUpdateByQuery,
            options,
            TaskSubmissionResponse::fromXContent,
            emptySet()
        );
    }

    /**
     * Asynchronously executes an update by query request.
     * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-update-by-query.html">
     *     Update By Query API on elastic.co</a>
     * @param updateByQueryRequest the request
     * @param options the request options (e.g. headers), use {@link RequestOptions#DEFAULT} if nothing needs to be customized
     * @param listener the listener to be notified upon request completion
     * @return cancellable that may be used to cancel the request
     */
    public final Cancellable updateByQueryAsync(
        UpdateByQueryRequest updateByQueryRequest,
        RequestOptions options,
        ActionListener<BulkByScrollResponse> listener
    ) {
        return performRequestAsyncAndParseEntity(
            updateByQueryRequest,
            RequestConverters::updateByQuery,
            options,
            BulkByScrollResponse::fromXContent,
            listener,
            singleton(409)
        );
    }

    /**
     * Executes a delete by query request.
     * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-delete-by-query.html">
     *     Delete By Query API on elastic.co</a>
     * @param deleteByQueryRequest the request
     * @param options the request options (e.g. headers), use {@link RequestOptions#DEFAULT} if nothing needs to be customized
     * @return the response
     */
    public final BulkByScrollResponse deleteByQuery(DeleteByQueryRequest deleteByQueryRequest, RequestOptions options) throws IOException {
        return performRequestAndParseEntity(
            deleteByQueryRequest,
            RequestConverters::deleteByQuery,
            options,
            BulkByScrollResponse::fromXContent,
            singleton(409)
        );
    }

    /**
     * Submits a delete by query task
     * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-delete-by-query.html">
     *      Delete By Query API on elastic.co</a>
     * @param deleteByQueryRequest the request
     * @param options the request options (e.g. headers), use {@link RequestOptions#DEFAULT} if nothing needs to be customized
     * @return the submission response
     */
    public final TaskSubmissionResponse submitDeleteByQueryTask(DeleteByQueryRequest deleteByQueryRequest, RequestOptions options)
        throws IOException {
        return performRequestAndParseEntity(
            deleteByQueryRequest,
            RequestConverters::submitDeleteByQuery,
            options,
            TaskSubmissionResponse::fromXContent,
            emptySet()
        );
    }

    /**
     * Asynchronously executes a delete by query request.
     * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-delete-by-query.html">
     *     Delete By Query API on elastic.co</a>
     * @param deleteByQueryRequest the request
     * @param options the request options (e.g. headers), use {@link RequestOptions#DEFAULT} if nothing needs to be customized
     * @param listener the listener to be notified upon request completion
     * @return cancellable that may be used to cancel the request
     */
    public final Cancellable deleteByQueryAsync(
        DeleteByQueryRequest deleteByQueryRequest,
        RequestOptions options,
        ActionListener<BulkByScrollResponse> listener
    ) {
        return performRequestAsyncAndParseEntity(
            deleteByQueryRequest,
            RequestConverters::deleteByQuery,
            options,
            BulkByScrollResponse::fromXContent,
            listener,
            singleton(409)
        );
    }

    /**
     * Executes a delete by query rethrottle request.
     * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-delete-by-query.html">
     *     Delete By Query API on elastic.co</a>
     * @param rethrottleRequest the request
     * @param options the request options (e.g. headers), use {@link RequestOptions#DEFAULT} if nothing needs to be customized
     * @return the response
     */
    public final ListTasksResponse deleteByQueryRethrottle(RethrottleRequest rethrottleRequest, RequestOptions options) throws IOException {
        return performRequestAndParseEntity(
            rethrottleRequest,
            RequestConverters::rethrottleDeleteByQuery,
            options,
            ListTasksResponse::fromXContent,
            emptySet()
        );
    }

    /**
     * Asynchronously execute an delete by query rethrottle request.
     * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-delete-by-query.html">
     *     Delete By Query API on elastic.co</a>
     * @param rethrottleRequest the request
     * @param options the request options (e.g. headers), use {@link RequestOptions#DEFAULT} if nothing needs to be customized
     * @param listener the listener to be notified upon request completion
     * @return cancellable that may be used to cancel the request
     */
    public final Cancellable deleteByQueryRethrottleAsync(
        RethrottleRequest rethrottleRequest,
        RequestOptions options,
        ActionListener<ListTasksResponse> listener
    ) {
        return performRequestAsyncAndParseEntity(
            rethrottleRequest,
            RequestConverters::rethrottleDeleteByQuery,
            options,
            ListTasksResponse::fromXContent,
            listener,
            emptySet()
        );
    }

    /**
     * Executes a update by query rethrottle request.
     * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-update-by-query.html">
     *     Update By Query API on elastic.co</a>
     * @param rethrottleRequest the request
     * @param options the request options (e.g. headers), use {@link RequestOptions#DEFAULT} if nothing needs to be customized
     * @return the response
     */
    public final ListTasksResponse updateByQueryRethrottle(RethrottleRequest rethrottleRequest, RequestOptions options) throws IOException {
        return performRequestAndParseEntity(
            rethrottleRequest,
            RequestConverters::rethrottleUpdateByQuery,
            options,
            ListTasksResponse::fromXContent,
            emptySet()
        );
    }

    /**
     * Asynchronously execute an update by query rethrottle request.
     * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-update-by-query.html">
     *     Update By Query API on elastic.co</a>
     * @param rethrottleRequest the request
     * @param options the request options (e.g. headers), use {@link RequestOptions#DEFAULT} if nothing needs to be customized
     * @param listener the listener to be notified upon request completion
     * @return cancellable that may be used to cancel the request
     */
    public final Cancellable updateByQueryRethrottleAsync(
        RethrottleRequest rethrottleRequest,
        RequestOptions options,
        ActionListener<ListTasksResponse> listener
    ) {
        return performRequestAsyncAndParseEntity(
            rethrottleRequest,
            RequestConverters::rethrottleUpdateByQuery,
            options,
            ListTasksResponse::fromXContent,
            listener,
            emptySet()
        );
    }

    /**
     * Executes a reindex rethrottling request.
     * See the <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-reindex.html#docs-reindex-rethrottle">
     * Reindex rethrottling API on elastic.co</a>
     *
     * @param rethrottleRequest the request
     * @param options           the request options (e.g. headers), use {@link RequestOptions#DEFAULT} if nothing needs to be customized
     * @return the response
     */
    public final ListTasksResponse reindexRethrottle(RethrottleRequest rethrottleRequest, RequestOptions options) throws IOException {
        return performRequestAndParseEntity(
            rethrottleRequest,
            RequestConverters::rethrottleReindex,
            options,
            ListTasksResponse::fromXContent,
            emptySet()
        );
    }

    /**
     * Executes a reindex rethrottling request.
     * See the <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-reindex.html#docs-reindex-rethrottle">
     * Reindex rethrottling API on elastic.co</a>
     * @param rethrottleRequest the request
     * @param options           the request options (e.g. headers), use {@link RequestOptions#DEFAULT} if nothing needs to be customized
     * @param listener          the listener to be notified upon request completion
     * @return cancellable that may be used to cancel the request
     */
    public final Cancellable reindexRethrottleAsync(
        RethrottleRequest rethrottleRequest,
        RequestOptions options,
        ActionListener<ListTasksResponse> listener
    ) {
        return performRequestAsyncAndParseEntity(
            rethrottleRequest,
            RequestConverters::rethrottleReindex,
            options,
            ListTasksResponse::fromXContent,
            listener,
            emptySet()
        );
    }

    /**
     * Pings the remote Elasticsearch cluster and returns true if the ping succeeded, false otherwise
     * @param options the request options (e.g. headers), use {@link RequestOptions#DEFAULT} if nothing needs to be customized
     * @return <code>true</code> if the ping succeeded, false otherwise
     */
    public final boolean ping(RequestOptions options) throws IOException {
        return performRequest(
            new MainRequest(),
            (request) -> RequestConverters.ping(),
            options,
            RestHighLevelClient::convertExistsResponse,
            emptySet()
        );
    }

    /**
     * Get the cluster info otherwise provided when sending an HTTP request to '/'
     * @param options the request options (e.g. headers), use {@link RequestOptions#DEFAULT} if nothing needs to be customized
     * @return the response
     */
    public final MainResponse info(RequestOptions options) throws IOException {
        return performRequestAndParseEntity(
            new MainRequest(),
            (request) -> RequestConverters.info(),
            options,
            MainResponse::fromXContent,
            emptySet()
        );
    }

    /**
     * Retrieves a document by id using the Get API.
     * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-get.html">Get API on elastic.co</a>
     * @param getRequest the request
     * @param options the request options (e.g. headers), use {@link RequestOptions#DEFAULT} if nothing needs to be customized
     * @return the response
     */
    public final GetResponse get(GetRequest getRequest, RequestOptions options) throws IOException {
        return performRequestAndParseEntity(getRequest, RequestConverters::get, options, GetResponse::fromXContent, singleton(404));
    }

    /**
     * Asynchronously retrieves a document by id using the Get API.
     * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-get.html">Get API on elastic.co</a>
     * @param getRequest the request
     * @param options the request options (e.g. headers), use {@link RequestOptions#DEFAULT} if nothing needs to be customized
     * @param listener the listener to be notified upon request completion
     * @return cancellable that may be used to cancel the request
     */
    public final Cancellable getAsync(GetRequest getRequest, RequestOptions options, ActionListener<GetResponse> listener) {
        return performRequestAsyncAndParseEntity(
            getRequest,
            RequestConverters::get,
            options,
            GetResponse::fromXContent,
            listener,
            singleton(404)
        );
    }

    /**
     * Retrieves multiple documents by id using the Multi Get API.
     * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-multi-get.html">Multi Get API on elastic.co</a>
     * @param multiGetRequest the request
     * @param options the request options (e.g. headers), use {@link RequestOptions#DEFAULT} if nothing needs to be customized
     * @return the response
     * @deprecated use {@link #mget(MultiGetRequest, RequestOptions)} instead
     */
    @Deprecated
    public final MultiGetResponse multiGet(MultiGetRequest multiGetRequest, RequestOptions options) throws IOException {
        return mget(multiGetRequest, options);
    }

    /**
     * Retrieves multiple documents by id using the Multi Get API.
     * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-multi-get.html">Multi Get API on elastic.co</a>
     * @param multiGetRequest the request
     * @param options the request options (e.g. headers), use {@link RequestOptions#DEFAULT} if nothing needs to be customized
     * @return the response
     */
    public final MultiGetResponse mget(MultiGetRequest multiGetRequest, RequestOptions options) throws IOException {
        return performRequestAndParseEntity(
            multiGetRequest,
            RequestConverters::multiGet,
            options,
            MultiGetResponse::fromXContent,
            singleton(404)
        );
    }

    /**
     * Asynchronously retrieves multiple documents by id using the Multi Get API.
     * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-multi-get.html">Multi Get API on elastic.co</a>
     * @param multiGetRequest the request
     * @param options the request options (e.g. headers), use {@link RequestOptions#DEFAULT} if nothing needs to be customized
     * @param listener the listener to be notified upon request completion
     * @deprecated use {@link #mgetAsync(MultiGetRequest, RequestOptions, ActionListener)} instead
     * @return cancellable that may be used to cancel the request
     */
    @Deprecated
    public final Cancellable multiGetAsync(
        MultiGetRequest multiGetRequest,
        RequestOptions options,
        ActionListener<MultiGetResponse> listener
    ) {
        return mgetAsync(multiGetRequest, options, listener);
    }

    /**
     * Asynchronously retrieves multiple documents by id using the Multi Get API.
     * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-multi-get.html">Multi Get API on elastic.co</a>
     * @param multiGetRequest the request
     * @param options the request options (e.g. headers), use {@link RequestOptions#DEFAULT} if nothing needs to be customized
     * @param listener the listener to be notified upon request completion
     * @return cancellable that may be used to cancel the request
     */
    public final Cancellable mgetAsync(MultiGetRequest multiGetRequest, RequestOptions options, ActionListener<MultiGetResponse> listener) {
        return performRequestAsyncAndParseEntity(
            multiGetRequest,
            RequestConverters::multiGet,
            options,
            MultiGetResponse::fromXContent,
            listener,
            singleton(404)
        );
    }

    /**
     * Checks for the existence of a document. Returns true if it exists, false otherwise.
     * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-get.html">Get API on elastic.co</a>
     * @param getRequest the request
     * @param options the request options (e.g. headers), use {@link RequestOptions#DEFAULT} if nothing needs to be customized
     * @return <code>true</code> if the document exists, <code>false</code> otherwise
     */
    public final boolean exists(GetRequest getRequest, RequestOptions options) throws IOException {
        return performRequest(getRequest, RequestConverters::exists, options, RestHighLevelClient::convertExistsResponse, emptySet());
    }

    /**
     * Asynchronously checks for the existence of a document. Returns true if it exists, false otherwise.
     * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-get.html">Get API on elastic.co</a>
     * @param getRequest the request
     * @param options the request options (e.g. headers), use {@link RequestOptions#DEFAULT} if nothing needs to be customized
     * @param listener the listener to be notified upon request completion
     * @return cancellable that may be used to cancel the request
     */
    public final Cancellable existsAsync(GetRequest getRequest, RequestOptions options, ActionListener<Boolean> listener) {
        return performRequestAsync(
            getRequest,
            RequestConverters::exists,
            options,
            RestHighLevelClient::convertExistsResponse,
            listener,
            emptySet()
        );
    }

    /**
     * Checks for the existence of a document with a "_source" field. Returns true if it exists, false otherwise.
     * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-get.html#_source">Source exists API
     * on elastic.co</a>
     * @param getRequest the request
     * @param options the request options (e.g. headers), use {@link RequestOptions#DEFAULT} if nothing needs to be customized
     * @return <code>true</code> if the document and _source field exists, <code>false</code> otherwise
     * @deprecated use {@link #existsSource(GetSourceRequest, RequestOptions)} instead
     */
    @Deprecated
    public boolean existsSource(GetRequest getRequest, RequestOptions options) throws IOException {
        GetSourceRequest getSourceRequest = GetSourceRequest.from(getRequest);
        return performRequest(
            getSourceRequest,
            RequestConverters::sourceExists,
            options,
            RestHighLevelClient::convertExistsResponse,
            emptySet()
        );
    }

    /**
     * Asynchronously checks for the existence of a document with a "_source" field. Returns true if it exists, false otherwise.
     * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-get.html#_source">Source exists API
     * on elastic.co</a>
     * @param getRequest the request
     * @param options the request options (e.g. headers), use {@link RequestOptions#DEFAULT} if nothing needs to be customized
     * @param listener the listener to be notified upon request completion
     * @return cancellable that may be used to cancel the request
     * @deprecated use {@link #existsSourceAsync(GetSourceRequest, RequestOptions, ActionListener)} instead
     */
    @Deprecated
    public final Cancellable existsSourceAsync(GetRequest getRequest, RequestOptions options, ActionListener<Boolean> listener) {
        GetSourceRequest getSourceRequest = GetSourceRequest.from(getRequest);
        return performRequestAsync(
            getSourceRequest,
            RequestConverters::sourceExists,
            options,
            RestHighLevelClient::convertExistsResponse,
            listener,
            emptySet()
        );
    }

    /**
     * Checks for the existence of a document with a "_source" field. Returns true if it exists, false otherwise.
     * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-get.html#_source">Source exists API
     * on elastic.co</a>
     * @param getSourceRequest the request
     * @param options the request options (e.g. headers), use {@link RequestOptions#DEFAULT} if nothing needs to be customized
     * @return <code>true</code> if the document and _source field exists, <code>false</code> otherwise
     */
    public boolean existsSource(GetSourceRequest getSourceRequest, RequestOptions options) throws IOException {
        return performRequest(
            getSourceRequest,
            RequestConverters::sourceExists,
            options,
            RestHighLevelClient::convertExistsResponse,
            emptySet()
        );
    }

    /**
     * Asynchronously checks for the existence of a document with a "_source" field. Returns true if it exists, false otherwise.
     * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-get.html#_source">Source exists API
     * on elastic.co</a>
     * @param getSourceRequest the request
     * @param options the request options (e.g. headers), use {@link RequestOptions#DEFAULT} if nothing needs to be customized
     * @param listener the listener to be notified upon request completion
     * @return cancellable that may be used to cancel the request
     */
    public final Cancellable existsSourceAsync(
        GetSourceRequest getSourceRequest,
        RequestOptions options,
        ActionListener<Boolean> listener
    ) {
        return performRequestAsync(
            getSourceRequest,
            RequestConverters::sourceExists,
            options,
            RestHighLevelClient::convertExistsResponse,
            listener,
            emptySet()
        );
    }

    /**
     * Retrieves the source field only of a document using GetSource API.
     * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-get.html#_source">Get Source API
     * on elastic.co</a>
     * @param getSourceRequest the request
     * @param options the request options (e.g. headers), use {@link RequestOptions#DEFAULT} if nothing needs to be customized
     * @return the response
     */
    public GetSourceResponse getSource(GetSourceRequest getSourceRequest, RequestOptions options) throws IOException {
        return performRequestAndParseEntity(
            getSourceRequest,
            RequestConverters::getSource,
            options,
            GetSourceResponse::fromXContent,
            emptySet()
        );
    }

    /**
     * Asynchronously retrieves the source field only of a document using GetSource API.
     * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-get.html#_source">Get Source API
     * on elastic.co</a>
     * @param getSourceRequest the request
     * @param options the request options (e.g. headers), use {@link RequestOptions#DEFAULT} if nothing needs to be customized
     * @param listener the listener to be notified upon request completion
     * @return cancellable that may be used to cancel the request
     */
    public final Cancellable getSourceAsync(
        GetSourceRequest getSourceRequest,
        RequestOptions options,
        ActionListener<GetSourceResponse> listener
    ) {
        return performRequestAsyncAndParseEntity(
            getSourceRequest,
            RequestConverters::getSource,
            options,
            GetSourceResponse::fromXContent,
            listener,
            emptySet()
        );
    }

    /**
     * Index a document using the Index API.
     * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-index_.html">Index API on elastic.co</a>
     * @param indexRequest the request
     * @param options the request options (e.g. headers), use {@link RequestOptions#DEFAULT} if nothing needs to be customized
     * @return the response
     */
    public final IndexResponse index(IndexRequest indexRequest, RequestOptions options) throws IOException {
        return performRequestAndParseEntity(indexRequest, RequestConverters::index, options, IndexResponse::fromXContent, emptySet());
    }

    /**
     * Asynchronously index a document using the Index API.
     * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-index_.html">Index API on elastic.co</a>
     * @param indexRequest the request
     * @param options the request options (e.g. headers), use {@link RequestOptions#DEFAULT} if nothing needs to be customized
     * @param listener the listener to be notified upon request completion
     * @return cancellable that may be used to cancel the request
     */
    public final Cancellable indexAsync(IndexRequest indexRequest, RequestOptions options, ActionListener<IndexResponse> listener) {
        return performRequestAsyncAndParseEntity(
            indexRequest,
            RequestConverters::index,
            options,
            IndexResponse::fromXContent,
            listener,
            emptySet()
        );
    }

    /**
     * Executes a count request using the Count API.
     * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/search-count.html">Count API on elastic.co</a>
     * @param countRequest the request
     * @param options the request options (e.g. headers), use {@link RequestOptions#DEFAULT} if nothing needs to be customized
     * @return the response
     */
    public final CountResponse count(CountRequest countRequest, RequestOptions options) throws IOException {
        return performRequestAndParseEntity(countRequest, RequestConverters::count, options, CountResponse::fromXContent, emptySet());
    }

    /**
     * Asynchronously executes a count request using the Count API.
     * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/search-count.html">Count API on elastic.co</a>
     * @param countRequest the request
     * @param options the request options (e.g. headers), use {@link RequestOptions#DEFAULT} if nothing needs to be customized
     * @param listener the listener to be notified upon request completion
     * @return cancellable that may be used to cancel the request
     */
    public final Cancellable countAsync(CountRequest countRequest, RequestOptions options, ActionListener<CountResponse> listener) {
        return performRequestAsyncAndParseEntity(
            countRequest,
            RequestConverters::count,
            options,
            CountResponse::fromXContent,
            listener,
            emptySet()
        );
    }

    /**
     * Updates a document using the Update API.
     * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-update.html">Update API on elastic.co</a>
     * @param updateRequest the request
     * @param options the request options (e.g. headers), use {@link RequestOptions#DEFAULT} if nothing needs to be customized
     * @return the response
     */
    public final UpdateResponse update(UpdateRequest updateRequest, RequestOptions options) throws IOException {
        return performRequestAndParseEntity(updateRequest, RequestConverters::update, options, UpdateResponse::fromXContent, emptySet());
    }

    /**
     * Asynchronously updates a document using the Update API.
     * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-update.html">Update API on elastic.co</a>
     * @param updateRequest the request
     * @param options the request options (e.g. headers), use {@link RequestOptions#DEFAULT} if nothing needs to be customized
     * @param listener the listener to be notified upon request completion
     * @return cancellable that may be used to cancel the request
     */
    public final Cancellable updateAsync(UpdateRequest updateRequest, RequestOptions options, ActionListener<UpdateResponse> listener) {
        return performRequestAsyncAndParseEntity(
            updateRequest,
            RequestConverters::update,
            options,
            UpdateResponse::fromXContent,
            listener,
            emptySet()
        );
    }

    /**
     * Deletes a document by id using the Delete API.
     * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-delete.html">Delete API on elastic.co</a>
     * @param deleteRequest the request
     * @param options the request options (e.g. headers), use {@link RequestOptions#DEFAULT} if nothing needs to be customized
     * @return the response
     */
    public final DeleteResponse delete(DeleteRequest deleteRequest, RequestOptions options) throws IOException {
        return performRequestAndParseEntity(
            deleteRequest,
            RequestConverters::delete,
            options,
            DeleteResponse::fromXContent,
            singleton(404)
        );
    }

    /**
     * Asynchronously deletes a document by id using the Delete API.
     * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-delete.html">Delete API on elastic.co</a>
     * @param deleteRequest the request
     * @param options the request options (e.g. headers), use {@link RequestOptions#DEFAULT} if nothing needs to be customized
     * @param listener the listener to be notified upon request completion
     * @return cancellable that may be used to cancel the request
     */
    public final Cancellable deleteAsync(DeleteRequest deleteRequest, RequestOptions options, ActionListener<DeleteResponse> listener) {
        return performRequestAsyncAndParseEntity(
            deleteRequest,
            RequestConverters::delete,
            options,
            DeleteResponse::fromXContent,
            listener,
            Collections.singleton(404)
        );
    }

    /**
     * Executes a search request using the Search API.
     * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/search-search.html">Search API on elastic.co</a>
     * @param searchRequest the request
     * @param options the request options (e.g. headers), use {@link RequestOptions#DEFAULT} if nothing needs to be customized
     * @return the response
     */
    public final SearchResponse search(SearchRequest searchRequest, RequestOptions options) throws IOException {
        return performRequestAndParseEntity(
            searchRequest,
            r -> RequestConverters.search(r, "_search"),
            options,
            SearchResponse::fromXContent,
            emptySet()
        );
    }

    /**
     * Asynchronously executes a search using the Search API.
     * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/search-search.html">Search API on elastic.co</a>
     * @param searchRequest the request
     * @param options the request options (e.g. headers), use {@link RequestOptions#DEFAULT} if nothing needs to be customized
     * @param listener the listener to be notified upon request completion
     * @return cancellable that may be used to cancel the request
     */
    public final Cancellable searchAsync(SearchRequest searchRequest, RequestOptions options, ActionListener<SearchResponse> listener) {
        return performRequestAsyncAndParseEntity(
            searchRequest,
            r -> RequestConverters.search(r, "_search"),
            options,
            SearchResponse::fromXContent,
            listener,
            emptySet()
        );
    }

    /**
     * Executes a multi search using the msearch API.
     * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/search-multi-search.html">Multi search API on
     * elastic.co</a>
     * @param multiSearchRequest the request
     * @param options the request options (e.g. headers), use {@link RequestOptions#DEFAULT} if nothing needs to be customized
     * @return the response
     * @deprecated use {@link #msearch(MultiSearchRequest, RequestOptions)} instead
     */
    @Deprecated
    public final MultiSearchResponse multiSearch(MultiSearchRequest multiSearchRequest, RequestOptions options) throws IOException {
        return msearch(multiSearchRequest, options);
    }

    /**
     * Executes a multi search using the msearch API.
     * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/search-multi-search.html">Multi search API on
     * elastic.co</a>
     * @param multiSearchRequest the request
     * @param options the request options (e.g. headers), use {@link RequestOptions#DEFAULT} if nothing needs to be customized
     * @return the response
     */
    public final MultiSearchResponse msearch(MultiSearchRequest multiSearchRequest, RequestOptions options) throws IOException {
        return performRequestAndParseEntity(
            multiSearchRequest,
            RequestConverters::multiSearch,
            options,
            MultiSearchResponse::fromXContext,
            emptySet()
        );
    }

    /**
     * Asynchronously executes a multi search using the msearch API.
     * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/search-multi-search.html">Multi search API on
     * elastic.co</a>
     * @param searchRequest the request
     * @param options the request options (e.g. headers), use {@link RequestOptions#DEFAULT} if nothing needs to be customized
     * @param listener the listener to be notified upon request completion
     * @deprecated use {@link #msearchAsync(MultiSearchRequest, RequestOptions, ActionListener)} instead
     * @return cancellable that may be used to cancel the request
     */
    @Deprecated
    public final Cancellable multiSearchAsync(
        MultiSearchRequest searchRequest,
        RequestOptions options,
        ActionListener<MultiSearchResponse> listener
    ) {
        return msearchAsync(searchRequest, options, listener);
    }

    /**
     * Asynchronously executes a multi search using the msearch API.
     * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/search-multi-search.html">Multi search API on
     * elastic.co</a>
     * @param searchRequest the request
     * @param options the request options (e.g. headers), use {@link RequestOptions#DEFAULT} if nothing needs to be customized
     * @param listener the listener to be notified upon request completion
     * @return cancellable that may be used to cancel the request
     */
    public final Cancellable msearchAsync(
        MultiSearchRequest searchRequest,
        RequestOptions options,
        ActionListener<MultiSearchResponse> listener
    ) {
        return performRequestAsyncAndParseEntity(
            searchRequest,
            RequestConverters::multiSearch,
            options,
            MultiSearchResponse::fromXContext,
            listener,
            emptySet()
        );
    }

    /**
     * Executes a search using the Search Scroll API.
     * See <a
     * href="https://www.elastic.co/guide/en/elasticsearch/reference/master/search-request-body.html#request-body-search-scroll">Search
     * Scroll API on elastic.co</a>
     * @param searchScrollRequest the request
     * @param options the request options (e.g. headers), use {@link RequestOptions#DEFAULT} if nothing needs to be customized
     * @return the response
     * @deprecated use {@link #scroll(SearchScrollRequest, RequestOptions)} instead
     */
    @Deprecated
    public final SearchResponse searchScroll(SearchScrollRequest searchScrollRequest, RequestOptions options) throws IOException {
        return scroll(searchScrollRequest, options);
    }

    /**
     * Executes a search using the Search Scroll API.
     * See <a
     * href="https://www.elastic.co/guide/en/elasticsearch/reference/master/search-request-body.html#request-body-search-scroll">Search
     * Scroll API on elastic.co</a>
     * @param searchScrollRequest the request
     * @param options the request options (e.g. headers), use {@link RequestOptions#DEFAULT} if nothing needs to be customized
     * @return the response
     */
    public final SearchResponse scroll(SearchScrollRequest searchScrollRequest, RequestOptions options) throws IOException {
        return performRequestAndParseEntity(
            searchScrollRequest,
            RequestConverters::searchScroll,
            options,
            SearchResponse::fromXContent,
            emptySet()
        );
    }

    /**
     * Asynchronously executes a search using the Search Scroll API.
     * See <a
     * href="https://www.elastic.co/guide/en/elasticsearch/reference/master/search-request-body.html#request-body-search-scroll">Search
     * Scroll API on elastic.co</a>
     * @param searchScrollRequest the request
     * @param options the request options (e.g. headers), use {@link RequestOptions#DEFAULT} if nothing needs to be customized
     * @param listener the listener to be notified upon request completion
     * @deprecated use {@link #scrollAsync(SearchScrollRequest, RequestOptions, ActionListener)} instead
     * @return cancellable that may be used to cancel the request
     */
    @Deprecated
    public final Cancellable searchScrollAsync(
        SearchScrollRequest searchScrollRequest,
        RequestOptions options,
        ActionListener<SearchResponse> listener
    ) {
        return scrollAsync(searchScrollRequest, options, listener);
    }

    /**
     * Asynchronously executes a search using the Search Scroll API.
     * See <a
     * href="https://www.elastic.co/guide/en/elasticsearch/reference/master/search-request-body.html#request-body-search-scroll">Search
     * Scroll API on elastic.co</a>
     * @param searchScrollRequest the request
     * @param options the request options (e.g. headers), use {@link RequestOptions#DEFAULT} if nothing needs to be customized
     * @param listener the listener to be notified upon request completion
     * @return cancellable that may be used to cancel the request
     */
    public final Cancellable scrollAsync(
        SearchScrollRequest searchScrollRequest,
        RequestOptions options,
        ActionListener<SearchResponse> listener
    ) {
        return performRequestAsyncAndParseEntity(
            searchScrollRequest,
            RequestConverters::searchScroll,
            options,
            SearchResponse::fromXContent,
            listener,
            emptySet()
        );
    }

    /**
     * Clears one or more scroll ids using the Clear Scroll API.
     * See <a
     * href="https://www.elastic.co/guide/en/elasticsearch/reference/master/search-request-body.html#_clear_scroll_api">
     * Clear Scroll API on elastic.co</a>
     * @param clearScrollRequest the request
     * @param options the request options (e.g. headers), use {@link RequestOptions#DEFAULT} if nothing needs to be customized
     * @return the response
     */
    public final ClearScrollResponse clearScroll(ClearScrollRequest clearScrollRequest, RequestOptions options) throws IOException {
        return performRequestAndParseEntity(
            clearScrollRequest,
            RequestConverters::clearScroll,
            options,
            ClearScrollResponse::fromXContent,
            emptySet()
        );
    }

    /**
     * Asynchronously clears one or more scroll ids using the Clear Scroll API.
     * See <a
     * href="https://www.elastic.co/guide/en/elasticsearch/reference/master/search-request-body.html#_clear_scroll_api">
     * Clear Scroll API on elastic.co</a>
     * @param clearScrollRequest the request
     * @param options the request options (e.g. headers), use {@link RequestOptions#DEFAULT} if nothing needs to be customized
     * @param listener the listener to be notified upon request completion
     * @return cancellable that may be used to cancel the request
     */
    public final Cancellable clearScrollAsync(
        ClearScrollRequest clearScrollRequest,
        RequestOptions options,
        ActionListener<ClearScrollResponse> listener
    ) {
        return performRequestAsyncAndParseEntity(
            clearScrollRequest,
            RequestConverters::clearScroll,
            options,
            ClearScrollResponse::fromXContent,
            listener,
            emptySet()
        );
    }

    /**
     * Open a point in time before using it in search requests.
     * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/master/point-in-time-api.html"> Point in time API </a>
     * @param openRequest the open request
     * @param options the request options (e.g. headers), use {@link RequestOptions#DEFAULT} if nothing needs to be customized
     * @return the response containing the point in time id
     */
    public final OpenPointInTimeResponse openPointInTime(OpenPointInTimeRequest openRequest, RequestOptions options) throws IOException {
        return performRequestAndParseEntity(
            openRequest,
            RequestConverters::openPointInTime,
            options,
            OpenPointInTimeResponse::fromXContent,
            emptySet()
        );
    }

    /**
     * Asynchronously open a point in time before using it in search requests
     * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/master/point-in-time-api.html"> Point in time API </a>
     * @param openRequest the open request
     * @param options the request options (e.g. headers), use {@link RequestOptions#DEFAULT} if nothing needs to be customized
     * @param listener the listener to be notified upon request completion
     * @return a cancellable that may be used to cancel the request
     */
    public final Cancellable openPointInTimeAsync(
        OpenPointInTimeRequest openRequest,
        RequestOptions options,
        ActionListener<OpenPointInTimeResponse> listener
    ) {
        return performRequestAsyncAndParseEntity(
            openRequest,
            RequestConverters::openPointInTime,
            options,
            OpenPointInTimeResponse::fromXContent,
            listener,
            emptySet()
        );
    }

    /**
     * Close a point in time that is opened with {@link #openPointInTime(OpenPointInTimeRequest, RequestOptions)} or
     * {@link #openPointInTimeAsync(OpenPointInTimeRequest, RequestOptions, ActionListener)}.
     * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/master/point-in-time-api.html#close-point-in-time-api">
     * Close point in time API</a>
     * @param closeRequest the close request
     * @param options the request options (e.g. headers), use {@link RequestOptions#DEFAULT} if nothing needs to be customized
     * @return the response
     */
    public final ClosePointInTimeResponse closePointInTime(ClosePointInTimeRequest closeRequest, RequestOptions options)
        throws IOException {
        return performRequestAndParseEntity(
            closeRequest,
            RequestConverters::closePointInTime,
            options,
            ClosePointInTimeResponse::fromXContent,
            emptySet()
        );
    }

    /**
     * Asynchronously close a point in time that is opened with {@link #openPointInTime(OpenPointInTimeRequest, RequestOptions)} or
     * {@link #openPointInTimeAsync(OpenPointInTimeRequest, RequestOptions, ActionListener)}.
     * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/master/point-in-time-api.html#close-point-in-time-api">
     * Close point in time API</a>
     * @param closeRequest the close request
     * @param options the request options (e.g. headers), use {@link RequestOptions#DEFAULT} if nothing needs to be customized
     * @param listener the listener to be notified upon request completion
     * @return a cancellable that may be used to cancel the request
     */
    public final Cancellable closePointInTimeAsync(
        ClosePointInTimeRequest closeRequest,
        RequestOptions options,
        ActionListener<ClosePointInTimeResponse> listener
    ) {
        return performRequestAsyncAndParseEntity(
            closeRequest,
            RequestConverters::closePointInTime,
            options,
            ClosePointInTimeResponse::fromXContent,
            listener,
            emptySet()
        );
    }

    /**
     * Executes a request using the Search Template API.
     * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/search-template.html">Search Template API
     * on elastic.co</a>.
     * @param searchTemplateRequest the request
     * @param options the request options (e.g. headers), use {@link RequestOptions#DEFAULT} if nothing needs to be customized
     * @return the response
     */
    public final SearchTemplateResponse searchTemplate(SearchTemplateRequest searchTemplateRequest, RequestOptions options)
        throws IOException {
        return performRequestAndParseEntity(
            searchTemplateRequest,
            RequestConverters::searchTemplate,
            options,
            SearchTemplateResponse::fromXContent,
            emptySet()
        );
    }

    /**
     * Asynchronously executes a request using the Search Template API.
     *
     * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/search-template.html">Search Template API
     * on elastic.co</a>.
     * @return cancellable that may be used to cancel the request
     */
    public final Cancellable searchTemplateAsync(
        SearchTemplateRequest searchTemplateRequest,
        RequestOptions options,
        ActionListener<SearchTemplateResponse> listener
    ) {
        return performRequestAsyncAndParseEntity(
            searchTemplateRequest,
            RequestConverters::searchTemplate,
            options,
            SearchTemplateResponse::fromXContent,
            listener,
            emptySet()
        );
    }

    /**
     * Executes a request using the Explain API.
     * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/search-explain.html">Explain API on elastic.co</a>
     * @param explainRequest the request
     * @param options the request options (e.g. headers), use {@link RequestOptions#DEFAULT} if nothing needs to be customized
     * @return the response
     */
    public final ExplainResponse explain(ExplainRequest explainRequest, RequestOptions options) throws IOException {
        return performRequest(explainRequest, RequestConverters::explain, options, response -> {
            CheckedFunction<XContentParser, ExplainResponse, IOException> entityParser = parser -> ExplainResponse.fromXContent(
                parser,
                convertExistsResponse(response)
            );
            return parseEntity(response.getEntity(), entityParser);
        }, singleton(404));
    }

    /**
     * Asynchronously executes a request using the Explain API.
     *
     * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/search-explain.html">Explain API on elastic.co</a>
     * @param explainRequest the request
     * @param options the request options (e.g. headers), use {@link RequestOptions#DEFAULT} if nothing needs to be customized
     * @param listener the listener to be notified upon request completion
     * @return cancellable that may be used to cancel the request
     */
    public final Cancellable explainAsync(ExplainRequest explainRequest, RequestOptions options, ActionListener<ExplainResponse> listener) {
        return performRequestAsync(explainRequest, RequestConverters::explain, options, response -> {
            CheckedFunction<XContentParser, ExplainResponse, IOException> entityParser = parser -> ExplainResponse.fromXContent(
                parser,
                convertExistsResponse(response)
            );
            return parseEntity(response.getEntity(), entityParser);
        }, listener, singleton(404));
    }

    /**
     * Calls the Term Vectors API
     *
     * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-termvectors.html">Term Vectors API on
     * elastic.co</a>
     *
     * @param request   the request
     * @param options   the request options (e.g. headers), use {@link RequestOptions#DEFAULT} if nothing needs to be customized
     */
    public final TermVectorsResponse termvectors(TermVectorsRequest request, RequestOptions options) throws IOException {
        return performRequestAndParseEntity(
            request,
            RequestConverters::termVectors,
            options,
            TermVectorsResponse::fromXContent,
            emptySet()
        );
    }

    /**
     * Asynchronously calls the Term Vectors API
     *
     * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-termvectors.html">Term Vectors API on
     * elastic.co</a>
     * @param request the request
     * @param options the request options (e.g. headers), use {@link RequestOptions#DEFAULT} if nothing needs to be customized
     * @param listener the listener to be notified upon request completion
     * @return cancellable that may be used to cancel the request
     */
    public final Cancellable termvectorsAsync(
        TermVectorsRequest request,
        RequestOptions options,
        ActionListener<TermVectorsResponse> listener
    ) {
        return performRequestAsyncAndParseEntity(
            request,
            RequestConverters::termVectors,
            options,
            TermVectorsResponse::fromXContent,
            listener,
            emptySet()
        );
    }

    /**
     * Calls the Multi Term Vectors API
     *
     * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-multi-termvectors.html">Multi Term Vectors API
     * on elastic.co</a>
     *
     * @param request   the request
     * @param options   the request options (e.g. headers), use {@link RequestOptions#DEFAULT} if nothing needs to be customized
     */
    public final MultiTermVectorsResponse mtermvectors(MultiTermVectorsRequest request, RequestOptions options) throws IOException {
        return performRequestAndParseEntity(
            request,
            RequestConverters::mtermVectors,
            options,
            MultiTermVectorsResponse::fromXContent,
            emptySet()
        );
    }

    /**
     * Asynchronously calls the Multi Term Vectors API
     *
     * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-multi-termvectors.html">Multi Term Vectors API
     * on elastic.co</a>
     * @param request the request
     * @param options the request options (e.g. headers), use {@link RequestOptions#DEFAULT} if nothing needs to be customized
     * @param listener the listener to be notified upon request completion
     * @return cancellable that may be used to cancel the request
     */
    public final Cancellable mtermvectorsAsync(
        MultiTermVectorsRequest request,
        RequestOptions options,
        ActionListener<MultiTermVectorsResponse> listener
    ) {
        return performRequestAsyncAndParseEntity(
            request,
            RequestConverters::mtermVectors,
            options,
            MultiTermVectorsResponse::fromXContent,
            listener,
            emptySet()
        );
    }

    /**
     * Executes a request using the Ranking Evaluation API.
     * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/search-rank-eval.html">Ranking Evaluation API
     * on elastic.co</a>
     * @param rankEvalRequest the request
     * @param options the request options (e.g. headers), use {@link RequestOptions#DEFAULT} if nothing needs to be customized
     * @return the response
     */
    public final RankEvalResponse rankEval(RankEvalRequest rankEvalRequest, RequestOptions options) throws IOException {
        return performRequestAndParseEntity(
            rankEvalRequest,
            RequestConverters::rankEval,
            options,
            RankEvalResponse::fromXContent,
            emptySet()
        );
    }

    /**
     * Executes a request using the Multi Search Template API.
     *
     * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/multi-search-template.html">Multi Search Template API
     * on elastic.co</a>.
     */
    public final MultiSearchTemplateResponse msearchTemplate(MultiSearchTemplateRequest multiSearchTemplateRequest, RequestOptions options)
        throws IOException {
        return performRequestAndParseEntity(
            multiSearchTemplateRequest,
            RequestConverters::multiSearchTemplate,
            options,
            MultiSearchTemplateResponse::fromXContext,
            emptySet()
        );
    }

    /**
     * Asynchronously executes a request using the Multi Search Template API
     *
     * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/multi-search-template.html">Multi Search Template API
     * on elastic.co</a>.
     * @return cancellable that may be used to cancel the request
     */
    public final Cancellable msearchTemplateAsync(
        MultiSearchTemplateRequest multiSearchTemplateRequest,
        RequestOptions options,
        ActionListener<MultiSearchTemplateResponse> listener
    ) {
        return performRequestAsyncAndParseEntity(
            multiSearchTemplateRequest,
            RequestConverters::multiSearchTemplate,
            options,
            MultiSearchTemplateResponse::fromXContext,
            listener,
            emptySet()
        );
    }

    /**
     * Asynchronously executes a request using the Ranking Evaluation API.
     * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/search-rank-eval.html">Ranking Evaluation API
     * on elastic.co</a>
     * @param rankEvalRequest the request
     * @param options the request options (e.g. headers), use {@link RequestOptions#DEFAULT} if nothing needs to be customized
     * @param listener the listener to be notified upon request completion
     * @return cancellable that may be used to cancel the request
     */
    public final Cancellable rankEvalAsync(
        RankEvalRequest rankEvalRequest,
        RequestOptions options,
        ActionListener<RankEvalResponse> listener
    ) {
        return performRequestAsyncAndParseEntity(
            rankEvalRequest,
            RequestConverters::rankEval,
            options,
            RankEvalResponse::fromXContent,
            listener,
            emptySet()
        );
    }

    /**
     * Executes a request using the Field Capabilities API.
     * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/search-field-caps.html">Field Capabilities API
     * on elastic.co</a>.
     * @param fieldCapabilitiesRequest the request
     * @param options the request options (e.g. headers), use {@link RequestOptions#DEFAULT} if nothing needs to be customized
     * @return the response
     */
    public final FieldCapabilitiesResponse fieldCaps(FieldCapabilitiesRequest fieldCapabilitiesRequest, RequestOptions options)
        throws IOException {
        return performRequestAndParseEntity(
            fieldCapabilitiesRequest,
            RequestConverters::fieldCaps,
            options,
            FieldCapabilitiesResponse::fromXContent,
            emptySet()
        );
    }

    /**
     * Get stored script by id.
     * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/modules-scripting-using.html">
     *     How to use scripts on elastic.co</a>
     * @param request the request
     * @param options the request options (e.g. headers), use {@link RequestOptions#DEFAULT} if nothing needs to be customized
     * @return the response
     */
    public GetStoredScriptResponse getScript(GetStoredScriptRequest request, RequestOptions options) throws IOException {
        return performRequestAndParseEntity(
            request,
            RequestConverters::getScript,
            options,
            GetStoredScriptResponse::fromXContent,
            emptySet()
        );
    }

    /**
     * Asynchronously get stored script by id.
     * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/modules-scripting-using.html">
     *     How to use scripts on elastic.co</a>
     * @param request the request
     * @param options the request options (e.g. headers), use {@link RequestOptions#DEFAULT} if nothing needs to be customized
     * @param listener the listener to be notified upon request completion
     * @return cancellable that may be used to cancel the request
     */
    public Cancellable getScriptAsync(
        GetStoredScriptRequest request,
        RequestOptions options,
        ActionListener<GetStoredScriptResponse> listener
    ) {
        return performRequestAsyncAndParseEntity(
            request,
            RequestConverters::getScript,
            options,
            GetStoredScriptResponse::fromXContent,
            listener,
            emptySet()
        );
    }

    /**
     * Delete stored script by id.
     * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/modules-scripting-using.html">
     *     How to use scripts on elastic.co</a>
     * @param request the request
     * @param options the request options (e.g. headers), use {@link RequestOptions#DEFAULT} if nothing needs to be customized
     * @return the response
     */
    public AcknowledgedResponse deleteScript(DeleteStoredScriptRequest request, RequestOptions options) throws IOException {
        return performRequestAndParseEntity(
            request,
            RequestConverters::deleteScript,
            options,
            AcknowledgedResponse::fromXContent,
            emptySet()
        );
    }

    /**
     * Asynchronously delete stored script by id.
     * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/modules-scripting-using.html">
     *     How to use scripts on elastic.co</a>
     * @param request the request
     * @param options the request options (e.g. headers), use {@link RequestOptions#DEFAULT} if nothing needs to be customized
     * @param listener the listener to be notified upon request completion
     * @return cancellable that may be used to cancel the request
     */
    public Cancellable deleteScriptAsync(
        DeleteStoredScriptRequest request,
        RequestOptions options,
        ActionListener<AcknowledgedResponse> listener
    ) {
        return performRequestAsyncAndParseEntity(
            request,
            RequestConverters::deleteScript,
            options,
            AcknowledgedResponse::fromXContent,
            listener,
            emptySet()
        );
    }

    /**
     * Puts an stored script using the Scripting API.
     * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/modules-scripting-using.html"> Scripting API
     * on elastic.co</a>
     * @param putStoredScriptRequest the request
     * @param options the request options (e.g. headers), use {@link RequestOptions#DEFAULT} if nothing needs to be customized
     * @return the response
     */
    public AcknowledgedResponse putScript(PutStoredScriptRequest putStoredScriptRequest, RequestOptions options) throws IOException {
        return performRequestAndParseEntity(
            putStoredScriptRequest,
            RequestConverters::putScript,
            options,
            AcknowledgedResponse::fromXContent,
            emptySet()
        );
    }

    /**
     * Asynchronously puts an stored script using the Scripting API.
     * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/modules-scripting-using.html"> Scripting API
     * on elastic.co</a>
     * @param putStoredScriptRequest the request
     * @param options the request options (e.g. headers), use {@link RequestOptions#DEFAULT} if nothing needs to be customized
     * @param listener the listener to be notified upon request completion
     * @return cancellable that may be used to cancel the request
     */
    public Cancellable putScriptAsync(
        PutStoredScriptRequest putStoredScriptRequest,
        RequestOptions options,
        ActionListener<AcknowledgedResponse> listener
    ) {
        return performRequestAsyncAndParseEntity(
            putStoredScriptRequest,
            RequestConverters::putScript,
            options,
            AcknowledgedResponse::fromXContent,
            listener,
            emptySet()
        );
    }

    /**
     * Asynchronously executes a request using the Field Capabilities API.
     * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/search-field-caps.html">Field Capabilities API
     * on elastic.co</a>.
     * @param fieldCapabilitiesRequest the request
     * @param options the request options (e.g. headers), use {@link RequestOptions#DEFAULT} if nothing needs to be customized
     * @param listener the listener to be notified upon request completion
     * @return cancellable that may be used to cancel the request
     */
    public final Cancellable fieldCapsAsync(
        FieldCapabilitiesRequest fieldCapabilitiesRequest,
        RequestOptions options,
        ActionListener<FieldCapabilitiesResponse> listener
    ) {
        return performRequestAsyncAndParseEntity(
            fieldCapabilitiesRequest,
            RequestConverters::fieldCaps,
            options,
            FieldCapabilitiesResponse::fromXContent,
            listener,
            emptySet()
        );
    }

    /**
     * @deprecated If creating a new HLRC ReST API call, consider creating new actions instead of reusing server actions. The Validation
     * layer has been added to the ReST client, and requests should extend {@link Validatable} instead of {@link ActionRequest}.
     */
    @Deprecated
    protected final <Req extends ActionRequest, Resp> Resp performRequestAndParseEntity(
        Req request,
        CheckedFunction<Req, Request, IOException> requestConverter,
        RequestOptions options,
        CheckedFunction<XContentParser, Resp, IOException> entityParser,
        Set<Integer> ignores
    ) throws IOException {
        return performRequest(request, requestConverter, options, response -> parseEntity(response.getEntity(), entityParser), ignores);
    }

    /**
     * Defines a helper method for performing a request and then parsing the returned entity using the provided entityParser.
     */
    protected final <Req extends Validatable, Resp> Resp performRequestAndParseEntity(
        Req request,
        CheckedFunction<Req, Request, IOException> requestConverter,
        RequestOptions options,
        CheckedFunction<XContentParser, Resp, IOException> entityParser,
        Set<Integer> ignores
    ) throws IOException {
        return performRequest(request, requestConverter, options, response -> parseEntity(response.getEntity(), entityParser), ignores);
    }

    /**
     * @deprecated If creating a new HLRC ReST API call, consider creating new actions instead of reusing server actions. The Validation
     * layer has been added to the ReST client, and requests should extend {@link Validatable} instead of {@link ActionRequest}.
     */
    @Deprecated
    protected final <Req extends ActionRequest, Resp> Resp performRequest(
        Req request,
        CheckedFunction<Req, Request, IOException> requestConverter,
        RequestOptions options,
        CheckedFunction<Response, Resp, IOException> responseConverter,
        Set<Integer> ignores
    ) throws IOException {
        ActionRequestValidationException validationException = request.validate();
        if (validationException != null && validationException.validationErrors().isEmpty() == false) {
            throw validationException;
        }
        return internalPerformRequest(request, requestConverter, options, responseConverter, ignores);
    }

    /**
     * Defines a helper method for performing a request.
     */
    protected final <Req extends Validatable, Resp> Resp performRequest(
        Req request,
        CheckedFunction<Req, Request, IOException> requestConverter,
        RequestOptions options,
        CheckedFunction<Response, Resp, IOException> responseConverter,
        Set<Integer> ignores
    ) throws IOException {
        Optional<ValidationException> validationException = request.validate();
        if (validationException != null && validationException.isPresent()) {
            throw validationException.get();
        }
        return internalPerformRequest(request, requestConverter, options, responseConverter, ignores);
    }

    /**
     * Provides common functionality for performing a request.
     */
    private <Req, Resp> Resp internalPerformRequest(
        Req request,
        CheckedFunction<Req, Request, IOException> requestConverter,
        RequestOptions options,
        CheckedFunction<Response, Resp, IOException> responseConverter,
        Set<Integer> ignores
    ) throws IOException {
        Request req = requestConverter.apply(request);
        req.setOptions(options);
        Response response;
        try {
            response = performClientRequest(req);
        } catch (ResponseException e) {
            if (ignores.contains(e.getResponse().getStatusLine().getStatusCode())) {
                try {
                    return responseConverter.apply(e.getResponse());
                } catch (Exception innerException) {
                    // the exception is ignored as we now try to parse the response as an error.
                    // this covers cases like get where 404 can either be a valid document not found response,
                    // or an error for which parsing is completely different. We try to consider the 404 response as a valid one
                    // first. If parsing of the response breaks, we fall back to parsing it as an error.
                    throw parseResponseException(e);
                }
            }
            throw parseResponseException(e);
        }

        try {
            return responseConverter.apply(response);
        } catch (Exception e) {
            throw new IOException("Unable to parse response body for " + response, e);
        }
    }

    /**
     * Defines a helper method for requests that can 404 and in which case will return an empty Optional
     * otherwise tries to parse the response body
     */
    protected final <Req extends Validatable, Resp> Optional<Resp> performRequestAndParseOptionalEntity(
        Req request,
        CheckedFunction<Req, Request, IOException> requestConverter,
        RequestOptions options,
        CheckedFunction<XContentParser, Resp, IOException> entityParser
    ) throws IOException {
        Optional<ValidationException> validationException = request.validate();
        if (validationException != null && validationException.isPresent()) {
            throw validationException.get();
        }
        Request req = requestConverter.apply(request);
        req.setOptions(options);
        Response response;
        try {
            response = performClientRequest(req);
        } catch (ResponseException e) {
            if (RestStatus.NOT_FOUND.getStatus() == e.getResponse().getStatusLine().getStatusCode()) {
                return Optional.empty();
            }
            throw parseResponseException(e);
        }

        try {
            return Optional.of(parseEntity(response.getEntity(), entityParser));
        } catch (Exception e) {
            throw new IOException("Unable to parse response body for " + response, e);
        }
    }

    /**
     * @deprecated If creating a new HLRC ReST API call, consider creating new actions instead of reusing server actions. The Validation
     * layer has been added to the ReST client, and requests should extend {@link Validatable} instead of {@link ActionRequest}.
     * @return Cancellable instance that may be used to cancel the request
     */
    @Deprecated
    protected final <Req extends ActionRequest, Resp> Cancellable performRequestAsyncAndParseEntity(
        Req request,
        CheckedFunction<Req, Request, IOException> requestConverter,
        RequestOptions options,
        CheckedFunction<XContentParser, Resp, IOException> entityParser,
        ActionListener<Resp> listener,
        Set<Integer> ignores
    ) {
        return performRequestAsync(
            request,
            requestConverter,
            options,
            response -> parseEntity(response.getEntity(), entityParser),
            listener,
            ignores
        );
    }

    /**
     * Defines a helper method for asynchronously performing a request.
     * @return Cancellable instance that may be used to cancel the request
     */
    protected final <Req extends Validatable, Resp> Cancellable performRequestAsyncAndParseEntity(
        Req request,
        CheckedFunction<Req, Request, IOException> requestConverter,
        RequestOptions options,
        CheckedFunction<XContentParser, Resp, IOException> entityParser,
        ActionListener<Resp> listener,
        Set<Integer> ignores
    ) {
        return performRequestAsync(
            request,
            requestConverter,
            options,
            response -> parseEntity(response.getEntity(), entityParser),
            listener,
            ignores
        );
    }

    /**
     * @deprecated If creating a new HLRC ReST API call, consider creating new actions instead of reusing server actions. The Validation
     * layer has been added to the ReST client, and requests should extend {@link Validatable} instead of {@link ActionRequest}.
     * @return Cancellable instance that may be used to cancel the request
     */
    @Deprecated
    protected final <Req extends ActionRequest, Resp> Cancellable performRequestAsync(
        Req request,
        CheckedFunction<Req, Request, IOException> requestConverter,
        RequestOptions options,
        CheckedFunction<Response, Resp, IOException> responseConverter,
        ActionListener<Resp> listener,
        Set<Integer> ignores
    ) {
        ActionRequestValidationException validationException = request.validate();
        if (validationException != null && validationException.validationErrors().isEmpty() == false) {
            listener.onFailure(validationException);
            return Cancellable.NO_OP;
        }
        return internalPerformRequestAsync(request, requestConverter, options, responseConverter, listener, ignores);
    }

    /**
     * Defines a helper method for asynchronously performing a request.
     * @return Cancellable instance that may be used to cancel the request
     */
    protected final <Req extends Validatable, Resp> Cancellable performRequestAsync(
        Req request,
        CheckedFunction<Req, Request, IOException> requestConverter,
        RequestOptions options,
        CheckedFunction<Response, Resp, IOException> responseConverter,
        ActionListener<Resp> listener,
        Set<Integer> ignores
    ) {
        Optional<ValidationException> validationException = request.validate();
        if (validationException != null && validationException.isPresent()) {
            listener.onFailure(validationException.get());
            return Cancellable.NO_OP;
        }
        return internalPerformRequestAsync(request, requestConverter, options, responseConverter, listener, ignores);
    }

    /**
     * Provides common functionality for asynchronously performing a request.
     * @return Cancellable instance that may be used to cancel the request
     */
    private <Req, Resp> Cancellable internalPerformRequestAsync(
        Req request,
        CheckedFunction<Req, Request, IOException> requestConverter,
        RequestOptions options,
        CheckedFunction<Response, Resp, IOException> responseConverter,
        ActionListener<Resp> listener,
        Set<Integer> ignores
    ) {
        Request req;
        try {
            req = requestConverter.apply(request);
        } catch (Exception e) {
            listener.onFailure(e);
            return Cancellable.NO_OP;
        }
        req.setOptions(options);

        ResponseListener responseListener = wrapResponseListener(responseConverter, listener, ignores);
        return performClientRequestAsync(req, responseListener);
    }

    final <Resp> ResponseListener wrapResponseListener(
        CheckedFunction<Response, Resp, IOException> responseConverter,
        ActionListener<Resp> actionListener,
        Set<Integer> ignores
    ) {
        return new ResponseListener() {
            @Override
            public void onSuccess(Response response) {
                try {
                    actionListener.onResponse(responseConverter.apply(response));
                } catch (Exception e) {
                    IOException ioe = new IOException("Unable to parse response body for " + response, e);
                    onFailure(ioe);
                }
            }

            @Override
            public void onFailure(Exception exception) {
                if (exception instanceof ResponseException responseException) {
                    Response response = responseException.getResponse();
                    if (ignores.contains(response.getStatusLine().getStatusCode())) {
                        try {
                            actionListener.onResponse(responseConverter.apply(response));
                        } catch (Exception innerException) {
                            // the exception is ignored as we now try to parse the response as an error.
                            // this covers cases like get where 404 can either be a valid document not found response,
                            // or an error for which parsing is completely different. We try to consider the 404 response as a valid one
                            // first. If parsing of the response breaks, we fall back to parsing it as an error.
                            actionListener.onFailure(parseResponseException(responseException));
                        }
                    } else {
                        actionListener.onFailure(parseResponseException(responseException));
                    }
                } else {
                    actionListener.onFailure(exception);
                }
            }
        };
    }

    /**
     * Asynchronous request which returns empty {@link Optional}s in the case of 404s or parses entity into an Optional
     * @return Cancellable instance that may be used to cancel the request
     */
    protected final <Req extends Validatable, Resp> Cancellable performRequestAsyncAndParseOptionalEntity(
        Req request,
        CheckedFunction<Req, Request, IOException> requestConverter,
        RequestOptions options,
        CheckedFunction<XContentParser, Resp, IOException> entityParser,
        ActionListener<Optional<Resp>> listener
    ) {
        Optional<ValidationException> validationException = request.validate();
        if (validationException != null && validationException.isPresent()) {
            listener.onFailure(validationException.get());
            return Cancellable.NO_OP;
        }
        Request req;
        try {
            req = requestConverter.apply(request);
        } catch (Exception e) {
            listener.onFailure(e);
            return Cancellable.NO_OP;
        }
        req.setOptions(options);
        ResponseListener responseListener = wrapResponseListener404sOptional(
            response -> parseEntity(response.getEntity(), entityParser),
            listener
        );
        return performClientRequestAsync(req, responseListener);
    }

    final <Resp> ResponseListener wrapResponseListener404sOptional(
        CheckedFunction<Response, Resp, IOException> responseConverter,
        ActionListener<Optional<Resp>> actionListener
    ) {
        return new ResponseListener() {
            @Override
            public void onSuccess(Response response) {
                try {
                    actionListener.onResponse(Optional.of(responseConverter.apply(response)));
                } catch (Exception e) {
                    IOException ioe = new IOException("Unable to parse response body for " + response, e);
                    onFailure(ioe);
                }
            }

            @Override
            public void onFailure(Exception exception) {
                if (exception instanceof ResponseException responseException) {
                    Response response = responseException.getResponse();
                    if (RestStatus.NOT_FOUND.getStatus() == response.getStatusLine().getStatusCode()) {
                        actionListener.onResponse(Optional.empty());
                    } else {
                        actionListener.onFailure(parseResponseException(responseException));
                    }
                } else {
                    actionListener.onFailure(exception);
                }
            }
        };
    }

    /**
     * Converts a {@link ResponseException} obtained from the low level REST client into an {@link ElasticsearchException}.
     * If a response body was returned, tries to parse it as an error returned from Elasticsearch.
     * If no response body was returned or anything goes wrong while parsing the error, returns a new {@link ElasticsearchStatusException}
     * that wraps the original {@link ResponseException}. The potential exception obtained while parsing is added to the returned
     * exception as a suppressed exception. This method is guaranteed to not throw any exception eventually thrown while parsing.
     */
    protected final ElasticsearchStatusException parseResponseException(ResponseException responseException) {
        Response response = responseException.getResponse();
        HttpEntity entity = response.getEntity();
        ElasticsearchStatusException elasticsearchException;
        RestStatus restStatus = RestStatus.fromCode(response.getStatusLine().getStatusCode());

        if (entity == null) {
            elasticsearchException = new ElasticsearchStatusException(responseException.getMessage(), restStatus, responseException);
        } else {
            try {
                elasticsearchException = parseEntity(entity, BytesRestResponse::errorFromXContent);
                elasticsearchException.addSuppressed(responseException);
            } catch (Exception e) {
                elasticsearchException = new ElasticsearchStatusException("Unable to parse response body", restStatus, responseException);
                elasticsearchException.addSuppressed(e);
            }
        }
        return elasticsearchException;
    }

    protected final <Resp> Resp parseEntity(final HttpEntity entity, final CheckedFunction<XContentParser, Resp, IOException> entityParser)
        throws IOException {
        if (entity == null) {
            throw new IllegalStateException("Response body expected but not returned");
        }
        if (entity.getContentType() == null) {
            throw new IllegalStateException("Elasticsearch didn't return the [Content-Type] header, unable to parse response body");
        }
        XContentType xContentType = XContentType.fromMediaType(entity.getContentType().getValue());
        if (xContentType == null) {
            throw new IllegalStateException("Unsupported Content-Type: " + entity.getContentType().getValue());
        }
        try (XContentParser parser = xContentType.xContent().createParser(registry, DEPRECATION_HANDLER, entity.getContent())) {
            return entityParser.apply(parser);
        }
    }

    protected static boolean convertExistsResponse(Response response) {
        return response.getStatusLine().getStatusCode() == 200;
    }

    private enum EntityType {
        JSON() {
            @Override
            public String header() {
                return "application/json";
            }

            @Override
            public String compatibleHeader() {
                return "application/vnd.elasticsearch+json; compatible-with=7";
            }
        },
        NDJSON() {
            @Override
            public String header() {
                return "application/x-ndjson";
            }

            @Override
            public String compatibleHeader() {
                return "application/vnd.elasticsearch+x-ndjson; compatible-with=7";
            }
        },
        STAR() {
            @Override
            public String header() {
                return "application/*";
            }

            @Override
            public String compatibleHeader() {
                return "application/vnd.elasticsearch+json; compatible-with=7";
            }
        },
        YAML() {
            @Override
            public String header() {
                return "application/yaml";
            }

            @Override
            public String compatibleHeader() {
                return "application/vnd.elasticsearch+yaml; compatible-with=7";
            }
        },
        SMILE() {
            @Override
            public String header() {
                return "application/smile";
            }

            @Override
            public String compatibleHeader() {
                return "application/vnd.elasticsearch+smile; compatible-with=7";
            }
        },
        CBOR() {
            @Override
            public String header() {
                return "application/cbor";
            }

            @Override
            public String compatibleHeader() {
                return "application/vnd.elasticsearch+cbor; compatible-with=7";
            }
        };

        public abstract String header();

        public abstract String compatibleHeader();

        @Override
        public String toString() {
            return header();
        }
    }

    private Cancellable performClientRequestAsync(Request request, ResponseListener listener) {
        // Add compatibility request headers if compatibility mode has been enabled
        if (this.useAPICompatibility) {
            modifyRequestForCompatibility(request);
        }

        ListenableFuture<Optional<String>> versionCheck = getVersionValidationFuture();

        // Create a future that tracks cancellation of this method's result and forwards cancellation to the actual LLRC request.
        CompletableFuture<Void> cancellationForwarder = new CompletableFuture<Void>();
        Cancellable result = new Cancellable() {
            @Override
            public void cancel() {
                // Raise the flag by completing the future
                FutureUtils.cancel(cancellationForwarder);
            }

            @Override
            void runIfNotCancelled(Runnable runnable) {
                if (cancellationForwarder.isCancelled()) {
                    throw newCancellationException();
                }
                runnable.run();
            }
        };

        // Send the request after we have done the version compatibility check. Note that if it has already happened, the listener will
        // be called immediately on the same thread with no asynchronous scheduling overhead.
        versionCheck.addListener(new ActionListener<Optional<String>>() {
            @Override
            public void onResponse(Optional<String> validation) {
                if (validation.isPresent() == false) {
                    // Send the request and propagate cancellation
                    Cancellable call = client.performRequestAsync(request, listener);
                    cancellationForwarder.whenComplete((r, t) ->
                    // Forward cancellation to the actual request (no need to check parameters as the
                    // only way for cancellationForwarder to be completed is by being cancelled).
                    call.cancel());
                } else {
                    // Version validation wasn't successful, fail the request with the validation result.
                    listener.onFailure(new ElasticsearchException(validation.get()));
                }
            }

            @Override
            public void onFailure(Exception e) {
                // Propagate validation request failure. This will be transient since `getVersionValidationFuture` clears the validation
                // future if the request fails, leading to retries at the next HLRC request (see comments below).
                listener.onFailure(e);
            }
        });

        return result;
    };

    /**
     * Go through all the request's existing headers, looking for {@code headerName} headers and if they exist,
     * changing them to use version compatibility. If no request headers are changed, modify the entity type header if appropriate
     */
    boolean addCompatibilityFor(RequestOptions.Builder newOptions, Header entityHeader, String headerName) {
        // Modify any existing "Content-Type" headers on the request to use the version compatibility, if available
        boolean contentTypeModified = false;
        for (Header header : new ArrayList<>(newOptions.getHeaders())) {
            if (headerName.equalsIgnoreCase(header.getName()) == false) {
                continue;
            }
            contentTypeModified = contentTypeModified || modifyHeader(newOptions, header, headerName);
        }

        // If there were no request-specific headers, modify the request entity's header to be compatible
        if (entityHeader != null && contentTypeModified == false) {
            contentTypeModified = modifyHeader(newOptions, entityHeader, headerName);
        }

        return contentTypeModified;
    }

    /**
     * Modify the given header to be version compatible, if necessary.
     * Returns true if a modification was made, false otherwise.
     */
    boolean modifyHeader(RequestOptions.Builder newOptions, Header header, String headerName) {
        for (EntityType type : EntityType.values()) {
            final String headerValue = header.getValue();
            if (headerValue.startsWith(type.header())) {
                String newHeaderValue = headerValue.replace(type.header(), type.compatibleHeader());
                newOptions.removeHeader(header.getName());
                newOptions.addHeader(headerName, newHeaderValue);
                return true;
            }
        }
        return false;
    }

    /**
     * Make all necessary changes to support API compatibility for the given request. This includes
     * modifying the "Content-Type" and "Accept" headers if present, or modifying the header based
     * on the request's entity type.
     */
    void modifyRequestForCompatibility(Request request) {
        final Header entityHeader = request.getEntity() == null ? null : request.getEntity().getContentType();
        final RequestOptions.Builder newOptions = request.getOptions().toBuilder();

        addCompatibilityFor(newOptions, entityHeader, "Content-Type");
        if (request.getOptions().containsHeader("Accept")) {
            addCompatibilityFor(newOptions, entityHeader, "Accept");
        } else {
            // There is no entity, and no existing accept header, but we still need one
            // with compatibility, so use the compatible JSON (default output) format
            newOptions.addHeader("Accept", EntityType.JSON.compatibleHeader());
        }
        request.setOptions(newOptions);
    }

    private Response performClientRequest(Request request) throws IOException {
        // Add compatibility request headers if compatibility mode has been enabled
        if (this.useAPICompatibility) {
            modifyRequestForCompatibility(request);
        }

        Optional<String> versionValidation;
        try {
            versionValidation = getVersionValidationFuture().get();
        } catch (InterruptedException | ExecutionException e) {
            // Unlikely to happen
            throw new ElasticsearchException(e);
        }

        if (versionValidation.isPresent() == false) {
            return client.performRequest(request);
        } else {
            throw new ElasticsearchException(versionValidation.get());
        }
    }

    /**
     * Returns a future that asynchronously validates the Elasticsearch product version. Its result is an optional string: if empty then
     * validation was successful, if present it contains the validation error. API requests should be chained to this future and check
     * the validation result before going further.
     * <p>
     * This future is a memoization of the first successful request to the "/" endpoint and the subsequent compatibility check
     * ({@see #versionValidationFuture}). Further client requests reuse its result.
     * <p>
     * If the version check request fails (e.g. network error), {@link #versionValidationFuture} is cleared so that a new validation
     * request is sent at the next HLRC request. This allows retries to happen while avoiding a busy retry loop (LLRC retries on the node
     * pool still happen).
     */
    private ListenableFuture<Optional<String>> getVersionValidationFuture() {
        ListenableFuture<Optional<String>> currentFuture = this.versionValidationFuture;
        if (currentFuture != null) {
            return currentFuture;
        } else {
            synchronized (this) {
                // Re-check in synchronized block
                currentFuture = this.versionValidationFuture;
                if (currentFuture != null) {
                    return currentFuture;
                }
                ListenableFuture<Optional<String>> future = new ListenableFuture<>();
                this.versionValidationFuture = future;

                // Asynchronously call the info endpoint and complete the future with the version validation result.
                Request req = new Request("GET", "/");
                // These status codes are nominal in the context of product version verification
                req.addParameter("ignore", "401,403");
                client.performRequestAsync(req, new ResponseListener() {
                    @Override
                    public void onSuccess(Response response) {
                        Optional<String> validation;
                        try {
                            validation = getVersionValidation(response);
                        } catch (Exception e) {
                            logger.error("Failed to parse info response", e);
                            validation = Optional.of(
                                "Failed to parse info response. Check logs for detailed information - " + e.getMessage()
                            );
                        }
                        future.onResponse(validation);
                    }

                    @Override
                    public void onFailure(Exception exception) {

                        // Fail the requests (this one and the ones waiting for it) and clear the future
                        // so that we retry the next time the client executes a request.
                        versionValidationFuture = null;
                        future.onFailure(exception);
                    }
                });

                return future;
            }
        }
    }

    /**
     * Validates that the response info() is a compatible Elasticsearch version.
     *
     * @return an optional string. If empty, version is compatible. Otherwise, it's the message to return to the application.
     */
    private Optional<String> getVersionValidation(Response response) throws IOException {
        // Let requests go through if the client doesn't have permissions for the info endpoint.
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode == 401 || statusCode == 403) {
            return Optional.empty();
        }

        MainResponse mainResponse;
        try {
            mainResponse = parseEntity(response.getEntity(), MainResponse::fromXContent);
        } catch (ResponseException e) {
            throw parseResponseException(e);
        }

        String version = mainResponse.getVersion().getNumber();
        if (Strings.hasLength(version) == false) {
            return Optional.of("Missing version.number in info response");
        }

        String[] parts = version.split("\\.");
        if (parts.length < 2) {
            return Optional.of("Wrong version.number format in info response");
        }

        int major = Integer.parseInt(parts[0]);
        int minor = Integer.parseInt(parts[1]);

        if (major < 6) {
            return Optional.of("Elasticsearch version 6 or more is required");
        }

        if (major == 6 || (major == 7 && minor < 14)) {
            if ("You Know, for Search".equalsIgnoreCase(mainResponse.getTagline()) == false) {
                return Optional.of("Invalid or missing tagline [" + mainResponse.getTagline() + "]");
            }

            if (major == 7) {
                // >= 7.0 and < 7.14
                String responseFlavor = mainResponse.getVersion().getBuildFlavor();
                if ("default".equals(responseFlavor) == false) {
                    // Flavor is unknown when running tests, and non-mocked responses will return an unknown flavor
                    if (Build.CURRENT.flavor() != Build.Flavor.UNKNOWN || "unknown".equals(responseFlavor) == false) {
                        return Optional.of("Invalid or missing build flavor [" + responseFlavor + "]");
                    }
                }
            }

            return Optional.empty();
        }

        String header = response.getHeader("X-Elastic-Product");
        if (header == null) {
            return Optional.of(
                "Missing [X-Elastic-Product] header. Please check that you are connecting to an Elasticsearch "
                    + "instance, and that any networking filters are preserving that header."
            );
        }

        if ("Elasticsearch".equals(header) == false) {
            return Optional.of("Invalid value [" + header + "] for [X-Elastic-Product] header.");
        }

        return Optional.empty();
    }

    /**
     * Ignores deprecation warnings. This is appropriate because it is only
     * used to parse responses from Elasticsearch. Any deprecation warnings
     * emitted there just mean that you are talking to an old version of
     * Elasticsearch. There isn't anything you can do about the deprecation.
     */
    private static final DeprecationHandler DEPRECATION_HANDLER = DeprecationHandler.IGNORE_DEPRECATIONS;

    static List<NamedXContentRegistry.Entry> getDefaultNamedXContents() {
        Map<String, ContextParser<Object, ? extends Aggregation>> map = new HashMap<>();
        map.put(CardinalityAggregationBuilder.NAME, (p, c) -> ParsedCardinality.fromXContent(p, (String) c));
        map.put(InternalHDRPercentiles.NAME, (p, c) -> ParsedHDRPercentiles.fromXContent(p, (String) c));
        map.put(InternalHDRPercentileRanks.NAME, (p, c) -> ParsedHDRPercentileRanks.fromXContent(p, (String) c));
        map.put(InternalTDigestPercentiles.NAME, (p, c) -> ParsedTDigestPercentiles.fromXContent(p, (String) c));
        map.put(InternalTDigestPercentileRanks.NAME, (p, c) -> ParsedTDigestPercentileRanks.fromXContent(p, (String) c));
        map.put(PercentilesBucketPipelineAggregationBuilder.NAME, (p, c) -> ParsedPercentilesBucket.fromXContent(p, (String) c));
        map.put(MedianAbsoluteDeviationAggregationBuilder.NAME, (p, c) -> ParsedMedianAbsoluteDeviation.fromXContent(p, (String) c));
        map.put(MinAggregationBuilder.NAME, (p, c) -> ParsedMin.fromXContent(p, (String) c));
        map.put(MaxAggregationBuilder.NAME, (p, c) -> ParsedMax.fromXContent(p, (String) c));
        map.put(SumAggregationBuilder.NAME, (p, c) -> ParsedSum.fromXContent(p, (String) c));
        map.put(AvgAggregationBuilder.NAME, (p, c) -> ParsedAvg.fromXContent(p, (String) c));
        map.put(WeightedAvgAggregationBuilder.NAME, (p, c) -> ParsedWeightedAvg.fromXContent(p, (String) c));
        map.put(ValueCountAggregationBuilder.NAME, (p, c) -> ParsedValueCount.fromXContent(p, (String) c));
        map.put(InternalSimpleValue.NAME, (p, c) -> ParsedSimpleValue.fromXContent(p, (String) c));
        map.put(DerivativePipelineAggregationBuilder.NAME, (p, c) -> ParsedDerivative.fromXContent(p, (String) c));
        map.put(InternalBucketMetricValue.NAME, (p, c) -> ParsedBucketMetricValue.fromXContent(p, (String) c));
        map.put(StatsAggregationBuilder.NAME, (p, c) -> ParsedStats.fromXContent(p, (String) c));
        map.put(StatsBucketPipelineAggregationBuilder.NAME, (p, c) -> ParsedStatsBucket.fromXContent(p, (String) c));
        map.put(ExtendedStatsAggregationBuilder.NAME, (p, c) -> ParsedExtendedStats.fromXContent(p, (String) c));
        map.put(ExtendedStatsBucketPipelineAggregationBuilder.NAME, (p, c) -> ParsedExtendedStatsBucket.fromXContent(p, (String) c));
        map.put(GeoBoundsAggregationBuilder.NAME, (p, c) -> ParsedGeoBounds.fromXContent(p, (String) c));
        map.put(GeoCentroidAggregationBuilder.NAME, (p, c) -> ParsedGeoCentroid.fromXContent(p, (String) c));
        map.put(HistogramAggregationBuilder.NAME, (p, c) -> ParsedHistogram.fromXContent(p, (String) c));
        map.put(DateHistogramAggregationBuilder.NAME, (p, c) -> ParsedDateHistogram.fromXContent(p, (String) c));
        map.put(AutoDateHistogramAggregationBuilder.NAME, (p, c) -> ParsedAutoDateHistogram.fromXContent(p, (String) c));
        map.put(VariableWidthHistogramAggregationBuilder.NAME, (p, c) -> ParsedVariableWidthHistogram.fromXContent(p, (String) c));
        map.put(StringTerms.NAME, (p, c) -> ParsedStringTerms.fromXContent(p, (String) c));
        map.put(LongTerms.NAME, (p, c) -> ParsedLongTerms.fromXContent(p, (String) c));
        map.put(DoubleTerms.NAME, (p, c) -> ParsedDoubleTerms.fromXContent(p, (String) c));
        map.put(LongRareTerms.NAME, (p, c) -> ParsedLongRareTerms.fromXContent(p, (String) c));
        map.put(StringRareTerms.NAME, (p, c) -> ParsedStringRareTerms.fromXContent(p, (String) c));
        map.put(MissingAggregationBuilder.NAME, (p, c) -> ParsedMissing.fromXContent(p, (String) c));
        map.put(NestedAggregationBuilder.NAME, (p, c) -> ParsedNested.fromXContent(p, (String) c));
        map.put(ReverseNestedAggregationBuilder.NAME, (p, c) -> ParsedReverseNested.fromXContent(p, (String) c));
        map.put(GlobalAggregationBuilder.NAME, (p, c) -> ParsedGlobal.fromXContent(p, (String) c));
        map.put(FilterAggregationBuilder.NAME, (p, c) -> ParsedFilter.fromXContent(p, (String) c));
        map.put(InternalSampler.PARSER_NAME, (p, c) -> ParsedSampler.fromXContent(p, (String) c));
        map.put(GeoHashGridAggregationBuilder.NAME, (p, c) -> ParsedGeoHashGrid.fromXContent(p, (String) c));
        map.put(GeoTileGridAggregationBuilder.NAME, (p, c) -> ParsedGeoTileGrid.fromXContent(p, (String) c));
        map.put(RangeAggregationBuilder.NAME, (p, c) -> ParsedRange.fromXContent(p, (String) c));
        map.put(DateRangeAggregationBuilder.NAME, (p, c) -> ParsedDateRange.fromXContent(p, (String) c));
        map.put(GeoDistanceAggregationBuilder.NAME, (p, c) -> ParsedGeoDistance.fromXContent(p, (String) c));
        map.put(FiltersAggregationBuilder.NAME, (p, c) -> ParsedFilters.fromXContent(p, (String) c));
        map.put(AdjacencyMatrixAggregationBuilder.NAME, (p, c) -> ParsedAdjacencyMatrix.fromXContent(p, (String) c));
        map.put(SignificantLongTerms.NAME, (p, c) -> ParsedSignificantLongTerms.fromXContent(p, (String) c));
        map.put(SignificantStringTerms.NAME, (p, c) -> ParsedSignificantStringTerms.fromXContent(p, (String) c));
        map.put(ScriptedMetricAggregationBuilder.NAME, (p, c) -> ParsedScriptedMetric.fromXContent(p, (String) c));
        map.put(IpRangeAggregationBuilder.NAME, (p, c) -> ParsedBinaryRange.fromXContent(p, (String) c));
        map.put(TopHitsAggregationBuilder.NAME, (p, c) -> ParsedTopHits.fromXContent(p, (String) c));
        map.put(CompositeAggregationBuilder.NAME, (p, c) -> ParsedComposite.fromXContent(p, (String) c));
        map.put(StringStatsAggregationBuilder.NAME, (p, c) -> ParsedStringStats.PARSER.parse(p, (String) c));
        map.put(TopMetricsAggregationBuilder.NAME, (p, c) -> ParsedTopMetrics.PARSER.parse(p, (String) c));
        map.put(InferencePipelineAggregationBuilder.NAME, (p, c) -> ParsedInference.fromXContent(p, (String) (c)));
        map.put(TimeSeriesAggregationBuilder.NAME, (p, c) -> ParsedTimeSeries.fromXContent(p, (String) (c)));
        List<NamedXContentRegistry.Entry> entries = map.entrySet()
            .stream()
            .map(entry -> new NamedXContentRegistry.Entry(Aggregation.class, new ParseField(entry.getKey()), entry.getValue()))
            .collect(Collectors.toList());
        entries.add(
            new NamedXContentRegistry.Entry(
                Suggest.Suggestion.class,
                new ParseField(TermSuggestionBuilder.SUGGESTION_NAME),
                (parser, context) -> TermSuggestion.fromXContent(parser, (String) context)
            )
        );
        entries.add(
            new NamedXContentRegistry.Entry(
                Suggest.Suggestion.class,
                new ParseField(PhraseSuggestionBuilder.SUGGESTION_NAME),
                (parser, context) -> PhraseSuggestion.fromXContent(parser, (String) context)
            )
        );
        entries.add(
            new NamedXContentRegistry.Entry(
                Suggest.Suggestion.class,
                new ParseField(CompletionSuggestionBuilder.SUGGESTION_NAME),
                (parser, context) -> CompletionSuggestion.fromXContent(parser, (String) context)
            )
        );
        return entries;
    }

    /**
     * Loads and returns the {@link NamedXContentRegistry.Entry} parsers provided by plugins.
     */
    static List<NamedXContentRegistry.Entry> getProvidedNamedXContents() {
        List<NamedXContentRegistry.Entry> entries = new ArrayList<>();
        for (NamedXContentProvider service : ServiceLoader.load(NamedXContentProvider.class)) {
            entries.addAll(service.getNamedXContentParsers());
        }
        return entries;
    }
}
