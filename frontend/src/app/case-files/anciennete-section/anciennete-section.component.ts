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
import { BaremeService } from '../../core/services/bareme.service';
import { BaremeResponse } from '../../core/models/bareme.model';
import { SourceExplanationService } from '../../core/services/source-explanation.service';
import { SourceExplanation } from '../../core/models/source-explanation.model';
import { CoherencePopoverTriggerDirective } from '../../shared/coherence-popover/coherence-popover-trigger.directive';

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
    CoherencePopoverTriggerDirective,
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
    if (!this.showForm()) return {};
    const alerts: Partial<Record<AncienneteAlertField, AncienneteCoherenceAlert>> = {};
    const bareme = this.currentBareme();

    // Convention — exact match upper-case (IA uniquement)
    if (ai?.conventionCollective && this.conventionCode()) {
      if (ai.conventionCollective.toUpperCase() !== this.conventionCode().toUpperCase()) {
        alerts.CONVENTION = { field: 'CONVENTION', iaValue: ai.conventionCollective };
      }
    }

    // Date entrée — écart ≥ 15 jours (IA uniquement)
    if (ai?.dateEntree && this.dateEntree()) {
      const diff = dateDaysDiff(ai.dateEntree, this.dateEntree());
      if (diff !== null && diff >= 15) {
        alerts.DATE_ENTREE = { field: 'DATE_ENTREE', iaValue: ai.dateEntree };
      }
    }

    // Salaire — écart relatif ≥ 5 % (IA uniquement)
    if (ai?.salaireBrutMensuel != null && this.salaireBase() > 0) {
      const rel = percentDiff(ai.salaireBrutMensuel, this.salaireBase());
      if (rel >= 0.05) {
        alerts.SALAIRE = { field: 'SALAIRE', iaValue: `${ai.salaireBrutMensuel} €` };
      }
    }

    // Congés — écart ≥ 1 jour. Référence : IA si fournie, sinon bareme.
    if (this.congesContrat() > 0) {
      if (ai?.congesContractuels != null) {
        if (Math.abs(ai.congesContractuels - this.congesContrat()) >= 1) {
          alerts.CONGES = { field: 'CONGES', iaValue: `${ai.congesContractuels} j` };
        }
      } else if (bareme) {
        const total = BaremeService.totalConges(bareme, this.computeAnneesAnciennete());
        if (total > 0 && this.congesContrat() < total) {
          alerts.CONGES = { field: 'CONGES', iaValue: `${total} j (convention)` };
        }
      }
    }

    // Prime — écart ≥ 0,5 pt. Référence : IA si fournie, sinon bareme.
    if (this.primeContrat() > 0) {
      if (ai?.primeAncienneteContractuelle != null) {
        if (Math.abs(ai.primeAncienneteContractuelle - this.primeContrat()) >= 0.5) {
          alerts.PRIME = { field: 'PRIME', iaValue: `${ai.primeAncienneteContractuelle} %` };
        }
      } else if (bareme) {
        const pct = BaremeService.maxPrimePourcentage(bareme, this.computeAnneesAnciennete());
        if (pct > 0 && this.primeContrat() + 0.5 <= pct) {
          alerts.PRIME = { field: 'PRIME', iaValue: `${pct} % (convention)` };
        }
      }
    }

    return alerts;
  });

  alertsSummary = computed(() => ({ total: Object.keys(this.coherenceAlerts()).length }));

  alertTooltip(alert: AncienneteCoherenceAlert): string {
    return `Détecté : ${alert.iaValue}`;
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

  // SF-DT-07-05 — bareme courant, chargé pour prefill et alerte vs convention
  private currentBareme = signal<BaremeResponse | null>(null);

  // SF-IA-03-15a — map {sourceKey → explanation} pour le popover enrichi
  sourceExplanations = signal<Map<string, SourceExplanation>>(new Map());

  constructor(
    private ancienneteService: AncienneteService,
    private baremeService: BaremeService,
    private sourceExplanationService: SourceExplanationService,
    private snackBar: MatSnackBar,
    @Optional() private refreshService: CaseDashboardRefreshService | null,
  ) {}

  ngOnInit(): void {
    this.aiDataSignal.set(this.aiData);
    this.loadExisting();
    this.loadSourceExplanations();
  }

  private loadSourceExplanations(): void {
    if (!this.caseFileId) return;
    this.sourceExplanationService.getForCaseFile(this.caseFileId).subscribe({
      next: map => this.sourceExplanations.set(map),
      error: () => { /* fail-open : pas de popover enrichi, tombe en fallback */ },
    });
  }

  /** SF-IA-03-15a : mapping champ → sourceKey. Si alerte vs IA, sourceKey direct ; si alerte vs bareme, convention_collective. */
  sourceKeyFor(field: AncienneteAlertField): string {
    const ai = this.aiDataSignal();
    switch (field) {
      case 'CONVENTION': return 'convention_collective';
      case 'DATE_ENTREE': return 'date_entree';
      case 'SALAIRE': return 'salaire_brut_mensuel';
      case 'CONGES':
        return ai?.congesContractuels != null ? 'conges_contractuels' : 'convention_collective';
      case 'PRIME':
        return ai?.primeAncienneteContractuelle != null ? 'prime_anciennete_contractuelle' : 'convention_collective';
    }
  }

  explanationFor(field: AncienneteAlertField): SourceExplanation | null {
    return this.sourceExplanations().get(this.sourceKeyFor(field)) ?? null;
  }

  /** SF-IA-03-15a : phrase de "reason" fallback si aucune explication Haiku disponible. */
  reasonFor(field: AncienneteAlertField): string {
    const alert = this.coherenceAlerts()[field];
    if (!alert) return '';
    switch (field) {
      case 'CONVENTION': return `La convention attendue est ${alert.iaValue}.`;
      case 'DATE_ENTREE': return `La date d'entrée détectée est ${alert.iaValue}.`;
      case 'SALAIRE': return `Le salaire brut mensuel détecté est ${alert.iaValue}.`;
      case 'CONGES': return `Le nombre de jours de congés attendu est ${alert.iaValue}.`;
      case 'PRIME': return `Le pourcentage de prime attendu est ${alert.iaValue}.`;
    }
  }


  ngOnChanges(changes: SimpleChanges): void {
    if (changes['aiData']) {
      this.aiDataSignal.set(this.aiData);
      if (this.showForm() && !this.result()) {
        this.prefillFromAi();
      }
      // SF-IA-03-18 : l'arrivée d'un nouveau aiData signale une analyse fraîche → re-fetch.
      if (!changes['aiData'].firstChange) {
        this.loadSourceExplanations();
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
    if (!this.aiData) {
      // Pas d'IA mais on peut quand même charger le bareme par défaut.
      this.loadBaremeAndPrefill();
      return;
    }
    if (this.aiData.conventionCollective) this.conventionCode.set(this.aiData.conventionCollective);
    if (this.aiData.dateEntree) this.dateEntree.set(this.aiData.dateEntree);
    if (this.aiData.salaireBrutMensuel) this.salaireBase.set(this.aiData.salaireBrutMensuel);
    if (this.aiData.congesContractuels != null) this.congesContrat.set(this.aiData.congesContractuels);
    if (this.aiData.primeAncienneteContractuelle != null) this.primeContrat.set(this.aiData.primeAncienneteContractuelle);
    // SF-DT-07-05 : charger le bareme et compléter prefill pour les champs qu'IA n'a pas extraits.
    this.loadBaremeAndPrefill();
  }

  /** SF-DT-07-05 : charge le bareme courant et pré-remplit prime/congés depuis convention si IA n'a rien fourni. */
  private loadBaremeAndPrefill(): void {
    const code = this.conventionCode();
    if (!code) return;
    this.baremeService.get(code).subscribe({
      next: bareme => {
        this.currentBareme.set(bareme);
        // Calcule l'ancienneté approximative à partir de la dateEntree.
        const annees = this.computeAnneesAnciennete();
        // Si IA n'a pas fourni primeContrat, prefill depuis bareme.
        if (this.aiData?.primeAncienneteContractuelle == null && this.primeContrat() === 0) {
          const baremePct = BaremeService.maxPrimePourcentage(bareme, annees);
          if (baremePct > 0) this.primeContrat.set(baremePct);
        }
        // Si IA n'a pas fourni congesContrat, prefill depuis bareme.
        if (this.aiData?.congesContractuels == null && this.congesContrat() === 25) {
          const totalConges = BaremeService.totalConges(bareme, annees);
          if (totalConges > 0) this.congesContrat.set(totalConges);
        }
      },
      error: () => { /* convention inconnue côté bareme — on garde les défauts */ },
    });
  }

  private computeAnneesAnciennete(): number {
    const d = this.dateEntree();
    if (!d) return 0;
    const entree = new Date(d);
    if (Number.isNaN(entree.getTime())) return 0;
    const now = new Date();
    return Math.floor((now.getTime() - entree.getTime()) / (1000 * 60 * 60 * 24 * 365.25));
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
