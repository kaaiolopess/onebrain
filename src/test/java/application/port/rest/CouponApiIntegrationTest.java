package application.port.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onebrain.coupon.MainApplication;
import com.onebrain.coupon.domain.model.CouponStatus;
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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = MainApplication.class)
@AutoConfigureMockMvc
class CouponApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private CouponRepository couponRepository;

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
    @DisplayName("POST /coupon deve rejeitar desconto abaixo de 0.5")
    void naoDeveCriarCouponComDescontoAbaixoDoMinimo() throws Exception {
        criar(payload("ABC123", "0.49", futuro(), null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]", containsString("mínimo")));
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
    @DisplayName("GET /coupon/{id} deve retornar o cupom criado")
    void deveBuscarCoupon() throws Exception {
        String id = criarERetornarId();

        mockMvc.perform(get("/coupon/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(id)))
                .andExpect(jsonPath("$.code", is("ABC123")))
                .andExpect(jsonPath("$.status", is("ACTIVE")));
    }

    @Test
    @DisplayName("GET /coupon/{id} deve retornar 404 para cupom inexistente")
    void deveRetornarNotFoundAoBuscarCouponInexistente() throws Exception {
        mockMvc.perform(get("/coupon/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is("NOT_FOUND")));
    }

    @Test
    @DisplayName("GET /coupon/{id} deve retornar 400 para id que não é UUID")
    void deveRetornarBadRequestParaIdInvalido() throws Exception {
        mockMvc.perform(get("/coupon/{id}", "abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /coupon/{id} deve fazer soft delete preservando os dados no banco")
    void deveApagarCouponComSoftDelete() throws Exception {
        String id = criarERetornarId();

        mockMvc.perform(delete("/coupon/{id}", id))
                .andExpect(status().isNoContent());

        CouponEntity entity = couponRepository.findById(UUID.fromString(id)).orElseThrow();
        assertEquals(CouponStatus.DELETED, entity.getStatus());
        assertNotNull(entity.getDeletedAt());
        assertEquals("ABC123", entity.getCode());
        assertEquals("Cupom de teste", entity.getDescription());

        mockMvc.perform(get("/coupon/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /coupon/{id} não deve apagar cupom já apagado")
    void naoDeveApagarCouponJaApagado() throws Exception {
        String id = criarERetornarId();
        mockMvc.perform(delete("/coupon/{id}", id)).andExpect(status().isNoContent());

        mockMvc.perform(delete("/coupon/{id}", id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is("CONFLICT")))
                .andExpect(jsonPath("$.errors[0]", containsString("já foi apagado")));
    }

    @Test
    @DisplayName("DELETE /coupon/{id} deve retornar 404 para cupom inexistente")
    void deveRetornarNotFoundAoApagarCouponInexistente() throws Exception {
        mockMvc.perform(delete("/coupon/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
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
