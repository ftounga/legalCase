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
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSnackBar } from '@angular/material/snack-bar';
import { IndemnitePreavisService } from '../../core/services/indemnite-preavis.service';
import {
  FONCTIONS_PREAVIS,
  FonctionPreavis,
  IndemnitePreavisRequest,
  IndemnitePreavisResponse,
  SourceDureePreavis,
} from '../../core/models/indemnite-preavis.model';
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
import {
  ConventionOption,
  ConventionReferentialService,
} from '../../core/services/convention-referential.service';
import { SourceExplanation } from '../../core/models/source-explanation.model';
import { SourceExplanationService } from '../../core/services/source-explanation.service';

/**
 * SF-DT-25-02 : champs d'alerte de cohérence F-IA-03 exposés par l'outil
 * F-DT-25 (indemnité compensatrice de préavis FR).
 */
export type IndPreavisAlertField = 'SALAIRE' | 'DATE_RUPTURE' | 'CONVENTION';

// Alias rétro-compat pour l'usage interne template / tests.
export type IndPreavisAlertSource = CoherenceAlertSource;
export type IndPreavisCoherenceAlert = CoherenceAlert<IndPreavisAlertField>;

/**
 * Seuil d'écart relatif au-delà duquel on affiche une alerte de cohérence
 * entre le salaire mensuel extrait par l'IA et la saisie de l'avocat.
 * Aligné avec template canonique `harcelement-licenciement-nul-section` (10 %).
 */
const SALAIRE_DIVERGENCE_RATIO = 0.10;

/**
 * Seuil d'écart en jours absolu au-delà duquel on affiche une alerte de
 * cohérence entre la date de rupture extraite par l'IA et la saisie avocat.
 * Aligné avec `anciennete-section.dateEntree` (15 jours).
 */
const DATE_RUPTURE_DIVERGENCE_DAYS = 15;

/**
 * SF-DT-25-02 : outil décisionnel "Indemnité compensatrice de préavis"
 * (F-DT-25). France uniquement (art. L.1234-1 et s. Code du travail).
 * Consomme l'API SF-DT-25-01.
 *
 * Affiché conditionnellement par le panel F-IA-04 (tool_id
 * `F-DT-25-indemnite-preavis`, règle ALWAYS_ON FR + travail).
 *
 * Pattern canonique : `harcelement-licenciement-nul-section` (SF-155-04-A1).
 * Pré-fill IA sur 3 champs (salaire / dateRupture / convention) +
 * alertes cohérence F-IA-03 via `CoherenceAlertBuilder`.
 */
@Component({
  selector: 'app-indemnite-preavis-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule, DecimalPipe,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatProgressSpinnerModule, MatSlideToggleModule,
    LegalCitationsPipe,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './indemnite-preavis-section.component.html',
  styleUrl: './indemnite-preavis-section.component.scss',
})
export class IndemnitePreavisSectionComponent implements OnInit, OnChanges {
  @Input() caseFileId!: string;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';
  // SF-DT-25-02 : inputs IA (tous optionnels — null-safe partout).
  @Input() aiData?: TravailExtractedData | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;

  // Snapshots signal des inputs IA pour réactivité computed.
  private aiDataSignal = signal<TravailExtractedData | null | undefined>(undefined);
  private procedureChecksSignal = signal<ProcedureCheck[]>([]);
  private aiQuestionsSignal = signal<AiQuestion[]>([]);
  private piecesManquantesSignal = signal<PieceManquanteEntry[]>([]);

  readonly fonctionOptions = FONCTIONS_PREAVIS;

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<IndemnitePreavisResponse | null>(null);

  // Champs du form (signals — édités via handlers).
  ancienneteAnnees = signal<number | null>(null);
  ancienneteMois = signal<number | null>(null);
  salaireMensuelBrutEur = signal<number | null>(null);
  conventionCollectiveCode = signal<string | null>(null);
  fonction = signal<FonctionPreavis | null>(null);
  exemptionEmployeur = signal<boolean>(false);
  dateRupture = signal<string | null>(null);

  // Liste dynamique conventions FR (filtrée).
  conventionsFrance = signal<ConventionOption[]>([]);

  // Provenance IA par champ pré-rempli.
  provenanceSalaire = signal<'IA' | null>(null);
  provenanceDateRupture = signal<'IA' | null>(null);
  provenanceConvention = signal<'IA' | null>(null);

  // SF-IA-03-15a : map {sourceKey → explanations}.
  sourceExplanations = signal<Map<string, SourceExplanation[]>>(new Map());

  /**
   * SF-DT-25-02 : alertes de cohérence calculées dynamiquement (3 fields).
   * Gate : uniquement en mode formulaire (pattern anti-bug SF-IA-03-12).
   */
  coherenceAlerts = computed<Partial<Record<IndPreavisAlertField, IndPreavisCoherenceAlert>>>(() => {
    if (!this.showForm()) return {};
    const alerts: Partial<Record<IndPreavisAlertField, IndPreavisCoherenceAlert>> = {};
    const salaireAlert = this.buildSalaireAlert();
    if (salaireAlert) alerts.SALAIRE = salaireAlert;
    const dateAlert = this.buildDateRuptureAlert();
    if (dateAlert) alerts.DATE_RUPTURE = dateAlert;
    const convAlert = this.buildConventionAlert();
    if (convAlert) alerts.CONVENTION = convAlert;
    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    return { total: values.length, blockers: 0 };
  });

  constructor(
    private service: IndemnitePreavisService,
    private conventionRefService: ConventionReferentialService,
    private snackBar: MatSnackBar,
    @Optional() private dashboardRefresh: CaseDashboardRefreshService | null,
    @Optional() private sourceExplanationService: SourceExplanationService | null,
  ) {}

  ngOnInit(): void {
    this.aiDataSignal.set(this.aiData);
    this.procedureChecksSignal.set(this.procedureChecks ?? []);
    this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    this.piecesManquantesSignal.set(this.piecesManquantes ?? []);
    if (this.workspaceCountry === 'FRANCE') {
      this.loadConventions();
      this.load();
      this.loadSourceExplanations();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['aiData']) this.aiDataSignal.set(this.aiData);
    if (changes['procedureChecks']) this.procedureChecksSignal.set(this.procedureChecks ?? []);
    if (changes['aiQuestions']) this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    if (changes['piecesManquantes']) this.piecesManquantesSignal.set(this.piecesManquantes ?? []);

    // Ré-appliquer le pré-fill quand `aiData` change avant première résolution.
    if (changes['aiData'] && !changes['aiData'].firstChange
        && this.showForm() && !this.result()) {
      this.prefillFromAi();
    }
  }

  private loadConventions(): void {
    this.conventionRefService.list().subscribe({
      next: (options) => {
        this.conventionsFrance.set(options.filter((o) => o.country === 'FRANCE'));
        // Si aiData déjà là, retry le mapping convention.
        this.maybePreselectConventionFromAi();
      },
      error: () => { /* fallback in service — fail-open */ },
    });
  }

  private maybePreselectConventionFromAi(): void {
    const ai = this.aiDataSignal();
    if (!ai?.conventionCollective) return;
    if (this.conventionCollectiveCode() !== null && this.provenanceConvention() !== 'IA') return;
    const normalized = ConventionReferentialService.normalizeCode(ai.conventionCollective);
    if (!normalized) return;
    const match = this.conventionsFrance().find((o) => o.code.toUpperCase() === normalized);
    if (match) {
      this.conventionCollectiveCode.set(match.code);
      this.provenanceConvention.set('IA');
    }
  }

  /**
   * SF-DT-25-02 : pré-remplit le form depuis `aiData`. N'écrase jamais une
   * saisie avocat existante (la garde est dans le call site).
   */
  private prefillFromAi(): void {
    const ai = this.aiDataSignal();
    if (!ai) return;
    if (this.workspaceCountry !== 'FRANCE') return;

    // 1. Salaire brut mensuel.
    if (typeof ai.salaireBrutMensuel === 'number' && ai.salaireBrutMensuel > 0) {
      if (this.salaireMensuelBrutEur() === null || this.provenanceSalaire() === 'IA') {
        this.salaireMensuelBrutEur.set(ai.salaireBrutMensuel);
        this.provenanceSalaire.set('IA');
      }
    }

    // 2. Date de rupture (mappée depuis dateLicenciement IA).
    if (ai.dateLicenciement) {
      if (this.dateRupture() === null || this.provenanceDateRupture() === 'IA') {
        this.dateRupture.set(ai.dateLicenciement);
        this.provenanceDateRupture.set('IA');
      }
    }

    // 3. Convention collective (normalisée + matchée vs liste référentiel).
    this.maybePreselectConventionFromAi();
  }

  private loadSourceExplanations(): void {
    if (!this.caseFileId || !this.sourceExplanationService) return;
    this.sourceExplanationService.getForCaseFile(this.caseFileId).subscribe({
      next: (map) => this.sourceExplanations.set(map),
      error: () => { /* fail-open */ },
    });
  }

  /**
   * SF-IA-03-15a : mapping field → sourceKey pour le popover enrichi.
   */
  explanationFor(field: IndPreavisAlertField): SourceExplanation[] {
    const key = (() => {
      switch (field) {
        case 'SALAIRE': return 'salaire_brut_mensuel';
        case 'DATE_RUPTURE': return 'date_licenciement';
        case 'CONVENTION': return 'convention_collective';
      }
    })();
    return this.sourceExplanations().get(key) ?? [];
  }

  alertTooltip(alert: IndPreavisCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: IndPreavisCoherenceAlert): string {
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

  /**
   * Divergence salaire — écart relatif > 10 % entre IA et saisie avocat.
   */
  private buildSalaireAlert(): IndPreavisCoherenceAlert | null {
    const aiSalaire = this.aiDataSignal()?.salaireBrutMensuel;
    const userSalaire = this.salaireMensuelBrutEur();
    if (typeof aiSalaire !== 'number' || aiSalaire <= 0) return null;
    if (typeof userSalaire !== 'number' || userSalaire <= 0) return null;
    const ratio = Math.abs(userSalaire - aiSalaire) / aiSalaire;
    if (ratio <= SALAIRE_DIVERGENCE_RATIO) return null;
    const builder = CoherenceAlertBuilder.forField<IndPreavisAlertField>('SALAIRE')
      .addSource('IA', {
        expectedDisplay: `${aiSalaire.toLocaleString('fr-FR')} €`,
        reason: `Analyse du dossier : salaire brut mensuel ~${aiSalaire.toLocaleString('fr-FR')} €`,
      });
    const piece = this.findPieceManquante(['SALAIRE_BRUT_MENSUEL', 'IND_PREAVIS_SALAIRE']);
    if (piece) builder.addPieceManquante(piece);
    return builder.build();
  }

  /**
   * Divergence date de rupture — écart absolu ≥ 15 jours entre IA et saisie.
   */
  private buildDateRuptureAlert(): IndPreavisCoherenceAlert | null {
    const aiDate = this.aiDataSignal()?.dateLicenciement;
    const userDate = this.dateRupture();
    if (!aiDate || !userDate) return null;
    const diff = dateDaysDiff(aiDate, userDate);
    if (diff === null || diff < DATE_RUPTURE_DIVERGENCE_DAYS) return null;
    const builder = CoherenceAlertBuilder.forField<IndPreavisAlertField>('DATE_RUPTURE')
      .addSource('IA', {
        expectedDisplay: aiDate,
        reason: `Analyse du dossier : date de licenciement ${aiDate}`,
      });
    return builder.build();
  }

  /**
   * Divergence convention collective — comparaison upper-case stricte entre
   * code IA normalisé et code saisi par l'avocat.
   */
  private buildConventionAlert(): IndPreavisCoherenceAlert | null {
    const aiConv = this.aiDataSignal()?.conventionCollective;
    const userConv = this.conventionCollectiveCode();
    if (!aiConv || !userConv) return null;
    const normalized = ConventionReferentialService.normalizeCode(aiConv);
    if (!normalized) return null;
    if (normalized === userConv.toUpperCase()) return null;
    const builder = CoherenceAlertBuilder.forField<IndPreavisAlertField>('CONVENTION')
      .addSource('IA', {
        expectedDisplay: aiConv,
        reason: `Analyse du dossier : convention ${aiConv}`,
      });
    return builder.build();
  }

  /**
   * SF-155-06 : retourne le `texte` d'une `PieceManquanteEntry` dont
   * `critereCode` (case-insensitive) appartient à la liste des codes acceptés.
   */
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
    const aa = this.ancienneteAnnees();
    const am = this.ancienneteMois();
    const s = this.salaireMensuelBrutEur();
    const f = this.fonction();
    const dr = this.dateRupture();
    return (
      aa !== null && Number.isFinite(aa) && aa >= 0 &&
      am !== null && Number.isFinite(am) && am >= 0 && am <= 11 &&
      s !== null && Number.isFinite(s) && s > 0 &&
      f !== null &&
      !!dr
    );
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // -- Handlers --
  onAncienneteAnneesChange(value: number | null): void {
    this.ancienneteAnnees.set(value);
  }

  onAncienneteMoisChange(value: number | null): void {
    this.ancienneteMois.set(value);
  }

  onSalaireChange(value: number | null): void {
    this.salaireMensuelBrutEur.set(value);
    this.provenanceSalaire.set(null);
  }

  onConventionChange(value: string | null): void {
    this.conventionCollectiveCode.set(value);
    this.provenanceConvention.set(null);
  }

  onFonctionChange(value: FonctionPreavis | null): void {
    this.fonction.set(value);
  }

  onExemptionChange(value: boolean): void {
    this.exemptionEmployeur.set(value);
  }

  onDateRuptureChange(value: string | null): void {
    this.dateRupture.set(value);
    this.provenanceDateRupture.set(null);
  }

  /**
   * SF-DT-25-02 : libellé d'affichage pour le badge `sourceDuree`.
   */
  sourceDureeLabel(source: SourceDureePreavis): string {
    switch (source) {
      case 'LEGALE': return 'Légale (L.1234-1)';
      case 'CCN': return 'Convention collective';
      case 'USAGE': return 'Usage';
    }
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request: IndemnitePreavisRequest = {
      ancienneteAnnees: this.ancienneteAnnees()!,
      ancienneteMois: this.ancienneteMois()!,
      salaireMensuelBrutEur: this.salaireMensuelBrutEur()!,
      conventionCollectiveCode: this.conventionCollectiveCode(),
      fonction: this.fonction()!,
      exemptionEmployeur: this.exemptionEmployeur(),
      dateRupture: this.dateRupture()!,
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Indemnité calculée', 'OK', { duration: 2500 });
        this.dashboardRefresh?.triggerRefresh();
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
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.ancienneteAnnees.set(r.ancienneteAnnees);
        this.ancienneteMois.set(r.ancienneteMois);
        this.salaireMensuelBrutEur.set(r.salaireMensuelBrutEur);
        this.conventionCollectiveCode.set(r.conventionCollectiveCode ?? null);
        this.fonction.set(r.fonction);
        this.exemptionEmployeur.set(r.exemptionEmployeur);
        this.dateRupture.set(r.dateRupture);
        // Valeurs persistées = saisie avocat — jamais de badge IA.
        this.provenanceSalaire.set(null);
        this.provenanceDateRupture.set(null);
        this.provenanceConvention.set(null);
        this.showForm.set(false);
        this.loading.set(false);
      },
      error: () => {
        // 404 attendu si aucune analyse — mode formulaire + pré-fill IA.
        this.prefillFromAi();
        this.loading.set(false);
      },
    });
  }
}

/**
 * Utilitaire : différence absolue en jours entre 2 dates ISO. Renvoie null si
 * l'une des dates n'est pas parsable.
 */
function dateDaysDiff(a: string, b: string): number | null {
  const ta = Date.parse(a);
  const tb = Date.parse(b);
  if (Number.isNaN(ta) || Number.isNaN(tb)) return null;
  return Math.abs(ta - tb) / 86400000;
}
