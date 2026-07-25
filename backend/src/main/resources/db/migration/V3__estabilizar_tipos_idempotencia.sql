UPDATE idempotencias
SET resultado_tipo = 'BOOLEAN'
WHERE resultado_tipo = 'java.lang.Boolean';

UPDATE idempotencias
SET resultado_tipo = 'SESSAO_CRIADA'
WHERE resultado_tipo = 'br.leobarros.compracerta.autenticacao.sessao.SessaoCriada';
