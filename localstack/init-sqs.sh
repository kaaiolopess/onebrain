#!/bin/bash
# Cria a fila de atualização de status e a DLQ no LocalStack.
set -e

awslocal sqs create-queue --queue-name coupon-status-dlq

DLQ_ARN=$(awslocal sqs get-queue-attributes \
  --queue-url http://sqs.us-east-1.localhost.localstack.cloud:4566/000000000000/coupon-status-dlq \
  --attribute-names QueueArn --query 'Attributes.QueueArn' --output text)

# depois de 3 tentativas sem sucesso, a mensagem vai para a DLQ
awslocal sqs create-queue --queue-name coupon-status-queue \
  --attributes "{\"RedrivePolicy\":\"{\\\"deadLetterTargetArn\\\":\\\"${DLQ_ARN}\\\",\\\"maxReceiveCount\\\":\\\"3\\\"}\",\"VisibilityTimeout\":\"30\"}"

echo "Filas criadas: coupon-status-queue e coupon-status-dlq"
