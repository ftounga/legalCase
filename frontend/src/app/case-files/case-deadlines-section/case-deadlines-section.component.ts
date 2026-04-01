import { Component, computed, Input, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CaseDeadlineService } from '../../core/services/case-deadline.service';
import { CaseDeadline } from '../../core/models/case-deadline.model';

@Component({
  selector: 'app-case-deadlines-section',
  standalone: true,
  imports: [
    FormsModule, DatePipe,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule
  ],
  templateUrl: './case-deadlines-section.component.html',
  styleUrl: './case-deadlines-section.component.scss'
})
export class CaseDeadlinesSectionComponent implements OnInit {
  @Input() caseFileId!: string;

  deadlines = signal<CaseDeadline[]>([]);
  collapsed = signal(true);
  validating = signal<string | null>(null);

  confirmedDeadlines = computed(() =>
    this.deadlines().filter(d => d.source === 'MANUAL' || d.aiStatus === 'ACCEPTED')
  );

  pendingAiDeadlines = computed(() =>
    this.deadlines().filter(d => d.source === 'AI' && d.aiStatus === 'PENDING')
  );

  toggleCollapsed(): void { this.collapsed.update(v => !v); }
  newLabel = '';
  newDueDate = '';
  editingDeadlineId = signal<string | null>(null);
  editingLabel = '';
  editingDueDate = '';

  constructor(
    private deadlineService: CaseDeadlineService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadDeadlines();
  }

  loadDeadlines(): void {
    this.deadlineService.list(this.caseFileId).subscribe({
      next: deadlines => this.deadlines.set(deadlines),
      error: () => this.snackBar.open('Erreur lors du chargement des délais.', 'Fermer', {
        duration: 4000, panelClass: ['snack-error']
      })
    });
  }

  canAdd(): boolean {
    return !!this.newLabel.trim() && !!this.newDueDate;
  }

  addDeadline(): void {
    if (!this.canAdd()) return;
    this.deadlineService.create(this.caseFileId, this.newLabel.trim(), this.newDueDate).subscribe({
      next: d => {
        this.deadlines.update(list => [...list, d].sort((a, b) => a.dueDate.localeCompare(b.dueDate)));
        this.newLabel = '';
        this.newDueDate = '';
        this.snackBar.open('Délai ajouté.', 'Fermer', { duration: 3000, panelClass: ['snack-success'] });
      },
      error: () => this.snackBar.open('Erreur lors de l\'ajout.', 'Fermer', {
        duration: 4000, panelClass: ['snack-error']
      })
    });
  }

  startEdit(d: CaseDeadline): void {
    this.editingDeadlineId.set(d.id);
    this.editingLabel = d.label;
    this.editingDueDate = d.dueDate;
  }

  cancelEdit(): void {
    this.editingDeadlineId.set(null);
    this.editingLabel = '';
    this.editingDueDate = '';
  }

  saveEdit(d: CaseDeadline): void {
    if (!this.editingLabel.trim() || !this.editingDueDate) return;
    this.deadlineService.update(this.caseFileId, d.id, this.editingLabel.trim(), this.editingDueDate).subscribe({
      next: updated => {
        this.deadlines.update(list =>
          list.map(x => x.id === updated.id ? updated : x)
              .sort((a, b) => a.dueDate.localeCompare(b.dueDate))
        );
        this.editingDeadlineId.set(null);
        this.snackBar.open('Délai modifié.', 'Fermer', { duration: 3000, panelClass: ['snack-success'] });
      },
      error: () => this.snackBar.open('Erreur lors de la modification.', 'Fermer', {
        duration: 4000, panelClass: ['snack-error']
      })
    });
  }

  deleteDeadline(d: CaseDeadline): void {
    this.deadlineService.delete(this.caseFileId, d.id).subscribe({
      next: () => {
        this.deadlines.update(list => list.filter(x => x.id !== d.id));
        this.snackBar.open('Délai supprimé.', 'Fermer', { duration: 3000, panelClass: ['snack-success'] });
      },
      error: () => this.snackBar.open('Erreur lors de la suppression.', 'Fermer', {
        duration: 4000, panelClass: ['snack-error']
      })
    });
  }

  daysUntil(dueDate: string): number {
    const due = new Date(dueDate + 'T12:00:00');
    const today = new Date();
    today.setHours(12, 0, 0, 0);
    return Math.ceil((due.getTime() - today.getTime()) / 86400000);
  }

  deadlineClass(dueDate: string): string {
    const days = this.daysUntil(dueDate);
    if (days < 0) return 'deadline--past';
    if (days <= 15) return 'deadline--soon';
    return 'deadline--ok';
  }

  daysLabel(dueDate: string): string {
    const days = this.daysUntil(dueDate);
    if (days === 0) return 'Aujourd\'hui';
    if (days < 0) return `J+${Math.abs(days)} (dépassé)`;
    return `J-${days}`;
  }

  acceptDeadline(deadline: CaseDeadline): void {
    this.validating.set(deadline.id);
    this.deadlineService.validateDeadline(this.caseFileId, deadline.id, 'ACCEPT').subscribe({
      next: updated => {
        if (updated) {
          this.deadlines.update(list => list.map(d => d.id === deadline.id ? updated : d));
        }
        this.snackBar.open('Délai accepté.', 'Fermer', { duration: 3000, panelClass: ['snack-success'] });
      },
      error: () => {
        this.snackBar.open('Erreur lors de la validation du délai.', 'Fermer', {
          duration: 4000, panelClass: ['snack-error']
        });
      },
      complete: () => this.validating.set(null)
    });
  }

  rejectDeadline(deadline: CaseDeadline): void {
    this.validating.set(deadline.id);
    this.deadlineService.validateDeadline(this.caseFileId, deadline.id, 'REJECT').subscribe({
      next: () => {
        this.deadlines.update(list => list.filter(d => d.id !== deadline.id));
        this.snackBar.open('Délai rejeté.', 'Fermer', { duration: 3000, panelClass: ['snack-success'] });
      },
      error: () => {
        this.snackBar.open('Erreur lors de la validation du délai.', 'Fermer', {
          duration: 4000, panelClass: ['snack-error']
        });
      },
      complete: () => this.validating.set(null)
    });
  }
}
