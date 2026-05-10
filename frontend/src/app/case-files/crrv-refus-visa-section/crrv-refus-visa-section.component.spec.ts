import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { CrrvRefusVisaSectionComponent } from './crrv-refus-visa-section.component';

describe('CrrvRefusVisaSectionComponent', () => {
  let component: CrrvRefusVisaSectionComponent;
  let fixture: ComponentFixture<CrrvRefusVisaSectionComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CrrvRefusVisaSectionComponent, NoopAnimationsModule],
    }).compileComponents();

    fixture = TestBed.createComponent(CrrvRefusVisaSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'test-case-id';
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should toggle collapsed state', () => {
    expect(component.collapsed()).toBe(false);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(true);
  });

  it('should expand when forceExpanded is true', () => {
    component.collapsed.set(true);
    component.forceExpanded = true;
    component.ngOnInit();
    expect(component.collapsed()).toBe(false);
  });
});
