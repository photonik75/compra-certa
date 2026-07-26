CREATE TABLE categorias (
    id UUID PRIMARY KEY,
    conta_id UUID NOT NULL REFERENCES contas(id) ON DELETE CASCADE,
    nome VARCHAR(40) NOT NULL,
    icone VARCHAR(8) NOT NULL,
    criada_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    atualizada_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    versao BIGINT NOT NULL DEFAULT 1,
    excluida BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE produtos (
    id UUID PRIMARY KEY,
    conta_id UUID NOT NULL REFERENCES contas(id) ON DELETE CASCADE,
    categoria_id UUID NOT NULL REFERENCES categorias(id),
    nome VARCHAR(60) NOT NULL,
    unidade VARCHAR(20) NOT NULL,
    categoria_nome VARCHAR(40) NOT NULL,
    categoria_icone VARCHAR(8) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    versao BIGINT NOT NULL DEFAULT 1
);

ALTER TABLE itens_lista
    ADD COLUMN produto_id UUID REFERENCES produtos(id),
    ADD COLUMN produto_nome VARCHAR(60),
    ADD COLUMN categoria_id UUID REFERENCES categorias(id),
    ADD COLUMN categoria_nome VARCHAR(40),
    ADD COLUMN categoria_icone VARCHAR(8),
    ADD COLUMN quantidade NUMERIC(12,3) NOT NULL DEFAULT 1,
    ADD COLUMN unidade VARCHAR(20) NOT NULL DEFAULT 'UNIT',
    ADD COLUMN observacoes VARCHAR(240),
    ADD COLUMN criado_por UUID REFERENCES contas(id),
    ADD COLUMN criado_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN atualizado_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN marcado_em TIMESTAMPTZ,
    ADD COLUMN marcado_por UUID REFERENCES contas(id),
    ADD COLUMN versao BIGINT NOT NULL DEFAULT 1;

CREATE TABLE convites_lista (
    id UUID PRIMARY KEY,
    lista_id UUID NOT NULL REFERENCES listas(id) ON DELETE CASCADE,
    email VARCHAR(254) NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    estado VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    estado_entrega VARCHAR(16) NOT NULL DEFAULT 'SENT',
    expira_em TIMESTAMPTZ NOT NULL,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    versao BIGINT NOT NULL DEFAULT 1
);

CREATE TABLE eventos_lista (
    id UUID PRIMARY KEY,
    lista_id UUID NOT NULL REFERENCES listas(id) ON DELETE CASCADE,
    tipo VARCHAR(40) NOT NULL,
    recurso_id UUID,
    ator_id UUID REFERENCES contas(id),
    versao_lista BIGINT NOT NULL,
    ocorrido_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    payload TEXT NOT NULL DEFAULT '{}'
);

CREATE TABLE operacoes_idempotentes (
    conta_id UUID NOT NULL REFERENCES contas(id) ON DELETE CASCADE,
    escopo VARCHAR(80) NOT NULL,
    chave VARCHAR(263) NOT NULL,
    fingerprint VARCHAR(64) NOT NULL,
    recurso_id UUID,
    resultado VARCHAR(40),
    PRIMARY KEY (conta_id, escopo, chave)
);

CREATE UNIQUE INDEX categorias_nome_ativo_idx ON categorias (
    conta_id,
    lower(translate(nome, 'ÁÀÂÃÄÉÈÊËÍÌÎÏÓÒÔÕÖÚÙÛÜÇáàâãäéèêëíìîïóòôõöúùûüç',
        'AAAAAEEEEIIIIOOOOOUUUUCaaaaaeeeeiiiiooooouuuuc'))
) WHERE NOT excluida;
CREATE UNIQUE INDEX produtos_nome_ativo_idx ON produtos (
    conta_id,
    lower(translate(nome, 'ÁÀÂÃÄÉÈÊËÍÌÎÏÓÒÔÕÖÚÙÛÜÇáàâãäéèêëíìîïóòôõöúùûüç',
        'AAAAAEEEEIIIIOOOOOUUUUCaaaaaeeeeiiiiooooouuuuc'))
) WHERE ativo;
CREATE UNIQUE INDEX convites_lista_email_pendente_idx ON convites_lista (lista_id, lower(email))
WHERE estado = 'PENDING';
CREATE INDEX categorias_conta_ordem_idx ON categorias (conta_id, nome, id) WHERE NOT excluida;
CREATE INDEX produtos_conta_ordem_idx ON produtos (conta_id, nome, id);
CREATE INDEX itens_lista_ordem_idx ON itens_lista (lista_id, criado_em, id) WHERE NOT excluido;
CREATE INDEX eventos_lista_ordem_idx ON eventos_lista (lista_id, ocorrido_em, id);

CREATE OR REPLACE FUNCTION criar_categorias_iniciais() RETURNS trigger AS $$
BEGIN
    INSERT INTO categorias(id, conta_id, nome, icone) VALUES
        (gen_random_uuid(), NEW.id, 'Hortifruti', '🥬'),
        (gen_random_uuid(), NEW.id, 'Mercearia', '🛍️'),
        (gen_random_uuid(), NEW.id, 'Bebidas', '🧃'),
        (gen_random_uuid(), NEW.id, 'Limpeza', '🧴');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER contas_categorias_iniciais
AFTER INSERT ON contas FOR EACH ROW EXECUTE FUNCTION criar_categorias_iniciais();

INSERT INTO categorias(id, conta_id, nome, icone)
SELECT gen_random_uuid(), c.id, padrao.nome, padrao.icone
FROM contas c
CROSS JOIN (VALUES
    ('Hortifruti', '🥬'),
    ('Mercearia', '🛍️'),
    ('Bebidas', '🧃'),
    ('Limpeza', '🧴')
) AS padrao(nome, icone)
WHERE NOT EXISTS (SELECT 1 FROM categorias categoria WHERE categoria.conta_id = c.id);
