package com.onebrain.coupon.infrastructure.repository.impl;

import com.onebrain.coupon.domain.exception.CouponNotFoundException;
import com.onebrain.coupon.domain.model.Coupon;
import com.onebrain.coupon.domain.port.repository.IBuscarCouponRepositoryPort;
import com.onebrain.coupon.infrastructure.exception.BuscarCouponRepositoryException;
import com.onebrain.coupon.infrastructure.repository.interfaces.CouponRepository;
import com.onebrain.coupon.infrastructure.repository.mapper.CouponEntityMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@AllArgsConstructor
@Slf4j
public class BuscarCouponRepositoryImpl implements IBuscarCouponRepositoryPort {

    private final CouponRepository couponRepository;

    @Override
    public Coupon buscarPorId(UUID id) {
        try {
            return couponRepository.findById(id)
                    .map(CouponEntityMapper::toDomain)
                    .orElseThrow(() -> new CouponNotFoundException("Cupom não encontrado com id: " + id));
        } catch (DataAccessException e) {
            log.error("Erro ao buscar cupom com id {}", id, e);
            throw new BuscarCouponRepositoryException("Erro ao buscar cupom", e);
        }
    }
}
