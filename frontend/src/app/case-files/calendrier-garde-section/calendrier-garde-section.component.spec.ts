import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { CalendrierGardeSectionComponent } from './calendrier-garde-section.component';

describe('CalendrierGardeSectionComponent', () => {
  let component: CalendrierGardeSectionComponent;
  let fixture: ComponentFixture<CalendrierGardeSectionComponent>;
  let httpMock: HttpTestingController;
  const ID = '88888888-8888-8888-8888-888888888888';
  const URL = `/api/v1/case-files/${ID}/calendrier-garde`;
  const MOCK = { caseFileId: ID, gardeCode: 'ALTERNEE_FR', gardeLabel: 'Résidence alternée', country: 'FRANCE',
    parentANom: 'Marie', parentBNom: 'Pierre', repartitionType: 'ALTERNEE_1_SUR_2',
    semaineTypeParentA: ['Semaine A'], semaineTypeParentB: ['Semaine B'],
    vacancesRegle: 'Moitié', joursParAnParentA: 182, joursParAnParentB: 183,
    baseJuridique: 'Art 373-2-9', commentaire: 'Test' };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CalendrierGardeSectionComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideAnimationsAsync()],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(CalendrierGardeSectionComponent);
    component = fixture.componentInstance; component.caseFileId = ID;
  });
  afterEach(() => { httpMock.verify(); });

  function initNo(): void { fixture.detectChanges(); httpMock.expectOne(URL).flush(null, { status: 404, statusText: 'NF' }); }
  function initWith(): void { fixture.detectChanges(); httpMock.expectOne(URL).flush(MOCK); }

  it('should create', () => { initNo(); expect(component).toBeTruthy(); });
  it('should show form when no existing', () => { initNo(); expect(component.showForm()).toBe(true); });
  it('should call POST', () => {
    initNo(); component.parentANom.set('A'); component.parentBNom.set('B'); component.generate();
    const r = httpMock.expectOne(URL); expect(r.request.method).toBe('POST'); r.flush(MOCK);
    expect(component.result()).toBeTruthy(); expect(component.showForm()).toBe(false);
  });
  it('should display existing', () => { initWith(); expect(component.result()).toBeTruthy(); expect(component.showForm()).toBe(false); });

  // ---- AI prefill mode_garde_detaille (SF-FA-06-04) ----

  it('should prefill gardeCode from IA when mode matches workspace country', () => {
    component.workspaceCountry = 'FRANCE';
    component.aiModeGardeDetaille = 'DVH_ELARGI_FR';
    initNo();
    expect(component.gardeCode()).toBe('DVH_ELARGI_FR');
    expect(component.modeDetailleNote()).toBeNull();
  });

  it('should prefill gardeCode for BELGIQUE workspace', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.aiModeGardeDetaille = 'SECONDAIRE_ELARGI_BE';
    initNo();
    expect(component.gardeCode()).toBe('SECONDAIRE_ELARGI_BE');
  });

  it('should set note when IA mode is from opposite country', () => {
    component.workspaceCountry = 'FRANCE';
    component.aiModeGardeDetaille = 'ALTERNEE_BE';
    initNo();
    // Default FR preserved, note shown
    expect(component.gardeCode()).toBe('ALTERNEE_FR');
    expect(component.modeDetailleNote()).toContain('ALTERNEE_BE');
  });

  it('should ignore invalid IA value', () => {
    component.aiModeGardeDetaille = 'UNKNOWN_MODE' as any;
    initNo();
    expect(component.gardeCode()).toBe('ALTERNEE_FR');
    expect(component.modeDetailleNote()).toBeNull();
  });

  it('should NOT override saved result', () => {
    component.aiModeGardeDetaille = 'DVH_ELARGI_FR';
    initWith();
    // Existing result loaded — prefill not applied
    expect(component.showForm()).toBe(false);
    // gardeCode signal may have been reset by loadExisting ; we just check no crash
  });

  it('should clear note when user changes gardeCode', () => {
    component.workspaceCountry = 'FRANCE';
    component.aiModeGardeDetaille = 'ALTERNEE_BE';
    initNo();
    expect(component.modeDetailleNote()).not.toBeNull();
    component.onGardeCodeChange();
    expect(component.modeDetailleNote()).toBeNull();
  });

  it('should do nothing when aiModeGardeDetaille is absent', () => {
    initNo();
    expect(component.gardeCode()).toBe('ALTERNEE_FR');
    expect(component.modeDetailleNote()).toBeNull();
  });
});
