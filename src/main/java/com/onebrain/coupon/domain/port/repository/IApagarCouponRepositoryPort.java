package com.onebrain.coupon.domain.port.repository;

import com.onebrain.coupon.domain.model.Coupon;

public interface IApagarCouponRepositoryPort {
    void apagar(Coupon coupon);
}
