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
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';

import { CongeParentalEducationService } from '../../core/services/conge-parental-education.service';
import {
  CongeParentalEducationModalite,
  CongeParentalEducationRequest,
  CongeParentalEducationResponse,
} from '../../core/models/conge-parental-education.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { CongeParentalEducationPrefillRules } from './conge-parental-education-section-prefill-rules';
import { CoherencePopoverTriggerDirective } from '../../shared/coherence-popover/coherence-popover-trigger.directive';
import { CoherenceAlert } from '../../shared/coherence-popover/coherence-alert.model';
import { CoherenceAlertBuilder } from '../../shared/coherence-popover/coherence-alert-builder';

/**
 * SF-218-46 : champs d'alerte de cohérence F-IA-03 exposés par l'outil F-DT-78
 * (congé parental d'éducation).
 */
export type CongeParentalAlertField = 'DATE_NAISSANCE';
export type CongeParentalCoherenceAlert = CoherenceAlert<CongeParentalAlertField>;

interface ModaliteOption {
  value: CongeParentalEducationModalite;
  label: string;
}

/**
 * SF-218-46 — Outil décisionnel « Congé parental d'éducation »
 * (F-DT-78-conge-parental-education).
 *
 * FRANCE uniquement — détermine l'éligibilité au congé parental d'éducation
 * (art. L.1225-47 à L.1225-60 CT : un an d'ancienneté minimum à la naissance /
 * adoption) et la date de fin maximale du droit (3e anniversaire de l'enfant).
 * Distinct du congé de paternité/maternité (F-212) et du congé pour évènements
 * familiaux (F-DT-76) — invariant « un outil = une situation ».
 *
 * Thème DIAGNOSTIC. Conforme F-IA-04 :
 *  - standalone OnPush ; dateFinMax / baseJuridique en JetBrains Mono ; badge
 *    ELIGIBLE (vert) / NON_ELIGIBLE (rouge) ; note protection / PreParE ;
 *    bannière gate FR ; MatSnackBar.
 *  - pré-fill IA (dateNaissanceOuAdoption) depuis `Sf218dDetail` (clé snake_case
 *    `date_naissance_ou_adoption`) + badge `auto_awesome` de provenance.
 *  - OnPush + ChangeDetectorRef.markForCheck() dans subscribe (next/error)
 *    cf. feedback_onpush_subscribe_markforcheck.
 *  - F-IA-03 : coherenceAlerts (divergence IA ↔ saisie) via CoherenceAlertBuilder
 *    + popover de divergence.
 */
@Component({
  selector: 'app-conge-parental-education-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatProgressSpinnerModule,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './conge-parental-education-section.component.html',
  styleUrl: './conge-parental-education-section.component.scss',
})
export class CongeParentalEducationSectionComponent implements OnInit, OnChanges {
  static readonly TOOL_LABEL = 'CONGÉ PARENTAL D\'ÉDUCATION (FR)';
  static readonly TOOL_ICON = 'child_care';

  static getPrefillCount(input: PrefillCountInput): number {
    return CongeParentalEducationPrefillRules.computePrefillCount(input);
  }

  readonly modaliteOptions: ReadonlyArray<ModaliteOption> = [
    { value: 'TEMPS_PLEIN', label: 'Congé total (suspension du contrat)' },
    { value: 'TEMPS_PARTIEL', label: 'Activité à temps partiel' },
  ];

  @Input() caseFileId!: string;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';
  @Input() aiData?: TravailExtractedData | null;
  @Input() forceExpanded = false;
  @Input() standaloneMode = false;

  collapsed = signal(true);
  loading = signal(false);
  analyzing = signal(false);
  showForm = signal(true);
  result = signal<CongeParentalEducationResponse | null>(null);

  // --- formulaire -----------------------------------------------------------
  ancienneteMois = signal<number | null>(null);
  modalite = signal<CongeParentalEducationModalite>('TEMPS_PLEIN');
  nombreEnfants = signal<number>(1);
  dateNaissanceOuAdoption = signal<string | null>(null);

  provenanceDate = signal<'IA' | null>(null);

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  /**
   * F-IA-03 — alertes de cohérence (non bloquantes) entre la date extraite par
   * l'IA (`date_naissance_ou_adoption`) et la saisie de l'avocat, via
   * `CoherenceAlertBuilder`.
   */
  coherenceAlerts = computed<Partial<Record<CongeParentalAlertField, CongeParentalCoherenceAlert>>>(() => {
    if (this.standaloneMode || !this.showForm()) return {};
    const alerts: Partial<Record<CongeParentalAlertField, CongeParentalCoherenceAlert>> = {};
    const dateAlert = this.buildDateAlert();
    if (dateAlert) alerts.DATE_NAISSANCE = dateAlert;
    return alerts;
  });

  constructor(
    private readonly service: CongeParentalEducationService,
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

  /** Soumettable dès qu'ancienneté (≥ 0), date et nombre d'enfants (≥ 1) sont renseignés. */
  formValid(): boolean {
    const a = this.ancienneteMois();
    if (a === null || !Number.isFinite(a) || a < 0) return false;
    const n = this.nombreEnfants();
    if (n === null || !Number.isFinite(n) || n < 1) return false;
    if (!this.dateNaissanceOuAdoption()) return false;
    return true;
  }

  onAncienneteChange(value: number | null): void {
    this.ancienneteMois.set(value === null || value === undefined ? null : Number(value));
  }

  onModaliteChange(value: CongeParentalEducationModalite): void {
    this.modalite.set(value);
  }

  onNombreEnfantsChange(value: number | null): void {
    this.nombreEnfants.set(value === null || value === undefined ? 1 : Number(value));
  }

  onDateChange(value: string | null): void {
    this.dateNaissanceOuAdoption.set(value || null);
    this.provenanceDate.set(null);
  }

  analyze(): void {
    if (!this.formValid()) return;
    const request: CongeParentalEducationRequest = {
      ancienneteMois: this.ancienneteMois()!,
      modalite: this.modalite(),
      nombreEnfants: this.nombreEnfants(),
      dateNaissanceOuAdoption: this.dateNaissanceOuAdoption()!,
    };
    this.analyzing.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('Analyse congé parental d\'éducation enregistrée', 'OK', { duration: 2500 });
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

  modaliteLabel(m: CongeParentalEducationModalite | null | undefined): string {
    return this.modaliteOptions.find((o) => o.value === m)?.label ?? '';
  }

  statutChipClass(statut: 'ELIGIBLE' | 'NON_ELIGIBLE' | null | undefined): string {
    switch (statut) {
      case 'ELIGIBLE':     return 'is-chip is-chip--success';
      case 'NON_ELIGIBLE': return 'is-chip is-chip--danger';
      default:             return 'is-chip';
    }
  }

  statutLabel(statut: 'ELIGIBLE' | 'NON_ELIGIBLE' | null | undefined): string {
    switch (statut) {
      case 'ELIGIBLE':     return 'Éligible';
      case 'NON_ELIGIBLE': return 'Non éligible';
      default:             return '';
    }
  }

  alertTooltip(alert: CongeParentalCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: CongeParentalCoherenceAlert): string {
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

  private buildDateAlert(): CongeParentalCoherenceAlert | null {
    const input: PrefillCountInput = {
      aiData: this.aiData,
      workspaceCountry: this.workspaceCountry,
    };
    const aiDate = CongeParentalEducationPrefillRules.computeDateNaissanceOuAdoption(input);
    const user = this.dateNaissanceOuAdoption();
    if (aiDate === null) return null;
    if (!user) return null;
    if (aiDate === user) return null;
    return CoherenceAlertBuilder.forField<CongeParentalAlertField>('DATE_NAISSANCE')
      .addSource('IA', {
        expectedDisplay: aiDate,
        reason: `Analyse du dossier : naissance / arrivée de l'enfant identifiée le ${aiDate}`,
      })
      .build();
  }

  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    const input: PrefillCountInput = {
      aiData: this.aiData,
      workspaceCountry: this.workspaceCountry,
    };

    const date = CongeParentalEducationPrefillRules.computeDateNaissanceOuAdoption(input);
    if (date !== null && this.provenanceDate() === null && this.dateNaissanceOuAdoption() === null) {
      this.dateNaissanceOuAdoption.set(date);
      this.provenanceDate.set('IA');
    }
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.ancienneteMois.set(r.ancienneteMois ?? null);
        this.modalite.set(r.modaliteRetenue ?? 'TEMPS_PLEIN');
        this.nombreEnfants.set(r.nombreEnfants ?? 1);
        this.dateNaissanceOuAdoption.set(r.dateNaissanceOuAdoption ?? null);
        this.provenanceDate.set(null);
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
