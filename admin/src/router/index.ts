import { createRouter, createWebHistory } from 'vue-router';
import { getToken } from '../api/client';

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/dashboard' },
    { path: '/login', component: () => import('../views/login/LoginView.vue') },
    { path: '/dashboard', component: () => import('../views/dashboard/DashboardView.vue') },
    { path: '/config', component: () => import('../views/config/ConfigView.vue') },
    { path: '/event-types', component: () => import('../views/event-types/EventTypesView.vue') },
    { path: '/themes', component: () => import('../views/themes/ThemesView.vue') },
    { path: '/theme-copywriting', component: () => import('../views/theme-copywriting/ThemeCopywritingView.vue') },
    { path: '/plans', component: () => import('../views/plans/PlansView.vue') },
    { path: '/templates', component: () => import('../views/templates/TemplatesView.vue') },
    { path: '/invitations', component: () => import('../views/invitations/InvitationsView.vue') },
    { path: '/devices', component: () => import('../views/devices/DevicesView.vue') },
    { path: '/orders', component: () => import('../views/orders/OrdersView.vue') },
    { path: '/payments', component: () => import('../views/payments/PaymentsView.vue') },
    { path: '/banquets', component: () => import('../views/banquets/BanquetsView.vue') },
    { path: '/business', component: () => import('../views/business/BusinessView.vue') },
    { path: '/broadcast-logs', component: () => import('../views/broadcast/BroadcastLogsView.vue') },
    { path: '/operation-logs', component: () => import('../views/operation-logs/OperationLogsView.vue') },
    { path: '/miniapp-users', component: () => import('../views/miniapp-users/MiniappUsersView.vue') }
  ]
});

router.beforeEach((to) => {
  if (to.path !== '/login' && !getToken()) {
    return { path: '/login', query: { redirect: to.fullPath } };
  }
  if (to.path === '/login' && getToken()) {
    return { path: '/dashboard' };
  }
  return true;
});
