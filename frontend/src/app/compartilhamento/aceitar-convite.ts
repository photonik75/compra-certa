import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { CompartilhamentoService } from './compartilhamento.service';

@Component({
  selector: 'app-aceitar-convite',
  imports: [RouterLink],
  templateUrl: './aceitar-convite.html',
  styleUrl: './aceitar-convite.css',
})
export class AceitarConvite implements OnInit {
  private readonly service = inject(CompartilhamentoService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly changeDetector = inject(ChangeDetectorRef);
  private readonly token = this.route.snapshot.fragment ?? '';
  preview: any;
  sending = false;
  notice = '';

  ngOnInit(): void {
    this.service.preview(this.token).subscribe({
      next: (preview) => { this.preview = preview; this.changeDetector.markForCheck(); },
      error: (response) => {
        const messages: Record<string, string> = {
          INVITATION_EXPIRED: 'Este convite expirou.',
          INVITATION_CANCELLED: 'Este convite foi cancelado.',
          INVITATION_USED: 'Este convite já foi utilizado.',
          LIST_COMPLETED: 'Esta lista está concluída e não aceita novos participantes.',
        };
        this.notice = messages[response?.error?.code] ?? 'Convite inválido ou indisponível.';
        this.changeDetector.markForCheck();
      },
    });
  }

  accept(): void {
    if (this.sending || !this.preview) return;
    this.sending = true;
    this.service.aceitar(this.token).pipe(finalize(() => this.sending = false)).subscribe({
      next: (result) => this.router.navigate(['/listas', result.listId]),
      error: (response) => {
        this.notice = response?.error?.code === 'EMAIL_MISMATCH'
          ? 'Entre com o e-mail para o qual este convite foi enviado.'
          : 'Não foi possível aceitar o convite.';
        this.changeDetector.markForCheck();
      },
    });
  }
}
