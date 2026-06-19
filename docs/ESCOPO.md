# Escopo — Gestão de Processos Jurídicos

Documento que define **o que** foi construído na Parte 1 do teste, **por que** essa
funcionalidade foi escolhida e quais são os **limites** do escopo.

## 1. Funcionalidade escolhida

**Cadastro e gestão de processos jurídicos** (entidade `LegalCase` no código).

O teste deixa a funcionalidade em aberto ("implemente uma funcionalidade ponta a ponta
em um módulo do produto"). A definição do escopo, portanto, faz parte do desafio.
Optei por um módulo de processos jurídicos por três motivos:

1. **Aderência ao domínio.** Gestão de processos é o núcleo de uma procuradoria digital.
2. **Riqueza de regras de negócio.** Permite demonstrar validação de domínio real
   (número CNJ) e regra de negócio além de validação de campo (transições de status).
3. **Escopo controlável.** Uma única entidade bem-modelada cobre todos os itens exigidos
   (front com fluxo completo, API documentada, persistência, logs e testes) sem inflar o projeto.

## 2. Requisitos funcionais

Como usuário da procuradoria, eu quero:

- **RF01** — Cadastrar um novo processo informando seus dados principais.
- **RF02** — Editar os dados de um processo existente.
- **RF03** — Consultar os detalhes de um processo.
- **RF04** — Listar processos, com filtros e paginação.
- **RF05** — Alterar o status de um processo, respeitando as transições permitidas.
- **RF06** — Excluir um processo via **exclusão lógica (soft delete)** — processos jurídicos
  carregam expectativa de retenção/auditoria, então o registro é marcado como removido, não
  apagado fisicamente.

## 3. Modelo de domínio

Entidade principal: **`LegalCase`**. Os nomes refletem o código (em inglês).

| Campo              | Tipo          | Regras                                                          |
|--------------------|---------------|-----------------------------------------------------------------|
| `id`               | identificador | Gerado pelo sistema                                             |
| `cnjNumber`        | texto         | Obrigatório, único, formato CNJ válido (com dígito verificador) |
| `plaintiff`        | texto         | Obrigatório (parte autora)                                      |
| `defendant`        | texto         | Obrigatório (parte ré)                                          |
| `court`            | texto         | Obrigatório (vara)                                              |
| `judicialDistrict` | texto         | Opcional (comarca)                                              |
| `claimValue`       | decimal       | Opcional, `>= 0` (valor da causa)                               |
| `filingDate`       | data          | Opcional, não pode ser futura (data de distribuição)            |
| `status`           | enum          | Controlado por máquina de estados (ver seção 4)                 |
| `createdAt`        | timestamp     | Gerado pelo sistema                                             |
| `updatedAt`        | timestamp     | Gerado pelo sistema                                             |
| `deletedAt`        | timestamp     | Nulo = ativo; preenchido = removido logicamente                 |

## 4. Regras de negócio

- **RN01 — Validação do número CNJ.** O número deve seguir o formato
  `NNNNNNN-DD.AAAA.J.TR.OOOO` e ter dígito verificador válido (ISO 7064, MOD 97-10).
- **RN02 — Unicidade do número CNJ.** Não pode haver dois processos com o mesmo número
  (validado na aplicação **e** com constraint única no banco). A unicidade é **global**,
  inclusive sobre registros removidos logicamente — um CNJ é único para sempre.
- **RN03 — Máquina de estados de status.** Transições permitidas:
  - `FILED → IN_PROGRESS`
  - `IN_PROGRESS → SUSPENDED`
  - `SUSPENDED → IN_PROGRESS`
  - `IN_PROGRESS → ARCHIVED`
  - `IN_PROGRESS → CLOSED`

  `ARCHIVED` e `CLOSED` são estados terminais. Qualquer transição fora das permitidas
  (incluindo auto-transição) é rejeitada com `422`.

## 5. Requisitos não-funcionais

- **RNF01** — API documentada (OpenAPI/Swagger).
- **RNF02** — Validações no front-end e no back-end.
- **RNF03** — Logs com `correlationId` por requisição para diagnóstico.
- **RNF04** — Testes (unitários e de integração) cobrindo os cenários principais.
- **RNF05** — Projeto executável com poucos comandos (Docker Compose) e README claro.

## 6. Fora de escopo

Decisões conscientes para manter o foco e o tempo (detalhadas na nota técnica):

- Autenticação e autorização.
- Auditoria de **autoria** (`created_by`/`updated_by`) — depende de autenticação; o "quando"
  já existe (`createdAt`/`updatedAt`), só o "quem" ficou de fora.
- Multi-tenant / multiusuário.
- Anexos / upload de documentos do processo.
- Cadastro de prazos, audiências e movimentações.
- Integrações externas (tribunais, e-mail, notificações).

## 7. Critérios de aceite

- Cadastro válido persiste e retorna `201` com header `Location`; o processo nasce `FILED`.
- Cadastro com CNJ inválido (formato ou dígito) retorna `400` com `errors.cnjNumber`.
- Cadastro com CNJ duplicado retorna `409`.
- Transição de status inválida retorna `422`.
- Recurso inexistente retorna `404`.
- Listagem retorna resultados paginados e filtráveis; removidos não aparecem.
- Exclusão retorna `204` e o recurso some das consultas (soft delete).
- Endpoints documentados e acessíveis via Swagger.
- Testes principais passando.

## 8. Mapa para as duas partes do teste

| Parte | Entrega                                                                 |
|-------|-------------------------------------------------------------------------|
| 1     | Módulo de processos (front + API + banco + logs + testes)               |
| 2     | Análise de um incidente recorrente usando os logs gerados pelo módulo   |

A Parte 2 reaproveita a instrumentação de logs da Parte 1: o incidente analisado é um cenário
realista do próprio módulo (ver [`ANALISE-INCIDENTE.md`](ANALISE-INCIDENTE.md)).
