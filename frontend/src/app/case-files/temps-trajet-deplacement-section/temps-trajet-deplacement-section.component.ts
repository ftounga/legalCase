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

import { TempsTrajetDeplacementService } from '../../core/services/temps-trajet-deplacement.service';
import {
  TempsTrajetDeplacementRequest,
  TempsTrajetDeplacementResponse,
  TempsTrajetQualification,
  TypeTrajet,
} from '../../core/models/temps-trajet-deplacement.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { TempsTrajetDeplacementPrefillRules } from './temps-trajet-deplacement-section-prefill-rules';
import { CoherencePopoverTriggerDirective } from '../../shared/coherence-popover/coherence-popover-trigger.directive';
import { CoherenceAlert } from '../../shared/coherence-popover/coherence-alert.model';
import { CoherenceAlertBuilder } from '../../shared/coherence-popover/coherence-alert-builder';

/**
 * SF-218-52 : champs d'alerte de cohérence F-IA-03 exposés par l'outil F-DT-81
 * (temps de trajet / déplacement professionnel).
 */
export type TempsTrajetAlertField = 'TYPE_TRAJET' | 'QUOTIDIEN';
export type TempsTrajetCoherenceAlert = CoherenceAlert<TempsTrajetAlertField>;

/**
 * SF-218-52 — Outil décisionnel « Temps de trajet / déplacement professionnel »
 * (F-DT-81-temps-trajet-deplacement).
 *
 * FRANCE uniquement — qualifie le temps de trajet professionnel (temps de
 * travail effectif ou non) et détermine si une contrepartie (repos /
 * financière) est due (art. L.3121-4 CT ; CJUE 10/09/2015 C-266/14 « Tyco ») :
 * le trajet domicile ↔ lieu habituel de travail n'est pas du temps de travail
 * effectif ; s'il dépasse le temps normal de trajet, il ouvre droit à une
 * contrepartie sauf si une contrepartie est déjà prévue par accord. Pour un
 * salarié itinérant sans lieu de travail fixe, le déplacement domicile ↔
 * premier/dernier client est qualifié de temps de travail effectif.
 *
 * DISTINCT du remboursement de frais de déplacement et de l'astreinte —
 * invariant « un outil = une situation ».
 *
 * Thème INDEMNITES. Conforme F-IA-04 :
 *  - standalone OnPush ; baseJuridique en JetBrains Mono ; badge contrepartie
 *    DUE (vert) / NON_DUE (rouge) ; badge qualification temps de travail
 *    effectif (vert) / hors temps de travail (gris) ; bannière gate FR ;
 *    MatSnackBar.
 *  - pré-fill IA (typeTrajet, tempsTrajetQuotidienMinutes) depuis `Sf218dDetail`
 *    (clés snake_case `type_trajet` / `temps_trajet_quotidien_minutes`) + badge
 *    `auto_awesome`.
 *  - OnPush + ChangeDetectorRef.markForCheck() dans subscribe (next/error)
 *    cf. feedback_onpush_subscribe_markforcheck.
 *  - F-IA-03 : coherenceAlerts (divergence IA ↔ saisie) via CoherenceAlertBuilder
 *    + popover de divergence.
 */
@Component({
  selector: 'app-temps-trajet-deplacement-section',
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
  templateUrl: './temps-trajet-deplacement-section.component.html',
  styleUrl: './temps-trajet-deplacement-section.component.scss',
})
export class TempsTrajetDeplacementSectionComponent implements OnInit, OnChanges {
  static readonly TOOL_LABEL = 'TEMPS DE TRAJET (FR)';
  static readonly TOOL_ICON = 'directions_car';

  static getPrefillCount(input: PrefillCountInput): number {
    return TempsTrajetDeplacementPrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';
  @Input() aiData?: TravailExtractedData | null;
  @Input() forceExpanded = false;
  @Input() standaloneMode = false;

  readonly typeTrajetOptions: ReadonlyArray<{ value: TypeTrajet; label: string }> = [
    { value: 'DOMICILE_TRAVAIL_HABITUEL', label: 'Domicile ↔ lieu habituel de travail' },
    { value: 'DOMICILE_CLIENT_DEPASSEMENT', label: 'Domicile ↔ client (déplacement professionnel)' },
    { value: 'ITINERANT_SANS_LIEU_FIXE', label: 'Salarié itinérant sans lieu de travail fixe' },
  ];

  collapsed = signal(true);
  loading = signal(false);
  analyzing = signal(false);
  showForm = signal(true);
  result = signal<TempsTrajetDeplacementResponse | null>(null);

  // --- formulaire -----------------------------------------------------------
  typeTrajet = signal<TypeTrajet | null>(null);
  tempsTrajetQuotidienMinutes = signal<number | null>(null);
  tempsTrajetNormalMinutes = signal<number | null>(null);
  contrepartiePrevueAccord = signal<boolean>(false);

  provenanceType = signal<'IA' | null>(null);
  provenanceQuotidien = signal<'IA' | null>(null);

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  /** Le temps normal n'est pas requis pour un salarié itinérant. */
  isItinerant = computed<boolean>(() => this.typeTrajet() === 'ITINERANT_SANS_LIEU_FIXE');

  /**
   * F-IA-03 — alertes de cohérence (non bloquantes) entre le type de trajet /
   * temps quotidien extraits par l'IA et la saisie de l'avocat, via
   * `CoherenceAlertBuilder`.
   */
  coherenceAlerts = computed<Partial<Record<TempsTrajetAlertField, TempsTrajetCoherenceAlert>>>(() => {
    if (this.standaloneMode || !this.showForm()) return {};
    const alerts: Partial<Record<TempsTrajetAlertField, TempsTrajetCoherenceAlert>> = {};
    const typeAlert = this.buildTypeAlert();
    if (typeAlert) alerts.TYPE_TRAJET = typeAlert;
    const quotidienAlert = this.buildQuotidienAlert();
    if (quotidienAlert) alerts.QUOTIDIEN = quotidienAlert;
    return alerts;
  });

  constructor(
    private readonly service: TempsTrajetDeplacementService,
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
   * Soumettable dès qu'un type de trajet est choisi et que le temps quotidien
   * est renseigné (≥ 0). Le temps normal est requis pour les trajets non
   * itinérants (défaut 0 sinon).
   */
  formValid(): boolean {
    if (this.typeTrajet() === null) return false;
    const q = this.tempsTrajetQuotidienMinutes();
    if (q === null || !Number.isFinite(q) || q < 0) return false;
    if (!this.isItinerant()) {
      const n = this.tempsTrajetNormalMinutes();
      if (n === null || !Number.isFinite(n) || n < 0) return false;
    }
    return true;
  }

  onTypeTrajetChange(value: TypeTrajet | null): void {
    this.typeTrajet.set(value ?? null);
    this.provenanceType.set(null);
  }

  onQuotidienChange(value: number | null): void {
    this.tempsTrajetQuotidienMinutes.set(value === null || value === undefined ? null : Number(value));
    this.provenanceQuotidien.set(null);
  }

  onNormalChange(value: number | null): void {
    this.tempsTrajetNormalMinutes.set(value === null || value === undefined ? null : Number(value));
  }

  onContrepartiePrevueChange(value: boolean): void {
    this.contrepartiePrevueAccord.set(value);
  }

  analyze(): void {
    if (!this.formValid()) return;
    const request: TempsTrajetDeplacementRequest = {
      typeTrajet: this.typeTrajet()!,
      tempsTrajetQuotidienMinutes: this.tempsTrajetQuotidienMinutes()!,
      tempsTrajetNormalMinutes: this.isItinerant() ? (this.tempsTrajetNormalMinutes() ?? 0) : this.tempsTrajetNormalMinutes()!,
      contrepartiePrevueAccord: this.contrepartiePrevueAccord(),
    };
    this.analyzing.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('Analyse du temps de trajet enregistrée', 'OK', { duration: 2500 });
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

  typeTrajetLabel(value: TypeTrajet | null | undefined): string {
    return this.typeTrajetOptions.find((o) => o.value === value)?.label ?? '';
  }

  qualificationChipClass(q: TempsTrajetQualification | null | undefined): string {
    switch (q) {
      case 'TEMPS_TRAVAIL':            return 'is-chip is-chip--success';
      case 'TRAJET_AVEC_CONTREPARTIE': return 'is-chip is-chip--info';
      case 'TRAJET_SANS_CONTREPARTIE': return 'is-chip is-chip--neutral';
      default:                         return 'is-chip';
    }
  }

  qualificationLabel(q: TempsTrajetQualification | null | undefined): string {
    switch (q) {
      case 'TEMPS_TRAVAIL':            return 'Temps de travail effectif';
      case 'TRAJET_AVEC_CONTREPARTIE': return 'Trajet avec contrepartie';
      case 'TRAJET_SANS_CONTREPARTIE': return 'Trajet sans contrepartie';
      default:                         return '';
    }
  }

  contrepartieChipClass(due: boolean | null | undefined): string {
    return due ? 'is-chip is-chip--success' : 'is-chip is-chip--danger';
  }

  contrepartieLabel(due: boolean | null | undefined): string {
    return due ? 'Contrepartie DUE' : 'Contrepartie NON DUE';
  }

  alertTooltip(alert: TempsTrajetCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: TempsTrajetCoherenceAlert): string {
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

  private buildTypeAlert(): TempsTrajetCoherenceAlert | null {
    const input: PrefillCountInput = {
      aiData: this.aiData,
      workspaceCountry: this.workspaceCountry,
    };
    const aiType = TempsTrajetDeplacementPrefillRules.computeTypeTrajet(input);
    const user = this.typeTrajet();
    if (aiType === null || user === null) return null;
    if (aiType === user) return null;
    return CoherenceAlertBuilder.forField<TempsTrajetAlertField>('TYPE_TRAJET')
      .addSource('IA', {
        expectedDisplay: this.typeTrajetLabel(aiType),
        reason: `Analyse du dossier : type de trajet identifié comme « ${this.typeTrajetLabel(aiType)} »`,
      })
      .build();
  }

  private buildQuotidienAlert(): TempsTrajetCoherenceAlert | null {
    const input: PrefillCountInput = {
      aiData: this.aiData,
      workspaceCountry: this.workspaceCountry,
    };
    const aiQuotidien = TempsTrajetDeplacementPrefillRules.computeTempsTrajetQuotidienMinutes(input);
    const user = this.tempsTrajetQuotidienMinutes();
    if (aiQuotidien === null) return null;
    if (user === null || !Number.isFinite(user)) return null;
    if (aiQuotidien === user) return null;
    return CoherenceAlertBuilder.forField<TempsTrajetAlertField>('QUOTIDIEN')
      .addSource('IA', {
        expectedDisplay: `${aiQuotidien} min`,
        reason: `Analyse du dossier : temps de trajet quotidien identifié à ${aiQuotidien} min`,
      })
      .build();
  }

  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    const input: PrefillCountInput = {
      aiData: this.aiData,
      workspaceCountry: this.workspaceCountry,
    };

    const type = TempsTrajetDeplacementPrefillRules.computeTypeTrajet(input);
    if (type !== null && this.provenanceType() === null && this.typeTrajet() === null) {
      this.typeTrajet.set(type);
      this.provenanceType.set('IA');
    }

    const quotidien = TempsTrajetDeplacementPrefillRules.computeTempsTrajetQuotidienMinutes(input);
    if (quotidien !== null && this.provenanceQuotidien() === null && this.tempsTrajetQuotidienMinutes() === null) {
      this.tempsTrajetQuotidienMinutes.set(quotidien);
      this.provenanceQuotidien.set('IA');
    }
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.typeTrajet.set(r.typeTrajet ?? null);
        this.tempsTrajetQuotidienMinutes.set(r.tempsTrajetQuotidienMinutes ?? null);
        this.tempsTrajetNormalMinutes.set(r.tempsTrajetNormalMinutes ?? null);
        this.contrepartiePrevueAccord.set(r.contrepartiePrevueAccord ?? false);
        this.provenanceType.set(null);
        this.provenanceQuotidien.set(null);
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
