# Nota Técnica

Este documento registra as decisões de engenharia, os trade-offs assumidos e as melhorias
futuras identificadas. A ideia é deixar explícito o raciocínio por trás do código.

## 1. Contexto e objetivo

Implementar, de ponta a ponta, um módulo de gestão de processos jurídicos para uma plataforma
de procuradoria digital, demonstrando boas práticas em front-end, API, persistência, logging e
testes. O escopo detalhado está em [`ESCOPO.md`](ESCOPO.md) e a análise de incidente da Parte 2
em [`ANALISE-INCIDENTE.md`](ANALISE-INCIDENTE.md).

## 2. Stack

- Java 21 + Spring Boot 3.5.x (Spring Web, Data JPA, Bean Validation).
- Flyway para versionamento de schema e springdoc-openapi para a documentação.
- Angular 20 no front-end (componentes standalone, Reactive Forms).
- PostgreSQL em dev e prod; H2 apenas nos testes.

Sobre as versões: a primeira intenção era Java 25 + Spring Boot 4, mas optei por Java 21 (LTS)
+ Spring Boot 3.5.x, que são versões maduras e com ecossistema estável. Num teste com prazo, o
risco de atrito de compatibilidade (springdoc, Lombok e companhia ainda alcançando o JDK e o
framework novos) não compensava o ganho.

## 3. Arquitetura em camadas

A estrutura é `Controller → Service → Repository`, com DTOs na borda da API e um mapper
dedicado. Isso separa responsabilidades, facilita o teste (basta mockar o service) e isola o
modelo de persistência do contrato público da API.

## 4. Domínio e máquina de estados

O ciclo de vida do processo é uma máquina de estados implementada no próprio enum `CaseStatus`
(método `canTransitionTo` mais uma tabela de transições permitidas). Manter a regra dentro do
domínio impede saltos ilógicos, como ir de `FILED` direto para `ARCHIVED`, e torna o
comportamento trivial de testar isoladamente.

Não usei o State pattern (GoF) completo de propósito. Para cinco estados com regra simples, uma
classe por estado seria over-engineering; a tabela no enum é mais legível e dá conta do recado.

## 5. Validação

As validações de campo (obrigatoriedade, tamanho, faixas) ficam por conta do Bean Validation.
A regra de domínio do CNJ é um validador customizado (`@ValidCnj`), uma anotação que se pluga no
Bean Validation e roda automaticamente sobre o DTO. O dígito verificador segue o ISO 7064
(MOD 97-10), que é o algoritmo usado pelo CNJ.

Os DTOs de criação e de atualização são separados. O `cnjNumber` não muda depois de criado, e
pedir num update um campo que seria ignorado é um cheiro ruim. Por isso o update não expõe o CNJ
nem o status (status só muda pelo fluxo dedicado).

## 6. Persistência e soft delete

A persistência usa Spring Data JPA com Flyway. As migrations são escritas em SQL agnóstico, que
roda tanto em Postgres quanto em H2 no modo PostgreSQL, e o `ddl-auto` fica em `validate`: o
Flyway é dono do schema e o Hibernate apenas valida.

O soft delete é feito com `@SQLDelete` e `@SQLRestriction` (Hibernate 6). O `delete` vira um
update de `deleted_at` e todas as queries passam a filtrar os removidos automaticamente. Faz
sentido num domínio jurídico, onde se preserva o registro por retenção e auditoria.

Há uma nuance de unicidade que conecta com a Parte 2: a constraint única de `cnj_number` é
global e inclui os registros removidos. Isso está correto (um CNJ é único para sempre), mas cria
uma divergência interessante. A checagem na aplicação não enxerga os removidos, por causa do
`@SQLRestriction`, enquanto a constraint do banco enxerga. Essa lacuna entre "checagem na app" e
"garantia no banco" é justamente o tema do incidente analisado na Parte 2.

## 7. Concorrência e unicidade

A unicidade tem dupla guarda. O `existsByCnjNumber` resolve o caso comum com uma mensagem
amigável, e a constraint do banco somada ao `catch` cobre a condição de corrida. Nenhuma das
duas sozinha é suficiente.

No cadastro uso `saveAndFlush` em vez de `save`. Com `save`, a violação da constraint só
estouraria no commit, fora do `try`. O flush força a exceção a aparecer dentro do bloco, onde é
traduzida num `409` limpo. É exatamente o mecanismo descrito na Parte 2.

Além disso, o `cnjNumber` é imutável e o status só muda pela máquina de estados, nunca pelo
update genérico.

## 8. API e contrato

As rotas são versionadas em `/api/v1/legal-cases`. O cadastro responde `201 Created` com header
`Location` apontando para o recurso criado.

Na listagem devolvo `PagedModel` (de `spring-data-web`) em vez do `Page` cru, porque o Spring
desencoraja serializar `PageImpl` diretamente (o JSON é instável entre versões); o `PagedModel`
dá um contrato estável, com `content` mais os metadados de página.

A alteração de status fica num `PATCH /{id}/status` separado do `PUT /{id}` de detalhes, e o
soft delete responde `204`.

A documentação Swagger fica numa interface dedicada (`LegalCaseControllerDoc`), referenciada
pelo controller. O custo é a verbosidade (dois arquivos por controller), mas em troca o
controller fica limpo e o compilador garante a consistência via `@Override`.

## 9. Tratamento de erros

Um `@RestControllerAdvice` global devolve `ProblemDetail` (RFC 7807), com o `correlationId`
anexado a cada erro. O mapeamento de status:

- `404` para recurso não encontrado.
- `409` para CNJ duplicado, que é um conflito real de recurso.
- `422` para transição de status inválida: a requisição está bem-formada, mas viola uma regra de
  negócio sobre o estado atual. O `422` é uma escolha defensável e o `409` também seria; o que
  importa é ser consistente e saber justificar.
- `400` para falha de validação, acompanhada do mapa `errors` (campo para mensagem).

## 10. Logging e observabilidade

Cada requisição recebe um `correlationId` (filtro `OncePerRequestFilter` mais MDC), devolvido no
header `X-Correlation-Id` e incluído no corpo dos erros. Os eventos relevantes são registrados:
criação, transição de status, duplicidade concorrente e exceções. Logging em JSON estruturado
(encoder do Logstash) fica como melhoria futura; para o teste, o padrão com `correlationId` já
entrega o "log mínimo para diagnóstico" pedido no enunciado.

## 11. Design patterns

Usei o Specification (`JpaSpecificationExecutor`) porque há vários filtros opcionais e
combináveis. Sem ele haveria uma explosão de métodos no repository (`findByStatus`,
`findByStatusAndCourt`, e assim por diante); com ele, cada filtro vira uma peça componível.
A montagem segue a API moderna do Spring Data JPA 3.5+, com `Specification.allOf(...)` e
`Specification.unrestricted()` em vez de retornar `null` da lambda (que é desencorajado), e a
checagem de nulo ou vazio fica fora da lambda.

Alguns padrões foram evitados de propósito: o State pattern completo (over-engineering para cinco
estados), o MapStruct (um mapper manual é transparente para uma entidade só, e fica como
melhoria se o projeto crescer) e um CRUD genérico abstrato (abstração prematura).

## 12. Estratégia de testes

Os testes unitários cobrem o `CnjValidator`, a máquina de estados (`CaseStatus`) e o service com
Mockito. Os testes de fatia usam `@WebMvcTest` (controller, handler e validação) e `@DataJpaTest`
contra o schema real do Flyway, exercitando o soft delete e a constraint única.

Evitei APIs depreciadas: `@MockitoBean` no lugar de `@MockBean` (depreciado no 3.4),
`MockitoExtension` com `@Mock`/`@InjectMocks` nos unitários e `@AutoConfigureTestDatabase(replace = NONE)`
para rodar contra o schema real.

Uma ressalva honesta sobre o dado de teste do CNJ: usei um número que a própria aplicação
aceitou. É ótimo como teste de regressão, mas levemente circular, já que testa o validador
contra ele mesmo. Vale cruzar com um CNJ sabidamente válido de fonte externa para confirmar o
algoritmo, e não só a consistência interna.

A cobertura com JaCoCo pode ser plugada no `pom.xml` para gerar relatório no `./mvnw test`, como
melhoria opcional.

No front-end, o fluxo foi validado manualmente pela interface. Testes automatizados de front
(componentes e validadores) ficaram como próximo passo, descrito nas melhorias.

## 13. Trade-offs assumidos

- Sem autenticação e autorização, por estarem fora do escopo; o foco ficou no fluxo de negócio.
- Sem auditoria de autoria (`created_by`/`updated_by`), que depende de autenticação (ver
  melhorias).
- H2 só nos testes e Postgres em dev e prod, mantendo paridade com produção e testes rápidos e
  isolados.
- Monolito, adequado ao tamanho do problema; microsserviços seriam over-engineering aqui.
- Sem cache, porque o volume não justifica.
- Documentação Swagger em interface separada, aceitando a verbosidade em troca de um controller
  limpo.

## 14. Melhorias futuras

- Autenticação e autorização por perfil (procurador, assistente) e, a partir disso, auditoria de
  autoria com Spring Data JPA Auditing (`@CreatedBy`/`@LastModifiedBy` mais um
  `AuditorAware<String>` lendo o usuário do `SecurityContext`).
- Idempotência no cadastro (header `Idempotency-Key`) para tratar retries com segurança.
- Logging em JSON estruturado e métricas com alertas sobre os erros (Micrometer + Prometheus).
- Teste de concorrência real, com duas threads, reproduzindo a corrida do CNJ.
- Testes automatizados de front-end e cobertura com JaCoCo no CI; MapStruct, caso o número de
  mapeamentos cresça.
- Cadastro de prazos, audiências e movimentações; anexos; histórico completo de alterações.

## 15. Priorização

Dado o prazo, priorizei a robustez do back-end e dos testes: validações, máquina de estados,
unicidade sob concorrência, tratamento de erros padronizado e diagnóstico via `correlationId`,
com o front cobrindo o fluxo completo essencial. Preferi entregar menos coisas bem feitas a um
escopo maior e mais raso.
