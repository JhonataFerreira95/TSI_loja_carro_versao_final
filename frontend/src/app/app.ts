import { Component, inject, OnInit } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { Api } from './core/api';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App implements OnInit {
  readonly api = inject(Api);
  readonly router = inject(Router);
  menuAberto = false;
  erroLogout = '';

  ngOnInit() {
    this.api.carregarSessao().subscribe({ error: () => this.api.usuario.set(null) });
  }

  sair() {
    this.erroLogout = '';
    this.api.logout().subscribe({
      next: () => {
        this.menuAberto = false;
        void this.router.navigateByUrl('/login');
      },
      error: () => {
        this.erroLogout = 'Não foi possível encerrar a sessão. Tente novamente.';
      }
    });
  }
}
