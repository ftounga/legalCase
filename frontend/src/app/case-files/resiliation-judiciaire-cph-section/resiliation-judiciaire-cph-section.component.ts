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
import { ResiliationJudiciaireCphService } from '../../core/services/resiliation-judiciaire-cph.service';
import {
  ResiliationJudiciaireCphDateEffetProbable,
  ResiliationJudiciaireCphRequest,
  ResiliationJudiciaireCphResponse,
  ResiliationJudiciaireCphVerdict,
} from '../../core/models/resiliation-judiciaire-cph.model';
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
import { ResiliationJudiciaireCphSectionPrefillRules } from './resiliation-judiciaire-cph-section-prefill-rules';

/**
 * SF-206-08 : champs F-IA-03 audités par l'outil F-DT-40.
 * Cinq champs croisables avec la checklist procédurale F-96 / les questions
 * IA / les pièces manquantes (cf. SF-206-07 §critereCode).
 */
export type RJCAlertField =
  | 'DEFAUT_PAIEMENT'
  | 'HARCELEMENT'
  | 'MANQUEMENT_SECURITE'
  | 'MODIFICATION_CONTRAT'
  | 'MANQUEMENTS_PERSISTANTS';

export type RJCCoherenceAlert = CoherenceAlert<RJCAlertField>;

/** Codes `critereCode` (F-96 / questions IA) reconnus par field. */
const F96_CODE_DEFAUT_PAIEMENT = 'DT40_DEFAUT_PAIEMENT';
const F96_CODE_HARCELEMENT = 'DT40_HARCELEMENT';
const F96_CODE_MANQUEMENT_SECURITE = 'DT40_MANQUEMENT_SECURITE';
const F96_CODE_MODIFICATION_CONTRAT = 'DT40_MODIFICATION_CONTRAT';
const F96_CODE_MANQUEMENTS_PERSISTANTS = 'DT40_MANQUEMENTS_PERSISTANTS';

/**
 * SF-206-08 : composant Angular standalone pour l'outil décisionnel
 * "Résiliation judiciaire du contrat aux torts de l'employeur" (F-DT-40).
 * FRANCE uniquement (Cass. soc. 16/03/1989 ; Cass. soc. 20/01/1998 ; art.
 * L.1411-1 CT ; art. 1224, 1227-1228 C. civ. — mécanisme franco-français).
 *
 * Pattern de référence : `prise-acte-rupture-section` (F-DT-39, SF-206-06) —
 * outil jumeau. Formulaire de saisie, POST de calcul, verdict 3 niveaux,
 * F-IA-03, refresh dashboard, OnPush + markForCheck dans les subscribe.
 *
 * Verdict 3 niveaux :
 *  - RESILIATION_FAVORABLE (vert — manquements graves et persistants : la
 *    résiliation est sérieusement envisageable)
 *  - RESILIATION_INCERTAINE (or — issue incertaine)
 *  - RESILIATION_DEFAVORABLE (navy/grisé — manquements insuffisants ; PAS
 *    rouge car le rejet ne rompt PAS le contrat, voie sans risque)
 *
 * Message persistant : la résiliation judiciaire est une voie **moins
 * risquée** que la prise d'acte — un rejet ne rompt pas le contrat, le
 * salarié reste en poste pendant l'instance. À comparer avec l'outil
 * **prise d'acte** (SF-206-06) qui rompt immédiatement.
 *
 * Date d'effet probable :
 *  - DATE_DECISION : effets licenciement sans cause à la date du jugement
 *    (cas nominal — Cass. soc. 11/01/2007 n°05-40.626).
 *  - DATE_LICENCIEMENT : effets reportés au licenciement intervenu en cours
 *    d'instance (Cass. soc. 21/12/2006 n°05-42.251).
 */
@Component({
  selector: 'app-resiliation-judiciaire-cph-section',
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
  templateUrl: './resiliation-judiciaire-cph-section.component.html',
  styleUrl: './resiliation-judiciaire-cph-section.component.scss',
})
export class ResiliationJudiciaireCphSectionComponent implements OnInit, OnChanges {
  // F-177 SF-177-03b : metadata statique consommée par le panel pour la card.
  static readonly TOOL_LABEL = 'RÉSILIATION JUDICIAIRE DU CONTRAT';
  static readonly TOOL_ICON = 'balance';

  /**
   * SF-244 — délégué au helper partagé (parité stricte avec `prefillFromAi()`).
   * Retourne le nombre exact de champs pré-remplissables FR pour le badge tab.
   * Couvre les 12 champs `resiliationJud*`.
   */
  static getPrefillCount(input: {
    aiData?: TravailExtractedData | null;
    procedureChecks?: unknown[];
    aiQuestions?: unknown[];
    piecesManquantes?: unknown[];
    triggerEvents?: unknown[];
    workspaceCountry?: string;
  }): number {
    return ResiliationJudiciaireCphSectionPrefillRules.computePrefillCount({
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
  result = signal<ResiliationJudiciaireCphResponse | null>(null);

  // --- Form fields (13 champs du contrat) ---
  defautPaiementSalaire = signal<boolean>(false);
  montantImpayesEur = signal<number | null>(null);
  harcelement = signal<boolean>(false);
  manquementSecurite = signal<boolean>(false);
  modificationUnilateraleContrat = signal<boolean>(false);
  declassement = signal<boolean>(false);
  discrimination = signal<boolean>(false);
  heuresSupNonPayees = signal<boolean>(false);
  nonRespectDureesRepos = signal<boolean>(false);
  manquementsPersistantsAuJourDemande = signal<boolean>(true);
  salarieToujoursEnPoste = signal<boolean>(true);
  licenciementIntervenuEnCoursInstance = signal<boolean>(false);
  manquementsCommentaire = signal<string>('');

  /**
   * SF-206-08 : provenance IA par champ pré-rempli. `'IA'` tant que la valeur
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
  provenanceManquementsPersistants = signal<'IA' | null>(null);
  provenanceSalarieEnPoste = signal<'IA' | null>(null);
  provenanceLicenciementEnCours = signal<'IA' | null>(null);

  /**
   * Coherence alerts F-IA-03 — gate `showForm()` strict (alertes masquées
   * après calcul rendu). Aucune source IA en standalone.
   */
  coherenceAlerts = computed<Partial<Record<RJCAlertField, RJCCoherenceAlert>>>(() => {
    if (this.standaloneMode) return {};
    if (!this.showForm()) return {};
    const alerts: Partial<Record<RJCAlertField, RJCCoherenceAlert>> = {};
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
    const persistantsAlert = this.buildBooleanAlert(
      'MANQUEMENTS_PERSISTANTS', F96_CODE_MANQUEMENTS_PERSISTANTS,
      this.manquementsPersistantsAuJourDemande(), 'Manquements persistants au jour de la demande',
    );
    if (persistantsAlert) alerts.MANQUEMENTS_PERSISTANTS = persistantsAlert;
    return alerts;
  });

  alertsSummary = computed(() => ({ total: Object.values(this.coherenceAlerts()).length }));

  constructor(
    private service: ResiliationJudiciaireCphService,
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
   * Pré-fill IA (SF-206-08) — renseigne les 12 champs (8 manquements + montant
   * + 3 modulateurs) depuis le sous-objet `resiliation_judiciaire_detail`
   * (projeté à plat dans `travailExtractedData`).
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

    const rules = ResiliationJudiciaireCphSectionPrefillRules;
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
    const persistants = rules.computeManquementsPersistants(ruleInput);
    if (persistants !== null) {
      this.manquementsPersistantsAuJourDemande.set(persistants);
      this.provenanceManquementsPersistants.set('IA');
    }
    const enPoste = rules.computeSalarieEnPoste(ruleInput);
    if (enPoste !== null) {
      this.salarieToujoursEnPoste.set(enPoste);
      this.provenanceSalarieEnPoste.set('IA');
    }
    const licEnCours = rules.computeLicenciementEnCours(ruleInput);
    if (licEnCours !== null) {
      this.licenciementIntervenuEnCoursInstance.set(licEnCours);
      this.provenanceLicenciementEnCours.set('IA');
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

  onManquementsPersistantsChange(value: boolean): void {
    this.manquementsPersistantsAuJourDemande.set(value);
    this.provenanceManquementsPersistants.set(null);
  }

  onSalarieEnPosteChange(value: boolean): void {
    this.salarieToujoursEnPoste.set(value);
    this.provenanceSalarieEnPoste.set(null);
  }

  onLicenciementEnCoursChange(value: boolean): void {
    this.licenciementIntervenuEnCoursInstance.set(value);
    this.provenanceLicenciementEnCours.set(null);
  }

  onManquementsCommentaireChange(value: string): void {
    this.manquementsCommentaire.set(value ?? '');
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request: ResiliationJudiciaireCphRequest = {
      defautPaiementSalaire: this.defautPaiementSalaire(),
      montantImpayesEur: this.defautPaiementSalaire() ? this.montantImpayesEur() : null,
      harcelement: this.harcelement(),
      manquementSecurite: this.manquementSecurite(),
      modificationUnilateraleContrat: this.modificationUnilateraleContrat(),
      declassement: this.declassement(),
      discrimination: this.discrimination(),
      heuresSupNonPayees: this.heuresSupNonPayees(),
      nonRespectDureesRepos: this.nonRespectDureesRepos(),
      manquementsPersistantsAuJourDemande: this.manquementsPersistantsAuJourDemande(),
      salarieToujoursEnPoste: this.salarieToujoursEnPoste(),
      licenciementIntervenuEnCoursInstance: this.licenciementIntervenuEnCoursInstance(),
      manquementsCommentaire: this.manquementsCommentaire().trim() || null,
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.applyResult(r);
        this.calculating.set(false);
        this.snackBar.open('Analyse de résiliation judiciaire calculée', 'OK', { duration: 2500 });
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

  private applyResult(r: ResiliationJudiciaireCphResponse): void {
    this.result.set(r);
    this.hydrateForm(r);
    this.showForm.set(false);
  }

  /**
   * Ré-injecte le snapshot d'inputs de la réponse dans les champs du
   * formulaire — sans quoi un clic « Modifier » repartirait des valeurs par
   * défaut au lieu des dernières valeurs saisies.
   */
  private hydrateForm(r: ResiliationJudiciaireCphResponse): void {
    this.defautPaiementSalaire.set(!!r.defautPaiementSalaire);
    this.montantImpayesEur.set(r.montantImpayesEur);
    this.harcelement.set(!!r.harcelement);
    this.manquementSecurite.set(!!r.manquementSecurite);
    this.modificationUnilateraleContrat.set(!!r.modificationUnilateraleContrat);
    this.declassement.set(!!r.declassement);
    this.discrimination.set(!!r.discrimination);
    this.heuresSupNonPayees.set(!!r.heuresSupNonPayees);
    this.nonRespectDureesRepos.set(!!r.nonRespectDureesRepos);
    this.manquementsPersistantsAuJourDemande.set(!!r.manquementsPersistantsAuJourDemande);
    this.salarieToujoursEnPoste.set(!!r.salarieToujoursEnPoste);
    this.licenciementIntervenuEnCoursInstance.set(!!r.licenciementIntervenuEnCoursInstance);
    this.manquementsCommentaire.set(r.manquementsCommentaire ?? '');
    // SF-206-08 : valeurs persistées = saisie avocat — jamais de badge IA.
    this.provenanceDefautPaiement.set(null);
    this.provenanceMontantImpayes.set(null);
    this.provenanceHarcelement.set(null);
    this.provenanceManquementSecurite.set(null);
    this.provenanceModificationContrat.set(null);
    this.provenanceDeclassement.set(null);
    this.provenanceDiscrimination.set(null);
    this.provenanceHeuresSupNonPayees.set(null);
    this.provenanceNonRespectRepos.set(null);
    this.provenanceManquementsPersistants.set(null);
    this.provenanceSalarieEnPoste.set(null);
    this.provenanceLicenciementEnCours.set(null);
  }

  // ---------------------------------------------------------------------------
  // F-IA-03 — builder d'alertes générique pour les 5 champs booléens audités
  // ---------------------------------------------------------------------------

  /**
   * Construit une alerte F-IA-03 pour un champ booléen donné, en croisant la
   * saisie courante avec la checklist procédurale F-96 et les questions IA.
   */
  private buildBooleanAlert(
    field: RJCAlertField,
    code: string,
    user: boolean,
    libelle: string,
  ): RJCCoherenceAlert | null {
    const builder = CoherenceAlertBuilder.forField<RJCAlertField>(field);
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

  alertTooltip(alert: RJCCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: RJCCoherenceAlert): string {
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
   *  - RESILIATION_FAVORABLE → vert (favorable)
   *  - RESILIATION_INCERTAINE → or (issue incertaine)
   *  - RESILIATION_DEFAVORABLE → navy/grisé (manquements insuffisants — NON
   *    rouge : le rejet ne rompt pas le contrat, voie sans risque)
   */
  verdictBannerClass(verdict: ResiliationJudiciaireCphVerdict): string {
    switch (verdict) {
      case 'RESILIATION_FAVORABLE':
        return 'rjc-verdict-banner rjc-verdict-banner--favorable';
      case 'RESILIATION_INCERTAINE':
        return 'rjc-verdict-banner rjc-verdict-banner--incertaine';
      case 'RESILIATION_DEFAVORABLE':
        return 'rjc-verdict-banner rjc-verdict-banner--defavorable';
    }
  }

  verdictBannerLabel(verdict: ResiliationJudiciaireCphVerdict): string {
    switch (verdict) {
      case 'RESILIATION_FAVORABLE': return 'Résiliation judiciaire favorable';
      case 'RESILIATION_INCERTAINE': return 'Résiliation judiciaire incertaine';
      case 'RESILIATION_DEFAVORABLE': return 'Résiliation judiciaire défavorable';
    }
  }

  verdictBannerIcon(verdict: ResiliationJudiciaireCphVerdict): string {
    switch (verdict) {
      case 'RESILIATION_FAVORABLE': return 'check_circle';
      case 'RESILIATION_INCERTAINE': return 'warning';
      case 'RESILIATION_DEFAVORABLE': return 'info';
    }
  }

  /** Date d'effet probable → libellé + classe de couleur. */
  dateEffetProbableLabel(effet: ResiliationJudiciaireCphDateEffetProbable): string {
    switch (effet) {
      case 'DATE_DECISION':
        return 'Effets licenciement sans cause à la date du jugement (Cass. soc. 11/01/2007 n°05-40.626)';
      case 'DATE_LICENCIEMENT':
        return 'Effets datés au licenciement intervenu en cours d\'instance (Cass. soc. 21/12/2006 n°05-42.251)';
    }
  }

  dateEffetProbableClass(effet: ResiliationJudiciaireCphDateEffetProbable): string {
    switch (effet) {
      case 'DATE_DECISION':
        return 'rjc-effet-pill rjc-effet-pill--date-decision';
      case 'DATE_LICENCIEMENT':
        return 'rjc-effet-pill rjc-effet-pill--date-licenciement';
    }
  }
}
