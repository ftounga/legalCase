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
import { MineursImmigrationService } from '../../core/services/mineurs-immigration.service';
import {
  DISPOSITIF_MINEUR_LABELS,
  DispositifMineur,
  MineursImmigrationRequest,
  MineursImmigrationResponse,
  VerdictEligibiliteMineur,
  dispositifLabel,
} from '../../core/models/mineurs-immigration.model';
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
import { MineursImmigrationPrefillRules } from './mineurs-immigration-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-IM-19-02 : champs F-IA-03 audités par l'outil "Mineurs étrangers".
 * - DATE_NAISSANCE : divergence si IA détecte une `dateNaissance` ≠ saisie.
 * - DATE_ENTREE : idem pour `dateEntreeFrance`.
 */
export type MineursAlertField = 'DATE_NAISSANCE' | 'DATE_ENTREE';

export type MineursAlertSource = CoherenceAlertSource;
export type MineursCoherenceAlert = CoherenceAlert<MineursAlertField>;

/**
 * SF-IM-19-02 : section frontend dédiée Mineurs étrangers (FR).
 * Régime CESEDA L.435-3 + R.321-3 + R.321-7 + Cciv art. 375 + CASF
 * L.221-2-2.
 *
 * Single-country FR — Belgique : bannière info ("régime mineurs étrangers
 * propre à la France") + form masqué (pas d'appel HTTP). L'équivalent BE
 * relève de la procédure tutelle MENA via DGDE (feature jumelle au
 * backlog F-IM-19-BE).
 *
 * Pattern de référence : `changement-statut-section` (SF-IM-11-02 PR #640).
 * Builder F-IA-03 partagé : `CoherenceAlertBuilder` (pattern
 * `immigration-title-decision-section`).
 */
@Component({
  selector: 'app-mineurs-immigration-section',
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
  templateUrl: './mineurs-immigration-section.component.html',
  styleUrl: './mineurs-immigration-section.component.scss',
})
export class MineursImmigrationSectionComponent implements OnInit, OnChanges {
  // F-JU-03 SF-JU-03-99c v2 — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-IM-19-mineurs';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'MINEURS ÉTRANGERS (FR)';
  static readonly TOOL_ICON = 'child_care';

  /** F-236 SF-236-02 — Délègue intégralement au helper pur partagé. */
  static getPrefillCount(input: PrefillCountInput): number {
    return MineursImmigrationPrefillRules.computePrefillCount(input);
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
   *  - `calculate()` POSTe sur `/api/v1/simulators/F-IM-19-mineurs/calculate`.
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
  result = signal<MineursImmigrationResponse | null>(null);

  // Form fields (signal-based).
  dispositifVise = signal<DispositifMineur | null>(null);
  dateNaissance = signal<string | null>(null);
  dateEntreeFrance = signal<string | null>(null);
  parentRegulier = signal<boolean>(false);
  isolementAvere = signal<boolean>(false);
  motifOrdrePublic = signal<boolean>(false);
  nationalite = signal<string | null>(null);

  /** Provenance IA — badges effaçables. */
  provenanceDateNaissance = signal<'IA' | null>(null);
  provenanceDateEntreeFrance = signal<'IA' | null>(null);
  provenanceNationalite = signal<'IA' | null>(null);

  /** Listes pour mat-radio. */
  readonly dispositifOptions = DISPOSITIF_MINEUR_LABELS;

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  /** Visibilité conditionnelle des champs selon dispositif. */
  showIsolement = computed<boolean>(() => this.dispositifVise() === 'MNA_ORDONNANCE_JE');
  showParentRegulier = computed<boolean>(() => this.dispositifVise() === 'TITRE_SEJOUR_L435_3');
  showMotifOrdrePublic = computed<boolean>(() => this.dispositifVise() === 'DCEM');
  showTirNote = computed<boolean>(() => this.dispositifVise() === 'TIR');
  showDateEntree = computed<boolean>(() => {
    const d = this.dispositifVise();
    return d === 'TITRE_SEJOUR_L435_3' || d === 'MNA_ORDONNANCE_JE';
  });

  /**
   * Calcul d'âge à la date du jour. Retourne null si dateNaissance absente
   * ou invalide.
   */
  ageAnnees = computed<number | null>(() => {
    const d = this.dateNaissance();
    if (!d) return null;
    const dn = new Date(d);
    if (isNaN(dn.getTime())) return null;
    const today = new Date();
    let age = today.getFullYear() - dn.getFullYear();
    const m = today.getMonth() - dn.getMonth();
    if (m < 0 || (m === 0 && today.getDate() < dn.getDate())) age -= 1;
    return age;
  });

  /**
   * Bandeau bloquant transversal : âge ≥ 18 ans avant POST.
   * Les 4 dispositifs sont strictement réservés aux mineurs.
   */
  isMajeurAlert = computed<boolean>(() => {
    const a = this.ageAnnees();
    return a !== null && a >= 18;
  });

  /** Alertes de cohérence F-IA-03 par field — gate uniquement en mode formulaire. */
  coherenceAlerts = computed<Partial<Record<MineursAlertField, MineursCoherenceAlert>>>(() => {
    // F-163 SF-163-02d : aucune source IA en standalone.
    if (this.standaloneMode) return {};
    if (!this.showForm()) return {};
    if (!this.isFrance()) return {};
    const alerts: Partial<Record<MineursAlertField, MineursCoherenceAlert>> = {};
    const dn = this.buildDateNaissanceAlert();
    if (dn) alerts.DATE_NAISSANCE = dn;
    const de = this.buildDateEntreeAlert();
    if (de) alerts.DATE_ENTREE = de;
    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    const blockers = values.filter((a) => a.severity === 'CRITICAL').length;
    return { total: values.length, blockers };
  });

  constructor(
    private service: MineursImmigrationService,
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

  formValid(): boolean {
    if (!this.dispositifVise()) return false;
    if (!this.dateNaissance()) return false;
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // Handlers de modification — effacent la provenance IA.
  onDispositifChange(value: DispositifMineur | null): void {
    this.dispositifVise.set(value);
  }

  onDateNaissanceChange(value: string | null): void {
    this.dateNaissance.set(value && value.trim() ? value : null);
    this.provenanceDateNaissance.set(null);
  }

  onDateEntreeChange(value: string | null): void {
    this.dateEntreeFrance.set(value && value.trim() ? value : null);
    this.provenanceDateEntreeFrance.set(null);
  }

  onNationaliteChange(value: string | null): void {
    this.nationalite.set(value && value.trim() ? value.trim() : null);
    this.provenanceNationalite.set(null);
  }

  onParentRegulierChange(value: boolean): void {
    this.parentRegulier.set(value);
  }

  onIsolementChange(value: boolean): void {
    this.isolementAvere.set(value);
  }

  onMotifOpChange(value: boolean): void {
    this.motifOrdrePublic.set(value);
  }

  /**
   * Bandeau verdict :
   * - ELEVEE → navy/info, dispositif éligible.
   * - MOYENNE → or/warning, conditions limites (minorité contestable).
   * - FAIBLE → rouge alerte, critère bloquant.
   */
  bannerClass(verdict: VerdictEligibiliteMineur | string | undefined | null): string {
    switch (verdict) {
      case 'ELEVEE': return 'mi-banner mi-banner--info';
      case 'MOYENNE': return 'mi-banner mi-banner--warning';
      case 'FAIBLE': return 'mi-banner mi-banner--critical';
      default: return 'mi-banner mi-banner--info';
    }
  }

  bannerIcon(verdict: VerdictEligibiliteMineur | string | undefined | null): string {
    switch (verdict) {
      case 'ELEVEE': return 'verified';
      case 'MOYENNE': return 'warning';
      case 'FAIBLE': return 'gpp_bad';
      default: return 'info_outline';
    }
  }

  bannerLabel(verdict: VerdictEligibiliteMineur | string | undefined | null): string {
    switch (verdict) {
      case 'ELEVEE': return 'Dispositif éligible';
      case 'MOYENNE': return 'Conditions limites — éligibilité contestable';
      case 'FAIBLE': return 'Dispositif non applicable';
      default: return '';
    }
  }

  /** Libellé humain depuis un code dispositif (export model). */
  dispositifLabel(code: string | null | undefined): string {
    return dispositifLabel(code as DispositifMineur | null);
  }

  /**
   * SF-IM-19-02 : pré-fill depuis `aiData` (ImmigrationExtractedData).
   * Règles (fail-open) :
   * - passe silencieusement si aiData absent
   * - ne pré-remplit QUE si le champ est encore vide (préserve les edits)
   * - n'écrase jamais si provenance !== 'IA'
   *
   * Note : les champs `dateNaissance`, `dateEntreeFrance`, `nationalite`
   * ne sont pas encore présents dans `ImmigrationExtractedData` (cast
   * défensif `any`). Extension future via SF dédiée pipeline IA.
   */
  private prefillFromAi(): void {
    // F-236 SF-236-02 : délègue au helper pur partagé.
    const input: PrefillCountInput = {
      aiData: this.aiDataSignal(),
      workspaceCountry: this.workspaceCountry,
    };
    const dN = MineursImmigrationPrefillRules.computeDateNaissance(input);
    if (dN !== null
        && (this.dateNaissance() === null || this.provenanceDateNaissance() === 'IA')) {
      this.dateNaissance.set(dN);
      this.provenanceDateNaissance.set('IA');
    }
    const dE = MineursImmigrationPrefillRules.computeDateEntreeFrance(input);
    if (dE !== null
        && (this.dateEntreeFrance() === null || this.provenanceDateEntreeFrance() === 'IA')) {
      this.dateEntreeFrance.set(dE);
      this.provenanceDateEntreeFrance.set('IA');
    }
    const nat = MineursImmigrationPrefillRules.computeNationalite(input);
    if (nat !== null
        && (this.nationalite() === null || this.provenanceNationalite() === 'IA')) {
      this.nationalite.set(nat);
      this.provenanceNationalite.set('IA');
    }
  }

  /**
   * Divergence date naissance — IA détecte une dateNaissance différente
   * de la saisie avocat.
   */
  private buildDateNaissanceAlert(): MineursCoherenceAlert | null {
    const userVal = this.dateNaissance();
    if (!userVal) return null;

    const builder = CoherenceAlertBuilder.forField<MineursAlertField>('DATE_NAISSANCE');

    // 1. F-96
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'IM19_DATE_NAISSANCE') continue;
      const ev = chk.expectedValue;
      if (!ev || ev === userVal) continue;
      builder.addSource('F96', {
        expectedDisplay: ev,
        reason: `Checklist procédurale : date naissance attendue ${ev}`
          + (chk.raison ? ` (${chk.raison})` : ''),
      });
      break;
    }

    // 2. QUESTION_IA
    for (const q of this.aiQuestionsSignal()) {
      if (q.critereCode?.toUpperCase() !== 'IM19_DATE_NAISSANCE') continue;
      const ev = q.expectedValue;
      if (!ev || ev === userVal) continue;
      builder.addSource('QUESTION_IA', {
        expectedDisplay: ev,
        reason: `Question complémentaire : "${q.questionText}" → "${q.answerText}"`,
      });
      break;
    }

    // 3. IA — `aiData.mineursDateNaissance` (SF-246-19 : accès typé, cast supprimé).
    const ai = this.aiDataSignal();
    const iaVal: string | null = ai?.mineursDateNaissance ?? null;
    if (iaVal && iaVal !== userVal) {
      builder.addSource('IA', {
        expectedDisplay: iaVal,
        reason: `Analyse du dossier : date naissance détectée ${iaVal}`,
      });
    }

    // 4. PIECE_MANQUANTE — contributor enrichissant.
    const piece = this.findPieceManquante(['IM19_DATE_NAISSANCE', 'ACTE_NAISSANCE']);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  /**
   * Divergence date entrée France — IA détecte une dateEntreeFrance
   * différente de la saisie avocat.
   */
  private buildDateEntreeAlert(): MineursCoherenceAlert | null {
    const userVal = this.dateEntreeFrance();
    if (!userVal) return null;

    const builder = CoherenceAlertBuilder.forField<MineursAlertField>('DATE_ENTREE');

    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'IM19_DATE_ENTREE') continue;
      const ev = chk.expectedValue;
      if (!ev || ev === userVal) continue;
      builder.addSource('F96', {
        expectedDisplay: ev,
        reason: `Checklist procédurale : date entrée attendue ${ev}`
          + (chk.raison ? ` (${chk.raison})` : ''),
      });
      break;
    }

    for (const q of this.aiQuestionsSignal()) {
      if (q.critereCode?.toUpperCase() !== 'IM19_DATE_ENTREE') continue;
      const ev = q.expectedValue;
      if (!ev || ev === userVal) continue;
      builder.addSource('QUESTION_IA', {
        expectedDisplay: ev,
        reason: `Question complémentaire : "${q.questionText}" → "${q.answerText}"`,
      });
      break;
    }

    // SF-246-19 : accès typé via aesDateEntreeFrance (cast supprimé).
    const ai = this.aiDataSignal();
    const iaVal: string | null = ai?.aesDateEntreeFrance ?? null;
    if (iaVal && iaVal !== userVal) {
      builder.addSource('IA', {
        expectedDisplay: iaVal,
        reason: `Analyse du dossier : date entrée détectée ${iaVal}`,
      });
    }

    const piece = this.findPieceManquante(['IM19_DATE_ENTREE', 'JUSTIFICATIF_ENTREE_FRANCE']);
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

  alertTooltip(alert: MineursCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: MineursCoherenceAlert): string {
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
   * Classe CSS d'un critère non rempli selon sa nature. Heuristique :
   * - "majorité", "ordre public", "bloquant" → rouge (critical).
   * - autre → or (warning).
   */
  riskChipClass(risk: string): string {
    if (!risk) return 'mi-chip-risk mi-chip-risk--warning';
    const r = risk.toLowerCase();
    if (r.includes('majorité') || r.includes('ordre public')
        || r.includes('bloquant') || r.includes('non avéré')
        || r.includes('non régulière') || r.includes('non en france')) {
      return 'mi-chip-risk mi-chip-risk--critical';
    }
    return 'mi-chip-risk mi-chip-risk--warning';
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request: MineursImmigrationRequest = {
      dispositifVise: this.dispositifVise()!,
      dateNaissance: this.dateNaissance()!,
    };
    if (this.dateEntreeFrance()) request.dateEntreeFrance = this.dateEntreeFrance()!;
    if (this.nationalite()) request.nationalite = this.nationalite()!;
    // Toggles : envoyés selon le dispositif (le backend les gère défensivement).
    if (this.showIsolement()) request.isolementAvere = this.isolementAvere();
    if (this.showParentRegulier()) request.parentRegulier = this.parentRegulier();
    if (this.showMotifOrdrePublic()) request.motifOrdrePublic = this.motifOrdrePublic();

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
        this.snackBar.open('Éligibilité mineur étranger analysée', 'OK', { duration: 2500 });
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
        // Hydrate le form avec les valeurs persistées.
        this.dispositifVise.set(r.dispositifVise as DispositifMineur);
        this.dateNaissance.set(r.dateNaissance);
        this.dateEntreeFrance.set(r.dateEntreeFrance);
        this.parentRegulier.set(r.parentRegulier);
        this.isolementAvere.set(r.isolementAvere);
        this.motifOrdrePublic.set(r.motifOrdrePublic);
        this.nationalite.set(r.nationalite);
        // Provenance reset — valeurs persistées = saisie avocat.
        this.provenanceDateNaissance.set(null);
        this.provenanceDateEntreeFrance.set(null);
        this.provenanceNationalite.set(null);
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
