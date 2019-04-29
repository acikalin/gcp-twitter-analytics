#!/bin/bash

PROJECT_ID=acikalin4
gcloud config set project $PROJECT_ID
gcloud config set compute/zone us-west1-a
gcloud config set compute/region us-west1

# Create a Pub/Sub topic that will collect all the tweets
#gcloud beta pubsub topics create twitter

## Create a Google Container Engine Cluster (enabled to write on Pub/Sub)
#gcloud container clusters create tweets --zone us-west1-a --scopes=bigquery,pubsub,storage-ro,compute-rw
#
## Acquire the credentials to access the K8S Master
#gcloud container clusters get-credentials tweets --zone us-west1-a --project $PROJECT_ID
#
## Deploy our application on the cluster, within a ReplicationController
#kubectl create -f gcp-twitter-analytics/k8s-twitter-to-pubsub/twitter-stream.yaml

## Create the BigQuery dataset
#bq mk twitter
#
## Create a staging bucket
gsutil mb gs://$PROJECT_ID
#
## Launch the Dataflow Pipeline
cd gcp-twitter-analytics/dataflow-pubsub-to-bigquery/
mvn compile exec:java -Dexec.mainClass=com.example.dataflow.TwitterProcessor -Dexec.args="--streaming --project=$PROJECT_ID --stagingLocation=gs://$PROJECT_ID/staging"
#
## Create an App Engine
#gcloud app create --project=$PROJECT_ID --region=us-west2
#
## Launch the App Engine Show Data
#cd ..
#cd springboot-appengine-standard/
#mvn clean package
#mvn appengine:deploy
#
## Get your application URL
#gcloud app browse