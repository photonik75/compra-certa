## Diretrizes de implementação

- Testes unitários de componentes substituem o serviço injetável da funcionalidade por stub, fake ou spy. Eles verificam interação, estado visual e navegação sem conhecer URL, status ou formato HTTP.
- Testes unitários dos serviços verificam URL, método, corpo, status e tradução do contrato HTTP com `HttpTestingController`.
- Testes de guards e rotas substituem o serviço de sessão por stub e usam `RouterTestingHarness`.
- Testes de integração dos interceptadores mock exercitam o serviço e a cadeia HTTP, confirmando a resposta
  simulada e o encaminhamento de requisições fora do escopo.
- Testes de integração com Playwright ficam ao fim da tabela e cobrem somente comportamentos que dependem de recarga ou do histórico real do navegador.

| Trecho da seção Requisitos | Código(s) | Descrição do(s) teste(s) |
|---|---|---|
| **Tela “Crie sua conta” (imagem Cadastro)** | `CAD-1` | Verifica se os componentes título, Nome, E-mail, Senha, Confirmar senha, dois controles de visibilidade, botão “Criar conta” e link “Entrar” estão presentes na tela de cadastro. |
| Tela Crie sua conta > Campo Nome > Obrigatório | `CAD-2` | Testa o impedimento do cadastro quando o nome está vazio ou contém somente espaços. |
| Tela Crie sua conta > Campo Nome > Aceita de 2 a 100 caracteres | `CAD-3` | Confirma que nomes com 1 ou 101 caracteres são rejeitados e nomes com 2 ou 100 caracteres são aceitos. |
| Tela Crie sua conta > Campo Nome > Preserva a capitalização informada | `CAD-4` | Verifica a remoção de espaços excedentes do nome sem alterar letras maiúsculas ou minúsculas. |
| Tela Crie sua conta > Campo E-mail > Obrigatório | `CAD-5` | Garante que o cadastro é bloqueado quando o e-mail está vazio, contém somente espaços ou é inválido e que a mensagem “Por favor, informe um e-mail válido” é exibida. |
| Tela Crie sua conta > Campo E-mail > Aceita endereço válido com até 254 caracteres | `CAD-6` | Confirma que formatos inválidos e endereços com 255 caracteres são rejeitados e que um endereço válido com 254 caracteres é aceito. |
| Tela Crie sua conta > Campo Senha > Obrigatório | `CAD-7` | Testa o impedimento do cadastro quando a senha está vazia. |
| Tela Crie sua conta > Campo Senha > Aceita de 8 a 128 caracteres | `CAD-8` | Confirma que senhas com 7 ou 129 caracteres são rejeitadas e senhas com 8 ou 128 caracteres são aceitas. |
| Tela Crie sua conta > Campo Senha > Aceita espaços sem removê-los | `CAD-9` | Verifica a preservação dos espaços digitados no início, no meio e no fim da senha. |
| Tela Crie sua conta > Confirmar senha > Obrigatório | `CAD-10` | Testa o impedimento do cadastro quando a confirmação da senha está vazia. |
| Tela Crie sua conta > Confirmar senha > Deve ser idêntico à Senha | `CAD-11` | Confirma a rejeição de uma confirmação que difere da senha por caractere, capitalização ou espaço. |
| Tela Crie sua conta > Mostrar/Ocultar > Alternam somente os respectivos campos e preservam conteúdo | `CAD-12` | Verifica se cada controle alterna somente a visibilidade de seu campo e preserva o valor digitado. |
| Tela Crie sua conta > Criar conta > Valida todos os campos | `CAD-13` | Confirma que um envio inválido apresenta todos os erros e não solicita a criação da conta. |
| Tela Crie sua conta > Criar conta > E-mail já cadastrado | `CAD-14` | Verifica se uma resposta de e-mail duplicado é reconhecida pelo serviço e faz a tela informar “E-mail já foi cadastrado”, sem autenticar ou sair do cadastro. |
| Tela Crie sua conta > Criar conta > Em caso de falha, não cria conta parcialmente | `CAD-15` | Confirma que uma falha no cadastro apresenta a mensagem geral de erro, não exibe sucesso e não autentica o usuário. |
| Tela Crie sua conta > Criar conta > Sucesso autentica e abre Minhas Listas | `CAD-16` | Confirma que uma criação bem-sucedida autentica o usuário e abre “Minhas Listas”. |
| Tela Crie sua conta > Criar conta > Enquanto processa, não permite novo envio | `CAD-17` | Verifica se, durante o cadastro, o envio permanece desabilitado e cliques adicionais não criam novas solicitações. |
| Tela Crie sua conta > Link Entrar | `CAD-18` | Confirma que o link “Entrar” abre a tela “Entre na sua conta”. |
| **Tela “Entre na sua conta” (imagem Login)** | `LOG-1` | Verifica se título, E-mail, Senha, Mostrar/Ocultar, “Manter-me conectado”, “Entrar”, “Criar uma conta” e “Esqueci minha senha” estão presentes na tela de login. |
| Tela Entre na sua conta > Campo E-mail > Obrigatório e válido, com até 254 caracteres | `LOG-2` | Confirma que e-mails vazios, inválidos ou com 255 caracteres são rejeitados e que um endereço válido com 254 caracteres é aceito. |
| Tela Entre na sua conta > Campo Senha > Obrigatório | `LOG-3` | Testa o impedimento do login com senha vazia e confirma que as regras de criação de senha não são aplicadas à senha informada. |
| Tela Entre na sua conta > Campo Senha > Placeholder “Mínimo de 8 caracteres” | `LOG-4` | Verifica se “Mínimo de 8 caracteres” é exibido como orientação no campo Senha. |
| Tela Entre na sua conta > Mostrar/Ocultar > Alterna visualização e preserva conteúdo | `LOG-5` | Confirma que o controle Mostrar/Ocultar alterna a visibilidade da senha sem alterar seu valor. |
| Tela Entre na sua conta > Manter-me conectado > Inicia desmarcado | `LOG-6` | Verifica se “Manter-me conectado” é exibido inicialmente desmarcado. |
| Tela Entre na sua conta > Manter-me conectado > Desmarcado: 12 horas/24 horas; marcado: até 30 dias | `LOG-7` | Confirma que a solicitação de login informa corretamente se “Manter-me conectado” foi marcado. Os prazos efetivos da sessão são validados no backend. |
| Tela Entre na sua conta > Entrar > Dados incorretos exibem mensagem genérica | `LOG-8` | Confirma que tanto um e-mail inexistente quanto uma senha incorreta apresentam “E-mail ou senha inválidos”. |
| Tela Entre na sua conta > Entrar > Bloqueio após tentativas malsucedidas | `LOG-9` | Verifica se uma resposta de bloqueio é reconhecida pelo serviço e faz a tela apresentar a mensagem normativa com o tempo de espera. |
| Tela Entre na sua conta > Entrar > Sucesso abre rota solicitada ou Minhas Listas | `LOG-10` | Confirma que, após o login, é aberta a rota interna solicitada anteriormente ou, quando ela não existe, “Minhas Listas”. |
| Tela Entre na sua conta > Entrar > Enquanto processa, não permite novo envio | `LOG-11` | Verifica se, durante o login, o envio permanece desabilitado e cliques adicionais não criam novas solicitações. |
| Tela Entre na sua conta > Link Criar uma conta | `LOG-12` | Confirma que o link “Criar uma conta” abre a tela “Crie sua conta”. |
| Tela Entre na sua conta > Link Esqueci minha senha | `LOG-13` | Confirma que o link “Esqueci minha senha” abre a tela de recuperação de senha. |
| **Tela de recuperação de senha** | `REC-1` | Verifica se o título acessível, o campo E-mail e o botão de envio estão presentes. |
| Tela de recuperação > Campo E-mail > Obrigatório e válido | `REC-2` | Testa o impedimento da solicitação de recuperação quando o e-mail está vazio ou é inválido. |
| Tela de recuperação > Envio > Sempre apresenta a mesma mensagem | `REC-3` | Confirma que e-mails de contas existentes e inexistentes apresentam a mesma confirmação de solicitação. |
| Tela de recuperação > Conta existente recebe link único válido por 30 minutos | `REC-4` | Verifica se uma solicitação aceita apresenta confirmação sem revelar se a conta existe. A geração, a unicidade e a validade do link são validadas no backend. |
| Tela de recuperação > Novo pedido invalida links anteriores | `REC-5` | Confirma que um link inválido oferece uma nova solicitação de recuperação. A invalidação dos links anteriores é validada no backend. |
| Tela de recuperação > Enquanto processa, não permite novo envio | `REC-6` | Verifica se, durante a solicitação, o envio permanece desabilitado e cliques adicionais não criam novas solicitações. |
| **Tela de redefinição de senha** | `RED-1` | Verifica se o título acessível, Nova senha, Confirmar nova senha, dois controles de visibilidade e o botão de confirmação estão presentes. |
| Tela de redefinição > Nova senha > Obrigatória e de 8 a 128 caracteres | `RED-2` | Confirma que senhas vazias ou com 7 ou 129 caracteres são rejeitadas e senhas com 8 ou 128 caracteres são aceitas. |
| Tela de redefinição > Confirmar nova senha > Obrigatória e idêntica | `RED-3` | Confirma a rejeição de uma confirmação vazia ou diferente da nova senha. |
| Tela de redefinição > Mostrar/Ocultar > Alternam visualização e preservam conteúdo | `RED-4` | Verifica se cada controle alterna somente a visibilidade de seu campo e preserva o valor digitado. |
| Tela de redefinição > Link inválido, usado ou expirado | `RED-5` | Confirma que um link inválido, usado ou expirado oferece nova solicitação, sem apresentar sucesso nem iniciar uma sessão. |
| Tela de redefinição > Sucesso altera senha, encerra acessos e abre login | `RED-6` | Verifica se uma redefinição bem-sucedida limpa a sessão local e abre o login. A alteração da senha e o encerramento dos acessos remotos são validados no backend. |
| Tela de redefinição > Enquanto processa, não permite novo envio | `RED-7` | Verifica se, durante a redefinição, a confirmação permanece desabilitada e cliques adicionais não criam novas solicitações. |
| **Tela “Minhas Listas” após cadastro** | `LIS-1` | Verifica se a tela apresenta o título “Minhas Listas”. |
| Tela Minhas Listas > Exibe somente o título | `LIS-2` | Confirma que a tela exibe somente o título, sem listas, itens, ações ou outros conteúdos. |
| Navegação > Visitante abre página interna | `NAV-1` | Confirma que um visitante é direcionado ao login ao abrir uma página interna e retorna à página originalmente solicitada após se autenticar. |
| Navegação > Autenticado abre login ou cadastro | `NAV-2` | Confirma que um usuário autenticado é direcionado a “Minhas Listas” ao abrir o login ou o cadastro. |
| Navegação > Ao sair, abre login e exige nova autenticação | `NAV-3` | Verifica se, ao sair, o login é aberto e as páginas internas voltam a exigir autenticação. |
| Formulários > Estados inicial, erro por campo, processamento, sucesso e erro geral | `FOR-1` | Verifica se cada formulário apresenta corretamente seus estados inicial, de validação, de processamento, de sucesso e de erro geral. |
| Formulários > Erros preservam nome e e-mail, mas podem limpar senhas | `FOR-2` | Confirma que, após um erro, nome e e-mail permanecem preenchidos e as senhas podem ser limpas sem alterar os demais campos. |
| Formulários > Perceptíveis e operáveis por teclado e tecnologias assistivas | `FOR-3` | Verifica se todos os controles funcionam por teclado, têm nomes acessíveis, associam os erros aos campos, anunciam mudanças de estado e não comunicam informações somente por cor. |
| **Integração com interceptadores mock** |  |  |
| Mock de login > Requisição de login | `MOCK-LOG-1` | Confirma que o interceptor captura a criação de sessão e retorna uma sessão simulada sem encaminhar a requisição ao backend. |
| Mock de login > Requisição fora do escopo | `MOCK-LOG-2` | Confirma que o interceptor encaminha requisições que não correspondem ao endpoint de login. |
| Mock de cadastro > Requisição de cadastro | `MOCK-CAD-1` | Confirma que o interceptor captura a criação de conta e retorna uma sessão simulada sem encaminhar a requisição ao backend. |
| Mock de cadastro > Requisição fora do escopo | `MOCK-CAD-2` | Confirma que o interceptor encaminha requisições que não correspondem ao endpoint de cadastro. |
| **Testes de integração** |  |  |
| Tela Entre na sua conta > Manter-me conectado > Restauração e expiração da sessão | `INT-1` | Confirma que, após recarregar a aplicação, uma sessão válida restaura a interface autenticada e uma sessão expirada abre a interface não autenticada. |
| Navegação > Voltar após sair não revela dados protegidos | `INT-2` | Verifica se, após o logout, usar a ação Voltar do navegador não volta a exibir conteúdo protegido. |
