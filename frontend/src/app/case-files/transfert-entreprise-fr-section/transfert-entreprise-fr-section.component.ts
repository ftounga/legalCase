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
import { TransfertEntrepriseFrService } from '../../core/services/transfert-entreprise-fr.service';
import {
  TransfertEntrepriseFrPointAnalyse,
  TransfertEntrepriseFrRequest,
  TransfertEntrepriseFrResponse,
  TransfertEntrepriseFrTypeTransfert,
  TransfertEntrepriseFrVerdict,
} from '../../core/models/transfert-entreprise-fr.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { TransfertEntrepriseFrSectionPrefillRules } from './transfert-entreprise-fr-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-212-06 : composant Angular standalone pour l'outil décisionnel
 * "Transfert d'entreprise — L. 1224-1" — tool_id canonique
 * `F-DT-72-transfert-entreprise-l1224-1`. FRANCE uniquement (la notion
 * d'entité économique autonome est française — Cass. soc. 18/07/2000
 * n°98-46.071 ; en BE, le maintien relève de la CCT 32bis distincte).
 *
 * Pattern de référence : `licenciement-faute-grave-lourd-section` (F-DT-36,
 * SF-212-02) — formulaire de saisie, POST de calcul, verdict 3 niveaux,
 * refresh dashboard, OnPush + markForCheck dans les subscribe.
 *
 * Verdict 3 niveaux :
 *  - L1224_APPLICABLE (vert — maintien automatique des contrats au repreneur)
 *  - L1224_INCERTAIN (or — EEA identifiée mais activité non préservée)
 *  - L1224_INAPPLICABLE (rouge — pas d'EEA, transfert simple d'actifs)
 *
 * Alertes :
 *  - alerteLicenciementsFrauduleux (rouge — licenciements pré-transfert nuls)
 *  - alerteDefautConsultationCse (orange — défaut info-consultation L. 1224-3)
 */
@Component({
  selector: 'app-transfert-entreprise-fr-section',
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
    ToolJurisprudenceCitationsComponent,
  ],
  templateUrl: './transfert-entreprise-fr-section.component.html',
  styleUrl: './transfert-entreprise-fr-section.component.scss',
})
export class TransfertEntrepriseFrSectionComponent implements OnInit, OnChanges {
  // F-JU-03 — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-DT-72-transfert-entreprise-l1224-1';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommée par le panel pour la card.
  static readonly TOOL_LABEL = 'TRANSFERT D’ENTREPRISE (L. 1224-1)';
  static readonly TOOL_ICON = 'corporate_fare';

  /**
   * SF-244 — délégué au helper partagé (parité stricte avec `prefillFromAi()`).
   * Retourne le nombre exact de champs pré-remplissables FR pour le badge tab.
   * Couvre les 5 champs `transfert*`.
   */
  static getPrefillCount(input: {
    aiData?: TravailExtractedData | null;
    procedureChecks?: unknown[];
    aiQuestions?: unknown[];
    piecesManquantes?: unknown[];
    triggerEvents?: unknown[];
    workspaceCountry?: string;
  }): number {
    return TransfertEntrepriseFrSectionPrefillRules.computePrefillCount({
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
  result = signal<TransfertEntrepriseFrResponse | null>(null);

  // --- Form fields (9 champs du contrat backend) ---
  typeTransfert = signal<TransfertEntrepriseFrTypeTransfert>('CESSION');
  eeaIdentifieeAvantTransfert = signal<boolean>(true);
  activiteEconomiquePreservee = signal<boolean>(true);
  salariesTransferes = signal<boolean>(true);
  contratsModifiesParRepreneur = signal<boolean>(false);
  licenciementsPreTransfert = signal<boolean>(false);
  nbLicenciementsPreTransfert = signal<number>(0);
  informationConsultationCseRealisee = signal<boolean>(true);
  dateTransfert = signal<string | null>(null);

  // Provenance IA par champ pré-rempli — badge `auto_awesome` s'efface au
  // premier `onXxxChange()` manuel sur ce champ.
  provenanceTypeTransfert = signal<'IA' | null>(null);
  provenanceEea = signal<'IA' | null>(null);
  provenanceActivite = signal<'IA' | null>(null);
  provenanceLicenciementsPre = signal<'IA' | null>(null);
  provenanceDateTransfert = signal<'IA' | null>(null);

  constructor(
    private service: TransfertEntrepriseFrService,
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
   * Pré-fill IA (SF-212-06) — renseigne les 5 champs depuis le sous-objet
   * `transfert_entreprise_detail` (projeté à plat dans `travailExtractedData`).
   * Parité stricte avec `getPrefillCount()` : mêmes mappings, même gate
   * `workspaceCountry === 'FRANCE'`. No-op gracieux si `aiData` absent ou
   * dossier BE.
   */
  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    const ai = this.aiData;
    if (!ai) return;
    const rules = TransfertEntrepriseFrSectionPrefillRules;
    const ruleInput = { aiData: ai, workspaceCountry: this.workspaceCountry };

    const type = rules.computeTypeTransfert(ruleInput);
    if (type !== null) {
      this.typeTransfert.set(type);
      this.provenanceTypeTransfert.set('IA');
    }
    const eea = rules.computeEeaIdentifiee(ruleInput);
    if (eea !== null) {
      this.eeaIdentifieeAvantTransfert.set(eea);
      this.provenanceEea.set('IA');
    }
    const activite = rules.computeActivitePreservee(ruleInput);
    if (activite !== null) {
      this.activiteEconomiquePreservee.set(activite);
      this.provenanceActivite.set('IA');
    }
    const licenciements = rules.computeLicenciementsPreTransfert(ruleInput);
    if (licenciements !== null) {
      this.licenciementsPreTransfert.set(licenciements);
      this.provenanceLicenciementsPre.set('IA');
    }
    const date = rules.computeDateTransfert(ruleInput);
    if (date !== null) {
      this.dateTransfert.set(date);
      this.provenanceDateTransfert.set('IA');
    }
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  editMode(): void {
    this.showForm.set(true);
  }

  /** Form valide : FRANCE + nb licenciements ≥ 0. */
  formValid(): boolean {
    if (this.workspaceCountry !== 'FRANCE') return false;
    const nb = this.nbLicenciementsPreTransfert();
    if (!Number.isFinite(nb) || nb < 0) return false;
    return true;
  }

  // --- Handlers — toute modification manuelle efface le badge IA du champ ---

  onTypeTransfertChange(value: TransfertEntrepriseFrTypeTransfert): void {
    this.typeTransfert.set(value);
    this.provenanceTypeTransfert.set(null);
  }

  onEeaChange(value: boolean): void {
    this.eeaIdentifieeAvantTransfert.set(value);
    this.provenanceEea.set(null);
  }

  onActiviteChange(value: boolean): void {
    this.activiteEconomiquePreservee.set(value);
    this.provenanceActivite.set(null);
  }

  onSalariesTransferesChange(value: boolean): void {
    this.salariesTransferes.set(value);
  }

  onContratsModifiesChange(value: boolean): void {
    this.contratsModifiesParRepreneur.set(value);
  }

  onLicenciementsPreChange(value: boolean): void {
    this.licenciementsPreTransfert.set(value);
    this.provenanceLicenciementsPre.set(null);
    if (!value) {
      this.nbLicenciementsPreTransfert.set(0);
    }
  }

  onNbLicenciementsChange(value: number | string | null): void {
    if (value === null || value === '' || value === undefined) {
      this.nbLicenciementsPreTransfert.set(0);
    } else {
      const num = typeof value === 'number' ? value : Number.parseInt(String(value), 10);
      this.nbLicenciementsPreTransfert.set(Number.isFinite(num) && num >= 0 ? num : 0);
    }
  }

  onCseConsulteChange(value: boolean): void {
    this.informationConsultationCseRealisee.set(value);
  }

  onDateTransfertChange(value: string | null): void {
    this.dateTransfert.set(value && value.trim().length > 0 ? value : null);
    this.provenanceDateTransfert.set(null);
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request: TransfertEntrepriseFrRequest = {
      typeTransfert: this.typeTransfert(),
      eeaIdentifieeAvantTransfert: this.eeaIdentifieeAvantTransfert(),
      activiteEconomiquePreservee: this.activiteEconomiquePreservee(),
      salariesTransferes: this.salariesTransferes(),
      contratsModifiesParRepreneur: this.contratsModifiesParRepreneur(),
      licenciementsPreTransfert: this.licenciementsPreTransfert(),
      nbLicenciementsPreTransfert: this.nbLicenciementsPreTransfert(),
      informationConsultationCseRealisee: this.informationConsultationCseRealisee(),
      dateTransfert: this.dateTransfert(),
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.applyResult(r);
        this.calculating.set(false);
        this.snackBar.open('Analyse de transfert d’entreprise calculée', 'OK', { duration: 2500 });
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

  private applyResult(r: TransfertEntrepriseFrResponse): void {
    this.result.set(r);
    this.hydrateForm(r);
    this.showForm.set(false);
  }

  /**
   * Ré-injecte le snapshot d'inputs de la réponse dans les champs du
   * formulaire — sans quoi un clic « Modifier » repartirait des valeurs par
   * défaut au lieu des dernières valeurs saisies.
   */
  private hydrateForm(r: TransfertEntrepriseFrResponse): void {
    this.typeTransfert.set(r.typeTransfert);
    this.eeaIdentifieeAvantTransfert.set(r.eeaIdentifieeAvantTransfert ?? true);
    this.activiteEconomiquePreservee.set(r.activiteEconomiquePreservee ?? true);
    this.salariesTransferes.set(r.salariesTransferes ?? true);
    this.contratsModifiesParRepreneur.set(r.contratsModifiesParRepreneur ?? false);
    this.licenciementsPreTransfert.set(r.licenciementsPreTransfert ?? false);
    this.nbLicenciementsPreTransfert.set(r.nbLicenciementsPreTransfert ?? 0);
    this.informationConsultationCseRealisee.set(r.informationConsultationCseRealisee ?? true);
    this.dateTransfert.set(r.dateTransfert ?? null);
    // Valeurs persistées = saisie avocat — jamais de badge IA.
    this.provenanceTypeTransfert.set(null);
    this.provenanceEea.set(null);
    this.provenanceActivite.set(null);
    this.provenanceLicenciementsPre.set(null);
    this.provenanceDateTransfert.set(null);
  }

  // ---------------------------------------------------------------------------
  // Helpers d'affichage du résultat
  // ---------------------------------------------------------------------------

  /**
   * Verdict → bannière de couleur. 3 niveaux :
   *  - L1224_APPLICABLE → vert (maintien automatique)
   *  - L1224_INCERTAIN → or (EEA mais activité incertaine)
   *  - L1224_INAPPLICABLE → rouge (pas d'EEA)
   */
  verdictBannerClass(v: TransfertEntrepriseFrVerdict): string {
    switch (v) {
      case 'L1224_APPLICABLE':
        return 'tef-verdict-banner tef-verdict-banner--applicable';
      case 'L1224_INCERTAIN':
        return 'tef-verdict-banner tef-verdict-banner--incertain';
      case 'L1224_INAPPLICABLE':
        return 'tef-verdict-banner tef-verdict-banner--inapplicable';
    }
  }

  verdictBannerLabel(v: TransfertEntrepriseFrVerdict): string {
    switch (v) {
      case 'L1224_APPLICABLE': return 'L. 1224-1 applicable';
      case 'L1224_INCERTAIN': return 'Applicabilité incertaine';
      case 'L1224_INAPPLICABLE': return 'L. 1224-1 inapplicable';
    }
  }

  verdictBannerIcon(v: TransfertEntrepriseFrVerdict): string {
    switch (v) {
      case 'L1224_APPLICABLE': return 'check_circle';
      case 'L1224_INCERTAIN': return 'help_outline';
      case 'L1224_INAPPLICABLE': return 'cancel';
    }
  }

  /** Libellé court d'un point d'analyse (utilisé pour le tracking dans la liste). */
  pointTrackKey(_index: number, p: TransfertEntrepriseFrPointAnalyse): string {
    return `${p.code}:${p.libelle}`;
  }
}
