import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  Input,
  OnChanges,
  OnInit,
  Optional,
  SimpleChanges,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { PrestationCompensatoireSf216Service } from '../../core/services/prestation-compensatoire-sf216.service';
import {
  FormePrestation,
  PrestationCompensatoireRequest,
  PrestationCompensatoireResponse,
} from '../../core/models/prestation-compensatoire-sf216.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { PrestationCompensatoireSectionPrefillRules } from './prestation-compensatoire-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-216-02 : composant Angular standalone pour l'outil décisionnel
 * "Prestation compensatoire" (F-FA-01).
 *
 * FRANCE uniquement (art. 270-281 Cciv — mécanisme franco-français qui
 * compense la disparité de niveaux de vie entre époux à la dissolution
 * du mariage par divorce).
 *
 * Remplace le wrapper présentationnel SF-198-01 (qui ne faisait qu'afficher
 * `synthesis.prestationCompensatoireEstimate`) par un vrai simulateur qui
 * appelle le backend SF-216-01 (table `prestation_compensatoire_analyses_sf216`).
 *
 * Pattern de référence : `resiliation-judiciaire-cph-section` (SF-206-08).
 *
 * Formulaire (9 champs) :
 *  - dureeMariageAnnees (obligatoire, ≥ 0)
 *  - revenusAnnuelsEpoux1Eur (obligatoire, ≥ 0)
 *  - revenusAnnuelsEpoux2Eur (obligatoire, ≥ 0)
 *  - patrimoinePropre1Eur (facultatif, ≥ 0)
 *  - patrimoinePropre2Eur (facultatif, ≥ 0)
 *  - ageEpoux1Annees (obligatoire, ≥ 0)
 *  - ageEpoux2Annees (obligatoire, ≥ 0)
 *  - formePrestationDemandee (enum CAPITAL/RENTE/RENTE_CONVERTIBLE/INCERTAIN)
 *  - avantageMatrimonialDetecte (booléen)
 *
 * Verdict affiché :
 *  - montantCapitalIndicatifEur
 *  - renteIndicativeMensuelleEur
 *  - formeRecommandee
 *  - disparité niveaux de vie (texte)
 *  - base juridique
 *  - messages / alertes
 *
 * Country gate : `workspaceCountry !== 'FRANCE'` → bannière info + aucun appel.
 *
 * OnPush + ChangeDetectorRef.markForCheck() dans next/error des subscribe
 * (mémoire `feedback_onpush_subscribe_markforcheck`).
 */
@Component({
  selector: 'app-prestation-compensatoire-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatSelectModule, MatSlideToggleModule,
    MatProgressSpinnerModule,
    LegalCitationsPipe,
    ToolJurisprudenceCitationsComponent,
  ],
  templateUrl: './prestation-compensatoire-section.component.html',
  styleUrl: './prestation-compensatoire-section.component.scss',
})
export class PrestationCompensatoireSectionComponent implements OnInit, OnChanges {
  // F-JU-03 SF-JU-03-99e v3 — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-FA-01-prestation-compensatoire';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommée par le panel pour la card.
  static readonly TOOL_LABEL = 'PRESTATION COMPENSATOIRE';
  static readonly TOOL_ICON = 'balance';

  /**
   * SF-216-02 — délégué au helper partagé (parité stricte avec `prefillFromAi`).
   * Retourne le nombre de champs pré-remplissables FR (6 champs IA).
   */
  static getPrefillCount(input: {
    aiData?: FamilleExtractedData | null;
    workspaceCountry?: string;
  }): number {
    return PrestationCompensatoireSectionPrefillRules.computePrefillCount({
      aiData: input.aiData,
      workspaceCountry: input.workspaceCountry,
    });
  }

  @Input() caseFileId!: string;
  // F-177 SF-177-03b : force l'expansion (mode modal F-177).
  @Input() forceExpanded = false;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';
  // Pré-fill IA — branché sur `synthesis.familleExtractedData`.
  @Input() aiData?: FamilleExtractedData | null;
  /** Mode simulateur autonome (hors dossier) — coupe le refresh dashboard. */
  @Input() standaloneMode = false;

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<PrestationCompensatoireResponse | null>(null);

  // --- Form fields (9 champs du contrat backend SF-216-01) ---
  dureeMariageAnnees = signal<number | null>(null);
  revenusAnnuelsEpoux1Eur = signal<number | null>(null);
  revenusAnnuelsEpoux2Eur = signal<number | null>(null);
  patrimoinePropre1Eur = signal<number | null>(null);
  patrimoinePropre2Eur = signal<number | null>(null);
  ageEpoux1Annees = signal<number | null>(null);
  ageEpoux2Annees = signal<number | null>(null);
  formePrestationDemandee = signal<FormePrestation | null>(null);
  avantageMatrimonialDetecte = signal<boolean>(false);

  // Provenance IA par champ — `'IA'` tant que la valeur vient de l'analyse ;
  // remis à null dès qu'un handler `onXxxChange()` détecte une modification.
  provenanceDureeMariage = signal<'IA' | null>(null);
  provenanceRevenusEpoux1 = signal<'IA' | null>(null);
  provenanceRevenusEpoux2 = signal<'IA' | null>(null);
  provenanceAgeEpoux1 = signal<'IA' | null>(null);
  provenanceAgeEpoux2 = signal<'IA' | null>(null);
  provenanceAvantageMatrimonial = signal<'IA' | null>(null);

  /** Options du select `formePrestationDemandee`. */
  readonly formesPrestation: ReadonlyArray<{ value: FormePrestation; label: string }> = [
    { value: 'CAPITAL', label: 'Capital (art. 274 Cciv)' },
    { value: 'RENTE', label: 'Rente viagère (art. 276 Cciv)' },
    { value: 'RENTE_CONVERTIBLE', label: 'Rente convertible en capital' },
    { value: 'INCERTAIN', label: 'Incertain / à arbitrer par le juge' },
  ];

  constructor(
    private service: PrestationCompensatoireSf216Service,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef,
    @Optional() private dashboardRefresh: CaseDashboardRefreshService | null,
  ) {}

  ngOnInit(): void {
    if (this.forceExpanded) this.collapsed.set(false);
    this.prefillFromAi();
    if (this.workspaceCountry === 'FRANCE') {
      this.load();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['forceExpanded'] && this.forceExpanded) this.collapsed.set(false);
    if (changes['aiData'] && !changes['aiData'].firstChange
        && this.showForm() && !this.result()) {
      this.prefillFromAi();
    }
  }

  /**
   * Pré-fill IA SF-216-02 — renseigne les 6 champs (durée mariage, revenus 1/2,
   * âges 1/2, avantage matrimonial) depuis `familleExtractedData`. Délègue au
   * helper partagé (parité stricte avec `getPrefillCount()`). No-op gracieux
   * si `aiData` absent ou dossier BE.
   */
  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    const ai = this.aiData;
    if (!ai) return;

    const rules = PrestationCompensatoireSectionPrefillRules;
    const ruleInput = { aiData: ai, workspaceCountry: this.workspaceCountry };

    const duree = rules.computeDureeMariageAnnees(ruleInput);
    if (duree !== null) {
      this.dureeMariageAnnees.set(duree);
      this.provenanceDureeMariage.set('IA');
    }
    const r1 = rules.computeRevenusEpoux1(ruleInput);
    if (r1 !== null) {
      this.revenusAnnuelsEpoux1Eur.set(r1);
      this.provenanceRevenusEpoux1.set('IA');
    }
    const r2 = rules.computeRevenusEpoux2(ruleInput);
    if (r2 !== null) {
      this.revenusAnnuelsEpoux2Eur.set(r2);
      this.provenanceRevenusEpoux2.set('IA');
    }
    const a1 = rules.computeAgeEpoux1(ruleInput);
    if (a1 !== null) {
      this.ageEpoux1Annees.set(a1);
      this.provenanceAgeEpoux1.set('IA');
    }
    const a2 = rules.computeAgeEpoux2(ruleInput);
    if (a2 !== null) {
      this.ageEpoux2Annees.set(a2);
      this.provenanceAgeEpoux2.set('IA');
    }
    const av = rules.computeAvantageMatrimonial(ruleInput);
    if (av !== null) {
      this.avantageMatrimonialDetecte.set(av);
      this.provenanceAvantageMatrimonial.set('IA');
    }
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  editMode(): void {
    this.showForm.set(true);
  }

  /** Form valide : FR + 5 champs obligatoires renseignés et ≥ 0. */
  formValid(): boolean {
    if (this.workspaceCountry !== 'FRANCE') return false;
    const required: Array<number | null> = [
      this.dureeMariageAnnees(),
      this.revenusAnnuelsEpoux1Eur(),
      this.revenusAnnuelsEpoux2Eur(),
      this.ageEpoux1Annees(),
      this.ageEpoux2Annees(),
    ];
    for (const v of required) {
      if (v === null || !Number.isFinite(v) || v < 0) return false;
    }
    const p1 = this.patrimoinePropre1Eur();
    if (p1 !== null && (!Number.isFinite(p1) || p1 < 0)) return false;
    const p2 = this.patrimoinePropre2Eur();
    if (p2 !== null && (!Number.isFinite(p2) || p2 < 0)) return false;
    return true;
  }

  // --- Handlers — toute modification manuelle efface le badge IA ---

  onDureeMariageChange(value: number | string | null): void {
    this.dureeMariageAnnees.set(this.parseNonNegativeInt(value));
    this.provenanceDureeMariage.set(null);
  }

  onRevenusEpoux1Change(value: number | string | null): void {
    this.revenusAnnuelsEpoux1Eur.set(this.parseNonNegativeInt(value));
    this.provenanceRevenusEpoux1.set(null);
  }

  onRevenusEpoux2Change(value: number | string | null): void {
    this.revenusAnnuelsEpoux2Eur.set(this.parseNonNegativeInt(value));
    this.provenanceRevenusEpoux2.set(null);
  }

  onPatrimoine1Change(value: number | string | null): void {
    this.patrimoinePropre1Eur.set(this.parseNonNegativeInt(value));
  }

  onPatrimoine2Change(value: number | string | null): void {
    this.patrimoinePropre2Eur.set(this.parseNonNegativeInt(value));
  }

  onAgeEpoux1Change(value: number | string | null): void {
    this.ageEpoux1Annees.set(this.parseNonNegativeInt(value));
    this.provenanceAgeEpoux1.set(null);
  }

  onAgeEpoux2Change(value: number | string | null): void {
    this.ageEpoux2Annees.set(this.parseNonNegativeInt(value));
    this.provenanceAgeEpoux2.set(null);
  }

  onFormePrestationChange(value: FormePrestation | null): void {
    this.formePrestationDemandee.set(value);
  }

  onAvantageMatrimonialChange(value: boolean): void {
    this.avantageMatrimonialDetecte.set(value);
    this.provenanceAvantageMatrimonial.set(null);
  }

  private parseNonNegativeInt(value: number | string | null | undefined): number | null {
    if (value === null || value === undefined || value === '') return null;
    const num = typeof value === 'number' ? value : Number.parseInt(String(value), 10);
    if (!Number.isFinite(num) || num < 0) return null;
    return num;
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request: PrestationCompensatoireRequest = {
      dureeMariageAnnees: this.dureeMariageAnnees(),
      revenusAnnuelsEpoux1Eur: this.revenusAnnuelsEpoux1Eur(),
      revenusAnnuelsEpoux2Eur: this.revenusAnnuelsEpoux2Eur(),
      patrimoinePropre1Eur: this.patrimoinePropre1Eur(),
      patrimoinePropre2Eur: this.patrimoinePropre2Eur(),
      ageEpoux1Annees: this.ageEpoux1Annees(),
      ageEpoux2Annees: this.ageEpoux2Annees(),
      formePrestationDemandee: this.formePrestationDemandee(),
      avantageMatrimonialDetecte: this.avantageMatrimonialDetecte(),
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.applyResult(r);
        this.calculating.set(false);
        this.snackBar.open('Prestation compensatoire calculée', 'OK', { duration: 2500 });
        if (!this.standaloneMode) this.dashboardRefresh?.triggerRefresh();
        // OnPush + subscribe : forcer la détection de changement.
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.calculating.set(false);
        const msg = err?.error?.message || err?.error || 'Erreur lors du calcul';
        this.snackBar.open(String(msg), 'Fermer', { duration: 5000, panelClass: 'snack-error' });
        this.cdr.markForCheck();
      },
    });
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.applyResult(r);
        this.loading.set(false);
        this.cdr.markForCheck();
      },
      error: () => {
        // 404 attendu si aucune analyse — on reste en mode formulaire.
        this.loading.set(false);
        this.cdr.markForCheck();
      },
    });
  }

  private applyResult(r: PrestationCompensatoireResponse): void {
    this.result.set(r);
    this.showForm.set(false);
  }

  // ---------------------------------------------------------------------------
  // Helpers d'affichage du résultat
  // ---------------------------------------------------------------------------

  /** Forme recommandée → libellé humain. */
  formeRecommandeeLabel(forme: FormePrestation): string {
    switch (forme) {
      case 'CAPITAL': return 'Capital (art. 274 Cciv)';
      case 'RENTE': return 'Rente viagère (art. 276 Cciv)';
      case 'RENTE_CONVERTIBLE': return 'Rente convertible en capital';
      case 'INCERTAIN': return 'Incertain — à arbitrer par le juge';
    }
  }

  formeRecommandeeClass(forme: FormePrestation): string {
    switch (forme) {
      case 'CAPITAL': return 'pc-forme-pill pc-forme-pill--capital';
      case 'RENTE':
      case 'RENTE_CONVERTIBLE': return 'pc-forme-pill pc-forme-pill--rente';
      case 'INCERTAIN': return 'pc-forme-pill pc-forme-pill--incertain';
    }
  }

  formatEuros(amount: number): string {
    return new Intl.NumberFormat('fr-FR', {
      style: 'currency',
      currency: 'EUR',
      maximumFractionDigits: 0,
    }).format(amount);
  }
}
