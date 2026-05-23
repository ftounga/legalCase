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
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { DivorceAccepteService } from '../../core/services/divorce-accepte.service';
import {
  DivorceAccepteRequest,
  DivorceAccepteResponse,
  DivorceAccepteVerdict,
  FamilleExtractedData,
} from '../../core/models/divorce-accepte.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { PieceManquanteEntry } from '../../core/models/case-analysis.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { AiQuestion } from '../../core/models/ai-question.model';
import { CoherencePopoverTriggerDirective } from '../../shared/coherence-popover/coherence-popover-trigger.directive';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';
import {
  CoherenceAlert,
  CoherenceAlertSource,
} from '../../shared/coherence-popover/coherence-alert.model';
import { CoherenceAlertBuilder } from '../../shared/coherence-popover/coherence-alert-builder';
import { SourceExplanation } from '../../core/models/source-explanation.model';
import { SourceExplanationService } from '../../core/services/source-explanation.service';
import { DivorceAcceptePrefillRules } from './divorce-accepte-section-prefill-rules';

/**
 * SF-FA-10-02 : champs F-IA-03 audités par l'outil F-FA-10 (divorce accepté).
 * Un par field pré-remplissable depuis l'IA (`FamilleExtractedData`).
 */
export type DivorceAccepteAlertField =
  | 'DUREE_MARIAGE'
  | 'REVENUS_EPOUX1'
  | 'REVENUS_EPOUX2'
  | 'PATRIMOINE_COMMUN'
  | 'DATE_ACCEPTATION_PV';

export type DivorceAccepteAlertSource = CoherenceAlertSource;
export type DivorceAccepteCoherenceAlert = CoherenceAlert<DivorceAccepteAlertField>;

/** Seuil divergence sur les montants annuels (10 %, aligné sur F-DT-09). */
const REVENUS_DIVERGENCE_RATIO = 0.10;
/** Seuil divergence sur la durée du mariage (1 an absolu). */
const DUREE_MARIAGE_TOLERANCE_ANNEES = 1;
const ISO_DATE_REGEX = /^\d{4}-\d{2}-\d{2}$/;

/**
 * SF-FA-10-02 : composant Angular standalone pour l'outil "Divorce accepté"
 * (F-FA-10 — art. 233-234 Cciv + 1123 CPC). FRANCE uniquement, droit famille.
 *
 * Pattern de référence : `harcelement-licenciement-nul-section` (template
 * canonique 2026-04-24, cf. `ai-skills/frontend-coherence-audit.md` §5).
 *
 * - Form : slide-toggles + numériques + dates natives `<input type="date">`.
 * - Verdict binaire ELEVEE/FAIBLE → palette navy `--available` / rouge
 *   classique `--danger` (palette standard, pas de gradation).
 * - Pré-fill IA via `aiData?: FamilleExtractedData | null` (5 champs ouverts,
 *   no-op si absent).
 * - Validation F-IA-03 sur 5 fields via `CoherenceAlertBuilder`.
 * - Refresh dashboard F-IA-02 + MatSnackBar erreurs.
 */
@Component({
  selector: 'app-divorce-accepte-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule, DecimalPipe,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatSlideToggleModule, MatProgressSpinnerModule,
    LegalCitationsPipe,
    CoherencePopoverTriggerDirective,,
    ToolJurisprudenceCitationsComponent
  ],
  templateUrl: './divorce-accepte-section.component.html',
  styleUrl: './divorce-accepte-section.component.scss',
})
export class DivorceAccepteSectionComponent implements OnInit, OnChanges {
  // F-JU-03 SF-JU-03-05 — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-FA-10-divorce-accepte';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'DIVORCE ACCEPTÉ — ART. 233 CCIV';
  static readonly TOOL_ICON = 'how_to_vote';

  /**
   * F-236 SF-236-02 — Compteur miroir de `prefillFromAi()` via helper partagé.
   */
  static getPrefillCount(input: {
    aiData?: FamilleExtractedData | null;
    procedureChecks?: unknown[];
    aiQuestions?: unknown[];
    piecesManquantes?: unknown[];
    triggerEvents?: unknown[];
    workspaceCountry?: string;
  }): number {
    return DivorceAcceptePrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';
  // Inputs IA (tous optionnels — null-safe partout).
  @Input() aiData?: FamilleExtractedData | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;

  // Snapshots signal des inputs IA pour réactivité des `computed` signals.
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
   *  - `calculate()` POSTe sur `/api/v1/simulators/F-FA-10-divorce-accepte/calculate`.
   *  - `triggerRefresh()` jamais invoqué.
   *
   * Default `false` — mode case-file scoped inchangé.
   */
  @Input() standaloneMode: boolean = false;
  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<DivorceAccepteResponse | null>(null);

  // Form fields
  acceptationPrincipeSignee = signal<boolean>(false);
  dateAcceptationPV = signal<string | null>(null);
  dureeMariageAnnees = signal<number | null>(null);
  revenusAnnuelsEpoux1Eur = signal<number | null>(null);
  revenusAnnuelsEpoux2Eur = signal<number | null>(null);
  patrimoineCommun = signal<boolean>(false);
  dateAssignation = signal<string | null>(null);

  // Provenance IA par champ pré-remplissable.
  provenanceDureeMariage = signal<'IA' | null>(null);
  provenanceRevenusEpoux1 = signal<'IA' | null>(null);
  provenanceRevenusEpoux2 = signal<'IA' | null>(null);
  provenancePatrimoineCommun = signal<'IA' | null>(null);
  provenanceDateAcceptationPV = signal<'IA' | null>(null);

  // Map {sourceKey → explanations} pour le popover F-IA-03-15c (fail-open).
  sourceExplanations = signal<Map<string, SourceExplanation[]>>(new Map());

  /**
   * Coherence alerts F-IA-03 — gate `showForm()` strict (pattern anti-bug
   * SF-IA-03-12 : pas de `|| result()`).
   */
  coherenceAlerts = computed<Partial<Record<DivorceAccepteAlertField, DivorceAccepteCoherenceAlert>>>(() => {
    // F-163 SF-163-02c : aucune source IA en standalone.
    if (this.standaloneMode) return {};
    if (!this.showForm()) return {};
    const alerts: Partial<Record<DivorceAccepteAlertField, DivorceAccepteCoherenceAlert>> = {};
    const dureeAlert = this.buildDureeMariageAlert();
    if (dureeAlert) alerts.DUREE_MARIAGE = dureeAlert;
    const r1Alert = this.buildRevenusEpoux1Alert();
    if (r1Alert) alerts.REVENUS_EPOUX1 = r1Alert;
    const r2Alert = this.buildRevenusEpoux2Alert();
    if (r2Alert) alerts.REVENUS_EPOUX2 = r2Alert;
    const patAlert = this.buildPatrimoineAlert();
    if (patAlert) alerts.PATRIMOINE_COMMUN = patAlert;
    const pvAlert = this.buildDateAcceptationPVAlert();
    if (pvAlert) alerts.DATE_ACCEPTATION_PV = pvAlert;
    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    return { total: values.length };
  });

  constructor(
    private service: DivorceAccepteService,
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
    if (this.workspaceCountry === 'FRANCE') {
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

  explanationFor(field: DivorceAccepteAlertField): SourceExplanation[] {
    const key = (() => {
      switch (field) {
        case 'DUREE_MARIAGE': return 'FA10_DUREE_MARIAGE';
        case 'REVENUS_EPOUX1': return 'FA10_REVENUS_EPOUX1';
        case 'REVENUS_EPOUX2': return 'FA10_REVENUS_EPOUX2';
        case 'PATRIMOINE_COMMUN': return 'FA10_PATRIMOINE_COMMUN';
        case 'DATE_ACCEPTATION_PV': return 'FA10_DATE_ACCEPTATION_PV';
      }
    })();
    return this.sourceExplanations().get(key) ?? [];
  }

  alertTooltip(alert: DivorceAccepteCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: DivorceAccepteCoherenceAlert): string {
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

  formValid(): boolean {
    const acc = this.acceptationPrincipeSignee();
    const duree = this.dureeMariageAnnees();
    const r1 = this.revenusAnnuelsEpoux1Eur();
    const r2 = this.revenusAnnuelsEpoux2Eur();
    return acc === true
      && duree !== null && duree >= 0 && Number.isInteger(duree)
      && r1 !== null && r1 >= 0
      && r2 !== null && r2 >= 0;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // Handlers manuels — effacent le badge IA correspondant.
  onAcceptationChange(value: boolean): void {
    this.acceptationPrincipeSignee.set(value);
  }

  onDateAcceptationPVChange(value: string | null): void {
    this.dateAcceptationPV.set(value || null);
    this.provenanceDateAcceptationPV.set(null);
  }

  onDureeMariageChange(value: number | null): void {
    this.dureeMariageAnnees.set(value);
    this.provenanceDureeMariage.set(null);
  }

  onRevenusEpoux1Change(value: number | null): void {
    this.revenusAnnuelsEpoux1Eur.set(value);
    this.provenanceRevenusEpoux1.set(null);
  }

  onRevenusEpoux2Change(value: number | null): void {
    this.revenusAnnuelsEpoux2Eur.set(value);
    this.provenanceRevenusEpoux2.set(null);
  }

  onPatrimoineCommunChange(value: boolean): void {
    this.patrimoineCommun.set(value);
    this.provenancePatrimoineCommun.set(null);
  }

  onDateAssignationChange(value: string | null): void {
    this.dateAssignation.set(value || null);
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request: DivorceAccepteRequest = {
      acceptationPrincipeSignee: this.acceptationPrincipeSignee(),
      dureeMariageAnnees: this.dureeMariageAnnees()!,
      revenusAnnuelsEpoux1Eur: this.revenusAnnuelsEpoux1Eur()!,
      revenusAnnuelsEpoux2Eur: this.revenusAnnuelsEpoux2Eur()!,
      patrimoineCommun: this.patrimoineCommun(),
      ...(this.dateAcceptationPV() ? { dateAcceptationPV: this.dateAcceptationPV()! } : {}),
      ...(this.dateAssignation() ? { dateAssignation: this.dateAssignation()! } : {}),
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
        this.snackBar.open('Analyse divorce accepté calculée', 'OK', { duration: 2500 });
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
        this.applyResult(r);
        this.loading.set(false);
      },
      error: () => {
        // 404 attendu si aucune analyse — on reste en mode formulaire.
        this.prefillFromAi();
        this.loading.set(false);
      },
    });
  }

  private applyResult(r: DivorceAccepteResponse): void {
    this.result.set(r);
    this.acceptationPrincipeSignee.set(r.acceptationPrincipeSignee);
    this.dateAcceptationPV.set(r.dateAcceptationPV);
    this.dureeMariageAnnees.set(r.dureeMariageAnnees);
    this.revenusAnnuelsEpoux1Eur.set(r.revenusAnnuelsEpoux1Eur);
    this.revenusAnnuelsEpoux2Eur.set(r.revenusAnnuelsEpoux2Eur);
    this.patrimoineCommun.set(r.patrimoineCommun);
    this.dateAssignation.set(r.dateAssignation);
    // Valeurs persistées = saisie avocat — jamais de badge IA.
    this.provenanceDureeMariage.set(null);
    this.provenanceRevenusEpoux1.set(null);
    this.provenanceRevenusEpoux2.set(null);
    this.provenancePatrimoineCommun.set(null);
    this.provenanceDateAcceptationPV.set(null);
    this.showForm.set(false);
  }

  /**
   * Pré-fill 5 champs IA :
   *  - dureeMariageAnnees
   *  - revenusAnnuelsEpoux1Eur
   *  - revenusAnnuelsEpoux2Eur
   *  - patrimoineCommun
   *  - dateAcceptationPV
   *
   * No-op gracieux si champ absent. N'écrase pas une saisie avocat
   * (provenance === null indique modification manuelle).
   */
  private prefillFromAi(): void {
    // F-236 SF-236-02 — délégation au helper partagé (parité runtime/static).
    const helperInput = { aiData: this.aiDataSignal() };

    const duree = DivorceAcceptePrefillRules.computeDureeMariage(helperInput);
    if (duree !== null
        && (this.dureeMariageAnnees() === null || this.provenanceDureeMariage() === 'IA')) {
      this.dureeMariageAnnees.set(duree);
      this.provenanceDureeMariage.set('IA');
    }

    const r1 = DivorceAcceptePrefillRules.computeRevenusEpoux1(helperInput);
    if (r1 !== null
        && (this.revenusAnnuelsEpoux1Eur() === null || this.provenanceRevenusEpoux1() === 'IA')) {
      this.revenusAnnuelsEpoux1Eur.set(r1);
      this.provenanceRevenusEpoux1.set('IA');
    }

    const r2 = DivorceAcceptePrefillRules.computeRevenusEpoux2(helperInput);
    if (r2 !== null
        && (this.revenusAnnuelsEpoux2Eur() === null || this.provenanceRevenusEpoux2() === 'IA')) {
      this.revenusAnnuelsEpoux2Eur.set(r2);
      this.provenanceRevenusEpoux2.set('IA');
    }

    const patr = DivorceAcceptePrefillRules.computePatrimoineCommun(helperInput);
    if (patr !== null
        && (this.provenancePatrimoineCommun() === 'IA' || this.patrimoineCommun() === false)) {
      // Heuristique conservatrice : si l'avocat a déjà coché true, on n'écrase pas.
      this.patrimoineCommun.set(patr);
      this.provenancePatrimoineCommun.set('IA');
    }

    const pv = DivorceAcceptePrefillRules.computeDateAcceptationPV(helperInput);
    if (pv !== null
        && (this.dateAcceptationPV() === null || this.provenanceDateAcceptationPV() === 'IA')) {
      this.dateAcceptationPV.set(pv);
      this.provenanceDateAcceptationPV.set('IA');
    }
  }

  // ---------------------------------------------------------------------------
  // Builders d'alertes F-IA-03 (un par field)
  // ---------------------------------------------------------------------------

  private buildDureeMariageAlert(): DivorceAccepteCoherenceAlert | null {
    const iaVal = this.aiDataSignal()?.dureeMariageAnnees;
    const user = this.dureeMariageAnnees();
    if (typeof iaVal !== 'number') return null;
    if (user === null) return null;
    if (Math.abs(user - iaVal) <= DUREE_MARIAGE_TOLERANCE_ANNEES) return null;
    const builder = CoherenceAlertBuilder.forField<DivorceAccepteAlertField>('DUREE_MARIAGE')
      .addSource('IA', {
        expectedDisplay: `${iaVal} an(s)`,
        reason: `Analyse du dossier : durée du mariage estimée à ${iaVal} an(s)`,
      });
    const piece = this.findPieceManquante(['FA10_DUREE_MARIAGE', 'ACTE_MARIAGE']);
    if (piece) builder.addPieceManquante(piece);
    return builder.build();
  }

  private buildRevenusEpoux1Alert(): DivorceAccepteCoherenceAlert | null {
    return this.buildRevenusAlert(
      'REVENUS_EPOUX1',
      this.aiDataSignal()?.revenusAnnuelsEpoux1Eur,
      this.revenusAnnuelsEpoux1Eur(),
      'époux 1',
      ['FA10_REVENUS_EPOUX1', 'AVIS_IMPOSITION_EPOUX1'],
    );
  }

  private buildRevenusEpoux2Alert(): DivorceAccepteCoherenceAlert | null {
    return this.buildRevenusAlert(
      'REVENUS_EPOUX2',
      this.aiDataSignal()?.revenusAnnuelsEpoux2Eur,
      this.revenusAnnuelsEpoux2Eur(),
      'époux 2',
      ['FA10_REVENUS_EPOUX2', 'AVIS_IMPOSITION_EPOUX2'],
    );
  }

  private buildRevenusAlert(
    field: DivorceAccepteAlertField,
    iaVal: number | null | undefined,
    user: number | null,
    label: string,
    pieceCodes: string[],
  ): DivorceAccepteCoherenceAlert | null {
    if (typeof iaVal !== 'number' || iaVal <= 0) return null;
    if (user === null || user <= 0) return null;
    const ratio = Math.abs(user - iaVal) / iaVal;
    if (ratio <= REVENUS_DIVERGENCE_RATIO) return null;
    const builder = CoherenceAlertBuilder.forField<DivorceAccepteAlertField>(field)
      .addSource('IA', {
        expectedDisplay: `${iaVal.toLocaleString('fr-FR')} €`,
        reason: `Analyse du dossier : revenus annuels ${label} ~${iaVal.toLocaleString('fr-FR')} € (écart > 10 % avec la valeur saisie)`,
      });
    const piece = this.findPieceManquante(pieceCodes);
    if (piece) builder.addPieceManquante(piece);
    return builder.build();
  }

  private buildPatrimoineAlert(): DivorceAccepteCoherenceAlert | null {
    const iaVal = this.aiDataSignal()?.patrimoineCommun;
    const user = this.patrimoineCommun();
    if (typeof iaVal !== 'boolean') return null;
    if (iaVal === user) return null;
    const builder = CoherenceAlertBuilder.forField<DivorceAccepteAlertField>('PATRIMOINE_COMMUN')
      .addSource('IA', {
        expectedDisplay: iaVal ? 'Patrimoine commun' : 'Pas de patrimoine commun',
        reason: `Analyse du dossier : régime ${iaVal ? 'avec patrimoine commun' : 'sans patrimoine commun'}`,
      });
    const piece = this.findPieceManquante(['FA10_PATRIMOINE_COMMUN', 'CONTRAT_MARIAGE']);
    if (piece) builder.addPieceManquante(piece);
    return builder.build();
  }

  private buildDateAcceptationPVAlert(): DivorceAccepteCoherenceAlert | null {
    const iaDate = this.aiDataSignal()?.dateAcceptationPV;
    const user = this.dateAcceptationPV();
    if (!user) return null;
    if (!iaDate || !ISO_DATE_REGEX.test(iaDate)) return null;
    if (user === iaDate) return null;
    const builder = CoherenceAlertBuilder.forField<DivorceAccepteAlertField>('DATE_ACCEPTATION_PV')
      .addSource('IA', {
        expectedDisplay: iaDate,
        reason: `Analyse du dossier : PV signé le ${iaDate}`,
      });
    const piece = this.findPieceManquante(['FA10_DATE_ACCEPTATION_PV', 'PV_ACCEPTATION']);
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

  // ---------------------------------------------------------------------------
  // Helpers d'affichage du résultat
  // ---------------------------------------------------------------------------

  verdictBannerClass(verdict: DivorceAccepteVerdict): string {
    return verdict === 'ELEVEE'
      ? 'da-verdict-banner da-verdict-banner--available'
      : 'da-verdict-banner da-verdict-banner--danger';
  }

  verdictBannerLabel(verdict: DivorceAccepteVerdict): string {
    return verdict === 'ELEVEE' ? 'Éligibilité élevée' : 'Éligibilité faible';
  }

  verdictBannerIcon(verdict: DivorceAccepteVerdict): string {
    return verdict === 'ELEVEE' ? 'check_circle' : 'error';
  }
}
