import { createRouter, createWebHistory } from 'vue-router';

export const router = createRouter({
  history: createWebHistory('/confirm-screen/'),
  routes: [
    { path: '/', redirect: '/standby' },
    { path: '/bind', component: () => import('../views/bind/BindView.vue') },
    { path: '/standby', component: () => import('../views/standby/StandbyView.vue') },
    { path: '/success', component: () => import('../views/success/SuccessView.vue') },
    { path: '/offline', component: () => import('../views/offline/OfflineView.vue') }
  ]
});
