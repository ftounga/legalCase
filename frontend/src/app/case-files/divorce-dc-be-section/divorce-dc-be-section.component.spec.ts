import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { DivorceDcBeSectionComponent } from './divorce-dc-be-section.component';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import { DivorceDcBeResponse } from '../../core/models/divorce-dc-be.model';

/**
 * F-243 : tests du composant complet DC BE (remplace l'ancien spec placeholder).
 */
describe('DivorceDcBeSectionComponent', () => {
  let component: DivorceDcBeSectionComponent;
  let fixture: ComponentFixture<DivorceDcBeSectionComponent>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DivorceDcBeSectionComponent, HttpClientTestingModule, NoopAnimationsModule],
    }).compileComponents();
    fixture = TestBed.createComponent(DivorceDcBeSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('expose statics TOOL_LABEL, TOOL_ICON, getPrefillCount', () => {
    expect(DivorceDcBeSectionComponent.TOOL_LABEL).toBe('DIVORCE CONSENTEMENT MUTUEL — CJ ART. 1287 BE');
    expect(DivorceDcBeSectionComponent.TOOL_ICON).toBe('handshake');
    expect(typeof DivorceDcBeSectionComponent.getPrefillCount).toBe('function');
  });

  it('getPrefillCount retourne 0 sans aiData', () => {
    expect(DivorceDcBeSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('getPrefillCount retourne 1 si aiData.dateAcceptationPV est ISO valide', () => {
    const aiData: FamilleExtractedData = { dateAcceptationPV: '2025-12-12' };
    expect(DivorceDcBeSectionComponent.getPrefillCount({ aiData })).toBe(1);
  });

  it('getPrefillCount retourne 0 si dateAcceptationPV non ISO', () => {
    const aiData: FamilleExtractedData = { dateAcceptationPV: '12/12/2025' };
    expect(DivorceDcBeSectionComponent.getPrefillCount({ aiData })).toBe(0);
  });

  it('rendu nominal : titre + form 4 toggles convention quand expanded', () => {
    fixture.componentRef.setInput('forceExpanded', true);
    fixture.componentRef.setInput('workspaceCountry', 'BELGIQUE');
    fixture.detectChanges();
    // ngOnInit déclenche un load (GET) qu'on doit absorber
    const r = httpMock.expectOne('/api/v1/case-files/case-1/divorce-dc-be-analysis');
    r.flush({}, { status: 404, statusText: 'Not Found' });
    httpMock.expectOne('/api/v1/case-files/case-1/source-explanations').flush([]);
    fixture.detectChanges();
    const html = fixture.nativeElement as HTMLElement;
    expect(html.textContent).toContain('DIVORCE CONSENTEMENT MUTUEL');
    expect(html.textContent).toContain('Convention sur le logement');
    expect(html.textContent).toContain('Convention sur la liquidation');
    expect(html.textContent).toContain('Convention sur la garde');
    expect(html.textContent).toContain('Convention sur les contributions');
  });

  it('gate workspaceCountry=FRANCE → bannière info, pas de form', () => {
    fixture.componentRef.setInput('workspaceCountry', 'FRANCE');
    fixture.componentRef.setInput('forceExpanded', true);
    fixture.detectChanges();
    // Aucun GET attendu (gate avant load)
    httpMock.expectNone('/api/v1/case-files/case-1/divorce-dc-be-analysis');
    const html = fixture.nativeElement as HTMLElement;
    expect(html.textContent).toContain('Outil Belgique uniquement');
  });

  it('pré-fill IA : dateAcceptationPV ISO → dateSignatureConvention + badge auto_awesome', () => {
    fixture.componentRef.setInput('forceExpanded', true);
    fixture.componentRef.setInput('workspaceCountry', 'BELGIQUE');
    fixture.componentRef.setInput('aiData', { dateAcceptationPV: '2025-12-12' });
    fixture.detectChanges();
    const r = httpMock.expectOne('/api/v1/case-files/case-1/divorce-dc-be-analysis');
    r.flush({}, { status: 404, statusText: 'Not Found' });
    httpMock.expectOne('/api/v1/case-files/case-1/source-explanations').flush([]);
    fixture.detectChanges();
    expect(component.dateSignatureConvention()).toBe('2025-12-12');
    expect(component.provenanceDateSignatureConvention()).toBe('IA');
  });

  it('calculate POSTe la request et bascule en mode résultat', () => {
    fixture.componentRef.setInput('forceExpanded', true);
    fixture.componentRef.setInput('workspaceCountry', 'BELGIQUE');
    fixture.detectChanges();
    httpMock.expectOne('/api/v1/case-files/case-1/divorce-dc-be-analysis')
      .flush({}, { status: 404, statusText: 'Not Found' });
    httpMock.expectOne('/api/v1/case-files/case-1/source-explanations').flush([]);
    fixture.detectChanges();

    component.dateSignatureConvention.set('2025-12-12');
    component.conventionLogement.set(true);
    component.conventionBiens.set(true);
    component.conventionGardeEnfants.set(true);
    component.conventionContributions.set(true);
    component.calculate();
    const postReq = httpMock.expectOne('/api/v1/case-files/case-1/divorce-dc-be-analysis');
    expect(postReq.request.method).toBe('POST');
    expect(postReq.request.body.dateSignatureConvention).toBe('2025-12-12');
    expect(postReq.request.body.conventionLogement).toBe(true);
    const response: DivorceDcBeResponse = {
      caseFileId: 'case-1',
      country: 'BELGIQUE',
      dateSignatureConvention: '2025-12-12',
      dateAudienceHomologation: null,
      delaiReflexionJours: -1,
      delaiReflexionRespecte: false,
      conventionLogement: true,
      conventionBiens: true,
      conventionGardeEnfants: true,
      conventionContributions: true,
      conventionComplete: true,
      enfantsMineursCommuns: false,
      epouxConsentent: true,
      verdict: 'RECEVABLE',
      motifsIrrecevabilite: [],
      formule: 'DC art. 1287+ CJ — recevabilité validée.',
      baseJuridique: 'CJ art. 1287-1304 ; Loi du 27/04/2007.',
      messages: [],
    };
    postReq.flush(response);
    fixture.detectChanges();
    expect(component.result()?.verdict).toBe('RECEVABLE');
    expect(component.showForm()).toBe(false);
  });
});
