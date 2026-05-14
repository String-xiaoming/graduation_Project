<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import {
  createAdminUser,
  deleteAdminJob,
  deleteAdminUser,
  fetchAdminJobs,
  fetchAdminUsers,
  updateAdminJob,
  updateAdminUser,
} from '@/api/admin'

const activeTab = ref('users')
const loading = ref(false)
const message = ref('')
const errorMessage = ref('')
const editorOpen = ref(false)

const userPage = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0,
  list: [],
})

const userQuery = reactive({
  keyword: '',
  role: '',
  status: '',
})

const userForm = reactive({
  id: null,
  email: '',
  password: '',
  nickname: '',
  role: 'USER',
  status: 1,
  localCity: '',
  educationText: '',
  expectedPosition: '',
  expectedSalaryMin: '',
  expectedSalaryMax: '',
  skillInputText: '',
})

const jobPage = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0,
  list: [],
})

const jobQuery = reactive({
  keyword: '',
  city: '',
  status: '',
})

const jobForm = reactive({
  id: null,
  jobTitle: '',
  companyName: '',
  city: '',
  workAddress: '',
  educationText: '',
  experienceText: '',
  salaryText: '',
  salaryMin: '',
  salaryMax: '',
  jobDescription: '',
  status: 1,
})

const userTotalPages = computed(() => Math.max(1, Math.ceil(userPage.total / userPage.pageSize)))
const jobTotalPages = computed(() => Math.max(1, Math.ceil(jobPage.total / jobPage.pageSize)))
const isEditingUser = computed(() => Boolean(userForm.id))
const isEditingJob = computed(() => Boolean(jobForm.id))

function normalizeStatus(value) {
  if (value === '' || value === null || value === undefined) return ''
  return Number(value)
}

function showSuccess(text) {
  message.value = text
  errorMessage.value = ''
}

function showError(error) {
  errorMessage.value = error.message || '操作失败'
  message.value = ''
}

function resetUserForm() {
  Object.assign(userForm, {
    id: null,
    email: '',
    password: '',
    nickname: '',
    role: 'USER',
    status: 1,
    localCity: '',
    educationText: '',
    expectedPosition: '',
    expectedSalaryMin: '',
    expectedSalaryMax: '',
    skillInputText: '',
  })
}

function openCreateUser() {
  resetUserForm()
  editorOpen.value = true
}

function fillUserForm(user) {
  Object.assign(userForm, {
    id: user.id,
    email: user.email || '',
    password: '',
    nickname: user.nickname || '',
    role: user.role || 'USER',
    status: user.status ?? 1,
    localCity: user.localCity || '',
    educationText: user.educationText || '',
    expectedPosition: user.expectedPosition || '',
    expectedSalaryMin: user.expectedSalaryMin ?? '',
    expectedSalaryMax: user.expectedSalaryMax ?? '',
    skillInputText: user.skillInputText || '',
  })
  editorOpen.value = true
}

function resetJobForm() {
  Object.assign(jobForm, {
    id: null,
    jobTitle: '',
    companyName: '',
    city: '',
    workAddress: '',
    educationText: '',
    experienceText: '',
    salaryText: '',
    salaryMin: '',
    salaryMax: '',
    jobDescription: '',
    status: 1,
  })
}

function fillJobForm(job) {
  Object.assign(jobForm, {
    id: job.id,
    jobTitle: job.jobTitle || '',
    companyName: job.companyName || '',
    city: job.city || '',
    workAddress: job.workAddress || '',
    educationText: job.educationText || '',
    experienceText: job.experienceText || '',
    salaryText: job.salaryText || '',
    salaryMin: job.salaryMin ?? '',
    salaryMax: job.salaryMax ?? '',
    jobDescription: job.jobDescription || '',
    status: job.status ?? 1,
  })
  editorOpen.value = true
}

function closeEditor() {
  editorOpen.value = false
}

async function loadUsers(pageNum = userPage.pageNum) {
  loading.value = true
  try {
    const data = await fetchAdminUsers({
      pageNum,
      pageSize: userPage.pageSize,
      keyword: userQuery.keyword || undefined,
      role: userQuery.role || undefined,
      status: normalizeStatus(userQuery.status),
    })
    userPage.pageNum = data.pageNum
    userPage.pageSize = data.pageSize
    userPage.total = data.total
    userPage.list = data.list || []
  } catch (error) {
    showError(error)
  } finally {
    loading.value = false
  }
}

async function loadJobs(pageNum = jobPage.pageNum) {
  loading.value = true
  try {
    const data = await fetchAdminJobs({
      pageNum,
      pageSize: jobPage.pageSize,
      keyword: jobQuery.keyword || undefined,
      city: jobQuery.city || undefined,
      status: normalizeStatus(jobQuery.status),
    })
    jobPage.pageNum = data.pageNum
    jobPage.pageSize = data.pageSize
    jobPage.total = data.total
    jobPage.list = data.list || []
  } catch (error) {
    showError(error)
  } finally {
    loading.value = false
  }
}

async function submitUser() {
  if (!userForm.email || !userForm.nickname) {
    errorMessage.value = '请填写邮箱和昵称'
    return
  }

  if (!isEditingUser.value && !userForm.password) {
    errorMessage.value = '新增用户必须填写初始密码'
    return
  }

  const payload = {
    email: userForm.email,
    password: userForm.password || undefined,
    nickname: userForm.nickname,
    role: userForm.role,
    status: Number(userForm.status),
    localCity: userForm.localCity || null,
    educationText: userForm.educationText || null,
    expectedPosition: userForm.expectedPosition || null,
    expectedSalaryMin: userForm.expectedSalaryMin === '' ? null : Number(userForm.expectedSalaryMin),
    expectedSalaryMax: userForm.expectedSalaryMax === '' ? null : Number(userForm.expectedSalaryMax),
    skillInputText: userForm.skillInputText || null,
  }

  loading.value = true
  try {
    if (isEditingUser.value) {
      await updateAdminUser(userForm.id, payload)
      showSuccess('用户信息已更新')
    } else {
      await createAdminUser(payload)
      showSuccess('用户已新增')
    }
    resetUserForm()
    closeEditor()
    await loadUsers(1)
  } catch (error) {
    showError(error)
  } finally {
    loading.value = false
  }
}

async function removeUser(user) {
  if (!window.confirm(`确认删除或禁用用户：${user.nickname || user.email}？`)) return

  loading.value = true
  try {
    await deleteAdminUser(user.id)
    showSuccess('用户已处理')
    await loadUsers(userPage.pageNum)
  } catch (error) {
    showError(error)
  } finally {
    loading.value = false
  }
}

async function submitJob() {
  if (!jobForm.id) {
    errorMessage.value = '请先选择要编辑的岗位'
    return
  }

  const payload = {
    jobTitle: jobForm.jobTitle,
    companyName: jobForm.companyName,
    city: jobForm.city,
    workAddress: jobForm.workAddress,
    educationText: jobForm.educationText,
    experienceText: jobForm.experienceText,
    salaryText: jobForm.salaryText,
    salaryMin: jobForm.salaryMin === '' ? null : Number(jobForm.salaryMin),
    salaryMax: jobForm.salaryMax === '' ? null : Number(jobForm.salaryMax),
    jobDescription: jobForm.jobDescription,
    status: Number(jobForm.status),
  }

  loading.value = true
  try {
    await updateAdminJob(jobForm.id, payload)
    showSuccess('岗位信息已更新')
    resetJobForm()
    closeEditor()
    await loadJobs(jobPage.pageNum)
  } catch (error) {
    showError(error)
  } finally {
    loading.value = false
  }
}

async function removeJob(job) {
  if (!window.confirm(`确认下架或删除岗位：${job.jobTitle}？`)) return

  loading.value = true
  try {
    await deleteAdminJob(job.id)
    showSuccess('岗位已处理')
    await loadJobs(jobPage.pageNum)
  } catch (error) {
    showError(error)
  } finally {
    loading.value = false
  }
}

function switchTab(tab) {
  activeTab.value = tab
  message.value = ''
  errorMessage.value = ''
  closeEditor()
  if (tab === 'users') loadUsers(1)
  if (tab === 'jobs') loadJobs(1)
}

onMounted(() => {
  loadUsers()
})
</script>

<template>
  <section class="admin-page page-section">
    <div class="admin-hero">
      <div>
        <p class="eyebrow">ADMIN CENTER</p>
        <h1>管理后台</h1>
        <p>统一维护用户账号、角色状态和岗位数据，保证前台展示的数据稳定可用。</p>
      </div>
      <div class="admin-hero__badge">
        <span>当前模块</span>
        <strong>{{ activeTab === 'users' ? '用户管理' : '岗位管理' }}</strong>
      </div>
    </div>

    <div class="admin-summary-grid">
      <article>
        <span>当前模块</span>
        <strong>{{ activeTab === 'users' ? '用户管理' : '岗位管理' }}</strong>
        <small>集中维护账号、权限与岗位状态</small>
      </article>
      <article>
        <span>{{ activeTab === 'users' ? '用户总数' : '岗位总数' }}</span>
        <strong>{{ activeTab === 'users' ? userPage.total : jobPage.total }}</strong>
        <small>跟随当前筛选条件实时变化</small>
      </article>
      <article>
        <span>当前页</span>
        <strong>{{ activeTab === 'users' ? userPage.pageNum : jobPage.pageNum }}</strong>
        <small>{{ activeTab === 'users' ? userPage.pageSize : jobPage.pageSize }} 条 / 页</small>
      </article>
    </div>

    <div class="admin-tabs">
      <button type="button" :class="{ active: activeTab === 'users' }" @click="switchTab('users')">
        用户管理
      </button>
      <button type="button" :class="{ active: activeTab === 'jobs' }" @click="switchTab('jobs')">
        岗位管理
      </button>
    </div>

    <div v-if="message" class="form-success">{{ message }}</div>
    <div v-if="errorMessage" class="form-error">{{ errorMessage }}</div>

    <div v-if="activeTab === 'users'" class="admin-layout">
      <div class="admin-panel">
        <div class="admin-panel__head">
          <div>
            <p class="eyebrow">USER LIST</p>
            <h2>用户列表</h2>
          </div>
          <div class="admin-panel__tools">
            <span>共 {{ userPage.total }} 人</span>
            <button type="button" @click="openCreateUser">新增用户</button>
          </div>
        </div>

        <form class="admin-filter" @submit.prevent="loadUsers(1)">
          <input v-model.trim="userQuery.keyword" placeholder="搜索邮箱、昵称、城市" />
          <select v-model="userQuery.role">
            <option value="">全部角色</option>
            <option value="USER">普通用户</option>
            <option value="ADMIN">管理员</option>
          </select>
          <select v-model="userQuery.status">
            <option value="">全部状态</option>
            <option :value="1">启用</option>
            <option :value="0">禁用</option>
          </select>
          <button type="submit" :disabled="loading">查询</button>
        </form>

        <div class="admin-table-wrap">
          <table class="admin-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>邮箱</th>
                <th>昵称</th>
                <th>角色</th>
                <th>城市</th>
                <th>状态</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="user in userPage.list" :key="user.id">
                <td>{{ user.id }}</td>
                <td>{{ user.email }}</td>
                <td>{{ user.nickname }}</td>
                <td>{{ user.role === 'ADMIN' ? '管理员' : '普通用户' }}</td>
                <td>{{ user.localCity || '-' }}</td>
                <td>
                  <span class="status-pill" :class="{ off: user.status === 0 }">
                    {{ user.status === 0 ? '禁用' : '启用' }}
                  </span>
                </td>
                <td>
                  <button class="text-button" type="button" @click="fillUserForm(user)">编辑</button>
                  <button class="text-button danger-link" type="button" @click="removeUser(user)">删除</button>
                </td>
              </tr>
              <tr v-if="!loading && !userPage.list.length" class="admin-empty-row">
                <td colspan="7">
                  <strong>暂无用户数据</strong>
                  <span>调整筛选条件后重新查询，或在右侧新增用户。</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="pagination">
          <button :disabled="loading || userPage.pageNum <= 1" @click="loadUsers(userPage.pageNum - 1)">
            上一页
          </button>
          <span>{{ userPage.pageNum }} / {{ userTotalPages }}</span>
          <button :disabled="loading || userPage.pageNum >= userTotalPages" @click="loadUsers(userPage.pageNum + 1)">
            下一页
          </button>
        </div>
      </div>

      <button v-if="editorOpen" class="admin-drawer-backdrop" type="button" aria-label="关闭编辑栏" @click="closeEditor"></button>

      <form v-if="editorOpen" class="admin-editor admin-drawer" @submit.prevent="submitUser">
        <button class="admin-drawer-close" type="button" aria-label="关闭编辑栏" @click="closeEditor">x</button>
        <p class="eyebrow">{{ isEditingUser ? 'EDIT USER' : 'CREATE USER' }}</p>
        <h2>{{ isEditingUser ? '编辑用户' : '新增用户' }}</h2>
        <label>邮箱<input v-model.trim="userForm.email" type="email" placeholder="user@qq.com" /></label>
        <label>
          {{ isEditingUser ? '新密码（不填则不修改）' : '初始密码' }}
          <input v-model="userForm.password" type="password" placeholder="请输入密码" />
        </label>
        <label>昵称<input v-model.trim="userForm.nickname" placeholder="用户昵称" /></label>
        <div class="admin-form-row">
          <label>
            角色
            <select v-model="userForm.role">
              <option value="USER">普通用户</option>
              <option value="ADMIN">管理员</option>
            </select>
          </label>
          <label>
            状态
            <select v-model="userForm.status">
              <option :value="1">启用</option>
              <option :value="0">禁用</option>
            </select>
          </label>
        </div>
        <label>所在城市<input v-model.trim="userForm.localCity" placeholder="如：贵阳" /></label>
        <label>学历<input v-model.trim="userForm.educationText" placeholder="如：本科" /></label>
        <label>期望岗位<input v-model.trim="userForm.expectedPosition" placeholder="如：Java开发" /></label>
        <div class="admin-form-row">
          <label>最低薪资<input v-model="userForm.expectedSalaryMin" type="number" min="0" /></label>
          <label>最高薪资<input v-model="userForm.expectedSalaryMax" type="number" min="0" /></label>
        </div>
        <label>技能描述<textarea v-model.trim="userForm.skillInputText" rows="3" placeholder="会 Java、Python、Vue"></textarea></label>
        <div class="admin-actions">
          <button type="submit" :disabled="loading">{{ isEditingUser ? '保存修改' : '新增用户' }}</button>
          <button class="ghost-button" type="button" @click="resetUserForm">清空</button>
        </div>
      </form>
    </div>

    <div v-else class="admin-layout">
      <div class="admin-panel">
        <div class="admin-panel__head">
          <div>
            <p class="eyebrow">JOB LIST</p>
            <h2>岗位列表</h2>
          </div>
          <span>共 {{ jobPage.total }} 条</span>
        </div>

        <form class="admin-filter" @submit.prevent="loadJobs(1)">
          <input v-model.trim="jobQuery.keyword" placeholder="搜索岗位、公司、地址" />
          <input v-model.trim="jobQuery.city" placeholder="城市" />
          <select v-model="jobQuery.status">
            <option value="">全部状态</option>
            <option :value="1">展示中</option>
            <option :value="0">已下架</option>
          </select>
          <button type="submit" :disabled="loading">查询</button>
        </form>

        <div class="admin-table-wrap">
          <table class="admin-table admin-table--jobs">
            <thead>
              <tr>
                <th>ID</th>
                <th>岗位</th>
                <th>公司</th>
                <th>城市</th>
                <th>薪资</th>
                <th>状态</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="job in jobPage.list" :key="job.id">
                <td>{{ job.id }}</td>
                <td>{{ job.jobTitle }}</td>
                <td>{{ job.companyName }}</td>
                <td>{{ job.city }}</td>
                <td>{{ job.salaryText || '-' }}</td>
                <td>
                  <span class="status-pill" :class="{ off: job.status === 0 }">
                    {{ job.status === 0 ? '下架' : '展示' }}
                  </span>
                </td>
                <td>
                  <button class="text-button" type="button" @click="fillJobForm(job)">编辑</button>
                  <button class="text-button danger-link" type="button" @click="removeJob(job)">删除</button>
                </td>
              </tr>
              <tr v-if="!loading && !jobPage.list.length" class="admin-empty-row">
                <td colspan="7">
                  <strong>暂无岗位数据</strong>
                  <span>调整岗位、公司、城市或状态条件后重新查询。</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="pagination">
          <button :disabled="loading || jobPage.pageNum <= 1" @click="loadJobs(jobPage.pageNum - 1)">
            上一页
          </button>
          <span>{{ jobPage.pageNum }} / {{ jobTotalPages }}</span>
          <button :disabled="loading || jobPage.pageNum >= jobTotalPages" @click="loadJobs(jobPage.pageNum + 1)">
            下一页
          </button>
        </div>
      </div>

      <button v-if="editorOpen" class="admin-drawer-backdrop" type="button" aria-label="关闭编辑栏" @click="closeEditor"></button>

      <form v-if="editorOpen" class="admin-editor admin-drawer" @submit.prevent="submitJob">
        <button class="admin-drawer-close" type="button" aria-label="关闭编辑栏" @click="closeEditor">x</button>
        <p class="eyebrow">EDIT JOB</p>
        <h2>{{ isEditingJob ? '编辑岗位' : '选择岗位后编辑' }}</h2>
        <label>岗位名称<input v-model.trim="jobForm.jobTitle" placeholder="岗位名称" /></label>
        <label>公司名称<input v-model.trim="jobForm.companyName" placeholder="公司名称" /></label>
        <div class="admin-form-row">
          <label>城市<input v-model.trim="jobForm.city" /></label>
          <label>状态
            <select v-model="jobForm.status">
              <option :value="1">展示</option>
              <option :value="0">下架</option>
            </select>
          </label>
        </div>
        <label>工作地址<input v-model.trim="jobForm.workAddress" /></label>
        <div class="admin-form-row">
          <label>学历<input v-model.trim="jobForm.educationText" /></label>
          <label>经验<input v-model.trim="jobForm.experienceText" /></label>
        </div>
        <label>薪资文本<input v-model.trim="jobForm.salaryText" /></label>
        <div class="admin-form-row">
          <label>最低薪资<input v-model="jobForm.salaryMin" type="number" min="0" /></label>
          <label>最高薪资<input v-model="jobForm.salaryMax" type="number" min="0" /></label>
        </div>
        <label>岗位描述<textarea v-model.trim="jobForm.jobDescription" rows="7"></textarea></label>
        <div class="admin-actions">
          <button type="submit" :disabled="loading || !isEditingJob">保存岗位</button>
          <button class="ghost-button" type="button" @click="resetJobForm">清空</button>
        </div>
      </form>
    </div>
  </section>
</template>
