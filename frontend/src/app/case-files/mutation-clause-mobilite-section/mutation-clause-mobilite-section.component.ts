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
import { MutationClauseMobiliteService } from '../../core/services/mutation-clause-mobilite.service';
import {
  MutationAnalyse,
  MutationConsequence,
  MutationPointAnalyse,
  MutationClauseMobiliteRequest,
  MutationClauseMobiliteResponse,
  MutationReponseSalarie,
} from '../../core/models/mutation-clause-mobilite.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { MutationClauseMobiliteSectionPrefillRules } from './mutation-clause-mobilite-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-212-14 : composant Angular standalone pour l'outil décisionnel
 * "Mutation — validité de la clause de mobilité" — tool_id canonique
 * `F-DT-71-mutation-clause-mobilite`. FRANCE uniquement (Cass. soc.
 * constante sur la clause de mobilité).
 *
 * Pattern de référence : `modification-contrat-refus-section` (F-DT-70,
 * SF-212-12) — formulaire avec toggles + select + entrée numérique,
 * POST de calcul, verdict 3 niveaux, refresh dashboard, OnPush +
 * markForCheck dans les subscribe.
 *
 * Verdict 3 niveaux :
 *  - CLAUSE_VALIDE (vert — refus = faute possible)
 *  - CLAUSE_INVALIDE (rouge — refus sans faute / licenciement sans cause)
 *  - ABSENCE_CLAUSE (or — modification du contrat à formaliser par avenant)
 */
@Component({
  selector: 'app-mutation-clause-mobilite-section',
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
  templateUrl: './mutation-clause-mobilite-section.component.html',
  styleUrl: './mutation-clause-mobilite-section.component.scss',
})
export class MutationClauseMobiliteSectionComponent implements OnInit, OnChanges {
  // F-JU-03 — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-DT-71-mutation-clause-mobilite';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommée par le panel pour la card.
  static readonly TOOL_LABEL = 'MUTATION — CLAUSE DE MOBILITÉ';
  static readonly TOOL_ICON = 'swap_horiz';

  /**
   * Délégué au helper partagé (parité stricte avec `prefillFromAi()`).
   * Retourne le nombre exact de champs pré-remplissables FR pour le badge tab.
   * Couvre les 6 champs `mutation*`.
   */
  static getPrefillCount(input: {
    aiData?: TravailExtractedData | null;
    procedureChecks?: unknown[];
    aiQuestions?: unknown[];
    piecesManquantes?: unknown[];
    triggerEvents?: unknown[];
    workspaceCountry?: string;
  }): number {
    return MutationClauseMobiliteSectionPrefillRules.computePrefillCount({
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
  result = signal<MutationClauseMobiliteResponse | null>(null);

  // --- Form fields (7 champs du contrat backend) ---
  clauseMobilitePresente = signal<boolean>(true);
  zoneGeographiquePrecise = signal<boolean>(true);
  interetLegitimeEmployeur = signal<boolean>(true);
  delaiPrevenanceSemaines = signal<number | null>(null);
  situationFamilialeSalarieContraignante = signal<boolean>(false);
  motifMutationProfessionnel = signal<boolean>(true);
  reponseSalarie = signal<MutationReponseSalarie>('REFUS');

  // Provenance IA par champ pré-rempli — badge `auto_awesome` s'efface au
  // premier `onXxxChange()` manuel sur ce champ.
  provenanceClausePresente = signal<'IA' | null>(null);
  provenanceZonePrecise = signal<'IA' | null>(null);
  provenanceInteretLegitime = signal<'IA' | null>(null);
  provenanceDelai = signal<'IA' | null>(null);
  provenanceSituationFamiliale = signal<'IA' | null>(null);
  provenanceMotifPro = signal<'IA' | null>(null);

  readonly reponseOptions: ReadonlyArray<{ value: MutationReponseSalarie; label: string }> = [
    { value: 'REFUS', label: 'Refus du salarié' },
    { value: 'ACCEPTATION', label: 'Acceptation' },
    { value: 'EN_ATTENTE', label: 'En attente' },
  ];

  constructor(
    private service: MutationClauseMobiliteService,
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
   * Pré-fill IA (SF-212-14) — renseigne les 6 champs depuis le sous-objet
   * `mutation_mobilite_detail` (projeté à plat dans `travailExtractedData`).
   * Parité stricte avec `getPrefillCount()` : mêmes mappings, même gate
   * `workspaceCountry === 'FRANCE'`. No-op gracieux si `aiData` absent ou
   * dossier BE.
   */
  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    const ai = this.aiData;
    if (!ai) return;
    const rules = MutationClauseMobiliteSectionPrefillRules;
    const ruleInput = { aiData: ai, workspaceCountry: this.workspaceCountry };

    const clause = rules.computeClausePresente(ruleInput);
    if (clause !== null) {
      this.clauseMobilitePresente.set(clause);
      this.provenanceClausePresente.set('IA');
    }
    const zone = rules.computeZonePrecise(ruleInput);
    if (zone !== null) {
      this.zoneGeographiquePrecise.set(zone);
      this.provenanceZonePrecise.set('IA');
    }
    const interet = rules.computeInteretLegitime(ruleInput);
    if (interet !== null) {
      this.interetLegitimeEmployeur.set(interet);
      this.provenanceInteretLegitime.set('IA');
    }
    const delai = rules.computeDelaiPrevenance(ruleInput);
    if (delai !== null) {
      this.delaiPrevenanceSemaines.set(delai);
      this.provenanceDelai.set('IA');
    }
    const sit = rules.computeSituationFamiliale(ruleInput);
    if (sit !== null) {
      this.situationFamilialeSalarieContraignante.set(sit);
      this.provenanceSituationFamiliale.set('IA');
    }
    const motifPro = rules.computeMotifProfessionnel(ruleInput);
    if (motifPro !== null) {
      this.motifMutationProfessionnel.set(motifPro);
      this.provenanceMotifPro.set('IA');
    }
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  editMode(): void {
    this.showForm.set(true);
  }

  /** Form valide : FRANCE + réponse choisie + délai ≥ 0 si renseigné. */
  formValid(): boolean {
    if (this.workspaceCountry !== 'FRANCE') return false;
    if (!this.reponseSalarie()) return false;
    const delai = this.delaiPrevenanceSemaines();
    if (delai !== null && (delai < 0 || !Number.isFinite(delai))) return false;
    return true;
  }

  // --- Handlers — toute modification manuelle efface le badge IA du champ ---

  onClausePresenteChange(value: boolean): void {
    this.clauseMobilitePresente.set(value);
    this.provenanceClausePresente.set(null);
  }

  onZonePreciseChange(value: boolean): void {
    this.zoneGeographiquePrecise.set(value);
    this.provenanceZonePrecise.set(null);
  }

  onInteretLegitimeChange(value: boolean): void {
    this.interetLegitimeEmployeur.set(value);
    this.provenanceInteretLegitime.set(null);
  }

  onDelaiChange(value: number | string | null): void {
    this.provenanceDelai.set(null);
    if (value === null || value === '' || value === undefined) {
      this.delaiPrevenanceSemaines.set(null);
    } else {
      const num = typeof value === 'number' ? value : Number.parseInt(String(value), 10);
      this.delaiPrevenanceSemaines.set(
          Number.isFinite(num) && num >= 0 ? Math.trunc(num) : null);
    }
  }

  onSituationFamilialeChange(value: boolean): void {
    this.situationFamilialeSalarieContraignante.set(value);
    this.provenanceSituationFamiliale.set(null);
  }

  onMotifProChange(value: boolean): void {
    this.motifMutationProfessionnel.set(value);
    this.provenanceMotifPro.set(null);
  }

  onReponseChange(value: MutationReponseSalarie): void {
    this.reponseSalarie.set(value);
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request: MutationClauseMobiliteRequest = {
      clauseMobilitePresente: this.clauseMobilitePresente(),
      zoneGeographiquePrecise: this.zoneGeographiquePrecise(),
      interetLegitimeEmployeur: this.interetLegitimeEmployeur(),
      delaiPrevenanceSemaines: this.delaiPrevenanceSemaines(),
      situationFamilialeSalarieContraignante: this.situationFamilialeSalarieContraignante(),
      motifMutationProfessionnel: this.motifMutationProfessionnel(),
      reponseSalarie: this.reponseSalarie(),
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.applyResult(r);
        this.calculating.set(false);
        this.snackBar.open('Analyse de la mutation calculée', 'OK', { duration: 2500 });
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

  private applyResult(r: MutationClauseMobiliteResponse): void {
    this.result.set(r);
    this.hydrateForm(r);
    this.showForm.set(false);
  }

  /**
   * Ré-injecte le snapshot d'inputs de la réponse dans les champs du
   * formulaire — sans quoi un clic « Modifier » repartirait des valeurs par
   * défaut au lieu des dernières valeurs saisies.
   */
  private hydrateForm(r: MutationClauseMobiliteResponse): void {
    this.clauseMobilitePresente.set(r.clauseMobilitePresente ?? true);
    this.zoneGeographiquePrecise.set(r.zoneGeographiquePrecise ?? true);
    this.interetLegitimeEmployeur.set(r.interetLegitimeEmployeur ?? true);
    this.delaiPrevenanceSemaines.set(r.delaiPrevenanceSemaines ?? null);
    this.situationFamilialeSalarieContraignante.set(r.situationFamilialeSalarieContraignante ?? false);
    this.motifMutationProfessionnel.set(r.motifMutationProfessionnel ?? true);
    this.reponseSalarie.set(r.reponseSalarie ?? 'REFUS');
    // Valeurs persistées = saisie avocat — jamais de badge IA.
    this.provenanceClausePresente.set(null);
    this.provenanceZonePrecise.set(null);
    this.provenanceInteretLegitime.set(null);
    this.provenanceDelai.set(null);
    this.provenanceSituationFamiliale.set(null);
    this.provenanceMotifPro.set(null);
  }

  // ---------------------------------------------------------------------------
  // Helpers d'affichage du résultat
  // ---------------------------------------------------------------------------

  /**
   * Verdict → bannière de couleur. 3 niveaux :
   *  - CLAUSE_VALIDE → vert (clause opposable, refus = faute)
   *  - CLAUSE_INVALIDE → rouge (vice, refus sans faute)
   *  - ABSENCE_CLAUSE → or (modification du contrat à formaliser par avenant)
   */
  verdictBannerClass(v: MutationAnalyse): string {
    switch (v) {
      case 'CLAUSE_VALIDE':
        return 'mcm-verdict-banner mcm-verdict-banner--valide';
      case 'CLAUSE_INVALIDE':
        return 'mcm-verdict-banner mcm-verdict-banner--invalide';
      case 'ABSENCE_CLAUSE':
        return 'mcm-verdict-banner mcm-verdict-banner--absence';
    }
  }

  verdictBannerLabel(v: MutationAnalyse): string {
    switch (v) {
      case 'CLAUSE_VALIDE': return 'Clause de mobilité valide';
      case 'CLAUSE_INVALIDE': return 'Clause de mobilité invalide';
      case 'ABSENCE_CLAUSE': return 'Absence de clause de mobilité';
    }
  }

  verdictBannerIcon(v: MutationAnalyse): string {
    switch (v) {
      case 'CLAUSE_VALIDE': return 'check_circle';
      case 'CLAUSE_INVALIDE': return 'error_outline';
      case 'ABSENCE_CLAUSE': return 'warning_amber';
    }
  }

  /** Libellé court d'une conséquence (utilisé pour le tracking dans la liste). */
  consequenceTrackKey(_index: number, c: MutationConsequence): string {
    return `${c.type}:${c.description.slice(0, 50)}`;
  }

  /** Tracker pour les points d'analyse. */
  pointTrackKey(_index: number, p: MutationPointAnalyse): string {
    return p.code;
  }
}
