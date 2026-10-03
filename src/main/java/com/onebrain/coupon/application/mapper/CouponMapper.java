package com.onebrain.coupon.application.mapper;

import com.onebrain.coupon.domain.model.Coupon;
import org.openapitools.model.CouponInput;
import org.openapitools.model.CouponResponse;
import org.openapitools.model.CouponStatus;

public class CouponMapper {

    public static Coupon toDomain(CouponInput input) {
        if (input == null) return null;

        return Coupon.criar(
                input.getCode(),
                input.getDescription(),
                input.getDiscountValue(),
                input.getExpirationDate(),
                input.getPublished()
        );
    }

    public static CouponResponse toCouponApi(Coupon coupon) {
        if (coupon == null) return null;

        return new CouponResponse()
                .id(coupon.getId())
                .code(coupon.getCode())
                .description(coupon.getDescription())
                .discountValue(coupon.getDiscountValue())
                .expirationDate(coupon.getExpirationDate())
                .status(CouponStatus.valueOf(coupon.getStatus().name()))
                .published(coupon.isPublished())
                .redeemed(coupon.isRedeemed());
    }
}
