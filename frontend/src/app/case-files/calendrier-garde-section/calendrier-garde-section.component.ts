import { Component, Input, OnInit, OnChanges, SimpleChanges, Optional, signal, computed } from '@angular/core';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar } from '@angular/material/snack-bar';
import { FormsModule } from '@angular/forms';
import { CalendrierGardeService } from '../../core/services/calendrier-garde.service';
import { CalendrierGardeResponse } from '../../core/models/calendrier-garde.model';
import { CaseAnalysisResult, PieceManquanteEntry } from '../../core/models/case-analysis.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { AiQuestion } from '../../core/models/ai-question.model';
import { SourceExplanationService } from '../../core/services/source-explanation.service';
import { SourceExplanation } from '../../core/models/source-explanation.model';
import { CoherencePopoverTriggerDirective } from '../../shared/coherence-popover/coherence-popover-trigger.directive';
import { CoherenceAlert, CoherenceAlertSource } from '../../shared/coherence-popover/coherence-alert.model';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';
import { CoherenceAlertBuilder } from '../../shared/coherence-popover/coherence-alert-builder';

import {
  ALL_MODES,
  CalendrierGardePrefillRules,
  MODES_BE,
  MODES_FR,
} from './calendrier-garde-section-prefill-rules';
const ALTERNEE_MODES = new Set(['ALTERNEE_FR', 'ALTERNEE_BE']);

/**
 * SF-155-18 : champs d'alerte de cohérence F-IA-03 exposés par l'outil
 * F-FA-06 (calendrier de garde).
 *
 * Seul `MODE_GARDE` (mode juridique de garde) est décisionnel — les noms
 * de parents sont purement informationnels et ne déclenchent pas
 * de vérification IA.
 */
export type FGAlertField = 'MODE_GARDE';

// SF-155-05 : alias locaux rétro-compat — utilise l'interface générique partagée.
export type FGAlertSource = CoherenceAlertSource;
export type FGCoherenceAlert = CoherenceAlert<FGAlertField>;

@Component({
  selector: 'app-calendrier-garde-section',
  standalone: true,
  imports: [FormsModule, MatButtonModule, MatIconModule, MatSelectModule, MatFormFieldModule, MatInputModule, MatProgressSpinnerModule, MatTooltipModule, CoherencePopoverTriggerDirective,
    ToolJurisprudenceCitationsComponent],
  templateUrl: './calendrier-garde-section.component.html',
  styleUrl: './calendrier-garde-section.component.scss'
})
export class CalendrierGardeSectionComponent implements OnInit, OnChanges {
  // F-JU-03 — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-FA-06-calendrier-garde';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'CALENDRIER DE GARDE';
  static readonly TOOL_ICON = 'calendar_month';

  // F-JU-03 SF-JU-03-99c — flag standalone (mode simulateur). False = mode dossier (défaut).
  @Input() standaloneMode = false;
  @Input() caseFileId!: string;
  @Input() aiModeGardeDetaille?: string | null;
  @Input() workspaceCountry: string = 'FRANCE';
  /** SF-246-10 : données IA famille pour pré-fill âges + dates calendrier. */
  @Input() aiData?: Partial<FamilleExtractedData> | null;
  @Input() synthesis?: CaseAnalysisResult | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;

  private aiDataSignal = signal<Partial<FamilleExtractedData> | null | undefined>(undefined);
  private synthesisSignal = signal<CaseAnalysisResult | null | undefined>(undefined);
  private procedureChecksSignal = signal<ProcedureCheck[]>([]);
  private aiQuestionsSignal = signal<AiQuestion[]>([]);
  private piecesManquantesSignal = signal<PieceManquanteEntry[]>([]);

  // F-177 SF-177-03b : force l'expansion (mode modal F-177).
  @Input() forceExpanded = false;

  collapsed = signal(true);
  loading = signal(false);
  generating = signal(false);
  showForm = signal(true);
  result = signal<CalendrierGardeResponse | null>(null);

  gardeCode = signal('ALTERNEE_FR');
  parentANom = signal('');
  parentBNom = signal('');
  modeDetailleNote = signal<string | null>(null);

  // SF-246-10 : nouveaux champs pré-remplis depuis aiData (FamilleExtractedData).
  agesEnfants = signal<number[]>([]);
  agesEnfantsRaw = signal<string>('');
  dateDebutCalendrier = signal<string>('');
  dateFinCalendrier = signal<string>('');

  // SF-155-18 : provenance IA par champ (badge "Pré-rempli depuis l'analyse").
  // Effacé dès que l'avocat modifie manuellement (onGardeCodeChange).
  provenanceGardeCode = signal<'IA' | null>(null);
  // SF-246-10 : provenance par champ nouveau.
  provenanceAgesEnfants = signal<'IA' | null>(null);
  provenanceDateDebutCalendrier = signal<'IA' | null>(null);
  provenanceDateFinCalendrier = signal<'IA' | null>(null);

  readonly modes = [
    { group: 'France', items: [
      { value: 'ALTERNEE_FR', label: 'Résidence alternée' },
      { value: 'DVH_CLASSIQUE_FR', label: 'DVH classique' },
      { value: 'DVH_ELARGI_FR', label: 'DVH élargi' },
    ]},
    { group: 'Belgique', items: [
      { value: 'ALTERNEE_BE', label: 'Hébergement égalitaire' },
      { value: 'SECONDAIRE_BE', label: 'Hébergement secondaire' },
      { value: 'SECONDAIRE_ELARGI_BE', label: 'Hébergement secondaire élargi' },
    ]},
  ];

  // SF-IA-03-15b — map {sourceKey → explanation}
  sourceExplanations = signal<Map<string, SourceExplanation[]>>(new Map());

  constructor(
    private gardeService: CalendrierGardeService,
    @Optional() private sourceExplanationService: SourceExplanationService | null,
    private snackBar: MatSnackBar,
    @Optional() private refreshService: CaseDashboardRefreshService | null,
  ) {}

  private loadSourceExplanations(): void {
    if (!this.caseFileId || !this.sourceExplanationService) return;
    this.sourceExplanationService.getForCaseFile(this.caseFileId).subscribe({
      next: map => this.sourceExplanations.set(map),
      error: () => { /* fail-open */ },
    });
  }

  /** SF-IA-03-15b : mapping field → sourceKey pour le popover. */
  explanationFor(_field: FGAlertField): SourceExplanation[] {
    return this.sourceExplanations().get('FA06_MODE_GARDE') ?? [];
  }

  /**
   * SF-155-18 : alertes de cohérence F-IA-03 calculées dynamiquement via le
   * `CoherenceAlertBuilder` partagé (SF-155-05). Gate : uniquement en mode
   * formulaire (pattern anti-bug SF-IA-03-12 — pas d'alerte après le
   * rendu du calendrier).
   */
  coherenceAlerts = computed<Partial<Record<FGAlertField, FGCoherenceAlert>>>(() => {
    if (!this.showForm()) return {};
    const alerts: Partial<Record<FGAlertField, FGCoherenceAlert>> = {};
    const modeAlert = this.buildModeGardeAlert();
    if (modeAlert) alerts.MODE_GARDE = modeAlert;
    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    const blockers = values.filter(a => a && a.severity === 'CRITICAL').length;
    return { total: values.length, blockers };
  });

  ngOnInit(): void {
    // F-177 SF-177-03b : appliqué dès le mount pour le mode modal.
    if (this.forceExpanded) this.collapsed.set(false);
    this.aiDataSignal.set(this.aiData);
    this.synthesisSignal.set(this.synthesis);
    this.procedureChecksSignal.set(this.procedureChecks ?? []);
    this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    this.piecesManquantesSignal.set(this.piecesManquantes ?? []);
    // SF-155-18 : garde-fou — pré-remplit au mount si aiModeGardeDetaille est
    // déjà disponible (cas : pipeline déjà tourné avant ouverture du dossier).
    this.prefillFromAi();
    this.loadExisting();
    this.loadSourceExplanations();
  }

  ngOnChanges(changes: SimpleChanges): void {
    // F-177 SF-177-03b : applique le forceExpanded quand il passe à true en cours de vie.
    if (changes['forceExpanded'] && this.forceExpanded) this.collapsed.set(false);
    if (changes['aiData']) this.aiDataSignal.set(this.aiData);
    if (changes['synthesis']) this.synthesisSignal.set(this.synthesis);
    if (changes['procedureChecks']) this.procedureChecksSignal.set(this.procedureChecks ?? []);
    if (changes['aiQuestions']) this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    if (changes['piecesManquantes']) this.piecesManquantesSignal.set(this.piecesManquantes ?? []);
    // SF-155-18 : ré-applique le prefill dès que aiModeGardeDetaille change,
    // tant qu'aucun résultat persistant n'a été chargé et qu'on est en mode
    // formulaire (pattern canonique immigration-title-decision-section).
    if ((changes['aiModeGardeDetaille'] || changes['aiData']) && this.showForm() && !this.result()) {
      this.prefillFromAi();
    }
  }

  /**
   * SF-155-18 : pré-remplit le mode de garde depuis l'analyse IA. N'écrase
   * jamais une valeur si l'avocat a déjà modifié (`provenanceGardeCode`
   * passé à null). Règle pays : si le mode IA appartient à l'autre pays que
   * le workspace courant, on affiche une note informative mais on ne
   * pré-remplit pas (gate `workspaceCountry`).
   */
  private prefillFromAi(): void {
    // F-236 SF-236-02 — délégation au helper partagé.
    const input = {
      aiModeGardeDetaille: this.aiModeGardeDetaille ?? null,
      workspaceCountry: this.workspaceCountry,
      aiData: this.aiDataSignal() ?? null,
    };
    const mode = CalendrierGardePrefillRules.computeGardeCode(input);
    const note = CalendrierGardePrefillRules.computeModeDetailleNote(input);
    if (mode) {
      this.gardeCode.set(mode);
      this.provenanceGardeCode.set('IA');
    }
    this.modeDetailleNote.set(note);

    // SF-246-10 : pré-fill âges enfants.
    const ages = CalendrierGardePrefillRules.computeAgesEnfants(input);
    if (ages.length > 0
        && (this.agesEnfants().length === 0 || this.provenanceAgesEnfants() === 'IA')) {
      this.agesEnfants.set(ages);
      this.agesEnfantsRaw.set(ages.join(', '));
      this.provenanceAgesEnfants.set('IA');
    }

    // SF-246-10 : pré-fill date de début du calendrier.
    const dateDebut = CalendrierGardePrefillRules.computeDateDebutCalendrier(input);
    if (dateDebut !== null
        && (this.dateDebutCalendrier() === '' || this.provenanceDateDebutCalendrier() === 'IA')) {
      this.dateDebutCalendrier.set(dateDebut);
      this.provenanceDateDebutCalendrier.set('IA');
    }

    // SF-246-10 : pré-fill date de fin du calendrier.
    const dateFin = CalendrierGardePrefillRules.computeDateFinCalendrier(input);
    if (dateFin !== null
        && (this.dateFinCalendrier() === '' || this.provenanceDateFinCalendrier() === 'IA')) {
      this.dateFinCalendrier.set(dateFin);
      this.provenanceDateFinCalendrier.set('IA');
    }
  }

  private buildPiecesIndex(pieces: PieceManquanteEntry[]): Record<string, string> {
    const index: Record<string, string> = {};
    if (!pieces) return index;
    for (const p of pieces) {
      const code = p.critereCode?.toUpperCase();
      if (!code) continue;
      if (code !== 'FA06_MODE_GARDE') continue;
      if (!index[code]) index[code] = p.texte;
    }
    return index;
  }

  alertTooltip(alert: FGCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: FGCoherenceAlert): string {
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
   * SF-155-18 : divergence mode de garde — scanne les 4 sources F-IA-03
   * (F96 / QUESTION_IA / IA fine + coarse / PIECE_MANQUANTE) via le builder
   * partagé SF-155-05. Hiérarchie de sources F-96 > QUESTION_IA > IA >
   * IA_COARSE (fallback) ; la première `expectedDisplay` gagne, les autres
   * consolident si même valeur (pattern canonique).
   *
   * Note : la version historique de ce composant utilisait `IA_COARSE`
   * comme source dédiée (quand l'IA n'a que la catégorie ALTERNEE/EXCLUSIVE).
   * Depuis SF-155-05 la source canonique est `IA` avec une `expectedDisplay`
   * dérivée ("mode alterné"/"mode non alterné").
   */
  private buildModeGardeAlert(): FGCoherenceAlert | null {
    const user = this.gardeCode();
    if (!user || !ALL_MODES.has(user)) return null;

    const piecesIndex = this.buildPiecesIndex(this.piecesManquantesSignal());
    const pieceTexte = piecesIndex['FA06_MODE_GARDE'] ?? null;
    const builder = CoherenceAlertBuilder.forField<FGAlertField>('MODE_GARDE');

    // 1. F-96 VERIFIED avec expected_value
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'FA06_MODE_GARDE') continue;
      if (chk.statut !== 'VERIFIED') continue;
      const ev = chk.expectedValue?.toUpperCase();
      if (!ev || !ALL_MODES.has(ev)) continue;
      if (ev !== user) {
        builder.addSource('F96', {
          expectedDisplay: ev,
          reason: `Checklist procédurale : ${ev}${chk.raison ? ' (' + chk.raison + ')' : ''}`,
        });
      }
      break;
    }

    // 2. Question IA répondue "oui" avec expected_value
    for (const q of this.aiQuestionsSignal()) {
      if (q.critereCode?.toUpperCase() !== 'FA06_MODE_GARDE') continue;
      const answer = q.answerText?.trim().toLowerCase();
      if (!answer) continue;
      const isOui = answer === 'oui' || answer.startsWith('oui ')
        || answer.startsWith('oui,') || answer.startsWith('oui.');
      if (!isOui) continue;
      const ev = q.expectedValue?.toUpperCase();
      if (!ev || !ALL_MODES.has(ev)) continue;
      if (ev === user) continue;
      builder.addSource('QUESTION_IA', {
        expectedDisplay: ev,
        reason: `Question complémentaire : "${q.questionText}" → "${q.answerText}"`,
      });
      break;
    }

    // 3. IA synthesis.pensionAlimentaireEstimate.modeGardeDetaille (valeur fine)
    const iaDetaille = this.synthesisSignal()?.pensionAlimentaireEstimate?.modeGardeDetaille?.toUpperCase();
    if (iaDetaille && ALL_MODES.has(iaDetaille) && iaDetaille !== user) {
      builder.addSource('IA', {
        expectedDisplay: iaDetaille,
        reason: `Analyse du dossier : ${iaDetaille}`,
      });
    } else {
      // 4. IA coarse (ALTERNEE/EXCLUSIVE) — fallback si pas de valeur fine.
      const iaCoarse = this.synthesisSignal()?.pensionAlimentaireEstimate?.modeGarde;
      if (iaCoarse === 'ALTERNEE' || iaCoarse === 'EXCLUSIVE') {
        const userIsAlternee = ALTERNEE_MODES.has(user);
        const iaIsAlternee = iaCoarse === 'ALTERNEE';
        if (userIsAlternee !== iaIsAlternee) {
          builder.addSource('IA', {
            expectedDisplay: iaCoarse === 'ALTERNEE' ? 'mode alterné' : 'mode non alterné',
            reason: `Analyse du dossier (catégorie) : ${iaCoarse}`,
          });
        }
      }
    }

    // 5. Pièce manquante (contributor additionnel si alerte initiée).
    if (pieceTexte) {
      builder.addPieceManquante(pieceTexte);
    }

    return builder.build();
  }

  toggleCollapsed(): void { this.collapsed.update(v => !v); }

  loadExisting(): void {
    this.loading.set(true);
    this.gardeService.get(this.caseFileId).subscribe({
      next: r => {
        this.result.set(r);
        this.prefillForm(r);
        // SF-155-18 : valeurs persistées = saisie avocat — jamais de badge IA.
        this.provenanceGardeCode.set(null);
        // SF-246-10 : même règle sur les nouveaux champs.
        this.provenanceAgesEnfants.set(null);
        this.provenanceDateDebutCalendrier.set(null);
        this.provenanceDateFinCalendrier.set(null);
        this.showForm.set(false);
        this.loading.set(false);
      },
      error: () => {
        // 404 attendu si aucun calendrier — on reste en mode formulaire.
        // SF-155-18 : fallback pré-fill IA (idempotent avec ngOnInit).
        this.showForm.set(true);
        this.loading.set(false);
        this.prefillFromAi();
      },
    });
  }

  private prefillForm(resp: CalendrierGardeResponse): void {
    if (resp.gardeCode) this.gardeCode.set(resp.gardeCode);
    if (resp.parentANom != null) this.parentANom.set(resp.parentANom);
    if (resp.parentBNom != null) this.parentBNom.set(resp.parentBNom);
  }

  generate(): void {
    this.generating.set(true);
    this.gardeService.generate(this.caseFileId, {
      gardeCode: this.gardeCode(), parentANom: this.parentANom(), parentBNom: this.parentBNom(),
    }).subscribe({
      next: r => { this.result.set(r); this.showForm.set(false); this.generating.set(false); this.refreshService?.triggerRefresh(); },
      error: () => { this.generating.set(false); this.snackBar.open('Erreur', 'Fermer', { duration: 4000 }); },
    });
  }

  editForm(): void {
    const r = this.result();
    if (r) this.prefillForm(r);
    this.showForm.set(true);
  }

  /**
   * SF-155-18 : handler change mode de garde — efface la provenance IA dès
   * que l'avocat modifie manuellement (pattern canonique), et la note
   * informative de pays mismatch.
   */
  onGardeCodeChange(): void {
    this.provenanceGardeCode.set(null);
    this.modeDetailleNote.set(null);
  }

  /**
   * SF-246-10 : handler changement du champ âges des enfants (texte brut).
   */
  onAgesEnfantsRawChange(value: string): void {
    this.agesEnfantsRaw.set(value ?? '');
    this.agesEnfants.set(this.parseAgesEnfants(value ?? ''));
    this.provenanceAgesEnfants.set(null);
  }

  /** SF-246-10 : handler date de début du calendrier. */
  onDateDebutCalendrierChange(value: string): void {
    this.dateDebutCalendrier.set(value ?? '');
    this.provenanceDateDebutCalendrier.set(null);
  }

  /** SF-246-10 : handler date de fin du calendrier. */
  onDateFinCalendrierChange(value: string): void {
    this.dateFinCalendrier.set(value ?? '');
    this.provenanceDateFinCalendrier.set(null);
  }

  /** SF-246-10 : parse la saisie textuelle d'âges (ex : "12, 8, 4") en liste d'entiers. */
  parseAgesEnfants(raw: string): number[] {
    if (!raw || !raw.trim()) return [];
    return raw.split(',')
      .map(s => parseInt(s.trim(), 10))
      .filter(n => !isNaN(n) && Number.isInteger(n) && n >= 0 && n <= 25);
  }

  /**
   * SF-155-18 : handler change parent A — pas de provenance IA (champ
   * purement informatif, non pré-rempli), méthode fournie pour symétrie
   * et hook futur.
   */
  onParentANomChange(): void {
    // Pas de provenance IA sur ce champ textuel libre.
  }

  /**
   * SF-155-18 : handler change parent B — pas de provenance IA (champ
   * purement informatif, non pré-rempli), méthode fournie pour symétrie
   * et hook futur.
   */
  onParentBNomChange(): void {
    // Pas de provenance IA sur ce champ textuel libre.
  }

  /**
   * F-IA-04 / F-177 SF-177-12 — compteur de champs pré-remplis affiché
   * en badge sur la card avant ouverture. Lit le mode de garde détecté
   * dans `synthesis.pensionAlimentaireEstimate.modeGardeDetaille` (et non
   * `aiData` direct, car F-FA-06 a son propre @Input `aiModeGardeDetaille`
   * sourcé depuis l'estimate, pas depuis FamilleExtractedData).
   *
   * Stricte parité avec `prefillFromAi()` :
   * - mode absent / non reconnu → 0
   * - mode du même pays que le workspace → 1
   * - mode de l'autre pays → 0 (note informative seulement, pas de prefill)
   */
  static getPrefillCount(input: {
    synthesis?: any;
    workspaceCountry?: string;
    aiModeGardeDetaille?: string | null;
    aiData?: Partial<FamilleExtractedData> | null;
  }): number {
    // F-236 SF-236-02 — délégation au helper partagé.
    return CalendrierGardePrefillRules.computePrefillCount(input);
  }
}
