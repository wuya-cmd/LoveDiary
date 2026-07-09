# LoveDiary 项目审查与清理计划

## 项目功能总结

**LoveDiary（朝暮）** 是一款情侣日记 Android 应用，使用 Kotlin + Jetpack Compose + Room 开发。

### 核心功能
1. **日记管理** - 添加/编辑/删除日记，支持文字内容和图片附件
2. **隐私保护** - 公开/私密两种级别，密码验证（默认520520），5分钟自动锁定
3. **数据备份** - JSON导出/导入，小程序备份导入
4. **灵犀同步** - WiFi点对点同步，情侣间日记共享
5. **纪念日** - 设置相识日期，显示在一起天数
6. **图片功能** - 压缩存储，全屏查看支持缩放

---

## 发现的问题

### 严重 Bug

#### 1. 数据库文件名不一致（高优先级）
- **位置**: `DiaryDatabase.kt` 第41行 vs `MainActivity.kt` 第55行
- **问题**: Database 定义 `love_diary_database`，但 MainActivity 创建 `diary_database`
- **影响**: 可能导致数据丢失或读取不到已有数据
- **修复**: 统一为 `love_diary_database`

### 代码质量问题

#### 2. 阻塞式方法不应使用（中优先级）
- **位置**: `DiaryRepository.kt` 第126-161行
- **问题**: `getAllDiariesRawNonSuspend()`、`getImagesByDiaryIdNonSuspend()`、`addDiaryNonSuspend()`、`addImageToDiaryNonSuspend()` 使用 `runBlocking`
- **影响**: 可能阻塞UI线程，导致ANR
- **修复**: 检查是否被使用，若被 `LinxiSyncManager` 使用则改为协程方式

#### 3. AGP 废弃 API（低优先级）
- **位置**: `build.gradle.kts` 第69-72行
- **问题**: `dexOptions` 在 AGP 8.x 已废弃
- **修复**: 删除该配置块

### 无效代码

#### 4. 注释掉的代码块
| 文件 | 行号 | 内容 |
|------|------|------|
| HomeScreen.kt | 100-103 | 注释的分类/标签状态 |
| HomeScreen.kt | 244-250 | 注释的导入小程序备份按钮 |
| HomeScreen.kt | 368-393 | 注释的图标代码 |
| AddDiaryScreen.kt | 517-522 | 注释的图标代码 |
| EditDiaryScreen.kt | 668-672 | 注释的图标代码 |

**修复**: 删除这些注释代码

#### 5. 功能不完整的 DisplayScreen
- **位置**: `DisplayScreen.kt`
- **问题**: 该屏幕只有简单的图片选择和文字输入，没有实际功能
- **建议**: 保留（可能是"最近心情"功能的占位，待后续开发）

---

## 修改计划

### 第一步：修复严重 Bug

1. **统一数据库文件名**
   - 文件：`MainActivity.kt`
   - 修改：第55行 `"diary_database"` → `"love_diary_database"`

### 第二步：清理无效代码

2. **删除注释代码**
   - `HomeScreen.kt`: 删除第100-103行、第244-250行、第368-393行
   - `AddDiaryScreen.kt`: 删除第517-522行
   - `EditDiaryScreen.kt`: 删除第668-672行

### 第三步：修复代码质量问题

3. **删除废弃的 dexOptions**
   - 文件：`app/build.gradle.kts`
   - 删除：第69-72行整个 `dexOptions` 块

4. **处理阻塞式方法**
   - 检查 `LinxiSyncManager.kt` 是否使用了这些非挂起方法
   - 若使用，需重构为协程方式（这是一个较大的改动，需要谨慎）
   - 本次暂不修改，仅标记为待优化项

### 第四步：Git 提交

5. **提交清理后的代码**
   - 提交信息：`refactor: 清理无效代码，修复数据库文件名不一致问题`

---

## 修改清单

| 序号 | 文件 | 修改内容 | 风险等级 |
|------|------|----------|----------|
| 1 | MainActivity.kt | 统一数据库文件名 | 高（需测试数据迁移） |
| 2 | HomeScreen.kt | 删除注释代码 | 低 |
| 3 | AddDiaryScreen.kt | 删除注释代码 | 低 |
| 4 | EditDiaryScreen.kt | 删除注释代码 | 低 |
| 5 | build.gradle.kts | 删除废弃 dexOptions | 低 |

---

## 注意事项

1. **数据库文件名修改风险**: 修改后，已安装应用会创建新数据库文件。如果用户已有数据，需要迁移。但由于当前数据库版本没有迁移策略，且使用了 `fallbackToDestructiveMigration()`，修改数据库名称可能导致数据丢失。建议在发布时做好数据备份提示。

2. **阻塞式方法**: `LinxiSyncManager` 使用了这些方法，但它本身在后台线程（`thread`）中运行，暂时不会导致ANR。但这是不推荐的实践，建议后续重构。

---

## 验证步骤

1. 编译通过：`./gradlew assembleDebug`
2. 运行应用，测试主要功能：
   - 添加日记
   - 编辑日记
   - 删除日记
   - 图片上传
   - 隐私验证
   - 数据导出/导入
3. 确认无运行时崩溃

---

## 待优化项（本次不处理）

1. `LinxiSyncManager.kt`: 使用 `thread` 而非协程，应重构
2. `DisplayScreen.kt`: 功能不完整，待后续开发
3. `DiaryRepository.kt`: 阻塞式方法应重构为协程
4. 日记分类和标签功能：UI未完全暴露，待后续开发