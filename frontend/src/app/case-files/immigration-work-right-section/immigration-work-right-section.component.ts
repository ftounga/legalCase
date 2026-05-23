import { Component, Input, OnInit, OnChanges, SimpleChanges, Optional, signal, computed } from '@angular/core';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar } from '@angular/material/snack-bar';
import { FormsModule } from '@angular/forms';
import { ImmigrationWorkRightService } from '../../core/services/immigration-work-right.service';
import { WorkRightResponse } from '../../core/models/immigration-work-right.model';
import { ImmigrationExtractedData, PieceManquanteEntry } from '../../core/models/case-analysis.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { AiQuestion } from '../../core/models/ai-question.model';
import { SourceExplanationService } from '../../core/services/source-explanation.service';
import { SourceExplanation } from '../../core/models/source-explanation.model';
import { CoherencePopoverTriggerDirective } from '../../shared/coherence-popover/coherence-popover-trigger.directive';
import { CoherenceAlert, CoherenceAlertSource } from '../../shared/coherence-popover/coherence-alert.model';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';
import { CoherenceAlertBuilder } from '../../shared/coherence-popover/coherence-alert-builder';
import { ProcedureCheckAlignment } from '../../core/models/procedure-check-alignment.model';
import { ProcedureChecksOutputComponent } from '../decisional-tools-panel/procedure-checks-output/procedure-checks-output.component';
import { computeBadge, ProcedureChecksBadge } from '../decisional-tools-panel/procedure-check-badge.helper';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import {
  ImmigrationWorkRightPrefillRules,
  FR_TITRE_CODES,
  BE_TITRE_CODES,
  ALL_TITRE_CODES,
} from './immigration-work-right-section-prefill-rules';

/**
 * SF-155-11 : fields d'alerte de cohérence F-IA-03 exposés par l'outil
 * F-IM-07 (droit au travail). Un par field pré-remplissable.
 */
export type IM07AlertField = 'TITRE_TYPE' | 'COUNTRY';

// SF-155-11 : alias rétro-compat — utilise l'interface générique partagée
// `CoherenceAlert<F>` / `CoherenceAlertSource` (cf. SF-155-05 DIV-1).
export type IM07AlertSource = CoherenceAlertSource;
export type IM07CoherenceAlert = CoherenceAlert<IM07AlertField>;

@Component({
  selector: 'app-immigration-work-right-section',
  standalone: true,
  imports: [
    FormsModule,
    MatButtonModule, MatIconModule,
    MatSelectModule, MatFormFieldModule, MatInputModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    CoherencePopoverTriggerDirective,
    ProcedureChecksOutputComponent,
    ToolJurisprudenceCitationsComponent
  ],
  templateUrl: './immigration-work-right-section.component.html',
  styleUrl: './immigration-work-right-section.component.scss'
})
export class ImmigrationWorkRightSectionComponent implements OnInit, OnChanges {
  // F-JU-03 SF-JU-03-03 — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-IM-07-droit-au-travail';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'DROIT AU TRAVAIL';
  static readonly TOOL_ICON = 'work';

  /**
   * F-236 SF-236-02 — Délègue intégralement au helper pur partagé
   * `ImmigrationWorkRightPrefillRules`. Le runtime `prefillFromAi()`
   * consomme la même fonction sur le même input — divergence impossible
   * par construction.
   *
   * `country` n'est pas comptabilisé : le runtime aligne juste la valeur
   * locale sur `workspaceCountry` (cohérence interne, pas un champ IA).
   */
  static getPrefillCount(input: PrefillCountInput): number {
    return ImmigrationWorkRightPrefillRules.computePrefillCount(input);
  }

  /** F-193 SF-193-02 — Pattern miroir cf. licenciement-section. */
  static getProcedureChecksBadge(input: {
    proceduresChecksAlignment?: ProcedureCheckAlignment[] | null;
  }): ProcedureChecksBadge {
    return computeBadge(Array.isArray(input.proceduresChecksAlignment) ? input.proceduresChecksAlignment : []);
  }

  @Input() caseFileId!: string;
  @Input() workspaceCountry: string = 'FRANCE';
  // SF-155-11 : inputs IA (tous optionnels — null-safe partout).
  @Input() aiData?: ImmigrationExtractedData | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;
  /** F-193 SF-193-02 — alignement F-96 pré-filtré sur cet outil. */
  @Input() proceduresChecksAlignment?: ProcedureCheckAlignment[] | null;
  // F-177 SF-177-03b : force l'expansion (mode modal F-177).
  @Input() forceExpanded = false;

  // SF-155-11 : snapshots signal des inputs IA pour que `computed` réagisse.
  // Même pattern qu'`immigration-title-decision-section` (F-IM-05).
  private aiDataSignal = signal<ImmigrationExtractedData | null | undefined>(undefined);
  private procedureChecksSignal = signal<ProcedureCheck[]>([]);
  private aiQuestionsSignal = signal<AiQuestion[]>([]);
  private piecesManquantesSignal = signal<PieceManquanteEntry[]>([]);

  collapsed = signal(true);
  loading = signal(false);
  resolving = signal(false);
  showForm = signal(true);
  result = signal<WorkRightResponse | null>(null);

  titreType = signal('VLS_TS_SALARIE');
  country = signal('FRANCE');

  // SF-155-11 : provenance IA par champ pré-rempli. 'IA' quand le champ a été
  // posé par `prefillFromAi()` ; remis à null dès que l'avocat modifie
  // manuellement (onXxxChange) — pattern canonique F-IM-05.
  provenanceTitreType = signal<'IA' | null>(null);
  provenanceCountry = signal<'IA' | null>(null);

  readonly titresFrance = [
    { value: 'VLS_TS_ETUDIANT', label: 'VLS-TS Étudiant' },
    { value: 'VLS_TS_SALARIE', label: 'VLS-TS Salarié' },
    { value: 'CST_SALARIE', label: 'CST Salarié' },
    // SF-IM-07-04 : le générique reste pour rétro-compat mais renvoie un
    // message CONDITIONNEL invitant à choisir un sous-type.
    { value: 'CARTE_PLURIANNUELLE', label: 'Carte pluriannuelle (générique — préciser le motif)' },
    { value: 'CARTE_PLURIANNUELLE_ETUDIANT_RECHERCHE', label: 'Carte pluriannuelle — Étudiant / Étudiant-Recherche' },
    { value: 'CARTE_PLURIANNUELLE_SALARIE', label: 'Carte pluriannuelle — Salarié' },
    { value: 'CARTE_PLURIANNUELLE_PASSEPORT_TALENT', label: 'Carte pluriannuelle — Passeport Talent' },
    { value: 'CARTE_PLURIANNUELLE_VPF', label: 'Carte pluriannuelle — Vie privée et familiale' },
    { value: 'CARTE_RESIDENT', label: 'Carte de résident' },
    { value: 'APS', label: 'APS' },
    { value: 'CST_VPF', label: 'Carte vie privée et familiale (générique)' },
    { value: 'CST_VPF_CONJOINT_FR', label: 'CST VPF — Conjoint de Français (L.423-1)' },
    { value: 'RECEPISSE_ASILE', label: 'Récépissé demande d\'asile' },
  ];

  readonly titresBelgique = [
    { value: 'CARTE_A_TRAVAIL', label: 'Carte A — Travail' },
    { value: 'CARTE_A_ETUDES', label: 'Carte A — Études' },
    { value: 'CARTE_A_FAMILLE', label: 'Carte A — Famille' },
    { value: 'CARTE_B', label: 'Carte B' },
    { value: 'CARTE_C', label: 'Carte C' },
    { value: 'PERMIS_UNIQUE', label: 'Permis unique' },
    { value: 'ANNEXE_15', label: 'Annexe 15' },
    { value: 'ATTESTATION_IMMATRICULATION', label: 'Attestation d\'immatriculation' },
  ];

  get titresForCountry() {
    return this.country() === 'BELGIQUE' ? this.titresBelgique : this.titresFrance;
  }

  // SF-155-11 : alertes de cohérence F-IA-03 via CoherenceAlertBuilder partagé.
  // Avant SF-155-11 : alerte unique (coherenceAlert) définie localement — dette
  // de convergence corrigée (cf. F-155 audit 2026-04-25).
  // Gate : uniquement en mode formulaire (pattern anti-bug SF-IA-03-12).
  coherenceAlerts = computed<Partial<Record<IM07AlertField, IM07CoherenceAlert>>>(() => {
    if (!this.showForm()) return {};
    const alerts: Partial<Record<IM07AlertField, IM07CoherenceAlert>> = {};
    const titreAlert = this.buildTitreTypeAlert();
    if (titreAlert) alerts.TITRE_TYPE = titreAlert;
    const countryAlert = this.buildCountryAlert();
    if (countryAlert) alerts.COUNTRY = countryAlert;
    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    return { total: values.length, blockers: 0 };
  });

  // SF-IA-03-15c — map {sourceKey → explanation}
  sourceExplanations = signal<Map<string, SourceExplanation[]>>(new Map());

  constructor(
    private workRightService: ImmigrationWorkRightService,
    // SF-155-11 : `@Optional()` pour cohérence stricte avec le canonique
    // `harcelement-licenciement-nul-section`. Fail-open si service absent.
    @Optional() private sourceExplanationService: SourceExplanationService | null,
    private snackBar: MatSnackBar,
    @Optional() private refreshService: CaseDashboardRefreshService | null,
  ) {}

  private loadSourceExplanations(): void {
    if (!this.caseFileId || !this.sourceExplanationService) return;
    this.sourceExplanationService.getForCaseFile(this.caseFileId).subscribe({
      next: map => this.sourceExplanations.set(map),
      error: () => { /* fail-open */ },
    });
  }

  /** SF-IA-03-15c : mapping field → sourceKey pour le popover. */
  explanationFor(field: IM07AlertField): SourceExplanation[] {
    const key = field === 'TITRE_TYPE' ? 'IM07_TITRE_TYPE' : 'IM07_COUNTRY';
    return this.sourceExplanations().get(key) ?? [];
  }

  ngOnInit(): void {
    // F-177 SF-177-03b : appliqué dès le mount pour le mode modal.
    if (this.forceExpanded) this.collapsed.set(false);
    // SF-155-11 : push inputs vers signals avant toute évaluation computed.
    this.aiDataSignal.set(this.aiData);
    this.procedureChecksSignal.set(this.procedureChecks ?? []);
    this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    this.piecesManquantesSignal.set(this.piecesManquantes ?? []);
    // Défaut pays : aligné sur le workspace (avocat belge démarre en BELGIQUE).
    this.country.set(this.workspaceCountry);
    if (this.workspaceCountry === 'BELGIQUE') {
      this.titreType.set(this.titresBelgique[0].value);
    }
    // SF-IM-07-05 / SF-155-11 : garde-fou — pré-remplit au mount si aiData
    // est déjà disponible (pipeline déjà tourné avant ouverture du dossier).
    this.prefillFromAi();
    this.loadSourceExplanations();
    this.loadExisting();
  }

  ngOnChanges(changes: SimpleChanges): void {
    // F-177 SF-177-03b : applique le forceExpanded quand il passe à true en cours de vie.
    if (changes['forceExpanded'] && this.forceExpanded) this.collapsed.set(false);
    if (changes['aiData']) this.aiDataSignal.set(this.aiData);
    if (changes['procedureChecks']) this.procedureChecksSignal.set(this.procedureChecks ?? []);
    if (changes['aiQuestions']) this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    if (changes['piecesManquantes']) this.piecesManquantesSignal.set(this.piecesManquantes ?? []);
    // SF-155-11 : ré-applique le pré-fill dès que aiData OU workspaceCountry
    // change avant première résolution (ne jamais écraser après GET 200).
    if ((changes['aiData'] || changes['workspaceCountry']) && this.showForm() && !this.result()) {
      if (changes['workspaceCountry']) {
        this.country.set(this.workspaceCountry);
        this.provenanceCountry.set('IA');
      }
      this.prefillFromAi();
    }
  }

  private buildPiecesIndex(pieces: PieceManquanteEntry[]): Record<string, string> {
    const index: Record<string, string> = {};
    if (!pieces) return index;
    for (const p of pieces) {
      const code = p.critereCode?.toUpperCase();
      if (!code) continue;
      if (code !== 'IM07_TITRE_TYPE') continue;
      if (!index[code]) index[code] = p.texte;
    }
    return index;
  }

  toggleCollapsed(): void {
    this.collapsed.update(v => !v);
  }

  /** SF-155-11 : handler change titre — efface le badge IA (pattern canonique). */
  onTitreTypeChange(): void {
    this.provenanceTitreType.set(null);
  }

  /**
   * SF-155-11 : handler change pays — efface le badge IA. Le champ pays est
   * techniquement en lecture seule (driven par workspaceCountry) mais le
   * handler est exposé pour cohérence avec le pattern canonique et pour
   * couvrir un futur changement manuel de pays (déplacement dossier).
   */
  onCountryChange(): void {
    this.provenanceCountry.set(null);
  }

  private prefillFromAi(): void {
    // 1. Pays : toujours aligné sur le workspace (source authoritative).
    //    Conservé en logique composant — n'est PAS un champ pré-rempli IA
    //    au sens du contrat (non compté par getPrefillCount).
    if (this.country() !== this.workspaceCountry) {
      this.country.set(this.workspaceCountry);
    }
    this.provenanceCountry.set('IA');

    // 2. F-236 SF-236-02 : titre de séjour via le helper pur partagé
    //    (parité runtime/static garantie par construction).
    const input: PrefillCountInput = {
      aiData: this.aiData,
      workspaceCountry: this.country(),
    };
    const titre = ImmigrationWorkRightPrefillRules.computeTitreType(input);
    if (titre !== null) {
      this.titreType.set(titre);
      this.provenanceTitreType.set('IA');
    }
  }

  alertTooltip(alert: IM07CoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: IM07CoherenceAlert): string {
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
   * SF-155-11 : divergence titre de séjour — 4 sources (F96, QUESTION_IA, IA,
   * PIECE_MANQUANTE) via `CoherenceAlertBuilder` partagé. Hiérarchie :
   * F-96 > Question IA > IA détection > Pièce manquante (règle F-IA-03).
   */
  private buildTitreTypeAlert(): IM07CoherenceAlert | null {
    const user = this.titreType();
    if (!user) return null;

    const piecesIndex = this.buildPiecesIndex(this.piecesManquantesSignal());
    const pieceTexte = piecesIndex['IM07_TITRE_TYPE'] ?? null;
    const builder = CoherenceAlertBuilder.forField<IM07AlertField>('TITRE_TYPE');

    // 1. F-96 VERIFIED — critereCode `IM07_TITRE_TYPE` ciblé.
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'IM07_TITRE_TYPE') continue;
      if (chk.statut !== 'VERIFIED') continue;
      const ev = chk.expectedValue?.toUpperCase();
      if (!ev || !ALL_TITRE_CODES.has(ev)) continue;
      if (ev === user) continue;
      builder.addSource('F96', {
        expectedDisplay: ev,
        reason: `Checklist procédurale : ${ev}${chk.raison ? ' (' + chk.raison + ')' : ''}`,
      });
      break;
    }

    // 2. QUESTION_IA — réponse "oui" sur `IM07_TITRE_TYPE` avec `expectedValue`.
    for (const q of this.aiQuestionsSignal()) {
      if (q.critereCode?.toUpperCase() !== 'IM07_TITRE_TYPE') continue;
      const answer = q.answerText?.trim().toLowerCase();
      if (!answer) continue;
      const isOui = answer === 'oui' || answer.startsWith('oui ')
        || answer.startsWith('oui,') || answer.startsWith('oui.');
      if (!isOui) continue;
      const ev = q.expectedValue?.toUpperCase();
      if (!ev || !ALL_TITRE_CODES.has(ev)) continue;
      if (ev === user) continue;
      builder.addSource('QUESTION_IA', {
        expectedDisplay: ev,
        reason: `Question complémentaire : "${q.questionText}" → "${q.answerText}"`,
      });
      break;
    }

    // 3. IA détection — aiData.typeTitreSejourCode.
    const iaCode = this.aiDataSignal()?.typeTitreSejourCode?.toUpperCase();
    if (iaCode && ALL_TITRE_CODES.has(iaCode) && iaCode !== user) {
      builder.addSource('IA', {
        expectedDisplay: iaCode,
        reason: `Analyse du dossier : ${iaCode}`,
      });
    }

    // 4. PIECE_MANQUANTE — contributor si alerte déjà initiée.
    if (pieceTexte) builder.addPieceManquante(pieceTexte);

    return builder.build();
  }

  /**
   * SF-155-11 : divergence pays — alerte si l'IA détecte un code titre d'un
   * autre pays que le workspace courant (ex. avocat FR ouvre un dossier avec
   * un titre BE `CARTE_B` extrait par l'IA). Ce cas signale souvent une
   * erreur de rattachement workspace, ou un dossier cross-border à ré-aiguiller.
   */
  private buildCountryAlert(): IM07CoherenceAlert | null {
    const iaCode = this.aiDataSignal()?.typeTitreSejourCode?.toUpperCase();
    if (!iaCode) return null;
    const isFR = FR_TITRE_CODES.has(iaCode);
    const isBE = BE_TITRE_CODES.has(iaCode);
    if (!isFR && !isBE) return null;
    const iaCountry = isFR ? 'FRANCE' : 'BELGIQUE';
    if (iaCountry === this.country()) return null;
    return CoherenceAlertBuilder.forField<IM07AlertField>('COUNTRY')
      .addSource('IA', {
        expectedDisplay: iaCountry === 'FRANCE' ? 'France' : 'Belgique',
        reason: `Analyse du dossier : code titre ${iaCode} rattaché à ${iaCountry === 'FRANCE' ? 'la France' : 'la Belgique'}`,
      })
      .build();
  }

  loadExisting(): void {
    this.loading.set(true);
    this.workRightService.get(this.caseFileId).subscribe({
      next: resp => {
        this.result.set(resp);
        this.country.set(resp.country);
        this.titreType.set(resp.titreType);
        // SF-155-11 : valeurs persistées = saisie avocat — jamais de badge IA.
        this.provenanceTitreType.set(null);
        this.provenanceCountry.set(null);
        this.showForm.set(false);
        this.loading.set(false);
      },
      error: () => {
        this.showForm.set(true);
        this.loading.set(false);
        // SF-155-11 : fallback pré-fill IA uniquement si pas de résultat persisté.
        this.prefillFromAi();
      },
    });
  }

  resolve(): void {
    this.resolving.set(true);
    this.workRightService.resolve(this.caseFileId, {
      titreType: this.titreType(),
      country: this.country(),
    }).subscribe({
      next: resp => {
        this.result.set(resp);
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

  editForm(): void {
    const r = this.result();
    if (r) {
      this.country.set(r.country);
      this.titreType.set(r.titreType);
    }
    this.showForm.set(true);
  }
}
