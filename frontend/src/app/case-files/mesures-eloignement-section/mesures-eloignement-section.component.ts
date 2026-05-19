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
import { MatRadioModule } from '@angular/material/radio';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MesuresEloignementService } from '../../core/services/mesures-eloignement.service';
import {
  DISPOSITIF_ELOIGNEMENT_LABELS,
  DispositifEloignementCode,
  MOTIF_MENACE_LABELS,
  MesuresEloignementRequest,
  MesuresEloignementResponse,
  MotifMenaceCode,
  VerdictLegaliteEloignement,
  dispositifLabel,
  formatDelaiRecours,
  mapDispositifFromIa,
  mapMotifMenaceFromIa,
  motifMenaceLabel,
} from '../../core/models/mesures-eloignement.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import {
  ImmigrationExtractedData,
  PieceManquanteEntry,
} from '../../core/models/case-analysis.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { AiQuestion } from '../../core/models/ai-question.model';
import { CoherencePopoverTriggerDirective } from '../../shared/coherence-popover/coherence-popover-trigger.directive';
import {
  CoherenceAlert,
  CoherenceAlertSource,
} from '../../shared/coherence-popover/coherence-alert.model';
import { CoherenceAlertBuilder } from '../../shared/coherence-popover/coherence-alert-builder';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { MesuresEloignementPrefillRules } from './mesures-eloignement-section-prefill-rules';

/**
 * SF-IM-20-02 : champs F-IA-03 audités par l'outil "Mesures d'éloignement".
 * - DISPOSITIF    : divergence si l'IA détecte un dispositif (typeProcedureDetectee)
 *                   différent de la saisie avocat.
 * - MOTIF_MENACE  : divergence multi-sources sur le motif (F96 / QUESTION_IA / IA).
 */
export type MesuresEloignementAlertField = 'DISPOSITIF' | 'MOTIF_MENACE';

export type MesuresEloignementAlertSource = CoherenceAlertSource;
export type MesuresEloignementCoherenceAlert = CoherenceAlert<MesuresEloignementAlertField>;

/**
 * SF-IM-20-02 : section frontend "Mesures d'éloignement avancées" (F-IM-20).
 * 5 dispositifs CESEDA L.631+ / L.612+ / L.222+ : EXPULSION_PREFECTORALE,
 * EXPULSION_MINISTERIELLE, EXPULSION_SECURITE_ETAT, IRTF, IAT.
 *
 * Single-country FR — Belgique : bannière info (régime CESEDA propre à la
 * France). L'équivalent BE relève de la Loi 15/12/1980 art. 20-21 / 74/15
 * (feature jumelle au backlog F-IM-20-BE).
 *
 * Pattern de référence : `naturalisation-section` (SF-IM-13-02 PR #646
 * — palette navy/or/rouge + builder F-IA-03 + gate FR).
 */
@Component({
  selector: 'app-mesures-eloignement-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatRadioModule, MatSlideToggleModule,
    MatChipsModule, MatProgressSpinnerModule,
    LegalCitationsPipe,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './mesures-eloignement-section.component.html',
  styleUrl: './mesures-eloignement-section.component.scss',
})
export class MesuresEloignementSectionComponent implements OnInit, OnChanges {
  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = "MESURE D'ÉLOIGNEMENT (FR)";
  static readonly TOOL_ICON = 'flight_takeoff';

  /** F-236 SF-236-02 — Délègue intégralement au helper pur partagé. */
  static getPrefillCount(input: PrefillCountInput): number {
    return MesuresEloignementPrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';
  // Sources IA pour pré-fill + alertes de cohérence F-IA-03.
  @Input() aiData?: ImmigrationExtractedData | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;
  // F-177 SF-177-03b : force l'expansion (mode modal F-177).
  @Input() forceExpanded = false;


  /**
   * F-163 SF-163-02d — Mode simulateur autonome (hors dossier client).
   *
   * Quand `true` :
   *  - Bannière « 🧪 Mode simulateur » affichée en haut.
   *  - `prefillFromAi()` court-circuité.
   *  - `coherenceAlerts` retourne `{}`.
   *  - `loadExisting()` / `load()` court-circuité.
   *  - `calculate()` POSTe sur `/api/v1/simulators/F-IM-20-mesures-eloignement/calculate`.
   *  - `triggerRefresh()` jamais invoqué.
   *
   * Default `false` — mode case-file scoped inchangé.
   */
  @Input() standaloneMode: boolean = false;
  // Snapshots signal des inputs IA pour que `computed` réagisse.
  private aiDataSignal = signal<ImmigrationExtractedData | null | undefined>(undefined);
  private procedureChecksSignal = signal<ProcedureCheck[]>([]);
  private aiQuestionsSignal = signal<AiQuestion[]>([]);
  private piecesManquantesSignal = signal<PieceManquanteEntry[]>([]);

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<MesuresEloignementResponse | null>(null);

  // Form fields (signal-based).
  dispositif = signal<DispositifEloignementCode | null>(null);
  motifMenace = signal<MotifMenaceCode | null>(null);
  procedureCommissionRespectee = signal<boolean>(true);
  urgenceAbsolueJustifiee = signal<boolean>(false);
  dureeCircularitePrecaire = signal<number | null>(null);
  dureePresenceIrreguliereMois = signal<number | null>(null);
  comportementAggravant = signal<boolean>(false);
  recoursDelai = signal<string | null>(null);

  /** Provenance IA — badge "Pré-rempli depuis l'analyse" effaçable. */
  provenanceDispositif = signal<'IA' | null>(null);
  provenanceMotif = signal<'IA' | null>(null);
  /** SF-246-19 : provenance IA pour la durée de présence irrégulière. */
  provenanceDureePresenceIrr = signal<'IA' | null>(null);

  /** Listes pour mat-radio. */
  readonly dispositifOptions = DISPOSITIF_ELOIGNEMENT_LABELS;
  readonly motifOptions = MOTIF_MENACE_LABELS;

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  /** Champs conditionnels selon le dispositif sélectionné. */
  showExpulsionFields = computed<boolean>(() => {
    const d = this.dispositif();
    return d === 'EXPULSION_PREFECTORALE'
        || d === 'EXPULSION_MINISTERIELLE'
        || d === 'EXPULSION_SECURITE_ETAT';
  });
  showIrtfFields = computed<boolean>(() => this.dispositif() === 'IRTF');
  showIatFields = computed<boolean>(() => this.dispositif() === 'IAT');

  /** Date max pour le datepicker recoursDelai (+1 an). */
  recoursDelaiMax = computed<string>(() => {
    const d = new Date();
    d.setFullYear(d.getFullYear() + 1);
    return d.toISOString().slice(0, 10);
  });

  /** Alertes de cohérence F-IA-03 par field — gate uniquement en mode formulaire. */
  coherenceAlerts = computed<Partial<Record<MesuresEloignementAlertField, MesuresEloignementCoherenceAlert>>>(() => {
    // F-163 SF-163-02d : aucune source IA en standalone.
    if (this.standaloneMode) return {};
    if (!this.showForm()) return {};
    if (!this.isFrance()) return {};
    const alerts: Partial<Record<MesuresEloignementAlertField, MesuresEloignementCoherenceAlert>> = {};
    const d = this.buildDispositifAlert();
    if (d) alerts.DISPOSITIF = d;
    const m = this.buildMotifAlert();
    if (m) alerts.MOTIF_MENACE = m;
    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    const blockers = values.filter((a) => a.severity === 'CRITICAL').length;
    return { total: values.length, blockers };
  });

  constructor(
    private service: MesuresEloignementService,
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

      // F-163 SF-163-02d : pas de dossier à interroger en standalone.

      this.collapsed.set(false);

      this.loading.set(false);

      this.showForm.set(true);

      return;

    }
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

    // Re-prefill quand aiData arrive après le mount, sans écraser les valeurs avocat.
    if (changes['aiData'] && !changes['aiData'].firstChange
        && this.isFrance() && this.showForm() && !this.result()) {
      this.prefillFromAi();
    }
  }

  toggleCollapse(): void {
    this.collapsed.update((v) => !v);
  }

  /**
   * Form valid si le dispositif et le motif sont saisis. Pour IRTF, les
   * durées numériques saisies doivent être ≥ 0 si non null (validation
   * native HTML5 + check explicit).
   */
  formValid(): boolean {
    const d = this.dispositif();
    const m = this.motifMenace();
    if (!d || !m) return false;
    if (d === 'IRTF') {
      const dp = this.dureePresenceIrreguliereMois();
      const dc = this.dureeCircularitePrecaire();
      if (dp !== null && dp !== undefined && dp < 0) return false;
      if (dc !== null && dc !== undefined && dc < 0) return false;
    }
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // Handlers de modification — effacent la provenance IA.
  onDispositifChange(value: DispositifEloignementCode | null): void {
    this.dispositif.set(value);
    this.provenanceDispositif.set(null);
  }

  onMotifMenaceChange(value: MotifMenaceCode | null): void {
    this.motifMenace.set(value);
    this.provenanceMotif.set(null);
  }

  onProcedureCommissionRespecteeChange(value: boolean): void {
    this.procedureCommissionRespectee.set(value);
  }

  onUrgenceAbsolueJustifieeChange(value: boolean): void {
    this.urgenceAbsolueJustifiee.set(value);
  }

  onDureeCircularitePrecaireChange(value: number | null): void {
    this.dureeCircularitePrecaire.set(value === null || value === undefined ? null : value);
  }

  onDureePresenceIrreguliereChange(value: number | null): void {
    this.dureePresenceIrreguliereMois.set(value === null || value === undefined ? null : value);
    this.provenanceDureePresenceIrr.set(null);
  }

  onComportementAggravantChange(value: boolean): void {
    this.comportementAggravant.set(value);
  }

  onRecoursDelaiChange(value: string | null): void {
    this.recoursDelai.set(value && value.length ? value : null);
  }

  /**
   * Bandeau verdict :
   * - VALIDE      → navy/info, mesure techniquement légale.
   * - CONTESTABLE → or/warning, vice procédure ou motif limite.
   * - NUL         → rouge alerte, mesure sans fondement.
   */
  bannerClass(verdict: VerdictLegaliteEloignement | string | undefined | null): string {
    switch (verdict) {
      case 'VALIDE': return 'eloi-banner eloi-banner--info';
      case 'CONTESTABLE': return 'eloi-banner eloi-banner--warning';
      case 'NUL': return 'eloi-banner eloi-banner--critical';
      default: return 'eloi-banner eloi-banner--info';
    }
  }

  bannerIcon(verdict: VerdictLegaliteEloignement | string | undefined | null): string {
    switch (verdict) {
      case 'VALIDE': return 'verified';
      case 'CONTESTABLE': return 'warning';
      case 'NUL': return 'gpp_bad';
      default: return 'info_outline';
    }
  }

  bannerLabel(verdict: VerdictLegaliteEloignement | string | undefined | null): string {
    switch (verdict) {
      case 'VALIDE': return 'Mesure légale — recours possible dans les délais';
      case 'CONTESTABLE': return 'Mesure contestable — vice de procédure ou motif limite';
      case 'NUL': return 'Mesure sans fondement — annulation probable';
      default: return '';
    }
  }

  /** Libellé humain depuis un code dispositif (export model). */
  dispositifLabel(code: string | null | undefined): string {
    return dispositifLabel(code as DispositifEloignementCode | null);
  }

  /** Libellé humain depuis un code motif. */
  motifMenaceLabel(code: string | null | undefined): string {
    return motifMenaceLabel(code as MotifMenaceCode | null);
  }

  /** Délai recours formaté "30 jours (TA)". */
  formatDelaiRecours(jours: number | null | undefined,
                     juridiction: string | null | undefined): string {
    if (jours === null || jours === undefined) return '';
    const j = (juridiction === 'CE' ? 'CE' : 'TA');
    return formatDelaiRecours(jours, j as 'TA' | 'CE');
  }

  /**
   * SF-IM-20-02 : pré-fill depuis `aiData` (ImmigrationExtractedData).
   *
   * Mappe `typeProcedureDetectee` (chaîne libre IA) vers `dispositif`
   * via `mapDispositifFromIa`. Pas de pré-fill du motif (le prompt IA
   * actuel n'expose pas `motifMenaceDetecte` — extension backlog).
   *
   * Règles (fail-open) :
   * - passe silencieusement si aiData absent
   * - ne pré-remplit QUE si le champ est encore vide (préserve les edits)
   * - n'écrase jamais si provenance !== 'IA'
   */
  private prefillFromAi(): void {
    // F-236 SF-236-02 : délègue au helper pur partagé.
    const input: PrefillCountInput = {
      aiData: this.aiDataSignal(),
      workspaceCountry: this.workspaceCountry,
    };

    // 1. Dispositif.
    if (this.dispositif() === null) {
      const mapped = MesuresEloignementPrefillRules.computeDispositif(input);
      if (mapped !== null) {
        this.dispositif.set(mapped);
        this.provenanceDispositif.set('IA');
      }
    }

    // 2. SF-246-19 : motif menace.
    const motif = MesuresEloignementPrefillRules.computeMotifMenace(input);
    if (motif !== null
        && (this.motifMenace() === null || this.provenanceMotif() === 'IA')) {
      this.motifMenace.set(motif);
      this.provenanceMotif.set('IA');
    }

    // 3. SF-246-19 : durée présence irrégulière.
    const duree = MesuresEloignementPrefillRules.computeDureePresenceIrreguliereMois(input);
    if (duree !== null
        && (this.dureePresenceIrreguliereMois() === null
            || this.provenanceDureePresenceIrr() === 'IA')) {
      this.dureePresenceIrreguliereMois.set(duree);
      this.provenanceDureePresenceIrr.set('IA');
    }
  }

  /**
   * Divergence dispositif — IA / F-96 / Question IA détecte un dispositif
   * via expectedValue divergente de la saisie avocat. Multi-sources F96 /
   * QUESTION_IA / IA / PIECE_MANQUANTE.
   */
  private buildDispositifAlert(): MesuresEloignementCoherenceAlert | null {
    const userDisp = this.dispositif();
    if (!userDisp) return null;

    const builder = CoherenceAlertBuilder.forField<MesuresEloignementAlertField>('DISPOSITIF');

    // 1. F-96
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'IM20_DISPOSITIF_ELOIGNEMENT') continue;
      const ev = chk.expectedValue;
      if (!ev) continue;
      const expected = mapDispositifFromIa(ev);
      if (!expected || expected === userDisp) continue;
      builder.addSource('F96', {
        expectedDisplay: this.dispositifLabel(expected),
        reason: `Checklist procédurale : dispositif attendu ${this.dispositifLabel(expected)}`
          + (chk.raison ? ` (${chk.raison})` : ''),
      });
      break;
    }

    // 2. QUESTION_IA
    for (const q of this.aiQuestionsSignal()) {
      if (q.critereCode?.toUpperCase() !== 'IM20_DISPOSITIF_ELOIGNEMENT') continue;
      const answer = q.answerText?.trim().toLowerCase();
      if (!answer) continue;
      const isOui = answer === 'oui' || answer.startsWith('oui ')
        || answer.startsWith('oui,') || answer.startsWith('oui.');
      if (!isOui) continue;
      const ev = q.expectedValue;
      if (!ev) continue;
      const expected = mapDispositifFromIa(ev);
      if (!expected || expected === userDisp) continue;
      builder.addSource('QUESTION_IA', {
        expectedDisplay: this.dispositifLabel(expected),
        reason: `Question complémentaire : "${q.questionText}" → "${q.answerText}"`,
      });
      break;
    }

    // 3. IA — `aiData.typeProcedureDetectee` (mapping libre).
    const ai = this.aiDataSignal();
    const procDetectee = ai?.typeProcedureDetectee ?? null;
    const iaDisp = mapDispositifFromIa(procDetectee);
    if (iaDisp && iaDisp !== userDisp) {
      builder.addSource('IA', {
        expectedDisplay: this.dispositifLabel(iaDisp),
        reason: `Analyse du dossier : dispositif détecté "${this.dispositifLabel(iaDisp)}"`,
      });
    }

    // 4. PIECE_MANQUANTE — contributor enrichissant.
    const piece = this.findPieceManquante([
      'IM20_DISPOSITIF_ELOIGNEMENT',
      'MESURE_ELOIGNEMENT_DOSSIER',
    ]);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  /** Divergence motif menace — F96 + QUESTION_IA. */
  private buildMotifAlert(): MesuresEloignementCoherenceAlert | null {
    const userMotif = this.motifMenace();
    if (!userMotif) return null;

    const builder = CoherenceAlertBuilder.forField<MesuresEloignementAlertField>('MOTIF_MENACE');

    // 1. F-96
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'IM20_MOTIF_MENACE') continue;
      const ev = chk.expectedValue;
      if (!ev) continue;
      const expected = mapMotifMenaceFromIa(ev);
      if (!expected || expected === userMotif) continue;
      builder.addSource('F96', {
        expectedDisplay: this.motifMenaceLabel(expected),
        reason: `Checklist procédurale : motif attendu ${this.motifMenaceLabel(expected)}`
          + (chk.raison ? ` (${chk.raison})` : ''),
      });
      break;
    }

    // 2. QUESTION_IA
    for (const q of this.aiQuestionsSignal()) {
      if (q.critereCode?.toUpperCase() !== 'IM20_MOTIF_MENACE') continue;
      const answer = q.answerText?.trim().toLowerCase();
      if (!answer) continue;
      const isOui = answer === 'oui' || answer.startsWith('oui ')
        || answer.startsWith('oui,') || answer.startsWith('oui.');
      if (!isOui) continue;
      const ev = q.expectedValue;
      if (!ev) continue;
      const expected = mapMotifMenaceFromIa(ev);
      if (!expected || expected === userMotif) continue;
      builder.addSource('QUESTION_IA', {
        expectedDisplay: this.motifMenaceLabel(expected),
        reason: `Question complémentaire : "${q.questionText}" → "${q.answerText}"`,
      });
      break;
    }

    // 3. PIECE_MANQUANTE
    const piece = this.findPieceManquante(['IM20_MOTIF_MENACE']);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
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

  alertTooltip(alert: MesuresEloignementCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: MesuresEloignementCoherenceAlert): string {
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
   * Classe CSS d'un risque d'annulation selon sa nature.
   * - "vice", "expir", "absent", "défaut", "manqu", "incompétence" → rouge
   * - autre → or (warning)
   */
  risqueChipClass(risque: string): string {
    if (!risque) return 'eloi-chip-risque eloi-chip-risque--warning';
    const r = risque.toLowerCase();
    if (r.includes('vice') || r.includes('expir')
        || r.includes('absent') || r.includes('défaut') || r.includes('defaut')
        || r.includes('manqu') || r.includes('incompétence') || r.includes('incompetence')
        || r.includes('annulation') || r.includes('irrecev')
        || r.includes('null')) {
      return 'eloi-chip-risque eloi-chip-risque--critical';
    }
    return 'eloi-chip-risque eloi-chip-risque--warning';
  }

  calculate(): void {
    if (!this.formValid()) return;
    const dispositif = this.dispositif()!;
    const motifMenace = this.motifMenace()!;
    const request: MesuresEloignementRequest = {
      dispositif,
      motifMenace,
    };

    // Champs spécifiques par dispositif.
    if (dispositif === 'EXPULSION_PREFECTORALE'
        || dispositif === 'EXPULSION_MINISTERIELLE'
        || dispositif === 'EXPULSION_SECURITE_ETAT') {
      request.procedureCommissionRespectee = this.procedureCommissionRespectee();
      request.urgenceAbsolueJustifiee = this.urgenceAbsolueJustifiee();
    } else if (dispositif === 'IRTF') {
      request.dureePresenceIrreguliereMois = this.dureePresenceIrreguliereMois();
      request.dureeCircularitePrecaire = this.dureeCircularitePrecaire();
      request.comportementAggravant = this.comportementAggravant();
    }
    // IAT : motif uniquement.

    if (this.recoursDelai()) {
      request.recoursDelai = this.recoursDelai();
    }

    this.calculating.set(true);
    // F-163 SF-163-02d : en standalone, POST sur le dispatcher générique.

    const request$ = this.standaloneMode

      ? this.service.calculateStandalone(request)

      : this.service.calculate(this.caseFileId, request);

    request$.subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Mesure d\'éloignement analysée', 'OK', { duration: 2500 });
        // F-163 SF-163-02d : pas de dashboard à rafraîchir en standalone.

        if (!this.standaloneMode) { this.dashboardRefresh?.triggerRefresh(); }
      },
      error: (err) => {
        this.calculating.set(false);
        const msg = err?.error?.message || err?.error || 'Erreur lors de l\'analyse';
        this.snackBar.open(String(msg), 'Fermer', { duration: 5000, panelClass: 'snack-error' });
      },
    });
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        // Hydrate le form avec les valeurs persistées (pour ré-éditer).
        const disp = mapDispositifFromIa(r.dispositif);
        if (disp) this.dispositif.set(disp);
        const motif = mapMotifMenaceFromIa(r.motifMenace);
        if (motif) this.motifMenace.set(motif);
        this.procedureCommissionRespectee.set(r.procedureCommissionRespectee ?? true);
        this.urgenceAbsolueJustifiee.set(r.urgenceAbsolueJustifiee ?? false);
        this.comportementAggravant.set(r.comportementAggravant ?? false);
        // Provenance reset — valeurs persistées = saisie avocat.
        this.provenanceDispositif.set(null);
        this.provenanceMotif.set(null);
        this.provenanceDureePresenceIrr.set(null);
        this.showForm.set(false);
        this.loading.set(false);
      },
      error: () => {
        // 404 attendu si jamais calculé — fallback pré-fill IA.
        this.prefillFromAi();
        this.loading.set(false);
      },
    });
  }
}
