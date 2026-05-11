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
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { RecompensesService } from '../../core/services/recompenses.service';
import {
  NATURE_BIEN_OPTIONS,
  NatureBienRecompense,
  OperationRecompense,
  REGIME_OPTIONS,
  RecompensesRequest,
  RecompensesResponse,
  RegimeMatrimonialRecompenses,
  TYPE_OPERATION_OPTIONS,
  TypeOperationRecompense,
} from '../../core/models/recompenses.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import { PieceManquanteEntry } from '../../core/models/case-analysis.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { AiQuestion } from '../../core/models/ai-question.model';
import { CoherencePopoverTriggerDirective } from '../../shared/coherence-popover/coherence-popover-trigger.directive';
import {
  CoherenceAlert,
  CoherenceAlertSource,
} from '../../shared/coherence-popover/coherence-alert.model';
import { CoherenceAlertBuilder } from '../../shared/coherence-popover/coherence-alert-builder';
import {
  RecompensesPrefillRules,
  VALID_REGIMES,
} from './recompenses-section-prefill-rules';

/**
 * SF-FA-15-02 : champs F-IA-03 audités par l'outil récompenses.
 * - `REGIME_MATRIMONIAL` : divergence entre IA / F-96 / QUESTION_IA et la
 *   sélection avocat sur le régime matrimonial.
 * - `REGIME_APPLICABILITE` : alerte structurelle quand l'IA a détecté un
 *   régime exclu de la logique récompenses (`SEPARATION_BIENS`) — l'avocat
 *   doit savoir que l'outil pourrait ne pas s'appliquer à ce dossier.
 */
export type RecompensesAlertField = 'REGIME_MATRIMONIAL' | 'REGIME_APPLICABILITE';

// SF-155-05 : alias rétro-compat — utilise l'interface partagée.
export type RecompensesAlertSource = CoherenceAlertSource;
export type RecompensesCoherenceAlert = CoherenceAlert<RecompensesAlertField>;

/**
 * Outil décisionnel Récompenses (art. 1437 et 1469 Cciv) — single-country FR.
 *
 * Pattern de référence : `harcelement-licenciement-nul-section` (template
 * canonique, F-DT-11-02) pour le pré-fill IA + validation F-IA-03 ; et
 * `partage-immobilier-section` (F-FA-05) pour la palette navy/or famille.
 *
 * Affiché conditionnellement par le panel F-IA-04 (tool_id `F-FA-15-recompenses`).
 */
@Component({
  selector: 'app-recompenses-section',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    DecimalPipe,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './recompenses-section.component.html',
  styleUrl: './recompenses-section.component.scss',
})
export class RecompensesSectionComponent implements OnInit, OnChanges {
  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'RÉCOMPENSES (ART. 1437/1469 CCIV)';
  static readonly TOOL_ICON = 'account_balance';

  /**
   * F-236 SF-236-02 — Pattern miroir SF-177-12. Compte les champs que
   * `prefillFromAi()` runtime poserait, sans instancier le composant.
   *
   * Délègue à `RecompensesPrefillRules.computePrefillCount` (helper partagé
   * consommé aussi par le runtime) — divergence runtime/static impossible
   * par construction.
   */
  static getPrefillCount(input: {
    aiData?: FamilleExtractedData | null;
    procedureChecks?: unknown[];
    aiQuestions?: unknown[];
    piecesManquantes?: unknown[];
    triggerEvents?: unknown[];
    workspaceCountry?: string;
  }): number {
    return RecompensesPrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';

  // SF-FA-15-02 : inputs IA (tous optionnels — null-safe partout, no-op gracieux).
  @Input() aiData?: FamilleExtractedData | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;

  // Snapshots signal — réactivité des `computed` signals.
  private aiDataSignal = signal<FamilleExtractedData | null | undefined>(undefined);
  private procedureChecksSignal = signal<ProcedureCheck[]>([]);
  private aiQuestionsSignal = signal<AiQuestion[]>([]);
  private piecesManquantesSignal = signal<PieceManquanteEntry[]>([]);

  // États UI.
  // F-177 SF-177-03b : force l'expansion (mode modal F-177).
  @Input() forceExpanded = false;


  /**
   * F-163 SF-163-02c — Mode simulateur autonome (hors dossier client).
   *
   * Quand `true` :
   *  - Bannière « 🧪 Mode simulateur » affichée en haut.
   *  - `prefillFromAi()` court-circuité.
   *  - `coherenceAlerts` retourne `{}`.
   *  - `loadExisting()` court-circuité.
   *  - `calculate()` POSTe sur `/api/v1/simulators/F-FA-15-recompenses/calculate`.
   *  - `triggerRefresh()` jamais invoqué.
   *
   * Default `false` — mode case-file scoped inchangé.
   */
  @Input() standaloneMode: boolean = false;
  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<RecompensesResponse | null>(null);

  // Form fields.
  regimeMatrimonial = signal<RegimeMatrimonialRecompenses | null>(null);
  operations = signal<OperationRecompense[]>([]);
  private nextOperationSeq = 1;

  // SF-FA-15-02 : provenance IA — un signal par champ pré-remplissable.
  // `regime` est le seul champ pré-fillable (les opérations sont saisies avocat).
  provenanceRegime = signal<'IA' | null>(null);

  // Options statiques exposées au template.
  readonly regimeOptions = REGIME_OPTIONS;
  readonly typeOperationOptions = TYPE_OPERATION_OPTIONS;
  readonly natureBienOptions = NATURE_BIEN_OPTIONS;

  /**
   * SF-FA-15-02 : alertes F-IA-03 — gate `showForm()` strict (anti-bug
   * SF-IA-03-12 : pas d'alertes affichées après calcul).
   */
  coherenceAlerts = computed<Partial<Record<RecompensesAlertField, RecompensesCoherenceAlert>>>(() => {
    // F-163 SF-163-02c : aucune source IA en standalone.
    if (this.standaloneMode) return {};
    if (!this.showForm()) return {};
    const alerts: Partial<Record<RecompensesAlertField, RecompensesCoherenceAlert>> = {};
    const regimeAlert = this.buildRegimeAlert();
    if (regimeAlert) alerts.REGIME_MATRIMONIAL = regimeAlert;
    const applicabiliteAlert = this.buildApplicabiliteAlert();
    if (applicabiliteAlert) alerts.REGIME_APPLICABILITE = applicabiliteAlert;
    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    return { total: values.length, blockers: 0 };
  });

  constructor(
    private service: RecompensesService,
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

    if (this.standaloneMode) {
      // F-163 SF-163-02c : pas de dossier à interroger en standalone.
      this.collapsed.set(false);
      this.loading.set(false);
      this.showForm.set(true);
      return;
    }
    if (this.workspaceCountry === 'FRANCE') {
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

    // Re-prefill quand `aiData` arrive après mount (cas analyse IA terminée
    // après ouverture de l'outil), tant que l'avocat n'a pas saisi et qu'aucun
    // résultat n'est affiché.
    if (changes['aiData'] && !changes['aiData'].firstChange && this.showForm() && !this.result() && !this.standaloneMode) {
      this.prefillFromAi();
    }
  }

  /**
   * SF-FA-15-02 / F-236 SF-236-02 : pré-remplit `regimeMatrimonial` depuis `aiData`.
   * N'écrase pas une saisie avocat (provenance === null indique modification manuelle).
   * No-op gracieux si `aiData` absent ou si la valeur IA est `SEPARATION_BIENS`
   * (régime exclu — pas de récompenses).
   *
   * Délègue le calcul à `RecompensesPrefillRules` (helper partagé runtime/static).
   */
  private prefillFromAi(): void {
    const normalized = RecompensesPrefillRules.computeRegimeMatrimonial({
      aiData: this.aiDataSignal(),
    });
    if (normalized === null) return;
    if (this.regimeMatrimonial() !== null && this.provenanceRegime() !== 'IA') return;
    this.regimeMatrimonial.set(normalized);
    this.provenanceRegime.set('IA');
  }

  /**
   * SF-FA-15-02 : alerte cohérence sur le régime — divergence entre l'IA
   * et la sélection avocat. Sources scannées : F96 (procedureCheck
   * `FA15_REGIME_MATRIMONIAL`), QUESTION_IA, IA (`aiData.regimeMatrimonialDetecte`).
   */
  private buildRegimeAlert(): RecompensesCoherenceAlert | null {
    const userRegime = this.regimeMatrimonial();
    if (!userRegime) return null;

    const builder = CoherenceAlertBuilder.forField<RecompensesAlertField>('REGIME_MATRIMONIAL');

    // 1. F-96 VERIFIED — `FA15_REGIME_MATRIMONIAL` — première source qui fixe canonique.
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'FA15_REGIME_MATRIMONIAL') continue;
      if (chk.statut !== 'VERIFIED') continue;
      const expected = chk.expectedValue?.toUpperCase();
      if (!expected || expected === userRegime) continue;
      const label = this.regimeLabel(expected);
      builder.addSource('F96', {
        expectedDisplay: label,
        reason: `Checklist procédurale : régime attendu ${label}${chk.raison ? ' (' + chk.raison + ')' : ''}`,
      });
      break;
    }

    // 2. QUESTION_IA — réponse "oui" sur `FA15_REGIME_MATRIMONIAL`.
    for (const q of this.aiQuestionsSignal()) {
      if (q.critereCode?.toUpperCase() !== 'FA15_REGIME_MATRIMONIAL') continue;
      const answer = q.answerText?.trim().toLowerCase();
      if (!answer) continue;
      const isOui = answer === 'oui' || answer.startsWith('oui ')
        || answer.startsWith('oui,') || answer.startsWith('oui.');
      if (!isOui) continue;
      const expected = q.expectedValue?.toUpperCase();
      if (!expected || expected === userRegime) continue;
      const label = this.regimeLabel(expected);
      builder.addSource('QUESTION_IA', {
        expectedDisplay: label,
        reason: `Question complémentaire : "${q.questionText}" → "${q.answerText}"`,
      });
      break;
    }

    // 3. IA — `aiData.regimeMatrimonialDetecte`.
    const iaRegime = this.aiDataSignal()?.regimeMatrimonialDetecte?.toUpperCase();
    if (iaRegime && iaRegime !== userRegime) {
      const label = this.regimeLabel(iaRegime);
      builder.addSource('IA', {
        expectedDisplay: label,
        reason: `Analyse du dossier : régime matrimonial détecté "${label}"`,
      });
    }

    // 4. PIECE_MANQUANTE — contributor (contrat de mariage manquant).
    const piece = this.findPieceManquante(['FA15_REGIME_MATRIMONIAL', 'FA15_CONTRAT_MARIAGE']);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  /**
   * SF-FA-15-02 : alerte structurelle si l'IA a détecté `SEPARATION_BIENS`
   * (régime exclu — pas de récompenses au sens de 1437/1469). N'apparaît que
   * si l'avocat a néanmoins sélectionné un régime communautaire — divergence
   * forte qui doit être visible avant le calcul.
   */
  private buildApplicabiliteAlert(): RecompensesCoherenceAlert | null {
    const ai = this.aiDataSignal();
    if (!ai) return null;
    const detected = ai.regimeMatrimonialDetecte?.toUpperCase();
    if (detected !== 'SEPARATION_BIENS') return null;
    const userRegime = this.regimeMatrimonial();
    if (!userRegime) return null; // pas encore de saisie → pas d'alerte
    const builder = CoherenceAlertBuilder.forField<RecompensesAlertField>('REGIME_APPLICABILITE');
    builder.addSource('IA', {
      expectedDisplay: 'Séparation de biens',
      reason: 'Analyse du dossier : régime de séparation de biens détecté — '
        + 'récompenses non applicables (art. 1536 Cciv, créances entre patrimoines hors scope)',
    });
    return builder.build();
  }

  private regimeLabel(code: string): string {
    return REGIME_OPTIONS.find(o => o.code === code)?.label ?? code;
  }

  private findPieceManquante(acceptedCodes: string[]): string | null {
    const norm = new Set(acceptedCodes.map(c => c.toUpperCase()));
    for (const p of this.piecesManquantesSignal()) {
      const code = p.critereCode?.toUpperCase();
      if (code && norm.has(code)) return p.texte;
    }
    return null;
  }

  alertTooltip(alert: RecompensesCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: RecompensesCoherenceAlert): string {
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

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  // ---------------------------------------------------------------------------
  // Form actions
  // ---------------------------------------------------------------------------

  formValid(): boolean {
    return this.regimeMatrimonial() !== null;
  }

  /**
   * Ajoute une nouvelle opération vide à la liste avec id auto-généré.
   */
  addOperation(): void {
    const id = `op-${this.nextOperationSeq++}`;
    const op: OperationRecompense = {
      id,
      type: 'DEPENSE_PROPRE_AU_PROFIT_COMMUNAUTE',
      natureBien: 'AUTRE',
      montantDepenseEur: 0,
      valeurInitialeBienEur: null,
      valeurActuelleBienEur: null,
      description: null,
    };
    this.operations.update(list => [...list, op]);
  }

  removeOperation(id: string): void {
    this.operations.update(list => list.filter(o => o.id !== id));
  }

  trackByOperationId(_index: number, op: OperationRecompense): string {
    return op.id;
  }

  /**
   * SF-FA-15-02 : handlers manuels — effacent le badge IA correspondant.
   */
  onRegimeChange(value: RegimeMatrimonialRecompenses | null): void {
    this.regimeMatrimonial.set(value);
    this.provenanceRegime.set(null);
  }

  onOperationTypeChange(id: string, value: TypeOperationRecompense): void {
    this.operations.update(list => list.map(o => o.id === id ? { ...o, type: value } : o));
  }

  onOperationNatureChange(id: string, value: NatureBienRecompense): void {
    this.operations.update(list => list.map(o => o.id === id ? { ...o, natureBien: value } : o));
  }

  onOperationFieldChange(
    id: string,
    field: 'montantDepenseEur' | 'valeurInitialeBienEur' | 'valeurActuelleBienEur',
    value: number | null,
  ): void {
    this.operations.update(list => list.map(o => {
      if (o.id !== id) return o;
      return { ...o, [field]: value };
    }));
  }

  onOperationDescriptionChange(id: string, value: string): void {
    this.operations.update(list => list.map(o => o.id === id ? { ...o, description: value || null } : o));
  }

  // ---------------------------------------------------------------------------
  // HTTP
  // ---------------------------------------------------------------------------

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: r => {
        this.result.set(r);
        // Restaure le régime sélectionné depuis le résultat persisté.
        if (VALID_REGIMES.has(r.regimeMatrimonial)) {
          this.regimeMatrimonial.set(r.regimeMatrimonial);
        }
        // SF-FA-15-02 : valeurs persistées = saisie avocat — pas de badge IA.
        this.provenanceRegime.set(null);
        this.showForm.set(false);
        this.loading.set(false);
      },
      error: () => {
        // 404 attendu si aucune analyse — reste en mode formulaire.
        // Fallback pré-fill IA uniquement ici (pas après GET 200).
        this.prefillFromAi();
        this.loading.set(false);
      },
    });
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request: RecompensesRequest = {
      regimeMatrimonial: this.regimeMatrimonial()!,
      operations: this.operations(),
    };
    this.calculating.set(true);
    // F-163 SF-163-02c : en standalone, POST sur le dispatcher générique.

    const request$ = this.standaloneMode

      ? this.service.calculateStandalone(request)

      : this.service.calculate(this.caseFileId, request);

    request$.subscribe({
      next: r => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Récompenses calculées', 'OK', { duration: 2500 });
        // F-163 SF-163-02c : pas de dashboard à rafraîchir en standalone.

        if (!this.standaloneMode) { this.dashboardRefresh?.triggerRefresh(); }
      },
      error: err => {
        this.calculating.set(false);
        const msg = err?.error?.message || err?.error || 'Erreur lors du calcul';
        this.snackBar.open(String(msg), 'Fermer', { duration: 5000, panelClass: 'snack-error' });
      },
    });
  }

  editMode(): void {
    this.showForm.set(true);
  }
}
