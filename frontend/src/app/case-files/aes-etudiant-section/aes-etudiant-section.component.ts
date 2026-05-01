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
import { AesEtudiantService } from '../../core/services/aes-etudiant.service';
import {
  AesEtudiantNiveauEtudes,
  AesEtudiantRequest,
  AesEtudiantResponse,
  AesEtudiantResultats,
} from '../../core/models/aes-etudiant.model';
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

export type AesEtudiantAlertField =
  | 'DATE_ENTREE_FRANCE' | 'DUREE_PRESENCE' | 'DATE_DEPOT_DEMANDE';

export type AesEtudiantAlertSeverity = CoherenceAlertSeverity;
export type AesEtudiantCoherenceAlert = CoherenceAlert<AesEtudiantAlertField>;

const NIVEAUX_OPTIONS: { value: AesEtudiantNiveauEtudes; label: string }[] = [
  { value: 'LYCEE', label: 'Lycée (secondaire)' },
  { value: 'BAC_PLUS_1_2', label: 'Bac + 1 / Bac + 2 (DUT, BTS, L1-L2)' },
  { value: 'BAC_PLUS_3_4', label: 'Bac + 3 / Bac + 4 (Licence, Master 1)' },
  { value: 'BAC_PLUS_5_PLUS', label: 'Bac + 5 et plus (Master 2, Doctorat)' },
];

const RESULTATS_OPTIONS: { value: AesEtudiantResultats; label: string }[] = [
  { value: 'EXCELLENT', label: 'Excellents' },
  { value: 'MOYEN_PASSABLE', label: 'Moyens / passables' },
  { value: 'DIFFICULTES_REPETEES', label: 'Difficultés répétées' },
];

/**
 * SF-IM-09-08 : section frontend dédiée AES voie étudiante (FR).
 * Circulaire Valls 28/11/2012 (point III) actualisée Darmanin —
 * art. L.412-1 CESEDA. Régularisation d'un étudiant présent en France
 * sans titre adapté.
 *
 * Single-country FR — Belgique : bannière info ("régime français
 * uniquement") + form masqué (pas d'appel HTTP). Cohérent avec les
 * autres voies AES (SF-IM-09-05 Métiers en tension, SF-IM-09-06 Famille).
 *
 * Template canonique F-155 SF-155-04/05/06 — pré-fill IA + alertes
 * F-IA-03 + `CoherenceAlertBuilder` partagé.
 */
@Component({
  selector: 'app-aes-etudiant-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatChipsModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatProgressSpinnerModule, MatSelectModule, MatSlideToggleModule,
    LegalCitationsPipe,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './aes-etudiant-section.component.html',
  styleUrl: './aes-etudiant-section.component.scss',
})
export class AesEtudiantSectionComponent implements OnInit, OnChanges {
  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'AES VOIE ÉTUDIANTE (FR)';
  static readonly TOOL_ICON = 'school';

  @Input() caseFileId!: string;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';
  // SF-IM-09-08 : sources IA pour pré-fill + alertes de cohérence F-IA-03.
  @Input() aiData?: ImmigrationExtractedData | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;
  // F-177 SF-177-03b : force l'expansion (mode modal F-177).
  @Input() forceExpanded = false;

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
  result = signal<AesEtudiantResponse | null>(null);

  // Form fields (signal-based).
  dateEntreeFrance = signal<string | null>(null);
  dureePresenceMois = signal<number | null>(null);
  anneesScolariteEnFranceConsecutives = signal<number | null>(null);
  niveauEtudesActuel = signal<AesEtudiantNiveauEtudes | null>(null);
  resultatsAcademiques = signal<AesEtudiantResultats | null>(null);
  inscriptionEtablissementReconnu = signal<boolean>(false);
  moyensSubsistance = signal<boolean>(false);
  menaceOrdrePublic = signal<boolean>(false);
  parcoursCoherent = signal<boolean>(false);
  dateDepotDemande = signal<string | null>(null);

  // Provenance IA — badges "Pré-rempli depuis l'analyse" effaçables.
  // 2 champs pré-remplissables : `dateEntreeFrance` (futur quand pipeline
  // IA exposera le champ — fallback `as { dateEntreeFrance?: string }`,
  // pattern aes-famille SF-IM-09-06) + `dateDepotDemande` (depuis
  // `dateDepotProcedure`).
  provenanceDateEntreeFrance = signal<'IA' | null>(null);
  provenanceDateDepotDemande = signal<'IA' | null>(null);

  // SF-IA-03-15c : explanations map.
  sourceExplanations = signal<Map<string, SourceExplanation[]>>(new Map());

  // Options pour les selects.
  readonly niveauxOptions = NIVEAUX_OPTIONS;
  readonly resultatsOptions = RESULTATS_OPTIONS;

  /** Aujourd'hui en YYYY-MM-DD pour l'attribut max des inputs date. */
  readonly todayIso: string = new Date().toISOString().slice(0, 10);

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  // Alertes de cohérence F-IA-03 (computed).
  coherenceAlerts = computed<Partial<Record<AesEtudiantAlertField, AesEtudiantCoherenceAlert>>>(() => {
    if (!this.showForm()) return {};
    if (!this.isFrance()) return {};
    const alerts: Partial<Record<AesEtudiantAlertField, AesEtudiantCoherenceAlert>> = {};
    const a1 = this.buildDateEntreeFranceAlert();
    if (a1) alerts.DATE_ENTREE_FRANCE = a1;
    const a2 = this.buildDureePresenceAlert();
    if (a2) alerts.DUREE_PRESENCE = a2;
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
    private service: AesEtudiantService,
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
   * SF-IA-03-15c : mapping field → sourceKey. Convention `IM09_ETU_<FIELD>`.
   */
  explanationFor(field: AesEtudiantAlertField): SourceExplanation[] {
    const key = (() => {
      switch (field) {
        case 'DATE_ENTREE_FRANCE': return 'IM09_ETU_DATE_ENTREE_FRANCE';
        case 'DUREE_PRESENCE': return 'IM09_ETU_DUREE_PRESENCE';
        case 'DATE_DEPOT_DEMANDE': return 'IM09_ETU_DATE_DEPOT_DEMANDE';
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
    const presence = this.dureePresenceMois();
    if (presence === null || presence < 0 || presence > 600) return false;
    const scol = this.anneesScolariteEnFranceConsecutives();
    if (scol === null || scol < 0 || scol > 20) return false;
    if (!this.niveauEtudesActuel()) return false;
    if (!this.resultatsAcademiques()) return false;
    const dDepot = this.dateDepotDemande();
    if (dDepot && (!ISO_DATE_RE.test(dDepot) || dDepot < d)) return false;
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // Handlers — effacent la provenance IA si applicable.
  onDateEntreeFranceChange(value: string | null): void {
    this.dateEntreeFrance.set(value || null);
    this.provenanceDateEntreeFrance.set(null);
  }

  onDureePresenceMoisChange(value: number | null): void {
    this.dureePresenceMois.set(value);
  }

  onAnneesScolariteChange(value: number | null): void {
    this.anneesScolariteEnFranceConsecutives.set(value);
  }

  onNiveauEtudesChange(value: AesEtudiantNiveauEtudes | null): void {
    this.niveauEtudesActuel.set(value);
  }

  onResultatsAcademiquesChange(value: AesEtudiantResultats | null): void {
    this.resultatsAcademiques.set(value);
  }

  onInscriptionChange(value: boolean): void {
    this.inscriptionEtablissementReconnu.set(value);
  }

  onMoyensSubsistanceChange(value: boolean): void {
    this.moyensSubsistance.set(value);
  }

  onMenaceOrdrePublicChange(value: boolean): void {
    this.menaceOrdrePublic.set(value);
  }

  onParcoursCoherentChange(value: boolean): void {
    this.parcoursCoherent.set(value);
  }

  onDateDepotDemandeChange(value: string | null): void {
    this.dateDepotDemande.set(value || null);
    this.provenanceDateDepotDemande.set(null);
  }

  /**
   * Bannière verdict :
   * - vert (success) si verdictProbabiliteAcceptation='ELEVEE'
   * - or (gold) si 'MOYENNE'
   * - rouge si 'FAIBLE' (alerte critique justifiée — rejet AES probable
   *   = risque éloignement / OQTF subséquent).
   */
  bannerClass(r: AesEtudiantResponse | null): string {
    if (!r) return 'aes-banner';
    switch (r.verdictProbabiliteAcceptation) {
      case 'ELEVEE': return 'aes-banner aes-banner--success';
      case 'MOYENNE': return 'aes-banner';
      case 'FAIBLE': return 'aes-banner aes-banner--danger';
      default: return 'aes-banner';
    }
  }

  bannerIcon(r: AesEtudiantResponse | null): string {
    if (!r) return 'info_outline';
    switch (r.verdictProbabiliteAcceptation) {
      case 'ELEVEE': return 'check_circle';
      case 'MOYENNE': return 'help_outline';
      case 'FAIBLE': return 'error';
      default: return 'info_outline';
    }
  }

  bannerLabel(r: AesEtudiantResponse | null): string {
    if (!r) return '';
    switch (r.verdictProbabiliteAcceptation) {
      case 'ELEVEE': return 'Probabilité acceptation ÉLEVÉE';
      case 'MOYENNE': return 'Probabilité acceptation MOYENNE';
      case 'FAIBLE': return 'Probabilité acceptation FAIBLE';
      default: return '';
    }
  }

  /**
   * SF-IM-09-08 : pré-remplissage depuis l'analyse IA.
   *
   * IMPORTANT : aujourd'hui `ImmigrationExtractedData` n'expose pas
   * formellement `dateEntreeFrance`, `dureePresenceMois`,
   * `anneesScolariteEnFrance`, `niveauEtudesActuel`, `resultatsAcademiques`.
   *
   * Deux champs pré-remplis (pattern canonique aes-famille SF-IM-09-06
   * + aes-metiers-tension SF-IM-09-05) :
   *  - `dateEntreeFrance` via fallback `as { dateEntreeFrance?: string }`
   *    (champ futur du pipeline — no-op gracieux aujourd'hui).
   *  - `dateDepotDemande` via `dateDepotProcedure` (champ standard).
   */
  private prefillFromAi(): void {
    const ai = this.aiData;
    if (!ai) return;

    // dateEntreeFrance — fallback gracieux sur champ futur (pattern aes-famille).
    const aiDateEntree = (ai as { dateEntreeFrance?: string | null }).dateEntreeFrance;
    if (typeof aiDateEntree === 'string' && ISO_DATE_RE.test(aiDateEntree)
        && aiDateEntree <= this.todayIso) {
      if (!this.dateEntreeFrance()) {
        this.dateEntreeFrance.set(aiDateEntree);
        this.provenanceDateEntreeFrance.set('IA');
      }
    }

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
   * Activable via F96 (`IM09_ETU_DATE_ENTREE_FRANCE`) ou pièce manquante
   * (passeport, justificatif domicile à l'arrivée).
   */
  private buildDateEntreeFranceAlert(): AesEtudiantCoherenceAlert | null {
    const user = this.dateEntreeFrance();
    if (!user) return null;
    const builder = CoherenceAlertBuilder
      .forField<AesEtudiantAlertField>('DATE_ENTREE_FRANCE')
      .withSeverity('WARNING');

    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'IM09_ETU_DATE_ENTREE_FRANCE') continue;
      const ev = chk.expectedValue;
      if (!ev || !ISO_DATE_RE.test(ev) || ev === user) continue;
      builder.addSource('F96', {
        expectedDisplay: ev,
        reason: `Checklist procédurale : date d'entrée attendue ${ev}${chk.raison ? ' (' + chk.raison + ')' : ''}`,
      });
      break;
    }

    const piece = this.findPieceManquante([
      'IM09_ETU_DATE_ENTREE_FRANCE', 'IM09_ETU_PRESENCE_3_ANS',
    ]);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  /**
   * Alerte cohérence DUREE_PRESENCE — fail-open similaire.
   * Activable via F96 (`IM09_ETU_DUREE_PRESENCE`) ou pièce manquante
   * (justificatifs de présence continue).
   */
  private buildDureePresenceAlert(): AesEtudiantCoherenceAlert | null {
    const user = this.dureePresenceMois();
    if (user === null) return null;
    const builder = CoherenceAlertBuilder
      .forField<AesEtudiantAlertField>('DUREE_PRESENCE')
      .withSeverity('WARNING');

    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'IM09_ETU_DUREE_PRESENCE') continue;
      const evRaw = chk.expectedValue;
      if (!evRaw) continue;
      const ev = parseInt(evRaw, 10);
      if (Number.isNaN(ev) || ev < 0 || ev > 600 || ev === user) continue;
      builder.addSource('F96', {
        expectedDisplay: `${ev} mois`,
        reason: `Checklist procédurale : ${ev} mois de présence attendus`,
      });
      break;
    }

    const piece = this.findPieceManquante([
      'IM09_ETU_DUREE_PRESENCE', 'IM09_ETU_JUSTIFICATIFS_PRESENCE',
    ]);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  /**
   * Alerte DATE_DEPOT_DEMANDE — divergence entre la date saisie par
   * l'avocat et `dateDepotProcedure` extraite par l'IA.
   */
  private buildDateDepotAlert(): AesEtudiantCoherenceAlert | null {
    const user = this.dateDepotDemande();
    if (!user) return null;
    const builder = CoherenceAlertBuilder
      .forField<AesEtudiantAlertField>('DATE_DEPOT_DEMANDE')
      .withSeverity('WARNING');

    const ai = this.aiDataSignal()?.dateDepotProcedure;
    if (typeof ai === 'string' && ISO_DATE_RE.test(ai) && ai !== user) {
      builder.addSource('IA', {
        expectedDisplay: ai,
        reason: `L'analyse a détecté une date de dépôt différente : ${ai}`,
      });
    }

    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'IM09_ETU_DATE_DEPOT_DEMANDE') continue;
      const ev = chk.expectedValue;
      if (!ev || !ISO_DATE_RE.test(ev) || ev === user) continue;
      builder.addSource('F96', {
        expectedDisplay: ev,
        reason: `Checklist procédurale : date de dépôt attendue ${ev}`,
      });
      break;
    }

    const piece = this.findPieceManquante(['IM09_ETU_DATE_DEPOT_DEMANDE']);
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
  alertTooltip(alert: AesEtudiantCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  /**
   * Libellé du badge alerte affiché en UI.
   */
  alertBadgeLabel(alert: AesEtudiantCoherenceAlert): string {
    const prefix = alert.severity === 'CRITICAL'
      ? 'Risque d\'irrecevabilité'
      : 'Incohérence détectée';
    return `${prefix} (${alert.expectedDisplay})`;
  }

  /** Libellé humain pour un niveau d'études (pour la carte résultat). */
  niveauLabel(value: AesEtudiantNiveauEtudes | null | undefined): string {
    if (!value) return '';
    return NIVEAUX_OPTIONS.find((o) => o.value === value)?.label ?? value;
  }

  /** Libellé humain pour des résultats académiques (pour la carte résultat). */
  resultatsLabel(value: AesEtudiantResultats | null | undefined): string {
    if (!value) return '';
    return RESULTATS_OPTIONS.find((o) => o.value === value)?.label ?? value;
  }

  calculate(): void {
    if (!this.formValid()) return;
    const dDepot = this.dateDepotDemande();
    const request: AesEtudiantRequest = {
      dateEntreeFrance: this.dateEntreeFrance()!,
      dureePresenceMois: this.dureePresenceMois()!,
      anneesScolariteEnFranceConsecutives: this.anneesScolariteEnFranceConsecutives()!,
      niveauEtudesActuel: this.niveauEtudesActuel()!,
      resultatsAcademiques: this.resultatsAcademiques()!,
      inscriptionEtablissementReconnu: this.inscriptionEtablissementReconnu(),
      moyensSubsistance: this.moyensSubsistance(),
      menaceOrdrePublic: this.menaceOrdrePublic(),
      parcoursCoherent: this.parcoursCoherent(),
      ...(dDepot ? { dateDepotDemande: dDepot } : {}),
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open(
          `AES voie étudiante : probabilité ${r.verdictProbabiliteAcceptation.toLowerCase()}`,
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
        this.dureePresenceMois.set(r.dureePresenceMois);
        this.anneesScolariteEnFranceConsecutives.set(r.anneesScolariteEnFranceConsecutives);
        this.niveauEtudesActuel.set(r.niveauEtudesActuel);
        this.resultatsAcademiques.set(r.resultatsAcademiques);
        this.inscriptionEtablissementReconnu.set(r.inscriptionEtablissementReconnu);
        this.moyensSubsistance.set(r.moyensSubsistance);
        this.menaceOrdrePublic.set(r.menaceOrdrePublic);
        this.parcoursCoherent.set(r.parcoursCoherent);
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
