package br.leobarros.compracerta.suites;

import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectPackages({
		"br.leobarros.compracerta.unit",
		"br.leobarros.compracerta.listas",
		"br.leobarros.compracerta.categorias",
		"br.leobarros.compracerta.produtos",
		"br.leobarros.compracerta.itens",
		"br.leobarros.compracerta.ciclodevida",
		"br.leobarros.compracerta.compartilhamento",
		"br.leobarros.compracerta.eventos"
})
public class SuiteDeTestesUnitarios {
}
