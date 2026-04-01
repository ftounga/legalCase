import { TestBed, ComponentFixture } from '@angular/core/testing';
import { CaseFileEditDialogComponent, CaseFileEditDialogData } from './case-file-edit-dialog.component';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { CaseFileService } from '../../core/services/case-file.service';
import { MatSnackBar } from '@angular/material/snack-bar';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { of, throwError } from 'rxjs';
import { CaseFile } from '../../core/models/case-file.model';

const mockCaseFile: CaseFile = {
  id: 'cf1', title: 'Titre modifié', legalDomain: 'DROIT_DU_TRAVAIL',
  description: 'Nouvelle description', status: 'OPEN', createdAt: '2026-03-17T10:00:00Z', lastDocumentDeletedAt: null, riskLevel: null, riskScore: null
};

const dialogData: CaseFileEditDialogData = {
  id: 'cf1',
  title: 'Titre initial',
  description: 'Description initiale'
};

describe('CaseFileEditDialogComponent', () => {
  let fixture: ComponentFixture<CaseFileEditDialogComponent>;
  let component: CaseFileEditDialogComponent;
  let dialogRefSpy: jasmine.SpyObj<MatDialogRef<CaseFileEditDialogComponent>>;
  let caseFileServiceSpy: jasmine.SpyObj<CaseFileService>;
  let snackBarSpy: jasmine.SpyObj<MatSnackBar>;

  beforeEach(async () => {
    dialogRefSpy = jasmine.createSpyObj('MatDialogRef', ['close']);
    caseFileServiceSpy = jasmine.createSpyObj('CaseFileService', ['update']);
    snackBarSpy = jasmine.createSpyObj('MatSnackBar', ['open']);

    await TestBed.configureTestingModule({
      imports: [CaseFileEditDialogComponent],
      providers: [
        { provide: MatDialogRef, useValue: dialogRefSpy },
        { provide: MAT_DIALOG_DATA, useValue: dialogData },
        { provide: CaseFileService, useValue: caseFileServiceSpy },
        { provide: MatSnackBar, useValue: snackBarSpy },
        provideAnimationsAsync()
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(CaseFileEditDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('dialog créé avec données pré-remplies', () => {
    expect(component).toBeTruthy();
    expect(component.form.controls.title.value).toBe('Titre initial');
    expect(component.form.controls.description.value).toBe('Description initiale');
  });

  it('titre vide → bouton submit désactivé (formulaire invalide)', () => {
    component.form.controls.title.setValue('');
    expect(component.form.invalid).toBeTrue();
  });

  it('succès API → dialog fermé avec le dossier mis à jour', () => {
    caseFileServiceSpy.update.and.returnValue(of(mockCaseFile));
    component.form.controls.title.setValue('Titre modifié');
    component.form.controls.description.setValue('Nouvelle description');
    component.submit();
    expect(caseFileServiceSpy.update).toHaveBeenCalledWith('cf1', {
      title: 'Titre modifié',
      description: 'Nouvelle description'
    });
    expect(dialogRefSpy.close).toHaveBeenCalledWith(mockCaseFile);
  });

  it('erreur API → snackbar affiché, dialog non fermé', () => {
    caseFileServiceSpy.update.and.returnValue(throwError(() => ({ status: 500 })));
    component.form.controls.title.setValue('Titre modifié');
    component.submit();
    expect(snackBarSpy.open).toHaveBeenCalled();
    expect(dialogRefSpy.close).not.toHaveBeenCalled();
  });

  it('cancel → ferme la dialog avec null', () => {
    component.cancel();
    expect(dialogRefSpy.close).toHaveBeenCalledWith(null);
  });
});
