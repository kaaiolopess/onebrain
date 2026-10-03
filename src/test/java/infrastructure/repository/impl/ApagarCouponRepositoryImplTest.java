package infrastructure.repository.impl;

import com.onebrain.coupon.domain.exception.CouponNotFoundException;
import com.onebrain.coupon.domain.model.Coupon;
import com.onebrain.coupon.domain.model.CouponStatus;
import com.onebrain.coupon.infrastructure.exception.ApagarCouponRepositoryException;
import com.onebrain.coupon.infrastructure.repository.entity.CouponEntity;
import com.onebrain.coupon.infrastructure.repository.impl.ApagarCouponRepositoryImpl;
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
import org.springframework.dao.DataAccessException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApagarCouponRepositoryImplTest {

    @Mock
    private CouponRepository couponRepository;
    @InjectMocks
    private ApagarCouponRepositoryImpl repository;

    @Test
    @DisplayName("Deve fazer soft delete: atualiza o registro em vez de removê-lo")
    void deveFazerSoftDelete() {
        UUID id = UUID.randomUUID();
        Coupon apagado = CouponFactory.criarCouponApagado(id);
        when(couponRepository.findById(id)).thenReturn(Optional.of(CouponEntityFactory.criarCouponEntity(id)));

        repository.apagar(apagado);

        ArgumentCaptor<CouponEntity> captor = ArgumentCaptor.forClass(CouponEntity.class);
        verify(couponRepository, times(1)).save(captor.capture());
        assertEquals(CouponStatus.DELETED, captor.getValue().getStatus());
        assertEquals(apagado.getDeletedAt(), captor.getValue().getDeletedAt());
        assertEquals("ABC123", captor.getValue().getCode());
        verify(couponRepository, never()).deleteById(any());
        verify(couponRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Deve lançar CouponNotFoundException quando o ID não existe")
    void deveLancarNotFound() {
        UUID id = UUID.randomUUID();
        when(couponRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(CouponNotFoundException.class, () -> repository.apagar(CouponFactory.criarCouponApagado(id)));

        verify(couponRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar ApagarCouponRepositoryException quando o banco falha")
    void deveLancarRepositoryException() {
        UUID id = UUID.randomUUID();
        when(couponRepository.findById(id)).thenReturn(Optional.of(CouponEntityFactory.criarCouponEntity(id)));
        when(couponRepository.save(any(CouponEntity.class))).thenThrow(new DataAccessException("Database error") {});

        assertThrows(ApagarCouponRepositoryException.class, () -> repository.apagar(CouponFactory.criarCouponApagado(id)));
    }
}
