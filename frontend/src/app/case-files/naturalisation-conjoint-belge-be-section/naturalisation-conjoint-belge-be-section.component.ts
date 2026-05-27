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
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatListModule } from '@angular/material/list';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';

import { NaturalisationConjointBelgeBeService } from '../../core/services/naturalisation-conjoint-belge-be.service';
import {
  NATURALISATION_CONJOINT_BELGE_BE_NIVEAUX_LANGUE,
  NATURALISATION_CONJOINT_BELGE_DUREE_COHABITATION_MOIS,
  NaturalisationConjointBelgeBeNiveauLangue,
  NaturalisationConjointBelgeBeNiveauLangueOption,
  NaturalisationConjointBelgeBeRequest,
  NaturalisationConjointBelgeBeResponse,
} from '../../core/models/naturalisation-conjoint-belge-be.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { NaturalisationConjointBelgeBePrefillRules } from './naturalisation-conjoint-belge-be-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-215-10 — Outil décisionnel « Naturalisation conjoint Belge (art. 16) »
 * (F-IM-29-naturalisation-conjoint-belge-be).
 *
 * BELGIQUE uniquement — scoring d'éligibilité à la déclaration de
 * nationalité belge par voie de l'article 16 du Code de la nationalité
 * belge, ouverte au conjoint étranger d'un(e) Belge. Verdict
 * `ELIGIBLE` / `INELIGIBLE` + `dureeManquante` (mois) si la cohabitation
 * 5 ans n'est pas encore atteinte + liste des critères non remplis.
 *
 * Visibilité CONTEXTUAL — flag `naturalisation_be_envisagee` (partagé
 * avec F-IM-28 art. 12bis SF-215-08).
 *
 * Pattern miroir : {@link Naturalisation12bisBeSectionComponent} (SF-215-08)
 * — 3 pré-fills RÉELS (dateMarriage, dureeCohabitationMois, niveauLangue)
 * + 4 checkboxes ASPIRATIONNELLES (cohabitationLegale, preuveIntegration,
 * menaceOrdrePublic, condamnationPenale) que l'IA NE pré-remplit PAS
 * (`PREFILL_COUNT_ALWAYS_ZERO`).
 *
 * <p>Conforme F-IA-04 :
 *  - standalone OnPush, palette : vert ELIGIBLE, rouge INELIGIBLE
 *  - dashboardRefreshService.triggerRefresh() post-POST succès
 *  - static getPrefillCount miroir du runtime prefillFromAi() — max 3
 *  - OnPush + ChangeDetectorRef.markForCheck() dans subscribe (next/error)
 *    cf. feedback_onpush_subscribe_markforcheck
 *
 * <p>Anti-régression hotfix #1385 / #1386 (SF-215-08 master-red) :
 * <strong>aucune directive `appCoherencePopover` n'est utilisée dans le
 * template</strong> — pas de binding `[coherenceAlert]` orphelin
 * possible. F-IA-03 reste à activer en V2 via la directive partagée
 * (inputs `[appCoherencePopover]` / `[appCoherencePopoverReason]`) une
 * fois le mode batch stable.
 */
@Component({
  selector: 'app-naturalisation-conjoint-belge-be-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatListModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    LegalCitationsPipe,
    ToolJurisprudenceCitationsComponent,
  ],
  templateUrl: './naturalisation-conjoint-belge-be-section.component.html',
  styleUrl: './naturalisation-conjoint-belge-be-section.component.scss',
})
export class NaturalisationConjointBelgeBeSectionComponent implements OnInit, OnChanges {
  // F-JU-03 SF-JU-03-99c — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-IM-29-naturalisation-conjoint-belge-be';
  protected readonly brancheActiveForJurisprudence = 'default';

  static readonly TOOL_LABEL = 'NATURALISATION CONJOINT BELGE (ART. 16)';
  static readonly TOOL_ICON = 'family_restroom';

  static getPrefillCount(input: PrefillCountInput): number {
    return NaturalisationConjointBelgeBePrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'BELGIQUE';
  @Input() aiData?: ImmigrationExtractedData | null;
  @Input() forceExpanded = false;
  @Input() standaloneMode = false;

  collapsed = signal(true);
  loading = signal(false);
  analyzing = signal(false);
  showForm = signal(true);
  result = signal<NaturalisationConjointBelgeBeResponse | null>(null);

  dateMarriage = signal<string | null>(null);
  cohabitationLegale = signal<boolean>(false);
  dureeCohabitationMois = signal<number | null>(null);
  niveauLangue = signal<NaturalisationConjointBelgeBeNiveauLangue | null>(null);
  preuveIntegration = signal<boolean>(false);
  menaceOrdrePublic = signal<boolean>(false);
  condamnationPenale = signal<boolean>(false);

  // Provenance — 3 réels (les 4 autres champs restent saisie avocat —
  // `PREFILL_COUNT_ALWAYS_ZERO`).
  provenanceDateMarriage = signal<'IA' | null>(null);
  provenanceDureeCohabitation = signal<'IA' | null>(null);
  provenanceNiveauLangue = signal<'IA' | null>(null);

  readonly niveauxLangue: ReadonlyArray<NaturalisationConjointBelgeBeNiveauLangueOption> =
    NATURALISATION_CONJOINT_BELGE_BE_NIVEAUX_LANGUE;

  readonly dureeRequise = NATURALISATION_CONJOINT_BELGE_DUREE_COHABITATION_MOIS;

  isBelgique = computed<boolean>(() => this.workspaceCountry === 'BELGIQUE');

  constructor(
    private readonly service: NaturalisationConjointBelgeBeService,
    private readonly cdr: ChangeDetectorRef,
    private readonly snackBar: MatSnackBar,
    @Optional() private readonly dashboardRefresh?: CaseDashboardRefreshService,
  ) {}

  ngOnInit(): void {
    if (this.forceExpanded) this.collapsed.set(false);
    if (this.standaloneMode) {
      this.collapsed.set(false);
      this.loading.set(false);
      this.showForm.set(true);
      return;
    }
    if (this.isBelgique() && this.caseFileId) {
      this.load();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['forceExpanded'] && this.forceExpanded) {
      this.collapsed.set(false);
    }
    if (changes['aiData'] && this.isBelgique() && this.showForm() && !this.result()) {
      this.prefillFromAi();
    }
  }

  toggleCollapse(): void {
    this.collapsed.update((v) => !v);
  }

  editMode(): void {
    this.showForm.set(true);
  }

  formValid(): boolean {
    if (!this.dateMarriage()) return false;
    const duree = this.dureeCohabitationMois();
    if (duree === null || duree === undefined || isNaN(duree) || duree < 0 || duree > 600) return false;
    if (!this.niveauLangue()) return false;
    return true;
  }

  onDateMarriageChange(value: string | null | undefined): void {
    this.dateMarriage.set(value ?? null);
    this.provenanceDateMarriage.set(null);
  }

  onCohabitationLegaleChange(value: boolean): void {
    this.cohabitationLegale.set(value);
  }

  onDureeCohabitationChange(value: number | null | undefined): void {
    this.dureeCohabitationMois.set(value ?? null);
    this.provenanceDureeCohabitation.set(null);
  }

  onNiveauLangueChange(value: NaturalisationConjointBelgeBeNiveauLangue | null): void {
    this.niveauLangue.set(value);
    this.provenanceNiveauLangue.set(null);
  }

  onPreuveIntegrationChange(value: boolean): void {
    this.preuveIntegration.set(value);
  }

  onMenaceOrdrePublicChange(value: boolean): void {
    this.menaceOrdrePublic.set(value);
  }

  onCondamnationPenaleChange(value: boolean): void {
    this.condamnationPenale.set(value);
  }

  analyze(): void {
    if (!this.formValid()) return;
    const request: NaturalisationConjointBelgeBeRequest = {
      dateMarriage: this.dateMarriage()!,
      cohabitationLegale: this.cohabitationLegale(),
      dureeCohabitationMois: this.dureeCohabitationMois()!,
      niveauLangue: this.niveauLangue()!,
      preuveIntegration: this.preuveIntegration(),
      menaceOrdrePublic: this.menaceOrdrePublic(),
      condamnationPenale: this.condamnationPenale(),
    };
    this.analyzing.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('Analyse naturalisation conjoint Belge enregistrée', 'OK', { duration: 2500 });
        if (!this.standaloneMode) {
          this.dashboardRefresh?.triggerRefresh();
        }
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.analyzing.set(false);
        const msg = err?.error?.message || err?.error || "Erreur lors de l'analyse";
        this.snackBar.open(String(msg), 'Fermer', {
          duration: 5000,
          panelClass: 'snack-error',
        });
        this.cdr.markForCheck();
      },
    });
  }

  eligibleBannerClass(eligible: boolean | null | undefined): string {
    if (eligible === true)  return 'natcjbe-banner natcjbe-banner--success';
    if (eligible === false) return 'natcjbe-banner natcjbe-banner--danger';
    return 'natcjbe-banner';
  }

  eligibleChipClass(eligible: boolean | null | undefined): string {
    if (eligible === true)  return 'natcjbe-chip natcjbe-chip--success';
    if (eligible === false) return 'natcjbe-chip natcjbe-chip--danger';
    return 'natcjbe-chip';
  }

  eligibleIcon(eligible: boolean | null | undefined): string {
    if (eligible === true)  return 'check_circle';
    if (eligible === false) return 'block';
    return 'info_outline';
  }

  eligibleLabel(eligible: boolean | null | undefined): string {
    if (eligible === true)  return 'ELIGIBLE';
    if (eligible === false) return 'INELIGIBLE';
    return '';
  }

  niveauLangueLabel(code: NaturalisationConjointBelgeBeNiveauLangue | string | null | undefined): string {
    if (!code) return '';
    return this.niveauxLangue.find((o) => o.code === code)?.label ?? String(code);
  }

  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    const input: PrefillCountInput = {
      aiData: this.aiData,
      workspaceCountry: this.workspaceCountry,
    };

    const date = NaturalisationConjointBelgeBePrefillRules.computeDateMarriage(input);
    if (date !== null && !this.dateMarriage()) {
      this.dateMarriage.set(date);
      this.provenanceDateMarriage.set('IA');
    }

    const duree = NaturalisationConjointBelgeBePrefillRules.computeDureeCohabitation(input);
    if (duree !== null && this.dureeCohabitationMois() === null) {
      this.dureeCohabitationMois.set(duree);
      this.provenanceDureeCohabitation.set('IA');
    }

    const niveau = NaturalisationConjointBelgeBePrefillRules.computeNiveauLangue(input);
    if (niveau !== null && !this.niveauLangue()) {
      this.niveauLangue.set(niveau);
      this.provenanceNiveauLangue.set('IA');
    }
    // Les 4 champs (cohabitationLegale, preuveIntegration,
    // menaceOrdrePublic, condamnationPenale) ne sont JAMAIS pré-remplis
    // (PREFILL_COUNT_ALWAYS_ZERO).
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.dateMarriage.set(r.dateMarriage ?? null);
        this.cohabitationLegale.set(r.cohabitationLegale ?? false);
        this.dureeCohabitationMois.set(r.dureeCohabitationMois ?? null);
        this.niveauLangue.set((r.niveauLangue as NaturalisationConjointBelgeBeNiveauLangue) ?? null);
        this.preuveIntegration.set(r.preuveIntegration ?? false);
        this.menaceOrdrePublic.set(r.menaceOrdrePublic ?? false);
        this.condamnationPenale.set(r.condamnationPenale ?? false);
        this.provenanceDateMarriage.set(null);
        this.provenanceDureeCohabitation.set(null);
        this.provenanceNiveauLangue.set(null);
        this.showForm.set(false);
        this.loading.set(false);
        this.cdr.markForCheck();
      },
      error: () => {
        // 404 attendu si aucune analyse — on tente le pré-fill IA.
        this.prefillFromAi();
        this.loading.set(false);
        this.cdr.markForCheck();
      },
    });
  }
}
