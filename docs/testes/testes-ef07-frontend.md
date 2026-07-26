# EF-07 — Testes TDD do frontend

Executar um teste por ciclo vermelho/verde. Componentes substituem serviços de ciclo de vida e sincronização.

| Código | Trecho da especificação | Teste automatizado a implementar |
|---|---|---|
| `FE-LIFE-01` | Requisitos > Concluir lista | Diálogo mostra consequência e pendentes e é aberto somente ao proprietário de ativa. |
| `FE-LIFE-02` | Requisitos > Concluir > sucesso/erro | Confirma uma vez, abre concluída/avisa no sucesso e mantém estado com mensagem no erro. |
| `FE-LIFE-03` | Requisitos > Concluir > conflito | Preserva estado, informa conflito e oferece recarregar dados. |
| `FE-LIFE-04` | Requisitos > Lista concluída | Exibe todos os dados históricos/resumo e remove ou desabilita todas as mutações. |
| `FE-LIFE-05` | Requisitos > ações por papel | Proprietário vê reabrir/excluir; participante consulta sem ações exclusivas. |
| `FE-LIFE-06` | Requisitos > Reabrir | Confirma, abre lista ativa e avisa no sucesso; trata conflito/falha sem alteração local indevida. |
| `FE-LIFE-07` | Requisitos > Excluir | Exibe nome/consequência, redireciona/avisa no sucesso e preserva lista no conflito/falha. |
| `FE-LIFE-08` | Requisitos > clientes conectados | Evento de conclusão ativa somente leitura; reabertura libera ações; exclusão redireciona ao painel. |
| `FE-LIFE-09` | Requisitos > Cancelar | Botão cancelar e `Esc` fecham sem chamada e devolvem foco ao acionador. |
| `FE-LIFE-10` | Contrato de API | Serviço envia status/headers corretos, trata `ETag`, `204` e traduz todos os erros. |
| `FE-LIFE-11` | Requisitos não funcionais > comunicação | Componentes acessam apenas serviços, sem HTTP ou transporte de eventos direto. |
| `FE-LIFE-12` | Requisitos não funcionais > acessibilidade | Diálogos, foco, estado somente leitura e mensagens são acessíveis e operáveis por teclado. |
| `FE-LIFE-13` | Testes de validação `LIFE-001` a `LIFE-010` | Playwright cobre transições, histórico, permissões, exclusão, concorrência, cancelamento e foco. |

