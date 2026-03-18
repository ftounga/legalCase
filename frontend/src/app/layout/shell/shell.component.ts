import { Component, OnInit, signal } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatSnackBar } from '@angular/material/snack-bar';
import { AuthService } from '../../core/services/auth.service';
import { WorkspaceService } from '../../core/services/workspace.service';
import { WorkspaceInvitationService } from '../../core/services/workspace-invitation.service';
import { Workspace } from '../../core/models/workspace.model';
import { PENDING_INVITATION_TOKEN_KEY } from '../../invite-accept/invite-accept.component';
import { TrialBannerComponent } from '../trial-banner/trial-banner.component';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [
    RouterOutlet, RouterLink, RouterLinkActive,
    MatToolbarModule, MatSidenavModule, MatListModule,
    MatIconModule, MatButtonModule, MatMenuModule,
    TrialBannerComponent
  ],
  templateUrl: './shell.component.html',
  styleUrl: './shell.component.scss'
})
export class ShellComponent implements OnInit {
  workspace = signal<Workspace | null>(null);

  constructor(
    readonly auth: AuthService,
    private workspaceService: WorkspaceService,
    private invitationService: WorkspaceInvitationService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.workspaceService.getCurrentWorkspace().subscribe({
      next: ws => this.workspace.set(ws),
      error: () => {}
    });

    const pendingToken = localStorage.getItem(PENDING_INVITATION_TOKEN_KEY);
    if (pendingToken) {
      localStorage.removeItem(PENDING_INVITATION_TOKEN_KEY);
      this.invitationService.acceptInvitation(pendingToken).subscribe({
        next: () => {
          this.snackBar.open('Invitation acceptée — bienvenue dans le workspace !', 'Fermer', {
            duration: 6000, panelClass: ['snack-success']
          });
          this.workspaceService.getCurrentWorkspace().subscribe({
            next: ws => this.workspace.set(ws),
            error: () => {}
          });
        },
        error: () => {
          this.snackBar.open('Lien d\'invitation invalide ou expiré.', 'Fermer', {
            duration: 6000, panelClass: ['snack-error']
          });
        }
      });
    }
  }
}
