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
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { DivorceDcBeService } from '../../core/services/divorce-dc-be.service';
import {
  DivorceDcBeRequest,
  DivorceDcBeResponse,
  DivorceDcBeVerdict,
} from '../../core/models/divorce-dc-be.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { PieceManquanteEntry } from '../../core/models/case-analysis.model';
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
import { DivorceDcBeSectionPrefillRules } from './divorce-dc-be-section-prefill-rules';

/**
 * F-243 : champs F-IA-03 audités par l'outil F-FA-11 BE.
 * Un par field pré-remplissable ou contradictoirement saisissable depuis l'IA.
 */
export type DivorceDcBeAlertField = 'DATE_SIGNATURE_CONVENTION';

export type DivorceDcBeCoherenceAlert = CoherenceAlert<DivorceDcBeAlertField>;
export type DivorceDcBeAlertSource = CoherenceAlertSource;

const ISO_DATE_REGEX = /^\d{4}-\d{2}-\d{2}$/;

/**
 * F-243 : composant Angular standalone pour l'outil "Divorce par consentement
 * mutuel — Belgique" (F-FA-11 BE, CJ art. 1287-1304, Loi 27/04/2007).
 *
 * Pattern de référence : `divorce-accepte-section` (équivalent FR).
 *
 * - Form : 2 dates (signature convention + audience homologation) + 4
 *   slide-toggles convention (logement / biens / garde / contributions)
 *   + 2 slide-toggles contextuels (enfants mineurs / consentement).
 * - Verdict ternaire RECEVABLE / IRRECEVABLE / NON_CONCERNE — palette
 *   navy `--available` / rouge `--danger` / gris neutre.
 * - Pré-fill IA via `aiData?.dateAcceptationPV` (sortie F-241 prompt
 *   `date_accord_initial_divorce`).
 * - Validation F-IA-03 sur le field `DATE_SIGNATURE_CONVENTION` via
 *   `CoherenceAlertBuilder`.
 * - Refresh dashboard F-IA-02 + MatSnackBar erreurs.
 * - Gate `workspaceCountry === 'BELGIQUE'` — bannière info si mismatch.
 */
@Component({
  selector: 'app-divorce-dc-be-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatSlideToggleModule, MatProgressSpinnerModule,
    LegalCitationsPipe,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './divorce-dc-be-section.component.html',
  styleUrl: './divorce-dc-be-section.component.scss',
})
export class DivorceDcBeSectionComponent implements OnInit, OnChanges {
  static readonly TOOL_LABEL = 'DIVORCE CONSENTEMENT MUTUEL — CJ ART. 1287 BE';
  static readonly TOOL_ICON = 'handshake';

  /**
   * F-236 : compteur miroir de `prefillFromAi()` runtime via helper partagé.
   */
  static getPrefillCount(input: {
    aiData?: FamilleExtractedData | null;
    procedureChecks?: unknown[];
    aiQuestions?: unknown[];
    piecesManquantes?: unknown[];
    triggerEvents?: unknown[];
    workspaceCountry?: string;
  }): number {
    return DivorceDcBeSectionPrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'BELGIQUE';
  @Input() aiData?: FamilleExtractedData | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;
  @Input() forceExpanded = false;

  private aiDataSignal = signal<FamilleExtractedData | null | undefined>(undefined);
  private procedureChecksSignal = signal<ProcedureCheck[]>([]);
  private aiQuestionsSignal = signal<AiQuestion[]>([]);
  private piecesManquantesSignal = signal<PieceManquanteEntry[]>([]);

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<DivorceDcBeResponse | null>(null);

  // Form fields
  dateSignatureConvention = signal<string | null>(null);
  dateAudienceHomologation = signal<string | null>(null);
  conventionLogement = signal<boolean>(false);
  conventionBiens = signal<boolean>(false);
  conventionGardeEnfants = signal<boolean>(false);
  conventionContributions = signal<boolean>(false);
  enfantsMineursCommuns = signal<boolean>(false);
  epouxConsentent = signal<boolean>(true);

  // Provenance IA par champ pré-remplissable.
  provenanceDateSignatureConvention = signal<'IA' | null>(null);

  sourceExplanations = signal<Map<string, SourceExplanation[]>>(new Map());

  coherenceAlerts = computed<Partial<Record<DivorceDcBeAlertField, DivorceDcBeCoherenceAlert>>>(() => {
    if (!this.showForm()) return {};
    const alerts: Partial<Record<DivorceDcBeAlertField, DivorceDcBeCoherenceAlert>> = {};
    const dateAlert = this.buildDateSignatureConventionAlert();
    if (dateAlert) alerts.DATE_SIGNATURE_CONVENTION = dateAlert;
    return alerts;
  });

  alertsSummary = computed(() => ({ total: Object.values(this.coherenceAlerts()).length }));

  constructor(
    private service: DivorceDcBeService,
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

    if (changes['aiData'] && !changes['aiData'].firstChange
        && this.showForm() && !this.result()) {
      this.prefillFromAi();
    }
  }

  private loadSourceExplanations(): void {
    if (!this.caseFileId || !this.sourceExplanationService) return;
    this.sourceExplanationService.getForCaseFile(this.caseFileId).subscribe({
      next: (map) => this.sourceExplanations.set(map),
      error: () => { /* fail-open */ },
    });
  }

  explanationFor(field: DivorceDcBeAlertField): SourceExplanation[] {
    const key = (() => {
      switch (field) {
        case 'DATE_SIGNATURE_CONVENTION': return 'FA11_DATE_SIGNATURE_CONVENTION';
      }
    })();
    return this.sourceExplanations().get(key) ?? [];
  }

  alertTooltip(alert: DivorceDcBeCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: DivorceDcBeCoherenceAlert): string {
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

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  formValid(): boolean {
    const dateSig = this.dateSignatureConvention();
    return !!dateSig && ISO_DATE_REGEX.test(dateSig);
  }

  editMode(): void {
    this.showForm.set(true);
  }

  onDateSignatureConventionChange(value: string | null): void {
    this.dateSignatureConvention.set(value || null);
    this.provenanceDateSignatureConvention.set(null);
  }

  onDateAudienceHomologationChange(value: string | null): void {
    this.dateAudienceHomologation.set(value || null);
  }

  onConventionLogementChange(value: boolean): void { this.conventionLogement.set(value); }
  onConventionBiensChange(value: boolean): void { this.conventionBiens.set(value); }
  onConventionGardeEnfantsChange(value: boolean): void { this.conventionGardeEnfants.set(value); }
  onConventionContributionsChange(value: boolean): void { this.conventionContributions.set(value); }
  onEnfantsMineursCommunsChange(value: boolean): void { this.enfantsMineursCommuns.set(value); }
  onEpouxConsententChange(value: boolean): void { this.epouxConsentent.set(value); }

  calculate(): void {
    if (!this.formValid()) return;
    const request: DivorceDcBeRequest = {
      dateSignatureConvention: this.dateSignatureConvention()!,
      dateAudienceHomologation: this.dateAudienceHomologation(),
      conventionLogement: this.conventionLogement(),
      conventionBiens: this.conventionBiens(),
      conventionGardeEnfants: this.conventionGardeEnfants(),
      conventionContributions: this.conventionContributions(),
      enfantsMineursCommuns: this.enfantsMineursCommuns(),
      epouxConsentent: this.epouxConsentent(),
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Analyse divorce consentement mutuel BE calculée', 'OK', { duration: 2500 });
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
        this.applyResult(r);
        this.loading.set(false);
      },
      error: () => {
        // 404 attendu si aucune analyse — on reste en mode formulaire.
        this.prefillFromAi();
        this.loading.set(false);
      },
    });
  }

  private applyResult(r: DivorceDcBeResponse): void {
    this.result.set(r);
    this.dateSignatureConvention.set(r.dateSignatureConvention);
    this.dateAudienceHomologation.set(r.dateAudienceHomologation);
    this.conventionLogement.set(r.conventionLogement);
    this.conventionBiens.set(r.conventionBiens);
    this.conventionGardeEnfants.set(r.conventionGardeEnfants);
    this.conventionContributions.set(r.conventionContributions);
    this.enfantsMineursCommuns.set(r.enfantsMineursCommuns);
    this.epouxConsentent.set(r.epouxConsentent);
    this.provenanceDateSignatureConvention.set(null);
    this.showForm.set(false);
  }

  private prefillFromAi(): void {
    const helperInput = { aiData: this.aiDataSignal() };
    const dateSig = DivorceDcBeSectionPrefillRules.computeDateSignatureConvention(helperInput);
    if (dateSig !== null
        && (this.dateSignatureConvention() === null || this.provenanceDateSignatureConvention() === 'IA')) {
      this.dateSignatureConvention.set(dateSig);
      this.provenanceDateSignatureConvention.set('IA');
    }
  }

  // ---------------------------------------------------------------------------
  // Builder d'alerte F-IA-03
  // ---------------------------------------------------------------------------

  private buildDateSignatureConventionAlert(): DivorceDcBeCoherenceAlert | null {
    const iaDate = this.aiDataSignal()?.dateAcceptationPV;
    const user = this.dateSignatureConvention();
    if (!user) return null;
    if (!iaDate || !ISO_DATE_REGEX.test(iaDate)) return null;
    if (user === iaDate) return null;
    const builder = CoherenceAlertBuilder.forField<DivorceDcBeAlertField>('DATE_SIGNATURE_CONVENTION')
      .addSource('IA', {
        expectedDisplay: iaDate,
        reason: `Analyse du dossier : convention préalable signée le ${iaDate}`,
      });
    const piece = this.findPieceManquante(['FA11_DATE_SIGNATURE_CONVENTION', 'CONVENTION_PREALABLE']);
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
  // Helpers d'affichage du résultat
  // ---------------------------------------------------------------------------

  verdictBannerClass(verdict: DivorceDcBeVerdict): string {
    switch (verdict) {
      case 'RECEVABLE': return 'dc-be-verdict-banner dc-be-verdict-banner--available';
      case 'IRRECEVABLE': return 'dc-be-verdict-banner dc-be-verdict-banner--danger';
      case 'NON_CONCERNE': return 'dc-be-verdict-banner dc-be-verdict-banner--neutral';
    }
  }

  verdictBannerLabel(verdict: DivorceDcBeVerdict): string {
    switch (verdict) {
      case 'RECEVABLE': return 'Procédure DC recevable';
      case 'IRRECEVABLE': return 'Procédure DC irrecevable';
      case 'NON_CONCERNE': return 'DC non concerné';
    }
  }

  verdictBannerIcon(verdict: DivorceDcBeVerdict): string {
    switch (verdict) {
      case 'RECEVABLE': return 'check_circle';
      case 'IRRECEVABLE': return 'error';
      case 'NON_CONCERNE': return 'info';
    }
  }
}
