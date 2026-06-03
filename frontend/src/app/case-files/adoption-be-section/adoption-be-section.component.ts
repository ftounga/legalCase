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
import { AdoptionBeService } from '../../core/services/adoption-be.service';
import {
  AdoptionBeRequest,
  AdoptionBeResponse,
  AdoptionBeVerdict,
  ConsentementParentsBiologiques,
  TypeAdoptionBe,
} from '../../core/models/adoption-be.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';
import { AdoptionBeSectionPrefillRules } from './adoption-be-section-prefill-rules';

interface TypeOption { value: TypeAdoptionBe; label: string; }
interface ConsentementParentsOption { value: ConsentementParentsBiologiques; label: string; }

/**
 * SF-223-02 : composant Angular standalone pour l'outil décisionnel
 * "Recevabilité de l'adoption en Belgique" (`adoption-be`). BELGIQUE uniquement
 * (loi du 24/04/2003 ; CC art. 343-1 et s. — à vérifier).
 *
 * Outil multi-vues unique (PLENIERE / SIMPLE / CO_PARENTALE) — 1 outil = 1
 * situation « recevabilité de l'adoption ». ≠ adoption française. L'adoption
 * internationale (Convention de La Haye) est différée (P4 F-224).
 *
 * Pré-fill IA F-246 : type d'adoption + agrément mentionné pré-remplis si
 * factualisables (sous-objet `adoption_be_detection`). Aucune citation
 * jurisprudentielle BE (F-JU-04 parké). OnPush + ChangeDetectorRef.markForCheck()
 * dans next/error des subscribe (mémoire `feedback_onpush_subscribe_markforcheck`).
 */
@Component({
  selector: 'app-adoption-be-section',
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
  templateUrl: './adoption-be-section.component.html',
  styleUrl: './adoption-be-section.component.scss',
})
export class AdoptionBeSectionComponent implements OnInit, OnChanges {
  // F-JU-03 — citations jurisprudentielles F-JU-01 (BE parké F-JU-04 : aucune en V1).
  protected readonly toolIdForJurisprudence = 'adoption-be';
  protected readonly brancheActiveForJurisprudence = 'default';

  static readonly TOOL_LABEL = 'ADOPTION — RECEVABILITÉ (BELGIQUE)';
  static readonly TOOL_ICON = 'family_restroom';

  /**
   * SF-223-02 — pré-fill F-246 : type d'adoption + agrément mentionné si
   * factualisables. Délègue au helper partagé (parité runtime/static —
   * garde-fou `prefill-count-integrity`).
   */
  static getPrefillCount(input: {
    aiData?: unknown;
    procedureChecks?: unknown[];
    aiQuestions?: unknown[];
    piecesManquantes?: unknown[];
    triggerEvents?: unknown[];
    workspaceCountry?: string;
  }): number {
    return AdoptionBeSectionPrefillRules.computePrefillCount(input);
  }

  static readonly TYPE_OPTIONS: readonly TypeOption[] = [
    { value: 'PLENIERE', label: 'Adoption plénière (rupture du lien d\'origine)' },
    { value: 'SIMPLE', label: 'Adoption simple (maintien de certains liens d\'origine)' },
    { value: 'CO_PARENTALE', label: 'Adoption co-parentale (enfant du conjoint / cohabitant légal)' },
  ];
  readonly typeOptions = AdoptionBeSectionComponent.TYPE_OPTIONS;

  static readonly CONSENTEMENT_PARENTS_OPTIONS: readonly ConsentementParentsOption[] = [
    { value: 'OBTENU', label: 'Obtenu' },
    { value: 'REFUSE', label: 'Refusé' },
    { value: 'SANS_OBJET', label: 'Sans objet (filiation d\'origine non établie)' },
  ];
  readonly consentementParentsOptions = AdoptionBeSectionComponent.CONSENTEMENT_PARENTS_OPTIONS;

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
  result = signal<AdoptionBeResponse | null>(null);

  // --- Form fields ---
  typeAdoption = signal<TypeAdoptionBe | null>(null);
  ageAdoptant = signal<number | null>(null);
  ageAdopte = signal<number | null>(null);
  ecartAgeAnnees = signal<number | null>(null);
  adoptantEnCoupleAvecParentBiologique = signal<boolean>(false);
  lienCohabitationLegaleOuMariage = signal<boolean>(false);
  agrementObtenu = signal<boolean>(false);
  consentementAdopteOuRepresentants = signal<boolean>(false);
  consentementParentsBiologiques = signal<ConsentementParentsBiologiques | null>(null);
  commentaire = signal<string | null>(null);

  /** Bloc co-parentale (lien de couple / mariage) affiché uniquement pour CO_PARENTALE. */
  isCoParentale = computed(() => this.typeAdoption() === 'CO_PARENTALE');

  constructor(
    private service: AdoptionBeService,
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

  /**
   * F-246 — pré-remplit le type d'adoption et l'agrément depuis le sous-objet
   * IA `adoption_be_detection` (clés camelCase plates, @JsonUnwrapped). Aucun
   * autre champ n'est factualisable de manière stable en V1.
   */
  private prefillFromAi(): void {
    if (this.workspaceCountry !== 'BELGIQUE') {
      this.cdr.markForCheck();
      return;
    }
    const ai = this.aiData as (FamilleExtractedData & {
      adoptionBeTypeDetecte?: string | null;
      adoptionBeAgrementMentionneBeDetecte?: boolean | null;
    }) | null | undefined;
    if (!ai) {
      this.cdr.markForCheck();
      return;
    }
    const type = ai.adoptionBeTypeDetecte;
    if (type === 'PLENIERE' || type === 'SIMPLE' || type === 'CO_PARENTALE') {
      this.typeAdoption.set(type);
    }
    if (typeof ai.adoptionBeAgrementMentionneBeDetecte === 'boolean') {
      this.agrementObtenu.set(ai.adoptionBeAgrementMentionneBeDetecte);
    }
    this.cdr.markForCheck();
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  editMode(): void {
    this.showForm.set(true);
  }

  onTypeChange(value: TypeAdoptionBe | null): void {
    this.typeAdoption.set(value);
    if (value !== 'CO_PARENTALE') {
      this.adoptantEnCoupleAvecParentBiologique.set(false);
      this.lienCohabitationLegaleOuMariage.set(false);
    }
  }

  /**
   * Form valide : workspace BE, type d'adoption renseigné, âges renseignés
   * (> 0) et état du consentement des parents biologiques renseigné.
   */
  formValid(): boolean {
    if (this.workspaceCountry !== 'BELGIQUE') return false;
    if (!this.typeAdoption()) return false;
    if (!this.consentementParentsBiologiques()) return false;
    const adoptant = this.ageAdoptant();
    const adopte = this.ageAdopte();
    if (adoptant === null || adoptant <= 0) return false;
    if (adopte === null || adopte <= 0) return false;
    return true;
  }

  calculate(): void {
    if (!this.formValid()) return;
    const coParentale = this.typeAdoption() === 'CO_PARENTALE';
    const request: AdoptionBeRequest = {
      typeAdoption: this.typeAdoption()!,
      ageAdoptant: this.ageAdoptant(),
      ageAdopte: this.ageAdopte(),
      ecartAgeAnnees: this.ecartAgeAnnees(),
      adoptantEnCoupleAvecParentBiologique: coParentale
        ? this.adoptantEnCoupleAvecParentBiologique() : null,
      lienCohabitationLegaleOuMariage: coParentale
        ? this.lienCohabitationLegaleOuMariage() : null,
      agrementObtenu: this.agrementObtenu(),
      consentementAdopteOuRepresentants: this.consentementAdopteOuRepresentants(),
      consentementParentsBiologiques: this.consentementParentsBiologiques()!,
      commentaire: this.commentaire()?.trim() || null,
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.applyResult(r);
        this.calculating.set(false);
        this.snackBar.open('Analyse de recevabilité de l\'adoption calculée', 'OK', { duration: 2500 });
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

  private applyResult(r: AdoptionBeResponse): void {
    this.result.set(r);
    this.hydrateForm(r);
    this.showForm.set(false);
  }

  private hydrateForm(r: AdoptionBeResponse): void {
    this.typeAdoption.set(r.typeAdoption);
    this.ageAdoptant.set(r.ageAdoptant ?? null);
    this.ageAdopte.set(r.ageAdopte ?? null);
    this.ecartAgeAnnees.set(r.ecartAgeAnnees ?? null);
    this.adoptantEnCoupleAvecParentBiologique.set(r.adoptantEnCoupleAvecParentBiologique ?? false);
    this.lienCohabitationLegaleOuMariage.set(r.lienCohabitationLegaleOuMariage ?? false);
    this.agrementObtenu.set(r.agrementObtenu ?? false);
    this.consentementAdopteOuRepresentants.set(r.consentementAdopteOuRepresentants ?? false);
    this.consentementParentsBiologiques.set(r.consentementParentsBiologiques);
    this.commentaire.set(r.commentaire);
  }

  // ---------------------------------------------------------------------------
  // Helpers d'affichage du résultat
  // ---------------------------------------------------------------------------

  /**
   * Rouge réservé à IRRECEVABLE. Vert pour RECEVABLE. Or pour
   * RECEVABLE_SOUS_CONDITIONS. Navy info pour QUALIFICATION_INCOMPLETE.
   */
  verdictBannerClass(verdict: AdoptionBeVerdict): string {
    switch (verdict) {
      case 'RECEVABLE':
        return 'adobe-verdict-banner adobe-verdict-banner--ok';
      case 'RECEVABLE_SOUS_CONDITIONS':
        return 'adobe-verdict-banner adobe-verdict-banner--warn';
      case 'IRRECEVABLE':
        return 'adobe-verdict-banner adobe-verdict-banner--danger';
      case 'QUALIFICATION_INCOMPLETE':
        return 'adobe-verdict-banner adobe-verdict-banner--info';
    }
  }

  verdictBannerLabel(verdict: AdoptionBeVerdict): string {
    switch (verdict) {
      case 'RECEVABLE': return 'Recevable';
      case 'RECEVABLE_SOUS_CONDITIONS': return 'Recevable sous conditions';
      case 'IRRECEVABLE': return 'Irrecevable';
      case 'QUALIFICATION_INCOMPLETE': return 'Qualification incomplète';
    }
  }

  verdictBannerIcon(verdict: AdoptionBeVerdict): string {
    switch (verdict) {
      case 'RECEVABLE': return 'check_circle';
      case 'RECEVABLE_SOUS_CONDITIONS': return 'rule';
      case 'IRRECEVABLE': return 'error';
      case 'QUALIFICATION_INCOMPLETE': return 'info';
    }
  }

  typeLabel(t: TypeAdoptionBe): string {
    return this.typeOptions.find(o => o.value === t)?.label ?? t;
  }

  /** Titre de la liste motifs/conditions selon le verdict. */
  motifsTitle(verdict: AdoptionBeVerdict): string {
    return verdict === 'IRRECEVABLE'
      ? 'Motifs d\'irrecevabilité'
      : 'Conditions à satisfaire';
  }
}
