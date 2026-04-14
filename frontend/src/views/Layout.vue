<template>
  <el-container class="layout" direction="vertical">
    <!-- 一级：左三 + 右退出（与线框一致） -->
    <el-header class="top-bar" height="52px">
      <nav class="top-left" aria-label="主导航">
        <div
          class="nav-dropdown"
          @mouseenter="onBillNavEnter"
          @mouseleave="onBillNavLeave"
        >
          <button
            type="button"
            class="top-item"
            :class="{ active: isBillSection }"
            @click="goBill"
          >
            账单分析
          </button>
          <div
            v-show="megaOpen"
            class="mega-dropdown"
            role="navigation"
            aria-label="账单分析子菜单"
          >
            <div class="mega-panel">
              <div class="mega-row">
                <span class="mega-l2">数据分析：</span>
                <router-link
                  to="/analytics"
                  class="mega-l3"
                  active-class="mega-l3-active"
                  @click="onMegaLinkClick"
                >
                  分析看板
                </router-link>
                <router-link
                  to="/analytics/group"
                  class="mega-l3"
                  active-class="mega-l3-active"
                  @click="onMegaLinkClick"
                >
                  分组看板
                </router-link>
              </div>
              <div class="mega-row">
                <span class="mega-l2">数据修改：</span>
                <router-link
                  to="/import"
                  class="mega-l3"
                  active-class="mega-l3-active"
                  @click="onMegaLinkClick"
                >
                  导入账单
                </router-link>
                <router-link
                  to="/phones"
                  class="mega-l3"
                  active-class="mega-l3-active"
                  @click="onMegaLinkClick"
                >
                  绑定手机号
                </router-link>
              </div>
              <div v-if="isAdmin" class="mega-row">
                <span class="mega-l2">其他功能：</span>
                <router-link
                  to="/admin/users"
                  class="mega-l3"
                  active-class="mega-l3-active"
                  @click="onMegaLinkClick"
                >
                  用户与角色
                </router-link>
                <router-link
                  to="/admin/phone-binds"
                  class="mega-l3"
                  active-class="mega-l3-active"
                  @click="onMegaLinkClick"
                >
                  手机号审核
                </router-link>
              </div>
            </div>
          </div>
        </div>
        <router-link to="/memo" class="top-item" active-class="active">备忘录</router-link>
        <router-link to="/tools" class="top-item" active-class="active">其他工具</router-link>
      </nav>
      <div class="top-right">
        <span class="user">{{ user }}</span>
        <el-button link type="danger" @click="logout">退出</el-button>
      </div>
    </el-header>

    <el-main class="main-fill"><router-view /></el-main>
  </el-container>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../api'

const route = useRoute()
const router = useRouter()
const user = computed(() => localStorage.getItem('username') || '—')

/** 悬停展开；点击三级链接后关闭，且在指针离开本区域前不再因悬停弹出 */
const megaOpen = ref(false)
const megaSuppressHover = ref(false)

function onBillNavEnter() {
  if (!megaSuppressHover.value) {
    megaOpen.value = true
  }
}

function onBillNavLeave() {
  megaOpen.value = false
  megaSuppressHover.value = false
}

function onMegaLinkClick() {
  megaOpen.value = false
  megaSuppressHover.value = true
}

const isAdmin = computed(() => {
  try {
    const a = JSON.parse(localStorage.getItem('authorities') || '[]')
    return a.includes('PERM_USER_ADMIN')
  } catch {
    return false
  }
})

const billPaths = [
  '/analytics',
  '/analytics/group',
  '/import',
  '/phones',
  '/admin/users',
  '/admin/phone-binds'
]

const isBillSection = computed(() => {
  const p = route.path
  return billPaths.some((x) => p === x)
})

function goBill() {
  megaOpen.value = false
  megaSuppressHover.value = true
  router.push('/analytics')
}

function logout() {
  localStorage.removeItem('token')
  localStorage.removeItem('username')
  localStorage.removeItem('authorities')
  localStorage.removeItem('phones')
  router.push('/login')
}

function sessionIsAdmin() {
  try {
    const a = JSON.parse(localStorage.getItem('authorities') || '[]')
    return Array.isArray(a) && a.includes('PERM_USER_ADMIN')
  } catch {
    return false
  }
}

onMounted(async () => {
  if (!localStorage.getItem('token')) return
  try {
    if (sessionIsAdmin()) {
      const { data } = await api.get('/me/bill-phones')
      const list = (data || []).map((p) => p.mobileCn).filter(Boolean)
      localStorage.setItem('phones', JSON.stringify(list))
    } else {
      const { data } = await api.get('/me/phones')
      localStorage.setItem('phones', JSON.stringify(data || []))
    }
  } catch {
    /* ignore */
  }
})
</script>

<style scoped>
.layout {
  height: 100%;
  min-height: 100vh;
  overflow: visible;
}

.top-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px 0 20px;
  border-bottom: 1px solid #dcdfe6;
  flex-shrink: 0;
  background: #fff;
  overflow: visible;
}

.top-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.nav-dropdown {
  position: relative;
}

.mega-dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  z-index: 2000;
  padding-top: 4px;
}

.mega-dropdown .mega-panel {
  min-width: 420px;
  max-width: min(96vw, 720px);
  padding: 12px 18px 14px;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
}

.top-item {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 36px;
  padding: 0 18px;
  font-size: 15px;
  color: #303133;
  text-decoration: none;
  border: 1px solid transparent;
  border-radius: 4px;
  background: transparent;
  cursor: pointer;
  transition:
    color 0.15s,
    background 0.15s,
    border-color 0.15s;
}
.top-item:hover {
  color: #409eff;
  background: #ecf5ff;
}
.top-item.active {
  color: #409eff;
  font-weight: 600;
  border-color: #b3d8ff;
  background: #ecf5ff;
}

button.top-item.active {
  border-color: #b3d8ff;
}

.top-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.user {
  color: #606266;
  font-size: 14px;
}

.mega-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 16px;
  margin-bottom: 10px;
}
.mega-row:last-child {
  margin-bottom: 0;
}

.mega-l2 {
  font-size: 14px;
  color: #606266;
  user-select: none;
  pointer-events: none;
  white-space: nowrap;
  min-width: 5.5em;
}

.mega-l3 {
  font-size: 14px;
  color: #409eff;
  text-decoration: none;
  padding: 4px 12px;
  border-radius: 4px;
  border: 1px solid transparent;
  transition:
    background 0.15s,
    border-color 0.15s;
}
.mega-l3:hover {
  background: #ecf5ff;
  border-color: #d9ecff;
}
.mega-l3-active {
  color: #fff !important;
  background: #409eff !important;
  border-color: #409eff !important;
  font-weight: 600;
}

.main-fill {
  padding: 12px 16px;
  box-sizing: border-box;
}
</style>
