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
import { MiseAPiedDisciplinaireService } from '../../core/services/mise-a-pied-disciplinaire.service';
import {
  MapAnalyse,
  MapNatureMiseAPied,
  MapPointRegularite,
  MiseAPiedDisciplinaireRequest,
  MiseAPiedDisciplinaireResponse,
} from '../../core/models/mise-a-pied-disciplinaire.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { MiseAPiedDisciplinaireSectionPrefillRules } from './mise-a-pied-disciplinaire-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-212-20 : composant Angular standalone pour l'outil décisionnel "Mise à
 * pied disciplinaire — régularité" — tool_id canonique
 * `F-DT-48-mise-a-pied-disciplinaire`. FRANCE uniquement (L. 1331-1 CT ;
 * L. 1332-1 à L. 1332-4 CT ; Cass. soc. constante).
 *
 * Pattern de référence : `mutation-clause-mobilite-section` (F-DT-71,
 * SF-212-14) — formulaire avec toggles + select + entrées numériques,
 * POST de calcul, verdict 4 niveaux, refresh dashboard, OnPush +
 * markForCheck dans les subscribe.
 *
 * Verdict 4 niveaux :
 *  - REGULIERE (vert — sanction opposable, salaire suspendu licite)
 *  - IRREGULIERE_FORME (or — vice de forme, salaire dû)
 *  - IRREGULIERE_FOND (rouge — prescription / double sanction, salaire dû)
 *  - CONSERVATOIRE (bleu — mesure provisoire, salaire dû)
 */
@Component({
  selector: 'app-mise-a-pied-disciplinaire-section',
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
  templateUrl: './mise-a-pied-disciplinaire-section.component.html',
  styleUrl: './mise-a-pied-disciplinaire-section.component.scss',
})
export class MiseAPiedDisciplinaireSectionComponent implements OnInit, OnChanges {
  // F-JU-03 — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-DT-48-mise-a-pied-disciplinaire';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommée par le panel pour la card.
  static readonly TOOL_LABEL = 'MISE À PIED DISCIPLINAIRE';
  static readonly TOOL_ICON = 'gavel';

  /**
   * Délégué au helper partagé (parité stricte avec `prefillFromAi()`).
   * Retourne le nombre exact de champs pré-remplissables FR pour le badge tab.
   * Couvre les 7 champs `mapDisciplinaire*`.
   */
  static getPrefillCount(input: {
    aiData?: TravailExtractedData | null;
    procedureChecks?: unknown[];
    aiQuestions?: unknown[];
    piecesManquantes?: unknown[];
    triggerEvents?: unknown[];
    workspaceCountry?: string;
  }): number {
    return MiseAPiedDisciplinaireSectionPrefillRules.computePrefillCount({
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
  result = signal<MiseAPiedDisciplinaireResponse | null>(null);

  // --- Form fields (8 champs du contrat backend) ---
  natureMiseAPied = signal<MapNatureMiseAPied>('DISCIPLINAIRE');
  procedureEntretienSuivie = signal<boolean>(true);
  prescriptionFauteVerifiee = signal<boolean>(true);
  dureeDefiniedansRI = signal<boolean>(true);
  dureeJours = signal<number | null>(null);
  salaireSuspendu = signal<boolean>(true);
  sancionsAnterieuresMemesFaits = signal<boolean>(false);
  salaireMensuelBrutEuros = signal<number>(0);

  // Provenance IA par champ pré-rempli — badge `auto_awesome` s'efface au
  // premier `onXxxChange()` manuel sur ce champ.
  provenanceNature = signal<'IA' | null>(null);
  provenanceProcedure = signal<'IA' | null>(null);
  provenancePrescription = signal<'IA' | null>(null);
  provenanceDureeRi = signal<'IA' | null>(null);
  provenanceDureeJours = signal<'IA' | null>(null);
  provenanceSalaireSuspendu = signal<'IA' | null>(null);
  provenanceSanctionsAnt = signal<'IA' | null>(null);

  readonly natureOptions: ReadonlyArray<{ value: MapNatureMiseAPied; label: string }> = [
    { value: 'DISCIPLINAIRE', label: 'Disciplinaire (sanction définitive)' },
    { value: 'CONSERVATOIRE', label: 'Conservatoire (mesure provisoire)' },
    { value: 'INCONNUE', label: 'Inconnue / à requalifier' },
  ];

  constructor(
    private service: MiseAPiedDisciplinaireService,
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
   * Pré-fill IA (SF-212-20) — renseigne les 7 champs depuis le sous-objet
   * `mise_a_pied_detail` (projeté à plat dans `travailExtractedData`).
   * Parité stricte avec `getPrefillCount()` : mêmes mappings, même gate
   * `workspaceCountry === 'FRANCE'`. No-op gracieux si `aiData` absent ou
   * dossier BE.
   */
  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    const ai = this.aiData;
    if (!ai) return;
    const rules = MiseAPiedDisciplinaireSectionPrefillRules;
    const ruleInput = { aiData: ai, workspaceCountry: this.workspaceCountry };

    const nature = rules.computeNature(ruleInput);
    if (nature !== null) {
      this.natureMiseAPied.set(nature);
      this.provenanceNature.set('IA');
    }
    const procedure = rules.computeProcedureSuivie(ruleInput);
    if (procedure !== null) {
      this.procedureEntretienSuivie.set(procedure);
      this.provenanceProcedure.set('IA');
    }
    const prescription = rules.computePrescription(ruleInput);
    if (prescription !== null) {
      this.prescriptionFauteVerifiee.set(prescription);
      this.provenancePrescription.set('IA');
    }
    const dureeRi = rules.computeDureeRi(ruleInput);
    if (dureeRi !== null) {
      this.dureeDefiniedansRI.set(dureeRi);
      this.provenanceDureeRi.set('IA');
    }
    const dureeJours = rules.computeDureeJours(ruleInput);
    if (dureeJours !== null) {
      this.dureeJours.set(dureeJours);
      this.provenanceDureeJours.set('IA');
    }
    const salaireSuspendu = rules.computeSalaireSuspendu(ruleInput);
    if (salaireSuspendu !== null) {
      this.salaireSuspendu.set(salaireSuspendu);
      this.provenanceSalaireSuspendu.set('IA');
    }
    const sanctionsAnt = rules.computeSanctionsAnterieures(ruleInput);
    if (sanctionsAnt !== null) {
      this.sancionsAnterieuresMemesFaits.set(sanctionsAnt);
      this.provenanceSanctionsAnt.set('IA');
    }
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  editMode(): void {
    this.showForm.set(true);
  }

  /** Form valide : FRANCE + nature choisie + durée ≥ 0 si renseignée + salaire ≥ 0. */
  formValid(): boolean {
    if (this.workspaceCountry !== 'FRANCE') return false;
    if (!this.natureMiseAPied()) return false;
    const duree = this.dureeJours();
    if (duree !== null && (duree < 0 || !Number.isFinite(duree))) return false;
    const salaire = this.salaireMensuelBrutEuros();
    if (salaire < 0 || !Number.isFinite(salaire)) return false;
    return true;
  }

  // --- Handlers — toute modification manuelle efface le badge IA du champ ---

  onNatureChange(value: MapNatureMiseAPied): void {
    this.natureMiseAPied.set(value);
    this.provenanceNature.set(null);
  }

  onProcedureChange(value: boolean): void {
    this.procedureEntretienSuivie.set(value);
    this.provenanceProcedure.set(null);
  }

  onPrescriptionChange(value: boolean): void {
    this.prescriptionFauteVerifiee.set(value);
    this.provenancePrescription.set(null);
  }

  onDureeRiChange(value: boolean): void {
    this.dureeDefiniedansRI.set(value);
    this.provenanceDureeRi.set(null);
  }

  onDureeJoursChange(value: number | string | null): void {
    this.provenanceDureeJours.set(null);
    if (value === null || value === '' || value === undefined) {
      this.dureeJours.set(null);
    } else {
      const num = typeof value === 'number' ? value : Number.parseInt(String(value), 10);
      this.dureeJours.set(
          Number.isFinite(num) && num >= 0 ? Math.trunc(num) : null);
    }
  }

  onSalaireSuspenduChange(value: boolean): void {
    this.salaireSuspendu.set(value);
    this.provenanceSalaireSuspendu.set(null);
  }

  onSanctionsAntChange(value: boolean): void {
    this.sancionsAnterieuresMemesFaits.set(value);
    this.provenanceSanctionsAnt.set(null);
  }

  onSalaireMensuelChange(value: number | string | null): void {
    if (value === null || value === '' || value === undefined) {
      this.salaireMensuelBrutEuros.set(0);
    } else {
      const num = typeof value === 'number' ? value : Number.parseFloat(String(value));
      this.salaireMensuelBrutEuros.set(
          Number.isFinite(num) && num >= 0 ? num : 0);
    }
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request: MiseAPiedDisciplinaireRequest = {
      natureMiseAPied: this.natureMiseAPied(),
      procedureEntretienSuivie: this.procedureEntretienSuivie(),
      prescriptionFauteVerifiee: this.prescriptionFauteVerifiee(),
      dureeDefiniedansRI: this.dureeDefiniedansRI(),
      dureeJours: this.dureeJours(),
      salaireSuspendu: this.salaireSuspendu(),
      sancionsAnterieuresMemesFaits: this.sancionsAnterieuresMemesFaits(),
      salaireMensuelBrutEuros: this.salaireMensuelBrutEuros(),
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.applyResult(r);
        this.calculating.set(false);
        this.snackBar.open('Analyse de la mise à pied calculée', 'OK', { duration: 2500 });
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

  private applyResult(r: MiseAPiedDisciplinaireResponse): void {
    this.result.set(r);
    this.hydrateForm(r);
    this.showForm.set(false);
  }

  /**
   * Ré-injecte le snapshot d'inputs de la réponse dans les champs du
   * formulaire — sans quoi un clic « Modifier » repartirait des valeurs par
   * défaut au lieu des dernières valeurs saisies.
   */
  private hydrateForm(r: MiseAPiedDisciplinaireResponse): void {
    this.natureMiseAPied.set(r.natureMiseAPied ?? 'DISCIPLINAIRE');
    this.procedureEntretienSuivie.set(r.procedureEntretienSuivie ?? true);
    this.prescriptionFauteVerifiee.set(r.prescriptionFauteVerifiee ?? true);
    this.dureeDefiniedansRI.set(r.dureeDefiniedansRI ?? true);
    this.dureeJours.set(r.dureeJours ?? null);
    this.salaireSuspendu.set(r.salaireSuspendu ?? true);
    this.sancionsAnterieuresMemesFaits.set(r.sancionsAnterieuresMemesFaits ?? false);
    this.salaireMensuelBrutEuros.set(r.salaireMensuelBrutEuros ?? 0);
    // Valeurs persistées = saisie avocat — jamais de badge IA.
    this.provenanceNature.set(null);
    this.provenanceProcedure.set(null);
    this.provenancePrescription.set(null);
    this.provenanceDureeRi.set(null);
    this.provenanceDureeJours.set(null);
    this.provenanceSalaireSuspendu.set(null);
    this.provenanceSanctionsAnt.set(null);
  }

  // ---------------------------------------------------------------------------
  // Helpers d'affichage du résultat
  // ---------------------------------------------------------------------------

  /**
   * Verdict → bannière de couleur. 4 niveaux :
   *  - REGULIERE → vert (sanction opposable, salaire suspendu licite)
   *  - IRREGULIERE_FORME → or (vice de forme, salaire dû)
   *  - IRREGULIERE_FOND → rouge (prescription / double sanction, salaire dû)
   *  - CONSERVATOIRE → bleu (mesure provisoire, salaire dû)
   */
  verdictBannerClass(v: MapAnalyse): string {
    switch (v) {
      case 'REGULIERE':
        return 'mapd-verdict-banner mapd-verdict-banner--reguliere';
      case 'IRREGULIERE_FORME':
        return 'mapd-verdict-banner mapd-verdict-banner--forme';
      case 'IRREGULIERE_FOND':
        return 'mapd-verdict-banner mapd-verdict-banner--fond';
      case 'CONSERVATOIRE':
        return 'mapd-verdict-banner mapd-verdict-banner--conservatoire';
    }
  }

  verdictBannerLabel(v: MapAnalyse): string {
    switch (v) {
      case 'REGULIERE': return 'Mise à pied disciplinaire régulière';
      case 'IRREGULIERE_FORME': return 'Mise à pied irrégulière (vice de forme)';
      case 'IRREGULIERE_FOND': return 'Mise à pied irrégulière (vice de fond)';
      case 'CONSERVATOIRE': return 'Mise à pied conservatoire';
    }
  }

  verdictBannerIcon(v: MapAnalyse): string {
    switch (v) {
      case 'REGULIERE': return 'check_circle';
      case 'IRREGULIERE_FORME': return 'warning_amber';
      case 'IRREGULIERE_FOND': return 'error_outline';
      case 'CONSERVATOIRE': return 'pause_circle_outline';
    }
  }

  /** Tracker pour les points d'analyse. */
  pointTrackKey(_index: number, p: MapPointRegularite): string {
    return p.code + ':' + p.libelle.slice(0, 30);
  }
}
