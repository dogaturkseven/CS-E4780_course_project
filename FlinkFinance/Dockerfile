# Base Flink image
FROM flink:1.18.0-scala_2.12

# Set the working directory
WORKDIR /flink-job

# Copy the pre-built JAR file into the container
COPY target/flinkFinance-1.0-SNAPSHOT.jar /flink-job/flinkFinance-1.0-SNAPSHOT.jar


# Keep the container running idle
CMD ["tail", "-f", "/dev/null"]