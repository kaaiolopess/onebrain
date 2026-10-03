package com.onebrain.coupon.domain.useCase.interfaces;

import com.onebrain.coupon.domain.model.Coupon;

import java.util.UUID;

public interface IBuscarCouponPorIdUseCase {
    Coupon execute(UUID id);
}
