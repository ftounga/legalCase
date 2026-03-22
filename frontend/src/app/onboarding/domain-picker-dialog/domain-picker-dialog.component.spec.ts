import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DomainPickerDialogComponent } from './domain-picker-dialog.component';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

describe('DomainPickerDialogComponent', () => {
  let fixture: ComponentFixture<DomainPickerDialogComponent>;
  let component: DomainPickerDialogComponent;
  let dialogRefSpy: jasmine.SpyObj<MatDialogRef<DomainPickerDialogComponent>>;

  beforeEach(async () => {
    dialogRefSpy = jasmine.createSpyObj('MatDialogRef', ['close']);
    Object.defineProperty(dialogRefSpy, 'disableClose', { writable: true, value: false });

    await TestBed.configureTestingModule({
      imports: [DomainPickerDialogComponent, NoopAnimationsModule],
      providers: [
        { provide: MatDialogRef, useValue: dialogRefSpy },
        { provide: MAT_DIALOG_DATA, useValue: { workspaceName: 'Cabinet Test' } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(DomainPickerDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('affiche 3 tuiles de domaines', () => {
    expect(component.domains.length).toBe(3);
  });

  it('seul DROIT_DU_TRAVAIL est disponible', () => {
    const available = component.domains.filter(d => d.available);
    expect(available.length).toBe(1);
    expect(available[0].key).toBe('DROIT_DU_TRAVAIL');
  });

  it('DROIT_DU_TRAVAIL est sélectionné par défaut', () => {
    expect(component.selected).toBe('DROIT_DU_TRAVAIL');
  });

  it('confirm() ferme la dialog avec le domaine sélectionné', () => {
    component.confirm();
    expect(dialogRefSpy.close).toHaveBeenCalledWith('DROIT_DU_TRAVAIL');
  });
});
