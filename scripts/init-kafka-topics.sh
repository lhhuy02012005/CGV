#!/bin/sh
set -eu

# Tự động nạp đường dẫn chứa các file thực thi Kafka (.sh) trên cả Apache Kafka và Bitnami
export PATH=$PATH:/opt/kafka/bin:/opt/bitnami/kafka/bin

echo ">>> Waiting for Kafka to be fully ready..."
attempt=0
until kafka-topics.sh --bootstrap-server "${KAFKA_BOOTSTRAP_SERVERS:-localhost:9092}" --list >/dev/null 2>&1; do
    attempt=$((attempt + 1))
    [ "$attempt" -lt 30 ] || { echo "Kafka is not ready" >&2; exit 1; }
    sleep 2
done

BOOTSTRAP="${KAFKA_BOOTSTRAP_SERVERS:-localhost:9092}"

echo ">>> Creating Kafka topics on $BOOTSTRAP..."

# Kafka Connect internal state must be compacted, including existing auto-created topics.
for topic in "${CONNECT_CONFIG_TOPIC:-cinematrix_connect_configs}" "${CONNECT_OFFSET_TOPIC:-debezium_connect_offsets}" "${CONNECT_STATUS_TOPIC:-debezium_connect_statuses}"; do
    kafka-topics.sh --bootstrap-server "$BOOTSTRAP" --create --if-not-exists \
        --topic "$topic" --partitions 1 --replication-factor 3 --config cleanup.policy=compact
    kafka-configs.sh --bootstrap-server "$BOOTSTRAP" --entity-type topics \
        --entity-name "$topic" --alter --add-config cleanup.policy=compact
done

# 1. Các Topics chính (3 partitions, replication-factor=3)
kafka-topics.sh --bootstrap-server $BOOTSTRAP --create --if-not-exists --topic user.registered    --partitions 3 --replication-factor 3
kafka-topics.sh --bootstrap-server $BOOTSTRAP --create --if-not-exists --topic booking.created    --partitions 3 --replication-factor 3
kafka-topics.sh --bootstrap-server $BOOTSTRAP --create --if-not-exists --topic booking.confirmed  --partitions 3 --replication-factor 3
kafka-topics.sh --bootstrap-server $BOOTSTRAP --create --if-not-exists --topic booking.cancelled  --partitions 3 --replication-factor 3
kafka-topics.sh --bootstrap-server $BOOTSTRAP --create --if-not-exists --topic payment.completed  --partitions 3 --replication-factor 3
kafka-topics.sh --bootstrap-server $BOOTSTRAP --create --if-not-exists --topic payment.failed     --partitions 3 --replication-factor 3
kafka-topics.sh --bootstrap-server $BOOTSTRAP --create --if-not-exists --topic notification.send  --partitions 3 --replication-factor 3

# 2. Dead Letter Queue Topics (DLT)
kafka-topics.sh --bootstrap-server $BOOTSTRAP --create --if-not-exists --topic user.registered.DLT    --partitions 3 --replication-factor 3
kafka-topics.sh --bootstrap-server $BOOTSTRAP --create --if-not-exists --topic booking.created.DLT    --partitions 3 --replication-factor 3
kafka-topics.sh --bootstrap-server $BOOTSTRAP --create --if-not-exists --topic booking.confirmed.DLT  --partitions 3 --replication-factor 3
kafka-topics.sh --bootstrap-server $BOOTSTRAP --create --if-not-exists --topic payment.completed.DLT  --partitions 3 --replication-factor 3
kafka-topics.sh --bootstrap-server $BOOTSTRAP --create --if-not-exists --topic notification.send.DLT  --partitions 3 --replication-factor 3

echo ">>> All Kafka topics created successfully!"
kafka-topics.sh --bootstrap-server $BOOTSTRAP --list
