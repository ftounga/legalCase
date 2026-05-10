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
import { DocumentsFinContratService } from '../../core/services/documents-fin-contrat.service';
import {
  DocumentsFinContratRequest,
  DocumentsFinContratResponse,
} from '../../core/models/documents-fin-contrat.model';
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
  DocumentsFinContratSectionPrefillRules,
} from './documents-fin-contrat-section-prefill-rules';

/**
 * SF-DT-32-02 : champs alertables F-IA-03 exposés par l'outil F-DT-32
 * (documents de fin de contrat). Seuls 2 champs sont alimentés par l'IA
 * travail (`salaireBrutMensuel` → SALAIRE, `dateLicenciement` → DATE_FIN_CONTRAT).
 * Les flags booléens et dates de remise sont saisis par l'avocat — pas
 * d'alerte IA possible.
 */
export type DfcAlertField = 'SALAIRE' | 'DATE_FIN_CONTRAT';

// SF-155-05 : alias locaux compatibles avec l'interface partagée.
export type DfcAlertSource = CoherenceAlertSource;
export type DfcCoherenceAlert = CoherenceAlert<DfcAlertField>;

/**
 * Seuil d'écart relatif au-delà duquel on déclenche une alerte de cohérence
 * salaire (aligné F-DT-09 / F-DT-11 / F-DT-26).
 */
const SALAIRE_DIVERGENCE_RATIO = 0.10;

/**
 * SF-DT-32-02 : outil décisionnel "Documents de fin de contrat" (F-DT-32).
 * FRANCE uniquement (BE = feature jumelle backlog : C4 ONEM, certificat
 * Loi 03/07/1978 art. 22, attestation vacances).
 *
 * Consomme l'API SF-DT-32-01 mergée PR #595.
 *
 * Affiché par le panel F-IA-04 (tool_id `F-DT-32-documents-fin-contrat`,
 * règle ALWAYS_ON FR + travail, priority 59).
 *
 * Patterns de référence :
 * - Template canonique : `harcelement-licenciement-nul-section` (F-DT-11-02).
 * - Multi-fields + dates + score : `conges-payes-section` (F-DT-26-02).
 * - Multi-cards / multi-items : `divorce-checklist-section` (F-FA-07-02).
 * - Pattern IA : `immigration-title-decision-section` (F-IM-05) +
 *   `harcelement-licenciement-nul-section` (F-DT-11-02).
 * - Helper partagé : `CoherenceAlertBuilder` + `CoherenceAlert<F>` (SF-155-05).
 */
@Component({
  selector: 'app-documents-fin-contrat-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule, DecimalPipe,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatSlideToggleModule,
    MatProgressSpinnerModule,
    LegalCitationsPipe,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './documents-fin-contrat-section.component.html',
  styleUrl: './documents-fin-contrat-section.component.scss',
})
export class DocumentsFinContratSectionComponent implements OnInit, OnChanges {
  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'DOCUMENTS DE FIN DE CONTRAT — CONFORMITÉ';

  /** F-177 SF-177-12 / F-236 SF-236-02 — délègue au helper partagé (parité runtime). */
  static getPrefillCount(input: {
    aiData?: any;
    procedureChecks?: any[];
    aiQuestions?: any[];
    piecesManquantes?: any[];
    triggerEvents?: any[];
    workspaceCountry?: string;
  }): number {
    return DocumentsFinContratSectionPrefillRules.computePrefillCount({
      aiData: input.aiData,
      workspaceCountry: input.workspaceCountry,
    });
  }

  static readonly TOOL_ICON = 'fact_check';

  @Input() caseFileId!: string;
  // F-177 SF-177-03b : force l'expansion (mode modal F-177).
  @Input() forceExpanded = false;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';
  // SF-155-04 : inputs IA (tous optionnels — null-safe partout).
  @Input() aiData?: TravailExtractedData | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;

  // Snapshots signal des inputs IA pour que `computed` réagisse.
  private aiDataSignal = signal<TravailExtractedData | null | undefined>(undefined);
  private procedureChecksSignal = signal<ProcedureCheck[]>([]);
  private aiQuestionsSignal = signal<AiQuestion[]>([]);
  private piecesManquantesSignal = signal<PieceManquanteEntry[]>([]);

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<DocumentsFinContratResponse | null>(null);

  // --- Champs envoyés au backend ---
  dateFinContrat = signal<string | null>(null);
  certificatTravailRemis = signal<boolean>(false);
  dateCertificatTravail = signal<string | null>(null);
  attestationFranceTravailRemise = signal<boolean>(false);
  dateAttestationFranceTravail = signal<string | null>(null);
  souldeToutCompteSigne = signal<boolean>(false);
  dateSouldeToutCompte = signal<string | null>(null);
  salaireMensuelBrutEur = signal<number | null>(null);

  // --- Toggle UI-side informatif (NON envoyé au backend) ---
  // Le backend calcule `souldeToutCompteContestable` lui-même via la fenêtre
  // 6 mois L.1234-20 al. 2. Ce toggle est purement informatif côté avocat.
  souldeToutCompteContestableDelai6mois = signal<boolean>(false);

  // SF-155-04 : provenance IA par champ.
  provenanceSalaire = signal<'IA' | null>(null);
  provenanceDateFinContrat = signal<'IA' | null>(null);

  // SF-IA-03-15c : map {sourceKey → explanations} pour le popover.
  sourceExplanations = signal<Map<string, SourceExplanation[]>>(new Map());

  /** Aujourd'hui en YYYY-MM-DD pour l'attribut max des inputs date. */
  readonly todayIso: string = new Date().toISOString().slice(0, 10);

  /**
   * Alertes de cohérence F-IA-03 par field.
   * Gate : uniquement en mode formulaire (pattern anti-bug SF-IA-03-12 —
   * pas d'alertes après que le calcul est rendu).
   */
  coherenceAlerts = computed<Partial<Record<DfcAlertField, DfcCoherenceAlert>>>(() => {
    if (!this.showForm()) return {};
    const alerts: Partial<Record<DfcAlertField, DfcCoherenceAlert>> = {};
    const salaireA = this.buildSalaireAlert();
    if (salaireA) alerts.SALAIRE = salaireA;
    const dateA = this.buildDateFinContratAlert();
    if (dateA) alerts.DATE_FIN_CONTRAT = dateA;
    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    // Outil indemnitaire / scoring — jamais bloquant, jamais CRITICAL.
    return { total: values.length, blockers: 0 };
  });

  constructor(
    private service: DocumentsFinContratService,
    private snackBar: MatSnackBar,
    @Optional() private dashboardRefresh: CaseDashboardRefreshService | null,
    @Optional() private sourceExplanationService: SourceExplanationService | null,
  ) {}

  ngOnInit(): void {
    // F-177 SF-177-03b : appliqué dès le mount pour le mode modal.
    if (this.forceExpanded) this.collapsed.set(false);

    // Push inputs vers signals avant toute évaluation computed.
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

    // Ré-applique le prefill si aiData change avant première résolution.
    if (changes['aiData'] && !changes['aiData'].firstChange
        && this.workspaceCountry === 'FRANCE' && this.showForm() && !this.result()) {
      this.prefillFromAi();
    }
  }

  /**
   * SF-DT-32-02 : pré-fill depuis `aiData` (TravailExtractedData).
   * Règles (fail-open) :
   * - passe silencieusement si aiData absent
   * - chaque champ indépendant (prefill partiel OK)
   * - ne pré-remplit QUE si le champ est encore vide ou déjà 'IA'
   * - n'écrase jamais une saisie manuelle de l'avocat
   */
  private prefillFromAi(): void {
    const ai = this.aiDataSignal();
    if (!ai) return;
    if (this.workspaceCountry !== 'FRANCE') return;

    // F-236 SF-236-02 : valeurs calculées par le helper partagé (parité static).
    const ruleInput = { aiData: ai, workspaceCountry: this.workspaceCountry };

    const salaire = DocumentsFinContratSectionPrefillRules.computeSalaireMensuelBrutEur(ruleInput);
    if (salaire !== null) {
      if (this.salaireMensuelBrutEur() === null || this.provenanceSalaire() === 'IA') {
        this.salaireMensuelBrutEur.set(salaire);
        this.provenanceSalaire.set('IA');
      }
    }

    const dateFinContrat = DocumentsFinContratSectionPrefillRules.computeDateFinContrat(ruleInput);
    if (dateFinContrat !== null) {
      if (this.dateFinContrat() === null || this.provenanceDateFinContrat() === 'IA') {
        this.dateFinContrat.set(dateFinContrat);
        this.provenanceDateFinContrat.set('IA');
      }
    }
  }

  private loadSourceExplanations(): void {
    if (!this.caseFileId || !this.sourceExplanationService) return;
    this.sourceExplanationService.getForCaseFile(this.caseFileId).subscribe({
      next: (map) => this.sourceExplanations.set(map),
      error: () => { /* fail-open */ },
    });
  }

  /** SF-IA-03-15c : map field → sourceKey. */
  explanationFor(field: DfcAlertField): SourceExplanation[] {
    const key = field === 'SALAIRE' ? 'salaire_brut_mensuel'
              : field === 'DATE_FIN_CONTRAT' ? 'date_licenciement'
              : '';
    return this.sourceExplanations().get(key) ?? [];
  }

  alertTooltip(alert: DfcCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: DfcCoherenceAlert): string {
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
   * Divergence salaire — écart relatif > 10 % entre IA et saisie avocat.
   * Construit via CoherenceAlertBuilder partagé (SF-155-05).
   * Multi-sources : IA + PIECE_MANQUANTE (fiche de paie manquante).
   */
  private buildSalaireAlert(): DfcCoherenceAlert | null {
    const aiSalaire = this.aiDataSignal()?.salaireBrutMensuel;
    const userSalaire = this.salaireMensuelBrutEur();
    if (typeof aiSalaire !== 'number' || aiSalaire <= 0) return null;
    if (typeof userSalaire !== 'number' || userSalaire <= 0) return null;
    const ratio = Math.abs(userSalaire - aiSalaire) / aiSalaire;
    if (ratio <= SALAIRE_DIVERGENCE_RATIO) return null;
    const builder = CoherenceAlertBuilder.forField<DfcAlertField>('SALAIRE')
      .addSource('IA', {
        expectedDisplay: `${aiSalaire.toLocaleString('fr-FR')} €`,
        reason: `Analyse du dossier : salaire brut mensuel ~${aiSalaire.toLocaleString('fr-FR')} €`,
      });
    const piece = this.findPieceManquante(['SALAIRE_BRUT_MENSUEL', 'DFC_SALAIRE']);
    if (piece) builder.addPieceManquante(piece);
    return builder.build();
  }

  /**
   * Divergence `dateLicenciement` IA vs `dateFinContrat` saisie.
   * Toute différence est suspecte (fin du contrat = 1 seule date).
   * Multi-sources : IA + PIECE_MANQUANTE (lettre licenciement / fin contrat).
   */
  private buildDateFinContratAlert(): DfcCoherenceAlert | null {
    const userDate = this.dateFinContrat();
    if (!userDate) return null;
    const ai = this.aiDataSignal();
    const aiDate = typeof ai?.dateLicenciement === 'string' ? ai.dateLicenciement : null;
    if (!aiDate || aiDate === userDate) return null;
    const builder = CoherenceAlertBuilder.forField<DfcAlertField>('DATE_FIN_CONTRAT')
      .withSeverity('WARNING')
      .addSource('IA', {
        expectedDisplay: aiDate,
        reason: `Analyse du dossier : date de fin de contrat ${aiDate}`,
      });
    const piece = this.findPieceManquante(['DATE_LICENCIEMENT', 'DFC_DATE_FIN']);
    if (piece) builder.addPieceManquante(piece);
    return builder.build();
  }

  /**
   * Renvoie le `texte` d'une PieceManquanteEntry dont `critereCode`
   * (case-insensitive) appartient à la liste des codes acceptés, ou `null`.
   */
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
    const date = this.dateFinContrat();
    const salaire = this.salaireMensuelBrutEur();
    if (!date) return false;
    if (date > this.todayIso) return false;
    if (salaire === null || salaire <= 0) return false;
    // Si un toggle de remise est activé, sa date doit être saisie.
    if (this.certificatTravailRemis() && !this.dateCertificatTravail()) return false;
    if (this.attestationFranceTravailRemise() && !this.dateAttestationFranceTravail()) return false;
    if (this.souldeToutCompteSigne() && !this.dateSouldeToutCompte()) return false;
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // --- Handlers onChange — effacent la provenance IA au 1er changement manuel. ---
  onDateFinContratChange(value: string | null): void {
    this.dateFinContrat.set(value || null);
    this.provenanceDateFinContrat.set(null);
  }

  onSalaireChange(value: number | null): void {
    this.salaireMensuelBrutEur.set(value);
    this.provenanceSalaire.set(null);
  }

  onCertificatRemisChange(value: boolean): void {
    this.certificatTravailRemis.set(value);
    if (!value) this.dateCertificatTravail.set(null);
  }

  onDateCertificatChange(value: string | null): void {
    this.dateCertificatTravail.set(value || null);
  }

  onAttestationRemiseChange(value: boolean): void {
    this.attestationFranceTravailRemise.set(value);
    if (!value) this.dateAttestationFranceTravail.set(null);
  }

  onDateAttestationChange(value: string | null): void {
    this.dateAttestationFranceTravail.set(value || null);
  }

  onSouldeSigneChange(value: boolean): void {
    this.souldeToutCompteSigne.set(value);
    if (!value) {
      this.dateSouldeToutCompte.set(null);
      this.souldeToutCompteContestableDelai6mois.set(false);
    }
  }

  onDateSouldeChange(value: string | null): void {
    this.dateSouldeToutCompte.set(value || null);
  }

  onSouldeContestableChange(value: boolean): void {
    this.souldeToutCompteContestableDelai6mois.set(value);
  }

  // --- Helpers d'affichage des cartes documents ---

  certificatStatutClass(): string {
    const r = this.result();
    if (!r) return '';
    if (r.certificatRemisDansDelai) return 'dfc-card--ok';
    return r.certificatTravailRemis ? 'dfc-card--retard' : 'dfc-card--manquant';
  }

  certificatStatutLabel(): string {
    const r = this.result();
    if (!r) return '';
    if (r.certificatRemisDansDelai) return 'OK — délai respecté';
    return r.certificatTravailRemis ? 'Hors délai' : 'Non remis';
  }

  attestationStatutClass(): string {
    const r = this.result();
    if (!r) return '';
    if (r.attestationRemiseDansDelai) return 'dfc-card--ok';
    return r.attestationFranceTravailRemise ? 'dfc-card--retard' : 'dfc-card--manquant';
  }

  attestationStatutLabel(): string {
    const r = this.result();
    if (!r) return '';
    if (r.attestationRemiseDansDelai) return 'OK — délai respecté';
    return r.attestationFranceTravailRemise ? 'Hors délai' : 'Non remis';
  }

  stcStatutClass(): string {
    const r = this.result();
    if (!r) return '';
    if (!r.souldeToutCompteSigne) return 'dfc-card--manquant';
    return r.souldeToutCompteValide ? 'dfc-card--ok' : 'dfc-card--retard';
  }

  stcStatutLabel(): string {
    const r = this.result();
    if (!r) return '';
    if (!r.souldeToutCompteSigne) return 'Non signé';
    return r.souldeToutCompteValide ? 'OK — signé dans les 30 j.' : 'Hors délai 30 j.';
  }

  verdictClass(): string {
    const r = this.result();
    if (!r) return '';
    return `dfc-verdict--${r.verdictRisqueContentieux.toLowerCase()}`;
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request: DocumentsFinContratRequest = {
      dateFinContrat: this.dateFinContrat()!,
      certificatTravailRemis: this.certificatTravailRemis(),
      dateCertificatTravail: this.dateCertificatTravail(),
      attestationFranceTravailRemise: this.attestationFranceTravailRemise(),
      dateAttestationFranceTravail: this.dateAttestationFranceTravail(),
      souldeToutCompteSigne: this.souldeToutCompteSigne(),
      dateSouldeToutCompte: this.dateSouldeToutCompte(),
      salaireMensuelBrutEur: this.salaireMensuelBrutEur()!,
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Conformité documents calculée', 'OK', { duration: 2500 });
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
        this.dateFinContrat.set(r.dateFinContrat);
        this.certificatTravailRemis.set(r.certificatTravailRemis === true);
        this.dateCertificatTravail.set(r.dateCertificatTravail);
        this.attestationFranceTravailRemise.set(r.attestationFranceTravailRemise === true);
        this.dateAttestationFranceTravail.set(r.dateAttestationFranceTravail);
        this.souldeToutCompteSigne.set(r.souldeToutCompteSigne === true);
        this.dateSouldeToutCompte.set(r.dateSouldeToutCompte);
        this.salaireMensuelBrutEur.set(r.salaireMensuelBrutEur);
        this.souldeToutCompteContestableDelai6mois.set(r.souldeToutCompteContestable);
        // Valeurs persistées = saisie avocat — jamais de badge IA.
        this.provenanceSalaire.set(null);
        this.provenanceDateFinContrat.set(null);
        this.showForm.set(false);
        this.loading.set(false);
      },
      error: () => {
        // 404 attendu si aucune analyse — mode formulaire + pré-fill IA.
        this.prefillFromAi();
        this.loading.set(false);
      },
    });
  }
}
