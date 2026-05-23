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
import { MariageEtrangerBeReconnaissanceService } from '../../core/services/mariage-etranger-be-reconnaissance.service';
import {
  MariageEtrangerBeReconnaissanceRequest,
  MariageEtrangerBeReconnaissanceResponse,
  MariageEtrangerBeReconnaissanceVerdict,
  NationalitePartiesBe,
  NatureActeEtrangerBe,
  ResidenceHabituelleBe,
  SeveriteMotifBe,
} from '../../core/models/mariage-etranger-be-reconnaissance.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { PieceManquanteEntry } from '../../core/models/case-analysis.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { AiQuestion } from '../../core/models/ai-question.model';
import { CoherencePopoverTriggerDirective } from '../../shared/coherence-popover/coherence-popover-trigger.directive';
import { CoherenceAlert } from '../../shared/coherence-popover/coherence-alert.model';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';
import { CoherenceAlertBuilder } from '../../shared/coherence-popover/coherence-alert-builder';
import { MariageEtrangerBeReconnaissanceSectionPrefillRules } from './mariage-etranger-be-reconnaissance-section-prefill-rules';

/**
 * F-217 SF-217-17 : champ F-IA-03 audité par l'outil reconnaissance mariage
 * étranger BE. `dateActe` potentiellement extractible par le pipeline V2.
 */
export type MariageEtrangerBeReconnaissanceAlertField = 'DATE_ACTE';

export type MariageEtrangerBeReconnaissanceCoherenceAlert =
  CoherenceAlert<MariageEtrangerBeReconnaissanceAlertField>;

const ISO_DATE_REGEX = /^\d{4}-\d{2}-\d{2}$/;
const ISO2_REGEX = /^[A-Z]{2}$/;
const F96_CODE_DATE_ACTE = 'F217_DATE_ACTE_ETRANGER';

interface NatureActeOption { value: NatureActeEtrangerBe; label: string; }
interface ResidenceOption { value: ResidenceHabituelleBe; label: string; }
interface NationaliteOption { value: NationalitePartiesBe; label: string; }

/**
 * F-217 SF-217-17 : composant Angular standalone pour l'outil décisionnel
 * "Reconnaissance mariage / divorce étranger (Belgique)"
 * (`mariage-etranger-be-reconnaissance`). BELGIQUE uniquement.
 *
 * Pattern de référence : `succession-be-acceptation-renonciation-section`
 * (F-217 SF-217-13).
 *
 * - Form : nature de l'acte étranger (5 valeurs), pays d'origine ISO-2,
 *   date de l'acte, résidence habituelle, nationalité, conformité fond /
 *   forme, bloc talaq conditionnel (4 booleans affichés uniquement si
 *   nature = TALAQ_REPUDIATION), convention bilatérale, commentaire.
 * - Verdict 5 niveaux (RECONNAISSANCE_DE_PLEIN_DROIT /
 *   RECONNAISSANCE_POSSIBLE_SOUS_CONDITIONS /
 *   RECONNAISSANCE_REFUSEE_ORDRE_PUBLIC / EXEQUATUR_REQUIS /
 *   QUALIFICATION_INCOMPLETE). Rouge réservé à
 *   RECONNAISSANCE_REFUSEE_ORDRE_PUBLIC.
 * - Affichage : verdict, motifs de refus, motifs de réserve (avec sévérité
 *   colorée), actes à produire, bases juridiques.
 * - Pré-fill IA V1 = 0 champ (`PREFILL_COUNT_ALWAYS_ZERO = true`).
 * - Validation F-IA-03 sur le field `DATE_ACTE`.
 * - Refresh dashboard F-IA-02 + MatSnackBar erreurs.
 * - Gate `workspaceCountry === 'BELGIQUE'` — bannière info si mismatch.
 */
@Component({
  selector: 'app-mariage-etranger-be-reconnaissance-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatSlideToggleModule, MatProgressSpinnerModule,
    LegalCitationsPipe,
    CoherencePopoverTriggerDirective,
    ToolJurisprudenceCitationsComponent
  ],
  templateUrl: './mariage-etranger-be-reconnaissance-section.component.html',
  styleUrl: './mariage-etranger-be-reconnaissance-section.component.scss',
})
export class MariageEtrangerBeReconnaissanceSectionComponent implements OnInit, OnChanges {
  // F-JU-03 — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'mariage-etranger-be-reconnaissance';
  protected readonly brancheActiveForJurisprudence = 'default';

  static readonly TOOL_LABEL = "RECONNAISSANCE MARIAGE / DIVORCE ÉTRANGER — CDIP BE";
  static readonly TOOL_ICON = 'public';

  /**
   * F-217 SF-217-17 — V1 : aucun champ pré-rempli par l'IA. Pattern documenté
   * `PREFILL_COUNT_ALWAYS_ZERO = true` (cf. `SF-217-00-coherence.md`).
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
    return MariageEtrangerBeReconnaissanceSectionPrefillRules.computePrefillCount(input);
  }

  static readonly NATURE_OPTIONS: readonly NatureActeOption[] = [
    { value: 'MARIAGE_CIVIL_ETRANGER', label: 'Mariage civil étranger' },
    { value: 'MARIAGE_RELIGIEUX_NON_CIVIL', label: 'Mariage religieux non précédé du civil' },
    { value: 'MARIAGE_POLYGAME', label: 'Mariage polygame' },
    { value: 'DIVORCE_JUDICIAIRE_ETRANGER', label: 'Divorce judiciaire étranger' },
    { value: 'TALAQ_REPUDIATION', label: 'Talaq / répudiation' },
  ];
  readonly natureOptions = MariageEtrangerBeReconnaissanceSectionComponent.NATURE_OPTIONS;

  static readonly RESIDENCE_OPTIONS: readonly ResidenceOption[] = [
    { value: 'BELGIQUE', label: 'Belgique' },
    { value: 'ETRANGER', label: 'À l\'étranger' },
    { value: 'INCONNU', label: 'Inconnu' },
  ];
  readonly residenceOptions = MariageEtrangerBeReconnaissanceSectionComponent.RESIDENCE_OPTIONS;

  static readonly NATIONALITE_OPTIONS: readonly NationaliteOption[] = [
    { value: 'BELGIQUE', label: 'Belgique' },
    { value: 'UE', label: 'État membre de l\'UE (hors BE)' },
    { value: 'HORS_UE', label: 'Hors UE' },
    { value: 'INCONNU', label: 'Inconnu' },
  ];
  readonly nationaliteOptions = MariageEtrangerBeReconnaissanceSectionComponent.NATIONALITE_OPTIONS;

  @Input() caseFileId!: string;
  /** F-177 — force l'expansion (mode modal). */
  @Input() forceExpanded = false;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'BELGIQUE';
  @Input() aiData?: FamilleExtractedData | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;
  @Input() standaloneMode = false;

  private procedureChecksSignal = signal<ProcedureCheck[]>([]);
  private aiQuestionsSignal = signal<AiQuestion[]>([]);
  private piecesManquantesSignal = signal<PieceManquanteEntry[]>([]);

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<MariageEtrangerBeReconnaissanceResponse | null>(null);

  // --- Form fields ---
  natureActe = signal<NatureActeEtrangerBe | null>(null);
  paysOrigine = signal<string | null>(null);
  dateActe = signal<string | null>(null);
  residenceHabituelleAuMoinsUnePartie = signal<ResidenceHabituelleBe | null>(null);
  nationaliteAuMoinsUnePartie = signal<NationalitePartiesBe | null>(null);
  conformiteDroitFondPersonnel = signal<boolean>(true);
  conformiteFormeLocusRegitActum = signal<boolean>(true);
  consentementEpouse = signal<boolean>(true);
  epousePresente = signal<boolean>(true);
  procedureContradictoire = signal<boolean>(true);
  decisionEcriteOfficielle = signal<boolean>(true);
  conventionBilateraleApplicable = signal<boolean>(false);
  commentaire = signal<string | null>(null);

  /** Bloc talaq affiché uniquement si natureActe === 'TALAQ_REPUDIATION'. */
  isTalaq = computed(() => this.natureActe() === 'TALAQ_REPUDIATION');

  coherenceAlerts = computed<
    Partial<Record<MariageEtrangerBeReconnaissanceAlertField, MariageEtrangerBeReconnaissanceCoherenceAlert>>
  >(() => {
    if (this.standaloneMode) return {};
    if (!this.showForm()) return {};
    const alerts: Partial<Record<MariageEtrangerBeReconnaissanceAlertField, MariageEtrangerBeReconnaissanceCoherenceAlert>> = {};
    const dateAlert = this.buildDateActeAlert();
    if (dateAlert) alerts.DATE_ACTE = dateAlert;
    return alerts;
  });

  alertsSummary = computed(() => ({ total: Object.values(this.coherenceAlerts()).length }));

  constructor(
    private service: MariageEtrangerBeReconnaissanceService,
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

  /** V1 : no-op — aucun signal IA dédié à la reconnaissance d'un mariage étranger. */
  private prefillFromAi(): void {
    this.cdr.markForCheck();
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  editMode(): void {
    this.showForm.set(true);
  }

  onPaysOrigineChange(value: string): void {
    // Normalisation immédiate : majuscule, max 2 caractères.
    const normalized = (value || '').trim().toUpperCase().slice(0, 2);
    this.paysOrigine.set(normalized || null);
  }

  /**
   * Form valide si workspace BE, champs requis renseignés et pays d'origine
   * au format ISO-2. Si natureActe = TALAQ_REPUDIATION, les 4 booleans
   * talaq deviennent obligatoires (ici toujours alimentés, mais validation
   * structurelle conservée).
   */
  formValid(): boolean {
    if (this.workspaceCountry !== 'BELGIQUE') return false;
    if (!this.natureActe()) return false;
    const pays = this.paysOrigine();
    if (!pays || !ISO2_REGEX.test(pays)) return false;
    const date = this.dateActe();
    if (!date || !ISO_DATE_REGEX.test(date)) return false;
    if (!this.residenceHabituelleAuMoinsUnePartie()) return false;
    if (!this.nationaliteAuMoinsUnePartie()) return false;
    return true;
  }

  calculate(): void {
    if (!this.formValid()) return;
    const talaq = this.isTalaq();
    const request: MariageEtrangerBeReconnaissanceRequest = {
      natureActe: this.natureActe()!,
      paysOrigine: this.paysOrigine()!,
      dateActe: this.dateActe()!,
      residenceHabituelleAuMoinsUnePartie: this.residenceHabituelleAuMoinsUnePartie()!,
      nationaliteAuMoinsUnePartie: this.nationaliteAuMoinsUnePartie()!,
      conformiteDroitFondPersonnel: this.conformiteDroitFondPersonnel(),
      conformiteFormeLocusRegitActum: this.conformiteFormeLocusRegitActum(),
      consentementEpouse: talaq ? this.consentementEpouse() : null,
      epousePresente: talaq ? this.epousePresente() : null,
      procedureContradictoire: talaq ? this.procedureContradictoire() : null,
      decisionEcriteOfficielle: talaq ? this.decisionEcriteOfficielle() : null,
      conventionBilateraleApplicable: this.conventionBilateraleApplicable(),
      commentaire: this.commentaire()?.trim() || null,
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.applyResult(r);
        this.calculating.set(false);
        this.snackBar.open("Analyse de reconnaissance calculée", 'OK', { duration: 2500 });
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
        this.loading.set(false);
        this.cdr.markForCheck();
      },
    });
  }

  private applyResult(r: MariageEtrangerBeReconnaissanceResponse): void {
    this.result.set(r);
    this.hydrateForm(r);
    this.showForm.set(false);
  }

  private hydrateForm(r: MariageEtrangerBeReconnaissanceResponse): void {
    this.natureActe.set(r.natureActe);
    this.paysOrigine.set(r.paysOrigine);
    this.dateActe.set(r.dateActe);
    this.residenceHabituelleAuMoinsUnePartie.set(r.residenceHabituelleAuMoinsUnePartie);
    this.nationaliteAuMoinsUnePartie.set(r.nationaliteAuMoinsUnePartie);
    this.conformiteDroitFondPersonnel.set(!!r.conformiteDroitFondPersonnel);
    this.conformiteFormeLocusRegitActum.set(!!r.conformiteFormeLocusRegitActum);
    this.consentementEpouse.set(r.consentementEpouse ?? true);
    this.epousePresente.set(r.epousePresente ?? true);
    this.procedureContradictoire.set(r.procedureContradictoire ?? true);
    this.decisionEcriteOfficielle.set(r.decisionEcriteOfficielle ?? true);
    this.conventionBilateraleApplicable.set(!!r.conventionBilateraleApplicable);
    this.commentaire.set(r.commentaire);
  }

  // ---------------------------------------------------------------------------
  // Builder d'alerte F-IA-03 — date de l'acte
  // ---------------------------------------------------------------------------

  private buildDateActeAlert(): MariageEtrangerBeReconnaissanceCoherenceAlert | null {
    const user = this.dateActe();
    if (!user) return null;
    const builder = CoherenceAlertBuilder.forField<MariageEtrangerBeReconnaissanceAlertField>('DATE_ACTE');
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== F96_CODE_DATE_ACTE) continue;
      const ev = chk.expectedValue?.trim();
      if (!ev || ev === user) continue;
      builder.addSource('F96', {
        expectedDisplay: ev,
        reason: `Checklist procédurale : date de l'acte attendue le ${ev}${chk.raison ? ' (' + chk.raison + ')' : ''}`,
      });
      break;
    }
    for (const q of this.aiQuestionsSignal()) {
      if (q.critereCode?.toUpperCase() !== F96_CODE_DATE_ACTE) continue;
      const ev = q.expectedValue?.trim();
      if (!ev || ev === user) continue;
      builder.addSource('QUESTION_IA', {
        expectedDisplay: ev,
        reason: `Question complémentaire : "${q.questionText}" → "${q.answerText}"`,
      });
      break;
    }
    const piece = this.findPieceManquante(['F217_DATE_ACTE_ETRANGER', 'ACTE_MARIAGE_ETRANGER']);
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

  alertTooltip(alert: MariageEtrangerBeReconnaissanceCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: MariageEtrangerBeReconnaissanceCoherenceAlert): string {
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
   * Rouge réservé à RECONNAISSANCE_REFUSEE_ORDRE_PUBLIC.
   * Or pour RECONNAISSANCE_POSSIBLE_SOUS_CONDITIONS et EXEQUATUR_REQUIS.
   * Vert pour RECONNAISSANCE_DE_PLEIN_DROIT. Navy info pour
   * QUALIFICATION_INCOMPLETE.
   */
  verdictBannerClass(verdict: MariageEtrangerBeReconnaissanceVerdict): string {
    switch (verdict) {
      case 'RECONNAISSANCE_DE_PLEIN_DROIT':
        return 'mer-verdict-banner mer-verdict-banner--ok';
      case 'RECONNAISSANCE_POSSIBLE_SOUS_CONDITIONS':
      case 'EXEQUATUR_REQUIS':
        return 'mer-verdict-banner mer-verdict-banner--medium';
      case 'RECONNAISSANCE_REFUSEE_ORDRE_PUBLIC':
        return 'mer-verdict-banner mer-verdict-banner--danger';
      case 'QUALIFICATION_INCOMPLETE':
        return 'mer-verdict-banner mer-verdict-banner--info';
    }
  }

  verdictBannerLabel(verdict: MariageEtrangerBeReconnaissanceVerdict): string {
    switch (verdict) {
      case 'RECONNAISSANCE_DE_PLEIN_DROIT': return 'Reconnaissance de plein droit';
      case 'RECONNAISSANCE_POSSIBLE_SOUS_CONDITIONS': return 'Reconnaissance possible sous conditions';
      case 'RECONNAISSANCE_REFUSEE_ORDRE_PUBLIC': return 'Reconnaissance refusée (ordre public)';
      case 'EXEQUATUR_REQUIS': return 'Procédure d\'exequatur requise';
      case 'QUALIFICATION_INCOMPLETE': return 'Qualification incomplète';
    }
  }

  verdictBannerIcon(verdict: MariageEtrangerBeReconnaissanceVerdict): string {
    switch (verdict) {
      case 'RECONNAISSANCE_DE_PLEIN_DROIT': return 'check_circle';
      case 'RECONNAISSANCE_POSSIBLE_SOUS_CONDITIONS':
      case 'EXEQUATUR_REQUIS':
        return 'warning';
      case 'RECONNAISSANCE_REFUSEE_ORDRE_PUBLIC': return 'error';
      case 'QUALIFICATION_INCOMPLETE': return 'info';
    }
  }

  /** Couleur de la sévérité d'un motif. Rouge pour HIGH. */
  severiteClass(severite: SeveriteMotifBe): string {
    switch (severite) {
      case 'HIGH': return 'mer-severite mer-severite--high';
      case 'MEDIUM': return 'mer-severite mer-severite--medium';
      case 'LOW': return 'mer-severite mer-severite--low';
    }
  }

  severiteLabel(severite: SeveriteMotifBe): string {
    switch (severite) {
      case 'HIGH': return 'Sévérité élevée';
      case 'MEDIUM': return 'Sévérité moyenne';
      case 'LOW': return 'Sévérité faible';
    }
  }

  natureLabel(n: NatureActeEtrangerBe): string {
    const opt = this.natureOptions.find(o => o.value === n);
    return opt?.label ?? n;
  }
}
