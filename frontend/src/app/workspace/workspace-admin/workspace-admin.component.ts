import { Component, OnInit, signal, computed } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { DecimalPipe, DatePipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { forkJoin } from 'rxjs';
import { WorkspaceService } from '../../core/services/workspace.service';
import { WorkspaceMemberService } from '../../core/services/workspace-member.service';
import { AdminUsageService } from '../../core/services/admin-usage.service';
import { TimeService } from '../../core/services/time.service';
import { TimeReportService } from '../../core/services/time-report.service';
import { Workspace } from '../../core/models/workspace.model';
import { BillingRateResponse, TimeReportRow } from '../../core/models/time-tracking.models';
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
    DecimalPipe, DatePipe, RouterLink, FormsModule,
    MatCardModule, MatTableModule, MatProgressSpinnerModule, MatProgressBarModule,
    MatIconModule, MatButtonModule, MatChipsModule,
    MatFormFieldModule, MatInputModule, MatSelectModule
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

  memberRates = signal<Record<string, BillingRateResponse>>({});
  memberRateInputs = signal<Record<string, number | null>>({});
  savingRateFor = signal<string | null>(null);

  readonly memberColumns = ['email', 'role', 'rateActuel', 'rateNouv', 'rateAction'];

  readonly reportMonths = this.buildMonths();
  selectedReportMonth = signal(this.buildMonths()[0].value);
  reportRows = signal<TimeReportRow[]>([]);
  reportLoading = signal(false);

  readonly reportTotalSeconds = computed(() =>
    this.reportRows().reduce((acc, r) => acc + r.totalSeconds, 0)
  );
  readonly reportTotalAmount = computed(() =>
    this.reportRows().reduce((acc, r) => acc + (r.amount ?? 0), 0)
  );

  constructor(
    private workspaceService: WorkspaceService,
    private memberService: WorkspaceMemberService,
    private adminUsageService: AdminUsageService,
    private timeService: TimeService,
    private timeReportService: TimeReportService,
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

    this.timeService.getMemberRates().subscribe({
      next: rates => {
        this.memberRates.set(rates);
        const inputs: Record<string, number | null> = {};
        Object.entries(rates).forEach(([uid, r]) => { inputs[uid] = r.ratePerHour; });
        this.memberRateInputs.set(inputs);
      },
      error: () => { /* fail-open si non admin */ }
    });

    this.loadTimeReport();
  }

  onReportMonthChange(): void {
    this.loadTimeReport();
  }

  loadTimeReport(): void {
    this.reportLoading.set(true);
    this.timeReportService.getReport(this.selectedReportMonth()).subscribe({
      next: rows => {
        this.reportRows.set(rows);
        this.reportLoading.set(false);
      },
      error: () => {
        this.reportLoading.set(false);
      }
    });
  }

  formatReportDuration(seconds: number): string {
    const h = Math.floor(seconds / 3600);
    const m = Math.floor((seconds % 3600) / 60);
    if (h > 0) return `${h}h ${String(m).padStart(2, '0')}min`;
    if (m > 0) return `${m}min`;
    return `${seconds}s`;
  }

  private buildMonths(): { label: string; value: string }[] {
    const result: { label: string; value: string }[] = [];
    const now = new Date();
    for (let i = 0; i < 13; i++) {
      const d = new Date(now.getFullYear(), now.getMonth() - i, 1);
      const value = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
      const label = d.toLocaleDateString('fr-FR', { month: 'long', year: 'numeric' });
      result.push({ value, label });
    }
    return result;
  }

  getMemberRateInput(userId: string): number | null {
    return this.memberRateInputs()[userId] ?? null;
  }

  setMemberRateInput(userId: string, value: number | null): void {
    this.memberRateInputs.update(inputs => ({ ...inputs, [userId]: value }));
  }

  getMemberRate(userId: string): number | null {
    return this.memberRates()[userId]?.ratePerHour ?? null;
  }

  saveRateForMember(userId: string): void {
    const rate = this.getMemberRateInput(userId);
    if (rate == null || rate <= 0) return;
    this.savingRateFor.set(userId);
    this.timeService.setRateForMember(userId, rate).subscribe({
      next: saved => {
        this.memberRates.update(r => ({ ...r, [userId]: saved }));
        this.savingRateFor.set(null);
        this.snackBar.open('Taux enregistré', 'Fermer', {
          duration: 3000, panelClass: ['snack-success']
        });
      },
      error: () => {
        this.savingRateFor.set(null);
        this.snackBar.open('Erreur lors de l\'enregistrement.', 'Fermer', {
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

  /** SF-122-12 : pourcentage OCR = used / (monthlyBudget + packsRemaining) */
  get monthlyOcrProgressPercent(): number {
    const u = this.usage();
    if (!u) return 0;
    const total = u.ocrMonthlyBudget + u.ocrPacksRemaining;
    if (total === 0) return 0;
    return Math.min(100, Math.round((u.ocrPagesUsed / total) * 100));
  }

  get monthlyOcrProgressColor(): 'primary' | 'accent' | 'warn' {
    const p = this.monthlyOcrProgressPercent;
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
