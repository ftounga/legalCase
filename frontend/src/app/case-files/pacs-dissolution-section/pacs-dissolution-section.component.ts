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
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { PacsDissolutionService } from '../../core/services/pacs-dissolution.service';
import {
  CREANCES_ALLEGUEES_FR,
  CreanceAlleguee,
  MODES_DISSOLUTION_FR,
  ModeDissolutionPacs,
  PacsDissolutionRequest,
  PacsDissolutionResponse,
  REGIMES_BIENS_PACS_FR,
  RegimeBiensPacs,
  VerdictPacs,
  creanceAllegueeLabel,
  modeDissolutionLabel,
  regimeBiensPacsLabel,
  verdictPacsLabel,
} from '../../core/models/pacs-dissolution.model';
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
import { PacsDissolutionPrefillRules } from './pacs-dissolution-section-prefill-rules';
import { SourceExplanation } from '../../core/models/source-explanation.model';
import { SourceExplanationService } from '../../core/services/source-explanation.service';

/**
 * SF-FA-20-02 : champs F-IA-03 audités par l'outil "Dissolution PACS"
 * (F-FA-20, art. 515-7 Cciv). Un par champ pré-remplissable depuis l'IA
 * (`FamilleExtractedData`).
 */
export type PacsDissolutionAlertField =
  | 'DATE_CONCLUSION_PACS'
  | 'MODE_DISSOLUTION'
  | 'REGIME_BIENS'
  | 'CREANCES_ALLEGUEES'
  | 'PATRIMOINE_COMMUN';

export type PacsDissolutionAlertSource = CoherenceAlertSource;
export type PacsDissolutionCoherenceAlert =
    CoherenceAlert<PacsDissolutionAlertField>;

const ISO_DATE_REGEX = /^\d{4}-\d{2}-\d{2}$/;

const VALID_MODES: ReadonlySet<string> = new Set<ModeDissolutionPacs>([
  'DECLARATION_UNILATERALE',
  'DECLARATION_CONJOINTE',
  'MARIAGE_PARTENAIRES',
  'MARIAGE_TIERS',
  'DECES',
]);

const VALID_REGIMES: ReadonlySet<string> = new Set<RegimeBiensPacs>([
  'SEPARATION_BIENS',
  'INDIVISION_AMENAGEE',
  'INDIVISION_PAR_DEFAUT',
]);

const VALID_CREANCES: ReadonlySet<string> = new Set<CreanceAlleguee>([
  'CONTRIBUTION_DESEQUILIBRE',
  'INVESTISSEMENT_BIEN_PROPRE',
  'ENRICHISSEMENT_INJUSTE',
  'PRESTATION_TRAVAIL_NON_REMUNEREE',
  'AUCUNE',
]);

/**
 * SF-FA-20-02 : composant Angular standalone pour l'outil "Dissolution PACS
 * art. 515-7 Cciv" (F-FA-20, FRANCE uniquement).
 *
 * Pattern de référence : `desaccords-parentaux-section` (template canonique
 * 2026-04-25 — multi-select F-IA-03 + 5 fields pré-fillables) +
 * `divorce-accepte-section` (datepickers natifs + provenance signals) +
 * `recompenses-section` (palette navy/or famille).
 *
 * - Form : 3 datepickers natifs `<input type="date">` (datePacs, dateDissolution,
 *   dateNotification) + mat-select modeDissolution (5) + mat-select regimeBiens
 *   (3) + mat-select multiple creancesAlleguees (5) + 2 inputs numériques
 *   (dureeUnion, enfantsCommuns) + slide-toggle patrimoineCommunSignificatif.
 * - Verdict 4 valeurs (LIQUIDATION_AMIABLE / MEDIATION_OU_JAF /
 *   CONTENTIEUX_INEVITABLE / RIEN_A_FAIRE) → palette navy/or/rouge classique.
 * - Pré-fill IA via `aiData?: FamilleExtractedData | null` (5 fields ouverts,
 *   no-op gracieux si absent).
 * - Validation F-IA-03 sur 5 fields via `CoherenceAlertBuilder` + popover.
 * - Refresh dashboard F-IA-02 + `MatSnackBar` pour erreurs.
 */
@Component({
  selector: 'app-pacs-dissolution-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule, DecimalPipe,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatSlideToggleModule, MatChipsModule,
    MatProgressSpinnerModule,
    LegalCitationsPipe,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './pacs-dissolution-section.component.html',
  styleUrl: './pacs-dissolution-section.component.scss',
})
export class PacsDissolutionSectionComponent implements OnInit, OnChanges {
  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'DISSOLUTION PACS (FR) — ART. 515-7 CCIV';
  static readonly TOOL_ICON = 'link_off';

  /** F-236 SF-236-02 — compteur miroir prefillFromAi via helper. */
  static getPrefillCount(input: {
    aiData?: Partial<FamilleExtractedData> | null;
    procedureChecks?: unknown[];
    aiQuestions?: unknown[];
    piecesManquantes?: unknown[];
    triggerEvents?: unknown[];
    workspaceCountry?: string;
  }): number {
    return PacsDissolutionPrefillRules.computePrefillCount(input);
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
  result = signal<PacsDissolutionResponse | null>(null);

  // Form fields (signals).
  dateConclusionPacs = signal<string | null>(null);
  modeDissolution = signal<ModeDissolutionPacs | null>(null);
  dateDissolution = signal<string | null>(null);
  dureeUnionAnnees = signal<number | null>(null);
  regimeBiens = signal<RegimeBiensPacs | null>(null);
  patrimoineCommunSignificatif = signal<boolean>(false);
  creancesAlleguees = signal<CreanceAlleguee[]>([]);
  enfantsCommuns = signal<number | null>(null);
  dateNotificationPartenaire = signal<string | null>(null);

  // Provenance IA par champ pré-remplissable (5 fields).
  provenanceDateConclusionPacs = signal<'IA' | null>(null);
  provenanceModeDissolution = signal<'IA' | null>(null);
  provenanceRegimeBiens = signal<'IA' | null>(null);
  provenanceCreancesAlleguees = signal<'IA' | null>(null);
  provenancePatrimoineCommun = signal<'IA' | null>(null);

  // Map {sourceKey → explanations} pour le popover F-IA-03-15c (fail-open).
  sourceExplanations = signal<Map<string, SourceExplanation[]>>(new Map());

  readonly modesOptions = MODES_DISSOLUTION_FR;
  readonly regimesOptions = REGIMES_BIENS_PACS_FR;
  readonly creancesOptions = CREANCES_ALLEGUEES_FR;

  /**
   * Coherence alerts F-IA-03 — gate `showForm()` strict (pattern anti-bug
   * SF-IA-03-12 : pas de `|| result()`).
   */
  coherenceAlerts = computed<Partial<Record<PacsDissolutionAlertField, PacsDissolutionCoherenceAlert>>>(() => {
    if (!this.showForm()) return {};
    const alerts: Partial<Record<PacsDissolutionAlertField, PacsDissolutionCoherenceAlert>> = {};
    const dateAlert = this.buildDateConclusionAlert();
    if (dateAlert) alerts.DATE_CONCLUSION_PACS = dateAlert;
    const modeAlert = this.buildModeDissolutionAlert();
    if (modeAlert) alerts.MODE_DISSOLUTION = modeAlert;
    const regimeAlert = this.buildRegimeBiensAlert();
    if (regimeAlert) alerts.REGIME_BIENS = regimeAlert;
    const creancesAlert = this.buildCreancesAlert();
    if (creancesAlert) alerts.CREANCES_ALLEGUEES = creancesAlert;
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
    private service: PacsDissolutionService,
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
  explanationFor(field: PacsDissolutionAlertField): SourceExplanation[] {
    const key = (() => {
      switch (field) {
        case 'DATE_CONCLUSION_PACS': return 'FA20_DATE_CONCLUSION_PACS';
        case 'MODE_DISSOLUTION': return 'FA20_MODE_DISSOLUTION';
        case 'REGIME_BIENS': return 'FA20_REGIME_BIENS';
        case 'CREANCES_ALLEGUEES': return 'FA20_CREANCES_ALLEGUEES';
        case 'PATRIMOINE_COMMUN': return 'FA20_PATRIMOINE_COMMUN';
      }
    })();
    return this.sourceExplanations().get(key) ?? [];
  }

  alertTooltip(alert: PacsDissolutionCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: PacsDissolutionCoherenceAlert): string {
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
    const datePacs = this.dateConclusionPacs();
    const mode = this.modeDissolution();
    const dateDis = this.dateDissolution();
    const duree = this.dureeUnionAnnees();
    const reg = this.regimeBiens();
    const cr = this.creancesAlleguees();
    const enf = this.enfantsCommuns();
    const dateNotif = this.dateNotificationPartenaire();
    return !!datePacs && ISO_DATE_REGEX.test(datePacs)
      && mode !== null
      && !!dateDis && ISO_DATE_REGEX.test(dateDis)
      && duree !== null && Number.isInteger(duree) && duree >= 0
      && reg !== null
      && cr.length > 0
      && enf !== null && Number.isInteger(enf) && enf >= 0
      && !!dateNotif && ISO_DATE_REGEX.test(dateNotif);
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // Handlers manuels — effacent le badge IA correspondant.
  onDateConclusionPacsChange(value: string | null): void {
    this.dateConclusionPacs.set(value && value.length > 0 ? value : null);
    this.provenanceDateConclusionPacs.set(null);
  }

  onModeDissolutionChange(value: ModeDissolutionPacs | null): void {
    this.modeDissolution.set(value);
    this.provenanceModeDissolution.set(null);
  }

  onDateDissolutionChange(value: string | null): void {
    this.dateDissolution.set(value && value.length > 0 ? value : null);
  }

  onDureeUnionAnneesChange(value: number | null): void {
    this.dureeUnionAnnees.set(value);
  }

  onRegimeBiensChange(value: RegimeBiensPacs | null): void {
    this.regimeBiens.set(value);
    this.provenanceRegimeBiens.set(null);
  }

  onPatrimoineCommunChange(value: boolean): void {
    this.patrimoineCommunSignificatif.set(!!value);
    this.provenancePatrimoineCommun.set(null);
  }

  onCreancesAlleguueesChange(value: CreanceAlleguee[] | null): void {
    this.creancesAlleguees.set(value ?? []);
    this.provenanceCreancesAlleguees.set(null);
  }

  onEnfantsCommunsChange(value: number | null): void {
    this.enfantsCommuns.set(value);
  }

  onDateNotificationPartenaireChange(value: string | null): void {
    this.dateNotificationPartenaire.set(value && value.length > 0 ? value : null);
  }

  // ---------------------------------------------------------------------------
  // HTTP : load + calculate
  // ---------------------------------------------------------------------------

  calculate(): void {
    if (!this.formValid()) return;
    const request: PacsDissolutionRequest = {
      dateConclusionPacs: this.dateConclusionPacs()!,
      modeDissolution: this.modeDissolution()!,
      dateDissolution: this.dateDissolution()!,
      dureeUnionAnnees: this.dureeUnionAnnees()!,
      regimeBiens: this.regimeBiens()!,
      patrimoineCommunSignificatif: this.patrimoineCommunSignificatif(),
      creancesAlleguees: this.creancesAlleguees(),
      enfantsCommuns: this.enfantsCommuns()!,
      dateNotificationPartenaire: this.dateNotificationPartenaire()!,
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Analyse dissolution PACS calculée', 'OK', { duration: 2500 });
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

  private applyPersistedResult(r: PacsDissolutionResponse): void {
    this.result.set(r);
    this.dateConclusionPacs.set(r.dateConclusionPacs);
    this.modeDissolution.set(r.modeDissolution);
    this.dateDissolution.set(r.dateDissolution);
    this.dureeUnionAnnees.set(r.dureeUnionAnnees);
    this.regimeBiens.set(r.regimeBiens);
    this.patrimoineCommunSignificatif.set(r.patrimoineCommunSignificatif);
    this.creancesAlleguees.set(r.creancesAlleguees ?? []);
    this.enfantsCommuns.set(r.enfantsCommuns);
    this.dateNotificationPartenaire.set(r.dateNotificationPartenaire);
    // Valeurs persistées = saisie avocat — jamais de badge IA.
    this.provenanceDateConclusionPacs.set(null);
    this.provenanceModeDissolution.set(null);
    this.provenanceRegimeBiens.set(null);
    this.provenanceCreancesAlleguees.set(null);
    this.provenancePatrimoineCommun.set(null);
    this.showForm.set(false);
  }

  /**
   * Pré-fill 5 champs IA :
   *  - dateConclusionPacs
   *  - modeDissolution (depuis modeDissolutionPacsDetecte)
   *  - regimeBiens (depuis regimeBiensPacsDetecte)
   *  - creancesAlleguees (depuis creancesAllegueesDetectees)
   *  - patrimoineCommunSignificatif (depuis patrimoineCommunSignificatifDetecte)
   *
   * No-op gracieux si champ absent. N'écrase pas une saisie avocat
   * (provenance === null indique modification manuelle).
   */
  private prefillFromAi(): void {
    // F-236 SF-236-02 — délégation au helper partagé.
    const h = { aiData: this.aiDataSignal() };

    const date = PacsDissolutionPrefillRules.computeDateConclusion(h);
    if (date !== null
        && (this.dateConclusionPacs() === null || this.provenanceDateConclusionPacs() === 'IA')) {
      this.dateConclusionPacs.set(date);
      this.provenanceDateConclusionPacs.set('IA');
    }

    const mode = PacsDissolutionPrefillRules.computeModeDissolution(h);
    if (mode !== null
        && (this.modeDissolution() === null || this.provenanceModeDissolution() === 'IA')) {
      this.modeDissolution.set(mode);
      this.provenanceModeDissolution.set('IA');
    }

    const reg = PacsDissolutionPrefillRules.computeRegimeBiens(h);
    if (reg !== null
        && (this.regimeBiens() === null || this.provenanceRegimeBiens() === 'IA')) {
      this.regimeBiens.set(reg);
      this.provenanceRegimeBiens.set('IA');
    }

    const cre = PacsDissolutionPrefillRules.computeCreances(h);
    if (cre.length > 0
        && (this.creancesAlleguees().length === 0 || this.provenanceCreancesAlleguees() === 'IA')) {
      this.creancesAlleguees.set(cre);
      this.provenanceCreancesAlleguees.set('IA');
    }

    const patr = PacsDissolutionPrefillRules.computePatrimoineCommun(h);
    if (patr !== null
        && (this.provenancePatrimoineCommun() === 'IA' || this.patrimoineCommunSignificatif() === false)) {
      this.patrimoineCommunSignificatif.set(patr);
      this.provenancePatrimoineCommun.set('IA');
    }
  }

  // ---------------------------------------------------------------------------
  // Builders d'alertes F-IA-03 (un par field) — via CoherenceAlertBuilder.
  // ---------------------------------------------------------------------------

  /** Date conclusion PACS — divergence stricte sur ISO date. */
  private buildDateConclusionAlert(): PacsDissolutionCoherenceAlert | null {
    const iaDate = this.aiDataSignal()?.dateConclusionPacs;
    const user = this.dateConclusionPacs();
    if (!user) return null;
    if (!iaDate || !ISO_DATE_REGEX.test(iaDate)) return null;
    if (user === iaDate) return null;
    const builder = CoherenceAlertBuilder.forField<PacsDissolutionAlertField>('DATE_CONCLUSION_PACS')
      .addSource('IA', {
        expectedDisplay: iaDate,
        reason: `Analyse du dossier : PACS conclu le ${iaDate}`,
      });
    const piece = this.findPieceManquante(['FA20_DATE_CONCLUSION_PACS', 'CONVENTION_PACS']);
    if (piece) builder.addPieceManquante(piece);
    return builder.build();
  }

  /** Mode dissolution — divergence stricte sur enum 5 valeurs. */
  private buildModeDissolutionAlert(): PacsDissolutionCoherenceAlert | null {
    const ai = this.aiDataSignal();
    const aiModeRaw = ai?.modeDissolutionPacsDetecte;
    if (typeof aiModeRaw !== 'string' || aiModeRaw.length === 0) return null;
    const aiMode = aiModeRaw.toUpperCase();
    if (!VALID_MODES.has(aiMode)) return null;
    const userMode = this.modeDissolution();
    if (userMode === null) return null;
    if (userMode === aiMode) return null;

    const display = modeDissolutionLabel(aiMode as ModeDissolutionPacs);
    const builder = CoherenceAlertBuilder.forField<PacsDissolutionAlertField>('MODE_DISSOLUTION')
      .addSource('IA', {
        expectedDisplay: display,
        reason: `Analyse du dossier : mode "${display}"`,
      });

    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'FA20_MODE_DISSOLUTION') continue;
      if (!chk.expectedValue) continue;
      builder.addSource('F96', {
        expectedDisplay: display,
        reason: `Checklist procédurale : mode attendu "${chk.expectedValue}"${chk.raison ? ' (' + chk.raison + ')' : ''}`,
      });
      break;
    }

    const piece = this.findPieceManquante(['FA20_MODE_DISSOLUTION', 'NOTIFICATION_PARTENAIRE']);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  /** Régime biens — divergence stricte sur enum 3 valeurs. */
  private buildRegimeBiensAlert(): PacsDissolutionCoherenceAlert | null {
    const ai = this.aiDataSignal();
    const aiRegRaw = ai?.regimeBiensPacsDetecte;
    if (typeof aiRegRaw !== 'string' || aiRegRaw.length === 0) return null;
    const aiReg = aiRegRaw.toUpperCase();
    if (!VALID_REGIMES.has(aiReg)) return null;
    const userReg = this.regimeBiens();
    if (userReg === null) return null;
    if (userReg === aiReg) return null;

    const display = regimeBiensPacsLabel(aiReg as RegimeBiensPacs);
    const builder = CoherenceAlertBuilder.forField<PacsDissolutionAlertField>('REGIME_BIENS')
      .addSource('IA', {
        expectedDisplay: display,
        reason: `Analyse du dossier : régime "${display}"`,
      });

    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'FA20_REGIME_BIENS') continue;
      if (!chk.expectedValue) continue;
      builder.addSource('F96', {
        expectedDisplay: display,
        reason: `Checklist procédurale : régime attendu "${chk.expectedValue}"${chk.raison ? ' (' + chk.raison + ')' : ''}`,
      });
      break;
    }

    const piece = this.findPieceManquante(['FA20_REGIME_BIENS', 'CONVENTION_PACS']);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  /** Créances alléguées — divergence ensembliste IA vs user. */
  private buildCreancesAlert(): PacsDissolutionCoherenceAlert | null {
    const ai = this.aiDataSignal();
    const aiCr = ai?.creancesAllegueesDetectees;
    if (!Array.isArray(aiCr) || aiCr.length === 0) return null;
    const aiSet = aiCr
      .filter((t): t is string => typeof t === 'string' && t.length > 0)
      .map((t) => t.toUpperCase())
      .filter((t) => VALID_CREANCES.has(t));
    if (aiSet.length === 0) return null;
    const userSet = this.creancesAlleguees();
    if (userSet.length === 0) return null;

    const aiSorted = [...aiSet].sort();
    const userSorted = [...userSet].sort();
    const same = aiSorted.length === userSorted.length
      && aiSorted.every((v, i) => v === userSorted[i]);
    if (same) return null;

    const display = aiSet.map((t) => creanceAllegueeLabel(t as CreanceAlleguee)).join(', ');
    const builder = CoherenceAlertBuilder.forField<PacsDissolutionAlertField>('CREANCES_ALLEGUEES')
      .addSource('IA', {
        expectedDisplay: display,
        reason: `Analyse du dossier : créances "${display}"`,
      });

    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'FA20_CREANCES_ALLEGUEES') continue;
      if (!chk.expectedValue) continue;
      builder.addSource('F96', {
        expectedDisplay: display,
        reason: `Checklist procédurale : créances attendues "${chk.expectedValue}"${chk.raison ? ' (' + chk.raison + ')' : ''}`,
      });
      break;
    }

    const piece = this.findPieceManquante([
      'FA20_CREANCES_ALLEGUEES',
      'JUSTIFICATIF_INVESTISSEMENT',
      'RELEVES_BANCAIRES',
    ]);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  /** Patrimoine commun significatif — divergence stricte boolean. */
  private buildPatrimoineAlert(): PacsDissolutionCoherenceAlert | null {
    const iaVal = this.aiDataSignal()?.patrimoineCommunSignificatifDetecte;
    if (typeof iaVal !== 'boolean') return null;
    const user = this.patrimoineCommunSignificatif();
    if (iaVal === user) return null;
    const display = iaVal ? 'Patrimoine commun significatif' : 'Pas de patrimoine commun significatif';
    const builder = CoherenceAlertBuilder.forField<PacsDissolutionAlertField>('PATRIMOINE_COMMUN')
      .addSource('IA', {
        expectedDisplay: display,
        reason: `Analyse du dossier : ${display.toLowerCase()}`,
      });
    const piece = this.findPieceManquante(['FA20_PATRIMOINE_COMMUN', 'RELEVES_BANCAIRES']);
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

  modeDissolutionLabel = modeDissolutionLabel;
  regimeBiensPacsLabel = regimeBiensPacsLabel;
  creanceAllegueeLabel = creanceAllegueeLabel;
  verdictLabel = verdictPacsLabel;

  verdictBannerClass(verdict: VerdictPacs): string {
    switch (verdict) {
      case 'RIEN_A_FAIRE': return 'pd-verdict-banner pd-verdict-banner--strong';
      case 'LIQUIDATION_AMIABLE': return 'pd-verdict-banner pd-verdict-banner--strong';
      case 'MEDIATION_OU_JAF': return 'pd-verdict-banner pd-verdict-banner--medium';
      case 'CONTENTIEUX_INEVITABLE': return 'pd-verdict-banner pd-verdict-banner--weak';
    }
  }

  verdictChipClass(verdict: VerdictPacs): string {
    switch (verdict) {
      case 'RIEN_A_FAIRE': return 'pd-chip--strong';
      case 'LIQUIDATION_AMIABLE': return 'pd-chip--strong';
      case 'MEDIATION_OU_JAF': return 'pd-chip--medium';
      case 'CONTENTIEUX_INEVITABLE': return 'pd-chip--weak';
    }
  }

  verdictBannerIcon(verdict: VerdictPacs): string {
    switch (verdict) {
      case 'RIEN_A_FAIRE': return 'check_circle';
      case 'LIQUIDATION_AMIABLE': return 'handshake';
      case 'MEDIATION_OU_JAF': return 'balance';
      case 'CONTENTIEUX_INEVITABLE': return 'gavel';
    }
  }
}
