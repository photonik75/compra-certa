# CompraCerta

Repositório do CompraCerta, organizado como monorepositório para manter frontend, backend, documentação e
ambiente local em um único histórico.

## Estrutura

- `frontend/`: aplicação Angular.
- `backend/`: API Spring Boot.
- `docs/`: especificações funcionais, contrato OpenAPI e definições de testes.
- `prototipo/`: protótipo navegável da interface.

## Frontend

Pré-requisitos: Node.js e npm.

```bash
cd frontend
npm install
npm start
```

Testes unitários:

```bash
cd frontend
npm test
```

Testes de integração:

```bash
cd frontend
npm run test:integration
```

## Backend

O projeto Spring Boot será criado em `backend/`. Consulte `docs/api/openapi.yaml` para o contrato HTTP e
`docs/testes/testes-ef01-backend.md` para a ordem sugerida dos ciclos TDD.
