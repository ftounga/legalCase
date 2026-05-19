import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { MatDividerModule } from '@angular/material/divider';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatBadgeModule } from '@angular/material/badge';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog } from '@angular/material/dialog';
import { BreakpointObserver } from '@angular/cdk/layout';
import { AuthService } from '../../core/services/auth.service';
import { WorkspaceService } from '../../core/services/workspace.service';
import { WorkspaceInvitationService } from '../../core/services/workspace-invitation.service';
import { WorkspaceStateService } from '../../core/services/workspace-state.service';
import { ReferentialService } from '../../core/services/referential.service';
import { Workspace } from '../../core/models/workspace.model';
import { PENDING_INVITATION_TOKEN_KEY } from '../../invite-accept/invite-accept.component';
import { TrialBannerComponent } from '../trial-banner/trial-banner.component';
import { NotificationCenterComponent } from '../notification-center/notification-center.component';
import { WorkspaceCreateDialogComponent } from '../workspace-create-dialog/workspace-create-dialog.component';
import { WorkspaceStatusBannerComponent } from '../workspace-status-banner/workspace-status-banner.component';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [
    RouterOutlet, RouterLink, RouterLinkActive,
    MatToolbarModule, MatSidenavModule, MatListModule,
    MatIconModule, MatButtonModule, MatMenuModule,
    MatProgressSpinnerModule, MatBadgeModule, MatDividerModule,
    TrialBannerComponent, NotificationCenterComponent,
    WorkspaceStatusBannerComponent,
  ],
  templateUrl: './shell.component.html',
  styleUrl: './shell.component.scss'
})
export class ShellComponent implements OnInit, OnDestroy {
  workspace = signal<Workspace | null>(null);
  workspaces = signal<Workspace[]>([]);
  ready = signal(false);
  isMobile = signal(false);
  sidenavOpen = signal(true);
  pendingAlertsCount = signal(0);

  /** F-156 SF-156-02 — option « + Créer un workspace » conditionnée au plan OWNER courant (CA1/CA2). */
  readonly canCreateWorkspace = computed(() => {
    const plan = (this.workspace()?.planCode ?? '').toUpperCase();
    return plan === 'TEAM' || plan === 'PRO';
  });

  get userInitials(): string {
    const user = this.auth.currentUser();
    if (!user?.email) return '?';
    const parts = user.email.split('@')[0].split(/[._-]/);
    if (parts.length >= 2) {
      return (parts[0][0] + parts[1][0]).toUpperCase();
    }
    return user.email.substring(0, 2).toUpperCase();
  }
  private alertPollingTimer?: ReturnType<typeof setInterval>;
  private readonly workspaceState = inject(WorkspaceStateService);
  private readonly route = inject(ActivatedRoute);

  constructor(
    readonly auth: AuthService,
    private workspaceService: WorkspaceService,
    private invitationService: WorkspaceInvitationService,
    private referentialService: ReferentialService,
    private snackBar: MatSnackBar,
    private router: Router,
    private breakpointObserver: BreakpointObserver,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    this.breakpointObserver.observe('(max-width: 767px)').subscribe(result => {
      this.isMobile.set(result.matches);
      this.sidenavOpen.set(!result.matches);
    });

    this.pollAlertCount();
    this.alertPollingTimer = setInterval(() => this.pollAlertCount(), 5 * 60 * 1000);

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

    // F-156 SF-156-02 — détection retour Stripe (CA6, CA10).
    // Le success_url backend pointe vers `?workspace_created=success&workspace_id=<id>`.
    this.handleStripeReturn();
  }

  ngOnDestroy(): void {
    if (this.alertPollingTimer) {
      clearInterval(this.alertPollingTimer);
    }
  }

  private pollAlertCount(): void {
    this.referentialService.getPendingAlertsCount().subscribe({
      next: res => this.pendingAlertsCount.set(res.count),
      error: () => {}
    });
  }

  toggleSidenav(): void {
    this.sidenavOpen.update(open => !open);
  }

  onNavClick(): void {
    if (this.isMobile()) {
      this.sidenavOpen.set(false);
    }
  }

  switchTo(ws: Workspace): void {
    if (ws.primary) return;
    this.workspaceService.switchWorkspace(ws.id).subscribe({
      next: newWs => {
        this.workspace.set(newWs);
        this.workspaceState.setCurrent(newWs);
        this.workspaces.update(list => list.map(w => ({ ...w, primary: w.id === newWs.id })));
        this.workspaceService.notifyWorkspaceSwitched();
        this.router.navigate(['/case-files']);
      },
      error: () => this.snackBar.open('Erreur lors du changement de workspace.', 'Fermer', {
        duration: 4000, panelClass: ['snack-error']
      })
    });
  }

  /**
   * F-156 SF-156-02 — ouvre le dialog de création d'un workspace payant (TEAM/PRO).
   *
   * <p>Au succès, le dialog redirige le navigateur vers Stripe Checkout (`window.location.href`).
   * Le retour Stripe est géré par {@link handleStripeReturn} via le query param `workspace_created`.
   *
   * <p>Gate amont : l'option « + Créer » n'est rendue dans le template que si
   * {@link canCreateWorkspace} est true (CA1/CA2).
   */
  openCreateWorkspaceDialog(): void {
    if (!this.canCreateWorkspace()) {
      // Sécurité défense en profondeur — ne devrait jamais arriver car l'option est cachée.
      this.router.navigate(['/workspaces/upgrade-required']);
      return;
    }
    this.dialog.open(WorkspaceCreateDialogComponent, {
      width: '720px',
      maxWidth: '95vw',
      autoFocus: false,
      disableClose: false,
    });
    // Volontairement pas de afterClosed() avec switch ici : la redirection Stripe quitte l'app.
  }

  /**
   * F-156 SF-156-02 — handler du retour Stripe (CA6 / CA10).
   * - `?workspace_created=success&workspace_id=<id>` → bascule sur le nouveau workspace + nettoie l'URL.
   * - `?workspace_created=cancelled` → snackbar « Création annulée — aucun frais prélevé »
   *   + reste sur le workspace courant.
   */
  private handleStripeReturn(): void {
    this.route.queryParams.subscribe(params => {
      const created = params['workspace_created'];
      if (!created) return;
      const workspaceId = params['workspace_id'];

      if (created === 'success' && workspaceId) {
        this.workspaceService.switchWorkspace(workspaceId).subscribe({
          next: ws => {
            this.workspace.set(ws);
            this.workspaceState.setCurrent(ws);
            this.loadWorkspaceList();
            this.workspaceService.notifyWorkspaceSwitched();
            // Nettoyage des query params pour éviter de re-déclencher au refresh.
            this.router.navigate([], {
              relativeTo: this.route,
              queryParams: { workspace_created: null, workspace_id: null },
              queryParamsHandling: 'merge',
              replaceUrl: true,
            });
          },
          error: () => this.snackBar.open(
            'Workspace créé mais basculement impossible. Rafraîchissez la page.',
            'Fermer',
            { duration: 6000, panelClass: ['snack-error'] }
          )
        });
      } else if (created === 'cancelled') {
        this.snackBar.open('Création annulée — aucun frais prélevé.', 'Fermer', {
          duration: 4000, panelClass: ['snack-info']
        });
        this.router.navigate([], {
          relativeTo: this.route,
          queryParams: { workspace_created: null, workspace_id: null },
          queryParamsHandling: 'merge',
          replaceUrl: true,
        });
      }
    });
  }

  private loadWorkspace(): void {
    this.workspaceService.getCurrentWorkspace().subscribe({
      next: ws => {
        this.workspace.set(ws);
        this.workspaceState.setCurrent(ws);
        this.loadWorkspaceList();
        this.ready.set(true);
      },
      error: () => this.ready.set(true)
    });
  }

  domainColor(legalDomain?: string): string {
    switch (legalDomain) {
      case 'DROIT_DU_TRAVAIL':  return '#27AE60';
      case 'DROIT_FAMILLE':     return '#C9973A';
      case 'DROIT_IMMIGRATION':
      default:                  return '#1A3A5C';
    }
  }

  private loadWorkspaceList(): void {
    this.workspaceService.listWorkspaces().subscribe({
      next: list => this.workspaces.set(list),
      error: () => {}
    });
  }
}
