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
import { SuccessionBeAcceptationRenonciationService } from '../../core/services/succession-be-acceptation-renonciation.service';
import {
  ActeHeritierBe,
  DelaiStatutBe,
  EtatPatrimoineSuccessoralBe,
  OptionRecommandeeSuccessionBe,
  QualiteHeritierBe,
  SeveriteRisqueBe,
  SuccessionBeAcceptationRenonciationRequest,
  SuccessionBeAcceptationRenonciationResponse,
  SuccessionBeAcceptationRenonciationVerdict,
  VolonteClientSuccessionBe,
} from '../../core/models/succession-be-acceptation-renonciation.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { PieceManquanteEntry } from '../../core/models/case-analysis.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { AiQuestion } from '../../core/models/ai-question.model';
import { CoherencePopoverTriggerDirective } from '../../shared/coherence-popover/coherence-popover-trigger.directive';
import { CoherenceAlert } from '../../shared/coherence-popover/coherence-alert.model';
import { CoherenceAlertBuilder } from '../../shared/coherence-popover/coherence-alert-builder';
import { SuccessionBeAcceptationRenonciationSectionPrefillRules } from './succession-be-acceptation-renonciation-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * F-217 SF-217-13 : champ F-IA-03 audité par l'outil acceptation /
 * renonciation BE. `dateDeces` potentiellement extractible par le pipeline V2.
 */
export type SuccessionBeAcceptationRenonciationAlertField = 'DATE_DECES';

export type SuccessionBeAcceptationRenonciationCoherenceAlert =
  CoherenceAlert<SuccessionBeAcceptationRenonciationAlertField>;

const ISO_DATE_REGEX = /^\d{4}-\d{2}-\d{2}$/;

const F96_CODE_DATE_DECES = 'F217_DATE_DECES';

interface QualiteOption { value: QualiteHeritierBe; label: string; }
interface EtatPatrimoineOption { value: EtatPatrimoineSuccessoralBe; label: string; }
interface ActeOption { value: ActeHeritierBe; label: string; tacite: boolean; }
interface VolonteOption { value: VolonteClientSuccessionBe; label: string; }

/**
 * F-217 SF-217-13 : composant Angular standalone pour l'outil décisionnel
 * "Succession BE — acceptation / renonciation"
 * (`succession-be-acceptation-renonciation`). BELGIQUE uniquement.
 *
 * Pattern de référence : `regime-communaute-legale-be-section` (F-217 SF-217-03).
 *
 * - Form : date du décès, qualité d'héritier, état du patrimoine successoral,
 *   liste à cocher des actes accomplis (avec marqueur « tacite » pour les
 *   actes valant acceptation tacite), volonté du client, mise en demeure
 *   d'un créancier (avec date conditionnelle), commentaire.
 * - Verdict 7 niveaux (OPTION_LIBRE_DELAI_OK / OPTION_RECOMMANDEE_BENEFICE_INVENTAIRE
 *   / OPTION_RECOMMANDEE_RENONCIATION / ACCEPTATION_TACITE_PROBABLE / DELAI_CRITIQUE
 *   / DELAI_DEPASSE / QUALIFICATION_INCOMPLETE). Rouge réservé à
 *   ACCEPTATION_TACITE_PROBABLE / DELAI_CRITIQUE / DELAI_DEPASSE.
 * - Affichage : verdict, option recommandée, date limite + jours restants +
 *   statut coloré, risques avec sévérité, actions concrètes, bases juridiques.
 * - Pré-fill IA V1 = 0 champ (`PREFILL_COUNT_ALWAYS_ZERO = true`).
 * - Validation F-IA-03 sur le field `DATE_DECES`.
 * - Refresh dashboard F-IA-02 + MatSnackBar erreurs.
 * - Gate `workspaceCountry === 'BELGIQUE'` — bannière info si mismatch.
 */
@Component({
  selector: 'app-succession-be-acceptation-renonciation-section',
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
  templateUrl: './succession-be-acceptation-renonciation-section.component.html',
  styleUrl: './succession-be-acceptation-renonciation-section.component.scss',
})
export class SuccessionBeAcceptationRenonciationSectionComponent implements OnInit, OnChanges {
  // F-JU-03 SF-JU-03-99f — citations jurisprudentielles F-JU-01 (BE).
  protected readonly toolIdForJurisprudence = 'succession-be-acceptation-renonciation';
  protected readonly brancheActiveForJurisprudence = 'default';

  static readonly TOOL_LABEL = "ACCEPTATION / RENONCIATION À SUCCESSION — CC art. 774+ BE";
  static readonly TOOL_ICON = 'gavel';

  /**
   * F-217 SF-217-13 — V1 : aucun champ pré-rempli par l'IA. Pattern documenté
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
    return SuccessionBeAcceptationRenonciationSectionPrefillRules.computePrefillCount(input);
  }

  static readonly QUALITE_OPTIONS: readonly QualiteOption[] = [
    { value: 'CONJOINT_SURVIVANT', label: 'Conjoint survivant' },
    { value: 'COHABITANT_LEGAL_SURVIVANT', label: 'Cohabitant légal survivant' },
    { value: 'ENFANT', label: 'Enfant' },
    { value: 'DESCENDANT_REPRESENTATION', label: 'Descendant (représentation)' },
    { value: 'PARENT', label: 'Parent' },
    { value: 'FRERE_SOEUR', label: 'Frère / sœur' },
    { value: 'LEGATAIRE_UNIVERSEL', label: 'Légataire universel' },
    { value: 'LEGATAIRE_PARTICULIER', label: 'Légataire particulier' },
  ];
  readonly qualiteOptions = SuccessionBeAcceptationRenonciationSectionComponent.QUALITE_OPTIONS;

  static readonly ETAT_PATRIMOINE_OPTIONS: readonly EtatPatrimoineOption[] = [
    { value: 'SOLVABLE', label: 'Solvable (actif > passif)' },
    { value: 'DOUTEUX', label: 'Douteux (incertitude)' },
    { value: 'INSOLVABLE', label: 'Insolvable (passif > actif)' },
    { value: 'INCONNU', label: 'Inconnu (à inventorier)' },
  ];
  readonly etatPatrimoineOptions = SuccessionBeAcceptationRenonciationSectionComponent.ETAT_PATRIMOINE_OPTIONS;

  static readonly ACTE_OPTIONS: readonly ActeOption[] = [
    { value: 'ENCAISSEMENT_CREANCE_DEFUNT', label: 'Encaissement d\'une créance du défunt', tacite: true },
    { value: 'PAIEMENT_DETTE_DEFUNT', label: 'Paiement d\'une dette du défunt', tacite: true },
    { value: 'VENTE_BIEN_SUCCESSION', label: 'Vente d\'un bien de la succession', tacite: true },
    { value: 'PRISE_POSSESSION_BIENS', label: 'Prise de possession des biens', tacite: true },
    { value: 'ACTE_CONSERVATOIRE_NEUTRE', label: 'Acte conservatoire neutre (à vérifier)', tacite: false },
    { value: 'DEMANDE_INVENTAIRE', label: 'Demande d\'inventaire (à vérifier)', tacite: false },
  ];
  readonly acteOptions = SuccessionBeAcceptationRenonciationSectionComponent.ACTE_OPTIONS;

  static readonly VOLONTE_OPTIONS: readonly VolonteOption[] = [
    { value: 'ACCEPTER', label: 'Accepter purement et simplement' },
    { value: 'ACCEPTER_BENEFICE_INVENTAIRE', label: 'Accepter sous bénéfice d\'inventaire' },
    { value: 'RENONCER', label: 'Renoncer' },
    { value: 'INDECIS', label: 'Indécis' },
  ];
  readonly volonteOptions = SuccessionBeAcceptationRenonciationSectionComponent.VOLONTE_OPTIONS;

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
  result = signal<SuccessionBeAcceptationRenonciationResponse | null>(null);

  // --- Form fields ---
  dateDeces = signal<string | null>(null);
  qualiteHeritier = signal<QualiteHeritierBe | null>(null);
  etatPatrimoineSuccessoral = signal<EtatPatrimoineSuccessoralBe | null>(null);
  actesAccomplis = signal<ActeHeritierBe[]>([]);
  volonteClient = signal<VolonteClientSuccessionBe | null>(null);
  miseEnDemeureCreancier = signal<boolean>(false);
  dateMiseEnDemeureCreancier = signal<string | null>(null);
  commentaire = signal<string | null>(null);

  coherenceAlerts = computed<
    Partial<Record<SuccessionBeAcceptationRenonciationAlertField, SuccessionBeAcceptationRenonciationCoherenceAlert>>
  >(() => {
    if (this.standaloneMode) return {};
    if (!this.showForm()) return {};
    const alerts: Partial<Record<SuccessionBeAcceptationRenonciationAlertField, SuccessionBeAcceptationRenonciationCoherenceAlert>> = {};
    const dateAlert = this.buildDateDecesAlert();
    if (dateAlert) alerts.DATE_DECES = dateAlert;
    return alerts;
  });

  alertsSummary = computed(() => ({ total: Object.values(this.coherenceAlerts()).length }));

  constructor(
    private service: SuccessionBeAcceptationRenonciationService,
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

  /** V1 : no-op — aucun signal IA dédié aux successions BE. */
  private prefillFromAi(): void {
    this.cdr.markForCheck();
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  editMode(): void {
    this.showForm.set(true);
  }

  toggleActe(acte: ActeHeritierBe, checked: boolean): void {
    this.actesAccomplis.update(list => {
      if (checked) {
        return list.includes(acte) ? list : [...list, acte];
      }
      return list.filter(a => a !== acte);
    });
  }

  isActeChecked(acte: ActeHeritierBe): boolean {
    return this.actesAccomplis().includes(acte);
  }

  onMiseEnDemeureChange(value: boolean): void {
    this.miseEnDemeureCreancier.set(value);
    if (!value) {
      this.dateMiseEnDemeureCreancier.set(null);
    }
  }

  /**
   * Form valide si workspace BE, date du décès ISO, qualité d'héritier, état
   * patrimoine et volonté du client renseignés. Si mise en demeure cochée, la
   * date associée doit être au format ISO.
   */
  formValid(): boolean {
    if (this.workspaceCountry !== 'BELGIQUE') return false;
    const date = this.dateDeces();
    if (!date || !ISO_DATE_REGEX.test(date)) return false;
    if (!this.qualiteHeritier()) return false;
    if (!this.etatPatrimoineSuccessoral()) return false;
    if (!this.volonteClient()) return false;
    if (this.miseEnDemeureCreancier()) {
      const med = this.dateMiseEnDemeureCreancier();
      if (!med || !ISO_DATE_REGEX.test(med)) return false;
    }
    return true;
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request: SuccessionBeAcceptationRenonciationRequest = {
      dateDeces: this.dateDeces()!,
      qualiteHeritier: this.qualiteHeritier()!,
      etatPatrimoineSuccessoral: this.etatPatrimoineSuccessoral()!,
      actesAccomplis: this.actesAccomplis(),
      volonteClient: this.volonteClient()!,
      miseEnDemeureCreancier: this.miseEnDemeureCreancier(),
      dateMiseEnDemeureCreancier: this.miseEnDemeureCreancier() ? this.dateMiseEnDemeureCreancier() : null,
      commentaire: this.commentaire()?.trim() || null,
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.applyResult(r);
        this.calculating.set(false);
        this.snackBar.open("Analyse de l'option successorale calculée", 'OK', { duration: 2500 });
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

  private applyResult(r: SuccessionBeAcceptationRenonciationResponse): void {
    this.result.set(r);
    this.hydrateForm(r);
    this.showForm.set(false);
  }

  private hydrateForm(r: SuccessionBeAcceptationRenonciationResponse): void {
    this.dateDeces.set(r.dateDeces);
    this.qualiteHeritier.set(r.qualiteHeritier);
    this.etatPatrimoineSuccessoral.set(r.etatPatrimoineSuccessoral);
    this.actesAccomplis.set([...(r.actesAccomplis ?? [])]);
    this.volonteClient.set(r.volonteClient);
    this.miseEnDemeureCreancier.set(!!r.miseEnDemeureCreancier);
    this.dateMiseEnDemeureCreancier.set(r.dateMiseEnDemeureCreancier);
    this.commentaire.set(r.commentaire);
  }

  // ---------------------------------------------------------------------------
  // Builder d'alerte F-IA-03 — date du décès
  // ---------------------------------------------------------------------------

  private buildDateDecesAlert(): SuccessionBeAcceptationRenonciationCoherenceAlert | null {
    const user = this.dateDeces();
    if (!user) return null;
    const builder = CoherenceAlertBuilder.forField<SuccessionBeAcceptationRenonciationAlertField>('DATE_DECES');
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

  alertTooltip(alert: SuccessionBeAcceptationRenonciationCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: SuccessionBeAcceptationRenonciationCoherenceAlert): string {
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
   * Rouge réservé aux verdicts critiques : ACCEPTATION_TACITE_PROBABLE,
   * DELAI_CRITIQUE, DELAI_DEPASSE. Or pour les recommandations B.I. ou
   * renonciation. Vert pour OPTION_LIBRE_DELAI_OK. Navy info pour
   * QUALIFICATION_INCOMPLETE.
   */
  verdictBannerClass(verdict: SuccessionBeAcceptationRenonciationVerdict): string {
    switch (verdict) {
      case 'OPTION_LIBRE_DELAI_OK':
        return 'sba-verdict-banner sba-verdict-banner--ok';
      case 'OPTION_RECOMMANDEE_BENEFICE_INVENTAIRE':
      case 'OPTION_RECOMMANDEE_RENONCIATION':
        return 'sba-verdict-banner sba-verdict-banner--medium';
      case 'ACCEPTATION_TACITE_PROBABLE':
      case 'DELAI_CRITIQUE':
      case 'DELAI_DEPASSE':
        return 'sba-verdict-banner sba-verdict-banner--danger';
      case 'QUALIFICATION_INCOMPLETE':
        return 'sba-verdict-banner sba-verdict-banner--info';
    }
  }

  verdictBannerLabel(verdict: SuccessionBeAcceptationRenonciationVerdict): string {
    switch (verdict) {
      case 'OPTION_LIBRE_DELAI_OK': return 'Option libre, délai OK';
      case 'OPTION_RECOMMANDEE_BENEFICE_INVENTAIRE': return 'Acceptation sous bénéfice d\'inventaire recommandée';
      case 'OPTION_RECOMMANDEE_RENONCIATION': return 'Renonciation recommandée';
      case 'ACCEPTATION_TACITE_PROBABLE': return 'Acceptation tacite probable';
      case 'DELAI_CRITIQUE': return 'Délai critique';
      case 'DELAI_DEPASSE': return 'Délai dépassé';
      case 'QUALIFICATION_INCOMPLETE': return 'Qualification incomplète';
    }
  }

  verdictBannerIcon(verdict: SuccessionBeAcceptationRenonciationVerdict): string {
    switch (verdict) {
      case 'OPTION_LIBRE_DELAI_OK': return 'check_circle';
      case 'OPTION_RECOMMANDEE_BENEFICE_INVENTAIRE':
      case 'OPTION_RECOMMANDEE_RENONCIATION':
        return 'warning';
      case 'ACCEPTATION_TACITE_PROBABLE':
      case 'DELAI_CRITIQUE':
      case 'DELAI_DEPASSE':
        return 'error';
      case 'QUALIFICATION_INCOMPLETE': return 'info';
    }
  }

  optionRecommandeeLabel(opt: OptionRecommandeeSuccessionBe): string {
    switch (opt) {
      case 'ACCEPTER': return 'Accepter purement et simplement';
      case 'ACCEPTER_BENEFICE_INVENTAIRE': return 'Accepter sous bénéfice d\'inventaire';
      case 'RENONCER': return 'Renoncer';
    }
  }

  /** Couleur du statut du délai. Rouge pour CRITIQUE / DEPASSE. */
  delaiStatutClass(statut: DelaiStatutBe): string {
    switch (statut) {
      case 'OK': return 'sba-delai-statut sba-delai-statut--ok';
      case 'CRITIQUE':
      case 'DEPASSE':
        return 'sba-delai-statut sba-delai-statut--danger';
    }
  }

  delaiStatutLabel(statut: DelaiStatutBe): string {
    switch (statut) {
      case 'OK': return 'Délai OK';
      case 'CRITIQUE': return 'Délai critique';
      case 'DEPASSE': return 'Délai dépassé';
    }
  }

  /** Couleur de la sévérité d'un risque. Rouge pour HIGH. */
  severiteClass(severite: SeveriteRisqueBe): string {
    switch (severite) {
      case 'HIGH': return 'sba-severite sba-severite--high';
      case 'MEDIUM': return 'sba-severite sba-severite--medium';
      case 'LOW': return 'sba-severite sba-severite--low';
    }
  }

  severiteLabel(severite: SeveriteRisqueBe): string {
    switch (severite) {
      case 'HIGH': return 'Sévérité élevée';
      case 'MEDIUM': return 'Sévérité moyenne';
      case 'LOW': return 'Sévérité faible';
    }
  }

  qualiteLabel(q: QualiteHeritierBe): string {
    const opt = this.qualiteOptions.find(o => o.value === q);
    return opt?.label ?? q;
  }

  volonteLabel(v: VolonteClientSuccessionBe): string {
    const opt = this.volonteOptions.find(o => o.value === v);
    return opt?.label ?? v;
  }

  acteLabel(a: ActeHeritierBe): string {
    const opt = this.acteOptions.find(o => o.value === a);
    return opt?.label ?? a;
  }
}
