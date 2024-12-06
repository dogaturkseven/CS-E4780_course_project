import requests
import csv
import json
import time
import pandas as pd
import copy
from datetime import datetime, timedelta

from confluent_kafka import SerializingProducer, KafkaError

KAFKA_BROKER = 'broker:29092' #'localhost:9092'
TOPIC = "my_topic"
TIME_SCALE = 1


def create_producer():
    for attempt in range(5):  # Retry up to 5 times
        try:
            # Create SerializingProducer with required configurations
            producer = SerializingProducer({
                'bootstrap.servers': KAFKA_BROKER,
                'key.serializer': lambda k, ctx: k.encode('utf-8'),
                'value.serializer': lambda v, ctx: json.dumps(v).encode('utf-8'),  # Use the value serializer
                'queue.buffering.max.messages': 100000,  # Max messages in the producer's buffer
            })
            producer.flush()
            print("Connected to Kafka broker")
            return producer
        except KafkaError as e:
            print(f"Connection attempt {attempt + 1} failed: {e}")
            time.sleep(5)
    raise Exception("Failed to connect to Kafka after several attempts.")


# Fetch CSV data
def fetch_csv_data(url):
    #response = requests.get(url, stream=True)
    
    #if response.status_code == 200:
    #    print("Streaming CSV data...")
    #    return response.iter_lines(decode_unicode=True)  # Stream lines as an iterator
    #else:
    #    raise Exception(f"Failed to fetch CSV data, status code: {response.status_code}")
    """
    if response.status_code == 200:
        return response.text
    else:
        raise Exception(f"Failed to fetch CSV data, status code: {response.status_code}")
    """


# Function to parse and send CSV data to Kafka
def process_and_send_to_kafka(csv_data, producer, day):
    #csv_data_stream = csv_data.splitlines()
    #filtered_lines = [line for line in csv_data if not line.startswith('#')]  # Skip lines starting with #
    #filtered_lines = (line for line in csv_data_stream if not line.startswith('#'))


    #reader = csv.DictReader(filtered_lines)  # Read filtered lines with DictReader
    i = 1
    trade_start = 0
    # start_time = datetime.now()
    print("Streaming day {} CSV data".format(day))

    response = requests.get(url, stream=True)
    comments_done = False
    column_names = []
    id_index = 0 
    sectype_index = 0
    last_index = 0
    trading_time_index = 0
    day_json = "{}.11.2021".format(day)
    # Check if the request was successful
    if response.status_code == 200:
        # Decode the response content line by line and use a CSV reader
        for line in response.iter_lines():
            # Decode bytes to string and skip empty lines
            if line:
                decoded_line = line.decode('utf-8')
                
                if (not comments_done):
                    if decoded_line.startswith("#"):
                        continue
                    list_of_items = decoded_line.split(',')
                    if (list_of_items[0] == "ID" and comments_done == False):
                        comments_done = True
                        column_names = copy.deepcopy(list_of_items)
                        id_index = column_names.index('ID')  
                        sectype_index = column_names.index('SecType')  
                        last_index = column_names.index('Last') 
                        trading_time_index = column_names.index('Trading time')
                        continue
                
                list_of_items = decoded_line.split(',')
                
                #row = next(csv.reader(decoded_line))
                
                filtered_row = {
                    'id': list_of_items[id_index],  # Map 'ID' to 'id'
                    'secType': list_of_items[sectype_index],  # Map 'SecType' to 'secType'
                    'lastTradePrice': list_of_items[last_index],  # Map 'Last' to 'lastTradePrice'
                    'tradingTime': list_of_items[trading_time_index],  # Map 'Trading time' to 'tradingTime'
                    'tradingDate': day_json  # BUNU DA AYARLA
                }
                
                try:
                    last_trade_price = float(filtered_row['lastTradePrice']) if filtered_row['lastTradePrice'] else None
                except ValueError:
                    last_trade_price = None

                if filtered_row['tradingTime'] and last_trade_price:
                    trading_time_str = f"{filtered_row['tradingTime']}"
                    trading_time = datetime.strptime(trading_time_str, "%H:%M:%S.%f").time()

                    if (i == 1):
                        start_time = datetime.now()
                        trade_start = trading_time_seconds = (trading_time.hour * 3600 +
                                                                trading_time.minute * 60 +
                                                                trading_time.second +
                                                                trading_time.microsecond / 1_000_000)

                    # Calculate the delay relative to the start time
                    elapsed_since_start = (datetime.now() - start_time).total_seconds() * TIME_SCALE
                    trading_time_seconds = (trading_time.hour * 3600 +
                                            trading_time.minute * 60 +
                                            trading_time.second +
                                            trading_time.microsecond / 1_000_000)
                    delay = trading_time_seconds - trade_start - elapsed_since_start

                    # If the target time has not yet passed, wait until it does
                    if delay > 0:
                        log_message = f"Waiting for {delay / TIME_SCALE:.2f} seconds to send row with tradingTime {trading_time}"
                        print(log_message)
                        #log_file.write(log_message + "\n")
                        time.sleep(delay / TIME_SCALE)

                    instrument_id = filtered_row['id']  # Use the new 'id' key for partitioning
                    log_message = f"Sending {i}th row: {json.dumps(filtered_row)}"  # Print each row for confirmation
                    print(log_message)
                    #log_file.write(log_message + "\n")
                    i += 1

                    # Send the message with the ID as the partition key
                    try:
                        producer.produce(TOPIC, key=instrument_id, value=filtered_row)
                        producer.poll(0)  # Allow the producer to handle events
                    except KafkaError as e:
                        if e.code() == KafkaError.QUEUE_FULL:
                            # If the queue is full, wait before retrying
                            log_message = "Queue is full, backing off"
                            print(log_message)
                            #log_file.write(log_message + "\n")
                            time.sleep(1)
            else:
                print(f"Failed to fetch the file. Status code: {response.status_code}")
        


if __name__ == "__main__":
    try:
        producer = create_producer()

        start_time = time.time()
        # Fetch data and process it
        for day in ["08", "09", "10", "11", "12", "13", "14"]:
            url = "https://zenodo.org/api/records/6382482/files/debs2022-gc-trading-day-{}-11-21.csv/content".format(day)
            #csv_data_stream = fetch_csv_data(url)
            #csv_data_stream = fetch_csv_data(url)
            

            process_and_send_to_kafka(url, producer, day)

            end_time = time.time()
            total_time = end_time - start_time
            print(f"Time taken to process and send the data: {total_time:.2f} seconds")

            print("Finished sending data for day {}. Keeping producer alive...".format(day))
        try:
            while True:
                time.sleep(1)  # Prevent CPU overuse
        except KeyboardInterrupt:
            print("Exiting producer gracefully.")
            producer.flush()

        # Close producer
        producer.flush()
    except Exception as e:
        print(f"An unexpected error occurred: {e}")
