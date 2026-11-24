import { Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Api, Usuario } from '../../core/api';

@Component({
  selector: 'app-admin',
  imports: [RouterLink],
  templateUrl: './admin.html',
  styleUrl: './admin.scss',
})
export class Admin implements OnInit {
  carros = 0;
  agendamentos = 0;
  usuarios = 0;
  listaUsuarios: Usuario[] = [];
  carregandoUsuarios = true;
  excluindoUsuarioId: number | null = null;
  mensagemUsuarios = '';
  erroUsuarios = false;

  constructor(private api: Api) {}

  get usuarioLogadoId() {
    return this.api.usuario()?.id;
  }

  ngOnInit() {
    this.api.listarCarros().subscribe(itens => this.carros = itens.length);
    this.api.listarAgendamentos().subscribe(itens => this.agendamentos = itens.length);
    this.carregarUsuarios();
  }

  carregarUsuarios() {
    this.carregandoUsuarios = true;
    this.api.listarUsuarios().subscribe({
      next: itens => {
        this.listaUsuarios = itens;
        this.usuarios = itens.length;
        this.carregandoUsuarios = false;
      },
      error: resposta => {
        this.erroUsuarios = true;
        this.mensagemUsuarios = resposta.error?.erro || 'Não foi possível carregar os usuários.';
        this.carregandoUsuarios = false;
      }
    });
  }

  excluirUsuario(usuario: Usuario) {
    if (usuario.id === this.api.usuario()?.id) {
      this.erroUsuarios = true;
      this.mensagemUsuarios = 'Não é possível excluir sua própria conta.';
      return;
    }

    if (!confirm(`Deseja excluir o usuário ${usuario.nome}?`)) return;

    this.excluindoUsuarioId = usuario.id;
    this.mensagemUsuarios = '';
    this.api.excluirUsuario(usuario.id).subscribe({
      next: () => {
        this.listaUsuarios = this.listaUsuarios.filter(item => item.id !== usuario.id);
        this.usuarios = this.listaUsuarios.length;
        this.mensagemUsuarios = `Usuário ${usuario.nome} excluído.`;
        this.erroUsuarios = false;
        this.excluindoUsuarioId = null;
      },
      error: resposta => {
        this.mensagemUsuarios = resposta.error?.erro || 'Não foi possível excluir o usuário.';
        this.erroUsuarios = true;
        this.excluindoUsuarioId = null;
      }
    });
  }
}
