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
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ChangementEtatCivilService } from '../../core/services/changement-etat-civil.service';
import {
  ChangementEtatCivilRequest,
  ChangementEtatCivilResponse,
  CompetenceProcedure,
  MOTIFS_INVOQUES_FR,
  MotifInvoque,
  PREUVES_ETAT_CIVIL_FR,
  PreuveEtatCivil,
  TYPES_CHANGEMENT_FR,
  TypeChangement,
  VerdictEtatCivil,
  competenceProcedureLabel,
  motifInvoqueLabel,
  preuveEtatCivilLabel,
  typeChangementLabel,
  verdictEtatCivilLabel,
} from '../../core/models/changement-etat-civil.model';
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
import { SourceExplanation } from '../../core/models/source-explanation.model';
import { SourceExplanationService } from '../../core/services/source-explanation.service';

/**
 * SF-FA-26-02 : champs F-IA-03 audités par l'outil F-FA-26 (changement
 * d'état civil). Un par champ pré-remplissable depuis l'IA.
 */
export type ChangementEtatCivilAlertField =
  | 'TYPE_CHANGEMENT'
  | 'MOTIF_INVOQUE'
  | 'DATE_NAISSANCE'
  | 'MAJEUR_DEMANDEUR'
  | 'CONSENTEMENT_PARENTAL';

export type ChangementEtatCivilAlertSource = CoherenceAlertSource;
export type ChangementEtatCivilCoherenceAlert =
  CoherenceAlert<ChangementEtatCivilAlertField>;

const ISO_DATE_REGEX = /^\d{4}-\d{2}-\d{2}$/;

const VALID_TYPES: ReadonlySet<string> = new Set<TypeChangement>([
  'NOM',
  'PRENOM',
  'SEXE',
  'NOM_ET_PRENOM',
]);

const VALID_MOTIFS: ReadonlySet<string> = new Set<MotifInvoque>([
  'INTERET_LEGITIME',
  'MARIAGE',
  'RECTIFICATION_ERREUR',
  'IDENTIFICATION_GENRE',
  'AUTRE',
]);

/**
 * SF-FA-26-02 : composant Angular standalone pour l'outil "Changement d'état
 * civil" (F-FA-26, FRANCE uniquement, art. 60 / 61-1 et s. / 61-5 et s. Cciv ;
 * loi 2016-1547 / loi 2022-301).
 *
 * Pattern de référence : `harcelement-licenciement-nul-section` (template
 * canonique 2026-04-24, cf. `ai-skills/frontend-coherence-audit.md` §5).
 * Miroirs famille : `divorce-faute-section` (multi-select preuves),
 * `majeurs-proteges-section` (carte recommandation = compétence procédure),
 * `changement-residence-section` (palette navy/or/rouge classique).
 *
 * - Form : mat-select TypeChangement (4) + MotifInvoque (5) + multi-select
 *   PreuveEtatCivil (7), 4 slide-toggle (majeurDemandeur, consentementParental,
 *   datesDocsConcordants, dejaChangeAuparavant), `<input type="date">`
 *   dateNaissanceDemandeur, input texte departementDeclaration.
 * - Verdict 3 paliers ELEVEE/MOYENNE/FAIBLE → palette navy/or/rouge classique.
 * - Carte "Compétence procédure" (MAIRIE / JUGE / TRIBUNAL_JUDICIAIRE).
 * - Pré-fill IA via `aiData?: FamilleExtractedData | null` (5 champs ouverts,
 *   no-op gracieux si absent).
 * - Validation F-IA-03 sur 5 fields via `CoherenceAlertBuilder` (multi-sources
 *   IA + F96 + QUESTION_IA + PIECE_MANQUANTE) + popover.
 * - Refresh dashboard F-IA-02 + `MatSnackBar` pour erreurs.
 */
@Component({
  selector: 'app-changement-etat-civil-section',
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
  templateUrl: './changement-etat-civil-section.component.html',
  styleUrl: './changement-etat-civil-section.component.scss',
})
export class ChangementEtatCivilSectionComponent implements OnInit, OnChanges {
  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'CHANGEMENT D\'ÉTAT CIVIL (FR) — ART. 60 / 61-1 / 61-5 CCIV';
  static readonly TOOL_ICON = 'badge';

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

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<ChangementEtatCivilResponse | null>(null);

  // Form fields.
  typeChangement = signal<TypeChangement | null>(null);
  motifInvoque = signal<MotifInvoque | null>(null);
  preuvesProduites = signal<PreuveEtatCivil[]>([]);
  majeurDemandeur = signal<boolean>(true);
  consentementParental = signal<boolean>(false);
  datesDocsConcordants = signal<boolean>(false);
  dejaChangeAuparavant = signal<boolean>(false);
  dateNaissanceDemandeur = signal<string | null>(null);
  departementDeclaration = signal<string>('');

  // Provenance IA par champ pré-remplissable (5 fields).
  provenanceTypeChangement = signal<'IA' | null>(null);
  provenanceMotifInvoque = signal<'IA' | null>(null);
  provenanceDateNaissance = signal<'IA' | null>(null);
  provenanceMajeurDemandeur = signal<'IA' | null>(null);
  provenanceConsentementParental = signal<'IA' | null>(null);

  // Map {sourceKey → explanations} pour le popover F-IA-03-15c (fail-open).
  sourceExplanations = signal<Map<string, SourceExplanation[]>>(new Map());

  readonly typesOptions = TYPES_CHANGEMENT_FR;
  readonly motifsOptions = MOTIFS_INVOQUES_FR;
  readonly preuvesOptions = PREUVES_ETAT_CIVIL_FR;

  /**
   * Coherence alerts F-IA-03 — gate `showForm()` strict (pattern anti-bug
   * SF-IA-03-12 : pas de `|| result()`).
   */
  coherenceAlerts = computed<Partial<Record<ChangementEtatCivilAlertField, ChangementEtatCivilCoherenceAlert>>>(() => {
    if (!this.showForm()) return {};
    const alerts: Partial<Record<ChangementEtatCivilAlertField, ChangementEtatCivilCoherenceAlert>> = {};
    const t = this.buildTypeChangementAlert();
    if (t) alerts.TYPE_CHANGEMENT = t;
    const m = this.buildMotifInvoqueAlert();
    if (m) alerts.MOTIF_INVOQUE = m;
    const d = this.buildDateNaissanceAlert();
    if (d) alerts.DATE_NAISSANCE = d;
    const maj = this.buildMajeurAlert();
    if (maj) alerts.MAJEUR_DEMANDEUR = maj;
    const cp = this.buildConsentementParentalAlert();
    if (cp) alerts.CONSENTEMENT_PARENTAL = cp;
    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    return { total: values.length, blockers: 0 };
  });

  isFrance = computed(() => this.workspaceCountry === 'FRANCE');

  constructor(
    private service: ChangementEtatCivilService,
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
        && this.showForm() && !this.result()) {
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
  explanationFor(field: ChangementEtatCivilAlertField): SourceExplanation[] {
    const key = (() => {
      switch (field) {
        case 'TYPE_CHANGEMENT': return 'FA26_TYPE_CHANGEMENT';
        case 'MOTIF_INVOQUE': return 'FA26_MOTIF_INVOQUE';
        case 'DATE_NAISSANCE': return 'FA26_DATE_NAISSANCE';
        case 'MAJEUR_DEMANDEUR': return 'FA26_MAJEUR_DEMANDEUR';
        case 'CONSENTEMENT_PARENTAL': return 'FA26_CONSENTEMENT_PARENTAL';
      }
    })();
    return this.sourceExplanations().get(key) ?? [];
  }

  alertTooltip(alert: ChangementEtatCivilCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: ChangementEtatCivilCoherenceAlert): string {
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

  formValid(): boolean {
    const t = this.typeChangement();
    const m = this.motifInvoque();
    const preuves = this.preuvesProduites();
    const date = this.dateNaissanceDemandeur();
    const dept = this.departementDeclaration();
    return t !== null
      && m !== null
      && preuves.length > 0
      && date !== null && ISO_DATE_REGEX.test(date)
      && dept.trim().length > 0;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // Handlers manuels — effacent le badge IA correspondant.
  onTypeChangementChange(value: TypeChangement | null): void {
    this.typeChangement.set(value);
    this.provenanceTypeChangement.set(null);
  }

  onMotifInvoqueChange(value: MotifInvoque | null): void {
    this.motifInvoque.set(value);
    this.provenanceMotifInvoque.set(null);
  }

  onPreuvesChange(value: PreuveEtatCivil[]): void {
    this.preuvesProduites.set(value ?? []);
  }

  onMajeurDemandeurChange(value: boolean): void {
    this.majeurDemandeur.set(!!value);
    this.provenanceMajeurDemandeur.set(null);
  }

  onConsentementParentalChange(value: boolean): void {
    this.consentementParental.set(!!value);
    this.provenanceConsentementParental.set(null);
  }

  onDatesDocsChange(value: boolean): void {
    this.datesDocsConcordants.set(!!value);
  }

  onDejaChangeChange(value: boolean): void {
    this.dejaChangeAuparavant.set(!!value);
  }

  onDateNaissanceChange(value: string | null): void {
    this.dateNaissanceDemandeur.set(value && value.length > 0 ? value : null);
    this.provenanceDateNaissance.set(null);
  }

  onDepartementChange(value: string): void {
    this.departementDeclaration.set(value ?? '');
  }

  // ---------------------------------------------------------------------------
  // HTTP : load + calculate
  // ---------------------------------------------------------------------------

  calculate(): void {
    if (!this.formValid()) return;
    const request: ChangementEtatCivilRequest = {
      typeChangement: this.typeChangement()!,
      motifInvoque: this.motifInvoque()!,
      preuvesProduites: this.preuvesProduites(),
      majeurDemandeur: this.majeurDemandeur(),
      consentementParental: this.consentementParental(),
      datesDocsConcordants: this.datesDocsConcordants(),
      dejaChangeAuparavant: this.dejaChangeAuparavant(),
      dateNaissanceDemandeur: this.dateNaissanceDemandeur()!,
      departementDeclaration: this.departementDeclaration().trim(),
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Analyse changement état civil calculée', 'OK', { duration: 2500 });
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

  private applyPersistedResult(r: ChangementEtatCivilResponse): void {
    this.result.set(r);
    this.typeChangement.set(r.typeChangement);
    this.motifInvoque.set(r.motifInvoque);
    this.preuvesProduites.set(r.preuvesProduites ?? []);
    this.majeurDemandeur.set(r.majeurDemandeur);
    this.consentementParental.set(r.consentementParental);
    this.datesDocsConcordants.set(r.datesDocsConcordants);
    this.dejaChangeAuparavant.set(r.dejaChangeAuparavant);
    this.dateNaissanceDemandeur.set(r.dateNaissanceDemandeur);
    this.departementDeclaration.set(r.departementDeclaration ?? '');
    // Valeurs persistées = saisie avocat — jamais de badge IA.
    this.provenanceTypeChangement.set(null);
    this.provenanceMotifInvoque.set(null);
    this.provenanceDateNaissance.set(null);
    this.provenanceMajeurDemandeur.set(null);
    this.provenanceConsentementParental.set(null);
    this.showForm.set(false);
  }

  /**
   * Pré-fill 5 champs IA :
   *  - typeChangement (depuis `typeChangementDetecte`)
   *  - motifInvoque (depuis `motifChangementDetecte`)
   *  - dateNaissanceDemandeur (depuis `dateNaissanceDemandeurDetectee`)
   *  - majeurDemandeur (depuis `majeurDemandeurDetected`)
   *  - consentementParental (depuis `consentementParentalDetected`)
   *
   * No-op gracieux si champ absent. N'écrase pas une saisie avocat (heuristique :
   * provenance === null sur un champ rempli ou booléen différent de la valeur
   * par défaut indique modification manuelle).
   */
  private prefillFromAi(): void {
    const ai = this.aiDataSignal();
    if (!ai) return;

    // 1. Type changement.
    const typeAi = ai.typeChangementDetecte;
    if (typeof typeAi === 'string' && typeAi.length > 0) {
      const upper = typeAi.toUpperCase();
      if (VALID_TYPES.has(upper)) {
        if (this.typeChangement() === null || this.provenanceTypeChangement() === 'IA') {
          this.typeChangement.set(upper as TypeChangement);
          this.provenanceTypeChangement.set('IA');
        }
      }
    }

    // 2. Motif invoqué.
    const motifAi = ai.motifChangementDetecte;
    if (typeof motifAi === 'string' && motifAi.length > 0) {
      const upper = motifAi.toUpperCase();
      if (VALID_MOTIFS.has(upper)) {
        if (this.motifInvoque() === null || this.provenanceMotifInvoque() === 'IA') {
          this.motifInvoque.set(upper as MotifInvoque);
          this.provenanceMotifInvoque.set('IA');
        }
      }
    }

    // 3. Date naissance demandeur.
    const dateAi = ai.dateNaissanceDemandeurDetectee;
    if (typeof dateAi === 'string' && ISO_DATE_REGEX.test(dateAi)) {
      if (this.dateNaissanceDemandeur() === null || this.provenanceDateNaissance() === 'IA') {
        this.dateNaissanceDemandeur.set(dateAi);
        this.provenanceDateNaissance.set('IA');
      }
    }

    // 4. Majeur demandeur (boolean).
    if (typeof ai.majeurDemandeurDetected === 'boolean') {
      if (this.provenanceMajeurDemandeur() === 'IA' || this.majeurDemandeur() === true) {
        // Heuristique : la valeur par défaut est `true`. Si l'IA dit `false`
        // (mineur), on prend l'IA. Si l'avocat a explicitement coché `false`,
        // provenance=null le protège déjà via la condition initiale.
        this.majeurDemandeur.set(ai.majeurDemandeurDetected);
        this.provenanceMajeurDemandeur.set('IA');
      }
    }

    // 5. Consentement parental (boolean).
    if (typeof ai.consentementParentalDetected === 'boolean') {
      if (this.provenanceConsentementParental() === 'IA' || this.consentementParental() === false) {
        this.consentementParental.set(ai.consentementParentalDetected);
        this.provenanceConsentementParental.set('IA');
      }
    }
  }

  // ---------------------------------------------------------------------------
  // Builders d'alertes F-IA-03 (un par field) — via CoherenceAlertBuilder.
  // ---------------------------------------------------------------------------

  /**
   * Type changement — divergence stricte sur l'enum 4 valeurs.
   * Sources : IA + F96 + QUESTION_IA + PIECE_MANQUANTE.
   */
  private buildTypeChangementAlert(): ChangementEtatCivilCoherenceAlert | null {
    const ai = this.aiDataSignal();
    const aiRaw = ai?.typeChangementDetecte;
    if (typeof aiRaw !== 'string' || aiRaw.length === 0) return null;
    const aiVal = aiRaw.toUpperCase();
    if (!VALID_TYPES.has(aiVal)) return null;
    const userVal = this.typeChangement();
    if (userVal === null) return null;
    if (userVal === aiVal) return null;

    const display = typeChangementLabel(aiVal as TypeChangement);
    const builder = CoherenceAlertBuilder.forField<ChangementEtatCivilAlertField>('TYPE_CHANGEMENT')
      .addSource('IA', {
        expectedDisplay: display,
        reason: `Analyse du dossier : type de changement "${display}"`,
      });

    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'FA26_TYPE_CHANGEMENT') continue;
      if (!chk.expectedValue) continue;
      builder.addSource('F96', {
        expectedDisplay: display,
        reason: `Checklist procédurale : type attendu "${chk.expectedValue}"${chk.raison ? ' (' + chk.raison + ')' : ''}`,
      });
      break;
    }

    for (const q of this.aiQuestionsSignal()) {
      if (q.critereCode?.toUpperCase() !== 'FA26_TYPE_CHANGEMENT') continue;
      if (!q.answerText) continue;
      builder.addSource('QUESTION_IA', {
        expectedDisplay: display,
        reason: `Question complémentaire : "${q.questionText}" — réponse "${q.answerText}"`,
      });
      break;
    }

    const piece = this.findPieceManquante(['FA26_TYPE_CHANGEMENT']);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  /** Motif invoqué — divergence stricte sur l'enum 5 valeurs. */
  private buildMotifInvoqueAlert(): ChangementEtatCivilCoherenceAlert | null {
    const ai = this.aiDataSignal();
    const aiRaw = ai?.motifChangementDetecte;
    if (typeof aiRaw !== 'string' || aiRaw.length === 0) return null;
    const aiVal = aiRaw.toUpperCase();
    if (!VALID_MOTIFS.has(aiVal)) return null;
    const userVal = this.motifInvoque();
    if (userVal === null) return null;
    if (userVal === aiVal) return null;

    const display = motifInvoqueLabel(aiVal as MotifInvoque);
    const builder = CoherenceAlertBuilder.forField<ChangementEtatCivilAlertField>('MOTIF_INVOQUE')
      .addSource('IA', {
        expectedDisplay: display,
        reason: `Analyse du dossier : motif "${display}"`,
      });

    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'FA26_MOTIF_INVOQUE') continue;
      if (!chk.expectedValue) continue;
      builder.addSource('F96', {
        expectedDisplay: display,
        reason: `Checklist procédurale : motif attendu "${chk.expectedValue}"${chk.raison ? ' (' + chk.raison + ')' : ''}`,
      });
      break;
    }

    const piece = this.findPieceManquante(['FA26_MOTIF_INVOQUE', 'JUSTIFICATIF_MOTIF']);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  /** Date naissance demandeur — divergence stricte sur la chaîne ISO. */
  private buildDateNaissanceAlert(): ChangementEtatCivilCoherenceAlert | null {
    const ai = this.aiDataSignal();
    const aiDate = ai?.dateNaissanceDemandeurDetectee;
    const user = this.dateNaissanceDemandeur();
    if (!user) return null;
    if (!aiDate || !ISO_DATE_REGEX.test(aiDate)) return null;
    if (user === aiDate) return null;

    const builder = CoherenceAlertBuilder.forField<ChangementEtatCivilAlertField>('DATE_NAISSANCE')
      .addSource('IA', {
        expectedDisplay: aiDate,
        reason: `Analyse du dossier : date de naissance ${aiDate}`,
      });

    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'FA26_DATE_NAISSANCE') continue;
      if (!chk.expectedValue) continue;
      builder.addSource('F96', {
        expectedDisplay: aiDate,
        reason: `Checklist procédurale : date naissance attendue ${chk.expectedValue}${chk.raison ? ' (' + chk.raison + ')' : ''}`,
      });
      break;
    }

    const piece = this.findPieceManquante([
      'FA26_DATE_NAISSANCE',
      'CERTIFICAT_NAISSANCE',
      'LIVRET_FAMILLE',
    ]);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  /** Majeur demandeur — divergence stricte boolean (mineur/majeur). */
  private buildMajeurAlert(): ChangementEtatCivilCoherenceAlert | null {
    const ai = this.aiDataSignal();
    if (typeof ai?.majeurDemandeurDetected !== 'boolean') return null;
    if (this.majeurDemandeur() === ai.majeurDemandeurDetected) return null;

    const display = ai.majeurDemandeurDetected
      ? 'Demandeur majeur'
      : 'Demandeur mineur';
    const builder = CoherenceAlertBuilder.forField<ChangementEtatCivilAlertField>('MAJEUR_DEMANDEUR')
      .addSource('IA', {
        expectedDisplay: display,
        reason: `Analyse du dossier : ${display.toLowerCase()}`,
      });

    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'FA26_MAJEUR_DEMANDEUR') continue;
      if (!chk.expectedValue) continue;
      builder.addSource('F96', {
        expectedDisplay: display,
        reason: `Checklist procédurale : ${chk.expectedValue}${chk.raison ? ' (' + chk.raison + ')' : ''}`,
      });
      break;
    }

    const piece = this.findPieceManquante(['FA26_MAJEUR_DEMANDEUR', 'CERTIFICAT_NAISSANCE']);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  /**
   * Consentement parental — alerte si IA détecte présence/absence et l'avocat
   * a saisi l'inverse (critique pour les mineurs — art. 60 al. 3 Cciv).
   */
  private buildConsentementParentalAlert(): ChangementEtatCivilCoherenceAlert | null {
    const ai = this.aiDataSignal();
    if (typeof ai?.consentementParentalDetected !== 'boolean') return null;
    if (this.consentementParental() === ai.consentementParentalDetected) return null;

    const display = ai.consentementParentalDetected
      ? 'Consentement parental détecté'
      : 'Pas de consentement parental détecté';
    const builder = CoherenceAlertBuilder.forField<ChangementEtatCivilAlertField>('CONSENTEMENT_PARENTAL')
      .addSource('IA', {
        expectedDisplay: display,
        reason: `Analyse du dossier : ${display.toLowerCase()}`,
      });

    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'FA26_CONSENTEMENT_PARENTAL') continue;
      if (!chk.expectedValue) continue;
      builder.addSource('F96', {
        expectedDisplay: display,
        reason: `Checklist procédurale : ${chk.expectedValue}${chk.raison ? ' (' + chk.raison + ')' : ''}`,
      });
      break;
    }

    const piece = this.findPieceManquante([
      'FA26_CONSENTEMENT_PARENTAL',
      'AUTORISATION_PARENTALE',
    ]);
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

  typeChangementLabel = typeChangementLabel;
  motifInvoqueLabel = motifInvoqueLabel;
  preuveEtatCivilLabel = preuveEtatCivilLabel;
  competenceProcedureLabel = competenceProcedureLabel;
  verdictLabel = verdictEtatCivilLabel;

  verdictBannerClass(verdict: VerdictEtatCivil): string {
    switch (verdict) {
      case 'ELEVEE': return 'cec-verdict-banner cec-verdict-banner--strong';
      case 'MOYENNE': return 'cec-verdict-banner cec-verdict-banner--medium';
      case 'FAIBLE': return 'cec-verdict-banner cec-verdict-banner--weak';
    }
  }

  competenceCardClass(c: CompetenceProcedure): string {
    switch (c) {
      case 'MAIRIE': return 'cec-competence-card cec-competence-card--mairie';
      case 'JUGE': return 'cec-competence-card cec-competence-card--juge';
      case 'TRIBUNAL_JUDICIAIRE': return 'cec-competence-card cec-competence-card--tribunal';
    }
  }

  competenceIcon(c: CompetenceProcedure): string {
    switch (c) {
      case 'MAIRIE': return 'account_balance';
      case 'JUGE': return 'gavel';
      case 'TRIBUNAL_JUDICIAIRE': return 'balance';
    }
  }
}
