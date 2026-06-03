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
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { RegimeAlgerienBeService } from '../../core/services/regime-algerien-be.service';
import {
  RegimeAlgerienBeRequest,
  RegimeAlgerienBeResponse,
  RegimeAlgerienBeVerdict,
  NatureActe,
  LienRattachement,
} from '../../core/models/regime-algerien-be.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';
import { RegimeAlgerienBeSectionPrefillRules } from './regime-algerien-be-section-prefill-rules';

interface NatureOption { value: NatureActe; label: string; }
interface RattachementOption { value: LienRattachement; label: string; }

/**
 * SF-223-05 : composant Angular standalone pour l'outil décisionnel "Régime
 * algérien — reconnaissance mariage / talaq / dot (Belgique)"
 * (`regime-algerien-be`). BELGIQUE uniquement (corridor algérien — CDIP,
 * Convention algéro-belge — à vérifier par avocat belge).
 *
 * 1 outil = 1 situation « sort en Belgique d'un mariage / talaq / dot relevant
 * du droit algérien ». Cadre la SPÉCIFICITÉ ALGÉRIENNE → DISTINCT de l'outil
 * GÉNÉRAL `mariage-etranger-be-reconnaissance` (F-217), vers lequel il renvoie
 * pour la mécanique CDIP générale.
 *
 * Pré-fill IA F-246 : nature de l'acte + date + montant de la dot pré-remplis si
 * factualisables (sous-objet `regime_algerien_be_detection`). Aucune citation
 * jurisprudentielle BE (F-JU-04 parké — silence > erreur). OnPush +
 * ChangeDetectorRef.markForCheck() dans next/error des subscribe (mémoire
 * `feedback_onpush_subscribe_markforcheck`).
 */
@Component({
  selector: 'app-regime-algerien-be-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatSlideToggleModule, MatProgressSpinnerModule,
    LegalCitationsPipe,
    ToolJurisprudenceCitationsComponent,
  ],
  templateUrl: './regime-algerien-be-section.component.html',
  styleUrl: './regime-algerien-be-section.component.scss',
})
export class RegimeAlgerienBeSectionComponent implements OnInit, OnChanges {
  // F-JU-03 — citations jurisprudentielles F-JU-01 (BE parké F-JU-04 : aucune en V1).
  protected readonly toolIdForJurisprudence = 'regime-algerien-be';
  protected readonly brancheActiveForJurisprudence = 'default';

  static readonly TOOL_LABEL = 'RÉGIME ALGÉRIEN (BELGIQUE)';
  static readonly TOOL_ICON = 'family_restroom';

  /**
   * SF-223-05 — pré-fill F-246 : nature de l'acte + date + montant de la dot si
   * factualisables. Délègue au helper partagé (parité runtime/static —
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
    return RegimeAlgerienBeSectionPrefillRules.computePrefillCount(input);
  }

  static readonly NATURE_OPTIONS: readonly NatureOption[] = [
    { value: 'MARIAGE_ALGERIEN', label: 'Mariage célébré selon le droit algérien' },
    { value: 'TALAQ_ALGERIEN', label: 'Talaq (répudiation) selon le droit algérien' },
    { value: 'DOT_MAHR', label: 'Dot (mahr) — effet patrimonial' },
  ];
  readonly natureOptions = RegimeAlgerienBeSectionComponent.NATURE_OPTIONS;

  static readonly RATTACHEMENT_OPTIONS: readonly RattachementOption[] = [
    { value: 'RESIDENCE', label: 'Résidence en Belgique' },
    { value: 'NATIONALITE', label: 'Nationalité belge' },
    { value: 'AUCUN', label: 'Aucun rattachement à la Belgique' },
  ];
  readonly rattachementOptions = RegimeAlgerienBeSectionComponent.RATTACHEMENT_OPTIONS;

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
  result = signal<RegimeAlgerienBeResponse | null>(null);

  // --- Form fields ---
  natureActe = signal<NatureActe | null>(null);
  dateActe = signal<string | null>(null);
  consentementEpouxEpouse = signal<boolean>(false);
  dotMahrPrevue = signal<boolean>(false);
  montantDotConnu = signal<number | null>(null);
  conventionAlgeroBelgeInvoquee = signal<boolean>(false);
  lienRattachementBelgique = signal<LienRattachement | null>(null);

  constructor(
    private service: RegimeAlgerienBeService,
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
   * F-246 — pré-remplit nature de l'acte + date + montant de la dot depuis le
   * sous-objet IA `regime_algerien_be_detection` (clés camelCase plates,
   * @JsonUnwrapped). Aucun autre champ n'est factualisable de manière stable
   * en V1.
   */
  private prefillFromAi(): void {
    if (this.workspaceCountry !== 'BELGIQUE') {
      this.cdr.markForCheck();
      return;
    }
    const ai = this.aiData as (FamilleExtractedData & {
      regimeAlgerienBeNatureActeDetecte?: string | null;
      regimeAlgerienBeDateActeDetectee?: string | null;
      regimeAlgerienBeMontantDotDetecte?: string | null;
    }) | null | undefined;
    if (!ai) {
      this.cdr.markForCheck();
      return;
    }
    const nature = ai.regimeAlgerienBeNatureActeDetecte;
    if (nature === 'MARIAGE_ALGERIEN' || nature === 'TALAQ_ALGERIEN' || nature === 'DOT_MAHR') {
      this.natureActe.set(nature);
    }
    const date = ai.regimeAlgerienBeDateActeDetectee;
    if (typeof date === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(date)) {
      this.dateActe.set(date);
    }
    const montant = ai.regimeAlgerienBeMontantDotDetecte;
    if (typeof montant === 'string' && montant.trim() !== ''
        && !Number.isNaN(Number(montant)) && Number(montant) >= 0) {
      this.montantDotConnu.set(Number(montant));
    }
    this.cdr.markForCheck();
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  editMode(): void {
    this.showForm.set(true);
  }

  /** Form valide : workspace BE + nature + lien de rattachement renseignés. */
  formValid(): boolean {
    if (this.workspaceCountry !== 'BELGIQUE') return false;
    if (!this.natureActe()) return false;
    if (!this.lienRattachementBelgique()) return false;
    return true;
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request: RegimeAlgerienBeRequest = {
      natureActe: this.natureActe()!,
      dateActe: this.dateActe() || null,
      consentementEpouxEpouse: this.consentementEpouxEpouse(),
      dotMahrPrevue: this.dotMahrPrevue(),
      montantDotConnu: this.montantDotConnu(),
      conventionAlgeroBelgeInvoquee: this.conventionAlgeroBelgeInvoquee(),
      lienRattachementBelgique: this.lienRattachementBelgique()!,
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.applyResult(r);
        this.calculating.set(false);
        this.snackBar.open('Analyse du régime algérien calculée', 'OK', { duration: 2500 });
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

  private applyResult(r: RegimeAlgerienBeResponse): void {
    this.result.set(r);
    this.hydrateForm(r);
    this.showForm.set(false);
  }

  private hydrateForm(r: RegimeAlgerienBeResponse): void {
    this.natureActe.set(r.natureActe);
    this.dateActe.set(r.dateActe);
    this.consentementEpouxEpouse.set(r.consentementEpouxEpouse ?? false);
    this.dotMahrPrevue.set(r.dotMahrPrevue ?? false);
    this.montantDotConnu.set(r.montantDotConnu);
    this.conventionAlgeroBelgeInvoquee.set(r.conventionAlgeroBelgeInvoquee ?? false);
    this.lienRattachementBelgique.set(r.lienRattachementBelgique);
  }

  // ---------------------------------------------------------------------------
  // Helpers d'affichage du résultat
  // ---------------------------------------------------------------------------

  /**
   * Vert pour RECONNAISSANCE_DE_PLEIN_DROIT. Or pour
   * RECONNAISSANCE_SOUS_CONDITIONS. Rouge pour
   * RECONNAISSANCE_REFUSEE_ORDRE_PUBLIC (atteinte à l'ordre public — refus
   * réel). Navy info pour QUALIFICATION_INCOMPLETE.
   */
  verdictBannerClass(verdict: RegimeAlgerienBeVerdict): string {
    switch (verdict) {
      case 'RECONNAISSANCE_DE_PLEIN_DROIT':
        return 'ralg-verdict-banner ralg-verdict-banner--ok';
      case 'RECONNAISSANCE_SOUS_CONDITIONS':
        return 'ralg-verdict-banner ralg-verdict-banner--warn';
      case 'RECONNAISSANCE_REFUSEE_ORDRE_PUBLIC':
        return 'ralg-verdict-banner ralg-verdict-banner--danger';
      case 'QUALIFICATION_INCOMPLETE':
        return 'ralg-verdict-banner ralg-verdict-banner--info';
    }
  }

  verdictBannerLabel(verdict: RegimeAlgerienBeVerdict): string {
    switch (verdict) {
      case 'RECONNAISSANCE_DE_PLEIN_DROIT': return 'Reconnaissance de plein droit';
      case 'RECONNAISSANCE_SOUS_CONDITIONS': return 'Reconnaissance sous conditions';
      case 'RECONNAISSANCE_REFUSEE_ORDRE_PUBLIC': return 'Reconnaissance refusée (ordre public)';
      case 'QUALIFICATION_INCOMPLETE': return 'Qualification incomplète';
    }
  }

  verdictBannerIcon(verdict: RegimeAlgerienBeVerdict): string {
    switch (verdict) {
      case 'RECONNAISSANCE_DE_PLEIN_DROIT': return 'check_circle';
      case 'RECONNAISSANCE_SOUS_CONDITIONS': return 'rule';
      case 'RECONNAISSANCE_REFUSEE_ORDRE_PUBLIC': return 'gavel';
      case 'QUALIFICATION_INCOMPLETE': return 'info';
    }
  }

  natureLabel(o: NatureActe): string {
    return this.natureOptions.find(opt => opt.value === o)?.label ?? o;
  }

  rattachementLabel(o: LienRattachement): string {
    return this.rattachementOptions.find(opt => opt.value === o)?.label ?? o;
  }
}
