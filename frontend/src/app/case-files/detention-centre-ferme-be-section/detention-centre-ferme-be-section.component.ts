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
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';

import { DetentionCentreFermeBeService } from '../../core/services/detention-centre-ferme-be.service';
import {
  DetentionBaseLegale,
  DetentionCentreFermeBeRequest,
  DetentionCentreFermeBeResponse,
  DetentionCentreFermeBeVerdict,
} from '../../core/models/detention-centre-ferme-be.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { DetentionCentreFermeBePrefillRules } from './detention-centre-ferme-be-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';
import { CoherencePopoverTriggerDirective } from '../../shared/coherence-popover/coherence-popover-trigger.directive';
import {
  CoherenceAlert,
  CoherenceAlertSource,
} from '../../shared/coherence-popover/coherence-alert.model';
import { CoherenceAlertBuilder } from '../../shared/coherence-popover/coherence-alert-builder';

const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

export type IM56_DetAlertField = 'DATE_DEBUT';
export type IM56_DetAlertSource = CoherenceAlertSource;
export type IM56_DetCoherenceAlert = CoherenceAlert<IM56_DetAlertField>;

interface BaseLegaleOption {
  value: DetentionBaseLegale;
  label: string;
}

/**
 * SF-221-04 — Outil décisionnel « Détention en centre fermé + requête de mise en
 * liberté (BE) » (F-IM-56-detention-centre-ferme-be).
 *
 * BELGIQUE uniquement — calcule la durée de la détention administrative en centre fermé
 * (art. 7 al. 3 / 27 / 29 / 74/5 Loi 15/12/1980 ; AR 02/08/2002) et cadre la requête de
 * mise en liberté devant la CHAMBRE DU CONSEIL (art. 71 et s. ; fenêtre indicative 5 j —
 * à vérifier par avocat). Visibilité CONTEXTUAL — flag `detention_centre_ferme_detecte`.
 *
 * Une situation fusionnée : la détention ET son recours. La chambre du conseil est une
 * juridiction JUDICIAIRE, DISTINCTE du CCE (F-IM-31 / F-IM-32 / F-IM-57).
 * Pattern miroir : {@link ResidenceLongueDureeUeBeSectionComponent}.
 *
 * <p>Conforme F-IA-04 :
 *  - standalone OnPush, palette : vert REQUETE_OUVERTE / PROLONGATION_A_CONTESTER,
 *    orange REQUETE_TARDIVE, bleu DETENTION_EN_COURS / REQUETE_DEPOSEE
 *  - pré-fill IA RÉEL 3 champs (dateDebutDetention, baseLegaleDetention,
 *    dateNotificationDecisionDetention) ; prolongationNotifiee + dateProlongation +
 *    requeteMiseEnLiberteDeposee aspirationnels → jamais comptés
 *  - dashboardRefreshService.triggerRefresh() post-POST succès
 *  - static getPrefillCount miroir du runtime prefillFromAi()
 *  - OnPush + ChangeDetectorRef.markForCheck() dans subscribe (next/error)
 */
@Component({
  selector: 'app-detention-centre-ferme-be-section',
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
    MatSelectModule,
    LegalCitationsPipe,
    ToolJurisprudenceCitationsComponent,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './detention-centre-ferme-be-section.component.html',
  styleUrl: './detention-centre-ferme-be-section.component.scss',
})
export class DetentionCentreFermeBeSectionComponent implements OnInit, OnChanges {
  // F-JU-03 — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-IM-56-detention-centre-ferme-be';
  protected readonly brancheActiveForJurisprudence = 'default';

  static readonly TOOL_LABEL = 'DÉTENTION CENTRE FERMÉ (BE)';
  static readonly TOOL_ICON = 'lock';

  static getPrefillCount(input: PrefillCountInput): number {
    return DetentionCentreFermeBePrefillRules.computePrefillCount(input);
  }

  readonly basesLegales: readonly BaseLegaleOption[] = [
    { value: 'ART_7', label: 'Art. 7 al. 3 — maintien en vue de l\'éloignement' },
    { value: 'ART_27', label: 'Art. 27 — exécution forcée de l\'éloignement' },
    { value: 'ART_29', label: 'Art. 29 — refoulement' },
    { value: 'ART_74_5', label: 'Art. 74/5 — maintien à la frontière' },
    { value: 'AUTRE', label: 'Autre base légale de maintien' },
  ];

  @Input() caseFileId!: string;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'BELGIQUE';
  @Input() aiData?: ImmigrationExtractedData | null;
  @Input() forceExpanded = false;
  @Input() standaloneMode = false;

  collapsed = signal(true);
  loading = signal(false);
  analyzing = signal(false);
  showForm = signal(true);
  result = signal<DetentionCentreFermeBeResponse | null>(null);

  dateDebutDetention = signal<string | null>(null);
  baseLegaleDetention = signal<DetentionBaseLegale>('ART_7');
  prolongationNotifiee = signal<boolean>(false);
  dateProlongation = signal<string | null>(null);
  requeteMiseEnLiberteDeposee = signal<boolean>(false);
  dateNotificationDecisionDetention = signal<string | null>(null);

  provenanceDateDebut = signal<'IA' | null>(null);
  provenanceBaseLegale = signal<'IA' | null>(null);
  provenanceDateNotification = signal<'IA' | null>(null);

  isBelgique = computed<boolean>(() => this.workspaceCountry === 'BELGIQUE');

  /** Alertes de cohérence F-IA-03 — sur la date de début de détention uniquement. */
  coherenceAlerts = computed<Partial<Record<IM56_DetAlertField, IM56_DetCoherenceAlert>>>(() => {
    if (this.standaloneMode) return {};
    if (!this.showForm()) return {};
    const alerts: Partial<Record<IM56_DetAlertField, IM56_DetCoherenceAlert>> = {};
    const dateAlert = this.buildDateDebutAlert();
    if (dateAlert) alerts.DATE_DEBUT = dateAlert;
    return alerts;
  });

  constructor(
    private readonly service: DetentionCentreFermeBeService,
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
    const debut = this.dateDebutDetention();
    if (!debut || !ISO_DATE_RE.test(debut)) return false;
    // Validations conditionnelles miroir backend.
    if (this.prolongationNotifiee() && !this.dateProlongation()) return false;
    if (this.requeteMiseEnLiberteDeposee() && !this.dateNotificationDecisionDetention()) return false;
    return true;
  }

  onDateDebutChange(value: string | null): void {
    this.dateDebutDetention.set(value || null);
    this.provenanceDateDebut.set(null);
  }

  onBaseLegaleChange(value: DetentionBaseLegale): void {
    this.baseLegaleDetention.set(value);
    this.provenanceBaseLegale.set(null);
  }

  onProlongationNotifieeChange(value: boolean): void {
    this.prolongationNotifiee.set(value);
    if (!value) this.dateProlongation.set(null);
  }

  onDateProlongationChange(value: string | null): void {
    this.dateProlongation.set(value || null);
  }

  onRequeteDeposeeChange(value: boolean): void {
    this.requeteMiseEnLiberteDeposee.set(value);
    if (!value) this.dateNotificationDecisionDetention.set(null);
  }

  onDateNotificationChange(value: string | null): void {
    this.dateNotificationDecisionDetention.set(value || null);
    this.provenanceDateNotification.set(null);
  }

  baseLegaleLabel(value: DetentionBaseLegale | null | undefined): string {
    return this.basesLegales.find((b) => b.value === value)?.label ?? '—';
  }

  analyze(): void {
    if (!this.formValid()) return;
    const request: DetentionCentreFermeBeRequest = {
      dateDebutDetention: this.dateDebutDetention()!,
      baseLegaleDetention: this.baseLegaleDetention(),
      prolongationNotifiee: this.prolongationNotifiee(),
      dateProlongation: this.prolongationNotifiee() ? this.dateProlongation() : null,
      requeteMiseEnLiberteDeposee: this.requeteMiseEnLiberteDeposee(),
      dateNotificationDecisionDetention: this.dateNotificationDecisionDetention(),
    };
    this.analyzing.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('Analyse détention en centre fermé enregistrée', 'OK', { duration: 2500 });
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

  verdictBannerClass(verdict: DetentionCentreFermeBeVerdict | null | undefined): string {
    switch (verdict) {
      case 'REQUETE_OUVERTE':           return 'det-banner det-banner--success';
      case 'PROLONGATION_A_CONTESTER':  return 'det-banner det-banner--success';
      case 'REQUETE_TARDIVE':           return 'det-banner det-banner--warning';
      case 'DETENTION_EN_COURS':        return 'det-banner det-banner--info';
      case 'REQUETE_DEPOSEE':           return 'det-banner det-banner--info';
      default:                          return 'det-banner';
    }
  }

  verdictChipClass(verdict: DetentionCentreFermeBeVerdict | null | undefined): string {
    switch (verdict) {
      case 'REQUETE_OUVERTE':           return 'det-chip det-chip--success';
      case 'PROLONGATION_A_CONTESTER':  return 'det-chip det-chip--success';
      case 'REQUETE_TARDIVE':           return 'det-chip det-chip--warning';
      case 'DETENTION_EN_COURS':        return 'det-chip det-chip--info';
      case 'REQUETE_DEPOSEE':           return 'det-chip det-chip--info';
      default:                          return 'det-chip';
    }
  }

  verdictIcon(verdict: DetentionCentreFermeBeVerdict | null | undefined): string {
    switch (verdict) {
      case 'REQUETE_OUVERTE':           return 'gavel';
      case 'PROLONGATION_A_CONTESTER':  return 'event_repeat';
      case 'REQUETE_TARDIVE':           return 'hourglass_disabled';
      case 'DETENTION_EN_COURS':        return 'lock';
      case 'REQUETE_DEPOSEE':           return 'task_alt';
      default:                          return 'info_outline';
    }
  }

  verdictLabel(verdict: DetentionCentreFermeBeVerdict | null | undefined): string {
    switch (verdict) {
      case 'REQUETE_OUVERTE':           return 'Requête ouverte';
      case 'PROLONGATION_A_CONTESTER':  return 'Prolongation à contester';
      case 'REQUETE_TARDIVE':           return 'Requête tardive';
      case 'DETENTION_EN_COURS':        return 'Détention en cours';
      case 'REQUETE_DEPOSEE':           return 'Requête déposée';
      default:                          return '';
    }
  }

  /** Format JJ/MM/YYYY depuis une date ISO yyyy-MM-dd. */
  formatDateFr(iso: string | null | undefined): string {
    if (!iso || !ISO_DATE_RE.test(iso)) return '—';
    const [y, m, d] = iso.split('-');
    return `${d}/${m}/${y}`;
  }

  alertBadgeLabel(alert: IM56_DetCoherenceAlert): string {
    if (alert.severity === 'CRITICAL') return `Alerte critique (${alert.expectedDisplay})`;
    return `Incohérence détectée (${alert.expectedDisplay})`;
  }

  private buildDateDebutAlert(): IM56_DetCoherenceAlert | null {
    const userDate = this.dateDebutDetention();
    if (!userDate) return null;
    const ai = this.aiData;
    const aiDate = ai?.detentionDateDebut;
    const builder = CoherenceAlertBuilder.forField<IM56_DetAlertField>('DATE_DEBUT')
      .withSeverity('WARNING');
    if (
      typeof aiDate === 'string' &&
      ISO_DATE_RE.test(aiDate) &&
      aiDate !== userDate
    ) {
      builder.addSource('IA', {
        expectedDisplay: this.formatDateFr(aiDate),
        reason: `Analyse du dossier : début de détention le ${this.formatDateFr(aiDate)}`,
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

    const dateDebut = DetentionCentreFermeBePrefillRules.computeDateDebut(input);
    if (dateDebut !== null && !this.dateDebutDetention()) {
      this.dateDebutDetention.set(dateDebut);
      this.provenanceDateDebut.set('IA');
    }

    const baseLegale = DetentionCentreFermeBePrefillRules.computeBaseLegale(input);
    if (baseLegale !== null) {
      this.baseLegaleDetention.set(baseLegale);
      this.provenanceBaseLegale.set('IA');
    }

    const dateNotif = DetentionCentreFermeBePrefillRules.computeDateNotification(input);
    if (dateNotif !== null && !this.dateNotificationDecisionDetention()) {
      this.dateNotificationDecisionDetention.set(dateNotif);
      this.provenanceDateNotification.set('IA');
    }
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.dateDebutDetention.set(r.dateDebutDetention ?? null);
        this.baseLegaleDetention.set(r.baseLegaleDetention ?? 'ART_7');
        this.prolongationNotifiee.set(r.prolongationNotifiee ?? false);
        this.dateProlongation.set(r.dateProlongation ?? null);
        this.requeteMiseEnLiberteDeposee.set(r.requeteMiseEnLiberteDeposee ?? false);
        this.dateNotificationDecisionDetention.set(r.dateNotificationDecisionDetention ?? null);
        this.provenanceDateDebut.set(null);
        this.provenanceBaseLegale.set(null);
        this.provenanceDateNotification.set(null);
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
