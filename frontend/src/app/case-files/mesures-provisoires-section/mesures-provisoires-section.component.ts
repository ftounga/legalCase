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
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';

import { MesuresProvisoiresService } from '../../core/services/mesures-provisoires.service';
import {
  EnfantMineurInfo,
  LOGEMENT_PROPRIETAIRE_OPTIONS,
  LogementProprietaire,
  MesuresProvisoiresAiData,
  MesuresProvisoiresRequest,
  MesuresProvisoiresResponse,
  ResidenceEnfantsRecommande,
  SOUHAIT_RESIDENCE_OPTIONS,
  SouhaitResidenceEnfants,
  VerdictAcceptabilite,
  attributionLogementLabel,
  residenceEnfantsLabel,
  verdictAcceptabiliteLabel,
} from '../../core/models/mesures-provisoires.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { AiQuestion } from '../../core/models/ai-question.model';
import { PieceManquanteEntry } from '../../core/models/case-analysis.model';
import { CoherencePopoverTriggerDirective } from '../../shared/coherence-popover/coherence-popover-trigger.directive';
import { CoherenceAlert } from '../../shared/coherence-popover/coherence-alert.model';
import { CoherenceAlertBuilder } from '../../shared/coherence-popover/coherence-alert-builder';

/**
 * SF-FA-12-02 : champs de form audités par F-IA-03 pour l'outil F-FA-12.
 */
export type MPAlertField =
  | 'REVENUS_DEMANDEUR'
  | 'REVENUS_DEFENDEUR'
  | 'DATE_AUDIENCE'
  | 'VIOLENCES';

export type MPCoherenceAlert = CoherenceAlert<MPAlertField>;

/** SF-FA-12-02 : seuil divergence revenus (10 %, aligné F-FA-08/F-DT-09). */
const REVENUS_DIVERGENCE_RATIO = 0.10;

/** Regex ISO YYYY-MM-DD. */
const ISO_DATE_REGEX = /^\d{4}-\d{2}-\d{2}$/;

/**
 * SF-FA-12-02 : composant outil décisionnel "Mesures provisoires"
 * (art. 254-256 Cciv, F-FA-12). Single-country FR — BE = F-FA-11.
 *
 * Pattern emprunté au canonique `harcelement-licenciement-nul-section`
 * (palette navy / or — pas de rouge dominant) + miroir `divorce-alteration-section`
 * (pré-fill IA + alertes cohérence multi-sources via `CoherenceAlertBuilder`).
 *
 * Affichage : bannière verdict ELEVEE/MOYENNE/FAIBLE, score 0-100, cartes
 * recommandations (logement, résidence enfants, pension alimentaire,
 * contribution charges, mesure conservatoire), messages, baseJuridique
 * + formule en JetBrains Mono.
 */
@Component({
  selector: 'app-mesures-provisoires-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule, DecimalPipe,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatSlideToggleModule, MatProgressSpinnerModule,
    LegalCitationsPipe,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './mesures-provisoires-section.component.html',
  styleUrl: './mesures-provisoires-section.component.scss',
})
export class MesuresProvisoiresSectionComponent implements OnInit, OnChanges {
  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'MESURES PROVISOIRES (FR) — ART. 254 CCIV';
  static readonly TOOL_ICON = 'gavel';

  @Input() caseFileId!: string;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';
  /** SF-FA-12-02 : pré-fill IA Famille — facultatif, no-op si absent. */
  @Input() aiData?: MesuresProvisoiresAiData | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;

  // Snapshots signal pour réactivité computed.
  private aiDataSignal = signal<MesuresProvisoiresAiData | null | undefined>(undefined);
  private procedureChecksSignal = signal<ProcedureCheck[]>([]);
  private aiQuestionsSignal = signal<AiQuestion[]>([]);
  private piecesManquantesSignal = signal<PieceManquanteEntry[]>([]);

  // F-177 SF-177-03b : force l'expansion (mode modal F-177).
  @Input() forceExpanded = false;

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<MesuresProvisoiresResponse | null>(null);

  // Form signals
  dateAudienceAOMP = signal<string | null>(null);
  revenusEpouxDemandeurEur = signal<number | null>(null);
  revenusEpouxDefendeurEur = signal<number | null>(null);
  logementCommunDescription = signal<string>('');
  logementProprietaire = signal<LogementProprietaire | null>(null);
  enfantsMineurs = signal<EnfantMineurInfo[]>([]);
  souhaitResidenceEnfants = signal<SouhaitResidenceEnfants>('ALTERNEE');
  violencesAlleguees = signal<boolean>(false);
  patrimoineCommunIsignificatif = signal<boolean>(false);
  demandeMesureConservatoire = signal<boolean>(false);

  // Provenance IA par champ pré-remplissable.
  provenanceDateAudience = signal<'IA' | null>(null);
  provenanceRevenusDemandeur = signal<'IA' | null>(null);
  provenanceRevenusDefendeur = signal<'IA' | null>(null);
  provenanceViolences = signal<'IA' | null>(null);
  provenancePatrimoine = signal<'IA' | null>(null);
  provenanceLogementProprietaire = signal<'IA' | null>(null);

  readonly logementOptions = LOGEMENT_PROPRIETAIRE_OPTIONS;
  readonly residenceOptions = SOUHAIT_RESIDENCE_OPTIONS;

  /** SF-FA-12-02 : alertes cohérence (gate showForm pour éviter affichage post-result). */
  coherenceAlerts = computed<Partial<Record<MPAlertField, MPCoherenceAlert>>>(() => {
    if (!this.showForm()) return {};
    const alerts: Partial<Record<MPAlertField, MPCoherenceAlert>> = {};
    const dem = this.buildRevenusAlert('REVENUS_DEMANDEUR');
    if (dem) alerts.REVENUS_DEMANDEUR = dem;
    const def = this.buildRevenusAlert('REVENUS_DEFENDEUR');
    if (def) alerts.REVENUS_DEFENDEUR = def;
    const date = this.buildDateAudienceAlert();
    if (date) alerts.DATE_AUDIENCE = date;
    const vio = this.buildViolencesAlert();
    if (vio) alerts.VIOLENCES = vio;
    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    return { total: values.length, blockers: 0 };
  });

  isFranceGate = computed(() => this.workspaceCountry === 'FRANCE');

  constructor(
    private service: MesuresProvisoiresService,
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
    if (this.isFranceGate()) {
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

    // Ré-applique le pré-fill quand `aiData` change après mount, sauf si
    // l'avocat a déjà rempli le form ou si un résultat est persisté.
    if (changes['aiData'] && !changes['aiData'].firstChange
        && this.showForm() && !this.result()) {
      this.prefillFromAi();
    }
  }

  toggleCollapse(): void {
    this.collapsed.update((v) => !v);
  }

  formValid(): boolean {
    if (!this.isFranceGate()) return false;
    if (!this.dateAudienceAOMP()) return false;
    const rd = this.revenusEpouxDemandeurEur();
    const rf = this.revenusEpouxDefendeurEur();
    if (rd === null || rd < 0) return false;
    if (rf === null || rf < 0) return false;
    if (!this.logementProprietaire()) return false;
    if (!this.souhaitResidenceEnfants()) return false;
    return true;
  }

  // ---------------------------------------------------------------------------
  // Form handlers (chacun efface la provenance IA en cas d'edit manuel)
  // ---------------------------------------------------------------------------

  onDateAudienceChange(value: string | null): void {
    this.dateAudienceAOMP.set(value && value.length > 0 ? value : null);
    this.provenanceDateAudience.set(null);
  }

  onRevenusDemandeurChange(value: number | null): void {
    this.revenusEpouxDemandeurEur.set(value);
    this.provenanceRevenusDemandeur.set(null);
  }

  onRevenusDefendeurChange(value: number | null): void {
    this.revenusEpouxDefendeurEur.set(value);
    this.provenanceRevenusDefendeur.set(null);
  }

  onLogementProprietaireChange(value: LogementProprietaire | null): void {
    this.logementProprietaire.set(value);
    this.provenanceLogementProprietaire.set(null);
  }

  onLogementDescriptionChange(value: string): void {
    this.logementCommunDescription.set(value ?? '');
  }

  onSouhaitResidenceChange(value: SouhaitResidenceEnfants): void {
    this.souhaitResidenceEnfants.set(value);
  }

  onViolencesChange(value: boolean): void {
    this.violencesAlleguees.set(!!value);
    this.provenanceViolences.set(null);
  }

  onPatrimoineChange(value: boolean): void {
    this.patrimoineCommunIsignificatif.set(!!value);
    this.provenancePatrimoine.set(null);
  }

  onMesureConservatoireChange(value: boolean): void {
    this.demandeMesureConservatoire.set(!!value);
  }

  addEnfant(): void {
    this.enfantsMineurs.update((arr) => [...arr, { prenom: '', age: 0 }]);
  }

  removeEnfant(index: number): void {
    this.enfantsMineurs.update((arr) => arr.filter((_, i) => i !== index));
  }

  updateEnfantPrenom(index: number, prenom: string): void {
    this.enfantsMineurs.update((arr) => arr.map((e, i) =>
      i === index ? { ...e, prenom: prenom ?? '' } : e));
  }

  updateEnfantAge(index: number, age: number | null): void {
    this.enfantsMineurs.update((arr) => arr.map((e, i) =>
      i === index ? { ...e, age: age === null || isNaN(age) ? 0 : age } : e));
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // ---------------------------------------------------------------------------
  // Pré-fill IA
  // ---------------------------------------------------------------------------

  /**
   * SF-FA-12-02 : pré-remplit le form depuis `aiData`. N'écrase jamais une
   * saisie avocat existante (provenance IA OK ou champ vide). No-op si `aiData`
   * absent ou si l'on est en mode résultat.
   */
  private prefillFromAi(): void {
    const ai = this.aiDataSignal();
    if (!ai) return;

    // 1. dateAudienceAOMP
    if (typeof ai.dateAudienceAOMP === 'string'
        && ai.dateAudienceAOMP.length > 0
        && ISO_DATE_REGEX.test(ai.dateAudienceAOMP)) {
      if (this.dateAudienceAOMP() === null
          || this.provenanceDateAudience() === 'IA') {
        this.dateAudienceAOMP.set(ai.dateAudienceAOMP);
        this.provenanceDateAudience.set('IA');
      }
    }

    // 2. revenus demandeur (mensuel direct ou annuel /12)
    const revD = this.resolveRevenusDemandeur(ai);
    if (typeof revD === 'number' && revD >= 0) {
      if (this.revenusEpouxDemandeurEur() === null
          || this.provenanceRevenusDemandeur() === 'IA') {
        this.revenusEpouxDemandeurEur.set(revD);
        this.provenanceRevenusDemandeur.set('IA');
      }
    }

    // 3. revenus défendeur (mensuel direct ou annuel /12)
    const revF = this.resolveRevenusDefendeur(ai);
    if (typeof revF === 'number' && revF >= 0) {
      if (this.revenusEpouxDefendeurEur() === null
          || this.provenanceRevenusDefendeur() === 'IA') {
        this.revenusEpouxDefendeurEur.set(revF);
        this.provenanceRevenusDefendeur.set('IA');
      }
    }

    // 4. violencesAlleguees (boolean)
    if (typeof ai.violencesAlleguees === 'boolean') {
      if (this.provenanceViolences() === 'IA' || !this.violencesAlleguees()) {
        this.violencesAlleguees.set(ai.violencesAlleguees);
        this.provenanceViolences.set('IA');
      }
    }

    // 5. patrimoineCommunSignificatif (boolean — préfixé `Significatif` côté IA)
    if (typeof ai.patrimoineCommunSignificatif === 'boolean') {
      if (this.provenancePatrimoine() === 'IA' || !this.patrimoineCommunIsignificatif()) {
        this.patrimoineCommunIsignificatif.set(ai.patrimoineCommunSignificatif);
        this.provenancePatrimoine.set('IA');
      }
    }
  }

  /**
   * SF-FA-12-02 : rétro-compatibilité — l'IA peut fournir mensuel ou annuel.
   */
  private resolveRevenusDemandeur(ai: MesuresProvisoiresAiData): number | null {
    if (typeof ai.revenusEpouxDemandeurEur === 'number') return ai.revenusEpouxDemandeurEur;
    if (typeof ai.revenusAnnuelsEpoux1Eur === 'number' && ai.revenusAnnuelsEpoux1Eur >= 0) {
      return Math.round(ai.revenusAnnuelsEpoux1Eur / 12);
    }
    return null;
  }

  private resolveRevenusDefendeur(ai: MesuresProvisoiresAiData): number | null {
    if (typeof ai.revenusEpouxDefendeurEur === 'number') return ai.revenusEpouxDefendeurEur;
    if (typeof ai.revenusAnnuelsEpoux2Eur === 'number' && ai.revenusAnnuelsEpoux2Eur >= 0) {
      return Math.round(ai.revenusAnnuelsEpoux2Eur / 12);
    }
    return null;
  }

  // ---------------------------------------------------------------------------
  // Coherence alerts builders (CoherenceAlertBuilder partagé — SF-155-05/10)
  // ---------------------------------------------------------------------------

  /**
   * SF-FA-12-02 : revenus (demandeur/défendeur) — écart relatif > 10 % IA vs user.
   * Multi-sources : F96 + QUESTION_IA + IA + PIECE_MANQUANTE (avis imposition).
   */
  private buildRevenusAlert(field: 'REVENUS_DEMANDEUR' | 'REVENUS_DEFENDEUR'): MPCoherenceAlert | null {
    if (!this.isFranceGate()) return null;
    const userValue = field === 'REVENUS_DEMANDEUR'
      ? this.revenusEpouxDemandeurEur()
      : this.revenusEpouxDefendeurEur();
    if (typeof userValue !== 'number' || userValue <= 0) return null;

    const ai = this.aiDataSignal();
    const aiValue = ai
      ? (field === 'REVENUS_DEMANDEUR'
          ? this.resolveRevenusDemandeur(ai)
          : this.resolveRevenusDefendeur(ai))
      : null;

    const critereCode = field === 'REVENUS_DEMANDEUR'
      ? 'FA12_REVENUS_DEMANDEUR'
      : 'FA12_REVENUS_DEFENDEUR';
    const pieceCodes = field === 'REVENUS_DEMANDEUR'
      ? ['FA12_REVENUS_DEMANDEUR', 'AVIS_IMPOSITION_DEMANDEUR']
      : ['FA12_REVENUS_DEFENDEUR', 'AVIS_IMPOSITION_DEFENDEUR'];
    const subject = field === 'REVENUS_DEMANDEUR' ? 'demandeur' : 'défendeur';

    const builder = CoherenceAlertBuilder.forField<MPAlertField>(field);

    // 1. F-96 (premier hit fixe l'expectedDisplay).
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== critereCode) continue;
      const ev = this.parseNumber(chk.expectedValue);
      if (ev === null || ev <= 0) continue;
      const ratio = Math.abs(userValue - ev) / ev;
      if (ratio <= REVENUS_DIVERGENCE_RATIO) continue;
      builder.addSource('F96', {
        expectedDisplay: this.formatEuros(ev),
        reason: `Checklist procédurale : revenus ${subject} ${this.formatEuros(ev)}`
          + (chk.raison ? ' (' + chk.raison + ')' : ''),
      });
      break;
    }

    // 2. QUESTION_IA "oui" + expectedValue.
    for (const q of this.aiQuestionsSignal()) {
      if (q.critereCode?.toUpperCase() !== critereCode) continue;
      const answer = q.answerText?.trim().toLowerCase();
      if (!answer) continue;
      const isOui = answer === 'oui' || answer.startsWith('oui ')
        || answer.startsWith('oui,') || answer.startsWith('oui.');
      if (!isOui) continue;
      const ev = this.parseNumber(q.expectedValue);
      if (ev === null || ev <= 0) continue;
      const ratio = Math.abs(userValue - ev) / ev;
      if (ratio <= REVENUS_DIVERGENCE_RATIO) continue;
      builder.addSource('QUESTION_IA', {
        expectedDisplay: this.formatEuros(ev),
        reason: `Question complémentaire : "${q.questionText}" → "${q.answerText}"`,
      });
      break;
    }

    // 3. IA aiData.
    if (typeof aiValue === 'number' && aiValue > 0) {
      const ratio = Math.abs(userValue - aiValue) / aiValue;
      if (ratio > REVENUS_DIVERGENCE_RATIO) {
        builder.addSource('IA', {
          expectedDisplay: this.formatEuros(aiValue),
          reason: `Analyse du dossier : revenus ${subject} ${this.formatEuros(aiValue)}`,
        });
      }
    }

    // 4. PIECE_MANQUANTE.
    const piece = this.findPieceManquante(pieceCodes);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  /**
   * SF-FA-12-02 : date d'audience — divergence stricte sur la chaîne ISO.
   * Multi-sources : IA + F96 + PIECE_MANQUANTE (convocation JAF).
   */
  private buildDateAudienceAlert(): MPCoherenceAlert | null {
    if (!this.isFranceGate()) return null;
    const user = this.dateAudienceAOMP();
    if (!user) return null;

    const ai = this.aiDataSignal();
    const aiDate = ai?.dateAudienceAOMP;
    const aiValid = typeof aiDate === 'string' && ISO_DATE_REGEX.test(aiDate);

    const builder = CoherenceAlertBuilder.forField<MPAlertField>('DATE_AUDIENCE');

    // 1. F-96
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'FA12_DATE_AUDIENCE') continue;
      const expected = chk.expectedValue?.trim();
      if (!expected) continue;
      if (expected === user) continue;
      builder.addSource('F96', {
        expectedDisplay: expected,
        reason: `Checklist procédurale : date audience attendue ${expected}`
          + (chk.raison ? ' (' + chk.raison + ')' : ''),
      });
      break;
    }

    // 2. IA
    if (aiValid && aiDate !== user) {
      builder.addSource('IA', {
        expectedDisplay: aiDate!,
        reason: `Analyse du dossier : audience AOMP fixée le ${aiDate}`,
      });
    }

    // 3. PIECE_MANQUANTE (convocation JAF).
    const piece = this.findPieceManquante(['FA12_DATE_AUDIENCE', 'CONVOCATION_JAF']);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  /**
   * SF-FA-12-02 : violences alléguées — divergence boolean IA vs user.
   * Sévérité CRITICAL si l'IA a détecté des violences mais l'avocat ne l'a
   * pas indiqué (impact sur attribution logement + ordonnance protection).
   */
  private buildViolencesAlert(): MPCoherenceAlert | null {
    if (!this.isFranceGate()) return null;
    const user = this.violencesAlleguees();
    const ai = this.aiDataSignal();
    const aiVal = ai?.violencesAlleguees;
    if (typeof aiVal !== 'boolean') return null;
    if (aiVal === user) return null;

    const display = aiVal ? 'Oui' : 'Non';
    const severity = aiVal && !user ? 'CRITICAL' : 'WARNING';

    const builder = CoherenceAlertBuilder.forField<MPAlertField>('VIOLENCES')
      .withSeverity(severity)
      .addSource('IA', {
        expectedDisplay: display,
        reason: aiVal
          ? `Analyse du dossier : violences alléguées détectées (impact attribution logement + art. 515-9 ordonnance protection)`
          : `Analyse du dossier : aucune violence alléguée détectée`,
      });

    // F-96
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'FA12_VIOLENCES') continue;
      const ev = chk.expectedValue?.trim().toLowerCase();
      if (!ev) continue;
      const expectedBool = ev === 'oui' || ev === 'true';
      if (expectedBool === user) continue;
      builder.addSource('F96', {
        expectedDisplay: expectedBool ? 'Oui' : 'Non',
        reason: `Checklist procédurale : violences ${expectedBool ? 'alléguées' : 'absentes'}`
          + (chk.raison ? ' (' + chk.raison + ')' : ''),
      });
      break;
    }

    // PIECE_MANQUANTE (mains courantes, constats huissier, certificats médicaux).
    const piece = this.findPieceManquante([
      'FA12_VIOLENCES',
      'MAIN_COURANTE',
      'CONSTAT_HUISSIER',
      'CERTIFICAT_MEDICAL',
    ]);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  // ---------------------------------------------------------------------------
  // Utilitaires
  // ---------------------------------------------------------------------------

  private parseNumber(raw: string | null | undefined): number | null {
    if (raw === null || raw === undefined || raw === '') return null;
    const n = Number(raw);
    return Number.isFinite(n) ? n : null;
  }

  private formatEuros(value: number): string {
    return `${Math.round(value).toLocaleString('fr-FR')} €`;
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

  alertTooltip(alert: MPCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: MPCoherenceAlert): string {
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
  // Display helpers
  // ---------------------------------------------------------------------------

  verdictAcceptabiliteLabel = verdictAcceptabiliteLabel;
  attributionLogementLabel = attributionLogementLabel;
  residenceEnfantsLabel = residenceEnfantsLabel;

  verdictBannerClass(v: VerdictAcceptabilite): string {
    switch (v) {
      case 'ELEVEE': return 'mp-verdict-banner mp-verdict-banner--strong';
      case 'MOYENNE': return 'mp-verdict-banner mp-verdict-banner--medium';
      case 'FAIBLE': return 'mp-verdict-banner mp-verdict-banner--weak';
    }
  }

  residenceCardClass(r: ResidenceEnfantsRecommande): string {
    switch (r) {
      case 'ALTERNEE': return 'mp-card mp-card--ok';
      case 'EXCLUSIVE_DEMANDEUR':
      case 'EXCLUSIVE_DEFENDEUR': return 'mp-card mp-card--neutral';
      case 'SANS_OBJET': return 'mp-card mp-card--muted';
    }
  }

  // ---------------------------------------------------------------------------
  // HTTP
  // ---------------------------------------------------------------------------

  calculate(): void {
    if (!this.formValid()) return;
    const request: MesuresProvisoiresRequest = {
      dateAudienceAOMP: this.dateAudienceAOMP()!,
      revenusEpouxDemandeurEur: this.revenusEpouxDemandeurEur()!,
      revenusEpouxDefendeurEur: this.revenusEpouxDefendeurEur()!,
      logementCommunDescription: this.logementCommunDescription() || null,
      logementProprietaire: this.logementProprietaire()!,
      enfantsMineurs: this.enfantsMineurs()
        .filter((e) => e.prenom.trim().length > 0 || e.age >= 0)
        .map((e) => ({ prenom: e.prenom.trim(), age: e.age })),
      souhaitResidenceEnfants: this.souhaitResidenceEnfants(),
      violencesAlleguees: this.violencesAlleguees(),
      patrimoineCommunIsignificatif: this.patrimoineCommunIsignificatif(),
      demandeMesureConservatoire: this.demandeMesureConservatoire(),
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.applyResult(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Mesures provisoires calculées', 'OK', { duration: 2500 });
        this.dashboardRefresh?.triggerRefresh();
      },
      error: (err) => {
        this.calculating.set(false);
        const msg = err?.error?.message || err?.error || 'Erreur lors du calcul';
        this.snackBar.open(String(msg), 'Fermer',
          { duration: 5000, panelClass: 'snack-error' });
      },
    });
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.applyResult(r);
        // Valeurs persistées = saisie avocat — pas de badge IA.
        this.provenanceDateAudience.set(null);
        this.provenanceRevenusDemandeur.set(null);
        this.provenanceRevenusDefendeur.set(null);
        this.provenanceViolences.set(null);
        this.provenancePatrimoine.set(null);
        this.provenanceLogementProprietaire.set(null);
        this.showForm.set(false);
        this.loading.set(false);
      },
      error: () => {
        // 404 attendu si pas d'analyse — tente le pré-fill IA.
        this.prefillFromAi();
        this.loading.set(false);
      },
    });
  }

  private applyResult(r: MesuresProvisoiresResponse): void {
    this.result.set(r);
    this.dateAudienceAOMP.set(r.dateAudienceAOMP);
    this.revenusEpouxDemandeurEur.set(r.revenusEpouxDemandeurEur);
    this.revenusEpouxDefendeurEur.set(r.revenusEpouxDefendeurEur);
    this.logementCommunDescription.set(r.logementCommunDescription ?? '');
    this.logementProprietaire.set(r.logementProprietaire);
    this.enfantsMineurs.set([...(r.enfantsMineurs ?? [])]);
    this.souhaitResidenceEnfants.set(r.souhaitResidenceEnfants);
    this.violencesAlleguees.set(r.violencesAlleguees);
    this.patrimoineCommunIsignificatif.set(r.patrimoineCommunIsignificatif);
    this.demandeMesureConservatoire.set(r.demandeMesureConservatoire);
  }
}
