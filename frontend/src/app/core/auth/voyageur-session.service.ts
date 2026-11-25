import { Injectable, Injector, computed, effect, inject, signal } from '@angular/core';
import { AuthApiService } from '../api/auth-api.service';
import { LoginRequest, VoyageurResponse } from '../../shared/models/voyageur.model';
import { EMPTY, Observable, catchError, filter, switchMap, tap, timer } from 'rxjs';
import { VoyageursApiService } from '../api/voyageurs-api.service';
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
  private readonly injector = inject(Injector);

  readonly voyageur = computed(() => this.sessionState()?.voyageur ?? null);
  readonly voyageurId = computed(() => this.sessionState()?.voyageur.id ?? null);
  readonly matricule = computed(() => this.sessionState()?.voyageur.matricule ?? null);
  readonly token = computed(() => this.sessionState()?.token ?? null);
  readonly isAuthenticated = computed(() => !!this.sessionState()?.voyageur.id);
  readonly participation = computed(() => this.voyageur()?.participation);

  constructor() {
    effect((onCleanup) => {
      const id = this.voyageurId();
      const token = this.token();
      const participation = this.participation();
      if (!id || !token) return;

      const fin = participation?.suspensionActive && participation.finSuspension;
      const attente = fin ? Math.min(60_000, Math.max(1_000, Date.parse(fin) - Date.now())) : 60_000;
      const abonnement = timer(attente, 60_000).pipe(
        switchMap(() => {
          const session = this.sessionState();
          return this.injector.get(VoyageursApiService).getCurrent().pipe(
            filter(() => this.sessionState() === session),
            catchError(() => EMPTY),
          );
        }),
      ).subscribe((profil) => {
        if (profil.id === id && this.voyageurId() === id && this.token() === token) {
          this.setVoyageur(profil, token);
        }
      });
      onCleanup(() => abonnement.unsubscribe());
    });
  }

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
