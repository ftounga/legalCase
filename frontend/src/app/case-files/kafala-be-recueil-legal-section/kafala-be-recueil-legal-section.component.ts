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
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { KafalaBeRecueilLegalService } from '../../core/services/kafala-be-recueil-legal.service';
import {
  KafalaBeRequest,
  KafalaBeResponse,
  KafalaBeVerdict,
  ObjectifEnvisage,
} from '../../core/models/kafala-be-recueil-legal.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';
import { KafalaBeRecueilLegalSectionPrefillRules } from './kafala-be-recueil-legal-section-prefill-rules';

interface ObjectifOption { value: ObjectifEnvisage; label: string; }

/**
 * SF-223-03 : composant Angular standalone pour l'outil décisionnel "Recueil
 * légal (kafala) — Belgique" (`kafala-be-recueil-legal`). BELGIQUE uniquement
 * (CDIP, loi du 16/07/2004 ; CC art. 343 al. 2 nouveau — à vérifier).
 *
 * 1 outil = 1 situation « sort d'une kafala étrangère en Belgique ». La kafala
 * n'est PAS une adoption (aucun lien de filiation) → DISTINCT de `adoption-be`.
 *
 * Pré-fill IA F-246 : pays d'origine + date de l'acte pré-remplis si
 * factualisables (sous-objet `kafala_be_detection`). Aucune citation
 * jurisprudentielle BE (F-JU-04 parké). OnPush + ChangeDetectorRef.markForCheck()
 * dans next/error des subscribe (mémoire `feedback_onpush_subscribe_markforcheck`).
 */
@Component({
  selector: 'app-kafala-be-recueil-legal-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatSlideToggleModule, MatProgressSpinnerModule,
    LegalCitationsPipe,
    ToolJurisprudenceCitationsComponent,
  ],
  templateUrl: './kafala-be-recueil-legal-section.component.html',
  styleUrl: './kafala-be-recueil-legal-section.component.scss',
})
export class KafalaBeRecueilLegalSectionComponent implements OnInit, OnChanges {
  // F-JU-03 — citations jurisprudentielles F-JU-01 (BE parké F-JU-04 : aucune en V1).
  protected readonly toolIdForJurisprudence = 'kafala-be-recueil-legal';
  protected readonly brancheActiveForJurisprudence = 'default';

  static readonly TOOL_LABEL = 'KAFALA — RECUEIL LÉGAL (BELGIQUE)';
  static readonly TOOL_ICON = 'volunteer_activism';

  /**
   * SF-223-03 — pré-fill F-246 : pays d'origine + date de l'acte si
   * factualisables. Délègue au helper partagé (parité runtime/static —
   * garde-fou `prefill-count-integrity`).
   */
  static getPrefillCount(input: {
    aiData?: unknown;
    procedureChecks?: unknown[];
    aiQuestions?: unknown[];
    piecesManquantes?: unknown[];
    triggerEvents?: unknown[];
    workspaceCountry?: string;
  }): number {
    return KafalaBeRecueilLegalSectionPrefillRules.computePrefillCount(input);
  }

  static readonly OBJECTIF_OPTIONS: readonly ObjectifOption[] = [
    { value: 'RECUEIL_PROTECTION', label: 'Recueil / protection de l\'enfant (reconnaissance de la mesure)' },
    { value: 'TENTATIVE_ADOPTION', label: 'Adoption envisagée à partir de la kafala' },
    { value: 'SEJOUR', label: 'Séjour de l\'enfant en Belgique' },
    { value: 'SUCCESSION', label: 'Effets successoraux' },
  ];
  readonly objectifOptions = KafalaBeRecueilLegalSectionComponent.OBJECTIF_OPTIONS;

  @Input() caseFileId!: string;
  /** F-177 — force l'expansion (mode modal). */
  @Input() forceExpanded = false;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'BELGIQUE';
  @Input() aiData?: FamilleExtractedData | null;
  @Input() standaloneMode = false;

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<KafalaBeResponse | null>(null);

  // --- Form fields ---
  paysOrigineKafala = signal<string | null>(null);
  dateActeKafala = signal<string | null>(null);
  acteOfficielJudiciaireOuAdoul = signal<boolean>(false);
  enfantAbandonneOuOrphelin = signal<boolean>(false);
  kafilDomicilieBelgique = signal<boolean>(false);
  consentementParentsBiologiquesOuAutorite = signal<boolean>(false);
  objectifEnvisage = signal<ObjectifEnvisage | null>(null);
  commentaire = signal<string | null>(null);

  constructor(
    private service: KafalaBeRecueilLegalService,
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
   * F-246 — pré-remplit le pays d'origine (ISO-2) et la date de l'acte depuis
   * le sous-objet IA `kafala_be_detection` (clés camelCase plates,
   * @JsonUnwrapped). Aucun autre champ n'est factualisable de manière stable
   * en V1.
   */
  private prefillFromAi(): void {
    if (this.workspaceCountry !== 'BELGIQUE') {
      this.cdr.markForCheck();
      return;
    }
    const ai = this.aiData as (FamilleExtractedData & {
      kafalaPaysOrigineDetecte?: string | null;
      kafalaDateActeDetectee?: string | null;
    }) | null | undefined;
    if (!ai) {
      this.cdr.markForCheck();
      return;
    }
    const pays = ai.kafalaPaysOrigineDetecte;
    if (typeof pays === 'string' && /^[A-Z]{2}$/.test(pays)) {
      this.paysOrigineKafala.set(pays);
    }
    const date = ai.kafalaDateActeDetectee;
    if (typeof date === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(date)) {
      this.dateActeKafala.set(date);
    }
    this.cdr.markForCheck();
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  editMode(): void {
    this.showForm.set(true);
  }

  /** Form valide : workspace BE + objectif renseigné. */
  formValid(): boolean {
    if (this.workspaceCountry !== 'BELGIQUE') return false;
    if (!this.objectifEnvisage()) return false;
    return true;
  }

  calculate(): void {
    if (!this.formValid()) return;
    const pays = this.paysOrigineKafala()?.trim().toUpperCase() || null;
    const request: KafalaBeRequest = {
      paysOrigineKafala: pays,
      dateActeKafala: this.dateActeKafala() || null,
      acteOfficielJudiciaireOuAdoul: this.acteOfficielJudiciaireOuAdoul(),
      enfantAbandonneOuOrphelin: this.enfantAbandonneOuOrphelin(),
      kafilDomicilieBelgique: this.kafilDomicilieBelgique(),
      consentementParentsBiologiquesOuAutorite: this.consentementParentsBiologiquesOuAutorite(),
      objectifEnvisage: this.objectifEnvisage()!,
      commentaire: this.commentaire()?.trim() || null,
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.applyResult(r);
        this.calculating.set(false);
        this.snackBar.open('Analyse du recueil légal (kafala) calculée', 'OK', { duration: 2500 });
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

  private applyResult(r: KafalaBeResponse): void {
    this.result.set(r);
    this.hydrateForm(r);
    this.showForm.set(false);
  }

  private hydrateForm(r: KafalaBeResponse): void {
    this.paysOrigineKafala.set(r.paysOrigineKafala ?? null);
    this.dateActeKafala.set(r.dateActeKafala ?? null);
    this.acteOfficielJudiciaireOuAdoul.set(r.acteOfficielJudiciaireOuAdoul ?? false);
    this.enfantAbandonneOuOrphelin.set(r.enfantAbandonneOuOrphelin ?? false);
    this.kafilDomicilieBelgique.set(r.kafilDomicilieBelgique ?? false);
    this.consentementParentsBiologiquesOuAutorite.set(r.consentementParentsBiologiquesOuAutorite ?? false);
    this.objectifEnvisage.set(r.objectifEnvisage);
    this.commentaire.set(r.commentaire);
  }

  // ---------------------------------------------------------------------------
  // Helpers d'affichage du résultat
  // ---------------------------------------------------------------------------

  /**
   * Vert pour RECUEIL_RECONNU_COMME_PROTECTION. Or pour
   * RECONNAISSANCE_SOUS_CONDITIONS. Rouge réservé à EFFET_ADOPTIF_EXCLU
   * (blocage de la voie adoptive). Navy info pour QUALIFICATION_INCOMPLETE.
   */
  verdictBannerClass(verdict: KafalaBeVerdict): string {
    switch (verdict) {
      case 'RECUEIL_RECONNU_COMME_PROTECTION':
        return 'kaf-verdict-banner kaf-verdict-banner--ok';
      case 'RECONNAISSANCE_SOUS_CONDITIONS':
        return 'kaf-verdict-banner kaf-verdict-banner--warn';
      case 'EFFET_ADOPTIF_EXCLU':
        return 'kaf-verdict-banner kaf-verdict-banner--danger';
      case 'QUALIFICATION_INCOMPLETE':
        return 'kaf-verdict-banner kaf-verdict-banner--info';
    }
  }

  verdictBannerLabel(verdict: KafalaBeVerdict): string {
    switch (verdict) {
      case 'RECUEIL_RECONNU_COMME_PROTECTION': return 'Recueil reconnu (mesure de protection)';
      case 'RECONNAISSANCE_SOUS_CONDITIONS': return 'Reconnaissance sous conditions';
      case 'EFFET_ADOPTIF_EXCLU': return 'Effet adoptif exclu';
      case 'QUALIFICATION_INCOMPLETE': return 'Qualification incomplète';
    }
  }

  verdictBannerIcon(verdict: KafalaBeVerdict): string {
    switch (verdict) {
      case 'RECUEIL_RECONNU_COMME_PROTECTION': return 'check_circle';
      case 'RECONNAISSANCE_SOUS_CONDITIONS': return 'rule';
      case 'EFFET_ADOPTIF_EXCLU': return 'error';
      case 'QUALIFICATION_INCOMPLETE': return 'info';
    }
  }

  objectifLabel(o: ObjectifEnvisage): string {
    return this.objectifOptions.find(opt => opt.value === o)?.label ?? o;
  }

  /** Titre de la liste motifs/conditions selon le verdict. */
  motifsTitle(verdict: KafalaBeVerdict): string {
    return verdict === 'EFFET_ADOPTIF_EXCLU'
      ? 'Motifs d\'exclusion de l\'effet adoptif'
      : 'Conditions à satisfaire';
  }
}
