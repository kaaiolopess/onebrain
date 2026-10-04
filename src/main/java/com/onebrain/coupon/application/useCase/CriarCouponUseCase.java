package com.onebrain.coupon.application.useCase;

import com.onebrain.coupon.application.useCase.command.CriarCouponCommand;
import com.onebrain.coupon.application.useCase.interfaces.ICriarCouponUseCase;
import com.onebrain.coupon.domain.model.Coupon;
import com.onebrain.coupon.domain.port.repository.ISalvarCouponRepositoryPort;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@AllArgsConstructor
public class CriarCouponUseCase implements ICriarCouponUseCase {

    private final ISalvarCouponRepositoryPort salvarCouponRepositoryPort;

    @Override
    public Coupon execute(CriarCouponCommand command) {
        log.info("m=execute, stg=INIT, msg=Criando cupom: {}", command);
        Coupon coupon = Coupon.criar(
                command.code(),
                command.description(),
                command.discountValue(),
                command.expirationDate(),
                command.published()
        );
        return salvarCouponRepositoryPort.salvar(coupon);
    }
}
