# gcp-twitter-analytics
Analyze tweets with a Serverless Data Processing pipeline on Google Cloud Platform.

The architecture consists of:
- a Google Container Engine cluster running a Python application that gathers tweets and sends them to Google Pub/Sub;
- a Google Cloud Pub/Sub topic;
- a Google Cloud Dataflow pipeline that reads from the Pub/Sub topic and uses the Natural Language API to retrieve the sentiment of each tweet;
- a Google BigQuery dataset containing two tables, respectively for "raw" and "annotated" tweets.
- a Google App Engine shows results

## Setup
The repository contains a bash script that automates most of the work. However, there is still something you have to do yourself:

- Create a new Google Cloud Platform project (see https://support.google.com/cloud/answer/6251787?hl=en for instructions).
- Enable the Natural Language API from the Cloud Console (https://console.cloud.google.com/apis/api/language.googleapis.com/overview).
- Enable the PubSub from the Cloud Console (https://console.cloud.google.com/cloudpubsub).
- Enable the DataFlow from the Cloud Console (https://console.cloud.google.com/dataflow).
- Create a new App Engine Java Application (https://console.cloud.google.com/appengine).
- Open Google Cloud Shell.
- Within Cloud Shell, clone the Git Repository: `git clone https://github.com/acikalin/gcp-twitter-analytics.git gcp-twitter-analytics`.
- [Create a Twitter application](https://apps.twitter.com/app/new) and paste the required information in the gcp-twitter-analytics/k8s-twitter-to-pubsub/twitter-stream.yaml file. Use your preferred text editor, like `vi` or `nano`: `nano gcp-twitter-analytics/k8s-twitter-to-pubsub/twitter-stream.yaml`
- launch the start.sh file to provision and start the processing pipeline: `bash gcp-twitter-analytics/start.sh`.

## Credits
The Python appplication that collects tweets and publish them on Pub/Sub comes from the really nice example "Real-Time Data Analysis with Kubernetes, Cloud Pub/Sub, and BigQuery" published here: https://cloud.google.com/solutions/real-time/kubernetes-pubsub-bigquery.
