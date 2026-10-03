package domain.usecase;

import com.onebrain.coupon.domain.model.Coupon;
import com.onebrain.coupon.domain.port.repository.ISalvarCouponRepositoryPort;
import com.onebrain.coupon.domain.useCase.SalvarCouponUseCase;
import domain.factory.CouponFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalvarCouponUseCaseTest {

    @Mock
    private ISalvarCouponRepositoryPort salvarCouponRepositoryPort;
    @InjectMocks
    private SalvarCouponUseCase useCase;

    @Test
    @DisplayName("Deve salvar o cupom")
    void deveSalvarCoupon() {
        Coupon novo = CouponFactory.criarCouponNovo();
        Coupon salvo = CouponFactory.criarCouponSalvo(UUID.randomUUID());
        when(salvarCouponRepositoryPort.salvar(novo)).thenReturn(salvo);

        Coupon result = useCase.execute(novo);

        assertEquals(salvo, result);
        verify(salvarCouponRepositoryPort, times(1)).salvar(novo);
    }
}
