import {
  ChangeDetectorRef,
  Component,
  Input,
  OnChanges,
  OnInit,
  Optional,
  SimpleChanges,
  computed,
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
import { RegimeCommunauteLegaleBeService } from '../../core/services/regime-communaute-legale-be.service';
import {
  BienBeInput,
  BienCategorieBe,
  DetteBeInput,
  RegimeCommunauteLegaleBeRequest,
  RegimeCommunauteLegaleBeResponse,
  RegimeCommunauteLegaleBeVerdict,
} from '../../core/models/regime-communaute-legale-be.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { PieceManquanteEntry } from '../../core/models/case-analysis.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { AiQuestion } from '../../core/models/ai-question.model';
import { CoherencePopoverTriggerDirective } from '../../shared/coherence-popover/coherence-popover-trigger.directive';
import { CoherenceAlert } from '../../shared/coherence-popover/coherence-alert.model';
import { CoherenceAlertBuilder } from '../../shared/coherence-popover/coherence-alert-builder';
import { RegimeCommunauteLegaleBeSectionPrefillRules } from './regime-communaute-legale-be-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * F-217 SF-217-03 : champ F-IA-03 audité par l'outil régime de communauté
 * légale BE — la date de mariage est souvent extraite par le pipeline famille.
 */
export type RegimeCommunauteLegaleBeAlertField = 'DATE_MARIAGE';

export type RegimeCommunauteLegaleBeCoherenceAlert =
  CoherenceAlert<RegimeCommunauteLegaleBeAlertField>;

const ISO_DATE_REGEX = /^\d{4}-\d{2}-\d{2}$/;

/** Code `critereCode` (F-96 / questions IA) reconnu pour la date de mariage. */
const F96_CODE_DATE_MARIAGE = 'F217_DATE_MARIAGE';

/** Options de catégorie de bien (alignées sur l'enum backend `BienCategorieBe`). */
interface BienCategorieOption {
  value: BienCategorieBe;
  label: string;
}

/**
 * F-217 SF-217-03 : composant Angular standalone pour l'outil décisionnel
 * "Régime de communauté légale (Belgique)" (`regime-mat-be-communaute-legale`).
 * BELGIQUE uniquement.
 *
 * Pattern de référence : `procedure-nullite-licenciement-section` (F-DT-36).
 *
 * - Form : date de mariage (`<input type="date">`), toggle contrat de mariage
 *   signé, listes dynamiques de biens et de dettes (ajout / suppression de
 *   lignes, chaque ligne portant ses critères de qualification).
 * - Verdict ternaire COMMUNAUTE_LEGALE_APPLICABLE (navy) /
 *   REGIME_CONVENTIONNEL_DETECTE (or) / QUALIFICATION_INCOMPLETE (rouge).
 *   Rouge réservé à QUALIFICATION_INCOMPLETE.
 * - Affichage : biens qualifiés, dettes qualifiées, synthèse de composition,
 *   bases juridiques (JetBrains Mono pour `fondement`).
 * - Pré-fill IA : V1 = 0 champ (`PREFILL_COUNT_ALWAYS_ZERO`).
 * - Validation F-IA-03 sur le field `DATE_MARIAGE`.
 * - Refresh dashboard F-IA-02 + MatSnackBar erreurs.
 * - Gate `workspaceCountry === 'BELGIQUE'` — bannière info si mismatch.
 */
@Component({
  selector: 'app-regime-communaute-legale-be-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatSlideToggleModule, MatProgressSpinnerModule,
    LegalCitationsPipe,
    ToolJurisprudenceCitationsComponent,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './regime-communaute-legale-be-section.component.html',
  styleUrl: './regime-communaute-legale-be-section.component.scss',
})
export class RegimeCommunauteLegaleBeSectionComponent implements OnInit, OnChanges {
  // F-JU-03 SF-JU-03-99f — citations jurisprudentielles F-JU-01 (BE).
  protected readonly toolIdForJurisprudence = 'regime-mat-be-communaute-legale';
  protected readonly brancheActiveForJurisprudence = 'default';

  static readonly TOOL_LABEL = 'RÉGIME DE COMMUNAUTÉ LÉGALE — CC LIVRE 3 BE';
  static readonly TOOL_ICON = 'account_balance';

  /**
   * SF-246-28 / F-236 / F-237 — délègue au helper partagé.
   * 2 champs maximum (dateMariage, contratMariageSigne).
   */
  static getPrefillCount(input: {
    aiData?: unknown;
    procedureChecks?: unknown[];
    aiQuestions?: unknown[];
    piecesManquantes?: unknown[];
    triggerEvents?: unknown[];
    workspaceCountry?: string;
  }): number {
    return RegimeCommunauteLegaleBeSectionPrefillRules.computePrefillCount({
      aiData: input.aiData,
    });
  }

  static readonly CATEGORIE_OPTIONS: readonly BienCategorieOption[] = [
    { value: 'IMMOBILIER', label: 'Immobilier' },
    { value: 'MOBILIER', label: 'Mobilier' },
    { value: 'FINANCIER', label: 'Financier' },
    { value: 'PROFESSIONNEL', label: 'Professionnel' },
    { value: 'AUTRE', label: 'Autre' },
  ];
  readonly categorieOptions = RegimeCommunauteLegaleBeSectionComponent.CATEGORIE_OPTIONS;

  @Input() caseFileId!: string;
  /** F-177 — force l'expansion (mode modal). */
  @Input() forceExpanded = false;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'BELGIQUE';
  @Input() aiData?: FamilleExtractedData | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;
  /** Mode simulateur autonome (hors dossier) — coupe F-IA-03 + refresh. */
  @Input() standaloneMode = false;

  private procedureChecksSignal = signal<ProcedureCheck[]>([]);
  private aiQuestionsSignal = signal<AiQuestion[]>([]);
  private piecesManquantesSignal = signal<PieceManquanteEntry[]>([]);

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<RegimeCommunauteLegaleBeResponse | null>(null);

  // --- Form fields ---
  dateMariage = signal<string | null>(null);
  contratMariageSigne = signal<boolean>(false);
  biens = signal<BienBeInput[]>([this.emptyBien()]);
  dettes = signal<DetteBeInput[]>([]);

  /** SF-246-28 — signaux de provenance (null = manuel, 'IA' = pré-rempli). */
  provenanceDateMariage = signal<'IA' | null>(null);
  provenanceContratMariage = signal<'IA' | null>(null);

  /**
   * Coherence alerts F-IA-03 — gate `showForm()` strict (alertes masquées
   * après calcul rendu). Aucune source IA en standalone.
   */
  coherenceAlerts = computed<Partial<Record<RegimeCommunauteLegaleBeAlertField, RegimeCommunauteLegaleBeCoherenceAlert>>>(() => {
    if (this.standaloneMode) return {};
    if (!this.showForm()) return {};
    const alerts: Partial<Record<RegimeCommunauteLegaleBeAlertField, RegimeCommunauteLegaleBeCoherenceAlert>> = {};
    const dateAlert = this.buildDateMariageAlert();
    if (dateAlert) alerts.DATE_MARIAGE = dateAlert;
    return alerts;
  });

  alertsSummary = computed(() => ({ total: Object.values(this.coherenceAlerts()).length }));

  constructor(
    private service: RegimeCommunauteLegaleBeService,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef,
    @Optional() private dashboardRefresh: CaseDashboardRefreshService | null,
  ) {}

  ngOnInit(): void {
    if (this.forceExpanded) this.collapsed.set(false);
    this.procedureChecksSignal.set(this.procedureChecks ?? []);
    this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    this.piecesManquantesSignal.set(this.piecesManquantes ?? []);
    this.prefillFromAi();
    if (this.workspaceCountry === 'BELGIQUE') {
      this.load();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['forceExpanded'] && this.forceExpanded) this.collapsed.set(false);
    if (changes['procedureChecks']) this.procedureChecksSignal.set(this.procedureChecks ?? []);
    if (changes['aiQuestions']) this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    if (changes['piecesManquantes']) this.piecesManquantesSignal.set(this.piecesManquantes ?? []);
    if (changes['aiData'] && !changes['aiData'].firstChange
        && this.showForm() && !this.result()) {
      this.prefillFromAi();
    }
  }

  /**
   * SF-246-28 — Pré-fill IA réel pour les 2 champs Régime Communauté Légale BE.
   * `dateMariage` et `contratMariageSigne` pré-remplis depuis
   * `famille_be_detection_v2`. No-op gracieux si `aiData` est null.
   * Note : `dateMariage` pré-remplie alimente également la coherence alert
   * F-IA-03 déjà active sur ce composant.
   */
  private prefillFromAi(): void {
    const rules = RegimeCommunauteLegaleBeSectionPrefillRules;
    const input = { aiData: this.aiData };
    let changed = false;

    const dateMar = rules.computeDateMariage(input);
    if (dateMar != null) {
      this.dateMariage.set(dateMar);
      this.provenanceDateMariage.set('IA');
      changed = true;
    }

    const contrat = rules.computeContratMariage(input);
    if (contrat != null) {
      this.contratMariageSigne.set(contrat);
      this.provenanceContratMariage.set('IA');
      changed = true;
    }

    if (changed) this.cdr.markForCheck();
  }

  /** SF-246-28 — Reset provenance date de mariage au changement manuel. */
  onDateMariageChange(): void {
    this.provenanceDateMariage.set(null);
  }

  /** SF-246-28 — Reset provenance contrat de mariage au changement manuel. */
  onContratMariageChange(value: boolean): void {
    this.contratMariageSigne.set(value);
    this.provenanceContratMariage.set(null);
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // --- Listes dynamiques ---

  private emptyBien(): BienBeInput {
    return {
      libelle: '',
      categorie: 'IMMOBILIER',
      acquisAvantMariage: false,
      acquisParDonationOuSuccession: false,
      financeParFondsPropres: false,
      affectationProfessionnelle: false,
      outilDeTravailPersonnel: false,
      commentaire: null,
    };
  }

  private emptyDette(): DetteBeInput {
    return {
      libelle: '',
      anterieureAuMariage: false,
      contracteeDansLInteretDuMenage: false,
      contracteeParUnSeulEpoux: false,
      commentaire: null,
    };
  }

  addBien(): void {
    this.biens.update(list => [...list, this.emptyBien()]);
  }

  removeBien(index: number): void {
    this.biens.update(list => list.filter((_, i) => i !== index));
  }

  updateBien(index: number, patch: Partial<BienBeInput>): void {
    this.biens.update(list =>
      list.map((b, i) => (i === index ? { ...b, ...patch } : b)),
    );
  }

  addDette(): void {
    this.dettes.update(list => [...list, this.emptyDette()]);
  }

  removeDette(index: number): void {
    this.dettes.update(list => list.filter((_, i) => i !== index));
  }

  updateDette(index: number, patch: Partial<DetteBeInput>): void {
    this.dettes.update(list =>
      list.map((d, i) => (i === index ? { ...d, ...patch } : d)),
    );
  }

  /**
   * Form valide si workspace BE, date de mariage ISO et au moins un bien avec
   * un libellé non vide (le backend exige une liste de biens non vide).
   */
  formValid(): boolean {
    if (this.workspaceCountry !== 'BELGIQUE') return false;
    const date = this.dateMariage();
    if (!date || !ISO_DATE_REGEX.test(date)) return false;
    return this.biens().some(b => b.libelle.trim().length > 0);
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request: RegimeCommunauteLegaleBeRequest = {
      dateMariage: this.dateMariage()!,
      contratMariageSigne: this.contratMariageSigne(),
      biens: this.biens()
        .filter(b => b.libelle.trim().length > 0)
        .map(b => ({
          ...b,
          libelle: b.libelle.trim(),
          commentaire: b.commentaire?.trim() || null,
        })),
      dettes: this.dettes()
        .filter(d => d.libelle.trim().length > 0)
        .map(d => ({
          ...d,
          libelle: d.libelle.trim(),
          commentaire: d.commentaire?.trim() || null,
        })),
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.applyResult(r);
        this.calculating.set(false);
        this.snackBar.open('Analyse du régime de communauté légale calculée', 'OK', { duration: 2500 });
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

  private applyResult(r: RegimeCommunauteLegaleBeResponse): void {
    this.result.set(r);
    this.hydrateForm(r);
    this.showForm.set(false);
  }

  /**
   * Ré-injecte le snapshot d'inputs de la réponse dans les champs du
   * formulaire — sans quoi un clic « Modifier » repartirait des valeurs par
   * défaut au lieu des dernières valeurs saisies (leçon F-DT-36).
   */
  private hydrateForm(r: RegimeCommunauteLegaleBeResponse): void {
    this.dateMariage.set(r.dateMariage);
    this.contratMariageSigne.set(r.contratMariageSigne);
    this.biens.set(
      r.biens && r.biens.length > 0
        ? r.biens.map(b => ({ ...b }))
        : [this.emptyBien()],
    );
    this.dettes.set((r.dettes ?? []).map(d => ({ ...d })));
  }

  // ---------------------------------------------------------------------------
  // Builder d'alerte F-IA-03
  // ---------------------------------------------------------------------------

  /** Date de mariage — divergence stricte F-96 / question IA / IA détection. */
  private buildDateMariageAlert(): RegimeCommunauteLegaleBeCoherenceAlert | null {
    const user = this.dateMariage();
    if (!user) return null;
    const builder = CoherenceAlertBuilder.forField<RegimeCommunauteLegaleBeAlertField>('DATE_MARIAGE');
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== F96_CODE_DATE_MARIAGE) continue;
      const ev = chk.expectedValue?.trim();
      if (!ev || ev === user) continue;
      builder.addSource('F96', {
        expectedDisplay: ev,
        reason: `Checklist procédurale : mariage attendu le ${ev}${chk.raison ? ' (' + chk.raison + ')' : ''}`,
      });
      break;
    }
    for (const q of this.aiQuestionsSignal()) {
      if (q.critereCode?.toUpperCase() !== F96_CODE_DATE_MARIAGE) continue;
      const ev = q.expectedValue?.trim();
      if (!ev || ev === user) continue;
      builder.addSource('QUESTION_IA', {
        expectedDisplay: ev,
        reason: `Question complémentaire : "${q.questionText}" → "${q.answerText}"`,
      });
      break;
    }
    const piece = this.findPieceManquante(['F217_DATE_MARIAGE', 'ACTE_MARIAGE', 'CONTRAT_MARIAGE']);
    if (piece) builder.addPieceManquante(piece);
    return builder.build();
  }

  private findPieceManquante(acceptedCodes: string[]): string | null {
    const norm = new Set(acceptedCodes.map((c) => c.toUpperCase()));
    for (const p of this.piecesManquantesSignal()) {
      const code = p.critereCode?.toUpperCase();
      if (!code) continue;
      if (norm.has(code)) return p.texte;
    }
    return null;
  }

  alertTooltip(alert: RegimeCommunauteLegaleBeCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: RegimeCommunauteLegaleBeCoherenceAlert): string {
    const prefix = (() => {
      switch (alert.source) {
        case 'F96': return 'Incohérence Checklist procédurale';
        case 'QUESTION_IA': return 'Incohérence Question complémentaire';
        case 'IA': return 'Incohérence détectée';
        case 'PIECE_MANQUANTE': return 'Pièce manquante';
        case 'MULTI': return 'Incohérence multiple';
      }
    })();
    return `${prefix} (${alert.expectedDisplay})`;
  }

  // ---------------------------------------------------------------------------
  // Helpers d'affichage du résultat
  // ---------------------------------------------------------------------------

  /** Rouge réservé à QUALIFICATION_INCOMPLETE ; or pour conventionnel ; navy sinon. */
  verdictBannerClass(verdict: RegimeCommunauteLegaleBeVerdict): string {
    switch (verdict) {
      case 'COMMUNAUTE_LEGALE_APPLICABLE':
        return 'rcl-verdict-banner rcl-verdict-banner--available';
      case 'REGIME_CONVENTIONNEL_DETECTE':
        return 'rcl-verdict-banner rcl-verdict-banner--medium';
      case 'QUALIFICATION_INCOMPLETE':
        return 'rcl-verdict-banner rcl-verdict-banner--danger';
    }
  }

  verdictBannerLabel(verdict: RegimeCommunauteLegaleBeVerdict): string {
    switch (verdict) {
      case 'COMMUNAUTE_LEGALE_APPLICABLE': return 'Communauté légale applicable';
      case 'REGIME_CONVENTIONNEL_DETECTE': return 'Régime conventionnel détecté';
      case 'QUALIFICATION_INCOMPLETE': return 'Qualification incomplète';
    }
  }

  verdictBannerIcon(verdict: RegimeCommunauteLegaleBeVerdict): string {
    switch (verdict) {
      case 'COMMUNAUTE_LEGALE_APPLICABLE': return 'check_circle';
      case 'REGIME_CONVENTIONNEL_DETECTE': return 'warning';
      case 'QUALIFICATION_INCOMPLETE': return 'error';
    }
  }

  /** Classe CSS du chip de qualification d'un bien. */
  qualificationBienClass(qualification: string): string {
    switch (qualification) {
      case 'COMMUN': return 'rcl-qualif rcl-qualif--commun';
      case 'PROPRE': return 'rcl-qualif rcl-qualif--propre';
      case 'MIXTE_RECOMPENSE': return 'rcl-qualif rcl-qualif--mixte';
      case 'INDETERMINE': return 'rcl-qualif rcl-qualif--indetermine';
      default: return 'rcl-qualif';
    }
  }

  qualificationBienLabel(qualification: string): string {
    switch (qualification) {
      case 'COMMUN': return 'Commun';
      case 'PROPRE': return 'Propre';
      case 'MIXTE_RECOMPENSE': return 'Mixte (récompense)';
      case 'INDETERMINE': return 'Indéterminé';
      default: return qualification;
    }
  }

  modeGestionLabel(mode: string): string {
    switch (mode) {
      case 'GESTION_CONCURRENTE': return 'Gestion concurrente';
      case 'GESTION_EXCLUSIVE': return 'Gestion exclusive';
      case 'COGESTION': return 'Cogestion';
      default: return mode;
    }
  }

  /** Classe CSS du chip de qualification d'une dette. */
  qualificationDetteClass(qualification: string): string {
    switch (qualification) {
      case 'DETTE_COMMUNE': return 'rcl-qualif rcl-qualif--commun';
      case 'DETTE_PROPRE': return 'rcl-qualif rcl-qualif--propre';
      case 'DETTE_PROPRE_AVEC_RECOURS': return 'rcl-qualif rcl-qualif--mixte';
      default: return 'rcl-qualif';
    }
  }

  qualificationDetteLabel(qualification: string): string {
    switch (qualification) {
      case 'DETTE_COMMUNE': return 'Dette commune';
      case 'DETTE_PROPRE': return 'Dette propre';
      case 'DETTE_PROPRE_AVEC_RECOURS': return 'Dette propre avec recours';
      default: return qualification;
    }
  }
}
