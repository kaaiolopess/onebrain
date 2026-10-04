package com.onebrain.coupon.application.useCase.interfaces;

import com.onebrain.coupon.application.useCase.command.AtualizarStatusCouponCommand;

public interface IAtualizarStatusCouponUseCase {
    void execute(AtualizarStatusCouponCommand command);
}
