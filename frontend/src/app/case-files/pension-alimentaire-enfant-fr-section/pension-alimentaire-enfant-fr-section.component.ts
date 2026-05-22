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
import { PensionAlimentaireEnfantFrService } from '../../core/services/pension-alimentaire-enfant-fr.service';
import {
  PensionAlimentaireEnfantFrRequest,
  PensionAlimentaireEnfantFrResponse,
  ModeResidenceEnfant,
} from '../../core/models/pension-alimentaire-enfant-fr.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { PensionAlimentaireEnfantFrPrefillRules } from './pension-alimentaire-enfant-fr-section-prefill-rules';

/**
 * SF-216-04 : composant Angular standalone pour l'outil décisionnel
 * "Pension alimentaire enfant FR" (F-FA-02).
 *
 * FRANCE uniquement (art. 371-2 Cciv + barème indicatif Cour de cassation
 * 2010 révisé + art. 373-2-2 Cciv révision).
 *
 * Remplace le wrapper présentationnel SF-198-02 (qui ne faisait qu'afficher
 * `synthesis.pensionAlimentaireEstimate`) par un vrai simulateur qui appelle
 * le backend SF-216-03 (table `pension_alimentaire_enfant_fr_analyses`).
 *
 * Pattern de référence : `liquidation-communaute-fr-section` (SF-216-06).
 *
 * Formulaire (5 champs) :
 *  - revenusNetsParent1Eur / revenusNetsParent2Eur (€/mois ≥ 0, facultatifs)
 *  - nombreEnfants (entier 1-12)
 *  - agesEnfants (liste, longueur = nombreEnfants)
 *  - modeResidence (ALTERNEE / PRINCIPALE_PARENT1 / PRINCIPALE_PARENT2)
 *
 * Verdict affiché :
 *  - montantParEnfantMensuelEur[] + totalMensuelEur
 *  - tauxApplique (badge barème Cass.)
 *  - parentDebiteur (PARENT1 / PARENT2 / AUCUN)
 *  - base juridique + messages + alertes
 *
 * Country gate : `workspaceCountry !== 'FRANCE'` → bannière info + aucun appel.
 *
 * OnPush + ChangeDetectorRef.markForCheck() dans next/error des subscribe
 * (mémoire `feedback_onpush_subscribe_markforcheck`).
 */
@Component({
  selector: 'app-pension-alimentaire-enfant-fr-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatSelectModule,
    MatProgressSpinnerModule,
    LegalCitationsPipe,
  ],
  templateUrl: './pension-alimentaire-enfant-fr-section.component.html',
  styleUrl: './pension-alimentaire-enfant-fr-section.component.scss',
})
export class PensionAlimentaireEnfantFrSectionComponent implements OnInit, OnChanges {
  // F-177 SF-177-03b : metadata statique consommée par le panel pour la card.
  static readonly TOOL_LABEL = 'PENSION ALIMENTAIRE ENFANT';
  static readonly TOOL_ICON = 'family_restroom';

  /**
   * SF-216-04 — délégué au helper partagé (parité stricte avec `prefillFromAi`).
   * Retourne le nombre de champs pré-remplissables FR (5 max — 2 revenus, nombre,
   * âges, mode résidence).
   */
  static getPrefillCount(input: {
    aiData?: FamilleExtractedData | null;
    workspaceCountry?: string;
  }): number {
    return PensionAlimentaireEnfantFrPrefillRules.computePrefillCount({
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
  result = signal<PensionAlimentaireEnfantFrResponse | null>(null);

  // --- Form fields (5 champs du contrat backend SF-216-03) ---
  revenusNetsParent1Eur = signal<number | null>(null);
  revenusNetsParent2Eur = signal<number | null>(null);
  nombreEnfants = signal<number | null>(null);
  agesEnfants = signal<number[]>([]);
  modeResidence = signal<ModeResidenceEnfant | null>(null);

  // Provenance IA par champ pré-rempli — `'IA'` tant que la valeur vient de l'analyse.
  provenanceRevenus1 = signal<'IA' | null>(null);
  provenanceRevenus2 = signal<'IA' | null>(null);
  provenanceNombreEnfants = signal<'IA' | null>(null);
  provenanceAgesEnfants = signal<'IA' | null>(null);
  provenanceModeResidence = signal<'IA' | null>(null);

  /** Options du select `modeResidence`. */
  readonly modesResidence: ReadonlyArray<{ value: ModeResidenceEnfant; label: string }> = [
    { value: 'PRINCIPALE_PARENT1', label: 'Résidence principale chez parent 1' },
    { value: 'PRINCIPALE_PARENT2', label: 'Résidence principale chez parent 2' },
    { value: 'ALTERNEE', label: 'Garde alternée (50/50)' },
  ];

  constructor(
    private service: PensionAlimentaireEnfantFrService,
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
   * Pré-fill IA SF-216-04 — renseigne les 5 champs depuis `familleExtractedData`.
   * Délègue au helper partagé. No-op gracieux si `aiData` absent ou dossier BE.
   */
  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    const ai = this.aiData;
    if (!ai) return;

    const rules = PensionAlimentaireEnfantFrPrefillRules;
    const ruleInput = { aiData: ai, workspaceCountry: this.workspaceCountry };

    const r1 = rules.computeRevenusNetsParent1(ruleInput);
    if (r1 !== null) {
      this.revenusNetsParent1Eur.set(r1);
      this.provenanceRevenus1.set('IA');
    }
    const r2 = rules.computeRevenusNetsParent2(ruleInput);
    if (r2 !== null) {
      this.revenusNetsParent2Eur.set(r2);
      this.provenanceRevenus2.set('IA');
    }
    const n = rules.computeNombreEnfants(ruleInput);
    if (n !== null) {
      this.nombreEnfants.set(n);
      this.provenanceNombreEnfants.set('IA');
    }
    const ages = rules.computeAgesEnfants(ruleInput);
    if (ages !== null) {
      this.agesEnfants.set(ages);
      this.provenanceAgesEnfants.set('IA');
      // Synchroniser nombreEnfants si manquant.
      if (this.nombreEnfants() === null) {
        this.nombreEnfants.set(ages.length);
      }
    }
    const mode = rules.computeModeResidence(ruleInput);
    if (mode !== null) {
      this.modeResidence.set(mode);
      this.provenanceModeResidence.set('IA');
    }
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  editMode(): void {
    this.showForm.set(true);
  }

  /**
   * Form valide : FR + nombreEnfants ≥ 1 + agesEnfants alignés + modeResidence + revenus ≥ 0.
   */
  formValid(): boolean {
    if (this.workspaceCountry !== 'FRANCE') return false;
    const n = this.nombreEnfants();
    if (n === null || n < 1 || n > 12) return false;
    const ages = this.agesEnfants();
    if (!Array.isArray(ages) || ages.length !== n) return false;
    if (ages.some((a) => !Number.isFinite(a) || a < 0)) return false;
    if (this.modeResidence() === null) return false;
    const r1 = this.revenusNetsParent1Eur();
    const r2 = this.revenusNetsParent2Eur();
    if (r1 !== null && (!Number.isFinite(r1) || r1 < 0)) return false;
    if (r2 !== null && (!Number.isFinite(r2) || r2 < 0)) return false;
    return true;
  }

  // --- Handlers — toute modification manuelle efface le badge IA ---

  onRevenus1Change(value: number | string | null): void {
    this.revenusNetsParent1Eur.set(this.parseNonNegativeInt(value));
    this.provenanceRevenus1.set(null);
  }

  onRevenus2Change(value: number | string | null): void {
    this.revenusNetsParent2Eur.set(this.parseNonNegativeInt(value));
    this.provenanceRevenus2.set(null);
  }

  onNombreEnfantsChange(value: number | string | null): void {
    const n = this.parseNonNegativeInt(value);
    this.nombreEnfants.set(n);
    this.provenanceNombreEnfants.set(null);
    // Redimensionner agesEnfants si nécessaire (préservation des valeurs).
    if (n !== null && n > 0) {
      const current = this.agesEnfants();
      if (current.length !== n) {
        const next = [...current];
        while (next.length < n) next.push(0);
        next.length = n;
        this.agesEnfants.set(next);
        this.provenanceAgesEnfants.set(null);
      }
    } else {
      this.agesEnfants.set([]);
    }
  }

  onAgeEnfantChange(index: number, value: number | string | null): void {
    const v = this.parseNonNegativeInt(value);
    const ages = [...this.agesEnfants()];
    ages[index] = v === null ? 0 : v;
    this.agesEnfants.set(ages);
    this.provenanceAgesEnfants.set(null);
  }

  onModeResidenceChange(value: ModeResidenceEnfant | null): void {
    this.modeResidence.set(value);
    this.provenanceModeResidence.set(null);
  }

  private parseNonNegativeInt(value: number | string | null | undefined): number | null {
    if (value === null || value === undefined || value === '') return null;
    const num = typeof value === 'number' ? value : Number.parseInt(String(value), 10);
    if (!Number.isFinite(num) || num < 0) return null;
    return num;
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request: PensionAlimentaireEnfantFrRequest = {
      revenusNetsParent1Eur: this.revenusNetsParent1Eur(),
      revenusNetsParent2Eur: this.revenusNetsParent2Eur(),
      nombreEnfants: this.nombreEnfants(),
      agesEnfants: this.agesEnfants(),
      modeResidence: this.modeResidence(),
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.applyResult(r);
        this.calculating.set(false);
        this.snackBar.open('Pension alimentaire calculée', 'OK', { duration: 2500 });
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

  private applyResult(r: PensionAlimentaireEnfantFrResponse): void {
    this.result.set(r);
    this.showForm.set(false);
  }

  // ---------------------------------------------------------------------------
  // Helpers d'affichage du résultat
  // ---------------------------------------------------------------------------

  formatEuros(amount: number): string {
    return new Intl.NumberFormat('fr-FR', {
      style: 'currency',
      currency: 'EUR',
      maximumFractionDigits: 0,
    }).format(amount);
  }

  /** Libellé humain pour l'enum parent débiteur. */
  parentDebiteurLabel(p: string): string {
    if (p === 'PARENT1') return 'Parent 1';
    if (p === 'PARENT2') return 'Parent 2';
    if (p === 'AUCUN') return 'Aucun (revenus comparables en garde alternée)';
    return p;
  }

  /** Iteration sur les âges pour le template (Angular @for). */
  ageIndices(): number[] {
    return this.agesEnfants().map((_, i) => i);
  }

  tauxPourcent(): string {
    const r = this.result();
    if (!r) return '';
    return (r.tauxApplique * 100).toFixed(0) + ' %';
  }
}
