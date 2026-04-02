import { Component, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar } from '@angular/material/snack-bar';
import { DatePipe } from '@angular/common';
import { forkJoin } from 'rxjs';
import { WorkspaceService } from '../../core/services/workspace.service';
import { WorkspaceMemberService } from '../../core/services/workspace-member.service';
import { AdminUsageService } from '../../core/services/admin-usage.service';
import { TimeService } from '../../core/services/time.service';
import { Workspace } from '../../core/models/workspace.model';
import { BillingRateResponse } from '../../core/models/time-tracking.models';
import { fadeInUp } from '../../shared/animations';
import { WorkspaceMember } from '../../core/models/workspace-member.model';
import { WorkspaceUsageSummary } from '../../core/models/workspace-usage-summary.model';

const PLAN_QUOTA: Record<string, string> = {
  FREE: 'Essai gratuit',
  SOLO: '15 dossiers',
  TEAM: '40 dossiers',
  PRO: 'Illimité'
};

@Component({
  selector: 'app-workspace-admin',
  standalone: true,
  imports: [
    DatePipe, RouterLink, FormsModule,
    MatCardModule, MatTableModule, MatProgressSpinnerModule, MatProgressBarModule,
    MatIconModule, MatButtonModule, MatChipsModule,
    MatFormFieldModule, MatInputModule
  ],
  templateUrl: './workspace-admin.component.html',
  styleUrl: './workspace-admin.component.scss',
  animations: [fadeInUp],
  host: { '[@fadeInUp]': '' },
})
export class WorkspaceAdminComponent implements OnInit {
  workspace = signal<Workspace | null>(null);
  members = signal<WorkspaceMember[]>([]);
  usage = signal<WorkspaceUsageSummary | null>(null);
  loading = signal(true);
  accessDenied = signal(false);

  billingRate = signal<BillingRateResponse | null>(null);
  billingRateInput = signal<number | null>(null);
  savingBillingRate = signal(false);

  readonly memberColumns = ['email', 'role'];

  constructor(
    private workspaceService: WorkspaceService,
    private memberService: WorkspaceMemberService,
    private adminUsageService: AdminUsageService,
    private timeService: TimeService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    forkJoin({
      workspace: this.workspaceService.getCurrentWorkspace(),
      members: this.memberService.getMembers(),
      usage: this.adminUsageService.getSummary()
    }).subscribe({
      next: ({ workspace, members, usage }) => {
        this.workspace.set(workspace);
        this.members.set(members);
        this.usage.set(usage);
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

    this.timeService.getBillingRate().subscribe({
      next: rate => {
        this.billingRate.set(rate);
        if (rate) {
          this.billingRateInput.set(rate.ratePerHour);
        }
      },
      error: () => { /* fail-open */ }
    });
  }

  saveBillingRate(): void {
    const rate = this.billingRateInput();
    if (rate == null || rate <= 0) return;
    this.savingBillingRate.set(true);
    this.timeService.saveBillingRate(rate).subscribe({
      next: saved => {
        this.billingRate.set(saved);
        this.billingRateInput.set(saved.ratePerHour);
        this.savingBillingRate.set(false);
        this.snackBar.open('Taux horaire enregistré', 'Fermer', {
          duration: 3000, panelClass: ['snack-success']
        });
      },
      error: () => {
        this.savingBillingRate.set(false);
        this.snackBar.open('Erreur lors de l\'enregistrement du taux horaire.', 'Fermer', {
          duration: 4000, panelClass: ['snack-error']
        });
      }
    });
  }

  getPlanQuota(planCode: string): string {
    return PLAN_QUOTA[planCode] ?? planCode;
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
