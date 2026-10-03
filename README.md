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
| `GET`    | `/coupon/{id}`  | Busca um cupom pelo ID            | `200 OK`         | `404` não encontrado (ou já apagado)       |
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

- **Domain** → Regras de negócio puras (`model`), casos de uso (`useCase`) e portas (`port`)
- **Application** → Entrada HTTP (`port/rest`), mapeamento da API, tratamento de erros e configuração
- **Infrastructure** → Acesso a dados (JPA), entidades e implementações das portas

```
src/main/java/com/onebrain/coupon
├── application
│   ├── config          # OpenApiConfig
│   ├── exception       # GlobalExceptionHandler, ApiErrorMessage
│   ├── mapper          # API <-> domínio
│   └── port/rest       # CouponController
├── domain
│   ├── exception       # violações de regra de negócio
│   ├── model           # Coupon, CouponStatus
│   ├── port/repository # portas de saída
│   └── useCase         # casos de uso e suas interfaces
└── infrastructure
    ├── exception
    └── repository      # entity, impl, interfaces (Spring Data), mapper
```

O contrato da API fica em [`src/main/resources/openapi.yaml`](src/main/resources/openapi.yaml); a interface `CouponApi` e os modelos de requisição/resposta são gerados a partir dele no build.

---

## 📦 Banco de Dados

A aplicação utiliza o **H2 Database** (em memória), facilitando o uso sem necessidade de instalação.

Acesse o console H2 em:
- [http://localhost:8080/h2-console](http://localhost:8080/h2-console)

- **JDBC URL:** `jdbc:h2:mem:onebrain`

- **Usuário:** `sa`

- **Senha:** *(em branco)*
