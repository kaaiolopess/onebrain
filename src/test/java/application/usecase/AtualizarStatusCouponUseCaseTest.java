package application.usecase;

import com.onebrain.coupon.application.exception.EventoEmProcessamentoException;
import com.onebrain.coupon.application.port.IIdempotenciaPort;
import com.onebrain.coupon.application.useCase.AtualizarStatusCouponUseCase;
import com.onebrain.coupon.application.useCase.command.AtualizarStatusCouponCommand;
import com.onebrain.coupon.domain.exception.CouponJaApagadoException;
import com.onebrain.coupon.domain.exception.CouponNotFoundException;
import com.onebrain.coupon.domain.model.Coupon;
import com.onebrain.coupon.domain.model.CouponStatus;
import com.onebrain.coupon.domain.port.repository.IAtualizarCouponRepositoryPort;
import com.onebrain.coupon.domain.port.repository.IBuscarCouponRepositoryPort;
import domain.factory.CouponFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AtualizarStatusCouponUseCaseTest {

    private static final String EVENT_ID = "evento-1";

    @Mock
    private IIdempotenciaPort idempotenciaPort;
    @Mock
    private IBuscarCouponRepositoryPort buscarCouponRepositoryPort;
    @Mock
    private IAtualizarCouponRepositoryPort atualizarCouponRepositoryPort;
    @InjectMocks
    private AtualizarStatusCouponUseCase useCase;

    @Test
    @DisplayName("Deve alterar o status, salvar e marcar o evento como concluído")
    void deveAtualizarStatus() {
        UUID id = UUID.randomUUID();
        when(idempotenciaPort.iniciar(EVENT_ID)).thenReturn(true);
        when(buscarCouponRepositoryPort.buscarPorId(id)).thenReturn(CouponFactory.criarCouponSalvo(id));

        useCase.execute(new AtualizarStatusCouponCommand(EVENT_ID, id, CouponStatus.INACTIVE));

        ArgumentCaptor<Coupon> captor = ArgumentCaptor.forClass(Coupon.class);
        verify(atualizarCouponRepositoryPort).atualizar(captor.capture());
        assertEquals(CouponStatus.INACTIVE, captor.getValue().getStatus());
        verify(idempotenciaPort).concluir(EVENT_ID);
        verify(idempotenciaPort, never()).cancelar(any());
    }

    @Test
    @DisplayName("Não deve processar de novo um evento já concluído")
    void naoDeveProcessarEventoDuplicado() {
        when(idempotenciaPort.iniciar(EVENT_ID)).thenReturn(false);

        useCase.execute(new AtualizarStatusCouponCommand(EVENT_ID, UUID.randomUUID(), CouponStatus.INACTIVE));

        verifyNoInteractions(buscarCouponRepositoryPort, atualizarCouponRepositoryPort);
        verify(idempotenciaPort, never()).concluir(any());
        verify(idempotenciaPort, never()).cancelar(any());
    }

    @Test
    @DisplayName("Deve liberar o evento quando a regra de negócio rejeita a alteração")
    void deveLiberarEventoQuandoRegraViolada() {
        UUID id = UUID.randomUUID();
        when(idempotenciaPort.iniciar(EVENT_ID)).thenReturn(true);
        when(buscarCouponRepositoryPort.buscarPorId(id)).thenReturn(CouponFactory.criarCouponApagado(id));

        assertThrows(CouponJaApagadoException.class,
                () -> useCase.execute(new AtualizarStatusCouponCommand(EVENT_ID, id, CouponStatus.ACTIVE)));

        verify(atualizarCouponRepositoryPort, never()).atualizar(any());
        verify(idempotenciaPort).cancelar(EVENT_ID);
        verify(idempotenciaPort, never()).concluir(any());
    }

    @Test
    @DisplayName("Deve liberar o evento quando o cupom não existe")
    void deveLiberarEventoQuandoCouponNaoExiste() {
        UUID id = UUID.randomUUID();
        when(idempotenciaPort.iniciar(EVENT_ID)).thenReturn(true);
        when(buscarCouponRepositoryPort.buscarPorId(id)).thenThrow(new CouponNotFoundException("não encontrado"));

        assertThrows(CouponNotFoundException.class,
                () -> useCase.execute(new AtualizarStatusCouponCommand(EVENT_ID, id, CouponStatus.INACTIVE)));

        verify(idempotenciaPort).cancelar(EVENT_ID);
    }

    @Test
    @DisplayName("Deve liberar o evento quando salvar falha, para a mensagem ser reprocessada")
    void deveLiberarEventoQuandoSalvarFalha() {
        UUID id = UUID.randomUUID();
        when(idempotenciaPort.iniciar(EVENT_ID)).thenReturn(true);
        when(buscarCouponRepositoryPort.buscarPorId(id)).thenReturn(CouponFactory.criarCouponSalvo(id));
        doThrow(new IllegalStateException("banco fora")).when(atualizarCouponRepositoryPort).atualizar(any());

        assertThrows(IllegalStateException.class,
                () -> useCase.execute(new AtualizarStatusCouponCommand(EVENT_ID, id, CouponStatus.INACTIVE)));

        verify(idempotenciaPort).cancelar(EVENT_ID);
        verify(idempotenciaPort, never()).concluir(any());
    }

    @Test
    @DisplayName("Deve propagar quando outra instância ainda processa o mesmo evento")
    void devePropagarEventoEmProcessamento() {
        when(idempotenciaPort.iniciar(EVENT_ID)).thenThrow(new EventoEmProcessamentoException("em processamento"));

        assertThrows(EventoEmProcessamentoException.class,
                () -> useCase.execute(new AtualizarStatusCouponCommand(EVENT_ID, UUID.randomUUID(), CouponStatus.INACTIVE)));

        verifyNoInteractions(buscarCouponRepositoryPort, atualizarCouponRepositoryPort);
        verify(idempotenciaPort, never()).cancelar(any());
    }
}
