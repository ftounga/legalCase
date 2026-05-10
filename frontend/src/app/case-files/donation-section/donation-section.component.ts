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
import { MatRadioModule } from '@angular/material/radio';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { DonationService } from '../../core/services/donation.service';
import {
  CodeRisqueRequalification,
  CODE_RISQUE_REQUALIFICATION_LABELS,
  DonationRequest,
  DonationResponse,
  FormeDonation,
  FORME_DONATION_LABELS,
  RisqueRequalification,
  VerdictValiditeDonation,
  VERDICT_VALIDITE_DONATION_LABELS,
} from '../../core/models/donation.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { PieceManquanteEntry } from '../../core/models/case-analysis.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { AiQuestion } from '../../core/models/ai-question.model';
import { CoherencePopoverTriggerDirective } from '../../shared/coherence-popover/coherence-popover-trigger.directive';
import {
  CoherenceAlert,
  CoherenceAlertSource,
} from '../../shared/coherence-popover/coherence-alert.model';
import { CoherenceAlertBuilder } from '../../shared/coherence-popover/coherence-alert-builder';
import { DonationPrefillRules } from './donation-section-prefill-rules';

/**
 * SF-FA-24-06 : champs d'alerte F-IA-03 exposés par l'outil "Validité donation".
 * - FORME : divergence sur la forme de la donation détectée par l'IA.
 * - SAINE_ESPRIT : divergence sur la capacité du donateur (art. 902 Cciv).
 * - RESPECT_QUOTITE : divergence sur le respect de la quotité disponible
 *   (art. 913+) — déclenche l'action en réduction (art. 920+).
 */
export type DonationAlertField =
  | 'FORME'
  | 'SAINE_ESPRIT'
  | 'RESPECT_QUOTITE';

export type DonationAlertSource = CoherenceAlertSource;
export type DonationCoherenceAlert = CoherenceAlert<DonationAlertField>;

/**
 * SF-FA-24-06 : outil décisionnel "Validité donation entre vifs" (FR — art.
 * 893-958, 902-906, 920+, 931, 953-958 Cciv : 4 formes + capacité + vices
 * consentement + formalisme + quotité disponible + révocation).
 *
 * FR uniquement (bannière info si dossier BE — équivalent CC BE art. 893+
 * traité dans la feature jumelle backlog F-FA-24-BE-donation).
 *
 * Consomme l'API SF-FA-24-05 (mergée PR #671). Affiché conditionnellement
 * par le panel F-IA-04 (tool_id 'F-FA-24-donation' — migration 184).
 *
 * Pattern de référence : `testament-validite-section` (PR #665, F-FA-24 jumeau).
 * Helper partagé : `CoherenceAlertBuilder` + `CoherenceAlert<F>` (SF-155-05).
 */
@Component({
  selector: 'app-donation-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule, DecimalPipe,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatRadioModule, MatCheckboxModule,
    MatProgressSpinnerModule,
    LegalCitationsPipe,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './donation-section.component.html',
  styleUrl: './donation-section.component.scss',
})
export class DonationSectionComponent implements OnInit, OnChanges {
  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'VALIDITÉ DONATION ENTRE VIFS (FR)';
  static readonly TOOL_ICON = 'redeem';

  /** F-236 SF-236-02 — compteur miroir prefillFromAi via helper. */
  static getPrefillCount(input: {
    aiData?: FamilleExtractedData | null;
    procedureChecks?: unknown[];
    aiQuestions?: unknown[];
    piecesManquantes?: unknown[];
    triggerEvents?: unknown[];
    workspaceCountry?: string;
  }): number {
    return DonationPrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';
  @Input() aiData?: FamilleExtractedData | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;

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
  result = signal<DonationResponse | null>(null);

  // Form fields — communs.
  formeDonation = signal<FormeDonation | null>(null);
  dateDonation = signal<string | null>(null);
  ageDonateurAns = signal<number | null>(null);
  saineDEsprit = signal<boolean | null>(null);
  capaciteDonateur = signal<boolean | null>(null);
  capaciteRecipiendaire = signal<boolean | null>(null);
  consentementLibre = signal<boolean | null>(null);
  objetDetermine = signal<boolean | null>(null);
  respectFormalisme = signal<boolean | null>(null);
  respectQuotiteDisponible = signal<boolean | null>(null);

  // Notariée
  acteAuthentique = signal<boolean | null>(null);
  acceptationExpresse = signal<boolean | null>(null);

  // Manuelle
  remiseEffective = signal<boolean | null>(null);
  bienMeuble = signal<boolean | null>(null);

  // Don indirect
  intentionLiberale = signal<boolean | null>(null);
  actePrincipalNeutre = signal<boolean | null>(null);

  // Donation déguisée
  apparenceOnerueuse = signal<boolean | null>(null);
  prixIncoherent = signal<boolean | null>(null);

  // Vices consentement.
  vicesConsentementDol = signal<boolean>(false);
  erreurSubstantielle = signal<boolean>(false);

  // Révocation post-formation.
  ingratitudeAvere = signal<boolean>(false);
  inexecutionCharge = signal<boolean>(false);

  /** Provenance IA des champs pré-remplis. */
  provenanceForme = signal<'IA' | null>(null);
  provenanceDateDonation = signal<'IA' | null>(null);
  provenanceSaineEsprit = signal<'IA' | null>(null);
  provenanceRespectQuotite = signal<'IA' | null>(null);

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  isNotariee = computed<boolean>(() => this.formeDonation() === 'DONATION_NOTARIEE');
  isManuelle = computed<boolean>(() => this.formeDonation() === 'DONATION_MANUELLE');
  isIndirect = computed<boolean>(() => this.formeDonation() === 'DON_INDIRECT');
  isDeguisee = computed<boolean>(() => this.formeDonation() === 'DONATION_DEGUISEE');

  coherenceAlerts = computed<Partial<Record<DonationAlertField, DonationCoherenceAlert>>>(() => {
    if (!this.showForm()) return {};
    const alerts: Partial<Record<DonationAlertField, DonationCoherenceAlert>> = {};
    const forme = this.buildFormeAlert();
    if (forme) alerts.FORME = forme;
    const saine = this.buildSaineEspritAlert();
    if (saine) alerts.SAINE_ESPRIT = saine;
    const quotite = this.buildRespectQuotiteAlert();
    if (quotite) alerts.RESPECT_QUOTITE = quotite;
    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    const blockers = values.filter((a) => a.severity === 'CRITICAL').length;
    return { total: values.length, blockers };
  });

  readonly formeOptions: ReadonlyArray<{ code: FormeDonation; label: string }> = [
    { code: 'DONATION_NOTARIEE', label: FORME_DONATION_LABELS.DONATION_NOTARIEE },
    { code: 'DONATION_MANUELLE', label: FORME_DONATION_LABELS.DONATION_MANUELLE },
    { code: 'DON_INDIRECT', label: FORME_DONATION_LABELS.DON_INDIRECT },
    { code: 'DONATION_DEGUISEE', label: FORME_DONATION_LABELS.DONATION_DEGUISEE },
  ];

  constructor(
    private service: DonationService,
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

    if (changes['aiData'] && !changes['aiData'].firstChange
        && this.isFrance() && this.showForm() && !this.result()) {
      this.prefillFromAi();
    }
  }

  alertTooltip(alert: DonationCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: DonationCoherenceAlert): string {
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

  /** Divergence sur la forme de la donation. */
  private buildFormeAlert(): DonationCoherenceAlert | null {
    const userVal = this.formeDonation();
    if (userVal === null) return null;

    const builder = CoherenceAlertBuilder.forField<DonationAlertField>('FORME');

    // 1. F-96
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'DONATION_FORME') continue;
      const ev = chk.expectedValue;
      if (!ev) continue;
      const expected = this.parseFormeFromIa(ev);
      if (!expected || expected === userVal) continue;
      builder.addSource('F96', {
        expectedDisplay: this.formeLabel(expected),
        reason: `Checklist procédurale : ${this.formeLabel(expected)}`
          + (chk.raison ? ` (${chk.raison})` : ''),
      });
      break;
    }

    // 2. QUESTION_IA
    for (const q of this.aiQuestionsSignal()) {
      if (q.critereCode?.toUpperCase() !== 'DONATION_FORME') continue;
      const answer = q.answerText?.trim();
      if (!answer) continue;
      const expected = this.parseFormeFromIa(q.expectedValue ?? answer);
      if (!expected || expected === userVal) continue;
      builder.addSource('QUESTION_IA', {
        expectedDisplay: this.formeLabel(expected),
        reason: `Question complémentaire : "${q.questionText}" → "${q.answerText}"`,
      });
      break;
    }

    // 3. IA — `aiData.formeDonationDetectee`.
    const iaForme = this.parseFormeFromIa(this.aiDataSignal()?.formeDonationDetectee ?? null);
    if (iaForme && iaForme !== userVal) {
      builder.addSource('IA', {
        expectedDisplay: this.formeLabel(iaForme),
        reason: `Analyse du dossier : ${this.formeLabel(iaForme)}`,
      });
    }

    // 4. PIECE_MANQUANTE
    const piece = this.findPieceManquante(['DONATION_FORME', 'DONATION', 'ACTE_DONATION']);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  /** Divergence sur la capacité (art. 901-902 Cciv). */
  private buildSaineEspritAlert(): DonationCoherenceAlert | null {
    const userVal = this.saineDEsprit();
    if (userVal === null) return null;

    const builder = CoherenceAlertBuilder.forField<DonationAlertField>('SAINE_ESPRIT');

    // 1. F-96
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'DONATION_SAINE_ESPRIT') continue;
      const expected = this.parseBoolFromIa(chk.expectedValue);
      if (expected === null || expected === userVal) continue;
      builder.addSource('F96', {
        expectedDisplay: expected ? "Sain d'esprit" : "Insanité d'esprit",
        reason: `Checklist procédurale : ${expected ? "donateur sain d'esprit" : "insanité d'esprit détectée"}`
          + (chk.raison ? ` (${chk.raison})` : ''),
      });
      break;
    }

    // 2. QUESTION_IA
    for (const q of this.aiQuestionsSignal()) {
      if (q.critereCode?.toUpperCase() !== 'DONATION_SAINE_ESPRIT') continue;
      const answer = q.answerText?.trim().toLowerCase();
      if (!answer) continue;
      const expected = this.parseBoolFromIa(q.expectedValue);
      if (expected === null || expected === userVal) continue;
      builder.addSource('QUESTION_IA', {
        expectedDisplay: expected ? "Sain d'esprit" : "Insanité d'esprit",
        reason: `Question complémentaire : "${q.questionText}" → "${q.answerText}"`,
      });
      break;
    }

    // 3. IA — `aiData.saineDEspritDonateurDetected`.
    const iaSaine = this.aiDataSignal()?.saineDEspritDonateurDetected;
    if (iaSaine !== null && iaSaine !== undefined && iaSaine !== userVal) {
      builder.addSource('IA', {
        expectedDisplay: iaSaine ? "Sain d'esprit" : "Insanité d'esprit",
        reason: `Analyse du dossier : ${iaSaine ? "donateur sain d'esprit" : "insanité d'esprit détectée"}`,
      });
    }

    // 4. PIECE_MANQUANTE
    const piece = this.findPieceManquante(['DONATION_SAINE_ESPRIT', 'CERTIFICAT_MEDICAL']);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  /** Divergence sur le respect de la quotité disponible. */
  private buildRespectQuotiteAlert(): DonationCoherenceAlert | null {
    const userVal = this.respectQuotiteDisponible();
    if (userVal === null) return null;

    const builder = CoherenceAlertBuilder.forField<DonationAlertField>('RESPECT_QUOTITE');

    // 1. F-96
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'DONATION_QUOTITE') continue;
      const expected = this.parseBoolFromIa(chk.expectedValue);
      if (expected === null || expected === userVal) continue;
      builder.addSource('F96', {
        expectedDisplay: expected ? 'Quotité respectée' : 'Excès de quotité disponible',
        reason: `Checklist procédurale : ${expected ? 'quotité respectée' : 'donation excède la quotité disponible'}`
          + (chk.raison ? ` (${chk.raison})` : ''),
      });
      break;
    }

    // 2. QUESTION_IA
    for (const q of this.aiQuestionsSignal()) {
      if (q.critereCode?.toUpperCase() !== 'DONATION_QUOTITE') continue;
      const expected = this.parseBoolFromIa(q.expectedValue);
      if (expected === null || expected === userVal) continue;
      builder.addSource('QUESTION_IA', {
        expectedDisplay: expected ? 'Quotité respectée' : 'Excès de quotité disponible',
        reason: `Question complémentaire : "${q.questionText}" → "${q.answerText}"`,
      });
      break;
    }

    // 3. IA — `aiData.respectQuotiteDisponibleDetected`.
    const iaQuotite = this.aiDataSignal()?.respectQuotiteDisponibleDetected;
    if (iaQuotite !== null && iaQuotite !== undefined && iaQuotite !== userVal) {
      builder.addSource('IA', {
        expectedDisplay: iaQuotite ? 'Quotité respectée' : 'Excès de quotité disponible',
        reason: `Analyse du dossier : ${iaQuotite ? 'quotité respectée' : 'donation excède la quotité disponible'}`,
      });
    }

    return builder.build();
  }

  private parseBoolFromIa(value: string | null | undefined): boolean | null {
    if (value === null || value === undefined) return null;
    const v = value.toString().trim().toLowerCase();
    if (!v) return null;
    if (v === 'oui' || v === 'true' || v === 'vrai' || v === '1' || v === 'yes') return true;
    if (v === 'non' || v === 'false' || v === 'faux' || v === '0' || v === 'no') return false;
    return null;
  }

  /**
   * Parse la forme de la donation depuis une valeur IA / F-96.
   * Accepte les valeurs canoniques (`DONATION_NOTARIEE` etc.) ou alias
   * courts (`NOTARIEE`, `MANUELLE`, `INDIRECT`/`INDIRECTE`, `DEGUISEE`).
   */
  private parseFormeFromIa(value: string | null | undefined): FormeDonation | null {
    if (!value) return null;
    const v = value.toString().trim().toUpperCase();
    if (!v) return null;
    if (v === 'DONATION_NOTARIEE' || v === 'NOTARIEE' || v === 'NOTARIE') return 'DONATION_NOTARIEE';
    if (v === 'DONATION_MANUELLE' || v === 'MANUELLE' || v === 'MANUEL') return 'DONATION_MANUELLE';
    if (v === 'DON_INDIRECT' || v === 'INDIRECT' || v === 'INDIRECTE') return 'DON_INDIRECT';
    if (v === 'DONATION_DEGUISEE' || v === 'DEGUISEE' || v === 'DEGUISE') return 'DONATION_DEGUISEE';
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

  toggleCollapse(): void {
    this.collapsed.update((v) => !v);
  }

  formValid(): boolean {
    if (this.formeDonation() === null) return false;
    if (!this.dateDonation()) return false;
    const age = this.ageDonateurAns();
    if (age === null || age === undefined || age < 0 || age > 130) return false;
    if (this.saineDEsprit() === null) return false;
    if (this.capaciteDonateur() === null) return false;
    if (this.capaciteRecipiendaire() === null) return false;
    if (this.consentementLibre() === null) return false;
    if (this.objetDetermine() === null) return false;
    if (this.respectFormalisme() === null) return false;
    if (this.respectQuotiteDisponible() === null) return false;

    // Champs conditionnels requis selon la forme.
    if (this.isNotariee()) {
      if (this.acteAuthentique() === null) return false;
      if (this.acceptationExpresse() === null) return false;
    }
    if (this.isManuelle()) {
      if (this.remiseEffective() === null) return false;
      if (this.bienMeuble() === null) return false;
    }
    if (this.isIndirect()) {
      if (this.intentionLiberale() === null) return false;
      if (this.actePrincipalNeutre() === null) return false;
    }
    if (this.isDeguisee()) {
      if (this.apparenceOnerueuse() === null) return false;
      if (this.prixIncoherent() === null) return false;
    }
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // Handlers — effacent provenance IA au 1er changement manuel.

  onFormeDonationChange(value: FormeDonation | null): void {
    this.formeDonation.set(value);
    this.provenanceForme.set(null);
    // Reset des champs spécifiques aux formes non sélectionnées.
    if (value !== 'DONATION_NOTARIEE') {
      this.acteAuthentique.set(null);
      this.acceptationExpresse.set(null);
    }
    if (value !== 'DONATION_MANUELLE') {
      this.remiseEffective.set(null);
      this.bienMeuble.set(null);
    }
    if (value !== 'DON_INDIRECT') {
      this.intentionLiberale.set(null);
      this.actePrincipalNeutre.set(null);
    }
    if (value !== 'DONATION_DEGUISEE') {
      this.apparenceOnerueuse.set(null);
      this.prixIncoherent.set(null);
    }
  }

  onDateDonationChange(value: string | null): void {
    this.dateDonation.set(value || null);
    this.provenanceDateDonation.set(null);
  }

  onAgeDonateurChange(value: number | null): void {
    this.ageDonateurAns.set(value === null || value === undefined ? null : value);
  }

  onSaineDEspritChange(value: boolean | null): void {
    this.saineDEsprit.set(value);
    this.provenanceSaineEsprit.set(null);
  }

  onCapaciteDonateurChange(value: boolean | null): void {
    this.capaciteDonateur.set(value);
  }

  onCapaciteRecipiendaireChange(value: boolean | null): void {
    this.capaciteRecipiendaire.set(value);
  }

  onConsentementLibreChange(value: boolean | null): void {
    this.consentementLibre.set(value);
  }

  onObjetDetermineChange(value: boolean | null): void {
    this.objetDetermine.set(value);
  }

  onRespectFormalismeChange(value: boolean | null): void {
    this.respectFormalisme.set(value);
  }

  onRespectQuotiteDisponibleChange(value: boolean | null): void {
    this.respectQuotiteDisponible.set(value);
    this.provenanceRespectQuotite.set(null);
  }

  onActeAuthentiqueChange(value: boolean | null): void {
    this.acteAuthentique.set(value);
  }

  onAcceptationExpresseChange(value: boolean | null): void {
    this.acceptationExpresse.set(value);
  }

  onRemiseEffectiveChange(value: boolean | null): void {
    this.remiseEffective.set(value);
  }

  onBienMeubleChange(value: boolean | null): void {
    this.bienMeuble.set(value);
  }

  onIntentionLiberaleChange(value: boolean | null): void {
    this.intentionLiberale.set(value);
  }

  onActePrincipalNeutreChange(value: boolean | null): void {
    this.actePrincipalNeutre.set(value);
  }

  onApparenceOnerueuseChange(value: boolean | null): void {
    this.apparenceOnerueuse.set(value);
  }

  onPrixIncoherentChange(value: boolean | null): void {
    this.prixIncoherent.set(value);
  }

  onVicesConsentementDolChange(value: boolean): void {
    this.vicesConsentementDol.set(value);
  }

  onErreurSubstantielleChange(value: boolean): void {
    this.erreurSubstantielle.set(value);
  }

  onIngratitudeAvereChange(value: boolean): void {
    this.ingratitudeAvere.set(value);
  }

  onInexecutionChargeChange(value: boolean): void {
    this.inexecutionCharge.set(value);
  }

  /**
   * Pré-fill depuis `aiData` (FamilleExtractedData).
   * Règles :
   * - silencieux si aiData absent
   * - ne pré-remplit que si le champ est encore vide ou marqué IA
   * - n'écrase jamais une saisie avocat (provenance !== 'IA')
   */
  private prefillFromAi(): void {
    // F-236 SF-236-02 — délégation au helper partagé.
    const h = { aiData: this.aiDataSignal() };
    const fm = DonationPrefillRules.computeFormeDonation(h);
    if (fm !== null && (this.formeDonation() === null || this.provenanceForme() === 'IA')) {
      this.formeDonation.set(fm);
      this.provenanceForme.set('IA');
    }
    const dt = DonationPrefillRules.computeDateDonation(h);
    if (dt !== null && (!this.dateDonation() || this.provenanceDateDonation() === 'IA')) {
      this.dateDonation.set(dt);
      this.provenanceDateDonation.set('IA');
    }
    const se = DonationPrefillRules.computeSaineDEsprit(h);
    if (se !== null && (this.saineDEsprit() === null || this.provenanceSaineEsprit() === 'IA')) {
      this.saineDEsprit.set(se);
      this.provenanceSaineEsprit.set('IA');
    }
    const rq = DonationPrefillRules.computeRespectQuotite(h);
    if (rq !== null && (this.respectQuotiteDisponible() === null || this.provenanceRespectQuotite() === 'IA')) {
      this.respectQuotiteDisponible.set(rq);
      this.provenanceRespectQuotite.set('IA');
    }
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request: DonationRequest = {
      formeDonation: this.formeDonation()!,
      dateDonation: this.dateDonation()!,
      ageDonateurAns: this.ageDonateurAns()!,
      saineDEsprit: this.saineDEsprit()!,
      capaciteDonateur: this.capaciteDonateur()!,
      capaciteRecipiendaire: this.capaciteRecipiendaire()!,
      consentementLibre: this.consentementLibre()!,
      objetDetermine: this.objetDetermine()!,
      respectFormalisme: this.respectFormalisme()!,
      respectQuotiteDisponible: this.respectQuotiteDisponible()!,
      acteAuthentique: this.acteAuthentique(),
      acceptationExpresse: this.acceptationExpresse(),
      remiseEffective: this.remiseEffective(),
      bienMeuble: this.bienMeuble(),
      intentionLiberale: this.intentionLiberale(),
      actePrincipalNeutre: this.actePrincipalNeutre(),
      apparenceOnerueuse: this.apparenceOnerueuse(),
      prixIncoherent: this.prixIncoherent(),
      vicesConsentementDol: this.vicesConsentementDol(),
      erreurSubstantielle: this.erreurSubstantielle(),
      ingratitudeAvere: this.ingratitudeAvere(),
      inexecutionCharge: this.inexecutionCharge(),
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Validité donation analysée', 'OK', { duration: 2500 });
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
        // Provenance reset — valeurs persistées = saisie avocat.
        this.provenanceForme.set(null);
        this.provenanceDateDonation.set(null);
        this.provenanceSaineEsprit.set(null);
        this.provenanceRespectQuotite.set(null);
        this.showForm.set(false);
        this.loading.set(false);
      },
      error: () => {
        // 404 attendu si aucune analyse — on reste en mode formulaire.
        this.prefillFromAi();
        this.loading.set(false);
      },
    });
  }

  /** Libellé humain de la forme. */
  formeLabel(code: FormeDonation | null | undefined): string {
    if (!code) return '';
    return FORME_DONATION_LABELS[code] ?? code;
  }

  /** Libellé humain du verdict. */
  verdictLabel(code: VerdictValiditeDonation | null | undefined): string {
    if (!code) return '';
    return VERDICT_VALIDITE_DONATION_LABELS[code] ?? code;
  }

  /** Libellé humain d'un code de risque de requalification. */
  risqueLabel(code: CodeRisqueRequalification): string {
    return CODE_RISQUE_REQUALIFICATION_LABELS[code] ?? code;
  }

  /**
   * Classe CSS du bandeau verdict :
   * - `NUL` → critical (rouge), seul cas réservé à la palette rouge.
   * - `CONTESTABLE` → warn (or).
   * - `VALIDE` → info (navy).
   */
  verdictBannerClass(code: VerdictValiditeDonation | null | undefined): string {
    if (!code) return 'donation-banner donation-banner--info';
    if (code === 'NUL') return 'donation-banner donation-banner--critical';
    if (code === 'CONTESTABLE') return 'donation-banner donation-banner--warn';
    return 'donation-banner donation-banner--info';
  }

  /** Classe CSS du chip header (header collapsé). */
  verdictChipClass(code: VerdictValiditeDonation | null | undefined): string {
    if (!code) return 'donation-chip donation-chip--info';
    if (code === 'NUL') return 'donation-chip donation-chip--critical';
    if (code === 'CONTESTABLE') return 'donation-chip donation-chip--warn';
    return 'donation-chip donation-chip--info';
  }

  /** Icône du bandeau verdict. */
  verdictIcon(code: VerdictValiditeDonation | null | undefined): string {
    if (code === 'NUL') return 'gpp_bad';
    if (code === 'CONTESTABLE') return 'warning';
    return 'verified';
  }

  /** TrackBy pour la liste des risques. */
  trackRisque(_i: number, r: RisqueRequalification): string {
    return r.code;
  }
}
