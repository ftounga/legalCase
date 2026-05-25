import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  Input,
  OnChanges,
  OnInit,
  Optional,
  SimpleChanges,
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
import { AdoptionInternationaleFrService } from '../../core/services/adoption-internationale-fr.service';
import {
  AdoptionInternationaleRequest,
  AdoptionInternationaleResponse,
  FormeAdoption,
  VoieProcedureAdoption,
} from '../../core/models/adoption-internationale-fr.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { AdoptionInternationaleFrPrefillRules } from './adoption-internationale-fr-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-216-18 : composant Angular standalone pour l'outil décisionnel
 * "Adoption internationale" (F-FA-ADOPTION-INTERNATIONALE).
 *
 * FRANCE uniquement (art. 370-3 à 370-5 Cciv + Convention La Haye 1993 +
 * Loi n°2001-111 du 6/2/2001 — agrément).
 *
 * Pattern de référence : `adoption-intra-fr-section` (SF-216-16) et
 * `aripa-recouvrement-fr-section` (SF-216-08).
 *
 * Pré-fill IA (3 champs F-246) :
 *  - paysOrigineEnfant ← `paysOrigineAdopteDetecte`
 *  - agrement2025 ← `agrement2025DetecteValide`
 *  - exequaturRequis ← `exequaturRequisDetecte`
 *
 * Country gate : `workspaceCountry !== 'FRANCE'` → bannière info + aucun appel.
 *
 * OnPush + ChangeDetectorRef.markForCheck() dans next/error des subscribe
 * (mémoire `feedback_onpush_subscribe_markforcheck`).
 */
@Component({
  selector: 'app-adoption-internationale-fr-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatSelectModule, MatCheckboxModule,
    MatProgressSpinnerModule,
    LegalCitationsPipe,
    ToolJurisprudenceCitationsComponent,
  ],
  templateUrl: './adoption-internationale-fr-section.component.html',
  styleUrl: './adoption-internationale-fr-section.component.scss',
})
export class AdoptionInternationaleFrSectionComponent implements OnInit, OnChanges {
  // F-JU-03 SF-JU-03-99e — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-FA-ADOPTION-INTERNATIONALE';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommée par le panel pour la card.
  static readonly TOOL_LABEL = 'ADOPTION INTERNATIONALE';
  static readonly TOOL_ICON = 'public';

  /**
   * SF-216-18 — délégué au helper partagé (parité stricte avec prefillFromAi).
   */
  static getPrefillCount(input: {
    aiData?: FamilleExtractedData | null;
    workspaceCountry?: string;
  }): number {
    return AdoptionInternationaleFrPrefillRules.computePrefillCount({
      aiData: input.aiData,
      workspaceCountry: input.workspaceCountry,
    });
  }

  @Input() caseFileId!: string;
  @Input() forceExpanded = false;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';
  @Input() aiData?: FamilleExtractedData | null;
  /** Mode simulateur autonome (hors dossier) — coupe le refresh dashboard. */
  @Input() standaloneMode = false;

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<AdoptionInternationaleResponse | null>(null);

  // --- Form fields (9 champs contrat backend SF-216-17) ---
  paysOrigineEnfant = signal<string | null>(null);
  conventionLaHayeApplicable = signal<boolean | null>(null);
  agrement2025 = signal<boolean | null>(null);
  ageAdoptant = signal<number | null>(null);
  ageAdopte = signal<number | null>(null);
  adoptantMarie = signal<boolean>(false);
  voieProcedure = signal<VoieProcedureAdoption | null>(null);
  formeAdoptionDemandee = signal<FormeAdoption | null>(null);
  exequaturRequis = signal<boolean>(false);

  /** Options select voie procédurale. */
  readonly voiesProcedure: ReadonlyArray<{ value: VoieProcedureAdoption; label: string }> = [
    { value: 'OAA_AGREE', label: 'OAA agréé (Convention La Haye)' },
    { value: 'VOIE_AUTONOME', label: 'Voie autonome (démarche individuelle)' },
    { value: 'ORGANISME_ETAT', label: 'Organisme d\'État (AFA)' },
  ];

  /** Options select forme d'adoption. */
  readonly formesAdoption: ReadonlyArray<{ value: FormeAdoption; label: string }> = [
    { value: 'SIMPLE', label: 'Adoption simple (art. 360-362 Cciv)' },
    { value: 'PLENIERE', label: 'Adoption plénière (art. 345-1 Cciv) — IRRÉVERSIBLE' },
  ];

  constructor(
    private service: AdoptionInternationaleFrService,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef,
    @Optional() private dashboardRefresh: CaseDashboardRefreshService | null,
  ) {}

  ngOnInit(): void {
    if (this.forceExpanded) this.collapsed.set(false);
    if (this.workspaceCountry === 'FRANCE') {
      this.applyPrefillFromAi();
      this.load();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['forceExpanded'] && this.forceExpanded) this.collapsed.set(false);
    if (changes['aiData'] && this.workspaceCountry === 'FRANCE') {
      this.applyPrefillFromAi();
    }
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  editMode(): void {
    this.showForm.set(true);
  }

  /**
   * Form valide : FR + pays renseigné + agrement2025 défini + âges valides.
   */
  formValid(): boolean {
    if (this.workspaceCountry !== 'FRANCE') return false;
    const pays = this.paysOrigineEnfant();
    if (!pays || pays.trim() === '') return false;
    if (this.agrement2025() === null) return false;
    const ageA = this.ageAdoptant();
    if (ageA === null || !Number.isFinite(ageA) || ageA < 0) return false;
    const ageE = this.ageAdopte();
    if (ageE === null || !Number.isFinite(ageE) || ageE < 0) return false;
    return true;
  }

  // --- Handlers ---

  onPaysChange(value: string | null): void {
    this.paysOrigineEnfant.set(value && value.trim() !== '' ? value : null);
  }

  onConventionChange(checked: boolean): void {
    this.conventionLaHayeApplicable.set(checked);
  }

  onAgrementChange(checked: boolean): void {
    this.agrement2025.set(checked);
  }

  onAgeAdoptantChange(value: number | string | null): void {
    this.ageAdoptant.set(this.parseNonNegativeInt(value));
  }

  onAgeAdopteChange(value: number | string | null): void {
    this.ageAdopte.set(this.parseNonNegativeInt(value));
  }

  onAdoptantMarieChange(checked: boolean): void {
    this.adoptantMarie.set(checked);
  }

  onVoieChange(value: VoieProcedureAdoption | null): void {
    this.voieProcedure.set(value);
  }

  onFormeChange(value: FormeAdoption | null): void {
    this.formeAdoptionDemandee.set(value);
  }

  onExequaturChange(checked: boolean): void {
    this.exequaturRequis.set(checked);
  }

  private parseNonNegativeInt(value: number | string | null | undefined): number | null {
    if (value === null || value === undefined || value === '') return null;
    const num = typeof value === 'number' ? value : Number.parseInt(String(value), 10);
    if (!Number.isFinite(num) || num < 0) return null;
    return num;
  }

  /**
   * Détecte si le pays saisi est un pays Maghreb à kafala (Maroc / Algérie /
   * Tunisie). Utilisé pour afficher l'alerte UI préventive. La normalisation
   * (accents / underscores) est laissée au backend — ici on compare sur
   * majuscule trim simple.
   */
  isKafalaPays(): boolean {
    const pays = this.paysOrigineEnfant();
    if (!pays) return false;
    const normalized = pays.trim().toUpperCase();
    return ['MAROC', 'ALGERIE', 'ALGÉRIE', 'TUNISIE'].includes(normalized);
  }

  private applyPrefillFromAi(): void {
    const v = AdoptionInternationaleFrPrefillRules.prefillFromAi({
      aiData: this.aiData ?? null,
      workspaceCountry: this.workspaceCountry,
    });
    if (v.paysOrigineEnfant !== null && this.paysOrigineEnfant() === null) {
      this.paysOrigineEnfant.set(v.paysOrigineEnfant);
    }
    if (v.agrement2025 !== null && this.agrement2025() === null) {
      this.agrement2025.set(v.agrement2025);
    }
    if (v.exequaturRequis !== null) {
      this.exequaturRequis.set(v.exequaturRequis);
    }
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request: AdoptionInternationaleRequest = {
      paysOrigineEnfant: this.paysOrigineEnfant(),
      conventionLaHayeApplicable: this.conventionLaHayeApplicable(),
      agrement2025: this.agrement2025(),
      ageAdoptant: this.ageAdoptant(),
      ageAdopte: this.ageAdopte(),
      adoptantMarie: this.adoptantMarie(),
      voieProcedure: this.voieProcedure(),
      formeAdoptionDemandee: this.formeAdoptionDemandee(),
      exequaturRequis: this.exequaturRequis(),
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.applyResult(r);
        this.calculating.set(false);
        this.snackBar.open('Adoption internationale évaluée', 'OK', { duration: 2500 });
        if (!this.standaloneMode) this.dashboardRefresh?.triggerRefresh();
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.calculating.set(false);
        const msg = err?.error?.message || err?.error || 'Erreur lors du calcul';
        this.snackBar.open(String(msg), 'Fermer', { duration: 5000, panelClass: 'snack-error' });
        this.cdr.markForCheck();
      },
    });
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.applyResult(r);
        this.loading.set(false);
        this.cdr.markForCheck();
      },
      error: () => {
        this.loading.set(false);
        this.cdr.markForCheck();
      },
    });
  }

  private applyResult(r: AdoptionInternationaleResponse): void {
    this.result.set(r);
    this.showForm.set(false);
  }

  // ---------------------------------------------------------------------------
  // Helpers d'affichage
  // ---------------------------------------------------------------------------

  voieLabel(v: VoieProcedureAdoption): string {
    const labels: Record<VoieProcedureAdoption, string> = {
      OAA_AGREE: 'OAA agréé (Convention La Haye)',
      VOIE_AUTONOME: 'Voie autonome',
      ORGANISME_ETAT: 'Organisme d\'État',
    };
    return labels[v];
  }

  /** Classe CSS verdict. */
  verdictClass(): string {
    const r = this.result();
    if (!r) return '';
    if (r.alerteKafala || !r.conditionsRemplies) return 'verdict-blocked';
    if (r.verdict === 'VOIE_AUTONOME_RISQUE') return 'verdict-warning';
    return 'verdict-success';
  }
}
