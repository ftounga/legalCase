import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of } from 'rxjs';
import { CaseNotesSectionComponent } from './case-notes-section.component';
import { CaseNoteService } from '../../core/services/case-note.service';
import { CaseNote } from '../../core/models/case-note.model';

const NOTE_OWN: CaseNote = {
  id: 'n-1', content: 'Première note', authorEmail: 'alice@example.com',
  createdAt: '2026-03-01T10:00:00Z', updatedAt: '2026-03-01T10:00:00Z'
};

const NOTE_OTHER: CaseNote = {
  id: 'n-2', content: 'Note de Bob', authorEmail: 'bob@example.com',
  createdAt: '2026-03-01T11:00:00Z', updatedAt: '2026-03-01T11:00:00Z'
};

describe('CaseNotesSectionComponent', () => {
  let fixture: ComponentFixture<CaseNotesSectionComponent>;
  let component: CaseNotesSectionComponent;
  let noteServiceSpy: jasmine.SpyObj<CaseNoteService>;

  beforeEach(async () => {
    noteServiceSpy = jasmine.createSpyObj('CaseNoteService', ['list', 'create', 'update', 'delete']);
    noteServiceSpy.list.and.returnValue(of([]));

    await TestBed.configureTestingModule({
      imports: [CaseNotesSectionComponent, NoopAnimationsModule],
      providers: [
        { provide: CaseNoteService, useValue: noteServiceSpy },
        { provide: MatSnackBar, useValue: { open: () => {} } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CaseNotesSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'cf-1';
    component.currentUserEmail = 'alice@example.com';
    fixture.detectChanges();
  });

  it('U-01: shows empty state when no notes (expanded)', () => {
    component.toggleCollapsed();
    fixture.detectChanges();
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('.notes-empty')?.textContent).toContain('Aucune note');
  });

  it('U-02: renders note items when notes exist (expanded)', () => {
    noteServiceSpy.list.and.returnValue(of([NOTE_OWN]));
    component.loadNotes();
    component.toggleCollapsed();
    fixture.detectChanges();
    const items = fixture.nativeElement.querySelectorAll('.note-item');
    expect(items.length).toBe(1);
    expect(items[0].textContent).toContain('Première note');
  });

  it('U-03: add button disabled when textarea empty, enabled when filled', () => {
    component.toggleCollapsed();
    fixture.detectChanges();
    const btn: HTMLButtonElement = fixture.nativeElement.querySelector('.new-note-actions button');
    expect(btn.disabled).toBeTrue();
    component.newContent = 'une note';
    fixture.detectChanges();
    expect(btn.disabled).toBeFalse();
  });

  it('U-04: edit/delete buttons visible only for own notes', () => {
    noteServiceSpy.list.and.returnValue(of([NOTE_OWN, NOTE_OTHER]));
    component.loadNotes();
    component.toggleCollapsed();
    fixture.detectChanges();
    const items = fixture.nativeElement.querySelectorAll('.note-item');
    expect(items[0].querySelector('.note-actions')).toBeTruthy();
    expect(items[1].querySelector('.note-actions')).toBeFalsy();
  });

  it('U-05: saveEdit calls noteService.update with trimmed content', () => {
    noteServiceSpy.list.and.returnValue(of([NOTE_OWN]));
    noteServiceSpy.update.and.returnValue(of({ ...NOTE_OWN, content: 'Modifiée' }));
    component.loadNotes();
    fixture.detectChanges();
    component.startEdit(NOTE_OWN);
    component.editingContent = '  Modifiée  ';
    component.saveEdit(NOTE_OWN);
    expect(noteServiceSpy.update).toHaveBeenCalledWith('cf-1', 'n-1', 'Modifiée');
  });

  // ── Collapsible (SF-71-01) ────────────────────────────────────────────────

  it('SF71-U-04: section repliée par défaut — contenu masqué', () => {
    expect(component.collapsed()).toBeTrue();
    const noteField = fixture.nativeElement.querySelector('.new-note-field');
    expect(noteField).toBeNull();
  });

  it('SF71-U-05: après toggleCollapsed() — contenu visible, badge masqué', () => {
    noteServiceSpy.list.and.returnValue(of([NOTE_OWN]));
    component.loadNotes();
    component.toggleCollapsed();
    fixture.detectChanges();
    expect(component.collapsed()).toBeFalse();
    expect(fixture.nativeElement.querySelector('.new-note-field')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('.section-badge')).toBeNull();
  });

  it('SF71-U-06: double toggle — retour à l\'état replié, badge visible', () => {
    noteServiceSpy.list.and.returnValue(of([NOTE_OWN, NOTE_OTHER]));
    component.loadNotes();
    component.toggleCollapsed();
    component.toggleCollapsed();
    fixture.detectChanges();
    expect(component.collapsed()).toBeTrue();
    const badge = fixture.nativeElement.querySelector('.section-badge');
    expect(badge).toBeTruthy();
    expect(badge.textContent).toContain('2 notes');
  });
});
