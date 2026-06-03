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
import { CommonModule, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';

import { PpvExonerationService } from '../../core/services/ppv-exoneration.service';
import {
  PpvExonerationRequest,
  PpvExonerationResponse,
  PpvExonerationStatut,
} from '../../core/models/ppv-exoneration.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { PpvExonerationPrefillRules } from './ppv-exoneration-section-prefill-rules';
import { CoherencePopoverTriggerDirective } from '../../shared/coherence-popover/coherence-popover-trigger.directive';
import { CoherenceAlert } from '../../shared/coherence-popover/coherence-alert.model';
import { CoherenceAlertBuilder } from '../../shared/coherence-popover/coherence-alert-builder';

/**
 * SF-218-40 : champs d'alerte de cohérence F-IA-03 exposés par l'outil F-DT-52
 * (PPV — exonération).
 */
export type PpvAlertField = 'MONTANT_PRIME' | 'ACCORD_INTERESSEMENT';
export type PpvCoherenceAlert = CoherenceAlert<PpvAlertField>;

/** Écart relatif au-delà duquel on affiche une alerte de cohérence IA ↔ saisie. */
const DIVERGENCE_RATIO = 0.10;

/**
 * SF-218-40 — Outil décisionnel « PPV — exonération (prime de partage de la
 * valeur) » (F-DT-52-ppv-exoneration).
 *
 * FRANCE uniquement — calculateur d'exonération de la prime de partage de la
 * valeur (loi n° 2022-1158 du 16/08/2022 art. 1 + loi n° 2023-1107 du
 * 29/11/2023) : conformité au plafond d'exonération sociale (3 000 € de droit
 * commun ; 6 000 € si accord d'intéressement OU effectif < 50), part exonérée vs
 * part imposable, exonération fiscale IR conditionnelle (effectif < 50 +
 * rémunération < 3 SMIC, jusqu'au 31/12/2026). Distinct de F-DT-53
 * intéressement / participation.
 *
 * Verdict de conformité (2 états) :
 *  - CONFORME (vert) : montant ≤ plafond → intégralement exonéré.
 *  - PLAFOND_DEPASSE (rouge) : fraction excédentaire réintégrée.
 *
 * Conforme F-IA-04 :
 *  - standalone OnPush, palette navy/or ; rouge réservé PLAFOND_DEPASSE ; vert
 *    CONFORME ; JetBrains Mono montants / plafond / baseJuridique ; bannière gate
 *    FR ; MatSnackBar.
 *  - pré-fill IA (montantPrime, accordInteressementPresent) depuis `Sf218dDetail`
 *    (clés snake_case) + badge `auto_awesome` de provenance.
 *  - OnPush + ChangeDetectorRef.markForCheck() dans subscribe (next/error)
 *    cf. feedback_onpush_subscribe_markforcheck.
 *  - F-IA-03 : coherenceAlerts (divergence IA ↔ saisie) via CoherenceAlertBuilder
 *    + popover de divergence.
 */
@Component({
  selector: 'app-ppv-exoneration-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    FormsModule,
    DecimalPipe,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatCheckboxModule,
    MatProgressSpinnerModule,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './ppv-exoneration-section.component.html',
  styleUrl: './ppv-exoneration-section.component.scss',
})
export class PpvExonerationSectionComponent implements OnInit, OnChanges {
  static readonly TOOL_LABEL = 'PPV — EXONÉRATION (FR)';
  static readonly TOOL_ICON = 'redeem';

  static getPrefillCount(input: PrefillCountInput): number {
    return PpvExonerationPrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';
  @Input() aiData?: TravailExtractedData | null;
  @Input() forceExpanded = false;
  @Input() standaloneMode = false;

  collapsed = signal(true);
  loading = signal(false);
  analyzing = signal(false);
  showForm = signal(true);
  result = signal<PpvExonerationResponse | null>(null);

  // --- formulaire -----------------------------------------------------------
  montantPrime = signal<number | null>(null);
  accordInteressementPresent = signal<boolean>(false);
  remunerationAnnuelleBrute = signal<number | null>(null);
  effectifMoins50 = signal<boolean>(false);
  versementPlanEpargne = signal<boolean>(false);

  provenanceMontant = signal<'IA' | null>(null);
  provenanceAccord = signal<'IA' | null>(null);

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  /** Plafond social prévisible côté UI (3 000 € / 6 000 €) avant calcul backend. */
  plafondPrevisible = computed<number>(() =>
    this.accordInteressementPresent() || this.effectifMoins50() ? 6000 : 3000);

  /**
   * F-IA-03 — alertes de cohérence (non bloquantes) entre les valeurs extraites
   * par l'IA (sous-record `Sf218dDetail`) et la saisie de l'avocat, via
   * `CoherenceAlertBuilder` (divergence > 10 % pour le montant ; valeur
   * différente pour le booléen accord).
   */
  coherenceAlerts = computed<Partial<Record<PpvAlertField, PpvCoherenceAlert>>>(() => {
    if (this.standaloneMode || !this.showForm()) return {};
    const alerts: Partial<Record<PpvAlertField, PpvCoherenceAlert>> = {};
    const montantAlert = this.buildMontantAlert();
    if (montantAlert) alerts.MONTANT_PRIME = montantAlert;
    const accordAlert = this.buildAccordAlert();
    if (accordAlert) alerts.ACCORD_INTERESSEMENT = accordAlert;
    return alerts;
  });

  constructor(
    private readonly service: PpvExonerationService,
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
    if (this.isFrance() && this.caseFileId) {
      this.load();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['forceExpanded'] && this.forceExpanded) {
      this.collapsed.set(false);
    }
    if (changes['aiData'] && this.isFrance() && this.showForm() && !this.result()) {
      this.prefillFromAi();
    }
  }

  toggleCollapse(): void {
    this.collapsed.update((v) => !v);
  }

  editMode(): void {
    this.showForm.set(true);
  }

  /** Soumettable dès que montant > 0 et rémunération > 0 sont saisis. */
  formValid(): boolean {
    const m = this.montantPrime();
    const r = this.remunerationAnnuelleBrute();
    return m !== null && Number.isFinite(m) && m > 0
        && r !== null && Number.isFinite(r) && r > 0;
  }

  onMontantChange(value: number | null): void {
    this.montantPrime.set(value === null || value === undefined ? null : Number(value));
    this.provenanceMontant.set(null);
  }

  onAccordChange(value: boolean): void {
    this.accordInteressementPresent.set(!!value);
    this.provenanceAccord.set(null);
  }

  onRemunerationChange(value: number | null): void {
    this.remunerationAnnuelleBrute.set(value === null || value === undefined ? null : Number(value));
  }

  onEffectifChange(value: boolean): void {
    this.effectifMoins50.set(!!value);
  }

  onPlanEpargneChange(value: boolean): void {
    this.versementPlanEpargne.set(!!value);
  }

  analyze(): void {
    if (!this.formValid()) return;
    const request: PpvExonerationRequest = {
      montantPrime: this.montantPrime()!,
      accordInteressementPresent: this.accordInteressementPresent(),
      remunerationAnnuelleBrute: this.remunerationAnnuelleBrute()!,
      effectifMoins50: this.effectifMoins50(),
      versementPlanEpargne: this.versementPlanEpargne(),
    };
    this.analyzing.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('Analyse exonération PPV enregistrée', 'OK', { duration: 2500 });
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

  // --- labels / helpers -----------------------------------------------------

  statutLabel(s: PpvExonerationStatut | null | undefined): string {
    switch (s) {
      case 'CONFORME':        return 'PPV conforme au plafond';
      case 'PLAFOND_DEPASSE': return 'Plafond d\'exonération dépassé';
      default:                return '';
    }
  }

  statutChipClass(s: PpvExonerationStatut | null | undefined): string {
    switch (s) {
      case 'CONFORME':        return 'is-chip is-chip--success';
      case 'PLAFOND_DEPASSE': return 'is-chip is-chip--danger';
      default:                return 'is-chip';
    }
  }

  bannerClass(s: PpvExonerationStatut | null | undefined): string {
    switch (s) {
      case 'CONFORME':        return 'is-banner is-banner--success';
      case 'PLAFOND_DEPASSE': return 'is-banner is-banner--danger';
      default:                return 'is-banner is-banner--navy';
    }
  }

  bannerIcon(s: PpvExonerationStatut | null | undefined): string {
    switch (s) {
      case 'CONFORME':        return 'verified';
      case 'PLAFOND_DEPASSE': return 'gpp_bad';
      default:                return 'info_outline';
    }
  }

  alertTooltip(alert: PpvCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: PpvCoherenceAlert): string {
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

  private buildMontantAlert(): PpvCoherenceAlert | null {
    const ai = this.aiData?.montant_ppv;
    const user = this.montantPrime();
    if (typeof ai !== 'number' || ai <= 0) return null;
    if (typeof user !== 'number' || user <= 0) return null;
    const ratio = Math.abs(user - ai) / ai;
    if (ratio <= DIVERGENCE_RATIO) return null;
    return CoherenceAlertBuilder.forField<PpvAlertField>('MONTANT_PRIME')
      .addSource('IA', {
        expectedDisplay: `${ai.toLocaleString('fr-FR')} €`,
        reason: `Analyse du dossier : PPV ~${ai.toLocaleString('fr-FR')} €`,
      })
      .build();
  }

  private buildAccordAlert(): PpvCoherenceAlert | null {
    const ai = this.aiData?.accord_interessement_present;
    const user = this.accordInteressementPresent();
    if (typeof ai !== 'boolean') return null;
    if (ai === user) return null;
    return CoherenceAlertBuilder.forField<PpvAlertField>('ACCORD_INTERESSEMENT')
      .addSource('IA', {
        expectedDisplay: ai ? 'Accord présent' : 'Pas d\'accord',
        reason: `Analyse du dossier : accord d'intéressement ${ai ? 'détecté' : 'non détecté'}`,
      })
      .build();
  }

  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    const input: PrefillCountInput = {
      aiData: this.aiData,
      workspaceCountry: this.workspaceCountry,
    };

    const montant = PpvExonerationPrefillRules.computeMontantPrime(input);
    if (montant !== null && this.montantPrime() === null) {
      this.montantPrime.set(montant);
      this.provenanceMontant.set('IA');
    }

    const accord = PpvExonerationPrefillRules.computeAccordInteressementPresent(input);
    if (accord !== null && this.provenanceAccord() === null && !this.accordInteressementPresent()) {
      this.accordInteressementPresent.set(accord);
      this.provenanceAccord.set('IA');
    }
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.montantPrime.set(r.montantPrime ?? null);
        this.accordInteressementPresent.set(r.accordInteressementPresent ?? false);
        this.remunerationAnnuelleBrute.set(r.remunerationAnnuelleBrute ?? null);
        this.effectifMoins50.set(r.effectifMoins50 ?? false);
        this.versementPlanEpargne.set(r.versementPlanEpargne ?? false);
        this.provenanceMontant.set(null);
        this.provenanceAccord.set(null);
        this.showForm.set(false);
        this.loading.set(false);
        this.cdr.markForCheck();
      },
      error: () => {
        // 404 = pas encore d'analyse, on tente le pré-fill IA.
        this.prefillFromAi();
        this.loading.set(false);
        this.cdr.markForCheck();
      },
    });
  }
}
