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
import { MatListModule } from '@angular/material/list';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatRadioModule } from '@angular/material/radio';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';

import { Regroupement10terBeService } from '../../core/services/regroupement-10ter-be.service';
import {
  REGROUPEMENT_10TER_LIENS_FAMILIAUX,
  REGROUPEMENT_10TER_TYPES_CARTE,
  Regroupement10terBeRequest,
  Regroupement10terBeResponse,
  Regroupement10terLienFamilial,
  Regroupement10terLienFamilialOption,
  Regroupement10terTypeCarte,
  Regroupement10terTypeCarteOption,
  Regroupement10terVerdict,
  SEUIL_RESSOURCES_DEFAULT,
} from '../../core/models/regroupement-10ter-be.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { Regroupement10terBePrefillRules } from './regroupement-10ter-be-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';
import { CoherencePopoverTriggerDirective } from '../../shared/coherence-popover/coherence-popover-trigger.directive';
import {
  CoherenceAlert,
  CoherenceAlertSource,
} from '../../shared/coherence-popover/coherence-alert.model';
import { CoherenceAlertBuilder } from '../../shared/coherence-popover/coherence-alert-builder';

export type IM26_10terAlertField = 'REVENUS_MENSUELS';

export type IM26_10terAlertSource = CoherenceAlertSource;
export type IM26_10terCoherenceAlert = CoherenceAlert<IM26_10terAlertField>;

/**
 * SF-215-04 — Outil décisionnel « Regroupement familial art. 10ter (BE) »
 * (F-IM-26-regroupement-10ter-be).
 *
 * BELGIQUE uniquement — scoring d'éligibilité pour le regroupement familial
 * sous l'art. 10ter de la Loi du 15/12/1980, avec seuil de ressources
 * mensuelles fixé à 120 % du RIS (AR du 17/05/2007 — seuil opérationnel
 * 1 500 €/mois). Visibilité CONTEXTUAL — flag `regroupement_10ter_detecte`.
 *
 * Pattern miroir : {@link SinglePermitBeSectionComponent} (F-IM-25-single-permit-be,
 * SF-215-02) + {@link Belgian40terSectionComponent} (F-IM-14, scoring/verdict/
 * différentiel signé).
 *
 * <p>Conforme F-IA-04 :
 *  - standalone OnPush, palette : vert ELIGIBLE, orange SOUS_RESERVE,
 *    rouge INELIGIBLE
 *  - pré-fill IA 4 champs (lienFamilial, typeCarte, revenusMensuels, dureeSejour)
 *  - dashboardRefreshService.triggerRefresh() post-POST succès
 *  - static getPrefillCount miroir du runtime prefillFromAi()
 *  - OnPush + ChangeDetectorRef.markForCheck() dans subscribe (next/error)
 *    cf. feedback_onpush_subscribe_markforcheck
 */
@Component({
  selector: 'app-regroupement-10ter-be-section',
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
    MatListModule,
    MatProgressBarModule,
    MatProgressSpinnerModule,
    MatRadioModule,
    MatSelectModule,
    LegalCitationsPipe,
    ToolJurisprudenceCitationsComponent,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './regroupement-10ter-be-section.component.html',
  styleUrl: './regroupement-10ter-be-section.component.scss',
})
export class Regroupement10terBeSectionComponent implements OnInit, OnChanges {
  // F-JU-03 SF-JU-03-99c — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-IM-26-regroupement-10ter-be';
  protected readonly brancheActiveForJurisprudence = 'default';

  static readonly TOOL_LABEL = 'REGROUPEMENT 10TER (BE)';
  static readonly TOOL_ICON = 'family_restroom';

  static getPrefillCount(input: PrefillCountInput): number {
    return Regroupement10terBePrefillRules.computePrefillCount(input);
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
  result = signal<Regroupement10terBeResponse | null>(null);

  lienFamilial = signal<Regroupement10terLienFamilial | null>(null);
  typeCarteRegroupant = signal<Regroupement10terTypeCarte | null>(null);
  revenusMensuelsNetsRegroupant = signal<number | null>(null);
  dureeSejour = signal<number | null>(null);
  logementConforme = signal<boolean>(false);
  assuranceMaladie = signal<boolean>(false);
  menaceOrdrePublic = signal<boolean>(false);

  provenanceLienFamilial = signal<'IA' | null>(null);
  provenanceTypeCarte = signal<'IA' | null>(null);
  provenanceRevenus = signal<'IA' | null>(null);
  provenanceDureeSejour = signal<'IA' | null>(null);

  readonly liensFamiliaux: ReadonlyArray<Regroupement10terLienFamilialOption> =
    REGROUPEMENT_10TER_LIENS_FAMILIAUX;

  readonly typesCarte: ReadonlyArray<Regroupement10terTypeCarteOption> =
    REGROUPEMENT_10TER_TYPES_CARTE;

  readonly seuilRessources = SEUIL_RESSOURCES_DEFAULT;

  isBelgique = computed<boolean>(() => this.workspaceCountry === 'BELGIQUE');

  /**
   * Alertes de cohérence F-IA-03 exposées par field (revenus uniquement —
   * les autres champs sont des enums whitelistés strictement pré-remplis).
   */
  coherenceAlerts = computed<Partial<Record<IM26_10terAlertField, IM26_10terCoherenceAlert>>>(() => {
    if (this.standaloneMode) return {};
    if (!this.showForm()) return {};
    const alerts: Partial<Record<IM26_10terAlertField, IM26_10terCoherenceAlert>> = {};
    const revenus = this.buildRevenusAlert();
    if (revenus) alerts.REVENUS_MENSUELS = revenus;
    return alerts;
  });

  constructor(
    private readonly service: Regroupement10terBeService,
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
    if (!this.lienFamilial()) return false;
    if (!this.typeCarteRegroupant()) return false;
    const rev = this.revenusMensuelsNetsRegroupant();
    if (rev === null || rev === undefined || isNaN(rev) || rev < 0 || rev > 100_000) return false;
    const duree = this.dureeSejour();
    if (duree === null || duree === undefined || isNaN(duree) || duree < 0 || duree > 600) return false;
    return true;
  }

  onLienFamilialChange(value: Regroupement10terLienFamilial | null): void {
    this.lienFamilial.set(value);
    this.provenanceLienFamilial.set(null);
  }

  onTypeCarteChange(value: Regroupement10terTypeCarte | null): void {
    this.typeCarteRegroupant.set(value);
    this.provenanceTypeCarte.set(null);
  }

  onRevenusChange(value: number | null | undefined): void {
    this.revenusMensuelsNetsRegroupant.set(value ?? null);
    this.provenanceRevenus.set(null);
  }

  onDureeSejourChange(value: number | null | undefined): void {
    this.dureeSejour.set(value ?? null);
    this.provenanceDureeSejour.set(null);
  }

  onLogementChange(value: boolean): void {
    this.logementConforme.set(value);
  }

  onAssuranceMaladieChange(value: boolean): void {
    this.assuranceMaladie.set(value);
  }

  onMenaceOrdrePublicChange(value: boolean): void {
    this.menaceOrdrePublic.set(value);
  }

  analyze(): void {
    if (!this.formValid()) return;
    const request: Regroupement10terBeRequest = {
      lienFamilial: this.lienFamilial()!,
      typeCarteRegroupant: this.typeCarteRegroupant()!,
      revenusMensuelsNetsRegroupant: this.revenusMensuelsNetsRegroupant()!,
      dureeSejour: this.dureeSejour()!,
      logementConforme: this.logementConforme(),
      assuranceMaladie: this.assuranceMaladie(),
      menaceOrdrePublic: this.menaceOrdrePublic(),
    };
    this.analyzing.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('Analyse regroupement 10ter BE enregistrée', 'OK', { duration: 2500 });
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

  verdictClass(verdict: Regroupement10terVerdict | null | undefined): string {
    switch (verdict) {
      case 'ELIGIBLE':     return 're10ter-banner re10ter-banner--success';
      case 'SOUS_RESERVE': return 're10ter-banner re10ter-banner--warning';
      case 'INELIGIBLE':   return 're10ter-banner re10ter-banner--danger';
      default:             return 're10ter-banner';
    }
  }

  verdictChipClass(verdict: Regroupement10terVerdict | null | undefined): string {
    switch (verdict) {
      case 'ELIGIBLE':     return 're10ter-chip re10ter-chip--success';
      case 'SOUS_RESERVE': return 're10ter-chip re10ter-chip--warning';
      case 'INELIGIBLE':   return 're10ter-chip re10ter-chip--danger';
      default:             return 're10ter-chip';
    }
  }

  verdictIcon(verdict: Regroupement10terVerdict | null | undefined): string {
    switch (verdict) {
      case 'ELIGIBLE':     return 'check_circle';
      case 'SOUS_RESERVE': return 'warning';
      case 'INELIGIBLE':   return 'block';
      default:             return 'info_outline';
    }
  }

  verdictLabel(verdict: Regroupement10terVerdict | null | undefined): string {
    switch (verdict) {
      case 'ELIGIBLE':     return 'Éligible';
      case 'SOUS_RESERVE': return 'Sous réserve';
      case 'INELIGIBLE':   return 'Inéligible';
      default:             return '';
    }
  }

  /** Barre Material — `primary` (vert/navy), `accent` (orange), `warn` (rouge). */
  scoreBarColor(verdict: Regroupement10terVerdict | null | undefined): 'primary' | 'accent' | 'warn' {
    switch (verdict) {
      case 'ELIGIBLE':     return 'primary';
      case 'SOUS_RESERVE': return 'accent';
      case 'INELIGIBLE':   return 'warn';
      default:             return 'primary';
    }
  }

  /** Classe CSS du badge différentiel — vert si ≥ 0, rouge si négatif. */
  differentielBadgeClass(diff: number | null | undefined): string {
    if (diff === null || diff === undefined) return 're10ter-diff';
    return diff >= 0
      ? 're10ter-diff re10ter-diff--positive'
      : 're10ter-diff re10ter-diff--negative';
  }

  /** Texte humain du différentiel signé (en €/mois). */
  differentielText(diff: number | null | undefined): string {
    if (diff === null || diff === undefined) return '';
    if (diff >= 0) {
      return `+${diff} € / mois au-dessus du seuil`;
    }
    // diff négatif — déjà signé, on n'ajoute pas de signe en double.
    return `${diff} € / mois en-dessous du seuil`;
  }

  lienFamilialLabel(code: Regroupement10terLienFamilial | string | null | undefined): string {
    if (!code) return '';
    return this.liensFamiliaux.find((l) => l.code === code)?.label ?? String(code);
  }

  typeCarteLabel(code: Regroupement10terTypeCarte | string | null | undefined): string {
    if (!code) return '';
    return this.typesCarte.find((t) => t.code === code)?.label ?? String(code);
  }

  alertTooltip(alert: IM26_10terCoherenceAlert): string {
    return alert.reason;
  }

  alertBadgeLabel(alert: IM26_10terCoherenceAlert): string {
    if (alert.severity === 'CRITICAL') return `Alerte critique (${alert.expectedDisplay})`;
    return `Incohérence détectée (${alert.expectedDisplay})`;
  }

  private buildRevenusAlert(): IM26_10terCoherenceAlert | null {
    const userRevenus = this.revenusMensuelsNetsRegroupant();
    if (userRevenus === null || userRevenus === undefined) return null;
    const builder = CoherenceAlertBuilder.forField<IM26_10terAlertField>('REVENUS_MENSUELS')
      .withSeverity('WARNING');
    const ai = this.aiData;
    const aiRevenus = ai?.be10terRevenusMensuels;
    if (
      typeof aiRevenus === 'number' &&
      !isNaN(aiRevenus) &&
      aiRevenus > 0 &&
      aiRevenus !== userRevenus
    ) {
      builder.addSource('IA', {
        expectedDisplay: `${aiRevenus} €/mois`,
        reason: `Analyse du dossier : revenus mensuels nets = ${aiRevenus} €`,
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

    const lien = Regroupement10terBePrefillRules.computeLienFamilial(input);
    if (lien !== null && !this.lienFamilial()) {
      this.lienFamilial.set(lien);
      this.provenanceLienFamilial.set('IA');
    }

    const carte = Regroupement10terBePrefillRules.computeTypeCarteRegroupant(input);
    if (carte !== null && !this.typeCarteRegroupant()) {
      this.typeCarteRegroupant.set(carte);
      this.provenanceTypeCarte.set('IA');
    }

    const rev = Regroupement10terBePrefillRules.computeRevenusMensuelsNetsRegroupant(input);
    if (rev !== null && this.revenusMensuelsNetsRegroupant() === null) {
      this.revenusMensuelsNetsRegroupant.set(rev);
      this.provenanceRevenus.set('IA');
    }

    const duree = Regroupement10terBePrefillRules.computeDureeSejour(input);
    if (duree !== null && this.dureeSejour() === null) {
      this.dureeSejour.set(duree);
      this.provenanceDureeSejour.set('IA');
    }
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.lienFamilial.set((r.lienFamilial as Regroupement10terLienFamilial) ?? null);
        this.typeCarteRegroupant.set((r.typeCarteRegroupant as Regroupement10terTypeCarte) ?? null);
        this.revenusMensuelsNetsRegroupant.set(r.revenusMensuelsNetsRegroupant ?? null);
        this.dureeSejour.set(r.dureeSejour ?? null);
        this.logementConforme.set(r.logementConforme ?? false);
        this.assuranceMaladie.set(r.assuranceMaladie ?? false);
        this.menaceOrdrePublic.set(r.menaceOrdrePublic ?? false);
        this.provenanceLienFamilial.set(null);
        this.provenanceTypeCarte.set(null);
        this.provenanceRevenus.set(null);
        this.provenanceDureeSejour.set(null);
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
