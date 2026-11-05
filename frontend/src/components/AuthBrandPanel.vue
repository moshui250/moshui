<script setup>
import { DataAnalysis, MagicStick, Reading } from '@element-plus/icons-vue'
</script>

<template>
  <section class="auth-brand">
    <div class="auth-brand-top">
      <div class="brand-mark">&lt;/&gt;</div>
      <div>
        <strong>CodeAgent</strong>
        <span>AI 编程练习</span>
      </div>
    </div>

    <div class="auth-hero">
      <div class="auth-eyebrow">AI-NATIVE CODE PRACTICE</div>
      <h1>把“不会做”<br /><em>拆成下一步。</em></h1>
      <p>
        从一道题开始，先写第一版，再看判题结果。AI 会指出具体哪一行、
        为什么错，把卡住的地方拆成可以继续的小步骤。
      </p>

      <div class="auth-metrics">
        <div><strong>15</strong><span>道公开题</span></div>
        <div><strong>5</strong><span>个知识点</span></div>
        <div><strong>2</strong><span>种语言</span></div>
      </div>

      <div class="auth-features">
        <div class="auth-feature">
          <div class="feature-icon">
            <el-icon><Reading /></el-icon>
          </div>
          <div>
            <strong>先写第一版</strong>
            <span>Monaco 编辑器、C++ / Java 模板与公开样例。</span>
          </div>
        </div>
        <div class="auth-feature">
          <div class="feature-icon">
            <el-icon><MagicStick /></el-icon>
          </div>
          <div>
            <strong>让 AI 指出下一步</strong>
            <span>定位失败用例和行号，再给提示，不直接给完整代码。</span>
          </div>
        </div>
        <div class="auth-feature">
          <div class="feature-icon">
            <el-icon><DataAnalysis /></el-icon>
          </div>
          <div>
            <strong>错题变成同类练习</strong>
            <span>自动收录错题，按知识点复盘，再推荐三道同类题。</span>
          </div>
        </div>
      </div>
    </div>

    <div class="auth-brand-foot">
      <span class="foot-dot"></span>
      <span>CodeAgent · 让每一次报错都有回应</span>
    </div>
  </section>
</template>
