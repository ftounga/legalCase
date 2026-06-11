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
  private readonly cdr = inject(ChangeDetectorRef);

  /** Identifiant du dossier, lu du paramètre de route. */
  readonly caseFileId = signal<string>('');
  /** Dossier chargé (pour le titre de page) ; `null` tant que non résolu. */
  readonly caseFile = signal<CaseFile | null>(null);
  /** Chargement de l'entête (le dossier). La section conclusions gère son propre état. */
  readonly loading = signal(true);

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id') ?? '';
    this.caseFileId.set(id);

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
}
