package infrastructure.web.filter;

import com.onebrain.coupon.infrastructure.web.filter.MdcFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MdcFilterTest {

    private final MdcFilter filter = new MdcFilter();
    private final Map<String, String> mdcDuranteRequisicao = new HashMap<>();
    private final FilterChain capturaMdc = (req, res) -> mdcDuranteRequisicao.putAll(MDC.getCopyOfContextMap());

    @AfterEach
    void limparMdc() {
        MDC.clear();
    }

    @Test
    @DisplayName("Deve gerar correlationId quando o header não é enviado")
    void deveGerarCorrelationId() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(new MockHttpServletRequest("POST", "/coupon"), response, capturaMdc);

        String correlationId = mdcDuranteRequisicao.get(MdcFilter.CORRELATION_ID);
        assertNotNull(correlationId);
        assertEquals(correlationId, response.getHeader(MdcFilter.CORRELATION_ID_HEADER));
        assertEquals("POST", mdcDuranteRequisicao.get(MdcFilter.HTTP_METHOD));
        assertEquals("/coupon", mdcDuranteRequisicao.get(MdcFilter.PATH));
    }

    @Test
    @DisplayName("Deve reaproveitar o correlationId recebido no header")
    void deveReaproveitarCorrelationId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/coupon/123");
        request.addHeader(MdcFilter.CORRELATION_ID_HEADER, "abc-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, capturaMdc);

        assertEquals("abc-123", mdcDuranteRequisicao.get(MdcFilter.CORRELATION_ID));
        assertEquals("abc-123", response.getHeader(MdcFilter.CORRELATION_ID_HEADER));
    }

    @Test
    @DisplayName("Deve descartar correlationId com caracteres inválidos")
    void deveDescartarCorrelationIdInvalido() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/coupon/123");
        request.addHeader(MdcFilter.CORRELATION_ID_HEADER, "abc 123\nlinha-forjada");

        filter.doFilter(request, new MockHttpServletResponse(), capturaMdc);

        assertNotEquals("abc 123\nlinha-forjada", mdcDuranteRequisicao.get(MdcFilter.CORRELATION_ID));
        assertNotNull(mdcDuranteRequisicao.get(MdcFilter.CORRELATION_ID));
    }

    @Test
    @DisplayName("Deve limpar o MDC ao final da requisição")
    void deveLimparMdcAoFinal() throws Exception {
        filter.doFilter(new MockHttpServletRequest("POST", "/coupon"), new MockHttpServletResponse(), capturaMdc);

        assertNull(MDC.get(MdcFilter.CORRELATION_ID));
        assertNull(MDC.get(MdcFilter.PATH));
    }

    @Test
    @DisplayName("Deve limpar o MDC mesmo quando a requisição falha")
    void deveLimparMdcQuandoFalha() {
        FilterChain falha = (req, res) -> {
            throw new ServletException("erro");
        };

        assertThrows(ServletException.class,
                () -> filter.doFilter(new MockHttpServletRequest("POST", "/coupon"), new MockHttpServletResponse(), falha));

        assertNull(MDC.get(MdcFilter.CORRELATION_ID));
    }

    @Test
    @DisplayName("Não deve atuar em caminhos fora de /coupon")
    void naoDeveAtuarForaDeCoupon() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain semMdc = (req, res) -> assertNull(MDC.get(MdcFilter.CORRELATION_ID));

        filter.doFilter(new MockHttpServletRequest("GET", "/swagger-ui.html"), response, semMdc);

        assertNull(response.getHeader(MdcFilter.CORRELATION_ID_HEADER));
    }
}
