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
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { LicenciementNulDetectionService } from '../../core/services/licenciement-nul-detection.service';
import {
  LicenciementNulDetectionRequest,
  LicenciementNulDetectionResponse,
  LicenciementNulProtection,
  LicenciementNulVerdict,
  PROTECTION_LABELS,
} from '../../core/models/licenciement-nul-detection.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import {
  PieceManquanteEntry,
  TravailExtractedData,
} from '../../core/models/case-analysis.model';
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
import {
  LicenciementNulDetectionSectionPrefillRules,
} from './licenciement-nul-detection-section-prefill-rules';

/**
 * SF-DT-16-02 : champs F-IA-03 audités par l'outil F-DT-16 (licenciement nul
 * détection multi-protections). Un par field pré-remplissable depuis l'IA.
 */
export type LNDAlertField = 'SALAIRE' | 'DATE_NOTIFICATION' | 'PROTECTIONS';

export type LNDCoherenceAlert = CoherenceAlert<LNDAlertField>;

/** Seuil divergence salaire — aligné F-DT-09 / F-DT-11 (10 %). */
const SALAIRE_DIVERGENCE_RATIO = 0.10;
const ISO_DATE_REGEX = /^\d{4}-\d{2}-\d{2}$/;

/**
 * SF-DT-16-02 : mapping `motifNullitePressenti` (TravailExtractedData) →
 * booléen de protection à pré-remplir. Fail-open : valeur IA non mappable
 * = pas de pré-fill.
 */
const AI_MOTIF_TO_PROTECTION_FLAG: Readonly<Record<string, keyof Pick<
  LicenciementNulDetectionRequest,
  'salarieEnceinte' | 'salarieAccidentTravail' | 'salarieHarceleAvere'
  | 'salarieDiscriminationAlleguee' | 'salarieMotifLanceurAlerte'
  | 'salarieMandatRepresentant' | 'salarieActionJustice'
>>> = {
  HARCELEMENT_MORAL: 'salarieHarceleAvere',
  HARCELEMENT_SEXUEL: 'salarieHarceleAvere',
  DISCRIMINATION: 'salarieDiscriminationAlleguee',
  MATERNITE_PATERNITE: 'salarieEnceinte',
  ACCIDENT_MP: 'salarieAccidentTravail',
  RETORSION: 'salarieActionJustice',
  SYNDICAL: 'salarieMandatRepresentant',
};

/**
 * SF-DT-16-02 : composant Angular standalone pour l'outil "Licenciement nul
 * détection multi-protections + indemnité plancher 6 mois" (F-DT-16,
 * art. L.1235-3-1 al. 2 Code du travail). FRANCE uniquement.
 *
 * Pattern de référence : `harcelement-licenciement-nul-section` (template
 * canonique 2026-04-24, cf. `ai-skills/frontend-coherence-audit.md` §5)
 * + `divorce-accepte-section` (verdict 3 niveaux navy/or/rouge classique).
 *
 * - Form : 7 slide-toggles (1 par protection) + 2 datepickers conditionnels
 *   (accouchement si enceinte, consolidation AT si AT en cours) + numériques
 *   salaire / ancienneté + datepicker notification.
 * - Verdict : ELEVEE (navy) / MOYENNE (or) / FAIBLE (rouge classique).
 * - Pré-fill IA : `salaireBrutMensuel`, `dateLicenciement` (→ notification),
 *   `motifNullitePressenti` (→ flag de protection correspondant).
 * - Validation F-IA-03 sur 3 fields (SALAIRE, DATE_NOTIFICATION, PROTECTIONS).
 * - Refresh dashboard F-IA-02 + MatSnackBar erreurs.
 */
@Component({
  selector: 'app-licenciement-nul-detection-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule, DecimalPipe,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatSlideToggleModule, MatProgressSpinnerModule,
    LegalCitationsPipe,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './licenciement-nul-detection-section.component.html',
  styleUrl: './licenciement-nul-detection-section.component.scss',
})
export class LicenciementNulDetectionSectionComponent implements OnInit, OnChanges {
  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'LICENCIEMENT NUL — DÉTECTION (ART. L.1235-3-1)';

  /** F-177 SF-177-12 / F-236 SF-236-02 — délègue au helper partagé (parité runtime). */
  static getPrefillCount(input: {
    aiData?: any;
    procedureChecks?: any[];
    aiQuestions?: any[];
    piecesManquantes?: any[];
    triggerEvents?: any[];
    workspaceCountry?: string;
  }): number {
    return LicenciementNulDetectionSectionPrefillRules.computePrefillCount({
      aiData: input.aiData,
    });
  }

  static readonly TOOL_ICON = 'policy';

  @Input() caseFileId!: string;
  // F-177 SF-177-03b : force l'expansion (mode modal F-177).
  @Input() forceExpanded = false;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';
  // Inputs IA (tous optionnels — null-safe partout).
  @Input() aiData?: TravailExtractedData | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;

  // Snapshots signal des inputs IA pour réactivité des `computed`.
  private aiDataSignal = signal<TravailExtractedData | null | undefined>(undefined);
  private procedureChecksSignal = signal<ProcedureCheck[]>([]);
  private aiQuestionsSignal = signal<AiQuestion[]>([]);
  private piecesManquantesSignal = signal<PieceManquanteEntry[]>([]);

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<LicenciementNulDetectionResponse | null>(null);

  // --- Form fields ---
  // 7 booléens de protection
  salarieEnceinte = signal<boolean>(false);
  salarieAccidentTravail = signal<boolean>(false);
  salarieHarceleAvere = signal<boolean>(false);
  salarieDiscriminationAlleguee = signal<boolean>(false);
  salarieMotifLanceurAlerte = signal<boolean>(false);
  salarieMandatRepresentant = signal<boolean>(false);
  salarieActionJustice = signal<boolean>(false);
  // Dates contextuelles + numériques + date notification
  dateAccouchement = signal<string | null>(null);
  dateConsolidationAT = signal<string | null>(null);
  salaireMensuelBrutEur = signal<number | null>(null);
  ancienneteAnnees = signal<number | null>(null);
  dateNotificationLicenciement = signal<string | null>(null);

  // Provenance IA par champ pré-remplissable.
  provenanceSalaire = signal<'IA' | null>(null);
  provenanceDateNotification = signal<'IA' | null>(null);
  // Granularité protections : 1 signal global (l'IA pose au plus 1 motif).
  provenanceProtections = signal<'IA' | null>(null);

  // Map {sourceKey → explanations} pour le popover F-IA-03-15c (fail-open).
  sourceExplanations = signal<Map<string, SourceExplanation[]>>(new Map());

  /**
   * Coherence alerts F-IA-03 — gate `showForm()` strict (pattern anti-bug
   * SF-IA-03-12 : alertes masquées après calcul rendu).
   */
  coherenceAlerts = computed<Partial<Record<LNDAlertField, LNDCoherenceAlert>>>(() => {
    if (!this.showForm()) return {};
    const alerts: Partial<Record<LNDAlertField, LNDCoherenceAlert>> = {};
    const salaireAlert = this.buildSalaireAlert();
    if (salaireAlert) alerts.SALAIRE = salaireAlert;
    const dateAlert = this.buildDateNotificationAlert();
    if (dateAlert) alerts.DATE_NOTIFICATION = dateAlert;
    const protAlert = this.buildProtectionsAlert();
    if (protAlert) alerts.PROTECTIONS = protAlert;
    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    return { total: values.length };
  });

  // Liste des descripteurs pour itération template (chips, libellés).
  readonly protectionLabels = PROTECTION_LABELS;

  constructor(
    private service: LicenciementNulDetectionService,
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
    if (this.workspaceCountry === 'FRANCE') {
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

  explanationFor(field: LNDAlertField): SourceExplanation[] {
    const key = (() => {
      switch (field) {
        case 'SALAIRE': return 'salaire_brut_mensuel';
        case 'DATE_NOTIFICATION': return 'LND_DATE_NOTIFICATION';
        case 'PROTECTIONS': return 'LND_PROTECTIONS';
      }
    })();
    return this.sourceExplanations().get(key) ?? [];
  }

  alertTooltip(alert: LNDCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: LNDCoherenceAlert): string {
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

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  formValid(): boolean {
    const salaire = this.salaireMensuelBrutEur();
    const dateNotif = this.dateNotificationLicenciement();
    return salaire !== null && salaire > 0
      && dateNotif !== null && ISO_DATE_REGEX.test(dateNotif);
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // --- Handlers manuels — effacent le badge IA correspondant ---

  onSalaireChange(value: number | null): void {
    this.salaireMensuelBrutEur.set(value);
    this.provenanceSalaire.set(null);
  }

  onAncienneteChange(value: number | null): void {
    this.ancienneteAnnees.set(value);
  }

  onDateNotificationChange(value: string | null): void {
    this.dateNotificationLicenciement.set(value || null);
    this.provenanceDateNotification.set(null);
  }

  onDateAccouchementChange(value: string | null): void {
    this.dateAccouchement.set(value || null);
  }

  onDateConsolidationATChange(value: string | null): void {
    this.dateConsolidationAT.set(value || null);
  }

  onSalarieEnceinteChange(value: boolean): void {
    this.salarieEnceinte.set(value);
    this.provenanceProtections.set(null);
    if (!value) this.dateAccouchement.set(null);
  }

  onSalarieAccidentTravailChange(value: boolean): void {
    this.salarieAccidentTravail.set(value);
    this.provenanceProtections.set(null);
    if (!value) this.dateConsolidationAT.set(null);
  }

  onSalarieHarceleAvereChange(value: boolean): void {
    this.salarieHarceleAvere.set(value);
    this.provenanceProtections.set(null);
  }

  onSalarieDiscriminationAllegueeChange(value: boolean): void {
    this.salarieDiscriminationAlleguee.set(value);
    this.provenanceProtections.set(null);
  }

  onSalarieMotifLanceurAlerteChange(value: boolean): void {
    this.salarieMotifLanceurAlerte.set(value);
    this.provenanceProtections.set(null);
  }

  onSalarieMandatRepresentantChange(value: boolean): void {
    this.salarieMandatRepresentant.set(value);
    this.provenanceProtections.set(null);
  }

  onSalarieActionJusticeChange(value: boolean): void {
    this.salarieActionJustice.set(value);
    this.provenanceProtections.set(null);
  }

  /**
   * SF-130-01 : true si l'IA a déduit le salaire brut d'un montant net
   * (× 1,30) — note discrète sous le champ.
   */
  salaireEstDeduit(): boolean {
    return this.aiDataSignal()?.salaireEstDeduit === true;
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request: LicenciementNulDetectionRequest = {
      dateNotificationLicenciement: this.dateNotificationLicenciement()!,
      salarieEnceinte: this.salarieEnceinte(),
      dateAccouchement: this.dateAccouchement() || null,
      salarieAccidentTravail: this.salarieAccidentTravail(),
      dateConsolidationAT: this.dateConsolidationAT() || null,
      salarieHarceleAvere: this.salarieHarceleAvere(),
      salarieDiscriminationAlleguee: this.salarieDiscriminationAlleguee(),
      salarieMotifLanceurAlerte: this.salarieMotifLanceurAlerte(),
      salarieMandatRepresentant: this.salarieMandatRepresentant(),
      salarieActionJustice: this.salarieActionJustice(),
      salaireMensuelBrutEur: this.salaireMensuelBrutEur()!,
      ancienneteAnnees: this.ancienneteAnnees(),
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.applyResult(r);
        this.calculating.set(false);
        this.snackBar.open('Détection licenciement nul calculée', 'OK', { duration: 2500 });
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
        this.applyResult(r);
        this.loading.set(false);
      },
      error: () => {
        // 404 attendu si aucune analyse — on reste en mode formulaire.
        this.prefillFromAi();
        this.loading.set(false);
      },
    });
  }

  private applyResult(r: LicenciementNulDetectionResponse): void {
    this.result.set(r);
    this.dateNotificationLicenciement.set(r.dateNotificationLicenciement);
    this.salarieEnceinte.set(r.salarieEnceinte);
    this.dateAccouchement.set(r.dateAccouchement);
    this.salarieAccidentTravail.set(r.salarieAccidentTravail);
    this.dateConsolidationAT.set(r.dateConsolidationAT);
    this.salarieHarceleAvere.set(r.salarieHarceleAvere);
    this.salarieDiscriminationAlleguee.set(r.salarieDiscriminationAlleguee);
    this.salarieMotifLanceurAlerte.set(r.salarieMotifLanceurAlerte);
    this.salarieMandatRepresentant.set(r.salarieMandatRepresentant);
    this.salarieActionJustice.set(r.salarieActionJustice);
    this.salaireMensuelBrutEur.set(r.salaireMensuelBrutEur);
    this.ancienneteAnnees.set(r.ancienneteAnnees);
    // Valeurs persistées = saisie avocat — jamais de badge IA.
    this.provenanceSalaire.set(null);
    this.provenanceDateNotification.set(null);
    this.provenanceProtections.set(null);
    this.showForm.set(false);
  }

  /**
   * Pré-fill IA depuis `TravailExtractedData` :
   *  - `salaireBrutMensuel` → `salaireMensuelBrutEur`
   *  - `dateLicenciement` (ISO) → `dateNotificationLicenciement`
   *  - `motifNullitePressenti` → flag de protection correspondant (mapping ci-dessus)
   *
   * No-op gracieux si champ absent. N'écrase pas une saisie avocat
   * (provenance === null indique modification manuelle).
   */
  private prefillFromAi(): void {
    const ai = this.aiDataSignal();
    if (!ai) return;

    // F-236 SF-236-02 : valeurs calculées par le helper partagé (parité static).
    const ruleInput = { aiData: ai };

    const salaire = LicenciementNulDetectionSectionPrefillRules.computeSalaire(ruleInput);
    if (salaire !== null) {
      if (this.salaireMensuelBrutEur() === null || this.provenanceSalaire() === 'IA') {
        this.salaireMensuelBrutEur.set(salaire);
        this.provenanceSalaire.set('IA');
      }
    }

    const dateNotif = LicenciementNulDetectionSectionPrefillRules.computeDateNotification(ruleInput);
    if (dateNotif) {
      if (this.dateNotificationLicenciement() === null || this.provenanceDateNotification() === 'IA') {
        this.dateNotificationLicenciement.set(dateNotif);
        this.provenanceDateNotification.set('IA');
      }
    }

    const flag = LicenciementNulDetectionSectionPrefillRules.computeProtectionFlag(ruleInput) as
      | 'salarieEnceinte' | 'salarieAccidentTravail' | 'salarieHarceleAvere'
      | 'salarieDiscriminationAlleguee' | 'salarieMotifLanceurAlerte'
      | 'salarieMandatRepresentant' | 'salarieActionJustice' | null;
    if (flag) {
      const currentSignal = this.protectionSignalFor(flag);
      // N'écrase pas une saisie manuelle (provenance null AVEC une valeur true
      // signifie que l'avocat a coché — on n'efface pas).
      if (!currentSignal() || this.provenanceProtections() === 'IA') {
        currentSignal.set(true);
        this.provenanceProtections.set('IA');
      }
    }
  }

  private protectionSignalFor(
    flag: keyof Pick<LicenciementNulDetectionRequest,
      'salarieEnceinte' | 'salarieAccidentTravail' | 'salarieHarceleAvere'
      | 'salarieDiscriminationAlleguee' | 'salarieMotifLanceurAlerte'
      | 'salarieMandatRepresentant' | 'salarieActionJustice'>
  ) {
    switch (flag) {
      case 'salarieEnceinte': return this.salarieEnceinte;
      case 'salarieAccidentTravail': return this.salarieAccidentTravail;
      case 'salarieHarceleAvere': return this.salarieHarceleAvere;
      case 'salarieDiscriminationAlleguee': return this.salarieDiscriminationAlleguee;
      case 'salarieMotifLanceurAlerte': return this.salarieMotifLanceurAlerte;
      case 'salarieMandatRepresentant': return this.salarieMandatRepresentant;
      case 'salarieActionJustice': return this.salarieActionJustice;
    }
  }

  // ---------------------------------------------------------------------------
  // Builders d'alertes F-IA-03 (un par field)
  // ---------------------------------------------------------------------------

  private buildSalaireAlert(): LNDCoherenceAlert | null {
    const aiSalaire = this.aiDataSignal()?.salaireBrutMensuel;
    const userSalaire = this.salaireMensuelBrutEur();
    if (typeof aiSalaire !== 'number' || aiSalaire <= 0) return null;
    if (typeof userSalaire !== 'number' || userSalaire <= 0) return null;
    const ratio = Math.abs(userSalaire - aiSalaire) / aiSalaire;
    if (ratio <= SALAIRE_DIVERGENCE_RATIO) return null;
    const builder = CoherenceAlertBuilder.forField<LNDAlertField>('SALAIRE')
      .addSource('IA', {
        expectedDisplay: `${aiSalaire.toLocaleString('fr-FR')} €`,
        reason: `Analyse du dossier : salaire brut mensuel ~${aiSalaire.toLocaleString('fr-FR')} €`,
      });
    const piece = this.findPieceManquante(['SALAIRE_BRUT_MENSUEL', 'LND_SALAIRE']);
    if (piece) builder.addPieceManquante(piece);
    return builder.build();
  }

  private buildDateNotificationAlert(): LNDCoherenceAlert | null {
    const aiDate = this.aiDataSignal()?.dateLicenciement;
    const user = this.dateNotificationLicenciement();
    if (!user) return null;
    if (!aiDate || !ISO_DATE_REGEX.test(aiDate)) return null;
    if (user === aiDate) return null;
    const builder = CoherenceAlertBuilder.forField<LNDAlertField>('DATE_NOTIFICATION')
      .addSource('IA', {
        expectedDisplay: aiDate,
        reason: `Analyse du dossier : licenciement notifié le ${aiDate}`,
      });
    const piece = this.findPieceManquante(['LND_DATE_NOTIFICATION', 'DATE_LICENCIEMENT', 'LETTRE_LICENCIEMENT']);
    if (piece) builder.addPieceManquante(piece);
    return builder.build();
  }

  /**
   * Alerte si l'IA a détecté un motif (`motifNullitePressenti`) mais que
   * l'avocat n'a coché aucun toggle correspondant.
   */
  private buildProtectionsAlert(): LNDCoherenceAlert | null {
    const iaMotif = this.aiDataSignal()?.motifNullitePressenti?.toUpperCase();
    if (!iaMotif) return null;
    const flag = AI_MOTIF_TO_PROTECTION_FLAG[iaMotif];
    if (!flag) return null;
    const userToggleOn = this.protectionSignalFor(flag)();
    if (userToggleOn) return null; // Avocat aligne avec l'IA — pas d'alerte.
    const motifLabel = this.motifNullitePressentiLabel(iaMotif);
    const builder = CoherenceAlertBuilder.forField<LNDAlertField>('PROTECTIONS')
      .addSource('IA', {
        expectedDisplay: motifLabel,
        reason: `Analyse du dossier : motif de nullité pressenti "${motifLabel}" — vérifier le toggle correspondant`,
      });
    const piece = this.findPieceManquante(['LND_PROTECTIONS', 'LND_MOTIF', 'PIECES_PROTECTION']);
    if (piece) builder.addPieceManquante(piece);
    return builder.build();
  }

  private motifNullitePressentiLabel(iaMotif: string): string {
    switch (iaMotif) {
      case 'HARCELEMENT_MORAL': return 'Harcèlement moral';
      case 'HARCELEMENT_SEXUEL': return 'Harcèlement sexuel';
      case 'DISCRIMINATION': return 'Discrimination';
      case 'MATERNITE_PATERNITE': return 'Maternité / paternité';
      case 'ACCIDENT_MP': return 'Accident du travail / maladie pro.';
      case 'RETORSION': return 'Action en justice / représailles';
      case 'SYNDICAL': return 'Mandat syndical (salarié protégé)';
      default: return iaMotif;
    }
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

  // ---------------------------------------------------------------------------
  // Helpers d'affichage du résultat
  // ---------------------------------------------------------------------------

  verdictBannerClass(verdict: LicenciementNulVerdict): string {
    switch (verdict) {
      case 'ELEVEE':
        return 'lnd-verdict-banner lnd-verdict-banner--available';
      case 'MOYENNE':
        return 'lnd-verdict-banner lnd-verdict-banner--medium';
      case 'FAIBLE':
        return 'lnd-verdict-banner lnd-verdict-banner--danger';
    }
  }

  verdictBannerLabel(verdict: LicenciementNulVerdict): string {
    switch (verdict) {
      case 'ELEVEE': return 'Probabilité de nullité élevée';
      case 'MOYENNE': return 'Probabilité de nullité moyenne';
      case 'FAIBLE': return 'Probabilité de nullité faible';
    }
  }

  verdictBannerIcon(verdict: LicenciementNulVerdict): string {
    switch (verdict) {
      case 'ELEVEE': return 'check_circle';
      case 'MOYENNE': return 'warning';
      case 'FAIBLE': return 'error';
    }
  }

  /** Classe CSS du chip protection — palette navy par défaut. */
  protectionChipClass(_code: LicenciementNulProtection): string {
    return 'lnd-chip lnd-chip--available';
  }

  /** Liste ordonnée des protections détectées (ordre stable backend). */
  protectionsList(): LicenciementNulProtection[] {
    return this.result()?.protectionsDetectees ?? [];
  }
}
