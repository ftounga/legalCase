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
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';

import { Regroupement10bisBeService } from '../../core/services/regroupement-10bis-be.service';
import {
  REGROUPEMENT_10BIS_LIENS_FAMILIAUX,
  REGROUPEMENT_10BIS_TYPES_CARTE,
  Regroupement10bisBeRequest,
  Regroupement10bisBeResponse,
  Regroupement10bisLienFamilial,
  Regroupement10bisLienFamilialOption,
  Regroupement10bisTypeCarte,
  Regroupement10bisVerdict,
  SEUIL_RESSOURCES_DEFAULT_10BIS,
} from '../../core/models/regroupement-10bis-be.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { Regroupement10bisBePrefillRules } from './regroupement-10bis-be-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';
import { CoherencePopoverTriggerDirective } from '../../shared/coherence-popover/coherence-popover-trigger.directive';
import {
  CoherenceAlert,
  CoherenceAlertSource,
} from '../../shared/coherence-popover/coherence-alert.model';
import { CoherenceAlertBuilder } from '../../shared/coherence-popover/coherence-alert-builder';

export type IM27_10bisAlertField = 'REVENUS_MENSUELS' | 'DATE_FIN_CARTE_A';

export type IM27_10bisAlertSource = CoherenceAlertSource;
export type IM27_10bisCoherenceAlert = CoherenceAlert<IM27_10bisAlertField>;

/**
 * SF-215-06 — Outil décisionnel « Regroupement familial art. 10bis (BE) »
 * (F-IM-27-regroupement-10bis-be).
 *
 * BELGIQUE uniquement — scoring d'éligibilité pour le regroupement familial
 * sous l'art. 10bis de la Loi du 15/12/1980 (vers un séjour LIMITÉ —
 * carte A — d'où la condition supplémentaire `dateFinCarteA` à vérifier).
 * Seuil de ressources mensuelles fixé à 120 % du RIS (AR du 17/05/2007
 * — seuil opérationnel 1 500 €/mois). Visibilité CONTEXTUAL — flag
 * `regroupement_10bis_detecte`.
 *
 * Pattern miroir : {@link Regroupement10terBeSectionComponent}
 * (F-IM-26-regroupement-10ter-be, SF-215-04) avec 3 différences clés :
 *   1. `typeCarteRegroupant` est forcé à `CARTE_A` (1 seule valeur possible
 *      pour l'art. 10bis) — affiché en lecture seule, non pré-rempli IA.
 *   2. Champ `dateFinCarteA` (input date) — pré-fill IA, alerte visuelle
 *      rouge en bandeau si saisi < today (sans POST), miroir backend via
 *      `conditionTitreEnCours` dans la réponse.
 *   3. Pré-fill IA total = 4 champs (lienFamilial, revenusMensuels,
 *      dureeSejour, dateFinCarteA) au lieu de 4 chez 10ter (lien,
 *      typeCarte, revenusMensuels, dureeSejour).
 *
 * <p>Conforme F-IA-04 :
 *  - standalone OnPush, palette : vert ELIGIBLE, orange SOUS_RESERVE,
 *    rouge INELIGIBLE
 *  - dashboardRefreshService.triggerRefresh() post-POST succès
 *  - static getPrefillCount miroir du runtime prefillFromAi()
 *  - OnPush + ChangeDetectorRef.markForCheck() dans subscribe (next/error)
 *    cf. feedback_onpush_subscribe_markforcheck
 */
@Component({
  selector: 'app-regroupement-10bis-be-section',
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
    MatSelectModule,
    LegalCitationsPipe,
    ToolJurisprudenceCitationsComponent,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './regroupement-10bis-be-section.component.html',
  styleUrl: './regroupement-10bis-be-section.component.scss',
})
export class Regroupement10bisBeSectionComponent implements OnInit, OnChanges {
  // F-JU-03 SF-JU-03-99c — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-IM-27-regroupement-10bis-be';
  protected readonly brancheActiveForJurisprudence = 'default';

  static readonly TOOL_LABEL = 'REGROUPEMENT 10BIS (BE)';
  static readonly TOOL_ICON = 'family_restroom';

  static getPrefillCount(input: PrefillCountInput): number {
    return Regroupement10bisBePrefillRules.computePrefillCount(input);
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
  result = signal<Regroupement10bisBeResponse | null>(null);

  lienFamilial = signal<Regroupement10bisLienFamilial | null>(null);
  // Forced to CARTE_A by construction (art. 10bis = séjour limité only).
  readonly typeCarteRegroupant: Regroupement10bisTypeCarte = 'CARTE_A';
  revenusMensuelsNetsRegroupant = signal<number | null>(null);
  dureeSejour = signal<number | null>(null);
  dateFinCarteA = signal<string | null>(null);
  logementConforme = signal<boolean>(false);
  assuranceMaladie = signal<boolean>(false);
  menaceOrdrePublic = signal<boolean>(false);

  provenanceLienFamilial = signal<'IA' | null>(null);
  provenanceRevenus = signal<'IA' | null>(null);
  provenanceDureeSejour = signal<'IA' | null>(null);
  provenanceDateFinCarte = signal<'IA' | null>(null);

  readonly liensFamiliaux: ReadonlyArray<Regroupement10bisLienFamilialOption> =
    REGROUPEMENT_10BIS_LIENS_FAMILIAUX;

  readonly typeCarteLabel = REGROUPEMENT_10BIS_TYPES_CARTE[0].label;

  readonly seuilRessources = SEUIL_RESSOURCES_DEFAULT_10BIS;

  isBelgique = computed<boolean>(() => this.workspaceCountry === 'BELGIQUE');

  /**
   * SF-215-06 — Calcul UI-only : la carte A est-elle expirée par rapport à
   * la date du jour ? Permet l'affichage d'un bandeau d'avertissement AVANT
   * tout POST. Le backend re-calcule (autorité) et expose le résultat dans
   * la réponse via `conditionTitreEnCours`.
   */
  carteAExpiree = computed<boolean>(() => {
    const d = this.dateFinCarteA();
    if (!d) return false;
    const ts = Date.parse(d);
    if (isNaN(ts)) return false;
    // Compare on calendar day (today's local midnight).
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    return ts < today.getTime();
  });

  /**
   * Alertes de cohérence F-IA-03 exposées par field (revenus + dateFinCarteA —
   * les autres champs sont des enums whitelistés strictement pré-remplis).
   */
  coherenceAlerts = computed<Partial<Record<IM27_10bisAlertField, IM27_10bisCoherenceAlert>>>(() => {
    if (this.standaloneMode) return {};
    if (!this.showForm()) return {};
    const alerts: Partial<Record<IM27_10bisAlertField, IM27_10bisCoherenceAlert>> = {};
    const revenus = this.buildRevenusAlert();
    if (revenus) alerts.REVENUS_MENSUELS = revenus;
    const date = this.buildDateFinCarteAlert();
    if (date) alerts.DATE_FIN_CARTE_A = date;
    return alerts;
  });

  constructor(
    private readonly service: Regroupement10bisBeService,
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
    const rev = this.revenusMensuelsNetsRegroupant();
    if (rev === null || rev === undefined || isNaN(rev) || rev < 0 || rev > 100_000) return false;
    const duree = this.dureeSejour();
    if (duree === null || duree === undefined || isNaN(duree) || duree < 0 || duree > 600) return false;
    const date = this.dateFinCarteA();
    if (!date || !/^\d{4}-\d{2}-\d{2}$/.test(date) || isNaN(Date.parse(date))) return false;
    return true;
  }

  onLienFamilialChange(value: Regroupement10bisLienFamilial | null): void {
    this.lienFamilial.set(value);
    this.provenanceLienFamilial.set(null);
  }

  onRevenusChange(value: number | null | undefined): void {
    this.revenusMensuelsNetsRegroupant.set(value ?? null);
    this.provenanceRevenus.set(null);
  }

  onDureeSejourChange(value: number | null | undefined): void {
    this.dureeSejour.set(value ?? null);
    this.provenanceDureeSejour.set(null);
  }

  onDateFinCarteAChange(value: string | null | undefined): void {
    this.dateFinCarteA.set(value ? value : null);
    this.provenanceDateFinCarte.set(null);
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
    const request: Regroupement10bisBeRequest = {
      lienFamilial: this.lienFamilial()!,
      typeCarteRegroupant: this.typeCarteRegroupant,
      revenusMensuelsNetsRegroupant: this.revenusMensuelsNetsRegroupant()!,
      dureeSejour: this.dureeSejour()!,
      dateFinCarteA: this.dateFinCarteA()!,
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
        this.snackBar.open('Analyse regroupement 10bis BE enregistrée', 'OK', { duration: 2500 });
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

  verdictClass(verdict: Regroupement10bisVerdict | null | undefined): string {
    switch (verdict) {
      case 'ELIGIBLE':     return 're10bis-banner re10bis-banner--success';
      case 'SOUS_RESERVE': return 're10bis-banner re10bis-banner--warning';
      case 'INELIGIBLE':   return 're10bis-banner re10bis-banner--danger';
      default:             return 're10bis-banner';
    }
  }

  verdictChipClass(verdict: Regroupement10bisVerdict | null | undefined): string {
    switch (verdict) {
      case 'ELIGIBLE':     return 're10bis-chip re10bis-chip--success';
      case 'SOUS_RESERVE': return 're10bis-chip re10bis-chip--warning';
      case 'INELIGIBLE':   return 're10bis-chip re10bis-chip--danger';
      default:             return 're10bis-chip';
    }
  }

  verdictIcon(verdict: Regroupement10bisVerdict | null | undefined): string {
    switch (verdict) {
      case 'ELIGIBLE':     return 'check_circle';
      case 'SOUS_RESERVE': return 'warning';
      case 'INELIGIBLE':   return 'block';
      default:             return 'info_outline';
    }
  }

  verdictLabel(verdict: Regroupement10bisVerdict | null | undefined): string {
    switch (verdict) {
      case 'ELIGIBLE':     return 'Éligible';
      case 'SOUS_RESERVE': return 'Sous réserve';
      case 'INELIGIBLE':   return 'Inéligible';
      default:             return '';
    }
  }

  /** Barre Material — `primary` (vert/navy), `accent` (orange), `warn` (rouge). */
  scoreBarColor(verdict: Regroupement10bisVerdict | null | undefined): 'primary' | 'accent' | 'warn' {
    switch (verdict) {
      case 'ELIGIBLE':     return 'primary';
      case 'SOUS_RESERVE': return 'accent';
      case 'INELIGIBLE':   return 'warn';
      default:             return 'primary';
    }
  }

  /** Classe CSS du badge différentiel — vert si ≥ 0, rouge si négatif. */
  differentielBadgeClass(diff: number | null | undefined): string {
    if (diff === null || diff === undefined) return 're10bis-diff';
    return diff >= 0
      ? 're10bis-diff re10bis-diff--positive'
      : 're10bis-diff re10bis-diff--negative';
  }

  /** Texte humain du différentiel signé (en €/mois). */
  differentielText(diff: number | null | undefined): string {
    if (diff === null || diff === undefined) return '';
    if (diff >= 0) {
      return `+${diff} € / mois au-dessus du seuil`;
    }
    return `${diff} € / mois en-dessous du seuil`;
  }

  lienFamilialLabel(code: Regroupement10bisLienFamilial | string | null | undefined): string {
    if (!code) return '';
    return this.liensFamiliaux.find((l) => l.code === code)?.label ?? String(code);
  }

  alertTooltip(alert: IM27_10bisCoherenceAlert): string {
    return alert.reason;
  }

  alertBadgeLabel(alert: IM27_10bisCoherenceAlert): string {
    if (alert.severity === 'CRITICAL') return `Alerte critique (${alert.expectedDisplay})`;
    return `Incohérence détectée (${alert.expectedDisplay})`;
  }

  private buildRevenusAlert(): IM27_10bisCoherenceAlert | null {
    const userRevenus = this.revenusMensuelsNetsRegroupant();
    if (userRevenus === null || userRevenus === undefined) return null;
    const builder = CoherenceAlertBuilder.forField<IM27_10bisAlertField>('REVENUS_MENSUELS')
      .withSeverity('WARNING');
    const ai = this.aiData;
    const aiRevenus = ai?.be10bisRevenusMensuels;
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

  private buildDateFinCarteAlert(): IM27_10bisCoherenceAlert | null {
    const userDate = this.dateFinCarteA();
    if (!userDate) return null;
    const builder = CoherenceAlertBuilder.forField<IM27_10bisAlertField>('DATE_FIN_CARTE_A')
      .withSeverity('WARNING');
    const aiDate = this.aiData?.be10bisDateFinCarteA;
    if (typeof aiDate === 'string' && aiDate.length > 0 && aiDate !== userDate) {
      builder.addSource('IA', {
        expectedDisplay: aiDate,
        reason: `Analyse du dossier : date de fin de carte A = ${aiDate}`,
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

    const lien = Regroupement10bisBePrefillRules.computeLienFamilial(input);
    if (lien !== null && !this.lienFamilial()) {
      this.lienFamilial.set(lien);
      this.provenanceLienFamilial.set('IA');
    }

    const rev = Regroupement10bisBePrefillRules.computeRevenusMensuelsNetsRegroupant(input);
    if (rev !== null && this.revenusMensuelsNetsRegroupant() === null) {
      this.revenusMensuelsNetsRegroupant.set(rev);
      this.provenanceRevenus.set('IA');
    }

    const duree = Regroupement10bisBePrefillRules.computeDureeSejour(input);
    if (duree !== null && this.dureeSejour() === null) {
      this.dureeSejour.set(duree);
      this.provenanceDureeSejour.set('IA');
    }

    const date = Regroupement10bisBePrefillRules.computeDateFinCarteA(input);
    if (date !== null && this.dateFinCarteA() === null) {
      this.dateFinCarteA.set(date);
      this.provenanceDateFinCarte.set('IA');
    }
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.lienFamilial.set((r.lienFamilial as Regroupement10bisLienFamilial) ?? null);
        this.revenusMensuelsNetsRegroupant.set(r.revenusMensuelsNetsRegroupant ?? null);
        this.dureeSejour.set(r.dureeSejour ?? null);
        this.dateFinCarteA.set(r.dateFinCarteA ?? null);
        this.logementConforme.set(r.logementConforme ?? false);
        this.assuranceMaladie.set(r.assuranceMaladie ?? false);
        this.menaceOrdrePublic.set(r.menaceOrdrePublic ?? false);
        this.provenanceLienFamilial.set(null);
        this.provenanceRevenus.set(null);
        this.provenanceDureeSejour.set(null);
        this.provenanceDateFinCarte.set(null);
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
