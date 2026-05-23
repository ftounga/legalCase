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
import { LicenciementFauteGraveLourdService } from '../../core/services/licenciement-faute-grave-lourd.service';
import {
  LicenciementFauteGraveLourdFacteur,
  LicenciementFauteGraveLourdQualification,
  LicenciementFauteGraveLourdRequest,
  LicenciementFauteGraveLourdResponse,
} from '../../core/models/licenciement-faute-grave-lourd.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';
import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { LicenciementFauteGraveLourdSectionPrefillRules } from './licenciement-faute-grave-lourd-section-prefill-rules';

/**
 * SF-212-02 : composant Angular standalone pour l'outil décisionnel
 * "Licenciement pour faute grave / faute lourde" — tool_id canonique
 * `F-DT-36-licenciement-faute-grave-lourde` (F-DT-36). FRANCE uniquement
 * (distinction faute grave / faute lourde strictement française —
 * L.1234-1 s. CT ; L.1234-9 CT ; Cass. soc. 18/06/2013 n°11-14.393).
 *
 * Pattern de référence : `prise-acte-rupture-section` (F-DT-39, SF-206-06) —
 * formulaire de saisie, POST de calcul, verdict 3 niveaux, refresh dashboard,
 * OnPush + markForCheck dans les subscribe.
 *
 * Verdict 3 niveaux :
 *  - FAUTE_SIMPLE (vert — préavis et indemnités maintenus)
 *  - FAUTE_GRAVE (or — privation préavis + IL ; CP maintenus)
 *  - FAUTE_LOURDE (rouge — privation préavis + IL ; CP maintenus depuis
 *    Cass. soc. 18/06/2013 n°11-14.393 ; DI possibles à charge du salarié)
 */
@Component({
  selector: 'app-licenciement-faute-grave-lourd-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatSelectModule,
    MatSlideToggleModule,
    MatProgressSpinnerModule,
    LegalCitationsPipe,
    ToolJurisprudenceCitationsComponent
  ],
  templateUrl: './licenciement-faute-grave-lourd-section.component.html',
  styleUrl: './licenciement-faute-grave-lourd-section.component.scss',
})
export class LicenciementFauteGraveLourdSectionComponent implements OnInit, OnChanges {
  // F-JU-03 — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-DT-36-licenciement-faute-grave-lourd';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommée par le panel pour la card.
  static readonly TOOL_LABEL = 'FAUTE GRAVE / FAUTE LOURDE';
  static readonly TOOL_ICON = 'gavel';

  /**
   * SF-244 — délégué au helper partagé (parité stricte avec `prefillFromAi()`).
   * Retourne le nombre exact de champs pré-remplissables FR pour le badge tab.
   * Couvre les 6 champs `fauteGrave*`.
   */
  static getPrefillCount(input: {
    aiData?: TravailExtractedData | null;
    procedureChecks?: unknown[];
    aiQuestions?: unknown[];
    piecesManquantes?: unknown[];
    triggerEvents?: unknown[];
    workspaceCountry?: string;
  }): number {
    return LicenciementFauteGraveLourdSectionPrefillRules.computePrefillCount({
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
  result = signal<LicenciementFauteGraveLourdResponse | null>(null);

  // --- Form fields (11 champs du contrat backend) ---
  faitsReproches = signal<string>('');
  datesFaits = signal<string[]>([]);
  dateSaisineEmployeur = signal<string | null>(null);
  prescriptionFauteVerifiee = signal<boolean | null>(null);
  ancienneteMois = signal<number | null>(null);
  salaireMensuelBrutEuros = signal<number | null>(null);
  qualificationEmployeur = signal<LicenciementFauteGraveLourdQualification>('FAUTE_SIMPLE');
  intentionNuireAlleeguee = signal<boolean>(false);
  preuveIntentionNuire = signal<string>('');
  attenteConvocation = signal<boolean>(true);
  motivationLettreAdequate = signal<boolean>(true);

  // Provenance IA par champ pré-rempli — badge `auto_awesome` s'efface au
  // premier `onXxxChange()` manuel sur ce champ.
  provenanceFaitsReproches = signal<'IA' | null>(null);
  provenanceDatesFaits = signal<'IA' | null>(null);
  provenanceQualification = signal<'IA' | null>(null);
  provenanceIntentionNuire = signal<'IA' | null>(null);
  provenanceAncienneteMois = signal<'IA' | null>(null);
  provenanceSalaireMensuelBrut = signal<'IA' | null>(null);

  constructor(
    private service: LicenciementFauteGraveLourdService,
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
   * Pré-fill IA (SF-212-02) — renseigne les 6 champs depuis le sous-objet
   * `faute_grave_detail` (projeté à plat dans `travailExtractedData`).
   * Parité stricte avec `getPrefillCount()` : mêmes mappings, même gate
   * `workspaceCountry === 'FRANCE'`. No-op gracieux si `aiData` absent ou
   * dossier BE.
   */
  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    const ai = this.aiData;
    if (!ai) return;
    const rules = LicenciementFauteGraveLourdSectionPrefillRules;
    const ruleInput = { aiData: ai, workspaceCountry: this.workspaceCountry };

    const faits = rules.computeFaitsReproches(ruleInput);
    if (faits !== null) {
      this.faitsReproches.set(faits);
      this.provenanceFaitsReproches.set('IA');
    }
    const dates = rules.computeDatesFaits(ruleInput);
    if (dates !== null) {
      this.datesFaits.set(dates);
      this.provenanceDatesFaits.set('IA');
    }
    const qualification = rules.computeQualificationEmployeur(ruleInput);
    if (qualification !== null) {
      this.qualificationEmployeur.set(qualification);
      this.provenanceQualification.set('IA');
    }
    const intention = rules.computeIntentionNuireAlleeguee(ruleInput);
    if (intention !== null) {
      this.intentionNuireAlleeguee.set(intention);
      this.provenanceIntentionNuire.set('IA');
    }
    const anciennete = rules.computeAncienneteMois(ruleInput);
    if (anciennete !== null) {
      this.ancienneteMois.set(anciennete);
      this.provenanceAncienneteMois.set('IA');
    }
    const salaire = rules.computeSalaireMensuelBrut(ruleInput);
    if (salaire !== null) {
      this.salaireMensuelBrutEuros.set(salaire);
      this.provenanceSalaireMensuelBrut.set('IA');
    }
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  editMode(): void {
    this.showForm.set(true);
  }

  /** Form valide : FRANCE + ancienneté ≥ 0 + salaire > 0 + faits non vides. */
  formValid(): boolean {
    if (this.workspaceCountry !== 'FRANCE') return false;
    const a = this.ancienneteMois();
    const s = this.salaireMensuelBrutEuros();
    if (a === null || !Number.isFinite(a) || a < 0) return false;
    if (s === null || !Number.isFinite(s) || s <= 0) return false;
    if (!this.faitsReproches() || this.faitsReproches().trim().length === 0) return false;
    return true;
  }

  // --- Handlers — toute modification manuelle efface le badge IA du champ ---

  onFaitsReprochesChange(value: string): void {
    this.faitsReproches.set(value ?? '');
    this.provenanceFaitsReproches.set(null);
  }

  onDatesFaitsTextChange(raw: string): void {
    // Saisie utilisateur : dates séparées par virgules, sauts de ligne ou point-virgules.
    const items = (raw ?? '')
      .split(/[,;\n]/)
      .map(s => s.trim())
      .filter(s => s.length > 0);
    this.datesFaits.set(items);
    this.provenanceDatesFaits.set(null);
  }

  datesFaitsAsText(): string {
    return this.datesFaits().join(', ');
  }

  onDateSaisineEmployeurChange(value: string | null): void {
    this.dateSaisineEmployeur.set(value && value.trim().length > 0 ? value : null);
  }

  onPrescriptionVerifieeChange(value: boolean): void {
    this.prescriptionFauteVerifiee.set(value);
  }

  onAncienneteMoisChange(value: number | string | null): void {
    if (value === null || value === '' || value === undefined) {
      this.ancienneteMois.set(null);
    } else {
      const num = typeof value === 'number' ? value : Number.parseInt(String(value), 10);
      this.ancienneteMois.set(Number.isFinite(num) && num >= 0 ? num : null);
    }
    this.provenanceAncienneteMois.set(null);
  }

  onSalaireChange(value: number | string | null): void {
    if (value === null || value === '' || value === undefined) {
      this.salaireMensuelBrutEuros.set(null);
    } else {
      const num = typeof value === 'number' ? value : Number.parseFloat(String(value));
      this.salaireMensuelBrutEuros.set(Number.isFinite(num) && num > 0 ? num : null);
    }
    this.provenanceSalaireMensuelBrut.set(null);
  }

  onQualificationChange(value: LicenciementFauteGraveLourdQualification): void {
    this.qualificationEmployeur.set(value);
    this.provenanceQualification.set(null);
  }

  onIntentionNuireChange(value: boolean): void {
    this.intentionNuireAlleeguee.set(value);
    this.provenanceIntentionNuire.set(null);
    if (!value) {
      // Pas d'intention de nuire → preuve non pertinente.
      this.preuveIntentionNuire.set('');
    }
  }

  onPreuveIntentionNuireChange(value: string): void {
    this.preuveIntentionNuire.set(value ?? '');
  }

  onAttenteConvocationChange(value: boolean): void {
    this.attenteConvocation.set(value);
  }

  onMotivationLettreAdequateChange(value: boolean): void {
    this.motivationLettreAdequate.set(value);
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request: LicenciementFauteGraveLourdRequest = {
      faitsReproches: this.faitsReproches().trim(),
      datesFaits: this.datesFaits(),
      dateSaisineEmployeur: this.dateSaisineEmployeur(),
      prescriptionFauteVerifiee: this.prescriptionFauteVerifiee(),
      ancienneteMois: this.ancienneteMois()!,
      salaireMensuelBrutEuros: this.salaireMensuelBrutEuros()!,
      qualificationEmployeur: this.qualificationEmployeur(),
      intentionNuireAlleeguee: this.intentionNuireAlleeguee(),
      preuveIntentionNuire: this.preuveIntentionNuire().trim() || null,
      attenteConvocation: this.attenteConvocation(),
      motivationLettreAdequate: this.motivationLettreAdequate(),
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.applyResult(r);
        this.calculating.set(false);
        this.snackBar.open('Analyse de la faute disciplinaire calculée', 'OK', { duration: 2500 });
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
        // 204 ou 404 attendu si aucune analyse — on reste en mode formulaire.
        this.loading.set(false);
        this.cdr.markForCheck();
      },
    });
  }

  private applyResult(r: LicenciementFauteGraveLourdResponse): void {
    this.result.set(r);
    this.hydrateForm(r);
    this.showForm.set(false);
  }

  /**
   * Ré-injecte le snapshot d'inputs de la réponse dans les champs du
   * formulaire — sans quoi un clic « Modifier » repartirait des valeurs par
   * défaut au lieu des dernières valeurs saisies.
   */
  private hydrateForm(r: LicenciementFauteGraveLourdResponse): void {
    this.faitsReproches.set(r.faitsReproches ?? '');
    this.datesFaits.set(Array.isArray(r.datesFaits) ? r.datesFaits : []);
    this.dateSaisineEmployeur.set(r.dateSaisineEmployeur ?? null);
    this.prescriptionFauteVerifiee.set(r.prescriptionFauteVerifiee ?? null);
    this.ancienneteMois.set(r.ancienneteMois);
    this.salaireMensuelBrutEuros.set(r.salaireMensuelBrutEuros);
    this.qualificationEmployeur.set(r.qualificationEmployeur);
    this.intentionNuireAlleeguee.set(!!r.intentionNuireAlleeguee);
    this.preuveIntentionNuire.set(r.preuveIntentionNuire ?? '');
    this.attenteConvocation.set(r.attenteConvocation ?? true);
    this.motivationLettreAdequate.set(r.motivationLettreAdequate ?? true);
    // Valeurs persistées = saisie avocat — jamais de badge IA.
    this.provenanceFaitsReproches.set(null);
    this.provenanceDatesFaits.set(null);
    this.provenanceQualification.set(null);
    this.provenanceIntentionNuire.set(null);
    this.provenanceAncienneteMois.set(null);
    this.provenanceSalaireMensuelBrut.set(null);
  }

  // ---------------------------------------------------------------------------
  // Helpers d'affichage du résultat
  // ---------------------------------------------------------------------------

  /**
   * Qualification → bannière de couleur. 3 niveaux :
   *  - FAUTE_SIMPLE → vert (préavis + IL maintenus)
   *  - FAUTE_GRAVE → or (privation préavis + IL, CP maintenus)
   *  - FAUTE_LOURDE → rouge (privation préavis + IL, CP maintenus — Cass. soc.
   *    18/06/2013 — DI possibles à charge du salarié si préjudice)
   */
  verdictBannerClass(q: LicenciementFauteGraveLourdQualification): string {
    switch (q) {
      case 'FAUTE_SIMPLE':
        return 'fgl-verdict-banner fgl-verdict-banner--simple';
      case 'FAUTE_GRAVE':
        return 'fgl-verdict-banner fgl-verdict-banner--grave';
      case 'FAUTE_LOURDE':
        return 'fgl-verdict-banner fgl-verdict-banner--lourde';
    }
  }

  verdictBannerLabel(q: LicenciementFauteGraveLourdQualification): string {
    switch (q) {
      case 'FAUTE_SIMPLE': return 'Faute simple';
      case 'FAUTE_GRAVE': return 'Faute grave';
      case 'FAUTE_LOURDE': return 'Faute lourde';
    }
  }

  verdictBannerIcon(q: LicenciementFauteGraveLourdQualification): string {
    switch (q) {
      case 'FAUTE_SIMPLE': return 'check_circle';
      case 'FAUTE_GRAVE': return 'warning';
      case 'FAUTE_LOURDE': return 'error';
    }
  }

  /** Libellé court d'un facteur (utilisé pour le tracking dans la liste). */
  facteurTrackKey(_index: number, f: LicenciementFauteGraveLourdFacteur): string {
    return `${f.code}:${f.libelle}`;
  }
}
