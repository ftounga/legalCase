import { Component, Input, OnInit, OnChanges, SimpleChanges, Optional, signal, computed } from '@angular/core';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { ImmigrationExtractedData, ImmigrationTriggerEvent, PieceManquanteEntry } from '../../core/models/case-analysis.model';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { FormsModule } from '@angular/forms';
import { ImmigrationTitleDecisionService } from '../../core/services/immigration-title-decision.service';
import { TitleDecisionResponse, TitleRecommendation } from '../../core/models/immigration-title-decision.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { AiQuestion } from '../../core/models/ai-question.model';
import { SourceExplanationService } from '../../core/services/source-explanation.service';
import { SourceExplanation } from '../../core/models/source-explanation.model';
import { CoherencePopoverTriggerDirective } from '../../shared/coherence-popover/coherence-popover-trigger.directive';
import { CoherenceAlert, CoherenceAlertSource } from '../../shared/coherence-popover/coherence-alert.model';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';
import { CoherenceAlertBuilder } from '../../shared/coherence-popover/coherence-alert-builder';
import { RetainedPisteAlignment } from '../../core/models/retained-piste-alignment.model';
import { ProcedureCheckAlignment } from '../../core/models/procedure-check-alignment.model';
import { ProcedureChecksOutputComponent } from '../decisional-tools-panel/procedure-checks-output/procedure-checks-output.component';
import { computeBadge, ProcedureChecksBadge } from '../decisional-tools-panel/procedure-check-badge.helper';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { ImmigrationTitleDecisionPrefillRules } from './immigration-title-decision-section-prefill-rules';

/**
 * F-192 SF-192-02 — Verdict utilisé par la card du panel F-IA-04 pour afficher
 * un pill or `🎯 Aligné`/`🎯 Divergence` à côté du pill `auto_awesome`. Calculé
 * via static helper `getRetainedPistesBadge()` — pattern miroir `getPrefillCount`
 * (SF-177-12), permet l'affichage AVANT instanciation du composant outil.
 */
export type RetainedPistesBadgeKind = 'aligned' | 'divergent' | 'none';
export interface RetainedPistesBadge {
  kind: RetainedPistesBadgeKind;
  count: number;
}

const MOTIFS_ENUM = new Set(['TRAVAIL', 'ETUDES', 'FAMILLE', 'ASILE', 'AUTRE']);

export type IM05AlertField = 'MOTIF' | 'NATIONALITE_UE';

// SF-155-05 : alias local rétro-compat — utilise l'interface générique partagée.
export type IM05AlertSource = CoherenceAlertSource;
export type IM05CoherenceAlert = CoherenceAlert<IM05AlertField>;

// F-236 SF-236-02 : les constantes CODE_TO_MOTIF / TRIGGER_TO_CRITERIA sont
// désormais déclarées dans le helper partagé `<component>-prefill-rules.ts`.
// On en récupère un alias local pour `buildMotifAlert` (lecture seule).
const CODE_TO_MOTIF = ImmigrationTitleDecisionPrefillRules.CODE_TO_MOTIF;

@Component({
  selector: 'app-immigration-title-decision-section',
  standalone: true,
  imports: [
    FormsModule,
    MatButtonModule, MatIconModule,
    MatSelectModule, MatFormFieldModule,
    MatProgressSpinnerModule, MatSlideToggleModule,
    MatTooltipModule,
    CoherencePopoverTriggerDirective,
    ProcedureChecksOutputComponent,
    ToolJurisprudenceCitationsComponent
  ],
  templateUrl: './immigration-title-decision-section.component.html',
  styleUrl: './immigration-title-decision-section.component.scss'
})
export class ImmigrationTitleDecisionSectionComponent implements OnInit, OnChanges {
  // F-JU-03 SF-JU-03-03 — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-IM-05-arbre-decisionnel-titre';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'TITRE DE SÉJOUR RECOMMANDÉ';
  static readonly TOOL_ICON = 'account_tree';

  /**
   * F-236 SF-236-02 — Délègue intégralement au helper pur partagé
   * `ImmigrationTitleDecisionPrefillRules`. Le runtime `prefillFromAi()`
   * ci-dessous consomme les mêmes fonctions sur le même input — divergence
   * impossible par construction (cf. `<component>-prefill-rules.ts`).
   */
  static getPrefillCount(input: PrefillCountInput): number {
    return ImmigrationTitleDecisionPrefillRules.computePrefillCount(input);
  }

  /**
   * F-192 SF-192-02 — Pattern miroir de `getPrefillCount` : permet au panel
   * F-IA-04 d'afficher le pill or `🎯 Aligné stratégie retenue (N)` ou
   * `🎯 Divergence stratégie retenue (N)` AVANT instanciation du composant.
   *
   * <p>Règles V1 :
   * <ul>
   *   <li>0 piste avec `toolIdCible = F-IM-05-arbre-decisionnel-titre` →
   *   `kind = 'none'`, `count = 0`.</li>
   *   <li>≥ 1 piste `DIVERGENT` (priorité) → `kind = 'divergent'`,
   *   `count = total filtré sur ce tool` (ALIGNED + DIVERGENT + NOT_ANALYZED).</li>
   *   <li>≥ 1 piste `ALIGNED` et 0 DIVERGENT → `kind = 'aligned'`,
   *   `count = total filtré`.</li>
   *   <li>Que des `NOT_ANALYZED` → `kind = 'none'` (pas encore comparé).</li>
   * </ul></p>
   */
  static getRetainedPistesBadge(input: {
    pistesRetenues?: RetainedPisteAlignment[] | null;
  }): RetainedPistesBadge {
    const all = Array.isArray(input.pistesRetenues) ? input.pistesRetenues : [];
    const filtered = all.filter(
      p => p.toolIdCible === 'F-IM-05-arbre-decisionnel-titre',
    );
    if (filtered.length === 0) return { kind: 'none', count: 0 };
    const hasDivergent = filtered.some(p => p.matchStatus === 'DIVERGENT');
    const hasAligned = filtered.some(p => p.matchStatus === 'ALIGNED');
    if (hasDivergent) return { kind: 'divergent', count: filtered.length };
    if (hasAligned) return { kind: 'aligned', count: filtered.length };
    return { kind: 'none', count: 0 };
  }

  /**
   * F-193 SF-193-02 — Pattern miroir `getRetainedPistesBadge` ci-dessus.
   * La liste reçue est déjà pré-filtrée par le panel via `inputs(ctx)` sur
   * `toolIdCible === 'F-IM-05-arbre-decisionnel-titre'`.
   */
  static getProcedureChecksBadge(input: {
    proceduresChecksAlignment?: ProcedureCheckAlignment[] | null;
  }): ProcedureChecksBadge {
    return computeBadge(Array.isArray(input.proceduresChecksAlignment) ? input.proceduresChecksAlignment : []);
  }

  @Input() caseFileId!: string;
  @Input() aiData?: ImmigrationExtractedData | null;
  /** SF-IM-05-04 : événements déclencheurs F-150 — priorité sur aiData pour déduire motif+situation. */
  @Input() triggerEvents?: ImmigrationTriggerEvent[] | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;
  /**
   * F-192 SF-192-02 — Alignement IA des pistes 🟢 RETAINED (F-176) avec
   * l'outil F-IM-05. Pré-filtré côté panel via `TOOL_REGISTRY.inputs(ctx)`
   * sur `toolIdCible === 'F-IM-05-arbre-decisionnel-titre'`. Affichage pur :
   * la sortie outil affiche les badges/blocs « Stratégies retenues par vous »
   * sans modifier le formulaire ni la décision IA.
   */
  @Input() pistesRetenues?: RetainedPisteAlignment[] | null;
  /**
   * F-193 SF-193-02 — Alignement des checks F-96 sur cet outil (déjà
   * pré-filtré par le panel via TOOL_REGISTRY.inputs(ctx)).
   */
  @Input() proceduresChecksAlignment?: ProcedureCheckAlignment[] | null;
  /**
   * F-194 SF-194-02 — Libellés des pièces taggées « OBTENUE » par l'avocat
   * et alignées sur cet outil (pré-filtrées par le panel). V1 = passif
   * (signal d'aide visuel forward-compat).
   */
  @Input() piecesObtenues?: string[] | null;
  // F-177 SF-177-03b : force l'expansion (mode modal F-177).
  @Input() forceExpanded = false;

  private aiDataSignal = signal<ImmigrationExtractedData | null | undefined>(undefined);
  private procedureChecksSignal = signal<ProcedureCheck[]>([]);
  private aiQuestionsSignal = signal<AiQuestion[]>([]);
  private piecesManquantesSignal = signal<PieceManquanteEntry[]>([]);

  collapsed = signal(true);
  loading = signal(false);
  resolving = signal(false);
  showForm = signal(true);
  decision = signal<TitleDecisionResponse | null>(null);

  country = signal('FRANCE');
  nationaliteUe = signal(false);
  motif = signal('TRAVAIL');
  duree = signal('LONG_SEJOUR');
  situationFamiliale = signal<string | null>(null);

  // SF-IM-05-04 : provenance IA par champ (badge "Pré-rempli depuis l'analyse").
  // Effacé dès que l'avocat modifie manuellement un champ (onXxxChange).
  provenanceMotif = signal<'IA' | null>(null);
  provenanceSituationFamiliale = signal<'IA' | null>(null);
  provenanceNationaliteUe = signal<'IA' | null>(null);

  readonly countries = [
    { value: 'FRANCE', label: 'France' },
    { value: 'BELGIQUE', label: 'Belgique' },
  ];

  readonly motifs = [
    { value: 'TRAVAIL', label: 'Travail' },
    { value: 'ETUDES', label: 'Études' },
    { value: 'FAMILLE', label: 'Famille' },
    { value: 'ASILE', label: 'Asile' },
    { value: 'AUTRE', label: 'Autre' },
  ];

  readonly durees = [
    { value: 'COURT_SEJOUR', label: 'Court séjour (< 1 an)' },
    { value: 'LONG_SEJOUR', label: 'Long séjour (≥ 1 an)' },
  ];

  readonly situations = [
    { value: 'CELIBATAIRE', label: 'Célibataire' },
    { value: 'MARIE', label: 'Marié(e)' },
    { value: 'PACS_COHABITATION', label: 'PACS / Cohabitation légale' },
  ];

  // SF-IA-03-15c — map {sourceKey → explanation}
  sourceExplanations = signal<Map<string, SourceExplanation[]>>(new Map());

  constructor(
    private decisionService: ImmigrationTitleDecisionService,
    // SF-155-07 (DIV-11) : `@Optional()` pour cohérence stricte avec le canonique
    // `harcelement-licenciement-nul-section`. Fail-open si service absent (tests / DI minimal).
    @Optional() private sourceExplanationService: SourceExplanationService | null,
    private snackBar: MatSnackBar,
    @Optional() private refreshService: CaseDashboardRefreshService | null,
  ) {}

  private loadSourceExplanations(): void {
    if (!this.caseFileId) return;
    // SF-155-07 (DIV-11) : garde null-safe alignée sur le canonique.
    if (!this.sourceExplanationService) return;
    this.sourceExplanationService.getForCaseFile(this.caseFileId).subscribe({
      next: map => this.sourceExplanations.set(map),
      error: () => { /* fail-open */ },
    });
  }

  /** SF-IA-03-15c : mapping vers sourceKey (F96 pour MOTIF, générique pour NATIONALITE_UE). */
  explanationFor(field: IM05AlertField): SourceExplanation[] {
    const key = field === 'MOTIF' ? 'IM05_MOTIF' : 'nationalite_ue';
    return this.sourceExplanations().get(key) ?? [];
  }

  coherenceAlerts = computed<Partial<Record<IM05AlertField, IM05CoherenceAlert>>>(() => {
    if (!this.showForm()) return {};
    const alerts: Partial<Record<IM05AlertField, IM05CoherenceAlert>> = {};
    const motifAlert = this.buildMotifAlert();
    if (motifAlert) alerts.MOTIF = motifAlert;
    const natAlert = this.buildNationaliteAlert();
    if (natAlert) alerts.NATIONALITE_UE = natAlert;
    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    return { total: values.length, blockers: 0 };
  });

  // F-192 SF-192-02 — pistes retenues filtrées par matchStatus pour la sortie outil.
  // Les @Input changent par référence ; on lit `this.pistesRetenues` directement
  // (les computed sont recalculés à chaque CD via les signals d'inputs ci-dessus).
  retainedPistesAligned = computed(() =>
    (this.pistesRetenues ?? []).filter(p => p.matchStatus === 'ALIGNED'),
  );
  retainedPistesDivergent = computed(() =>
    (this.pistesRetenues ?? []).filter(p => p.matchStatus === 'DIVERGENT'),
  );
  retainedPistesNotAnalyzed = computed(() =>
    (this.pistesRetenues ?? []).filter(p => p.matchStatus === 'NOT_ANALYZED'),
  );
  hasRetainedPistesAligned = computed(() => this.retainedPistesAligned().length > 0);
  hasRetainedPistesNonRecommended = computed(
    () => this.retainedPistesDivergent().length + this.retainedPistesNotAnalyzed().length > 0,
  );

  ngOnInit(): void {
    // F-177 SF-177-03b : appliqué dès le mount pour le mode modal.
    if (this.forceExpanded) this.collapsed.set(false);
    this.aiDataSignal.set(this.aiData);
    this.procedureChecksSignal.set(this.procedureChecks ?? []);
    this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    this.piecesManquantesSignal.set(this.piecesManquantes ?? []);
    // SF-IM-05-04 : garde-fou — pré-remplit au mount si aiData/triggerEvents
    // sont déjà disponibles (cas : pipeline déjà tourné avant ouverture du dossier).
    this.prefillFromAi();
    this.loadExisting();
    this.loadSourceExplanations();
  }

  ngOnChanges(changes: SimpleChanges): void {
    // F-177 SF-177-03b : applique le forceExpanded quand il passe à true en cours de vie.
    if (changes['forceExpanded'] && this.forceExpanded) this.collapsed.set(false);
    if (changes['aiData']) this.aiDataSignal.set(this.aiData);
    if (changes['procedureChecks']) this.procedureChecksSignal.set(this.procedureChecks ?? []);
    if (changes['aiQuestions']) this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    if (changes['piecesManquantes']) this.piecesManquantesSignal.set(this.piecesManquantes ?? []);
    // SF-IM-05-04 : ré-applique le prefill dès que aiData OU triggerEvents change
    // (les 2 sont maintenant pris en compte — les triggers sont prioritaires).
    if ((changes['aiData'] || changes['triggerEvents']) && this.showForm() && !this.decision()) {
      this.prefillFromAi();
    }
  }

  private buildPiecesIndex(pieces: PieceManquanteEntry[]): Record<string, string> {
    const index: Record<string, string> = {};
    if (!pieces) return index;
    for (const p of pieces) {
      const code = p.critereCode?.toUpperCase();
      if (!code) continue;
      if (code !== 'IM05_MOTIF') continue;
      if (!index[code]) index[code] = p.texte;
    }
    return index;
  }

  alertTooltip(alert: IM05CoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: IM05CoherenceAlert): string {
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

  private buildMotifAlert(): IM05CoherenceAlert | null {
    const user = this.motif();
    if (!user) return null;

    const piecesIndex = this.buildPiecesIndex(this.piecesManquantesSignal());
    const pieceTexte = piecesIndex['IM05_MOTIF'] ?? null;
    const builder = CoherenceAlertBuilder.forField<IM05AlertField>('MOTIF');

    // F-96 VERIFIED
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'IM05_MOTIF') continue;
      if (chk.statut !== 'VERIFIED') continue;
      const ev = chk.expectedValue?.toUpperCase();
      if (!ev || !MOTIFS_ENUM.has(ev)) continue;
      if (ev !== user) {
        builder.addSource('F96', {
          expectedDisplay: ev,
          reason: `Checklist procédurale : motif ${ev}${chk.raison ? ' (' + chk.raison + ')' : ''}`,
        });
      }
      break;
    }

    // Question IA "oui"
    for (const q of this.aiQuestionsSignal()) {
      if (q.critereCode?.toUpperCase() !== 'IM05_MOTIF') continue;
      const answer = q.answerText?.trim().toLowerCase();
      if (!answer) continue;
      const isOui = answer === 'oui' || answer.startsWith('oui ') || answer.startsWith('oui,') || answer.startsWith('oui.');
      if (!isOui) continue;
      const ev = q.expectedValue?.toUpperCase();
      if (!ev || !MOTIFS_ENUM.has(ev)) continue;
      if (ev === user) continue;
      builder.addSource('QUESTION_IA', {
        expectedDisplay: ev,
        reason: `Question complémentaire : "${q.questionText}" → "${q.answerText}"`,
      });
      break;
    }

    // IA typeTitreSejourCode via CODE_TO_MOTIF
    const iaCode = this.aiDataSignal()?.typeTitreSejourCode?.toUpperCase();
    if (iaCode && CODE_TO_MOTIF[iaCode]) {
      const iaMotif = CODE_TO_MOTIF[iaCode];
      if (iaMotif !== user) {
        builder.addSource('IA', {
          expectedDisplay: iaMotif,
          reason: `Analyse du dossier : code ${iaCode} → motif ${iaMotif}`,
        });
      }
    }

    // Pièce manquante (contributor additionnel si une alerte est déjà initiée).
    if (pieceTexte) {
      builder.addPieceManquante(pieceTexte);
    }

    return builder.build();
  }

  private buildNationaliteAlert(): IM05CoherenceAlert | null {
    const iaNat = this.aiDataSignal()?.nationaliteUe;
    if (typeof iaNat !== 'boolean') return null;
    if (iaNat === this.nationaliteUe()) return null;
    return CoherenceAlertBuilder.forField<IM05AlertField>('NATIONALITE_UE')
      .addSource('IA', {
        expectedDisplay: iaNat ? 'Ressortissant UE' : 'Pays tiers',
        reason: `Analyse du dossier : ${iaNat ? 'nationalité UE/EEE/Suisse' : 'pays tiers'}`,
      })
      .build();
  }

  toggleCollapsed(): void {
    this.collapsed.update(v => !v);
  }

  onMotifChange(): void {
    this.provenanceMotif.set(null);
    if (this.motif() !== 'FAMILLE') {
      this.situationFamiliale.set(null);
      this.provenanceSituationFamiliale.set(null);
    }
  }

  loadExisting(): void {
    this.loading.set(true);
    this.decisionService.get(this.caseFileId).subscribe({
      next: resp => {
        this.decision.set(resp);
        this.prefillForm(resp);
        this.showForm.set(false);
        this.loading.set(false);
      },
      error: () => {
        this.prefillFromAi();
        this.showForm.set(true);
        this.loading.set(false);
      },
    });
  }

  private prefillFromAi(): void {
    // F-236 SF-236-02 : délègue au helper pur partagé pour garantir la parité
    // runtime / static `getPrefillCount` (divergence impossible par construction).
    const input: PrefillCountInput = {
      aiData: this.aiData,
      triggerEvents: this.triggerEvents ?? undefined,
    };
    const ue = ImmigrationTitleDecisionPrefillRules.computeNationaliteUe(input);
    if (ue !== null) {
      this.nationaliteUe.set(ue);
      this.provenanceNationaliteUe.set('IA');
    }
    const motif = ImmigrationTitleDecisionPrefillRules.computeMotif(input);
    if (motif !== null) {
      this.motif.set(motif);
      this.provenanceMotif.set('IA');
    }
    const sit = ImmigrationTitleDecisionPrefillRules.computeSituationFamiliale(input);
    if (sit !== null) {
      this.situationFamiliale.set(sit);
      this.provenanceSituationFamiliale.set('IA');
    }
  }

  /** SF-IM-05-04 : effacer les badges IA quand l'avocat modifie manuellement. */
  onSituationFamilialeChange(): void {
    this.provenanceSituationFamiliale.set(null);
  }

  onNationaliteUeChange(): void {
    this.provenanceNationaliteUe.set(null);
  }

  resolve(): void {
    this.resolving.set(true);
    this.decisionService.resolve(this.caseFileId, {
      country: this.country(),
      nationaliteUe: this.nationaliteUe(),
      motif: this.motif(),
      duree: this.duree(),
      situationFamiliale: this.motif() === 'FAMILLE' ? this.situationFamiliale() : null,
    }).subscribe({
      next: resp => {
        this.decision.set(resp);
        this.showForm.set(false);
        this.resolving.set(false);
        this.refreshService?.triggerRefresh();
      },
      error: () => {
        this.resolving.set(false);
        this.snackBar.open('Erreur lors de l\'analyse', 'Fermer', { duration: 4000 });
      },
    });
  }

  editCriteria(): void {
    const d = this.decision();
    if (d) this.prefillForm(d);
    this.showForm.set(true);
  }

  private prefillForm(resp: TitleDecisionResponse): void {
    this.country.set(resp.country);
    this.nationaliteUe.set(resp.nationaliteUe);
    this.motif.set(resp.motif);
    this.duree.set(resp.duree);
    this.situationFamiliale.set(resp.situationFamiliale);
  }
}
