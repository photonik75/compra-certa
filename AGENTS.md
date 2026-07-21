# Geral
Forneça respostas sucintas e objetivas. Somente quando solicitado, dê maiores detalhes.

# Arquitetura do frontend
Componentes, diretivas e pipes não devem importar ou injetar `HttpClient`, conhecer URLs de endpoints ou interpretar contratos HTTP. Toda comunicação com o servidor deve ser encapsulada em um serviço injetável da funcionalidade.
Nos testes unitários de componentes, substitua esses serviços por stubs, fakes ou spies. Não use `HttpTestingController`, `provideHttpClient()` ou `provideHttpClientTesting()` em testes unitários de componentes. Esses recursos pertencem aos testes dos serviços HTTP, interceptors e testes explícitos de integração.
Crie somente a menor API de serviço exigida pelo caso de uso atual. Não antecipe serviços, métodos ou abstrações para funcionalidades futuras.

# Codificação
Evite pular linhas em métodos. Dê preferência a métodos compactos.
Se a mesma string for necessária em mais de um lugar, use constantes para promover reuso. Isso também vale para testes.
Sempre que necessário criar um componente, use o comando "ng generate component <nomedocomponente>" ou implementar um resultado equivalente (código dividido em html, css, ts e spec.ts).

# Testes automatizados
Implemente somente o teste pedido e garanta que falhe (vermelho). Só implemente o necessário para ficar verde quando solicitado.
Ao implementar a solução para um teste que falha, implemente a mudança mais simples que faça o novo teste passar e mantenha os antigos testes funcionando, nada além.
Se houver mais de uma opção de solução igualmente simples, antes de implementar me ofereça as opções.