import { Component, DestroyRef, inject, OnInit } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { NgClass, DatePipe } from '@angular/common';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CaseFileService } from '../../core/services/case-file.service';
import { WorkspaceService } from '../../core/services/workspace.service';
import { CaseFile } from '../../core/models/case-file.model';
import { CaseFileCreateDialogComponent } from '../case-file-create-dialog/case-file-create-dialog.component';

@Component({
  selector: 'app-case-files-list',
  standalone: true,
  imports: [
    RouterLink, NgClass, DatePipe,
    MatTableModule, MatPaginatorModule, MatButtonModule, MatIconModule
  ],
  templateUrl: './case-files-list.component.html',
  styleUrl: './case-files-list.component.scss'
})
export class CaseFilesListComponent implements OnInit {
  displayedColumns = ['title', 'legalDomain', 'status', 'createdAt', 'actions'];
  dataSource: CaseFile[] = [];
  totalElements = 0;
  pageSize = 20;
  pageIndex = 0;

  private destroyRef = inject(DestroyRef);

  constructor(
    private caseFileService: CaseFileService,
    private workspaceService: WorkspaceService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadCaseFiles();
    this.workspaceService.workspaceSwitched$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.pageIndex = 0;
        this.loadCaseFiles();
      });
  }

  loadCaseFiles(): void {
    this.caseFileService.list(this.pageIndex, this.pageSize).subscribe({
      next: page => {
        this.dataSource = page.content;
        this.totalElements = page.totalElements;
      },
      error: () => this.snackBar.open('Erreur lors du chargement des dossiers', 'Fermer', {
        duration: 4000,
        panelClass: ['snack-error']
      })
    });
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadCaseFiles();
  }

  openCreateDialog(): void {
    const ref = this.dialog.open(CaseFileCreateDialogComponent, { width: '480px' });
    ref.afterClosed().subscribe(result => {
      if (result) {
        this.loadCaseFiles();
        this.snackBar.open('Dossier créé avec succès', 'Fermer', {
          duration: 4000,
          panelClass: ['snack-success']
        });
      }
    });
  }

  statusLabel(status: string): string {
    if (status === 'OPEN') return 'Ouvert';
    if (status === 'CLOSED') return 'Clôturé';
    return status;
  }

  statusClass(status: string): string {
    return status === 'OPEN' ? 'badge--success' : 'badge--neutral';
  }
}
