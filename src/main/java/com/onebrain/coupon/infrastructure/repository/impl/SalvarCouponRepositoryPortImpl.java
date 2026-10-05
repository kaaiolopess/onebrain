package com.onebrain.coupon.infrastructure.repository.impl;

import com.onebrain.coupon.domain.exception.CouponCodigoDuplicadoException;
import com.onebrain.coupon.domain.model.Coupon;
import com.onebrain.coupon.domain.port.repository.ISalvarCouponRepositoryPort;
import com.onebrain.coupon.infrastructure.exception.PersistenceCouponException;
import com.onebrain.coupon.infrastructure.repository.interfaces.CouponRepository;
import com.onebrain.coupon.infrastructure.repository.mapper.CouponEntityMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@Slf4j
public class SalvarCouponRepositoryPortImpl implements ISalvarCouponRepositoryPort {

    private final CouponRepository couponRepository;

    @Override
    public Coupon salvar(Coupon coupon) {
        try {
            var couponSalvo = couponRepository.save(CouponEntityMapper.toEntity(coupon));
            log.info("m=salvar, stg=END, msg=Cupom salvo com sucesso: ID {}", couponSalvo.getId());
            return CouponEntityMapper.toDomain(couponSalvo);
        } catch (DataIntegrityViolationException e) {
            // os demais campos já foram validados pelo domínio: o que sobra é a constraint única do código
            log.warn("m=salvar, stg=END, msg=Código {} já cadastrado", coupon.getCode());
            throw new CouponCodigoDuplicadoException("Já existe um cupom com o código " + coupon.getCode());
        } catch (Exception e) {
            throw new PersistenceCouponException("Falha ao salvar o cupom", e);
        }
    }
}
