import { Component, OnInit } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { WorkspaceMemberService } from '../../core/services/workspace-member.service';
import { WorkspaceInvitationService } from '../../core/services/workspace-invitation.service';
import { AuthService } from '../../core/services/auth.service';
import { WorkspaceMember } from '../../core/models/workspace-member.model';
import { WorkspaceInvitation } from '../../core/models/workspace-invitation.model';

@Component({
  selector: 'app-workspace-members',
  standalone: true,
  imports: [
    DatePipe, ReactiveFormsModule,
    MatTableModule, MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './workspace-members.component.html',
  styleUrl: './workspace-members.component.scss'
})
export class WorkspaceMembersComponent implements OnInit {
  members: WorkspaceMember[] = [];
  invitations: WorkspaceInvitation[] = [];
  memberColumns = ['name', 'email', 'role', 'joinedAt', 'actions'];
  invitationColumns = ['email', 'role', 'expiresAt', 'actions'];
  loading = false;

  inviteForm: FormGroup;
  inviting = false;

  constructor(
    private memberService: WorkspaceMemberService,
    private invitationService: WorkspaceInvitationService,
    readonly auth: AuthService,
    private snackBar: MatSnackBar,
    private fb: FormBuilder
  ) {
    this.inviteForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      role: ['LAWYER', Validators.required]
    });
  }

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
  }

  sendInvitation(): void {
    if (this.inviteForm.invalid) return;
    this.inviting = true;
    const { email, role } = this.inviteForm.value;

    this.invitationService.createInvitation(email, role).subscribe({
      next: () => {
        this.snackBar.open(`Invitation envoyée à ${email}`, 'Fermer', {
          duration: 4000, panelClass: ['snack-success']
        });
        this.inviteForm.reset({ role: 'LAWYER' });
        this.inviting = false;
        this.invitationService.getInvitations().subscribe(inv => this.invitations = inv);
      },
      error: err => {
        const msg = err.status === 409
          ? 'Une invitation est déjà en attente pour cet email'
          : 'Erreur lors de l\'envoi de l\'invitation';
        this.snackBar.open(msg, 'Fermer', { duration: 4000, panelClass: ['snack-error'] });
        this.inviting = false;
      }
    });
  }

  removeMember(member: WorkspaceMember): void {
    this.memberService.removeMember(member.userId).subscribe({
      next: () => {
        this.members = this.members.filter(m => m.userId !== member.userId);
        this.snackBar.open('Membre retiré', 'Fermer', {
          duration: 4000, panelClass: ['snack-success']
        });
      },
      error: () => this.snackBar.open('Erreur lors du retrait du membre', 'Fermer', {
        duration: 4000, panelClass: ['snack-error']
      })
    });
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
}
