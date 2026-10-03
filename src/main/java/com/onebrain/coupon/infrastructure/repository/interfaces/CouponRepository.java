package com.onebrain.coupon.infrastructure.repository.interfaces;

import com.onebrain.coupon.infrastructure.repository.entity.CouponEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CouponRepository extends JpaRepository<CouponEntity, UUID> {
}
