import { render } from '@testing-library/angular';
import { describe, expect, it } from 'vitest';

import { CadastroComponent } from '../../../../../../../src/br/leobarros/compracerta/autenticacao/cadastro/cadastro.component';

describe('br.leobarros.compracerta.autenticacao.cadastro.CadastroComponent', () => {
  it('renderiza os elementos necessários para criar uma conta', async () => {
    const tela = await render(CadastroComponent);

    expect(
      tela.getByRole('heading', { level: 1, name: 'Crie sua conta' }),
    ).toBeInTheDocument();
    expect(tela.getByRole('textbox', { name: 'Nome' })).toBeInTheDocument();
    expect(tela.getByRole('textbox', { name: 'E-mail' })).toBeInTheDocument();
    expect(tela.getByLabelText('Senha')).toBeInTheDocument();
    expect(tela.getByLabelText('Confirmar senha')).toBeInTheDocument();
    expect(
      tela.getAllByRole('button', { name: /mostrar/i }),
    ).toHaveLength(2);
    expect(
      tela.getByRole('button', { name: 'Criar conta' }),
    ).toBeInTheDocument();
    expect(tela.getByRole('link', { name: 'Entrar' })).toBeInTheDocument();
  });
});
