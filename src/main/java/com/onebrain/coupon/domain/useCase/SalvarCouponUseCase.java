package com.onebrain.coupon.domain.useCase;

import com.onebrain.coupon.domain.model.Coupon;
import com.onebrain.coupon.domain.port.repository.ISalvarCouponRepositoryPort;
import com.onebrain.coupon.domain.useCase.interfaces.ISalvarCouponUseCase;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@AllArgsConstructor
public class SalvarCouponUseCase implements ISalvarCouponUseCase {

    private final ISalvarCouponRepositoryPort salvarCouponRepositoryPort;

    @Override
    public Coupon execute(Coupon coupon) {
        log.info("m=execute, stg=INIT, msg=Salvando cupom: {}", coupon);
        return salvarCouponRepositoryPort.salvar(coupon);
    }
}
