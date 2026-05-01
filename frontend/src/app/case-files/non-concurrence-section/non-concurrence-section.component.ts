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
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NonConcurrenceService } from '../../core/services/non-concurrence.service';
import {
  NonConcurrenceRequest,
  NonConcurrenceResponse,
  SECTEUR_ACTIVITE_OPTIONS,
  SecteurActivite,
  VerdictValiditeNc,
  secteurActiviteLabel,
  verdictValiditeNcLabel,
} from '../../core/models/non-concurrence.model';
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
 * SF-DT-24-02 : champs d'alerte de cohérence F-IA-03 exposés par l'outil
 * F-DT-24 (clause de non-concurrence).
 */
export type NonConcurrenceAlertField = 'SALAIRE_MENSUEL';

export type NonConcurrenceAlertSource = CoherenceAlertSource;
export type NonConcurrenceCoherenceAlert = CoherenceAlert<NonConcurrenceAlertField>;

/**
 * Seuil d'écart relatif (10 %) au-delà duquel on déclenche une alerte sur
 * le salaire mensuel — aligné avec F-DT-17 / F-DT-21 / F-DT-22 / F-DT-23.
 */
const SALAIRE_DIVERGENCE_RATIO = 0.10;

/**
 * SF-DT-24-02 : outil décisionnel "Clause de non-concurrence" (F-DT-24).
 * France uniquement (Cass. soc. 10/07/2002 + art. L.1121-1 Code du travail).
 * Consomme l'API figée SF-DT-24-01.
 *
 * Affiché conditionnellement par le panel F-IA-04 (tool_id
 * `F-DT-24-non-concurrence`, règle ALWAYS_ON FR + travail).
 *
 * Pattern canonique : `harcelement-licenciement-nul-section` (cf.
 * `ai-skills/frontend-coherence-audit.md` §5). Pattern jumeau direct :
 * `requalification-interim-cdi-section` (F-DT-23-02, structure verdict +
 * scoring + 1 alerte salaire identique).
 *
 * Pré-fill IA : `salaireMensuelBrutEur` depuis `aiData.salaireBrutMensuel`.
 * Validation F-IA-03 : alerte sur SALAIRE_MENSUEL via `CoherenceAlertBuilder`
 * (multi-sources IA / F96 / QUESTION_IA / PIECE_MANQUANTE).
 */
@Component({
  selector: 'app-non-concurrence-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule, DecimalPipe,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatSelectModule, MatSlideToggleModule,
    MatProgressSpinnerModule,
    LegalCitationsPipe,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './non-concurrence-section.component.html',
  styleUrl: './non-concurrence-section.component.scss',
})
export class NonConcurrenceSectionComponent implements OnInit, OnChanges {
  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'CLAUSE DE NON-CONCURRENCE (FR) — CASS. SOC. 10/07/2002';
  static readonly TOOL_ICON = 'block';

  @Input() caseFileId!: string;
  // F-177 SF-177-03b : force l'expansion (mode modal F-177).
  @Input() forceExpanded = false;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';
  @Input() aiData?: TravailExtractedData | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;

  private aiDataSignal = signal<TravailExtractedData | null | undefined>(undefined);
  private procedureChecksSignal = signal<ProcedureCheck[]>([]);
  private aiQuestionsSignal = signal<AiQuestion[]>([]);
  private piecesManquantesSignal = signal<PieceManquanteEntry[]>([]);

  readonly secteurOptions = SECTEUR_ACTIVITE_OPTIONS;

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<NonConcurrenceResponse | null>(null);

  // Form state.
  clausePresenteContrat = signal<boolean>(true);
  limiteTerritoireDefini = signal<boolean>(false);
  territoireDescription = signal<string>('');
  limiteDureeDefinie = signal<boolean>(false);
  dureeMois = signal<number | null>(null);
  limiteObjetDefini = signal<boolean>(false);
  objetDescription = signal<string>('');
  contrepartieFinancierePresente = signal<boolean>(false);
  contrepartieMontantMensuelEur = signal<number | null>(null);
  salaireMensuelBrutEur = signal<number | null>(null);
  secteurActivite = signal<SecteurActivite | null>(null);
  datePriseEffet = signal<string | null>(null);

  provenanceSalaire = signal<'IA' | null>(null);

  sourceExplanations = signal<Map<string, SourceExplanation[]>>(new Map());

  // SF-DT-24-02 / SF-155 : alertes de cohérence F-IA-03 calculées dynamiquement.
  // Gate strict `showForm()` (anti-bug SF-IA-03-12 — pas d'alertes en mode lecture).
  coherenceAlerts = computed<Partial<Record<NonConcurrenceAlertField, NonConcurrenceCoherenceAlert>>>(() => {
    if (!this.showForm()) return {};
    const alerts: Partial<Record<NonConcurrenceAlertField, NonConcurrenceCoherenceAlert>> = {};
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
    private service: NonConcurrenceService,
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
      this.prefillFromAi();
    }
  }

  /**
   * SF-DT-24-02 : pré-remplit `salaireMensuelBrutEur` depuis l'analyse IA.
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

  explanationFor(field: NonConcurrenceAlertField): SourceExplanation[] {
    const key = field === 'SALAIRE_MENSUEL' ? 'salaire_brut_mensuel' : '';
    return this.sourceExplanations().get(key) ?? [];
  }

  alertTooltip(alert: NonConcurrenceCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: NonConcurrenceCoherenceAlert): string {
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
   * SF-DT-24-02 : alerte salaire mensuel — écart relatif > 10 % IA vs avocat.
   * Multi-sources : IA + F96 (`DT24_SALAIRE` ou `SALAIRE_BRUT_MENSUEL`) +
   * QUESTION_IA + PIECE_MANQUANTE.
   */
  private buildSalaireAlert(): NonConcurrenceCoherenceAlert | null {
    const aiSalaire = this.aiDataSignal()?.salaireBrutMensuel;
    const userSalaire = this.salaireMensuelBrutEur();
    if (typeof aiSalaire !== 'number' || aiSalaire <= 0) return null;
    if (typeof userSalaire !== 'number' || userSalaire <= 0) return null;
    const ratio = Math.abs(userSalaire - aiSalaire) / aiSalaire;
    if (ratio <= SALAIRE_DIVERGENCE_RATIO) return null;

    const display = `${aiSalaire.toLocaleString('fr-FR')} €`;
    const builder = CoherenceAlertBuilder.forField<NonConcurrenceAlertField>('SALAIRE_MENSUEL')
      .addSource('IA', {
        expectedDisplay: display,
        reason: `Analyse du dossier : salaire brut mensuel ~${display}`,
      });

    // F-96 checklist procédurale (critère salaire).
    for (const chk of this.procedureChecksSignal()) {
      const code = chk.critereCode?.toUpperCase();
      if (code !== 'DT24_SALAIRE' && code !== 'SALAIRE_BRUT_MENSUEL') continue;
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
      if (code !== 'DT24_SALAIRE' && code !== 'SALAIRE_BRUT_MENSUEL') continue;
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
      'DT24_SALAIRE',
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
    const salaire = this.salaireMensuelBrutEur();
    if (salaire === null || salaire <= 0) return false;
    if (this.secteurActivite() === null) return false;
    const date = this.datePriseEffet();
    if (!date || date.length === 0) return false;

    if (this.limiteTerritoireDefini() && this.territoireDescription().trim().length === 0) {
      return false;
    }
    if (this.limiteDureeDefinie()) {
      const d = this.dureeMois();
      if (d === null || d <= 0) return false;
    }
    if (this.limiteObjetDefini() && this.objetDescription().trim().length === 0) {
      return false;
    }
    if (this.contrepartieFinancierePresente()) {
      const m = this.contrepartieMontantMensuelEur();
      if (m === null || m <= 0) return false;
    }
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  onClausePresenteContratChange(value: boolean): void {
    this.clausePresenteContrat.set(!!value);
  }

  onLimiteTerritoireDefiniChange(value: boolean): void {
    this.limiteTerritoireDefini.set(!!value);
    if (!value) this.territoireDescription.set('');
  }

  onTerritoireDescriptionChange(value: string): void {
    this.territoireDescription.set(value ?? '');
  }

  onLimiteDureeDefinieChange(value: boolean): void {
    this.limiteDureeDefinie.set(!!value);
    if (!value) this.dureeMois.set(null);
  }

  onDureeMoisChange(value: number | null): void {
    this.dureeMois.set(value);
  }

  onLimiteObjetDefiniChange(value: boolean): void {
    this.limiteObjetDefini.set(!!value);
    if (!value) this.objetDescription.set('');
  }

  onObjetDescriptionChange(value: string): void {
    this.objetDescription.set(value ?? '');
  }

  onContrepartieFinancierePresenteChange(value: boolean): void {
    this.contrepartieFinancierePresente.set(!!value);
    if (!value) this.contrepartieMontantMensuelEur.set(null);
  }

  onContrepartieMontantChange(value: number | null): void {
    this.contrepartieMontantMensuelEur.set(value);
  }

  onSalaireChange(value: number | null): void {
    this.salaireMensuelBrutEur.set(value);
    this.provenanceSalaire.set(null);
  }

  onSecteurActiviteChange(value: SecteurActivite | null): void {
    this.secteurActivite.set(value);
  }

  onDatePriseEffetChange(value: string | null): void {
    this.datePriseEffet.set(value && value.length > 0 ? value : null);
  }

  // ---------------------------------------------------------------------------
  // Display helpers
  // ---------------------------------------------------------------------------

  secteurActiviteLabel = secteurActiviteLabel;
  verdictValiditeNcLabel = verdictValiditeNcLabel;

  /**
   * Palette navy/or/rouge alignée DESIGN_SYSTEM.md.
   * `NULLE` = rouge alerte critique (clause inopposable, indemnité due
   * potentielle pour rupture abusive — palette danger justifiée par
   * conséquence financière directe + nullité radicale).
   * `RISQUE_NULLITE_PARTIELLE` = or (risque modulable selon office du juge).
   * `VALIDE` = navy (situation maitrisée).
   */
  verdictBannerClass(verdict: VerdictValiditeNc): string {
    switch (verdict) {
      case 'NULLE': return 'nc-verdict-banner nc-verdict-banner--danger';
      case 'RISQUE_NULLITE_PARTIELLE': return 'nc-verdict-banner nc-verdict-banner--warn';
      case 'VALIDE': return 'nc-verdict-banner nc-verdict-banner--info';
    }
  }

  verdictIcon(verdict: VerdictValiditeNc): string {
    switch (verdict) {
      case 'NULLE': return 'gavel';
      case 'RISQUE_NULLITE_PARTIELLE': return 'balance';
      case 'VALIDE': return 'verified';
    }
  }

  // ---------------------------------------------------------------------------
  // HTTP
  // ---------------------------------------------------------------------------

  calculate(): void {
    if (!this.formValid()) return;
    const request: NonConcurrenceRequest = {
      clausePresenteContrat: this.clausePresenteContrat(),
      limiteTerritoireDefini: this.limiteTerritoireDefini(),
      territoireDescription: this.territoireDescription(),
      limiteDureeDefinie: this.limiteDureeDefinie(),
      dureeMois: this.dureeMois() ?? 0,
      limiteObjetDefini: this.limiteObjetDefini(),
      objetDescription: this.objetDescription(),
      contrepartieFinancierePresente: this.contrepartieFinancierePresente(),
      contrepartieMontantMensuelEur: this.contrepartieMontantMensuelEur() ?? 0,
      salaireMensuelBrutEur: this.salaireMensuelBrutEur()!,
      secteurActivite: this.secteurActivite()!,
      datePriseEffet: this.datePriseEffet()!,
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.applyPersistedResult(r);
        this.calculating.set(false);
        this.snackBar.open('Analyse de validité de la clause calculée', 'OK', { duration: 2500 });
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

  private applyPersistedResult(r: NonConcurrenceResponse): void {
    this.result.set(r);
    this.clausePresenteContrat.set(!!r.clausePresenteContrat);
    this.limiteTerritoireDefini.set(!!r.limiteTerritoireDefini);
    this.territoireDescription.set(r.territoireDescription ?? '');
    this.limiteDureeDefinie.set(!!r.limiteDureeDefinie);
    this.dureeMois.set(r.dureeMois);
    this.limiteObjetDefini.set(!!r.limiteObjetDefini);
    this.objetDescription.set(r.objetDescription ?? '');
    this.contrepartieFinancierePresente.set(!!r.contrepartieFinancierePresente);
    this.contrepartieMontantMensuelEur.set(r.contrepartieMontantMensuelEur);
    this.salaireMensuelBrutEur.set(r.salaireMensuelBrutEur);
    this.secteurActivite.set(r.secteurActivite);
    this.datePriseEffet.set(r.datePriseEffet);
    // Valeurs persistées = saisie avocat — jamais de badge IA.
    this.provenanceSalaire.set(null);
    this.showForm.set(false);
  }
}
