package com.onebrain.coupon.infrastructure.repository.mapper;

import com.onebrain.coupon.domain.model.Coupon;
import com.onebrain.coupon.infrastructure.repository.entity.CouponEntity;

public class CouponEntityMapper {

    public static CouponEntity toEntity(Coupon coupon) {
        if (coupon == null) return null;

        return CouponEntity.builder()
                .id(coupon.getId())
                .code(coupon.getCode())
                .description(coupon.getDescription())
                .discountValue(coupon.getDiscountValue())
                .expirationDate(coupon.getExpirationDate())
                .published(coupon.isPublished())
                .redeemed(coupon.isRedeemed())
                .status(coupon.getStatus())
                .deletedAt(coupon.getDeletedAt())
                .build();
    }

    public static Coupon toDomain(CouponEntity entity) {
        if (entity == null) return null;

        return Coupon.restaurar(
                entity.getId(),
                entity.getCode(),
                entity.getDescription(),
                entity.getDiscountValue().stripTrailingZeros(),
                entity.getExpirationDate(),
                Boolean.TRUE.equals(entity.getPublished()),
                Boolean.TRUE.equals(entity.getRedeemed()),
                entity.getStatus(),
                entity.getDeletedAt()
        );
    }
}
