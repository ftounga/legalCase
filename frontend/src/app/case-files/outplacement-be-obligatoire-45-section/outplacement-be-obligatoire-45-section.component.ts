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
import { MatRadioModule } from '@angular/material/radio';
import { MatSelectModule } from '@angular/material/select';
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
  OutplacementBeMotifLicenciement,
  OutplacementBeRequest,
  OutplacementBeResponse,
  OutplacementBeVerdict,
  humanizeMotifLicenciement,
  humanizeRaisonNonObligation,
} from '../../core/models/outplacement-be-obligatoire-45.model';
import { OutplacementBeObligatoire45Service } from '../../core/services/outplacement-be-obligatoire-45.service';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { OutplacementBeObligatoire45PrefillRules } from './outplacement-be-obligatoire-45-section-prefill-rules';

/**
 * SF-207-08b : outil décisionnel "Outplacement BE obligatoire 45+" (F-207).
 * BE uniquement (CCT n°82 ; CCT n°82 bis ; Loi du 5 septembre 2001 art. 13 ;
 * AR du 30 mai 2018 ; AR du 25 novembre 1991 art. 154). Consomme l'API
 * SF-207-08 (PR #1160).
 *
 * <p>Affiché conditionnellement par le panel F-IA-04 (tool_id
 * `outplacement-be-obligatoire-45`, règle ALWAYS_ON BE + travail,
 * priority 96).</p>
 *
 * <p>Pattern miroir : `c4-onem-checklist-section` (SF-207-02b PR #1133) —
 * verdict coloré + sanction quantifiée + base juridique mono. Spécificités :
 * 5 verdicts conditionnels (vert/rouge/ambre), formulaire à visibilité
 * conditionnelle (3 champs apparaissent quand `offreOutplacementRecue=true`),
 * sanction soit employeur (€ fixe 1 800 € — AR 30/05/2018) soit salarié
 * (range 4-52 semaines — AR 25/11/1991 art. 154).</p>
 *
 * <p>DERNIÈRE SF de F-207 — 8/8 outils Travail BE livrés.</p>
 */
@Component({
  selector: 'app-outplacement-be-obligatoire-45-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatCheckboxModule,
    MatRadioModule, MatSelectModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './outplacement-be-obligatoire-45-section.component.html',
  styleUrl: './outplacement-be-obligatoire-45-section.component.scss',
})
export class OutplacementBeObligatoire45SectionComponent implements OnInit, OnChanges {
  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'OUTPLACEMENT 45+ (BE)';
  static readonly TOOL_ICON = 'work_history';

  /** F-177 SF-177-12 / F-236 SF-236-02 — délègue au helper partagé (parité runtime). */
  static getPrefillCount(input: PrefillCountInput): number {
    return OutplacementBeObligatoire45PrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() forceExpanded = false;
  /** Gate strict BE — l'outil n'a pas d'équivalent FR (régime spécifique BE). */
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'BELGIQUE';
  @Input() aiData?: TravailExtractedData | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;

  // Exposé au template pour humaniser les enums.
  readonly humanizeRaisonNonObligation = humanizeRaisonNonObligation;
  readonly humanizeMotifLicenciement = humanizeMotifLicenciement;

  /** Whitelist des motifs (alimente le mat-select). */
  readonly motifsLicenciement: ReadonlyArray<OutplacementBeMotifLicenciement> = [
    'LICENCIEMENT_ECONOMIQUE',
    'LICENCIEMENT_AUTRE',
    'FAUTE_GRAVE',
    'DEMISSION',
  ];

  private cdr = inject(ChangeDetectorRef);

  // Snapshots signal des inputs IA pour réactivité computed.
  private aiDataSignal = signal<TravailExtractedData | null | undefined>(undefined);

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<OutplacementBeResponse | null>(null);

  // ── Champs du form (signals — édités via handlers). ────────────────────
  // 6 toujours visibles.
  dateLicenciement = signal<string | null>(null);
  dateNaissanceSalarie = signal<string | null>(null);
  ancienneteAnnees = signal<number | null>(null);
  motifLicenciement = signal<OutplacementBeMotifLicenciement | null>(null);
  contratTempsPlein = signal<boolean>(true);
  offreOutplacementRecue = signal<boolean>(false);
  // 3 conditionnels (si offreOutplacementRecue=true).
  dateOffreOutplacement = signal<string | null>(null);
  offreConformeCCT82 = signal<boolean | null>(null);
  salarieAcceptantOffre = signal<boolean | null>(null);

  // Provenance IA par champ pré-rempli (5 instrumentés).
  provenanceDateLicenciement = signal<'IA' | null>(null);
  provenanceDateNaissance = signal<'IA' | null>(null);
  provenanceAnciennete = signal<'IA' | null>(null);
  provenanceMotif = signal<'IA' | null>(null);
  provenanceOffreRecue = signal<'IA' | null>(null);

  /** Gate strict workspace BE (l'outil n'existe pas côté FR). */
  isAvailable = computed(() => this.workspaceCountry === 'BELGIQUE');

  // Formateur €  (fr-BE) instancié une fois — sanction employeur 1 800 €.
  private readonly euroFormatter = new Intl.NumberFormat('fr-BE', {
    style: 'currency',
    currency: 'EUR',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });

  constructor(
    private service: OutplacementBeObligatoire45Service,
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
   * SF-207-08b — Pré-remplit le form depuis `aiData`. Délègue intégralement
   * au helper pur partagé pour garantir la parité runtime / static
   * `getPrefillCount` (divergence impossible par construction).
   */
  private prefillFromAi(): void {
    const ai = this.aiDataSignal();
    if (!ai) return;
    if (this.workspaceCountry !== 'BELGIQUE') return;

    const ruleInput: PrefillCountInput = { aiData: ai, workspaceCountry: this.workspaceCountry };

    // 1. dateLicenciement
    const dLic = OutplacementBeObligatoire45PrefillRules.computeDateLicenciement(ruleInput);
    if (dLic !== null && (this.dateLicenciement() === null || this.provenanceDateLicenciement() === 'IA')) {
      this.dateLicenciement.set(dLic);
      this.provenanceDateLicenciement.set('IA');
    }

    // 2. dateNaissanceSalarie
    const dNais = OutplacementBeObligatoire45PrefillRules.computeDateNaissanceSalarie(ruleInput);
    if (dNais !== null && (this.dateNaissanceSalarie() === null || this.provenanceDateNaissance() === 'IA')) {
      this.dateNaissanceSalarie.set(dNais);
      this.provenanceDateNaissance.set('IA');
    }

    // 3. ancienneteAnnees
    const anc = OutplacementBeObligatoire45PrefillRules.computeAncienneteAnnees(ruleInput);
    if (anc !== null && (this.ancienneteAnnees() === null || this.provenanceAnciennete() === 'IA')) {
      this.ancienneteAnnees.set(anc);
      this.provenanceAnciennete.set('IA');
    }

    // 4. motifLicenciement
    const mot = OutplacementBeObligatoire45PrefillRules.computeMotifLicenciement(ruleInput);
    if (mot !== null && (this.motifLicenciement() === null || this.provenanceMotif() === 'IA')) {
      this.motifLicenciement.set(mot);
      this.provenanceMotif.set('IA');
    }

    // 5. offreOutplacementRecue (true only)
    const offre = OutplacementBeObligatoire45PrefillRules.computeOffreOutplacementRecue(ruleInput);
    if (offre === true && (this.provenanceOffreRecue() === null || this.provenanceOffreRecue() === 'IA')) {
      this.offreOutplacementRecue.set(true);
      this.provenanceOffreRecue.set('IA');
    }
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  formValid(): boolean {
    if (!this.dateLicenciement()) return false;
    if (!this.dateNaissanceSalarie()) return false;
    const anc = this.ancienneteAnnees();
    if (anc === null || anc === undefined || Number.isNaN(anc) || anc < 0) return false;
    if (!this.motifLicenciement()) return false;

    // Validation conditionnelle : si l'offre est reçue, dateOffre + conforme
    // sont requis. `salarieAcceptantOffre` reste tri-état (peut être null).
    if (this.offreOutplacementRecue()) {
      if (!this.dateOffreOutplacement()) return false;
      if (this.offreConformeCCT82() === null) return false;
    }

    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // -- Handlers --
  onDateLicenciementChange(value: string | null): void {
    this.dateLicenciement.set(value);
    this.provenanceDateLicenciement.set(null);
  }
  onDateNaissanceChange(value: string | null): void {
    this.dateNaissanceSalarie.set(value);
    this.provenanceDateNaissance.set(null);
  }
  onAncienneteChange(value: number | null): void {
    this.ancienneteAnnees.set(value);
    this.provenanceAnciennete.set(null);
  }
  onMotifChange(value: OutplacementBeMotifLicenciement | null): void {
    this.motifLicenciement.set(value);
    this.provenanceMotif.set(null);
  }
  onContratTempsPleinChange(value: boolean): void {
    this.contratTempsPlein.set(value);
  }
  onOffreRecueChange(value: boolean): void {
    this.offreOutplacementRecue.set(value);
    this.provenanceOffreRecue.set(null);
    // Si décochée : reset des 3 champs conditionnels (anti-état zombie).
    if (!value) {
      this.dateOffreOutplacement.set(null);
      this.offreConformeCCT82.set(null);
      this.salarieAcceptantOffre.set(null);
    }
  }
  onDateOffreChange(value: string | null): void {
    this.dateOffreOutplacement.set(value);
  }
  onOffreConformeChange(value: boolean | null): void {
    this.offreConformeCCT82.set(value);
  }
  onSalarieAcceptantChange(value: boolean | null): void {
    this.salarieAcceptantOffre.set(value);
  }

  /** Classe CSS du verdict (vert / rouge / ambre). */
  verdictClass(verdict: OutplacementBeVerdict): string {
    switch (verdict) {
      case 'OUTPLACEMENT_NON_DU':
      case 'OFFRE_CONFORME_RESPECTEE':
        return 'verdict-ok';
      case 'OFFRE_NON_FAITE_SANCTION_EMPLOYEUR':
      case 'OFFRE_NON_CONFORME_SANCTION_EMPLOYEUR':
        return 'verdict-critical';
      case 'OFFRE_REFUSEE_SANCTION_SALARIE':
        return 'verdict-warn';
    }
  }

  verdictIcon(verdict: OutplacementBeVerdict): string {
    switch (verdict) {
      case 'OUTPLACEMENT_NON_DU':
      case 'OFFRE_CONFORME_RESPECTEE':
        return 'check_circle';
      case 'OFFRE_NON_FAITE_SANCTION_EMPLOYEUR':
      case 'OFFRE_NON_CONFORME_SANCTION_EMPLOYEUR':
        return 'error';
      case 'OFFRE_REFUSEE_SANCTION_SALARIE':
        return 'warning';
    }
  }

  verdictLabel(verdict: OutplacementBeVerdict): string {
    switch (verdict) {
      case 'OUTPLACEMENT_NON_DU': return 'Outplacement non dû';
      case 'OFFRE_CONFORME_RESPECTEE': return 'Offre conforme respectée';
      case 'OFFRE_NON_FAITE_SANCTION_EMPLOYEUR': return 'Offre non faite — sanction employeur';
      case 'OFFRE_NON_CONFORME_SANCTION_EMPLOYEUR': return 'Offre non conforme — sanction employeur';
      case 'OFFRE_REFUSEE_SANCTION_SALARIE': return 'Offre refusée — sanction salarié';
    }
  }

  /** true si le verdict implique une sanction employeur (€ fixe). */
  isSanctionEmployeur(verdict: OutplacementBeVerdict): boolean {
    return verdict === 'OFFRE_NON_FAITE_SANCTION_EMPLOYEUR'
        || verdict === 'OFFRE_NON_CONFORME_SANCTION_EMPLOYEUR';
  }

  /** true si le verdict implique une sanction salarié (range semaines). */
  isSanctionSalarie(verdict: OutplacementBeVerdict): boolean {
    return verdict === 'OFFRE_REFUSEE_SANCTION_SALARIE';
  }

  /**
   * Formate un montant € au format fr-BE (séparateurs locaux + 2 décimales).
   * `null` / `undefined` / `NaN` rendus `« — »` (état vide gracieux).
   */
  formatEuro(value: number | null | undefined): string {
    if (value === null || value === undefined || Number.isNaN(value)) return '—';
    return this.euroFormatter.format(value);
  }

  calculate(): void {
    if (!this.formValid()) return;
    if (this.workspaceCountry !== 'BELGIQUE') {
      this.snackBar.open('Outil indisponible pour ce workspace', 'Fermer',
        { duration: 4000, panelClass: 'snack-error' });
      return;
    }
    const offreRecue = this.offreOutplacementRecue();
    const request: OutplacementBeRequest = {
      dateLicenciement: this.dateLicenciement()!,
      dateNaissanceSalarie: this.dateNaissanceSalarie()!,
      ancienneteAnnees: this.ancienneteAnnees()!,
      motifLicenciement: this.motifLicenciement()!,
      contratTempsPlein: this.contratTempsPlein(),
      offreOutplacementRecue: offreRecue,
      dateOffreOutplacement: offreRecue ? this.dateOffreOutplacement() : null,
      offreConformeCCT82: offreRecue ? this.offreConformeCCT82() : null,
      salarieAcceptantOffre: offreRecue ? this.salarieAcceptantOffre() : null,
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
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
        this.dateLicenciement.set(r.dateLicenciement);
        this.dateNaissanceSalarie.set(r.dateNaissanceSalarie);
        this.ancienneteAnnees.set(r.ancienneteAnnees);
        this.motifLicenciement.set(r.motifLicenciement);
        this.contratTempsPlein.set(r.contratTempsPlein);
        this.offreOutplacementRecue.set(r.offreOutplacementRecue);
        this.dateOffreOutplacement.set(r.dateOffreOutplacement);
        this.offreConformeCCT82.set(r.offreConformeCCT82);
        this.salarieAcceptantOffre.set(r.salarieAcceptantOffre);
        // Valeurs persistées = saisie avocat — jamais de badge IA.
        this.provenanceDateLicenciement.set(null);
        this.provenanceDateNaissance.set(null);
        this.provenanceAnciennete.set(null);
        this.provenanceMotif.set(null);
        this.provenanceOffreRecue.set(null);
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
