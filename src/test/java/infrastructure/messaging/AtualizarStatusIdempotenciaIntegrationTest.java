package infrastructure.messaging;

import com.onebrain.coupon.MainApplication;
import com.onebrain.coupon.application.exception.EventoEmProcessamentoException;
import com.onebrain.coupon.application.port.IIdempotenciaPort;
import com.onebrain.coupon.application.useCase.interfaces.IAtualizarStatusCouponUseCase;
import com.onebrain.coupon.domain.model.CouponStatus;
import com.onebrain.coupon.infrastructure.messaging.CouponStatusListener;
import com.onebrain.coupon.infrastructure.messaging.CouponStatusMessage;
import com.onebrain.coupon.infrastructure.repository.entity.CouponEntity;
import com.onebrain.coupon.infrastructure.repository.interfaces.CouponRepository;
import infrastructure.repository.factory.CouponEntityFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Fluxo completo da mensagem (listener -> caso de uso -> domínio -> H2), com o Redis trocado por um
 * armazenamento em memória de mesmo comportamento.
 */
// o consumer real fica desligado: o listener é chamado direto, sem depender de LocalStack
@SpringBootTest(classes = MainApplication.class, properties = "coupon.sqs.enabled=false")
@Import(AtualizarStatusIdempotenciaIntegrationTest.IdempotenciaEmMemoriaConfig.class)
class AtualizarStatusIdempotenciaIntegrationTest {

    @Autowired
    private IAtualizarStatusCouponUseCase atualizarStatusCouponUseCase;
    @Autowired
    private CouponRepository couponRepository;
    @Autowired
    private IdempotenciaEmMemoria idempotencia;

    private CouponStatusListener listener;

    @BeforeEach
    void setUp() {
        couponRepository.deleteAll();
        idempotencia.limpar();
        listener = new CouponStatusListener(atualizarStatusCouponUseCase);
    }

    @Test
    @DisplayName("Deve inativar o cupom ao receber a mensagem")
    void deveInativarCoupon() {
        UUID id = salvarCoupon(CouponStatus.ACTIVE);

        listener.receber(new CouponStatusMessage("evento-1", id.toString(), "INACTIVE"));

        assertEquals(CouponStatus.INACTIVE, buscar(id).getStatus());
        assertEquals(1L, buscar(id).getVersion());
    }

    @Test
    @DisplayName("A mesma mensagem entregue duas vezes só é aplicada uma vez")
    void mensagemDuplicadaSoEAplicadaUmaVez() {
        UUID id = salvarCoupon(CouponStatus.ACTIVE);
        listener.receber(new CouponStatusMessage("evento-1", id.toString(), "INACTIVE"));
        // outro evento reativa o cupom; a duplicata do primeiro não pode inativá-lo de novo
        listener.receber(new CouponStatusMessage("evento-2", id.toString(), "ACTIVE"));

        listener.receber(new CouponStatusMessage("evento-1", id.toString(), "INACTIVE"));

        assertEquals(CouponStatus.ACTIVE, buscar(id).getStatus());
        assertEquals(2L, buscar(id).getVersion());
    }

    @Test
    @DisplayName("Mensagem rejeitada pela regra de negócio não altera o cupom nem trava o evento")
    void mensagemParaCouponApagadoEDescartada() {
        UUID id = salvarCoupon(CouponStatus.DELETED);

        listener.receber(new CouponStatusMessage("evento-1", id.toString(), "ACTIVE"));

        assertEquals(CouponStatus.DELETED, buscar(id).getStatus());
        assertEquals(0, idempotencia.total());
    }

    @Test
    @DisplayName("Mensagem pedindo DELETED é descartada: apagar só pela exclusão")
    void mensagemComStatusDeletedEDescartada() {
        UUID id = salvarCoupon(CouponStatus.ACTIVE);

        listener.receber(new CouponStatusMessage("evento-1", id.toString(), "DELETED"));

        assertEquals(CouponStatus.ACTIVE, buscar(id).getStatus());
    }

    private UUID salvarCoupon(CouponStatus status) {
        CouponEntity entity = CouponEntityFactory.criarCouponEntity(null);
        entity.setVersion(null);
        entity.setStatus(status);
        entity.setDeletedAt(status == CouponStatus.DELETED ? OffsetDateTime.now() : null);
        return couponRepository.save(entity).getId();
    }

    private CouponEntity buscar(UUID id) {
        return couponRepository.findById(id).orElseThrow();
    }

    @TestConfiguration
    static class IdempotenciaEmMemoriaConfig {
        @Bean
        @Primary
        IdempotenciaEmMemoria idempotenciaEmMemoria() {
            return new IdempotenciaEmMemoria();
        }
    }

    static class IdempotenciaEmMemoria implements IIdempotenciaPort {
        private final Map<String, String> eventos = new ConcurrentHashMap<>();

        @Override
        public boolean iniciar(String eventId) {
            String anterior = eventos.putIfAbsent(eventId, "PROCESSING");
            if (anterior == null) {
                return true;
            }
            if ("DONE".equals(anterior)) {
                return false;
            }
            throw new EventoEmProcessamentoException("Evento " + eventId + " ainda está em processamento");
        }

        @Override
        public void concluir(String eventId) {
            eventos.put(eventId, "DONE");
        }

        @Override
        public void cancelar(String eventId) {
            eventos.remove(eventId);
        }

        void limpar() {
            eventos.clear();
        }

        int total() {
            return eventos.size();
        }
    }
}
