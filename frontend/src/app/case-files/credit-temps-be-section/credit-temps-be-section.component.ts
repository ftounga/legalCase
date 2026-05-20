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
import { MatSnackBar } from '@angular/material/snack-bar';
import { CreditTempsBeService } from '../../core/services/credit-temps-be.service';
import {
  CreditTempsBeRequest,
  CreditTempsBeResponse,
  CreditTempsRegime,
  CreditTempsMotif,
  DureeReductionType,
  REGIME_LABELS,
  MOTIF_LABELS,
  DUREE_REDUCTION_LABELS,
} from '../../core/models/credit-temps-be.model';
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
  CreditTempsBeSectionPrefillRules,
  computeDateDemande,
} from './credit-temps-be-section-prefill-rules';

/**
 * SF-DT-29-02 : champs d'alerte F-IA-03 exposés par l'outil F-DT-29
 * (crédit-temps BE).
 * - ANCIENNETE : divergence si IA a `dateEntree` calculée en mois et l'avocat
 *   saisit un nombre de mois éloigné (> 6 mois d'écart absolu).
 * - AGE : divergence si IA a `ageDemandeurAnnees` et l'avocat saisit
 *   un nombre éloigné (> 1 an d'écart absolu).
 */
export type CreditTempsAlertField = 'ANCIENNETE' | 'AGE';

export type CreditTempsAlertSource = CoherenceAlertSource;
export type CreditTempsCoherenceAlert = CoherenceAlert<CreditTempsAlertField>;

/** Seuil d'écart absolu (mois) pour l'ancienneté avant alerte F-IA-03. */
const ANCIENNETE_DIVERGENCE_MOIS = 6;

/** Seuil d'écart absolu (années) pour l'âge avant alerte F-IA-03. */
const AGE_DIVERGENCE_ANNEES = 1;

/**
 * SF-DT-29-02 : outil décisionnel "Crédit-temps / interruption de carrière BE".
 * Régimes CCT 103 (avec/sans motif) + AR 29/10/1997 (fin de carrière).
 * BE uniquement (bannière info si dossier FR — pas masquage silencieux).
 *
 * Consomme l'API SF-DT-29-01. Affiché conditionnellement par le panel F-IA-04
 * (tool_id 'F-DT-29-credit-temps-be').
 *
 * Patterns de référence :
 * - Template canonique : `harcelement-licenciement-nul-section` (F-DT-11-02).
 * - Pattern jumeau BE : `avantages-conventionnels-be-section` (F-DT-28-02).
 * - Helper partagé : `CoherenceAlertBuilder` + `CoherenceAlert<F>` (SF-155-05).
 */
@Component({
  selector: 'app-credit-temps-be-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule, DecimalPipe,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatProgressSpinnerModule,
    LegalCitationsPipe,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './credit-temps-be-section.component.html',
  styleUrl: './credit-temps-be-section.component.scss',
})
export class CreditTempsBeSectionComponent implements OnInit, OnChanges {
  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'CRÉDIT-TEMPS / INTERRUPTION DE CARRIÈRE BE';

  /** F-177 SF-177-12 / F-236 SF-236-02 — délègue au helper partagé (parité runtime). */
  static getPrefillCount(input: {
    aiData?: any;
    procedureChecks?: any[];
    aiQuestions?: any[];
    piecesManquantes?: any[];
    triggerEvents?: any[];
    workspaceCountry?: string;
  }): number {
    return CreditTempsBeSectionPrefillRules.computePrefillCount({
      aiData: input.aiData,
      workspaceCountry: input.workspaceCountry,
    });
  }

  static readonly TOOL_ICON = 'schedule';

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
  result = signal<CreditTempsBeResponse | null>(null);

  // Form fields
  regime = signal<CreditTempsRegime | null>(null);
  motif = signal<CreditTempsMotif | null>(null);
  ancienneteEntrepriseMois = signal<number | null>(null);
  tailleEntrepriseEtp = signal<number | null>(null);
  dureeReductionType = signal<DureeReductionType | null>(null);
  ageDemandeurAnnees = signal<number | null>(null);
  dateDemande = signal<string | null>(null);

  /** Provenance IA pour les champs pré-remplissables. */
  provenanceAnciennete = signal<'IA' | null>(null);
  provenanceAge = signal<'IA' | null>(null);
  /** SF-246-23 BELGIQUE — provenance IA pour dateDemande. */
  provenanceDateDemande = signal<'IA' | null>(null);

  // SF-IA-03-15c : map {sourceKey → explanations} pour le popover.
  sourceExplanations = signal<Map<string, SourceExplanation[]>>(new Map());

  /** Listes pour mat-select. */
  readonly regimeOptions = REGIME_LABELS;
  readonly motifOptions = MOTIF_LABELS;
  readonly dureeReductionOptions = DUREE_REDUCTION_LABELS;

  isBelgium = computed<boolean>(() => this.workspaceCountry === 'BELGIQUE');

  /** True si motif obligatoire (régime AVEC_MOTIF). */
  motifRequired = computed<boolean>(() => this.regime() === 'AVEC_MOTIF');

  /**
   * Alertes de cohérence F-IA-03 par field.
   * Gate : uniquement en mode formulaire (pattern anti-bug SF-IA-03-12).
   */
  coherenceAlerts = computed<Partial<Record<CreditTempsAlertField, CreditTempsCoherenceAlert>>>(() => {
    // F-163 SF-163-02b : aucune source IA en standalone.
    if (this.standaloneMode) return {} as any;
    if (!this.showForm()) return {};
    const alerts: Partial<Record<CreditTempsAlertField, CreditTempsCoherenceAlert>> = {};
    const ancienneteA = this.buildAncienneteAlert();
    if (ancienneteA) alerts.ANCIENNETE = ancienneteA;
    const ageA = this.buildAgeAlert();
    if (ageA) alerts.AGE = ageA;
    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    const blockers = values.filter((a) => a.severity === 'CRITICAL').length;
    return { total: values.length, blockers };
  });

  constructor(
    private service: CreditTempsBeService,
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

    // Ré-applique le prefill si aiData change, seulement en mode formulaire
    // ET qu'aucun résultat persisté n'a été chargé.
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
  explanationFor(field: CreditTempsAlertField): SourceExplanation[] {
    const key = field === 'ANCIENNETE' ? 'CREDIT_TEMPS_ANCIENNETE' : 'CREDIT_TEMPS_AGE';
    return this.sourceExplanations().get(key) ?? [];
  }

  alertTooltip(alert: CreditTempsCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: CreditTempsCoherenceAlert): string {
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
   * Calcule l'ancienneté en mois entiers depuis `dateEntree` (YYYY-MM-DD)
   * jusqu'à la date du jour. Retourne null si la date est invalide ou
   * postérieure à aujourd'hui.
   */
  private computeAncienneteMois(dateEntree: string | null | undefined): number | null {
    if (!dateEntree) return null;
    const d = new Date(dateEntree);
    if (isNaN(d.getTime())) return null;
    const today = new Date();
    if (d > today) return null;
    const months = (today.getFullYear() - d.getFullYear()) * 12
      + (today.getMonth() - d.getMonth());
    return months >= 0 ? months : null;
  }

  /**
   * Divergence ancienneté — écart absolu > 6 mois entre IA (calculée depuis
   * `dateEntree`) et saisie avocat. Multi-sources F96 / QUESTION_IA / IA /
   * PIECE_MANQUANTE.
   */
  private buildAncienneteAlert(): CreditTempsCoherenceAlert | null {
    const userAnc = this.ancienneteEntrepriseMois();
    if (typeof userAnc !== 'number' || userAnc < 0) return null;

    const builder = CoherenceAlertBuilder.forField<CreditTempsAlertField>('ANCIENNETE');

    // 1. F-96 — `critereCode === 'CREDIT_TEMPS_ANCIENNETE'` avec expectedValue numérique.
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'CREDIT_TEMPS_ANCIENNETE') continue;
      const ev = chk.expectedValue;
      if (!ev) continue;
      const evNum = Number(ev);
      if (isNaN(evNum) || evNum < 0) continue;
      if (Math.abs(userAnc - evNum) <= ANCIENNETE_DIVERGENCE_MOIS) continue;
      builder.addSource('F96', {
        expectedDisplay: `${evNum} mois`,
        reason: `Checklist procédurale : ancienneté attendue ${evNum} mois`
          + (chk.raison ? ` (${chk.raison})` : ''),
      });
      break;
    }

    // 2. QUESTION_IA — réponse "oui" sur `CREDIT_TEMPS_ANCIENNETE`.
    for (const q of this.aiQuestionsSignal()) {
      if (q.critereCode?.toUpperCase() !== 'CREDIT_TEMPS_ANCIENNETE') continue;
      const answer = q.answerText?.trim().toLowerCase();
      if (!answer) continue;
      const isOui = answer === 'oui' || answer.startsWith('oui ')
        || answer.startsWith('oui,') || answer.startsWith('oui.');
      if (!isOui) continue;
      const ev = q.expectedValue;
      if (!ev) continue;
      const evNum = Number(ev);
      if (isNaN(evNum) || evNum < 0) continue;
      if (Math.abs(userAnc - evNum) <= ANCIENNETE_DIVERGENCE_MOIS) continue;
      builder.addSource('QUESTION_IA', {
        expectedDisplay: `${evNum} mois`,
        reason: `Question complémentaire : "${q.questionText}" → "${q.answerText}"`,
      });
      break;
    }

    // 3. IA — calcul depuis `dateEntree`.
    const iaAnc = this.computeAncienneteMois(this.aiDataSignal()?.dateEntree);
    if (typeof iaAnc === 'number' && iaAnc >= 0
        && Math.abs(userAnc - iaAnc) > ANCIENNETE_DIVERGENCE_MOIS) {
      builder.addSource('IA', {
        expectedDisplay: `${iaAnc} mois`,
        reason: `Analyse du dossier : ancienneté ~${iaAnc} mois (depuis date d'entrée)`,
      });
    }

    // 4. PIECE_MANQUANTE — contributor enrichissant si alerte déjà initiée.
    const piece = this.findPieceManquante(['CREDIT_TEMPS_ANCIENNETE', 'ANCIENNETE_ENTREPRISE']);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  /**
   * Divergence âge — écart absolu > 1 an entre IA (`ageDemandeurAnnees`)
   * et saisie avocat. Multi-sources F96 / QUESTION_IA / IA / PIECE_MANQUANTE.
   */
  private buildAgeAlert(): CreditTempsCoherenceAlert | null {
    const userAge = this.ageDemandeurAnnees();
    if (typeof userAge !== 'number' || userAge < 0) return null;

    const builder = CoherenceAlertBuilder.forField<CreditTempsAlertField>('AGE');

    // 1. F-96 — `critereCode === 'CREDIT_TEMPS_AGE'`.
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'CREDIT_TEMPS_AGE') continue;
      const ev = chk.expectedValue;
      if (!ev) continue;
      const evNum = Number(ev);
      if (isNaN(evNum) || evNum < 0) continue;
      if (Math.abs(userAge - evNum) <= AGE_DIVERGENCE_ANNEES) continue;
      builder.addSource('F96', {
        expectedDisplay: `${evNum} ans`,
        reason: `Checklist procédurale : âge attendu ${evNum} ans`
          + (chk.raison ? ` (${chk.raison})` : ''),
      });
      break;
    }

    // 2. QUESTION_IA — réponse "oui" sur `CREDIT_TEMPS_AGE`.
    for (const q of this.aiQuestionsSignal()) {
      if (q.critereCode?.toUpperCase() !== 'CREDIT_TEMPS_AGE') continue;
      const answer = q.answerText?.trim().toLowerCase();
      if (!answer) continue;
      const isOui = answer === 'oui' || answer.startsWith('oui ')
        || answer.startsWith('oui,') || answer.startsWith('oui.');
      if (!isOui) continue;
      const ev = q.expectedValue;
      if (!ev) continue;
      const evNum = Number(ev);
      if (isNaN(evNum) || evNum < 0) continue;
      if (Math.abs(userAge - evNum) <= AGE_DIVERGENCE_ANNEES) continue;
      builder.addSource('QUESTION_IA', {
        expectedDisplay: `${evNum} ans`,
        reason: `Question complémentaire : "${q.questionText}" → "${q.answerText}"`,
      });
      break;
    }

    // 3. IA — `aiData.ageDemandeurAnnees`.
    const iaAge = this.aiDataSignal()?.ageDemandeurAnnees;
    if (typeof iaAge === 'number' && iaAge >= 0
        && Math.abs(userAge - iaAge) > AGE_DIVERGENCE_ANNEES) {
      builder.addSource('IA', {
        expectedDisplay: `${iaAge} ans`,
        reason: `Analyse du dossier : âge du demandeur ${iaAge} ans`,
      });
    }

    // 4. PIECE_MANQUANTE.
    const piece = this.findPieceManquante(['CREDIT_TEMPS_AGE', 'AGE_DEMANDEUR']);
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

  toggleCollapse(): void {
    this.collapsed.update((v) => !v);
  }

  formValid(): boolean {
    const reg = this.regime();
    if (!reg) return false;
    // Motif requis ssi régime AVEC_MOTIF.
    if (reg === 'AVEC_MOTIF' && !this.motif()) return false;
    const anc = this.ancienneteEntrepriseMois();
    if (anc === null || anc === undefined || anc < 0 || !Number.isInteger(anc)) return false;
    const taille = this.tailleEntrepriseEtp();
    if (taille === null || taille === undefined || taille < 0) return false;
    if (!this.dureeReductionType()) return false;
    const age = this.ageDemandeurAnnees();
    if (age === null || age === undefined || age < 0 || age > 75 || !Number.isInteger(age)) return false;
    if (!this.dateDemande()) return false;
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // Handlers onChange — effacent la provenance IA au 1er changement manuel.
  onRegimeChange(value: CreditTempsRegime | null): void {
    this.regime.set(value);
    // Si on quitte AVEC_MOTIF, on efface le motif (cohérence form).
    if (value !== 'AVEC_MOTIF') {
      this.motif.set(null);
    }
  }

  onMotifChange(value: CreditTempsMotif | null): void {
    this.motif.set(value);
  }

  onAncienneteChange(value: number | null): void {
    this.ancienneteEntrepriseMois.set(value === null || value === undefined ? null : value);
    this.provenanceAnciennete.set(null);
  }

  onTailleEtpChange(value: number | null): void {
    this.tailleEntrepriseEtp.set(value === null || value === undefined ? null : value);
  }

  onDureeReductionChange(value: DureeReductionType | null): void {
    this.dureeReductionType.set(value);
  }

  onAgeChange(value: number | null): void {
    this.ageDemandeurAnnees.set(value === null || value === undefined ? null : value);
    this.provenanceAge.set(null);
  }

  onDateDemandeChange(value: string | null): void {
    this.dateDemande.set(value || null);
    this.provenanceDateDemande.set(null); // SF-246-23 : reset provenance IA
  }

  /**
   * SF-DT-29-02 : pré-fill depuis `aiData` (TravailExtractedData).
   * F-236 SF-236-02 : la logique est déléguée au helper partagé
   * `CreditTempsBeSectionPrefillRules` pour garantir la parité avec le
   * static `getPrefillCount` (divergence impossible par construction).
   *
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

    // F-236 SF-236-02 : valeurs calculées par le helper partagé (parité static).
    const ruleInput = { aiData: ai, workspaceCountry: this.workspaceCountry };

    // 1. Ancienneté en mois — calculée depuis dateEntree.
    const iaAnc = CreditTempsBeSectionPrefillRules.computeAncienneteMois(ruleInput);
    if (iaAnc !== null) {
      if (this.ancienneteEntrepriseMois() === null
          || this.provenanceAnciennete() === 'IA') {
        this.ancienneteEntrepriseMois.set(iaAnc);
        this.provenanceAnciennete.set('IA');
      }
    }

    // 2. Âge du demandeur.
    const iaAge = CreditTempsBeSectionPrefillRules.computeAgeDemandeur(ruleInput);
    if (iaAge !== null) {
      if (this.ageDemandeurAnnees() === null
          || this.provenanceAge() === 'IA') {
        this.ageDemandeurAnnees.set(iaAge);
        this.provenanceAge.set('IA');
      }
    }

    // SF-246-23 BELGIQUE : date de demande de crédit-temps.
    const iaDemande = computeDateDemande(ruleInput);
    if (iaDemande !== null) {
      if (this.dateDemande() === null || this.provenanceDateDemande() === 'IA') {
        this.dateDemande.set(iaDemande);
        this.provenanceDateDemande.set('IA');
      }
    }
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request: CreditTempsBeRequest = {
      regime: this.regime()!,
      ancienneteEntrepriseMois: this.ancienneteEntrepriseMois()!,
      tailleEntrepriseEtp: this.tailleEntrepriseEtp()!,
      dureeReductionType: this.dureeReductionType()!,
      ageDemandeurAnnees: this.ageDemandeurAnnees()!,
      dateDemande: this.dateDemande()!,
    };
    if (this.regime() === 'AVEC_MOTIF' && this.motif()) {
      request.motif = this.motif()!;
    }
    this.calculating.set(true);
    (this.standaloneMode
      ? this.service.calculateStandalone(request)
      : this.service.calculate(this.caseFileId, request))
      .subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Crédit-temps analysé', 'OK', { duration: 2500 });
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
        this.result.set(r);
        this.regime.set(r.regime);
        // Note : on ne hydrate pas tous les champs depuis r (le backend ne renvoie
        // que la réponse calculée — pas tous les inputs). Si l'avocat clique
        // "Modifier", il revoit son dernier formulaire. Pattern accepté car les
        // inputs sont conservés dans les signals tant que la session est ouverte.
        // Valeurs persistées = saisie avocat — jamais de badge IA.
        this.provenanceAnciennete.set(null);
        this.provenanceAge.set(null);
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
   * Classe CSS du bandeau verdict.
   * - ELEVEE → navy (info, fond bleu doux).
   * - MOYENNE → or soutenu.
   * - FAIBLE → neutre (gris) — pas de rouge (pas d'urgence temporelle critique).
   */
  bannerClass(verdict: string | undefined | null): string {
    switch (verdict) {
      case 'ELEVEE': return 'credit-banner credit-banner--info';
      case 'MOYENNE': return 'credit-banner credit-banner--warning';
      case 'FAIBLE':
      default: return 'credit-banner credit-banner--neutre';
    }
  }

  /** Libellé humain d'un code régime. */
  regimeLabel(code: string | null | undefined): string {
    if (!code) return '';
    const found = REGIME_LABELS.find((r) => r.code === code);
    return found ? found.label : code;
  }
}
