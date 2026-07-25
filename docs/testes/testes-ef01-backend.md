# Testes do backend da EF-01 por TDD

## Objetivo

Lista ordenada dos testes necessários para implementar por TDD o backend da EF-01 — Autenticação e conta.
Cada item representa um comportamento observável e deve ser implementado em um ciclo vermelho-verde-refatora.

## Diretrizes

- Começar pelo teste indicado e escrever somente a produção necessária para fazê-lo passar.
- Usar relógio, gerador de identificadores, repositórios, hash de senha e envio de e-mail substituíveis nos testes
  unitários.
- Usar banco isolado nos testes de integração e limpar os dados entre casos.
- Não usar serviços externos reais; substituir o envio de e-mail por fake ou spy.
- Validar o contrato HTTP em testes de integração, incluindo status, corpo, cabeçalhos e cookies.
- Garantir que mensagens de erro sejam polidas e não revelem dados sensíveis.

## 1. Cadastro

| ID | Nível | Teste necessário |
|---|---|---|
| `BE-CAD-01` | Unitário | Cadastra nome, e-mail e hash da senha quando todos os dados são válidos. |
| `BE-CAD-02` | Unitário | Preserva a capitalização do nome cadastrado. |
| `BE-CAD-03` | Unitário | Normaliza o e-mail para minúsculas antes de consultar duplicidade e persistir. |
| `BE-CAD-04` | Unitário | Preserva todos os espaços da senha ao enviá-la ao gerador de hash. |
| `BE-CAD-05` | Unitário | Rejeita nome ausente, vazio, somente com espaços ou fora do intervalo de 2 a 100 caracteres. |
| `BE-CAD-06` | Unitário | Rejeita e-mail ausente, inválido ou com mais de 254 caracteres. |
| `BE-CAD-07` | Unitário | Rejeita senha ausente ou fora do intervalo de 8 a 128 caracteres. |
| `BE-CAD-08` | Unitário | Rejeita confirmação ausente ou diferente da senha, inclusive por caixa ou espaço. |
| `BE-CAD-09` | Unitário | Não consulta o hash nem persiste dados quando a entrada é inválida. |
| `BE-CAD-10` | Integração | `POST /api/v1/auth/registrations` retorna `400` e erros por campo para entrada inválida. |
| `BE-CAD-11` | Integração | E-mail já cadastrado, mesmo com outra caixa, retorna `409 CONFLICT` com o erro normativo. |
| `BE-CAD-12` | Integração | Uma disputa de cadastros com o mesmo e-mail cria somente uma conta. |
| `BE-CAD-13` | Integração | Falha durante o cadastro não deixa conta nem sessão parcialmente persistidas. |
| `BE-CAD-14` | Integração | Persiste somente hash forte e não reversível; nunca persiste a senha ou sua confirmação. |
| `BE-CAD-15` | Integração | Cadastro válido retorna `201`, `SessionResponse`, cookie seguro e `Cache-Control: no-store`. |
| `BE-CAD-16` | Integração | `SessionResponse.user` possui somente `id`, `name`, `email`, `status` e `createdAt`. |

## 2. Idempotência do cadastro

| ID | Nível | Teste necessário |
|---|---|---|
| `BE-IDC-01` | Integração | Ausência ou chave `Idempotency-Key` vazia ou maior que 255 caracteres é rejeitada. |
| `BE-IDC-02` | Integração | Repetir a mesma requisição com a mesma chave retorna o resultado original sem nova conta ou sessão. |
| `BE-IDC-03` | Integração | Reutilizar a mesma chave com conteúdo diferente é rejeitado com erro polido. |
| `BE-IDC-04` | Integração | Requisições com chaves diferentes são processadas independentemente. |

## 3. Login e bloqueio temporário

| ID | Nível | Teste necessário |
|---|---|---|
| `BE-LOG-01` | Unitário | Autentica conta ativa com e-mail sem distinção entre maiúsculas e minúsculas e senha correta. |
| `BE-LOG-02` | Unitário | Rejeita e-mail ausente, inválido ou com mais de 254 caracteres. |
| `BE-LOG-03` | Unitário | Rejeita senha ausente ou com menos de 8 caracteres. |
| `BE-LOG-04` | Unitário | Rejeita `manterConectado` ausente ou que não seja booleano. |
| `BE-LOG-05` | Integração | E-mail inexistente e senha incorreta retornam o mesmo `401` e a mesma mensagem genérica. |
| `BE-LOG-06` | Unitário | A verificação de senha também é executada para e-mail inexistente, reduzindo diferença temporal observável. |
| `BE-LOG-07` | Unitário | Cada falha de autenticação é contabilizada na janela móvel de 15 minutos. |
| `BE-LOG-08` | Unitário | Até a quarta tentativa malsucedida o login ainda não está bloqueado. |
| `BE-LOG-09` | Unitário | Após a quinta falha em 15 minutos, novas tentativas ficam bloqueadas por 15 minutos. |
| `BE-LOG-10` | Integração | Durante o bloqueio retorna `429`, `Retry-After` correto e a mensagem normativa. |
| `BE-LOG-11` | Unitário | Depois do fim do bloqueio uma credencial válida volta a autenticar. |
| `BE-LOG-12` | Unitário | Tentativas fora da janela não são somadas às tentativas atuais. |
| `BE-LOG-13` | Unitário | Login bem-sucedido limpa o histórico de falhas aplicável. |
| `BE-LOG-14` | Integração | Login válido retorna `200`, `SessionResponse`, cookie seguro e `Cache-Control: no-store`. |

## 4. Sessões

| ID | Nível | Teste necessário |
|---|---|---|
| `BE-SES-01` | Unitário | Login sem “manter conectado” cria sessão com 12 horas de inatividade e 24 horas de vida máxima. |
| `BE-SES-02` | Unitário | Atividade válida renova somente o prazo de inatividade sem ultrapassar as 24 horas máximas. |
| `BE-SES-03` | Unitário | Login com “manter conectado” cria sessão válida por no máximo 30 dias. |
| `BE-SES-04` | Unitário | Sessão expirada por inatividade, prazo máximo ou 30 dias é rejeitada. |
| `BE-SES-05` | Unitário | Identificador e token CSRF da sessão são imprevisíveis e armazenados de forma segura. |
| `BE-SES-06` | Integração | O cookie `cc_session` possui `HttpOnly`, `Secure`, `SameSite=Lax` e `Path=/api/v1`. |
| `BE-SES-07` | Integração | `GET /api/v1/auth/session` retorna `200`, dados da sessão válida e `Cache-Control: no-store`. |
| `BE-SES-08` | Integração | Consulta sem cookie, com cookie desconhecido ou sessão expirada retorna `401` polido. |
| `BE-SES-09` | Integração | Nenhuma resposta de sessão contém senha, hash, identificador interno de sessão ou outro segredo. |
| `BE-SES-10` | Unitário | A consulta pode renovar o token CSRF e invalida o anterior quando houver rotação. |

## 5. Logout e CSRF

| ID | Nível | Teste necessário |
|---|---|---|
| `BE-SAI-01` | Integração | `DELETE /api/v1/auth/sessions/current` invalida a sessão atual e retorna `204`. |
| `BE-SAI-02` | Integração | O logout expira `cc_session` com os mesmos atributos de segurança e `Max-Age=0`. |
| `BE-SAI-03` | Integração | Depois do logout, o cookie anterior não autentica novas requisições. |
| `BE-SAI-04` | Integração | Logout sem sessão válida retorna `401`. |
| `BE-SAI-05` | Integração | Logout sem token CSRF, com token incorreto ou pertencente a outra sessão retorna `403`. |
| `BE-SAI-06` | Integração | Logout com sessão e token CSRF correspondentes encerra somente a sessão atual. |

## 6. Solicitação de recuperação de senha

| ID | Nível | Teste necessário |
|---|---|---|
| `BE-REC-01` | Unitário | Rejeita e-mail ausente, inválido ou com mais de 254 caracteres. |
| `BE-REC-02` | Unitário | Normaliza o e-mail antes de localizar a conta. |
| `BE-REC-03` | Integração | E-mail existente e inexistente recebem o mesmo status `202` e corpo indistinguível. |
| `BE-REC-04` | Unitário | Para conta existente, cria token imprevisível, de uso único e válido por 30 minutos. |
| `BE-REC-05` | Unitário | Para e-mail inexistente, não cria token nem solicita envio de mensagem. |
| `BE-REC-06` | Unitário | Um novo pedido invalida todos os tokens anteriores ainda válidos da conta. |
| `BE-REC-07` | Integração | Persiste somente o hash do token de recuperação, nunca o token em texto puro. |
| `BE-REC-08` | Unitário | A mensagem enviada coloca o token no fragmento da URL, não na query string. |
| `BE-REC-09` | Integração | Falha no envio retorna `500` com a mensagem polida definida no contrato. |
| `BE-REC-10` | Integração | Repetir a mesma chave idempotente não cria nem envia um segundo token. |
| `BE-REC-11` | Integração | Chave idempotente ausente, inválida ou reutilizada com outro conteúdo é rejeitada. |

## 7. Redefinição de senha

| ID | Nível | Teste necessário |
|---|---|---|
| `BE-RED-01` | Unitário | Rejeita token ausente. |
| `BE-RED-02` | Unitário | Rejeita nova senha ausente ou fora do intervalo de 8 a 128 caracteres. |
| `BE-RED-03` | Unitário | Rejeita confirmação ausente ou diferente da nova senha, inclusive por caixa ou espaço. |
| `BE-RED-04` | Unitário | Token desconhecido, expirado, usado ou invalidado por novo pedido produz o mesmo erro. |
| `BE-RED-05` | Unitário | Token inválido não altera a senha, não cria sessão e não invalida acessos. |
| `BE-RED-06` | Unitário | Token válido altera o hash da senha e preserva os espaços da nova senha. |
| `BE-RED-07` | Unitário | Redefinição válida marca o token como usado e invalida todas as sessões da conta. |
| `BE-RED-08` | Integração | Reutilizar um token após o sucesso retorna `400` e não altera novamente a senha. |
| `BE-RED-09` | Integração | Senha antiga deixa de autenticar e somente a nova senha permite novo login. |
| `BE-RED-10` | Integração | Sessões abertas antes da redefinição passam a retornar `401`. |
| `BE-RED-11` | Integração | Redefinição válida retorna `204`, sem criar cookie ou autenticar automaticamente. |
| `BE-RED-12` | Integração | Alteração da senha, consumo do token e invalidação das sessões são atômicos. |
| `BE-RED-13` | Integração | Repetir a mesma chave idempotente após sucesso retorna o resultado sem novo processamento. |
| `BE-RED-14` | Integração | Chave idempotente ausente, inválida ou reutilizada com outro conteúdo é rejeitada. |

## 8. Comportamentos transversais

| ID | Nível | Teste necessário |
|---|---|---|
| `BE-CTR-01` | Integração | Todos os seis endpoints aceitam somente os métodos, tipos de conteúdo e schemas documentados. |
| `BE-CTR-02` | Integração | Campos obrigatórios ausentes, tipos incorretos e propriedades adicionais retornam `400`. |
| `BE-CTR-03` | Integração | Erros usam `application/problem+json`, `code` estável, `detail` polido e `fieldErrors` quando aplicável. |
| `BE-CTR-04` | Integração | Erros inesperados retornam mensagem genérica sem stack trace ou detalhes internos. |
| `BE-SEG-01` | Integração | Senhas, confirmações, cookies, tokens CSRF e tokens de recuperação não aparecem nos logs. |
| `BE-SEG-02` | Integração | Respostas de autenticação e recuperação não permitem inferir a existência de uma conta. |
| `BE-SEG-03` | Integração | Endpoints autenticados não aceitam sessão expirada, revogada ou pertencente a conta inativa. |
| `BE-SEG-04` | Integração | Respostas que contêm dados de sessão nunca são armazenáveis em cache. |

## 9. Validação ponta a ponta do backend

### Classificação dos níveis

Para evitar ambiguidade, os níveis deste documento possuem os seguintes significados:

- **Unitário**: exercita uma classe isolada e substitui todas as dependências por stubs, fakes ou spies.
- **Integração HTTP isolada**: exercita controller, serialização, validação, segurança e resposta HTTP, mas
  substitui serviços, repositórios ou infraestrutura que não sejam o objeto do teste.
- **Ponta a ponta do backend**: inicia o contexto real da aplicação e atravessa controller, service, repository,
  transação, migrations e PostgreSQL em Testcontainers. Somente integrações externas inevitáveis, como entrega de
  e-mail, são substituídas por fakes ou spies.

Os testes já classificados apenas como **Integração** devem ser entendidos como **Integração HTTP isolada**. Os
testes ponta a ponta usam o prefixo `BE-E2E` e não devem declarar implementações alternativas dos repositórios da
aplicação.

### Ambiente

- Usar PostgreSQL real iniciado por Testcontainers.
- Aplicar as migrations Flyway da aplicação, sem criar o schema manualmente no teste.
- Limpar os dados entre os casos sem substituir os repositórios de produção.
- Fazer requisições pela camada HTTP e validar resultados observáveis pela API sempre que possível.
- Consultar diretamente o banco somente quando o objetivo do teste for validar persistência, restrição, hash ou
  atomicidade que não seja observável pela API.
- Substituir apenas a entrega de e-mail por fake ou spy; relógio e gerador de identificadores podem ser
  controláveis quando o cenário exigir determinismo.

### Testes necessários

1. **`BE-E2E-01` — Inicialização e migrations:** a aplicação inicia com PostgreSQL vazio e todas as migrations
   Flyway são aplicadas com sucesso.
2. **`BE-E2E-02` — Persistência do cadastro:** cadastro válido atravessa a API e persiste uma única conta com nome
   preservado, e-mail normalizado e status ativo.
3. **`BE-E2E-03` — Proteção da senha:** a conta persiste somente o hash forte da senha; senha e confirmação nunca
   são armazenadas.
4. **`BE-E2E-04` — Unicidade concorrente:** a restrição do banco impede duas contas com o mesmo e-mail
   normalizado, inclusive sob concorrência.
5. **`BE-E2E-05` — Atomicidade do cadastro:** cadastro e criação da sessão são atômicos; uma falha após criar a
   conta não deixa conta nem sessão persistida.
6. **`BE-E2E-06` — Idempotência persistida:** repetir um cadastro com a mesma chave retorna o resultado original
   após nova requisição HTTP, sem duplicar conta ou sessão.
7. **`BE-E2E-07` — Login com conta persistida:** o login aceita a senha correta e rejeita a incorreta sem revelar
   se o e-mail existe.
8. **`BE-E2E-08` — Bloqueio de login persistido:** falhas e bloqueio temporário são respeitados por requisições
   independentes.
9. **`BE-E2E-09` — Persistência da sessão:** a sessão criada por cadastro ou login pode ser consultada em outra
   requisição e contém os prazos correspondentes à opção de permanência.
10. **`BE-E2E-10` — Revogação no logout:** logout com CSRF válido revoga no banco somente a sessão atual. O cookie
    anterior deixa de autenticar.
11. **`BE-E2E-11` — Token de recuperação persistido:** solicitação para conta existente persiste somente o hash do
    token, invalida tokens anteriores e solicita uma única entrega.
12. **`BE-E2E-12` — Recuperação indistinguível:** solicitação para e-mail inexistente mantém resposta
    indistinguível e não persiste token nem solicita entrega.
13. **`BE-E2E-13` — Redefinição atômica:** redefinição válida altera o hash da senha, consome o token e revoga
    todas as sessões da conta na mesma transação.
14. **`BE-E2E-14` — Rollback da redefinição:** uma falha reverte a alteração da senha, o consumo do token e a
    revogação das sessões.
15. **`BE-E2E-15` — Acesso após redefinição:** a senha antiga e as sessões anteriores falham, enquanto a nova
    senha permite criar outra sessão.
16. **`BE-E2E-16` — Restrições do schema:** tamanho, nulidade, unicidade e relacionamentos do schema são
    compatíveis com as validações e regras da aplicação.
17. **`BE-E2E-17` — Persistência entre contextos:** reiniciar o contexto preserva contas, sessões, idempotência e
    tokens ainda válidos no PostgreSQL.

## Ordem sugerida dos ciclos TDD

1. Cadastro e validações (`BE-CAD`).
2. Idempotência do cadastro (`BE-IDC`).
3. Login e bloqueio (`BE-LOG`).
4. Ciclo de vida das sessões (`BE-SES`).
5. Logout e CSRF (`BE-SAI`).
6. Solicitação de recuperação (`BE-REC`).
7. Redefinição de senha (`BE-RED`).
8. Comportamentos transversais (`BE-CTR` e `BE-SEG`).
9. Validação ponta a ponta do backend (`BE-E2E`), implementada incrementalmente junto da infraestrutura de cada
   fluxo, e não somente ao final.

## Rastreabilidade com os critérios de aceite

| Critério da EF-01 | Testes principais |
|---|---|
| Cadastro cria uma única conta e autentica | `BE-CAD-01`, `BE-CAD-12`, `BE-CAD-15` |
| Duplicidade por caixa e confirmação divergente não persistem | `BE-CAD-03`, `BE-CAD-08`, `BE-CAD-11` |
| Login não revela a existência da conta | `BE-LOG-05`, `BE-LOG-06` |
| Prazos da sessão e bloqueio são respeitados | `BE-LOG-07` a `BE-LOG-13`, `BE-SES-01` a `BE-SES-04` |
| Logout volta a exigir autenticação | `BE-SAI-01` a `BE-SAI-06` |
| Recuperação é indistinguível | `BE-REC-03`, `BE-REC-05` |
| Redefinição protege token e encerra acessos | `BE-RED-04` a `BE-RED-12` |
| Segredos não são expostos | `BE-CAD-14`, `BE-SES-09`, `BE-REC-07`, `BE-SEG-01` |

