
<script setup lang="ts">
import { ref } from 'vue'

defineProps<{
  activeMenu: string
}>()

const emit = defineEmits<{
  navigate: [key: string]
}>()

const collapsed = ref(false)

const menuGroups = [
  {
    title: '工作空间',
    items: [
      { key: 'dashboard', label: '工作台', icon: 'dashboard' },
      { key: 'projects', label: '项目管理', icon: 'projects' }
    ]
  },
  {
    title: '测试资产',
    items: [
      { key: 'cases', label: 'API 用例', icon: 'cases' },
      { key: 'environments', label: '测试环境', icon: 'environment' },
      { key: 'scenarios', label: '测试场景', icon: 'scenario' }
    ]
  },
  {
    title: '测试执行',
    items: [
      { key: 'history', label: '执行历史', icon: 'history' }
    ]
  },
  {
    title: '质量分析',
    items: [
      { key: 'reports', label: '测试报告', icon: 'report' },
      { key: 'trends', label: '趋势分析', icon: 'trend' }
    ]
  }
]

function toggleSidebar() {
  collapsed.value = !collapsed.value
}
</script>

<template>
  <div class="sidebar-shell" :class="{ collapsed }">
    <aside class="sidebar">
      <div class="sidebar-brand">
        <div class="brand-logo">
          <svg viewBox="0 0 24 24" fill="none">
            <path d="M4 18V6L12 13L20 6V18" stroke="currentColor"
              stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" />
            <path d="M8 18V12M16 18V12" stroke="currentColor"
              stroke-width="2.2" stroke-linecap="round" />
          </svg>
        </div>
        <div v-if="!collapsed" class="brand-text">
          <div class="brand-name">AutoTest Hub</div>
          <div class="brand-subtitle">自动化测试管理平台</div>
        </div>
      </div>

      <div class="sidebar-content">
        <div
          v-for="group in menuGroups"
          :key="group.title"
          class="menu-group"
        >
          <div v-if="!collapsed" class="group-title">
            {{ group.title }}
          </div>

          <button
            v-for="item in group.items"
            :key="item.key"
            type="button"
            class="menu-item"
            :class="{ active: activeMenu === item.key }"
            :title="collapsed ? item.label : ''"
            @click="emit('navigate', item.key)"
          >
            <span class="menu-icon">
              <svg viewBox="0 0 24 24" fill="none"
                stroke="currentColor" stroke-width="1.8"
                stroke-linecap="round" stroke-linejoin="round">

                <template v-if="item.icon === 'dashboard'">
                  <rect x="3" y="3" width="8" height="8" rx="1.5" />
                  <rect x="13" y="3" width="8" height="5" rx="1.5" />
                  <rect x="13" y="10" width="8" height="11" rx="1.5" />
                  <rect x="3" y="13" width="8" height="8" rx="1.5" />
                </template>

                <template v-else-if="item.icon === 'projects'">
                  <path d="M3 7.5A1.5 1.5 0 0 1 4.5 6H10l2 2h7.5A1.5 1.5 0 0 1 21 9.5v9a1.5 1.5 0 0 1-1.5 1.5h-15A1.5 1.5 0 0 1 3 18.5z" />
                  <path d="M3 10h18" />
                </template>

                <template v-else-if="item.icon === 'cases'">
                  <rect x="4" y="3" width="16" height="18" rx="2" />
                  <path d="M8 8h8M8 12h8M8 16h5" />
                </template>

                <template v-else-if="item.icon === 'environment'">
                  <path d="M12 22s8-4 8-11V5l-8-3-8 3v6c0 7 8 11 8 11z" />
                  <path d="m9 12 2 2 4-4" />
                </template>

                <template v-else-if="item.icon === 'scenario'">
                  <circle cx="5" cy="5" r="2" />
                  <circle cx="19" cy="19" r="2" />
                  <circle cx="19" cy="5" r="2" />
                  <circle cx="5" cy="19" r="2" />
                  <path d="M7 5h8a4 4 0 0 1 4 4v8M5 7v8a4 4 0 0 0 4 4h8" />
                </template>

                <template v-else-if="item.icon === 'history'">
                  <path d="M3 12a9 9 0 1 0 2.6-6.4L3 8" />
                  <path d="M3 3v5h5M12 7v5l3 2" />
                </template>

                <template v-else-if="item.icon === 'report'">
                  <path d="M6 3h9l4 4v14H6a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z" />
                  <path d="M14 3v5h5M8 13h8M8 17h8" />
                </template>

                <template v-else-if="item.icon === 'trend'">
                  <path d="M3 3v18h18" />
                  <path d="m7 14 4-4 4 3 6-7" />
                  <path d="M16 6h5v5" />
                </template>
              </svg>
            </span>

            <span v-if="!collapsed" class="menu-label">
              {{ item.label }}
            </span>
          </button>
        </div>
      </div>

      <div class="sidebar-footer">
        <div class="footer-dot"></div>
        <span v-if="!collapsed">系统运行正常</span>
      </div>
    </aside>

    <button
      class="sidebar-toggle"
      type="button"
      :title="collapsed ? '展开导航' : '收起导航'"
      @click="toggleSidebar"
    >
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <path
          v-if="collapsed"
          d="m9 5 7 7-7 7"
        />
        <path
          v-else
          d="m15 5-7 7 7 7"
        />
      </svg>
    </button>
  </div>
</template>

<style scoped>
.sidebar-shell {
  position: relative;
  flex: 0 0 256px;
  width: 256px;
  height: 100%;
  min-height: 0;
  display: flex;
  overflow: visible;
  transition: width 0.25s ease, flex-basis 0.25s ease;
  z-index: 30;
}

.sidebar-shell.collapsed {
  flex-basis: 96px;
  width: 96px;
}

.sidebar {
  width: 232px;
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: #192a40;
  color: #d5deea;
  transition: width 0.25s ease;
}

.collapsed .sidebar {
  width: 72px;
}

.sidebar-brand {
  height: 76px;
  min-height: 76px;
  padding: 0 20px;
  display: flex;
  align-items: center;
  gap: 12px;
  overflow: hidden;
}

.collapsed .sidebar-brand {
  padding: 0;
  justify-content: center;
}

.brand-logo {
  width: 36px;
  height: 36px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  background: #165dff;
  border-radius: 10px;
}

.brand-logo svg {
  width: 23px;
  height: 23px;
}

.brand-text {
  min-width: 0;
  white-space: nowrap;
}

.brand-name {
  color: #fff;
  font-size: 16px;
  font-weight: 650;
  letter-spacing: 0.2px;
}

.brand-subtitle {
  margin-top: 4px;
  color: #91a2b8;
  font-size: 11px;
}

.sidebar-content {
  flex: 1;
  min-height: 0;
  padding: 8px 10px;
  overflow-x: hidden;
  overflow-y: auto;
}

.collapsed .sidebar-content {
  padding: 8px 8px;
}

.menu-group {
  margin-bottom: 20px;
}

.group-title {
  padding: 8px 10px;
  color: #8293aa;
  font-size: 12px;
  white-space: nowrap;
}

.menu-item {
  position: relative;
  width: 100%;
  height: 44px;
  margin: 3px 0;
  padding: 0 12px;
  display: flex;
  align-items: center;
  gap: 13px;
  border: none;
  border-radius: 7px;
  outline: none;
  background: transparent;
  color: #b9c6d6;
  text-align: left;
  cursor: pointer;
  transition: background 0.18s ease, color 0.18s ease;
}

.menu-item:hover {
  background: rgba(255, 255, 255, 0.08);
  color: #fff;
}

.menu-item.active {
  background: #25466f;
  color: #fff;
}

.menu-item.active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 9px;
  bottom: 9px;
  width: 3px;
  border-radius: 0 3px 3px 0;
  background: #4080ff;
}

.collapsed .menu-item {
  width: 56px;
  height: 46px;
  margin: 4px 0;
  padding: 0;
  justify-content: center;
  gap: 0;
}

.collapsed .menu-item.active::before {
  left: 0;
}

.menu-icon {
  width: 24px;
  height: 24px;
  flex: 0 0 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: inherit;
}

.menu-icon svg {
  display: block;
  width: 21px;
  height: 21px;
  flex-shrink: 0;
}

.menu-label {
  font-size: 14px;
  white-space: nowrap;
}

.sidebar-footer {
  height: 52px;
  min-height: 52px;
  padding: 0 20px;
  display: flex;
  align-items: center;
  gap: 9px;
  color: #91a2b8;
  font-size: 12px;
  white-space: nowrap;
  border-top: 1px solid rgba(255, 255, 255, 0.07);
}

.collapsed .sidebar-footer {
  padding: 0;
  justify-content: center;
}

.footer-dot {
  width: 7px;
  height: 7px;
  flex-shrink: 0;
  border-radius: 50%;
  background: #00b42a;
  box-shadow: 0 0 7px rgba(0, 180, 42, 0.5);
}

/* 导航栏外侧的半圆角方块按钮 */
.sidebar-toggle {
  position: absolute;
  top: 50%;
  right: 0;
  transform: translateY(-50%);
  z-index: 100;
  width: 24px;
  height: 64px;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: 0 10px 10px 0;
  outline: none;
  background: rgba(148, 163, 184, 0.28);
  color: #475569;
  cursor: pointer;
  transition: background 0.2s ease, color 0.2s ease;
}

.sidebar-toggle:hover {
  background: rgba(148, 163, 184, 0.48);
  color: #165dff;
}

.sidebar-toggle svg {
  display: block;
  width: 19px;
  height: 19px;
  fill: none;
  stroke: currentColor;
  stroke-width: 2.5;
  stroke-linecap: round;
  stroke-linejoin: round;
}

@media (max-width: 700px) {
  .sidebar-shell {
    flex-basis: 96px;
    width: 96px;
  }

  .sidebar {
    width: 72px;
  }

  .sidebar-brand {
    padding: 0;
    justify-content: center;
  }

  .brand-text,
  .group-title,
  .menu-label,
  .sidebar-footer span {
    display: none;
  }

  .sidebar-content {
    padding: 8px;
  }

  .menu-item {
    width: 56px;
    justify-content: center;
    padding: 0;
  }
}
</style>
