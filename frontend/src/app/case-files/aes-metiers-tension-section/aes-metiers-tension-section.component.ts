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
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { AesMetiersTensionService } from '../../core/services/aes-metiers-tension.service';
import {
  AesMetiersTensionRequest,
  AesMetiersTensionResponse,
} from '../../core/models/aes-metiers-tension.model';
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
  CoherenceAlertSeverity,
} from '../../shared/coherence-popover/coherence-alert.model';
import { CoherenceAlertBuilder } from '../../shared/coherence-popover/coherence-alert-builder';
import { SourceExplanation } from '../../core/models/source-explanation.model';
import { SourceExplanationService } from '../../core/services/source-explanation.service';

/** Regex ISO strict YYYY-MM-DD. */
const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

export type AesMtAlertField = 'DATE_ENTREE_FRANCE' | 'MOIS_ACTIVITE' | 'DATE_DEPOT_DEMANDE';

export type AesMtAlertSeverity = CoherenceAlertSeverity;
export type AesMtCoherenceAlert = CoherenceAlert<AesMtAlertField>;

/**
 * SF-IM-09-05 : section frontend dédiée AES Métiers en tension (FR).
 * Loi n° 2024-42 du 26 janvier 2024 art. 26 → CESEDA L.435-4
 * (voie expérimentale 1er janvier 2024 — 31 décembre 2026).
 *
 * Single-country FR — Belgique : bannière info ("régime français
 * uniquement") + form masqué (pas d'appel HTTP). Cohérent avec F-IM-08
 * OQTF FR (oqtf-avec-delai-section).
 *
 * Template canonique F-155 SF-155-04 — pré-fill IA + alertes F-IA-03.
 * Pattern emprunté à `harcelement-licenciement-nul-section` (verdict +
 * formule + JetBrains Mono) et `oqtf-avec-delai-section` (gate FR).
 */
@Component({
  selector: 'app-aes-metiers-tension-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatSlideToggleModule, MatChipsModule, MatProgressSpinnerModule,
    LegalCitationsPipe,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './aes-metiers-tension-section.component.html',
  styleUrl: './aes-metiers-tension-section.component.scss',
})
export class AesMetiersTensionSectionComponent implements OnInit, OnChanges {
  @Input() caseFileId!: string;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';
  // SF-IM-09-05 : sources IA pour pré-fill + alertes de cohérence.
  @Input() aiData?: ImmigrationExtractedData | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;

  // Signals miroirs pour que `coherenceAlerts` réagisse aux changements
  // post-mount (pattern canonique F-155 SF-155-06).
  private aiDataSignal = signal<ImmigrationExtractedData | null | undefined>(undefined);
  private procedureChecksSignal = signal<ProcedureCheck[]>([]);
  private aiQuestionsSignal = signal<AiQuestion[]>([]);
  private piecesManquantesSignal = signal<PieceManquanteEntry[]>([]);

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<AesMetiersTensionResponse | null>(null);

  // Form fields (signal-based).
  dateEntreeFrance = signal<string | null>(null);
  moisActiviteSalarieeDernieres24Mois = signal<number | null>(null);
  metierEstEnTension = signal<boolean>(false);
  codeMetier = signal<string>('');
  menaceOrdrePublic = signal<boolean>(false);
  contratOuPromesseValide = signal<boolean>(false);
  dateDepotDemande = signal<string | null>(null);

  // Provenance IA — badge "Pré-rempli depuis l'analyse" effaçable.
  provenanceDateEntreeFrance = signal<'IA' | null>(null);
  provenanceDateDepotDemande = signal<'IA' | null>(null);

  // SF-IA-03-15c : explanations map.
  sourceExplanations = signal<Map<string, SourceExplanation[]>>(new Map());

  /** Aujourd'hui en YYYY-MM-DD pour l'attribut max des inputs date. */
  readonly todayIso: string = new Date().toISOString().slice(0, 10);

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  // Alertes de cohérence F-IA-03 (computed).
  coherenceAlerts = computed<Partial<Record<AesMtAlertField, AesMtCoherenceAlert>>>(() => {
    if (!this.showForm()) return {};
    if (!this.isFrance()) return {};
    const alerts: Partial<Record<AesMtAlertField, AesMtCoherenceAlert>> = {};
    const a1 = this.buildDateEntreeFranceAlert();
    if (a1) alerts.DATE_ENTREE_FRANCE = a1;
    const a2 = this.buildMoisActiviteAlert();
    if (a2) alerts.MOIS_ACTIVITE = a2;
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
    private service: AesMetiersTensionService,
    private snackBar: MatSnackBar,
    @Optional() private dashboardRefresh: CaseDashboardRefreshService | null,
    @Optional() private sourceExplanationService: SourceExplanationService | null,
  ) {}

  ngOnInit(): void {
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
    if (changes['aiData']) this.aiDataSignal.set(this.aiData);
    if (changes['procedureChecks']) this.procedureChecksSignal.set(this.procedureChecks ?? []);
    if (changes['aiQuestions']) this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    if (changes['piecesManquantes']) this.piecesManquantesSignal.set(this.piecesManquantes ?? []);
    // Re-prefill quand aiData arrive après le mount, sans écraser les valeurs avocat.
    if (changes['aiData'] && this.isFrance() && this.showForm() && !this.result()) {
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

  /**
   * SF-IA-03-15c : mapping field → sourceKey. Convention `IM09_<FIELD>`.
   */
  explanationFor(field: AesMtAlertField): SourceExplanation[] {
    const key = (() => {
      switch (field) {
        case 'DATE_ENTREE_FRANCE': return 'IM09_DATE_ENTREE_FRANCE';
        case 'MOIS_ACTIVITE': return 'IM09_MOIS_ACTIVITE';
        case 'DATE_DEPOT_DEMANDE': return 'IM09_DATE_DEPOT_DEMANDE';
      }
    })();
    return this.sourceExplanations().get(key) ?? [];
  }

  toggleCollapse(): void {
    this.collapsed.update((v) => !v);
  }

  formValid(): boolean {
    const d = this.dateEntreeFrance();
    if (!d || !ISO_DATE_RE.test(d) || d > this.todayIso) return false;
    const m = this.moisActiviteSalarieeDernieres24Mois();
    if (m === null || m < 0 || m > 24) return false;
    const dDepot = this.dateDepotDemande();
    if (dDepot && (!ISO_DATE_RE.test(dDepot) || dDepot < d)) return false;
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // Handlers de modification — effacent la provenance IA.
  onDateEntreeFranceChange(value: string | null): void {
    this.dateEntreeFrance.set(value || null);
    this.provenanceDateEntreeFrance.set(null);
  }

  onMoisActiviteChange(value: number | null): void {
    this.moisActiviteSalarieeDernieres24Mois.set(value);
  }

  onMetierEstEnTensionChange(value: boolean): void {
    this.metierEstEnTension.set(value);
    if (!value) {
      // codeMetier non pertinent si le métier n'est pas en tension.
      this.codeMetier.set('');
    }
  }

  onCodeMetierChange(value: string): void {
    this.codeMetier.set(value);
  }

  onMenaceOrdrePublicChange(value: boolean): void {
    this.menaceOrdrePublic.set(value);
  }

  onContratOuPromesseValideChange(value: boolean): void {
    this.contratOuPromesseValide.set(value);
  }

  onDateDepotDemandeChange(value: string | null): void {
    this.dateDepotDemande.set(value || null);
    this.provenanceDateDepotDemande.set(null);
  }

  /**
   * Bannière verdict :
   * - vert (success / navy + or) si conditionsReunies=true
   * - rouge si conditionsReunies=false (alerte critique justifiée — rejet
   *   AES = risque éloignement et obligation de quitter le territoire).
   */
  bannerClass(r: AesMetiersTensionResponse | null): string {
    if (!r) return 'aes-banner';
    return r.conditionsReunies ? 'aes-banner aes-banner--success' : 'aes-banner aes-banner--danger';
  }

  bannerIcon(r: AesMetiersTensionResponse | null): string {
    if (!r) return 'info_outline';
    return r.conditionsReunies ? 'check_circle' : 'error';
  }

  bannerLabel(r: AesMetiersTensionResponse | null): string {
    if (!r) return '';
    return r.conditionsReunies ? 'Conditions réunies' : 'Conditions non réunies';
  }

  /**
   * SF-IM-09-05 : pré-remplissage depuis l'analyse IA.
   *
   * IMPORTANT : aujourd'hui `ImmigrationExtractedData` n'expose pas
   * de `dateEntreeFrance`. Le champ reste vide jusqu'à ce que le
   * pipeline IA l'expose. Seul `dateDepotProcedure` est utilisé pour
   * pré-remplir `dateDepotDemande` (graceful no-op pour les autres).
   */
  private prefillFromAi(): void {
    const ai = this.aiData;
    if (!ai) return;

    const depot = ai.dateDepotProcedure;
    if (typeof depot === 'string' && ISO_DATE_RE.test(depot) && depot <= this.todayIso) {
      if (!this.dateDepotDemande()) {
        this.dateDepotDemande.set(depot);
        this.provenanceDateDepotDemande.set('IA');
      }
    }
  }

  /**
   * Alerte cohérence DATE_ENTREE_FRANCE — fail-open structurel.
   * Aujourd'hui aucune source IA n'expose la date d'entrée en France ;
   * le builder retourne donc null. Maintenu en place pour activation
   * automatique quand le pipeline exposera le champ + scan F96 / pièce
   * manquante (`IM09_DATE_ENTREE_FRANCE`).
   */
  private buildDateEntreeFranceAlert(): AesMtCoherenceAlert | null {
    const user = this.dateEntreeFrance();
    if (!user) return null;
    const builder = CoherenceAlertBuilder
      .forField<AesMtAlertField>('DATE_ENTREE_FRANCE')
      .withSeverity('WARNING');

    // F96 — checklist procédurale `IM09_DATE_ENTREE_FRANCE`.
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'IM09_DATE_ENTREE_FRANCE') continue;
      const ev = chk.expectedValue;
      if (!ev || !ISO_DATE_RE.test(ev) || ev === user) continue;
      builder.addSource('F96', {
        expectedDisplay: ev,
        reason: `Checklist procédurale : date d'entrée attendue ${ev}${chk.raison ? ' (' + chk.raison + ')' : ''}`,
      });
      break;
    }

    // PIECE_MANQUANTE (passeport, justificatif domicile à l'arrivée).
    const piece = this.findPieceManquante(['IM09_DATE_ENTREE_FRANCE', 'IM09_PRESENCE_3_ANS']);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  /**
   * Alerte cohérence MOIS_ACTIVITE — fail-open similaire.
   * Activable via F96 ou pièce manquante (bulletins de salaire, attestation
   * Pôle emploi).
   */
  private buildMoisActiviteAlert(): AesMtCoherenceAlert | null {
    const user = this.moisActiviteSalarieeDernieres24Mois();
    if (user === null) return null;
    const builder = CoherenceAlertBuilder
      .forField<AesMtAlertField>('MOIS_ACTIVITE')
      .withSeverity('WARNING');

    // F96 — checklist procédurale `IM09_MOIS_ACTIVITE`.
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'IM09_MOIS_ACTIVITE') continue;
      const evRaw = chk.expectedValue;
      if (!evRaw) continue;
      const ev = parseInt(evRaw, 10);
      if (Number.isNaN(ev) || ev < 0 || ev > 24 || ev === user) continue;
      builder.addSource('F96', {
        expectedDisplay: `${ev} mois`,
        reason: `Checklist procédurale : ${ev} mois d'activité salariée attendus`,
      });
      break;
    }

    // PIECE_MANQUANTE (bulletins de salaire / contrats).
    const piece = this.findPieceManquante(['IM09_MOIS_ACTIVITE', 'IM09_BULLETINS_SALAIRE']);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  /**
   * Alerte DATE_DEPOT_DEMANDE — divergence entre la date saisie par
   * l'avocat et `dateDepotProcedure` extraite par l'IA.
   */
  private buildDateDepotAlert(): AesMtCoherenceAlert | null {
    const user = this.dateDepotDemande();
    if (!user) return null;
    const builder = CoherenceAlertBuilder
      .forField<AesMtAlertField>('DATE_DEPOT_DEMANDE')
      .withSeverity('WARNING');

    const ai = this.aiDataSignal()?.dateDepotProcedure;
    if (typeof ai === 'string' && ISO_DATE_RE.test(ai) && ai !== user) {
      builder.addSource('IA', {
        expectedDisplay: ai,
        reason: `L'analyse a détecté une date de dépôt différente : ${ai}`,
      });
    }

    const piece = this.findPieceManquante(['IM09_DATE_DEPOT_DEMANDE']);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  /**
   * SF-155-06 : recherche d'une `PieceManquanteEntry` matchant un des codes.
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

  /**
   * Tooltip du badge alerte — fallback hors popover.
   */
  alertTooltip(alert: AesMtCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  /**
   * Libellé du badge alerte affiché en UI.
   */
  alertBadgeLabel(alert: AesMtCoherenceAlert): string {
    const prefix = alert.severity === 'CRITICAL'
      ? 'Risque d\'irrecevabilité'
      : 'Incohérence détectée';
    return `${prefix} (${alert.expectedDisplay})`;
  }

  calculate(): void {
    if (!this.formValid()) return;
    const codeM = this.codeMetier().trim();
    const dDepot = this.dateDepotDemande();
    const request: AesMetiersTensionRequest = {
      dateEntreeFrance: this.dateEntreeFrance()!,
      moisActiviteSalarieeDernieres24Mois: this.moisActiviteSalarieeDernieres24Mois()!,
      metierEstEnTension: this.metierEstEnTension(),
      menaceOrdrePublic: this.menaceOrdrePublic(),
      contratOuPromesseValide: this.contratOuPromesseValide(),
      ...(this.metierEstEnTension() && codeM ? { codeMetier: codeM } : {}),
      ...(dDepot ? { dateDepotDemande: dDepot } : {}),
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open(
          r.conditionsReunies ? 'AES Métiers en tension : conditions réunies' : 'AES analysée',
          'OK',
          { duration: 2500 },
        );
        this.dashboardRefresh?.triggerRefresh();
      },
      error: (err) => {
        this.calculating.set(false);
        const msg = err?.error?.message || err?.error || 'Erreur lors du calcul AES';
        this.snackBar.open(String(msg), 'Fermer', { duration: 5000, panelClass: 'snack-error' });
      },
    });
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.dateEntreeFrance.set(r.dateEntreeFrance);
        this.moisActiviteSalarieeDernieres24Mois.set(r.moisActiviteSalarieeDernieres24Mois);
        this.metierEstEnTension.set(r.metierEstEnTension);
        this.codeMetier.set(r.codeMetier ?? '');
        this.menaceOrdrePublic.set(r.menaceOrdrePublic);
        this.contratOuPromesseValide.set(r.contratOuPromesseValide);
        this.dateDepotDemande.set(r.dateDepotDemande);
        this.provenanceDateEntreeFrance.set(null);
        this.provenanceDateDepotDemande.set(null);
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
