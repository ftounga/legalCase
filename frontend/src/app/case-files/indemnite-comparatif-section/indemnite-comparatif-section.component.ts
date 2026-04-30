import { Component, Input, OnInit, OnChanges, SimpleChanges, Optional, signal, computed } from '@angular/core';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { CaseAnalysisResult, PieceManquanteEntry, TravailExtractedData } from '../../core/models/case-analysis.model';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar } from '@angular/material/snack-bar';
import { FormsModule } from '@angular/forms';
import { DecimalPipe } from '@angular/common';
import { IndemniteComparatifService } from '../../core/services/indemnite-comparatif.service';
import { IndemniteComparatifResponse } from '../../core/models/indemnite-comparatif.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { AiQuestion } from '../../core/models/ai-question.model';
import { SourceExplanationService } from '../../core/services/source-explanation.service';
import { SourceExplanation } from '../../core/models/source-explanation.model';
import { CoherencePopoverTriggerDirective } from '../../shared/coherence-popover/coherence-popover-trigger.directive';
import { CoherenceAlert, CoherenceAlertSource } from '../../shared/coherence-popover/coherence-alert.model';
import { CoherenceAlertBuilder } from '../../shared/coherence-popover/coherence-alert-builder';

interface TypeRuptureOption {
  value: string;
  label: string;
}

/**
 * SF-155-15 : champs d'alerte de cohérence F-IA-03 exposés par F-DT-09
 * (comparateur indemnités licenciement — FR / BE).
 */
export type IndemniteAlertField = 'TYPE_RUPTURE' | 'ANCIENNETE' | 'SALAIRE';

// SF-155-15 : alias locaux rétro-compat — utilise l'interface générique partagée.
export type IndemniteAlertSource = CoherenceAlertSource;
export type IndemniteCoherenceAlert = CoherenceAlert<IndemniteAlertField>;

const KNOWN_TYPE_RUPTURE_VALUES = new Set([
  'LICENCIEMENT', 'LICENCIEMENT_ECONOMIQUE', 'RUPTURE_CONVENTIONNELLE',
  'LICENCIEMENT_ORDINAIRE', 'RUPTURE_AMIABLE',
]);

// SF-132-02/03 : RUPTURE_CONVENTIONNELLE (FR) et RUPTURE_AMIABLE (BE) ont leurs
// outils dédiés ; le comparateur Macron/CCT 109 ne les accepte plus.
const TYPES_FR: TypeRuptureOption[] = [
  { value: 'LICENCIEMENT', label: 'Licenciement (cause réelle et sérieuse)' },
  { value: 'LICENCIEMENT_ECONOMIQUE', label: 'Licenciement économique' },
];
const TYPES_BE: TypeRuptureOption[] = [
  { value: 'LICENCIEMENT_ORDINAIRE', label: 'Licenciement ordinaire' },
];

@Component({
  selector: 'app-indemnite-comparatif-section',
  standalone: true,
  imports: [
    FormsModule, DecimalPipe,
    MatButtonModule, MatIconModule,
    MatSelectModule, MatFormFieldModule,
    MatInputModule, MatProgressSpinnerModule,
    MatTooltipModule,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './indemnite-comparatif-section.component.html',
  styleUrl: './indemnite-comparatif-section.component.scss'
})
export class IndemniteComparatifSectionComponent implements OnInit, OnChanges {
  // F-177 SF-177-03 : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'COMPARATEUR INDEMNITÉS';
  static readonly TOOL_ICON = 'euro_symbol';

  @Input() caseFileId!: string;
  @Input() workspaceCountry: string = 'FRANCE';
  @Input() aiData?: TravailExtractedData | null;
  @Input() synthesis?: CaseAnalysisResult | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;
  // F-177 SF-177-03 : force l'expansion (mode modal F-177).
  @Input() forceExpanded = false;

  private aiDataSignal = signal<TravailExtractedData | null | undefined>(undefined);
  private synthesisSignal = signal<CaseAnalysisResult | null | undefined>(undefined);
  private procedureChecksSignal = signal<ProcedureCheck[]>([]);
  private aiQuestionsSignal = signal<AiQuestion[]>([]);
  private piecesManquantesSignal = signal<PieceManquanteEntry[]>([]);

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<IndemniteComparatifResponse | null>(null);

  country = signal('FRANCE');
  typeRupture = signal<string>('LICENCIEMENT');
  typeRuptureNote = signal<string | null>(null);
  ancienneteAnnees = signal(5);
  ancienneteMois = signal(0);
  age = signal(35);
  salaireMensuel = signal(3000);

  // SF-155-15 : provenance IA par champ pré-rempli (badge "Pré-rempli depuis
  // l'analyse"). Remise à null dès que l'avocat modifie manuellement le champ
  // via les handlers onXxxChange() — pattern canonique immigration-title-decision-section.
  provenanceTypeRupture = signal<'IA' | null>(null);
  provenanceAncienneteAnnees = signal<'IA' | null>(null);
  provenanceAncienneteMois = signal<'IA' | null>(null);
  provenanceSalaire = signal<'IA' | null>(null);

  typeRuptureOptions = computed<TypeRuptureOption[]>(() =>
    this.country() === 'BELGIQUE' ? TYPES_BE : TYPES_FR
  );

  barMaxWidth = computed(() => {
    const r = this.result();
    if (!r) return 0;
    return r.baremePlafondMois;
  });

  coherenceAlerts = computed<Partial<Record<IndemniteAlertField, IndemniteCoherenceAlert>>>(() => {
    if (!this.showForm()) return {};
    const alerts: Partial<Record<IndemniteAlertField, IndemniteCoherenceAlert>> = {};
    const typeAlert = this.buildTypeRuptureAlert();
    if (typeAlert) alerts.TYPE_RUPTURE = typeAlert;
    const ancAlert = this.buildAncienneteAlert();
    if (ancAlert) alerts.ANCIENNETE = ancAlert;
    const salAlert = this.buildSalaireAlert();
    if (salAlert) alerts.SALAIRE = salAlert;
    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    return {
      total: values.length,
      blockers: values.filter(a => a.severity === 'CRITICAL').length,
    };
  });

  /**
   * SF-155-15 : divergence `TYPE_RUPTURE` — aggrégation F96 + QUESTION_IA + IA.
   * `severity: CRITICAL` car une saisie de type de rupture divergente envoie
   * l'avocat vers un barème incorrect (enjeu financier direct sur l'indemnité).
   */
  private buildTypeRuptureAlert(): IndemniteCoherenceAlert | null {
    const userValue = this.typeRupture();
    if (!userValue) return null;

    // Collecte des sources qui affirment une valeur attendue (même match ou divergence).
    // Si au moins une source confirme la saisie avocat, pas d'alerte (pattern canonique :
    // "la première source qui confirme gagne — les divergences ultérieures sont stales").
    type Candidate = { source: Exclude<CoherenceAlertSource, 'MULTI'>; expected: string; reason: string };
    const candidates: Candidate[] = [];

    // B — F-96 VERIFIED avec expected_value
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'DT09_TYPE_RUPTURE') continue;
      if (chk.statut !== 'VERIFIED') continue;
      const ev = chk.expectedValue?.toUpperCase();
      if (!ev || !KNOWN_TYPE_RUPTURE_VALUES.has(ev)) continue;
      candidates.push({
        source: 'F96',
        expected: ev,
        reason: `Checklist procédurale : ${ev}${chk.raison ? ' (' + chk.raison + ')' : ''}`,
      });
      break;
    }

    // C — Question IA "oui" avec expected_value
    for (const q of this.aiQuestionsSignal()) {
      if (q.critereCode?.toUpperCase() !== 'DT09_TYPE_RUPTURE') continue;
      const answer = q.answerText?.trim().toLowerCase();
      if (!answer) continue;
      const isOui = answer === 'oui' || answer.startsWith('oui ') || answer.startsWith('oui,') || answer.startsWith('oui.');
      if (!isOui) continue;
      const ev = q.expectedValue?.toUpperCase();
      if (!ev || !KNOWN_TYPE_RUPTURE_VALUES.has(ev)) continue;
      candidates.push({
        source: 'QUESTION_IA',
        expected: ev,
        reason: `Question complémentaire : "${q.questionText}" → "${q.answerText}"`,
      });
      break;
    }

    // D — IA detection (compensationEstimate)
    const iaType = this.synthesisSignal()?.compensationEstimate?.typeRupture?.toUpperCase();
    if (iaType && KNOWN_TYPE_RUPTURE_VALUES.has(iaType)) {
      candidates.push({ source: 'IA', expected: iaType, reason: `Analyse du dossier : ${iaType}` });
    }

    if (candidates.length === 0) return null;
    // Si n'importe quelle source confirme la saisie avocat, pas d'alerte.
    if (candidates.some(c => c.expected === userValue)) return null;

    const builder = CoherenceAlertBuilder
      .forField<IndemniteAlertField>('TYPE_RUPTURE')
      .withSeverity('CRITICAL');
    for (const c of candidates) {
      builder.addSource(c.source, { expectedDisplay: c.expected, reason: c.reason });
    }

    // E — Pièce manquante (contributor additionnel si alerte déjà initiée)
    const pieceTexte = this.findPieceManquante(['DT09_TYPE_RUPTURE']);
    if (pieceTexte) builder.addPieceManquante(pieceTexte);

    return builder.build();
  }

  /**
   * SF-155-15 : divergence `ANCIENNETE` — écart ≥ 1 mois total avec l'IA.
   * `severity: WARNING` (impact graduel sur le barème, pas bloquant).
   */
  private buildAncienneteAlert(): IndemniteCoherenceAlert | null {
    const ce = this.synthesisSignal()?.compensationEstimate;
    if (!ce) return null;
    const iaYears = ce.ancienneteAnnees;
    const iaMonths = ce.ancienneteMois ?? 0;
    if (iaYears == null) return null;
    const iaTotalMois = iaYears * 12 + iaMonths;
    const userYears = this.ancienneteAnnees();
    const userMois = this.ancienneteMois();
    if (userYears <= 0 && userMois <= 0) return null;
    const userTotalMois = userYears * 12 + userMois;
    // Seuil 1 mois pour éviter les faux positifs mais rester actionnable.
    if (Math.abs(userTotalMois - iaTotalMois) < 1) return null;
    return CoherenceAlertBuilder
      .forField<IndemniteAlertField>('ANCIENNETE')
      .addSource('IA', {
        expectedDisplay: `${iaYears} ans ${iaMonths ? iaMonths + ' mois' : ''}`.trim(),
        reason: `Analyse du dossier : ${iaYears} ans ${iaMonths ? iaMonths + ' mois' : ''}`.trim(),
      })
      .build();
  }

  /**
   * SF-155-15 : divergence `SALAIRE` — écart relatif > 5 % avec l'IA.
   * `severity: WARNING`.
   */
  private buildSalaireAlert(): IndemniteCoherenceAlert | null {
    const ce = this.synthesisSignal()?.compensationEstimate;
    if (!ce || ce.salaireReference == null) return null;
    const ia = ce.salaireReference;
    const user = this.salaireMensuel();
    if (user <= 0) return null;
    const base = Math.max(Math.abs(ia), 1);
    if (Math.abs(user - ia) / base < 0.05) return null;
    return CoherenceAlertBuilder
      .forField<IndemniteAlertField>('SALAIRE')
      .addSource('IA', {
        expectedDisplay: `${ia} €`,
        reason: `Analyse du dossier : ${ia} €`,
      })
      .build();
  }

  /**
   * SF-155-15 : recherche d'une pièce manquante F-145 sur un des codes
   * critères acceptés ; renvoie le texte avocat ou null si aucune match.
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

  alertTooltip(alert: IndemniteCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: IndemniteCoherenceAlert): string {
    const prefix = (() => {
      switch (alert.source) {
        case 'F96': return 'Incohérence Checklist procédurale';
        case 'QUESTION_IA': return 'Incohérence Question complémentaire';
        case 'IA': return 'Incohérence détectée';
        case 'PIECE_MANQUANTE': return 'Pièce manquante';
        case 'MULTI': return 'Incohérence multiple';
      }
    })();
    return alert.field === 'TYPE_RUPTURE' ? `${prefix} (${alert.expectedDisplay})` : prefix;
  }

  // SF-IA-03-15b — map {sourceKey → explanation} pour le popover enrichi
  sourceExplanations = signal<Map<string, SourceExplanation[]>>(new Map());

  constructor(
    private comparatifService: IndemniteComparatifService,
    // SF-155-15 : @Optional() aligné sur le canonique pour fail-open si
    // le service n'est pas dispo (tests DI minimaux).
    @Optional() private sourceExplanationService: SourceExplanationService | null,
    private snackBar: MatSnackBar,
    @Optional() private refreshService: CaseDashboardRefreshService | null,
  ) {}

  ngOnInit(): void {
    // F-177 SF-177-03 : appliqué dès le mount pour le mode modal.
    if (this.forceExpanded) this.collapsed.set(false);

    // SF-106-06 : initialiser le pays depuis le workspace (plus de FRANCE en dur).
    this.country.set(this.workspaceCountry);
    this.typeRupture.set(this.workspaceCountry === 'BELGIQUE' ? 'LICENCIEMENT_ORDINAIRE' : 'LICENCIEMENT');
    // SF-155-15 : push inputs vers signals avant toute évaluation computed.
    this.aiDataSignal.set(this.aiData);
    this.synthesisSignal.set(this.synthesis);
    this.procedureChecksSignal.set(this.procedureChecks ?? []);
    this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    this.piecesManquantesSignal.set(this.piecesManquantes ?? []);
    this.loadExisting();
    this.loadSourceExplanations();
  }

  private loadSourceExplanations(): void {
    if (!this.caseFileId || !this.sourceExplanationService) return;
    this.sourceExplanationService.getForCaseFile(this.caseFileId).subscribe({
      next: map => this.sourceExplanations.set(map),
      error: () => { /* fail-open */ },
    });
  }

  /** SF-IA-03-15b : mapping champ → sourceKey F-DT-09 (F96 code pour TYPE_RUPTURE, génériques sinon). */
  explanationFor(field: IndemniteAlertField): SourceExplanation[] {
    const key = field === 'TYPE_RUPTURE' ? 'DT09_TYPE_RUPTURE'
              : field === 'ANCIENNETE' ? 'date_entree'
              : 'salaire_brut_mensuel';
    return this.sourceExplanations().get(key) ?? [];
  }

  ngOnChanges(changes: SimpleChanges): void {
    // F-177 SF-177-03 : applique le forceExpanded quand il passe à true en cours de vie.
    if (changes['forceExpanded'] && this.forceExpanded) this.collapsed.set(false);

    if (changes['aiData']) this.aiDataSignal.set(this.aiData);
    if (changes['synthesis']) this.synthesisSignal.set(this.synthesis);
    if (changes['procedureChecks']) this.procedureChecksSignal.set(this.procedureChecks ?? []);
    if (changes['aiQuestions']) this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    if (changes['piecesManquantes']) this.piecesManquantesSignal.set(this.piecesManquantes ?? []);
    if ((changes['aiData'] || changes['synthesis']) && this.showForm() && !this.result()) {
      this.prefillFromAi();
    }
  }

  toggleCollapsed(): void {
    this.collapsed.update(v => !v);
  }

  loadExisting(): void {
    this.loading.set(true);
    this.comparatifService.get(this.caseFileId).subscribe({
      next: resp => {
        this.result.set(resp);
        this.prefillForm(resp);
        this.showForm.set(false);
        this.loading.set(false);
      },
      error: () => {
        this.prefillFromAi();
        this.showForm.set(true);
        this.loading.set(false);
      },
    });
  }

  calculate(): void {
    this.calculating.set(true);
    this.comparatifService.calculate(this.caseFileId, {
      country: this.country(),
      typeRupture: this.typeRupture(),
      ancienneteAnnees: this.ancienneteAnnees(),
      ancienneteMois: this.ancienneteMois(),
      age: this.age(),
      salaireMensuel: this.salaireMensuel(),
    }).subscribe({
      next: resp => {
        this.result.set(resp);
        this.showForm.set(false);
        this.calculating.set(false);
        this.refreshService?.triggerRefresh();
      },
      error: () => {
        this.calculating.set(false);
        this.snackBar.open('Erreur lors du calcul', 'Fermer', { duration: 4000 });
      },
    });
  }

  editForm(): void {
    const r = this.result();
    if (r) this.prefillForm(r);
    this.showForm.set(true);
  }

  barWidth(mois: number): string {
    const max = this.barMaxWidth();
    if (!max || max === 0) return '0%';
    return Math.min(100, (mois / max) * 100) + '%';
  }

  private prefillForm(resp: IndemniteComparatifResponse): void {
    this.country.set(resp.country);
    this.ancienneteAnnees.set(resp.ancienneteAnnees);
    if (resp.ancienneteMois != null) this.ancienneteMois.set(resp.ancienneteMois);
    this.age.set(resp.age);
    this.salaireMensuel.set(resp.salaireMensuel);
    if (resp.typeRupture) {
      this.typeRupture.set(resp.typeRupture);
    } else {
      // Legacy result sans type — fallback par défaut selon pays
      this.typeRupture.set(resp.country === 'BELGIQUE' ? 'LICENCIEMENT_ORDINAIRE' : 'LICENCIEMENT');
    }
    // SF-155-15 : valeurs persistées = saisie avocat (jamais de badge IA).
    this.provenanceTypeRupture.set(null);
    this.provenanceAncienneteAnnees.set(null);
    this.provenanceAncienneteMois.set(null);
    this.provenanceSalaire.set(null);
  }

  /**
   * SF-155-15 : pré-remplit les champs depuis `aiData` + `synthesis` (IA).
   * Signaux `provenance*` marqués 'IA' pour afficher un badge `auto_awesome`
   * à côté de chaque champ. Pattern canonique immigration-title-decision-section.
   */
  private prefillFromAi(): void {
    // 1. Salaire brut mensuel depuis aiData.
    const ai = this.aiDataSignal();
    if (ai?.salaireBrutMensuel) {
      this.salaireMensuel.set(ai.salaireBrutMensuel);
      this.provenanceSalaire.set('IA');
    }
    // 2. Ancienneté (années + mois) depuis compensationEstimate.
    const ce = this.synthesisSignal()?.compensationEstimate;
    if (ce?.ancienneteAnnees != null) {
      this.ancienneteAnnees.set(ce.ancienneteAnnees);
      this.provenanceAncienneteAnnees.set('IA');
    }
    if (ce?.ancienneteMois != null) {
      this.ancienneteMois.set(ce.ancienneteMois);
      this.provenanceAncienneteMois.set('IA');
    }
    // 3. Type de rupture (logique dédiée : gate pays + note si non supporté).
    this.applyTypeRupturePrefill();
  }

  private applyTypeRupturePrefill(): void {
    const iaType = this.synthesisSignal()?.compensationEstimate?.typeRupture;
    if (!iaType) {
      this.typeRuptureNote.set(null);
      return;
    }
    const allowed = this.typeRuptureOptions().map(o => o.value);
    if (allowed.includes(iaType)) {
      this.typeRupture.set(iaType);
      this.provenanceTypeRupture.set('IA');
      this.typeRuptureNote.set(null);
    } else {
      // IA détecte autre chose (démission, prise d'acte, ou type de l'autre pays)
      this.typeRupture.set(allowed[0]);
      this.provenanceTypeRupture.set(null);
      this.typeRuptureNote.set(
        `Type détecté : "${iaType}" non couvert par cet outil. Vérifier que le comparateur est adapté.`
      );
    }
  }

  // SF-155-15 : handlers de changement manuel — effacent le badge "Pré-rempli
  // depuis l'analyse" dès que l'avocat modifie la valeur (pattern canonique).

  onTypeRuptureChange(): void {
    this.provenanceTypeRupture.set(null);
  }

  onAncienneteAnneesChange(): void {
    this.provenanceAncienneteAnnees.set(null);
  }

  onAncienneteMoisChange(): void {
    this.provenanceAncienneteMois.set(null);
  }

  onSalaireChange(): void {
    this.provenanceSalaire.set(null);
  }
}
