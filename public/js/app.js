const API = '/api/students';

let deleteTargetId = null;
let allStudents = [];

// =================== Fetch & Render ===================

async function fetchStudents(query = '') {
  const url = query ? `${API}?q=${encodeURIComponent(query)}` : API;
  try {
    const res = await fetch(url);
    const data = await res.json();
    allStudents = query ? allStudents : data; // keep full list for stats
    renderTable(data);
    if (!query) updateStats(data);
  } catch {
    renderError('加载失败，请刷新页面 / Failed to load data');
  }
}

function renderTable(students) {
  const tbody = document.getElementById('tableBody');
  if (students.length === 0) {
    tbody.innerHTML = '<tr><td colspan="9" class="empty-row">暂无数据 / No records found</td></tr>';
    return;
  }
  tbody.innerHTML = students
    .map(
      (s, i) => `
    <tr>
      <td>${i + 1}</td>
      <td><strong>${escHtml(s.studentId)}</strong></td>
      <td>${escHtml(s.name)}</td>
      <td>${genderBadge(s.gender)}</td>
      <td>${escHtml(s.major)}</td>
      <td>${escHtml(s.grade)}</td>
      <td>${escHtml(s.email)}</td>
      <td>${escHtml(s.phone)}</td>
      <td>
        <div class="actions">
          <button class="btn btn-primary btn-sm js-edit" data-id="${escHtml(s.id)}">编辑</button>
          <button class="btn btn-danger btn-sm js-delete" data-id="${escHtml(s.id)}" data-name="${escHtml(s.name)}">删除</button>
        </div>
      </td>
    </tr>`
    )
    .join('');
}

function renderError(msg) {
  document.getElementById('tableBody').innerHTML =
    `<tr><td colspan="9" class="empty-row">${msg}</td></tr>`;
}

function updateStats(students) {
  document.getElementById('totalCount').textContent = students.length;
  document.getElementById('maleCount').textContent = students.filter((s) => s.gender === '男').length;
  document.getElementById('femaleCount').textContent = students.filter((s) => s.gender === '女').length;
  const majors = new Set(students.map((s) => s.major).filter(Boolean));
  document.getElementById('majorCount').textContent = majors.size;
}

// =================== Helpers ===================

function escHtml(str) {
  if (!str) return '';
  return str
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

function genderBadge(gender) {
  if (gender === '男') return '<span class="gender-badge gender-male">男</span>';
  if (gender === '女') return '<span class="gender-badge gender-female">女</span>';
  return '<span style="color:#aaa">-</span>';
}

// =================== Add / Edit Modal ===================

function openAdd() {
  document.getElementById('modalTitle').textContent = '新增学生';
  document.getElementById('studentForm').reset();
  document.getElementById('studentDbId').value = '';
  document.getElementById('formError').textContent = '';
  showModal('modal');
}

async function openEdit(id) {
  try {
    const res = await fetch(`${API}/${id}`);
    if (!res.ok) throw new Error();
    const s = await res.json();
    document.getElementById('modalTitle').textContent = '编辑学生';
    document.getElementById('studentDbId').value = s.id;
    document.getElementById('inputName').value = s.name || '';
    document.getElementById('inputStudentId').value = s.studentId || '';
    document.getElementById('inputGender').value = s.gender || '';
    document.getElementById('inputGrade').value = s.grade || '';
    document.getElementById('inputMajor').value = s.major || '';
    document.getElementById('inputEmail').value = s.email || '';
    document.getElementById('inputPhone').value = s.phone || '';
    document.getElementById('formError').textContent = '';
    showModal('modal');
  } catch {
    alert('获取学生信息失败 / Failed to fetch student info');
  }
}

async function handleFormSubmit(e) {
  e.preventDefault();
  const id = document.getElementById('studentDbId').value;
  const body = {
    name: document.getElementById('inputName').value.trim(),
    studentId: document.getElementById('inputStudentId').value.trim(),
    gender: document.getElementById('inputGender').value,
    grade: document.getElementById('inputGrade').value.trim(),
    major: document.getElementById('inputMajor').value.trim(),
    email: document.getElementById('inputEmail').value.trim(),
    phone: document.getElementById('inputPhone').value.trim()
  };

  if (!body.name || !body.studentId) {
    document.getElementById('formError').textContent = '姓名和学号为必填项';
    return;
  }

  const method = id ? 'PUT' : 'POST';
  const url = id ? `${API}/${id}` : API;

  try {
    const res = await fetch(url, {
      method,
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body)
    });
    const data = await res.json();
    if (!res.ok) {
      document.getElementById('formError').textContent = data.error || '保存失败';
      return;
    }
    hideModal('modal');
    fetchStudents();
  } catch {
    document.getElementById('formError').textContent = '网络错误，请重试';
  }
}

// =================== Delete Modal ===================

function openDelete(id, name) {
  deleteTargetId = id;
  document.getElementById('deleteMessage').textContent =
    `确定要删除学生「${name}」的信息吗？此操作不可撤销。`;
  showModal('deleteModal');
}

async function confirmDelete() {
  if (!deleteTargetId) return;
  try {
    const res = await fetch(`${API}/${deleteTargetId}`, { method: 'DELETE' });
    if (!res.ok) throw new Error();
    hideModal('deleteModal');
    fetchStudents();
  } catch {
    alert('删除失败，请重试 / Delete failed');
  } finally {
    deleteTargetId = null;
  }
}

// =================== Modal Control ===================

function showModal(id) {
  document.getElementById(id).classList.remove('hidden');
  document.body.style.overflow = 'hidden';
}

function hideModal(id) {
  document.getElementById(id).classList.add('hidden');
  document.body.style.overflow = '';
}

// =================== Event Listeners ===================

document.addEventListener('DOMContentLoaded', () => {
  fetchStudents();

  // Table action buttons (event delegation)
  document.getElementById('tableBody').addEventListener('click', (e) => {
    const editBtn = e.target.closest('.js-edit');
    const deleteBtn = e.target.closest('.js-delete');
    if (editBtn) openEdit(editBtn.dataset.id);
    if (deleteBtn) openDelete(deleteBtn.dataset.id, deleteBtn.dataset.name);
  });

  // Search
  document.getElementById('searchBtn').addEventListener('click', () => {
    fetchStudents(document.getElementById('searchInput').value.trim());
  });
  document.getElementById('clearBtn').addEventListener('click', () => {
    document.getElementById('searchInput').value = '';
    fetchStudents();
  });
  document.getElementById('searchInput').addEventListener('keydown', (e) => {
    if (e.key === 'Enter') {
      fetchStudents(document.getElementById('searchInput').value.trim());
    }
  });

  // Add button
  document.getElementById('addBtn').addEventListener('click', openAdd);

  // Form submit
  document.getElementById('studentForm').addEventListener('submit', handleFormSubmit);

  // Modal close buttons
  document.getElementById('modalClose').addEventListener('click', () => hideModal('modal'));
  document.getElementById('cancelBtn').addEventListener('click', () => hideModal('modal'));
  document.getElementById('modalOverlay').addEventListener('click', () => hideModal('modal'));

  document.getElementById('deleteClose').addEventListener('click', () => hideModal('deleteModal'));
  document.getElementById('deleteCancelBtn').addEventListener('click', () => hideModal('deleteModal'));
  document.getElementById('deleteOverlay').addEventListener('click', () => hideModal('deleteModal'));
  document.getElementById('deleteConfirmBtn').addEventListener('click', confirmDelete);

  // Keyboard: close on Escape
  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') {
      hideModal('modal');
      hideModal('deleteModal');
    }
  });
});
