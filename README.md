# Gestão de Processos Jurídicos — Teste Técnico

> Módulo de **cadastro e gestão de processos jurídicos** para uma plataforma de
> **procuradoria digital**. Implementação ponta a ponta (Angular + Spring Boot),
> desenvolvida como parte de um teste técnico de desenvolvimento e análise de incidentes.

<!-- TODO (opcional): adicionar badges de build/coverage depois de configurar CI -->

## Sumário

- [Sobre o projeto](#sobre-o-projeto)
- [Funcionalidades](#funcionalidades)
- [Stack e tecnologias](#stack-e-tecnologias)
- [Arquitetura](#arquitetura)
- [Pré-requisitos](#pré-requisitos)
- [Como executar](#como-executar)
- [Documentação da API](#documentação-da-api)
- [Coleção do Postman](#coleção-do-postman)
- [Testes](#testes)
- [Logs e diagnóstico](#logs-e-diagnóstico)
- [Estrutura do repositório](#estrutura-do-repositório)
- [Parte 2 — Análise de Incidente](#parte-2--análise-de-incidente)
- [Documentos relacionados](#documentos-relacionados)
- [Autor](#autor)

## Sobre o projeto

Este repositório contém a solução para um teste técnico composto por duas partes:

1. **Desenvolvimento** — uma funcionalidade ponta a ponta (front-end, API, persistência e logs).
2. **Análise de incidente** — investigação de um erro recorrente a partir de logs, com causa-raiz, correção e prevenção.

A funcionalidade escolhida foi o **gerenciamento de processos jurídicos** (entidade `LegalCase`),
por ser central ao domínio de uma procuradoria digital e por permitir demonstrar validações de
domínio reais (número CNJ), regras de negócio (transições de status) e um fluxo CRUD completo.
A justificativa detalhada está em [`docs/ESCOPO.md`](docs/ESCOPO.md).

## Funcionalidades

- Cadastrar, editar, consultar e listar processos jurídicos.
- Validação do **número único CNJ** (formato `NNNNNNN-DD.AAAA.J.TR.OOOO` + dígito verificador, ISO 7064).
- **Máquina de estados** de status: `FILED → IN_PROGRESS → (SUSPENDED ⇄ IN_PROGRESS) → ARCHIVED | CLOSED`.
- Listagem com **filtros** (status, parte, vara, intervalo de data) e **paginação**.
- **Exclusão lógica (soft delete)** — registros removidos não aparecem nas consultas.
- **Validações** no front-end e no back-end.
- **Logs** com `correlationId` por requisição, para diagnóstico.

## Stack e tecnologias

**Back-end**
- Java 21
- Spring Boot 3.5.x (Spring Web, Spring Data JPA, Bean Validation)
- Flyway (migrations)
- springdoc-openapi (Swagger UI)
- SLF4J / Logback (logging com correlationId)
- JUnit 5 + Mockito (testes)

**Front-end**
- Angular `<!-- TODO: versão -->` (componentes standalone, Reactive Forms)
- TypeScript

**Banco de dados**
- PostgreSQL (dev e produção, via Docker)
- H2 em memória (perfil de teste)

**Infra de desenvolvimento**
- Docker / Docker Compose

## Arquitetura

Arquitetura em camadas no back-end (`Controller → Service → Repository`), com DTOs na borda,
tratamento global de erros (`@RestControllerAdvice` retornando `ProblemDetail` / RFC 7807) e
logging transversal via filtro de `correlationId` (MDC).

No front-end, uma camada de `service` (HttpClient) isola o acesso à API, com um interceptor para
tratamento centralizado de erros e os componentes de listagem e formulário.

Decisões detalhadas em [`docs/NOTA-TECNICA.md`](docs/NOTA-TECNICA.md).

## Pré-requisitos

- Docker e Docker Compose
- (Para rodar fora do Docker) JDK 21, Maven (wrapper incluso) e Node.js `<!-- TODO: versão -->`

## Como executar

### Opção 1 — Docker Compose (recomendado)

```bash
# TODO: validar os comandos após criar o docker-compose.yml
docker-compose up --build
```

Após subir:
- API: `http://localhost:8080`
- Front-end: `http://localhost:4200`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

### Opção 2 — Executando manualmente

**Banco (Postgres para dev):**
```bash
docker run --name legalcase-pg \
  -e POSTGRES_DB=legalcase -e POSTGRES_USER=legalcase -e POSTGRES_PASSWORD=legalcase \
  -p 5432:5432 -d postgres:16
```

**Back-end:**
```bash
cd backend/processos-juridicos
./mvnw spring-boot:run
```

**Front-end:**
```bash
cd frontend
npm install
npm start
```

## Documentação da API

Endpoints documentados via OpenAPI/Swagger em `http://localhost:8080/swagger-ui.html`.

| Método | Rota                                  | Descrição                              |
|--------|---------------------------------------|----------------------------------------|
| POST   | `/api/v1/legal-cases`                 | Cadastra um processo                   |
| GET    | `/api/v1/legal-cases/{id}`            | Consulta um processo                   |
| GET    | `/api/v1/legal-cases`                 | Lista/filtra processos (paginado)      |
| PUT    | `/api/v1/legal-cases/{id}`            | Atualiza os dados de um processo       |
| PATCH  | `/api/v1/legal-cases/{id}/status`     | Altera o status (máquina de estados)   |
| DELETE | `/api/v1/legal-cases/{id}`            | Exclui logicamente (soft delete)       |

Exemplo de requisição:

```bash
curl -i -X POST http://localhost:8080/api/v1/legal-cases \
  -H "Content-Type: application/json" \
  -d '{
    "cnjNumber": "9068906-21.2026.4.02.3738",
    "plaintiff": "Município de São Paulo",
    "defendant": "Empresa XYZ Ltda",
    "court": "1ª Vara da Fazenda Pública",
    "judicialDistrict": "São Paulo",
    "claimValue": 150000.00,
    "filingDate": "2024-03-15"
  }'
```

Erros seguem o formato RFC 7807 (`application/problem+json`), com `type`, `title`, `status`,
`detail`, `correlationId` e, em validações, o mapa `errors`.

## Coleção do Postman

A coleção com todos os endpoints e os principais cenários está em
[`postman/legal-case-api.postman_collection.json`](postman/legal-case-api.postman_collection.json).

Para usar: **Import** no Postman → selecione o arquivo. A coleção usa a variável
`{{baseUrl}}` (padrão `http://localhost:8080`); ajuste no environment se necessário.

## Testes

```bash
# Back-end
cd backend/processos-juridicos
./mvnw test

# Front-end
cd frontend
npm test   # TODO: confirmar após implementar
```

Cenários cobertos no back-end: validação de CNJ (formato e dígito), máquina de estados (transições
válidas e inválidas), unicidade sob concorrência (constraint + soft delete), e o contrato HTTP de
cada endpoint (201/200/204/400/404/409/422).

## Logs e diagnóstico

A aplicação emite logs com `correlationId` por requisição, no padrão:

```
2026-06-19 15:17:39.311 INFO  [<correlationId>] c.a.l.service.LegalCaseService - Legal case created id=6 cnj=9068906-21.2026.4.02.3738
```

O mesmo `correlationId` é devolvido no header `X-Correlation-Id` e incluído no corpo dos erros,
permitindo correlacionar requisição → log → erro. Esses logs são a base da Parte 2.

## Estrutura do repositório

```
.
├── backend/
│   └── processos-juridicos/   # API em Spring Boot
├── frontend/                  # Aplicação Angular
├── postman/                   # Coleção do Postman
├── docs/
│   ├── ESCOPO.md              # Funcionalidade escolhida e requisitos
│   ├── NOTA-TECNICA.md        # Decisões, trade-offs e melhorias futuras
│   └── ANALISE-INCIDENTE.md   # Parte 2 — análise do incidente
├── docker-compose.yml         # TODO
└── README.md
```

## Parte 2 — Análise de Incidente

A análise do erro recorrente (duplicidade de CNJ sob concorrência), com causa-raiz, correção e
medidas de prevenção, está em [`docs/ANALISE-INCIDENTE.md`](docs/ANALISE-INCIDENTE.md).

## Documentos relacionados

- [Escopo do desafio](docs/ESCOPO.md)
- [Nota técnica](docs/NOTA-TECNICA.md)
- [Análise de incidente](docs/ANALISE-INCIDENTE.md)

## Autor

`<!-- TODO: seu nome e contato -->`
