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
  CumulRccAllocationsActiviteAutorisee,
  CumulRccAllocationsDisponibiliteRegime,
  CumulRccAllocationsRequest,
  CumulRccAllocationsResponse,
  CumulRccAllocationsVerdict,
} from '../../core/models/cumul-rcc-allocations.model';
import {
  CumulRccAllocationsService,
} from '../../core/services/cumul-rcc-allocations.service';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import {
  CumulRccAllocationsSectionPrefillRules,
} from './cumul-rcc-allocations-section-prefill-rules';
import {
  ToolJurisprudenceCitationsComponent,
} from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-219-04b : outil décisionnel « Cumul RCC + allocations / pension (BE) »
 * (F-219).
 *
 * <p>BE uniquement — <b>CCT n° 17</b> du 19/12/1974 (Conseil National du
 * Travail — régime d'indemnité complémentaire) + <b>AR du 25/11/1991</b>
 * portant réglementation du chômage (ONEM, plafond cumul) + <b>AR du
 * 03/05/2007 art. 22 et suiv.</b> (disponibilité ajustée selon l'âge).</p>
 *
 * <p>Affiché par le panel F-IA-04 (tool_id {@code cumul-rcc-allocations},
 * visibility ALWAYS_ON priority 122 BE / DROIT_DU_TRAVAIL — au-dessus de
 * {@code rcc-be-entreprise-difficulte} (121). Pattern miroir des autres
 * outils RCC BE de la F-219 (SF-219-01b/02b/03b).</p>
 *
 * <p><b>Verdict 4 états</b> :
 * <ul>
 *   <li>{@code CUMUL_CONFORME} (vert) — total ≤ rémunération nette de
 *       référence + disponibilité OK + pas de bascule pension.</li>
 *   <li>{@code CUMUL_PLAFOND_DEPASSE} (rouge) — l'indemnité complémentaire
 *       doit être réduite (montant exact affiché).</li>
 *   <li>{@code BASCULE_PENSION_LEGALE} (ambre) — âge légal pension atteint,
 *       le RCC s'éteint automatiquement.</li>
 *   <li>{@code INCOMPATIBLE_ACTIVITE_NON_AUTORISEE} (rouge) — sortie du
 *       régime + récupération ONEM.</li>
 * </ul></p>
 *
 * <p><b>Pré-fill IA V1</b> : aucun champ — alignement pattern uniforme
 * F-213/F-219 (qualification carrière ETP ONSS + montants nets ONEM +
 * date prise de cours RCC non extractibles depuis la pipeline Travail BE
 * actuelle ; saisie avocat assumée depuis attestation C4 + bulletins de
 * paie + accord CCT 17).</p>
 *
 * <p>Distinct de {@code rcc-be-conditions} (F-207 SF-207-06b — éligibilité
 * RCC général), {@code rcc-be-indemnite-complementaire} (F-207 SF-207-07b —
 * calcul d'indemnité), {@code rcc-be-metiers-lourds} (SF-219-01b),
 * {@code rcc-be-longue-carriere} (SF-219-02b) et
 * {@code rcc-be-entreprise-difficulte} (SF-219-03b). Cet outil intervient
 * <em>après</em> l'éligibilité acquise : vérification du cumul mensuel
 * avec les allocations chômage ONEM + détection bascule pension légale +
 * compatibilité activités complémentaires.</p>
 */
@Component({
  selector: 'app-cumul-rcc-allocations-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatSelectModule,
    MatListModule,
    MatProgressSpinnerModule,
    ToolJurisprudenceCitationsComponent,
  ],
  templateUrl: './cumul-rcc-allocations-section.component.html',
  styleUrl: './cumul-rcc-allocations-section.component.scss',
})
export class CumulRccAllocationsSectionComponent
  implements OnInit, OnChanges {

  // F-JU-03 — citations jurisprudentielles F-JU-01 (BE).
  protected readonly toolIdForJurisprudence = 'cumul-rcc-allocations';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'CUMUL RCC + ALLOCATIONS (BE)';
  static readonly TOOL_ICON = 'account_balance_wallet';

  /** F-177 SF-177-12 / F-236 SF-236-02 — délègue au helper partagé (parité runtime). */
  static getPrefillCount(input: PrefillCountInput): number {
    return CumulRccAllocationsSectionPrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() forceExpanded = false;
  /** Gate strict BE — pas d'équivalent FR. */
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
  result = signal<CumulRccAllocationsResponse | null>(null);

  // ── Champs du form (signals — édités via handlers). ────────────────────
  dateNaissance = signal<string | null>(null);
  dateDebutRcc = signal<string | null>(null);
  allocationChomageMensuelle = signal<number | null>(null);
  indemniteComplementaireMensuelle = signal<number | null>(null);
  remunerationNetteReference = signal<number | null>(null);
  anneesCarriereTotale = signal<number | null>(null);
  activiteComplementaire = signal<CumulRccAllocationsActiviteAutorisee | null>(null);
  ageLegalPension = signal<number | null>(null);

  /** Gate strict workspace BE. */
  isAvailable = computed(() => this.workspaceCountry === 'BELGIQUE');

  constructor(
    private service: CumulRccAllocationsService,
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
    // SF-219-04b V1 : pas de pré-fill IA (cf. helper doc) — pas de réaction aiData.
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  formValid(): boolean {
    if (!this.dateNaissance()) return false;
    if (!this.dateDebutRcc()) return false;
    if (this.allocationChomageMensuelle() === null) return false;
    if (this.indemniteComplementaireMensuelle() === null) return false;
    if (this.remunerationNetteReference() === null) return false;
    if (this.anneesCarriereTotale() === null) return false;
    if (this.activiteComplementaire() === null) return false;
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // -- Handlers --
  onDateNaissanceChange(value: string | null): void {
    this.dateNaissance.set(value);
  }
  onDateDebutRccChange(value: string | null): void {
    this.dateDebutRcc.set(value);
  }
  onAllocationChomageMensuelleChange(value: number | null): void {
    this.allocationChomageMensuelle.set(value);
  }
  onIndemniteComplementaireMensuelleChange(value: number | null): void {
    this.indemniteComplementaireMensuelle.set(value);
  }
  onRemunerationNetteReferenceChange(value: number | null): void {
    this.remunerationNetteReference.set(value);
  }
  onAnneesCarriereTotaleChange(value: number | null): void {
    this.anneesCarriereTotale.set(value);
  }
  onActiviteComplementaireChange(value: CumulRccAllocationsActiviteAutorisee): void {
    this.activiteComplementaire.set(value);
  }
  onAgeLegalPensionChange(value: number | null): void {
    this.ageLegalPension.set(value);
  }

  // -- Libellés humains --
  activiteComplementaireLabel(
    value: CumulRccAllocationsActiviteAutorisee | null,
  ): string {
    switch (value) {
      case 'AUCUNE':
        return 'Aucune activité complémentaire';
      case 'ACTIVITE_INDEPENDANT_ACCESSOIRE':
        return 'Indépendant accessoire (déclaration ONEM)';
      case 'ACTIVITE_BENEVOLE_DECLAREE':
        return 'Bénévolat / mandat déclaré ONEM';
      case 'ACTIVITE_NON_DECLAREE':
        return 'Activité non déclarée';
      default:
        return '—';
    }
  }

  regimeDisponibiliteLabel(
    value: CumulRccAllocationsDisponibiliteRegime | null,
  ): string {
    switch (value) {
      case 'DISPONIBILITE_ADAPTEE':
        return 'Disponibilité adaptée (recherche active allégée)';
      case 'DISPONIBILITE_PASSIVE':
        return 'Disponibilité passive (dispense recherche)';
      case 'DISPENSE_RECHERCHE_EMPLOI':
        return 'Dispense totale de recherche';
      default:
        return '—';
    }
  }

  /** Verdict en français lisible. */
  verdictLabel(): string {
    const r = this.result();
    if (!r) return '—';
    switch (r.verdict) {
      case 'CUMUL_CONFORME':
        return 'Cumul conforme aux plafonds';
      case 'CUMUL_PLAFOND_DEPASSE':
        return 'Plafond de cumul dépassé';
      case 'BASCULE_PENSION_LEGALE':
        return 'Bascule pension légale — RCC éteint';
      case 'INCOMPATIBLE_ACTIVITE_NON_AUTORISEE':
        return 'Activité non autorisée — sortie du régime';
      default:
        return '—';
    }
  }

  /** Classe CSS verdict (vert conforme, rouge anomalie, ambre bascule). */
  verdictClass(): string {
    const r = this.result();
    if (!r) return '';
    switch (r.verdict) {
      case 'CUMUL_CONFORME':
        return 'verdict-ok';
      case 'CUMUL_PLAFOND_DEPASSE':
      case 'INCOMPATIBLE_ACTIVITE_NON_AUTORISEE':
        return 'verdict-critical';
      case 'BASCULE_PENSION_LEGALE':
        return 'verdict-warn';
      default:
        return '';
    }
  }

  /** Icône Material du verdict. */
  verdictIcon(): string {
    const r = this.result();
    if (!r) return 'gavel';
    switch (r.verdict) {
      case 'CUMUL_CONFORME':
        return 'verified';
      case 'CUMUL_PLAFOND_DEPASSE':
        return 'trending_down';
      case 'BASCULE_PENSION_LEGALE':
        return 'elderly';
      case 'INCOMPATIBLE_ACTIVITE_NON_AUTORISEE':
        return 'gpp_bad';
      default:
        return 'gavel';
    }
  }

  /** Libellé bref pour le badge du header. */
  verdictBadge(verdict: CumulRccAllocationsVerdict): string {
    switch (verdict) {
      case 'CUMUL_CONFORME':
        return 'CONFORME';
      case 'CUMUL_PLAFOND_DEPASSE':
        return 'PLAFOND DÉPASSÉ';
      case 'BASCULE_PENSION_LEGALE':
        return 'BASCULE PENSION';
      case 'INCOMPATIBLE_ACTIVITE_NON_AUTORISEE':
        return 'ACTIVITÉ INTERDITE';
      default:
        return '';
    }
  }

  /** Classe CSS badge du header. */
  verdictBadgeClass(verdict: CumulRccAllocationsVerdict): string {
    switch (verdict) {
      case 'CUMUL_CONFORME':
        return 'badge-ok';
      case 'CUMUL_PLAFOND_DEPASSE':
      case 'INCOMPATIBLE_ACTIVITE_NON_AUTORISEE':
        return 'badge-critical';
      case 'BASCULE_PENSION_LEGALE':
        return 'badge-warn';
      default:
        return '';
    }
  }

  calculate(): void {
    if (!this.formValid()) return;
    if (this.workspaceCountry !== 'BELGIQUE') {
      this.snackBar.open('Outil indisponible pour ce workspace', 'Fermer',
        { duration: 4000, panelClass: 'snack-error' });
      return;
    }
    const request: CumulRccAllocationsRequest = {
      dateNaissance: this.dateNaissance()!,
      dateDebutRcc: this.dateDebutRcc()!,
      allocationChomageMensuelle: this.allocationChomageMensuelle()!,
      indemniteComplementaireMensuelle: this.indemniteComplementaireMensuelle()!,
      remunerationNetteReference: this.remunerationNetteReference()!,
      anneesCarriereTotale: this.anneesCarriereTotale()!,
      activiteComplementaire: this.activiteComplementaire()!,
      ageLegalPension: this.ageLegalPension(),
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Cumul RCC analysé', 'OK', { duration: 2500 });
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
        this.dateNaissance.set(r.dateNaissance);
        this.dateDebutRcc.set(r.dateDebutRcc);
        this.allocationChomageMensuelle.set(r.allocationChomageMensuelle);
        this.indemniteComplementaireMensuelle.set(r.indemniteComplementaireMensuelle);
        this.remunerationNetteReference.set(r.remunerationNetteReference);
        this.anneesCarriereTotale.set(r.anneesCarriereTotale);
        this.activiteComplementaire.set(r.activiteComplementaire);
        this.ageLegalPension.set(r.ageLegalPension);
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
