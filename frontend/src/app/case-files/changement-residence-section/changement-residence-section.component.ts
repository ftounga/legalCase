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
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ChangementResidenceService } from '../../core/services/changement-residence.service';
import {
  ChangementResidenceRequest,
  ChangementResidenceResponse,
  ChangementResidenceVerdict,
  MODES_RESIDENCE_FR,
  ModeResidenceCh,
  RAISONS_CHANGEMENT_FR,
  RaisonChangement,
  modeResidenceLabel,
  raisonChangementLabel,
  verdictChangementResidenceLabel,
} from '../../core/models/changement-residence.model';
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
import { ChangementResidencePrefillRules } from './changement-residence-section-prefill-rules';
import { SourceExplanation } from '../../core/models/source-explanation.model';
import { SourceExplanationService } from '../../core/services/source-explanation.service';

/**
 * SF-FA-19-04 : fields F-IA-03 audités par l'outil F-FA-19 (changement de
 * résidence). Un par champ pré-remplissable depuis l'IA `FamilleExtractedData`.
 */
export type ChangementResidenceAlertField =
  | 'RAISON_CHANGEMENT'
  | 'CONSENTEMENT_AUTRE_PARENT'
  | 'INFORME_PREALABLEMENT'
  | 'MODE_RESIDENCE_ACTUEL'
  | 'AGE_ENFANTS';

export type ChangementResidenceAlertSource = CoherenceAlertSource;
export type ChangementResidenceCoherenceAlert =
  CoherenceAlert<ChangementResidenceAlertField>;

const ISO_DATE_REGEX = /^\d{4}-\d{2}-\d{2}$/;

const VALID_RAISONS: ReadonlySet<string> = new Set<RaisonChangement>([
  'TRAVAIL',
  'FAMILLE',
  'LOGEMENT',
  'RAPPROCHEMENT_FAMILIAL',
  'AUTRE',
]);

const VALID_MODES: ReadonlySet<string> = new Set<ModeResidenceCh>([
  'ALTERNEE',
  'EXCLUSIVE_DEMANDEUR',
  'EXCLUSIVE_DEFENDEUR',
]);

/**
 * SF-FA-19-04 : composant Angular standalone pour l'outil "Changement de
 * résidence" (F-FA-19, FRANCE uniquement, art. 373-2 Cciv — obligation
 * d'information préalable, délai de préavis raisonnable, modification
 * éventuelle des modalités de la DVH).
 *
 * Pattern de référence : `harcelement-licenciement-nul-section` (template
 * canonique 2026-04-24, cf. `ai-skills/frontend-coherence-audit.md` §5).
 * Miroir famille : `autorite-parentale-section` (SF-FA-19-02, même module
 * F-FA-19, FamilleExtractedData + builder F-IA-03 + 5 fields, parsing CSV
 * âges enfants).
 *
 * - Form : `<input type="date">` dateChangementPrevu + numérique distanceKm +
 *   mat-select RaisonChangement (5 options) + 4 slide-toggles (consentement /
 *   informePrealablement / scolariteImpactee / modificationDvhDemandee) +
 *   numérique delaiInformationJours + mat-select ModeResidenceCh (3 options) +
 *   input texte CSV âges.
 * - Verdict 3 paliers ELEVEE/MOYENNE/FAIBLE → palette navy/or/rouge classique.
 * - Pré-fill IA via `aiData?: FamilleExtractedData | null` (5 fields ouverts,
 *   no-op gracieux si absent).
 * - Validation F-IA-03 sur 5 fields via `CoherenceAlertBuilder` (multi-sources
 *   IA + F96 + QUESTION_IA + PIECE_MANQUANTE) + popover.
 * - Refresh dashboard F-IA-02 + `MatSnackBar` pour erreurs.
 */
@Component({
  selector: 'app-changement-residence-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatSlideToggleModule,
    MatProgressSpinnerModule,
    LegalCitationsPipe,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './changement-residence-section.component.html',
  styleUrl: './changement-residence-section.component.scss',
})
export class ChangementResidenceSectionComponent implements OnInit, OnChanges {
  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'CHANGEMENT DE RÉSIDENCE (FR) — ART. 373-2 CCIV';
  static readonly TOOL_ICON = 'home_work';

  /** F-236 SF-236-02 — compteur miroir prefillFromAi via helper. */
  static getPrefillCount(input: {
    aiData?: FamilleExtractedData | null;
    procedureChecks?: unknown[];
    aiQuestions?: unknown[];
    piecesManquantes?: unknown[];
    triggerEvents?: unknown[];
    workspaceCountry?: string;
  }): number {
    return ChangementResidencePrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';
  // Pré-fill IA + sources F-IA-03 (tous optionnels — null-safe partout).
  @Input() aiData?: FamilleExtractedData | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;

  // Snapshots signal pour réactivité des `computed` signals F-IA-03.
  private aiDataSignal = signal<FamilleExtractedData | null | undefined>(undefined);
  private procedureChecksSignal = signal<ProcedureCheck[]>([]);
  private aiQuestionsSignal = signal<AiQuestion[]>([]);
  private piecesManquantesSignal = signal<PieceManquanteEntry[]>([]);

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
   *  - `calculate()` POSTe sur `/api/v1/simulators/F-FA-19-changement-residence/calculate`.
   *  - `triggerRefresh()` jamais invoqué.
   *
   * Default `false` — mode case-file scoped inchangé.
   */
  @Input() standaloneMode: boolean = false;
  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<ChangementResidenceResponse | null>(null);

  // Form fields.
  dateChangementPrevu = signal<string | null>(null);
  distanceKm = signal<number | null>(null);
  raisonChangement = signal<RaisonChangement | null>(null);
  consentementAutreParent = signal<boolean>(false);
  informePrealablement = signal<boolean>(false);
  delaiInformationJours = signal<number>(0);
  modeResidenceActuel = signal<ModeResidenceCh | null>(null);
  /** Saisi via input texte CSV (parsé). */
  ageEnfantsRaw = signal<string>('');
  ageEnfants = signal<number[]>([]);
  scolariteImpactee = signal<boolean>(false);
  modificationDvhDemandee = signal<boolean>(false);

  // Provenance IA par champ pré-remplissable (5 fields).
  provenanceRaisonChangement = signal<'IA' | null>(null);
  provenanceConsentementAutreParent = signal<'IA' | null>(null);
  provenanceInformePrealablement = signal<'IA' | null>(null);
  provenanceModeResidenceActuel = signal<'IA' | null>(null);
  provenanceAgeEnfants = signal<'IA' | null>(null);

  // Map {sourceKey → explanations} pour le popover F-IA-03-15c (fail-open).
  sourceExplanations = signal<Map<string, SourceExplanation[]>>(new Map());

  readonly raisonsOptions = RAISONS_CHANGEMENT_FR;
  readonly modesOptions = MODES_RESIDENCE_FR;

  /**
   * Coherence alerts F-IA-03 — gate `showForm()` strict (pattern anti-bug
   * SF-IA-03-12 : pas de `|| result()`).
   */
  coherenceAlerts = computed<Partial<Record<ChangementResidenceAlertField, ChangementResidenceCoherenceAlert>>>(() => {
    // F-163 SF-163-02c : aucune source IA en standalone.
    if (this.standaloneMode) return {};
    if (!this.showForm()) return {};
    const alerts: Partial<Record<ChangementResidenceAlertField, ChangementResidenceCoherenceAlert>> = {};
    const raisonAlert = this.buildRaisonAlert();
    if (raisonAlert) alerts.RAISON_CHANGEMENT = raisonAlert;
    const consentementAlert = this.buildConsentementAlert();
    if (consentementAlert) alerts.CONSENTEMENT_AUTRE_PARENT = consentementAlert;
    const informeAlert = this.buildInformePrealablementAlert();
    if (informeAlert) alerts.INFORME_PREALABLEMENT = informeAlert;
    const modeAlert = this.buildModeResidenceAlert();
    if (modeAlert) alerts.MODE_RESIDENCE_ACTUEL = modeAlert;
    const ageAlert = this.buildAgeEnfantsAlert();
    if (ageAlert) alerts.AGE_ENFANTS = ageAlert;
    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    return { total: values.length };
  });

  isFrance = computed(() => this.workspaceCountry === 'FRANCE');

  constructor(
    private service: ChangementResidenceService,
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

    if (this.standaloneMode) {
      // F-163 SF-163-02c : pas de dossier à interroger en standalone.
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

    // Re-prefill quand aiData arrive après mount, tant que l'avocat n'a pas
    // saisi manuellement et qu'aucun résultat n'est affiché.
    if (changes['aiData'] && !changes['aiData'].firstChange
        && this.showForm() && !this.result() && !this.standaloneMode) {
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
  explanationFor(field: ChangementResidenceAlertField): SourceExplanation[] {
    const key = (() => {
      switch (field) {
        case 'RAISON_CHANGEMENT': return 'FA19_RAISON_CHANGEMENT';
        case 'CONSENTEMENT_AUTRE_PARENT': return 'FA19_CONSENTEMENT_AUTRE_PARENT';
        case 'INFORME_PREALABLEMENT': return 'FA19_INFORME_PREALABLEMENT';
        case 'MODE_RESIDENCE_ACTUEL': return 'FA19_MODE_RESIDENCE_ACTUEL';
        case 'AGE_ENFANTS': return 'FA19_AGE_ENFANTS';
      }
    })();
    return this.sourceExplanations().get(key) ?? [];
  }

  alertTooltip(alert: ChangementResidenceCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: ChangementResidenceCoherenceAlert): string {
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

  /**
   * Parse la chaîne CSV des âges enfants — accepte espaces / virgules /
   * point-virgules. Filtre les valeurs non numériques ou hors range [0, 30].
   */
  parseAgeEnfants(raw: string): number[] {
    if (!raw) return [];
    const tokens = raw.split(/[,;\s]+/).filter((t) => t.length > 0);
    const ages: number[] = [];
    for (const t of tokens) {
      const n = Number(t);
      if (Number.isInteger(n) && n >= 0 && n <= 30) {
        ages.push(n);
      }
    }
    return ages;
  }

  formValid(): boolean {
    const date = this.dateChangementPrevu();
    const dist = this.distanceKm();
    const raison = this.raisonChangement();
    const mode = this.modeResidenceActuel();
    const delai = this.delaiInformationJours();
    const ages = this.ageEnfants();
    return date !== null && ISO_DATE_REGEX.test(date)
      && dist !== null && dist > 0
      && raison !== null
      && mode !== null
      && Number.isInteger(delai) && delai >= 0
      && ages.length > 0;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // Handlers manuels — effacent le badge IA correspondant.
  onDateChangementChange(value: string | null): void {
    this.dateChangementPrevu.set(value && value.length > 0 ? value : null);
  }

  onDistanceChange(value: number | string | null): void {
    if (value === null || value === '' || value === undefined) {
      this.distanceKm.set(null);
      return;
    }
    const n = Number(value);
    this.distanceKm.set(Number.isFinite(n) ? n : null);
  }

  onRaisonChange(value: RaisonChangement | null): void {
    this.raisonChangement.set(value);
    this.provenanceRaisonChangement.set(null);
  }

  onConsentementChange(value: boolean): void {
    this.consentementAutreParent.set(!!value);
    this.provenanceConsentementAutreParent.set(null);
  }

  onInformePrealablementChange(value: boolean): void {
    this.informePrealablement.set(!!value);
    this.provenanceInformePrealablement.set(null);
  }

  onDelaiInformationChange(value: number | string | null): void {
    if (value === null || value === '' || value === undefined) {
      this.delaiInformationJours.set(0);
      return;
    }
    const n = Number(value);
    this.delaiInformationJours.set(Number.isInteger(n) && n >= 0 ? n : 0);
  }

  onModeResidenceChange(value: ModeResidenceCh | null): void {
    this.modeResidenceActuel.set(value);
    this.provenanceModeResidenceActuel.set(null);
  }

  onAgeEnfantsRawChange(value: string): void {
    this.ageEnfantsRaw.set(value ?? '');
    this.ageEnfants.set(this.parseAgeEnfants(value ?? ''));
    this.provenanceAgeEnfants.set(null);
  }

  onScolariteChange(value: boolean): void {
    this.scolariteImpactee.set(!!value);
  }

  onModificationDvhChange(value: boolean): void {
    this.modificationDvhDemandee.set(!!value);
  }

  // ---------------------------------------------------------------------------
  // HTTP : load + calculate
  // ---------------------------------------------------------------------------

  calculate(): void {
    if (!this.formValid()) return;
    const request: ChangementResidenceRequest = {
      dateChangementPrevu: this.dateChangementPrevu()!,
      distanceKm: this.distanceKm()!,
      raisonChangement: this.raisonChangement()!,
      consentementAutreParent: this.consentementAutreParent(),
      informePrealablement: this.informePrealablement(),
      delaiInformationJours: this.delaiInformationJours(),
      modeResidenceActuel: this.modeResidenceActuel()!,
      ageEnfants: this.ageEnfants(),
      scolariteImpactee: this.scolariteImpactee(),
      modificationDvhDemandee: this.modificationDvhDemandee(),
    };
    this.calculating.set(true);
    // F-163 SF-163-02c : en standalone, POST sur le dispatcher générique.

    const request$ = this.standaloneMode

      ? this.service.calculateStandalone(request)

      : this.service.calculate(this.caseFileId, request);

    request$.subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Analyse changement de résidence calculée', 'OK', { duration: 2500 });
        // F-163 SF-163-02c : pas de dashboard à rafraîchir en standalone.

        if (!this.standaloneMode) { this.dashboardRefresh?.triggerRefresh(); }
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

  private applyPersistedResult(r: ChangementResidenceResponse): void {
    this.result.set(r);
    this.dateChangementPrevu.set(r.dateChangementPrevu);
    this.distanceKm.set(r.distanceKm);
    this.raisonChangement.set(r.raisonChangement);
    this.consentementAutreParent.set(r.consentementAutreParent);
    this.informePrealablement.set(r.informePrealablement);
    this.delaiInformationJours.set(r.delaiInformationJours);
    this.modeResidenceActuel.set(r.modeResidenceActuel);
    this.ageEnfants.set(r.ageEnfants ?? []);
    this.ageEnfantsRaw.set((r.ageEnfants ?? []).join(', '));
    this.scolariteImpactee.set(r.scolariteImpactee);
    this.modificationDvhDemandee.set(r.modificationDvhDemandee);
    // Valeurs persistées = saisie avocat — jamais de badge IA.
    this.provenanceRaisonChangement.set(null);
    this.provenanceConsentementAutreParent.set(null);
    this.provenanceInformePrealablement.set(null);
    this.provenanceModeResidenceActuel.set(null);
    this.provenanceAgeEnfants.set(null);
    this.showForm.set(false);
  }

  /**
   * Pré-fill 5 champs IA :
   *  - raisonChangement (depuis `raisonChangementDetectee`)
   *  - consentementAutreParent
   *  - informePrealablement
   *  - modeResidenceActuel
   *  - agesEnfantsDetectes (SF-246-10 : source backend réelle)
   *
   * No-op gracieux si champ absent. N'écrase pas une saisie avocat
   * (heuristique : provenance === null sur un champ rempli ou booléen
   * différent de la valeur par défaut indique modification manuelle).
   */
  private prefillFromAi(): void {
    // F-236 SF-236-02 — délégation au helper partagé.
    const h = { aiData: this.aiDataSignal() };

    const raison = ChangementResidencePrefillRules.computeRaisonChangement(h);
    if (raison !== null
        && (this.raisonChangement() === null || this.provenanceRaisonChangement() === 'IA')) {
      this.raisonChangement.set(raison);
      this.provenanceRaisonChangement.set('IA');
    }

    const cons = ChangementResidencePrefillRules.computeConsentementAutreParent(h);
    if (cons !== null
        && (this.provenanceConsentementAutreParent() === 'IA' || this.consentementAutreParent() === false)) {
      this.consentementAutreParent.set(cons);
      this.provenanceConsentementAutreParent.set('IA');
    }

    const info = ChangementResidencePrefillRules.computeInformePrealablement(h);
    if (info !== null
        && (this.provenanceInformePrealablement() === 'IA' || this.informePrealablement() === false)) {
      this.informePrealablement.set(info);
      this.provenanceInformePrealablement.set('IA');
    }

    const mode = ChangementResidencePrefillRules.computeModeResidenceActuel(h);
    if (mode !== null
        && (this.modeResidenceActuel() === null || this.provenanceModeResidenceActuel() === 'IA')) {
      this.modeResidenceActuel.set(mode);
      this.provenanceModeResidenceActuel.set('IA');
    }

    const ages = ChangementResidencePrefillRules.computeAgesEnfants(h);
    if (ages.length > 0
        && (this.ageEnfants().length === 0 || this.provenanceAgeEnfants() === 'IA')) {
      this.ageEnfants.set(ages);
      this.ageEnfantsRaw.set(ages.join(', '));
      this.provenanceAgeEnfants.set('IA');
    }
  }

  // ---------------------------------------------------------------------------
  // Builders d'alertes F-IA-03 (un par field) — via CoherenceAlertBuilder.
  // ---------------------------------------------------------------------------

  /**
   * Raison du changement — divergence stricte sur l'enum 5 valeurs.
   * Sources : IA + F96 + QUESTION_IA + PIECE_MANQUANTE.
   */
  private buildRaisonAlert(): ChangementResidenceCoherenceAlert | null {
    const ai = this.aiDataSignal();
    const aiRaw = ai?.raisonChangementDetectee;
    if (typeof aiRaw !== 'string' || aiRaw.length === 0) return null;
    const aiVal = aiRaw.toUpperCase();
    if (!VALID_RAISONS.has(aiVal)) return null;
    const userVal = this.raisonChangement();
    if (userVal === null) return null;
    if (userVal === aiVal) return null;

    const display = raisonChangementLabel(aiVal as RaisonChangement);
    const builder = CoherenceAlertBuilder.forField<ChangementResidenceAlertField>('RAISON_CHANGEMENT')
      .addSource('IA', {
        expectedDisplay: display,
        reason: `Analyse du dossier : raison du changement "${display}"`,
      });

    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'FA19_RAISON_CHANGEMENT') continue;
      if (!chk.expectedValue) continue;
      builder.addSource('F96', {
        expectedDisplay: display,
        reason: `Checklist procédurale : raison attendue "${chk.expectedValue}"${chk.raison ? ' (' + chk.raison + ')' : ''}`,
      });
      break;
    }

    for (const q of this.aiQuestionsSignal()) {
      if (q.critereCode?.toUpperCase() !== 'FA19_RAISON_CHANGEMENT') continue;
      if (!q.answerText) continue;
      builder.addSource('QUESTION_IA', {
        expectedDisplay: display,
        reason: `Question complémentaire : "${q.questionText}" — réponse "${q.answerText}"`,
      });
      break;
    }

    const piece = this.findPieceManquante(['FA19_RAISON_CHANGEMENT', 'JUSTIFICATIF_DEMENAGEMENT']);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  /**
   * Consentement autre parent — divergence stricte sur boolean (asymétrie
   * conservatrice : alerte uniquement si IA dit `false` et user a coché `true`,
   * surévaluation du consentement par l'avocat).
   */
  private buildConsentementAlert(): ChangementResidenceCoherenceAlert | null {
    const ai = this.aiDataSignal();
    if (typeof ai?.consentementAutreParent !== 'boolean') return null;
    if (this.consentementAutreParent() === ai.consentementAutreParent) return null;

    const display = ai.consentementAutreParent
      ? 'Consentement détecté'
      : 'Pas de consentement détecté';
    const builder = CoherenceAlertBuilder.forField<ChangementResidenceAlertField>('CONSENTEMENT_AUTRE_PARENT')
      .addSource('IA', {
        expectedDisplay: display,
        reason: `Analyse du dossier : ${display.toLowerCase()}`,
      });

    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'FA19_CONSENTEMENT_AUTRE_PARENT') continue;
      if (!chk.expectedValue) continue;
      builder.addSource('F96', {
        expectedDisplay: display,
        reason: `Checklist procédurale : ${chk.expectedValue}${chk.raison ? ' (' + chk.raison + ')' : ''}`,
      });
      break;
    }

    const piece = this.findPieceManquante(['FA19_CONSENTEMENT_AUTRE_PARENT', 'ACCORD_PARENTAL']);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  /**
   * Informé préalablement — alerte si IA détecte que l'autre parent a été
   * informé (`true`) et l'avocat a coché `false`, OU l'inverse (sous-déclaration
   * critique pour l'art. 373-2 Cciv qui impose l'obligation).
   */
  private buildInformePrealablementAlert(): ChangementResidenceCoherenceAlert | null {
    const ai = this.aiDataSignal();
    if (typeof ai?.informePrealablement !== 'boolean') return null;
    if (this.informePrealablement() === ai.informePrealablement) return null;

    const display = ai.informePrealablement
      ? 'Information préalable détectée (art. 373-2)'
      : 'Aucune information préalable détectée';
    const builder = CoherenceAlertBuilder.forField<ChangementResidenceAlertField>('INFORME_PREALABLEMENT')
      .addSource('IA', {
        expectedDisplay: display,
        reason: `Analyse du dossier : ${display.toLowerCase()}`,
      });

    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'FA19_INFORME_PREALABLEMENT') continue;
      if (!chk.expectedValue) continue;
      builder.addSource('F96', {
        expectedDisplay: display,
        reason: `Checklist procédurale : ${chk.expectedValue}${chk.raison ? ' (' + chk.raison + ')' : ''}`,
      });
      break;
    }

    const piece = this.findPieceManquante([
      'FA19_INFORME_PREALABLEMENT',
      'COURRIER_INFORMATION',
      'EMAIL_INFORMATION',
    ]);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  /**
   * Mode résidence actuel — divergence stricte sur l'enum 3 valeurs.
   */
  private buildModeResidenceAlert(): ChangementResidenceCoherenceAlert | null {
    const ai = this.aiDataSignal();
    const aiRaw = ai?.modeResidenceActuel;
    if (typeof aiRaw !== 'string' || aiRaw.length === 0) return null;
    const aiVal = aiRaw.toUpperCase();
    if (!VALID_MODES.has(aiVal)) return null;
    const userVal = this.modeResidenceActuel();
    if (userVal === null) return null;
    if (userVal === aiVal) return null;

    const display = modeResidenceLabel(aiVal as ModeResidenceCh);
    const builder = CoherenceAlertBuilder.forField<ChangementResidenceAlertField>('MODE_RESIDENCE_ACTUEL')
      .addSource('IA', {
        expectedDisplay: display,
        reason: `Analyse du dossier : mode de résidence actuel "${display}"`,
      });

    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'FA19_MODE_RESIDENCE_ACTUEL') continue;
      if (!chk.expectedValue) continue;
      builder.addSource('F96', {
        expectedDisplay: display,
        reason: `Checklist procédurale : mode attendu "${chk.expectedValue}"${chk.raison ? ' (' + chk.raison + ')' : ''}`,
      });
      break;
    }

    const piece = this.findPieceManquante([
      'FA19_MODE_RESIDENCE_ACTUEL',
      'JUGEMENT_RESIDENCE',
      'CONVENTION_PARENTALE',
    ]);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  /**
   * Âges enfants — divergence ensembliste IA vs user (au moins une valeur
   * différente OU nombre d'enfants différent).
   */
  private buildAgeEnfantsAlert(): ChangementResidenceCoherenceAlert | null {
    const ai = this.aiDataSignal();
    const aiAges = ai?.agesEnfantsDetectes;
    if (!Array.isArray(aiAges) || aiAges.length === 0) return null;
    const userAges = this.ageEnfants();
    if (userAges.length === 0) return null;

    const aiSorted = [...aiAges].filter((n): n is number => typeof n === 'number').sort((a, b) => a - b);
    const userSorted = [...userAges].sort((a, b) => a - b);

    const same = aiSorted.length === userSorted.length
      && aiSorted.every((v, i) => v === userSorted[i]);
    if (same) return null;

    const display = aiSorted.join(', ') + ' an(s)';
    const builder = CoherenceAlertBuilder.forField<ChangementResidenceAlertField>('AGE_ENFANTS')
      .addSource('IA', {
        expectedDisplay: display,
        reason: `Analyse du dossier : âges des enfants ${display}`,
      });

    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'FA19_AGE_ENFANTS') continue;
      if (!chk.expectedValue) continue;
      builder.addSource('F96', {
        expectedDisplay: display,
        reason: `Checklist procédurale : âges attendus ${chk.expectedValue}${chk.raison ? ' (' + chk.raison + ')' : ''}`,
      });
      break;
    }

    const piece = this.findPieceManquante(['FA19_AGE_ENFANTS', 'ACTE_NAISSANCE_ENFANT', 'LIVRET_FAMILLE']);
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

  raisonChangementLabel = raisonChangementLabel;
  modeResidenceLabel = modeResidenceLabel;
  verdictLabel = verdictChangementResidenceLabel;

  verdictBannerClass(verdict: ChangementResidenceVerdict): string {
    switch (verdict) {
      case 'ELEVEE': return 'cr-verdict-banner cr-verdict-banner--strong';
      case 'MOYENNE': return 'cr-verdict-banner cr-verdict-banner--medium';
      case 'FAIBLE': return 'cr-verdict-banner cr-verdict-banner--weak';
    }
  }
}
