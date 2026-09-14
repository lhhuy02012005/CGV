#!/bin/sh

echo ">>> Waiting for Kafka to be fully ready..."
sleep 5

BOOTSTRAP="cgv-kafka-1:19092"

echo ">>> Creating Kafka topics..."

# Tạo topic chính (3 partition, replication-factor=3)
kafka-topics.sh --bootstrap-server $BOOTSTRAP --create --if-not-exists --topic user.registered   --partitions 3 --replication-factor 3
kafka-topics.sh --bootstrap-server $BOOTSTRAP --create --if-not-exists --topic booking.created   --partitions 3 --replication-factor 3
kafka-topics.sh --bootstrap-server $BOOTSTRAP --create --if-not-exists --topic booking.cancelled --partitions 3 --replication-factor 3
kafka-topics.sh --bootstrap-server $BOOTSTRAP --create --if-not-exists --topic payment.completed --partitions 3 --replication-factor 3
kafka-topics.sh --bootstrap-server $BOOTSTRAP --create --if-not-exists --topic payment.failed    --partitions 3 --replication-factor 3
kafka-topics.sh --bootstrap-server $BOOTSTRAP --create --if-not-exists --topic notification.send --partitions 3 --replication-factor 3

# Dead Letter Topics (DLT)
kafka-topics.sh --bootstrap-server $BOOTSTRAP --create --if-not-exists --topic user.registered.DLT   --partitions 3 --replication-factor 3
kafka-topics.sh --bootstrap-server $BOOTSTRAP --create --if-not-exists --topic booking.created.DLT   --partitions 3 --replication-factor 3
kafka-topics.sh --bootstrap-server $BOOTSTRAP --create --if-not-exists --topic payment.completed.DLT --partitions 3 --replication-factor 3
kafka-topics.sh --bootstrap-server $BOOTSTRAP --create --if-not-exists --topic notification.send.DLT --partitions 3 --replication-factor 3

echo ">>> All Kafka topics created successfully!"
kafka-topics.sh --bootstrap-server $BOOTSTRAP --list