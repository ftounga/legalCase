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
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CongesPayesArretMaladieService } from '../../core/services/conges-payes-arret-maladie.service';
import {
  CongesPayesArretMaladieRequest,
  CongesPayesArretMaladieResponse,
  CongesPayesArretMaladieTypeArret,
  CongesPayesArretMaladieVerdict,
} from '../../core/models/conges-payes-arret-maladie.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import {
  PieceManquanteEntry,
  TravailExtractedData,
} from '../../core/models/case-analysis.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { AiQuestion } from '../../core/models/ai-question.model';
import { CoherencePopoverTriggerDirective } from '../../shared/coherence-popover/coherence-popover-trigger.directive';
import { CoherenceAlert } from '../../shared/coherence-popover/coherence-alert.model';
import { CoherenceAlertBuilder } from '../../shared/coherence-popover/coherence-alert-builder';
import { CongesPayesArretMaladieSectionPrefillRules } from './conges-payes-arret-maladie-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-206-04 : champs F-IA-03 audités par l'outil F-DT-75.
 * Trois champs croisables avec la checklist procédurale F-96 / les questions
 * IA / les pièces manquantes (cf. SF-206-03 §critereCode).
 */
export type CPMAlertField = 'TYPE_ARRET' | 'DUREE_ARRET' | 'SALARIE_EN_POSTE';

export type CPMCoherenceAlert = CoherenceAlert<CPMAlertField>;

/** Codes `critereCode` (F-96 / questions IA) reconnus par field. */
const F96_CODE_TYPE_ARRET = 'DT75_TYPE_ARRET';
const F96_CODE_DUREE_ARRET = 'DT75_DUREE_ARRET';
const F96_CODE_SALARIE_EN_POSTE = 'DT75_SALARIE_EN_POSTE';

/**
 * SF-206-04 : composant Angular standalone pour l'outil décisionnel
 * "Congés payés acquis pendant arrêt maladie" (F-DT-75). FRANCE uniquement
 * (régime L.3141-5 / L.3141-5-1 CT — loi 22/04/2024 art. 37 transposant Cass.
 * soc. 13/09/2023 n°22-17.340).
 *
 * Pattern de référence : `abandon-poste-presomption-demission-section`
 * (F-DT-42) — formulaire de saisie, POST de calcul, verdict 4 niveaux, F-IA-03,
 * refresh dashboard, OnPush + markForCheck dans les subscribe.
 *
 * Verdict 4 niveaux :
 *  - RAPPEL_SIGNIFICATIF (orange — signal d'action, ≥ 5 j et action ouverte)
 *  - RAPPEL_LIMITE (navy — 0 < jours < 5 et action ouverte)
 *  - PAS_DE_RAPPEL (navy grisé — 0 jour et action ouverte)
 *  - ACTION_FORCLOSE (rouge — délai dépassé, alerte explicite)
 *
 * Échéance inter-onglets (SF-206-00b) : `dateLimiteAction` affichée avec un
 * renvoi explicite vers l'onglet Suivi.
 */
@Component({
  selector: 'app-conges-payes-arret-maladie-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatSelectModule, MatSlideToggleModule,
    MatProgressSpinnerModule,
    LegalCitationsPipe,
    CoherencePopoverTriggerDirective,
    ToolJurisprudenceCitationsComponent,
  ],
  templateUrl: './conges-payes-arret-maladie-section.component.html',
  styleUrl: './conges-payes-arret-maladie-section.component.scss',
})
export class CongesPayesArretMaladieSectionComponent implements OnInit, OnChanges {
  // F-177 SF-177-03b : metadata statique consommée par le panel pour la card.
  static readonly TOOL_LABEL = 'CONGÉS PAYÉS ACQUIS PENDANT ARRÊT MALADIE';
  static readonly TOOL_ICON = 'event_available';

  /**
   * SF-244 — délégué au helper partagé (parité stricte avec `prefillFromAi()`).
   * Retourne le nombre exact de champs pré-remplissables FR pour le badge tab.
   * Couvre les 5 champs `cpArretMaladie*` + `salaireBrutMensuel` (déjà extrait).
   */
  static getPrefillCount(input: {
    aiData?: TravailExtractedData | null;
    procedureChecks?: unknown[];
    aiQuestions?: unknown[];
    piecesManquantes?: unknown[];
    triggerEvents?: unknown[];
    workspaceCountry?: string;
  }): number {
    return CongesPayesArretMaladieSectionPrefillRules.computePrefillCount({
      aiData: input.aiData,
      workspaceCountry: input.workspaceCountry,
    });
  }

  @Input() caseFileId!: string;
  // F-177 SF-177-03b : force l'expansion (mode modal F-177).
  @Input() forceExpanded = false;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';
  // Inputs IA / contexte (tous optionnels — null-safe partout).
  @Input() aiData?: TravailExtractedData | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;
  /** Mode simulateur autonome (hors dossier) — coupe F-IA-03 + refresh. */
  @Input() standaloneMode = false;

  // F-JU-03 SF-JU-03-100 — citations jurisprudentielles F-JU-01 (instrumentation
  // résiduelle : outil ajouté après la vague F-JU-03). toolId == clé TOOL_REGISTRY.
  protected readonly toolIdForJurisprudence = 'F-DT-75-conges-payes-arret-maladie';
  protected readonly brancheActiveForJurisprudence = 'default';

  // Snapshots signal des inputs IA pour réactivité des `computed`.
  private procedureChecksSignal = signal<ProcedureCheck[]>([]);
  private aiQuestionsSignal = signal<AiQuestion[]>([]);
  private piecesManquantesSignal = signal<PieceManquanteEntry[]>([]);

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<CongesPayesArretMaladieResponse | null>(null);

  // --- Form fields ---
  typeArret = signal<CongesPayesArretMaladieTypeArret>('MALADIE_NON_PROFESSIONNELLE');
  nombreMoisArret = signal<number>(12);
  salarieEncoreEnPoste = signal<boolean>(true);
  dateRuptureContrat = signal<string | null>(null);
  joursCpDejaAccordes = signal<number>(0);
  salaireBrutMensuel = signal<number>(0);

  /**
   * SF-206-04 : provenance IA par champ pré-rempli. `'IA'` tant que la valeur
   * provient de l'analyse ; remis à `null` dès qu'un handler `onXxxChange()`
   * détecte une modification manuelle (le badge `auto_awesome` disparaît).
   */
  provenanceTypeArret = signal<'IA' | null>(null);
  provenanceNombreMoisArret = signal<'IA' | null>(null);
  provenanceSalarieEnPoste = signal<'IA' | null>(null);
  provenanceDateRupture = signal<'IA' | null>(null);
  provenanceJoursDejaAccordes = signal<'IA' | null>(null);
  provenanceSalaireBrutMensuel = signal<'IA' | null>(null);

  /**
   * Coherence alerts F-IA-03 — gate `showForm()` strict (alertes masquées
   * après calcul rendu). Aucune source IA en standalone.
   */
  coherenceAlerts = computed<Partial<Record<CPMAlertField, CPMCoherenceAlert>>>(() => {
    if (this.standaloneMode) return {};
    if (!this.showForm()) return {};
    const alerts: Partial<Record<CPMAlertField, CPMCoherenceAlert>> = {};
    const typeAlert = this.buildTypeArretAlert();
    if (typeAlert) alerts.TYPE_ARRET = typeAlert;
    const dureeAlert = this.buildDureeArretAlert();
    if (dureeAlert) alerts.DUREE_ARRET = dureeAlert;
    const posteAlert = this.buildSalarieEnPosteAlert();
    if (posteAlert) alerts.SALARIE_EN_POSTE = posteAlert;
    return alerts;
  });

  alertsSummary = computed(() => ({ total: Object.values(this.coherenceAlerts()).length }));

  /** Options du <mat-select> type d'arrêt. */
  readonly typeArretOptions: { value: CongesPayesArretMaladieTypeArret; label: string }[] = [
    { value: 'MALADIE_NON_PROFESSIONNELLE',
      label: 'Maladie non professionnelle (2 j/mois, plafond 24 j/an)' },
    { value: 'ACCIDENT_TRAVAIL_MALADIE_PRO',
      label: 'Accident du travail / maladie professionnelle (2,5 j/mois, sans plafond)' },
  ];

  constructor(
    private service: CongesPayesArretMaladieService,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef,
    @Optional() private dashboardRefresh: CaseDashboardRefreshService | null,
  ) {}

  ngOnInit(): void {
    if (this.forceExpanded) this.collapsed.set(false);
    this.procedureChecksSignal.set(this.procedureChecks ?? []);
    this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    this.piecesManquantesSignal.set(this.piecesManquantes ?? []);
    this.prefillFromAi();
    if (this.workspaceCountry === 'FRANCE') {
      this.load();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['forceExpanded'] && this.forceExpanded) this.collapsed.set(false);
    if (changes['procedureChecks']) this.procedureChecksSignal.set(this.procedureChecks ?? []);
    if (changes['aiQuestions']) this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    if (changes['piecesManquantes']) this.piecesManquantesSignal.set(this.piecesManquantes ?? []);
    if (changes['aiData'] && !changes['aiData'].firstChange
        && this.showForm() && !this.result()) {
      this.prefillFromAi();
    }
  }

  /**
   * Pré-fill IA (SF-206-04) — renseigne les 6 champs (5 `cpArretMaladie*` +
   * `salaireBrutMensuel`) depuis le sous-objet `conges_payes_arret_maladie_detail`
   * (projeté à plat dans `travailExtractedData`).
   *
   * Parité stricte avec `getPrefillCount()` : mêmes mappings, même gate
   * `workspaceCountry === 'FRANCE'`. Délègue au helper partagé. No-op
   * gracieux si `aiData` absent ou dossier BE. N'écrase pas une saisie
   * avocat (provenance `null` AVEC valeur modifiée = modification manuelle).
   */
  private prefillFromAi(): void {
    // F-163 SF-163-02b : aucune source IA en standalone.
    if (this.standaloneMode) return;
    const ai = this.aiData;
    if (!ai) return;

    const rules = CongesPayesArretMaladieSectionPrefillRules;
    const ruleInput = { aiData: ai, workspaceCountry: this.workspaceCountry };

    // --- Type d'arrêt ---
    const type = rules.computeTypeArret(ruleInput);
    if (type !== null) {
      this.typeArret.set(type);
      this.provenanceTypeArret.set('IA');
    }

    // --- Nombre de mois d'arrêt ---
    const mois = rules.computeNombreMoisArret(ruleInput);
    if (mois !== null) {
      this.nombreMoisArret.set(mois);
      this.provenanceNombreMoisArret.set('IA');
    }

    // --- Salarié encore en poste ---
    const enPoste = rules.computeSalarieEnPoste(ruleInput);
    if (enPoste !== null) {
      this.salarieEncoreEnPoste.set(enPoste);
      this.provenanceSalarieEnPoste.set('IA');
    }

    // --- Date de rupture (si salarié sorti) ---
    const dateRupture = rules.computeDateRupture(ruleInput);
    if (dateRupture
        && (this.dateRuptureContrat() === null || this.provenanceDateRupture() === 'IA')) {
      this.dateRuptureContrat.set(dateRupture);
      this.provenanceDateRupture.set('IA');
    }

    // --- Jours déjà accordés ---
    const jours = rules.computeJoursDejaAccordes(ruleInput);
    if (jours !== null) {
      this.joursCpDejaAccordes.set(jours);
      this.provenanceJoursDejaAccordes.set('IA');
    }

    // --- Salaire brut mensuel (déjà extrait par le pré-fill existant) ---
    const salaire = rules.computeSalaireBrutMensuel(ruleInput);
    if (salaire !== null) {
      this.salaireBrutMensuel.set(salaire);
      this.provenanceSalaireBrutMensuel.set('IA');
    }
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  editMode(): void {
    this.showForm.set(true);
  }

  /**
   * Form valide : FRANCE + nombreMois > 0 + salaire ≥ 0 + jours déjà
   * accordés ≥ 0 + (dateRupture renseignée si salarié sorti).
   */
  formValid(): boolean {
    if (this.workspaceCountry !== 'FRANCE') return false;
    const mois = this.nombreMoisArret();
    if (typeof mois !== 'number' || mois <= 0) return false;
    const salaire = this.salaireBrutMensuel();
    if (typeof salaire !== 'number' || salaire < 0) return false;
    const jours = this.joursCpDejaAccordes();
    if (typeof jours !== 'number' || jours < 0) return false;
    if (!this.salarieEncoreEnPoste() && !this.dateRuptureContrat()) return false;
    return true;
  }

  // --- Handlers — toute modification manuelle efface le badge IA du champ ---

  onTypeArretChange(value: CongesPayesArretMaladieTypeArret): void {
    this.typeArret.set(value);
    this.provenanceTypeArret.set(null);
  }

  onNombreMoisArretChange(value: number | string | null): void {
    const num = typeof value === 'number' ? value : Number.parseInt(String(value ?? ''), 10);
    if (!Number.isFinite(num) || num < 0) {
      this.nombreMoisArret.set(0);
    } else {
      this.nombreMoisArret.set(Math.trunc(num));
    }
    this.provenanceNombreMoisArret.set(null);
  }

  onSalarieEnPosteChange(value: boolean): void {
    this.salarieEncoreEnPoste.set(value);
    this.provenanceSalarieEnPoste.set(null);
    if (value) {
      // Salarié en poste → on garde la date côté UI mais elle est ignorée
      // backend ; on évite de laisser un badge IA orphelin.
      this.dateRuptureContrat.set(null);
      this.provenanceDateRupture.set(null);
    }
  }

  onDateRuptureChange(value: string | null): void {
    this.dateRuptureContrat.set(value || null);
    this.provenanceDateRupture.set(null);
  }

  onJoursDejaAccordesChange(value: number | string | null): void {
    const num = typeof value === 'number' ? value : Number.parseFloat(String(value ?? ''));
    if (!Number.isFinite(num) || num < 0) {
      this.joursCpDejaAccordes.set(0);
    } else {
      this.joursCpDejaAccordes.set(num);
    }
    this.provenanceJoursDejaAccordes.set(null);
  }

  onSalaireBrutMensuelChange(value: number | string | null): void {
    const num = typeof value === 'number' ? value : Number.parseFloat(String(value ?? ''));
    if (!Number.isFinite(num) || num < 0) {
      this.salaireBrutMensuel.set(0);
    } else {
      this.salaireBrutMensuel.set(num);
    }
    this.provenanceSalaireBrutMensuel.set(null);
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request: CongesPayesArretMaladieRequest = {
      typeArret: this.typeArret(),
      nombreMoisArret: this.nombreMoisArret(),
      salarieEncoreEnPoste: this.salarieEncoreEnPoste(),
      dateRuptureContrat: this.salarieEncoreEnPoste()
        ? null
        : (this.dateRuptureContrat() || null),
      joursCpDejaAccordes: this.joursCpDejaAccordes(),
      salaireBrutMensuel: this.salaireBrutMensuel(),
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.applyResult(r);
        this.calculating.set(false);
        this.snackBar.open('Rappel de congés payés calculé', 'OK', { duration: 2500 });
        if (!this.standaloneMode) this.dashboardRefresh?.triggerRefresh();
        // OnPush + subscribe : forcer la détection de changement.
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

  private applyResult(r: CongesPayesArretMaladieResponse): void {
    this.result.set(r);
    this.hydrateForm(r);
    this.showForm.set(false);
  }

  /**
   * Ré-injecte le snapshot d'inputs de la réponse dans les champs du
   * formulaire — sans quoi un clic « Modifier » repartirait des valeurs par
   * défaut au lieu des dernières valeurs saisies.
   */
  private hydrateForm(r: CongesPayesArretMaladieResponse): void {
    this.typeArret.set(r.typeArret);
    this.nombreMoisArret.set(r.nombreMoisArret);
    this.salarieEncoreEnPoste.set(!!r.salarieEncoreEnPoste);
    this.dateRuptureContrat.set(r.dateRuptureContrat);
    this.joursCpDejaAccordes.set(r.joursCpDejaAccordes);
    this.salaireBrutMensuel.set(r.salaireBrutMensuel);
    // SF-206-04 : valeurs persistées = saisie avocat — jamais de badge IA.
    this.provenanceTypeArret.set(null);
    this.provenanceNombreMoisArret.set(null);
    this.provenanceSalarieEnPoste.set(null);
    this.provenanceDateRupture.set(null);
    this.provenanceJoursDejaAccordes.set(null);
    this.provenanceSalaireBrutMensuel.set(null);
  }

  // ---------------------------------------------------------------------------
  // Builders d'alertes F-IA-03 (un par field croisable)
  // ---------------------------------------------------------------------------

  /** Type d'arrêt — divergence stricte F-96 / question IA. */
  private buildTypeArretAlert(): CPMCoherenceAlert | null {
    const user = this.typeArret();
    const builder = CoherenceAlertBuilder.forField<CPMAlertField>('TYPE_ARRET');
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== F96_CODE_TYPE_ARRET) continue;
      const ev = chk.expectedValue?.trim().toUpperCase();
      if (!ev || ev === user) continue;
      builder.addSource('F96', {
        expectedDisplay: ev,
        reason: `Checklist procédurale : type d'arrêt attendu ${ev}${chk.raison ? ' (' + chk.raison + ')' : ''}`,
      });
      break;
    }
    for (const q of this.aiQuestionsSignal()) {
      if (q.critereCode?.toUpperCase() !== F96_CODE_TYPE_ARRET) continue;
      const ev = q.expectedValue?.trim().toUpperCase();
      if (!ev || ev === user) continue;
      builder.addSource('QUESTION_IA', {
        expectedDisplay: ev,
        reason: `Question complémentaire : "${q.questionText}" → "${q.answerText}"`,
      });
      break;
    }
    const piece = this.findPieceManquante(['DT75_TYPE_ARRET', 'ARRET_DE_TRAVAIL', 'CERTIFICAT_MEDICAL']);
    if (piece) builder.addPieceManquante(piece);
    return builder.build();
  }

  /** Durée de l'arrêt en mois — divergence numérique F-96 / question IA. */
  private buildDureeArretAlert(): CPMCoherenceAlert | null {
    const user = this.nombreMoisArret();
    const builder = CoherenceAlertBuilder.forField<CPMAlertField>('DUREE_ARRET');
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== F96_CODE_DUREE_ARRET) continue;
      const ev = chk.expectedValue?.trim();
      if (!ev || ev === String(user)) continue;
      builder.addSource('F96', {
        expectedDisplay: ev,
        reason: `Checklist procédurale : durée attendue ${ev} mois${chk.raison ? ' (' + chk.raison + ')' : ''}`,
      });
      break;
    }
    for (const q of this.aiQuestionsSignal()) {
      if (q.critereCode?.toUpperCase() !== F96_CODE_DUREE_ARRET) continue;
      const ev = q.expectedValue?.trim();
      if (!ev || ev === String(user)) continue;
      builder.addSource('QUESTION_IA', {
        expectedDisplay: ev,
        reason: `Question complémentaire : "${q.questionText}" → "${q.answerText}"`,
      });
      break;
    }
    return builder.build();
  }

  /** Salarié encore en poste — divergence F-96 / question IA. */
  private buildSalarieEnPosteAlert(): CPMCoherenceAlert | null {
    const user = this.salarieEncoreEnPoste();
    const builder = CoherenceAlertBuilder.forField<CPMAlertField>('SALARIE_EN_POSTE');
    const userDisplay = user ? 'Encore en poste' : 'Contrat rompu';
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== F96_CODE_SALARIE_EN_POSTE) continue;
      const expected = this.parseBoolean(chk.expectedValue);
      if (expected === null || expected === user) continue;
      builder.addSource('F96', {
        expectedDisplay: expected ? 'Encore en poste' : 'Contrat rompu',
        reason: `Checklist procédurale : salarié ${expected ? 'encore en poste' : 'sorti'}${chk.raison ? ' (' + chk.raison + ')' : ''} — saisie : ${userDisplay.toLowerCase()}`,
      });
      break;
    }
    for (const q of this.aiQuestionsSignal()) {
      if (q.critereCode?.toUpperCase() !== F96_CODE_SALARIE_EN_POSTE) continue;
      const expected = this.parseBoolean(q.expectedValue);
      if (expected === null || expected === user) continue;
      builder.addSource('QUESTION_IA', {
        expectedDisplay: expected ? 'Encore en poste' : 'Contrat rompu',
        reason: `Question complémentaire : "${q.questionText}" → "${q.answerText}"`,
      });
      break;
    }
    return builder.build();
  }

  /** Parse une `expectedValue` textuelle en booléen, ou null si non mappable. */
  private parseBoolean(raw: string | null | undefined): boolean | null {
    if (!raw) return null;
    const v = raw.trim().toLowerCase();
    if (v === 'true' || v === 'oui' || v === '1') return true;
    if (v === 'false' || v === 'non' || v === '0') return false;
    return null;
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

  alertTooltip(alert: CPMCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: CPMCoherenceAlert): string {
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

  // ---------------------------------------------------------------------------
  // Helpers d'affichage du résultat
  // ---------------------------------------------------------------------------

  /**
   * Verdict → bannière de couleur. 4 niveaux :
   *  - RAPPEL_SIGNIFICATIF → orange (call-to-action — montant matériel)
   *  - RAPPEL_LIMITE → navy (rappel existe mais limité)
   *  - PAS_DE_RAPPEL → navy grisé (rien à demander)
   *  - ACTION_FORCLOSE → rouge (alerte — délai dépassé)
   */
  verdictBannerClass(verdict: CongesPayesArretMaladieVerdict): string {
    switch (verdict) {
      case 'RAPPEL_SIGNIFICATIF':
        return 'cpm-verdict-banner cpm-verdict-banner--significant';
      case 'RAPPEL_LIMITE':
        return 'cpm-verdict-banner cpm-verdict-banner--limited';
      case 'PAS_DE_RAPPEL':
        return 'cpm-verdict-banner cpm-verdict-banner--none';
      case 'ACTION_FORCLOSE':
        return 'cpm-verdict-banner cpm-verdict-banner--danger';
    }
  }

  verdictBannerLabel(verdict: CongesPayesArretMaladieVerdict): string {
    switch (verdict) {
      case 'RAPPEL_SIGNIFICATIF': return 'Rappel de congés payés significatif';
      case 'RAPPEL_LIMITE': return 'Rappel de congés payés limité';
      case 'PAS_DE_RAPPEL': return 'Pas de rappel à demander';
      case 'ACTION_FORCLOSE': return 'Action forclose — délai dépassé';
    }
  }

  verdictBannerIcon(verdict: CongesPayesArretMaladieVerdict): string {
    switch (verdict) {
      case 'RAPPEL_SIGNIFICATIF': return 'priority_high';
      case 'RAPPEL_LIMITE': return 'check_circle';
      case 'PAS_DE_RAPPEL': return 'info_outline';
      case 'ACTION_FORCLOSE': return 'error';
    }
  }
}
