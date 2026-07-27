import {
  ChangeDetectorRef,
  Component,
  ElementRef,
  HostListener,
  OnDestroy,
  OnInit,
  ViewChild,
  inject,
} from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { finalize } from 'rxjs';
import { SessaoService } from '../auth/sessao.service';
import { UserSummary } from '../auth/models/user-summary';

const LOGIN_ROUTE = '/entrar';
const LOGOUT_ERROR = 'Não foi possível sair da sua conta. Verifique sua conexão e tente novamente.';

@Component({
  selector: 'app-layout-interno',
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './layout-interno.html',
  styleUrl: './layout-interno.css',
})
export class LayoutInterno implements OnInit, OnDestroy {
  private readonly session = inject(SessaoService);
  private readonly router = inject(Router);
  private readonly changeDetector = inject(ChangeDetectorRef);
  @ViewChild('menuToggle') private menuToggle?: ElementRef<HTMLButtonElement>;
  @ViewChild('menuPanel') private menuPanel?: ElementRef<HTMLElement>;
  user?: UserSummary;
  menuOpen = false;
  signingOut = false;
  error = '';

  ngOnInit(): void {
    this.session.consultar().subscribe({
      next: ({ user }) => {
        this.user = user;
        this.changeDetector.markForCheck();
      },
      error: () => this.router.navigateByUrl(LOGIN_ROUTE),
    });
  }

  ngOnDestroy(): void {
    document.body.style.overflow = '';
  }

  get initials(): string {
    return this.user?.name
      .trim()
      .split(/\s+/)
      .slice(0, 2)
      .map((part) => part.charAt(0).toLocaleUpperCase('pt-BR'))
      .join('') ?? '';
  }

  toggleMenu(): void {
    this.menuOpen ? this.closeMenu() : this.openMenu();
  }

  openMenu(): void {
    this.menuOpen = true;
    document.body.style.overflow = 'hidden';
    this.changeDetector.detectChanges();
    this.focusFirstMenuControl();
  }

  closeMenu(): void {
    if (!this.menuOpen) return;
    this.menuOpen = false;
    document.body.style.overflow = '';
    this.changeDetector.detectChanges();
    this.menuToggle?.nativeElement.focus();
  }

  navigate(): void {
    if (this.menuOpen) this.closeMenu();
  }

  logout(): void {
    if (this.signingOut) return;
    this.error = '';
    this.signingOut = true;
    this.session.sair().pipe(finalize(() => {
      this.signingOut = false;
      this.changeDetector.markForCheck();
    })).subscribe({
      next: () => this.router.navigateByUrl(LOGIN_ROUTE),
      error: () => {
        this.error = LOGOUT_ERROR;
        this.changeDetector.markForCheck();
      },
    });
  }

  @HostListener('document:keydown', ['$event'])
  onKeydown(event: KeyboardEvent): void {
    if (!this.menuOpen) return;
    if (event.key === 'Escape') {
      event.preventDefault();
      this.closeMenu();
      return;
    }
    if (event.key === 'Tab') this.trapFocus(event);
  }

  private focusFirstMenuControl(): void {
    this.menuPanel?.nativeElement.querySelector<HTMLElement>('a, button')?.focus();
  }

  private trapFocus(event: KeyboardEvent): void {
    const controls = [...this.menuPanel?.nativeElement.querySelectorAll<HTMLElement>('a, button') ?? []]
      .filter((element) => !element.hasAttribute('disabled'));
    if (!controls.length) return;
    const first = controls[0];
    const last = controls[controls.length - 1];
    if (event.shiftKey && document.activeElement === first) {
      event.preventDefault();
      last.focus();
    } else if (!event.shiftKey && document.activeElement === last) {
      event.preventDefault();
      first.focus();
    }
  }
}
