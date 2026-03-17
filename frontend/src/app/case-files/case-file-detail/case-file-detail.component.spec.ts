import { TestBed, ComponentFixture } from '@angular/core/testing';
import { CaseFileDetailComponent } from './case-file-detail.component';
import { CaseFileService } from '../../core/services/case-file.service';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { of, throwError } from 'rxjs';
import { CaseFile } from '../../core/models/case-file.model';

const mockCaseFile: CaseFile = {
  id: 'cf1', title: 'Dossier A', legalDomain: 'EMPLOYMENT_LAW',
  description: 'Description test', status: 'OPEN', createdAt: '2026-03-17T10:00:00Z'
};

describe('CaseFileDetailComponent', () => {
  let fixture: ComponentFixture<CaseFileDetailComponent>;
  let component: CaseFileDetailComponent;
  let caseFileServiceSpy: jasmine.SpyObj<CaseFileService>;
  let snackBarSpy: jasmine.SpyObj<MatSnackBar>;

  beforeEach(async () => {
    caseFileServiceSpy = jasmine.createSpyObj('CaseFileService', ['getById']);
    snackBarSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    caseFileServiceSpy.getById.and.returnValue(of(mockCaseFile));

    await TestBed.configureTestingModule({
      imports: [CaseFileDetailComponent],
      providers: [
        { provide: CaseFileService, useValue: caseFileServiceSpy },
        { provide: MatSnackBar, useValue: snackBarSpy },
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ id: 'cf1' }) } } },
        provideAnimationsAsync()
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(CaseFileDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('ngOnInit — succès → charge le dossier et passe loading à false', () => {
    expect(caseFileServiceSpy.getById).toHaveBeenCalledWith('cf1');
    expect(component.caseFile()).toEqual(mockCaseFile);
    expect(component.loading()).toBeFalse();
  });

  it('ngOnInit — erreur → affiche snackbar et passe loading à false', async () => {
    caseFileServiceSpy.getById.and.returnValue(throwError(() => new Error('404')));
    component.ngOnInit();
    expect(snackBarSpy.open).toHaveBeenCalled();
    expect(component.loading()).toBeFalse();
  });

  it('statusLabel OPEN → "Ouvert"', () => {
    expect(component.statusLabel('OPEN')).toBe('Ouvert');
  });
});
