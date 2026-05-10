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
import { MatRadioModule } from '@angular/material/radio';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MediationFamilialePreSaisineService } from '../../core/services/mediation-familiale.service';
import {
  MediationException,
  MediationFamilialePreSaisineRequest,
  MediationFamilialePreSaisineResponse,
  MediationMotifSaisine,
  MediationVerdict,
} from '../../core/models/mediation-familiale.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import { PieceManquanteEntry } from '../../core/models/case-analysis.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { AiQuestion } from '../../core/models/ai-question.model';
import {
  CoherenceAlert,
} from '../../shared/coherence-popover/coherence-alert.model';
import { CoherenceAlertBuilder } from '../../shared/coherence-popover/coherence-alert-builder';

export type MediationAlertField = 'MOTIF_SAISINE';
export type MediationCoherenceAlert = CoherenceAlert<MediationAlertField>;

/**
 * F-210 SF-210-02 : composant Angular standalone pour l'outil "Médiation
 * familiale obligatoire pré-saisine JAF" (Cciv art. 373-2-10 al. 3 + Loi
 * 18/11/2016 + CPC art. 1108). FRANCE uniquement, droit famille.
 *
 * Pattern de référence : `divorce-accepte-section`.
 *
 * Pré-fill IA : `motifSaisine` via {@code FamilleExtractedData.motifSaisineMediationDetecte}.
 * Validation F-IA-03 : alerte si l'avocat sélectionne un motif différent du
 * motif détecté par l'IA.
 */
@Component({
  selector: 'app-mediation-familiale-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatRadioModule, MatSelectModule,
    MatSlideToggleModule, MatProgressSpinnerModule,
    MatTooltipModule,
    LegalCitationsPipe,
  ],
  templateUrl: './mediation-familiale-section.component.html',
  styleUrl: './mediation-familiale-section.component.scss',
})
export class MediationFamilialeSectionComponent implements OnInit, OnChanges {
  static readonly TOOL_LABEL = 'MÉDIATION FAMILIALE PRÉ-SAISINE (FR)';
  static readonly TOOL_ICON = 'group';

  @Input() caseFileId!: string;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';
  @Input() aiData?: FamilleExtractedData | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;
  @Input() forceExpanded = false;

  private aiDataSignal = signal<FamilleExtractedData | null | undefined>(undefined);

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<MediationFamilialePreSaisineResponse | null>(null);

  motifSaisine = signal<MediationMotifSaisine | null>(null);
  mediationTentee = signal<boolean>(false);
  dateMediation = signal<string | null>(null);
  exceptionApplicable = signal<MediationException>('AUCUNE');
  exceptionDetail = signal<string | null>(null);

  provenanceMotif = signal<'IA' | null>(null);

  readonly motifsOptions: { value: MediationMotifSaisine; label: string }[] = [
    { value: 'AUTORITE_PARENTALE', label: 'Autorité parentale' },
    { value: 'CONTRIBUTION_ENTRETIEN', label: 'Contribution à l\'entretien' },
    { value: 'RESIDENCE_ENFANT', label: 'Résidence de l\'enfant' },
    { value: 'DROIT_VISITE', label: 'Droit de visite et d\'hébergement' },
    { value: 'DIVORCE_CONTENTIEUX', label: 'Divorce contentieux' },
    { value: 'SUCCESSION', label: 'Succession' },
    { value: 'AUTRE', label: 'Autre' },
  ];

  readonly exceptionsOptions: { value: MediationException; label: string }[] = [
    { value: 'AUCUNE', label: 'Aucune' },
    { value: 'URGENCE', label: 'Urgence caractérisée' },
    { value: 'VIOLENCE', label: 'Violences conjugales / familiales' },
    { value: 'ACCORD_PREALABLE', label: 'Accord préalable conjoint' },
    { value: 'MOTIF_LEGITIME', label: 'Motif légitime' },
    { value: 'AUTRE', label: 'Autre' },
  ];

  coherenceAlerts = computed<Partial<Record<MediationAlertField, MediationCoherenceAlert>>>(() => {
    if (!this.showForm()) return {};
    const alerts: Partial<Record<MediationAlertField, MediationCoherenceAlert>> = {};
    const motifAlert = this.buildMotifAlert();
    if (motifAlert) alerts.MOTIF_SAISINE = motifAlert;
    return alerts;
  });

  alertsSummary = computed(() => ({
    total: Object.values(this.coherenceAlerts()).length,
  }));

  constructor(
    private service: MediationFamilialePreSaisineService,
    private snackBar: MatSnackBar,
    @Optional() private dashboardRefresh: CaseDashboardRefreshService | null,
  ) {}

  ngOnInit(): void {
    if (this.forceExpanded) this.collapsed.set(false);
    this.aiDataSignal.set(this.aiData);
    if (this.workspaceCountry === 'FRANCE') {
      this.load();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['forceExpanded'] && this.forceExpanded) this.collapsed.set(false);
    if (changes['aiData']) this.aiDataSignal.set(this.aiData);
    if (changes['aiData'] && !changes['aiData'].firstChange
        && this.showForm() && !this.result()) {
      this.prefillFromAi();
    }
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  isFrance(): boolean {
    return this.workspaceCountry === 'FRANCE';
  }

  formValid(): boolean {
    return this.motifSaisine() !== null;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  onMotifChange(value: MediationMotifSaisine): void {
    this.motifSaisine.set(value);
    this.provenanceMotif.set(null);
  }

  onMediationTenteeChange(value: boolean): void {
    this.mediationTentee.set(value);
  }

  onDateMediationChange(value: string | null): void {
    this.dateMediation.set(value || null);
  }

  onExceptionChange(value: MediationException): void {
    this.exceptionApplicable.set(value);
  }

  onExceptionDetailChange(value: string | null): void {
    this.exceptionDetail.set(value || null);
  }

  calculate(): void {
    if (!this.formValid() || this.motifSaisine() === null) return;
    const request: MediationFamilialePreSaisineRequest = {
      motifSaisine: this.motifSaisine()!,
      mediationTentee: this.mediationTentee(),
      dateMediation: this.dateMediation(),
      exceptionApplicable: this.exceptionApplicable(),
      exceptionDetail: this.exceptionDetail(),
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Analyse de recevabilité calculée', 'OK', { duration: 2500 });
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
        this.prefillFromAi();
        this.loading.set(false);
      },
    });
  }

  private applyResult(r: MediationFamilialePreSaisineResponse): void {
    this.result.set(r);
    this.motifSaisine.set(r.motifSaisine);
    this.mediationTentee.set(r.mediationTentee);
    this.dateMediation.set(r.dateMediation);
    this.exceptionApplicable.set(r.exceptionApplicable);
    this.exceptionDetail.set(r.exceptionDetail);
    this.provenanceMotif.set(null);
    this.showForm.set(false);
  }

  private prefillFromAi(): void {
    const ai = this.aiDataSignal();
    if (!ai) return;
    const motif = ai.motifSaisineMediationDetecte;
    if (motif && (this.motifSaisine() === null || this.provenanceMotif() === 'IA')) {
      this.motifSaisine.set(motif as MediationMotifSaisine);
      this.provenanceMotif.set('IA');
    }
  }

  private buildMotifAlert(): MediationCoherenceAlert | null {
    const iaVal = this.aiDataSignal()?.motifSaisineMediationDetecte;
    const user = this.motifSaisine();
    if (!iaVal || !user) return null;
    if (iaVal === user) return null;
    if (this.provenanceMotif() === 'IA') return null;
    const builder = CoherenceAlertBuilder.forField<MediationAlertField>('MOTIF_SAISINE')
      .addSource('IA', {
        expectedDisplay: this.motifLabel(iaVal as MediationMotifSaisine),
        reason: `Analyse du dossier : motif de saisine détecté = ${this.motifLabel(iaVal as MediationMotifSaisine)}`,
      });
    return builder.build();
  }

  motifLabel(m: MediationMotifSaisine): string {
    return this.motifsOptions.find(o => o.value === m)?.label ?? m;
  }

  exceptionLabel(e: MediationException): string {
    return this.exceptionsOptions.find(o => o.value === e)?.label ?? e;
  }

  alertTooltip(alert: MediationCoherenceAlert): string {
    return alert.reason;
  }

  alertBadgeLabel(alert: MediationCoherenceAlert): string {
    return `Incohérence détectée (${alert.expectedDisplay})`;
  }

  verdictBannerClass(v: MediationVerdict): string {
    switch (v) {
      case 'RECEVABLE': return 'mfp-verdict-banner mfp-verdict-banner--available';
      case 'IRRECEVABLE': return 'mfp-verdict-banner mfp-verdict-banner--danger';
      default: return 'mfp-verdict-banner mfp-verdict-banner--info';
    }
  }

  verdictBannerLabel(v: MediationVerdict): string {
    switch (v) {
      case 'RECEVABLE': return 'Saisine recevable';
      case 'IRRECEVABLE': return 'Saisine irrecevable en l\'état';
      default: return 'Motif non concerné';
    }
  }

  verdictBannerIcon(v: MediationVerdict): string {
    switch (v) {
      case 'RECEVABLE': return 'check_circle';
      case 'IRRECEVABLE': return 'error';
      default: return 'info';
    }
  }

  /**
   * F-210 SF-210-02 : pattern miroir TOOL_LABEL — utilisé par le panel
   * F-IA-04 pour afficher le badge "Pré-rempli par l'IA (N champs)".
   */
  static getPrefillCount(input: { aiData?: FamilleExtractedData | null }): number {
    const ai = input.aiData;
    if (!ai) return 0;
    let n = 0;
    if (typeof ai.motifSaisineMediationDetecte === 'string'
        && ai.motifSaisineMediationDetecte) n++;
    return n;
  }
}
