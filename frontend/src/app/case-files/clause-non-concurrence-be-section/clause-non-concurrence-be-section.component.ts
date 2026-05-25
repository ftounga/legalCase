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
import { MatCheckboxModule } from '@angular/material/checkbox';
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
  ClauseNonConcurrenceBeRequest,
  ClauseNonConcurrenceBeResponse,
  ClauseNonConcurrenceBeVerdict,
  ClauseNonConcurrenceBeZone,
  humanizeRaisonNullite,
  humanizeVerdict,
  humanizeZoneGeographique,
} from '../../core/models/clause-non-concurrence-be.model';
import { ClauseNonConcurrenceBeService } from '../../core/services/clause-non-concurrence-be.service';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { ClauseNonConcurrenceBeSectionPrefillRules } from './clause-non-concurrence-be-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-213-01b : outil décisionnel "Clause de non-concurrence BE" (F-213).
 * BE uniquement (Loi du 3 juillet 1978 art. 65 + CCT n°13 du 24/02/1971).
 * Consomme l'API SF-213-01 backend.
 *
 * <p>Affiché conditionnellement par le panel F-IA-04 (tool_id
 * `clause-non-concurrence-be`, règle CONTEXTUAL BE — trigger
 * `clause_non_concurrence_presente=true`).</p>
 *
 * <p>Pattern miroir : `outplacement-be-obligatoire-45-section` (SF-207-08b) —
 * verdict 3 états + sanction quantifiée + base juridique mono. Spécificités :
 * indemnité légale obligatoire (= ½ rémunération brute × durée), distinct
 * du FR (F-DT-24, montant libre).</p>
 */
@Component({
  selector: 'app-clause-non-concurrence-be-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatCheckboxModule,
    MatSelectModule,
    MatProgressSpinnerModule,
    ToolJurisprudenceCitationsComponent,
  ],
  templateUrl: './clause-non-concurrence-be-section.component.html',
  styleUrl: './clause-non-concurrence-be-section.component.scss',
})
export class ClauseNonConcurrenceBeSectionComponent implements OnInit, OnChanges {
  // F-JU-03 — citations jurisprudentielles F-JU-01 (BE).
  protected readonly toolIdForJurisprudence = 'clause-non-concurrence-be';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'CLAUSE DE NON-CONCURRENCE (BE)';
  static readonly TOOL_ICON = 'block';

  /** F-177 SF-177-12 / F-236 SF-236-02 — délègue au helper partagé (parité runtime). */
  static getPrefillCount(input: PrefillCountInput): number {
    return ClauseNonConcurrenceBeSectionPrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() forceExpanded = false;
  /** Gate strict BE — l'outil n'a pas d'équivalent FR direct (F-DT-24 FR ≠ régime). */
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'BELGIQUE';
  @Input() aiData?: TravailExtractedData | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;

  // Exposé au template.
  readonly humanizeRaisonNullite = humanizeRaisonNullite;
  readonly humanizeVerdict = humanizeVerdict;
  readonly humanizeZoneGeographique = humanizeZoneGeographique;

  /** Whitelist des zones (alimente le mat-select). */
  readonly zonesGeographiques: ReadonlyArray<ClauseNonConcurrenceBeZone> = [
    'BELGIQUE_UNIQUEMENT',
    'BELGIQUE_ET_ETRANGER',
    'NON_SPECIFIEE',
  ];

  private cdr = inject(ChangeDetectorRef);

  // Snapshot signal de aiData pour réactivité computed (parité pattern outplacement-be).
  private aiDataSignal = signal<TravailExtractedData | null | undefined>(undefined);

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<ClauseNonConcurrenceBeResponse | null>(null);

  // ── Champs du form (signals — édités via handlers). ────────────────────
  remunerationAnnuelleBrute = signal<number | null>(null);
  dureeMois = signal<number | null>(null);
  zoneGeographique = signal<ClauseNonConcurrenceBeZone | null>(null);
  activiteInternationaleProuvee = signal<boolean>(false);

  // Provenance IA par champ pré-rempli (3 instrumentés).
  provenanceRemuneration = signal<'IA' | null>(null);
  provenanceDureeMois = signal<'IA' | null>(null);
  provenanceZone = signal<'IA' | null>(null);

  /** Gate strict workspace BE (l'outil n'existe pas côté FR — régime distinct). */
  isAvailable = computed(() => this.workspaceCountry === 'BELGIQUE');

  // Formateur €  (fr-BE) instancié une fois.
  private readonly euroFormatter = new Intl.NumberFormat('fr-BE', {
    style: 'currency',
    currency: 'EUR',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });

  constructor(
    private service: ClauseNonConcurrenceBeService,
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
   * SF-213-01b — Pré-remplit le form depuis `aiData`. Délègue au helper pur
   * partagé pour garantir la parité runtime / static `getPrefillCount`
   * (divergence impossible par construction).
   */
  private prefillFromAi(): void {
    const ai = this.aiDataSignal();
    if (!ai) return;
    if (this.workspaceCountry !== 'BELGIQUE') return;

    const ruleInput: PrefillCountInput = { aiData: ai, workspaceCountry: this.workspaceCountry };

    // 1. remunerationAnnuelleBrute
    const remu = ClauseNonConcurrenceBeSectionPrefillRules.computeRemunerationAnnuelleBrute(ruleInput);
    if (remu !== null && (this.remunerationAnnuelleBrute() === null || this.provenanceRemuneration() === 'IA')) {
      this.remunerationAnnuelleBrute.set(remu);
      this.provenanceRemuneration.set('IA');
    }

    // 2. dureeMois
    const duree = ClauseNonConcurrenceBeSectionPrefillRules.computeDureeMois(ruleInput);
    if (duree !== null && (this.dureeMois() === null || this.provenanceDureeMois() === 'IA')) {
      this.dureeMois.set(duree);
      this.provenanceDureeMois.set('IA');
    }

    // 3. zoneGeographique
    const zone = ClauseNonConcurrenceBeSectionPrefillRules.computeZoneGeographique(ruleInput);
    if (zone !== null && (this.zoneGeographique() === null || this.provenanceZone() === 'IA')) {
      this.zoneGeographique.set(zone);
      this.provenanceZone.set('IA');
    }
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  formValid(): boolean {
    const remu = this.remunerationAnnuelleBrute();
    if (remu === null || remu === undefined || Number.isNaN(remu) || remu <= 0) return false;
    const duree = this.dureeMois();
    if (duree === null || duree === undefined || !Number.isInteger(duree) || duree < 1 || duree > 12) return false;
    if (!this.zoneGeographique()) return false;
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // -- Handlers — effacent la provenance IA au 1er changement manuel. --
  onRemunerationChange(value: number | null): void {
    this.remunerationAnnuelleBrute.set(value);
    this.provenanceRemuneration.set(null);
  }
  onDureeMoisChange(value: number | null): void {
    this.dureeMois.set(value);
    this.provenanceDureeMois.set(null);
  }
  onZoneChange(value: ClauseNonConcurrenceBeZone | null): void {
    this.zoneGeographique.set(value);
    this.provenanceZone.set(null);
  }
  onActiviteInternationaleChange(value: boolean): void {
    this.activiteInternationaleProuvee.set(value);
  }

  /** Classe CSS du verdict (vert / rouge / ambre — aligné DESIGN_SYSTEM). */
  verdictClass(verdict: ClauseNonConcurrenceBeVerdict): string {
    switch (verdict) {
      case 'VALIDE': return 'verdict-ok';
      case 'NULLE': return 'verdict-critical';
      case 'PARTIELLEMENT_NULLE': return 'verdict-warn';
    }
  }

  verdictIcon(verdict: ClauseNonConcurrenceBeVerdict): string {
    switch (verdict) {
      case 'VALIDE': return 'check_circle';
      case 'NULLE': return 'error';
      case 'PARTIELLEMENT_NULLE': return 'warning';
    }
  }

  /**
   * Formate un montant € au format fr-BE (séparateurs locaux + 2 décimales).
   * `null` / `undefined` / `NaN` → '—' (état vide gracieux).
   */
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
    const request: ClauseNonConcurrenceBeRequest = {
      remunerationAnnuelleBrute: this.remunerationAnnuelleBrute()!,
      dureeMois: this.dureeMois()!,
      zoneGeographique: this.zoneGeographique()!,
      activiteInternationaleProuvee: this.activiteInternationaleProuvee(),
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Clause de non-concurrence BE analysée', 'OK', { duration: 2500 });
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
        // Le GET ne re-pose pas le détail des inputs (pattern miroir motif-grave),
        // mais le verdict suffit pour rendre l'écran récap. L'avocat peut cliquer
        // "Modifier" pour réouvrir le form avec pré-fill IA frais.
        // Reset provenance — valeurs persistées = saisie avocat, pas badge IA.
        this.provenanceRemuneration.set(null);
        this.provenanceDureeMois.set(null);
        this.provenanceZone.set(null);
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
