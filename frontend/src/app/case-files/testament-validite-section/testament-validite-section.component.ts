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
import { MatRadioModule } from '@angular/material/radio';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { TestamentValiditeService } from '../../core/services/testament-validite.service';
import {
  CodeVice,
  CODE_VICE_LABELS,
  FormeTestament,
  FORME_TESTAMENT_LABELS,
  TestamentValiditeRequest,
  TestamentValiditeResponse,
  VerdictValidite,
  VERDICT_VALIDITE_LABELS,
  ViceIdentifie,
} from '../../core/models/testament-validite.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { PieceManquanteEntry } from '../../core/models/case-analysis.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { AiQuestion } from '../../core/models/ai-question.model';
import { CoherencePopoverTriggerDirective } from '../../shared/coherence-popover/coherence-popover-trigger.directive';
import {
  CoherenceAlert,
  CoherenceAlertSource,
} from '../../shared/coherence-popover/coherence-alert.model';
import { CoherenceAlertBuilder } from '../../shared/coherence-popover/coherence-alert-builder';
import { TestamentValiditePrefillRules } from './testament-validite-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-FA-24-04 : champs d'alerte F-IA-03 exposés par l'outil "Validité testament".
 * - FORME : divergence sur la forme du testament détectée par l'IA.
 * - SAINE_ESPRIT : divergence sur la capacité du testateur (art. 901 Cciv).
 * - LEGS_EXCEDE_QUOTITE : divergence sur le dépassement de la quotité
 *   disponible (art. 913+) — déclenche l'action en réduction (art. 920+).
 */
export type TestamentValiditeAlertField =
  | 'FORME'
  | 'SAINE_ESPRIT'
  | 'LEGS_EXCEDE_QUOTITE';

export type TestamentValiditeAlertSource = CoherenceAlertSource;
export type TestamentValiditeCoherenceAlert =
  CoherenceAlert<TestamentValiditeAlertField>;

/**
 * SF-FA-24-04 : outil décisionnel "Validité testament" (FR — art. 967-1035
 * + 901-911 + 920+ Cciv : 4 formes + capacité + vices consentement +
 * révocation + quotité disponible).
 *
 * FR uniquement (bannière info si dossier BE — équivalent CC BE art. 895+
 * traité dans la feature jumelle backlog F-FA-24-BE-testament).
 *
 * Consomme l'API SF-FA-24-03 (mergée PR #661). Affiché conditionnellement
 * par le panel F-IA-04 (tool_id 'F-FA-24-testament-validite' — migration 182).
 *
 * Pattern de référence : `devolution-legale-section` (PR #658, F-FA-24 jumeau).
 * Helper partagé : `CoherenceAlertBuilder` + `CoherenceAlert<F>` (SF-155-05).
 */
@Component({
  selector: 'app-testament-validite-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule, DecimalPipe,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatRadioModule, MatCheckboxModule,
    MatProgressSpinnerModule,
    LegalCitationsPipe,
    ToolJurisprudenceCitationsComponent,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './testament-validite-section.component.html',
  styleUrl: './testament-validite-section.component.scss',
})
export class TestamentValiditeSectionComponent implements OnInit, OnChanges {
  // F-JU-03 SF-JU-03-99d — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-FA-24-testament-validite';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'VALIDITÉ TESTAMENT (FR)';
  static readonly TOOL_ICON = 'history_edu';

  /** F-236 SF-236-02 — compteur miroir prefillFromAi via helper. */
  static getPrefillCount(input: {
    aiData?: FamilleExtractedData | null;
    procedureChecks?: unknown[];
    aiQuestions?: unknown[];
    piecesManquantes?: unknown[];
    triggerEvents?: unknown[];
    workspaceCountry?: string;
  }): number {
    return TestamentValiditePrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';
  @Input() aiData?: FamilleExtractedData | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;

  private aiDataSignal = signal<FamilleExtractedData | null | undefined>(undefined);
  private procedureChecksSignal = signal<ProcedureCheck[]>([]);
  private aiQuestionsSignal = signal<AiQuestion[]>([]);
  private piecesManquantesSignal = signal<PieceManquanteEntry[]>([]);

  // F-177 SF-177-03b : force l'expansion (mode modal F-177).
  @Input() forceExpanded = false;

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<TestamentValiditeResponse | null>(null);

  // Form fields — communs.
  formeTestament = signal<FormeTestament | null>(null);
  dateRedaction = signal<string | null>(null);
  ageTestateurAnsRedaction = signal<number | null>(null);
  saineDEsprit = signal<boolean | null>(null);
  majeurProtegeAvecAssistance = signal<boolean | null>(null);

  // Olographe.
  ecritureManuscritIntegrale = signal<boolean | null>(null);
  dateComplete = signal<boolean | null>(null);
  signatureTestateur = signal<boolean | null>(null);

  // Authentique.
  presenceNotaireEtTemoinsConforme = signal<boolean | null>(null);
  dicteEnPresence = signal<boolean | null>(null);
  lectureFinaleAuTestateur = signal<boolean | null>(null);
  signaturesCompletes = signal<boolean | null>(null);

  // Mystique.
  remiseSousPliCache = signal<boolean | null>(null);
  declarationDevant2Temoins = signal<boolean | null>(null);
  acteSuscriptionNotaire = signal<boolean | null>(null);

  // International.
  respecteFormeWashington = signal<boolean | null>(null);

  // Vices consentement.
  vicesConsentementDol = signal<boolean>(false);
  erreurSubstantielle = signal<boolean>(false);

  // Révocation.
  testamentPosterieurContradictoire = signal<boolean>(false);
  dechirureVolontaireOriginal = signal<boolean>(false);

  // Quotité disponible.
  legsExcedeQuotiteDisponible = signal<boolean>(false);

  /** Provenance IA des champs pré-remplis. */
  provenanceForme = signal<'IA' | null>(null);
  provenanceDateRedaction = signal<'IA' | null>(null);
  provenanceSaineEsprit = signal<'IA' | null>(null);
  provenanceLegsExcedeQuotite = signal<'IA' | null>(null);

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  isOlographe = computed<boolean>(() => this.formeTestament() === 'TESTAMENT_OLOGRAPHE');
  isAuthentique = computed<boolean>(() => this.formeTestament() === 'TESTAMENT_AUTHENTIQUE');
  isMystique = computed<boolean>(() => this.formeTestament() === 'TESTAMENT_MYSTIQUE');
  isInternational = computed<boolean>(() => this.formeTestament() === 'TESTAMENT_INTERNATIONAL');

  coherenceAlerts = computed<Partial<Record<TestamentValiditeAlertField, TestamentValiditeCoherenceAlert>>>(() => {
    if (!this.showForm()) return {};
    const alerts: Partial<Record<TestamentValiditeAlertField, TestamentValiditeCoherenceAlert>> = {};
    const forme = this.buildFormeAlert();
    if (forme) alerts.FORME = forme;
    const saine = this.buildSaineEspritAlert();
    if (saine) alerts.SAINE_ESPRIT = saine;
    const legs = this.buildLegsExcedeQuotiteAlert();
    if (legs) alerts.LEGS_EXCEDE_QUOTITE = legs;
    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    const blockers = values.filter((a) => a.severity === 'CRITICAL').length;
    return { total: values.length, blockers };
  });

  readonly formeOptions: ReadonlyArray<{ code: FormeTestament; label: string }> = [
    { code: 'TESTAMENT_OLOGRAPHE', label: FORME_TESTAMENT_LABELS.TESTAMENT_OLOGRAPHE },
    { code: 'TESTAMENT_AUTHENTIQUE', label: FORME_TESTAMENT_LABELS.TESTAMENT_AUTHENTIQUE },
    { code: 'TESTAMENT_MYSTIQUE', label: FORME_TESTAMENT_LABELS.TESTAMENT_MYSTIQUE },
    { code: 'TESTAMENT_INTERNATIONAL', label: FORME_TESTAMENT_LABELS.TESTAMENT_INTERNATIONAL },
  ];

  constructor(
    private service: TestamentValiditeService,
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
    if (this.isFrance()) {
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

    if (changes['aiData'] && !changes['aiData'].firstChange
        && this.isFrance() && this.showForm() && !this.result()) {
      this.prefillFromAi();
    }
  }

  alertTooltip(alert: TestamentValiditeCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: TestamentValiditeCoherenceAlert): string {
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

  /** Divergence sur la forme du testament. */
  private buildFormeAlert(): TestamentValiditeCoherenceAlert | null {
    const userVal = this.formeTestament();
    if (userVal === null) return null;

    const builder = CoherenceAlertBuilder.forField<TestamentValiditeAlertField>('FORME');

    // 1. F-96
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'TESTAMENT_FORME') continue;
      const ev = chk.expectedValue;
      if (!ev) continue;
      const expected = this.parseFormeFromIa(ev);
      if (!expected || expected === userVal) continue;
      builder.addSource('F96', {
        expectedDisplay: this.formeLabel(expected),
        reason: `Checklist procédurale : ${this.formeLabel(expected)}`
          + (chk.raison ? ` (${chk.raison})` : ''),
      });
      break;
    }

    // 2. QUESTION_IA
    for (const q of this.aiQuestionsSignal()) {
      if (q.critereCode?.toUpperCase() !== 'TESTAMENT_FORME') continue;
      const answer = q.answerText?.trim();
      if (!answer) continue;
      const expected = this.parseFormeFromIa(q.expectedValue ?? answer);
      if (!expected || expected === userVal) continue;
      builder.addSource('QUESTION_IA', {
        expectedDisplay: this.formeLabel(expected),
        reason: `Question complémentaire : "${q.questionText}" → "${q.answerText}"`,
      });
      break;
    }

    // 3. IA — `aiData.formeTestamentDetectee`.
    const iaForme = this.parseFormeFromIa(this.aiDataSignal()?.formeTestamentDetectee ?? null);
    if (iaForme && iaForme !== userVal) {
      builder.addSource('IA', {
        expectedDisplay: this.formeLabel(iaForme),
        reason: `Analyse du dossier : ${this.formeLabel(iaForme)}`,
      });
    }

    // 4. PIECE_MANQUANTE
    const piece = this.findPieceManquante(['TESTAMENT_FORME', 'TESTAMENT']);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  /** Divergence sur la capacité (art. 901 Cciv). */
  private buildSaineEspritAlert(): TestamentValiditeCoherenceAlert | null {
    const userVal = this.saineDEsprit();
    if (userVal === null) return null;

    const builder = CoherenceAlertBuilder.forField<TestamentValiditeAlertField>('SAINE_ESPRIT');

    // 1. F-96
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'TESTAMENT_SAINE_ESPRIT') continue;
      const expected = this.parseBoolFromIa(chk.expectedValue);
      if (expected === null || expected === userVal) continue;
      builder.addSource('F96', {
        expectedDisplay: expected ? "Sain d'esprit" : "Insanité d'esprit",
        reason: `Checklist procédurale : ${expected ? "testateur sain d'esprit" : "insanité d'esprit détectée"}`
          + (chk.raison ? ` (${chk.raison})` : ''),
      });
      break;
    }

    // 2. QUESTION_IA
    for (const q of this.aiQuestionsSignal()) {
      if (q.critereCode?.toUpperCase() !== 'TESTAMENT_SAINE_ESPRIT') continue;
      const answer = q.answerText?.trim().toLowerCase();
      if (!answer) continue;
      const expected = this.parseBoolFromIa(q.expectedValue);
      if (expected === null || expected === userVal) continue;
      builder.addSource('QUESTION_IA', {
        expectedDisplay: expected ? "Sain d'esprit" : "Insanité d'esprit",
        reason: `Question complémentaire : "${q.questionText}" → "${q.answerText}"`,
      });
      break;
    }

    // 3. IA — `aiData.saineDEspritTestateurDetected`.
    const iaSaine = this.aiDataSignal()?.saineDEspritTestateurDetected;
    if (iaSaine !== null && iaSaine !== undefined && iaSaine !== userVal) {
      builder.addSource('IA', {
        expectedDisplay: iaSaine ? "Sain d'esprit" : "Insanité d'esprit",
        reason: `Analyse du dossier : ${iaSaine ? "testateur sain d'esprit" : "insanité d'esprit détectée"}`,
      });
    }

    // 4. PIECE_MANQUANTE
    const piece = this.findPieceManquante(['TESTAMENT_SAINE_ESPRIT', 'CERTIFICAT_MEDICAL']);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  /** Divergence sur le dépassement de quotité disponible. */
  private buildLegsExcedeQuotiteAlert(): TestamentValiditeCoherenceAlert | null {
    const userVal = this.legsExcedeQuotiteDisponible();

    const builder = CoherenceAlertBuilder.forField<TestamentValiditeAlertField>('LEGS_EXCEDE_QUOTITE');

    // 1. F-96
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'TESTAMENT_QUOTITE') continue;
      const expected = this.parseBoolFromIa(chk.expectedValue);
      if (expected === null || expected === userVal) continue;
      builder.addSource('F96', {
        expectedDisplay: expected ? 'Legs > quotité disponible' : 'Legs dans la quotité',
        reason: `Checklist procédurale : ${expected ? 'legs excède la quotité disponible' : 'legs respecte la quotité'}`
          + (chk.raison ? ` (${chk.raison})` : ''),
      });
      break;
    }

    // 2. QUESTION_IA
    for (const q of this.aiQuestionsSignal()) {
      if (q.critereCode?.toUpperCase() !== 'TESTAMENT_QUOTITE') continue;
      const expected = this.parseBoolFromIa(q.expectedValue);
      if (expected === null || expected === userVal) continue;
      builder.addSource('QUESTION_IA', {
        expectedDisplay: expected ? 'Legs > quotité disponible' : 'Legs dans la quotité',
        reason: `Question complémentaire : "${q.questionText}" → "${q.answerText}"`,
      });
      break;
    }

    // 3. IA — `aiData.legsExcedeQuotiteDisponibleDetected`.
    const iaLegs = this.aiDataSignal()?.legsExcedeQuotiteDisponibleDetected;
    if (iaLegs !== null && iaLegs !== undefined && iaLegs !== userVal) {
      builder.addSource('IA', {
        expectedDisplay: iaLegs ? 'Legs > quotité disponible' : 'Legs dans la quotité',
        reason: `Analyse du dossier : ${iaLegs ? 'legs excède la quotité disponible' : 'legs respecte la quotité'}`,
      });
    }

    return builder.build();
  }

  private parseBoolFromIa(value: string | null | undefined): boolean | null {
    if (value === null || value === undefined) return null;
    const v = value.toString().trim().toLowerCase();
    if (!v) return null;
    if (v === 'oui' || v === 'true' || v === 'vrai' || v === '1' || v === 'yes') return true;
    if (v === 'non' || v === 'false' || v === 'faux' || v === '0' || v === 'no') return false;
    return null;
  }

  /**
   * Parse la forme du testament depuis une valeur IA / F-96.
   * Accepte les valeurs canoniques (`TESTAMENT_OLOGRAPHE` etc.) ou alias
   * courts (`OLOGRAPHE`, `AUTHENTIQUE`, `MYSTIQUE`, `INTERNATIONAL`).
   */
  private parseFormeFromIa(value: string | null | undefined): FormeTestament | null {
    if (!value) return null;
    const v = value.toString().trim().toUpperCase();
    if (!v) return null;
    if (v === 'TESTAMENT_OLOGRAPHE' || v === 'OLOGRAPHE') return 'TESTAMENT_OLOGRAPHE';
    if (v === 'TESTAMENT_AUTHENTIQUE' || v === 'AUTHENTIQUE') return 'TESTAMENT_AUTHENTIQUE';
    if (v === 'TESTAMENT_MYSTIQUE' || v === 'MYSTIQUE') return 'TESTAMENT_MYSTIQUE';
    if (v === 'TESTAMENT_INTERNATIONAL' || v === 'INTERNATIONAL') return 'TESTAMENT_INTERNATIONAL';
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

  toggleCollapse(): void {
    this.collapsed.update((v) => !v);
  }

  formValid(): boolean {
    if (this.formeTestament() === null) return false;
    if (!this.dateRedaction()) return false;
    const age = this.ageTestateurAnsRedaction();
    if (age === null || age === undefined || age < 0 || age > 130) return false;
    if (this.saineDEsprit() === null) return false;

    // Champs conditionnels requis selon la forme.
    if (this.isOlographe()) {
      if (this.ecritureManuscritIntegrale() === null) return false;
      if (this.dateComplete() === null) return false;
      if (this.signatureTestateur() === null) return false;
    }
    if (this.isAuthentique()) {
      if (this.presenceNotaireEtTemoinsConforme() === null) return false;
      if (this.dicteEnPresence() === null) return false;
      if (this.lectureFinaleAuTestateur() === null) return false;
      if (this.signaturesCompletes() === null) return false;
    }
    if (this.isMystique()) {
      if (this.remiseSousPliCache() === null) return false;
      if (this.declarationDevant2Temoins() === null) return false;
      if (this.acteSuscriptionNotaire() === null) return false;
    }
    if (this.isInternational()) {
      if (this.respecteFormeWashington() === null) return false;
      if (this.signaturesCompletes() === null) return false;
    }
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // Handlers — effacent provenance IA au 1er changement manuel.

  onFormeTestamentChange(value: FormeTestament | null): void {
    this.formeTestament.set(value);
    this.provenanceForme.set(null);
    // Reset des champs spécifiques aux formes non sélectionnées.
    if (value !== 'TESTAMENT_OLOGRAPHE') {
      this.ecritureManuscritIntegrale.set(null);
      this.dateComplete.set(null);
      this.signatureTestateur.set(null);
    }
    if (value !== 'TESTAMENT_AUTHENTIQUE') {
      this.presenceNotaireEtTemoinsConforme.set(null);
      this.dicteEnPresence.set(null);
      this.lectureFinaleAuTestateur.set(null);
    }
    if (value !== 'TESTAMENT_AUTHENTIQUE' && value !== 'TESTAMENT_INTERNATIONAL') {
      this.signaturesCompletes.set(null);
    }
    if (value !== 'TESTAMENT_MYSTIQUE') {
      this.remiseSousPliCache.set(null);
      this.declarationDevant2Temoins.set(null);
      this.acteSuscriptionNotaire.set(null);
    }
    if (value !== 'TESTAMENT_INTERNATIONAL') {
      this.respecteFormeWashington.set(null);
    }
  }

  onDateRedactionChange(value: string | null): void {
    this.dateRedaction.set(value || null);
    this.provenanceDateRedaction.set(null);
  }

  onAgeTestateurChange(value: number | null): void {
    this.ageTestateurAnsRedaction.set(value === null || value === undefined ? null : value);
  }

  onSaineDEspritChange(value: boolean | null): void {
    this.saineDEsprit.set(value);
    this.provenanceSaineEsprit.set(null);
  }

  onMajeurProtegeChange(value: boolean | null): void {
    this.majeurProtegeAvecAssistance.set(value);
  }

  onEcritureManuscritIntegraleChange(value: boolean | null): void {
    this.ecritureManuscritIntegrale.set(value);
  }

  onDateCompleteChange(value: boolean | null): void {
    this.dateComplete.set(value);
  }

  onSignatureTestateurChange(value: boolean | null): void {
    this.signatureTestateur.set(value);
  }

  onPresenceNotaireEtTemoinsConformeChange(value: boolean | null): void {
    this.presenceNotaireEtTemoinsConforme.set(value);
  }

  onDicteEnPresenceChange(value: boolean | null): void {
    this.dicteEnPresence.set(value);
  }

  onLectureFinaleAuTestateurChange(value: boolean | null): void {
    this.lectureFinaleAuTestateur.set(value);
  }

  onSignaturesCompletesChange(value: boolean | null): void {
    this.signaturesCompletes.set(value);
  }

  onRemiseSousPliCacheChange(value: boolean | null): void {
    this.remiseSousPliCache.set(value);
  }

  onDeclarationDevant2TemoinsChange(value: boolean | null): void {
    this.declarationDevant2Temoins.set(value);
  }

  onActeSuscriptionNotaireChange(value: boolean | null): void {
    this.acteSuscriptionNotaire.set(value);
  }

  onRespecteFormeWashingtonChange(value: boolean | null): void {
    this.respecteFormeWashington.set(value);
  }

  onVicesConsentementDolChange(value: boolean): void {
    this.vicesConsentementDol.set(value);
  }

  onErreurSubstantielleChange(value: boolean): void {
    this.erreurSubstantielle.set(value);
  }

  onTestamentPosterieurContradictoireChange(value: boolean): void {
    this.testamentPosterieurContradictoire.set(value);
  }

  onDechirureVolontaireOriginalChange(value: boolean): void {
    this.dechirureVolontaireOriginal.set(value);
  }

  onLegsExcedeQuotiteDisponibleChange(value: boolean): void {
    this.legsExcedeQuotiteDisponible.set(value);
    this.provenanceLegsExcedeQuotite.set(null);
  }

  /**
   * Pré-fill depuis `aiData` (FamilleExtractedData).
   * Règles :
   * - silencieux si aiData absent
   * - ne pré-remplit que si le champ est encore vide ou marqué IA
   * - n'écrase jamais une saisie avocat (provenance !== 'IA')
   */
  private prefillFromAi(): void {
    // F-236 SF-236-02 — délégation au helper partagé.
    const h = { aiData: this.aiDataSignal() };

    const forme = TestamentValiditePrefillRules.computeFormeTestament(h);
    if (forme !== null
        && (this.formeTestament() === null || this.provenanceForme() === 'IA')) {
      this.formeTestament.set(forme);
      this.provenanceForme.set('IA');
    }

    const date = TestamentValiditePrefillRules.computeDateRedaction(h);
    if (date !== null
        && (!this.dateRedaction() || this.provenanceDateRedaction() === 'IA')) {
      this.dateRedaction.set(date);
      this.provenanceDateRedaction.set('IA');
    }

    const saine = TestamentValiditePrefillRules.computeSaineDEsprit(h);
    if (saine !== null
        && (this.saineDEsprit() === null || this.provenanceSaineEsprit() === 'IA')) {
      this.saineDEsprit.set(saine);
      this.provenanceSaineEsprit.set('IA');
    }

    const legs = TestamentValiditePrefillRules.computeLegsExcedeQuotite(h);
    if (legs !== null
        && (this.provenanceLegsExcedeQuotite() === 'IA'
            || this.legsExcedeQuotiteDisponible() === false)) {
      this.legsExcedeQuotiteDisponible.set(legs);
      this.provenanceLegsExcedeQuotite.set('IA');
    }
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request: TestamentValiditeRequest = {
      formeTestament: this.formeTestament()!,
      dateRedaction: this.dateRedaction()!,
      ageTestateurAnsRedaction: this.ageTestateurAnsRedaction()!,
      saineDEsprit: this.saineDEsprit()!,
      majeurProtegeAvecAssistance: this.majeurProtegeAvecAssistance(),
      ecritureManuscritIntegrale: this.ecritureManuscritIntegrale(),
      dateComplete: this.dateComplete(),
      signatureTestateur: this.signatureTestateur(),
      presenceNotaireEtTemoinsConforme: this.presenceNotaireEtTemoinsConforme(),
      dicteEnPresence: this.dicteEnPresence(),
      lectureFinaleAuTestateur: this.lectureFinaleAuTestateur(),
      signaturesCompletes: this.signaturesCompletes(),
      remiseSousPliCache: this.remiseSousPliCache(),
      declarationDevant2Temoins: this.declarationDevant2Temoins(),
      acteSuscriptionNotaire: this.acteSuscriptionNotaire(),
      respecteFormeWashington: this.respecteFormeWashington(),
      vicesConsentementDol: this.vicesConsentementDol(),
      erreurSubstantielle: this.erreurSubstantielle(),
      testamentPosterieurContradictoire: this.testamentPosterieurContradictoire(),
      dechirureVolontaireOriginal: this.dechirureVolontaireOriginal(),
      legsExcedeQuotiteDisponible: this.legsExcedeQuotiteDisponible(),
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Validité testament analysée', 'OK', { duration: 2500 });
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
        this.result.set(r);
        // Provenance reset — valeurs persistées = saisie avocat.
        this.provenanceForme.set(null);
        this.provenanceDateRedaction.set(null);
        this.provenanceSaineEsprit.set(null);
        this.provenanceLegsExcedeQuotite.set(null);
        this.showForm.set(false);
        this.loading.set(false);
      },
      error: () => {
        // 404 attendu si aucune analyse — on reste en mode formulaire.
        this.prefillFromAi();
        this.loading.set(false);
      },
    });
  }

  /** Libellé humain de la forme. */
  formeLabel(code: FormeTestament | null | undefined): string {
    if (!code) return '';
    return FORME_TESTAMENT_LABELS[code] ?? code;
  }

  /** Libellé humain du verdict. */
  verdictLabel(code: VerdictValidite | null | undefined): string {
    if (!code) return '';
    return VERDICT_VALIDITE_LABELS[code] ?? code;
  }

  /** Libellé humain d'un code de vice. */
  viceLabel(code: CodeVice): string {
    return CODE_VICE_LABELS[code] ?? code;
  }

  /**
   * Classe CSS du bandeau verdict :
   * - `NUL` → critical (rouge), seul cas réservé à la palette rouge.
   * - `CONTESTABLE` → warn (or).
   * - `VALIDE` → info (navy).
   */
  verdictBannerClass(code: VerdictValidite | null | undefined): string {
    if (!code) return 'testament-banner testament-banner--info';
    if (code === 'NUL') return 'testament-banner testament-banner--critical';
    if (code === 'CONTESTABLE') return 'testament-banner testament-banner--warn';
    return 'testament-banner testament-banner--info';
  }

  /** Classe CSS du chip header (header collapsé). */
  verdictChipClass(code: VerdictValidite | null | undefined): string {
    if (!code) return 'testament-chip testament-chip--info';
    if (code === 'NUL') return 'testament-chip testament-chip--critical';
    if (code === 'CONTESTABLE') return 'testament-chip testament-chip--warn';
    return 'testament-chip testament-chip--info';
  }

  /** Icône du bandeau verdict. */
  verdictIcon(code: VerdictValidite | null | undefined): string {
    if (code === 'NUL') return 'gpp_bad';
    if (code === 'CONTESTABLE') return 'warning';
    return 'verified';
  }

  /** TrackBy pour la liste des vices. */
  trackVice(_i: number, v: ViceIdentifie): string {
    return v.code;
  }
}
