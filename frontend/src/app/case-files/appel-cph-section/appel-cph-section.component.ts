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
import { MatOptionModule } from '@angular/material/core';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { AppelCphService } from '../../core/services/appel-cph.service';
import {
  AppelCphRequest,
  AppelCphResponse,
  AppelCphStatut,
  ModeNotification,
  PartieAppelante,
  RepresentationConstituee,
} from '../../core/models/appel-cph.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { AppelCphSectionPrefillRules } from './appel-cph-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-218-02 : composant Angular standalone pour l'outil décisionnel
 * "Appel CPH devant la Cour d'appel" — tool_id canonique
 * `F-DT-86-appel-cph-cour-appel`. FRANCE uniquement (R. 1461-1 et s. CPC ;
 * art. 538 CPC — délai d'appel 1 mois ; art. 901 CPC — déclaration d'appel ;
 * art. 946 CPC — procédure orale ; R. 1461-2 CPC — représentation obligatoire).
 *
 * <p>F-218a — Procédure CPH avancée (P3 Travail FR).</p>
 *
 * <p>Pattern de référence : `conciliation-cph-bca-section` (F-DT-84,
 * SF-212-38) — formulaire CPH, POST de calcul, encadré verdict + checklist
 * formalités, refresh dashboard, OnPush + markForCheck dans les subscribe.</p>
 *
 * <p>Calculateur de délai (1 mois, art. 538 CPC) : verdict coloré
 * `DELAI_OUVERT` (vert) / `DELAI_URGENT` (or) / `DELAI_EXPIRE` (rouge) /
 * `VOIE_FERMEE` (navy, lien croisé pourvoi F-DT-87 si jugement en dernier
 * ressort). `dateEcheanceAppel` + `joursRestants` en JetBrains Mono.</p>
 *
 * <p>Bannière FR-only : si le workspace n'est pas en FRANCE, l'écran affiche
 * uniquement un avertissement pédagogique (la BE dispose d'un régime distinct
 * d'appel devant la cour du travail, Code judiciaire belge).</p>
 */
@Component({
  selector: 'app-appel-cph-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatSelectModule, MatOptionModule,
    MatCheckboxModule,
    MatProgressSpinnerModule,
    LegalCitationsPipe,
    ToolJurisprudenceCitationsComponent,
  ],
  templateUrl: './appel-cph-section.component.html',
  styleUrl: './appel-cph-section.component.scss',
})
export class AppelCphSectionComponent implements OnInit, OnChanges {
  // F-JU-03 — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-DT-86-appel-cph-cour-appel';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommée par le panel pour la card.
  static readonly TOOL_LABEL = "APPEL CPH — COUR D'APPEL";
  static readonly TOOL_ICON = 'gavel';

  /** Lien croisé vers l'outil pourvoi en cassation (R. 1462-1 CPC). */
  static readonly POURVOI_TOOL_ID = 'F-DT-87';

  /**
   * SF-218-02 — délégué au helper partagé (parité stricte avec
   * `prefillFromAi()`). Retourne le nombre exact de champs pré-remplissables
   * FR pour le badge tab. Couvre le seul champ `dateNotificationJugement`.
   */
  static getPrefillCount(input: {
    aiData?: TravailExtractedData | null;
    procedureChecks?: unknown[];
    aiQuestions?: unknown[];
    piecesManquantes?: unknown[];
    triggerEvents?: unknown[];
    workspaceCountry?: string;
  }): number {
    return AppelCphSectionPrefillRules.computePrefillCount({
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

  readonly POURVOI_TOOL_ID = AppelCphSectionComponent.POURVOI_TOOL_ID;

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<AppelCphResponse | null>(null);

  // --- Form fields (5 champs du contrat backend) ---
  dateNotificationJugement = signal<string>('');
  partieAppelante = signal<PartieAppelante>('SALARIE');
  modeNotification = signal<ModeNotification>('SIGNIFICATION');
  representationConstituee = signal<RepresentationConstituee>('AUCUNE');
  jugementEnDernierRessort = signal<boolean>(false);

  // Provenance IA par champ pré-rempli.
  provenanceDateNotification = signal<'IA' | null>(null);

  readonly partieOptions: { value: PartieAppelante; label: string }[] = [
    { value: 'SALARIE', label: 'Le salarié' },
    { value: 'EMPLOYEUR', label: "L'employeur" },
  ];

  readonly modeOptions: { value: ModeNotification; label: string }[] = [
    { value: 'SIGNIFICATION', label: 'Signification par huissier' },
    { value: 'LRAR', label: 'Lettre recommandée AR (greffe)' },
  ];

  readonly representationOptions: { value: RepresentationConstituee; label: string }[] = [
    { value: 'AVOCAT', label: 'Avocat' },
    { value: 'DEFENSEUR_SYNDICAL', label: 'Défenseur syndical' },
    { value: 'AUCUNE', label: 'Aucune (à constituer)' },
  ];

  constructor(
    private service: AppelCphService,
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
   * Pré-fill IA (SF-218-02) — renseigne `dateNotificationJugement` depuis
   * `TravailExtractedData`. Parité stricte avec `getPrefillCount()` : même
   * mapping, même gate `workspaceCountry === 'FRANCE'`.
   */
  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    const ai = this.aiData;
    if (!ai) return;
    const rules = AppelCphSectionPrefillRules;
    const ruleInput = { aiData: ai, workspaceCountry: this.workspaceCountry };

    const date = rules.computeDateNotificationJugement(ruleInput);
    if (date !== null) {
      this.dateNotificationJugement.set(date);
      this.provenanceDateNotification.set('IA');
    }
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  editMode(): void {
    this.showForm.set(true);
  }

  /** Form valide : FRANCE + date de notification renseignée et non future. */
  formValid(): boolean {
    if (this.workspaceCountry !== 'FRANCE') return false;
    const d = this.dateNotificationJugement();
    if (!d) return false;
    const ts = Date.parse(d);
    if (Number.isNaN(ts)) return false;
    // Notification future = invalide (le jugement ne peut être notifié à venir).
    if (ts > Date.now()) return false;
    return true;
  }

  // --- Handlers — toute modification manuelle efface le badge IA du champ ---

  onDateNotificationChange(value: string): void {
    this.dateNotificationJugement.set(value);
    this.provenanceDateNotification.set(null);
  }

  onPartieChange(value: PartieAppelante): void {
    this.partieAppelante.set(value);
  }

  onModeChange(value: ModeNotification): void {
    this.modeNotification.set(value);
  }

  onRepresentationChange(value: RepresentationConstituee): void {
    this.representationConstituee.set(value);
  }

  onDernierRessortChange(value: boolean): void {
    this.jugementEnDernierRessort.set(value);
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request: AppelCphRequest = {
      dateNotificationJugement: this.dateNotificationJugement(),
      partieAppelante: this.partieAppelante(),
      modeNotification: this.modeNotification(),
      representationConstituee: this.representationConstituee(),
      jugementEnDernierRessort: this.jugementEnDernierRessort(),
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.applyResult(r);
        this.calculating.set(false);
        this.snackBar.open("Analyse de l'appel CPH calculée", 'OK', { duration: 2500 });
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

  private applyResult(r: AppelCphResponse): void {
    this.result.set(r);
    this.hydrateForm(r);
    this.showForm.set(false);
  }

  /**
   * Ré-injecte le snapshot d'inputs de la réponse dans les champs du
   * formulaire — sans quoi un clic « Modifier » repartirait des valeurs par
   * défaut au lieu des dernières valeurs saisies.
   */
  private hydrateForm(r: AppelCphResponse): void {
    this.dateNotificationJugement.set(r.dateNotificationJugement ?? '');
    this.partieAppelante.set(r.partieAppelante ?? 'SALARIE');
    this.modeNotification.set(r.modeNotification ?? 'SIGNIFICATION');
    this.representationConstituee.set(r.representationConstituee ?? 'AUCUNE');
    this.jugementEnDernierRessort.set(r.jugementEnDernierRessort ?? false);
    // Valeurs persistées = saisie avocat — jamais de badge IA.
    this.provenanceDateNotification.set(null);
  }

  // ---------------------------------------------------------------------------
  // Helpers d'affichage
  // ---------------------------------------------------------------------------

  /** Classe CSS du chip verdict selon le statut. */
  statutChipClass(statut: AppelCphStatut | null | undefined): string {
    switch (statut) {
      case 'DELAI_OUVERT': return 'appel-chip-ouvert';
      case 'DELAI_URGENT': return 'appel-chip-urgent';
      case 'DELAI_EXPIRE': return 'appel-chip-expire';
      case 'VOIE_FERMEE': return 'appel-chip-fermee';
      default: return '';
    }
  }

  /** Libellé lisible du verdict. */
  statutLabel(statut: AppelCphStatut | null | undefined): string {
    switch (statut) {
      case 'DELAI_OUVERT': return "Délai d'appel ouvert";
      case 'DELAI_URGENT': return 'Délai urgent';
      case 'DELAI_EXPIRE': return "Délai d'appel expiré";
      case 'VOIE_FERMEE': return "Voie d'appel fermée (dernier ressort)";
      default: return '—';
    }
  }

  /** Icône Material du verdict. */
  statutIcon(statut: AppelCphStatut | null | undefined): string {
    switch (statut) {
      case 'DELAI_OUVERT': return 'check_circle';
      case 'DELAI_URGENT': return 'schedule';
      case 'DELAI_EXPIRE': return 'cancel';
      case 'VOIE_FERMEE': return 'block';
      default: return 'help_outline';
    }
  }

  /** True si l'outil pointe vers le pourvoi en cassation (voie d'appel fermée). */
  showPourvoiLink(): boolean {
    const r = this.result();
    if (!r) return false;
    return r.statut === 'VOIE_FERMEE' || r.renvoiPourvoiCassation === true;
  }

  /** Formatage d'une date ISO en libellé FR (jj/mm/aaaa). */
  formatDate(iso: string | null | undefined): string {
    if (!iso) return '—';
    const ts = Date.parse(iso);
    if (Number.isNaN(ts)) return '—';
    return new Intl.DateTimeFormat('fr-FR', {
      day: '2-digit', month: '2-digit', year: 'numeric',
    }).format(new Date(ts));
  }

  /** Track function pour la checklist des formalités. */
  checklistTrackKey(_index: number, item: { libelle: string }): string {
    return item.libelle;
  }
}
