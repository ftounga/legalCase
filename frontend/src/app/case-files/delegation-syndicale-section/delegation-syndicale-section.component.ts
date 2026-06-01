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
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';

import { DelegationSyndicaleService } from '../../core/services/delegation-syndicale.service';
import {
  DelegationSyndicaleRequest,
  DelegationSyndicaleResponse,
  DelegationSyndicaleRisqueNullite,
  DelegationSyndicaleStatutDesignation,
  MandatSyndicalType,
} from '../../core/models/delegation-syndicale.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { DelegationSyndicalePrefillRules } from './delegation-syndicale-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-218-34 — Outil décisionnel « Délégué syndical / RSS : désignation et
 * protection » (F-DT-69-delegation-syndicale-protection).
 *
 * FRANCE uniquement — analyseur de statut + protection : régularité de la
 * désignation d'un délégué syndical (DS) ou d'un représentant de section
 * syndicale (RSS) (effectif ≥ 50 pour le DS, représentativité de l'organisation
 * désignante, score personnel du candidat ≥ 10 % pour le DS, art. L.2143-1 et
 * s., L.2142-1-1, L.2143-3 CT) et risque de nullité d'un licenciement de salarié
 * protégé prononcé sans autorisation préalable de l'inspecteur du travail
 * (art. L.2411-3 CT — nullité + droit à réintégration).
 *
 * Verdict de régularité (3 états) :
 *  - REGULIERE (vert) : tous les items conformes.
 *  - IRREGULIERE (rouge) : item d'effectif / représentativité (ou score DS < 10 %)
 *    non conforme.
 *  - A_VERIFIER (orange) : DS sans score personnel renseigné (condition des 10 %
 *    à confirmer).
 *
 * Protection : DS et RSS sont des salariés protégés (statutProtege = OUI). Risque
 * de nullité du licenciement : ELEVE (rouge) sans autorisation / FAIBLE (vert)
 * avec autorisation / SANS_OBJET (gris) si aucun licenciement envisagé.
 *
 * Conforme F-IA-04 / ai-skills/frontend-coherence-audit.md :
 *  - standalone OnPush, palette navy/or ; rouge réservé IRREGULIERE / risque
 *    ELEVE ; orange A_VERIFIER ; gris SANS_OBJET ; vert REGULIERE / risque
 *    FAIBLE ; JetBrains Mono score / baseJuridique ; bannière gate FR ;
 *    MatSnackBar.
 *  - pré-fill IA (effectif, typeMandat) depuis TravailExtractedData + badge
 *    `auto_awesome` de provenance.
 *  - OnPush + ChangeDetectorRef.markForCheck() dans subscribe (next/error)
 *    cf. feedback_onpush_subscribe_markforcheck (mémoire utilisateur).
 *  - F-IA-03 : coherenceAlerts (DS sans organisation représentative ; RSS désigné
 *    par une organisation représentative ; licenciement envisagé sans autorisation
 *    inspecteur du travail).
 */
@Component({
  selector: 'app-delegation-syndicale-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatProgressSpinnerModule,
    ToolJurisprudenceCitationsComponent,
  ],
  templateUrl: './delegation-syndicale-section.component.html',
  styleUrl: './delegation-syndicale-section.component.scss',
})
export class DelegationSyndicaleSectionComponent implements OnInit, OnChanges {
  // F-JU-03 SF-JU-03-99c — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-DT-69-delegation-syndicale-protection';
  protected readonly brancheActiveForJurisprudence = 'default';

  static readonly TOOL_LABEL = 'DÉLÉGUÉ SYNDICAL / RSS (FR)';
  static readonly TOOL_ICON = 'diversity_3';

  static getPrefillCount(input: PrefillCountInput): number {
    return DelegationSyndicalePrefillRules.computePrefillCount(input);
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
  result = signal<DelegationSyndicaleResponse | null>(null);

  // --- formulaire -----------------------------------------------------------
  effectif = signal<number | null>(null);
  typeMandat = signal<MandatSyndicalType>('DELEGUE_SYNDICAL');
  syndicatRepresentatif = signal<boolean>(false);
  pourcentageScorePersonnel = signal<number | null>(null);
  dateDesignation = signal<string | null>(null);
  licenciementEnvisage = signal<boolean>(false);
  autorisationInspecteurTravail = signal<boolean>(false);

  provenanceEffectif = signal<'IA' | null>(null);
  provenanceTypeMandat = signal<'IA' | null>(null);

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');
  isDs = computed<boolean>(() => this.typeMandat() === 'DELEGUE_SYNDICAL');

  /**
   * F-IA-03 — alertes de cohérence (non bloquantes) :
   *  - DS désigné par une organisation non représentative ;
   *  - RSS désigné par une organisation représentative (incohérence du mandat) ;
   *  - licenciement envisagé sans autorisation de l'inspecteur du travail.
   */
  coherenceAlerts = computed<string[]>(() => {
    const alerts: string[] = [];
    const ds = this.isDs();
    const representatif = this.syndicatRepresentatif();
    if (ds && !representatif) {
      alerts.push(
        'Le délégué syndical doit être désigné par une organisation syndicale représentative (≥ 10 % des suffrages au 1er tour CSE, art. L.2143-3) : une organisation non représentative désigne un représentant de section syndicale (RSS).',
      );
    }
    if (!ds && representatif) {
      alerts.push(
        'Un représentant de section syndicale (RSS) est désigné par un syndicat NON représentatif (art. L.2142-1-1) : un syndicat représentatif désigne un délégué syndical. Vérifier la qualification du mandat.',
      );
    }
    if (this.licenciementEnvisage() && !this.autorisationInspecteurTravail()) {
      alerts.push(
        'Un licenciement est envisagé sans autorisation préalable de l\'inspecteur du travail : le DS / RSS est un salarié protégé, à défaut d\'autorisation le licenciement est nul et ouvre droit à réintégration (art. L.2411-3 CT).',
      );
    }
    return alerts;
  });

  constructor(
    private readonly service: DelegationSyndicaleService,
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

  /** Le formulaire est soumettable dès que l'effectif est un entier > 0. */
  formValid(): boolean {
    const eff = this.effectif();
    if (eff === null || !Number.isFinite(eff) || eff <= 0) return false;
    const score = this.pourcentageScorePersonnel();
    if (score !== null && (!Number.isFinite(score) || score < 0 || score > 100)) return false;
    return true;
  }

  onEffectifChange(value: number | null): void {
    this.effectif.set(value === null || value === undefined ? null : Number(value));
    this.provenanceEffectif.set(null);
  }

  onTypeMandatChange(value: MandatSyndicalType): void {
    this.typeMandat.set(value);
    this.provenanceTypeMandat.set(null);
    // Le score personnel ne concerne que le délégué syndical : on le purge pour
    // le RSS afin de ne pas transmettre une valeur sans objet.
    if (value === 'RSS') {
      this.pourcentageScorePersonnel.set(null);
    }
  }

  onSyndicatRepresentatifChange(value: boolean): void {
    this.syndicatRepresentatif.set(value);
  }

  onScoreChange(value: number | null): void {
    this.pourcentageScorePersonnel.set(value === null || value === undefined ? null : Number(value));
  }

  onDateDesignationChange(value: string | null): void {
    this.dateDesignation.set(value ? value : null);
  }

  onLicenciementEnvisageChange(value: boolean): void {
    this.licenciementEnvisage.set(value);
    // L'autorisation de l'inspecteur du travail est sans objet si aucun
    // licenciement n'est envisagé.
    if (!value) {
      this.autorisationInspecteurTravail.set(false);
    }
  }

  onAutorisationChange(value: boolean): void {
    this.autorisationInspecteurTravail.set(value);
  }

  analyze(): void {
    if (!this.formValid()) return;
    const isDs = this.isDs();
    const request: DelegationSyndicaleRequest = {
      effectif: this.effectif()!,
      typeMandat: this.typeMandat(),
      syndicatRepresentatif: this.syndicatRepresentatif(),
      pourcentageScorePersonnel: isDs ? this.pourcentageScorePersonnel() : null,
      dateDesignation: this.dateDesignation(),
      licenciementEnvisage: this.licenciementEnvisage(),
      autorisationInspecteurTravail: this.autorisationInspecteurTravail(),
    };
    this.analyzing.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('Analyse de la désignation syndicale enregistrée', 'OK', { duration: 2500 });
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

  typeMandatLabel(t: MandatSyndicalType | null | undefined): string {
    switch (t) {
      case 'DELEGUE_SYNDICAL': return 'Délégué syndical (DS)';
      case 'RSS':              return 'Représentant de section syndicale (RSS)';
      default:                 return '';
    }
  }

  statutDesignationLabel(s: DelegationSyndicaleStatutDesignation | null | undefined): string {
    switch (s) {
      case 'REGULIERE':   return 'Désignation régulière';
      case 'IRREGULIERE': return 'Désignation irrégulière';
      case 'A_VERIFIER':  return 'Désignation à vérifier';
      default:            return '';
    }
  }

  statutDesignationChipClass(s: DelegationSyndicaleStatutDesignation | null | undefined): string {
    switch (s) {
      case 'REGULIERE':   return 'is-chip is-chip--success';
      case 'IRREGULIERE': return 'is-chip is-chip--danger';
      case 'A_VERIFIER':  return 'is-chip is-chip--warning';
      default:            return 'is-chip';
    }
  }

  bannerClass(s: DelegationSyndicaleStatutDesignation | null | undefined): string {
    switch (s) {
      case 'REGULIERE':   return 'is-banner is-banner--success';
      case 'IRREGULIERE': return 'is-banner is-banner--danger';
      case 'A_VERIFIER':  return 'is-banner is-banner--warning';
      default:            return 'is-banner is-banner--navy';
    }
  }

  bannerIcon(s: DelegationSyndicaleStatutDesignation | null | undefined): string {
    switch (s) {
      case 'REGULIERE':   return 'verified';
      case 'IRREGULIERE': return 'gpp_bad';
      case 'A_VERIFIER':  return 'help_outline';
      default:            return 'info_outline';
    }
  }

  risqueLabel(r: DelegationSyndicaleRisqueNullite | null | undefined): string {
    switch (r) {
      case 'FAIBLE':     return 'Risque faible';
      case 'ELEVE':      return 'Risque élevé';
      case 'SANS_OBJET': return 'Sans objet';
      default:           return '';
    }
  }

  risqueChipClass(r: DelegationSyndicaleRisqueNullite | null | undefined): string {
    switch (r) {
      case 'FAIBLE':     return 'is-chip is-chip--success';
      case 'ELEVE':      return 'is-chip is-chip--danger';
      case 'SANS_OBJET': return 'is-chip is-chip--neutral';
      default:           return 'is-chip';
    }
  }

  /** Note explicative — nullité du licenciement + réintégration. */
  risqueNote(r: DelegationSyndicaleRisqueNullite | null | undefined): string {
    switch (r) {
      case 'FAIBLE':
        return 'Le licenciement envisagé a fait l\'objet de l\'autorisation préalable de l\'inspecteur du travail (art. L.2411-3 CT) : le risque de nullité au titre du statut protecteur est faible.';
      case 'ELEVE':
        return 'Un licenciement est envisagé sans autorisation préalable de l\'inspecteur du travail alors que le DS / RSS est un salarié protégé : le licenciement est nul et le salarié a droit à sa réintégration, outre une indemnisation (art. L.2411-3 CT).';
      case 'SANS_OBJET':
        return 'Aucun licenciement n\'est envisagé : la question de l\'autorisation préalable de l\'inspecteur du travail ne se pose pas à ce stade.';
      default:
        return '';
    }
  }

  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    const input: PrefillCountInput = {
      aiData: this.aiData,
      workspaceCountry: this.workspaceCountry,
    };

    const eff = DelegationSyndicalePrefillRules.computeEffectif(input);
    if (eff !== null && this.effectif() === null) {
      this.effectif.set(eff);
      this.provenanceEffectif.set('IA');
    }

    const type = DelegationSyndicalePrefillRules.computeTypeMandat(input);
    if (type !== null && this.provenanceTypeMandat() === null) {
      this.typeMandat.set(type);
      this.provenanceTypeMandat.set('IA');
      if (type === 'RSS') {
        this.pourcentageScorePersonnel.set(null);
      }
    }
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.effectif.set(r.effectif ?? null);
        this.typeMandat.set(r.typeMandat ?? 'DELEGUE_SYNDICAL');
        this.syndicatRepresentatif.set(r.syndicatRepresentatif ?? false);
        this.pourcentageScorePersonnel.set(r.pourcentageScorePersonnel ?? null);
        this.dateDesignation.set(r.dateDesignation ?? null);
        this.licenciementEnvisage.set(r.licenciementEnvisage ?? false);
        this.autorisationInspecteurTravail.set(r.autorisationInspecteurTravail ?? false);
        this.provenanceEffectif.set(null);
        this.provenanceTypeMandat.set(null);
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
