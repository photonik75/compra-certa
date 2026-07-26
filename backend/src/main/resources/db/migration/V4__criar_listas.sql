CREATE TABLE listas (
    id UUID PRIMARY KEY,
    proprietario_id UUID NOT NULL REFERENCES contas(id),
    nome VARCHAR(60) NOT NULL,
    descricao VARCHAR(240),
    estado VARCHAR(16) NOT NULL CHECK (estado IN ('ACTIVE', 'COMPLETED')),
    criada_em TIMESTAMPTZ NOT NULL,
    atualizada_em TIMESTAMPTZ NOT NULL,
    concluida_em TIMESTAMPTZ,
    versao BIGINT NOT NULL DEFAULT 1,
    excluida BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE participantes_lista (
    lista_id UUID NOT NULL REFERENCES listas(id) ON DELETE CASCADE,
    conta_id UUID NOT NULL REFERENCES contas(id) ON DELETE CASCADE,
    papel VARCHAR(16) NOT NULL DEFAULT 'EDITOR' CHECK (papel = 'EDITOR'),
    PRIMARY KEY (lista_id, conta_id)
);

CREATE TABLE itens_lista (
    id UUID PRIMARY KEY,
    lista_id UUID NOT NULL REFERENCES listas(id) ON DELETE CASCADE,
    marcado BOOLEAN NOT NULL DEFAULT FALSE,
    excluido BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE criacoes_lista_idempotentes (
    conta_id UUID NOT NULL REFERENCES contas(id) ON DELETE CASCADE,
    chave VARCHAR(263) NOT NULL,
    fingerprint VARCHAR(64) NOT NULL,
    lista_id UUID REFERENCES listas(id),
    PRIMARY KEY (conta_id, chave)
);

CREATE UNIQUE INDEX listas_nome_proprietario_ativo_idx
ON listas (
    proprietario_id,
    lower(translate(nome, 'ÁÀÂÃÄÉÈÊËÍÌÎÏÓÒÔÕÖÚÙÛÜÇáàâãäéèêëíìîïóòôõöúùûüç',
        'AAAAAEEEEIIIIOOOOOUUUUCaaaaaeeeeiiiiooooouuuuc'))
)
WHERE excluida = FALSE;

CREATE INDEX listas_ordenacao_idx ON listas (atualizada_em DESC, id ASC) WHERE excluida = FALSE;
CREATE INDEX participantes_lista_conta_idx ON participantes_lista (conta_id, lista_id);
CREATE INDEX itens_lista_resumo_idx ON itens_lista (lista_id) WHERE excluido = FALSE;
