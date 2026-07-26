import { TestBed } from '@angular/core/testing';
import { CompartilharLista } from './compartilhar-lista';

describe('CompartilharLista', () => {
  it('FE-SHARE-01 - Renderiza pessoas, convites e ações conforme o papel.', async () => {
    await TestBed.configureTestingModule({ imports: [CompartilharLista] }).compileComponents();
    const fixture = TestBed.createComponent(CompartilharLista);
    fixture.detectChanges();
    const texto = fixture.nativeElement.textContent;
    expect(texto).toContain('Compartilhar lista');
    expect(texto).toContain('Convidar participante');
    expect(texto).toContain('Pessoas com acesso');
    expect(texto).toContain('Proprietário');
    expect(texto).toContain('Participante');
    expect(texto).toContain('Convites pendentes');
  });
});
