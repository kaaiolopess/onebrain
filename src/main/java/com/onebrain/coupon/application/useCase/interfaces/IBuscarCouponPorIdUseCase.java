package com.onebrain.coupon.application.useCase.interfaces;

import com.onebrain.coupon.domain.model.Coupon;

import java.util.UUID;

public interface IBuscarCouponPorIdUseCase {
    Coupon execute(UUID id);
}
