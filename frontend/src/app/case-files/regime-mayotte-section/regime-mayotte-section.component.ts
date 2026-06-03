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
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatSnackBar } from '@angular/material/snack-bar';

import { RegimeMayotteService } from '../../core/services/regime-mayotte.service';
import {
  RegimeMayotteRequest,
  RegimeMayotteResponse,
  TypeTitreMayotte,
  PorteeTerritorialeMayotte,
} from '../../core/models/regime-mayotte.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { RegimeMayottePrefillRules } from './regime-mayotte-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-220-02 — Outil décisionnel « Portée territoriale du titre à Mayotte » (F-IM-48).
 *
 * FR uniquement. Analyse la portée territoriale d'un titre délivré à Mayotte
 * (Ord. 2014-464, CESEDA L.832-1 et s.) : un titre mahorais ne vaut PAS
 * autorisation de circuler ou de séjourner en métropole sans démarche spécifique.
 * Objet = dérogation territoriale, pas le choix du titre (renvoi F-IM-05).
 * Pattern miroir : {@link RegimeTunisienSectionComponent}.
 *
 * <p>Conforme F-IA-04 :
 *  - standalone OnPush, palette navy/or Immigration
 *  - pré-fill IA (titre délivré à Mayotte, type de titre, projet déplacement)
 *  - dashboardRefreshService.triggerRefresh() post-POST succès
 *  - static getPrefillCount miroir du runtime prefillFromAi()
 *  - OnPush + ChangeDetectorRef.markForCheck() dans subscribe (next/error)</p>
 */
@Component({
  selector: 'app-regime-mayotte-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatCheckboxModule,
    MatProgressSpinnerModule,
    LegalCitationsPipe,
    ToolJurisprudenceCitationsComponent,
  ],
  templateUrl: './regime-mayotte-section.component.html',
  styleUrl: './regime-mayotte-section.component.scss',
})
export class RegimeMayotteSectionComponent implements OnInit, OnChanges {
  // F-JU-03 — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-IM-48-regime-mayotte-fr';
  protected readonly brancheActiveForJurisprudence = 'default';

  static readonly TOOL_LABEL = 'PORTÉE TERRITORIALE DU TITRE À MAYOTTE (FR)';
  static readonly TOOL_ICON = 'public';

  static getPrefillCount(input: PrefillCountInput): number {
    return RegimeMayottePrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';
  @Input() aiData?: ImmigrationExtractedData | null;
  @Input() forceExpanded = false;
  @Input() standaloneMode = false;

  collapsed = signal(true);
  loading = signal(false);
  analyzing = signal(false);
  showForm = signal(true);
  result = signal<RegimeMayotteResponse | null>(null);

  titreDelivreAMayotte = signal<boolean>(false);
  typeTitre = signal<TypeTitreMayotte | null>(null);
  projetDeplacementMetropole = signal<boolean>(false);
  dateDelivrance = signal<string | null>(null);

  provenanceTitreMayotte = signal<'IA' | null>(null);
  provenanceTypeTitre = signal<'IA' | null>(null);
  provenanceProjet = signal<'IA' | null>(null);

  readonly typeTitreOptions: ReadonlyArray<{ code: TypeTitreMayotte; label: string }> = [
    { code: 'VPF', label: 'Vie privée et familiale' },
    { code: 'SALARIE', label: 'Salarié' },
    { code: 'ETUDIANT', label: 'Étudiant' },
    { code: 'RESIDENT', label: 'Résident' },
    { code: 'AUTRE', label: 'Autre' },
  ];

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  constructor(
    private readonly service: RegimeMayotteService,
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
    if (this.isFrance() && this.caseFileId) {
      this.load();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['forceExpanded'] && this.forceExpanded) {
      this.collapsed.set(false);
    }
    if (changes['aiData'] && this.isFrance() && this.showForm() && !this.result()) {
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
    return this.typeTitre() !== null;
  }

  onTitreMayotteChange(value: boolean): void {
    this.titreDelivreAMayotte.set(value);
    this.provenanceTitreMayotte.set(null);
  }

  onTypeTitreChange(value: TypeTitreMayotte | null): void {
    this.typeTitre.set(value);
    this.provenanceTypeTitre.set(null);
  }

  onProjetChange(value: boolean): void {
    this.projetDeplacementMetropole.set(value);
    this.provenanceProjet.set(null);
  }

  onDateChange(value: string | null): void {
    this.dateDelivrance.set(value);
  }

  analyze(): void {
    if (!this.formValid()) return;
    const request: RegimeMayotteRequest = {
      titreDelivreAMayotte: this.titreDelivreAMayotte(),
      typeTitre: this.typeTitre()!,
      projetDeplacementMetropole: this.projetDeplacementMetropole(),
      dateDelivrance: this.dateDelivrance(),
    };
    this.analyzing.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('Analyse portée territoriale Mayotte enregistrée', 'OK', { duration: 2500 });
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

  bannerClass(result: RegimeMayotteResponse | null | undefined): string {
    if (!result) return 'rm-banner';
    if (result.porteeTerritoriale === 'DROIT_COMMUN') return 'rm-banner rm-banner--neutral';
    // MAYOTTE_UNIQUEMENT : warning si blocage déplacement, info sinon.
    return result.sousStatutDeplacement === 'BLOCAGE_DEPLACEMENT'
      ? 'rm-banner rm-banner--warning'
      : 'rm-banner rm-banner--info';
  }

  bannerIcon(result: RegimeMayotteResponse | null | undefined): string {
    if (!result) return 'info_outline';
    if (result.porteeTerritoriale === 'DROIT_COMMUN') return 'menu_book';
    return result.sousStatutDeplacement === 'BLOCAGE_DEPLACEMENT' ? 'block' : 'place';
  }

  porteeLabel(portee: PorteeTerritorialeMayotte | null | undefined): string {
    switch (portee) {
      case 'MAYOTTE_UNIQUEMENT':
        return 'Portée territoriale limitée à Mayotte';
      case 'DROIT_COMMUN':
        return 'Droit commun (pas de dérogation territoriale mahoraise)';
      default:
        return '';
    }
  }

  deplacementLabel(result: RegimeMayotteResponse | null | undefined): string {
    if (!result) return '';
    return result.sousStatutDeplacement === 'BLOCAGE_DEPLACEMENT'
      ? 'Déplacement métropole bloqué — démarches requises'
      : 'Déplacement libre';
  }

  typeTitreLabel(code: TypeTitreMayotte | null | undefined): string {
    switch (code) {
      case 'VPF':      return 'Vie privée et familiale';
      case 'SALARIE':  return 'Salarié';
      case 'ETUDIANT': return 'Étudiant';
      case 'RESIDENT': return 'Résident';
      case 'AUTRE':    return 'Autre';
      default:         return '';
    }
  }

  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    const input: PrefillCountInput = {
      aiData: this.aiData,
      workspaceCountry: this.workspaceCountry,
    };

    const titre = RegimeMayottePrefillRules.computeTitreDelivreAMayotte(input);
    if (titre !== null && !this.titreDelivreAMayotte()) {
      this.titreDelivreAMayotte.set(titre);
      this.provenanceTitreMayotte.set('IA');
    }

    const type = RegimeMayottePrefillRules.computeTypeTitre(input);
    if (type !== null && !this.typeTitre()) {
      this.typeTitre.set(type);
      this.provenanceTypeTitre.set('IA');
    }

    const projet = RegimeMayottePrefillRules.computeProjetDeplacementMetropole(input);
    if (projet !== null && !this.projetDeplacementMetropole()) {
      this.projetDeplacementMetropole.set(projet);
      this.provenanceProjet.set('IA');
    }
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.titreDelivreAMayotte.set(r.titreDelivreAMayotte ?? false);
        this.typeTitre.set(r.typeTitre ?? null);
        this.projetDeplacementMetropole.set(r.projetDeplacementMetropole ?? false);
        this.dateDelivrance.set(r.dateDelivrance ?? null);
        this.provenanceTitreMayotte.set(null);
        this.provenanceTypeTitre.set(null);
        this.provenanceProjet.set(null);
        this.showForm.set(false);
        this.loading.set(false);
        this.cdr.markForCheck();
      },
      error: () => {
        // 404 = pas encore d'analyse, on tente le pré-fill IA.
        this.prefillFromAi();
        this.loading.set(false);
        this.cdr.markForCheck();
      },
    });
  }
}
