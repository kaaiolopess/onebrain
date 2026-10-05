package com.onebrain.coupon.infrastructure.repository.interfaces;

import com.onebrain.coupon.infrastructure.repository.entity.CouponEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Repository
public interface CouponRepository extends JpaRepository<CouponEntity, UUID> {

    /**
     * Soft delete em um único update, que só alcança o cupom ainda não apagado.
     * A versão é incrementada para que uma alteração feita a partir de uma leitura anterior seja rejeitada.
     *
     * @return quantidade de registros alterados: 0 quando o cupom já estava apagado
     */
    @Transactional
    @Modifying
    @Query("""
            update CouponEntity c
               set c.status = com.onebrain.coupon.domain.model.CouponStatus.DELETED,
                   c.deletedAt = :deletedAt,
                   c.updated = local_datetime,
                   c.version = c.version + 1
             where c.id = :id
               and c.status <> com.onebrain.coupon.domain.model.CouponStatus.DELETED
            """)
    int marcarComoApagado(@Param("id") UUID id, @Param("deletedAt") OffsetDateTime deletedAt);
}
