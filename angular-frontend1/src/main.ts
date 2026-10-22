import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { AppComponent } from './app/app.component';

/** Starts the browser app. Tests pass a stand-in so a failed start can be observed. */
export function startMicroVault(start: typeof bootstrapApplication = bootstrapApplication): Promise<unknown> {
  return start(AppComponent, appConfig).catch((err) => console.error(err));
}

void startMicroVault();
