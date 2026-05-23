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
import { FauteInexcusableFrService } from '../../core/services/faute-inexcusable-fr.service';
import {
  FauteInexcusableFrEvaluation,
  FauteInexcusableFrFacteur,
  FauteInexcusableFrRequest,
  FauteInexcusableFrResponse,
} from '../../core/models/faute-inexcusable-fr.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { FauteInexcusableFrSectionPrefillRules } from './faute-inexcusable-fr-section-prefill-rules';

/**
 * SF-212-10 : composant Angular standalone pour l'outil décisionnel
 * "Faute inexcusable de l'employeur" — tool_id canonique
 * `F-DT-91-faute-inexcusable-employeur`. FRANCE uniquement (faute inexcusable
 * définie par la jurisprudence Cass. ass. plén. 24/06/2005 + L. 452-X CSS ;
 * en BE, régimes faute grave / intentionnelle distincts).
 *
 * Pattern de référence : `transfert-entreprise-fr-section` (F-DT-72,
 * SF-212-06) — formulaire 8 champs, POST de calcul, verdict 3 niveaux,
 * refresh dashboard, OnPush + markForCheck dans les subscribe.
 *
 * Verdict 3 niveaux :
 *  - FAUTE_INEXCUSABLE_PROBABLE (vert — conditions Cass. ass. plén. 24/06/2005 réunies)
 *  - FAUTE_INEXCUSABLE_POSSIBLE (or — indices convergents mais une condition non démontrée)
 *  - FAUTE_INEXCUSABLE_PEU_PROBABLE (rouge — conditions non réunies)
 *
 * Bannière alerte procédure : <b>toujours visible</b> — invariant mini-spec
 * (la distinction procédurale pôle social TJ vs CPH ne doit jamais être
 * oubliée).
 */
@Component({
  selector: 'app-faute-inexcusable-fr-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatSlideToggleModule,
    MatProgressSpinnerModule,
    LegalCitationsPipe,
  ],
  templateUrl: './faute-inexcusable-fr-section.component.html',
  styleUrl: './faute-inexcusable-fr-section.component.scss',
})
export class FauteInexcusableFrSectionComponent implements OnInit, OnChanges {
  // F-177 SF-177-03b : metadata statique consommée par le panel pour la card.
  static readonly TOOL_LABEL = 'FAUTE INEXCUSABLE DE L’EMPLOYEUR';
  static readonly TOOL_ICON = 'gpp_maybe';

  /**
   * SF-244 — délégué au helper partagé (parité stricte avec `prefillFromAi()`).
   * Retourne le nombre exact de champs pré-remplissables FR pour le badge tab.
   * Couvre les 4 champs `fauteInexcusable*`.
   */
  static getPrefillCount(input: {
    aiData?: TravailExtractedData | null;
    procedureChecks?: unknown[];
    aiQuestions?: unknown[];
    piecesManquantes?: unknown[];
    triggerEvents?: unknown[];
    workspaceCountry?: string;
  }): number {
    return FauteInexcusableFrSectionPrefillRules.computePrefillCount({
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
  result = signal<FauteInexcusableFrResponse | null>(null);

  // --- Form fields (8 champs du contrat backend) ---
  conscienceDangerEmployeurEtablie = signal<boolean>(false);
  signalementDangerPrior = signal<boolean>(false);
  mesuresPreventionPrises = signal<boolean>(true);
  documentUniqueEvalue = signal<boolean>(true);
  formationSecuriteProdiguee = signal<boolean>(true);
  tauxIpp = signal<number>(0);
  renteMensuelleEuros = signal<number | null>(null);
  salaireMensuelBrutEuros = signal<number>(0);

  // Provenance IA par champ pré-rempli — badge `auto_awesome` s'efface au
  // premier `onXxxChange()` manuel sur ce champ.
  provenanceConscienceDanger = signal<'IA' | null>(null);
  provenanceSignalementPrior = signal<'IA' | null>(null);
  provenanceMesuresPrevention = signal<'IA' | null>(null);
  provenanceTauxIpp = signal<'IA' | null>(null);

  constructor(
    private service: FauteInexcusableFrService,
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
   * Pré-fill IA (SF-212-10) — renseigne les 4 champs depuis le sous-objet
   * `faute_inexcusable_detail` (projeté à plat dans `travailExtractedData`).
   * Parité stricte avec `getPrefillCount()` : mêmes mappings, même gate
   * `workspaceCountry === 'FRANCE'`. No-op gracieux si `aiData` absent ou
   * dossier BE.
   */
  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    const ai = this.aiData;
    if (!ai) return;
    const rules = FauteInexcusableFrSectionPrefillRules;
    const ruleInput = { aiData: ai, workspaceCountry: this.workspaceCountry };

    const conscience = rules.computeConscienceDanger(ruleInput);
    if (conscience !== null) {
      this.conscienceDangerEmployeurEtablie.set(conscience);
      this.provenanceConscienceDanger.set('IA');
    }
    const signalement = rules.computeSignalementPrior(ruleInput);
    if (signalement !== null) {
      this.signalementDangerPrior.set(signalement);
      this.provenanceSignalementPrior.set('IA');
    }
    const mesures = rules.computeMesuresPrevention(ruleInput);
    if (mesures !== null) {
      this.mesuresPreventionPrises.set(mesures);
      this.provenanceMesuresPrevention.set('IA');
    }
    const ipp = rules.computeTauxIpp(ruleInput);
    if (ipp !== null) {
      this.tauxIpp.set(ipp);
      this.provenanceTauxIpp.set('IA');
    }
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  editMode(): void {
    this.showForm.set(true);
  }

  /** Form valide : FRANCE + IPP [0,100] + salaire ≥ 0. */
  formValid(): boolean {
    if (this.workspaceCountry !== 'FRANCE') return false;
    const ipp = this.tauxIpp();
    if (!Number.isFinite(ipp) || ipp < 0 || ipp > 100) return false;
    const salaire = this.salaireMensuelBrutEuros();
    if (!Number.isFinite(salaire) || salaire < 0) return false;
    const rente = this.renteMensuelleEuros();
    if (rente !== null && (rente < 0 || !Number.isFinite(rente))) return false;
    return true;
  }

  // --- Handlers — toute modification manuelle efface le badge IA du champ ---

  onConscienceChange(value: boolean): void {
    this.conscienceDangerEmployeurEtablie.set(value);
    this.provenanceConscienceDanger.set(null);
  }

  onSignalementChange(value: boolean): void {
    this.signalementDangerPrior.set(value);
    this.provenanceSignalementPrior.set(null);
  }

  onMesuresChange(value: boolean): void {
    this.mesuresPreventionPrises.set(value);
    this.provenanceMesuresPrevention.set(null);
  }

  onDuerChange(value: boolean): void {
    this.documentUniqueEvalue.set(value);
  }

  onFormationChange(value: boolean): void {
    this.formationSecuriteProdiguee.set(value);
  }

  onTauxIppChange(value: number | string | null): void {
    if (value === null || value === '' || value === undefined) {
      this.tauxIpp.set(0);
    } else {
      const num = typeof value === 'number' ? value : Number.parseFloat(String(value));
      this.tauxIpp.set(Number.isFinite(num) ? Math.max(0, Math.min(100, num)) : 0);
    }
    this.provenanceTauxIpp.set(null);
  }

  onRenteChange(value: number | string | null): void {
    if (value === null || value === '' || value === undefined) {
      this.renteMensuelleEuros.set(null);
    } else {
      const num = typeof value === 'number' ? value : Number.parseFloat(String(value));
      this.renteMensuelleEuros.set(Number.isFinite(num) && num >= 0 ? num : null);
    }
  }

  onSalaireChange(value: number | string | null): void {
    if (value === null || value === '' || value === undefined) {
      this.salaireMensuelBrutEuros.set(0);
    } else {
      const num = typeof value === 'number' ? value : Number.parseFloat(String(value));
      this.salaireMensuelBrutEuros.set(Number.isFinite(num) && num >= 0 ? num : 0);
    }
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request: FauteInexcusableFrRequest = {
      conscienceDangerEmployeurEtablie: this.conscienceDangerEmployeurEtablie(),
      signalementDangerPrior: this.signalementDangerPrior(),
      mesuresPreventionPrises: this.mesuresPreventionPrises(),
      documentUniqueEvalue: this.documentUniqueEvalue(),
      formationSecuriteProdiguee: this.formationSecuriteProdiguee(),
      tauxIpp: this.tauxIpp(),
      renteMensuelleEuros: this.renteMensuelleEuros(),
      salaireMensuelBrutEuros: this.salaireMensuelBrutEuros(),
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.applyResult(r);
        this.calculating.set(false);
        this.snackBar.open('Évaluation de la faute inexcusable calculée', 'OK', { duration: 2500 });
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

  private applyResult(r: FauteInexcusableFrResponse): void {
    this.result.set(r);
    this.hydrateForm(r);
    this.showForm.set(false);
  }

  /**
   * Ré-injecte le snapshot d'inputs de la réponse dans les champs du
   * formulaire — sans quoi un clic « Modifier » repartirait des valeurs par
   * défaut au lieu des dernières valeurs saisies.
   */
  private hydrateForm(r: FauteInexcusableFrResponse): void {
    this.conscienceDangerEmployeurEtablie.set(r.conscienceDangerEmployeurEtablie ?? false);
    this.signalementDangerPrior.set(r.signalementDangerPrior ?? false);
    this.mesuresPreventionPrises.set(r.mesuresPreventionPrises ?? true);
    this.documentUniqueEvalue.set(r.documentUniqueEvalue ?? true);
    this.formationSecuriteProdiguee.set(r.formationSecuriteProdiguee ?? true);
    this.tauxIpp.set(r.tauxIpp ?? 0);
    this.renteMensuelleEuros.set(r.renteMensuelleEuros ?? null);
    this.salaireMensuelBrutEuros.set(r.salaireMensuelBrutEuros ?? 0);
    // Valeurs persistées = saisie avocat — jamais de badge IA.
    this.provenanceConscienceDanger.set(null);
    this.provenanceSignalementPrior.set(null);
    this.provenanceMesuresPrevention.set(null);
    this.provenanceTauxIpp.set(null);
  }

  // ---------------------------------------------------------------------------
  // Helpers d'affichage du résultat
  // ---------------------------------------------------------------------------

  /**
   * Verdict → bannière de couleur. 3 niveaux :
   *  - FAUTE_INEXCUSABLE_PROBABLE → vert
   *  - FAUTE_INEXCUSABLE_POSSIBLE → or
   *  - FAUTE_INEXCUSABLE_PEU_PROBABLE → rouge
   */
  verdictBannerClass(v: FauteInexcusableFrEvaluation): string {
    switch (v) {
      case 'FAUTE_INEXCUSABLE_PROBABLE':
        return 'fie-verdict-banner fie-verdict-banner--probable';
      case 'FAUTE_INEXCUSABLE_POSSIBLE':
        return 'fie-verdict-banner fie-verdict-banner--possible';
      case 'FAUTE_INEXCUSABLE_PEU_PROBABLE':
        return 'fie-verdict-banner fie-verdict-banner--peu-probable';
    }
  }

  verdictBannerLabel(v: FauteInexcusableFrEvaluation): string {
    switch (v) {
      case 'FAUTE_INEXCUSABLE_PROBABLE': return 'Faute inexcusable probable';
      case 'FAUTE_INEXCUSABLE_POSSIBLE': return 'Faute inexcusable possible';
      case 'FAUTE_INEXCUSABLE_PEU_PROBABLE': return 'Faute inexcusable peu probable';
    }
  }

  verdictBannerIcon(v: FauteInexcusableFrEvaluation): string {
    switch (v) {
      case 'FAUTE_INEXCUSABLE_PROBABLE': return 'check_circle';
      case 'FAUTE_INEXCUSABLE_POSSIBLE': return 'help_outline';
      case 'FAUTE_INEXCUSABLE_PEU_PROBABLE': return 'cancel';
    }
  }

  /** Libellé court d'un facteur (utilisé pour le tracking dans la liste). */
  facteurTrackKey(_index: number, f: FauteInexcusableFrFacteur): string {
    return `${f.code}:${f.libelle}`;
  }

  /** Format euros locale FR. */
  formatEuros(v: number | null | undefined): string {
    if (v === null || v === undefined || !Number.isFinite(v)) return '—';
    return new Intl.NumberFormat('fr-FR', {
      style: 'currency',
      currency: 'EUR',
      maximumFractionDigits: 2,
    }).format(v);
  }
}
