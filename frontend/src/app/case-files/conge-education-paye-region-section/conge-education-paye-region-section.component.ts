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
import { MatSelectModule } from '@angular/material/select';
import { MatCheckboxModule } from '@angular/material/checkbox';
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
  CongeEducationPayeRegionRegion,
  CongeEducationPayeRegionRequest,
  CongeEducationPayeRegionResponse,
  CongeEducationPayeRegionTypeFormation,
  CongeEducationPayeRegionVerdict,
} from '../../core/models/conge-education-paye-region.model';
import {
  CongeEducationPayeRegionService,
} from '../../core/services/conge-education-paye-region.service';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import {
  CongeEducationPayeRegionSectionPrefillRules,
} from './conge-education-paye-region-section-prefill-rules';
import {
  ToolJurisprudenceCitationsComponent,
} from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-219-11b : outil décisionnel « Congé-éducation payé régionalisé »
 * (F-219).
 *
 * <p>BE uniquement — <b>Loi du 22/01/1985</b> contenant des dispositions
 * sociales, Section 6 « Congé-éducation payé » (art. 108 à 117) + <b>Loi
 * spéciale du 06/01/2014</b> (6e réforme de l'État, transfert aux Régions
 * au 01/07/2014) + <b>Décret WBR 19/02/2014 + AGW 19/12/2014</b>
 * (Wallonie) + <b>Décret 12/12/2014 — Vlaams Opleidingsverlof VOV</b>
 * (Flandre, en vigueur 01/09/2019) + <b>Ordonnance 02/07/2015 + AGBR
 * 14/04/2016</b> (Bruxelles-Capitale).</p>
 *
 * <p>Outil unique qui branche en interne sur le régime régional
 * applicable. La région compétente est celle du <b>lieu de travail</b>
 * du travailleur, pas du domicile.</p>
 *
 * <p>Affiché par le panel F-IA-04 (tool_id
 * {@code conge-education-paye-region}, visibility ALWAYS_ON priority 129
 * BE / DROIT_DU_TRAVAIL — au-dessus de {@code delegue-syndical-cct-5}
 * (128, SF-219-10b).</p>
 *
 * <p><b>Verdict 5 états</b> :
 * <ul>
 *   <li>{@code ELIGIBLE_PLEIN_DROIT} (vert) — temps plein + formation
 *       agréée — accès au plafond régional intégral.</li>
 *   <li>{@code ELIGIBLE_PRORATA} (vert) — temps partiel ≥ seuil
 *       régional + formation agréée — plafond proratisé.</li>
 *   <li>{@code INELIGIBLE_HORS_FORMATION_AGREEE} (rouge) — formation
 *       absente de la liste agréée régionale — aucun droit.</li>
 *   <li>{@code INELIGIBLE_OCCUPATION_INSUFFISANTE} (rouge) — temps de
 *       travail sous le seuil régional (sauf dérogation FLA publics
 *       fragilisés) — aucun droit.</li>
 *   <li>{@code A_ANALYSER} (info) — situation ambiguë (changement de
 *       région en cours d'année, formation transfrontalière, statut
 *       hybride) — analyse avocat BE requise.</li>
 * </ul></p>
 *
 * <p><b>Pré-fill IA V1</b> : aucun champ — alignement pattern uniforme
 * F-213/F-219 (région du lieu de travail, type formation, heures et
 * agrément régional non extractibles depuis la pipeline Travail BE
 * actuelle ; saisie avocat depuis le contrat de travail, la convention
 * de formation et la vérification de la liste agréée régionale).</p>
 */
@Component({
  selector: 'app-conge-education-paye-region-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatSelectModule,
    MatCheckboxModule,
    MatProgressSpinnerModule,
    ToolJurisprudenceCitationsComponent,
  ],
  templateUrl: './conge-education-paye-region-section.component.html',
  styleUrl: './conge-education-paye-region-section.component.scss',
})
export class CongeEducationPayeRegionSectionComponent
  implements OnInit, OnChanges {

  // F-JU-03 — citations jurisprudentielles F-JU-01 (BE).
  protected readonly toolIdForJurisprudence = 'conge-education-paye-region';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'CONGÉ-ÉDUCATION PAYÉ (BE — RÉGIONALISÉ)';
  static readonly TOOL_ICON = 'school';

  /** F-177 SF-177-12 / F-236 SF-236-02 — délègue au helper partagé (parité runtime). */
  static getPrefillCount(input: PrefillCountInput): number {
    return CongeEducationPayeRegionSectionPrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() forceExpanded = false;
  /** Gate strict BE — pas d'équivalent FR strict (CPF / Pro-A distincts). */
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'BELGIQUE';
  @Input() aiData?: TravailExtractedData | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;

  private cdr = inject(ChangeDetectorRef);

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<CongeEducationPayeRegionResponse | null>(null);

  // ── Champs du form (signals — édités via handlers). ────────────────────
  region = signal<CongeEducationPayeRegionRegion | null>(null);
  typeFormation = signal<CongeEducationPayeRegionTypeFormation | null>(null);
  heuresFormationAnnuelles = signal<number | null>(null);
  /** Défaut métier 1.0 (temps plein le plus fréquent). */
  tauxOccupation = signal<number>(1.0);
  publicFragilise = signal<boolean>(false);
  formationAgreeeRegion = signal<boolean>(true);

  /** Gate strict workspace BE. */
  isAvailable = computed(() => this.workspaceCountry === 'BELGIQUE');

  constructor(
    private service: CongeEducationPayeRegionService,
    private snackBar: MatSnackBar,
    @Optional() private dashboardRefresh: CaseDashboardRefreshService | null,
  ) {}

  ngOnInit(): void {
    if (this.forceExpanded) this.collapsed.set(false);
    if (this.workspaceCountry === 'BELGIQUE') {
      this.load();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['forceExpanded'] && this.forceExpanded) this.collapsed.set(false);
    // SF-219-11b V1 : pas de pré-fill IA (cf. helper doc) — pas de réaction aiData.
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  formValid(): boolean {
    if (this.region() === null) return false;
    if (this.typeFormation() === null) return false;
    if (this.heuresFormationAnnuelles() === null) return false;
    if (this.heuresFormationAnnuelles()! < 0) return false;
    if (this.tauxOccupation() === null) return false;
    if (this.tauxOccupation() < 0 || this.tauxOccupation() > 1) return false;
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // -- Handlers --
  onRegionChange(value: CongeEducationPayeRegionRegion): void {
    this.region.set(value);
  }
  onTypeFormationChange(value: CongeEducationPayeRegionTypeFormation): void {
    this.typeFormation.set(value);
  }
  onHeuresFormationAnnuellesChange(value: number | null): void {
    this.heuresFormationAnnuelles.set(value);
  }
  onTauxOccupationChange(value: number | null): void {
    this.tauxOccupation.set(value ?? 1.0);
  }
  onPublicFragiliseChange(value: boolean): void {
    this.publicFragilise.set(value);
  }
  onFormationAgreeeRegionChange(value: boolean): void {
    this.formationAgreeeRegion.set(value);
  }

  // -- Libellés humains --
  regionLabel(value: CongeEducationPayeRegionRegion | null): string {
    switch (value) {
      case 'WALLONIE': return 'Wallonie (Décret 19/02/2014 + AGW 19/12/2014)';
      case 'FLANDRE': return 'Flandre (Décret 12/12/2014 — Vlaams Opleidingsverlof VOV)';
      case 'BRUXELLES': return 'Bruxelles-Capitale (Ordonnance 02/07/2015 + AGBR 14/04/2016)';
      default: return '—';
    }
  }

  typeFormationLabel(value: CongeEducationPayeRegionTypeFormation | null): string {
    switch (value) {
      case 'PROFESSIONNELLE_QUALIFIANTE':
        return 'Formation professionnelle qualifiante';
      case 'GENERALE':
        return 'Formation générale (langues, informatique, sciences humaines)';
      case 'ENSEIGNEMENT_SUPERIEUR_ALTERNANCE':
        return 'Enseignement supérieur en alternance';
      case 'RECONVERSION_PUBLIC_FRAGILISE':
        return 'Reconversion public fragilisé';
      case 'HORS_LISTE_AGREEE':
        return 'Formation hors liste agréée régionale';
      default:
        return '—';
    }
  }

  /** Verdict en français lisible. */
  verdictLabel(): string {
    const r = this.result();
    if (!r) return '—';
    switch (r.verdict) {
      case 'ELIGIBLE_PLEIN_DROIT':
        return 'Droit reconnu — plafond régional intégral (temps plein)';
      case 'ELIGIBLE_PRORATA':
        return 'Droit reconnu — plafond proratisé au taux d\'occupation';
      case 'INELIGIBLE_HORS_FORMATION_AGREEE':
        return 'Inéligible — formation hors liste agréée régionale';
      case 'INELIGIBLE_OCCUPATION_INSUFFISANTE':
        return 'Inéligible — occupation sous le seuil régional';
      case 'A_ANALYSER':
        return 'À analyser — situation ambiguë (avocat BE requis)';
      default:
        return '—';
    }
  }

  /** Classe CSS verdict (vert ok, ambre warn, rouge critical, gris info). */
  verdictClass(): string {
    const r = this.result();
    if (!r) return '';
    switch (r.verdict) {
      case 'ELIGIBLE_PLEIN_DROIT':
      case 'ELIGIBLE_PRORATA':
        return 'verdict-ok';
      case 'INELIGIBLE_HORS_FORMATION_AGREEE':
      case 'INELIGIBLE_OCCUPATION_INSUFFISANTE':
        return 'verdict-critical';
      case 'A_ANALYSER':
        return 'verdict-info';
      default:
        return '';
    }
  }

  /** Icône Material du verdict. */
  verdictIcon(): string {
    const r = this.result();
    if (!r) return 'school';
    switch (r.verdict) {
      case 'ELIGIBLE_PLEIN_DROIT':
        return 'verified';
      case 'ELIGIBLE_PRORATA':
        return 'check_circle';
      case 'INELIGIBLE_HORS_FORMATION_AGREEE':
      case 'INELIGIBLE_OCCUPATION_INSUFFISANTE':
        return 'block';
      case 'A_ANALYSER':
        return 'help';
      default:
        return 'school';
    }
  }

  /** Libellé bref pour le badge du header. */
  verdictBadge(verdict: CongeEducationPayeRegionVerdict): string {
    switch (verdict) {
      case 'ELIGIBLE_PLEIN_DROIT':
        return 'PLEIN DROIT';
      case 'ELIGIBLE_PRORATA':
        return 'PRORATA';
      case 'INELIGIBLE_HORS_FORMATION_AGREEE':
        return 'HORS LISTE';
      case 'INELIGIBLE_OCCUPATION_INSUFFISANTE':
        return 'OCC. INSUFF.';
      case 'A_ANALYSER':
        return 'À ANALYSER';
      default:
        return '';
    }
  }

  /** Classe CSS badge du header. */
  verdictBadgeClass(verdict: CongeEducationPayeRegionVerdict): string {
    switch (verdict) {
      case 'ELIGIBLE_PLEIN_DROIT':
      case 'ELIGIBLE_PRORATA':
        return 'badge-ok';
      case 'INELIGIBLE_HORS_FORMATION_AGREEE':
      case 'INELIGIBLE_OCCUPATION_INSUFFISANTE':
        return 'badge-critical';
      case 'A_ANALYSER':
        return 'badge-info';
      default:
        return '';
    }
  }

  calculate(): void {
    if (!this.formValid()) return;
    if (this.workspaceCountry !== 'BELGIQUE') {
      this.snackBar.open('Outil indisponible pour ce workspace', 'Fermer',
        { duration: 4000, panelClass: 'snack-error' });
      return;
    }
    const request: CongeEducationPayeRegionRequest = {
      region: this.region()!,
      typeFormation: this.typeFormation()!,
      heuresFormationAnnuelles: this.heuresFormationAnnuelles()!,
      tauxOccupation: this.tauxOccupation(),
      publicFragilise: this.publicFragilise(),
      formationAgreeeRegion: this.formationAgreeeRegion(),
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Congé-éducation payé analysé', 'OK', { duration: 2500 });
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
        this.showForm.set(false);
        this.loading.set(false);
        // Rehydratation form pour permettre l'édition.
        this.region.set(r.region);
        this.typeFormation.set(r.typeFormation);
        this.heuresFormationAnnuelles.set(r.heuresFormationAnnuelles);
        this.tauxOccupation.set(r.tauxOccupation);
        this.publicFragilise.set(r.publicFragilise);
        this.formationAgreeeRegion.set(r.formationAgreeeRegion);
        this.cdr.markForCheck();
      },
      error: () => {
        // 404 attendu si aucune analyse — mode formulaire vierge.
        this.loading.set(false);
        this.cdr.markForCheck();
      },
    });
  }
}
