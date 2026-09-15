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

const missing = [...palette].filter((c) => !tokens.has(c))

if (missing.length > 0) {
  console.error('x chart-palette.js 中存在 tokens.scss 未定义的色值：')
  missing.forEach((c) => console.error('  ' + c))
  console.error('修复：先在 tokens.scss 定义该令牌，再回到 chart-palette.js 同步。')
  process.exit(1)
}

console.log(`ok 图表色板与令牌一致（${palette.size} 个色值全部来自 tokens.scss）`)
