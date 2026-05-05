import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import {
  SynthesisShortBlockDialogComponent,
  SynthesisShortBlockDialogData,
} from './synthesis-short-block-dialog.component';

describe('SynthesisShortBlockDialogComponent (F-162 SF-162-06)', () => {
  let fixture: ComponentFixture<SynthesisShortBlockDialogComponent>;

  function setup(data: SynthesisShortBlockDialogData): void {
    TestBed.configureTestingModule({
      imports: [SynthesisShortBlockDialogComponent, NoopAnimationsModule],
      providers: [
        { provide: MAT_DIALOG_DATA, useValue: data },
        { provide: MatDialogRef, useValue: { close: jest.fn() } },
      ],
    });
    fixture = TestBed.createComponent(SynthesisShortBlockDialogComponent);
    fixture.detectChanges();
  }

  // U-3a : rend la liste passée en data.
  it('renders the list provided in data', () => {
    setup({
      title: 'Pièces manquantes',
      icon: 'report_problem',
      items: ['Contrat de travail', 'Bulletin de salaire'],
    });

    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Pièces manquantes');
    expect(text).toContain('Contrat de travail');
    expect(text).toContain('Bulletin de salaire');
    expect(text).toContain('2'); // compteur
  });

  // U-3b : items vide → message "Aucun élément".
  it('renders empty message when items array is empty', () => {
    setup({ title: 'Questions ouvertes', icon: 'help_outline', items: [] });

    expect(fixture.nativeElement.textContent).toContain('Aucun élément');
  });
});
