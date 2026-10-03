package com.onebrain.coupon.infrastructure.repository.impl;

import com.onebrain.coupon.domain.exception.CouponNotFoundException;
import com.onebrain.coupon.domain.model.Coupon;
import com.onebrain.coupon.domain.port.repository.IApagarCouponRepositoryPort;
import com.onebrain.coupon.infrastructure.exception.ApagarCouponRepositoryException;
import com.onebrain.coupon.infrastructure.repository.interfaces.CouponRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@Slf4j
public class ApagarCouponRepositoryImpl implements IApagarCouponRepositoryPort {

    private final CouponRepository couponRepository;

    @Override
    public void apagar(Coupon coupon) {
        try {
            var couponEntity = couponRepository.findById(coupon.getId())
                    .orElseThrow(() -> new CouponNotFoundException("Cupom não encontrado com id: " + coupon.getId()));

            // soft delete: o registro permanece no banco, apenas marcado como apagado
            couponEntity.setStatus(coupon.getStatus());
            couponEntity.setDeletedAt(coupon.getDeletedAt());
            couponRepository.save(couponEntity);
            log.info("Cupom com id {} apagado com sucesso", coupon.getId());
        } catch (DataAccessException e) {
            log.error("Erro ao apagar cupom com id {}", coupon.getId(), e);
            throw new ApagarCouponRepositoryException("Erro ao apagar cupom", e);
        }
    }
}
