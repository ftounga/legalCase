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
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSnackBar } from '@angular/material/snack-bar';
import { RappelSalaireService } from '../../core/services/rappel-salaire.service';
import {
  METHODES_CP_SUR_RAPPEL,
  RappelSalaireMethodeCpSurRappel,
  RappelSalaireRequest,
  RappelSalaireResponse,
} from '../../core/models/rappel-salaire.model';
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
import {
  RappelSalaireSectionPrefillRules,
} from './rappel-salaire-section-prefill-rules';

/**
 * SF-DT-20-02 : champs d'alerte de cohérence F-IA-03 exposés par l'outil
 * F-DT-20 (rappel de salaire FR). Seulement les 2 fields alimentés par l'IA
 * travail (`salaireBrutMensuel` → SALAIRE, `conventionCollective` → CONVENTION).
 */
export type RappelAlertField = 'SALAIRE' | 'CONVENTION';

// Alias rétro-compat pour l'usage interne template / tests.
export type RappelAlertSource = CoherenceAlertSource;
export type RappelCoherenceAlert = CoherenceAlert<RappelAlertField>;

/**
 * Seuil d'écart relatif au-delà duquel on affiche une alerte de cohérence
 * entre le salaire mensuel extrait par l'IA et la saisie de l'avocat.
 * Aligné avec template canonique `harcelement-licenciement-nul-section` et
 * F-DT-25/26/27 (10 %).
 */
const SALAIRE_DIVERGENCE_RATIO = 0.10;

/**
 * SF-DT-20-02 : outil décisionnel "Rappel de salaire" (F-DT-20). France
 * uniquement (art. L.3242-1 + L.3245-1 + L.3141-26 Code du travail + CCN).
 * Consomme l'API SF-DT-20-01 (backend, PR #584).
 *
 * Affiché conditionnellement par le panel F-IA-04 (tool_id
 * `F-DT-20-rappel-salaire`, règle ALWAYS_ON FR + travail, priority 57).
 *
 * Patterns de référence :
 * - Template canonique : `harcelement-licenciement-nul-section` (F-DT-11-02).
 * - Sélecteur CCN : `indemnite-preavis-section` (SF-DT-25-02) — réutilise
 *   `ConventionReferentialService` filtré FRANCE + `normalizeCode()`.
 * - Radio méthode CP : `conges-payes-section` (SF-DT-26-02) — 3 options ici
 *   au lieu de 2 + null.
 * - Période 2 datepickers : `heures-sup-section` (F-DT-19).
 * - Helper partagé : `CoherenceAlertBuilder` + `CoherenceAlert<F>` (SF-155-05).
 */
@Component({
  selector: 'app-rappel-salaire-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule, DecimalPipe,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatSelectModule, MatRadioModule,
    MatProgressSpinnerModule, MatSlideToggleModule,
    LegalCitationsPipe,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './rappel-salaire-section.component.html',
  styleUrl: './rappel-salaire-section.component.scss',
})
export class RappelSalaireSectionComponent implements OnInit, OnChanges {
  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'RAPPEL DE SALAIRE';

  /** F-177 SF-177-12 / F-236 SF-236-02 — délègue au helper partagé (parité runtime). */
  static getPrefillCount(input: {
    aiData?: any;
    procedureChecks?: any[];
    aiQuestions?: any[];
    piecesManquantes?: any[];
    triggerEvents?: any[];
    workspaceCountry?: string;
  }): number {
    return RappelSalaireSectionPrefillRules.computePrefillCount({
      aiData: input.aiData,
      workspaceCountry: input.workspaceCountry,
    });
  }

  static readonly TOOL_ICON = 'payments';

  @Input() caseFileId!: string;
  // F-177 SF-177-03b : force l'expansion (mode modal F-177).
  @Input() forceExpanded = false;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';
  // SF-DT-20-02 : inputs IA (tous optionnels — null-safe partout).
  @Input() aiData?: TravailExtractedData | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;

  // Snapshots signal des inputs IA pour réactivité computed.
  private aiDataSignal = signal<TravailExtractedData | null | undefined>(undefined);
  private procedureChecksSignal = signal<ProcedureCheck[]>([]);
  private aiQuestionsSignal = signal<AiQuestion[]>([]);
  private piecesManquantesSignal = signal<PieceManquanteEntry[]>([]);

  readonly methodeOptions = METHODES_CP_SUR_RAPPEL;

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<RappelSalaireResponse | null>(null);

  // Champs du form (signals).
  periodeDebut = signal<string | null>(null);
  periodeFin = signal<string | null>(null);
  montantSalaireDuMensuelEur = signal<number | null>(null);
  montantSalairePerVerseMensuelEur = signal<number | null>(null);
  conventionCollectiveCode = signal<string | null>(null);
  ancienneteAnneesPrime = signal<number | null>(null);
  indexInseeRevalorise = signal<boolean>(false);
  tauxRevalorisationPct = signal<number | null>(null);
  methodeCpSurRappel = signal<RappelSalaireMethodeCpSurRappel>('DIX_POURCENT');

  // Liste dynamique conventions FR (filtrée).
  conventionsFrance = signal<ConventionOption[]>([]);

  // Provenance IA par champ pré-rempli.
  provenanceMontantDu = signal<'IA' | null>(null);
  provenanceConvention = signal<'IA' | null>(null);

  // SF-IA-03-15a : map {sourceKey → explanations}.
  sourceExplanations = signal<Map<string, SourceExplanation[]>>(new Map());

  /**
   * SF-DT-20-02 : alertes de cohérence calculées dynamiquement (2 fields).
   * Gate : uniquement en mode formulaire (pattern anti-bug SF-IA-03-12).
   */
  coherenceAlerts = computed<Partial<Record<RappelAlertField, RappelCoherenceAlert>>>(() => {
    if (!this.showForm()) return {};
    const alerts: Partial<Record<RappelAlertField, RappelCoherenceAlert>> = {};
    const salaireAlert = this.buildSalaireAlert();
    if (salaireAlert) alerts.SALAIRE = salaireAlert;
    const convAlert = this.buildConventionAlert();
    if (convAlert) alerts.CONVENTION = convAlert;
    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    // Rappel de salaire = calcul indemnitaire, pas urgence — jamais de CRITICAL.
    return { total: values.length, blockers: 0 };
  });

  constructor(
    private service: RappelSalaireService,
    private conventionRefService: ConventionReferentialService,
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
      this.loadConventions();
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

    // Ré-appliquer le pré-fill quand `aiData` change avant première résolution.
    if (changes['aiData'] && !changes['aiData'].firstChange
        && this.workspaceCountry === 'FRANCE' && this.showForm() && !this.result()) {
      this.prefillFromAi();
    }
  }

  private loadConventions(): void {
    this.conventionRefService.list().subscribe({
      next: (options) => {
        this.conventionsFrance.set(options.filter((o) => o.country === 'FRANCE'));
        this.maybePreselectConventionFromAi();
      },
      error: () => { /* fallback in service — fail-open */ },
    });
  }

  private maybePreselectConventionFromAi(): void {
    const ai = this.aiDataSignal();
    if (!ai?.conventionCollective) return;
    // Mode résultat (GET 200) — la valeur persistée prime, pas de pré-fill IA.
    if (this.result() !== null) return;
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
   * SF-DT-20-02 : pré-remplit le form depuis `aiData`. N'écrase jamais une
   * saisie avocat existante (la garde est dans le call site).
   *
   * Mappings :
   * - `salaireBrutMensuel` → `montantSalaireDuMensuelEur` (salaire dû — l'avocat
   *   ajustera le versé manuellement, info absente du dossier en général).
   * - `conventionCollective` → `conventionCollectiveCode` (via normalize + match).
   */
  private prefillFromAi(): void {
    const ai = this.aiDataSignal();
    if (!ai) return;
    if (this.workspaceCountry !== 'FRANCE') return;

    // F-236 SF-236-02 : valeur calculée par le helper partagé (parité static).
    const ruleInput = { aiData: ai, workspaceCountry: this.workspaceCountry };
    const salaire = RappelSalaireSectionPrefillRules.computeMontantSalaireDuMensuel(ruleInput);
    if (salaire !== null) {
      if (this.montantSalaireDuMensuelEur() === null || this.provenanceMontantDu() === 'IA') {
        this.montantSalaireDuMensuelEur.set(salaire);
        this.provenanceMontantDu.set('IA');
      }
    }

    // SF-246-21 : montant salaire perversé mensuel et périodes.
    const perverse = RappelSalaireSectionPrefillRules.computeMontantSalairePerverseMensuel(ruleInput);
    if (perverse !== null && this.montantSalairePerVerseMensuelEur() === null) {
      this.montantSalairePerVerseMensuelEur.set(perverse);
    }
    const periodeDebut = RappelSalaireSectionPrefillRules.computePeriodeDebut(ruleInput);
    if (periodeDebut !== null && this.periodeDebut() === null) {
      this.periodeDebut.set(periodeDebut);
    }
    const periodeFin = RappelSalaireSectionPrefillRules.computePeriodeFin(ruleInput);
    if (periodeFin !== null && this.periodeFin() === null) {
      this.periodeFin.set(periodeFin);
    }

    // 2. Convention collective (normalisée + matchée vs liste référentiel).
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
  explanationFor(field: RappelAlertField): SourceExplanation[] {
    const key = field === 'SALAIRE' ? 'salaire_brut_mensuel'
              : field === 'CONVENTION' ? 'convention_collective'
              : '';
    return this.sourceExplanations().get(key) ?? [];
  }

  alertTooltip(alert: RappelCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: RappelCoherenceAlert): string {
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
   * Divergence salaire — écart relatif > 10 % entre salaire IA et montantDû saisi.
   */
  private buildSalaireAlert(): RappelCoherenceAlert | null {
    const aiSalaire = this.aiDataSignal()?.salaireBrutMensuel;
    const userSalaire = this.montantSalaireDuMensuelEur();
    if (typeof aiSalaire !== 'number' || aiSalaire <= 0) return null;
    if (typeof userSalaire !== 'number' || userSalaire <= 0) return null;
    const ratio = Math.abs(userSalaire - aiSalaire) / aiSalaire;
    if (ratio <= SALAIRE_DIVERGENCE_RATIO) return null;
    const builder = CoherenceAlertBuilder.forField<RappelAlertField>('SALAIRE')
      .addSource('IA', {
        expectedDisplay: `${aiSalaire.toLocaleString('fr-FR')} €`,
        reason: `Analyse du dossier : salaire brut mensuel ~${aiSalaire.toLocaleString('fr-FR')} €`,
      });
    const piece = this.findPieceManquante(['SALAIRE_BRUT_MENSUEL', 'RAPPEL_SALAIRE_DU']);
    if (piece) builder.addPieceManquante(piece);
    return builder.build();
  }

  /**
   * Divergence convention collective — comparaison upper-case stricte entre
   * code IA normalisé et code saisi par l'avocat.
   */
  private buildConventionAlert(): RappelCoherenceAlert | null {
    const aiConv = this.aiDataSignal()?.conventionCollective;
    const userConv = this.conventionCollectiveCode();
    if (!aiConv || !userConv) return null;
    const normalized = ConventionReferentialService.normalizeCode(aiConv);
    if (!normalized) return null;
    if (normalized === userConv.toUpperCase()) return null;
    const builder = CoherenceAlertBuilder.forField<RappelAlertField>('CONVENTION')
      .addSource('IA', {
        expectedDisplay: aiConv,
        reason: `Analyse du dossier : convention ${aiConv}`,
      });
    return builder.build();
  }

  /**
   * Renvoie le `texte` d'une PieceManquanteEntry dont `critereCode`
   * (case-insensitive) appartient à la liste des codes acceptés, ou `null`.
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
    this.collapsed.update((v) => !v);
  }

  formValid(): boolean {
    const debut = this.periodeDebut();
    const fin = this.periodeFin();
    const du = this.montantSalaireDuMensuelEur();
    const verse = this.montantSalairePerVerseMensuelEur();
    const anc = this.ancienneteAnneesPrime();
    const indexed = this.indexInseeRevalorise();
    const taux = this.tauxRevalorisationPct();
    const methode = this.methodeCpSurRappel();

    if (!debut || !fin) return false;
    if (debut > fin) return false;
    if (du === null || !Number.isFinite(du) || du <= 0) return false;
    if (verse === null || !Number.isFinite(verse) || verse < 0) return false;
    if (verse >= du) return false; // pas de différentiel à réclamer
    if (anc === null || !Number.isFinite(anc) || anc < 0) return false;
    if (indexed) {
      if (taux === null || !Number.isFinite(taux) || taux < 0 || taux > 100) return false;
    }
    if (!methode) return false;
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // -- Handlers (effacent provenance IA au 1er changement manuel) --
  onPeriodeDebutChange(value: string | null): void {
    this.periodeDebut.set(value || null);
  }

  onPeriodeFinChange(value: string | null): void {
    this.periodeFin.set(value || null);
  }

  onMontantDuChange(value: number | null): void {
    this.montantSalaireDuMensuelEur.set(value);
    this.provenanceMontantDu.set(null);
  }

  onMontantVerseChange(value: number | null): void {
    this.montantSalairePerVerseMensuelEur.set(value);
  }

  onConventionChange(value: string | null): void {
    this.conventionCollectiveCode.set(value);
    this.provenanceConvention.set(null);
  }

  onAncienneteChange(value: number | null): void {
    this.ancienneteAnneesPrime.set(value);
  }

  onIndexInseeChange(value: boolean): void {
    this.indexInseeRevalorise.set(value);
    if (!value) {
      // Si l'avocat désactive la revalorisation, on nettoie le taux.
      this.tauxRevalorisationPct.set(null);
    }
  }

  onTauxRevalorisationChange(value: number | null): void {
    this.tauxRevalorisationPct.set(value);
  }

  onMethodeCpChange(value: RappelSalaireMethodeCpSurRappel): void {
    this.methodeCpSurRappel.set(value);
  }

  /** Helper template : libellé d'une méthode CP. */
  methodeLabel(m: RappelSalaireMethodeCpSurRappel): string {
    return this.methodeOptions.find((o) => o.code === m)?.label ?? String(m);
  }

  /** Helper template : article juridique d'une méthode CP. */
  methodeArticle(m: RappelSalaireMethodeCpSurRappel): string {
    return this.methodeOptions.find((o) => o.code === m)?.article ?? '';
  }

  calculate(): void {
    if (!this.formValid()) return;
    const indexed = this.indexInseeRevalorise();
    const request: RappelSalaireRequest = {
      periodeDebut: this.periodeDebut()!,
      periodeFin: this.periodeFin()!,
      montantSalaireDuMensuelEur: this.montantSalaireDuMensuelEur()!,
      montantSalairePerVerseMensuelEur: this.montantSalairePerVerseMensuelEur()!,
      conventionCollectiveCode: this.conventionCollectiveCode(),
      ancienneteAnneesPrime: this.ancienneteAnneesPrime()!,
      indexInseeRevalorise: indexed,
      tauxRevalorisationPct: indexed ? this.tauxRevalorisationPct() : null,
      methodeCpSurRappel: this.methodeCpSurRappel(),
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Rappel de salaire calculé', 'OK', { duration: 2500 });
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
        this.periodeDebut.set(r.periodeDebut);
        this.periodeFin.set(r.periodeFin);
        this.montantSalaireDuMensuelEur.set(r.montantSalaireDuMensuelEur);
        this.montantSalairePerVerseMensuelEur.set(r.montantSalairePerVerseMensuelEur);
        this.conventionCollectiveCode.set(r.conventionCollectiveCode ?? null);
        this.ancienneteAnneesPrime.set(r.ancienneteAnneesPrime);
        this.indexInseeRevalorise.set(r.indexInseeRevalorise);
        this.tauxRevalorisationPct.set(r.tauxRevalorisationPct ?? null);
        this.methodeCpSurRappel.set(r.methodeCpSurRappel);
        // Valeurs persistées = saisie avocat — jamais de badge IA.
        this.provenanceMontantDu.set(null);
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
