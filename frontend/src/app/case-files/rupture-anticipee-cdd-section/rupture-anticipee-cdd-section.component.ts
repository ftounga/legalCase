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
import { RuptureAnticipeeCddService } from '../../core/services/rupture-anticipee-cdd.service';
import {
  RacAnalyse,
  RacAuteurRupture,
  RacMotifRupture,
  RacPointAnalyse,
  RuptureAnticipeeCddRequest,
  RuptureAnticipeeCddResponse,
} from '../../core/models/rupture-anticipee-cdd.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { RuptureAnticipeeCddSectionPrefillRules } from './rupture-anticipee-cdd-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-212-18 : composant Angular standalone pour l'outil décisionnel "Rupture
 * anticipée du CDD" — tool_id canonique `F-DT-43-rupture-anticipee-cdd`.
 * FRANCE uniquement (L. 1243-1 à L. 1243-4 CT ; L. 1243-8 CT ; L. 1226-4-2 CT
 * applicable au CDD ; Cass. soc. constante).
 *
 * Pattern de référence : `mise-a-pied-disciplinaire-section` (F-DT-48,
 * SF-212-20) — formulaire avec select + entrées numériques + dates, POST de
 * calcul, verdict 3 niveaux, refresh dashboard, OnPush + markForCheck dans
 * les subscribe.
 *
 * Verdict 3 niveaux :
 *  - LEGITIME (vert — motif autorisé L. 1243-1 à L. 1243-3 CT)
 *  - ILLEGITIME_EMPLOYEUR (rouge — sanction L. 1243-4 al. 1 : salaires
 *    restants jusqu'au terme + précarité 10 %)
 *  - ILLEGITIME_SALARIE (or — sanction L. 1243-4 al. 3 : dommages-intérêts
 *    à l'employeur, montant non chiffré par l'outil)
 */
@Component({
  selector: 'app-rupture-anticipee-cdd-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatSelectModule, MatProgressSpinnerModule,
    LegalCitationsPipe,
    ToolJurisprudenceCitationsComponent,
  ],
  templateUrl: './rupture-anticipee-cdd-section.component.html',
  styleUrl: './rupture-anticipee-cdd-section.component.scss',
})
export class RuptureAnticipeeCddSectionComponent implements OnInit, OnChanges {
  // F-JU-03 — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-DT-43-rupture-anticipee-cdd';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommée par le panel pour la card.
  static readonly TOOL_LABEL = 'RUPTURE ANTICIPÉE CDD';
  static readonly TOOL_ICON = 'event_busy';

  /**
   * Délégué au helper partagé (parité stricte avec `prefillFromAi()`).
   * Retourne 0 actuellement — pas de sous-objet IA projeté côté backend pour
   * F-DT-43 (limite JVM 255 slots de TravailExtractedData saturée par les
   * vagues F-212 antérieures + SF-212-23). SF de rattrapage à prévoir.
   */
  static getPrefillCount(input: {
    aiData?: TravailExtractedData | null;
    procedureChecks?: unknown[];
    aiQuestions?: unknown[];
    piecesManquantes?: unknown[];
    triggerEvents?: unknown[];
    workspaceCountry?: string;
  }): number {
    return RuptureAnticipeeCddSectionPrefillRules.computePrefillCount({
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
  result = signal<RuptureAnticipeeCddResponse | null>(null);

  // --- Form fields (6 champs du contrat backend) ---
  auteurRupture = signal<RacAuteurRupture>('EMPLOYEUR');
  motifRupture = signal<RacMotifRupture>('AUTRE');
  dateTermeCdd = signal<string>('');
  dateRupture = signal<string>('');
  salaireMensuelBrutEuros = signal<number>(0);
  remunerationBruteTotaleContratEuros = signal<number>(0);

  // Provenance IA par champ pré-rempli — pas de pré-fill IA actuellement
  // pour F-DT-43 (cf. doc de classe). Signaux conservés pour parité avec le
  // pattern des autres outils décisionnels — toujours null.
  provenanceAuteur = signal<'IA' | null>(null);
  provenanceMotif = signal<'IA' | null>(null);
  provenanceDateTerme = signal<'IA' | null>(null);

  readonly auteurOptions: ReadonlyArray<{ value: RacAuteurRupture; label: string }> = [
    { value: 'EMPLOYEUR', label: 'Employeur' },
    { value: 'SALARIE', label: 'Salarié' },
  ];

  readonly motifOptions: ReadonlyArray<{ value: RacMotifRupture; label: string }> = [
    { value: 'ACCORD_PARTIES', label: 'Accord des parties (L. 1243-3 CT)' },
    { value: 'FAUTE_GRAVE', label: 'Faute grave du salarié (L. 1243-1 CT)' },
    { value: 'FORCE_MAJEURE', label: 'Force majeure (L. 1243-1 CT)' },
    { value: 'INAPTITUDE', label: 'Inaptitude médicale (L. 1226-4-2 CT)' },
    { value: 'CDI_EMBAUCHE', label: 'Embauche en CDI ailleurs — salarié (L. 1243-2 CT)' },
    { value: 'AUTRE', label: 'Aucun motif légal (rupture sans cause)' },
  ];

  constructor(
    private service: RuptureAnticipeeCddService,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef,
    @Optional() private dashboardRefresh: CaseDashboardRefreshService | null,
  ) {}

  ngOnInit(): void {
    if (this.forceExpanded) this.collapsed.set(false);
    this.prefillFromAi();
    if (this.workspaceCountry === 'FRANCE') {
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
   * Pré-fill IA (SF-212-18) — no-op actuellement : le sous-objet IA
   * `rupture_anticipee_cdd_detail` n'est pas projeté côté backend (limite JVM
   * 255 slots de TravailExtractedData saturée). Conservé pour parité avec le
   * pattern des autres outils. Le composant attend une SF de rattrapage
   * post-refactor de TravailExtractedData pour activer le pré-fill IA.
   */
  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    // No-op — cf. doc de classe.
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  editMode(): void {
    this.showForm.set(true);
  }

  /** Form valide : FRANCE + auteur + motif + dates non vides + salaires ≥ 0. */
  formValid(): boolean {
    if (this.workspaceCountry !== 'FRANCE') return false;
    if (!this.auteurRupture() || !this.motifRupture()) return false;
    if (!this.dateTermeCdd() || !this.dateRupture()) return false;
    if (this.salaireMensuelBrutEuros() < 0 || this.remunerationBruteTotaleContratEuros() < 0) return false;
    if (!Number.isFinite(this.salaireMensuelBrutEuros())) return false;
    if (!Number.isFinite(this.remunerationBruteTotaleContratEuros())) return false;
    return true;
  }

  // --- Handlers — toute modification manuelle efface le badge IA du champ ---

  onAuteurChange(value: RacAuteurRupture): void {
    this.auteurRupture.set(value);
    this.provenanceAuteur.set(null);
  }

  onMotifChange(value: RacMotifRupture): void {
    this.motifRupture.set(value);
    this.provenanceMotif.set(null);
  }

  onDateTermeChange(value: string): void {
    this.dateTermeCdd.set(value || '');
    this.provenanceDateTerme.set(null);
  }

  onDateRuptureChange(value: string): void {
    this.dateRupture.set(value || '');
  }

  onSalaireMensuelChange(value: number | string | null): void {
    if (value === null || value === '' || value === undefined) {
      this.salaireMensuelBrutEuros.set(0);
    } else {
      const num = typeof value === 'number' ? value : Number.parseFloat(String(value));
      this.salaireMensuelBrutEuros.set(Number.isFinite(num) && num >= 0 ? num : 0);
    }
  }

  onRemunerationTotaleChange(value: number | string | null): void {
    if (value === null || value === '' || value === undefined) {
      this.remunerationBruteTotaleContratEuros.set(0);
    } else {
      const num = typeof value === 'number' ? value : Number.parseFloat(String(value));
      this.remunerationBruteTotaleContratEuros.set(Number.isFinite(num) && num >= 0 ? num : 0);
    }
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request: RuptureAnticipeeCddRequest = {
      auteurRupture: this.auteurRupture(),
      motifRupture: this.motifRupture(),
      dateTermeCdd: this.dateTermeCdd(),
      dateRupture: this.dateRupture(),
      salaireMensuelBrutEuros: this.salaireMensuelBrutEuros(),
      remunerationBruteTotaleContratEuros: this.remunerationBruteTotaleContratEuros(),
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.applyResult(r);
        this.calculating.set(false);
        this.snackBar.open('Analyse de la rupture anticipée calculée', 'OK', { duration: 2500 });
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

  private applyResult(r: RuptureAnticipeeCddResponse): void {
    this.result.set(r);
    this.hydrateForm(r);
    this.showForm.set(false);
  }

  /**
   * Ré-injecte le snapshot d'inputs de la réponse dans les champs du
   * formulaire — sans quoi un clic « Modifier » repartirait des valeurs par
   * défaut au lieu des dernières valeurs saisies.
   */
  private hydrateForm(r: RuptureAnticipeeCddResponse): void {
    this.auteurRupture.set(r.auteurRupture ?? 'EMPLOYEUR');
    this.motifRupture.set(r.motifRupture ?? 'AUTRE');
    this.dateTermeCdd.set(r.dateTermeCdd ?? '');
    this.dateRupture.set(r.dateRupture ?? '');
    this.salaireMensuelBrutEuros.set(r.salaireMensuelBrutEuros ?? 0);
    this.remunerationBruteTotaleContratEuros.set(r.remunerationBruteTotaleContratEuros ?? 0);
    // Valeurs persistées = saisie avocat — jamais de badge IA.
    this.provenanceAuteur.set(null);
    this.provenanceMotif.set(null);
    this.provenanceDateTerme.set(null);
  }

  // ---------------------------------------------------------------------------
  // Helpers d'affichage du résultat
  // ---------------------------------------------------------------------------

  /**
   * Verdict → bannière de couleur. 3 niveaux :
   *  - LEGITIME → vert (motif autorisé)
   *  - ILLEGITIME_EMPLOYEUR → rouge (salaires restants + précarité dus)
   *  - ILLEGITIME_SALARIE → or (dommages-intérêts à l'employeur)
   */
  verdictBannerClass(v: RacAnalyse): string {
    switch (v) {
      case 'LEGITIME':
        return 'rac-verdict-banner rac-verdict-banner--legitime';
      case 'ILLEGITIME_EMPLOYEUR':
        return 'rac-verdict-banner rac-verdict-banner--employeur';
      case 'ILLEGITIME_SALARIE':
        return 'rac-verdict-banner rac-verdict-banner--salarie';
    }
  }

  verdictBannerLabel(v: RacAnalyse): string {
    switch (v) {
      case 'LEGITIME': return 'Rupture anticipée légitime';
      case 'ILLEGITIME_EMPLOYEUR': return 'Rupture irrégulière par l\'employeur';
      case 'ILLEGITIME_SALARIE': return 'Rupture irrégulière par le salarié';
    }
  }

  verdictBannerIcon(v: RacAnalyse): string {
    switch (v) {
      case 'LEGITIME': return 'check_circle';
      case 'ILLEGITIME_EMPLOYEUR': return 'error_outline';
      case 'ILLEGITIME_SALARIE': return 'warning_amber';
    }
  }

  /** Tracker pour les points d'analyse. */
  pointTrackKey(_index: number, p: RacPointAnalyse): string {
    return p.code + ':' + p.libelle.slice(0, 30);
  }
}
