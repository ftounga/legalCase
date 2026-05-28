import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  Input,
  OnChanges,
  OnInit,
  Optional,
  SimpleChanges,
  computed,
  inject,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatListModule } from '@angular/material/list';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import {
  PieceManquanteEntry,
  TravailExtractedData,
} from '../../core/models/case-analysis.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { AiQuestion } from '../../core/models/ai-question.model';
import {
  EcoChequesChequesRepasBeRequest,
  EcoChequesChequesRepasBeResponse,
  EcoChequesChequesRepasBeTypeAvantage,
  EcoChequesChequesRepasBeVerdict,
} from '../../core/models/eco-cheques-cheques-repas-be.model';
import {
  EcoChequesChequesRepasBeService,
} from '../../core/services/eco-cheques-cheques-repas-be.service';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import {
  EcoChequesChequesRepasBeSectionPrefillRules,
} from './eco-cheques-cheques-repas-be-section-prefill-rules';
import {
  ToolJurisprudenceCitationsComponent,
} from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-219-21b : outil décisionnel « éco-chèques + chèques-repas BE »
 * (F-219).
 *
 * <p>BE uniquement — <b>CCT n°98 du CNT du 20/02/2009</b> pour les
 * éco-chèques (M.B. 06/05/2009 — AR 12/04/2009 rendant la CCT
 * obligatoire), art. 3 (CCT sectorielle ou d'entreprise ou convention
 * individuelle écrite), art. 4 (interdiction de substitution à
 * rémunération), art. 6 (plafond 250 EUR/an), art. 13 (validité
 * 24 mois) ; <b>Loi du 25/04/2014</b> portant des dispositions
 * diverses (M.B. 07/05/2014) art. 51 (paiement électronique obligatoire
 * des chèques-repas depuis 01/01/2016) ; <b>AR du 03/02/2010</b>
 * modifiant l'art. 19bis de l'AR du 28/11/1969 portant exécution de
 * la loi du 27/06/1969 sur la sécurité sociale (M.B. 17/02/2010),
 * conditions cumulatives d'exonération ONSS des chèques-repas (un
 * chèque par jour effectivement presté, nominatif, usage repas /
 * denrées alimentaires uniquement, intervention employeur max
 * 6,91 EUR, contribution travailleur min 1,09 EUR, valeur faciale
 * max 8 EUR, validité 12 mois, pas de cumul frais de bouche) ;
 * <b>Cass. 16/10/2017 S.16.0042.F</b> ONSS c/ SA — interdiction de
 * substitution à rémunération préexistante (requalification intégrale
 * rétroactive).</p>
 *
 * <p>Affiché par le panel F-IA-04 (tool_id
 * {@code eco-cheques-cheques-repas-be}, visibility ALWAYS_ON priority
 * 139 BE / DROIT_DU_TRAVAIL — au-dessus de {@code pecule-vacances-be}
 * (138, SF-219-20b) et {@code droit-deconnexion-be} (137, SF-219-19b).</p>
 *
 * <h2>Verdict hiérarchisé 6 états (court-circuits backend)</h2>
 * <ul>
 *   <li>{@code A_ANALYSER} (gris info) — type d'avantage indéterminé.</li>
 *   <li>{@code NON_CONFORME_SUBSTITUTION_REMUNERATION} (rouge) —
 *       substitution à une rémunération préexistante (art. 4 CCT n°98
 *       + Cass. 16/10/2017 S.16.0042.F) → requalification intégrale
 *       rétroactive.</li>
 *   <li>{@code NON_CONFORME_CONDITION_MANQUANTE} (rouge) — condition
 *       cumulative non remplie (CCT/convention absente, paiement non
 *       électronique post 01/01/2016, contribution travailleur
 *       insuffisante, cumul avec frais de bouche, validité dépassée).</li>
 *   <li>{@code NON_CONFORME_DEPASSEMENT_PLAFOND} (ambre) — dépassement
 *       du plafond légal (250 EUR/an éco-chèques ou 6,91 EUR/jour
 *       chèques-repas).</li>
 *   <li>{@code CONFORME_PARTIELLEMENT_EXONERE} (ambre) — montant
 *       supérieur au plafond, fraction excédentaire requalifiée.</li>
 *   <li>{@code CONFORME_EXONERATION_INTEGRALE} (vert) — toutes
 *       conditions remplies, exonération ONSS et impôt intégrale.</li>
 * </ul>
 *
 * <p><b>Pré-fill IA V1</b> : aucun champ — alignement pattern
 * uniforme F-213 / F-219 (type d'avantage, montants, jours prestés,
 * flags juridiques (CCT, convention individuelle, paiement
 * électronique, cumul frais de bouche, substitution rémunération),
 * date d'attribution non extractibles du pipeline Travail BE actuel —
 * déclaratifs RH / fiches de paie / avocat).</p>
 */
@Component({
  selector: 'app-eco-cheques-cheques-repas-be-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatSelectModule,
    MatListModule,
    MatCheckboxModule,
    MatProgressSpinnerModule,
    ToolJurisprudenceCitationsComponent,
  ],
  templateUrl: './eco-cheques-cheques-repas-be-section.component.html',
  styleUrl: './eco-cheques-cheques-repas-be-section.component.scss',
})
export class EcoChequesChequesRepasBeSectionComponent
  implements OnInit, OnChanges {

  // F-JU-03 — citations jurisprudentielles F-JU-01 (BE).
  protected readonly toolIdForJurisprudence = 'eco-cheques-cheques-repas-be';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'ÉCO-CHÈQUES + CHÈQUES-REPAS (BE)';
  static readonly TOOL_ICON = 'redeem';

  /** F-177 SF-177-12 / F-236 SF-236-02 — délègue au helper partagé (parité runtime). */
  static getPrefillCount(input: PrefillCountInput): number {
    return EcoChequesChequesRepasBeSectionPrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() forceExpanded = false;
  /** Gate strict BE — pas d'équivalent FR mécanique (titres-restaurant FR = régime distinct). */
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'BELGIQUE';
  @Input() aiData?: TravailExtractedData | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;

  private cdr = inject(ChangeDetectorRef);

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<EcoChequesChequesRepasBeResponse | null>(null);

  // ── Champs du form (signals — édités via handlers). ────────────────────
  typeAvantage = signal<EcoChequesChequesRepasBeTypeAvantage | null>(null);
  montantAnnuelEur = signal<number | null>(null);
  valeurFacialeUnitaireEur = signal<number | null>(null);
  contributionTravailleurUnitaireEur = signal<number | null>(null);
  joursEffectivementPrestes = signal<number | null>(null);
  cctSectorielleOuEntrepriseExiste = signal<boolean | null>(null);
  conventionIndividuelleEcrite = signal<boolean | null>(null);
  paiementElectronique = signal<boolean | null>(null);
  cumulFraisBouche = signal<boolean | null>(null);
  substitutionRemuneration = signal<boolean | null>(null);
  dateAttribution = signal<string | null>(null);

  /** Gate strict workspace BE. */
  isAvailable = computed(() => this.workspaceCountry === 'BELGIQUE');

  /** Champs spécifiques chèques-repas (paiement électronique, cumul, contribution travailleur). */
  isChequesRepas = computed(
    () => this.typeAvantage() === 'CHEQUES_REPAS');

  constructor(
    private service: EcoChequesChequesRepasBeService,
    private snackBar: MatSnackBar,
    @Optional() private dashboardRefresh: CaseDashboardRefreshService | null,
  ) {}

  ngOnInit(): void {
    if (this.forceExpanded) this.collapsed.set(false);
    if (this.workspaceCountry === 'BELGIQUE') {
      this.load();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['forceExpanded'] && this.forceExpanded) this.collapsed.set(false);
    // SF-219-21b V1 : pas de pré-fill IA (cf. helper doc) — pas de réaction aiData.
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  formValid(): boolean {
    if (this.typeAvantage() === null) return false;
    if (this.montantAnnuelEur() === null) return false;
    const mont = this.montantAnnuelEur() as number;
    if (mont < 0 || mont > 100000) return false;
    if (this.valeurFacialeUnitaireEur() === null) return false;
    const vf = this.valeurFacialeUnitaireEur() as number;
    if (vf < 0 || vf > 1000) return false;
    if (this.contributionTravailleurUnitaireEur() === null) return false;
    const ct = this.contributionTravailleurUnitaireEur() as number;
    if (ct < 0 || ct > 1000) return false;
    if (this.joursEffectivementPrestes() === null) return false;
    const jours = this.joursEffectivementPrestes() as number;
    if (jours < 0) return false;
    if (this.cctSectorielleOuEntrepriseExiste() === null) return false;
    if (this.conventionIndividuelleEcrite() === null) return false;
    if (this.paiementElectronique() === null) return false;
    if (this.cumulFraisBouche() === null) return false;
    if (this.substitutionRemuneration() === null) return false;
    // dateAttribution facultative (sert au contrôle de validité mais non bloquante).
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // -- Handlers --
  onTypeAvantageChange(value: EcoChequesChequesRepasBeTypeAvantage): void {
    this.typeAvantage.set(value);
  }
  onMontantAnnuelEurChange(value: string | number | null): void {
    this.montantAnnuelEur.set(this.toNumberOrNull(value));
  }
  onValeurFacialeUnitaireEurChange(value: string | number | null): void {
    this.valeurFacialeUnitaireEur.set(this.toNumberOrNull(value));
  }
  onContributionTravailleurUnitaireEurChange(value: string | number | null): void {
    this.contributionTravailleurUnitaireEur.set(this.toNumberOrNull(value));
  }
  onJoursEffectivementPrestesChange(value: string | number | null): void {
    this.joursEffectivementPrestes.set(this.toNumberOrNull(value));
  }
  onCctSectorielleOuEntrepriseExisteChange(value: boolean): void {
    this.cctSectorielleOuEntrepriseExiste.set(value);
  }
  onConventionIndividuelleEcriteChange(value: boolean): void {
    this.conventionIndividuelleEcrite.set(value);
  }
  onPaiementElectroniqueChange(value: boolean): void {
    this.paiementElectronique.set(value);
  }
  onCumulFraisBoucheChange(value: boolean): void {
    this.cumulFraisBouche.set(value);
  }
  onSubstitutionRemunerationChange(value: boolean): void {
    this.substitutionRemuneration.set(value);
  }
  onDateAttributionChange(value: string | null): void {
    this.dateAttribution.set(value);
  }

  private toNumberOrNull(value: string | number | null): number | null {
    if (value === null || value === '') return null;
    const n = typeof value === 'number' ? value : Number(value);
    return Number.isFinite(n) ? n : null;
  }

  // -- Libellés humains --
  typeAvantageLabel(value: EcoChequesChequesRepasBeTypeAvantage | null): string {
    switch (value) {
      case 'ECO_CHEQUES':
        return 'Éco-chèques — CCT n°98 du 20/02/2009';
      case 'CHEQUES_REPAS':
        return 'Chèques-repas — Loi 25/04/2014 + AR 03/02/2010';
      case 'INDETERMINE':
        return 'Type indéterminé (analyse cas par cas)';
      default:
        return '—';
    }
  }

  /** Verdict en français lisible. */
  verdictLabel(): string {
    const r = this.result();
    if (!r) return '—';
    switch (r.verdict) {
      case 'CONFORME_EXONERATION_INTEGRALE':
        return 'Avantage extra-légal intégralement conforme — exonération ONSS et impôt (toutes conditions cumulatives remplies)';
      case 'CONFORME_PARTIELLEMENT_EXONERE':
        return 'Avantage partiellement conforme — fraction excédentaire requalifiée en rémunération (montant supérieur au plafond légal)';
      case 'NON_CONFORME_DEPASSEMENT_PLAFOND':
        return 'Dépassement du plafond légal — la fraction excédentaire est requalifiée en rémunération soumise aux cotisations ONSS et à l\'impôt (art. 6 CCT n°98 ou art. 19bis §2 6° AR 28/11/1969)';
      case 'NON_CONFORME_SUBSTITUTION_REMUNERATION':
        return 'Substitution à une rémunération préexistante — requalification intégrale rétroactive 5 ans (art. 4 CCT n°98 + Cass. 16/10/2017 S.16.0042.F + art. 42 al. 1 Loi 27/06/1969)';
      case 'NON_CONFORME_CONDITION_MANQUANTE':
        return 'Condition cumulative manquante — requalification en rémunération (CCT/convention absente, paiement non électronique post 01/01/2016, contribution travailleur insuffisante 1,09 EUR min, cumul avec frais de bouche, validité dépassée)';
      case 'A_ANALYSER':
        return 'Type d\'avantage indéterminé — analyse cas par cas requise';
      default:
        return '—';
    }
  }

  /** Classe CSS verdict (vert / rouge / ambre / gris info). */
  verdictClass(): string {
    const r = this.result();
    if (!r) return '';
    switch (r.verdict) {
      case 'CONFORME_EXONERATION_INTEGRALE':
        return 'verdict-ok';
      case 'NON_CONFORME_SUBSTITUTION_REMUNERATION':
      case 'NON_CONFORME_CONDITION_MANQUANTE':
        return 'verdict-critical';
      case 'NON_CONFORME_DEPASSEMENT_PLAFOND':
      case 'CONFORME_PARTIELLEMENT_EXONERE':
        return 'verdict-warn';
      case 'A_ANALYSER':
        return 'verdict-neutral';
      default:
        return '';
    }
  }

  /** Icône Material du verdict. */
  verdictIcon(): string {
    const r = this.result();
    if (!r) return 'redeem';
    switch (r.verdict) {
      case 'CONFORME_EXONERATION_INTEGRALE':
        return 'verified';
      case 'CONFORME_PARTIELLEMENT_EXONERE':
        return 'rule';
      case 'NON_CONFORME_DEPASSEMENT_PLAFOND':
        return 'price_change';
      case 'NON_CONFORME_SUBSTITUTION_REMUNERATION':
        return 'swap_horiz';
      case 'NON_CONFORME_CONDITION_MANQUANTE':
        return 'report_problem';
      case 'A_ANALYSER':
        return 'help_outline';
      default:
        return 'redeem';
    }
  }

  /** Libellé bref pour le badge du header. */
  verdictBadge(verdict: EcoChequesChequesRepasBeVerdict): string {
    switch (verdict) {
      case 'CONFORME_EXONERATION_INTEGRALE':
        return 'CONFORME';
      case 'CONFORME_PARTIELLEMENT_EXONERE':
        return 'PARTIEL';
      case 'NON_CONFORME_DEPASSEMENT_PLAFOND':
        return 'PLAFOND';
      case 'NON_CONFORME_SUBSTITUTION_REMUNERATION':
        return 'SUBSTITUTION';
      case 'NON_CONFORME_CONDITION_MANQUANTE':
        return 'CONDITION';
      case 'A_ANALYSER':
        return 'À ANALYSER';
      default:
        return '';
    }
  }

  /** Classe CSS badge du header. */
  verdictBadgeClass(verdict: EcoChequesChequesRepasBeVerdict): string {
    switch (verdict) {
      case 'CONFORME_EXONERATION_INTEGRALE':
        return 'badge-ok';
      case 'NON_CONFORME_SUBSTITUTION_REMUNERATION':
      case 'NON_CONFORME_CONDITION_MANQUANTE':
        return 'badge-critical';
      case 'NON_CONFORME_DEPASSEMENT_PLAFOND':
      case 'CONFORME_PARTIELLEMENT_EXONERE':
        return 'badge-warn';
      case 'A_ANALYSER':
        return 'badge-neutral';
      default:
        return '';
    }
  }

  /** Format montant € arrondi 2 décimales (locale FR). */
  formatMontant(value: number | null | undefined): string {
    if (value === null || value === undefined) return '—';
    return value.toLocaleString('fr-FR', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }) + ' €';
  }

  /** Boolean → Oui / Non / —. */
  formatBoolean(value: boolean | null | undefined): string {
    if (value === null || value === undefined) return '—';
    return value ? 'Oui' : 'Non';
  }

  calculate(): void {
    if (!this.formValid()) return;
    if (this.workspaceCountry !== 'BELGIQUE') {
      this.snackBar.open('Outil indisponible pour ce workspace', 'Fermer',
        { duration: 4000, panelClass: 'snack-error' });
      return;
    }
    const request: EcoChequesChequesRepasBeRequest = {
      typeAvantage: this.typeAvantage()!,
      montantAnnuelEur: this.montantAnnuelEur()!,
      valeurFacialeUnitaireEur: this.valeurFacialeUnitaireEur()!,
      contributionTravailleurUnitaireEur:
          this.contributionTravailleurUnitaireEur()!,
      joursEffectivementPrestes: this.joursEffectivementPrestes()!,
      cctSectorielleOuEntrepriseExiste:
          this.cctSectorielleOuEntrepriseExiste()!,
      conventionIndividuelleEcrite: this.conventionIndividuelleEcrite()!,
      paiementElectronique: this.paiementElectronique()!,
      cumulFraisBouche: this.cumulFraisBouche()!,
      substitutionRemuneration: this.substitutionRemuneration()!,
      dateAttribution: this.dateAttribution(),
    };
    this.calculating.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Conformité éco-chèques / chèques-repas calculée', 'OK', { duration: 2500 });
        this.dashboardRefresh?.triggerRefresh();
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.calculating.set(false);
        const status = err?.status;
        let msg: string;
        if (status === 404) {
          msg = 'Dossier introuvable';
        } else if (status === 400) {
          msg = err?.error?.message || err?.error || 'Données saisies invalides';
        } else {
          msg = 'Une erreur est survenue. Veuillez réessayer.';
        }
        this.snackBar.open(String(msg), 'Fermer',
          { duration: 5000, panelClass: 'snack-error' });
        this.cdr.markForCheck();
      },
    });
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.loading.set(false);
        // Rehydratation form pour permettre l'édition.
        this.typeAvantage.set(r.typeAvantage);
        this.montantAnnuelEur.set(r.montantAnnuelEur);
        this.valeurFacialeUnitaireEur.set(r.valeurFacialeUnitaireEur);
        this.contributionTravailleurUnitaireEur.set(
            r.contributionTravailleurUnitaireEur);
        this.joursEffectivementPrestes.set(r.joursEffectivementPrestes);
        this.cctSectorielleOuEntrepriseExiste.set(
            r.cctSectorielleOuEntrepriseExiste);
        this.conventionIndividuelleEcrite.set(r.conventionIndividuelleEcrite);
        this.paiementElectronique.set(r.paiementElectronique);
        this.cumulFraisBouche.set(r.cumulFraisBouche);
        this.substitutionRemuneration.set(r.substitutionRemuneration);
        this.dateAttribution.set(r.dateAttribution);
        this.cdr.markForCheck();
      },
      error: () => {
        // 404 attendu si aucune analyse — mode formulaire vierge.
        this.loading.set(false);
        this.cdr.markForCheck();
      },
    });
  }
}
