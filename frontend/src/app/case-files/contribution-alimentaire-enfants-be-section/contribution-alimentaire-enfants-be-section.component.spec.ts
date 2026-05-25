import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ContributionAlimentaireEnfantsBeSectionComponent } from './contribution-alimentaire-enfants-be-section.component';
import { ContributionAlimentaireEnfantsBeResponse } from '../../core/models/contribution-alimentaire-enfants-be.model';

/**
 * SF-217-07 : tests du composant "Contribution alimentaire des enfants (Belgique)".
 */
describe('ContributionAlimentaireEnfantsBeSectionComponent', () => {
  let component: ContributionAlimentaireEnfantsBeSectionComponent;
  let fixture: ComponentFixture<ContributionAlimentaireEnfantsBeSectionComponent>;
  let httpMock: HttpTestingController;

  const URL = '/api/v1/case-files/case-1/contribution-alimentaire-enfants-be';

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ContributionAlimentaireEnfantsBeSectionComponent, HttpClientTestingModule, NoopAnimationsModule],
    }).compileComponents();
    fixture = TestBed.createComponent(ContributionAlimentaireEnfantsBeSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.match(r => r.url.includes('/jurisprudence-citations')).forEach(r => r.flush({ items: [] }));
    httpMock.verify();
  });

  it('expose les statics TOOL_LABEL, TOOL_ICON, getPrefillCount', () => {
    expect(ContributionAlimentaireEnfantsBeSectionComponent.TOOL_LABEL)
      .toBe('CONTRIBUTION ALIMENTAIRE ENFANTS — MÉTHODE RENARD BE');
    expect(ContributionAlimentaireEnfantsBeSectionComponent.TOOL_ICON).toBe('savings');
    expect(typeof ContributionAlimentaireEnfantsBeSectionComponent.getPrefillCount).toBe('function');
  });

  // SF-246-28 : levée PREFILL_COUNT_ALWAYS_ZERO — 6 champs possibles
  it('getPrefillCount retourne 0 si aiData absent', () => {
    expect(ContributionAlimentaireEnfantsBeSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('getPrefillCount retourne 6 si tous les 6 champs BE détectés', () => {
    expect(
      ContributionAlimentaireEnfantsBeSectionComponent.getPrefillCount({
        aiData: {
          nombreEnfantsBeDetecte: 2,
          revenuMensuelParent1BeDetecte: 2800,
          revenuMensuelParent2BeDetecte: 1900,
          allocationsFamilialesMensuellesBeDetectees: 320,
          nuitsHebergementParent1BeDetectees: 15,
          nuitsHebergementParent2BeDetectees: 15,
        },
      }),
    ).toBe(6);
  });

  it('rendu nominal BE : titre + champs affichés quand expanded', () => {
    fixture.componentRef.setInput('forceExpanded', true);
    fixture.componentRef.setInput('workspaceCountry', 'BELGIQUE');
    fixture.detectChanges();
    httpMock.expectOne(URL).flush({}, { status: 404, statusText: 'Not Found' });
    fixture.detectChanges();
    const html = fixture.nativeElement as HTMLElement;
    expect(html.textContent).toContain('CONTRIBUTION ALIMENTAIRE ENFANTS');
    expect(html.textContent).toContain("Nombre d'enfants");
    expect(html.textContent).toContain('Revenu mensuel net');
  });

  it('gate workspaceCountry=FRANCE → bannière info, pas de form, aucun GET', () => {
    fixture.componentRef.setInput('workspaceCountry', 'FRANCE');
    fixture.componentRef.setInput('forceExpanded', true);
    fixture.detectChanges();
    httpMock.expectNone(URL);
    const html = fixture.nativeElement as HTMLElement;
    expect(html.textContent).toContain('Outil propre au droit belge');
  });

  it('formValid exige nombre d\'enfants 1-12 et les deux revenus', () => {
    fixture.componentRef.setInput('workspaceCountry', 'BELGIQUE');
    expect(component.formValid()).toBe(false);
    component.nombreEnfants.set(2);
    component.revenuMensuelParent1.set(2800);
    component.revenuMensuelParent2.set(1900);
    expect(component.formValid()).toBe(true);
    component.nombreEnfants.set(0);
    expect(component.formValid()).toBe(false);
  });

  it('calculate() POSTe la request et bascule en mode résultat', () => {
    fixture.componentRef.setInput('forceExpanded', true);
    fixture.componentRef.setInput('workspaceCountry', 'BELGIQUE');
    fixture.detectChanges();
    httpMock.expectOne(URL).flush({}, { status: 404, statusText: 'Not Found' });
    fixture.detectChanges();

    component.nombreEnfants.set(2);
    component.revenuMensuelParent1.set(2800);
    component.revenuMensuelParent2.set(1900);
    component.nuitsHebergementParent1.set(110);
    component.nuitsHebergementParent2.set(255);
    component.calculate();

    const postReq = httpMock.expectOne(URL);
    expect(postReq.request.method).toBe('POST');
    expect(postReq.request.body.nombreEnfants).toBe(2);
    expect(postReq.request.body.revenuMensuelParent1).toBe(2800);

    const response: ContributionAlimentaireEnfantsBeResponse = {
      caseFileId: 'case-1',
      nombreEnfants: 2,
      trancheAgeEnfants: 'ENFANT_6_11',
      revenuMensuelParent1: 2800,
      revenuMensuelParent2: 1900,
      coutMensuelGlobalEnfants: null,
      nuitsHebergementParent1: 110,
      nuitsHebergementParent2: 255,
      allocationsFamilialesMensuelles: null,
      fraisExtraordinairesMensuels: null,
      parentDebiteurEstParent1: true,
      commentaire: null,
      verdict: 'CONTRIBUTION_DUE',
      // Modèle Renard : coef 6-11 (0,2032) × (2800+1900) × 2 enfants = 1910,08.
      coutMensuelRetenu: 1910.08,
      coutNetApresAllocations: 1910.08,
      quotePartParent1Pct: 59.6,
      quotePartParent2Pct: 40.4,
      partContributiveParent1: 1138.41,
      partContributiveParent2: 771.67,
      partHebergementParent1: 575.74,
      partHebergementParent2: 1334.34,
      contributionMensuelleNette: 562.67,
      parentDebiteur: 'PARENT_1',
      fraisExtraordinairesQuotePartParent1: 0,
      fraisExtraordinairesQuotePartParent2: 0,
      detailCalcul: ['Coût mensuel retenu (modèle Renard) : 1910,08 €'],
      basesJuridiques: ['CC art. 203 §1'],
      messages: [],
      country: 'BELGIQUE',
      calculatedAt: '2026-05-18T10:00:00Z',
    };
    postReq.flush(response);
    fixture.detectChanges();
    expect(component.result()?.verdict).toBe('CONTRIBUTION_DUE');
    expect(component.showForm()).toBe(false);
  });

  it('verdictBannerClass : or pour DONNEES_INSUFFISANTES, navy pour les verdicts chiffrés', () => {
    expect(component.verdictBannerClass('DONNEES_INSUFFISANTES')).toContain('--medium');
    expect(component.verdictBannerClass('CONTRIBUTION_DUE')).toContain('--available');
    expect(component.verdictBannerClass('CONTRIBUTION_EQUILIBREE')).toContain('--available');
  });
});
