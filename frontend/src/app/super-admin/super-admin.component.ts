import { Component, OnInit, signal } from '@angular/core';
import { environment } from '../../environments/environment';
import { Router } from '@angular/router';
import { DatePipe, DecimalPipe, LowerCasePipe } from '@angular/common';
import { forkJoin } from 'rxjs';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { AuthService } from '../core/services/auth.service';
import { SuperAdminService } from '../core/services/super-admin.service';
import { SuperAdminWorkspace, SuperAdminUsage, SuperAdminUser, SuperAdminMetrics } from '../core/models/super-admin.model';
import { SuperAdminConfirmDialogComponent } from './super-admin-confirm-dialog.component';
import { fadeInUp } from '../shared/animations';

interface WorkspaceRow extends SuperAdminWorkspace {
  totalTokensInput: number;
  totalTokensOutput: number;
  totalCost: number;
}

@Component({
  selector: 'app-super-admin',
  standalone: true,
  imports: [
    DatePipe, DecimalPipe, LowerCasePipe,
    MatCardModule, MatTableModule, MatButtonModule,
    MatIconModule, MatProgressSpinnerModule, MatPaginatorModule, MatDialogModule
  ],
  templateUrl: './super-admin.component.html',
  styleUrl: './super-admin.component.scss',
  animations: [fadeInUp],
  host: { '[@fadeInUp]': '' },
})
export class SuperAdminComponent implements OnInit {
  workspaceRows = signal<WorkspaceRow[]>([]);
  wsTotalElements = signal(0);
  wsPage = signal(0);
  wsSize = signal(20);

  users = signal<SuperAdminUser[]>([]);
  usersTotalElements = signal(0);
  usersPage = signal(0);
  usersSize = signal(20);

  metrics = signal<SuperAdminMetrics | null>(null);
  loading = signal(true);

  private usageMap = new Map<string, SuperAdminUsage>();

  readonly rabbitmqUrl = environment.rabbitmqUrl;
  readonly workspaceColumns = ['name', 'plan', 'members', 'tokensInput', 'tokensOutput', 'cost', 'createdAt', 'actions'];
  readonly userColumns = ['email', 'name', 'workspaces', 'actions'];

  constructor(
    private superAdminService: SuperAdminService,
    readonly auth: AuthService,
    private router: Router,
    private snackBar: MatSnackBar,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    if (!this.auth.currentUser()?.isSuperAdmin) {
      this.router.navigate(['/case-files']);
      return;
    }
    this.loadAll();
  }

  loadAll(): void {
    this.loading.set(true);
    forkJoin({
      workspaces: this.superAdminService.listWorkspaces(this.wsPage(), this.wsSize()),
      usage: this.superAdminService.getUsage(),
      users: this.superAdminService.listUsers(this.usersPage(), this.usersSize()),
      metrics: this.superAdminService.getMetrics()
    }).subscribe({
      next: ({ workspaces, usage, users, metrics }) => {
        this.usageMap = new Map(usage.map(u => [u.workspaceId, u]));
        this.applyWorkspacePage(workspaces.content, workspaces.totalElements);
        this.users.set(users.content);
        this.usersTotalElements.set(users.totalElements);
        this.metrics.set(metrics);
        this.loading.set(false);
      },
      error: (err: any) => {
        this.loading.set(false);
        if (err.status === 403) {
          this.router.navigate(['/case-files']);
        } else {
          this.snackBar.open('Erreur lors du chargement des données', 'Fermer', {
            duration: 4000, panelClass: ['snack-error']
          });
        }
      }
    });
  }

  onWsPageChange(event: PageEvent): void {
    this.wsPage.set(event.pageIndex);
    this.wsSize.set(event.pageSize);
    this.superAdminService.listWorkspaces(event.pageIndex, event.pageSize).subscribe({
      next: page => this.applyWorkspacePage(page.content, page.totalElements),
      error: () => this.snackBar.open('Erreur lors du chargement des workspaces', 'Fermer', {
        duration: 4000, panelClass: ['snack-error']
      })
    });
  }

  onUsersPageChange(event: PageEvent): void {
    this.usersPage.set(event.pageIndex);
    this.usersSize.set(event.pageSize);
    this.superAdminService.listUsers(event.pageIndex, event.pageSize).subscribe({
      next: page => {
        this.users.set(page.content);
        this.usersTotalElements.set(page.totalElements);
      },
      error: () => this.snackBar.open('Erreur lors du chargement des utilisateurs', 'Fermer', {
        duration: 4000, panelClass: ['snack-error']
      })
    });
  }

  confirmDeleteWorkspace(ws: WorkspaceRow): void {
    const ref = this.dialog.open(SuperAdminConfirmDialogComponent, {
      width: '480px',
      data: { message: `Supprimer le workspace « ${ws.name} » et toutes ses données ? Cette action est irréversible.` }
    });
    ref.afterClosed().subscribe(confirmed => {
      if (!confirmed) return;
      this.superAdminService.deleteWorkspace(ws.id).subscribe({
        next: () => {
          this.snackBar.open(`Workspace « ${ws.name} » supprimé`, 'Fermer', {
            duration: 4000, panelClass: ['snack-success']
          });
          this.loadAll();
        },
        error: () => this.snackBar.open('Erreur lors de la suppression du workspace', 'Fermer', {
          duration: 4000, panelClass: ['snack-error']
        })
      });
    });
  }

  confirmDeleteUser(user: SuperAdminUser): void {
    const name = user.firstName ? `${user.firstName} ${user.lastName ?? ''}`.trim() : user.email;
    const ref = this.dialog.open(SuperAdminConfirmDialogComponent, {
      width: '480px',
      data: { message: `Supprimer l'utilisateur « ${name} » de tous ses workspaces ? Cette action est irréversible.` }
    });
    ref.afterClosed().subscribe(confirmed => {
      if (!confirmed) return;
      this.superAdminService.deleteUser(user.id).subscribe({
        next: () => {
          this.snackBar.open(`Utilisateur supprimé`, 'Fermer', {
            duration: 4000, panelClass: ['snack-success']
          });
          this.loadAll();
        },
        error: () => this.snackBar.open('Erreur lors de la suppression de l\'utilisateur', 'Fermer', {
          duration: 4000, panelClass: ['snack-error']
        })
      });
    });
  }

  planLabel(code: string): string {
    const labels: Record<string, string> = { FREE: 'Essai', SOLO: 'Solo', TEAM: 'Team', PRO: 'Pro' };
    return labels[code] ?? code;
  }

  userDisplayName(user: SuperAdminUser): string {
    const full = [user.firstName, user.lastName].filter(Boolean).join(' ');
    return full || '—';
  }

  private applyWorkspacePage(workspaces: SuperAdminWorkspace[], total: number): void {
    this.workspaceRows.set(workspaces.map(ws => {
      const u = this.usageMap.get(ws.id);
      return {
        ...ws,
        totalTokensInput: u?.totalTokensInput ?? 0,
        totalTokensOutput: u?.totalTokensOutput ?? 0,
        totalCost: u?.totalCost ?? 0
      };
    }));
    this.wsTotalElements.set(total);
  }
}
