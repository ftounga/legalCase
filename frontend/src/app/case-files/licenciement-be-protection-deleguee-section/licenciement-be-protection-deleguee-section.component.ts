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
  LicenciementBeProtectionDelegueeRequest,
  LicenciementBeProtectionDelegueeResponse,
  LicenciementBeStatutProtege,
} from '../../core/models/licenciement-be-protection-deleguee.model';
import {
  LicenciementBeProtectionDelegueeService,
} from '../../core/services/licenciement-be-protection-deleguee.service';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import {
  LicenciementBeProtectionDelegueeSectionPrefillRules,
} from './licenciement-be-protection-deleguee-section-prefill-rules';
import {
  ToolJurisprudenceCitationsComponent,
} from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-213-08b : outil décisionnel « Licenciement BE — protection délégué
 * syndical » (F-213).
 *
 * <p>BE uniquement — <b>Loi du 19 mars 1991</b> (protection des
 * représentants des travailleurs au CE et CPPT) +
 * <b>CCT n° 5 du 24 mai 1971</b> (statut des délégations syndicales).</p>
 *
 * <p>Affiché par le panel F-IA-04 (tool_id
 * {@code licenciement-be-protection-deleguee}, visibility ALWAYS_ON
 * priority 116 BE / DROIT_DU_TRAVAIL — juste au-dessus de
 * {@code harcelement-be-procedure-formelle} (115). Pattern miroir des
 * autres outils F-213 vague 1-7.</p>
 *
 * <p><b>Verdict binaire</b> :
 * <ul>
 *   <li>{@code LICENCIEMENT_INTERDIT_SANS_PROCEDURE} (rouge) — la rupture
 *       intervient dans la fenêtre de protection (2 ans
 *       candidat/CCT n° 5, 4 ans élu CE/CPPT). Affichage :
 *       <ul>
 *         <li>indemnité forfaitaire (rémunération annuelle × 2 ou × 4
 *             selon circonstances aggravantes) ;</li>
 *         <li>délai 30 jours (Loi 19/03/1991 art. 14) pour demander la
 *             réintégration — badge rouge si dépassé ou ≤ 7 jours,
 *             ambre si ≤ 30 jours.</li>
 *       </ul></li>
 *   <li>{@code HORS_PERIODE_PROTECTION} (vert) — droit commun (préavis,
 *       indemnité compensatoire, motif grave).</li>
 * </ul></p>
 *
 * <p><b>Pré-fill IA V1</b> : aucun champ — alignement pattern uniforme
 * F-213 vagues 6b/7b ; l'extraction IA Travail BE ne couvre pas la
 * représentativité (statut, mandat) ni la dérivation rémunération
 * annuelle.</p>
 *
 * <p>Distinct de {@code licenciement-be-protection-grossesse}
 * (SF-213-05b — autre population protégée). Distinct de
 * {@code F-DT-30-protection-rp} (statut représentatif FR — L. 2411-1 et s.
 * C. trav.) — gating par {@code workspaceCountry}.</p>
 */
@Component({
  selector: 'app-licenciement-be-protection-deleguee-section',
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
  templateUrl: './licenciement-be-protection-deleguee-section.component.html',
  styleUrl: './licenciement-be-protection-deleguee-section.component.scss',
})
export class LicenciementBeProtectionDelegueeSectionComponent
  implements OnInit, OnChanges {

  // F-JU-03 — citations jurisprudentielles F-JU-01 (BE).
  protected readonly toolIdForJurisprudence = 'licenciement-be-protection-deleguee';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'LICENCIEMENT — PROTECTION DÉLÉGUÉ (BE)';
  static readonly TOOL_ICON = 'verified_user';

  /** F-177 SF-177-12 / F-236 SF-236-02 — délègue au helper partagé (parité runtime). */
  static getPrefillCount(input: PrefillCountInput): number {
    return LicenciementBeProtectionDelegueeSectionPrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() forceExpanded = false;
  /** Gate strict BE — pas d'équivalent FR (statut protégé délégué CSE distinct). */
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
  result = signal<LicenciementBeProtectionDelegueeResponse | null>(null);

  // ── Champs du form (signals — édités via handlers). ────────────────────
  statutProtege = signal<LicenciementBeStatutProtege | null>(null);
  ancienneteAnnees = signal<number | null>(null);
  remunerationAnnuelleBrute = signal<number | null>(null);
  dateLicenciement = signal<string | null>(null);
  dateElectionOuMandat = signal<string | null>(null);
  demandeReintegrationDansTrente = signal<boolean>(false);
  employeurRefuseReintegration = signal<boolean>(false);
  circonstancesAggravantes = signal<boolean>(false);

  /** Gate strict workspace BE. */
  isAvailable = computed(() => this.workspaceCountry === 'BELGIQUE');

  /**
   * {@code employeurRefuseReintegration} n'est saisissable que si
   * {@code demandeReintegrationDansTrente=true} (cohérence Validator).
   */
  employeurRefuseEditable = computed(() => this.demandeReintegrationDansTrente());

  constructor(
    private service: LicenciementBeProtectionDelegueeService,
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
    // SF-213-08b V1 : pas de pré-fill IA (cf. helper doc) — pas de réaction aiData.
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  formValid(): boolean {
    if (this.statutProtege() === null) return false;
    if (this.ancienneteAnnees() === null || this.ancienneteAnnees()! < 0) return false;
    if (this.remunerationAnnuelleBrute() === null || this.remunerationAnnuelleBrute()! <= 0) return false;
    if (!this.dateLicenciement()) return false;
    if (!this.dateElectionOuMandat()) return false;
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // -- Handlers --
  onStatutProtegeChange(value: LicenciementBeStatutProtege): void {
    this.statutProtege.set(value);
  }
  onAncienneteAnneesChange(value: number | null): void {
    this.ancienneteAnnees.set(value);
  }
  onRemunerationAnnuelleBruteChange(value: number | null): void {
    this.remunerationAnnuelleBrute.set(value);
  }
  onDateLicenciementChange(value: string | null): void {
    this.dateLicenciement.set(value || null);
  }
  onDateElectionOuMandatChange(value: string | null): void {
    this.dateElectionOuMandat.set(value || null);
  }
  onDemandeReintegrationDansTrenteChange(value: boolean): void {
    this.demandeReintegrationDansTrente.set(value);
    // Cohérence Validator : si on décoche la demande, on décoche aussi le refus.
    if (!value) {
      this.employeurRefuseReintegration.set(false);
    }
  }
  onEmployeurRefuseReintegrationChange(value: boolean): void {
    this.employeurRefuseReintegration.set(value);
  }
  onCirconstancesAggravantesChange(value: boolean): void {
    this.circonstancesAggravantes.set(value);
  }

  // -- Libellés humains --
  statutProtegeLabel(value: LicenciementBeStatutProtege | null): string {
    switch (value) {
      case 'DELEGUE_SYNDICAL_ELU': return 'Délégué syndical élu (CE / CPPT)';
      case 'CANDIDAT_NON_ELU': return 'Candidat non élu';
      case 'DELEGUE_CCT5': return 'Délégué syndical (CCT n° 5)';
      case 'CONSEILLER_CPPT': return 'Conseiller CPPT élu';
      default: return '—';
    }
  }

  /** Verdict en français lisible. */
  verdictLabel(): string {
    const r = this.result();
    if (!r) return '—';
    switch (r.verdict) {
      case 'LICENCIEMENT_INTERDIT_SANS_PROCEDURE':
        return 'Licenciement interdit sans procédure';
      case 'HORS_PERIODE_PROTECTION':
        return 'Hors période de protection';
      default:
        return '—';
    }
  }

  /** Classe CSS pour la bannière verdict (rouge si interdit, vert sinon). */
  verdictClass(): string {
    const r = this.result();
    if (!r) return '';
    return r.verdict === 'LICENCIEMENT_INTERDIT_SANS_PROCEDURE'
      ? 'verdict-critical'
      : 'verdict-ok';
  }

  /** Icône Material correspondant au verdict. */
  verdictIcon(): string {
    const r = this.result();
    if (!r) return 'gavel';
    return r.verdict === 'LICENCIEMENT_INTERDIT_SANS_PROCEDURE'
      ? 'gpp_bad'
      : 'verified';
  }

  /**
   * Classe CSS du surlignage du délai de réintégration (30 j) :
   * - rouge bold si dépassé ou ≤ 7 jours
   * - ambre si ≤ 30 jours
   * - normal sinon
   */
  delaiReintegrationClass(): string {
    const r = this.result();
    if (!r || r.joursRestantsReintegration === null) return '';
    if (r.delaiReintegrationDepasse || r.joursRestantsReintegration <= 7) {
      return 'delai-critical';
    }
    if (r.joursRestantsReintegration <= 30) {
      return 'delai-warn';
    }
    return 'delai-ok';
  }

  calculate(): void {
    if (!this.formValid()) return;
    if (this.workspaceCountry !== 'BELGIQUE') {
      this.snackBar.open('Outil indisponible pour ce workspace', 'Fermer',
        { duration: 4000, panelClass: 'snack-error' });
      return;
    }
    const request: LicenciementBeProtectionDelegueeRequest = {
      statutProtege: this.statutProtege()!,
      ancienneteAnnees: this.ancienneteAnnees()!,
      remunerationAnnuelleBrute: this.remunerationAnnuelleBrute()!,
      dateLicenciement: this.dateLicenciement()!,
      dateElectionOuMandat: this.dateElectionOuMandat()!,
      demandeReintegrationDansTrente: this.demandeReintegrationDansTrente(),
      employeurRefuseReintegration: this.employeurRefuseReintegration(),
      circonstancesAggravantes: this.circonstancesAggravantes(),
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Protection délégué BE analysée', 'OK', { duration: 2500 });
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
        this.statutProtege.set(r.statutProtege);
        this.ancienneteAnnees.set(r.ancienneteAnnees);
        this.remunerationAnnuelleBrute.set(r.remunerationAnnuelleBrute);
        this.dateLicenciement.set(r.dateLicenciement);
        this.dateElectionOuMandat.set(r.dateElectionOuMandat);
        this.demandeReintegrationDansTrente.set(r.demandeReintegrationDansTrente);
        this.employeurRefuseReintegration.set(r.employeurRefuseReintegration);
        this.circonstancesAggravantes.set(r.circonstancesAggravantes);
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
