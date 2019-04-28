package com.example.dataflow;

import com.google.api.services.bigquery.model.TableFieldSchema;
import com.google.cloud.language.v1.*;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO;
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

import java.util.ArrayList;
import java.util.List;

public class TwitterProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(TwitterProcessor.class);
    public static void main(String[] args) {
        DataflowPipelineOptions options = PipelineOptionsFactory
                .fromArgs(args).withValidation()
                .create()
                .as(DataflowPipelineOptions.class);

        options.setRunner(DataflowRunner.class);
        options.setAutoscalingAlgorithm(AutoscalingAlgorithmType.THROUGHPUT_BASED);
        options.setMaxNumWorkers(3);
        String projectId = options.getProject();
        LOG.info("MERHABA");
        Pipeline pipeline = Pipeline.create(options);
        pipeline.apply("TweetsReadPubSub", PubsubIO.readStrings().fromTopic("projects/" + projectId + "/topics/twitter"))
                .apply("ConvertDataToTableRows", ParDo.of(new DoFn<String, TableRow>() {
                    @ProcessElement
                    public void processElement(ProcessContext c) {
                        TableRow row = new TableRow();
                        try {
                            JsonObject jsonTweet = new JsonParser().parse(c.element()).getAsJsonObject();

                            if (jsonTweet != null && jsonTweet.getAsJsonPrimitive("text") != null && jsonTweet.getAsJsonPrimitive("lang") != null) {

                                if (jsonTweet.getAsJsonPrimitive("lang").getAsString().equalsIgnoreCase("en")) {

                                    LOG.info("Processing tweet: " + c.element());

                                    Sentiment sentiment = analyzeSentiment(jsonTweet.getAsJsonPrimitive("text").getAsString());
                                    List<Token> tokens = analyzeSyntaxText(jsonTweet.getAsJsonPrimitive("text").getAsString());
                                    JSONArray jsonTokens = new JSONArray();
                                    for (Token token : tokens) {
                                        JSONObject jsonToken = new JSONObject();
                                        jsonToken.put("partOfSpeech", token.getPartOfSpeech().getTag());
                                        jsonToken.put("content", token.getText().getContent());
                                        jsonTokens.add(jsonToken);
                                    }
                                    row.set("tweet_object", c.element())
                                            .set("syntax", jsonTokens.toJSONString())
                                            .set("score", sentiment.getScore())
                                            .set("magnitude", sentiment.getMagnitude());
                                    c.output(row);
                                }
                            }
                        } catch (Exception e) {
                            row.set("tweet_object", e.toString());
                            c.output(row);
                        }
                    }
                }))
                .apply("InsertToBigQuery", BigQueryIO
                        .writeTableRows()
                        .to(getTableReference(projectId))
                        .withSchema(getTableSchema())
                        .withCreateDisposition(BigQueryIO.Write.CreateDisposition.CREATE_IF_NEEDED)
                        .withWriteDisposition(BigQueryIO.Write.WriteDisposition.WRITE_APPEND)
                        .withoutValidation());
        pipeline.run();
    }

    private static Sentiment analyzeSentiment(String text) throws Exception {
        try (LanguageServiceClient language = LanguageServiceClient.create()) {
            Document doc = Document.newBuilder()
                    .setContent(text)
                    .setType(Type.PLAIN_TEXT)
                    .build();
            AnalyzeSentimentResponse response = language.analyzeSentiment(doc);
            return response.getDocumentSentiment();
        }
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

    private static TableSchema getTableSchema() {
        List<TableFieldSchema> fields = new ArrayList<>();
        fields.add(new TableFieldSchema().setName("tweet_object").setType("STRING"));
        fields.add(new TableFieldSchema().setName("syntax").setType("STRING"));
        fields.add(new TableFieldSchema().setName("score").setType("FLOAT"));
        fields.add(new TableFieldSchema().setName("magnitude").setType("FLOAT"));
        return new TableSchema().setFields(fields);
    }

    private static TableReference getTableReference(String projectId) {
        TableReference tableReference = new TableReference();
        tableReference.setProjectId(projectId);
        tableReference.setDatasetId("twitter");
        tableReference.setTableId("tweets_raw");
        return tableReference;
    }
}
