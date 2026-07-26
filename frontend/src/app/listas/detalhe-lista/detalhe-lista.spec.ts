import { TestBed } from '@angular/core/testing';
import { DetalheLista } from './detalhe-lista';

describe('DetalheLista', () => {
  it('FE-ITEM-01 - Renderiza grupos, itens e ações de gestão.', async () => {
    await TestBed.configureTestingModule({ imports: [DetalheLista] }).compileComponents();
    const fixture = TestBed.createComponent(DetalheLista);
    fixture.detectChanges();
    const texto = fixture.nativeElement.textContent;
    expect(texto).toContain('Detalhe da lista');
    expect(texto).toContain('Adicionar item');
    expect(texto).toContain('Categoria');
    expect(texto).toContain('Quantidade');
    expect(texto).toContain('Unidade');
    expect(texto).toContain('Observação');
  });

  it('FE-SHOP-01 - Renderiza o progresso e o resumo da compra.', async () => {
    await TestBed.configureTestingModule({ imports: [DetalheLista] }).compileComponents();
    const fixture = TestBed.createComponent(DetalheLista);
    fixture.detectChanges();
    const texto = fixture.nativeElement.textContent;
    expect(texto).toContain('Progresso da compra');
    expect(texto).toContain('Total');
    expect(texto).toContain('Comprados');
    expect(texto).toContain('Pendentes');
  });
});
