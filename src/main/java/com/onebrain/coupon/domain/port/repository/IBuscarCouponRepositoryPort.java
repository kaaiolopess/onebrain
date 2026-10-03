package com.onebrain.coupon.domain.port.repository;

import com.onebrain.coupon.domain.model.Coupon;

import java.util.UUID;

public interface IBuscarCouponRepositoryPort {
    Coupon buscarPorId(UUID id);
}
