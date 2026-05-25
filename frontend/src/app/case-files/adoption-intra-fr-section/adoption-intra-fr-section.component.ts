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
import { AdoptionIntraFrService } from '../../core/services/adoption-intra-fr.service';
import {
  AdoptionIntraFrRequest,
  AdoptionIntraFrResponse,
  FormeAdoption,
} from '../../core/models/adoption-intra-fr.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { AdoptionIntraFrPrefillRules } from './adoption-intra-fr-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-216-16 : composant Angular standalone pour l'outil décisionnel "Adoption
 * de l'enfant du conjoint (adoption intra-familiale)" (F-FA-ADOPTION-INTRA).
 *
 * FRANCE uniquement (art. 343-1 al. 2 Cciv + art. 345-1 Cciv + art. 360-362
 * Cciv + Loi n°2022-219 du 21/2/2022 — réforme adoption).
 *
 * Pattern de référence : `aripa-recouvrement-fr-section` (SF-216-08) et
 * `pension-alimentaire-enfant-fr-section` (SF-216-04).
 *
 * Formulaire (8 champs) :
 *  - formeAdoptionDemandee (PLENIERE | SIMPLE)
 *  - ageAdoptant / ageAdopte (≥ 0)
 *  - mariageOuPacsAdoptantParent (checkbox — pré-requis bloquant art. 345-1 al. 1)
 *  - consentementEnfant (checkbox tri-état facultatif — ≥ 13 ans, art. 360 al. 2)
 *  - consentementAutreParentBiologique (checkbox)
 *  - decheanceAutreParent (checkbox)
 *  - autreParentDecede (checkbox)
 *
 * Verdict affiché :
 *  - formesPossibles (PLENIERE et/ou SIMPLE)
 *  - conditionsRemplies pour la forme demandée
 *  - alerteIrreversibilite (rouge si PLENIERE — art. 356 Cciv)
 *  - consentementEnfantRequis (≥ 13 ans)
 *  - baseLegale, messages, alertes
 *
 * Country gate : `workspaceCountry !== 'FRANCE'` → bannière info + aucun appel.
 *
 * Pré-fill IA V1 : aucun champ pré-rempli (PREFILL_COUNT_ALWAYS_ZERO — voir
 * helper). Le flag `adoption_intra_detection.envisagee` sert uniquement à
 * activer la visibilité du panel F-IA-04.
 *
 * OnPush + ChangeDetectorRef.markForCheck() dans next/error des subscribe
 * (mémoire `feedback_onpush_subscribe_markforcheck`).
 */
@Component({
  selector: 'app-adoption-intra-fr-section',
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
  templateUrl: './adoption-intra-fr-section.component.html',
  styleUrl: './adoption-intra-fr-section.component.scss',
})
export class AdoptionIntraFrSectionComponent implements OnInit, OnChanges {
  // F-JU-03 SF-JU-03-99e — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-FA-ADOPTION-INTRA';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommée par le panel pour la card.
  static readonly TOOL_LABEL = 'ADOPTION INTRA-FAMILIALE';
  static readonly TOOL_ICON = 'family_restroom';

  /**
   * SF-216-16 — délégué au helper partagé (parité stricte avec `prefillFromAi`).
   * V1 — retourne toujours 0 (aucun champ pré-remplissable par l'IA).
   */
  static getPrefillCount(input: {
    aiData?: FamilleExtractedData | null;
    workspaceCountry?: string;
  }): number {
    return AdoptionIntraFrPrefillRules.computePrefillCount({
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
  result = signal<AdoptionIntraFrResponse | null>(null);

  // --- Form fields (8 champs du contrat backend SF-216-15) ---
  formeAdoptionDemandee = signal<FormeAdoption | null>(null);
  ageAdoptant = signal<number | null>(null);
  ageAdopte = signal<number | null>(null);
  mariageOuPacsAdoptantParent = signal<boolean>(false);
  consentementEnfant = signal<boolean | null>(null);
  consentementAutreParentBiologique = signal<boolean>(false);
  decheanceAutreParent = signal<boolean>(false);
  autreParentDecede = signal<boolean>(false);

  /** Options select forme d'adoption. */
  readonly formesAdoption: ReadonlyArray<{ value: FormeAdoption; label: string }> = [
    { value: 'SIMPLE', label: 'Adoption simple (art. 360-362 Cciv)' },
    { value: 'PLENIERE', label: 'Adoption plénière (art. 345-1 Cciv) — IRRÉVERSIBLE' },
  ];

  constructor(
    private service: AdoptionIntraFrService,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef,
    @Optional() private dashboardRefresh: CaseDashboardRefreshService | null,
  ) {}

  ngOnInit(): void {
    if (this.forceExpanded) this.collapsed.set(false);
    // V1 — pas de pré-fill IA (PREFILL_COUNT_ALWAYS_ZERO).
    if (this.workspaceCountry === 'FRANCE') {
      this.load();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['forceExpanded'] && this.forceExpanded) this.collapsed.set(false);
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  editMode(): void {
    this.showForm.set(true);
  }

  /**
   * Form valide : FR + forme demandée + lien mariage/PACS + âges renseignés.
   */
  formValid(): boolean {
    if (this.workspaceCountry !== 'FRANCE') return false;
    if (this.formeAdoptionDemandee() === null) return false;
    const ageA = this.ageAdoptant();
    if (ageA === null || !Number.isFinite(ageA) || ageA < 0) return false;
    const ageE = this.ageAdopte();
    if (ageE === null || !Number.isFinite(ageE) || ageE < 0) return false;
    return true;
  }

  // --- Handlers ---

  onFormeChange(value: FormeAdoption | null): void {
    this.formeAdoptionDemandee.set(value);
  }

  onAgeAdoptantChange(value: number | string | null): void {
    this.ageAdoptant.set(this.parseNonNegativeInt(value));
  }

  onAgeAdopteChange(value: number | string | null): void {
    this.ageAdopte.set(this.parseNonNegativeInt(value));
  }

  onMariageOuPacsChange(checked: boolean): void {
    this.mariageOuPacsAdoptantParent.set(checked);
  }

  onConsentementEnfantChange(checked: boolean): void {
    this.consentementEnfant.set(checked);
  }

  onConsentementAutreParentChange(checked: boolean): void {
    this.consentementAutreParentBiologique.set(checked);
  }

  onDecheanceChange(checked: boolean): void {
    this.decheanceAutreParent.set(checked);
  }

  onAutreParentDecedeChange(checked: boolean): void {
    this.autreParentDecede.set(checked);
  }

  private parseNonNegativeInt(value: number | string | null | undefined): number | null {
    if (value === null || value === undefined || value === '') return null;
    const num = typeof value === 'number' ? value : Number.parseInt(String(value), 10);
    if (!Number.isFinite(num) || num < 0) return null;
    return num;
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request: AdoptionIntraFrRequest = {
      formeAdoptionDemandee: this.formeAdoptionDemandee(),
      ageAdoptant: this.ageAdoptant(),
      ageAdopte: this.ageAdopte(),
      mariageOuPacsAdoptantParent: this.mariageOuPacsAdoptantParent(),
      consentementEnfant: this.consentementEnfant(),
      consentementAutreParentBiologique: this.consentementAutreParentBiologique(),
      decheanceAutreParent: this.decheanceAutreParent(),
      autreParentDecede: this.autreParentDecede(),
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.applyResult(r);
        this.calculating.set(false);
        this.snackBar.open('Recevabilité de l\'adoption évaluée', 'OK', { duration: 2500 });
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

  private applyResult(r: AdoptionIntraFrResponse): void {
    this.result.set(r);
    this.showForm.set(false);
  }

  // ---------------------------------------------------------------------------
  // Helpers d'affichage
  // ---------------------------------------------------------------------------

  /** Libellé humain pour une forme d'adoption. */
  formeLabel(forme: FormeAdoption): string {
    const labels: Record<FormeAdoption, string> = {
      PLENIERE: 'Plénière',
      SIMPLE: 'Simple',
    };
    return labels[forme];
  }

  /** Classe CSS verdict (rouge si plénière). */
  verdictClass(): string {
    const r = this.result();
    if (!r) return '';
    if (!r.conditionsRemplies) return 'verdict-blocked';
    if (r.alerteIrreversibilite) return 'verdict-warning';
    return 'verdict-success';
  }
}
