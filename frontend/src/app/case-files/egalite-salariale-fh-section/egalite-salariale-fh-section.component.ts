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
import { EgaliteSalarialeFhService } from '../../core/services/egalite-salariale-fh.service';
import {
  EgsAnalyse,
  EgsFacteurDisparite,
  EgsSexeSalarie,
  EgaliteSalarialeFhRequest,
  EgaliteSalarialeFhResponse,
} from '../../core/models/egalite-salariale-fh.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { EgaliteSalarialeFhSectionPrefillRules } from './egalite-salariale-fh-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-212-24 : composant Angular standalone pour l'outil décisionnel "Égalité
 * salariale femmes/hommes" — tool_id canonique
 * `F-DT-56-egalite-salariale-femmes-hommes`. FRANCE uniquement (L. 1142-7 à
 * L. 1142-10 CT ; L. 1144-1 CT charge de la preuve aménagée ; L. 3221-2 CT
 * à travail égal salaire égal ; L. 1132-1 CT ; L. 1134-5 CT prescription
 * 5 ans ; loi 05/09/2018 « avenir professionnel »).
 *
 * Pattern de référence : `mise-a-pied-disciplinaire-section` (F-DT-48,
 * SF-212-20) — formulaire avec toggles + selects + entrées numériques,
 * POST de calcul, verdict 3 niveaux, refresh dashboard, OnPush +
 * markForCheck dans les subscribe.
 *
 * Verdict 3 niveaux :
 *  - DISCRIMINATION_PROBABLE (rouge — présomption forte L. 1144-1)
 *  - DISCRIMINATION_POSSIBLE (or — éléments à consolider)
 *  - PAS_DE_DISCRIMINATION_APPARENTE (vert — pas d'élément déterminant)
 *
 * Invariant produit : l'alerte non-plafonnement Macron (L. 1235-3 CT non
 * applicable) est TOUJOURS visible quand un résultat est affiché.
 */
@Component({
  selector: 'app-egalite-salariale-fh-section',
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
  templateUrl: './egalite-salariale-fh-section.component.html',
  styleUrl: './egalite-salariale-fh-section.component.scss',
})
export class EgaliteSalarialeFhSectionComponent implements OnInit, OnChanges {
  // F-JU-03 — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-DT-56-egalite-salariale-femmes-hommes';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommée par le panel pour la card.
  static readonly TOOL_LABEL = 'ÉGALITÉ SALARIALE F/H';
  static readonly TOOL_ICON = 'balance';

  /**
   * Délégué au helper partagé (parité stricte avec `prefillFromAi()`).
   * Retourne le nombre exact de champs pré-remplissables FR pour le badge tab.
   * Couvre les 4 champs du sous-objet `egaliteSalarialeDetail`.
   */
  static getPrefillCount(input: {
    aiData?: TravailExtractedData | null;
    procedureChecks?: unknown[];
    aiQuestions?: unknown[];
    piecesManquantes?: unknown[];
    triggerEvents?: unknown[];
    workspaceCountry?: string;
  }): number {
    return EgaliteSalarialeFhSectionPrefillRules.computePrefillCount({
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
  result = signal<EgaliteSalarialeFhResponse | null>(null);

  // --- Form fields (10 champs du contrat backend) ---
  sexeSalarie = signal<EgsSexeSalarie>('FEMME');
  salaireMensuelBrutSalarieEuros = signal<number>(0);
  ancienneteMois = signal<number>(0);
  qualification = signal<string>('');
  nombreComparantsMieuxPayes = signal<number>(0);
  ecartSalaireMoyenComparantsEuros = signal<number>(0);
  ecartPourcentage = signal<number>(0);
  indexEgaliteConnu = signal<boolean>(false);
  scoreIndexEgalite = signal<number | null>(null);
  justificationsEmployeurObjectives = signal<boolean>(false);

  // Provenance IA par champ pré-rempli — badge `auto_awesome` s'efface au
  // premier `onXxxChange()` manuel sur ce champ. 4 champs IA seulement.
  provenanceSexe = signal<'IA' | null>(null);
  provenanceSalaire = signal<'IA' | null>(null);
  provenanceAnciennete = signal<'IA' | null>(null);
  provenanceEcartPct = signal<'IA' | null>(null);

  readonly sexeOptions: ReadonlyArray<{ value: EgsSexeSalarie; label: string }> = [
    { value: 'FEMME', label: 'Femme' },
    { value: 'HOMME', label: 'Homme' },
  ];

  constructor(
    private service: EgaliteSalarialeFhService,
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
   * Pré-fill IA (SF-212-24) — renseigne les 4 champs depuis le sous-objet
   * `egaliteSalarialeDetail` (sub-record imbriqué dans TravailExtractedData).
   * Parité stricte avec `getPrefillCount()` : mêmes mappings, même gate
   * `workspaceCountry === 'FRANCE'`. No-op gracieux si `aiData` absent ou
   * dossier BE.
   */
  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    const ai = this.aiData;
    if (!ai) return;
    const rules = EgaliteSalarialeFhSectionPrefillRules;
    const ruleInput = { aiData: ai, workspaceCountry: this.workspaceCountry };

    const sexe = rules.computeSexeSalarie(ruleInput);
    if (sexe !== null) {
      this.sexeSalarie.set(sexe);
      this.provenanceSexe.set('IA');
    }
    const salaire = rules.computeSalaireBrut(ruleInput);
    if (salaire !== null) {
      this.salaireMensuelBrutSalarieEuros.set(salaire);
      this.provenanceSalaire.set('IA');
    }
    const anciennete = rules.computeAncienneteMois(ruleInput);
    if (anciennete !== null) {
      this.ancienneteMois.set(anciennete);
      this.provenanceAnciennete.set('IA');
    }
    const ecartPct = rules.computeEcartPourcentage(ruleInput);
    if (ecartPct !== null) {
      this.ecartPourcentage.set(ecartPct);
      this.provenanceEcartPct.set('IA');
    }
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  editMode(): void {
    this.showForm.set(true);
  }

  /** Form valide : FRANCE + sexe choisi + valeurs ≥ 0 + écart ≤ 100. */
  formValid(): boolean {
    if (this.workspaceCountry !== 'FRANCE') return false;
    if (!this.sexeSalarie()) return false;
    if (this.salaireMensuelBrutSalarieEuros() < 0) return false;
    if (this.ancienneteMois() < 0) return false;
    if (this.nombreComparantsMieuxPayes() < 0) return false;
    if (this.ecartPourcentage() < 0 || this.ecartPourcentage() > 100) return false;
    const score = this.scoreIndexEgalite();
    if (score !== null && (score < 0 || score > 100)) return false;
    return true;
  }

  // --- Handlers — modifications manuelles effacent le badge IA des champs IA ---

  onSexeChange(value: EgsSexeSalarie): void {
    this.sexeSalarie.set(value);
    this.provenanceSexe.set(null);
  }

  onQualificationChange(value: string): void {
    this.qualification.set(value ?? '');
  }

  onSalaireChange(value: number | string | null): void {
    this.provenanceSalaire.set(null);
    const num = this.parseNumber(value);
    this.salaireMensuelBrutSalarieEuros.set(num !== null && num >= 0 ? num : 0);
  }

  onAncienneteChange(value: number | string | null): void {
    this.provenanceAnciennete.set(null);
    const num = this.parseNumber(value);
    this.ancienneteMois.set(num !== null && num >= 0 ? Math.trunc(num) : 0);
  }

  onNbComparantsChange(value: number | string | null): void {
    const num = this.parseNumber(value);
    this.nombreComparantsMieuxPayes.set(num !== null && num >= 0 ? Math.trunc(num) : 0);
  }

  onEcartEurosChange(value: number | string | null): void {
    const num = this.parseNumber(value);
    this.ecartSalaireMoyenComparantsEuros.set(num !== null && num >= 0 ? num : 0);
  }

  onEcartPctChange(value: number | string | null): void {
    this.provenanceEcartPct.set(null);
    const num = this.parseNumber(value);
    this.ecartPourcentage.set(num !== null && num >= 0 && num <= 100 ? num : 0);
  }

  onIndexConnuChange(value: boolean): void {
    this.indexEgaliteConnu.set(value);
    if (!value) {
      this.scoreIndexEgalite.set(null);
    }
  }

  onScoreIndexChange(value: number | string | null): void {
    const num = this.parseNumber(value);
    if (num === null) {
      this.scoreIndexEgalite.set(null);
    } else {
      const clamped = Math.max(0, Math.min(100, Math.trunc(num)));
      this.scoreIndexEgalite.set(clamped);
    }
  }

  onJustificationsChange(value: boolean): void {
    this.justificationsEmployeurObjectives.set(value);
  }

  private parseNumber(value: number | string | null): number | null {
    if (value === null || value === '' || value === undefined) return null;
    const num = typeof value === 'number' ? value : Number.parseFloat(String(value));
    return Number.isFinite(num) ? num : null;
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request: EgaliteSalarialeFhRequest = {
      sexeSalarie: this.sexeSalarie(),
      salaireMensuelBrutSalarieEuros: this.salaireMensuelBrutSalarieEuros(),
      ancienneteMois: this.ancienneteMois(),
      qualification: this.qualification(),
      nombreComparantsMieuxPayes: this.nombreComparantsMieuxPayes(),
      ecartSalaireMoyenComparantsEuros: this.ecartSalaireMoyenComparantsEuros(),
      ecartPourcentage: this.ecartPourcentage(),
      indexEgaliteConnu: this.indexEgaliteConnu(),
      scoreIndexEgalite: this.scoreIndexEgalite(),
      justificationsEmployeurObjectives: this.justificationsEmployeurObjectives(),
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.applyResult(r);
        this.calculating.set(false);
        this.snackBar.open('Analyse d\'égalité salariale calculée', 'OK', { duration: 2500 });
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

  private applyResult(r: EgaliteSalarialeFhResponse): void {
    this.result.set(r);
    this.hydrateForm(r);
    this.showForm.set(false);
  }

  /**
   * Ré-injecte le snapshot d'inputs de la réponse dans les champs du
   * formulaire — sans quoi un clic « Modifier » repartirait des valeurs par
   * défaut au lieu des dernières valeurs saisies.
   */
  private hydrateForm(r: EgaliteSalarialeFhResponse): void {
    this.sexeSalarie.set(r.sexeSalarie ?? 'FEMME');
    this.salaireMensuelBrutSalarieEuros.set(r.salaireMensuelBrutSalarieEuros ?? 0);
    this.ancienneteMois.set(r.ancienneteMois ?? 0);
    this.qualification.set(r.qualification ?? '');
    this.nombreComparantsMieuxPayes.set(r.nombreComparantsMieuxPayes ?? 0);
    this.ecartSalaireMoyenComparantsEuros.set(r.ecartSalaireMoyenComparantsEuros ?? 0);
    this.ecartPourcentage.set(r.ecartPourcentage ?? 0);
    this.indexEgaliteConnu.set(r.indexEgaliteConnu ?? false);
    this.scoreIndexEgalite.set(r.scoreIndexEgalite ?? null);
    this.justificationsEmployeurObjectives.set(r.justificationsEmployeurObjectives ?? false);
    // Valeurs persistées = saisie avocat — jamais de badge IA.
    this.provenanceSexe.set(null);
    this.provenanceSalaire.set(null);
    this.provenanceAnciennete.set(null);
    this.provenanceEcartPct.set(null);
  }

  // ---------------------------------------------------------------------------
  // Helpers d'affichage du résultat
  // ---------------------------------------------------------------------------

  /**
   * Verdict → bannière de couleur. 3 niveaux :
   *  - DISCRIMINATION_PROBABLE → rouge (présomption forte L. 1144-1)
   *  - DISCRIMINATION_POSSIBLE → or (éléments à consolider)
   *  - PAS_DE_DISCRIMINATION_APPARENTE → vert
   */
  verdictBannerClass(v: EgsAnalyse): string {
    switch (v) {
      case 'DISCRIMINATION_PROBABLE':
        return 'egs-verdict-banner egs-verdict-banner--probable';
      case 'DISCRIMINATION_POSSIBLE':
        return 'egs-verdict-banner egs-verdict-banner--possible';
      case 'PAS_DE_DISCRIMINATION_APPARENTE':
        return 'egs-verdict-banner egs-verdict-banner--apparent';
    }
  }

  verdictBannerLabel(v: EgsAnalyse): string {
    switch (v) {
      case 'DISCRIMINATION_PROBABLE': return 'Discrimination salariale probable';
      case 'DISCRIMINATION_POSSIBLE': return 'Discrimination salariale possible';
      case 'PAS_DE_DISCRIMINATION_APPARENTE': return 'Pas de discrimination apparente';
    }
  }

  verdictBannerIcon(v: EgsAnalyse): string {
    switch (v) {
      case 'DISCRIMINATION_PROBABLE': return 'error_outline';
      case 'DISCRIMINATION_POSSIBLE': return 'warning_amber';
      case 'PAS_DE_DISCRIMINATION_APPARENTE': return 'check_circle';
    }
  }

  /** Tracker pour les facteurs de disparité. */
  factorTrackKey(_index: number, f: EgsFacteurDisparite): string {
    return f.code + ':' + f.libelle.slice(0, 30);
  }
}
