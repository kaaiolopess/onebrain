package domain.usecase;

import com.onebrain.coupon.domain.exception.CouponNotFoundException;
import com.onebrain.coupon.domain.model.Coupon;
import com.onebrain.coupon.domain.port.repository.IBuscarCouponRepositoryPort;
import com.onebrain.coupon.domain.useCase.BuscarCouponPorIdUseCase;
import domain.factory.CouponFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BuscarCouponPorIdUseCaseTest {

    @Mock
    private IBuscarCouponRepositoryPort buscarCouponRepositoryPort;
    @InjectMocks
    private BuscarCouponPorIdUseCase useCase;

    @Test
    @DisplayName("Deve retornar o cupom quando ele existe")
    void deveRetornarCoupon() {
        UUID id = UUID.randomUUID();
        Coupon coupon = CouponFactory.criarCouponSalvo(id);
        when(buscarCouponRepositoryPort.buscarPorId(id)).thenReturn(coupon);

        Coupon result = useCase.execute(id);

        assertEquals(coupon, result);
    }

    @Test
    @DisplayName("Deve tratar cupom apagado como não encontrado")
    void deveLancarNotFoundQuandoCouponApagado() {
        UUID id = UUID.randomUUID();
        when(buscarCouponRepositoryPort.buscarPorId(id)).thenReturn(CouponFactory.criarCouponApagado(id));

        assertThrows(CouponNotFoundException.class, () -> useCase.execute(id));
    }

    @Test
    @DisplayName("Deve propagar não encontrado quando o cupom não existe")
    void devePropagarNotFound() {
        UUID id = UUID.randomUUID();
        when(buscarCouponRepositoryPort.buscarPorId(id)).thenThrow(new CouponNotFoundException("não encontrado"));

        assertThrows(CouponNotFoundException.class, () -> useCase.execute(id));
    }
}
