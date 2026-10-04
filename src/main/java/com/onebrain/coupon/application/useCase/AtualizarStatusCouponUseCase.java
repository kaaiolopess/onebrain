package com.onebrain.coupon.application.useCase;

import com.onebrain.coupon.application.port.IIdempotenciaPort;
import com.onebrain.coupon.application.useCase.command.AtualizarStatusCouponCommand;
import com.onebrain.coupon.application.useCase.interfaces.IAtualizarStatusCouponUseCase;
import com.onebrain.coupon.domain.model.Coupon;
import com.onebrain.coupon.domain.port.repository.IAtualizarCouponRepositoryPort;
import com.onebrain.coupon.domain.port.repository.IBuscarCouponRepositoryPort;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@AllArgsConstructor
public class AtualizarStatusCouponUseCase implements IAtualizarStatusCouponUseCase {

    private final IIdempotenciaPort idempotenciaPort;
    private final IBuscarCouponRepositoryPort buscarCouponRepositoryPort;
    private final IAtualizarCouponRepositoryPort atualizarCouponRepositoryPort;

    @Override
    public void execute(AtualizarStatusCouponCommand command) {
        log.info("m=execute, stg=INIT, msg=Atualizando status do cupom: {}", command);
        if (!idempotenciaPort.iniciar(command.eventId())) {
            log.info("m=execute, stg=END, msg=Evento {} já processado, ignorando", command.eventId());
            return;
        }

        try {
            Coupon coupon = buscarCouponRepositoryPort.buscarPorId(command.couponId());
            coupon.alterarStatus(command.status());
            atualizarCouponRepositoryPort.atualizar(coupon);
            idempotenciaPort.concluir(command.eventId());
        } catch (RuntimeException e) {
            // libera o evento para que uma nova entrega da mensagem possa processá-lo
            idempotenciaPort.cancelar(command.eventId());
            throw e;
        }
    }
}
