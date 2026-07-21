import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MinhasListas } from './minhas-listas';

describe('MinhasListas', () => {
  let component: MinhasListas;
  let fixture: ComponentFixture<MinhasListas>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [MinhasListas] }).compileComponents();
    fixture = TestBed.createComponent(MinhasListas);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
