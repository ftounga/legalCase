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
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { DipBeReconnaissanceDecisionEtrangereService } from '../../core/services/dip-be-reconnaissance-decision-etrangere.service';
import {
  DipBeReconnaissanceDecisionEtrangereRequest,
  DipBeReconnaissanceDecisionEtrangereResponse,
  DipBeReconnaissanceVerdict,
  NatureDecision,
} from '../../core/models/dip-be-reconnaissance-decision-etrangere.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';
import { DipBeReconnaissanceDecisionEtrangereSectionPrefillRules } from './dip-be-reconnaissance-decision-etrangere-section-prefill-rules';

interface NatureOption { value: NatureDecision; label: string; }

/**
 * SF-223-08 : composant Angular standalone pour l'outil décisionnel
 * "Reconnaissance / exequatur d'une décision familiale étrangère (Belgique —
 * DIP)" (`dip-be-reconnaissance-decision-etrangere`). BELGIQUE uniquement (CDIP
 * art. 22-27 ; art. 21 Const. / CC art. 161 — à vérifier par avocat belge).
 *
 * 1 outil = 1 situation « reconnaissance / exequatur d'une décision étrangère
 * déjà rendue + mariage religieux non-civil ». DISTINCT de la détermination de
 * la loi applicable (`dip-be-loi-applicable-famille`, SF-223-07) ET de la
 * reconnaissance d'un mariage / divorce valablement célébré à l'étranger
 * (`mariage-etranger-be-reconnaissance`, F-217).
 *
 * Pré-fill IA F-246 : nature + pays + date pré-remplis si factualisables
 * (sous-objet `dip_reconnaissance_decision_be_detection`). Aucune citation
 * jurisprudentielle BE (F-JU-04 parké — silence > erreur). OnPush +
 * ChangeDetectorRef.markForCheck() dans next/error des subscribe (mémoire
 * `feedback_onpush_subscribe_markforcheck`).
 */
@Component({
  selector: 'app-dip-be-reconnaissance-decision-etrangere-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatSelectModule, MatCheckboxModule,
    MatProgressSpinnerModule,
    LegalCitationsPipe,
    ToolJurisprudenceCitationsComponent,
  ],
  templateUrl: './dip-be-reconnaissance-decision-etrangere-section.component.html',
  styleUrl: './dip-be-reconnaissance-decision-etrangere-section.component.scss',
})
export class DipBeReconnaissanceDecisionEtrangereSectionComponent implements OnInit, OnChanges {
  // F-JU-03 — citations jurisprudentielles F-JU-01 (BE parké F-JU-04 : aucune en V1).
  protected readonly toolIdForJurisprudence = 'dip-be-reconnaissance-decision-etrangere';
  protected readonly brancheActiveForJurisprudence = 'default';

  static readonly TOOL_LABEL = 'RECONNAISSANCE / EXEQUATUR — DÉCISION FAMILIALE ÉTRANGÈRE (BELGIQUE)';
  static readonly TOOL_ICON = 'gavel';

  /**
   * SF-223-08 — pré-fill F-246 : nature + pays + date si factualisables.
   * Délègue au helper partagé (parité runtime/static — garde-fou
   * `prefill-count-integrity`).
   */
  static getPrefillCount(input: {
    aiData?: unknown;
    procedureChecks?: unknown[];
    aiQuestions?: unknown[];
    piecesManquantes?: unknown[];
    triggerEvents?: unknown[];
    workspaceCountry?: string;
  }): number {
    return DipBeReconnaissanceDecisionEtrangereSectionPrefillRules.computePrefillCount(input);
  }

  static readonly NATURE_OPTIONS: readonly NatureOption[] = [
    { value: 'JUGEMENT_ETRANGER_HORS_UE', label: 'Jugement étranger hors UE (CDIP art. 22-25)' },
    { value: 'MARIAGE_RELIGIEUX_NON_CIVIL', label: 'Mariage religieux non précédé d\'un mariage civil (art. 21 Const. / CC 161)' },
  ];
  readonly natureOptions = DipBeReconnaissanceDecisionEtrangereSectionComponent.NATURE_OPTIONS;

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
  result = signal<DipBeReconnaissanceDecisionEtrangereResponse | null>(null);

  // --- Form fields ---
  natureDecision = signal<NatureDecision | null>(null);
  paysOrigine = signal<string | null>(null);
  dateDecision = signal<string | null>(null);
  decisionDefinitive = signal<boolean>(false);
  droitsDefenseRespectes = signal<boolean>(false);
  conformiteOrdrePublicBelge = signal<boolean>(true);
  absenceFraude = signal<boolean>(false);
  mariageCivilPrealable = signal<boolean>(false);

  constructor(
    private service: DipBeReconnaissanceDecisionEtrangereService,
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
   * F-246 — pré-remplit nature + pays + date depuis le sous-objet IA
   * `dip_reconnaissance_decision_be_detection` (clés camelCase plates,
   * @JsonUnwrapped). Les booleans de fond restent à apprécier par l'avocat.
   */
  private prefillFromAi(): void {
    if (this.workspaceCountry !== 'BELGIQUE') {
      this.cdr.markForCheck();
      return;
    }
    const ai = this.aiData as (FamilleExtractedData & {
      dipReconnaissanceNatureDetectee?: string | null;
      dipReconnaissancePaysDetecte?: string | null;
      dipReconnaissanceDateDetectee?: string | null;
    }) | null | undefined;
    if (!ai) {
      this.cdr.markForCheck();
      return;
    }
    const nature = ai.dipReconnaissanceNatureDetectee;
    if (nature === 'JUGEMENT_ETRANGER_HORS_UE' || nature === 'MARIAGE_RELIGIEUX_NON_CIVIL') {
      this.natureDecision.set(nature);
    }
    if (typeof ai.dipReconnaissancePaysDetecte === 'string'
        && /^[A-Z]{2}$/.test(ai.dipReconnaissancePaysDetecte)) {
      this.paysOrigine.set(ai.dipReconnaissancePaysDetecte);
    }
    if (typeof ai.dipReconnaissanceDateDetectee === 'string'
        && /^\d{4}-\d{2}-\d{2}$/.test(ai.dipReconnaissanceDateDetectee)) {
      this.dateDecision.set(ai.dipReconnaissanceDateDetectee);
    }
    this.cdr.markForCheck();
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  editMode(): void {
    this.showForm.set(true);
  }

  /** True si le cas du jugement étranger est sélectionné (champs de fond visibles). */
  isJugement(): boolean {
    return this.natureDecision() === 'JUGEMENT_ETRANGER_HORS_UE';
  }

  /** True si le cas du mariage religieux non-civil est sélectionné. */
  isMariageReligieux(): boolean {
    return this.natureDecision() === 'MARIAGE_RELIGIEUX_NON_CIVIL';
  }

  /** Form valide : workspace BE + nature renseignée. */
  formValid(): boolean {
    if (this.workspaceCountry !== 'BELGIQUE') return false;
    if (!this.natureDecision()) return false;
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
    const jugement = this.isJugement();
    const request: DipBeReconnaissanceDecisionEtrangereRequest = {
      natureDecision: this.natureDecision()!,
      paysOrigine: this.iso2OrNull(this.paysOrigine()),
      dateDecision: this.dateDecision() || null,
      decisionDefinitive: jugement ? this.decisionDefinitive() : null,
      droitsDefenseRespectes: jugement ? this.droitsDefenseRespectes() : null,
      conformiteOrdrePublicBelge: this.conformiteOrdrePublicBelge(),
      absenceFraude: jugement ? this.absenceFraude() : null,
      mariageCivilPrealable: this.isMariageReligieux() ? this.mariageCivilPrealable() : null,
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.applyResult(r);
        this.calculating.set(false);
        this.snackBar.open('Reconnaissance qualifiée', 'OK', { duration: 2500 });
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

  private applyResult(r: DipBeReconnaissanceDecisionEtrangereResponse): void {
    this.result.set(r);
    this.hydrateForm(r);
    this.showForm.set(false);
  }

  private hydrateForm(r: DipBeReconnaissanceDecisionEtrangereResponse): void {
    this.natureDecision.set(r.natureDecision);
    this.paysOrigine.set(r.paysOrigine);
    this.dateDecision.set(r.dateDecision);
    this.decisionDefinitive.set(r.decisionDefinitive ?? false);
    this.droitsDefenseRespectes.set(r.droitsDefenseRespectes ?? false);
    this.conformiteOrdrePublicBelge.set(r.conformiteOrdrePublicBelge);
    this.absenceFraude.set(r.absenceFraude ?? false);
    this.mariageCivilPrealable.set(r.mariageCivilPrealable ?? false);
  }

  // ---------------------------------------------------------------------------
  // Helpers d'affichage du résultat
  // ---------------------------------------------------------------------------

  /**
   * Vert pour RECONNAISSANCE_DE_PLEIN_DROIT. Or pour EXEQUATUR_REQUIS. Rouge
   * pour RECONNAISSANCE_REFUSEE. Navy info pour QUALIFICATION_INCOMPLETE.
   */
  verdictBannerClass(verdict: DipBeReconnaissanceVerdict): string {
    switch (verdict) {
      case 'RECONNAISSANCE_DE_PLEIN_DROIT':
        return 'dip-verdict-banner dip-verdict-banner--ok';
      case 'EXEQUATUR_REQUIS':
        return 'dip-verdict-banner dip-verdict-banner--warn';
      case 'RECONNAISSANCE_REFUSEE':
        return 'dip-verdict-banner dip-verdict-banner--ko';
      case 'QUALIFICATION_INCOMPLETE':
        return 'dip-verdict-banner dip-verdict-banner--info';
    }
  }

  verdictBannerLabel(verdict: DipBeReconnaissanceVerdict): string {
    switch (verdict) {
      case 'RECONNAISSANCE_DE_PLEIN_DROIT': return 'Reconnaissance de plein droit';
      case 'EXEQUATUR_REQUIS': return 'Exequatur requis';
      case 'RECONNAISSANCE_REFUSEE': return 'Reconnaissance refusée';
      case 'QUALIFICATION_INCOMPLETE': return 'Qualification incomplète';
    }
  }

  verdictBannerIcon(verdict: DipBeReconnaissanceVerdict): string {
    switch (verdict) {
      case 'RECONNAISSANCE_DE_PLEIN_DROIT': return 'check_circle';
      case 'EXEQUATUR_REQUIS': return 'gavel';
      case 'RECONNAISSANCE_REFUSEE': return 'block';
      case 'QUALIFICATION_INCOMPLETE': return 'info';
    }
  }

  natureLabel(n: NatureDecision): string {
    return this.natureOptions.find(opt => opt.value === n)?.label ?? n;
  }
}
