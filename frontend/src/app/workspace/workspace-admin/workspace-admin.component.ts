import { Component, OnInit, signal, ViewChild } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';
import { RouterLink } from '@angular/router';
import { AdminUsageService } from '../../core/services/admin-usage.service';
import { WorkspaceUsageSummary, CaseFileUsageSummary, UserUsageSummary } from '../../core/models/workspace-usage-summary.model';

@Component({
  selector: 'app-workspace-admin',
  standalone: true,
  imports: [
    DecimalPipe,
    MatCardModule, MatTableModule, MatSortModule, MatPaginatorModule,
    MatProgressSpinnerModule, MatIconModule, RouterLink
  ],
  templateUrl: './workspace-admin.component.html',
  styleUrl: './workspace-admin.component.scss'
})
export class WorkspaceAdminComponent implements OnInit {
  @ViewChild('caseFilePaginator') caseFilePaginator!: MatPaginator;
  @ViewChild('userPaginator') userPaginator!: MatPaginator;
  @ViewChild('caseFileSort') caseFileSort!: MatSort;
  @ViewChild('userSort') userSort!: MatSort;

  summary = signal<WorkspaceUsageSummary | null>(null);
  loading = signal(true);
  accessDenied = signal(false);

  caseFileDataSource = new MatTableDataSource<CaseFileUsageSummary>([]);
  userDataSource = new MatTableDataSource<UserUsageSummary>([]);

  readonly caseFileColumns = ['caseFileTitle', 'tokensInput', 'tokensOutput', 'totalCost'];
  readonly userColumns = ['userEmail', 'tokensInput', 'tokensOutput', 'totalCost'];

  constructor(
    private adminUsageService: AdminUsageService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.adminUsageService.getSummary().subscribe({
      next: data => {
        this.summary.set(data);
        this.caseFileDataSource.data = data.byCaseFile;
        this.userDataSource.data = data.byUser;
        this.loading.set(false);
        setTimeout(() => {
          this.caseFileDataSource.paginator = this.caseFilePaginator;
          this.caseFileDataSource.sort = this.caseFileSort;
          this.userDataSource.paginator = this.userPaginator;
          this.userDataSource.sort = this.userSort;
        });
      },
      error: (err: any) => {
        this.loading.set(false);
        if (err.status === 403) {
          this.accessDenied.set(true);
        } else {
          this.snackBar.open('Erreur lors du chargement des données d\'administration', 'Fermer', {
            duration: 4000, panelClass: ['snack-error']
          });
        }
      }
    });
  }
}
