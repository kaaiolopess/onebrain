#!/bin/bash
# Envia uma mensagem de atualização de status para a fila, sem precisar montar o JSON na mão.
# Uso: docker exec onebrain-localstack coupon-status <eventId> <couponId> <ACTIVE|INACTIVE>

if [ "$#" -ne 3 ]; then
  echo "Uso: coupon-status <eventId> <couponId> <ACTIVE|INACTIVE>"
  exit 1
fi

BODY="{\"eventId\":\"$1\",\"couponId\":\"$2\",\"status\":\"$3\"}"

awslocal sqs send-message \
  --queue-url http://localhost:4566/000000000000/coupon-status-queue \
  --message-body "$BODY" > /dev/null

echo "Mensagem enviada: $BODY"
