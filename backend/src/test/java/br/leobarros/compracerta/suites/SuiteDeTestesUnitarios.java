package br.leobarros.compracerta.suites;

import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectPackages({
		"br.leobarros.compracerta.unit",
		"br.leobarros.compracerta.listas",
		"br.leobarros.compracerta.categorias"
})
public class SuiteDeTestesUnitarios {
}
