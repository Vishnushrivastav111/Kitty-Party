import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../../service/auth.service';
import { homePath } from '../../../shared/format';

@Component({
  selector: 'app-logout',
  standalone: true,
  templateUrl: './logout.component.html',
  styleUrl: './logout.component.scss',
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
