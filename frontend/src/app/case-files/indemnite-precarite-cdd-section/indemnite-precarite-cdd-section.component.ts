import {
  Component,
  Input,
  OnChanges,
  OnInit,
  Optional,
  SimpleChanges,
  computed,
  signal,
} from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatRadioModule } from '@angular/material/radio';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { IndemnitePrecariteCddService } from '../../core/services/indemnite-precarite-cdd.service';
import {
  CAS_EXCLUSION_OPTIONS,
  CasExclusionCdd,
  IndemnitePrecariteCddRequest,
  IndemnitePrecariteCddResponse,
} from '../../core/models/indemnite-precarite-cdd.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import {
  PieceManquanteEntry,
  TravailExtractedData,
} from '../../core/models/case-analysis.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { AiQuestion } from '../../core/models/ai-question.model';
import { CoherencePopoverTriggerDirective } from '../../shared/coherence-popover/coherence-popover-trigger.directive';
import {
  CoherenceAlert,
  CoherenceAlertSource,
} from '../../shared/coherence-popover/coherence-alert.model';
import { CoherenceAlertBuilder } from '../../shared/coherence-popover/coherence-alert-builder';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';
import { SourceExplanation } from '../../core/models/source-explanation.model';
import { SourceExplanationService } from '../../core/services/source-explanation.service';
import {
  IndemnitePrecariteCddSectionPrefillRules,
} from './indemnite-precarite-cdd-section-prefill-rules';

/**
 * SF-DT-17-02 : champs d'alerte de cohérence F-IA-03 exposés par l'outil
 * F-DT-17 (indemnité précarité CDD).
 */
export type IPCAlertField = 'SALAIRE_MENSUEL';

export type IPCAlertSource = CoherenceAlertSource;
export type IPCCoherenceAlert = CoherenceAlert<IPCAlertField>;

/**
 * Seuil d'écart relatif au-delà duquel on affiche une alerte de cohérence
 * entre le salaire mensuel extrait par l'IA et la saisie de l'avocat.
 * Aligné avec SALAIRE_DIVERGENCE_RATIO du template canonique
 * `harcelement-licenciement-nul-section` (10 %).
 */
const SALAIRE_DIVERGENCE_RATIO = 0.10;

/**
 * SF-DT-17-02 : outil décisionnel "Indemnité précarité CDD" (F-DT-17).
 * France uniquement (pas d'équivalent légal forfaitaire en Belgique,
 * loi du 03/07/1978). Consomme l'API SF-DT-17-01.
 *
 * Affiché conditionnellement par le panel F-IA-04 (tool_id
 * `F-DT-17-indemnite-precarite-cdd`, règle ALWAYS_ON FR+travail).
 *
 * Pattern canonique : `harcelement-licenciement-nul-section` (SF-155-04-A1).
 * Pré-fill IA sur `salaireMensuelReference` (champ d'aide non envoyé au
 * backend) + alertes cohérence F-IA-03 via `CoherenceAlertBuilder`.
 */
@Component({
  selector: 'app-indemnite-precarite-cdd-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule, DecimalPipe,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatSelectModule, MatRadioModule,
    MatProgressSpinnerModule,
    LegalCitationsPipe,
    CoherencePopoverTriggerDirective,
    ToolJurisprudenceCitationsComponent,
  ],
  templateUrl: './indemnite-precarite-cdd-section.component.html',
  styleUrl: './indemnite-precarite-cdd-section.component.scss',
})
export class IndemnitePrecariteCddSectionComponent implements OnInit, OnChanges {
  // F-JU-03 SF-JU-03-01 — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-DT-17-indemnite-precarite-cdd';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'INDEMNITÉ PRÉCARITÉ CDD';

  /** F-177 SF-177-12 / F-236 SF-236-02 — délègue au helper partagé (parité runtime). */
  static getPrefillCount(input: {
    aiData?: any;
    procedureChecks?: any[];
    aiQuestions?: any[];
    piecesManquantes?: any[];
    triggerEvents?: any[];
    workspaceCountry?: string;
  }): number {
    return IndemnitePrecariteCddSectionPrefillRules.computePrefillCount({
      aiData: input.aiData,
      workspaceCountry: input.workspaceCountry,
    });
  }

  static readonly TOOL_ICON = 'savings';

  @Input() caseFileId!: string;
  // F-177 SF-177-03b : force l'expansion (mode modal F-177).
  @Input() forceExpanded = false;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';
  @Input() aiData?: TravailExtractedData | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;

  /**
   * F-163 SF-163-02b — Mode simulateur autonome (hors dossier client).
   * Quand `true` : bannière simulateur affichée, prefillFromAi() / coherenceAlerts /
   * loadExisting() / triggerRefresh() court-circuités, POST routé vers le dispatcher
   * générique /api/v1/simulators/{toolId}/calculate (SF-163-03).
   */
  @Input() standaloneMode: boolean = false;

  private aiDataSignal = signal<TravailExtractedData | null | undefined>(undefined);
  private procedureChecksSignal = signal<ProcedureCheck[]>([]);
  private aiQuestionsSignal = signal<AiQuestion[]>([]);
  private piecesManquantesSignal = signal<PieceManquanteEntry[]>([]);

  readonly casExclusionOptions = CAS_EXCLUSION_OPTIONS;

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<IndemnitePrecariteCddResponse | null>(null);

  // Champs d'aide UX (non envoyés au backend, mais utilisés pour pré-fill IA
  // et auto-calcul de totalSalairesBruts).
  salaireMensuelReference = signal<number | null>(null);
  dureeCddMois = signal<number | null>(null);

  // Champs envoyés au backend.
  totalSalairesBruts = signal<number | null>(null);
  tauxPrecarite = signal<6 | 10>(10);
  casExclusion = signal<CasExclusionCdd | null>(null);

  // true si totalSalairesBruts a été auto-calculé et que l'avocat n'a pas
  // encore tapé manuellement dedans — permet de ne pas écraser un edit manuel.
  private totalSalairesAutoCalc = signal(false);

  provenanceSalaire = signal<'IA' | null>(null);

  sourceExplanations = signal<Map<string, SourceExplanation[]>>(new Map());

  coherenceAlerts = computed<Partial<Record<IPCAlertField, IPCCoherenceAlert>>>(() => {
    // F-163 SF-163-02b : aucune source IA en standalone.
    if (this.standaloneMode) return {} as any;
    if (!this.showForm()) return {};
    const alerts: Partial<Record<IPCAlertField, IPCCoherenceAlert>> = {};
    const salaireAlert = this.buildSalaireAlert();
    if (salaireAlert) alerts.SALAIRE_MENSUEL = salaireAlert;
    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    return { total: values.length, blockers: 0 };
  });

  constructor(
    private service: IndemnitePrecariteCddService,
    private snackBar: MatSnackBar,
    @Optional() private dashboardRefresh: CaseDashboardRefreshService | null,
    @Optional() private sourceExplanationService: SourceExplanationService | null,
  ) {}

  ngOnInit(): void {
    // F-177 SF-177-03b : appliqué dès le mount pour le mode modal.
    if (this.forceExpanded) this.collapsed.set(false);

    this.aiDataSignal.set(this.aiData);
    this.procedureChecksSignal.set(this.procedureChecks ?? []);
    this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    this.piecesManquantesSignal.set(this.piecesManquantes ?? []);
    if (this.workspaceCountry === 'FRANCE') {
      this.load();
      this.loadSourceExplanations();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    // F-177 SF-177-03b : applique le forceExpanded quand il passe à true en cours de vie.
    if (changes['forceExpanded'] && this.forceExpanded) this.collapsed.set(false);

    if (changes['aiData']) this.aiDataSignal.set(this.aiData);
    if (changes['procedureChecks']) this.procedureChecksSignal.set(this.procedureChecks ?? []);
    if (changes['aiQuestions']) this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    if (changes['piecesManquantes']) this.piecesManquantesSignal.set(this.piecesManquantes ?? []);

    if (changes['aiData'] && !changes['aiData'].firstChange
        && this.showForm() && !this.result()) {
      if (!this.standaloneMode) this.prefillFromAi();
    }
  }

  private prefillFromAi(): void {
    // F-163 SF-163-02b : aucune source IA en standalone.
    if (this.standaloneMode) return;
    const ai = this.aiDataSignal();
    if (!ai) return;
    if (this.workspaceCountry !== 'FRANCE') return;

    // F-236 SF-236-02 : valeur calculée par le helper partagé (parité static).
    const ruleInput = { aiData: ai, workspaceCountry: this.workspaceCountry };
    const salaire = IndemnitePrecariteCddSectionPrefillRules.computeSalaireMensuelReference(ruleInput);
    if (salaire !== null) {
      if (this.salaireMensuelReference() === null || this.provenanceSalaire() === 'IA') {
        this.salaireMensuelReference.set(salaire);
        this.provenanceSalaire.set('IA');
        this.recomputeTotalSalaires();
      }
    }

    // SF-246-21 : durée CDD en mois.
    const duree = IndemnitePrecariteCddSectionPrefillRules.computeDureeCddMois(ruleInput);
    if (duree !== null && this.dureeCddMois() === null) {
      this.dureeCddMois.set(duree);
      this.recomputeTotalSalaires();
    }
  }

  private loadSourceExplanations(): void {
    // F-163 SF-163-02b : pas de dossier en standalone.
    if (this.standaloneMode) return;
    if (!this.caseFileId || !this.sourceExplanationService) return;
    this.sourceExplanationService.getForCaseFile(this.caseFileId).subscribe({
      next: (map) => this.sourceExplanations.set(map),
      error: () => { /* fail-open */ },
    });
  }

  explanationFor(field: IPCAlertField): SourceExplanation[] {
    const key = field === 'SALAIRE_MENSUEL' ? 'salaire_brut_mensuel' : '';
    return this.sourceExplanations().get(key) ?? [];
  }

  alertTooltip(alert: IPCCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: IPCCoherenceAlert): string {
    const prefix = (() => {
      switch (alert.source) {
        case 'F96': return 'Incohérence Checklist procédurale';
        case 'QUESTION_IA': return 'Incohérence Question complémentaire';
        case 'IA': return 'Incohérence détectée';
        case 'PIECE_MANQUANTE': return 'Pièce manquante';
        case 'MULTI': return 'Incohérence multiple';
      }
    })();
    return `${prefix} (${alert.expectedDisplay})`;
  }

  private buildSalaireAlert(): IPCCoherenceAlert | null {
    const aiSalaire = this.aiDataSignal()?.salaireBrutMensuel;
    const userSalaire = this.salaireMensuelReference();
    if (typeof aiSalaire !== 'number' || aiSalaire <= 0) return null;
    if (typeof userSalaire !== 'number' || userSalaire <= 0) return null;
    const ratio = Math.abs(userSalaire - aiSalaire) / aiSalaire;
    if (ratio <= SALAIRE_DIVERGENCE_RATIO) return null;
    const builder = CoherenceAlertBuilder.forField<IPCAlertField>('SALAIRE_MENSUEL')
      .addSource('IA', {
        expectedDisplay: `${aiSalaire.toLocaleString('fr-FR')} €`,
        reason: `Analyse du dossier : salaire brut mensuel ~${aiSalaire.toLocaleString('fr-FR')} €`,
      });
    const salairePiece = this.findPieceManquante(['SALAIRE_BRUT_MENSUEL', 'IPC_SALAIRE']);
    if (salairePiece) builder.addPieceManquante(salairePiece);
    return builder.build();
  }

  private findPieceManquante(acceptedCodes: string[]): string | null {
    const norm = new Set(acceptedCodes.map((c) => c.toUpperCase()));
    for (const p of this.piecesManquantesSignal()) {
      const code = p.critereCode?.toUpperCase();
      if (!code) continue;
      if (norm.has(code)) return p.texte;
    }
    return null;
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  formValid(): boolean {
    const total = this.totalSalairesBruts();
    return total !== null && total > 0;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  onSalaireChange(value: number | null): void {
    this.salaireMensuelReference.set(value);
    this.provenanceSalaire.set(null);
    this.recomputeTotalSalaires();
  }

  onDureeChange(value: number | null): void {
    this.dureeCddMois.set(value);
    this.recomputeTotalSalaires();
  }

  onTotalSalairesChange(value: number | null): void {
    this.totalSalairesBruts.set(value);
    // L'avocat a saisi manuellement → l'auto-calc n'écrasera plus.
    this.totalSalairesAutoCalc.set(false);
  }

  onTauxChange(value: number): void {
    this.tauxPrecarite.set(value === 6 ? 6 : 10);
  }

  onCasExclusionChange(value: CasExclusionCdd | null): void {
    this.casExclusion.set(value);
  }

  /**
   * Auto-calcule `totalSalairesBruts` si les deux champs d'aide sont remplis
   * ET que l'avocat n'a pas encore saisi manuellement `totalSalairesBruts`.
   */
  private recomputeTotalSalaires(): void {
    // Ne jamais écraser une saisie manuelle.
    if (!this.totalSalairesAutoCalc() && this.totalSalairesBruts() !== null) return;
    const s = this.salaireMensuelReference();
    const d = this.dureeCddMois();
    if (typeof s === 'number' && s > 0 && typeof d === 'number' && d > 0) {
      const total = Math.round(s * d * 100) / 100;
      this.totalSalairesBruts.set(total);
      this.totalSalairesAutoCalc.set(true);
    }
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request: IndemnitePrecariteCddRequest = {
      totalSalairesBruts: this.totalSalairesBruts()!,
      tauxPrecarite: this.tauxPrecarite(),
      casExclusion: this.casExclusion(),
    };
    this.calculating.set(true);
    (this.standaloneMode
      ? this.service.calculateStandalone(request)
      : this.service.calculate(this.caseFileId, request))
      .subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Indemnité calculée', 'OK', { duration: 2500 });
        // F-163 SF-163-02b : pas de dashboard à rafraîchir en standalone.

        if (!this.standaloneMode) this.dashboardRefresh?.triggerRefresh();
      },
      error: (err) => {
        this.calculating.set(false);
        const msg = err?.error?.message || err?.error || 'Erreur lors du calcul';
        this.snackBar.open(String(msg), 'Fermer', { duration: 5000, panelClass: 'snack-error' });
      },
    });
  }

  private load(): void {
    this.loading.set(true);
    // F-163 SF-163-02b : en standalone, pas de dossier à interroger.

    if (this.standaloneMode) {

      this.loading.set(false);

      this.showForm.set(true);

      if (this.collapsed && typeof (this.collapsed as any).set === 'function') this.collapsed.set(false);

      return;

    }

    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.totalSalairesBruts.set(r.totalSalairesBruts);
        this.tauxPrecarite.set(r.tauxPrecarite === 6 ? 6 : 10);
        this.casExclusion.set(r.casExclusion);
        // Valeurs persistées = saisie avocat — jamais de badge IA.
        this.provenanceSalaire.set(null);
        this.showForm.set(false);
        this.loading.set(false);
      },
      error: () => {
        // 404 attendu si aucune analyse — mode formulaire + pré-fill IA.
        if (!this.standaloneMode) this.prefillFromAi();
        this.loading.set(false);
      },
    });
  }
}
