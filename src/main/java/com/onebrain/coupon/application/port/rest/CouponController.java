package com.onebrain.coupon.application.port.rest;

import com.onebrain.coupon.application.mapper.CouponMapper;
import com.onebrain.coupon.domain.useCase.interfaces.IApagarCouponUseCase;
import com.onebrain.coupon.domain.useCase.interfaces.IBuscarCouponPorIdUseCase;
import com.onebrain.coupon.domain.useCase.interfaces.ISalvarCouponUseCase;
import lombok.AllArgsConstructor;
import org.openapitools.api.CouponApi;
import org.openapitools.model.CouponInput;
import org.openapitools.model.CouponResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@AllArgsConstructor
public class CouponController implements CouponApi {

    private final ISalvarCouponUseCase salvarCouponUseCase;
    private final IBuscarCouponPorIdUseCase buscarCouponPorIdUseCase;
    private final IApagarCouponUseCase apagarCouponUseCase;

    @Override
    public ResponseEntity<CouponResponse> criarCoupon(CouponInput couponInput) {
        var novoCoupon = salvarCouponUseCase.execute(CouponMapper.toDomain(couponInput));
        return ResponseEntity.status(HttpStatus.CREATED).body(CouponMapper.toCouponApi(novoCoupon));
    }

    @Override
    public ResponseEntity<CouponResponse> buscarCouponPorId(UUID id) {
        var coupon = buscarCouponPorIdUseCase.execute(id);
        return ResponseEntity.ok(CouponMapper.toCouponApi(coupon));
    }

    @Override
    public ResponseEntity<Void> apagarCoupon(UUID id) {
        apagarCouponUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }
}
