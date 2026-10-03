package com.onebrain.coupon.domain.useCase;

import com.onebrain.coupon.domain.model.Coupon;
import com.onebrain.coupon.domain.port.repository.IApagarCouponRepositoryPort;
import com.onebrain.coupon.domain.port.repository.IBuscarCouponRepositoryPort;
import com.onebrain.coupon.domain.useCase.interfaces.IApagarCouponUseCase;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
@AllArgsConstructor
public class ApagarCouponUseCase implements IApagarCouponUseCase {

    private final IBuscarCouponRepositoryPort buscarCouponRepositoryPort;
    private final IApagarCouponRepositoryPort apagarCouponRepositoryPort;

    @Override
    public void execute(UUID id) {
        log.info("m=execute, stg=INIT, msg=Apagando cupom com ID: {}", id);
        Coupon coupon = buscarCouponRepositoryPort.buscarPorId(id);
        coupon.apagar();
        apagarCouponRepositoryPort.apagar(coupon);
    }
}
