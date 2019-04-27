FROM python:2

RUN pip install --upgrade pip
RUN pip install tweepy
RUN pip install --upgrade google-api-python-client
RUN pip install python-dateutil

ADD twitter-to-pubsub.py /twitter-to-pubsub.py
ADD controller.py /controller.py
ADD utils.py /utils.py

CMD python controller.py