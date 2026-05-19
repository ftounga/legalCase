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
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import {
  PieceManquanteEntry,
  TravailExtractedData,
} from '../../core/models/case-analysis.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { AiQuestion } from '../../core/models/ai-question.model';
import { CoherencePopoverTriggerDirective } from '../../shared/coherence-popover/coherence-popover-trigger.directive';
import {
  CoherenceAlert,
  CoherenceAlertSource,
} from '../../shared/coherence-popover/coherence-alert.model';
import { CoherenceAlertBuilder } from '../../shared/coherence-popover/coherence-alert-builder';
import { SourceExplanation } from '../../core/models/source-explanation.model';
import { SourceExplanationService } from '../../core/services/source-explanation.service';
import { PrescriptionBeLitigeTravailService } from '../../core/services/prescription-be-litige-travail.service';
import {
  PrescriptionBeLitigeTravailRequest,
  PrescriptionBeLitigeTravailResponse,
  TYPES_CREANCE,
  TypeCreance,
} from '../../core/models/prescription-be-litige-travail.model';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { PrescriptionBeLitigeTravailPrefillRules } from './prescription-be-litige-travail-section-prefill-rules';

/**
 * SF-207-01b — Champs d'alerte de cohérence F-IA-03 exposés par l'outil
 * F-207 (prescription Travail BE).
 */
export type PrescriptionBeAlertField = 'DATE_RUPTURE' | 'TYPE_CREANCE';

// Alias rétro-compat pour usage interne template / tests.
export type PrescriptionBeAlertSource = CoherenceAlertSource;
export type PrescriptionBeCoherenceAlert = CoherenceAlert<PrescriptionBeAlertField>;

/**
 * Seuil d'écart en jours absolu au-delà duquel on affiche une alerte de
 * cohérence entre la date IA et la saisie avocat (aligné canonique
 * F-DT-25 `indemnite-preavis-section`).
 */
const DATE_RUPTURE_DIVERGENCE_DAYS = 15;

/**
 * SF-207-01b : outil décisionnel "Prescription d'une action de droit du
 * travail belge" (F-207). BE uniquement (Loi 03/07/1978 art. 15 + CCT 109
 * art. 11). Consomme l'API SF-207-01 (PR #1119).
 *
 * Affiché conditionnellement par le panel F-IA-04 (tool_id
 * `prescription-be-litige-travail`, règle ALWAYS_ON BE + travail).
 *
 * Pattern canonique : `immigration-title-decision-section` (F-IA-04) +
 * `indemnite-preavis-section` (F-DT-25) pour la mécanique date + pré-fill IA.
 * Pré-fill IA sur 2 champs (dateRupture / typeCreance via mapping motifRupture)
 * + alertes cohérence F-IA-03 via `CoherenceAlertBuilder`.
 */
@Component({
  selector: 'app-prescription-be-litige-travail-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatProgressSpinnerModule,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './prescription-be-litige-travail-section.component.html',
  styleUrl: './prescription-be-litige-travail-section.component.scss',
})
export class PrescriptionBeLitigeTravailSectionComponent implements OnInit, OnChanges {
  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'PRESCRIPTION — LITIGE TRAVAIL (BE)';
  static readonly TOOL_ICON = 'schedule';

  /** F-177 SF-177-12 / F-236 SF-236-02 — délègue au helper partagé (parité runtime). */
  static getPrefillCount(input: PrefillCountInput): number {
    return PrescriptionBeLitigeTravailPrefillRules.computePrefillCount(input);
  }

  readonly typeCreanceOptions = TYPES_CREANCE;

  @Input() caseFileId!: string;
  // F-177 SF-177-03b : force l'expansion (mode modal F-177).
  @Input() forceExpanded = false;
  /** Gate strict BE — l'outil n'a pas d'équivalent FR. */
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'BELGIQUE';
  @Input() aiData?: TravailExtractedData | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;

  // Snapshots signal des inputs IA pour réactivité computed.
  private aiDataSignal = signal<TravailExtractedData | null | undefined>(undefined);
  private procedureChecksSignal = signal<ProcedureCheck[]>([]);
  private aiQuestionsSignal = signal<AiQuestion[]>([]);
  private piecesManquantesSignal = signal<PieceManquanteEntry[]>([]);

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<PrescriptionBeLitigeTravailResponse | null>(null);

  // Champs du form (signals — édités via handlers).
  dateRupture = signal<string | null>(null);
  typeCreance = signal<TypeCreance | null>(null);
  dateActionEnvisagee = signal<string | null>(null);

  // Provenance IA par champ pré-rempli.
  provenanceDateRupture = signal<'IA' | null>(null);
  provenanceTypeCreance = signal<'IA' | null>(null);

  // SF-IA-03-15 : map {sourceKey → explanations} (popover enrichi).
  sourceExplanations = signal<Map<string, SourceExplanation[]>>(new Map());

  /** Gate strict workspace BE (l'outil n'existe pas côté FR). */
  isAvailable = computed(() => this.workspaceCountry === 'BELGIQUE');

  /**
   * Alertes de cohérence calculées dynamiquement (2 fields).
   * Gate : uniquement en mode formulaire (pattern anti-bug SF-IA-03-12).
   */
  coherenceAlerts = computed<Partial<Record<PrescriptionBeAlertField, PrescriptionBeCoherenceAlert>>>(() => {
    if (!this.showForm()) return {};
    const alerts: Partial<Record<PrescriptionBeAlertField, PrescriptionBeCoherenceAlert>> = {};
    const dateAlert = this.buildDateRuptureAlert();
    if (dateAlert) alerts.DATE_RUPTURE = dateAlert;
    const typeAlert = this.buildTypeCreanceAlert();
    if (typeAlert) alerts.TYPE_CREANCE = typeAlert;
    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    return { total: values.length, blockers: 0 };
  });

  constructor(
    private service: PrescriptionBeLitigeTravailService,
    private snackBar: MatSnackBar,
    @Optional() private dashboardRefresh: CaseDashboardRefreshService | null,
    @Optional() private sourceExplanationService: SourceExplanationService | null,
  ) {}

  ngOnInit(): void {
    if (this.forceExpanded) this.collapsed.set(false);

    this.aiDataSignal.set(this.aiData);
    this.procedureChecksSignal.set(this.procedureChecks ?? []);
    this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    this.piecesManquantesSignal.set(this.piecesManquantes ?? []);

    if (this.workspaceCountry === 'BELGIQUE') {
      this.load();
      this.loadSourceExplanations();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['forceExpanded'] && this.forceExpanded) this.collapsed.set(false);

    if (changes['aiData']) this.aiDataSignal.set(this.aiData);
    if (changes['procedureChecks']) this.procedureChecksSignal.set(this.procedureChecks ?? []);
    if (changes['aiQuestions']) this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    if (changes['piecesManquantes']) this.piecesManquantesSignal.set(this.piecesManquantes ?? []);

    // Ré-applique le pré-fill si aiData change avant première résolution.
    if (changes['aiData'] && !changes['aiData'].firstChange
        && this.showForm() && !this.result()
        && this.workspaceCountry === 'BELGIQUE') {
      this.prefillFromAi();
    }
  }

  /**
   * SF-207-01b — Pré-remplit le form depuis `aiData`. Délègue intégralement
   * au helper pur partagé pour garantir la parité runtime / static
   * `getPrefillCount` (divergence impossible par construction).
   */
  private prefillFromAi(): void {
    const ai = this.aiDataSignal();
    if (!ai) return;
    if (this.workspaceCountry !== 'BELGIQUE') return;

    const ruleInput: PrefillCountInput = { aiData: ai, workspaceCountry: this.workspaceCountry };

    const date = PrescriptionBeLitigeTravailPrefillRules.computeDateRupture(ruleInput);
    if (date !== null) {
      if (this.dateRupture() === null || this.provenanceDateRupture() === 'IA') {
        this.dateRupture.set(date);
        this.provenanceDateRupture.set('IA');
      }
    }

    const type = PrescriptionBeLitigeTravailPrefillRules.computeTypeCreance(ruleInput);
    if (type !== null) {
      if (this.typeCreance() === null || this.provenanceTypeCreance() === 'IA') {
        this.typeCreance.set(type);
        this.provenanceTypeCreance.set('IA');
      }
    }
  }

  private loadSourceExplanations(): void {
    if (!this.caseFileId || !this.sourceExplanationService) return;
    this.sourceExplanationService.getForCaseFile(this.caseFileId).subscribe({
      next: (map) => this.sourceExplanations.set(map),
      error: () => { /* fail-open */ },
    });
  }

  /** SF-IA-03-15 : mapping field → sourceKey pour le popover enrichi. */
  explanationFor(field: PrescriptionBeAlertField): SourceExplanation[] {
    const key = (() => {
      switch (field) {
        case 'DATE_RUPTURE': return 'date_rupture_contrat';
        case 'TYPE_CREANCE': return 'motif_rupture';
      }
    })();
    return this.sourceExplanations().get(key) ?? [];
  }

  alertTooltip(alert: PrescriptionBeCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: PrescriptionBeCoherenceAlert): string {
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

  /**
   * Divergence date de rupture — écart absolu ≥ 15 jours entre IA et saisie.
   */
  private buildDateRuptureAlert(): PrescriptionBeCoherenceAlert | null {
    const aiDate = this.aiDataSignal()?.dateRuptureContrat;
    const userDate = this.dateRupture();
    if (!aiDate || !userDate) return null;
    const diff = dateDaysDiff(aiDate, userDate);
    if (diff === null || diff < DATE_RUPTURE_DIVERGENCE_DAYS) return null;
    return CoherenceAlertBuilder.forField<PrescriptionBeAlertField>('DATE_RUPTURE')
      .addSource('IA', {
        expectedDisplay: aiDate,
        reason: `Analyse du dossier : date de rupture du contrat ${aiDate}`,
      })
      .build();
  }

  /**
   * Divergence type de créance — l'IA déduit `EX_CONTRAT_GENERAL` depuis un
   * motif mappable mais l'avocat a choisi un autre code (probable cas
   * `PENDANT_CONTRAT` / `ARRIERES_SALAIRE` où l'IA n'a pas le contexte temporel).
   */
  private buildTypeCreanceAlert(): PrescriptionBeCoherenceAlert | null {
    const ai = this.aiDataSignal();
    if (!ai) return null;
    const iaType = PrescriptionBeLitigeTravailPrefillRules.computeTypeCreance({ aiData: ai });
    const userType = this.typeCreance();
    if (!iaType || !userType) return null;
    if (iaType === userType) return null;
    return CoherenceAlertBuilder.forField<PrescriptionBeAlertField>('TYPE_CREANCE')
      .addSource('IA', {
        expectedDisplay: iaType,
        reason: `Analyse du dossier : motif "${ai.motifRupture}" → type ${iaType}`,
      })
      .build();
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  formValid(): boolean {
    const dr = this.dateRupture();
    const tc = this.typeCreance();
    return !!dr && !!tc;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // -- Handlers --
  onDateRuptureChange(value: string | null): void {
    this.dateRupture.set(value);
    this.provenanceDateRupture.set(null);
  }

  onTypeCreanceChange(value: TypeCreance | null): void {
    this.typeCreance.set(value);
    this.provenanceTypeCreance.set(null);
  }

  onDateActionEnvisageeChange(value: string | null): void {
    this.dateActionEnvisagee.set(value);
  }

  /** Classe CSS du verdict (verte / ambre / rouge). */
  verdictClass(verdict: PrescriptionBeLitigeTravailResponse['verdict']): string {
    switch (verdict) {
      case 'NON_PRESCRIT': return 'verdict-ok';
      case 'IMMINENT': return 'verdict-warn';
      case 'PRESCRIT': return 'verdict-critical';
    }
  }

  verdictIcon(verdict: PrescriptionBeLitigeTravailResponse['verdict']): string {
    switch (verdict) {
      case 'NON_PRESCRIT': return 'check_circle';
      case 'IMMINENT': return 'warning';
      case 'PRESCRIT': return 'error';
    }
  }

  verdictLabel(verdict: PrescriptionBeLitigeTravailResponse['verdict']): string {
    switch (verdict) {
      case 'NON_PRESCRIT': return 'Action non prescrite';
      case 'IMMINENT': return 'Prescription imminente';
      case 'PRESCRIT': return 'Action prescrite';
    }
  }

  calculate(): void {
    if (!this.formValid()) return;
    if (this.workspaceCountry !== 'BELGIQUE') {
      this.snackBar.open('Outil indisponible pour ce workspace', 'Fermer',
        { duration: 4000, panelClass: 'snack-error' });
      return;
    }
    const request: PrescriptionBeLitigeTravailRequest = {
      dateRupture: this.dateRupture()!,
      typeCreance: this.typeCreance()!,
      dateActionEnvisagee: this.dateActionEnvisagee() || undefined,
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        // Refresh dashboard décisionnel (pattern SF-IA-02-03).
        this.dashboardRefresh?.triggerRefresh();
      },
      error: (err) => {
        this.calculating.set(false);
        const status = err?.status;
        let msg: string;
        if (status === 404) {
          msg = 'Dossier introuvable';
        } else if (status === 400) {
          msg = err?.error?.message || err?.error || 'Données saisies invalides';
        } else {
          msg = 'Une erreur est survenue. Veuillez réessayer.';
        }
        this.snackBar.open(String(msg), 'Fermer',
          { duration: 5000, panelClass: 'snack-error' });
      },
    });
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.dateRupture.set(r.dateRupture);
        this.typeCreance.set(r.typeCreance);
        this.dateActionEnvisagee.set(r.dateActionEnvisagee);
        // Valeurs persistées = saisie avocat — jamais de badge IA.
        this.provenanceDateRupture.set(null);
        this.provenanceTypeCreance.set(null);
        this.showForm.set(false);
        this.loading.set(false);
      },
      error: () => {
        // 404 attendu si aucune analyse — mode formulaire + pré-fill IA.
        this.prefillFromAi();
        this.loading.set(false);
      },
    });
  }
}

/**
 * Utilitaire : différence absolue en jours entre 2 dates ISO. Renvoie null
 * si l'une des dates n'est pas parsable.
 */
function dateDaysDiff(a: string, b: string): number | null {
  const ta = Date.parse(a);
  const tb = Date.parse(b);
  if (Number.isNaN(ta) || Number.isNaN(tb)) return null;
  return Math.abs(ta - tb) / 86400000;
}
