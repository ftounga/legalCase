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
import { AsileAvanceService } from '../../core/services/asile-avance.service';
import {
  AsileAvanceRequest,
  AsileAvanceResponse,
  DISPOSITIF_ASILE_LABELS,
  DispositifAsileCode,
  VerdictAsileAvance,
  dispositifLabel,
  mapDispositifFromIa,
} from '../../core/models/asile-avance.model';
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
import { AsileAvancePrefillRules } from './asile-avance-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-IM-12-02 : champs F-IA-03 audités par l'outil "Asile avancé".
 * - DISPOSITIF_ASILE : divergence si l'IA détecte une procédure ASILE_X
 *   (procedureChecks / questions / aiData.typeProcedureDetectee) avec un
 *   dispositif expectedValue ≠ saisie avocat.
 */
export type AsileAvanceAlertField = 'DISPOSITIF_ASILE';

export type AsileAvanceAlertSource = CoherenceAlertSource;
export type AsileAvanceCoherenceAlert = CoherenceAlert<AsileAvanceAlertField>;

/**
 * SF-IM-12-02 : section frontend dédiée Asile avancé (CESEDA Livre V).
 * 5 dispositifs : DUBLIN_III / PROCEDURE_ACCELEREE / REEXAMEN / APATRIDIE
 * / PROTECTION_SUBSIDIAIRE.
 *
 * Single-country FR — Belgique : bannière info ("régime CESEDA propre à
 * la France") + form masqué (pas d'appel HTTP). L'équivalent BE relève du
 * CGRA + Loi du 15/12/1980 (feature jumelle au backlog F-IM-12-BE).
 *
 * Pattern de référence : `naturalisation-section` (SF-IM-13-02 PR #646
 * — multi-fieldsets + bandeau verdict 3 niveaux + builder F-IA-03 + gate FR).
 */
@Component({
  selector: 'app-asile-avance-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatRadioModule, MatSlideToggleModule,
    MatChipsModule, MatProgressSpinnerModule,
    LegalCitationsPipe,
    ToolJurisprudenceCitationsComponent,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './asile-avance-section.component.html',
  styleUrl: './asile-avance-section.component.scss',
})
export class AsileAvanceSectionComponent implements OnInit, OnChanges {
  // F-JU-03 SF-JU-03-99c v2 — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-IM-12-asile-avance';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'ASILE AVANCÉ (FR)';
  static readonly TOOL_ICON = 'support';

  /** F-236 SF-236-02 — Délègue intégralement au helper pur partagé. */
  static getPrefillCount(input: PrefillCountInput): number {
    return AsileAvancePrefillRules.computePrefillCount(input);
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
   *  - `calculate()` POSTe sur `/api/v1/simulators/F-IM-12-asile-avance/calculate`.
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
  result = signal<AsileAvanceResponse | null>(null);

  // Form fields (signal-based).
  dispositifAsile = signal<DispositifAsileCode | null>(null);
  // DUBLIN_III
  empreintesEurodacAutresEm = signal<boolean>(false);
  demandeurEnFuite = signal<boolean>(false);
  // PROCEDURE_ACCELEREE
  paysOrigineDansListeSurs = signal<boolean>(false);
  fraudeDocumentaireAvere = signal<boolean>(false);
  refusPriseEmpreintes = signal<boolean>(false);
  // REEXAMEN
  dateDecisionAnterieure = signal<string | null>(null);
  elementsNouveaux = signal<boolean>(false);
  // APATRIDIE / PROTECTION_SUBSIDIAIRE
  motifsExclusion = signal<boolean>(false);
  presenceReguliere = signal<boolean>(true);
  // PROTECTION_SUBSIDIAIRE
  traitementsGravesEtablis = signal<boolean>(false);

  /** Provenance IA — badge "Pré-rempli depuis l'analyse" effaçable. */
  provenanceDispositif = signal<'IA' | null>(null);
  /** SF-246-19 : provenance IA pour la date de décision antérieure. */
  provenanceDateDecision = signal<'IA' | null>(null);

  /** Listes pour mat-radio. */
  readonly dispositifOptions = DISPOSITIF_ASILE_LABELS;

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  /** Champs conditionnels selon le dispositif sélectionné. */
  showDublinFields = computed<boolean>(() => this.dispositifAsile() === 'DUBLIN_III');
  showAccelereeFields = computed<boolean>(() => this.dispositifAsile() === 'PROCEDURE_ACCELEREE');
  showReexamenFields = computed<boolean>(() => this.dispositifAsile() === 'REEXAMEN');
  showApatridieFields = computed<boolean>(() => this.dispositifAsile() === 'APATRIDIE');
  showProtectionSubFields = computed<boolean>(
    () => this.dispositifAsile() === 'PROTECTION_SUBSIDIAIRE');

  /** Alertes de cohérence F-IA-03 par field — gate uniquement en mode formulaire. */
  coherenceAlerts = computed<Partial<Record<AsileAvanceAlertField, AsileAvanceCoherenceAlert>>>(() => {
    // F-163 SF-163-02d : aucune source IA en standalone.
    if (this.standaloneMode) return {};
    if (!this.showForm()) return {};
    if (!this.isFrance()) return {};
    const alerts: Partial<Record<AsileAvanceAlertField, AsileAvanceCoherenceAlert>> = {};
    const a = this.buildDispositifAlert();
    if (a) alerts.DISPOSITIF_ASILE = a;
    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    const blockers = values.filter((a) => a.severity === 'CRITICAL').length;
    return { total: values.length, blockers };
  });

  constructor(
    private service: AsileAvanceService,
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
   * Form valid si dispositif choisi. REEXAMEN exige en plus une date de
   * décision antérieure (sinon le backend produit IRRECEVABLE).
   */
  formValid(): boolean {
    const disp = this.dispositifAsile();
    if (!disp) return false;
    if (disp === 'REEXAMEN') {
      const d = this.dateDecisionAnterieure();
      return !!d && d.length >= 8;
    }
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // Handlers de modification — effacent la provenance IA pour le dispositif.
  onDispositifChange(value: DispositifAsileCode | null): void {
    this.dispositifAsile.set(value);
    this.provenanceDispositif.set(null);
  }

  onEmpreintesChange(value: boolean): void {
    this.empreintesEurodacAutresEm.set(value);
  }

  onFuiteChange(value: boolean): void {
    this.demandeurEnFuite.set(value);
  }

  onPaysSurChange(value: boolean): void {
    this.paysOrigineDansListeSurs.set(value);
  }

  onFraudeChange(value: boolean): void {
    this.fraudeDocumentaireAvere.set(value);
  }

  onRefusEmpreintesChange(value: boolean): void {
    this.refusPriseEmpreintes.set(value);
  }

  onDateDecisionChange(value: string | null): void {
    this.dateDecisionAnterieure.set(value && value.length > 0 ? value : null);
    this.provenanceDateDecision.set(null);
  }

  onElementsNouveauxChange(value: boolean): void {
    this.elementsNouveaux.set(value);
  }

  onMotifsExclusionChange(value: boolean): void {
    this.motifsExclusion.set(value);
  }

  onPresenceReguliereChange(value: boolean): void {
    this.presenceReguliere.set(value);
  }

  onTraitementsGravesChange(value: boolean): void {
    this.traitementsGravesEtablis.set(value);
  }

  /**
   * Bandeau verdict :
   * - RECEVABLE_*, FRANCE_COMPETENTE → navy/info
   * - ACCELEREE_APPLICABLE / ACCELEREE_NON_APPLICABLE → or/warning
   * - IRRECEVABLE → rouge/critical
   */
  bannerClass(verdict: VerdictAsileAvance | string | undefined | null): string {
    switch (verdict) {
      case 'IRRECEVABLE':
        return 'asile-banner asile-banner--critical';
      case 'ACCELEREE_APPLICABLE':
      case 'ACCELEREE_NON_APPLICABLE':
        return 'asile-banner asile-banner--warning';
      case 'RECEVABLE_TRANSFERT':
      case 'FRANCE_COMPETENTE':
      case 'RECEVABLE_REEXAMEN':
      case 'RECEVABLE_APATRIDIE':
      case 'RECEVABLE_PROTECTION_SUBSIDIAIRE':
        return 'asile-banner asile-banner--info';
      default:
        return 'asile-banner asile-banner--info';
    }
  }

  bannerIcon(verdict: VerdictAsileAvance | string | undefined | null): string {
    switch (verdict) {
      case 'IRRECEVABLE': return 'gpp_bad';
      case 'ACCELEREE_APPLICABLE':
      case 'ACCELEREE_NON_APPLICABLE': return 'warning';
      default: return 'verified';
    }
  }

  bannerLabel(verdict: VerdictAsileAvance | string | undefined | null): string {
    switch (verdict) {
      case 'RECEVABLE_TRANSFERT': return 'Transfert Dublin III recevable';
      case 'FRANCE_COMPETENTE': return 'France compétente — examen au fond';
      case 'ACCELEREE_APPLICABLE': return 'Procédure accélérée applicable';
      case 'ACCELEREE_NON_APPLICABLE': return 'Procédure accélérée non applicable';
      case 'RECEVABLE_REEXAMEN': return 'Réexamen recevable';
      case 'RECEVABLE_APATRIDIE': return 'Statut d\'apatride recevable';
      case 'RECEVABLE_PROTECTION_SUBSIDIAIRE': return 'Protection subsidiaire recevable';
      case 'IRRECEVABLE': return 'Demande irrecevable — critère bloquant';
      default: return '';
    }
  }

  /** Libellé humain depuis un code dispositif (export model). */
  dispositifLabel(code: string | null | undefined): string {
    return dispositifLabel(code as DispositifAsileCode | null);
  }

  /**
   * SF-IM-12-02 : pré-fill depuis `aiData` (ImmigrationExtractedData).
   *
   * Mapping pragmatique sur `typeProcedureDetectee` : si l'IA détecte une
   * procédure ASILE_X (Dublin / accélérée / réexamen / apatridie / PS), le
   * dispositif est pré-rempli avec provenance IA.
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

    // 1. Dispositif asile.
    if (!this.dispositifAsile()) {
      const detected = AsileAvancePrefillRules.computeDispositifAsile(input);
      if (detected !== null) {
        this.dispositifAsile.set(detected);
        this.provenanceDispositif.set('IA');
      }
    }

    // 2. SF-246-19 : date de décision antérieure.
    const dateDec = AsileAvancePrefillRules.computeDateDecisionAnterieure(input);
    if (dateDec !== null
        && (this.dateDecisionAnterieure() === null || this.provenanceDateDecision() === 'IA')) {
      this.dateDecisionAnterieure.set(dateDec);
      this.provenanceDateDecision.set('IA');
    }
  }

  /**
   * Divergence dispositif d'asile — IA / F-96 / Question IA détecte une
   * procédure ASILE_X via expectedValue divergente de la saisie avocat.
   * Multi-sources F-96 / QUESTION_IA / IA / PIECE_MANQUANTE.
   */
  private buildDispositifAlert(): AsileAvanceCoherenceAlert | null {
    const userDisp = this.dispositifAsile();
    if (!userDisp) return null;

    const builder = CoherenceAlertBuilder.forField<AsileAvanceAlertField>('DISPOSITIF_ASILE');

    // 1. F-96
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'IM12_DISPOSITIF_ASILE') continue;
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
      if (q.critereCode?.toUpperCase() !== 'IM12_DISPOSITIF_ASILE') continue;
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

    // 3. IA — `aiData.typeProcedureDetectee` (si ASILE_X).
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
    const piece = this.findPieceManquante(['IM12_DISPOSITIF_ASILE', 'ASILE_DOSSIER']);
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

  alertTooltip(alert: AsileAvanceCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: AsileAvanceCoherenceAlert): string {
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
   * Classe CSS d'un risque selon sa nature.
   * - "exclusion", "refus", "fuite", "irrecevable" → rouge
   * - autre → or
   */
  risqueChipClass(risque: string): string {
    if (!risque) return 'asile-chip-risque asile-chip-risque--warning';
    const r = risque.toLowerCase();
    if (r.includes('exclusion') || r.includes('refus')
        || r.includes('fuite') || r.includes('irrecevable')
        || r.includes('refusé') || r.includes('non établi')
        || r.includes('non démontré') || r.includes('non vérifiable')
        || r.includes('non régulière') || r.includes('irrégulière')) {
      return 'asile-chip-risque asile-chip-risque--critical';
    }
    return 'asile-chip-risque asile-chip-risque--warning';
  }

  calculate(): void {
    if (!this.formValid()) return;
    const disp = this.dispositifAsile()!;
    const request: AsileAvanceRequest = {
      dispositifAsile: disp,
    };
    // Champs spécifiques par dispositif.
    switch (disp) {
      case 'DUBLIN_III':
        request.empreintesEurodacAutresEm = this.empreintesEurodacAutresEm();
        request.demandeurEnFuite = this.demandeurEnFuite();
        break;
      case 'PROCEDURE_ACCELEREE':
        request.paysOrigineDansListeSurs = this.paysOrigineDansListeSurs();
        request.fraudeDocumentaireAvere = this.fraudeDocumentaireAvere();
        request.refusPriseEmpreintes = this.refusPriseEmpreintes();
        break;
      case 'REEXAMEN':
        request.dateDecisionAnterieure = this.dateDecisionAnterieure();
        request.elementsNouveaux = this.elementsNouveaux();
        break;
      case 'APATRIDIE':
        request.motifsExclusion = this.motifsExclusion();
        request.presenceReguliere = this.presenceReguliere();
        break;
      case 'PROTECTION_SUBSIDIAIRE':
        request.traitementsGravesEtablis = this.traitementsGravesEtablis();
        request.motifsExclusion = this.motifsExclusion();
        break;
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
        this.snackBar.open('Asile avancé analysé', 'OK', { duration: 2500 });
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
        const disp = mapDispositifFromIa(r.dispositifAsile);
        if (disp) this.dispositifAsile.set(disp);
        if (r.dateDecisionAnterieure) this.dateDecisionAnterieure.set(r.dateDecisionAnterieure);
        // Provenance reset — valeurs persistées = saisie avocat.
        this.provenanceDispositif.set(null);
        this.provenanceDateDecision.set(null);
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
