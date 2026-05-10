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
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSnackBar } from '@angular/material/snack-bar';
import { AesHumanitaireService } from '../../core/services/aes-humanitaire.service';
import {
  AesHumanitaireRequest,
  AesHumanitaireResponse,
  MotifHumanitaire,
} from '../../core/models/aes-humanitaire.model';
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
import { AesHumanitairePrefillRules } from './aes-humanitaire-section-prefill-rules';
import { SourceExplanation } from '../../core/models/source-explanation.model';
import { SourceExplanationService } from '../../core/services/source-explanation.service';

/**
 * SF-IM-09-07 : champs d'alerte de cohérence F-IA-03 exposés par l'outil
 * F-IM-09 AES voie humanitaire (L.435-2).
 */
export type AesHumanitaireAlertField =
  | 'DATE_ENTREE_FRANCE'
  | 'MOTIF_HUMANITAIRE'
  | 'DATE_DEPOT_DEMANDE';

export type AesHumanitaireAlertSource = CoherenceAlertSource;
export type AesHumanitaireCoherenceAlert = CoherenceAlert<AesHumanitaireAlertField>;

/** Regex ISO strict YYYY-MM-DD. */
const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

interface MotifOption {
  code: MotifHumanitaire;
  label: string;
}

const MOTIF_OPTIONS: ReadonlyArray<MotifOption> = [
  { code: 'RISQUES_AU_RETOUR', label: 'Risques personnels au retour' },
  { code: 'ISOLEMENT_TOTAL', label: 'Isolement total (pas de famille au pays)' },
  { code: 'VICTIME_VIOLENCES', label: 'Victime de violences' },
  { code: 'VICTIME_TRAITE', label: 'Victime de traite / proxénétisme' },
  {
    code: 'SITUATION_MEDICALE_PRECAIRE_HORS_L425_9',
    label: 'Situation médicale précaire (hors L.425-9)',
  },
  { code: 'AUTRE_HUMANITAIRE', label: 'Autre motif humanitaire' },
];

/**
 * SF-IM-09-07 : outil décisionnel AES voie humanitaire (L.435-2 CESEDA +
 * L.432-14 commission du titre de séjour). Single-country FRANCE — bannière
 * info pour BE.
 *
 * Pattern canonique : `harcelement-licenciement-nul-section` (F-DT-11-02) +
 * pré-fill IA / cohérence F-IA-03 (`immigration-title-decision-section`,
 * F-IM-05-04). Patterns directs : `aes-famille-section` (SF-IM-09-06) et
 * `aes-metiers-tension-section` (SF-IM-09-05).
 */
@Component({
  selector: 'app-aes-humanitaire-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatChipsModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatProgressSpinnerModule, MatSelectModule, MatSlideToggleModule,
    LegalCitationsPipe,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './aes-humanitaire-section.component.html',
  styleUrl: './aes-humanitaire-section.component.scss',
})
export class AesHumanitaireSectionComponent implements OnInit, OnChanges {
  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'AES — VOIE HUMANITAIRE (L.435-2)';
  static readonly TOOL_ICON = 'volunteer_activism';

  /** F-236 SF-236-02 — Délègue intégralement au helper pur partagé. */
  static getPrefillCount(input: PrefillCountInput): number {
    return AesHumanitairePrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';
  @Input() aiData?: ImmigrationExtractedData | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;
  // F-177 SF-177-03b : force l'expansion (mode modal F-177).
  @Input() forceExpanded = false;

  // Signals miroirs des inputs IA (réactivité post-mount, pattern canonique
  // F-155 SF-155-06).
  private aiDataSignal = signal<ImmigrationExtractedData | null | undefined>(undefined);
  private procedureChecksSignal = signal<ProcedureCheck[]>([]);
  private aiQuestionsSignal = signal<AiQuestion[]>([]);
  private piecesManquantesSignal = signal<PieceManquanteEntry[]>([]);

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<AesHumanitaireResponse | null>(null);

  // Form signals.
  dateEntreeFrance = signal<string | null>(null);
  motifHumanitaireDominant = signal<MotifHumanitaire | null>(null);
  preuvesMedicales = signal(false);
  preuvesViolencesOuTraite = signal(false);
  demandeAsileDeposeeEtRejetee = signal(false);
  commissionTitreSejourSaisie = signal(false);
  menaceOrdrePublic = signal(false);
  dateDepotDemande = signal<string | null>(null);

  // Provenance IA (badges "Pré-rempli depuis l'analyse").
  provenanceDateEntree = signal<'IA' | null>(null);
  provenanceMotif = signal<'IA' | null>(null);
  provenanceDateDepot = signal<'IA' | null>(null);

  // Source explanations (F-IA-03-15c).
  sourceExplanations = signal<Map<string, SourceExplanation[]>>(new Map());

  /** Aujourd'hui en YYYY-MM-DD pour `max` des inputs date. */
  readonly todayIso: string = new Date().toISOString().slice(0, 10);

  readonly motifOptions: ReadonlyArray<MotifOption> = MOTIF_OPTIONS;

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  // Alertes de cohérence F-IA-03 (computed).
  coherenceAlerts = computed<Partial<Record<AesHumanitaireAlertField, AesHumanitaireCoherenceAlert>>>(() => {
    if (!this.showForm()) return {};
    if (!this.isFrance()) return {};
    const alerts: Partial<Record<AesHumanitaireAlertField, AesHumanitaireCoherenceAlert>> = {};
    const a1 = this.buildDateEntreeAlert();
    if (a1) alerts.DATE_ENTREE_FRANCE = a1;
    const a2 = this.buildMotifAlert();
    if (a2) alerts.MOTIF_HUMANITAIRE = a2;
    const a3 = this.buildDateDepotAlert();
    if (a3) alerts.DATE_DEPOT_DEMANDE = a3;
    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    const blockers = values.filter((a) => a?.severity === 'CRITICAL').length;
    return { total: values.length, blockers };
  });

  constructor(
    private service: AesHumanitaireService,
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
    if (this.isFrance()) {
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

    // Re-prefill quand aiData arrive après mount (avant première résolution).
    if (changes['aiData'] && !changes['aiData'].firstChange
        && this.isFrance() && this.showForm() && !this.result()) {
      this.prefillFromAi();
    }
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.applyResultToForm(r);
        this.provenanceDateEntree.set(null);
        this.provenanceMotif.set(null);
        this.provenanceDateDepot.set(null);
        this.showForm.set(false);
        this.loading.set(false);
      },
      error: () => {
        this.prefillFromAi();
        this.loading.set(false);
      },
    });
  }

  private loadSourceExplanations(): void {
    if (!this.caseFileId || !this.sourceExplanationService) return;
    this.sourceExplanationService.getForCaseFile(this.caseFileId).subscribe({
      next: (map) => this.sourceExplanations.set(map),
      error: () => { /* fail-open */ },
    });
  }

  /**
   * Pré-remplit le form depuis aiData. `ImmigrationExtractedData` n'expose
   * pas `dateEntreeFrance` typée — fallback gracieux via cast (pattern
   * SF-IM-09-06 aes-famille). Le motif humanitaire n'a pas non plus de
   * champ IA dédié à ce stade : no-op gracieux.
   */
  private prefillFromAi(): void {
    // F-236 SF-236-02 : délègue au helper pur partagé.
    if (!this.isFrance()) return;
    const input: PrefillCountInput = {
      aiData: this.aiDataSignal(),
      workspaceCountry: this.workspaceCountry,
    };
    const entree = AesHumanitairePrefillRules.computeDateEntreeFrance(input);
    if (entree !== null
        && (this.dateEntreeFrance() === null || this.provenanceDateEntree() === 'IA')) {
      this.dateEntreeFrance.set(entree);
      this.provenanceDateEntree.set('IA');
    }
    const depot = AesHumanitairePrefillRules.computeDateDepotDemande(input);
    if (depot !== null
        && (this.dateDepotDemande() === null || this.provenanceDateDepot() === 'IA')) {
      this.dateDepotDemande.set(depot);
      this.provenanceDateDepot.set('IA');
    }
  }

  /**
   * Mapping field → sourceKey pour le popover F-IA-03-15c.
   * Convention `IM09H_<FIELD>` (humanitaire) — alignée AES famille / métiers tension.
   */
  explanationFor(field: AesHumanitaireAlertField): SourceExplanation[] {
    const key = (() => {
      switch (field) {
        case 'DATE_ENTREE_FRANCE': return 'IM09H_DATE_ENTREE_FRANCE';
        case 'MOTIF_HUMANITAIRE': return 'IM09H_MOTIF_HUMANITAIRE';
        case 'DATE_DEPOT_DEMANDE': return 'IM09H_DATE_DEPOT_DEMANDE';
      }
    })();
    return this.sourceExplanations().get(key) ?? [];
  }

  alertTooltip(alert: AesHumanitaireCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: AesHumanitaireCoherenceAlert): string {
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

  private buildDateEntreeAlert(): AesHumanitaireCoherenceAlert | null {
    const user = this.dateEntreeFrance();
    if (!user) return null;
    const builder = CoherenceAlertBuilder
      .forField<AesHumanitaireAlertField>('DATE_ENTREE_FRANCE')
      .withSeverity('WARNING');

    // IA — valeur extraite de l'analyse (champ non typé natif).
    const ai = this.aiDataSignal();
    const aiDate = (ai as { dateEntreeFrance?: string | null } | null | undefined)?.dateEntreeFrance;
    if (typeof aiDate === 'string' && aiDate.length >= 10) {
      const aiPart = aiDate.substring(0, 10);
      if (aiPart !== user) {
        builder.addSource('IA', {
          expectedDisplay: aiPart,
          reason: `Analyse du dossier : date d'entrée en France ${aiPart}`,
        });
      }
    }

    // F96 — checklist procédurale `IM09H_DATE_ENTREE_FRANCE`.
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'IM09H_DATE_ENTREE_FRANCE') continue;
      const ev = chk.expectedValue;
      if (!ev || !ISO_DATE_RE.test(ev) || ev === user) continue;
      builder.addSource('F96', {
        expectedDisplay: ev,
        reason: `Checklist procédurale : date d'entrée attendue ${ev}${chk.raison ? ' (' + chk.raison + ')' : ''}`,
      });
      break;
    }

    // PIECE_MANQUANTE (passeport, justificatif domicile à l'arrivée).
    const piece = this.findPieceManquante(['IM09H_DATE_ENTREE_FRANCE', 'IM09_PRESENCE_3_ANS']);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  private buildMotifAlert(): AesHumanitaireCoherenceAlert | null {
    const user = this.motifHumanitaireDominant();
    if (!user) return null;
    const builder = CoherenceAlertBuilder
      .forField<AesHumanitaireAlertField>('MOTIF_HUMANITAIRE')
      .withSeverity('WARNING');

    // F96 — checklist procédurale `IM09H_MOTIF_HUMANITAIRE` (valeur attendue
    // = code enum MotifHumanitaire).
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'IM09H_MOTIF_HUMANITAIRE') continue;
      const ev = chk.expectedValue?.toUpperCase();
      if (!ev || ev === user) continue;
      builder.addSource('F96', {
        expectedDisplay: this.labelForMotif(ev as MotifHumanitaire) ?? ev,
        reason: `Checklist procédurale : motif attendu ${ev}${chk.raison ? ' (' + chk.raison + ')' : ''}`,
      });
      break;
    }

    // QUESTION_IA — réponse à une question complémentaire sur le motif humanitaire.
    for (const q of this.aiQuestionsSignal()) {
      const code = q.critereCode?.toUpperCase();
      if (code !== 'IM09H_MOTIF_HUMANITAIRE') continue;
      const ans = q.expectedValue?.toUpperCase();
      if (!ans || ans === user) continue;
      builder.addSource('QUESTION_IA', {
        expectedDisplay: this.labelForMotif(ans as MotifHumanitaire) ?? ans,
        reason: `Question complémentaire : motif renseigné ${ans}`,
      });
      break;
    }

    // PIECE_MANQUANTE associée au motif humanitaire (certificats médicaux,
    // plaintes, etc.).
    const piece = this.findPieceManquante(['IM09H_MOTIF_HUMANITAIRE', 'IM09H_PREUVES_MEDICALES', 'IM09H_PREUVES_VIOLENCES']);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  private buildDateDepotAlert(): AesHumanitaireCoherenceAlert | null {
    const user = this.dateDepotDemande();
    if (!user) return null;
    const builder = CoherenceAlertBuilder
      .forField<AesHumanitaireAlertField>('DATE_DEPOT_DEMANDE')
      .withSeverity('WARNING');

    const ai = this.aiDataSignal()?.dateDepotProcedure;
    if (typeof ai === 'string' && ISO_DATE_RE.test(ai) && ai !== user) {
      builder.addSource('IA', {
        expectedDisplay: ai,
        reason: `L'analyse a détecté une date de dépôt différente : ${ai}`,
      });
    }

    const piece = this.findPieceManquante(['IM09H_DATE_DEPOT_DEMANDE']);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  /**
   * Recherche d'une `PieceManquanteEntry` matchant un des codes.
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

  labelForMotif(code: MotifHumanitaire | null): string | null {
    if (!code) return null;
    const found = MOTIF_OPTIONS.find((o) => o.code === code);
    return found ? found.label : code;
  }

  toggleCollapse(): void {
    this.collapsed.update((v) => !v);
  }

  formValid(): boolean {
    const date = this.dateEntreeFrance();
    if (!date || !ISO_DATE_RE.test(date) || date > this.todayIso) return false;
    if (!this.motifHumanitaireDominant()) return false;
    const dDepot = this.dateDepotDemande();
    if (dDepot && (!ISO_DATE_RE.test(dDepot) || dDepot < date)) return false;
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // Handlers — effacent provenance IA si l'avocat modifie manuellement.
  onDateEntreeChange(value: string | null): void {
    this.dateEntreeFrance.set(value || null);
    this.provenanceDateEntree.set(null);
  }

  onMotifChange(value: MotifHumanitaire | null): void {
    this.motifHumanitaireDominant.set(value);
    this.provenanceMotif.set(null);
  }

  onPreuvesMedicalesChange(value: boolean): void {
    this.preuvesMedicales.set(value);
  }

  onPreuvesViolencesChange(value: boolean): void {
    this.preuvesViolencesOuTraite.set(value);
  }

  onDemandeAsileChange(value: boolean): void {
    this.demandeAsileDeposeeEtRejetee.set(value);
  }

  onCommissionChange(value: boolean): void {
    this.commissionTitreSejourSaisie.set(value);
  }

  onMenaceOrdrePublicChange(value: boolean): void {
    this.menaceOrdrePublic.set(value);
  }

  onDateDepotChange(value: string | null): void {
    this.dateDepotDemande.set(value && value.length >= 10 ? value : null);
    this.provenanceDateDepot.set(null);
  }

  /**
   * Verdict bannière classe CSS — palette navy/or classique
   * (pas de rouge dominant — pas d'urgence < 72h).
   */
  verdictBannerClass(): string {
    const v = this.result()?.verdictProbabiliteAcceptation;
    if (v === 'ELEVEE') return 'aes-verdict-banner aes-verdict-banner--success';
    if (v === 'MOYENNE') return 'aes-verdict-banner aes-verdict-banner--info';
    return 'aes-verdict-banner aes-verdict-banner--danger';
  }

  verdictIcon(): string {
    const v = this.result()?.verdictProbabiliteAcceptation;
    if (v === 'ELEVEE') return 'check_circle';
    if (v === 'MOYENNE') return 'info_outline';
    return 'error';
  }

  verdictLabel(): string {
    const v = this.result()?.verdictProbabiliteAcceptation;
    if (v === 'ELEVEE') return 'Probabilité élevée';
    if (v === 'MOYENNE') return 'Probabilité moyenne';
    return 'Probabilité faible';
  }

  calculate(): void {
    if (!this.formValid()) return;
    if (!this.isFrance()) return;
    const motif = this.motifHumanitaireDominant();
    if (!motif) return;
    const request: AesHumanitaireRequest = {
      dateEntreeFrance: this.dateEntreeFrance()!,
      motifHumanitaireDominant: motif,
      preuvesMedicales: this.preuvesMedicales(),
      preuvesViolencesOuTraite: this.preuvesViolencesOuTraite(),
      demandeAsileDeposeeEtRejetee: this.demandeAsileDeposeeEtRejetee(),
      commissionTitreSejourSaisie: this.commissionTitreSejourSaisie(),
      menaceOrdrePublic: this.menaceOrdrePublic(),
      dateDepotDemande: this.dateDepotDemande(),
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Analyse AES humanitaire calculée', 'OK', { duration: 2500 });
        this.dashboardRefresh?.triggerRefresh();
      },
      error: (err) => {
        this.calculating.set(false);
        const msg = err?.error?.message || err?.error || 'Erreur lors du calcul';
        this.snackBar.open(String(msg), 'Fermer', { duration: 5000, panelClass: 'snack-error' });
      },
    });
  }

  private applyResultToForm(r: AesHumanitaireResponse): void {
    this.dateEntreeFrance.set(r.dateEntreeFrance);
    this.motifHumanitaireDominant.set(r.motifHumanitaireDominant);
    this.preuvesMedicales.set(r.preuvesMedicales);
    this.preuvesViolencesOuTraite.set(r.preuvesViolencesOuTraite);
    this.demandeAsileDeposeeEtRejetee.set(r.demandeAsileDeposeeEtRejetee);
    this.commissionTitreSejourSaisie.set(r.commissionTitreSejourSaisie);
    this.menaceOrdrePublic.set(r.menaceOrdrePublic);
    this.dateDepotDemande.set(r.dateDepotDemande);
  }
}
