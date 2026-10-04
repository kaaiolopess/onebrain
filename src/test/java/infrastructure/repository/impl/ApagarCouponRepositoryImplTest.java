package infrastructure.repository.impl;

import com.onebrain.coupon.domain.exception.CouponJaApagadoException;
import com.onebrain.coupon.domain.model.Coupon;
import com.onebrain.coupon.domain.model.CouponStatus;
import com.onebrain.coupon.infrastructure.exception.ApagarCouponRepositoryException;
import com.onebrain.coupon.infrastructure.repository.entity.CouponEntity;
import com.onebrain.coupon.infrastructure.repository.impl.ApagarCouponRepositoryImpl;
import com.onebrain.coupon.infrastructure.repository.interfaces.CouponRepository;
import domain.factory.CouponFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

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

        repository.apagar(apagado);

        ArgumentCaptor<CouponEntity> captor = ArgumentCaptor.forClass(CouponEntity.class);
        verify(couponRepository, times(1)).save(captor.capture());
        assertEquals(id, captor.getValue().getId());
        assertEquals(CouponStatus.DELETED, captor.getValue().getStatus());
        assertEquals(apagado.getDeletedAt(), captor.getValue().getDeletedAt());
        assertEquals(apagado.getVersion(), captor.getValue().getVersion());
        assertEquals("ABC123", captor.getValue().getCode());
        verify(couponRepository, never()).deleteById(any());
        verify(couponRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Deve lançar CouponJaApagadoException quando outra requisição apagou o cupom antes")
    void deveLancarJaApagadoEmConflitoDeVersao() {
        when(couponRepository.save(any(CouponEntity.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException(CouponEntity.class, "id"));

        assertThrows(CouponJaApagadoException.class,
                () -> repository.apagar(CouponFactory.criarCouponApagado(UUID.randomUUID())));
    }

    @Test
    @DisplayName("Deve lançar ApagarCouponRepositoryException quando o banco falha")
    void deveLancarRepositoryException() {
        when(couponRepository.save(any(CouponEntity.class))).thenThrow(new DataAccessException("Database error") {});

        assertThrows(ApagarCouponRepositoryException.class,
                () -> repository.apagar(CouponFactory.criarCouponApagado(UUID.randomUUID())));
    }
}
