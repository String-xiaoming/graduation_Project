const express = require('express');
const fs = require('fs');
const path = require('path');

const app = express();
const PORT = process.env.PORT || 3000;
const DATA_FILE = path.join(__dirname, 'data', 'students.json');

app.use(express.json());
app.use(express.static(path.join(__dirname, 'public')));

// Helper: generate a unique ID
function generateId() {
  return `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
}

// Helper: read students from file
function readStudents() {
  if (!fs.existsSync(DATA_FILE)) {
    return [];
  }
  try {
    const raw = fs.readFileSync(DATA_FILE, 'utf-8');
    return JSON.parse(raw);
  } catch {
    return [];
  }
}

// Helper: write students to file
function writeStudents(students) {
  fs.writeFileSync(DATA_FILE, JSON.stringify(students, null, 2), 'utf-8');
}

// GET /api/students - list all students (supports ?q= search)
app.get('/api/students', (req, res) => {
  const students = readStudents();
  const q = req.query.q ? req.query.q.toLowerCase() : '';
  const result = q
    ? students.filter(
        (s) =>
          s.name.toLowerCase().includes(q) ||
          s.studentId.toLowerCase().includes(q)
      )
    : students;
  res.json(result);
});

// GET /api/students/:id - get single student
app.get('/api/students/:id', (req, res) => {
  const students = readStudents();
  const student = students.find((s) => s.id === req.params.id);
  if (!student) {
    return res.status(404).json({ error: '学生不存在 / Student not found' });
  }
  res.json(student);
});

// POST /api/students - add student
app.post('/api/students', (req, res) => {
  const { name, studentId, gender, major, grade, email, phone } = req.body;
  if (!name || !studentId) {
    return res.status(400).json({ error: '姓名和学号为必填项 / Name and Student ID are required' });
  }
  const students = readStudents();
  if (students.find((s) => s.studentId === studentId)) {
    return res.status(409).json({ error: '学号已存在 / Student ID already exists' });
  }
  const newStudent = {
    id: generateId(),
    name,
    studentId,
    gender: gender || '',
    major: major || '',
    grade: grade || '',
    email: email || '',
    phone: phone || '',
    createdAt: new Date().toISOString()
  };
  students.push(newStudent);
  writeStudents(students);
  res.status(201).json(newStudent);
});

// PUT /api/students/:id - update student
app.put('/api/students/:id', (req, res) => {
  const students = readStudents();
  const index = students.findIndex((s) => s.id === req.params.id);
  if (index === -1) {
    return res.status(404).json({ error: '学生不存在 / Student not found' });
  }
  const { name, studentId, gender, major, grade, email, phone } = req.body;
  if (!name || !studentId) {
    return res.status(400).json({ error: '姓名和学号为必填项 / Name and Student ID are required' });
  }
  // Check duplicate studentId (allow same student to keep their own id)
  const duplicate = students.find(
    (s) => s.studentId === studentId && s.id !== req.params.id
  );
  if (duplicate) {
    return res.status(409).json({ error: '学号已存在 / Student ID already exists' });
  }
  students[index] = {
    ...students[index],
    name,
    studentId,
    gender: gender || '',
    major: major || '',
    grade: grade || '',
    email: email || '',
    phone: phone || '',
    updatedAt: new Date().toISOString()
  };
  writeStudents(students);
  res.json(students[index]);
});

// DELETE /api/students/:id - delete student
app.delete('/api/students/:id', (req, res) => {
  const students = readStudents();
  const index = students.findIndex((s) => s.id === req.params.id);
  if (index === -1) {
    return res.status(404).json({ error: '学生不存在 / Student not found' });
  }
  students.splice(index, 1);
  writeStudents(students);
  res.json({ message: '删除成功 / Deleted successfully' });
});

app.listen(PORT, () => {
  console.log(`服务已启动 / Server running at http://localhost:${PORT}`);
});

module.exports = app;
