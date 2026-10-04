package com.onebrain.coupon.application.useCase.command;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record CriarCouponCommand(
        String code,
        String description,
        BigDecimal discountValue,
        OffsetDateTime expirationDate,
        Boolean published
) {
}
