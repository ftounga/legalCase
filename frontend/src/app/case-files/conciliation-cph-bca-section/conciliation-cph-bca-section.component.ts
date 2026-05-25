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
import { MatOptionModule } from '@angular/material/core';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ConciliationCphBcaService } from '../../core/services/conciliation-cph-bca.service';
import {
  ConciliationCphBcaOpportunite,
  ConciliationCphBcaRequest,
  ConciliationCphBcaResponse,
} from '../../core/models/conciliation-cph-bca.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { ConciliationCphBcaSectionPrefillRules } from './conciliation-cph-bca-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-212-38 : composant Angular standalone pour l'outil décisionnel
 * "Conciliation CPH — Bureau de Conciliation et d'Orientation (BCO)" —
 * tool_id canonique `F-DT-84-conciliation-cph-bca`. FRANCE uniquement
 * (R. 1454-7 à R. 1454-12 CT ; L. 1235-1 al. 3 CT — barème transactions
 * BCA ; L. 1411-1 CT — compétence CPH).
 *
 * <p><b>F-212 19/19 — dernier outil livré du bundle F-212.</b></p>
 *
 * <p>Pattern de référence : `elections-cse-conformite-section` (F-DT-65,
 * SF-212-32) — formulaire 4 champs, POST de calcul, encadré BCA (palier
 * + montant minimum) + comparaison BCA vs Macron + checklist BCO, refresh
 * dashboard, OnPush + markForCheck dans les subscribe.</p>
 *
 * <p>Bannière FR-only : si le workspace n'est pas en FRANCE, l'écran
 * affiche uniquement un avertissement pédagogique (la BE dispose d'un
 * régime distinct tribunal du travail + chambre de conciliation, Code
 * judiciaire belge art. 734 et s.).</p>
 */
@Component({
  selector: 'app-conciliation-cph-bca-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatSelectModule, MatOptionModule,
    MatProgressSpinnerModule,
    LegalCitationsPipe,
    ToolJurisprudenceCitationsComponent,
  ],
  templateUrl: './conciliation-cph-bca-section.component.html',
  styleUrl: './conciliation-cph-bca-section.component.scss',
})
export class ConciliationCphBcaSectionComponent implements OnInit, OnChanges {
  // F-JU-03 — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-DT-84-conciliation-cph-bca';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommée par le panel pour la card.
  static readonly TOOL_LABEL = 'CONCILIATION CPH — BCO';
  static readonly TOOL_ICON = 'gavel';

  /**
   * SF-244 — délégué au helper partagé (parité stricte avec `prefillFromAi()`).
   * Retourne le nombre exact de champs pré-remplissables FR pour le badge tab.
   * Couvre les 3 champs `conciliationCph*` IA.
   */
  static getPrefillCount(input: {
    aiData?: TravailExtractedData | null;
    procedureChecks?: unknown[];
    aiQuestions?: unknown[];
    piecesManquantes?: unknown[];
    triggerEvents?: unknown[];
    workspaceCountry?: string;
  }): number {
    return ConciliationCphBcaSectionPrefillRules.computePrefillCount({
      aiData: input.aiData,
      workspaceCountry: input.workspaceCountry,
    });
  }

  @Input() caseFileId!: string;
  @Input() forceExpanded = false;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';
  @Input() aiData?: TravailExtractedData | null;
  /** Mode simulateur autonome (hors dossier) — coupe refresh dashboard. */
  @Input() standaloneMode = false;

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<ConciliationCphBcaResponse | null>(null);

  // --- Form fields (4 champs du contrat backend) ---
  ancienneteMois = signal<number>(0);
  salaireMensuelBrutEuros = signal<number>(0);
  montantDemandesEuros = signal<number>(0);
  opportuniteConciliation = signal<ConciliationCphBcaOpportunite>('INCERTAIN');

  // Provenance IA par champ pré-rempli.
  provenanceAnciennete = signal<'IA' | null>(null);
  provenanceSalaire = signal<'IA' | null>(null);
  provenanceMontantDemandes = signal<'IA' | null>(null);

  readonly opportuniteOptions: { value: ConciliationCphBcaOpportunite; label: string }[] = [
    { value: 'FAVORABLE', label: 'Favorable — orienter vers la conciliation' },
    { value: 'DEFAVORABLE', label: 'Défavorable — orienter vers le Bureau de Jugement' },
    { value: 'INCERTAIN', label: 'Incertaine — à étudier' },
  ];

  constructor(
    private service: ConciliationCphBcaService,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef,
    @Optional() private dashboardRefresh: CaseDashboardRefreshService | null,
  ) {}

  ngOnInit(): void {
    if (this.forceExpanded) this.collapsed.set(false);
    this.prefillFromAi();
    if (this.workspaceCountry === 'FRANCE') {
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
   * Pré-fill IA (SF-212-38) — renseigne les 3 champs chiffrables depuis
   * le sous-objet `conciliation_cph_detail` (projeté à plat dans
   * `travailExtractedData` via Jackson @JsonUnwrapped côté backend).
   * Parité stricte avec `getPrefillCount()` : mêmes mappings, même gate
   * `workspaceCountry === 'FRANCE'`.
   */
  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    const ai = this.aiData;
    if (!ai) return;
    const rules = ConciliationCphBcaSectionPrefillRules;
    const ruleInput = { aiData: ai, workspaceCountry: this.workspaceCountry };

    const anc = rules.computeAncienneteMois(ruleInput);
    if (anc !== null) {
      this.ancienneteMois.set(anc);
      this.provenanceAnciennete.set('IA');
    }
    const sal = rules.computeSalaire(ruleInput);
    if (sal !== null) {
      this.salaireMensuelBrutEuros.set(sal);
      this.provenanceSalaire.set('IA');
    }
    const dem = rules.computeMontantDemandes(ruleInput);
    if (dem !== null) {
      this.montantDemandesEuros.set(dem);
      this.provenanceMontantDemandes.set('IA');
    }
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  editMode(): void {
    this.showForm.set(true);
  }

  /** Form valide : FRANCE + ancienneté ≥ 0 + salaire ≥ 0. */
  formValid(): boolean {
    if (this.workspaceCountry !== 'FRANCE') return false;
    if (this.ancienneteMois() < 0) return false;
    if (this.salaireMensuelBrutEuros() < 0) return false;
    return true;
  }

  // --- Handlers — toute modification manuelle efface le badge IA du champ ---

  onAncienneteChange(value: number): void {
    this.ancienneteMois.set(value);
    this.provenanceAnciennete.set(null);
  }

  onSalaireChange(value: number): void {
    this.salaireMensuelBrutEuros.set(value);
    this.provenanceSalaire.set(null);
  }

  onMontantDemandesChange(value: number): void {
    this.montantDemandesEuros.set(value);
    this.provenanceMontantDemandes.set(null);
  }

  onOpportuniteChange(value: ConciliationCphBcaOpportunite): void {
    this.opportuniteConciliation.set(value);
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request: ConciliationCphBcaRequest = {
      ancienneteMois: this.ancienneteMois(),
      salaireMensuelBrutEuros: this.salaireMensuelBrutEuros(),
      montantDemandesEuros: this.montantDemandesEuros(),
      opportuniteConciliation: this.opportuniteConciliation(),
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.applyResult(r);
        this.calculating.set(false);
        this.snackBar.open('Analyse de conciliation CPH BCA calculée', 'OK', { duration: 2500 });
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
        if (r) {
          this.applyResult(r);
        }
        this.loading.set(false);
        this.cdr.markForCheck();
      },
      error: () => {
        this.loading.set(false);
        this.cdr.markForCheck();
      },
    });
  }

  private applyResult(r: ConciliationCphBcaResponse): void {
    this.result.set(r);
    this.hydrateForm(r);
    this.showForm.set(false);
  }

  /**
   * Ré-injecte le snapshot d'inputs de la réponse dans les champs du
   * formulaire — sans quoi un clic « Modifier » repartirait des valeurs
   * par défaut au lieu des dernières valeurs saisies.
   */
  private hydrateForm(r: ConciliationCphBcaResponse): void {
    this.ancienneteMois.set(r.ancienneteMois ?? 0);
    this.salaireMensuelBrutEuros.set(r.salaireMensuelBrutEuros ?? 0);
    this.montantDemandesEuros.set(r.montantDemandesEuros ?? 0);
    this.opportuniteConciliation.set(r.opportuniteConciliation ?? 'INCERTAIN');
    // Valeurs persistées = saisie avocat — jamais de badge IA.
    this.provenanceAnciennete.set(null);
    this.provenanceSalaire.set(null);
    this.provenanceMontantDemandes.set(null);
  }

  // ---------------------------------------------------------------------------
  // Helpers d'affichage
  // ---------------------------------------------------------------------------

  /** Formatage € avec séparateurs FR (NBSP + virgule décimale). */
  formatEuros(v: number | null | undefined): string {
    if (v === null || v === undefined || !Number.isFinite(v)) return '—';
    return new Intl.NumberFormat('fr-FR', {
      style: 'currency',
      currency: 'EUR',
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }).format(v);
  }

  /** Track function pour la checklist BCO. */
  checklistTrackKey(_index: number, item: string): string {
    return item;
  }
}
