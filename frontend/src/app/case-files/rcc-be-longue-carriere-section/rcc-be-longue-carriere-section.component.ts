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
  RccBeLongueCarriereRequest,
  RccBeLongueCarriereResponse,
  RccBeLongueCarriereVerdict,
} from '../../core/models/rcc-be-longue-carriere.model';
import {
  RccBeLongueCarriereService,
} from '../../core/services/rcc-be-longue-carriere.service';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import {
  RccBeLongueCarriereSectionPrefillRules,
} from './rcc-be-longue-carriere-section-prefill-rules';
import {
  ToolJurisprudenceCitationsComponent,
} from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-219-02b : outil décisionnel « RCC BE — longue carrière » (F-219).
 *
 * <p>BE uniquement — <b>Loi du 26/12/2013</b> (pacte de solidarité entre
 * les générations) + <b>CCT n° 17 du 19/12/1974</b> (CNT — RCC) +
 * <b>AR du 03/05/2007 art. 3</b> (régime spécifique longue carrière :
 * conditions cumulatives âge ≥ 59 ans à la fin du contrat + carrière
 * ≥ 40 ans + licenciement effectif). Aucun équivalent strict en droit
 * français.</p>
 *
 * <p>Affiché par le panel F-IA-04 (tool_id {@code rcc-be-longue-carriere},
 * visibility ALWAYS_ON priority 120 BE / DROIT_DU_TRAVAIL — au-dessus de
 * {@code licenciement-be-cct109-deraisonnable} (118). Pattern miroir des
 * autres outils F-213 / F-219 vagues 7b-10b.</p>
 *
 * <p><b>Verdict 4 états</b> (hiérarchie de priorité backend : démission
 * &gt; âge &gt; carrière &gt; éligible) :
 * <ul>
 *   <li>{@code ELIGIBLE_RCC_LONGUE_CARRIERE} (vert) — toutes conditions
 *       cumulatives remplies, RCC longue carrière ouvert. Indemnité
 *       complémentaire mensuelle indicative (0,5 × diff. rém. nette /
 *       allocation chômage) si les 2 montants financiers sont fournis.</li>
 *   <li>{@code INELIGIBLE_DEMISSION} (rouge) — démission : le RCC est
 *       réservé au licenciement par l'employeur (CCT n° 17 art. 3).</li>
 *   <li>{@code INELIGIBLE_AGE_INSUFFISANT} (rouge) — âge à la fin du
 *       contrat &lt; 59 ans.</li>
 *   <li>{@code INELIGIBLE_CARRIERE_INSUFFISANTE} (rouge) — carrière
 *       professionnelle (jours équivalents temps plein) &lt; 40 ans.</li>
 * </ul></p>
 *
 * <p><b>Pré-fill IA V1</b> : aucun champ — alignement pattern uniforme
 * F-213/F-219 vagues 7b-10b ; l'extraction IA Travail BE actuelle ne couvre
 * pas la carrière totale ETP (relevé ONSS / DRS), n'expose pas un âge
 * dérivé et fournit du brut salarial (pas du net de référence RCC).</p>
 *
 * <p>Distinct de :
 * <ul>
 *   <li>{@code rcc-be-conditions} (SF-207-06b) — analyseur 4 régimes
 *       parallèles (général, métiers lourds, longue carrière,
 *       entreprise en difficulté).</li>
 *   <li>{@code rcc-be-indemnite-complementaire} (SF-207-07b) —
 *       calculateur d'indemnité complémentaire mensuelle générique.</li>
 *   <li>{@code rcc-be-longue-carriere} (cette SF) — analyseur dédié au
 *       régime longue carrière (AR 03/05/2007 art. 3 — 59+/40).</li>
 * </ul></p>
 */
@Component({
  selector: 'app-rcc-be-longue-carriere-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatCheckboxModule,
    MatProgressSpinnerModule,
    ToolJurisprudenceCitationsComponent,
  ],
  templateUrl: './rcc-be-longue-carriere-section.component.html',
  styleUrl: './rcc-be-longue-carriere-section.component.scss',
})
export class RccBeLongueCarriereSectionComponent
  implements OnInit, OnChanges {

  // F-JU-03 — citations jurisprudentielles F-JU-01 (BE).
  protected readonly toolIdForJurisprudence = 'rcc-be-longue-carriere';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'RCC BE — LONGUE CARRIÈRE';
  static readonly TOOL_ICON = 'elderly';

  /** F-177 SF-177-12 / F-236 SF-236-02 — délègue au helper partagé (parité runtime). */
  static getPrefillCount(input: PrefillCountInput): number {
    return RccBeLongueCarriereSectionPrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() forceExpanded = false;
  /** Gate strict BE — pas d'équivalent FR (RCC longue carrière = AR BE 03/05/2007). */
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
  result = signal<RccBeLongueCarriereResponse | null>(null);

  // ── Champs du form (signals — édités via handlers). ────────────────────
  ageFinContrat = signal<number | null>(null);
  anneesCarriereTotale = signal<number | null>(null);
  dateFinContrat = signal<string | null>(null);
  licenciementEffectif = signal<boolean>(true);
  remunerationNetteMensuelleReference = signal<number | null>(null);
  allocationChomageMensuelleEstimee = signal<number | null>(null);

  /** Gate strict workspace BE. */
  isAvailable = computed(() => this.workspaceCountry === 'BELGIQUE');

  constructor(
    private service: RccBeLongueCarriereService,
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
    // SF-219-02b V1 : pas de pré-fill IA (cf. helper doc) — pas de réaction aiData.
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  formValid(): boolean {
    if (this.ageFinContrat() === null || this.ageFinContrat()! < 0) return false;
    if (this.anneesCarriereTotale() === null || this.anneesCarriereTotale()! < 0) return false;
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
  onAgeFinContratChange(value: number | null): void {
    this.ageFinContrat.set(value);
  }
  onAnneesCarriereTotaleChange(value: number | null): void {
    this.anneesCarriereTotale.set(value);
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

  /** Verdict en français lisible. */
  verdictLabel(): string {
    const r = this.result();
    if (!r) return '—';
    switch (r.verdict) {
      case 'ELIGIBLE_RCC_LONGUE_CARRIERE':
        return 'Éligible — RCC longue carrière ouvert';
      case 'INELIGIBLE_DEMISSION':
        return 'Inéligible — démission';
      case 'INELIGIBLE_AGE_INSUFFISANT':
        return 'Inéligible — âge < 59 ans';
      case 'INELIGIBLE_CARRIERE_INSUFFISANTE':
        return 'Inéligible — carrière < 40 ans';
      default:
        return '—';
    }
  }

  /** Classe CSS pour la bannière verdict (vert si éligible, rouge sinon). */
  verdictClass(): string {
    const r = this.result();
    if (!r) return '';
    return r.verdict === 'ELIGIBLE_RCC_LONGUE_CARRIERE'
      ? 'verdict-ok'
      : 'verdict-critical';
  }

  /** Classe CSS du badge header (vert si éligible, rouge sinon). */
  verdictBadgeClass(): string {
    const r = this.result();
    if (!r) return '';
    return r.verdict === 'ELIGIBLE_RCC_LONGUE_CARRIERE'
      ? 'badge-ok'
      : 'badge-critical';
  }

  /** Label court (header). */
  verdictBadgeLabel(): string {
    const r = this.result();
    if (!r) return '';
    return r.verdict === 'ELIGIBLE_RCC_LONGUE_CARRIERE'
      ? 'ÉLIGIBLE'
      : 'INÉLIGIBLE';
  }

  /** Icône Material correspondant au verdict. */
  verdictIcon(): string {
    const r = this.result();
    if (!r) return 'elderly';
    return r.verdict === 'ELIGIBLE_RCC_LONGUE_CARRIERE'
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
  verdictOf(r: RccBeLongueCarriereResponse): RccBeLongueCarriereVerdict {
    return r.verdict;
  }

  calculate(): void {
    if (!this.formValid()) return;
    if (this.workspaceCountry !== 'BELGIQUE') {
      this.snackBar.open('Outil indisponible pour ce workspace', 'Fermer',
        { duration: 4000, panelClass: 'snack-error' });
      return;
    }
    const request: RccBeLongueCarriereRequest = {
      ageFinContrat: this.ageFinContrat()!,
      anneesCarriereTotale: this.anneesCarriereTotale()!,
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
        this.snackBar.open('RCC longue carrière analysé', 'OK', { duration: 2500 });
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
        this.ageFinContrat.set(r.ageFinContrat);
        this.anneesCarriereTotale.set(r.anneesCarriereTotale);
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
