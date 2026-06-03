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
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { DipBeLoiApplicableFamilleService } from '../../core/services/dip-be-loi-applicable-famille.service';
import {
  DipBeLoiApplicableFamilleRequest,
  DipBeLoiApplicableFamilleResponse,
  DipBeLoiApplicableFamilleVerdict,
  FondementRattachement,
  Matiere,
} from '../../core/models/dip-be-loi-applicable-famille.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';
import { DipBeLoiApplicableFamilleSectionPrefillRules } from './dip-be-loi-applicable-famille-section-prefill-rules';

interface MatiereOption { value: Matiere; label: string; }

/**
 * SF-223-07 : composant Angular standalone pour l'outil décisionnel "Loi
 * applicable en droit de la famille (Belgique — DIP)"
 * (`dip-be-loi-applicable-famille`). BELGIQUE uniquement (Rome III ; Règl. UE
 * 2016/1103 ; Règl. UE 650/2012 ; CDIP — à vérifier par avocat belge).
 *
 * 1 outil = 1 situation « détermination de la loi applicable à une situation
 * familiale internationale à instruire ». DISTINCT de la reconnaissance /
 * exequatur d'une décision étrangère déjà rendue
 * (`dip-be-reconnaissance-decision-etrangere`, SF-223-08).
 *
 * Pré-fill IA F-246 : matière + résidence commune + nationalité commune + choix
 * de loi pré-remplis si factualisables (sous-objet `dip_famille_be_detection`).
 * Aucune citation jurisprudentielle BE (F-JU-04 parké — silence > erreur).
 * OnPush + ChangeDetectorRef.markForCheck() dans next/error des subscribe
 * (mémoire `feedback_onpush_subscribe_markforcheck`).
 */
@Component({
  selector: 'app-dip-be-loi-applicable-famille-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatProgressSpinnerModule,
    LegalCitationsPipe,
    ToolJurisprudenceCitationsComponent,
  ],
  templateUrl: './dip-be-loi-applicable-famille-section.component.html',
  styleUrl: './dip-be-loi-applicable-famille-section.component.scss',
})
export class DipBeLoiApplicableFamilleSectionComponent implements OnInit, OnChanges {
  // F-JU-03 — citations jurisprudentielles F-JU-01 (BE parké F-JU-04 : aucune en V1).
  protected readonly toolIdForJurisprudence = 'dip-be-loi-applicable-famille';
  protected readonly brancheActiveForJurisprudence = 'default';

  static readonly TOOL_LABEL = 'LOI APPLICABLE — DROIT DE LA FAMILLE (BELGIQUE)';
  static readonly TOOL_ICON = 'public';

  /**
   * SF-223-07 — pré-fill F-246 : matière + résidence + nationalité + choix de
   * loi si factualisables. Délègue au helper partagé (parité runtime/static —
   * garde-fou `prefill-count-integrity`).
   */
  static getPrefillCount(input: {
    aiData?: unknown;
    procedureChecks?: unknown[];
    aiQuestions?: unknown[];
    piecesManquantes?: unknown[];
    triggerEvents?: unknown[];
    workspaceCountry?: string;
  }): number {
    return DipBeLoiApplicableFamilleSectionPrefillRules.computePrefillCount(input);
  }

  static readonly MATIERE_OPTIONS: readonly MatiereOption[] = [
    { value: 'DIVORCE', label: 'Divorce (Rome III)' },
    { value: 'REGIME_MATRIMONIAL', label: 'Régime matrimonial (Règl. UE 2016/1103)' },
    { value: 'SUCCESSION', label: 'Succession (Règl. UE 650/2012)' },
  ];
  readonly matiereOptions = DipBeLoiApplicableFamilleSectionComponent.MATIERE_OPTIONS;

  @Input() caseFileId!: string;
  /** F-177 — force l'expansion (mode modal). */
  @Input() forceExpanded = false;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'BELGIQUE';
  @Input() aiData?: FamilleExtractedData | null;
  @Input() standaloneMode = false;

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<DipBeLoiApplicableFamilleResponse | null>(null);

  // --- Form fields ---
  matiere = signal<Matiere | null>(null);
  residenceHabituelleCommune = signal<string | null>(null);
  nationaliteCommune = signal<string | null>(null);
  choixLoiParLesParties = signal<string | null>(null);
  dateMariageOuDeces = signal<string | null>(null);
  lieuSituationBiensImmobiliers = signal<string | null>(null);

  constructor(
    private service: DipBeLoiApplicableFamilleService,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef,
    @Optional() private dashboardRefresh: CaseDashboardRefreshService | null,
  ) {}

  ngOnInit(): void {
    if (this.forceExpanded) this.collapsed.set(false);
    this.prefillFromAi();
    if (this.workspaceCountry === 'BELGIQUE') {
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
   * F-246 — pré-remplit matière + résidence + nationalité + choix de loi depuis
   * le sous-objet IA `dip_famille_be_detection` (clés camelCase plates,
   * @JsonUnwrapped). La date et le lieu des biens ne sont pas pré-remplis (peu
   * factualisables de manière stable en V1).
   */
  private prefillFromAi(): void {
    if (this.workspaceCountry !== 'BELGIQUE') {
      this.cdr.markForCheck();
      return;
    }
    const ai = this.aiData as (FamilleExtractedData & {
      dipFamilleBeMatiereDetectee?: string | null;
      dipFamilleBeResidenceCommuneDetectee?: string | null;
      dipFamilleBeNationaliteCommuneDetectee?: string | null;
      dipFamilleBeChoixLoiDetecte?: string | null;
    }) | null | undefined;
    if (!ai) {
      this.cdr.markForCheck();
      return;
    }
    const matiere = ai.dipFamilleBeMatiereDetectee;
    if (matiere === 'DIVORCE' || matiere === 'REGIME_MATRIMONIAL' || matiere === 'SUCCESSION') {
      this.matiere.set(matiere);
    }
    this.setIso2(ai.dipFamilleBeResidenceCommuneDetectee, this.residenceHabituelleCommune);
    this.setIso2(ai.dipFamilleBeNationaliteCommuneDetectee, this.nationaliteCommune);
    this.setIso2(ai.dipFamilleBeChoixLoiDetecte, this.choixLoiParLesParties);
    this.cdr.markForCheck();
  }

  private setIso2(raw: string | null | undefined, target: { set: (v: string) => void }): void {
    if (typeof raw === 'string' && /^[A-Z]{2}$/.test(raw)) {
      target.set(raw);
    }
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  editMode(): void {
    this.showForm.set(true);
  }

  /** Form valide : workspace BE + matière renseignée. */
  formValid(): boolean {
    if (this.workspaceCountry !== 'BELGIQUE') return false;
    if (!this.matiere()) return false;
    return true;
  }

  /** Normalise une saisie ISO-2 vide → null (le backend valide `^[A-Z]{2}$`). */
  private iso2OrNull(raw: string | null): string | null {
    if (raw === null) return null;
    const trimmed = raw.trim().toUpperCase();
    return trimmed === '' ? null : trimmed;
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request: DipBeLoiApplicableFamilleRequest = {
      matiere: this.matiere()!,
      residenceHabituelleCommune: this.iso2OrNull(this.residenceHabituelleCommune()),
      nationaliteCommune: this.iso2OrNull(this.nationaliteCommune()),
      choixLoiParLesParties: this.iso2OrNull(this.choixLoiParLesParties()),
      dateMariageOuDeces: this.dateMariageOuDeces() || null,
      lieuSituationBiensImmobiliers: this.iso2OrNull(this.lieuSituationBiensImmobiliers()),
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.applyResult(r);
        this.calculating.set(false);
        this.snackBar.open('Loi applicable déterminée', 'OK', { duration: 2500 });
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
        this.applyResult(r);
        this.loading.set(false);
        this.cdr.markForCheck();
      },
      error: () => {
        // 404 attendu si aucune analyse — on reste en mode formulaire.
        this.loading.set(false);
        this.cdr.markForCheck();
      },
    });
  }

  private applyResult(r: DipBeLoiApplicableFamilleResponse): void {
    this.result.set(r);
    this.hydrateForm(r);
    this.showForm.set(false);
  }

  private hydrateForm(r: DipBeLoiApplicableFamilleResponse): void {
    this.matiere.set(r.matiere);
    this.residenceHabituelleCommune.set(r.residenceHabituelleCommune);
    this.nationaliteCommune.set(r.nationaliteCommune);
    this.choixLoiParLesParties.set(r.choixLoiParLesParties);
    this.dateMariageOuDeces.set(r.dateMariageOuDeces);
    this.lieuSituationBiensImmobiliers.set(r.lieuSituationBiensImmobiliers);
  }

  // ---------------------------------------------------------------------------
  // Helpers d'affichage du résultat
  // ---------------------------------------------------------------------------

  /**
   * Vert pour LOI_APPLICABLE_DETERMINEE. Navy info pour LOI_INDETERMINABLE
   * (compléter les éléments de rattachement).
   */
  verdictBannerClass(verdict: DipBeLoiApplicableFamilleVerdict): string {
    switch (verdict) {
      case 'LOI_APPLICABLE_DETERMINEE':
        return 'dip-verdict-banner dip-verdict-banner--ok';
      case 'LOI_INDETERMINABLE':
        return 'dip-verdict-banner dip-verdict-banner--info';
    }
  }

  verdictBannerLabel(verdict: DipBeLoiApplicableFamilleVerdict): string {
    switch (verdict) {
      case 'LOI_APPLICABLE_DETERMINEE': return 'Loi applicable déterminée';
      case 'LOI_INDETERMINABLE': return 'Loi applicable indéterminable';
    }
  }

  verdictBannerIcon(verdict: DipBeLoiApplicableFamilleVerdict): string {
    switch (verdict) {
      case 'LOI_APPLICABLE_DETERMINEE': return 'check_circle';
      case 'LOI_INDETERMINABLE': return 'info';
    }
  }

  matiereLabel(m: Matiere): string {
    return this.matiereOptions.find(opt => opt.value === m)?.label ?? m;
  }

  fondementLabel(f: FondementRattachement): string {
    switch (f) {
      case 'CHOIX_LOI_PARTIES': return 'Loi choisie par les parties (professio juris)';
      case 'RESIDENCE_HABITUELLE_COMMUNE': return 'Résidence habituelle commune';
      case 'NATIONALITE_COMMUNE': return 'Nationalité commune';
      case 'RESIDENCE_HABITUELLE_DEFUNT': return 'Résidence habituelle du défunt au décès';
      case 'LOI_DU_FOR': return 'Loi de la juridiction saisie (loi du for)';
      case 'LIEN_LE_PLUS_ETROIT': return 'Lien le plus étroit';
      case 'AUCUN': return 'Indéterminé';
    }
  }
}
