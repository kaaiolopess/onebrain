package com.onebrain.coupon.domain.port.repository;

import com.onebrain.coupon.domain.model.Coupon;

public interface ISalvarCouponRepositoryPort {
    Coupon salvar(Coupon coupon);
}
