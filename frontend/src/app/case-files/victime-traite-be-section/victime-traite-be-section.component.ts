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
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';

import { VictimeTraiteBeService } from '../../core/services/victime-traite-be.service';
import {
  VictimeTraiteBePhase,
  VictimeTraiteBeRequest,
  VictimeTraiteBeResponse,
  VictimeTraiteBeVerdict,
} from '../../core/models/victime-traite-be.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { VictimeTraiteBePrefillRules } from './victime-traite-be-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

/**
 * SF-221-06 — Outil décisionnel « Titre de séjour victime de la traite des êtres humains
 * (BE) » (F-IM-58-victime-traite-be).
 *
 * BELGIQUE uniquement — évalue l'éligibilité au titre victime de la traite (art. 61/2 et
 * s. Loi 15/12/1980, circulaire du 26/09/2008) : coopération judiciaire + rupture avec le
 * réseau + accompagnement par un centre spécialisé agréé (PAG-ASA / Sürya / Payoke), et
 * situe l'étape de la procédure. Visibilité CONTEXTUAL — flag `victime_traite_detecte`.
 *
 * Régime BE PROPRE (3 phases : délai de réflexion → titre temporaire → titre lié à la
 * procédure pénale), DISTINCT du pendant FR F-IM-35 (L. 425-1 CESEDA).
 *
 * <p>Conforme F-IA-04 :
 *  - standalone OnPush, palette : vert ELIGIBLE_*, orange CONDITIONS_NON_REUNIES,
 *    bleu DELAI_REFLEXION / A_ORIENTER_CENTRE
 *  - pré-fill IA RÉEL 3 champs (phaseProcedure, ruptureAvecReseau,
 *    accompagnementCentreSpecialise) ; cooperationJudiciaire + dateDebutAccompagnement
 *    aspirationnels → jamais comptés
 *  - dashboardRefreshService.triggerRefresh() post-POST succès
 *  - static getPrefillCount miroir du runtime prefillFromAi()
 *  - OnPush + ChangeDetectorRef.markForCheck() dans subscribe (next/error)
 */
@Component({
  selector: 'app-victime-traite-be-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    LegalCitationsPipe,
    ToolJurisprudenceCitationsComponent,
  ],
  templateUrl: './victime-traite-be-section.component.html',
  styleUrl: './victime-traite-be-section.component.scss',
})
export class VictimeTraiteBeSectionComponent implements OnInit, OnChanges {
  // F-JU-03 — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-IM-58-victime-traite-be';
  protected readonly brancheActiveForJurisprudence = 'default';

  static readonly TOOL_LABEL = 'VICTIME DE LA TRAITE (BE)';
  static readonly TOOL_ICON = 'support';

  static getPrefillCount(input: PrefillCountInput): number {
    return VictimeTraiteBePrefillRules.computePrefillCount(input);
  }

  readonly phaseOptions: ReadonlyArray<{ value: VictimeTraiteBePhase; label: string }> = [
    { value: 'REFLEXION_45J', label: 'Délai de réflexion (~45 jours)' },
    { value: 'DECLARATION_FAITE', label: 'Déclaration faite' },
    { value: 'PROCEDURE_PENALE_EN_COURS', label: 'Procédure pénale en cours' },
    { value: 'AUCUNE', label: 'Aucune démarche engagée' },
  ];

  @Input() caseFileId!: string;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'BELGIQUE';
  @Input() aiData?: ImmigrationExtractedData | null;
  @Input() forceExpanded = false;
  @Input() standaloneMode = false;

  collapsed = signal(true);
  loading = signal(false);
  analyzing = signal(false);
  showForm = signal(true);
  result = signal<VictimeTraiteBeResponse | null>(null);

  phaseProcedure = signal<VictimeTraiteBePhase | null>(null);
  ruptureAvecReseau = signal<boolean>(false);
  cooperationJudiciaire = signal<boolean>(false);
  accompagnementCentreSpecialise = signal<boolean>(false);
  dateDebutAccompagnement = signal<string | null>(null);

  provenancePhase = signal<'IA' | null>(null);
  provenanceRupture = signal<'IA' | null>(null);
  provenanceAccompagnement = signal<'IA' | null>(null);

  isBelgique = computed<boolean>(() => this.workspaceCountry === 'BELGIQUE');

  constructor(
    private readonly service: VictimeTraiteBeService,
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
    if (this.isBelgique() && this.caseFileId) {
      this.load();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['forceExpanded'] && this.forceExpanded) {
      this.collapsed.set(false);
    }
    if (changes['aiData'] && this.isBelgique() && this.showForm() && !this.result()) {
      this.prefillFromAi();
    }
  }

  toggleCollapse(): void {
    this.collapsed.update((v) => !v);
  }

  editMode(): void {
    this.showForm.set(true);
  }

  formValid(): boolean {
    return this.phaseProcedure() !== null;
  }

  onPhaseChange(value: VictimeTraiteBePhase | null): void {
    this.phaseProcedure.set(value);
    this.provenancePhase.set(null);
  }

  onRuptureChange(value: boolean): void {
    this.ruptureAvecReseau.set(value);
    this.provenanceRupture.set(null);
  }

  onCooperationChange(value: boolean): void {
    this.cooperationJudiciaire.set(value);
  }

  onAccompagnementChange(value: boolean): void {
    this.accompagnementCentreSpecialise.set(value);
    this.provenanceAccompagnement.set(null);
  }

  onDateDebutChange(value: string | null): void {
    this.dateDebutAccompagnement.set(value || null);
  }

  analyze(): void {
    if (!this.formValid()) return;
    const request: VictimeTraiteBeRequest = {
      phaseProcedure: this.phaseProcedure()!,
      ruptureAvecReseau: this.ruptureAvecReseau(),
      cooperationJudiciaire: this.cooperationJudiciaire(),
      accompagnementCentreSpecialise: this.accompagnementCentreSpecialise(),
      dateDebutAccompagnement: this.dateDebutAccompagnement(),
    };
    this.analyzing.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('Analyse victime de la traite enregistrée', 'OK', { duration: 2500 });
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

  verdictBannerClass(verdict: VictimeTraiteBeVerdict | null | undefined): string {
    switch (verdict) {
      case 'ELIGIBLE_TITRE_TEMPORAIRE':      return 'vt-banner vt-banner--success';
      case 'ELIGIBLE_SOUS_PROCEDURE_PENALE': return 'vt-banner vt-banner--success';
      case 'CONDITIONS_NON_REUNIES':         return 'vt-banner vt-banner--warning';
      case 'DELAI_REFLEXION':                return 'vt-banner vt-banner--info';
      case 'A_ORIENTER_CENTRE':              return 'vt-banner vt-banner--info';
      default:                               return 'vt-banner';
    }
  }

  verdictChipClass(verdict: VictimeTraiteBeVerdict | null | undefined): string {
    switch (verdict) {
      case 'ELIGIBLE_TITRE_TEMPORAIRE':      return 'vt-chip vt-chip--success';
      case 'ELIGIBLE_SOUS_PROCEDURE_PENALE': return 'vt-chip vt-chip--success';
      case 'CONDITIONS_NON_REUNIES':         return 'vt-chip vt-chip--warning';
      case 'DELAI_REFLEXION':                return 'vt-chip vt-chip--info';
      case 'A_ORIENTER_CENTRE':              return 'vt-chip vt-chip--info';
      default:                               return 'vt-chip';
    }
  }

  verdictIcon(verdict: VictimeTraiteBeVerdict | null | undefined): string {
    switch (verdict) {
      case 'ELIGIBLE_TITRE_TEMPORAIRE':      return 'verified';
      case 'ELIGIBLE_SOUS_PROCEDURE_PENALE': return 'gavel';
      case 'CONDITIONS_NON_REUNIES':         return 'report_problem';
      case 'DELAI_REFLEXION':                return 'hourglass_top';
      case 'A_ORIENTER_CENTRE':              return 'support';
      default:                               return 'info_outline';
    }
  }

  verdictLabel(verdict: VictimeTraiteBeVerdict | null | undefined): string {
    switch (verdict) {
      case 'ELIGIBLE_TITRE_TEMPORAIRE':      return 'Éligible titre temporaire';
      case 'ELIGIBLE_SOUS_PROCEDURE_PENALE': return 'Éligible sous procédure pénale';
      case 'CONDITIONS_NON_REUNIES':         return 'Conditions non réunies';
      case 'DELAI_REFLEXION':                return 'Délai de réflexion';
      case 'A_ORIENTER_CENTRE':              return 'Orienter vers un centre';
      default:                               return '';
    }
  }

  phaseLabel(phase: VictimeTraiteBePhase | null | undefined): string {
    const opt = this.phaseOptions.find((o) => o.value === phase);
    return opt ? opt.label : '—';
  }

  /** Format JJ/MM/YYYY depuis une date ISO yyyy-MM-dd. */
  formatDateFr(iso: string | null | undefined): string {
    if (!iso || !ISO_DATE_RE.test(iso)) return '—';
    const [y, m, d] = iso.split('-');
    return `${d}/${m}/${y}`;
  }

  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    const input: PrefillCountInput = {
      aiData: this.aiData,
      workspaceCountry: this.workspaceCountry,
    };

    const phase = VictimeTraiteBePrefillRules.computePhase(input);
    if (phase !== null && this.phaseProcedure() === null) {
      this.phaseProcedure.set(phase);
      this.provenancePhase.set('IA');
    }

    const rupture = VictimeTraiteBePrefillRules.computeRupture(input);
    if (rupture !== null) {
      this.ruptureAvecReseau.set(rupture);
      this.provenanceRupture.set('IA');
    }

    const accompagnement = VictimeTraiteBePrefillRules.computeAccompagnement(input);
    if (accompagnement !== null) {
      this.accompagnementCentreSpecialise.set(accompagnement);
      this.provenanceAccompagnement.set('IA');
    }
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.phaseProcedure.set(r.phaseProcedure ?? null);
        this.ruptureAvecReseau.set(r.ruptureAvecReseau ?? false);
        this.cooperationJudiciaire.set(r.cooperationJudiciaire ?? false);
        this.accompagnementCentreSpecialise.set(r.accompagnementCentreSpecialise ?? false);
        this.dateDebutAccompagnement.set(r.dateDebutAccompagnement ?? null);
        this.provenancePhase.set(null);
        this.provenanceRupture.set(null);
        this.provenanceAccompagnement.set(null);
        this.showForm.set(false);
        this.loading.set(false);
        this.cdr.markForCheck();
      },
      error: () => {
        // 404 attendu si aucune analyse — on tente le pré-fill IA.
        this.prefillFromAi();
        this.loading.set(false);
        this.cdr.markForCheck();
      },
    });
  }
}
