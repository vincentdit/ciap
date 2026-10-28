import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  ApiResponse, DepartmentMember, DepartmentQueueSummary, DepartmentSummary
} from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class DepartmentApi {
  private readonly base = `${environment.apiBaseUrl}/departments`;

  constructor(private http: HttpClient) {}

  list(): Observable<ApiResponse<DepartmentSummary[]>> {
    return this.http.get<ApiResponse<DepartmentSummary[]>>(this.base);
  }

  summary(): Observable<ApiResponse<DepartmentQueueSummary>> {
    return this.http.get<ApiResponse<DepartmentQueueSummary>>(`${this.base}/summary`);
  }

  appoint(departmentId: number, userId: number): Observable<ApiResponse<DepartmentSummary>> {
    return this.http.put<ApiResponse<DepartmentSummary>>(`${this.base}/${departmentId}/hod`, { userId });
  }

  addMember(departmentId: number, userId: number, canReceiveWork = true): Observable<ApiResponse<DepartmentMember>> {
    return this.http.post<ApiResponse<DepartmentMember>>(`${this.base}/${departmentId}/members`, { userId, canReceiveWork });
  }

  members(departmentId: number): Observable<ApiResponse<DepartmentMember[]>> {
    return this.http.get<ApiResponse<DepartmentMember[]>>(`${this.base}/${departmentId}/members`);
  }
}
