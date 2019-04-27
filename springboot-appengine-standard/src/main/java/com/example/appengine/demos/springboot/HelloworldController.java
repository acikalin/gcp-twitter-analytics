/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.appengine.demos.springboot;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.QueryJobConfiguration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloworldController {
  @GetMapping("/")
  public String hello() throws InterruptedException {

    BigQuery bigquery = BigQueryOptions.getDefaultInstance().getService();

    String query = "#standardSQL\n" +
            "WITH words_in_tweets AS (\n" +
            "  SELECT \n" +
            "    ARRAY(\n" +
            "      SELECT AS STRUCT word, COUNT(1) AS cnt \n" +
            "      FROM UNNEST(REGEXP_EXTRACT_ALL(text, r'[\\w_]+')) AS word \n" +
            "      GROUP BY word\n" +
            "      HAVING NOT REGEXP_CONTAINS(word, r'^\\d+$')\n" +
            "    ) AS words\n" +
            "  FROM `twitter.tweets`\n" +
            "),\n" +
            "words_in_corpus AS (\n" +
            "  SELECT word, SUM(cnt) AS cnt\n" +
            "  FROM words_in_tweets, UNNEST(words) AS words\n" +
            "  GROUP BY word\n" +
            ")\n" +
            "SELECT * \n" +
            "FROM words_in_corpus\n" +
            "ORDER BY cnt DESC\n" +
            "Limit 10";
    // Create a query request
    QueryJobConfiguration queryConfig =
            QueryJobConfiguration.newBuilder(query).build();
    // Read rows
    return "Hello world - springboot-appengine-standard!" + bigquery.query(queryConfig).toString();
  }
}
