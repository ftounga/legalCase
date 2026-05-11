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
import { OqtfSansDelaiService } from '../../core/services/oqtf-sans-delai.service';
import {
  MOTIFS_SANS_DELAI,
  MotifSansDelai,
  MotifSansDelaiOption,
  OqtfSansDelaiResponse,
  StatutDelaiSd,
} from '../../core/models/oqtf-sans-delai.model';
import {
  ImmigrationExtractedData,
  PieceManquanteEntry,
} from '../../core/models/case-analysis.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { AiQuestion } from '../../core/models/ai-question.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { DecisionalHeaderFlagComponent } from '../decisional-tools-panel/decisional-header-flag/decisional-header-flag.component';
import { CoherencePopoverTriggerDirective } from '../../shared/coherence-popover/coherence-popover-trigger.directive';
import { CoherenceAlert, CoherenceAlertSeverity } from '../../shared/coherence-popover/coherence-alert.model';
import { CoherenceAlertBuilder } from '../../shared/coherence-popover/coherence-alert-builder';
import { SourceExplanation } from '../../core/models/source-explanation.model';
import { SourceExplanationService } from '../../core/services/source-explanation.service';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { OqtfSansDelaiPrefillRules } from './oqtf-sans-delai-section-prefill-rules';

/**
 * SF-IM-08-04 : outil décisionnel dédié "OQTF SANS délai de départ
 * volontaire — France" (F-IM-08). FR uniquement. Procédure d'urgence
 * absolue : 48h pour former un recours (contre 30 jours pour l'OQTF
 * avec délai). Invariant "un outil = une situation métier" : séparé
 * de SF-IM-08-02 (OQTF avec délai).
 * Consomme l'API SF-IM-08-03. Affiché conditionnellement par le panel
 * F-IA-04 (tool_id 'F-IM-08-oqtf-sans-delai-fr').
 *
 * SF-155-04-B2 : pré-fill IA depuis `ImmigrationExtractedData` + alertes
 * cohérence F-IA-03 critiques (délai 48h dépassé, placement CRA détecté,
 * divergence date/heure, contradiction recours).
 * SF-155-05 : interfaces factorisées via `CoherenceAlert<F>` partagée.
 */
export type OqtfSdAlertField =
  | 'DATE_HEURE_NOTIFICATION'
  | 'PLACEMENT_CRA'
  | 'RECOURS_FORME'
  | 'MOTIF_SANS_DELAI';

export type OqtfSdAlertSeverity = CoherenceAlertSeverity;
export type OqtfSdCoherenceAlert = CoherenceAlert<OqtfSdAlertField>;

// F-236 SF-236-02 : `ALLOWED_MOTIFS_SANS_DELAI` est désormais déclaré dans le
// helper partagé `<component>-prefill-rules.ts` (re-export ALLOWED_MOTIFS_SANS_DELAI).

const MS_ONE_HOUR = 3_600_000;
const MS_48H = 48 * MS_ONE_HOUR;

@Component({
  selector: 'app-oqtf-sans-delai-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatSlideToggleModule, MatChipsModule, MatProgressSpinnerModule,
    LegalCitationsPipe,
    DecisionalHeaderFlagComponent,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './oqtf-sans-delai-section.component.html',
  styleUrl: './oqtf-sans-delai-section.component.scss',
})
export class OqtfSansDelaiSectionComponent implements OnInit, OnChanges {
  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'OQTF SANS DÉLAI DE DÉPART VOLONTAIRE (FR) — 48H';
  static readonly TOOL_ICON = 'gavel';

  /** F-236 SF-236-02 — Délègue intégralement au helper pur partagé. */
  static getPrefillCount(input: PrefillCountInput): number {
    return OqtfSansDelaiPrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  // F-177 SF-177-03b : force l'expansion (mode modal F-177).
  @Input() forceExpanded = false;

  // SF-155-04-B2 fix : setter-backed signal pour que `isFrance` (computed)
  // réagisse aux assignations @Input de workspaceCountry, y compris quand
  // les tests le changent via `component.workspaceCountry = 'BELGIQUE';`
  // sans passer par le cycle fixture/ngOnChanges.
  private readonly _workspaceCountry = signal<'FRANCE' | 'BELGIQUE'>('FRANCE');
  @Input()
  set workspaceCountry(v: 'FRANCE' | 'BELGIQUE') { this._workspaceCountry.set(v); }
  get workspaceCountry(): 'FRANCE' | 'BELGIQUE' { return this._workspaceCountry(); }

  // SF-155-04-B2 : Inputs IA (synthèse dossier + checklist F-96 + questions F-IA-02 + pièces F-145).
  @Input() aiData?: ImmigrationExtractedData | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;


  /**
   * F-163 SF-163-02d — Mode simulateur autonome (hors dossier client).
   *
   * Quand `true` :
   *  - Bannière « 🧪 Mode simulateur » affichée en haut.
   *  - `prefillFromAi()` court-circuité.
   *  - `coherenceAlerts` retourne `{}`.
   *  - `loadExisting()` / `load()` court-circuité.
   *  - `analyze()` POSTe sur `/api/v1/simulators/F-IM-08-oqtf-sans-delai-fr/calculate`.
   *  - `triggerRefresh()` jamais invoqué.
   *
   * Default `false` — mode case-file scoped inchangé.
   */
  @Input() standaloneMode: boolean = false;
  // SF-155-06 : signals miroirs (pattern canonique F-IM-05) pour que le
  // `coherenceAlerts` computed réagisse aux changements de procedureChecks /
  // aiQuestions / piecesManquantes post-mount.
  private aiDataSignal = signal<ImmigrationExtractedData | null | undefined>(undefined);
  private procedureChecksSignal = signal<ProcedureCheck[]>([]);
  private aiQuestionsSignal = signal<AiQuestion[]>([]);
  private piecesManquantesSignal = signal<PieceManquanteEntry[]>([]);

  collapsed = signal(true);
  loading = signal(false);
  analyzing = signal(false);
  showForm = signal(true);
  result = signal<OqtfSansDelaiResponse | null>(null);

  dateHeureNotificationOqtf = signal<string | null>(null);
  motifSansDelai = signal<MotifSansDelai | null>(null);
  placementCra = signal<boolean>(false);
  recoursForme = signal<boolean>(false);
  dateHeureRecours = signal<string | null>(null);

  // SF-155-04-B2 : signals de provenance IA (badge "Pré-rempli depuis l'analyse").
  // Effacés dès que l'avocat modifie manuellement un champ (onXxxChange).
  provenanceDateHeure = signal<'IA' | null>(null);
  provenanceMotifSansDelai = signal<'IA' | null>(null);
  provenancePlacementCra = signal<'IA' | null>(null);
  provenanceRecoursForme = signal<'IA' | null>(null);

  // SF-155-04-B2 : signal d'override de l'horloge courante (pour tests déterministes).
  // En prod : utilise toujours Date.now().
  private nowMsOverride: number | null = null;

  readonly motifs: MotifSansDelaiOption[] = MOTIFS_SANS_DELAI;

  /** Maintenant au format ISO local (YYYY-MM-DDTHH:mm) pour `max` des inputs datetime-local. */
  readonly nowLocalIso: string = this.buildNowLocalIso();

  isFrance = computed<boolean>(() => this._workspaceCountry() === 'FRANCE');

  // SF-155-04-B2 : alertes cohérence F-IA-03. Gate strict : uniquement
  // quand le formulaire est affiché (règle SF-IA-03-12 : ne pas réafficher
  // après calcul, seul le verdict importe).
  // SF-155-06 : lit `this.aiData` directement pour préserver la compat des
  // tests existants qui assignent `component.aiData` APRÈS `ngOnInit()` sans
  // appeler `ngOnChanges`. Le recompute est déclenché par les autres signals
  // qui changent (recoursForme, placementCra, etc.). Les signals miroirs
  // `procedureChecksSignal` / `aiQuestionsSignal` / `piecesManquantesSignal`
  // restent utilisés par les builders pour les nouvelles sources.
  coherenceAlerts = computed<Partial<Record<OqtfSdAlertField, OqtfSdCoherenceAlert>>>(() => {
    // F-163 SF-163-02d : aucune source IA en standalone.
    if (this.standaloneMode) return {};
    if (!this.showForm()) return {};
    if (!this.isFrance()) return {};
    const alerts: Partial<Record<OqtfSdAlertField, OqtfSdCoherenceAlert>> = {};
    // SF-155-06 : fallback sur `this.aiData` si le signal n'a pas encore été hydraté
    // (cas de tests qui mutent `component.aiData` après `ngOnInit`).
    // `ai` peut être null — les builders sont tolérants aux champs IA absents
    // pour pouvoir émettre une alerte purement F96 / QUESTION_IA / PIECE_MANQUANTE.
    const ai = this.aiDataSignal() ?? this.aiData ?? null;

    // ALERTE CRITIQUE 48h : notif IA > 48h + recoursForme=false (recours hors délai probable).
    const critical48h = ai ? this.buildAlert48hExpired(ai) : null;
    if (critical48h) {
      alerts.RECOURS_FORME = critical48h;
    } else {
      // Contradiction recours formé (non critique 48h) : IA dit OUI/NON et avocat diverge.
      // Possible aussi avec QUESTION_IA seule sans aiData.
      const recoursContradiction = this.buildRecoursContradictionAlert(ai);
      if (recoursContradiction) alerts.RECOURS_FORME = recoursContradiction;
    }

    // ALERTE CRITIQUE CRA : IA a détecté CRA, avocat dit NON.
    if (ai) {
      const criticalCra = this.buildCraAlert(ai);
      if (criticalCra) alerts.PLACEMENT_CRA = criticalCra;
    }

    // Divergence date/heure saisie vs IA > 1h.
    if (ai) {
      const dateAlert = this.buildDateHeureDivergenceAlert(ai);
      if (dateAlert) alerts.DATE_HEURE_NOTIFICATION = dateAlert;
    }

    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    const blockers = values.filter((a) => a && a.severity === 'CRITICAL').length;
    return { total: values.length, blockers };
  });

  // SF-155-07 (DIV-7) : map {sourceKey → explanations} pour popovers SF-IA-03-15c.
  sourceExplanations = signal<Map<string, SourceExplanation[]>>(new Map());

  constructor(
    private service: OqtfSansDelaiService,
    private snackBar: MatSnackBar,
    @Optional() private dashboardRefresh: CaseDashboardRefreshService,
    // SF-155-07 (DIV-7) : injection `@Optional()` — fail-open strict.
    @Optional() private sourceExplanationService: SourceExplanationService | null,
  ) {}

  ngOnInit(): void {
    // F-177 SF-177-03b : appliqué dès le mount pour le mode modal.
    if (this.forceExpanded) this.collapsed.set(false);
    // SF-155-06 : hydrate les signals miroirs avant évaluations computed.
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
      // SF-155-04-B2 : pré-fill IA au mount si aiData est déjà disponible.
      // Ne préempte pas le load() existant — prefillFromAi ne touche que
      // les signals encore à leur valeur défaut.
      this.prefillFromAi();
      this.load();
      // SF-155-07 (DIV-7) : pré-charge les explications F-IA-03-15a.
      this.loadSourceExplanations();
    }
  }

  /**
   * SF-155-07 (DIV-7) : charge les explications de sources via F-IA-03-15a.
   * Fail-open strict : erreur ou absence = map vide.
   */
  private loadSourceExplanations(): void {
    if (!this.caseFileId) return;
    if (!this.sourceExplanationService) return;
    this.sourceExplanationService.getForCaseFile(this.caseFileId).subscribe({
      next: (map) => this.sourceExplanations.set(map),
      error: () => { /* fail-open */ },
    });
  }

  /**
   * SF-155-07 (DIV-7) : mapping field → sourceKey. Convention `IM08_<FIELD>`.
   */
  explanationFor(field: OqtfSdAlertField): SourceExplanation[] {
    const key = (() => {
      switch (field) {
        case 'DATE_HEURE_NOTIFICATION': return 'IM08_DATE_HEURE_NOTIFICATION';
        case 'PLACEMENT_CRA': return 'IM08_PLACEMENT_CRA';
        case 'RECOURS_FORME': return 'IM08_RECOURS_FORME';
        case 'MOTIF_SANS_DELAI': return 'IM08_MOTIF_SANS_DELAI';
      }
    })();
    return this.sourceExplanations().get(key) ?? [];
  }

  ngOnChanges(changes: SimpleChanges): void {
    // F-177 SF-177-03b : applique le forceExpanded quand il passe à true en cours de vie.
    if (changes['forceExpanded'] && this.forceExpanded) this.collapsed.set(false);
    // SF-155-06 : ré-hydrate les signals miroirs à chaque changement d'input.
    if (changes['aiData']) this.aiDataSignal.set(this.aiData);
    if (changes['procedureChecks']) this.procedureChecksSignal.set(this.procedureChecks ?? []);
    if (changes['aiQuestions']) this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    if (changes['piecesManquantes']) this.piecesManquantesSignal.set(this.piecesManquantes ?? []);
    // SF-155-04-B2 : re-applique le pré-fill dès que aiData change avant la
    // première résolution (cas : pipeline IA termine après l'ouverture du
    // dossier). Si le form n'est plus affiché (result() présent), le
    // pré-fill serait contre-productif — on s'abstient.
    if (
      changes['aiData'] &&
      !changes['aiData'].firstChange &&
      this.isFrance() &&
      this.showForm() &&
      !this.result()
    ) {
      this.prefillFromAi();
    }
  }

  toggleCollapse(): void {
    this.collapsed.update((v) => !v);
  }

  formValid(): boolean {
    const d = this.dateHeureNotificationOqtf();
    const m = this.motifSansDelai();
    if (!d || !m) return false;
    if (d > this.nowLocalIso) return false; // pas dans le futur
    if (this.recoursForme()) {
      const dr = this.dateHeureRecours();
      if (!dr) return false;
      if (dr < d) return false; // recours >= notification
    }
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  analyze(): void {
    if (!this.formValid()) return;
    const request = {
      dateHeureNotificationOqtf: this.dateHeureNotificationOqtf()!,
      motifSansDelai: this.motifSansDelai()!,
      placementCra: this.placementCra(),
      recoursForme: this.recoursForme(),
      ...(this.recoursForme()
        ? { dateHeureRecours: this.dateHeureRecours()! }
        : {}),
    };
    this.analyzing.set(true);
    // F-163 SF-163-02d : en standalone, POST sur le dispatcher générique.

    const request$ = this.standaloneMode

      ? this.service.analyzeStandalone(request)

      : this.service.analyze(this.caseFileId, request);

    request$.subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('OQTF sans délai analysée', 'OK', { duration: 2500 });
        // F-163 SF-163-02d : pas de dashboard à rafraîchir en standalone.

        if (!this.standaloneMode) { this.dashboardRefresh?.triggerRefresh(); }
      },
      error: (err) => {
        this.analyzing.set(false);
        const msg = err?.error?.message || err?.error || 'Erreur lors de l\'analyse';
        this.snackBar.open(String(msg), 'Fermer', { duration: 5000, panelClass: 'snack-error' });
      },
    });
  }

  /**
   * Classe CSS du bandeau de résultat selon le statut calculé.
   * Urgence absolue 48h : palette rouge dominante — DISPONIBLE et
   * URGENT sont déjà en rouge (contrairement à SF-IM-08-02 où DISPONIBLE
   * est bleu / URGENT est or). Seul RECOURS_FORME bascule en vert.
   * Autorisé par DESIGN_SYSTEM.md pour les situations d'urgence critique.
   */
  bannerClass(statut: StatutDelaiSd | null | undefined): string {
    switch (statut) {
      case 'DISPONIBLE':    return 'oqtf-sd-banner oqtf-sd-banner--danger-medium';
      case 'URGENT':        return 'oqtf-sd-banner oqtf-sd-banner--danger-strong';
      case 'EXPIRE':        return 'oqtf-sd-banner oqtf-sd-banner--danger-dark';
      case 'RECOURS_FORME': return 'oqtf-sd-banner oqtf-sd-banner--success';
      default:              return 'oqtf-sd-banner';
    }
  }

  bannerIcon(statut: StatutDelaiSd | null | undefined): string {
    switch (statut) {
      case 'DISPONIBLE':    return 'warning';
      case 'URGENT':        return 'error';
      case 'EXPIRE':        return 'error';
      case 'RECOURS_FORME': return 'check_circle';
      default:              return 'warning';
    }
  }

  statutLabel(statut: StatutDelaiSd | null | undefined): string {
    switch (statut) {
      case 'DISPONIBLE':    return 'Délai 48h disponible';
      case 'URGENT':        return 'Délai 48h urgent';
      case 'EXPIRE':        return 'Délai 48h expiré';
      case 'RECOURS_FORME': return 'Recours formé';
      default:              return '';
    }
  }

  /** Affiche le compteur d'heures restantes si pertinent (hors EXPIRE/RECOURS_FORME). */
  showHeuresRestantes(r: OqtfSansDelaiResponse | null): boolean {
    if (!r) return false;
    return r.statutDelaiRecours === 'DISPONIBLE' || r.statutDelaiRecours === 'URGENT';
  }

  // -----------------------------------------------------------------
  // SF-155-04-B2 — Handlers manuels : effacent la provenance IA.
  // -----------------------------------------------------------------

  onDateHeureNotificationChange(value: string | null): void {
    this.dateHeureNotificationOqtf.set(value || null);
    this.provenanceDateHeure.set(null);
  }

  onMotifSansDelaiChange(value: MotifSansDelai | null): void {
    this.motifSansDelai.set(value);
    this.provenanceMotifSansDelai.set(null);
  }

  onPlacementCraChange(value: boolean): void {
    this.placementCra.set(value);
    this.provenancePlacementCra.set(null);
  }

  onRecoursFormeChange(value: boolean): void {
    this.recoursForme.set(value);
    this.provenanceRecoursForme.set(null);
  }

  // -----------------------------------------------------------------
  // SF-155-04-B2 — Helpers alertes cohérence.
  // -----------------------------------------------------------------

  alertTooltip(alert: OqtfSdCoherenceAlert): string {
    return alert.reason;
  }

  alertBadgeLabel(alert: OqtfSdCoherenceAlert): string {
    return alert.severity === 'CRITICAL'
      ? `Alerte critique : ${alert.expectedDisplay}`
      : `Incohérence détectée : ${alert.expectedDisplay}`;
  }

  /**
   * Test hook — permet aux specs de figer l'horloge courante pour tester
   * les seuils 48h de façon déterministe. Ne modifie pas le comportement
   * en production (Date.now() reste la référence quand override est null).
   */
  setNowMsOverride(ms: number | null): void {
    this.nowMsOverride = ms;
  }

  private nowMs(): number {
    return this.nowMsOverride ?? Date.now();
  }

  /** SF-155-04-B2 : alerte critique 48h — notification > 48h ET recours non formé.
   *  SF-155-06 : enrichi avec PIECE_MANQUANTE (requête JLD 48h manquante). */
  private buildAlert48hExpired(ai: ImmigrationExtractedData): OqtfSdCoherenceAlert | null {
    const iso = ai.dateHeureNotificationOqtfSansDelai;
    if (!iso) return null;
    if (this.recoursForme()) return null;
    const notifMs = this.parseIsoToMs(iso);
    if (notifMs === null) return null;
    const deltaMs = this.nowMs() - notifMs;
    if (deltaMs <= MS_48H) return null;
    const hours = Math.round(deltaMs / MS_ONE_HOUR);
    const builder = CoherenceAlertBuilder.forField<OqtfSdAlertField>('RECOURS_FORME')
      .withSeverity('CRITICAL')
      .addSource('IA', {
        expectedDisplay: 'Recours hors délai probable',
        reason: `Notification OQTF il y a environ ${hours} h (IA : ${iso}). Délai 48h probablement dépassé — aucun recours formé dans le dossier.`,
      });
    const piece = this.findPieceManquante(['IM08_RECOURS_FORME']);
    if (piece) builder.addPieceManquante(piece);
    return builder.build();
  }

  /**
   * Contradiction non critique : IA a détecté que le recours est (ou n'est
   * pas) formé, avocat dit l'inverse. Pas déclenchée si le CRITIQUE 48h
   * est déjà actif (prime sur cet alerte).
   * SF-155-06 : enrichi avec QUESTION_IA (réponse "oui"/"non") + PIECE_MANQUANTE.
   */
  private buildRecoursContradictionAlert(ai: ImmigrationExtractedData | null): OqtfSdCoherenceAlert | null {
    const userRecours = this.recoursForme();
    const builder = CoherenceAlertBuilder.forField<OqtfSdAlertField>('RECOURS_FORME').withSeverity('WARNING');

    // SF-155-06 : QUESTION_IA — réponse oui/non sur IM08_RECOURS_FORME.
    for (const q of this.aiQuestionsSignal()) {
      if (q.critereCode?.toUpperCase() !== 'IM08_RECOURS_FORME') continue;
      const answer = q.answerText?.trim().toLowerCase();
      if (!answer) continue;
      let qRecours: boolean | null = null;
      if (answer === 'oui' || answer.startsWith('oui ') || answer.startsWith('oui,') || answer.startsWith('oui.')) {
        qRecours = true;
      } else if (answer === 'non' || answer.startsWith('non ') || answer.startsWith('non,') || answer.startsWith('non.')) {
        qRecours = false;
      }
      if (qRecours === null) continue;
      if (qRecours === userRecours) continue;
      builder.addSource('QUESTION_IA', {
        expectedDisplay: qRecours ? 'Recours déjà formé' : 'Aucun recours formé',
        reason: `Question complémentaire : "${q.questionText}" → "${q.answerText}"`,
      });
      break;
    }

    // IA : recoursFormeDetected (si aiData disponible).
    const reponse = ai?.recoursFormeDetected?.reponse;
    if (reponse === 'OUI' || reponse === 'NON') {
      const iaRecours = reponse === 'OUI';
      if (iaRecours !== userRecours) {
        const just = ai?.recoursFormeDetected?.justification;
        const justSuffix = just ? ` (${just})` : '';
        builder.addSource('IA', {
          expectedDisplay: iaRecours ? 'Recours déjà formé' : 'Aucun recours formé',
          reason: `Analyse du dossier : recours ${iaRecours ? 'déjà formé' : 'non formé'}${justSuffix}`,
        });
      }
    }

    // SF-155-06 : PIECE_MANQUANTE.
    const piece = this.findPieceManquante(['IM08_RECOURS_FORME']);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  /** SF-155-04-B2 : alerte critique CRA détecté par IA, avocat coche non.
   *  SF-155-06 : enrichi avec PIECE_MANQUANTE. */
  private buildCraAlert(ai: ImmigrationExtractedData): OqtfSdCoherenceAlert | null {
    if (ai.placementCraDetected !== true) return null;
    if (this.placementCra() === true) return null;
    const builder = CoherenceAlertBuilder.forField<OqtfSdAlertField>('PLACEMENT_CRA')
      .withSeverity('CRITICAL')
      .addSource('IA', {
        expectedDisplay: 'Placement CRA détecté',
        reason: 'L\'analyse du dossier a détecté une mention de placement en CRA — vérifier les pièces.',
      });
    const piece = this.findPieceManquante(['IM08_PLACEMENT_CRA']);
    if (piece) builder.addPieceManquante(piece);
    return builder.build();
  }

  /** Divergence date/heure saisie vs IA > 1h — alerte warning.
   *  SF-155-06 : enrichi avec PIECE_MANQUANTE (accusé de notification). */
  private buildDateHeureDivergenceAlert(ai: ImmigrationExtractedData): OqtfSdCoherenceAlert | null {
    const iso = ai.dateHeureNotificationOqtfSansDelai;
    if (!iso) return null;
    const saisie = this.dateHeureNotificationOqtf();
    if (!saisie) return null;
    const iaMs = this.parseIsoToMs(iso);
    const saisieMs = this.parseIsoToMs(saisie);
    if (iaMs === null || saisieMs === null) return null;
    if (Math.abs(iaMs - saisieMs) <= MS_ONE_HOUR) return null;
    const builder = CoherenceAlertBuilder.forField<OqtfSdAlertField>('DATE_HEURE_NOTIFICATION')
      .withSeverity('WARNING')
      .addSource('IA', {
        expectedDisplay: 'Date IA divergente',
        reason: `Analyse du dossier : ${iso} — écart > 1 h avec la saisie (${saisie}).`,
      });
    const piece = this.findPieceManquante(['IM08_DATE_NOTIFICATION', 'IM08_DATE_HEURE_NOTIFICATION']);
    if (piece) builder.addPieceManquante(piece);
    return builder.build();
  }

  /**
   * SF-155-06 : recherche une `PieceManquanteEntry` dont `critereCode`
   * appartient (case-insensitive) à la liste acceptée. Retourne le 1er `texte`
   * trouvé, ou `null`.
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

  /**
   * Parse ISO partiel (YYYY-MM-DDTHH:mm[:ss]) en ms. Null si format invalide.
   * Utilise `new Date()` qui interprète la chaîne en timezone LOCALE quand il
   * n'y a ni Z ni offset — cohérent avec les valeurs saisies par l'avocat
   * via `<input type="datetime-local">`.
   */
  private parseIsoToMs(iso: string): number | null {
    if (!iso) return null;
    if (!/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(:\d{2})?$/.test(iso)) return null;
    const t = new Date(iso).getTime();
    if (Number.isNaN(t)) return null;
    return t;
  }

  // -----------------------------------------------------------------
  // SF-155-04-B2 — Pré-fill IA.
  // -----------------------------------------------------------------

  private prefillFromAi(): void {
    // F-236 SF-236-02 : délègue au helper pur partagé. Garde-fou supplémentaire :
    // ne pas écraser si le form n'est pas affiché (analyse déjà faite).
    if (!this.isFrance()) return;
    if (!this.showForm()) return;
    const input: PrefillCountInput = {
      aiData: this.aiData,
      workspaceCountry: this.workspaceCountry,
    };

    // 1) dateHeureNotificationOqtf
    const dateHeure = OqtfSansDelaiPrefillRules.computeDateHeureNotificationOqtf(input);
    if (dateHeure !== null && !this.dateHeureNotificationOqtf()) {
      this.dateHeureNotificationOqtf.set(dateHeure);
      this.provenanceDateHeure.set('IA');
    }

    // 2) motifSansDelai
    const motif = OqtfSansDelaiPrefillRules.computeMotifSansDelai(input);
    if (motif !== null && !this.motifSansDelai()) {
      this.motifSansDelai.set(motif);
      this.provenanceMotifSansDelai.set('IA');
    }

    // 3) placementCra
    const placement = OqtfSansDelaiPrefillRules.computePlacementCra(input);
    if (placement !== null
        && this.provenancePlacementCra() === null
        && this.placementCra() === false) {
      this.placementCra.set(placement);
      this.provenancePlacementCra.set('IA');
    }

    // 4) recoursForme
    const recours = OqtfSansDelaiPrefillRules.computeRecoursForme(input);
    if (recours !== null
        && this.provenanceRecoursForme() === null
        && this.recoursForme() === false) {
      this.recoursForme.set(recours);
      this.provenanceRecoursForme.set('IA');
    }
  }

  /**
   * Convertit un ISO partiel (YYYY-MM-DDTHH:mm[:ss]) en chaîne compatible
   * `<input type="datetime-local">` (YYYY-MM-DDTHH:mm, sans secondes).
   * Retourne null si la chaîne ne matche pas le format attendu.
   */
  private normalizeDatetimeLocalInput(iso: string): string | null {
    if (!iso) return null;
    const m = iso.match(/^(\d{4}-\d{2}-\d{2}T\d{2}:\d{2})(:\d{2})?$/);
    if (!m) return null;
    return m[1];
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.dateHeureNotificationOqtf.set(this.toLocalInputValue(r.dateHeureNotificationOqtf));
        this.motifSansDelai.set(r.motifSansDelai);
        this.placementCra.set(r.placementCra);
        this.recoursForme.set(r.recoursForme);
        this.dateHeureRecours.set(r.dateHeureRecours ? this.toLocalInputValue(r.dateHeureRecours) : null);
        this.showForm.set(false);
        // SF-155-04-B2 : reset provenance IA (les valeurs viennent maintenant
        // du backend, plus de l'analyse IA — badge "Pré-rempli depuis l'analyse"
        // ne doit pas s'afficher).
        this.provenanceDateHeure.set(null);
        this.provenanceMotifSansDelai.set(null);
        this.provenancePlacementCra.set(null);
        this.provenanceRecoursForme.set(null);
        this.loading.set(false);
      },
      error: () => {
        // 404 attendu si aucune analyse — on reste en mode formulaire.
        this.loading.set(false);
      },
    });
  }

  /**
   * Convertit une ISO string backend (avec ou sans offset) en chaîne
   * compatible avec `<input type="datetime-local">` (YYYY-MM-DDTHH:mm).
   * Tolérant aux valeurs déjà au bon format.
   */
  private toLocalInputValue(iso: string): string {
    if (!iso) return '';
    // Déjà au format datetime-local (pas de Z / pas d'offset)
    if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/.test(iso)) return iso;
    const d = new Date(iso);
    if (isNaN(d.getTime())) return iso;
    const pad = (n: number) => n.toString().padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
  }

  private buildNowLocalIso(): string {
    const d = new Date();
    const pad = (n: number) => n.toString().padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
  }
}
