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
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';

import { EpargneSalarialeConformiteService } from '../../core/services/epargne-salariale-conformite.service';
import {
  EpargneSalarialeConformiteRequest,
  EpargneSalarialeConformiteResponse,
  EpargneSalarialeConformiteStatut,
} from '../../core/models/epargne-salariale-conformite.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { EpargneSalarialeConformitePrefillRules } from './epargne-salariale-conformite-section-prefill-rules';
import { CoherencePopoverTriggerDirective } from '../../shared/coherence-popover/coherence-popover-trigger.directive';
import { CoherenceAlert } from '../../shared/coherence-popover/coherence-alert.model';
import { CoherenceAlertBuilder } from '../../shared/coherence-popover/coherence-alert-builder';

/**
 * SF-218-42 : champs d'alerte de cohérence F-IA-03 exposés par l'outil F-DT-53
 * (épargne salariale — conformité).
 */
export type EpargneSalarialeAlertField =
  | 'ACCORD_PARTICIPATION'
  | 'ACCORD_INTERESSEMENT';
export type EpargneSalarialeCoherenceAlert =
  CoherenceAlert<EpargneSalarialeAlertField>;

/** Seuil d'effectif rendant la participation obligatoire (prévisible UI). */
const SEUIL_PARTICIPATION_OBLIGATOIRE = 50;

/**
 * SF-218-42 — Outil décisionnel « Épargne salariale — conformité (intéressement /
 * participation / partage de la valeur) » (F-DT-53-epargne-salariale-conformite).
 *
 * FRANCE uniquement — analyseur de conformité aux obligations d'épargne salariale
 * (art. L.3311-1 et s., L.3321-1 et s., L.3322-2 CT + loi n° 2023-1107 du
 * 29/11/2023) : participation obligatoire au-delà de 50 salariés, obligation de
 * dispositif de partage de la valeur pour les entreprises de 11 à 49 salariés
 * rentables (exercices ouverts à compter de 2025). Distinct de F-DT-52 PPV
 * exonération (invariant « un outil = une situation »).
 *
 * Verdict de conformité (3 états) :
 *  - CONFORME (vert) : toutes les obligations applicables remplies.
 *  - OBLIGATION_NON_REMPLIE (rouge) : au moins une obligation non remplie.
 *  - NON_REQUIS (gris) : aucune obligation applicable.
 *
 * Thème DIAGNOSTIC. Conforme F-IA-04 :
 *  - standalone OnPush ; rouge réservé OBLIGATION_NON_REMPLIE / item manquant ;
 *    vert conforme ; gris NON_REQUIS ; JetBrains Mono compteur / baseJuridique ;
 *    bannière gate FR ; MatSnackBar.
 *  - pré-fill IA (accordParticipationPresent, accordInteressementPresent) depuis
 *    `Sf218dDetail` (clés snake_case) + badge `auto_awesome` de provenance.
 *  - OnPush + ChangeDetectorRef.markForCheck() dans subscribe (next/error)
 *    cf. feedback_onpush_subscribe_markforcheck.
 *  - F-IA-03 : coherenceAlerts (divergence IA ↔ saisie) via CoherenceAlertBuilder
 *    + popover de divergence.
 */
@Component({
  selector: 'app-epargne-salariale-conformite-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatCheckboxModule,
    MatProgressSpinnerModule,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './epargne-salariale-conformite-section.component.html',
  styleUrl: './epargne-salariale-conformite-section.component.scss',
})
export class EpargneSalarialeConformiteSectionComponent implements OnInit, OnChanges {
  static readonly TOOL_LABEL = 'ÉPARGNE SALARIALE — CONFORMITÉ (FR)';
  static readonly TOOL_ICON = 'savings';

  static getPrefillCount(input: PrefillCountInput): number {
    return EpargneSalarialeConformitePrefillRules.computePrefillCount(input);
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
  result = signal<EpargneSalarialeConformiteResponse | null>(null);

  // --- formulaire -----------------------------------------------------------
  effectif = signal<number | null>(null);
  accordParticipationPresent = signal<boolean>(false);
  accordInteressementPresent = signal<boolean>(false);
  beneficeNetFiscalPositif3Ans = signal<boolean>(false);
  entreprise11a49 = signal<boolean>(false);

  provenanceParticipation = signal<'IA' | null>(null);
  provenanceInteressement = signal<'IA' | null>(null);

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  /** Participation obligatoire prévisible côté UI (effectif ≥ 50) avant calcul. */
  participationObligatoirePrevisible = computed<boolean>(() => {
    const e = this.effectif();
    return e !== null && Number.isFinite(e) && e >= SEUIL_PARTICIPATION_OBLIGATOIRE;
  });

  /**
   * F-IA-03 — alertes de cohérence (non bloquantes) entre les valeurs extraites
   * par l'IA (sous-record `Sf218dDetail`) et la saisie de l'avocat, via
   * `CoherenceAlertBuilder` (valeur différente pour les booléens d'accord).
   */
  coherenceAlerts = computed<Partial<Record<EpargneSalarialeAlertField, EpargneSalarialeCoherenceAlert>>>(() => {
    if (this.standaloneMode || !this.showForm()) return {};
    const alerts: Partial<Record<EpargneSalarialeAlertField, EpargneSalarialeCoherenceAlert>> = {};
    const participationAlert = this.buildParticipationAlert();
    if (participationAlert) alerts.ACCORD_PARTICIPATION = participationAlert;
    const interessementAlert = this.buildInteressementAlert();
    if (interessementAlert) alerts.ACCORD_INTERESSEMENT = interessementAlert;
    return alerts;
  });

  constructor(
    private readonly service: EpargneSalarialeConformiteService,
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

  /** Soumettable dès que l'effectif > 0 est saisi. */
  formValid(): boolean {
    const e = this.effectif();
    return e !== null && Number.isFinite(e) && e > 0;
  }

  onEffectifChange(value: number | null): void {
    this.effectif.set(value === null || value === undefined ? null : Number(value));
  }

  onParticipationChange(value: boolean): void {
    this.accordParticipationPresent.set(!!value);
    this.provenanceParticipation.set(null);
  }

  onInteressementChange(value: boolean): void {
    this.accordInteressementPresent.set(!!value);
    this.provenanceInteressement.set(null);
  }

  onBeneficeChange(value: boolean): void {
    this.beneficeNetFiscalPositif3Ans.set(!!value);
  }

  onEntreprise11a49Change(value: boolean): void {
    this.entreprise11a49.set(!!value);
  }

  analyze(): void {
    if (!this.formValid()) return;
    const request: EpargneSalarialeConformiteRequest = {
      effectif: this.effectif()!,
      accordParticipationPresent: this.accordParticipationPresent(),
      accordInteressementPresent: this.accordInteressementPresent(),
      beneficeNetFiscalPositif3Ans: this.beneficeNetFiscalPositif3Ans(),
      entreprise11a49: this.entreprise11a49(),
    };
    this.analyzing.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('Analyse conformité épargne salariale enregistrée', 'OK', { duration: 2500 });
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

  statutLabel(s: EpargneSalarialeConformiteStatut | null | undefined): string {
    switch (s) {
      case 'CONFORME':               return 'Obligations d\'épargne salariale respectées';
      case 'OBLIGATION_NON_REMPLIE': return 'Obligation d\'épargne salariale non remplie';
      case 'NON_REQUIS':             return 'Aucune obligation applicable';
      default:                       return '';
    }
  }

  statutChipClass(s: EpargneSalarialeConformiteStatut | null | undefined): string {
    switch (s) {
      case 'CONFORME':               return 'is-chip is-chip--success';
      case 'OBLIGATION_NON_REMPLIE': return 'is-chip is-chip--danger';
      case 'NON_REQUIS':             return 'is-chip is-chip--neutral';
      default:                       return 'is-chip';
    }
  }

  bannerClass(s: EpargneSalarialeConformiteStatut | null | undefined): string {
    switch (s) {
      case 'CONFORME':               return 'is-banner is-banner--success';
      case 'OBLIGATION_NON_REMPLIE': return 'is-banner is-banner--danger';
      case 'NON_REQUIS':             return 'is-banner is-banner--neutral';
      default:                       return 'is-banner is-banner--navy';
    }
  }

  bannerIcon(s: EpargneSalarialeConformiteStatut | null | undefined): string {
    switch (s) {
      case 'CONFORME':               return 'verified';
      case 'OBLIGATION_NON_REMPLIE': return 'gpp_bad';
      case 'NON_REQUIS':             return 'info_outline';
      default:                       return 'info_outline';
    }
  }

  alertTooltip(alert: EpargneSalarialeCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: EpargneSalarialeCoherenceAlert): string {
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

  private buildParticipationAlert(): EpargneSalarialeCoherenceAlert | null {
    const ai = this.aiData?.accord_participation_present;
    const user = this.accordParticipationPresent();
    if (typeof ai !== 'boolean') return null;
    if (ai === user) return null;
    return CoherenceAlertBuilder.forField<EpargneSalarialeAlertField>('ACCORD_PARTICIPATION')
      .addSource('IA', {
        expectedDisplay: ai ? 'Accord présent' : 'Pas d\'accord',
        reason: `Analyse du dossier : accord de participation ${ai ? 'détecté' : 'non détecté'}`,
      })
      .build();
  }

  private buildInteressementAlert(): EpargneSalarialeCoherenceAlert | null {
    const ai = this.aiData?.accord_interessement_present;
    const user = this.accordInteressementPresent();
    if (typeof ai !== 'boolean') return null;
    if (ai === user) return null;
    return CoherenceAlertBuilder.forField<EpargneSalarialeAlertField>('ACCORD_INTERESSEMENT')
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

    const participation = EpargneSalarialeConformitePrefillRules.computeAccordParticipationPresent(input);
    if (participation !== null && this.provenanceParticipation() === null && !this.accordParticipationPresent()) {
      this.accordParticipationPresent.set(participation);
      this.provenanceParticipation.set('IA');
    }

    const interessement = EpargneSalarialeConformitePrefillRules.computeAccordInteressementPresent(input);
    if (interessement !== null && this.provenanceInteressement() === null && !this.accordInteressementPresent()) {
      this.accordInteressementPresent.set(interessement);
      this.provenanceInteressement.set('IA');
    }
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.effectif.set(r.effectif ?? null);
        this.accordParticipationPresent.set(r.accordParticipationPresent ?? false);
        this.accordInteressementPresent.set(r.accordInteressementPresent ?? false);
        this.beneficeNetFiscalPositif3Ans.set(r.beneficeNetFiscalPositif3Ans ?? false);
        this.entreprise11a49.set(r.entreprise11a49 ?? false);
        this.provenanceParticipation.set(null);
        this.provenanceInteressement.set(null);
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
