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
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';

import { CceSuspensionBeService } from '../../core/services/cce-suspension-be.service';
import {
  CceSuspensionBeRequest,
  CceSuspensionBeResponse,
  CceSuspensionBeVerdict,
} from '../../core/models/cce-suspension-be.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { CceSuspensionBePrefillRules } from './cce-suspension-be-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';
import { CoherencePopoverTriggerDirective } from '../../shared/coherence-popover/coherence-popover-trigger.directive';
import {
  CoherenceAlert,
  CoherenceAlertSource,
} from '../../shared/coherence-popover/coherence-alert.model';
import { CoherenceAlertBuilder } from '../../shared/coherence-popover/coherence-alert-builder';

const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

export type IM57_SuspAlertField = 'DATE_NOTIFICATION';
export type IM57_SuspAlertSource = CoherenceAlertSource;
export type IM57_SuspCoherenceAlert = CoherenceAlert<IM57_SuspAlertField>;

/**
 * SF-221-05 — Outil décisionnel « Recours CCE en suspension ordinaire (BE) »
 * (F-IM-57-cce-suspension-be).
 *
 * BELGIQUE uniquement — évalue les conditions de la SUSPENSION ORDINAIRE (référé
 * administratif : urgence non extrême + risque de préjudice grave difficilement
 * réparable, art. 39/82 Loi 15/12/1980 ; loi 15/09/2006), greffée sur la requête en
 * annulation 30j. Visibilité CONTEXTUAL — flag `cce_suspension_detecte`.
 *
 * 3e recours CCE DISTINCT de l'annulation 30j (F-IM-31) et de l'extrême urgence
 * 5j ouvrables (F-IM-32). Quand une mesure d'éloignement est imminente, le verdict
 * REORIENTER_EXTREME_URGENCE renvoie vers F-IM-32.
 *
 * <p>Conforme F-IA-04 :
 *  - standalone OnPush, palette : vert CONDITIONS_REUNIES, orange URGENCE/PREJUDICE/
 *    HORS_DELAI, bleu REORIENTER_EXTREME_URGENCE
 *  - pré-fill IA RÉEL 3 champs (dateNotificationDecision, urgenceInvocable,
 *    risquePrejudiceGraveDifficilementReparable) ; recoursAnnulationIntroduit +
 *    mesureEloignementImminente aspirationnels → jamais comptés
 *  - dashboardRefreshService.triggerRefresh() post-POST succès
 *  - static getPrefillCount miroir du runtime prefillFromAi()
 *  - OnPush + ChangeDetectorRef.markForCheck() dans subscribe (next/error)
 */
@Component({
  selector: 'app-cce-suspension-be-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    LegalCitationsPipe,
    ToolJurisprudenceCitationsComponent,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './cce-suspension-be-section.component.html',
  styleUrl: './cce-suspension-be-section.component.scss',
})
export class CceSuspensionBeSectionComponent implements OnInit, OnChanges {
  // F-JU-03 — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-IM-57-cce-suspension-be';
  protected readonly brancheActiveForJurisprudence = 'default';

  static readonly TOOL_LABEL = 'RECOURS CCE SUSPENSION (BE)';
  static readonly TOOL_ICON = 'pause_circle';

  static getPrefillCount(input: PrefillCountInput): number {
    return CceSuspensionBePrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'BELGIQUE';
  @Input() aiData?: ImmigrationExtractedData | null;
  @Input() forceExpanded = false;
  @Input() standaloneMode = false;

  collapsed = signal(true);
  loading = signal(false);
  analyzing = signal(false);
  showForm = signal(true);
  result = signal<CceSuspensionBeResponse | null>(null);

  dateNotificationDecision = signal<string | null>(null);
  recoursAnnulationIntroduit = signal<boolean>(false);
  urgenceInvocable = signal<boolean>(false);
  risquePrejudiceGraveDifficilementReparable = signal<boolean>(false);
  mesureEloignementImminente = signal<boolean>(false);

  provenanceDateNotification = signal<'IA' | null>(null);
  provenanceUrgence = signal<'IA' | null>(null);
  provenancePrejudice = signal<'IA' | null>(null);

  isBelgique = computed<boolean>(() => this.workspaceCountry === 'BELGIQUE');

  /** Alertes de cohérence F-IA-03 — sur la date de notification uniquement. */
  coherenceAlerts = computed<Partial<Record<IM57_SuspAlertField, IM57_SuspCoherenceAlert>>>(() => {
    if (this.standaloneMode) return {};
    if (!this.showForm()) return {};
    const alerts: Partial<Record<IM57_SuspAlertField, IM57_SuspCoherenceAlert>> = {};
    const dateAlert = this.buildDateNotificationAlert();
    if (dateAlert) alerts.DATE_NOTIFICATION = dateAlert;
    return alerts;
  });

  constructor(
    private readonly service: CceSuspensionBeService,
    private readonly cdr: ChangeDetectorRef,
    private readonly snackBar: MatSnackBar,
    @Optional() private readonly dashboardRefresh?: CaseDashboardRefreshService,
  ) {}

  ngOnInit(): void {
    if (this.forceExpanded) this.collapsed.set(false);
    if (this.standaloneMode) {
      this.collapsed.set(false);
      this.loading.set(false);
      this.showForm.set(true);
      return;
    }
    if (this.isBelgique() && this.caseFileId) {
      this.load();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['forceExpanded'] && this.forceExpanded) {
      this.collapsed.set(false);
    }
    if (changes['aiData'] && this.isBelgique() && this.showForm() && !this.result()) {
      this.prefillFromAi();
    }
  }

  toggleCollapse(): void {
    this.collapsed.update((v) => !v);
  }

  editMode(): void {
    this.showForm.set(true);
  }

  formValid(): boolean {
    const notif = this.dateNotificationDecision();
    return !!notif && ISO_DATE_RE.test(notif);
  }

  onDateNotificationChange(value: string | null): void {
    this.dateNotificationDecision.set(value || null);
    this.provenanceDateNotification.set(null);
  }

  onRecoursAnnulationChange(value: boolean): void {
    this.recoursAnnulationIntroduit.set(value);
  }

  onUrgenceChange(value: boolean): void {
    this.urgenceInvocable.set(value);
    this.provenanceUrgence.set(null);
  }

  onPrejudiceChange(value: boolean): void {
    this.risquePrejudiceGraveDifficilementReparable.set(value);
    this.provenancePrejudice.set(null);
  }

  onEloignementChange(value: boolean): void {
    this.mesureEloignementImminente.set(value);
  }

  analyze(): void {
    if (!this.formValid()) return;
    const request: CceSuspensionBeRequest = {
      dateNotificationDecision: this.dateNotificationDecision()!,
      recoursAnnulationIntroduit: this.recoursAnnulationIntroduit(),
      urgenceInvocable: this.urgenceInvocable(),
      risquePrejudiceGraveDifficilementReparable: this.risquePrejudiceGraveDifficilementReparable(),
      mesureEloignementImminente: this.mesureEloignementImminente(),
    };
    this.analyzing.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('Analyse recours CCE suspension enregistrée', 'OK', { duration: 2500 });
        if (!this.standaloneMode) {
          this.dashboardRefresh?.triggerRefresh();
        }
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.analyzing.set(false);
        const msg = err?.error?.message || err?.error || "Erreur lors de l'analyse";
        this.snackBar.open(String(msg), 'Fermer', {
          duration: 5000,
          panelClass: 'snack-error',
        });
        this.cdr.markForCheck();
      },
    });
  }

  verdictBannerClass(verdict: CceSuspensionBeVerdict | null | undefined): string {
    switch (verdict) {
      case 'CONDITIONS_REUNIES':         return 'susp-banner susp-banner--success';
      case 'URGENCE_NON_DEMONTREE':      return 'susp-banner susp-banner--warning';
      case 'PREJUDICE_NON_DEMONTRE':     return 'susp-banner susp-banner--warning';
      case 'HORS_DELAI_ANNULATION':      return 'susp-banner susp-banner--warning';
      case 'REORIENTER_EXTREME_URGENCE': return 'susp-banner susp-banner--info';
      default:                           return 'susp-banner';
    }
  }

  verdictChipClass(verdict: CceSuspensionBeVerdict | null | undefined): string {
    switch (verdict) {
      case 'CONDITIONS_REUNIES':         return 'susp-chip susp-chip--success';
      case 'URGENCE_NON_DEMONTREE':      return 'susp-chip susp-chip--warning';
      case 'PREJUDICE_NON_DEMONTRE':     return 'susp-chip susp-chip--warning';
      case 'HORS_DELAI_ANNULATION':      return 'susp-chip susp-chip--warning';
      case 'REORIENTER_EXTREME_URGENCE': return 'susp-chip susp-chip--info';
      default:                           return 'susp-chip';
    }
  }

  verdictIcon(verdict: CceSuspensionBeVerdict | null | undefined): string {
    switch (verdict) {
      case 'CONDITIONS_REUNIES':         return 'gavel';
      case 'URGENCE_NON_DEMONTREE':      return 'hourglass_disabled';
      case 'PREJUDICE_NON_DEMONTRE':     return 'report_problem';
      case 'HORS_DELAI_ANNULATION':      return 'event_busy';
      case 'REORIENTER_EXTREME_URGENCE': return 'bolt';
      default:                           return 'info_outline';
    }
  }

  verdictLabel(verdict: CceSuspensionBeVerdict | null | undefined): string {
    switch (verdict) {
      case 'CONDITIONS_REUNIES':         return 'Conditions réunies';
      case 'URGENCE_NON_DEMONTREE':      return 'Urgence non démontrée';
      case 'PREJUDICE_NON_DEMONTRE':     return 'Préjudice non démontré';
      case 'HORS_DELAI_ANNULATION':      return 'Hors délai d\'annulation';
      case 'REORIENTER_EXTREME_URGENCE': return 'Réorienter extrême urgence';
      default:                           return '';
    }
  }

  /** Format JJ/MM/YYYY depuis une date ISO yyyy-MM-dd. */
  formatDateFr(iso: string | null | undefined): string {
    if (!iso || !ISO_DATE_RE.test(iso)) return '—';
    const [y, m, d] = iso.split('-');
    return `${d}/${m}/${y}`;
  }

  alertBadgeLabel(alert: IM57_SuspCoherenceAlert): string {
    if (alert.severity === 'CRITICAL') return `Alerte critique (${alert.expectedDisplay})`;
    return `Incohérence détectée (${alert.expectedDisplay})`;
  }

  private buildDateNotificationAlert(): IM57_SuspCoherenceAlert | null {
    const userDate = this.dateNotificationDecision();
    if (!userDate) return null;
    const ai = this.aiData;
    const aiDate = ai?.cceSuspensionDateNotification;
    const builder = CoherenceAlertBuilder.forField<IM57_SuspAlertField>('DATE_NOTIFICATION')
      .withSeverity('WARNING');
    if (
      typeof aiDate === 'string' &&
      ISO_DATE_RE.test(aiDate) &&
      aiDate !== userDate
    ) {
      builder.addSource('IA', {
        expectedDisplay: this.formatDateFr(aiDate),
        reason: `Analyse du dossier : notification de la décision le ${this.formatDateFr(aiDate)}`,
      });
    }
    return builder.build();
  }

  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    const input: PrefillCountInput = {
      aiData: this.aiData,
      workspaceCountry: this.workspaceCountry,
    };

    const dateNotif = CceSuspensionBePrefillRules.computeDateNotification(input);
    if (dateNotif !== null && !this.dateNotificationDecision()) {
      this.dateNotificationDecision.set(dateNotif);
      this.provenanceDateNotification.set('IA');
    }

    const urgence = CceSuspensionBePrefillRules.computeUrgence(input);
    if (urgence !== null) {
      this.urgenceInvocable.set(urgence);
      this.provenanceUrgence.set('IA');
    }

    const prejudice = CceSuspensionBePrefillRules.computePrejudiceGrave(input);
    if (prejudice !== null) {
      this.risquePrejudiceGraveDifficilementReparable.set(prejudice);
      this.provenancePrejudice.set('IA');
    }
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.dateNotificationDecision.set(r.dateNotificationDecision ?? null);
        this.recoursAnnulationIntroduit.set(r.recoursAnnulationIntroduit ?? false);
        this.urgenceInvocable.set(r.urgenceInvocable ?? false);
        this.risquePrejudiceGraveDifficilementReparable.set(
          r.risquePrejudiceGraveDifficilementReparable ?? false);
        this.mesureEloignementImminente.set(r.mesureEloignementImminente ?? false);
        this.provenanceDateNotification.set(null);
        this.provenanceUrgence.set(null);
        this.provenancePrejudice.set(null);
        this.showForm.set(false);
        this.loading.set(false);
        this.cdr.markForCheck();
      },
      error: () => {
        // 404 attendu si aucune analyse — on tente le pré-fill IA.
        this.prefillFromAi();
        this.loading.set(false);
        this.cdr.markForCheck();
      },
    });
  }
}
