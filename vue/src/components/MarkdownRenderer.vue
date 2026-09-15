<template>
  <div class="markdown-content" :class="{ 'ai-markdown': isAiMessage }">
    <div v-html="renderedContent"></div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  content: {
    type: String,
    required: true
  },
  isAiMessage: {
    type: Boolean,
    default: false
  }
})

// 简单的Markdown渲染器
const renderedContent = computed(() => {
  let html = props.content

  // 转义HTML标签（防止XSS）
  html = html.replace(/</g, '&lt;').replace(/>/g, '&gt;')

  // 处理代码块（```）
  html = html.replace(/```(\w+)?\n([\s\S]*?)\n```/g, (match, lang, code) => {
    return `<pre class="code-block"><code class="language-${lang || 'text'}">${code.trim()}</code></pre>`
  })

  // 处理行内代码（`）
  html = html.replace(/`([^`]+)`/g, '<code class="inline-code">$1</code>')

  // 处理粗体（**）
  html = html.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')

  // 处理斜体（*）
  html = html.replace(/\*(.*?)\*/g, '<em>$1</em>')

  // 处理标题
  html = html.replace(/^### (.*$)/gm, '<h3>$1</h3>')
  html = html.replace(/^## (.*$)/gm, '<h2>$1</h2>')
  html = html.replace(/^# (.*$)/gm, '<h1>$1</h1>')

  // 处理链接
  html = html.replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2" target="_blank" rel="noopener noreferrer">$1</a>')

  // 处理无序列表
  html = html.replace(/^- (.*)$/gm, '<li>$1</li>')
  html = html.replace(/(<li>.*<\/li>)/s, '<ul>$1</ul>')

  // 处理有序列表
  html = html.replace(/^\d+\. (.*)$/gm, '<li>$1</li>')

  // 处理引用
  html = html.replace(/^> (.*)$/gm, '<blockquote>$1</blockquote>')

  // 处理分割线
  html = html.replace(/^---$/gm, '<hr>')

  // 处理换行
  html = html.replace(/\n/g, '<br>')

  // 清理多余的br标签
  html = html.replace(/<br><br>/g, '<br>')

  return html
})
</script>

<style scoped>
.markdown-content {
  line-height: 1.75;
  color: inherit;
  font-size: 15px;
  text-align: left;
}

.markdown-content h1,
.markdown-content h2,
.markdown-content h3 {
  margin: 1em 0 0.5em 0;
  font-weight: 600;
  line-height: 1.3;
}

.markdown-content h1 {
  font-size: 1.5em;
  border-bottom: 2px solid var(--color-border);
  padding-bottom: 0.3em;
}

.markdown-content h2 {
  font-size: 1.3em;
  color: var(--color-text);
}

.markdown-content h3 {
  font-size: 1.1em;
  color: var(--color-text-secondary);
}

.markdown-content p {
  margin: 0.5em 0;
}

.markdown-content ul,
.markdown-content ol {
  margin: 0.5em 0;
  padding-left: 1.5em;
}

.markdown-content li {
  margin: 0.3em 0;
}

.markdown-content blockquote {
  border-left: 3px solid var(--color-border);
  padding-left: 1em;
  margin: 1em 0;
  color: var(--color-text-secondary);
  font-style: italic;
  background: var(--color-border-light);
  border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
  padding: 0.5em 1em;
}

.ai-markdown blockquote {
  border-left-color: var(--color-primary);
  background: var(--color-primary-light);
}

.markdown-content hr {
  border: none;
  border-top: 2px solid var(--color-border);
  margin: 1.5em 0;
}

/* 行内代码文字用 --color-danger：该令牌按 spec §3.3 的标准（可直接当文字用，白底 ≥4.5:1）取值，
   故底色必须是纯白面 --color-surface（实测 4.54:1）。若沿用简报映射的 --color-border-light 底，
   同一对只有 4.10:1，低于 12.75px 文字所需的 4.5:1。 */
.markdown-content code.inline-code {
  background: var(--color-surface);
  padding: 0.2em 0.4em;
  border-radius: 0.25em;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 0.85em;
  color: var(--color-danger);
}

.ai-markdown code.inline-code {
  background: var(--color-primary-light);
  color: var(--color-primary-dark);
}

.markdown-content pre.code-block {
  background: var(--color-code-bg);
  color: var(--color-code-text);
  padding: 1em;
  border-radius: 0.5em;
  overflow-x: auto;
  margin: 1em 0;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 0.85em;
  line-height: 1.4;
}

.markdown-content pre.code-block code {
  background: none;
  padding: 0;
  color: inherit;
}

.markdown-content a {
  color: var(--color-primary);
  text-decoration: none;
  border-bottom: 1px solid transparent;
  transition: border-color 0.2s ease;
}

.markdown-content a:hover {
  border-bottom-color: var(--color-primary);
}

.ai-markdown a {
  color: var(--color-primary-dark);
}

.ai-markdown a:hover {
  border-bottom-color: var(--color-primary-dark);
}

.markdown-content strong {
  font-weight: 600;
  color: var(--color-text);
}

.ai-markdown strong {
  color: var(--color-primary-dark);
}

.markdown-content em {
  font-style: italic;
  color: var(--color-text-secondary);
}
</style>





