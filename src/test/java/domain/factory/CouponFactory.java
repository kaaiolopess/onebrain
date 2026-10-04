package domain.factory;

import com.onebrain.coupon.domain.model.Coupon;
import com.onebrain.coupon.domain.model.CouponStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public class CouponFactory {

    public static Coupon criarCouponNovo() {
        return Coupon.criar("ABC-123", "Cupom de teste", new BigDecimal("0.8"), dataFutura(), false);
    }

    public static Coupon criarCouponSalvo(UUID id) {
        return criarCoupon(id, CouponStatus.ACTIVE, null);
    }

    public static Coupon criarCouponApagado(UUID id) {
        return criarCoupon(id, CouponStatus.DELETED, OffsetDateTime.now().minusDays(1));
    }

    public static OffsetDateTime dataFutura() {
        return OffsetDateTime.now().plusDays(30);
    }

    private static Coupon criarCoupon(UUID id, CouponStatus status, OffsetDateTime deletedAt) {
        return Coupon.restaurar(id, "ABC123", "Cupom de teste", new BigDecimal("0.8"), dataFutura(),
                false, false, status, deletedAt, 0L);
    }
}
