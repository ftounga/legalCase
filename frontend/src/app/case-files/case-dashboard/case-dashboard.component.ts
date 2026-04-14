import { Component, DestroyRef, Input, OnInit, Optional, inject, signal, computed } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { DecimalPipe } from '@angular/common';
import { debounceTime } from 'rxjs';
import { CaseDashboardService } from '../../core/services/case-dashboard.service';
import { DashboardResponse } from '../../core/models/case-dashboard.model';
import { CaseDashboardRefreshService } from './case-dashboard-refresh.service';

@Component({
  selector: 'app-case-dashboard',
  standalone: true,
  imports: [MatIconModule, MatProgressSpinnerModule, DecimalPipe],
  templateUrl: './case-dashboard.component.html',
  styleUrl: './case-dashboard.component.scss'
})
export class CaseDashboardComponent implements OnInit {
  @Input() caseFileId!: string;

  loading = signal(false);
  dashboard = signal<DashboardResponse | null>(null);

  hasAnyData = computed(() => {
    const d = this.dashboard();
    if (!d) return false;
    return d.licenciement || d.indemnites || d.anciennete || d.titleDecision ||
           d.workRight || d.recours || d.partage || d.garde || d.divorce || d.riskScore != null;
  });

  riskColor = computed(() => {
    const d = this.dashboard();
    if (!d || d.riskScore == null) return '#6B7A8D';
    if (d.riskScore < 30) return '#27AE60';
    if (d.riskScore < 60) return '#F59E0B';
    return '#C0392B';
  });

  private readonly destroyRef = inject(DestroyRef);

  constructor(
    private dashboardService: CaseDashboardService,
    @Optional() private refreshService: CaseDashboardRefreshService | null,
  ) {}

  ngOnInit(): void {
    this.reload(true);
    if (this.refreshService) {
      this.refreshService.refresh$
        .pipe(debounceTime(300), takeUntilDestroyed(this.destroyRef))
        .subscribe(() => this.reload(false));
    }
  }

  private reload(showSpinner: boolean): void {
    if (showSpinner) this.loading.set(true);
    this.dashboardService.get(this.caseFileId).subscribe({
      next: d => { this.dashboard.set(d); this.loading.set(false); },
      error: () => { this.loading.set(false); },
    });
  }
}
