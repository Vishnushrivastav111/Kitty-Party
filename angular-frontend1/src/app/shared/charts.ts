import { Chart, ChartConfiguration, registerables } from 'chart.js';

Chart.register(...registerables);

const live = new Set<Chart>();

export function mountChart(canvas: HTMLCanvasElement | undefined | null, config: ChartConfiguration): Chart | null {
  if (!canvas) return null;
  const existing = Chart.getChart(canvas);
  existing?.destroy();
  const chart = new Chart(canvas, config);
  live.add(chart);
  return chart;
}

export function clearCharts(): void {
  live.forEach((chart) => chart.destroy());
  live.clear();
}
