import { Component, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { WorkspaceMemberService } from '../../core/services/workspace-member.service';
import { WorkspaceInvitationService } from '../../core/services/workspace-invitation.service';
import { BillingService } from '../../core/services/billing.service';
import { AuthService } from '../../core/services/auth.service';
import { WorkspaceMember } from '../../core/models/workspace-member.model';
import { WorkspaceInvitation } from '../../core/models/workspace-invitation.model';
import { SeatsSummary } from '../../core/models/seats-summary.model';
import { fadeInUp } from '../../shared/animations';
import { DisabledIfPendingPaymentDirective } from '../../shared/directives/disabled-if-pending-payment.directive';
import {
  InviteMemberDialogComponent, InviteMemberDialogResult
} from './invite-member-dialog.component';
import {
  ConfirmRemoveMemberDialogComponent
} from './confirm-remove-member-dialog.component';

@Component({
  selector: 'app-workspace-members',
  standalone: true,
  imports: [
    DatePipe,
    MatTableModule, MatButtonModule, MatIconModule,
    MatProgressSpinnerModule,
    DisabledIfPendingPaymentDirective
  ],
  templateUrl: './workspace-members.component.html',
  styleUrl: './workspace-members.component.scss',
  animations: [fadeInUp],
  host: { '[@fadeInUp]': '' },
})
export class WorkspaceMembersComponent implements OnInit {
  members: WorkspaceMember[] = [];
  invitations: WorkspaceInvitation[] = [];
  memberColumns = ['name', 'email', 'role', 'joinedAt', 'actions'];
  invitationColumns = ['email', 'role', 'expiresAt', 'actions'];
  loading = false;

  readonly seatsSummary = signal<SeatsSummary | null>(null);

  constructor(
    private memberService: WorkspaceMemberService,
    private invitationService: WorkspaceInvitationService,
    private billingService: BillingService,
    readonly auth: AuthService,
    private snackBar: MatSnackBar,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    this.loadAll();
  }

  loadAll(): void {
    this.loading = true;
    this.memberService.getMembers().subscribe({
      next: members => {
        this.members = members;
        this.loading = false;
      },
      error: () => {
        this.snackBar.open('Erreur lors du chargement des membres', 'Fermer', {
          duration: 4000, panelClass: ['snack-error']
        });
        this.loading = false;
      }
    });

    this.invitationService.getInvitations().subscribe({
      next: invitations => this.invitations = invitations,
      error: () => {}
    });

    this.loadSeatsSummary();
  }

  loadSeatsSummary(): void {
    this.billingService.getSeatsSummary().subscribe({
      next: s => this.seatsSummary.set(s),
      error: () => this.seatsSummary.set(null)
    });
  }

  openInviteDialog(): void {
    const ref = this.dialog.open<InviteMemberDialogComponent, { summary: SeatsSummary | null }, InviteMemberDialogResult>(
      InviteMemberDialogComponent,
      { data: { summary: this.seatsSummary() }, autoFocus: 'dialog' }
    );

    ref.afterClosed().subscribe(result => {
      if (!result) return;
      this.invitationService.createInvitation(result.email, result.role).subscribe({
        next: () => {
          this.snackBar.open(`Invitation envoyée à ${result.email}`, 'Fermer', {
            duration: 4000, panelClass: ['snack-success']
          });
          this.invitationService.getInvitations().subscribe(inv => this.invitations = inv);
          this.loadSeatsSummary();
        },
        error: err => {
          const msg = err.status === 402
            ? (err.error?.message ?? 'Limite d\'utilisateurs atteinte pour votre plan.')
            : err.status === 409
              ? 'Une invitation est déjà en attente pour cet email'
              : 'Erreur lors de l\'envoi de l\'invitation';
          this.snackBar.open(msg, 'Fermer', { duration: 6000, panelClass: ['snack-error'] });
        }
      });
    });
  }

  removeMember(member: WorkspaceMember): void {
    const s = this.seatsSummary();
    const currentSeats = s?.seatCount ?? this.members.length;
    const nextSeats = Math.max(1, currentSeats - 1);
    const extraNow = s ? Math.max(0, currentSeats - s.includedSeats) : 0;
    const extraAfter = s ? Math.max(0, nextSeats - s.includedSeats) : 0;
    const baseEuros = s ? s.baseMonthlyCostCents / 100 : 0;
    const extraEuros = s ? s.extraSeatPriceCents / 100 : 0;
    const currentCost = Math.round(baseEuros + extraNow * extraEuros);
    const newCost = Math.round(baseEuros + extraAfter * extraEuros);

    const ref = this.dialog.open<ConfirmRemoveMemberDialogComponent,
      ReturnType<typeof this.buildRemoveData>, boolean>(
      ConfirmRemoveMemberDialogComponent,
      { data: this.buildRemoveData(member, currentCost, newCost) }
    );

    ref.afterClosed().subscribe(confirmed => {
      if (!confirmed) return;
      this.memberService.removeMember(member.userId).subscribe({
        next: () => {
          this.members = this.members.filter(m => m.userId !== member.userId);
          this.snackBar.open('Membre retiré', 'Fermer', {
            duration: 4000, panelClass: ['snack-success']
          });
          this.loadSeatsSummary();
        },
        error: () => this.snackBar.open('Erreur lors du retrait du membre', 'Fermer', {
          duration: 4000, panelClass: ['snack-error']
        })
      });
    });
  }

  private buildRemoveData(member: WorkspaceMember, currentCost: number, newCost: number) {
    return {
      memberName: this.memberName(member),
      currentCostEuros: currentCost,
      newCostEuros: newCost,
      currency: '€',
      savesCost: currentCost !== newCost
    };
  }

  revokeInvitation(invitation: WorkspaceInvitation): void {
    this.invitationService.revokeInvitation(invitation.id).subscribe({
      next: () => {
        this.invitations = this.invitations.filter(i => i.id !== invitation.id);
        this.snackBar.open('Invitation révoquée', 'Fermer', {
          duration: 4000, panelClass: ['snack-success']
        });
      },
      error: () => this.snackBar.open('Erreur lors de la révocation', 'Fermer', {
        duration: 4000, panelClass: ['snack-error']
      })
    });
  }

  memberName(m: WorkspaceMember): string {
    const full = [m.firstName, m.lastName].filter(Boolean).join(' ');
    return full || m.email;
  }

  roleLabel(role: string): string {
    const labels: Record<string, string> = {
      OWNER: 'Propriétaire', ADMIN: 'Admin', LAWYER: 'Avocat', MEMBER: 'Membre'
    };
    return labels[role] ?? role;
  }

  isCurrentUser(member: WorkspaceMember): boolean {
    return member.email === this.auth.currentUser()?.email;
  }

  roleBadgeClass(role: string): string {
    const classes: Record<string, string> = {
      OWNER: 'badge--owner', ADMIN: 'badge--admin',
      LAWYER: 'badge--lawyer', MEMBER: 'badge--member'
    };
    return classes[role] ?? 'badge--member';
  }

  planLabel(plan: string): string {
    const l: Record<string, string> = { FREE: 'Free', SOLO: 'Solo', TEAM: 'Team', PRO: 'Pro' };
    return l[plan] ?? plan;
  }

  seatsLabel(s: SeatsSummary): string {
    if (s.maxSeats === Number.MAX_SAFE_INTEGER || s.maxSeats > 1000) {
      return `${s.seatCount} utilisateurs (illimité)`;
    }
    return `${s.seatCount} / ${s.maxSeats} utilisateurs`;
  }

  totalEuros(s: SeatsSummary): number {
    return Math.round(s.totalMonthlyCostCents / 100);
  }
}
