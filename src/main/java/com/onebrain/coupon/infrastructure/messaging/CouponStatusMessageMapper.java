package com.onebrain.coupon.infrastructure.messaging;

import com.onebrain.coupon.application.useCase.command.AtualizarStatusCouponCommand;
import com.onebrain.coupon.domain.model.CouponStatus;

import java.util.UUID;

public class CouponStatusMessageMapper {

    public static AtualizarStatusCouponCommand toCommand(CouponStatusMessage message) {
        if (message == null) {
            throw new MensagemInvalidaException("Mensagem vazia");
        }
        if (message.eventId() == null || message.eventId().isBlank()) {
            throw new MensagemInvalidaException("eventId é obrigatório");
        }

        return new AtualizarStatusCouponCommand(
                message.eventId(),
                toCouponId(message.couponId()),
                toStatus(message.status())
        );
    }

    private static UUID toCouponId(String couponId) {
        try {
            return UUID.fromString(couponId);
        } catch (RuntimeException e) {
            throw new MensagemInvalidaException("couponId inválido: " + couponId);
        }
    }

    private static CouponStatus toStatus(String status) {
        try {
            return CouponStatus.valueOf(status);
        } catch (RuntimeException e) {
            throw new MensagemInvalidaException("status inválido: " + status);
        }
    }
}
