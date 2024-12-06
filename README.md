# CS-E4780_course_project
CS-E4780 Course Project - Detecting Trading Trends in Financial Tick Data


----------------------------------------------------------------

SETUP

1) Install Java11 (OpenJDK11) and Maven.

2) Clone the course project to your workspace

3) Run the following commands:
```
cd FlinkFinance
mvn clean install package
```

2) Run the docker compose to start the application:
```
cd ..
docker-compose up --build
```

3) When the containers are up, access the Grafana UI via the link below:

http://localhost:3000/

4) Enter the credentials:
```
username: admin
password: admin
```

5) At Grafana home page, Connection -> Add new Connection 

- Choose InfluxDB -> Add new datasource 
- Fill in HTTP field URL: http://influxdb:8086
- Fill in InfluxDB Details field Database: trading_db
- Click Save & Test


5) Run  the following commands:
```
curl -XPOST http://localhost:8086/query --data-urlencode "q=CREATE DATABASE trading_db"
docker exec -it broker kafka-topics --bootstrap-server broker:9092 --create --topic my_topic --partitions 4 --replication-factor 1
docker exec -it flink-job flink run -m flink-jobmanager:8081 -c flinkFinance.DataStreamJob /flink-job/flinkFinance-1.0-SNAPSHOT.jar
docker exec -it kafka-producer python /app/producer.py
```

6) To see the Dashboard, go to [http://localhost:3000/dashboards](http://localhost:3000/dashboards)






