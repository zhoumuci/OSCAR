import axios from "axios";
import { buildApiUrl } from "@/config/api";

export interface VisitorPoint {
  name: string;
  lat: number;
  lon: number;
  value: number;
}

export async function fetchVisitorPoints(): Promise<VisitorPoint[]> {
  const { data } = await axios.get<VisitorPoint[]>(
    buildApiUrl("api/visitors/map-points")
  );
  return data;
}

export interface VisitorStats {
  totalVisitors: number;
  countryCount: number;
  activeToday: number;
}

export async function fetchVisitorStats(): Promise<VisitorStats> {
  const { data } = await axios.get<VisitorStats>(
    buildApiUrl("api/visitors/stats")
  );
  return data;
}

/**
 * 每次页面加载上报一次访问（同 IP 多次打开累计多次）。
 * 静默失败：统计接口挂了也不影响页面。
 */
export async function reportVisit(): Promise<void> {
  try {
    await axios.post(buildApiUrl("api/visitors/visit"));
  } catch {
    // ignore
  }
}
