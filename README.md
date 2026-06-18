# Gestão de Processos Jurídicos — Teste Técnico

> Módulo de **cadastro e gestão de processos jurídicos** para uma plataforma de
> **procuradoria digital**. Implementação ponta a ponta (Angular + Spring Boot),
> desenvolvida como parte de um teste técnico de desenvolvimento e análise de incidentes.

<!-- TODO (opcional): adicionar badges de build/coverage depois de configurar CI -->
<!-- ![build](...) ![coverage](...) -->

## Sumário

- [Sobre o projeto](#sobre-o-projeto)
- [Funcionalidades](#funcionalidades)
- [Stack e tecnologias](#stack-e-tecnologias)
- [Arquitetura](#arquitetura)
- [Pré-requisitos](#pré-requisitos)
- [Como executar](#como-executar)
- [Documentação da API](#documentação-da-api)
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

A funcionalidade escolhida foi o **gerenciamento de processos jurídicos**, por ser central
ao domínio de uma procuradoria digital e por permitir demonstrar validações de domínio reais
(número CNJ), regras de negócio (transições de status) e um fluxo CRUD completo.
A justificativa detalhada da escolha está em [`docs/ESCOPO.md`](docs/ESCOPO.md).

## Funcionalidades

- Cadastrar, editar, consultar e listar processos jurídicos.
- Validação do **número único CNJ** (formato `NNNNNNN-DD.AAAA.J.TR.OOOO` e dígito verificador).
- **Máquina de estados** de status do processo, com transições controladas (ex.: `DISTRIBUIDO → EM_ANDAMENTO → SUSPENSO → ARQUIVADO`).
- Listagem com **busca e paginação**.
- **Validações de formulário** no front-end e validação de domínio no back-end.
- **Logs estruturados** com `correlationId` por requisição, para diagnóstico.

<!-- TODO: ajustar a lista conforme o que você realmente implementar -->

## Stack e tecnologias

**Back-end**
- Java 25
- Spring Boot 4 (Spring Web, Spring Data JPA, Bean Validation)
- Flyway (migrations)
- springdoc-openapi (Swagger UI)
- SLF4J / Logback (logging estruturado)
- JUnit 5 + Mockito (testes)

**Front-end**
- Angular `<!-- TODO: versão -->` (componentes standalone, Reactive Forms)
- TypeScript

**Banco de dados**
- PostgreSQL (execução via Docker)
- H2 em memória (perfil de dev/teste)

**Infra de desenvolvimento**
- Docker / Docker Compose

> Observação: Java 25 e Spring Boot 4 são versões recentes; a escolha e seus
> trade-offs estão comentados na [nota técnica](docs/NOTA-TECNICA.md).

## Arquitetura

Arquitetura em camadas no back-end (`Controller → Service → Repository`), com DTOs na borda,
tratamento global de erros (`@ControllerAdvice` retornando `ProblemDetail` / RFC 7807) e
logging transversal via filtro de `correlationId` (MDC).

No front-end, uma camada de `service` (HttpClient) isola o acesso à API, com um interceptor
para tratamento centralizado de erros e os componentes de listagem e formulário.

Diagrama e decisões detalhadas em [`docs/NOTA-TECNICA.md`](docs/NOTA-TECNICA.md).

<!-- TODO (opcional): incluir um diagrama (imagem ou mermaid) aqui -->

## Pré-requisitos

- Docker e Docker Compose `<!-- TODO: versões -->`
- (Para rodar fora do Docker) JDK 25, Maven `<!-- ou Gradle -->`, Node.js `<!-- TODO: versão -->`

## Como executar

### Opção 1 — Docker Compose (recomendado)

```bash
# TODO: validar os comandos após criar o docker-compose.yml
docker-compose up --build
```

Após subir:
- API: `http://localhost:8080`
- Front-end: `http://localhost:4200`
- Swagger UI: `http://localhost:8080/swagger-ui.html` <!-- TODO: confirmar caminho -->

### Opção 2 — Executando manualmente

**Back-end**
```bash
cd backend
# TODO: ajustar conforme Maven ou Gradle
./mvnw spring-boot:run
```

**Front-end**
```bash
cd frontend
npm install
npm start
```

<!-- TODO: documentar variáveis de ambiente / application.properties relevantes -->

## Documentação da API

Os endpoints estão documentados via OpenAPI/Swagger em `http://localhost:8080/swagger-ui.html`.

Resumo dos principais endpoints:

| Método | Rota                     | Descrição                          |
|--------|--------------------------|------------------------------------|
| GET    | `/api/processos`         | Lista processos (busca + paginação)|
| GET    | `/api/processos/{id}`    | Consulta um processo               |
| POST   | `/api/processos`         | Cadastra um processo               |
| PUT    | `/api/processos/{id}`    | Atualiza um processo               |
| PATCH  | `/api/processos/{id}/status` | Altera o status do processo    |
| DELETE | `/api/processos/{id}`    | Remove um processo                 |

<!-- TODO: ajustar a tabela aos endpoints reais que você implementar -->

Exemplo de requisição:

```bash
# TODO: substituir pelo payload real
curl -X POST http://localhost:8080/api/processos \
  -H "Content-Type: application/json" \
  -d '{
    "numeroCnj": "0000000-00.0000.0.00.0000",
    "parteAutora": "...",
    "parteRe": "...",
    "vara": "...",
    "valorCausa": 0.0
  }'
```

## Testes

```bash
# Back-end
cd backend
./mvnw test   # TODO: ajustar Maven/Gradle

# Front-end
cd frontend
npm test
```

Cenários cobertos: caminho feliz (cadastro válido), validação falhando
(CNJ inválido), e transição de status não permitida.
<!-- TODO: confirmar a cobertura real após escrever os testes -->

## Logs e diagnóstico

A aplicação emite logs estruturados com `correlationId`, endpoint e contexto da operação.
Eventos relevantes (validação falha, transição de status, falha de persistência, exceções)
são registrados para permitir diagnóstico. Esses logs são a base da Parte 2.

<!-- TODO: descrever onde os logs são gravados e o formato (console JSON, arquivo, etc.) -->

## Estrutura do repositório

```
.
├── backend/            # API em Spring Boot
├── frontend/           # Aplicação Angular
├── docs/
│   ├── ESCOPO.md             # Funcionalidade escolhida e requisitos
│   ├── NOTA-TECNICA.md       # Decisões, trade-offs e melhorias futuras
│   └── ANALISE-INCIDENTE.md  # Parte 2 — análise do incidente
├── docker-compose.yml
└── README.md
```
<!-- TODO: ajustar à estrutura real -->

## Parte 2 — Análise de Incidente

A análise do erro recorrente, com causa-raiz, correção e medidas de prevenção,
está em [`docs/ANALISE-INCIDENTE.md`](docs/ANALISE-INCIDENTE.md).

## Documentos relacionados

- [Escopo do desafio](docs/ESCOPO.md)
- [Nota técnica](docs/NOTA-TECNICA.md)
- [Análise de incidente](docs/ANALISE-INCIDENTE.md)

## Autor

`<!-- TODO: seu nome e contato -->`
