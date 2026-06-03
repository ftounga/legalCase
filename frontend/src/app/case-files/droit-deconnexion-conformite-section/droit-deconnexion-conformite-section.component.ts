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

import { DroitDeconnexionConformiteService } from '../../core/services/droit-deconnexion-conformite.service';
import {
  DroitDeconnexionConformiteRequest,
  DroitDeconnexionConformiteResponse,
  DroitDeconnexionConformiteStatut,
} from '../../core/models/droit-deconnexion-conformite.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { DroitDeconnexionConformitePrefillRules } from './droit-deconnexion-conformite-section-prefill-rules';
import { CoherencePopoverTriggerDirective } from '../../shared/coherence-popover/coherence-popover-trigger.directive';
import { CoherenceAlert } from '../../shared/coherence-popover/coherence-alert.model';
import { CoherenceAlertBuilder } from '../../shared/coherence-popover/coherence-alert-builder';

/**
 * SF-218-54 : champs d'alerte de cohérence F-IA-03 exposés par l'outil F-DT-83
 * (droit à la déconnexion — conformité).
 */
export type DroitDeconnexionAlertField = 'ACCORD_OU_CHARTE';
export type DroitDeconnexionCoherenceAlert =
  CoherenceAlert<DroitDeconnexionAlertField>;

/** Seuil d'effectif déclenchant l'obligation de négocier (prévisible UI). */
const SEUIL_OBLIGATION_NEGOCIER = 50;

/**
 * SF-218-54 — Outil décisionnel « Droit à la déconnexion — conformité »
 * (F-DT-83-droit-deconnexion-conformite).
 *
 * FRANCE uniquement — analyseur de conformité à l'obligation relative au droit à
 * la déconnexion (art. L.2242-17 7° CT) : pour les entreprises d'au moins 50
 * salariés dotées d'au moins un délégué syndical, le droit à la déconnexion doit
 * être négocié dans la NAO QVCT ; à défaut d'accord, l'employeur élabore une
 * charte, après avis du CSE, prévoyant des actions de formation et de
 * sensibilisation. Distinct de la NAO dans son ensemble (F-DT-66) et de la
 * désignation du délégué syndical (F-DT-69) — invariant « un outil = une
 * situation ».
 *
 * Verdict de conformité (3 états) :
 *  - CONFORME (vert) : obligation applicable, tous les items remplis.
 *  - NON_CONFORME (rouge) : obligation applicable, au moins un item manquant.
 *  - NON_REQUIS (gris) : obligation de négocier non déclenchée.
 *
 * Thème DIAGNOSTIC. Conforme F-IA-04 :
 *  - standalone OnPush ; rouge réservé NON_CONFORME / item manquant ; vert
 *    conforme ; gris NON_REQUIS ; JetBrains Mono compteur / baseJuridique ;
 *    bannière gate FR ; MatSnackBar.
 *  - pré-fill IA (accordOuChartePresent) depuis `Sf218dDetail` (clé snake_case
 *    `accord_deconnexion_present`) + badge `auto_awesome` de provenance.
 *  - OnPush + ChangeDetectorRef.markForCheck() dans subscribe (next/error)
 *    cf. feedback_onpush_subscribe_markforcheck.
 *  - F-IA-03 : coherenceAlerts (divergence IA ↔ saisie) via CoherenceAlertBuilder
 *    + popover de divergence.
 */
@Component({
  selector: 'app-droit-deconnexion-conformite-section',
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
  templateUrl: './droit-deconnexion-conformite-section.component.html',
  styleUrl: './droit-deconnexion-conformite-section.component.scss',
})
export class DroitDeconnexionConformiteSectionComponent implements OnInit, OnChanges {
  static readonly TOOL_LABEL = 'DROIT À LA DÉCONNEXION — CONFORMITÉ (FR)';
  static readonly TOOL_ICON = 'phonelink_erase';

  static getPrefillCount(input: PrefillCountInput): number {
    return DroitDeconnexionConformitePrefillRules.computePrefillCount(input);
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
  result = signal<DroitDeconnexionConformiteResponse | null>(null);

  // --- formulaire -----------------------------------------------------------
  effectif = signal<number | null>(null);
  delegueSyndicalPresent = signal<boolean>(false);
  accordOuChartePresent = signal<boolean>(false);
  plagesDeconnexionDefinies = signal<boolean>(false);
  actionsSensibilisation = signal<boolean>(false);
  avisCseRecueilliPourCharte = signal<boolean>(false);

  provenanceAccordOuCharte = signal<'IA' | null>(null);

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  /** Obligation de négocier prévisible côté UI (effectif ≥ 50 ET DS) avant calcul. */
  obligationPrevisible = computed<boolean>(() => {
    const e = this.effectif();
    return e !== null && Number.isFinite(e) && e >= SEUIL_OBLIGATION_NEGOCIER
      && this.delegueSyndicalPresent();
  });

  /**
   * F-IA-03 — alertes de cohérence (non bloquantes) entre la valeur extraite par
   * l'IA (sous-record `Sf218dDetail`) et la saisie de l'avocat, via
   * `CoherenceAlertBuilder` (valeur différente pour le booléen accord/charte).
   */
  coherenceAlerts = computed<Partial<Record<DroitDeconnexionAlertField, DroitDeconnexionCoherenceAlert>>>(() => {
    if (this.standaloneMode || !this.showForm()) return {};
    const alerts: Partial<Record<DroitDeconnexionAlertField, DroitDeconnexionCoherenceAlert>> = {};
    const accordAlert = this.buildAccordOuCharteAlert();
    if (accordAlert) alerts.ACCORD_OU_CHARTE = accordAlert;
    return alerts;
  });

  constructor(
    private readonly service: DroitDeconnexionConformiteService,
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

  onDelegueSyndicalChange(value: boolean): void {
    this.delegueSyndicalPresent.set(!!value);
  }

  onAccordOuCharteChange(value: boolean): void {
    this.accordOuChartePresent.set(!!value);
    this.provenanceAccordOuCharte.set(null);
  }

  onPlagesChange(value: boolean): void {
    this.plagesDeconnexionDefinies.set(!!value);
  }

  onSensibilisationChange(value: boolean): void {
    this.actionsSensibilisation.set(!!value);
  }

  onAvisCseChange(value: boolean): void {
    this.avisCseRecueilliPourCharte.set(!!value);
  }

  analyze(): void {
    if (!this.formValid()) return;
    const request: DroitDeconnexionConformiteRequest = {
      effectif: this.effectif()!,
      delegueSyndicalPresent: this.delegueSyndicalPresent(),
      accordOuChartePresent: this.accordOuChartePresent(),
      plagesDeconnexionDefinies: this.plagesDeconnexionDefinies(),
      actionsSensibilisation: this.actionsSensibilisation(),
      avisCseRecueilliPourCharte: this.avisCseRecueilliPourCharte(),
    };
    this.analyzing.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('Analyse conformité droit à la déconnexion enregistrée', 'OK', { duration: 2500 });
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

  statutLabel(s: DroitDeconnexionConformiteStatut | null | undefined): string {
    switch (s) {
      case 'CONFORME':     return 'Obligation relative au droit à la déconnexion respectée';
      case 'NON_CONFORME': return 'Obligation relative au droit à la déconnexion non remplie';
      case 'NON_REQUIS':   return 'Obligation de négociation non applicable';
      default:             return '';
    }
  }

  statutChipClass(s: DroitDeconnexionConformiteStatut | null | undefined): string {
    switch (s) {
      case 'CONFORME':     return 'is-chip is-chip--success';
      case 'NON_CONFORME': return 'is-chip is-chip--danger';
      case 'NON_REQUIS':   return 'is-chip is-chip--neutral';
      default:             return 'is-chip';
    }
  }

  bannerClass(s: DroitDeconnexionConformiteStatut | null | undefined): string {
    switch (s) {
      case 'CONFORME':     return 'is-banner is-banner--success';
      case 'NON_CONFORME': return 'is-banner is-banner--danger';
      case 'NON_REQUIS':   return 'is-banner is-banner--neutral';
      default:             return 'is-banner is-banner--navy';
    }
  }

  bannerIcon(s: DroitDeconnexionConformiteStatut | null | undefined): string {
    switch (s) {
      case 'CONFORME':     return 'verified';
      case 'NON_CONFORME': return 'gpp_bad';
      case 'NON_REQUIS':   return 'info_outline';
      default:             return 'info_outline';
    }
  }

  alertTooltip(alert: DroitDeconnexionCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: DroitDeconnexionCoherenceAlert): string {
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

  private buildAccordOuCharteAlert(): DroitDeconnexionCoherenceAlert | null {
    const ai = this.aiData?.accord_deconnexion_present;
    const user = this.accordOuChartePresent();
    if (typeof ai !== 'boolean') return null;
    if (ai === user) return null;
    return CoherenceAlertBuilder.forField<DroitDeconnexionAlertField>('ACCORD_OU_CHARTE')
      .addSource('IA', {
        expectedDisplay: ai ? 'Accord / charte présent' : 'Pas d\'accord / charte',
        reason: `Analyse du dossier : accord ou charte de déconnexion ${ai ? 'détecté' : 'non détecté'}`,
      })
      .build();
  }

  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    const input: PrefillCountInput = {
      aiData: this.aiData,
      workspaceCountry: this.workspaceCountry,
    };

    const accordOuCharte = DroitDeconnexionConformitePrefillRules.computeAccordOuChartePresent(input);
    if (accordOuCharte !== null && this.provenanceAccordOuCharte() === null && !this.accordOuChartePresent()) {
      this.accordOuChartePresent.set(accordOuCharte);
      this.provenanceAccordOuCharte.set('IA');
    }
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.effectif.set(r.effectif ?? null);
        this.delegueSyndicalPresent.set(r.delegueSyndicalPresent ?? false);
        this.accordOuChartePresent.set(r.accordOuChartePresent ?? false);
        this.plagesDeconnexionDefinies.set(r.plagesDeconnexionDefinies ?? false);
        this.actionsSensibilisation.set(r.actionsSensibilisation ?? false);
        this.avisCseRecueilliPourCharte.set(r.avisCseRecueilliPourCharte ?? false);
        this.provenanceAccordOuCharte.set(null);
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
