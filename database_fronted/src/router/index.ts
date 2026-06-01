import { createRouter, createWebHistory } from "vue-router";
import type { RouteRecordRaw } from "vue-router";
import HomeView from "@/views/HomeView.vue";

const routes: RouteRecordRaw[] = [
  { path: "/", name: "Home", component: HomeView },


  { path: "/browse", component: () => import("@/views/DataBrowseView.vue") },
  { path: "/search", component: () => import("@/views/SearchView.vue") },
  { path: "/analysis", component: () => import("@/views/AnalysisView.vue") },
  { path: "/download", component: () => import("@/views/DownloadView.vue") },
  { path: "/stats", component: HomeView },
  { path: "/contact", component: () => import("@/views/ContactView.vue") },
  { path: "/help", component: HomeView },
  {
    path: "/search/result",
    name: "SearchResult",
    component: () => import("@/views/SearchResultView.vue"),
  },
  {
    path: "/search-result/:id",
    component: () => import("@/views/SearchResultView.vue"),
  },
  {
    path: "/feature-detail",
    name: "FeatureDetail",
    component: () => import("@/views/FeatureDetailView.vue"),
  },
];

export const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 };
  },
});
