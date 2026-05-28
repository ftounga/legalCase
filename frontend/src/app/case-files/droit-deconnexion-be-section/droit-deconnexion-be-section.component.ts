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
  DroitDeconnexionBeRequest,
  DroitDeconnexionBeResponse,
  DroitDeconnexionBeStatutAccord,
  DroitDeconnexionBeVerdict,
} from '../../core/models/droit-deconnexion-be.model';
import {
  DroitDeconnexionBeService,
} from '../../core/services/droit-deconnexion-be.service';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import {
  DroitDeconnexionBeSectionPrefillRules,
} from './droit-deconnexion-be-section-prefill-rules';
import {
  ToolJurisprudenceCitationsComponent,
} from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-219-19b : outil décisionnel « droit à la déconnexion BE » (F-219).
 *
 * <p>BE uniquement — <b>Loi du 03/10/2022</b> portant des dispositions
 * diverses en matière de travail dite « Deal pour l'emploi » (M.B.
 * 10/11/2022), <b>art. 16</b> (obligation employeur ≥ 20 travailleurs
 * de conclure une CCT d'entreprise ou modifier le règlement de
 * travail définissant les modalités du droit à la déconnexion sur 3
 * thèmes : modalités pratiques, sensibilisation/formation, modalités
 * d'organisation du travail) ; <b>AR du 19/02/2023</b> (entrée en
 * vigueur 01/04/2023) ; <b>CCT n° 149</b> du Conseil national du
 * travail (cadre supplétif lorsque l'employeur n'a pas adopté son
 * propre instrument) ; <b>Loi du 08/04/1965</b> art. 11-12
 * (procédure règlement de travail) ; <b>Code pénal social</b> art.
 * 137 (sanction de niveau 2 en cas de manquement).</p>
 *
 * <p>Affiché par le panel F-IA-04 (tool_id {@code droit-deconnexion-be},
 * visibility ALWAYS_ON priority 137 BE / DROIT_DU_TRAVAIL — au-dessus
 * de {@code semaine-4-jours-be} (136, SF-219-18b),
 * {@code clause-ecolage-be} (135, SF-219-17b),
 * {@code teletravail-be-cct-85-149} (134, SF-219-16b).</p>
 *
 * <h2>Verdict hiérarchisé 6 états (court-circuits backend)</h2>
 * <ul>
 *   <li>{@code HORS_CHAMP_SEUIL_NON_ATTEINT} (gris info) — employeur
 *       &lt; 20 travailleurs : obligation déconnexion non applicable
 *       (art. 16 § 1).</li>
 *   <li>{@code MANQUEMENT_GRAVE_AUCUNE_INITIATIVE} (rouge) — aucun
 *       accord ni amorce de concertation : manquement frontal
 *       exposant aux sanctions C. pén. social art. 137 (niveau 2)
 *       et à l'action en cessation par les organisations syndicales.</li>
 *   <li>{@code NON_CONFORME_INSTRUMENT_MANQUANT} (ambre) —
 *       concertation engagée mais aucun instrument formalisé :
 *       manquement à l'obligation de résultat (art. 16 § 1).</li>
 *   <li>{@code NON_CONFORME_CONTENU_INCOMPLET} (ambre) — instrument
 *       formalisé mais thèmes obligatoires manquants (art. 16 § 2
 *       1° / 2° / 3°).</li>
 *   <li>{@code CONFORME_ACCORD_COMPLET} (vert) — CCT ou règlement
 *       de travail formalisé ET 3 thèmes art. 16 § 2 traités.</li>
 *   <li>{@code A_ANALYSER} (gris info) — configuration ambigüe
 *       (statut indéterminé, articulation CCT 149 supplétive ou
 *       CCT 85 télétravail) → analyse cas par cas.</li>
 * </ul>
 *
 * <p><b>Pré-fill IA V1</b> : aucun champ — alignement pattern
 * uniforme F-213 / F-219 (effectif employeur, statut accord, date
 * entrée en vigueur instrument, modalités pratiques / sensibilisation
 * / organisation, consultation organe concertation, manquement
 * signalé CBE/SPF non extractibles du pipeline Travail BE actuel —
 * déclaratifs RH employeur ou avocat).</p>
 */
@Component({
  selector: 'app-droit-deconnexion-be-section',
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
  templateUrl: './droit-deconnexion-be-section.component.html',
  styleUrl: './droit-deconnexion-be-section.component.scss',
})
export class DroitDeconnexionBeSectionComponent
  implements OnInit, OnChanges {

  // F-JU-03 — citations jurisprudentielles F-JU-01 (BE).
  protected readonly toolIdForJurisprudence = 'droit-deconnexion-be';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'DROIT À LA DÉCONNEXION (BE)';
  static readonly TOOL_ICON = 'phonelink_erase';

  /** F-177 SF-177-12 / F-236 SF-236-02 — délègue au helper partagé (parité runtime). */
  static getPrefillCount(input: PrefillCountInput): number {
    return DroitDeconnexionBeSectionPrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() forceExpanded = false;
  /** Gate strict BE — pas d'équivalent FR mécanique (Loi El Khomri 2016 = négociation annuelle 50+). */
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
  result = signal<DroitDeconnexionBeResponse | null>(null);

  // ── Champs du form (signals — édités via handlers). ────────────────────
  effectifEntreprise = signal<number | null>(null);
  statutAccord = signal<DroitDeconnexionBeStatutAccord | null>(null);
  dateEntreeVigueurInstrument = signal<string | null>(null);
  modalitesPratiquesDeconnexionDefinies = signal<boolean | null>(null);
  sensibilisationFormationPrevue = signal<boolean | null>(null);
  modalitesOrganisationTravailDefinies = signal<boolean | null>(null);
  consultationOrganeConcertationEffectuee = signal<boolean | null>(null);
  manquementSignaleCbeOuSpf = signal<boolean | null>(null);

  /** Gate strict workspace BE. */
  isAvailable = computed(() => this.workspaceCountry === 'BELGIQUE');

  /**
   * Date d'entrée en vigueur requise uniquement si un instrument est
   * formalisé (CCT déposée ou règlement modifié).
   */
  isInstrumentFormalise = computed(
    () => this.statutAccord() === 'CCT_ENTREPRISE_DEPOSEE'
       || this.statutAccord() === 'REGLEMENT_TRAVAIL_MODIFIE');

  constructor(
    private service: DroitDeconnexionBeService,
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
    // SF-219-19b V1 : pas de pré-fill IA (cf. helper doc) — pas de réaction aiData.
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  formValid(): boolean {
    if (this.effectifEntreprise() === null) return false;
    const effectif = this.effectifEntreprise() as number;
    if (effectif < 0 || effectif > 1000000) return false;
    if (this.statutAccord() === null) return false;
    if (this.modalitesPratiquesDeconnexionDefinies() === null) return false;
    if (this.sensibilisationFormationPrevue() === null) return false;
    if (this.modalitesOrganisationTravailDefinies() === null) return false;
    if (this.consultationOrganeConcertationEffectuee() === null) return false;
    if (this.manquementSignaleCbeOuSpf() === null) return false;
    // dateEntreeVigueurInstrument requise si instrument formalisé.
    if (this.isInstrumentFormalise() && !this.dateEntreeVigueurInstrument()) return false;
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // -- Handlers --
  onEffectifEntrepriseChange(value: string | number | null): void {
    this.effectifEntreprise.set(this.toNumberOrNull(value));
  }
  onStatutAccordChange(value: DroitDeconnexionBeStatutAccord): void {
    this.statutAccord.set(value);
  }
  onDateEntreeVigueurInstrumentChange(value: string | null): void {
    this.dateEntreeVigueurInstrument.set(value);
  }
  onModalitesPratiquesDeconnexionDefiniesChange(value: boolean): void {
    this.modalitesPratiquesDeconnexionDefinies.set(value);
  }
  onSensibilisationFormationPrevueChange(value: boolean): void {
    this.sensibilisationFormationPrevue.set(value);
  }
  onModalitesOrganisationTravailDefiniesChange(value: boolean): void {
    this.modalitesOrganisationTravailDefinies.set(value);
  }
  onConsultationOrganeConcertationEffectueeChange(value: boolean): void {
    this.consultationOrganeConcertationEffectuee.set(value);
  }
  onManquementSignaleCbeOuSpfChange(value: boolean): void {
    this.manquementSignaleCbeOuSpf.set(value);
  }

  private toNumberOrNull(value: string | number | null): number | null {
    if (value === null || value === '') return null;
    const n = typeof value === 'number' ? value : Number(value);
    return Number.isFinite(n) ? n : null;
  }

  // -- Libellés humains --
  statutAccordLabel(value: DroitDeconnexionBeStatutAccord | null): string {
    switch (value) {
      case 'CCT_ENTREPRISE_DEPOSEE':
        return 'CCT d\'entreprise déposée au greffe du SPF Emploi';
      case 'REGLEMENT_TRAVAIL_MODIFIE':
        return 'Modification du règlement de travail (procédure ordinaire)';
      case 'NEGOCIATION_EN_COURS':
        return 'Concertation engagée, aucun instrument formalisé';
      case 'AUCUNE_INITIATIVE':
        return 'Aucun accord ni amorce de concertation';
      case 'INDETERMINE':
        return 'Statut indéterminé (analyse cas par cas)';
      default:
        return '—';
    }
  }

  /** Verdict en français lisible. */
  verdictLabel(): string {
    const r = this.result();
    if (!r) return '—';
    switch (r.verdict) {
      case 'HORS_CHAMP_SEUIL_NON_ATTEINT':
        return 'Employeur < 20 travailleurs — obligation déconnexion non applicable (art. 16 § 1 Loi 03/10/2022)';
      case 'CONFORME_ACCORD_COMPLET':
        return 'Conforme — CCT ou règlement de travail formalisé ET 3 thèmes art. 16 § 2 traités (modalités pratiques + sensibilisation/formation + organisation du travail)';
      case 'NON_CONFORME_CONTENU_INCOMPLET':
        return 'Instrument formalisé mais thèmes obligatoires manquants (art. 16 § 2) — non-conformité à compléter';
      case 'NON_CONFORME_INSTRUMENT_MANQUANT':
        return 'Concertation engagée mais aucun instrument formalisé — manquement à l\'obligation de résultat (art. 16 § 1)';
      case 'MANQUEMENT_GRAVE_AUCUNE_INITIATIVE':
        return 'Aucun accord ni amorce de concertation — manquement frontal exposant aux sanctions C. pén. social art. 137 (niveau 2) et à l\'action en cessation';
      case 'A_ANALYSER':
        return 'Configuration ambigüe — analyse cas par cas requise';
      default:
        return '—';
    }
  }

  /** Classe CSS verdict (vert / ambre / rouge / gris info). */
  verdictClass(): string {
    const r = this.result();
    if (!r) return '';
    switch (r.verdict) {
      case 'CONFORME_ACCORD_COMPLET':
        return 'verdict-ok';
      case 'MANQUEMENT_GRAVE_AUCUNE_INITIATIVE':
        return 'verdict-critical';
      case 'NON_CONFORME_CONTENU_INCOMPLET':
      case 'NON_CONFORME_INSTRUMENT_MANQUANT':
        return 'verdict-warn';
      case 'HORS_CHAMP_SEUIL_NON_ATTEINT':
      case 'A_ANALYSER':
        return 'verdict-neutral';
      default:
        return '';
    }
  }

  /** Icône Material du verdict. */
  verdictIcon(): string {
    const r = this.result();
    if (!r) return 'phonelink_erase';
    switch (r.verdict) {
      case 'CONFORME_ACCORD_COMPLET':
        return 'check_circle';
      case 'MANQUEMENT_GRAVE_AUCUNE_INITIATIVE':
        return 'gavel';
      case 'NON_CONFORME_INSTRUMENT_MANQUANT':
        return 'description';
      case 'NON_CONFORME_CONTENU_INCOMPLET':
        return 'edit_document';
      case 'HORS_CHAMP_SEUIL_NON_ATTEINT':
        return 'do_not_disturb_on';
      case 'A_ANALYSER':
        return 'help_outline';
      default:
        return 'phonelink_erase';
    }
  }

  /** Libellé bref pour le badge du header. */
  verdictBadge(verdict: DroitDeconnexionBeVerdict): string {
    switch (verdict) {
      case 'CONFORME_ACCORD_COMPLET':
        return 'CONFORME';
      case 'MANQUEMENT_GRAVE_AUCUNE_INITIATIVE':
        return 'MANQUEMENT GRAVE';
      case 'NON_CONFORME_INSTRUMENT_MANQUANT':
        return 'INSTRUMENT MANQUANT';
      case 'NON_CONFORME_CONTENU_INCOMPLET':
        return 'CONTENU INCOMPLET';
      case 'HORS_CHAMP_SEUIL_NON_ATTEINT':
        return 'HORS CHAMP';
      case 'A_ANALYSER':
        return 'À ANALYSER';
      default:
        return '';
    }
  }

  /** Classe CSS badge du header. */
  verdictBadgeClass(verdict: DroitDeconnexionBeVerdict): string {
    switch (verdict) {
      case 'CONFORME_ACCORD_COMPLET':
        return 'badge-ok';
      case 'MANQUEMENT_GRAVE_AUCUNE_INITIATIVE':
        return 'badge-critical';
      case 'NON_CONFORME_INSTRUMENT_MANQUANT':
      case 'NON_CONFORME_CONTENU_INCOMPLET':
        return 'badge-warn';
      case 'HORS_CHAMP_SEUIL_NON_ATTEINT':
      case 'A_ANALYSER':
        return 'badge-neutral';
      default:
        return '';
    }
  }

  /** Format effectif (locale FR). */
  formatEffectif(value: number | null | undefined): string {
    if (value === null || value === undefined) return '—';
    return value.toLocaleString('fr-FR') + ' travailleurs';
  }

  /** Boolean → Oui / Non / —. */
  formatBoolean(value: boolean | null | undefined): string {
    if (value === null || value === undefined) return '—';
    return value ? 'Oui' : 'Non';
  }

  calculate(): void {
    if (!this.formValid()) return;
    if (this.workspaceCountry !== 'BELGIQUE') {
      this.snackBar.open('Outil indisponible pour ce workspace', 'Fermer',
        { duration: 4000, panelClass: 'snack-error' });
      return;
    }
    const request: DroitDeconnexionBeRequest = {
      effectifEntreprise: this.effectifEntreprise()!,
      statutAccord: this.statutAccord()!,
      dateEntreeVigueurInstrument: this.dateEntreeVigueurInstrument(),
      modalitesPratiquesDeconnexionDefinies: this.modalitesPratiquesDeconnexionDefinies()!,
      sensibilisationFormationPrevue: this.sensibilisationFormationPrevue()!,
      modalitesOrganisationTravailDefinies: this.modalitesOrganisationTravailDefinies()!,
      consultationOrganeConcertationEffectuee: this.consultationOrganeConcertationEffectuee()!,
      manquementSignaleCbeOuSpf: this.manquementSignaleCbeOuSpf()!,
    };
    this.calculating.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Conformité droit à la déconnexion analysée', 'OK', { duration: 2500 });
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
        this.effectifEntreprise.set(r.effectifEntreprise);
        this.statutAccord.set(r.statutAccord);
        this.dateEntreeVigueurInstrument.set(r.dateEntreeVigueurInstrument);
        this.modalitesPratiquesDeconnexionDefinies.set(r.modalitesPratiquesDeconnexionDefinies);
        this.sensibilisationFormationPrevue.set(r.sensibilisationFormationPrevue);
        this.modalitesOrganisationTravailDefinies.set(r.modalitesOrganisationTravailDefinies);
        this.consultationOrganeConcertationEffectuee.set(r.consultationOrganeConcertationEffectuee);
        this.manquementSignaleCbeOuSpf.set(r.manquementSignaleCbeOuSpf);
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
