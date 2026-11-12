import { HttpClient, HttpHeaders } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { CreateTrajetRequest, TrajetRequest, TrajetResponse } from '../../shared/models/trajet.model';
import { apiUrl } from './api-url';
import { VoyageurSessionService } from '../auth/voyageur-session.service';

@Injectable({ providedIn: 'root' })
export class TrajetsApiService {
  private readonly voyageurSession = inject(VoyageurSessionService);

  constructor(private readonly http: HttpClient) {}

  getAll(): Observable<TrajetResponse[]> {
    return this.http.get<TrajetResponse[]>(apiUrl('/trajets'));
  }

  getById(id: number): Observable<TrajetResponse> {
    return this.http.get<TrajetResponse>(apiUrl(`/trajets/${id}`));
  }

  getPublic(): Observable<TrajetResponse[]> {
    return this.http.get<TrajetResponse[]>(apiUrl('/trajets/public'));
  }

  getByAgence(agenceId: number): Observable<TrajetResponse[]> {
    return this.http.get<TrajetResponse[]>(apiUrl(`/trajets/agence/${agenceId}`));
  }

  getByOrganisateur(organisateurId: number): Observable<TrajetResponse[]> {
    return this.http.get<TrajetResponse[]>(apiUrl(`/trajets/organisateur/${organisateurId}`));
  }

  create(payload: CreateTrajetRequest): Observable<TrajetResponse> {
    const token = this.voyageurSession.token();
    const headers = token
      ? new HttpHeaders({ Authorization: `Bearer ${token}` })
      : new HttpHeaders();
    return this.http.post<TrajetResponse>(apiUrl('/trajets'), payload, { headers });
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
