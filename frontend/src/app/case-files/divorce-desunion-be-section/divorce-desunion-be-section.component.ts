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
import { CommonModule, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';

import { DivorceDesunionBeService } from '../../core/services/divorce-desunion-be.service';
import {
  DivorceDesunionBeRequest,
  DivorceDesunionBeResponse,
  DivorceDesunionBeVerdict,
} from '../../core/models/divorce-desunion-be.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { AiQuestion } from '../../core/models/ai-question.model';
import { PieceManquanteEntry } from '../../core/models/case-analysis.model';
import { CoherencePopoverTriggerDirective } from '../../shared/coherence-popover/coherence-popover-trigger.directive';
import { CoherenceAlert } from '../../shared/coherence-popover/coherence-alert.model';
import { CoherenceAlertBuilder } from '../../shared/coherence-popover/coherence-alert-builder';

/**
 * SF-FA-11-02 : champs audités par F-IA-03 pour la désunion irrémédiable BE.
 */
export type DesUBeAlertField =
  | 'DATE_SEPARATION'
  | 'SEPARATION_CONSENTUE'
  | 'DATE_ASSIGNATION';

export type DesUBeCoherenceAlert = CoherenceAlert<DesUBeAlertField>;

/**
 * SF-FA-11-02 : mapping field → critereCodes acceptés (F-96 / QUESTION_IA /
 * PIECE_MANQUANTE). Permet le scan multi-source sans dupliquer la logique.
 */
const FIELD_CRITERE_CODES: Readonly<Record<DesUBeAlertField, readonly string[]>> = {
  DATE_SEPARATION: ['DESU_BE_DATE_SEPARATION'],
  SEPARATION_CONSENTUE: ['DESU_BE_CONSENTEE', 'DESU_BE_SEPARATION_CONSENTUE'],
  DATE_ASSIGNATION: ['DESU_BE_DATE_ASSIGNATION'],
};

/**
 * SF-FA-11-02 : composant outil décisionnel "Divorce pour désunion irrémédiable"
 * (Code civil belge, art. 229 — loi du 27/04/2007). BE uniquement.
 *
 * Pattern emprunté à `divorce-alteration-section` (F-FA-08, FR — structure
 * verdict + carte délai très proche) et `belgian-9bis-section` (gate BE +
 * pré-fill IA + coherenceAlerts via builder partagé). Palette navy / or —
 * rouge réservé au verdict FAIBLE (alerte non-recevabilité).
 *
 * - Outil single-country BE. La gate `workspaceCountry === 'FRANCE'` masque
 *   le formulaire et affiche une bannière info pointant vers F-FA-08 / F-FA-10.
 * - Pré-fill IA via `aiData?: Partial<FamilleExtractedData>` — no-op gracieux
 *   si champs absents (règle CLAUDE.md ligne 190).
 * - Validation F-IA-03 au changement (CoherenceAlertBuilder partagé).
 */
@Component({
  selector: 'app-divorce-desunion-be-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule, DecimalPipe,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatSlideToggleModule, MatProgressSpinnerModule,
    LegalCitationsPipe,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './divorce-desunion-be-section.component.html',
  styleUrl: './divorce-desunion-be-section.component.scss',
})
export class DivorceDesunionBeSectionComponent implements OnInit, OnChanges {
  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'DIVORCE BE — DÉSUNION IRRÉMÉDIABLE (ART. 229 CC)';
  static readonly TOOL_ICON = 'balance';

  @Input() caseFileId!: string;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'BELGIQUE';
  /** SF-FA-11-02 : pré-fill IA Famille — facultatif, no-op si absent. */
  @Input() aiData?: Partial<FamilleExtractedData> | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;

  // Snapshots signals — alimentent les `computed`.
  private aiDataSignal = signal<Partial<FamilleExtractedData> | null | undefined>(undefined);
  private procedureChecksSignal = signal<ProcedureCheck[]>([]);
  private aiQuestionsSignal = signal<AiQuestion[]>([]);
  private piecesManquantesSignal = signal<PieceManquanteEntry[]>([]);

  // F-177 SF-177-03b : force l'expansion (mode modal F-177).
  @Input() forceExpanded = false;

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<DivorceDesunionBeResponse | null>(null);

  // Form signals (6 champs)
  dateSeparation = signal<string | null>(null);
  separationConsentue = signal<boolean>(false);
  preuvesSeparation = signal<boolean>(false);
  preuvesDocumentaires = signal<boolean>(false);
  tentativesReconciliation = signal<boolean>(false);
  dateAssignation = signal<string | null>(null);

  // Provenance IA par champ pré-remplissable.
  provenanceDateSeparation = signal<'IA' | null>(null);
  provenanceSeparationConsentue = signal<'IA' | null>(null);
  // Crochets prêts pour future intégration IA des autres fields (no-op aujourd'hui).
  provenancePreuvesSeparation = signal<'IA' | null>(null);
  provenancePreuvesDocumentaires = signal<'IA' | null>(null);
  provenanceTentativesReconciliation = signal<'IA' | null>(null);
  provenanceDateAssignation = signal<'IA' | null>(null);

  /** Aujourd'hui en YYYY-MM-DD pour [max] des champs date. */
  readonly todayIso: string = new Date().toISOString().slice(0, 10);

  /** SF-FA-11-02 : alertes de cohérence F-IA-03 (gate showForm). */
  coherenceAlerts = computed<Partial<Record<DesUBeAlertField, DesUBeCoherenceAlert>>>(() => {
    if (!this.showForm()) return {};
    const alerts: Partial<Record<DesUBeAlertField, DesUBeCoherenceAlert>> = {};
    const dateSepAlert = this.buildDateSeparationAlert();
    if (dateSepAlert) alerts.DATE_SEPARATION = dateSepAlert;
    const consenteeAlert = this.buildSeparationConsentueAlert();
    if (consenteeAlert) alerts.SEPARATION_CONSENTUE = consenteeAlert;
    const dateAssAlert = this.buildDateAssignationAlert();
    if (dateAssAlert) alerts.DATE_ASSIGNATION = dateAssAlert;
    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    return { total: values.length, blockers: 0 };
  });

  constructor(
    private service: DivorceDesunionBeService,
    private snackBar: MatSnackBar,
    @Optional() private dashboardRefresh: CaseDashboardRefreshService | null,
  ) {}

  ngOnInit(): void {
    // F-177 SF-177-03b : appliqué dès le mount pour le mode modal.
    if (this.forceExpanded) this.collapsed.set(false);
    this.aiDataSignal.set(this.aiData);
    this.procedureChecksSignal.set(this.procedureChecks ?? []);
    this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    this.piecesManquantesSignal.set(this.piecesManquantes ?? []);
    if (this.isBelgiumGate()) {
      this.load();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    // F-177 SF-177-03b : applique le forceExpanded quand il passe à true en cours de vie.
    if (changes['forceExpanded'] && this.forceExpanded) this.collapsed.set(false);
    if (changes['aiData']) this.aiDataSignal.set(this.aiData);
    if (changes['procedureChecks']) this.procedureChecksSignal.set(this.procedureChecks ?? []);
    if (changes['aiQuestions']) this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    if (changes['piecesManquantes']) this.piecesManquantesSignal.set(this.piecesManquantes ?? []);

    // Ré-applique le prefill dès que aiData change avant première résolution
    // backend — ne JAMAIS écraser si un résultat persisté est déjà rendu.
    if (changes['aiData'] && !changes['aiData'].firstChange
        && this.isBelgiumGate() && this.showForm() && !this.result()) {
      this.prefillFromAi();
    }
  }

  toggleCollapse(): void {
    this.collapsed.update((v) => !v);
  }

  /** SF-FA-11-02 : true si la gate BE est satisfaite. */
  isBelgiumGate(): boolean {
    return this.workspaceCountry === 'BELGIQUE';
  }

  // ---------------------------------------------------------------------------
  // Form / handlers
  // ---------------------------------------------------------------------------

  formValid(): boolean {
    if (!this.isBelgiumGate()) return false;
    const date = this.dateSeparation();
    if (!date) return false;
    if (date > this.todayIso) return false;
    const dateAss = this.dateAssignation();
    if (dateAss && dateAss < date) return false;
    if (dateAss && dateAss > this.todayIso) return false;
    return true;
  }

  onDateSeparationChange(value: string | null): void {
    this.dateSeparation.set(value || null);
    this.provenanceDateSeparation.set(null);
  }

  onSeparationConsentueChange(value: boolean): void {
    this.separationConsentue.set(value);
    this.provenanceSeparationConsentue.set(null);
  }

  onPreuvesSeparationChange(value: boolean): void {
    this.preuvesSeparation.set(value);
    this.provenancePreuvesSeparation.set(null);
  }

  onPreuvesDocumentairesChange(value: boolean): void {
    this.preuvesDocumentaires.set(value);
    this.provenancePreuvesDocumentaires.set(null);
  }

  onTentativesReconciliationChange(value: boolean): void {
    this.tentativesReconciliation.set(value);
    this.provenanceTentativesReconciliation.set(null);
  }

  onDateAssignationChange(value: string | null): void {
    this.dateAssignation.set(value || null);
    this.provenanceDateAssignation.set(null);
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // ---------------------------------------------------------------------------
  // Pré-fill IA
  // ---------------------------------------------------------------------------

  /**
   * SF-FA-11-02 : pré-fill IA depuis FamilleExtractedData. Règles fail-open :
   * - aiData absent → no-op (l'avocat saisit manuellement)
   * - dateSeparation (string) → dateSeparation + provenance IA
   * - separationConsentue (boolean) → separationConsentue + provenance IA
   * - Les autres fields (preuvesSeparation, preuvesDocumentaires,
   *   tentativesReconciliation) n'ont pas de mapping IA natif — crochets prêts.
   */
  private prefillFromAi(): void {
    const ai = this.aiDataSignal();
    if (!ai) return;

    if (typeof ai.dateSeparation === 'string' && ai.dateSeparation.length > 0) {
      if (this.dateSeparation() === null
          || this.provenanceDateSeparation() === 'IA') {
        this.dateSeparation.set(ai.dateSeparation);
        this.provenanceDateSeparation.set('IA');
      }
    }
    if (typeof ai.separationConsentue === 'boolean') {
      // Pas d'écrasement si l'avocat a déjà saisi (provenance null).
      if (this.provenanceSeparationConsentue() === 'IA' || !this.separationConsentue()) {
        this.separationConsentue.set(ai.separationConsentue);
        this.provenanceSeparationConsentue.set('IA');
      }
    }
  }

  // ---------------------------------------------------------------------------
  // Coherence alerts builders (F-IA-03)
  // ---------------------------------------------------------------------------

  private buildDateSeparationAlert(): DesUBeCoherenceAlert | null {
    if (!this.isBelgiumGate()) return null;
    const user = this.dateSeparation();
    if (!user) return null;

    const builder = CoherenceAlertBuilder.forField<DesUBeAlertField>('DATE_SEPARATION');

    // 1. F-96 VERIFIED
    this.scanProcedureChecks(builder, 'DATE_SEPARATION', user);
    // 2. QUESTION_IA "oui"
    this.scanAiQuestions(builder, 'DATE_SEPARATION', user);

    // 3. IA aiData.dateSeparation
    const iaDate = this.aiDataSignal()?.dateSeparation;
    if (typeof iaDate === 'string' && iaDate.length > 0 && iaDate !== user) {
      builder.addSource('IA', {
        expectedDisplay: iaDate,
        reason: `Analyse du dossier : date de séparation ${iaDate}`,
      });
    }

    // 4. PIECE_MANQUANTE
    const piece = this.findPieceManquante(FIELD_CRITERE_CODES.DATE_SEPARATION);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  private buildSeparationConsentueAlert(): DesUBeCoherenceAlert | null {
    if (!this.isBelgiumGate()) return null;
    const userBool = this.separationConsentue();
    const userStr = userBool ? 'true' : 'false';

    const builder = CoherenceAlertBuilder.forField<DesUBeAlertField>('SEPARATION_CONSENTUE');

    this.scanProcedureChecks(builder, 'SEPARATION_CONSENTUE', userStr);
    this.scanAiQuestions(builder, 'SEPARATION_CONSENTUE', userStr);

    const iaConsentee = this.aiDataSignal()?.separationConsentue;
    if (typeof iaConsentee === 'boolean' && iaConsentee !== userBool) {
      const iaStr = iaConsentee ? 'Consentue' : 'Non consentue';
      builder.addSource('IA', {
        expectedDisplay: iaStr,
        reason: `Analyse du dossier : séparation ${iaStr.toLowerCase()}`,
      });
    }

    const piece = this.findPieceManquante(FIELD_CRITERE_CODES.SEPARATION_CONSENTUE);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  private buildDateAssignationAlert(): DesUBeCoherenceAlert | null {
    if (!this.isBelgiumGate()) return null;
    const user = this.dateAssignation();
    if (!user) return null;

    const builder = CoherenceAlertBuilder.forField<DesUBeAlertField>('DATE_ASSIGNATION');

    this.scanProcedureChecks(builder, 'DATE_ASSIGNATION', user);
    this.scanAiQuestions(builder, 'DATE_ASSIGNATION', user);

    // Pas d'IA native sur dateAssignation aujourd'hui — crochet prêt.
    const piece = this.findPieceManquante(FIELD_CRITERE_CODES.DATE_ASSIGNATION);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  private scanProcedureChecks(
    builder: CoherenceAlertBuilder<DesUBeAlertField>,
    field: DesUBeAlertField,
    userValue: string | null,
  ): void {
    const codes = new Set(FIELD_CRITERE_CODES[field].map((c) => c.toUpperCase()));
    for (const chk of this.procedureChecksSignal()) {
      const chkCode = chk.critereCode?.toUpperCase();
      if (!chkCode || !codes.has(chkCode)) continue;
      if (chk.statut !== 'VERIFIED') continue;
      const ev = chk.expectedValue;
      if (!ev) continue;
      if (userValue !== null && userValue.toLowerCase() === ev.toLowerCase()) continue;
      builder.addSource('F96', {
        expectedDisplay: ev,
        reason: `Checklist procédurale : ${ev}${chk.raison ? ' (' + chk.raison + ')' : ''}`,
      });
      break;
    }
  }

  private scanAiQuestions(
    builder: CoherenceAlertBuilder<DesUBeAlertField>,
    field: DesUBeAlertField,
    userValue: string | null,
  ): void {
    const codes = new Set(FIELD_CRITERE_CODES[field].map((c) => c.toUpperCase()));
    for (const q of this.aiQuestionsSignal()) {
      const qCode = q.critereCode?.toUpperCase();
      if (!qCode || !codes.has(qCode)) continue;
      const answer = q.answerText?.trim().toLowerCase();
      if (!answer) continue;
      const isOui = answer === 'oui' || answer.startsWith('oui ')
        || answer.startsWith('oui,') || answer.startsWith('oui.');
      if (!isOui) continue;
      const ev = q.expectedValue;
      if (!ev) continue;
      if (userValue !== null && userValue.toLowerCase() === ev.toLowerCase()) continue;
      builder.addSource('QUESTION_IA', {
        expectedDisplay: ev,
        reason: `Question complémentaire : "${q.questionText}" → "${q.answerText}"`,
      });
      break;
    }
  }

  private findPieceManquante(acceptedCodes: readonly string[]): string | null {
    const norm = new Set(acceptedCodes.map((c) => c.toUpperCase()));
    for (const p of this.piecesManquantesSignal()) {
      const code = p.critereCode?.toUpperCase();
      if (!code) continue;
      if (norm.has(code)) return p.texte;
    }
    return null;
  }

  alertTooltip(alert: DesUBeCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: DesUBeCoherenceAlert): string {
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
  // Verdict / display helpers
  // ---------------------------------------------------------------------------

  verdictLabel(verdict: DivorceDesunionBeVerdict): string {
    switch (verdict) {
      case 'ELEVEE': return 'Probabilité élevée';
      case 'MOYENNE': return 'Probabilité moyenne';
      case 'FAIBLE': return 'Probabilité faible';
    }
  }

  verdictClass(verdict: DivorceDesunionBeVerdict): string {
    switch (verdict) {
      case 'ELEVEE': return 'desu-banner desu-banner--ok';
      case 'MOYENNE': return 'desu-banner desu-banner--warn';
      case 'FAIBLE': return 'desu-banner desu-banner--ko';
    }
  }

  // ---------------------------------------------------------------------------
  // HTTP
  // ---------------------------------------------------------------------------

  calculate(): void {
    if (!this.formValid()) return;
    const request: DivorceDesunionBeRequest = {
      dateSeparation: this.dateSeparation()!,
      separationConsentue: this.separationConsentue(),
      preuvesSeparation: this.preuvesSeparation(),
      preuvesDocumentaires: this.preuvesDocumentaires(),
      tentativesReconciliation: this.tentativesReconciliation(),
      dateAssignation: this.dateAssignation(),
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.applyResult(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Analyse désunion irrémédiable enregistrée', 'OK',
          { duration: 2500 });
        this.dashboardRefresh?.triggerRefresh();
      },
      error: (err) => {
        this.calculating.set(false);
        const msg = err?.error?.message || err?.error || 'Erreur lors du calcul';
        this.snackBar.open(String(msg), 'Fermer',
          { duration: 5000, panelClass: 'snack-error' });
      },
    });
  }

  private load(): void {
    if (!this.isBelgiumGate()) return;
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.applyResult(r);
        // Valeurs persistées = saisie avocat historique — pas de badge IA.
        this.provenanceDateSeparation.set(null);
        this.provenanceSeparationConsentue.set(null);
        this.provenancePreuvesSeparation.set(null);
        this.provenancePreuvesDocumentaires.set(null);
        this.provenanceTentativesReconciliation.set(null);
        this.provenanceDateAssignation.set(null);
        this.showForm.set(false);
        this.loading.set(false);
      },
      error: () => {
        // 404 attendu si pas d'analyse persistée — on reste en form mode
        // et on tente le pré-fill IA.
        this.prefillFromAi();
        this.loading.set(false);
      },
    });
  }

  private applyResult(r: DivorceDesunionBeResponse): void {
    this.dateSeparation.set(r.dateSeparation);
    this.separationConsentue.set(r.separationConsentue);
    this.preuvesSeparation.set(r.preuvesSeparation);
    this.preuvesDocumentaires.set(r.preuvesDocumentaires);
    this.tentativesReconciliation.set(r.tentativesReconciliation);
    this.dateAssignation.set(r.dateAssignation);
  }
}
