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
  EgaliteFemmesHommesBeRequest,
  EgaliteFemmesHommesBeResponse,
  EgaliteFemmesHommesBeStatutRapport,
  EgaliteFemmesHommesBeVerdict,
} from '../../core/models/egalite-femmes-hommes-be.model';
import {
  EgaliteFemmesHommesBeService,
} from '../../core/services/egalite-femmes-hommes-be.service';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import {
  EgaliteFemmesHommesBeSectionPrefillRules,
} from './egalite-femmes-hommes-be-section-prefill-rules';
import {
  ToolJurisprudenceCitationsComponent,
} from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-219-22b : outil décisionnel « égalité salariale femmes / hommes
 * BE » (F-219).
 *
 * <p>BE uniquement — <b>Loi du 22/04/2012</b> visant à lutter contre
 * l'écart salarial entre hommes et femmes (M.B. 28/08/2012), art. 2
 * (obligation rapport biennal d'analyse pour employeurs ≥ 50
 * travailleurs), art. 5 (plan d'action si écart non justifié), art. 12
 * (interdiction discrimination), art. 14 (faculté médiateur) ;
 * <b>AR du 17/08/2013</b> portant exécution de l'art. 2, art. 4
 * (ventilation par niveau de fonction, ancienneté, qualification,
 * régime de travail, composants de rémunération) ; <b>AR du
 * 25/04/2014</b> formulaires standardisés (ann. I ≥ 100 ETP, ann. II
 * 50-99 ETP, dépôt CE + dépôt central EBES auprès de la BNB) ;
 * <b>CCT n° 25 du 15/10/1975</b> du CNT (NAR) sur l'égalité de
 * rémunération H/F ; <b>C. pén. social art. 195/1</b> (sanction
 * niveau 2).</p>
 *
 * <p>Affiché par le panel F-IA-04 (tool_id
 * {@code egalite-femmes-hommes-be}, visibility ALWAYS_ON priority 140
 * BE / DROIT_DU_TRAVAIL — au-dessus de
 * {@code eco-cheques-cheques-repas-be} (139, SF-219-21b parallèle),
 * {@code pecule-vacances-be} (138, SF-219-20b) et
 * {@code droit-deconnexion-be} (137, SF-219-19b).</p>
 *
 * <h2>Verdict hiérarchisé 6 états (court-circuits backend)</h2>
 * <ul>
 *   <li>{@code A_ANALYSER} (gris info) — statut indéterminé ou
 *       exercice en cours.</li>
 *   <li>{@code HORS_CHAMP_SEUIL_NON_ATTEINT} (gris info) — effectif
 *       &lt; 50 travailleurs (art. 2 § 1er).</li>
 *   <li>{@code MANQUEMENT_GRAVE_RAPPORT_NON_DEPOSE} (rouge) — délai
 *       dépassé sans rapport (sanction art. 195/1 C. pén. social).</li>
 *   <li>{@code NON_CONFORME_RAPPORT_INCOMPLET} (rouge) — ventilation
 *       art. 4 AR 17/08/2013 incomplète.</li>
 *   <li>{@code NON_CONFORME_PLAN_ACTION_MANQUANT} (rouge) — écart
 *       constaté sans plan d'action art. 5.</li>
 *   <li>{@code CONFORME_RAPPORT_PLAN_COMPLETS} (vert) — conformité
 *       globale.</li>
 * </ul>
 *
 * <p><b>Pré-fill IA V1</b> : aucun champ — alignement pattern
 * uniforme F-213 / F-219 (effectif moyen ETP, statut du rapport
 * biennal, dates de dépôt, ventilations art. 4, écart salarial
 * constaté, plan d'action, médiateur, plainte IEFH non extractibles
 * du dossier salarié individuel — déclaratifs RH employeur ou avocat
 * post-instruction).</p>
 */
@Component({
  selector: 'app-egalite-femmes-hommes-be-section',
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
  templateUrl: './egalite-femmes-hommes-be-section.component.html',
  styleUrl: './egalite-femmes-hommes-be-section.component.scss',
})
export class EgaliteFemmesHommesBeSectionComponent
  implements OnInit, OnChanges {

  // F-JU-03 — citations jurisprudentielles F-JU-01 (BE).
  protected readonly toolIdForJurisprudence = 'egalite-femmes-hommes-be';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'ÉGALITÉ SALARIALE F/H (BE)';
  static readonly TOOL_ICON = 'balance';

  /** F-177 SF-177-12 / F-236 SF-236-02 — délègue au helper partagé (parité runtime). */
  static getPrefillCount(input: PrefillCountInput): number {
    return EgaliteFemmesHommesBeSectionPrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() forceExpanded = false;
  /** Gate strict BE — pas d'équivalent FR mécanique (Index égalité FR = régime distinct). */
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
  result = signal<EgaliteFemmesHommesBeResponse | null>(null);

  // ── Champs du form (signals — édités via handlers). ────────────────────
  effectifEntreprise = signal<number | null>(null);
  statutRapport = signal<EgaliteFemmesHommesBeStatutRapport | null>(null);
  dateDepotRapport = signal<string | null>(null);
  ventilationNiveauFonctionFournie = signal<boolean | null>(null);
  ventilationAncienneteFournie = signal<boolean | null>(null);
  ventilationQualificationFournie = signal<boolean | null>(null);
  ventilationRegimeTravailFournie = signal<boolean | null>(null);
  ventilationComposantsRemunerationFournie = signal<boolean | null>(null);
  ecartSalarialNonJustifieConstate = signal<boolean | null>(null);
  pourcentageEcartConstate = signal<number | null>(null);
  planActionEtabli = signal<boolean | null>(null);
  mediateurDesigne = signal<boolean | null>(null);
  plainteIefhOuInspectionEnCours = signal<boolean | null>(null);

  /** Gate strict workspace BE. */
  isAvailable = computed(() => this.workspaceCountry === 'BELGIQUE');

  constructor(
    private service: EgaliteFemmesHommesBeService,
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
    // SF-219-22b V1 : pas de pré-fill IA (cf. helper doc) — pas de réaction aiData.
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  formValid(): boolean {
    if (this.effectifEntreprise() === null) return false;
    const eff = this.effectifEntreprise() as number;
    if (eff < 0 || eff > 1000000) return false;
    if (this.statutRapport() === null) return false;
    if (this.ventilationNiveauFonctionFournie() === null) return false;
    if (this.ventilationAncienneteFournie() === null) return false;
    if (this.ventilationQualificationFournie() === null) return false;
    if (this.ventilationRegimeTravailFournie() === null) return false;
    if (this.ventilationComposantsRemunerationFournie() === null) return false;
    if (this.ecartSalarialNonJustifieConstate() === null) return false;
    if (this.planActionEtabli() === null) return false;
    if (this.mediateurDesigne() === null) return false;
    if (this.plainteIefhOuInspectionEnCours() === null) return false;
    const pct = this.pourcentageEcartConstate();
    if (pct !== null && (pct < 0 || pct > 100)) return false;
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // -- Handlers --
  onEffectifEntrepriseChange(value: string | number | null): void {
    this.effectifEntreprise.set(this.toNumberOrNull(value));
  }
  onStatutRapportChange(value: EgaliteFemmesHommesBeStatutRapport): void {
    this.statutRapport.set(value);
  }
  onDateDepotRapportChange(value: string | null): void {
    this.dateDepotRapport.set(value);
  }
  onVentilationNiveauFonctionFournieChange(value: boolean): void {
    this.ventilationNiveauFonctionFournie.set(value);
  }
  onVentilationAncienneteFournieChange(value: boolean): void {
    this.ventilationAncienneteFournie.set(value);
  }
  onVentilationQualificationFournieChange(value: boolean): void {
    this.ventilationQualificationFournie.set(value);
  }
  onVentilationRegimeTravailFournieChange(value: boolean): void {
    this.ventilationRegimeTravailFournie.set(value);
  }
  onVentilationComposantsRemunerationFournieChange(value: boolean): void {
    this.ventilationComposantsRemunerationFournie.set(value);
  }
  onEcartSalarialNonJustifieConstateChange(value: boolean): void {
    this.ecartSalarialNonJustifieConstate.set(value);
  }
  onPourcentageEcartConstateChange(value: string | number | null): void {
    this.pourcentageEcartConstate.set(this.toNumberOrNull(value));
  }
  onPlanActionEtabliChange(value: boolean): void {
    this.planActionEtabli.set(value);
  }
  onMediateurDesigneChange(value: boolean): void {
    this.mediateurDesigne.set(value);
  }
  onPlainteIefhOuInspectionEnCoursChange(value: boolean): void {
    this.plainteIefhOuInspectionEnCours.set(value);
  }

  private toNumberOrNull(value: string | number | null): number | null {
    if (value === null || value === '') return null;
    const n = typeof value === 'number' ? value : Number(value);
    return Number.isFinite(n) ? n : null;
  }

  // -- Libellés humains --
  statutRapportLabel(value: EgaliteFemmesHommesBeStatutRapport | null): string {
    switch (value) {
      case 'DEPOSE_FORMULAIRE_COMPLET':
        return 'Rapport biennal déposé — formulaire complet';
      case 'DEPOSE_FORMULAIRE_INCOMPLET':
        return 'Rapport biennal déposé — formulaire incomplet';
      case 'EN_PREPARATION':
        return 'Exercice biennal en cours — collecte engagée';
      case 'NON_DEPOSE_DELAI_DEPASSE':
        return 'Délai dépassé sans rapport — manquement frontal';
      case 'INDETERMINE':
        return 'Statut indéterminé (analyse cas par cas)';
      default:
        return '—';
    }
  }

  formulaireLabel(value: string | null | undefined): string {
    switch (value) {
      case 'ANNEXE_I_DETAILLE':
        return 'Annexe I — formulaire détaillé (≥ 100 ETP)';
      case 'ANNEXE_II_SIMPLIFIE':
        return 'Annexe II — formulaire simplifié (50-99 ETP)';
      case 'NON_APPLICABLE':
        return 'Non applicable — effectif < 50';
      default:
        return '—';
    }
  }

  /** Verdict en français lisible. */
  verdictLabel(): string {
    const r = this.result();
    if (!r) return '—';
    switch (r.verdict) {
      case 'CONFORME_RAPPORT_PLAN_COMPLETS':
        return 'Conformité globale — rapport biennal complet déposé et, le cas échéant, plan d\'action concret établi (art. 2 + art. 5 Loi 22/04/2012)';
      case 'HORS_CHAMP_SEUIL_NON_ATTEINT':
        return 'Hors champ — effectif < 50 travailleurs (art. 2 § 1er Loi 22/04/2012) : obligation rapport biennal non applicable (interdiction discrimination art. 12 + CCT n° 25 reste applicable)';
      case 'NON_CONFORME_RAPPORT_INCOMPLET':
        return 'Non-conformité matérielle — rapport déposé mais sections de ventilation art. 4 AR 17/08/2013 manquantes (avenant correctif requis)';
      case 'NON_CONFORME_PLAN_ACTION_MANQUANT':
        return 'Non-conformité — rapport complet révèle un écart non justifié mais aucun plan d\'action n\'a été établi dans le délai (art. 5 Loi 22/04/2012)';
      case 'MANQUEMENT_GRAVE_RAPPORT_NON_DEPOSE':
        return 'Manquement frontal — délai de dépôt dépassé sans rapport biennal (sanction niveau 2 C. pén. social art. 195/1 + action en cessation organisations syndicales / IEFH)';
      case 'A_ANALYSER':
        return 'Configuration ambigüe — statut indéterminé ou exercice en cours — analyse cas par cas';
      default:
        return '—';
    }
  }

  /** Classe CSS verdict (vert / rouge / gris info). */
  verdictClass(): string {
    const r = this.result();
    if (!r) return '';
    switch (r.verdict) {
      case 'CONFORME_RAPPORT_PLAN_COMPLETS':
        return 'verdict-ok';
      case 'MANQUEMENT_GRAVE_RAPPORT_NON_DEPOSE':
      case 'NON_CONFORME_RAPPORT_INCOMPLET':
      case 'NON_CONFORME_PLAN_ACTION_MANQUANT':
        return 'verdict-critical';
      case 'HORS_CHAMP_SEUIL_NON_ATTEINT':
      case 'A_ANALYSER':
        return 'verdict-neutral';
      default:
        return '';
    }
  }

  /** Icône Material du verdict. */
  verdictIcon(): string {
    const r = this.result();
    if (!r) return 'balance';
    switch (r.verdict) {
      case 'CONFORME_RAPPORT_PLAN_COMPLETS':
        return 'verified';
      case 'MANQUEMENT_GRAVE_RAPPORT_NON_DEPOSE':
        return 'report';
      case 'NON_CONFORME_RAPPORT_INCOMPLET':
        return 'rule_folder';
      case 'NON_CONFORME_PLAN_ACTION_MANQUANT':
        return 'pending_actions';
      case 'HORS_CHAMP_SEUIL_NON_ATTEINT':
        return 'do_not_disturb_on';
      case 'A_ANALYSER':
        return 'help_outline';
      default:
        return 'balance';
    }
  }

  /** Libellé bref pour le badge du header. */
  verdictBadge(verdict: EgaliteFemmesHommesBeVerdict): string {
    switch (verdict) {
      case 'CONFORME_RAPPORT_PLAN_COMPLETS':
        return 'CONFORME';
      case 'MANQUEMENT_GRAVE_RAPPORT_NON_DEPOSE':
        return 'NON DÉPOSÉ';
      case 'NON_CONFORME_RAPPORT_INCOMPLET':
        return 'INCOMPLET';
      case 'NON_CONFORME_PLAN_ACTION_MANQUANT':
        return 'PLAN MANQUANT';
      case 'HORS_CHAMP_SEUIL_NON_ATTEINT':
        return 'HORS CHAMP';
      case 'A_ANALYSER':
        return 'À ANALYSER';
      default:
        return '';
    }
  }

  /** Classe CSS badge du header. */
  verdictBadgeClass(verdict: EgaliteFemmesHommesBeVerdict): string {
    switch (verdict) {
      case 'CONFORME_RAPPORT_PLAN_COMPLETS':
        return 'badge-ok';
      case 'MANQUEMENT_GRAVE_RAPPORT_NON_DEPOSE':
      case 'NON_CONFORME_RAPPORT_INCOMPLET':
      case 'NON_CONFORME_PLAN_ACTION_MANQUANT':
        return 'badge-critical';
      case 'HORS_CHAMP_SEUIL_NON_ATTEINT':
      case 'A_ANALYSER':
        return 'badge-neutral';
      default:
        return '';
    }
  }

  /** Format pourcentage (valeur 0..100) — 2 décimales, locale FR. */
  formatPourcentage(value: number | null | undefined): string {
    if (value === null || value === undefined) return '—';
    return value.toLocaleString('fr-FR', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }) + ' %';
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
    const request: EgaliteFemmesHommesBeRequest = {
      effectifEntreprise: this.effectifEntreprise()!,
      statutRapport: this.statutRapport()!,
      dateDepotRapport: this.dateDepotRapport(),
      ventilationNiveauFonctionFournie:
          this.ventilationNiveauFonctionFournie()!,
      ventilationAncienneteFournie:
          this.ventilationAncienneteFournie()!,
      ventilationQualificationFournie:
          this.ventilationQualificationFournie()!,
      ventilationRegimeTravailFournie:
          this.ventilationRegimeTravailFournie()!,
      ventilationComposantsRemunerationFournie:
          this.ventilationComposantsRemunerationFournie()!,
      ecartSalarialNonJustifieConstate:
          this.ecartSalarialNonJustifieConstate()!,
      pourcentageEcartConstate: this.pourcentageEcartConstate(),
      planActionEtabli: this.planActionEtabli()!,
      mediateurDesigne: this.mediateurDesigne()!,
      plainteIefhOuInspectionEnCours:
          this.plainteIefhOuInspectionEnCours()!,
    };
    this.calculating.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Analyse égalité H/F calculée', 'OK', { duration: 2500 });
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
        this.effectifEntreprise.set(r.effectifEntreprise);
        this.statutRapport.set(r.statutRapport);
        this.dateDepotRapport.set(r.dateDepotRapport);
        this.ventilationNiveauFonctionFournie.set(
            r.ventilationNiveauFonctionFournie);
        this.ventilationAncienneteFournie.set(r.ventilationAncienneteFournie);
        this.ventilationQualificationFournie.set(
            r.ventilationQualificationFournie);
        this.ventilationRegimeTravailFournie.set(
            r.ventilationRegimeTravailFournie);
        this.ventilationComposantsRemunerationFournie.set(
            r.ventilationComposantsRemunerationFournie);
        this.ecartSalarialNonJustifieConstate.set(
            r.ecartSalarialNonJustifieConstate);
        this.pourcentageEcartConstate.set(r.pourcentageEcartConstate);
        this.planActionEtabli.set(r.planActionEtabli);
        this.mediateurDesigne.set(r.mediateurDesigne);
        this.plainteIefhOuInspectionEnCours.set(
            r.plainteIefhOuInspectionEnCours);
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
