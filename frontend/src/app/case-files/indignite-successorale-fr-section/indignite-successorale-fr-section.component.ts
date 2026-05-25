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
import { IndigniteSuccessoraleFrService } from '../../core/services/indignite-successorale-fr.service';
import {
  IndigniteSuccessoraleRequest,
  IndigniteSuccessoraleResponse,
  MotifIndignite,
} from '../../core/models/indignite-successorale-fr.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { IndigniteSuccessoraleFrPrefillRules } from './indignite-successorale-fr-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-216-20 : composant Angular standalone pour l'outil décisionnel
 * "Indignité successorale" (F-FA-INDIGNITE-SUCCESSORALE).
 *
 * FRANCE uniquement (art. 726-729-1 Cciv + Loi n°2001-1135 du 3/12/2001 +
 * Loi n°2022-1617 du 23/12/2022).
 *
 * Pattern de référence : `adoption-internationale-fr-section` (SF-216-18) et
 * `retrait-ap-fr-section` (SF-216-12).
 *
 * Pré-fill IA (3 champs F-246) :
 *  - condamnationPrononcee       ← `condamnationPenaleSuccessionDetectee`
 *  - pardonTestamentaireDetecte  ← `pardonTestamentaireDetecte`
 *  - dateOuvertureSuccession     ← `dateOuvertureSuccessionDetectee`
 *
 * Country gate : `workspaceCountry !== 'FRANCE'` → bannière info + aucun appel.
 *
 * OnPush + ChangeDetectorRef.markForCheck() dans next/error des subscribe
 * (mémoire `feedback_onpush_subscribe_markforcheck`).
 */
@Component({
  selector: 'app-indignite-successorale-fr-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatSelectModule, MatCheckboxModule,
    MatProgressSpinnerModule,
    LegalCitationsPipe,
    ToolJurisprudenceCitationsComponent,
  ],
  templateUrl: './indignite-successorale-fr-section.component.html',
  styleUrl: './indignite-successorale-fr-section.component.scss',
})
export class IndigniteSuccessoraleFrSectionComponent implements OnInit, OnChanges {
  // F-JU-03 SF-JU-03-99d — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-FA-INDIGNITE-SUCCESSORALE';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommée par le panel pour la card.
  static readonly TOOL_LABEL = 'INDIGNITE SUCCESSORALE';
  static readonly TOOL_ICON = 'gavel';

  /**
   * SF-216-20 — délégué au helper partagé (parité stricte avec prefillFromAi).
   */
  static getPrefillCount(input: {
    aiData?: FamilleExtractedData | null;
    workspaceCountry?: string;
  }): number {
    return IndigniteSuccessoraleFrPrefillRules.computePrefillCount({
      aiData: input.aiData,
      workspaceCountry: input.workspaceCountry,
    });
  }

  @Input() caseFileId!: string;
  @Input() forceExpanded = false;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';
  @Input() aiData?: FamilleExtractedData | null;
  /** Mode simulateur autonome (hors dossier) — coupe le refresh dashboard. */
  @Input() standaloneMode = false;

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<IndigniteSuccessoraleResponse | null>(null);

  // --- Form fields (6 champs contrat backend SF-216-19) ---
  motifIndignite = signal<MotifIndignite | null>(null);
  condamnationPrononcee = signal<boolean | null>(null);
  indigniteJudiciaireDemandee = signal<boolean>(false);
  pardonTestamentaireDetecte = signal<boolean | null>(null);
  dateOuvertureSuccession = signal<string | null>(null);
  nbCoheritiersRestants = signal<number | null>(null);

  /** Options select motif d'indignité. */
  readonly motifsIndignite: ReadonlyArray<{ value: MotifIndignite; label: string }> = [
    { value: 'MEURTRE', label: 'Meurtre du défunt (art. 726 Cciv)' },
    { value: 'MEURTRE_TENTE', label: 'Tentative de meurtre du défunt (art. 726 Cciv)' },
    { value: 'VIOLENCES_GRAVES', label: 'Violences graves / actes de barbarie (art. 727 — loi 2022-1617)' },
    { value: 'TEMOIGNAGE_FAUX', label: 'Témoignage faux en justice contre le défunt (art. 727)' },
    { value: 'OBSTRUCTION_TESTAMENT', label: 'Obstruction / recel du testament (art. 727)' },
    { value: 'NON_SIGNALEMENT_HOMICIDE', label: 'Non-signalement d\'un homicide sur le défunt (art. 727)' },
    { value: 'AUTRE', label: 'Autre motif (à motiver dans la requête)' },
  ];

  constructor(
    private service: IndigniteSuccessoraleFrService,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef,
    @Optional() private dashboardRefresh: CaseDashboardRefreshService | null,
  ) {}

  ngOnInit(): void {
    if (this.forceExpanded) this.collapsed.set(false);
    if (this.workspaceCountry === 'FRANCE') {
      this.applyPrefillFromAi();
      this.load();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['forceExpanded'] && this.forceExpanded) this.collapsed.set(false);
    if (changes['aiData'] && this.workspaceCountry === 'FRANCE') {
      this.applyPrefillFromAi();
    }
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  editMode(): void {
    this.showForm.set(true);
  }

  /**
   * Form valide : FR + motif + dateOuverture renseignée + condamnation définie.
   */
  formValid(): boolean {
    if (this.workspaceCountry !== 'FRANCE') return false;
    if (this.motifIndignite() === null) return false;
    if (this.condamnationPrononcee() === null) return false;
    const d = this.dateOuvertureSuccession();
    if (!d || d.trim() === '') return false;
    return true;
  }

  // --- Handlers ---

  onMotifChange(value: MotifIndignite | null): void {
    this.motifIndignite.set(value);
  }

  onCondamnationChange(checked: boolean): void {
    this.condamnationPrononcee.set(checked);
  }

  onIndigniteDemandeeChange(checked: boolean): void {
    this.indigniteJudiciaireDemandee.set(checked);
  }

  onPardonChange(checked: boolean): void {
    this.pardonTestamentaireDetecte.set(checked);
  }

  onDateOuvertureChange(value: string | null): void {
    this.dateOuvertureSuccession.set(value && value.trim() !== '' ? value : null);
  }

  onNbCoheritiersChange(value: number | string | null): void {
    this.nbCoheritiersRestants.set(this.parseNonNegativeInt(value));
  }

  private parseNonNegativeInt(value: number | string | null | undefined): number | null {
    if (value === null || value === undefined || value === '') return null;
    const num = typeof value === 'number' ? value : Number.parseInt(String(value), 10);
    if (!Number.isFinite(num) || num < 0) return null;
    return num;
  }

  private applyPrefillFromAi(): void {
    const v = IndigniteSuccessoraleFrPrefillRules.prefillFromAi({
      aiData: this.aiData ?? null,
      workspaceCountry: this.workspaceCountry,
    });
    if (v.condamnationPrononcee !== null && this.condamnationPrononcee() === null) {
      this.condamnationPrononcee.set(v.condamnationPrononcee);
    }
    if (v.pardonTestamentaireDetecte !== null && this.pardonTestamentaireDetecte() === null) {
      this.pardonTestamentaireDetecte.set(v.pardonTestamentaireDetecte);
    }
    if (v.dateOuvertureSuccession !== null && this.dateOuvertureSuccession() === null) {
      this.dateOuvertureSuccession.set(v.dateOuvertureSuccession);
    }
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request: IndigniteSuccessoraleRequest = {
      motifIndignite: this.motifIndignite(),
      condamnationPrononcee: this.condamnationPrononcee(),
      indigniteJudiciaireDemandee: this.indigniteJudiciaireDemandee(),
      pardonTestamentaireDetecte: this.pardonTestamentaireDetecte(),
      dateOuvertureSuccession: this.dateOuvertureSuccession(),
      nbCoheritiersRestants: this.nbCoheritiersRestants(),
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.applyResult(r);
        this.calculating.set(false);
        this.snackBar.open('Indignité successorale évaluée', 'OK', { duration: 2500 });
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
        this.loading.set(false);
        this.cdr.markForCheck();
      },
    });
  }

  private applyResult(r: IndigniteSuccessoraleResponse): void {
    this.result.set(r);
    this.showForm.set(false);
  }

  // ---------------------------------------------------------------------------
  // Helpers d'affichage
  // ---------------------------------------------------------------------------

  typeIndigniteLabel(t: string): string {
    const labels: Record<string, string> = {
      PLEIN_DROIT: 'Indignité de plein droit (art. 726)',
      JUDICIAIRE: 'Indignité judiciaire (art. 727)',
      PARDONNEE: 'Pardon testamentaire (art. 728) — neutralisée',
      AUCUNE: 'Aucune indignité caractérisée',
    };
    return labels[t] ?? t;
  }

  /** Classe CSS verdict. */
  verdictClass(): string {
    const r = this.result();
    if (!r) return '';
    if (r.typeIndignite === 'PLEIN_DROIT') return 'verdict-blocked';
    if (r.typeIndignite === 'JUDICIAIRE' && !r.delaiForclos) return 'verdict-warning';
    if (r.delaiForclos) return 'verdict-blocked';
    return 'verdict-success';
  }
}
