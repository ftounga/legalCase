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
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SeparationCorpsService } from '../../core/services/separation-corps.service';
import {
  MODES_PROCEDURE_SEP_FR,
  ModeProcedureSep,
  SeparationCorpsRequest,
  SeparationCorpsResponse,
  VerdictConversion,
  modeProcedureSepLabel,
  verdictConversionLabel,
} from '../../core/models/separation-corps.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { PieceManquanteEntry } from '../../core/models/case-analysis.model';
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
import { SeparationCorpsPrefillRules } from './separation-corps-section-prefill-rules';

/**
 * SF-FA-21-02 : champs F-IA-03 audités par l'outil "Séparation de corps +
 * conversion divorce" (F-FA-21, art. 296+306 Cciv). Un par champ
 * pré-remplissable depuis l'IA (`FamilleExtractedData`).
 */
export type SeparationCorpsAlertField =
  | 'DATE_JUGEMENT_SEPARATION'
  | 'PATRIMOINE_COMMUN';

export type SeparationCorpsAlertSource = CoherenceAlertSource;
export type SeparationCorpsCoherenceAlert =
    CoherenceAlert<SeparationCorpsAlertField>;

const ISO_DATE_REGEX = /^\d{4}-\d{2}-\d{2}$/;

/**
 * SF-FA-21-02 : composant Angular standalone pour l'outil "Séparation de
 * corps + conversion divorce" (F-FA-21, art. 296+306 Cciv, FRANCE uniquement).
 *
 * Pattern de référence : `pacs-dissolution-section` (template canonique
 * 2026-04-25 — datepickers natifs nullable + mat-select + provenance signals)
 * + `divorce-accepte-section` (template canonique — 5 provenance signals +
 * helpers verdict).
 *
 * - Form : 1 mat-select `modeProcedure` (4 valeurs) + 2 datepickers natifs
 *   `<input type="date">` nullable (`dateJugementSeparationCorps`,
 *   `dateRequeteConversion`) + 2 inputs numériques (`dureeSeparationAnnees`,
 *   `enfantsMineurs`) + 3 slide-toggles (`consentementMutuelConversion`,
 *   `patrimoineCommun`, `demandeReconciliationFormulee`).
 * - Verdict 3 valeurs (POSSIBLE / PREMATUREE / RECONCILIATION_BLOQUE) →
 *   palette navy/or/rouge classique (rouge réservé `RECONCILIATION_BLOQUE`).
 * - Pré-fill IA via `aiData?: Partial<FamilleExtractedData> | null` (champs
 *   `patrimoineCommun` + `dateSeparation` mappé sur
 *   `dateJugementSeparationCorps`), no-op gracieux si absent.
 * - Validation F-IA-03 sur 2 fields via `CoherenceAlertBuilder` + popover.
 * - Refresh dashboard F-IA-02 + `MatSnackBar` pour erreurs.
 */
@Component({
  selector: 'app-separation-corps-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule, DecimalPipe,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatSlideToggleModule, MatProgressSpinnerModule,
    LegalCitationsPipe,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './separation-corps-section.component.html',
  styleUrl: './separation-corps-section.component.scss',
})
export class SeparationCorpsSectionComponent implements OnInit, OnChanges {
  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'SÉPARATION DE CORPS + CONVERSION DIVORCE — ART. 296+306 CCIV';
  static readonly TOOL_ICON = 'family_restroom';

  /** F-236 SF-236-02 — compteur miroir prefillFromAi via helper. */
  static getPrefillCount(input: {
    aiData?: Partial<FamilleExtractedData> | null;
    procedureChecks?: unknown[];
    aiQuestions?: unknown[];
    piecesManquantes?: unknown[];
    triggerEvents?: unknown[];
    workspaceCountry?: string;
  }): number {
    return SeparationCorpsPrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';
  // Pré-fill IA + sources F-IA-03 (tous optionnels — null-safe partout).
  @Input() aiData?: Partial<FamilleExtractedData> | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;

  // Snapshots signal — réactivité des `computed` signals F-IA-03.
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
  result = signal<SeparationCorpsResponse | null>(null);

  // Form fields (signals).
  modeProcedure = signal<ModeProcedureSep | null>(null);
  dateJugementSeparationCorps = signal<string | null>(null);
  dateRequeteConversion = signal<string | null>(null);
  dureeSeparationAnnees = signal<number | null>(null);
  consentementMutuelConversion = signal<boolean>(false);
  patrimoineCommun = signal<boolean>(false);
  enfantsMineurs = signal<number | null>(null);
  demandeReconciliationFormulee = signal<boolean>(false);

  // Provenance IA par champ pré-remplissable (2 fields).
  provenanceDateJugementSeparation = signal<'IA' | null>(null);
  provenancePatrimoineCommun = signal<'IA' | null>(null);

  // Map {sourceKey → explanations} pour le popover F-IA-03-15c (fail-open).
  sourceExplanations = signal<Map<string, SourceExplanation[]>>(new Map());

  readonly modesOptions = MODES_PROCEDURE_SEP_FR;

  /**
   * Coherence alerts F-IA-03 — gate `showForm()` strict (pattern anti-bug
   * SF-IA-03-12 : pas de `|| result()`).
   */
  coherenceAlerts = computed<Partial<Record<SeparationCorpsAlertField, SeparationCorpsCoherenceAlert>>>(() => {
    if (!this.showForm()) return {};
    const alerts: Partial<Record<SeparationCorpsAlertField, SeparationCorpsCoherenceAlert>> = {};
    const dateAlert = this.buildDateJugementAlert();
    if (dateAlert) alerts.DATE_JUGEMENT_SEPARATION = dateAlert;
    const patrimoineAlert = this.buildPatrimoineAlert();
    if (patrimoineAlert) alerts.PATRIMOINE_COMMUN = patrimoineAlert;
    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    return { total: values.length };
  });

  isFrance = computed(() => this.workspaceCountry === 'FRANCE');

  constructor(
    private service: SeparationCorpsService,
    private snackBar: MatSnackBar,
    @Optional() private dashboardRefresh: CaseDashboardRefreshService | null,
    @Optional() private sourceExplanationService: SourceExplanationService | null,
  ) {}

  ngOnInit(): void {
    // F-177 SF-177-03b : appliqué dès le mount pour le mode modal.
    if (this.forceExpanded) this.collapsed.set(false);
    this.aiDataSignal.set(this.aiData);
    this.procedureChecksSignal.set(this.procedureChecks ?? []);
    this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    this.piecesManquantesSignal.set(this.piecesManquantes ?? []);
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

    // Re-prefill quand aiData arrive après mount, tant que l'avocat n'a pas
    // saisi manuellement et qu'aucun résultat n'est affiché.
    if (changes['aiData'] && !changes['aiData'].firstChange
        && this.showForm() && !this.result()) {
      this.prefillFromAi();
    }
  }

  private loadSourceExplanations(): void {
    if (!this.caseFileId || !this.sourceExplanationService) return;
    this.sourceExplanationService.getForCaseFile(this.caseFileId).subscribe({
      next: (map) => this.sourceExplanations.set(map),
      error: () => { /* fail-open */ },
    });
  }

  /** SF-IA-03-15c : mapping field → sourceKey pour le popover F-IA-03. */
  explanationFor(field: SeparationCorpsAlertField): SourceExplanation[] {
    const key = (() => {
      switch (field) {
        case 'DATE_JUGEMENT_SEPARATION': return 'FA21_DATE_JUGEMENT_SEPARATION';
        case 'PATRIMOINE_COMMUN': return 'FA21_PATRIMOINE_COMMUN';
      }
    })();
    return this.sourceExplanations().get(key) ?? [];
  }

  alertTooltip(alert: SeparationCorpsCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: SeparationCorpsCoherenceAlert): string {
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
  // Form helpers / handlers
  // ---------------------------------------------------------------------------

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  formValid(): boolean {
    const mode = this.modeProcedure();
    const duree = this.dureeSeparationAnnees();
    const enfants = this.enfantsMineurs();
    return mode !== null
      && duree !== null && Number.isInteger(duree) && duree >= 0
      && enfants !== null && Number.isInteger(enfants) && enfants >= 0;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // Handlers manuels — effacent le badge IA correspondant.
  onModeProcedureChange(value: ModeProcedureSep | null): void {
    this.modeProcedure.set(value);
  }

  onDateJugementSeparationChange(value: string | null): void {
    this.dateJugementSeparationCorps.set(value && value.length > 0 ? value : null);
    this.provenanceDateJugementSeparation.set(null);
  }

  onDateRequeteConversionChange(value: string | null): void {
    this.dateRequeteConversion.set(value && value.length > 0 ? value : null);
  }

  onDureeSeparationAnneesChange(value: number | null): void {
    this.dureeSeparationAnnees.set(value);
  }

  onConsentementMutuelConversionChange(value: boolean): void {
    this.consentementMutuelConversion.set(!!value);
  }

  onPatrimoineCommunChange(value: boolean): void {
    this.patrimoineCommun.set(!!value);
    this.provenancePatrimoineCommun.set(null);
  }

  onEnfantsMineursChange(value: number | null): void {
    this.enfantsMineurs.set(value);
  }

  onDemandeReconciliationFormuleeChange(value: boolean): void {
    this.demandeReconciliationFormulee.set(!!value);
  }

  // ---------------------------------------------------------------------------
  // HTTP : load + calculate
  // ---------------------------------------------------------------------------

  calculate(): void {
    if (!this.formValid()) return;
    const request: SeparationCorpsRequest = {
      modeProcedure: this.modeProcedure()!,
      dateJugementSeparationCorps: this.dateJugementSeparationCorps(),
      dateRequeteConversion: this.dateRequeteConversion(),
      dureeSeparationAnnees: this.dureeSeparationAnnees()!,
      consentementMutuelConversion: this.consentementMutuelConversion(),
      patrimoineCommun: this.patrimoineCommun(),
      enfantsMineurs: this.enfantsMineurs()!,
      demandeReconciliationFormulee: this.demandeReconciliationFormulee(),
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Analyse séparation de corps calculée', 'OK', { duration: 2500 });
        this.dashboardRefresh?.triggerRefresh();
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
        this.applyPersistedResult(r);
        this.loading.set(false);
      },
      error: () => {
        // 404 attendu si aucune analyse — on reste en mode formulaire.
        this.prefillFromAi();
        this.loading.set(false);
      },
    });
  }

  private applyPersistedResult(r: SeparationCorpsResponse): void {
    this.result.set(r);
    this.modeProcedure.set(r.modeProcedure);
    this.dateJugementSeparationCorps.set(r.dateJugementSeparationCorps);
    this.dateRequeteConversion.set(r.dateRequeteConversion);
    this.dureeSeparationAnnees.set(r.dureeSeparationAnnees);
    this.consentementMutuelConversion.set(r.consentementMutuelConversion);
    this.patrimoineCommun.set(r.patrimoineCommun);
    this.enfantsMineurs.set(r.enfantsMineurs);
    this.demandeReconciliationFormulee.set(r.demandeReconciliationFormulee);
    // Valeurs persistées = saisie avocat — jamais de badge IA.
    this.provenanceDateJugementSeparation.set(null);
    this.provenancePatrimoineCommun.set(null);
    this.showForm.set(false);
  }

  /**
   * Pré-fill 2 champs IA :
   *  - dateJugementSeparationCorps (depuis `dateSeparation`)
   *  - patrimoineCommun
   *
   * No-op gracieux si champ absent. N'écrase pas une saisie avocat
   * (provenance === null indique modification manuelle).
   */
  private prefillFromAi(): void {
    // F-236 SF-236-02 — délégation au helper partagé.
    const h = { aiData: this.aiDataSignal() };

    const date = SeparationCorpsPrefillRules.computeDateJugement(h);
    if (date !== null
        && (this.dateJugementSeparationCorps() === null
            || this.provenanceDateJugementSeparation() === 'IA')) {
      this.dateJugementSeparationCorps.set(date);
      this.provenanceDateJugementSeparation.set('IA');
    }

    const patr = SeparationCorpsPrefillRules.computePatrimoineCommun(h);
    if (patr !== null
        && (this.provenancePatrimoineCommun() === 'IA' || this.patrimoineCommun() === false)) {
      this.patrimoineCommun.set(patr);
      this.provenancePatrimoineCommun.set('IA');
    }
  }

  // ---------------------------------------------------------------------------
  // Builders d'alertes F-IA-03 (un par field) — via CoherenceAlertBuilder.
  // ---------------------------------------------------------------------------

  /** Date jugement séparation — divergence stricte sur ISO date. */
  private buildDateJugementAlert(): SeparationCorpsCoherenceAlert | null {
    const iaDate = this.aiDataSignal()?.dateSeparation;
    const user = this.dateJugementSeparationCorps();
    if (!user) return null;
    if (!iaDate || !ISO_DATE_REGEX.test(iaDate)) return null;
    if (user === iaDate) return null;
    const builder = CoherenceAlertBuilder.forField<SeparationCorpsAlertField>('DATE_JUGEMENT_SEPARATION')
      .addSource('IA', {
        expectedDisplay: iaDate,
        reason: `Analyse du dossier : séparation prononcée le ${iaDate}`,
      });

    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'FA21_DATE_JUGEMENT_SEPARATION') continue;
      if (!chk.expectedValue) continue;
      builder.addSource('F96', {
        expectedDisplay: chk.expectedValue,
        reason: `Checklist procédurale : date attendue "${chk.expectedValue}"${chk.raison ? ' (' + chk.raison + ')' : ''}`,
      });
      break;
    }

    const piece = this.findPieceManquante([
      'FA21_DATE_JUGEMENT_SEPARATION',
      'JUGEMENT_SEPARATION_CORPS',
    ]);
    if (piece) builder.addPieceManquante(piece);
    return builder.build();
  }

  /** Patrimoine commun — divergence stricte boolean. */
  private buildPatrimoineAlert(): SeparationCorpsCoherenceAlert | null {
    const iaVal = this.aiDataSignal()?.patrimoineCommun;
    if (typeof iaVal !== 'boolean') return null;
    const user = this.patrimoineCommun();
    if (iaVal === user) return null;
    const display = iaVal ? 'Patrimoine commun' : 'Pas de patrimoine commun';
    const builder = CoherenceAlertBuilder.forField<SeparationCorpsAlertField>('PATRIMOINE_COMMUN')
      .addSource('IA', {
        expectedDisplay: display,
        reason: `Analyse du dossier : ${display.toLowerCase()}`,
      });
    const piece = this.findPieceManquante(['FA21_PATRIMOINE_COMMUN', 'CONTRAT_MARIAGE']);
    if (piece) builder.addPieceManquante(piece);
    return builder.build();
  }

  /** Retourne le `texte` d'une `PieceManquanteEntry` matchant un code. */
  private findPieceManquante(acceptedCodes: string[]): string | null {
    const norm = new Set(acceptedCodes.map((c) => c.toUpperCase()));
    for (const p of this.piecesManquantesSignal()) {
      const code = p.critereCode?.toUpperCase();
      if (!code) continue;
      if (norm.has(code)) return p.texte;
    }
    return null;
  }

  // ---------------------------------------------------------------------------
  // Display helpers
  // ---------------------------------------------------------------------------

  modeProcedureSepLabel = modeProcedureSepLabel;
  verdictLabel = verdictConversionLabel;

  verdictBannerClass(verdict: VerdictConversion): string {
    switch (verdict) {
      case 'POSSIBLE': return 'sc-verdict-banner sc-verdict-banner--strong';
      case 'PREMATUREE': return 'sc-verdict-banner sc-verdict-banner--medium';
      case 'RECONCILIATION_BLOQUE': return 'sc-verdict-banner sc-verdict-banner--weak';
    }
  }

  verdictChipClass(verdict: VerdictConversion): string {
    switch (verdict) {
      case 'POSSIBLE': return 'sc-chip--strong';
      case 'PREMATUREE': return 'sc-chip--medium';
      case 'RECONCILIATION_BLOQUE': return 'sc-chip--weak';
    }
  }

  verdictBannerIcon(verdict: VerdictConversion): string {
    switch (verdict) {
      case 'POSSIBLE': return 'check_circle';
      case 'PREMATUREE': return 'schedule';
      case 'RECONCILIATION_BLOQUE': return 'error';
    }
  }
}
