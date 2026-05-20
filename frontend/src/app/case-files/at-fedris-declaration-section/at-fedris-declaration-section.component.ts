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
import { MatCheckboxModule } from '@angular/material/checkbox';
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
} from '../../shared/coherence-popover/coherence-alert.model';
import { CoherenceAlertBuilder } from '../../shared/coherence-popover/coherence-alert-builder';
import { SourceExplanation } from '../../core/models/source-explanation.model';
import { SourceExplanationService } from '../../core/services/source-explanation.service';
import { AtFedrisDeclarationService } from '../../core/services/at-fedris-declaration.service';
import {
  AtFedrisDeclarationRequest,
  AtFedrisDeclarationResponse,
  AtFedrisDeclarationVerdict,
  humanizeVerdict,
} from '../../core/models/at-fedris-declaration.model';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { AtFedrisDeclarationPrefillRules } from './at-fedris-declaration-section-prefill-rules';

/**
 * SF-207-04b — Champs d'alerte de cohérence F-IA-03 exposés par l'outil
 * F-207 (déclaration AT Fedris Travail BE). 2 champs pré-remplissables ;
 * une divergence avocat/IA sur ces 2 champs porte un risque procédural
 * (délai de 8 jours dépassé / mauvaise date de connaissance employeur).
 */
export type AtFedrisDeclarationAlertField =
  | 'DATE_ACCIDENT'
  | 'DATE_CONNAISSANCE_EMPLOYEUR';

export type AtFedrisDeclarationCoherenceAlert =
  CoherenceAlert<AtFedrisDeclarationAlertField>;

/**
 * Seuil d'écart en jours absolu au-delà duquel on affiche une alerte de
 * cohérence entre la date IA et la saisie avocat (aligné canonique
 * F-DT-25 / SF-207-01b / SF-207-02b / SF-207-03b).
 */
const DATE_DIVERGENCE_DAYS = 15;

/**
 * SF-207-04b : outil décisionnel "Déclaration AT Fedris" (F-207). BE uniquement
 * (Loi 10/04/1971 art. 62 + AR 21/12/1971 art. 25 — délai 8 jours
 * calendaires à compter de la connaissance de l'accident par l'employeur).
 * Consomme l'API SF-207-04 (PR #1142).
 *
 * Affiché conditionnellement par le panel F-IA-04 (tool_id
 * `at-fedris-declaration`, règle ALWAYS_ON BE + travail).
 *
 * Pattern canonique : `contestation-c4-onem-section` (SF-207-03b) dupliqué
 * et adapté aux 5 verdicts (3 prospectifs + 2 rétrospectifs) + checkbox
 * mode rétrospectif (au lieu du toggle Cas A/B radio).
 */
@Component({
  selector: 'app-at-fedris-declaration-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatCheckboxModule,
    MatProgressSpinnerModule,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './at-fedris-declaration-section.component.html',
  styleUrl: './at-fedris-declaration-section.component.scss',
})
export class AtFedrisDeclarationSectionComponent implements OnInit, OnChanges {
  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'DÉCLARATION AT FEDRIS (BE)';
  static readonly TOOL_ICON = 'event_busy';

  /** F-177 SF-177-12 / F-236 SF-236-02 — délègue au helper partagé (parité runtime). */
  static getPrefillCount(input: PrefillCountInput): number {
    return AtFedrisDeclarationPrefillRules.computePrefillCount(input);
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
  result = signal<AtFedrisDeclarationResponse | null>(null);

  // Champs du form (signals).
  dateAccident = signal<string | null>(null);
  dateConnaissanceEmployeur = signal<string | null>(null);
  dateActionEnvisagee = signal<string | null>(null);
  declarationDejaEffectuee = signal<boolean>(false);
  dateDeclarationEffectuee = signal<string | null>(null);

  // Provenance IA par champ pré-rempli.
  provenanceDateAccident = signal<'IA' | null>(null);
  provenanceDateConnaissance = signal<'IA' | null>(null);

  // SF-IA-03-15 : map {sourceKey → explanations} (popover enrichi).
  sourceExplanations = signal<Map<string, SourceExplanation[]>>(new Map());

  /** Gate strict workspace BE (l'outil n'existe pas côté FR). */
  isAvailable = computed(() => this.workspaceCountry === 'BELGIQUE');

  /**
   * Alertes de cohérence calculées dynamiquement (2 fields).
   * Gate : uniquement en mode formulaire (pattern anti-bug SF-IA-03-12).
   */
  coherenceAlerts = computed<Partial<Record<AtFedrisDeclarationAlertField, AtFedrisDeclarationCoherenceAlert>>>(() => {
    if (!this.showForm()) return {};
    const alerts: Partial<Record<AtFedrisDeclarationAlertField, AtFedrisDeclarationCoherenceAlert>> = {};
    const dAcc = this.buildDateAlert('DATE_ACCIDENT');
    if (dAcc) alerts.DATE_ACCIDENT = dAcc;
    const dConn = this.buildDateAlert('DATE_CONNAISSANCE_EMPLOYEUR');
    if (dConn) alerts.DATE_CONNAISSANCE_EMPLOYEUR = dConn;
    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    return { total: values.length, blockers: 0 };
  });

  constructor(
    private service: AtFedrisDeclarationService,
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
   * SF-207-04b — Pré-remplit le form depuis `aiData`. Délègue intégralement
   * au helper pur partagé pour garantir la parité runtime / static
   * `getPrefillCount` (divergence impossible par construction).
   */
  private prefillFromAi(): void {
    const ai = this.aiDataSignal();
    if (!ai) return;
    if (this.workspaceCountry !== 'BELGIQUE') return;

    const ruleInput: PrefillCountInput = { aiData: ai, workspaceCountry: this.workspaceCountry };

    // 1. dateAccident
    const dAcc = AtFedrisDeclarationPrefillRules.computeDateAccident(ruleInput);
    if (dAcc !== null && (this.dateAccident() === null || this.provenanceDateAccident() === 'IA')) {
      this.dateAccident.set(dAcc);
      this.provenanceDateAccident.set('IA');
    }

    // 2. dateConnaissanceEmployeur
    const dConn = AtFedrisDeclarationPrefillRules.computeDateConnaissanceEmployeur(ruleInput);
    if (dConn !== null && (this.dateConnaissanceEmployeur() === null || this.provenanceDateConnaissance() === 'IA')) {
      this.dateConnaissanceEmployeur.set(dConn);
      this.provenanceDateConnaissance.set('IA');
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
  explanationFor(field: AtFedrisDeclarationAlertField): SourceExplanation[] {
    const key = (() => {
      switch (field) {
        case 'DATE_ACCIDENT': return 'date_accident';
        case 'DATE_CONNAISSANCE_EMPLOYEUR': return 'date_connaissance_accident_employeur';
      }
    })();
    return this.sourceExplanations().get(key) ?? [];
  }

  alertTooltip(alert: AtFedrisDeclarationCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: AtFedrisDeclarationCoherenceAlert): string {
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
   * Divergence date — écart absolu ≥ 15 jours entre IA et saisie.
   * Source IA :
   *   - DATE_ACCIDENT ← `aiData.dateAccident`
   *   - DATE_CONNAISSANCE_EMPLOYEUR ← `aiData.dateConnaissanceAccidentEmployeur`
   */
  private buildDateAlert(field: AtFedrisDeclarationAlertField): AtFedrisDeclarationCoherenceAlert | null {
    const ai = this.aiDataSignal();
    if (!ai) return null;
    const aiDate = field === 'DATE_ACCIDENT'
      ? (ai as any).dateAccident
      : (ai as any).dateConnaissanceAccidentEmployeur;
    const userDate = field === 'DATE_ACCIDENT'
      ? this.dateAccident()
      : this.dateConnaissanceEmployeur();
    if (!aiDate || !userDate) return null;
    const diff = dateDaysDiff(aiDate, userDate);
    if (diff === null || diff < DATE_DIVERGENCE_DAYS) return null;
    const label = field === 'DATE_ACCIDENT'
      ? 'date de survenance de l\'accident'
      : 'date de connaissance de l\'accident par l\'employeur';
    return CoherenceAlertBuilder.forField<AtFedrisDeclarationAlertField>(field)
      .addSource('IA', {
        expectedDisplay: aiDate,
        reason: `Analyse du dossier : ${label} ${aiDate}`,
      })
      .build();
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  formValid(): boolean {
    const dAcc = this.dateAccident();
    if (!dAcc) return false;
    if (this.declarationDejaEffectuee()) {
      const dDecl = this.dateDeclarationEffectuee();
      if (!dDecl) return false;
    }
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // -- Handlers --
  onDateAccidentChange(value: string | null): void {
    this.dateAccident.set(value);
    this.provenanceDateAccident.set(null);
  }
  onDateConnaissanceChange(value: string | null): void {
    this.dateConnaissanceEmployeur.set(value);
    this.provenanceDateConnaissance.set(null);
  }
  onDateActionChange(value: string | null): void {
    this.dateActionEnvisagee.set(value);
  }
  onDeclarationDejaEffectueeChange(value: boolean): void {
    this.declarationDejaEffectuee.set(value);
    // Si on décoche, on efface la date de déclaration effective (non pertinente).
    if (!value) {
      this.dateDeclarationEffectuee.set(null);
    }
  }
  onDateDeclarationEffectueeChange(value: string | null): void {
    this.dateDeclarationEffectuee.set(value);
  }

  /** Classe CSS du verdict (vert / ambre / rouge). */
  verdictClass(verdict: AtFedrisDeclarationVerdict): string {
    if (verdict === 'DELAI_OUVERT' || verdict === 'DECLARATION_DANS_LES_TEMPS') {
      return 'verdict-ok';
    }
    if (verdict === 'DELAI_IMMINENT') {
      return 'verdict-warn';
    }
    // DELAI_DEPASSE, DECLARATION_HORS_DELAI
    return 'verdict-critical';
  }

  verdictIcon(verdict: AtFedrisDeclarationVerdict): string {
    if (verdict === 'DELAI_OUVERT' || verdict === 'DECLARATION_DANS_LES_TEMPS') {
      return 'check_circle';
    }
    if (verdict === 'DELAI_IMMINENT') {
      return 'schedule';
    }
    return 'error';
  }

  verdictLabel(verdict: AtFedrisDeclarationVerdict): string {
    return humanizeVerdict(verdict);
  }

  /**
   * Classe CSS de l'encart conséquences :
   *   - rouge si verdict DEPASSE / HORS_DELAI
   *   - ambre si IMMINENT
   *   - neutre (info) sinon.
   */
  consequencesClass(verdict: AtFedrisDeclarationVerdict): string {
    if (verdict === 'DELAI_DEPASSE' || verdict === 'DECLARATION_HORS_DELAI') {
      return 'alert-critical';
    }
    if (verdict === 'DELAI_IMMINENT') {
      return 'alert-warning';
    }
    return 'alert-info';
  }

  /** Vrai si `joursRestants` est négatif → afficher « Retard de N jour(s) ». */
  isJoursEnRetard(): boolean {
    const j = this.result()?.joursRestants;
    return typeof j === 'number' && j < 0;
  }

  /** Valeur absolue des jours de retard (à n'utiliser que si isJoursEnRetard() === true). */
  joursDeRetard(): number {
    return Math.abs(this.result()?.joursRestants ?? 0);
  }

  /** Classe CSS pour la cellule joursRestants (≤ 0 rouge, ≤ 2 ambre). */
  joursRestantsClass(): string {
    const j = this.result()?.joursRestants;
    if (j === null || j === undefined) return '';
    if (j < 0) return 'jours-critical';
    if (j <= 2) return 'jours-warning';
    return 'jours-ok';
  }

  calculate(): void {
    if (!this.formValid()) return;
    if (this.workspaceCountry !== 'BELGIQUE') {
      this.snackBar.open('Outil indisponible pour ce workspace', 'Fermer',
        { duration: 4000, panelClass: 'snack-error' });
      return;
    }
    const request: AtFedrisDeclarationRequest = {
      dateAccident: this.dateAccident()!,
      dateConnaissanceEmployeur: this.dateConnaissanceEmployeur() || null,
      dateActionEnvisagee: this.dateActionEnvisagee() || null,
      dateDeclarationEffectuee: this.declarationDejaEffectuee()
        ? (this.dateDeclarationEffectuee() || null)
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
        this.dateAccident.set(r.dateAccident);
        this.dateConnaissanceEmployeur.set(r.dateConnaissanceEmployeur);
        this.dateActionEnvisagee.set(r.dateActionEnvisagee);
        this.declarationDejaEffectuee.set(r.dateDeclarationEffectuee !== null);
        this.dateDeclarationEffectuee.set(r.dateDeclarationEffectuee);
        // Valeurs persistées = saisie avocat — jamais de badge IA.
        this.provenanceDateAccident.set(null);
        this.provenanceDateConnaissance.set(null);
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
