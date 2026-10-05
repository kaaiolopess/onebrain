package infrastructure.web.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onebrain.coupon.MainApplication;
import com.onebrain.coupon.domain.exception.CouponJaApagadoException;
import com.onebrain.coupon.domain.model.Coupon;
import com.onebrain.coupon.domain.model.CouponStatus;
import com.onebrain.coupon.domain.port.repository.IApagarCouponRepositoryPort;
import com.onebrain.coupon.domain.port.repository.IAtualizarCouponRepositoryPort;
import com.onebrain.coupon.domain.port.repository.IBuscarCouponRepositoryPort;
import com.onebrain.coupon.infrastructure.exception.AtualizarCouponRepositoryException;
import com.onebrain.coupon.infrastructure.repository.entity.CouponEntity;
import com.onebrain.coupon.infrastructure.repository.interfaces.CouponRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// o consumer da fila fica desligado: estes testes não dependem de LocalStack
@SpringBootTest(classes = MainApplication.class, properties = "coupon.sqs.enabled=false")
@AutoConfigureMockMvc
class CouponApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private CouponRepository couponRepository;
    @Autowired
    private IBuscarCouponRepositoryPort buscarCouponRepositoryPort;
    @Autowired
    private IApagarCouponRepositoryPort apagarCouponRepositoryPort;
    @Autowired
    private IAtualizarCouponRepositoryPort atualizarCouponRepositoryPort;

    @BeforeEach
    void limparBanco() {
        couponRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /coupon deve criar cupom removendo caracteres especiais do código")
    void deveCriarCoupon() throws Exception {
        criar(payload("ABC-123", "0.8", futuro(), null))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", not(emptyOrNullString())))
                .andExpect(jsonPath("$.code", is("ABC123")))
                .andExpect(jsonPath("$.description", is("Cupom de teste")))
                .andExpect(jsonPath("$.discountValue", is(0.8)))
                .andExpect(jsonPath("$.status", is("ACTIVE")))
                .andExpect(jsonPath("$.published", is(false)))
                .andExpect(jsonPath("$.redeemed", is(false)));

        assertEquals(1, couponRepository.count());
        assertEquals("ABC123", couponRepository.findAll().get(0).getCode());
    }

    @Test
    @DisplayName("POST /coupon deve permitir criar cupom já publicado")
    void deveCriarCouponPublicado() throws Exception {
        criar(payload("ABC123", "10", futuro(), true))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.published", is(true)));
    }

    @Test
    @DisplayName("POST /coupon deve rejeitar código que não tenha 6 caracteres após a limpeza")
    void naoDeveCriarCouponComCodigoInvalido() throws Exception {
        criar(payload("AB-12", "0.8", futuro(), null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("BAD_REQUEST")))
                .andExpect(jsonPath("$.errors[0]", containsString("6 caracteres")));

        assertEquals(0, couponRepository.count());
    }

    @Test
    @DisplayName("POST /coupon deve gravar o código em maiúsculas")
    void deveCriarCouponComCodigoEmMaiusculas() throws Exception {
        criar(payload("abc-123", "0.8", futuro(), null))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code", is("ABC123")));
    }

    @Test
    @DisplayName("POST /coupon deve rejeitar código que já existe, mesmo escrito de outra forma")
    void naoDeveCriarCouponComCodigoDuplicado() throws Exception {
        criarERetornarId();

        criar(payload("abc_123", "0.8", futuro(), null))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is("CONFLICT")))
                .andExpect(jsonPath("$.errors[0]", containsString("ABC123")));

        assertEquals(1, couponRepository.count());
    }

    @Test
    @DisplayName("POST /coupon não deve reaproveitar o código de um cupom apagado")
    void naoDeveReaproveitarCodigoDeCouponApagado() throws Exception {
        String id = criarERetornarId();
        mockMvc.perform(delete("/coupon/{id}", id)).andExpect(status().isOk());

        criar(payload("ABC-123", "0.8", futuro(), null))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /coupon deve rejeitar desconto abaixo de 0.5")
    void naoDeveCriarCouponComDescontoAbaixoDoMinimo() throws Exception {
        criar(payload("ABC123", "0.49", futuro(), null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]", containsString("mínimo")));
    }

    @Test
    @DisplayName("POST /coupon deve rejeitar desconto grande demais para ser guardado, em vez de falhar no banco")
    void naoDeveCriarCouponComDescontoGigante() throws Exception {
        criar(payload("ABC123", "100000000000000000000", futuro(), null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]", containsString("dígitos inteiros")));

        assertEquals(0, couponRepository.count());
    }

    @Test
    @DisplayName("POST /coupon deve rejeitar desconto com mais casas decimais do que o cadastro guarda")
    void naoDeveCriarCouponComCasasDecimaisDemais() throws Exception {
        criar(payload("ABC123", "0.50001", futuro(), null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]", containsString("casas decimais")));

        assertEquals(0, couponRepository.count());
    }

    @Test
    @DisplayName("O desconto devolvido na criação é o mesmo devolvido na busca")
    void descontoDaCriacaoEIgualAoDaBusca() throws Exception {
        String body = criar(payload("ABC123", "999999999999999.9999", futuro(), null))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(body).get("id").asText();

        String buscado = mockMvc.perform(get("/coupon/{id}", id))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // compara o texto do JSON: converter para double perderia justamente as casas que o teste quer conferir
        assertTrue(body.contains("\"discountValue\":999999999999999.9999"), body);
        assertTrue(buscado.contains("\"discountValue\":999999999999999.9999"), buscado);
    }

    @Test
    @DisplayName("POST /coupon deve rejeitar data de expiração no passado")
    void naoDeveCriarCouponComExpiracaoNoPassado() throws Exception {
        criar(payload("ABC123", "0.8", OffsetDateTime.now().minusDays(1).toString(), null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]", containsString("passado")));
    }

    @Test
    @DisplayName("POST /coupon deve rejeitar requisição sem campos obrigatórios")
    void naoDeveCriarCouponSemCamposObrigatorios() throws Exception {
        criar(Map.of("published", true))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(4)))
                .andExpect(jsonPath("$.errors", hasItem("Campo obrigatório ausente: code")));
    }

    @Test
    @DisplayName("POST /coupon deve rejeitar corpo mal formatado")
    void naoDeveCriarCouponComCorpoInvalido() throws Exception {
        mockMvc.perform(post("/coupon").contentType(MediaType.APPLICATION_JSON).content("{ invalido"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]", containsString("inválido")));
    }

    @Test
    @DisplayName("Erros do framework saem no mesmo formato dos erros da aplicação")
    void errosDoFrameworkUsamOMesmoFormato() throws Exception {
        mockMvc.perform(post("/coupon").contentType(MediaType.TEXT_PLAIN).content("{}"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status", is("UNSUPPORTED_MEDIA_TYPE")))
                .andExpect(jsonPath("$.errors[0]", containsString("application/json")));

        mockMvc.perform(put("/coupon").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status", is("METHOD_NOT_ALLOWED")))
                .andExpect(jsonPath("$.errors[0]", containsString("Método")));

        mockMvc.perform(get("/cupons"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is("NOT_FOUND")))
                .andExpect(jsonPath("$.errors[0]", containsString("não encontrado")));
    }

    @Test
    @DisplayName("Deve devolver o X-Correlation-Id recebido e gerar um quando não vem")
    void deveDevolverCorrelationId() throws Exception {
        mockMvc.perform(get("/coupon/{id}", UUID.randomUUID()).header("X-Correlation-Id", "teste-123"))
                .andExpect(header().string("X-Correlation-Id", "teste-123"));

        mockMvc.perform(get("/coupon/{id}", UUID.randomUUID()))
                .andExpect(header().string("X-Correlation-Id", not(emptyOrNullString())));
    }

    @Test
    @DisplayName("GET /coupon/{id} deve retornar o cupom criado")
    void deveBuscarCoupon() throws Exception {
        String id = criarERetornarId();

        mockMvc.perform(get("/coupon/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(id)))
                .andExpect(jsonPath("$.code", is("ABC123")))
                .andExpect(jsonPath("$.description", is("Cupom de teste")))
                .andExpect(jsonPath("$.discountValue", is(0.8)))
                .andExpect(jsonPath("$.status", is("ACTIVE")))
                .andExpect(jsonPath("$.published", is(false)))
                .andExpect(jsonPath("$.redeemed", is(false)));
    }

    @Test
    @DisplayName("GET /coupon/{id} deve retornar 404 para cupom inexistente")
    void deveRetornarNotFoundAoBuscarCouponInexistente() throws Exception {
        mockMvc.perform(get("/coupon/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is("NOT_FOUND")))
                .andExpect(jsonPath("$.errors[0]", containsString("não encontrado")));
    }

    @Test
    @DisplayName("GET /coupon/{id} deve retornar 404 para cupom apagado")
    void deveRetornarNotFoundAoBuscarCouponApagado() throws Exception {
        String id = criarERetornarId();
        mockMvc.perform(delete("/coupon/{id}", id)).andExpect(status().isOk());

        mockMvc.perform(get("/coupon/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors[0]", is("Cupom foi apagado")));
    }

    @Test
    @DisplayName("GET /coupon/{id} deve retornar 400 para id que não é UUID")
    void deveRetornarBadRequestAoBuscarComIdInvalido() throws Exception {
        mockMvc.perform(get("/coupon/{id}", "abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /coupon/{id} deve retornar 400 para id que não é UUID")
    void deveRetornarBadRequestParaIdInvalido() throws Exception {
        mockMvc.perform(delete("/coupon/{id}", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("BAD_REQUEST")));
    }

    @Test
    @DisplayName("DELETE /coupon/{id} deve fazer soft delete preservando os dados no banco")
    void deveApagarCouponComSoftDelete() throws Exception {
        String id = criarERetornarId();

        mockMvc.perform(delete("/coupon/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Cupom apagado com sucesso")));

        CouponEntity entity = couponRepository.findById(UUID.fromString(id)).orElseThrow();
        assertEquals(CouponStatus.DELETED, entity.getStatus());
        assertNotNull(entity.getDeletedAt());
        assertEquals("ABC123", entity.getCode());
        assertEquals("Cupom de teste", entity.getDescription());
        assertNotNull(entity.getCreated());
        assertEquals(1, couponRepository.count());
    }

    @Test
    @DisplayName("DELETE /coupon/{id} não deve apagar cupom já apagado")
    void naoDeveApagarCouponJaApagado() throws Exception {
        String id = criarERetornarId();
        mockMvc.perform(delete("/coupon/{id}", id)).andExpect(status().isOk());

        mockMvc.perform(delete("/coupon/{id}", id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is("CONFLICT")))
                .andExpect(jsonPath("$.errors[0]", containsString("já foi apagado")));
    }

    @Test
    @DisplayName("Dois deletes concorrentes do mesmo cupom: só o primeiro passa")
    void naoDeveApagarDuasVezesEmConcorrencia() throws Exception {
        UUID id = UUID.fromString(criarERetornarId());

        // as duas requisições leem o cupom ainda ativo, antes de qualquer uma salvar
        Coupon leituraA = buscarCouponRepositoryPort.buscarPorId(id);
        Coupon leituraB = buscarCouponRepositoryPort.buscarPorId(id);
        leituraA.apagar();
        leituraB.apagar();

        apagarCouponRepositoryPort.apagar(leituraA);

        assertThrows(CouponJaApagadoException.class, () -> apagarCouponRepositoryPort.apagar(leituraB));
        CouponEntity entity = couponRepository.findById(id).orElseThrow();
        assertEquals(CouponStatus.DELETED, entity.getStatus());
        assertEquals(leituraA.getDeletedAt().toInstant().truncatedTo(ChronoUnit.MILLIS),
                entity.getDeletedAt().toInstant().truncatedTo(ChronoUnit.MILLIS));
    }

    @Test
    @DisplayName("O delete vale mesmo que o status do cupom mude depois da leitura")
    void deveApagarMesmoComMudancaDeStatusConcorrente() throws Exception {
        UUID id = UUID.fromString(criarERetornarId());

        // o delete lê o cupom e, antes de salvar, a fila inativa o mesmo cupom
        Coupon leituraDoDelete = buscarCouponRepositoryPort.buscarPorId(id);
        Coupon leituraDaFila = buscarCouponRepositoryPort.buscarPorId(id);
        leituraDaFila.alterarStatus(CouponStatus.INACTIVE);
        atualizarCouponRepositoryPort.atualizar(leituraDaFila);
        leituraDoDelete.apagar();

        apagarCouponRepositoryPort.apagar(leituraDoDelete);

        CouponEntity entity = couponRepository.findById(id).orElseThrow();
        assertEquals(CouponStatus.DELETED, entity.getStatus());
        assertNotNull(entity.getDeletedAt());
        assertEquals(2L, entity.getVersion());
    }

    @Test
    @DisplayName("Uma alteração de status lida antes do delete não desfaz o delete")
    void alteracaoDeStatusAtrasadaNaoDesfazODelete() throws Exception {
        UUID id = UUID.fromString(criarERetornarId());

        // a fila lê o cupom ainda ativo, o delete é aplicado, e só então a fila tenta salvar
        Coupon leituraDaFila = buscarCouponRepositoryPort.buscarPorId(id);
        Coupon leituraDoDelete = buscarCouponRepositoryPort.buscarPorId(id);
        leituraDoDelete.apagar();
        apagarCouponRepositoryPort.apagar(leituraDoDelete);
        leituraDaFila.alterarStatus(CouponStatus.INACTIVE);

        assertThrows(AtualizarCouponRepositoryException.class,
                () -> atualizarCouponRepositoryPort.atualizar(leituraDaFila));
        assertEquals(CouponStatus.DELETED, couponRepository.findById(id).orElseThrow().getStatus());
    }

    @Test
    @DisplayName("DELETE /coupon/{id} deve retornar 404 para cupom inexistente")
    void deveRetornarNotFoundAoApagarCouponInexistente() throws Exception {
        mockMvc.perform(delete("/coupon/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is("NOT_FOUND")));
    }

    private ResultActions criar(Map<String, Object> payload) throws Exception {
        return mockMvc.perform(post("/coupon")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)));
    }

    private String criarERetornarId() throws Exception {
        String body = criar(payload("ABC-123", "0.8", futuro(), null))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }

    private static Map<String, Object> payload(String code, String discountValue, String expirationDate, Boolean published) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("code", code);
        payload.put("description", "Cupom de teste");
        payload.put("discountValue", new java.math.BigDecimal(discountValue));
        payload.put("expirationDate", expirationDate);
        if (published != null) {
            payload.put("published", published);
        }
        return payload;
    }

    private static String futuro() {
        return OffsetDateTime.now().plusDays(30).toString();
    }
}
