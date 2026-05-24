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
import { TeletravailAccordService } from '../../core/services/teletravail-accord.service';
import {
  TeletravailAnalyse,
  TeletravailCadre,
  TeletravailPointAnalyse,
  TeletravailAccordRequest,
  TeletravailAccordResponse,
} from '../../core/models/teletravail-accord.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { TeletravailAccordSectionPrefillRules } from './teletravail-accord-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-212-16 : composant Angular standalone pour l'outil décisionnel
 * "Télétravail — conformité et litige" — tool_id canonique
 * `F-DT-82-teletravail-accord`. FRANCE uniquement (L. 1222-9 à L. 1222-11
 * CT ; ANI télétravail du 26/11/2020).
 *
 * Pattern de référence : `mutation-clause-mobilite-section` (F-DT-71,
 * SF-212-14) — formulaire avec toggles + select + entrée numérique, POST
 * de calcul, verdict 3 niveaux, refresh dashboard, OnPush + markForCheck
 * dans les subscribe.
 *
 * Verdict 3 niveaux :
 *  - CONFORME (vert — dispositif respectant L. 1222-9 + ANI 2020)
 *  - NON_CONFORME (rouge — absence de cadre ou violation structurelle)
 *  - LITIGE_IDENTIFIE (or — accident à domicile, retour bureau imposé,
 *    refus invoqué comme cause)
 *
 * Alertes distinctes (UX explicite) :
 *  - alerteAccidentTravailDomicile : présomption AT L. 1222-9 al. 4
 *  - alerteRefusTeletravailLicenciement : interdit par L. 1222-9 al. 6
 */
@Component({
  selector: 'app-teletravail-accord-section',
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
  templateUrl: './teletravail-accord-section.component.html',
  styleUrl: './teletravail-accord-section.component.scss',
})
export class TeletravailAccordSectionComponent implements OnInit, OnChanges {
  // F-JU-03 — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-DT-82-teletravail-accord';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommée par le panel pour la card.
  static readonly TOOL_LABEL = 'TÉLÉTRAVAIL — CONFORMITÉ';
  static readonly TOOL_ICON = 'desktop_windows';

  /**
   * Délégué au helper partagé (parité stricte avec `prefillFromAi()`).
   * Retourne le nombre exact de champs pré-remplissables FR pour le badge tab.
   * Couvre les 7 champs `teletravail*`.
   */
  static getPrefillCount(input: {
    aiData?: TravailExtractedData | null;
    procedureChecks?: unknown[];
    aiQuestions?: unknown[];
    piecesManquantes?: unknown[];
    triggerEvents?: unknown[];
    workspaceCountry?: string;
  }): number {
    return TeletravailAccordSectionPrefillRules.computePrefillCount({
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
  result = signal<TeletravailAccordResponse | null>(null);

  // --- Form fields (7 champs du contrat backend) ---
  cadreTeletravail = signal<TeletravailCadre>('ACCORD_COLLECTIF');
  doubleVolontariatRespectee = signal<boolean>(true);
  indemniteOccupationVersee = signal<boolean>(true);
  montantIndemniteJournalierEuros = signal<number | null>(null);
  accidentDomicileDetecte = signal<boolean>(false);
  retourBureauImposeUnilateralement = signal<boolean>(false);
  refusTeletravailCauseIncrimination = signal<boolean>(false);

  // Provenance IA par champ pré-rempli — badge `auto_awesome` s'efface au
  // premier `onXxxChange()` manuel sur ce champ.
  provenanceCadre = signal<'IA' | null>(null);
  provenanceDoubleVolontariat = signal<'IA' | null>(null);
  provenanceIndemnite = signal<'IA' | null>(null);
  provenanceMontant = signal<'IA' | null>(null);
  provenanceAccident = signal<'IA' | null>(null);
  provenanceRetourImpose = signal<'IA' | null>(null);
  provenanceRefus = signal<'IA' | null>(null);

  readonly cadreOptions: ReadonlyArray<{ value: TeletravailCadre; label: string }> = [
    { value: 'ACCORD_COLLECTIF', label: 'Accord collectif (L. 1222-9 al. 1)' },
    { value: 'CHARTE_UNILATERALE', label: 'Charte unilatérale (L. 1222-9 al. 2)' },
    { value: 'ACCORD_INDIVIDUEL', label: 'Accord individuel (L. 1222-9 al. 3)' },
    { value: 'AUCUN', label: 'Aucun cadre formalisé' },
  ];

  constructor(
    private service: TeletravailAccordService,
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
   * Pré-fill IA (SF-212-16) — renseigne les 7 champs depuis le sous-objet
   * `teletravail_detail` (projeté à plat dans `travailExtractedData`).
   * Parité stricte avec `getPrefillCount()` : mêmes mappings, même gate
   * `workspaceCountry === 'FRANCE'`. No-op gracieux si `aiData` absent ou
   * dossier BE.
   */
  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    const ai = this.aiData;
    if (!ai) return;
    const rules = TeletravailAccordSectionPrefillRules;
    const ruleInput = { aiData: ai, workspaceCountry: this.workspaceCountry };

    const cadre = rules.computeCadre(ruleInput);
    if (cadre !== null) {
      this.cadreTeletravail.set(cadre);
      this.provenanceCadre.set('IA');
    }
    const dv = rules.computeDoubleVolontariat(ruleInput);
    if (dv !== null) {
      this.doubleVolontariatRespectee.set(dv);
      this.provenanceDoubleVolontariat.set('IA');
    }
    const indem = rules.computeIndemniteVersee(ruleInput);
    if (indem !== null) {
      this.indemniteOccupationVersee.set(indem);
      this.provenanceIndemnite.set('IA');
    }
    const mont = rules.computeMontantJournalier(ruleInput);
    if (mont !== null) {
      this.montantIndemniteJournalierEuros.set(mont);
      this.provenanceMontant.set('IA');
    }
    const acc = rules.computeAccidentDomicile(ruleInput);
    if (acc !== null) {
      this.accidentDomicileDetecte.set(acc);
      this.provenanceAccident.set('IA');
    }
    const retour = rules.computeRetourBureauImpose(ruleInput);
    if (retour !== null) {
      this.retourBureauImposeUnilateralement.set(retour);
      this.provenanceRetourImpose.set('IA');
    }
    const refus = rules.computeRefusIncrimine(ruleInput);
    if (refus !== null) {
      this.refusTeletravailCauseIncrimination.set(refus);
      this.provenanceRefus.set('IA');
    }
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  editMode(): void {
    this.showForm.set(true);
  }

  /** Form valide : FRANCE + cadre sélectionné + montant ≥ 0 si renseigné. */
  formValid(): boolean {
    if (this.workspaceCountry !== 'FRANCE') return false;
    if (!this.cadreTeletravail()) return false;
    const m = this.montantIndemniteJournalierEuros();
    if (m !== null && (m < 0 || !Number.isFinite(m))) return false;
    return true;
  }

  // --- Handlers — toute modification manuelle efface le badge IA du champ ---

  onCadreChange(value: TeletravailCadre): void {
    this.cadreTeletravail.set(value);
    this.provenanceCadre.set(null);
  }

  onDoubleVolontariatChange(value: boolean): void {
    this.doubleVolontariatRespectee.set(value);
    this.provenanceDoubleVolontariat.set(null);
  }

  onIndemniteVerseeChange(value: boolean): void {
    this.indemniteOccupationVersee.set(value);
    this.provenanceIndemnite.set(null);
  }

  onMontantChange(value: number | string | null): void {
    this.provenanceMontant.set(null);
    if (value === null || value === '' || value === undefined) {
      this.montantIndemniteJournalierEuros.set(null);
    } else {
      const num = typeof value === 'number' ? value : Number.parseFloat(String(value));
      this.montantIndemniteJournalierEuros.set(
          Number.isFinite(num) && num >= 0 ? num : null);
    }
  }

  onAccidentChange(value: boolean): void {
    this.accidentDomicileDetecte.set(value);
    this.provenanceAccident.set(null);
  }

  onRetourImposeChange(value: boolean): void {
    this.retourBureauImposeUnilateralement.set(value);
    this.provenanceRetourImpose.set(null);
  }

  onRefusIncrimineChange(value: boolean): void {
    this.refusTeletravailCauseIncrimination.set(value);
    this.provenanceRefus.set(null);
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request: TeletravailAccordRequest = {
      cadreTeletravail: this.cadreTeletravail(),
      doubleVolontariatRespectee: this.doubleVolontariatRespectee(),
      indemniteOccupationVersee: this.indemniteOccupationVersee(),
      montantIndemniteJournalierEuros: this.montantIndemniteJournalierEuros(),
      accidentDomicileDetecte: this.accidentDomicileDetecte(),
      retourBureauImposeUnilateralement: this.retourBureauImposeUnilateralement(),
      refusTeletravailCauseIncrimination: this.refusTeletravailCauseIncrimination(),
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.applyResult(r);
        this.calculating.set(false);
        this.snackBar.open('Analyse du télétravail calculée', 'OK', { duration: 2500 });
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

  private applyResult(r: TeletravailAccordResponse): void {
    this.result.set(r);
    this.hydrateForm(r);
    this.showForm.set(false);
  }

  /**
   * Ré-injecte le snapshot d'inputs de la réponse dans les champs du
   * formulaire — sans quoi un clic « Modifier » repartirait des valeurs par
   * défaut au lieu des dernières valeurs saisies.
   */
  private hydrateForm(r: TeletravailAccordResponse): void {
    this.cadreTeletravail.set(r.cadreTeletravail ?? 'ACCORD_COLLECTIF');
    this.doubleVolontariatRespectee.set(r.doubleVolontariatRespectee ?? true);
    this.indemniteOccupationVersee.set(r.indemniteOccupationVersee ?? true);
    this.montantIndemniteJournalierEuros.set(r.montantIndemniteJournalierEuros ?? null);
    this.accidentDomicileDetecte.set(r.accidentDomicileDetecte ?? false);
    this.retourBureauImposeUnilateralement.set(r.retourBureauImposeUnilateralement ?? false);
    this.refusTeletravailCauseIncrimination.set(r.refusTeletravailCauseIncrimination ?? false);
    // Valeurs persistées = saisie avocat — jamais de badge IA.
    this.provenanceCadre.set(null);
    this.provenanceDoubleVolontariat.set(null);
    this.provenanceIndemnite.set(null);
    this.provenanceMontant.set(null);
    this.provenanceAccident.set(null);
    this.provenanceRetourImpose.set(null);
    this.provenanceRefus.set(null);
  }

  // ---------------------------------------------------------------------------
  // Helpers d'affichage du résultat
  // ---------------------------------------------------------------------------

  /**
   * Verdict → bannière de couleur. 3 niveaux :
   *  - CONFORME → vert (dispositif conforme L. 1222-9 + ANI 2020)
   *  - NON_CONFORME → rouge (absence cadre ou violation structurelle)
   *  - LITIGE_IDENTIFIE → or (litige caractérisé)
   */
  verdictBannerClass(v: TeletravailAnalyse): string {
    switch (v) {
      case 'CONFORME':
        return 'tta-verdict-banner tta-verdict-banner--conforme';
      case 'NON_CONFORME':
        return 'tta-verdict-banner tta-verdict-banner--non-conforme';
      case 'LITIGE_IDENTIFIE':
        return 'tta-verdict-banner tta-verdict-banner--litige';
    }
  }

  verdictBannerLabel(v: TeletravailAnalyse): string {
    switch (v) {
      case 'CONFORME': return 'Dispositif de télétravail conforme';
      case 'NON_CONFORME': return 'Dispositif non conforme';
      case 'LITIGE_IDENTIFIE': return 'Litige identifié';
    }
  }

  verdictBannerIcon(v: TeletravailAnalyse): string {
    switch (v) {
      case 'CONFORME': return 'check_circle';
      case 'NON_CONFORME': return 'error_outline';
      case 'LITIGE_IDENTIFIE': return 'warning_amber';
    }
  }

  /** Tracker pour les points d'analyse. */
  pointTrackKey(_index: number, p: TeletravailPointAnalyse): string {
    return p.code;
  }
}
