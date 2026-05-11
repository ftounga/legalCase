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
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ReserveHereditaireService } from '../../core/services/reserve-heriditaire.service';
import {
  BaremeReserveRow,
  QualiteDemandeurReserve,
  QUALITE_DEMANDEUR_RESERVE_LABELS,
  ReserveHereditaireRequest,
  ReserveHereditaireResponse,
  VerdictRecevabiliteReserve,
  VERDICT_RECEVABILITE_RESERVE_LABELS,
} from '../../core/models/reserve-heriditaire.model';
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
import { ReserveHeriditairePrefillRules } from './reserve-heriditaire-section-prefill-rules';

/**
 * SF-FA-24-08 : champs d'alerte F-IA-03 audités par l'outil
 * "Réserve héréditaire et action en réduction".
 * - CONJOINT_SURVIVANT : présence du conjoint (impacte barème art. 914-1).
 * - NOMBRE_ENFANTS : impacte directement la quotité disponible art. 913.
 * - MONTANT_SUCCESSION / MONTANT_LIBS : valeurs métier sensibles.
 * - QUALITE_DEMANDEUR : recevabilité de l'action (art. 920).
 */
export type ReserveHereditaireAlertField =
  | 'CONJOINT_SURVIVANT'
  | 'NOMBRE_ENFANTS'
  | 'MONTANT_SUCCESSION'
  | 'MONTANT_LIBS'
  | 'QUALITE_DEMANDEUR';

export type ReserveHereditaireAlertSource = CoherenceAlertSource;
export type ReserveHereditaireCoherenceAlert =
  CoherenceAlert<ReserveHereditaireAlertField>;

/**
 * SF-FA-24-08 : outil décisionnel "Réserve héréditaire et action en
 * réduction" (FR — art. 913 + 914-1 + 920-928 Cciv).
 *
 * FR uniquement (bannière info si dossier BE — équivalent CC BE traité dans
 * la feature jumelle backlog F-FA-24-BE, barème différent).
 *
 * Consomme l'API SF-FA-24-07 (mergée PR #672). Affiché conditionnellement
 * par le panel F-IA-04 (tool_id 'F-FA-24-reserve-heriditaire').
 *
 * Pattern de référence : `devolution-legale-section` (PR #658).
 * Helper partagé : `CoherenceAlertBuilder` + `CoherenceAlert<F>` (SF-155-05).
 */
@Component({
  selector: 'app-reserve-heriditaire-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule, DecimalPipe,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatRadioModule,
    MatProgressSpinnerModule,
    LegalCitationsPipe,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './reserve-heriditaire-section.component.html',
  styleUrl: './reserve-heriditaire-section.component.scss',
})
export class ReserveHeriditaireSectionComponent implements OnInit, OnChanges {
  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'RÉSERVE HÉRÉDITAIRE & ACTION EN RÉDUCTION (FR)';
  static readonly TOOL_ICON = 'balance';

  /** F-236 SF-236-02 — compteur miroir prefillFromAi via helper. */
  static getPrefillCount(input: {
    aiData?: FamilleExtractedData | null;
    procedureChecks?: unknown[];
    aiQuestions?: unknown[];
    piecesManquantes?: unknown[];
    triggerEvents?: unknown[];
    workspaceCountry?: string;
  }): number {
    return ReserveHeriditairePrefillRules.computePrefillCount(input);
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


  /**
   * F-163 SF-163-02c — Mode simulateur autonome (hors dossier client).
   *
   * Quand `true` :
   *  - Bannière « 🧪 Mode simulateur » affichée en haut.
   *  - `prefillFromAi()` court-circuité.
   *  - `coherenceAlerts` retourne `{}`.
   *  - `loadExisting()` court-circuité.
   *  - `calculate()` POSTe sur `/api/v1/simulators/F-FA-24-reserve-heriditaire/calculate`.
   *  - `triggerRefresh()` jamais invoqué.
   *
   * Default `false` — mode case-file scoped inchangé.
   */
  @Input() standaloneMode: boolean = false;
  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<ReserveHereditaireResponse | null>(null);

  // Form fields
  nombreEnfants = signal<number | null>(null);
  conjointSurvivant = signal<boolean | null>(null);
  montantSuccession = signal<number | null>(null);
  montantLibsTotal = signal<number | null>(null);
  dateOuvertureSuccession = signal<string | null>(null);
  qualiteDuDemandeur = signal<QualiteDemandeurReserve | null>(null);

  /** Provenance IA des champs pré-remplis. */
  provenanceNombreEnfants = signal<'IA' | null>(null);
  provenanceConjoint = signal<'IA' | null>(null);
  provenanceMontantSuccession = signal<'IA' | null>(null);
  provenanceMontantLibs = signal<'IA' | null>(null);
  provenanceDateOuverture = signal<'IA' | null>(null);
  provenanceQualiteDemandeur = signal<'IA' | null>(null);

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  /**
   * Tableau visuel du barème — 5 configurations (art. 913 + 914-1 Cciv).
   * Highlighted = ligne correspondant à la saisie courante (ou résultat).
   */
  baremeRows = computed<BaremeReserveRow[]>(() => {
    const ne = this.result()?.nombreEnfants ?? this.nombreEnfants();
    const cs = this.result()?.conjointSurvivant ?? this.conjointSurvivant();
    return [
      {
        configuration: '0 enfant + conjoint (art. 914-1)',
        quotitePct: 75,
        reservePct: 25,
        highlighted: ne === 0 && cs === true,
      },
      {
        configuration: '0 enfant sans conjoint',
        quotitePct: 100,
        reservePct: 0,
        highlighted: ne === 0 && cs === false,
      },
      {
        configuration: '1 enfant',
        quotitePct: 50,
        reservePct: 50,
        highlighted: ne === 1,
      },
      {
        configuration: '2 enfants',
        quotitePct: 33.33,
        reservePct: 66.67,
        highlighted: ne === 2,
      },
      {
        configuration: '3 enfants ou plus',
        quotitePct: 25,
        reservePct: 75,
        highlighted: ne !== null && ne !== undefined && ne >= 3,
      },
    ];
  });

  /** Pourcentage libs / quotité disponible (clampé à 100 % pour la barre). */
  libsRatioPct = computed<number>(() => {
    const r = this.result();
    if (!r || r.montantQuotiteDispoEur <= 0) return 0;
    const ratio = (r.montantLibsTotal / r.montantQuotiteDispoEur) * 100;
    return Math.min(150, Math.max(0, ratio));
  });

  /** Couleur de la barre : info < 100 %, alerte critique ≥ 100 % (excédent). */
  libsBarCritical = computed<boolean>(() => {
    const r = this.result();
    return !!r && r.excedentReductibleEur > 0;
  });

  /** Vrai si prescription < 12 mois (chip rouge). */
  prescriptionUrgent = computed<boolean>(() => {
    const r = this.result();
    return !!r && r.delaiPrescriptionRestantMois > 0 && r.delaiPrescriptionRestantMois < 12;
  });

  coherenceAlerts = computed<Partial<Record<ReserveHereditaireAlertField, ReserveHereditaireCoherenceAlert>>>(() => {
    // F-163 SF-163-02c : aucune source IA en standalone.
    if (this.standaloneMode) return {};
    if (!this.showForm()) return {};
    const alerts: Partial<Record<ReserveHereditaireAlertField, ReserveHereditaireCoherenceAlert>> = {};
    const c = this.buildConjointAlert();
    if (c) alerts.CONJOINT_SURVIVANT = c;
    const ne = this.buildNombreEnfantsAlert();
    if (ne) alerts.NOMBRE_ENFANTS = ne;
    const ms = this.buildMontantSuccessionAlert();
    if (ms) alerts.MONTANT_SUCCESSION = ms;
    const ml = this.buildMontantLibsAlert();
    if (ml) alerts.MONTANT_LIBS = ml;
    const q = this.buildQualiteDemandeurAlert();
    if (q) alerts.QUALITE_DEMANDEUR = q;
    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    const blockers = values.filter((a) => a.severity === 'CRITICAL').length;
    return { total: values.length, blockers };
  });

  readonly qualiteDemandeurOptions: ReadonlyArray<{
    code: QualiteDemandeurReserve;
    label: string;
  }> = (Object.entries(QUALITE_DEMANDEUR_RESERVE_LABELS) as
    Array<[QualiteDemandeurReserve, string]>
  ).map(([code, label]) => ({ code, label }));

  constructor(
    private service: ReserveHereditaireService,
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
      // F-163 SF-163-02c : pas de dossier à interroger en standalone.
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

    if (changes['aiData'] && !changes['aiData'].firstChange
        && this.isFrance() && this.showForm() && !this.result() && !this.standaloneMode) {
      this.prefillFromAi();
    }
  }

  alertTooltip(alert: ReserveHereditaireCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: ReserveHereditaireCoherenceAlert): string {
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

  // ============================================================
  // F-IA-03 builders (CONJOINT, ENFANTS, MONTANT, QUALITE)
  // ============================================================

  private buildConjointAlert(): ReserveHereditaireCoherenceAlert | null {
    const userVal = this.conjointSurvivant();
    if (userVal === null) return null;

    const builder = CoherenceAlertBuilder
      .forField<ReserveHereditaireAlertField>('CONJOINT_SURVIVANT');

    // 1. F-96
    for (const chk of this.procedureChecksSignal()) {
      const code = chk.critereCode?.toUpperCase();
      if (code !== 'RESERVE_CONJOINT_SURVIVANT' && code !== 'CONJOINT_SURVIVANT') continue;
      const expected = this.parseBoolFromIa(chk.expectedValue);
      if (expected === null || expected === userVal) continue;
      builder.addSource('F96', {
        expectedDisplay: expected ? 'Conjoint survivant' : 'Pas de conjoint',
        reason: `Checklist procédurale : ${expected ? 'conjoint survivant détecté' : 'aucun conjoint survivant'}`
          + (chk.raison ? ` (${chk.raison})` : ''),
      });
      break;
    }

    // 2. QUESTION_IA
    for (const q of this.aiQuestionsSignal()) {
      const code = q.critereCode?.toUpperCase();
      if (code !== 'RESERVE_CONJOINT_SURVIVANT' && code !== 'CONJOINT_SURVIVANT') continue;
      const answer = q.answerText?.trim().toLowerCase();
      if (!answer) continue;
      const isOui = answer === 'oui' || answer.startsWith('oui ')
        || answer.startsWith('oui,') || answer.startsWith('oui.');
      if (!isOui) continue;
      const expected = this.parseBoolFromIa(q.expectedValue);
      if (expected === null || expected === userVal) continue;
      builder.addSource('QUESTION_IA', {
        expectedDisplay: expected ? 'Conjoint survivant' : 'Pas de conjoint',
        reason: `Question complémentaire : "${q.questionText}" → "${q.answerText}"`,
      });
      break;
    }

    // 3. IA — `aiData.conjointSurvivantDetected`.
    const iaConj = this.aiDataSignal()?.conjointSurvivantDetected;
    if (iaConj !== null && iaConj !== undefined && iaConj !== userVal) {
      builder.addSource('IA', {
        expectedDisplay: iaConj ? 'Conjoint survivant' : 'Pas de conjoint',
        reason: `Analyse du dossier : ${iaConj ? 'conjoint survivant détecté' : 'aucun conjoint survivant'}`,
      });
    }

    // 4. PIECE_MANQUANTE
    const piece = this.findPieceManquante([
      'RESERVE_CONJOINT_SURVIVANT', 'CONJOINT_SURVIVANT',
    ]);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  private buildNombreEnfantsAlert(): ReserveHereditaireCoherenceAlert | null {
    const userVal = this.nombreEnfants();
    if (userVal === null || userVal === undefined) return null;

    const builder = CoherenceAlertBuilder
      .forField<ReserveHereditaireAlertField>('NOMBRE_ENFANTS');

    // 1. F-96
    for (const chk of this.procedureChecksSignal()) {
      const code = chk.critereCode?.toUpperCase();
      if (code !== 'RESERVE_NB_ENFANTS' && code !== 'NB_ENFANTS') continue;
      const expected = this.parseIntFromIa(chk.expectedValue);
      if (expected === null || expected === userVal) continue;
      builder.addSource('F96', {
        expectedDisplay: `${expected} enfant${expected > 1 ? 's' : ''}`,
        reason: `Checklist procédurale : ${expected} enfant(s) détecté(s)`
          + (chk.raison ? ` (${chk.raison})` : ''),
      });
      break;
    }

    // 2. QUESTION_IA
    for (const q of this.aiQuestionsSignal()) {
      const code = q.critereCode?.toUpperCase();
      if (code !== 'RESERVE_NB_ENFANTS' && code !== 'NB_ENFANTS') continue;
      const expected = this.parseIntFromIa(q.expectedValue ?? q.answerText);
      if (expected === null || expected === userVal) continue;
      builder.addSource('QUESTION_IA', {
        expectedDisplay: `${expected} enfant${expected > 1 ? 's' : ''}`,
        reason: `Question complémentaire : "${q.questionText}" → "${q.answerText}"`,
      });
      break;
    }

    // 3. IA — `aiData.nombreEnfantsSuccessionDetecte` (ou nbDescendantsDetecte fallback).
    const iaNe = this.aiDataSignal()?.nombreEnfantsSuccessionDetecte
      ?? this.aiDataSignal()?.nbDescendantsDetecte;
    if (iaNe !== null && iaNe !== undefined && iaNe !== userVal) {
      builder.addSource('IA', {
        expectedDisplay: `${iaNe} enfant${iaNe > 1 ? 's' : ''}`,
        reason: `Analyse du dossier : ${iaNe} enfant(s) détecté(s)`,
      });
    }

    // 4. PIECE_MANQUANTE
    const piece = this.findPieceManquante([
      'RESERVE_NB_ENFANTS', 'NB_ENFANTS', 'LIVRET_FAMILLE',
    ]);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  private buildMontantSuccessionAlert(): ReserveHereditaireCoherenceAlert | null {
    const userVal = this.montantSuccession();
    if (userVal === null || userVal === undefined || userVal <= 0) return null;

    const builder = CoherenceAlertBuilder
      .forField<ReserveHereditaireAlertField>('MONTANT_SUCCESSION');

    // 1. IA only — F-96 et QUESTION_IA pas pertinents pour un montant numérique.
    const iaVal = this.aiDataSignal()?.montantSuccessionEurDetecte;
    if (iaVal !== null && iaVal !== undefined && iaVal > 0) {
      const delta = Math.abs(iaVal - userVal);
      // Tolérance ± 1 % pour éviter les faux positifs sur arrondis.
      if (delta / iaVal > 0.01) {
        builder.addSource('IA', {
          expectedDisplay: `${iaVal.toLocaleString('fr-FR')} €`,
          reason: `Analyse du dossier : montant successoral estimé à ${iaVal.toLocaleString('fr-FR')} €`,
        });
      }
    }

    // 4. PIECE_MANQUANTE
    const piece = this.findPieceManquante([
      'RESERVE_MONTANT_SUCCESSION', 'INVENTAIRE_NOTAIRE', 'MONTANT_SUCCESSION',
    ]);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  private buildMontantLibsAlert(): ReserveHereditaireCoherenceAlert | null {
    const userVal = this.montantLibsTotal();
    if (userVal === null || userVal === undefined || userVal < 0) return null;

    const builder = CoherenceAlertBuilder
      .forField<ReserveHereditaireAlertField>('MONTANT_LIBS');

    const iaVal = this.aiDataSignal()?.montantLibsTotalEurDetecte;
    if (iaVal !== null && iaVal !== undefined && iaVal >= 0) {
      const refValue = iaVal === 0 ? 1 : iaVal;
      const delta = Math.abs(iaVal - userVal);
      if (delta / refValue > 0.01) {
        builder.addSource('IA', {
          expectedDisplay: `${iaVal.toLocaleString('fr-FR')} €`,
          reason: `Analyse du dossier : libéralités totales estimées à ${iaVal.toLocaleString('fr-FR')} €`,
        });
      }
    }

    const piece = this.findPieceManquante([
      'RESERVE_MONTANT_LIBS', 'DONATIONS_LEGS', 'TESTAMENT',
    ]);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  private buildQualiteDemandeurAlert(): ReserveHereditaireCoherenceAlert | null {
    const userVal = this.qualiteDuDemandeur();
    if (userVal === null) return null;

    const builder = CoherenceAlertBuilder
      .forField<ReserveHereditaireAlertField>('QUALITE_DEMANDEUR');

    const iaVal = this.aiDataSignal()?.qualiteDuDemandeurReserveDetecte;
    if (iaVal && iaVal !== userVal) {
      builder.addSource('IA', {
        expectedDisplay: QUALITE_DEMANDEUR_RESERVE_LABELS[iaVal],
        reason: `Analyse du dossier : qualité détectée = ${QUALITE_DEMANDEUR_RESERVE_LABELS[iaVal]}`,
      });
    }

    const piece = this.findPieceManquante([
      'RESERVE_QUALITE_DEMANDEUR', 'QUALITE_DEMANDEUR',
    ]);
    if (piece) builder.addPieceManquante(piece);

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

  private parseIntFromIa(value: string | null | undefined): number | null {
    if (value === null || value === undefined) return null;
    const v = value.toString().trim();
    if (!v) return null;
    const parsed = parseInt(v, 10);
    if (Number.isNaN(parsed) || parsed < 0) return null;
    return parsed;
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
    if (this.nombreEnfants() === null || (this.nombreEnfants() ?? -1) < 0) return false;
    if (this.conjointSurvivant() === null) return false;
    const ms = this.montantSuccession();
    if (ms === null || ms === undefined || ms <= 0) return false;
    const ml = this.montantLibsTotal();
    if (ml === null || ml === undefined || ml < 0) return false;
    if (!this.dateOuvertureSuccession()) return false;
    if (this.qualiteDuDemandeur() === null) return false;
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // Handlers — effacent la provenance IA au 1er changement manuel.
  onNombreEnfantsChange(value: number | null): void {
    this.nombreEnfants.set(value === null || value === undefined ? null : value);
    this.provenanceNombreEnfants.set(null);
  }

  onConjointSurvivantChange(value: boolean | null): void {
    this.conjointSurvivant.set(value);
    this.provenanceConjoint.set(null);
  }

  onMontantSuccessionChange(value: number | null): void {
    this.montantSuccession.set(value === null || value === undefined ? null : value);
    this.provenanceMontantSuccession.set(null);
  }

  onMontantLibsChange(value: number | null): void {
    this.montantLibsTotal.set(value === null || value === undefined ? null : value);
    this.provenanceMontantLibs.set(null);
  }

  onDateOuvertureChange(value: string | null): void {
    this.dateOuvertureSuccession.set(value || null);
    this.provenanceDateOuverture.set(null);
  }

  onQualiteDemandeurChange(value: QualiteDemandeurReserve | null): void {
    this.qualiteDuDemandeur.set(value);
    this.provenanceQualiteDemandeur.set(null);
  }

  /**
   * Pré-fill depuis `aiData` (FamilleExtractedData).
   * - silencieux si aiData absent
   * - ne pré-remplit que si le champ est encore vide ou marqué IA
   */
  private prefillFromAi(): void {
    // F-236 SF-236-02 — délégation au helper partagé.
    const h = { aiData: this.aiDataSignal() };

    const ne = ReserveHeriditairePrefillRules.computeNombreEnfants(h);
    if (ne !== null && (this.nombreEnfants() === null || this.provenanceNombreEnfants() === 'IA')) {
      this.nombreEnfants.set(ne);
      this.provenanceNombreEnfants.set('IA');
    }
    const cj = ReserveHeriditairePrefillRules.computeConjointSurvivant(h);
    if (cj !== null && (this.conjointSurvivant() === null || this.provenanceConjoint() === 'IA')) {
      this.conjointSurvivant.set(cj);
      this.provenanceConjoint.set('IA');
    }
    const ms = ReserveHeriditairePrefillRules.computeMontantSuccession(h);
    if (ms !== null && (this.montantSuccession() === null || this.provenanceMontantSuccession() === 'IA')) {
      this.montantSuccession.set(ms);
      this.provenanceMontantSuccession.set('IA');
    }
    const ml = ReserveHeriditairePrefillRules.computeMontantLibs(h);
    if (ml !== null && (this.montantLibsTotal() === null || this.provenanceMontantLibs() === 'IA')) {
      this.montantLibsTotal.set(ml);
      this.provenanceMontantLibs.set('IA');
    }
    const dt = ReserveHeriditairePrefillRules.computeDateOuverture(h);
    if (dt !== null && (this.dateOuvertureSuccession() === null || this.provenanceDateOuverture() === 'IA')) {
      this.dateOuvertureSuccession.set(dt);
      this.provenanceDateOuverture.set('IA');
    }
    const qa = ReserveHeriditairePrefillRules.computeQualiteDemandeur(h);
    if (qa !== null && (this.qualiteDuDemandeur() === null || this.provenanceQualiteDemandeur() === 'IA')) {
      this.qualiteDuDemandeur.set(qa as any);
      this.provenanceQualiteDemandeur.set('IA');
    }
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request: ReserveHereditaireRequest = {
      nombreEnfants: this.nombreEnfants()!,
      conjointSurvivant: this.conjointSurvivant()!,
      montantSuccession: this.montantSuccession()!,
      montantLibsTotal: this.montantLibsTotal()!,
      dateOuvertureSuccession: this.dateOuvertureSuccession()!,
      qualiteDuDemandeur: this.qualiteDuDemandeur()!,
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
        this.snackBar.open('Réserve héréditaire analysée', 'OK', { duration: 2500 });
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
        this.result.set(r);
        // Provenance reset — valeurs persistées = saisie avocat.
        this.provenanceNombreEnfants.set(null);
        this.provenanceConjoint.set(null);
        this.provenanceMontantSuccession.set(null);
        this.provenanceMontantLibs.set(null);
        this.provenanceDateOuverture.set(null);
        this.provenanceQualiteDemandeur.set(null);
        this.showForm.set(false);
        this.loading.set(false);
      },
      error: () => {
        // 404 attendu si aucune analyse — on reste en mode formulaire + pré-fill IA.
        this.prefillFromAi();
        this.loading.set(false);
      },
    });
  }

  /** Libellé humain du verdict de recevabilité. */
  verdictLabel(code: VerdictRecevabiliteReserve | null | undefined): string {
    if (!code) return '';
    return VERDICT_RECEVABILITE_RESERVE_LABELS[code] ?? code;
  }

  /** Libellé humain de la qualité du demandeur. */
  qualiteLabel(code: QualiteDemandeurReserve | null | undefined): string {
    if (!code) return '';
    return QUALITE_DEMANDEUR_RESERVE_LABELS[code] ?? code;
  }

  /** Classe CSS du chip de recevabilité. */
  verdictChipClass(code: VerdictRecevabiliteReserve | null | undefined): string {
    const base = 'reserve-her-verdict-chip';
    if (!code) return `${base} ${base}--info`;
    if (code === 'RECEVABLE') return `${base} ${base}--critical`;
    return `${base} ${base}--info`;
  }

  trackBareme(_index: number, row: BaremeReserveRow): string {
    return row.configuration;
  }
}
