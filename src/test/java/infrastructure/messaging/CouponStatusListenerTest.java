package infrastructure.messaging;

import com.onebrain.coupon.application.exception.EventoEmProcessamentoException;
import com.onebrain.coupon.application.useCase.command.AtualizarStatusCouponCommand;
import com.onebrain.coupon.application.useCase.interfaces.IAtualizarStatusCouponUseCase;
import com.onebrain.coupon.domain.exception.CouponJaApagadoException;
import com.onebrain.coupon.domain.exception.CouponNotFoundException;
import com.onebrain.coupon.domain.model.CouponStatus;
import com.onebrain.coupon.infrastructure.messaging.CouponStatusListener;
import com.onebrain.coupon.infrastructure.messaging.CouponStatusMessage;
import com.onebrain.coupon.infrastructure.web.filter.MdcFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CouponStatusListenerTest {

    @Mock
    private IAtualizarStatusCouponUseCase atualizarStatusCouponUseCase;
    @InjectMocks
    private CouponStatusListener listener;

    @Test
    @DisplayName("Deve converter a mensagem e chamar o caso de uso")
    void deveProcessarMensagemValida() {
        UUID id = UUID.randomUUID();

        listener.receber(new CouponStatusMessage("evento-1", id.toString(), "INACTIVE"));

        ArgumentCaptor<AtualizarStatusCouponCommand> captor = ArgumentCaptor.forClass(AtualizarStatusCouponCommand.class);
        verify(atualizarStatusCouponUseCase).execute(captor.capture());
        assertEquals("evento-1", captor.getValue().eventId());
        assertEquals(id, captor.getValue().couponId());
        assertEquals(CouponStatus.INACTIVE, captor.getValue().status());
        assertNull(MDC.get(MdcFilter.CORRELATION_ID));
    }

    @ParameterizedTest
    @CsvSource(value = {
            "NULL, 7727dd1b-3373-4840-845b-75c224a6f5fd, INACTIVE",
            "'', 7727dd1b-3373-4840-845b-75c224a6f5fd, INACTIVE",
            "evento-1, nao-e-uuid, INACTIVE",
            "evento-1, NULL, INACTIVE",
            "evento-1, 7727dd1b-3373-4840-845b-75c224a6f5fd, DESCONHECIDO",
            "evento-1, 7727dd1b-3373-4840-845b-75c224a6f5fd, NULL"
    }, nullValues = "NULL")
    @DisplayName("Deve descartar mensagem inválida sem chamar o caso de uso")
    void deveDescartarMensagemInvalida(String eventId, String couponId, String status) {
        assertDoesNotThrow(() -> listener.receber(new CouponStatusMessage(eventId, couponId, status)));

        verifyNoInteractions(atualizarStatusCouponUseCase);
    }

    @Test
    @DisplayName("Deve descartar mensagem vazia")
    void deveDescartarMensagemVazia() {
        assertDoesNotThrow(() -> listener.receber(null));

        verifyNoInteractions(atualizarStatusCouponUseCase);
    }

    @Test
    @DisplayName("Deve descartar a mensagem quando a regra de negócio rejeita")
    void deveDescartarQuandoRegraViolada() {
        doThrow(new CouponJaApagadoException("apagado")).when(atualizarStatusCouponUseCase).execute(any());

        assertDoesNotThrow(() -> listener.receber(mensagemValida()));
    }

    @Test
    @DisplayName("Deve descartar a mensagem quando o cupom não existe")
    void deveDescartarQuandoCouponNaoExiste() {
        doThrow(new CouponNotFoundException("não encontrado")).when(atualizarStatusCouponUseCase).execute(any());

        assertDoesNotThrow(() -> listener.receber(mensagemValida()));
    }

    @Test
    @DisplayName("Deve relançar erro temporário para a mensagem voltar à fila")
    void deveRelancarErroTemporario() {
        doThrow(new EventoEmProcessamentoException("em processamento")).when(atualizarStatusCouponUseCase).execute(any());

        assertThrows(EventoEmProcessamentoException.class, () -> listener.receber(mensagemValida()));
        assertNull(MDC.get(MdcFilter.CORRELATION_ID));
    }

    private static CouponStatusMessage mensagemValida() {
        return new CouponStatusMessage("evento-1", UUID.randomUUID().toString(), "INACTIVE");
    }
}
