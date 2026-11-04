import { marked } from 'marked'
import DOMPurify from 'dompurify'

marked.setOptions({
  gfm: true,
  breaks: true
})

/**
 * 把题面 Markdown 转成可安全插入页面的 HTML。
 * 先交给 marked 解析，再由 DOMPurify 过滤脚本、事件属性和危险 URL。
 *
 * @param {string} content Markdown 原文
 * @returns {string} 已净化的 HTML
 */
export function renderMarkdown(content) {
  const source = String(content ?? '').trim()
  if (!source) return ''
  return DOMPurify.sanitize(marked.parse(source))
}
