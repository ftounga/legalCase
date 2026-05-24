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
import { ModificationContratRefusService } from '../../core/services/modification-contrat-refus.service';
import {
  ModificationContratAnalyse,
  ModificationContratConsequence,
  ModificationContratElementModifie,
  ModificationContratRefusRequest,
  ModificationContratRefusResponse,
  ModificationContratReponseSalarie,
} from '../../core/models/modification-contrat-refus.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { ModificationContratRefusSectionPrefillRules } from './modification-contrat-refus-section-prefill-rules';

/**
 * SF-212-12 : composant Angular standalone pour l'outil décisionnel
 * "Modification du contrat — refus du salarié" — tool_id canonique
 * `F-DT-70-modification-contrat-refus`. FRANCE uniquement (Cass. soc. ;
 * L. 1222-6 CT).
 *
 * Pattern de référence : `faute-inexcusable-fr-section` (F-DT-91, SF-212-10)
 * — formulaire avec select + toggles, POST de calcul, verdict 3 niveaux,
 * refresh dashboard, OnPush + markForCheck dans les subscribe.
 *
 * Verdict 3 niveaux :
 *  - MODIFICATION_CONTRAT (or — élément essentiel ; accord requis)
 *  - CHANGEMENT_CONDITIONS_TRAVAIL (vert — pouvoir de direction)
 *  - INCERTAIN (gris — qualification à apprécier au cas d'espèce)
 *
 * Bannière alerte L. 1222-6 : visible si `alerteDelaiReflexion = true` après
 * calcul (procédure de modification pour motif économique non respectée).
 */
@Component({
  selector: 'app-modification-contrat-refus-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatSelectModule, MatSlideToggleModule,
    MatProgressSpinnerModule,
    LegalCitationsPipe,
  ],
  templateUrl: './modification-contrat-refus-section.component.html',
  styleUrl: './modification-contrat-refus-section.component.scss',
})
export class ModificationContratRefusSectionComponent implements OnInit, OnChanges {
  // F-177 SF-177-03b : metadata statique consommée par le panel pour la card.
  static readonly TOOL_LABEL = 'MODIFICATION DU CONTRAT — REFUS DU SALARIÉ';
  static readonly TOOL_ICON = 'edit_note';

  /**
   * Délégué au helper partagé (parité stricte avec `prefillFromAi()`).
   * Retourne le nombre exact de champs pré-remplissables FR pour le badge tab.
   * Couvre les 4 champs `modifContrat*`.
   */
  static getPrefillCount(input: {
    aiData?: TravailExtractedData | null;
    procedureChecks?: unknown[];
    aiQuestions?: unknown[];
    piecesManquantes?: unknown[];
    triggerEvents?: unknown[];
    workspaceCountry?: string;
  }): number {
    return ModificationContratRefusSectionPrefillRules.computePrefillCount({
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
  result = signal<ModificationContratRefusResponse | null>(null);

  // --- Form fields (6 champs du contrat backend) ---
  elementModifie = signal<ModificationContratElementModifie>('REMUNERATION');
  elementExplicitementContractualise = signal<boolean>(true);
  motifEconomique = signal<boolean>(false);
  notificationEcriteL1222_6 = signal<boolean | null>(null);
  delaiReflexionRespecteMois = signal<number | null>(null);
  reponseSalarie = signal<ModificationContratReponseSalarie>('REFUS');

  // Provenance IA par champ pré-rempli — badge `auto_awesome` s'efface au
  // premier `onXxxChange()` manuel sur ce champ.
  provenanceElementModifie = signal<'IA' | null>(null);
  provenanceContractualise = signal<'IA' | null>(null);
  provenanceMotifEco = signal<'IA' | null>(null);
  provenanceNotifEcrite = signal<'IA' | null>(null);

  // Options du select élément modifié.
  readonly elementOptions: ReadonlyArray<{ value: ModificationContratElementModifie; label: string }> = [
    { value: 'REMUNERATION', label: 'Rémunération' },
    { value: 'QUALIFICATION', label: 'Qualification professionnelle' },
    { value: 'DUREE_TRAVAIL', label: 'Durée du travail' },
    { value: 'LIEU_TRAVAIL', label: 'Lieu de travail' },
    { value: 'HORAIRES', label: 'Horaires' },
    { value: 'TACHES', label: 'Tâches' },
    { value: 'AUTRE', label: 'Autre' },
  ];

  readonly reponseOptions: ReadonlyArray<{ value: ModificationContratReponseSalarie; label: string }> = [
    { value: 'REFUS', label: 'Refus' },
    { value: 'ACCEPTATION', label: 'Acceptation' },
    { value: 'EN_ATTENTE', label: 'En attente' },
  ];

  constructor(
    private service: ModificationContratRefusService,
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
   * Pré-fill IA (SF-212-12) — renseigne les 4 champs depuis le sous-objet
   * `modification_contrat_detail` (projeté à plat dans `travailExtractedData`).
   * Parité stricte avec `getPrefillCount()` : mêmes mappings, même gate
   * `workspaceCountry === 'FRANCE'`. No-op gracieux si `aiData` absent ou
   * dossier BE.
   */
  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    const ai = this.aiData;
    if (!ai) return;
    const rules = ModificationContratRefusSectionPrefillRules;
    const ruleInput = { aiData: ai, workspaceCountry: this.workspaceCountry };

    const elem = rules.computeElementModifie(ruleInput);
    if (elem !== null) {
      this.elementModifie.set(elem);
      this.provenanceElementModifie.set('IA');
    }
    const contractualise = rules.computeContractualise(ruleInput);
    if (contractualise !== null) {
      this.elementExplicitementContractualise.set(contractualise);
      this.provenanceContractualise.set('IA');
    }
    const motifEco = rules.computeMotifEco(ruleInput);
    if (motifEco !== null) {
      this.motifEconomique.set(motifEco);
      this.provenanceMotifEco.set('IA');
    }
    const notif = rules.computeNotifEcrite(ruleInput);
    if (notif !== null) {
      this.notificationEcriteL1222_6.set(notif);
      this.provenanceNotifEcrite.set('IA');
    }
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  editMode(): void {
    this.showForm.set(true);
  }

  /** Form valide : FRANCE + élément choisi + réponse choisie. */
  formValid(): boolean {
    if (this.workspaceCountry !== 'FRANCE') return false;
    if (!this.elementModifie()) return false;
    if (!this.reponseSalarie()) return false;
    const delai = this.delaiReflexionRespecteMois();
    if (delai !== null && (delai < 0 || !Number.isFinite(delai))) return false;
    return true;
  }

  // --- Handlers — toute modification manuelle efface le badge IA du champ ---

  onElementChange(value: ModificationContratElementModifie): void {
    this.elementModifie.set(value);
    this.provenanceElementModifie.set(null);
  }

  onContractualiseChange(value: boolean): void {
    this.elementExplicitementContractualise.set(value);
    this.provenanceContractualise.set(null);
  }

  onMotifEcoChange(value: boolean): void {
    this.motifEconomique.set(value);
    this.provenanceMotifEco.set(null);
  }

  onNotifEcriteChange(value: boolean): void {
    this.notificationEcriteL1222_6.set(value);
    this.provenanceNotifEcrite.set(null);
  }

  onDelaiChange(value: number | string | null): void {
    if (value === null || value === '' || value === undefined) {
      this.delaiReflexionRespecteMois.set(null);
    } else {
      const num = typeof value === 'number' ? value : Number.parseInt(String(value), 10);
      this.delaiReflexionRespecteMois.set(
          Number.isFinite(num) && num >= 0 ? Math.trunc(num) : null);
    }
  }

  onReponseChange(value: ModificationContratReponseSalarie): void {
    this.reponseSalarie.set(value);
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request: ModificationContratRefusRequest = {
      elementModifie: this.elementModifie(),
      elementExplicitementContractualise: this.elementExplicitementContractualise(),
      motifEconomique: this.motifEconomique(),
      notificationEcriteL1222_6: this.notificationEcriteL1222_6(),
      delaiReflexionRespecteMois: this.delaiReflexionRespecteMois(),
      reponseSalarie: this.reponseSalarie(),
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.applyResult(r);
        this.calculating.set(false);
        this.snackBar.open('Analyse de la modification calculée', 'OK', { duration: 2500 });
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

  private applyResult(r: ModificationContratRefusResponse): void {
    this.result.set(r);
    this.hydrateForm(r);
    this.showForm.set(false);
  }

  /**
   * Ré-injecte le snapshot d'inputs de la réponse dans les champs du
   * formulaire — sans quoi un clic « Modifier » repartirait des valeurs par
   * défaut au lieu des dernières valeurs saisies.
   */
  private hydrateForm(r: ModificationContratRefusResponse): void {
    this.elementModifie.set(r.elementModifie ?? 'REMUNERATION');
    this.elementExplicitementContractualise.set(r.elementExplicitementContractualise ?? true);
    this.motifEconomique.set(r.motifEconomique ?? false);
    this.notificationEcriteL1222_6.set(r.notificationEcriteL1222_6 ?? null);
    this.delaiReflexionRespecteMois.set(r.delaiReflexionRespecteMois ?? null);
    this.reponseSalarie.set(r.reponseSalarie ?? 'REFUS');
    // Valeurs persistées = saisie avocat — jamais de badge IA.
    this.provenanceElementModifie.set(null);
    this.provenanceContractualise.set(null);
    this.provenanceMotifEco.set(null);
    this.provenanceNotifEcrite.set(null);
  }

  // ---------------------------------------------------------------------------
  // Helpers d'affichage du résultat
  // ---------------------------------------------------------------------------

  /**
   * Verdict → bannière de couleur. 3 niveaux :
   *  - MODIFICATION_CONTRAT → or (alerte — accord requis)
   *  - CHANGEMENT_CONDITIONS_TRAVAIL → vert (pouvoir de direction)
   *  - INCERTAIN → gris
   */
  verdictBannerClass(v: ModificationContratAnalyse): string {
    switch (v) {
      case 'MODIFICATION_CONTRAT':
        return 'mcr-verdict-banner mcr-verdict-banner--modification';
      case 'CHANGEMENT_CONDITIONS_TRAVAIL':
        return 'mcr-verdict-banner mcr-verdict-banner--changement';
      case 'INCERTAIN':
        return 'mcr-verdict-banner mcr-verdict-banner--incertain';
    }
  }

  verdictBannerLabel(v: ModificationContratAnalyse): string {
    switch (v) {
      case 'MODIFICATION_CONTRAT': return 'Modification du contrat (accord requis)';
      case 'CHANGEMENT_CONDITIONS_TRAVAIL': return 'Changement des conditions de travail';
      case 'INCERTAIN': return 'Qualification incertaine';
    }
  }

  verdictBannerIcon(v: ModificationContratAnalyse): string {
    switch (v) {
      case 'MODIFICATION_CONTRAT': return 'warning_amber';
      case 'CHANGEMENT_CONDITIONS_TRAVAIL': return 'check_circle';
      case 'INCERTAIN': return 'help_outline';
    }
  }

  /** Libellé court d'une conséquence (utilisé pour le tracking dans la liste). */
  consequenceTrackKey(_index: number, c: ModificationContratConsequence): string {
    return `${c.type}:${c.description.slice(0, 50)}`;
  }
}
