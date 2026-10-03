package infrastructure.repository.impl;

import com.onebrain.coupon.domain.exception.CouponNotFoundException;
import com.onebrain.coupon.domain.model.Coupon;
import com.onebrain.coupon.infrastructure.exception.BuscarCouponRepositoryException;
import com.onebrain.coupon.infrastructure.repository.impl.BuscarCouponRepositoryImpl;
import com.onebrain.coupon.infrastructure.repository.interfaces.CouponRepository;
import infrastructure.repository.factory.CouponEntityFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BuscarCouponRepositoryImplTest {

    @Mock
    private CouponRepository couponRepository;
    @InjectMocks
    private BuscarCouponRepositoryImpl repository;

    @Test
    @DisplayName("Deve retornar o cupom quando o ID existe")
    void deveRetornarCoupon() {
        UUID id = UUID.randomUUID();
        when(couponRepository.findById(id)).thenReturn(Optional.of(CouponEntityFactory.criarCouponEntity(id)));

        Coupon result = repository.buscarPorId(id);

        assertEquals(id, result.getId());
        assertEquals("ABC123", result.getCode());
    }

    @Test
    @DisplayName("Deve lançar CouponNotFoundException quando o ID não existe")
    void deveLancarNotFound() {
        UUID id = UUID.randomUUID();
        when(couponRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(CouponNotFoundException.class, () -> repository.buscarPorId(id));
    }

    @Test
    @DisplayName("Deve lançar BuscarCouponRepositoryException quando o banco falha")
    void deveLancarRepositoryException() {
        UUID id = UUID.randomUUID();
        when(couponRepository.findById(id)).thenThrow(new DataAccessException("Database error") {});

        assertThrows(BuscarCouponRepositoryException.class, () -> repository.buscarPorId(id));
    }
}
