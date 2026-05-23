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
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { LiquidationCommunauteFrService } from '../../core/services/liquidation-communaute-fr.service';
import {
  LiquidationCommunauteFrRequest,
  LiquidationCommunauteFrResponse,
  OccupationLogement,
} from '../../core/models/liquidation-communaute-fr.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';
import { LiquidationCommunauteFrPrefillRules } from './liquidation-communaute-fr-section-prefill-rules';

/**
 * SF-216-06 : composant Angular standalone pour l'outil décisionnel
 * "Liquidation communauté légale" (F-FA-04).
 *
 * FRANCE uniquement (art. 1467-1517 Cciv — dissolution + partage de la masse
 * commune ; art. 1433-1435 Cciv — récompenses entre époux et communauté ;
 * art. 815-832 Cciv — indivision résiduelle).
 *
 * Remplace le wrapper présentationnel SF-198-03 (qui ne faisait qu'afficher
 * `synthesis.liquidationCommunaute`) par un vrai simulateur qui appelle le
 * backend SF-216-05 (table `liquidation_communaute_fr_analyses`).
 *
 * Pattern de référence : `prestation-compensatoire-section` (SF-216-02).
 *
 * Formulaire (11 champs) :
 *  - valeurImmeubleCommun1Eur, valeurImmeubleCommun2Eur (facultatifs, ≥ 0)
 *  - capitalRestantDuEur (facultatif, ≥ 0) — crédit immobilier commun
 *  - valeurMobilierCommunEur, autresActifsCommunsEur (facultatifs, ≥ 0)
 *  - recompensesEpoux1Eur, recompensesEpoux2Eur (facultatifs, ≥ 0)
 *  - biensPropresEpoux1Eur, biensPropresEpoux2Eur (facultatifs, ≥ 0)
 *  - occupationLogementEpoux (enum EPOUX1/EPOUX2/AUCUN/null)
 *  - regimeMatrimonialDetecte (lecture seule, depuis IA)
 *
 * Verdict affiché :
 *  - masseCommuneEur
 *  - quotaPartEpoux1Eur, quotaPartEpoux2Eur
 *  - soulteEur
 *  - alerteIndivision (booléen)
 *  - base juridique
 *  - messages / alertes
 *
 * Country gate : `workspaceCountry !== 'FRANCE'` → bannière info + aucun appel.
 *
 * OnPush + ChangeDetectorRef.markForCheck() dans next/error des subscribe
 * (mémoire `feedback_onpush_subscribe_markforcheck`).
 */
@Component({
  selector: 'app-liquidation-communaute-fr-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatSelectModule,
    MatProgressSpinnerModule,
    LegalCitationsPipe,,
    ToolJurisprudenceCitationsComponent
  ],
  templateUrl: './liquidation-communaute-fr-section.component.html',
  styleUrl: './liquidation-communaute-fr-section.component.scss',
})
export class LiquidationCommunauteFrSectionComponent implements OnInit, OnChanges {
  // F-JU-03 SF-JU-03-05 — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-FA-04-liquidation-communaute';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommée par le panel pour la card.
  static readonly TOOL_LABEL = 'LIQUIDATION DE COMMUNAUTÉ';
  static readonly TOOL_ICON = 'account_balance';

  /**
   * SF-216-06 — délégué au helper partagé (parité stricte avec `prefillFromAi`).
   * Retourne le nombre de champs pré-remplissables FR (2 champs IA — récompenses 1/2).
   */
  static getPrefillCount(input: {
    aiData?: FamilleExtractedData | null;
    workspaceCountry?: string;
  }): number {
    return LiquidationCommunauteFrPrefillRules.computePrefillCount({
      aiData: input.aiData,
      workspaceCountry: input.workspaceCountry,
    });
  }

  @Input() caseFileId!: string;
  @Input() forceExpanded = false;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';
  @Input() aiData?: FamilleExtractedData | null;
  /** Mode simulateur autonome (hors dossier) — coupe le refresh dashboard. */
  @Input() standaloneMode = false;

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<LiquidationCommunauteFrResponse | null>(null);

  // --- Form fields (11 champs du contrat backend SF-216-05) ---
  valeurImmeubleCommun1Eur = signal<number | null>(null);
  valeurImmeubleCommun2Eur = signal<number | null>(null);
  capitalRestantDuEur = signal<number | null>(null);
  valeurMobilierCommunEur = signal<number | null>(null);
  autresActifsCommunsEur = signal<number | null>(null);
  recompensesEpoux1Eur = signal<number | null>(null);
  recompensesEpoux2Eur = signal<number | null>(null);
  biensPropresEpoux1Eur = signal<number | null>(null);
  biensPropresEpoux2Eur = signal<number | null>(null);
  occupationLogementEpoux = signal<OccupationLogement | null>(null);
  // Régime matrimonial : lecture seule, depuis IA — alimente l'alerte UX si != COMMUNAUTE_LEGALE.
  regimeMatrimonialDetecte = signal<string | null>(null);
  // Signal informatif clause attribution intégrale (renvoi vers F-FA-16).
  clauseAttributionIntegraleDetectee = signal<boolean | null>(null);

  // Provenance IA par champ — `'IA'` tant que la valeur vient de l'analyse ;
  // remis à null dès qu'un handler `onXxxChange()` détecte une modification.
  provenanceRecompensesEpoux1 = signal<'IA' | null>(null);
  provenanceRecompensesEpoux2 = signal<'IA' | null>(null);

  /** Options du select `occupationLogementEpoux`. */
  readonly occupationsLogement: ReadonlyArray<{ value: OccupationLogement; label: string }> = [
    { value: 'EPOUX1', label: 'Époux 1 conserve le logement' },
    { value: 'EPOUX2', label: 'Époux 2 conserve le logement' },
    { value: 'AUCUN', label: 'Aucun (vente ou indivision)' },
  ];

  constructor(
    private service: LiquidationCommunauteFrService,
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
   * Pré-fill IA SF-216-06 — renseigne les 2 champs récompenses (1/2) depuis
   * `familleExtractedData`, et lit les signaux informatifs régime + clause
   * attribution intégrale. Délègue au helper partagé. No-op gracieux si
   * `aiData` absent ou dossier BE.
   */
  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    const ai = this.aiData;
    if (!ai) return;

    const rules = LiquidationCommunauteFrPrefillRules;
    const ruleInput = { aiData: ai, workspaceCountry: this.workspaceCountry };

    // Signaux informatifs (régime + clause attribution intégrale) — toujours lus,
    // pas comptés par computePrefillCount.
    this.regimeMatrimonialDetecte.set(rules.computeRegimeMatrimonialDetecte(ruleInput));
    this.clauseAttributionIntegraleDetectee.set(rules.computeClauseAttributionIntegrale(ruleInput));

    // Champs formulaire pré-remplissables (2 — comptés par computePrefillCount).
    const r1 = rules.computeRecompensesEpoux1(ruleInput);
    if (r1 !== null) {
      this.recompensesEpoux1Eur.set(r1);
      this.provenanceRecompensesEpoux1.set('IA');
    }
    const r2 = rules.computeRecompensesEpoux2(ruleInput);
    if (r2 !== null) {
      this.recompensesEpoux2Eur.set(r2);
      this.provenanceRecompensesEpoux2.set('IA');
    }
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  editMode(): void {
    this.showForm.set(true);
  }

  /**
   * Form valide : FR + tous les champs renseignés sont ≥ 0 + régime compatible
   * (null ou COMMUNAUTE_LEGALE).
   */
  formValid(): boolean {
    if (this.workspaceCountry !== 'FRANCE') return false;
    const numericFields: Array<number | null> = [
      this.valeurImmeubleCommun1Eur(),
      this.valeurImmeubleCommun2Eur(),
      this.capitalRestantDuEur(),
      this.valeurMobilierCommunEur(),
      this.autresActifsCommunsEur(),
      this.recompensesEpoux1Eur(),
      this.recompensesEpoux2Eur(),
      this.biensPropresEpoux1Eur(),
      this.biensPropresEpoux2Eur(),
    ];
    for (const v of numericFields) {
      if (v !== null && (!Number.isFinite(v) || v < 0)) return false;
    }
    const regime = this.regimeMatrimonialDetecte();
    if (regime !== null && regime !== 'COMMUNAUTE_LEGALE') return false;
    return true;
  }

  /**
   * Indique si l'outil est dans un état "régime incompatible" — affichage UX
   * d'une bannière dédiée renvoyant l'avocat vers F-FA-15/16/17 selon le régime.
   */
  regimeIncompatible(): boolean {
    const regime = this.regimeMatrimonialDetecte();
    return regime !== null && regime !== 'COMMUNAUTE_LEGALE';
  }

  // --- Handlers — toute modification manuelle efface le badge IA ---

  onImmeuble1Change(value: number | string | null): void {
    this.valeurImmeubleCommun1Eur.set(this.parseNonNegativeInt(value));
  }

  onImmeuble2Change(value: number | string | null): void {
    this.valeurImmeubleCommun2Eur.set(this.parseNonNegativeInt(value));
  }

  onCapitalRestantDuChange(value: number | string | null): void {
    this.capitalRestantDuEur.set(this.parseNonNegativeInt(value));
  }

  onMobilierChange(value: number | string | null): void {
    this.valeurMobilierCommunEur.set(this.parseNonNegativeInt(value));
  }

  onAutresActifsChange(value: number | string | null): void {
    this.autresActifsCommunsEur.set(this.parseNonNegativeInt(value));
  }

  onRecompenses1Change(value: number | string | null): void {
    this.recompensesEpoux1Eur.set(this.parseNonNegativeInt(value));
    this.provenanceRecompensesEpoux1.set(null);
  }

  onRecompenses2Change(value: number | string | null): void {
    this.recompensesEpoux2Eur.set(this.parseNonNegativeInt(value));
    this.provenanceRecompensesEpoux2.set(null);
  }

  onBiensPropres1Change(value: number | string | null): void {
    this.biensPropresEpoux1Eur.set(this.parseNonNegativeInt(value));
  }

  onBiensPropres2Change(value: number | string | null): void {
    this.biensPropresEpoux2Eur.set(this.parseNonNegativeInt(value));
  }

  onOccupationChange(value: OccupationLogement | null): void {
    this.occupationLogementEpoux.set(value);
  }

  private parseNonNegativeInt(value: number | string | null | undefined): number | null {
    if (value === null || value === undefined || value === '') return null;
    const num = typeof value === 'number' ? value : Number.parseInt(String(value), 10);
    if (!Number.isFinite(num) || num < 0) return null;
    return num;
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request: LiquidationCommunauteFrRequest = {
      valeurImmeubleCommun1Eur: this.valeurImmeubleCommun1Eur(),
      valeurImmeubleCommun2Eur: this.valeurImmeubleCommun2Eur(),
      capitalRestantDuEur: this.capitalRestantDuEur(),
      valeurMobilierCommunEur: this.valeurMobilierCommunEur(),
      autresActifsCommunsEur: this.autresActifsCommunsEur(),
      recompensesEpoux1Eur: this.recompensesEpoux1Eur(),
      recompensesEpoux2Eur: this.recompensesEpoux2Eur(),
      biensPropresEpoux1Eur: this.biensPropresEpoux1Eur(),
      biensPropresEpoux2Eur: this.biensPropresEpoux2Eur(),
      occupationLogementEpoux: this.occupationLogementEpoux(),
      regimeMatrimonialDetecte: this.regimeMatrimonialDetecte(),
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.applyResult(r);
        this.calculating.set(false);
        this.snackBar.open('Liquidation calculée', 'OK', { duration: 2500 });
        if (!this.standaloneMode) this.dashboardRefresh?.triggerRefresh();
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

  private applyResult(r: LiquidationCommunauteFrResponse): void {
    this.result.set(r);
    this.showForm.set(false);
  }

  // ---------------------------------------------------------------------------
  // Helpers d'affichage du résultat
  // ---------------------------------------------------------------------------

  formatEuros(amount: number): string {
    return new Intl.NumberFormat('fr-FR', {
      style: 'currency',
      currency: 'EUR',
      maximumFractionDigits: 0,
    }).format(amount);
  }

  /** Libellé humain pour l'enum régime matrimonial (signal informatif). */
  regimeLabel(regime: string | null): string {
    if (regime === null) return 'Régime non documenté';
    const labels: Record<string, string> = {
      COMMUNAUTE_LEGALE: 'Communauté légale',
      COMMUNAUTE_UNIVERSELLE: 'Communauté universelle',
      SEPARATION_BIENS: 'Séparation de biens',
      PARTICIPATION_ACQUETS: 'Participation aux acquêts',
    };
    return labels[regime] ?? regime;
  }
}
