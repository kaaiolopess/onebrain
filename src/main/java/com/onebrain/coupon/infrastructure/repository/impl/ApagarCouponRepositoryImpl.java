package com.onebrain.coupon.infrastructure.repository.impl;

import com.onebrain.coupon.domain.exception.CouponJaApagadoException;
import com.onebrain.coupon.domain.model.Coupon;
import com.onebrain.coupon.domain.port.repository.IApagarCouponRepositoryPort;
import com.onebrain.coupon.infrastructure.exception.ApagarCouponRepositoryException;
import com.onebrain.coupon.infrastructure.repository.interfaces.CouponRepository;
import com.onebrain.coupon.infrastructure.repository.mapper.CouponEntityMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@Slf4j
public class ApagarCouponRepositoryImpl implements IApagarCouponRepositoryPort {

    private final CouponRepository couponRepository;

    @Override
    public void apagar(Coupon coupon) {
        try {
            // soft delete: o registro permanece no banco, apenas marcado como apagado
            couponRepository.save(CouponEntityMapper.toEntity(coupon));
            log.info("Cupom com id {} apagado com sucesso", coupon.getId());
        } catch (OptimisticLockingFailureException e) {
            // outra requisição alterou o cupom depois da leitura; a única alteração possível é o delete
            log.warn("Cupom com id {} foi apagado por outra requisição", coupon.getId());
            throw new CouponJaApagadoException("Cupom já foi apagado");
        } catch (DataAccessException e) {
            log.error("Erro ao apagar cupom com id {}", coupon.getId(), e);
            throw new ApagarCouponRepositoryException("Erro ao apagar cupom", e);
        }
    }
}
