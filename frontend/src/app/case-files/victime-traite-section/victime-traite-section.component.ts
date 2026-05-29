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
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { RouterModule } from '@angular/router';

import { VictimeTraiteService } from '../../core/services/victime-traite.service';
import {
  VictimeTraiteRequest,
  VictimeTraiteResponse,
  VictimeTraiteVerdict,
} from '../../core/models/victime-traite.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { VictimeTraitePrefillRules } from './victime-traite-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-214-22 — Outil décisionnel « Victime de traite des êtres humains —
 * protection L. 425-1 » (F-IM-35).
 *
 * FR uniquement (régime distinct en BE). Évalue l'éligibilité à la carte de
 * séjour temporaire « vie privée et familiale » de l'article L. 425-1 du
 * CESEDA pour la victime de traite / proxénétisme ayant porté plainte ou
 * témoigné, liste les mesures de protection et alerte par une bannière ROUGE
 * sur le risque immédiat pour la sécurité de la victime
 * (risqueVictimeEnDanger). Pattern miroir :
 * {@link RecepisseAttestationSectionComponent} (F-IM-32).
 *
 * <p>Conforme F-IA-04 / ai-skills/frontend-coherence-audit.md :
 *  - standalone OnPush, palette navy/or
 *  - gate FRANCE + bannière BE
 *  - pré-fill IA (plainteDeposee depuis tehPlainteDeposee, datePlainte depuis
 *    tehDatePlainte)
 *  - dashboardRefreshService.triggerRefresh() post-POST succès
 *  - static getPrefillCount miroir du runtime prefillFromAi()
 *  - OnPush + ChangeDetectorRef.markForCheck() dans subscribe (next/error)
 *    cf. feedback_onpush_subscribe_markforcheck (mémoire utilisateur)
 */
@Component({
  selector: 'app-victime-traite-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatCheckboxModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
    RouterModule,
    LegalCitationsPipe,
    ToolJurisprudenceCitationsComponent,
  ],
  templateUrl: './victime-traite-section.component.html',
  styleUrl: './victime-traite-section.component.scss',
})
export class VictimeTraiteSectionComponent implements OnInit, OnChanges {
  // F-JU-03 — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-IM-35-victime-traite-l4251-fr';
  protected readonly brancheActiveForJurisprudence = 'default';

  static readonly TOOL_LABEL = 'VICTIME TRAITE L. 425-1 (FR)';
  static readonly TOOL_ICON = 'shield';

  static getPrefillCount(input: PrefillCountInput): number {
    return VictimeTraitePrefillRules.computePrefillCount(input);
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
  result = signal<VictimeTraiteResponse | null>(null);

  plainteDeposee = signal(false);
  datePlainte = signal<string | null>(null);
  collaborationOCRTEH = signal(false);
  presenceAutoriteRefugieDetectee = signal(false);

  provenancePlainte = signal<'IA' | null>(null);
  provenanceDatePlainte = signal<'IA' | null>(null);

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  constructor(
    private readonly service: VictimeTraiteService,
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
    const date = this.datePlainte();
    // datePlainte est optionnel : valide si vide, sinon doit être ISO.
    if (date && !VictimeTraitePrefillRules.ISO_DATE_RE.test(date)) return false;
    return true;
  }

  onPlainteChange(value: boolean): void {
    this.plainteDeposee.set(value);
    this.provenancePlainte.set(null);
  }

  onDatePlainteChange(value: string | null): void {
    this.datePlainte.set(value);
    this.provenanceDatePlainte.set(null);
  }

  onCollaborationChange(value: boolean): void {
    this.collaborationOCRTEH.set(value);
  }

  onPresenceAutoriteChange(value: boolean): void {
    this.presenceAutoriteRefugieDetectee.set(value);
  }

  verdictLabel(verdict: VictimeTraiteVerdict | null | undefined): string {
    switch (verdict) {
      case 'ELIGIBLE_PROBABLE': return 'Éligible probable';
      case 'ELIGIBLE_SOUS_RESERVE_PLAINTE': return 'Éligible sous réserve de plainte';
      case 'NON_ELIGIBLE': return 'Non éligible';
      case 'EN_COURS_IDENTIFICATION': return 'En cours d\'identification';
      default: return '';
    }
  }

  verdictClass(verdict: VictimeTraiteVerdict | null | undefined): string {
    switch (verdict) {
      case 'ELIGIBLE_PROBABLE': return 'vt-chip--ok';
      case 'ELIGIBLE_SOUS_RESERVE_PLAINTE': return 'vt-chip--warning';
      case 'NON_ELIGIBLE': return 'vt-chip--ko';
      default: return 'vt-chip--neutral';
    }
  }

  analyze(): void {
    if (!this.formValid()) return;
    const request: VictimeTraiteRequest = {
      plainteDeposee: this.plainteDeposee(),
      collaborationOCRTEH: this.collaborationOCRTEH(),
      datePlainte: this.datePlainte() ?? null,
      presenceAutoriteRefugieDetectee: this.presenceAutoriteRefugieDetectee(),
    };
    this.analyzing.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('Analyse victime de traite enregistrée', 'OK', { duration: 2500 });
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

  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    const input: PrefillCountInput = {
      aiData: this.aiData,
      workspaceCountry: this.workspaceCountry,
    };

    const plainte = VictimeTraitePrefillRules.computePlainteDeposee(input);
    if (plainte !== null && this.provenancePlainte() === null && !this.plainteDeposee()) {
      this.plainteDeposee.set(plainte);
      this.provenancePlainte.set('IA');
    }

    const date = VictimeTraitePrefillRules.computeDatePlainte(input);
    if (date !== null && this.datePlainte() === null) {
      this.datePlainte.set(date);
      this.provenanceDatePlainte.set('IA');
    }
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
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
