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
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { DesaccordsParentauxService } from '../../core/services/desaccords-parentaux.service';
import {
  DesaccordsParentauxRequest,
  DesaccordsParentauxResponse,
  DesaccordsParentauxVerdict,
  DOMAINES_DESACCORD_FR,
  DomaineDesaccord,
  INTENSITES_DESACCORD_FR,
  IntensiteDesaccord,
  TENTATIVES_MEDIATION_FR,
  TentativeMediation,
  domaineDesaccordLabel,
  intensiteDesaccordLabel,
  tentativeMediationLabel,
  verdictDesaccordsParentauxLabel,
} from '../../core/models/desaccords-parentaux.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { PieceManquanteEntry } from '../../core/models/case-analysis.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { AiQuestion } from '../../core/models/ai-question.model';
import { CoherencePopoverTriggerDirective } from '../../shared/coherence-popover/coherence-popover-trigger.directive';
import {
  CoherenceAlert,
  CoherenceAlertSource,
} from '../../shared/coherence-popover/coherence-alert.model';
import { CoherenceAlertBuilder } from '../../shared/coherence-popover/coherence-alert-builder';
import { DesaccordsParentauxPrefillRules } from './desaccords-parentaux-section-prefill-rules';
import { SourceExplanation } from '../../core/models/source-explanation.model';
import { SourceExplanationService } from '../../core/services/source-explanation.service';

/**
 * SF-FA-19-06 : fields F-IA-03 audités par l'outil "Désaccords parentaux"
 * (F-FA-19, art. 373-2-10 Cciv). Un par champ pré-remplissable depuis
 * l'analyse IA (`FamilleExtractedData`).
 */
export type DesaccordsParentauxAlertField =
  | 'DOMAINE_DESACCORD'
  | 'INTENSITE_DESACCORD'
  | 'TENTATIVES_MEDIATION'
  | 'AGE_ENFANTS_CONCERNES'
  | 'URGENCE';

export type DesaccordsParentauxAlertSource = CoherenceAlertSource;
export type DesaccordsParentauxCoherenceAlert =
    CoherenceAlert<DesaccordsParentauxAlertField>;

const ISO_DATE_REGEX = /^\d{4}-\d{2}-\d{2}$/;

const VALID_DOMAINES: ReadonlySet<string> = new Set<DomaineDesaccord>([
  'SCOLARITE',
  'SANTE',
  'RELIGION',
  'LOISIRS_SPORTS',
  'CHOIX_EDUCATIFS',
  'DEMENAGEMENT',
  'AUTRE',
]);

const VALID_INTENSITES: ReadonlySet<string> = new Set<IntensiteDesaccord>([
  'MAJEUR',
  'MOYEN',
  'MINEUR',
]);

const VALID_TENTATIVES: ReadonlySet<string> = new Set<TentativeMediation>([
  'MEDIATION_FAMILIALE',
  'MEDIATION_JUDICIAIRE',
  'DISCUSSIONS_DIRECTES',
  'THERAPIE_FAMILIALE',
  'AUCUNE',
]);

/**
 * SF-FA-19-06 : composant Angular standalone pour l'outil "Désaccords
 * parentaux art. 373-2-10" (F-FA-19, FRANCE uniquement).
 *
 * Pattern de référence : `harcelement-licenciement-nul-section` (template
 * canonique 2026-04-24, cf. `ai-skills/frontend-coherence-audit.md` §5).
 * Miroir famille : `autorite-parentale-section` (jumeau F-FA-19,
 * FamilleExtractedData + builder F-IA-03 + 5 fields + CSV âges).
 *
 * - Form : 1 mat-select DomaineDesaccord (7) + 1 mat-select IntensiteDesaccord
 *   (3) + 1 mat-select multiple TentativeMediation (5) + 3 slide-toggles +
 *   ages CSV + `<input type="date">` requête.
 * - Verdict 3 paliers ELEVEE/MOYENNE/FAIBLE → palette navy/or/rouge classique.
 * - Pré-fill IA via `aiData?: FamilleExtractedData | null` (5 fields ouverts,
 *   no-op gracieux si absent).
 * - Validation F-IA-03 sur 5 fields via `CoherenceAlertBuilder` (multi-sources
 *   IA + F96 + QUESTION_IA + PIECE_MANQUANTE) + popover.
 * - Refresh dashboard F-IA-02 + `MatSnackBar` pour erreurs.
 */
@Component({
  selector: 'app-desaccords-parentaux-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatSlideToggleModule, MatChipsModule,
    MatProgressSpinnerModule,
    LegalCitationsPipe,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './desaccords-parentaux-section.component.html',
  styleUrl: './desaccords-parentaux-section.component.scss',
})
export class DesaccordsParentauxSectionComponent implements OnInit, OnChanges {
  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'DÉSACCORDS PARENTAUX (FR) — ART. 373-2-10 CCIV';
  static readonly TOOL_ICON = 'forum';

  /** F-236 SF-236-02 — compteur miroir prefillFromAi via helper. */
  static getPrefillCount(input: {
    aiData?: FamilleExtractedData | null;
    procedureChecks?: unknown[];
    aiQuestions?: unknown[];
    piecesManquantes?: unknown[];
    triggerEvents?: unknown[];
    workspaceCountry?: string;
  }): number {
    return DesaccordsParentauxPrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';
  // Pré-fill IA + sources F-IA-03 (tous optionnels — null-safe partout).
  @Input() aiData?: FamilleExtractedData | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;

  // Snapshots signal pour réactivité des `computed` signals F-IA-03.
  private aiDataSignal = signal<FamilleExtractedData | null | undefined>(undefined);
  private procedureChecksSignal = signal<ProcedureCheck[]>([]);
  private aiQuestionsSignal = signal<AiQuestion[]>([]);
  private piecesManquantesSignal = signal<PieceManquanteEntry[]>([]);

  // F-177 SF-177-03b : force l'expansion (mode modal F-177).
  @Input() forceExpanded = false;


  /**
   * F-163 SF-163-02c — Mode simulateur autonome (hors dossier client).
   *
   * Quand `true` :
   *  - Bannière « 🧪 Mode simulateur » affichée en haut.
   *  - `prefillFromAi()` court-circuité.
   *  - `coherenceAlerts` retourne `{}`.
   *  - `loadExisting()` court-circuité.
   *  - `calculate()` POSTe sur `/api/v1/simulators/F-FA-19-desaccords-parentaux/calculate`.
   *  - `triggerRefresh()` jamais invoqué.
   *
   * Default `false` — mode case-file scoped inchangé.
   */
  @Input() standaloneMode: boolean = false;
  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<DesaccordsParentauxResponse | null>(null);

  // Form fields.
  domaineDesaccord = signal<DomaineDesaccord | null>(null);
  intensiteDesaccord = signal<IntensiteDesaccord | null>(null);
  tentativesMediation = signal<TentativeMediation[]>([]);
  /** Saisi via input texte CSV (parsé). */
  ageEnfantsRaw = signal<string>('');
  ageEnfantsConcernes = signal<number[]>([]);
  interetSuperieurInvoque = signal<boolean>(false);
  expertiseDejaRealisee = signal<boolean>(false);
  urgence = signal<boolean>(false);
  dateRequete = signal<string | null>(null);

  // Provenance IA par champ pré-remplissable (5 fields).
  provenanceDomaine = signal<'IA' | null>(null);
  provenanceIntensite = signal<'IA' | null>(null);
  provenanceTentatives = signal<'IA' | null>(null);
  provenanceAgeEnfants = signal<'IA' | null>(null);
  provenanceUrgence = signal<'IA' | null>(null);

  // Map {sourceKey → explanations} pour le popover F-IA-03-15c (fail-open).
  sourceExplanations = signal<Map<string, SourceExplanation[]>>(new Map());

  readonly domainesOptions = DOMAINES_DESACCORD_FR;
  readonly intensitesOptions = INTENSITES_DESACCORD_FR;
  readonly tentativesOptions = TENTATIVES_MEDIATION_FR;

  /**
   * Coherence alerts F-IA-03 — gate `showForm()` strict (pattern anti-bug
   * SF-IA-03-12 : pas de `|| result()`).
   */
  coherenceAlerts = computed<Partial<Record<DesaccordsParentauxAlertField, DesaccordsParentauxCoherenceAlert>>>(() => {
    // F-163 SF-163-02c : aucune source IA en standalone.
    if (this.standaloneMode) return {};
    if (!this.showForm()) return {};
    const alerts: Partial<Record<DesaccordsParentauxAlertField, DesaccordsParentauxCoherenceAlert>> = {};
    const domaineAlert = this.buildDomaineAlert();
    if (domaineAlert) alerts.DOMAINE_DESACCORD = domaineAlert;
    const intensiteAlert = this.buildIntensiteAlert();
    if (intensiteAlert) alerts.INTENSITE_DESACCORD = intensiteAlert;
    const tentativesAlert = this.buildTentativesAlert();
    if (tentativesAlert) alerts.TENTATIVES_MEDIATION = tentativesAlert;
    const ageAlert = this.buildAgeEnfantsAlert();
    if (ageAlert) alerts.AGE_ENFANTS_CONCERNES = ageAlert;
    const urgenceAlert = this.buildUrgenceAlert();
    if (urgenceAlert) alerts.URGENCE = urgenceAlert;
    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    return { total: values.length };
  });

  isFrance = computed(() => this.workspaceCountry === 'FRANCE');

  constructor(
    private service: DesaccordsParentauxService,
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

    if (this.standaloneMode) {
      // F-163 SF-163-02c : pas de dossier à interroger en standalone.
      this.collapsed.set(false);
      this.loading.set(false);
      this.showForm.set(true);
      return;
    }
    if (this.isFrance()) {
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

    // Re-prefill quand aiData arrive après mount, tant que l'avocat n'a pas
    // saisi manuellement et qu'aucun résultat n'est affiché.
    if (changes['aiData'] && !changes['aiData'].firstChange
        && this.showForm() && !this.result() && !this.standaloneMode) {
      this.prefillFromAi();
    }
  }

  private loadSourceExplanations(): void {
    if (!this.caseFileId || !this.sourceExplanationService) return;
    this.sourceExplanationService.getForCaseFile(this.caseFileId).subscribe({
      next: (map) => this.sourceExplanations.set(map),
      error: () => { /* fail-open */ },
    });
  }

  /** SF-IA-03-15c : mapping field → sourceKey pour le popover F-IA-03. */
  explanationFor(field: DesaccordsParentauxAlertField): SourceExplanation[] {
    const key = (() => {
      switch (field) {
        case 'DOMAINE_DESACCORD': return 'FA19_DOMAINE_DESACCORD';
        case 'INTENSITE_DESACCORD': return 'FA19_INTENSITE_DESACCORD';
        case 'TENTATIVES_MEDIATION': return 'FA19_TENTATIVES_MEDIATION';
        case 'AGE_ENFANTS_CONCERNES': return 'FA19_AGE_ENFANTS_CONCERNES';
        case 'URGENCE': return 'FA19_URGENCE';
      }
    })();
    return this.sourceExplanations().get(key) ?? [];
  }

  alertTooltip(alert: DesaccordsParentauxCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: DesaccordsParentauxCoherenceAlert): string {
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
  // Form helpers / handlers
  // ---------------------------------------------------------------------------

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  /**
   * Parse la chaîne CSV des âges enfants — accepte espaces / virgules /
   * point-virgules. Filtre les valeurs non numériques ou hors range [0, 30].
   */
  parseAgeEnfants(raw: string): number[] {
    if (!raw) return [];
    const tokens = raw.split(/[,;\s]+/).filter((t) => t.length > 0);
    const ages: number[] = [];
    for (const t of tokens) {
      const n = Number(t);
      if (Number.isInteger(n) && n >= 0 && n <= 30) {
        ages.push(n);
      }
    }
    return ages;
  }

  formValid(): boolean {
    const dom = this.domaineDesaccord();
    const int = this.intensiteDesaccord();
    const ten = this.tentativesMediation();
    const ages = this.ageEnfantsConcernes();
    const date = this.dateRequete();
    return dom !== null
      && int !== null
      && ten.length > 0
      && ages.length > 0
      && date !== null && ISO_DATE_REGEX.test(date);
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // Handlers manuels — effacent le badge IA correspondant.
  onDomaineChange(value: DomaineDesaccord | null): void {
    this.domaineDesaccord.set(value);
    this.provenanceDomaine.set(null);
  }

  onIntensiteChange(value: IntensiteDesaccord | null): void {
    this.intensiteDesaccord.set(value);
    this.provenanceIntensite.set(null);
  }

  onTentativesChange(value: TentativeMediation[]): void {
    this.tentativesMediation.set(value ?? []);
    this.provenanceTentatives.set(null);
  }

  onAgeEnfantsRawChange(value: string): void {
    this.ageEnfantsRaw.set(value ?? '');
    this.ageEnfantsConcernes.set(this.parseAgeEnfants(value ?? ''));
    this.provenanceAgeEnfants.set(null);
  }

  onInteretSuperieurChange(value: boolean): void {
    this.interetSuperieurInvoque.set(!!value);
  }

  onExpertiseDejaRealiseeChange(value: boolean): void {
    this.expertiseDejaRealisee.set(!!value);
  }

  onUrgenceChange(value: boolean): void {
    this.urgence.set(!!value);
    this.provenanceUrgence.set(null);
  }

  onDateRequeteChange(value: string | null): void {
    this.dateRequete.set(value && value.length > 0 ? value : null);
  }

  // ---------------------------------------------------------------------------
  // HTTP : load + calculate
  // ---------------------------------------------------------------------------

  calculate(): void {
    if (!this.formValid()) return;
    const request: DesaccordsParentauxRequest = {
      domaineDesaccord: this.domaineDesaccord()!,
      intensiteDesaccord: this.intensiteDesaccord()!,
      tentativesMediation: this.tentativesMediation(),
      ageEnfantsConcernes: this.ageEnfantsConcernes(),
      interetSuperieurInvoque: this.interetSuperieurInvoque(),
      expertiseDejaRealisee: this.expertiseDejaRealisee(),
      urgence: this.urgence(),
      dateRequete: this.dateRequete()!,
    };
    this.calculating.set(true);
    // F-163 SF-163-02c : en standalone, POST sur le dispatcher générique.

    const request$ = this.standaloneMode

      ? this.service.calculateStandalone(request)

      : this.service.calculate(this.caseFileId, request);

    request$.subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Analyse désaccords parentaux calculée', 'OK', { duration: 2500 });
        // F-163 SF-163-02c : pas de dashboard à rafraîchir en standalone.

        if (!this.standaloneMode) { this.dashboardRefresh?.triggerRefresh(); }
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
        this.applyPersistedResult(r);
        this.loading.set(false);
      },
      error: () => {
        // 404 attendu si aucune analyse — on reste en mode formulaire.
        this.prefillFromAi();
        this.loading.set(false);
      },
    });
  }

  private applyPersistedResult(r: DesaccordsParentauxResponse): void {
    this.result.set(r);
    this.domaineDesaccord.set(r.domaineDesaccord);
    this.intensiteDesaccord.set(r.intensiteDesaccord);
    this.tentativesMediation.set(r.tentativesMediation ?? []);
    this.ageEnfantsConcernes.set(r.ageEnfantsConcernes ?? []);
    this.ageEnfantsRaw.set((r.ageEnfantsConcernes ?? []).join(', '));
    this.interetSuperieurInvoque.set(r.interetSuperieurInvoque);
    this.expertiseDejaRealisee.set(r.expertiseDejaRealisee);
    this.urgence.set(r.urgence);
    this.dateRequete.set(r.dateRequete);
    // Valeurs persistées = saisie avocat — jamais de badge IA.
    this.provenanceDomaine.set(null);
    this.provenanceIntensite.set(null);
    this.provenanceTentatives.set(null);
    this.provenanceAgeEnfants.set(null);
    this.provenanceUrgence.set(null);
    this.showForm.set(false);
  }

  /**
   * Pré-fill 5 champs IA :
   *  - domaineDesaccord (depuis `domaineDesaccordDetecte`)
   *  - intensiteDesaccord (depuis `intensiteDesaccordDetecte`)
   *  - tentativesMediation (depuis `tentativesMediationDetectees`)
   *  - ageEnfantsConcernes (depuis `ageEnfants` — réutilisé)
   *  - urgence (depuis `urgenceDetectee`)
   *
   * No-op gracieux si champ absent.
   */
  private prefillFromAi(): void {
    // F-236 SF-236-02 — délégation au helper partagé.
    const h = { aiData: this.aiDataSignal() };

    const dom = DesaccordsParentauxPrefillRules.computeDomaine(h);
    if (dom !== null
        && (this.domaineDesaccord() === null || this.provenanceDomaine() === 'IA')) {
      this.domaineDesaccord.set(dom);
      this.provenanceDomaine.set('IA');
    }

    const inten = DesaccordsParentauxPrefillRules.computeIntensite(h);
    if (inten !== null
        && (this.intensiteDesaccord() === null || this.provenanceIntensite() === 'IA')) {
      this.intensiteDesaccord.set(inten);
      this.provenanceIntensite.set('IA');
    }

    const tents = DesaccordsParentauxPrefillRules.computeTentatives(h);
    if (tents.length > 0
        && (this.tentativesMediation().length === 0 || this.provenanceTentatives() === 'IA')) {
      this.tentativesMediation.set(tents);
      this.provenanceTentatives.set('IA');
    }

    const ages = DesaccordsParentauxPrefillRules.computeAgeEnfants(h);
    if (ages.length > 0
        && (this.ageEnfantsConcernes().length === 0 || this.provenanceAgeEnfants() === 'IA')) {
      this.ageEnfantsConcernes.set(ages);
      this.ageEnfantsRaw.set(ages.join(', '));
      this.provenanceAgeEnfants.set('IA');
    }

    const urg = DesaccordsParentauxPrefillRules.computeUrgence(h);
    if (urg !== null
        && (this.provenanceUrgence() === 'IA' || this.urgence() === false)) {
      this.urgence.set(urg);
      this.provenanceUrgence.set('IA');
    }
  }

  // ---------------------------------------------------------------------------
  // Builders d'alertes F-IA-03 (un par field) — via CoherenceAlertBuilder.
  // ---------------------------------------------------------------------------

  /**
   * Domaine désaccord — divergence stricte sur l'enum 7 valeurs.
   * Sources : IA + F96 + QUESTION_IA + PIECE_MANQUANTE.
   */
  private buildDomaineAlert(): DesaccordsParentauxCoherenceAlert | null {
    const ai = this.aiDataSignal();
    const aiDomRaw = ai?.domaineDesaccordDetecte;
    if (typeof aiDomRaw !== 'string' || aiDomRaw.length === 0) return null;
    const aiDom = aiDomRaw.toUpperCase();
    if (!VALID_DOMAINES.has(aiDom)) return null;
    const userDom = this.domaineDesaccord();
    if (userDom === null) return null;
    if (userDom === aiDom) return null;

    const display = domaineDesaccordLabel(aiDom as DomaineDesaccord);
    const builder = CoherenceAlertBuilder.forField<DesaccordsParentauxAlertField>('DOMAINE_DESACCORD')
      .addSource('IA', {
        expectedDisplay: display,
        reason: `Analyse du dossier : domaine "${display}"`,
      });

    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'FA19_DOMAINE_DESACCORD') continue;
      if (!chk.expectedValue) continue;
      builder.addSource('F96', {
        expectedDisplay: display,
        reason: `Checklist procédurale : domaine attendu "${chk.expectedValue}"${chk.raison ? ' (' + chk.raison + ')' : ''}`,
      });
      break;
    }

    const piece = this.findPieceManquante(['FA19_DOMAINE_DESACCORD', 'COURRIERS_PARENTAUX']);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  /**
   * Intensité désaccord — divergence stricte sur l'enum 3 valeurs.
   */
  private buildIntensiteAlert(): DesaccordsParentauxCoherenceAlert | null {
    const ai = this.aiDataSignal();
    const aiIntRaw = ai?.intensiteDesaccordDetecte;
    if (typeof aiIntRaw !== 'string' || aiIntRaw.length === 0) return null;
    const aiInt = aiIntRaw.toUpperCase();
    if (!VALID_INTENSITES.has(aiInt)) return null;
    const userInt = this.intensiteDesaccord();
    if (userInt === null) return null;
    if (userInt === aiInt) return null;

    const display = intensiteDesaccordLabel(aiInt as IntensiteDesaccord);
    const builder = CoherenceAlertBuilder.forField<DesaccordsParentauxAlertField>('INTENSITE_DESACCORD')
      .addSource('IA', {
        expectedDisplay: display,
        reason: `Analyse du dossier : intensité "${display}"`,
      });

    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'FA19_INTENSITE_DESACCORD') continue;
      if (!chk.expectedValue) continue;
      builder.addSource('F96', {
        expectedDisplay: display,
        reason: `Checklist procédurale : intensité attendue "${chk.expectedValue}"${chk.raison ? ' (' + chk.raison + ')' : ''}`,
      });
      break;
    }

    const piece = this.findPieceManquante(['FA19_INTENSITE_DESACCORD']);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  /**
   * Tentatives médiation — divergence ensembliste IA vs user (au moins une
   * valeur différente OU nombre différent).
   */
  private buildTentativesAlert(): DesaccordsParentauxCoherenceAlert | null {
    const ai = this.aiDataSignal();
    const aiTent = ai?.tentativesMediationDetectees;
    if (!Array.isArray(aiTent) || aiTent.length === 0) return null;
    const aiSet = aiTent
      .filter((t): t is string => typeof t === 'string' && t.length > 0)
      .map((t) => t.toUpperCase())
      .filter((t) => VALID_TENTATIVES.has(t));
    if (aiSet.length === 0) return null;
    const userSet = this.tentativesMediation();
    if (userSet.length === 0) return null;

    const aiSorted = [...aiSet].sort();
    const userSorted = [...userSet].sort();
    const same = aiSorted.length === userSorted.length
      && aiSorted.every((v, i) => v === userSorted[i]);
    if (same) return null;

    const display = aiSet.map((t) => tentativeMediationLabel(t as TentativeMediation)).join(', ');
    const builder = CoherenceAlertBuilder.forField<DesaccordsParentauxAlertField>('TENTATIVES_MEDIATION')
      .addSource('IA', {
        expectedDisplay: display,
        reason: `Analyse du dossier : tentatives "${display}"`,
      });

    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'FA19_TENTATIVES_MEDIATION') continue;
      if (!chk.expectedValue) continue;
      builder.addSource('F96', {
        expectedDisplay: display,
        reason: `Checklist procédurale : tentatives attendues "${chk.expectedValue}"${chk.raison ? ' (' + chk.raison + ')' : ''}`,
      });
      break;
    }

    const piece = this.findPieceManquante([
      'FA19_TENTATIVES_MEDIATION',
      'ATTESTATION_MEDIATEUR',
      'PV_MEDIATION',
    ]);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  /**
   * Âges enfants concernés — divergence ensembliste IA vs user.
   */
  private buildAgeEnfantsAlert(): DesaccordsParentauxCoherenceAlert | null {
    const ai = this.aiDataSignal();
    const aiAges = ai?.ageEnfants;
    if (!Array.isArray(aiAges) || aiAges.length === 0) return null;
    const userAges = this.ageEnfantsConcernes();
    if (userAges.length === 0) return null;

    const aiSorted = [...aiAges].filter((n): n is number => typeof n === 'number').sort((a, b) => a - b);
    const userSorted = [...userAges].sort((a, b) => a - b);

    const same = aiSorted.length === userSorted.length
      && aiSorted.every((v, i) => v === userSorted[i]);
    if (same) return null;

    const display = aiSorted.join(', ') + ' an(s)';
    const builder = CoherenceAlertBuilder.forField<DesaccordsParentauxAlertField>('AGE_ENFANTS_CONCERNES')
      .addSource('IA', {
        expectedDisplay: display,
        reason: `Analyse du dossier : âges des enfants ${display}`,
      });

    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'FA19_AGE_ENFANTS_CONCERNES') continue;
      if (!chk.expectedValue) continue;
      builder.addSource('F96', {
        expectedDisplay: display,
        reason: `Checklist procédurale : âges attendus ${chk.expectedValue}${chk.raison ? ' (' + chk.raison + ')' : ''}`,
      });
      break;
    }

    const piece = this.findPieceManquante([
      'FA19_AGE_ENFANTS_CONCERNES',
      'ACTE_NAISSANCE_ENFANT',
      'LIVRET_FAMILLE',
    ]);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  /**
   * Urgence — alerte si IA détecte une urgence et l'avocat n'a pas coché.
   * Sens critique : sous-évaluation de l'urgence par l'avocat
   * (impact direct sur le délai de traitement 30j vs 90j).
   */
  private buildUrgenceAlert(): DesaccordsParentauxCoherenceAlert | null {
    const ai = this.aiDataSignal();
    if (typeof ai?.urgenceDetectee !== 'boolean') return null;
    if (ai.urgenceDetectee === false) return null; // pas d'alerte si IA neutre
    if (this.urgence() === true) return null; // pas de divergence

    const display = 'Urgence détectée';
    const builder = CoherenceAlertBuilder.forField<DesaccordsParentauxAlertField>('URGENCE')
      .addSource('IA', {
        expectedDisplay: display,
        reason: 'Analyse du dossier : urgence dans le désaccord parental détectée',
      });

    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'FA19_URGENCE') continue;
      if (!chk.expectedValue) continue;
      builder.addSource('F96', {
        expectedDisplay: display,
        reason: `Checklist procédurale : ${chk.expectedValue}${chk.raison ? ' (' + chk.raison + ')' : ''}`,
      });
      break;
    }

    const piece = this.findPieceManquante(['FA19_URGENCE', 'CERTIFICAT_MEDICAL_URGENCE']);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  /** Retourne le `texte` d'une `PieceManquanteEntry` matchant un code. */
  private findPieceManquante(acceptedCodes: string[]): string | null {
    const norm = new Set(acceptedCodes.map((c) => c.toUpperCase()));
    for (const p of this.piecesManquantesSignal()) {
      const code = p.critereCode?.toUpperCase();
      if (!code) continue;
      if (norm.has(code)) return p.texte;
    }
    return null;
  }

  // ---------------------------------------------------------------------------
  // Display helpers
  // ---------------------------------------------------------------------------

  domaineDesaccordLabel = domaineDesaccordLabel;
  intensiteDesaccordLabel = intensiteDesaccordLabel;
  tentativeMediationLabel = tentativeMediationLabel;
  verdictLabel = verdictDesaccordsParentauxLabel;

  verdictBannerClass(verdict: DesaccordsParentauxVerdict): string {
    switch (verdict) {
      case 'ELEVEE': return 'dp-verdict-banner dp-verdict-banner--strong';
      case 'MOYENNE': return 'dp-verdict-banner dp-verdict-banner--medium';
      case 'FAIBLE': return 'dp-verdict-banner dp-verdict-banner--weak';
    }
  }
}
