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
import { CommunauteUniverselleService } from '../../core/services/communaute-universelle.service';
import {
  CommunauteUniverselleRequest,
  CommunauteUniverselleResponse,
  DispositifAnalyse,
  DISPOSITIF_ANALYSE_LABELS,
  VerdictValidite,
} from '../../core/models/communaute-universelle.model';
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
 * SF-FA-16-02 : champs d'alerte F-IA-03 exposés par l'outil
 * "Communauté universelle".
 * - CONTRAT_NOTARIE : préalable absolu (art. 1394 Cciv) — divergence
 *   IA atteste contrat notarié alors que l'avocat saisit l'inverse.
 * - ENFANTS_NON_COMMUNS : déclencheur de l'action en retranchement
 *   (art. 1527 al. 2 Cciv) si CAI activée — alerte multi-sources.
 */
export type CommUnivAlertField = 'CONTRAT_NOTARIE' | 'ENFANTS_NON_COMMUNS';

export type CommUnivAlertSource = CoherenceAlertSource;
export type CommUnivCoherenceAlert = CoherenceAlert<CommUnivAlertField>;

/**
 * SF-FA-16-02 : outil décisionnel "Communauté universelle" (FR — art. 1526
 * + 1527 al. 2 Cciv). 4ᵉ régime matrimonial conventionnel.
 *
 * 2 dispositifs mutuellement exclusifs : VALIDITE_CONVENTION (vérification
 * du contrat de mariage notarié + opposabilité + consentement libre +
 * réserve héréditaire si enfants non communs) et LIQUIDATION_DECES (avec
 * ou sans clause d'attribution intégrale + détection action en
 * retranchement art. 1527 al. 2 si enfants 1er lit).
 *
 * FR uniquement (bannière info si dossier BE — régime distinct CC belge).
 *
 * Consomme l'API SF-FA-16-01 (mergée PR #648). Affiché conditionnellement
 * par le panel F-IA-04 (tool_id 'F-FA-16-communaute-universelle' —
 * migration 177).
 *
 * Pattern de référence : `partage-judiciaire-section` (PR #638).
 * Helper partagé : `CoherenceAlertBuilder` + `CoherenceAlert<F>` (SF-155-05).
 */
@Component({
  selector: 'app-communaute-universelle-section',
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
  templateUrl: './communaute-universelle-section.component.html',
  styleUrl: './communaute-universelle-section.component.scss',
})
export class CommunauteUniverselleSectionComponent implements OnInit, OnChanges {
  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'COMMUNAUTÉ UNIVERSELLE (FR)';
  static readonly TOOL_ICON = 'family_restroom';

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
  result = signal<CommunauteUniverselleResponse | null>(null);

  // Form fields
  dispositifAnalyse = signal<DispositifAnalyse | null>('VALIDITE_CONVENTION');
  contratNotarie = signal<boolean | null>(null);
  inscriptionEtatCivil = signal<boolean | null>(null);
  consentementLibreDesEpoux = signal<boolean | null>(null);
  respectReserveHereditaire = signal<boolean | null>(null);
  clauseAttributionIntegrale = signal<boolean | null>(null);
  enfantsNonCommuns = signal<boolean | null>(null);
  valeurCommunauteEur = signal<number | null>(null);

  /** Provenance IA pour les champs pré-remplissables. */
  provenanceContratNotarie = signal<'IA' | null>(null);
  provenanceEnfantsNonCommuns = signal<'IA' | null>(null);
  provenanceClauseAttributionIntegrale = signal<'IA' | null>(null);
  provenanceValeurCommunaute = signal<'IA' | null>(null);

  readonly dispositifOptions = DISPOSITIF_ANALYSE_LABELS;

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  isValiditeConvention = computed<boolean>(
    () => this.dispositifAnalyse() === 'VALIDITE_CONVENTION',
  );

  isLiquidationDeces = computed<boolean>(
    () => this.dispositifAnalyse() === 'LIQUIDATION_DECES',
  );

  /**
   * Alertes de cohérence F-IA-03 par field.
   * Gate : uniquement en mode formulaire.
   */
  coherenceAlerts = computed<Partial<Record<CommUnivAlertField, CommUnivCoherenceAlert>>>(() => {
    if (!this.showForm()) return {};
    const alerts: Partial<Record<CommUnivAlertField, CommUnivCoherenceAlert>> = {};
    const cn = this.buildContratNotarieAlert();
    if (cn) alerts.CONTRAT_NOTARIE = cn;
    const en = this.buildEnfantsNonCommunsAlert();
    if (en) alerts.ENFANTS_NON_COMMUNS = en;
    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    const blockers = values.filter((a) => a.severity === 'CRITICAL').length;
    return { total: values.length, blockers };
  });

  constructor(
    private service: CommunauteUniverselleService,
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

  alertTooltip(alert: CommUnivCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: CommUnivCoherenceAlert): string {
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
   * Divergence Contrat notarié (art. 1394 Cciv) — préalable absolu de
   * validité. Multi-sources F96 / QUESTION_IA / IA / PIECE_MANQUANTE.
   */
  private buildContratNotarieAlert(): CommUnivCoherenceAlert | null {
    const userVal = this.contratNotarie();
    if (userVal === null) return null;

    const builder = CoherenceAlertBuilder.forField<CommUnivAlertField>('CONTRAT_NOTARIE');

    // 1. F-96
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'COMMUNAUTE_UNIVERSELLE_CONTRAT_NOTARIE') continue;
      const ev = chk.expectedValue;
      if (!ev) continue;
      const expected = this.parseBoolFromIa(ev);
      if (expected === null || expected === userVal) continue;
      builder.addSource('F96', {
        expectedDisplay: expected ? 'Contrat notarié' : 'Pas de contrat notarié',
        reason: `Checklist procédurale : contrat de mariage ${expected ? 'notarié' : 'non notarié'}`
          + (chk.raison ? ` (${chk.raison})` : ''),
      });
      break;
    }

    // 2. QUESTION_IA
    for (const q of this.aiQuestionsSignal()) {
      if (q.critereCode?.toUpperCase() !== 'COMMUNAUTE_UNIVERSELLE_CONTRAT_NOTARIE') continue;
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
        expectedDisplay: expected ? 'Contrat notarié' : 'Pas de contrat notarié',
        reason: `Question complémentaire : "${q.questionText}" → "${q.answerText}"`,
      });
      break;
    }

    // 3. IA — `aiData.contratNotarieDetected`.
    const iaCn = this.aiDataSignal()?.contratNotarieDetected;
    if (iaCn !== null && iaCn !== undefined && iaCn !== userVal) {
      builder.addSource('IA', {
        expectedDisplay: iaCn ? 'Contrat notarié' : 'Pas de contrat notarié',
        reason: `Analyse du dossier : contrat de mariage ${iaCn ? 'notarié' : 'non notarié'}`,
      });
    }

    // 4. PIECE_MANQUANTE — contributor enrichissant.
    const piece = this.findPieceManquante([
      'COMMUNAUTE_UNIVERSELLE_CONTRAT_NOTARIE',
      'CONTRAT_MARIAGE',
    ]);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  /**
   * Divergence Enfants non communs (art. 1527 al. 2 Cciv) — déclencheur
   * de l'action en retranchement si CAI activée.
   */
  private buildEnfantsNonCommunsAlert(): CommUnivCoherenceAlert | null {
    const userVal = this.enfantsNonCommuns();
    if (userVal === null) return null;

    const builder = CoherenceAlertBuilder.forField<CommUnivAlertField>('ENFANTS_NON_COMMUNS');

    // 1. F-96
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'COMMUNAUTE_UNIVERSELLE_ENFANTS_NON_COMMUNS') continue;
      const ev = chk.expectedValue;
      if (!ev) continue;
      const expected = this.parseBoolFromIa(ev);
      if (expected === null || expected === userVal) continue;
      builder.addSource('F96', {
        expectedDisplay: expected ? 'Enfants 1er lit présents' : 'Pas d\'enfants 1er lit',
        reason: `Checklist procédurale : enfants non communs ${expected ? 'présents' : 'absents'}`
          + (chk.raison ? ` (${chk.raison})` : ''),
      });
      break;
    }

    // 2. QUESTION_IA
    for (const q of this.aiQuestionsSignal()) {
      if (q.critereCode?.toUpperCase() !== 'COMMUNAUTE_UNIVERSELLE_ENFANTS_NON_COMMUNS') continue;
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
        expectedDisplay: expected ? 'Enfants 1er lit présents' : 'Pas d\'enfants 1er lit',
        reason: `Question complémentaire : "${q.questionText}" → "${q.answerText}"`,
      });
      break;
    }

    // 3. IA — `aiData.enfantsNonCommunsDetected`.
    const iaEn = this.aiDataSignal()?.enfantsNonCommunsDetected;
    if (iaEn !== null && iaEn !== undefined && iaEn !== userVal) {
      builder.addSource('IA', {
        expectedDisplay: iaEn ? 'Enfants 1er lit présents' : 'Pas d\'enfants 1er lit',
        reason: `Analyse du dossier : enfants non communs ${iaEn ? 'détectés' : 'non détectés'}`,
      });
    }

    // 4. PIECE_MANQUANTE.
    const piece = this.findPieceManquante([
      'COMMUNAUTE_UNIVERSELLE_ENFANTS_NON_COMMUNS',
      'ENFANTS_NON_COMMUNS',
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
    if (!this.dispositifAnalyse()) return false;
    if (this.contratNotarie() === null) return false;

    if (this.isValiditeConvention()) {
      if (this.inscriptionEtatCivil() === null) return false;
      if (this.consentementLibreDesEpoux() === null) return false;
      if (this.respectReserveHereditaire() === null) return false;
    } else if (this.isLiquidationDeces()) {
      if (this.clauseAttributionIntegrale() === null) return false;
      if (this.enfantsNonCommuns() === null) return false;
      const v = this.valeurCommunauteEur();
      if (v === null || v === undefined || v < 0) return false;
    }
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  onDispositifChange(value: DispositifAnalyse | null): void {
    this.dispositifAnalyse.set(value);
    // Reset des champs propres à l'autre dispositif pour éviter les
    // submits ambigus.
    if (value === 'VALIDITE_CONVENTION') {
      this.clauseAttributionIntegrale.set(null);
      this.enfantsNonCommuns.set(null);
      this.valeurCommunauteEur.set(null);
      this.provenanceClauseAttributionIntegrale.set(null);
      this.provenanceEnfantsNonCommuns.set(null);
      this.provenanceValeurCommunaute.set(null);
    } else if (value === 'LIQUIDATION_DECES') {
      this.inscriptionEtatCivil.set(null);
      this.consentementLibreDesEpoux.set(null);
      this.respectReserveHereditaire.set(null);
    }
  }

  // Handlers onChange — effacent la provenance IA au 1er changement manuel.
  onContratNotarieChange(value: boolean | null): void {
    this.contratNotarie.set(value);
    this.provenanceContratNotarie.set(null);
  }

  onInscriptionEtatCivilChange(value: boolean | null): void {
    this.inscriptionEtatCivil.set(value);
  }

  onConsentementLibreChange(value: boolean | null): void {
    this.consentementLibreDesEpoux.set(value);
  }

  onRespectReserveHereditaireChange(value: boolean | null): void {
    this.respectReserveHereditaire.set(value);
  }

  onClauseAttributionIntegraleChange(value: boolean | null): void {
    this.clauseAttributionIntegrale.set(value);
    this.provenanceClauseAttributionIntegrale.set(null);
  }

  onEnfantsNonCommunsChange(value: boolean | null): void {
    this.enfantsNonCommuns.set(value);
    this.provenanceEnfantsNonCommuns.set(null);
  }

  onValeurCommunauteChange(value: number | null): void {
    this.valeurCommunauteEur.set(value === null || value === undefined ? null : value);
    this.provenanceValeurCommunaute.set(null);
  }

  /**
   * SF-FA-16-02 : pré-fill depuis `aiData` (FamilleExtractedData).
   * Règles (fail-open) :
   * - passe silencieusement si aiData absent
   * - ne pré-remplit QUE si le champ est encore vide / `null` (préserve
   *   les saisies)
   * - n'écrase jamais si provenance !== 'IA'
   */
  private prefillFromAi(): void {
    const ai = this.aiDataSignal();
    if (!ai) return;

    // contratNotarie ← aiData.contratNotarieDetected.
    const iaCn = ai.contratNotarieDetected;
    if (iaCn !== null && iaCn !== undefined) {
      if (this.contratNotarie() === null
          || this.provenanceContratNotarie() === 'IA') {
        this.contratNotarie.set(iaCn);
        this.provenanceContratNotarie.set('IA');
      }
    }

    // enfantsNonCommuns ← aiData.enfantsNonCommunsDetected.
    const iaEn = ai.enfantsNonCommunsDetected;
    if (iaEn !== null && iaEn !== undefined) {
      if (this.enfantsNonCommuns() === null
          || this.provenanceEnfantsNonCommuns() === 'IA') {
        this.enfantsNonCommuns.set(iaEn);
        this.provenanceEnfantsNonCommuns.set('IA');
      }
    }

    // clauseAttributionIntegrale ← aiData.clauseAttributionIntegraleDetected.
    const iaCai = ai.clauseAttributionIntegraleDetected;
    if (iaCai !== null && iaCai !== undefined) {
      if (this.clauseAttributionIntegrale() === null
          || this.provenanceClauseAttributionIntegrale() === 'IA') {
        this.clauseAttributionIntegrale.set(iaCai);
        this.provenanceClauseAttributionIntegrale.set('IA');
      }
    }

    // valeurCommunauteEur ← aiData.valeurCommunauteEurDetectee.
    const iaValeur = ai.valeurCommunauteEurDetectee;
    if (iaValeur !== null && iaValeur !== undefined && iaValeur >= 0) {
      if (this.valeurCommunauteEur() === null
          || this.provenanceValeurCommunaute() === 'IA') {
        this.valeurCommunauteEur.set(iaValeur);
        this.provenanceValeurCommunaute.set('IA');
      }
    }
  }

  calculate(): void {
    if (!this.formValid()) return;
    const dispositif = this.dispositifAnalyse()!;
    const request: CommunauteUniverselleRequest = {
      dispositifAnalyse: dispositif,
      contratNotarie: this.contratNotarie()!,
      inscriptionEtatCivil: dispositif === 'VALIDITE_CONVENTION'
        ? this.inscriptionEtatCivil() : null,
      consentementLibreDesEpoux: dispositif === 'VALIDITE_CONVENTION'
        ? this.consentementLibreDesEpoux() : null,
      respectReserveHereditaire: dispositif === 'VALIDITE_CONVENTION'
        ? this.respectReserveHereditaire() : null,
      clauseAttributionIntegrale: dispositif === 'LIQUIDATION_DECES'
        ? this.clauseAttributionIntegrale() : null,
      enfantsNonCommuns: dispositif === 'LIQUIDATION_DECES'
        ? this.enfantsNonCommuns() : null,
      valeurCommunauteEur: dispositif === 'LIQUIDATION_DECES'
        ? this.valeurCommunauteEur() : null,
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Communauté universelle analysée', 'OK', { duration: 2500 });
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
        this.provenanceContratNotarie.set(null);
        this.provenanceEnfantsNonCommuns.set(null);
        this.provenanceClauseAttributionIntegrale.set(null);
        this.provenanceValeurCommunaute.set(null);
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
   * - VALIDE sans action retranchement → navy/info.
   * - VALIDE + actionRetranchementPossible → orange/warning (attention CAI
   *   + enfants 1er lit).
   * - CONTESTABLE → orange/warning.
   * - NUL → rouge/critical (préalable obligatoire non rempli — art. 1394).
   */
  bannerClass(
    verdict: VerdictValidite | string | undefined | null,
    actionRetranchement?: boolean,
  ): string {
    if (verdict === 'NUL') return 'comm-univ-banner comm-univ-banner--critical';
    if (verdict === 'CONTESTABLE') return 'comm-univ-banner comm-univ-banner--warning';
    if (verdict === 'VALIDE' && actionRetranchement) {
      return 'comm-univ-banner comm-univ-banner--warning';
    }
    return 'comm-univ-banner comm-univ-banner--info';
  }

  /** Libellé humain d'un dispositif. */
  dispositifLabel(code: DispositifAnalyse | null | undefined): string {
    if (!code) return '';
    const opt = DISPOSITIF_ANALYSE_LABELS.find((o) => o.code === code);
    return opt?.label ?? code;
  }
}
