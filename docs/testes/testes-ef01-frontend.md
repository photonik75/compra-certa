| Trecho da seção Requisitos | Código(s) | Descrição do(s) teste(s) | Tipo |
|---|---|---|---|
| **Tela “Crie sua conta” (imagem Cadastro)** | `CAD-1` | Renderiza título, Nome, E-mail, Senha, Confirmar senha, dois controles de visibilidade, botão “Criar conta” e link “Entrar”. | Angular Testing Library |
| Tela Crie sua conta > Campo Nome > Obrigatório | `CAD-2` | Rejeita nome vazio ou composto somente por espaços. | Vitest |
| Tela Crie sua conta > Campo Nome > Aceita de 2 a 100 caracteres | `CAD-3` | Rejeita 1 caractere.<br>Aceita 2 caracteres.<br>Aceita 100 caracteres.<br>Rejeita 101 caracteres. | Vitest |
| Tela Crie sua conta > Campo Nome > Preserva a capitalização informada | `CAD-4` | Normaliza espaços sem modificar letras maiúsculas e minúsculas. | Vitest |
| Tela Crie sua conta > Campo E-mail > Obrigatório | `CAD-5` | Rejeita e-mail vazio, composto somente por espaços ou inválido e exibe “Por favor, informe um e-mail válido”. | Vitest |
| Tela Crie sua conta > Campo E-mail > Aceita endereço válido com até 254 caracteres | `CAD-6` | Rejeita formatos inválidos.<br>Aceita 254 e rejeita 255 caracteres. | Vitest |
| Tela Crie sua conta > Campo Senha > Obrigatório | `CAD-7` | Rejeita senha vazia. | Vitest |
| Tela Crie sua conta > Campo Senha > Aceita de 8 a 128 caracteres | `CAD-8` | Rejeita 7, aceita 8 e 128 e rejeita 129 caracteres. | Vitest |
| Tela Crie sua conta > Campo Senha > Aceita espaços sem removê-los | `CAD-9` | Preserva espaços no início, meio e fim da senha. | Vitest |
| Tela Crie sua conta > Confirmar senha > Obrigatório | `CAD-10` | Rejeita confirmação vazia. | Vitest |
| Tela Crie sua conta > Confirmar senha > Deve ser idêntico à Senha | `CAD-11` | Rejeita confirmação diferente por caractere, caixa ou espaço. | Vitest |
| Tela Crie sua conta > Mostrar/Ocultar > Alternam somente os respectivos campos e preservam conteúdo | `CAD-12` | Cada controle altera apenas o campo associado.<br>Alternar visibilidade preserva o conteúdo digitado. | Angular Testing Library |
| Tela Crie sua conta > Criar conta > Valida todos os campos | `CAD-13` | Envio inválido mostra todos os erros e não realiza requisição. | Angular Testing Library |
| Tela Crie sua conta > Criar conta > E-mail já cadastrado | `CAD-14` | Abre o pop-up “E-mail já foi cadastrado”.<br>Permanece não autenticado e na tela de cadastro. | Angular Testing Library + HttpTestingController<br>Vitest |
| Tela Crie sua conta > Criar conta > Em caso de falha, não cria conta parcialmente | `CAD-15` | Falha mockada exibe “Ocorreu um erro ao tentar criar sua conta. Aguarde e tente novamente em alguns instantes.” e não produz estado visual de sucesso ou autenticação. A atomicidade real será testada no backend. | Angular Testing Library + HttpTestingController |
| Tela Crie sua conta > Criar conta > Sucesso autentica e abre Minhas Listas | `CAD-16` | `201 SessionResponse` define o usuário autenticado.<br>Navega para “Minhas Listas”. | Vitest + HttpTestingController<br>RouterTestingHarness |
| Tela Crie sua conta > Criar conta > Enquanto processa, não permite novo envio | `CAD-17` | Desabilita o envio e múltiplos cliques produzem uma requisição. | Angular Testing Library |
| Tela Crie sua conta > Link Entrar | `CAD-18` | Navega para “Entre na sua conta”. | RouterTestingHarness |
| **Tela “Entre na sua conta” (imagem Login)** | `CAD-19` | Renderiza título, E-mail, Senha, Mostrar/Ocultar, “Manter-me conectado”, “Entrar”, “Criar uma conta” e “Esqueci minha senha”. | Angular Testing Library |
| Tela Entre na sua conta > Campo E-mail > Obrigatório e válido, com até 254 caracteres | `CAD-20` | Rejeita vazio e formato inválido.<br>Aceita 254 e rejeita 255 caracteres. | Vitest |
| Tela Entre na sua conta > Campo Senha > Obrigatório | `CAD-21` | Rejeita senha vazia sem aplicar as regras do cadastro. | Vitest |
| Tela Entre na sua conta > Campo Senha > Placeholder “Mínimo de 8 caracteres” | `CAD-22` | Renderiza o placeholder normativo. | Angular Testing Library |
| Tela Entre na sua conta > Mostrar/Ocultar > Alterna visualização e preserva conteúdo | `CAD-23` | Alterna o tipo do campo sem alterar a senha. | Angular Testing Library |
| Tela Entre na sua conta > Manter-me conectado > Inicia desmarcado | `CAD-24` | Renderiza o checkbox inicialmente desmarcado. | Angular Testing Library |
| Tela Entre na sua conta > Manter-me conectado > Desmarcado: 12 horas/24 horas; marcado: até 30 dias | `CAD-25` | Envia corretamente `manterConectado`.<br>Restaura ou encerra a interface conforme sessões mockadas válidas ou expiradas. Os prazos reais pertencem ao backend. | HttpTestingController<br>Playwright com backend mockado |
| Tela Entre na sua conta > Entrar > Dados incorretos exibem mensagem genérica | `CAD-26` | E-mail inexistente e senha incorreta exibem “E-mail ou senha inválidos”. | Angular Testing Library |
| Tela Entre na sua conta > Entrar > Bloqueio após tentativas malsucedidas | `CAD-27` | Resposta mock `429` apresenta a mensagem normativa e respeita `Retry-After` visualmente. | Angular Testing Library + HttpTestingController |
| Tela Entre na sua conta > Entrar > Sucesso abre rota solicitada ou Minhas Listas | `CAD-28` | Usa a rota guardada ou “Minhas Listas” como fallback. | RouterTestingHarness |
| Tela Entre na sua conta > Entrar > Enquanto processa, não permite novo envio | `CAD-29` | Desabilita o envio e evita requisições duplicadas. | Angular Testing Library |
| Tela Entre na sua conta > Link Criar uma conta | `CAD-30` | Navega para “Crie sua conta”. | RouterTestingHarness |
| Tela Entre na sua conta > Link Esqueci minha senha | `CAD-31` | Navega para a recuperação de senha. | RouterTestingHarness |
| **Tela de recuperação de senha** | `CAD-32` | Renderiza título acessível, campo E-mail e botão de envio. | Angular Testing Library |
| Tela de recuperação > Campo E-mail > Obrigatório e válido | `CAD-33` | Rejeita e-mail vazio ou inválido antes da requisição. | Vitest |
| Tela de recuperação > Envio > Sempre apresenta a mesma mensagem | `CAD-34` | Contas existente e inexistente simuladas apresentam confirmação idêntica. | Angular Testing Library |
| Tela de recuperação > Conta existente recebe link único válido por 30 minutos | `CAD-35` | Trata `202` sem tentar inferir se houve envio. Geração, unicidade e validade pertencem ao backend. | HttpTestingController |
| Tela de recuperação > Novo pedido invalida links anteriores | `CAD-36` | Trata um link anterior rejeitado pelo backend como inválido. A invalidação real pertence ao backend. | Angular Testing Library |
| Tela de recuperação > Enquanto processa, não permite novo envio | `CAD-37` | Desabilita o botão e evita requisições duplicadas. | Angular Testing Library |
| **Tela de redefinição de senha** | `CAD-38` | Renderiza título acessível, Nova senha, Confirmar nova senha, dois controles de visibilidade e botão de confirmação. | Angular Testing Library |
| Tela de redefinição > Nova senha > Obrigatória e de 8 a 128 caracteres | `CAD-39` | Rejeita vazio, 7 e 129; aceita 8 e 128 caracteres. | Vitest |
| Tela de redefinição > Confirmar nova senha > Obrigatória e idêntica | `CAD-40` | Rejeita confirmação vazia ou divergente. | Vitest |
| Tela de redefinição > Mostrar/Ocultar > Alternam visualização e preservam conteúdo | `CAD-41` | Alterna cada campo independentemente sem modificar valores. | Angular Testing Library |
| Tela de redefinição > Link inválido, usado ou expirado | `CAD-42` | Apresenta um estado seguro e oferece nova solicitação.<br>Não apresenta sucesso nem inicia sessão. | Angular Testing Library + HttpTestingController |
| Tela de redefinição > Sucesso altera senha, encerra acessos e abre login | `CAD-43` | Resposta `204` limpa o estado autenticado.<br>Navega para login. Alteração da senha e invalidação remota pertencem ao backend. | Vitest + HttpTestingController<br>RouterTestingHarness |
| Tela de redefinição > Enquanto processa, não permite novo envio | `CAD-44` | Desabilita a confirmação e impede requisições duplicadas. | Angular Testing Library |
| **Tela “Minhas Listas” após cadastro** | `CAD-45` | Renderiza a tela com o heading “Minhas Listas”. | Angular Testing Library |
| Tela Minhas Listas > Exibe somente o título | `CAD-46` | Não renderiza listas, itens, ações ou outros conteúdos. | Angular Testing Library |
| Navegação > Visitante abre página interna | `CAD-47` | Redireciona para login.<br>Depois do login, retorna à rota originalmente solicitada. | RouterTestingHarness |
| Navegação > Autenticado abre login ou cadastro | `CAD-48` | Redireciona para “Minhas Listas”. | RouterTestingHarness |
| Navegação > Ao sair, abre login e exige nova autenticação | `CAD-49` | Limpa o estado e abre login.<br>Guard volta a bloquear páginas internas. | RouterTestingHarness |
| Navegação > Voltar após sair não revela dados protegidos | `CAD-50` | Após logout e ação Voltar, conteúdo protegido não reaparece. | Playwright com backend mockado |
| Formulários > Estados inicial, erro por campo, processamento, sucesso e erro geral | `CAD-51` | Verifica todos os estados visuais em cada formulário. | Angular Testing Library |
| Formulários > Erros preservam nome e e-mail, mas podem limpar senhas | `CAD-52` | Mantém nome e e-mail após erro.<br>Permite limpar senhas sem afetar outros campos. | Angular Testing Library |
| Formulários > Perceptíveis e operáveis por teclado e tecnologias assistivas | `CAD-53` | Opera todos os controles por teclado.<br>Verifica nomes acessíveis e associação de erros.<br>Anuncia processamento, erro e sucesso.<br>Garante que estados não dependam somente de cor. | Angular Testing Library + user-event + axe |
