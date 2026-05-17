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
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ProcedureNulliteLicenciementService } from '../../core/services/procedure-nullite-licenciement.service';
import {
  ProcedureNulliteLicenciementRequest,
  ProcedureNulliteLicenciementResponse,
  ProcedureNulliteVerdict,
} from '../../core/models/procedure-nullite-licenciement.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import {
  PieceManquanteEntry,
  TravailExtractedData,
} from '../../core/models/case-analysis.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { AiQuestion } from '../../core/models/ai-question.model';
import { CoherencePopoverTriggerDirective } from '../../shared/coherence-popover/coherence-popover-trigger.directive';
import { CoherenceAlert } from '../../shared/coherence-popover/coherence-alert.model';
import { CoherenceAlertBuilder } from '../../shared/coherence-popover/coherence-alert-builder';
import { ProcedureNulliteLicenciementSectionPrefillRules } from './procedure-nullite-licenciement-section-prefill-rules';

/**
 * SF-DT-36-02 : champs F-IA-03 audités par l'outil F-DT-36. Les trois champs
 * croisables avec la checklist procédurale F-96 / les questions IA / les pièces
 * manquantes.
 */
export type PNLAlertField = 'DATE_ENTRETIEN' | 'MOTIVATION' | 'ENTRETIEN_TENU';

export type PNLCoherenceAlert = CoherenceAlert<PNLAlertField>;

/** Codes `critereCode` (F-96 / questions IA) reconnus par field. */
const F96_CODE_DATE_ENTRETIEN = 'DT36_DATE_ENTRETIEN';
const F96_CODE_MOTIVATION = 'DT36_MOTIVATION';
const F96_CODE_ENTRETIEN_TENU = 'DT36_ENTRETIEN_TENU';

/**
 * SF-DT-36-02 : composant Angular standalone pour l'outil décisionnel
 * "Nullité de procédure de licenciement" (F-DT-36). FRANCE uniquement.
 *
 * Pattern de référence : `licenciement-nul-detection-section` (F-DT-16) —
 * formulaire de saisie, POST de calcul, verdict 3 niveaux, F-IA-03, refresh
 * dashboard.
 *
 * - Form : dates `<input type="date">` (convocation présentée, entretien
 *   préalable, notification) + toggles (convocation envoyée, entretien tenu,
 *   lettre écrite / motivée, motivation suffisante, motif grave, licenciement
 *   collectif + CSE, convention collective applicable / respectée).
 * - Verdict : NULLITE_AVEREE (rouge) / NULLITE_PROBABLE (or) /
 *   PROCEDURE_REGULIERE (navy). Rouge réservé à `NULLITE_AVEREE`.
 * - Liste des vices : libellé + fondement (JetBrains Mono) + explication.
 * - Pré-fill IA : V1 = 0 champ (`PREFILL_COUNT_ALWAYS_ZERO`).
 * - Validation F-IA-03 sur 3 fields croisables (DATE_ENTRETIEN, MOTIVATION,
 *   ENTRETIEN_TENU).
 * - Refresh dashboard F-IA-02 + MatSnackBar erreurs.
 */
@Component({
  selector: 'app-procedure-nullite-licenciement-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatSlideToggleModule, MatProgressSpinnerModule,
    LegalCitationsPipe,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './procedure-nullite-licenciement-section.component.html',
  styleUrl: './procedure-nullite-licenciement-section.component.scss',
})
export class ProcedureNulliteLicenciementSectionComponent implements OnInit, OnChanges {
  // F-177 SF-177-03b : metadata statique consommée par le panel pour la card.
  static readonly TOOL_LABEL = 'NULLITÉ DE PROCÉDURE DE LICENCIEMENT';
  static readonly TOOL_ICON = 'gavel';

  /**
   * F-236 SF-236-01 / F-237 SF-237-01 — délègue au helper partagé.
   * V1 : aucun flag procédural extrait par l'IA → toujours 0
   * (`PREFILL_COUNT_ALWAYS_ZERO`).
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
    return ProcedureNulliteLicenciementSectionPrefillRules.computePrefillCount({
      aiData: input.aiData,
    });
  }

  @Input() caseFileId!: string;
  // F-177 SF-177-03b : force l'expansion (mode modal F-177).
  @Input() forceExpanded = false;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';
  // Inputs IA / contexte (tous optionnels — null-safe partout).
  @Input() aiData?: TravailExtractedData | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;
  /** Mode simulateur autonome (hors dossier) — coupe F-IA-03 + refresh. */
  @Input() standaloneMode = false;

  // Snapshots signal des inputs IA pour réactivité des `computed`.
  private procedureChecksSignal = signal<ProcedureCheck[]>([]);
  private aiQuestionsSignal = signal<AiQuestion[]>([]);
  private piecesManquantesSignal = signal<PieceManquanteEntry[]>([]);

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<ProcedureNulliteLicenciementResponse | null>(null);

  // --- Form fields ---
  convocationEnvoyee = signal<boolean>(true);
  dateConvocationPresentee = signal<string | null>(null);
  dateEntretienPrealable = signal<string | null>(null);
  entretienTenu = signal<boolean>(true);
  dateNotificationLicenciement = signal<string | null>(null);
  lettreLicenciementEcrite = signal<boolean>(true);
  lettreMotivee = signal<boolean>(true);
  motivationSuffisante = signal<boolean>(true);
  motivationCommentaire = signal<string | null>(null);
  licenciementPourMotifGrave = signal<boolean>(false);
  licenciementCollectif = signal<boolean>(false);
  procedureCseRespectee = signal<boolean>(true);
  conventionCollectiveApplicable = signal<boolean>(false);
  conventionCollectiveRespectee = signal<boolean>(true);
  conventionCollectiveCommentaire = signal<string | null>(null);

  /**
   * Coherence alerts F-IA-03 — gate `showForm()` strict (alertes masquées
   * après calcul rendu). Aucune source IA en standalone.
   */
  coherenceAlerts = computed<Partial<Record<PNLAlertField, PNLCoherenceAlert>>>(() => {
    if (this.standaloneMode) return {};
    if (!this.showForm()) return {};
    const alerts: Partial<Record<PNLAlertField, PNLCoherenceAlert>> = {};
    const dateAlert = this.buildDateEntretienAlert();
    if (dateAlert) alerts.DATE_ENTRETIEN = dateAlert;
    const motivAlert = this.buildMotivationAlert();
    if (motivAlert) alerts.MOTIVATION = motivAlert;
    const entretienAlert = this.buildEntretienTenuAlert();
    if (entretienAlert) alerts.ENTRETIEN_TENU = entretienAlert;
    return alerts;
  });

  alertsSummary = computed(() => ({ total: Object.values(this.coherenceAlerts()).length }));

  constructor(
    private service: ProcedureNulliteLicenciementService,
    private snackBar: MatSnackBar,
    @Optional() private dashboardRefresh: CaseDashboardRefreshService | null,
  ) {}

  ngOnInit(): void {
    if (this.forceExpanded) this.collapsed.set(false);
    this.procedureChecksSignal.set(this.procedureChecks ?? []);
    this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    this.piecesManquantesSignal.set(this.piecesManquantes ?? []);
    this.prefillFromAi();
    if (this.workspaceCountry === 'FRANCE') {
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
   * Pré-fill IA — V1 : aucun flag procédural dédié n'est extrait par le
   * pipeline (`PREFILL_COUNT_ALWAYS_ZERO`). No-op assumé et documenté ;
   * l'extension IA est une SF ultérieure.
   */
  private prefillFromAi(): void {
    // V1 : aucun champ pré-rempli — parité stricte avec getPrefillCount() = 0.
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  editMode(): void {
    this.showForm.set(true);
  }

  /**
   * Form valide si au moins un champ de procédure est exploitable.
   * Le calcul reste possible avec des dates nulles (le backend détecte
   * l'absence de convocation / entretien via les booléens).
   */
  formValid(): boolean {
    return this.workspaceCountry === 'FRANCE';
  }

  // --- Handlers ---

  onConvocationEnvoyeeChange(value: boolean): void {
    this.convocationEnvoyee.set(value);
    if (!value) this.dateConvocationPresentee.set(null);
  }

  onEntretienTenuChange(value: boolean): void {
    this.entretienTenu.set(value);
    if (!value) this.dateEntretienPrealable.set(null);
  }

  onLettreEcriteChange(value: boolean): void {
    this.lettreLicenciementEcrite.set(value);
    if (!value) {
      this.lettreMotivee.set(false);
      this.motivationSuffisante.set(false);
    }
  }

  onLettreMotiveeChange(value: boolean): void {
    this.lettreMotivee.set(value);
    if (!value) this.motivationSuffisante.set(false);
  }

  onLicenciementCollectifChange(value: boolean): void {
    this.licenciementCollectif.set(value);
    if (!value) this.procedureCseRespectee.set(true);
  }

  onConventionApplicableChange(value: boolean): void {
    this.conventionCollectiveApplicable.set(value);
    if (!value) {
      this.conventionCollectiveRespectee.set(true);
      this.conventionCollectiveCommentaire.set(null);
    }
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request: ProcedureNulliteLicenciementRequest = {
      convocationEnvoyee: this.convocationEnvoyee(),
      dateConvocationPresentee: this.dateConvocationPresentee() || null,
      dateEntretienPrealable: this.dateEntretienPrealable() || null,
      entretienTenu: this.entretienTenu(),
      dateNotificationLicenciement: this.dateNotificationLicenciement() || null,
      lettreLicenciementEcrite: this.lettreLicenciementEcrite(),
      lettreMotivee: this.lettreMotivee(),
      motivationSuffisante: this.motivationSuffisante(),
      motivationCommentaire: this.motivationCommentaire()?.trim() || null,
      licenciementPourMotifGrave: this.licenciementPourMotifGrave(),
      licenciementCollectif: this.licenciementCollectif(),
      procedureCseRespectee: this.licenciementCollectif() ? this.procedureCseRespectee() : null,
      conventionCollectiveApplicable: this.conventionCollectiveApplicable(),
      conventionCollectiveRespectee: this.conventionCollectiveRespectee(),
      conventionCollectiveCommentaire: this.conventionCollectiveCommentaire()?.trim() || null,
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.applyResult(r);
        this.calculating.set(false);
        this.snackBar.open('Analyse de nullité de procédure calculée', 'OK', { duration: 2500 });
        if (!this.standaloneMode) this.dashboardRefresh?.triggerRefresh();
      },
      error: (err) => {
        this.calculating.set(false);
        const msg = err?.error?.message || err?.error || 'Erreur lors du calcul';
        this.snackBar.open(String(msg), 'Fermer', { duration: 5000, panelClass: 'snack-error' });
      },
    });
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.applyResult(r);
        this.loading.set(false);
      },
      error: () => {
        // 404 attendu si aucune analyse — on reste en mode formulaire.
        this.loading.set(false);
      },
    });
  }

  private applyResult(r: ProcedureNulliteLicenciementResponse): void {
    this.result.set(r);
    this.showForm.set(false);
  }

  // ---------------------------------------------------------------------------
  // Builders d'alertes F-IA-03 (un par field croisable)
  // ---------------------------------------------------------------------------

  /** Date d'entretien préalable — divergence stricte F-96 / question IA. */
  private buildDateEntretienAlert(): PNLCoherenceAlert | null {
    const user = this.dateEntretienPrealable();
    if (!user) return null;
    const builder = CoherenceAlertBuilder.forField<PNLAlertField>('DATE_ENTRETIEN');
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== F96_CODE_DATE_ENTRETIEN) continue;
      const ev = chk.expectedValue?.trim();
      if (!ev || ev === user) continue;
      builder.addSource('F96', {
        expectedDisplay: ev,
        reason: `Checklist procédurale : entretien préalable attendu le ${ev}${chk.raison ? ' (' + chk.raison + ')' : ''}`,
      });
      break;
    }
    for (const q of this.aiQuestionsSignal()) {
      if (q.critereCode?.toUpperCase() !== F96_CODE_DATE_ENTRETIEN) continue;
      const ev = q.expectedValue?.trim();
      if (!ev || ev === user) continue;
      builder.addSource('QUESTION_IA', {
        expectedDisplay: ev,
        reason: `Question complémentaire : "${q.questionText}" → "${q.answerText}"`,
      });
      break;
    }
    const piece = this.findPieceManquante(['DT36_DATE_ENTRETIEN', 'CONVOCATION_ENTRETIEN', 'COMPTE_RENDU_ENTRETIEN']);
    if (piece) builder.addPieceManquante(piece);
    return builder.build();
  }

  /** Motivation suffisante — divergence F-96 / question IA. */
  private buildMotivationAlert(): PNLCoherenceAlert | null {
    const builder = CoherenceAlertBuilder.forField<PNLAlertField>('MOTIVATION');
    const userDisplay = this.motivationSuffisante() ? 'Motivation suffisante' : 'Motivation insuffisante';
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== F96_CODE_MOTIVATION) continue;
      const expected = this.parseBoolean(chk.expectedValue);
      if (expected === null || expected === this.motivationSuffisante()) continue;
      builder.addSource('F96', {
        expectedDisplay: expected ? 'Motivation suffisante' : 'Motivation insuffisante',
        reason: `Checklist procédurale : motivation jugée ${expected ? 'suffisante' : 'insuffisante'}${chk.raison ? ' (' + chk.raison + ')' : ''} — saisie : ${userDisplay.toLowerCase()}`,
      });
      break;
    }
    for (const q of this.aiQuestionsSignal()) {
      if (q.critereCode?.toUpperCase() !== F96_CODE_MOTIVATION) continue;
      const expected = this.parseBoolean(q.expectedValue);
      if (expected === null || expected === this.motivationSuffisante()) continue;
      builder.addSource('QUESTION_IA', {
        expectedDisplay: expected ? 'Motivation suffisante' : 'Motivation insuffisante',
        reason: `Question complémentaire : "${q.questionText}" → "${q.answerText}"`,
      });
      break;
    }
    const piece = this.findPieceManquante(['DT36_MOTIVATION', 'LETTRE_LICENCIEMENT']);
    if (piece) builder.addPieceManquante(piece);
    return builder.build();
  }

  /** Entretien préalable tenu — divergence F-96 / question IA. */
  private buildEntretienTenuAlert(): PNLCoherenceAlert | null {
    const builder = CoherenceAlertBuilder.forField<PNLAlertField>('ENTRETIEN_TENU');
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== F96_CODE_ENTRETIEN_TENU) continue;
      const expected = this.parseBoolean(chk.expectedValue);
      if (expected === null || expected === this.entretienTenu()) continue;
      builder.addSource('F96', {
        expectedDisplay: expected ? 'Entretien tenu' : 'Entretien non tenu',
        reason: `Checklist procédurale : entretien préalable ${expected ? 'tenu' : 'non tenu'}${chk.raison ? ' (' + chk.raison + ')' : ''}`,
      });
      break;
    }
    for (const q of this.aiQuestionsSignal()) {
      if (q.critereCode?.toUpperCase() !== F96_CODE_ENTRETIEN_TENU) continue;
      const expected = this.parseBoolean(q.expectedValue);
      if (expected === null || expected === this.entretienTenu()) continue;
      builder.addSource('QUESTION_IA', {
        expectedDisplay: expected ? 'Entretien tenu' : 'Entretien non tenu',
        reason: `Question complémentaire : "${q.questionText}" → "${q.answerText}"`,
      });
      break;
    }
    const piece = this.findPieceManquante(['DT36_ENTRETIEN_TENU', 'COMPTE_RENDU_ENTRETIEN']);
    if (piece) builder.addPieceManquante(piece);
    return builder.build();
  }

  /** Parse une `expectedValue` textuelle en booléen, ou null si non mappable. */
  private parseBoolean(raw: string | null | undefined): boolean | null {
    if (!raw) return null;
    const v = raw.trim().toLowerCase();
    if (v === 'true' || v === 'oui' || v === '1') return true;
    if (v === 'false' || v === 'non' || v === '0') return false;
    return null;
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

  alertTooltip(alert: PNLCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: PNLCoherenceAlert): string {
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

  /** Rouge réservé à NULLITE_AVEREE ; or pour PROBABLE ; navy pour REGULIERE. */
  verdictBannerClass(verdict: ProcedureNulliteVerdict): string {
    switch (verdict) {
      case 'NULLITE_AVEREE':
        return 'pnl-verdict-banner pnl-verdict-banner--danger';
      case 'NULLITE_PROBABLE':
        return 'pnl-verdict-banner pnl-verdict-banner--medium';
      case 'PROCEDURE_REGULIERE':
        return 'pnl-verdict-banner pnl-verdict-banner--available';
    }
  }

  verdictBannerLabel(verdict: ProcedureNulliteVerdict): string {
    switch (verdict) {
      case 'NULLITE_AVEREE': return 'Nullité de procédure avérée';
      case 'NULLITE_PROBABLE': return 'Nullité de procédure probable';
      case 'PROCEDURE_REGULIERE': return 'Procédure régulière';
    }
  }

  verdictBannerIcon(verdict: ProcedureNulliteVerdict): string {
    switch (verdict) {
      case 'NULLITE_AVEREE': return 'error';
      case 'NULLITE_PROBABLE': return 'warning';
      case 'PROCEDURE_REGULIERE': return 'check_circle';
    }
  }

  /** Classe CSS du chip gravité d'un vice. */
  viceGraviteClass(gravite: 'AVERE' | 'PROBABLE'): string {
    return gravite === 'AVERE' ? 'pnl-vice-gravite pnl-vice-gravite--avere'
      : 'pnl-vice-gravite pnl-vice-gravite--probable';
  }

  viceGraviteLabel(gravite: 'AVERE' | 'PROBABLE'): string {
    return gravite === 'AVERE' ? 'Avéré' : 'Probable';
  }
}
