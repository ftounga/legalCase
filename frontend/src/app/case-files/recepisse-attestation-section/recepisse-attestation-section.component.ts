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
import { MatSnackBar } from '@angular/material/snack-bar';
import { RouterModule } from '@angular/router';

import { RecepisseAttestationService } from '../../core/services/recepisse-attestation.service';
import {
  RecepisseAttestationRequest,
  RecepisseAttestationResponse,
  TypeDocumentSejour,
} from '../../core/models/recepisse-attestation.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { RecepisseAttestationPrefillRules } from './recepisse-attestation-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-214-16 — Outil décisionnel « Récépissé vs attestation » (F-IM-32).
 *
 * FR uniquement. Distingue les droits attachés à un récépissé de demande de
 * titre de ceux d'une attestation de prolongation d'instruction (droit au
 * séjour vs droit au travail, durée de validité) et alerte sur le risque
 * employeur (sanctions L. 8253-1) quand le document est une attestation de
 * prolongation. Pattern miroir : {@link OqtfCategoriesSectionComponent} (F-IM-29).
 *
 * <p>Conforme F-IA-04 / ai-skills/frontend-coherence-audit.md :
 *  - standalone OnPush, palette navy/or
 *  - gate FRANCE + bannière BE
 *  - pré-fill IA (dateExpiration depuis dateExpirationTitre,
 *    typeDocument depuis recepisseOuAttestationType)
 *  - dashboardRefreshService.triggerRefresh() post-POST succès
 *  - static getPrefillCount miroir du runtime prefillFromAi()
 *  - OnPush + ChangeDetectorRef.markForCheck() dans subscribe (next/error)
 *    cf. feedback_onpush_subscribe_markforcheck (mémoire utilisateur)
 */
@Component({
  selector: 'app-recepisse-attestation-section',
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
    MatProgressSpinnerModule,
    RouterModule,
    LegalCitationsPipe,
    ToolJurisprudenceCitationsComponent,
  ],
  templateUrl: './recepisse-attestation-section.component.html',
  styleUrl: './recepisse-attestation-section.component.scss',
})
export class RecepisseAttestationSectionComponent implements OnInit, OnChanges {
  // F-JU-03 SF-JU-03-99c — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-IM-32-recepisse-attestation-fr';
  protected readonly brancheActiveForJurisprudence = 'default';

  static readonly TOOL_LABEL = 'RÉCÉPISSÉ VS ATTESTATION (FR)';
  static readonly TOOL_ICON = 'badge';

  static getPrefillCount(input: PrefillCountInput): number {
    return RecepisseAttestationPrefillRules.computePrefillCount(input);
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
  result = signal<RecepisseAttestationResponse | null>(null);

  typeDocument = signal<TypeDocumentSejour | null>(null);
  dateDelivrance = signal<string | null>(null);
  dateExpiration = signal<string | null>(null);

  provenanceType = signal<'IA' | null>(null);
  provenanceExpiration = signal<'IA' | null>(null);

  readonly typeDocumentOptions: ReadonlyArray<{ code: TypeDocumentSejour; label: string }> = [
    { code: 'RECEPISSE', label: 'Récépissé de demande de titre' },
    { code: 'ATTESTATION_PROLONGATION', label: 'Attestation de prolongation d\'instruction' },
    { code: 'INCONNU', label: 'Type indéterminé' },
  ];

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  constructor(
    private readonly service: RecepisseAttestationService,
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
    if (!this.typeDocument()) return false;
    const delivrance = this.dateDelivrance();
    if (!delivrance || !RecepisseAttestationPrefillRules.ISO_DATE_RE.test(delivrance)) return false;
    const expiration = this.dateExpiration();
    if (!expiration || !RecepisseAttestationPrefillRules.ISO_DATE_RE.test(expiration)) return false;
    if (delivrance > expiration) return false;
    return true;
  }

  onTypeChange(value: TypeDocumentSejour | null): void {
    this.typeDocument.set(value);
    this.provenanceType.set(null);
  }

  onDelivranceChange(value: string | null): void {
    this.dateDelivrance.set(value);
  }

  onExpirationChange(value: string | null): void {
    this.dateExpiration.set(value);
    this.provenanceExpiration.set(null);
  }

  typeLabel(code: TypeDocumentSejour | null | undefined): string {
    const opt = this.typeDocumentOptions.find((o) => o.code === code);
    return opt ? opt.label : '';
  }

  analyze(): void {
    if (!this.formValid()) return;
    const request: RecepisseAttestationRequest = {
      typeDocument: this.typeDocument()!,
      dateDelivrance: this.dateDelivrance()!,
      dateExpiration: this.dateExpiration()!,
    };
    this.analyzing.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('Analyse récépissé / attestation enregistrée', 'OK', { duration: 2500 });
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

    const type = RecepisseAttestationPrefillRules.computeTypeDocument(input);
    if (type !== null && this.typeDocument() === null) {
      this.typeDocument.set(type);
      this.provenanceType.set('IA');
    }

    const expiration = RecepisseAttestationPrefillRules.computeDateExpiration(input);
    if (expiration !== null && this.dateExpiration() === null) {
      this.dateExpiration.set(expiration);
      this.provenanceExpiration.set('IA');
    }
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.typeDocument.set(r.typeDocument ?? null);
        this.dateDelivrance.set(r.dateDelivrance ?? null);
        this.dateExpiration.set(r.dateExpiration ?? null);
        this.provenanceType.set(null);
        this.provenanceExpiration.set(null);
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
