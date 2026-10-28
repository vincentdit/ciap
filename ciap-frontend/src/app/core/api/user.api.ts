import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse, PageResponse } from '../models/api.models';

export interface UserAccount {
  id: number;
  email: string;
  fullName: string;
  phone: string | null;
  enabled: boolean;
  roles: string[];
}

export interface CreateStaffRequest {
  email: string;
  password: string;
  fullName: string;
  phone?: string;
  roles: string[];
}

@Injectable({ providedIn: 'root' })
export class UserApi {
  private readonly base = environment.apiBaseUrl;

  constructor(private http: HttpClient) {}

  list(page = 0, size = 20, q?: string): Observable<ApiResponse<PageResponse<UserAccount>>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (q?.trim()) params = params.set('q', q.trim());
    return this.http.get<ApiResponse<PageResponse<UserAccount>>>(`${this.base}/users`, { params });
  }

  createStaff(body: CreateStaffRequest): Observable<ApiResponse<UserAccount>> {
    return this.http.post<ApiResponse<UserAccount>>(`${this.base}/auth/staff`, body);
  }

  updateRole(id: number, role: string): Observable<ApiResponse<UserAccount>> {
    return this.http.put<ApiResponse<UserAccount>>(`${this.base}/users/${id}/role`, { role });
  }

  setEnabled(id: number, enabled: boolean): Observable<ApiResponse<UserAccount>> {
    return this.http.patch<ApiResponse<UserAccount>>(`${this.base}/users/${id}/enabled`, { enabled });
  }
}
