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
import { AutoriteParentaleService } from '../../core/services/autorite-parentale.service';
import {
  AutoriteParentaleRequest,
  AutoriteParentaleResponse,
  AutoriteParentaleVerdict,
  MOTIFS_CHANGEMENT_FR,
  MotifChangementAutorite,
  PREUVES_AUTORITE_FR,
  PreuveAutorite,
  REGIMES_EXERCICE_FR,
  RegimeExercice,
  motifChangementLabel,
  preuveAutoriteLabel,
  regimeExerciceLabel,
  verdictAutoriteParentaleLabel,
} from '../../core/models/autorite-parentale.model';
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
import { AutoriteParentalePrefillRules } from './autorite-parentale-section-prefill-rules';
import { SourceExplanation } from '../../core/models/source-explanation.model';
import { SourceExplanationService } from '../../core/services/source-explanation.service';

/**
 * SF-FA-19-02 : fields F-IA-03 audités par l'outil F-FA-19 (autorité parentale).
 * Un par champ pré-remplissable depuis l'IA `FamilleExtractedData`.
 */
export type AutoriteParentaleAlertField =
  | 'REGIME_EXERCICE_ACTUEL'
  | 'DANGER_CARACTERISE'
  | 'CONSENTEMENT_AUTRE_PARENT'
  | 'INTERFERENCE_VIE_ENFANT'
  | 'AGE_ENFANTS';

export type AutoriteParentaleAlertSource = CoherenceAlertSource;
export type AutoriteParentaleCoherenceAlert = CoherenceAlert<AutoriteParentaleAlertField>;

const ISO_DATE_REGEX = /^\d{4}-\d{2}-\d{2}$/;

const VALID_REGIMES: ReadonlySet<string> = new Set<RegimeExercice>([
  'CONJOINT',
  'EXCLUSIF_MERE',
  'EXCLUSIF_PERE',
  'DELEGATION_TIERS',
]);

/**
 * SF-FA-19-02 : composant Angular standalone pour l'outil "Autorité parentale —
 * exercice" (F-FA-19, FRANCE uniquement, art. 372-373 / 373-2-10 Cciv).
 *
 * Pattern de référence : `harcelement-licenciement-nul-section` (template
 * canonique 2026-04-24, cf. `ai-skills/frontend-coherence-audit.md` §5).
 * Miroir famille : `divorce-accepte-section` (FamilleExtractedData + builder
 * F-IA-03 + 5 fields).
 *
 * - Form : 2 mat-select RegimeExercice + 1 mat-select MotifChangement +
 *   3 slide-toggles + 1 mat-select multiple Preuves + ages CSV +
 *   `<input type="date">` requête.
 * - Verdict 3 paliers ELEVEE/MOYENNE/FAIBLE → palette navy/or/rouge classique.
 * - Pré-fill IA via `aiData?: FamilleExtractedData | null` (5 fields ouverts,
 *   no-op gracieux si absent).
 * - Validation F-IA-03 sur 5 fields via `CoherenceAlertBuilder` (multi-sources
 *   IA + F96 + QUESTION_IA + PIECE_MANQUANTE) + popover.
 * - Refresh dashboard F-IA-02 + `MatSnackBar` pour erreurs.
 */
@Component({
  selector: 'app-autorite-parentale-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatSlideToggleModule, MatChipsModule,
    MatProgressSpinnerModule,
    LegalCitationsPipe,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './autorite-parentale-section.component.html',
  styleUrl: './autorite-parentale-section.component.scss',
})
export class AutoriteParentaleSectionComponent implements OnInit, OnChanges {
  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'AUTORITÉ PARENTALE — EXERCICE (FR) — ART. 372-373 CCIV';
  static readonly TOOL_ICON = 'family_restroom';

  /** F-236 SF-236-02 — compteur miroir prefillFromAi via helper. */
  static getPrefillCount(input: {
    aiData?: FamilleExtractedData | null;
    procedureChecks?: unknown[];
    aiQuestions?: unknown[];
    piecesManquantes?: unknown[];
    triggerEvents?: unknown[];
    workspaceCountry?: string;
  }): number {
    return AutoriteParentalePrefillRules.computePrefillCount(input);
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
   *  - `calculate()` POSTe sur `/api/v1/simulators/F-FA-19-autorite-parentale/calculate`.
   *  - `triggerRefresh()` jamais invoqué.
   *
   * Default `false` — mode case-file scoped inchangé.
   */
  @Input() standaloneMode: boolean = false;
  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<AutoriteParentaleResponse | null>(null);

  // Form fields.
  regimeExerciceActuel = signal<RegimeExercice | null>(null);
  regimeExerciceDemande = signal<RegimeExercice | null>(null);
  motifChangement = signal<MotifChangementAutorite | null>(null);
  dangerCaracterise = signal<boolean>(false);
  preuvesProduites = signal<PreuveAutorite[]>([]);
  /** Saisi via input texte CSV (parsé). */
  ageEnfantsRaw = signal<string>('');
  ageEnfants = signal<number[]>([]);
  consentementAutreParent = signal<boolean>(false);
  interferenceVieEnfant = signal<boolean>(false);
  dateRequete = signal<string | null>(null);

  // Provenance IA par champ pré-remplissable (5 fields).
  provenanceRegimeActuel = signal<'IA' | null>(null);
  provenanceDangerCaracterise = signal<'IA' | null>(null);
  provenanceConsentementAutreParent = signal<'IA' | null>(null);
  provenanceInterferenceVieEnfant = signal<'IA' | null>(null);
  provenanceAgeEnfants = signal<'IA' | null>(null);

  // Map {sourceKey → explanations} pour le popover F-IA-03-15c (fail-open).
  sourceExplanations = signal<Map<string, SourceExplanation[]>>(new Map());

  readonly regimesOptions = REGIMES_EXERCICE_FR;
  readonly motifsOptions = MOTIFS_CHANGEMENT_FR;
  readonly preuvesOptions = PREUVES_AUTORITE_FR;

  /**
   * Coherence alerts F-IA-03 — gate `showForm()` strict (pattern anti-bug
   * SF-IA-03-12 : pas de `|| result()`).
   */
  coherenceAlerts = computed<Partial<Record<AutoriteParentaleAlertField, AutoriteParentaleCoherenceAlert>>>(() => {
    // F-163 SF-163-02c : aucune source IA en standalone.
    if (this.standaloneMode) return {};
    if (!this.showForm()) return {};
    const alerts: Partial<Record<AutoriteParentaleAlertField, AutoriteParentaleCoherenceAlert>> = {};
    const regimeAlert = this.buildRegimeActuelAlert();
    if (regimeAlert) alerts.REGIME_EXERCICE_ACTUEL = regimeAlert;
    const dangerAlert = this.buildDangerAlert();
    if (dangerAlert) alerts.DANGER_CARACTERISE = dangerAlert;
    const consentementAlert = this.buildConsentementAlert();
    if (consentementAlert) alerts.CONSENTEMENT_AUTRE_PARENT = consentementAlert;
    const interferenceAlert = this.buildInterferenceAlert();
    if (interferenceAlert) alerts.INTERFERENCE_VIE_ENFANT = interferenceAlert;
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
    private service: AutoriteParentaleService,
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
  explanationFor(field: AutoriteParentaleAlertField): SourceExplanation[] {
    const key = (() => {
      switch (field) {
        case 'REGIME_EXERCICE_ACTUEL': return 'FA19_REGIME_EXERCICE_ACTUEL';
        case 'DANGER_CARACTERISE': return 'FA19_DANGER_CARACTERISE';
        case 'CONSENTEMENT_AUTRE_PARENT': return 'FA19_CONSENTEMENT_AUTRE_PARENT';
        case 'INTERFERENCE_VIE_ENFANT': return 'FA19_INTERFERENCE_VIE_ENFANT';
        case 'AGE_ENFANTS': return 'FA19_AGE_ENFANTS';
      }
    })();
    return this.sourceExplanations().get(key) ?? [];
  }

  alertTooltip(alert: AutoriteParentaleCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: AutoriteParentaleCoherenceAlert): string {
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
    const regAct = this.regimeExerciceActuel();
    const regDem = this.regimeExerciceDemande();
    const motif = this.motifChangement();
    const ages = this.ageEnfants();
    const date = this.dateRequete();
    return regAct !== null
      && regDem !== null
      && regAct !== regDem
      && motif !== null
      && ages.length > 0
      && date !== null && ISO_DATE_REGEX.test(date);
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // Handlers manuels — effacent le badge IA correspondant.
  onRegimeActuelChange(value: RegimeExercice | null): void {
    this.regimeExerciceActuel.set(value);
    this.provenanceRegimeActuel.set(null);
  }

  onRegimeDemandeChange(value: RegimeExercice | null): void {
    this.regimeExerciceDemande.set(value);
  }

  onMotifChange(value: MotifChangementAutorite | null): void {
    this.motifChangement.set(value);
  }

  onDangerChange(value: boolean): void {
    this.dangerCaracterise.set(!!value);
    this.provenanceDangerCaracterise.set(null);
  }

  onPreuvesChange(value: PreuveAutorite[]): void {
    this.preuvesProduites.set(value ?? []);
  }

  onAgeEnfantsRawChange(value: string): void {
    this.ageEnfantsRaw.set(value ?? '');
    this.ageEnfants.set(this.parseAgeEnfants(value ?? ''));
    this.provenanceAgeEnfants.set(null);
  }

  onConsentementChange(value: boolean): void {
    this.consentementAutreParent.set(!!value);
    this.provenanceConsentementAutreParent.set(null);
  }

  onInterferenceChange(value: boolean): void {
    this.interferenceVieEnfant.set(!!value);
    this.provenanceInterferenceVieEnfant.set(null);
  }

  onDateRequeteChange(value: string | null): void {
    this.dateRequete.set(value && value.length > 0 ? value : null);
  }

  // ---------------------------------------------------------------------------
  // HTTP : load + calculate
  // ---------------------------------------------------------------------------

  calculate(): void {
    if (!this.formValid()) return;
    const request: AutoriteParentaleRequest = {
      regimeExerciceActuel: this.regimeExerciceActuel()!,
      regimeExerciceDemande: this.regimeExerciceDemande()!,
      motifChangement: this.motifChangement()!,
      dangerCaracterise: this.dangerCaracterise(),
      preuvesProduites: this.preuvesProduites(),
      ageEnfants: this.ageEnfants(),
      consentementAutreParent: this.consentementAutreParent(),
      interferenceVieEnfant: this.interferenceVieEnfant(),
      dateRequete: this.dateRequete()!,
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
        this.snackBar.open('Analyse autorité parentale calculée', 'OK', { duration: 2500 });
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

  private applyPersistedResult(r: AutoriteParentaleResponse): void {
    this.result.set(r);
    this.regimeExerciceActuel.set(r.regimeExerciceActuel);
    this.regimeExerciceDemande.set(r.regimeExerciceDemande);
    this.motifChangement.set(r.motifChangement);
    this.dangerCaracterise.set(r.dangerCaracterise);
    this.preuvesProduites.set(r.preuvesProduites ?? []);
    this.ageEnfants.set(r.ageEnfants ?? []);
    this.ageEnfantsRaw.set((r.ageEnfants ?? []).join(', '));
    this.consentementAutreParent.set(r.consentementAutreParent);
    this.interferenceVieEnfant.set(r.interferenceVieEnfant);
    this.dateRequete.set(r.dateRequete);
    // Valeurs persistées = saisie avocat — jamais de badge IA.
    this.provenanceRegimeActuel.set(null);
    this.provenanceDangerCaracterise.set(null);
    this.provenanceConsentementAutreParent.set(null);
    this.provenanceInterferenceVieEnfant.set(null);
    this.provenanceAgeEnfants.set(null);
    this.showForm.set(false);
  }

  /**
   * Pré-fill 5 champs IA :
   *  - regimeExerciceActuel
   *  - dangerCaracterise
   *  - consentementAutreParent
   *  - interferenceVieEnfant
   *  - ageEnfants
   *
   * No-op gracieux si champ absent. N'écrase pas une saisie avocat
   * (heuristique : provenance === null sur un champ rempli ou booléen
   * différent de la valeur par défaut indique modification manuelle).
   */
  private prefillFromAi(): void {
    // F-236 SF-236-02 — délégation au helper partagé.
    const h = { aiData: this.aiDataSignal() };

    const regAi = AutoriteParentalePrefillRules.computeRegimeActuel(h);
    if (regAi !== null
        && (this.regimeExerciceActuel() === null || this.provenanceRegimeActuel() === 'IA')) {
      this.regimeExerciceActuel.set(regAi);
      this.provenanceRegimeActuel.set('IA');
    }

    const danger = AutoriteParentalePrefillRules.computeDangerCaracterise(h);
    if (danger !== null
        && (this.provenanceDangerCaracterise() === 'IA' || this.dangerCaracterise() === false)) {
      this.dangerCaracterise.set(danger);
      this.provenanceDangerCaracterise.set('IA');
    }

    const cons = AutoriteParentalePrefillRules.computeConsentementAutreParent(h);
    if (cons !== null
        && (this.provenanceConsentementAutreParent() === 'IA' || this.consentementAutreParent() === false)) {
      this.consentementAutreParent.set(cons);
      this.provenanceConsentementAutreParent.set('IA');
    }

    const inter = AutoriteParentalePrefillRules.computeInterferenceVieEnfant(h);
    if (inter !== null
        && (this.provenanceInterferenceVieEnfant() === 'IA' || this.interferenceVieEnfant() === false)) {
      this.interferenceVieEnfant.set(inter);
      this.provenanceInterferenceVieEnfant.set('IA');
    }

    const ages = AutoriteParentalePrefillRules.computeAgeEnfants(h);
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
   * Régime exercice actuel — divergence stricte sur l'enum 4 valeurs.
   * Sources : IA + F96 + QUESTION_IA + PIECE_MANQUANTE.
   */
  private buildRegimeActuelAlert(): AutoriteParentaleCoherenceAlert | null {
    const ai = this.aiDataSignal();
    const aiRegRaw = ai?.regimeExerciceActuel;
    if (typeof aiRegRaw !== 'string' || aiRegRaw.length === 0) return null;
    const aiReg = aiRegRaw.toUpperCase();
    if (!VALID_REGIMES.has(aiReg)) return null;
    const userReg = this.regimeExerciceActuel();
    if (userReg === null) return null;
    if (userReg === aiReg) return null;

    const display = regimeExerciceLabel(aiReg as RegimeExercice);
    const builder = CoherenceAlertBuilder.forField<AutoriteParentaleAlertField>('REGIME_EXERCICE_ACTUEL')
      .addSource('IA', {
        expectedDisplay: display,
        reason: `Analyse du dossier : régime actuel "${display}"`,
      });

    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'FA19_REGIME_EXERCICE_ACTUEL') continue;
      if (!chk.expectedValue) continue;
      builder.addSource('F96', {
        expectedDisplay: display,
        reason: `Checklist procédurale : régime attendu "${chk.expectedValue}"${chk.raison ? ' (' + chk.raison + ')' : ''}`,
      });
      break;
    }

    const piece = this.findPieceManquante(['FA19_REGIME_EXERCICE_ACTUEL', 'JUGEMENT_AUTORITE_PARENTALE']);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  /**
   * Danger caractérisé — alerte si IA dit `true` et user a coché `false`
   * (sens critique : sous-évaluation du risque par l'avocat).
   */
  private buildDangerAlert(): AutoriteParentaleCoherenceAlert | null {
    const ai = this.aiDataSignal();
    if (typeof ai?.dangerCaracterise !== 'boolean') return null;
    if (ai.dangerCaracterise === false) return null; // pas d'alerte si IA neutre
    if (this.dangerCaracterise() === true) return null; // pas de divergence

    const display = 'Danger caractérisé détecté';
    const builder = CoherenceAlertBuilder.forField<AutoriteParentaleAlertField>('DANGER_CARACTERISE')
      .addSource('IA', {
        expectedDisplay: display,
        reason: 'Analyse du dossier : danger caractérisé pour l\'enfant détecté',
      });

    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'FA19_DANGER_CARACTERISE') continue;
      if (!chk.expectedValue) continue;
      builder.addSource('F96', {
        expectedDisplay: display,
        reason: `Checklist procédurale : ${chk.expectedValue}${chk.raison ? ' (' + chk.raison + ')' : ''}`,
      });
      break;
    }

    const piece = this.findPieceManquante(['FA19_DANGER_CARACTERISE', 'CERTIFICAT_MEDICAL', 'MAINS_COURANTES']);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  /**
   * Consentement autre parent — alerte si IA dit `false` et user a coché `true`
   * (sens critique : surévaluation du consentement).
   */
  private buildConsentementAlert(): AutoriteParentaleCoherenceAlert | null {
    const ai = this.aiDataSignal();
    if (typeof ai?.consentementAutreParent !== 'boolean') return null;
    if (this.consentementAutreParent() === ai.consentementAutreParent) return null;

    const display = ai.consentementAutreParent
      ? 'Consentement détecté'
      : 'Pas de consentement détecté';
    const builder = CoherenceAlertBuilder.forField<AutoriteParentaleAlertField>('CONSENTEMENT_AUTRE_PARENT')
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
   * Interférence vie enfant — alerte si IA détecte une interférence et
   * l'avocat a coché `false`.
   */
  private buildInterferenceAlert(): AutoriteParentaleCoherenceAlert | null {
    const ai = this.aiDataSignal();
    if (typeof ai?.interferenceVieEnfant !== 'boolean') return null;
    if (ai.interferenceVieEnfant === false) return null;
    if (this.interferenceVieEnfant() === true) return null;

    const display = 'Interférence parentale détectée';
    const builder = CoherenceAlertBuilder.forField<AutoriteParentaleAlertField>('INTERFERENCE_VIE_ENFANT')
      .addSource('IA', {
        expectedDisplay: display,
        reason: 'Analyse du dossier : interférence d\'un parent dans la vie de l\'enfant détectée',
      });

    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'FA19_INTERFERENCE_VIE_ENFANT') continue;
      if (!chk.expectedValue) continue;
      builder.addSource('F96', {
        expectedDisplay: display,
        reason: `Checklist procédurale : ${chk.expectedValue}${chk.raison ? ' (' + chk.raison + ')' : ''}`,
      });
      break;
    }

    const piece = this.findPieceManquante([
      'FA19_INTERFERENCE_VIE_ENFANT',
      'TEMOIGNAGES_PROCHES',
      'TEMOIGNAGES_ECOLE',
    ]);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  /**
   * Âges enfants — divergence ensembliste IA vs user (au moins une valeur
   * différente OU nombre d'enfants différent).
   */
  private buildAgeEnfantsAlert(): AutoriteParentaleCoherenceAlert | null {
    const ai = this.aiDataSignal();
    const aiAges = ai?.ageEnfants;
    if (!Array.isArray(aiAges) || aiAges.length === 0) return null;
    const userAges = this.ageEnfants();
    if (userAges.length === 0) return null;

    const aiSorted = [...aiAges].filter((n): n is number => typeof n === 'number').sort((a, b) => a - b);
    const userSorted = [...userAges].sort((a, b) => a - b);

    const same = aiSorted.length === userSorted.length
      && aiSorted.every((v, i) => v === userSorted[i]);
    if (same) return null;

    const display = aiSorted.join(', ') + ' an(s)';
    const builder = CoherenceAlertBuilder.forField<AutoriteParentaleAlertField>('AGE_ENFANTS')
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

  regimeExerciceLabel = regimeExerciceLabel;
  motifChangementLabel = motifChangementLabel;
  preuveAutoriteLabel = preuveAutoriteLabel;
  verdictLabel = verdictAutoriteParentaleLabel;

  verdictBannerClass(verdict: AutoriteParentaleVerdict): string {
    switch (verdict) {
      case 'ELEVEE': return 'ap-verdict-banner ap-verdict-banner--strong';
      case 'MOYENNE': return 'ap-verdict-banner ap-verdict-banner--medium';
      case 'FAIBLE': return 'ap-verdict-banner ap-verdict-banner--weak';
    }
  }

  /** Audition enfants ≥ 13 ans : porté par le backend, indicatif côté UI. */
  hasEnfantSuffisammentAge(ages: number[] | null | undefined): boolean {
    if (!Array.isArray(ages)) return false;
    return ages.some((a) => typeof a === 'number' && a >= 13);
  }
}
