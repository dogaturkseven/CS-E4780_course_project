# Use the official Python image as the base
FROM python:3.11-slim

# Set the working directory inside the container
WORKDIR /app

# Copy the producer script and requirements file
COPY producer.py /app/producer.py
COPY requirements.txt /app/requirements.txt

# Install dependencies
RUN pip install --no-cache-dir -r requirements.txt

#"dummy" process that keeps the container active
CMD ["tail", "-f", "/dev/null"]