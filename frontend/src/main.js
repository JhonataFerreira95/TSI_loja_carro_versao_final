import { createApp } from 'vue'
import { createRouter, createWebHashHistory } from 'vue-router'
import App from './App.vue'
import LoginView from './views/LoginView.vue'
import HomeView from './views/HomeView.vue'
import CarsView from './views/CarsView.vue'
import UsersView from './views/UsersView.vue'
import PhotosView from './views/PhotosView.vue'
import LogsView from './views/LogsView.vue'
import './style.css'

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: '/login', component: LoginView },
    { path: '/', component: HomeView, meta: { requiresAuth: true } },
    { path: '/carros', component: CarsView, meta: { requiresAuth: true } },
    { path: '/usuarios', component: UsersView, meta: { requiresAuth: true } },
    { path: '/fotos', component: PhotosView, meta: { requiresAuth: true } },
    { path: '/logs', component: LogsView, meta: { requiresAuth: true, requiresAdmin: true } },
  ],
})

router.beforeEach(async (to) => {
  if (!to.meta.requiresAuth) return true

  const resposta = await fetch('/usuario/logado')
  if (!resposta.ok) return '/login'

  const usuario = await resposta.json()
  if (to.meta.requiresAdmin && usuario.papel !== 'ADMIN') return '/'
  return true
})

createApp(App).use(router).mount('#app')
