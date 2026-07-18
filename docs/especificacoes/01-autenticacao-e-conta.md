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

## Fora do escopo específico

Verificação obrigatória de e-mail, alteração de perfil, troca de e-mail e exclusão de conta.
