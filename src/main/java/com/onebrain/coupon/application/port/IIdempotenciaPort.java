package com.onebrain.coupon.application.port;

/**
 * Controla quais eventos já foram processados, para que uma mensagem repetida não seja aplicada duas vezes.
 */
public interface IIdempotenciaPort {

    /**
     * Reserva o evento para processamento.
     *
     * @return false se o evento já foi concluído antes (mensagem duplicada)
     * @throws com.onebrain.coupon.application.exception.EventoEmProcessamentoException
     *         se outra instância ainda está processando o mesmo evento
     */
    boolean iniciar(String eventId);

    void concluir(String eventId);

    void cancelar(String eventId);
}
