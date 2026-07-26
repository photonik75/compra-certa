import { TestBed } from '@angular/core/testing';
import { Produtos } from './produtos';

describe('Produtos', () => {
  it('FE-PROD-01 - Renderiza os controles e dados principais.', async () => {
    await TestBed.configureTestingModule({ imports: [Produtos] }).compileComponents();
    const fixture = TestBed.createComponent(Produtos);
    fixture.detectChanges();
    const texto = fixture.nativeElement.textContent;
    expect(texto).toContain('Produtos');
    expect(texto).toContain('Pesquisar produtos');
    expect(texto).toContain('Todas as categorias');
    expect(texto).toContain('Novo produto');
    expect(texto).toContain('Histórico preservado');
  });
});
