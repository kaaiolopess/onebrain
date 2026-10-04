package com.onebrain.coupon.application.useCase.command;

import com.onebrain.coupon.domain.model.CouponStatus;

import java.util.UUID;

public record AtualizarStatusCouponCommand(
        String eventId,
        UUID couponId,
        CouponStatus status
) {
}
