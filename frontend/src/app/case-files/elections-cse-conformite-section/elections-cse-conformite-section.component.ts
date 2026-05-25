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
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ElectionsCseConformiteService } from '../../core/services/elections-cse-conformite.service';
import {
  ElectionsCseAnalyse,
  ElectionsCsePointIrregularite,
  ElectionsCseConformiteRequest,
  ElectionsCseConformiteResponse,
} from '../../core/models/elections-cse-conformite.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { ElectionsCseConformiteSectionPrefillRules } from './elections-cse-conformite-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-212-32 : composant Angular standalone pour l'outil décisionnel
 * "Élections CSE — conformité procédure" — tool_id canonique
 * `F-DT-65-elections-cse-conformite`. FRANCE uniquement (L. 2314-1 à
 * L. 2314-37 CT ; ordonnance n°2017-1386 du 22/09/2017 instituant le CSE).
 *
 * Pattern de référence : `burnout-reconnaissance-mp-section` (F-DT-64,
 * SF-212-28) — formulaire, POST de calcul, verdict 3 niveaux, refresh
 * dashboard, OnPush + markForCheck dans les subscribe.
 *
 * Verdict 3 niveaux :
 *  - CONFORME (vert — procédure régulière)
 *  - IRREGULARITE_MINEURE (or — formalité accessoire, à corriger ou à
 *    invoquer si influence sur le scrutin)
 *  - IRREGULARITE_MAJEURE (rouge — PAP absent, collèges non conformes —
 *    fondement d'une annulation)
 *
 * Délai de contestation : <b>15 jours</b> à compter de l'élection
 * (L. 2314-32 CT) — affiché en rouge si approche (alerteDelaiContestation).
 *
 * Bannière FR-only : si le workspace n'est pas en FRANCE, l'écran affiche
 * uniquement un avertissement pédagogique (la BE dispose d'un régime
 * distinct conseil d'entreprise + délégation syndicale, loi du 4/8/1996).
 */
@Component({
  selector: 'app-elections-cse-conformite-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatSlideToggleModule,
    MatProgressSpinnerModule,
    MatDatepickerModule, MatNativeDateModule,
    LegalCitationsPipe,
    ToolJurisprudenceCitationsComponent,
  ],
  templateUrl: './elections-cse-conformite-section.component.html',
  styleUrl: './elections-cse-conformite-section.component.scss',
})
export class ElectionsCseConformiteSectionComponent implements OnInit, OnChanges {
  // F-JU-03 — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-DT-65-elections-cse-conformite';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommée par le panel pour la card.
  static readonly TOOL_LABEL = 'ÉLECTIONS CSE — CONFORMITÉ';
  static readonly TOOL_ICON = 'how_to_vote';

  /**
   * SF-244 — délégué au helper partagé (parité stricte avec `prefillFromAi()`).
   * Retourne le nombre exact de champs pré-remplissables FR pour le badge tab.
   * Couvre les 4 champs `electionCse*`.
   */
  static getPrefillCount(input: {
    aiData?: TravailExtractedData | null;
    procedureChecks?: unknown[];
    aiQuestions?: unknown[];
    piecesManquantes?: unknown[];
    triggerEvents?: unknown[];
    workspaceCountry?: string;
  }): number {
    return ElectionsCseConformiteSectionPrefillRules.computePrefillCount({
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
  result = signal<ElectionsCseConformiteResponse | null>(null);

  // --- Form fields (7 champs du contrat backend) ---
  effectifEntreprise = signal<number>(11);
  papNegocieAvecOS = signal<boolean>(false);
  delaiInvitationOSRespectee = signal<boolean>(false);
  collegesConformes = signal<boolean>(false);
  resultatsContestes = signal<boolean>(false);
  /** Date d'élection (LocalDate côté backend, Date côté Material datepicker). */
  dateElection = signal<Date | null>(null);
  motifContestation = signal<string>('');

  // Provenance IA par champ pré-rempli.
  provenanceDateElection = signal<'IA' | null>(null);
  provenancePapNegocie = signal<'IA' | null>(null);
  provenanceColleges = signal<'IA' | null>(null);
  provenanceResultatsContestes = signal<'IA' | null>(null);

  constructor(
    private service: ElectionsCseConformiteService,
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
   * Pré-fill IA (SF-212-32) — renseigne les 4 champs depuis le sous-objet
   * `elections_cse_detail` (projeté à plat dans `travailExtractedData` via
   * Jackson @JsonUnwrapped côté backend). Parité stricte avec
   * `getPrefillCount()` : mêmes mappings, même gate `workspaceCountry === 'FRANCE'`.
   */
  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    const ai = this.aiData;
    if (!ai) return;
    const rules = ElectionsCseConformiteSectionPrefillRules;
    const ruleInput = { aiData: ai, workspaceCountry: this.workspaceCountry };

    const dateIso = rules.computeDateElection(ruleInput);
    if (dateIso !== null) {
      // ISO YYYY-MM-DD → Date locale (interprétation UTC pour stabilité).
      const d = new Date(dateIso + 'T00:00:00Z');
      if (!isNaN(d.getTime())) {
        this.dateElection.set(d);
        this.provenanceDateElection.set('IA');
      }
    }
    const pap = rules.computePapNegocie(ruleInput);
    if (pap !== null) {
      this.papNegocieAvecOS.set(pap);
      this.provenancePapNegocie.set('IA');
    }
    const colleges = rules.computeCollegesConformes(ruleInput);
    if (colleges !== null) {
      this.collegesConformes.set(colleges);
      this.provenanceColleges.set('IA');
    }
    const contestes = rules.computeResultatsContestes(ruleInput);
    if (contestes !== null) {
      this.resultatsContestes.set(contestes);
      this.provenanceResultatsContestes.set('IA');
    }
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  editMode(): void {
    this.showForm.set(true);
  }

  /** Form valide : FRANCE + effectif >= 1. */
  formValid(): boolean {
    if (this.workspaceCountry !== 'FRANCE') return false;
    if (this.effectifEntreprise() < 1) return false;
    return true;
  }

  // --- Handlers — toute modification manuelle efface le badge IA du champ ---

  onEffectifChange(value: number): void {
    this.effectifEntreprise.set(value);
  }

  onPapChange(value: boolean): void {
    this.papNegocieAvecOS.set(value);
    this.provenancePapNegocie.set(null);
  }

  onDelaiInvitationChange(value: boolean): void {
    this.delaiInvitationOSRespectee.set(value);
  }

  onCollegesChange(value: boolean): void {
    this.collegesConformes.set(value);
    this.provenanceColleges.set(null);
  }

  onResultatsContestesChange(value: boolean): void {
    this.resultatsContestes.set(value);
    this.provenanceResultatsContestes.set(null);
  }

  onDateElectionChange(value: Date | null): void {
    this.dateElection.set(value);
    this.provenanceDateElection.set(null);
  }

  onMotifChange(value: string): void {
    this.motifContestation.set(value);
  }

  /** Sérialise la Date locale en ISO YYYY-MM-DD (LocalDate côté backend). */
  private toIsoDate(d: Date | null): string | null {
    if (!d) return null;
    const yyyy = d.getFullYear();
    const mm = String(d.getMonth() + 1).padStart(2, '0');
    const dd = String(d.getDate()).padStart(2, '0');
    return `${yyyy}-${mm}-${dd}`;
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request: ElectionsCseConformiteRequest = {
      effectifEntreprise: this.effectifEntreprise(),
      papNegocieAvecOS: this.papNegocieAvecOS(),
      delaiInvitationOSRespectee: this.delaiInvitationOSRespectee(),
      collegesConformes: this.collegesConformes(),
      resultatsContestes: this.resultatsContestes(),
      dateElection: this.toIsoDate(this.dateElection()),
      motifContestation: this.motifContestation()?.trim() || null,
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.applyResult(r);
        this.calculating.set(false);
        this.snackBar.open('Analyse de conformité élections CSE calculée', 'OK', { duration: 2500 });
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

  private applyResult(r: ElectionsCseConformiteResponse): void {
    this.result.set(r);
    this.hydrateForm(r);
    this.showForm.set(false);
  }

  /**
   * Ré-injecte le snapshot d'inputs de la réponse dans les champs du
   * formulaire — sans quoi un clic « Modifier » repartirait des valeurs
   * par défaut au lieu des dernières valeurs saisies.
   */
  private hydrateForm(r: ElectionsCseConformiteResponse): void {
    this.effectifEntreprise.set(r.effectifEntreprise ?? 11);
    this.papNegocieAvecOS.set(r.papNegocieAvecOS ?? false);
    this.delaiInvitationOSRespectee.set(r.delaiInvitationOSRespectee ?? false);
    this.collegesConformes.set(r.collegesConformes ?? false);
    this.resultatsContestes.set(r.resultatsContestes ?? false);
    if (r.dateElection) {
      const d = new Date(r.dateElection + 'T00:00:00Z');
      this.dateElection.set(isNaN(d.getTime()) ? null : d);
    } else {
      this.dateElection.set(null);
    }
    this.motifContestation.set(r.motifContestation ?? '');
    // Valeurs persistées = saisie avocat — jamais de badge IA.
    this.provenanceDateElection.set(null);
    this.provenancePapNegocie.set(null);
    this.provenanceColleges.set(null);
    this.provenanceResultatsContestes.set(null);
  }

  // ---------------------------------------------------------------------------
  // Helpers d'affichage du résultat
  // ---------------------------------------------------------------------------

  /**
   * Verdict → bannière de couleur. 3 niveaux :
   *  - CONFORME → vert
   *  - IRREGULARITE_MINEURE → or
   *  - IRREGULARITE_MAJEURE → rouge
   */
  verdictBannerClass(v: ElectionsCseAnalyse): string {
    switch (v) {
      case 'CONFORME':
        return 'cse-verdict-banner cse-verdict-banner--conforme';
      case 'IRREGULARITE_MINEURE':
        return 'cse-verdict-banner cse-verdict-banner--mineure';
      case 'IRREGULARITE_MAJEURE':
        return 'cse-verdict-banner cse-verdict-banner--majeure';
    }
  }

  verdictBannerLabel(v: ElectionsCseAnalyse): string {
    switch (v) {
      case 'CONFORME': return 'Procédure conforme';
      case 'IRREGULARITE_MINEURE': return 'Irrégularité mineure';
      case 'IRREGULARITE_MAJEURE': return 'Irrégularité majeure';
    }
  }

  verdictBannerIcon(v: ElectionsCseAnalyse): string {
    switch (v) {
      case 'CONFORME': return 'verified';
      case 'IRREGULARITE_MINEURE': return 'help_outline';
      case 'IRREGULARITE_MAJEURE': return 'report_problem';
    }
  }

  /** Libellé court d'un point d'irrégularité (utilisé pour le tracking). */
  pointTrackKey(_index: number, p: ElectionsCsePointIrregularite): string {
    return `${p.code}:${p.libelle}`;
  }

  /** Format d'affichage du délai — toujours 15 jours (L. 2314-32 CT). */
  formatDelaiContestation(): string {
    return '15 jours (L. 2314-32 CT)';
  }
}
