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
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import {
  TribunalTravailFicheSectionPrefillRules,
  computeCommissionParitaire as computeCommissionParitaireRule,
  computeTypeContrat as computeTypeContratRule,
  computeDateDebut as computeDateDebutRule,
  computeDateFin as computeDateFinRule,
  computeMotifRupture as computeMotifRuptureRule,
  computeNomRequerant as computeNomRequerantRule,
  computePrenomRequerant as computePrenomRequerantRule,
  computeDomicileRequerant as computeDomicileRequerantRule,
  computeNomDefendeur as computeNomDefendeurRule,
  computeSiegeSocialDefendeur as computeSiegeSocialDefendeurRule,
  computeNumeroBce as computeNumeroBceRule,
} from './tribunal-travail-fiche-section-prefill-rules';

/**
 * SF-173-02 : champs d'alerte de cohérence F-IA-03 exposés par F-DT-06
 * (requête tribunal du travail BE). Critiques : dateDebut, motifRupture.
 * SF-246-15 : ajout `NOM_REQUERANT` et `NOM_DEFENDEUR` (identités pré-remplies).
 */
export type TribunalAlertField = 'DATE_DEBUT' | 'MOTIF_RUPTURE' | 'COMMISSION_PARITAIRE' | 'TYPE_CONTRAT' | 'NOM_REQUERANT' | 'NOM_DEFENDEUR';

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
  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'REQUÊTE TRIBUNAL DU TRAVAIL';
  static readonly TOOL_ICON = 'balance';

  /** F-177 SF-177-12 / F-236 SF-236-02 — délègue au helper partagé (parité runtime). */
  static getPrefillCount(input: {
    aiData?: any;
    procedureChecks?: any[];
    aiQuestions?: any[];
    piecesManquantes?: any[];
    triggerEvents?: any[];
    workspaceCountry?: string;
  }): number {
    return TribunalTravailFicheSectionPrefillRules.computePrefillCount({
      aiData: input.aiData,
    });
  }

  @Input() caseFileId!: string;
  // F-177 SF-177-03b : force l'expansion (mode modal F-177).
  @Input() forceExpanded = false;
  @Input() caseFileTitle: string = '';
  // SF-173-02 : inputs IA pour pré-fill + validation F-IA-03 (pattern canonique F-155).
  @Input() aiData?: TravailExtractedData | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;
  /**
   * F-194 SF-194-02 — Libellés des pièces taggées « OBTENUE » alignées
   * sur cet outil. V1 = passif (signal d'aide visuel forward-compat).
   */
  @Input() piecesObtenues?: string[] | null;

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
  // SF-246-15 : provenance IA pour les champs identités.
  provenanceNomRequerant = signal<'IA' | null>(null);
  provenancePrenomRequerant = signal<'IA' | null>(null);
  provenanceDomicileRequerant = signal<'IA' | null>(null);
  provenanceNomDefendeur = signal<'IA' | null>(null);
  provenanceSiegeSocialDefendeur = signal<'IA' | null>(null);
  provenanceNumeroBce = signal<'IA' | null>(null);

  sourceExplanations = signal<Map<string, SourceExplanation[]>>(new Map());

  form: FormGroup;

  // SF-173-02 + SF-246-15 : alertes F-IA-03.
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

    // SF-246-15 : alertes identités.
    const nomReqAlert = this.buildNomRequerantAlert();
    if (nomReqAlert) alerts.NOM_REQUERANT = nomReqAlert;

    const nomDefAlert = this.buildNomDefendeurAlert();
    if (nomDefAlert) alerts.NOM_DEFENDEUR = nomDefAlert;

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
    @Optional() private dashboardRefresh: CaseDashboardRefreshService | null = null,
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

    // SF-246-15 : invalider provenance IA sur modification manuelle des identités.
    this.form.get('requerant.nom')?.valueChanges.subscribe(v => {
      if (this.provenanceNomRequerant() === 'IA' && v !== this.aiDataSignal()?.nomSalarie) {
        this.provenanceNomRequerant.set(null);
      }
    });
    this.form.get('requerant.prenom')?.valueChanges.subscribe(v => {
      if (this.provenancePrenomRequerant() === 'IA' && v !== this.aiDataSignal()?.prenomSalarie) {
        this.provenancePrenomRequerant.set(null);
      }
    });
    this.form.get('requerant.domicile')?.valueChanges.subscribe(v => {
      if (this.provenanceDomicileRequerant() === 'IA' && v !== this.aiDataSignal()?.adresseSalarie) {
        this.provenanceDomicileRequerant.set(null);
      }
    });
    this.form.get('defendeur.nom')?.valueChanges.subscribe(v => {
      if (this.provenanceNomDefendeur() === 'IA' && v !== this.aiDataSignal()?.nomEmployeur) {
        this.provenanceNomDefendeur.set(null);
      }
    });
    this.form.get('defendeur.siegeSocial')?.valueChanges.subscribe(v => {
      if (this.provenanceSiegeSocialDefendeur() === 'IA' && v !== this.aiDataSignal()?.adresseEmployeur) {
        this.provenanceSiegeSocialDefendeur.set(null);
      }
    });
    this.form.get('defendeur.numeroBce')?.valueChanges.subscribe(v => {
      if (this.provenanceNumeroBce() === 'IA' && v !== this.aiDataSignal()?.bceEmployeur) {
        this.provenanceNumeroBce.set(null);
      }
    });

    this.form.valueChanges.subscribe(v => this.formValueSignal.set(v));
  }

  ngOnInit(): void {
    // F-177 SF-177-03b : appliqué dès le mount pour le mode modal.
    if (this.forceExpanded) this.collapsed.set(false);

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
    // F-177 SF-177-03b : applique le forceExpanded quand il passe à true en cours de vie.
    if (changes['forceExpanded'] && this.forceExpanded) this.collapsed.set(false);

    if (changes['aiData']) this.aiDataSignal.set(this.aiData);
    if (changes['procedureChecks']) this.procedureChecksSignal.set(this.procedureChecks ?? []);
    if (changes['aiQuestions']) this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    if (changes['piecesManquantes']) this.piecesManquantesSignal.set(this.piecesManquantes ?? []);

    if (changes['aiData'] && !changes['aiData'].firstChange && !this.hasPersistedFiche()) {
      this.prefillFromAi();
    }
  }

  /**
   * SF-173-02 + SF-246-15 : pré-remplit le form depuis `aiData`. N'écrase pas
   * une saisie manuelle (la condition `!ctrl.value` garantit la préservation).
   *
   * Mapping `TravailExtractedData` → FormGroup (BE) :
   * - `conventionCollective`  → `procedureInfo.commissionParitaire`
   * - `typeContrat`           → `contratInfo.typeContrat` (normalisé EMPLOYE/OUVRIER)
   * - `dateEntree`            → `contratInfo.dateDebut`
   * - `dateLicenciement`      → `contratInfo.dateFin`
   * - `motifLicenciement`     → `contratInfo.motifRupture`
   * - `nomSalarie`            → `requerant.nom`            (SF-246-15)
   * - `prenomSalarie`         → `requerant.prenom`         (SF-246-15)
   * - `adresseSalarie`        → `requerant.domicile`       (SF-246-15)
   * - `nomEmployeur`          → `defendeur.nom`            (SF-246-15)
   * - `adresseEmployeur`      → `defendeur.siegeSocial`    (SF-246-15)
   * - `bceEmployeur`          → `defendeur.numeroBce`      (SF-246-15, BE uniquement)
   */
  private prefillFromAi(): void {
    const ai = this.aiDataSignal();
    if (!ai) return;

    // F-236 SF-236-02 : valeurs calculées par le helper partagé (parité static).
    const commissionParitaire = computeCommissionParitaireRule({ aiData: ai });
    if (commissionParitaire) {
      const ctrl = this.form.get('procedureInfo.commissionParitaire');
      if (ctrl && !ctrl.value) {
        ctrl.setValue(commissionParitaire, { emitEvent: false });
        this.provenanceCommissionParitaire.set('IA');
      }
    }

    const typeNorm = computeTypeContratRule({ aiData: ai });
    if (typeNorm) {
      const ctrl = this.form.get('contratInfo.typeContrat');
      if (ctrl && !ctrl.value) {
        ctrl.setValue(typeNorm, { emitEvent: false });
        this.provenanceTypeContrat.set('IA');
      }
    }

    const dateDebut = computeDateDebutRule({ aiData: ai });
    if (dateDebut) {
      const ctrl = this.form.get('contratInfo.dateDebut');
      if (ctrl && !ctrl.value) {
        ctrl.setValue(dateDebut, { emitEvent: false });
        this.provenanceDateDebut.set('IA');
      }
    }

    const dateFin = computeDateFinRule({ aiData: ai });
    if (dateFin) {
      const ctrl = this.form.get('contratInfo.dateFin');
      if (ctrl && !ctrl.value) {
        ctrl.setValue(dateFin, { emitEvent: false });
        this.provenanceDateFin.set('IA');
      }
    }

    const motifRupture = computeMotifRuptureRule({ aiData: ai });
    if (motifRupture) {
      const ctrl = this.form.get('contratInfo.motifRupture');
      if (ctrl && !ctrl.value) {
        ctrl.setValue(motifRupture, { emitEvent: false });
        this.provenanceMotifRupture.set('IA');
      }
    }

    // SF-246-15 : identités salarié/employeur.
    const nomRequerant = computeNomRequerantRule({ aiData: ai });
    if (nomRequerant) {
      const ctrl = this.form.get('requerant.nom');
      if (ctrl && !ctrl.value) {
        ctrl.setValue(nomRequerant, { emitEvent: false });
        this.provenanceNomRequerant.set('IA');
      }
    }

    const prenomRequerant = computePrenomRequerantRule({ aiData: ai });
    if (prenomRequerant) {
      const ctrl = this.form.get('requerant.prenom');
      if (ctrl && !ctrl.value) {
        ctrl.setValue(prenomRequerant, { emitEvent: false });
        this.provenancePrenomRequerant.set('IA');
      }
    }

    const domicile = computeDomicileRequerantRule({ aiData: ai });
    if (domicile) {
      const ctrl = this.form.get('requerant.domicile');
      if (ctrl && !ctrl.value) {
        ctrl.setValue(domicile, { emitEvent: false });
        this.provenanceDomicileRequerant.set('IA');
      }
    }

    const nomDefendeur = computeNomDefendeurRule({ aiData: ai });
    if (nomDefendeur) {
      const ctrl = this.form.get('defendeur.nom');
      if (ctrl && !ctrl.value) {
        ctrl.setValue(nomDefendeur, { emitEvent: false });
        this.provenanceNomDefendeur.set('IA');
      }
    }

    const siegeSocial = computeSiegeSocialDefendeurRule({ aiData: ai });
    if (siegeSocial) {
      const ctrl = this.form.get('defendeur.siegeSocial');
      if (ctrl && !ctrl.value) {
        ctrl.setValue(siegeSocial, { emitEvent: false });
        this.provenanceSiegeSocialDefendeur.set('IA');
      }
    }

    const numeroBce = computeNumeroBceRule({ aiData: ai });
    if (numeroBce) {
      const ctrl = this.form.get('defendeur.numeroBce');
      if (ctrl && !ctrl.value) {
        ctrl.setValue(numeroBce, { emitEvent: false });
        this.provenanceNumeroBce.set('IA');
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

  // SF-246-15 : handlers manuels identités.
  onNomRequerantChange(): void { this.provenanceNomRequerant.set(null); }
  onPrenomRequerantChange(): void { this.provenancePrenomRequerant.set(null); }
  onDomicileRequerantChange(): void { this.provenanceDomicileRequerant.set(null); }
  onNomDefendeurChange(): void { this.provenanceNomDefendeur.set(null); }
  onSiegeSocialDefendeurChange(): void { this.provenanceSiegeSocialDefendeur.set(null); }
  onNumeroBceChange(): void { this.provenanceNumeroBce.set(null); }

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
        case 'NOM_REQUERANT': return 'nom_salarie';
        case 'NOM_DEFENDEUR': return 'nom_employeur';
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

  // SF-246-15 : alertes identités.
  private buildNomRequerantAlert(): TribunalCoherenceAlert | null {
    const userValue = this.form.get('requerant.nom')?.value as string | null;
    if (!userValue || !userValue.trim()) return null;
    const ai = this.aiDataSignal();
    if (!ai?.nomSalarie) return null;
    if (userValue.trim().toLowerCase() === ai.nomSalarie.trim().toLowerCase()) return null;
    return CoherenceAlertBuilder.forField<TribunalAlertField>('NOM_REQUERANT')
      .addSource('IA', {
        expectedDisplay: ai.nomSalarie,
        reason: `Analyse du dossier : nom salarié ${ai.nomSalarie}`,
      })
      .build();
  }

  private buildNomDefendeurAlert(): TribunalCoherenceAlert | null {
    const userValue = this.form.get('defendeur.nom')?.value as string | null;
    if (!userValue || !userValue.trim()) return null;
    const ai = this.aiDataSignal();
    if (!ai?.nomEmployeur) return null;
    if (userValue.trim().toLowerCase() === ai.nomEmployeur.trim().toLowerCase()) return null;
    return CoherenceAlertBuilder.forField<TribunalAlertField>('NOM_DEFENDEUR')
      .addSource('IA', {
        expectedDisplay: ai.nomEmployeur,
        reason: `Analyse du dossier : nom employeur ${ai.nomEmployeur}`,
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
        // SF-173-02 + SF-246-15 : valeurs persistées validées par l'avocat — efface les badges IA.
        this.provenanceCommissionParitaire.set(null);
        this.provenanceTypeContrat.set(null);
        this.provenanceDateDebut.set(null);
        this.provenanceDateFin.set(null);
        this.provenanceMotifRupture.set(null);
        this.provenanceNomRequerant.set(null);
        this.provenancePrenomRequerant.set(null);
        this.provenanceDomicileRequerant.set(null);
        this.provenanceNomDefendeur.set(null);
        this.provenanceSiegeSocialDefendeur.set(null);
        this.provenanceNumeroBce.set(null);
        this.snackBar.open('Requête enregistrée', 'Fermer', {
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
    // SF-173-02 + SF-246-15 : valeurs persistées validées par l'avocat — efface les badges IA.
    this.provenanceCommissionParitaire.set(null);
    this.provenanceTypeContrat.set(null);
    this.provenanceDateDebut.set(null);
    this.provenanceDateFin.set(null);
    this.provenanceMotifRupture.set(null);
    this.provenanceNomRequerant.set(null);
    this.provenancePrenomRequerant.set(null);
    this.provenanceDomicileRequerant.set(null);
    this.provenanceNomDefendeur.set(null);
    this.provenanceSiegeSocialDefendeur.set(null);
    this.provenanceNumeroBce.set(null);
  }
}
