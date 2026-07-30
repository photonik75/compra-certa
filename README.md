# CompraCerta

Repositório do CompraCerta, organizado como monorepositório para manter frontend, backend, documentação e
ambiente local em um único histórico.

## Estrutura

- `frontend/`: aplicação Angular.
- `backend/`: API Spring Boot.
- `docs/`: especificações funcionais, contrato OpenAPI e definições de testes.
- `prototipo/`: protótipo navegável da interface.

## Execução dos testes

Pré-requisitos: Node.js, npm, Java 25 e Docker.

| Suíte | Frontend (em `frontend/`) | Backend (em `backend/`) |
|---|---|---|
| Unitários | `npm test` | `.\mvnw.cmd -Dtest=SuiteDeTestesUnitarios test` |
| Integração | `npm run test:integration` | `.\mvnw.cmd -Dtest=SuiteDeTestesDeIntegracao test` |
| E2E | `npm run test:e2e` | `.\mvnw.cmd -Dtest=SuiteDeTestesDeE2E test` |

Antes da primeira execução do frontend, rode `npm install`. Para os testes E2E do frontend, inicie o backend e
o frontend conforme indicado abaixo.

## Execução para testes manuais

Em terminais separados:

```powershell
cd backend
.\run.ps1
```

```powershell
cd frontend
.\run.ps1
```
