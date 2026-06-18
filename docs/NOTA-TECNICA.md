# Nota Técnica

Documento que registra as **decisões de engenharia**, os **trade-offs** assumidos e as
**melhorias futuras** identificadas. O objetivo é tornar o raciocínio por trás do código
explícito e o projeto autoexplicativo.

## 1. Contexto e objetivo

Implementar, de forma ponta a ponta, um módulo de **gestão de processos jurídicos** para
uma plataforma de procuradoria digital, demonstrando boas práticas em front-end, API,
persistência, logging e testes. O escopo detalhado está em [`ESCOPO.md`](docs/ESCOPO.md).

## 2. Decisões de arquitetura

### 2.1. Stack

- **Java 25 + Spring Boot 4.** `<!-- TODO: justifique brevemente: produtividade, ecossistema, versões LTS/recentes -->`
- **Angular** no front-end, com componentes standalone e Reactive Forms.
- **PostgreSQL** como banco de produção, **H2** para desenvolvimento/teste.

> Trade-off: optar por versões recentes (Java 25 / Spring Boot 4) traz ganhos de linguagem
> e plataforma, mas exige atenção à maturidade do ecossistema e à compatibilidade de
> dependências. `<!-- TODO: comente se enfrentou algum atrito -->`

### 2.2. Camadas do back-end

Estrutura `Controller → Service → Repository`, com DTOs na borda da API.

- **Por quê:** separação de responsabilidades, facilidade de teste (mock do service),
  e isolamento do modelo de persistência em relação ao contrato da API.

### 2.3. Validação

- **Bean Validation** para validações de campo (obrigatoriedade, formato, ranges).
- **Validador customizado de CNJ** para a regra de domínio (formato + dígito verificador).
- **Por quê:** validações declarativas no DTO mantêm o controller limpo; o validador
  customizado encapsula a regra de negócio específica do domínio jurídico.

### 2.4. Regra de negócio — máquina de estados

As transições de status são controladas explicitamente no service, rejeitando transições
inválidas. `<!-- TODO: descreva como implementou: enum + mapa de transições, etc. -->`

- **Por quê:** garante a integridade do ciclo de vida do processo e produz um ponto de
  log claro para diagnóstico.

### 2.5. Tratamento de erros

`@ControllerAdvice` global retornando `ProblemDetail` (RFC 7807), mapeando:
validação → `400/422`, conflito/duplicidade → `409`, não encontrado → `404`.

- **Por quê:** respostas de erro consistentes e previsíveis para o front, e um ponto
  central de logging das exceções.

### 2.6. Persistência e migrations

Spring Data JPA + **Flyway** para versionamento do schema, com **constraint única** no
`numeroCnj`.

- **Por quê:** schema versionado e reproduzível; a unicidade no banco protege contra
  condições de corrida (ver Parte 2).

### 2.7. Logging e observabilidade

Logs estruturados com `correlationId` por requisição (filtro + MDC), registrando eventos
relevantes do fluxo.

- **Por quê:** rastreabilidade ponta a ponta e base concreta para a análise de incidente
  da Parte 2.

## 3. Trade-offs assumidos

`<!-- Liste o que você decidiu NÃO fazer e por quê. Exemplos abaixo. -->`

- **Sem autenticação/autorização** — fora do escopo do teste; foco no fluxo de negócio.
- **H2 em dev** — execução simples; Postgres reservado para o ambiente "de produção" via Docker.
- **Monolito** — adequado ao tamanho do problema; microsserviços seriam over-engineering.
- **Sem cache** — volume não justifica; adicionaria complexidade sem ganho no escopo.
- `<!-- TODO: adicione/ajuste conforme suas decisões reais -->`

## 4. Estratégia de testes

- **Unitários** no service (regras de negócio: validação de CNJ, transições de status) com Mockito.
- **De integração** no controller (MockMvc) e/ou na camada de persistência (`@DataJpaTest`).
- Cenários priorizados: caminho feliz, validação falhando, transição inválida, duplicidade.

`<!-- TODO: registre a cobertura e o que ficou de fora, se algo ficou -->`

## 5. Melhorias futuras

`<!-- Demonstra visão de produto/engenharia. Exemplos: -->`

- Autenticação/autorização por perfil (procurador, assistente).
- Cadastro de prazos, audiências e movimentações vinculados ao processo.
- Anexos de documentos.
- Auditoria completa (histórico de alterações).
- Métricas e alertas (ex.: Micrometer + Prometheus) sobre os erros logados.
- Paginação e filtros mais ricos na listagem.
- `<!-- TODO: ajuste à sua visão -->`

## 6. Priorização e tempo

`<!-- Opcional, mas valorizado: o que você priorizou dado o tempo e por quê. -->`
`<!-- Ex.: priorizei robustez do back e dos testes; o front cobre o fluxo essencial. -->`
