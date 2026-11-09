const light = {
  color: ['#557596', '#2f7d5b', '#b08a5a', '#8f7aa6', '#5f9a9a'],
  backgroundColor: 'transparent',
  textStyle: { color: '#4b5563' },
  title: { textStyle: { color: '#111827' } },
  legend: { textStyle: { color: '#4b5563' } },
  tooltip: {
    backgroundColor: '#ffffff',
    borderColor: '#e4e8ef',
    textStyle: { color: '#111827' }
  },
  categoryAxis: {
    axisLine: { lineStyle: { color: '#e4e8ef' } },
    axisTick: { lineStyle: { color: '#e4e8ef' } },
    axisLabel: { color: '#7c869c' },
    splitLine: { lineStyle: { color: '#eef2f7' } }
  },
  valueAxis: {
    axisLine: { lineStyle: { color: '#e4e8ef' } },
    axisTick: { lineStyle: { color: '#e4e8ef' } },
    axisLabel: { color: '#7c869c' },
    splitLine: { lineStyle: { color: '#eef2f7' } }
  }
}

const dark = {
  color: ['#a8bdd3', '#6fa98b', '#c4a36c', '#ad98c2', '#7db3b3'],
  backgroundColor: 'transparent',
  textStyle: { color: '#c9d1d9' },
  title: { textStyle: { color: '#f0f6fc' } },
  legend: { textStyle: { color: '#c9d1d9' } },
  tooltip: {
    backgroundColor: '#1a212b',
    borderColor: '#303946',
    textStyle: { color: '#e6edf7' }
  },
  categoryAxis: {
    axisLine: { lineStyle: { color: '#2b3441' } },
    axisTick: { lineStyle: { color: '#2b3441' } },
    axisLabel: { color: '#8b949e' },
    splitLine: { lineStyle: { color: '#222c39' } }
  },
  valueAxis: {
    axisLine: { lineStyle: { color: '#2b3441' } },
    axisTick: { lineStyle: { color: '#2b3441' } },
    axisLabel: { color: '#8b949e' },
    splitLine: { lineStyle: { color: '#222c39' } }
  }
}

let registered = false

export function registerChartThemes(echarts) {
  if (registered) return
  echarts.registerTheme('dsh-light', light)
  echarts.registerTheme('dsh-dark', dark)
  registered = true
}

export function chartThemeName(dark) {
  return dark ? 'dsh-dark' : 'dsh-light'
}

export const chartTokens = { light, dark }
