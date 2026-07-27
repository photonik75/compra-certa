import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { LayoutInterno } from './layout-interno';

describe('LayoutInterno', () => {
  let fixture: ComponentFixture<LayoutInterno>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LayoutInterno],
      providers: [provideRouter([])],
    }).compileComponents();
    fixture = TestBed.createComponent(LayoutInterno);
    fixture.detectChanges();
  });

  it('exibe o painel lateral e a área destinada à página funcional', () => {
    expect(fixture.nativeElement.querySelector('.apresentacao')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('router-outlet')).not.toBeNull();
  });
});
