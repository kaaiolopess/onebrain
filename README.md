# OneBrain - Coupon API

API REST desenvolvida em **Java 21** e **Spring Boot 3**, seguindo princípios da **Arquitetura Hexagonal**, para gerenciamento de cupons de desconto.
Permite **criar**, **buscar** e **apagar** (soft delete) cupons, com persistência em **H2 Database** em memória.

---

## Tecnologias Utilizadas

- **Java 21**
- **Spring Boot 3.5**
- **Spring Web**
- **Spring Data JPA**
- **Bean Validation**
- **Lombok**
- **JUnit 5 & Mockito**
- **JaCoCo** (cobertura mínima de 80%)
- **H2 Database** (em memória)
- **OpenAPI / Swagger** (contract-first, com OpenAPI Generator)
- **Docker & Docker Compose**

---

## 🚀 Como Rodar o Projeto

### 1. Clone o repositório
```bash
git clone https://github.com/kaaiolopess/onebrain.git
cd onebrain
```

### 2. Com Docker (não precisa de Java nem Maven instalados)
```bash
docker compose up --build
```

### 3. Ou com Maven (requer JDK 21)
```bash
./mvnw clean install
./mvnw spring-boot:run
```

### 4. Acesse a documentação da API
👉 [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

---

## 📚 Endpoints da API

| Método   | Endpoint        | Descrição                         | Sucesso          | Erros                                      |
|----------|-----------------|-----------------------------------|------------------|--------------------------------------------|
| `POST`   | `/coupon`       | Cria um novo cupom                | `201 Created`    | `400` requisição inválida / regra violada  |
| `GET`    | `/coupon/{id}`  | Busca um cupom pelo ID            | `200 OK`         | `404` não encontrado ou apagado            |
| `DELETE` | `/coupon/{id}`  | Apaga um cupom (soft delete)      | `204 No Content` | `404` não encontrado, `409` já apagado     |

### 📄 Exemplo de requisição (`POST /coupon`)

```json
{
  "code": "ABC-123",
  "description": "Cupom de boas-vindas",
  "discountValue": 0.8,
  "expirationDate": "2030-11-04T17:14:45.180Z",
  "published": false
}
```

### 📄 Exemplo de resposta (`201 Created`)

```json
{
  "id": "cef9d1e3-aae5-4ab6-a297-358c6032b1e7",
  "code": "ABC123",
  "description": "Cupom de boas-vindas",
  "discountValue": 0.8,
  "expirationDate": "2030-11-04T17:14:45.18Z",
  "status": "ACTIVE",
  "published": false,
  "redeemed": false
}
```

### 📄 Exemplo de erro

```json
{
  "status": "BAD_REQUEST",
  "errors": ["O código do cupom deve ter exatamente 6 caracteres alfanuméricos"]
}
```

---

## 📏 Regras de Negócio

Todas as regras estão encapsuladas no objeto de domínio [`Coupon`](src/main/java/com/onebrain/coupon/domain/model/Coupon.java).

### Create
- `code`, `description`, `discountValue` e `expirationDate` são obrigatórios.
- O código é alfanumérico com **6 caracteres**. Caracteres especiais são aceitos na entrada, mas **removidos** antes de salvar e de retornar (`ABC-123` → `ABC123`). Se, depois da limpeza, o código não tiver exatamente 6 caracteres, a criação é rejeitada.
- O valor de desconto tem **mínimo de 0.5**, sem máximo.
- A data de expiração **não pode estar no passado**.
- O cupom pode ser criado como **já publicado** (`published: true`).
- Todo cupom nasce com status `ACTIVE` e `redeemed: false`.

### Delete
- Um cupom pode ser apagado a qualquer momento (inclusive depois de expirado).
- É feito **soft delete**: o registro permanece no banco com status `DELETED` e a data em `deleted_at`, preservando os dados do cadastro.
- **Não é possível apagar um cupom já apagado** (`409 Conflict`).

---

## 🧪 Testes Automatizados

A aplicação conta com testes **unitários** (JUnit 5 + Mockito) e de **integração** (Spring Boot Test + MockMvc + H2), cobrindo as regras de negócio.

```bash
./mvnw verify
```

- O build **falha** se a cobertura de linhas ficar abaixo de **80%** (JaCoCo).
- Relatório de cobertura: `target/site/jacoco/index.html`

---

## 🧠 Arquitetura

Este projeto segue os princípios de **Arquitetura Hexagonal / Clean Architecture**, separando bem as responsabilidades:

- **Domain** → Regras de negócio puras (`model`) e portas de saída (`port`). Não conhece web nem banco.
- **Application** → Casos de uso: orquestram o fluxo chamando o domínio e as portas, sem regra de negócio e sem saber se a entrada é HTTP, fila ou terminal.
- **Infrastructure** → Adaptadores: entrada HTTP (`web`) e acesso a dados com JPA (`repository`).

```
src/main/java/com/onebrain/coupon
├── application
│   └── useCase         # CriarCouponUseCase, BuscarCouponPorIdUseCase, ApagarCouponUseCase
│       ├── command     # dados de entrada do caso de uso
│       └── interfaces
├── domain
│   ├── exception       # violações de regra de negócio
│   ├── model           # Coupon, CouponStatus
│   └── port/repository # portas de saída
└── infrastructure
    ├── exception
    ├── repository      # entity, impl, interfaces (Spring Data), mapper
    └── web
        ├── config      # OpenApiConfig
        ├── exception   # GlobalExceptionHandler, ApiErrorMessage
        ├── mapper      # API <-> caso de uso
        └── rest        # CouponController
```

O contrato da API fica em [`src/main/resources/openapi.yaml`](src/main/resources/openapi.yaml); a interface `CouponApi` e os modelos de requisição/resposta são gerados a partir dele no build.

---

## 🔎 Observabilidade

Cada requisição em `/coupon` recebe um **correlationId**, propagado pelo **MDC** para todos os logs daquela requisição.

- Se o cliente enviar o header `X-Correlation-Id`, ele é reaproveitado; caso contrário, a aplicação gera um UUID.
- O mesmo valor volta no header `X-Correlation-Id` da resposta.
- Campos no MDC: `correlationId`, `couponId`, `httpMethod` e `path`.
- Ao final de cada requisição é registrado um log com status e tempo de resposta.

Rodando local, os logs saem em texto:

```
INFO [onebrain] [correlationId=teste-123 couponId=7727dd1b-...] ... Requisição finalizada: DELETE /coupon/7727dd1b-..., status=204, duracaoMs=12
```

No Docker (`docker compose up`), os logs saem em **JSON estruturado** (Elastic Common Schema), com os campos do MDC, prontos para Datadog ou Grafana Loki. Para ligar fora do Docker:

```bash
LOGGING_STRUCTURED_FORMAT_CONSOLE=ecs ./mvnw spring-boot:run
```

---

## 📨 Consumer SQS: atualização de status

Além da API, a aplicação consome uma fila **SQS** para ativar ou inativar cupons. O consumer fica **desligado por padrão** (a aplicação sobe sem AWS) e é ligado no `docker compose`, que sobe um **LocalStack** com a fila `coupon-status-queue` e a DLQ `coupon-status-dlq`, mais um **Redis**.

### Mensagem

```json
{ "eventId": "c1f0a2d4-0001", "couponId": "cef9d1e3-aae5-4ab6-a297-358c6032b1e7", "status": "INACTIVE" }
```

### Regras (no domínio, em `Coupon.alterarStatus`)
- A fila só alterna entre `ACTIVE` e `INACTIVE`.
- Cupom apagado não pode ter o status alterado.
- `DELETED` não é aceito pela fila: apagar é só pelo `DELETE /coupon/{id}`.

### Idempotência
O SQS entrega cada mensagem **pelo menos uma vez**, então a mesma mensagem pode chegar repetida. Cada `eventId` é registrado no **Redis**:

| Estado da chave | O que acontece com a mensagem |
|---|---|
| não existe | é reservada como `PROCESSING` (TTL 30s) e processada; ao terminar vira `DONE` (TTL 24h) |
| `DONE` | duplicata: é confirmada e ignorada |
| `PROCESSING` | outra instância ainda está processando: volta para a fila |

Se o processamento falha, a chave é apagada para a próxima entrega poder tentar de novo.

### Tratamento de falhas
- Mensagem inválida, cupom inexistente ou regra de negócio violada: registra log e descarta (repetir não resolveria).
- Falha temporária (banco ou Redis fora, conflito de versão): a mensagem volta para a fila e, depois de 3 tentativas, vai para a DLQ.

### Testando com o LocalStack

```bash
docker compose up --build

# envia uma mensagem (troque o couponId por um id criado pelo POST /coupon)
docker exec onebrain-localstack awslocal sqs send-message \
  --queue-url http://sqs.us-east-1.localhost.localstack.cloud:4566/000000000000/coupon-status-queue \
  --message-body '{"eventId":"evento-1","couponId":"<id>","status":"INACTIVE"}'
```

Variáveis de ambiente: `COUPON_SQS_ENABLED`, `COUPON_SQS_QUEUE`, `REDIS_HOST`, `REDIS_PORT`, `AWS_REGION` e `SPRING_CLOUD_AWS_ENDPOINT`.

---

## 📦 Banco de Dados

A aplicação utiliza o **H2 Database** (em memória), facilitando o uso sem necessidade de instalação.

Acesse o console H2 em:
- [http://localhost:8080/h2-console](http://localhost:8080/h2-console)

- **JDBC URL:** `jdbc:h2:mem:onebrain`

- **Usuário:** `sa`

- **Senha:** *(em branco)*
