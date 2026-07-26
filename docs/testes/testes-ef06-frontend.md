# EF-06 — Testes TDD do frontend

Cada teste deve iniciar falhando. Componentes usam stubs/spies dos serviços de compra e sincronização.

| Código | Trecho da especificação | Teste automatizado a implementar |
|---|---|---|
| `FE-SHOP-01` | Requisitos > Tela de compra | Renderiza itens agrupados, estado, quantidade/unidade/nota, progresso e resumo. |
| `FE-SHOP-02` | Requisitos > marcar item | Proprietário/participante de ativa marca e desmarca; concluída/alheio não oferece controle. |
| `FE-SHOP-03` | Requisitos > marcação | Atualiza item e resumo após confirmação, exibindo autor e horário fornecidos pelo servidor. |
| `FE-SHOP-04` | Requisitos > processamento/falha | Evita clique repetido, sinaliza processamento e reverte estado otimista com mensagem polida. |
| `FE-SHOP-05` | Requisitos > conflito | Em `CONFLICT`, descarta estado local, exibe aviso e aplica item/resumo atuais do servidor. |
| `FE-SHOP-06` | Requisitos > atualização colaborativa | Evento válido atualiza item/resumo em até cinco segundos e ignora evento de outra lista. |
| `FE-SHOP-07` | Requisitos > ordenação durante compra | Marcação não muda grupo nem ordem do item e mantém snapshots exibidos. |
| `FE-SHOP-08` | Requisitos > sem conexão | Exibe estado desconectado, impede escrita e mantém consulta do último estado conhecido. |
| `FE-SHOP-09` | Requisitos > reconexão | Ressincroniza do servidor antes de liberar nova marcação e converge resumo/itens. |
| `FE-SHOP-10` | Contrato de API | Serviço envia rota/corpo/`If-Match`/chave e traduz resposta, `ETag` e erros. |
| `FE-SHOP-11` | Requisitos não funcionais > comunicação | Componente não conhece HTTP/transporte de eventos e usa somente serviços injetados. |
| `FE-SHOP-12` | Requisitos não funcionais > acessibilidade | Checkbox, conexão, processamento, conflito e resumo são nomeados/anunciados e operáveis por teclado. |
| `FE-SHOP-13` | Testes de validação `SHOP-001` a `SHOP-011` | Playwright cobre marcação, concorrência, falha, offline/reconexão, papéis e resumo simultâneo. |

