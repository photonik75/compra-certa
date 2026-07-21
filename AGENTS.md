# Geral
Forneça respostas sucintas e objetivas. Somente quando solicitado, dê maiores detalhes.

# Arquitetura do frontend
Componentes, diretivas e pipes não devem fazer acesso direto ao servidor. Toda comunicação com o servidor deve ser encapsulada em serviços.

# Codificação
Evite pular linhas em métodos. Dê preferência a métodos compactos.
Se a mesma string for necessária em mais de um lugar, use constantes para promover reuso. Isso também vale para testes.
Sempre que necessário criar um componente, use o comando "ng generate component <nomedocomponente>" ou implementar um resultado equivalente (código dividido em html, css, ts e spec.ts).
Evite passar de 120 caracteres por linha. Quebre e indente onde adequado.

# Testes automatizados
Implemente somente o teste pedido e garanta que falhe (vermelho). Só implemente o necessário para ficar verde quando solicitado.
Ao implementar a solução para um teste que falha, implemente a mudança mais simples que faça o novo teste passar e mantenha os antigos testes funcionando, nada além.
Se houver mais de uma opção de solução igualmente simples, antes de implementar me ofereça as opções.
Nos testes unitários, não use configurações reais; sempre que houver chamada a outro componente ou serviço, use stubs ou spies.