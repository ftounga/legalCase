import { Component, DestroyRef, inject, OnInit } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { NgClass, DatePipe } from '@angular/common';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CaseFileService } from '../../core/services/case-file.service';
import { WorkspaceService } from '../../core/services/workspace.service';
import { TourService } from '../../core/services/tour.service';
import { CaseFile } from '../../core/models/case-file.model';
import { CaseFileCreateDialogComponent } from '../case-file-create-dialog/case-file-create-dialog.component';
import { fadeInUp, listStagger } from '../../shared/animations';

@Component({
  selector: 'app-case-files-list',
  standalone: true,
  imports: [
    RouterLink, NgClass, DatePipe,
    MatTableModule, MatPaginatorModule, MatSortModule, MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatSelectModule, MatButtonToggleModule
  ],
  templateUrl: './case-files-list.component.html',
  styleUrl: './case-files-list.component.scss',
  animations: [fadeInUp, listStagger],
  host: { '[@fadeInUp]': '' },
})
export class CaseFilesListComponent implements OnInit {
  displayedColumns = ['title', 'legalDomain', 'status', 'createdAt', 'actions'];
  dataSource: CaseFile[] = [];
  totalElements = 0;
  pageSize = 20;
  pageIndex = 0;
  searchTerm = '';
  sortColumn = '';
  sortDirection: 'asc' | 'desc' | '' = '';
  filterStatus = '';
  filterDomain = '';

  get filteredDataSource(): CaseFile[] {
    const term = this.searchTerm.trim().toLowerCase();
    let filtered = term
      ? this.dataSource.filter(cf => cf.title.toLowerCase().includes(term))
      : [...this.dataSource];

    if (this.filterStatus) filtered = filtered.filter(cf => cf.status === this.filterStatus);
    if (this.filterDomain) filtered = filtered.filter(cf => cf.legalDomain === this.filterDomain);

    if (!this.sortColumn || !this.sortDirection) return filtered;

    return filtered.sort((a, b) => {
      const dir = this.sortDirection === 'asc' ? 1 : -1;
      const col = this.sortColumn as keyof CaseFile;
      const va = (a[col] ?? '') as string;
      const vb = (b[col] ?? '') as string;
      return va.localeCompare(vb) * dir;
    });
  }

  private destroyRef = inject(DestroyRef);

  constructor(
    private caseFileService: CaseFileService,
    private workspaceService: WorkspaceService,
    private tourService: TourService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadCaseFiles();
    this.maybeShowWizard();
    this.workspaceService.workspaceSwitched$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.pageIndex = 0;
        this.searchTerm = '';
        this.filterStatus = '';
        this.filterDomain = '';
        this.loadCaseFiles();
      });
  }

  private maybeShowWizard(): void {
    this.workspaceService.getCurrentWorkspace().subscribe({
      next: ws => this.tourService.start(ws.id),
      error: () => {}
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

  onSearch(value: string): void {
    this.searchTerm = value;
  }

  onSort(sort: Sort): void {
    this.sortColumn = sort.active;
    this.sortDirection = sort.direction;
  }

  onFilterStatus(value: string): void {
    this.filterStatus = value;
  }

  onFilterDomain(value: string): void {
    this.filterDomain = value;
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
