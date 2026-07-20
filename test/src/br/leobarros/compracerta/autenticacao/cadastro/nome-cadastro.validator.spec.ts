import { FormControl } from '@angular/forms';
import { describe, expect, it } from 'vitest';

import { nomeCadastroValidator } from '../../../../../../../src/br/leobarros/compracerta/autenticacao/cadastro/nome-cadastro.validator';

describe('nome vazio ou composto somente por espaços é inválido', () => {
  it.each(['', ' ', '   ', '\t', '\n'])(
    'considera o nome %j obrigatório',
    (nome) => {
      const controle = new FormControl(nome);

      expect(nomeCadastroValidator(controle)).toEqual({ required: true });
    },
  );
});
