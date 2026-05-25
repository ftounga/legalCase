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
import { MatSelectModule } from '@angular/material/select';
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
  RappelSalaireBeRequest,
  RappelSalaireBeResponse,
  RappelSalaireBeStatutPrescription,
  RappelSalaireBeTypeArriere,
  humanizeStatutPrescription,
  humanizeTypeArriere,
} from '../../core/models/rappel-salaire-be.model';
import { RappelSalaireBeService } from '../../core/services/rappel-salaire-be.service';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { RappelSalaireBeSectionPrefillRules } from './rappel-salaire-be-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-213-02b : outil décisionnel "Rappel de salaire BE" (F-213).
 * BE uniquement (Loi 12/04/1965 art. 10 — intérêts moratoires 10 % + Loi
 * 03/07/1978 art. 15 al. 1 — prescription 1 an post-rupture / 5 ans pendant
 * contrat). Consomme l'API SF-213-02 backend.
 *
 * <p>Affiché par le panel F-IA-04 (tool_id `rappel-salaire-be`, visibility
 * ALWAYS_ON BE / DROIT_DU_TRAVAIL — outil très fréquent et transversal).</p>
 *
 * <p>Pattern miroir : `clause-non-concurrence-be-section` (SF-213-01b). Le
 * verdict UX est ici un badge `statutPrescription` 3-états (NON_PRESCRIT
 * vert, IMMINENT ambre, PRESCRIT rouge) — la "santé" du calcul = la
 * faisabilité juridique de l'action.</p>
 */
@Component({
  selector: 'app-rappel-salaire-be-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatSelectModule,
    MatProgressSpinnerModule,
    ToolJurisprudenceCitationsComponent,
  ],
  templateUrl: './rappel-salaire-be-section.component.html',
  styleUrl: './rappel-salaire-be-section.component.scss',
})
export class RappelSalaireBeSectionComponent implements OnInit, OnChanges {
  // F-JU-03 — citations jurisprudentielles F-JU-01 (BE).
  protected readonly toolIdForJurisprudence = 'rappel-salaire-be';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'RAPPEL DE SALAIRE (BE)';
  static readonly TOOL_ICON = 'paid';

  /** F-177 SF-177-12 / F-236 SF-236-02 — délègue au helper partagé (parité runtime). */
  static getPrefillCount(input: PrefillCountInput): number {
    return RappelSalaireBeSectionPrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() forceExpanded = false;
  /** Gate strict BE — l'outil n'a pas d'équivalent FR direct (régime distinct). */
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'BELGIQUE';
  @Input() aiData?: TravailExtractedData | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;

  // Exposé au template.
  readonly humanizeStatutPrescription = humanizeStatutPrescription;
  readonly humanizeTypeArriere = humanizeTypeArriere;

  /** Whitelist des types d'arriéré pour le mat-select. */
  readonly typesArriere: ReadonlyArray<RappelSalaireBeTypeArriere> = [
    'PENDANT_CONTRAT',
    'POST_RUPTURE',
    'MIXTE',
  ];

  private cdr = inject(ChangeDetectorRef);

  // Snapshot signal de aiData pour réactivité computed (parité pattern).
  private aiDataSignal = signal<TravailExtractedData | null | undefined>(undefined);

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<RappelSalaireBeResponse | null>(null);

  // ── Champs du form (signals — édités via handlers). ────────────────────
  montantBrut = signal<number | null>(null);
  dateDebutPeriode = signal<string | null>(null);
  dateFinPeriode = signal<string | null>(null);
  dateRupture = signal<string | null>(null);
  typeArriereEnum = signal<RappelSalaireBeTypeArriere | null>(null);

  // Provenance IA par champ pré-rempli (4 instrumentés + le type dérivé).
  provenanceMontantBrut = signal<'IA' | null>(null);
  provenanceDateDebutPeriode = signal<'IA' | null>(null);
  provenanceDateFinPeriode = signal<'IA' | null>(null);
  provenanceDateRupture = signal<'IA' | null>(null);
  provenanceTypeArriere = signal<'IA' | null>(null);

  /** Gate strict workspace BE. */
  isAvailable = computed(() => this.workspaceCountry === 'BELGIQUE');

  // Formateur € (fr-BE) instancié une fois.
  private readonly euroFormatter = new Intl.NumberFormat('fr-BE', {
    style: 'currency',
    currency: 'EUR',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });

  constructor(
    private service: RappelSalaireBeService,
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

    // Ré-applique le pré-fill si aiData change avant première résolution.
    if (changes['aiData'] && !changes['aiData'].firstChange
        && this.showForm() && !this.result()
        && this.workspaceCountry === 'BELGIQUE') {
      this.prefillFromAi();
    }
  }

  /**
   * SF-213-02b — Pré-remplit le form depuis `aiData` via le helper partagé.
   * Parité runtime / static `getPrefillCount` garantie par construction.
   */
  private prefillFromAi(): void {
    const ai = this.aiDataSignal();
    if (!ai) return;
    if (this.workspaceCountry !== 'BELGIQUE') return;

    const ruleInput: PrefillCountInput = { aiData: ai, workspaceCountry: this.workspaceCountry };

    // 1. montantBrut
    const montant = RappelSalaireBeSectionPrefillRules.computeMontantBrut(ruleInput);
    if (montant !== null && (this.montantBrut() === null || this.provenanceMontantBrut() === 'IA')) {
      this.montantBrut.set(montant);
      this.provenanceMontantBrut.set('IA');
    }

    // 2. dateDebutPeriode
    const dateDeb = RappelSalaireBeSectionPrefillRules.computeDateDebutPeriode(ruleInput);
    if (dateDeb !== null && (this.dateDebutPeriode() === null || this.provenanceDateDebutPeriode() === 'IA')) {
      this.dateDebutPeriode.set(dateDeb);
      this.provenanceDateDebutPeriode.set('IA');
    }

    // 3. dateFinPeriode
    const dateFin = RappelSalaireBeSectionPrefillRules.computeDateFinPeriode(ruleInput);
    if (dateFin !== null && (this.dateFinPeriode() === null || this.provenanceDateFinPeriode() === 'IA')) {
      this.dateFinPeriode.set(dateFin);
      this.provenanceDateFinPeriode.set('IA');
    }

    // 4. dateRupture
    const dateRup = RappelSalaireBeSectionPrefillRules.computeDateRupture(ruleInput);
    if (dateRup !== null && (this.dateRupture() === null || this.provenanceDateRupture() === 'IA')) {
      this.dateRupture.set(dateRup);
      this.provenanceDateRupture.set('IA');
    }

    // 5. typeArriereEnum (dérivé) — n'est pas compté dans le badge mais
    // initialisé pour faciliter le clic « Calculer ».
    const typeArriere = RappelSalaireBeSectionPrefillRules.computeTypeArriereEnum(ruleInput);
    if (typeArriere !== null && (this.typeArriereEnum() === null || this.provenanceTypeArriere() === 'IA')) {
      this.typeArriereEnum.set(typeArriere);
      this.provenanceTypeArriere.set('IA');
    }
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  formValid(): boolean {
    const montant = this.montantBrut();
    if (montant === null || montant === undefined || Number.isNaN(montant) || montant <= 0) return false;
    if (!this.dateDebutPeriode()) return false;
    if (!this.dateFinPeriode()) return false;
    if (!this.typeArriereEnum()) return false;
    // POST_RUPTURE exige dateRupture (parité backend §"Cas d'erreur").
    if (this.typeArriereEnum() === 'POST_RUPTURE' && !this.dateRupture()) return false;
    // Période valide : dateFin >= dateDebut (parité backend).
    const deb = this.dateDebutPeriode()!;
    const fin = this.dateFinPeriode()!;
    if (fin < deb) return false;
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // -- Handlers — effacent la provenance IA au 1er changement manuel. --
  onMontantBrutChange(value: number | null): void {
    this.montantBrut.set(value);
    this.provenanceMontantBrut.set(null);
  }
  onDateDebutPeriodeChange(value: string | null): void {
    this.dateDebutPeriode.set(value || null);
    this.provenanceDateDebutPeriode.set(null);
  }
  onDateFinPeriodeChange(value: string | null): void {
    this.dateFinPeriode.set(value || null);
    this.provenanceDateFinPeriode.set(null);
  }
  onDateRuptureChange(value: string | null): void {
    this.dateRupture.set(value || null);
    this.provenanceDateRupture.set(null);
  }
  onTypeArriereChange(value: RappelSalaireBeTypeArriere | null): void {
    this.typeArriereEnum.set(value);
    this.provenanceTypeArriere.set(null);
  }

  /** Classe CSS du statut de prescription (vert / ambre / rouge). */
  prescriptionClass(s: RappelSalaireBeStatutPrescription): string {
    switch (s) {
      case 'NON_PRESCRIT': return 'verdict-ok';
      case 'IMMINENT': return 'verdict-warn';
      case 'PRESCRIT': return 'verdict-critical';
    }
  }

  prescriptionIcon(s: RappelSalaireBeStatutPrescription): string {
    switch (s) {
      case 'NON_PRESCRIT': return 'check_circle';
      case 'IMMINENT': return 'warning';
      case 'PRESCRIT': return 'error';
    }
  }

  /** Formate un montant € au format fr-BE. `null` / `NaN` → '—'. */
  formatEuro(value: number | null | undefined): string {
    if (value === null || value === undefined || Number.isNaN(value)) return '—';
    return this.euroFormatter.format(value);
  }

  calculate(): void {
    if (!this.formValid()) return;
    if (this.workspaceCountry !== 'BELGIQUE') {
      this.snackBar.open('Outil indisponible pour ce workspace', 'Fermer',
        { duration: 4000, panelClass: 'snack-error' });
      return;
    }
    const request: RappelSalaireBeRequest = {
      montantBrut: this.montantBrut()!,
      dateDebutPeriode: this.dateDebutPeriode()!,
      dateFinPeriode: this.dateFinPeriode()!,
      dateRupture: this.dateRupture() ?? null,
      typeArriereEnum: this.typeArriereEnum()!,
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Rappel de salaire BE calculé', 'OK', { duration: 2500 });
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
        this.showForm.set(false);
        this.loading.set(false);
        // GET ne re-pose pas le détail des inputs — valeurs persistées =
        // saisie avocat, pas badge IA. Reset des provenance.
        this.provenanceMontantBrut.set(null);
        this.provenanceDateDebutPeriode.set(null);
        this.provenanceDateFinPeriode.set(null);
        this.provenanceDateRupture.set(null);
        this.provenanceTypeArriere.set(null);
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
