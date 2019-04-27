PROJECT_ID=[YOUR_PROJECT_NAME]
PROJECT_ID=[YOUR_PROJECT_NAME]
gcloud config set compute/zone us-west1-a
gcloud config set compute/region us-west1
# Build a Docker image for our Python application
docker build -t gcr.io/$PROJECT_ID/pubsub_pipeline gcp-twitter-analytics/k8s-twitter-to-pubsub

# Save the image on Google Container Registry
gcloud docker -- push gcr.io/$PROJECT_ID/pubsub_pipeline

# Create a Pub/Sub topic that will collect all the tweets
gcloud beta pubsub topics create twitter

# Create a Google Container Engine Cluster (enabled to write on Pub/Sub)
gcloud container clusters create $PROJECT_ID-cluster --num-nodes=1 --scopes=bigquery, pubsub, storage-ro, compute-rw

# Acquire the credentials to access the K8S Master
gcloud container clusters get-credentials $PROJECT_ID-cluster

# Deploy our application on the cluster, within a ReplicationController
kubectl create -f gcp-twitter-analytics/k8s-twitter-to-pubsub/twitter-stream.yaml

# Create the BigQuery dataset
bq mk twitter

# Create a staging bucket
gsutil mb -l US gs://${PROJECT_ID}-staging

# Launch the Dataflow Pipeline
cd gcp-twitter-analytics/dataflow-pubsub-to-bigquery/
mvn compile exec:java -Dexec.mainClass=com.example.dataflow.TwitterProcessor -Dexec.args="--streaming --stagingLocation=gs://$PROJECT_ID-staging --project=$PROJECT_ID"

# Create an App Engine
gcloud app create --project=$PROJECT_ID --region=us-west2
# Launch the App Engine Show Data
cd ..
cd springboot-appengine-standard/
mvn clean package
mvn mvn appengine:deploy
