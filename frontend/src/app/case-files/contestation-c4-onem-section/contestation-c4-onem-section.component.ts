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
  inject,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatRadioModule } from '@angular/material/radio';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
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
import { ContestationC4OnemService } from '../../core/services/contestation-c4-onem.service';
import {
  ContestationC4OnemEtapeSuivante,
  ContestationC4OnemPalier,
  ContestationC4OnemPalierType,
  ContestationC4OnemRequest,
  ContestationC4OnemResponse,
  ContestationC4OnemVerdict,
  humanizeVerdict,
  palierLabel,
} from '../../core/models/contestation-c4-onem.model';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { ContestationC4OnemPrefillRules } from './contestation-c4-onem-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-207-03b — Champs d'alerte de cohérence F-IA-03 exposés par l'outil
 * F-207 (contestation C4 ONEM Travail BE). 3 champs pré-remplissables ;
 * une divergence avocat/IA sur ces 3 champs porte un risque procédural
 * (forclusion).
 */
export type ContestationC4OnemAlertField =
  | 'DATE_NOTIFICATION'
  | 'DATE_DIRECTEUR'
  | 'RECOURS_ADMIN';

export type ContestationC4OnemCoherenceAlert = CoherenceAlert<ContestationC4OnemAlertField>;

/**
 * Seuil d'écart en jours absolu au-delà duquel on affiche une alerte de
 * cohérence entre la date IA et la saisie avocat (aligné canonique
 * F-DT-25 / SF-207-01b / SF-207-02b).
 */
const DATE_DIVERGENCE_DAYS = 15;

/**
 * SF-207-03b : outil décisionnel "Contestation C4 ONEM" (F-207). BE uniquement
 * (AR 25/11/1991 art. 144 + Code judiciaire art. 580 2° + Loi 03/07/1978).
 * Consomme l'API SF-207-03 (PR #1137).
 *
 * Affiché conditionnellement par le panel F-IA-04 (tool_id
 * `contestation-c4-onem`, règle ALWAYS_ON BE + travail).
 *
 * Pattern canonique : `c4-onem-checklist-section` (SF-207-02b) dupliqué
 * et adapté au double palier ADMIN/TRIBUNAL + toggle Cas A/B.
 */
@Component({
  selector: 'app-contestation-c4-onem-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatRadioModule,
    MatProgressSpinnerModule,
    CoherencePopoverTriggerDirective,    ToolJurisprudenceCitationsComponent,
  ],
  templateUrl: './contestation-c4-onem-section.component.html',
  styleUrl: './contestation-c4-onem-section.component.scss',
})
export class ContestationC4OnemSectionComponent implements OnInit, OnChanges {
  // F-JU-03 SF-JU-03-99f — citations jurisprudentielles F-JU-01 (BE).
  protected readonly toolIdForJurisprudence = 'contestation-c4-onem';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'CONTESTATION C4 ONEM (BE)';
  static readonly TOOL_ICON = 'gavel';

  /** F-177 SF-177-12 / F-236 SF-236-02 — délègue au helper partagé (parité runtime). */
  static getPrefillCount(input: PrefillCountInput): number {
    return ContestationC4OnemPrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() forceExpanded = false;
  /** Gate strict BE — l'outil n'a pas d'équivalent FR. */
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'BELGIQUE';
  @Input() aiData?: TravailExtractedData | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;

  // Exposé au template.
  readonly humanizeVerdict = humanizeVerdict;
  readonly palierLabel = palierLabel;

  private cdr = inject(ChangeDetectorRef);

  // Snapshots signal des inputs IA pour réactivité computed.
  private aiDataSignal = signal<TravailExtractedData | null | undefined>(undefined);
  private procedureChecksSignal = signal<ProcedureCheck[]>([]);
  private aiQuestionsSignal = signal<AiQuestion[]>([]);
  private piecesManquantesSignal = signal<PieceManquanteEntry[]>([]);

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<ContestationC4OnemResponse | null>(null);

  // Champs du form (signals).
  dateNotificationDecisionOnem = signal<string | null>(null);
  dateActionEnvisagee = signal<string | null>(null);
  recoursAdminDejaForme = signal<boolean>(false);
  dateDecisionDirecteur = signal<string | null>(null);

  // Provenance IA par champ pré-rempli.
  provenanceDateNotification = signal<'IA' | null>(null);
  provenanceRecoursAdmin = signal<'IA' | null>(null);
  provenanceDateDirecteur = signal<'IA' | null>(null);

  // SF-IA-03-15 : map {sourceKey → explanations} (popover enrichi).
  sourceExplanations = signal<Map<string, SourceExplanation[]>>(new Map());

  /** Gate strict workspace BE (l'outil n'existe pas côté FR). */
  isAvailable = computed(() => this.workspaceCountry === 'BELGIQUE');

  /**
   * Alertes de cohérence calculées dynamiquement (3 fields).
   * Gate : uniquement en mode formulaire (pattern anti-bug SF-IA-03-12).
   */
  coherenceAlerts = computed<Partial<Record<ContestationC4OnemAlertField, ContestationC4OnemCoherenceAlert>>>(() => {
    if (!this.showForm()) return {};
    const alerts: Partial<Record<ContestationC4OnemAlertField, ContestationC4OnemCoherenceAlert>> = {};
    const dNotif = this.buildDateAlert('DATE_NOTIFICATION');
    if (dNotif) alerts.DATE_NOTIFICATION = dNotif;
    const dDir = this.buildDateAlert('DATE_DIRECTEUR');
    if (dDir) alerts.DATE_DIRECTEUR = dDir;
    const rec = this.buildRecoursAdminAlert();
    if (rec) alerts.RECOURS_ADMIN = rec;
    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    return { total: values.length, blockers: 0 };
  });

  constructor(
    private service: ContestationC4OnemService,
    private snackBar: MatSnackBar,
    @Optional() private dashboardRefresh: CaseDashboardRefreshService | null,
    @Optional() private sourceExplanationService: SourceExplanationService | null,
  ) {}

  ngOnInit(): void {
    if (this.forceExpanded) this.collapsed.set(false);

    this.aiDataSignal.set(this.aiData);
    this.procedureChecksSignal.set(this.procedureChecks ?? []);
    this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    this.piecesManquantesSignal.set(this.piecesManquantes ?? []);

    if (this.workspaceCountry === 'BELGIQUE') {
      this.load();
      this.loadSourceExplanations();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['forceExpanded'] && this.forceExpanded) this.collapsed.set(false);

    if (changes['aiData']) this.aiDataSignal.set(this.aiData);
    if (changes['procedureChecks']) this.procedureChecksSignal.set(this.procedureChecks ?? []);
    if (changes['aiQuestions']) this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    if (changes['piecesManquantes']) this.piecesManquantesSignal.set(this.piecesManquantes ?? []);

    // Ré-applique le pré-fill si aiData change avant première résolution.
    if (changes['aiData'] && !changes['aiData'].firstChange
        && this.showForm() && !this.result()
        && this.workspaceCountry === 'BELGIQUE') {
      this.prefillFromAi();
    }
  }

  /**
   * SF-207-03b — Pré-remplit le form depuis `aiData`. Délègue intégralement
   * au helper pur partagé pour garantir la parité runtime / static
   * `getPrefillCount` (divergence impossible par construction).
   */
  private prefillFromAi(): void {
    const ai = this.aiDataSignal();
    if (!ai) return;
    if (this.workspaceCountry !== 'BELGIQUE') return;

    const ruleInput: PrefillCountInput = { aiData: ai, workspaceCountry: this.workspaceCountry };

    // 1. dateNotificationDecisionOnem
    const dNotif = ContestationC4OnemPrefillRules.computeDateNotificationDecisionOnem(ruleInput);
    if (dNotif !== null && (this.dateNotificationDecisionOnem() === null || this.provenanceDateNotification() === 'IA')) {
      this.dateNotificationDecisionOnem.set(dNotif);
      this.provenanceDateNotification.set('IA');
    }

    // 2. recoursAdminDejaForme (true OR false signal)
    const rec = ContestationC4OnemPrefillRules.computeRecoursAdminDejaForme(ruleInput);
    if (rec !== null && (this.provenanceRecoursAdmin() === null || this.provenanceRecoursAdmin() === 'IA')) {
      this.recoursAdminDejaForme.set(rec);
      this.provenanceRecoursAdmin.set('IA');
    }

    // 3. dateDecisionDirecteur
    const dDir = ContestationC4OnemPrefillRules.computeDateDecisionDirecteur(ruleInput);
    if (dDir !== null && (this.dateDecisionDirecteur() === null || this.provenanceDateDirecteur() === 'IA')) {
      this.dateDecisionDirecteur.set(dDir);
      this.provenanceDateDirecteur.set('IA');
    }
  }

  private loadSourceExplanations(): void {
    if (!this.caseFileId || !this.sourceExplanationService) return;
    this.sourceExplanationService.getForCaseFile(this.caseFileId).subscribe({
      next: (map) => { this.sourceExplanations.set(map); this.cdr.markForCheck(); },
      error: () => { /* fail-open */ },
    });
  }

  /** SF-IA-03-15 : mapping field → sourceKey pour le popover enrichi. */
  explanationFor(field: ContestationC4OnemAlertField): SourceExplanation[] {
    const key = (() => {
      switch (field) {
        case 'DATE_NOTIFICATION': return 'date_notification_decision_onem';
        case 'DATE_DIRECTEUR': return 'date_decision_directeur';
        case 'RECOURS_ADMIN': return 'recours_admin_deja_forme';
      }
    })();
    return this.sourceExplanations().get(key) ?? [];
  }

  alertTooltip(alert: ContestationC4OnemCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: ContestationC4OnemCoherenceAlert): string {
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
   * Divergence date (notification / décision directeur) — écart absolu
   * ≥ 15 jours entre IA et saisie. Source IA :
   *   - DATE_NOTIFICATION ← `aiData.dateNotificationDecisionOnem`
   *   - DATE_DIRECTEUR ← `aiData.dateDecisionDirecteur`
   */
  private buildDateAlert(field: 'DATE_NOTIFICATION' | 'DATE_DIRECTEUR'): ContestationC4OnemCoherenceAlert | null {
    const ai = this.aiDataSignal();
    if (!ai) return null;
    const aiDate = field === 'DATE_NOTIFICATION'
      ? ai.dateNotificationDecisionOnem
      : ai.dateDecisionDirecteur;
    const userDate = field === 'DATE_NOTIFICATION'
      ? this.dateNotificationDecisionOnem()
      : this.dateDecisionDirecteur();
    if (!aiDate || !userDate) return null;
    const diff = dateDaysDiff(aiDate, userDate);
    if (diff === null || diff < DATE_DIVERGENCE_DAYS) return null;
    const label = field === 'DATE_NOTIFICATION'
      ? 'date de notification de la décision ONEM'
      : 'date de notification de la décision du Directeur';
    return CoherenceAlertBuilder.forField<ContestationC4OnemAlertField>(field)
      .addSource('IA', {
        expectedDisplay: aiDate,
        reason: `Analyse du dossier : ${label} ${aiDate}`,
      })
      .build();
  }

  /**
   * Divergence "recours admin déjà formé" — l'IA détecte un état (true/false)
   * mais l'avocat saisit l'opposé.
   */
  private buildRecoursAdminAlert(): ContestationC4OnemCoherenceAlert | null {
    const ai = this.aiDataSignal();
    if (!ai) return null;
    const iaRec = ContestationC4OnemPrefillRules.computeRecoursAdminDejaForme({ aiData: ai });
    if (iaRec === null) return null;
    const userRec = this.recoursAdminDejaForme();
    if (iaRec === userRec) return null;
    return CoherenceAlertBuilder.forField<ContestationC4OnemAlertField>('RECOURS_ADMIN')
      .addSource('IA', {
        expectedDisplay: iaRec ? 'oui (recours déjà formé)' : 'non (recours non encore formé)',
        reason: `Analyse du dossier : recours administratif ${iaRec ? 'déjà' : 'non encore'} formé`,
      })
      .build();
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  formValid(): boolean {
    const dn = this.dateNotificationDecisionOnem();
    if (!dn) return false;
    if (this.recoursAdminDejaForme()) {
      const dd = this.dateDecisionDirecteur();
      if (!dd) return false;
      // Cohérence min : décision directeur ≥ notification ONEM (Le calculateur re-vérifie).
      if (dd < dn) return false;
    }
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // -- Handlers --
  onDateNotificationChange(value: string | null): void {
    this.dateNotificationDecisionOnem.set(value);
    this.provenanceDateNotification.set(null);
  }
  onDateActionChange(value: string | null): void {
    this.dateActionEnvisagee.set(value);
  }
  onRecoursAdminChange(value: boolean): void {
    this.recoursAdminDejaForme.set(value);
    this.provenanceRecoursAdmin.set(null);
    // Si on bascule Cas B → A, on efface dateDecisionDirecteur (non pertinent).
    if (!value) {
      this.dateDecisionDirecteur.set(null);
      this.provenanceDateDirecteur.set(null);
    }
  }
  onDateDirecteurChange(value: string | null): void {
    this.dateDecisionDirecteur.set(value);
    this.provenanceDateDirecteur.set(null);
  }

  /** Classe CSS du verdict (vert/ambre/rouge). */
  verdictClass(verdict: ContestationC4OnemVerdict): string {
    if (verdict.endsWith('_OUVERT')) return 'verdict-ok';
    if (verdict.endsWith('_IMMINENT')) return 'verdict-warn';
    return 'verdict-critical'; // _PRESCRIT
  }

  verdictIcon(verdict: ContestationC4OnemVerdict): string {
    if (verdict.endsWith('_OUVERT')) return 'check_circle';
    if (verdict.endsWith('_IMMINENT')) return 'schedule';
    return 'error'; // _PRESCRIT
  }

  verdictLabel(verdict: ContestationC4OnemVerdict): string {
    return humanizeVerdict(verdict);
  }

  /** Vrai si on affiche l'alerte rouge « Forclusion totale » sous le verdict. */
  isForclusionTotale(): boolean {
    return this.result()?.etapeSuivante === 'FORCLUSION_TOTALE';
  }

  /**
   * Vrai si on affiche l'alerte ambre « saut palier admin possible »
   * (Cas A — admin prescrit mais tribunal pas encore entamé : CJ 580 2°).
   */
  isSautPalierAdmin(): boolean {
    return this.result()?.verdict === 'RECOURS_ADMIN_PRESCRIT';
  }

  /** Classe CSS pour la cellule joursRestants (≤ 0 rouge, ≤ seuil ambre). */
  joursRestantsClass(palier: ContestationC4OnemPalier): string {
    const j = palier.joursRestants;
    if (j === null || j === undefined) return '';
    if (j <= 0) return 'jours-critical';
    const seuil = palier.type === 'ADMIN' ? 7 : 14;
    if (j <= seuil) return 'jours-warning';
    return 'jours-ok';
  }

  /** Libellé court FR du palier pour le tableau. */
  palierShortLabel(type: ContestationC4OnemPalierType): string {
    return type === 'ADMIN' ? 'Recours administratif (Directeur ONEM)' : 'Recours tribunal du travail';
  }

  /** Libellé orientation procédurale FR. */
  etapeSuivanteLabel(etape: ContestationC4OnemEtapeSuivante): string {
    switch (etape) {
      case 'RECOURS_ADMIN_DIRECTEUR': return 'Rédiger et envoyer le recours administratif au Directeur';
      case 'RECOURS_TRIBUNAL_TRAVAIL': return 'Saisir le tribunal du travail (requête contradictoire)';
      case 'FORCLUSION_TOTALE': return 'Forclusion totale — étudier les exceptions';
      case 'AUCUNE': return 'Aucune action recommandée';
    }
  }

  calculate(): void {
    if (!this.formValid()) return;
    if (this.workspaceCountry !== 'BELGIQUE') {
      this.snackBar.open('Outil indisponible pour ce workspace', 'Fermer',
        { duration: 4000, panelClass: 'snack-error' });
      return;
    }
    const request: ContestationC4OnemRequest = {
      dateNotificationDecisionOnem: this.dateNotificationDecisionOnem()!,
      dateActionEnvisagee: this.dateActionEnvisagee() || null,
      recoursAdminDejaForme: this.recoursAdminDejaForme(),
      dateDecisionDirecteur: this.recoursAdminDejaForme()
        ? (this.dateDecisionDirecteur() || null)
        : null,
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        // Refresh dashboard décisionnel (pattern SF-IA-02-03).
        this.dashboardRefresh?.triggerRefresh();
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.calculating.set(false);
        const status = err?.status;
        let msg: string;
        if (status === 404) {
          msg = 'Dossier introuvable';
        } else if (status === 400) {
          msg = err?.error?.message || err?.error || 'Données saisies invalides';
        } else {
          msg = 'Une erreur est survenue. Veuillez réessayer.';
        }
        this.snackBar.open(String(msg), 'Fermer',
          { duration: 5000, panelClass: 'snack-error' });
        this.cdr.markForCheck();
      },
    });
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.dateNotificationDecisionOnem.set(r.dateNotificationDecisionOnem);
        this.dateActionEnvisagee.set(r.dateActionEnvisagee);
        this.recoursAdminDejaForme.set(r.recoursAdminDejaForme);
        this.dateDecisionDirecteur.set(r.dateDecisionDirecteur);
        // Valeurs persistées = saisie avocat — jamais de badge IA.
        this.provenanceDateNotification.set(null);
        this.provenanceRecoursAdmin.set(null);
        this.provenanceDateDirecteur.set(null);
        this.showForm.set(false);
        this.loading.set(false);
        this.cdr.markForCheck();
      },
      error: () => {
        // 404 attendu si aucune analyse — mode formulaire + pré-fill IA.
        this.prefillFromAi();
        this.loading.set(false);
        this.cdr.markForCheck();
      },
    });
  }
}

/**
 * Utilitaire : différence absolue en jours entre 2 dates ISO. Renvoie null
 * si l'une des dates n'est pas parsable.
 */
function dateDaysDiff(a: string, b: string): number | null {
  const ta = Date.parse(a);
  const tb = Date.parse(b);
  if (Number.isNaN(ta) || Number.isNaN(tb)) return null;
  return Math.abs(ta - tb) / 86400000;
}
