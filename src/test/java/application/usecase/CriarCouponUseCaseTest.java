package application.usecase;

import com.onebrain.coupon.application.useCase.CriarCouponUseCase;
import com.onebrain.coupon.application.useCase.command.CriarCouponCommand;
import com.onebrain.coupon.domain.exception.RegraNegocioException;
import com.onebrain.coupon.domain.model.Coupon;
import com.onebrain.coupon.domain.model.CouponStatus;
import com.onebrain.coupon.domain.port.repository.ISalvarCouponRepositoryPort;
import domain.factory.CouponFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CriarCouponUseCaseTest {

    @Mock
    private ISalvarCouponRepositoryPort salvarCouponRepositoryPort;
    @InjectMocks
    private CriarCouponUseCase useCase;

    @Test
    @DisplayName("Deve criar o cupom pelo domínio e salvar")
    void deveCriarESalvarCoupon() {
        Coupon salvo = CouponFactory.criarCouponSalvo(UUID.randomUUID());
        when(salvarCouponRepositoryPort.salvar(any(Coupon.class))).thenReturn(salvo);

        Coupon result = useCase.execute(new CriarCouponCommand(
                "ABC-123", "Cupom de teste", new BigDecimal("0.8"), CouponFactory.dataFutura(), true));

        ArgumentCaptor<Coupon> captor = ArgumentCaptor.forClass(Coupon.class);
        verify(salvarCouponRepositoryPort, times(1)).salvar(captor.capture());
        assertEquals("ABC123", captor.getValue().getCode());
        assertEquals(CouponStatus.ACTIVE, captor.getValue().getStatus());
        assertEquals(true, captor.getValue().isPublished());
        assertEquals(salvo, result);
    }

    @Test
    @DisplayName("Não deve salvar quando o domínio rejeita o cupom")
    void naoDeveSalvarQuandoRegraDeNegocioViolada() {
        var command = new CriarCouponCommand(
                "ABC123", "Cupom de teste", new BigDecimal("0.8"), OffsetDateTime.now().minusDays(1), false);

        assertThrows(RegraNegocioException.class, () -> useCase.execute(command));

        verify(salvarCouponRepositoryPort, never()).salvar(any());
    }
}
