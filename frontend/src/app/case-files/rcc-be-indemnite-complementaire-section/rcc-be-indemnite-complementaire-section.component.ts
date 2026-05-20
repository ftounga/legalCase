import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  Input,
  OnChanges,
  OnInit,
  Optional,
  SimpleChanges,
  computed,
  inject,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import {
  PieceManquanteEntry,
  TravailExtractedData,
} from '../../core/models/case-analysis.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { AiQuestion } from '../../core/models/ai-question.model';
import {
  RccBeIndemniteComplementaireRequest,
  RccBeIndemniteComplementaireResponse,
} from '../../core/models/rcc-be-indemnite-complementaire.model';
import { RccBeIndemniteComplementaireService } from '../../core/services/rcc-be-indemnite-complementaire.service';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { RccBeIndemniteComplementairePrefillRules } from './rcc-be-indemnite-complementaire-section-prefill-rules';

/**
 * SF-207-07b : outil décisionnel "RCC BE — indemnité complémentaire" (F-207).
 * BE uniquement (Régime de Chômage avec Complément d'entreprise, ex-prépension —
 * CCT n°17 art. 5 ; Loi du 3 juillet 1978 ; AR du 3 mai 2007). Consomme
 * l'API SF-207-07 (PR #1155).
 *
 * <p>Affiché conditionnellement par le panel F-IA-04 (tool_id
 * `rcc-be-indemnite-complementaire`, règle ALWAYS_ON BE + travail,
 * priority 95).</p>
 *
 * <p>Pattern miroir : `rcc-be-conditions-section` (SF-207-06b PR #1152).
 * Spécificité : calculateur <b>pur</b> sans verdict — affichage de
 * 3 cartes (indemnité mensuelle / durée / coût total) plutôt qu'un badge
 * coloré ELIGIBLE/INCERTAIN/NON_ELIGIBLE.</p>
 */
@Component({
  selector: 'app-rcc-be-indemnite-complementaire-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './rcc-be-indemnite-complementaire-section.component.html',
  styleUrl: './rcc-be-indemnite-complementaire-section.component.scss',
})
export class RccBeIndemniteComplementaireSectionComponent implements OnInit, OnChanges {
  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'RCC BE — INDEMNITÉ COMPLÉMENTAIRE';
  static readonly TOOL_ICON = 'euro';

  /** F-177 SF-177-12 / F-236 SF-236-02 — délègue au helper partagé (parité runtime). */
  static getPrefillCount(input: PrefillCountInput): number {
    return RccBeIndemniteComplementairePrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() forceExpanded = false;
  /** Gate strict BE — pas d'équivalent FR du RCC. */
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'BELGIQUE';
  @Input() aiData?: TravailExtractedData | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;

  private cdr = inject(ChangeDetectorRef);

  // Snapshots signal des inputs IA pour réactivité computed.
  private aiDataSignal = signal<TravailExtractedData | null | undefined>(undefined);

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<RccBeIndemniteComplementaireResponse | null>(null);

  // Champs du form (signals).
  remunerationNetteReference = signal<number | null>(null);
  allocationOnemMensuelle = signal<number | null>(null);
  dateNaissanceSalarie = signal<string | null>(null);
  dateDebutRcc = signal<string | null>(null);
  ageLegalPension = signal<number | null>(null);
  planchersSectoriel = signal<number | null>(null);

  // Provenance IA par champ pré-rempli (4 instrumentés).
  provenanceRemunerationNette = signal<'IA' | null>(null);
  provenanceAllocationOnem = signal<'IA' | null>(null);
  provenanceDateNaissance = signal<'IA' | null>(null);
  provenanceDateDebutRcc = signal<'IA' | null>(null);

  /** Gate strict workspace BE (l'outil n'existe pas côté FR). */
  isAvailable = computed(() => this.workspaceCountry === 'BELGIQUE');

  // Formateur €  (fr-BE) instancié une fois.
  private readonly euroFormatter = new Intl.NumberFormat('fr-BE', {
    style: 'currency',
    currency: 'EUR',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });

  constructor(
    private service: RccBeIndemniteComplementaireService,
    private snackBar: MatSnackBar,
    @Optional() private dashboardRefresh: CaseDashboardRefreshService | null,
  ) {}

  ngOnInit(): void {
    if (this.forceExpanded) this.collapsed.set(false);

    this.aiDataSignal.set(this.aiData);

    if (this.workspaceCountry === 'BELGIQUE') {
      this.load();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['forceExpanded'] && this.forceExpanded) this.collapsed.set(false);

    if (changes['aiData']) this.aiDataSignal.set(this.aiData);

    if (changes['aiData'] && !changes['aiData'].firstChange
        && this.showForm() && !this.result()
        && this.workspaceCountry === 'BELGIQUE') {
      this.prefillFromAi();
    }
  }

  /**
   * SF-207-07b — Pré-remplit le form depuis `aiData`. Délègue intégralement
   * au helper pur partagé pour garantir la parité runtime / static
   * `getPrefillCount` (divergence impossible par construction).
   */
  private prefillFromAi(): void {
    const ai = this.aiDataSignal();
    if (!ai) return;
    if (this.workspaceCountry !== 'BELGIQUE') return;

    const ruleInput: PrefillCountInput = { aiData: ai, workspaceCountry: this.workspaceCountry };

    // 1. remunerationNetteReference
    const remun = RccBeIndemniteComplementairePrefillRules.computeRemunerationNetteReference(ruleInput);
    if (remun !== null && (this.remunerationNetteReference() === null || this.provenanceRemunerationNette() === 'IA')) {
      this.remunerationNetteReference.set(remun);
      this.provenanceRemunerationNette.set('IA');
    }

    // 2. allocationOnemMensuelle
    const alloc = RccBeIndemniteComplementairePrefillRules.computeAllocationOnemMensuelle(ruleInput);
    if (alloc !== null && (this.allocationOnemMensuelle() === null || this.provenanceAllocationOnem() === 'IA')) {
      this.allocationOnemMensuelle.set(alloc);
      this.provenanceAllocationOnem.set('IA');
    }

    // 3. dateNaissanceSalarie
    const dn = RccBeIndemniteComplementairePrefillRules.computeDateNaissanceSalarie(ruleInput);
    if (dn !== null && (this.dateNaissanceSalarie() === null || this.provenanceDateNaissance() === 'IA')) {
      this.dateNaissanceSalarie.set(dn);
      this.provenanceDateNaissance.set('IA');
    }

    // 4. dateDebutRcc
    const ddr = RccBeIndemniteComplementairePrefillRules.computeDateDebutRcc(ruleInput);
    if (ddr !== null && (this.dateDebutRcc() === null || this.provenanceDateDebutRcc() === 'IA')) {
      this.dateDebutRcc.set(ddr);
      this.provenanceDateDebutRcc.set('IA');
    }
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  formValid(): boolean {
    const remun = this.remunerationNetteReference();
    if (remun === null || remun === undefined || Number.isNaN(remun) || remun <= 0) return false;
    const alloc = this.allocationOnemMensuelle();
    if (alloc === null || alloc === undefined || Number.isNaN(alloc) || alloc < 0) return false;
    if (!this.dateNaissanceSalarie()) return false;
    if (!this.dateDebutRcc()) return false;
    const age = this.ageLegalPension();
    if (age !== null && age !== undefined && !Number.isNaN(age) && (age < 60 || age > 75)) return false;
    const planch = this.planchersSectoriel();
    if (planch !== null && planch !== undefined && !Number.isNaN(planch) && planch < 0) return false;
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // -- Handlers --
  onRemunerationNetteChange(value: number | null): void {
    this.remunerationNetteReference.set(value);
    this.provenanceRemunerationNette.set(null);
  }
  onAllocationOnemChange(value: number | null): void {
    this.allocationOnemMensuelle.set(value);
    this.provenanceAllocationOnem.set(null);
  }
  onDateNaissanceChange(value: string | null): void {
    this.dateNaissanceSalarie.set(value);
    this.provenanceDateNaissance.set(null);
  }
  onDateDebutRccChange(value: string | null): void {
    this.dateDebutRcc.set(value);
    this.provenanceDateDebutRcc.set(null);
  }
  onAgeLegalPensionChange(value: number | null): void {
    this.ageLegalPension.set(value);
  }
  onPlanchersSectorielChange(value: number | null): void {
    this.planchersSectoriel.set(value);
  }

  /**
   * Formate un montant € au format fr-BE (séparateurs locaux + 2 décimales).
   * `null` / `undefined` / `NaN` rendus `« — »` (état vide gracieux).
   */
  formatEuro(value: number | null | undefined): string {
    if (value === null || value === undefined || Number.isNaN(value)) return '—';
    return this.euroFormatter.format(value);
  }

  /**
   * Convertit un nombre de mois en libellé humain "X années Y mois".
   * - 0 → "0 mois" (cas RCC après âge pension géré par alerte info séparée).
   * - < 12 → "Y mois".
   * - ≥ 12 mais < 24 → "1 année Y mois" (ou "1 année" si Y=0).
   * - sinon → "X années Y mois" (ou "X années" si Y=0).
   */
  formatDuree(mois: number | null | undefined): string {
    if (mois === null || mois === undefined || Number.isNaN(mois)) return '—';
    const total = Math.max(0, Math.floor(mois));
    if (total === 0) return '0 mois';
    const annees = Math.floor(total / 12);
    const reste = total % 12;
    if (annees === 0) return `${reste} mois`;
    const labelAnnees = annees === 1 ? '1 année' : `${annees} années`;
    if (reste === 0) return labelAnnees;
    return `${labelAnnees} ${reste} mois`;
  }

  calculate(): void {
    if (!this.formValid()) return;
    if (this.workspaceCountry !== 'BELGIQUE') {
      this.snackBar.open('Outil indisponible pour ce workspace', 'Fermer',
        { duration: 4000, panelClass: 'snack-error' });
      return;
    }
    const request: RccBeIndemniteComplementaireRequest = {
      remunerationNetteReference: this.remunerationNetteReference()!,
      allocationOnemMensuelle: this.allocationOnemMensuelle()!,
      dateNaissanceSalarie: this.dateNaissanceSalarie()!,
      dateDebutRcc: this.dateDebutRcc()!,
      ageLegalPension: this.ageLegalPension(),
      planchersSectoriel: this.planchersSectoriel(),
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.dashboardRefresh?.triggerRefresh();
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.calculating.set(false);
        const status = err?.status;
        let msg: string;
        if (status === 404) {
          msg = 'Dossier introuvable';
        } else if (status === 400) {
          msg = err?.error?.message || err?.error || 'Données saisies invalides';
        } else {
          msg = 'Une erreur est survenue. Veuillez réessayer.';
        }
        this.snackBar.open(String(msg), 'Fermer',
          { duration: 5000, panelClass: 'snack-error' });
        this.cdr.markForCheck();
      },
    });
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.remunerationNetteReference.set(r.remunerationNetteReference);
        this.allocationOnemMensuelle.set(r.allocationOnemMensuelle);
        this.dateNaissanceSalarie.set(r.dateNaissanceSalarie);
        this.dateDebutRcc.set(r.dateDebutRcc);
        this.ageLegalPension.set(r.ageLegalPensionApplique);
        this.planchersSectoriel.set(r.planchersSectoriel);
        // Valeurs persistées = saisie avocat — jamais de badge IA.
        this.provenanceRemunerationNette.set(null);
        this.provenanceAllocationOnem.set(null);
        this.provenanceDateNaissance.set(null);
        this.provenanceDateDebutRcc.set(null);
        this.showForm.set(false);
        this.loading.set(false);
        this.cdr.markForCheck();
      },
      error: () => {
        // 404 attendu si aucune analyse — mode formulaire + pré-fill IA.
        this.prefillFromAi();
        this.loading.set(false);
        this.cdr.markForCheck();
      },
    });
  }
}
