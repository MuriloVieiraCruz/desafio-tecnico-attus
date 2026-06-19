# Nota Técnica

Documento que registra as **decisões de engenharia**, os **trade-offs** assumidos e as
**melhorias futuras** identificadas. O objetivo é tornar o raciocínio por trás do código
explícito e o projeto autoexplicativo.

## 1. Contexto e objetivo

Implementar, de forma ponta a ponta, um módulo de **gestão de processos jurídicos** para uma
plataforma de procuradoria digital, demonstrando boas práticas em front-end, API, persistência,
logging e testes. O escopo detalhado está em [`ESCOPO.md`](ESCOPO.md) e a análise de incidente
da Parte 2 em [`ANALISE-INCIDENTE.md`](ANALISE-INCIDENTE.md).

## 2. Stack

- **Java 21 + Spring Boot 3.5.x** (Spring Web, Data JPA, Bean Validation).
- **Flyway** para versionamento de schema; **springdoc-openapi** para a documentação.
- **Angular** no front-end (componentes standalone, Reactive Forms).
- **PostgreSQL** em dev e prod; **H2** apenas nos testes.

> **Trade-off — versões maduras em vez de bleeding-edge.** A primeira intenção era Java 25 +
> Spring Boot 4, mas optei por Java 21 (LTS) + Spring Boot 3.5.x por serem versões maduras com
> ecossistema estável. Em um teste com prazo, o risco de atrito de compatibilidade (springdoc,
> Lombok e afins ainda alcançando o JDK/framework novos) não compensava o ganho.

## 3. Arquitetura em camadas

Estrutura `Controller → Service → Repository`, com DTOs na borda da API e um mapper dedicado.
Isso dá separação de responsabilidades, facilita o teste (mock do service) e isola o modelo de
persistência do contrato público da API.

## 4. Domínio e máquina de estados

O ciclo de vida do processo é uma **máquina de estados** implementada no próprio enum
`CaseStatus` (método `canTransitionTo` + tabela de transições permitidas). Isso mantém a regra
de negócio dentro do domínio, impede saltos ilógicos (ex.: `FILED` direto para `ARCHIVED`) e é
trivial de testar isoladamente.

Optei por **não** usar o State pattern (GoF) completo: para 5 estados com regra simples, uma
classe por estado seria over-engineering; a tabela no enum é mais legível e suficiente.

## 5. Validação

- **Bean Validation** para validações de campo (obrigatoriedade, tamanho, faixas).
- **Validador customizado de CNJ** (`@ValidCnj`) como anotação que pluga no Bean Validation e
  roda automaticamente no DTO. O dígito verificador segue **ISO 7064 (MOD 97-10)**, que é o
  algoritmo do CNJ.
- **DTOs `Create` e `Update` separados:** o `cnjNumber` não muda depois de criado; pedir um
  campo que seria ignorado no update é um cheiro ruim. Por isso o update não expõe o CNJ nem o
  status (status só muda pelo fluxo dedicado).

## 6. Persistência e soft delete

- Spring Data JPA + **Flyway**, com migrations em **SQL agnóstico** (rodam tanto em Postgres
  quanto em H2 modo PostgreSQL) e `ddl-auto=validate` (Flyway é dono do schema; Hibernate valida).
- **Soft delete** via `@SQLDelete` + `@SQLRestriction` (Hibernate 6): o `delete` vira um update
  de `deleted_at` e todas as queries filtram os removidos automaticamente. Adequado a um domínio
  jurídico, onde se preserva o registro por retenção/auditoria.
- **Nuance da unicidade (conecta com a Parte 2):** a constraint única de `cnj_number` é
  **global**, incluindo registros removidos. Isso é correto (um CNJ é único para sempre), mas
  cria uma divergência interessante: a checagem na aplicação não enxerga os removidos (por causa
  do `@SQLRestriction`), enquanto a constraint do banco sim. Essa lacuna "checagem na app vs.
  garantia no banco" é o tema do incidente analisado na Parte 2.

## 7. Concorrência e unicidade

- **Dupla guarda de unicidade:** `existsByCnjNumber` resolve o caso comum com mensagem amigável;
  a **constraint do banco + `catch`** cobrem a condição de corrida. Nenhuma sozinha é suficiente.
- **`saveAndFlush` em vez de `save`** no cadastro: com `save`, a violação da constraint só
  estouraria no commit, fora do `try`. O flush força a exceção a aparecer dentro do bloco, onde é
  traduzida num `409` limpo. É exatamente o mecanismo descrito na Parte 2.
- **`cnjNumber` imutável** e **status sempre via fluxo dedicado** (máquina de estados), nunca pelo
  update genérico.

## 8. API e contrato

- Rotas versionadas em `/api/v1/legal-cases`.
- **`201 Created` + header `Location`** apontando para o recurso criado.
- **`PagedModel`** (de `spring-data-web`) em vez de devolver `Page` cru — o Spring desencoraja
  serializar `PageImpl` diretamente (JSON instável); o `PagedModel` dá um contrato estável com
  `content` + metadados de página.
- **`PATCH /{id}/status`** (alteração parcial) separado do **`PUT /{id}`** de detalhes.
- **Soft delete devolvendo `204`**.
- **Documentação Swagger em interface dedicada** (`LegalCaseControllerDoc`), referenciada pelo
  controller. O trade-off é a verbosidade (dois arquivos por controller), mas mantém o controller
  limpo e a consistência é garantida pelo compilador (`@Override`).

## 9. Tratamento de erros

`@RestControllerAdvice` global retornando `ProblemDetail` (RFC 7807), com `correlationId`
anexado a cada erro. Mapeamento de status:

- `404` — recurso não encontrado.
- `409` — CNJ duplicado (conflito real de recurso).
- `422` — transição de status inválida (requisição bem-formada, mas viola regra de negócio sobre
  o estado atual). O `422` é uma escolha defensável; `409` também seria — o importante é ser
  consistente e justificar.
- `400` — falha de validação, com o mapa `errors` (campo → mensagem).

## 10. Logging e observabilidade

Logs com `correlationId` por requisição (filtro `OncePerRequestFilter` + MDC), devolvido também
no header `X-Correlation-Id` e incluído no corpo dos erros. Eventos relevantes (criação,
transição de status, duplicidade concorrente, exceções) são registrados. Logging em **JSON
estruturado** (encoder do Logstash) fica como melhoria futura; para o teste, o padrão com
`correlationId` já entrega o "log mínimo para diagnóstico" do enunciado.

## 11. Design patterns

- **Specification (`JpaSpecificationExecutor`)** — justificado: há vários filtros opcionais e
  combináveis; sem ele, haveria explosão combinatória de métodos no repository
  (`findByStatus`, `findByStatusAndCourt`, ...). Cada filtro é uma peça componível.
  Uso a API moderna do Spring Data JPA 3.5+: `Specification.allOf(...)` e
  `Specification.unrestricted()` (em vez de retornar `null` da lambda, que é desencorajado), com
  a checagem de nulo/vazio feita **fora** da lambda.
- **Deliberadamente evitados:** State pattern completo (over-engineering para 5 estados),
  MapStruct (mapper manual é transparente para uma entidade só — melhoria futura se crescer) e
  CRUD genérico abstrato (abstração prematura).

## 12. Estratégia de testes

- **Unitários:** `CnjValidator`, máquina de estados (`CaseStatus`) e o service com Mockito.
- **Slices:** `@WebMvcTest` (controller + handler + validação) e `@DataJpaTest` contra o schema
  real do Flyway (testa soft delete e a constraint única).
- **Depreciações evitadas:** `@MockitoBean` (não `@MockBean`, depreciado no 3.4),
  `MockitoExtension` + `@Mock`/`@InjectMocks` nos unitários e
  `@AutoConfigureTestDatabase(replace = NONE)` para usar o schema real.
- **Ressalva do dado de teste do CNJ:** usei um número que a própria aplicação aceitou — ótimo
  como teste de regressão, mas levemente circular (testa o validador contra ele mesmo). Vale
  cruzar com um CNJ sabidamente válido de fonte externa para confirmar o **algoritmo**, não só a
  consistência.
- **Cobertura:** o JaCoCo pode ser plugado no `pom.xml` para gerar relatório com `./mvnw test`
  (melhoria opcional).

## 13. Trade-offs assumidos

- **Sem autenticação/autorização** — fora do escopo; foco no fluxo de negócio.
- **Sem auditoria de autoria** (`created_by`/`updated_by`) — depende de auth (ver melhorias).
- **H2 só em testes; Postgres em dev/prod** — paridade dev/prod, com testes rápidos e isolados.
- **Monolito** — adequado ao tamanho do problema; microsserviços seriam over-engineering.
- **Sem cache** — volume não justifica.
- **Verbosidade da doc em interface** — aceita em troca de controller limpo e organização.

## 14. Melhorias futuras

- Autenticação/autorização por perfil (procurador, assistente) e, com isso, **auditoria de
  autoria** via Spring Data JPA Auditing (`@CreatedBy`/`@LastModifiedBy` + um `AuditorAware<String>`
  lendo o usuário do `SecurityContext`).
- **Idempotência** no cadastro (header `Idempotency-Key`) para tratar *retries* de forma segura.
- Logging em **JSON estruturado** e métricas/alertas (Micrometer + Prometheus) sobre os erros.
- Teste de **concorrência** real (duas threads) reproduzindo a corrida do CNJ.
- MapStruct, caso o número de mapeamentos cresça; cobertura com JaCoCo no CI.
- Cadastro de prazos, audiências e movimentações; anexos; histórico completo de alterações.

## 15. Priorização

Dado o tempo, priorizei a **robustez do back-end e dos testes** (validações, máquina de estados,
unicidade sob concorrência, tratamento de erros padronizado e diagnóstico via `correlationId`),
com o front cobrindo o fluxo completo essencial. As decisões "feitas do jeito certo" foram
preferidas a um escopo maior e mais raso.
