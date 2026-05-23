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
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSnackBar } from '@angular/material/snack-bar';
import { AvantagesConventionnelsBeService } from '../../core/services/avantages-conventionnels-be.service';
import {
  AvantagesConventionnelsBeRequest,
  AvantagesConventionnelsBeResponse,
  COMMISSION_PARITAIRE_LABELS,
  CommissionParitaireBe,
} from '../../core/models/avantages-conventionnels-be.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import {
  PieceManquanteEntry,
  TravailExtractedData,
} from '../../core/models/case-analysis.model';
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
import {
  AvantagesConventionnelsBeSectionPrefillRules,
  computeCommissionParitaire,
  computeJoursTravailles,
  computeJoursPrestes,
} from './avantages-conventionnels-be-section-prefill-rules';

/**
 * SF-DT-28-02 : champs d'alerte F-IA-03 exposés par l'outil F-DT-28
 * (avantages conventionnels BE).
 * SF-246-23 : ajout COMMISSION_PARITAIRE, JOURS_TRAVAILLES, JOURS_PRESTES
 * (désormais extraits depuis travail_be_detection).
 */
export type AvantagesBeAlertField = 'SALAIRE' | 'COMMISSION_PARITAIRE' | 'JOURS_TRAVAILLES' | 'JOURS_PRESTES';

export type AvantagesBeAlertSource = CoherenceAlertSource;
export type AvantagesBeCoherenceAlert = CoherenceAlert<AvantagesBeAlertField>;

/**
 * Seuil d'écart relatif au-delà duquel on affiche une alerte de cohérence
 * entre le salaire mensuel extrait par l'IA et la saisie de l'avocat.
 * Aligné avec le seuil utilisé par F-DT-09, F-DT-11, F-DT-25, F-DT-27.
 */
const SALAIRE_DIVERGENCE_RATIO = 0.10;

const ANNEE_MIN = 2020;
const ANNEE_MAX = 2030;

/**
 * SF-DT-28-02 : outil décisionnel "Avantages conventionnels BE".
 * Calcule pécule de vacances simple + double pécule (loi 28/06/1971),
 * prime de fin d'année (CCT/CP), éco-chèques (CCT 98), chèques-repas,
 * et le total annuel.
 *
 * BE uniquement. L'équivalent FR (13e mois, prime conventionnelle,
 * tickets-restaurant) est géré directement par la convention collective
 * de l'entreprise — pas d'outil dédié actuellement.
 *
 * Consomme l'API SF-DT-28-01. Affiché conditionnellement par le panel
 * F-IA-04 (tool_id 'F-DT-28-avantages-conventionnels-be', règle
 * ALWAYS_ON BELGIQUE).
 *
 * Patterns de référence :
 * - Template canonique : `harcelement-licenciement-nul-section` (F-DT-11-02).
 * - Pattern BE-only + IA : `motif-grave-be-section` (SF-DT-27-02).
 * - Pattern sélecteur convention : `indemnite-preavis-section` (F-DT-25-02).
 * - Helper partagé : `CoherenceAlertBuilder` + `CoherenceAlert<F>` (SF-155-05).
 */
@Component({
  selector: 'app-avantages-conventionnels-be-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule, DecimalPipe,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatProgressSpinnerModule, MatSlideToggleModule,
    LegalCitationsPipe,
    CoherencePopoverTriggerDirective,
    ToolJurisprudenceCitationsComponent
  ],
  templateUrl: './avantages-conventionnels-be-section.component.html',
  styleUrl: './avantages-conventionnels-be-section.component.scss',
})
export class AvantagesConventionnelsBeSectionComponent implements OnInit, OnChanges {
  // F-JU-03 SF-JU-03 — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-DT-28-avantages-conventionnels-be';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'AVANTAGES CONVENTIONNELS BE';

  /** F-177 SF-177-12 / F-236 SF-236-02 — délègue au helper partagé (parité runtime). */
  static getPrefillCount(input: {
    aiData?: any;
    procedureChecks?: any[];
    aiQuestions?: any[];
    piecesManquantes?: any[];
    triggerEvents?: any[];
    workspaceCountry?: string;
  }): number {
    return AvantagesConventionnelsBeSectionPrefillRules.computePrefillCount({
      aiData: input.aiData,
      workspaceCountry: input.workspaceCountry,
    });
  }

  static readonly TOOL_ICON = 'card_giftcard';

  @Input() caseFileId!: string;
  // F-177 SF-177-03b : force l'expansion (mode modal F-177).
  @Input() forceExpanded = false;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'BELGIQUE';
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

  // Snapshots signal des inputs IA pour que `computed` réagisse.
  private aiDataSignal = signal<TravailExtractedData | null | undefined>(undefined);
  private procedureChecksSignal = signal<ProcedureCheck[]>([]);
  private aiQuestionsSignal = signal<AiQuestion[]>([]);
  private piecesManquantesSignal = signal<PieceManquanteEntry[]>([]);

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<AvantagesConventionnelsBeResponse | null>(null);

  // Form fields
  salaireMensuelBrutEur = signal<number | null>(null);
  joursTravaillesAnneePrecedente = signal<number | null>(null);
  anciennetteMois = signal<number | null>(null);
  commissionParitaire = signal<CommissionParitaireBe | null>(null);
  annee = signal<number | null>(new Date().getFullYear());
  doublePeculeVacancesPercu = signal<boolean>(false);
  primeFinAnneePrevueCcCp = signal<boolean>(false);
  ecoChequesPrevuCcCp = signal<boolean>(false);
  ecoChequesUtilisationDansAn = signal<boolean>(false);
  chequesRepasPrevu = signal<boolean>(false);
  joursPrestesEffectifs = signal<number | null>(null);

  /** Provenance IA pour le salaire. */
  provenanceSalaire = signal<'IA' | null>(null);
  /** SF-246-23 BELGIQUE — provenance IA pour la commission paritaire. */
  provenanceCommissionParitaire = signal<'IA' | null>(null);
  /** SF-246-23 BELGIQUE — provenance IA pour les jours travaillés. */
  provenanceJoursTravailles = signal<'IA' | null>(null);
  /** SF-246-23 BELGIQUE — provenance IA pour les jours prestés. */
  provenanceJoursPrestes = signal<'IA' | null>(null);

  // SF-IA-03-15c : map {sourceKey → explanations} pour le popover.
  sourceExplanations = signal<Map<string, SourceExplanation[]>>(new Map());

  /** Liste des CP exposées au mat-select. */
  readonly commissionParitaireOptions = COMMISSION_PARITAIRE_LABELS;

  /** Bornes années — utilisées dans la validation et les attributs HTML min/max. */
  readonly anneeMin = ANNEE_MIN;
  readonly anneeMax = ANNEE_MAX;

  isBelgium = computed<boolean>(() => this.workspaceCountry === 'BELGIQUE');

  /**
   * Alertes de cohérence F-IA-03 par field.
   * Gate : uniquement en mode formulaire (pattern anti-bug SF-IA-03-12).
   */
  coherenceAlerts = computed<Partial<Record<AvantagesBeAlertField, AvantagesBeCoherenceAlert>>>(() => {
    // F-163 SF-163-02b : aucune source IA en standalone.
    if (this.standaloneMode) return {} as any;
    if (!this.showForm()) return {};
    const alerts: Partial<Record<AvantagesBeAlertField, AvantagesBeCoherenceAlert>> = {};
    const salaireA = this.buildSalaireAlert();
    if (salaireA) alerts.SALAIRE = salaireA;
    // SF-246-23 : alertes pour CP et jours.
    const cpA = this.buildCommissionParitaireAlert();
    if (cpA) alerts.COMMISSION_PARITAIRE = cpA;
    const jtA = this.buildJoursTravaillesAlert();
    if (jtA) alerts.JOURS_TRAVAILLES = jtA;
    const jpA = this.buildJoursPrestesAlert();
    if (jpA) alerts.JOURS_PRESTES = jpA;
    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    const blockers = values.filter((a) => a.severity === 'CRITICAL').length;
    return { total: values.length, blockers };
  });

  constructor(
    private service: AvantagesConventionnelsBeService,
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
    if (this.isBelgium()) {
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

    // Ré-applique le prefill si aiData change, seulement si l'on est encore en
    // mode formulaire ET qu'aucun résultat persisté n'a été chargé.
    if (changes['aiData'] && !changes['aiData'].firstChange
        && this.isBelgium() && this.showForm() && !this.result()) {
      if (!this.standaloneMode) this.prefillFromAi();
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

  /** SF-IA-03-15c : map field → sourceKey. */
  explanationFor(field: AvantagesBeAlertField): SourceExplanation[] {
    const key = field === 'SALAIRE' ? 'salaire_brut_mensuel' : field;
    return this.sourceExplanations().get(key) ?? [];
  }

  alertTooltip(alert: AvantagesBeCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: AvantagesBeCoherenceAlert): string {
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
   * Divergence salaire — écart relatif > 10 % entre IA et saisie avocat.
   * Construit via CoherenceAlertBuilder partagé (SF-155-05).
   */
  private buildSalaireAlert(): AvantagesBeCoherenceAlert | null {
    const aiSalaire = this.aiDataSignal()?.salaireBrutMensuel;
    const userSalaire = this.salaireMensuelBrutEur();
    if (typeof aiSalaire !== 'number' || aiSalaire <= 0) return null;
    if (typeof userSalaire !== 'number' || userSalaire <= 0) return null;
    const ratio = Math.abs(userSalaire - aiSalaire) / aiSalaire;
    if (ratio <= SALAIRE_DIVERGENCE_RATIO) return null;
    const builder = CoherenceAlertBuilder.forField<AvantagesBeAlertField>('SALAIRE')
      .addSource('IA', {
        expectedDisplay: `${aiSalaire.toLocaleString('fr-FR')} €`,
        reason: `Analyse du dossier : salaire brut mensuel ~${aiSalaire.toLocaleString('fr-FR')} €`,
      });
    const piece = this.findPieceManquante(['SALAIRE_BRUT_MENSUEL', 'AVANTAGES_BE_SALAIRE']);
    if (piece) builder.addPieceManquante(piece);
    return builder.build();
  }

  /**
   * SF-246-23 BELGIQUE : divergence commission paritaire.
   * Alerte si la CP extraite par l'IA diffère de la CP saisie.
   */
  private buildCommissionParitaireAlert(): AvantagesBeCoherenceAlert | null {
    const userCp = this.commissionParitaire();
    if (!userCp) return null;
    const aiRaw = this.aiDataSignal()?.commissionParitaireBe;
    if (!aiRaw) return null;
    // Pas d'alerte si le code normalisé coïncide avec la saisie.
    const normalized = computeCommissionParitaire({
      aiData: { commissionParitaireBe: aiRaw },
      workspaceCountry: this.workspaceCountry,
    });
    if (!normalized || normalized === userCp) return null;
    return CoherenceAlertBuilder.forField<AvantagesBeAlertField>('COMMISSION_PARITAIRE')
      .withSeverity('WARNING')
      .addSource('IA', {
        expectedDisplay: aiRaw,
        reason: `Analyse du dossier : commission paritaire détectée "${aiRaw}"`,
      })
      .build();
  }

  /**
   * SF-246-23 BELGIQUE : divergence jours travaillés — écart absolu > 10 j.
   */
  private buildJoursTravaillesAlert(): AvantagesBeCoherenceAlert | null {
    const userJours = this.joursTravaillesAnneePrecedente();
    if (typeof userJours !== 'number' || userJours < 0) return null;
    const aiJours = this.aiDataSignal()?.joursTravaillesAnneePrecedenteBe;
    if (typeof aiJours !== 'number' || aiJours < 0) return null;
    if (Math.abs(userJours - aiJours) <= 10) return null;
    return CoherenceAlertBuilder.forField<AvantagesBeAlertField>('JOURS_TRAVAILLES')
      .withSeverity('WARNING')
      .addSource('IA', {
        expectedDisplay: `${aiJours} j`,
        reason: `Analyse du dossier : jours travaillés année précédente ~${aiJours} j`,
      })
      .build();
  }

  /**
   * SF-246-23 BELGIQUE : divergence jours prestés — écart absolu > 10 j.
   */
  private buildJoursPrestesAlert(): AvantagesBeCoherenceAlert | null {
    const userJours = this.joursPrestesEffectifs();
    if (typeof userJours !== 'number' || userJours < 0) return null;
    const aiJours = this.aiDataSignal()?.joursPrestesBe;
    if (typeof aiJours !== 'number' || aiJours < 0) return null;
    if (Math.abs(userJours - aiJours) <= 10) return null;
    return CoherenceAlertBuilder.forField<AvantagesBeAlertField>('JOURS_PRESTES')
      .withSeverity('WARNING')
      .addSource('IA', {
        expectedDisplay: `${aiJours} j`,
        reason: `Analyse du dossier : jours prestés depuis 1er avril ~${aiJours} j`,
      })
      .build();
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

  toggleCollapse(): void {
    this.collapsed.update((v) => !v);
  }

  formValid(): boolean {
    const sal = this.salaireMensuelBrutEur();
    const jrsTrav = this.joursTravaillesAnneePrecedente();
    const ancMois = this.anciennetteMois();
    const cp = this.commissionParitaire();
    const an = this.annee();
    const jrsPres = this.joursPrestesEffectifs();
    if (sal === null || sal === undefined || sal <= 0) return false;
    if (jrsTrav === null || jrsTrav === undefined
        || jrsTrav < 0 || jrsTrav > 365 || !Number.isInteger(jrsTrav)) return false;
    if (ancMois === null || ancMois === undefined
        || ancMois < 0 || !Number.isInteger(ancMois)) return false;
    if (!cp) return false;
    if (an === null || an === undefined
        || an < ANNEE_MIN || an > ANNEE_MAX || !Number.isInteger(an)) return false;
    if (jrsPres === null || jrsPres === undefined
        || jrsPres < 0 || !Number.isInteger(jrsPres)) return false;
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // Handlers onChange — effacent la provenance IA au 1er changement manuel.
  onSalaireChange(value: number | null): void {
    this.salaireMensuelBrutEur.set(value);
    this.provenanceSalaire.set(null);
  }

  onJoursTravaillesChange(value: number | null): void {
    this.joursTravaillesAnneePrecedente.set(value === null || value === undefined ? null : value);
    this.provenanceJoursTravailles.set(null); // SF-246-23 : reset provenance IA
  }

  onAnciennetteMoisChange(value: number | null): void {
    this.anciennetteMois.set(value === null || value === undefined ? null : value);
  }

  onCommissionParitaireChange(value: CommissionParitaireBe | null): void {
    this.commissionParitaire.set(value || null);
    this.provenanceCommissionParitaire.set(null); // SF-246-23 : reset provenance IA
  }

  onAnneeChange(value: number | null): void {
    this.annee.set(value === null || value === undefined ? null : value);
  }

  onDoublePeculeChange(value: boolean): void {
    this.doublePeculeVacancesPercu.set(!!value);
  }

  onPrimeFinAnneeChange(value: boolean): void {
    this.primeFinAnneePrevueCcCp.set(!!value);
  }

  onEcoChequesPrevuChange(value: boolean): void {
    this.ecoChequesPrevuCcCp.set(!!value);
  }

  onEcoChequesUtilisationChange(value: boolean): void {
    this.ecoChequesUtilisationDansAn.set(!!value);
  }

  onChequesRepasChange(value: boolean): void {
    this.chequesRepasPrevu.set(!!value);
  }

  onJoursPrestesChange(value: number | null): void {
    this.joursPrestesEffectifs.set(value === null || value === undefined ? null : value);
    this.provenanceJoursPrestes.set(null); // SF-246-23 : reset provenance IA
  }

  /**
   * SF-DT-28-02 : pré-fill depuis `aiData` (TravailExtractedData).
   * Règles (fail-open) :
   * - passe silencieusement si aiData absent
   * - ne pré-remplit QUE si le champ est encore vide (préserve les edits)
   * - n'écrase jamais si provenance !== 'IA'
   */
  private prefillFromAi(): void {
    // F-163 SF-163-02b : aucune source IA en standalone.
    if (this.standaloneMode) return;
    const ai = this.aiDataSignal();
    if (!ai) return;

    // F-236 SF-236-02 : valeur calculée par le helper partagé (parité static).
    const ruleInput = { aiData: ai, workspaceCountry: this.workspaceCountry };

    const salaire = AvantagesConventionnelsBeSectionPrefillRules.computeSalaireMensuelBrutEur(ruleInput);
    if (salaire !== null) {
      if (this.salaireMensuelBrutEur() === null || this.provenanceSalaire() === 'IA') {
        this.salaireMensuelBrutEur.set(salaire);
        this.provenanceSalaire.set('IA');
      }
    }

    // SF-246-23 BELGIQUE : 3 nouveaux champs depuis travail_be_detection.
    const cpCode = computeCommissionParitaire(ruleInput);
    if (cpCode !== null) {
      if (this.commissionParitaire() === null || this.provenanceCommissionParitaire() === 'IA') {
        this.commissionParitaire.set(cpCode as CommissionParitaireBe);
        this.provenanceCommissionParitaire.set('IA');
      }
    }

    const joursTrav = computeJoursTravailles(ruleInput);
    if (joursTrav !== null) {
      if (this.joursTravaillesAnneePrecedente() === null || this.provenanceJoursTravailles() === 'IA') {
        this.joursTravaillesAnneePrecedente.set(joursTrav);
        this.provenanceJoursTravailles.set('IA');
      }
    }

    const joursPrestes = computeJoursPrestes(ruleInput);
    if (joursPrestes !== null) {
      if (this.joursPrestesEffectifs() === null || this.provenanceJoursPrestes() === 'IA') {
        this.joursPrestesEffectifs.set(joursPrestes);
        this.provenanceJoursPrestes.set('IA');
      }
    }
  }

  /** True si l'IA a déduit le salaire brut d'un montant net (×1,30). */
  salaireEstDeduit(): boolean {
    return this.aiDataSignal()?.salaireEstDeduit === true;
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request: AvantagesConventionnelsBeRequest = {
      salaireMensuelBrutEur: this.salaireMensuelBrutEur()!,
      joursTravaillesAnneePrecedente: this.joursTravaillesAnneePrecedente()!,
      anciennetteMois: this.anciennetteMois()!,
      commissionParitaire: this.commissionParitaire()!,
      annee: this.annee()!,
      doublePeculeVacancesPercu: this.doublePeculeVacancesPercu(),
      primeFinAnneePrevueCcCp: this.primeFinAnneePrevueCcCp(),
      ecoChequesPrevuCcCp: this.ecoChequesPrevuCcCp(),
      ecoChequesUtilisationDansAn: this.ecoChequesUtilisationDansAn(),
      chequesRepasPrevu: this.chequesRepasPrevu(),
      joursPrestesEffectifs: this.joursPrestesEffectifs()!,
    };
    this.calculating.set(true);
    (this.standaloneMode
      ? this.service.calculateStandalone(request)
      : this.service.calculate(this.caseFileId, request))
      .subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Avantages conventionnels analysés', 'OK', { duration: 2500 });
        // F-163 SF-163-02b : pas de dashboard à rafraîchir en standalone.

        if (!this.standaloneMode) this.dashboardRefresh?.triggerRefresh();
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
    // F-163 SF-163-02b : en standalone, pas de dossier à interroger.

    if (this.standaloneMode) {

      this.loading.set(false);

      this.showForm.set(true);

      if (this.collapsed && typeof (this.collapsed as any).set === 'function') this.collapsed.set(false);

      return;

    }

    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.salaireMensuelBrutEur.set(r.salaireMensuelBrutEur);
        this.joursTravaillesAnneePrecedente.set(r.joursTravaillesAnneePrecedente);
        this.anciennetteMois.set(r.anciennetteMois);
        this.commissionParitaire.set(r.commissionParitaire as CommissionParitaireBe);
        this.annee.set(r.annee);
        this.doublePeculeVacancesPercu.set(r.doublePeculeVacancesPercu);
        this.primeFinAnneePrevueCcCp.set(r.primeFinAnneePrevueCcCp);
        this.ecoChequesPrevuCcCp.set(r.ecoChequesPrevuCcCp);
        this.ecoChequesUtilisationDansAn.set(r.ecoChequesUtilisationDansAn);
        this.chequesRepasPrevu.set(r.chequesRepasPrevu);
        this.joursPrestesEffectifs.set(r.joursPrestesEffectifs);
        // Valeurs persistées = saisie avocat — jamais de badge IA.
        this.provenanceSalaire.set(null);
        this.provenanceCommissionParitaire.set(null);  // SF-246-23
        this.provenanceJoursTravailles.set(null);      // SF-246-23
        this.provenanceJoursPrestes.set(null);         // SF-246-23
        this.showForm.set(false);
        this.loading.set(false);
      },
      error: () => {
        // 404 attendu si aucune analyse — on reste en mode formulaire.
        // Fallback pré-fill IA uniquement ici (pas si GET 200).
        if (!this.standaloneMode) this.prefillFromAi();
        this.loading.set(false);
      },
    });
  }

  /**
   * Classe CSS du bandeau de total. Navy si total > 0 (avantages dus),
   * neutre si 0 (avantages non dus dans la situation décrite).
   * Pas de rouge : pas d'urgence temporelle critique.
   */
  bannerClass(total: number | undefined | null): string {
    if (typeof total === 'number' && total > 0) {
      return 'avantages-banner avantages-banner--info';
    }
    return 'avantages-banner avantages-banner--neutre';
  }

  /** Renvoie le libellé humain d'une CP code. */
  commissionParitaireLabel(code: string | null | undefined): string {
    if (!code) return '';
    const found = COMMISSION_PARITAIRE_LABELS.find((c) => c.code === code);
    return found ? found.label : code;
  }
}
