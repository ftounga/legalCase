import { Component, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatSnackBar } from '@angular/material/snack-bar';
import { DatePipe } from '@angular/common';
import { forkJoin } from 'rxjs';
import { WorkspaceService } from '../../core/services/workspace.service';
import { WorkspaceMemberService } from '../../core/services/workspace-member.service';
import { AdminUsageService } from '../../core/services/admin-usage.service';
import { AuditLogService } from '../../core/services/audit-log.service';
import { Workspace } from '../../core/models/workspace.model';
import { WorkspaceMember } from '../../core/models/workspace-member.model';
import { WorkspaceUsageSummary } from '../../core/models/workspace-usage-summary.model';
import { AuditLogEntry } from '../../core/models/audit-log-entry.model';

const PLAN_QUOTA: Record<string, number | null> = {
  FREE: null,
  STARTER: 3,
  PRO: 20
};

@Component({
  selector: 'app-workspace-admin',
  standalone: true,
  imports: [
    DatePipe, RouterLink,
    MatCardModule, MatTableModule, MatProgressSpinnerModule, MatProgressBarModule,
    MatIconModule, MatButtonModule, MatChipsModule
  ],
  templateUrl: './workspace-admin.component.html',
  styleUrl: './workspace-admin.component.scss'
})
export class WorkspaceAdminComponent implements OnInit {
  workspace = signal<Workspace | null>(null);
  members = signal<WorkspaceMember[]>([]);
  usage = signal<WorkspaceUsageSummary | null>(null);
  auditLogs = signal<AuditLogEntry[]>([]);
  loading = signal(true);
  accessDenied = signal(false);

  readonly memberColumns = ['email', 'role'];
  readonly auditColumns = ['createdAt', 'action', 'userEmail', 'caseFileTitle'];

  constructor(
    private workspaceService: WorkspaceService,
    private memberService: WorkspaceMemberService,
    private adminUsageService: AdminUsageService,
    private auditLogService: AuditLogService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    forkJoin({
      workspace: this.workspaceService.getCurrentWorkspace(),
      members: this.memberService.getMembers(),
      usage: this.adminUsageService.getSummary(),
      auditLogs: this.auditLogService.getAuditLogs()
    }).subscribe({
      next: ({ workspace, members, usage, auditLogs }) => {
        this.workspace.set(workspace);
        this.members.set(members);
        this.usage.set(usage);
        this.auditLogs.set(auditLogs);
        this.loading.set(false);
      },
      error: (err: any) => {
        this.loading.set(false);
        if (err.status === 403) {
          this.accessDenied.set(true);
        } else {
          this.snackBar.open('Erreur lors du chargement des données', 'Fermer', {
            duration: 4000, panelClass: ['snack-error']
          });
        }
      }
    });
  }

  getPlanQuota(planCode: string): string {
    const quota = PLAN_QUOTA[planCode];
    return quota !== null && quota !== undefined ? `${quota} dossiers` : 'Essai gratuit';
  }

  isTrial(workspace: Workspace): boolean {
    return workspace.planCode === 'FREE' && !!workspace.expiresAt;
  }

  get monthlyProgressPercent(): number {
    const u = this.usage();
    if (!u || u.monthlyTokensBudget === 0) return 0;
    return Math.min(100, Math.round((u.monthlyTokensUsed / u.monthlyTokensBudget) * 100));
  }

  get monthlyProgressColor(): 'primary' | 'accent' | 'warn' {
    const p = this.monthlyProgressPercent;
    if (p >= 80) return 'warn';
    if (p >= 60) return 'accent';
    return 'primary';
  }

  formatTokens(n: number): string {
    if (n >= 1_000_000) return (n / 1_000_000).toFixed(1) + 'M';
    if (n >= 1_000) return (n / 1_000).toFixed(0) + 'K';
    return n.toString();
  }
}
