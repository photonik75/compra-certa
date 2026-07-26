import { TestBed } from '@angular/core/testing';
import { Categorias } from './categorias';

describe('Categorias', () => {
  it('FE-CAT-01 - Renderiza os controles e dados principais.', async () => {
    await TestBed.configureTestingModule({ imports: [Categorias] }).compileComponents();
    const fixture = TestBed.createComponent(Categorias);
    fixture.detectChanges();
    const texto = fixture.nativeElement.textContent;
    expect(texto).toContain('Categorias');
    expect(texto).toContain('Pesquisar categorias');
    expect(texto).toContain('Nova categoria');
    expect(texto).toContain('Nome');
    expect(texto).toContain('Produtos ativos');
  });
});
