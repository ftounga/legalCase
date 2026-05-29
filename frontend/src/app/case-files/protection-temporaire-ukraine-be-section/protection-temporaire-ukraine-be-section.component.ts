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
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';

import { ProtectionTemporaireUkraineBeService } from '../../core/services/protection-temporaire-ukraine-be.service';
import {
  PROTECTION_TEMPORAIRE_UKRAINE_TITRES,
  ProtectionTemporaireUkraineBeRequest,
  ProtectionTemporaireUkraineBeResponse,
  TitreSejourBE,
  TitreSejourBeOption,
} from '../../core/models/protection-temporaire-ukraine-be.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { ProtectionTemporaireUkraineBePrefillRules } from './protection-temporaire-ukraine-be-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

/** Seuil (jours) en deçà duquel le prochain renouvellement est jugé imminent. */
const RENOUVELLEMENT_IMMINENT_SEUIL_JOURS = 90;

/**
 * Alerte de cohérence F-IA-03 — VOIE (a) : badge inline computed, AUCUNE
 * directive `appCoherencePopover` ni binding `[coherenceAlert]`. L'alerte est un
 * simple objet {label, tooltip} affiché en badge statique sous le champ divergent.
 */
export interface IM34_InlineCoherenceAlert {
  label: string;
  tooltip: string;
}

/**
 * SF-215-20 — Outil décisionnel « Protection temporaire Ukraine (BE) »
 * (F-IM-34-protection-temporaire-ukraine-be).
 *
 * BELGIQUE uniquement — régime de protection temporaire institué par la décision
 * d'exécution (UE) 2022/382 du Conseil (activation de la directive 2001/55/CE)
 * au bénéfice des personnes déplacées d'Ukraine.
 * Visibilité CONTEXTUAL — flag `protection_temporaire_ukraine_detectee`.
 *
 * Pattern miroir : {@link Annexe13quinquiesBeSectionComponent}
 * (F-IM-33-annexe13quinquies-ie-be, SF-215-18).
 *
 * <p>Conforme F-IA-04 :
 *  - standalone OnPush ; badge éligibilité ELIGIBLE vert / INELIGIBLE rouge ;
 *    `dureeProtectionRestante` (X jours) en JetBrains Mono ; bandeau orange si
 *    renouvellement imminent (< 90 j) ; bloc droits travail proéminent (mention
 *    « pas de single permit requis ») ; `cheminProcedure` en liste numérotée
 *  - pré-fill IA RÉEL 2 champs (dateArrivee, nationaliteUkrainienne) ;
 *    residenceUkraineAvant24Fev2022 + apatridesUkraine + membreFamilleProtege +
 *    titreSejourBE aspirationnels → non comptés
 *  - VOIE (a) F-IA-03 : alerte = badge inline computed (PAS de directive popover)
 *  - dashboardRefreshService.triggerRefresh() post-POST succès
 *  - static getPrefillCount miroir du runtime prefillFromAi()
 *  - OnPush + ChangeDetectorRef.markForCheck() dans subscribe (next/error)
 *    cf. feedback_onpush_subscribe_markforcheck
 */
@Component({
  selector: 'app-protection-temporaire-ukraine-be-section',
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
    MatProgressSpinnerModule,
    MatSelectModule,
    LegalCitationsPipe,
    ToolJurisprudenceCitationsComponent,
  ],
  templateUrl: './protection-temporaire-ukraine-be-section.component.html',
  styleUrl: './protection-temporaire-ukraine-be-section.component.scss',
})
export class ProtectionTemporaireUkraineBeSectionComponent implements OnInit, OnChanges {
  // F-JU-03 SF-JU-03-99c — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-IM-34-protection-temporaire-ukraine-be';
  protected readonly brancheActiveForJurisprudence = 'default';

  static readonly TOOL_LABEL = 'PROTECTION TEMPORAIRE UKRAINE (BE)';
  static readonly TOOL_ICON = 'shield';

  static getPrefillCount(input: PrefillCountInput): number {
    return ProtectionTemporaireUkraineBePrefillRules.computePrefillCount(input);
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
  result = signal<ProtectionTemporaireUkraineBeResponse | null>(null);

  dateArrivee = signal<string | null>(null);
  nationaliteUkrainienne = signal<boolean>(false);
  residenceUkraineAvant24Fev2022 = signal<boolean>(false);
  apatridesUkraine = signal<boolean>(false);
  membreFamilleProtege = signal<boolean>(false);
  titreSejourBE = signal<TitreSejourBE | null>(null);

  provenanceDateArrivee = signal<'IA' | null>(null);
  provenanceNationalite = signal<'IA' | null>(null);

  readonly titres: ReadonlyArray<TitreSejourBeOption> =
    PROTECTION_TEMPORAIRE_UKRAINE_TITRES;

  isBelgique = computed<boolean>(() => this.workspaceCountry === 'BELGIQUE');

  /**
   * VOIE (a) F-IA-03 — alerte inline sur la date d'arrivée uniquement. Renvoie
   * un objet {label, tooltip} ou null. AUCUNE directive popover.
   */
  dateArriveeAlert = computed<IM34_InlineCoherenceAlert | null>(() => {
    if (this.standaloneMode) return null;
    if (!this.showForm()) return null;
    const userDate = this.dateArrivee();
    if (!userDate) return null;
    const aiDate = this.aiData?.ptUkraineDateArrivee;
    if (typeof aiDate !== 'string' || !ISO_DATE_RE.test(aiDate)) return null;
    if (aiDate === userDate) return null;
    const aiFr = this.formatDateFr(aiDate);
    return {
      label: `Incohérence détectée (${aiFr})`,
      tooltip: `Analyse du dossier : arrivée sur le territoire le ${aiFr}`,
    };
  });

  constructor(
    private readonly service: ProtectionTemporaireUkraineBeService,
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
    const dateArr = this.dateArrivee();
    if (!dateArr || !ISO_DATE_RE.test(dateArr)) return false;
    if (!this.titreSejourBE()) return false;
    return true;
  }

  onDateArriveeChange(value: string | null): void {
    this.dateArrivee.set(value || null);
    this.provenanceDateArrivee.set(null);
  }

  onNationaliteChange(value: boolean): void {
    this.nationaliteUkrainienne.set(value);
    this.provenanceNationalite.set(null);
  }

  onResidenceChange(value: boolean): void {
    this.residenceUkraineAvant24Fev2022.set(value);
  }

  onApatridesChange(value: boolean): void {
    this.apatridesUkraine.set(value);
  }

  onMembreFamilleChange(value: boolean): void {
    this.membreFamilleProtege.set(value);
  }

  onTitreSejourChange(value: TitreSejourBE | null): void {
    this.titreSejourBE.set(value);
  }

  analyze(): void {
    if (!this.formValid()) return;
    const request: ProtectionTemporaireUkraineBeRequest = {
      dateArrivee: this.dateArrivee()!,
      nationaliteUkrainienne: this.nationaliteUkrainienne(),
      residenceUkraineAvant24Fev2022: this.residenceUkraineAvant24Fev2022(),
      apatridesUkraine: this.apatridesUkraine(),
      membreFamilleProtege: this.membreFamilleProtege(),
      titreSejourBE: this.titreSejourBE()!,
    };
    this.analyzing.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('Analyse protection temporaire Ukraine enregistrée', 'OK', { duration: 2500 });
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

  // ---- Badge éligibilité : ELIGIBLE vert / INELIGIBLE rouge ----
  eligibiliteBadgeClass(eligible: boolean | null | undefined): string {
    if (eligible === true) return 'pt-elig-badge pt-elig-badge--ok';
    if (eligible === false) return 'pt-elig-badge pt-elig-badge--ko';
    return 'pt-elig-badge';
  }

  eligibiliteLabel(eligible: boolean | null | undefined): string {
    if (eligible === true) return 'ELIGIBLE';
    if (eligible === false) return 'INELIGIBLE';
    return '—';
  }

  eligibiliteIcon(eligible: boolean | null | undefined): string {
    if (eligible === true) return 'verified_user';
    if (eligible === false) return 'gpp_bad';
    return 'shield';
  }

  /** Bandeau orange si la protection est éligible et expire bientôt (< 90 j). */
  renouvellementImminent(r: ProtectionTemporaireUkraineBeResponse | null): boolean {
    if (!r || !r.eligible) return false;
    return r.dureeProtectionRestante <= RENOUVELLEMENT_IMMINENT_SEUIL_JOURS;
  }

  /** Texte du prochain renouvellement, que `prochainRenouvellement` soit bool ou string. */
  renouvellementMessage(r: ProtectionTemporaireUkraineBeResponse | null): string {
    if (!r) return '';
    const v = r.prochainRenouvellement;
    if (typeof v === 'string') return v;
    return v
      ? 'Un renouvellement de la protection temporaire est requis prochainement.'
      : "Aucun renouvellement immédiat requis.";
  }

  /** Format JJ/MM/YYYY depuis une date ISO yyyy-MM-dd. */
  formatDateFr(iso: string | null | undefined): string {
    if (!iso || !ISO_DATE_RE.test(iso)) return '—';
    const [y, m, d] = iso.split('-');
    return `${d}/${m}/${y}`;
  }

  titreLabel(code: TitreSejourBE | string | null | undefined): string {
    if (!code) return '';
    return this.titres.find((t) => t.code === code)?.label ?? String(code);
  }

  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    const input: PrefillCountInput = {
      aiData: this.aiData,
      workspaceCountry: this.workspaceCountry,
    };

    const dateArr = ProtectionTemporaireUkraineBePrefillRules.computeDateArrivee(input);
    if (dateArr !== null && !this.dateArrivee()) {
      this.dateArrivee.set(dateArr);
      this.provenanceDateArrivee.set('IA');
    }

    const nat = ProtectionTemporaireUkraineBePrefillRules.computeNationaliteUkrainienne(input);
    if (nat === true && !this.nationaliteUkrainienne()) {
      this.nationaliteUkrainienne.set(true);
      this.provenanceNationalite.set('IA');
    }
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.dateArrivee.set(r.dateArrivee ?? null);
        this.nationaliteUkrainienne.set(r.nationaliteUkrainienne ?? false);
        this.residenceUkraineAvant24Fev2022.set(r.residenceUkraineAvant24Fev2022 ?? false);
        this.apatridesUkraine.set(r.apatridesUkraine ?? false);
        this.membreFamilleProtege.set(r.membreFamilleProtege ?? false);
        this.titreSejourBE.set((r.titreSejourBE as TitreSejourBE) ?? null);
        this.provenanceDateArrivee.set(null);
        this.provenanceNationalite.set(null);
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
