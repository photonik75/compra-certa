package br.leobarros.compracerta.autenticacao.erro;

import java.util.List;

public record ApiError(String code, String detail, List<ApiFieldError> fieldErrors) {
}
