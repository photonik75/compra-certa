import { TestBed } from '@angular/core/testing';
import { ConcluirLista } from './concluir-lista';

describe('ConcluirLista', () => {
  it('FE-LIFE-01 - Renderiza a confirmação e a quantidade pendente.', async () => {
    await TestBed.configureTestingModule({ imports: [ConcluirLista] }).compileComponents();
    const fixture = TestBed.createComponent(ConcluirLista);
    fixture.detectChanges();
    const texto = fixture.nativeElement.textContent;
    expect(texto).toContain('Concluir lista');
    expect(texto).toContain('itens pendentes');
    expect(texto).toContain('somente para consulta');
    expect(texto).toContain('Concluir');
    expect(texto).toContain('Cancelar');
  });
});
