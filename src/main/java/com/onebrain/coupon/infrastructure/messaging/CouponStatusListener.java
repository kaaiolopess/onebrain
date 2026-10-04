package com.onebrain.coupon.infrastructure.messaging;

import com.onebrain.coupon.application.useCase.interfaces.IAtualizarStatusCouponUseCase;
import com.onebrain.coupon.domain.exception.CouponNotFoundException;
import com.onebrain.coupon.domain.exception.RegraNegocioException;
import com.onebrain.coupon.infrastructure.web.filter.MdcFilter;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Consome a fila de atualização de status de cupom.
 * Erros que não se resolvem repetindo (mensagem inválida, regra de negócio) são descartados;
 * qualquer outro erro é relançado, e a mensagem volta para a fila até ir para a DLQ.
 */
@Component
@ConditionalOnProperty(name = "coupon.sqs.enabled", havingValue = "true")
@AllArgsConstructor
@Slf4j
public class CouponStatusListener {

    private final IAtualizarStatusCouponUseCase atualizarStatusCouponUseCase;

    @SqsListener("${coupon.sqs.queue}")
    public void receber(CouponStatusMessage message) {
        try {
            if (message != null) {
                MDC.put(MdcFilter.CORRELATION_ID, String.valueOf(message.eventId()));
                MDC.put(MdcFilter.COUPON_ID, String.valueOf(message.couponId()));
            }
            atualizarStatusCouponUseCase.execute(CouponStatusMessageMapper.toCommand(message));
            log.info("m=receber, stg=END, msg=Mensagem processada");
        } catch (MensagemInvalidaException | RegraNegocioException | CouponNotFoundException e) {
            log.warn("m=receber, stg=END, msg=Mensagem descartada: {}", e.getMessage());
        } finally {
            MDC.clear();
        }
    }
}
