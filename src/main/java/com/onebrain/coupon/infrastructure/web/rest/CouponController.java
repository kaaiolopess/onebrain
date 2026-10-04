package com.onebrain.coupon.infrastructure.web.rest;

import com.onebrain.coupon.application.useCase.interfaces.IApagarCouponUseCase;
import com.onebrain.coupon.application.useCase.interfaces.IBuscarCouponPorIdUseCase;
import com.onebrain.coupon.application.useCase.interfaces.ICriarCouponUseCase;
import com.onebrain.coupon.infrastructure.web.filter.MdcFilter;
import com.onebrain.coupon.infrastructure.web.mapper.CouponMapper;
import lombok.AllArgsConstructor;
import org.openapitools.api.CouponApi;
import org.openapitools.model.CouponInput;
import org.openapitools.model.CouponResponse;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@AllArgsConstructor
public class CouponController implements CouponApi {

    private final ICriarCouponUseCase criarCouponUseCase;
    private final IBuscarCouponPorIdUseCase buscarCouponPorIdUseCase;
    private final IApagarCouponUseCase apagarCouponUseCase;

    @Override
    public ResponseEntity<CouponResponse> criarCoupon(CouponInput couponInput) {
        var novoCoupon = criarCouponUseCase.execute(CouponMapper.toCommand(couponInput));
        MDC.put(MdcFilter.COUPON_ID, String.valueOf(novoCoupon.getId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(CouponMapper.toCouponApi(novoCoupon));
    }

    @Override
    public ResponseEntity<CouponResponse> buscarCouponPorId(UUID id) {
        MDC.put(MdcFilter.COUPON_ID, String.valueOf(id));
        var coupon = buscarCouponPorIdUseCase.execute(id);
        return ResponseEntity.ok(CouponMapper.toCouponApi(coupon));
    }

    @Override
    public ResponseEntity<Void> apagarCoupon(UUID id) {
        MDC.put(MdcFilter.COUPON_ID, String.valueOf(id));
        apagarCouponUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }
}
