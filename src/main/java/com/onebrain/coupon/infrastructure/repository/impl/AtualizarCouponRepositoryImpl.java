package com.onebrain.coupon.infrastructure.repository.impl;

import com.onebrain.coupon.domain.model.Coupon;
import com.onebrain.coupon.domain.port.repository.IAtualizarCouponRepositoryPort;
import com.onebrain.coupon.infrastructure.exception.AtualizarCouponRepositoryException;
import com.onebrain.coupon.infrastructure.repository.interfaces.CouponRepository;
import com.onebrain.coupon.infrastructure.repository.mapper.CouponEntityMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@Slf4j
public class AtualizarCouponRepositoryImpl implements IAtualizarCouponRepositoryPort {

    private final CouponRepository couponRepository;

    @Override
    public void atualizar(Coupon coupon) {
        try {
            couponRepository.save(CouponEntityMapper.toEntity(coupon));
            log.info("Cupom com id {} atualizado com sucesso", coupon.getId());
        } catch (DataAccessException e) {
            // inclui conflito de versão: outra alteração chegou antes, a operação deve ser repetida
            log.error("Erro ao atualizar cupom com id {}", coupon.getId(), e);
            throw new AtualizarCouponRepositoryException("Erro ao atualizar cupom", e);
        }
    }
}
