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
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { HarcelementNulliteService } from '../../core/services/harcelement-nullite.service';
import {
  HarcelementNulliteResponse,
  MotifNullite,
  MotifNulliteFr,
  MotifOption,
  MOTIFS_BE,
  MOTIFS_FR,
} from '../../core/models/harcelement-nullite.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import {
  PieceManquanteEntry,
  TravailExtractedData,
} from '../../core/models/case-analysis.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { AiQuestion } from '../../core/models/ai-question.model';
import { CoherencePopoverTriggerDirective } from '../../shared/coherence-popover/coherence-popover-trigger.directive';
import { CoherenceAlert, CoherenceAlertSource } from '../../shared/coherence-popover/coherence-alert.model';
import { CoherenceAlertBuilder } from '../../shared/coherence-popover/coherence-alert-builder';
import { SourceExplanation } from '../../core/models/source-explanation.model';
import { SourceExplanationService } from '../../core/services/source-explanation.service';

/**
 * SF-155-04-A1 : champs d'alerte de cohérence F-IA-03 exposés par l'outil
 * F-DT-11 (harcèlement / licenciement nul). Un par field pré-remplissable.
 */
export type HLNAlertField = 'SALAIRE' | 'MOTIF_NULLITE';

// SF-155-05 : alias locaux rétro-compat — utilise l'interface générique partagée.
export type HLNAlertSource = CoherenceAlertSource;
export type HLNCoherenceAlert = CoherenceAlert<HLNAlertField>;

/**
 * SF-155-04-A1 : mapping IA (`motifNullitePressenti`) → UI (`MotifNulliteFr`).
 * Les valeurs IA sans équivalent direct (RETORSION, SYNDICAL, ACCIDENT_MP) ne
 * sont PAS mappées — pas de pré-fill pour ces cas (fail-open). L'avocat
 * saisira manuellement en se basant sur l'analyse affichée par ailleurs.
 */
const AI_MOTIF_TO_MOTIF_NULLITE_FR: Readonly<Record<string, MotifNulliteFr>> = {
  DISCRIMINATION: 'DISCRIMINATION',
  HARCELEMENT_MORAL: 'HARCELEMENT_MORAL',
  HARCELEMENT_SEXUEL: 'HARCELEMENT_SEXUEL',
  MATERNITE_PATERNITE: 'GROSSESSE',
};

/**
 * SF-155-04-A1 : seuil d'écart relatif au-delà duquel on affiche une alerte
 * de cohérence entre le salaire extrait par l'IA et la saisie de l'avocat.
 * Aligné avec le seuil utilisé par F-DT-09 comparateur indemnités.
 */
const SALAIRE_DIVERGENCE_RATIO = 0.10;

/**
 * SF-DT-11-02 : outil décisionnel dédié "Indemnité minimum licenciement nul
 * — harcèlement" (F-DT-11). FR + BE. Consomme l'API SF-DT-11-01.
 * Affiché conditionnellement par le panel F-IA-04 (tool_id
 * 'F-DT-11-harcelement-licenciement-nul').
 *
 * SF-155-04-A1 : template canonique IA-compliant (pré-fill IA + validation
 * F-IA-03). Pattern emprunté à `immigration-title-decision-section`
 * (F-IM-05) — cf. mini-spec SF-155-04-A1.
 */
@Component({
  selector: 'app-harcelement-licenciement-nul-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule, DecimalPipe,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatProgressSpinnerModule,
    LegalCitationsPipe,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './harcelement-licenciement-nul-section.component.html',
  styleUrl: './harcelement-licenciement-nul-section.component.scss',
})
export class HarcelementLicenciementNulSectionComponent implements OnInit, OnChanges {
  @Input() caseFileId!: string;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';
  // SF-155-04-A1 : inputs IA (tous optionnels — null-safe partout).
  @Input() aiData?: TravailExtractedData | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;

  // SF-155-04-A1 : snapshots signal des inputs IA pour que `computed` réagisse.
  // Même pattern qu'`immigration-title-decision-section`.
  private aiDataSignal = signal<TravailExtractedData | null | undefined>(undefined);
  private procedureChecksSignal = signal<ProcedureCheck[]>([]);
  private aiQuestionsSignal = signal<AiQuestion[]>([]);
  private piecesManquantesSignal = signal<PieceManquanteEntry[]>([]);

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<HarcelementNulliteResponse | null>(null);

  salaireMensuelReference = signal<number | null>(null);
  motifNullite = signal<MotifNullite | null>(null);

  // SF-155-04-A1 : provenance IA par champ. 'IA' quand le champ a été
  // pré-rempli par `prefillFromAi()` ; remis à null dès que l'avocat
  // modifie manuellement (onXxxChange).
  provenanceSalaire = signal<'IA' | null>(null);
  provenanceMotifNullite = signal<'IA' | null>(null);

  // SF-IA-03-15c : map {sourceKey → explanations} pour le popover.
  sourceExplanations = signal<Map<string, SourceExplanation[]>>(new Map());

  motifsDisponibles = computed<MotifOption[]>(() =>
    this.workspaceCountry === 'BELGIQUE' ? MOTIFS_BE : MOTIFS_FR
  );

  // SF-155-04-A1 : alertes de cohérence calculées dynamiquement.
  // Gate : uniquement en mode formulaire (pattern anti-bug SF-IA-03-12 —
  // ne pas afficher d'alertes après que le calcul est rendu).
  coherenceAlerts = computed<Partial<Record<HLNAlertField, HLNCoherenceAlert>>>(() => {
    if (!this.showForm()) return {};
    const alerts: Partial<Record<HLNAlertField, HLNCoherenceAlert>> = {};
    const salaireAlert = this.buildSalaireAlert();
    if (salaireAlert) alerts.SALAIRE = salaireAlert;
    const motifAlert = this.buildMotifNulliteAlert();
    if (motifAlert) alerts.MOTIF_NULLITE = motifAlert;
    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    return { total: values.length, blockers: 0 };
  });

  constructor(
    private service: HarcelementNulliteService,
    private snackBar: MatSnackBar,
    @Optional() private dashboardRefresh: CaseDashboardRefreshService | null,
    @Optional() private sourceExplanationService: SourceExplanationService | null,
  ) {}

  ngOnInit(): void {
    // SF-155-04-A1 : push inputs vers signals avant toute évaluation computed.
    this.aiDataSignal.set(this.aiData);
    this.procedureChecksSignal.set(this.procedureChecks ?? []);
    this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    this.piecesManquantesSignal.set(this.piecesManquantes ?? []);
    this.load();
    this.loadSourceExplanations();
  }

  ngOnChanges(changes: SimpleChanges): void {
    // SF-155-04-A1 : ré-hydrater les signals à chaque changement d'input.
    if (changes['aiData']) this.aiDataSignal.set(this.aiData);
    if (changes['procedureChecks']) this.procedureChecksSignal.set(this.procedureChecks ?? []);
    if (changes['aiQuestions']) this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    if (changes['piecesManquantes']) this.piecesManquantesSignal.set(this.piecesManquantes ?? []);

    // SF-155-04-A1 : ré-appliquer le pré-fill quand `aiData` change avant
    // première résolution backend. Ne PAS écraser si l'avocat a déjà saisi
    // manuellement (provenance devenue null) OU si un résultat persisté est
    // présent (form masqué).
    if (changes['aiData'] && !changes['aiData'].firstChange && this.showForm() && !this.result()) {
      this.prefillFromAi();
    }
  }

  /**
   * SF-155-04-A1 : pré-remplit le form depuis `aiData`. N'écrase jamais une
   * saisie avocat existante (la garde est dans le call site).
   */
  private prefillFromAi(): void {
    const ai = this.aiDataSignal();
    if (!ai) return;

    // 1. Salaire brut mensuel → salaireMensuelReference (si > 0).
    if (typeof ai.salaireBrutMensuel === 'number' && ai.salaireBrutMensuel > 0) {
      // Ne pré-rempli QUE si le champ est encore vide (préserve les edits avocats
      // arrivant après première résolution 404 + modif).
      if (this.salaireMensuelReference() === null || this.provenanceSalaire() === 'IA') {
        this.salaireMensuelReference.set(ai.salaireBrutMensuel);
        this.provenanceSalaire.set('IA');
      }
    }

    // 2. Motif de nullité : mapping IA → UI + gate pays (FR uniquement —
    //    côté backend, `motifNullitePressenti` n'est rempli que sur dossiers FR).
    if (this.workspaceCountry !== 'FRANCE') return;
    const iaMotif = ai.motifNullitePressenti?.toUpperCase();
    if (!iaMotif) return;
    const mapped = AI_MOTIF_TO_MOTIF_NULLITE_FR[iaMotif];
    if (!mapped) return; // Valeur IA non mappable → pas de pré-fill (graceful).
    if (this.motifNullite() === null || this.provenanceMotifNullite() === 'IA') {
      this.motifNullite.set(mapped);
      this.provenanceMotifNullite.set('IA');
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
   * SF-IA-03-15c : mapping field → sourceKey pour le popover.
   */
  explanationFor(field: HLNAlertField): SourceExplanation[] {
    const key = field === 'SALAIRE' ? 'salaire_brut_mensuel' : 'HLN_MOTIF_NULLITE';
    return this.sourceExplanations().get(key) ?? [];
  }

  alertTooltip(alert: HLNCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: HLNCoherenceAlert): string {
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
   * SF-155-04-A1 : divergence salaire — écart relatif > 10 %
   * (SALAIRE_DIVERGENCE_RATIO) entre valeur IA et saisie avocat.
   * SF-155-05 : via CoherenceAlertBuilder partagé.
   */
  private buildSalaireAlert(): HLNCoherenceAlert | null {
    const aiSalaire = this.aiDataSignal()?.salaireBrutMensuel;
    const userSalaire = this.salaireMensuelReference();
    if (typeof aiSalaire !== 'number' || aiSalaire <= 0) return null;
    if (typeof userSalaire !== 'number' || userSalaire <= 0) return null;
    const ratio = Math.abs(userSalaire - aiSalaire) / aiSalaire;
    if (ratio <= SALAIRE_DIVERGENCE_RATIO) return null;
    return CoherenceAlertBuilder.forField<HLNAlertField>('SALAIRE')
      .addSource('IA', {
        expectedDisplay: `${aiSalaire.toLocaleString('fr-FR')} €`,
        reason: `Analyse du dossier : salaire brut mensuel ~${aiSalaire.toLocaleString('fr-FR')} €`,
      })
      .build();
  }

  /**
   * SF-155-04-A1 : divergence motif — mapping IA vs saisie avocat.
   * Gate FR uniquement (cf. prefillFromAi — même invariant côté backend).
   * SF-155-05 : via CoherenceAlertBuilder partagé.
   */
  private buildMotifNulliteAlert(): HLNCoherenceAlert | null {
    if (this.workspaceCountry !== 'FRANCE') return null;
    const iaMotif = this.aiDataSignal()?.motifNullitePressenti?.toUpperCase();
    if (!iaMotif) return null;
    const mapped = AI_MOTIF_TO_MOTIF_NULLITE_FR[iaMotif];
    if (!mapped) return null;
    const userMotif = this.motifNullite();
    if (!userMotif || userMotif === mapped) return null;
    const mappedLabel = MOTIFS_FR.find((m) => m.code === mapped)?.label ?? mapped;
    return CoherenceAlertBuilder.forField<HLNAlertField>('MOTIF_NULLITE')
      .addSource('IA', {
        expectedDisplay: mappedLabel,
        reason: `Analyse du dossier : motif de nullité pressenti "${mappedLabel}"`,
      })
      .build();
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  formValid(): boolean {
    const s = this.salaireMensuelReference();
    const m = this.motifNullite();
    return s !== null && s > 0 && m !== null;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  /**
   * SF-155-04-A1 : handler change salaire — efface le badge IA dès que
   * l'avocat modifie manuellement (pattern canonique).
   */
  onSalaireChange(value: number | null): void {
    this.salaireMensuelReference.set(value);
    this.provenanceSalaire.set(null);
  }

  /**
   * SF-155-04-A1 : handler change motif — efface le badge IA.
   */
  onMotifNulliteChange(value: MotifNullite | null): void {
    this.motifNullite.set(value);
    this.provenanceMotifNullite.set(null);
  }

  /**
   * SF-155-04-A1 : true si l'IA a déduit le salaire brut d'un montant net
   * (via × 1,30 — cf. SF-130-01). On affiche alors une note discrète sous
   * le champ pour contextualiser la valeur pré-remplie.
   */
  salaireEstDeduit(): boolean {
    return this.aiDataSignal()?.salaireEstDeduit === true;
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request = {
      salaireMensuelReference: this.salaireMensuelReference()!,
      motifNullite: this.motifNullite()!,
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
        this.motifNullite.set(r.motifNullite);
        // SF-155-04-A1 : valeurs persistées = saisie avocat — jamais de badge IA.
        this.provenanceSalaire.set(null);
        this.provenanceMotifNullite.set(null);
        this.showForm.set(false);
        this.loading.set(false);
      },
      error: () => {
        // 404 attendu si aucune analyse — on reste en mode formulaire.
        // SF-155-04-A1 : fallback pré-fill IA uniquement ici (pas si GET 200).
        this.prefillFromAi();
        this.loading.set(false);
      },
    });
  }
}
