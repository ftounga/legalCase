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
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar } from '@angular/material/snack-bar';
import { PrudhomeFicheService } from '../../core/services/prudhome-fiche.service';
import { PdfExportService } from '../../core/services/pdf-export.service';
import { PrudhomeFiche, PrudhomePiece } from '../../core/models/prudhome-fiche.model';
import {
  PieceManquanteEntry,
  TravailExtractedData,
} from '../../core/models/case-analysis.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { AiQuestion } from '../../core/models/ai-question.model';
import { CoherencePopoverTriggerDirective } from '../../shared/coherence-popover/coherence-popover-trigger.directive';
import { CoherenceAlert } from '../../shared/coherence-popover/coherence-alert.model';
import { CoherenceAlertBuilder } from '../../shared/coherence-popover/coherence-alert-builder';
import { SourceExplanation } from '../../core/models/source-explanation.model';
import { SourceExplanationService } from '../../core/services/source-explanation.service';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import {
  PrudhomeFicheSectionPrefillRules,
  computeProfession as computeProfessionRule,
} from './prudhome-fiche-section-prefill-rules';

/**
 * SF-173-01 : champs d'alerte de cohérence F-IA-03 exposés par F-DT-04
 * (fiche prud'homale FR). Volontairement minimal — la fiche FR ne contient pas
 * directement les champs salaire/dates/motif (uniquement profession en mappable
 * direct depuis `TravailExtractedData`). Si une SF future enrichit la fiche
 * avec dateRupture / salaire, étendre cet enum.
 */
export type PrudhomeAlertField = 'PROFESSION';

export type PrudhomeCoherenceAlert = CoherenceAlert<PrudhomeAlertField>;

@Component({
  selector: 'app-prudhome-fiche-section',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatTooltipModule,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './prudhome-fiche-section.component.html',
  styleUrl: './prudhome-fiche-section.component.scss'
})
export class PrudhomeFicheSectionComponent implements OnInit, OnChanges {
  // F-177 SF-177-03 : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'FICHE PRUD\'HOMALE';
  static readonly TOOL_ICON = 'gavel';

  /** F-177 SF-177-12 / F-236 SF-236-02 — délègue au helper partagé (parité runtime). */
  static getPrefillCount(input: {
    aiData?: any;
    procedureChecks?: any[];
    aiQuestions?: any[];
    piecesManquantes?: any[];
    triggerEvents?: any[];
    workspaceCountry?: string;
  }): number {
    return PrudhomeFicheSectionPrefillRules.computePrefillCount({ aiData: input.aiData });
  }

  @Input() caseFileId!: string;
  @Input() caseFileTitle: string = '';
  // F-177 SF-177-03 : force l'expansion au mount/change (utilisé quand le composant
  // est rendu dans le modal F-177).
  @Input() forceExpanded = false;
  // SF-173-01 : inputs IA pour pré-fill + validation F-IA-03 (pattern canonique
  // F-155 emprunté à `immigration-title-decision-section`).
  @Input() aiData?: TravailExtractedData | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;
  /**
   * F-194 SF-194-02 — Libellés des pièces taggées « OBTENUE » par l'avocat
   * et alignées sur cet outil (pré-filtrées par le panel via `piecesAlignment`
   * et `piecesObtenuesFor(toolId)`). Sert à signaler à l'avocat les pièces
   * déjà reçues (signal d'aide visuel — V1 : passif, V2 = pré-cochage des
   * checklist correspondantes).
   */
  @Input() piecesObtenues?: string[] | null;

  // SF-173-01 : snapshots signal pour que les `computed` réagissent.
  private aiDataSignal = signal<TravailExtractedData | null | undefined>(undefined);
  private procedureChecksSignal = signal<ProcedureCheck[]>([]);
  private aiQuestionsSignal = signal<AiQuestion[]>([]);
  private piecesManquantesSignal = signal<PieceManquanteEntry[]>([]);

  // SF-173-01 : signal du form value pour que `coherenceAlerts` se rafraîchisse
  // au changement utilisateur.
  private formValueSignal = signal<unknown>(null);

  collapsed = signal(true);
  saving = signal(false);
  pieces = signal<PrudhomePiece[]>([]);

  // SF-173-01 : un résultat persisté est arrivé (200 GET) → on n'écrase pas
  // par un re-prefill si aiData change ensuite.
  private hasPersistedFiche = signal(false);

  // SF-173-01 : provenance IA par champ pré-rempli. Effacé dès que l'avocat
  // modifie manuellement (handler `onXxxChange()` ou via `valueChanges`).
  provenanceProfession = signal<'IA' | null>(null);

  // SF-IA-03-15c : map {sourceKey → explanations} pour le popover enrichi.
  sourceExplanations = signal<Map<string, SourceExplanation[]>>(new Map());

  form: FormGroup;

  // SF-173-01 : alertes F-IA-03 dynamiques. Pour la fiche prud'homale FR, seul
  // `profession` est directement comparable avec `aiData.poste`. Les autres
  // champs de la fiche (nom, adresse, demandes, faits) ne sont pas extraits
  // par l'IA Travail à ce jour.
  coherenceAlerts = computed<Partial<Record<PrudhomeAlertField, PrudhomeCoherenceAlert>>>(() => {
    // Force re-eval au changement form value.
    this.formValueSignal();
    if (this.hasPersistedFiche()) return {};
    const ai = this.aiDataSignal();
    if (!ai) return {};
    const alerts: Partial<Record<PrudhomeAlertField, PrudhomeCoherenceAlert>> = {};

    const professionAlert = this.buildProfessionAlert();
    if (professionAlert) alerts.PROFESSION = professionAlert;

    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    return { total: values.length, blockers: 0 };
  });

  constructor(
    private fb: FormBuilder,
    private ficheService: PrudhomeFicheService,
    private pdfExportService: PdfExportService,
    private snackBar: MatSnackBar,
    @Optional() private sourceExplanationService: SourceExplanationService | null,
    @Optional() private dashboardRefresh: CaseDashboardRefreshService | null = null,
  ) {
    this.form = this.fb.group({
      demandeur: this.fb.group({
        nom: ['', Validators.required],
        prenom: [''],
        adresse: [''],
        telephone: [''],
        email: [''],
        profession: ['']
      }),
      defendeur: this.fb.group({
        nom: [''],
        adresse: [''],
        siret: [''],
        representant: ['']
      }),
      demandes: this.fb.array([]),
      faitsTexte: [''],
      moyensDroitTexte: ['']
    });

    // SF-173-01 : remet provenance à null quand l'avocat édite manuellement
    // un champ pré-rempli (le badge IA disparaît).
    this.form.get('demandeur.profession')?.valueChanges.subscribe(() => {
      // Ignore le set initial via prefill (provenance vient d'être posée à 'IA').
      // Le pattern canonique F-155 utilise des handlers (ngModelChange) ; ici on
      // utilise valueChanges car le formulaire est ReactiveForms — le résultat
      // est équivalent : la provenance IA est invalidée à toute modif suivante.
      // Garde : seule la 1re modif après prefill compte ; les suivantes sont no-op.
      if (this.provenanceProfession() === 'IA') {
        // Détecte si la nouvelle valeur est différente de la valeur IA actuelle.
        const aiValue = this.aiDataSignal()?.poste;
        const current = this.form.get('demandeur.profession')?.value;
        if (current !== aiValue) {
          this.provenanceProfession.set(null);
        }
      }
    });

    this.form.valueChanges.subscribe(v => this.formValueSignal.set(v));
  }

  ngOnInit(): void {
    // F-177 SF-177-03 : appliqué dès le mount pour le mode modal.
    if (this.forceExpanded) this.collapsed.set(false);

    // SF-173-01 : push inputs vers signals avant toute évaluation computed.
    this.aiDataSignal.set(this.aiData);
    this.procedureChecksSignal.set(this.procedureChecks ?? []);
    this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    this.piecesManquantesSignal.set(this.piecesManquantes ?? []);

    // SF-173-01 : pré-fill dès le mount si aiData déjà disponible (pattern
    // canonique F-155). Si la fiche persistée arrive, elle écrase via patchForm.
    this.prefillFromAi();

    this.ficheService.get(this.caseFileId).subscribe({
      next: fiche => {
        this.hasPersistedFiche.set(true);
        this.patchForm(fiche);
      },
      error: () => { /* fail-open : formulaire conserve le pré-fill IA / vide */ }
    });

    this.loadSourceExplanations();
  }

  ngOnChanges(changes: SimpleChanges): void {
    // F-177 SF-177-03 : applique le forceExpanded quand il passe à true en cours de vie.
    if (changes['forceExpanded'] && this.forceExpanded) this.collapsed.set(false);

    // SF-173-01 : ré-hydrater les signals à chaque changement d'input.
    if (changes['aiData']) this.aiDataSignal.set(this.aiData);
    if (changes['procedureChecks']) this.procedureChecksSignal.set(this.procedureChecks ?? []);
    if (changes['aiQuestions']) this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    if (changes['piecesManquantes']) this.piecesManquantesSignal.set(this.piecesManquantes ?? []);

    // SF-173-01 : ré-applique le prefill quand aiData arrive après le mount,
    // tant qu'aucune fiche persistée n'est arrivée. Ne PAS écraser les saisies
    // manuelles (la garde "champ vide" dans `prefillFromAi` s'en charge).
    if (changes['aiData'] && !changes['aiData'].firstChange && !this.hasPersistedFiche()) {
      this.prefillFromAi();
    }
  }

  /**
   * SF-173-01 : pré-remplit le form depuis `aiData`. N'écrase pas une saisie
   * manuelle (la condition `!current` garantit que les valeurs déjà saisies
   * par l'avocat sont préservées). Pour la fiche prud'homale FR, seul
   * `aiData.poste → demandeur.profession` est mappable directement.
   *
   * Champs non mappés (par construction de TravailExtractedData) :
   * `demandeur.nom/prenom/adresse/telephone/email`, `defendeur.nom/adresse/siret/representant`,
   * `demandes`, `faitsTexte`, `moyensDroitTexte`. Ces champs restent vides — l'avocat
   * les complète manuellement.
   */
  private prefillFromAi(): void {
    const ai = this.aiDataSignal();
    if (!ai) return;

    // F-236 SF-236-02 : valeur calculée par le helper partagé (parité static).
    const profession = computeProfessionRule({ aiData: ai });
    if (profession) {
      const ctrl = this.form.get('demandeur.profession');
      if (ctrl && !ctrl.value) {
        ctrl.setValue(profession, { emitEvent: false });
        this.provenanceProfession.set('IA');
      }
    }
  }

  /** SF-173-01 : effacer le badge IA quand l'avocat modifie manuellement. */
  onProfessionChange(): void {
    this.provenanceProfession.set(null);
  }

  private loadSourceExplanations(): void {
    if (!this.caseFileId) return;
    if (!this.sourceExplanationService) return;
    this.sourceExplanationService.getForCaseFile(this.caseFileId).subscribe({
      next: map => this.sourceExplanations.set(map),
      error: () => { /* fail-open */ },
    });
  }

  /** SF-IA-03-15c : mapping vers sourceKey. */
  explanationFor(field: PrudhomeAlertField): SourceExplanation[] {
    const key = field === 'PROFESSION' ? 'poste' : '';
    return this.sourceExplanations().get(key) ?? [];
  }

  alertTooltip(alert: PrudhomeCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: PrudhomeCoherenceAlert): string {
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

  private buildProfessionAlert(): PrudhomeCoherenceAlert | null {
    const userValue = this.form.get('demandeur.profession')?.value as string | null;
    if (!userValue || !userValue.trim()) return null;

    const ai = this.aiDataSignal();
    if (!ai?.poste) return null;

    const userNorm = userValue.trim().toLowerCase();
    const aiNorm = ai.poste.trim().toLowerCase();
    if (userNorm === aiNorm) return null;

    return CoherenceAlertBuilder.forField<PrudhomeAlertField>('PROFESSION')
      .addSource('IA', {
        expectedDisplay: ai.poste,
        reason: `Analyse du dossier : poste ${ai.poste}`,
      })
      .build();
  }

  get demandesArray(): FormArray {
    return this.form.get('demandes') as FormArray;
  }

  toggleCollapsed(): void {
    this.collapsed.update(v => !v);
  }

  addDemande(): void {
    this.demandesArray.push(
      this.fb.group({
        label: ['', Validators.required],
        montant: [null, [Validators.min(0)]]
      })
    );
  }

  removeDemande(index: number): void {
    this.demandesArray.removeAt(index);
  }

  save(): void {
    if (this.form.invalid) return;
    this.saving.set(true);
    const value = this.form.getRawValue();
    this.ficheService.save(this.caseFileId, value).subscribe({
      next: fiche => {
        this.saving.set(false);
        this.pieces.set(fiche.piecesList);
        this.hasPersistedFiche.set(true);
        // SF-173-01 : valeurs persistées par l'avocat — efface les badges IA.
        this.provenanceProfession.set(null);
        this.snackBar.open('Fiche enregistrée', 'Fermer', {
          duration: 3000, panelClass: ['snack-success']
        });
        // SF-177-14 — notifie dashboard + panel pour reload (pattern F-IA-02-03).
        this.dashboardRefresh?.triggerRefresh();
      },
      error: () => {
        this.saving.set(false);
        this.snackBar.open('Erreur lors de la sauvegarde', 'Fermer', {
          duration: 4000, panelClass: ['snack-error']
        });
      }
    });
  }

  exportPdf(): void {
    const value = this.form.getRawValue();
    const fiche: PrudhomeFiche = {
      id: null,
      demandeur: value.demandeur,
      defendeur: value.defendeur,
      demandes: value.demandes,
      faitsTexte: value.faitsTexte || null,
      moyensDroitTexte: value.moyensDroitTexte || null,
      piecesList: this.pieces(),
      updatedAt: null,
    };
    try {
      this.pdfExportService.exportPrudhomeFiche(fiche, this.caseFileTitle);
    } catch {
      this.snackBar.open('Erreur lors de la génération du PDF', 'Fermer', {
        duration: 4000, panelClass: ['snack-error']
      });
    }
  }

  private patchForm(fiche: PrudhomeFiche): void {
    this.form.patchValue({
      demandeur: fiche.demandeur,
      defendeur: fiche.defendeur,
      faitsTexte: fiche.faitsTexte ?? '',
      moyensDroitTexte: fiche.moyensDroitTexte ?? ''
    });
    this.demandesArray.clear();
    for (const d of fiche.demandes ?? []) {
      this.demandesArray.push(
        this.fb.group({
          label: [d.label, Validators.required],
          montant: [d.montant, [Validators.min(0)]]
        })
      );
    }
    this.pieces.set(fiche.piecesList ?? []);
    // SF-173-01 : valeurs persistées validées par l'avocat — efface les badges IA.
    this.provenanceProfession.set(null);
  }
}
