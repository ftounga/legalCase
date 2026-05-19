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
  computeNomSalarie as computeNomSalarieRule,
  computePrenomSalarie as computePrenomSalarieRule,
  computeAdresseSalarie as computeAdresseSalarieRule,
  computeNomEmployeur as computeNomEmployeurRule,
  computeAdresseEmployeur as computeAdresseEmployeurRule,
  computeSiretEmployeur as computeSiretEmployeurRule,
} from './prudhome-fiche-section-prefill-rules';

/**
 * SF-173-01 : champs d'alerte de cohérence F-IA-03 exposés par F-DT-04
 * (fiche prud'homale FR).
 * SF-246-15 : ajout `NOM_SALARIE` et `NOM_EMPLOYEUR` (identités pré-remplies).
 */
export type PrudhomeAlertField = 'PROFESSION' | 'NOM_SALARIE' | 'NOM_EMPLOYEUR';

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
  // SF-246-15 : provenance IA pour les champs identités.
  provenanceNomSalarie = signal<'IA' | null>(null);
  provenancePrenomSalarie = signal<'IA' | null>(null);
  provenanceAdresseSalarie = signal<'IA' | null>(null);
  provenanceNomEmployeur = signal<'IA' | null>(null);
  provenanceAdresseEmployeur = signal<'IA' | null>(null);
  provenanceSiretEmployeur = signal<'IA' | null>(null);

  // SF-IA-03-15c : map {sourceKey → explanations} pour le popover enrichi.
  sourceExplanations = signal<Map<string, SourceExplanation[]>>(new Map());

  form: FormGroup;

  // SF-173-01 + SF-246-15 : alertes F-IA-03 dynamiques.
  coherenceAlerts = computed<Partial<Record<PrudhomeAlertField, PrudhomeCoherenceAlert>>>(() => {
    // Force re-eval au changement form value.
    this.formValueSignal();
    if (this.hasPersistedFiche()) return {};
    const ai = this.aiDataSignal();
    if (!ai) return {};
    const alerts: Partial<Record<PrudhomeAlertField, PrudhomeCoherenceAlert>> = {};

    const professionAlert = this.buildProfessionAlert();
    if (professionAlert) alerts.PROFESSION = professionAlert;

    // SF-246-15 : alertes identités.
    const nomSalarieAlert = this.buildNomSalarieAlert();
    if (nomSalarieAlert) alerts.NOM_SALARIE = nomSalarieAlert;

    const nomEmployeurAlert = this.buildNomEmployeurAlert();
    if (nomEmployeurAlert) alerts.NOM_EMPLOYEUR = nomEmployeurAlert;

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
      if (this.provenanceProfession() === 'IA') {
        const aiValue = this.aiDataSignal()?.poste;
        const current = this.form.get('demandeur.profession')?.value;
        if (current !== aiValue) {
          this.provenanceProfession.set(null);
        }
      }
    });

    // SF-246-15 : invalider provenance IA sur modification manuelle des identités.
    this.form.get('demandeur.nom')?.valueChanges.subscribe(v => {
      if (this.provenanceNomSalarie() === 'IA' && v !== this.aiDataSignal()?.nomSalarie) {
        this.provenanceNomSalarie.set(null);
      }
    });
    this.form.get('demandeur.prenom')?.valueChanges.subscribe(v => {
      if (this.provenancePrenomSalarie() === 'IA' && v !== this.aiDataSignal()?.prenomSalarie) {
        this.provenancePrenomSalarie.set(null);
      }
    });
    this.form.get('demandeur.adresse')?.valueChanges.subscribe(v => {
      if (this.provenanceAdresseSalarie() === 'IA' && v !== this.aiDataSignal()?.adresseSalarie) {
        this.provenanceAdresseSalarie.set(null);
      }
    });
    this.form.get('defendeur.nom')?.valueChanges.subscribe(v => {
      if (this.provenanceNomEmployeur() === 'IA' && v !== this.aiDataSignal()?.nomEmployeur) {
        this.provenanceNomEmployeur.set(null);
      }
    });
    this.form.get('defendeur.adresse')?.valueChanges.subscribe(v => {
      if (this.provenanceAdresseEmployeur() === 'IA' && v !== this.aiDataSignal()?.adresseEmployeur) {
        this.provenanceAdresseEmployeur.set(null);
      }
    });
    this.form.get('defendeur.siret')?.valueChanges.subscribe(v => {
      if (this.provenanceSiretEmployeur() === 'IA' && v !== this.aiDataSignal()?.siretEmployeur) {
        this.provenanceSiretEmployeur.set(null);
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
   * SF-173-01 + SF-246-15 : pré-remplit le form depuis `aiData`. N'écrase pas une saisie
   * manuelle (la condition `!ctrl.value` garantit la préservation des saisies).
   *
   * Mapping `TravailExtractedData` → FormGroup (FR) :
   * - `poste`          → `demandeur.profession`
   * - `nomSalarie`     → `demandeur.nom`         (SF-246-15)
   * - `prenomSalarie`  → `demandeur.prenom`       (SF-246-15)
   * - `adresseSalarie` → `demandeur.adresse`      (SF-246-15)
   * - `nomEmployeur`   → `defendeur.nom`          (SF-246-15)
   * - `adresseEmployeur` → `defendeur.adresse`    (SF-246-15)
   * - `siretEmployeur` → `defendeur.siret`        (SF-246-15, FR uniquement)
   *
   * Champs non mappés : `demandeur.telephone/email`, `defendeur.representant`,
   * `demandes`, `faitsTexte`, `moyensDroitTexte` — l'avocat les complète manuellement.
   */
  private prefillFromAi(): void {
    const ai = this.aiDataSignal();
    if (!ai) return;

    // F-236 SF-236-02 : profession.
    const profession = computeProfessionRule({ aiData: ai });
    if (profession) {
      const ctrl = this.form.get('demandeur.profession');
      if (ctrl && !ctrl.value) {
        ctrl.setValue(profession, { emitEvent: false });
        this.provenanceProfession.set('IA');
      }
    }

    // SF-246-15 : identités salarié.
    const nomSalarie = computeNomSalarieRule({ aiData: ai });
    if (nomSalarie) {
      const ctrl = this.form.get('demandeur.nom');
      if (ctrl && !ctrl.value) {
        ctrl.setValue(nomSalarie, { emitEvent: false });
        this.provenanceNomSalarie.set('IA');
      }
    }

    const prenomSalarie = computePrenomSalarieRule({ aiData: ai });
    if (prenomSalarie) {
      const ctrl = this.form.get('demandeur.prenom');
      if (ctrl && !ctrl.value) {
        ctrl.setValue(prenomSalarie, { emitEvent: false });
        this.provenancePrenomSalarie.set('IA');
      }
    }

    const adresseSalarie = computeAdresseSalarieRule({ aiData: ai });
    if (adresseSalarie) {
      const ctrl = this.form.get('demandeur.adresse');
      if (ctrl && !ctrl.value) {
        ctrl.setValue(adresseSalarie, { emitEvent: false });
        this.provenanceAdresseSalarie.set('IA');
      }
    }

    // SF-246-15 : identités employeur.
    const nomEmployeur = computeNomEmployeurRule({ aiData: ai });
    if (nomEmployeur) {
      const ctrl = this.form.get('defendeur.nom');
      if (ctrl && !ctrl.value) {
        ctrl.setValue(nomEmployeur, { emitEvent: false });
        this.provenanceNomEmployeur.set('IA');
      }
    }

    const adresseEmployeur = computeAdresseEmployeurRule({ aiData: ai });
    if (adresseEmployeur) {
      const ctrl = this.form.get('defendeur.adresse');
      if (ctrl && !ctrl.value) {
        ctrl.setValue(adresseEmployeur, { emitEvent: false });
        this.provenanceAdresseEmployeur.set('IA');
      }
    }

    const siretEmployeur = computeSiretEmployeurRule({ aiData: ai });
    if (siretEmployeur) {
      const ctrl = this.form.get('defendeur.siret');
      if (ctrl && !ctrl.value) {
        ctrl.setValue(siretEmployeur, { emitEvent: false });
        this.provenanceSiretEmployeur.set('IA');
      }
    }
  }

  /** SF-173-01 : effacer le badge IA quand l'avocat modifie manuellement. */
  onProfessionChange(): void {
    this.provenanceProfession.set(null);
  }

  // SF-246-15 : handlers manuels identités.
  onNomSalarieChange(): void { this.provenanceNomSalarie.set(null); }
  onPrenomSalarieChange(): void { this.provenancePrenomSalarie.set(null); }
  onAdresseSalarieChange(): void { this.provenanceAdresseSalarie.set(null); }
  onNomEmployeurChange(): void { this.provenanceNomEmployeur.set(null); }
  onAdresseEmployeurChange(): void { this.provenanceAdresseEmployeur.set(null); }
  onSiretEmployeurChange(): void { this.provenanceSiretEmployeur.set(null); }

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
    const key = (() => {
      switch (field) {
        case 'PROFESSION': return 'poste';
        case 'NOM_SALARIE': return 'nom_salarie';
        case 'NOM_EMPLOYEUR': return 'nom_employeur';
      }
    })();
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

  // SF-246-15 : alertes identités.
  private buildNomSalarieAlert(): PrudhomeCoherenceAlert | null {
    const userValue = this.form.get('demandeur.nom')?.value as string | null;
    if (!userValue || !userValue.trim()) return null;
    const ai = this.aiDataSignal();
    if (!ai?.nomSalarie) return null;
    if (userValue.trim().toLowerCase() === ai.nomSalarie.trim().toLowerCase()) return null;
    return CoherenceAlertBuilder.forField<PrudhomeAlertField>('NOM_SALARIE')
      .addSource('IA', {
        expectedDisplay: ai.nomSalarie,
        reason: `Analyse du dossier : nom salarié ${ai.nomSalarie}`,
      })
      .build();
  }

  private buildNomEmployeurAlert(): PrudhomeCoherenceAlert | null {
    const userValue = this.form.get('defendeur.nom')?.value as string | null;
    if (!userValue || !userValue.trim()) return null;
    const ai = this.aiDataSignal();
    if (!ai?.nomEmployeur) return null;
    if (userValue.trim().toLowerCase() === ai.nomEmployeur.trim().toLowerCase()) return null;
    return CoherenceAlertBuilder.forField<PrudhomeAlertField>('NOM_EMPLOYEUR')
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
        // SF-173-01 + SF-246-15 : valeurs persistées par l'avocat — efface les badges IA.
        this.provenanceProfession.set(null);
        this.provenanceNomSalarie.set(null);
        this.provenancePrenomSalarie.set(null);
        this.provenanceAdresseSalarie.set(null);
        this.provenanceNomEmployeur.set(null);
        this.provenanceAdresseEmployeur.set(null);
        this.provenanceSiretEmployeur.set(null);
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
    // SF-173-01 + SF-246-15 : valeurs persistées validées par l'avocat — efface les badges IA.
    this.provenanceProfession.set(null);
    this.provenanceNomSalarie.set(null);
    this.provenancePrenomSalarie.set(null);
    this.provenanceAdresseSalarie.set(null);
    this.provenanceNomEmployeur.set(null);
    this.provenanceAdresseEmployeur.set(null);
    this.provenanceSiretEmployeur.set(null);
  }
}
