import { Component, Input, OnInit, OnChanges, SimpleChanges, Optional, signal, computed } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { InaptitudeService } from '../../core/services/inaptitude.service';
import {
  InaptitudeResponse,
  OrigineInaptitude,
  OrigineInaptitudeOption,
  ORIGINES_BE,
  ORIGINES_FR,
} from '../../core/models/inaptitude.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { TravailExtractedData, PieceManquanteEntry } from '../../core/models/case-analysis.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { AiQuestion } from '../../core/models/ai-question.model';
import { CoherencePopoverTriggerDirective } from '../../shared/coherence-popover/coherence-popover-trigger.directive';
import { CoherenceAlert, CoherenceAlertSource } from '../../shared/coherence-popover/coherence-alert.model';
import { CoherenceAlertBuilder } from '../../shared/coherence-popover/coherence-alert-builder';
import { SourceExplanation } from '../../core/models/source-explanation.model';
import { SourceExplanationService } from '../../core/services/source-explanation.service';
import {
  InaptitudeSectionPrefillRules,
} from './inaptitude-section-prefill-rules';

/**
 * SF-155-04-A2 : types pour les alertes de cohérence IA (pattern F-IA-03 aligné
 * sur `immigration-title-decision-section`).
 * SF-155-05 : interface factorisée via `CoherenceAlert<F>` shared.
 */
export type InaptitudeAlertField = 'SALAIRE' | 'ORIGINE' | 'AVIS_DATE' | 'RECLASSEMENT';
export type InaptitudeAlertSource = CoherenceAlertSource;
export type InaptitudeCoherenceAlert = CoherenceAlert<InaptitudeAlertField>;

/**
 * SF-155-04-A2 : mapping origine IA (enum tripartite AT/MP/MO) vers enum
 * frontend FR binaire (PROFESSIONNELLE/NON_PROFESSIONNELLE — article L.1226-14
 * CT couvre accident travail + maladie professionnelle).
 */
const ORIGINE_IA_TO_FRONT: Record<string, OrigineInaptitude> = {
  ACCIDENT_TRAVAIL: 'PROFESSIONNELLE',
  MALADIE_PROFESSIONNELLE: 'PROFESSIONNELLE',
  MALADIE_ORDINAIRE: 'NON_PROFESSIONNELLE',
};

const SALAIRE_DIVERGENCE_THRESHOLD = 0.10; // 10 %
const ISO_DATE_REGEX = /^\d{4}-\d{2}-\d{2}$/;

/**
 * SF-DT-15-02 : outil décisionnel dédié "Licenciement pour inaptitude"
 * (F-DT-15). FR + BE. Consomme l'API SF-DT-15-01. Affiché conditionnellement
 * par le panel F-IA-04 (tool_id 'F-DT-15-inaptitude').
 *
 * SF-155-04-A2 : ajout du pré-remplissage IA (5 champs) + validation F-IA-03
 * (alertes cohérence) aligné sur le pattern canonique
 * `immigration-title-decision-section` (F-IM-05 SF-IM-05-04).
 */
@Component({
  selector: 'app-inaptitude-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule, DecimalPipe,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatSlideToggleModule, MatProgressSpinnerModule,
    LegalCitationsPipe,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './inaptitude-section.component.html',
  styleUrl: './inaptitude-section.component.scss',
})
export class InaptitudeSectionComponent implements OnInit, OnChanges {
  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'LICENCIEMENT POUR INAPTITUDE';

  /** F-177 SF-177-12 / F-236 SF-236-02 — délègue au helper partagé (parité runtime). */
  static getPrefillCount(input: {
    aiData?: any;
    procedureChecks?: any[];
    aiQuestions?: any[];
    piecesManquantes?: any[];
    triggerEvents?: any[];
    workspaceCountry?: string;
  }): number {
    return InaptitudeSectionPrefillRules.computePrefillCount({
      aiData: input.aiData,
      workspaceCountry: input.workspaceCountry,
    });
  }

  static readonly TOOL_ICON = 'medical_services';

  @Input() caseFileId!: string;
  // F-177 SF-177-03b : force l'expansion (mode modal F-177).
  @Input() forceExpanded = false;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';
  // SF-155-04-A2 : sources IA pour pré-fill + validation F-IA-03
  @Input() aiData?: TravailExtractedData | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;

  // SF-155-06 : signals miroirs des inputs IA pour que les `computed`
  // (coherenceAlerts) réagissent aux changements post-mount. Aligné sur le
  // pattern canonique `immigration-title-decision-section` (lignes 95-98).
  private aiDataSignal = signal<TravailExtractedData | null | undefined>(undefined);
  private procedureChecksSignal = signal<ProcedureCheck[]>([]);
  private aiQuestionsSignal = signal<AiQuestion[]>([]);
  private piecesManquantesSignal = signal<PieceManquanteEntry[]>([]);

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<InaptitudeResponse | null>(null);

  salaireMensuelReference = signal<number | null>(null);
  ancienneteAnnees = signal<number | null>(null);
  origineInaptitude = signal<OrigineInaptitude | null>(null);
  reclassementRespecte = signal<boolean>(false);
  avisMedecinTravailDate = signal<string | null>(null);

  // SF-155-04-A2 : provenance IA par champ (badge "Pré-rempli depuis l'analyse").
  // Effacée dès que l'avocat modifie manuellement un champ (onXxxChange).
  provenanceSalaire = signal<'IA' | null>(null);
  provenanceAnciennete = signal<'IA' | null>(null);
  provenanceOrigineInaptitude = signal<'IA' | null>(null);
  provenanceAvisMedecinDate = signal<'IA' | null>(null);
  provenanceReclassement = signal<'IA' | null>(null);

  originesDisponibles = computed<OrigineInaptitudeOption[]>(() =>
    this.workspaceCountry === 'BELGIQUE' ? ORIGINES_BE : ORIGINES_FR
  );

  /** SF-155-04-A2 : note info IA (pas une alerte bloquante) si salaire déduit d'un net × 1,30. */
  salaireEstDeduitNote = computed<boolean>(() => this.aiDataSignal()?.salaireEstDeduit === true);

  /** SF-155-04-A2 : alertes de cohérence F-IA-03 entre valeurs avocat et IA. */
  coherenceAlerts = computed<Partial<Record<InaptitudeAlertField, InaptitudeCoherenceAlert>>>(() => {
    if (!this.showForm()) return {};
    const alerts: Partial<Record<InaptitudeAlertField, InaptitudeCoherenceAlert>> = {};
    const salaireAlert = this.buildSalaireAlert();
    if (salaireAlert) alerts.SALAIRE = salaireAlert;
    const origineAlert = this.buildOrigineAlert();
    if (origineAlert) alerts.ORIGINE = origineAlert;
    const avisAlert = this.buildAvisDateAlert();
    if (avisAlert) alerts.AVIS_DATE = avisAlert;
    const reclassAlert = this.buildReclassementAlert();
    if (reclassAlert) alerts.RECLASSEMENT = reclassAlert;
    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    return { total: values.length };
  });

  // SF-155-07 (DIV-7) : map {sourceKey → explanations} pour les popovers
  // SF-IA-03-15c. Alimentée au mount via `loadSourceExplanations()`, consultée
  // par `explanationFor(field)`. Fail-open (map vide si service absent ou erreur).
  sourceExplanations = signal<Map<string, SourceExplanation[]>>(new Map());

  constructor(
    private service: InaptitudeService,
    private snackBar: MatSnackBar,
    @Optional() private dashboardRefresh: CaseDashboardRefreshService,
    // SF-155-07 (DIV-7) : injection `@Optional()` — fail-open strict.
    @Optional() private sourceExplanationService: SourceExplanationService | null,
  ) {}

  ngOnInit(): void {
    // F-177 SF-177-03b : appliqué dès le mount pour le mode modal.
    if (this.forceExpanded) this.collapsed.set(false);

    // SF-155-06 : hydrate les signals miroirs avant toute évaluation computed.
    this.aiDataSignal.set(this.aiData);
    this.procedureChecksSignal.set(this.procedureChecks ?? []);
    this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    this.piecesManquantesSignal.set(this.piecesManquantes ?? []);
    this.load();
    // SF-155-07 (DIV-7) : pré-charge les explications de sources F-IA-03-15a.
    this.loadSourceExplanations();
  }

  /**
   * SF-155-07 (DIV-7) : charge les explications de sources via l'endpoint
   * transversal F-IA-03-15a. Fail-open strict : toute erreur ou absence du
   * service laisse la map vide, les popovers affichent alors le fallback
   * `reason` sans contenu enrichi.
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
   * Convention `INAPT_<FIELD>` (upper_case).
   */
  explanationFor(field: InaptitudeAlertField): SourceExplanation[] {
    const key = (() => {
      switch (field) {
        case 'SALAIRE': return 'INAPT_SALAIRE';
        case 'ORIGINE': return 'INAPT_ORIGINE';
        case 'AVIS_DATE': return 'INAPT_AVIS_MEDECIN';
        case 'RECLASSEMENT': return 'INAPT_RECLASSEMENT';
      }
    })();
    return this.sourceExplanations().get(key) ?? [];
  }

  ngOnChanges(changes: SimpleChanges): void {
    // F-177 SF-177-03b : applique le forceExpanded quand il passe à true en cours de vie.
    if (changes['forceExpanded'] && this.forceExpanded) this.collapsed.set(false);

    // SF-155-06 : ré-hydrate les signals miroirs à chaque changement d'input.
    if (changes['aiData']) this.aiDataSignal.set(this.aiData);
    if (changes['procedureChecks']) this.procedureChecksSignal.set(this.procedureChecks ?? []);
    if (changes['aiQuestions']) this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    if (changes['piecesManquantes']) this.piecesManquantesSignal.set(this.piecesManquantes ?? []);
    // SF-155-04-A2 : ré-applique le pré-fill si aiData change APRÈS ngOnInit,
    // tant que l'avocat n'a pas encore validé un résultat (form actif, pas de
    // persistance côté backend). Pas d'écrasement si result() est déjà chargé.
    if (changes['aiData'] && !changes['aiData'].firstChange
        && this.showForm() && !this.result()) {
      this.prefillFromAi();
    }
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  formValid(): boolean {
    const s = this.salaireMensuelReference();
    const a = this.ancienneteAnnees();
    const o = this.origineInaptitude();
    return s !== null && s > 0
      && a !== null && a >= 0 && Number.isInteger(a)
      && o !== null;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request = {
      salaireMensuelReference: this.salaireMensuelReference()!,
      ancienneteAnnees: this.ancienneteAnnees()!,
      origineInaptitude: this.origineInaptitude()!,
      reclassementRespecte: this.reclassementRespecte(),
      ...(this.avisMedecinTravailDate()
        ? { avisMedecinTravailDate: this.avisMedecinTravailDate()! }
        : {}),
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Indemnité calculée', 'OK', { duration: 2500 });
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
        this.salaireMensuelReference.set(r.salaireMensuelReference);
        this.ancienneteAnnees.set(r.ancienneteAnnees);
        this.origineInaptitude.set(r.origineInaptitude);
        this.reclassementRespecte.set(r.reclassementRespecte);
        this.avisMedecinTravailDate.set(r.avisMedecinTravailDate);
        this.showForm.set(false);
        this.loading.set(false);
      },
      error: () => {
        // 404 attendu si aucune analyse — on reste en mode formulaire.
        // SF-155-04-A2 : dans ce cas, on tente un pré-fill depuis l'analyse IA.
        this.loading.set(false);
        this.prefillFromAi();
      },
    });
  }

  /**
   * SF-155-04-A2 : pré-remplit les champs depuis l'analyse IA (`aiData`).
   * Appelée uniquement quand aucune analyse persistée n'existe (404) ou quand
   * `aiData` change pendant l'édition (ngOnChanges). Ne jamais écraser une
   * analyse déjà persistée côté backend.
   */
  private prefillFromAi(): void {
    if (!this.aiData) return;

    // F-236 SF-236-02 : valeurs calculées par le helper partagé (parité static).
    const ruleInput = { aiData: this.aiData, workspaceCountry: this.workspaceCountry };

    // 1. Salaire brut mensuel → salaireMensuelReference
    const salaire = InaptitudeSectionPrefillRules.computeSalaireMensuel(ruleInput);
    if (salaire !== null) {
      this.salaireMensuelReference.set(salaire);
      this.provenanceSalaire.set('IA');
    }

    // 2. Ancienneté dérivée depuis dateEntree
    const anciennete = InaptitudeSectionPrefillRules.computeAncienneteAnnees(ruleInput);
    if (anciennete !== null) {
      this.ancienneteAnnees.set(anciennete);
      this.provenanceAnciennete.set('IA');
    }

    // 3. Origine inaptitude (FR uniquement)
    const origine = InaptitudeSectionPrefillRules.computeOrigineInaptitude(ruleInput);
    if (origine !== null) {
      this.origineInaptitude.set(origine);
      this.provenanceOrigineInaptitude.set('IA');
    }

    // 4. Date avis médecin du travail (YYYY-MM-DD strict)
    const avisDate = InaptitudeSectionPrefillRules.computeAvisMedecinDate(ruleInput);
    if (avisDate !== null) {
      this.avisMedecinTravailDate.set(avisDate);
      this.provenanceAvisMedecinDate.set('IA');
    }

    // 5. Reclassement respecté : OUI → true, NON → false, INCONNU → ne touche pas
    const reclassement = InaptitudeSectionPrefillRules.computeReclassementRespecte(ruleInput);
    if (reclassement !== null) {
      this.reclassementRespecte.set(reclassement);
      this.provenanceReclassement.set('IA');
    }
  }

  /**
   * Calcule l'ancienneté en années entières (arrondi inférieur) entre `dateEntree`
   * et aujourd'hui. Retourne `null` si date future ou date invalide.
   */
  private computeAncienneteAnnees(dateEntree: string): number | null {
    const entree = new Date(dateEntree);
    if (isNaN(entree.getTime())) return null;
    const now = new Date();
    if (entree > now) return null;
    const diffMs = now.getTime() - entree.getTime();
    const years = Math.floor(diffMs / (365.25 * 24 * 60 * 60 * 1000));
    return years >= 0 ? years : null;
  }

  // Handlers manuels : effacent le badge de provenance IA correspondant
  onSalaireChange(value: number | null): void {
    this.salaireMensuelReference.set(value);
    this.provenanceSalaire.set(null);
  }

  onAncienneteChange(value: number | null): void {
    this.ancienneteAnnees.set(value);
    this.provenanceAnciennete.set(null);
  }

  onOrigineChange(value: OrigineInaptitude | null): void {
    this.origineInaptitude.set(value);
    this.provenanceOrigineInaptitude.set(null);
  }

  onAvisMedecinDateChange(value: string | null): void {
    this.avisMedecinTravailDate.set(value || null);
    this.provenanceAvisMedecinDate.set(null);
  }

  onReclassementChange(value: boolean): void {
    this.reclassementRespecte.set(value);
    this.provenanceReclassement.set(null);
  }

  // Helpers alert builders (pattern F-IM-05 / F-IA-03)
  alertTooltip(alert: InaptitudeCoherenceAlert): string {
    return alert.reason;
  }

  alertBadgeLabel(alert: InaptitudeCoherenceAlert): string {
    return `Incohérence détectée (${alert.expectedDisplay})`;
  }

  private buildSalaireAlert(): InaptitudeCoherenceAlert | null {
    const iaVal = this.aiDataSignal()?.salaireBrutMensuel;
    const user = this.salaireMensuelReference();
    if (typeof iaVal !== 'number' || iaVal <= 0) return null;
    if (user === null || user <= 0) return null;
    const ratio = Math.abs(user - iaVal) / iaVal;
    if (ratio <= SALAIRE_DIVERGENCE_THRESHOLD) return null;
    const builder = CoherenceAlertBuilder.forField<InaptitudeAlertField>('SALAIRE')
      .addSource('IA', {
        expectedDisplay: `${iaVal.toLocaleString('fr-FR')} €`,
        reason: `Analyse du dossier : salaire ${iaVal.toLocaleString('fr-FR')} € (écart > 10 % avec la valeur saisie)`,
      });
    // SF-155-06 : fiche de paie manquante — contributor additionnel.
    const piece = this.findPieceManquante(['SALAIRE_BRUT_MENSUEL', 'INAPT_SALAIRE']);
    if (piece) builder.addPieceManquante(piece);
    return builder.build();
  }

  private buildOrigineAlert(): InaptitudeCoherenceAlert | null {
    // Ignorer côté BE (mapping non applicable)
    if (this.workspaceCountry === 'BELGIQUE') return null;
    const user = this.origineInaptitude();
    if (!user) return null;
    const builder = CoherenceAlertBuilder.forField<InaptitudeAlertField>('ORIGINE');

    // SF-155-06 : F96 — critereCode `INAPT_ORIGINE` NON_COMPLIANT avec expectedValue dans l'enum front.
    const origineEnum = new Set<OrigineInaptitude>(['PROFESSIONNELLE', 'NON_PROFESSIONNELLE']);
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'INAPT_ORIGINE') continue;
      const ev = chk.expectedValue?.toUpperCase() as OrigineInaptitude | undefined;
      if (!ev || !origineEnum.has(ev)) continue;
      if (ev === user) continue;
      const label = ev === 'PROFESSIONNELLE' ? 'Origine professionnelle' : 'Origine non professionnelle';
      builder.addSource('F96', {
        expectedDisplay: label,
        reason: `Checklist procédurale : origine attendue ${label}${chk.raison ? ' (' + chk.raison + ')' : ''}`,
      });
      break;
    }

    // SF-155-06 : QUESTION_IA — réponse "oui" sur `INAPT_ORIGINE`.
    for (const q of this.aiQuestionsSignal()) {
      if (q.critereCode?.toUpperCase() !== 'INAPT_ORIGINE') continue;
      const answer = q.answerText?.trim().toLowerCase();
      if (!answer) continue;
      const isOui = answer === 'oui' || answer.startsWith('oui ')
        || answer.startsWith('oui,') || answer.startsWith('oui.');
      if (!isOui) continue;
      const ev = q.expectedValue?.toUpperCase() as OrigineInaptitude | undefined;
      if (!ev || !origineEnum.has(ev)) continue;
      if (ev === user) continue;
      const label = ev === 'PROFESSIONNELLE' ? 'Origine professionnelle' : 'Origine non professionnelle';
      builder.addSource('QUESTION_IA', {
        expectedDisplay: label,
        reason: `Question complémentaire : "${q.questionText}" → "${q.answerText}"`,
      });
      break;
    }

    // IA — mapping AT/MP/MO → enum FR.
    const iaCode = this.aiDataSignal()?.origineInaptitudePressentie;
    if (iaCode && ORIGINE_IA_TO_FRONT[iaCode]) {
      const mapped = ORIGINE_IA_TO_FRONT[iaCode];
      if (mapped !== user) {
        const label = mapped === 'PROFESSIONNELLE'
          ? 'Origine professionnelle'
          : 'Origine non professionnelle';
        builder.addSource('IA', {
          expectedDisplay: label,
          reason: `Analyse du dossier : origine ${iaCode} → ${label}`,
        });
      }
    }

    // SF-155-06 : PIECE_MANQUANTE (accidentologique / certificat MP).
    const piece = this.findPieceManquante(['INAPT_ORIGINE']);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  private buildAvisDateAlert(): InaptitudeCoherenceAlert | null {
    const iaDate = this.aiDataSignal()?.avisMedecinTravailDate;
    const user = this.avisMedecinTravailDate();
    if (!user) return null;
    const builder = CoherenceAlertBuilder.forField<InaptitudeAlertField>('AVIS_DATE');
    // IA : divergence date.
    if (iaDate && ISO_DATE_REGEX.test(iaDate) && user !== iaDate) {
      builder.addSource('IA', {
        expectedDisplay: iaDate,
        reason: `Analyse du dossier : avis du ${iaDate}`,
      });
    }
    // SF-155-06 : PIECE_MANQUANTE (avis du médecin du travail absent).
    const piece = this.findPieceManquante(['INAPT_AVIS_MEDECIN', 'AVIS_MEDECIN_DATE']);
    if (piece) builder.addPieceManquante(piece);
    return builder.build();
  }

  private buildReclassementAlert(): InaptitudeCoherenceAlert | null {
    const builder = CoherenceAlertBuilder.forField<InaptitudeAlertField>('RECLASSEMENT');
    const user = this.reclassementRespecte();

    // SF-155-06 : QUESTION_IA — question "Obligation de reclassement a-t-elle été respectée ?"
    // où la réponse "oui"/"non" est traduite en booléen et comparée à l'avocat.
    for (const q of this.aiQuestionsSignal()) {
      if (q.critereCode?.toUpperCase() !== 'INAPT_RECLASSEMENT') continue;
      const answer = q.answerText?.trim().toLowerCase();
      if (!answer) continue;
      // On utilise l'answerText directement (pas besoin d'expectedValue pour
      // un booléen — la réponse binaire encapsule déjà la valeur attendue).
      let qRespecte: boolean | null = null;
      if (answer === 'oui' || answer.startsWith('oui ') || answer.startsWith('oui,') || answer.startsWith('oui.')) {
        qRespecte = true;
      } else if (answer === 'non' || answer.startsWith('non ') || answer.startsWith('non,') || answer.startsWith('non.')) {
        qRespecte = false;
      }
      if (qRespecte === null) continue;
      if (qRespecte === user) continue;
      const label = qRespecte ? 'Reclassement respecté' : 'Reclassement NON respecté';
      builder.addSource('QUESTION_IA', {
        expectedDisplay: label,
        reason: `Question complémentaire : "${q.questionText}" → "${q.answerText}"`,
      });
      break;
    }

    // IA — reclassementRespecteDetected.
    const iaDetect = this.aiDataSignal()?.reclassementRespecteDetected;
    if (iaDetect && iaDetect.reponse !== 'INCONNU' && iaDetect.reponse) {
      const iaRespecte = iaDetect.reponse === 'OUI';
      if (iaRespecte !== user) {
        builder.addSource('IA', {
          expectedDisplay: iaRespecte ? 'Reclassement respecté' : 'Reclassement NON respecté',
          reason: iaDetect.justification
            ? `Analyse du dossier : ${iaDetect.reponse} (${iaDetect.justification})`
            : `Analyse du dossier : obligation ${iaRespecte ? 'respectée' : 'NON respectée'}`,
        });
      }
    }

    // SF-155-06 : PIECE_MANQUANTE (courriers reclassement, preuves de propositions).
    const piece = this.findPieceManquante(['INAPT_RECLASSEMENT']);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  /**
   * SF-155-06 : recherche une `PieceManquanteEntry` dont `critereCode`
   * appartient (case-insensitive) à la liste acceptée. Retourne la 1re `texte`
   * trouvée, ou `null`. Méthode centrale du composant pour éviter la
   * duplication de logique d'indexation entre les 4 builders.
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
}
