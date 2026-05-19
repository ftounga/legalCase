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
import { MatRadioModule } from '@angular/material/radio';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NaturalisationService } from '../../core/services/naturalisation.service';
import {
  NaturalisationRequest,
  NaturalisationResponse,
  VOIE_NATURALISATION_LABELS,
  VerdictRecevabilite,
  VoieNaturalisationCode,
  mapVoieFromIa,
  voieLabel,
} from '../../core/models/naturalisation.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import {
  ImmigrationExtractedData,
  PieceManquanteEntry,
} from '../../core/models/case-analysis.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { AiQuestion } from '../../core/models/ai-question.model';
import { CoherencePopoverTriggerDirective } from '../../shared/coherence-popover/coherence-popover-trigger.directive';
import {
  CoherenceAlert,
  CoherenceAlertSource,
} from '../../shared/coherence-popover/coherence-alert.model';
import { CoherenceAlertBuilder } from '../../shared/coherence-popover/coherence-alert-builder';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { NaturalisationPrefillRules } from './naturalisation-section-prefill-rules';

/**
 * SF-IM-13-02 : champs F-IA-03 audités par l'outil "Naturalisation".
 * - VOIE_NATURALISATION : divergence si l'IA détecte une procédure
 *   NATURALISATION_X (procedureChecks / questions) avec une voie
 *   expectedValue ≠ saisie avocat.
 */
export type NaturalisationAlertField = 'VOIE_NATURALISATION';

export type NaturalisationAlertSource = CoherenceAlertSource;
export type NaturalisationCoherenceAlert = CoherenceAlert<NaturalisationAlertField>;

/**
 * SF-IM-13-02 : section frontend dédiée Naturalisation française (Code civil art. 21+).
 * 6 voies : DECRET / MARIAGE / ASCENDANT / MINEUR / REINTEGRATION / OPPOSITION.
 *
 * Single-country FR — Belgique : bannière info ("régime Code civil propre
 * à la France") + form masqué (pas d'appel HTTP). L'équivalent BE relève
 * du CNB (art. 12bis et s. — feature jumelle au backlog F-IM-13-BE).
 *
 * Pattern de référence : `changement-statut-section` (SF-IM-11-02 PR #640
 * — multi-fieldsets + bandeau verdict 3 niveaux + builder F-IA-03 + gate FR).
 */
@Component({
  selector: 'app-naturalisation-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatRadioModule, MatSlideToggleModule,
    MatChipsModule, MatProgressSpinnerModule,
    LegalCitationsPipe,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './naturalisation-section.component.html',
  styleUrl: './naturalisation-section.component.scss',
})
export class NaturalisationSectionComponent implements OnInit, OnChanges {
  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'NATURALISATION (FR)';
  static readonly TOOL_ICON = 'how_to_reg';

  /** F-236 SF-236-02 — Délègue au helper pur partagé (count=0, no-op runtime). */
  static getPrefillCount(input: PrefillCountInput): number {
    return NaturalisationPrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';
  // Sources IA pour pré-fill + alertes de cohérence F-IA-03.
  @Input() aiData?: ImmigrationExtractedData | null;
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
   *  - `calculate()` POSTe sur `/api/v1/simulators/F-IM-13-naturalisation/calculate`.
   *  - `triggerRefresh()` jamais invoqué.
   *
   * Default `false` — mode case-file scoped inchangé.
   */
  @Input() standaloneMode: boolean = false;
  // Snapshots signal des inputs IA pour que `computed` réagisse.
  private aiDataSignal = signal<ImmigrationExtractedData | null | undefined>(undefined);
  private procedureChecksSignal = signal<ProcedureCheck[]>([]);
  private aiQuestionsSignal = signal<AiQuestion[]>([]);
  private piecesManquantesSignal = signal<PieceManquanteEntry[]>([]);

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<NaturalisationResponse | null>(null);

  // Form fields (signal-based).
  voieNaturalisation = signal<VoieNaturalisationCode | null>(null);
  dureeResidenceReguliereAnnees = signal<number | null>(null);
  dureeMariageAnnees = signal<number | null>(null);
  cohabitationContinue = signal<boolean>(false);
  ageDemandeur = signal<number | null>(null);
  ascendantDirectFrancais = signal<boolean>(false);
  parentAcquiertNationalite = signal<boolean>(false);
  vitAvecParentAcquereur = signal<boolean>(false);
  ancienFrancais = signal<boolean>(false);
  casierJudiciaireVierge = signal<boolean>(true);
  assimilationLangueB1 = signal<boolean>(false);
  ressourcesStables = signal<boolean>(false);
  oppositionGouvernementaleActive = signal<boolean>(false);
  etudesSuperieuresFrance = signal<boolean>(false);

  /** Provenance IA — badge "Pré-rempli depuis l'analyse" effaçable. */
  provenanceVoie = signal<'IA' | null>(null);
  /** SF-246-19 : provenances IA pour les champs numériques naturalisation. */
  provenanceDureeResidence = signal<'IA' | null>(null);
  provenanceDureeMariage = signal<'IA' | null>(null);
  provenanceAge = signal<'IA' | null>(null);

  /** Listes pour mat-radio. */
  readonly voieOptions = VOIE_NATURALISATION_LABELS;

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  /** Champs conditionnels selon la voie sélectionnée. */
  showDecretFields = computed<boolean>(() => this.voieNaturalisation() === 'DECRET');
  showMariageFields = computed<boolean>(() => this.voieNaturalisation() === 'MARIAGE');
  showAscendantFields = computed<boolean>(() => this.voieNaturalisation() === 'ASCENDANT');
  showMineurFields = computed<boolean>(() => this.voieNaturalisation() === 'MINEUR');
  showReintegrationFields = computed<boolean>(() => this.voieNaturalisation() === 'REINTEGRATION');
  showOppositionFields = computed<boolean>(() => this.voieNaturalisation() === 'OPPOSITION');

  /** Alertes de cohérence F-IA-03 par field — gate uniquement en mode formulaire. */
  coherenceAlerts = computed<Partial<Record<NaturalisationAlertField, NaturalisationCoherenceAlert>>>(() => {
    // F-163 SF-163-02d : aucune source IA en standalone.
    if (this.standaloneMode) return {};
    if (!this.showForm()) return {};
    if (!this.isFrance()) return {};
    const alerts: Partial<Record<NaturalisationAlertField, NaturalisationCoherenceAlert>> = {};
    const a = this.buildVoieAlert();
    if (a) alerts.VOIE_NATURALISATION = a;
    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    const blockers = values.filter((a) => a.severity === 'CRITICAL').length;
    return { total: values.length, blockers };
  });

  constructor(
    private service: NaturalisationService,
    private snackBar: MatSnackBar,
    @Optional() private dashboardRefresh: CaseDashboardRefreshService | null,
  ) {}

  ngOnInit(): void {
    // F-177 SF-177-03b : appliqué dès le mount pour le mode modal.
    if (this.forceExpanded) this.collapsed.set(false);
    this.aiDataSignal.set(this.aiData);
    this.procedureChecksSignal.set(this.procedureChecks ?? []);
    this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    this.piecesManquantesSignal.set(this.piecesManquantes ?? []);
    if (this.standaloneMode) {

      // F-163 SF-163-02d : pas de dossier à interroger en standalone.

      this.collapsed.set(false);

      this.loading.set(false);

      this.showForm.set(true);

      return;

    }
    if (this.isFrance()) {
      this.load();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    // F-177 SF-177-03b : applique le forceExpanded quand il passe à true en cours de vie.
    if (changes['forceExpanded'] && this.forceExpanded) this.collapsed.set(false);
    if (changes['aiData']) this.aiDataSignal.set(this.aiData);
    if (changes['procedureChecks']) this.procedureChecksSignal.set(this.procedureChecks ?? []);
    if (changes['aiQuestions']) this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    if (changes['piecesManquantes']) this.piecesManquantesSignal.set(this.piecesManquantes ?? []);

    // Re-prefill quand aiData arrive après le mount, sans écraser les valeurs avocat.
    if (changes['aiData'] && !changes['aiData'].firstChange
        && this.isFrance() && this.showForm() && !this.result()) {
      this.prefillFromAi();
    }
  }

  toggleCollapse(): void {
    this.collapsed.update((v) => !v);
  }

  /**
   * Form valid si la voie est choisie et tous les champs requis pour cette voie
   * sont saisis. La voie OPPOSITION n'a pas de champ requis (info-only).
   */
  formValid(): boolean {
    const voie = this.voieNaturalisation();
    if (!voie) return false;
    switch (voie) {
      case 'DECRET': {
        const d = this.dureeResidenceReguliereAnnees();
        return d !== null && d !== undefined && d >= 0;
      }
      case 'MARIAGE': {
        const d = this.dureeMariageAnnees();
        return d !== null && d !== undefined && d >= 0;
      }
      case 'ASCENDANT': {
        const a = this.ageDemandeur();
        const r = this.dureeResidenceReguliereAnnees();
        return a !== null && a !== undefined && a >= 0
          && r !== null && r !== undefined && r >= 0;
      }
      case 'MINEUR':
        // Toggles boolean — toujours valide (même false explicite).
        return true;
      case 'REINTEGRATION':
        return true;
      case 'OPPOSITION':
        // Voie info-only — pas de champ supplémentaire.
        return true;
      default:
        return false;
    }
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // Handlers de modification — effacent la provenance IA pour la voie.
  onVoieChange(value: VoieNaturalisationCode | null): void {
    this.voieNaturalisation.set(value);
    this.provenanceVoie.set(null);
  }

  onDureeResidenceChange(value: number | null): void {
    this.dureeResidenceReguliereAnnees.set(value === null || value === undefined ? null : value);
    this.provenanceDureeResidence.set(null);
  }

  onDureeMariageChange(value: number | null): void {
    this.dureeMariageAnnees.set(value === null || value === undefined ? null : value);
    this.provenanceDureeMariage.set(null);
  }

  onCohabitationContinueChange(value: boolean): void {
    this.cohabitationContinue.set(value);
  }

  onAgeChange(value: number | null): void {
    this.ageDemandeur.set(value === null || value === undefined ? null : value);
    this.provenanceAge.set(null);
  }

  onAscendantDirectFrancaisChange(value: boolean): void {
    this.ascendantDirectFrancais.set(value);
  }

  onParentAcquiertNationaliteChange(value: boolean): void {
    this.parentAcquiertNationalite.set(value);
  }

  onVitAvecParentAcquereurChange(value: boolean): void {
    this.vitAvecParentAcquereur.set(value);
  }

  onAncienFrancaisChange(value: boolean): void {
    this.ancienFrancais.set(value);
  }

  onCasierJudiciaireChange(value: boolean): void {
    this.casierJudiciaireVierge.set(value);
  }

  onAssimilationLangueB1Change(value: boolean): void {
    this.assimilationLangueB1.set(value);
  }

  onRessourcesStablesChange(value: boolean): void {
    this.ressourcesStables.set(value);
  }

  onOppositionActiveChange(value: boolean): void {
    this.oppositionGouvernementaleActive.set(value);
  }

  onEtudesSuperieuresFranceChange(value: boolean): void {
    this.etudesSuperieuresFrance.set(value);
  }

  /**
   * Bandeau verdict :
   * - ELEVEE → navy/info, recevabilité technique acquise.
   * - MOYENNE → or/warning, voie ouverte mais conditions limites.
   * - FAIBLE → rouge alerte, critère bloquant.
   */
  bannerClass(verdict: VerdictRecevabilite | string | undefined | null): string {
    switch (verdict) {
      case 'ELEVEE': return 'nat-banner nat-banner--info';
      case 'MOYENNE': return 'nat-banner nat-banner--warning';
      case 'FAIBLE': return 'nat-banner nat-banner--critical';
      default: return 'nat-banner nat-banner--info';
    }
  }

  bannerIcon(verdict: VerdictRecevabilite | string | undefined | null): string {
    switch (verdict) {
      case 'ELEVEE': return 'verified';
      case 'MOYENNE': return 'warning';
      case 'FAIBLE': return 'gpp_bad';
      default: return 'info_outline';
    }
  }

  bannerLabel(verdict: VerdictRecevabilite | string | undefined | null): string {
    switch (verdict) {
      case 'ELEVEE': return 'Recevabilité élevée';
      case 'MOYENNE': return 'Recevabilité possible — conditions limites';
      case 'FAIBLE': return 'Recevabilité faible — critère bloquant';
      default: return '';
    }
  }

  /** Libellé humain depuis un code voie (export model). */
  voieLabel(code: string | null | undefined): string {
    return voieLabel(code as VoieNaturalisationCode | null);
  }

  /**
   * SF-IM-13-02 : pré-fill depuis `aiData` (ImmigrationExtractedData).
   *
   * Le prompt IA Immigration n'expose pas (encore) les champs naturalisation
   * directs (durée résidence, âge, durée mariage). Le pré-fill reste donc
   * limité — pattern en place pour recevoir ces champs sans changement
   * structurel quand l'extracteur sera enrichi (SF future backlog).
   *
   * Règles (fail-open) :
   * - passe silencieusement si aiData absent
   * - ne pré-remplit QUE si le champ est encore vide (préserve les edits)
   * - n'écrase jamais si provenance !== 'IA'
   */
  private prefillFromAi(): void {
    const ai = this.aiDataSignal();
    if (!ai) return;
    // F-236 SF-236-02 : délègue au helper pur partagé.
    const input: PrefillCountInput = {
      aiData: ai,
      workspaceCountry: this.workspaceCountry,
    };

    // SF-246-19 : durée de résidence régulière.
    const dureeRes = NaturalisationPrefillRules.computeDureeResidenceReguliereAnnees(input);
    if (dureeRes !== null
        && (this.dureeResidenceReguliereAnnees() === null
            || this.provenanceDureeResidence() === 'IA')) {
      this.dureeResidenceReguliereAnnees.set(dureeRes);
      this.provenanceDureeResidence.set('IA');
    }

    // SF-246-19 : durée de mariage.
    const dureeMar = NaturalisationPrefillRules.computeDureeMariageAnnees(input);
    if (dureeMar !== null
        && (this.dureeMariageAnnees() === null || this.provenanceDureeMariage() === 'IA')) {
      this.dureeMariageAnnees.set(dureeMar);
      this.provenanceDureeMariage.set('IA');
    }

    // SF-246-19 : âge demandeur.
    const age = NaturalisationPrefillRules.computeAgeDemandeur(input);
    if (age !== null
        && (this.ageDemandeur() === null || this.provenanceAge() === 'IA')) {
      this.ageDemandeur.set(age);
      this.provenanceAge.set('IA');
    }
  }

  /**
   * Divergence voie naturalisation — IA / F-96 / Question IA détecte une
   * procédure NATURALISATION_X via expectedValue divergente de la saisie
   * avocat.
   * Multi-sources F-96 / QUESTION_IA / IA / PIECE_MANQUANTE.
   */
  private buildVoieAlert(): NaturalisationCoherenceAlert | null {
    const userVoie = this.voieNaturalisation();
    if (!userVoie) return null;

    const builder = CoherenceAlertBuilder.forField<NaturalisationAlertField>('VOIE_NATURALISATION');

    // 1. F-96
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'IM13_VOIE_NATURALISATION') continue;
      const ev = chk.expectedValue;
      if (!ev) continue;
      const expected = mapVoieFromIa(ev);
      if (!expected || expected === userVoie) continue;
      builder.addSource('F96', {
        expectedDisplay: this.voieLabel(expected),
        reason: `Checklist procédurale : voie attendue ${this.voieLabel(expected)}`
          + (chk.raison ? ` (${chk.raison})` : ''),
      });
      break;
    }

    // 2. QUESTION_IA
    for (const q of this.aiQuestionsSignal()) {
      if (q.critereCode?.toUpperCase() !== 'IM13_VOIE_NATURALISATION') continue;
      const answer = q.answerText?.trim().toLowerCase();
      if (!answer) continue;
      const isOui = answer === 'oui' || answer.startsWith('oui ')
        || answer.startsWith('oui,') || answer.startsWith('oui.');
      if (!isOui) continue;
      const ev = q.expectedValue;
      if (!ev) continue;
      const expected = mapVoieFromIa(ev);
      if (!expected || expected === userVoie) continue;
      builder.addSource('QUESTION_IA', {
        expectedDisplay: this.voieLabel(expected),
        reason: `Question complémentaire : "${q.questionText}" → "${q.answerText}"`,
      });
      break;
    }

    // 3. IA — `aiData.typeProcedureDetectee` (si NATURALISATION_X).
    const ai = this.aiDataSignal();
    const procDetectee = ai?.typeProcedureDetectee ?? null;
    const iaVoie = mapVoieFromIa(procDetectee);
    if (iaVoie && iaVoie !== userVoie) {
      builder.addSource('IA', {
        expectedDisplay: this.voieLabel(iaVoie),
        reason: `Analyse du dossier : voie détectée "${this.voieLabel(iaVoie)}"`,
      });
    }

    // 4. PIECE_MANQUANTE — contributor enrichissant.
    const piece = this.findPieceManquante(['IM13_VOIE_NATURALISATION', 'NATURALISATION_DOSSIER']);
    if (piece) builder.addPieceManquante(piece);

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

  alertTooltip(alert: NaturalisationCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: NaturalisationCoherenceAlert): string {
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
   * Classe CSS d'un critère non rempli selon sa nature.
   * - "casier", "opposition", "insuffisant", "bloquant" → rouge (critical)
   * - autre → or (warning)
   */
  critereChipClass(critere: string): string {
    if (!critere) return 'nat-chip-critere nat-chip-critere--warning';
    const r = critere.toLowerCase();
    if (r.includes('casier') || r.includes('opposition')
        || r.includes('insuffisant') || r.includes('bloquant')
        || r.includes('inapplicable') || r.includes('impossible')) {
      return 'nat-chip-critere nat-chip-critere--critical';
    }
    return 'nat-chip-critere nat-chip-critere--warning';
  }

  calculate(): void {
    if (!this.formValid()) return;
    const voie = this.voieNaturalisation()!;
    const request: NaturalisationRequest = {
      voieNaturalisation: voie,
      casierJudiciaireVierge: this.casierJudiciaireVierge(),
      oppositionGouvernementaleActive: this.oppositionGouvernementaleActive(),
    };
    // Champs spécifiques par voie.
    switch (voie) {
      case 'DECRET':
        request.dureeResidenceReguliereAnnees = this.dureeResidenceReguliereAnnees();
        request.assimilationLangueB1 = this.assimilationLangueB1();
        request.ressourcesStables = this.ressourcesStables();
        request.etudesSuperieuresFrance = this.etudesSuperieuresFrance();
        break;
      case 'MARIAGE':
        request.dureeMariageAnnees = this.dureeMariageAnnees();
        request.cohabitationContinue = this.cohabitationContinue();
        request.assimilationLangueB1 = this.assimilationLangueB1();
        break;
      case 'ASCENDANT':
        request.ageDemandeur = this.ageDemandeur();
        request.dureeResidenceReguliereAnnees = this.dureeResidenceReguliereAnnees();
        request.ascendantDirectFrancais = this.ascendantDirectFrancais();
        break;
      case 'MINEUR':
        request.parentAcquiertNationalite = this.parentAcquiertNationalite();
        request.vitAvecParentAcquereur = this.vitAvecParentAcquereur();
        break;
      case 'REINTEGRATION':
        request.ancienFrancais = this.ancienFrancais();
        break;
      case 'OPPOSITION':
        // Voie info-only — pas de champ supplémentaire.
        break;
    }
    this.calculating.set(true);
    // F-163 SF-163-02d : en standalone, POST sur le dispatcher générique.

    const request$ = this.standaloneMode

      ? this.service.calculateStandalone(request)

      : this.service.calculate(this.caseFileId, request);

    request$.subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Naturalisation analysée', 'OK', { duration: 2500 });
        // F-163 SF-163-02d : pas de dashboard à rafraîchir en standalone.

        if (!this.standaloneMode) { this.dashboardRefresh?.triggerRefresh(); }
      },
      error: (err) => {
        this.calculating.set(false);
        const msg = err?.error?.message || err?.error || 'Erreur lors de l\'analyse';
        this.snackBar.open(String(msg), 'Fermer', { duration: 5000, panelClass: 'snack-error' });
      },
    });
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        // Hydrate le form avec les valeurs persistées (pour ré-éditer).
        const voie = mapVoieFromIa(r.voieNaturalisation);
        if (voie) this.voieNaturalisation.set(voie);
        if (r.dureeResidenceReguliereAnnees !== undefined && r.dureeResidenceReguliereAnnees !== null) {
          this.dureeResidenceReguliereAnnees.set(r.dureeResidenceReguliereAnnees);
        }
        if (r.dureeMariageAnnees !== undefined && r.dureeMariageAnnees !== null) {
          this.dureeMariageAnnees.set(r.dureeMariageAnnees);
        }
        if (r.ageDemandeur !== undefined && r.ageDemandeur !== null) {
          this.ageDemandeur.set(r.ageDemandeur);
        }
        // Provenance reset — valeurs persistées = saisie avocat.
        this.provenanceVoie.set(null);
        this.provenanceDureeResidence.set(null);
        this.provenanceDureeMariage.set(null);
        this.provenanceAge.set(null);
        this.showForm.set(false);
        this.loading.set(false);
      },
      error: () => {
        // 404 attendu si jamais calculé — fallback pré-fill IA.
        this.prefillFromAi();
        this.loading.set(false);
      },
    });
  }
}
