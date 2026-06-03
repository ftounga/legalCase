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
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';

import { CongeProcheAidantService } from '../../core/services/conge-proche-aidant.service';
import {
  CongeProcheAidantLien,
  CongeProcheAidantRequest,
  CongeProcheAidantResponse,
} from '../../core/models/conge-proche-aidant.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { CongeProcheAidantPrefillRules } from './conge-proche-aidant-section-prefill-rules';
import { CoherencePopoverTriggerDirective } from '../../shared/coherence-popover/coherence-popover-trigger.directive';
import { CoherenceAlert } from '../../shared/coherence-popover/coherence-alert.model';
import { CoherenceAlertBuilder } from '../../shared/coherence-popover/coherence-alert-builder';

/**
 * SF-218-48 : champs d'alerte de cohérence F-IA-03 exposés par l'outil F-DT-79
 * (congé de proche aidant).
 */
export type CongeProcheAidantAlertField = 'LIEN';
export type CongeProcheAidantCoherenceAlert = CoherenceAlert<CongeProcheAidantAlertField>;

interface LienOption {
  value: CongeProcheAidantLien;
  label: string;
}

/**
 * SF-218-48 — Outil décisionnel « Congé de proche aidant »
 * (F-DT-79-conge-proche-aidant).
 *
 * FRANCE uniquement — détermine l'éligibilité au congé de proche aidant
 * (art. L.3142-16 à L.3142-27 CT, loi n° 2020-220 du 06/03/2020 : la personne
 * aidée doit résider en France/EEE), la durée maximale (12 mois sur la carrière,
 * L.3142-19) et une estimation indicative de l'AJPA versée par la CAF (plafond
 * 66 jours indemnisés). Distinct du congé parental d'éducation (F-DT-78) et du
 * congé pour évènements familiaux (F-DT-76) — invariant « un outil = une
 * situation ».
 *
 * Thème INDEMNITES. Conforme F-IA-04 :
 *  - standalone OnPush ; dureeMaxMois / estimationAjpa / baseJuridique en
 *    JetBrains Mono ; badge ELIGIBLE (vert) / NON_ELIGIBLE (rouge) ; mention
 *    « estimation indicative » + disclaimer « montant à vérifier » ; bannière
 *    gate FR ; MatSnackBar.
 *  - pré-fill IA (lienPersonneAidee) depuis `Sf218dDetail` (clé snake_case
 *    `lien_personne_aidee`) + badge `auto_awesome` de provenance.
 *  - OnPush + ChangeDetectorRef.markForCheck() dans subscribe (next/error)
 *    cf. feedback_onpush_subscribe_markforcheck.
 *  - F-IA-03 : coherenceAlerts (divergence IA ↔ saisie) via CoherenceAlertBuilder
 *    + popover de divergence.
 */
@Component({
  selector: 'app-conge-proche-aidant-section',
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
    MatCheckboxModule,
    MatProgressSpinnerModule,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './conge-proche-aidant-section.component.html',
  styleUrl: './conge-proche-aidant-section.component.scss',
})
export class CongeProcheAidantSectionComponent implements OnInit, OnChanges {
  static readonly TOOL_LABEL = 'CONGÉ DE PROCHE AIDANT (FR)';
  static readonly TOOL_ICON = 'volunteer_activism';

  static getPrefillCount(input: PrefillCountInput): number {
    return CongeProcheAidantPrefillRules.computePrefillCount(input);
  }

  readonly lienOptions: ReadonlyArray<LienOption> = [
    { value: 'CONJOINT', label: 'Conjoint, concubin ou partenaire de PACS' },
    { value: 'ASCENDANT', label: 'Ascendant (parent, grand-parent…)' },
    { value: 'DESCENDANT', label: 'Descendant (enfant, petit-enfant…)' },
    { value: 'COLLATERAL', label: 'Collatéral jusqu\'au 4e degré' },
    { value: 'SANS_LIEN_RESIDENCE_COMMUNE', label: 'Sans lien — résidence / liens étroits et stables' },
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
  result = signal<CongeProcheAidantResponse | null>(null);

  // --- formulaire -----------------------------------------------------------
  lienPersonneAidee = signal<CongeProcheAidantLien | null>(null);
  personneAideeResideFrance = signal<boolean>(true);
  dureeSouhaiteeMois = signal<number | null>(null);
  ajpaDemandee = signal<boolean>(false);

  provenanceLien = signal<'IA' | null>(null);

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  /**
   * F-IA-03 — alertes de cohérence (non bloquantes) entre le lien extrait par
   * l'IA (`lien_personne_aidee`) et la saisie de l'avocat, via
   * `CoherenceAlertBuilder`.
   */
  coherenceAlerts = computed<Partial<Record<CongeProcheAidantAlertField, CongeProcheAidantCoherenceAlert>>>(() => {
    if (this.standaloneMode || !this.showForm()) return {};
    const alerts: Partial<Record<CongeProcheAidantAlertField, CongeProcheAidantCoherenceAlert>> = {};
    const lienAlert = this.buildLienAlert();
    if (lienAlert) alerts.LIEN = lienAlert;
    return alerts;
  });

  constructor(
    private readonly service: CongeProcheAidantService,
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

  /** Soumettable dès que le lien et une durée (> 0) sont renseignés. */
  formValid(): boolean {
    if (!this.lienPersonneAidee()) return false;
    const d = this.dureeSouhaiteeMois();
    if (d === null || !Number.isFinite(d) || d <= 0) return false;
    return true;
  }

  onLienChange(value: CongeProcheAidantLien): void {
    this.lienPersonneAidee.set(value);
    this.provenanceLien.set(null);
  }

  onResideFranceChange(value: boolean): void {
    this.personneAideeResideFrance.set(value);
  }

  onDureeChange(value: number | null): void {
    this.dureeSouhaiteeMois.set(value === null || value === undefined ? null : Number(value));
  }

  onAjpaChange(value: boolean): void {
    this.ajpaDemandee.set(value);
  }

  analyze(): void {
    if (!this.formValid()) return;
    const request: CongeProcheAidantRequest = {
      lienPersonneAidee: this.lienPersonneAidee()!,
      personneAideeResideFrance: this.personneAideeResideFrance(),
      dureeSouhaiteeMois: this.dureeSouhaiteeMois()!,
      ajpaDemandee: this.ajpaDemandee(),
    };
    this.analyzing.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('Analyse congé de proche aidant enregistrée', 'OK', { duration: 2500 });
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

  lienLabel(l: CongeProcheAidantLien | null | undefined): string {
    return this.lienOptions.find((o) => o.value === l)?.label ?? '';
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

  alertTooltip(alert: CongeProcheAidantCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: CongeProcheAidantCoherenceAlert): string {
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

  private buildLienAlert(): CongeProcheAidantCoherenceAlert | null {
    const input: PrefillCountInput = {
      aiData: this.aiData,
      workspaceCountry: this.workspaceCountry,
    };
    const aiLien = CongeProcheAidantPrefillRules.computeLienPersonneAidee(input);
    const user = this.lienPersonneAidee();
    if (aiLien === null) return null;
    if (!user) return null;
    if (aiLien === user) return null;
    return CoherenceAlertBuilder.forField<CongeProcheAidantAlertField>('LIEN')
      .addSource('IA', {
        expectedDisplay: this.lienLabel(aiLien),
        reason: `Analyse du dossier : lien avec la personne aidée identifié comme ${this.lienLabel(aiLien)}`,
      })
      .build();
  }

  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    const input: PrefillCountInput = {
      aiData: this.aiData,
      workspaceCountry: this.workspaceCountry,
    };

    const lien = CongeProcheAidantPrefillRules.computeLienPersonneAidee(input);
    if (lien !== null && this.provenanceLien() === null && this.lienPersonneAidee() === null) {
      this.lienPersonneAidee.set(lien);
      this.provenanceLien.set('IA');
    }
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.lienPersonneAidee.set(r.lienPersonneAidee ?? null);
        this.personneAideeResideFrance.set(r.personneAideeResideFrance ?? true);
        this.dureeSouhaiteeMois.set(r.dureeSouhaiteeMois ?? null);
        this.ajpaDemandee.set(r.ajpaDemandee ?? false);
        this.provenanceLien.set(null);
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
