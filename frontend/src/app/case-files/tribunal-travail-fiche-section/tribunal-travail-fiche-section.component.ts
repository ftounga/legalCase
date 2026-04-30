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
import { MatSelectModule } from '@angular/material/select';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar } from '@angular/material/snack-bar';
import { TribunalTravailFicheService } from '../../core/services/tribunal-travail-fiche.service';
import { PdfExportService } from '../../core/services/pdf-export.service';
import { TribunalTravailFiche, TribunalPiece } from '../../core/models/tribunal-travail-fiche.model';
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

/**
 * SF-173-02 : champs d'alerte de cohérence F-IA-03 exposés par F-DT-06
 * (requête tribunal du travail BE). Critiques : dateDebut (vs aiData.dateEntree),
 * motifRupture (vs aiData.motifLicenciement). `commissionParitaire`
 * (équivalent BE de la convention collective FR) traité en alerte secondaire.
 */
export type TribunalAlertField = 'DATE_DEBUT' | 'MOTIF_RUPTURE' | 'COMMISSION_PARITAIRE' | 'TYPE_CONTRAT';

export type TribunalCoherenceAlert = CoherenceAlert<TribunalAlertField>;

@Component({
  selector: 'app-tribunal-travail-fiche-section',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatTooltipModule,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './tribunal-travail-fiche-section.component.html',
  styleUrl: './tribunal-travail-fiche-section.component.scss'
})
export class TribunalTravailFicheSectionComponent implements OnInit, OnChanges {
  @Input() caseFileId!: string;
  @Input() caseFileTitle: string = '';
  // SF-173-02 : inputs IA pour pré-fill + validation F-IA-03 (pattern canonique F-155).
  @Input() aiData?: TravailExtractedData | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;

  // SF-173-02 : snapshots signal pour que `computed` réagissent.
  private aiDataSignal = signal<TravailExtractedData | null | undefined>(undefined);
  private procedureChecksSignal = signal<ProcedureCheck[]>([]);
  private aiQuestionsSignal = signal<AiQuestion[]>([]);
  private piecesManquantesSignal = signal<PieceManquanteEntry[]>([]);
  private formValueSignal = signal<unknown>(null);

  collapsed = signal(true);
  saving = signal(false);
  pieces = signal<TribunalPiece[]>([]);

  private hasPersistedFiche = signal(false);

  // SF-173-02 : provenance IA par champ pré-rempli.
  provenanceCommissionParitaire = signal<'IA' | null>(null);
  provenanceTypeContrat = signal<'IA' | null>(null);
  provenanceDateDebut = signal<'IA' | null>(null);
  provenanceDateFin = signal<'IA' | null>(null);
  provenanceMotifRupture = signal<'IA' | null>(null);

  sourceExplanations = signal<Map<string, SourceExplanation[]>>(new Map());

  form: FormGroup;

  // SF-173-02 : alertes F-IA-03 (uniquement avant persistance, pour cohérence
  // avec le pattern canonique F-155). Les fields critiques sont dateDebut /
  // motifRupture / commissionParitaire / typeContrat.
  coherenceAlerts = computed<Partial<Record<TribunalAlertField, TribunalCoherenceAlert>>>(() => {
    this.formValueSignal();
    if (this.hasPersistedFiche()) return {};
    const ai = this.aiDataSignal();
    if (!ai) return {};
    const alerts: Partial<Record<TribunalAlertField, TribunalCoherenceAlert>> = {};

    const cpAlert = this.buildCommissionParitaireAlert();
    if (cpAlert) alerts.COMMISSION_PARITAIRE = cpAlert;

    const tcAlert = this.buildTypeContratAlert();
    if (tcAlert) alerts.TYPE_CONTRAT = tcAlert;

    const ddAlert = this.buildDateDebutAlert();
    if (ddAlert) alerts.DATE_DEBUT = ddAlert;

    const mrAlert = this.buildMotifRuptureAlert();
    if (mrAlert) alerts.MOTIF_RUPTURE = mrAlert;

    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    return { total: values.length, blockers: 0 };
  });

  constructor(
    private fb: FormBuilder,
    private ficheService: TribunalTravailFicheService,
    private pdfExportService: PdfExportService,
    private snackBar: MatSnackBar,
    @Optional() private sourceExplanationService: SourceExplanationService | null,
  ) {
    this.form = this.fb.group({
      requerant: this.fb.group({
        nom: ['', Validators.required],
        prenom: [''],
        domicile: [''],
        registreNational: ['']
      }),
      defendeur: this.fb.group({
        nom: [''],
        siegeSocial: [''],
        numeroBce: [''],
        representant: ['']
      }),
      procedureInfo: this.fb.group({
        tribunal: [''],
        division: [''],
        langue: ['FR'],
        commissionParitaire: ['']
      }),
      contratInfo: this.fb.group({
        typeContrat: [''],
        dateDebut: [''],
        dateFin: [''],
        motifRupture: ['']
      }),
      demandes: this.fb.array([]),
      exposeDesMoyens: ['']
    });

    // SF-173-02 : invalider provenance IA dès que l'avocat modifie un champ
    // pré-rempli (le badge disparaît).
    this.form.get('procedureInfo.commissionParitaire')?.valueChanges.subscribe(() => {
      if (this.provenanceCommissionParitaire() === 'IA' &&
          this.form.get('procedureInfo.commissionParitaire')?.value !== this.aiDataSignal()?.conventionCollective) {
        this.provenanceCommissionParitaire.set(null);
      }
    });
    this.form.get('contratInfo.typeContrat')?.valueChanges.subscribe(() => {
      if (this.provenanceTypeContrat() === 'IA' &&
          this.form.get('contratInfo.typeContrat')?.value !== this.normalizeTypeContrat(this.aiDataSignal()?.typeContrat ?? null)) {
        this.provenanceTypeContrat.set(null);
      }
    });
    this.form.get('contratInfo.dateDebut')?.valueChanges.subscribe(() => {
      if (this.provenanceDateDebut() === 'IA' &&
          this.form.get('contratInfo.dateDebut')?.value !== this.aiDataSignal()?.dateEntree) {
        this.provenanceDateDebut.set(null);
      }
    });
    this.form.get('contratInfo.dateFin')?.valueChanges.subscribe(() => {
      if (this.provenanceDateFin() === 'IA' &&
          this.form.get('contratInfo.dateFin')?.value !== this.aiDataSignal()?.dateLicenciement) {
        this.provenanceDateFin.set(null);
      }
    });
    this.form.get('contratInfo.motifRupture')?.valueChanges.subscribe(() => {
      if (this.provenanceMotifRupture() === 'IA' &&
          this.form.get('contratInfo.motifRupture')?.value !== this.aiDataSignal()?.motifLicenciement) {
        this.provenanceMotifRupture.set(null);
      }
    });

    this.form.valueChanges.subscribe(v => this.formValueSignal.set(v));
  }

  ngOnInit(): void {
    this.aiDataSignal.set(this.aiData);
    this.procedureChecksSignal.set(this.procedureChecks ?? []);
    this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    this.piecesManquantesSignal.set(this.piecesManquantes ?? []);

    // SF-173-02 : pré-fill au mount avant tout GET (pattern canonique F-155).
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
    if (changes['aiData']) this.aiDataSignal.set(this.aiData);
    if (changes['procedureChecks']) this.procedureChecksSignal.set(this.procedureChecks ?? []);
    if (changes['aiQuestions']) this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    if (changes['piecesManquantes']) this.piecesManquantesSignal.set(this.piecesManquantes ?? []);

    if (changes['aiData'] && !changes['aiData'].firstChange && !this.hasPersistedFiche()) {
      this.prefillFromAi();
    }
  }

  /**
   * SF-173-02 : pré-remplit le form depuis `aiData`. N'écrase pas une saisie
   * manuelle (la condition `!current` garantit la préservation des saisies).
   *
   * Mapping `TravailExtractedData` → FormGroup (BE) :
   * - `aiData.conventionCollective` → `procedureInfo.commissionParitaire` (équivalent BE)
   * - `aiData.typeContrat` → `contratInfo.typeContrat` (normalisé EMPLOYE/OUVRIER)
   * - `aiData.dateEntree` → `contratInfo.dateDebut`
   * - `aiData.dateLicenciement` → `contratInfo.dateFin`
   * - `aiData.motifLicenciement` → `contratInfo.motifRupture`
   *
   * Champs non mappés : `requerant.*`, `defendeur.*`, `procedureInfo.tribunal/division/langue`,
   * `demandes`, `exposeDesMoyens` — l'avocat les complète manuellement.
   */
  private prefillFromAi(): void {
    const ai = this.aiDataSignal();
    if (!ai) return;

    if (ai.conventionCollective) {
      const ctrl = this.form.get('procedureInfo.commissionParitaire');
      if (ctrl && !ctrl.value) {
        ctrl.setValue(ai.conventionCollective, { emitEvent: false });
        this.provenanceCommissionParitaire.set('IA');
      }
    }

    const typeNorm = this.normalizeTypeContrat(ai.typeContrat ?? null);
    if (typeNorm) {
      const ctrl = this.form.get('contratInfo.typeContrat');
      if (ctrl && !ctrl.value) {
        ctrl.setValue(typeNorm, { emitEvent: false });
        this.provenanceTypeContrat.set('IA');
      }
    }

    if (ai.dateEntree) {
      const ctrl = this.form.get('contratInfo.dateDebut');
      if (ctrl && !ctrl.value) {
        ctrl.setValue(ai.dateEntree, { emitEvent: false });
        this.provenanceDateDebut.set('IA');
      }
    }

    if (ai.dateLicenciement) {
      const ctrl = this.form.get('contratInfo.dateFin');
      if (ctrl && !ctrl.value) {
        ctrl.setValue(ai.dateLicenciement, { emitEvent: false });
        this.provenanceDateFin.set('IA');
      }
    }

    if (ai.motifLicenciement) {
      const ctrl = this.form.get('contratInfo.motifRupture');
      if (ctrl && !ctrl.value) {
        ctrl.setValue(ai.motifLicenciement, { emitEvent: false });
        this.provenanceMotifRupture.set('IA');
      }
    }
  }

  /** SF-173-02 : normalise un libellé typeContrat IA vers l'enum BE EMPLOYE/OUVRIER (sinon null). */
  private normalizeTypeContrat(raw: string | null): 'EMPLOYE' | 'OUVRIER' | null {
    if (!raw) return null;
    const u = raw.toUpperCase().normalize('NFD').replace(/[̀-ͯ]/g, '');
    if (u.includes('EMPLOYE') || u === 'CDI' || u === 'CDD') return 'EMPLOYE';
    if (u.includes('OUVRIER')) return 'OUVRIER';
    return null;
  }

  // SF-173-02 : handlers manuels (utilisés via (change) dans le template).
  onCommissionParitaireChange(): void { this.provenanceCommissionParitaire.set(null); }
  onTypeContratChange(): void { this.provenanceTypeContrat.set(null); }
  onDateDebutChange(): void { this.provenanceDateDebut.set(null); }
  onDateFinChange(): void { this.provenanceDateFin.set(null); }
  onMotifRuptureChange(): void { this.provenanceMotifRupture.set(null); }

  private loadSourceExplanations(): void {
    if (!this.caseFileId) return;
    if (!this.sourceExplanationService) return;
    this.sourceExplanationService.getForCaseFile(this.caseFileId).subscribe({
      next: map => this.sourceExplanations.set(map),
      error: () => { /* fail-open */ },
    });
  }

  explanationFor(field: TribunalAlertField): SourceExplanation[] {
    const key = (() => {
      switch (field) {
        case 'COMMISSION_PARITAIRE': return 'convention_collective';
        case 'TYPE_CONTRAT': return 'type_contrat';
        case 'DATE_DEBUT': return 'date_entree';
        case 'MOTIF_RUPTURE': return 'motif_licenciement';
      }
    })();
    return this.sourceExplanations().get(key) ?? [];
  }

  alertTooltip(alert: TribunalCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: TribunalCoherenceAlert): string {
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

  private buildCommissionParitaireAlert(): TribunalCoherenceAlert | null {
    const userValue = this.form.get('procedureInfo.commissionParitaire')?.value as string | null;
    if (!userValue || !userValue.trim()) return null;
    const ai = this.aiDataSignal();
    if (!ai?.conventionCollective) return null;
    if (userValue.trim().toLowerCase() === ai.conventionCollective.trim().toLowerCase()) return null;
    return CoherenceAlertBuilder.forField<TribunalAlertField>('COMMISSION_PARITAIRE')
      .addSource('IA', {
        expectedDisplay: ai.conventionCollective,
        reason: `Analyse du dossier : commission paritaire ${ai.conventionCollective}`,
      })
      .build();
  }

  private buildTypeContratAlert(): TribunalCoherenceAlert | null {
    const userValue = this.form.get('contratInfo.typeContrat')?.value as string | null;
    if (!userValue) return null;
    const ai = this.aiDataSignal();
    const aiNorm = this.normalizeTypeContrat(ai?.typeContrat ?? null);
    if (!aiNorm) return null;
    if (userValue === aiNorm) return null;
    return CoherenceAlertBuilder.forField<TribunalAlertField>('TYPE_CONTRAT')
      .addSource('IA', {
        expectedDisplay: aiNorm,
        reason: `Analyse du dossier : type contrat ${aiNorm}`,
      })
      .build();
  }

  private buildDateDebutAlert(): TribunalCoherenceAlert | null {
    const userValue = this.form.get('contratInfo.dateDebut')?.value as string | null;
    if (!userValue) return null;
    const ai = this.aiDataSignal();
    if (!ai?.dateEntree) return null;
    if (userValue === ai.dateEntree) return null;
    return CoherenceAlertBuilder.forField<TribunalAlertField>('DATE_DEBUT')
      .addSource('IA', {
        expectedDisplay: ai.dateEntree,
        reason: `Analyse du dossier : date d'entrée ${ai.dateEntree}`,
      })
      .build();
  }

  private buildMotifRuptureAlert(): TribunalCoherenceAlert | null {
    const userValue = this.form.get('contratInfo.motifRupture')?.value as string | null;
    if (!userValue || !userValue.trim()) return null;
    const ai = this.aiDataSignal();
    if (!ai?.motifLicenciement) return null;
    if (userValue.trim().toLowerCase() === ai.motifLicenciement.trim().toLowerCase()) return null;
    return CoherenceAlertBuilder.forField<TribunalAlertField>('MOTIF_RUPTURE')
      .addSource('IA', {
        expectedDisplay: ai.motifLicenciement,
        reason: `Analyse du dossier : motif rupture ${ai.motifLicenciement}`,
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
        // SF-173-02 : valeurs persistées validées par l'avocat — efface les badges IA.
        this.provenanceCommissionParitaire.set(null);
        this.provenanceTypeContrat.set(null);
        this.provenanceDateDebut.set(null);
        this.provenanceDateFin.set(null);
        this.provenanceMotifRupture.set(null);
        this.snackBar.open('Requête enregistrée', 'Fermer', {
          duration: 3000, panelClass: ['snack-success']
        });
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
    const fiche: TribunalTravailFiche = {
      id: null,
      requerant: value.requerant,
      defendeur: value.defendeur,
      procedureInfo: value.procedureInfo,
      contratInfo: value.contratInfo,
      demandes: value.demandes,
      exposeDesMoyens: value.exposeDesMoyens || null,
      piecesList: this.pieces(),
      updatedAt: null,
    };
    try {
      this.pdfExportService.exportTribunalTravailFiche(fiche, this.caseFileTitle);
    } catch {
      this.snackBar.open('Erreur lors de la génération du PDF', 'Fermer', {
        duration: 4000, panelClass: ['snack-error']
      });
    }
  }

  private patchForm(fiche: TribunalTravailFiche): void {
    this.form.patchValue({
      requerant: fiche.requerant,
      defendeur: fiche.defendeur,
      procedureInfo: fiche.procedureInfo,
      contratInfo: fiche.contratInfo,
      exposeDesMoyens: fiche.exposeDesMoyens ?? ''
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
    // SF-173-02 : valeurs persistées validées par l'avocat — efface les badges IA.
    this.provenanceCommissionParitaire.set(null);
    this.provenanceTypeContrat.set(null);
    this.provenanceDateDebut.set(null);
    this.provenanceDateFin.set(null);
    this.provenanceMotifRupture.set(null);
  }
}
