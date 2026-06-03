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
import { RegimeBeSeparationBiensService } from '../../core/services/regime-be-separation-biens.service';
import {
  RegimeBeSeparationBiensRequest,
  RegimeBeSeparationBiensResponse,
  RegimeBeSeparationBiensVerdict,
  VarianteRegime,
} from '../../core/models/regime-be-separation-biens.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';
import { RegimeBeSeparationBiensSectionPrefillRules } from './regime-be-separation-biens-section-prefill-rules';

interface VarianteOption { value: VarianteRegime; label: string; }

/**
 * SF-223-06 : composant Angular standalone pour l'outil décisionnel "Régime de
 * séparation de biens (Belgique)" (`regime-be-separation-biens`). BELGIQUE
 * uniquement (Livre 3 CC ; loi du 22/07/2018 — à vérifier par avocat belge).
 *
 * 1 outil = 1 situation « régime de séparation de biens BE et ses variantes ».
 * Cadre la SPÉCIFICITÉ BE post-2018 → DISTINCT du régime de COMMUNAUTÉ légale BE
 * (`regime-mat-be-communaute-legale`, F-217) et de la liquidation-partage
 * notariale chiffrée (`liquidation-partage-be`, F-217), vers laquelle il
 * renvoie pour tout chiffrage.
 *
 * Pré-fill IA F-246 : variante + date du contrat + patrimoines pré-remplis si
 * factualisables (sous-objet `regime_separation_biens_be_detection`). Aucune
 * citation jurisprudentielle BE (F-JU-04 parké — silence > erreur). OnPush +
 * ChangeDetectorRef.markForCheck() dans next/error des subscribe (mémoire
 * `feedback_onpush_subscribe_markforcheck`).
 */
@Component({
  selector: 'app-regime-be-separation-biens-section',
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
  templateUrl: './regime-be-separation-biens-section.component.html',
  styleUrl: './regime-be-separation-biens-section.component.scss',
})
export class RegimeBeSeparationBiensSectionComponent implements OnInit, OnChanges {
  // F-JU-03 — citations jurisprudentielles F-JU-01 (BE parké F-JU-04 : aucune en V1).
  protected readonly toolIdForJurisprudence = 'regime-be-separation-biens';
  protected readonly brancheActiveForJurisprudence = 'default';

  static readonly TOOL_LABEL = 'SÉPARATION DE BIENS (BELGIQUE)';
  static readonly TOOL_ICON = 'family_restroom';

  /**
   * SF-223-06 — pré-fill F-246 : variante + date du contrat + patrimoines si
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
    return RegimeBeSeparationBiensSectionPrefillRules.computePrefillCount(input);
  }

  static readonly VARIANTE_OPTIONS: readonly VarianteOption[] = [
    { value: 'SEPARATION_PURE', label: 'Séparation de biens pure' },
    { value: 'SEPARATION_AVEC_SOCIETE_ACQUETS', label: 'Séparation avec société d\'acquêts' },
    { value: 'SEPARATION_AVEC_PARTICIPATION_ACQUETS', label: 'Séparation avec participation aux acquêts' },
  ];
  readonly varianteOptions = RegimeBeSeparationBiensSectionComponent.VARIANTE_OPTIONS;

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
  result = signal<RegimeBeSeparationBiensResponse | null>(null);

  // --- Form fields ---
  varianteRegime = signal<VarianteRegime | null>(null);
  contratMariageNotarie = signal<boolean>(false);
  clauseParticipationPrevue = signal<boolean>(false);
  disproportionPatrimonialeAllegee = signal<boolean>(false);
  dateContrat = signal<string | null>(null);
  patrimoinePropreEpoux1Eur = signal<number | null>(null);
  patrimoinePropreEpoux2Eur = signal<number | null>(null);

  constructor(
    private service: RegimeBeSeparationBiensService,
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
   * F-246 — pré-remplit variante + date du contrat + patrimoines depuis le
   * sous-objet IA `regime_separation_biens_be_detection` (clés camelCase
   * plates, @JsonUnwrapped). Aucun autre champ n'est factualisable de manière
   * stable en V1.
   */
  private prefillFromAi(): void {
    if (this.workspaceCountry !== 'BELGIQUE') {
      this.cdr.markForCheck();
      return;
    }
    const ai = this.aiData as (FamilleExtractedData & {
      regimeSeparationBiensBeVarianteDetectee?: string | null;
      regimeSeparationBiensBeDateContratDetectee?: string | null;
      regimeSeparationBiensBePatrimoineEpoux1Detecte?: string | null;
      regimeSeparationBiensBePatrimoineEpoux2Detecte?: string | null;
    }) | null | undefined;
    if (!ai) {
      this.cdr.markForCheck();
      return;
    }
    const variante = ai.regimeSeparationBiensBeVarianteDetectee;
    if (variante === 'SEPARATION_PURE'
        || variante === 'SEPARATION_AVEC_SOCIETE_ACQUETS'
        || variante === 'SEPARATION_AVEC_PARTICIPATION_ACQUETS') {
      this.varianteRegime.set(variante);
    }
    const date = ai.regimeSeparationBiensBeDateContratDetectee;
    if (typeof date === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(date)) {
      this.dateContrat.set(date);
    }
    this.setMontant(ai.regimeSeparationBiensBePatrimoineEpoux1Detecte, this.patrimoinePropreEpoux1Eur);
    this.setMontant(ai.regimeSeparationBiensBePatrimoineEpoux2Detecte, this.patrimoinePropreEpoux2Eur);
    this.cdr.markForCheck();
  }

  private setMontant(raw: string | null | undefined, target: { set: (v: number) => void }): void {
    if (typeof raw === 'string' && raw.trim() !== ''
        && !Number.isNaN(Number(raw)) && Number(raw) >= 0) {
      target.set(Number(raw));
    }
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  editMode(): void {
    this.showForm.set(true);
  }

  /** Form valide : workspace BE + variante renseignée. */
  formValid(): boolean {
    if (this.workspaceCountry !== 'BELGIQUE') return false;
    if (!this.varianteRegime()) return false;
    return true;
  }

  calculate(): void {
    if (!this.formValid()) return;
    const variante = this.varianteRegime()!;
    const request: RegimeBeSeparationBiensRequest = {
      varianteRegime: variante,
      contratMariageNotarie: this.contratMariageNotarie(),
      clauseParticipationPrevue:
        variante === 'SEPARATION_AVEC_PARTICIPATION_ACQUETS'
          ? this.clauseParticipationPrevue()
          : null,
      disproportionPatrimonialeAllegee: this.disproportionPatrimonialeAllegee(),
      dateContrat: this.dateContrat() || null,
      patrimoinePropreEpoux1Eur: this.patrimoinePropreEpoux1Eur(),
      patrimoinePropreEpoux2Eur: this.patrimoinePropreEpoux2Eur(),
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.applyResult(r);
        this.calculating.set(false);
        this.snackBar.open('Analyse du régime de séparation de biens calculée', 'OK', { duration: 2500 });
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

  private applyResult(r: RegimeBeSeparationBiensResponse): void {
    this.result.set(r);
    this.hydrateForm(r);
    this.showForm.set(false);
  }

  private hydrateForm(r: RegimeBeSeparationBiensResponse): void {
    this.varianteRegime.set(r.varianteRegime);
    this.contratMariageNotarie.set(r.contratMariageNotarie ?? false);
    this.clauseParticipationPrevue.set(r.clauseParticipationPrevue ?? false);
    this.disproportionPatrimonialeAllegee.set(r.disproportionPatrimonialeAllegee ?? false);
    this.dateContrat.set(r.dateContrat);
    this.patrimoinePropreEpoux1Eur.set(r.patrimoinePropreEpoux1Eur);
    this.patrimoinePropreEpoux2Eur.set(r.patrimoinePropreEpoux2Eur);
  }

  // ---------------------------------------------------------------------------
  // Helpers d'affichage du résultat
  // ---------------------------------------------------------------------------

  /**
   * Vert pour SEPARATION_PURE_QUALIFIEE. Or pour SOCIETE_ACQUETS_QUALIFIEE et
   * PARTICIPATION_ACQUETS_QUALIFIEE (masse / créance à liquider — orientation).
   * Navy info pour QUALIFICATION_INCOMPLETE.
   */
  verdictBannerClass(verdict: RegimeBeSeparationBiensVerdict): string {
    switch (verdict) {
      case 'SEPARATION_PURE_QUALIFIEE':
        return 'rsb-verdict-banner rsb-verdict-banner--ok';
      case 'SOCIETE_ACQUETS_QUALIFIEE':
      case 'PARTICIPATION_ACQUETS_QUALIFIEE':
        return 'rsb-verdict-banner rsb-verdict-banner--warn';
      case 'QUALIFICATION_INCOMPLETE':
        return 'rsb-verdict-banner rsb-verdict-banner--info';
    }
  }

  verdictBannerLabel(verdict: RegimeBeSeparationBiensVerdict): string {
    switch (verdict) {
      case 'SEPARATION_PURE_QUALIFIEE': return 'Séparation de biens pure qualifiée';
      case 'SOCIETE_ACQUETS_QUALIFIEE': return 'Société d\'acquêts adjointe qualifiée';
      case 'PARTICIPATION_ACQUETS_QUALIFIEE': return 'Participation aux acquêts qualifiée';
      case 'QUALIFICATION_INCOMPLETE': return 'Qualification incomplète';
    }
  }

  verdictBannerIcon(verdict: RegimeBeSeparationBiensVerdict): string {
    switch (verdict) {
      case 'SEPARATION_PURE_QUALIFIEE': return 'check_circle';
      case 'SOCIETE_ACQUETS_QUALIFIEE': return 'rule';
      case 'PARTICIPATION_ACQUETS_QUALIFIEE': return 'rule';
      case 'QUALIFICATION_INCOMPLETE': return 'info';
    }
  }

  varianteLabel(o: VarianteRegime): string {
    return this.varianteOptions.find(opt => opt.value === o)?.label ?? o;
  }
}
