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
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';

import { AesmMenaBeService } from '../../core/services/aesm-mena-be.service';
import {
  AesmMenaBeRequest,
  AesmMenaBeResponse,
  AesmMenaBeVerdict,
} from '../../core/models/aesm-mena-be.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { AesmMenaBePrefillRules } from './aesm-mena-be-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-215-12 — Outil décisionnel composite « AESM + tutelle DGDE (MENA) »
 * (F-IM-30-aesm-mena-be).
 *
 * BELGIQUE uniquement — outil composite 2 volets dédié aux Mineurs
 * Étrangers Non Accompagnés (MENA) :
 *  - Volet 1 — Tutelle DGDE (Loi 04/05/2007) : saisine Service Tutelles
 *    obligatoire dans les 24h ouvrables. Si `tuteurDesigne=false`, le
 *    backend renvoie `etapeTutelle` (à faire) + `delaiDesignationTuteur`
 *    (ISO yyyy-MM-dd, mis en évidence en JetBrains Mono).
 *  - Volet 2 — AESM scoring (Art. 9bis Loi 15/12/1980 adapté +
 *    Circulaire OE 15/09/2005) : scoring 0-100 d'éligibilité au séjour
 *    humanitaire spécifique MENA. Verdict FAVORABLE / SOUS_RESERVE /
 *    DEFAVORABLE + bonus +5 si scolarisation longue durée (> 24 mois).
 *
 * Visibilité CONTEXTUAL — flag `mineur_non_accompagne_be_detecte`
 * (F-203).
 *
 * Pattern miroir : {@link NaturalisationConjointBelgeBeSectionComponent}
 * (SF-215-10) — 3 pré-fills RÉELS (menaAge, menaDateArrivee,
 * menaDureeScolaire) + 5 checkboxes ASPIRATIONNELLES (tuteurDesigne,
 * integrationScolaire, projetVieElabore, perspectiveAutonomie,
 * menaceOrdrePublic) que l'IA NE pré-remplit PAS
 * (`PREFILL_COUNT_ALWAYS_ZERO`).
 *
 * <p>Spécificités UI :
 *  - Formulaire en 2 parties visuellement délimitées (Tutelle DGDE / AESM).
 *  - `dureeScolaire` n'est saisissable QUE si `integrationScolaire=true`
 *    (affichage conditionnel).
 *  - **Bandeau urgence ROUGE** si `prioriteUrgence=true` (ageActuel >= 17)
 *    → « Approche de la majorité — procédure urgente ».
 *  - Bloc info ORANGE si `etapeTutelle !== null` → « À faire : … »
 *    + délai DGDE en JetBrains Mono.
 *  - Badge `verdictAESM` 3 couleurs (vert / orange / rouge).
 *  - Jauge `mat-progress-bar` colorée cohérent verdict.
 *  - Bonus +5 pts scolarité affiché si > 0.
 *
 * <p>Conforme F-IA-04 :
 *  - standalone OnPush
 *  - dashboardRefreshService.triggerRefresh() post-POST succès
 *  - static getPrefillCount miroir du runtime prefillFromAi() — max 3
 *  - OnPush + ChangeDetectorRef.markForCheck() dans subscribe (next/error)
 *    cf. feedback_onpush_subscribe_markforcheck
 *
 * <p>Anti-régression hotfix #1385 / #1386 (SF-215-08 master-red) :
 * <strong>aucune directive `appCoherencePopover` n'est utilisée dans le
 * template</strong> — pas de binding `[coherenceAlert]` orphelin possible
 * (voie a SF-215-10).
 */
@Component({
  selector: 'app-aesm-mena-be-section',
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
    MatProgressBarModule,
    MatProgressSpinnerModule,
    LegalCitationsPipe,
    ToolJurisprudenceCitationsComponent,
  ],
  templateUrl: './aesm-mena-be-section.component.html',
  styleUrl: './aesm-mena-be-section.component.scss',
})
export class AesmMenaBeSectionComponent implements OnInit, OnChanges {
  // F-JU-03 SF-JU-03-99c — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-IM-30-aesm-mena-be';
  protected readonly brancheActiveForJurisprudence = 'default';

  static readonly TOOL_LABEL = 'AESM + TUTELLE DGDE (MENA)';
  static readonly TOOL_ICON = 'child_care';

  static getPrefillCount(input: PrefillCountInput): number {
    return AesmMenaBePrefillRules.computePrefillCount(input);
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
  result = signal<AesmMenaBeResponse | null>(null);

  // Volet tutelle DGDE
  tuteurDesigne = signal<boolean>(false);
  dateArriveeBelgique = signal<string | null>(null);

  // Volet AESM scoring
  ageActuel = signal<number | null>(null);
  integrationScolaire = signal<boolean>(false);
  dureeScolaire = signal<number | null>(null);
  projetVieElabore = signal<boolean>(false);
  perspectiveAutonomie = signal<boolean>(false);
  menaceOrdrePublic = signal<boolean>(false);

  // Provenance — 3 réels (les 5 autres champs restent saisie avocat —
  // `PREFILL_COUNT_ALWAYS_ZERO`).
  provenanceAge = signal<'IA' | null>(null);
  provenanceDateArrivee = signal<'IA' | null>(null);
  provenanceDureeScolaire = signal<'IA' | null>(null);

  isBelgique = computed<boolean>(() => this.workspaceCountry === 'BELGIQUE');

  constructor(
    private readonly service: AesmMenaBeService,
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
    const age = this.ageActuel();
    if (age === null || age === undefined || isNaN(age) || !Number.isInteger(age) || age < 0 || age > 17) {
      return false;
    }
    if (!this.dateArriveeBelgique()) return false;
    // Si integrationScolaire=true alors dureeScolaire requis (0-120)
    if (this.integrationScolaire()) {
      const duree = this.dureeScolaire();
      if (duree === null || duree === undefined || isNaN(duree) || !Number.isInteger(duree) || duree < 0 || duree > 120) {
        return false;
      }
    }
    return true;
  }

  onTuteurDesigneChange(value: boolean): void {
    this.tuteurDesigne.set(value);
  }

  onDateArriveeBelgiqueChange(value: string | null | undefined): void {
    this.dateArriveeBelgique.set(value ?? null);
    this.provenanceDateArrivee.set(null);
  }

  onAgeActuelChange(value: number | null | undefined): void {
    this.ageActuel.set(value ?? null);
    this.provenanceAge.set(null);
  }

  onIntegrationScolaireChange(value: boolean): void {
    this.integrationScolaire.set(value);
    // Si décoché, on remet dureeScolaire à null pour ne pas envoyer une valeur résiduelle.
    if (!value) {
      this.dureeScolaire.set(null);
      this.provenanceDureeScolaire.set(null);
    }
  }

  onDureeScolaireChange(value: number | null | undefined): void {
    this.dureeScolaire.set(value ?? null);
    this.provenanceDureeScolaire.set(null);
  }

  onProjetVieElaboreChange(value: boolean): void {
    this.projetVieElabore.set(value);
  }

  onPerspectiveAutonomieChange(value: boolean): void {
    this.perspectiveAutonomie.set(value);
  }

  onMenaceOrdrePublicChange(value: boolean): void {
    this.menaceOrdrePublic.set(value);
  }

  analyze(): void {
    if (!this.formValid()) return;
    const request: AesmMenaBeRequest = {
      ageActuel: this.ageActuel()!,
      dateArriveeBelgique: this.dateArriveeBelgique()!,
      tuteurDesigne: this.tuteurDesigne(),
      integrationScolaire: this.integrationScolaire(),
      dureeScolaire: this.integrationScolaire() ? this.dureeScolaire() : null,
      projetVieElabore: this.projetVieElabore(),
      perspectiveAutonomie: this.perspectiveAutonomie(),
      menaceOrdrePublic: this.menaceOrdrePublic(),
    };
    this.analyzing.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('Analyse AESM/MENA enregistrée', 'OK', { duration: 2500 });
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

  verdictBannerClass(verdict: AesmMenaBeVerdict | null | undefined): string {
    if (verdict === 'FAVORABLE')     return 'aesm-mena-banner aesm-mena-banner--success';
    if (verdict === 'SOUS_RESERVE')  return 'aesm-mena-banner aesm-mena-banner--warn';
    if (verdict === 'DEFAVORABLE')   return 'aesm-mena-banner aesm-mena-banner--danger';
    return 'aesm-mena-banner';
  }

  verdictChipClass(verdict: AesmMenaBeVerdict | null | undefined): string {
    if (verdict === 'FAVORABLE')     return 'aesm-mena-chip aesm-mena-chip--success';
    if (verdict === 'SOUS_RESERVE')  return 'aesm-mena-chip aesm-mena-chip--warn';
    if (verdict === 'DEFAVORABLE')   return 'aesm-mena-chip aesm-mena-chip--danger';
    return 'aesm-mena-chip';
  }

  verdictIcon(verdict: AesmMenaBeVerdict | null | undefined): string {
    if (verdict === 'FAVORABLE')     return 'check_circle';
    if (verdict === 'SOUS_RESERVE')  return 'warning';
    if (verdict === 'DEFAVORABLE')   return 'block';
    return 'info_outline';
  }

  verdictLabel(verdict: AesmMenaBeVerdict | null | undefined): string {
    if (verdict === 'FAVORABLE')     return 'FAVORABLE';
    if (verdict === 'SOUS_RESERVE')  return 'SOUS RÉSERVE';
    if (verdict === 'DEFAVORABLE')   return 'DÉFAVORABLE';
    return '';
  }

  /** Couleur barre de progression cohérente avec le verdict. */
  progressColor(verdict: AesmMenaBeVerdict | null | undefined): 'primary' | 'accent' | 'warn' {
    if (verdict === 'FAVORABLE')     return 'primary';
    if (verdict === 'SOUS_RESERVE')  return 'accent';
    if (verdict === 'DEFAVORABLE')   return 'warn';
    return 'primary';
  }

  /** Format DD/MM/YYYY pour affichage des dates de délai en JetBrains Mono. */
  formatDateFr(iso: string | null | undefined): string {
    if (!iso) return '';
    const m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(iso);
    if (!m) return iso;
    return `${m[3]}/${m[2]}/${m[1]}`;
  }

  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    const input: PrefillCountInput = {
      aiData: this.aiData,
      workspaceCountry: this.workspaceCountry,
    };

    const age = AesmMenaBePrefillRules.computeAgeActuel(input);
    if (age !== null && this.ageActuel() === null) {
      this.ageActuel.set(age);
      this.provenanceAge.set('IA');
    }

    const date = AesmMenaBePrefillRules.computeDateArriveeBelgique(input);
    if (date !== null && !this.dateArriveeBelgique()) {
      this.dateArriveeBelgique.set(date);
      this.provenanceDateArrivee.set('IA');
    }

    const duree = AesmMenaBePrefillRules.computeDureeScolaire(input);
    if (duree !== null && this.dureeScolaire() === null) {
      this.dureeScolaire.set(duree);
      this.provenanceDureeScolaire.set('IA');
    }
    // Les 5 champs (tuteurDesigne, integrationScolaire, projetVieElabore,
    // perspectiveAutonomie, menaceOrdrePublic) ne sont JAMAIS pré-remplis
    // (PREFILL_COUNT_ALWAYS_ZERO).
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.ageActuel.set(r.ageActuel ?? null);
        this.dateArriveeBelgique.set(r.dateArriveeBelgique ?? null);
        this.tuteurDesigne.set(r.tuteurDesigne ?? false);
        this.integrationScolaire.set(r.integrationScolaire ?? false);
        this.dureeScolaire.set(r.dureeScolaire ?? null);
        this.projetVieElabore.set(r.projetVieElabore ?? false);
        this.perspectiveAutonomie.set(r.perspectiveAutonomie ?? false);
        this.menaceOrdrePublic.set(r.menaceOrdrePublic ?? false);
        this.provenanceAge.set(null);
        this.provenanceDateArrivee.set(null);
        this.provenanceDureeScolaire.set(null);
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
