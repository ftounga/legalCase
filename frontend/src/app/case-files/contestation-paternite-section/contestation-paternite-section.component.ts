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
import { ContestationPaterniteService } from '../../core/services/contestation-paternite.service';
import {
  ContestationPaterniteRequest,
  ContestationPaterniteResponse,
  QUALITE_AAGIR_LABELS,
  QualiteAagir,
  VerdictRecevabiliteContestation,
} from '../../core/models/contestation-paternite.model';
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

/**
 * SF-FA-18-04 : champs d'alerte F-IA-03 exposés par l'outil
 * "Contestation de paternité" (art. 332-335 Cciv). Multi-sources.
 */
export type ContestationPaterniteAlertField =
  | 'QUALITE_AAGIR'
  | 'MOTIFS_SERIEUX'
  | 'EXPERTISE_ADN'
  | 'POSSESSION_ETAT';

export type ContestationPaterniteAlertSource = CoherenceAlertSource;
export type ContestationPaterniteCoherenceAlert =
  CoherenceAlert<ContestationPaterniteAlertField>;

/**
 * SF-FA-18-04 : outil décisionnel "Contestation de paternité"
 * (FR — art. 332-335 + 311-1 + 321 + 372 Cciv).
 *
 * 4 qualités à agir distinctes (PERE_DECLARE / PERE_BIOLOGIQUE_PRESUME /
 * MERE / ENFANT_MAJEUR), avec délai de prescription 5 ans (10 pour l'enfant
 * à compter de sa majorité, art. 321) et fin de non-recevoir possession
 * d'état conforme 5 ans (art. 333 al. 2).
 *
 * FR uniquement (bannière info si dossier BE — équivalent CC art. 318/330
 * au backlog jumeau).
 *
 * Consomme l'API SF-FA-18-03 (mergée PR #660). Affiché conditionnellement par
 * le panel F-IA-04 (tool_id 'F-FA-18-contestation-paternite' — migration 181).
 *
 * Pattern de référence : `reconnaissance-paternelle-section` (PR #655 —
 * chantier F-FA-18 jumeau).
 */
@Component({
  selector: 'app-contestation-paternite-section',
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
  templateUrl: './contestation-paternite-section.component.html',
  styleUrl: './contestation-paternite-section.component.scss',
})
export class ContestationPaterniteSectionComponent implements OnInit, OnChanges {
  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'CONTESTATION DE PATERNITÉ (FR)';
  static readonly TOOL_ICON = 'gavel';

  @Input() caseFileId!: string;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';
  @Input() aiData?: FamilleExtractedData | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;

  // Snapshots signal des inputs IA pour que `computed` réagisse.
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
  result = signal<ContestationPaterniteResponse | null>(null);

  // Form fields
  qualiteAagir = signal<QualiteAagir | null>(null);
  /** ISO YYYY-MM-DD via input type="date". */
  dateEtablissementFiliation = signal<string | null>(null);
  dateConnaissanceVerite = signal<string | null>(null);
  dateMajoriteEnfant = signal<string | null>(null);
  possessionEtatConforme5Ans = signal<boolean | null>(null);
  expertiseAdnDemandee = signal<boolean>(false);
  motifsSerieux = signal<boolean | null>(null);

  /** Provenance IA pour les champs pré-remplissables. */
  provenanceQualiteAagir = signal<'IA' | null>(null);
  provenanceDateEtablissement = signal<'IA' | null>(null);
  provenanceDateConnaissance = signal<'IA' | null>(null);
  provenanceDateMajorite = signal<'IA' | null>(null);
  provenancePossessionEtat = signal<'IA' | null>(null);
  provenanceExpertiseAdn = signal<'IA' | null>(null);
  provenanceMotifsSerieux = signal<'IA' | null>(null);

  /** Listes pour mat-radio. */
  readonly qualiteOptions = QUALITE_AAGIR_LABELS;

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  /** Qualité ENFANT_MAJEUR → date de majorité requise (art. 321 Cciv). */
  isEnfantMajeur = computed<boolean>(() => this.qualiteAagir() === 'ENFANT_MAJEUR');

  /**
   * Alertes de cohérence F-IA-03 par field. Gate : uniquement en mode formulaire.
   */
  coherenceAlerts = computed<Partial<Record<ContestationPaterniteAlertField,
      ContestationPaterniteCoherenceAlert>>>(() => {
    if (!this.showForm()) return {};
    const alerts: Partial<Record<ContestationPaterniteAlertField,
        ContestationPaterniteCoherenceAlert>> = {};
    const qa = this.buildQualiteAagirAlert();
    if (qa) alerts.QUALITE_AAGIR = qa;
    const ms = this.buildMotifsSerieuxAlert();
    if (ms) alerts.MOTIFS_SERIEUX = ms;
    const ea = this.buildExpertiseAdnAlert();
    if (ea) alerts.EXPERTISE_ADN = ea;
    const pe = this.buildPossessionEtatAlert();
    if (pe) alerts.POSSESSION_ETAT = pe;
    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    const blockers = values.filter((a) => a.severity === 'CRITICAL').length;
    return { total: values.length, blockers };
  });

  /**
   * Chip alerte délai de prescription :
   * - rouge / critical si ≤ 6 mois ou prescrit (négatif).
   * - or / warning si > 6 et < 12 mois.
   * - null sinon (pas de chip).
   */
  delaiPrescriptionAlert = computed<'critical' | 'warning' | null>(() => {
    const r = this.result();
    if (!r) return null;
    const restant = r.delaiPrescriptionRestantMois;
    if (restant <= 6) return 'critical';
    if (restant < 12) return 'warning';
    return null;
  });

  constructor(
    private service: ContestationPaterniteService,
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

    // Ré-applique le prefill si aiData change post-mount, en mode formulaire
    // ET sans résultat persisté chargé.
    if (changes['aiData'] && !changes['aiData'].firstChange
        && this.isFrance() && this.showForm() && !this.result()) {
      this.prefillFromAi();
    }
  }

  alertTooltip(alert: ContestationPaterniteCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: ContestationPaterniteCoherenceAlert): string {
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
   * Helper interne factorisé pour construire une alerte multi-sources sur un
   * champ booléen.
   */
  private buildBooleanAlert(
    field: ContestationPaterniteAlertField,
    userVal: boolean | null,
    critereCode: string,
    iaVal: boolean | null | undefined,
    trueLabel: string,
    falseLabel: string,
    fieldLabel: string,
  ): ContestationPaterniteCoherenceAlert | null {
    if (userVal === null) return null;

    const builder = CoherenceAlertBuilder
        .forField<ContestationPaterniteAlertField>(field);

    // 1. F-96
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== critereCode) continue;
      const ev = chk.expectedValue;
      if (!ev) continue;
      const expected = this.parseBoolFromIa(ev);
      if (expected === null || expected === userVal) continue;
      builder.addSource('F96', {
        expectedDisplay: expected ? trueLabel : falseLabel,
        reason: `Checklist procédurale : ${fieldLabel} ${expected ? 'vérifié' : 'non vérifié'}`
          + (chk.raison ? ` (${chk.raison})` : ''),
      });
      break;
    }

    // 2. QUESTION_IA
    for (const q of this.aiQuestionsSignal()) {
      if (q.critereCode?.toUpperCase() !== critereCode) continue;
      const answer = q.answerText?.trim().toLowerCase();
      if (!answer) continue;
      const isOui = answer === 'oui' || answer.startsWith('oui ')
        || answer.startsWith('oui,') || answer.startsWith('oui.');
      if (!isOui) continue;
      const ev = q.expectedValue;
      if (!ev) continue;
      const expected = this.parseBoolFromIa(ev);
      if (expected === null || expected === userVal) continue;
      builder.addSource('QUESTION_IA', {
        expectedDisplay: expected ? trueLabel : falseLabel,
        reason: `Question complémentaire : "${q.questionText}" → "${q.answerText}"`,
      });
      break;
    }

    // 3. IA — `aiData.*Detected`.
    if (iaVal !== null && iaVal !== undefined && iaVal !== userVal) {
      builder.addSource('IA', {
        expectedDisplay: iaVal ? trueLabel : falseLabel,
        reason: `Analyse du dossier : ${fieldLabel} ${iaVal ? 'vérifié' : 'non vérifié'}`,
      });
    }

    // 4. PIECE_MANQUANTE — contributor enrichissant.
    const piece = this.findPieceManquante([critereCode]);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  private buildQualiteAagirAlert(): ContestationPaterniteCoherenceAlert | null {
    const userVal = this.qualiteAagir();
    if (userVal === null) return null;
    const iaVal = this.aiDataSignal()?.qualiteAagirContestationDetected;
    if (!iaVal || iaVal === userVal) return null;

    const labelOf = (q: QualiteAagir) =>
      QUALITE_AAGIR_LABELS.find((o) => o.code === q)?.label ?? q;

    const builder = CoherenceAlertBuilder
        .forField<ContestationPaterniteAlertField>('QUALITE_AAGIR');
    builder.addSource('IA', {
      expectedDisplay: labelOf(iaVal),
      reason: `Analyse du dossier : qualité à agir = ${labelOf(iaVal)}`,
    });
    return builder.build();
  }

  private buildMotifsSerieuxAlert(): ContestationPaterniteCoherenceAlert | null {
    return this.buildBooleanAlert(
      'MOTIFS_SERIEUX',
      this.motifsSerieux(),
      'CONTESTATION_PATERNITE_MOTIFS_SERIEUX',
      this.aiDataSignal()?.motifsSerieuxDetected,
      'Motifs sérieux',
      'Pas de motifs sérieux',
      'motifs sérieux',
    );
  }

  private buildExpertiseAdnAlert(): ContestationPaterniteCoherenceAlert | null {
    return this.buildBooleanAlert(
      'EXPERTISE_ADN',
      this.expertiseAdnDemandee(),
      'CONTESTATION_PATERNITE_EXPERTISE_ADN',
      this.aiDataSignal()?.expertiseAdnDemandeeDetected,
      'Expertise ADN demandée',
      'Pas d\'expertise ADN',
      'expertise ADN demandée',
    );
  }

  private buildPossessionEtatAlert(): ContestationPaterniteCoherenceAlert | null {
    return this.buildBooleanAlert(
      'POSSESSION_ETAT',
      this.possessionEtatConforme5Ans(),
      'CONTESTATION_PATERNITE_POSSESSION_ETAT',
      this.aiDataSignal()?.possessionEtatConforme5AnsDetected,
      'Possession d\'état 5 ans',
      'Pas de possession d\'état 5 ans',
      'possession d\'état conforme 5 ans',
    );
  }

  private parseBoolFromIa(value: string | null | undefined): boolean | null {
    if (value === null || value === undefined) return null;
    const v = value.toString().trim().toLowerCase();
    if (!v) return null;
    if (v === 'oui' || v === 'true' || v === 'vrai' || v === '1' || v === 'yes') return true;
    if (v === 'non' || v === 'false' || v === 'faux' || v === '0' || v === 'no') return false;
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
    if (!this.qualiteAagir()) return false;
    if (!this.dateEtablissementFiliation()) return false;
    if (!this.dateConnaissanceVerite()) return false;
    if (this.motifsSerieux() === null) return false;
    if (this.possessionEtatConforme5Ans() === null) return false;
    // Date majorité requise pour ENFANT_MAJEUR (art. 321 Cciv).
    if (this.isEnfantMajeur() && !this.dateMajoriteEnfant()) return false;
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // Handlers onChange — effacent la provenance IA au 1er changement manuel.
  onQualiteAagirChange(value: QualiteAagir | null): void {
    this.qualiteAagir.set(value);
    this.provenanceQualiteAagir.set(null);
  }

  onDateEtablissementChange(value: string | null): void {
    this.dateEtablissementFiliation.set(value || null);
    this.provenanceDateEtablissement.set(null);
  }

  onDateConnaissanceChange(value: string | null): void {
    this.dateConnaissanceVerite.set(value || null);
    this.provenanceDateConnaissance.set(null);
  }

  onDateMajoriteChange(value: string | null): void {
    this.dateMajoriteEnfant.set(value || null);
    this.provenanceDateMajorite.set(null);
  }

  onPossessionEtatChange(value: boolean | null): void {
    this.possessionEtatConforme5Ans.set(value);
    this.provenancePossessionEtat.set(null);
  }

  onExpertiseAdnChange(value: boolean): void {
    this.expertiseAdnDemandee.set(value);
    this.provenanceExpertiseAdn.set(null);
  }

  onMotifsSerieuxChange(value: boolean | null): void {
    this.motifsSerieux.set(value);
    this.provenanceMotifsSerieux.set(null);
  }

  /**
   * SF-FA-18-04 : pré-fill depuis `aiData` (FamilleExtractedData).
   * Règles (fail-open) :
   * - passe silencieusement si aiData absent
   * - ne pré-remplit QUE si le champ est encore vide / `null` (préserve les edits)
   * - n'écrase jamais si provenance !== 'IA'
   */
  private prefillFromAi(): void {
    const ai = this.aiDataSignal();
    if (!ai) return;

    // qualiteAagir ← aiData.qualiteAagirContestationDetected.
    const iaQ = ai.qualiteAagirContestationDetected;
    if (iaQ) {
      if (this.qualiteAagir() === null
          || this.provenanceQualiteAagir() === 'IA') {
        this.qualiteAagir.set(iaQ);
        this.provenanceQualiteAagir.set('IA');
      }
    }

    // dateEtablissementFiliation ← aiData.dateEtablissementFiliationDetectee.
    const iaDE = ai.dateEtablissementFiliationDetectee;
    if (iaDE) {
      if (this.dateEtablissementFiliation() === null
          || this.provenanceDateEtablissement() === 'IA') {
        this.dateEtablissementFiliation.set(iaDE);
        this.provenanceDateEtablissement.set('IA');
      }
    }

    // dateConnaissanceVerite ← aiData.dateConnaissanceVeriteDetectee.
    const iaDC = ai.dateConnaissanceVeriteDetectee;
    if (iaDC) {
      if (this.dateConnaissanceVerite() === null
          || this.provenanceDateConnaissance() === 'IA') {
        this.dateConnaissanceVerite.set(iaDC);
        this.provenanceDateConnaissance.set('IA');
      }
    }

    // dateMajoriteEnfant ← aiData.dateMajoriteEnfantDetectee.
    const iaDM = ai.dateMajoriteEnfantDetectee;
    if (iaDM) {
      if (this.dateMajoriteEnfant() === null
          || this.provenanceDateMajorite() === 'IA') {
        this.dateMajoriteEnfant.set(iaDM);
        this.provenanceDateMajorite.set('IA');
      }
    }

    // possessionEtatConforme5Ans ← aiData.possessionEtatConforme5AnsDetected.
    const iaPE = ai.possessionEtatConforme5AnsDetected;
    if (iaPE !== null && iaPE !== undefined) {
      if (this.possessionEtatConforme5Ans() === null
          || this.provenancePossessionEtat() === 'IA') {
        this.possessionEtatConforme5Ans.set(iaPE);
        this.provenancePossessionEtat.set('IA');
      }
    }

    // expertiseAdnDemandee ← aiData.expertiseAdnDemandeeDetected.
    const iaEA = ai.expertiseAdnDemandeeDetected;
    if (iaEA !== null && iaEA !== undefined) {
      if (!this.expertiseAdnDemandee()
          || this.provenanceExpertiseAdn() === 'IA') {
        this.expertiseAdnDemandee.set(iaEA);
        this.provenanceExpertiseAdn.set('IA');
      }
    }

    // motifsSerieux ← aiData.motifsSerieuxDetected.
    const iaMS = ai.motifsSerieuxDetected;
    if (iaMS !== null && iaMS !== undefined) {
      if (this.motifsSerieux() === null
          || this.provenanceMotifsSerieux() === 'IA') {
        this.motifsSerieux.set(iaMS);
        this.provenanceMotifsSerieux.set('IA');
      }
    }
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request: ContestationPaterniteRequest = {
      qualiteAagir: this.qualiteAagir()!,
      dateEtablissementFiliation: this.dateEtablissementFiliation()!,
      dateConnaissanceVerite: this.dateConnaissanceVerite()!,
      dateMajoriteEnfant: this.dateMajoriteEnfant() || null,
      possessionEtatConforme5Ans: this.possessionEtatConforme5Ans()!,
      expertiseAdnDemandee: this.expertiseAdnDemandee(),
      motifsSerieux: this.motifsSerieux()!,
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Contestation de paternité analysée', 'OK', { duration: 2500 });
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
        this.provenanceQualiteAagir.set(null);
        this.provenanceDateEtablissement.set(null);
        this.provenanceDateConnaissance.set(null);
        this.provenanceDateMajorite.set(null);
        this.provenancePossessionEtat.set(null);
        this.provenanceExpertiseAdn.set(null);
        this.provenanceMotifsSerieux.set(null);
        this.showForm.set(false);
        this.loading.set(false);
      },
      error: () => {
        // 404 attendu si aucune analyse — on reste en mode formulaire.
        // Fallback pré-fill IA uniquement ici (pas si GET 200).
        this.prefillFromAi();
        this.loading.set(false);
      },
    });
  }

  /**
   * Classe CSS du bandeau verdict.
   * - ELEVEE → navy/info (recevabilité forte).
   * - MOYENNE → or/warning (procédure partiellement contestable).
   * - FAIBLE → rouge/critical (prescription, fin de non-recevoir).
   */
  bannerClass(verdict: VerdictRecevabiliteContestation | string | undefined | null): string {
    switch (verdict) {
      case 'ELEVEE': return 'cont-pat-banner cont-pat-banner--info';
      case 'MOYENNE': return 'cont-pat-banner cont-pat-banner--warning';
      case 'FAIBLE': return 'cont-pat-banner cont-pat-banner--critical';
      default: return 'cont-pat-banner cont-pat-banner--info';
    }
  }

  /** Libellé humain d'une qualité à agir. */
  qualiteLabel(code: QualiteAagir | null | undefined): string {
    if (!code) return '';
    const opt = QUALITE_AAGIR_LABELS.find((o) => o.code === code);
    return opt?.label ?? code;
  }
}
