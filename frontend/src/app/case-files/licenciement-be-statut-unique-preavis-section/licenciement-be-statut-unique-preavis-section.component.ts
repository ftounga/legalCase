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
  LicenciementBeStatutUniqueBadge,
  LicenciementBeStatutUniquePreavisRequest,
  LicenciementBeStatutUniquePreavisResponse,
  badgeFromResponse,
  humanizeStatutUniqueBadge,
} from '../../core/models/licenciement-be-statut-unique-preavis.model';
import {
  LicenciementBeStatutUniquePreavisService,
} from '../../core/services/licenciement-be-statut-unique-preavis.service';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import {
  LicenciementBeStatutUniquePreavisSectionPrefillRules,
} from './licenciement-be-statut-unique-preavis-section-prefill-rules';
import {
  ToolJurisprudenceCitationsComponent,
} from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-213-03b : outil décisionnel "Préavis statut unique BE" (F-213).
 *
 * BE uniquement (Loi du 26 décembre 2013 portant introduction d'un statut
 * unique entre ouvriers et employés + art. 37/2 §1er Loi 03/07/1978 sur
 * les contrats de travail — barème par paliers d'ancienneté en semaines).
 * Consomme l'API SF-213-03 backend.
 *
 * <p>Affiché par le panel F-IA-04 (tool_id `licenciement-be-statut-unique-preavis`,
 * visibility ALWAYS_ON BE / DROIT_DU_TRAVAIL — outil transversal sur tout
 * dossier de licenciement BE). Le checkbox `partieStatutUniqueSeulement`
 * gère le distinguo Claeys (mixte pré + post 2014) vs statut unique pur.</p>
 *
 * <p>Pattern miroir : `rappel-salaire-be-section` (SF-213-02b mergée). Le
 * verdict UX est un badge `STATUT_UNIQUE_PUR` (vert) vs
 * `MIXTE_CLAEYS_PLUS_STATUT_UNIQUE` (ambre) — alerte l'avocat que la
 * fraction Claeys reste à calculer dans un second outil (SF-213-04).</p>
 */
@Component({
  selector: 'app-licenciement-be-statut-unique-preavis-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatCheckboxModule,
    MatProgressSpinnerModule,
    ToolJurisprudenceCitationsComponent,
  ],
  templateUrl: './licenciement-be-statut-unique-preavis-section.component.html',
  styleUrl: './licenciement-be-statut-unique-preavis-section.component.scss',
})
export class LicenciementBeStatutUniquePreavisSectionComponent
  implements OnInit, OnChanges {

  // F-JU-03 — citations jurisprudentielles F-JU-01 (BE).
  protected readonly toolIdForJurisprudence = 'licenciement-be-statut-unique-preavis';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'PRÉAVIS STATUT UNIQUE (BE)';
  static readonly TOOL_ICON = 'event_busy';

  /** F-177 SF-177-12 / F-236 SF-236-02 — délègue au helper partagé (parité runtime). */
  static getPrefillCount(input: PrefillCountInput): number {
    return LicenciementBeStatutUniquePreavisSectionPrefillRules.computePrefillCount(input);
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
  readonly humanizeStatutUniqueBadge = humanizeStatutUniqueBadge;
  readonly badgeFromResponse = badgeFromResponse;

  private cdr = inject(ChangeDetectorRef);

  // Snapshot signal de aiData pour réactivité computed (parité pattern).
  private aiDataSignal = signal<TravailExtractedData | null | undefined>(undefined);

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<LicenciementBeStatutUniquePreavisResponse | null>(null);

  // ── Champs du form (signals — édités via handlers). ────────────────────
  ancienneteAnnees = signal<number | null>(null);
  ancienneteMoisSupplementaires = signal<number | null>(null);
  salaireHebdomadaireBrut = signal<number | null>(null);
  dateNotificationLicenciement = signal<string | null>(null);
  /** Défaut UI : true (statut unique pur, parité backend defaultIfNull). */
  partieStatutUniqueSeulement = signal<boolean>(true);

  // Provenance IA par champ pré-rempli (4 instrumentés + flag dérivé).
  provenanceAnciennete = signal<'IA' | null>(null);
  provenanceSalaireHebdo = signal<'IA' | null>(null);
  provenanceDateNotification = signal<'IA' | null>(null);
  provenancePartieStatutUnique = signal<'IA' | null>(null);

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
    private service: LicenciementBeStatutUniquePreavisService,
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
   * SF-213-03b — Pré-remplit le form depuis `aiData` via le helper partagé.
   * Parité runtime / static `getPrefillCount` garantie par construction.
   */
  private prefillFromAi(): void {
    const ai = this.aiDataSignal();
    if (!ai) return;
    if (this.workspaceCountry !== 'BELGIQUE') return;

    const ruleInput: PrefillCountInput = { aiData: ai, workspaceCountry: this.workspaceCountry };

    // 1. ancienneté (couple années/mois — pré-fillés ensemble depuis 1 source).
    const annees = LicenciementBeStatutUniquePreavisSectionPrefillRules.computeAncienneteAnnees(ruleInput);
    const mois = LicenciementBeStatutUniquePreavisSectionPrefillRules.computeAncienneteMoisSupplementaires(ruleInput);
    if (annees !== null
        && (this.ancienneteAnnees() === null || this.provenanceAnciennete() === 'IA')) {
      this.ancienneteAnnees.set(annees);
      this.ancienneteMoisSupplementaires.set(mois ?? 0);
      this.provenanceAnciennete.set('IA');
    }

    // 2. salaire hebdomadaire brut.
    const salaire = LicenciementBeStatutUniquePreavisSectionPrefillRules.computeSalaireHebdomadaireBrut(ruleInput);
    if (salaire !== null
        && (this.salaireHebdomadaireBrut() === null || this.provenanceSalaireHebdo() === 'IA')) {
      this.salaireHebdomadaireBrut.set(salaire);
      this.provenanceSalaireHebdo.set('IA');
    }

    // 3. date de notification licenciement.
    const dateNotif = LicenciementBeStatutUniquePreavisSectionPrefillRules.computeDateNotificationLicenciement(ruleInput);
    if (dateNotif !== null
        && (this.dateNotificationLicenciement() === null || this.provenanceDateNotification() === 'IA')) {
      this.dateNotificationLicenciement.set(dateNotif);
      this.provenanceDateNotification.set('IA');
    }

    // 4. partieStatutUniqueSeulement (dérivé) — n'est pas compté dans le badge
    // mais piloté par dateEntree pour signaler la fraction Claeys.
    const flag = LicenciementBeStatutUniquePreavisSectionPrefillRules.computePartieStatutUniqueSeulement(ruleInput);
    if (flag !== null && this.provenancePartieStatutUnique() !== null
        && this.provenancePartieStatutUnique() !== 'IA') {
      // Avocat a déjà touché → respect.
    } else if (flag !== null) {
      this.partieStatutUniqueSeulement.set(flag);
      this.provenancePartieStatutUnique.set('IA');
    }
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  formValid(): boolean {
    const annees = this.ancienneteAnnees();
    if (annees === null || annees === undefined || Number.isNaN(annees) || annees < 0 || annees > 80) return false;
    const mois = this.ancienneteMoisSupplementaires();
    if (mois !== null && mois !== undefined && (Number.isNaN(mois) || mois < 0 || mois > 11)) return false;
    const salaire = this.salaireHebdomadaireBrut();
    if (salaire === null || salaire === undefined || Number.isNaN(salaire) || salaire <= 0) return false;
    if (!this.dateNotificationLicenciement()) return false;
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // -- Handlers — effacent la provenance IA au 1er changement manuel. --
  onAncienneteAnneesChange(value: number | null): void {
    this.ancienneteAnnees.set(value);
    this.provenanceAnciennete.set(null);
  }
  onAncienneteMoisChange(value: number | null): void {
    this.ancienneteMoisSupplementaires.set(value);
    this.provenanceAnciennete.set(null);
  }
  onSalaireHebdoChange(value: number | null): void {
    this.salaireHebdomadaireBrut.set(value);
    this.provenanceSalaireHebdo.set(null);
  }
  onDateNotificationChange(value: string | null): void {
    this.dateNotificationLicenciement.set(value || null);
    this.provenanceDateNotification.set(null);
  }
  onPartieStatutUniqueChange(value: boolean): void {
    this.partieStatutUniqueSeulement.set(value);
    this.provenancePartieStatutUnique.set(null);
  }

  /** Classe CSS du badge (vert / ambre). */
  badgeClass(b: LicenciementBeStatutUniqueBadge): string {
    return b === 'STATUT_UNIQUE_PUR' ? 'verdict-ok' : 'verdict-warn';
  }

  /** Icône du badge. */
  badgeIcon(b: LicenciementBeStatutUniqueBadge): string {
    return b === 'STATUT_UNIQUE_PUR' ? 'check_circle' : 'warning';
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
    const request: LicenciementBeStatutUniquePreavisRequest = {
      ancienneteAnnees: this.ancienneteAnnees()!,
      ancienneteMoisSupplementaires: this.ancienneteMoisSupplementaires() ?? 0,
      salaireHebdomadaireBrut: this.salaireHebdomadaireBrut()!,
      dateNotificationLicenciement: this.dateNotificationLicenciement()!,
      partieStatutUniqueSeulement: this.partieStatutUniqueSeulement(),
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Préavis statut unique BE calculé', 'OK', { duration: 2500 });
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
        // GET = saisie avocat persistée → reset provenance IA.
        this.provenanceAnciennete.set(null);
        this.provenanceSalaireHebdo.set(null);
        this.provenanceDateNotification.set(null);
        this.provenancePartieStatutUnique.set(null);
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
