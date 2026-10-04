package domain.model;

import com.onebrain.coupon.domain.exception.CouponJaApagadoException;
import com.onebrain.coupon.domain.exception.CouponNotFoundException;
import com.onebrain.coupon.domain.exception.RegraNegocioException;
import com.onebrain.coupon.domain.model.Coupon;
import com.onebrain.coupon.domain.model.CouponStatus;
import domain.factory.CouponFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CouponTest {

    private static final String DESCRICAO = "Cupom de teste";
    private static final BigDecimal DESCONTO = new BigDecimal("0.8");

    @Test
    @DisplayName("Deve criar cupom ativo, não publicado e não resgatado")
    void deveCriarCouponValido() {
        OffsetDateTime expiracao = CouponFactory.dataFutura();

        Coupon coupon = Coupon.criar("ABC123", DESCRICAO, DESCONTO, expiracao, null);

        assertNull(coupon.getId());
        assertEquals("ABC123", coupon.getCode());
        assertEquals(DESCRICAO, coupon.getDescription());
        assertEquals(DESCONTO, coupon.getDiscountValue());
        assertEquals(expiracao, coupon.getExpirationDate());
        assertEquals(CouponStatus.ACTIVE, coupon.getStatus());
        assertFalse(coupon.isPublished());
        assertFalse(coupon.isRedeemed());
        assertFalse(coupon.isApagado());
        assertNull(coupon.getDeletedAt());
    }

    @Test
    @DisplayName("Deve permitir criar cupom já publicado")
    void deveCriarCouponPublicado() {
        Coupon coupon = Coupon.criar("ABC123", DESCRICAO, DESCONTO, CouponFactory.dataFutura(), true);

        assertTrue(coupon.isPublished());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ABC-123", "A!B@C#1$2%3", " ABC 123 ", "ABC_12.3"})
    @DisplayName("Deve remover caracteres especiais do código mantendo 6 caracteres")
    void deveRemoverCaracteresEspeciaisDoCodigo(String codigo) {
        Coupon coupon = Coupon.criar(codigo, DESCRICAO, DESCONTO, CouponFactory.dataFutura(), false);

        assertEquals("ABC123", coupon.getCode());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ABC12", "ABC1234", "AB-12", "ABC-1234", "!@#$%&"})
    @DisplayName("Não deve criar cupom cujo código limpo não tenha 6 caracteres")
    void naoDeveCriarCouponComCodigoDeTamanhoInvalido(String codigo) {
        assertThrows(RegraNegocioException.class,
                () -> Coupon.criar(codigo, DESCRICAO, DESCONTO, CouponFactory.dataFutura(), false));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Não deve criar cupom sem código")
    void naoDeveCriarCouponSemCodigo(String codigo) {
        assertThrows(RegraNegocioException.class,
                () -> Coupon.criar(codigo, DESCRICAO, DESCONTO, CouponFactory.dataFutura(), false));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Não deve criar cupom sem descrição")
    void naoDeveCriarCouponSemDescricao(String descricao) {
        assertThrows(RegraNegocioException.class,
                () -> Coupon.criar("ABC123", descricao, DESCONTO, CouponFactory.dataFutura(), false));
    }

    @Test
    @DisplayName("Deve aceitar o valor mínimo de desconto de 0.5")
    void deveAceitarDescontoMinimo() {
        Coupon coupon = Coupon.criar("ABC123", DESCRICAO, new BigDecimal("0.5"), CouponFactory.dataFutura(), false);

        assertEquals(new BigDecimal("0.5"), coupon.getDiscountValue());
    }

    @Test
    @DisplayName("Deve aceitar desconto alto, pois não há máximo predeterminado")
    void deveAceitarDescontoSemMaximo() {
        BigDecimal desconto = new BigDecimal("999999999.99");

        Coupon coupon = Coupon.criar("ABC123", DESCRICAO, desconto, CouponFactory.dataFutura(), false);

        assertEquals(desconto, coupon.getDiscountValue());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.49", "0", "-1"})
    @DisplayName("Não deve criar cupom com desconto abaixo de 0.5")
    void naoDeveCriarCouponComDescontoAbaixoDoMinimo(String desconto) {
        assertThrows(RegraNegocioException.class,
                () -> Coupon.criar("ABC123", DESCRICAO, new BigDecimal(desconto), CouponFactory.dataFutura(), false));
    }

    @Test
    @DisplayName("Não deve criar cupom sem valor de desconto")
    void naoDeveCriarCouponSemDesconto() {
        assertThrows(RegraNegocioException.class,
                () -> Coupon.criar("ABC123", DESCRICAO, null, CouponFactory.dataFutura(), false));
    }

    @Test
    @DisplayName("Não deve criar cupom com data de expiração no passado")
    void naoDeveCriarCouponComExpiracaoNoPassado() {
        OffsetDateTime passado = OffsetDateTime.now().minusSeconds(1);

        assertThrows(RegraNegocioException.class,
                () -> Coupon.criar("ABC123", DESCRICAO, DESCONTO, passado, false));
    }

    @Test
    @DisplayName("Não deve criar cupom sem data de expiração")
    void naoDeveCriarCouponSemExpiracao() {
        assertThrows(RegraNegocioException.class,
                () -> Coupon.criar("ABC123", DESCRICAO, DESCONTO, null, false));
    }

    @Test
    @DisplayName("Deve apagar cupom com soft delete, preservando os dados do cadastro")
    void deveApagarCoupon() {
        UUID id = UUID.randomUUID();
        Coupon coupon = CouponFactory.criarCouponSalvo(id);

        coupon.apagar();

        assertTrue(coupon.isApagado());
        assertEquals(CouponStatus.DELETED, coupon.getStatus());
        assertNotNull(coupon.getDeletedAt());
        assertEquals(id, coupon.getId());
        assertEquals("ABC123", coupon.getCode());
        assertEquals(DESCRICAO, coupon.getDescription());
        assertEquals(DESCONTO, coupon.getDiscountValue());
    }

    @Test
    @DisplayName("Não deve apagar cupom já apagado")
    void naoDeveApagarCouponJaApagado() {
        Coupon coupon = CouponFactory.criarCouponApagado(UUID.randomUUID());
        OffsetDateTime apagadoEm = coupon.getDeletedAt();

        assertThrows(CouponJaApagadoException.class, coupon::apagar);
        assertEquals(apagadoEm, coupon.getDeletedAt());
    }

    @Test
    @DisplayName("Deve inativar e reativar um cupom")
    void deveAlterarStatus() {
        Coupon coupon = CouponFactory.criarCouponSalvo(UUID.randomUUID());

        coupon.alterarStatus(CouponStatus.INACTIVE);
        assertEquals(CouponStatus.INACTIVE, coupon.getStatus());

        coupon.alterarStatus(CouponStatus.ACTIVE);
        assertEquals(CouponStatus.ACTIVE, coupon.getStatus());
    }

    @Test
    @DisplayName("Pedir o status atual não altera o cupom")
    void deveManterStatusQuandoIgual() {
        Coupon coupon = CouponFactory.criarCouponSalvo(UUID.randomUUID());

        coupon.alterarStatus(CouponStatus.ACTIVE);

        assertEquals(CouponStatus.ACTIVE, coupon.getStatus());
    }

    @Test
    @DisplayName("Não deve alterar o status de um cupom apagado")
    void naoDeveAlterarStatusDeCouponApagado() {
        Coupon coupon = CouponFactory.criarCouponApagado(UUID.randomUUID());

        assertThrows(CouponJaApagadoException.class, () -> coupon.alterarStatus(CouponStatus.ACTIVE));
        assertEquals(CouponStatus.DELETED, coupon.getStatus());
    }

    @Test
    @DisplayName("Não deve apagar um cupom pela alteração de status")
    void naoDeveApagarPelaAlteracaoDeStatus() {
        Coupon coupon = CouponFactory.criarCouponSalvo(UUID.randomUUID());

        assertThrows(RegraNegocioException.class, () -> coupon.alterarStatus(CouponStatus.DELETED));
        assertEquals(CouponStatus.ACTIVE, coupon.getStatus());
        assertNull(coupon.getDeletedAt());
    }

    @Test
    @DisplayName("Não deve alterar o status sem informar o novo status")
    void naoDeveAlterarStatusSemNovoStatus() {
        Coupon coupon = CouponFactory.criarCouponSalvo(UUID.randomUUID());

        assertThrows(RegraNegocioException.class, () -> coupon.alterarStatus(null));
    }

    @Test
    @DisplayName("Cupom ativo pode ser consultado")
    void devePermitirConsultarCouponAtivo() {
        Coupon coupon = CouponFactory.criarCouponSalvo(UUID.randomUUID());

        assertDoesNotThrow(coupon::garantirNaoApagado);
    }

    @Test
    @DisplayName("Cupom apagado não pode ser consultado")
    void naoDevePermitirConsultarCouponApagado() {
        Coupon coupon = CouponFactory.criarCouponApagado(UUID.randomUUID());

        assertThrows(CouponNotFoundException.class, coupon::garantirNaoApagado);
    }

    @Test
    @DisplayName("Deve permitir apagar cupom mesmo depois de expirado")
    void deveApagarCouponExpirado() {
        Coupon coupon = Coupon.restaurar(UUID.randomUUID(), "ABC123", DESCRICAO, DESCONTO,
                OffsetDateTime.now().minusDays(10), true, false, CouponStatus.ACTIVE, null, 0L);

        coupon.apagar();

        assertTrue(coupon.isApagado());
    }
}
