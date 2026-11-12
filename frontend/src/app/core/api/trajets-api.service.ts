import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, shareReplay } from 'rxjs';
import { CreateTrajetRequest, TrajetRequest, TrajetResponse, VilleOption, RechercheTrajet } from '../../shared/models/trajet.model';
import { apiUrl } from './api-url';

@Injectable({ providedIn: 'root' })
export class TrajetsApiService {
  private villes$?: Observable<VilleOption[]>;

  constructor(private readonly http: HttpClient) {}

  getAll(): Observable<TrajetResponse[]> {
    return this.http.get<TrajetResponse[]>(apiUrl('/trajets'));
  }

  getById(id: number): Observable<TrajetResponse> {
    return this.http.get<TrajetResponse>(apiUrl(`/trajets/${id}`));
  }

  getPublic(recherche: RechercheTrajet = {}): Observable<TrajetResponse[]> {
    const params = Object.fromEntries(Object.entries(recherche).filter(([, valeur]) => !!valeur));
    return this.http.get<TrajetResponse[]>(apiUrl('/trajets/public'), { params });
  }

  getVilles(): Observable<VilleOption[]> {
    return this.villes$ ??= this.http.get<VilleOption[]>(apiUrl('/trajets/villes')).pipe(shareReplay(1));
  }

  getByAgence(agenceId: number): Observable<TrajetResponse[]> {
    return this.http.get<TrajetResponse[]>(apiUrl(`/trajets/agence/${agenceId}`));
  }

  getByOrganisateur(organisateurId: number): Observable<TrajetResponse[]> {
    return this.http.get<TrajetResponse[]>(apiUrl(`/trajets/organisateur/${organisateurId}`));
  }

  create(payload: CreateTrajetRequest): Observable<TrajetResponse> {
    return this.http.post<TrajetResponse>(apiUrl('/trajets'), payload);
  }

  update(id: number, payload: TrajetRequest): Observable<TrajetResponse> {
    return this.http.put<TrajetResponse>(apiUrl(`/trajets/${id}`), payload);
  }

  cancel(id: number, requesterId: number): Observable<void> {
    return this.http.patch<void>(apiUrl(`/trajets/${id}/cancel?requesterId=${requesterId}`), {});
  }

  convertToPublic(id: number): Observable<void> {
    return this.http.patch<void>(apiUrl(`/trajets/${id}/convert-public`), {});
  }
}
