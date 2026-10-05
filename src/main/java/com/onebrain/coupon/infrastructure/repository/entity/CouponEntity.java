package com.onebrain.coupon.infrastructure.repository.entity;

import com.onebrain.coupon.domain.model.Coupon;
import com.onebrain.coupon.domain.model.CouponStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "COUPONS", uniqueConstraints = @UniqueConstraint(name = CouponEntity.UK_CODE, columnNames = "code"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponEntity {

    public static final String UK_CODE = "UK_COUPONS_CODE";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 6)
    private String code;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = Coupon.DESCONTO_DIGITOS_INTEIROS + Coupon.DESCONTO_CASAS_DECIMAIS,
            scale = Coupon.DESCONTO_CASAS_DECIMAIS)
    private BigDecimal discountValue;

    @Column(nullable = false)
    private OffsetDateTime expirationDate;

    @Column(nullable = false)
    private Boolean published;

    @Column(nullable = false)
    private Boolean redeemed;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private CouponStatus status;

    private OffsetDateTime deletedAt;

    // lock otimista: salvar com uma versão desatualizada é rejeitado pelo banco
    @Version
    private Long version;

    @Column(updatable = false)
    private LocalDateTime created;

    private LocalDateTime updated;

    @PrePersist
    public void prePersist() {
        created = LocalDateTime.now();
        updated = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updated = LocalDateTime.now();
    }
}
