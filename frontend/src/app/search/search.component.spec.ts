import { TestBed, ComponentFixture, fakeAsync, tick } from '@angular/core/testing';
import { SearchComponent } from './search.component';
import { SynthesisSearchService } from '../core/services/synthesis-search.service';
import { MatSnackBar } from '@angular/material/snack-bar';
import { provideRouter } from '@angular/router';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { of, throwError } from 'rxjs';
import { SynthesisSearchResponse } from '../core/models/search.model';

const mockResponse: SynthesisSearchResponse = {
  query: 'licenciement',
  totalResults: 1,
  results: [{
    caseFileId: 'cf-1',
    caseFileTitle: 'Dossier Dupont',
    legalDomain: 'DROIT_DU_TRAVAIL',
    analysisType: 'STANDARD',
    matchCount: 2,
    excerpts: ['Le salarié a été licencié le 15 janvier.']
  }]
};

const emptyResponse: SynthesisSearchResponse = {
  query: 'xyz',
  totalResults: 0,
  results: []
};

describe('SearchComponent', () => {
  let fixture: ComponentFixture<SearchComponent>;
  let component: SearchComponent;
  let searchServiceSpy: jest.Mocked<SynthesisSearchService>;
  let snackBarSpy: jest.Mocked<MatSnackBar>;

  beforeEach(async () => {
    searchServiceSpy = jasmine.createSpyObj('SynthesisSearchService', ['search']);
    snackBarSpy = jasmine.createSpyObj('MatSnackBar', ['open']);

    await TestBed.configureTestingModule({
      imports: [SearchComponent],
      providers: [
        { provide: SynthesisSearchService, useValue: searchServiceSpy },
        { provide: MatSnackBar, useValue: snackBarSpy },
        provideRouter([]),
        provideAnimationsAsync()
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(SearchComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  // U-01 : moins de 2 chars → search() non appelé
  it('should not call search() when query is less than 2 chars', fakeAsync(() => {
    component.queryControl.setValue('a');
    tick(500);
    expect(searchServiceSpy.search).not.toHaveBeenCalled();
  }));

  // U-02 : 2+ chars après debounce → search() appelé
  it('should call search() after debounce with 2+ chars', fakeAsync(() => {
    searchServiceSpy.search.mockReturnValue(of(mockResponse));
    component.queryControl.setValue('licenciement');
    tick(500);
    expect(searchServiceSpy.search).toHaveBeenCalledWith('licenciement');
  }));

  // U-03 : résultats reçus → results signal alimenté
  it('should populate results signal on success', fakeAsync(() => {
    searchServiceSpy.search.mockReturnValue(of(mockResponse));
    component.queryControl.setValue('licenciement');
    tick(500);
    expect(component.results().length).toBe(1);
    expect(component.results()[0].caseFileTitle).toBe('Dossier Dupont');
    expect(component.searched()).toBe(true);
  }));

  // U-04 : erreur HTTP → snackbar affiché
  it('should show snackbar on HTTP error', fakeAsync(() => {
    searchServiceSpy.search.mockReturnValue(throwError(() => new Error('500')));
    component.queryControl.setValue('licenciement');
    tick(500);
    expect(snackBarSpy.open).toHaveBeenCalled();
    expect(component.loading()).toBe(false);
  }));

  // U-05 : champ vidé → résultats effacés
  it('should clear results when query is cleared', fakeAsync(() => {
    searchServiceSpy.search.mockReturnValue(of(mockResponse));
    component.queryControl.setValue('licenciement');
    tick(500);
    expect(component.results().length).toBe(1);

    component.queryControl.setValue('');
    tick(500);
    expect(component.results().length).toBe(0);
    expect(component.searched()).toBe(false);
  }));

  // U-06 : 0 résultats → searched() true et results vide
  it('should set searched true with empty results', fakeAsync(() => {
    searchServiceSpy.search.mockReturnValue(of(emptyResponse));
    component.queryControl.setValue('xyz123');
    tick(500);
    expect(component.results().length).toBe(0);
    expect(component.searched()).toBe(true);
  }));
});
