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
import { MatChipsModule } from '@angular/material/chips';
import { MatSnackBar } from '@angular/material/snack-bar';
import { OrdonnanceProtectionService } from '../../core/services/ordonnance-protection.service';
import {
  MesureCode,
  MESURES_FR,
  MesureOption,
  OrdonnanceProtectionResponse,
  PreuveCode,
  PREUVES_FR,
  PreuveOption,
  VerdictProbabiliteOctroi,
  ViolenceCode,
  VIOLENCES_FR,
  ViolenceOption,
  mesureLabel,
  preuveLabel,
  verdictProbabiliteOctroiLabel,
  violenceLabel,
} from '../../core/models/ordonnance-protection.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
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
import { SourceExplanation } from '../../core/models/source-explanation.model';
import { SourceExplanationService } from '../../core/services/source-explanation.service';

/**
 * SF-FA-14-02 : champs d'alerte F-IA-03 exposés par l'outil F-FA-14
 * (ordonnance de protection, FR uniquement, art. 515-9 Cciv).
 */
export type OrdonnanceProtectionAlertField =
  | 'DATE_REQUETE'
  | 'VIOLENCES_ALLEGUEES'
  | 'LOGEMENT_COMMUN';

export type OrdonnanceProtectionAlertSource = CoherenceAlertSource;
export type OrdonnanceProtectionCoherenceAlert =
  CoherenceAlert<OrdonnanceProtectionAlertField>;

import {
  ISO_DATE_REGEX,
  OrdonnanceProtectionPrefillRules,
  VALID_PREUVE_CODES,
  VALID_VIOLENCE_CODES,
} from './ordonnance-protection-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-FA-14-02 : outil décisionnel "Ordonnance de protection" (FR uniquement,
 * art. 515-9 à 515-13 Cciv + Loi 30/07/2020 BAR).
 *
 * Affiché conditionnellement par le panel F-IA-04 (tool_id
 * `F-FA-14-ordonnance-protection`). Consomme l'API SF-FA-14-01.
 *
 * Pattern de référence canonique :
 * `harcelement-licenciement-nul-section` (cf. `ai-skills/frontend-coherence-audit.md` §5).
 * Miroir le plus proche (multi-select enum + scoring famille FR) : `divorce-faute-section`.
 * Pré-fill IA + F-IA-03 multi-sources : `divorce-accepte-section` (F-FA-10).
 *
 * Palette : navy/or/rouge **classique** (verdict ELEVEE → strong navy =
 * succès procédural pour la victime, FAIBLE → rouge = échec procédural).
 * On ne bascule **pas** en gradation rouge dominante (--danger-medium/-strong
 * /-dark) car la sémantique métier "score élevé = bon" justifie le navy
 * en strong (cf. SF-155-07 / DIV-8). Le délai 6 j JAF reste signalé par le
 * message contextuel — l'urgence procédurale ≠ urgence palette dominante
 * réservée au < 72h (F-IM-08 SF-04 OQTF JLD 48h).
 */
@Component({
  selector: 'app-ordonnance-protection-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatSlideToggleModule, MatChipsModule,
    MatProgressSpinnerModule,
    LegalCitationsPipe,
    ToolJurisprudenceCitationsComponent,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './ordonnance-protection-section.component.html',
  styleUrl: './ordonnance-protection-section.component.scss',
})
export class OrdonnanceProtectionSectionComponent implements OnInit, OnChanges {
  // F-JU-03 SF-JU-03-99e v4 — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-FA-14-ordonnance-protection';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'ORDONNANCE DE PROTECTION (FR) — ART. 515-9 CCIV';
  static readonly TOOL_ICON = 'shield';

  /** F-236 SF-236-02 — compteur miroir prefillFromAi via helper. */
  static getPrefillCount(input: {
    aiData?: FamilleExtractedData | null;
    procedureChecks?: unknown[];
    aiQuestions?: unknown[];
    piecesManquantes?: unknown[];
    triggerEvents?: unknown[];
    workspaceCountry?: string;
  }): number {
    return OrdonnanceProtectionPrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';
  // SF-FA-14-02 : pré-fill IA (tous optionnels — null-safe partout).
  @Input() aiData?: FamilleExtractedData | null;
  // Inputs multi-sources F-IA-03 (F96 / QUESTION_IA / PIECE_MANQUANTE).
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;

  // Snapshots pour que les `computed` réagissent aux changements d'input.
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
   *  - `calculate()` POSTe sur `/api/v1/simulators/F-FA-14-ordonnance-protection/calculate`.
   *  - `triggerRefresh()` jamais invoqué.
   *
   * Default `false` — mode case-file scoped inchangé.
   */
  @Input() standaloneMode: boolean = false;
  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<OrdonnanceProtectionResponse | null>(null);

  // Form state.
  dateRequete = signal<string | null>(null);
  violencesAlleguees = signal<ViolenceCode[]>([]);
  preuvesViolences = signal<PreuveCode[]>([]);
  dangerImmediat = signal<boolean>(false);
  presenceEnfants = signal<boolean>(false);
  logementCommun = signal<boolean>(false);
  victimeFinanciairementDependante = signal<boolean>(false);
  demandeurDejaProtege = signal<boolean>(false);
  demandeMesures = signal<MesureCode[]>([]);

  // Provenance IA par champ.
  provenanceDateRequete = signal<'IA' | null>(null);
  provenanceViolencesAlleguees = signal<'IA' | null>(null);
  provenancePreuvesViolences = signal<'IA' | null>(null);
  provenanceDangerImmediat = signal<'IA' | null>(null);
  provenancePresenceEnfants = signal<'IA' | null>(null);
  provenanceLogementCommun = signal<'IA' | null>(null);
  provenanceVictimeFinanciairementDependante = signal<'IA' | null>(null);
  provenanceDemandeurDejaProtege = signal<'IA' | null>(null);

  // SF-IA-03-15c : map {sourceKey → explanations} pour le popover F-IA-03.
  sourceExplanations = signal<Map<string, SourceExplanation[]>>(new Map());

  readonly violencesOptions: ViolenceOption[] = VIOLENCES_FR;
  readonly preuvesOptions: PreuveOption[] = PREUVES_FR;
  readonly mesuresOptions: MesureOption[] = MESURES_FR;

  // Alertes calculées dynamiquement via le builder partagé.
  // Gate strict `showForm()` (pattern anti-bug SF-IA-03-12).
  coherenceAlerts = computed<Partial<Record<OrdonnanceProtectionAlertField, OrdonnanceProtectionCoherenceAlert>>>(() => {
    // F-163 SF-163-02c : aucune source IA en standalone.
    if (this.standaloneMode) return {};
    if (!this.showForm()) return {};
    const alerts: Partial<Record<OrdonnanceProtectionAlertField, OrdonnanceProtectionCoherenceAlert>> = {};
    const date = this.buildDateRequeteAlert();
    if (date) alerts.DATE_REQUETE = date;
    const violences = this.buildViolencesAlleguees();
    if (violences) alerts.VIOLENCES_ALLEGUEES = violences;
    const logement = this.buildLogementCommunAlert();
    if (logement) alerts.LOGEMENT_COMMUN = logement;
    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    return { total: values.length, blockers: 0 };
  });

  isFrance = computed(() => this.workspaceCountry === 'FRANCE');

  constructor(
    private service: OrdonnanceProtectionService,
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

    if (changes['aiData'] && !changes['aiData'].firstChange && this.showForm() && !this.result() && !this.standaloneMode) {
      this.prefillFromAi();
    }
  }

  /**
   * SF-FA-14-02 : pré-remplit le form depuis `aiData`. N'écrase jamais une
   * saisie avocat existante (la garde est faite par champ — provenance IA OK
   * ou champ encore vide / valeur par défaut).
   */
  private prefillFromAi(): void {
    // F-236 SF-236-02 — délégation au helper partagé.
    const helperInput = { aiData: this.aiDataSignal() };

    // 1. Date requête (ISO YYYY-MM-DD).
    const date = OrdonnanceProtectionPrefillRules.computeDateRequete(helperInput);
    if (date !== null
        && (this.dateRequete() === null || this.provenanceDateRequete() === 'IA')) {
      this.dateRequete.set(date);
      this.provenanceDateRequete.set('IA');
    }

    // 2. Violences alléguées (codes filtrés).
    const violences = OrdonnanceProtectionPrefillRules.computeViolencesAllegueesCodes(helperInput);
    if (violences.length > 0
        && (this.violencesAlleguees().length === 0 || this.provenanceViolencesAlleguees() === 'IA')) {
      this.violencesAlleguees.set(violences);
      this.provenanceViolencesAlleguees.set('IA');
    }

    // 3. Preuves violences (codes filtrés).
    const preuves = OrdonnanceProtectionPrefillRules.computePreuvesViolencesCodes(helperInput);
    if (preuves.length > 0
        && (this.preuvesViolences().length === 0 || this.provenancePreuvesViolences() === 'IA')) {
      this.preuvesViolences.set(preuves);
      this.provenancePreuvesViolences.set('IA');
    }
    const ai = this.aiDataSignal();
    if (!ai) return;

    // 4. Booléens — provenance IA seulement si la valeur diffère de la valeur
    // par défaut (false). Sinon, on ne pose pas de provenance pour ne pas
    // saturer le badge "Pré-rempli".
    if (typeof ai.dangerImmediatDetected === 'boolean'
        && (this.provenanceDangerImmediat() === 'IA' || this.dangerImmediat() === false)) {
      this.dangerImmediat.set(ai.dangerImmediatDetected);
      if (ai.dangerImmediatDetected) this.provenanceDangerImmediat.set('IA');
    }
    if (typeof ai.presenceEnfantsDetected === 'boolean'
        && (this.provenancePresenceEnfants() === 'IA' || this.presenceEnfants() === false)) {
      this.presenceEnfants.set(ai.presenceEnfantsDetected);
      if (ai.presenceEnfantsDetected) this.provenancePresenceEnfants.set('IA');
    }
    if (typeof ai.logementCommunDetected === 'boolean'
        && (this.provenanceLogementCommun() === 'IA' || this.logementCommun() === false)) {
      this.logementCommun.set(ai.logementCommunDetected);
      if (ai.logementCommunDetected) this.provenanceLogementCommun.set('IA');
    }
    if (typeof ai.victimeFinanciairementDependanteDetected === 'boolean'
        && (this.provenanceVictimeFinanciairementDependante() === 'IA'
            || this.victimeFinanciairementDependante() === false)) {
      this.victimeFinanciairementDependante.set(ai.victimeFinanciairementDependanteDetected);
      if (ai.victimeFinanciairementDependanteDetected) {
        this.provenanceVictimeFinanciairementDependante.set('IA');
      }
    }
    if (typeof ai.demandeurDejaProtegeDetected === 'boolean'
        && (this.provenanceDemandeurDejaProtege() === 'IA'
            || this.demandeurDejaProtege() === false)) {
      this.demandeurDejaProtege.set(ai.demandeurDejaProtegeDetected);
      if (ai.demandeurDejaProtegeDetected) {
        this.provenanceDemandeurDejaProtege.set('IA');
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

  /**
   * SF-IA-03-15c : mapping field → sourceKey pour le popover F-IA-03.
   */
  explanationFor(field: OrdonnanceProtectionAlertField): SourceExplanation[] {
    const key = (() => {
      switch (field) {
        case 'DATE_REQUETE': return 'FA14_DATE_REQUETE';
        case 'VIOLENCES_ALLEGUEES': return 'FA14_VIOLENCES_ALLEGUEES';
        case 'LOGEMENT_COMMUN': return 'FA14_LOGEMENT_COMMUN';
      }
    })();
    return this.sourceExplanations().get(key) ?? [];
  }

  alertTooltip(alert: OrdonnanceProtectionCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: OrdonnanceProtectionCoherenceAlert): string {
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
  // Builders d'alertes F-IA-03 (un par field) — via CoherenceAlertBuilder.
  // ---------------------------------------------------------------------------

  /**
   * Date requête : divergence stricte ISO entre IA et user.
   * Multi-sources : F96 + PIECE_MANQUANTE.
   */
  private buildDateRequeteAlert(): OrdonnanceProtectionCoherenceAlert | null {
    const ai = this.aiDataSignal();
    const aiDate = ai?.dateRequeteOP;
    const user = this.dateRequete();
    if (!user) return null;
    if (!aiDate || !ISO_DATE_REGEX.test(aiDate)) return null;
    if (user === aiDate) return null;

    const builder = CoherenceAlertBuilder.forField<OrdonnanceProtectionAlertField>('DATE_REQUETE')
      .addSource('IA', {
        expectedDisplay: aiDate,
        reason: `Analyse du dossier : requête datée ${aiDate}`,
      });

    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'FA14_DATE_REQUETE') continue;
      if (!chk.expectedValue) continue;
      builder.addSource('F96', {
        expectedDisplay: aiDate,
        reason: `Checklist procédurale : date attendue ${chk.expectedValue}${chk.raison ? ' (' + chk.raison + ')' : ''}`,
      });
      break;
    }

    const piece = this.findPieceManquante(['FA14_DATE_REQUETE', 'COPIE_REQUETE_OP']);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  /**
   * Violences alléguées : divergence ensembliste IA vs user.
   * Multi-sources : F96 + PIECE_MANQUANTE.
   */
  private buildViolencesAlleguees(): OrdonnanceProtectionCoherenceAlert | null {
    const ai = this.aiDataSignal();
    const aiList = ai?.violencesAllegueesDetectees;
    if (!Array.isArray(aiList) || aiList.length === 0) return null;
    const aiSet = new Set(aiList.map((v) => v?.toUpperCase()).filter((v): v is string => !!v));
    if (aiSet.size === 0) return null;
    const userSet = new Set(this.violencesAlleguees());

    let display: string;
    let iaReason: string;
    if (userSet.size === 0) {
      const sample = Array.from(aiSet).slice(0, 3).join(', ');
      display = sample;
      iaReason = `Analyse du dossier : violences détectées (${sample})`;
    } else {
      const same = aiSet.size === userSet.size
        && Array.from(aiSet).every((v) => userSet.has(v as ViolenceCode));
      if (same) return null;
      const sample = Array.from(aiSet).slice(0, 3).join(', ');
      display = sample;
      iaReason = `Analyse du dossier : violences détectées (${sample}) — divergence avec votre saisie`;
    }

    const builder = CoherenceAlertBuilder.forField<OrdonnanceProtectionAlertField>('VIOLENCES_ALLEGUEES')
      .addSource('IA', { expectedDisplay: display, reason: iaReason });

    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'FA14_VIOLENCES_ALLEGUEES') continue;
      if (!chk.expectedValue) continue;
      builder.addSource('F96', {
        expectedDisplay: display,
        reason: `Checklist procédurale : violences attendues ${chk.expectedValue}${chk.raison ? ' (' + chk.raison + ')' : ''}`,
      });
      break;
    }

    const piece = this.findPieceManquante([
      'FA14_VIOLENCES_ALLEGUEES',
      'CERTIFICAT_MEDICAL',
      'CONSTAT_HUISSIER',
      'TEMOIGNAGES',
    ]);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  /**
   * Logement commun : divergence boolean IA vs user.
   * Multi-sources : F96 + PIECE_MANQUANTE.
   */
  private buildLogementCommunAlert(): OrdonnanceProtectionCoherenceAlert | null {
    const ai = this.aiDataSignal();
    const aiVal = ai?.logementCommunDetected;
    if (typeof aiVal !== 'boolean') return null;
    const user = this.logementCommun();
    if (user === aiVal) return null;

    const display = aiVal ? 'Oui' : 'Non';
    const builder = CoherenceAlertBuilder.forField<OrdonnanceProtectionAlertField>('LOGEMENT_COMMUN')
      .addSource('IA', {
        expectedDisplay: display,
        reason: `Analyse du dossier : logement commun ${display.toLowerCase()}`,
      });

    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'FA14_LOGEMENT_COMMUN') continue;
      if (!chk.expectedValue) continue;
      builder.addSource('F96', {
        expectedDisplay: display,
        reason: `Checklist procédurale : logement commun ${chk.expectedValue}${chk.raison ? ' (' + chk.raison + ')' : ''}`,
      });
      break;
    }

    const piece = this.findPieceManquante(['FA14_LOGEMENT_COMMUN', 'JUSTIFICATIF_DOMICILE']);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  /**
   * Renvoie le `texte` d'une `PieceManquanteEntry` dont `critereCode`
   * (case-insensitive) appartient à la liste des codes acceptés, ou `null`
   * si aucune pièce ne matche.
   */
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
  // Form handlers
  // ---------------------------------------------------------------------------

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  formValid(): boolean {
    return this.violencesAlleguees().length > 0
      && this.demandeMesures().length > 0;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  onDateRequeteChange(value: string | null): void {
    this.dateRequete.set(value && value.length > 0 ? value : null);
    this.provenanceDateRequete.set(null);
  }

  onViolencesChange(value: ViolenceCode[]): void {
    this.violencesAlleguees.set(value ?? []);
    this.provenanceViolencesAlleguees.set(null);
  }

  onPreuvesChange(value: PreuveCode[]): void {
    this.preuvesViolences.set(value ?? []);
    this.provenancePreuvesViolences.set(null);
  }

  onDangerImmediatChange(value: boolean): void {
    this.dangerImmediat.set(!!value);
    this.provenanceDangerImmediat.set(null);
  }

  onPresenceEnfantsChange(value: boolean): void {
    this.presenceEnfants.set(!!value);
    this.provenancePresenceEnfants.set(null);
  }

  onLogementCommunChange(value: boolean): void {
    this.logementCommun.set(!!value);
    this.provenanceLogementCommun.set(null);
  }

  onVictimeDependanteChange(value: boolean): void {
    this.victimeFinanciairementDependante.set(!!value);
    this.provenanceVictimeFinanciairementDependante.set(null);
  }

  onDemandeurDejaProtegeChange(value: boolean): void {
    this.demandeurDejaProtege.set(!!value);
    this.provenanceDemandeurDejaProtege.set(null);
  }

  onMesuresChange(value: MesureCode[]): void {
    this.demandeMesures.set(value ?? []);
  }

  // ---------------------------------------------------------------------------
  // Display helpers
  // ---------------------------------------------------------------------------

  violenceLabel = violenceLabel;
  preuveLabel = preuveLabel;
  mesureLabel = mesureLabel;
  verdictProbabiliteOctroiLabel = verdictProbabiliteOctroiLabel;

  /**
   * Palette navy/or/rouge classique. Justification (cf. SF-155-07 / DIV-8) :
   * la sémantique métier est "score élevé = procédure réussie" → verdict
   * ELEVEE = navy strong (succès), MOYENNE = or (intermédiaire), FAIBLE
   * = rouge (échec). On NE bascule PAS en gradation `--danger-medium/-strong
   * /-dark` (réservée à urgence absolue < 72h, ex. F-IM-08 SF-04 OQTF JLD 48h).
   */
  verdictBannerClass(verdict: VerdictProbabiliteOctroi): string {
    switch (verdict) {
      case 'ELEVEE': return 'op-verdict-banner op-verdict-banner--strong';
      case 'MOYENNE': return 'op-verdict-banner op-verdict-banner--medium';
      case 'FAIBLE': return 'op-verdict-banner op-verdict-banner--weak';
    }
  }

  // ---------------------------------------------------------------------------
  // HTTP
  // ---------------------------------------------------------------------------

  calculate(): void {
    if (!this.formValid()) return;
    const request = {
      dateRequete: this.dateRequete(),
      violencesAlleguees: this.violencesAlleguees(),
      preuvesViolences: this.preuvesViolences(),
      dangerImmediat: this.dangerImmediat(),
      presenceEnfants: this.presenceEnfants(),
      ageEnfants: null,
      logementCommun: this.logementCommun(),
      victimeFinanciairementDependante: this.victimeFinanciairementDependante(),
      demandeurDejaProtege: this.demandeurDejaProtege(),
      demandeMesures: this.demandeMesures(),
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
        this.snackBar.open('Analyse ordonnance de protection calculée', 'OK', { duration: 2500 });
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

  private applyPersistedResult(r: OrdonnanceProtectionResponse): void {
    this.result.set(r);
    this.dateRequete.set(r.dateRequete);
    this.violencesAlleguees.set(r.violencesAlleguees ?? []);
    this.preuvesViolences.set(r.preuvesViolences ?? []);
    this.dangerImmediat.set(!!r.dangerImmediat);
    this.presenceEnfants.set(!!r.presenceEnfants);
    this.logementCommun.set(!!r.logementCommun);
    this.victimeFinanciairementDependante.set(!!r.victimeFinanciairementDependante);
    this.demandeurDejaProtege.set(!!r.demandeurDejaProtege);
    this.demandeMesures.set(r.demandeMesures ?? []);
    // Valeurs persistées = saisie avocat — jamais de badge IA.
    this.provenanceDateRequete.set(null);
    this.provenanceViolencesAlleguees.set(null);
    this.provenancePreuvesViolences.set(null);
    this.provenanceDangerImmediat.set(null);
    this.provenancePresenceEnfants.set(null);
    this.provenanceLogementCommun.set(null);
    this.provenanceVictimeFinanciairementDependante.set(null);
    this.provenanceDemandeurDejaProtege.set(null);
    this.showForm.set(false);
  }
}
