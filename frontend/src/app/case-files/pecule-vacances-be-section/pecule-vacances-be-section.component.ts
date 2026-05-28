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
  PeculeVacancesBeRequest,
  PeculeVacancesBeResponse,
  PeculeVacancesBeStatut,
  PeculeVacancesBeTypeCalcul,
  PeculeVacancesBeVerdict,
} from '../../core/models/pecule-vacances-be.model';
import {
  PeculeVacancesBeService,
} from '../../core/services/pecule-vacances-be.service';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import {
  PeculeVacancesBeSectionPrefillRules,
} from './pecule-vacances-be-section-prefill-rules';
import {
  ToolJurisprudenceCitationsComponent,
} from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-219-20b : outil décisionnel « pécule de vacances BE » (F-219).
 *
 * <p>BE uniquement — <b>Lois coordonnées du 28/06/1971</b> relatives
 * aux vacances annuelles des travailleurs salariés (M.B. 30/09/1971),
 * art. 3 (durée minimale 4 semaines à temps plein), art. 6 (double
 * pécule), art. 7 (pécule de sortie) ; <b>AR du 30/03/1967</b>
 * déterminant les modalités générales d'exécution (M.B. 06/04/1967),
 * art. 9 (vacances jeunes), art. 19 (pécule ouvrier 15,38 % de la
 * rémunération brute annuelle 108 % — liquidé par l'ONVA / Caisse de
 * vacances sectorielle), art. 38 (pécule employé — rémunération
 * maintenue + 85 % du salaire mensuel comme double pécule), art. 46
 * (pécule de départ employé — 15,34 %) ; <b>Loi du 03/07/1978</b>
 * art. 15 (prescription : 1 an post-contrat, 5 ans depuis le fait
 * générateur).</p>
 *
 * <p>Affiché par le panel F-IA-04 (tool_id {@code pecule-vacances-be},
 * visibility ALWAYS_ON priority 138 BE / DROIT_DU_TRAVAIL — au-dessus
 * de {@code droit-deconnexion-be} (137, SF-219-19b parallèle) et
 * {@code semaine-4-jours-be} (136, SF-219-18b).</p>
 *
 * <h2>Verdict hiérarchisé 8 états (court-circuits backend)</h2>
 * <ul>
 *   <li>{@code A_ANALYSER} (gris info) — statut indéterminé.</li>
 *   <li>{@code NON_DU_DEJA_PAYE} (gris info) — pécule déjà liquidé,
 *       action sans objet.</li>
 *   <li>{@code NON_DU_OUVRIER_VIA_ONVA} (gris info) — l'ouvrier doit
 *       s'adresser à l'ONVA / Caisse de vacances et non à l'employeur
 *       (art. 19 AR 30/03/1967).</li>
 *   <li>{@code PRESCRIT} (rouge) — créance prescrite (art. 15 Loi
 *       03/07/1978 — 1 an depuis la fin du contrat).</li>
 *   <li>{@code NON_DU_JOURS_INSUFFISANTS} (gris info) — durée
 *       d'occupation insuffisante (vacances jeunes art. 9 AR
 *       30/03/1967).</li>
 *   <li>{@code DU_PECULE_SIMPLE_CALCULE} (vert) — pécule simple
 *       liquidé, créance due.</li>
 *   <li>{@code DU_DOUBLE_PECULE_CALCULE} (vert) — double pécule
 *       liquidé, créance due.</li>
 *   <li>{@code DU_PECULE_DEPART_CALCULE} (vert) — pécule de sortie
 *       liquidé, créance due par l'employeur.</li>
 * </ul>
 *
 * <p><b>Pré-fill IA V1</b> : aucun champ — alignement pattern
 * uniforme F-213 / F-219 (statut employé/ouvrier/jeune travailleur,
 * type calcul simple/double/départ, montants rémunération exercice de
 * vacances, jours pris, flag pécule déjà payé, dates contentieuses
 * non extractibles du pipeline Travail BE actuel — déclaratifs RH
 * employeur ou avocat).</p>
 */
@Component({
  selector: 'app-pecule-vacances-be-section',
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
  templateUrl: './pecule-vacances-be-section.component.html',
  styleUrl: './pecule-vacances-be-section.component.scss',
})
export class PeculeVacancesBeSectionComponent
  implements OnInit, OnChanges {

  // F-JU-03 — citations jurisprudentielles F-JU-01 (BE).
  protected readonly toolIdForJurisprudence = 'pecule-vacances-be';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'PÉCULE DE VACANCES (BE)';
  static readonly TOOL_ICON = 'beach_access';

  /** F-177 SF-177-12 / F-236 SF-236-02 — délègue au helper partagé (parité runtime). */
  static getPrefillCount(input: PrefillCountInput): number {
    return PeculeVacancesBeSectionPrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() forceExpanded = false;
  /** Gate strict BE — pas d'équivalent FR mécanique (ICCP française = régime distinct). */
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
  result = signal<PeculeVacancesBeResponse | null>(null);

  // ── Champs du form (signals — édités via handlers). ────────────────────
  statut = signal<PeculeVacancesBeStatut | null>(null);
  typeCalcul = signal<PeculeVacancesBeTypeCalcul | null>(null);
  remunerationBruteAnnuelleExerciceEur = signal<number | null>(null);
  remunerationMensuelleBruteEur = signal<number | null>(null);
  joursCongesPris = signal<number | null>(null);
  peculeDejaPaye = signal<boolean | null>(null);
  remunerationBruteAnnuelleExercicePrecedentEur = signal<number | null>(null);
  dateSortie = signal<string | null>(null);
  dateFinContrat = signal<string | null>(null);
  dateReclamation = signal<string | null>(null);

  /** Gate strict workspace BE. */
  isAvailable = computed(() => this.workspaceCountry === 'BELGIQUE');

  /** dateSortie pertinente uniquement si typeCalcul === PECULE_DEPART. */
  isDepartScenario = computed(
    () => this.typeCalcul() === 'PECULE_DEPART');

  constructor(
    private service: PeculeVacancesBeService,
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
    // SF-219-20b V1 : pas de pré-fill IA (cf. helper doc) — pas de réaction aiData.
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  formValid(): boolean {
    if (this.statut() === null) return false;
    if (this.typeCalcul() === null) return false;
    if (this.remunerationBruteAnnuelleExerciceEur() === null) return false;
    const remExe = this.remunerationBruteAnnuelleExerciceEur() as number;
    if (remExe < 0 || remExe > 10000000) return false;
    if (this.remunerationMensuelleBruteEur() === null) return false;
    const remMen = this.remunerationMensuelleBruteEur() as number;
    if (remMen < 0 || remMen > 1000000) return false;
    if (this.joursCongesPris() === null) return false;
    const jours = this.joursCongesPris() as number;
    if (jours < 0 || jours > 30) return false;
    if (this.peculeDejaPaye() === null) return false;
    if (this.remunerationBruteAnnuelleExercicePrecedentEur() === null) return false;
    const remPrec = this.remunerationBruteAnnuelleExercicePrecedentEur() as number;
    if (remPrec < 0 || remPrec > 10000000) return false;
    if (!this.dateReclamation()) return false;
    // dateSortie requise si PECULE_DEPART, sinon facultative.
    if (this.isDepartScenario() && !this.dateSortie()) return false;
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // -- Handlers --
  onStatutChange(value: PeculeVacancesBeStatut): void {
    this.statut.set(value);
  }
  onTypeCalculChange(value: PeculeVacancesBeTypeCalcul): void {
    this.typeCalcul.set(value);
  }
  onRemunerationBruteAnnuelleExerciceEurChange(value: string | number | null): void {
    this.remunerationBruteAnnuelleExerciceEur.set(this.toNumberOrNull(value));
  }
  onRemunerationMensuelleBruteEurChange(value: string | number | null): void {
    this.remunerationMensuelleBruteEur.set(this.toNumberOrNull(value));
  }
  onJoursCongesPrisChange(value: string | number | null): void {
    this.joursCongesPris.set(this.toNumberOrNull(value));
  }
  onPeculeDejaPayeChange(value: boolean): void {
    this.peculeDejaPaye.set(value);
  }
  onRemunerationBruteAnnuelleExercicePrecedentEurChange(
      value: string | number | null): void {
    this.remunerationBruteAnnuelleExercicePrecedentEur.set(
        this.toNumberOrNull(value));
  }
  onDateSortieChange(value: string | null): void {
    this.dateSortie.set(value);
  }
  onDateFinContratChange(value: string | null): void {
    this.dateFinContrat.set(value);
  }
  onDateReclamationChange(value: string | null): void {
    this.dateReclamation.set(value);
  }

  private toNumberOrNull(value: string | number | null): number | null {
    if (value === null || value === '') return null;
    const n = typeof value === 'number' ? value : Number(value);
    return Number.isFinite(n) ? n : null;
  }

  // -- Libellés humains --
  statutLabel(value: PeculeVacancesBeStatut | null): string {
    switch (value) {
      case 'EMPLOYE':
        return 'Employé(e) — pécule payé par l\'employeur';
      case 'OUVRIER':
        return 'Ouvrier(ère) — pécule payé par l\'ONVA / Caisse de vacances';
      case 'JEUNE_TRAVAILLEUR':
        return 'Jeune travailleur 1re année (art. 9 AR 30/03/1967)';
      case 'INDETERMINE':
        return 'Statut indéterminé (analyse cas par cas)';
      default:
        return '—';
    }
  }

  typeCalculLabel(value: PeculeVacancesBeTypeCalcul | null): string {
    switch (value) {
      case 'PECULE_SIMPLE':
        return 'Pécule simple — rémunération maintenue';
      case 'DOUBLE_PECULE':
        return 'Double pécule — supplément vacances principales';
      case 'PECULE_DEPART':
        return 'Pécule de départ — sortie de l\'employé(e)';
      default:
        return '—';
    }
  }

  /** Verdict en français lisible. */
  verdictLabel(): string {
    const r = this.result();
    if (!r) return '—';
    switch (r.verdict) {
      case 'DU_PECULE_SIMPLE_CALCULE':
        return 'Pécule simple liquidé — créance due par l\'employeur (art. 38 AR 30/03/1967 — rémunération maintenue pendant les jours de congé)';
      case 'DU_DOUBLE_PECULE_CALCULE':
        return 'Double pécule liquidé — créance due au moment des vacances principales (art. 6 Lois 28/06/1971 + art. 38 AR 30/03/1967 — 85 % du salaire mensuel brut)';
      case 'DU_PECULE_DEPART_CALCULE':
        return 'Pécule de sortie liquidé — créance due par l\'employeur à la sortie (art. 7 Lois 28/06/1971 + art. 46 AR 30/03/1967 — 15,34 %)';
      case 'NON_DU_OUVRIER_VIA_ONVA':
        return 'Pécule non dû par l\'employeur — l\'ouvrier doit s\'adresser à l\'ONVA / Caisse de vacances sectorielle (art. 19 AR 30/03/1967)';
      case 'NON_DU_DEJA_PAYE':
        return 'Pécule déjà liquidé — action en paiement sans objet (contrôle du montant versé recommandé)';
      case 'NON_DU_JOURS_INSUFFISANTS':
        return 'Durée d\'occupation insuffisante — droit non ouvert (vacances jeunes — art. 9 AR 30/03/1967)';
      case 'PRESCRIT':
        return 'Créance prescrite — action irrecevable (art. 15 Loi 03/07/1978 — 1 an depuis la fin du contrat, 5 ans depuis le fait générateur)';
      case 'A_ANALYSER':
        return 'Configuration ambigüe — analyse cas par cas requise';
      default:
        return '—';
    }
  }

  /** Classe CSS verdict (vert / rouge / gris info). */
  verdictClass(): string {
    const r = this.result();
    if (!r) return '';
    switch (r.verdict) {
      case 'DU_PECULE_SIMPLE_CALCULE':
      case 'DU_DOUBLE_PECULE_CALCULE':
      case 'DU_PECULE_DEPART_CALCULE':
        return 'verdict-ok';
      case 'PRESCRIT':
        return 'verdict-critical';
      case 'NON_DU_OUVRIER_VIA_ONVA':
      case 'NON_DU_DEJA_PAYE':
      case 'NON_DU_JOURS_INSUFFISANTS':
      case 'A_ANALYSER':
        return 'verdict-neutral';
      default:
        return '';
    }
  }

  /** Icône Material du verdict. */
  verdictIcon(): string {
    const r = this.result();
    if (!r) return 'beach_access';
    switch (r.verdict) {
      case 'DU_PECULE_SIMPLE_CALCULE':
        return 'paid';
      case 'DU_DOUBLE_PECULE_CALCULE':
        return 'savings';
      case 'DU_PECULE_DEPART_CALCULE':
        return 'logout';
      case 'NON_DU_OUVRIER_VIA_ONVA':
        return 'account_balance';
      case 'NON_DU_DEJA_PAYE':
        return 'check_circle_outline';
      case 'NON_DU_JOURS_INSUFFISANTS':
        return 'do_not_disturb_on';
      case 'PRESCRIT':
        return 'gavel';
      case 'A_ANALYSER':
        return 'help_outline';
      default:
        return 'beach_access';
    }
  }

  /** Libellé bref pour le badge du header. */
  verdictBadge(verdict: PeculeVacancesBeVerdict): string {
    switch (verdict) {
      case 'DU_PECULE_SIMPLE_CALCULE':
        return 'SIMPLE DÛ';
      case 'DU_DOUBLE_PECULE_CALCULE':
        return 'DOUBLE DÛ';
      case 'DU_PECULE_DEPART_CALCULE':
        return 'DÉPART DÛ';
      case 'NON_DU_OUVRIER_VIA_ONVA':
        return 'VOIR ONVA';
      case 'NON_DU_DEJA_PAYE':
        return 'DÉJÀ PAYÉ';
      case 'NON_DU_JOURS_INSUFFISANTS':
        return 'JOURS INSUFFISANTS';
      case 'PRESCRIT':
        return 'PRESCRIT';
      case 'A_ANALYSER':
        return 'À ANALYSER';
      default:
        return '';
    }
  }

  /** Classe CSS badge du header. */
  verdictBadgeClass(verdict: PeculeVacancesBeVerdict): string {
    switch (verdict) {
      case 'DU_PECULE_SIMPLE_CALCULE':
      case 'DU_DOUBLE_PECULE_CALCULE':
      case 'DU_PECULE_DEPART_CALCULE':
        return 'badge-ok';
      case 'PRESCRIT':
        return 'badge-critical';
      case 'NON_DU_OUVRIER_VIA_ONVA':
      case 'NON_DU_DEJA_PAYE':
      case 'NON_DU_JOURS_INSUFFISANTS':
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

  /** Format pourcentage (fraction 0..1) arrondi 2 décimales (locale FR). */
  formatPourcentage(value: number | null | undefined): string {
    if (value === null || value === undefined) return '—';
    return (value * 100).toLocaleString('fr-FR', {
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
    const request: PeculeVacancesBeRequest = {
      statut: this.statut()!,
      typeCalcul: this.typeCalcul()!,
      remunerationBruteAnnuelleExerciceEur:
          this.remunerationBruteAnnuelleExerciceEur()!,
      remunerationMensuelleBruteEur: this.remunerationMensuelleBruteEur()!,
      joursCongesPris: this.joursCongesPris()!,
      peculeDejaPaye: this.peculeDejaPaye()!,
      remunerationBruteAnnuelleExercicePrecedentEur:
          this.remunerationBruteAnnuelleExercicePrecedentEur()!,
      dateSortie: this.dateSortie(),
      dateFinContrat: this.dateFinContrat(),
      dateReclamation: this.dateReclamation()!,
    };
    this.calculating.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Pécule de vacances calculé', 'OK', { duration: 2500 });
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
        this.statut.set(r.statut);
        this.typeCalcul.set(r.typeCalcul);
        this.remunerationBruteAnnuelleExerciceEur.set(
            r.remunerationBruteAnnuelleExerciceEur);
        this.remunerationMensuelleBruteEur.set(r.remunerationMensuelleBruteEur);
        this.joursCongesPris.set(r.joursCongesPris);
        this.peculeDejaPaye.set(r.peculeDejaPaye);
        this.remunerationBruteAnnuelleExercicePrecedentEur.set(
            r.remunerationBruteAnnuelleExercicePrecedentEur);
        this.dateSortie.set(r.dateSortie);
        this.dateFinContrat.set(r.dateFinContrat);
        this.dateReclamation.set(r.dateReclamation);
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
