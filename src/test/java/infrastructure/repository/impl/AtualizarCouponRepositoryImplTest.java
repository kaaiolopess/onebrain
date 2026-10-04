package infrastructure.repository.impl;

import com.onebrain.coupon.domain.model.Coupon;
import com.onebrain.coupon.domain.model.CouponStatus;
import com.onebrain.coupon.infrastructure.exception.AtualizarCouponRepositoryException;
import com.onebrain.coupon.infrastructure.repository.entity.CouponEntity;
import com.onebrain.coupon.infrastructure.repository.impl.AtualizarCouponRepositoryImpl;
import com.onebrain.coupon.infrastructure.repository.interfaces.CouponRepository;
import domain.factory.CouponFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AtualizarCouponRepositoryImplTest {

    @Mock
    private CouponRepository couponRepository;
    @InjectMocks
    private AtualizarCouponRepositoryImpl repository;

    @Test
    @DisplayName("Deve salvar o cupom com o novo status e a versão lida")
    void deveAtualizarCoupon() {
        UUID id = UUID.randomUUID();
        Coupon coupon = CouponFactory.criarCouponSalvo(id);
        coupon.alterarStatus(CouponStatus.INACTIVE);

        repository.atualizar(coupon);

        ArgumentCaptor<CouponEntity> captor = ArgumentCaptor.forClass(CouponEntity.class);
        verify(couponRepository).save(captor.capture());
        assertEquals(id, captor.getValue().getId());
        assertEquals(CouponStatus.INACTIVE, captor.getValue().getStatus());
        assertEquals(coupon.getVersion(), captor.getValue().getVersion());
    }

    @Test
    @DisplayName("Deve lançar AtualizarCouponRepositoryException em conflito de versão")
    void deveLancarExceptionEmConflitoDeVersao() {
        when(couponRepository.save(any(CouponEntity.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException(CouponEntity.class, "id"));

        assertThrows(AtualizarCouponRepositoryException.class,
                () -> repository.atualizar(CouponFactory.criarCouponSalvo(UUID.randomUUID())));
    }
}
