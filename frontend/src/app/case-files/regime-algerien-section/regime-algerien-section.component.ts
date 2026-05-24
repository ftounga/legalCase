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
import { RegimeAlgerienService } from '../../core/services/regime-algerien.service';
import {
  RegimeAlgerienRequest,
  RegimeAlgerienResponse,
  VOIE_REGIME_ALGERIEN_LABELS,
  VerdictRecevabilite,
  VoieRegimeAlgerienCode,
  detectNationaliteAlgerienneFromIa,
  mapVoieFromIa,
  voieLabel,
} from '../../core/models/regime-algerien.model';
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
import { RegimeAlgerienPrefillRules } from './regime-algerien-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-IM-17-02 : champs F-IA-03 audités par l'outil "Régime algérien".
 * - VOIE : divergence si l'IA détecte une procédure CRA_X (procedureChecks /
 *   questions / `aiData.typeProcedureDetectee`) avec une voie expectedValue
 *   ≠ saisie avocat.
 */
export type RegimeAlgerienAlertField = 'VOIE';

export type RegimeAlgerienAlertSource = CoherenceAlertSource;
export type RegimeAlgerienCoherenceAlert = CoherenceAlert<RegimeAlgerienAlertField>;

/**
 * SF-IM-17-02 : section frontend dédiée Régime algérien (Accord franco-algérien
 * du 27/12/1968). 5 voies CRA : CRA_1_AN / CRA_10_ANS_LIEN_FRANCE /
 * CRA_10_ANS_RESIDENT_ANCIEN / CHANGEMENT_VERS_TRAVAILLEUR /
 * REGROUPEMENT_FAMILIAL_ACCORD_1968.
 *
 * Single-country FR — Belgique : bannière info ("Régime bilatéral FR-DZ
 * uniquement") + form masqué (pas d'appel HTTP). Pas d'équivalent BE.
 *
 * Gate nationalité : `nationaliteAlgerienne` doit être true pour activer
 * le formulaire (l'accord ne s'applique qu'aux ressortissants algériens —
 * régime CESEDA distinct).
 *
 * Pattern de référence : `naturalisation-section` (SF-IM-13-02 PR #646
 * — multi-fieldsets + bandeau verdict 3 niveaux + builder F-IA-03 + gate FR).
 */
@Component({
  selector: 'app-regime-algerien-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatRadioModule, MatSlideToggleModule,
    MatChipsModule, MatProgressSpinnerModule,
    LegalCitationsPipe,
    ToolJurisprudenceCitationsComponent,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './regime-algerien-section.component.html',
  styleUrl: './regime-algerien-section.component.scss',
})
export class RegimeAlgerienSectionComponent implements OnInit, OnChanges {
  // F-JU-03 SF-JU-03-99c v2 — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-IM-17-regime-algerien';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'RÉGIME ALGÉRIEN (FR)';
  static readonly TOOL_ICON = 'flag';

  /** F-236 SF-236-02 — Délègue intégralement au helper pur partagé. */
  static getPrefillCount(input: PrefillCountInput): number {
    return RegimeAlgerienPrefillRules.computePrefillCount(input);
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
   *  - `calculate()` POSTe sur `/api/v1/simulators/F-IM-17-regime-algerien/calculate`.
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
  result = signal<RegimeAlgerienResponse | null>(null);

  // Form fields (signal-based).
  voieDemande = signal<VoieRegimeAlgerienCode | null>(null);
  /** Gate nationalité — doit être true pour activer la soumission. */
  nationaliteAlgerienne = signal<boolean>(false);
  documentEtatCivilOriginal = signal<boolean>(false);
  presenceReguliereFranceMois = signal<number | null>(null);
  casierJudiciaireVierge = signal<boolean>(true);
  visaLongSejourValide = signal<boolean>(false);
  conjointFrancais = signal<boolean>(false);
  parentEnfantFrancais = signal<boolean>(false);
  neEnFrance = signal<boolean>(false);
  arriveeAvant13Ans = signal<boolean>(false);
  contratTravailValide = signal<boolean>(false);
  ressourcesSuffisantes = signal<boolean>(false);
  logementDecent = signal<boolean>(false);
  nombrePersonnesFoyer = signal<number | null>(null);

  /** Provenance IA — badges "Pré-rempli depuis l'analyse" effaçables. */
  provenanceVoie = signal<'IA' | null>(null);
  provenanceNationalite = signal<'IA' | null>(null);
  /** SF-246-19 : provenance IA pour la durée de présence régulière. */
  provenancePresenceReguliere = signal<'IA' | null>(null);

  /** Listes pour mat-radio. */
  readonly voieOptions = VOIE_REGIME_ALGERIEN_LABELS;

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  /** Champs conditionnels selon la voie sélectionnée. */
  showCra1AnFields = computed<boolean>(() => this.voieDemande() === 'CRA_1_AN');
  showLienFranceFields = computed<boolean>(() => this.voieDemande() === 'CRA_10_ANS_LIEN_FRANCE');
  showResidentAncienFields = computed<boolean>(() => this.voieDemande() === 'CRA_10_ANS_RESIDENT_ANCIEN');
  showTravailleurFields = computed<boolean>(() => this.voieDemande() === 'CHANGEMENT_VERS_TRAVAILLEUR');
  showRegroupementFields = computed<boolean>(() => this.voieDemande() === 'REGROUPEMENT_FAMILIAL_ACCORD_1968');

  /**
   * La vraie barrière "form actif" :
   * - en FR
   * - avec nationalité algérienne confirmée par l'avocat (la backend gate
   *   rejette `nationaliteAlgerienne === false` en 400).
   */
  formActive = computed<boolean>(() => this.isFrance() && this.nationaliteAlgerienne());

  /** Alertes de cohérence F-IA-03 par field — gate uniquement en mode formulaire actif. */
  coherenceAlerts = computed<Partial<Record<RegimeAlgerienAlertField, RegimeAlgerienCoherenceAlert>>>(() => {
    // F-163 SF-163-02d : aucune source IA en standalone.
    if (this.standaloneMode) return {};
    if (!this.showForm()) return {};
    if (!this.formActive()) return {};
    const alerts: Partial<Record<RegimeAlgerienAlertField, RegimeAlgerienCoherenceAlert>> = {};
    const a = this.buildVoieAlert();
    if (a) alerts.VOIE = a;
    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    const blockers = values.filter((a) => a.severity === 'CRITICAL').length;
    return { total: values.length, blockers };
  });

  constructor(
    private service: RegimeAlgerienService,
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
   * Form valid ssi : nationalité algérienne confirmée + voie choisie + champs
   * requis pour cette voie sont saisis.
   */
  formValid(): boolean {
    if (!this.nationaliteAlgerienne()) return false;
    const voie = this.voieDemande();
    if (!voie) return false;
    switch (voie) {
      case 'CRA_1_AN': {
        const m = this.presenceReguliereFranceMois();
        return m !== null && m !== undefined && m >= 0;
      }
      case 'CRA_10_ANS_LIEN_FRANCE': {
        // Au moins un lien France doit être coché.
        const m = this.presenceReguliereFranceMois();
        const dureeOk = m !== null && m !== undefined && m >= 0;
        return dureeOk;
      }
      case 'CRA_10_ANS_RESIDENT_ANCIEN':
        // Toggles boolean — toujours techniquement valide.
        return true;
      case 'CHANGEMENT_VERS_TRAVAILLEUR':
        return true;
      case 'REGROUPEMENT_FAMILIAL_ACCORD_1968': {
        const n = this.nombrePersonnesFoyer();
        return n !== null && n !== undefined && n >= 0;
      }
      default:
        return false;
    }
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // Handlers de modification — effacent la provenance IA pour le champ.
  onVoieChange(value: VoieRegimeAlgerienCode | null): void {
    this.voieDemande.set(value);
    this.provenanceVoie.set(null);
  }

  onNationaliteAlgerienneChange(value: boolean): void {
    this.nationaliteAlgerienne.set(value);
    this.provenanceNationalite.set(null);
  }

  onDocumentEtatCivilChange(value: boolean): void {
    this.documentEtatCivilOriginal.set(value);
  }

  onPresenceReguliereChange(value: number | null): void {
    this.presenceReguliereFranceMois.set(value === null || value === undefined ? null : value);
    this.provenancePresenceReguliere.set(null);
  }

  onCasierJudiciaireChange(value: boolean): void {
    this.casierJudiciaireVierge.set(value);
  }

  onVisaLongSejourChange(value: boolean): void {
    this.visaLongSejourValide.set(value);
  }

  onConjointFrancaisChange(value: boolean): void {
    this.conjointFrancais.set(value);
  }

  onParentEnfantFrancaisChange(value: boolean): void {
    this.parentEnfantFrancais.set(value);
  }

  onNeEnFranceChange(value: boolean): void {
    this.neEnFrance.set(value);
  }

  onArriveeAvant13Change(value: boolean): void {
    this.arriveeAvant13Ans.set(value);
  }

  onContratTravailChange(value: boolean): void {
    this.contratTravailValide.set(value);
  }

  onRessourcesSuffisantesChange(value: boolean): void {
    this.ressourcesSuffisantes.set(value);
  }

  onLogementDecentChange(value: boolean): void {
    this.logementDecent.set(value);
  }

  onNombrePersonnesChange(value: number | null): void {
    this.nombrePersonnesFoyer.set(value === null || value === undefined ? null : value);
  }

  /**
   * Bandeau verdict :
   * - ELEVEE → navy/info, recevabilité technique acquise.
   * - MOYENNE → or/warning, voie ouverte mais conditions limites.
   * - FAIBLE → rouge alerte, critère bloquant.
   */
  bannerClass(verdict: VerdictRecevabilite | string | undefined | null): string {
    switch (verdict) {
      case 'ELEVEE': return 'rgalg-banner rgalg-banner--info';
      case 'MOYENNE': return 'rgalg-banner rgalg-banner--warning';
      case 'FAIBLE': return 'rgalg-banner rgalg-banner--critical';
      default: return 'rgalg-banner rgalg-banner--info';
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
    return voieLabel(code as VoieRegimeAlgerienCode | null);
  }

  /**
   * SF-IM-17-02 : pré-fill depuis `aiData` (ImmigrationExtractedData).
   *
   * - `nationaliteAlgerienne` : heuristique défensive (cf.
   *   `detectNationaliteAlgerienneFromIa`). Le prompt IA Immigration n'expose
   *   pas (encore) un champ `nationalite` dédié. Le pré-fill reste donc
   *   limité — pattern présent pour bénéficier sans changement structurel
   *   d'un futur enrichissement extracteur.
   * - `voieDemande` : tentative via `aiData.typeProcedureDetectee`.
   *
   * Règles (fail-open) :
   * - passe silencieusement si aiData absent
   * - ne pré-remplit QUE si le champ est encore vide (préserve les edits)
   * - n'écrase jamais si provenance !== 'IA'
   */
  private prefillFromAi(): void {
    // F-236 SF-236-02 : délègue au helper pur partagé.
    const input: PrefillCountInput = {
      aiData: this.aiDataSignal(),
      workspaceCountry: this.workspaceCountry,
    };
    if (!this.nationaliteAlgerienne() && this.provenanceNationalite() === null) {
      const nat = RegimeAlgerienPrefillRules.computeNationaliteAlgerienne(input);
      if (nat === true) {
        this.nationaliteAlgerienne.set(true);
        this.provenanceNationalite.set('IA');
      }
    }
    if (this.voieDemande() === null && this.provenanceVoie() === null) {
      const voie = RegimeAlgerienPrefillRules.computeVoieDemande(input);
      if (voie !== null) {
        this.voieDemande.set(voie);
        this.provenanceVoie.set('IA');
      }
    }

    // SF-246-19 : durée présence régulière en France.
    const presenceMois = RegimeAlgerienPrefillRules.computePresenceReguliereFranceMois(input);
    if (presenceMois !== null
        && (this.presenceReguliereFranceMois() === null
            || this.provenancePresenceReguliere() === 'IA')) {
      this.presenceReguliereFranceMois.set(presenceMois);
      this.provenancePresenceReguliere.set('IA');
    }
  }

  /**
   * Divergence voie — IA / F-96 / Question IA détecte une procédure CRA_X
   * via expectedValue divergente de la saisie avocat.
   * Multi-sources F-96 / QUESTION_IA / IA / PIECE_MANQUANTE.
   */
  private buildVoieAlert(): RegimeAlgerienCoherenceAlert | null {
    const userVoie = this.voieDemande();
    if (!userVoie) return null;

    const builder = CoherenceAlertBuilder.forField<RegimeAlgerienAlertField>('VOIE');

    // 1. F-96
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'IM17_VOIE_REGIME_ALGERIEN') continue;
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
      if (q.critereCode?.toUpperCase() !== 'IM17_VOIE_REGIME_ALGERIEN') continue;
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

    // 3. IA — `aiData.typeProcedureDetectee` (si CRA_X).
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
    const piece = this.findPieceManquante(['IM17_VOIE_REGIME_ALGERIEN', 'REGIME_ALGERIEN_DOSSIER']);
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

  alertTooltip(alert: RegimeAlgerienCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: RegimeAlgerienCoherenceAlert): string {
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
   * - "casier", "insuffisant", "bloquant", "manquant" → rouge (critical)
   * - autre → or (warning)
   */
  critereChipClass(critere: string): string {
    if (!critere) return 'rgalg-chip-critere rgalg-chip-critere--warning';
    const r = critere.toLowerCase();
    if (r.includes('casier') || r.includes('insuffisant')
        || r.includes('bloquant') || r.includes('manquant')
        || r.includes('inapplicable') || r.includes('impossible')
        || r.includes('non algérien')) {
      return 'rgalg-chip-critere rgalg-chip-critere--critical';
    }
    return 'rgalg-chip-critere rgalg-chip-critere--warning';
  }

  calculate(): void {
    if (!this.formValid()) return;
    const voie = this.voieDemande()!;
    const request: RegimeAlgerienRequest = {
      voieDemande: voie,
      nationaliteAlgerienne: this.nationaliteAlgerienne(),
      documentEtatCivilOriginal: this.documentEtatCivilOriginal(),
      casierJudiciaireVierge: this.casierJudiciaireVierge(),
    };
    // Champs spécifiques par voie.
    switch (voie) {
      case 'CRA_1_AN':
        request.presenceReguliereFranceMois = this.presenceReguliereFranceMois();
        request.visaLongSejourValide = this.visaLongSejourValide();
        break;
      case 'CRA_10_ANS_LIEN_FRANCE':
        request.presenceReguliereFranceMois = this.presenceReguliereFranceMois();
        request.conjointFrancais = this.conjointFrancais();
        request.parentEnfantFrancais = this.parentEnfantFrancais();
        break;
      case 'CRA_10_ANS_RESIDENT_ANCIEN':
        request.neEnFrance = this.neEnFrance();
        request.arriveeAvant13Ans = this.arriveeAvant13Ans();
        break;
      case 'CHANGEMENT_VERS_TRAVAILLEUR':
        request.contratTravailValide = this.contratTravailValide();
        break;
      case 'REGROUPEMENT_FAMILIAL_ACCORD_1968':
        request.ressourcesSuffisantes = this.ressourcesSuffisantes();
        request.logementDecent = this.logementDecent();
        request.nombrePersonnesFoyer = this.nombrePersonnesFoyer();
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
        this.snackBar.open('Régime algérien analysé', 'OK', { duration: 2500 });
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
        const voie = mapVoieFromIa(r.voieDemande);
        if (voie) this.voieDemande.set(voie);
        // Nationalité = forcément true puisque l'analyse a abouti côté backend.
        this.nationaliteAlgerienne.set(true);
        // Provenance reset — valeurs persistées = saisie avocat.
        this.provenanceVoie.set(null);
        this.provenanceNationalite.set(null);
        this.provenancePresenceReguliere.set(null);
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
