# Transaction Fraud Solution Kit
## Model Inference for Fraudulent Transactions

This tutorial serves as a follow-up to "Leveraging Graph Features to Build ML Models for Fraud Detection." It is crucial to review the previous tutorial first, as it covers the model training that we will utilize here. This guide demonstrates how to deploy the trained model to predict fraudulent transactions using two methods: local and cloud-based deployment via AWS SageMaker.

Prerequisites:
* Model saved from the "Leveraging Graph Features to Build ML Models for Fraud Detection" tutorial.
* Familiarity with machine learning workflow and AWS SageMaker (for the SageMaker Inference approach).

## Approach 1: Local Deployment

Here, the model is stored locally on disk. We'll implement a `Predictor` class that loads the model from the file and provides a `predict` method to evaluate either a single transaction ID or a list of them. This approach is intended for local testing and forms the basis for more sophisticated deployment strategies.


```python
from pyTigerGraph import TigerGraphConnection
import pandas as pd
import numpy as np
from xgboost import XGBClassifier

class Predictor:
    """Class to predict fraudulent transactions.
    """
    def __init__(self):
        self.conn = None
        self.model = None
        self.features = [
            "mer_com_size", "cd_com_size", "mer_pagerank", "cd_pagerank", "shortest_path_length",
            "max_txn_amt_interval", "max_txn_cnt_interval", "cnt_repeated_card", "com_mer_txn_cnt", "com_cd_txn_cnt",
            "com_mer_txn_total_amt", "com_cd_txn_total_amt", "com_mer_txn_avg_amt", "com_cd_txn_avg_amt",
            "com_mer_txn_max_amt", "com_cd_txn_max_amt", "com_mer_txn_min_amt", "com_cd_txn_min_amt",
            "mer_cat_cnt", "mer_cat_total_amt", "mer_cat_avg_amt", "mer_cat_max_amt", "mer_cat_min_amt",
            "indegree", "outdegree", "amount", "age", "city_pop", "gender"
        ]
        
    def init_db_connection(self, host: str, username: str, password: str, graphname: str, getToken: bool):
        """Initialize connection to TigerGraph database
        
        Args:
            host (str):
                Address of the database
            username (str):
                Database username
            password (str):
                Database password
            graphname (str):
                Name of the graph 
            getToken (bool):
                Wether the database has token authentication enabled.
        """
        self.conn = TigerGraphConnection(
            host=host,
            username=username,
            password=password,
            graphname=graphname
        )
        resp = self.conn.ping()
        if resp["error"]:
            raise ConnectionError("Failed to connect to the database")
        if getToken:
            self.conn.getToken()
            
    def load_model(self, model_path: str):
        """Load an XGBoost model from a specified file path.
        
        Args:
            model_path (str):
                Path to the file that stores the trained xgboost model.
        """
        self.model = XGBClassifier()
        self.model.load_model(model_path)
        
    def _get_data(self, primary_id):
        if not self.conn:
            raise "Please initialize database connection first"
        transactions = self.conn.getVertexDataFrameById(
            "Payment_Transaction", 
            primary_id,
            select=",".join(self.features+["transaction_time"]))
        return transactions
    
    @staticmethod
    def _preprocess(transactions: pd.DataFrame):
        transactions["gender"] = transactions.gender.map({"F":0, "M":1})
        transactions["transaction_time"] = pd.to_datetime(transactions["transaction_time"])
        transactions['hour'] = transactions['transaction_time'].dt.hour
        transactions['day'] = transactions['transaction_time'].dt.day
        transactions['month'] = transactions['transaction_time'].dt.month
        transactions["extra_amount"]=np.log(transactions["amount"])-np.log(transactions["mer_cat_avg_amt"])
        return transactions
    
    def predict(self, primary_id):
        """Predict the likelihood of transactions being fraudulent.
        
        Args:
            primary_id (str or List[str]):
                ID(s) of the transaction(s) to make prediction on.
        """
        if not self.model:
            raise "Please load model first"
        transactions = self._get_data(primary_id)
        transactions = self._preprocess(transactions)
        train_features = self.features + ["extra_amount", "hour", "day", "month"]
        y = self.model.predict_proba(transactions[train_features])[:,1]
        result = {"status": "ok", "predictions": dict(zip(transactions.id, y.astype("float")))}
        
        return result
```

### Creating Predictor


```python
predictor = Predictor()
```


```python
# Initialize connection to database
# Set getToken to False if your database doesn't have token auth enabled
predictor.init_db_connection(
    host="http://DATABASE_ADDRESS",
    username="YOUR_USERNAME",
    password="YOUR_PASSWORD",
    graphname="Transaction_Fraud",
    getToken=True
)
```


```python
# Load the trained model
predictor.load_model("xgboost_v1.json")
```

### Making Predictions


```python
# Predict for a single transaction
predictor.predict("9b3792eff452fbfe8bf24403b7b02cdf")
```




    {'status': 'ok',
     'predictions': {'9b3792eff452fbfe8bf24403b7b02cdf': 0.01762988232076168}}




```python
# Predict for a short list of transactions
predictor.predict(["9b3792eff452fbfe8bf24403b7b02cdf", "3ca3b37911069332c0fca7ab9f4e7c59"])
```




    {'status': 'ok',
     'predictions': {'9b3792eff452fbfe8bf24403b7b02cdf': 0.01762988232076168,
      '3ca3b37911069332c0fca7ab9f4e7c59': 0.890177309513092}}



The response from the `predict` function is a dictionary. The "predictions" field of it contains the probability of fraud for each input transaction.

## Approach 2: Sagemaker Inference

[Amazon Sagemaker Inference](https://aws.amazon.com/sagemaker/deploy/) is a model hosting and serving service from AWS. We use it as an example to show how to deploy the model to a remote server for online inference. The detailed steps for your model serving platform would probably differ and vary from case to case, but the idea is generally applicable. 

### Initial Setup

Before we start, we need to install the dependencies and configure the AWS settings. The packages `boto3` and `sagemaker` are required for the deployment. To install then, please uncomment and run the cell below. They only need to be installed once. Then set the correct AWS credentials and the region of your choice.


```python
#%pip install boto3 sagemaker
```


```python
import boto3
import sagemaker
import os

# Set your AWS access key and secret
os.environ["AWS_ACCESS_KEY_ID"]="YOUR_ACCESS_KEY_ID"
os.environ["AWS_SECRET_ACCESS_KEY"]="YOUR_SECRET_ACCESS_KEY"
# Set the region_name to yours
boto3.setup_default_session(region_name="us-west-1")
```

    sagemaker.config INFO - Not applying SDK defaults from location: /etc/xdg/sagemaker/config.yaml
    sagemaker.config INFO - Not applying SDK defaults from location: /home/tigergraph/.config/sagemaker/config.yaml


First, we compress the saved model file into a tarball. Then you need to upload this tarball to a S3 bucket of your choice.


```python
import tarfile

# Compress the file
with tarfile.open("model.tar.gz", "w:gz") as tar:
    tar.add("xgboost_v1.json")

# Upload it to S3. Change the bucket name to yours. 
# You can add prefix to the object_name but it has to end with model.tar.gz.
bucket = "YOUR_BUCKET"
object_name = "model.tar.gz"
s3_client = boto3.client('s3')
s3_client.upload_file("model.tar.gz", bucket, object_name)
```

### Inference Script

Next, we create a python script for inference. This script is for Sagemaker only and thus your model hosting platform might require something different. But the workflow is generally similar: load model, preprocess input, perform prediction, and postprocess for output. 

In the script below, you will need to change the TigerGraph database configurations to match yours.


```python
%%file inference.py

import os
from subprocess import check_call, run, CalledProcessError
import sys

# Install pyTigerGraph as it is not built into the Sagemaker images.
if not os.environ.get("INSTALL_SUCCESS"):
    try:
        check_call(
            [ sys.executable, "pip", "install", "pyTigerGraph"]
        )
    except CalledProcessError:
        run(
            ["pip", "install", "pyTigerGraph"]
        )
    os.environ["INSTALL_SUCCESS"] = "True"

import pandas as pd
import numpy as np
from xgboost import XGBClassifier
import json
from pyTigerGraph import TigerGraphConnection
    
def model_fn(model_dir):
    # Load model
    model = XGBClassifier()
    model.load_model(os.path.join(model_dir, "xgboost_v1.json"))
    # Create DB connection. Change the configurations to yours.
    conn = TigerGraphConnection(
        host="http://DATABAE_ADDRESS",
        username="YOUR_USERNAME",
        password="YOUR_PASSWORD",
        graphname="Transaction_Fraud",
    )
    # Comment out the line below if your TigerGraph doesn't have token authentication enabled.
    conn.getToken()
    return (model, conn)

def input_fn(request_body, request_content_type):
    features = request_body.decode("utf-8")
    features = features.split(",")
    if len(features) == 1:
        features = features[0]
    return features

def predict_fn(input_object, model):
    xgb_model, conn = model
    features = [
            "mer_com_size", "cd_com_size", "mer_pagerank", "cd_pagerank", "shortest_path_length",
            "max_txn_amt_interval", "max_txn_cnt_interval", "cnt_repeated_card", "com_mer_txn_cnt", "com_cd_txn_cnt",
            "com_mer_txn_total_amt", "com_cd_txn_total_amt", "com_mer_txn_avg_amt", "com_cd_txn_avg_amt",
            "com_mer_txn_max_amt", "com_cd_txn_max_amt", "com_mer_txn_min_amt", "com_cd_txn_min_amt",
            "mer_cat_cnt", "mer_cat_total_amt", "mer_cat_avg_amt", "mer_cat_max_amt", "mer_cat_min_amt",
            "indegree", "outdegree", "amount", "age", "city_pop", "gender"
    ]
    transactions = conn.getVertexDataFrameById(
        "Payment_Transaction", 
        input_object,
        select=",".join(features+["transaction_time"]))
    # Feature engineering
    transactions["gender"] = transactions.gender.map({"F":0, "M":1})
    transactions["transaction_time"] = pd.to_datetime(transactions["transaction_time"])
    transactions['hour'] = transactions['transaction_time'].dt.hour
    transactions['day'] = transactions['transaction_time'].dt.day
    transactions['month'] = transactions['transaction_time'].dt.month
    transactions["extra_amount"]=np.log(transactions["amount"])-np.log(transactions["mer_cat_avg_amt"])
    train_features = features + ["extra_amount", "hour", "day", "month"]
    y = xgb_model.predict_proba(transactions[train_features])[:,1]
    return dict(zip(transactions.id, y.astype("float")))
    
def output_fn(prediction, content_type):
    resp = {
        "status": "ok", 
        "predictions": prediction
    }
    return json.dumps(resp).encode()
```

    Overwriting inference.py


### Cloud Deployment

Finanlly, we are going to deploy the model.


```python
from sagemaker.xgboost.model import XGBoostModel

# Change the role to your Sagemaker role and make sure it has access to Sagemaker and S3
xgboost_model = XGBoostModel(
    model_data="s3://"+bucket+"/"+object_name,
    role="YOUR_SAGEMAKER_ROLE",
    entry_point="inference.py",
    framework_version="1.7-1"
)

predictor = xgboost_model.deploy(
    instance_type='ml.c4.large',
    initial_instance_count=1
)
predictor.deserializer=sagemaker.base_deserializers.JSONDeserializer()
```

    --------!

Once the cell above finished execution successfully, you have the model deployed to cloud! During and after the execution, you can also check the status of your deployment on the Sagemaker console under the Inference section. 

Then you can make predictions using this predictor object returned by the deployment.


```python
# Predict a single transaction
predictor.predict("9b3792eff452fbfe8bf24403b7b02cdf")
```




    {'status': 'ok',
     'predictions': {'9b3792eff452fbfe8bf24403b7b02cdf': 0.01762988232076168}}




```python
# Predict a small list of transactions. 
# Note that it is a comma-separated string rather than a python list
predictor.predict("9b3792eff452fbfe8bf24403b7b02cdf,3ca3b37911069332c0fca7ab9f4e7c59")
```




    {'status': 'ok',
     'predictions': {'9b3792eff452fbfe8bf24403b7b02cdf': 0.01762988232076168,
      '3ca3b37911069332c0fca7ab9f4e7c59': 0.890177309513092}}



### Online Inference

Since the model is deployed to cloud, you should be able to access its prediction function in a different session rather than just using the returned predictor object. Indeed, Sagemaker creates an endpoint that you can request for predictions. However, as the endpoint is not open to public, you need proper authentication to access that endpoint. Sagemaker provides many SDKs to make that easy. Please see the [documentation](https://docs.aws.amazon.com/sagemaker/latest/APIReference/API_runtime_InvokeEndpoint.html) for details. Here we show how to create a new predictor from the deployed model in Python. 

First, we need to remember the endpoint name from our newly deployed model.


```python
predictor.endpoint_name
```




    'sagemaker-xgboost-2024-04-15-23-17-20-742'



Then, pretending we are are in a different Python session, we can create a predictor as the following. This predictor works the same way as the one returned by the deployment.


```python
from sagemaker.xgboost.model import XGBoostPredictor

predictor2 = XGBoostPredictor(
    'sagemaker-xgboost-2024-04-15-23-17-20-742',
    deserializer=sagemaker.base_deserializers.JSONDeserializer())
```

Finally, let's make some predictions!


```python
predictor2.predict("9b3792eff452fbfe8bf24403b7b02cdf")
```




    {'status': 'ok',
     'predictions': {'9b3792eff452fbfe8bf24403b7b02cdf': 0.01762988232076168}}



### Cleanup

As we walk through this tutorial on Sagemaker Inference, several artifacts are generated under your AWS account. If you don't need them any more, it is good habbit to delete them to save the cost. 


```python
import boto3

# Change the region to yours
sagemaker_client = boto3.client('sagemaker', region_name="us-west-1")
```


```python
# Change the endpoint name to yours
endpoint_name = 'sagemaker-xgboost-2024-04-15-23-17-20-742'

# Delete endpoint
sagemaker_client.delete_endpoint(EndpointName=endpoint_name)

# Delete model
response = sagemaker_client.describe_endpoint_config(EndpointConfigName=endpoint_name)
model_name = response['ProductionVariants'][0]['ModelName']
sagemaker_client.delete_model(ModelName=model_name)

# Delete endpoint configuration
sagemaker_client.delete_endpoint_config(EndpointConfigName=endpoint_name)
```
