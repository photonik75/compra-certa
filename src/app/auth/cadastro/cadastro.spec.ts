import { fireEvent, render, screen } from '@testing-library/angular';
import { Cadastro } from './cadastro';

describe('Testes unitários do componente Cadastro', () => {
  it('REG-UI-001 - renderiza todos os campos e controles do cadastro', async () => {
    await render(Cadastro);

    expect(
      screen.getByRole('heading', { name: 'Crie sua conta' }),
    ).toBeTruthy();
    expect(screen.getByRole('textbox', { name: 'Nome' })).toBeTruthy();
    expect(screen.getByRole('textbox', { name: 'E-mail' })).toBeTruthy();
    expect((screen.getByLabelText('Senha') as HTMLInputElement).type).toBe(
      'password',
    );
    expect(
      (screen.getByLabelText('Confirmar senha') as HTMLInputElement).type,
    ).toBe('password');
    expect(
      screen.getAllByRole('button', { name: /^(mostrar|ocultar)$/i }),
    ).toHaveLength(2);
    expect(
      screen.getByRole('button', { name: 'Criar conta' }),
    ).toBeTruthy();
    expect(screen.getByRole('link', { name: 'Entrar' })).toBeTruthy();
  });

  it('REG-VAL-001 - rejeita nome vazio ou composto somente por espaços', async () => {
    await render(Cadastro);
    const nome = screen.getByRole('textbox', { name: 'Nome'}) as HTMLInputElement;
    const criarConta = screen.getByRole('button', { name: 'Criar conta' });
    fireEvent.click(criarConta);
    expect(screen.getByRole('alert').textContent).toBe('Por favor, informe seu nome');
    fireEvent.input(nome, { target: { value: 'Maria' } });
    expect(screen.queryByRole('alert')).toBeNull();
    fireEvent.input(nome, { target: { value: '   ' } });
    fireEvent.click(criarConta);
    expect(screen.getByRole('alert').textContent).toBe('Por favor, informe seu nome');
  });
});
