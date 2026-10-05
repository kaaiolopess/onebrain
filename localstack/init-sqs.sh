#!/bin/bash
# Cria a fila de atualização de status e a DLQ no LocalStack.
# As filas são FIFO: as mensagens de um mesmo cupom (MessageGroupId) são entregues na ordem de envio.
set -e

# a DLQ de uma fila FIFO também precisa ser FIFO
awslocal sqs create-queue --queue-name coupon-status-dlq.fifo --attributes FifoQueue=true

DLQ_ARN=$(awslocal sqs get-queue-attributes \
  --queue-url http://sqs.us-east-1.localhost.localstack.cloud:4566/000000000000/coupon-status-dlq.fifo \
  --attribute-names QueueArn --query 'Attributes.QueueArn' --output text)

# depois de 3 tentativas sem sucesso, a mensagem vai para a DLQ
awslocal sqs create-queue --queue-name coupon-status-queue.fifo \
  --attributes "{\"FifoQueue\":\"true\",\"RedrivePolicy\":\"{\\\"deadLetterTargetArn\\\":\\\"${DLQ_ARN}\\\",\\\"maxReceiveCount\\\":\\\"3\\\"}\",\"VisibilityTimeout\":\"30\"}"

echo "Filas criadas: coupon-status-queue.fifo e coupon-status-dlq.fifo"
