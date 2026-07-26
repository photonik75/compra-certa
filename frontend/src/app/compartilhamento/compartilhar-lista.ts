import { ChangeDetectorRef, Component, HostListener, OnInit, inject } from '@angular/core';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { finalize } from 'rxjs';
import { CompartilhamentoService } from './compartilhamento.service';

@Component({
  selector: 'app-compartilhar-lista',
  imports: [ReactiveFormsModule],
  templateUrl: './compartilhar-lista.html',
  styleUrl: './compartilhar-lista.css',
})
export class CompartilharLista implements OnInit {
  private readonly service = inject(CompartilhamentoService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly changeDetector = inject(ChangeDetectorRef);
  readonly listId = this.route.snapshot.paramMap.get('listId')!;
  readonly email = new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.email] });
  access: any;
  selectedMember: any;
  submitted = false;
  sending = false;
  notice = '';

  ngOnInit(): void { this.load(); }

  invite(): void {
    if (this.sending) return;
    this.submitted = true;
    const email = this.email.value.trim().toLowerCase();
    this.email.setValue(email);
    if (this.email.invalid) { this.changeDetector.markForCheck(); return; }
    this.sending = true;
    this.service.convidar(this.listId, email).pipe(finalize(() => this.sending = false)).subscribe({
      next: (result) => {
        if (result.invitation) this.access.invitations = [...this.access.invitations, result.invitation];
        this.notice = result.outcome === 'MEMBER_ADDED'
          ? 'Participante adicionado com sucesso.' : 'Convite enviado com sucesso.';
        this.changeDetector.markForCheck();
      },
      error: (response) => {
        const messages: Record<string, string> = {
          OWNER_EMAIL: 'O proprietário já possui acesso à lista.',
          ALREADY_MEMBER: 'Esta pessoa já participa da lista.',
          INVITATION_PENDING: 'Já existe um convite pendente para este e-mail.',
        };
        this.notice = messages[response?.error?.code]
          ?? 'Não foi possível compartilhar a lista. Tente novamente em alguns instantes.';
        this.changeDetector.markForCheck();
      },
    });
  }

  resend(invitation: any): void {
    this.service.reenviar(this.listId, invitation.id, invitation.version).subscribe({
      next: (updated) => {
        this.access.invitations = this.access.invitations.map((item: any) =>
          item.id === updated.id ? updated : item);
        this.notice = 'Convite reenviado com sucesso.';
        this.changeDetector.markForCheck();
      },
    });
  }

  cancelInvitation(invitation: any): void {
    this.service.cancelarConvite(this.listId, invitation.id, invitation.version).subscribe({
      next: () => {
        this.access.invitations = this.access.invitations.filter((item: any) => item.id !== invitation.id);
        this.changeDetector.markForCheck();
      },
    });
  }

  requestRemove(member: any): void { this.selectedMember = member; this.changeDetector.markForCheck(); }
  cancelRemove(): void { this.selectedMember = undefined; this.changeDetector.markForCheck(); }
  confirmRemove(): void {
    if (!this.selectedMember) return;
    const member = this.selectedMember;
    this.service.removerMembro(this.listId, member.id, this.access.list.version).subscribe({
      next: () => {
        this.access.members = this.access.members.filter((item: any) => item.id !== member.id);
        this.selectedMember = undefined;
        this.notice = 'Participante removido com sucesso.';
        this.changeDetector.markForCheck();
      },
      error: () => { this.notice = 'Não foi possível remover o participante.'; this.changeDetector.markForCheck(); },
    });
  }

  leave(): void {
    this.service.sair(this.listId, this.access.list.version).subscribe({
      next: () => this.router.navigate(['/listas']),
    });
  }

  @HostListener('document:keydown.escape')
  onEscape(): void { if (this.selectedMember) this.cancelRemove(); }

  private load(): void {
    this.service.consultarAcesso(this.listId).subscribe({
      next: (access) => { this.access = access; this.changeDetector.markForCheck(); },
      error: () => this.notice = 'Lista não encontrada ou indisponível para sua conta.',
    });
  }
}
