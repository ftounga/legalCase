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
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import {
  PieceManquanteEntry,
  TravailExtractedData,
} from '../../core/models/case-analysis.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { AiQuestion } from '../../core/models/ai-question.model';
import {
  Condition,
  EtapeSuivante,
  MotifUrgence,
  RefereTribunalTravailBeRequest,
  RefereTribunalTravailBeResponse,
  Verdict,
  humanizeCondition,
  humanizeEtape,
  humanizeMotif,
  humanizeVerdict,
} from '../../core/models/refere-tribunal-travail-be.model';
import { RefereTribunalTravailBeService } from '../../core/services/refere-tribunal-travail-be.service';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { RefereTribunalTravailBePrefillRules } from './refere-tribunal-travail-be-section-prefill-rules';

/**
 * SF-207-05b : outil décisionnel "Référé tribunal du travail BE" (F-207).
 * BE uniquement (Code Judiciaire art. 584 — référé devant le président
 * du tribunal du travail). Consomme l'API SF-207-05 (PR #1147).
 *
 * Affiché conditionnellement par le panel F-IA-04 (tool_id
 * `refere-tribunal-travail-be`, règle ALWAYS_ON BE + travail).
 *
 * Pattern canonique : `c4-onem-checklist-section` (SF-207-02b) — checklist
 * + verdict tri-état + texte généré copiable (squelette de requête ≈ lettre
 * rectificative). Spécificités : 5 conditions (au lieu de 10 mentions),
 * score 0-5 + barre de progression colorée, encart etapeSuivante.
 */
@Component({
  selector: 'app-refere-tribunal-travail-be-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatCheckboxModule, MatSelectModule,
    MatProgressSpinnerModule, MatProgressBarModule,
  ],
  templateUrl: './refere-tribunal-travail-be-section.component.html',
  styleUrl: './refere-tribunal-travail-be-section.component.scss',
})
export class RefereTribunalTravailBeSectionComponent implements OnInit, OnChanges {
  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'RÉFÉRÉ TRIBUNAL DU TRAVAIL (BE)';
  static readonly TOOL_ICON = 'gavel';

  /** F-177 SF-177-12 / F-236 SF-236-02 — délègue au helper partagé (parité runtime). */
  static getPrefillCount(input: PrefillCountInput): number {
    return RefereTribunalTravailBePrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() forceExpanded = false;
  /** Gate strict BE — l'outil n'a pas d'équivalent FR (référé prud'homal R.1454-1 est un régime distinct). */
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'BELGIQUE';
  @Input() aiData?: TravailExtractedData | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;

  // Exposé au template pour humanisation.
  readonly humanizeVerdict = humanizeVerdict;
  readonly humanizeCondition = humanizeCondition;
  readonly humanizeMotif = humanizeMotif;
  readonly humanizeEtape = humanizeEtape;

  /** Options du select `motifUrgence` (4 codes whitelist). */
  readonly motifOptions: ReadonlyArray<MotifUrgence> = [
    'HARCELEMENT',
    'SALAIRE_IMPAYE',
    'MODIFICATION_UNILATERALE',
    'AUTRE',
  ];

  private cdr = inject(ChangeDetectorRef);

  // Snapshots signal des inputs IA pour réactivité computed.
  private aiDataSignal = signal<TravailExtractedData | null | undefined>(undefined);

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  copying = signal(false);
  showForm = signal(true);
  result = signal<RefereTribunalTravailBeResponse | null>(null);

  // Champs du form (signals — édités via handlers).
  motifUrgence = signal<MotifUrgence | null>(null);
  motifUrgenceDescription = signal<string | null>(null);
  dateFaitGenerateur = signal<string | null>(null);
  dateDemarcheAmiable = signal<string | null>(null);
  preuveUrgenceJointe = signal<boolean>(false);
  mesureProvisoireDemandee = signal<string | null>(null);
  perilEnDemeure = signal<boolean>(false);
  competenceTerritorialeIdentifiee = signal<boolean>(false);

  // Provenance IA par champ pré-rempli.
  provenanceMotif = signal<'IA' | null>(null);
  provenanceDate = signal<'IA' | null>(null);
  provenancePeril = signal<'IA' | null>(null);

  /** Gate strict workspace BE (l'outil n'existe pas côté FR). */
  isAvailable = computed(() => this.workspaceCountry === 'BELGIQUE');

  /**
   * Pour le motif AUTRE, on impose une description ≥ 30 caractères côté UX
   * (la condition `URGENCE_QUALIFIABLE` n'est remplie côté backend que si
   * la description est circonstanciée).
   */
  motifDescriptionMinLength = computed<number>(() =>
    this.motifUrgence() === 'AUTRE' ? 30 : 1,
  );

  constructor(
    private service: RefereTribunalTravailBeService,
    private snackBar: MatSnackBar,
    @Optional() private dashboardRefresh: CaseDashboardRefreshService | null,
  ) {}

  ngOnInit(): void {
    if (this.forceExpanded) this.collapsed.set(false);

    this.aiDataSignal.set(this.aiData);

    if (this.workspaceCountry === 'BELGIQUE') {
      this.load();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['forceExpanded'] && this.forceExpanded) this.collapsed.set(false);

    if (changes['aiData']) this.aiDataSignal.set(this.aiData);

    // Ré-applique le pré-fill si aiData change avant première résolution.
    if (changes['aiData'] && !changes['aiData'].firstChange
        && this.showForm() && !this.result()
        && this.workspaceCountry === 'BELGIQUE') {
      this.prefillFromAi();
    }
  }

  /**
   * SF-207-05b — Pré-remplit le form depuis `aiData`. Délègue intégralement
   * au helper pur partagé pour garantir la parité runtime / static
   * `getPrefillCount` (divergence impossible par construction).
   */
  private prefillFromAi(): void {
    const ai = this.aiDataSignal();
    if (!ai) return;
    if (this.workspaceCountry !== 'BELGIQUE') return;

    const ruleInput: PrefillCountInput = { aiData: ai, workspaceCountry: this.workspaceCountry };

    // 1. motifUrgence
    const mot = RefereTribunalTravailBePrefillRules.computeMotifUrgence(ruleInput);
    if (mot !== null && (this.motifUrgence() === null || this.provenanceMotif() === 'IA')) {
      this.motifUrgence.set(mot);
      this.provenanceMotif.set('IA');
    }

    // 2. dateFaitGenerateur
    const date = RefereTribunalTravailBePrefillRules.computeDateFaitGenerateur(ruleInput);
    if (date !== null && (this.dateFaitGenerateur() === null || this.provenanceDate() === 'IA')) {
      this.dateFaitGenerateur.set(date);
      this.provenanceDate.set('IA');
    }

    // 3. perilEnDemeure (true only)
    const peril = RefereTribunalTravailBePrefillRules.computePerilEnDemeure(ruleInput);
    if (peril === true && (this.provenancePeril() === null || this.provenancePeril() === 'IA')) {
      this.perilEnDemeure.set(true);
      this.provenancePeril.set('IA');
    }
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  formValid(): boolean {
    const motif = this.motifUrgence();
    if (!motif) return false;
    const desc = (this.motifUrgenceDescription() ?? '').trim();
    if (desc.length < this.motifDescriptionMinLength()) return false;
    if (!this.dateFaitGenerateur()) return false;
    const mesure = (this.mesureProvisoireDemandee() ?? '').trim();
    if (mesure.length === 0) return false;
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // -- Handlers --
  onMotifUrgenceChange(value: MotifUrgence | null): void {
    this.motifUrgence.set(value);
    this.provenanceMotif.set(null);
  }
  onMotifDescriptionChange(value: string | null): void {
    this.motifUrgenceDescription.set(value);
  }
  onDateFaitGenerateurChange(value: string | null): void {
    this.dateFaitGenerateur.set(value);
    this.provenanceDate.set(null);
  }
  onDateDemarcheAmiableChange(value: string | null): void {
    this.dateDemarcheAmiable.set(value);
  }
  onPreuveUrgenceJointeChange(value: boolean): void {
    this.preuveUrgenceJointe.set(value);
  }
  onMesureProvisoireChange(value: string | null): void {
    this.mesureProvisoireDemandee.set(value);
  }
  onPerilEnDemeureChange(value: boolean): void {
    this.perilEnDemeure.set(value);
    this.provenancePeril.set(null);
  }
  onCompetenceTerritorialeChange(value: boolean): void {
    this.competenceTerritorialeIdentifiee.set(value);
  }

  /** Classe CSS du verdict (vert ELIGIBLE / ambre INCERTAIN / rouge NON_ELIGIBLE). */
  verdictClass(verdict: Verdict): string {
    switch (verdict) {
      case 'REFERE_ELIGIBLE': return 'verdict-ok';
      case 'REFERE_INCERTAIN': return 'verdict-warn';
      case 'REFERE_NON_ELIGIBLE': return 'verdict-critical';
    }
  }

  verdictIcon(verdict: Verdict): string {
    switch (verdict) {
      case 'REFERE_ELIGIBLE': return 'check_circle';
      case 'REFERE_INCERTAIN': return 'warning';
      case 'REFERE_NON_ELIGIBLE': return 'block';
    }
  }

  /** Couleur de la barre de progression du score (mat-progress-bar accepte primary/accent/warn). */
  scoreBarColor(verdict: Verdict | undefined): 'primary' | 'accent' | 'warn' {
    if (!verdict) return 'primary';
    switch (verdict) {
      case 'REFERE_ELIGIBLE': return 'primary';
      case 'REFERE_INCERTAIN': return 'accent';
      case 'REFERE_NON_ELIGIBLE': return 'warn';
    }
  }

  /** Classe CSS de l'encart etapeSuivante (vert / ambre / info). */
  etapeClass(etape: EtapeSuivante): string {
    switch (etape) {
      case 'DEPOSER_REQUETE': return 'etape-ok';
      case 'RENFORCER_DOSSIER': return 'etape-warn';
      case 'ALTERNATIVE_PROCEDURE_FOND': return 'etape-info';
    }
  }

  etapeIcon(etape: EtapeSuivante): string {
    switch (etape) {
      case 'DEPOSER_REQUETE': return 'send';
      case 'RENFORCER_DOSSIER': return 'build';
      case 'ALTERNATIVE_PROCEDURE_FOND': return 'info';
    }
  }

  /** Score sur 5, exprimé en pourcentage pour la mat-progress-bar (0-100). */
  scorePercent(): number {
    const r = this.result();
    if (!r) return 0;
    return Math.max(0, Math.min(100, (r.scoreConditions / 5) * 100));
  }

  /**
   * Liste humanisée des conditions non remplies (FR) — alimente la liste à puces
   * sous le verdict.
   */
  conditionsNonRempliesHumanisees(): { code: Condition; label: string }[] {
    const r = this.result();
    if (!r) return [];
    return r.conditionsNonRemplies.map(c => ({ code: c, label: humanizeCondition(c) }));
  }

  calculate(): void {
    if (!this.formValid()) return;
    if (this.workspaceCountry !== 'BELGIQUE') {
      this.snackBar.open('Outil indisponible pour ce workspace', 'Fermer',
        { duration: 4000, panelClass: 'snack-error' });
      return;
    }
    const request: RefereTribunalTravailBeRequest = {
      motifUrgence: this.motifUrgence()!,
      motifUrgenceDescription: (this.motifUrgenceDescription() ?? '').trim(),
      dateFaitGenerateur: this.dateFaitGenerateur()!,
      dateDemarcheAmiable: this.dateDemarcheAmiable() || null,
      preuveUrgenceJointe: this.preuveUrgenceJointe(),
      mesureProvisoireDemandee: (this.mesureProvisoireDemandee() ?? '').trim(),
      perilEnDemeure: this.perilEnDemeure(),
      competenceTerritorialeIdentifiee: this.competenceTerritorialeIdentifiee(),
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        // Refresh dashboard décisionnel (pattern SF-IA-02-03).
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

  /** Copie le squelette de requête dans le presse-papier (Clipboard API). */
  copyRequete(): void {
    const requete = this.result()?.requeteSquelette;
    if (!requete || this.copying()) return;
    this.copying.set(true);
    navigator.clipboard.writeText(requete).then(
      () => {
        this.copying.set(false);
        this.snackBar.open(
          'Squelette de requête copié dans le presse-papier.',
          'Fermer',
          { duration: 3000, panelClass: 'snack-success' },
        );
        this.cdr.markForCheck();
      },
      () => {
        this.copying.set(false);
        this.snackBar.open(
          'Impossible de copier le squelette dans le presse-papier.',
          'Fermer',
          { duration: 4000, panelClass: 'snack-error' },
        );
        this.cdr.markForCheck();
      },
    );
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.motifUrgence.set(r.motifUrgence);
        this.motifUrgenceDescription.set(r.motifUrgenceDescription);
        this.dateFaitGenerateur.set(r.dateFaitGenerateur);
        this.dateDemarcheAmiable.set(r.dateDemarcheAmiable);
        this.preuveUrgenceJointe.set(r.preuveUrgenceJointe);
        this.mesureProvisoireDemandee.set(r.mesureProvisoireDemandee);
        this.perilEnDemeure.set(r.perilEnDemeure);
        this.competenceTerritorialeIdentifiee.set(r.competenceTerritorialeIdentifiee);
        // Valeurs persistées = saisie avocat — jamais de badge IA.
        this.provenanceMotif.set(null);
        this.provenanceDate.set(null);
        this.provenancePeril.set(null);
        this.showForm.set(false);
        this.loading.set(false);
        this.cdr.markForCheck();
      },
      error: () => {
        // 404 attendu si aucune analyse — mode formulaire + pré-fill IA.
        this.prefillFromAi();
        this.loading.set(false);
        this.cdr.markForCheck();
      },
    });
  }
}
