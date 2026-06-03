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
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { GpaBeSituationContentieuseService } from '../../core/services/gpa-be-situation-contentieuse.service';
import {
  GpaBeRequest,
  GpaBeResponse,
  GpaBeVerdict,
  LieuGpa,
  LienGenetique,
} from '../../core/models/gpa-be-situation-contentieuse.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';
import { GpaBeSituationContentieuseSectionPrefillRules } from './gpa-be-situation-contentieuse-section-prefill-rules';

interface LieuOption { value: LieuGpa; label: string; }
interface LienOption { value: LienGenetique; label: string; }

/**
 * SF-223-04 : composant Angular standalone pour l'outil décisionnel "Situation
 * contentieuse post-GPA — Belgique" (`gpa-be-situation-contentieuse`). BELGIQUE
 * uniquement (vide juridique GPA — à vérifier par avocat belge).
 *
 * 1 outil = 1 situation « établissement de la filiation après gestation pour
 * autrui en Belgique ». Outil de cadrage contentieux → DISTINCT de
 * `adoption-be` (l'adoption peut être UNE voie du verdict).
 *
 * Pré-fill IA F-246 : lieu de la GPA + lien génétique pré-remplis si
 * factualisables (sous-objet `gpa_be_detection`). Aucune citation
 * jurisprudentielle BE (F-JU-04 parké — silence > erreur en vide juridique).
 * OnPush + ChangeDetectorRef.markForCheck() dans next/error des subscribe
 * (mémoire `feedback_onpush_subscribe_markforcheck`).
 */
@Component({
  selector: 'app-gpa-be-situation-contentieuse-section',
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
  templateUrl: './gpa-be-situation-contentieuse-section.component.html',
  styleUrl: './gpa-be-situation-contentieuse-section.component.scss',
})
export class GpaBeSituationContentieuseSectionComponent implements OnInit, OnChanges {
  // F-JU-03 — citations jurisprudentielles F-JU-01 (BE parké F-JU-04 : aucune en V1).
  protected readonly toolIdForJurisprudence = 'gpa-be-situation-contentieuse';
  protected readonly brancheActiveForJurisprudence = 'default';

  static readonly TOOL_LABEL = 'GPA — FILIATION (BELGIQUE)';
  static readonly TOOL_ICON = 'family_restroom';

  /**
   * SF-223-04 — pré-fill F-246 : lieu de la GPA + lien génétique si
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
    return GpaBeSituationContentieuseSectionPrefillRules.computePrefillCount(input);
  }

  static readonly LIEU_OPTIONS: readonly LieuOption[] = [
    { value: 'BELGIQUE', label: 'GPA réalisée en Belgique' },
    { value: 'ETRANGER', label: 'GPA réalisée à l\'étranger' },
  ];
  readonly lieuOptions = GpaBeSituationContentieuseSectionComponent.LIEU_OPTIONS;

  static readonly LIEN_OPTIONS: readonly LienOption[] = [
    { value: 'PERE_INTENTIONNEL', label: 'Seul le père intentionnel a un lien génétique' },
    { value: 'MERE_INTENTIONNELLE', label: 'Seule la mère intentionnelle a un lien génétique' },
    { value: 'LES_DEUX', label: 'Les deux parents intentionnels ont un lien génétique' },
    { value: 'AUCUN', label: 'Aucun parent intentionnel n\'a de lien génétique (don de gamètes)' },
  ];
  readonly lienOptions = GpaBeSituationContentieuseSectionComponent.LIEN_OPTIONS;

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
  result = signal<GpaBeResponse | null>(null);

  // --- Form fields ---
  gpaRealiseeEnBelgiqueOuEtranger = signal<LieuGpa | null>(null);
  lienGenetiqueParentIntentionnel = signal<LienGenetique | null>(null);
  acteNaissanceEtrangerEtabli = signal<boolean>(false);
  merePorteuseDesignee = signal<boolean>(false);
  consentementMerePorteuse = signal<boolean>(false);
  coupleIntentionnelMarieOuCohabitant = signal<boolean>(false);
  commentaire = signal<string | null>(null);

  constructor(
    private service: GpaBeSituationContentieuseService,
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
   * F-246 — pré-remplit le lieu de la GPA et le lien génétique depuis le
   * sous-objet IA `gpa_be_detection` (clés camelCase plates, @JsonUnwrapped).
   * Aucun autre champ n'est factualisable de manière stable en V1.
   */
  private prefillFromAi(): void {
    if (this.workspaceCountry !== 'BELGIQUE') {
      this.cdr.markForCheck();
      return;
    }
    const ai = this.aiData as (FamilleExtractedData & {
      gpaBeLieuDetecte?: string | null;
      gpaBeLienGenetiqueDetecte?: string | null;
    }) | null | undefined;
    if (!ai) {
      this.cdr.markForCheck();
      return;
    }
    const lieu = ai.gpaBeLieuDetecte;
    if (lieu === 'BELGIQUE' || lieu === 'ETRANGER') {
      this.gpaRealiseeEnBelgiqueOuEtranger.set(lieu);
    }
    const lien = ai.gpaBeLienGenetiqueDetecte;
    if (lien === 'PERE_INTENTIONNEL' || lien === 'MERE_INTENTIONNELLE'
        || lien === 'AUCUN' || lien === 'LES_DEUX') {
      this.lienGenetiqueParentIntentionnel.set(lien);
    }
    this.cdr.markForCheck();
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  editMode(): void {
    this.showForm.set(true);
  }

  /** Form valide : workspace BE + lieu + lien génétique renseignés. */
  formValid(): boolean {
    if (this.workspaceCountry !== 'BELGIQUE') return false;
    if (!this.gpaRealiseeEnBelgiqueOuEtranger()) return false;
    if (!this.lienGenetiqueParentIntentionnel()) return false;
    return true;
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request: GpaBeRequest = {
      gpaRealiseeEnBelgiqueOuEtranger: this.gpaRealiseeEnBelgiqueOuEtranger()!,
      lienGenetiqueParentIntentionnel: this.lienGenetiqueParentIntentionnel()!,
      acteNaissanceEtrangerEtabli: this.acteNaissanceEtrangerEtabli(),
      merePorteuseDesignee: this.merePorteuseDesignee(),
      consentementMerePorteuse: this.consentementMerePorteuse(),
      coupleIntentionnelMarieOuCohabitant: this.coupleIntentionnelMarieOuCohabitant(),
      commentaire: this.commentaire()?.trim() || null,
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.applyResult(r);
        this.calculating.set(false);
        this.snackBar.open('Analyse de la situation post-GPA calculée', 'OK', { duration: 2500 });
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

  private applyResult(r: GpaBeResponse): void {
    this.result.set(r);
    this.hydrateForm(r);
    this.showForm.set(false);
  }

  private hydrateForm(r: GpaBeResponse): void {
    this.gpaRealiseeEnBelgiqueOuEtranger.set(r.gpaRealiseeEnBelgiqueOuEtranger);
    this.lienGenetiqueParentIntentionnel.set(r.lienGenetiqueParentIntentionnel);
    this.acteNaissanceEtrangerEtabli.set(r.acteNaissanceEtrangerEtabli ?? false);
    this.merePorteuseDesignee.set(r.merePorteuseDesignee ?? false);
    this.consentementMerePorteuse.set(r.consentementMerePorteuse ?? false);
    this.coupleIntentionnelMarieOuCohabitant.set(r.coupleIntentionnelMarieOuCohabitant ?? false);
    this.commentaire.set(r.commentaire);
  }

  // ---------------------------------------------------------------------------
  // Helpers d'affichage du résultat
  // ---------------------------------------------------------------------------

  /**
   * Vert pour FILIATION_PAR_RECONNAISSANCE (voie la plus directe). Or pour
   * FILIATION_PAR_ADOPTION_POST_NAISSANCE et
   * RECONNAISSANCE_ACTE_ETRANGER_A_INSTRUIRE (voies à instruire). Navy info
   * pour QUALIFICATION_INCOMPLETE. Pas de rouge : aucune voie n'est bloquée
   * (vide juridique, orientation seulement).
   */
  verdictBannerClass(verdict: GpaBeVerdict): string {
    switch (verdict) {
      case 'FILIATION_PAR_RECONNAISSANCE':
        return 'gpa-verdict-banner gpa-verdict-banner--ok';
      case 'FILIATION_PAR_ADOPTION_POST_NAISSANCE':
      case 'RECONNAISSANCE_ACTE_ETRANGER_A_INSTRUIRE':
        return 'gpa-verdict-banner gpa-verdict-banner--warn';
      case 'QUALIFICATION_INCOMPLETE':
        return 'gpa-verdict-banner gpa-verdict-banner--info';
    }
  }

  verdictBannerLabel(verdict: GpaBeVerdict): string {
    switch (verdict) {
      case 'FILIATION_PAR_RECONNAISSANCE': return 'Filiation par reconnaissance';
      case 'FILIATION_PAR_ADOPTION_POST_NAISSANCE': return 'Filiation par adoption après naissance';
      case 'RECONNAISSANCE_ACTE_ETRANGER_A_INSTRUIRE': return 'Reconnaissance de l\'acte étranger à instruire';
      case 'QUALIFICATION_INCOMPLETE': return 'Qualification incomplète';
    }
  }

  verdictBannerIcon(verdict: GpaBeVerdict): string {
    switch (verdict) {
      case 'FILIATION_PAR_RECONNAISSANCE': return 'check_circle';
      case 'FILIATION_PAR_ADOPTION_POST_NAISSANCE': return 'family_restroom';
      case 'RECONNAISSANCE_ACTE_ETRANGER_A_INSTRUIRE': return 'public';
      case 'QUALIFICATION_INCOMPLETE': return 'info';
    }
  }

  lieuLabel(o: LieuGpa): string {
    return this.lieuOptions.find(opt => opt.value === o)?.label ?? o;
  }

  lienLabel(o: LienGenetique): string {
    return this.lienOptions.find(opt => opt.value === o)?.label ?? o;
  }
}
