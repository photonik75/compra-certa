# EF-01 — Autenticação e conta

## Resultado esperado

Permitir que uma pessoa crie sua conta, entre e saia com segurança e recupere o acesso por e-mail. Após autenticar-se, ela deve chegar à área de listas.

## Atores

- Visitante: pessoa sem sessão válida.
- Usuário autenticado: pessoa com conta ativa e sessão válida.

## Dados funcionais

### Usuário

| Campo | Regra |
|---|---|
| `id` | Identificador imutável |
| `name` | Obrigatório, 2–100 caracteres |
| `email` | Obrigatório, formato válido, máximo 254 caracteres e único globalmente |
| `passwordHash` | Nunca armazenar ou registrar senha em texto puro |
| `status` | `ACTIVE` na versão 1 |
| `termsAcceptedAt` | Obrigatório no cadastro |
| `createdAt`, `updatedAt` | Conforme convenções globais |

O e-mail é convertido para minúsculas para autenticação e unicidade. O nome mantém a capitalização informada.

## Fluxos

### 1. Criar conta

Campos: nome, e-mail, senha, confirmação da senha e aceite dos termos.

Regras:

1. Todos os campos são obrigatórios.
2. A senha deve ter entre 8 e 128 caracteres. Espaços são permitidos e não são removidos.
3. Senha e confirmação devem ser idênticas.
4. O aceite dos termos deve estar marcado; salvar data/hora e versão vigente dos termos.
5. E-mail já cadastrado retorna `EMAIL_ALREADY_IN_USE`, sem criar outra conta.
6. O cadastro cria, na mesma transação lógica, as categorias iniciais da EF-03.
7. Quando o cadastro for iniciado por um convite válido, esse convite é associado conforme EF-08. Cadastro iniciado fora do link não concede acesso automaticamente.
8. Em sucesso, criar sessão, mostrar confirmação e redirecionar para “Minhas listas”.

### 2. Entrar

Campos: e-mail, senha e opção “Manter-me conectado”.

Regras:

1. Credenciais inválidas retornam a mensagem única “E-mail ou senha inválidos”.
2. Sem “Manter-me conectado”, a sessão expira após 12 horas de inatividade e no máximo em 24 horas.
3. Com a opção marcada, a sessão pode ser renovada por até 30 dias.
4. Uma autenticação bem-sucedida deve renovar o identificador de sessão.
5. Após sucesso, redirecionar para a rota originalmente solicitada; na ausência dela, para “Minhas listas”.
6. Aplicar limitação de tentativas por conta e origem. Após 5 falhas em 15 minutos, bloquear novas tentativas por 15 minutos e retornar `RATE_LIMITED`.

### 3. Mostrar ou ocultar senha

O controle altera apenas a apresentação local, preserva o valor e informa seu estado acessível (“Mostrar senha”/“Ocultar senha”).

### 4. Solicitar recuperação

1. Receber um e-mail válido.
2. Sempre mostrar a mesma confirmação, exista ou não uma conta: “Se houver uma conta para este e-mail, enviaremos as instruções”.
3. Para conta existente, criar token aleatório de uso único, armazenado de forma não reversível, com validade de 30 minutos.
4. Invalidar tokens anteriores ainda válidos para o usuário e enviar link de redefinição.
5. Limitar solicitações repetidas sem revelar o motivo ao visitante.

### 5. Redefinir senha

1. Exigir token válido, nova senha e confirmação.
2. Aplicar as mesmas regras de senha do cadastro.
3. Em sucesso, invalidar o token e todas as sessões existentes do usuário.
4. Redirecionar ao login com confirmação de senha alterada.
5. Token inválido, expirado ou usado apresenta link para nova solicitação e não altera a senha.

### 6. Sair

Invalidar a sessão atual no servidor, limpar credenciais locais e redirecionar ao login. Usar o botão voltar do navegador não pode reabrir dados protegidos sem nova validação.

## Estados de interface

- Formulário inicial, campos inválidos, envio em andamento, sucesso e erro geral.
- Rotas internas acessadas sem sessão redirecionam ao login guardando a rota de retorno.
- Usuário autenticado que acessa login ou cadastro é redirecionado para “Minhas listas”.

## Critérios de aceite

1. Dado um cadastro válido e e-mail novo, quando o usuário confirmar, então conta, categorias iniciais e sessão são criadas uma única vez.
2. Dado e-mail já usado com diferença apenas de caixa ou acento equivalente, o cadastro é recusado.
3. Dada confirmação de senha diferente, nenhum dado é persistido e o erro aparece no campo correspondente.
4. Dadas credenciais válidas, o login abre a área autenticada e a sessão sobrevive a uma atualização da página.
5. Dadas credenciais inválidas, a resposta não informa qual campo está incorreto.
6. Dado logout, a sessão deixa de acessar imediatamente qualquer rota protegida.
7. Dado pedido de recuperação para e-mail inexistente, a resposta visual é indistinguível daquela de e-mail existente.
8. Dado token de recuperação usado ou expirado, a senha permanece inalterada.
9. Dada redefinição válida, sessões antigas e o token deixam de funcionar.
10. Senhas e tokens não aparecem em logs, URLs após consumo, telemetria ou mensagens de erro.

## Contrato de API (futura OpenAPI)

### Endpoints

| Método e rota | Autenticação | Request | Sucesso |
|---|---|---|---|
| `GET /api/v1/terms/current` | Pública | — | `200 TermsSummary` |
| `POST /api/v1/auth/registrations` | Pública | `RegistrationRequest` + `Idempotency-Key` | `201 SessionResponse` + cookie |
| `POST /api/v1/auth/sessions` | Pública | `LoginRequest` | `200 SessionResponse` + cookie |
| `GET /api/v1/auth/session` | Sessão | — | `200 SessionResponse` |
| `DELETE /api/v1/auth/sessions/current` | Sessão + CSRF | — | `204` e cookie expirado |
| `POST /api/v1/auth/password-reset-requests` | Pública | `PasswordResetRequest` + `Idempotency-Key` | `202` |
| `POST /api/v1/auth/password-resets` | Pública | `PasswordResetConfirmation` + `Idempotency-Key` | `204` e sessões revogadas |

### Schemas

#### `TermsSummary`

| Campo | Tipo | Obrigatório | Regra |
|---|---|---:|---|
| `version` | string | Sim | Versão imutável aceita no cadastro |
| `url` | string/uri | Sim | Documento legível pelo usuário |
| `effectiveAt` | string/date-time | Sim | Início de vigência |

#### `RegistrationRequest`

```json
{
  "name": "Larissa Barros",
  "email": "larissa@example.com",
  "password": "senha-segura",
  "passwordConfirmation": "senha-segura",
  "termsVersion": "2026-07-01",
  "invitationToken": null
}
```

Nome, e-mail, senha, confirmação e versão dos termos são obrigatórios. `invitationToken` é opcional e usado apenas quando o cadastro veio da EF-08. Nesse caso, token, e-mail e lista são validados antes da transação; conta, categorias, sessão, vínculo e aceite são criados atomicamente. Falha do convite não cria a conta. Erros por campo usam `VALIDATION_ERROR`; e-mail existente retorna `409 EMAIL_ALREADY_IN_USE`; versão de termos inválida retorna `409 TERMS_VERSION_OUTDATED` com `meta.currentVersion`.

Para `invitationToken`, token inválido retorna `400 INVALID_TOKEN`, expirado retorna `410 INVITATION_EXPIRED`, e lista concluída retorna `409 LIST_COMPLETED`. Nenhum desses erros consome o token.

#### `LoginRequest`

```json
{ "email": "larissa@example.com", "password": "senha-segura", "rememberMe": false }
```

Credenciais incorretas retornam `401 INVALID_CREDENTIALS`; bloqueio temporário retorna `429 RATE_LIMITED` e header `Retry-After` em segundos.

#### `SessionResponse`

```json
{
  "user": {
    "id": "0b5a3425-38f8-4f8e-9600-df2a661f7fea",
    "name": "Larissa Barros",
    "email": "larissa@example.com",
    "status": "ACTIVE",
    "createdAt": "2026-07-18T14:30:00Z"
  },
  "csrfToken": "opaque-token",
  "expiresAt": "2026-07-19T14:30:00Z",
  "acceptedInvitation": null
}
```

`acceptedInvitation` é `null` fora do cadastro por convite ou `{ "listId": "..." }` quando o aceite ocorreu atomicamente. `GET /auth/session` sempre retorna esse campo como `null` e pode renovar `csrfToken`. Nunca retorna hash, senha, token de recuperação, token de convite ou identificador interno da sessão.

#### Recuperação de senha

- `PasswordResetRequest`: `{ "email": string/email }`.
- `PasswordResetConfirmation`: `{ "token": string, "newPassword": string, "passwordConfirmation": string }`.
- `POST /password-reset-requests` retorna sempre `202` com corpo vazio para e-mail sintaticamente válido, exista ou não conta.
- O e-mail coloca o token no fragmento da URL da aplicação; o frontend o envia somente no corpo HTTPS de `/password-resets`.
- Token inválido, usado ou expirado retorna `400 INVALID_TOKEN`; confirmação divergente ou senha fora da política retorna `400 VALIDATION_ERROR`.

### Cookies e cache

- Responses de sessão enviam `Set-Cookie: cc_session=...; HttpOnly; Secure; SameSite=Lax; Path=/api/v1`.
- Logout expira o cookie com o mesmo path e atributos.
- Endpoints de autenticação e qualquer response com `csrfToken` usam `Cache-Control: no-store`.

## Definições de testes funcionais (Playwright)

### AUTH-001 — Cadastro válido cria conta completa (`P0`)

- **Preparação:** visitante e e-mail ainda não utilizado.
- **Ação:** preencher todos os campos válidos, aceitar os termos e enviar.
- **Resultado:** abrir “Minhas listas” autenticado, exibir o nome informado e disponibilizar exatamente as quatro categorias iniciais. Recarregar mantém a sessão e não duplica conta ou categorias.

### AUTH-002 — Validações impedem cadastro inválido (`P0`)

- **Preparação:** formulário de cadastro aberto.
- **Ação:** exercitar, separadamente, campo obrigatório vazio, e-mail inválido, senha com 7 caracteres, senha acima de 128, confirmação divergente e termos não aceitos.
- **Resultado:** destacar o campo correspondente, manter valores seguros preenchidos e não criar conta nem sessão.

### AUTH-003 — E-mail não pode ser reutilizado (`P0`)

- **Preparação:** conta existente com `Pessoa@Exemplo.com`.
- **Ação:** cadastrar `pessoa@exemplo.com`.
- **Resultado:** apresentar `EMAIL_ALREADY_IN_USE`, permanecer no cadastro e não criar segunda conta.

### AUTH-004 — Login, rota de retorno e logout (`P0`)

- **Preparação:** conta ativa; visitante abre diretamente uma rota interna.
- **Ação:** autenticar com credenciais válidas, depois sair e tentar voltar pelo navegador.
- **Resultado:** retornar à rota originalmente solicitada; após logout, toda rota interna volta a exigir login e o cache não revela dados protegidos.

### AUTH-005 — Credencial inválida não revela o campo incorreto (`P0`)

- **Preparação:** conta ativa.
- **Ação:** tentar uma vez com e-mail existente/senha errada e outra com e-mail inexistente.
- **Resultado:** as duas tentativas apresentam a mesma mensagem e comportamento, sem indicar se a conta existe.

### AUTH-006 — Limitação de tentativas de login (`P1`)

- **Preparação:** conta ativa e relógio controlado.
- **Ação:** realizar cinco falhas em 15 minutos, tentar novamente antes e depois de avançar 15 minutos.
- **Resultado:** a tentativa durante o bloqueio retorna `RATE_LIMITED`; após a janela, credenciais válidas funcionam.

### AUTH-007 — Duração da sessão respeita “Manter-me conectado” (`P1`)

- **Preparação:** conta ativa e relógio controlado.
- **Ação:** criar sessões com e sem a opção, avançando o relógio pelos limites especificados.
- **Resultado:** a sessão comum expira conforme inatividade/limite absoluto e a persistente continua válida somente até 30 dias.

### AUTH-008 — Recuperação não permite enumerar contas (`P0`)

- **Preparação:** um e-mail cadastrado e outro inexistente; caixa de correio de teste vazia.
- **Ação:** solicitar recuperação para ambos.
- **Resultado:** interface e resposta observável são equivalentes; somente o e-mail cadastrado recebe mensagem com link válido e sem senha exposta.

### AUTH-009 — Redefinição válida revoga token e sessões (`P0`)

- **Preparação:** usuário autenticado em outro contexto e token de recuperação válido.
- **Ação:** definir nova senha e tentar reutilizar o token, usar a sessão antiga e entrar com senhas antiga e nova.
- **Resultado:** token e sessão antiga falham, senha antiga falha e nova senha autentica.

### AUTH-010 — Token inválido ou expirado não altera senha (`P0`)

- **Preparação:** token expirado por relógio controlado e URL com token inventado.
- **Ação:** tentar redefinir em ambos os casos.
- **Resultado:** mostrar caminho para nova solicitação, não alterar senha e não criar sessão.

### AUTH-011 — Controles de senha e formulários são acessíveis (`P2`)

- **Preparação:** páginas de login e cadastro em projetos desktop, mobile e navegadores de compatibilidade.
- **Ação:** percorrer por teclado, alternar visibilidade da senha e enviar formulário inválido.
- **Resultado:** foco segue ordem lógica, rótulos e estados são anunciáveis, alternância preserva o valor e o erro recebe foco ou anúncio sem depender de cor.

## Fora do escopo específico

Verificação obrigatória de e-mail, alteração de perfil, troca de e-mail e exclusão de conta.
