package infrastructure.repository.factory;

import com.onebrain.coupon.domain.model.CouponStatus;
import com.onebrain.coupon.infrastructure.repository.entity.CouponEntity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public class CouponEntityFactory {

    public static CouponEntity criarCouponEntity(UUID id) {
        return CouponEntity.builder()
                .id(id)
                .code("ABC123")
                .description("Cupom de teste")
                .discountValue(new BigDecimal("0.8"))
                .expirationDate(OffsetDateTime.now().plusDays(30))
                .published(false)
                .redeemed(false)
                .status(CouponStatus.ACTIVE)
                .build();
    }
}
