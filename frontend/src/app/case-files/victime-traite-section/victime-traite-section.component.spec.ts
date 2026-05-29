import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { RouterTestingModule } from '@angular/router/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';

import { VictimeTraiteSectionComponent } from './victime-traite-section.component';
import { VictimeTraiteResponse } from '../../core/models/victime-traite.model';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';

describe('VictimeTraiteSectionComponent', () => {
  let component: VictimeTraiteSectionComponent;
  let fixture: ComponentFixture<VictimeTraiteSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/victime-traite-analysis';

  function frResponse(overrides: Partial<VictimeTraiteResponse> = {}): VictimeTraiteResponse {
    return {
      caseFileId: 'case-1',
      country: 'FRANCE',
      verdict: 'ELIGIBLE_PROBABLE',
      chipsCriteresManquants: [],
      mesuresProtection: ['Carte de séjour temporaire « vie privée et familiale »'],
      risqueVictimeEnDanger: false,
      baseJuridique: 'CESEDA L. 425-1',
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
        VictimeTraiteSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
        RouterTestingModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(VictimeTraiteSectionComponent);
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
    expect(component.result()?.verdict).toBe('ELIGIBLE_PROBABLE');
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

  it('formValid : datePlainte optionnelle, vide = valide, format non ISO = invalide', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    expect(component.formValid()).toBe(true); // tout vide → valide (champs optionnels)
    component.datePlainte.set('10/01/2026'); // non ISO
    expect(component.formValid()).toBe(false);
    component.datePlainte.set('2026-01-10');
    expect(component.formValid()).toBe(true);
  });

  it('analyze : POST + bascule sur le résultat + snackbar', () => {
    fixture.detectChanges();
    flush404();
    component.plainteDeposee.set(true);
    component.collaborationOCRTEH.set(true);
    component.datePlainte.set('2026-01-10');
    component.analyze();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.plainteDeposee).toBe(true);
    expect(req.request.body.collaborationOCRTEH).toBe(true);
    expect(req.request.body.datePlainte).toBe('2026-01-10');
    req.flush(frResponse());
    expect(component.showForm()).toBe(false);
    expect(component.result()?.verdict).toBe('ELIGIBLE_PROBABLE');
    expect(snackSpy.open).toHaveBeenCalled();
  });

  it('analyze : erreur HTTP → snackbar erreur, reste sur le formulaire', () => {
    fixture.detectChanges();
    flush404();
    component.analyze();
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Boom' }, { status: 500, statusText: 'Server Error' });
    expect(component.analyzing()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalled();
  });

  it('risqueVictimeEnDanger true → bannière ROUGE alerte sécurité rendue', () => {
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(frResponse({
      verdict: 'ELIGIBLE_SOUS_RESERVE_PLAINTE',
      risqueVictimeEnDanger: true,
    }));
    component.collapsed.set(false);
    fixture.detectChanges();
    const danger = fixture.nativeElement.querySelector('[data-testid="risque-victime-en-danger"]');
    expect(danger).toBeTruthy();
    expect(danger.getAttribute('role')).toBe('alert');
    expect(danger.textContent).toContain('Alerte sécurité');
  });

  it('risqueVictimeEnDanger false → pas de bannière rouge', () => {
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(frResponse({ risqueVictimeEnDanger: false }));
    component.collapsed.set(false);
    fixture.detectChanges();
    const danger = fixture.nativeElement.querySelector('[data-testid="risque-victime-en-danger"]');
    expect(danger).toBeNull();
  });

  it('NON_ELIGIBLE : chips critères manquants rendus', () => {
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(frResponse({
      verdict: 'NON_ELIGIBLE',
      chipsCriteresManquants: ['Plainte non déposée', 'Pas de collaboration'],
      mesuresProtection: [],
    }));
    component.collapsed.set(false);
    fixture.detectChanges();
    const chips = fixture.nativeElement.querySelector('[data-testid="criteres-manquants"]');
    expect(chips).toBeTruthy();
    expect(chips.textContent).toContain('Plainte non déposée');
    const verdictChip = fixture.nativeElement.querySelector('[data-testid="verdict-chip"]');
    expect(verdictChip.textContent).toContain('Non éligible');
  });

  it('prefillFromAi : pré-remplit plainteDeposee + datePlainte depuis ImmigrationExtractedData', () => {
    const aiData = {
      tehPlainteDeposee: true,
      tehDatePlainte: '2026-02-15',
    } as ImmigrationExtractedData;
    fixture.detectChanges();
    flush404();
    component.aiData = aiData;
    component.ngOnChanges({ aiData: new SimpleChange(null, aiData, false) });
    expect(component.plainteDeposee()).toBe(true);
    expect(component.provenancePlainte()).toBe('IA');
    expect(component.datePlainte()).toBe('2026-02-15');
    expect(component.provenanceDatePlainte()).toBe('IA');
  });

  it('getPrefillCount static reflète le runtime (2 champs)', () => {
    const count = VictimeTraiteSectionComponent.getPrefillCount({
      aiData: { tehPlainteDeposee: true, tehDatePlainte: '2026-02-15' },
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
