package infrastructure.idempotency;

import com.onebrain.coupon.application.exception.EventoEmProcessamentoException;
import com.onebrain.coupon.infrastructure.idempotency.RedisIdempotenciaAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisIdempotenciaAdapterTest {

    private static final String CHAVE = "onebrain:coupon-status:evento:evento-1";
    private static final Duration TTL_PROCESSANDO = Duration.ofSeconds(30);
    private static final Duration TTL_CONCLUIDO = Duration.ofHours(24);

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisIdempotenciaAdapter adapter;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        adapter = new RedisIdempotenciaAdapter(redisTemplate, TTL_PROCESSANDO, TTL_CONCLUIDO);
    }

    @Test
    @DisplayName("Deve reservar um evento novo")
    void deveReservarEventoNovo() {
        when(valueOperations.setIfAbsent(CHAVE, "PROCESSING", TTL_PROCESSANDO)).thenReturn(true);

        assertTrue(adapter.iniciar("evento-1"));
    }

    @Test
    @DisplayName("Deve indicar duplicata quando o evento já foi concluído")
    void deveIndicarEventoJaConcluido() {
        when(valueOperations.setIfAbsent(CHAVE, "PROCESSING", TTL_PROCESSANDO)).thenReturn(false);
        when(valueOperations.get(CHAVE)).thenReturn("DONE");

        assertFalse(adapter.iniciar("evento-1"));
    }

    @Test
    @DisplayName("Deve recusar quando outra instância ainda processa o evento")
    void deveRecusarEventoEmProcessamento() {
        when(valueOperations.setIfAbsent(CHAVE, "PROCESSING", TTL_PROCESSANDO)).thenReturn(false);
        when(valueOperations.get(CHAVE)).thenReturn("PROCESSING");

        assertThrows(EventoEmProcessamentoException.class, () -> adapter.iniciar("evento-1"));
    }

    @Test
    @DisplayName("Deve marcar o evento como concluído com o TTL longo")
    void deveConcluirEvento() {
        adapter.concluir("evento-1");

        verify(valueOperations).set(CHAVE, "DONE", TTL_CONCLUIDO);
    }

    @Test
    @DisplayName("Deve apagar a chave ao cancelar")
    void deveCancelarEvento() {
        adapter.cancelar("evento-1");

        verify(redisTemplate).delete(CHAVE);
    }

    @Test
    @DisplayName("Cancelar não deve falhar se o Redis estiver fora")
    void cancelarNaoDeveFalharSemRedis() {
        when(redisTemplate.delete(CHAVE)).thenThrow(new RedisConnectionFailureException("fora"));

        assertDoesNotThrow(() -> adapter.cancelar("evento-1"));
    }
}
