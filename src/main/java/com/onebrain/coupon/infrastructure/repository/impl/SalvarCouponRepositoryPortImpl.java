package com.onebrain.coupon.infrastructure.repository.impl;

import com.onebrain.coupon.domain.exception.CouponCodigoDuplicadoException;
import com.onebrain.coupon.domain.model.Coupon;
import com.onebrain.coupon.domain.port.repository.ISalvarCouponRepositoryPort;
import com.onebrain.coupon.infrastructure.exception.PersistenceCouponException;
import com.onebrain.coupon.infrastructure.repository.entity.CouponEntity;
import com.onebrain.coupon.infrastructure.repository.interfaces.CouponRepository;
import com.onebrain.coupon.infrastructure.repository.mapper.CouponEntityMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.Locale;

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
            if (violouCodigoUnico(e)) {
                log.warn("m=salvar, stg=END, msg=Código {} já cadastrado", coupon.getCode());
                throw new CouponCodigoDuplicadoException("Já existe um cupom com o código " + coupon.getCode());
            }
            throw new PersistenceCouponException("Falha ao salvar o cupom", e);
        } catch (Exception e) {
            throw new PersistenceCouponException("Falha ao salvar o cupom", e);
        }
    }

    // outras violações de integridade (ex.: valor que não cabe na coluna) não são código repetido
    private static boolean violouCodigoUnico(DataIntegrityViolationException e) {
        return e.getCause() instanceof ConstraintViolationException violacao
                && violacao.getConstraintName() != null
                && violacao.getConstraintName().toUpperCase(Locale.ROOT).contains(CouponEntity.UK_CODE);
    }
}
