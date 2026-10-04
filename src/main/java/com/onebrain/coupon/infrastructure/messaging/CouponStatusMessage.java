package com.onebrain.coupon.infrastructure.messaging;

/**
 * Corpo da mensagem da fila. Os campos são texto para que valores inválidos sejam tratados pela aplicação.
 */
public record CouponStatusMessage(
        String eventId,
        String couponId,
        String status
) {
}
