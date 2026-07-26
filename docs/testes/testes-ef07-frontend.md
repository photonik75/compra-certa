# EF-07 — Testes TDD do frontend

Executar um teste por ciclo vermelho/verde. Componentes substituem serviços de ciclo de vida e sincronização.

Classificação: Unitário isola o elemento testado com stubs/spies; Integração exercita componentes ou
camadas em conjunto, substituindo banco, APIs e demais sistemas externos; E2E usa Playwright.

| Código | Nível | Trecho da especificação | Teste automatizado a implementar |
|---|---|---|---|
| `FE-LIFE-01` | Unitário | Requisitos > Concluir lista | Diálogo mostra consequência e pendentes e é aberto somente ao proprietário de ativa. |
| `FE-LIFE-02` | Unitário | Requisitos > Concluir > sucesso/erro | Confirma uma vez, abre concluída/avisa no sucesso e mantém estado com mensagem no erro. |
| `FE-LIFE-03` | Unitário | Requisitos > Concluir > conflito | Preserva estado, informa conflito e oferece recarregar dados. |
| `FE-LIFE-04` | Unitário | Requisitos > Lista concluída | Exibe todos os dados históricos/resumo e remove ou desabilita todas as mutações. |
| `FE-LIFE-05` | Unitário | Requisitos > ações por papel | Proprietário vê reabrir/excluir; participante consulta sem ações exclusivas. |
| `FE-LIFE-06` | Unitário | Requisitos > Reabrir | Confirma, abre lista ativa e avisa no sucesso; trata conflito/falha sem alteração local indevida. |
| `FE-LIFE-07` | Unitário | Requisitos > Excluir | Exibe nome/consequência, redireciona/avisa no sucesso e preserva lista no conflito/falha. |
| `FE-LIFE-08` | Unitário | Requisitos > clientes conectados | Evento de conclusão ativa somente leitura; reabertura libera ações; exclusão redireciona ao painel. |
| `FE-LIFE-09` | Unitário | Requisitos > Cancelar | Botão cancelar e `Esc` fecham sem chamada e devolvem foco ao acionador. |
| `FE-LIFE-10` | Unitário | Contrato de API | Serviço envia status/headers corretos, trata `ETag`, `204` e traduz todos os erros. |
| `FE-LIFE-11` | Unitário | Requisitos não funcionais > comunicação | Componentes acessam apenas serviços, sem HTTP ou transporte de eventos direto. |
| `FE-LIFE-12` | Unitário | Requisitos não funcionais > acessibilidade | Diálogos, foco, estado somente leitura e mensagens são acessíveis e operáveis por teclado. |
| `FE-LIFE-13` | E2E | Testes de validação `LIFE-001` a `LIFE-010` | Playwright cobre transições, histórico, permissões, exclusão, concorrência, cancelamento e foco. |
| `FE-LIFE-14` | Integração | Requisitos não funcionais > rota | `RouterTestingHarness` valida `/listas/:listId` nos estados ativo e concluído, os guards de sessão/acesso e a ausência de rotas próprias para os diálogos. |
