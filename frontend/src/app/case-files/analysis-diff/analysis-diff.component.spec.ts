import { TestBed } from '@angular/core/testing';
import { ComponentFixture } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute, Router } from '@angular/router';
import { signal } from '@angular/core';
import { of, throwError } from 'rxjs';
import { MatSnackBar } from '@angular/material/snack-bar';

import { AnalysisDiffComponent } from './analysis-diff.component';
import { CaseFileService } from '../../core/services/case-file.service';
import { CaseAnalysisService } from '../../core/services/case-analysis.service';
import { AnalysisDiff, CaseAnalysisVersionSummary } from '../../core/models/case-analysis.model';

const MOCK_VERSIONS: CaseAnalysisVersionSummary[] = [
  { id: 'v1-id', version: 1, analysisType: 'STANDARD', updatedAt: '2024-01-01T00:00:00Z' },
  { id: 'v2-id', version: 2, analysisType: 'ENRICHED', updatedAt: '2024-02-01T00:00:00Z' },
];

const MOCK_DIFF: AnalysisDiff = {
  from: { id: 'v1-id', version: 1, analysisType: 'STANDARD', updatedAt: '2024-01-01T00:00:00Z' },
  to:   { id: 'v2-id', version: 2, analysisType: 'ENRICHED', updatedAt: '2024-02-01T00:00:00Z' },
  faits: { added: ['Nouveau fait'], removed: ['Ancien fait'], unchanged: ['Fait commun'] },
  pointsJuridiques: { added: [], removed: [], unchanged: ['Art. L1234'] },
  risques: { added: [], removed: [], unchanged: [] },
  questionsOuvertes: { added: [], removed: [], unchanged: [] },
  timeline: {
    added: [{ date: '2024-06-01', evenement: 'Jugement' }],
    removed: [],
    unchanged: [{ date: '2024-01-01', evenement: 'Embauche' }]
  }
};

describe('AnalysisDiffComponent', () => {
  let fixture: ComponentFixture<AnalysisDiffComponent>;
  let component: AnalysisDiffComponent;
  let caseAnalysisServiceSpy: jasmine.SpyObj<CaseAnalysisService>;
  let caseFileServiceSpy: jasmine.SpyObj<CaseFileService>;
  let snackBarSpy: jasmine.SpyObj<MatSnackBar>;
  let router: Router;

  beforeEach(async () => {
    caseAnalysisServiceSpy = jasmine.createSpyObj('CaseAnalysisService', ['getVersions', 'getDiff']);
    caseFileServiceSpy = jasmine.createSpyObj('CaseFileService', ['getById']);
    snackBarSpy = jasmine.createSpyObj('MatSnackBar', ['open']);

    caseFileServiceSpy.getById.and.returnValue(of({ id: 'cf-1', title: 'Dossier test' } as any));
    caseAnalysisServiceSpy.getVersions.and.returnValue(of(MOCK_VERSIONS));
    caseAnalysisServiceSpy.getDiff.and.returnValue(of(MOCK_DIFF));

    await TestBed.configureTestingModule({
      imports: [AnalysisDiffComponent, NoopAnimationsModule],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: CaseFileService, useValue: caseFileServiceSpy },
        { provide: CaseAnalysisService, useValue: caseAnalysisServiceSpy },
        { provide: MatSnackBar, useValue: snackBarSpy },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: () => 'cf-1' } } }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AnalysisDiffComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  it('should load versions on init', () => {
    expect(caseAnalysisServiceSpy.getVersions).toHaveBeenCalledWith('cf-1');
    expect(component.versions()).toEqual(MOCK_VERSIONS);
  });

  it('canCompute() is false when no versions selected', () => {
    expect(component.canCompute()).toBeFalse();
  });

  it('canCompute() is false when fromId equals toId', () => {
    component.fromId.set('v1-id');
    component.toId.set('v1-id');
    expect(component.canCompute()).toBeFalse();
  });

  it('canCompute() is true when two different versions selected', () => {
    component.fromId.set('v1-id');
    component.toId.set('v2-id');
    expect(component.canCompute()).toBeTrue();
  });

  it('onVersionChange() calls getDiff when canCompute is true', () => {
    component.fromId.set('v1-id');
    component.toId.set('v2-id');
    component.onVersionChange();
    expect(caseAnalysisServiceSpy.getDiff).toHaveBeenCalledWith('cf-1', 'v1-id', 'v2-id');
  });

  it('onVersionChange() does not call getDiff when versions are the same', () => {
    component.fromId.set('v1-id');
    component.toId.set('v1-id');
    component.onVersionChange();
    expect(caseAnalysisServiceSpy.getDiff).not.toHaveBeenCalled();
  });

  it('sections() contains 5 sections after diff loaded', () => {
    component.fromId.set('v1-id');
    component.toId.set('v2-id');
    component.onVersionChange();
    expect(component.sections().length).toBe(5);
  });

  it('totalAdded() counts all added items across sections', () => {
    component.fromId.set('v1-id');
    component.toId.set('v2-id');
    component.onVersionChange();
    // faits.added=1, timeline.added=1 → 2
    expect(component.totalAdded()).toBe(2);
  });

  it('totalRemoved() counts all removed items across sections', () => {
    component.fromId.set('v1-id');
    component.toId.set('v2-id');
    component.onVersionChange();
    // faits.removed=1
    expect(component.totalRemoved()).toBe(1);
  });

  it('shows snackbar error when getDiff fails', () => {
    caseAnalysisServiceSpy.getDiff.and.returnValue(throwError(() => ({ status: 500 })));
    component.fromId.set('v1-id');
    component.toId.set('v2-id');
    component.onVersionChange();
    expect(snackBarSpy.open).toHaveBeenCalled();
  });

  it('goBack() navigates to case file detail', () => {
    const navigateSpy = spyOn(router, 'navigate');
    component.goBack();
    expect(navigateSpy).toHaveBeenCalledWith(['/case-files', 'cf-1']);
  });
});
