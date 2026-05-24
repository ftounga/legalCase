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
import { DemissionEquivoqueService } from '../../core/services/demission-equivoque.service';
import {
  DemAnalyse,
  DemFacteurEquivocite,
  DemModeExpression,
  DemissionEquivoqueRequest,
  DemissionEquivoqueResponse,
} from '../../core/models/demission-equivoque.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { DemissionEquivoqueSectionPrefillRules } from './demission-equivoque-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-212-22 : composant Angular standalone pour l'outil décisionnel
 * « Démission — validité et caractère équivoque » — tool_id canonique
 * `F-DT-41-demission-validite-equivoque`. FRANCE uniquement (L. 1237-1 CT ;
 * Cass. soc. 09/05/2007 — volonté claire et non équivoque ; Cass. soc.
 * constante — requalification possible en prise d'acte de la rupture aux
 * torts de l'employeur).
 *
 * Verdict 3 niveaux :
 *  - VOLONTE_CLAIRE (vert — démission régulière, pas d'équivocité)
 *  - DEMISSION_EQUIVOQUE (rouge — éléments d'équivocité caractérisés)
 *  - RETRACTATION_POSSIBLE (or — rétractation rapide ou délai favorable)
 *
 * Pré-remplissage IA : désactivé en SF-212-22 (sous-objet backend non
 * disponible — limite JVM 255 du record TravailExtractedData). Pattern
 * aligné sur SF-212-17 F-DT-43. À résorber par SF de rattrapage.
 */
@Component({
  selector: 'app-demission-equivoque-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatSelectModule, MatSlideToggleModule,
    MatProgressSpinnerModule,
    LegalCitationsPipe,
    ToolJurisprudenceCitationsComponent,
  ],
  templateUrl: './demission-equivoque-section.component.html',
  styleUrl: './demission-equivoque-section.component.scss',
})
export class DemissionEquivoqueSectionComponent implements OnInit, OnChanges {
  // F-JU-03 — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-DT-41-demission-validite-equivoque';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommée par le panel pour la card.
  static readonly TOOL_LABEL = 'DÉMISSION — VALIDITÉ';
  static readonly TOOL_ICON = 'help_outline';

  /**
   * Délégué au helper partagé — toujours 0 en SF-212-22 (sous-objet IA
   * backend non disponible). À brancher dès que la SF de rattrapage aura
   * livré `demissionEquivoqueDetail` dans `TravailExtractedData`.
   */
  static getPrefillCount(input: {
    aiData?: TravailExtractedData | null;
    procedureChecks?: unknown[];
    aiQuestions?: unknown[];
    piecesManquantes?: unknown[];
    triggerEvents?: unknown[];
    workspaceCountry?: string;
  }): number {
    return DemissionEquivoqueSectionPrefillRules.computePrefillCount({
      aiData: input.aiData,
      workspaceCountry: input.workspaceCountry,
    });
  }

  @Input() caseFileId!: string;
  @Input() forceExpanded = false;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';
  @Input() aiData?: TravailExtractedData | null;
  /** Mode simulateur autonome (hors dossier) — coupe refresh dashboard. */
  @Input() standaloneMode = false;

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<DemissionEquivoqueResponse | null>(null);

  // --- Form fields (7 champs du contrat backend) ---
  modeExpressionDemission = signal<DemModeExpression>('ECRIT_FORMEL');
  contexteAltercation = signal<boolean>(false);
  pressionOuMenace = signal<boolean>(false);
  retractationDansDelai = signal<boolean>(false);
  delaiRetractationJours = signal<number | null>(null);
  manquementsEmployeurContemporains = signal<boolean>(false);
  etatEmotionnelPerturbe = signal<boolean>(false);

  readonly modeOptions: ReadonlyArray<{ value: DemModeExpression; label: string }> = [
    { value: 'ECRIT_FORMEL', label: 'Écrit formel (LRAR, courrier recommandé)' },
    { value: 'EMAIL', label: 'E-mail' },
    { value: 'SMS', label: 'SMS' },
    { value: 'ORAL', label: 'Oral' },
    { value: 'AUTRE', label: 'Autre' },
  ];

  constructor(
    private service: DemissionEquivoqueService,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef,
    @Optional() private dashboardRefresh: CaseDashboardRefreshService | null,
  ) {}

  ngOnInit(): void {
    if (this.forceExpanded) this.collapsed.set(false);
    if (this.workspaceCountry === 'FRANCE') {
      this.load();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['forceExpanded'] && this.forceExpanded) this.collapsed.set(false);
    // Pas de pré-fill IA — sous-objet backend non disponible (SF-212-21).
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  editMode(): void {
    this.showForm.set(true);
  }

  /** Form valide : FRANCE + mode renseigné + délai (si rétractation) >= 0. */
  formValid(): boolean {
    if (this.workspaceCountry !== 'FRANCE') return false;
    if (!this.modeExpressionDemission()) return false;
    if (this.retractationDansDelai()) {
      const d = this.delaiRetractationJours();
      if (d !== null && d < 0) return false;
    }
    return true;
  }

  // --- Handlers ---

  onModeChange(value: DemModeExpression): void {
    this.modeExpressionDemission.set(value);
  }

  onAltercationChange(value: boolean): void {
    this.contexteAltercation.set(value);
  }

  onPressionChange(value: boolean): void {
    this.pressionOuMenace.set(value);
  }

  onRetractationChange(value: boolean): void {
    this.retractationDansDelai.set(value);
    if (!value) {
      this.delaiRetractationJours.set(null);
    }
  }

  onDelaiChange(value: number | string | null): void {
    const num = this.parseNumber(value);
    if (num === null) {
      this.delaiRetractationJours.set(null);
    } else {
      this.delaiRetractationJours.set(num >= 0 ? Math.trunc(num) : 0);
    }
  }

  onManquementsChange(value: boolean): void {
    this.manquementsEmployeurContemporains.set(value);
  }

  onEtatPerturbeChange(value: boolean): void {
    this.etatEmotionnelPerturbe.set(value);
  }

  private parseNumber(value: number | string | null): number | null {
    if (value === null || value === '' || value === undefined) return null;
    const num = typeof value === 'number' ? value : Number.parseFloat(String(value));
    return Number.isFinite(num) ? num : null;
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request: DemissionEquivoqueRequest = {
      modeExpressionDemission: this.modeExpressionDemission(),
      contexteAltercation: this.contexteAltercation(),
      pressionOuMenace: this.pressionOuMenace(),
      retractationDansDelai: this.retractationDansDelai(),
      delaiRetractationJours: this.delaiRetractationJours(),
      manquementsEmployeurContemporains: this.manquementsEmployeurContemporains(),
      etatEmotionnelPerturbe: this.etatEmotionnelPerturbe(),
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.applyResult(r);
        this.calculating.set(false);
        this.snackBar.open('Analyse de démission calculée', 'OK', { duration: 2500 });
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
        if (r) {
          this.applyResult(r);
        }
        this.loading.set(false);
        this.cdr.markForCheck();
      },
      error: () => {
        this.loading.set(false);
        this.cdr.markForCheck();
      },
    });
  }

  private applyResult(r: DemissionEquivoqueResponse): void {
    this.result.set(r);
    this.hydrateForm(r);
    this.showForm.set(false);
  }

  /**
   * Ré-injecte le snapshot d'inputs de la réponse dans les champs du
   * formulaire — sans quoi un clic « Modifier » repartirait des valeurs par
   * défaut au lieu des dernières valeurs saisies.
   */
  private hydrateForm(r: DemissionEquivoqueResponse): void {
    this.modeExpressionDemission.set(r.modeExpressionDemission ?? 'ECRIT_FORMEL');
    this.contexteAltercation.set(r.contexteAltercation ?? false);
    this.pressionOuMenace.set(r.pressionOuMenace ?? false);
    this.retractationDansDelai.set(r.retractationDansDelai ?? false);
    this.delaiRetractationJours.set(r.delaiRetractationJours ?? null);
    this.manquementsEmployeurContemporains.set(r.manquementsEmployeurContemporains ?? false);
    this.etatEmotionnelPerturbe.set(r.etatEmotionnelPerturbe ?? false);
  }

  // ---------------------------------------------------------------------------
  // Helpers d'affichage du résultat
  // ---------------------------------------------------------------------------

  /**
   * Verdict → bannière de couleur. 3 niveaux :
   *  - VOLONTE_CLAIRE → vert
   *  - DEMISSION_EQUIVOQUE → rouge (présomption forte)
   *  - RETRACTATION_POSSIBLE → or
   */
  verdictBannerClass(v: DemAnalyse): string {
    switch (v) {
      case 'VOLONTE_CLAIRE':
        return 'dem-verdict-banner dem-verdict-banner--claire';
      case 'DEMISSION_EQUIVOQUE':
        return 'dem-verdict-banner dem-verdict-banner--equivoque';
      case 'RETRACTATION_POSSIBLE':
        return 'dem-verdict-banner dem-verdict-banner--retract';
    }
  }

  verdictBannerLabel(v: DemAnalyse): string {
    switch (v) {
      case 'VOLONTE_CLAIRE': return 'Volonté claire et non équivoque';
      case 'DEMISSION_EQUIVOQUE': return 'Démission équivoque';
      case 'RETRACTATION_POSSIBLE': return 'Rétractation possible';
    }
  }

  verdictBannerIcon(v: DemAnalyse): string {
    switch (v) {
      case 'VOLONTE_CLAIRE': return 'check_circle';
      case 'DEMISSION_EQUIVOQUE': return 'error_outline';
      case 'RETRACTATION_POSSIBLE': return 'warning_amber';
    }
  }

  /** Tracker pour les facteurs d'équivocité. */
  factorTrackKey(_index: number, f: DemFacteurEquivocite): string {
    return f.code + ':' + f.libelle.slice(0, 30);
  }
}
