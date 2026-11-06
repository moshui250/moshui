<template>
  <footer class="site-footer">
    <div class="footer-inner">
      <span><strong>CodeAgent</strong> 智码刷题 · 让每一次报错都有回应</span>
    </div>
  </footer>
</template>

<style scoped>
.site-footer {
  margin-top: 72px;
  border-top: 1px solid #e8eaf0;
  background: #fff;
}

.footer-inner {
  width: min(1240px, calc(100% - 48px));
  margin: 0 auto;
  padding: 26px 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  color: #94a3b8;
  font-size: 12px;
}

.footer-inner strong {
  color: #475569;
  font-weight: 700;
}

.footer-meta {
  letter-spacing: 0.04em;
}

@media (max-width: 720px) {
  .footer-inner {
    width: calc(100% - 32px);
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
}
</style>
