import { Component, Input, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CaseNoteService } from '../../core/services/case-note.service';
import { CaseNote } from '../../core/models/case-note.model';

@Component({
  selector: 'app-case-notes-section',
  standalone: true,
  imports: [
    FormsModule, DatePipe,
    MatCardModule, MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule
  ],
  templateUrl: './case-notes-section.component.html',
  styleUrl: './case-notes-section.component.scss'
})
export class CaseNotesSectionComponent implements OnInit {
  @Input() caseFileId!: string;
  @Input() currentUserEmail: string | null = null;

  notes = signal<CaseNote[]>([]);
  newContent = '';
  editingNoteId = signal<string | null>(null);
  editingContent = '';

  constructor(
    private noteService: CaseNoteService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadNotes();
  }

  loadNotes(): void {
    this.noteService.list(this.caseFileId).subscribe({
      next: notes => this.notes.set(notes),
      error: () => this.snackBar.open('Erreur lors du chargement des notes.', 'Fermer', {
        duration: 4000, panelClass: ['snack-error']
      })
    });
  }

  addNote(): void {
    if (!this.newContent.trim()) return;
    this.noteService.create(this.caseFileId, this.newContent.trim()).subscribe({
      next: note => {
        this.notes.update(notes => [note, ...notes]);
        this.newContent = '';
        this.snackBar.open('Note ajoutée.', 'Fermer', { duration: 3000, panelClass: ['snack-success'] });
      },
      error: () => this.snackBar.open('Erreur lors de l\'ajout.', 'Fermer', {
        duration: 4000, panelClass: ['snack-error']
      })
    });
  }

  startEdit(note: CaseNote): void {
    this.editingNoteId.set(note.id);
    this.editingContent = note.content;
  }

  cancelEdit(): void {
    this.editingNoteId.set(null);
    this.editingContent = '';
  }

  saveEdit(note: CaseNote): void {
    if (!this.editingContent.trim()) return;
    this.noteService.update(this.caseFileId, note.id, this.editingContent.trim()).subscribe({
      next: updated => {
        this.notes.update(notes => notes.map(n => n.id === updated.id ? updated : n));
        this.editingNoteId.set(null);
        this.snackBar.open('Note modifiée.', 'Fermer', { duration: 3000, panelClass: ['snack-success'] });
      },
      error: () => this.snackBar.open('Erreur lors de la modification.', 'Fermer', {
        duration: 4000, panelClass: ['snack-error']
      })
    });
  }

  deleteNote(note: CaseNote): void {
    this.noteService.delete(this.caseFileId, note.id).subscribe({
      next: () => {
        this.notes.update(notes => notes.filter(n => n.id !== note.id));
        this.snackBar.open('Note supprimée.', 'Fermer', { duration: 3000, panelClass: ['snack-success'] });
      },
      error: () => this.snackBar.open('Erreur lors de la suppression.', 'Fermer', {
        duration: 4000, panelClass: ['snack-error']
      })
    });
  }

  isOwn(note: CaseNote): boolean {
    return !!this.currentUserEmail && note.authorEmail === this.currentUserEmail;
  }
}
