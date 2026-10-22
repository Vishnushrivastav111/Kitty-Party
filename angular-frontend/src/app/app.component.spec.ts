import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterOutlet } from '@angular/router';
import { AppComponent } from './app.component';
import { DialogHostComponent } from './shared/dialog-host.component';

@Component({ selector: 'app-dialog-host', standalone: true, template: '' })
class DialogHostStubComponent {}

describe('AppComponent', () => {
  let fixture: ComponentFixture<AppComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AppComponent],
    })
      .overrideComponent(AppComponent, {
        remove: { imports: [DialogHostComponent] },
        add: { imports: [DialogHostStubComponent] },
      })
      .compileComponents();

    fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
  });

  it('creates the root component', () => {
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('renders the router outlet', () => {
    expect(fixture.nativeElement.querySelector('router-outlet')).not.toBeNull();
  });

  it('imports RouterOutlet in the standalone component', () => {
    expect(AppComponent.ɵcmp.imports).toContain(RouterOutlet);
  });
});
