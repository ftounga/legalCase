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
import { ForfaitJoursFrService } from '../../core/services/forfait-jours-fr.service';
import {
  ForfaitJoursFacteurInvalidite,
  ForfaitJoursValiditeForfait,
  ForfaitJoursValiditeRequest,
  ForfaitJoursValiditeResponse,
} from '../../core/models/forfait-jours-fr.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { ForfaitJoursFrSectionPrefillRules } from './forfait-jours-fr-section-prefill-rules';

/**
 * SF-212-04 : composant Angular standalone pour l'outil décisionnel
 * "Forfait jours — validité et rappel HS" — tool_id canonique
 * `F-DT-50-forfait-jours-validite`. FRANCE uniquement (régime forfait jours
 * L. 3121-58 à L. 3121-66 CT avec exigences post-Cass. soc. 29/06/2011
 * n°09-71.107 ; L. 3245-1 CT prescription 3 ans rappel salaire).
 *
 * Pattern de référence : `licenciement-faute-grave-lourd-section` (F-DT-36,
 * SF-212-02) — formulaire de saisie, POST de calcul, verdict 3 niveaux,
 * refresh dashboard, OnPush + markForCheck dans les subscribe.
 *
 * Verdict 3 niveaux :
 *  - VALIDE (vert — convention de forfait conforme L.3121-58+ CT)
 *  - PARTIELLEMENT_NULLE (or — un ou plusieurs critères manquants,
 *    rappel HS possible)
 *  - NULLE (rouge — pas d'accord collectif ou carences multiples ;
 *    salarié relève du régime de droit commun, rappel HS sur 3 ans)
 */
@Component({
  selector: 'app-forfait-jours-fr-section',
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
  templateUrl: './forfait-jours-fr-section.component.html',
  styleUrl: './forfait-jours-fr-section.component.scss',
})
export class ForfaitJoursFrSectionComponent implements OnInit, OnChanges {
  // F-177 SF-177-03b : metadata statique consommée par le panel pour la card.
  static readonly TOOL_LABEL = 'FORFAIT JOURS — VALIDITÉ';
  static readonly TOOL_ICON = 'event_available';

  /**
   * SF-244 — délégué au helper partagé (parité stricte avec `prefillFromAi()`).
   * Retourne le nombre exact de champs pré-remplissables FR pour le badge tab.
   * Couvre les 5 champs `forfaitJours*`.
   */
  static getPrefillCount(input: {
    aiData?: TravailExtractedData | null;
    procedureChecks?: unknown[];
    aiQuestions?: unknown[];
    piecesManquantes?: unknown[];
    triggerEvents?: unknown[];
    workspaceCountry?: string;
  }): number {
    return ForfaitJoursFrSectionPrefillRules.computePrefillCount({
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
  result = signal<ForfaitJoursValiditeResponse | null>(null);

  // --- Form fields (10 champs du contrat backend) ---
  accordCollectifExiste = signal<boolean>(false);
  accordGarantitSuiviCharge = signal<boolean>(false);
  entretienAnnuelRealise = signal<boolean>(false);
  documentControleMensuelExiste = signal<boolean>(false);
  categorieAutonomeConfirmee = signal<boolean>(false);
  nbJoursForfait = signal<number>(218);
  ancienneteMois = signal<number | null>(null);
  salaireMensuelBrutEuros = signal<number | null>(null);
  hsEstimeesParSemaine = signal<number>(10);
  nbSemainesParAn = signal<number>(45);

  // Provenance IA par champ pré-rempli — badge `auto_awesome` s'efface au
  // premier `onXxxChange()` manuel sur ce champ.
  provenanceAccordCollectif = signal<'IA' | null>(null);
  provenanceEntretienAnnuel = signal<'IA' | null>(null);
  provenanceDocumentControle = signal<'IA' | null>(null);
  provenanceCategorieAutonome = signal<'IA' | null>(null);
  provenanceNbJours = signal<'IA' | null>(null);

  constructor(
    private service: ForfaitJoursFrService,
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
   * Pré-fill IA (SF-212-04) — renseigne les 5 champs depuis le sous-objet
   * `forfait_jours_detail` (projeté à plat dans `travailExtractedData`).
   * Parité stricte avec `getPrefillCount()` : mêmes mappings, même gate
   * `workspaceCountry === 'FRANCE'`. No-op gracieux si `aiData` absent ou
   * dossier BE.
   */
  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    const ai = this.aiData;
    if (!ai) return;
    const rules = ForfaitJoursFrSectionPrefillRules;
    const ruleInput = { aiData: ai, workspaceCountry: this.workspaceCountry };

    const accord = rules.computeAccordCollectifExiste(ruleInput);
    if (accord !== null) {
      this.accordCollectifExiste.set(accord);
      this.provenanceAccordCollectif.set('IA');
    }
    const entretien = rules.computeEntretienAnnuelRealise(ruleInput);
    if (entretien !== null) {
      this.entretienAnnuelRealise.set(entretien);
      this.provenanceEntretienAnnuel.set('IA');
    }
    const doc = rules.computeDocumentControleMensuelExiste(ruleInput);
    if (doc !== null) {
      this.documentControleMensuelExiste.set(doc);
      this.provenanceDocumentControle.set('IA');
    }
    const cat = rules.computeCategorieAutonomeConfirmee(ruleInput);
    if (cat !== null) {
      this.categorieAutonomeConfirmee.set(cat);
      this.provenanceCategorieAutonome.set('IA');
    }
    const nbJours = rules.computeNbJoursForfait(ruleInput);
    if (nbJours !== null) {
      this.nbJoursForfait.set(nbJours);
      this.provenanceNbJours.set('IA');
    }
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  editMode(): void {
    this.showForm.set(true);
  }

  /** Form valide : FRANCE + champs numériques bornés. */
  formValid(): boolean {
    if (this.workspaceCountry !== 'FRANCE') return false;
    const nb = this.nbJoursForfait();
    if (!Number.isFinite(nb) || nb < 0 || nb > 365) return false;
    const a = this.ancienneteMois();
    if (a === null || !Number.isFinite(a) || a < 0) return false;
    const s = this.salaireMensuelBrutEuros();
    if (s === null || !Number.isFinite(s) || s <= 0) return false;
    if (!Number.isFinite(this.hsEstimeesParSemaine()) || this.hsEstimeesParSemaine() < 0) return false;
    if (!Number.isFinite(this.nbSemainesParAn()) || this.nbSemainesParAn() < 0) return false;
    return true;
  }

  // --- Handlers — toute modification manuelle efface le badge IA du champ ---

  onAccordCollectifChange(value: boolean): void {
    this.accordCollectifExiste.set(value);
    this.provenanceAccordCollectif.set(null);
    if (!value) {
      // Pas d'accord = pas de garantie suivi charge a fortiori.
      this.accordGarantitSuiviCharge.set(false);
    }
  }

  onAccordGarantitChange(value: boolean): void {
    this.accordGarantitSuiviCharge.set(value);
  }

  onEntretienAnnuelChange(value: boolean): void {
    this.entretienAnnuelRealise.set(value);
    this.provenanceEntretienAnnuel.set(null);
  }

  onDocumentControleChange(value: boolean): void {
    this.documentControleMensuelExiste.set(value);
    this.provenanceDocumentControle.set(null);
  }

  onCategorieAutonomeChange(value: boolean): void {
    this.categorieAutonomeConfirmee.set(value);
    this.provenanceCategorieAutonome.set(null);
  }

  onNbJoursForfaitChange(value: number | string | null): void {
    const num = typeof value === 'number' ? value : Number.parseInt(String(value ?? ''), 10);
    this.nbJoursForfait.set(Number.isFinite(num) && num >= 0 ? num : 0);
    this.provenanceNbJours.set(null);
  }

  onAncienneteMoisChange(value: number | string | null): void {
    if (value === null || value === '' || value === undefined) {
      this.ancienneteMois.set(null);
    } else {
      const num = typeof value === 'number' ? value : Number.parseInt(String(value), 10);
      this.ancienneteMois.set(Number.isFinite(num) && num >= 0 ? num : null);
    }
  }

  onSalaireChange(value: number | string | null): void {
    if (value === null || value === '' || value === undefined) {
      this.salaireMensuelBrutEuros.set(null);
    } else {
      const num = typeof value === 'number' ? value : Number.parseFloat(String(value));
      this.salaireMensuelBrutEuros.set(Number.isFinite(num) && num > 0 ? num : null);
    }
  }

  onHsEstimeesChange(value: number | string | null): void {
    const num = typeof value === 'number' ? value : Number.parseInt(String(value ?? ''), 10);
    this.hsEstimeesParSemaine.set(Number.isFinite(num) && num >= 0 ? num : 0);
  }

  onNbSemainesParAnChange(value: number | string | null): void {
    const num = typeof value === 'number' ? value : Number.parseInt(String(value ?? ''), 10);
    this.nbSemainesParAn.set(Number.isFinite(num) && num >= 0 ? num : 0);
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request: ForfaitJoursValiditeRequest = {
      accordCollectifExiste: this.accordCollectifExiste(),
      accordGarantitSuiviCharge: this.accordGarantitSuiviCharge(),
      entretienAnnuelRealise: this.entretienAnnuelRealise(),
      documentControleMensuelExiste: this.documentControleMensuelExiste(),
      categorieAutonomeConfirmee: this.categorieAutonomeConfirmee(),
      nbJoursForfait: this.nbJoursForfait(),
      ancienneteMois: this.ancienneteMois()!,
      salaireMensuelBrutEuros: this.salaireMensuelBrutEuros()!,
      hsEstimeesParSemaine: this.hsEstimeesParSemaine(),
      nbSemainesParAn: this.nbSemainesParAn(),
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.applyResult(r);
        this.calculating.set(false);
        this.snackBar.open('Analyse de validité du forfait jours calculée', 'OK', { duration: 2500 });
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
        if (r) this.applyResult(r);
        this.loading.set(false);
        this.cdr.markForCheck();
      },
      error: () => {
        // 204 ou 404 attendu si aucune analyse — on reste en mode formulaire.
        this.loading.set(false);
        this.cdr.markForCheck();
      },
    });
  }

  private applyResult(r: ForfaitJoursValiditeResponse): void {
    this.result.set(r);
    this.hydrateForm(r);
    this.showForm.set(false);
  }

  /**
   * Ré-injecte le snapshot d'inputs de la réponse dans les champs du
   * formulaire — sans quoi un clic « Modifier » repartirait des valeurs par
   * défaut au lieu des dernières valeurs saisies.
   */
  private hydrateForm(r: ForfaitJoursValiditeResponse): void {
    this.accordCollectifExiste.set(!!r.accordCollectifExiste);
    this.accordGarantitSuiviCharge.set(!!r.accordGarantitSuiviCharge);
    this.entretienAnnuelRealise.set(!!r.entretienAnnuelRealise);
    this.documentControleMensuelExiste.set(!!r.documentControleMensuelExiste);
    this.categorieAutonomeConfirmee.set(!!r.categorieAutonomeConfirmee);
    this.nbJoursForfait.set(r.nbJoursForfait ?? 218);
    this.ancienneteMois.set(r.ancienneteMois);
    this.salaireMensuelBrutEuros.set(r.salaireMensuelBrutEuros);
    this.hsEstimeesParSemaine.set(r.hsEstimeesParSemaine ?? 10);
    this.nbSemainesParAn.set(r.nbSemainesParAn ?? 45);
    // Valeurs persistées = saisie avocat — jamais de badge IA.
    this.provenanceAccordCollectif.set(null);
    this.provenanceEntretienAnnuel.set(null);
    this.provenanceDocumentControle.set(null);
    this.provenanceCategorieAutonome.set(null);
    this.provenanceNbJours.set(null);
  }

  // ---------------------------------------------------------------------------
  // Helpers d'affichage du résultat
  // ---------------------------------------------------------------------------

  /**
   * Verdict → bannière de couleur. 3 niveaux :
   *  - VALIDE → vert
   *  - PARTIELLEMENT_NULLE → or
   *  - NULLE → rouge
   */
  verdictBannerClass(v: ForfaitJoursValiditeForfait): string {
    switch (v) {
      case 'VALIDE':
        return 'fj-verdict-banner fj-verdict-banner--valide';
      case 'PARTIELLEMENT_NULLE':
        return 'fj-verdict-banner fj-verdict-banner--partielle';
      case 'NULLE':
        return 'fj-verdict-banner fj-verdict-banner--nulle';
    }
  }

  verdictBannerLabel(v: ForfaitJoursValiditeForfait): string {
    switch (v) {
      case 'VALIDE': return 'Forfait jours valide';
      case 'PARTIELLEMENT_NULLE': return 'Forfait jours partiellement nul';
      case 'NULLE': return 'Forfait jours nul';
    }
  }

  verdictBannerIcon(v: ForfaitJoursValiditeForfait): string {
    switch (v) {
      case 'VALIDE': return 'check_circle';
      case 'PARTIELLEMENT_NULLE': return 'warning';
      case 'NULLE': return 'error';
    }
  }

  /** Format euros FR (Locale.FRANCE-like). */
  formatEuros(amount: number | null | undefined): string {
    if (amount === null || amount === undefined || !Number.isFinite(amount)) return '—';
    return new Intl.NumberFormat('fr-FR').format(Math.round(amount)) + ' €';
  }

  /** Libellé court d'un facteur (utilisé pour le tracking dans la liste). */
  facteurTrackKey(_index: number, f: ForfaitJoursFacteurInvalidite): string {
    return `${f.code}:${f.libelle}`;
  }
}
