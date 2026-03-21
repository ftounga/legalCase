import { Component, OnInit, signal } from '@angular/core';
import { Router, RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
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
    MatProgressSpinnerModule,
    TrialBannerComponent
  ],
  templateUrl: './shell.component.html',
  styleUrl: './shell.component.scss'
})
export class ShellComponent implements OnInit {
  workspace = signal<Workspace | null>(null);
  workspaces = signal<Workspace[]>([]);
  ready = signal(false);

  constructor(
    readonly auth: AuthService,
    private workspaceService: WorkspaceService,
    private invitationService: WorkspaceInvitationService,
    private snackBar: MatSnackBar,
    private router: Router
  ) {}

  ngOnInit(): void {
    const pendingToken = localStorage.getItem(PENDING_INVITATION_TOKEN_KEY);

    if (pendingToken) {
      localStorage.removeItem(PENDING_INVITATION_TOKEN_KEY);
      this.invitationService.acceptInvitation(pendingToken).subscribe({
        next: () => {
          this.snackBar.open('Invitation acceptée — bienvenue dans le workspace !', 'Fermer', {
            duration: 6000, panelClass: ['snack-success']
          });
          this.loadWorkspace();
        },
        error: () => {
          this.snackBar.open('Lien d\'invitation invalide ou expiré.', 'Fermer', {
            duration: 6000, panelClass: ['snack-error']
          });
          this.loadWorkspace();
        }
      });
    } else {
      this.loadWorkspace();
    }
  }

  switchTo(ws: Workspace): void {
    if (ws.primary) return;
    this.workspaceService.switchWorkspace(ws.id).subscribe({
      next: newWs => {
        this.workspace.set(newWs);
        this.workspaces.update(list => list.map(w => ({ ...w, primary: w.id === newWs.id })));
        this.workspaceService.notifyWorkspaceSwitched();
        this.router.navigate(['/case-files']);
      },
      error: () => this.snackBar.open('Erreur lors du changement de workspace.', 'Fermer', {
        duration: 4000, panelClass: ['snack-error']
      })
    });
  }

  private loadWorkspace(): void {
    this.workspaceService.getCurrentWorkspace().subscribe({
      next: ws => {
        this.workspace.set(ws);
        this.loadWorkspaceList();
        this.ready.set(true);
      },
      error: () => this.ready.set(true)
    });
  }

  private loadWorkspaceList(): void {
    this.workspaceService.listWorkspaces().subscribe({
      next: list => this.workspaces.set(list),
      error: () => {}
    });
  }
}
