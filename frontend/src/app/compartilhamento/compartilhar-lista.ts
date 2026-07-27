import { ChangeDetectorRef, Component, HostListener, OnInit, inject } from '@angular/core';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize, forkJoin } from 'rxjs';
import { CompartilhamentoService } from './compartilhamento.service';
import { ListasService } from '../listas/listas.service';
import { SessaoService } from '../auth/sessao.service';

@Component({
  selector: 'app-compartilhar-lista',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './compartilhar-lista.html',
  styleUrl: './compartilhar-lista.css',
})
export class CompartilharLista implements OnInit {
  private readonly service = inject(CompartilhamentoService);
  private readonly listsService = inject(ListasService);
  private readonly sessions = inject(SessaoService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly changeDetector = inject(ChangeDetectorRef);
  readonly listId = this.route.snapshot.paramMap.get('listId')!;
  readonly email = new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.email] });
  access: any;
  selectedMember: any;
  leaving = false;
  currentUserId = '';
  submitted = false;
  sending = false;
  notice = '';

  ngOnInit(): void { this.load(); }

  convidar(): void {
    if (this.sending) return;
    this.submitted = true;
    const email = this.email.value.trim().toLowerCase();
    this.email.setValue(email);
    if (this.email.invalid) { this.changeDetector.markForCheck(); return; }
    this.sending = true;
    this.service.convidar(this.listId, email).pipe(finalize(() => this.sending = false)).subscribe({
      next: (result) => {
        this.notice = result.outcome === 'MEMBER_ADDED'
          ? 'Participante adicionado com sucesso.' : 'Convite enviado com sucesso.';
        this.load();
      },
      error: (response) => {
        const messages: Record<string, string> = {
          OWNER_EMAIL: 'O proprietário já possui acesso à lista.',
          CANNOT_INVITE_OWNER: 'O proprietário já possui acesso à lista.',
          ALREADY_MEMBER: 'Esta pessoa já participa da lista.',
          INVITATION_PENDING: 'Já existe um convite pendente para este e-mail.',
          INVITATION_ALREADY_PENDING: 'Já existe um convite pendente para este e-mail.',
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
    this.service.removerMembro(this.listId, member.user.id, member.version).subscribe({
      next: () => {
        this.access.members = this.access.members.filter((item: any) => item.user.id !== member.user.id);
        this.selectedMember = undefined;
        this.notice = 'Participante removido com sucesso.';
        this.changeDetector.markForCheck();
      },
      error: () => { this.notice = 'Não foi possível remover o participante.'; this.changeDetector.markForCheck(); },
    });
  }

  requestLeave(): void { this.leaving = true; this.changeDetector.markForCheck(); }
  cancelLeave(): void { this.leaving = false; this.changeDetector.markForCheck(); }
  leave(): void {
    const membership = this.access.members.find((member: any) => member.user.id === this.currentUserId);
    if (!membership) return;
    this.service.sair(this.listId, membership.version).subscribe({
      next: () => this.router.navigate(['/listas']),
      error: () => {
        this.notice = 'Não foi possível sair da lista. Tente novamente.';
        this.changeDetector.markForCheck();
      },
    });
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.selectedMember) this.cancelRemove();
    if (this.leaving) this.cancelLeave();
  }

  private load(): void {
    forkJoin({
      access: this.service.consultarAcesso(this.listId),
      list: this.listsService.obter(this.listId),
      session: this.sessions.consultar(),
    }).subscribe({
      next: ({ access, list, session }) => {
        this.access = { ...access, list };
        this.currentUserId = session.user.id;
        this.changeDetector.markForCheck();
      },
      error: () => {
        this.notice = 'Lista não encontrada ou indisponível para sua conta.';
        this.changeDetector.markForCheck();
      },
    });
  }
}
