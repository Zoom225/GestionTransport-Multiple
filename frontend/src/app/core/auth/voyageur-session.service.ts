import { Injectable, computed, inject, signal } from '@angular/core';
import { AuthApiService } from '../api/auth-api.service';
import { LoginRequest, VoyageurResponse } from '../../shared/models/voyageur.model';
import { Observable, tap } from 'rxjs';
import { AdminSessionService } from './admin-session.service';

export const VOYAGEUR_SESSION_KEY = 'gestiontransport_voyageur_session';

interface VoyageurSessionState {
  voyageur: VoyageurResponse;
  token: string | null;
}

@Injectable({ providedIn: 'root' })
export class VoyageurSessionService {
  private readonly sessionState = signal<VoyageurSessionState | null>(this.loadFromStorage());
  private readonly authApi = inject(AuthApiService);
  private readonly adminSession = inject(AdminSessionService);

  readonly voyageur = computed(() => this.sessionState()?.voyageur ?? null);
  readonly voyageurId = computed(() => this.sessionState()?.voyageur.id ?? null);
  readonly matricule = computed(() => this.sessionState()?.voyageur.matricule ?? null);
  readonly token = computed(() => this.sessionState()?.token ?? null);
  readonly isAuthenticated = computed(() => !!this.sessionState()?.voyageur.id);

  setVoyageur(voyageur: VoyageurResponse, token?: string): void {
    const state: VoyageurSessionState = { voyageur, token: token ?? voyageur.token ?? null };
    this.sessionState.set(state);
    localStorage.setItem(VOYAGEUR_SESSION_KEY, JSON.stringify(state));
  }

  clearVoyageur(): void {
    this.sessionState.set(null);
    localStorage.removeItem(VOYAGEUR_SESSION_KEY);
  }

  /**
   * Authentifie un voyageur et stocke le token + voyageur en session
   */
  login(matricule: string, password: string): Observable<VoyageurResponse> { // Modified
    const payload: LoginRequest = { matricule, password }; // Modified
    return this.authApi.loginVoyageur(payload).pipe(
      tap((response: VoyageurResponse) => {
        this.adminSession.clearSession();
        this.setVoyageur(response, response.token);
      }),
    );
  }

  private loadFromStorage(): VoyageurSessionState | null {
    const raw = localStorage.getItem(VOYAGEUR_SESSION_KEY);
    if (!raw) {
      return null;
    }

    try {
      const parsed = JSON.parse(raw);
      // Migration of old local storage data
      if (parsed && 'id' in parsed) {
        return { voyageur: parsed as VoyageurResponse, token: parsed.token ?? null };
      }
      return parsed as VoyageurSessionState;
    } catch {
      localStorage.removeItem(VOYAGEUR_SESSION_KEY);
      return null;
    }
  }
}
