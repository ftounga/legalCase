import {
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
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { AutoriteParentaleBeService } from '../../core/services/autorite-parentale-be.service';
import {
  AutoriteParentaleBeRequest,
  AutoriteParentaleBeResponse,
  AutoriteParentaleBeVerdict,
  ModeHebergementBe,
  VoieProceduraleApBe,
} from '../../core/models/autorite-parentale-be.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { AutoriteParentaleBeSectionPrefillRules } from './autorite-parentale-be-section-prefill-rules';

/** Option de mode d'hébergement (alignée sur l'enum backend `ModeHebergementBe`). */
interface ModeHebergementOption {
  value: ModeHebergementBe;
  label: string;
}

/**
 * SF-217-05 : composant Angular standalone pour l'outil décisionnel
 * "Autorité parentale (Belgique)" (`autorite-parentale-be`). BELGIQUE uniquement.
 *
 * Pattern de référence : `regime-communaute-legale-be-section` (F-217 SF-217-03).
 *
 * - Form : 7 toggles (filiation établie aux deux parents, accord parental,
 *   demande d'autorité exclusive, désintérêt durable, mise en danger,
 *   incapacité, décision judiciaire antérieure) + select mode d'hébergement.
 * - Verdict 4 niveaux : AUTORITE_CONJOINTE (navy) / AUTORITE_EXCLUSIVE_FONDEE
 *   (navy) / AUTORITE_EXCLUSIVE_NON_FONDEE (rouge) / QUALIFICATION_INCOMPLETE
 *   (or). Rouge réservé à AUTORITE_EXCLUSIVE_NON_FONDEE (situation défavorable
 *   au demandeur).
 * - Affichage : voie procédurale recommandée, liste des facteurs (libellé +
 *   fondement JetBrains Mono + explication), bases juridiques, messages.
 * - Pré-fill IA : V1 = 0 champ (`PREFILL_COUNT_ALWAYS_ZERO`).
 * - Refresh dashboard F-IA-02 + MatSnackBar erreurs.
 * - Gate `workspaceCountry === 'BELGIQUE'` — bannière info si mismatch.
 */
@Component({
  selector: 'app-autorite-parentale-be-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatSlideToggleModule, MatProgressSpinnerModule,
    LegalCitationsPipe,
  ],
  templateUrl: './autorite-parentale-be-section.component.html',
  styleUrl: './autorite-parentale-be-section.component.scss',
})
export class AutoriteParentaleBeSectionComponent implements OnInit, OnChanges {
  static readonly TOOL_LABEL = 'AUTORITÉ PARENTALE — CC ART. 374-375 BE';
  static readonly TOOL_ICON = 'family_restroom';

  /**
   * F-236 / F-237 — délègue au helper partagé.
   * V1 : aucun flag d'autorité parentale extrait par l'IA → toujours 0
   * (`PREFILL_COUNT_ALWAYS_ZERO`).
   */
  static readonly PREFILL_COUNT_ALWAYS_ZERO = true;

  static getPrefillCount(input: {
    aiData?: unknown;
    procedureChecks?: unknown[];
    aiQuestions?: unknown[];
    piecesManquantes?: unknown[];
    triggerEvents?: unknown[];
    workspaceCountry?: string;
  }): number {
    return AutoriteParentaleBeSectionPrefillRules.computePrefillCount({
      aiData: input.aiData,
    });
  }

  static readonly MODE_HEBERGEMENT_OPTIONS: readonly ModeHebergementOption[] = [
    { value: 'HEBERGEMENT_EGALITAIRE', label: 'Hébergement égalitaire (alterné)' },
    { value: 'HEBERGEMENT_PRINCIPAL_UN_PARENT', label: 'Hébergement principal chez un parent' },
    { value: 'HEBERGEMENT_NON_FIXE', label: 'Hébergement non encore fixé' },
  ];
  readonly modeHebergementOptions =
    AutoriteParentaleBeSectionComponent.MODE_HEBERGEMENT_OPTIONS;

  @Input() caseFileId!: string;
  /** F-177 — force l'expansion (mode modal). */
  @Input() forceExpanded = false;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'BELGIQUE';
  @Input() aiData?: FamilleExtractedData | null;
  /** Mode simulateur autonome (hors dossier) — coupe le refresh dashboard. */
  @Input() standaloneMode = false;

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<AutoriteParentaleBeResponse | null>(null);

  // --- Form fields ---
  filiationEtablieDeuxParents = signal<boolean>(true);
  accordParentalExiste = signal<boolean>(false);
  demandeAutoriteExclusive = signal<boolean>(false);
  desinteretDurableParent = signal<boolean>(false);
  miseEnDangerEnfant = signal<boolean>(false);
  incapaciteParent = signal<boolean>(false);
  decisionJudiciaireAnterieure = signal<boolean>(false);
  modeHebergementPrincipal = signal<ModeHebergementBe>('HEBERGEMENT_PRINCIPAL_UN_PARENT');
  commentaire = signal<string | null>(null);

  constructor(
    private service: AutoriteParentaleBeService,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef,
    @Optional() private dashboardRefresh: CaseDashboardRefreshService | null,
  ) {}

  ngOnInit(): void {
    if (this.forceExpanded) this.collapsed.set(false);
    this.prefillFromAi();
    if (this.workspaceCountry === 'BELGIQUE') {
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
   * Pré-fill IA — V1 : aucun flag d'autorité parentale dédié n'est extrait par
   * le pipeline (`PREFILL_COUNT_ALWAYS_ZERO`). No-op assumé et documenté.
   */
  private prefillFromAi(): void {
    // V1 : aucun champ pré-rempli — parité stricte avec getPrefillCount() = 0.
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  editMode(): void {
    this.showForm.set(true);
  }

  /** Form valide si le workspace est belge (tous les champs ont une valeur par défaut). */
  formValid(): boolean {
    return this.workspaceCountry === 'BELGIQUE';
  }

  // --- Handlers ---

  /** Désactiver la demande d'exclusive remet à zéro les motifs graves saisis. */
  onDemandeExclusiveChange(value: boolean): void {
    this.demandeAutoriteExclusive.set(value);
    if (!value) {
      this.desinteretDurableParent.set(false);
      this.miseEnDangerEnfant.set(false);
      this.incapaciteParent.set(false);
    }
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request: AutoriteParentaleBeRequest = {
      filiationEtablieDeuxParents: this.filiationEtablieDeuxParents(),
      accordParentalExiste: this.accordParentalExiste(),
      demandeAutoriteExclusive: this.demandeAutoriteExclusive(),
      desinteretDurableParent: this.desinteretDurableParent(),
      miseEnDangerEnfant: this.miseEnDangerEnfant(),
      incapaciteParent: this.incapaciteParent(),
      decisionJudiciaireAnterieure: this.decisionJudiciaireAnterieure(),
      modeHebergementPrincipal: this.modeHebergementPrincipal(),
      commentaire: this.commentaire()?.trim() || null,
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.applyResult(r);
        this.calculating.set(false);
        this.snackBar.open('Analyse de l\'autorité parentale calculée', 'OK', { duration: 2500 });
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

  private applyResult(r: AutoriteParentaleBeResponse): void {
    this.result.set(r);
    this.hydrateForm(r);
    this.showForm.set(false);
  }

  /**
   * Ré-injecte le snapshot d'inputs de la réponse dans les champs du
   * formulaire — sans quoi un clic « Modifier » repartirait des valeurs par
   * défaut au lieu des dernières valeurs saisies (leçon F-DT-36).
   */
  private hydrateForm(r: AutoriteParentaleBeResponse): void {
    this.filiationEtablieDeuxParents.set(r.filiationEtablieDeuxParents);
    this.accordParentalExiste.set(r.accordParentalExiste);
    this.demandeAutoriteExclusive.set(r.demandeAutoriteExclusive);
    this.desinteretDurableParent.set(r.desinteretDurableParent);
    this.miseEnDangerEnfant.set(r.miseEnDangerEnfant);
    this.incapaciteParent.set(r.incapaciteParent);
    this.decisionJudiciaireAnterieure.set(r.decisionJudiciaireAnterieure);
    this.modeHebergementPrincipal.set(r.modeHebergementPrincipal);
    this.commentaire.set(r.commentaire);
  }

  // ---------------------------------------------------------------------------
  // Helpers d'affichage du résultat
  // ---------------------------------------------------------------------------

  /** Rouge réservé à AUTORITE_EXCLUSIVE_NON_FONDEE ; or pour incomplet ; navy sinon. */
  verdictBannerClass(verdict: AutoriteParentaleBeVerdict): string {
    switch (verdict) {
      case 'AUTORITE_CONJOINTE':
        return 'ap-verdict-banner ap-verdict-banner--available';
      case 'AUTORITE_EXCLUSIVE_FONDEE':
        return 'ap-verdict-banner ap-verdict-banner--available';
      case 'AUTORITE_EXCLUSIVE_NON_FONDEE':
        return 'ap-verdict-banner ap-verdict-banner--danger';
      case 'QUALIFICATION_INCOMPLETE':
        return 'ap-verdict-banner ap-verdict-banner--medium';
    }
  }

  verdictBannerLabel(verdict: AutoriteParentaleBeVerdict): string {
    switch (verdict) {
      case 'AUTORITE_CONJOINTE': return 'Autorité parentale conjointe';
      case 'AUTORITE_EXCLUSIVE_FONDEE': return 'Autorité exclusive — demande fondée';
      case 'AUTORITE_EXCLUSIVE_NON_FONDEE': return 'Autorité exclusive — demande non fondée';
      case 'QUALIFICATION_INCOMPLETE': return 'Qualification incomplète';
    }
  }

  verdictBannerIcon(verdict: AutoriteParentaleBeVerdict): string {
    switch (verdict) {
      case 'AUTORITE_CONJOINTE': return 'check_circle';
      case 'AUTORITE_EXCLUSIVE_FONDEE': return 'check_circle';
      case 'AUTORITE_EXCLUSIVE_NON_FONDEE': return 'error';
      case 'QUALIFICATION_INCOMPLETE': return 'warning';
    }
  }

  voieProceduraleLabel(voie: VoieProceduraleApBe): string {
    switch (voie) {
      case 'AUCUNE_AUTORITE_CONJOINTE_DROIT':
        return 'Autorité conjointe de plein droit — aucune saisine nécessaire';
      case 'ACCORD_HOMOLOGUE_TF':
        return 'Faire homologuer l\'accord parental par le Tribunal de la famille';
      case 'REQUETE_TRIBUNAL_FAMILLE':
        return 'Saisine du Tribunal de la famille par requête';
      case 'ETABLISSEMENT_FILIATION_PREALABLE':
        return 'Établir la filiation à l\'égard des deux parents au préalable';
    }
  }
}
