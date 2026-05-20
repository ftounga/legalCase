import {
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
import { ContributionAlimentaireEnfantsBeService } from '../../core/services/contribution-alimentaire-enfants-be.service';
import {
  ContributionAlimentaireEnfantsBeRequest,
  ContributionAlimentaireEnfantsBeResponse,
  ContributionAlimentaireEnfantsBeVerdict,
  ParentDebiteurBe,
  TrancheAgeEnfantBe,
} from '../../core/models/contribution-alimentaire-enfants-be.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { ContributionAlimentaireEnfantsBeSectionPrefillRules } from './contribution-alimentaire-enfants-be-section-prefill-rules';

/** Option de tranche d'âge (alignée sur l'enum backend `TrancheAgeEnfantBe`). */
interface TrancheAgeOption {
  value: TrancheAgeEnfantBe;
  label: string;
}

/**
 * SF-217-07 : composant Angular standalone pour l'outil décisionnel
 * "Contribution alimentaire des enfants (Belgique)"
 * (`contribution-alimentaire-enfants-be`). BELGIQUE uniquement.
 *
 * Pattern de référence : `regime-communaute-legale-be-section` (F-217 SF-217-03).
 *
 * - Form : nombre d'enfants, tranche d'âge (select), revenus des deux parents,
 *   coût mensuel global (optionnel — modèle Renard indexé sur les revenus sinon),
 *   nuits d'hébergement, allocations familiales, frais extraordinaires.
 * - Verdict 3 niveaux : CONTRIBUTION_DUE (navy) / CONTRIBUTION_EQUILIBREE
 *   (navy) / DONNEES_INSUFFISANTES (or).
 * - Affichage : montant mensuel net, détail du calcul Renard, frais
 *   extraordinaires répartis, bases juridiques, messages.
 * - Pré-fill IA : V1 = 0 champ (`PREFILL_COUNT_ALWAYS_ZERO`).
 * - Refresh dashboard F-IA-02 + MatSnackBar erreurs.
 * - Gate `workspaceCountry === 'BELGIQUE'` — bannière info si mismatch.
 */
@Component({
  selector: 'app-contribution-alimentaire-enfants-be-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatProgressSpinnerModule,
    LegalCitationsPipe,
  ],
  templateUrl: './contribution-alimentaire-enfants-be-section.component.html',
  styleUrl: './contribution-alimentaire-enfants-be-section.component.scss',
})
export class ContributionAlimentaireEnfantsBeSectionComponent implements OnInit, OnChanges {
  static readonly TOOL_LABEL = 'CONTRIBUTION ALIMENTAIRE ENFANTS — MÉTHODE RENARD BE';
  static readonly TOOL_ICON = 'savings';

  /**
   * SF-246-28 / F-236 / F-237 — délègue au helper partagé.
   * 6 champs maximum (nombreEnfants, revenus P1/P2, allocations, nuits P1/P2).
   */
  static getPrefillCount(input: {
    aiData?: unknown;
    procedureChecks?: unknown[];
    aiQuestions?: unknown[];
    piecesManquantes?: unknown[];
    triggerEvents?: unknown[];
    workspaceCountry?: string;
  }): number {
    return ContributionAlimentaireEnfantsBeSectionPrefillRules.computePrefillCount({
      aiData: input.aiData,
    });
  }

  static readonly TRANCHE_AGE_OPTIONS: readonly TrancheAgeOption[] = [
    { value: 'ENFANT_0_5', label: '0 à 5 ans' },
    { value: 'ENFANT_6_11', label: '6 à 11 ans' },
    { value: 'ENFANT_12_17', label: '12 à 17 ans' },
    { value: 'ENFANT_18_PLUS', label: '18 ans et plus (études)' },
  ];
  readonly trancheAgeOptions =
    ContributionAlimentaireEnfantsBeSectionComponent.TRANCHE_AGE_OPTIONS;

  @Input() caseFileId!: string;
  /** F-177 — force l'expansion (mode modal). */
  @Input() forceExpanded = false;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'BELGIQUE';
  @Input() aiData?: FamilleExtractedData | null;
  /** Mode simulateur autonome (hors dossier) — coupe le refresh dashboard. */
  @Input() standaloneMode = false;

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<ContributionAlimentaireEnfantsBeResponse | null>(null);

  // --- Form fields ---
  nombreEnfants = signal<number>(1);
  trancheAgeEnfants = signal<TrancheAgeEnfantBe>('ENFANT_6_11');
  revenuMensuelParent1 = signal<number | null>(null);
  revenuMensuelParent2 = signal<number | null>(null);
  coutMensuelGlobalEnfants = signal<number | null>(null);
  nuitsHebergementParent1 = signal<number | null>(null);
  nuitsHebergementParent2 = signal<number | null>(null);
  allocationsFamilialesMensuelles = signal<number | null>(null);
  fraisExtraordinairesMensuels = signal<number | null>(null);
  commentaire = signal<string | null>(null);

  /** SF-246-28 — signaux de provenance (null = manuel, 'IA' = pré-rempli). */
  provenanceNombreEnfants = signal<'IA' | null>(null);
  provenanceRevenuParent1 = signal<'IA' | null>(null);
  provenanceRevenuParent2 = signal<'IA' | null>(null);
  provenanceAllocations = signal<'IA' | null>(null);
  provenanceNuitsParent1 = signal<'IA' | null>(null);
  provenanceNuitsParent2 = signal<'IA' | null>(null);

  constructor(
    private service: ContributionAlimentaireEnfantsBeService,
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
   * SF-246-28 — Pré-fill IA réel pour les 6 champs Contribution Alimentaire BE.
   * No-op gracieux si `aiData` est null ou sous-objet absent.
   */
  private prefillFromAi(): void {
    const rules = ContributionAlimentaireEnfantsBeSectionPrefillRules;
    const input = { aiData: this.aiData };
    let changed = false;

    const nb = rules.computeNombreEnfants(input);
    if (nb != null) { this.nombreEnfants.set(nb); this.provenanceNombreEnfants.set('IA'); changed = true; }

    const r1 = rules.computeRevenuParent1(input);
    if (r1 != null) { this.revenuMensuelParent1.set(r1); this.provenanceRevenuParent1.set('IA'); changed = true; }

    const r2 = rules.computeRevenuParent2(input);
    if (r2 != null) { this.revenuMensuelParent2.set(r2); this.provenanceRevenuParent2.set('IA'); changed = true; }

    const alloc = rules.computeAllocationsFamiliales(input);
    if (alloc != null) { this.allocationsFamilialesMensuelles.set(alloc); this.provenanceAllocations.set('IA'); changed = true; }

    const n1 = rules.computeNuitsParent1(input);
    if (n1 != null) { this.nuitsHebergementParent1.set(n1); this.provenanceNuitsParent1.set('IA'); changed = true; }

    const n2 = rules.computeNuitsParent2(input);
    if (n2 != null) { this.nuitsHebergementParent2.set(n2); this.provenanceNuitsParent2.set('IA'); changed = true; }

    if (changed) this.cdr.markForCheck();
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  editMode(): void {
    this.showForm.set(true);
  }

  /**
   * Form valide si workspace BE, nombre d'enfants 1-12 et les deux revenus
   * renseignés (≥ 0). Le backend reste la source de vérité de la validation.
   */
  formValid(): boolean {
    if (this.workspaceCountry !== 'BELGIQUE') return false;
    const nb = this.nombreEnfants();
    if (nb == null || nb < 1 || nb > 12) return false;
    const r1 = this.revenuMensuelParent1();
    const r2 = this.revenuMensuelParent2();
    return r1 != null && r1 >= 0 && r2 != null && r2 >= 0;
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request: ContributionAlimentaireEnfantsBeRequest = {
      nombreEnfants: this.nombreEnfants(),
      trancheAgeEnfants: this.trancheAgeEnfants(),
      revenuMensuelParent1: this.revenuMensuelParent1()!,
      revenuMensuelParent2: this.revenuMensuelParent2()!,
      coutMensuelGlobalEnfants: this.coutMensuelGlobalEnfants(),
      nuitsHebergementParent1: this.nuitsHebergementParent1() ?? 0,
      nuitsHebergementParent2: this.nuitsHebergementParent2() ?? 0,
      allocationsFamilialesMensuelles: this.allocationsFamilialesMensuelles(),
      fraisExtraordinairesMensuels: this.fraisExtraordinairesMensuels(),
      parentDebiteurEstParent1: null,
      commentaire: this.commentaire()?.trim() || null,
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.applyResult(r);
        this.calculating.set(false);
        this.snackBar.open('Contribution alimentaire estimée', 'OK', { duration: 2500 });
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

  private applyResult(r: ContributionAlimentaireEnfantsBeResponse): void {
    this.result.set(r);
    this.hydrateForm(r);
    this.showForm.set(false);
  }

  /**
   * Ré-injecte le snapshot d'inputs de la réponse dans les champs du
   * formulaire — sans quoi un clic « Modifier » repartirait des valeurs par
   * défaut au lieu des dernières valeurs saisies (leçon F-DT-36).
   */
  private hydrateForm(r: ContributionAlimentaireEnfantsBeResponse): void {
    this.nombreEnfants.set(r.nombreEnfants);
    this.trancheAgeEnfants.set(r.trancheAgeEnfants);
    this.revenuMensuelParent1.set(r.revenuMensuelParent1);
    this.revenuMensuelParent2.set(r.revenuMensuelParent2);
    this.coutMensuelGlobalEnfants.set(r.coutMensuelGlobalEnfants);
    this.nuitsHebergementParent1.set(r.nuitsHebergementParent1);
    this.nuitsHebergementParent2.set(r.nuitsHebergementParent2);
    this.allocationsFamilialesMensuelles.set(r.allocationsFamilialesMensuelles);
    this.fraisExtraordinairesMensuels.set(r.fraisExtraordinairesMensuels);
    this.commentaire.set(r.commentaire);
  }

  // ---------------------------------------------------------------------------
  // Helpers d'affichage du résultat
  // ---------------------------------------------------------------------------

  /** Or réservé à DONNEES_INSUFFISANTES ; navy pour les verdicts chiffrés. */
  verdictBannerClass(verdict: ContributionAlimentaireEnfantsBeVerdict): string {
    switch (verdict) {
      case 'CONTRIBUTION_DUE':
        return 'cae-verdict-banner cae-verdict-banner--available';
      case 'CONTRIBUTION_EQUILIBREE':
        return 'cae-verdict-banner cae-verdict-banner--available';
      case 'DONNEES_INSUFFISANTES':
        return 'cae-verdict-banner cae-verdict-banner--medium';
    }
  }

  verdictBannerLabel(verdict: ContributionAlimentaireEnfantsBeVerdict): string {
    switch (verdict) {
      case 'CONTRIBUTION_DUE': return 'Contribution alimentaire due';
      case 'CONTRIBUTION_EQUILIBREE': return 'Contribution équilibrée';
      case 'DONNEES_INSUFFISANTES': return 'Données insuffisantes';
    }
  }

  verdictBannerIcon(verdict: ContributionAlimentaireEnfantsBeVerdict): string {
    switch (verdict) {
      case 'CONTRIBUTION_DUE': return 'savings';
      case 'CONTRIBUTION_EQUILIBREE': return 'balance';
      case 'DONNEES_INSUFFISANTES': return 'warning';
    }
  }

  parentDebiteurLabel(parent: ParentDebiteurBe): string {
    switch (parent) {
      case 'PARENT_1': return 'Parent 1';
      case 'PARENT_2': return 'Parent 2';
      case 'AUCUN': return 'Aucun (contribution équilibrée)';
    }
  }
}
