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
import { RequalificationCddCdiService } from '../../core/services/requalification-cdd-cdi.service';
import {
  CddSuccession,
  MOTIF_CDD_INVOQUE_OPTIONS,
  MOTIF_INTERDIT_TYPE_OPTIONS,
  MotifCddInvoque,
  MotifInterditType,
  RequalificationCddCdiRequest,
  RequalificationCddCdiResponse,
  VerdictProbabiliteRequalification,
  motifCddInvoqueLabel,
  motifInterditTypeLabel,
  verdictProbabiliteLabel,
} from '../../core/models/requalification-cdd-cdi.model';
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

/**
 * SF-DT-22-02 : champs d'alerte de cohérence F-IA-03 exposés par l'outil
 * F-DT-22 (requalification CDD → CDI).
 */
export type RequalificationCddAlertField = 'SALAIRE_MENSUEL';

export type RequalificationCddAlertSource = CoherenceAlertSource;
export type RequalificationCddCoherenceAlert = CoherenceAlert<RequalificationCddAlertField>;

/**
 * Seuil d'écart relatif (10 %) au-delà duquel on déclenche une alerte sur
 * le salaire mensuel — aligné avec F-DT-17 / F-DT-21 / template canonique.
 */
const SALAIRE_DIVERGENCE_RATIO = 0.10;

/**
 * SF-DT-22-02 : outil décisionnel "Requalification CDD → CDI" (F-DT-22).
 * France uniquement. Consomme l'API figée SF-DT-22-01.
 *
 * Affiché conditionnellement par le panel F-IA-04 (tool_id
 * `F-DT-22-requalification-cdd-cdi`, règle ALWAYS_ON FR + travail).
 *
 * Pattern canonique : `harcelement-licenciement-nul-section` (cf.
 * `ai-skills/frontend-coherence-audit.md` §5). Pattern jumeau structurel :
 * `indemnite-precarite-cdd-section` (F-DT-17-02), pattern jumeau verdict :
 * `divorce-faute-section` (F-FA-09-02).
 *
 * Pré-fill IA : `salaireMensuelBrutEur` depuis `aiData.salaireBrutMensuel`.
 * Validation F-IA-03 : alerte sur SALAIRE_MENSUEL via `CoherenceAlertBuilder`
 * (multi-sources IA / F96 / QUESTION_IA / PIECE_MANQUANTE).
 */
@Component({
  selector: 'app-requalification-cdd-cdi-section',
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
  templateUrl: './requalification-cdd-cdi-section.component.html',
  styleUrl: './requalification-cdd-cdi-section.component.scss',
})
export class RequalificationCddCdiSectionComponent implements OnInit, OnChanges {
  @Input() caseFileId!: string;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';
  @Input() aiData?: TravailExtractedData | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;

  private aiDataSignal = signal<TravailExtractedData | null | undefined>(undefined);
  private procedureChecksSignal = signal<ProcedureCheck[]>([]);
  private aiQuestionsSignal = signal<AiQuestion[]>([]);
  private piecesManquantesSignal = signal<PieceManquanteEntry[]>([]);

  readonly motifCddOptions = MOTIF_CDD_INVOQUE_OPTIONS;
  readonly motifInterditOptions = MOTIF_INTERDIT_TYPE_OPTIONS;

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<RequalificationCddCdiResponse | null>(null);

  // Form state.
  motifCddInvoque = signal<MotifCddInvoque | null>(null);
  motifInterdit = signal<boolean>(false);
  motifInterditType = signal<MotifInterditType | null>(null);
  successionCdd = signal<CddSuccession[]>([]);
  delaiCarenceRespecte = signal<boolean>(true);
  dureeContratMois = signal<number | null>(null);
  salaireMensuelBrutEur = signal<number | null>(null);
  dateFinDernierContrat = signal<string | null>(null);

  // Édition succession CDD : champs courants.
  newCddDateDebut = signal<string | null>(null);
  newCddDateFin = signal<string | null>(null);
  newCddMotif = signal<string>('');

  provenanceSalaire = signal<'IA' | null>(null);

  sourceExplanations = signal<Map<string, SourceExplanation[]>>(new Map());

  // SF-DT-22-02 / SF-155 : alertes de cohérence F-IA-03 calculées dynamiquement.
  // Gate strict `showForm()` (anti-bug SF-IA-03-12 — pas d'alertes en mode lecture).
  coherenceAlerts = computed<Partial<Record<RequalificationCddAlertField, RequalificationCddCoherenceAlert>>>(() => {
    if (!this.showForm()) return {};
    const alerts: Partial<Record<RequalificationCddAlertField, RequalificationCddCoherenceAlert>> = {};
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
    private service: RequalificationCddCdiService,
    private snackBar: MatSnackBar,
    @Optional() private dashboardRefresh: CaseDashboardRefreshService | null,
    @Optional() private sourceExplanationService: SourceExplanationService | null,
  ) {}

  ngOnInit(): void {
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
    if (changes['aiData']) this.aiDataSignal.set(this.aiData);
    if (changes['procedureChecks']) this.procedureChecksSignal.set(this.procedureChecks ?? []);
    if (changes['aiQuestions']) this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    if (changes['piecesManquantes']) this.piecesManquantesSignal.set(this.piecesManquantes ?? []);

    if (changes['aiData'] && !changes['aiData'].firstChange
        && this.showForm() && !this.result()) {
      this.prefillFromAi();
    }
  }

  /**
   * SF-DT-22-02 : pré-remplit `salaireMensuelBrutEur` depuis l'analyse IA.
   * N'écrase jamais une saisie manuelle (provenance null sur un champ rempli).
   */
  private prefillFromAi(): void {
    const ai = this.aiDataSignal();
    if (!ai) return;
    if (!this.isFrance()) return;

    if (typeof ai.salaireBrutMensuel === 'number' && ai.salaireBrutMensuel > 0) {
      if (this.salaireMensuelBrutEur() === null || this.provenanceSalaire() === 'IA') {
        this.salaireMensuelBrutEur.set(ai.salaireBrutMensuel);
        this.provenanceSalaire.set('IA');
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

  explanationFor(field: RequalificationCddAlertField): SourceExplanation[] {
    const key = field === 'SALAIRE_MENSUEL' ? 'salaire_brut_mensuel' : '';
    return this.sourceExplanations().get(key) ?? [];
  }

  alertTooltip(alert: RequalificationCddCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: RequalificationCddCoherenceAlert): string {
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
   * SF-DT-22-02 : alerte salaire mensuel — écart relatif > 10 % IA vs avocat.
   * Multi-sources : IA + F96 (`DT22_SALAIRE`) + QUESTION_IA + PIECE_MANQUANTE.
   */
  private buildSalaireAlert(): RequalificationCddCoherenceAlert | null {
    const aiSalaire = this.aiDataSignal()?.salaireBrutMensuel;
    const userSalaire = this.salaireMensuelBrutEur();
    if (typeof aiSalaire !== 'number' || aiSalaire <= 0) return null;
    if (typeof userSalaire !== 'number' || userSalaire <= 0) return null;
    const ratio = Math.abs(userSalaire - aiSalaire) / aiSalaire;
    if (ratio <= SALAIRE_DIVERGENCE_RATIO) return null;

    const display = `${aiSalaire.toLocaleString('fr-FR')} €`;
    const builder = CoherenceAlertBuilder.forField<RequalificationCddAlertField>('SALAIRE_MENSUEL')
      .addSource('IA', {
        expectedDisplay: display,
        reason: `Analyse du dossier : salaire brut mensuel ~${display}`,
      });

    // F-96 checklist procédurale (critère salaire).
    for (const chk of this.procedureChecksSignal()) {
      const code = chk.critereCode?.toUpperCase();
      if (code !== 'DT22_SALAIRE' && code !== 'SALAIRE_BRUT_MENSUEL') continue;
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
      if (code !== 'DT22_SALAIRE' && code !== 'SALAIRE_BRUT_MENSUEL') continue;
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
      'DT22_SALAIRE',
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
    if (this.motifCddInvoque() === null) return false;
    if (this.motifInterdit() && this.motifInterditType() === null) return false;
    const duree = this.dureeContratMois();
    if (duree === null || duree <= 0) return false;
    const salaire = this.salaireMensuelBrutEur();
    if (salaire === null || salaire <= 0) return false;
    const dateFin = this.dateFinDernierContrat();
    if (!dateFin || dateFin.length === 0) return false;
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  onMotifCddInvoqueChange(value: MotifCddInvoque | null): void {
    this.motifCddInvoque.set(value);
  }

  onMotifInterditChange(value: boolean): void {
    this.motifInterdit.set(!!value);
    if (!value) this.motifInterditType.set(null);
  }

  onMotifInterditTypeChange(value: MotifInterditType | null): void {
    this.motifInterditType.set(value);
  }

  onDelaiCarenceRespecteChange(value: boolean): void {
    this.delaiCarenceRespecte.set(!!value);
  }

  onDureeContratMoisChange(value: number | null): void {
    this.dureeContratMois.set(value);
  }

  onSalaireChange(value: number | null): void {
    this.salaireMensuelBrutEur.set(value);
    this.provenanceSalaire.set(null);
  }

  onDateFinChange(value: string | null): void {
    this.dateFinDernierContrat.set(value && value.length > 0 ? value : null);
  }

  onNewCddDateDebutChange(value: string | null): void {
    this.newCddDateDebut.set(value && value.length > 0 ? value : null);
  }

  onNewCddDateFinChange(value: string | null): void {
    this.newCddDateFin.set(value && value.length > 0 ? value : null);
  }

  onNewCddMotifChange(value: string): void {
    this.newCddMotif.set(value ?? '');
  }

  /**
   * Ajoute le CDD courant à la liste `successionCdd`. Les 3 champs doivent
   * être renseignés (dateDebut, dateFin, motif). Bouton désactivé sinon.
   */
  addCddSuccession(): void {
    const debut = this.newCddDateDebut();
    const fin = this.newCddDateFin();
    const motif = this.newCddMotif().trim();
    if (!debut || !fin || motif.length === 0) return;
    const entry: CddSuccession = { dateDebut: debut, dateFin: fin, motif };
    this.successionCdd.update((list) => [...list, entry]);
    // Reset des champs courants pour saisie suivante.
    this.newCddDateDebut.set(null);
    this.newCddDateFin.set(null);
    this.newCddMotif.set('');
  }

  removeCddSuccession(index: number): void {
    this.successionCdd.update((list) => list.filter((_, i) => i !== index));
  }

  newCddValid(): boolean {
    return !!this.newCddDateDebut()
      && !!this.newCddDateFin()
      && this.newCddMotif().trim().length > 0;
  }

  // ---------------------------------------------------------------------------
  // Display helpers
  // ---------------------------------------------------------------------------

  motifCddInvoqueLabel = motifCddInvoqueLabel;
  motifInterditTypeLabel = motifInterditTypeLabel;
  verdictProbabiliteLabel = verdictProbabiliteLabel;

  /**
   * Palette navy/or/rouge alignée DESIGN_SYSTEM.md.
   * Probabilité ELEVEE = rouge (alerte critique : risque concret de
   * requalification, à objectiver vite — palette danger justifiée par
   * conséquence financière directe + risque prescription L.1471-1).
   */
  verdictBannerClass(verdict: VerdictProbabiliteRequalification): string {
    switch (verdict) {
      case 'ELEVEE': return 'rcc-verdict-banner rcc-verdict-banner--danger';
      case 'MOYENNE': return 'rcc-verdict-banner rcc-verdict-banner--warn';
      case 'FAIBLE': return 'rcc-verdict-banner rcc-verdict-banner--info';
    }
  }

  verdictIcon(verdict: VerdictProbabiliteRequalification): string {
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
    const request: RequalificationCddCdiRequest = {
      motifCddInvoque: this.motifCddInvoque()!,
      motifInterdit: this.motifInterdit(),
      motifInterditType: this.motifInterdit() ? this.motifInterditType() : null,
      successionCdd: this.successionCdd(),
      delaiCarenceRespecte: this.delaiCarenceRespecte(),
      dureeContratMois: this.dureeContratMois()!,
      salaireMensuelBrutEur: this.salaireMensuelBrutEur()!,
      dateFinDernierContrat: this.dateFinDernierContrat()!,
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.applyPersistedResult(r);
        this.calculating.set(false);
        this.snackBar.open('Analyse de requalification calculée', 'OK', { duration: 2500 });
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
        this.applyPersistedResult(r);
        this.loading.set(false);
      },
      error: () => {
        // 404 attendu si aucune analyse — mode formulaire + pré-fill IA.
        this.prefillFromAi();
        this.loading.set(false);
      },
    });
  }

  private applyPersistedResult(r: RequalificationCddCdiResponse): void {
    this.result.set(r);
    this.motifCddInvoque.set(r.motifCddInvoque);
    this.motifInterdit.set(!!r.motifInterdit);
    this.motifInterditType.set(r.motifInterditType);
    this.successionCdd.set(r.successionCdd ?? []);
    this.delaiCarenceRespecte.set(!!r.delaiCarenceRespecte);
    this.dureeContratMois.set(r.dureeContratMois);
    this.salaireMensuelBrutEur.set(r.salaireMensuelBrutEur);
    this.dateFinDernierContrat.set(r.dateFinDernierContrat);
    // Valeurs persistées = saisie avocat — jamais de badge IA.
    this.provenanceSalaire.set(null);
    this.showForm.set(false);
  }
}
