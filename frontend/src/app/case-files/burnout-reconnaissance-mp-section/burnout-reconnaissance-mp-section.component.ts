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
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { BurnoutReconnaissanceMpService } from '../../core/services/burnout-reconnaissance-mp.service';
import {
  BurnoutAnalyseChances,
  BurnoutFacteurDossier,
  BurnoutReconnaissanceMpRequest,
  BurnoutReconnaissanceMpResponse,
} from '../../core/models/burnout-reconnaissance-mp.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { BurnoutReconnaissanceMpSectionPrefillRules } from './burnout-reconnaissance-mp-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-212-28 : composant Angular standalone pour l'outil décisionnel
 * "Burn-out — reconnaissance maladie professionnelle hors tableau" —
 * tool_id canonique `F-DT-64-burnout-reconnaissance-mp`. FRANCE uniquement
 * (procédure CRRMP — L. 461-1 al. 4 et 5 CSS ; circulaire DGT 2016/01).
 *
 * Pattern de référence : `lanceur-alerte-protection-section` (F-DT-61,
 * SF-212-26) — formulaire, POST de calcul, verdict 3 niveaux, refresh
 * dashboard, OnPush + markForCheck dans les subscribe.
 *
 * Verdict 3 niveaux :
 *  - CHANCES_IMPORTANTES (vert — dossier solide, taux IPP rempli, lien causal)
 *  - CHANCES_MODEREES (or — éléments incomplets, pièces complémentaires recommandées)
 *  - CHANCES_FAIBLES (rouge — IPP < 25 % OU faisceau d'indices insuffisant)
 *
 * Bannière FR-only : si le workspace n'est pas en FRANCE, l'écran affiche
 * uniquement un avertissement pédagogique (la BE reconnaît le burn-out via
 * Fedris dans un cadre distinct).
 */
@Component({
  selector: 'app-burnout-reconnaissance-mp-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatSlideToggleModule,
    MatProgressSpinnerModule,
    LegalCitationsPipe,
    ToolJurisprudenceCitationsComponent,
  ],
  templateUrl: './burnout-reconnaissance-mp-section.component.html',
  styleUrl: './burnout-reconnaissance-mp-section.component.scss',
})
export class BurnoutReconnaissanceMpSectionComponent implements OnInit, OnChanges {
  // F-JU-03 — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-DT-64-burnout-reconnaissance-mp';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommée par le panel pour la card.
  static readonly TOOL_LABEL = 'BURN-OUT — RECONNAISSANCE MP';
  static readonly TOOL_ICON = 'sentiment_very_dissatisfied';

  /**
   * SF-244 — délégué au helper partagé (parité stricte avec `prefillFromAi()`).
   * Retourne le nombre exact de champs pré-remplissables FR pour le badge tab.
   * Couvre les 4 champs `burnout*`.
   */
  static getPrefillCount(input: {
    aiData?: TravailExtractedData | null;
    procedureChecks?: unknown[];
    aiQuestions?: unknown[];
    piecesManquantes?: unknown[];
    triggerEvents?: unknown[];
    workspaceCountry?: string;
  }): number {
    return BurnoutReconnaissanceMpSectionPrefillRules.computePrefillCount({
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
  result = signal<BurnoutReconnaissanceMpResponse | null>(null);

  // --- Form fields (8 champs du contrat backend) ---
  diagnosticBurnoutPose = signal<boolean>(false);
  tauxIPPEstime = signal<number>(0);
  anneesExpositionProfessionnelle = signal<number>(0);
  surchargeChargeDocumentee = signal<boolean>(false);
  manquementsSecuriteDocumentes = signal<boolean>(false);
  harcelementConcomitant = signal<boolean>(false);
  arretsMaladieMultiples = signal<boolean>(false);
  lienCausalDirectEtabli = signal<boolean>(false);

  // Provenance IA par champ pré-rempli — badge `auto_awesome` s'efface au
  // premier `onXxxChange()` manuel sur ce champ.
  provenanceDiagnostic = signal<'IA' | null>(null);
  provenanceTauxIpp = signal<'IA' | null>(null);
  provenanceSurcharge = signal<'IA' | null>(null);
  provenanceArretsMaladie = signal<'IA' | null>(null);

  constructor(
    private service: BurnoutReconnaissanceMpService,
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
   * Pré-fill IA (SF-212-28) — renseigne les 4 champs depuis le sous-objet
   * `burnout_detail` (projeté à plat dans `travailExtractedData` via Jackson
   * @JsonUnwrapped côté backend). Parité stricte avec `getPrefillCount()` :
   * mêmes mappings, même gate `workspaceCountry === 'FRANCE'`.
   */
  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    const ai = this.aiData;
    if (!ai) return;
    const rules = BurnoutReconnaissanceMpSectionPrefillRules;
    const ruleInput = { aiData: ai, workspaceCountry: this.workspaceCountry };

    const diagnostic = rules.computeDiagnostic(ruleInput);
    if (diagnostic !== null) {
      this.diagnosticBurnoutPose.set(diagnostic);
      this.provenanceDiagnostic.set('IA');
    }
    const taux = rules.computeTauxIpp(ruleInput);
    if (taux !== null) {
      this.tauxIPPEstime.set(taux);
      this.provenanceTauxIpp.set('IA');
    }
    const surcharge = rules.computeSurchargeDocumentee(ruleInput);
    if (surcharge !== null) {
      this.surchargeChargeDocumentee.set(surcharge);
      this.provenanceSurcharge.set('IA');
    }
    const arrets = rules.computeArretsMaladie(ruleInput);
    if (arrets !== null) {
      this.arretsMaladieMultiples.set(arrets);
      this.provenanceArretsMaladie.set('IA');
    }
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  editMode(): void {
    this.showForm.set(true);
  }

  /** Form valide : FRANCE + taux IPP bornes [0, 100] + années expo >= 0. */
  formValid(): boolean {
    if (this.workspaceCountry !== 'FRANCE') return false;
    const taux = this.tauxIPPEstime();
    if (taux < 0 || taux > 100) return false;
    if (this.anneesExpositionProfessionnelle() < 0) return false;
    return true;
  }

  // --- Handlers — toute modification manuelle efface le badge IA du champ ---

  onDiagnosticChange(value: boolean): void {
    this.diagnosticBurnoutPose.set(value);
    this.provenanceDiagnostic.set(null);
  }

  onTauxIppChange(value: number): void {
    this.tauxIPPEstime.set(value);
    this.provenanceTauxIpp.set(null);
  }

  onAnneesExpoChange(value: number): void {
    this.anneesExpositionProfessionnelle.set(value);
  }

  onSurchargeChange(value: boolean): void {
    this.surchargeChargeDocumentee.set(value);
    this.provenanceSurcharge.set(null);
  }

  onManquementsChange(value: boolean): void {
    this.manquementsSecuriteDocumentes.set(value);
  }

  onHarcelementChange(value: boolean): void {
    this.harcelementConcomitant.set(value);
  }

  onArretsMaladieChange(value: boolean): void {
    this.arretsMaladieMultiples.set(value);
    this.provenanceArretsMaladie.set(null);
  }

  onLienCausalChange(value: boolean): void {
    this.lienCausalDirectEtabli.set(value);
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request: BurnoutReconnaissanceMpRequest = {
      diagnosticBurnoutPose: this.diagnosticBurnoutPose(),
      tauxIPPEstime: this.tauxIPPEstime(),
      anneesExpositionProfessionnelle: this.anneesExpositionProfessionnelle(),
      surchargeChargeDocumentee: this.surchargeChargeDocumentee(),
      manquementsSecuriteDocumentes: this.manquementsSecuriteDocumentes(),
      harcelementConcomitant: this.harcelementConcomitant(),
      arretsMaladieMultiples: this.arretsMaladieMultiples(),
      lienCausalDirectEtabli: this.lienCausalDirectEtabli(),
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.applyResult(r);
        this.calculating.set(false);
        this.snackBar.open('Analyse des chances de reconnaissance CRRMP calculée', 'OK', { duration: 2500 });
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

  private applyResult(r: BurnoutReconnaissanceMpResponse): void {
    this.result.set(r);
    this.hydrateForm(r);
    this.showForm.set(false);
  }

  /**
   * Ré-injecte le snapshot d'inputs de la réponse dans les champs du
   * formulaire — sans quoi un clic « Modifier » repartirait des valeurs
   * par défaut au lieu des dernières valeurs saisies.
   */
  private hydrateForm(r: BurnoutReconnaissanceMpResponse): void {
    this.diagnosticBurnoutPose.set(r.diagnosticBurnoutPose ?? false);
    this.tauxIPPEstime.set(r.tauxIPPEstime ?? 0);
    this.anneesExpositionProfessionnelle.set(r.anneesExpositionProfessionnelle ?? 0);
    this.surchargeChargeDocumentee.set(r.surchargeChargeDocumentee ?? false);
    this.manquementsSecuriteDocumentes.set(r.manquementsSecuriteDocumentes ?? false);
    this.harcelementConcomitant.set(r.harcelementConcomitant ?? false);
    this.arretsMaladieMultiples.set(r.arretsMaladieMultiples ?? false);
    this.lienCausalDirectEtabli.set(r.lienCausalDirectEtabli ?? false);
    // Valeurs persistées = saisie avocat — jamais de badge IA.
    this.provenanceDiagnostic.set(null);
    this.provenanceTauxIpp.set(null);
    this.provenanceSurcharge.set(null);
    this.provenanceArretsMaladie.set(null);
  }

  // ---------------------------------------------------------------------------
  // Helpers d'affichage du résultat
  // ---------------------------------------------------------------------------

  /**
   * Verdict → bannière de couleur. 3 niveaux :
   *  - CHANCES_IMPORTANTES → vert
   *  - CHANCES_MODEREES → or
   *  - CHANCES_FAIBLES → rouge
   */
  verdictBannerClass(v: BurnoutAnalyseChances): string {
    switch (v) {
      case 'CHANCES_IMPORTANTES':
        return 'bo-verdict-banner bo-verdict-banner--importantes';
      case 'CHANCES_MODEREES':
        return 'bo-verdict-banner bo-verdict-banner--moderees';
      case 'CHANCES_FAIBLES':
        return 'bo-verdict-banner bo-verdict-banner--faibles';
    }
  }

  verdictBannerLabel(v: BurnoutAnalyseChances): string {
    switch (v) {
      case 'CHANCES_IMPORTANTES': return 'Chances importantes';
      case 'CHANCES_MODEREES': return 'Chances modérées';
      case 'CHANCES_FAIBLES': return 'Chances faibles';
    }
  }

  verdictBannerIcon(v: BurnoutAnalyseChances): string {
    switch (v) {
      case 'CHANCES_IMPORTANTES': return 'verified';
      case 'CHANCES_MODEREES': return 'help_outline';
      case 'CHANCES_FAIBLES': return 'report_problem';
    }
  }

  /** Libellé court d'un facteur (utilisé pour le tracking dans la liste). */
  facteurTrackKey(_index: number, f: BurnoutFacteurDossier): string {
    return `${f.code}:${f.libelle}`;
  }

  /** Format affichage du délai CRRMP — toujours 4 à 6 mois. */
  formatDelaiInstruction(): string {
    return '4 à 6 mois';
  }
}
