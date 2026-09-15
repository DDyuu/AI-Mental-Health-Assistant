// ECharts 色板 —— tokens.scss 的镜像
//
// 存在的唯一原因：ECharts 的 color / borderColor 需要真实色值，无法消费 CSS 变量。
// 一致性由 `npm run check:tokens`（vue/scripts/check-token-parity.mjs）机械校验：
// 本文件中的每个色值都必须能在 tokens.scss 中找到。
// 修改流程：先改 tokens.scss，再同步此文件，然后跑校验。
//
// 键名与 --color-<name> 一一对应。

export const chartColors = {
  primary: '#40776B',
  primarySoft: '#5B9E8F',
  primaryDark: '#2F5A51',
  primaryLight: '#E9F3F0',
  accent: '#C97B4A',
  accentText: '#A85F2E',
  accentLight: '#FBF0E8',
  success: '#3D7A5F',
  successLight: '#E7F2EC',
  warning: '#8A6220',
  warningLight: '#F7EFE0',
  danger: '#B85A52',
  dangerLight: '#F8E9E7',
  info: '#4A6E91',
  infoLight: '#E9F0F6',
  text: '#2C3A36',
  textSecondary: '#5F6F6A',
  textPlaceholder: '#9AA8A3',
  border: '#E6EDEA',
  borderLight: '#F0F4F2',
  surface: '#FFFFFF',
  bg: '#F7F9F8',
  primaryWashStrong: 'rgba(64, 119, 107, 0.24)',
  accentWashStrong: 'rgba(201, 123, 74, 0.24)',
  infoWashStrong: 'rgba(74, 110, 145, 0.30)',
  successWashStrong: 'rgba(61, 122, 95, 0.40)'
}

// 多系列图表的默认色序（柔和降饱和，与全站色板同源）
export const chartSeries = [
  chartColors.primarySoft,
  chartColors.accent,
  chartColors.info,
  chartColors.warning,
  chartColors.success,
  chartColors.danger
]
