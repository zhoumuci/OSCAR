import { createRouter, createWebHistory } from "vue-router";
import type { RouteRecordRaw } from "vue-router";
import HomeView from "@/views/HomeView.vue";

const routes: RouteRecordRaw[] = [
  { path: "/", name: "Home", component: HomeView },


  { path: "/browse", component: () => import("@/views/DataBrowseView.vue") },
  { path: "/search", component: () => import("@/views/SearchView.vue") },
  { path: "/analysis", component: () => import("@/views/AnalysisView.vue") },
  { path: "/download", component: () => import("@/views/DownloadView.vue") },
  { path: "/contact", component: () => import("@/views/ContactView.vue") },
  { path: "/helps", component: () => import("@/views/HelpView.vue") },
  {
    path: "/sample/:id",
    name: "SampleDetail",
    component: () => import("@/views/SearchResultView.vue"),
  },
  {
    path: "/search/result",
    redirect: (to) => ({
      name: "SampleDetail",
      params: { id: String(to.query.id || "H_000001") },
      query: { domain: String(to.query.domain || "integration"), source: "search" },
    }),
  },
  {
    path: "/search-result/:id",
    redirect: (to) => `/sample/${to.params.id}`,
  },
  {
    path: "/feature-detail",
    name: "FeatureDetail",
    component: () => import("@/views/FeatureDetailView.vue"),
  },
];

const base = import.meta.env.VITE_PUBLIC_BASE || '/';

export const router = createRouter({
  history: createWebHistory(base),
  routes,
  scrollBehavior() {
    return { top: 0 };
  },
});
