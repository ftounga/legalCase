import { Component, OnDestroy, OnInit, computed, signal } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Subject, Subscription } from 'rxjs';
import { debounceTime } from 'rxjs/operators';
import { MatSortModule, Sort } from '@angular/material/sort';
import { AuthService } from '../../core/services/auth.service';
import { BacklogAdminService } from '../../core/services/backlog-admin.service';
import { DashboardAuditService } from '../../core/services/dashboard-audit.service';
import { DashboardAuditReport } from '../../core/models/dashboard-audit.model';
import {
  BacklogDomain,
  BacklogFeatureSummary,
  BacklogFreshness,
  BacklogMarketingStatus,
  BacklogMarketingTaskSummary,
  BacklogPriority,
  BacklogStatus,
} from '../../core/models/backlog.model';
import { fadeInUp } from '../../shared/animations';
import { BacklogStatusBadgeComponent } from './shared/backlog-status-badge.component';
import { BacklogFeatureDetailDialogComponent } from './feature-detail/backlog-feature-detail-dialog.component';

const FEATURE_STATUSES: BacklogStatus[] = [
  'PLANNED', 'READY', 'IN_PROGRESS', 'BLOCKED',
  'DONE', 'PARTIAL', 'ABSORBED', 'UNKNOWN',
];
const MARKETING_STATUSES: BacklogMarketingStatus[] = [
  'A_FAIRE', 'REDIGE', 'EN_COURS', 'TERMINE', 'BLOQUE', 'UNKNOWN',
];
const DOMAINS: BacklogDomain[] = [
  'DROIT_TRAVAIL', 'IMMIGRATION', 'FAMILLE', 'TRANSVERSAL', 'MARKETING', 'UNKNOWN',
];
const PRIORITIES: BacklogPriority[] = ['HIGH', 'MEDIUM', 'LOW', 'UNKNOWN'];

const DOMAIN_LABELS: Record<BacklogDomain, string> = {
  DROIT_TRAVAIL: 'Droit du travail',
  IMMIGRATION: 'Immigration',
  FAMILLE: 'Famille',
  TRANSVERSAL: 'Transversal',
  MARKETING: 'Marketing',
  UNKNOWN: '—',
};
const PRIORITY_LABELS: Record<BacklogPriority, string> = {
  HIGH: 'Haute',
  MEDIUM: 'Moyenne',
  LOW: 'Basse',
  UNKNOWN: '—',
};

const STATUS_FEATURE_LABELS: Record<BacklogStatus, string> = {
  PLANNED: 'À planifier',
  READY: 'Ready to dev',
  IN_PROGRESS: 'En cours',
  BLOCKED: 'Bloqué',
  DONE: 'Terminée',
  PARTIAL: 'Partielle',
  ABSORBED: 'Absorbée',
  UNKNOWN: 'Inconnu',
};
const STATUS_MARKETING_LABELS: Record<BacklogMarketingStatus, string> = {
  A_FAIRE: 'À faire',
  REDIGE: 'Rédigé',
  EN_COURS: 'En cours',
  TERMINE: 'Terminé',
  BLOQUE: 'Bloqué',
  UNKNOWN: 'Inconnu',
};

@Component({
  selector: 'app-super-admin-backlog',
  standalone: true,
  imports: [
    CommonModule, FormsModule, DatePipe,
    MatTabsModule, MatTableModule, MatPaginatorModule,
    MatFormFieldModule, MatSelectModule, MatInputModule,
    MatButtonModule, MatIconModule, MatProgressSpinnerModule, MatDialogModule,
    MatButtonToggleModule, MatSortModule,
    BacklogStatusBadgeComponent,
  ],
  templateUrl: './super-admin-backlog.component.html',
  styleUrl: './super-admin-backlog.component.scss',
  animations: [fadeInUp],
  host: { '[@fadeInUp]': '' },
})
export class SuperAdminBacklogComponent implements OnInit, OnDestroy {
  readonly featureStatuses = FEATURE_STATUSES;
  readonly marketingStatuses = MARKETING_STATUSES;
  readonly domains = DOMAINS;
  readonly priorities = PRIORITIES;
  readonly domainLabels = DOMAIN_LABELS;
  readonly priorityLabels = PRIORITY_LABELS;
  readonly statusFeatureLabels = STATUS_FEATURE_LABELS;
  readonly statusMarketingLabels = STATUS_MARKETING_LABELS;

  readonly featureColumns = ['code', 'title', 'targetVersion', 'status', 'domain', 'priority', 'updatedAt'];
  readonly marketingColumns = ['code', 'title', 'status', 'category', 'updatedAt'];

  freshness = signal<BacklogFreshness | null>(null);
  freshnessError = signal(false);

  // Tab Produit
  features = signal<BacklogFeatureSummary[]>([]);
  featuresTotal = signal(0);
  featuresPage = signal(0);
  featuresSize = signal(50);
  featuresLoading = signal(false);
  filterStatus = signal<BacklogStatus | null>(null);
  filterDomain = signal<BacklogDomain | null>(null);
  filterPriority = signal<BacklogPriority | null>(null);
  searchProduct = signal('');

  // Tab Marketing
  marketing = signal<BacklogMarketingTaskSummary[]>([]);
  marketingTotal = signal(0);
  marketingPage = signal(0);
  marketingSize = signal(50);
  marketingLoading = signal(false);
  marketingLoaded = signal(false);
  filterMarketingStatus = signal<BacklogMarketingStatus | null>(null);
  searchMarketing = signal('');

  resyncing = signal(false);
  selectedTabIndex = signal(0);

  // ── F-180 SF-180-02 — Tab « Audit dashboard » ──────────────────────────
  readonly crashedColumns = ['toolId', 'crashCount', 'lastExceptionClass', 'lastExceptionMessage', 'lastOccurredAt'];
  readonly activeColumns = ['tableName', 'rowCount'];

  auditReport = signal<DashboardAuditReport | null>(null);
  auditLoaded = signal(false);
  auditLoading = signal(false);
  auditRunning = signal(false);
  auditError = signal(false);
  crashedSort = signal<Sort>({ active: 'crashCount', direction: 'desc' });

  /** Mappers en erreur triés selon l'en-tête MatSort cliqué. */
  sortedCrashedMappers = computed(() => {
    const report = this.auditReport();
    if (!report) return [];
    const sort = this.crashedSort();
    const rows = [...report.crashedMappers];
    if (!sort.active || sort.direction === '') return rows;
    const dir = sort.direction === 'asc' ? 1 : -1;
    return rows.sort((a, b) => {
      switch (sort.active) {
        case 'crashCount': return (a.crashCount - b.crashCount) * dir;
        case 'lastOccurredAt': return a.lastOccurredAt.localeCompare(b.lastOccurredAt) * dir;
        case 'toolId': return a.toolId.localeCompare(b.toolId) * dir;
        case 'lastExceptionClass': return a.lastExceptionClass.localeCompare(b.lastExceptionClass) * dir;
        default: return 0;
      }
    });
  });

  // SF-178-05 — Vue kanban (Produit only)
  viewMode = signal<'list' | 'kanban'>('list');
  readonly kanbanColumns: ReadonlyArray<{ key: BacklogStatus | 'OTHER'; label: string }> = [
    { key: 'READY',       label: 'Ready to dev' },
    { key: 'IN_PROGRESS', label: 'En cours' },
    { key: 'BLOCKED',     label: 'Bloqué' },
    { key: 'DONE',        label: 'Terminée' },
    { key: 'PLANNED',     label: 'À planifier' },
    { key: 'OTHER',       label: 'Autres' },
  ];
  readonly kanbanPageSize = 200;

  groupedFeatures = computed(() => {
    const groups = new Map<BacklogStatus | 'OTHER', BacklogFeatureSummary[]>();
    for (const col of this.kanbanColumns) groups.set(col.key, []);
    for (const f of this.features()) {
      const isMain = (['READY', 'IN_PROGRESS', 'BLOCKED', 'DONE', 'PLANNED'] as BacklogStatus[]).includes(f.status);
      const key: BacklogStatus | 'OTHER' = isMain ? f.status : 'OTHER';
      groups.get(key)!.push(f);
    }
    return groups;
  });

  showOtherColumn = computed(() => (this.groupedFeatures().get('OTHER') ?? []).length > 0);

  private readonly searchProduct$ = new Subject<string>();
  private readonly searchMarketing$ = new Subject<string>();
  private readonly subs = new Subscription();

  constructor(
    private backlogService: BacklogAdminService,
    private auth: AuthService,
    private router: Router,
    private snackBar: MatSnackBar,
    private dialog: MatDialog,
    private dashboardAuditService: DashboardAuditService,
  ) {}

  openFeatureDetail(code: string): void {
    if (!code) return;
    this.dialog.open(BacklogFeatureDetailDialogComponent, {
      width: '880px',
      maxWidth: '95vw',
      maxHeight: '90vh',
      autoFocus: false,
      data: { code },
    });
  }

  onViewModeChange(mode: 'list' | 'kanban'): void {
    if (this.viewMode() === mode) return;
    this.viewMode.set(mode);
    this.featuresPage.set(0);
    this.loadFeatures();
  }

  effectivePageSize(): number {
    return this.viewMode() === 'kanban' ? this.kanbanPageSize : this.featuresSize();
  }

  ngOnInit(): void {
    if (!this.auth.currentUser()?.isSuperAdmin) {
      this.router.navigate(['/case-files']);
      return;
    }
    this.subs.add(this.searchProduct$.pipe(debounceTime(250)).subscribe(value => {
      this.searchProduct.set(value);
      this.featuresPage.set(0);
      this.loadFeatures();
    }));
    this.subs.add(this.searchMarketing$.pipe(debounceTime(250)).subscribe(value => {
      this.searchMarketing.set(value);
      this.marketingPage.set(0);
      this.loadMarketing();
    }));
    this.loadFreshness();
    this.loadFeatures();
  }

  ngOnDestroy(): void {
    this.subs.unsubscribe();
  }

  onTabChange(index: number): void {
    this.selectedTabIndex.set(index);
    if (index === 1 && !this.marketingLoaded()) {
      this.loadMarketing();
    }
    if (index === 2 && !this.auditLoaded()) {
      this.loadAudit();
    }
  }

  // ── Produit ────────────────────────────────────────────────────────────

  onFilterFeatureChange(): void {
    this.featuresPage.set(0);
    this.loadFeatures();
  }

  onFeatureSearchInput(value: string): void {
    this.searchProduct$.next(value ?? '');
  }

  onFeaturePage(event: PageEvent): void {
    this.featuresPage.set(event.pageIndex);
    this.featuresSize.set(event.pageSize);
    this.loadFeatures();
  }

  loadFeatures(): void {
    this.featuresLoading.set(true);
    this.backlogService.searchFeatures(
      {
        status: this.filterStatus(),
        domain: this.filterDomain(),
        priority: this.filterPriority(),
        search: this.searchProduct(),
      },
      this.featuresPage(),
      this.effectivePageSize(),
    ).subscribe({
      next: page => {
        this.features.set(page.content);
        this.featuresTotal.set(page.totalElements);
        this.featuresLoading.set(false);
      },
      error: err => {
        this.featuresLoading.set(false);
        if (err?.status === 403) {
          this.router.navigate(['/case-files']);
          return;
        }
        this.snackBar.open('Erreur lors du chargement des features', 'Fermer', {
          duration: 4000, panelClass: ['snack-error'],
        });
      },
    });
  }

  // ── Marketing ──────────────────────────────────────────────────────────

  onFilterMarketingChange(): void {
    this.marketingPage.set(0);
    this.loadMarketing();
  }

  onMarketingSearchInput(value: string): void {
    this.searchMarketing$.next(value ?? '');
  }

  onMarketingPage(event: PageEvent): void {
    this.marketingPage.set(event.pageIndex);
    this.marketingSize.set(event.pageSize);
    this.loadMarketing();
  }

  loadMarketing(): void {
    this.marketingLoading.set(true);
    this.backlogService.searchMarketingTasks(
      {
        status: this.filterMarketingStatus(),
        search: this.searchMarketing(),
      },
      this.marketingPage(),
      this.marketingSize(),
    ).subscribe({
      next: page => {
        this.marketing.set(page.content);
        this.marketingTotal.set(page.totalElements);
        this.marketingLoading.set(false);
        this.marketingLoaded.set(true);
      },
      error: err => {
        this.marketingLoading.set(false);
        if (err?.status === 403) {
          this.router.navigate(['/case-files']);
          return;
        }
        this.snackBar.open('Erreur lors du chargement des tâches marketing', 'Fermer', {
          duration: 4000, panelClass: ['snack-error'],
        });
      },
    });
  }

  // ── F-180 SF-180-02 — Audit dashboard ──────────────────────────────────

  loadAudit(): void {
    this.auditLoading.set(true);
    this.auditError.set(false);
    this.dashboardAuditService.getLatest().subscribe({
      next: report => {
        this.auditReport.set(report);
        this.auditLoaded.set(true);
        this.auditLoading.set(false);
      },
      error: err => {
        this.auditLoading.set(false);
        this.auditError.set(true);
        if (err?.status === 403) {
          this.router.navigate(['/case-files']);
          return;
        }
        this.snackBar.open('Erreur lors du chargement de l\'audit dashboard', 'Fermer', {
          duration: 4000, panelClass: ['snack-error'],
        });
      },
    });
  }

  triggerAuditRun(): void {
    if (this.auditRunning()) return;
    this.auditRunning.set(true);
    this.dashboardAuditService.runAudit().subscribe({
      next: report => {
        this.auditReport.set(report);
        this.auditLoaded.set(true);
        this.auditRunning.set(false);
        this.snackBar.open('Audit relancé', 'Fermer', {
          duration: 3000, panelClass: ['snack-success'],
        });
      },
      error: err => {
        this.auditRunning.set(false);
        if (err?.status === 403) {
          this.router.navigate(['/case-files']);
          return;
        }
        this.snackBar.open('Échec de la relance de l\'audit', 'Fermer', {
          duration: 5000, panelClass: ['snack-error'],
        });
      },
    });
  }

  onCrashedSortChange(sort: Sort): void {
    this.crashedSort.set(sort);
  }

  /** Commande kubectl logs suggérée pour investiguer un crash de mapper. */
  kubectlLogsHint(toolId: string): string {
    return `kubectl logs deploy/legalcase-backend | grep "fail-open per tile ${toolId}"`;
  }

  // ── Freshness + Resync ─────────────────────────────────────────────────

  loadFreshness(): void {
    this.freshnessError.set(false);
    this.backlogService.getFreshness().subscribe({
      next: f => {
        this.freshness.set(f);
        this.freshnessError.set(false);
      },
      error: () => {
        this.freshness.set(null);
        this.freshnessError.set(true);
      },
    });
  }

  triggerResync(): void {
    if (this.resyncing()) return;
    this.resyncing.set(true);
    this.backlogService.triggerSync().subscribe({
      next: result => {
        this.resyncing.set(false);
        const msg = `Resync OK — ${result.featuresCount} features, ${result.marketingCount} marketing (${result.durationMs} ms)`;
        this.snackBar.open(msg, 'Fermer', { duration: 4000, panelClass: ['snack-success'] });
        this.loadFreshness();
        this.loadFeatures();
        if (this.marketingLoaded()) this.loadMarketing();
      },
      error: () => {
        this.resyncing.set(false);
        this.snackBar.open('Échec resync — voir les logs serveur', 'Fermer', {
          duration: 5000, panelClass: ['snack-error'],
        });
      },
    });
  }

  freshnessTone(): 'ok' | 'stale' | 'error' | 'unknown' {
    const f = this.freshness();
    if (!f) return 'unknown';
    switch (f.status) {
      case 'OK': return 'ok';
      case 'STALE': return 'stale';
      case 'ERROR': return 'error';
      default: return 'unknown';
    }
  }

  domainLabel(domain: string | null | undefined): string {
    if (!domain) return DOMAIN_LABELS.UNKNOWN;
    return DOMAIN_LABELS[domain as BacklogDomain] ?? domain;
  }

  priorityLabel(priority: string | null | undefined): string {
    if (!priority) return PRIORITY_LABELS.UNKNOWN;
    return PRIORITY_LABELS[priority as BacklogPriority] ?? priority;
  }

  freshnessLabel(): string {
    const f = this.freshness();
    if (!f) return '—';
    const minutes = f.minutesSinceLastSync;
    const minutesLabel = minutes == null ? '?' : `${minutes}`;
    switch (f.status) {
      case 'OK':
        return `Synchronisé il y a ${minutesLabel} minute${minutes === 1 ? '' : 's'}`;
      case 'STALE':
        return `Sync obsolète depuis ${minutesLabel} minute${minutes === 1 ? '' : 's'}`;
      case 'ERROR':
        return 'Dernière sync en erreur';
      default:
        return 'Statut sync inconnu';
    }
  }
}
