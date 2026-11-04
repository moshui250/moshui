<script setup>
import { DataAnalysis, MagicStick, Reading } from '@element-plus/icons-vue'
</script>

<template>
  <section class="auth-brand">
    <div class="auth-brand-top">
      <div class="brand-mark">&lt;/&gt;</div>
      <div>
        <strong>CodeAgent</strong>
        <span>智码刷题</span>
      </div>
    </div>

    <div class="auth-hero">
      <h1>让每一次报错<br /><em>都有回应</em></h1>
      <p>
        面向编程新手的 AI 原生练习平台。从选题、编码到判题失败后的分层提示，
        把“不会做”拆成下一步可以完成的小事。
      </p>

      <div class="auth-features">
        <div class="auth-feature">
          <div class="feature-icon">
            <el-icon><Reading /></el-icon>
          </div>
          <div>
            <strong>5 个核心知识点</strong>
            <span>数组、字符串、链表、递归与排序，每个知识点配套练习。</span>
          </div>
        </div>
        <div class="auth-feature">
          <div class="feature-icon">
            <el-icon><MagicStick /></el-icon>
          </div>
          <div>
            <strong>失败后自动调试</strong>
            <span>AI 读取判题结果，先定位错误，再逐层给出提示，不直接泄题。</span>
          </div>
        </div>
        <div class="auth-feature">
          <div class="feature-icon">
            <el-icon><DataAnalysis /></el-icon>
          </div>
          <div>
            <strong>错题与学习轨迹</strong>
            <span>自动收录错题，按知识点聚合错误率，推荐同类练习。</span>
          </div>
        </div>
      </div>
    </div>

    <div class="auth-brand-foot">
      <span class="foot-dot"></span>
      <span>AI Native · Java 17 · Spring Boot 3.5 · Vue 3</span>
    </div>
  </section>
</template>
