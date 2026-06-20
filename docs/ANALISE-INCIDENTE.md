# Análise de Incidente — Cadastro duplicado de processos (CNJ) sob concorrência

> Documento referente à **Parte 2** do desafio. Como nenhum arquivo de log foi fornecido,
> este post-mortem analisa um incidente **realista e representativo** do fluxo de cadastro
> do serviço. As amostras de log estão no formato exato que a aplicação produz, incluindo o
> `correlationId` por requisição.

## 1. Resumo executivo

Em determinadas situações, dois processos jurídicos com o **mesmo número CNJ** eram
persistidos no sistema, violando a regra de unicidade (RN02). O problema era **intermitente**
e **recorrente**, concentrado em momentos de uso simultâneo.

A causa-raiz é uma **condição de corrida do tipo *check-then-act* (TOCTOU)**: a verificação de
duplicidade era feita apenas na aplicação, sem uma garantia atômica no banco. Sob concorrência,
duas requisições passavam pela verificação antes de qualquer uma persistir, e ambas concluíam o
cadastro.

A correção combina uma **constraint única no banco** (garantia autoritativa) com o
**tratamento gracioso** da violação resultante (resposta `409 Conflict` em vez de erro genérico),
mantendo a verificação na aplicação apenas como caminho rápido. A prevenção adota defesa em
profundidade: constraint, teste de concorrência, idempotência para *retries*, observabilidade e
uma trava no front-end.

## 2. Contexto

O endpoint `POST /api/v1/legal-cases` cadastra um processo. O número CNJ deve ser **único**
(RN02). O fluxo original validava essa unicidade da seguinte forma:

1. Consultar se já existe processo com aquele CNJ (`existsByCnjNumber`).
2. Se não existir, montar a entidade e persistir.

À primeira vista, correto. O problema aparece sob concorrência.

## 3. Sintoma

- Operadores relatam, de forma esporádica, **dois processos idênticos** (mesmo CNJ, ids
  diferentes) aparecendo na listagem.
- Sempre associado a **uso simultâneo**: duplo clique no botão de salvar, *retry* após lentidão
  de rede, ou dois operadores cadastrando o mesmo processo quase ao mesmo tempo.
- **Recorrente**: os chamados de suporte se repetem, sempre com o mesmo padrão.

Não havia *stack trace* nem erro 500 associado — os dois cadastros "davam certo". Esse é o
aspecto mais perigoso do incidente: **falha silenciosa de integridade de dados**.

## 4. Evidência nos logs

Filtrando os logs pelo CNJ relatado, encontram-se **dois cadastros bem-sucedidos**, com
`correlationId` distintos e poucos milissegundos de diferença:

```
2026-06-19 15:17:39.311 INFO  [2eebbcab-a9e0-4a65-8502-59bc391fcff6] c.a.l.service.LegalCaseService - Legal case created id=6 cnj=9068906-21.2026.4.02.3738
2026-06-19 15:17:39.319 INFO  [7b3d1f08-2c4a-4e9b-9a01-d5e6f7a8b9c0] c.a.l.service.LegalCaseService - Legal case created id=7 cnj=9068906-21.2026.4.02.3738
```

Leitura das evidências:

- **Mesmo CNJ** (`9068906-21.2026.4.02.3738`), **ids diferentes** (`6` e `7`).
- **`correlationId` diferentes** → são **duas requisições distintas**, processadas em paralelo.
- **8 ms de diferença** → praticamente simultâneas.
- Ambas em nível `INFO`, sem exceção → as duas transações **commitaram com sucesso**.

O `correlationId` é o que torna o diagnóstico possível: ele permite isolar e correlacionar cada
requisição de ponta a ponta.

## 5. Diagnóstico

As duas requisições executaram o mesmo fluxo `verificar → inserir` em janelas de tempo
sobrepostas. Como nenhuma exceção foi lançada, descarta-se falha de infraestrutura: trata-se de
uma duplicação **lógica**, originada na forma como a unicidade era garantida.

## 6. Causa-raiz

O fluxo `existsByCnjNumber` seguido de `insert` **não é atômico**. Ele tem o clássico problema
*check-then-act* (TOCTOU — *time-of-check to time-of-use*):

1. Transação **A** executa `existsByCnjNumber("...3738")` → `false`.
2. Transação **B** executa `existsByCnjNumber("...3738")` → também `false` (o insert de A ainda
   não commitou e, sob isolamento **READ COMMITTED** — padrão do PostgreSQL —, não é visível a B).
3. A insere e commita. B insere e commita.
4. Sem uma restrição de unicidade no banco, **as duas inserções são aceitas** → duplicidade.

O ponto central: **a verificação na aplicação é necessária, mas não é suficiente**. Ela resolve o
caso comum (cadastro duplicado sequencial, com mensagem amigável), mas não oferece nenhuma
garantia sob concorrência. A única forma de garantir uma invariante de unicidade de modo
confiável é delegá-la a quem é a fonte da verdade: **o banco de dados**.

Vale registrar por que **aumentar o nível de isolamento** (ex.: `SERIALIZABLE`) **não** é a
correção adequada aqui: resolveria o sintoma, mas ao custo de mais contenção e *retries* de
serialização em todo o fluxo, para proteger uma única invariante que uma constraint resolve de
forma mais simples, barata e definitiva.

## 7. Correção

A correção é em camadas:

1. **Constraint única no banco (garantia autoritativa).** A coluna `cnj_number` passa a ter uma
   restrição `UNIQUE` (`uk_legal_case_cnj_number`, na migration `V1`). A partir daí, a segunda
   inserção concorrente **falha** em vez de duplicar.

2. **Tornar a falha síncrona e traduzi-la.** No `LegalCaseService.create`, o `saveAndFlush`
   força a violação a aparecer **dentro** do `try` (com `save` simples, ela só estouraria no
   commit, fora do tratamento). A `DataIntegrityViolationException` é capturada e convertida em
   uma exceção de domínio, devolvida como **`409 Conflict`** — resposta clara para o cliente, em
   vez de um `500` genérico:

   ```java
   try {
       LegalCase saved = repository.saveAndFlush(entity);
       log.info("Legal case created id={} cnj={}", saved.getId(), saved.getCnjNumber());
       return mapper.toResponse(saved);
   } catch (DataIntegrityViolationException ex) {
       log.warn("Concurrent duplicate CNJ detected cnj={}", request.cnjNumber());
       throw new DuplicateCnjException(request.cnjNumber(), ex);
   }
   ```

3. **Manter a verificação na aplicação como caminho rápido.** O `existsByCnjNumber` continua
   resolvendo o caso comum (não concorrente) com uma mensagem amigável e sem tentativa de insert
   desperdiçada. Ele é uma otimização, não a garantia.

## 8. Validação da correção

O cenário é reproduzido de forma determinística no teste de repositório
`uniqueConstraint_blocksDuplicateEvenAfterSoftDelete`, que demonstra que a constraint barra a
duplicidade mesmo quando a checagem da aplicação não a enxerga.

Após a correção, a **mesma corrida** passa a produzir um resultado controlado e observável: o
primeiro cadastro conclui, o segundo é rejeitado com `409` e registra um log de aviso —
em vez de uma duplicação silenciosa:

```
2026-06-19 15:17:39.311 INFO  [2eebbcab-a9e0-4a65-8502-59bc391fcff6] c.a.l.service.LegalCaseService - Legal case created id=6 cnj=9068906-21.2026.4.02.3738
2026-06-19 15:17:39.319 WARN  [7b3d1f08-2c4a-4e9b-9a01-d5e6f7a8b9c0] c.a.l.service.LegalCaseService - Concurrent duplicate CNJ detected cnj=9068906-21.2026.4.02.3738
```

## 9. Medidas de prevenção (defesa em profundidade)

- **Constraint de unicidade no banco** — a garantia que faltava; nenhuma invariante crítica deve
  depender apenas de validação na aplicação.
- **Tradução de exceção** — a violação vira um `409` semântico, nunca um `500` opaco.
- **Teste de integração de concorrência** — além do teste atual da constraint, vale um teste que
  dispare duas criações em paralelo (ex.: `ExecutorService` com duas threads) e assegure que
  exatamente uma conclui e a outra recebe `409`.
- **Idempotência para *retries*** — *retries* legítimos (timeout de rede seguido de reenvio) são
  uma causa comum. Uma chave de idempotência (header `Idempotency-Key`) faria o reenvio retornar
  o recurso já criado, em vez de tentar criar de novo.
- **Observabilidade e alerta** — emitir uma métrica (ex.: contador Micrometer) no aviso
  *"Concurrent duplicate CNJ detected"* e alertar em caso de **pico**. Um aumento súbito sinaliza
  ou um bug de duplo envio no front, ou uma integração reenviando requisições.
- **Trava no front-end** — desabilitar o botão de salvar após o clique (e/ou *debounce*) elimina
  a causa mais frequente do incidente, o duplo clique. É uma camada complementar, não substituta
  das anteriores.

## 10. Lições aprendidas

- **O banco é a fonte da verdade para invariantes.** Validações na aplicação são otimizações e
  melhoram a UX, mas não substituem as garantias do banco sob concorrência.
- **Cuidado com *check-then-act*.** Sempre que houver "verificar e então agir" sem atomicidade,
  há uma corrida latente. O padrão deve ser projetado para falhar de forma segura e tratada.
- **`correlationId` é indispensável.** Sem ele, correlacionar duas requisições concorrentes nos
  logs seria muito mais difícil. Ele transformou um incidente "fantasma" em algo diagnosticável.
- **Defesa em profundidade.** Nenhuma camada sozinha resolve: banco (garantia) + aplicação
  (UX e caminho rápido) + front-end (prevenção da causa comum) + observabilidade (detecção).
