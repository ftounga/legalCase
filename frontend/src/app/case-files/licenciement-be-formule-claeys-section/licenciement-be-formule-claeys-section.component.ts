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
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
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
  LicenciementBeFormuleClaeysRequest,
  LicenciementBeFormuleClaeysResponse,
} from '../../core/models/licenciement-be-formule-claeys.model';
import {
  LicenciementBeFormuleClaeysService,
} from '../../core/services/licenciement-be-formule-claeys.service';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import {
  LicenciementBeFormuleClaeysSectionPrefillRules,
} from './licenciement-be-formule-claeys-section-prefill-rules';
import {
  ToolJurisprudenceCitationsComponent,
} from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-213-04b : outil décisionnel "Préavis Formule Claeys BE" (F-213).
 *
 * BE uniquement — ancien art. 82 §3 Loi 03/07/1978 sur les contrats de
 * travail (avant statut unique 01/01/2014), consacré par Cass. 28/02/2011
 * (RG S.10.0073.F) et combinable avec le préavis statut unique post-2014
 * via la clause de sauvegarde de la Loi 26/12/2013 art. 67.
 *
 * <p>Affiché par le panel F-IA-04 (tool_id `licenciement-be-formule-claeys`,
 * visibility ALWAYS_ON priority 112 BE / DROIT_DU_TRAVAIL — priorité juste
 * au-dessus de `licenciement-be-statut-unique-preavis` (111). Pattern
 * miroir : `licenciement-be-statut-unique-preavis-section` (SF-213-03b).</p>
 *
 * <p><b>Spécificité UX</b> : champs conditionnels post-2014 via
 * {@code mat-slide-toggle appliquerClauseSauvegarde} — quand le toggle est
 * désactivé, les champs sont masqués + reset à null + non envoyés au POST.
 * Bannière `avertissement` toujours présente (le backend renvoie
 * systématiquement le rappel jurisprudentiel Cass. 14/12/2015).</p>
 */
@Component({
  selector: 'app-licenciement-be-formule-claeys-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatCheckboxModule, MatSlideToggleModule,
    MatProgressSpinnerModule,
    ToolJurisprudenceCitationsComponent,
  ],
  templateUrl: './licenciement-be-formule-claeys-section.component.html',
  styleUrl: './licenciement-be-formule-claeys-section.component.scss',
})
export class LicenciementBeFormuleClaeysSectionComponent
  implements OnInit, OnChanges {

  // F-JU-03 — citations jurisprudentielles F-JU-01 (BE).
  protected readonly toolIdForJurisprudence = 'licenciement-be-formule-claeys';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'PRÉAVIS FORMULE CLAEYS (BE)';
  static readonly TOOL_ICON = 'history_edu';

  /** F-177 SF-177-12 / F-236 SF-236-02 — délègue au helper partagé (parité runtime). */
  static getPrefillCount(input: PrefillCountInput): number {
    return LicenciementBeFormuleClaeysSectionPrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() forceExpanded = false;
  /** Gate strict BE — l'outil n'a pas d'équivalent FR direct (régime distinct). */
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'BELGIQUE';
  @Input() aiData?: TravailExtractedData | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;

  private cdr = inject(ChangeDetectorRef);

  // Snapshot signal de aiData pour réactivité (parité pattern).
  private aiDataSignal = signal<TravailExtractedData | null | undefined>(undefined);

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<LicenciementBeFormuleClaeysResponse | null>(null);

  // ── Champs du form (signals — édités via handlers). ────────────────────
  ancienneteAnneesPreStatutUnique = signal<number | null>(null);
  ancienneteMoisPreStatutUnique = signal<number | null>(null);
  remunerationAnnuelleBruteEnMilliers = signal<number | null>(null);
  /** Défaut UI true (parité backend defaultIfNull). */
  appliquerClauseSauvegarde = signal<boolean>(true);
  ancienneteAnneesPostStatutUnique = signal<number | null>(null);
  salaireHebdomadaireBrut = signal<number | null>(null);

  // Provenance IA par champ pré-rempli.
  provenanceAncienneteEPre = signal<'IA' | null>(null);
  provenanceRemunerationAnnuelle = signal<'IA' | null>(null);
  provenanceClauseSauvegarde = signal<'IA' | null>(null);
  provenanceAncienneteEPost = signal<'IA' | null>(null);
  provenanceSalaireHebdo = signal<'IA' | null>(null);

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
    private service: LicenciementBeFormuleClaeysService,
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
   * SF-213-04b — Pré-remplit le form depuis `aiData` via le helper partagé.
   * Parité runtime / static `getPrefillCount` garantie par construction.
   */
  private prefillFromAi(): void {
    const ai = this.aiDataSignal();
    if (!ai) return;
    if (this.workspaceCountry !== 'BELGIQUE') return;

    const ruleInput: PrefillCountInput = { aiData: ai, workspaceCountry: this.workspaceCountry };

    // 1. ancienneté pré-statut unique (couple années/mois — 1 source).
    const anneesPre = LicenciementBeFormuleClaeysSectionPrefillRules.computeAncienneteAnneesPreStatutUnique(ruleInput);
    const moisPre = LicenciementBeFormuleClaeysSectionPrefillRules.computeAncienneteMoisPreStatutUnique(ruleInput);
    if (anneesPre !== null
        && (this.ancienneteAnneesPreStatutUnique() === null || this.provenanceAncienneteEPre() === 'IA')) {
      this.ancienneteAnneesPreStatutUnique.set(anneesPre);
      this.ancienneteMoisPreStatutUnique.set(moisPre ?? 0);
      this.provenanceAncienneteEPre.set('IA');
    }

    // 2. rémunération annuelle brute en K€.
    const remu = LicenciementBeFormuleClaeysSectionPrefillRules.computeRemunerationAnnuelleBruteEnMilliers(ruleInput);
    if (remu !== null
        && (this.remunerationAnnuelleBruteEnMilliers() === null || this.provenanceRemunerationAnnuelle() === 'IA')) {
      this.remunerationAnnuelleBruteEnMilliers.set(remu);
      this.provenanceRemunerationAnnuelle.set('IA');
    }

    // 3. clause de sauvegarde (dérivé) — toggle UX, non compté badge.
    const clause = LicenciementBeFormuleClaeysSectionPrefillRules.computeAppliquerClauseSauvegarde(ruleInput);
    if (clause !== null && this.provenanceClauseSauvegarde() === null) {
      this.appliquerClauseSauvegarde.set(clause);
      this.provenanceClauseSauvegarde.set('IA');
    }

    // 4. ancienneté post (uniquement si clause de sauvegarde activée).
    const anneesPost = LicenciementBeFormuleClaeysSectionPrefillRules.computeAncienneteAnneesPostStatutUnique(ruleInput);
    if (anneesPost !== null
        && this.appliquerClauseSauvegarde()
        && (this.ancienneteAnneesPostStatutUnique() === null || this.provenanceAncienneteEPost() === 'IA')) {
      this.ancienneteAnneesPostStatutUnique.set(anneesPost);
      this.provenanceAncienneteEPost.set('IA');
    }

    // 5. salaire hebdo brut (uniquement si clause de sauvegarde activée).
    const hebdo = LicenciementBeFormuleClaeysSectionPrefillRules.computeSalaireHebdomadaireBrut(ruleInput);
    if (hebdo !== null
        && this.appliquerClauseSauvegarde()
        && (this.salaireHebdomadaireBrut() === null || this.provenanceSalaireHebdo() === 'IA')) {
      this.salaireHebdomadaireBrut.set(hebdo);
      this.provenanceSalaireHebdo.set('IA');
    }
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  formValid(): boolean {
    const anneesPre = this.ancienneteAnneesPreStatutUnique();
    if (anneesPre === null || anneesPre === undefined || Number.isNaN(anneesPre)
        || anneesPre < 0 || anneesPre > 80) return false;
    const moisPre = this.ancienneteMoisPreStatutUnique();
    if (moisPre !== null && moisPre !== undefined
        && (Number.isNaN(moisPre) || moisPre < 0 || moisPre > 11)) return false;
    const remu = this.remunerationAnnuelleBruteEnMilliers();
    if (remu === null || remu === undefined || Number.isNaN(remu) || remu <= 0) return false;

    // Si clause de sauvegarde activée, exiger les 2 champs conditionnels.
    if (this.appliquerClauseSauvegarde()) {
      const anneesPost = this.ancienneteAnneesPostStatutUnique();
      if (anneesPost === null || anneesPost === undefined || Number.isNaN(anneesPost)
          || anneesPost < 0 || anneesPost > 80) return false;
      const hebdo = this.salaireHebdomadaireBrut();
      if (hebdo === null || hebdo === undefined || Number.isNaN(hebdo) || hebdo <= 0) return false;
    }
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // -- Handlers — effacent la provenance IA au 1er changement manuel. --
  onAncienneteAnneesPreChange(value: number | null): void {
    this.ancienneteAnneesPreStatutUnique.set(value);
    this.provenanceAncienneteEPre.set(null);
  }
  onAncienneteMoisPreChange(value: number | null): void {
    this.ancienneteMoisPreStatutUnique.set(value);
    this.provenanceAncienneteEPre.set(null);
  }
  onRemunerationAnnuelleChange(value: number | null): void {
    this.remunerationAnnuelleBruteEnMilliers.set(value);
    this.provenanceRemunerationAnnuelle.set(null);
  }
  /**
   * Toggle clause de sauvegarde — quand on désactive, on reset les 2
   * champs conditionnels (UI cohérente + non envoyés au POST).
   */
  onAppliquerClauseSauvegardeChange(value: boolean): void {
    this.appliquerClauseSauvegarde.set(value);
    this.provenanceClauseSauvegarde.set(null);
    if (!value) {
      this.ancienneteAnneesPostStatutUnique.set(null);
      this.salaireHebdomadaireBrut.set(null);
      this.provenanceAncienneteEPost.set(null);
      this.provenanceSalaireHebdo.set(null);
    }
  }
  onAncienneteAnneesPostChange(value: number | null): void {
    this.ancienneteAnneesPostStatutUnique.set(value);
    this.provenanceAncienneteEPost.set(null);
  }
  onSalaireHebdoChange(value: number | null): void {
    this.salaireHebdomadaireBrut.set(value);
    this.provenanceSalaireHebdo.set(null);
  }

  /** Formate un montant € au format fr-BE. `null` / `NaN` → '—'. */
  formatEuro(value: number | null | undefined): string {
    if (value === null || value === undefined || Number.isNaN(value)) return '—';
    return this.euroFormatter.format(value);
  }

  /** Formate un nombre décimal (ex: 11.52 mois) au format fr-BE 2 décimales. */
  formatDecimal(value: number | null | undefined): string {
    if (value === null || value === undefined || Number.isNaN(value)) return '—';
    return value.toLocaleString('fr-BE', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }

  calculate(): void {
    if (!this.formValid()) return;
    if (this.workspaceCountry !== 'BELGIQUE') {
      this.snackBar.open('Outil indisponible pour ce workspace', 'Fermer',
        { duration: 4000, panelClass: 'snack-error' });
      return;
    }
    const clause = this.appliquerClauseSauvegarde();
    const request: LicenciementBeFormuleClaeysRequest = {
      ancienneteAnneesPreStatutUnique: this.ancienneteAnneesPreStatutUnique()!,
      ancienneteMoisPreStatutUnique: this.ancienneteMoisPreStatutUnique() ?? 0,
      remunerationAnnuelleBruteEnMilliers: this.remunerationAnnuelleBruteEnMilliers()!,
      appliquerClauseSauvegarde: clause,
      // Toggle off → on ne transmet PAS les 2 champs conditionnels.
      ancienneteAnneesPostStatutUnique: clause ? this.ancienneteAnneesPostStatutUnique()! : null,
      salaireHebdomadaireBrut: clause ? this.salaireHebdomadaireBrut()! : null,
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Préavis Formule Claeys BE calculé', 'OK', { duration: 2500 });
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
        this.provenanceAncienneteEPre.set(null);
        this.provenanceRemunerationAnnuelle.set(null);
        this.provenanceClauseSauvegarde.set(null);
        this.provenanceAncienneteEPost.set(null);
        this.provenanceSalaireHebdo.set(null);
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
