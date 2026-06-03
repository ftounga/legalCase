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
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';

import { CongesEvenementsFamiliauxService } from '../../core/services/conges-evenements-familiaux.service';
import {
  CongesEvenementsFamiliauxBase,
  CongesEvenementsFamiliauxRequest,
  CongesEvenementsFamiliauxResponse,
  CongesEvenementsFamiliauxTypeEvenement,
} from '../../core/models/conges-evenements-familiaux.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { CongesEvenementsFamiliauxPrefillRules } from './conges-evenements-familiaux-section-prefill-rules';
import { CoherencePopoverTriggerDirective } from '../../shared/coherence-popover/coherence-popover-trigger.directive';
import { CoherenceAlert } from '../../shared/coherence-popover/coherence-alert.model';
import { CoherenceAlertBuilder } from '../../shared/coherence-popover/coherence-alert-builder';

/**
 * SF-218-44 : champs d'alerte de cohérence F-IA-03 exposés par l'outil F-DT-76
 * (congés pour évènements familiaux).
 */
export type CongesEvtFamiliauxAlertField = 'TYPE_EVENEMENT';
export type CongesEvtFamiliauxCoherenceAlert =
  CoherenceAlert<CongesEvtFamiliauxAlertField>;

interface TypeEvenementOption {
  value: CongesEvenementsFamiliauxTypeEvenement;
  label: string;
}

/**
 * SF-218-44 — Outil décisionnel « Congés pour évènements familiaux »
 * (F-DT-76-conges-evenements-familiaux).
 *
 * FRANCE uniquement — détermine la durée de congé applicable pour un évènement
 * familial (art. L.3142-1 à L.3142-5 CT) : durées légales minimales (L.3142-4),
 * durée conventionnelle plus favorable retenue le cas échéant (L.3142-5),
 * maintien intégral du salaire (assimilation à du temps de travail effectif).
 * Distinct du congé de paternité/maternité (F-212) et du congé parental
 * d'éducation (F-DT-78) — invariant « un outil = une situation ».
 *
 * Thème INDEMNITES. Conforme F-IA-04 :
 *  - standalone OnPush ; durée en JetBrains Mono ; badge maintien salaire (vert) ;
 *    base LEGALE / CONVENTIONNELLE ; note majoration décès enfant ; bannière gate
 *    FR ; MatSnackBar.
 *  - pré-fill IA (typeEvenement) depuis `Sf218dDetail` (clé snake_case
 *    `type_evenement_familial`) + badge `auto_awesome` de provenance.
 *  - OnPush + ChangeDetectorRef.markForCheck() dans subscribe (next/error)
 *    cf. feedback_onpush_subscribe_markforcheck.
 *  - F-IA-03 : coherenceAlerts (divergence IA ↔ saisie) via CoherenceAlertBuilder
 *    + popover de divergence.
 */
@Component({
  selector: 'app-conges-evenements-familiaux-section',
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
    MatSelectModule,
    MatProgressSpinnerModule,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './conges-evenements-familiaux-section.component.html',
  styleUrl: './conges-evenements-familiaux-section.component.scss',
})
export class CongesEvenementsFamiliauxSectionComponent implements OnInit, OnChanges {
  static readonly TOOL_LABEL = 'CONGÉS POUR ÉVÈNEMENTS FAMILIAUX (FR)';
  static readonly TOOL_ICON = 'family_restroom';

  static getPrefillCount(input: PrefillCountInput): number {
    return CongesEvenementsFamiliauxPrefillRules.computePrefillCount(input);
  }

  readonly typeOptions: ReadonlyArray<TypeEvenementOption> = [
    { value: 'MARIAGE_PACS', label: 'Mariage / PACS du salarié' },
    { value: 'NAISSANCE', label: 'Naissance / adoption' },
    { value: 'DECES_ENFANT', label: 'Décès d\'un enfant' },
    { value: 'DECES_CONJOINT_PARTENAIRE', label: 'Décès du conjoint / partenaire / concubin' },
    { value: 'DECES_PERE_MERE', label: 'Décès d\'un parent / beau-parent / frère / sœur' },
    { value: 'ANNONCE_HANDICAP_ENFANT', label: 'Annonce d\'un handicap / cancer / pathologie chronique d\'un enfant' },
    { value: 'DEMENAGEMENT_NON_LEGAL', label: 'Déménagement (hors congé légal)' },
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
  result = signal<CongesEvenementsFamiliauxResponse | null>(null);

  // --- formulaire -----------------------------------------------------------
  typeEvenement = signal<CongesEvenementsFamiliauxTypeEvenement | null>(null);
  conventionPlusFavorable = signal<boolean>(false);
  dureeConventionnelleJours = signal<number | null>(null);

  provenanceType = signal<'IA' | null>(null);

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  /**
   * F-IA-03 — alertes de cohérence (non bloquantes) entre la valeur extraite par
   * l'IA (`type_evenement_familial`) et la saisie de l'avocat, via
   * `CoherenceAlertBuilder`.
   */
  coherenceAlerts = computed<Partial<Record<CongesEvtFamiliauxAlertField, CongesEvtFamiliauxCoherenceAlert>>>(() => {
    if (this.standaloneMode || !this.showForm()) return {};
    const alerts: Partial<Record<CongesEvtFamiliauxAlertField, CongesEvtFamiliauxCoherenceAlert>> = {};
    const typeAlert = this.buildTypeAlert();
    if (typeAlert) alerts.TYPE_EVENEMENT = typeAlert;
    return alerts;
  });

  constructor(
    private readonly service: CongesEvenementsFamiliauxService,
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

  /** Soumettable dès qu'un type d'évènement est sélectionné. */
  formValid(): boolean {
    if (this.typeEvenement() === null) return false;
    if (this.conventionPlusFavorable()) {
      const d = this.dureeConventionnelleJours();
      return d !== null && Number.isFinite(d) && d > 0;
    }
    return true;
  }

  onTypeChange(value: CongesEvenementsFamiliauxTypeEvenement | null): void {
    this.typeEvenement.set(value ?? null);
    this.provenanceType.set(null);
  }

  onConventionChange(value: boolean): void {
    this.conventionPlusFavorable.set(!!value);
    if (!value) this.dureeConventionnelleJours.set(null);
  }

  onDureeConventionnelleChange(value: number | null): void {
    this.dureeConventionnelleJours.set(value === null || value === undefined ? null : Number(value));
  }

  analyze(): void {
    if (!this.formValid()) return;
    const request: CongesEvenementsFamiliauxRequest = {
      typeEvenement: this.typeEvenement()!,
      conventionPlusFavorable: this.conventionPlusFavorable(),
      dureeConventionnelleJours: this.conventionPlusFavorable()
        ? this.dureeConventionnelleJours()
        : null,
    };
    this.analyzing.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('Analyse congé pour évènement familial enregistrée', 'OK', { duration: 2500 });
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

  typeLabel(t: CongesEvenementsFamiliauxTypeEvenement | null | undefined): string {
    return this.typeOptions.find((o) => o.value === t)?.label ?? '';
  }

  baseLabel(b: CongesEvenementsFamiliauxBase | null | undefined): string {
    switch (b) {
      case 'LEGALE':         return 'Durée légale (Code du travail)';
      case 'CONVENTIONNELLE': return 'Durée conventionnelle (CCN plus favorable)';
      default:                return '';
    }
  }

  baseChipClass(b: CongesEvenementsFamiliauxBase | null | undefined): string {
    switch (b) {
      case 'CONVENTIONNELLE': return 'is-chip is-chip--info is-mono';
      case 'LEGALE':          return 'is-chip is-chip--neutral is-mono';
      default:                return 'is-chip is-mono';
    }
  }

  alertTooltip(alert: CongesEvtFamiliauxCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: CongesEvtFamiliauxCoherenceAlert): string {
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

  private buildTypeAlert(): CongesEvtFamiliauxCoherenceAlert | null {
    const input: PrefillCountInput = {
      aiData: this.aiData,
      workspaceCountry: this.workspaceCountry,
    };
    const aiType = CongesEvenementsFamiliauxPrefillRules.computeTypeEvenement(input);
    const user = this.typeEvenement();
    if (aiType === null) return null;
    if (user === null) return null;
    if (aiType === user) return null;
    return CoherenceAlertBuilder.forField<CongesEvtFamiliauxAlertField>('TYPE_EVENEMENT')
      .addSource('IA', {
        expectedDisplay: this.typeLabel(aiType),
        reason: `Analyse du dossier : évènement familial identifié « ${this.typeLabel(aiType)} »`,
      })
      .build();
  }

  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    const input: PrefillCountInput = {
      aiData: this.aiData,
      workspaceCountry: this.workspaceCountry,
    };

    const type = CongesEvenementsFamiliauxPrefillRules.computeTypeEvenement(input);
    if (type !== null && this.provenanceType() === null && this.typeEvenement() === null) {
      this.typeEvenement.set(type);
      this.provenanceType.set('IA');
    }
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.typeEvenement.set(r.typeEvenement ?? null);
        this.conventionPlusFavorable.set(r.conventionPlusFavorable ?? false);
        this.dureeConventionnelleJours.set(r.dureeConventionnelleJours ?? null);
        this.provenanceType.set(null);
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
