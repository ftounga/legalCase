import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  Input,
  OnChanges,
  OnInit,
  Optional,
  SimpleChanges,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar } from '@angular/material/snack-bar';
import { RetraitApFrService } from '../../core/services/retrait-ap-fr.service';
import {
  MotifRetraitAp,
  RetraitAutoriteParentaleRequest,
  RetraitAutoriteParentaleResponse,
  TypeRetraitAp,
  VerdictRetraitAp,
  VoieProceduraleRetraitAp,
} from '../../core/models/retrait-ap-fr.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { RetraitApFrPrefillRules } from './retrait-ap-fr-section-prefill-rules';

/**
 * SF-216-12 : composant Angular standalone pour l'outil décisionnel
 * "Retrait d'autorité parentale" (F-FA-RETRAIT-AP).
 *
 * FRANCE uniquement (art. 378-381 Cciv + loi n°2022-140 du 7 février 2022
 * LMVSS + art. 343-1 al. 2 Cciv + Cass. 1ère civ., 26/10/2011).
 *
 * Pattern de référence : `delegation-ap-fr-section` (SF-216-10) et
 * `aripa-recouvrement-fr-section` (SF-216-08).
 *
 * Formulaire (7 champs) :
 *  - typeRetrait (3 valeurs : TOTAL | PARTIEL_EXERCICE | PARTIEL_ATTRIBUTS)
 *  - motifRetrait (5 valeurs)
 *  - condamnationPenaleDetectee (checkbox, pré-coché si détecté)
 *  - dangerCaracterise (checkbox, pré-coché si F-246 danger immédiat)
 *  - violencesConjugalesDetectees (checkbox, pré-coché si F-246 violences
 *    alléguées OU SF-216-11 violences LMVSS 2022)
 *  - ageEnfant (0-18 ans, pré-rempli depuis filiation_detection_v2)
 *  - decisionsJudiciairesPrecedentes (checkbox)
 *
 * Verdict affiché : verdictRetrait, voieProcedurale, admissibiliteAdoption,
 * consequencesJuridiques, etapes (stepper), dureeEstimeeJours, baseLegale,
 * messages, alertes.
 *
 * Country gate : `workspaceCountry !== 'FRANCE'` → bannière info + aucun appel.
 *
 * OnPush + ChangeDetectorRef.markForCheck() dans next/error des subscribe
 * (mémoire `feedback_onpush_subscribe_markforcheck`).
 */
@Component({
  selector: 'app-retrait-ap-fr-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatSelectModule, MatCheckboxModule,
    MatProgressSpinnerModule, MatTooltipModule,
    LegalCitationsPipe,
  ],
  templateUrl: './retrait-ap-fr-section.component.html',
  styleUrl: './retrait-ap-fr-section.component.scss',
})
export class RetraitApFrSectionComponent implements OnInit, OnChanges {
  // F-177 SF-177-03b : metadata statique consommée par le panel pour la card.
  static readonly TOOL_LABEL = 'RETRAIT AUTORITE PARENTALE';
  static readonly TOOL_ICON = 'gpp_bad';

  /**
   * SF-216-12 — délégué au helper partagé (parité stricte avec `prefillFromAi`).
   * Retourne le nombre de champs pré-remplissables FR (max 4 : âge enfant,
   * condamnation pénale, danger caractérisé, violences conjugales).
   */
  static getPrefillCount(input: {
    aiData?: FamilleExtractedData | null;
    workspaceCountry?: string;
  }): number {
    return RetraitApFrPrefillRules.computePrefillCount({
      aiData: input.aiData,
      workspaceCountry: input.workspaceCountry,
    });
  }

  @Input() caseFileId!: string;
  @Input() forceExpanded = false;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';
  @Input() aiData?: FamilleExtractedData | null;
  /** Mode simulateur autonome (hors dossier) — coupe le refresh dashboard. */
  @Input() standaloneMode = false;

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<RetraitAutoriteParentaleResponse | null>(null);

  // --- Form fields (7 champs du contrat backend SF-216-11) ---
  typeRetrait = signal<TypeRetraitAp | null>(null);
  motifRetrait = signal<MotifRetraitAp | null>(null);
  condamnationPenaleDetectee = signal<boolean>(false);
  dangerCaracterise = signal<boolean>(false);
  violencesConjugalesDetectees = signal<boolean>(false);
  ageEnfant = signal<number | null>(null);
  decisionsJudiciairesPrecedentes = signal<boolean>(false);

  // Provenance IA par champ (badge dans l'UI tant que valeur non modifiée).
  provenanceAgeEnfant = signal<'IA' | null>(null);
  provenanceCondamnationPenale = signal<'IA' | null>(null);
  provenanceDanger = signal<'IA' | null>(null);
  provenanceViolencesConjugales = signal<'IA' | null>(null);

  /** Options select type de retrait. */
  readonly typesRetrait: ReadonlyArray<{ value: TypeRetraitAp; label: string }> = [
    { value: 'TOTAL', label: 'Retrait total (art. 378 Cciv)' },
    { value: 'PARTIEL_EXERCICE', label: 'Retrait partiel — exercice (art. 378-1 al. 1)' },
    { value: 'PARTIEL_ATTRIBUTS', label: 'Retrait partiel — attributs (art. 379-1)' },
  ];

  /** Options select motif. */
  readonly motifsRetrait: ReadonlyArray<{ value: MotifRetraitAp; label: string }> = [
    { value: 'CONDAMNATION_PENALE', label: 'Condamnation pénale crime/délit sur l\'enfant (art. 378 al. 1)' },
    { value: 'DANGER_CARACTERISE_VIOLENCES', label: 'Mauvais traitements / danger caractérisé (art. 378-1)' },
    { value: 'DESINTERET_GRAVE', label: 'Désintérêt manifeste > 2 ans (art. 378-1 al. 2)' },
    { value: 'COMPORTEMENT_GRAVEMENT_COMPROMETTANT', label: 'Comportement gravement compromettant (art. 378-1)' },
    { value: 'VIOLENCES_LMVSS_2022', label: 'Violences conjugales loi 2022-140 (LMVSS)' },
  ];

  constructor(
    private service: RetraitApFrService,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef,
    @Optional() private dashboardRefresh: CaseDashboardRefreshService | null,
  ) {}

  ngOnInit(): void {
    if (this.forceExpanded) this.collapsed.set(false);
    this.prefillFromAi();
    if (this.workspaceCountry === 'FRANCE') {
      this.load();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['forceExpanded'] && this.forceExpanded) this.collapsed.set(false);
    if (changes['aiData'] && !changes['aiData'].firstChange
        && this.showForm() && !this.result()) {
      this.prefillFromAi();
    }
  }

  /**
   * Pré-fill IA SF-216-12 — renseigne les 4 champs détectables depuis
   * `familleExtractedData` (âge enfant, condamnation pénale, danger,
   * violences conjugales). No-op gracieux si `aiData` absent ou dossier BE.
   */
  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    const ai = this.aiData;
    if (!ai) return;

    const rules = RetraitApFrPrefillRules;
    const ruleInput = { aiData: ai, workspaceCountry: this.workspaceCountry };

    const age = rules.computeAgeEnfant(ruleInput);
    if (age !== null) {
      this.ageEnfant.set(age);
      this.provenanceAgeEnfant.set('IA');
    }
    const cond = rules.computeCondamnationPenale(ruleInput);
    if (cond !== null) {
      this.condamnationPenaleDetectee.set(cond);
      this.provenanceCondamnationPenale.set('IA');
    }
    const danger = rules.computeDangerCaracterise(ruleInput);
    if (danger !== null) {
      this.dangerCaracterise.set(danger);
      this.provenanceDanger.set('IA');
    }
    const violences = rules.computeViolencesConjugales(ruleInput);
    if (violences !== null) {
      this.violencesConjugalesDetectees.set(violences);
      this.provenanceViolencesConjugales.set('IA');
    }
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  editMode(): void {
    this.showForm.set(true);
  }

  /**
   * Form valide : FR + type de retrait + motif + âge enfant renseignés.
   */
  formValid(): boolean {
    if (this.workspaceCountry !== 'FRANCE') return false;
    if (this.typeRetrait() === null) return false;
    if (this.motifRetrait() === null) return false;
    const age = this.ageEnfant();
    if (age === null || !Number.isFinite(age) || age < 0 || age > 18) return false;
    return true;
  }

  // --- Handlers — toute modification manuelle efface le badge IA ---

  onTypeRetraitChange(value: TypeRetraitAp | null): void {
    this.typeRetrait.set(value);
  }

  onMotifChange(value: MotifRetraitAp | null): void {
    this.motifRetrait.set(value);
  }

  onCondamnationPenaleChange(checked: boolean): void {
    this.condamnationPenaleDetectee.set(checked);
    this.provenanceCondamnationPenale.set(null);
  }

  onDangerChange(checked: boolean): void {
    this.dangerCaracterise.set(checked);
    this.provenanceDanger.set(null);
  }

  onViolencesConjugalesChange(checked: boolean): void {
    this.violencesConjugalesDetectees.set(checked);
    this.provenanceViolencesConjugales.set(null);
  }

  onAgeEnfantChange(value: number | string | null): void {
    this.ageEnfant.set(this.parseAge(value));
    this.provenanceAgeEnfant.set(null);
  }

  onDecisionsPrecChange(checked: boolean): void {
    this.decisionsJudiciairesPrecedentes.set(checked);
  }

  private parseAge(value: number | string | null | undefined): number | null {
    if (value === null || value === undefined || value === '') return null;
    const num = typeof value === 'number' ? value : Number.parseInt(String(value), 10);
    if (!Number.isFinite(num) || num < 0 || num > 18) return null;
    return num;
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request: RetraitAutoriteParentaleRequest = {
      typeRetrait: this.typeRetrait(),
      motifRetrait: this.motifRetrait(),
      condamnationPenaleDetectee: this.condamnationPenaleDetectee(),
      dangerCaracterise: this.dangerCaracterise(),
      violencesConjugalesDetectees: this.violencesConjugalesDetectees(),
      ageEnfant: this.ageEnfant(),
      decisionsJudiciairesPrecedentes: this.decisionsJudiciairesPrecedentes(),
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.applyResult(r);
        this.calculating.set(false);
        this.snackBar.open('Retrait AP analysé', 'OK', { duration: 2500 });
        if (!this.standaloneMode) this.dashboardRefresh?.triggerRefresh();
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.calculating.set(false);
        const msg = err?.error?.message || err?.error || 'Erreur lors du calcul';
        this.snackBar.open(String(msg), 'Fermer', { duration: 5000, panelClass: 'snack-error' });
        this.cdr.markForCheck();
      },
    });
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.applyResult(r);
        this.loading.set(false);
        this.cdr.markForCheck();
      },
      error: () => {
        // 404 attendu si aucune analyse — on reste en mode formulaire.
        this.loading.set(false);
        this.cdr.markForCheck();
      },
    });
  }

  private applyResult(r: RetraitAutoriteParentaleResponse): void {
    this.result.set(r);
    this.showForm.set(false);
  }

  // ---------------------------------------------------------------------------
  // Helpers d'affichage du résultat
  // ---------------------------------------------------------------------------

  /** Libellé humain pour le verdict de retrait. */
  verdictLabel(verdict: VerdictRetraitAp): string {
    const labels: Record<VerdictRetraitAp, string> = {
      RETRAIT_PLEIN_DROIT: 'Retrait de plein droit (juridiction pénale)',
      RETRAIT_CIVIL_JAF: 'Retrait civil — saisine JAF / TJ',
      SUSPENSION_ACCELEREE_LMVSS_2022: 'Suspension accélérée — loi 2022-140 LMVSS',
      IRRECEVABLE_ENFANT_MAJEUR: 'Irrecevable — enfant majeur',
      IRRECEVABLE_MOTIF_NON_CARACTERISE: 'Irrecevable — motif non caractérisé',
    };
    return labels[verdict];
  }

  /** Couleur Material par verdict pour la pastille. */
  verdictClass(verdict: VerdictRetraitAp): string {
    const cls: Record<VerdictRetraitAp, string> = {
      RETRAIT_PLEIN_DROIT: 'verdict-danger',
      RETRAIT_CIVIL_JAF: 'verdict-primary',
      SUSPENSION_ACCELEREE_LMVSS_2022: 'verdict-danger',
      IRRECEVABLE_ENFANT_MAJEUR: 'verdict-warning',
      IRRECEVABLE_MOTIF_NON_CARACTERISE: 'verdict-warning',
    };
    return cls[verdict];
  }

  /** Libellé humain pour la voie procédurale. */
  voieLabel(voie: VoieProceduraleRetraitAp): string {
    const labels: Record<VoieProceduraleRetraitAp, string> = {
      JURIDICTION_PENALE_ACCESSOIRE: 'Juridiction pénale — retrait accessoire',
      JAF_TRIBUNAL_JUDICIAIRE: 'JAF / tribunal judiciaire',
      PROCUREUR_REPUBLIQUE_ASSISTANCE_EDUCATIVE: 'Procureur de la République + assistance éducative',
      LMVSS_2022_SUSPENSION_AUTOMATIQUE: 'Suspension automatique loi 2022 + JAF accéléré',
      SANS_OBJET: 'Sans objet',
    };
    return labels[voie];
  }
}
