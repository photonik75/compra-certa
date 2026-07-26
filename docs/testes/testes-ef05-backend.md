# EF-05 — Testes TDD do backend

Cada linha é um ciclo TDD. Controllers, Services e Repositories são isolados por mocks nos testes unitários.

Classificação: Unitário isola o elemento testado com stubs/spies; Integração exercita componentes ou
camadas em conjunto, substituindo banco, APIs e demais sistemas externos; E2E usa Playwright.

| Código | Nível | Trecho da especificação | Teste automatizado a implementar |
|---|---|---|---|
| `BE-ITEM-01` | Unitário | Contrato > `GET .../items` | Valida acesso e paginação e retorna somente itens não excluídos em ordem/grupos determinísticos. |
| `BE-ITEM-02` | Unitário | Requisitos > acesso/estado | Proprietário e participante operam ativa; alheio recebe `NOT_FOUND`; concluída rejeita mutação. |
| `BE-ITEM-03` | Unitário | Contrato > `POST .../items` | Exige sessão/CSRF/chave, valida schema e responde `201` para criação. |
| `BE-ITEM-04` | Unitário | Requisitos > validações | Rejeita produto inválido/inativo/alheio, quantidade fora do intervalo, unidade/categoria e nota inválidas. |
| `BE-ITEM-05` | Unitário | Requisitos > snapshots | Criação copia nome/categoria/ícone e inicia desmarcado, sem depender de futuras mudanças do catálogo. |
| `BE-ITEM-06` | Unitário | Requisitos > duplicidade | Detecta nome normalizado entre não excluídos e retorna dados necessários à resolução. |
| `BE-ITEM-07` | Unitário | Requisitos > Somar quantidade | Soma atomicamente só com unidades iguais, preserva demais campos e rejeita total acima do máximo. |
| `BE-ITEM-08` | Unitário | Contrato > `PATCH .../items/{id}` | Exige `If-Match`, ao menos uma mudança e atualiza campos/snapshot sem alterar marcação/autoria. |
| `BE-ITEM-09` | Unitário | Requisitos > mesclagem | Atualiza destino e exclui logicamente origem na mesma transação, sem criar terceiro item. |
| `BE-ITEM-10` | Unitário | Contrato > `DELETE .../items/{id}` | Exclui logicamente, retorna resumo e repetição idempotente não cria nova alteração. |
| `BE-ITEM-11` | Unitário | Requisitos > resumo | Toda criação, soma, edição, mesclagem e remoção recalcula totais somente com não excluídos. |
| `BE-ITEM-12` | Unitário | Requisitos não funcionais > atomicidade | Falha durante item/resumo/mesclagem reverte toda a operação. |
| `BE-ITEM-13` | Unitário | Requisitos não funcionais > idempotência/concorrência | Repete chave sem efeito adicional; chave divergente e versão antiga retornam conflitos normativos. |
| `BE-ITEM-14` | Unitário | Contrato > erros | Mapeia validação, duplicidade, indisponibilidade, concluída, conflito e não encontrado. |
| `BE-ITEM-15` | Unitário | Requisitos não funcionais > arquitetura | Regra arquitetural garante Controller → Service → Repository no pacote de itens. |
| `BE-ITEM-16` | Integração | Testes de validação da EF-05 | Integração entre Controller, Service e Repository, com banco substituído, prova snapshots, soma/mescla atômica, resumo, isolamento e concorrência. |

