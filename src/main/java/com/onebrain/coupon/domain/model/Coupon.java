package com.onebrain.coupon.domain.model;

import com.onebrain.coupon.domain.exception.CouponJaApagadoException;
import com.onebrain.coupon.domain.exception.CouponNotFoundException;
import com.onebrain.coupon.domain.exception.RegraNegocioException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@ToString
@EqualsAndHashCode
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Coupon {

    public static final int TAMANHO_CODIGO = 6;
    public static final BigDecimal DESCONTO_MINIMO = new BigDecimal("0.5");

    private final UUID id;
    private final String code;
    private final String description;
    private final BigDecimal discountValue;
    private final OffsetDateTime expirationDate;
    private final boolean published;
    private final boolean redeemed;
    private CouponStatus status;
    private OffsetDateTime deletedAt;
    // versão do registro lida do banco, usada para detectar alterações concorrentes ao salvar
    private final Long version;

    /**
     * Cria um novo cupom aplicando as regras de negócio do cadastro.
     */
    public static Coupon criar(String code, String description, BigDecimal discountValue,
                               OffsetDateTime expirationDate, Boolean published) {
        return new Coupon(
                null,
                normalizarCodigo(code),
                validarDescricao(description),
                validarDesconto(discountValue),
                validarExpiracao(expirationDate),
                Boolean.TRUE.equals(published),
                false,
                CouponStatus.ACTIVE,
                null,
                null
        );
    }

    /**
     * Reconstrói um cupom já persistido, sem reaplicar as regras de criação.
     */
    public static Coupon restaurar(UUID id, String code, String description, BigDecimal discountValue,
                                   OffsetDateTime expirationDate, boolean published, boolean redeemed,
                                   CouponStatus status, OffsetDateTime deletedAt, Long version) {
        return new Coupon(id, code, description, discountValue, expirationDate, published, redeemed, status,
                deletedAt, version);
    }

    /**
     * Soft delete: o cupom é apenas marcado como apagado, preservando os dados do cadastro.
     */
    public void apagar() {
        if (isApagado()) {
            throw new CouponJaApagadoException("Cupom já foi apagado");
        }
        this.status = CouponStatus.DELETED;
        this.deletedAt = OffsetDateTime.now();
    }

    /**
     * Ativa ou inativa o cupom. Pedir o status em que ele já está não altera nada.
     */
    public void alterarStatus(CouponStatus novoStatus) {
        if (novoStatus == null) {
            throw new RegraNegocioException("O novo status do cupom é obrigatório");
        }
        if (isApagado()) {
            throw new CouponJaApagadoException("Cupom apagado não pode ter o status alterado");
        }
        if (novoStatus == CouponStatus.DELETED) {
            throw new RegraNegocioException("Um cupom só pode ser apagado pela operação de exclusão");
        }
        this.status = novoStatus;
    }

    /**
     * Um cupom apagado não pode mais ser consultado.
     */
    public void garantirNaoApagado() {
        if (isApagado()) {
            throw new CouponNotFoundException("Cupom foi apagado");
        }
    }

    public boolean isApagado() {
        return CouponStatus.DELETED == status;
    }

    private static String normalizarCodigo(String code) {
        if (code == null || code.isBlank()) {
            throw new RegraNegocioException("O código do cupom é obrigatório");
        }
        String codigoLimpo = code.replaceAll("[^A-Za-z0-9]", "");
        if (codigoLimpo.length() != TAMANHO_CODIGO) {
            throw new RegraNegocioException(
                    "O código do cupom deve ter exatamente " + TAMANHO_CODIGO + " caracteres alfanuméricos");
        }
        return codigoLimpo;
    }

    private static String validarDescricao(String description) {
        if (description == null || description.isBlank()) {
            throw new RegraNegocioException("A descrição do cupom é obrigatória");
        }
        return description;
    }

    private static BigDecimal validarDesconto(BigDecimal discountValue) {
        if (discountValue == null) {
            throw new RegraNegocioException("O valor de desconto do cupom é obrigatório");
        }
        if (discountValue.compareTo(DESCONTO_MINIMO) < 0) {
            throw new RegraNegocioException("O valor de desconto do cupom deve ser no mínimo " + DESCONTO_MINIMO);
        }
        return discountValue;
    }

    private static OffsetDateTime validarExpiracao(OffsetDateTime expirationDate) {
        if (expirationDate == null) {
            throw new RegraNegocioException("A data de expiração do cupom é obrigatória");
        }
        if (expirationDate.isBefore(OffsetDateTime.now())) {
            throw new RegraNegocioException("A data de expiração do cupom não pode estar no passado");
        }
        return expirationDate;
    }
}
