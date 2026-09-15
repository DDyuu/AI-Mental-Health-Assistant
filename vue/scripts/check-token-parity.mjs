// 校验 chart-palette.js 的每个色值都存在于 tokens.scss —— 防止令牌镜像漂移
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, resolve } from 'node:path'

const here = dirname(fileURLToPath(import.meta.url))
const tokensPath = resolve(here, '../src/styles/tokens.scss')
const palettePath = resolve(here, '../src/styles/chart-palette.js')

const normalize = (s) => s.toLowerCase().replace(/\s+/g, '')

const collect = (text) => {
  const hex = text.match(/#[0-9a-fA-F]{3,8}\b/g) || []
  const rgba = text.match(/rgba?\([^)]*\)/g) || []
  return new Set([...hex, ...rgba].map(normalize))
}

const tokens = collect(readFileSync(tokensPath, 'utf8'))
const palette = collect(readFileSync(palettePath, 'utf8'))

/* 空色板守卫：色板若解析出 0 个色值（例如 chartColors 被改成 {}），
   missing 恒为空数组，脚本会打印"0 个色值全部来自 tokens.scss"并退出 0——
   这是本校验唯一构造得出的假绿路径。宁可失败，不可假绿。 */
if (palette.size === 0) {
  console.error('x chart-palette.js 未解析出任何色值，校验无从进行（色板为空或写法不可识别）。')
  console.error('修复：确认该文件仍导出含 #hex / rgb() / rgba() 色值的 chartColors 与 chartSeries。')
  process.exit(1)
}

const missing = [...palette].filter((c) => !tokens.has(c))

if (missing.length > 0) {
  console.error('x chart-palette.js 中存在 tokens.scss 未定义的色值：')
  missing.forEach((c) => console.error('  ' + c))
  console.error('修复：先在 tokens.scss 定义该令牌，再回到 chart-palette.js 同步。')
  process.exit(1)
}

console.log(`ok 图表色板与令牌一致（${palette.size} 个色值全部来自 tokens.scss）`)
