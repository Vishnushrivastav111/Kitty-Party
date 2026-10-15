import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { DialogHostComponent } from './shared/dialog-host.component';
import { ToastHostComponent } from './shared/toast-host.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, DialogHostComponent, ToastHostComponent],
  template: `<router-outlet /><app-dialog-host /><app-toast-host />`,
})
export class AppComponent {}
