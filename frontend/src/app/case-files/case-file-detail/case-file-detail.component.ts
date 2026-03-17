import { Component, OnInit, signal, ViewChild, ElementRef } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { DatePipe, DecimalPipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CaseFileService } from '../../core/services/case-file.service';
import { DocumentService } from '../../core/services/document.service';
import { CaseFile } from '../../core/models/case-file.model';
import { Document } from '../../core/models/document.model';

@Component({
  selector: 'app-case-file-detail',
  standalone: true,
  imports: [
    RouterLink, DatePipe, DecimalPipe,
    MatCardModule, MatButtonModule, MatIconModule,
    MatTableModule, MatProgressSpinnerModule
  ],
  templateUrl: './case-file-detail.component.html',
  styleUrl: './case-file-detail.component.scss'
})
export class CaseFileDetailComponent implements OnInit {
  @ViewChild('fileInput') fileInput!: ElementRef<HTMLInputElement>;

  caseFile = signal<CaseFile | null>(null);
  documents = signal<Document[]>([]);
  loading = signal(true);
  uploading = signal(false);

  readonly docColumns = ['name', 'type', 'size', 'date', 'actions'];

  constructor(
    private route: ActivatedRoute,
    private caseFileService: CaseFileService,
    private documentService: DocumentService,
    private snackBar: MatSnackBar
  ) {}

  downloadUrl(doc: Document): string {
    return this.documentService.downloadUrl(this.caseFile()!.id, doc.id);
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id')!;
    this.caseFileService.getById(id).subscribe({
      next: cf => {
        this.caseFile.set(cf);
        this.loading.set(false);
        this.loadDocuments(id);
      },
      error: () => {
        this.loading.set(false);
        this.snackBar.open('Dossier introuvable', 'Fermer', {
          duration: 4000, panelClass: ['snack-error']
        });
      }
    });
  }

  loadDocuments(caseFileId: string): void {
    this.documentService.list(caseFileId).subscribe({
      next: docs => this.documents.set(docs),
      error: () => this.snackBar.open('Erreur lors du chargement des documents', 'Fermer', {
        duration: 4000, panelClass: ['snack-error']
      })
    });
  }

  triggerUpload(): void {
    this.fileInput.nativeElement.click();
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    const caseFileId = this.caseFile()!.id;
    this.uploading.set(true);
    this.documentService.upload(caseFileId, file).subscribe({
      next: doc => {
        this.documents.update(docs => [doc, ...docs]);
        this.uploading.set(false);
        this.snackBar.open('Document ajouté', 'Fermer', {
          duration: 3000, panelClass: ['snack-success']
        });
        input.value = '';
      },
      error: () => {
        this.uploading.set(false);
        this.snackBar.open("Erreur lors de l'upload. Vérifiez le type et la taille du fichier (max 50 Mo).", 'Fermer', {
          duration: 5000, panelClass: ['snack-error']
        });
        input.value = '';
      }
    });
  }

  formatSize(bytes: number): string {
    if (bytes < 1024) return `${bytes} o`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} Ko`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} Mo`;
  }

  statusLabel(status: string): string {
    return status === 'OPEN' ? 'Ouvert' : status;
  }
}
