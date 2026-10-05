package infrastructure.repository.impl;

import com.onebrain.coupon.domain.exception.CouponCodigoDuplicadoException;
import com.onebrain.coupon.domain.model.Coupon;
import com.onebrain.coupon.domain.model.CouponStatus;
import com.onebrain.coupon.infrastructure.exception.PersistenceCouponException;
import com.onebrain.coupon.infrastructure.repository.entity.CouponEntity;
import com.onebrain.coupon.infrastructure.repository.impl.SalvarCouponRepositoryPortImpl;
import com.onebrain.coupon.infrastructure.repository.interfaces.CouponRepository;
import domain.factory.CouponFactory;
import infrastructure.repository.factory.CouponEntityFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalvarCouponRepositoryPortImplTest {

    @Mock
    private CouponRepository couponRepository;
    @InjectMocks
    private SalvarCouponRepositoryPortImpl repository;

    @Test
    @DisplayName("Deve salvar o cupom e retornar o domínio com ID")
    void deveSalvarCoupon() {
        UUID id = UUID.randomUUID();
        when(couponRepository.save(any(CouponEntity.class))).thenReturn(CouponEntityFactory.criarCouponEntity(id));

        Coupon result = repository.salvar(CouponFactory.criarCouponNovo());

        ArgumentCaptor<CouponEntity> captor = ArgumentCaptor.forClass(CouponEntity.class);
        verify(couponRepository, times(1)).save(captor.capture());
        assertEquals("ABC123", captor.getValue().getCode());
        assertEquals(CouponStatus.ACTIVE, captor.getValue().getStatus());
        assertEquals(id, result.getId());
        assertEquals("ABC123", result.getCode());
    }

    @Test
    @DisplayName("Deve lançar CouponCodigoDuplicadoException quando o banco rejeita o código repetido")
    void deveLancarCodigoDuplicado() {
        when(couponRepository.save(any(CouponEntity.class)))
                .thenThrow(new DataIntegrityViolationException("UK_COUPONS_CODE"));

        assertThrows(CouponCodigoDuplicadoException.class, () -> repository.salvar(CouponFactory.criarCouponNovo()));
    }

    @Test
    @DisplayName("Deve lançar PersistenceCouponException quando o banco falha")
    void deveLancarPersistenceException() {
        when(couponRepository.save(any(CouponEntity.class))).thenThrow(new RuntimeException("Database error"));

        assertThrows(PersistenceCouponException.class, () -> repository.salvar(CouponFactory.criarCouponNovo()));
    }
}
