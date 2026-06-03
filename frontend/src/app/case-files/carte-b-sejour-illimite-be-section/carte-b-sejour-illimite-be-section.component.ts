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

import { CarteBSejourIllimiteBeService } from '../../core/services/carte-b-sejour-illimite-be.service';
import {
  CarteBSejourIllimiteBeRequest,
  CarteBSejourIllimiteBeResponse,
  CarteBSejourIllimiteVerdict,
} from '../../core/models/carte-b-sejour-illimite-be.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { CarteBSejourIllimiteBePrefillRules } from './carte-b-sejour-illimite-be-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';
import { CoherencePopoverTriggerDirective } from '../../shared/coherence-popover/coherence-popover-trigger.directive';
import {
  CoherenceAlert,
  CoherenceAlertSource,
} from '../../shared/coherence-popover/coherence-alert.model';
import { CoherenceAlertBuilder } from '../../shared/coherence-popover/coherence-alert-builder';

const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

export type IM54_CsiAlertField = 'DATE_DEBUT';
export type IM54_CsiAlertSource = CoherenceAlertSource;
export type IM54_CsiCoherenceAlert = CoherenceAlert<IM54_CsiAlertField>;

/**
 * SF-221-02 — Outil décisionnel « Carte B séjour illimité (ressortissant tiers BE) »
 * (F-IM-54-carte-b-sejour-illimite-be).
 *
 * BELGIQUE uniquement — analyseur d'éligibilité au passage carte A → carte B
 * (séjour ILLIMITÉ) après 5 ans (60 mois) de séjour régulier ininterrompu
 * (Loi 15/12/1980 art. 14 — à vérifier par avocat). Visibilité CONTEXTUAL —
 * flag `carte_b_sejour_illimite_detecte`.
 *
 * Pattern miroir : {@link CarteAProrogationBeSectionComponent}.
 *
 * <p>Conforme F-IA-04 :
 *  - standalone OnPush, palette : vert ELIGIBLE, orange DUREE_INSUFFISANTE,
 *    rouge CONTINUITE_ROMPUE / RISQUE_ORDRE_PUBLIC, bleu info A_EXAMINER
 *  - pré-fill IA RÉEL 3 champs (dateDebutSejourRegulier, sejourIninterrompu,
 *    motifSejourStable) ; absencesSuperieuresLimites + ordrePublicRisque
 *    aspirationnels → `PREFILL_COUNT_ALWAYS_ZERO`
 *  - dashboardRefreshService.triggerRefresh() post-POST succès
 *  - static getPrefillCount miroir du runtime prefillFromAi()
 *  - OnPush + ChangeDetectorRef.markForCheck() dans subscribe (next/error)
 */
@Component({
  selector: 'app-carte-b-sejour-illimite-be-section',
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
  templateUrl: './carte-b-sejour-illimite-be-section.component.html',
  styleUrl: './carte-b-sejour-illimite-be-section.component.scss',
})
export class CarteBSejourIllimiteBeSectionComponent implements OnInit, OnChanges {
  // F-JU-03 — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-IM-54-carte-b-sejour-illimite-be';
  protected readonly brancheActiveForJurisprudence = 'default';

  static readonly TOOL_LABEL = 'CARTE B SÉJOUR ILLIMITÉ (BE)';
  static readonly TOOL_ICON = 'all_inclusive';

  static getPrefillCount(input: PrefillCountInput): number {
    return CarteBSejourIllimiteBePrefillRules.computePrefillCount(input);
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
  result = signal<CarteBSejourIllimiteBeResponse | null>(null);

  dateDebutSejourRegulier = signal<string | null>(null);
  sejourIninterrompu = signal<boolean>(false);
  absencesSuperieuresLimites = signal<boolean>(false);
  motifSejourStable = signal<boolean>(false);
  ordrePublicRisque = signal<boolean>(false);

  provenanceDateDebut = signal<'IA' | null>(null);
  provenanceSejourIninterrompu = signal<'IA' | null>(null);
  provenanceMotifStable = signal<'IA' | null>(null);

  isBelgique = computed<boolean>(() => this.workspaceCountry === 'BELGIQUE');

  /** Alertes de cohérence F-IA-03 — sur la date de début de séjour uniquement. */
  coherenceAlerts = computed<Partial<Record<IM54_CsiAlertField, IM54_CsiCoherenceAlert>>>(() => {
    if (this.standaloneMode) return {};
    if (!this.showForm()) return {};
    const alerts: Partial<Record<IM54_CsiAlertField, IM54_CsiCoherenceAlert>> = {};
    const dateAlert = this.buildDateDebutAlert();
    if (dateAlert) alerts.DATE_DEBUT = dateAlert;
    return alerts;
  });

  constructor(
    private readonly service: CarteBSejourIllimiteBeService,
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
    const debut = this.dateDebutSejourRegulier();
    if (!debut || !ISO_DATE_RE.test(debut)) return false;
    return true;
  }

  onDateDebutChange(value: string | null): void {
    this.dateDebutSejourRegulier.set(value || null);
    this.provenanceDateDebut.set(null);
  }

  onSejourIninterrompuChange(value: boolean): void {
    this.sejourIninterrompu.set(value);
    this.provenanceSejourIninterrompu.set(null);
  }

  onAbsencesChange(value: boolean): void {
    this.absencesSuperieuresLimites.set(value);
  }

  onMotifStableChange(value: boolean): void {
    this.motifSejourStable.set(value);
    this.provenanceMotifStable.set(null);
  }

  onOrdrePublicChange(value: boolean): void {
    this.ordrePublicRisque.set(value);
  }

  analyze(): void {
    if (!this.formValid()) return;
    const request: CarteBSejourIllimiteBeRequest = {
      dateDebutSejourRegulier: this.dateDebutSejourRegulier()!,
      sejourIninterrompu: this.sejourIninterrompu(),
      absencesSuperieuresLimites: this.absencesSuperieuresLimites(),
      motifSejourStable: this.motifSejourStable(),
      ordrePublicRisque: this.ordrePublicRisque(),
    };
    this.analyzing.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('Analyse carte B séjour illimité enregistrée', 'OK', { duration: 2500 });
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

  verdictBannerClass(verdict: CarteBSejourIllimiteVerdict | null | undefined): string {
    switch (verdict) {
      case 'ELIGIBLE':            return 'csi-banner csi-banner--success';
      case 'DUREE_INSUFFISANTE':  return 'csi-banner csi-banner--warning';
      case 'CONTINUITE_ROMPUE':   return 'csi-banner csi-banner--danger';
      case 'RISQUE_ORDRE_PUBLIC': return 'csi-banner csi-banner--danger';
      case 'A_EXAMINER':          return 'csi-banner csi-banner--info';
      default:                    return 'csi-banner';
    }
  }

  verdictChipClass(verdict: CarteBSejourIllimiteVerdict | null | undefined): string {
    switch (verdict) {
      case 'ELIGIBLE':            return 'csi-chip csi-chip--success';
      case 'DUREE_INSUFFISANTE':  return 'csi-chip csi-chip--warning';
      case 'CONTINUITE_ROMPUE':   return 'csi-chip csi-chip--danger';
      case 'RISQUE_ORDRE_PUBLIC': return 'csi-chip csi-chip--danger';
      case 'A_EXAMINER':          return 'csi-chip csi-chip--info';
      default:                    return 'csi-chip';
    }
  }

  verdictIcon(verdict: CarteBSejourIllimiteVerdict | null | undefined): string {
    switch (verdict) {
      case 'ELIGIBLE':            return 'verified';
      case 'DUREE_INSUFFISANTE':  return 'hourglass_bottom';
      case 'CONTINUITE_ROMPUE':   return 'link_off';
      case 'RISQUE_ORDRE_PUBLIC': return 'gavel';
      case 'A_EXAMINER':          return 'help_outline';
      default:                    return 'info_outline';
    }
  }

  verdictLabel(verdict: CarteBSejourIllimiteVerdict | null | undefined): string {
    switch (verdict) {
      case 'ELIGIBLE':            return 'Éligible';
      case 'DUREE_INSUFFISANTE':  return 'Durée insuffisante';
      case 'CONTINUITE_ROMPUE':   return 'Continuité rompue';
      case 'RISQUE_ORDRE_PUBLIC': return "Risque d'ordre public";
      case 'A_EXAMINER':          return 'À examiner';
      default:                    return '';
    }
  }

  /** Format JJ/MM/YYYY depuis une date ISO yyyy-MM-dd. */
  formatDateFr(iso: string | null | undefined): string {
    if (!iso || !ISO_DATE_RE.test(iso)) return '—';
    const [y, m, d] = iso.split('-');
    return `${d}/${m}/${y}`;
  }

  alertBadgeLabel(alert: IM54_CsiCoherenceAlert): string {
    if (alert.severity === 'CRITICAL') return `Alerte critique (${alert.expectedDisplay})`;
    return `Incohérence détectée (${alert.expectedDisplay})`;
  }

  private buildDateDebutAlert(): IM54_CsiCoherenceAlert | null {
    const userDate = this.dateDebutSejourRegulier();
    if (!userDate) return null;
    const ai = this.aiData;
    const aiDate = ai?.carteBDateDebutSejour;
    const builder = CoherenceAlertBuilder.forField<IM54_CsiAlertField>('DATE_DEBUT')
      .withSeverity('WARNING');
    if (
      typeof aiDate === 'string' &&
      ISO_DATE_RE.test(aiDate) &&
      aiDate !== userDate
    ) {
      builder.addSource('IA', {
        expectedDisplay: this.formatDateFr(aiDate),
        reason: `Analyse du dossier : début de séjour régulier le ${this.formatDateFr(aiDate)}`,
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

    const dateDebut = CarteBSejourIllimiteBePrefillRules.computeDateDebut(input);
    if (dateDebut !== null && !this.dateDebutSejourRegulier()) {
      this.dateDebutSejourRegulier.set(dateDebut);
      this.provenanceDateDebut.set('IA');
    }

    const ininterrompu = CarteBSejourIllimiteBePrefillRules.computeSejourIninterrompu(input);
    if (ininterrompu !== null) {
      this.sejourIninterrompu.set(ininterrompu);
      this.provenanceSejourIninterrompu.set('IA');
    }

    const motifStable = CarteBSejourIllimiteBePrefillRules.computeMotifStable(input);
    if (motifStable !== null) {
      this.motifSejourStable.set(motifStable);
      this.provenanceMotifStable.set('IA');
    }
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.dateDebutSejourRegulier.set(r.dateDebutSejourRegulier ?? null);
        this.sejourIninterrompu.set(r.sejourIninterrompu ?? false);
        this.absencesSuperieuresLimites.set(r.absencesSuperieuresLimites ?? false);
        this.motifSejourStable.set(r.motifSejourStable ?? false);
        this.ordrePublicRisque.set(r.ordrePublicRisque ?? false);
        this.provenanceDateDebut.set(null);
        this.provenanceSejourIninterrompu.set(null);
        this.provenanceMotifStable.set(null);
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
