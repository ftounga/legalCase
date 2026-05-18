import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { AutoriteParentaleBeSectionComponent } from './autorite-parentale-be-section.component';
import { AutoriteParentaleBeResponse } from '../../core/models/autorite-parentale-be.model';

/**
 * SF-217-05 : tests du composant "Autorité parentale (Belgique)".
 */
describe('AutoriteParentaleBeSectionComponent', () => {
  let component: AutoriteParentaleBeSectionComponent;
  let fixture: ComponentFixture<AutoriteParentaleBeSectionComponent>;
  let httpMock: HttpTestingController;

  const URL = '/api/v1/case-files/case-1/autorite-parentale-be';

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AutoriteParentaleBeSectionComponent, HttpClientTestingModule, NoopAnimationsModule],
    }).compileComponents();
    fixture = TestBed.createComponent(AutoriteParentaleBeSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('expose les statics TOOL_LABEL, TOOL_ICON, getPrefillCount', () => {
    expect(AutoriteParentaleBeSectionComponent.TOOL_LABEL).toBe('AUTORITÉ PARENTALE — CC ART. 374-375 BE');
    expect(AutoriteParentaleBeSectionComponent.TOOL_ICON).toBe('family_restroom');
    expect(typeof AutoriteParentaleBeSectionComponent.getPrefillCount).toBe('function');
  });

  it('getPrefillCount retourne 0 (PREFILL_COUNT_ALWAYS_ZERO)', () => {
    expect(AutoriteParentaleBeSectionComponent.getPrefillCount({})).toBe(0);
    expect(AutoriteParentaleBeSectionComponent.PREFILL_COUNT_ALWAYS_ZERO).toBe(true);
  });

  it('rendu nominal BE : titre + toggles affichés quand expanded', () => {
    fixture.componentRef.setInput('forceExpanded', true);
    fixture.componentRef.setInput('workspaceCountry', 'BELGIQUE');
    fixture.detectChanges();
    httpMock.expectOne(URL).flush({}, { status: 404, statusText: 'Not Found' });
    fixture.detectChanges();
    const html = fixture.nativeElement as HTMLElement;
    expect(html.textContent).toContain('AUTORITÉ PARENTALE');
    expect(html.textContent).toContain('Filiation établie');
    expect(html.textContent).toContain('autorité parentale exclusive est demandée');
  });

  it('gate workspaceCountry=FRANCE → bannière info, pas de form, aucun GET', () => {
    fixture.componentRef.setInput('workspaceCountry', 'FRANCE');
    fixture.componentRef.setInput('forceExpanded', true);
    fixture.detectChanges();
    httpMock.expectNone(URL);
    const html = fixture.nativeElement as HTMLElement;
    expect(html.textContent).toContain('Outil propre au droit belge');
  });

  it('désactiver la demande d\'exclusive remet à zéro les motifs graves', () => {
    component.demandeAutoriteExclusive.set(true);
    component.miseEnDangerEnfant.set(true);
    component.onDemandeExclusiveChange(false);
    expect(component.demandeAutoriteExclusive()).toBe(false);
    expect(component.miseEnDangerEnfant()).toBe(false);
  });

  it('calculate() POSTe la request et bascule en mode résultat', () => {
    fixture.componentRef.setInput('forceExpanded', true);
    fixture.componentRef.setInput('workspaceCountry', 'BELGIQUE');
    fixture.detectChanges();
    httpMock.expectOne(URL).flush({}, { status: 404, statusText: 'Not Found' });
    fixture.detectChanges();

    component.demandeAutoriteExclusive.set(true);
    component.miseEnDangerEnfant.set(true);
    component.calculate();

    const postReq = httpMock.expectOne(URL);
    expect(postReq.request.method).toBe('POST');
    expect(postReq.request.body.demandeAutoriteExclusive).toBe(true);
    expect(postReq.request.body.miseEnDangerEnfant).toBe(true);

    const response: AutoriteParentaleBeResponse = {
      caseFileId: 'case-1',
      filiationEtablieDeuxParents: true,
      accordParentalExiste: false,
      demandeAutoriteExclusive: true,
      desinteretDurableParent: false,
      miseEnDangerEnfant: true,
      incapaciteParent: false,
      decisionJudiciaireAnterieure: false,
      modeHebergementPrincipal: 'HEBERGEMENT_PRINCIPAL_UN_PARENT',
      commentaire: null,
      verdict: 'AUTORITE_EXCLUSIVE_FONDEE',
      voieProcedurale: 'REQUETE_TRIBUNAL_FAMILLE',
      facteurs: [],
      basesJuridiques: ['CC art. 374 §1'],
      messages: [],
      country: 'BELGIQUE',
      calculatedAt: '2026-05-18T10:00:00Z',
    };
    postReq.flush(response);
    fixture.detectChanges();
    expect(component.result()?.verdict).toBe('AUTORITE_EXCLUSIVE_FONDEE');
    expect(component.showForm()).toBe(false);
  });

  it('verdictBannerClass : rouge réservé à AUTORITE_EXCLUSIVE_NON_FONDEE', () => {
    expect(component.verdictBannerClass('AUTORITE_EXCLUSIVE_NON_FONDEE')).toContain('--danger');
    expect(component.verdictBannerClass('AUTORITE_CONJOINTE')).toContain('--available');
    expect(component.verdictBannerClass('AUTORITE_EXCLUSIVE_FONDEE')).toContain('--available');
    expect(component.verdictBannerClass('QUALIFICATION_INCOMPLETE')).toContain('--medium');
  });
});
