import {
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
import { ContributionConjointBeService } from '../../core/services/contribution-conjoint-be.service';
import {
  ContributionConjointBeRequest,
  ContributionConjointBeResponse,
  ContributionConjointBeVerdict,
  TypeDivorceBe,
} from '../../core/models/contribution-conjoint-be.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { ContributionConjointBeSectionPrefillRules } from './contribution-conjoint-be-section-prefill-rules';

/** Option de type de divorce (alignée sur l'enum backend `TypeDivorceBe`). */
interface TypeDivorceOption {
  value: TypeDivorceBe;
  label: string;
}

/**
 * SF-217-09 : composant Angular standalone pour l'outil décisionnel "Pension
 * alimentaire entre ex-époux (Belgique)" (`contribution-conjoint-be`).
 * BELGIQUE uniquement.
 *
 * Pattern de référence : `regime-communaute-legale-be-section` (F-217 SF-217-03).
 *
 * - Form : type de divorce (select), 4 toggles (renonciation conventionnelle,
 *   état de besoin du créancier, faute grave, dégradation économique), durée
 *   du mariage, revenus des deux ex-époux.
 * - Verdict 4 niveaux : PENSION_DUE (navy) / PENSION_NON_DUE (rouge) /
 *   PENSION_CONVENTIONNELLE (navy) / DONNEES_INSUFFISANTES (or).
 * - Affichage : durée maximale légale, montant mensuel indicatif, motifs
 *   d'exclusion, détail du calcul, bases juridiques, messages.
 * - Pré-fill IA : V1 = 0 champ (`PREFILL_COUNT_ALWAYS_ZERO`).
 * - Refresh dashboard F-IA-02 + MatSnackBar erreurs.
 * - Gate `workspaceCountry === 'BELGIQUE'` — bannière info si mismatch.
 */
@Component({
  selector: 'app-contribution-conjoint-be-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatSlideToggleModule, MatProgressSpinnerModule,
    LegalCitationsPipe,
  ],
  templateUrl: './contribution-conjoint-be-section.component.html',
  styleUrl: './contribution-conjoint-be-section.component.scss',
})
export class ContributionConjointBeSectionComponent implements OnInit, OnChanges {
  static readonly TOOL_LABEL = 'PENSION ALIMENTAIRE ENTRE EX-ÉPOUX — CC ART. 301 BE';
  static readonly TOOL_ICON = 'volunteer_activism';

  /**
   * SF-246-28 / F-236 / F-237 — délègue au helper partagé.
   * 3 champs maximum (dureeMariage, revenuCreancier, revenuDebiteur).
   */
  static getPrefillCount(input: {
    aiData?: unknown;
    procedureChecks?: unknown[];
    aiQuestions?: unknown[];
    piecesManquantes?: unknown[];
    triggerEvents?: unknown[];
    workspaceCountry?: string;
  }): number {
    return ContributionConjointBeSectionPrefillRules.computePrefillCount({
      aiData: input.aiData,
    });
  }

  static readonly TYPE_DIVORCE_OPTIONS: readonly TypeDivorceOption[] = [
    { value: 'DDI', label: 'Divorce pour désunion irrémédiable (DDI)' },
    { value: 'DC', label: 'Divorce par consentement mutuel (DC)' },
  ];
  readonly typeDivorceOptions =
    ContributionConjointBeSectionComponent.TYPE_DIVORCE_OPTIONS;

  @Input() caseFileId!: string;
  /** F-177 — force l'expansion (mode modal). */
  @Input() forceExpanded = false;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'BELGIQUE';
  @Input() aiData?: FamilleExtractedData | null;
  /** Mode simulateur autonome (hors dossier) — coupe le refresh dashboard. */
  @Input() standaloneMode = false;

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<ContributionConjointBeResponse | null>(null);

  // --- Form fields ---
  typeDivorce = signal<TypeDivorceBe>('DDI');
  renonciationPensionConvention = signal<boolean>(false);
  creancierEnEtatDeBesoin = signal<boolean>(true);
  fauteGraveCreancier = signal<boolean>(false);
  dureeMariageAnnees = signal<number | null>(null);
  revenuMensuelCreancier = signal<number | null>(null);
  revenuMensuelDebiteur = signal<number | null>(null);
  degradationEconomiqueLieeAuMariage = signal<boolean>(false);
  commentaire = signal<string | null>(null);

  /** SF-246-28 — signaux de provenance (null = manuel, 'IA' = pré-rempli). */
  provenanceDureeMariage = signal<'IA' | null>(null);
  provenanceRevenuCreancier = signal<'IA' | null>(null);
  provenanceRevenuDebiteur = signal<'IA' | null>(null);

  constructor(
    private service: ContributionConjointBeService,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef,
    @Optional() private dashboardRefresh: CaseDashboardRefreshService | null,
  ) {}

  ngOnInit(): void {
    if (this.forceExpanded) this.collapsed.set(false);
    this.prefillFromAi();
    if (this.workspaceCountry === 'BELGIQUE') {
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
   * SF-246-28 — Pré-fill IA réel pour les 3 champs Contribution Conjoint BE.
   * No-op gracieux si `aiData` est null ou sous-objet absent.
   */
  private prefillFromAi(): void {
    const rules = ContributionConjointBeSectionPrefillRules;
    const input = { aiData: this.aiData };
    let changed = false;

    const duree = rules.computeDureeMariage(input);
    if (duree != null) { this.dureeMariageAnnees.set(duree); this.provenanceDureeMariage.set('IA'); changed = true; }

    const rc = rules.computeRevenuCreancier(input);
    if (rc != null) { this.revenuMensuelCreancier.set(rc); this.provenanceRevenuCreancier.set('IA'); changed = true; }

    const rd = rules.computeRevenuDebiteur(input);
    if (rd != null) { this.revenuMensuelDebiteur.set(rd); this.provenanceRevenuDebiteur.set('IA'); changed = true; }

    if (changed) this.cdr.markForCheck();
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  editMode(): void {
    this.showForm.set(true);
  }

  /**
   * Form valide si workspace BE, durée du mariage renseignée (0-80) et les
   * deux revenus renseignés (≥ 0). Le backend reste la source de vérité.
   */
  formValid(): boolean {
    if (this.workspaceCountry !== 'BELGIQUE') return false;
    const duree = this.dureeMariageAnnees();
    if (duree == null || duree < 0 || duree > 80) return false;
    const rc = this.revenuMensuelCreancier();
    const rd = this.revenuMensuelDebiteur();
    return rc != null && rc >= 0 && rd != null && rd >= 0;
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request: ContributionConjointBeRequest = {
      typeDivorce: this.typeDivorce(),
      renonciationPensionConvention: this.renonciationPensionConvention(),
      creancierEnEtatDeBesoin: this.creancierEnEtatDeBesoin(),
      fauteGraveCreancier: this.fauteGraveCreancier(),
      dureeMariageAnnees: this.dureeMariageAnnees()!,
      revenuMensuelCreancier: this.revenuMensuelCreancier()!,
      revenuMensuelDebiteur: this.revenuMensuelDebiteur()!,
      degradationEconomiqueLieeAuMariage: this.degradationEconomiqueLieeAuMariage(),
      commentaire: this.commentaire()?.trim() || null,
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.applyResult(r);
        this.calculating.set(false);
        this.snackBar.open('Analyse de la pension alimentaire calculée', 'OK', { duration: 2500 });
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

  private applyResult(r: ContributionConjointBeResponse): void {
    this.result.set(r);
    this.hydrateForm(r);
    this.showForm.set(false);
  }

  /**
   * Ré-injecte le snapshot d'inputs de la réponse dans les champs du
   * formulaire — sans quoi un clic « Modifier » repartirait des valeurs par
   * défaut au lieu des dernières valeurs saisies (leçon F-DT-36).
   */
  private hydrateForm(r: ContributionConjointBeResponse): void {
    this.typeDivorce.set(r.typeDivorce);
    this.renonciationPensionConvention.set(r.renonciationPensionConvention);
    this.creancierEnEtatDeBesoin.set(r.creancierEnEtatDeBesoin);
    this.fauteGraveCreancier.set(r.fauteGraveCreancier);
    this.dureeMariageAnnees.set(r.dureeMariageAnnees);
    this.revenuMensuelCreancier.set(r.revenuMensuelCreancier);
    this.revenuMensuelDebiteur.set(r.revenuMensuelDebiteur);
    this.degradationEconomiqueLieeAuMariage.set(r.degradationEconomiqueLieeAuMariage);
    this.commentaire.set(r.commentaire);
  }

  // ---------------------------------------------------------------------------
  // Helpers d'affichage du résultat
  // ---------------------------------------------------------------------------

  /** Rouge réservé à PENSION_NON_DUE ; or pour incomplet ; navy sinon. */
  verdictBannerClass(verdict: ContributionConjointBeVerdict): string {
    switch (verdict) {
      case 'PENSION_DUE':
        return 'ccj-verdict-banner ccj-verdict-banner--available';
      case 'PENSION_CONVENTIONNELLE':
        return 'ccj-verdict-banner ccj-verdict-banner--available';
      case 'PENSION_NON_DUE':
        return 'ccj-verdict-banner ccj-verdict-banner--danger';
      case 'DONNEES_INSUFFISANTES':
        return 'ccj-verdict-banner ccj-verdict-banner--medium';
    }
  }

  verdictBannerLabel(verdict: ContributionConjointBeVerdict): string {
    switch (verdict) {
      case 'PENSION_DUE': return 'Pension alimentaire due';
      case 'PENSION_NON_DUE': return 'Pension non due';
      case 'PENSION_CONVENTIONNELLE': return 'Pension conventionnelle (convention DC)';
      case 'DONNEES_INSUFFISANTES': return 'Données insuffisantes';
    }
  }

  verdictBannerIcon(verdict: ContributionConjointBeVerdict): string {
    switch (verdict) {
      case 'PENSION_DUE': return 'volunteer_activism';
      case 'PENSION_NON_DUE': return 'error';
      case 'PENSION_CONVENTIONNELLE': return 'description';
      case 'DONNEES_INSUFFISANTES': return 'warning';
    }
  }

  /** Durée maximale exprimée en années + mois pour lisibilité. */
  dureeMaximaleLabel(mois: number): string {
    if (mois <= 0) return '—';
    const annees = Math.floor(mois / 12);
    const reste = mois % 12;
    if (reste === 0) return `${annees} an(s) (${mois} mois)`;
    return `${annees} an(s) ${reste} mois (${mois} mois)`;
  }

  motifExclusionLabel(motif: string): string {
    switch (motif) {
      case 'FAUTE_GRAVE_CREANCIER': return 'Faute grave du créancier';
      case 'CREANCIER_HORS_BESOIN': return 'Créancier hors état de besoin';
      case 'RENONCIATION_CONVENTIONNELLE': return 'Renonciation conventionnelle à la pension';
      default: return motif;
    }
  }
}
