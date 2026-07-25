CREATE TABLE sessoes (
    token_hash VARCHAR(64) PRIMARY KEY,
    csrf_token_hash VARCHAR(64) NOT NULL,
    conta_id UUID NOT NULL REFERENCES contas(id) ON DELETE CASCADE,
    criada_em TIMESTAMPTZ NOT NULL,
    duracao_inatividade_segundos BIGINT NOT NULL CHECK (duracao_inatividade_segundos > 0),
    expira_por_inatividade TIMESTAMPTZ NOT NULL,
    expira_em_definitivo TIMESTAMPTZ NOT NULL,
    revogada BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX sessoes_conta_id_idx ON sessoes(conta_id);

CREATE TABLE tokens_recuperacao (
    token_hash VARCHAR(64) PRIMARY KEY,
    conta_id UUID NOT NULL REFERENCES contas(id) ON DELETE CASCADE,
    expira_em TIMESTAMPTZ NOT NULL,
    usado BOOLEAN NOT NULL DEFAULT FALSE,
    invalidado BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX tokens_recuperacao_conta_id_idx ON tokens_recuperacao(conta_id);

CREATE TABLE idempotencias (
    chave VARCHAR(263) PRIMARY KEY,
    fingerprint VARCHAR(64) NOT NULL,
    resultado_tipo VARCHAR(255),
    resultado_json TEXT
);

CREATE TABLE tentativas_login (
    email VARCHAR(254) NOT NULL,
    ocorrida_em TIMESTAMPTZ NOT NULL
);

CREATE INDEX tentativas_login_email_ocorrida_idx ON tentativas_login(email, ocorrida_em);
