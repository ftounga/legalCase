import {
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
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ReferesAdminService } from '../../core/services/referes-admin.service';
import {
  DECISIONS_CONTESTEES,
  DecisionContestee,
  DecisionContesteeOption,
  PREUVES_URGENCE,
  PreuveUrgence,
  PreuveUrgenceOption,
  ReferesAdminRequest,
  ReferesAdminResponse,
  TYPES_REFERE,
  TypeRefere,
  TypeRefereOption,
  VERDICT_LABELS,
  VerdictRecommandation,
} from '../../core/models/referes-admin.model';
import {
  ImmigrationExtractedData,
  PieceManquanteEntry,
} from '../../core/models/case-analysis.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { AiQuestion } from '../../core/models/ai-question.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { CoherencePopoverTriggerDirective } from '../../shared/coherence-popover/coherence-popover-trigger.directive';
import {
  CoherenceAlert,
  CoherenceAlertSeverity,
} from '../../shared/coherence-popover/coherence-alert.model';
import { CoherenceAlertBuilder } from '../../shared/coherence-popover/coherence-alert-builder';
import { DecisionalHeaderFlagComponent } from '../decisional-tools-panel/decisional-header-flag/decisional-header-flag.component';
import { SourceExplanation } from '../../core/models/source-explanation.model';
import { SourceExplanationService } from '../../core/services/source-explanation.service';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { ReferesAdminPrefillRules } from './referes-admin-section-prefill-rules';

/** Regex ISO strict YYYY-MM-DD. */
const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

const DECISIONS_SET: ReadonlySet<DecisionContestee> = new Set<DecisionContestee>([
  'OQTF', 'RETRAIT_TITRE', 'REFUS_TITRE', 'IRTF', 'AUTRE_MESURE_ADMIN',
]);

export type RefereAlertField =
  | 'DATE_NOTIFICATION'
  | 'DECISION_CONTESTEE'
  | 'ATTEINTE_LIBERTE';

export type RefereAlertSeverity = CoherenceAlertSeverity;
export type RefereCoherenceAlert = CoherenceAlert<RefereAlertField>;

/**
 * SF-IM-08-08 : outil décisionnel "Référés administratifs L.521-1 / L.521-2"
 * (F-IM-08). FR uniquement. Consomme l'API figée par SF-IM-08-07 (backend).
 *
 * Procédure : contestation OQTF/IRTF/RETRAIT/REFUS via référé suspension
 * (L.521-1 CJA, délai juge typique 30 jours) ou référé liberté (L.521-2 CJA,
 * délai juge 48h, art. R.522-1). Affiché conditionnellement par le panel
 * F-IA-04 (tool_id 'F-IM-08-referes-admin-fr').
 *
 * Palette : rouge dominant — autorisée par DESIGN_SYSTEM.md pour les
 * urgences absolues (≤ 72h). Le L.521-2 a un délai de jugement de 48h.
 *
 * Pré-fill IA + validation F-IA-03 obligatoires (règle CLAUDE.md).
 */
@Component({
  selector: 'app-referes-admin-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatSlideToggleModule, MatChipsModule, MatProgressSpinnerModule,
    LegalCitationsPipe,
    CoherencePopoverTriggerDirective,
    DecisionalHeaderFlagComponent,
  ],
  templateUrl: './referes-admin-section.component.html',
  styleUrl: './referes-admin-section.component.scss',
})
export class ReferesAdminSectionComponent implements OnInit, OnChanges {
  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'RÉFÉRÉS ADMINISTRATIFS L.521-1 / L.521-2 (FR)';
  static readonly TOOL_ICON = 'gavel';

  /** F-236 SF-236-02 — Délègue intégralement au helper pur partagé. */
  static getPrefillCount(input: PrefillCountInput): number {
    return ReferesAdminPrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';

  // Sources IA (pré-fill + alertes F-IA-03).
  @Input() aiData?: ImmigrationExtractedData | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;
  // F-177 SF-177-03b : force l'expansion (mode modal F-177).
  @Input() forceExpanded = false;


  /**
   * F-163 SF-163-02d — Mode simulateur autonome (hors dossier client).
   *
   * Quand `true` :
   *  - Bannière « 🧪 Mode simulateur » affichée en haut.
   *  - `prefillFromAi()` court-circuité.
   *  - `coherenceAlerts` retourne `{}`.
   *  - `loadExisting()` / `load()` court-circuité.
   *  - `analyze()` POSTe sur `/api/v1/simulators/F-IM-08-referes-admin-fr/calculate`.
   *  - `triggerRefresh()` jamais invoqué.
   *
   * Default `false` — mode case-file scoped inchangé.
   */
  @Input() standaloneMode: boolean = false;
  // Signals miroirs des inputs IA pour réactivité computed.
  private aiDataSignal = signal<ImmigrationExtractedData | null | undefined>(undefined);
  private procedureChecksSignal = signal<ProcedureCheck[]>([]);
  private aiQuestionsSignal = signal<AiQuestion[]>([]);
  private piecesManquantesSignal = signal<PieceManquanteEntry[]>([]);

  collapsed = signal(true);
  loading = signal(false);
  analyzing = signal(false);
  showForm = signal(true);
  result = signal<ReferesAdminResponse | null>(null);

  // Champs form.
  typeRefere = signal<TypeRefere | null>(null);
  decisionContestee = signal<DecisionContestee | null>(null);
  dateNotificationDecision = signal<string | null>(null);
  urgenceCaracterisee = signal<boolean>(false);
  atteinteLiberteFondamentale = signal<boolean>(false);
  doutesSerieuxLegalite = signal<boolean>(false);
  preuvesUrgence = signal<PreuveUrgence[]>([]);
  demandeurDejaPrived = signal<boolean>(false);

  // Provenance IA par champ.
  provenanceTypeRefere = signal<'IA' | null>(null);
  provenanceDecisionContestee = signal<'IA' | null>(null);
  provenanceDateNotification = signal<'IA' | null>(null);
  provenancePreuvesUrgence = signal<'IA' | null>(null);

  readonly typesRefere: TypeRefereOption[] = TYPES_REFERE;
  readonly decisionsContestees: DecisionContesteeOption[] = DECISIONS_CONTESTEES;
  readonly preuvesUrgenceOptions: PreuveUrgenceOption[] = PREUVES_URGENCE;
  readonly verdictLabels = VERDICT_LABELS;

  /** Aujourd'hui en YYYY-MM-DD pour l'attribut max des inputs date. */
  readonly todayIso: string = new Date().toISOString().slice(0, 10);

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  /** Alertes F-IA-03 — gate strict : seulement quand le formulaire est affiché. */
  coherenceAlerts = computed<Partial<Record<RefereAlertField, RefereCoherenceAlert>>>(() => {
    // F-163 SF-163-02d : aucune source IA en standalone.
    if (this.standaloneMode) return {};
    if (!this.showForm()) return {};
    if (!this.isFrance()) return {};
    const alerts: Partial<Record<RefereAlertField, RefereCoherenceAlert>> = {};
    const a1 = this.buildDateNotificationAlert();
    if (a1) alerts.DATE_NOTIFICATION = a1;
    const a2 = this.buildDecisionContesteeAlert();
    if (a2) alerts.DECISION_CONTESTEE = a2;
    const a3 = this.buildAtteinteLiberteAlert();
    if (a3) alerts.ATTEINTE_LIBERTE = a3;
    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    const blockers = values.filter((a) => a?.severity === 'CRITICAL').length;
    return { total: values.length, blockers };
  });

  // Source explanations (SF-155-07 DIV-7).
  sourceExplanations = signal<Map<string, SourceExplanation[]>>(new Map());

  constructor(
    private service: ReferesAdminService,
    private snackBar: MatSnackBar,
    @Optional() private dashboardRefresh: CaseDashboardRefreshService,
    @Optional() private sourceExplanationService: SourceExplanationService | null,
  ) {}

  ngOnInit(): void {
    // F-177 SF-177-03b : appliqué dès le mount pour le mode modal.
    if (this.forceExpanded) this.collapsed.set(false);
    this.aiDataSignal.set(this.aiData);
    this.procedureChecksSignal.set(this.procedureChecks ?? []);
    this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    this.piecesManquantesSignal.set(this.piecesManquantes ?? []);
    if (this.standaloneMode) {

      // F-163 SF-163-02d : pas de dossier à interroger en standalone.

      this.collapsed.set(false);

      this.loading.set(false);

      this.showForm.set(true);

      return;

    }
    if (this.isFrance()) {
      this.load();
      this.loadSourceExplanations();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    // F-177 SF-177-03b : applique le forceExpanded quand il passe à true en cours de vie.
    if (changes['forceExpanded'] && this.forceExpanded) this.collapsed.set(false);
    if (changes['aiData']) this.aiDataSignal.set(this.aiData);
    if (changes['procedureChecks']) this.procedureChecksSignal.set(this.procedureChecks ?? []);
    if (changes['aiQuestions']) this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    if (changes['piecesManquantes']) this.piecesManquantesSignal.set(this.piecesManquantes ?? []);
    // Re-prefill si aiData arrive après mount avant la première résolution.
    if (
      changes['aiData'] &&
      !changes['aiData'].firstChange &&
      this.isFrance() &&
      this.showForm() &&
      !this.result()
    ) {
      this.prefillFromAi();
    }
  }

  private loadSourceExplanations(): void {
    if (!this.caseFileId) return;
    if (!this.sourceExplanationService) return;
    this.sourceExplanationService.getForCaseFile(this.caseFileId).subscribe({
      next: (map) => this.sourceExplanations.set(map),
      error: () => { /* fail-open */ },
    });
  }

  /** Mapping field → sourceKey. Convention `IM08RA_<FIELD>`. */
  explanationFor(field: RefereAlertField): SourceExplanation[] {
    const key = (() => {
      switch (field) {
        case 'DATE_NOTIFICATION': return 'IM08RA_DATE_NOTIFICATION';
        case 'DECISION_CONTESTEE': return 'IM08RA_DECISION_CONTESTEE';
        case 'ATTEINTE_LIBERTE': return 'IM08RA_ATTEINTE_LIBERTE';
      }
    })();
    return this.sourceExplanations().get(key) ?? [];
  }

  toggleCollapse(): void {
    this.collapsed.update((v) => !v);
  }

  formValid(): boolean {
    const tr = this.typeRefere();
    const dc = this.decisionContestee();
    const dn = this.dateNotificationDecision();
    if (!tr || !dc || !dn) return false;
    if (dn > this.todayIso) return false;
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // ----- Handlers manuels (effacent provenance IA). -----

  onTypeRefereChange(value: TypeRefere | null): void {
    this.typeRefere.set(value);
    this.provenanceTypeRefere.set(null);
  }

  onDecisionContesteeChange(value: DecisionContestee | null): void {
    this.decisionContestee.set(value);
    this.provenanceDecisionContestee.set(null);
  }

  onDateNotificationChange(value: string | null): void {
    this.dateNotificationDecision.set(value || null);
    this.provenanceDateNotification.set(null);
  }

  onUrgenceChange(value: boolean): void {
    this.urgenceCaracterisee.set(value);
  }

  onAtteinteLiberteChange(value: boolean): void {
    this.atteinteLiberteFondamentale.set(value);
  }

  onDoutesChange(value: boolean): void {
    this.doutesSerieuxLegalite.set(value);
  }

  onPreuvesUrgenceChange(value: PreuveUrgence[] | null): void {
    this.preuvesUrgence.set(value ?? []);
    this.provenancePreuvesUrgence.set(null);
  }

  onDemandeurPrivedChange(value: boolean): void {
    this.demandeurDejaPrived.set(value);
  }

  analyze(): void {
    if (!this.formValid()) return;
    const request: ReferesAdminRequest = {
      typeRefere: this.typeRefere()!,
      decisionContestee: this.decisionContestee()!,
      dateNotificationDecision: this.dateNotificationDecision()!,
      urgenceCaracterisee: this.urgenceCaracterisee(),
      atteinteLiberteFondamentale: this.atteinteLiberteFondamentale(),
      doutesSerieuxLegalite: this.doutesSerieuxLegalite(),
      preuvesUrgence: this.preuvesUrgence(),
      demandeurDejaPrived: this.demandeurDejaPrived(),
    };
    this.analyzing.set(true);
    // F-163 SF-163-02d : en standalone, POST sur le dispatcher générique.

    const request$ = this.standaloneMode

      ? this.service.analyzeStandalone(request)

      : this.service.analyze(this.caseFileId, request);

    request$.subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('Référés administratifs analysés', 'OK', { duration: 2500 });
        // F-163 SF-163-02d : pas de dashboard à rafraîchir en standalone.

        if (!this.standaloneMode) { this.dashboardRefresh?.triggerRefresh(); }
      },
      error: (err) => {
        this.analyzing.set(false);
        const msg = err?.error?.message || err?.error || 'Erreur lors de l\'analyse';
        this.snackBar.open(String(msg), 'Fermer', { duration: 5000, panelClass: 'snack-error' });
      },
    });
  }

  // ----- Affichage verdict / scores -----

  verdictLabel(verdict: VerdictRecommandation | null | undefined): string {
    if (!verdict) return '';
    return this.verdictLabels[verdict];
  }

  /**
   * Palette rouge dominant — urgence absolue 48h L.521-2.
   * Documenté dans DESIGN_SYSTEM.md (gradation autorisée pour < 72h).
   */
  verdictBannerClass(verdict: VerdictRecommandation | null | undefined): string {
    switch (verdict) {
      case 'REFERE_LIBERTE_PRIORITAIRE':   return 'ra-banner ra-banner--danger-strong';
      case 'LES_DEUX_CUMULES':             return 'ra-banner ra-banner--danger-strong';
      case 'REFERE_SUSPENSION_PRIORITAIRE':return 'ra-banner ra-banner--danger-medium';
      case 'AUCUN_PROBABLE':               return 'ra-banner ra-banner--info';
      default:                             return 'ra-banner';
    }
  }

  verdictBannerIcon(verdict: VerdictRecommandation | null | undefined): string {
    switch (verdict) {
      case 'REFERE_LIBERTE_PRIORITAIRE':   return 'gpp_maybe';
      case 'LES_DEUX_CUMULES':             return 'priority_high';
      case 'REFERE_SUSPENSION_PRIORITAIRE':return 'warning';
      case 'AUCUN_PROBABLE':               return 'info_outline';
      default:                             return 'info_outline';
    }
  }

  /** Classe CSS d'un score selon sa valeur (couleur cohérente avec verdict). */
  scoreClass(score: number): string {
    if (score >= 70) return 'ra-score--high';
    if (score >= 40) return 'ra-score--medium';
    return 'ra-score--low';
  }

  alertBadgeLabel(alert: RefereCoherenceAlert): string {
    const prefix = alert.severity === 'CRITICAL'
      ? 'Risque d\'irrecevabilité'
      : 'Incohérence détectée';
    return `${prefix} (${alert.expectedDisplay})`;
  }

  // ----- Pré-fill IA -----

  private prefillFromAi(): void {
    // F-236 SF-236-02 : délègue au helper pur partagé. Garde-fou supplémentaire :
    // ne pas écraser si le form n'est pas affiché (analyse déjà faite).
    if (!this.isFrance()) return;
    if (!this.showForm()) return;
    const input: PrefillCountInput = {
      aiData: this.aiData,
      workspaceCountry: this.workspaceCountry,
    };

    const date = ReferesAdminPrefillRules.computeDateNotificationDecision(input);
    if (date !== null && !this.dateNotificationDecision()) {
      this.dateNotificationDecision.set(date);
      this.provenanceDateNotification.set('IA');
    }

    const decision = ReferesAdminPrefillRules.computeDecisionContestee(input);
    if (decision !== null && !this.decisionContestee()) {
      this.decisionContestee.set(decision);
      this.provenanceDecisionContestee.set('IA');
    }

    const preuves = ReferesAdminPrefillRules.computePreuvesUrgence(input);
    if (preuves !== null && this.preuvesUrgence().length === 0) {
      this.preuvesUrgence.set(preuves);
      this.provenancePreuvesUrgence.set('IA');
    }
  }

  // ----- Builders alertes F-IA-03 -----

  private buildDateNotificationAlert(): RefereCoherenceAlert | null {
    const user = this.dateNotificationDecision();
    if (!user) return null;
    const builder = CoherenceAlertBuilder.forField<RefereAlertField>('DATE_NOTIFICATION')
      .withSeverity('WARNING');

    const ai = this.aiDataSignal()?.dateNotificationDecisionContestee;
    if (typeof ai === 'string' && ISO_DATE_RE.test(ai) && ai !== user) {
      builder.addSource('IA', {
        expectedDisplay: ai,
        reason: `L'analyse a détecté une date de notification différente : ${ai}`,
      });
    }

    const piece = this.findPieceManquante(['IM08RA_DATE_NOTIFICATION', 'IM08_DATE_NOTIFICATION']);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  private buildDecisionContesteeAlert(): RefereCoherenceAlert | null {
    const user = this.decisionContestee();
    if (!user) return null;
    const builder = CoherenceAlertBuilder.forField<RefereAlertField>('DECISION_CONTESTEE')
      .withSeverity('WARNING');

    // IA : mapping typeRecoursCode (F-236 SF-236-02 : constante dans helper partagé).
    const recoursCode = this.aiDataSignal()?.typeRecoursCode;
    if (recoursCode && ReferesAdminPrefillRules.TYPE_RECOURS_TO_DECISION[recoursCode]) {
      const mapped = ReferesAdminPrefillRules.TYPE_RECOURS_TO_DECISION[recoursCode];
      if (mapped !== user && DECISIONS_SET.has(mapped)) {
        const label = DECISIONS_CONTESTEES.find((d) => d.code === mapped)?.label ?? mapped;
        builder.addSource('IA', {
          expectedDisplay: label,
          reason: `L'analyse a détecté un autre type de décision contestée : ${label}`,
        });
      }
    }

    // F96 : checklist procédurale.
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'IM08RA_DECISION_CONTESTEE') continue;
      const ev = chk.expectedValue?.toUpperCase() as DecisionContestee | undefined;
      if (!ev || !DECISIONS_SET.has(ev)) continue;
      if (ev === user) continue;
      const label = DECISIONS_CONTESTEES.find((d) => d.code === ev)?.label ?? ev;
      builder.addSource('F96', {
        expectedDisplay: label,
        reason: `Checklist procédurale : décision contestée attendue ${label}${chk.raison ? ' (' + chk.raison + ')' : ''}`,
      });
      break;
    }

    const piece = this.findPieceManquante(['IM08RA_DECISION_CONTESTEE']);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  /**
   * Alerte CRITIQUE : avocat coche urgence + L.521-2 mais NON sur
   * atteinteLiberteFondamentale → conditions cumulatives L.521-2 non
   * remplies (atteinte grave et manifestement illégale à une liberté
   * fondamentale est exigée par l'art. L.521-2 CJA).
   */
  private buildAtteinteLiberteAlert(): RefereCoherenceAlert | null {
    const tr = this.typeRefere();
    if (tr !== 'LIBERTE' && tr !== 'LES_DEUX') return null;
    if (this.atteinteLiberteFondamentale()) return null;
    const builder = CoherenceAlertBuilder.forField<RefereAlertField>('ATTEINTE_LIBERTE')
      .withSeverity('CRITICAL')
      .addSource('IA', {
        expectedDisplay: 'Atteinte à liberté fondamentale requise',
        reason: 'Le référé liberté (L.521-2 CJA) exige une atteinte grave et manifestement illégale à une liberté fondamentale. Sans cette condition, la requête sera probablement rejetée.',
      });

    const piece = this.findPieceManquante(['IM08RA_ATTEINTE_LIBERTE']);
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

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.typeRefere.set(r.typeRefere);
        this.decisionContestee.set(r.decisionContestee);
        this.dateNotificationDecision.set(r.dateNotificationDecision);
        this.urgenceCaracterisee.set(r.urgenceCaracterisee);
        this.atteinteLiberteFondamentale.set(r.atteinteLiberteFondamentale);
        this.doutesSerieuxLegalite.set(r.doutesSerieuxLegalite);
        this.preuvesUrgence.set(r.preuvesUrgence ?? []);
        this.demandeurDejaPrived.set(r.demandeurDejaPrived);
        this.showForm.set(false);
        // Reset provenance — les valeurs viennent du backend, plus de l'IA.
        this.provenanceTypeRefere.set(null);
        this.provenanceDecisionContestee.set(null);
        this.provenanceDateNotification.set(null);
        this.provenancePreuvesUrgence.set(null);
        this.loading.set(false);
      },
      error: () => {
        // 404 attendu si aucune analyse — on tente le pré-fill IA.
        this.prefillFromAi();
        this.loading.set(false);
      },
    });
  }
}
