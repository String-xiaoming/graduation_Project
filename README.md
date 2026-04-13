# 学生信息管理系统 / Student Information Management System

## 项目简介 / Project Overview

本项目是一个基于 Node.js + Express 的学生信息管理系统，实现了对学生基本信息的增删改查功能。

This is a web-based Student Information Management System built with Node.js and Express, supporting full CRUD operations on student records.

## 功能特性 / Features

- 📋 学生信息列表展示 / Student list with pagination
- ➕ 新增学生信息 / Add new student
- ✏️ 编辑学生信息 / Edit student information
- 🗑️ 删除学生记录 / Delete student record
- 🔍 按姓名/学号搜索 / Search by name or student ID
- 📊 统计信息展示 / Statistics dashboard

## 技术栈 / Tech Stack

- **后端 / Backend**: Node.js, Express.js
- **前端 / Frontend**: HTML5, CSS3, JavaScript (Vanilla)
- **数据存储 / Storage**: JSON file (lightweight, no DB setup)

## 快速开始 / Getting Started

### 环境要求 / Prerequisites

- Node.js >= 14.x

### 安装与运行 / Installation & Run

```bash
# 安装依赖 / Install dependencies
npm install

# 启动服务 / Start server
npm start

# 访问地址 / Open in browser
# http://localhost:3000
```

## 项目结构 / Project Structure

```
graduation_Project/
├── server.js          # Express 后端服务
├── package.json       # 项目配置
├── data/
│   └── students.json  # 学生数据文件
└── public/
    ├── index.html     # 前端页面
    ├── css/
    │   └── style.css  # 样式文件
    └── js/
        └── app.js     # 前端逻辑
```

## API 接口 / API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/students` | 获取所有学生 / Get all students |
| GET | `/api/students/:id` | 获取单个学生 / Get student by ID |
| POST | `/api/students` | 新增学生 / Add student |
| PUT | `/api/students/:id` | 更新学生 / Update student |
| DELETE | `/api/students/:id` | 删除学生 / Delete student |

## 作者 / Author

String-xiaoming
