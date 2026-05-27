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
  LicenciementBeCct109DeraisonnableRequest,
  LicenciementBeCct109DeraisonnableResponse,
  LicenciementBeCct109Echelon,
  LicenciementBeCct109MotifLieAPersonne,
} from '../../core/models/licenciement-be-cct109-deraisonnable.model';
import {
  LicenciementBeCct109DeraisonnableService,
} from '../../core/services/licenciement-be-cct109-deraisonnable.service';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import {
  LicenciementBeCct109DeraisonnableSectionPrefillRules,
} from './licenciement-be-cct109-deraisonnable-section-prefill-rules';
import {
  ToolJurisprudenceCitationsComponent,
} from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-213-10b : outil décisionnel « Score CCT n° 109 BE — licenciement
 * manifestement déraisonnable » (F-213).
 *
 * <p>BE uniquement — <b>CCT n° 109 du 12 février 2014</b> (motivation du
 * licenciement + indemnité 3/8/12/17 semaines pour licenciement
 * manifestement déraisonnable, art. 8-9).</p>
 *
 * <p>Affiché par le panel F-IA-04 (tool_id
 * {@code licenciement-be-cct109-deraisonnable}, visibility ALWAYS_ON
 * priority 118 BE / DROIT_DU_TRAVAIL — juste au-dessus de
 * {@code licenciement-be-acte-equivalent} (117) qui sera livrée par
 * SF-213-09b. Pattern miroir des autres outils F-213 vagues 1-8.</p>
 *
 * <p><b>Échelle score CCT 109</b> (5 échelons gradués) :
 * <ul>
 *   <li>{@code NON_DERAISONNABLE} — vert : motif valable + procédures
 *       respectées, pas d'indemnité CCT 109 due (seules les indemnités
 *       de droit commun s'appliquent — ICP, etc.).</li>
 *   <li>{@code 3_SEMAINES} — jaune : minimum légal (motif vague /
 *       absent sans aggravant).</li>
 *   <li>{@code 8_SEMAINES} — orange : licenciement manifestement
 *       déraisonnable de gravité ordinaire.</li>
 *   <li>{@code 12_SEMAINES} — rouge : gravité haute (discrimination ou
 *       représailles suspectées).</li>
 *   <li>{@code 17_SEMAINES} — rouge foncé : maximum légal (faits
 *       graves cumulés — discrimination + représailles).</li>
 * </ul></p>
 *
 * <p><b>Pré-fill IA V1</b> : aucun champ — alignement pattern uniforme
 * F-213 vagues 6b/7b/8b ; l'extraction IA Travail BE ne couvre pas la
 * communication écrite du motif au sens CCT 109 art. 5-8 ni la
 * dérivation hebdomadaire de la rémunération.</p>
 *
 * <p><b>Cumul ICP</b> : l'indemnité CCT 109 se cumule systématiquement
 * avec l'indemnité compensatoire de préavis (Loi 03/07/1978, statut
 * unique / Claeys). Une bannière info systématique invite l'avocat à
 * calculer l'ICP via les autres outils décisionnels BE
 * ({@code licenciement-be-statut-unique-preavis} ou
 * {@code licenciement-be-formule-claeys}).</p>
 *
 * <p>Distinct de {@code F-DT-27-motif-grave-be} (analyseur de validité
 * du motif grave, intervient en amont) et de
 * {@code licenciement-be-protection-deleguee} (population protégée
 * spéciale, indemnité forfaitaire ≠ CCT 109). Distinct du barème
 * Macron FR (L. 1235-3 C. trav.) — gating par {@code workspaceCountry}.</p>
 */
@Component({
  selector: 'app-licenciement-be-cct109-deraisonnable-section',
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
  templateUrl: './licenciement-be-cct109-deraisonnable-section.component.html',
  styleUrl: './licenciement-be-cct109-deraisonnable-section.component.scss',
})
export class LicenciementBeCct109DeraisonnableSectionComponent
  implements OnInit, OnChanges {

  // F-JU-03 — citations jurisprudentielles F-JU-01 (BE).
  protected readonly toolIdForJurisprudence = 'licenciement-be-cct109-deraisonnable';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'SCORE CCT 109 — LICENCIEMENT DÉRAISONNABLE (BE)';
  static readonly TOOL_ICON = 'rule';

  /** F-177 SF-177-12 / F-236 SF-236-02 — délègue au helper partagé (parité runtime). */
  static getPrefillCount(input: PrefillCountInput): number {
    return LicenciementBeCct109DeraisonnableSectionPrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() forceExpanded = false;
  /** Gate strict BE — pas d'équivalent FR (barème Macron L. 1235-3 distinct). */
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
  result = signal<LicenciementBeCct109DeraisonnableResponse | null>(null);

  // ── Champs du form (signals — édités via handlers). ────────────────────
  motifCommunique = signal<boolean | null>(null);
  motifLieAPersonne = signal<LicenciementBeCct109MotifLieAPersonne | null>(null);
  discriminationSuspectee = signal<boolean>(false);
  represaillesSuspectees = signal<boolean>(false);
  proceduresRespectees = signal<boolean>(true);
  remunerationHebdomadaireBrute = signal<number | null>(null);
  argumentsPatronal = signal<string | null>(null);

  /** Gate strict workspace BE. */
  isAvailable = computed(() => this.workspaceCountry === 'BELGIQUE');

  constructor(
    private service: LicenciementBeCct109DeraisonnableService,
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
    // SF-213-10b V1 : pas de pré-fill IA (cf. helper doc) — pas de réaction aiData.
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  formValid(): boolean {
    if (this.motifCommunique() === null) return false;
    if (this.motifLieAPersonne() === null) return false;
    if (this.remunerationHebdomadaireBrute() === null
        || this.remunerationHebdomadaireBrute()! <= 0) return false;
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // -- Handlers --
  onMotifCommuniqueChange(value: boolean): void {
    this.motifCommunique.set(value);
  }
  onMotifLieAPersonneChange(value: LicenciementBeCct109MotifLieAPersonne): void {
    this.motifLieAPersonne.set(value);
  }
  onDiscriminationSuspecteeChange(value: boolean): void {
    this.discriminationSuspectee.set(value);
  }
  onRepresaillesSuspecteesChange(value: boolean): void {
    this.represaillesSuspectees.set(value);
  }
  onProceduresRespecteesChange(value: boolean): void {
    this.proceduresRespectees.set(value);
  }
  onRemunerationHebdomadaireBruteChange(value: number | null): void {
    this.remunerationHebdomadaireBrute.set(value);
  }
  onArgumentsPatronalChange(value: string | null): void {
    this.argumentsPatronal.set(value || null);
  }

  // -- Libellés humains --
  motifLieAPersonneLabel(value: LicenciementBeCct109MotifLieAPersonne | null): string {
    switch (value) {
      case 'SANS_MOTIF': return 'Aucun motif communiqué';
      case 'MOTIF_VAGUE': return 'Motif vague / imprécis';
      case 'MOTIF_PRECIS_DISPUTE': return 'Motif précis contesté';
      case 'MOTIF_PROUVE_VALIDE': return 'Motif prouvé / valide';
      default: return '—';
    }
  }

  /** Libellé humain de l'échelon CCT 109. */
  echelonLabel(value: LicenciementBeCct109Echelon | null): string {
    switch (value) {
      case 'NON_DERAISONNABLE': return 'Non déraisonnable';
      case '3_SEMAINES': return 'Minimum 3 semaines';
      case '8_SEMAINES': return 'Manifestement déraisonnable — 8 semaines';
      case '12_SEMAINES': return 'Gravité haute — 12 semaines';
      case '17_SEMAINES': return 'Maximum légal — 17 semaines';
      default: return '—';
    }
  }

  /**
   * Classe CSS de la bannière échelon — gradient vert → rouge foncé selon
   * la gravité (NON_DERAISONNABLE vert, 3 sem. jaune, 8 sem. orange,
   * 12 sem. rouge, 17 sem. rouge foncé).
   */
  echelonClass(): string {
    const r = this.result();
    if (!r) return '';
    switch (r.echelonCct109) {
      case 'NON_DERAISONNABLE': return 'echelon-ok';
      case '3_SEMAINES': return 'echelon-warn';
      case '8_SEMAINES': return 'echelon-orange';
      case '12_SEMAINES': return 'echelon-critical';
      case '17_SEMAINES': return 'echelon-max';
      default: return '';
    }
  }

  /** Icône Material correspondant à l'échelon. */
  echelonIcon(): string {
    const r = this.result();
    if (!r) return 'rule';
    switch (r.echelonCct109) {
      case 'NON_DERAISONNABLE': return 'check_circle';
      case '3_SEMAINES': return 'info';
      case '8_SEMAINES': return 'warning_amber';
      case '12_SEMAINES': return 'gpp_bad';
      case '17_SEMAINES': return 'dangerous';
      default: return 'rule';
    }
  }

  /** Badge court (header) selon échelon. */
  echelonBadgeShort(value: LicenciementBeCct109Echelon): string {
    switch (value) {
      case 'NON_DERAISONNABLE': return 'NON DÉRAISONNABLE';
      case '3_SEMAINES': return '3 SEMAINES';
      case '8_SEMAINES': return '8 SEMAINES';
      case '12_SEMAINES': return '12 SEMAINES';
      case '17_SEMAINES': return '17 SEMAINES';
      default: return '—';
    }
  }

  /** Classe CSS du badge court (header) selon échelon. */
  echelonBadgeShortClass(value: LicenciementBeCct109Echelon): string {
    switch (value) {
      case 'NON_DERAISONNABLE': return 'badge-ok';
      case '3_SEMAINES': return 'badge-warn';
      case '8_SEMAINES': return 'badge-orange';
      case '12_SEMAINES': return 'badge-critical';
      case '17_SEMAINES': return 'badge-max';
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
    const request: LicenciementBeCct109DeraisonnableRequest = {
      motifCommunique: this.motifCommunique()!,
      motifLieAPersonne: this.motifLieAPersonne()!,
      discriminationSuspectee: this.discriminationSuspectee(),
      represaillesSuspectees: this.represaillesSuspectees(),
      proceduresRespectees: this.proceduresRespectees(),
      remunerationHebdomadaireBrute: this.remunerationHebdomadaireBrute()!,
      argumentsPatronal: this.argumentsPatronal(),
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Score CCT 109 BE analysé', 'OK', { duration: 2500 });
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
        this.motifCommunique.set(r.motifCommunique);
        this.motifLieAPersonne.set(r.motifLieAPersonne);
        this.discriminationSuspectee.set(r.discriminationSuspectee);
        this.represaillesSuspectees.set(r.represaillesSuspectees);
        this.proceduresRespectees.set(r.proceduresRespectees);
        this.remunerationHebdomadaireBrute.set(r.remunerationHebdomadaireBrute);
        this.argumentsPatronal.set(r.argumentsPatronal);
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
