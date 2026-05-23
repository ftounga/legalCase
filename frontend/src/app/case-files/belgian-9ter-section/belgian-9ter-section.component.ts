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
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Belgian9terService } from '../../core/services/belgian-9ter.service';
import {
  Belgian9terResponse,
  Verdict9ter,
} from '../../core/models/belgian-9ter.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import {
  ImmigrationExtractedData,
  PieceManquanteEntry,
} from '../../core/models/case-analysis.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { AiQuestion } from '../../core/models/ai-question.model';
import { CoherencePopoverTriggerDirective } from '../../shared/coherence-popover/coherence-popover-trigger.directive';
import { CoherenceAlert, CoherenceAlertSource } from '../../shared/coherence-popover/coherence-alert.model';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';
import { CoherenceAlertBuilder } from '../../shared/coherence-popover/coherence-alert-builder';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { Belgian9terPrefillRules } from './belgian-9ter-section-prefill-rules';
import { SourceExplanation } from '../../core/models/source-explanation.model';
import { SourceExplanationService } from '../../core/services/source-explanation.service';

/**
 * SF-155-09 : champs d'alerte de cohérence F-IA-03 exposés par l'outil
 * F-IM-14 (régularisation 9ter médical BE). Un par field clé du form
 * (4 dates/booléens médicaux qui peuvent diverger entre saisie avocat
 * et sources IA / F-96 / question complémentaire / pièce manquante).
 */
export type Belgian9terAlertField =
  | 'DATE_DEBUT_SYMPTOMES'
  | 'MALADIE_GRAVE'
  | 'SOINS_BE'
  | 'SOINS_INACCESSIBLES'
  | 'MENACE_ORDRE_PUBLIC'
  | 'DATE_DEPOT_DEMANDE';

// SF-155-09 : alias locaux — interface générique partagée.
export type Belgian9terAlertSource = CoherenceAlertSource;
export type Belgian9terCoherenceAlert = CoherenceAlert<Belgian9terAlertField>;

/**
 * SF-IM-14-06 : outil décisionnel "Régularisation 9ter médical" (BE).
 * Consomme l'API `POST + GET /api/v1/case-files/{id}/belgian-9ter`
 * livrée par SF-IM-14-02 (PR #509). BELGIQUE uniquement — l'équivalent
 * français (étranger malade L.425-9 CESEDA) est juridiquement
 * distinct (invariant "1 outil = 1 situation").
 *
 * Affiché conditionnellement par le panel F-IA-04 (tool_id
 * `F-IM-14-9ter-medical-be`, ALWAYS_ON BE, priority 65). Le wiring
 * effectif TOOL_REGISTRY est livré dans une SF d'intégration
 * dédiée (cf. mini-spec).
 *
 * Pré-fill IA : `aiData?: Partial<ImmigrationExtractedData>`. Les
 * champs médicaux ne sont pas (encore) exposés par
 * `ImmigrationExtractedData` — handler gracieux qui ignore les champs
 * absents (no-op si `aiData` ne fournit rien d'exploitable).
 *
 * SF-155-09 : ajout validation F-IA-03 — `coherenceAlerts` computed
 * signal construit via `CoherenceAlertBuilder` + popover trigger
 * (`[appCoherencePopover]`) sur les 4 fields médicaux clés
 * (DATE_DEBUT_SYMPTOMES, MALADIE_GRAVE, SOINS_BE, SOINS_INACCESSIBLES).
 * Pattern emprunté au template canonique
 * `harcelement-licenciement-nul-section` (F-DT-11-02).
 */
@Component({
  selector: 'app-belgian-9ter-section',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSlideToggleModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    LegalCitationsPipe,
    CoherencePopoverTriggerDirective,
    ToolJurisprudenceCitationsComponent
  ],
  templateUrl: './belgian-9ter-section.component.html',
  styleUrl: './belgian-9ter-section.component.scss',
})
export class Belgian9terSectionComponent implements OnInit, OnChanges {
  // F-JU-03 SF-JU-03 — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-IM-14-9ter-medical-be';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'RÉGULARISATION 9TER MÉDICAL (BE)';
  static readonly TOOL_ICON = 'medical_services';

  /** F-236 SF-236-02 — Délègue intégralement au helper pur partagé. */
  static getPrefillCount(input: PrefillCountInput): number {
    return Belgian9terPrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'BELGIQUE';
  /**
   * Pré-fill IA. À ce stade, `ImmigrationExtractedData` n'expose pas
   * de champs médicaux ; le composant accepte un objet partiel et
   * ignore gracieusement les champs absents (no-op).
   */
  @Input() aiData?: Partial<ImmigrationExtractedData> | null;

  // SF-155-09 : inputs F-IA-03 (tous optionnels — null-safe partout).
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;
  // F-177 SF-177-03b : force l'expansion (mode modal F-177).
  @Input() forceExpanded = false;


  /**
   * F-163 SF-163-02d — Mode simulateur autonome (hors dossier client).
   *
   * Quand `true` :
   *  - Bannière « 🧪 Mode simulateur » affichée en haut.
   *  - `prefillFromAi()` court-circuité.
   *  - `coherenceAlerts` retourne `{}`.
   *  - `loadExisting()` / `load()` court-circuité.
   *  - `analyze()` POSTe sur `/api/v1/simulators/F-IM-14-9ter-medical-be/calculate`.
   *  - `triggerRefresh()` jamais invoqué.
   *
   * Default `false` — mode case-file scoped inchangé.
   */
  @Input() standaloneMode: boolean = false;
  // SF-155-09 : snapshots signal des inputs IA/F96/Q/Piece pour que
  // `coherenceAlerts` (computed) réagisse. Même pattern que
  // `harcelement-licenciement-nul-section`.
  private aiDataSignal = signal<Partial<ImmigrationExtractedData> | null | undefined>(undefined);
  private procedureChecksSignal = signal<ProcedureCheck[]>([]);
  private aiQuestionsSignal = signal<AiQuestion[]>([]);
  private piecesManquantesSignal = signal<PieceManquanteEntry[]>([]);

  collapsed = signal(true);
  loading = signal(false);
  analyzing = signal(false);
  showForm = signal(true);
  result = signal<Belgian9terResponse | null>(null);

  // Form signals.
  dateDebutSymptomes = signal<string | null>(null);
  maladieGraveCertifiee = signal<boolean>(false);
  soinsNecessairesDisponiblesBe = signal<boolean>(false);
  soinsInaccessiblesPaysOrigine = signal<boolean>(false);
  menaceOrdrePublic = signal<boolean>(false);
  dateDepotDemande = signal<string | null>(null);

  // SF-155-09 : true quand l'avocat a touché un toggle booléen au moins
  // une fois (onXxxChange appelé). Les toggles booléens démarrent à
  // `false` par défaut ce qui empêcherait de distinguer "avocat n'a
  // pas saisi" de "avocat dit non" pour déclencher les alertes.
  // Sans touch, l'alerte IA vs booléen false est silencieuse.
  maladieGraveTouched = signal<boolean>(false);
  soinsBeTouched = signal<boolean>(false);
  soinsInaccessiblesTouched = signal<boolean>(false);
  menaceTouched = signal<boolean>(false);

  // Provenance IA par champ — effacée au 1er onChange manuel.
  // Reste null tant que `aiData` ne fournit pas de champ médical
  // exploitable (ImmigrationExtractedData ne les expose pas encore).
  provenanceDateDebutSymptomes = signal<'IA' | null>(null);
  provenanceMaladieGrave = signal<'IA' | null>(null);
  provenanceSoinsBe = signal<'IA' | null>(null);
  provenanceSoinsInaccessibles = signal<'IA' | null>(null);
  provenanceMenace = signal<'IA' | null>(null);
  provenanceDateDepotDemande = signal<'IA' | null>(null);

  // SF-IA-03-15c : map {sourceKey → explanations} pour le popover.
  sourceExplanations = signal<Map<string, SourceExplanation[]>>(new Map());

  /** Aujourd'hui en YYYY-MM-DD pour l'attribut max des inputs date. */
  readonly todayIso: string = new Date().toISOString().slice(0, 10);

  isBelgium = computed<boolean>(() => this.workspaceCountry === 'BELGIQUE');

  /**
   * SF-155-09 : alertes de cohérence F-IA-03 calculées dynamiquement.
   * Gate : uniquement en mode formulaire (pattern anti-bug SF-IA-03-12 —
   * ne pas afficher d'alertes après que le résultat backend est rendu).
   * Gate pays : BELGIQUE uniquement (outil BE-only).
   */
  coherenceAlerts = computed<Partial<Record<Belgian9terAlertField, Belgian9terCoherenceAlert>>>(() => {
    // F-163 SF-163-02d : aucune source IA en standalone.
    if (this.standaloneMode) return {};
    if (!this.showForm()) return {};
    if (!this.isBelgium()) return {};
    const alerts: Partial<Record<Belgian9terAlertField, Belgian9terCoherenceAlert>> = {};
    const dateDebutAlert = this.buildDateDebutSymptomesAlert();
    if (dateDebutAlert) alerts.DATE_DEBUT_SYMPTOMES = dateDebutAlert;
    const maladieAlert = this.buildMaladieGraveAlert();
    if (maladieAlert) alerts.MALADIE_GRAVE = maladieAlert;
    const soinsBeAlert = this.buildSoinsBeAlert();
    if (soinsBeAlert) alerts.SOINS_BE = soinsBeAlert;
    const soinsInaccAlert = this.buildSoinsInaccessiblesAlert();
    if (soinsInaccAlert) alerts.SOINS_INACCESSIBLES = soinsInaccAlert;
    const menaceAlert = this.buildMenaceOrdrePublicAlert();
    if (menaceAlert) alerts.MENACE_ORDRE_PUBLIC = menaceAlert;
    const dateDepotAlert = this.buildDateDepotDemandeAlert();
    if (dateDepotAlert) alerts.DATE_DEPOT_DEMANDE = dateDepotAlert;
    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    return { total: values.length, blockers: 0 };
  });

  constructor(
    private service: Belgian9terService,
    private snackBar: MatSnackBar,
    @Optional() private dashboardRefresh: CaseDashboardRefreshService | null,
    @Optional() private sourceExplanationService: SourceExplanationService | null,
  ) {}

  ngOnInit(): void {
    // F-177 SF-177-03b : appliqué dès le mount pour le mode modal.
    if (this.forceExpanded) this.collapsed.set(false);
    // SF-155-09 : push inputs vers signals avant toute évaluation computed.
    this.aiDataSignal.set(this.aiData);
    this.procedureChecksSignal.set(this.procedureChecks ?? []);
    this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    this.piecesManquantesSignal.set(this.piecesManquantes ?? []);
    if (this.isBelgium()) {
      if (this.standaloneMode) {

        // F-163 SF-163-02d : pas de dossier à interroger en standalone.

        this.collapsed.set(false);

        this.loading.set(false);

        this.showForm.set(true);

        return;

      }
      this.load();
      this.loadSourceExplanations();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    // F-177 SF-177-03b : applique le forceExpanded quand il passe à true en cours de vie.
    if (changes['forceExpanded'] && this.forceExpanded) this.collapsed.set(false);
    // SF-155-09 : ré-hydrater les signals à chaque changement d'input.
    if (changes['aiData']) this.aiDataSignal.set(this.aiData);
    if (changes['procedureChecks']) this.procedureChecksSignal.set(this.procedureChecks ?? []);
    if (changes['aiQuestions']) this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    if (changes['piecesManquantes']) this.piecesManquantesSignal.set(this.piecesManquantes ?? []);

    // Re-prefill si aiData change après le ngOnInit, en mode formulaire,
    // sans résultat persisté (évite d'écraser saisie avocat ou résultat backend).
    if (changes['aiData'] && !changes['aiData'].firstChange
        && this.isBelgium() && this.showForm() && !this.result()) {
      this.prefillFromAi();
    }
  }

  toggleCollapse(): void {
    this.collapsed.update((v) => !v);
  }

  /**
   * Toutes les conditions d'entrée sont des booléens — le formulaire est
   * valide même si tous les toggles sont à false (le backend acceptera
   * un score 0 et renverra un verdict FAIBLE).
   * Seules les contraintes temporelles bloquent la soumission :
   * - dateDebutSymptomes ≤ today,
   * - dateDepotDemande ≤ today,
   * - dateDepotDemande ≥ dateDebutSymptomes (si les deux fournies).
   */
  formValid(): boolean {
    const symp = this.dateDebutSymptomes();
    const depot = this.dateDepotDemande();
    if (symp && symp > this.todayIso) return false;
    if (depot && depot > this.todayIso) return false;
    if (symp && depot && depot < symp) return false;
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  analyze(): void {
    if (!this.formValid()) return;
    const request = {
      dateDebutSymptomes: this.dateDebutSymptomes(),
      maladieGraveCertifiee: this.maladieGraveCertifiee(),
      soinsNecessairesDisponiblesBe: this.soinsNecessairesDisponiblesBe(),
      soinsInaccessiblesPaysOrigine: this.soinsInaccessiblesPaysOrigine(),
      menaceOrdrePublic: this.menaceOrdrePublic(),
      dateDepotDemande: this.dateDepotDemande(),
    };
    this.analyzing.set(true);
    // F-163 SF-163-02d : en standalone, POST sur le dispatcher générique.

    const request$ = this.standaloneMode

      ? this.service.analyzeStandalone(request)

      : this.service.analyze(this.caseFileId, request);

    request$.subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('Régularisation 9ter analysée', 'OK', { duration: 2500 });
        // F-163 SF-163-02d : pas de dashboard à rafraîchir en standalone.

        if (!this.standaloneMode) { this.dashboardRefresh?.triggerRefresh(); }
      },
      error: (err) => {
        this.analyzing.set(false);
        const msg = err?.error?.message || err?.error || 'Erreur lors de l\'analyse';
        this.snackBar.open(String(msg), 'Fermer', {
          duration: 5000,
          panelClass: 'snack-error',
        });
      },
    });
  }

  // Handlers onChange — effacent la provenance IA au 1er changement.
  onDateDebutSymptomesChange(value: string | null): void {
    this.dateDebutSymptomes.set(value || null);
    this.provenanceDateDebutSymptomes.set(null);
  }

  onMaladieGraveChange(value: boolean): void {
    this.maladieGraveCertifiee.set(value);
    this.provenanceMaladieGrave.set(null);
    this.maladieGraveTouched.set(true);
  }

  onSoinsBeChange(value: boolean): void {
    this.soinsNecessairesDisponiblesBe.set(value);
    this.provenanceSoinsBe.set(null);
    this.soinsBeTouched.set(true);
  }

  onSoinsInaccessiblesChange(value: boolean): void {
    this.soinsInaccessiblesPaysOrigine.set(value);
    this.provenanceSoinsInaccessibles.set(null);
    this.soinsInaccessiblesTouched.set(true);
  }

  onMenaceChange(value: boolean): void {
    this.menaceOrdrePublic.set(value);
    this.provenanceMenace.set(null);
    this.menaceTouched.set(true);
  }

  onDateDepotDemandeChange(value: string | null): void {
    this.dateDepotDemande.set(value || null);
    this.provenanceDateDepotDemande.set(null);
  }

  /**
   * Pré-fill IA. À ce stade, `ImmigrationExtractedData` n'expose pas
   * de champs médicaux. Les champs ci-dessous sont lus de manière
   * défensive — s'ils existent un jour dans le DTO, le composant les
   * récupèrera automatiquement.
   *
   * Règles (fail-open) :
   * - Passe silencieusement si `aiData` absent.
   * - Chaque champ est indépendant (prefill partiel OK).
   * - Validation de type : string non vide pour les dates, boolean
   *   strict pour les toggles.
   */
  private prefillFromAi(): void {
    // F-236 SF-236-02 : délègue au helper pur partagé.
    // F-236 SF-236-04 : passe workspaceCountry pour gating BE-only.
    const input: PrefillCountInput = {
      aiData: this.aiDataSignal(),
      workspaceCountry: this.workspaceCountry,
    };

    const dDeb = Belgian9terPrefillRules.computeDateDebutSymptomes(input);
    if (dDeb !== null) { this.dateDebutSymptomes.set(dDeb); this.provenanceDateDebutSymptomes.set('IA'); }

    const mg = Belgian9terPrefillRules.computeMaladieGraveCertifiee(input);
    if (mg !== null) { this.maladieGraveCertifiee.set(mg); this.provenanceMaladieGrave.set('IA'); }

    const sBe = Belgian9terPrefillRules.computeSoinsNecessairesDisponiblesBe(input);
    if (sBe !== null) { this.soinsNecessairesDisponiblesBe.set(sBe); this.provenanceSoinsBe.set('IA'); }

    const sIn = Belgian9terPrefillRules.computeSoinsInaccessiblesPaysOrigine(input);
    if (sIn !== null) { this.soinsInaccessiblesPaysOrigine.set(sIn); this.provenanceSoinsInaccessibles.set('IA'); }

    const mOP = Belgian9terPrefillRules.computeMenaceOrdrePublic(input);
    if (mOP !== null) { this.menaceOrdrePublic.set(mOP); this.provenanceMenace.set('IA'); }

    const dDep = Belgian9terPrefillRules.computeDateDepotDemande(input);
    if (dDep !== null) { this.dateDepotDemande.set(dDep); this.provenanceDateDepotDemande.set('IA'); }
  }

  private loadSourceExplanations(): void {
    if (!this.caseFileId || !this.sourceExplanationService) return;
    this.sourceExplanationService.getForCaseFile(this.caseFileId).subscribe({
      next: (map) => this.sourceExplanations.set(map),
      error: () => { /* fail-open */ },
    });
  }

  /**
   * SF-IA-03-15c : mapping field → sourceKey pour le popover. Les clés
   * suivent la convention (`critereCode` F-96 / QUESTION_IA / F-145 en
   * minuscules pour les dates, majuscules pour les flags métier).
   */
  explanationFor(field: Belgian9terAlertField): SourceExplanation[] {
    const key = (() => {
      switch (field) {
        case 'DATE_DEBUT_SYMPTOMES': return 'date_debut_symptomes';
        case 'MALADIE_GRAVE':        return 'BE_9TER_MALADIE_GRAVE';
        case 'SOINS_BE':             return 'BE_9TER_SOINS_BE';
        case 'SOINS_INACCESSIBLES':  return 'BE_9TER_SOINS_INACCESSIBLES';
        case 'MENACE_ORDRE_PUBLIC':  return 'BE_9TER_MENACE_ORDRE_PUBLIC';
        case 'DATE_DEPOT_DEMANDE':   return 'date_depot_demande';
      }
    })();
    return this.sourceExplanations().get(key) ?? [];
  }

  alertTooltip(alert: Belgian9terCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: Belgian9terCoherenceAlert): string {
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
   * SF-155-09 : divergence sur `dateDebutSymptomes` — si l'IA a extrait
   * une date différente de celle saisie par l'avocat. Comparaison
   * exacte (format ISO `YYYY-MM-DD` attendu des deux côtés).
   */
  private buildDateDebutSymptomesAlert(): Belgian9terCoherenceAlert | null {
    const userDate = this.dateDebutSymptomes();
    if (!userDate) return null;
    const builder = CoherenceAlertBuilder.forField<Belgian9terAlertField>('DATE_DEBUT_SYMPTOMES');

    // SF-246-20 : lecture du champ typé be9terDateDebutSymptomes (alias aspirationnel supprimé).
    const aiDate = this.aiDataSignal()?.be9terDateDebutSymptomes;
    if (typeof aiDate === 'string' && aiDate.length > 0 && aiDate !== userDate) {
      builder.addSource('IA', {
        expectedDisplay: aiDate,
        reason: `Analyse du dossier : début des symptômes le ${aiDate}`,
      });
    }
    const piece = this.findPieceManquante(['DATE_DEBUT_SYMPTOMES', 'BE_9TER_DATE_DEBUT_SYMPTOMES']);
    if (piece) builder.addPieceManquante(piece);
    return builder.build();
  }

  /**
   * SF-155-09 : divergence sur `maladieGraveCertifiee` — consolide
   * IA + F-96 + QUESTION_IA + PIECE_MANQUANTE.
   * Alertes émises uniquement après première interaction avocat
   * (`maladieGraveTouched`) pour éviter de signaler "divergence"
   * dès l'ouverture quand tous les toggles sont à false par défaut.
   */
  private buildMaladieGraveAlert(): Belgian9terCoherenceAlert | null {
    if (!this.maladieGraveTouched()) return null;
    const user = this.maladieGraveCertifiee();
    const builder = CoherenceAlertBuilder.forField<Belgian9terAlertField>('MALADIE_GRAVE');

    // 1. F-96 — critereCode `BE_9TER_MALADIE_GRAVE` ciblé.
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'BE_9TER_MALADIE_GRAVE') continue;
      const ev = chk.expectedValue?.toUpperCase();
      if (ev !== 'TRUE' && ev !== 'FALSE') continue;
      const expected = ev === 'TRUE';
      if (expected === user) continue;
      const display = expected ? 'Maladie grave certifiée' : 'Pas de maladie grave certifiée';
      builder.addSource('F96', {
        expectedDisplay: display,
        reason: `Checklist procédurale : ${display.toLowerCase()}${chk.raison ? ' (' + chk.raison + ')' : ''}`,
      });
      break;
    }

    // 2. QUESTION_IA — réponse "oui" avec `expectedValue` TRUE/FALSE.
    for (const q of this.aiQuestionsSignal()) {
      if (q.critereCode?.toUpperCase() !== 'BE_9TER_MALADIE_GRAVE') continue;
      const answer = q.answerText?.trim().toLowerCase();
      if (!answer) continue;
      const isOui = answer === 'oui' || answer.startsWith('oui ')
        || answer.startsWith('oui,') || answer.startsWith('oui.');
      if (!isOui) continue;
      const ev = q.expectedValue?.toUpperCase();
      if (ev !== 'TRUE' && ev !== 'FALSE') continue;
      const expected = ev === 'TRUE';
      if (expected === user) continue;
      const display = expected ? 'Maladie grave certifiée' : 'Pas de maladie grave certifiée';
      builder.addSource('QUESTION_IA', {
        expectedDisplay: display,
        reason: `Question complémentaire : "${q.questionText}" → "${q.answerText}"`,
      });
      break;
    }

    // 3. IA — `maladieGraveCertifiee` ou alias `maladieGrave`.
    const ai = this.aiDataSignal() as any;
    const aiVal: boolean | undefined = (typeof ai?.maladieGraveCertifiee === 'boolean')
      ? ai.maladieGraveCertifiee
      : (typeof ai?.maladieGrave === 'boolean' ? ai.maladieGrave : undefined);
    if (typeof aiVal === 'boolean' && aiVal !== user) {
      const display = aiVal ? 'Maladie grave certifiée' : 'Pas de maladie grave certifiée';
      builder.addSource('IA', {
        expectedDisplay: display,
        reason: `Analyse du dossier : ${display.toLowerCase()}`,
      });
    }

    // 4. PIECE_MANQUANTE (contributor additionnel — typ. certificat CM1/CM2).
    const piece = this.findPieceManquante(['BE_9TER_MALADIE_GRAVE', 'BE_9TER_CERTIFICAT_MEDICAL']);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  /**
   * SF-155-09 : divergence sur `soinsNecessairesDisponiblesBe` —
   * booléen indiquant que les soins sont disponibles en Belgique.
   */
  private buildSoinsBeAlert(): Belgian9terCoherenceAlert | null {
    if (!this.soinsBeTouched()) return null;
    const user = this.soinsNecessairesDisponiblesBe();
    const builder = CoherenceAlertBuilder.forField<Belgian9terAlertField>('SOINS_BE');

    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'BE_9TER_SOINS_BE') continue;
      const ev = chk.expectedValue?.toUpperCase();
      if (ev !== 'TRUE' && ev !== 'FALSE') continue;
      const expected = ev === 'TRUE';
      if (expected === user) continue;
      const display = expected ? 'Soins disponibles en Belgique' : 'Soins indisponibles en Belgique';
      builder.addSource('F96', {
        expectedDisplay: display,
        reason: `Checklist procédurale : ${display.toLowerCase()}${chk.raison ? ' (' + chk.raison + ')' : ''}`,
      });
      break;
    }

    for (const q of this.aiQuestionsSignal()) {
      if (q.critereCode?.toUpperCase() !== 'BE_9TER_SOINS_BE') continue;
      const answer = q.answerText?.trim().toLowerCase();
      if (!answer) continue;
      const isOui = answer === 'oui' || answer.startsWith('oui ')
        || answer.startsWith('oui,') || answer.startsWith('oui.');
      if (!isOui) continue;
      const ev = q.expectedValue?.toUpperCase();
      if (ev !== 'TRUE' && ev !== 'FALSE') continue;
      const expected = ev === 'TRUE';
      if (expected === user) continue;
      const display = expected ? 'Soins disponibles en Belgique' : 'Soins indisponibles en Belgique';
      builder.addSource('QUESTION_IA', {
        expectedDisplay: display,
        reason: `Question complémentaire : "${q.questionText}" → "${q.answerText}"`,
      });
      break;
    }

    const aiVal = (this.aiDataSignal() as any)?.soinsNecessairesDisponiblesBe;
    if (typeof aiVal === 'boolean' && aiVal !== user) {
      const display = aiVal ? 'Soins disponibles en Belgique' : 'Soins indisponibles en Belgique';
      builder.addSource('IA', {
        expectedDisplay: display,
        reason: `Analyse du dossier : ${display.toLowerCase()}`,
      });
    }

    const piece = this.findPieceManquante(['BE_9TER_SOINS_BE']);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  /**
   * SF-155-09 : divergence sur `soinsInaccessiblesPaysOrigine` —
   * booléen indiquant que les soins sont inaccessibles au pays d'origine.
   */
  private buildSoinsInaccessiblesAlert(): Belgian9terCoherenceAlert | null {
    if (!this.soinsInaccessiblesTouched()) return null;
    const user = this.soinsInaccessiblesPaysOrigine();
    const builder = CoherenceAlertBuilder.forField<Belgian9terAlertField>('SOINS_INACCESSIBLES');

    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'BE_9TER_SOINS_INACCESSIBLES') continue;
      const ev = chk.expectedValue?.toUpperCase();
      if (ev !== 'TRUE' && ev !== 'FALSE') continue;
      const expected = ev === 'TRUE';
      if (expected === user) continue;
      const display = expected
        ? 'Soins inaccessibles au pays d\'origine'
        : 'Soins accessibles au pays d\'origine';
      builder.addSource('F96', {
        expectedDisplay: display,
        reason: `Checklist procédurale : ${display.toLowerCase()}${chk.raison ? ' (' + chk.raison + ')' : ''}`,
      });
      break;
    }

    for (const q of this.aiQuestionsSignal()) {
      if (q.critereCode?.toUpperCase() !== 'BE_9TER_SOINS_INACCESSIBLES') continue;
      const answer = q.answerText?.trim().toLowerCase();
      if (!answer) continue;
      const isOui = answer === 'oui' || answer.startsWith('oui ')
        || answer.startsWith('oui,') || answer.startsWith('oui.');
      if (!isOui) continue;
      const ev = q.expectedValue?.toUpperCase();
      if (ev !== 'TRUE' && ev !== 'FALSE') continue;
      const expected = ev === 'TRUE';
      if (expected === user) continue;
      const display = expected
        ? 'Soins inaccessibles au pays d\'origine'
        : 'Soins accessibles au pays d\'origine';
      builder.addSource('QUESTION_IA', {
        expectedDisplay: display,
        reason: `Question complémentaire : "${q.questionText}" → "${q.answerText}"`,
      });
      break;
    }

    const aiVal = (this.aiDataSignal() as any)?.soinsInaccessiblesPaysOrigine;
    if (typeof aiVal === 'boolean' && aiVal !== user) {
      const display = aiVal
        ? 'Soins inaccessibles au pays d\'origine'
        : 'Soins accessibles au pays d\'origine';
      builder.addSource('IA', {
        expectedDisplay: display,
        reason: `Analyse du dossier : ${display.toLowerCase()}`,
      });
    }

    const piece = this.findPieceManquante(['BE_9TER_SOINS_INACCESSIBLES']);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  /**
   * SF-155-09 : divergence sur `menaceOrdrePublic`.
   * Le critère est sensible : si l'IA suspecte une menace et que l'avocat
   * coche "non", il faut alerter — et inversement.
   */
  private buildMenaceOrdrePublicAlert(): Belgian9terCoherenceAlert | null {
    if (!this.menaceTouched()) return null;
    const user = this.menaceOrdrePublic();
    const builder = CoherenceAlertBuilder.forField<Belgian9terAlertField>('MENACE_ORDRE_PUBLIC');

    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'BE_9TER_MENACE_ORDRE_PUBLIC') continue;
      const ev = chk.expectedValue?.toUpperCase();
      if (ev !== 'TRUE' && ev !== 'FALSE') continue;
      const expected = ev === 'TRUE';
      if (expected === user) continue;
      const display = expected ? 'Menace à l\'ordre public' : 'Pas de menace à l\'ordre public';
      builder.addSource('F96', {
        expectedDisplay: display,
        reason: `Checklist procédurale : ${display.toLowerCase()}${chk.raison ? ' (' + chk.raison + ')' : ''}`,
      });
      break;
    }

    for (const q of this.aiQuestionsSignal()) {
      if (q.critereCode?.toUpperCase() !== 'BE_9TER_MENACE_ORDRE_PUBLIC') continue;
      const answer = q.answerText?.trim().toLowerCase();
      if (!answer) continue;
      const isOui = answer === 'oui' || answer.startsWith('oui ')
        || answer.startsWith('oui,') || answer.startsWith('oui.');
      if (!isOui) continue;
      const ev = q.expectedValue?.toUpperCase();
      if (ev !== 'TRUE' && ev !== 'FALSE') continue;
      const expected = ev === 'TRUE';
      if (expected === user) continue;
      const display = expected ? 'Menace à l\'ordre public' : 'Pas de menace à l\'ordre public';
      builder.addSource('QUESTION_IA', {
        expectedDisplay: display,
        reason: `Question complémentaire : "${q.questionText}" → "${q.answerText}"`,
      });
      break;
    }

    const aiVal = (this.aiDataSignal() as any)?.menaceOrdrePublic;
    if (typeof aiVal === 'boolean' && aiVal !== user) {
      const display = aiVal ? 'Menace à l\'ordre public' : 'Pas de menace à l\'ordre public';
      builder.addSource('IA', {
        expectedDisplay: display,
        reason: `Analyse du dossier : ${display.toLowerCase()}`,
      });
    }

    return builder.build();
  }

  /**
   * SF-155-09 : divergence sur `dateDepotDemande` (typ. divergence
   * entre date saisie et extraction IA d'un accusé de dépôt).
   */
  private buildDateDepotDemandeAlert(): Belgian9terCoherenceAlert | null {
    const userDate = this.dateDepotDemande();
    if (!userDate) return null;
    const builder = CoherenceAlertBuilder.forField<Belgian9terAlertField>('DATE_DEPOT_DEMANDE');

    const aiDate = (this.aiDataSignal() as any)?.dateDepotDemande;
    if (typeof aiDate === 'string' && aiDate.length > 0 && aiDate !== userDate) {
      builder.addSource('IA', {
        expectedDisplay: aiDate,
        reason: `Analyse du dossier : dépôt de la demande le ${aiDate}`,
      });
    }
    const piece = this.findPieceManquante(['DATE_DEPOT_DEMANDE', 'BE_9TER_DATE_DEPOT_DEMANDE']);
    if (piece) builder.addPieceManquante(piece);
    return builder.build();
  }

  /**
   * SF-155-09 : renvoie le `texte` d'une `PieceManquanteEntry` dont
   * `critereCode` (case-insensitive) appartient à la liste des codes acceptés,
   * ou `null` si aucune pièce ne matche. La première pièce trouvée gagne.
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

  /**
   * Bannière du résultat selon le verdict :
   * - ELEVEE → succès (vert).
   * - MOYENNE → info (navy).
   * - FAIBLE → warning (or).
   * Rouge réservé à la `criteresNonRemplis` "menace ordre public".
   */
  bannerClass(verdict: Verdict9ter | null | undefined): string {
    switch (verdict) {
      case 'ELEVEE':  return 'belgian-9ter-banner belgian-9ter-banner--success';
      case 'MOYENNE': return 'belgian-9ter-banner belgian-9ter-banner--info';
      case 'FAIBLE':  return 'belgian-9ter-banner belgian-9ter-banner--warning';
      default:        return 'belgian-9ter-banner';
    }
  }

  bannerIcon(verdict: Verdict9ter | null | undefined): string {
    switch (verdict) {
      case 'ELEVEE':  return 'check_circle';
      case 'MOYENNE': return 'info_outline';
      case 'FAIBLE':  return 'warning';
      default:        return 'info_outline';
    }
  }

  verdictLabel(verdict: Verdict9ter | null | undefined): string {
    switch (verdict) {
      case 'ELEVEE':  return 'Probabilité élevée';
      case 'MOYENNE': return 'Probabilité moyenne';
      case 'FAIBLE':  return 'Probabilité faible';
      default:        return '';
    }
  }

  /** True quand le verdict est FAIBLE et que la menace ordre public est présente. */
  hasCriticalMenace(r: Belgian9terResponse | null): boolean {
    if (!r) return false;
    return !r.pasMenace;
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.dateDebutSymptomes.set(r.dateDebutSymptomes);
        this.maladieGraveCertifiee.set(r.maladieGraveCertifiee);
        this.soinsNecessairesDisponiblesBe.set(r.soinsNecessairesDisponiblesBe);
        this.soinsInaccessiblesPaysOrigine.set(r.soinsInaccessiblesPaysOrigine);
        this.menaceOrdrePublic.set(r.menaceOrdrePublic);
        this.dateDepotDemande.set(r.dateDepotDemande);
        this.showForm.set(false);
        this.loading.set(false);
      },
      error: () => {
        // 404 attendu si aucune analyse — on reste en mode formulaire et
        // on applique le pré-fill IA si aiData disponible.
        this.loading.set(false);
        this.prefillFromAi();
      },
    });
  }
}
