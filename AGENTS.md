# Geral
Forneça respostas sucintas e objetivas. Somente quando solicitado, dê maiores detalhes.

# Especificações funcionais

As especificações devem ser tão sucintas e objetivas quanto possível, sem perda de informação.
A primeira seção das especificações se chama "Visão geral". Ela deve explicar, com uma frase ou até no máximo um parágrafo, o objetivo da funcionalidade sendo especificada.
A segunda seção se chama "Imagens". Ela deve conter capturas de tela do protótipo, relativas à funcionalidade sendo descrita. As imagens devem ser numeradas sequencialmente (Figura 1, Figura 2,...) e trazer uma legenda da tela sendo exibida.
A terceira seção se chama "Requisitos". Ela deve estar organizada em formato de árvores de componentes, em que a raiz é o nome da tela (e deve referenciar a Figura da seção Imagens sendo descrita), e cada subcomponente é exibido como um subtópico indentado (bullet), listando suas regras de negócio, aparência e comportamento. Caso esse subcomponente também apresente outros subcomponentes, um novo subnível de tópicos deve ser incluído, listando as mesmas definições para cada subcomponente, até que todas as informações necessárias para que um desenvolvedor possa criar a tela estejam presentes. 
A quarta seção ("Contrato de API") deve especificar a API (Endpoints e Schemas), a ser posteriormente implementada com OpenAPI; indique os endpoints e contratos, para que desenvolvedores frontend e backend possam trabalhar de forma independente.
A quinta seção ("Testes de validação") traz definições de testes funcionais caixa-preta a serem implementados posteriormente com Playwright.
As Especificações devem ser incrementais e auto-contidas, não ficarem referenciando outras. O texto os Requisitos deve especificar O QUE fazer, não COMO fazer.
Garanta que todas as validações exibam mensagens de erro polidas ao usuário.

# Arquitetura do frontend
Componentes, diretivas e pipes não devem fazer acesso direto ao servidor. Toda comunicação com o servidor deve ser encapsulada em serviços.

# Arquitetura do backend
Siga as convenções do Spring Boot, separando Controllers, Services e Repositories.
Organize os pacotes por funcionalidade.

# Codificação
Evite pular linhas em métodos. Dê preferência a métodos compactos.
Se a mesma string for necessária em mais de um lugar, use constantes para promover reuso. Isso também vale para testes.
Sempre que necessário criar um componente, use o comando "ng generate component <nomedocomponente>" ou implemente um resultado equivalente (código dividido em html, css, ts e spec.ts).
Evite passar de 120 caracteres por linha. Quebre e indente onde adequado.
Evite duplicação de código. Quando perceber oportunidades de refactoring para promover reuso, me avise.

# Testes automatizados
Implemente somente o teste pedido e garanta que falhe (vermelho). Só implemente o necessário para ficar verde quando solicitado.
Ao implementar a solução para um teste que falha, implemente a mudança mais simples que faça o novo teste passar e mantenha os antigos testes funcionando, nada além.
Se houver mais de uma opção de solução igualmente simples, antes de implementar me ofereça as opções.
Nos testes unitários, não use configurações reais; sempre que houver chamada a outro componente ou serviço, use stubs ou spies.
Após cada ciclo red/green, identifique oportunidades de refatoração para simplificar/remover duplicidades/melhorar a legibilidade, bem como remover métodos ou propriedades que não são mais necessários. Se encontrar, proponha.