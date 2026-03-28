import { Component, Inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CaseFileShareService } from '../../core/services/case-file-share.service';
import { ShareResponse } from '../../core/models/share.model';

export interface ShareDialogData {
  caseFileId: string;
  caseFileTitle: string;
}

@Component({
  selector: 'app-share-dialog',
  standalone: true,
  imports: [
    FormsModule, DatePipe,
    MatDialogModule, MatButtonModule, MatIconModule,
    MatProgressSpinnerModule, MatSelectModule
  ],
  templateUrl: './share-dialog.component.html',
  styleUrl: './share-dialog.component.scss'
})
export class ShareDialogComponent {
  expiresInDays = 7;
  generating = signal(false);
  generatedLink = signal<ShareResponse | null>(null);
  activeShares = signal<ShareResponse[]>([]);
  loadingShares = signal(true);
  revokingId = signal<string | null>(null);

  constructor(
    @Inject(MAT_DIALOG_DATA) readonly data: ShareDialogData,
    private shareService: CaseFileShareService,
    private snackBar: MatSnackBar
  ) {
    this.loadShares();
  }

  private loadShares(): void {
    this.shareService.listShares(this.data.caseFileId).subscribe({
      next: shares => {
        this.activeShares.set(shares);
        this.loadingShares.set(false);
      },
      error: () => this.loadingShares.set(false)
    });
  }

  generateLink(): void {
    this.generating.set(true);
    this.shareService.createShare(this.data.caseFileId, this.expiresInDays).subscribe({
      next: share => {
        this.generatedLink.set(share);
        this.activeShares.update(list => [share, ...list]);
        this.generating.set(false);
      },
      error: () => {
        this.snackBar.open('Erreur lors de la génération du lien.', 'Fermer', { duration: 4000 });
        this.generating.set(false);
      }
    });
  }

  copyLink(url: string): void {
    navigator.clipboard.writeText(url).then(() => {
      this.snackBar.open('Lien copié !', 'Fermer', { duration: 2000, panelClass: ['snack-success'] });
    });
  }

  revokeShare(share: ShareResponse): void {
    this.revokingId.set(share.id);
    this.shareService.revokeShare(this.data.caseFileId, share.id).subscribe({
      next: () => {
        this.activeShares.update(list => list.filter(s => s.id !== share.id));
        if (this.generatedLink()?.id === share.id) this.generatedLink.set(null);
        this.revokingId.set(null);
      },
      error: () => {
        this.snackBar.open('Erreur lors de la révocation.', 'Fermer', { duration: 4000 });
        this.revokingId.set(null);
      }
    });
  }
}
