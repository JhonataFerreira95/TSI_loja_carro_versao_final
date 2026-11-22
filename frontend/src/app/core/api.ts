import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { tap } from 'rxjs';

export interface Usuario {
  id: number;
  nome: string;
  email: string;
  papel: 'USER' | 'ADMIN';
}

export interface Carro {
  id: number;
  modelo: string;
  ano: number;
  preco: number;
  status: 'DISPONIVEL' | 'VENDIDO' | 'INATIVO';
  imagem?: string;
  cadastradoPor?: Usuario;
}

export interface Agendamento {
  id: number;
  carro: Carro;
  cliente: Usuario;
  dataHora: string;
  dataCriacao: string;
  status: 'PENDENTE' | 'CONFIRMADO' | 'CANCELADO' | 'REALIZADO' | 'CONCLUIDO';
  observacao?: string;
}

@Injectable({
  providedIn: 'root',
})
export class Api {
  readonly usuario = signal<Usuario | null>(null);

  constructor(private http: HttpClient) {}

  carregarSessao() {
    return this.http.get<Usuario>('/usuario/logado').pipe(
      tap(usuario => this.usuario.set(usuario))
    );
  }

  login(email: string, senha: string) {
    return this.http.post<Usuario>('/usuario/login', { email, senha }).pipe(
      tap(usuario => this.usuario.set(usuario))
    );
  }

  logout() {
    return this.http.post('/usuario/logout', {}).pipe(
      tap(() => this.usuario.set(null))
    );
  }

  cadastrarUsuario(dados: { nome: string; email: string; senha: string }) {
    return this.http.post<Usuario>('/usuario/salvar', dados);
  }

  listarCarros() { return this.http.get<Carro[]>('/carro/listarCarros'); }
  buscarCarro(id: number) { return this.http.get<Carro>(`/carro/${id}`); }
  meusCarros() { return this.http.get<Carro[]>('/carro/meusCarros'); }
  listarUsuarios() { return this.http.get<Usuario[]>('/usuario/listarUsuarios'); }

  salvarCarro(dados: FormData, id?: number) {
    return id
      ? this.http.put<Carro>(`/carro/${id}`, dados)
      : this.http.post<Carro>('/carro/salvar', dados);
  }

  excluirCarro(id: number) { return this.http.delete(`/carro/${id}`, { observe: 'response' }); }
  removerImagem(id: number) { return this.http.delete(`/carro/${id}/imagem`, { observe: 'response' }); }

  atualizarUsuario(id: number, dados: Partial<Usuario & { senha: string }>) {
    return this.http.put<Usuario>(`/usuario/${id}`, dados);
  }
  excluirUsuario(id: number) { return this.http.delete(`/usuario/${id}`, { observe: 'response' }); }

  criarAgendamento(dados: { carroId: number; dataHora: string; observacao?: string }) {
    return this.http.post<Agendamento>('/agendamento', dados);
  }
  meusAgendamentos() { return this.http.get<Agendamento[]>('/agendamento/meus'); }
  horariosDisponiveis(carroId: number, data: string) {
    return this.http.get<string[]>(`/agendamento/disponibilidade?carroId=${carroId}&data=${data}`);
  }
  cancelarAgendamento(id: number) {
    return this.http.put<Agendamento>(`/agendamento/${id}/cancelar`, {});
  }
  listarAgendamentos(filtros: { data?: string; carroId?: number } = {}) {
    const parametros = new URLSearchParams();
    if (filtros.data) parametros.set('data', filtros.data);
    if (filtros.carroId) parametros.set('carroId', String(filtros.carroId));
    const queryString = parametros.toString();
    const query = queryString ? `?${queryString}` : '';
    return this.http.get<Agendamento[]>(`/agendamento${query}`);
  }
  alterarStatus(id: number, status: string) {
    return this.http.put<Agendamento>(`/agendamento/${id}/status?status=${status}`, {});
  }
  alterarStatusCarro(id: number, status: Carro['status']) {
    return this.http.put<Carro>(`/carro/${id}/status?status=${status}`, {});
  }
}
