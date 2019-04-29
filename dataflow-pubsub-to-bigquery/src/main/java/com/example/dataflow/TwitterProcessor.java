package com.example.dataflow;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.cloud.language.v1.*;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO;
import org.apache.beam.sdk.io.gcp.testing.BigqueryClient;
import org.apache.beam.sdk.transforms.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.services.bigquery.model.TableReference;
import com.google.api.services.bigquery.model.TableRow;
import com.google.api.services.bigquery.model.TableSchema;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.runners.dataflow.options.DataflowPipelineOptions;
import org.apache.beam.runners.dataflow.options.DataflowPipelineWorkerPoolOptions.AutoscalingAlgorithmType;
import org.apache.beam.runners.dataflow.DataflowRunner;

import com.google.cloud.language.v1.AnalyzeSyntaxRequest;
import com.google.cloud.language.v1.Document.Type;
import com.google.cloud.language.v1.EncodingType;

public class TwitterProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(TwitterProcessor.class);

    public static void main(String[] args) throws Exception {
        DataflowPipelineOptions options = PipelineOptionsFactory
                .fromArgs(args).withValidation()
                .create()
                .as(DataflowPipelineOptions.class);

        options.setRunner(DataflowRunner.class);
        options.setAutoscalingAlgorithm(AutoscalingAlgorithmType.THROUGHPUT_BASED);
        options.setMaxNumWorkers(3);
        String projectId = options.getProject();

        Pipeline pipeline = Pipeline.create(options);
        pipeline.apply("TweetsReadPubSub", PubsubIO.readStrings().fromTopic("projects/" + projectId + "/topics/twitter"))
                .apply("ConvertDataToTableRows", ParDo.of(new DoFn<String, TableRow>() {
                    @ProcessElement
                    public void processElement(ProcessContext c) {
                        TableRow outputRow = new TableRow();
                        try {
                            JsonObject jsonTweet = new JsonParser().parse(c.element()).getAsJsonObject();

                            if (jsonTweet != null
                                    && (
                                    jsonTweet.get("text") != null
                                            && !jsonTweet.get("text").getAsString().isEmpty()
                                            && jsonTweet.get("text").getAsString().toLowerCase().contains("stark"))
                                    && (
                                    jsonTweet.get("lang") != null
                                            && !jsonTweet.get("lang").getAsString().isEmpty()
                                            && jsonTweet.get("lang").getAsString().equalsIgnoreCase("en"))
                            ) {
                                List<Token> tokens = analyzeSyntaxText(jsonTweet.get("text").getAsString());
                                List<TableRow> surfaceFormList = new ArrayList();

                                for (Token token : tokens){
                                    TableRow nestedRow = new TableRow();
                                    nestedRow.put("partOfSpeech", token.getPartOfSpeech().getTag());
                                    nestedRow.put("content", token.getText().getContent());
                                    surfaceFormList.add(nestedRow);
                                }
                                outputRow.set("syntax", surfaceFormList);
                            }

                        } catch (Exception e) {
                            LOG.error(e.toString());
                        }
                        c.output(outputRow);
                    }
                }))
                .apply("InsertToBigQuery", BigQueryIO
                        .writeTableRows()
                        .to(getTableReference(projectId))
                        .withSchema(getTableSchema(projectId))
                        .withCreateDisposition(BigQueryIO.Write.CreateDisposition.CREATE_IF_NEEDED)
                        .withWriteDisposition(BigQueryIO.Write.WriteDisposition.WRITE_APPEND)
                        .withoutValidation());
        pipeline.run();
    }

    private static List<Token> analyzeSyntaxText(String text) throws Exception {
        try (LanguageServiceClient language = LanguageServiceClient.create()) {
            Document doc = Document.newBuilder()
                    .setContent(text)
                    .setType(Type.PLAIN_TEXT)
                    .build();
            AnalyzeSyntaxRequest request = AnalyzeSyntaxRequest.newBuilder()
                    .setDocument(doc)
                    .setEncodingType(EncodingType.UTF16)
                    .build();
            AnalyzeSyntaxResponse response = language.analyzeSyntax(request);
            return response.getTokensList();
        }
    }

    private static TableReference getTableReference(String projectId) {
        TableReference tableReference = new TableReference();
        tableReference.setProjectId(projectId);
        tableReference.setDatasetId("twitter");
        tableReference.setTableId("tweets_raw");
        return tableReference;
    }

    private static TableSchema getTableSchema(String projectId) throws IOException, InterruptedException {
        return BigqueryClient
                .getClient(projectId)
                .getTableResource(projectId,"twitter","tweets_raw")
                .getSchema();
    }
}