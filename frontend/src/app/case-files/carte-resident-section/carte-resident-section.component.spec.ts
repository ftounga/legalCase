import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { RouterTestingModule } from '@angular/router/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';

import { CarteResidentSectionComponent } from './carte-resident-section.component';
import { CarteResidentResponse } from '../../core/models/carte-resident.model';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';

describe('CarteResidentSectionComponent', () => {
  let component: CarteResidentSectionComponent;
  let fixture: ComponentFixture<CarteResidentSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/carte-resident-analysis';

  function frResponse(overrides: Partial<CarteResidentResponse> = {}): CarteResidentResponse {
    return {
      caseFileId: 'case-1',
      country: 'FRANCE',
      verdict: 'ELIGIBLE',
      chipsCriteresNonRemplis: [],
      atouts: ['Séjour régulier de 5 ans continu'],
      baseJuridique: 'CESEDA L. 426-1',
      ...overrides,
    };
  }

  function flush404(): void {
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      imports: [
        CarteResidentSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
        RouterTestingModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(CarteResidentSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.match((r) => r.url.includes('/jurisprudence-citations')).forEach((r) => r.flush({ items: [] }));
    httpMock.verify();
  });

  it('creates', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('FRANCE : charge l\'analyse existante au ngOnInit', () => {
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush(frResponse());
    expect(component.result()?.verdict).toBe('ELIGIBLE');
    expect(component.showForm()).toBe(false);
  });

  it('404 : bascule sur le formulaire', () => {
    fixture.detectChanges();
    flush404();
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  it('gate FRANCE : aucun appel HTTP en Belgique + bannière BE', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.collapsed.set(false);
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);
    expect(component.isFrance()).toBe(false);
    const banner = fixture.nativeElement.querySelector('[data-testid="be-banner"]');
    expect(banner).toBeTruthy();
  });

  it('formValid : faux si durée ou ressources manquantes ou négatives', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    expect(component.formValid()).toBe(false); // tout vide
    component.dureeSejourRegulierAnnees.set(5);
    expect(component.formValid()).toBe(false); // ressources manquantes
    component.ressourcesMensuellesNettes.set(1850);
    expect(component.formValid()).toBe(true);
    component.dureeSejourRegulierAnnees.set(-1);
    expect(component.formValid()).toBe(false);
  });

  it('analyze : POST + bascule sur le résultat + snackbar', () => {
    fixture.detectChanges();
    flush404();
    component.dureeSejourRegulierAnnees.set(5);
    component.ressourcesMensuellesNettes.set(1850);
    component.niveauIntegration.set('FORT');
    component.condamnationsPenalesGraves.set(false);
    component.analyze();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.dureeSejourRegulierAnnees).toBe(5);
    expect(req.request.body.ressourcesMensuellesNettes).toBe(1850);
    expect(req.request.body.niveauIntegration).toBe('FORT');
    req.flush(frResponse());
    expect(component.showForm()).toBe(false);
    expect(component.result()?.verdict).toBe('ELIGIBLE');
    expect(snackSpy.open).toHaveBeenCalled();
  });

  it('analyze : erreur HTTP → snackbar erreur, reste sur le formulaire', () => {
    fixture.detectChanges();
    flush404();
    component.dureeSejourRegulierAnnees.set(5);
    component.ressourcesMensuellesNettes.set(1850);
    component.analyze();
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Boom' }, { status: 500, statusText: 'Server Error' });
    expect(component.analyzing()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalled();
  });

  it('INADMISSIBLE : chip verdict rendu avec la classe rouge sombre', () => {
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(frResponse({
      verdict: 'INADMISSIBLE',
      chipsCriteresNonRemplis: ['Condamnation pénale grave'],
      atouts: [],
    }));
    component.collapsed.set(false);
    fixture.detectChanges();
    const verdictChip = fixture.nativeElement.querySelector('[data-testid="verdict-chip"]');
    expect(verdictChip).toBeTruthy();
    expect(verdictChip.textContent).toContain('Inadmissible');
    expect(verdictChip.className).toContain('cr-chip--inadmissible');
    expect(component.verdictClass('INADMISSIBLE')).toBe('cr-chip--inadmissible');
  });

  it('NON_ELIGIBLE_RESSOURCES : chips critères non remplis rendus', () => {
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(frResponse({
      verdict: 'NON_ELIGIBLE_RESSOURCES',
      chipsCriteresNonRemplis: ['Ressources insuffisantes', 'Ressources instables'],
      atouts: [],
    }));
    component.collapsed.set(false);
    fixture.detectChanges();
    const chips = fixture.nativeElement.querySelector('[data-testid="criteres-non-remplis"]');
    expect(chips).toBeTruthy();
    expect(chips.textContent).toContain('Ressources insuffisantes');
    expect(component.verdictClass('NON_ELIGIBLE_RESSOURCES')).toBe('cr-chip--ko');
  });

  it('atouts rendus en liste quand présents', () => {
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(frResponse({ atouts: ['Intégration FORTE', 'Ressources stables'] }));
    component.collapsed.set(false);
    fixture.detectChanges();
    const atouts = fixture.nativeElement.querySelector('[data-testid="atouts"]');
    expect(atouts).toBeTruthy();
    expect(atouts.textContent).toContain('Intégration FORTE');
  });

  it('prefillFromAi : pré-remplit durée (÷12) + ressources depuis ImmigrationExtractedData', () => {
    const aiData = {
      aesDureePresenceMois: 66,
      carteResidentRessources: 1850,
    } as ImmigrationExtractedData;
    fixture.detectChanges();
    flush404();
    component.aiData = aiData;
    component.ngOnChanges({ aiData: new SimpleChange(null, aiData, false) });
    expect(component.dureeSejourRegulierAnnees()).toBe(5);
    expect(component.provenanceDuree()).toBe('IA');
    expect(component.ressourcesMensuellesNettes()).toBe(1850);
    expect(component.provenanceRessources()).toBe('IA');
  });

  it('getPrefillCount static reflète le runtime (2 champs)', () => {
    const count = CarteResidentSectionComponent.getPrefillCount({
      aiData: { aesDureePresenceMois: 66, carteResidentRessources: 1850 },
      workspaceCountry: 'FRANCE',
    });
    expect(count).toBe(2);
  });

  it('toggleCollapse inverse l\'état replié', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    const before = component.collapsed();
    component.toggleCollapse();
    expect(component.collapsed()).toBe(!before);
  });

  it('standaloneMode : pas d\'appel HTTP, formulaire visible', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);
    expect(component.showForm()).toBe(true);
    expect(component.collapsed()).toBe(false);
  });
});
