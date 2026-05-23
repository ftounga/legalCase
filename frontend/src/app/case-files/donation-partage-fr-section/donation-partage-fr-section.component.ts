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
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { DonationPartageFrService } from '../../core/services/donation-partage-fr.service';
import {
  DonationPartageRequest,
  DonationPartageResponse,
} from '../../core/models/donation-partage-fr.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { DonationPartageFrPrefillRules } from './donation-partage-fr-section-prefill-rules';

/**
 * SF-216-30 : composant Angular standalone pour l'outil décisionnel
 * "Donation-partage" (F-FA-DONATION-PARTAGE).
 *
 * FRANCE uniquement (art. 1075 à 1075-5 Cciv + art. 1078, 1078-1, 1080 +
 * art. 912-928).
 *
 * Pattern de référence : `donation-entre-epoux-fr-section` (SF-216-24).
 *
 * Pré-fill IA (4 champs F-246 + SF-216-29) :
 *  - nombreDescendants                    ← `nbDescendantsDetecte`
 *  - respectQuotiteDisponible             ← `respectQuotiteDisponibleDetected`
 *  - presencePetitsEnfantsParSubstitution ← `presencePetitsEnfantsSubstitutionDetectee`
 *  - donationPartageConjonctive           ← `donationPartageConjonctiveDetectee`
 *
 * Country gate : `workspaceCountry !== 'FRANCE'` → bannière info + aucun appel.
 *
 * OnPush + ChangeDetectorRef.markForCheck() dans next/error des subscribe
 * (mémoire `feedback_onpush_subscribe_markforcheck`).
 */
@Component({
  selector: 'app-donation-partage-fr-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatCheckboxModule,
    MatProgressSpinnerModule,
    LegalCitationsPipe,
  ],
  templateUrl: './donation-partage-fr-section.component.html',
  styleUrl: './donation-partage-fr-section.component.scss',
})
export class DonationPartageFrSectionComponent implements OnInit, OnChanges {
  // F-177 SF-177-03b : metadata statique consommée par le panel pour la card.
  static readonly TOOL_LABEL = 'DONATION-PARTAGE';
  static readonly TOOL_ICON = 'group';

  /**
   * SF-216-30 — délégué au helper partagé (parité stricte avec prefillFromAi).
   */
  static getPrefillCount(input: {
    aiData?: FamilleExtractedData | null;
    workspaceCountry?: string;
  }): number {
    return DonationPartageFrPrefillRules.computePrefillCount({
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
  result = signal<DonationPartageResponse | null>(null);

  // --- Form fields (7 champs contrat backend SF-216-29) ---
  nombreDescendants = signal<number | null>(null);
  presencePetitsEnfantsParSubstitution = signal<boolean | null>(null);
  donationPartageConjonctive = signal<boolean | null>(null);
  valeurPartageTotal = signal<number | null>(null);
  respectQuotiteDisponible = signal<boolean | null>(null);
  donationsAnterieuresAReinorporer = signal<boolean | null>(null);
  /** Âges donateurs — 1 ou 2 saisies (parent 1 / parent 2). */
  ageDonateur1 = signal<number | null>(null);
  ageDonateur2 = signal<number | null>(null);

  constructor(
    private service: DonationPartageFrService,
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

  /** Form valide : FR + nombreDescendants >= 1. */
  formValid(): boolean {
    if (this.workspaceCountry !== 'FRANCE') return false;
    const nb = this.nombreDescendants();
    if (nb === null || nb < 1) return false;
    return true;
  }

  // --- Handlers ---

  onNombreDescendantsChange(value: number | string | null): void {
    this.nombreDescendants.set(this.parsePositiveInt(value));
  }

  onPetitsEnfantsChange(checked: boolean): void {
    this.presencePetitsEnfantsParSubstitution.set(checked);
  }

  onConjonctiveChange(checked: boolean): void {
    this.donationPartageConjonctive.set(checked);
  }

  onValeurChange(value: number | string | null): void {
    this.valeurPartageTotal.set(this.parseNonNegativeInt(value));
  }

  onRespectQuotiteChange(checked: boolean): void {
    this.respectQuotiteDisponible.set(checked);
  }

  onReincorporationChange(checked: boolean): void {
    this.donationsAnterieuresAReinorporer.set(checked);
  }

  onAge1Change(value: number | string | null): void {
    this.ageDonateur1.set(this.parseNonNegativeInt(value));
  }

  onAge2Change(value: number | string | null): void {
    this.ageDonateur2.set(this.parseNonNegativeInt(value));
  }

  private parsePositiveInt(value: number | string | null | undefined): number | null {
    if (value === null || value === undefined || value === '') return null;
    const num = typeof value === 'number' ? value : Number.parseInt(String(value), 10);
    if (!Number.isFinite(num) || num < 1) return null;
    return Math.trunc(num);
  }

  private parseNonNegativeInt(value: number | string | null | undefined): number | null {
    if (value === null || value === undefined || value === '') return null;
    const num = typeof value === 'number' ? value : Number.parseInt(String(value), 10);
    if (!Number.isFinite(num) || num < 0) return null;
    return Math.trunc(num);
  }

  private applyPrefillFromAi(): void {
    const v = DonationPartageFrPrefillRules.prefillFromAi({
      aiData: this.aiData ?? null,
      workspaceCountry: this.workspaceCountry,
    });
    if (v.nombreDescendants !== null && this.nombreDescendants() === null) {
      this.nombreDescendants.set(v.nombreDescendants);
    }
    if (v.respectQuotiteDisponible !== null && this.respectQuotiteDisponible() === null) {
      this.respectQuotiteDisponible.set(v.respectQuotiteDisponible);
    }
    if (
      v.presencePetitsEnfantsParSubstitution !== null
      && this.presencePetitsEnfantsParSubstitution() === null
    ) {
      this.presencePetitsEnfantsParSubstitution.set(v.presencePetitsEnfantsParSubstitution);
    }
    if (
      v.donationPartageConjonctive !== null
      && this.donationPartageConjonctive() === null
    ) {
      this.donationPartageConjonctive.set(v.donationPartageConjonctive);
    }
  }

  calculate(): void {
    if (!this.formValid()) return;
    const ages: number[] = [];
    const a1 = this.ageDonateur1();
    const a2 = this.ageDonateur2();
    if (a1 !== null) ages.push(a1);
    if (a2 !== null) ages.push(a2);
    const request: DonationPartageRequest = {
      nombreDescendants: this.nombreDescendants(),
      presencePetitsEnfantsParSubstitution: this.presencePetitsEnfantsParSubstitution(),
      donationPartageConjonctive: this.donationPartageConjonctive(),
      valeurPartageTotal: this.valeurPartageTotal(),
      respectQuotiteDisponible: this.respectQuotiteDisponible(),
      donationsAnterieuresAReinorporer: this.donationsAnterieuresAReinorporer(),
      agesDonateurs: ages.length > 0 ? ages : null,
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.applyResult(r);
        this.calculating.set(false);
        this.snackBar.open('Donation-partage évaluée', 'OK', { duration: 2500 });
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

  private applyResult(r: DonationPartageResponse): void {
    this.result.set(r);
    this.showForm.set(false);
  }

  // ---------------------------------------------------------------------------
  // Helpers d'affichage
  // ---------------------------------------------------------------------------

  interetLabel(v: string): string {
    const labels: Record<string, string> = {
      FORT: 'Intérêt fort',
      MOYEN: 'Intérêt moyen',
      FAIBLE: 'Intérêt faible',
      INADAPTE: 'Inadapté',
    };
    return labels[v] ?? v;
  }

  /** Classe CSS verdict. */
  verdictClass(): string {
    const r = this.result();
    if (!r) return '';
    if (r.alerteQuotite) return 'verdict-warning';
    if (!r.conditionsRemplies) return 'verdict-blocked';
    if (r.interet === 'FORT') return 'verdict-success';
    if (r.interet === 'MOYEN') return 'verdict-warning';
    return 'verdict-success';
  }
}
