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
import { MatListModule } from '@angular/material/list';
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
  RccBeMetiersLourdsRequest,
  RccBeMetiersLourdsResponse,
  RccBeMetiersLourdsType,
  RccBeMetiersLourdsVerdict,
} from '../../core/models/rcc-be-metiers-lourds.model';
import {
  RccBeMetiersLourdsService,
} from '../../core/services/rcc-be-metiers-lourds.service';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import {
  RccBeMetiersLourdsSectionPrefillRules,
} from './rcc-be-metiers-lourds-section-prefill-rules';
import {
  ToolJurisprudenceCitationsComponent,
} from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-219-01b : outil décisionnel « RCC métiers lourds (Belgique) » (F-219).
 *
 * <p>BE uniquement — <b>CCT n° 17</b> du 19/12/1974 (Conseil National du
 * Travail — régime d'indemnité complémentaire aux travailleurs âgés en cas
 * de licenciement) + <b>AR du 03/05/2007</b> fixant le RCC, <b>art. 3</b>
 * (métiers lourds 58+/35 ans).</p>
 *
 * <p>Affiché par le panel F-IA-04 (tool_id {@code rcc-be-metiers-lourds},
 * visibility ALWAYS_ON priority 119 BE / DROIT_DU_TRAVAIL — juste au-dessus
 * de {@code licenciement-be-cct109-deraisonnable} (118). Pattern miroir
 * des autres outils F-213 vagues 1-10.</p>
 *
 * <p><b>Verdict 2 états</b> :
 * <ul>
 *   <li>{@code ELIGIBLE} (vert) — toutes conditions cumulatives remplies
 *       (licenciement effectif ; âge ≥ 58 ; carrière ≥ 35 ;
 *       5/10 OU 7/15 en métier lourd).</li>
 *   <li>{@code INELIGIBLE} (rouge) — au moins une condition manque
 *       (raison précise affichée).</li>
 * </ul></p>
 *
 * <p><b>Pré-fill IA V1</b> : aucun champ — alignement pattern uniforme
 * F-213 vagues 6b/7b/8b/9b/10b ; les champs (âge, carrière, métier lourd,
 * type) demandent une qualification juridique avocat fine non
 * automatisable depuis l'extraction Travail BE actuelle.</p>
 *
 * <p>Distinct de {@code rcc-be-conditions} (F-207 SF-207-06b — RCC général
 * 60+/40 ans) et de {@code rcc-be-indemnite-complementaire} (F-207
 * SF-207-07b — calcul d'indemnité applicable transversalement). Le RCC
 * métiers lourds est un régime spécifique avec abaissement des seuils
 * d'âge / carrière mais ajout d'une condition d'historique en métier
 * lourd.</p>
 */
@Component({
  selector: 'app-rcc-be-metiers-lourds-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatCheckboxModule, MatSelectModule,
    MatListModule,
    MatProgressSpinnerModule,
    ToolJurisprudenceCitationsComponent,
  ],
  templateUrl: './rcc-be-metiers-lourds-section.component.html',
  styleUrl: './rcc-be-metiers-lourds-section.component.scss',
})
export class RccBeMetiersLourdsSectionComponent
  implements OnInit, OnChanges {

  // F-JU-03 — citations jurisprudentielles F-JU-01 (BE).
  protected readonly toolIdForJurisprudence = 'rcc-be-metiers-lourds';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'RCC MÉTIERS LOURDS (BE)';
  static readonly TOOL_ICON = 'construction';

  /** F-177 SF-177-12 / F-236 SF-236-02 — délègue au helper partagé (parité runtime). */
  static getPrefillCount(input: PrefillCountInput): number {
    return RccBeMetiersLourdsSectionPrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() forceExpanded = false;
  /** Gate strict BE — pas d'équivalent FR (pré-retraite supprimée 2010). */
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
  result = signal<RccBeMetiersLourdsResponse | null>(null);

  // ── Champs du form (signals — édités via handlers). ────────────────────
  ageFinContrat = signal<number | null>(null);
  anneesCarriereTotal = signal<number | null>(null);
  anneesMetierLourdRecent10 = signal<number | null>(null);
  anneesMetierLourdRecent15 = signal<number | null>(null);
  typeMetierLourd = signal<RccBeMetiersLourdsType | null>(null);
  licenciementEffectif = signal<boolean>(true);

  /** Gate strict workspace BE. */
  isAvailable = computed(() => this.workspaceCountry === 'BELGIQUE');

  constructor(
    private service: RccBeMetiersLourdsService,
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
    // SF-219-01b V1 : pas de pré-fill IA (cf. helper doc) — pas de réaction aiData.
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  formValid(): boolean {
    if (this.ageFinContrat() === null) return false;
    if (this.anneesCarriereTotal() === null) return false;
    if (this.anneesMetierLourdRecent10() === null) return false;
    if (this.anneesMetierLourdRecent15() === null) return false;
    if (this.typeMetierLourd() === null) return false;
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // -- Handlers --
  onAgeFinContratChange(value: number | null): void {
    this.ageFinContrat.set(value);
  }
  onAnneesCarriereTotalChange(value: number | null): void {
    this.anneesCarriereTotal.set(value);
  }
  onAnneesMetierLourdRecent10Change(value: number | null): void {
    this.anneesMetierLourdRecent10.set(value);
  }
  onAnneesMetierLourdRecent15Change(value: number | null): void {
    this.anneesMetierLourdRecent15.set(value);
  }
  onTypeMetierLourdChange(value: RccBeMetiersLourdsType): void {
    this.typeMetierLourd.set(value);
  }
  onLicenciementEffectifChange(value: boolean): void {
    this.licenciementEffectif.set(value);
  }

  // -- Libellés humains --
  typeMetierLourdLabel(value: RccBeMetiersLourdsType | null): string {
    switch (value) {
      case 'EQUIPES_SUCCESSIVES_NUIT':
        return 'Équipes successives avec prestations de nuit';
      case 'INTERRUPTIONS_HORAIRES_VARIABLES':
        return 'Interruptions à horaires variables (jour-nuit)';
      case 'TRAVAIL_PENIBLE':
        return 'Travail pénible (manutention, construction, mines)';
      case 'AUTRE':
        return 'Autre métier lourd (à justifier)';
      default: return '—';
    }
  }

  raisonIneligibiliteLabel(): string {
    const r = this.result();
    if (!r || !r.raisonIneligibilite) return '';
    switch (r.raisonIneligibilite) {
      case 'DEMISSION_NON_ELIGIBLE':
        return 'Démission — le RCC est strictement réservé au licenciement.';
      case 'AGE_INSUFFISANT':
        return 'Âge inférieur à 58 ans à la fin du contrat.';
      case 'CARRIERE_INSUFFISANTE':
        return 'Carrière professionnelle totale inférieure à 35 ans.';
      case 'DUREE_METIER_LOURD_INSUFFISANTE':
        return 'Ni 5 ans en métier lourd sur les 10 dernières années, '
             + 'ni 7 ans sur les 15 dernières années.';
      default: return '';
    }
  }

  /** Verdict en français lisible. */
  verdictLabel(): string {
    const r = this.result();
    if (!r) return '—';
    switch (r.verdict) {
      case 'ELIGIBLE':
        return 'Éligible au RCC métiers lourds';
      case 'INELIGIBLE':
        return 'Non éligible au RCC métiers lourds';
      default:
        return '—';
    }
  }

  /** Classe CSS verdict (vert éligible, rouge inéligible). */
  verdictClass(): string {
    const r = this.result();
    if (!r) return '';
    switch (r.verdict) {
      case 'ELIGIBLE': return 'verdict-ok';
      case 'INELIGIBLE': return 'verdict-critical';
      default: return '';
    }
  }

  /** Icône Material du verdict. */
  verdictIcon(): string {
    const r = this.result();
    if (!r) return 'gavel';
    switch (r.verdict) {
      case 'ELIGIBLE': return 'verified';
      case 'INELIGIBLE': return 'gpp_bad';
      default: return 'gavel';
    }
  }

  /** Libellé bref pour le badge du header. */
  verdictBadge(verdict: RccBeMetiersLourdsVerdict): string {
    switch (verdict) {
      case 'ELIGIBLE': return 'ÉLIGIBLE';
      case 'INELIGIBLE': return 'NON ÉLIGIBLE';
      default: return '';
    }
  }

  /** Classe CSS badge du header. */
  verdictBadgeClass(verdict: RccBeMetiersLourdsVerdict): string {
    switch (verdict) {
      case 'ELIGIBLE': return 'badge-ok';
      case 'INELIGIBLE': return 'badge-critical';
      default: return '';
    }
  }

  calculate(): void {
    if (!this.formValid()) return;
    if (this.workspaceCountry !== 'BELGIQUE') {
      this.snackBar.open('Outil indisponible pour ce workspace', 'Fermer',
        { duration: 4000, panelClass: 'snack-error' });
      return;
    }
    const request: RccBeMetiersLourdsRequest = {
      ageFinContrat: this.ageFinContrat()!,
      anneesCarriereTotal: this.anneesCarriereTotal()!,
      anneesMetierLourdRecent10: this.anneesMetierLourdRecent10()!,
      anneesMetierLourdRecent15: this.anneesMetierLourdRecent15()!,
      typeMetierLourd: this.typeMetierLourd()!,
      licenciementEffectif: this.licenciementEffectif(),
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Éligibilité RCC métiers lourds analysée', 'OK', { duration: 2500 });
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
        this.anneesCarriereTotal.set(r.anneesCarriereTotal);
        this.anneesMetierLourdRecent10.set(r.anneesMetierLourdRecent10);
        this.anneesMetierLourdRecent15.set(r.anneesMetierLourdRecent15);
        this.typeMetierLourd.set(r.typeMetierLourd);
        this.licenciementEffectif.set(r.licenciementEffectif);
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
