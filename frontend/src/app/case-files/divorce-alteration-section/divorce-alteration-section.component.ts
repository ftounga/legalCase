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

import { DivorceAlterationService } from '../../core/services/divorce-alteration.service';
import {
  DivorceAlterationRequest,
  DivorceAlterationResponse,
  DivorceAlterationVerdict,
  FamilleAiData,
} from '../../core/models/divorce-alteration.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { DivorceAlterationPrefillRules } from './divorce-alteration-section-prefill-rules';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { AiQuestion } from '../../core/models/ai-question.model';
import { PieceManquanteEntry } from '../../core/models/case-analysis.model';
import { CoherencePopoverTriggerDirective } from '../../shared/coherence-popover/coherence-popover-trigger.directive';
import { CoherenceAlert } from '../../shared/coherence-popover/coherence-alert.model';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';
import { CoherenceAlertBuilder } from '../../shared/coherence-popover/coherence-alert-builder';

/**
 * SF-FA-08-02 : champs de form audités par F-IA-03.
 */
export type DAAlertField = 'DUREE_MARIAGE' | 'REVENUS_EPOUX1' | 'REVENUS_EPOUX2';

export type DACoherenceAlert = CoherenceAlert<DAAlertField>;

/** SF-FA-08-02 : seuil divergence revenus (aligné F-DT-09 / F-DT-11). */
const REVENUS_DIVERGENCE_RATIO = 0.10;

/** SF-FA-08-02 : seuil divergence durée mariage (1 an d'écart). */
const DUREE_MARIAGE_DIVERGENCE_ANNEES = 1;

/**
 * SF-FA-08-02 : composant outil décisionnel "Divorce pour altération
 * définitive du lien conjugal" (art. 237 Cciv, F-FA-08).
 *
 * Pattern emprunté au canonique `harcelement-licenciement-nul-section`
 * (palette navy / or — pas de rouge dominant) + `immigration-title-decision-section`
 * (pré-fill IA + alertes cohérence multi-sources).
 *
 * Outil single-country FR. La gate `workspaceCountry === 'BELGIQUE'`
 * masque le formulaire et redirige vers F-FA-11 (à venir).
 */
@Component({
  selector: 'app-divorce-alteration-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule, DecimalPipe,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatSlideToggleModule, MatProgressSpinnerModule,
    LegalCitationsPipe,
    CoherencePopoverTriggerDirective,
    ToolJurisprudenceCitationsComponent
  ],
  templateUrl: './divorce-alteration-section.component.html',
  styleUrl: './divorce-alteration-section.component.scss',
})
export class DivorceAlterationSectionComponent implements OnInit, OnChanges {
  // F-JU-03 SF-JU-03-05 — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-FA-08-divorce-alteration';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'DIVORCE — ALTÉRATION DÉFINITIVE LIEN CONJUGAL';
  static readonly TOOL_ICON = 'balance';

  /**
   * F-236 SF-236-02 — Compteur pré-fill miroir de `prefillFromAi()`.
   * Délègue au helper partagé `DivorceAlterationPrefillRules`.
   */
  static getPrefillCount(input: {
    aiData?: FamilleAiData | null;
    procedureChecks?: unknown[];
    aiQuestions?: unknown[];
    piecesManquantes?: unknown[];
    triggerEvents?: unknown[];
    workspaceCountry?: string;
  }): number {
    return DivorceAlterationPrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';
  /** SF-FA-08-02 : pré-fill IA Famille — facultatif, no-op si absent. */
  @Input() aiData?: FamilleAiData | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;

  // Snapshots signals
  private aiDataSignal = signal<FamilleAiData | null | undefined>(undefined);
  private procedureChecksSignal = signal<ProcedureCheck[]>([]);
  private aiQuestionsSignal = signal<AiQuestion[]>([]);
  private piecesManquantesSignal = signal<PieceManquanteEntry[]>([]);

  // F-177 SF-177-03b : force l'expansion (mode modal F-177).
  @Input() forceExpanded = false;


  /**
   * F-163 SF-163-02c — Mode simulateur autonome (hors dossier client).
   *
   * Quand `true` :
   *  - Bannière « 🧪 Mode simulateur » affichée en haut.
   *  - `prefillFromAi()` court-circuité.
   *  - `coherenceAlerts` retourne `{}`.
   *  - `loadExisting()` court-circuité.
   *  - `calculate()` POSTe sur `/api/v1/simulators/F-FA-08-divorce-alteration/calculate`.
   *  - `triggerRefresh()` jamais invoqué.
   *
   * Default `false` — mode case-file scoped inchangé.
   */
  @Input() standaloneMode: boolean = false;
  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<DivorceAlterationResponse | null>(null);

  // Form signals (8 champs)
  dateCessationVieCommune = signal<string | null>(null);
  preuvesSeparationDocumentaires = signal<boolean>(false);
  tentativesReconciliation = signal<boolean>(false);
  dureeMariageAnnees = signal<number | null>(null);
  revenusAnnuelsEpoux1Eur = signal<number | null>(null);
  revenusAnnuelsEpoux2Eur = signal<number | null>(null);
  patrimoineCommunSignificatif = signal<boolean>(false);
  dateAssignation = signal<string | null>(null);

  // Provenance IA par champ pré-remplissable
  provenanceDateCessation = signal<'IA' | null>(null);
  provenanceDureeMariage = signal<'IA' | null>(null);
  provenanceRevenus1 = signal<'IA' | null>(null);
  provenanceRevenus2 = signal<'IA' | null>(null);
  provenancePatrimoine = signal<'IA' | null>(null);

  /** SF-FA-08-02 : alertes cohérence (gate showForm). */
  coherenceAlerts = computed<Partial<Record<DAAlertField, DACoherenceAlert>>>(() => {
    // F-163 SF-163-02c : aucune source IA en standalone.
    if (this.standaloneMode) return {};
    if (!this.showForm()) return {};
    const alerts: Partial<Record<DAAlertField, DACoherenceAlert>> = {};
    const dureeAlert = this.buildDureeMariageAlert();
    if (dureeAlert) alerts.DUREE_MARIAGE = dureeAlert;
    const r1Alert = this.buildRevenusAlert(
      'REVENUS_EPOUX1',
      this.revenusAnnuelsEpoux1Eur(),
      this.aiDataSignal()?.revenusAnnuelsEpoux1Eur,
      'DA_REVENUS_EPOUX1',
    );
    if (r1Alert) alerts.REVENUS_EPOUX1 = r1Alert;
    const r2Alert = this.buildRevenusAlert(
      'REVENUS_EPOUX2',
      this.revenusAnnuelsEpoux2Eur(),
      this.aiDataSignal()?.revenusAnnuelsEpoux2Eur,
      'DA_REVENUS_EPOUX2',
    );
    if (r2Alert) alerts.REVENUS_EPOUX2 = r2Alert;
    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    return { total: values.length, blockers: 0 };
  });

  constructor(
    private service: DivorceAlterationService,
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
      // F-163 SF-163-02c : pas de dossier à interroger en standalone.
      this.collapsed.set(false);
      this.loading.set(false);
      this.showForm.set(true);
      return;
    }
    this.load();
  }

  ngOnChanges(changes: SimpleChanges): void {
    // F-177 SF-177-03b : applique le forceExpanded quand il passe à true en cours de vie.
    if (changes['forceExpanded'] && this.forceExpanded) this.collapsed.set(false);
    if (changes['aiData']) this.aiDataSignal.set(this.aiData);
    if (changes['procedureChecks']) this.procedureChecksSignal.set(this.procedureChecks ?? []);
    if (changes['aiQuestions']) this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    if (changes['piecesManquantes']) this.piecesManquantesSignal.set(this.piecesManquantes ?? []);

    if (changes['aiData'] && !changes['aiData'].firstChange
        && this.showForm() && !this.result() && !this.standaloneMode) {
      this.prefillFromAi();
    }
  }

  toggleCollapse(): void {
    this.collapsed.update((v) => !v);
  }

  /** SF-FA-08-02 : true si la gate FR est satisfaite. */
  isFranceGate(): boolean {
    return this.workspaceCountry === 'FRANCE';
  }

  // ---------------------------------------------------------------------------
  // Form / handlers
  // ---------------------------------------------------------------------------

  formValid(): boolean {
    if (!this.isFranceGate()) return false;
    const date = this.dateCessationVieCommune();
    const duree = this.dureeMariageAnnees();
    const r1 = this.revenusAnnuelsEpoux1Eur();
    const r2 = this.revenusAnnuelsEpoux2Eur();
    if (!date) return false;
    if (duree === null || duree < 0) return false;
    if (r1 === null || r1 < 0) return false;
    if (r2 === null || r2 < 0) return false;
    return true;
  }

  onDateCessationChange(value: string | null): void {
    this.dateCessationVieCommune.set(value || null);
    this.provenanceDateCessation.set(null);
  }

  onDureeMariageChange(value: number | null): void {
    this.dureeMariageAnnees.set(value);
    this.provenanceDureeMariage.set(null);
  }

  onRevenus1Change(value: number | null): void {
    this.revenusAnnuelsEpoux1Eur.set(value);
    this.provenanceRevenus1.set(null);
  }

  onRevenus2Change(value: number | null): void {
    this.revenusAnnuelsEpoux2Eur.set(value);
    this.provenanceRevenus2.set(null);
  }

  onPatrimoineChange(value: boolean): void {
    this.patrimoineCommunSignificatif.set(value);
    this.provenancePatrimoine.set(null);
  }

  onPreuvesChange(value: boolean): void {
    this.preuvesSeparationDocumentaires.set(value);
  }

  onReconciliationChange(value: boolean): void {
    this.tentativesReconciliation.set(value);
  }

  onDateAssignationChange(value: string | null): void {
    this.dateAssignation.set(value || null);
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // ---------------------------------------------------------------------------
  // Pré-fill IA
  // ---------------------------------------------------------------------------

  private prefillFromAi(): void {
    // F-236 SF-236-02 — délégation au helper partagé (parité runtime/static).
    const helperInput = { aiData: this.aiDataSignal() };
    const date = DivorceAlterationPrefillRules.computeDateCessation(helperInput);
    if (date !== null
        && (this.dateCessationVieCommune() === null || this.provenanceDateCessation() === 'IA')) {
      this.dateCessationVieCommune.set(date);
      this.provenanceDateCessation.set('IA');
    }
    const duree = DivorceAlterationPrefillRules.computeDureeMariage(helperInput);
    if (duree !== null
        && (this.dureeMariageAnnees() === null || this.provenanceDureeMariage() === 'IA')) {
      this.dureeMariageAnnees.set(duree);
      this.provenanceDureeMariage.set('IA');
    }
    const r1 = DivorceAlterationPrefillRules.computeRevenusEpoux1(helperInput);
    if (r1 !== null
        && (this.revenusAnnuelsEpoux1Eur() === null || this.provenanceRevenus1() === 'IA')) {
      this.revenusAnnuelsEpoux1Eur.set(r1);
      this.provenanceRevenus1.set('IA');
    }
    const r2 = DivorceAlterationPrefillRules.computeRevenusEpoux2(helperInput);
    if (r2 !== null
        && (this.revenusAnnuelsEpoux2Eur() === null || this.provenanceRevenus2() === 'IA')) {
      this.revenusAnnuelsEpoux2Eur.set(r2);
      this.provenanceRevenus2.set('IA');
    }
    const patrimoine = DivorceAlterationPrefillRules.computePatrimoineCommun(helperInput);
    if (patrimoine !== null
        && (this.provenancePatrimoine() === 'IA' || !this.patrimoineCommunSignificatif())) {
      this.patrimoineCommunSignificatif.set(patrimoine);
      this.provenancePatrimoine.set('IA');
    }
  }

  // ---------------------------------------------------------------------------
  // Coherence alerts builders
  // ---------------------------------------------------------------------------

  private buildDureeMariageAlert(): DACoherenceAlert | null {
    if (!this.isFranceGate()) return null;
    const user = this.dureeMariageAnnees();
    if (user === null) return null;

    const builder = CoherenceAlertBuilder.forField<DAAlertField>('DUREE_MARIAGE');

    // 1. F-96
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'DA_DUREE_MARIAGE') continue;
      const ev = this.parseNumber(chk.expectedValue);
      if (ev === null) continue;
      if (Math.abs(ev - user) < DUREE_MARIAGE_DIVERGENCE_ANNEES) continue;
      builder.addSource('F96', {
        expectedDisplay: `${ev} an(s)`,
        reason: `Checklist procédurale : durée mariage ${ev} an(s)`
          + (chk.raison ? ' (' + chk.raison + ')' : ''),
      });
      break;
    }

    // 2. QUESTION_IA "oui"
    for (const q of this.aiQuestionsSignal()) {
      if (q.critereCode?.toUpperCase() !== 'DA_DUREE_MARIAGE') continue;
      const answer = q.answerText?.trim().toLowerCase();
      if (!answer) continue;
      const isOui = answer === 'oui' || answer.startsWith('oui ')
        || answer.startsWith('oui,') || answer.startsWith('oui.');
      if (!isOui) continue;
      const ev = this.parseNumber(q.expectedValue);
      if (ev === null) continue;
      if (Math.abs(ev - user) < DUREE_MARIAGE_DIVERGENCE_ANNEES) continue;
      builder.addSource('QUESTION_IA', {
        expectedDisplay: `${ev} an(s)`,
        reason: `Question complémentaire : "${q.questionText}" → "${q.answerText}"`,
      });
      break;
    }

    // 3. IA aiData.dureeMariageAnnees
    const iaDuree = this.aiDataSignal()?.dureeMariageAnnees;
    if (typeof iaDuree === 'number' && iaDuree >= 0
        && Math.abs(iaDuree - user) >= DUREE_MARIAGE_DIVERGENCE_ANNEES) {
      builder.addSource('IA', {
        expectedDisplay: `${iaDuree} an(s)`,
        reason: `Analyse du dossier : durée du mariage ${iaDuree} an(s)`,
      });
    }

    // 4. PIECE_MANQUANTE
    const piece = this.findPieceManquante(['DA_DUREE_MARIAGE']);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  private buildRevenusAlert(field: DAAlertField,
                            userValue: number | null,
                            iaValue: number | null | undefined,
                            critereCode: string): DACoherenceAlert | null {
    if (!this.isFranceGate()) return null;
    if (typeof userValue !== 'number' || userValue <= 0) return null;

    const builder = CoherenceAlertBuilder.forField<DAAlertField>(field);

    // 1. F-96
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== critereCode.toUpperCase()) continue;
      const ev = this.parseNumber(chk.expectedValue);
      if (ev === null || ev <= 0) continue;
      const ratio = Math.abs(userValue - ev) / ev;
      if (ratio <= REVENUS_DIVERGENCE_RATIO) continue;
      builder.addSource('F96', {
        expectedDisplay: this.formatEuros(ev),
        reason: `Checklist procédurale : revenus ${this.formatEuros(ev)}`
          + (chk.raison ? ' (' + chk.raison + ')' : ''),
      });
      break;
    }

    // 2. QUESTION_IA "oui"
    for (const q of this.aiQuestionsSignal()) {
      if (q.critereCode?.toUpperCase() !== critereCode.toUpperCase()) continue;
      const answer = q.answerText?.trim().toLowerCase();
      if (!answer) continue;
      const isOui = answer === 'oui' || answer.startsWith('oui ')
        || answer.startsWith('oui,') || answer.startsWith('oui.');
      if (!isOui) continue;
      const ev = this.parseNumber(q.expectedValue);
      if (ev === null || ev <= 0) continue;
      const ratio = Math.abs(userValue - ev) / ev;
      if (ratio <= REVENUS_DIVERGENCE_RATIO) continue;
      builder.addSource('QUESTION_IA', {
        expectedDisplay: this.formatEuros(ev),
        reason: `Question complémentaire : "${q.questionText}" → "${q.answerText}"`,
      });
      break;
    }

    // 3. IA aiData
    if (typeof iaValue === 'number' && iaValue > 0) {
      const ratio = Math.abs(userValue - iaValue) / iaValue;
      if (ratio > REVENUS_DIVERGENCE_RATIO) {
        builder.addSource('IA', {
          expectedDisplay: this.formatEuros(iaValue),
          reason: `Analyse du dossier : revenus ${this.formatEuros(iaValue)}`,
        });
      }
    }

    // 4. PIECE_MANQUANTE
    const piece = this.findPieceManquante([critereCode]);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  private parseNumber(raw: string | null | undefined): number | null {
    if (raw === null || raw === undefined || raw === '') return null;
    const n = Number(raw);
    return Number.isFinite(n) ? n : null;
  }

  private formatEuros(value: number): string {
    return `${Math.round(value).toLocaleString('fr-FR')} €`;
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

  alertTooltip(alert: DACoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: DACoherenceAlert): string {
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
  // Verdict / display helpers
  // ---------------------------------------------------------------------------

  verdictLabel(verdict: DivorceAlterationVerdict): string {
    switch (verdict) {
      case 'ELEVEE': return 'Probabilité élevée';
      case 'MOYENNE': return 'Probabilité moyenne';
      case 'FAIBLE': return 'Probabilité faible';
    }
  }

  verdictClass(verdict: DivorceAlterationVerdict): string {
    switch (verdict) {
      case 'ELEVEE': return 'verdict-banner verdict-banner--ok';
      case 'MOYENNE': return 'verdict-banner verdict-banner--warn';
      case 'FAIBLE': return 'verdict-banner verdict-banner--ko';
    }
  }

  // ---------------------------------------------------------------------------
  // HTTP
  // ---------------------------------------------------------------------------

  calculate(): void {
    if (!this.formValid()) return;
    const request: DivorceAlterationRequest = {
      dateCessationVieCommune: this.dateCessationVieCommune()!,
      preuvesSeparationDocumentaires: this.preuvesSeparationDocumentaires(),
      tentativesReconciliation: this.tentativesReconciliation(),
      dureeMariageAnnees: this.dureeMariageAnnees()!,
      revenusAnnuelsEpoux1Eur: this.revenusAnnuelsEpoux1Eur()!,
      revenusAnnuelsEpoux2Eur: this.revenusAnnuelsEpoux2Eur()!,
      patrimoineCommunSignificatif: this.patrimoineCommunSignificatif(),
      dateAssignation: this.dateAssignation(),
    };
    this.calculating.set(true);
    // F-163 SF-163-02c : en standalone, POST sur le dispatcher générique.

    const request$ = this.standaloneMode

      ? this.service.calculateStandalone(request)

      : this.service.calculate(this.caseFileId, request);

    request$.subscribe({
      next: (r) => {
        this.result.set(r);
        this.applyResult(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Analyse divorce calculée', 'OK', { duration: 2500 });
        // F-163 SF-163-02c : pas de dashboard à rafraîchir en standalone.

        if (!this.standaloneMode) { this.dashboardRefresh?.triggerRefresh(); }
      },
      error: (err) => {
        this.calculating.set(false);
        const msg = err?.error?.message || err?.error || 'Erreur lors du calcul';
        this.snackBar.open(String(msg), 'Fermer',
          { duration: 5000, panelClass: 'snack-error' });
      },
    });
  }

  private load(): void {
    if (!this.isFranceGate()) {
      // Pas d'appel backend en BE — l'outil n'est pas disponible.
      return;
    }
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.applyResult(r);
        this.provenanceDateCessation.set(null);
        this.provenanceDureeMariage.set(null);
        this.provenanceRevenus1.set(null);
        this.provenanceRevenus2.set(null);
        this.provenancePatrimoine.set(null);
        this.showForm.set(false);
        this.loading.set(false);
      },
      error: () => {
        // 404 attendu si pas d'analyse persistée — on reste en form mode
        // et on tente le pré-fill IA.
        this.prefillFromAi();
        this.loading.set(false);
      },
    });
  }

  private applyResult(r: DivorceAlterationResponse): void {
    this.dateCessationVieCommune.set(r.dateCessationVieCommune);
    this.preuvesSeparationDocumentaires.set(r.preuvesSeparationDocumentaires);
    this.tentativesReconciliation.set(r.tentativesReconciliation);
    this.dureeMariageAnnees.set(r.dureeMariageAnnees);
    this.revenusAnnuelsEpoux1Eur.set(r.revenusAnnuelsEpoux1Eur);
    this.revenusAnnuelsEpoux2Eur.set(r.revenusAnnuelsEpoux2Eur);
    this.patrimoineCommunSignificatif.set(r.patrimoineCommunSignificatif);
    this.dateAssignation.set(r.dateAssignation);
  }
}
