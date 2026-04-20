import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { RuptureAmiableInfoSectionComponent } from './rupture-amiable-info-section.component';

describe('RuptureAmiableInfoSectionComponent', () => {
  let component: RuptureAmiableInfoSectionComponent;
  let fixture: ComponentFixture<RuptureAmiableInfoSectionComponent>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RuptureAmiableInfoSectionComponent, HttpClientTestingModule, NoopAnimationsModule],
    }).compileComponents();
    fixture = TestBed.createComponent(RuptureAmiableInfoSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    httpMock = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => httpMock.verify());

  it('rendu nominal : les 2 messages sont visibles', () => {
    const html = fixture.nativeElement as HTMLElement;
    expect(html.textContent).toContain("Aucun barème légal ne s'impose");
    expect(html.textContent).toContain("indemnité compensatoire de préavis");
  });

  it('toggle collapsed cache le contenu', () => {
    expect(component.collapsed()).toBe(false);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(true);
    fixture.detectChanges();
    const body = (fixture.nativeElement as HTMLElement).querySelector('#rai-body');
    expect(body).toBeNull();
  });

  it('ne déclenche aucun appel HTTP', () => {
    // Le composant est purement informationnel — aucun service/appel attendu
    httpMock.expectNone(() => true);
  });
});
