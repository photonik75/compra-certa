import { fireEvent, render, screen } from '@testing-library/angular';
import { Cadastro } from './cadastro';

describe('Testes unitários do componente Cadastro', () => {
  it('CAD-1 - renderiza todos os campos e controles do cadastro', async () => {
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

  it('CAD-2 - rejeita nome vazio ou composto somente por espaços', async () => {
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

  it('CAD-3 - aceita nome de 2 a 100 caracteres', async () => {
    await render(Cadastro);
    const nome = screen.getByRole('textbox', { name: 'Nome' }) as HTMLInputElement;
    fireEvent.input(nome, { target: { value: 'A' } });
    expect(nome.checkValidity()).toBe(false);
    fireEvent.input(nome, { target: { value: 'Al' } });
    expect(nome.checkValidity()).toBe(true);
    fireEvent.input(nome, { target: { value: 'A'.repeat(100) } });
    expect(nome.checkValidity()).toBe(true);
    fireEvent.input(nome, { target: { value: 'A'.repeat(101) } });
    expect(nome.checkValidity()).toBe(false);
  });

  it('CAD-4 - normaliza espaços sem modificar a capitalização do nome', async () => {
    await render(Cadastro);
    const nome = screen.getByRole('textbox', { name: 'Nome' }) as HTMLInputElement;
    fireEvent.input(nome, { target: { value: '  mARIA   sILVA  ' } });
    fireEvent.click(screen.getByRole('button', { name: 'Criar conta' }));
    expect(nome.value).toBe('mARIA sILVA');
  });

  it('CAD-5 - rejeita e-mail vazio ou composto somente por espaços', async () => {
    await render(Cadastro);
    const email = screen.getByRole('textbox', { name: 'E-mail' }) as HTMLInputElement;
    const criarConta = screen.getByRole('button', { name: 'Criar conta' });
    fireEvent.click(criarConta);
    expect(email.getAttribute('aria-invalid')).toBe('true');
    fireEvent.input(email, { target: { value: 'maria@example.com' } });
    expect(email.getAttribute('aria-invalid')).toBe('false');
    fireEvent.input(email, { target: { value: '   ' } });
    fireEvent.click(criarConta);
    expect(email.getAttribute('aria-invalid')).toBe('true');
  });
});
