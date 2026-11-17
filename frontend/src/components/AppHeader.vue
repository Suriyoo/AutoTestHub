
<script setup lang="ts">
defineProps<{
  username: string
  projectName?: string
}>()

const emit = defineEmits<{
  logout: []
  backHome: []
}>()
</script>

<template>
  <header class="app-header">
    <div class="header-left">
      <div class="breadcrumb">
        <span class="breadcrumb-home" @click="emit('backHome')">
          工作空间
        </span>

        <icon-right class="breadcrumb-arrow" />

        <span class="breadcrumb-current">
          {{ projectName || '工作台' }}
        </span>
      </div>
    </div>

    <div class="header-right">
      <a-dropdown trigger="hover">
        <div class="user-info">
          <a-avatar :size="34" class="user-avatar">
            {{ username ? username.charAt(0).toUpperCase() : 'U' }}
          </a-avatar>

          <span class="username">{{ username }}</span>
          <icon-down class="down-icon" />
        </div>

        <template #content>
          <a-doption @click="emit('backHome')">
            <icon-home />
            返回工作台
          </a-doption>

          <a-doption @click="emit('logout')">
            <icon-export />
            退出登录
          </a-doption>
        </template>
      </a-dropdown>
    </div>
  </header>
</template>

<style scoped>
.app-header {
  flex-shrink: 0;
  height: 64px;
  padding: 0 28px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #ffffff;
  border-bottom: 1px solid #e5e6eb;
  box-sizing: border-box;
}

.header-left,
.header-right {
  display: flex;
  align-items: center;
  min-width: 0;
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  white-space: nowrap;
}

.breadcrumb-home {
  color: #86909c;
  cursor: pointer;
}

.breadcrumb-home:hover {
  color: #165dff;
}

.breadcrumb-arrow {
  color: #c9cdd4;
  font-size: 12px;
}

.breadcrumb-current {
  overflow: hidden;
  max-width: 320px;
  color: #1d2129;
  font-weight: 600;
  text-overflow: ellipsis;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 9px;
  cursor: pointer;
}

.user-avatar {
  flex-shrink: 0;
  color: #ffffff;
  background: #165dff;
  font-weight: 600;
}

.username {
  color: #1d2129;
  font-size: 13px;
}

.down-icon {
  color: #86909c;
  font-size: 12px;
}

@media (max-width: 600px) {
  .app-header {
    padding: 0 14px;
  }

  .username {
    display: none;
  }
}
</style>
