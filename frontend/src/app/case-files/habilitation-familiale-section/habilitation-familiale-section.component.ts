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
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { HabilitationFamilialeService } from '../../core/services/habilitation-familiale.service';
import {
  EtendueHabilitation,
  HabilitationFamilialeRequest,
  HabilitationFamilialeResponse,
  LienFamilialHabilitation,
  ModaliteHabilitation,
  VerdictHabilitationFamiliale,
} from '../../core/models/habilitation-familiale.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { HabilitationFamilialePrefillRules } from './habilitation-familiale-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-222-03 : composant Angular standalone pour l'outil décisionnel
 * "Habilitation familiale" (F-FA-HABILITATION-FAMILIALE).
 *
 * FRANCE uniquement (art. 494-1 et s. Cciv). L'outil CONSEILLE l'avocat sur les
 * conditions de l'habilitation familiale ; le prononcé relève du juge des
 * contentieux de la protection.
 *
 * Pattern de référence : `tgd-section` (SF-222-02).
 *
 * Anti-doublon F-FA-25 (sélecteur de régime de protection) : cadre les conditions
 * PROPRES de l'habilitation familiale, distinct du re-choix du régime. En cas de
 * conditions non réunies, le résultat renvoie vers F-FA-25.
 *
 * Formulaire (6 critères du contrat backend SF-222-03) :
 *  - alterationFacultesMedicalementConstatee (checkbox)
 *  - lienFamilialEligible (select)
 *  - consensusFamilial (checkbox)
 *  - besoinActesPatrimoniaux (checkbox)
 *  - besoinActesPersonnels (checkbox)
 *  - protectionPonctuelleOuGenerale (select)
 *
 * OnPush + ChangeDetectorRef.markForCheck() dans next/error des subscribe
 * (mémoire `feedback_onpush_subscribe_markforcheck`).
 */
@Component({
  selector: 'app-habilitation-familiale-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatCheckboxModule, MatSelectModule, MatFormFieldModule,
    MatProgressSpinnerModule,
    LegalCitationsPipe,
    ToolJurisprudenceCitationsComponent,
  ],
  templateUrl: './habilitation-familiale-section.component.html',
  styleUrl: './habilitation-familiale-section.component.scss',
})
export class HabilitationFamilialeSectionComponent implements OnInit, OnChanges {
  // F-JU-03 — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-FA-HABILITATION-FAMILIALE';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommée par le panel pour la card.
  static readonly TOOL_LABEL = 'HABILITATION FAMILIALE';
  static readonly TOOL_ICON = 'family_restroom';

  readonly liensFamiliaux: { value: LienFamilialHabilitation; label: string }[] = [
    { value: 'ASCENDANT', label: 'Ascendant (parent, grand-parent)' },
    { value: 'DESCENDANT', label: 'Descendant (enfant, petit-enfant)' },
    { value: 'FRERE_SOEUR', label: 'Frère ou sœur' },
    { value: 'CONJOINT_PARTENAIRE', label: 'Conjoint, partenaire de PACS ou concubin' },
    { value: 'AUTRE', label: 'Autre lien (non éligible)' },
  ];
  readonly etendues: { value: EtendueHabilitation; label: string }[] = [
    { value: 'PONCTUELLE', label: 'Ponctuelle (actes déterminés — habilitation spéciale)' },
    { value: 'GENERALE', label: 'Générale (ensemble des actes — habilitation générale)' },
  ];

  /**
   * SF-222-03 — délégué au helper partagé (parité stricte avec `prefillFromAi`).
   * Retourne le nombre de critères pré-remplissables FR (max 6).
   */
  static getPrefillCount(input: {
    aiData?: FamilleExtractedData | null;
    workspaceCountry?: string;
  }): number {
    return HabilitationFamilialePrefillRules.computePrefillCount({
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
  result = signal<HabilitationFamilialeResponse | null>(null);

  // --- Form fields (6 critères du contrat backend SF-222-03) ---
  alterationFacultesMedicalementConstatee = signal<boolean>(false);
  lienFamilialEligible = signal<LienFamilialHabilitation | null>(null);
  consensusFamilial = signal<boolean>(false);
  besoinActesPatrimoniaux = signal<boolean>(false);
  besoinActesPersonnels = signal<boolean>(false);
  protectionPonctuelleOuGenerale = signal<EtendueHabilitation | null>(null);

  // Provenance IA par critère (badge dans l'UI tant que valeur non modifiée).
  provenanceAlteration = signal<'IA' | null>(null);
  provenanceLien = signal<'IA' | null>(null);
  provenanceConsensus = signal<'IA' | null>(null);
  provenanceActesPatrimoniaux = signal<'IA' | null>(null);
  provenanceActesPersonnels = signal<'IA' | null>(null);
  provenanceEtendue = signal<'IA' | null>(null);

  constructor(
    private service: HabilitationFamilialeService,
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
   * Pré-fill IA SF-222-03 — renseigne les 6 critères détectables depuis
   * `familleExtractedData`. No-op gracieux si `aiData` absent ou dossier BE.
   */
  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    const ai = this.aiData;
    if (!ai) return;

    const rules = HabilitationFamilialePrefillRules;
    const ruleInput = { aiData: ai, workspaceCountry: this.workspaceCountry };

    const alteration = rules.computeAlteration(ruleInput);
    if (alteration !== null) {
      this.alterationFacultesMedicalementConstatee.set(alteration);
      this.provenanceAlteration.set('IA');
    }
    const lien = rules.computeLienFamilial(ruleInput);
    if (lien !== null) {
      this.lienFamilialEligible.set(lien);
      this.provenanceLien.set('IA');
    }
    const consensus = rules.computeConsensus(ruleInput);
    if (consensus !== null) {
      this.consensusFamilial.set(consensus);
      this.provenanceConsensus.set('IA');
    }
    const actesPatrimoniaux = rules.computeActesPatrimoniaux(ruleInput);
    if (actesPatrimoniaux !== null) {
      this.besoinActesPatrimoniaux.set(actesPatrimoniaux);
      this.provenanceActesPatrimoniaux.set('IA');
    }
    const actesPersonnels = rules.computeActesPersonnels(ruleInput);
    if (actesPersonnels !== null) {
      this.besoinActesPersonnels.set(actesPersonnels);
      this.provenanceActesPersonnels.set('IA');
    }
    const etendue = rules.computeEtendue(ruleInput);
    if (etendue !== null) {
      this.protectionPonctuelleOuGenerale.set(etendue);
      this.provenanceEtendue.set('IA');
    }
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  editMode(): void {
    this.showForm.set(true);
  }

  /** Form valide : FR + étendue choisie (les booléens sont toujours saisissables). */
  formValid(): boolean {
    return this.workspaceCountry === 'FRANCE'
      && this.protectionPonctuelleOuGenerale() !== null;
  }

  // --- Handlers — toute modification manuelle efface le badge IA ---

  onAlterationChange(checked: boolean): void {
    this.alterationFacultesMedicalementConstatee.set(checked);
    this.provenanceAlteration.set(null);
  }

  onLienChange(value: LienFamilialHabilitation): void {
    this.lienFamilialEligible.set(value);
    this.provenanceLien.set(null);
  }

  onConsensusChange(checked: boolean): void {
    this.consensusFamilial.set(checked);
    this.provenanceConsensus.set(null);
  }

  onActesPatrimoniauxChange(checked: boolean): void {
    this.besoinActesPatrimoniaux.set(checked);
    this.provenanceActesPatrimoniaux.set(null);
  }

  onActesPersonnelsChange(checked: boolean): void {
    this.besoinActesPersonnels.set(checked);
    this.provenanceActesPersonnels.set(null);
  }

  onEtendueChange(value: EtendueHabilitation): void {
    this.protectionPonctuelleOuGenerale.set(value);
    this.provenanceEtendue.set(null);
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request: HabilitationFamilialeRequest = {
      alterationFacultesMedicalementConstatee: this.alterationFacultesMedicalementConstatee(),
      lienFamilialEligible: this.lienFamilialEligible(),
      consensusFamilial: this.consensusFamilial(),
      besoinActesPatrimoniaux: this.besoinActesPatrimoniaux(),
      besoinActesPersonnels: this.besoinActesPersonnels(),
      protectionPonctuelleOuGenerale: this.protectionPonctuelleOuGenerale(),
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.applyResult(r);
        this.calculating.set(false);
        this.snackBar.open('Habilitation familiale évaluée', 'OK', { duration: 2500 });
        if (!this.standaloneMode) this.dashboardRefresh?.triggerRefresh();
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.calculating.set(false);
        const msg = err?.error?.message || err?.error || 'Erreur lors de l\'analyse';
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

  private applyResult(r: HabilitationFamilialeResponse): void {
    this.result.set(r);
    this.showForm.set(false);
  }

  // ---------------------------------------------------------------------------
  // Helpers d'affichage du résultat
  // ---------------------------------------------------------------------------

  /** Libellé humain pour le verdict. */
  verdictLabel(verdict: VerdictHabilitationFamiliale): string {
    const labels: Record<VerdictHabilitationFamiliale, string> = {
      ELIGIBLE_HABILITATION_GENERALE: 'Éligible — habilitation familiale générale',
      ELIGIBLE_HABILITATION_SPECIALE: 'Éligible — habilitation familiale spéciale',
      ORIENTER_VERS_MESURE_JUDICIAIRE: 'Orienter vers une mesure judiciaire (F-FA-25)',
    };
    return labels[verdict];
  }

  /** Classe CSS par verdict pour la pastille. */
  verdictClass(verdict: VerdictHabilitationFamiliale): string {
    const cls: Record<VerdictHabilitationFamiliale, string> = {
      ELIGIBLE_HABILITATION_GENERALE: 'verdict-success',
      ELIGIBLE_HABILITATION_SPECIALE: 'verdict-info',
      ORIENTER_VERS_MESURE_JUDICIAIRE: 'verdict-warning',
    };
    return cls[verdict];
  }

  /** Libellé humain pour la modalité. */
  modaliteLabel(modalite: ModaliteHabilitation | null): string | null {
    if (modalite === null) return null;
    return modalite === 'REPRESENTATION' ? 'Représentation (besoin lourd)' : 'Assistance (besoin léger)';
  }
}
