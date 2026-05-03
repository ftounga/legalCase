import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import {
  PrefillDiffDialogComponent,
  PrefillDiffDialogData,
  PrefillDiffEntry,
} from './prefill-diff-dialog.component';

describe('PrefillDiffDialogComponent', () => {
  let fixture: ComponentFixture<PrefillDiffDialogComponent>;
  let dialogRefCloseSpy: jest.Mock;

  function configure(data: PrefillDiffDialogData): void {
    dialogRefCloseSpy = jest.fn();
    TestBed.configureTestingModule({
      imports: [PrefillDiffDialogComponent, NoopAnimationsModule],
      providers: [
        { provide: MatDialogRef, useValue: { close: dialogRefCloseSpy } },
        { provide: MAT_DIALOG_DATA, useValue: data },
      ],
    });
    fixture = TestBed.createComponent(PrefillDiffDialogComponent);
    fixture.detectChanges();
  }

  it('rend la liste des entries avec icône, label, delta', () => {
    const entries: PrefillDiffEntry[] = [
      { toolId: 't1', label: 'Indemnité licenciement', icon: 'gavel', delta: 3 },
      { toolId: 't2', label: 'Calculateur ancienneté', icon: 'calculate', delta: 1 },
    ];
    configure({ entries });

    const items = fixture.nativeElement.querySelectorAll('.entry');
    expect(items).toHaveLength(2);
    expect(items[0].querySelector('.entry__label').textContent.trim()).toBe('Indemnité licenciement');
    expect(items[0].querySelector('.entry__delta').textContent.trim()).toBe('+3 champs');
    expect(items[1].querySelector('.entry__delta').textContent.trim()).toBe('+1 champ');
  });

  it('affiche message vide si aucune entry', () => {
    configure({ entries: [] });
    expect(fixture.nativeElement.querySelector('.empty')).not.toBeNull();
    expect(fixture.nativeElement.querySelectorAll('.entry')).toHaveLength(0);
  });

  it('clic sur Fermer appelle dialogRef.close', () => {
    configure({ entries: [{ toolId: 't1', label: 'X', icon: 'x', delta: 1 }] });
    const closeBtn = Array.from<HTMLButtonElement>(fixture.nativeElement.querySelectorAll('button'))
      .find(b => b.textContent?.trim() === 'Fermer');
    expect(closeBtn).toBeDefined();
    closeBtn!.click();
    expect(dialogRefCloseSpy).toHaveBeenCalledTimes(1);
  });
});
