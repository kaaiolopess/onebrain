#!/bin/bash
# Envia uma mensagem de atualização de status para a fila, sem precisar montar o JSON na mão.
# Uso: docker exec onebrain-localstack coupon-status <eventId> <couponId> <ACTIVE|INACTIVE>

if [ "$#" -ne 3 ]; then
  echo "Uso: coupon-status <eventId> <couponId> <ACTIVE|INACTIVE>"
  exit 1
fi

BODY="{\"eventId\":\"$1\",\"couponId\":\"$2\",\"status\":\"$3\"}"

# Grupo = cupom: a fila FIFO mantém a ordem das mensagens de um mesmo cupom.
# O id de deduplicação é aleatório de propósito: usando o eventId, o próprio SQS descartaria o reenvio
# (janela de 5 minutos) e não daria para ver a idempotência da aplicação funcionando.
awslocal sqs send-message \
  --queue-url http://localhost:4566/000000000000/coupon-status-queue.fifo \
  --message-group-id "$2" \
  --message-deduplication-id "$(cat /proc/sys/kernel/random/uuid)" \
  --message-body "$BODY" > /dev/null

echo "Mensagem enviada: $BODY"
