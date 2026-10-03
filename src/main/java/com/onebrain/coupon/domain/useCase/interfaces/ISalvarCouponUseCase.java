package com.onebrain.coupon.domain.useCase.interfaces;

import com.onebrain.coupon.domain.model.Coupon;

public interface ISalvarCouponUseCase {
    Coupon execute(Coupon coupon);
}
