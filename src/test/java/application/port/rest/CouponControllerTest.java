package application.port.rest;

import com.onebrain.coupon.application.port.rest.CouponController;
import com.onebrain.coupon.domain.model.Coupon;
import com.onebrain.coupon.domain.useCase.interfaces.IApagarCouponUseCase;
import com.onebrain.coupon.domain.useCase.interfaces.IBuscarCouponPorIdUseCase;
import com.onebrain.coupon.domain.useCase.interfaces.ISalvarCouponUseCase;
import domain.factory.CouponFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapitools.model.CouponInput;
import org.openapitools.model.CouponResponse;
import org.openapitools.model.CouponStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CouponControllerTest {

    @Mock
    private ISalvarCouponUseCase salvarCouponUseCase;
    @Mock
    private IBuscarCouponPorIdUseCase buscarCouponPorIdUseCase;
    @Mock
    private IApagarCouponUseCase apagarCouponUseCase;

    @InjectMocks
    private CouponController controller;

    @Test
    @DisplayName("Deve criar cupom e retornar CREATED")
    void deveCriarCoupon() {
        UUID id = UUID.randomUUID();
        when(salvarCouponUseCase.execute(any())).thenReturn(CouponFactory.criarCouponSalvo(id));

        var input = new CouponInput("ABC-123", "Cupom de teste", new BigDecimal("0.8"), CouponFactory.dataFutura());
        ResponseEntity<CouponResponse> response = controller.criarCoupon(input);

        ArgumentCaptor<Coupon> captor = ArgumentCaptor.forClass(Coupon.class);
        verify(salvarCouponUseCase).execute(captor.capture());
        assertEquals("ABC123", captor.getValue().getCode());
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(id, response.getBody().getId());
        assertEquals("ABC123", response.getBody().getCode());
        assertEquals(CouponStatus.ACTIVE, response.getBody().getStatus());
    }

    @Test
    @DisplayName("Deve buscar cupom por id")
    void deveBuscarCouponPorId() {
        UUID id = UUID.randomUUID();
        when(buscarCouponPorIdUseCase.execute(id)).thenReturn(CouponFactory.criarCouponSalvo(id));

        ResponseEntity<CouponResponse> response = controller.buscarCouponPorId(id);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(id, response.getBody().getId());
    }

    @Test
    @DisplayName("Deve apagar cupom e retornar NO_CONTENT")
    void deveApagarCoupon() {
        UUID id = UUID.randomUUID();
        doNothing().when(apagarCouponUseCase).execute(id);

        ResponseEntity<Void> response = controller.apagarCoupon(id);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(apagarCouponUseCase).execute(id);
    }
}
