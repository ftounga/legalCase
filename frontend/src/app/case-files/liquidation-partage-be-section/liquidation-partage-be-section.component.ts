import {
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
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { LiquidationPartageBeService } from '../../core/services/liquidation-partage-be.service';
import {
  DelaiStatutBe,
  EtapeStatutBe,
  LiquidationPartageBeRequest,
  LiquidationPartageBeResponse,
  LiquidationPartageBeVerdict,
} from '../../core/models/liquidation-partage-be.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { PieceManquanteEntry } from '../../core/models/case-analysis.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { AiQuestion } from '../../core/models/ai-question.model';
import { CoherencePopoverTriggerDirective } from '../../shared/coherence-popover/coherence-popover-trigger.directive';
import { CoherenceAlert } from '../../shared/coherence-popover/coherence-alert.model';
import { CoherenceAlertBuilder } from '../../shared/coherence-popover/coherence-alert-builder';
import { LiquidationPartageBeSectionPrefillRules } from './liquidation-partage-be-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * F-217 SF-217-03 : champ F-IA-03 audité par l'outil liquidation-partage BE —
 * la date de notification du projet de liquidation est le point de départ du
 * délai de contredits (CJ art. 1218).
 */
export type LiquidationPartageBeAlertField = 'DATE_NOTIFICATION_PROJET';

export type LiquidationPartageBeCoherenceAlert =
  CoherenceAlert<LiquidationPartageBeAlertField>;

const ISO_DATE_REGEX = /^\d{4}-\d{2}-\d{2}$/;

/** Code `critereCode` (F-96 / questions IA) reconnu pour la notification du projet. */
const F96_CODE_NOTIFICATION_PROJET = 'F217_DATE_NOTIFICATION_PROJET';

/**
 * F-217 SF-217-03 : composant Angular standalone pour l'outil décisionnel
 * "Liquidation-partage post-divorce (Belgique)" (`liquidation-partage-be`).
 * BELGIQUE uniquement.
 *
 * Pattern de référence : `regime-communaute-legale-be-section` (SF-217-03) et
 * `procedure-nullite-licenciement-section` (F-DT-36).
 *
 * - Form : avancement des 8 étapes de la procédure de liquidation-partage
 *   (notaire commis — CJ art. 1207 et s.) sous forme de toggles + dates ISO
 *   (`<input type="date">`). Une date n'est demandée que si l'étape associée
 *   est cochée.
 * - Verdict 5 niveaux : PROCEDURE_NON_ENGAGEE (navy) / EN_COURS (navy) /
 *   DELAI_CONTREDITS_CRITIQUE (rouge) / EN_ATTENTE_HOMOLOGATION (or) /
 *   CLOTUREE (navy). Rouge réservé à DELAI_CONTREDITS_CRITIQUE.
 * - Affichage : checklist des étapes (statut + fondement), délais (échéance +
 *   jours restants + statut), prochaine étape, bases juridiques.
 * - Pré-fill IA : V1 = 0 champ (`PREFILL_COUNT_ALWAYS_ZERO`).
 * - Validation F-IA-03 sur le field `DATE_NOTIFICATION_PROJET`.
 * - Refresh dashboard F-IA-02 + MatSnackBar erreurs.
 * - Gate `workspaceCountry === 'BELGIQUE'` — bannière info si mismatch.
 */
@Component({
  selector: 'app-liquidation-partage-be-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatSlideToggleModule, MatProgressSpinnerModule,
    LegalCitationsPipe,
    ToolJurisprudenceCitationsComponent,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './liquidation-partage-be-section.component.html',
  styleUrl: './liquidation-partage-be-section.component.scss',
})
export class LiquidationPartageBeSectionComponent implements OnInit, OnChanges {
  // F-JU-03 SF-JU-03-99f — citations jurisprudentielles F-JU-01 (BE).
  protected readonly toolIdForJurisprudence = 'liquidation-partage-be';
  protected readonly brancheActiveForJurisprudence = 'default';

  static readonly TOOL_LABEL = 'LIQUIDATION-PARTAGE POST-DIVORCE — CJ ART. 1207+ BE';
  static readonly TOOL_ICON = 'gavel';

  /**
   * SF-246-28 / F-236 / F-237 — délègue au helper partagé.
   * 4 dates maximum (désignation notaire, ouverture opérations,
   * notification projet, homologation).
   */
  static getPrefillCount(input: {
    aiData?: unknown;
    procedureChecks?: unknown[];
    aiQuestions?: unknown[];
    piecesManquantes?: unknown[];
    triggerEvents?: unknown[];
    workspaceCountry?: string;
  }): number {
    return LiquidationPartageBeSectionPrefillRules.computePrefillCount({
      aiData: input.aiData,
    });
  }

  @Input() caseFileId!: string;
  /** F-177 — force l'expansion (mode modal). */
  @Input() forceExpanded = false;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'BELGIQUE';
  @Input() aiData?: FamilleExtractedData | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;
  /** Mode simulateur autonome (hors dossier) — coupe F-IA-03 + refresh. */
  @Input() standaloneMode = false;

  private procedureChecksSignal = signal<ProcedureCheck[]>([]);
  private aiQuestionsSignal = signal<AiQuestion[]>([]);
  private piecesManquantesSignal = signal<PieceManquanteEntry[]>([]);

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<LiquidationPartageBeResponse | null>(null);

  // --- Form fields : avancement des 8 étapes ---
  notaireDesigne = signal<boolean>(false);
  dateDesignationNotaire = signal<string | null>(null);
  operationsOuvertes = signal<boolean>(false);
  dateOuvertureOperations = signal<string | null>(null);
  inventaireEtabli = signal<boolean>(false);
  projetLiquidationEtabli = signal<boolean>(false);
  dateNotificationProjet = signal<string | null>(null);
  contreditsDeposes = signal<boolean>(false);
  procesVerbalDiresEtabli = signal<boolean>(false);
  homologationDemandee = signal<boolean>(false);
  dateHomologation = signal<string | null>(null);
  commentaire = signal<string | null>(null);

  /** SF-246-28 — signaux de provenance (null = manuel, 'IA' = pré-rempli). */
  provenanceDateDesignationNotaire = signal<'IA' | null>(null);
  provenanceDateOuvertureOperations = signal<'IA' | null>(null);
  provenanceDateNotificationProjet = signal<'IA' | null>(null);
  provenanceDateHomologation = signal<'IA' | null>(null);

  /**
   * Coherence alerts F-IA-03 — gate `showForm()` strict (alertes masquées
   * après calcul rendu). Aucune source IA en standalone.
   */
  coherenceAlerts = computed<Partial<Record<LiquidationPartageBeAlertField, LiquidationPartageBeCoherenceAlert>>>(() => {
    if (this.standaloneMode) return {};
    if (!this.showForm()) return {};
    const alerts: Partial<Record<LiquidationPartageBeAlertField, LiquidationPartageBeCoherenceAlert>> = {};
    const dateAlert = this.buildNotificationProjetAlert();
    if (dateAlert) alerts.DATE_NOTIFICATION_PROJET = dateAlert;
    return alerts;
  });

  alertsSummary = computed(() => ({ total: Object.values(this.coherenceAlerts()).length }));

  constructor(
    private service: LiquidationPartageBeService,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef,
    @Optional() private dashboardRefresh: CaseDashboardRefreshService | null,
  ) {}

  ngOnInit(): void {
    if (this.forceExpanded) this.collapsed.set(false);
    this.procedureChecksSignal.set(this.procedureChecks ?? []);
    this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    this.piecesManquantesSignal.set(this.piecesManquantes ?? []);
    this.prefillFromAi();
    if (this.workspaceCountry === 'BELGIQUE') {
      this.load();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['forceExpanded'] && this.forceExpanded) this.collapsed.set(false);
    if (changes['procedureChecks']) this.procedureChecksSignal.set(this.procedureChecks ?? []);
    if (changes['aiQuestions']) this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    if (changes['piecesManquantes']) this.piecesManquantesSignal.set(this.piecesManquantes ?? []);
    if (changes['aiData'] && !changes['aiData'].firstChange
        && this.showForm() && !this.result()) {
      this.prefillFromAi();
    }
  }

  /**
   * SF-246-28 — Pré-fill IA réel pour les 4 dates de la procédure
   * liquidation-partage BE. Si une date est pré-remplie, l'étape associée
   * est également cochée. No-op gracieux si `aiData` est null.
   */
  private prefillFromAi(): void {
    const rules = LiquidationPartageBeSectionPrefillRules;
    const input = { aiData: this.aiData };
    let changed = false;

    const dNotaire = rules.computeDateDesignationNotaire(input);
    if (dNotaire != null) {
      this.dateDesignationNotaire.set(dNotaire);
      this.notaireDesigne.set(true);
      this.provenanceDateDesignationNotaire.set('IA');
      changed = true;
    }

    const dOuverture = rules.computeDateOuvertureOperations(input);
    if (dOuverture != null) {
      this.dateOuvertureOperations.set(dOuverture);
      this.operationsOuvertes.set(true);
      this.provenanceDateOuvertureOperations.set('IA');
      changed = true;
    }

    const dNotif = rules.computeDateNotificationProjet(input);
    if (dNotif != null) {
      this.dateNotificationProjet.set(dNotif);
      this.projetLiquidationEtabli.set(true);
      this.provenanceDateNotificationProjet.set('IA');
      changed = true;
    }

    const dHomolog = rules.computeDateHomologation(input);
    if (dHomolog != null) {
      this.dateHomologation.set(dHomolog);
      this.homologationDemandee.set(true);
      this.provenanceDateHomologation.set('IA');
      changed = true;
    }

    if (changed) this.cdr.markForCheck();
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // --- Handlers d'étapes : décocher une étape efface sa date associée ---

  onNotaireChange(value: boolean): void {
    this.notaireDesigne.set(value);
    if (!value) { this.dateDesignationNotaire.set(null); this.provenanceDateDesignationNotaire.set(null); }
  }

  onOperationsChange(value: boolean): void {
    this.operationsOuvertes.set(value);
    if (!value) { this.dateOuvertureOperations.set(null); this.provenanceDateOuvertureOperations.set(null); }
  }

  onProjetLiquidationChange(value: boolean): void {
    this.projetLiquidationEtabli.set(value);
    if (!value) { this.dateNotificationProjet.set(null); this.provenanceDateNotificationProjet.set(null); }
  }

  onHomologationChange(value: boolean): void {
    this.homologationDemandee.set(value);
    if (!value) { this.dateHomologation.set(null); this.provenanceDateHomologation.set(null); }
  }

  /** SF-246-28 — Reset provenance date au changement manuel par l'avocat. */
  onDateChange(field: 'notaire' | 'ouverture' | 'notification' | 'homologation'): void {
    switch (field) {
      case 'notaire': this.provenanceDateDesignationNotaire.set(null); break;
      case 'ouverture': this.provenanceDateOuvertureOperations.set(null); break;
      case 'notification': this.provenanceDateNotificationProjet.set(null); break;
      case 'homologation': this.provenanceDateHomologation.set(null); break;
    }
  }

  /**
   * Form valide si workspace BE et aucune date saisie n'est mal formée
   * (la cohérence fine — date requise si étape cochée — est contrôlée par le
   * backend qui renvoie un 400 explicite).
   */
  formValid(): boolean {
    if (this.workspaceCountry !== 'BELGIQUE') return false;
    return this.datesWellFormed();
  }

  private datesWellFormed(): boolean {
    const dates = [
      this.dateDesignationNotaire(),
      this.dateOuvertureOperations(),
      this.dateNotificationProjet(),
      this.dateHomologation(),
    ];
    return dates.every(d => d === null || ISO_DATE_REGEX.test(d));
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request: LiquidationPartageBeRequest = {
      notaireDesigne: this.notaireDesigne(),
      dateDesignationNotaire: this.dateDesignationNotaire(),
      operationsOuvertes: this.operationsOuvertes(),
      dateOuvertureOperations: this.dateOuvertureOperations(),
      inventaireEtabli: this.inventaireEtabli(),
      projetLiquidationEtabli: this.projetLiquidationEtabli(),
      dateNotificationProjet: this.dateNotificationProjet(),
      contreditsDeposes: this.contreditsDeposes(),
      procesVerbalDiresEtabli: this.procesVerbalDiresEtabli(),
      homologationDemandee: this.homologationDemandee(),
      dateHomologation: this.dateHomologation(),
      commentaire: this.commentaire()?.trim() || null,
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.applyResult(r);
        this.calculating.set(false);
        this.snackBar.open('Suivi de liquidation-partage calculé', 'OK', { duration: 2500 });
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

  private applyResult(r: LiquidationPartageBeResponse): void {
    this.result.set(r);
    this.hydrateForm(r);
    this.showForm.set(false);
  }

  /**
   * Ré-injecte le snapshot d'inputs de la réponse dans les champs du
   * formulaire — sans quoi un clic « Modifier » repartirait des valeurs par
   * défaut au lieu des dernières valeurs saisies (leçon F-DT-36).
   */
  private hydrateForm(r: LiquidationPartageBeResponse): void {
    this.notaireDesigne.set(r.notaireDesigne ?? false);
    this.dateDesignationNotaire.set(r.dateDesignationNotaire ?? null);
    this.operationsOuvertes.set(r.operationsOuvertes ?? false);
    this.dateOuvertureOperations.set(r.dateOuvertureOperations ?? null);
    this.inventaireEtabli.set(r.inventaireEtabli ?? false);
    this.projetLiquidationEtabli.set(r.projetLiquidationEtabli ?? false);
    this.dateNotificationProjet.set(r.dateNotificationProjet ?? null);
    this.contreditsDeposes.set(r.contreditsDeposes ?? false);
    this.procesVerbalDiresEtabli.set(r.procesVerbalDiresEtabli ?? false);
    this.homologationDemandee.set(r.homologationDemandee ?? false);
    this.dateHomologation.set(r.dateHomologation ?? null);
    this.commentaire.set(r.commentaire ?? null);
  }

  // ---------------------------------------------------------------------------
  // Builder d'alerte F-IA-03
  // ---------------------------------------------------------------------------

  /** Date de notification du projet — divergence stricte F-96 / question IA / pièce. */
  private buildNotificationProjetAlert(): LiquidationPartageBeCoherenceAlert | null {
    const user = this.dateNotificationProjet();
    if (!user) return null;
    const builder = CoherenceAlertBuilder.forField<LiquidationPartageBeAlertField>('DATE_NOTIFICATION_PROJET');
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== F96_CODE_NOTIFICATION_PROJET) continue;
      const ev = chk.expectedValue?.trim();
      if (!ev || ev === user) continue;
      builder.addSource('F96', {
        expectedDisplay: ev,
        reason: `Checklist procédurale : notification du projet attendue le ${ev}${chk.raison ? ' (' + chk.raison + ')' : ''}`,
      });
      break;
    }
    for (const q of this.aiQuestionsSignal()) {
      if (q.critereCode?.toUpperCase() !== F96_CODE_NOTIFICATION_PROJET) continue;
      const ev = q.expectedValue?.trim();
      if (!ev || ev === user) continue;
      builder.addSource('QUESTION_IA', {
        expectedDisplay: ev,
        reason: `Question complémentaire : "${q.questionText}" → "${q.answerText}"`,
      });
      break;
    }
    const piece = this.findPieceManquante(['F217_DATE_NOTIFICATION_PROJET', 'PROJET_LIQUIDATION', 'PROCES_VERBAL_DIRES']);
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

  alertTooltip(alert: LiquidationPartageBeCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: LiquidationPartageBeCoherenceAlert): string {
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

  /** Rouge réservé à DELAI_CONTREDITS_CRITIQUE ; or pour homologation ; navy sinon. */
  verdictBannerClass(verdict: LiquidationPartageBeVerdict): string {
    switch (verdict) {
      case 'DELAI_CONTREDITS_CRITIQUE':
        return 'lpb-verdict-banner lpb-verdict-banner--danger';
      case 'EN_ATTENTE_HOMOLOGATION':
        return 'lpb-verdict-banner lpb-verdict-banner--medium';
      case 'PROCEDURE_NON_ENGAGEE':
      case 'EN_COURS':
      case 'CLOTUREE':
        return 'lpb-verdict-banner lpb-verdict-banner--available';
    }
  }

  verdictBannerLabel(verdict: LiquidationPartageBeVerdict): string {
    switch (verdict) {
      case 'PROCEDURE_NON_ENGAGEE': return 'Procédure non engagée';
      case 'EN_COURS': return 'Procédure en cours';
      case 'DELAI_CONTREDITS_CRITIQUE': return 'Délai de contredits critique';
      case 'EN_ATTENTE_HOMOLOGATION': return 'En attente d\'homologation';
      case 'CLOTUREE': return 'Liquidation-partage clôturée';
    }
  }

  verdictBannerIcon(verdict: LiquidationPartageBeVerdict): string {
    switch (verdict) {
      case 'PROCEDURE_NON_ENGAGEE': return 'schedule';
      case 'EN_COURS': return 'autorenew';
      case 'DELAI_CONTREDITS_CRITIQUE': return 'error';
      case 'EN_ATTENTE_HOMOLOGATION': return 'hourglass_top';
      case 'CLOTUREE': return 'check_circle';
    }
  }

  /** Classe CSS du pictogramme de statut d'une étape. */
  etapeStatutClass(statut: EtapeStatutBe): string {
    switch (statut) {
      case 'FAITE': return 'lpb-etape-statut lpb-etape-statut--faite';
      case 'EN_COURS': return 'lpb-etape-statut lpb-etape-statut--encours';
      case 'A_VENIR': return 'lpb-etape-statut lpb-etape-statut--avenir';
    }
  }

  etapeStatutIcon(statut: EtapeStatutBe): string {
    switch (statut) {
      case 'FAITE': return 'check_circle';
      case 'EN_COURS': return 'radio_button_checked';
      case 'A_VENIR': return 'radio_button_unchecked';
    }
  }

  etapeStatutLabel(statut: EtapeStatutBe): string {
    switch (statut) {
      case 'FAITE': return 'Faite';
      case 'EN_COURS': return 'En cours';
      case 'A_VENIR': return 'À venir';
    }
  }

  /** Classe CSS du chip de statut d'un délai. Rouge réservé à CRITIQUE / DEPASSE. */
  delaiStatutClass(statut: DelaiStatutBe): string {
    switch (statut) {
      case 'OK': return 'lpb-delai-chip lpb-delai-chip--ok';
      case 'CRITIQUE': return 'lpb-delai-chip lpb-delai-chip--danger';
      case 'DEPASSE': return 'lpb-delai-chip lpb-delai-chip--danger';
      case 'NON_DEMARRE': return 'lpb-delai-chip lpb-delai-chip--neutral';
    }
  }

  delaiStatutLabel(statut: DelaiStatutBe): string {
    switch (statut) {
      case 'OK': return 'Dans les délais';
      case 'CRITIQUE': return 'Échéance imminente';
      case 'DEPASSE': return 'Délai dépassé';
      case 'NON_DEMARRE': return 'Non démarré';
    }
  }
}
