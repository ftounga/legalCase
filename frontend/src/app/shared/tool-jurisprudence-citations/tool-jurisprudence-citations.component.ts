import { ChangeDetectionStrategy, ChangeDetectorRef, Component, inject, Input, OnChanges, OnInit, SimpleChanges } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';

import { ToolJurisprudenceClientService } from '../../core/services/tool-jurisprudence-client.service';
import { ToolJurisprudenceCitation } from '../../core/models/tool-jurisprudence-citation.model';

/**
 * F-JU-01 / SF-JU-01-04 — composant standalone qui affiche 0 à 3 arrêts
 * mappés pour un outil décisionnel × branche de calcul. Inséré sous le
 * résultat d'un composant outil (dans le modal F-177).
 *
 * Inputs :
 *  - [toolId] (requis) : identifiant `TOOL_REGISTRY`
 *  - [branchActive] (optionnel) : branche de calcul active ; si vide → bloc absent
 *
 * Comportement :
 *  - liste vide → bloc absent du DOM (silence > placeholder)
 *  - changement de `branchActive` → re-fetch automatique
 *  - lien Légifrance ouvre nouvel onglet
 *  - bouton « Signaler » → prompt inline minimaliste + snackbar de confirmation
 *  - mention prudence visible avec date de dernière vérification (la plus récente)
 */
@Component({
  selector: 'app-tool-jurisprudence-citations',
  standalone: true,
  imports: [CommonModule, FormsModule, MatButtonModule, MatIconModule, DatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './tool-jurisprudence-citations.component.html',
  styleUrl: './tool-jurisprudence-citations.component.scss',
})
export class ToolJurisprudenceCitationsComponent implements OnInit, OnChanges {

  @Input({ required: true }) toolId!: string;
  @Input() branchActive: string | null | undefined;

  citations: ToolJurisprudenceCitation[] = [];
  signalingCitationId: string | null = null;
  signalComment = '';

  private readonly client = inject(ToolJurisprudenceClientService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly cdr = inject(ChangeDetectorRef);

  ngOnInit(): void {
    this.fetch();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if ('branchActive' in changes || 'toolId' in changes) {
      this.fetch();
    }
  }

  protected get hasCitations(): boolean {
    return this.citations.length > 0;
  }

  protected get latestVerifiedAt(): string | null {
    if (!this.hasCitations) return null;
    return this.citations
      .map(c => c.lastVerifiedAt)
      .sort()
      .reverse()[0];
  }

  protected openSignal(citationId: string): void {
    this.signalingCitationId = citationId;
    this.signalComment = '';
  }

  protected cancelSignal(): void {
    this.signalingCitationId = null;
    this.signalComment = '';
  }

  protected submitSignal(): void {
    const citationId = this.signalingCitationId;
    if (!citationId) return;
    const comment = this.signalComment.trim() || undefined;
    this.client.signalProblem(this.toolId, citationId, comment).subscribe({
      next: () => {
        this.snackBar.open('Signalement transmis. Merci.', 'OK', { duration: 4000 });
        this.signalingCitationId = null;
        this.signalComment = '';
        this.cdr.markForCheck();
      },
      error: () => {
        this.snackBar.open('Échec du signalement. Réessaie plus tard.', 'OK', { duration: 4000 });
        this.cdr.markForCheck();
      },
    });
  }

  private fetch(): void {
    if (!this.toolId) {
      this.citations = [];
      this.cdr.markForCheck();
      return;
    }
    this.client.findByToolAndBranch(this.toolId, this.branchActive).subscribe({
      next: list => {
        this.citations = list;
        this.cdr.markForCheck();
      },
      error: () => {
        this.citations = [];
        this.cdr.markForCheck();
      },
    });
  }
}
