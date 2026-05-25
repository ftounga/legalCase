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
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SuccessionBeDevolutionReserveService } from '../../core/services/succession-be-devolution-reserve.service';
import {
  EtatCivilDefuntBe,
  HeritierCodeBe,
  RegimeMatrimonialDefuntBe,
  SuccessionBeDevolutionReserveRequest,
  SuccessionBeDevolutionReserveResponse,
  SuccessionBeDevolutionReserveVerdict,
} from '../../core/models/succession-be-devolution-reserve.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { PieceManquanteEntry } from '../../core/models/case-analysis.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { AiQuestion } from '../../core/models/ai-question.model';
import { CoherencePopoverTriggerDirective } from '../../shared/coherence-popover/coherence-popover-trigger.directive';
import { CoherenceAlert } from '../../shared/coherence-popover/coherence-alert.model';
import { CoherenceAlertBuilder } from '../../shared/coherence-popover/coherence-alert-builder';
import { SuccessionBeDevolutionReserveSectionPrefillRules } from './succession-be-devolution-reserve-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * F-217 SF-217-13 : champ F-IA-03 audité par l'outil dévolution / réserve BE.
 * `dateDeces` est potentiellement extractible par le pipeline V2 — câblage
 * présent même si l'alerte est rarement produite en V1 (aucun signal IA dédié).
 */
export type SuccessionBeDevolutionReserveAlertField = 'DATE_DECES';

export type SuccessionBeDevolutionReserveCoherenceAlert =
  CoherenceAlert<SuccessionBeDevolutionReserveAlertField>;

const ISO_DATE_REGEX = /^\d{4}-\d{2}-\d{2}$/;

/** Code `critereCode` (F-96 / questions IA) reconnu pour la date du décès. */
const F96_CODE_DATE_DECES = 'F217_DATE_DECES';

interface EtatCivilOption {
  value: EtatCivilDefuntBe;
  label: string;
}
interface RegimeMatrimonialOption {
  value: RegimeMatrimonialDefuntBe;
  label: string;
}

/**
 * F-217 SF-217-13 : composant Angular standalone pour l'outil décisionnel
 * "Succession BE — dévolution légale et réserve héréditaire post-2017"
 * (`succession-be-devolution-reserve`). BELGIQUE uniquement.
 *
 * Pattern de référence : `regime-communaute-legale-be-section` (F-217 SF-217-03).
 *
 * - Form : date du décès, état civil du défunt, régime matrimonial (si MARIE),
 *   nombre d'enfants vivants / prédécédés avec descendants, présence de parents
 *   / collatéraux, masse successorale (€), libéralités consenties (€), commentaire.
 * - Verdict 4 niveaux DEVOLUTION_ETABLIE (navy) / RESERVE_RESPECTEE (vert) /
 *   QUOTITE_DEPASSEE (rouge — critique) / QUALIFICATION_INCOMPLETE (navy info).
 *   Rouge réservé à QUOTITE_DEPASSEE.
 * - Affichage : liste des héritiers (fraction + €), réserve globale, quotité
 *   disponible, dépassement éventuel, bases juridiques (JetBrains Mono).
 * - Pré-fill IA V1 = 0 champ (`PREFILL_COUNT_ALWAYS_ZERO = true`).
 * - Validation F-IA-03 sur le field `DATE_DECES`.
 * - Refresh dashboard F-IA-02 + MatSnackBar erreurs.
 * - Gate `workspaceCountry === 'BELGIQUE'` — bannière info si mismatch.
 */
@Component({
  selector: 'app-succession-be-devolution-reserve-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatProgressSpinnerModule,
    LegalCitationsPipe,
    ToolJurisprudenceCitationsComponent,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './succession-be-devolution-reserve-section.component.html',
  styleUrl: './succession-be-devolution-reserve-section.component.scss',
})
export class SuccessionBeDevolutionReserveSectionComponent implements OnInit, OnChanges {
  // F-JU-03 SF-JU-03-99f — citations jurisprudentielles F-JU-01 (BE).
  protected readonly toolIdForJurisprudence = 'succession-be-devolution-reserve';
  protected readonly brancheActiveForJurisprudence = 'default';

  static readonly TOOL_LABEL =
    'DÉVOLUTION & RÉSERVE HÉRÉDITAIRE — CC LIVRE 4 BE (loi 31/07/2017)';
  static readonly TOOL_ICON = 'family_restroom';

  /**
   * F-217 SF-217-13 — V1 : aucun champ pré-rempli par l'IA (pas de signal pivot
   * dédié successions BE). Pattern documenté `PREFILL_COUNT_ALWAYS_ZERO = true`
   * — branche d'exemption du test `prefill-count-integrity.spec.ts`.
   */
  static readonly PREFILL_COUNT_ALWAYS_ZERO = true;
  static getPrefillCount(input: {
    aiData?: unknown;
    procedureChecks?: unknown[];
    aiQuestions?: unknown[];
    piecesManquantes?: unknown[];
    triggerEvents?: unknown[];
    workspaceCountry?: string;
  }): number {
    return SuccessionBeDevolutionReserveSectionPrefillRules.computePrefillCount(input);
  }

  static readonly ETAT_CIVIL_OPTIONS: readonly EtatCivilOption[] = [
    { value: 'MARIE', label: 'Marié(e)' },
    { value: 'COHABITANT_LEGAL', label: 'Cohabitant(e) légal(e)' },
    { value: 'CELIBATAIRE', label: 'Célibataire' },
    { value: 'DIVORCE', label: 'Divorcé(e)' },
    { value: 'VEUF', label: 'Veuf / Veuve' },
  ];
  readonly etatCivilOptions = SuccessionBeDevolutionReserveSectionComponent.ETAT_CIVIL_OPTIONS;

  static readonly REGIME_MATRIMONIAL_OPTIONS: readonly RegimeMatrimonialOption[] = [
    { value: 'COMMUNAUTE_LEGALE', label: 'Communauté légale' },
    { value: 'SEPARATION_BIENS', label: 'Séparation de biens' },
    { value: 'COMMUNAUTE_UNIVERSELLE', label: 'Communauté universelle' },
    { value: 'PARTICIPATION_ACQUETS', label: 'Participation aux acquêts' },
    { value: 'AUTRE', label: 'Autre' },
  ];
  readonly regimeMatrimonialOptions =
    SuccessionBeDevolutionReserveSectionComponent.REGIME_MATRIMONIAL_OPTIONS;

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
  result = signal<SuccessionBeDevolutionReserveResponse | null>(null);

  // --- Form fields ---
  dateDeces = signal<string | null>(null);
  etatCivilDefunt = signal<EtatCivilDefuntBe | null>(null);
  regimeMatrimonialDefunt = signal<RegimeMatrimonialDefuntBe | null>(null);
  nombreEnfantsVivants = signal<number>(0);
  nombreEnfantsPredecedesAvecDescendants = signal<number>(0);
  presenceParentsVivants = signal<boolean>(false);
  presenceFreresSoeursOuDescendants = signal<boolean>(false);
  masseSuccessoraleEur = signal<number>(0);
  libertesConsentiesEur = signal<number>(0);
  commentaire = signal<string | null>(null);

  /**
   * Coherence alerts F-IA-03 — gate `showForm()` strict (alertes masquées
   * après calcul rendu). Aucune source IA en standalone.
   */
  coherenceAlerts = computed<
    Partial<Record<SuccessionBeDevolutionReserveAlertField, SuccessionBeDevolutionReserveCoherenceAlert>>
  >(() => {
    if (this.standaloneMode) return {};
    if (!this.showForm()) return {};
    const alerts: Partial<Record<SuccessionBeDevolutionReserveAlertField, SuccessionBeDevolutionReserveCoherenceAlert>> = {};
    const dateAlert = this.buildDateDecesAlert();
    if (dateAlert) alerts.DATE_DECES = dateAlert;
    return alerts;
  });

  alertsSummary = computed(() => ({ total: Object.values(this.coherenceAlerts()).length }));

  /** True si le défunt est marié → le régime matrimonial devient requis. */
  marie = computed(() => this.etatCivilDefunt() === 'MARIE');

  constructor(
    private service: SuccessionBeDevolutionReserveService,
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
   * V1 : no-op (aucun flag pivot extrait par le pipeline IA pour les
   * successions BE — cf. `SF-217-00-coherence.md`). Câblage présent pour
   * branchement futur d'un éventuel `succession_be_detection` du pipeline V2.
   */
  private prefillFromAi(): void {
    // PREFILL_COUNT_ALWAYS_ZERO = true ; aucun champ pré-rempli en V1.
    this.cdr.markForCheck();
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  editMode(): void {
    this.showForm.set(true);
  }

  onNombreEnfantsVivantsChange(v: string | number): void {
    this.nombreEnfantsVivants.set(this.parseIntSafe(v));
  }
  onNombreEnfantsPredecedesChange(v: string | number): void {
    this.nombreEnfantsPredecedesAvecDescendants.set(this.parseIntSafe(v));
  }
  onMasseChange(v: string | number): void {
    this.masseSuccessoraleEur.set(this.parseAmountSafe(v));
  }
  onLiberalitesChange(v: string | number): void {
    this.libertesConsentiesEur.set(this.parseAmountSafe(v));
  }

  private parseIntSafe(v: string | number): number {
    const n = typeof v === 'number' ? v : parseInt(v, 10);
    return Number.isFinite(n) && n >= 0 ? n : 0;
  }

  private parseAmountSafe(v: string | number): number {
    const n = typeof v === 'number' ? v : parseFloat(v);
    return Number.isFinite(n) && n >= 0 ? n : 0;
  }

  /**
   * Form valide si workspace BE, date du décès ISO, état civil renseigné, et
   * si MARIE, régime matrimonial renseigné. La masse peut être nulle (cas
   * succession sans patrimoine).
   */
  formValid(): boolean {
    if (this.workspaceCountry !== 'BELGIQUE') return false;
    const date = this.dateDeces();
    if (!date || !ISO_DATE_REGEX.test(date)) return false;
    if (!this.etatCivilDefunt()) return false;
    if (this.marie() && !this.regimeMatrimonialDefunt()) return false;
    return true;
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request: SuccessionBeDevolutionReserveRequest = {
      dateDeces: this.dateDeces()!,
      etatCivilDefunt: this.etatCivilDefunt()!,
      regimeMatrimonialDefunt: this.marie() ? this.regimeMatrimonialDefunt() : null,
      nombreEnfantsVivants: this.nombreEnfantsVivants(),
      nombreEnfantsPredecedesAvecDescendants: this.nombreEnfantsPredecedesAvecDescendants(),
      presenceParentsVivants: this.presenceParentsVivants(),
      presenceFreresSoeursOuDescendants: this.presenceFreresSoeursOuDescendants(),
      masseSuccessoraleEur: this.masseSuccessoraleEur(),
      libertesConsentiesEur: this.libertesConsentiesEur(),
      commentaire: this.commentaire()?.trim() || null,
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.applyResult(r);
        this.calculating.set(false);
        this.snackBar.open('Analyse de la dévolution / réserve calculée', 'OK', { duration: 2500 });
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

  private applyResult(r: SuccessionBeDevolutionReserveResponse): void {
    this.result.set(r);
    this.hydrateForm(r);
    this.showForm.set(false);
  }

  /**
   * Ré-injecte le snapshot d'inputs de la réponse dans les champs du
   * formulaire — sans quoi un clic « Modifier » repartirait des valeurs par
   * défaut au lieu des dernières valeurs saisies (leçon F-DT-36).
   */
  private hydrateForm(r: SuccessionBeDevolutionReserveResponse): void {
    this.dateDeces.set(r.dateDeces);
    this.etatCivilDefunt.set(r.etatCivilDefunt);
    this.regimeMatrimonialDefunt.set(r.regimeMatrimonialDefunt);
    this.nombreEnfantsVivants.set(r.nombreEnfantsVivants);
    this.nombreEnfantsPredecedesAvecDescendants.set(r.nombreEnfantsPredecedesAvecDescendants);
    this.presenceParentsVivants.set(r.presenceParentsVivants);
    this.presenceFreresSoeursOuDescendants.set(r.presenceFreresSoeursOuDescendants);
    this.masseSuccessoraleEur.set(r.masseSuccessoraleEur);
    this.libertesConsentiesEur.set(r.libertesConsentiesEur);
    this.commentaire.set(r.commentaire);
  }

  // ---------------------------------------------------------------------------
  // Builder d'alerte F-IA-03 — date du décès
  // ---------------------------------------------------------------------------

  /** Date du décès — divergence stricte F-96 / question IA / IA détection. */
  private buildDateDecesAlert(): SuccessionBeDevolutionReserveCoherenceAlert | null {
    const user = this.dateDeces();
    if (!user) return null;
    const builder = CoherenceAlertBuilder.forField<SuccessionBeDevolutionReserveAlertField>('DATE_DECES');
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== F96_CODE_DATE_DECES) continue;
      const ev = chk.expectedValue?.trim();
      if (!ev || ev === user) continue;
      builder.addSource('F96', {
        expectedDisplay: ev,
        reason: `Checklist procédurale : décès attendu le ${ev}${chk.raison ? ' (' + chk.raison + ')' : ''}`,
      });
      break;
    }
    for (const q of this.aiQuestionsSignal()) {
      if (q.critereCode?.toUpperCase() !== F96_CODE_DATE_DECES) continue;
      const ev = q.expectedValue?.trim();
      if (!ev || ev === user) continue;
      builder.addSource('QUESTION_IA', {
        expectedDisplay: ev,
        reason: `Question complémentaire : "${q.questionText}" → "${q.answerText}"`,
      });
      break;
    }
    const piece = this.findPieceManquante(['F217_DATE_DECES', 'ACTE_DECES']);
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

  alertTooltip(alert: SuccessionBeDevolutionReserveCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: SuccessionBeDevolutionReserveCoherenceAlert): string {
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

  /**
   * Rouge réservé à QUOTITE_DEPASSEE (verdict critique).
   * Vert pour RESERVE_RESPECTEE. Navy info pour DEVOLUTION_ETABLIE et
   * QUALIFICATION_INCOMPLETE.
   */
  verdictBannerClass(verdict: SuccessionBeDevolutionReserveVerdict): string {
    switch (verdict) {
      case 'RESERVE_RESPECTEE':
        return 'sbd-verdict-banner sbd-verdict-banner--ok';
      case 'DEVOLUTION_ETABLIE':
      case 'QUALIFICATION_INCOMPLETE':
        return 'sbd-verdict-banner sbd-verdict-banner--info';
      case 'QUOTITE_DEPASSEE':
        return 'sbd-verdict-banner sbd-verdict-banner--danger';
    }
  }

  verdictBannerLabel(verdict: SuccessionBeDevolutionReserveVerdict): string {
    switch (verdict) {
      case 'DEVOLUTION_ETABLIE': return 'Dévolution établie (sans réserve héréditaire)';
      case 'RESERVE_RESPECTEE': return 'Réserve héréditaire respectée';
      case 'QUOTITE_DEPASSEE': return 'Quotité disponible dépassée';
      case 'QUALIFICATION_INCOMPLETE': return 'Qualification incomplète';
    }
  }

  verdictBannerIcon(verdict: SuccessionBeDevolutionReserveVerdict): string {
    switch (verdict) {
      case 'DEVOLUTION_ETABLIE': return 'info';
      case 'RESERVE_RESPECTEE': return 'check_circle';
      case 'QUOTITE_DEPASSEE': return 'error';
      case 'QUALIFICATION_INCOMPLETE': return 'info';
    }
  }

  /** Libellé humain pour le code héritier. */
  heritierCodeLabel(code: HeritierCodeBe): string {
    switch (code) {
      case 'CONJOINT_SURVIVANT': return 'Conjoint survivant';
      case 'COHABITANT_LEGAL_SURVIVANT': return 'Cohabitant légal survivant';
      case 'ENFANT': return 'Enfant';
      case 'DESCENDANT_REPRESENTATION': return 'Descendants par représentation';
      case 'PARENT': return 'Parents';
      case 'FRERE_SOEUR': return 'Frères / sœurs';
      case 'DESCENDANT_FRERE_SOEUR': return 'Descendants de frères / sœurs';
      case 'ETAT': return 'État (déshérence)';
      default: return code;
    }
  }
}
