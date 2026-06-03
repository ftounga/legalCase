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

import { RttAcquisitionService } from '../../core/services/rtt-acquisition.service';
import {
  RttAcquisitionRequest,
  RttAcquisitionResponse,
  RttAcquisitionStatut,
} from '../../core/models/rtt-acquisition.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { RttAcquisitionPrefillRules } from './rtt-acquisition-section-prefill-rules';
import { CoherencePopoverTriggerDirective } from '../../shared/coherence-popover/coherence-popover-trigger.directive';
import { CoherenceAlert } from '../../shared/coherence-popover/coherence-alert.model';
import { CoherenceAlertBuilder } from '../../shared/coherence-popover/coherence-alert-builder';

/**
 * SF-218-50 : champs d'alerte de cohérence F-IA-03 exposés par l'outil F-DT-80
 * (RTT — acquisition selon accord d'aménagement).
 */
export type RttAcquisitionAlertField = 'HORAIRE';
export type RttAcquisitionCoherenceAlert = CoherenceAlert<RttAcquisitionAlertField>;

/**
 * SF-218-50 — Outil décisionnel « RTT — acquisition selon accord d'aménagement »
 * (F-DT-80-rtt-acquisition).
 *
 * FRANCE uniquement — calcule le nombre théorique de JRTT acquis dans le cadre
 * d'un accord d'aménagement du temps de travail sur l'année (art. L.3121-41 à
 * L.3121-44 CT) : les heures effectuées entre 35 h et l'horaire collectif sont
 * compensées par des JRTT, SANS majoration. À défaut d'accord d'aménagement,
 * l'outil renvoie au régime des heures supplémentaires (F-DT-19).
 *
 * DISTINCT des heures supplémentaires (F-DT-19) et de la monétisation de RTT
 * (F-DT-51) — invariant « un outil = une situation ».
 *
 * Thème INDEMNITES. Conforme F-IA-04 :
 *  - standalone OnPush ; nombreJrttTheorique / baseJuridique en JetBrains Mono ;
 *    badge orange (état neutre RENVOI_HEURES_SUP) — pas de rouge, aucun verdict
 *    défavorable ; bannière gate FR ; MatSnackBar.
 *  - pré-fill IA (horaireHebdomadaireCollectif) depuis `Sf218dDetail` (clé
 *    snake_case `horaire_hebdomadaire_collectif`) + badge `auto_awesome`.
 *  - OnPush + ChangeDetectorRef.markForCheck() dans subscribe (next/error)
 *    cf. feedback_onpush_subscribe_markforcheck.
 *  - F-IA-03 : coherenceAlerts (divergence IA ↔ saisie) via CoherenceAlertBuilder
 *    + popover de divergence.
 */
@Component({
  selector: 'app-rtt-acquisition-section',
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
  templateUrl: './rtt-acquisition-section.component.html',
  styleUrl: './rtt-acquisition-section.component.scss',
})
export class RttAcquisitionSectionComponent implements OnInit, OnChanges {
  static readonly TOOL_LABEL = 'RTT — ACQUISITION (FR)';
  static readonly TOOL_ICON = 'event_available';

  static getPrefillCount(input: PrefillCountInput): number {
    return RttAcquisitionPrefillRules.computePrefillCount(input);
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
  result = signal<RttAcquisitionResponse | null>(null);

  // --- formulaire -----------------------------------------------------------
  horaireHebdomadaireCollectif = signal<number | null>(null);
  accordRttPresent = signal<boolean>(true);
  semainesTravailleesAn = signal<number | null>(null);

  provenanceHoraire = signal<'IA' | null>(null);

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  /**
   * F-IA-03 — alertes de cohérence (non bloquantes) entre l'horaire collectif
   * extrait par l'IA (`horaire_hebdomadaire_collectif`) et la saisie de l'avocat,
   * via `CoherenceAlertBuilder`.
   */
  coherenceAlerts = computed<Partial<Record<RttAcquisitionAlertField, RttAcquisitionCoherenceAlert>>>(() => {
    if (this.standaloneMode || !this.showForm()) return {};
    const alerts: Partial<Record<RttAcquisitionAlertField, RttAcquisitionCoherenceAlert>> = {};
    const horaireAlert = this.buildHoraireAlert();
    if (horaireAlert) alerts.HORAIRE = horaireAlert;
    return alerts;
  });

  constructor(
    private readonly service: RttAcquisitionService,
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

  /**
   * Soumettable dès que l'horaire collectif est renseigné et cohérent (> 35 et
   * ≤ 48), OU dès qu'aucun accord n'est présent (renvoi heures sup).
   */
  formValid(): boolean {
    if (!this.accordRttPresent()) return this.horaireValid();
    return this.horaireValid();
  }

  private horaireValid(): boolean {
    const h = this.horaireHebdomadaireCollectif();
    return h !== null && Number.isFinite(h) && h > 35 && h <= 48;
  }

  onHoraireChange(value: number | null): void {
    this.horaireHebdomadaireCollectif.set(value === null || value === undefined ? null : Number(value));
    this.provenanceHoraire.set(null);
  }

  onAccordChange(value: boolean): void {
    this.accordRttPresent.set(value);
  }

  onSemainesChange(value: number | null): void {
    this.semainesTravailleesAn.set(value === null || value === undefined ? null : Number(value));
  }

  analyze(): void {
    if (!this.formValid()) return;
    const request: RttAcquisitionRequest = {
      horaireHebdomadaireCollectif: this.horaireHebdomadaireCollectif()!,
      accordCollectifPresent: this.accordRttPresent(),
      semainesTravailleesAn: this.semainesTravailleesAn(),
    };
    this.analyzing.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('Analyse acquisition de RTT enregistrée', 'OK', { duration: 2500 });
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

  statutChipClass(statut: RttAcquisitionStatut | null | undefined): string {
    switch (statut) {
      case 'CALCULE':           return 'is-chip is-chip--success';
      case 'RENVOI_HEURES_SUP': return 'is-chip is-chip--warning';
      default:                  return 'is-chip';
    }
  }

  statutLabel(statut: RttAcquisitionStatut | null | undefined): string {
    switch (statut) {
      case 'CALCULE':           return 'JRTT calculés';
      case 'RENVOI_HEURES_SUP': return 'Renvoi heures supplémentaires';
      default:                  return '';
    }
  }

  alertTooltip(alert: RttAcquisitionCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: RttAcquisitionCoherenceAlert): string {
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

  private buildHoraireAlert(): RttAcquisitionCoherenceAlert | null {
    const input: PrefillCountInput = {
      aiData: this.aiData,
      workspaceCountry: this.workspaceCountry,
    };
    const aiHoraire = RttAcquisitionPrefillRules.computeHoraireHebdomadaireCollectif(input);
    const user = this.horaireHebdomadaireCollectif();
    if (aiHoraire === null) return null;
    if (user === null || !Number.isFinite(user)) return null;
    if (aiHoraire === user) return null;
    return CoherenceAlertBuilder.forField<RttAcquisitionAlertField>('HORAIRE')
      .addSource('IA', {
        expectedDisplay: `${aiHoraire} h`,
        reason: `Analyse du dossier : horaire hebdomadaire collectif identifié à ${aiHoraire} h`,
      })
      .build();
  }

  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    const input: PrefillCountInput = {
      aiData: this.aiData,
      workspaceCountry: this.workspaceCountry,
    };

    const horaire = RttAcquisitionPrefillRules.computeHoraireHebdomadaireCollectif(input);
    if (horaire !== null && this.provenanceHoraire() === null && this.horaireHebdomadaireCollectif() === null) {
      this.horaireHebdomadaireCollectif.set(horaire);
      this.provenanceHoraire.set('IA');
    }
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.horaireHebdomadaireCollectif.set(r.horaireHebdomadaireCollectif ?? null);
        this.accordRttPresent.set(r.accordCollectifPresent ?? true);
        this.semainesTravailleesAn.set(r.semainesTravailleesAn ?? null);
        this.provenanceHoraire.set(null);
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
