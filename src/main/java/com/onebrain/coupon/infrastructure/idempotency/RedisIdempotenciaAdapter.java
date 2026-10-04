package com.onebrain.coupon.infrastructure.idempotency;

import com.onebrain.coupon.application.exception.EventoEmProcessamentoException;
import com.onebrain.coupon.application.port.IIdempotenciaPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Guarda no Redis o estado de cada evento: PROCESSING enquanto é processado e DONE depois de concluído.
 */
@Component
@Slf4j
public class RedisIdempotenciaAdapter implements IIdempotenciaPort {

    static final String PREFIXO = "onebrain:coupon-status:evento:";
    static final String PROCESSING = "PROCESSING";
    static final String DONE = "DONE";

    private final StringRedisTemplate redisTemplate;
    private final Duration ttlProcessando;
    private final Duration ttlConcluido;

    public RedisIdempotenciaAdapter(StringRedisTemplate redisTemplate,
                                    @Value("${coupon.idempotencia.ttl-processando}") Duration ttlProcessando,
                                    @Value("${coupon.idempotencia.ttl-concluido}") Duration ttlConcluido) {
        this.redisTemplate = redisTemplate;
        this.ttlProcessando = ttlProcessando;
        this.ttlConcluido = ttlConcluido;
    }

    @Override
    public boolean iniciar(String eventId) {
        // SET NX: só uma instância consegue reservar o evento
        Boolean reservado = redisTemplate.opsForValue().setIfAbsent(chave(eventId), PROCESSING, ttlProcessando);
        if (Boolean.TRUE.equals(reservado)) {
            return true;
        }
        if (DONE.equals(redisTemplate.opsForValue().get(chave(eventId)))) {
            return false;
        }
        throw new EventoEmProcessamentoException("Evento " + eventId + " ainda está em processamento");
    }

    @Override
    public void concluir(String eventId) {
        redisTemplate.opsForValue().set(chave(eventId), DONE, ttlConcluido);
    }

    @Override
    public void cancelar(String eventId) {
        try {
            redisTemplate.delete(chave(eventId));
        } catch (RuntimeException e) {
            // se não der para apagar, a chave PROCESSING expira sozinha pelo TTL
            log.warn("Não foi possível liberar o evento {} no Redis", eventId, e);
        }
    }

    private String chave(String eventId) {
        return PREFIXO + eventId;
    }
}
