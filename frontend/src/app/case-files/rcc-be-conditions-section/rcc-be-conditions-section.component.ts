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
  Condition,
  RccBeConditionsRequest,
  RccBeConditionsResponse,
  Regime,
  Verdict,
  humanizeCondition,
  humanizeRegime,
  humanizeVerdict,
} from '../../core/models/rcc-be-conditions.model';
import { RccBeConditionsService } from '../../core/services/rcc-be-conditions.service';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { RccBeConditionsPrefillRules } from './rcc-be-conditions-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-207-06b : outil décisionnel "RCC BE — conditions d'éligibilité" (F-207).
 * BE uniquement (Régime de Chômage avec Complément d'entreprise, ex-prépension —
 * CCT 17, CCT 17/13, AR 03/05/2007). Consomme l'API SF-207-06 (PR #1151).
 *
 * Affiché conditionnellement par le panel F-IA-04 (tool_id `rcc-be-conditions`,
 * règle ALWAYS_ON BE + travail).
 *
 * Pattern canonique : `refere-tribunal-travail-be-section` (SF-207-05b PR #1149)
 * — verdict 3 états coloré + section conditions humanisées. Spécificités RCC :
 * 4 régimes parallèles (régime applicable + régimes éligibles cumulés) +
 * calculs annexes (âge à la date de licenciement + années de carrière).
 */
@Component({
  selector: 'app-rcc-be-conditions-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatCheckboxModule,
    MatProgressSpinnerModule,    ToolJurisprudenceCitationsComponent,
  ],
  templateUrl: './rcc-be-conditions-section.component.html',
  styleUrl: './rcc-be-conditions-section.component.scss',
})
export class RccBeConditionsSectionComponent implements OnInit, OnChanges {
  // F-JU-03 SF-JU-03-99f — citations jurisprudentielles F-JU-01 (BE).
  protected readonly toolIdForJurisprudence = 'rcc-be-conditions';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'RCC BE — CONDITIONS D\'ÉLIGIBILITÉ';
  static readonly TOOL_ICON = 'elderly';

  /** F-177 SF-177-12 / F-236 SF-236-02 — délègue au helper partagé (parité runtime). */
  static getPrefillCount(input: PrefillCountInput): number {
    return RccBeConditionsPrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() forceExpanded = false;
  /** Gate strict BE — l'outil n'a pas d'équivalent FR (RCC = ex-prépension belge). */
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'BELGIQUE';
  @Input() aiData?: TravailExtractedData | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;

  // Exposé au template pour humanisation.
  readonly humanizeVerdict = humanizeVerdict;
  readonly humanizeRegime = humanizeRegime;
  readonly humanizeCondition = humanizeCondition;

  private cdr = inject(ChangeDetectorRef);

  // Snapshots signal des inputs IA pour réactivité computed.
  private aiDataSignal = signal<TravailExtractedData | null | undefined>(undefined);

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<RccBeConditionsResponse | null>(null);

  // Champs du form (signals — édités via handlers).
  dateNaissance = signal<string | null>(null);
  anneesCarriereProfessionnelle = signal<number | null>(null);
  metierLourd = signal<boolean>(false);
  longueCarriere = signal<boolean>(false);
  entrepriseEnDifficulte = signal<boolean>(false);
  dateLicenciementEnvisagee = signal<string | null>(null);

  // Provenance IA par champ pré-rempli.
  provenanceDateNaissance = signal<'IA' | null>(null);
  provenanceAnneesCarriere = signal<'IA' | null>(null);
  provenanceMetierLourd = signal<'IA' | null>(null);
  provenanceEntrepriseEnDifficulte = signal<'IA' | null>(null);

  /** Gate strict workspace BE (l'outil n'existe pas côté FR). */
  isAvailable = computed(() => this.workspaceCountry === 'BELGIQUE');

  constructor(
    private service: RccBeConditionsService,
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
   * SF-207-06b — Pré-remplit le form depuis `aiData`. Délègue intégralement
   * au helper pur partagé pour garantir la parité runtime / static
   * `getPrefillCount` (divergence impossible par construction).
   */
  private prefillFromAi(): void {
    const ai = this.aiDataSignal();
    if (!ai) return;
    if (this.workspaceCountry !== 'BELGIQUE') return;

    const ruleInput: PrefillCountInput = { aiData: ai, workspaceCountry: this.workspaceCountry };

    // 1. dateNaissance
    const dn = RccBeConditionsPrefillRules.computeDateNaissance(ruleInput);
    if (dn !== null && (this.dateNaissance() === null || this.provenanceDateNaissance() === 'IA')) {
      this.dateNaissance.set(dn);
      this.provenanceDateNaissance.set('IA');
    }

    // 2. anneesCarriereProfessionnelle
    const ac = RccBeConditionsPrefillRules.computeAnneesCarriere(ruleInput);
    if (ac !== null && (this.anneesCarriereProfessionnelle() === null || this.provenanceAnneesCarriere() === 'IA')) {
      this.anneesCarriereProfessionnelle.set(ac);
      this.provenanceAnneesCarriere.set('IA');
    }

    // 3. metierLourd (true only)
    const ml = RccBeConditionsPrefillRules.computeMetierLourd(ruleInput);
    if (ml === true && (this.provenanceMetierLourd() === null || this.provenanceMetierLourd() === 'IA')) {
      this.metierLourd.set(true);
      this.provenanceMetierLourd.set('IA');
    }

    // 4. entrepriseEnDifficulte (true only)
    const ed = RccBeConditionsPrefillRules.computeEntrepriseEnDifficulte(ruleInput);
    if (ed === true && (this.provenanceEntrepriseEnDifficulte() === null || this.provenanceEntrepriseEnDifficulte() === 'IA')) {
      this.entrepriseEnDifficulte.set(true);
      this.provenanceEntrepriseEnDifficulte.set('IA');
    }
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  formValid(): boolean {
    if (!this.dateNaissance()) return false;
    const ac = this.anneesCarriereProfessionnelle();
    if (ac === null || ac === undefined || Number.isNaN(ac)) return false;
    if (ac < 0 || ac > 60) return false;
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // -- Handlers --
  onDateNaissanceChange(value: string | null): void {
    this.dateNaissance.set(value);
    this.provenanceDateNaissance.set(null);
  }
  onAnneesCarriereChange(value: number | null): void {
    this.anneesCarriereProfessionnelle.set(value);
    this.provenanceAnneesCarriere.set(null);
  }
  onMetierLourdChange(value: boolean): void {
    this.metierLourd.set(value);
    this.provenanceMetierLourd.set(null);
  }
  onLongueCarriereChange(value: boolean): void {
    this.longueCarriere.set(value);
  }
  onEntrepriseEnDifficulteChange(value: boolean): void {
    this.entrepriseEnDifficulte.set(value);
    this.provenanceEntrepriseEnDifficulte.set(null);
  }
  onDateLicenciementEnvisageeChange(value: string | null): void {
    this.dateLicenciementEnvisagee.set(value);
  }

  /** Classe CSS du verdict (vert ELIGIBLE / ambre INCERTAIN / rouge NON_ELIGIBLE). */
  verdictClass(verdict: Verdict): string {
    switch (verdict) {
      case 'ELIGIBLE': return 'verdict-ok';
      case 'INCERTAIN': return 'verdict-warn';
      case 'NON_ELIGIBLE': return 'verdict-critical';
    }
  }

  verdictIcon(verdict: Verdict): string {
    switch (verdict) {
      case 'ELIGIBLE': return 'check_circle';
      case 'INCERTAIN': return 'warning';
      case 'NON_ELIGIBLE': return 'block';
    }
  }

  /**
   * Liste humanisée des conditions manquantes (FR) — alimente la liste à puces
   * sous le verdict (vide si verdict ELIGIBLE).
   */
  conditionsManquantesHumanisees(): { code: Condition; label: string }[] {
    const r = this.result();
    if (!r) return [];
    return r.conditionsManquantes.map(c => ({ code: c, label: humanizeCondition(c) }));
  }

  /** Liste humanisée des régimes éligibles (affichée si > 1 régime). */
  regimesEligiblesHumanises(): { code: Regime; label: string }[] {
    const r = this.result();
    if (!r) return [];
    return r.regimesEligibles.map(reg => ({ code: reg, label: humanizeRegime(reg) }));
  }

  calculate(): void {
    if (!this.formValid()) return;
    if (this.workspaceCountry !== 'BELGIQUE') {
      this.snackBar.open('Outil indisponible pour ce workspace', 'Fermer',
        { duration: 4000, panelClass: 'snack-error' });
      return;
    }
    const request: RccBeConditionsRequest = {
      dateNaissance: this.dateNaissance()!,
      anneesCarriereProfessionnelle: this.anneesCarriereProfessionnelle()!,
      metierLourd: this.metierLourd(),
      longueCarriere: this.longueCarriere(),
      entrepriseEnDifficulte: this.entrepriseEnDifficulte(),
      dateLicenciementEnvisagee: this.dateLicenciementEnvisagee() || null,
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        // Refresh dashboard décisionnel (pattern SF-IA-02-03).
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
        this.dateNaissance.set(r.dateNaissance);
        this.anneesCarriereProfessionnelle.set(r.anneesCarriereProfessionnelle);
        this.metierLourd.set(r.metierLourd);
        this.longueCarriere.set(r.longueCarriere);
        this.entrepriseEnDifficulte.set(r.entrepriseEnDifficulte);
        this.dateLicenciementEnvisagee.set(r.dateLicenciementEnvisagee);
        // Valeurs persistées = saisie avocat — jamais de badge IA.
        this.provenanceDateNaissance.set(null);
        this.provenanceAnneesCarriere.set(null);
        this.provenanceMetierLourd.set(null);
        this.provenanceEntrepriseEnDifficulte.set(null);
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
