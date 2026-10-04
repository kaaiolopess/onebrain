package com.onebrain.coupon.application.useCase.interfaces;

import com.onebrain.coupon.application.useCase.command.CriarCouponCommand;
import com.onebrain.coupon.domain.model.Coupon;

public interface ICriarCouponUseCase {
    Coupon execute(CriarCouponCommand command);
}
