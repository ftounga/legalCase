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
import { MatListModule } from '@angular/material/list';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import {
  PieceManquanteEntry,
  TravailExtractedData,
} from '../../core/models/case-analysis.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { AiQuestion } from '../../core/models/ai-question.model';
import {
  LicenciementBeCollectifRenaultPhase,
  LicenciementBeCollectifRenaultRequest,
  LicenciementBeCollectifRenaultResponse,
  LicenciementBeCollectifRenaultTailleEntreprise,
  LicenciementBeCollectifRenaultVerdict,
} from '../../core/models/licenciement-be-collectif-renault.model';
import {
  LicenciementBeCollectifRenaultService,
} from '../../core/services/licenciement-be-collectif-renault.service';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import {
  LicenciementBeCollectifRenaultSectionPrefillRules,
} from './licenciement-be-collectif-renault-section-prefill-rules';
import {
  ToolJurisprudenceCitationsComponent,
} from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-219-07b : outil décisionnel « Licenciement collectif BE — Loi Renault »
 * (F-219).
 *
 * <p>BE uniquement — <b>Loi du 13/02/1998</b> (procédure dite « Renault »
 * suite à la fermeture de Renault-Vilvorde) + <b>CCT n° 24</b>
 * (information / consultation des travailleurs) + <b>CCT n° 39</b>
 * (mesures sociales d'accompagnement) + <b>Directive 98/59/CE</b>.</p>
 *
 * <p>Outil <b>checklist procédurale, pas calculateur</b> : vérifie que
 * l'employeur a respecté les 3 phases (information CE/CPPT, consultation,
 * décision + notification autorité régionale) + le délai d'attente
 * minimum de 30 jours après notification, qui conditionnent la validité
 * du licenciement collectif. Non-respect → sanctions civiles
 * (réintégration / indemnité spéciale art. 67) + nullité possible des
 * préavis notifiés en violation du délai d'attente.</p>
 *
 * <p>Affiché par le panel F-IA-04 (tool_id
 * {@code licenciement-be-collectif-renault}, visibility ALWAYS_ON
 * priority 125 BE / DROIT_DU_TRAVAIL — au-dessus de
 * {@code licenciement-be-fermeture-entreprise} (124, SF-219-06b).</p>
 *
 * <p><b>Verdict 6 états</b> :
 * <ul>
 *   <li>{@code NON_APPLICABLE_SEUIL} (gris/info) — seuil de
 *       déclenchement non franchi (PME &lt; 10 / 60 j, grande &lt; 10 %,
 *       très grande &lt; 30 / 60 j) — procédure Renault non applicable
 *       (CCT n° 9 / CCT n° 32bis restent possibles, hors scope).</li>
 *   <li>{@code CONFORME} (vert) — 3 phases + délai 30 j respectés :
 *       préavis individuels notifiables sans risque de nullité.</li>
 *   <li>{@code NON_CONFORME_PHASE_INFORMATION_INCOMPLETE} (rouge) —
 *       phase 1 information CE / CPPT / DS incomplète.</li>
 *   <li>{@code NON_CONFORME_CONSULTATION_INSUFFISANTE} (rouge) — phase 2
 *       consultation insuffisante (pas de débat réel).</li>
 *   <li>{@code NON_CONFORME_NOTIFICATION_AUTORITE_MANQUANTE} (rouge) —
 *       notification autorité régionale manquante — préavis nuls.</li>
 *   <li>{@code NON_CONFORME_DELAI_ATTENTE_NON_RESPECTE} (rouge) — délai
 *       30 j non respecté — préavis nuls + indemnité art. 67.</li>
 * </ul></p>
 *
 * <p><b>Pré-fill IA V1</b> : aucun champ — alignement pattern uniforme
 * F-213/F-219 (qualifications procédurales / statut effectif employeur
 * non extractibles depuis la pipeline Travail BE actuelle ; saisie
 * avocat depuis procès-verbaux CE + courriers employeur + accusé
 * réception Forem/Actiris/VDAB + lettre de préavis individuelle).</p>
 */
@Component({
  selector: 'app-licenciement-be-collectif-renault-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatSelectModule,
    MatListModule,
    MatCheckboxModule,
    MatProgressSpinnerModule,
    ToolJurisprudenceCitationsComponent,
  ],
  templateUrl: './licenciement-be-collectif-renault-section.component.html',
  styleUrl: './licenciement-be-collectif-renault-section.component.scss',
})
export class LicenciementBeCollectifRenaultSectionComponent
  implements OnInit, OnChanges {

  // F-JU-03 — citations jurisprudentielles F-JU-01 (BE).
  protected readonly toolIdForJurisprudence = 'licenciement-be-collectif-renault';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'LICENCIEMENT COLLECTIF (BE / RENAULT)';
  static readonly TOOL_ICON = 'groups';

  /** F-177 SF-177-12 / F-236 SF-236-02 — délègue au helper partagé (parité runtime). */
  static getPrefillCount(input: PrefillCountInput): number {
    return LicenciementBeCollectifRenaultSectionPrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() forceExpanded = false;
  /** Gate strict BE — pas d'équivalent FR (PSE distinct). */
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
  result = signal<LicenciementBeCollectifRenaultResponse | null>(null);

  // ── Champs du form (signals — édités via handlers). ────────────────────
  dateProjet = signal<string | null>(null);
  tailleEntreprise = signal<LicenciementBeCollectifRenaultTailleEntreprise | null>(null);
  effectifMoyen = signal<number | null>(null);
  nombreLicenciementsEnvisages = signal<number | null>(null);
  phaseAtteinte = signal<LicenciementBeCollectifRenaultPhase | null>(null);
  informationEcriteCePrealable = signal<boolean>(false);
  documentsLegauxCommuniques = signal<boolean>(false);
  consultationEffectiveTenue = signal<boolean>(false);
  reponsesMotiveesEmployeur = signal<boolean>(false);
  notificationAutoriteRegionaleFaite = signal<boolean>(false);
  dateNotificationAutorite = signal<string | null>(null);
  datePremierPreavisNotifie = signal<string | null>(null);

  /** Gate strict workspace BE. */
  isAvailable = computed(() => this.workspaceCountry === 'BELGIQUE');

  constructor(
    private service: LicenciementBeCollectifRenaultService,
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
    // SF-219-07b V1 : pas de pré-fill IA (cf. helper doc) — pas de réaction aiData.
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  formValid(): boolean {
    if (!this.dateProjet()) return false;
    if (this.tailleEntreprise() === null) return false;
    if (this.effectifMoyen() === null) return false;
    if (this.nombreLicenciementsEnvisages() === null) return false;
    if (this.phaseAtteinte() === null) return false;
    // Date notification requise si la case « notification autorité » est cochée.
    if (this.notificationAutoriteRegionaleFaite() && !this.dateNotificationAutorite()) {
      return false;
    }
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // -- Handlers --
  onDateProjetChange(value: string | null): void {
    this.dateProjet.set(value);
  }
  onTailleEntrepriseChange(value: LicenciementBeCollectifRenaultTailleEntreprise): void {
    this.tailleEntreprise.set(value);
  }
  onEffectifMoyenChange(value: number | null): void {
    this.effectifMoyen.set(value);
  }
  onNombreLicenciementsEnvisagesChange(value: number | null): void {
    this.nombreLicenciementsEnvisages.set(value);
  }
  onPhaseAtteinteChange(value: LicenciementBeCollectifRenaultPhase): void {
    this.phaseAtteinte.set(value);
  }
  onInformationEcriteCePrealableChange(value: boolean): void {
    this.informationEcriteCePrealable.set(value);
  }
  onDocumentsLegauxCommuniquesChange(value: boolean): void {
    this.documentsLegauxCommuniques.set(value);
  }
  onConsultationEffectiveTenueChange(value: boolean): void {
    this.consultationEffectiveTenue.set(value);
  }
  onReponsesMotiveesEmployeurChange(value: boolean): void {
    this.reponsesMotiveesEmployeur.set(value);
  }
  onNotificationAutoriteRegionaleFaiteChange(value: boolean): void {
    this.notificationAutoriteRegionaleFaite.set(value);
    // Si la case se décoche, on efface la date pour éviter une cohérence partielle.
    if (!value) {
      this.dateNotificationAutorite.set(null);
    }
  }
  onDateNotificationAutoriteChange(value: string | null): void {
    this.dateNotificationAutorite.set(value);
  }
  onDatePremierPreavisNotifieChange(value: string | null): void {
    this.datePremierPreavisNotifie.set(value);
  }

  // -- Libellés humains --
  tailleEntrepriseLabel(
    value: LicenciementBeCollectifRenaultTailleEntreprise | null,
  ): string {
    switch (value) {
      case 'PETITE_MOINS_20':
        return 'Petite (< 20 — Loi Renault non applicable)';
      case 'PME_20_99':
        return 'PME (20-99 — seuil 10 lic./60 j)';
      case 'GRANDE_100_299':
        return 'Grande (100-299 — seuil 10 % effectif/60 j)';
      case 'TRES_GRANDE_300_PLUS':
        return 'Très grande (≥ 300 — seuil 30 lic./60 j)';
      default:
        return '—';
    }
  }

  phaseAtteinteLabel(
    value: LicenciementBeCollectifRenaultPhase | null,
  ): string {
    switch (value) {
      case 'AUCUNE_PROCEDURE_DEMARREE':
        return 'Aucune procédure démarrée (projet)';
      case 'INFORMATION':
        return 'Phase 1 — Information CE / CPPT / DS';
      case 'CONSULTATION':
        return 'Phase 2 — Consultation effective';
      case 'DECISION_ET_NOTIFICATION':
        return 'Phase 3 — Décision + notification autorité';
      default:
        return '—';
    }
  }

  /** Verdict en français lisible. */
  verdictLabel(): string {
    const r = this.result();
    if (!r) return '—';
    switch (r.verdict) {
      case 'NON_APPLICABLE_SEUIL':
        return 'Loi Renault non applicable (seuil non franchi)';
      case 'CONFORME':
        return 'Procédure conforme — préavis notifiables';
      case 'NON_CONFORME_PHASE_INFORMATION_INCOMPLETE':
        return 'Non conforme — phase 1 information incomplète';
      case 'NON_CONFORME_CONSULTATION_INSUFFISANTE':
        return 'Non conforme — phase 2 consultation insuffisante';
      case 'NON_CONFORME_NOTIFICATION_AUTORITE_MANQUANTE':
        return 'Non conforme — notification autorité manquante';
      case 'NON_CONFORME_DELAI_ATTENTE_NON_RESPECTE':
        return 'Non conforme — délai 30 j non respecté';
      default:
        return '—';
    }
  }

  /** Classe CSS verdict (vert ok, rouge critical, gris info pour NON_APPLICABLE_SEUIL). */
  verdictClass(): string {
    const r = this.result();
    if (!r) return '';
    switch (r.verdict) {
      case 'CONFORME':
        return 'verdict-ok';
      case 'NON_APPLICABLE_SEUIL':
        return 'verdict-info';
      case 'NON_CONFORME_PHASE_INFORMATION_INCOMPLETE':
      case 'NON_CONFORME_CONSULTATION_INSUFFISANTE':
      case 'NON_CONFORME_NOTIFICATION_AUTORITE_MANQUANTE':
      case 'NON_CONFORME_DELAI_ATTENTE_NON_RESPECTE':
        return 'verdict-critical';
      default:
        return '';
    }
  }

  /** Icône Material du verdict. */
  verdictIcon(): string {
    const r = this.result();
    if (!r) return 'gavel';
    switch (r.verdict) {
      case 'CONFORME':
        return 'verified';
      case 'NON_APPLICABLE_SEUIL':
        return 'info';
      case 'NON_CONFORME_PHASE_INFORMATION_INCOMPLETE':
      case 'NON_CONFORME_CONSULTATION_INSUFFISANTE':
      case 'NON_CONFORME_NOTIFICATION_AUTORITE_MANQUANTE':
      case 'NON_CONFORME_DELAI_ATTENTE_NON_RESPECTE':
        return 'block';
      default:
        return 'gavel';
    }
  }

  /** Libellé bref pour le badge du header. */
  verdictBadge(verdict: LicenciementBeCollectifRenaultVerdict): string {
    switch (verdict) {
      case 'CONFORME':
        return 'CONFORME';
      case 'NON_APPLICABLE_SEUIL':
        return 'NON APPLICABLE';
      case 'NON_CONFORME_PHASE_INFORMATION_INCOMPLETE':
        return 'INFO. INCOMPLÈTE';
      case 'NON_CONFORME_CONSULTATION_INSUFFISANTE':
        return 'CONSULT. INSUFF.';
      case 'NON_CONFORME_NOTIFICATION_AUTORITE_MANQUANTE':
        return 'NOTIF. MANQUANTE';
      case 'NON_CONFORME_DELAI_ATTENTE_NON_RESPECTE':
        return 'DÉLAI 30 J VIOLÉ';
      default:
        return '';
    }
  }

  /** Classe CSS badge du header. */
  verdictBadgeClass(verdict: LicenciementBeCollectifRenaultVerdict): string {
    switch (verdict) {
      case 'CONFORME':
        return 'badge-ok';
      case 'NON_APPLICABLE_SEUIL':
        return 'badge-info';
      case 'NON_CONFORME_PHASE_INFORMATION_INCOMPLETE':
      case 'NON_CONFORME_CONSULTATION_INSUFFISANTE':
      case 'NON_CONFORME_NOTIFICATION_AUTORITE_MANQUANTE':
      case 'NON_CONFORME_DELAI_ATTENTE_NON_RESPECTE':
        return 'badge-critical';
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
    const request: LicenciementBeCollectifRenaultRequest = {
      dateProjet: this.dateProjet()!,
      tailleEntreprise: this.tailleEntreprise()!,
      effectifMoyen: this.effectifMoyen()!,
      nombreLicenciementsEnvisages: this.nombreLicenciementsEnvisages()!,
      phaseAtteinte: this.phaseAtteinte()!,
      informationEcriteCePrealable: this.informationEcriteCePrealable(),
      documentsLegauxCommuniques: this.documentsLegauxCommuniques(),
      consultationEffectiveTenue: this.consultationEffectiveTenue(),
      reponsesMotiveesEmployeur: this.reponsesMotiveesEmployeur(),
      notificationAutoriteRegionaleFaite: this.notificationAutoriteRegionaleFaite(),
      dateNotificationAutorite: this.dateNotificationAutorite(),
      datePremierPreavisNotifie: this.datePremierPreavisNotifie(),
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Procédure Loi Renault analysée', 'OK', { duration: 2500 });
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
        this.dateProjet.set(r.dateProjet);
        this.tailleEntreprise.set(r.tailleEntreprise);
        this.effectifMoyen.set(r.effectifMoyen);
        this.nombreLicenciementsEnvisages.set(r.nombreLicenciementsEnvisages);
        this.phaseAtteinte.set(r.phaseAtteinte);
        this.informationEcriteCePrealable.set(r.informationEcriteCePrealable);
        this.documentsLegauxCommuniques.set(r.documentsLegauxCommuniques);
        this.consultationEffectiveTenue.set(r.consultationEffectiveTenue);
        this.reponsesMotiveesEmployeur.set(r.reponsesMotiveesEmployeur);
        this.notificationAutoriteRegionaleFaite.set(r.notificationAutoriteRegionaleFaite);
        this.dateNotificationAutorite.set(r.dateNotificationAutorite);
        this.datePremierPreavisNotifie.set(r.datePremierPreavisNotifie);
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
