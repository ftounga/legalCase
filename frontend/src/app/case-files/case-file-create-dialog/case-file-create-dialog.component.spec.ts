import { TestBed, ComponentFixture } from '@angular/core/testing';
import { CaseFileCreateDialogComponent } from './case-file-create-dialog.component';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { CaseFileService } from '../../core/services/case-file.service';
import { MatSnackBar } from '@angular/material/snack-bar';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { of, throwError } from 'rxjs';
import { CaseFile } from '../../core/models/case-file.model';

const mockCaseFile: CaseFile = {
  id: 'cf1', title: 'Test', legalDomain: 'EMPLOYMENT_LAW',
  description: null, status: 'OPEN', createdAt: '2026-03-17T10:00:00Z'
};

describe('CaseFileCreateDialogComponent', () => {
  let fixture: ComponentFixture<CaseFileCreateDialogComponent>;
  let component: CaseFileCreateDialogComponent;
  let dialogRefSpy: jasmine.SpyObj<MatDialogRef<CaseFileCreateDialogComponent>>;
  let caseFileServiceSpy: jasmine.SpyObj<CaseFileService>;
  let snackBarSpy: jasmine.SpyObj<MatSnackBar>;

  beforeEach(async () => {
    dialogRefSpy = jasmine.createSpyObj('MatDialogRef', ['close']);
    caseFileServiceSpy = jasmine.createSpyObj('CaseFileService', ['create']);
    snackBarSpy = jasmine.createSpyObj('MatSnackBar', ['open']);

    await TestBed.configureTestingModule({
      imports: [CaseFileCreateDialogComponent],
      providers: [
        { provide: MatDialogRef, useValue: dialogRefSpy },
        { provide: MAT_DIALOG_DATA, useValue: {} },
        { provide: CaseFileService, useValue: caseFileServiceSpy },
        { provide: MatSnackBar, useValue: snackBarSpy },
        provideAnimationsAsync()
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(CaseFileCreateDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('formulaire invalide si titre vide', () => {
    component.form.controls.title.setValue('');
    expect(component.form.invalid).toBeTrue();
  });

  it('formulaire valide si titre renseigné', () => {
    component.form.controls.title.setValue('Licenciement Dupont');
    expect(component.form.valid).toBeTrue();
  });

  it('submit — succès → ferme la dialog avec le dossier créé', () => {
    caseFileServiceSpy.create.and.returnValue(of(mockCaseFile));
    component.form.controls.title.setValue('Licenciement Dupont');
    component.submit();
    expect(caseFileServiceSpy.create).toHaveBeenCalledWith({
      title: 'Licenciement Dupont',
      legalDomain: 'EMPLOYMENT_LAW',
      description: undefined
    });
    expect(dialogRefSpy.close).toHaveBeenCalledWith(mockCaseFile);
  });

  it('submit — erreur API → affiche snackbar, ne ferme pas la dialog', () => {
    caseFileServiceSpy.create.and.returnValue(throwError(() => new Error('500')));
    component.form.controls.title.setValue('Licenciement Dupont');
    component.submit();
    expect(snackBarSpy.open).toHaveBeenCalled();
    expect(dialogRefSpy.close).not.toHaveBeenCalled();
  });

  it('cancel → ferme la dialog avec null', () => {
    component.cancel();
    expect(dialogRefSpy.close).toHaveBeenCalledWith(null);
  });
});
