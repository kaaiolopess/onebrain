package com.onebrain.coupon.application.useCase;

import com.onebrain.coupon.application.useCase.interfaces.IBuscarCouponPorIdUseCase;
import com.onebrain.coupon.domain.model.Coupon;
import com.onebrain.coupon.domain.port.repository.IBuscarCouponRepositoryPort;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
@AllArgsConstructor
public class BuscarCouponPorIdUseCase implements IBuscarCouponPorIdUseCase {

    private final IBuscarCouponRepositoryPort buscarCouponRepositoryPort;

    @Override
    public Coupon execute(UUID id) {
        log.info("m=execute, stg=INIT, msg=Buscando cupom com ID: {}", id);
        Coupon coupon = buscarCouponRepositoryPort.buscarPorId(id);
        coupon.garantirNaoApagado();
        return coupon;
    }
}
