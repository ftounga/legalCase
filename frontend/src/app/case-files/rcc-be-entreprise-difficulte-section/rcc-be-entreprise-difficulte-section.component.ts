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
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatSelectModule } from '@angular/material/select';
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
  RccBeEntrepriseDifficulteRequest,
  RccBeEntrepriseDifficulteResponse,
  RccBeEntrepriseDifficulteType,
  RccBeEntrepriseDifficulteVerdict,
} from '../../core/models/rcc-be-entreprise-difficulte.model';
import {
  RccBeEntrepriseDifficulteService,
} from '../../core/services/rcc-be-entreprise-difficulte.service';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import {
  RccBeEntrepriseDifficulteSectionPrefillRules,
} from './rcc-be-entreprise-difficulte-section-prefill-rules';
import {
  ToolJurisprudenceCitationsComponent,
} from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-219-03b : outil décisionnel « RCC BE — entreprise en difficulté /
 * restructuration » (F-219).
 *
 * <p>BE uniquement — <b>Loi du 26/12/2013</b> (pacte de solidarité entre
 * les générations) + <b>CCT n° 17 du 19/12/1974</b> (CNT — RCC) +
 * <b>AR du 03/05/2007</b> (régime RCC général) + <b>AR de reconnaissance
 * ministérielle</b> de l'entreprise en difficulté ou en restructuration +
 * CCT sectorielle ad hoc. Aucun équivalent strict en droit français.</p>
 *
 * <p>Affiché par le panel F-IA-04 (tool_id
 * {@code rcc-be-entreprise-difficulte}, visibility ALWAYS_ON priority 121
 * BE / DROIT_DU_TRAVAIL — juste au-dessus de
 * {@code rcc-be-longue-carriere} (120). Pattern miroir des autres outils
 * F-213 / F-219 vagues 1b-10b + SF-219-01b/02b.</p>
 *
 * <p><b>Verdict 6 états</b> (hiérarchie de priorité backend : démission
 * &gt; reconnaissance &gt; âge &gt; carrière &gt; ancienneté &gt;
 * éligible) :
 * <ul>
 *   <li>{@code ELIGIBLE_RCC_ENTREPRISE_DIFFICULTE} (vert) — toutes
 *       conditions cumulatives remplies, RCC entreprise difficulté
 *       ouvert. Indemnité complémentaire mensuelle indicative (0,5 ×
 *       diff. rém. nette / allocation chômage) si éligible et couple
 *       financier fourni.</li>
 *   <li>{@code INELIGIBLE_DEMISSION} (rouge) — démission : le RCC est
 *       réservé au licenciement par l'employeur (CCT n° 17 art. 3).</li>
 *   <li>{@code INELIGIBLE_RECONNAISSANCE_ABSENTE} (rouge) — entreprise
 *       non reconnue en difficulté ou en restructuration : pas
 *       d'ouverture du régime dérogatoire.</li>
 *   <li>{@code INELIGIBLE_AGE_INSUFFISANT} (rouge) — âge à la fin du
 *       contrat &lt; âge réduit fixé par le plan agréé.</li>
 *   <li>{@code INELIGIBLE_CARRIERE_INSUFFISANTE} (rouge) — carrière
 *       professionnelle (jours équivalents temps plein) &lt; 10 ans.</li>
 *   <li>{@code INELIGIBLE_ANCIENNETE_INSUFFISANTE} (rouge) — ancienneté
 *       sectorielle / entreprise reconnue &lt; 5 ans.</li>
 * </ul></p>
 *
 * <p><b>Pré-fill IA V1</b> : aucun champ — alignement pattern uniforme
 * F-213/F-219 vagues 7b-10b/SF-219-01b/SF-219-02b ; l'extraction IA
 * Travail BE actuelle ne couvre pas la reconnaissance ministérielle
 * (acte administratif externe), n'extrait pas la carrière ETP / ONSS,
 * et fournit du brut salarial (pas du net de référence RCC).</p>
 *
 * <p>Distinct de :
 * <ul>
 *   <li>{@code rcc-be-conditions} (SF-207-06b) — analyseur 4 régimes
 *       parallèles (général, métiers lourds, longue carrière,
 *       entreprise en difficulté — vue d'orientation).</li>
 *   <li>{@code rcc-be-indemnite-complementaire} (SF-207-07b) —
 *       calculateur d'indemnité complémentaire mensuelle générique.</li>
 *   <li>{@code rcc-be-metiers-lourds} (SF-219-01b) — analyseur dédié
 *       métiers lourds (58+/35 + métier lourd).</li>
 *   <li>{@code rcc-be-longue-carriere} (SF-219-02b) — analyseur dédié
 *       longue carrière (59+/40).</li>
 *   <li>{@code rcc-be-entreprise-difficulte} (cette SF) — analyseur
 *       dédié régime entreprise reconnue en difficulté ou en
 *       restructuration (âge réduit fixé par le plan ministériel +
 *       carrière 10 ans + ancienneté secteur 5 ans).</li>
 * </ul></p>
 */
@Component({
  selector: 'app-rcc-be-entreprise-difficulte-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatCheckboxModule, MatSelectModule,
    MatProgressSpinnerModule,
    ToolJurisprudenceCitationsComponent,
  ],
  templateUrl: './rcc-be-entreprise-difficulte-section.component.html',
  styleUrl: './rcc-be-entreprise-difficulte-section.component.scss',
})
export class RccBeEntrepriseDifficulteSectionComponent
  implements OnInit, OnChanges {

  // F-JU-03 — citations jurisprudentielles F-JU-01 (BE).
  protected readonly toolIdForJurisprudence = 'rcc-be-entreprise-difficulte';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'RCC BE — ENTREPRISE EN DIFFICULTÉ';
  static readonly TOOL_ICON = 'business';

  /** F-177 SF-177-12 / F-236 SF-236-02 — délègue au helper partagé (parité runtime). */
  static getPrefillCount(input: PrefillCountInput): number {
    return RccBeEntrepriseDifficulteSectionPrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() forceExpanded = false;
  /** Gate strict BE — pas d'équivalent FR (régime RCC entreprise difficulté BE-only). */
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
  result = signal<RccBeEntrepriseDifficulteResponse | null>(null);

  // ── Champs du form (signals — édités via handlers). ────────────────────
  typeReconnaissance = signal<RccBeEntrepriseDifficulteType | null>(null);
  ageReduitPlan = signal<number | null>(55);
  ageFinContrat = signal<number | null>(null);
  anneesCarriereTotale = signal<number | null>(null);
  anneesAncienneteSecteur = signal<number | null>(null);
  dateFinContrat = signal<string | null>(null);
  licenciementEffectif = signal<boolean>(true);
  remunerationNetteMensuelleReference = signal<number | null>(null);
  allocationChomageMensuelleEstimee = signal<number | null>(null);

  /** Gate strict workspace BE. */
  isAvailable = computed(() => this.workspaceCountry === 'BELGIQUE');

  constructor(
    private service: RccBeEntrepriseDifficulteService,
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
    // SF-219-03b V1 : pas de pré-fill IA (cf. helper doc) — pas de réaction aiData.
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  formValid(): boolean {
    if (this.typeReconnaissance() === null) return false;
    if (this.ageReduitPlan() === null || this.ageReduitPlan()! < 0) return false;
    if (this.ageFinContrat() === null || this.ageFinContrat()! < 0) return false;
    if (this.anneesCarriereTotale() === null || this.anneesCarriereTotale()! < 0) return false;
    if (this.anneesAncienneteSecteur() === null || this.anneesAncienneteSecteur()! < 0) return false;
    if (!this.dateFinContrat()) return false;
    if (this.licenciementEffectif() === null) return false;
    // Couple financier : si l'un fourni, l'autre obligatoire et inversement ;
    // sinon on autorise les deux à null (calcul d'éligibilité seul, sans indemnité).
    const r = this.remunerationNetteMensuelleReference();
    const a = this.allocationChomageMensuelleEstimee();
    if ((r === null) !== (a === null)) return false;
    if (r !== null && r < 0) return false;
    if (a !== null && a < 0) return false;
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // -- Handlers --
  onTypeReconnaissanceChange(value: RccBeEntrepriseDifficulteType): void {
    this.typeReconnaissance.set(value);
  }
  onAgeReduitPlanChange(value: number | null): void {
    this.ageReduitPlan.set(value);
  }
  onAgeFinContratChange(value: number | null): void {
    this.ageFinContrat.set(value);
  }
  onAnneesCarriereTotaleChange(value: number | null): void {
    this.anneesCarriereTotale.set(value);
  }
  onAnneesAncienneteSecteurChange(value: number | null): void {
    this.anneesAncienneteSecteur.set(value);
  }
  onDateFinContratChange(value: string | null): void {
    this.dateFinContrat.set(value || null);
  }
  onLicenciementEffectifChange(value: boolean): void {
    this.licenciementEffectif.set(value);
  }
  onRemunerationNetteMensuelleReferenceChange(value: number | null): void {
    this.remunerationNetteMensuelleReference.set(value);
  }
  onAllocationChomageMensuelleEstimeeChange(value: number | null): void {
    this.allocationChomageMensuelleEstimee.set(value);
  }

  // -- Libellés humains --
  typeReconnaissanceLabel(value: RccBeEntrepriseDifficulteType | null): string {
    switch (value) {
      case 'EN_DIFFICULTE':
        return 'Entreprise reconnue en difficulté';
      case 'EN_RESTRUCTURATION':
        return 'Entreprise reconnue en restructuration';
      case 'NON_RECONNUE':
        return 'Aucune reconnaissance ministérielle';
      default: return '—';
    }
  }

  /** Verdict en français lisible. */
  verdictLabel(): string {
    const r = this.result();
    if (!r) return '—';
    switch (r.verdict) {
      case 'ELIGIBLE_RCC_ENTREPRISE_DIFFICULTE':
        return 'Éligible — RCC entreprise difficulté ouvert';
      case 'INELIGIBLE_DEMISSION':
        return 'Inéligible — démission';
      case 'INELIGIBLE_RECONNAISSANCE_ABSENTE':
        return 'Inéligible — entreprise non reconnue';
      case 'INELIGIBLE_AGE_INSUFFISANT':
        return 'Inéligible — âge < seuil du plan';
      case 'INELIGIBLE_CARRIERE_INSUFFISANTE':
        return 'Inéligible — carrière < 10 ans';
      case 'INELIGIBLE_ANCIENNETE_INSUFFISANTE':
        return 'Inéligible — ancienneté secteur < 5 ans';
      default:
        return '—';
    }
  }

  /** Classe CSS pour la bannière verdict (vert si éligible, rouge sinon). */
  verdictClass(): string {
    const r = this.result();
    if (!r) return '';
    return r.verdict === 'ELIGIBLE_RCC_ENTREPRISE_DIFFICULTE'
      ? 'verdict-ok'
      : 'verdict-critical';
  }

  /** Classe CSS du badge header (vert si éligible, rouge sinon). */
  verdictBadgeClass(): string {
    const r = this.result();
    if (!r) return '';
    return r.verdict === 'ELIGIBLE_RCC_ENTREPRISE_DIFFICULTE'
      ? 'badge-ok'
      : 'badge-critical';
  }

  /** Label court (header). */
  verdictBadgeLabel(): string {
    const r = this.result();
    if (!r) return '';
    return r.verdict === 'ELIGIBLE_RCC_ENTREPRISE_DIFFICULTE'
      ? 'ÉLIGIBLE'
      : 'INÉLIGIBLE';
  }

  /** Icône Material correspondant au verdict. */
  verdictIcon(): string {
    const r = this.result();
    if (!r) return 'business';
    return r.verdict === 'ELIGIBLE_RCC_ENTREPRISE_DIFFICULTE'
      ? 'verified'
      : 'gpp_bad';
  }

  /** Helper template : icône d'une condition (check vert / cross rouge). */
  conditionIcon(value: boolean): string {
    return value ? 'check_circle' : 'cancel';
  }

  /** Helper template : classe d'une condition (vert / rouge). */
  conditionClass(value: boolean): string {
    return value ? 'condition-ok' : 'condition-ko';
  }

  /** Verdict typed accessor pour le template. */
  verdictOf(r: RccBeEntrepriseDifficulteResponse): RccBeEntrepriseDifficulteVerdict {
    return r.verdict;
  }

  calculate(): void {
    if (!this.formValid()) return;
    if (this.workspaceCountry !== 'BELGIQUE') {
      this.snackBar.open('Outil indisponible pour ce workspace', 'Fermer',
        { duration: 4000, panelClass: 'snack-error' });
      return;
    }
    const request: RccBeEntrepriseDifficulteRequest = {
      typeReconnaissance: this.typeReconnaissance()!,
      ageReduitPlan: this.ageReduitPlan()!,
      ageFinContrat: this.ageFinContrat()!,
      anneesCarriereTotale: this.anneesCarriereTotale()!,
      anneesAncienneteSecteur: this.anneesAncienneteSecteur()!,
      dateFinContrat: this.dateFinContrat()!,
      licenciementEffectif: this.licenciementEffectif(),
      remunerationNetteMensuelleReference: this.remunerationNetteMensuelleReference(),
      allocationChomageMensuelleEstimee: this.allocationChomageMensuelleEstimee(),
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('RCC entreprise difficulté analysé', 'OK', { duration: 2500 });
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
        this.typeReconnaissance.set(r.typeReconnaissance);
        this.ageReduitPlan.set(r.ageReduitPlan);
        this.ageFinContrat.set(r.ageFinContrat);
        this.anneesCarriereTotale.set(r.anneesCarriereTotale);
        this.anneesAncienneteSecteur.set(r.anneesAncienneteSecteur);
        this.dateFinContrat.set(r.dateFinContrat);
        this.licenciementEffectif.set(r.licenciementEffectif);
        this.remunerationNetteMensuelleReference.set(r.remunerationNetteMensuelleReference);
        this.allocationChomageMensuelleEstimee.set(r.allocationChomageMensuelleEstimee);
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
