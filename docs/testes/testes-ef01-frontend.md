| Trecho da seção Requisitos | Código(s) | Descrição do(s) teste(s) | Tipo |
|---|---|---|---|
| **Tela “Crie sua conta” (imagem Cadastro)** | `REG-UI-001` | Renderiza título, Nome, E-mail, Senha, Confirmar senha, dois controles de visibilidade, botão “Criar conta” e link “Entrar”. | Angular Testing Library |
| Tela Crie sua conta > Campo Nome > Obrigatório | `REG-VAL-001` | Rejeita nome vazio ou composto somente por espaços. | Vitest |
| Tela Crie sua conta > Campo Nome > Aceita de 2 a 100 caracteres | `REG-VAL-002`<br>`REG-VAL-003`<br>`REG-VAL-004`<br>`REG-VAL-005` | Rejeita 1 caractere.<br>Aceita 2 caracteres.<br>Aceita 100 caracteres.<br>Rejeita 101 caracteres. | Vitest |
| Tela Crie sua conta > Campo Nome > Preserva a capitalização informada | `REG-VAL-006` | Normaliza espaços sem modificar letras maiúsculas e minúsculas. | Vitest |
| Tela Crie sua conta > Campo E-mail > Obrigatório | `REG-VAL-007` | Rejeita e-mail vazio ou composto somente por espaços. | Vitest |
| Tela Crie sua conta > Campo E-mail > Aceita endereço válido com até 254 caracteres | `REG-VAL-008`<br>`REG-VAL-009` | Rejeita formatos inválidos.<br>Aceita 254 e rejeita 255 caracteres. | Vitest |
| Tela Crie sua conta > Campo E-mail > Não diferencia caixa ao verificar duplicidade | `REG-HTTP-001`<br>`REG-STATE-001` | Trata `409` para e-mail com caixa diferente como duplicidade.<br>Não autentica nem navega após o conflito. | HttpTestingController<br>Vitest |
| Tela Crie sua conta > Campo Senha > Obrigatório | `REG-VAL-010` | Rejeita senha vazia. | Vitest |
| Tela Crie sua conta > Campo Senha > Aceita de 8 a 128 caracteres | `REG-VAL-011` | Rejeita 7, aceita 8 e 128 e rejeita 129 caracteres. | Vitest |
| Tela Crie sua conta > Campo Senha > Aceita espaços sem removê-los | `REG-VAL-012` | Preserva espaços no início, meio e fim da senha. | Vitest |
| Tela Crie sua conta > Confirmar senha > Obrigatório | `REG-VAL-013` | Rejeita confirmação vazia. | Vitest |
| Tela Crie sua conta > Confirmar senha > Deve ser idêntico à Senha | `REG-VAL-014` | Rejeita confirmação diferente por caractere, caixa ou espaço. | Vitest |
| Tela Crie sua conta > Mostrar/Ocultar > Alternam somente os respectivos campos e preservam conteúdo | `REG-UI-002`<br>`REG-UI-003` | Cada controle altera apenas o campo associado.<br>Alternar visibilidade preserva o conteúdo digitado. | Angular Testing Library |
| Tela Crie sua conta > Criar conta > Valida todos os campos | `REG-UI-004` | Envio inválido mostra todos os erros e não realiza requisição. | Angular Testing Library |
| Tela Crie sua conta > Criar conta > E-mail já cadastrado | `REG-UI-005`<br>`REG-STATE-001` | Abre o pop-up “E-mail já foi cadastrado”.<br>Permanece não autenticado e na tela de cadastro. | Angular Testing Library + HttpTestingController<br>Vitest |
| Tela Crie sua conta > Criar conta > Em caso de falha, não cria conta parcialmente | `REG-UI-006` | Falha mockada não produz estado visual de sucesso ou autenticação. A atomicidade real será testada no backend. | Angular Testing Library |
| Tela Crie sua conta > Criar conta > Sucesso autentica e abre Minhas Listas | `REG-STATE-002`<br>`REG-ROUTE-001` | `201 SessionResponse` define o usuário autenticado.<br>Navega para “Minhas Listas”. | Vitest + HttpTestingController<br>RouterTestingHarness |
| Tela Crie sua conta > Criar conta > Enquanto processa, não permite novo envio | `REG-UI-007` | Desabilita o envio e múltiplos cliques produzem uma requisição. | Angular Testing Library |
| Tela Crie sua conta > Link Entrar | `REG-ROUTE-002` | Navega para “Entre na sua conta”. | RouterTestingHarness |
| **Tela “Entre na sua conta” (imagem Login)** | `LOGIN-UI-001` | Renderiza título, E-mail, Senha, Mostrar/Ocultar, “Manter-me conectado”, “Entrar”, “Criar uma conta” e “Esqueci minha senha”. | Angular Testing Library |
| Tela Entre na sua conta > Campo E-mail > Obrigatório e válido, com até 254 caracteres | `LOGIN-VAL-001`<br>`LOGIN-VAL-002` | Rejeita vazio e formato inválido.<br>Aceita 254 e rejeita 255 caracteres. | Vitest |
| Tela Entre na sua conta > Campo E-mail > Não diferencia caixa ao localizar a conta | `LOGIN-HTTP-001` | Aceita a resposta de sucesso mockada independentemente da caixa do e-mail enviado. A localização real pertence ao backend. | HttpTestingController |
| Tela Entre na sua conta > Campo Senha > Obrigatório | `LOGIN-VAL-003` | Rejeita senha vazia sem aplicar as regras do cadastro. | Vitest |
| Tela Entre na sua conta > Campo Senha > Placeholder “Mínimo de 8 caracteres” | `LOGIN-UI-002` | Renderiza o placeholder normativo. | Angular Testing Library |
| Tela Entre na sua conta > Mostrar/Ocultar > Alterna visualização e preserva conteúdo | `LOGIN-UI-003` | Alterna o tipo do campo sem alterar a senha. | Angular Testing Library |
| Tela Entre na sua conta > Manter-me conectado > Inicia desmarcado | `LOGIN-UI-004` | Renderiza o checkbox inicialmente desmarcado. | Angular Testing Library |
| Tela Entre na sua conta > Manter-me conectado > Desmarcado: 12 horas/24 horas; marcado: até 30 dias | `LOGIN-HTTP-002`<br>`SESSION-PW-001` | Envia corretamente `manterConectado`.<br>Restaura ou encerra a interface conforme sessões mockadas válidas ou expiradas. Os prazos reais pertencem ao backend. | HttpTestingController<br>Playwright com backend mockado |
| Tela Entre na sua conta > Entrar > Dados incorretos exibem mensagem genérica | `LOGIN-UI-005` | E-mail inexistente e senha incorreta exibem “E-mail ou senha inválidos”. | Angular Testing Library |
| Tela Entre na sua conta > Entrar > Bloqueio após tentativas malsucedidas | `LOGIN-UI-006` | Resposta mock `429` apresenta a mensagem normativa e respeita `Retry-After` visualmente. | Angular Testing Library + HttpTestingController |
| Tela Entre na sua conta > Entrar > Sucesso abre rota solicitada ou Minhas Listas | `LOGIN-ROUTE-001` | Usa a rota guardada ou “Minhas Listas” como fallback. | RouterTestingHarness |
| Tela Entre na sua conta > Entrar > Enquanto processa, não permite novo envio | `LOGIN-UI-007` | Desabilita o envio e evita requisições duplicadas. | Angular Testing Library |
| Tela Entre na sua conta > Link Criar uma conta | `LOGIN-ROUTE-002` | Navega para “Crie sua conta”. | RouterTestingHarness |
| Tela Entre na sua conta > Link Esqueci minha senha | `LOGIN-ROUTE-003` | Navega para a recuperação de senha. | RouterTestingHarness |
| **Tela de recuperação de senha** | `RESET-REQ-UI-001` | Renderiza título acessível, campo E-mail e botão de envio. | Angular Testing Library |
| Tela de recuperação > Campo E-mail > Obrigatório e válido | `RESET-REQ-VAL-001` | Rejeita e-mail vazio ou inválido antes da requisição. | Vitest |
| Tela de recuperação > Envio > Sempre apresenta a mesma mensagem | `RESET-REQ-UI-002` | Contas existente e inexistente simuladas apresentam confirmação idêntica. | Angular Testing Library |
| Tela de recuperação > Conta existente recebe link único válido por 30 minutos | `RESET-REQ-HTTP-001` | Trata `202` sem tentar inferir se houve envio. Geração, unicidade e validade pertencem ao backend. | HttpTestingController |
| Tela de recuperação > Novo pedido invalida links anteriores | `RESET-UI-003` | Trata um link anterior rejeitado pelo backend como inválido. A invalidação real pertence ao backend. | Angular Testing Library |
| Tela de recuperação > Enquanto processa, não permite novo envio | `RESET-REQ-UI-003` | Desabilita o botão e evita requisições duplicadas. | Angular Testing Library |
| **Tela de redefinição de senha** | `RESET-UI-001` | Renderiza título acessível, Nova senha, Confirmar nova senha, dois controles de visibilidade e botão de confirmação. | Angular Testing Library |
| Tela de redefinição > Nova senha > Obrigatória e de 8 a 128 caracteres | `RESET-VAL-001` | Rejeita vazio, 7 e 129; aceita 8 e 128 caracteres. | Vitest |
| Tela de redefinição > Confirmar nova senha > Obrigatória e idêntica | `RESET-VAL-002` | Rejeita confirmação vazia ou divergente. | Vitest |
| Tela de redefinição > Mostrar/Ocultar > Alternam visualização e preservam conteúdo | `RESET-UI-002` | Alterna cada campo independentemente sem modificar valores. | Angular Testing Library |
| Tela de redefinição > Link inválido, usado ou expirado | `RESET-UI-003`<br>`RESET-UI-004` | Apresenta um estado seguro e oferece nova solicitação.<br>Não apresenta sucesso nem inicia sessão. | Angular Testing Library + HttpTestingController |
| Tela de redefinição > Sucesso altera senha, encerra acessos e abre login | `RESET-STATE-001`<br>`RESET-ROUTE-001` | Resposta `204` limpa o estado autenticado.<br>Navega para login. Alteração da senha e invalidação remota pertencem ao backend. | Vitest + HttpTestingController<br>RouterTestingHarness |
| Tela de redefinição > Enquanto processa, não permite novo envio | `RESET-UI-005` | Desabilita a confirmação e impede requisições duplicadas. | Angular Testing Library |
| **Tela “Minhas Listas” após cadastro** | `LISTS-UI-001` | Renderiza a tela com o heading “Minhas Listas”. | Angular Testing Library |
| Tela Minhas Listas > Exibe somente o título | `LISTS-UI-002` | Não renderiza listas, itens, ações ou outros conteúdos. | Angular Testing Library |
| Navegação > Visitante abre página interna | `AUTH-ROUTE-001`<br>`AUTH-ROUTE-002` | Redireciona para login.<br>Depois do login, retorna à rota originalmente solicitada. | RouterTestingHarness |
| Navegação > Autenticado abre login ou cadastro | `AUTH-ROUTE-003` | Redireciona para “Minhas Listas”. | RouterTestingHarness |
| Navegação > Ao sair, abre login e exige nova autenticação | `LOGOUT-ROUTE-001`<br>`LOGOUT-ROUTE-002` | Limpa o estado e abre login.<br>Guard volta a bloquear páginas internas. | RouterTestingHarness |
| Navegação > Voltar após sair não revela dados protegidos | `LOGOUT-PW-001` | Após logout e ação Voltar, conteúdo protegido não reaparece. | Playwright com backend mockado |
| Formulários > Estados inicial, erro por campo, processamento, sucesso e erro geral | `FORM-UI-001` | Verifica todos os estados visuais em cada formulário. | Angular Testing Library |
| Formulários > Erros preservam nome e e-mail, mas podem limpar senhas | `FORM-UI-002`<br>`FORM-UI-003` | Mantém nome e e-mail após erro.<br>Permite limpar senhas sem afetar outros campos. | Angular Testing Library |
| Formulários > Perceptíveis e operáveis por teclado e tecnologias assistivas | `AUTH-A11Y-001`<br>`AUTH-A11Y-002`<br>`AUTH-A11Y-003`<br>`AUTH-A11Y-004` | Opera todos os controles por teclado.<br>Verifica nomes acessíveis e associação de erros.<br>Anuncia processamento, erro e sucesso.<br>Garante que estados não dependam somente de cor. | Angular Testing Library + user-event + axe |