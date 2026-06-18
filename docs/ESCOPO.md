# Escopo — Gestão de Processos Jurídicos

Documento que define **o que** será construído na Parte 1 do teste, **por que** essa
funcionalidade foi escolhida e quais são os **limites** do escopo.

## 1. Funcionalidade escolhida

**Cadastro e gestão de processos jurídicos.**

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
- **RF04** — Listar processos, com busca e paginação.
- **RF05** — Alterar o status de um processo, respeitando as transições permitidas.
- **RF06** — Remover um processo. <!-- TODO: confirmar se entra no escopo ou só soft delete -->

## 3. Modelo de domínio

Entidade principal: **Processo**.

| Campo            | Tipo        | Regras                                                    |
|------------------|-------------|-----------------------------------------------------------|
| `id`             | identificador | Gerado pelo sistema                                     |
| `numeroCnj`      | texto       | Obrigatório, único, formato CNJ válido (com dígito verificador) |
| `parteAutora`    | texto       | Obrigatório                                               |
| `parteRe`        | texto       | Obrigatório                                               |
| `vara`           | texto       | Obrigatório <!-- TODO: ou opcional? -->                   |
| `comarca`        | texto       | Opcional                                                  |
| `valorCausa`     | decimal     | Opcional, >= 0                                            |
| `dataDistribuicao` | data      | Opcional <!-- TODO: definir regra -->                     |
| `status`         | enum        | Controlado por máquina de estados (ver seção 4)           |
| `criadoEm`       | timestamp   | Gerado pelo sistema                                       |
| `atualizadoEm`   | timestamp   | Gerado pelo sistema                                       |

<!-- TODO: ajustar campos conforme decidir o nível de fidelidade ao domínio -->

## 4. Regras de negócio

- **RN01 — Validação do número CNJ.** O número deve seguir o formato
  `NNNNNNN-DD.AAAA.J.TR.OOOO` e ter dígito verificador válido.
  <!-- TODO: confirmar o algoritmo exato do dígito verificador na implementação -->
- **RN02 — Unicidade do número CNJ.** Não pode haver dois processos com o mesmo número
  (validado na aplicação **e** com constraint única no banco).
- **RN03 — Máquina de estados de status.** Transições permitidas:
  - `DISTRIBUIDO → EM_ANDAMENTO`
  - `EM_ANDAMENTO → SUSPENSO`
  - `SUSPENSO → EM_ANDAMENTO`
  - `EM_ANDAMENTO → ARQUIVADO`
  - `EM_ANDAMENTO → BAIXADO`

  Qualquer transição fora dessas deve ser rejeitada com erro apropriado (`409`/`422`).
  <!-- TODO: revisar os estados e transições conforme seu entendimento do domínio -->

## 5. Requisitos não-funcionais

- **RNF01** — API documentada (OpenAPI/Swagger).
- **RNF02** — Validações no front-end e no back-end.
- **RNF03** — Logs estruturados com `correlationId` para diagnóstico.
- **RNF04** — Testes (unitários e/ou de integração) cobrindo os cenários principais.
- **RNF05** — Projeto executável com poucos comandos (Docker Compose) e README claro.

## 6. Fora de escopo

Decisões conscientes para manter o foco e o tempo (detalhadas na nota técnica):

- Autenticação e autorização.
- Multi-tenant / multiusuário.
- Anexos / upload de documentos do processo.
- Cadastro de prazos, audiências e movimentações.
- Integrações externas (tribunais, e-mail, notificações).

## 7. Critérios de aceite

- Cadastro de processo válido persiste e retorna `201`.
- Cadastro com CNJ inválido retorna erro de validação (`400`/`422`) com mensagem clara.
- Cadastro com CNJ duplicado retorna `409`.
- Transição de status inválida é rejeitada.
- Listagem retorna resultados paginados.
- Front-end executa o fluxo completo (listar → criar → editar) com validações.
- Endpoints documentados e acessíveis via Swagger.
- Testes principais passando.

## 8. Mapa para as duas partes do teste

| Parte | Entrega                                                                 |
|-------|-------------------------------------------------------------------------|
| 1     | Módulo de processos (front + API + banco + logs + testes)               |
| 2     | Análise de um incidente recorrente usando os logs gerados pelo módulo   |

A Parte 2 reaproveita a instrumentação de logs construída na Parte 1: o incidente analisado
é um cenário realista do próprio módulo (ver [`ANALISE-INCIDENTE.md`](ANALISE-INCIDENTE.md)).
