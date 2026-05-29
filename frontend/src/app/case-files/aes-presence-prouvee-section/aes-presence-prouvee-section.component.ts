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

import { AesPresenceProuveeService } from '../../core/services/aes-presence-prouvee.service';
import {
  AesPresenceProuveeRequest,
  AesPresenceProuveeResponse,
  PeriodePresentee,
  TypePiece,
} from '../../core/models/aes-presence-prouvee.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import {
  AesPresenceProuveePrefillRules,
  todayIso,
} from './aes-presence-prouvee-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-214-12 — Outil décisionnel « AES — calcul de présence prouvée » (F-IM-30).
 *
 * FR uniquement (admission exceptionnelle au séjour L.435-1 CESEDA / circulaire
 * Valls). Saisie dynamique de périodes de présence justifiées par pièce (ajout /
 * suppression de lignes), calcul du total d'années prouvées et de l'éligibilité
 * aux 4 voies AES (famille 5 ans, humanitaire 10 ans, étudiant 3 ans, métiers en
 * tension 3 ans).
 *
 * Pattern miroir : {@link RegroupementFamilialSectionComponent} (F-IM-26).
 *
 * <p>Conforme F-IA-04 / ai-skills/frontend-coherence-audit.md :
 *  - standalone OnPush, palette navy/or ; vert éligible, rouge non éligible
 *  - pré-fill IA : aesDateEntreeFrance → une période initiale
 *  - dashboardRefreshService.triggerRefresh() post-POST succès
 *  - static getPrefillCount miroir du runtime prefillFromAi()
 *  - OnPush + ChangeDetectorRef.markForCheck() dans subscribe (next/error)
 *    cf. feedback_onpush_subscribe_markforcheck (mémoire utilisateur)
 */
@Component({
  selector: 'app-aes-presence-prouvee-section',
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
    ToolJurisprudenceCitationsComponent,
  ],
  templateUrl: './aes-presence-prouvee-section.component.html',
  styleUrl: './aes-presence-prouvee-section.component.scss',
})
export class AesPresenceProuveeSectionComponent implements OnInit, OnChanges {
  // F-JU-03 SF-JU-03-99c — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-IM-30-aes-presence-prouvee-fr';
  protected readonly brancheActiveForJurisprudence = 'default';

  static readonly TOOL_LABEL = 'AES PRÉSENCE PROUVÉE (FR)';
  static readonly TOOL_ICON = 'event_available';

  static getPrefillCount(input: PrefillCountInput): number {
    return AesPresenceProuveePrefillRules.computePrefillCount(input);
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
  result = signal<AesPresenceProuveeResponse | null>(null);

  /** Liste dynamique des périodes saisies. */
  periodes = signal<PeriodePresentee[]>([]);
  /** Indique que la 1re période a été pré-remplie depuis l'IA. */
  provenanceInitialePeriode = signal<'IA' | null>(null);

  readonly typePieceOptions: ReadonlyArray<{ code: TypePiece; label: string }> = [
    { code: 'RIB_BANQUE', label: 'RIB / relevé bancaire' },
    { code: 'FACTURE_EDF_GAZ', label: 'Facture EDF / gaz' },
    { code: 'QUITTANCE_LOYER', label: 'Quittance de loyer' },
    { code: 'BULLETIN_SALAIRE', label: 'Bulletin de salaire' },
    { code: 'AVIS_IMPOSITION', label: "Avis d'imposition" },
    { code: 'SCOLARITE_ENFANT', label: 'Scolarité enfant' },
    { code: 'ATTESTATION_EMPLOYEUR', label: 'Attestation employeur' },
    { code: 'TITRE_SEJOUR', label: 'Titre de séjour' },
    { code: 'AUTRE', label: 'Autre' },
  ];

  readonly voies: ReadonlyArray<{ key: keyof AesPresenceProuveeResponse['eligibiliteParVoie']; label: string; seuil: string }> = [
    { key: 'aes_famille', label: 'AES famille', seuil: '5 ans' },
    { key: 'aes_humanitaire', label: 'AES humanitaire', seuil: '10 ans' },
    { key: 'aes_etudiant', label: 'AES étudiant', seuil: '3 ans' },
    { key: 'aes_metiers_tension', label: 'AES métiers en tension', seuil: '3 ans' },
  ];

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  constructor(
    private readonly service: AesPresenceProuveeService,
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

  // --- Gestion dynamique des lignes ---

  addPeriode(): void {
    this.periodes.update((rows) => [
      ...rows,
      { debut: '', fin: todayIso(), typePiece: 'AUTRE' as TypePiece },
    ]);
    // Une ligne ajoutée manuellement n'est plus une provenance IA pure.
    this.provenanceInitialePeriode.set(null);
  }

  removePeriode(index: number): void {
    this.periodes.update((rows) => rows.filter((_, i) => i !== index));
    this.provenanceInitialePeriode.set(null);
  }

  onDebutChange(index: number, value: string): void {
    this.updateRow(index, { debut: value });
  }

  onFinChange(index: number, value: string): void {
    this.updateRow(index, { fin: value });
  }

  onTypePieceChange(index: number, value: TypePiece): void {
    this.updateRow(index, { typePiece: value });
  }

  private updateRow(index: number, patch: Partial<PeriodePresentee>): void {
    this.periodes.update((rows) =>
      rows.map((row, i) => (i === index ? { ...row, ...patch } : row)),
    );
    this.provenanceInitialePeriode.set(null);
  }

  private periodeValid(p: PeriodePresentee): boolean {
    if (!p.debut || !p.fin || !p.typePiece) return false;
    // début <= fin (comparaison lexicographique ISO valable pour YYYY-MM-DD).
    return p.debut <= p.fin;
  }

  formValid(): boolean {
    const rows = this.periodes();
    if (rows.length === 0) return false;
    return rows.every((p) => this.periodeValid(p));
  }

  analyze(): void {
    if (!this.formValid()) return;
    const request: AesPresenceProuveeRequest = {
      periodesPresentees: this.periodes().map((p) => ({
        debut: p.debut,
        fin: p.fin,
        typePiece: p.typePiece,
      })),
    };
    this.analyzing.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('Analyse présence prouvée enregistrée', 'OK', { duration: 2500 });
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

  // --- Helpers d'affichage ---

  voieEligible(key: keyof AesPresenceProuveeResponse['eligibiliteParVoie']): boolean {
    const r = this.result();
    return !!r && !!r.eligibiliteParVoie?.[key];
  }

  typePieceLabel(code: TypePiece | null | undefined): string {
    const opt = this.typePieceOptions.find((o) => o.code === code);
    return opt ? opt.label : (code ?? '');
  }

  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    // Ne pré-remplir que si la liste est vide (pas d'écrasement de saisie).
    if (this.periodes().length > 0) return;
    const input: PrefillCountInput = {
      aiData: this.aiData,
      workspaceCountry: this.workspaceCountry,
    };
    const initiale = AesPresenceProuveePrefillRules.computeInitialPeriode(input);
    if (initiale !== null) {
      this.periodes.set([initiale]);
      this.provenanceInitialePeriode.set('IA');
    }
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.periodes.set((r.periodesPresentees ?? []).map((p) => ({ ...p })));
        this.provenanceInitialePeriode.set(null);
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
