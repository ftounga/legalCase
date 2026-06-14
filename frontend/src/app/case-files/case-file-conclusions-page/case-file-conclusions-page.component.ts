import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  OnInit,
  inject,
  signal,
} from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ConclusionsSectionComponent } from '../conclusions-section/conclusions-section.component';
import { CaseFileService } from '../../core/services/case-file.service';
import { CaseFile } from '../../core/models/case-file.model';
import { ContradictoireService } from '../../core/services/contradictoire.service';
import { ContradictoireRound } from '../../core/models/contradictoire.model';

/**
 * F-267 / SF-267-01 — Page dédiée « Projet de conclusions ».
 *
 * Sort le module conclusions de l'onglet Décision (où il était empilé, serré,
 * sous le tableau de bord) vers une page pleine largeur, soignée et aérée :
 * en-tête de page (retour dossier + titre + nom du dossier) puis l'acte présenté
 * comme une feuille centrée à largeur de lecture confortable.
 *
 * Wrapper léger : il ne fait que lire le `:id` de route, charger le dossier pour
 * obtenir le titre, puis déléguer TOUTE la logique (génération, versions,
 * édition F-264, co-rédaction F-265, export F-266, prérequis, alerte F-258) au
 * composant autonome `<app-conclusions-section>`.
 *
 * Standalone, OnPush + signals. L'état étant muté dans `subscribe()`, un
 * `ChangeDetectorRef.markForCheck()` est appelé dans `next` ET `error`
 * (cf. mémoire `feedback_onpush_subscribe_markforcheck`).
 */
@Component({
  selector: 'app-case-file-conclusions-page',
  standalone: true,
  imports: [
    RouterLink,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    ConclusionsSectionComponent,
  ],
  templateUrl: './case-file-conclusions-page.component.html',
  styleUrl: './case-file-conclusions-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CaseFileConclusionsPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly caseFileService = inject(CaseFileService);
  private readonly contradictoireService = inject(ContradictoireService);
  private readonly cdr = inject(ChangeDetectorRef);

  /** Identifiant du dossier, lu du paramètre de route. */
  readonly caseFileId = signal<string>('');
  /** Dossier chargé (pour le titre de page) ; `null` tant que non résolu. */
  readonly caseFile = signal<CaseFile | null>(null);
  /** Chargement de l'entête (le dossier). La section conclusions gère son propre état. */
  readonly loading = signal(true);

  /**
   * SF-282-03 (Part A) — Id de version à pré-sélectionner, lu du queryParam
   * `?version=` (chip « Conclusions v{n} » de la frise F-282). `null` = défaut.
   */
  readonly initialVersionId = signal<string | null>(null);

  /**
   * SF-282-03 (Part B) — Id du round contradictoire à rattacher à la version
   * générée, lu du queryParam `?roundId=` (bouton « Générer ma réplique »).
   * `null` = pas d'auto-rattachement demandé.
   */
  readonly roundId = signal<string | null>(null);

  /**
   * SF-282-03 (Part B) — vrai une fois l'auto-rattachement effectué pour ce
   * chargement de page : empêche un second PUT si plusieurs générations
   * aboutissent pendant la session (le rattachement est « one-shot »).
   */
  private autoLinked = false;

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id') ?? '';
    this.caseFileId.set(id);

    // SF-282-03 — paramètres de pilotage venant de la frise F-282.
    const qp = this.route.snapshot.queryParamMap;
    this.initialVersionId.set(qp.get('version'));
    this.roundId.set(qp.get('roundId'));

    if (!id) {
      this.loading.set(false);
      return;
    }

    // On charge le dossier pour afficher son titre dans l'en-tête. Si l'appel
    // échoue, on n'empêche PAS l'affichage de la section conclusions : elle est
    // autonome et le backend gate l'accès. Fail-open sur l'en-tête uniquement.
    this.caseFileService.getById(id).subscribe({
      next: cf => {
        this.caseFile.set(cf);
        this.loading.set(false);
        this.cdr.markForCheck();
      },
      error: () => {
        this.loading.set(false);
        this.cdr.markForCheck();
      },
    });
  }

  /**
   * F-258 — Réponse à `viewToolsRequested` : ramène l'avocat à l'onglet
   * Décision (outils décisionnels) du dossier pour calculer les outils manquants.
   */
  onViewToolsRequested(): void {
    this.router.navigate(['/case-files', this.caseFileId()], {
      queryParams: { section: 'decision' },
    });
  }

  /**
   * SF-282-03 (Part B) — Auto-rattachement du round contradictoire à la version
   * de conclusions qui vient d'aboutir. Déclenché par l'événement
   * `generationCompleted` de `<app-conclusions-section>`. Conditions strictes :
   *  - un `roundId` a été passé en queryParam (bouton « Générer ma réplique ») ;
   *  - on n'a pas déjà rattaché pour ce chargement de page (`autoLinked`) ;
   *  - le round n'a PAS déjà de source (on ne JAMAIS écrase un rattachement
   *    manuel) — vérifié en lisant la timeline (GET) avant le PUT.
   *
   * Le PUT renvoie l'intégralité des champs du round + `sourceConclusionId`
   * pour respecter le contrat backend (PUT = remplacement). En cas d'échec,
   * on reste silencieux (le rattachement auto est un confort, pas un blocage) :
   * l'avocat peut toujours rattacher manuellement depuis la frise.
   *
   * Cette méthode ne touche PAS la génération : elle ne fait que lire la
   * timeline et émettre un `PUT round` après-coup.
   */
  onGenerationCompleted(versionId: string): void {
    const roundId = this.roundId();
    if (!roundId || this.autoLinked || !versionId) {
      return;
    }
    const caseFileId = this.caseFileId();
    this.contradictoireService.timeline(caseFileId).subscribe({
      next: timeline => {
        const round = timeline.rounds.find(r => r.id === roundId);
        // Round introuvable, OU déjà une source (manuelle ou auto) → on ne fait
        // rien (jamais d'écrasement). On marque `autoLinked` pour ne pas
        // re-tenter inutilement à chaque génération de la session.
        if (
          !round ||
          round.sourceConclusionId ||
          round.sourceDocumentId
        ) {
          this.autoLinked = true;
          this.cdr.markForCheck();
          return;
        }
        this.autoLinked = true;
        this.contradictoireService
          .update(caseFileId, roundId, this.toRoundInput(round, versionId))
          .subscribe({
            next: () => this.cdr.markForCheck(),
            // Confort, pas blocage : échec silencieux (rattachement manuel reste possible).
            error: () => this.cdr.markForCheck(),
          });
      },
      error: () => this.cdr.markForCheck(),
    });
  }

  /**
   * SF-282-03 — Construit la charge utile de PUT à partir d'un round existant,
   * en y ajoutant le `sourceConclusionId` de la version générée (le round ciblé
   * est forcément `OURS` côté frise → seule la source conclusion est légale).
   */
  private toRoundInput(round: ContradictoireRound, conclusionId: string) {
    return {
      party: round.party,
      label: round.label,
      datedAt: round.datedAt,
      responseDueAt: round.responseDueAt,
      sourceDocumentId: null,
      sourceConclusionId: conclusionId,
    };
  }
}
