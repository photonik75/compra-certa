import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of } from 'rxjs';
import { SessaoService } from '../auth/sessao.service';
import { MinhasListas } from './minhas-listas';

describe('MinhasListas', () => {
  let component: MinhasListas;
  let fixture: ComponentFixture<MinhasListas>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MinhasListas],
      providers: [
        { provide: SessaoService, useValue: { sair: () => of(undefined) } },
        { provide: Router, useValue: { navigateByUrl: () => Promise.resolve(true) } },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(MinhasListas);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
