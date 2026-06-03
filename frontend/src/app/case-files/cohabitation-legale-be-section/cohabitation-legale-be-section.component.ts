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
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CohabitationLegaleBeService } from '../../core/services/cohabitation-legale-be.service';
import {
  CohabitationLegaleBeRequest,
  CohabitationLegaleBeResponse,
  CohabitationLegaleBeVerdict,
  ModeDissolutionCohabitationLegaleBe,
  VueCohabitationLegaleBe,
} from '../../core/models/cohabitation-legale-be.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';
import { CohabitationLegaleBeSectionPrefillRules } from './cohabitation-legale-be-section-prefill-rules';

interface VueOption { value: VueCohabitationLegaleBe; label: string; }
interface ModeDissolutionOption { value: ModeDissolutionCohabitationLegaleBe; label: string; }

/**
 * SF-223-01 : composant Angular standalone pour l'outil décisionnel "Régime de
 * la cohabitation légale en Belgique" (`cohabitation-legale-be`). BELGIQUE
 * uniquement (loi du 23/11/1998 ; CC art. 1475-1479 — à vérifier).
 *
 * Outil multi-vues unique (FORMATION / EFFETS / DISSOLUTION) — 1 outil = 1
 * situation « régime de la cohabitation légale ». ≠ PACS français (F-FA-12) et
 * ≠ cohabitation de fait (P4 F-224).
 *
 * Pré-fill IA V1 = 0 champ (`PREFILL_COUNT_ALWAYS_ZERO = true`). Aucune citation
 * jurisprudentielle BE (F-JU-04 parké). OnPush + ChangeDetectorRef.markForCheck()
 * dans next/error des subscribe (mémoire `feedback_onpush_subscribe_markforcheck`).
 */
@Component({
  selector: 'app-cohabitation-legale-be-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatSlideToggleModule, MatProgressSpinnerModule,
    LegalCitationsPipe,
    ToolJurisprudenceCitationsComponent,
  ],
  templateUrl: './cohabitation-legale-be-section.component.html',
  styleUrl: './cohabitation-legale-be-section.component.scss',
})
export class CohabitationLegaleBeSectionComponent implements OnInit, OnChanges {
  // F-JU-03 — citations jurisprudentielles F-JU-01 (BE parké F-JU-04 : aucune en V1).
  protected readonly toolIdForJurisprudence = 'cohabitation-legale-be';
  protected readonly brancheActiveForJurisprudence = 'default';

  static readonly TOOL_LABEL = 'COHABITATION LÉGALE — RÉGIME (BELGIQUE)';
  static readonly TOOL_ICON = 'diversity_3';

  /**
   * SF-223-01 — V1 : aucun champ pré-rempli par l'IA. Pattern documenté
   * `PREFILL_COUNT_ALWAYS_ZERO = true` (cf. SF-217-17).
   */
  static readonly PREFILL_COUNT_ALWAYS_ZERO = true;
  static getPrefillCount(input: {
    aiData?: unknown;
    procedureChecks?: unknown[];
    aiQuestions?: unknown[];
    piecesManquantes?: unknown[];
    triggerEvents?: unknown[];
    workspaceCountry?: string;
  }): number {
    return CohabitationLegaleBeSectionPrefillRules.computePrefillCount(input);
  }

  static readonly VUE_OPTIONS: readonly VueOption[] = [
    { value: 'FORMATION', label: 'Formation (déclaration à l\'officier de l\'état civil)' },
    { value: 'EFFETS', label: 'Effets (logement familial, charges)' },
    { value: 'DISSOLUTION', label: 'Dissolution (fin de la cohabitation légale)' },
  ];
  readonly vueOptions = CohabitationLegaleBeSectionComponent.VUE_OPTIONS;

  static readonly MODE_DISSOLUTION_OPTIONS: readonly ModeDissolutionOption[] = [
    { value: 'DECLARATION_COMMUNE', label: 'Déclaration commune' },
    { value: 'DECLARATION_UNILATERALE', label: 'Déclaration unilatérale (signifiée par huissier)' },
    { value: 'MARIAGE', label: 'Mariage de l\'un des cohabitants' },
    { value: 'DECES', label: 'Décès de l\'un des cohabitants' },
  ];
  readonly modeDissolutionOptions = CohabitationLegaleBeSectionComponent.MODE_DISSOLUTION_OPTIONS;

  @Input() caseFileId!: string;
  /** F-177 — force l'expansion (mode modal). */
  @Input() forceExpanded = false;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'BELGIQUE';
  @Input() aiData?: FamilleExtractedData | null;
  @Input() standaloneMode = false;

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<CohabitationLegaleBeResponse | null>(null);

  // --- Form fields ---
  vue = signal<VueCohabitationLegaleBe | null>(null);
  deuxPersonnesNonMariees = signal<boolean>(true);
  capaciteJuridique = signal<boolean>(true);
  pasDejaLieParMariageOuAutreCohabitation = signal<boolean>(true);
  domicileCommun = signal<boolean>(true);
  logementFamilialEnJeu = signal<boolean>(false);
  modeDissolutionEnvisage = signal<ModeDissolutionCohabitationLegaleBe | null>(null);
  commentaire = signal<string | null>(null);

  /** Bloc conditions de formation affiché uniquement pour la vue FORMATION. */
  isFormation = computed(() => this.vue() === 'FORMATION');
  /** Bloc effets affiché uniquement pour la vue EFFETS. */
  isEffets = computed(() => this.vue() === 'EFFETS');
  /** Bloc dissolution affiché uniquement pour la vue DISSOLUTION. */
  isDissolution = computed(() => this.vue() === 'DISSOLUTION');

  constructor(
    private service: CohabitationLegaleBeService,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef,
    @Optional() private dashboardRefresh: CaseDashboardRefreshService | null,
  ) {}

  ngOnInit(): void {
    if (this.forceExpanded) this.collapsed.set(false);
    this.prefillFromAi();
    if (this.workspaceCountry === 'BELGIQUE') {
      this.load();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['forceExpanded'] && this.forceExpanded) this.collapsed.set(false);
    if (changes['aiData'] && !changes['aiData'].firstChange
        && this.showForm() && !this.result()) {
      this.prefillFromAi();
    }
  }

  /** V1 : no-op — aucun champ saisissable pré-rempli (PREFILL_COUNT_ALWAYS_ZERO). */
  private prefillFromAi(): void {
    this.cdr.markForCheck();
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  editMode(): void {
    this.showForm.set(true);
  }

  onVueChange(value: VueCohabitationLegaleBe | null): void {
    this.vue.set(value);
    if (value !== 'DISSOLUTION') {
      this.modeDissolutionEnvisage.set(null);
    }
  }

  /**
   * Form valide : workspace BE, vue renseignée et — si DISSOLUTION — mode de
   * dissolution renseigné.
   */
  formValid(): boolean {
    if (this.workspaceCountry !== 'BELGIQUE') return false;
    if (!this.vue()) return false;
    if (this.vue() === 'DISSOLUTION' && !this.modeDissolutionEnvisage()) return false;
    return true;
  }

  calculate(): void {
    if (!this.formValid()) return;
    const dissolution = this.vue() === 'DISSOLUTION';
    const request: CohabitationLegaleBeRequest = {
      vue: this.vue()!,
      deuxPersonnesNonMariees: this.deuxPersonnesNonMariees(),
      capaciteJuridique: this.capaciteJuridique(),
      pasDejaLieParMariageOuAutreCohabitation: this.pasDejaLieParMariageOuAutreCohabitation(),
      domicileCommun: this.domicileCommun(),
      logementFamilialEnJeu: this.logementFamilialEnJeu(),
      modeDissolutionEnvisage: dissolution ? this.modeDissolutionEnvisage() : null,
      commentaire: this.commentaire()?.trim() || null,
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.applyResult(r);
        this.calculating.set(false);
        this.snackBar.open('Analyse de cohabitation légale calculée', 'OK', { duration: 2500 });
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
        // 404 attendu si aucune analyse — on reste en mode formulaire.
        this.loading.set(false);
        this.cdr.markForCheck();
      },
    });
  }

  private applyResult(r: CohabitationLegaleBeResponse): void {
    this.result.set(r);
    this.hydrateForm(r);
    this.showForm.set(false);
  }

  private hydrateForm(r: CohabitationLegaleBeResponse): void {
    this.vue.set(r.vue);
    this.deuxPersonnesNonMariees.set(r.deuxPersonnesNonMariees ?? true);
    this.capaciteJuridique.set(r.capaciteJuridique ?? true);
    this.pasDejaLieParMariageOuAutreCohabitation.set(r.pasDejaLieParMariageOuAutreCohabitation ?? true);
    this.domicileCommun.set(r.domicileCommun ?? true);
    this.logementFamilialEnJeu.set(r.logementFamilialEnJeu ?? false);
    this.modeDissolutionEnvisage.set(r.modeDissolutionEnvisage);
    this.commentaire.set(r.commentaire);
  }

  // ---------------------------------------------------------------------------
  // Helpers d'affichage du résultat
  // ---------------------------------------------------------------------------

  /**
   * Rouge réservé à FORMATION_IMPOSSIBLE. Vert pour FORMATION_VALIDE. Navy info
   * pour les vues EFFETS / DISSOLUTION (qualification, pas de blocage).
   */
  verdictBannerClass(verdict: CohabitationLegaleBeVerdict): string {
    switch (verdict) {
      case 'FORMATION_VALIDE':
        return 'clbe-verdict-banner clbe-verdict-banner--ok';
      case 'FORMATION_IMPOSSIBLE':
        return 'clbe-verdict-banner clbe-verdict-banner--danger';
      case 'EFFETS_QUALIFIES':
      case 'DISSOLUTION_QUALIFIEE':
        return 'clbe-verdict-banner clbe-verdict-banner--info';
    }
  }

  verdictBannerLabel(verdict: CohabitationLegaleBeVerdict): string {
    switch (verdict) {
      case 'FORMATION_VALIDE': return 'Formation valide';
      case 'FORMATION_IMPOSSIBLE': return 'Formation impossible';
      case 'EFFETS_QUALIFIES': return 'Effets qualifiés';
      case 'DISSOLUTION_QUALIFIEE': return 'Dissolution qualifiée';
    }
  }

  verdictBannerIcon(verdict: CohabitationLegaleBeVerdict): string {
    switch (verdict) {
      case 'FORMATION_VALIDE': return 'check_circle';
      case 'FORMATION_IMPOSSIBLE': return 'error';
      case 'EFFETS_QUALIFIES':
      case 'DISSOLUTION_QUALIFIEE':
        return 'info';
    }
  }

  vueLabel(v: VueCohabitationLegaleBe): string {
    return this.vueOptions.find(o => o.value === v)?.label ?? v;
  }
}
