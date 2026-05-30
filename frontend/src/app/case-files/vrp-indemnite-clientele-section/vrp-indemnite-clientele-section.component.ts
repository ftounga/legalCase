import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
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
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { VrpIndemniteClienteleService } from '../../core/services/vrp-indemnite-clientele.service';
import {
  VrpCauseRupture,
  VrpEligibiliteClientele,
  VrpIndemniteClienteleRequest,
  VrpIndemniteClienteleResponse,
  VrpOptionRecommandee,
  VrpType,
} from '../../core/models/vrp-indemnite-clientele.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { AiQuestion } from '../../core/models/ai-question.model';
import { CoherencePopoverTriggerDirective } from '../../shared/coherence-popover/coherence-popover-trigger.directive';
import { CoherenceAlert } from '../../shared/coherence-popover/coherence-alert.model';
import { CoherenceAlertBuilder } from '../../shared/coherence-popover/coherence-alert-builder';
import { VrpIndemniteClienteleSectionPrefillRules } from './vrp-indemnite-clientele-section-prefill-rules';

/**
 * SF-218-12 : champs F-IA-03 audités par l'outil F-DT-104. Deux dates
 * croisables avec la checklist procédurale F-96 / les questions IA.
 */
export type VrpAlertField = 'DATE_ENTREE' | 'DATE_RUPTURE';

export type VrpCoherenceAlert = CoherenceAlert<VrpAlertField>;

/** Codes `critereCode` (F-96 / questions IA) reconnus par field. */
const F96_CODE_DATE_ENTREE = 'DT104_DATE_ENTREE';
const F96_CODE_DATE_RUPTURE = 'DT104_DATE_RUPTURE';

/**
 * SF-218-12 : composant Angular standalone pour l'outil décisionnel
 * "VRP : statut, préavis et indemnité de clientèle" (F-DT-104). FRANCE
 * uniquement (statut VRP statutaire franco-français — art. L.7311-1 et s. CT).
 *
 * Pattern de référence : `abandon-poste-presomption-demission-section`
 * (F-DT-42) — formulaire de saisie, POST de calcul, verdict + badge,
 * pré-fill IA, F-IA-03 (CoherenceAlertBuilder + popover), OnPush + markForCheck
 * dans les subscribe.
 *
 * Éligibilité indemnité de clientèle : DUE (navy/vert) / NON_DUE (rouge —
 * signal d'alerte pour l'avocat du salarié). Rouge réservé à NON_DUE.
 */
@Component({
  selector: 'app-vrp-indemnite-clientele-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatSelectModule, MatSlideToggleModule,
    MatProgressSpinnerModule,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './vrp-indemnite-clientele-section.component.html',
  styleUrl: './vrp-indemnite-clientele-section.component.scss',
})
export class VrpIndemniteClienteleSectionComponent implements OnInit, OnChanges {
  // Metadata statique consommée par le panel pour la card.
  static readonly TOOL_LABEL = 'VRP : PRÉAVIS ET INDEMNITÉ DE CLIENTÈLE';
  static readonly TOOL_ICON = 'badge';

  /**
   * Délégué au helper partagé (parité stricte avec `prefillFromAi()`).
   * Retourne le nombre exact de champs pré-remplissables FR pour le badge tab.
   */
  static getPrefillCount(input: {
    aiData?: TravailExtractedData | null;
    procedureChecks?: unknown[];
    aiQuestions?: unknown[];
    piecesManquantes?: unknown[];
    triggerEvents?: unknown[];
    workspaceCountry?: string;
  }): number {
    return VrpIndemniteClienteleSectionPrefillRules.computePrefillCount({
      aiData: input.aiData,
      workspaceCountry: input.workspaceCountry,
    });
  }

  @Input() caseFileId!: string;
  @Input() forceExpanded = false;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';
  @Input() aiData?: TravailExtractedData | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  /** Mode simulateur autonome (hors dossier) — coupe F-IA-03 + refresh. */
  @Input() standaloneMode = false;

  // Snapshots signal des inputs IA pour réactivité des `computed`.
  private procedureChecksSignal = signal<ProcedureCheck[]>([]);
  private aiQuestionsSignal = signal<AiQuestion[]>([]);

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<VrpIndemniteClienteleResponse | null>(null);

  // --- Form fields ---
  dateEntree = signal<string | null>(null);
  dateRupture = signal<string | null>(null);
  causeRupture = signal<VrpCauseRupture>('LICENCIEMENT_CAUSE_REELLE');
  typeVrp = signal<VrpType>('EXCLUSIF');
  commissionsAnnuellesMoyennes = signal<number>(0);
  salaireMensuelMoyen = signal<number>(0);
  clienteleDeveloppee = signal<boolean>(true);

  /**
   * Provenance IA par champ pré-rempli. `'IA'` tant que la valeur provient de
   * l'analyse ; remis à `null` dès qu'un handler `onXxxChange()` détecte une
   * modification manuelle (le badge `auto_awesome` disparaît).
   */
  provenanceDateEntree = signal<'IA' | null>(null);
  provenanceDateRupture = signal<'IA' | null>(null);
  provenanceCommissions = signal<'IA' | null>(null);

  /**
   * Coherence alerts F-IA-03 — gate `showForm()` strict (alertes masquées
   * après calcul rendu). Aucune source IA en standalone.
   */
  coherenceAlerts = computed<Partial<Record<VrpAlertField, VrpCoherenceAlert>>>(() => {
    if (this.standaloneMode) return {};
    if (!this.showForm()) return {};
    const alerts: Partial<Record<VrpAlertField, VrpCoherenceAlert>> = {};
    const entreeAlert = this.buildDateAlert('DATE_ENTREE', F96_CODE_DATE_ENTREE, this.dateEntree());
    if (entreeAlert) alerts.DATE_ENTREE = entreeAlert;
    const ruptureAlert = this.buildDateAlert('DATE_RUPTURE', F96_CODE_DATE_RUPTURE, this.dateRupture());
    if (ruptureAlert) alerts.DATE_RUPTURE = ruptureAlert;
    return alerts;
  });

  alertsSummary = computed(() => ({ total: Object.values(this.coherenceAlerts()).length }));

  /** Options du <mat-select> cause de rupture. */
  readonly causeRuptureOptions: { value: VrpCauseRupture; label: string }[] = [
    { value: 'LICENCIEMENT_CAUSE_REELLE', label: 'Licenciement pour cause réelle et sérieuse' },
    { value: 'FAUTE_GRAVE', label: 'Licenciement pour faute grave' },
    { value: 'FAUTE_LOURDE', label: 'Licenciement pour faute lourde' },
    { value: 'DEMISSION', label: 'Démission' },
    { value: 'DEPART_RETRAITE', label: 'Départ en retraite' },
    { value: 'RUPTURE_CONVENTIONNELLE', label: 'Rupture conventionnelle' },
  ];

  /** Options du <mat-select> type de VRP. */
  readonly typeVrpOptions: { value: VrpType; label: string }[] = [
    { value: 'EXCLUSIF', label: 'VRP exclusif (un seul employeur)' },
    { value: 'MULTICARTES', label: 'VRP multicartes (plusieurs employeurs)' },
  ];

  constructor(
    private service: VrpIndemniteClienteleService,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef,
    @Optional() private dashboardRefresh: CaseDashboardRefreshService | null,
  ) {}

  ngOnInit(): void {
    if (this.forceExpanded) this.collapsed.set(false);
    this.procedureChecksSignal.set(this.procedureChecks ?? []);
    this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    this.prefillFromAi();
    if (this.workspaceCountry === 'FRANCE') {
      this.load();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['forceExpanded'] && this.forceExpanded) this.collapsed.set(false);
    if (changes['procedureChecks']) this.procedureChecksSignal.set(this.procedureChecks ?? []);
    if (changes['aiQuestions']) this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    if (changes['aiData'] && !changes['aiData'].firstChange
        && this.showForm() && !this.result()) {
      this.prefillFromAi();
    }
  }

  /**
   * Pré-fill IA (SF-218-12) — renseigne `dateEntree`, `dateRupture`,
   * `commissionsAnnuellesMoyennes` depuis `TravailExtractedData`.
   *
   * Parité stricte avec `getPrefillCount()` : mêmes mappings, même gate
   * `workspaceCountry === 'FRANCE'`. Délègue au helper partagé. No-op gracieux
   * si `aiData` absent ou dossier BE. N'écrase pas une saisie avocat.
   */
  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    const ai = this.aiData;
    if (!ai) return;

    const rules = VrpIndemniteClienteleSectionPrefillRules;
    const ruleInput = { aiData: ai, workspaceCountry: this.workspaceCountry };

    const dateEntree = rules.computeDateEntree(ruleInput);
    if (dateEntree
        && (this.dateEntree() === null || this.provenanceDateEntree() === 'IA')) {
      this.dateEntree.set(dateEntree);
      this.provenanceDateEntree.set('IA');
    }

    const dateRupture = rules.computeDateRupture(ruleInput);
    if (dateRupture
        && (this.dateRupture() === null || this.provenanceDateRupture() === 'IA')) {
      this.dateRupture.set(dateRupture);
      this.provenanceDateRupture.set('IA');
    }

    const commissions = rules.computeCommissionsAnnuelles(ruleInput);
    if (commissions !== null
        && (this.commissionsAnnuellesMoyennes() === 0 || this.provenanceCommissions() === 'IA')) {
      this.commissionsAnnuellesMoyennes.set(commissions);
      this.provenanceCommissions.set('IA');
    }
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  editMode(): void {
    this.showForm.set(true);
  }

  /** Form valide : FRANCE + dates présentes + dateRupture ≥ dateEntree + montants ≥ 0. */
  formValid(): boolean {
    if (this.workspaceCountry !== 'FRANCE') return false;
    const entree = this.dateEntree();
    const rupture = this.dateRupture();
    if (!entree || !rupture) return false;
    if (rupture < entree) return false;
    const com = this.commissionsAnnuellesMoyennes();
    const sal = this.salaireMensuelMoyen();
    return typeof com === 'number' && com >= 0 && typeof sal === 'number' && sal >= 0;
  }

  // --- Handlers — toute modification manuelle efface le badge IA du champ ---

  onDateEntreeChange(value: string | null): void {
    this.dateEntree.set(value || null);
    this.provenanceDateEntree.set(null);
  }

  onDateRuptureChange(value: string | null): void {
    this.dateRupture.set(value || null);
    this.provenanceDateRupture.set(null);
  }

  onCommissionsChange(value: number | string | null): void {
    const num = typeof value === 'number' ? value : Number.parseFloat(String(value ?? ''));
    this.commissionsAnnuellesMoyennes.set(Number.isFinite(num) && num >= 0 ? num : 0);
    this.provenanceCommissions.set(null);
  }

  onSalaireChange(value: number | string | null): void {
    const num = typeof value === 'number' ? value : Number.parseFloat(String(value ?? ''));
    this.salaireMensuelMoyen.set(Number.isFinite(num) && num >= 0 ? num : 0);
  }

  onCauseRuptureChange(value: VrpCauseRupture): void {
    this.causeRupture.set(value);
  }

  onTypeVrpChange(value: VrpType): void {
    this.typeVrp.set(value);
  }

  onClienteleDeveloppeeChange(value: boolean): void {
    this.clienteleDeveloppee.set(value);
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request: VrpIndemniteClienteleRequest = {
      dateEntree: this.dateEntree(),
      dateRupture: this.dateRupture(),
      causeRupture: this.causeRupture(),
      typeVrp: this.typeVrp(),
      commissionsAnnuellesMoyennes: this.commissionsAnnuellesMoyennes(),
      salaireMensuelMoyen: this.salaireMensuelMoyen(),
      clienteleDeveloppee: this.clienteleDeveloppee(),
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.applyResult(r);
        this.calculating.set(false);
        this.snackBar.open('Analyse VRP calculée', 'OK', { duration: 2500 });
        if (!this.standaloneMode) this.dashboardRefresh?.triggerRefresh();
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.calculating.set(false);
        const msg = err?.error?.message || err?.error || 'Erreur lors du calcul';
        this.snackBar.open(String(msg), 'Fermer', { duration: 5000, panelClass: 'snack-error' });
        this.cdr.markForCheck();
      },
    });
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.applyResult(r);
        this.loading.set(false);
        this.cdr.markForCheck();
      },
      error: () => {
        // 404 attendu si aucune analyse — on reste en mode formulaire.
        this.loading.set(false);
        this.cdr.markForCheck();
      },
    });
  }

  private applyResult(r: VrpIndemniteClienteleResponse): void {
    this.result.set(r);
    this.hydrateForm(r);
    this.showForm.set(false);
  }

  /**
   * Ré-injecte le snapshot d'inputs de la réponse dans les champs du formulaire
   * — sans quoi un clic « Modifier » repartirait des valeurs par défaut.
   */
  private hydrateForm(r: VrpIndemniteClienteleResponse): void {
    this.dateEntree.set(r.dateEntree);
    this.dateRupture.set(r.dateRupture);
    this.causeRupture.set(r.causeRupture);
    this.typeVrp.set(r.typeVrp);
    this.commissionsAnnuellesMoyennes.set(r.commissionsAnnuellesMoyennes);
    this.salaireMensuelMoyen.set(r.salaireMensuelMoyen);
    this.clienteleDeveloppee.set(!!r.clienteleDeveloppee);
    // Valeurs persistées = saisie avocat — jamais de badge IA.
    this.provenanceDateEntree.set(null);
    this.provenanceDateRupture.set(null);
    this.provenanceCommissions.set(null);
  }

  // ---------------------------------------------------------------------------
  // Builders d'alertes F-IA-03 (un par date croisable)
  // ---------------------------------------------------------------------------

  /** Date (entrée / rupture) — divergence stricte F-96 / question IA. */
  private buildDateAlert(
    field: VrpAlertField,
    f96Code: string,
    user: string | null,
  ): VrpCoherenceAlert | null {
    if (!user) return null;
    const builder = CoherenceAlertBuilder.forField<VrpAlertField>(field);
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== f96Code) continue;
      const ev = chk.expectedValue?.trim();
      if (!ev || ev === user) continue;
      builder.addSource('F96', {
        expectedDisplay: ev,
        reason: `Checklist procédurale : date attendue le ${ev}${chk.raison ? ' (' + chk.raison + ')' : ''}`,
      });
      break;
    }
    for (const q of this.aiQuestionsSignal()) {
      if (q.critereCode?.toUpperCase() !== f96Code) continue;
      const ev = q.expectedValue?.trim();
      if (!ev || ev === user) continue;
      builder.addSource('QUESTION_IA', {
        expectedDisplay: ev,
        reason: `Question complémentaire : "${q.questionText}" → "${q.answerText}"`,
      });
      break;
    }
    return builder.build();
  }

  alertTooltip(alert: VrpCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: VrpCoherenceAlert): string {
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

  // ---------------------------------------------------------------------------
  // Helpers d'affichage du résultat
  // ---------------------------------------------------------------------------

  /** Éligibilité → classe de bannière. Rouge réservé à NON_DUE. */
  eligibiliteBannerClass(eligibilite: VrpEligibiliteClientele): string {
    return eligibilite === 'DUE'
      ? 'vrp-verdict-banner vrp-verdict-banner--due'
      : 'vrp-verdict-banner vrp-verdict-banner--non-due';
  }

  eligibiliteBannerLabel(eligibilite: VrpEligibiliteClientele): string {
    return eligibilite === 'DUE'
      ? 'Indemnité de clientèle DUE'
      : 'Indemnité de clientèle NON DUE';
  }

  eligibiliteBannerIcon(eligibilite: VrpEligibiliteClientele): string {
    return eligibilite === 'DUE' ? 'check_circle' : 'cancel';
  }

  optionRecommandeeLabel(option: VrpOptionRecommandee): string {
    return option === 'INDEMNITE_CLIENTELE'
      ? 'Indemnité de clientèle (L.7313-13)'
      : 'Indemnité légale de licenciement (R.1234-2)';
  }
}
