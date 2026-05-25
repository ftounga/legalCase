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
import { DonationEntreEpouxFrService } from '../../core/services/donation-entre-epoux-fr.service';
import {
  AvantageMatrimonialType,
  BienDonneType,
  DonationEntreEpouxRequest,
  DonationEntreEpouxResponse,
  RegimeMatrimonial,
} from '../../core/models/donation-entre-epoux-fr.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { DonationEntreEpouxFrPrefillRules } from './donation-entre-epoux-fr-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-216-24 : composant Angular standalone pour l'outil décisionnel
 * "Donation entre époux" (F-FA-DONATION-ENTRE-EPOUX).
 *
 * FRANCE uniquement (art. 1091-1100 Cciv + art. 265 al. 2 + art. 1527 al. 2
 * + art. 912-928).
 *
 * Pattern de référence : `indignite-successorale-fr-section` (SF-216-20).
 *
 * Pré-fill IA (5 champs F-246 + SF-216-23) :
 *  - regimeMatrimonial            ← `regimeMatrimonialDetecte`
 *  - clauseAttributionIntegrale   ← `clauseAttributionIntegraleDetected`
 *  - enfantsNonCommuns            ← `enfantsNonCommunsDetected`
 *  - revocabiliteDetectee         ← `revocabiliteDetectee`
 *  - bienDonneType                ← `bienDonnePrincipalType`
 *
 * Country gate : `workspaceCountry !== 'FRANCE'` → bannière info + aucun appel.
 *
 * OnPush + ChangeDetectorRef.markForCheck() dans next/error des subscribe
 * (mémoire `feedback_onpush_subscribe_markforcheck`).
 */
@Component({
  selector: 'app-donation-entre-epoux-fr-section',
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
  templateUrl: './donation-entre-epoux-fr-section.component.html',
  styleUrl: './donation-entre-epoux-fr-section.component.scss',
})
export class DonationEntreEpouxFrSectionComponent implements OnInit, OnChanges {
  // F-JU-03 SF-JU-03-99e v3 — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-FA-DONATION-ENTRE-EPOUX';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommée par le panel pour la card.
  static readonly TOOL_LABEL = 'DONATION ENTRE ÉPOUX';
  static readonly TOOL_ICON = 'favorite';

  /**
   * SF-216-24 — délégué au helper partagé (parité stricte avec prefillFromAi).
   */
  static getPrefillCount(input: {
    aiData?: FamilleExtractedData | null;
    workspaceCountry?: string;
  }): number {
    return DonationEntreEpouxFrPrefillRules.computePrefillCount({
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
  result = signal<DonationEntreEpouxResponse | null>(null);

  // --- Form fields (8 champs contrat backend SF-216-23) ---
  avantageMatrimonialType = signal<AvantageMatrimonialType | null>(null);
  dateContrat = signal<string | null>(null);
  regimeMatrimonial = signal<RegimeMatrimonial | null>(null);
  mariageDissous = signal<boolean>(false);
  revocabiliteDetectee = signal<boolean | null>(null);
  bienDonneType = signal<BienDonneType | null>(null);
  valeurBienDonneEur = signal<number | null>(null);
  enfantsNonCommuns = signal<boolean | null>(null);
  /** Cohérence UI : `CLAUSE_ATTRIBUTION_INTEGRALE` saisie via une case dédiée. */
  clauseAttributionIntegrale = signal<boolean | null>(null);

  /** Options select type d'avantage matrimonial. */
  readonly typesAvantage: ReadonlyArray<{ value: AvantageMatrimonialType; label: string }> = [
    { value: 'DONATION_BIENS_FUTURS', label: 'Donation au dernier vivant (biens futurs — art. 1094 Cciv)' },
    { value: 'DONATION_BIEN_PRESENT', label: 'Donation de biens présents (art. 1091 Cciv)' },
    { value: 'CLAUSE_ATTRIBUTION_INTEGRALE', label: 'Clause d\'attribution intégrale (art. 1524 Cciv)' },
    { value: 'AVANTAGE_MATRIMONIAL', label: 'Avantage matrimonial (préciput, partage inégal — art. 1515-1525)' },
    { value: 'ASSURANCE_VIE', label: 'Assurance-vie au profit du conjoint' },
  ];

  readonly regimesMatrimoniaux: ReadonlyArray<{ value: RegimeMatrimonial; label: string }> = [
    { value: 'COMMUNAUTE_LEGALE', label: 'Communauté légale (réduite aux acquêts)' },
    { value: 'COMMUNAUTE_UNIVERSELLE', label: 'Communauté universelle' },
    { value: 'SEPARATION_DE_BIENS', label: 'Séparation de biens' },
    { value: 'PARTICIPATION_AUX_ACQUETS', label: 'Participation aux acquêts' },
    { value: 'AUTRE', label: 'Autre régime / régime étranger' },
  ];

  readonly typesBien: ReadonlyArray<{ value: BienDonneType; label: string }> = [
    { value: 'IMMOBILIER', label: 'Immobilier' },
    { value: 'MOBILIER', label: 'Mobilier' },
    { value: 'PORTEFEUILLE', label: 'Portefeuille / titres' },
    { value: 'NUMERAIRE', label: 'Numéraire / sommes d\'argent' },
    { value: 'AUTRE', label: 'Autre' },
  ];

  constructor(
    private service: DonationEntreEpouxFrService,
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

  /** Form valide : FR + type d'avantage sélectionné. */
  formValid(): boolean {
    if (this.workspaceCountry !== 'FRANCE') return false;
    if (this.avantageMatrimonialType() === null) return false;
    return true;
  }

  // --- Handlers ---

  onAvantageChange(value: AvantageMatrimonialType | null): void {
    this.avantageMatrimonialType.set(value);
    // Synchroniser la case CLAUSE_ATTRIBUTION_INTEGRALE avec le select.
    if (value === 'CLAUSE_ATTRIBUTION_INTEGRALE') {
      this.clauseAttributionIntegrale.set(true);
    }
  }

  onClauseChange(checked: boolean): void {
    this.clauseAttributionIntegrale.set(checked);
    if (checked) {
      this.avantageMatrimonialType.set('CLAUSE_ATTRIBUTION_INTEGRALE');
    }
  }

  onRegimeChange(value: RegimeMatrimonial | null): void {
    this.regimeMatrimonial.set(value);
  }

  onMariageDissousChange(checked: boolean): void {
    this.mariageDissous.set(checked);
  }

  onRevocabiliteChange(checked: boolean): void {
    this.revocabiliteDetectee.set(checked);
  }

  onEnfantsNonCommunsChange(checked: boolean): void {
    this.enfantsNonCommuns.set(checked);
  }

  onBienChange(value: BienDonneType | null): void {
    this.bienDonneType.set(value);
  }

  onDateContratChange(value: string | null): void {
    this.dateContrat.set(value && value.trim() !== '' ? value : null);
  }

  onValeurChange(value: number | string | null): void {
    this.valeurBienDonneEur.set(this.parseNonNegativeInt(value));
  }

  private parseNonNegativeInt(value: number | string | null | undefined): number | null {
    if (value === null || value === undefined || value === '') return null;
    const num = typeof value === 'number' ? value : Number.parseInt(String(value), 10);
    if (!Number.isFinite(num) || num < 0) return null;
    return num;
  }

  private applyPrefillFromAi(): void {
    const v = DonationEntreEpouxFrPrefillRules.prefillFromAi({
      aiData: this.aiData ?? null,
      workspaceCountry: this.workspaceCountry,
    });
    if (v.regimeMatrimonial !== null && this.regimeMatrimonial() === null) {
      this.regimeMatrimonial.set(v.regimeMatrimonial);
    }
    if (v.clauseAttributionIntegrale !== null && this.clauseAttributionIntegrale() === null) {
      this.clauseAttributionIntegrale.set(v.clauseAttributionIntegrale);
      if (v.clauseAttributionIntegrale === true && this.avantageMatrimonialType() === null) {
        this.avantageMatrimonialType.set('CLAUSE_ATTRIBUTION_INTEGRALE');
      }
    }
    if (v.enfantsNonCommuns !== null && this.enfantsNonCommuns() === null) {
      this.enfantsNonCommuns.set(v.enfantsNonCommuns);
    }
    if (v.revocabiliteDetectee !== null && this.revocabiliteDetectee() === null) {
      this.revocabiliteDetectee.set(v.revocabiliteDetectee);
    }
    if (v.bienDonneType !== null && this.bienDonneType() === null) {
      this.bienDonneType.set(v.bienDonneType);
    }
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request: DonationEntreEpouxRequest = {
      avantageMatrimonialType: this.avantageMatrimonialType(),
      dateContrat: this.dateContrat(),
      regimeMatrimonial: this.regimeMatrimonial(),
      mariageDissous: this.mariageDissous(),
      revocabiliteDetectee: this.revocabiliteDetectee(),
      bienDonneType: this.bienDonneType(),
      valeurBienDonneEur: this.valeurBienDonneEur(),
      enfantsNonCommunsDetected: this.enfantsNonCommuns(),
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.applyResult(r);
        this.calculating.set(false);
        this.snackBar.open('Donation entre époux évaluée', 'OK', { duration: 2500 });
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

  private applyResult(r: DonationEntreEpouxResponse): void {
    this.result.set(r);
    this.showForm.set(false);
  }

  // ---------------------------------------------------------------------------
  // Helpers d'affichage
  // ---------------------------------------------------------------------------

  validiteLabel(v: string): string {
    const labels: Record<string, string> = {
      VALIDE: 'Donation valide',
      REVOQUEE: 'Donation révoquée',
      A_VERIFIER: 'À vérifier',
    };
    return labels[v] ?? v;
  }

  revocabiliteLabel(r: string): string {
    const labels: Record<string, string> = {
      REVOCABLE_AD_NUTUM: 'Révocable ad nutum (art. 1096 al. 1)',
      REVOCATION_DE_PLEIN_DROIT: 'Révocation de plein droit (art. 265 al. 2)',
      REVOCATION_CONSTATEE: 'Révocation constatée (art. 1096 al. 2)',
      REVOCABILITE_LIMITEE: 'Révocabilité limitée (intégrée au contrat de mariage)',
      IRREVOCABLE: 'Irrévocable pendant le mariage',
      INDETERMINEE: 'Indéterminée',
    };
    return labels[r] ?? r;
  }

  /** Classe CSS verdict. */
  verdictClass(): string {
    const r = this.result();
    if (!r) return '';
    if (r.validite === 'REVOQUEE') return 'verdict-blocked';
    if (r.actionRetranchementPossible) return 'verdict-warning';
    if (r.validite === 'A_VERIFIER') return 'verdict-warning';
    return 'verdict-success';
  }
}
