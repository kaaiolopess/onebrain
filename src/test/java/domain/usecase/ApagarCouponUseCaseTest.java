package domain.usecase;

import com.onebrain.coupon.domain.exception.CouponJaApagadoException;
import com.onebrain.coupon.domain.exception.CouponNotFoundException;
import com.onebrain.coupon.domain.model.Coupon;
import com.onebrain.coupon.domain.model.CouponStatus;
import com.onebrain.coupon.domain.port.repository.IApagarCouponRepositoryPort;
import com.onebrain.coupon.domain.port.repository.IBuscarCouponRepositoryPort;
import com.onebrain.coupon.domain.useCase.ApagarCouponUseCase;
import domain.factory.CouponFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApagarCouponUseCaseTest {

    @Mock
    private IBuscarCouponRepositoryPort buscarCouponRepositoryPort;
    @Mock
    private IApagarCouponRepositoryPort apagarCouponRepositoryPort;
    @InjectMocks
    private ApagarCouponUseCase useCase;

    @Test
    @DisplayName("Deve marcar o cupom como apagado e persistir")
    void deveApagarCoupon() {
        UUID id = UUID.randomUUID();
        when(buscarCouponRepositoryPort.buscarPorId(id)).thenReturn(CouponFactory.criarCouponSalvo(id));

        useCase.execute(id);

        ArgumentCaptor<Coupon> captor = ArgumentCaptor.forClass(Coupon.class);
        verify(apagarCouponRepositoryPort, times(1)).apagar(captor.capture());
        assertEquals(id, captor.getValue().getId());
        assertEquals(CouponStatus.DELETED, captor.getValue().getStatus());
        assertNotNull(captor.getValue().getDeletedAt());
    }

    @Test
    @DisplayName("Não deve apagar cupom já apagado")
    void naoDeveApagarCouponJaApagado() {
        UUID id = UUID.randomUUID();
        when(buscarCouponRepositoryPort.buscarPorId(id)).thenReturn(CouponFactory.criarCouponApagado(id));

        assertThrows(CouponJaApagadoException.class, () -> useCase.execute(id));

        verify(apagarCouponRepositoryPort, never()).apagar(any());
    }

    @Test
    @DisplayName("Não deve apagar cupom inexistente")
    void naoDeveApagarCouponInexistente() {
        UUID id = UUID.randomUUID();
        when(buscarCouponRepositoryPort.buscarPorId(id)).thenThrow(new CouponNotFoundException("não encontrado"));

        assertThrows(CouponNotFoundException.class, () -> useCase.execute(id));

        verify(apagarCouponRepositoryPort, never()).apagar(any());
    }
}
