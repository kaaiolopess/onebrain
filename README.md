# OneBrain - Coupon API

API REST desenvolvida em **Java 21** e **Spring Boot 3**, seguindo princípios da **Arquitetura Hexagonal**, para gerenciamento de cupons de desconto.

- **API**: permite **criar**, **buscar** e **apagar** (soft delete) cupons, com persistência em **H2** em memória.
- **Consumer SQS** (extra): ativa ou inativa cupons a partir de mensagens de uma fila, com **idempotência** em **Redis**.

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
- **Spring Cloud AWS (SQS)** com **LocalStack**
- **Redis**

---

## 🚀 Como Rodar o Projeto

```bash
git clone https://github.com/kaaiolopess/onebrain.git
cd onebrain
```

Há três formas de subir. A primeira é a mais simples e já inclui tudo.

### Opção 1 — Tudo no Docker (recomendado)

Não precisa de Java nem Maven instalados. Sobe a aplicação, o Redis e o LocalStack (com a fila já criada):

```bash
docker compose up --build
```

Pronto: API, Swagger e consumer da fila funcionando. A primeira execução demora alguns minutos, porque baixa as imagens (o LocalStack tem cerca de 1,2 GB) e compila o projeto.

As portas `8080` (aplicação), `6379` (Redis) e `4566` (LocalStack) precisam estar livres. Para encerrar: `docker compose down`.

### Opção 2 — Aplicação pela IDE ou Maven, com a fila (requer JDK 21 e Docker)

Útil para depurar. O Redis e o LocalStack rodam no Docker e a aplicação roda na sua máquina. Não é preciso configurar nada: por padrão a aplicação já aponta para o LocalStack em `localhost:4566`.

```bash
docker compose up -d redis localstack
./mvnw spring-boot:run
```

Na IDE, em vez do segundo comando, rode a classe `MainApplication`.

> O consumer da fila vem **ligado por padrão**. Se o LocalStack não estiver no ar, a aplicação não sobe. Para isso existe a Opção 3.

### Opção 3 — Só a API, sem Docker (requer JDK 21)

Desliga o consumer com a variável `COUPON_SQS_ENABLED=false`. A aplicação sobe sozinha, sem Redis e sem fila:

```bash
# Linux / Mac
COUPON_SQS_ENABLED=false ./mvnw spring-boot:run

# Windows (PowerShell)
$env:COUPON_SQS_ENABLED="false"; .\mvnw.cmd spring-boot:run
```

### Endereços

| O quê | Endereço |
|---|---|
| Swagger | http://localhost:8080/swagger-ui.html |
| Console do H2 | http://localhost:8080/h2-console |

---

## 📚 Endpoints da API

| Método   | Endpoint        | Descrição                         | Sucesso          | Erros                                      |
|----------|-----------------|-----------------------------------|------------------|--------------------------------------------|
| `POST`   | `/coupon`       | Cria um novo cupom                | `201 Created`    | `400` requisição inválida / regra violada  |
| `GET`    | `/coupon/{id}`  | Busca um cupom pelo ID            | `200 OK`         | `404` não encontrado ou apagado            |
| `DELETE` | `/coupon/{id}`  | Apaga um cupom (soft delete)      | `200 OK`         | `404` não encontrado, `409` já apagado     |

O `{id}` é o identificador (UUID) devolvido pelo `POST`, não o código do cupom.

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

### 📄 Exemplo de resposta (`DELETE /coupon/{id}`)

```json
{ "message": "Cupom apagado com sucesso" }
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
- **Não é possível apagar um cupom já apagado** (`409 Conflict`). O soft delete é um único `UPDATE` condicional (só alcança o cupom ainda não apagado), então, com duas requisições simultâneas, o banco deixa passar apenas a primeira.
- O delete **não depende da versão lida**: se o status mudar pela fila entre a leitura e a gravação, o cupom é apagado mesmo assim. O update incrementa a versão (`@Version`), para que uma alteração de status lida antes do delete seja rejeitada em vez de "ressuscitar" o cupom.
- Um cupom apagado deixa de ser encontrado pelo `GET` (`404`).

---

## 🧭 Decisões e escopo

Pontos em que o enunciado deixava margem, e o que foi decidido:

- **Código com tamanho diferente de 6 após a limpeza:** a criação é **rejeitada** (`400`). A aplicação não trunca nem completa o código, para não transformar códigos diferentes no mesmo cupom sem o cliente saber.
- **Apagar um cupom já apagado:** responde `409 Conflict`, deixando explícito que a regra foi aplicada. Um cupom inexistente responde `404`.
- **Buscar um cupom apagado:** responde `404`. O registro continua no banco (soft delete), mas deixa de existir para quem consome a API.
- **Publicar ou alterar status por API:** ficou **fora do escopo de propósito**. O enunciado só define regras para criação e exclusão; o campo `published` é definido na criação, como pedido. Criar endpoints para isso exigiria inventar regras que o desafio não especifica.
- **Extras não solicitados:** o consumer SQS com idempotência e os logs com MDC foram adicionados para demonstrar mensageria e observabilidade. Eles não alteram o comportamento dos endpoints do desafio, e a API funciona sem a fila (`COUPON_SQS_ENABLED=false`).

---

## 📨 Consumer SQS: atualização de status

A aplicação consome a fila **FIFO** `coupon-status-queue.fifo` para **ativar ou inativar** cupons. Com o `docker compose`, o LocalStack já sobe com a fila e a DLQ (`coupon-status-dlq.fifo`) criadas.

### Testando em 4 passos

Com a aplicação no ar pela **Opção 1** ou pela **Opção 2**:

**1. Crie um cupom** pelo Swagger (`POST /coupon`) e copie o `id` da resposta.

**2. Envie uma mensagem para a fila.** O LocalStack tem um atalho que monta o JSON:

```bash
docker exec onebrain-localstack coupon-status evento-1 <id-do-cupom> INACTIVE
```

Os argumentos são: `eventId`, `couponId` e o novo status (`ACTIVE` ou `INACTIVE`).

**3. Confira o resultado.** O `GET /coupon/{id}` passa a mostrar `"status": "INACTIVE"`, e o log da aplicação registra:

```
[correlationId=evento-1 couponId=<id>] AtualizarStatusCouponUseCase : Atualizando status do cupom
[correlationId=evento-1 couponId=<id>] CouponStatusListener         : Mensagem processada
```

Para acompanhar o log no Docker: `docker logs -f onebrain`. Lá as mesmas mensagens saem em JSON, com `correlationId` e `couponId` como campos.

**4. Teste a idempotência.** Repita exatamente o comando do passo 2 (o atalho envia cada mensagem com um id de deduplicação novo, para que a repetição chegue até a aplicação em vez de ser descartada pelo SQS). O log mostra `Evento evento-1 já processado, ignorando` e o cupom não muda. Para reativar, envie um evento novo:

```bash
docker exec onebrain-localstack coupon-status evento-2 <id-do-cupom> ACTIVE
```

> **Atenção ao `eventId`:** o Redis guarda os eventos processados por 24 horas, mesmo que a aplicação seja reiniciada (o H2 zera, o Redis não). Use um `eventId` novo a cada mensagem, ou limpe os eventos com `docker exec onebrain-redis redis-cli flushall`.

### Comandos úteis

```bash
# estado de um evento no Redis (PROCESSING ou DONE)
docker exec onebrain-redis redis-cli get onebrain:coupon-status:evento:evento-1

# quantidade de mensagens na DLQ
docker exec onebrain-localstack awslocal sqs get-queue-attributes \
  --queue-url http://localhost:4566/000000000000/coupon-status-dlq.fifo \
  --attribute-names ApproximateNumberOfMessages
```

### Formato da mensagem

```json
{ "eventId": "evento-1", "couponId": "cef9d1e3-aae5-4ab6-a297-358c6032b1e7", "status": "INACTIVE" }
```

### Regras (no domínio, em `Coupon.alterarStatus`)
- A fila só alterna entre `ACTIVE` e `INACTIVE`.
- Cupom apagado não pode ter o status alterado.
- `DELETED` não é aceito pela fila: apagar é só pelo `DELETE /coupon/{id}`.

### Ordem das mensagens
A fila é **FIFO** e o `couponId` é o `MessageGroupId`: as mensagens de um mesmo cupom são entregues na ordem em que foram enviadas, uma de cada vez, e cupons diferentes continuam sendo processados em paralelo. Sem isso, um `INACTIVE` seguido de um `ACTIVE` poderia ser aplicado ao contrário e deixar o cupom no estado errado.

Enquanto uma mensagem falha e aguarda nova tentativa, as seguintes do mesmo cupom ficam retidas; elas só andam quando a mensagem é processada ou vai para a DLQ.

### Idempotência
A deduplicação da fila FIFO só cobre reenvios do produtor dentro de 5 minutos. Na entrega, a mensagem ainda volta se o consumer não confirmar a tempo, então a mesma mensagem pode chegar repetida. Cada `eventId` é registrado no **Redis**:

| Estado da chave | O que acontece com a mensagem |
|---|---|
| não existe | é reservada como `PROCESSING` (TTL 30s) e processada; ao terminar vira `DONE` (TTL 24h) |
| `DONE` | duplicata: é confirmada e ignorada |
| `PROCESSING` | outra instância ainda está processando: volta para a fila |

Se o processamento falha, a chave é apagada para a próxima entrega poder tentar de novo.

### Tratamento de falhas
- Mensagem inválida, cupom inexistente ou regra de negócio violada: registra log e descarta (repetir não resolveria).
- Falha temporária (banco ou Redis fora, conflito de versão) ou corpo que não é JSON: a mensagem volta para a fila e, depois de 3 tentativas, vai para a DLQ.

### Configuração

| Variável | Padrão | Para que serve |
|---|---|---|
| `COUPON_SQS_ENABLED` | `true` | liga ou desliga o consumer da fila |
| `COUPON_SQS_QUEUE` | `coupon-status-queue.fifo` | nome da fila |
| `AWS_ENDPOINT` | `http://localhost:4566` | endereço do SQS (LocalStack) |
| `AWS_REGION` | `us-east-1` | região |
| `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` | `test` / `test` | credenciais (as do LocalStack) |
| `REDIS_HOST` / `REDIS_PORT` | `localhost` / `6379` | Redis da idempotência |

Os padrões já funcionam com o `docker compose` deste repositório; nada precisa ser definido para testar.

---

## 🧪 Testes Automatizados

A aplicação conta com testes **unitários** (JUnit 5 + Mockito) e de **integração** (Spring Boot Test + MockMvc + H2), cobrindo as regras de negócio. Os testes não precisam de Docker, Redis nem LocalStack: eles desligam o consumer da fila.

```bash
./mvnw verify
```

- O build **falha** se a cobertura de linhas ficar abaixo de **80%** (JaCoCo).
- Relatório de cobertura: `target/site/jacoco/index.html`

---

## 🧠 Arquitetura

Este projeto segue os princípios de **Arquitetura Hexagonal / Clean Architecture**, separando bem as responsabilidades:

- **Domain** → Regras de negócio puras (`model`) e portas de saída (`port`). Não conhece web, fila nem banco.
- **Application** → Casos de uso: orquestram o fluxo chamando o domínio e as portas, sem regra de negócio e sem saber se a entrada é HTTP ou fila.
- **Infrastructure** → Adaptadores: entrada HTTP (`web`), entrada por fila (`messaging`), acesso a dados com JPA (`repository`) e idempotência com Redis (`idempotency`).

```
src/main/java/com/onebrain/coupon
├── application
│   ├── exception
│   ├── port            # IIdempotenciaPort
│   └── useCase         # Criar, BuscarPorId, Apagar e AtualizarStatus
│       ├── command     # dados de entrada dos casos de uso
│       └── interfaces
├── domain
│   ├── exception       # violações de regra de negócio
│   ├── model           # Coupon, CouponStatus
│   └── port/repository # portas de saída
└── infrastructure
    ├── exception
    ├── idempotency     # RedisIdempotenciaAdapter
    ├── messaging       # CouponStatusListener (SQS)
    ├── repository      # entity, impl, interfaces (Spring Data), mapper
    └── web
        ├── config      # OpenApiConfig
        ├── exception   # GlobalExceptionHandler, ApiErrorMessage
        ├── filter      # MdcFilter
        ├── mapper      # API <-> caso de uso
        └── rest        # CouponController
```

O contrato da API fica em [`src/main/resources/openapi.yaml`](src/main/resources/openapi.yaml); a interface `CouponApi`, implementada pelo `CouponController`, e os modelos de requisição/resposta são gerados a partir dele no build.

---

## 🔎 Observabilidade

Cada requisição em `/coupon` recebe um **correlationId**, propagado pelo **MDC** para todos os logs daquela requisição. Nas mensagens da fila, o `eventId` faz esse papel.

- Se o cliente enviar o header `X-Correlation-Id`, ele é reaproveitado; caso contrário, a aplicação gera um UUID.
- O mesmo valor volta no header `X-Correlation-Id` da resposta.
- Campos no MDC: `correlationId`, `couponId`, `httpMethod` e `path`.
- Ao final de cada requisição é registrado um log com status e tempo de resposta.

Rodando pela IDE ou pelo Maven, os logs saem em texto:

```
INFO [onebrain] [correlationId=teste-123 couponId=7727dd1b-...] ... Requisição finalizada: DELETE /coupon/7727dd1b-..., status=200, duracaoMs=12
```

No Docker, os logs saem em **JSON estruturado** (Elastic Common Schema), com os campos do MDC, prontos para Datadog ou Grafana Loki. Para ligar fora do Docker, defina a variável `LOGGING_STRUCTURED_FORMAT_CONSOLE=ecs`.

---

## 📦 Banco de Dados

A aplicação utiliza o **H2 Database** (em memória), facilitando o uso sem necessidade de instalação. Os dados são perdidos ao reiniciar a aplicação.

Acesse o console H2 em [http://localhost:8080/h2-console](http://localhost:8080/h2-console):

- **JDBC URL:** `jdbc:h2:mem:onebrain`
- **Usuário:** `sa`
- **Senha:** *(em branco)*

Para ver o soft delete: `SELECT ID, CODE, STATUS, DELETED_AT FROM COUPONS;`
