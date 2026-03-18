import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatButtonModule } from '@angular/material/button';
import { AuthService } from '../core/services/auth.service';
import { WorkspaceInvitationService } from '../core/services/workspace-invitation.service';

export const PENDING_INVITATION_TOKEN_KEY = 'pendingInvitationToken';

@Component({
  selector: 'app-invite-accept',
  standalone: true,
  imports: [MatProgressSpinnerModule, MatButtonModule],
  templateUrl: './invite-accept.component.html',
  styleUrl: './invite-accept.component.scss'
})
export class InviteAcceptComponent implements OnInit {
  state: 'loading' | 'error' | 'success' = 'loading';
  errorMessage = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private auth: AuthService,
    private invitationService: WorkspaceInvitationService
  ) {}

  ngOnInit(): void {
    const token = this.route.snapshot.queryParamMap.get('token');
    if (!token) {
      this.state = 'error';
      this.errorMessage = 'Lien d\'invitation invalide.';
      return;
    }

    this.auth.loadCurrentUser().subscribe(user => {
      if (!user) {
        localStorage.setItem(PENDING_INVITATION_TOKEN_KEY, token);
        this.auth.loginWithGoogle();
        return;
      }

      this.invitationService.acceptInvitation(token).subscribe({
        next: () => {
          this.state = 'success';
          setTimeout(() => this.router.navigate(['/case-files']), 2000);
        },
        error: err => {
          this.state = 'error';
          if (err.status === 409) {
            this.errorMessage = 'Ce lien d\'invitation a expiré ou a déjà été utilisé.';
          } else if (err.status === 403) {
            this.errorMessage = 'Cette invitation n\'est pas destinée à votre adresse email.';
          } else {
            this.errorMessage = 'Une erreur est survenue. Veuillez réessayer.';
          }
        }
      });
    });
  }

  goToLogin(): void {
    this.auth.loginWithGoogle();
  }
}
