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
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { PriseActeRuptureService } from '../../core/services/prise-acte-rupture.service';
import {
  PriseActeRuptureEffetProbable,
  PriseActeRuptureRequest,
  PriseActeRuptureResponse,
  PriseActeRuptureVerdict,
} from '../../core/models/prise-acte-rupture.model';
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
import { PriseActeRuptureSectionPrefillRules } from './prise-acte-rupture-section-prefill-rules';

/**
 * SF-206-06 : champs F-IA-03 audités par l'outil F-DT-39.
 * Cinq champs croisables avec la checklist procédurale F-96 / les questions
 * IA / les pièces manquantes (cf. SF-206-05 §critereCode).
 */
export type PARAlertField =
  | 'DEFAUT_PAIEMENT'
  | 'HARCELEMENT'
  | 'MANQUEMENT_SECURITE'
  | 'MODIFICATION_CONTRAT'
  | 'GRIEF_IMPOSSIBLE_POURSUITE';

export type PARCoherenceAlert = CoherenceAlert<PARAlertField>;

/** Codes `critereCode` (F-96 / questions IA) reconnus par field. */
const F96_CODE_DEFAUT_PAIEMENT = 'DT39_DEFAUT_PAIEMENT';
const F96_CODE_HARCELEMENT = 'DT39_HARCELEMENT';
const F96_CODE_MANQUEMENT_SECURITE = 'DT39_MANQUEMENT_SECURITE';
const F96_CODE_MODIFICATION_CONTRAT = 'DT39_MODIFICATION_CONTRAT';
const F96_CODE_GRIEF_IMPOSSIBLE_POURSUITE = 'DT39_GRIEF_IMPOSSIBLE_POURSUITE';

/**
 * SF-206-06 : composant Angular standalone pour l'outil décisionnel "Prise
 * d'acte de la rupture aux torts de l'employeur" (F-DT-39). FRANCE uniquement
 * (Cass. soc. 25/06/2003 n°01-42.679 — mécanisme franco-français).
 *
 * Pattern de référence : `conges-payes-arret-maladie-section` (F-DT-75,
 * SF-206-04) — formulaire de saisie, POST de calcul, verdict 3 niveaux,
 * F-IA-03, refresh dashboard, OnPush + markForCheck dans les subscribe.
 *
 * Verdict 3 niveaux :
 *  - PRISE_ACTE_FAVORABLE (navy/vert — effets licenciement probables)
 *  - PRISE_ACTE_RISQUEE (or — issue incertaine, à comparer avec résiliation)
 *  - PRISE_ACTE_DEFAVORABLE (rouge — effets démission probables = catastrophe)
 *
 * Avertissement persistant : la prise d'acte est une rupture **immédiate** ;
 * un verdict défavorable = effets démission (perte d'indemnités + chômage). À
 * comparer avec l'outil **résiliation judiciaire** (SF-206-07) qui maintient
 * le contrat pendant l'instance.
 */
@Component({
  selector: 'app-prise-acte-rupture-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatSlideToggleModule,
    MatProgressSpinnerModule,
    LegalCitationsPipe,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './prise-acte-rupture-section.component.html',
  styleUrl: './prise-acte-rupture-section.component.scss',
})
export class PriseActeRuptureSectionComponent implements OnInit, OnChanges {
  // F-177 SF-177-03b : metadata statique consommée par le panel pour la card.
  static readonly TOOL_LABEL = 'PRISE D\'ACTE DE LA RUPTURE';
  static readonly TOOL_ICON = 'gavel';

  /**
   * SF-244 — délégué au helper partagé (parité stricte avec `prefillFromAi()`).
   * Retourne le nombre exact de champs pré-remplissables FR pour le badge tab.
   * Couvre les 11 champs `priseActe*`.
   */
  static getPrefillCount(input: {
    aiData?: TravailExtractedData | null;
    procedureChecks?: unknown[];
    aiQuestions?: unknown[];
    piecesManquantes?: unknown[];
    triggerEvents?: unknown[];
    workspaceCountry?: string;
  }): number {
    return PriseActeRuptureSectionPrefillRules.computePrefillCount({
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

  // Snapshots signal des inputs IA pour réactivité des `computed`.
  private procedureChecksSignal = signal<ProcedureCheck[]>([]);
  private aiQuestionsSignal = signal<AiQuestion[]>([]);
  private piecesManquantesSignal = signal<PieceManquanteEntry[]>([]);

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<PriseActeRuptureResponse | null>(null);

  // --- Form fields (12 champs du contrat) ---
  defautPaiementSalaire = signal<boolean>(false);
  montantImpayesEur = signal<number | null>(null);
  harcelement = signal<boolean>(false);
  manquementSecurite = signal<boolean>(false);
  modificationUnilateraleContrat = signal<boolean>(false);
  declassement = signal<boolean>(false);
  discrimination = signal<boolean>(false);
  heuresSupNonPayees = signal<boolean>(false);
  nonRespectDureesRepos = signal<boolean>(false);
  griefsActuelsEtPersistants = signal<boolean>(true);
  griefRendImpossiblePoursuite = signal<boolean>(false);
  griefsCommentaire = signal<string>('');

  /**
   * SF-206-06 : provenance IA par champ pré-rempli. `'IA'` tant que la valeur
   * provient de l'analyse ; remis à `null` dès qu'un handler `onXxxChange()`
   * détecte une modification manuelle (le badge `auto_awesome` disparaît).
   */
  provenanceDefautPaiement = signal<'IA' | null>(null);
  provenanceMontantImpayes = signal<'IA' | null>(null);
  provenanceHarcelement = signal<'IA' | null>(null);
  provenanceManquementSecurite = signal<'IA' | null>(null);
  provenanceModificationContrat = signal<'IA' | null>(null);
  provenanceDeclassement = signal<'IA' | null>(null);
  provenanceDiscrimination = signal<'IA' | null>(null);
  provenanceHeuresSupNonPayees = signal<'IA' | null>(null);
  provenanceNonRespectRepos = signal<'IA' | null>(null);
  provenanceGriefsPersistants = signal<'IA' | null>(null);
  provenanceGriefImpossiblePoursuite = signal<'IA' | null>(null);

  /**
   * Coherence alerts F-IA-03 — gate `showForm()` strict (alertes masquées
   * après calcul rendu). Aucune source IA en standalone.
   */
  coherenceAlerts = computed<Partial<Record<PARAlertField, PARCoherenceAlert>>>(() => {
    if (this.standaloneMode) return {};
    if (!this.showForm()) return {};
    const alerts: Partial<Record<PARAlertField, PARCoherenceAlert>> = {};
    const defautPaiementAlert = this.buildBooleanAlert(
      'DEFAUT_PAIEMENT', F96_CODE_DEFAUT_PAIEMENT,
      this.defautPaiementSalaire(), 'Défaut de paiement du salaire',
    );
    if (defautPaiementAlert) alerts.DEFAUT_PAIEMENT = defautPaiementAlert;
    const harcelementAlert = this.buildBooleanAlert(
      'HARCELEMENT', F96_CODE_HARCELEMENT,
      this.harcelement(), 'Harcèlement moral ou sexuel',
    );
    if (harcelementAlert) alerts.HARCELEMENT = harcelementAlert;
    const securiteAlert = this.buildBooleanAlert(
      'MANQUEMENT_SECURITE', F96_CODE_MANQUEMENT_SECURITE,
      this.manquementSecurite(), 'Manquement à l\'obligation de sécurité',
    );
    if (securiteAlert) alerts.MANQUEMENT_SECURITE = securiteAlert;
    const modifAlert = this.buildBooleanAlert(
      'MODIFICATION_CONTRAT', F96_CODE_MODIFICATION_CONTRAT,
      this.modificationUnilateraleContrat(), 'Modification unilatérale du contrat',
    );
    if (modifAlert) alerts.MODIFICATION_CONTRAT = modifAlert;
    const impossiblePoursuiteAlert = this.buildBooleanAlert(
      'GRIEF_IMPOSSIBLE_POURSUITE', F96_CODE_GRIEF_IMPOSSIBLE_POURSUITE,
      this.griefRendImpossiblePoursuite(), 'Grief rendant impossible la poursuite du contrat',
    );
    if (impossiblePoursuiteAlert) alerts.GRIEF_IMPOSSIBLE_POURSUITE = impossiblePoursuiteAlert;
    return alerts;
  });

  alertsSummary = computed(() => ({ total: Object.values(this.coherenceAlerts()).length }));

  constructor(
    private service: PriseActeRuptureService,
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
   * Pré-fill IA (SF-206-06) — renseigne les 11 champs (8 griefs + montant +
   * 2 modulateurs) depuis le sous-objet `prise_acte_detail` (projeté à plat
   * dans `travailExtractedData`).
   *
   * Parité stricte avec `getPrefillCount()` : mêmes mappings, même gate
   * `workspaceCountry === 'FRANCE'`. Délègue au helper partagé. No-op
   * gracieux si `aiData` absent ou dossier BE.
   */
  private prefillFromAi(): void {
    // F-163 SF-163-02b : aucune source IA en standalone.
    if (this.standaloneMode) return;
    const ai = this.aiData;
    if (!ai) return;

    const rules = PriseActeRuptureSectionPrefillRules;
    const ruleInput = { aiData: ai, workspaceCountry: this.workspaceCountry };

    const defaut = rules.computeDefautPaiement(ruleInput);
    if (defaut !== null) {
      this.defautPaiementSalaire.set(defaut);
      this.provenanceDefautPaiement.set('IA');
    }
    const montant = rules.computeMontantImpayes(ruleInput);
    if (montant !== null) {
      this.montantImpayesEur.set(montant);
      this.provenanceMontantImpayes.set('IA');
    }
    const harcelement = rules.computeHarcelement(ruleInput);
    if (harcelement !== null) {
      this.harcelement.set(harcelement);
      this.provenanceHarcelement.set('IA');
    }
    const securite = rules.computeManquementSecurite(ruleInput);
    if (securite !== null) {
      this.manquementSecurite.set(securite);
      this.provenanceManquementSecurite.set('IA');
    }
    const modification = rules.computeModificationContrat(ruleInput);
    if (modification !== null) {
      this.modificationUnilateraleContrat.set(modification);
      this.provenanceModificationContrat.set('IA');
    }
    const declassement = rules.computeDeclassement(ruleInput);
    if (declassement !== null) {
      this.declassement.set(declassement);
      this.provenanceDeclassement.set('IA');
    }
    const discrimination = rules.computeDiscrimination(ruleInput);
    if (discrimination !== null) {
      this.discrimination.set(discrimination);
      this.provenanceDiscrimination.set('IA');
    }
    const heuresSup = rules.computeHeuresSupNonPayees(ruleInput);
    if (heuresSup !== null) {
      this.heuresSupNonPayees.set(heuresSup);
      this.provenanceHeuresSupNonPayees.set('IA');
    }
    const repos = rules.computeNonRespectRepos(ruleInput);
    if (repos !== null) {
      this.nonRespectDureesRepos.set(repos);
      this.provenanceNonRespectRepos.set('IA');
    }
    const persistants = rules.computeGriefsPersistants(ruleInput);
    if (persistants !== null) {
      this.griefsActuelsEtPersistants.set(persistants);
      this.provenanceGriefsPersistants.set('IA');
    }
    const impossiblePoursuite = rules.computeGriefImpossiblePoursuite(ruleInput);
    if (impossiblePoursuite !== null) {
      this.griefRendImpossiblePoursuite.set(impossiblePoursuite);
      this.provenanceGriefImpossiblePoursuite.set('IA');
    }
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  editMode(): void {
    this.showForm.set(true);
  }

  /** Form valide : FRANCE + montant ≥ 0 si renseigné. */
  formValid(): boolean {
    if (this.workspaceCountry !== 'FRANCE') return false;
    const m = this.montantImpayesEur();
    if (m !== null && (!Number.isFinite(m) || m < 0)) return false;
    return true;
  }

  // --- Handlers — toute modification manuelle efface le badge IA du champ ---

  onDefautPaiementChange(value: boolean): void {
    this.defautPaiementSalaire.set(value);
    this.provenanceDefautPaiement.set(null);
    if (!value) {
      // Pas de défaut de paiement → montant non pertinent.
      this.montantImpayesEur.set(null);
      this.provenanceMontantImpayes.set(null);
    }
  }

  onMontantImpayesChange(value: number | string | null): void {
    if (value === null || value === '' || value === undefined) {
      this.montantImpayesEur.set(null);
    } else {
      const num = typeof value === 'number' ? value : Number.parseFloat(String(value));
      if (!Number.isFinite(num) || num < 0) {
        this.montantImpayesEur.set(0);
      } else {
        this.montantImpayesEur.set(num);
      }
    }
    this.provenanceMontantImpayes.set(null);
  }

  onHarcelementChange(value: boolean): void {
    this.harcelement.set(value);
    this.provenanceHarcelement.set(null);
  }

  onManquementSecuriteChange(value: boolean): void {
    this.manquementSecurite.set(value);
    this.provenanceManquementSecurite.set(null);
  }

  onModificationContratChange(value: boolean): void {
    this.modificationUnilateraleContrat.set(value);
    this.provenanceModificationContrat.set(null);
  }

  onDeclassementChange(value: boolean): void {
    this.declassement.set(value);
    this.provenanceDeclassement.set(null);
  }

  onDiscriminationChange(value: boolean): void {
    this.discrimination.set(value);
    this.provenanceDiscrimination.set(null);
  }

  onHeuresSupNonPayeesChange(value: boolean): void {
    this.heuresSupNonPayees.set(value);
    this.provenanceHeuresSupNonPayees.set(null);
  }

  onNonRespectReposChange(value: boolean): void {
    this.nonRespectDureesRepos.set(value);
    this.provenanceNonRespectRepos.set(null);
  }

  onGriefsPersistantsChange(value: boolean): void {
    this.griefsActuelsEtPersistants.set(value);
    this.provenanceGriefsPersistants.set(null);
  }

  onGriefImpossiblePoursuiteChange(value: boolean): void {
    this.griefRendImpossiblePoursuite.set(value);
    this.provenanceGriefImpossiblePoursuite.set(null);
  }

  onGriefsCommentaireChange(value: string): void {
    this.griefsCommentaire.set(value ?? '');
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request: PriseActeRuptureRequest = {
      defautPaiementSalaire: this.defautPaiementSalaire(),
      montantImpayesEur: this.defautPaiementSalaire() ? this.montantImpayesEur() : null,
      harcelement: this.harcelement(),
      manquementSecurite: this.manquementSecurite(),
      modificationUnilateraleContrat: this.modificationUnilateraleContrat(),
      declassement: this.declassement(),
      discrimination: this.discrimination(),
      heuresSupNonPayees: this.heuresSupNonPayees(),
      nonRespectDureesRepos: this.nonRespectDureesRepos(),
      griefsActuelsEtPersistants: this.griefsActuelsEtPersistants(),
      griefRendImpossiblePoursuite: this.griefRendImpossiblePoursuite(),
      griefsCommentaire: this.griefsCommentaire().trim() || null,
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.applyResult(r);
        this.calculating.set(false);
        this.snackBar.open('Analyse de prise d\'acte calculée', 'OK', { duration: 2500 });
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

  private applyResult(r: PriseActeRuptureResponse): void {
    this.result.set(r);
    this.hydrateForm(r);
    this.showForm.set(false);
  }

  /**
   * Ré-injecte le snapshot d'inputs de la réponse dans les champs du
   * formulaire — sans quoi un clic « Modifier » repartirait des valeurs par
   * défaut au lieu des dernières valeurs saisies.
   */
  private hydrateForm(r: PriseActeRuptureResponse): void {
    this.defautPaiementSalaire.set(!!r.defautPaiementSalaire);
    this.montantImpayesEur.set(r.montantImpayesEur);
    this.harcelement.set(!!r.harcelement);
    this.manquementSecurite.set(!!r.manquementSecurite);
    this.modificationUnilateraleContrat.set(!!r.modificationUnilateraleContrat);
    this.declassement.set(!!r.declassement);
    this.discrimination.set(!!r.discrimination);
    this.heuresSupNonPayees.set(!!r.heuresSupNonPayees);
    this.nonRespectDureesRepos.set(!!r.nonRespectDureesRepos);
    this.griefsActuelsEtPersistants.set(!!r.griefsActuelsEtPersistants);
    this.griefRendImpossiblePoursuite.set(!!r.griefRendImpossiblePoursuite);
    this.griefsCommentaire.set(r.griefsCommentaire ?? '');
    // SF-206-06 : valeurs persistées = saisie avocat — jamais de badge IA.
    this.provenanceDefautPaiement.set(null);
    this.provenanceMontantImpayes.set(null);
    this.provenanceHarcelement.set(null);
    this.provenanceManquementSecurite.set(null);
    this.provenanceModificationContrat.set(null);
    this.provenanceDeclassement.set(null);
    this.provenanceDiscrimination.set(null);
    this.provenanceHeuresSupNonPayees.set(null);
    this.provenanceNonRespectRepos.set(null);
    this.provenanceGriefsPersistants.set(null);
    this.provenanceGriefImpossiblePoursuite.set(null);
  }

  // ---------------------------------------------------------------------------
  // F-IA-03 — builder d'alertes générique pour les 5 champs booléens audités
  // ---------------------------------------------------------------------------

  /**
   * Construit une alerte F-IA-03 pour un champ booléen donné, en croisant la
   * saisie courante avec la checklist procédurale F-96 et les questions IA.
   */
  private buildBooleanAlert(
    field: PARAlertField,
    code: string,
    user: boolean,
    libelle: string,
  ): PARCoherenceAlert | null {
    const builder = CoherenceAlertBuilder.forField<PARAlertField>(field);
    const userDisplay = user ? 'Coché' : 'Non coché';
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== code) continue;
      const expected = this.parseBoolean(chk.expectedValue);
      if (expected === null || expected === user) continue;
      builder.addSource('F96', {
        expectedDisplay: expected ? 'Coché' : 'Non coché',
        reason: `Checklist procédurale (${libelle}) : ${expected ? 'attendu coché' : 'attendu non coché'}${chk.raison ? ' (' + chk.raison + ')' : ''} — saisie : ${userDisplay.toLowerCase()}`,
      });
      break;
    }
    for (const q of this.aiQuestionsSignal()) {
      if (q.critereCode?.toUpperCase() !== code) continue;
      const expected = this.parseBoolean(q.expectedValue);
      if (expected === null || expected === user) continue;
      builder.addSource('QUESTION_IA', {
        expectedDisplay: expected ? 'Coché' : 'Non coché',
        reason: `Question complémentaire (${libelle}) : "${q.questionText}" → "${q.answerText}"`,
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

  alertTooltip(alert: PARCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: PARCoherenceAlert): string {
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
   * Verdict → bannière de couleur. 3 niveaux :
   *  - PRISE_ACTE_FAVORABLE → navy/vert (favorable)
   *  - PRISE_ACTE_RISQUEE → or (issue incertaine)
   *  - PRISE_ACTE_DEFAVORABLE → rouge (effets démission = catastrophe)
   */
  verdictBannerClass(verdict: PriseActeRuptureVerdict): string {
    switch (verdict) {
      case 'PRISE_ACTE_FAVORABLE':
        return 'par-verdict-banner par-verdict-banner--favorable';
      case 'PRISE_ACTE_RISQUEE':
        return 'par-verdict-banner par-verdict-banner--risquee';
      case 'PRISE_ACTE_DEFAVORABLE':
        return 'par-verdict-banner par-verdict-banner--defavorable';
    }
  }

  verdictBannerLabel(verdict: PriseActeRuptureVerdict): string {
    switch (verdict) {
      case 'PRISE_ACTE_FAVORABLE': return 'Prise d\'acte favorable';
      case 'PRISE_ACTE_RISQUEE': return 'Prise d\'acte risquée';
      case 'PRISE_ACTE_DEFAVORABLE': return 'Prise d\'acte défavorable';
    }
  }

  verdictBannerIcon(verdict: PriseActeRuptureVerdict): string {
    switch (verdict) {
      case 'PRISE_ACTE_FAVORABLE': return 'check_circle';
      case 'PRISE_ACTE_RISQUEE': return 'warning';
      case 'PRISE_ACTE_DEFAVORABLE': return 'error';
    }
  }

  /** Effet probable → libellé + classe de couleur (visual distinct from verdict). */
  effetProbableLabel(effet: PriseActeRuptureEffetProbable): string {
    switch (effet) {
      case 'LICENCIEMENT_SANS_CAUSE': return 'Licenciement sans cause réelle et sérieuse';
      case 'LICENCIEMENT_NUL': return 'Licenciement NUL (harcèlement / discrimination)';
      case 'DEMISSION': return 'Démission (perte indemnités + allocations chômage)';
    }
  }

  effetProbableClass(effet: PriseActeRuptureEffetProbable): string {
    switch (effet) {
      case 'LICENCIEMENT_SANS_CAUSE':
        return 'par-effet-pill par-effet-pill--licenciement-sans-cause';
      case 'LICENCIEMENT_NUL':
        return 'par-effet-pill par-effet-pill--licenciement-nul';
      case 'DEMISSION':
        return 'par-effet-pill par-effet-pill--demission';
    }
  }
}
