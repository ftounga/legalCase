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
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { RapportSuccessionService } from '../../core/services/rapport-succession.service';
import {
  ModeRapport,
  MODE_RAPPORT_LABELS,
  QualiteHeritierRapport,
  QUALITE_HERITIER_RAPPORT_LABELS,
  RapportSuccessionRequest,
  RapportSuccessionResponse,
  VerdictObligationRapport,
  VERDICT_OBLIGATION_RAPPORT_LABELS,
} from '../../core/models/rapport-succession.model';
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
import { RapportSuccessionPrefillRules } from './rapport-succession-section-prefill-rules';

/**
 * SF-FA-24-14 : champs F-IA-03 audités par l'outil "Rapport à succession".
 * - QUALITE_HERITIER : qualité vis-à-vis du rapport (art. 843).
 * - DONATIONS_RECUES_EUR : montant nominal au jour de la donation.
 * - VALEUR_AU_JOUR_PARTAGE : valeur retenue (art. 860).
 * - DATE_DONATION : pour traçabilité.
 * - DONATION_DISPENSE : dispense expresse art. 919.
 * - NATURE_NON_RAPPORTABLE : frais éducation / rémunératoire art. 851/852.
 */
export type RapportSuccessionAlertField =
  | 'QUALITE_HERITIER'
  | 'DONATIONS_RECUES_EUR'
  | 'VALEUR_AU_JOUR_PARTAGE'
  | 'DATE_DONATION'
  | 'DONATION_DISPENSE'
  | 'NATURE_NON_RAPPORTABLE';

export type RapportSuccessionAlertSource = CoherenceAlertSource;
export type RapportSuccessionCoherenceAlert =
  CoherenceAlert<RapportSuccessionAlertField>;

/**
 * SF-FA-24-14 : outil décisionnel "Rapport à succession" (FR — art. 843-863
 * + 919 Cciv).
 *
 * FR uniquement (bannière info si dossier BE — équivalent CC BE traité dans
 * la feature jumelle backlog F-FA-24-BE, barème différent).
 *
 * Consomme l'API SF-FA-24-13 (mergée PR #679). Affiché conditionnellement
 * par le panel F-IA-04 (tool_id 'F-FA-24-rapport-succession').
 *
 * Pattern de référence : `reserve-heriditaire-section` (PR #675).
 * Helper partagé : `CoherenceAlertBuilder` + `CoherenceAlert<F>` (SF-155-05).
 */
@Component({
  selector: 'app-rapport-succession-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule, DecimalPipe,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatRadioModule,
    MatSlideToggleModule,
    MatProgressSpinnerModule,
    LegalCitationsPipe,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './rapport-succession-section.component.html',
  styleUrl: './rapport-succession-section.component.scss',
})
export class RapportSuccessionSectionComponent implements OnInit, OnChanges {
  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'RAPPORT À SUCCESSION (FR)';
  static readonly TOOL_ICON = 'account_balance';

  /** F-236 SF-236-02 — compteur miroir prefillFromAi via helper. */
  static getPrefillCount(input: {
    aiData?: FamilleExtractedData | null;
    procedureChecks?: unknown[];
    aiQuestions?: unknown[];
    piecesManquantes?: unknown[];
    triggerEvents?: unknown[];
    workspaceCountry?: string;
  }): number {
    return RapportSuccessionPrefillRules.computePrefillCount(input);
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
   *  - `calculate()` POSTe sur `/api/v1/simulators/F-FA-24-rapport-succession/calculate`.
   *  - `triggerRefresh()` jamais invoqué.
   *
   * Default `false` — mode case-file scoped inchangé.
   */
  @Input() standaloneMode: boolean = false;
  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<RapportSuccessionResponse | null>(null);

  // Form fields
  qualiteHeritier = signal<QualiteHeritierRapport | null>(null);
  donationsRecuesEur = signal<number | null>(null);
  dateDonation = signal<string | null>(null);
  valeurAuJourPartage = signal<number | null>(null);
  donationDispenseDeRapport = signal<boolean>(false);
  naturePresumeeNonRapportable = signal<boolean>(false);

  /** Provenance IA des champs pré-remplis. */
  provenanceQualiteHeritier = signal<'IA' | null>(null);
  provenanceDonationsRecues = signal<'IA' | null>(null);
  provenanceDateDonation = signal<'IA' | null>(null);
  provenanceValeurPartage = signal<'IA' | null>(null);
  provenanceDispense = signal<'IA' | null>(null);
  provenanceNatureNonRapportable = signal<'IA' | null>(null);

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  coherenceAlerts = computed<Partial<Record<RapportSuccessionAlertField, RapportSuccessionCoherenceAlert>>>(() => {
    // F-163 SF-163-02c : aucune source IA en standalone.
    if (this.standaloneMode) return {};
    if (!this.showForm()) return {};
    const alerts: Partial<Record<RapportSuccessionAlertField, RapportSuccessionCoherenceAlert>> = {};
    const q = this.buildQualiteHeritierAlert();
    if (q) alerts.QUALITE_HERITIER = q;
    const dr = this.buildDonationsRecuesAlert();
    if (dr) alerts.DONATIONS_RECUES_EUR = dr;
    const vp = this.buildValeurPartageAlert();
    if (vp) alerts.VALEUR_AU_JOUR_PARTAGE = vp;
    const dd = this.buildDateDonationAlert();
    if (dd) alerts.DATE_DONATION = dd;
    const di = this.buildDispenseAlert();
    if (di) alerts.DONATION_DISPENSE = di;
    const nn = this.buildNatureNonRapportableAlert();
    if (nn) alerts.NATURE_NON_RAPPORTABLE = nn;
    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    const blockers = values.filter((a) => a.severity === 'CRITICAL').length;
    return { total: values.length, blockers };
  });

  readonly qualiteHeritierOptions: ReadonlyArray<{
    code: QualiteHeritierRapport;
    label: string;
  }> = (Object.entries(QUALITE_HERITIER_RAPPORT_LABELS) as
    Array<[QualiteHeritierRapport, string]>
  ).map(([code, label]) => ({ code, label }));

  constructor(
    private service: RapportSuccessionService,
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

  alertTooltip(alert: RapportSuccessionCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: RapportSuccessionCoherenceAlert): string {
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
  // F-IA-03 builders (V1 : IA + PIECE_MANQUANTE)
  // ============================================================

  private buildQualiteHeritierAlert(): RapportSuccessionCoherenceAlert | null {
    const userVal = this.qualiteHeritier();
    if (userVal === null) return null;
    const builder = CoherenceAlertBuilder
      .forField<RapportSuccessionAlertField>('QUALITE_HERITIER');

    const iaVal = this.aiDataSignal()?.qualiteHeritierRapportDetectee;
    if (iaVal && (iaVal === 'DESCENDANT' || iaVal === 'CONJOINT_SURVIVANT')
        && iaVal !== userVal) {
      builder.addSource('IA', {
        expectedDisplay: QUALITE_HERITIER_RAPPORT_LABELS[iaVal],
        reason: `Analyse du dossier : qualité détectée = ${QUALITE_HERITIER_RAPPORT_LABELS[iaVal]}`,
      });
    }

    const piece = this.findPieceManquante([
      'RAPPORT_QUALITE_HERITIER', 'QUALITE_HERITIER', 'ACTE_NOTORIETE',
    ]);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  private buildDonationsRecuesAlert(): RapportSuccessionCoherenceAlert | null {
    const userVal = this.donationsRecuesEur();
    if (userVal === null || userVal === undefined || userVal <= 0) return null;
    const builder = CoherenceAlertBuilder
      .forField<RapportSuccessionAlertField>('DONATIONS_RECUES_EUR');

    const iaVal = this.aiDataSignal()?.montantDonationsRecuesEurDetecte;
    if (iaVal !== null && iaVal !== undefined && iaVal > 0) {
      const delta = Math.abs(iaVal - userVal);
      // Tolérance ± 1 % pour éviter les faux positifs sur arrondis.
      if (delta / iaVal > 0.01) {
        builder.addSource('IA', {
          expectedDisplay: `${iaVal.toLocaleString('fr-FR')} €`,
          reason: `Analyse du dossier : montant nominal estimé à ${iaVal.toLocaleString('fr-FR')} €`,
        });
      }
    }

    const piece = this.findPieceManquante([
      'RAPPORT_DONATION_NOMINALE', 'ACTE_DONATION', 'MONTANT_DONATION',
    ]);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  private buildValeurPartageAlert(): RapportSuccessionCoherenceAlert | null {
    const userVal = this.valeurAuJourPartage();
    if (userVal === null || userVal === undefined || userVal <= 0) return null;
    const builder = CoherenceAlertBuilder
      .forField<RapportSuccessionAlertField>('VALEUR_AU_JOUR_PARTAGE');

    const iaVal = this.aiDataSignal()?.valeurDonationAuJourPartageEurDetectee;
    if (iaVal !== null && iaVal !== undefined && iaVal > 0) {
      const delta = Math.abs(iaVal - userVal);
      if (delta / iaVal > 0.01) {
        builder.addSource('IA', {
          expectedDisplay: `${iaVal.toLocaleString('fr-FR')} €`,
          reason: `Analyse du dossier : valeur au jour du partage estimée à ${iaVal.toLocaleString('fr-FR')} €`,
        });
      }
    }

    const piece = this.findPieceManquante([
      'RAPPORT_VALEUR_PARTAGE', 'EVALUATION_BIEN', 'EXPERTISE_NOTAIRE',
    ]);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  private buildDateDonationAlert(): RapportSuccessionCoherenceAlert | null {
    const userVal = this.dateDonation();
    if (!userVal) return null;
    const builder = CoherenceAlertBuilder
      .forField<RapportSuccessionAlertField>('DATE_DONATION');

    const iaVal = this.aiDataSignal()?.dateDonationDetectee;
    if (iaVal && iaVal !== userVal) {
      builder.addSource('IA', {
        expectedDisplay: iaVal,
        reason: `Analyse du dossier : date détectée = ${iaVal}`,
      });
    }

    const piece = this.findPieceManquante([
      'RAPPORT_DATE_DONATION', 'ACTE_DONATION_DATE',
    ]);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  private buildDispenseAlert(): RapportSuccessionCoherenceAlert | null {
    const userVal = this.donationDispenseDeRapport();
    const builder = CoherenceAlertBuilder
      .forField<RapportSuccessionAlertField>('DONATION_DISPENSE');

    const iaVal = this.aiDataSignal()?.donationDispenseDeRapportDetected;
    if (iaVal !== null && iaVal !== undefined && iaVal !== userVal) {
      builder.addSource('IA', {
        expectedDisplay: iaVal ? 'Dispense art. 919' : 'Pas de dispense',
        reason: iaVal
          ? `Analyse du dossier : clause de dispense expresse art. 919 détectée`
          : `Analyse du dossier : pas de clause de dispense détectée`,
      });
    }

    const piece = this.findPieceManquante([
      'RAPPORT_DISPENSE', 'CLAUSE_DISPENSE', 'ACTE_DONATION',
    ]);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  private buildNatureNonRapportableAlert(): RapportSuccessionCoherenceAlert | null {
    const userVal = this.naturePresumeeNonRapportable();
    const builder = CoherenceAlertBuilder
      .forField<RapportSuccessionAlertField>('NATURE_NON_RAPPORTABLE');

    const iaVal = this.aiDataSignal()?.naturePresumeeNonRapportableDetected;
    if (iaVal !== null && iaVal !== undefined && iaVal !== userVal) {
      builder.addSource('IA', {
        expectedDisplay: iaVal ? 'Frais éducation / rémunératoire' : 'Donation classique',
        reason: iaVal
          ? `Analyse du dossier : frais d'éducation (852) ou rémunératoire (851) détecté`
          : `Analyse du dossier : donation classique (rapportable par défaut)`,
      });
    }

    const piece = this.findPieceManquante([
      'RAPPORT_NATURE', 'NATURE_DONATION',
    ]);
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

  toggleCollapse(): void {
    this.collapsed.update((v) => !v);
  }

  formValid(): boolean {
    if (this.qualiteHeritier() === null) return false;
    const dr = this.donationsRecuesEur();
    if (dr === null || dr === undefined || dr <= 0) return false;
    const vp = this.valeurAuJourPartage();
    if (vp === null || vp === undefined || vp <= 0) return false;
    if (!this.dateDonation()) return false;
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // Handlers — effacent la provenance IA au 1er changement manuel.
  onQualiteHeritierChange(value: QualiteHeritierRapport | null): void {
    this.qualiteHeritier.set(value);
    this.provenanceQualiteHeritier.set(null);
  }

  onDonationsRecuesChange(value: number | null): void {
    this.donationsRecuesEur.set(value === null || value === undefined ? null : value);
    this.provenanceDonationsRecues.set(null);
  }

  onDateDonationChange(value: string | null): void {
    this.dateDonation.set(value || null);
    this.provenanceDateDonation.set(null);
  }

  onValeurPartageChange(value: number | null): void {
    this.valeurAuJourPartage.set(value === null || value === undefined ? null : value);
    this.provenanceValeurPartage.set(null);
  }

  onDispenseChange(value: boolean): void {
    this.donationDispenseDeRapport.set(!!value);
    this.provenanceDispense.set(null);
  }

  onNatureNonRapportableChange(value: boolean): void {
    this.naturePresumeeNonRapportable.set(!!value);
    this.provenanceNatureNonRapportable.set(null);
  }

  /**
   * Pré-fill depuis `aiData` (FamilleExtractedData). Silencieux si aiData
   * absent. Ne pré-remplit que si le champ est encore vide ou marqué IA.
   */
  private prefillFromAi(): void {
    // F-236 SF-236-02 — délégation au helper partagé.
    const h = { aiData: this.aiDataSignal() };

    const q = RapportSuccessionPrefillRules.computeQualiteHeritier(h);
    if (q !== null && (this.qualiteHeritier() === null || this.provenanceQualiteHeritier() === 'IA')) {
      this.qualiteHeritier.set(q);
      this.provenanceQualiteHeritier.set('IA');
    }
    const dr = RapportSuccessionPrefillRules.computeDonationsRecues(h);
    if (dr !== null && (this.donationsRecuesEur() === null || this.provenanceDonationsRecues() === 'IA')) {
      this.donationsRecuesEur.set(dr);
      this.provenanceDonationsRecues.set('IA');
    }
    const vp = RapportSuccessionPrefillRules.computeValeurPartage(h);
    if (vp !== null && (this.valeurAuJourPartage() === null || this.provenanceValeurPartage() === 'IA')) {
      this.valeurAuJourPartage.set(vp);
      this.provenanceValeurPartage.set('IA');
    }
    const dd = RapportSuccessionPrefillRules.computeDateDonation(h);
    if (dd !== null && (this.dateDonation() === null || this.provenanceDateDonation() === 'IA')) {
      this.dateDonation.set(dd);
      this.provenanceDateDonation.set('IA');
    }
    const dispense = RapportSuccessionPrefillRules.computeDispense(h);
    if (dispense !== null) {
      if ((this.provenanceDispense() === null && this.donationDispenseDeRapport() === false)
          || this.provenanceDispense() === 'IA') {
        this.donationDispenseDeRapport.set(dispense);
        this.provenanceDispense.set('IA');
      }
    }
    const nature = RapportSuccessionPrefillRules.computeNatureNonRapportable(h);
    if (nature !== null) {
      if ((this.provenanceNatureNonRapportable() === null && this.naturePresumeeNonRapportable() === false)
          || this.provenanceNatureNonRapportable() === 'IA') {
        this.naturePresumeeNonRapportable.set(nature);
        this.provenanceNatureNonRapportable.set('IA');
      }
    }
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request: RapportSuccessionRequest = {
      donationsRecuesEur: this.donationsRecuesEur()!,
      dateDonation: this.dateDonation()!,
      valeurAuJourPartage: this.valeurAuJourPartage()!,
      donationDispenseDeRapport: this.donationDispenseDeRapport(),
      naturePresumeeNonRapportable: this.naturePresumeeNonRapportable(),
      qualiteHeritier: this.qualiteHeritier()!,
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
        this.snackBar.open('Rapport à succession analysé', 'OK', { duration: 2500 });
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
        this.provenanceQualiteHeritier.set(null);
        this.provenanceDonationsRecues.set(null);
        this.provenanceDateDonation.set(null);
        this.provenanceValeurPartage.set(null);
        this.provenanceDispense.set(null);
        this.provenanceNatureNonRapportable.set(null);
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

  /** Libellé humain du verdict d'obligation. */
  verdictLabel(code: VerdictObligationRapport | null | undefined): string {
    if (!code) return '';
    return VERDICT_OBLIGATION_RAPPORT_LABELS[code] ?? code;
  }

  /** Libellé humain de la qualité d'héritier. */
  qualiteLabel(code: QualiteHeritierRapport | null | undefined): string {
    if (!code) return '';
    return QUALITE_HERITIER_RAPPORT_LABELS[code] ?? code;
  }

  /** Libellé humain du mode de rapport. */
  modeLabel(code: ModeRapport | null | undefined): string {
    if (!code) return '';
    return MODE_RAPPORT_LABELS[code] ?? code;
  }

  /** Classe CSS du chip de verdict — RAPPORTABLE = critique, autres = info. */
  verdictChipClass(code: VerdictObligationRapport | null | undefined): string {
    const base = 'rapport-succ-verdict-chip';
    if (!code) return `${base} ${base}--info`;
    if (code === 'RAPPORTABLE') return `${base} ${base}--critical`;
    return `${base} ${base}--info`;
  }
}
