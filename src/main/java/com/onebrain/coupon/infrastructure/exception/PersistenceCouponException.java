package com.onebrain.coupon.infrastructure.exception;

public class PersistenceCouponException extends RuntimeException {

    public PersistenceCouponException(String message, Throwable cause) {
        super(message, cause);
    }
}
