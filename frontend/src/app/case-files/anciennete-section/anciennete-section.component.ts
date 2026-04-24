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
import { ConventionReferentialService, ConventionOption } from '../../core/services/convention-referential.service';
import { SourceExplanationService } from '../../core/services/source-explanation.service';
import { SourceExplanation } from '../../core/models/source-explanation.model';
import { CoherencePopoverTriggerDirective } from '../../shared/coherence-popover/coherence-popover-trigger.directive';
import { CoherenceAlert } from '../../shared/coherence-popover/coherence-alert.model';
import { CoherenceAlertBuilder } from '../../shared/coherence-popover/coherence-alert-builder';

export type AncienneteAlertField = 'CONVENTION' | 'DATE_ENTREE' | 'SALAIRE' | 'CONGES' | 'PRIME';

// SF-155-14 : alias local rétro-compat — utilise l'interface générique partagée
// (`CoherenceAlertBuilder` / `CoherenceAlert<F>` — pattern canonique F-IM-05).
export type AncienneteCoherenceAlert = CoherenceAlert<AncienneteAlertField>;

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

  // SF-155-14 : provenance IA par champ (badge "Pré-rempli depuis l'analyse").
  // Effacé dès que l'avocat modifie manuellement un champ (onXxxChange).
  provenanceConvention = signal<'IA' | null>(null);
  provenanceDateEntree = signal<'IA' | null>(null);
  provenanceSalaire = signal<'IA' | null>(null);
  provenanceConges = signal<'IA' | null>(null);
  provenancePrime = signal<'IA' | null>(null);

  coherenceAlerts = computed<Partial<Record<AncienneteAlertField, AncienneteCoherenceAlert>>>(() => {
    const ai = this.aiDataSignal();
    if (!this.showForm()) return {};
    const alerts: Partial<Record<AncienneteAlertField, AncienneteCoherenceAlert>> = {};
    const bareme = this.currentBareme();

    // Convention — exact match upper-case (IA uniquement)
    if (ai?.conventionCollective && this.conventionCode()) {
      if (ai.conventionCollective.toUpperCase() !== this.conventionCode().toUpperCase()) {
        const a = CoherenceAlertBuilder.forField<AncienneteAlertField>('CONVENTION')
          .addSource('IA', {
            expectedDisplay: ai.conventionCollective,
            reason: `Analyse du dossier : convention ${ai.conventionCollective}`,
          })
          .build();
        if (a) alerts.CONVENTION = a;
      }
    }

    // Date entrée — écart ≥ 15 jours (IA uniquement)
    if (ai?.dateEntree && this.dateEntree()) {
      const diff = dateDaysDiff(ai.dateEntree, this.dateEntree());
      if (diff !== null && diff >= 15) {
        const a = CoherenceAlertBuilder.forField<AncienneteAlertField>('DATE_ENTREE')
          .addSource('IA', {
            expectedDisplay: ai.dateEntree,
            reason: `Analyse du dossier : date d'entrée ${ai.dateEntree}`,
          })
          .build();
        if (a) alerts.DATE_ENTREE = a;
      }
    }

    // Salaire — écart relatif ≥ 5 % (IA uniquement)
    if (ai?.salaireBrutMensuel != null && this.salaireBase() > 0) {
      const rel = percentDiff(ai.salaireBrutMensuel, this.salaireBase());
      if (rel >= 0.05) {
        const display = `${ai.salaireBrutMensuel} €`;
        const a = CoherenceAlertBuilder.forField<AncienneteAlertField>('SALAIRE')
          .addSource('IA', {
            expectedDisplay: display,
            reason: `Analyse du dossier : salaire brut mensuel ${display}`,
          })
          .build();
        if (a) alerts.SALAIRE = a;
      }
    }

    // Congés — écart ≥ 1 jour. Référence : IA si fournie, sinon bareme.
    if (this.congesContrat() > 0) {
      if (ai?.congesContractuels != null) {
        if (Math.abs(ai.congesContractuels - this.congesContrat()) >= 1) {
          const display = `${ai.congesContractuels} j`;
          const a = CoherenceAlertBuilder.forField<AncienneteAlertField>('CONGES')
            .addSource('IA', {
              expectedDisplay: display,
              reason: `Analyse du dossier : congés contractuels ${display}`,
            })
            .build();
          if (a) alerts.CONGES = a;
        }
      } else if (bareme) {
        const total = BaremeService.totalConges(bareme, this.computeAnneesAnciennete());
        if (total > 0 && this.congesContrat() < total) {
          const display = `${total} j (convention)`;
          const a = CoherenceAlertBuilder.forField<AncienneteAlertField>('CONGES')
            .addSource('IA', {
              expectedDisplay: display,
              reason: `Barème convention : ${display}`,
            })
            .build();
          if (a) alerts.CONGES = a;
        }
      }
    }

    // Prime — écart ≥ 0,5 pt. Référence : IA si fournie, sinon bareme.
    if (this.primeContrat() > 0) {
      if (ai?.primeAncienneteContractuelle != null) {
        if (Math.abs(ai.primeAncienneteContractuelle - this.primeContrat()) >= 0.5) {
          const display = `${ai.primeAncienneteContractuelle} %`;
          const a = CoherenceAlertBuilder.forField<AncienneteAlertField>('PRIME')
            .addSource('IA', {
              expectedDisplay: display,
              reason: `Analyse du dossier : prime ancienneté ${display}`,
            })
            .build();
          if (a) alerts.PRIME = a;
        }
      } else if (bareme) {
        const pct = BaremeService.maxPrimePourcentage(bareme, this.computeAnneesAnciennete());
        if (pct > 0 && this.primeContrat() + 0.5 <= pct) {
          const display = `${pct} % (convention)`;
          const a = CoherenceAlertBuilder.forField<AncienneteAlertField>('PRIME')
            .addSource('IA', {
              expectedDisplay: display,
              reason: `Barème convention : ${display}`,
            })
            .build();
          if (a) alerts.PRIME = a;
        }
      }
    }

    return alerts;
  });

  alertsSummary = computed(() => ({ total: Object.keys(this.coherenceAlerts()).length }));

  alertTooltip(alert: AncienneteCoherenceAlert): string {
    return `Détecté : ${alert.expectedDisplay}`;
  }

  // SF-129-01 : liste dynamique chargée depuis le backend (legal_referentials seedée
  // avec 49 CCN FR top effectif + 3 CP BE). Fallback statique via le service.
  conventionsFrance = signal<{ value: string; label: string }[]>([]);
  conventionsBelgique = signal<{ value: string; label: string }[]>([]);

  get allConventions() {
    return [
      { group: 'France', items: this.conventionsFrance() },
      { group: 'Belgique', items: this.conventionsBelgique() },
    ];
  }

  /** SF-130-01 : afficher le badge "Déduit du net" si l'IA a marqué le flag
   *  ET si un salaire brut est effectivement présent (sinon rien à justifier). */
  readonly showSalaireDeduitBadge = computed(() => {
    const ai = this.aiDataSignal();
    return ai?.salaireEstDeduit === true
        && ai?.salaireBrutMensuel != null
        && this.salaireBase() > 0;
  });

  /** SF-129-01 : valeur IA non présente dans le référentiel → badge informatif. */
  readonly aiConventionUnknown = computed(() => {
    const ai = this.aiDataSignal();
    if (!ai?.conventionCollective) return null;
    const normalized = ConventionReferentialService.normalizeCode(ai.conventionCollective);
    if (!normalized) return null;
    const all = [...this.conventionsFrance(), ...this.conventionsBelgique()];
    const match = all.find(o => o.value.toUpperCase() === normalized);
    return match ? null : ai.conventionCollective;
  });

  // SF-DT-07-05 — bareme courant, chargé pour prefill et alerte vs convention
  private currentBareme = signal<BaremeResponse | null>(null);

  // SF-IA-03-15a — map {sourceKey → explanation} pour le popover enrichi
  sourceExplanations = signal<Map<string, SourceExplanation[]>>(new Map());

  constructor(
    private ancienneteService: AncienneteService,
    private baremeService: BaremeService,
    private sourceExplanationService: SourceExplanationService,
    private conventionRefService: ConventionReferentialService,
    private snackBar: MatSnackBar,
    @Optional() private refreshService: CaseDashboardRefreshService | null,
  ) {}

  ngOnInit(): void {
    this.aiDataSignal.set(this.aiData);
    // SF-155-14 : garde-fou — pré-remplit au mount si aiData est déjà disponible
    // (cas : pipeline déjà tourné avant ouverture du dossier). Aligné sur pattern
    // canonique `immigration-title-decision-section` (F-IM-05).
    this.prefillFromAi();
    this.loadConventions();
    this.loadExisting();
    this.loadSourceExplanations();
  }

  private loadConventions(): void {
    this.conventionRefService.list().subscribe(options => {
      this.conventionsFrance.set(
        options.filter(o => o.country === 'FRANCE').map(o => ({ value: o.code, label: o.label }))
      );
      this.conventionsBelgique.set(
        options.filter(o => o.country === 'BELGIQUE').map(o => ({ value: o.code, label: o.label }))
      );
      // Si l'aiData est déjà là et qu'un match existe, re-trigger la pré-sélection
      this.maybePreselectFromAi();
    });
  }

  private maybePreselectFromAi(): void {
    const ai = this.aiDataSignal();
    if (!ai?.conventionCollective) return;
    const normalized = ConventionReferentialService.normalizeCode(ai.conventionCollective);
    if (!normalized) return;
    const all = [...this.conventionsFrance(), ...this.conventionsBelgique()];
    const match = all.find(o => o.value.toUpperCase() === normalized);
    if (match) {
      this.conventionCode.set(match.value);
      this.provenanceConvention.set('IA');
    }
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

  explanationFor(field: AncienneteAlertField): SourceExplanation[] {
    return this.sourceExplanations().get(this.sourceKeyFor(field)) ?? [];
  }

  /** SF-IA-03-15a : phrase de "reason" fallback si aucune explication Haiku disponible. */
  reasonFor(field: AncienneteAlertField): string {
    const alert = this.coherenceAlerts()[field];
    if (!alert) return '';
    switch (field) {
      case 'CONVENTION': return `La convention attendue est ${alert.expectedDisplay}.`;
      case 'DATE_ENTREE': return `La date d'entrée détectée est ${alert.expectedDisplay}.`;
      case 'SALAIRE': return `Le salaire brut mensuel détecté est ${alert.expectedDisplay}.`;
      case 'CONGES': return `Le nombre de jours de congés attendu est ${alert.expectedDisplay}.`;
      case 'PRIME': return `Le pourcentage de prime attendu est ${alert.expectedDisplay}.`;
    }
  }

  // SF-155-14 : handlers qui effacent le badge IA au changement manuel avocat.
  onConventionChange(): void { this.provenanceConvention.set(null); }
  onDateEntreeChange(): void { this.provenanceDateEntree.set(null); }
  onSalaireChange(): void { this.provenanceSalaire.set(null); }
  onCongesChange(): void { this.provenanceConges.set(null); }
  onPrimeChange(): void { this.provenancePrime.set(null); }


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
    // SF-155-14 : pour chaque field IA extrait, applique la valeur + marque provenance='IA'.
    // Le badge `auto_awesome` "Pré-rempli depuis l'analyse" s'affiche dans le template.
    if (this.aiData.conventionCollective) {
      // SF-129-01 : normaliser (ex. "3043" → "IDCC_3043", "METALLURGIE" → "IDCC_3248")
      const normalized = ConventionReferentialService.normalizeCode(this.aiData.conventionCollective);
      if (normalized) {
        this.conventionCode.set(normalized);
        this.provenanceConvention.set('IA');
      }
    }
    if (this.aiData.dateEntree) {
      this.dateEntree.set(this.aiData.dateEntree);
      this.provenanceDateEntree.set('IA');
    }
    if (this.aiData.salaireBrutMensuel) {
      this.salaireBase.set(this.aiData.salaireBrutMensuel);
      this.provenanceSalaire.set('IA');
    }
    if (this.aiData.congesContractuels != null) {
      this.congesContrat.set(this.aiData.congesContractuels);
      this.provenanceConges.set('IA');
    }
    if (this.aiData.primeAncienneteContractuelle != null) {
      this.primeContrat.set(this.aiData.primeAncienneteContractuelle);
      this.provenancePrime.set('IA');
    }
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
    // SF-155-14 : valeurs persistées — efface les badges IA (ces valeurs
    // ont été validées par l'avocat au calcul précédent, pas pré-remplies).
    this.conventionCode.set(resp.conventionCode);
    this.provenanceConvention.set(null);
    if (resp.dateEntree) { this.dateEntree.set(resp.dateEntree); this.provenanceDateEntree.set(null); }
    if (resp.salaireBase != null) { this.salaireBase.set(resp.salaireBase); this.provenanceSalaire.set(null); }
    if (resp.congesContrat != null) { this.congesContrat.set(resp.congesContrat); this.provenanceConges.set(null); }
    if (resp.primeContrat != null) { this.primeContrat.set(resp.primeContrat); this.provenancePrime.set(null); }
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
