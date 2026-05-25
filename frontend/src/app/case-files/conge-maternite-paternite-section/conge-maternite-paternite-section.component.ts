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
import { CongeMaternitePaterniteService } from '../../core/services/conge-maternite-paternite.service';
import {
  CongeMaternitePaterniteRequest,
  CongeMaternitePaterniteResponse,
  CongeMaternitePaterniteType,
} from '../../core/models/conge-maternite-paternite.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { CongeMaternitePaterniteSectionPrefillRules } from './conge-maternite-paternite-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-212-30 : composant Angular standalone pour l'outil décisionnel "Congé
 * maternité / paternité — protection et indemnités" — tool_id canonique
 * `F-DT-77-conge-paternite-maternite`. FRANCE uniquement (L. 1225-1 à
 * L. 1225-40 CT ; L. 331-3 CSS ; loi du 16/03/2021 portant le congé
 * paternité à 25 jours calendaires).
 *
 * <p>Pattern de référence : `pdv-rcc-conformite-section` (F-DT-46,
 * SF-212-36) — formulaire avec select + toggles + inputs nombres / date,
 * POST de calcul, tableau de synthèse, alertes visuelles, refresh
 * dashboard, OnPush + markForCheck dans les subscribe.</p>
 *
 * <p>Sortie : tableau de synthèse — durée (jours), date de fin théorique,
 * IJ journalière + totale estimées, date jusqu'à laquelle la protection
 * contre le licenciement s'applique, deux alertes visuelles distinctes
 * (licenciement notifié pendant la période protégée → nullité L. 1225-4 ;
 * retour sur poste différent → manquement L. 1225-25 CT).</p>
 */
@Component({
  selector: 'app-conge-maternite-paternite-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatSelectModule, MatSlideToggleModule,
    MatProgressSpinnerModule,
    LegalCitationsPipe,
    ToolJurisprudenceCitationsComponent,
  ],
  templateUrl: './conge-maternite-paternite-section.component.html',
  styleUrl: './conge-maternite-paternite-section.component.scss',
})
export class CongeMaternitePaterniteSectionComponent implements OnInit, OnChanges {
  // F-JU-03 — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-DT-77-conge-paternite-maternite';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommée par le panel pour la card.
  static readonly TOOL_LABEL = 'CONGÉ MATERNITÉ / PATERNITÉ — PROTECTION & INDEMNITÉS';
  static readonly TOOL_ICON = 'child_friendly';

  /**
   * Délégué au helper partagé (parité stricte avec `prefillFromAi()`).
   * Retourne le nombre exact de champs pré-remplissables FR pour le badge tab.
   * Couvre les 5 champs du sous-objet `congeMaternitePaterniteDetail`.
   */
  static getPrefillCount(input: {
    aiData?: TravailExtractedData | null;
    procedureChecks?: unknown[];
    aiQuestions?: unknown[];
    piecesManquantes?: unknown[];
    triggerEvents?: unknown[];
    workspaceCountry?: string;
  }): number {
    return CongeMaternitePaterniteSectionPrefillRules.computePrefillCount({
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
  result = signal<CongeMaternitePaterniteResponse | null>(null);

  // --- Form fields (7 champs du contrat backend) ---
  typeConge = signal<CongeMaternitePaterniteType>('MATERNITE');
  rangEnfant = signal<number>(1);
  naissanceMultiple = signal<boolean>(false);
  salaireMensuelBrutEuros = signal<number>(3000);
  dateDebutConge = signal<string>('');
  licenciementPendantConge = signal<boolean>(false);
  retourPosteDifferent = signal<boolean>(false);

  // Provenance IA par champ pré-rempli — badge `auto_awesome` s'efface au
  // premier `onXxxChange()` manuel sur ce champ. 5 champs IA.
  provenanceType = signal<'IA' | null>(null);
  provenanceRang = signal<'IA' | null>(null);
  provenanceMultiple = signal<'IA' | null>(null);
  provenanceDate = signal<'IA' | null>(null);
  provenanceSalaire = signal<'IA' | null>(null);

  readonly typeOptions: ReadonlyArray<{ value: CongeMaternitePaterniteType; label: string }> = [
    { value: 'MATERNITE', label: 'Maternité — L. 1225-17 CT (16 / 26 / 34 / 46 semaines)' },
    { value: 'PATERNITE', label: 'Paternité — L. 1225-35 CT (25 / 32 jours — loi 16/03/2021)' },
  ];

  constructor(
    private service: CongeMaternitePaterniteService,
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
   * Pré-fill IA (SF-212-30) — renseigne les 5 champs depuis le sous-objet
   * `congeMaternitePaterniteDetail`. Parité stricte avec `getPrefillCount()` :
   * mêmes mappings, même gate `workspaceCountry === 'FRANCE'`.
   */
  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    const ai = this.aiData;
    if (!ai) return;
    const rules = CongeMaternitePaterniteSectionPrefillRules;
    const ruleInput = { aiData: ai, workspaceCountry: this.workspaceCountry };

    const type = rules.computeTypeConge(ruleInput);
    if (type !== null) {
      this.typeConge.set(type);
      this.provenanceType.set('IA');
    }
    const rang = rules.computeRangEnfant(ruleInput);
    if (rang !== null) {
      this.rangEnfant.set(rang);
      this.provenanceRang.set('IA');
    }
    const multiple = rules.computeNaissanceMultiple(ruleInput);
    if (multiple !== null) {
      this.naissanceMultiple.set(multiple);
      this.provenanceMultiple.set('IA');
    }
    const date = rules.computeDateDebutConge(ruleInput);
    if (date !== null) {
      this.dateDebutConge.set(date);
      this.provenanceDate.set('IA');
    }
    const salaire = rules.computeSalaireMensuelBrut(ruleInput);
    if (salaire !== null) {
      this.salaireMensuelBrutEuros.set(salaire);
      this.provenanceSalaire.set('IA');
    }
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  editMode(): void {
    this.showForm.set(true);
  }

  /** Form valide : FRANCE + type + rang ≥ 1 + salaire ≥ 0 + date. */
  formValid(): boolean {
    if (this.workspaceCountry !== 'FRANCE') return false;
    if (!this.typeConge()) return false;
    if (this.rangEnfant() < 1) return false;
    if (this.salaireMensuelBrutEuros() < 0) return false;
    if (!this.dateDebutConge()) return false;
    return true;
  }

  // --- Handlers — modifications manuelles effacent le badge IA des champs IA ---

  onTypeChange(value: CongeMaternitePaterniteType): void {
    this.typeConge.set(value);
    this.provenanceType.set(null);
  }

  onRangChange(value: number): void {
    this.rangEnfant.set(value);
    this.provenanceRang.set(null);
  }

  onMultipleChange(value: boolean): void {
    this.naissanceMultiple.set(value);
    this.provenanceMultiple.set(null);
  }

  onDateChange(value: string): void {
    this.dateDebutConge.set(value);
    this.provenanceDate.set(null);
  }

  onSalaireChange(value: number): void {
    this.salaireMensuelBrutEuros.set(value);
    this.provenanceSalaire.set(null);
  }

  onLicenciementChange(value: boolean): void {
    this.licenciementPendantConge.set(value);
  }

  onRetourPosteChange(value: boolean): void {
    this.retourPosteDifferent.set(value);
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request: CongeMaternitePaterniteRequest = {
      typeConge: this.typeConge(),
      rangEnfant: this.rangEnfant(),
      naissanceMultiple: this.naissanceMultiple(),
      salaireMensuelBrutEuros: this.salaireMensuelBrutEuros(),
      dateDebutConge: this.dateDebutConge(),
      licenciementPendantConge: this.licenciementPendantConge(),
      retourPosteDifferent: this.retourPosteDifferent(),
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.applyResult(r);
        this.calculating.set(false);
        this.snackBar.open('Analyse congé maternité / paternité calculée', 'OK', { duration: 2500 });
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

  private applyResult(r: CongeMaternitePaterniteResponse): void {
    this.result.set(r);
    this.hydrateForm(r);
    this.showForm.set(false);
  }

  /**
   * Ré-injecte le snapshot d'inputs de la réponse dans les champs du
   * formulaire — sans quoi un clic « Modifier » repartirait des valeurs par
   * défaut au lieu des dernières valeurs saisies.
   */
  private hydrateForm(r: CongeMaternitePaterniteResponse): void {
    this.typeConge.set(r.typeConge ?? 'MATERNITE');
    this.rangEnfant.set(r.rangEnfant ?? 1);
    this.naissanceMultiple.set(r.naissanceMultiple ?? false);
    this.salaireMensuelBrutEuros.set(r.salaireMensuelBrutEuros ?? 0);
    this.dateDebutConge.set(r.dateDebutConge ?? '');
    this.licenciementPendantConge.set(r.licenciementPendantConge ?? false);
    this.retourPosteDifferent.set(r.retourPosteDifferent ?? false);
    // Valeurs persistées = saisie avocat — jamais de badge IA.
    this.provenanceType.set(null);
    this.provenanceRang.set(null);
    this.provenanceMultiple.set(null);
    this.provenanceDate.set(null);
    this.provenanceSalaire.set(null);
  }
}
