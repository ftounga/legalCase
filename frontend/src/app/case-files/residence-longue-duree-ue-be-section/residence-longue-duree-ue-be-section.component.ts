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

import { ResidenceLongueDureeUeBeService } from '../../core/services/residence-longue-duree-ue-be.service';
import {
  ResidenceLongueDureeUeBeRequest,
  ResidenceLongueDureeUeBeResponse,
  ResidenceLongueDureeUeVerdict,
} from '../../core/models/residence-longue-duree-ue-be.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { ResidenceLongueDureeUeBePrefillRules } from './residence-longue-duree-ue-be-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';
import { CoherencePopoverTriggerDirective } from '../../shared/coherence-popover/coherence-popover-trigger.directive';
import {
  CoherenceAlert,
  CoherenceAlertSource,
} from '../../shared/coherence-popover/coherence-alert.model';
import { CoherenceAlertBuilder } from '../../shared/coherence-popover/coherence-alert-builder';

const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

export type IM55_RlueAlertField = 'DATE_DEBUT';
export type IM55_RlueAlertSource = CoherenceAlertSource;
export type IM55_RlueCoherenceAlert = CoherenceAlert<IM55_RlueAlertField>;

/**
 * SF-221-03 — Outil décisionnel « Résident longue durée UE (BE) »
 * (F-IM-55-residence-longue-duree-ue-be).
 *
 * BELGIQUE uniquement — analyseur d'éligibilité au statut de résident longue durée
 * UE après 5 ans (60 mois) de séjour légal ininterrompu + ressources stables/
 * suffisantes + assurance maladie + condition d'intégration (art. 15bis Loi
 * 15/12/1980 — directive 2003/109/CE — à vérifier par avocat). Visibilité
 * CONTEXTUAL — flag `residence_longue_duree_ue_detecte`.
 *
 * DISTINCT de F-IM-54 (carte B, séjour ILLIMITÉ NATIONAL sans mobilité UE).
 * Pattern miroir : {@link CarteBSejourIllimiteBeSectionComponent}.
 *
 * <p>Conforme F-IA-04 :
 *  - standalone OnPush, palette : vert ELIGIBLE, orange DUREE_INSUFFISANTE /
 *    CONDITIONS_MATERIELLES_NON_REUNIES, rouge CONTINUITE_ROMPUE, bleu A_EXAMINER
 *  - pré-fill IA RÉEL 4 champs (dateDebutSejourLegal, ressourcesStablesSuffisantes,
 *    assuranceMaladie, conditionIntegrationRemplie) ; sejourLegalIninterrompu +
 *    absencesHorsUeExcessives aspirationnels → jamais comptés
 *  - dashboardRefreshService.triggerRefresh() post-POST succès
 *  - static getPrefillCount miroir du runtime prefillFromAi()
 *  - OnPush + ChangeDetectorRef.markForCheck() dans subscribe (next/error)
 */
@Component({
  selector: 'app-residence-longue-duree-ue-be-section',
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
  templateUrl: './residence-longue-duree-ue-be-section.component.html',
  styleUrl: './residence-longue-duree-ue-be-section.component.scss',
})
export class ResidenceLongueDureeUeBeSectionComponent implements OnInit, OnChanges {
  // F-JU-03 — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-IM-55-residence-longue-duree-ue-be';
  protected readonly brancheActiveForJurisprudence = 'default';

  static readonly TOOL_LABEL = 'RÉSIDENT LONGUE DURÉE UE (BE)';
  static readonly TOOL_ICON = 'public';

  static getPrefillCount(input: PrefillCountInput): number {
    return ResidenceLongueDureeUeBePrefillRules.computePrefillCount(input);
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
  result = signal<ResidenceLongueDureeUeBeResponse | null>(null);

  dateDebutSejourLegal = signal<string | null>(null);
  sejourLegalIninterrompu = signal<boolean>(false);
  ressourcesStablesSuffisantes = signal<boolean>(false);
  assuranceMaladie = signal<boolean>(false);
  conditionIntegrationRemplie = signal<boolean>(false);
  absencesHorsUeExcessives = signal<boolean>(false);

  provenanceDateDebut = signal<'IA' | null>(null);
  provenanceRessources = signal<'IA' | null>(null);
  provenanceAssurance = signal<'IA' | null>(null);
  provenanceIntegration = signal<'IA' | null>(null);

  isBelgique = computed<boolean>(() => this.workspaceCountry === 'BELGIQUE');

  /** Alertes de cohérence F-IA-03 — sur la date de début de séjour uniquement. */
  coherenceAlerts = computed<Partial<Record<IM55_RlueAlertField, IM55_RlueCoherenceAlert>>>(() => {
    if (this.standaloneMode) return {};
    if (!this.showForm()) return {};
    const alerts: Partial<Record<IM55_RlueAlertField, IM55_RlueCoherenceAlert>> = {};
    const dateAlert = this.buildDateDebutAlert();
    if (dateAlert) alerts.DATE_DEBUT = dateAlert;
    return alerts;
  });

  constructor(
    private readonly service: ResidenceLongueDureeUeBeService,
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
    const debut = this.dateDebutSejourLegal();
    if (!debut || !ISO_DATE_RE.test(debut)) return false;
    return true;
  }

  onDateDebutChange(value: string | null): void {
    this.dateDebutSejourLegal.set(value || null);
    this.provenanceDateDebut.set(null);
  }

  onSejourIninterrompuChange(value: boolean): void {
    this.sejourLegalIninterrompu.set(value);
  }

  onRessourcesChange(value: boolean): void {
    this.ressourcesStablesSuffisantes.set(value);
    this.provenanceRessources.set(null);
  }

  onAssuranceChange(value: boolean): void {
    this.assuranceMaladie.set(value);
    this.provenanceAssurance.set(null);
  }

  onIntegrationChange(value: boolean): void {
    this.conditionIntegrationRemplie.set(value);
    this.provenanceIntegration.set(null);
  }

  onAbsencesChange(value: boolean): void {
    this.absencesHorsUeExcessives.set(value);
  }

  analyze(): void {
    if (!this.formValid()) return;
    const request: ResidenceLongueDureeUeBeRequest = {
      dateDebutSejourLegal: this.dateDebutSejourLegal()!,
      sejourLegalIninterrompu: this.sejourLegalIninterrompu(),
      ressourcesStablesSuffisantes: this.ressourcesStablesSuffisantes(),
      assuranceMaladie: this.assuranceMaladie(),
      conditionIntegrationRemplie: this.conditionIntegrationRemplie(),
      absencesHorsUeExcessives: this.absencesHorsUeExcessives(),
    };
    this.analyzing.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('Analyse résident longue durée UE enregistrée', 'OK', { duration: 2500 });
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

  verdictBannerClass(verdict: ResidenceLongueDureeUeVerdict | null | undefined): string {
    switch (verdict) {
      case 'ELIGIBLE':                            return 'rlue-banner rlue-banner--success';
      case 'DUREE_INSUFFISANTE':                  return 'rlue-banner rlue-banner--warning';
      case 'CONDITIONS_MATERIELLES_NON_REUNIES':  return 'rlue-banner rlue-banner--warning';
      case 'CONTINUITE_ROMPUE':                   return 'rlue-banner rlue-banner--danger';
      case 'A_EXAMINER':                          return 'rlue-banner rlue-banner--info';
      default:                                    return 'rlue-banner';
    }
  }

  verdictChipClass(verdict: ResidenceLongueDureeUeVerdict | null | undefined): string {
    switch (verdict) {
      case 'ELIGIBLE':                            return 'rlue-chip rlue-chip--success';
      case 'DUREE_INSUFFISANTE':                  return 'rlue-chip rlue-chip--warning';
      case 'CONDITIONS_MATERIELLES_NON_REUNIES':  return 'rlue-chip rlue-chip--warning';
      case 'CONTINUITE_ROMPUE':                   return 'rlue-chip rlue-chip--danger';
      case 'A_EXAMINER':                          return 'rlue-chip rlue-chip--info';
      default:                                    return 'rlue-chip';
    }
  }

  verdictIcon(verdict: ResidenceLongueDureeUeVerdict | null | undefined): string {
    switch (verdict) {
      case 'ELIGIBLE':                            return 'verified';
      case 'DUREE_INSUFFISANTE':                  return 'hourglass_bottom';
      case 'CONDITIONS_MATERIELLES_NON_REUNIES':  return 'rule';
      case 'CONTINUITE_ROMPUE':                   return 'link_off';
      case 'A_EXAMINER':                          return 'help_outline';
      default:                                    return 'info_outline';
    }
  }

  verdictLabel(verdict: ResidenceLongueDureeUeVerdict | null | undefined): string {
    switch (verdict) {
      case 'ELIGIBLE':                            return 'Éligible';
      case 'DUREE_INSUFFISANTE':                  return 'Durée insuffisante';
      case 'CONDITIONS_MATERIELLES_NON_REUNIES':  return 'Conditions matérielles non réunies';
      case 'CONTINUITE_ROMPUE':                   return 'Continuité rompue';
      case 'A_EXAMINER':                          return 'À examiner';
      default:                                    return '';
    }
  }

  /** Format JJ/MM/YYYY depuis une date ISO yyyy-MM-dd. */
  formatDateFr(iso: string | null | undefined): string {
    if (!iso || !ISO_DATE_RE.test(iso)) return '—';
    const [y, m, d] = iso.split('-');
    return `${d}/${m}/${y}`;
  }

  alertBadgeLabel(alert: IM55_RlueCoherenceAlert): string {
    if (alert.severity === 'CRITICAL') return `Alerte critique (${alert.expectedDisplay})`;
    return `Incohérence détectée (${alert.expectedDisplay})`;
  }

  private buildDateDebutAlert(): IM55_RlueCoherenceAlert | null {
    const userDate = this.dateDebutSejourLegal();
    if (!userDate) return null;
    const ai = this.aiData;
    const aiDate = ai?.rlueDateDebutSejour;
    const builder = CoherenceAlertBuilder.forField<IM55_RlueAlertField>('DATE_DEBUT')
      .withSeverity('WARNING');
    if (
      typeof aiDate === 'string' &&
      ISO_DATE_RE.test(aiDate) &&
      aiDate !== userDate
    ) {
      builder.addSource('IA', {
        expectedDisplay: this.formatDateFr(aiDate),
        reason: `Analyse du dossier : début de séjour légal le ${this.formatDateFr(aiDate)}`,
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

    const dateDebut = ResidenceLongueDureeUeBePrefillRules.computeDateDebut(input);
    if (dateDebut !== null && !this.dateDebutSejourLegal()) {
      this.dateDebutSejourLegal.set(dateDebut);
      this.provenanceDateDebut.set('IA');
    }

    const ressources = ResidenceLongueDureeUeBePrefillRules.computeRessources(input);
    if (ressources !== null) {
      this.ressourcesStablesSuffisantes.set(ressources);
      this.provenanceRessources.set('IA');
    }

    const assurance = ResidenceLongueDureeUeBePrefillRules.computeAssurance(input);
    if (assurance !== null) {
      this.assuranceMaladie.set(assurance);
      this.provenanceAssurance.set('IA');
    }

    const integration = ResidenceLongueDureeUeBePrefillRules.computeIntegration(input);
    if (integration !== null) {
      this.conditionIntegrationRemplie.set(integration);
      this.provenanceIntegration.set('IA');
    }
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.dateDebutSejourLegal.set(r.dateDebutSejourLegal ?? null);
        this.sejourLegalIninterrompu.set(r.sejourLegalIninterrompu ?? false);
        this.ressourcesStablesSuffisantes.set(r.ressourcesStablesSuffisantes ?? false);
        this.assuranceMaladie.set(r.assuranceMaladie ?? false);
        this.conditionIntegrationRemplie.set(r.conditionIntegrationRemplie ?? false);
        this.absencesHorsUeExcessives.set(r.absencesHorsUeExcessives ?? false);
        this.provenanceDateDebut.set(null);
        this.provenanceRessources.set(null);
        this.provenanceAssurance.set(null);
        this.provenanceIntegration.set(null);
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
