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
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { RequalificationInterimCdiService } from '../../core/services/requalification-interim-cdi.service';
import {
  MissionInterim,
  MOTIF_INTERIM_INVOQUE_OPTIONS,
  MOTIF_INTERDIT_TYPE_INTERIM_OPTIONS,
  MotifInterimInvoque,
  MotifInterditTypeInterim,
  RequalificationInterimCdiRequest,
  RequalificationInterimCdiResponse,
  VerdictProbabiliteRequalificationInterim,
  motifInterimInvoqueLabel,
  motifInterditTypeInterimLabel,
  verdictProbabiliteInterimLabel,
} from '../../core/models/requalification-interim-cdi.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import {
  PieceManquanteEntry,
  TravailExtractedData,
} from '../../core/models/case-analysis.model';
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
import {
  RequalificationInterimCdiSectionPrefillRules,
} from './requalification-interim-cdi-section-prefill-rules';

/**
 * SF-DT-23-02 : champs d'alerte de cohérence F-IA-03 exposés par l'outil
 * F-DT-23 (requalification intérim → CDI).
 */
export type RequalificationInterimAlertField = 'SALAIRE_MENSUEL';

export type RequalificationInterimAlertSource = CoherenceAlertSource;
export type RequalificationInterimCoherenceAlert = CoherenceAlert<RequalificationInterimAlertField>;

/**
 * Seuil d'écart relatif (10 %) au-delà duquel on déclenche une alerte sur
 * le salaire mensuel — aligné avec F-DT-17 / F-DT-21 / F-DT-22 / template.
 */
const SALAIRE_DIVERGENCE_RATIO = 0.10;

/**
 * SF-DT-23-02 : outil décisionnel "Requalification intérim → CDI" (F-DT-23).
 * France uniquement. Consomme l'API figée SF-DT-23-01.
 *
 * Affiché conditionnellement par le panel F-IA-04 (tool_id
 * `F-DT-23-requalification-interim-cdi`, règle ALWAYS_ON FR + travail).
 *
 * Pattern canonique : `harcelement-licenciement-nul-section` (cf.
 * `ai-skills/frontend-coherence-audit.md` §5). Pattern jumeau direct :
 * `requalification-cdd-cdi-section` (F-DT-22-02, structure quasi-identique
 * — diff : MissionInterim ajoute `entrepriseUtilisatrice` + toggle
 * `memeEntrepriseUtilisatrice` + indemnité 10 % L.1251-32).
 *
 * Pré-fill IA : `salaireMensuelBrutEur` depuis `aiData.salaireBrutMensuel`.
 * Validation F-IA-03 : alerte sur SALAIRE_MENSUEL via `CoherenceAlertBuilder`
 * (multi-sources IA / F96 / QUESTION_IA / PIECE_MANQUANTE).
 */
@Component({
  selector: 'app-requalification-interim-cdi-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule, DecimalPipe,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatSelectModule, MatSlideToggleModule, MatChipsModule,
    MatProgressSpinnerModule,
    LegalCitationsPipe,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './requalification-interim-cdi-section.component.html',
  styleUrl: './requalification-interim-cdi-section.component.scss',
})
export class RequalificationInterimCdiSectionComponent implements OnInit, OnChanges {
  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'REQUALIFICATION INTÉRIM → CDI (FR) — ART. L.1251-40';

  /** F-177 SF-177-12 / F-236 SF-236-02 — délègue au helper partagé (parité runtime). */
  static getPrefillCount(input: {
    aiData?: any;
    procedureChecks?: any[];
    aiQuestions?: any[];
    piecesManquantes?: any[];
    triggerEvents?: any[];
    workspaceCountry?: string;
  }): number {
    return RequalificationInterimCdiSectionPrefillRules.computePrefillCount({
      aiData: input.aiData,
      workspaceCountry: input.workspaceCountry,
    });
  }

  static readonly TOOL_ICON = 'swap_horiz';

  @Input() caseFileId!: string;
  // F-177 SF-177-03b : force l'expansion (mode modal F-177).
  @Input() forceExpanded = false;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';
  @Input() aiData?: TravailExtractedData | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;

  /**
   * F-163 SF-163-02b — Mode simulateur autonome (hors dossier client).
   * Quand `true` : bannière simulateur affichée, prefillFromAi() / coherenceAlerts /
   * loadExisting() / triggerRefresh() court-circuités, POST routé vers le dispatcher
   * générique /api/v1/simulators/{toolId}/calculate (SF-163-03).
   */
  @Input() standaloneMode: boolean = false;

  private aiDataSignal = signal<TravailExtractedData | null | undefined>(undefined);
  private procedureChecksSignal = signal<ProcedureCheck[]>([]);
  private aiQuestionsSignal = signal<AiQuestion[]>([]);
  private piecesManquantesSignal = signal<PieceManquanteEntry[]>([]);

  readonly motifInterimOptions = MOTIF_INTERIM_INVOQUE_OPTIONS;
  readonly motifInterditOptions = MOTIF_INTERDIT_TYPE_INTERIM_OPTIONS;

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<RequalificationInterimCdiResponse | null>(null);

  // Form state.
  motifInterimInvoque = signal<MotifInterimInvoque | null>(null);
  motifInterdit = signal<boolean>(false);
  motifInterditType = signal<MotifInterditTypeInterim | null>(null);
  successionMissions = signal<MissionInterim[]>([]);
  delaiCarenceRespecte = signal<boolean>(true);
  dureeMissionsTotaleMois = signal<number | null>(null);
  salaireMensuelBrutEur = signal<number | null>(null);
  dateFinDerniereMission = signal<string | null>(null);
  memeEntrepriseUtilisatrice = signal<boolean>(false);

  // Édition succession missions intérim : champs courants.
  newMissionDateDebut = signal<string | null>(null);
  newMissionDateFin = signal<string | null>(null);
  newMissionMotif = signal<string>('');
  newMissionEntrepriseUtilisatrice = signal<string>('');

  provenanceSalaire = signal<'IA' | null>(null);

  sourceExplanations = signal<Map<string, SourceExplanation[]>>(new Map());

  // SF-DT-23-02 / SF-155 : alertes de cohérence F-IA-03 calculées dynamiquement.
  // Gate strict `showForm()` (anti-bug SF-IA-03-12 — pas d'alertes en mode lecture).
  coherenceAlerts = computed<Partial<Record<RequalificationInterimAlertField, RequalificationInterimCoherenceAlert>>>(() => {
    // F-163 SF-163-02b : aucune source IA en standalone.
    if (this.standaloneMode) return {} as any;
    if (!this.showForm()) return {};
    const alerts: Partial<Record<RequalificationInterimAlertField, RequalificationInterimCoherenceAlert>> = {};
    const salaireAlert = this.buildSalaireAlert();
    if (salaireAlert) alerts.SALAIRE_MENSUEL = salaireAlert;
    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    return { total: values.length, blockers: 0 };
  });

  isFrance = computed(() => this.workspaceCountry === 'FRANCE');

  constructor(
    private service: RequalificationInterimCdiService,
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

    if (changes['aiData'] && !changes['aiData'].firstChange
        && this.showForm() && !this.result()) {
      if (!this.standaloneMode) this.prefillFromAi();
    }
  }

  /**
   * SF-DT-23-02 : pré-remplit `salaireMensuelBrutEur` depuis l'analyse IA.
   * N'écrase jamais une saisie manuelle (provenance null sur un champ rempli).
   */
  private prefillFromAi(): void {
    // F-163 SF-163-02b : aucune source IA en standalone.
    if (this.standaloneMode) return;
    const ai = this.aiDataSignal();
    if (!ai) return;
    if (!this.isFrance()) return;

    // F-236 SF-236-02 : valeur calculée par le helper partagé (parité static).
    const salaire = RequalificationInterimCdiSectionPrefillRules.computeSalaireMensuelBrutEur({
      aiData: ai, workspaceCountry: this.workspaceCountry,
    });
    if (salaire !== null) {
      if (this.salaireMensuelBrutEur() === null || this.provenanceSalaire() === 'IA') {
        this.salaireMensuelBrutEur.set(salaire);
        this.provenanceSalaire.set('IA');
      }
    }
  }

  private loadSourceExplanations(): void {
    // F-163 SF-163-02b : pas de dossier en standalone.
    if (this.standaloneMode) return;
    if (!this.caseFileId || !this.sourceExplanationService) return;
    this.sourceExplanationService.getForCaseFile(this.caseFileId).subscribe({
      next: (map) => this.sourceExplanations.set(map),
      error: () => { /* fail-open */ },
    });
  }

  explanationFor(field: RequalificationInterimAlertField): SourceExplanation[] {
    const key = field === 'SALAIRE_MENSUEL' ? 'salaire_brut_mensuel' : '';
    return this.sourceExplanations().get(key) ?? [];
  }

  alertTooltip(alert: RequalificationInterimCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: RequalificationInterimCoherenceAlert): string {
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
   * SF-DT-23-02 : alerte salaire mensuel — écart relatif > 10 % IA vs avocat.
   * Multi-sources : IA + F96 (`DT23_SALAIRE`) + QUESTION_IA + PIECE_MANQUANTE.
   */
  private buildSalaireAlert(): RequalificationInterimCoherenceAlert | null {
    const aiSalaire = this.aiDataSignal()?.salaireBrutMensuel;
    const userSalaire = this.salaireMensuelBrutEur();
    if (typeof aiSalaire !== 'number' || aiSalaire <= 0) return null;
    if (typeof userSalaire !== 'number' || userSalaire <= 0) return null;
    const ratio = Math.abs(userSalaire - aiSalaire) / aiSalaire;
    if (ratio <= SALAIRE_DIVERGENCE_RATIO) return null;

    const display = `${aiSalaire.toLocaleString('fr-FR')} €`;
    const builder = CoherenceAlertBuilder.forField<RequalificationInterimAlertField>('SALAIRE_MENSUEL')
      .addSource('IA', {
        expectedDisplay: display,
        reason: `Analyse du dossier : salaire brut mensuel ~${display}`,
      });

    // F-96 checklist procédurale (critère salaire).
    for (const chk of this.procedureChecksSignal()) {
      const code = chk.critereCode?.toUpperCase();
      if (code !== 'DT23_SALAIRE' && code !== 'SALAIRE_BRUT_MENSUEL') continue;
      if (!chk.expectedValue) continue;
      builder.addSource('F96', {
        expectedDisplay: display,
        reason: `Checklist procédurale : salaire attendu ${chk.expectedValue}${chk.raison ? ' (' + chk.raison + ')' : ''}`,
      });
      break;
    }

    // QUESTION_IA — réponse "oui" sur le critère.
    for (const q of this.aiQuestionsSignal()) {
      const code = q.critereCode?.toUpperCase();
      if (code !== 'DT23_SALAIRE' && code !== 'SALAIRE_BRUT_MENSUEL') continue;
      const answer = q.answerText?.trim().toLowerCase();
      if (!answer) continue;
      const isOui = answer === 'oui' || answer.startsWith('oui ')
        || answer.startsWith('oui,') || answer.startsWith('oui.');
      if (!isOui) continue;
      if (!q.expectedValue) continue;
      builder.addSource('QUESTION_IA', {
        expectedDisplay: display,
        reason: `Question complémentaire : "${q.questionText}" → "${q.answerText}"`,
      });
      break;
    }

    // PIECE_MANQUANTE (bulletins de salaire).
    const piece = this.findPieceManquante([
      'DT23_SALAIRE',
      'SALAIRE_BRUT_MENSUEL',
      'BULLETINS_SALAIRE',
    ]);
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
  // Form handlers
  // ---------------------------------------------------------------------------

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  formValid(): boolean {
    if (this.motifInterimInvoque() === null) return false;
    if (this.motifInterdit() && this.motifInterditType() === null) return false;
    const duree = this.dureeMissionsTotaleMois();
    if (duree === null || duree <= 0) return false;
    const salaire = this.salaireMensuelBrutEur();
    if (salaire === null || salaire <= 0) return false;
    const dateFin = this.dateFinDerniereMission();
    if (!dateFin || dateFin.length === 0) return false;
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  onMotifInterimInvoqueChange(value: MotifInterimInvoque | null): void {
    this.motifInterimInvoque.set(value);
  }

  onMotifInterditChange(value: boolean): void {
    this.motifInterdit.set(!!value);
    if (!value) this.motifInterditType.set(null);
  }

  onMotifInterditTypeChange(value: MotifInterditTypeInterim | null): void {
    this.motifInterditType.set(value);
  }

  onDelaiCarenceRespecteChange(value: boolean): void {
    this.delaiCarenceRespecte.set(!!value);
  }

  onDureeMissionsTotaleMoisChange(value: number | null): void {
    this.dureeMissionsTotaleMois.set(value);
  }

  onSalaireChange(value: number | null): void {
    this.salaireMensuelBrutEur.set(value);
    this.provenanceSalaire.set(null);
  }

  onDateFinChange(value: string | null): void {
    this.dateFinDerniereMission.set(value && value.length > 0 ? value : null);
  }

  onMemeEntrepriseUtilisatriceChange(value: boolean): void {
    this.memeEntrepriseUtilisatrice.set(!!value);
  }

  onNewMissionDateDebutChange(value: string | null): void {
    this.newMissionDateDebut.set(value && value.length > 0 ? value : null);
  }

  onNewMissionDateFinChange(value: string | null): void {
    this.newMissionDateFin.set(value && value.length > 0 ? value : null);
  }

  onNewMissionMotifChange(value: string): void {
    this.newMissionMotif.set(value ?? '');
  }

  onNewMissionEntrepriseUtilisatriceChange(value: string): void {
    this.newMissionEntrepriseUtilisatrice.set(value ?? '');
  }

  /**
   * Ajoute la mission courante à la liste `successionMissions`. Les 4 champs
   * doivent être renseignés. Bouton désactivé sinon.
   */
  addMissionInterim(): void {
    const debut = this.newMissionDateDebut();
    const fin = this.newMissionDateFin();
    const motif = this.newMissionMotif().trim();
    const eu = this.newMissionEntrepriseUtilisatrice().trim();
    if (!debut || !fin || motif.length === 0 || eu.length === 0) return;
    const entry: MissionInterim = {
      dateDebut: debut,
      dateFin: fin,
      motif,
      entrepriseUtilisatrice: eu,
    };
    this.successionMissions.update((list) => [...list, entry]);
    // Reset des champs courants pour saisie suivante.
    this.newMissionDateDebut.set(null);
    this.newMissionDateFin.set(null);
    this.newMissionMotif.set('');
    this.newMissionEntrepriseUtilisatrice.set('');
  }

  removeMissionInterim(index: number): void {
    this.successionMissions.update((list) => list.filter((_, i) => i !== index));
  }

  newMissionValid(): boolean {
    return !!this.newMissionDateDebut()
      && !!this.newMissionDateFin()
      && this.newMissionMotif().trim().length > 0
      && this.newMissionEntrepriseUtilisatrice().trim().length > 0;
  }

  // ---------------------------------------------------------------------------
  // Display helpers
  // ---------------------------------------------------------------------------

  motifInterimInvoqueLabel = motifInterimInvoqueLabel;
  motifInterditTypeInterimLabel = motifInterditTypeInterimLabel;
  verdictProbabiliteInterimLabel = verdictProbabiliteInterimLabel;

  /**
   * Palette navy/or/rouge alignée DESIGN_SYSTEM.md.
   * Probabilité ELEVEE = rouge (alerte critique : risque concret de
   * requalification, à objectiver vite — palette danger justifiée par
   * conséquence financière directe + risque prescription L.1471-1).
   */
  verdictBannerClass(verdict: VerdictProbabiliteRequalificationInterim): string {
    switch (verdict) {
      case 'ELEVEE': return 'rci-verdict-banner rci-verdict-banner--danger';
      case 'MOYENNE': return 'rci-verdict-banner rci-verdict-banner--warn';
      case 'FAIBLE': return 'rci-verdict-banner rci-verdict-banner--info';
    }
  }

  verdictIcon(verdict: VerdictProbabiliteRequalificationInterim): string {
    switch (verdict) {
      case 'ELEVEE': return 'gavel';
      case 'MOYENNE': return 'balance';
      case 'FAIBLE': return 'info_outline';
    }
  }

  // ---------------------------------------------------------------------------
  // HTTP
  // ---------------------------------------------------------------------------

  calculate(): void {
    if (!this.formValid()) return;
    const request: RequalificationInterimCdiRequest = {
      motifInterimInvoque: this.motifInterimInvoque()!,
      motifInterdit: this.motifInterdit(),
      motifInterditType: this.motifInterdit() ? this.motifInterditType() : null,
      successionMissions: this.successionMissions(),
      delaiCarenceRespecte: this.delaiCarenceRespecte(),
      dureeMissionsTotaleMois: this.dureeMissionsTotaleMois()!,
      salaireMensuelBrutEur: this.salaireMensuelBrutEur()!,
      dateFinDerniereMission: this.dateFinDerniereMission()!,
      memeEntrepriseUtilisatrice: this.memeEntrepriseUtilisatrice(),
    };
    this.calculating.set(true);
    (this.standaloneMode
      ? this.service.calculateStandalone(request)
      : this.service.calculate(this.caseFileId, request))
      .subscribe({
      next: (r) => {
        this.applyPersistedResult(r);
        this.calculating.set(false);
        this.snackBar.open('Analyse de requalification calculée', 'OK', { duration: 2500 });
        // F-163 SF-163-02b : pas de dashboard à rafraîchir en standalone.

        if (!this.standaloneMode) this.dashboardRefresh?.triggerRefresh();
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
    // F-163 SF-163-02b : en standalone, pas de dossier à interroger.

    if (this.standaloneMode) {

      this.loading.set(false);

      this.showForm.set(true);

      if (this.collapsed && typeof (this.collapsed as any).set === 'function') this.collapsed.set(false);

      return;

    }

    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.applyPersistedResult(r);
        this.loading.set(false);
      },
      error: () => {
        // 404 attendu si aucune analyse — mode formulaire + pré-fill IA.
        if (!this.standaloneMode) this.prefillFromAi();
        this.loading.set(false);
      },
    });
  }

  private applyPersistedResult(r: RequalificationInterimCdiResponse): void {
    this.result.set(r);
    this.motifInterimInvoque.set(r.motifInterimInvoque);
    this.motifInterdit.set(!!r.motifInterdit);
    this.motifInterditType.set(r.motifInterditType);
    this.successionMissions.set(r.successionMissions ?? []);
    this.delaiCarenceRespecte.set(!!r.delaiCarenceRespecte);
    this.dureeMissionsTotaleMois.set(r.dureeMissionsTotaleMois);
    this.salaireMensuelBrutEur.set(r.salaireMensuelBrutEur);
    this.dateFinDerniereMission.set(r.dateFinDerniereMission);
    this.memeEntrepriseUtilisatrice.set(!!r.memeEntrepriseUtilisatrice);
    // Valeurs persistées = saisie avocat — jamais de badge IA.
    this.provenanceSalaire.set(null);
    this.showForm.set(false);
  }
}
