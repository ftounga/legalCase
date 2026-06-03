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
import { AsfCafService } from '../../core/services/asf-caf.service';
import {
  AsfCafRequest,
  AsfCafResponse,
  PensionPayee,
  VerdictAsf,
} from '../../core/models/asf-caf.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { AsfCafPrefillRules } from './asf-caf-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-222-01 : composant Angular standalone pour l'outil décisionnel "Allocation
 * de soutien familial" (F-FA-ASF-CAF).
 *
 * FRANCE uniquement (art. L. 523-1 CSS — ASF versée par la CAF au parent isolé
 * quand l'autre parent ne paie pas / paie insuffisamment la pension alimentaire,
 * ou est décédé / non identifié).
 *
 * Pattern de référence : `aripa-recouvrement-fr-section` (SF-216-08).
 *
 * Anti-doublon F-FA-ARIPA-RECOUVREMENT : l'ASF est le droit + montant de la
 * prestation CAF ; le recouvrement forcé de la pension reste l'outil ARIPA. En
 * cas d'ASF récupérable, le résultat renvoie vers ARIPA.
 *
 * Formulaire (6 champs du contrat backend SF-222-01) :
 *  - parentIsole (checkbox)
 *  - pensionFixee (checkbox)
 *  - montantPensionMensuel (entier ≥ 0, €/mois, nullable)
 *  - pensionPayee (enum NON_PAYEE / PARTIELLE / PAYEE)
 *  - nbEnfantsACharge (entier ≥ 1)
 *  - autreParentDecedeOuInconnu (checkbox)
 *
 * Verdict affiché : verdict (4 niveaux) + montant mensuel estimé +
 * recouvrement applicable (renvoi ARIPA) + bases juridiques + messages.
 *
 * OnPush + ChangeDetectorRef.markForCheck() dans next/error des subscribe
 * (mémoire `feedback_onpush_subscribe_markforcheck`).
 */
@Component({
  selector: 'app-asf-caf-section',
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
  templateUrl: './asf-caf-section.component.html',
  styleUrl: './asf-caf-section.component.scss',
})
export class AsfCafSectionComponent implements OnInit, OnChanges {
  // F-JU-03 — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-FA-ASF-CAF';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommée par le panel pour la card.
  static readonly TOOL_LABEL = 'ASF ALLOCATION SOUTIEN FAMILIAL';
  static readonly TOOL_ICON = 'family_restroom';

  /**
   * SF-222-01 — délégué au helper partagé (parité stricte avec `prefillFromAi`).
   * Retourne le nombre de champs pré-remplissables FR (max 5).
   */
  static getPrefillCount(input: {
    aiData?: FamilleExtractedData | null;
    workspaceCountry?: string;
  }): number {
    return AsfCafPrefillRules.computePrefillCount({
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
  result = signal<AsfCafResponse | null>(null);

  // --- Form fields (6 champs du contrat backend SF-222-01) ---
  parentIsole = signal<boolean>(false);
  pensionFixee = signal<boolean>(false);
  montantPensionMensuel = signal<number | null>(null);
  pensionPayee = signal<PensionPayee | null>(null);
  nbEnfantsACharge = signal<number | null>(null);
  autreParentDecedeOuInconnu = signal<boolean>(false);

  // Provenance IA par champ (badge dans l'UI tant que valeur non modifiée).
  provenanceParentIsole = signal<'IA' | null>(null);
  provenancePensionFixee = signal<'IA' | null>(null);
  provenanceMontantPension = signal<'IA' | null>(null);
  provenancePensionPayee = signal<'IA' | null>(null);
  provenanceNbEnfants = signal<'IA' | null>(null);

  /** Options select état de paiement de la pension. */
  readonly pensionPayeeOptions: ReadonlyArray<{ value: PensionPayee; label: string }> = [
    { value: 'NON_PAYEE', label: 'Non payée' },
    { value: 'PARTIELLE', label: 'Payée partiellement' },
    { value: 'PAYEE', label: 'Payée intégralement' },
  ];

  constructor(
    private service: AsfCafService,
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
   * Pré-fill IA SF-222-01 — renseigne les 5 champs détectables depuis
   * `familleExtractedData`. No-op gracieux si `aiData` absent ou dossier BE.
   */
  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    const ai = this.aiData;
    if (!ai) return;

    const rules = AsfCafPrefillRules;
    const ruleInput = { aiData: ai, workspaceCountry: this.workspaceCountry };

    const parentIsole = rules.computeParentIsole(ruleInput);
    if (parentIsole !== null) {
      this.parentIsole.set(parentIsole);
      this.provenanceParentIsole.set('IA');
    }
    const pensionFixee = rules.computePensionFixee(ruleInput);
    if (pensionFixee !== null) {
      this.pensionFixee.set(pensionFixee);
      this.provenancePensionFixee.set('IA');
    }
    const montant = rules.computeMontantPensionMensuel(ruleInput);
    if (montant !== null) {
      this.montantPensionMensuel.set(montant);
      this.provenanceMontantPension.set('IA');
    }
    const payee = rules.computePensionPayee(ruleInput);
    if (payee !== null) {
      this.pensionPayee.set(payee);
      this.provenancePensionPayee.set('IA');
    }
    const nbEnfants = rules.computeNbEnfantsACharge(ruleInput);
    if (nbEnfants !== null) {
      this.nbEnfantsACharge.set(nbEnfants);
      this.provenanceNbEnfants.set('IA');
    }
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  editMode(): void {
    this.showForm.set(true);
  }

  /** Form valide : FR + nb enfants ≥ 1. */
  formValid(): boolean {
    if (this.workspaceCountry !== 'FRANCE') return false;
    const nbEnfants = this.nbEnfantsACharge();
    if (nbEnfants === null || !Number.isFinite(nbEnfants) || nbEnfants < 1) return false;
    const montant = this.montantPensionMensuel();
    if (montant !== null && (!Number.isFinite(montant) || montant < 0)) return false;
    return true;
  }

  // --- Handlers — toute modification manuelle efface le badge IA ---

  onParentIsoleChange(checked: boolean): void {
    this.parentIsole.set(checked);
    this.provenanceParentIsole.set(null);
  }

  onPensionFixeeChange(checked: boolean): void {
    this.pensionFixee.set(checked);
    this.provenancePensionFixee.set(null);
  }

  onMontantPensionChange(value: number | string | null): void {
    this.montantPensionMensuel.set(this.parseNonNegativeInt(value));
    this.provenanceMontantPension.set(null);
  }

  onPensionPayeeChange(value: PensionPayee | null): void {
    this.pensionPayee.set(value);
    this.provenancePensionPayee.set(null);
  }

  onNbEnfantsChange(value: number | string | null): void {
    this.nbEnfantsACharge.set(this.parseNonNegativeInt(value));
    this.provenanceNbEnfants.set(null);
  }

  onAutreParentChange(checked: boolean): void {
    this.autreParentDecedeOuInconnu.set(checked);
  }

  private parseNonNegativeInt(value: number | string | null | undefined): number | null {
    if (value === null || value === undefined || value === '') return null;
    const num = typeof value === 'number' ? value : Number.parseInt(String(value), 10);
    if (!Number.isFinite(num) || num < 0) return null;
    return num;
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request: AsfCafRequest = {
      parentIsole: this.parentIsole(),
      pensionFixee: this.pensionFixee(),
      montantPensionMensuel: this.montantPensionMensuel(),
      pensionPayee: this.pensionPayee(),
      nbEnfantsACharge: this.nbEnfantsACharge(),
      autreParentDecedeOuInconnu: this.autreParentDecedeOuInconnu(),
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.applyResult(r);
        this.calculating.set(false);
        this.snackBar.open('Droit ASF évalué', 'OK', { duration: 2500 });
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

  private applyResult(r: AsfCafResponse): void {
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

  /** Libellé humain pour le verdict. */
  verdictLabel(verdict: VerdictAsf): string {
    const labels: Record<VerdictAsf, string> = {
      DROIT_ASF_PLEIN: 'Droit à l\'ASF à taux plein',
      DROIT_ASF_DIFFERENTIELLE: 'Droit à l\'ASF différentielle',
      DROIT_AVEC_RECOUVREMENT: 'Droit à l\'ASF avec recouvrement',
      PAS_DE_DROIT: 'Pas de droit à l\'ASF',
    };
    return labels[verdict];
  }

  /** Classe CSS par verdict pour la pastille. */
  verdictClass(verdict: VerdictAsf): string {
    const cls: Record<VerdictAsf, string> = {
      DROIT_ASF_PLEIN: 'verdict-success',
      DROIT_ASF_DIFFERENTIELLE: 'verdict-primary',
      DROIT_AVEC_RECOUVREMENT: 'verdict-info',
      PAS_DE_DROIT: 'verdict-warning',
    };
    return cls[verdict];
  }
}
