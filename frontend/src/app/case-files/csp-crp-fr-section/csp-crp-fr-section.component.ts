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
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CspCrpFrService } from '../../core/services/csp-crp-fr.service';
import {
  CspCrpConformiteCsp,
  CspCrpConformiteRequest,
  CspCrpConformiteResponse,
  CspCrpPointNonConformite,
} from '../../core/models/csp-crp-fr.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { CspCrpFrSectionPrefillRules } from './csp-crp-fr-section-prefill-rules';

/**
 * SF-212-08 : composant Angular standalone pour l'outil décisionnel
 * "CSP/CRP — conformité de la proposition" — tool_id canonique
 * `F-DT-44-csp-crp-conformite`. FRANCE uniquement (CSP L. 1233-65 à
 * L. 1233-70 CT ; ANI CSP 19/07/2011 ; DARES — entreprises < 1 000 salariés).
 *
 * Pattern de référence : `forfait-jours-fr-section` (F-DT-50, SF-212-04) —
 * formulaire de saisie, POST de calcul, verdict 3 niveaux, encadré ASP,
 * refresh dashboard, OnPush + markForCheck dans les subscribe.
 *
 * Verdict 3 niveaux :
 *  - CONFORME (vert — proposition CSP conforme L. 1233-65 à L. 1233-70 CT)
 *  - PARTIELLEMENT_CONFORME (or — un ou plusieurs points de non-conformité,
 *    indemnisation du préjudice possible)
 *  - NON_CONFORME (rouge — CSP non proposé ou vices multiples ; contribution
 *    Pôle emploi 2 mois de salaire + dommages-intérêts)
 */
@Component({
  selector: 'app-csp-crp-fr-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatSlideToggleModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    LegalCitationsPipe,
  ],
  templateUrl: './csp-crp-fr-section.component.html',
  styleUrl: './csp-crp-fr-section.component.scss',
})
export class CspCrpFrSectionComponent implements OnInit, OnChanges {
  // F-177 SF-177-03b : metadata statique consommée par le panel pour la card.
  static readonly TOOL_LABEL = 'CSP/CRP — CONFORMITÉ';
  static readonly TOOL_ICON = 'gavel';

  /** Seuil d'applicabilité — entreprises < 1 000 salariés (L. 1233-66 CT). */
  static readonly SEUIL_EFFECTIF = 1_000;

  /**
   * SF-244 — délégué au helper partagé (parité stricte avec `prefillFromAi()`).
   * Retourne le nombre exact de champs pré-remplissables FR pour le badge tab.
   * Couvre les 6 champs `csp*`.
   */
  static getPrefillCount(input: {
    aiData?: TravailExtractedData | null;
    procedureChecks?: unknown[];
    aiQuestions?: unknown[];
    piecesManquantes?: unknown[];
    triggerEvents?: unknown[];
    workspaceCountry?: string;
  }): number {
    return CspCrpFrSectionPrefillRules.computePrefillCount({
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
  result = signal<CspCrpConformiteResponse | null>(null);

  // --- Form fields (9 champs du contrat backend) ---
  effectifEntreprise = signal<number | null>(null);
  cspPropose = signal<boolean>(false);
  documentInformationRemis = signal<boolean>(false);
  delaiReflexionMentionne = signal<boolean>(false);
  dateRemise = signal<string | null>(null);
  dateEntretienPrealable = signal<string | null>(null);
  /** Tri-état adhésion — string pour mat-select : 'OUI' | 'NON' | 'INCONNU'. */
  adhesionSalarie = signal<'OUI' | 'NON' | 'INCONNU'>('INCONNU');
  salaireMensuelBrutEuros = signal<number | null>(null);
  remunerationBrute12MoisEuros = signal<number | null>(null);

  // Provenance IA par champ pré-rempli — badge `auto_awesome` s'efface au
  // premier `onXxxChange()` manuel sur ce champ.
  provenanceEffectif = signal<'IA' | null>(null);
  provenanceCspPropose = signal<'IA' | null>(null);
  provenanceDocumentRemis = signal<'IA' | null>(null);
  provenanceDateRemise = signal<'IA' | null>(null);
  provenanceAdhesion = signal<'IA' | null>(null);
  provenanceSalaire = signal<'IA' | null>(null);

  constructor(
    private service: CspCrpFrService,
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
   * Pré-fill IA (SF-212-08) — renseigne les 6 champs depuis le sous-objet
   * `csp_detail` (projeté à plat dans `travailExtractedData`). Parité
   * stricte avec `getPrefillCount()` : mêmes mappings, même gate
   * `workspaceCountry === 'FRANCE'`. No-op gracieux si `aiData` absent ou
   * dossier BE.
   */
  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    const ai = this.aiData;
    if (!ai) return;
    const rules = CspCrpFrSectionPrefillRules;
    const ruleInput = { aiData: ai, workspaceCountry: this.workspaceCountry };

    const eff = rules.computeEffectifEntreprise(ruleInput);
    if (eff !== null) {
      this.effectifEntreprise.set(eff);
      this.provenanceEffectif.set('IA');
    }
    const propose = rules.computeCspPropose(ruleInput);
    if (propose !== null) {
      this.cspPropose.set(propose);
      this.provenanceCspPropose.set('IA');
    }
    const docRemis = rules.computeDocumentInformationRemis(ruleInput);
    if (docRemis !== null) {
      this.documentInformationRemis.set(docRemis);
      this.provenanceDocumentRemis.set('IA');
    }
    const dateR = rules.computeDateRemise(ruleInput);
    if (dateR !== null) {
      this.dateRemise.set(dateR);
      this.provenanceDateRemise.set('IA');
    }
    const adh = rules.computeAdhesionSalarie(ruleInput);
    if (adh !== null) {
      this.adhesionSalarie.set(adh ? 'OUI' : 'NON');
      this.provenanceAdhesion.set('IA');
    }
    const sal = rules.computeSalaireMensuelBrutEuros(ruleInput);
    if (sal !== null) {
      this.salaireMensuelBrutEuros.set(sal);
      // La rémunération 12 mois peut être estimée si non fournie.
      if (this.remunerationBrute12MoisEuros() === null) {
        this.remunerationBrute12MoisEuros.set(Math.round(sal * 12 * 100) / 100);
      }
      this.provenanceSalaire.set('IA');
    }
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  editMode(): void {
    this.showForm.set(true);
  }

  /** Affiche le message informatif "outil réservé < 1 000 salariés" ? */
  effectifHorsChamp(): boolean {
    const e = this.effectifEntreprise();
    return e !== null && Number.isFinite(e) && e >= CspCrpFrSectionComponent.SEUIL_EFFECTIF;
  }

  /** Form valide : FRANCE + champs numériques bornés. */
  formValid(): boolean {
    if (this.workspaceCountry !== 'FRANCE') return false;
    const e = this.effectifEntreprise();
    if (e === null || !Number.isFinite(e) || e < 0) return false;
    const s = this.salaireMensuelBrutEuros();
    if (s === null || !Number.isFinite(s) || s <= 0) return false;
    const r = this.remunerationBrute12MoisEuros();
    if (r === null || !Number.isFinite(r) || r < 0) return false;
    return true;
  }

  // --- Handlers — toute modification manuelle efface le badge IA du champ ---

  onEffectifChange(value: number | string | null): void {
    if (value === null || value === '' || value === undefined) {
      this.effectifEntreprise.set(null);
    } else {
      const num = typeof value === 'number' ? value : Number.parseInt(String(value), 10);
      this.effectifEntreprise.set(Number.isFinite(num) && num >= 0 ? num : null);
    }
    this.provenanceEffectif.set(null);
  }

  onCspProposeChange(value: boolean): void {
    this.cspPropose.set(value);
    this.provenanceCspPropose.set(null);
    if (!value) {
      // Si pas de proposition, les autres champs perdent leur sens.
      this.documentInformationRemis.set(false);
      this.delaiReflexionMentionne.set(false);
    }
  }

  onDocumentRemisChange(value: boolean): void {
    this.documentInformationRemis.set(value);
    this.provenanceDocumentRemis.set(null);
  }

  onDelaiReflexionChange(value: boolean): void {
    this.delaiReflexionMentionne.set(value);
  }

  onDateRemiseChange(value: string | null): void {
    this.dateRemise.set(value || null);
    this.provenanceDateRemise.set(null);
  }

  onDateEntretienChange(value: string | null): void {
    this.dateEntretienPrealable.set(value || null);
  }

  onAdhesionChange(value: 'OUI' | 'NON' | 'INCONNU'): void {
    this.adhesionSalarie.set(value);
    this.provenanceAdhesion.set(null);
  }

  onSalaireChange(value: number | string | null): void {
    if (value === null || value === '' || value === undefined) {
      this.salaireMensuelBrutEuros.set(null);
    } else {
      const num = typeof value === 'number' ? value : Number.parseFloat(String(value));
      this.salaireMensuelBrutEuros.set(Number.isFinite(num) && num > 0 ? num : null);
    }
    this.provenanceSalaire.set(null);
  }

  onRemuneration12moisChange(value: number | string | null): void {
    if (value === null || value === '' || value === undefined) {
      this.remunerationBrute12MoisEuros.set(null);
    } else {
      const num = typeof value === 'number' ? value : Number.parseFloat(String(value));
      this.remunerationBrute12MoisEuros.set(Number.isFinite(num) && num >= 0 ? num : null);
    }
  }

  calculate(): void {
    if (!this.formValid()) return;
    const adh = this.adhesionSalarie();
    const adhesion: boolean | null =
        adh === 'OUI' ? true : adh === 'NON' ? false : null;
    const request: CspCrpConformiteRequest = {
      effectifEntreprise: this.effectifEntreprise()!,
      cspPropose: this.cspPropose(),
      documentInformationRemis: this.documentInformationRemis(),
      delaiReflexionMentionne: this.delaiReflexionMentionne(),
      dateRemise: this.dateRemise(),
      dateEntretienPrealable: this.dateEntretienPrealable(),
      adhesionSalarie: adhesion,
      salaireMensuelBrutEuros: this.salaireMensuelBrutEuros()!,
      remunerationBrute12MoisEuros: this.remunerationBrute12MoisEuros()!,
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.applyResult(r);
        this.calculating.set(false);
        this.snackBar.open('Analyse de conformité CSP calculée', 'OK', { duration: 2500 });
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

  private applyResult(r: CspCrpConformiteResponse): void {
    this.result.set(r);
    this.hydrateForm(r);
    this.showForm.set(false);
  }

  /**
   * Ré-injecte le snapshot d'inputs de la réponse dans les champs du
   * formulaire — sans quoi un clic « Modifier » repartirait des valeurs par
   * défaut au lieu des dernières valeurs saisies.
   */
  private hydrateForm(r: CspCrpConformiteResponse): void {
    this.effectifEntreprise.set(r.effectifEntreprise ?? null);
    this.cspPropose.set(!!r.cspPropose);
    this.documentInformationRemis.set(!!r.documentInformationRemis);
    this.delaiReflexionMentionne.set(!!r.delaiReflexionMentionne);
    this.dateRemise.set(r.dateRemise ?? null);
    this.dateEntretienPrealable.set(r.dateEntretienPrealable ?? null);
    if (r.adhesionSalarie === true) this.adhesionSalarie.set('OUI');
    else if (r.adhesionSalarie === false) this.adhesionSalarie.set('NON');
    else this.adhesionSalarie.set('INCONNU');
    this.salaireMensuelBrutEuros.set(r.salaireMensuelBrutEuros ?? null);
    this.remunerationBrute12MoisEuros.set(r.remunerationBrute12MoisEuros ?? null);
    // Valeurs persistées = saisie avocat — jamais de badge IA.
    this.provenanceEffectif.set(null);
    this.provenanceCspPropose.set(null);
    this.provenanceDocumentRemis.set(null);
    this.provenanceDateRemise.set(null);
    this.provenanceAdhesion.set(null);
    this.provenanceSalaire.set(null);
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
  verdictBannerClass(v: CspCrpConformiteCsp): string {
    switch (v) {
      case 'CONFORME':
        return 'csp-verdict-banner csp-verdict-banner--conforme';
      case 'PARTIELLEMENT_CONFORME':
        return 'csp-verdict-banner csp-verdict-banner--partielle';
      case 'NON_CONFORME':
        return 'csp-verdict-banner csp-verdict-banner--non-conforme';
    }
  }

  verdictBannerLabel(v: CspCrpConformiteCsp): string {
    switch (v) {
      case 'CONFORME': return 'Proposition CSP conforme';
      case 'PARTIELLEMENT_CONFORME': return 'Proposition CSP partiellement conforme';
      case 'NON_CONFORME': return 'Proposition CSP non conforme';
    }
  }

  verdictBannerIcon(v: CspCrpConformiteCsp): string {
    switch (v) {
      case 'CONFORME': return 'check_circle';
      case 'PARTIELLEMENT_CONFORME': return 'warning';
      case 'NON_CONFORME': return 'error';
    }
  }

  /** Format euros FR (Locale.FRANCE-like). */
  formatEuros(amount: number | null | undefined): string {
    if (amount === null || amount === undefined || !Number.isFinite(amount)) return '—';
    return new Intl.NumberFormat('fr-FR').format(Math.round(amount)) + ' €';
  }

  /** Libellé court d'un point (utilisé pour le tracking dans la liste). */
  pointTrackKey(_index: number, p: CspCrpPointNonConformite): string {
    return `${p.code}:${p.libelle}`;
  }
}
