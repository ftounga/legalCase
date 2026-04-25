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
import { ContestationAreService } from '../../core/services/contestation-are.service';
import {
  ContestationAreRequest,
  ContestationAreResponse,
  MOTIFS_CONTESTATION_ARE,
  MotifContestationAre,
  MotifContestationAreOption,
  PREUVES_ARE,
  PreuveAre,
  PreuveAreOption,
  TYPES_DECISION_CONTESTEE,
  TypeDecisionContestee,
  TypeDecisionContesteeOption,
  VERDICT_ARE_LABELS,
  VerdictAre,
} from '../../core/models/contestation-are.model';
import {
  PieceManquanteEntry,
  TravailExtractedData,
} from '../../core/models/case-analysis.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { AiQuestion } from '../../core/models/ai-question.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { CoherencePopoverTriggerDirective } from '../../shared/coherence-popover/coherence-popover-trigger.directive';
import {
  CoherenceAlert,
  CoherenceAlertSeverity,
} from '../../shared/coherence-popover/coherence-alert.model';
import { CoherenceAlertBuilder } from '../../shared/coherence-popover/coherence-alert-builder';
import { SourceExplanation } from '../../core/models/source-explanation.model';
import { SourceExplanationService } from '../../core/services/source-explanation.service';

/** Regex ISO strict YYYY-MM-DD. */
const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

export type ContestationAreAlertField = 'DATE_NOTIFICATION';

export type ContestationAreAlertSeverity = CoherenceAlertSeverity;
export type ContestationAreCoherenceAlert =
  CoherenceAlert<ContestationAreAlertField>;

/**
 * SF-DT-35-02 : outil décisionnel "Contestation ARE / France Travail
 * (ex-Pôle emploi)" (F-DT-35). FR uniquement. Consomme l'API figée
 * par SF-DT-35-01 (backend).
 *
 * Procédure : aide l'avocat à structurer une contestation de décision
 * France Travail (ex-Pôle emploi) — refus d'ouverture, montant indemnité,
 * radiation, trop-perçu — et à choisir entre recours hiérarchique
 * (priorité, délai 2 mois) et contentieux TA (délai 2 mois). Calcule
 * un score de probabilité de succès et un verdict de stratégie.
 *
 * Palette : navy / or (palette standard) — pas d'urgence < 72h.
 *
 * Pré-fill IA + validation F-IA-03 obligatoires (règle CLAUDE.md) :
 * - `dateLicenciement` (TravailExtractedData) → `dateNotificationDecision`
 *   en fallback gracieux (l'IA n'a pas accès à la date de notification
 *   France Travail elle-même, seulement à la date de rupture).
 *
 * Mention "France Travail" partout (ex-Pôle emploi explicité dans le titre).
 */
@Component({
  selector: 'app-contestation-are-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule, DecimalPipe,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatSlideToggleModule, MatProgressSpinnerModule,
    LegalCitationsPipe,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './contestation-are-section.component.html',
  styleUrl: './contestation-are-section.component.scss',
})
export class ContestationAreSectionComponent implements OnInit, OnChanges {
  @Input() caseFileId!: string;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';

  // Sources IA (pré-fill + alertes F-IA-03).
  @Input() aiData?: Partial<TravailExtractedData> | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;

  // Signals miroirs des inputs IA pour réactivité computed.
  private aiDataSignal = signal<Partial<TravailExtractedData> | null | undefined>(undefined);
  private procedureChecksSignal = signal<ProcedureCheck[]>([]);
  private aiQuestionsSignal = signal<AiQuestion[]>([]);
  private piecesManquantesSignal = signal<PieceManquanteEntry[]>([]);

  collapsed = signal(true);
  loading = signal(false);
  analyzing = signal(false);
  showForm = signal(true);
  result = signal<ContestationAreResponse | null>(null);

  // Champs form.
  typeDecisionContestee = signal<TypeDecisionContestee | null>(null);
  motifContestation = signal<MotifContestationAre | null>(null);
  dateNotificationDecision = signal<string | null>(null);
  dateRecoursHierarchiquePropose = signal<string | null>(null);
  preuvesProduites = signal<PreuveAre[]>([]);
  montantContesteEur = signal<number | null>(null);
  demandeurDejaSaisiTribunal = signal<boolean>(false);
  delaiContestationRespecte = signal<boolean>(true);

  // Provenance IA par champ.
  provenanceDateNotification = signal<'IA' | null>(null);

  readonly typesDecision: TypeDecisionContesteeOption[] = TYPES_DECISION_CONTESTEE;
  readonly motifsContestation: MotifContestationAreOption[] = MOTIFS_CONTESTATION_ARE;
  readonly preuvesOptions: PreuveAreOption[] = PREUVES_ARE;
  readonly verdictLabels = VERDICT_ARE_LABELS;

  /** Aujourd'hui en YYYY-MM-DD pour l'attribut max des inputs date. */
  readonly todayIso: string = new Date().toISOString().slice(0, 10);

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  /** Alertes F-IA-03 — gate strict : seulement quand le formulaire est affiché. */
  coherenceAlerts = computed<Partial<Record<ContestationAreAlertField,
                                            ContestationAreCoherenceAlert>>>(() => {
    if (!this.showForm()) return {};
    if (!this.isFrance()) return {};
    const alerts: Partial<Record<ContestationAreAlertField,
                                 ContestationAreCoherenceAlert>> = {};
    const a1 = this.buildDateNotificationAlert();
    if (a1) alerts.DATE_NOTIFICATION = a1;
    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    const blockers = values.filter((a) => a?.severity === 'CRITICAL').length;
    return { total: values.length, blockers };
  });

  // Source explanations (SF-155-07 DIV-7).
  sourceExplanations = signal<Map<string, SourceExplanation[]>>(new Map());

  constructor(
    private service: ContestationAreService,
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
    if (changes['piecesManquantes']) {
      this.piecesManquantesSignal.set(this.piecesManquantes ?? []);
    }
    // Re-prefill si aiData arrive après mount avant la première résolution.
    if (
      changes['aiData'] &&
      !changes['aiData'].firstChange &&
      this.isFrance() &&
      this.showForm() &&
      !this.result()
    ) {
      this.prefillFromAi();
    }
  }

  private loadSourceExplanations(): void {
    if (!this.caseFileId) return;
    if (!this.sourceExplanationService) return;
    this.sourceExplanationService.getForCaseFile(this.caseFileId).subscribe({
      next: (map) => this.sourceExplanations.set(map),
      error: () => { /* fail-open */ },
    });
  }

  /** Mapping field → sourceKey. Convention `DT35_<FIELD>`. */
  explanationFor(field: ContestationAreAlertField): SourceExplanation[] {
    const key = (() => {
      switch (field) {
        case 'DATE_NOTIFICATION': return 'DT35_DATE_NOTIFICATION';
      }
    })();
    return this.sourceExplanations().get(key) ?? [];
  }

  toggleCollapse(): void {
    this.collapsed.update((v) => !v);
  }

  formValid(): boolean {
    const t = this.typeDecisionContestee();
    const m = this.motifContestation();
    const dn = this.dateNotificationDecision();
    const dr = this.dateRecoursHierarchiquePropose();
    const mc = this.montantContesteEur();
    if (!t || !m || !dn || !dr) return false;
    if (mc === null || mc < 0) return false;
    if (dn > this.todayIso) return false;
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // ----- Handlers manuels (effacent provenance IA si concerné). -----

  onTypeDecisionChange(value: TypeDecisionContestee | null): void {
    this.typeDecisionContestee.set(value);
  }

  onMotifChange(value: MotifContestationAre | null): void {
    this.motifContestation.set(value);
  }

  onDateNotificationChange(value: string | null): void {
    this.dateNotificationDecision.set(value || null);
    this.provenanceDateNotification.set(null);
  }

  onDateRecoursChange(value: string | null): void {
    this.dateRecoursHierarchiquePropose.set(value || null);
  }

  onPreuvesChange(value: PreuveAre[] | null): void {
    this.preuvesProduites.set(value ?? []);
  }

  onMontantChange(value: number | null): void {
    this.montantContesteEur.set(value);
  }

  onTribunalChange(value: boolean): void {
    this.demandeurDejaSaisiTribunal.set(value);
  }

  onDelaiRespecteChange(value: boolean): void {
    this.delaiContestationRespecte.set(value);
  }

  analyze(): void {
    if (!this.formValid()) return;
    const request: ContestationAreRequest = {
      typeDecisionContestee: this.typeDecisionContestee()!,
      motifContestation: this.motifContestation()!,
      dateNotificationDecision: this.dateNotificationDecision()!,
      dateRecoursHierarchiquePropose: this.dateRecoursHierarchiquePropose()!,
      preuvesProduites: this.preuvesProduites(),
      montantContesteEur: this.montantContesteEur()!,
      demandeurDejaSaisiTribunal: this.demandeurDejaSaisiTribunal(),
      delaiContestationRespecte: this.delaiContestationRespecte(),
    };
    this.analyzing.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('Contestation ARE analysée', 'OK', { duration: 2500 });
        this.dashboardRefresh?.triggerRefresh();
      },
      error: (err) => {
        this.analyzing.set(false);
        const msg = err?.error?.message || err?.error || 'Erreur lors de l\'analyse';
        this.snackBar.open(String(msg), 'Fermer', { duration: 5000, panelClass: 'snack-error' });
      },
    });
  }

  // ----- Affichage verdict -----

  verdictLabel(verdict: VerdictAre | null | undefined): string {
    if (!verdict) return '';
    return this.verdictLabels[verdict];
  }

  /**
   * Palette navy/or standard. Rouge réservé à INSUFFISAMMENT_FONDE
   * (alerte critique : la contestation a peu de chances).
   */
  verdictBannerClass(verdict: VerdictAre | null | undefined): string {
    switch (verdict) {
      case 'RECOURS_HIERARCHIQUE_PRIORITAIRE': return 'ca-banner ca-banner--primary';
      case 'CONTENTIEUX_TA_DIRECT_POSSIBLE':   return 'ca-banner ca-banner--accent';
      case 'INSUFFISAMMENT_FONDE':             return 'ca-banner ca-banner--danger';
      case 'AUTRE_VOIE':                       return 'ca-banner ca-banner--info';
      default:                                 return 'ca-banner ca-banner--info';
    }
  }

  verdictBannerIcon(verdict: VerdictAre | null | undefined): string {
    switch (verdict) {
      case 'RECOURS_HIERARCHIQUE_PRIORITAIRE': return 'gavel';
      case 'CONTENTIEUX_TA_DIRECT_POSSIBLE':   return 'balance';
      case 'INSUFFISAMMENT_FONDE':             return 'error_outline';
      case 'AUTRE_VOIE':                       return 'info_outline';
      default:                                 return 'info_outline';
    }
  }

  /** Classe CSS du score. */
  scoreClass(score: number): string {
    if (score >= 70) return 'ca-score--high';
    if (score >= 40) return 'ca-score--medium';
    return 'ca-score--low';
  }

  alertBadgeLabel(alert: ContestationAreCoherenceAlert): string {
    const prefix = alert.severity === 'CRITICAL'
      ? 'Risque d\'irrecevabilité'
      : 'Incohérence détectée';
    return `${prefix} (${alert.expectedDisplay})`;
  }

  // ----- Pré-fill IA -----

  /**
   * SF-DT-35-02 : pré-remplit le form depuis `aiData`. N'écrase jamais une
   * saisie avocat existante — la garde est dans le call site.
   *
   * Mapping :
   * - `aiData.dateLicenciement` → `dateNotificationDecision` en fallback
   *   gracieux (l'IA n'a pas accès à la date de notification France Travail
   *   elle-même, seulement à la rupture du contrat de travail).
   */
  private prefillFromAi(): void {
    const ai = this.aiDataSignal();
    if (!ai) return;
    if (!this.isFrance()) return;
    if (!this.showForm()) return;

    const date = ai.dateLicenciement;
    if (typeof date === 'string' && ISO_DATE_RE.test(date) && date <= this.todayIso) {
      if (this.dateNotificationDecision() === null
          || this.provenanceDateNotification() === 'IA') {
        this.dateNotificationDecision.set(date);
        this.provenanceDateNotification.set('IA');
      }
    }
  }

  // ----- Builders alertes F-IA-03 -----

  /**
   * Alerte WARNING quand la `dateNotificationDecision` saisie diverge de la
   * `dateLicenciement` extraite par l'IA. La date de notification France
   * Travail est typiquement postérieure à la rupture, mais un écart > 12 mois
   * peut suggérer une erreur de saisie.
   */
  private buildDateNotificationAlert(): ContestationAreCoherenceAlert | null {
    const user = this.dateNotificationDecision();
    if (!user) return null;
    const builder = CoherenceAlertBuilder
      .forField<ContestationAreAlertField>('DATE_NOTIFICATION')
      .withSeverity('WARNING');

    const ai = this.aiDataSignal()?.dateLicenciement;
    if (typeof ai === 'string' && ISO_DATE_RE.test(ai) && ai !== user) {
      // Diverge sur date pure ; on note la rupture comme référence.
      builder.addSource('IA', {
        expectedDisplay: ai,
        reason: `L'analyse a détecté une date de rupture du contrat différente : ${ai}. Vérifiez que la date de notification France Travail est cohérente.`,
      });
    }

    const piece = this.findPieceManquante(['DT35_DATE_NOTIFICATION',
                                           'DATE_NOTIFICATION_FRANCE_TRAVAIL']);
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

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.typeDecisionContestee.set(r.typeDecisionContestee);
        this.motifContestation.set(r.motifContestation);
        this.dateNotificationDecision.set(r.dateNotificationDecision);
        this.dateRecoursHierarchiquePropose.set(r.dateRecoursHierarchiquePropose);
        this.preuvesProduites.set(r.preuvesProduites ?? []);
        this.montantContesteEur.set(r.montantContesteEur);
        this.demandeurDejaSaisiTribunal.set(r.demandeurDejaSaisiTribunal);
        this.delaiContestationRespecte.set(r.delaiContestationRespecte);
        this.showForm.set(false);
        // Reset provenance — les valeurs viennent du backend.
        this.provenanceDateNotification.set(null);
        this.loading.set(false);
      },
      error: () => {
        // 404 attendu si aucune analyse — on tente le pré-fill IA.
        this.prefillFromAi();
        this.loading.set(false);
      },
    });
  }
}
