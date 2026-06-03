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
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';

import { RttMonetisationService } from '../../core/services/rtt-monetisation.service';
import {
  RttMonetisationRequest,
  RttMonetisationResponse,
  RttMonetisationStatut,
} from '../../core/models/rtt-monetisation.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { RttMonetisationPrefillRules } from './rtt-monetisation-section-prefill-rules';
import { CoherencePopoverTriggerDirective } from '../../shared/coherence-popover/coherence-popover-trigger.directive';
import { CoherenceAlert } from '../../shared/coherence-popover/coherence-alert.model';
import { CoherenceAlertBuilder } from '../../shared/coherence-popover/coherence-alert-builder';

/**
 * SF-218-38 : champs d'alerte de cohérence F-IA-03 exposés par l'outil F-DT-51
 * (RTT — monétisation).
 */
export type RttAlertField = 'NB_JOURS' | 'SALAIRE_JOURNALIER';
export type RttCoherenceAlert = CoherenceAlert<RttAlertField>;

/** Écart relatif au-delà duquel on affiche une alerte de cohérence IA ↔ saisie. */
const DIVERGENCE_RATIO = 0.10;

/** Bornes de la fenêtre du dispositif de monétisation (loi LFR 2022). */
const FENETRE_DEBUT = '2022-01-01';
const FENETRE_FIN = '2026-12-31';

/**
 * SF-218-38 — Outil décisionnel « RTT — monétisation (rachat de jours de RTT) »
 * (F-DT-51-rtt-monetisation).
 *
 * FRANCE uniquement — calculateur d'indemnité de monétisation de jours de RTT
 * (loi n° 2022-1157 du 16/08/2022 art. 5, dispositif prolongé jusqu'au
 * 31/12/2026) : sur demande du salarié et avec accord de l'employeur, les
 * jours/demi-journées de RTT acquis entre le 01/01/2022 et le 31/12/2026 peuvent
 * être renoncés contre rémunération majorée (taux 10–25 %, régime social et
 * fiscal aligné sur les heures supplémentaires). Distinct de F-DT-19 heures
 * supplémentaires et de F-DT-80 acquisition de JRTT.
 *
 * Verdict d'éligibilité (2 états) :
 *  - ELIGIBLE (vert) : `dateRenonciation` dans la fenêtre → montant brut majoré.
 *  - NON_ELIGIBLE (rouge) : hors fenêtre → pas de monétisation.
 *
 * Conforme F-IA-04 :
 *  - standalone OnPush, palette navy/or ; rouge réservé NON_ELIGIBLE ; vert
 *    ELIGIBLE ; JetBrains Mono montant / taux / baseJuridique ; bannière gate FR ;
 *    MatSnackBar.
 *  - pré-fill IA (nombreJoursRttRenonces, salaireJournalierBrut) depuis
 *    `Sf218dDetail` (clés snake_case) + badge `auto_awesome` de provenance.
 *  - OnPush + ChangeDetectorRef.markForCheck() dans subscribe (next/error)
 *    cf. feedback_onpush_subscribe_markforcheck.
 *  - F-IA-03 : coherenceAlerts (divergence IA ↔ saisie) via CoherenceAlertBuilder
 *    + popover de divergence.
 */
@Component({
  selector: 'app-rtt-monetisation-section',
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
    MatProgressSpinnerModule,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './rtt-monetisation-section.component.html',
  styleUrl: './rtt-monetisation-section.component.scss',
})
export class RttMonetisationSectionComponent implements OnInit, OnChanges {
  static readonly TOOL_LABEL = 'RTT — MONÉTISATION (FR)';
  static readonly TOOL_ICON = 'savings';

  /** Bornes de la fenêtre du dispositif exposées au template. */
  readonly fenetreDebut = FENETRE_DEBUT;
  readonly fenetreFin = FENETRE_FIN;

  static getPrefillCount(input: PrefillCountInput): number {
    return RttMonetisationPrefillRules.computePrefillCount(input);
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
  result = signal<RttMonetisationResponse | null>(null);

  // --- formulaire -----------------------------------------------------------
  nombreJoursRttRenonces = signal<number | null>(null);
  salaireJournalierBrut = signal<number | null>(null);
  tauxMajorationConventionnel = signal<number | null>(null);
  /** Date de la renonciation — conditionne l'appartenance à la fenêtre. */
  dateRenonciation = signal<string | null>(null);

  provenanceNbJours = signal<'IA' | null>(null);
  provenanceSalaire = signal<'IA' | null>(null);

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  /**
   * `joursAcquisDansFenetre` dérivé de `dateRenonciation` : true si la date est
   * comprise dans [01/01/2022, 31/12/2026]. Si la date n'est pas saisie, on
   * considère par défaut la fenêtre ouverte (le backend tranche au calcul).
   */
  joursAcquisDansFenetre = computed<boolean>(() => {
    const d = this.dateRenonciation();
    if (!d) return true;
    return d >= FENETRE_DEBUT && d <= FENETRE_FIN;
  });

  /**
   * F-IA-03 — alertes de cohérence (non bloquantes) entre les valeurs extraites
   * par l'IA (sous-record `Sf218dDetail`) et la saisie de l'avocat, via
   * `CoherenceAlertBuilder` (divergence > 10 %).
   */
  coherenceAlerts = computed<Partial<Record<RttAlertField, RttCoherenceAlert>>>(() => {
    if (this.standaloneMode || !this.showForm()) return {};
    const alerts: Partial<Record<RttAlertField, RttCoherenceAlert>> = {};
    const nbAlert = this.buildNbJoursAlert();
    if (nbAlert) alerts.NB_JOURS = nbAlert;
    const salaireAlert = this.buildSalaireAlert();
    if (salaireAlert) alerts.SALAIRE_JOURNALIER = salaireAlert;
    return alerts;
  });

  constructor(
    private readonly service: RttMonetisationService,
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

  /** Soumettable dès que jours > 0 et salaire journalier > 0 sont saisis. */
  formValid(): boolean {
    const j = this.nombreJoursRttRenonces();
    const s = this.salaireJournalierBrut();
    return j !== null && Number.isFinite(j) && j > 0
        && s !== null && Number.isFinite(s) && s > 0;
  }

  onNbJoursChange(value: number | null): void {
    this.nombreJoursRttRenonces.set(value === null || value === undefined ? null : Number(value));
    this.provenanceNbJours.set(null);
  }

  onSalaireChange(value: number | null): void {
    this.salaireJournalierBrut.set(value === null || value === undefined ? null : Number(value));
    this.provenanceSalaire.set(null);
  }

  onTauxChange(value: number | null): void {
    this.tauxMajorationConventionnel.set(value === null || value === undefined ? null : Number(value));
  }

  onDateRenonciationChange(value: string | null): void {
    this.dateRenonciation.set(value && value.length > 0 ? value : null);
  }

  analyze(): void {
    if (!this.formValid()) return;
    const request: RttMonetisationRequest = {
      nombreJoursRttRenonces: this.nombreJoursRttRenonces()!,
      salaireJournalierBrut: this.salaireJournalierBrut()!,
      tauxMajorationConventionnel: this.tauxMajorationConventionnel(),
      joursAcquisDansFenetre: this.joursAcquisDansFenetre(),
    };
    this.analyzing.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('Analyse monétisation RTT enregistrée', 'OK', { duration: 2500 });
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

  statutLabel(s: RttMonetisationStatut | null | undefined): string {
    switch (s) {
      case 'ELIGIBLE':     return 'Monétisation éligible';
      case 'NON_ELIGIBLE': return 'Monétisation non éligible';
      default:             return '';
    }
  }

  statutChipClass(s: RttMonetisationStatut | null | undefined): string {
    switch (s) {
      case 'ELIGIBLE':     return 'is-chip is-chip--success';
      case 'NON_ELIGIBLE': return 'is-chip is-chip--danger';
      default:             return 'is-chip';
    }
  }

  bannerClass(s: RttMonetisationStatut | null | undefined): string {
    switch (s) {
      case 'ELIGIBLE':     return 'is-banner is-banner--success';
      case 'NON_ELIGIBLE': return 'is-banner is-banner--danger';
      default:             return 'is-banner is-banner--navy';
    }
  }

  bannerIcon(s: RttMonetisationStatut | null | undefined): string {
    switch (s) {
      case 'ELIGIBLE':     return 'verified';
      case 'NON_ELIGIBLE': return 'gpp_bad';
      default:             return 'info_outline';
    }
  }

  alertTooltip(alert: RttCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: RttCoherenceAlert): string {
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

  private buildNbJoursAlert(): RttCoherenceAlert | null {
    const ai = this.aiData?.nombre_jours_rtt_renonces;
    const user = this.nombreJoursRttRenonces();
    if (typeof ai !== 'number' || ai <= 0) return null;
    if (typeof user !== 'number' || user <= 0) return null;
    const ratio = Math.abs(user - ai) / ai;
    if (ratio <= DIVERGENCE_RATIO) return null;
    return CoherenceAlertBuilder.forField<RttAlertField>('NB_JOURS')
      .addSource('IA', {
        expectedDisplay: `${ai.toLocaleString('fr-FR')} j`,
        reason: `Analyse du dossier : ${ai.toLocaleString('fr-FR')} jour(s) de RTT renoncé(s)`,
      })
      .build();
  }

  private buildSalaireAlert(): RttCoherenceAlert | null {
    const ai = this.aiData?.salaire_journalier_brut;
    const user = this.salaireJournalierBrut();
    if (typeof ai !== 'number' || ai <= 0) return null;
    if (typeof user !== 'number' || user <= 0) return null;
    const ratio = Math.abs(user - ai) / ai;
    if (ratio <= DIVERGENCE_RATIO) return null;
    return CoherenceAlertBuilder.forField<RttAlertField>('SALAIRE_JOURNALIER')
      .addSource('IA', {
        expectedDisplay: `${ai.toLocaleString('fr-FR')} €`,
        reason: `Analyse du dossier : salaire journalier brut ~${ai.toLocaleString('fr-FR')} €`,
      })
      .build();
  }

  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    const input: PrefillCountInput = {
      aiData: this.aiData,
      workspaceCountry: this.workspaceCountry,
    };

    const nbJours = RttMonetisationPrefillRules.computeNombreJoursRttRenonces(input);
    if (nbJours !== null && this.nombreJoursRttRenonces() === null) {
      this.nombreJoursRttRenonces.set(nbJours);
      this.provenanceNbJours.set('IA');
    }

    const salaire = RttMonetisationPrefillRules.computeSalaireJournalierBrut(input);
    if (salaire !== null && this.salaireJournalierBrut() === null) {
      this.salaireJournalierBrut.set(salaire);
      this.provenanceSalaire.set('IA');
    }
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.nombreJoursRttRenonces.set(r.nombreJoursRttRenonces ?? null);
        this.salaireJournalierBrut.set(r.salaireJournalierBrut ?? null);
        this.tauxMajorationConventionnel.set(r.tauxApplique ?? null);
        this.provenanceNbJours.set(null);
        this.provenanceSalaire.set(null);
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
