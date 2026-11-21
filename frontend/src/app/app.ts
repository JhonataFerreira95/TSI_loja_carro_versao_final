import { Component, inject, OnInit } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { Api } from './core/api';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App implements OnInit {
  readonly api = inject(Api);
  menuAberto = false;

  ngOnInit() {
    this.api.carregarSessao().subscribe({ error: () => this.api.usuario.set(null) });
  }

  sair() {
    this.api.logout().subscribe();
  }
}
