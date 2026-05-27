import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  Input,
  OnChanges,
  OnInit,
  Optional,
  SimpleChanges,
  computed,
  inject,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatSelectModule } from '@angular/material/select';
import { MatListModule } from '@angular/material/list';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import {
  PieceManquanteEntry,
  TravailExtractedData,
} from '../../core/models/case-analysis.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { AiQuestion } from '../../core/models/ai-question.model';
import {
  HarcelementBeChecklistItem,
  HarcelementBeChecklistStatut,
  HarcelementBeEtape,
  HarcelementBeProcedureFormelleRequest,
  HarcelementBeProcedureFormelleResponse,
  HarcelementBeTailleEntreprise,
  HarcelementBeType,
} from '../../core/models/harcelement-be-procedure-formelle.model';
import {
  HarcelementBeProcedureFormelleService,
} from '../../core/services/harcelement-be-procedure-formelle.service';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import {
  HarcelementBeProcedureFormelleSectionPrefillRules,
} from './harcelement-be-procedure-formelle-section-prefill-rules';
import {
  ToolJurisprudenceCitationsComponent,
} from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-213-07b : outil décisionnel « Harcèlement BE — procédure formelle » (F-213).
 *
 * <p>BE uniquement — Loi du 4 août 1996 art. 32bis–32sexies + AR du
 * 10 avril 2014 (prévention des risques psychosociaux au travail).</p>
 *
 * <p>Affiché par le panel F-IA-04 (tool_id
 * {@code harcelement-be-procedure-formelle}, visibility ALWAYS_ON priority
 * 115 BE / DROIT_DU_TRAVAIL — juste au-dessus de
 * {@code transaction-be-travail} (114). Pattern miroir des autres outils
 * F-213 vague 1-6.</p>
 *
 * <p><b>Pas de verdict 4 états</b> — l'outil est une <b>checklist
 * procédurale</b> qui produit :
 * <ul>
 *   <li>une liste d'items contextuels (action / état) selon l'étape ;</li>
 *   <li>un drapeau {@code representaillesPossibles} → badge rouge si vrai ;</li>
 *   <li>une fenêtre de protection 12 mois (art. 32sexies) ;</li>
 *   <li>une date fatale (typiquement enquête 90 j) — surlignée rouge si
 *       ≤ 30 jours, ambre si ≤ 90 jours ;</li>
 *   <li>un avertissement optionnel (CPAP absent, fenêtre 12 mois dépassée…).</li>
 * </ul></p>
 *
 * <p><b>Pré-fill IA V1</b> : aucun champ — l'analyse contextuelle de la
 * procédure de plainte (type de harcèlement, étape procédurale, taille
 * d'entreprise, CPAP…) n'est pas extraite par le pipeline IA Travail BE
 * actuel (cf. helper doc). Saisie avocat des champs.</p>
 *
 * <p>Distinct de {@code F-DT-11} (qui couvre la nullité du licenciement
 * représailles BE) — cet outil intervient <i>avant</i> toute rupture pour
 * guider la procédure interne ; F-DT-11 intervient <i>après</i> pour
 * sanctionner la rupture.</p>
 */
@Component({
  selector: 'app-harcelement-be-procedure-formelle-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatCheckboxModule, MatSelectModule,
    MatListModule,
    MatProgressSpinnerModule,
    ToolJurisprudenceCitationsComponent,
  ],
  templateUrl: './harcelement-be-procedure-formelle-section.component.html',
  styleUrl: './harcelement-be-procedure-formelle-section.component.scss',
})
export class HarcelementBeProcedureFormelleSectionComponent
  implements OnInit, OnChanges {

  // F-JU-03 — citations jurisprudentielles F-JU-01 (BE).
  protected readonly toolIdForJurisprudence = 'harcelement-be-procedure-formelle';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'HARCÈLEMENT PROCÉDURE FORMELLE (BE)';
  static readonly TOOL_ICON = 'gavel';

  /** F-177 SF-177-12 / F-236 SF-236-02 — délègue au helper partagé (parité runtime). */
  static getPrefillCount(input: PrefillCountInput): number {
    return HarcelementBeProcedureFormelleSectionPrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() forceExpanded = false;
  /** Gate strict BE — pas d'équivalent FR (référent harcèlement / médecin du travail FR). */
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'BELGIQUE';
  @Input() aiData?: TravailExtractedData | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;

  private cdr = inject(ChangeDetectorRef);

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<HarcelementBeProcedureFormelleResponse | null>(null);

  // ── Champs du form (signals — édités via handlers). ────────────────────
  typeHarcelement = signal<HarcelementBeType | null>(null);
  etapeProcedure = signal<HarcelementBeEtape | null>(null);
  dateDepotPlainte = signal<string | null>(null);
  entreprisePossedeCPAP = signal<boolean>(false);
  entrepriseTaille = signal<HarcelementBeTailleEntreprise | null>(null);
  mesureDefavorableApres = signal<boolean>(false);
  delaiDepuisDepotJours = signal<number | null>(null);

  /** Gate strict workspace BE. */
  isAvailable = computed(() => this.workspaceCountry === 'BELGIQUE');

  /** Affichage conditionnel : champ dateDepotPlainte requis si étape != AVANT_DEPOT. */
  dateDepotRequired = computed(() => {
    const etape = this.etapeProcedure();
    return etape !== null && etape !== 'AVANT_DEPOT';
  });

  constructor(
    private service: HarcelementBeProcedureFormelleService,
    private snackBar: MatSnackBar,
    @Optional() private dashboardRefresh: CaseDashboardRefreshService | null,
  ) {}

  ngOnInit(): void {
    if (this.forceExpanded) this.collapsed.set(false);
    if (this.workspaceCountry === 'BELGIQUE') {
      this.load();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['forceExpanded'] && this.forceExpanded) this.collapsed.set(false);
    // SF-213-07b V1 : pas de pré-fill IA (cf. helper doc) — pas de réaction aiData.
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  formValid(): boolean {
    if (this.typeHarcelement() === null) return false;
    if (this.etapeProcedure() === null) return false;
    if (this.entrepriseTaille() === null) return false;
    // dateDepotPlainte obligatoire si étape != AVANT_DEPOT
    if (this.dateDepotRequired() && !this.dateDepotPlainte()) return false;
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // -- Handlers --
  onTypeHarcelementChange(value: HarcelementBeType): void {
    this.typeHarcelement.set(value);
  }
  onEtapeProcedureChange(value: HarcelementBeEtape): void {
    this.etapeProcedure.set(value);
    // Si AVANT_DEPOT, on efface la date pour cohérence (sera ignorée backend).
    if (value === 'AVANT_DEPOT') {
      this.dateDepotPlainte.set(null);
    }
  }
  onDateDepotPlainteChange(value: string | null): void {
    this.dateDepotPlainte.set(value || null);
  }
  onEntreprisePossedeCPAPChange(value: boolean): void {
    this.entreprisePossedeCPAP.set(value);
  }
  onEntrepriseTailleChange(value: HarcelementBeTailleEntreprise): void {
    this.entrepriseTaille.set(value);
  }
  onMesureDefavorableApresChange(value: boolean): void {
    this.mesureDefavorableApres.set(value);
  }
  onDelaiDepuisDepotJoursChange(value: number | null): void {
    this.delaiDepuisDepotJours.set(value);
  }

  // -- Libellés humains --
  typeHarcelementLabel(value: HarcelementBeType | null): string {
    switch (value) {
      case 'MORAL': return 'Moral';
      case 'SEXUEL': return 'Sexuel';
      case 'LES_DEUX': return 'Moral + sexuel';
      default: return '—';
    }
  }

  etapeProcedureLabel(value: HarcelementBeEtape | null): string {
    switch (value) {
      case 'AVANT_DEPOT': return 'Avant dépôt — orientation';
      case 'DEMANDE_INFORMELLE_EN_COURS': return 'Demande informelle en cours';
      case 'DEMANDE_FORMELLE_EN_COURS': return 'Demande formelle — enquête CPAP';
      case 'ENQUETE_TERMINEE': return 'Enquête terminée';
      case 'MESURE_DEFAVORABLE_APRES_PLAINTE': return 'Mesure défavorable post-plainte';
      default: return '—';
    }
  }

  entrepriseTailleLabel(value: HarcelementBeTailleEntreprise | null): string {
    switch (value) {
      case 'MOINS_DE_50': return '< 50 travailleurs';
      case 'CINQUANTE_ET_PLUS': return '≥ 50 travailleurs';
      default: return '—';
    }
  }

  /** Icône Material associée au statut d'un item de checklist. */
  statutIcon(statut: HarcelementBeChecklistStatut): string {
    switch (statut) {
      case 'A_FAIRE': return 'pending';
      case 'EN_COURS': return 'autorenew';
      case 'TERMINE': return 'check_circle';
      case 'ACTIF': return 'shield';
      default: return 'help';
    }
  }

  /** Classe CSS de l'item de checklist selon son statut. */
  statutClass(statut: HarcelementBeChecklistStatut): string {
    switch (statut) {
      case 'A_FAIRE': return 'statut-a-faire';
      case 'EN_COURS': return 'statut-en-cours';
      case 'TERMINE': return 'statut-termine';
      case 'ACTIF': return 'statut-actif';
      default: return '';
    }
  }

  /** Libellé humain du statut. */
  statutLabel(statut: HarcelementBeChecklistStatut): string {
    switch (statut) {
      case 'A_FAIRE': return 'À faire';
      case 'EN_COURS': return 'En cours';
      case 'TERMINE': return 'Terminé';
      case 'ACTIF': return 'Actif';
      default: return '—';
    }
  }

  /**
   * Nombre de jours restant avant la {@code prochainDelaiFatal}. Null si
   * pas de délai. Négatif si déjà dépassé.
   */
  joursRestantsDelaiFatal(): number | null {
    const r = this.result();
    if (!r || !r.prochainDelaiFatal) return null;
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const echeance = new Date(r.prochainDelaiFatal);
    if (Number.isNaN(echeance.getTime())) return null;
    echeance.setHours(0, 0, 0, 0);
    const diffMs = echeance.getTime() - today.getTime();
    return Math.round(diffMs / (1000 * 60 * 60 * 24));
  }

  /**
   * Classe CSS du surlignage de la prochaine date fatale :
   * - rouge bold si ≤ 30 jours (ou dépassé)
   * - ambre si ≤ 90 jours
   * - normal sinon
   */
  delaiFatalClass(): string {
    const j = this.joursRestantsDelaiFatal();
    if (j === null) return '';
    if (j <= 30) return 'delai-critical';
    if (j <= 90) return 'delai-warn';
    return 'delai-ok';
  }

  /** Vrai si une checklist non vide est présente dans le résultat. */
  hasChecklist(): boolean {
    const r = this.result();
    return !!r && Array.isArray(r.checklistItems) && r.checklistItems.length > 0;
  }

  /** Helper template — items typés pour @for. */
  checklistItems(): HarcelementBeChecklistItem[] {
    return this.result()?.checklistItems ?? [];
  }

  calculate(): void {
    if (!this.formValid()) return;
    if (this.workspaceCountry !== 'BELGIQUE') {
      this.snackBar.open('Outil indisponible pour ce workspace', 'Fermer',
        { duration: 4000, panelClass: 'snack-error' });
      return;
    }
    const request: HarcelementBeProcedureFormelleRequest = {
      typeHarcelement: this.typeHarcelement()!,
      etapeProcedure: this.etapeProcedure()!,
      dateDepotPlainte: this.dateDepotPlainte() ?? null,
      entreprisePossedeCPAP: this.entreprisePossedeCPAP(),
      entrepriseTaille: this.entrepriseTaille()!,
      mesureDefavorableApres: this.mesureDefavorableApres(),
      delaiDepuisDepotJours: this.delaiDepuisDepotJours() ?? null,
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Procédure harcèlement BE analysée', 'OK', { duration: 2500 });
        this.dashboardRefresh?.triggerRefresh();
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.calculating.set(false);
        const status = err?.status;
        let msg: string;
        if (status === 404) {
          msg = 'Dossier introuvable';
        } else if (status === 400) {
          msg = err?.error?.message || err?.error || 'Données saisies invalides';
        } else {
          msg = 'Une erreur est survenue. Veuillez réessayer.';
        }
        this.snackBar.open(String(msg), 'Fermer',
          { duration: 5000, panelClass: 'snack-error' });
        this.cdr.markForCheck();
      },
    });
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.loading.set(false);
        // Rehydratation form pour permettre l'édition.
        this.typeHarcelement.set(r.typeHarcelement);
        this.etapeProcedure.set(r.etapeProcedure);
        this.dateDepotPlainte.set(r.dateDepotPlainte);
        this.entreprisePossedeCPAP.set(r.entreprisePossedeCPAP);
        this.entrepriseTaille.set(r.entrepriseTaille);
        this.mesureDefavorableApres.set(r.mesureDefavorableApres);
        this.delaiDepuisDepotJours.set(r.delaiDepuisDepotJours);
        this.cdr.markForCheck();
      },
      error: () => {
        // 404 attendu si aucune analyse — mode formulaire vierge.
        this.loading.set(false);
        this.cdr.markForCheck();
      },
    });
  }
}
