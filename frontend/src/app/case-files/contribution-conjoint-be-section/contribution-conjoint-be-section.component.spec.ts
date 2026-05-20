import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ContributionConjointBeSectionComponent } from './contribution-conjoint-be-section.component';
import { ContributionConjointBeResponse } from '../../core/models/contribution-conjoint-be.model';

/**
 * SF-217-09 : tests du composant "Pension alimentaire entre ex-époux (Belgique)".
 */
describe('ContributionConjointBeSectionComponent', () => {
  let component: ContributionConjointBeSectionComponent;
  let fixture: ComponentFixture<ContributionConjointBeSectionComponent>;
  let httpMock: HttpTestingController;

  const URL = '/api/v1/case-files/case-1/contribution-conjoint-be';

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ContributionConjointBeSectionComponent, HttpClientTestingModule, NoopAnimationsModule],
    }).compileComponents();
    fixture = TestBed.createComponent(ContributionConjointBeSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('expose les statics TOOL_LABEL, TOOL_ICON, getPrefillCount', () => {
    expect(ContributionConjointBeSectionComponent.TOOL_LABEL)
      .toBe('PENSION ALIMENTAIRE ENTRE EX-ÉPOUX — CC ART. 301 BE');
    expect(ContributionConjointBeSectionComponent.TOOL_ICON).toBe('volunteer_activism');
    expect(typeof ContributionConjointBeSectionComponent.getPrefillCount).toBe('function');
  });

  // SF-246-28 : levée PREFILL_COUNT_ALWAYS_ZERO — 3 champs possibles
  it('getPrefillCount retourne 0 si aiData absent', () => {
    expect(ContributionConjointBeSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('getPrefillCount retourne 3 si les 3 champs BE détectés', () => {
    expect(
      ContributionConjointBeSectionComponent.getPrefillCount({
        aiData: {
          dureeMariageAnneesBeDetectee: 8,
          revenuMensuelCreancierBeDetecte: 1200,
          revenuMensuelDebiteurBeDetecte: 2800,
        },
      }),
    ).toBe(3);
  });

  it('rendu nominal BE : titre + champs affichés quand expanded', () => {
    fixture.componentRef.setInput('forceExpanded', true);
    fixture.componentRef.setInput('workspaceCountry', 'BELGIQUE');
    fixture.detectChanges();
    httpMock.expectOne(URL).flush({}, { status: 404, statusText: 'Not Found' });
    fixture.detectChanges();
    const html = fixture.nativeElement as HTMLElement;
    expect(html.textContent).toContain('PENSION ALIMENTAIRE ENTRE EX-ÉPOUX');
    expect(html.textContent).toContain('Type de divorce');
    expect(html.textContent).toContain('créancier est dans le besoin');
  });

  it('gate workspaceCountry=FRANCE → bannière info, pas de form, aucun GET', () => {
    fixture.componentRef.setInput('workspaceCountry', 'FRANCE');
    fixture.componentRef.setInput('forceExpanded', true);
    fixture.detectChanges();
    httpMock.expectNone(URL);
    const html = fixture.nativeElement as HTMLElement;
    expect(html.textContent).toContain('Outil propre au droit belge');
  });

  it('formValid exige durée du mariage 0-80 et les deux revenus', () => {
    fixture.componentRef.setInput('workspaceCountry', 'BELGIQUE');
    expect(component.formValid()).toBe(false);
    component.dureeMariageAnnees.set(18);
    component.revenuMensuelCreancier.set(900);
    component.revenuMensuelDebiteur.set(3600);
    expect(component.formValid()).toBe(true);
    component.dureeMariageAnnees.set(-1);
    expect(component.formValid()).toBe(false);
  });

  it('calculate() POSTe la request et bascule en mode résultat', () => {
    fixture.componentRef.setInput('forceExpanded', true);
    fixture.componentRef.setInput('workspaceCountry', 'BELGIQUE');
    fixture.detectChanges();
    httpMock.expectOne(URL).flush({}, { status: 404, statusText: 'Not Found' });
    fixture.detectChanges();

    component.dureeMariageAnnees.set(18);
    component.revenuMensuelCreancier.set(900);
    component.revenuMensuelDebiteur.set(3600);
    component.calculate();

    const postReq = httpMock.expectOne(URL);
    expect(postReq.request.method).toBe('POST');
    expect(postReq.request.body.dureeMariageAnnees).toBe(18);
    expect(postReq.request.body.typeDivorce).toBe('DDI');

    const response: ContributionConjointBeResponse = {
      caseFileId: 'case-1',
      typeDivorce: 'DDI',
      renonciationPensionConvention: false,
      creancierEnEtatDeBesoin: true,
      fauteGraveCreancier: false,
      dureeMariageAnnees: 18,
      revenuMensuelCreancier: 900,
      revenuMensuelDebiteur: 3600,
      degradationEconomiqueLieeAuMariage: true,
      commentaire: null,
      verdict: 'PENSION_DUE',
      dureeMaximaleMois: 216,
      montantMensuelIndicatif: 1200,
      plafondTiersRevenusDebiteur: 1200,
      motifsExclusion: [],
      detailCalcul: ['Durée maximale : 18 ans = 216 mois.'],
      basesJuridiques: ['CC art. 301 §1'],
      messages: [],
      country: 'BELGIQUE',
      calculatedAt: '2026-05-18T10:00:00Z',
    };
    postReq.flush(response);
    fixture.detectChanges();
    expect(component.result()?.verdict).toBe('PENSION_DUE');
    expect(component.showForm()).toBe(false);
  });

  it('verdictBannerClass : rouge réservé à PENSION_NON_DUE', () => {
    expect(component.verdictBannerClass('PENSION_NON_DUE')).toContain('--danger');
    expect(component.verdictBannerClass('PENSION_DUE')).toContain('--available');
    expect(component.verdictBannerClass('PENSION_CONVENTIONNELLE')).toContain('--available');
    expect(component.verdictBannerClass('DONNEES_INSUFFISANTES')).toContain('--medium');
  });

  it('dureeMaximaleLabel formate les mois en années + mois', () => {
    expect(component.dureeMaximaleLabel(216)).toContain('18 an');
    expect(component.dureeMaximaleLabel(0)).toBe('—');
  });
});
