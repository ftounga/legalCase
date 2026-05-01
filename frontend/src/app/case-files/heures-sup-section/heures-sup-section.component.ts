import {
  Component, Input, OnInit, OnChanges, SimpleChanges, Optional,
  signal, computed,
} from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { HeuresSupService } from '../../core/services/heures-sup.service';
import {
  HeuresSupRequest,
  HeuresSupResponse,
} from '../../core/models/heures-sup.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import {
  TravailExtractedData,
  PieceManquanteEntry,
} from '../../core/models/case-analysis.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { AiQuestion } from '../../core/models/ai-question.model';
import { CoherencePopoverTriggerDirective } from '../../shared/coherence-popover/coherence-popover-trigger.directive';
import { CoherenceAlert } from '../../shared/coherence-popover/coherence-alert.model';
import { CoherenceAlertBuilder } from '../../shared/coherence-popover/coherence-alert-builder';
import { SourceExplanation } from '../../core/models/source-explanation.model';
import { SourceExplanationService } from '../../core/services/source-explanation.service';

/** Durée légale française mensualisée (35 h × 52 semaines / 12). */
const HEURES_MOIS_FR = 151.67;
/** Écart relatif toléré entre tauxHoraireBrut saisi et dérivé IA (10 %). */
const SEUIL_TAUX_HORAIRE_REL = 0.10;
/** Écart absolu minimum heures sup saisies vs détectées IA (5 h). */
const SEUIL_HEURES_ABS = 5;
/** Écart relatif minimum heures sup (50 %), combiné au seuil absolu. */
const SEUIL_HEURES_REL = 0.50;

export type HsAlertField = 'TAUX_HORAIRE' | 'HEURES_SUP' | 'SALAIRE_DEDUIT';

/**
 * SF-155-05 : alerte F-IA-03 de l'outil heures sup, factorisée via
 * `CoherenceAlert<HsAlertField>`. Ancien contrat (`iaValue`, `level`)
 * remplacé par (`expectedDisplay`, `severity`) pour cohérence cross-component.
 *
 * Getter de compatibilité : `alert.iaValue` est le `expectedDisplay`
 * (même sémantique — texte descriptif de la valeur IA ou du motif).
 * `level` est dérivé de `severity` (`'warning'` ↔ `WARNING`, `'info'` ↔ `INFO`).
 */
export type HsCoherenceAlert = CoherenceAlert<HsAlertField>;

/**
 * SF-DT-19-02 : outil décisionnel dédié "Calculateur heures supplémentaires"
 * (F-DT-19). FR + BE. Consomme l'API SF-DT-19-01.
 *
 * SF-155-04-A3 (2026-04-24) : pré-remplissage IA + validation F-IA-03. Aligne
 * le composant sur le pattern canonique `immigration-title-decision-section`.
 * Les champs `salaireBrutMensuel` → `tauxHoraireBrut` et
 * `heuresSupMentionneesDansDossier` sont pré-remplis depuis la synthèse IA.
 * Les signaux `provenance<Field>` pilotent les badges "Pré-rempli depuis
 * l'analyse" et sont effacés au changement manuel.
 *
 * Affiché conditionnellement par le panel F-IA-04 (tool_id
 * 'F-DT-19-heures-sup').
 */
@Component({
  selector: 'app-heures-sup-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule, DecimalPipe,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatProgressSpinnerModule,
    LegalCitationsPipe,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './heures-sup-section.component.html',
  styleUrl: './heures-sup-section.component.scss',
})
export class HeuresSupSectionComponent implements OnInit, OnChanges {
  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'RAPPEL HEURES SUPPLÉMENTAIRES';
  static readonly TOOL_ICON = 'schedule';

  @Input() caseFileId!: string;
  // F-177 SF-177-03b : force l'expansion (mode modal F-177).
  @Input() forceExpanded = false;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';
  // SF-155-04-A3 : sources IA pour pré-fill + cohérence F-IA-03.
  @Input() aiData?: TravailExtractedData | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;

  // Signaux miroirs des inputs (déclenchent les computed coherenceAlerts).
  private aiDataSignal = signal<TravailExtractedData | null | undefined>(undefined);
  // SF-155-06 : signals miroirs pour F96 + QUESTION_IA + PIECE_MANQUANTE.
  private procedureChecksSignal = signal<ProcedureCheck[]>([]);
  private aiQuestionsSignal = signal<AiQuestion[]>([]);
  private piecesManquantesSignal = signal<PieceManquanteEntry[]>([]);

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<HeuresSupResponse | null>(null);

  tauxHoraireBrut = signal<number | null>(null);

  // FR
  heuresSupDeclarees25pct = signal<number | null>(null);
  heuresSupDeclarees50pct = signal<number | null>(null);
  heuresHorsContingent = signal<number | null>(null);
  tauxMajoration25 = signal<number | null>(25);
  tauxMajoration50 = signal<number | null>(50);

  // BE
  heuresSupSemaine = signal<number | null>(null);
  heuresDimancheJoursFeries = signal<number | null>(null);

  // SF-155-04-A3 : provenance IA par champ (badge "Pré-rempli depuis l'analyse").
  // Effacé dès que l'avocat modifie manuellement un champ (onXxxChange).
  provenanceTauxHoraire = signal<'IA' | null>(null);
  provenanceHeures25 = signal<'IA' | null>(null);
  provenanceHeures50 = signal<'IA' | null>(null);
  provenanceHorsContingent = signal<'IA' | null>(null);

  /**
   * SF-155-04-A3 : alertes de cohérence F-IA-03.
   * - TAUX_HORAIRE : écart ≥ 10 % entre taux saisi et dérivé IA.
   * - HEURES_SUP   : somme saisie >> somme IA (5 h abs ET 50 % rel).
   * - SALAIRE_DEDUIT : note info (pas warning) — taux horaire dérivé moins fiable.
   * Gate `showForm()` seul (évite le bug SF-IA-03-12 : pas de `|| result()`).
   * BE : pas d'alertes IA (concept FR-only).
   */
  coherenceAlerts = computed<Partial<Record<HsAlertField, HsCoherenceAlert>>>(() => {
    if (!this.showForm()) return {};
    if (this.workspaceCountry !== 'FRANCE') return {};
    const ai = this.aiDataSignal();
    if (!ai) return {};
    const alerts: Partial<Record<HsAlertField, HsCoherenceAlert>> = {};

    const tauxAlert = this.buildTauxHoraireAlert(ai);
    if (tauxAlert) alerts.TAUX_HORAIRE = tauxAlert;

    const heuresAlert = this.buildHeuresSupAlert(ai);
    if (heuresAlert) alerts.HEURES_SUP = heuresAlert;

    const salaireDeduit = this.buildSalaireDeduitNote(ai);
    if (salaireDeduit) alerts.SALAIRE_DEDUIT = salaireDeduit;

    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    return { total: values.length, blockers: 0 };
  });

  // SF-155-07 (DIV-7) : map {sourceKey → explanations} pour les popovers
  // SF-IA-03-15c. Alimentée au mount, consultée par `explanationFor(field)`.
  // Fail-open : map vide si service absent ou erreur HTTP.
  sourceExplanations = signal<Map<string, SourceExplanation[]>>(new Map());

  constructor(
    private service: HeuresSupService,
    private snackBar: MatSnackBar,
    @Optional() private dashboardRefresh: CaseDashboardRefreshService | null,
    // SF-155-07 (DIV-7) : injection `@Optional()` — fail-open strict.
    @Optional() private sourceExplanationService: SourceExplanationService | null,
  ) {}

  ngOnInit(): void {
    // F-177 SF-177-03b : appliqué dès le mount pour le mode modal.
    if (this.forceExpanded) this.collapsed.set(false);

    this.aiDataSignal.set(this.aiData);
    this.procedureChecksSignal.set(this.procedureChecks ?? []);
    this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    this.piecesManquantesSignal.set(this.piecesManquantes ?? []);
    this.load();
    // SF-155-07 (DIV-7) : pré-charge les explications F-IA-03-15a pour les popovers.
    this.loadSourceExplanations();
  }

  /**
   * SF-155-07 (DIV-7) : charge les explications de sources via F-IA-03-15a.
   * Fail-open strict : erreur ou absence de service = map vide.
   */
  private loadSourceExplanations(): void {
    if (!this.caseFileId) return;
    if (!this.sourceExplanationService) return;
    this.sourceExplanationService.getForCaseFile(this.caseFileId).subscribe({
      next: (map) => this.sourceExplanations.set(map),
      error: () => { /* fail-open */ },
    });
  }

  /**
   * SF-155-07 (DIV-7) : mapping field → sourceKey pour alimenter les popovers.
   * Convention `HS_<FIELD>` (upper_case).
   */
  explanationFor(field: HsAlertField): SourceExplanation[] {
    const key = (() => {
      switch (field) {
        case 'TAUX_HORAIRE': return 'HS_TAUX_HORAIRE';
        case 'HEURES_SUP': return 'HS_HEURES_SUP';
        case 'SALAIRE_DEDUIT': return 'HS_SALAIRE_DEDUIT';
      }
    })();
    return this.sourceExplanations().get(key) ?? [];
  }

  ngOnChanges(changes: SimpleChanges): void {
    // F-177 SF-177-03b : applique le forceExpanded quand il passe à true en cours de vie.
    if (changes['forceExpanded'] && this.forceExpanded) this.collapsed.set(false);

    if (changes['procedureChecks']) this.procedureChecksSignal.set(this.procedureChecks ?? []);
    if (changes['aiQuestions']) this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    if (changes['piecesManquantes']) this.piecesManquantesSignal.set(this.piecesManquantes ?? []);
    if (changes['aiData']) {
      this.aiDataSignal.set(this.aiData);
      // Ré-applique le prefill quand `aiData` arrive après ngOnInit,
      // mais uniquement si le form est en édition et aucune analyse persistée.
      // Les champs déjà modifiés manuellement (provenance null) ne sont pas écrasés.
      if (this.showForm() && !this.result()) {
        this.prefillFromAi();
      }
    }
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  editMode(): void {
    this.showForm.set(true);
  }

  /** SF-155-04-A3 : handlers qui effacent la provenance au changement manuel. */
  onTauxHoraireChange(): void { this.provenanceTauxHoraire.set(null); }
  onHeures25Change(): void { this.provenanceHeures25.set(null); }
  onHeures50Change(): void { this.provenanceHeures50.set(null); }
  onHorsContingentChange(): void { this.provenanceHorsContingent.set(null); }

  formValid(): boolean {
    const taux = this.tauxHoraireBrut();
    if (taux === null || taux <= 0) return false;
    if (this.workspaceCountry === 'FRANCE') {
      const h25 = this.heuresSupDeclarees25pct() ?? 0;
      const h50 = this.heuresSupDeclarees50pct() ?? 0;
      const hHc = this.heuresHorsContingent() ?? 0;
      if (h25 < 0 || h50 < 0 || hHc < 0) return false;
      if (h25 + h50 + hHc <= 0) return false;
      const tx25 = this.tauxMajoration25();
      const tx50 = this.tauxMajoration50();
      if (tx25 === null || tx25 < 10 || tx25 > 50) return false;
      if (tx50 === null || tx50 < 10 || tx50 > 50) return false;
      return true;
    }
    // BELGIQUE
    const hSem = this.heuresSupSemaine() ?? 0;
    const hDim = this.heuresDimancheJoursFeries() ?? 0;
    if (hSem < 0 || hDim < 0) return false;
    if (hSem + hDim <= 0) return false;
    return true;
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request: HeuresSupRequest = {
      tauxHoraireBrut: this.tauxHoraireBrut()!,
    };
    if (this.workspaceCountry === 'FRANCE') {
      request.heuresSupDeclarees25pct = this.heuresSupDeclarees25pct() ?? 0;
      request.heuresSupDeclarees50pct = this.heuresSupDeclarees50pct() ?? 0;
      request.heuresHorsContingent = this.heuresHorsContingent() ?? 0;
      request.tauxMajoration25 = this.tauxMajoration25() ?? 25;
      request.tauxMajoration50 = this.tauxMajoration50() ?? 50;
    } else {
      request.heuresSupSemaine = this.heuresSupSemaine() ?? 0;
      request.heuresDimancheJoursFeries = this.heuresDimancheJoursFeries() ?? 0;
    }
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Rappel heures sup calculé', 'OK', { duration: 2500 });
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
        this.tauxHoraireBrut.set(r.tauxHoraireBrut);
        this.heuresSupDeclarees25pct.set(r.heuresSupDeclarees25pct);
        this.heuresSupDeclarees50pct.set(r.heuresSupDeclarees50pct);
        this.heuresHorsContingent.set(r.heuresHorsContingent);
        this.tauxMajoration25.set(r.tauxMajoration25 ?? 25);
        this.tauxMajoration50.set(r.tauxMajoration50 ?? 50);
        this.heuresSupSemaine.set(r.heuresSupSemaine);
        this.heuresDimancheJoursFeries.set(r.heuresDimancheJoursFeries);
        this.showForm.set(false);
        this.loading.set(false);
      },
      error: () => {
        // 404 attendu si aucune analyse — on reste en mode formulaire + prefill IA.
        this.prefillFromAi();
        this.loading.set(false);
      },
    });
  }

  /**
   * SF-155-04-A3 : pré-remplit les champs depuis la synthèse IA
   * `TravailExtractedData`. FR uniquement (le backend ne remplit pas
   * `heuresSupMentionneesDansDossier` pour les dossiers BE).
   *
   * Chaque champ déjà modifié manuellement (provenance `null` préalable)
   * n'est pas ré-écrasé lors d'un second appel (cf. `ngOnChanges`).
   */
  private prefillFromAi(): void {
    if (this.workspaceCountry !== 'FRANCE') return;
    const ai = this.aiData;
    if (!ai) return;

    // 1. tauxHoraireBrut dérivé depuis salaireBrutMensuel.
    //    Pré-fill uniquement si le champ n'a pas encore de provenance IA
    //    et n'a pas été modifié manuellement (tauxHoraireBrut() null ou IA).
    if (
      typeof ai.salaireBrutMensuel === 'number' &&
      ai.salaireBrutMensuel > 0 &&
      this.canPrefill('TAUX')
    ) {
      const derived = Math.round((ai.salaireBrutMensuel / HEURES_MOIS_FR) * 100) / 100;
      this.tauxHoraireBrut.set(derived);
      this.provenanceTauxHoraire.set('IA');
    }

    // 2. heuresSupMentionneesDansDossier — garde-fou typeof object.
    const mentionnees = ai.heuresSupMentionneesDansDossier;
    if (mentionnees && typeof mentionnees === 'object') {
      if (typeof mentionnees.totalDeclarees25pct === 'number' && this.canPrefill('H25')) {
        this.heuresSupDeclarees25pct.set(mentionnees.totalDeclarees25pct);
        this.provenanceHeures25.set('IA');
      }
      if (typeof mentionnees.totalDeclarees50pct === 'number' && this.canPrefill('H50')) {
        this.heuresSupDeclarees50pct.set(mentionnees.totalDeclarees50pct);
        this.provenanceHeures50.set('IA');
      }
      if (typeof mentionnees.horsContingent === 'number' && this.canPrefill('HC')) {
        this.heuresHorsContingent.set(mentionnees.horsContingent);
        this.provenanceHorsContingent.set('IA');
      }
    }
  }

  /**
   * Un champ peut être pré-rempli si :
   *  - il est vide (signal null), OU
   *  - sa provenance est déjà 'IA' (on rafraîchit depuis la nouvelle synthèse).
   * Un champ avec valeur saisie manuellement (provenance null mais valeur non-null) est préservé.
   */
  private canPrefill(field: 'TAUX' | 'H25' | 'H50' | 'HC'): boolean {
    switch (field) {
      case 'TAUX':
        return this.tauxHoraireBrut() === null || this.provenanceTauxHoraire() === 'IA';
      case 'H25':
        return this.heuresSupDeclarees25pct() === null || this.provenanceHeures25() === 'IA';
      case 'H50':
        return this.heuresSupDeclarees50pct() === null || this.provenanceHeures50() === 'IA';
      case 'HC':
        return this.heuresHorsContingent() === null || this.provenanceHorsContingent() === 'IA';
    }
  }

  // --- Coherence alerts builders (SF-155-04-A3) ---

  private buildTauxHoraireAlert(ai: TravailExtractedData): HsCoherenceAlert | null {
    const taux = this.tauxHoraireBrut();
    if (taux === null || taux <= 0) return null;
    if (typeof ai.salaireBrutMensuel !== 'number' || ai.salaireBrutMensuel <= 0) return null;
    const derived = ai.salaireBrutMensuel / HEURES_MOIS_FR;
    const rel = Math.abs(taux - derived) / Math.max(derived, 1);
    if (rel < SEUIL_TAUX_HORAIRE_REL) return null;
    const text = `${derived.toFixed(2)} €/h (dérivé de ${ai.salaireBrutMensuel} € brut / ${HEURES_MOIS_FR} h)`;
    const builder = CoherenceAlertBuilder.forField<HsAlertField>('TAUX_HORAIRE')
      .withSeverity('WARNING')
      .addSource('IA', { expectedDisplay: text, reason: text });
    // SF-155-06 : fiche de paie / bulletin manquant — contributor additionnel.
    const piece = this.findPieceManquante(['SALAIRE_BRUT_MENSUEL', 'HS_TAUX_HORAIRE']);
    if (piece) builder.addPieceManquante(piece);
    return builder.build();
  }

  private buildHeuresSupAlert(ai: TravailExtractedData): HsCoherenceAlert | null {
    const m = ai.heuresSupMentionneesDansDossier;
    if (!m || typeof m !== 'object') return null;
    const iaTotal =
      (typeof m.totalDeclarees25pct === 'number' ? m.totalDeclarees25pct : 0) +
      (typeof m.totalDeclarees50pct === 'number' ? m.totalDeclarees50pct : 0) +
      (typeof m.horsContingent === 'number' ? m.horsContingent : 0);
    if (iaTotal <= 0) return null;
    const userTotal =
      (this.heuresSupDeclarees25pct() ?? 0) +
      (this.heuresSupDeclarees50pct() ?? 0) +
      (this.heuresHorsContingent() ?? 0);
    if (userTotal <= iaTotal) return null;
    const ecartAbs = userTotal - iaTotal;
    const ecartRel = ecartAbs / Math.max(iaTotal, 1);
    if (ecartAbs < SEUIL_HEURES_ABS || ecartRel < SEUIL_HEURES_REL) return null;
    const text = `${iaTotal} h détectées dans l'analyse, ${userTotal} h saisies`;
    const builder = CoherenceAlertBuilder.forField<HsAlertField>('HEURES_SUP')
      .withSeverity('WARNING')
      .addSource('IA', { expectedDisplay: text, reason: text });
    // SF-155-06 : relevés d'heures / plannings manquants — contributor additionnel.
    const piece = this.findPieceManquante(['HS_HEURES_SUP', 'HS_RELEVES_HEURES']);
    if (piece) builder.addPieceManquante(piece);
    return builder.build();
  }

  /**
   * SF-155-06 : recherche une `PieceManquanteEntry` dont `critereCode`
   * appartient (case-insensitive) à la liste acceptée. Retourne le 1er `texte`
   * trouvé, ou `null`. F96/QUESTION_IA ne sont pas exploités pour heures-sup
   * faute de matching sémantique évident (les heures sup ne font pas partie
   * de la checklist procédurale F-96, et il n'y a pas de question IA typée
   * sur des quantités horaires).
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

  private buildSalaireDeduitNote(ai: TravailExtractedData): HsCoherenceAlert | null {
    if (ai.salaireEstDeduit !== true) return null;
    if (typeof ai.salaireBrutMensuel !== 'number' || ai.salaireBrutMensuel <= 0) return null;
    const taux = this.tauxHoraireBrut();
    if (taux === null || taux <= 0) return null;
    const text = `Le salaire brut a été déduit d'un net (×1,30) — le taux horaire dérivé est indicatif`;
    return CoherenceAlertBuilder.forField<HsAlertField>('SALAIRE_DEDUIT')
      .withSeverity('INFO')
      .addSource('IA', { expectedDisplay: text, reason: text })
      .build();
  }

  alertTooltip(alert: HsCoherenceAlert): string {
    return alert.reason;
  }

  alertBadgeLabel(alert: HsCoherenceAlert): string {
    switch (alert.field) {
      case 'TAUX_HORAIRE': return `Incohérence taux horaire (${alert.expectedDisplay.split(' ')[0]})`;
      case 'HEURES_SUP': return 'Incohérence heures sup';
      case 'SALAIRE_DEDUIT': return 'Salaire brut déduit';
    }
  }
}
