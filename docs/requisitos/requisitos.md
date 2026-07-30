# Lista de Requisitos

## Requisitos Funcionais
RF01 — Cadastro e autenticação: O sistema deve permitir o cadastro, a autenticação e a identificação dos usuários.
RF02 — Gerenciamento de listas: O sistema deve permitir criar, nomear, editar, concluir e excluir listas de compras, solicitando confirmação nas operações de exclusão.
RF03 — Gerenciamento de itens: O sistema deve permitir adicionar, editar e excluir itens de uma lista de compras.
RF04 — Dados do item: Para cada item, o sistema deve permitir informar nome, quantidade, unidade de medida e categoria.
RF05 — Estado e visualização do item: O sistema deve considerar novos itens como pendentes e permitir marcá-los ou desmarcá-los como comprados, mantendo ambos visíveis até a conclusão da lista.
RF06 — Organização por categorias: O sistema deve permitir organizar e apresentar os itens agrupados por categoria.
RF07 — Compartilhamento: O sistema deve permitir que usuários autorizados compartilhem e acessem uma mesma lista de compras.
RF08 — Sincronização de alterações: O sistema deve disponibilizar aos usuários autorizados as alterações realizadas em uma lista compartilhada, evitando sobrescritas indevidas.
RF09 — Persistência e consulta: O sistema deve salvar e recuperar as listas e seus itens, permitindo sua consulta durante a realização das compras.

## Requisitos Não Funcionais
RNF01 — Usabilidade e acessibilidade: A interface deve ser simples, intuitiva e permitir executar as principais operações com poucas interações, sem necessidade de treinamento; deve oferecer telas com contraste adequado, com componentes de tamanho adequado para telas sensíveis ao toque e compatíveis com boas práticas de acessibilidade.
RNF02 — Responsividade: A aplicação deve funcionar adequadamente em smartphones e adaptar sua interface a diferentes tamanhos de tela.
RNF03 — Desempenho: Operações comuns, como abrir listas, adicionar itens e alterar seu estado, devem apresentar resposta rápida em condições normais de uso.
RNF04 — Disponibilidade e confiabilidade: As listas salvas devem permanecer disponíveis, e o sistema deve informar claramente eventuais falhas de carregamento ou salvamento.
RNF05 — Integridade e concorrência: O sistema deve evitar perda, duplicação ou sobrescrita indevida de dados, inclusive em alterações simultâneas de listas compartilhadas.
RNF06 — Segurança: O sistema deve exigir autenticação, armazenar senhas de forma segura e restringir o acesso às listas aos usuários autorizados.
RNF07 — Qualidade: A arquitetura deve ser modular, manutenível e permitir desenvolvimento guiado por testes automatizados — TDD — das principais regras de negócio.
RNF08 — Custo e evolução: A solução deve priorizar tecnologias de baixo custo e permitir a inclusão futura de preços, mercados, histórico, produtos recorrentes, notificações e outras funcionalidades.
