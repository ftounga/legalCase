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
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { TempsPartielRequalificationService } from '../../core/services/temps-partiel-requalification.service';
import {
  TempsPartielAnalyseRequalification,
  TempsPartielFacteurRequalification,
  TempsPartielRequalificationRequest,
  TempsPartielRequalificationResponse,
} from '../../core/models/temps-partiel-requalification.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { TempsPartielRequalificationSectionPrefillRules } from './temps-partiel-requalification-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-212-34 : composant Angular standalone pour l'outil décisionnel
 * "Temps partiel — requalification en temps plein" — tool_id canonique
 * `F-DT-49-temps-partiel-requalification`. FRANCE uniquement
 * (L. 3123-1 à L. 3123-20 CT ; L. 3123-6 mentions obligatoires ;
 * L. 3123-9 plafond heures complémentaires ; L. 3245-1 prescription
 * triennale rappel salaire ; Cass. soc. 22/01/1992 présomption de
 * temps complet réfragable).
 *
 * Pattern de référence : `burnout-reconnaissance-mp-section` (F-DT-64,
 * SF-212-28) — formulaire, POST de calcul, verdict 3 niveaux, refresh
 * dashboard, OnPush + markForCheck dans les subscribe.
 *
 * Verdict 3 niveaux :
 *  - REQUALIFICATION_PROBABLE (rouge — mentions L. 3123-6 absentes
 *    OU HC > 1/3 ⇒ dossier solide)
 *  - REQUALIFICATION_POSSIBLE (or — HC entre 1/10 et 1/3 OU modification
 *    unilatérale ⇒ faisceau d'indices)
 *  - PAS_DE_REQUALIFICATION (vert — contrat régulier)
 *
 * Bannière FR-only : si le workspace n'est pas en FRANCE, l'écran affiche
 * uniquement un avertissement pédagogique (régime BE Loi 03/07/1978 + CCT
 * n°35 distinct).
 */
@Component({
  selector: 'app-temps-partiel-requalification-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatSlideToggleModule,
    MatProgressSpinnerModule,
    LegalCitationsPipe,
    ToolJurisprudenceCitationsComponent,
  ],
  templateUrl: './temps-partiel-requalification-section.component.html',
  styleUrl: './temps-partiel-requalification-section.component.scss',
})
export class TempsPartielRequalificationSectionComponent implements OnInit, OnChanges {
  // F-JU-03 — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-DT-49-temps-partiel-requalification';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommée par le panel pour la card.
  static readonly TOOL_LABEL = 'TEMPS PARTIEL — REQUALIFICATION';
  static readonly TOOL_ICON = 'schedule';

  /**
   * SF-244 — délégué au helper partagé (parité stricte avec `prefillFromAi()`).
   * Retourne le nombre exact de champs pré-remplissables FR pour le badge tab.
   * Couvre les 4 champs `tempsPartiel*`.
   */
  static getPrefillCount(input: {
    aiData?: TravailExtractedData | null;
    procedureChecks?: unknown[];
    aiQuestions?: unknown[];
    piecesManquantes?: unknown[];
    triggerEvents?: unknown[];
    workspaceCountry?: string;
  }): number {
    return TempsPartielRequalificationSectionPrefillRules.computePrefillCount({
      aiData: input.aiData,
      workspaceCountry: input.workspaceCountry,
    });
  }

  @Input() caseFileId!: string;
  @Input() forceExpanded = false;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';
  @Input() aiData?: TravailExtractedData | null;
  /** Mode simulateur autonome (hors dossier) — coupe refresh dashboard. */
  @Input() standaloneMode = false;

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<TempsPartielRequalificationResponse | null>(null);

  // --- Form fields (8 champs du contrat backend) ---
  dureeContractuelleHeures = signal<number>(24);
  mentionsDureePresentes = signal<boolean>(true);
  mentionsRepartitionPresentes = signal<boolean>(true);
  mentionsHCPresentes = signal<boolean>(true);
  heuresComplementairesReelleMoyenneHeures = signal<number>(0);
  modificationRepartitionUnilaterale = signal<boolean>(false);
  ancienneteMois = signal<number>(0);
  salaireMensuelContractuelEuros = signal<number>(0);

  // Provenance IA par champ pré-rempli — badge `auto_awesome` s'efface au
  // premier `onXxxChange()` manuel sur ce champ.
  provenanceDuree = signal<'IA' | null>(null);
  provenanceMentionsDuree = signal<'IA' | null>(null);
  provenanceMentionsRepartition = signal<'IA' | null>(null);
  provenanceHCMoyenne = signal<'IA' | null>(null);

  constructor(
    private service: TempsPartielRequalificationService,
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
   * Pré-fill IA (SF-212-34) — renseigne les 4 champs depuis le sous-objet
   * `temps_partiel_requalification_detail` (projeté à plat dans
   * `travailExtractedData` via Jackson @JsonUnwrapped côté backend). Parité
   * stricte avec `getPrefillCount()` : mêmes mappings, même gate
   * `workspaceCountry === 'FRANCE'`.
   */
  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    const ai = this.aiData;
    if (!ai) return;
    const rules = TempsPartielRequalificationSectionPrefillRules;
    const ruleInput = { aiData: ai, workspaceCountry: this.workspaceCountry };

    const duree = rules.computeDureeContractuelle(ruleInput);
    if (duree !== null) {
      this.dureeContractuelleHeures.set(duree);
      this.provenanceDuree.set('IA');
    }
    const mentDuree = rules.computeMentionsDuree(ruleInput);
    if (mentDuree !== null) {
      this.mentionsDureePresentes.set(mentDuree);
      this.provenanceMentionsDuree.set('IA');
    }
    const mentRepart = rules.computeMentionsRepartition(ruleInput);
    if (mentRepart !== null) {
      this.mentionsRepartitionPresentes.set(mentRepart);
      this.provenanceMentionsRepartition.set('IA');
    }
    const hc = rules.computeHCMoyenne(ruleInput);
    if (hc !== null) {
      this.heuresComplementairesReelleMoyenneHeures.set(hc);
      this.provenanceHCMoyenne.set('IA');
    }
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  editMode(): void {
    this.showForm.set(true);
  }

  /** Form valide : FRANCE + durée (0, 35[ + HC >= 0 + ancienneté >= 0. */
  formValid(): boolean {
    if (this.workspaceCountry !== 'FRANCE') return false;
    const duree = this.dureeContractuelleHeures();
    if (duree <= 0 || duree >= 35) return false;
    if (this.heuresComplementairesReelleMoyenneHeures() < 0) return false;
    if (this.ancienneteMois() < 0) return false;
    if (this.salaireMensuelContractuelEuros() < 0) return false;
    return true;
  }

  // --- Handlers — toute modification manuelle efface le badge IA du champ ---

  onDureeChange(value: number): void {
    this.dureeContractuelleHeures.set(value);
    this.provenanceDuree.set(null);
  }

  onMentionsDureeChange(value: boolean): void {
    this.mentionsDureePresentes.set(value);
    this.provenanceMentionsDuree.set(null);
  }

  onMentionsRepartitionChange(value: boolean): void {
    this.mentionsRepartitionPresentes.set(value);
    this.provenanceMentionsRepartition.set(null);
  }

  onMentionsHCChange(value: boolean): void {
    this.mentionsHCPresentes.set(value);
  }

  onHCMoyenneChange(value: number): void {
    this.heuresComplementairesReelleMoyenneHeures.set(value);
    this.provenanceHCMoyenne.set(null);
  }

  onModificationUnilateraleChange(value: boolean): void {
    this.modificationRepartitionUnilaterale.set(value);
  }

  onAncienneteChange(value: number): void {
    this.ancienneteMois.set(value);
  }

  onSalaireChange(value: number): void {
    this.salaireMensuelContractuelEuros.set(value);
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request: TempsPartielRequalificationRequest = {
      dureeContractuelleHeures: this.dureeContractuelleHeures(),
      mentionsDureePresentes: this.mentionsDureePresentes(),
      mentionsRepartitionPresentes: this.mentionsRepartitionPresentes(),
      mentionsHCPresentes: this.mentionsHCPresentes(),
      heuresComplementairesReelleMoyenneHeures: this.heuresComplementairesReelleMoyenneHeures(),
      modificationRepartitionUnilaterale: this.modificationRepartitionUnilaterale(),
      ancienneteMois: this.ancienneteMois(),
      salaireMensuelContractuelEuros: this.salaireMensuelContractuelEuros(),
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.applyResult(r);
        this.calculating.set(false);
        this.snackBar.open('Analyse de requalification temps partiel calculée', 'OK', { duration: 2500 });
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
        if (r) {
          this.applyResult(r);
        }
        this.loading.set(false);
        this.cdr.markForCheck();
      },
      error: () => {
        this.loading.set(false);
        this.cdr.markForCheck();
      },
    });
  }

  private applyResult(r: TempsPartielRequalificationResponse): void {
    this.result.set(r);
    this.hydrateForm(r);
    this.showForm.set(false);
  }

  /**
   * Ré-injecte le snapshot d'inputs de la réponse dans les champs du
   * formulaire — sans quoi un clic « Modifier » repartirait des valeurs
   * par défaut au lieu des dernières valeurs saisies.
   */
  private hydrateForm(r: TempsPartielRequalificationResponse): void {
    this.dureeContractuelleHeures.set(r.dureeContractuelleHeures ?? 24);
    this.mentionsDureePresentes.set(r.mentionsDureePresentes ?? true);
    this.mentionsRepartitionPresentes.set(r.mentionsRepartitionPresentes ?? true);
    this.mentionsHCPresentes.set(r.mentionsHCPresentes ?? true);
    this.heuresComplementairesReelleMoyenneHeures.set(r.heuresComplementairesReelleMoyenneHeures ?? 0);
    this.modificationRepartitionUnilaterale.set(r.modificationRepartitionUnilaterale ?? false);
    this.ancienneteMois.set(r.ancienneteMois ?? 0);
    this.salaireMensuelContractuelEuros.set(r.salaireMensuelContractuelEuros ?? 0);
    // Valeurs persistées = saisie avocat — jamais de badge IA.
    this.provenanceDuree.set(null);
    this.provenanceMentionsDuree.set(null);
    this.provenanceMentionsRepartition.set(null);
    this.provenanceHCMoyenne.set(null);
  }

  // ---------------------------------------------------------------------------
  // Helpers d'affichage du résultat
  // ---------------------------------------------------------------------------

  /**
   * Verdict → bannière de couleur. 3 niveaux :
   *  - REQUALIFICATION_PROBABLE → rouge
   *  - REQUALIFICATION_POSSIBLE → or
   *  - PAS_DE_REQUALIFICATION → vert
   */
  verdictBannerClass(v: TempsPartielAnalyseRequalification): string {
    switch (v) {
      case 'REQUALIFICATION_PROBABLE':
        return 'tpr-verdict-banner tpr-verdict-banner--probable';
      case 'REQUALIFICATION_POSSIBLE':
        return 'tpr-verdict-banner tpr-verdict-banner--possible';
      case 'PAS_DE_REQUALIFICATION':
        return 'tpr-verdict-banner tpr-verdict-banner--pas-de';
    }
  }

  verdictBannerLabel(v: TempsPartielAnalyseRequalification): string {
    switch (v) {
      case 'REQUALIFICATION_PROBABLE': return 'Requalification probable';
      case 'REQUALIFICATION_POSSIBLE': return 'Requalification possible';
      case 'PAS_DE_REQUALIFICATION': return 'Pas de requalification';
    }
  }

  verdictBannerIcon(v: TempsPartielAnalyseRequalification): string {
    switch (v) {
      case 'REQUALIFICATION_PROBABLE': return 'report_problem';
      case 'REQUALIFICATION_POSSIBLE': return 'help_outline';
      case 'PAS_DE_REQUALIFICATION': return 'verified';
    }
  }

  /** Libellé court d'un facteur (utilisé pour le tracking dans la liste). */
  facteurTrackKey(_index: number, f: TempsPartielFacteurRequalification): string {
    return `${f.code}:${f.libelle}`;
  }

  /** Format affichage du rappel — null si pas de requalification. */
  formatRappelSalaire(): string | null {
    const r = this.result();
    if (!r || r.rappelSalaire3AnsEstimeEuros == null) return null;
    return new Intl.NumberFormat('fr-FR', {
      style: 'currency',
      currency: 'EUR',
      maximumFractionDigits: 0,
    }).format(r.rappelSalaire3AnsEstimeEuros);
  }
}
