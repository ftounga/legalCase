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
import { PdvRccConformiteService } from '../../core/services/pdv-rcc-conformite.service';
import {
  PdvRccAnalyse,
  PdvRccPointIrregularite,
  PdvRccTypeDispositif,
  PdvRccConformiteRequest,
  PdvRccConformiteResponse,
} from '../../core/models/pdv-rcc-conformite.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { PdvRccConformiteSectionPrefillRules } from './pdv-rcc-conformite-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-212-36 : composant Angular standalone pour l'outil décisionnel "PDV /
 * RCC — conformité" — tool_id canonique `F-DT-46-pdv-rcc-conformite`.
 * FRANCE uniquement (L. 1237-17 à L. 1237-19-14 CT — RCC instituée par
 * l'ord. n°2017-1387 du 22/09/2017 ; décret 2017-1718 du 20/12/2017 ;
 * circulaire DGT du 19/09/2018).
 *
 * Pattern de référence : `egalite-salariale-fh-section` (F-DT-56,
 * SF-212-24) — formulaire avec select + toggles, POST de calcul, verdict
 * 3 niveaux, refresh dashboard, OnPush + markForCheck dans les subscribe.
 *
 * Verdict 3 niveaux :
 *  - CONFORME (vert — dispositif validé, RCC ouvre droit à l'ARE)
 *  - PARTIELLEMENT_CONFORME (or — irrégularités régularisables)
 *  - NON_CONFORME (rouge — vice fondamental)
 *
 * Invariant produit : badge "Droit ARE confirmé" affiché si et seulement si
 * la RCC est conforme (différence majeure RCC vs PDV hors PSE).
 */
@Component({
  selector: 'app-pdv-rcc-conformite-section',
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
  templateUrl: './pdv-rcc-conformite-section.component.html',
  styleUrl: './pdv-rcc-conformite-section.component.scss',
})
export class PdvRccConformiteSectionComponent implements OnInit, OnChanges {
  // F-JU-03 — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-DT-46-pdv-rcc-conformite';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommée par le panel pour la card.
  static readonly TOOL_LABEL = 'PDV / RCC — CONFORMITÉ';
  static readonly TOOL_ICON = 'groups';

  /**
   * Délégué au helper partagé (parité stricte avec `prefillFromAi()`).
   * Retourne le nombre exact de champs pré-remplissables FR pour le badge tab.
   * Couvre les 4 champs du sous-objet `pdvRccDetail` (0 actuellement —
   * sous-objet non projeté côté backend, cf. dette F-DT-46).
   */
  static getPrefillCount(input: {
    aiData?: TravailExtractedData | null;
    procedureChecks?: unknown[];
    aiQuestions?: unknown[];
    piecesManquantes?: unknown[];
    triggerEvents?: unknown[];
    workspaceCountry?: string;
  }): number {
    return PdvRccConformiteSectionPrefillRules.computePrefillCount({
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
  result = signal<PdvRccConformiteResponse | null>(null);

  // --- Form fields (7 champs du contrat backend) ---
  typeDispositif = signal<PdvRccTypeDispositif>('RCC');
  accordCollectifMajoritaire = signal<boolean>(false);
  validationDREETSObtenue = signal<boolean>(false);
  delaiValidation15JRespectee = signal<boolean | null>(null);
  indemnitesAuMoinsLegales = signal<boolean>(false);
  adhesionVolontaireIndividuelle = signal<boolean>(false);
  informationIndividuelleComplete = signal<boolean>(false);

  // Provenance IA par champ pré-rempli — badge `auto_awesome` s'efface au
  // premier `onXxxChange()` manuel sur ce champ. 4 champs IA.
  provenanceType = signal<'IA' | null>(null);
  provenanceAccord = signal<'IA' | null>(null);
  provenanceDreets = signal<'IA' | null>(null);
  provenanceIndemnites = signal<'IA' | null>(null);

  readonly typeOptions: ReadonlyArray<{ value: PdvRccTypeDispositif; label: string }> = [
    { value: 'RCC', label: 'RCC — rupture conventionnelle collective' },
    { value: 'PDV', label: 'PDV — plan de départs volontaires' },
  ];

  constructor(
    private service: PdvRccConformiteService,
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
   * Pré-fill IA (SF-212-36) — renseigne les 4 champs depuis le sous-objet
   * `pdvRccDetail`. Parité stricte avec `getPrefillCount()` : mêmes
   * mappings, même gate `workspaceCountry === 'FRANCE'`. No-op gracieux
   * actuel car `pdvRccDetail` non projeté backend (dette F-DT-46).
   */
  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    const ai = this.aiData;
    if (!ai) return;
    const rules = PdvRccConformiteSectionPrefillRules;
    const ruleInput = { aiData: ai, workspaceCountry: this.workspaceCountry };

    const type = rules.computeTypeDispositif(ruleInput);
    if (type !== null) {
      this.typeDispositif.set(type);
      this.provenanceType.set('IA');
    }
    const accord = rules.computeAccordMajoritaire(ruleInput);
    if (accord !== null) {
      this.accordCollectifMajoritaire.set(accord);
      this.provenanceAccord.set('IA');
    }
    const dreets = rules.computeValidationDreets(ruleInput);
    if (dreets !== null) {
      this.validationDREETSObtenue.set(dreets);
      this.provenanceDreets.set('IA');
    }
    const indemnites = rules.computeIndemnitesLegales(ruleInput);
    if (indemnites !== null) {
      this.indemnitesAuMoinsLegales.set(indemnites);
      this.provenanceIndemnites.set('IA');
    }
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  editMode(): void {
    this.showForm.set(true);
  }

  /** Form valide : FRANCE + type dispositif choisi. */
  formValid(): boolean {
    if (this.workspaceCountry !== 'FRANCE') return false;
    if (!this.typeDispositif()) return false;
    return true;
  }

  // --- Handlers — modifications manuelles effacent le badge IA des champs IA ---

  onTypeChange(value: PdvRccTypeDispositif): void {
    this.typeDispositif.set(value);
    this.provenanceType.set(null);
  }

  onAccordChange(value: boolean): void {
    this.accordCollectifMajoritaire.set(value);
    this.provenanceAccord.set(null);
  }

  onDreetsChange(value: boolean): void {
    this.validationDREETSObtenue.set(value);
    this.provenanceDreets.set(null);
    if (!value) {
      this.delaiValidation15JRespectee.set(null);
    }
  }

  onDelai15JChange(value: boolean | null): void {
    this.delaiValidation15JRespectee.set(value);
  }

  onIndemnitesChange(value: boolean): void {
    this.indemnitesAuMoinsLegales.set(value);
    this.provenanceIndemnites.set(null);
  }

  onAdhesionChange(value: boolean): void {
    this.adhesionVolontaireIndividuelle.set(value);
  }

  onInformationChange(value: boolean): void {
    this.informationIndividuelleComplete.set(value);
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request: PdvRccConformiteRequest = {
      typeDispositif: this.typeDispositif(),
      accordCollectifMajoritaire: this.accordCollectifMajoritaire(),
      validationDREETSObtenue: this.validationDREETSObtenue(),
      delaiValidation15JRespectee: this.delaiValidation15JRespectee(),
      indemnitesAuMoinsLegales: this.indemnitesAuMoinsLegales(),
      adhesionVolontaireIndividuelle: this.adhesionVolontaireIndividuelle(),
      informationIndividuelleComplete: this.informationIndividuelleComplete(),
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.applyResult(r);
        this.calculating.set(false);
        this.snackBar.open('Analyse PDV / RCC calculée', 'OK', { duration: 2500 });
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

  private applyResult(r: PdvRccConformiteResponse): void {
    this.result.set(r);
    this.hydrateForm(r);
    this.showForm.set(false);
  }

  /**
   * Ré-injecte le snapshot d'inputs de la réponse dans les champs du
   * formulaire — sans quoi un clic « Modifier » repartirait des valeurs par
   * défaut au lieu des dernières valeurs saisies.
   */
  private hydrateForm(r: PdvRccConformiteResponse): void {
    this.typeDispositif.set(r.typeDispositif ?? 'RCC');
    this.accordCollectifMajoritaire.set(r.accordCollectifMajoritaire ?? false);
    this.validationDREETSObtenue.set(r.validationDREETSObtenue ?? false);
    this.delaiValidation15JRespectee.set(r.delaiValidation15JRespectee ?? null);
    this.indemnitesAuMoinsLegales.set(r.indemnitesAuMoinsLegales ?? false);
    this.adhesionVolontaireIndividuelle.set(r.adhesionVolontaireIndividuelle ?? false);
    this.informationIndividuelleComplete.set(r.informationIndividuelleComplete ?? false);
    // Valeurs persistées = saisie avocat — jamais de badge IA.
    this.provenanceType.set(null);
    this.provenanceAccord.set(null);
    this.provenanceDreets.set(null);
    this.provenanceIndemnites.set(null);
  }

  // ---------------------------------------------------------------------------
  // Helpers d'affichage du résultat
  // ---------------------------------------------------------------------------

  /**
   * Verdict → bannière de couleur. 3 niveaux :
   *  - CONFORME → vert
   *  - PARTIELLEMENT_CONFORME → or
   *  - NON_CONFORME → rouge
   */
  verdictBannerClass(v: PdvRccAnalyse): string {
    switch (v) {
      case 'CONFORME':
        return 'prc-verdict-banner prc-verdict-banner--conforme';
      case 'PARTIELLEMENT_CONFORME':
        return 'prc-verdict-banner prc-verdict-banner--partiel';
      case 'NON_CONFORME':
        return 'prc-verdict-banner prc-verdict-banner--nonconforme';
    }
  }

  verdictBannerLabel(v: PdvRccAnalyse): string {
    switch (v) {
      case 'CONFORME': return 'Dispositif conforme';
      case 'PARTIELLEMENT_CONFORME': return 'Dispositif partiellement conforme';
      case 'NON_CONFORME': return 'Dispositif non conforme';
    }
  }

  verdictBannerIcon(v: PdvRccAnalyse): string {
    switch (v) {
      case 'CONFORME': return 'check_circle';
      case 'PARTIELLEMENT_CONFORME': return 'warning_amber';
      case 'NON_CONFORME': return 'error_outline';
    }
  }

  /** Tracker pour les points d'irrégularité. */
  pointTrackKey(_index: number, p: PdvRccPointIrregularite): string {
    return p.code + ':' + p.libelle.slice(0, 30);
  }
}
