import { Component, Input, OnInit, OnChanges, SimpleChanges, Optional, signal, computed } from '@angular/core';
import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar } from '@angular/material/snack-bar';
import { FormsModule } from '@angular/forms';
import { AncienneteService } from '../../core/services/anciennete.service';
import { AncienneteResponse } from '../../core/models/anciennete.model';

export type AncienneteAlertField = 'CONVENTION' | 'DATE_ENTREE' | 'SALAIRE' | 'CONGES' | 'PRIME';

export interface AncienneteCoherenceAlert {
  field: AncienneteAlertField;
  iaValue: string;
}

@Component({
  selector: 'app-anciennete-section',
  standalone: true,
  imports: [
    FormsModule,
    MatButtonModule, MatIconModule,
    MatSelectModule, MatFormFieldModule,
    MatInputModule, MatProgressSpinnerModule,
    MatTooltipModule,
  ],
  templateUrl: './anciennete-section.component.html',
  styleUrl: './anciennete-section.component.scss'
})
export class AncienneteSectionComponent implements OnInit, OnChanges {
  @Input() caseFileId!: string;
  @Input() aiData?: TravailExtractedData | null;

  private aiDataSignal = signal<TravailExtractedData | null | undefined>(undefined);

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<AncienneteResponse | null>(null);

  conventionCode = signal('METALLURGIE');
  dateEntree = signal('');
  salaireBase = signal(0);
  congesContrat = signal(25);
  primeContrat = signal(0);

  coherenceAlerts = computed<Partial<Record<AncienneteAlertField, AncienneteCoherenceAlert>>>(() => {
    const ai = this.aiDataSignal();
    if (!ai || !this.showForm()) return {};
    const alerts: Partial<Record<AncienneteAlertField, AncienneteCoherenceAlert>> = {};

    // Convention — exact match upper-case
    if (ai.conventionCollective && this.conventionCode()) {
      if (ai.conventionCollective.toUpperCase() !== this.conventionCode().toUpperCase()) {
        alerts.CONVENTION = { field: 'CONVENTION', iaValue: ai.conventionCollective };
      }
    }

    // Date entrée — écart ≥ 15 jours
    if (ai.dateEntree && this.dateEntree()) {
      const diff = dateDaysDiff(ai.dateEntree, this.dateEntree());
      if (diff !== null && diff >= 15) {
        alerts.DATE_ENTREE = { field: 'DATE_ENTREE', iaValue: ai.dateEntree };
      }
    }

    // Salaire — écart relatif ≥ 5 %
    if (ai.salaireBrutMensuel != null && this.salaireBase() > 0) {
      const rel = percentDiff(ai.salaireBrutMensuel, this.salaireBase());
      if (rel >= 0.05) {
        alerts.SALAIRE = { field: 'SALAIRE', iaValue: `${ai.salaireBrutMensuel} €` };
      }
    }

    // Congés — écart ≥ 1 jour
    if (ai.congesContractuels != null && this.congesContrat() > 0) {
      if (Math.abs(ai.congesContractuels - this.congesContrat()) >= 1) {
        alerts.CONGES = { field: 'CONGES', iaValue: `${ai.congesContractuels} j` };
      }
    }

    // Prime — écart ≥ 0,5 pt
    if (ai.primeAncienneteContractuelle != null && this.primeContrat() > 0) {
      if (Math.abs(ai.primeAncienneteContractuelle - this.primeContrat()) >= 0.5) {
        alerts.PRIME = { field: 'PRIME', iaValue: `${ai.primeAncienneteContractuelle} %` };
      }
    }

    return alerts;
  });

  alertsSummary = computed(() => ({ total: Object.keys(this.coherenceAlerts()).length }));

  alertTooltip(alert: AncienneteCoherenceAlert): string {
    return `L'IA a détecté : ${alert.iaValue}`;
  }

  readonly conventionsFrance = [
    { value: 'METALLURGIE', label: 'Métallurgie (IDCC 3248)' },
    { value: 'COMMERCE', label: 'Commerce de détail (IDCC 2216)' },
    { value: 'BTP', label: 'BTP (IDCC 1596)' },
    { value: 'HCR', label: 'Hôtels, cafés, restaurants (IDCC 1979)' },
    { value: 'SYNTEC', label: 'Syntec (IDCC 1486)' },
  ];

  readonly conventionsBelgique = [
    { value: 'CP200', label: 'CP 200 — Employés' },
    { value: 'CP124', label: 'CP 124 — Construction' },
    { value: 'CP302', label: 'CP 302 — Hôtellerie' },
  ];

  get allConventions() {
    return [
      { group: 'France', items: this.conventionsFrance },
      { group: 'Belgique', items: this.conventionsBelgique },
    ];
  }

  constructor(
    private ancienneteService: AncienneteService,
    private snackBar: MatSnackBar,
    @Optional() private refreshService: CaseDashboardRefreshService | null,
  ) {}

  ngOnInit(): void {
    this.aiDataSignal.set(this.aiData);
    this.loadExisting();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['aiData']) {
      this.aiDataSignal.set(this.aiData);
      if (this.showForm() && !this.result()) {
        this.prefillFromAi();
      }
    }
  }

  toggleCollapsed(): void {
    this.collapsed.update(v => !v);
  }

  loadExisting(): void {
    this.loading.set(true);
    this.ancienneteService.get(this.caseFileId).subscribe({
      next: resp => {
        this.result.set(resp);
        this.prefillForm(resp);
        this.showForm.set(false);
        this.loading.set(false);
      },
      error: () => {
        this.prefillFromAi();
        this.showForm.set(true);
        this.loading.set(false);
      },
    });
  }

  private prefillFromAi(): void {
    if (!this.aiData) return;
    if (this.aiData.conventionCollective) this.conventionCode.set(this.aiData.conventionCollective);
    if (this.aiData.dateEntree) this.dateEntree.set(this.aiData.dateEntree);
    if (this.aiData.salaireBrutMensuel) this.salaireBase.set(this.aiData.salaireBrutMensuel);
    if (this.aiData.congesContractuels != null) this.congesContrat.set(this.aiData.congesContractuels);
    if (this.aiData.primeAncienneteContractuelle != null) this.primeContrat.set(this.aiData.primeAncienneteContractuelle);
  }

  calculate(): void {
    this.calculating.set(true);
    this.ancienneteService.calculate(this.caseFileId, {
      conventionCode: this.conventionCode(),
      dateEntree: this.dateEntree(),
      salaireBase: this.salaireBase(),
      congesContrat: this.congesContrat(),
      primeContrat: this.primeContrat(),
    }).subscribe({
      next: resp => {
        this.result.set(resp);
        this.showForm.set(false);
        this.calculating.set(false);
        this.refreshService?.triggerRefresh();
      },
      error: () => {
        this.calculating.set(false);
        this.snackBar.open('Erreur lors du calcul', 'Fermer', { duration: 4000 });
      },
    });
  }

  editForm(): void {
    const r = this.result();
    if (r) this.prefillForm(r);
    this.showForm.set(true);
  }

  private prefillForm(resp: AncienneteResponse): void {
    this.conventionCode.set(resp.conventionCode);
    if (resp.dateEntree) this.dateEntree.set(resp.dateEntree);
    if (resp.salaireBase != null) this.salaireBase.set(resp.salaireBase);
    if (resp.congesContrat != null) this.congesContrat.set(resp.congesContrat);
    if (resp.primeContrat != null) this.primeContrat.set(resp.primeContrat);
  }
}

function dateDaysDiff(a: string, b: string): number | null {
  const ta = Date.parse(a);
  const tb = Date.parse(b);
  if (Number.isNaN(ta) || Number.isNaN(tb)) return null;
  return Math.abs(ta - tb) / 86400000;
}

function percentDiff(iaValue: number, userValue: number): number {
  const base = Math.max(Math.abs(iaValue), 1);
  return Math.abs(userValue - iaValue) / base;
}
