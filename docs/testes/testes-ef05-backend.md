# EF-05 — Testes TDD do backend

Cada linha é um ciclo TDD. Controllers, Services e Repositories são isolados por mocks nos testes unitários.

| Código | Trecho da especificação | Teste automatizado a implementar |
|---|---|---|
| `BE-ITEM-01` | Contrato > `GET .../items` | Valida acesso e paginação e retorna somente itens não excluídos em ordem/grupos determinísticos. |
| `BE-ITEM-02` | Requisitos > acesso/estado | Proprietário e participante operam ativa; alheio recebe `NOT_FOUND`; concluída rejeita mutação. |
| `BE-ITEM-03` | Contrato > `POST .../items` | Exige sessão/CSRF/chave, valida schema e responde `201` para criação. |
| `BE-ITEM-04` | Requisitos > validações | Rejeita produto inválido/inativo/alheio, quantidade fora do intervalo, unidade/categoria e nota inválidas. |
| `BE-ITEM-05` | Requisitos > snapshots | Criação copia nome/categoria/ícone e inicia desmarcado, sem depender de futuras mudanças do catálogo. |
| `BE-ITEM-06` | Requisitos > duplicidade | Detecta nome normalizado entre não excluídos e retorna dados necessários à resolução. |
| `BE-ITEM-07` | Requisitos > Somar quantidade | Soma atomicamente só com unidades iguais, preserva demais campos e rejeita total acima do máximo. |
| `BE-ITEM-08` | Contrato > `PATCH .../items/{id}` | Exige `If-Match`, ao menos uma mudança e atualiza campos/snapshot sem alterar marcação/autoria. |
| `BE-ITEM-09` | Requisitos > mesclagem | Atualiza destino e exclui logicamente origem na mesma transação, sem criar terceiro item. |
| `BE-ITEM-10` | Contrato > `DELETE .../items/{id}` | Exclui logicamente, retorna resumo e repetição idempotente não cria nova alteração. |
| `BE-ITEM-11` | Requisitos > resumo | Toda criação, soma, edição, mesclagem e remoção recalcula totais somente com não excluídos. |
| `BE-ITEM-12` | Requisitos não funcionais > atomicidade | Falha durante item/resumo/mesclagem reverte toda a operação. |
| `BE-ITEM-13` | Requisitos não funcionais > idempotência/concorrência | Repete chave sem efeito adicional; chave divergente e versão antiga retornam conflitos normativos. |
| `BE-ITEM-14` | Contrato > erros | Mapeia validação, duplicidade, indisponibilidade, concluída, conflito e não encontrado. |
| `BE-ITEM-15` | Requisitos não funcionais > arquitetura | Regra arquitetural garante Controller → Service → Repository no pacote de itens. |
| `BE-ITEM-16` | Testes de validação da EF-05 | Integração HTTP/banco prova snapshots, soma/mescla atômica, resumo, isolamento e concorrência. |

