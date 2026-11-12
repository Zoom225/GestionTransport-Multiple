import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { AgenceRequest, AgenceResponse } from '../../shared/models/agence-vehicule.model';
import { apiUrl } from './api-url';

@Injectable({ providedIn: 'root' })
export class AgencesApiService {
  constructor(private readonly http: HttpClient) {}

  getAll(): Observable<AgenceResponse[]> {
    return this.http.get<AgenceResponse[]>(apiUrl('/agences'));
  }

  getById(id: number): Observable<AgenceResponse> {
    return this.http.get<AgenceResponse>(apiUrl(`/agences/${id}`));
  }

  create(payload: AgenceRequest): Observable<AgenceResponse> {
    return this.http.post<AgenceResponse>(apiUrl('/agences'), payload);
  }

  update(id: number, payload: AgenceRequest): Observable<AgenceResponse> {
    return this.http.put<AgenceResponse>(apiUrl(`/agences/${id}`), payload);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(apiUrl(`/agences/${id}`));
  }
}

