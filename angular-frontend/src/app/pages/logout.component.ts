import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { homePath } from '../core/format';

@Component({
  selector: 'app-logout',
  standalone: true,
  template: `
    <div class="panel" style="max-width:480px;margin:40px auto;text-align:center">
      <div style="font-size:42px;color:var(--navy);margin-bottom:12px"><i class="fas fa-right-from-bracket"></i></div>
      <h1 style="color:var(--navy);margin-bottom:8px">Sign out</h1>
      <p style="color:var(--muted);margin-bottom:20px">Are you sure you want to end your MicroVault session?</p>
      <div class="form-actions" style="justify-content:center">
        <button type="button" class="btn btn-outline" (click)="cancel()">Cancel</button>
        <button type="button" class="btn btn-danger" (click)="confirm()">Logout</button>
      </div>
    </div>
  `,
})
export class LogoutComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  cancel(): void {
    void this.router.navigateByUrl(homePath(this.auth.user()?.role));
  }

  async confirm(): Promise<void> {
    await this.auth.logout();
    await this.router.navigateByUrl('/login');
  }
}
