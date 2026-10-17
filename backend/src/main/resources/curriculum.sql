-- =====================================================
-- 学生选课系统 数据库初始化脚本
-- 使用前先执行：CREATE DATABASE curriculum DEFAULT CHARACTER SET utf8mb4;
-- 所有初始密码均为 123456（已用 BCrypt 加密存储）
--
-- 数据模型（重要）：
--   course          课程：一门课只有一条记录、一个课程编号（cno 唯一）
--   teaching_class  教学班：一门课在某学期的一次具体开课（教师/班级/时间/容量）
--   sc              选课记录：学生 -> 教学班
-- 因此「每门课的课程编号只有一个」，但「每门课可有多个教学班、可被多个学生选择」。
--
-- 课程分三类：公共必修（分班课按班级自动分配、合班课全员自动分配）/ 专业类（按班级自动分配）/ 选修类（自由选课）
-- =====================================================

SET NAMES utf8mb4;

-- 班级表（class 为 MySQL 保留字，使用反引号）
DROP TABLE IF EXISTS `class`;
CREATE TABLE `class` (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '班级id',
    class_name  VARCHAR(64)  NOT NULL COMMENT '班级名称',
    grade       VARCHAR(16)  COMMENT '年级',
    college     VARCHAR(64)  COMMENT '学院',
    major       VARCHAR(64)  COMMENT '专业'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '班级表';

-- 学生表
DROP TABLE IF EXISTS `student`;
CREATE TABLE `student` (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '学生id',
    sno         VARCHAR(32)  NOT NULL COMMENT '学号',
    password    VARCHAR(100) NOT NULL COMMENT '密码(BCrypt)',
    name        VARCHAR(64)  NOT NULL COMMENT '姓名',
    gender      VARCHAR(8)   COMMENT '性别',
    class_id    BIGINT       COMMENT '班级id',
    college     VARCHAR(64)  COMMENT '学院',
    major       VARCHAR(64)  COMMENT '专业',
    phone       VARCHAR(32)  COMMENT '电话',
    UNIQUE KEY uk_sno (sno)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '学生表';

-- 教师表
DROP TABLE IF EXISTS `teacher`;
CREATE TABLE `teacher` (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '教师id',
    tno         VARCHAR(32)  NOT NULL COMMENT '工号',
    password    VARCHAR(100) NOT NULL COMMENT '密码(BCrypt)',
    name        VARCHAR(64)  NOT NULL COMMENT '姓名',
    gender      VARCHAR(8)   COMMENT '性别',
    title       VARCHAR(32)  COMMENT '职称',
    college     VARCHAR(64)  COMMENT '学院',
    phone       VARCHAR(32)  COMMENT '电话',
    UNIQUE KEY uk_tno (tno)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '教师表';

-- 学期表
DROP TABLE IF EXISTS `semester`;
CREATE TABLE `semester` (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '学期id',
    semester_name VARCHAR(64) NOT NULL COMMENT '学期名称',
    start_date    DATE COMMENT '开始日期',
    end_date      DATE COMMENT '结束日期',
    is_current    TINYINT NOT NULL DEFAULT 0 COMMENT '是否当前学期(1=是)'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '学期表';

-- 课程表（课程目录：一门课一个课程编号）
DROP TABLE IF EXISTS `course`;
CREATE TABLE `course` (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '课程id',
    cno         VARCHAR(32) NOT NULL COMMENT '课程编号（唯一，一门课只有一个编号）',
    cname       VARCHAR(64) NOT NULL COMMENT '课程名称',
    credit      INT COMMENT '学分',
    hours       INT COMMENT '学时',
    course_type VARCHAR(16) NOT NULL DEFAULT '选修类' COMMENT '课程类型：公共必修/专业类/选修类',
    category    VARCHAR(32) COMMENT '课程类别（选修类细分小类，如 国学经典/诗词文学 等）',
    UNIQUE KEY uk_cno (cno)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '课程表';

-- 教学班表（一门课可开设多个教学班，供多个学生选择）
DROP TABLE IF EXISTS `teaching_class`;
CREATE TABLE `teaching_class` (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '教学班id',
    course_id      BIGINT NOT NULL COMMENT '课程id（course.id）',
    tc_no          VARCHAR(32) COMMENT '教学班编号（如 PK0101）',
    teacher_id     BIGINT COMMENT '授课教师id',
    semester_id    BIGINT COMMENT '所属学期id',
    class_id       BIGINT COMMENT '绑定班级id（分班公共必修/专业类；合班课与选修类为 NULL）',
    status         TINYINT NOT NULL DEFAULT 1 COMMENT '教学班状态(1=开设中 0=已停开)',
    min_students   INT DEFAULT 25 COMMENT '最低开课人数（选修类低于此人数可停止开设）',
    capacity       INT COMMENT '容量',
    selected_count INT NOT NULL DEFAULT 0 COMMENT '已选人数',
    location       VARCHAR(64) COMMENT '上课地点',
    schedule       VARCHAR(128) COMMENT '上课时间',
    UNIQUE KEY uk_tc_no (tc_no),
    KEY idx_course (course_id),
    KEY idx_teacher (teacher_id),
    KEY idx_semester (semester_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '教学班表';

-- 选课表（学生选教学班）
DROP TABLE IF EXISTS `sc`;
CREATE TABLE `sc` (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '选课记录id',
    student_id        BIGINT NOT NULL COMMENT '学生id',
    teaching_class_id BIGINT NOT NULL COMMENT '教学班id（teaching_class.id）',
    select_time       DATETIME COMMENT '选课时间',
    score             INT COMMENT '成绩(0-100)',
    UNIQUE KEY uk_student_tc (student_id, teaching_class_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '选课表';

-- =====================================================
-- 初始数据
-- =====================================================

-- 学期（1=当前学期，2=下学期）
INSERT INTO `semester` (semester_name, start_date, end_date, is_current) VALUES
('2025-2026-1', '2025-09-01', '2026-01-15', 1),
('2025-2026-2', '2026-02-20', '2026-07-01', 0);

-- 班级
INSERT INTO `class` (class_name, grade, college, major) VALUES
('软件工程2401', '2024', '计算机学院', '软件工程'),
('软件工程2402', '2024', '计算机学院', '软件工程'),
('计算机2401', '2024', '计算机学院', '计算机科学与技术'),
('计算机2402', '2024', '计算机学院', '计算机科学与技术'),
('人工智能2401', '2024', '人工智能学院', '人工智能'),
('网络工程2401', '2024', '计算机学院', '网络工程');

-- 教师（密码均为 123456 的 BCrypt 哈希，共 17 名）
INSERT INTO `teacher` (tno, password, name, gender, title, college, phone) VALUES
('T001', '$2b$10$4Pf5IMcM2DpAih1wCZZRvODEhky8FnUE.oejToTMfm5iBfCDacJp2', '张伟', '男', '教授',   '数学学院',     '13800000001'),
('T002', '$2b$10$4Pf5IMcM2DpAih1wCZZRvODEhky8FnUE.oejToTMfm5iBfCDacJp2', '李娜', '女', '讲师',   '数学学院',     '13800000002'),
('T003', '$2b$10$4Pf5IMcM2DpAih1wCZZRvODEhky8FnUE.oejToTMfm5iBfCDacJp2', '王强', '男', '副教授', '数学学院',     '13800000003'),
('T004', '$2b$10$4Pf5IMcM2DpAih1wCZZRvODEhky8FnUE.oejToTMfm5iBfCDacJp2', '刘洋', '女', '教授',   '外国语学院',   '13800000004'),
('T005', '$2b$10$4Pf5IMcM2DpAih1wCZZRvODEhky8FnUE.oejToTMfm5iBfCDacJp2', '陈静', '女', '讲师',   '外国语学院',   '13800000005'),
('T006', '$2b$10$4Pf5IMcM2DpAih1wCZZRvODEhky8FnUE.oejToTMfm5iBfCDacJp2', '杨帆', '男', '副教授', '外国语学院',   '13800000006'),
('T007', '$2b$10$4Pf5IMcM2DpAih1wCZZRvODEhky8FnUE.oejToTMfm5iBfCDacJp2', '周敏', '女', '教授',   '体育学院',     '13800000007'),
('T008', '$2b$10$4Pf5IMcM2DpAih1wCZZRvODEhky8FnUE.oejToTMfm5iBfCDacJp2', '吴磊', '男', '讲师',   '马克思主义学院', '13800000008'),
('T009', '$2b$10$4Pf5IMcM2DpAih1wCZZRvODEhky8FnUE.oejToTMfm5iBfCDacJp2', '郑涛', '男', '教授',   '马克思主义学院', '13800000009'),
('T010', '$2b$10$4Pf5IMcM2DpAih1wCZZRvODEhky8FnUE.oejToTMfm5iBfCDacJp2', '冯晓', '女', '副教授', '马克思主义学院', '13800000010'),
('T011', '$2b$10$4Pf5IMcM2DpAih1wCZZRvODEhky8FnUE.oejToTMfm5iBfCDacJp2', '蒋丽', '女', '讲师',   '计算机学院',   '13800000011'),
('T012', '$2b$10$4Pf5IMcM2DpAih1wCZZRvODEhky8FnUE.oejToTMfm5iBfCDacJp2', '沈括', '男', '副教授', '计算机学院',   '13800000012'),
('T013', '$2b$10$4Pf5IMcM2DpAih1wCZZRvODEhky8FnUE.oejToTMfm5iBfCDacJp2', '马腾', '男', '教授',   '计算机学院',   '13800000013'),
('T014', '$2b$10$4Pf5IMcM2DpAih1wCZZRvODEhky8FnUE.oejToTMfm5iBfCDacJp2', '白若雪', '女', '副教授', '计算机学院',   '13800000014'),
('T015', '$2b$10$4Pf5IMcM2DpAih1wCZZRvODEhky8FnUE.oejToTMfm5iBfCDacJp2', '林文远', '男', '教授',   '人文学院',     '13800000015'),
('T016', '$2b$10$4Pf5IMcM2DpAih1wCZZRvODEhky8FnUE.oejToTMfm5iBfCDacJp2', '苏静雅', '女', '副教授', '人文学院',     '13800000016'),
('T017', '$2b$10$4Pf5IMcM2DpAih1wCZZRvODEhky8FnUE.oejToTMfm5iBfCDacJp2', '唐悦',   '女', '讲师',   '人文学院',     '13800000017');

-- 学生（密码均为 123456 的 BCrypt 哈希，共 36 名，每班 6 名）
INSERT INTO `student` (sno, password, name, gender, class_id, phone) VALUES
('2024001', '$2b$10$4Pf5IMcM2DpAih1wCZZRvODEhky8FnUE.oejToTMfm5iBfCDacJp2', '王小明', '男', 1, '13900000001'),
('2024002', '$2b$10$4Pf5IMcM2DpAih1wCZZRvODEhky8FnUE.oejToTMfm5iBfCDacJp2', '李小红', '女', 1, '13900000002'),
('2024003', '$2b$10$4Pf5IMcM2DpAih1wCZZRvODEhky8FnUE.oejToTMfm5iBfCDacJp2', '张子涵', '男', 1, '13900000003'),
('2024004', '$2b$10$4Pf5IMcM2DpAih1wCZZRvODEhky8FnUE.oejToTMfm5iBfCDacJp2', '刘思雨', '女', 1, '13900000004'),
('2024005', '$2b$10$4Pf5IMcM2DpAih1wCZZRvODEhky8FnUE.oejToTMfm5iBfCDacJp2', '陈俊杰', '男', 1, '13900000005'),
('2024006', '$2b$10$4Pf5IMcM2DpAih1wCZZRvODEhky8FnUE.oejToTMfm5iBfCDacJp2', '杨欣怡', '女', 1, '13900000006'),
('2024007', '$2b$10$4Pf5IMcM2DpAih1wCZZRvODEhky8FnUE.oejToTMfm5iBfCDacJp2', '赵一鸣', '男', 2, '13900000007'),
('2024008', '$2b$10$4Pf5IMcM2DpAih1wCZZRvODEhky8FnUE.oejToTMfm5iBfCDacJp2', '钱思琪', '女', 2, '13900000008'),
('2024009', '$2b$10$4Pf5IMcM2DpAih1wCZZRvODEhky8FnUE.oejToTMfm5iBfCDacJp2', '孙浩然', '男', 2, '13900000009'),
('2024010', '$2b$10$4Pf5IMcM2DpAih1wCZZRvODEhky8FnUE.oejToTMfm5iBfCDacJp2', '周婉婷', '女', 2, '13900000010'),
('2024011', '$2b$10$4Pf5IMcM2DpAih1wCZZRvODEhky8FnUE.oejToTMfm5iBfCDacJp2', '吴志强', '男', 2, '13900000011'),
('2024012', '$2b$10$4Pf5IMcM2DpAih1wCZZRvODEhky8FnUE.oejToTMfm5iBfCDacJp2', '郑雅琴', '女', 2, '13900000012'),
('2024013', '$2b$10$4Pf5IMcM2DpAih1wCZZRvODEhky8FnUE.oejToTMfm5iBfCDacJp2', '王宇航', '男', 3, '13900000013'),
('2024014', '$2b$10$4Pf5IMcM2DpAih1wCZZRvODEhky8FnUE.oejToTMfm5iBfCDacJp2', '冯梦洁', '女', 3, '13900000014'),
('2024015', '$2b$10$4Pf5IMcM2DpAih1wCZZRvODEhky8FnUE.oejToTMfm5iBfCDacJp2', '陈子昂', '男', 3, '13900000015'),
('2024016', '$2b$10$4Pf5IMcM2DpAih1wCZZRvODEhky8FnUE.oejToTMfm5iBfCDacJp2', '褚新月', '女', 3, '13900000016'),
('2024017', '$2b$10$4Pf5IMcM2DpAih1wCZZRvODEhky8FnUE.oejToTMfm5iBfCDacJp2', '卫博文', '男', 3, '13900000017'),
('2024018', '$2b$10$4Pf5IMcM2DpAih1wCZZRvODEhky8FnUE.oejToTMfm5iBfCDacJp2', '蒋雨桐', '女', 3, '13900000018'),
('2024019', '$2b$10$4Pf5IMcM2DpAih1wCZZRvODEhky8FnUE.oejToTMfm5iBfCDacJp2', '沈泽宇', '男', 4, '13900000019'),
('2024020', '$2b$10$4Pf5IMcM2DpAih1wCZZRvODEhky8FnUE.oejToTMfm5iBfCDacJp2', '韩若曦', '女', 4, '13900000020'),
('2024021', '$2b$10$4Pf5IMcM2DpAih1wCZZRvODEhky8FnUE.oejToTMfm5iBfCDacJp2', '杨文昊', '男', 4, '13900000021'),
('2024022', '$2b$10$4Pf5IMcM2DpAih1wCZZRvODEhky8FnUE.oejToTMfm5iBfCDacJp2', '朱静怡', '女', 4, '13900000022'),
('2024023', '$2b$10$4Pf5IMcM2DpAih1wCZZRvODEhky8FnUE.oejToTMfm5iBfCDacJp2', '秦立群', '男', 4, '13900000023'),
('2024024', '$2b$10$4Pf5IMcM2DpAih1wCZZRvODEhky8FnUE.oejToTMfm5iBfCDacJp2', '尤美玲', '女', 4, '13900000024'),
('2024025', '$2b$10$4Pf5IMcM2DpAih1wCZZRvODEhky8FnUE.oejToTMfm5iBfCDacJp2', '许智博', '男', 5, '13900000025'),
('2024026', '$2b$10$4Pf5IMcM2DpAih1wCZZRvODEhky8FnUE.oejToTMfm5iBfCDacJp2', '何雨欣', '女', 5, '13900000026'),
('2024027', '$2b$10$4Pf5IMcM2DpAih1wCZZRvODEhky8FnUE.oejToTMfm5iBfCDacJp2', '吕文轩', '男', 5, '13900000027'),
('2024028', '$2b$10$4Pf5IMcM2DpAih1wCZZRvODEhky8FnUE.oejToTMfm5iBfCDacJp2', '施雅静', '女', 5, '13900000028'),
('2024029', '$2b$10$4Pf5IMcM2DpAih1wCZZRvODEhky8FnUE.oejToTMfm5iBfCDacJp2', '张昊然', '男', 5, '13900000029'),
('2024030', '$2b$10$4Pf5IMcM2DpAih1wCZZRvODEhky8FnUE.oejToTMfm5iBfCDacJp2', '孔诗涵', '女', 5, '13900000030'),
('2024031', '$2b$10$4Pf5IMcM2DpAih1wCZZRvODEhky8FnUE.oejToTMfm5iBfCDacJp2', '曹子墨', '男', 6, '13900000031'),
('2024032', '$2b$10$4Pf5IMcM2DpAih1wCZZRvODEhky8FnUE.oejToTMfm5iBfCDacJp2', '严晓芸', '女', 6, '13900000032'),
('2024033', '$2b$10$4Pf5IMcM2DpAih1wCZZRvODEhky8FnUE.oejToTMfm5iBfCDacJp2', '华俊熙', '男', 6, '13900000033'),
('2024034', '$2b$10$4Pf5IMcM2DpAih1wCZZRvODEhky8FnUE.oejToTMfm5iBfCDacJp2', '金乐怡', '女', 6, '13900000034'),
('2024035', '$2b$10$4Pf5IMcM2DpAih1wCZZRvODEhky8FnUE.oejToTMfm5iBfCDacJp2', '魏晨曦', '男', 6, '13900000035'),
('2024036', '$2b$10$4Pf5IMcM2DpAih1wCZZRvODEhky8FnUE.oejToTMfm5iBfCDacJp2', '陶思远', '女', 6, '13900000036');

-- 回填学生的学院/专业（与所属班级保持一致）
UPDATE `student` s JOIN `class` c ON s.class_id = c.id
SET s.college = c.college, s.major = c.major;

-- 课程（共 24 门：6 门公共必修 + 6 门专业类 + 12 门选修类）
-- 注意：一门课只有一条记录、一个课程编号；同一门课的多个教学班在 teaching_class 中。
INSERT INTO `course` (cno, cname, credit, hours, course_type, category) VALUES
('PK01', '高等数学',             4, 64, '公共必修', NULL),
('PK02', '大学英语',             3, 48, '公共必修', NULL),
('PK03', '大学体育',             1, 32, '公共必修', NULL),
('PK04', '思想道德与法治',       2, 32, '公共必修', NULL),
('PK05', '军事理论',             2, 32, '公共必修', NULL),
('PK06', '形势与政策',           1, 16, '公共必修', NULL),
('ZY01', '程序设计基础',         4, 64, '专业类', NULL),
('ZY02', '面向对象程序设计',     4, 64, '专业类', NULL),
('ZY03', '数据结构',             4, 64, '专业类', NULL),
('ZY04', '计算机组成原理',       3, 48, '专业类', NULL),
('ZY05', '机器学习导论',         3, 48, '专业类', NULL),
('ZY06', '网络协议分析',         3, 48, '专业类', NULL),
('XK01', 'Python数据分析',        2, 32, '选修类', '编程开发'),
('XK02', 'Web前端开发',           2, 32, '选修类', '编程开发'),
('XK03', '数据库原理与应用',      2, 32, '选修类', '数据库'),
('XK04', '人工智能概论',          2, 32, '选修类', '人工智能'),
('XK05', '网络安全基础',          2, 32, '选修类', '信息安全'),
('XK06', '云计算与大数据',        2, 32, '选修类', '大数据'),
('XK07', '《论语》精读',          2, 32, '选修类', '国学经典'),
('XK08', '中华诗词之美',          2, 32, '选修类', '诗词文学'),
('XK09', '美的历程：美学导论',    2, 32, '选修类', '美学艺术'),
('XK10', '心理健康与自我成长',    2, 32, '选修类', '心理健康'),
('XK11', '解读中国经济发展的密码', 2, 32, '选修类', '经济管理'),
('XK12', '中国书法史',            2, 32, '选修类', '书法艺术');

-- 教学班（共 34 个）：公共必修分两类
--   高等数学/大学英语为「分班」课（每班 1 个教学班，绑定班级，一个老师可带多个班）
--   大学体育/思想道德与法治/军事理论/形势与政策为「合班」大课（不绑定班级，全年级统一上）
--   专业类：按班级自动分配，每班 1 个教学班
--   选修类：学生自由选课，每门课 1 个教学班
-- selected_count 先置 0，末尾统一按选课表回填
-- 上课时间设计：公共必修周一~周三 1-4 节、专业类周四 1-2 节、选修类周四 3-4/5-6 与周五，互不冲突
INSERT INTO `teaching_class`
    (id, course_id, tc_no, teacher_id, semester_id, class_id, status,
     min_students, capacity, selected_count, location, schedule) VALUES
-- 公共必修（分班）：高等数学 PK01，每班 1 个教学班
(1,  1, 'PK0101', 1,  1, 1,    1, NULL, 40, 0, '教1-101', '周一 1-2节'),
(2,  1, 'PK0102', 1,  1, 2,    1, NULL, 40, 0, '教1-102', '周一 1-2节'),
(3,  1, 'PK0103', 2,  1, 3,    1, NULL, 40, 0, '教1-103', '周一 1-2节'),
(4,  1, 'PK0104', 2,  1, 4,    1, NULL, 40, 0, '教1-104', '周一 1-2节'),
(5,  1, 'PK0105', 3,  1, 5,    1, NULL, 40, 0, '教1-105', '周一 1-2节'),
(6,  1, 'PK0106', 3,  1, 6,    1, NULL, 40, 0, '教1-106', '周一 1-2节'),
-- 公共必修（分班）：大学英语 PK02，每班 1 个教学班
(7,  2, 'PK0201', 4,  1, 1,    1, NULL, 40, 0, '教2-201', '周一 3-4节'),
(8,  2, 'PK0202', 4,  1, 2,    1, NULL, 40, 0, '教2-202', '周一 3-4节'),
(9,  2, 'PK0203', 5,  1, 3,    1, NULL, 40, 0, '教2-203', '周一 3-4节'),
(10, 2, 'PK0204', 5,  1, 4,    1, NULL, 40, 0, '教2-204', '周一 3-4节'),
(11, 2, 'PK0205', 6,  1, 5,    1, NULL, 40, 0, '教2-205', '周一 3-4节'),
(12, 2, 'PK0206', 6,  1, 6,    1, NULL, 40, 0, '教2-206', '周一 3-4节'),
-- 公共必修（合班）：全年级统一上课，不绑定班级
(13, 3, 'PK03',   7,  1, NULL, 1, NULL, 60, 0, '操场',    '周二 1-2节'),
(14, 4, 'PK04',   8,  1, NULL, 1, NULL, 60, 0, '教3-301', '周二 3-4节'),
(15, 5, 'PK05',   9,  1, NULL, 1, NULL, 60, 0, '教1-201', '周三 1-2节'),
(16, 6, 'PK06',   10, 1, NULL, 1, NULL, 60, 0, '教2-205', '周三 3-4节'),
-- 专业类（按班级自动分配，每班 1 个教学班）
(17, 7,  'ZY01', 13, 1, 1,    1, NULL, 50, 0, '机房1', '周四 1-2节'),
(18, 8,  'ZY02', 14, 1, 2,    1, NULL, 50, 0, '机房2', '周四 1-2节'),
(19, 9,  'ZY03', 11, 1, 3,    1, NULL, 50, 0, '机房1', '周四 1-2节'),
(20, 10, 'ZY04', 12, 1, 4,    1, NULL, 50, 0, '机房2', '周四 1-2节'),
(21, 11, 'ZY05', 11, 1, 5,    1, NULL, 50, 0, '机房3', '周四 1-2节'),
(22, 12, 'ZY06', 12, 1, 6,    1, NULL, 50, 0, '机房3', '周四 1-2节'),
-- 选修类（学生自由选课，最低开课人数 25）：6 门计算机相关 + 6 门人文通识
(23, 13, 'XK01', 13, 1, NULL, 1, 25, 40, 0, '机房2',   '周五 1-2节'),
(24, 14, 'XK02', 14, 1, NULL, 1, 25, 40, 0, '机房3',   '周五 3-4节'),
(25, 15, 'XK03', 11, 1, NULL, 1, 25, 40, 0, '机房1',   '周四 3-4节'),
(26, 16, 'XK04', 12, 1, NULL, 1, 25, 40, 0, '机房2',   '周四 5-6节'),
(27, 17, 'XK05', 13, 1, NULL, 1, 25, 40, 0, '机房3',   '周五 5-6节'),
(28, 18, 'XK06', 14, 1, NULL, 1, 25, 40, 0, '机房1',   '周一 7-8节'),
(29, 19, 'XK07', 15, 1, NULL, 1, 25, 40, 0, '教3-101', '周二 5-6节'),
(30, 20, 'XK08', 15, 1, NULL, 1, 25, 40, 0, '教3-102', '周二 7-8节'),
(31, 21, 'XK09', 16, 1, NULL, 1, 25, 40, 0, '教3-103', '周三 5-6节'),
(32, 22, 'XK10', 17, 1, NULL, 1, 25, 40, 0, '教3-104', '周三 7-8节'),
(33, 23, 'XK11', 10, 1, NULL, 1, 25, 40, 0, '教3-201', '周一 5-6节'),
(34, 24, 'XK12', 16, 1, NULL, 1, 25, 40, 0, '教3-202', '周四 7-8节');

-- =====================================================
-- 选课记录
-- =====================================================

-- 分班课程（分班公共必修 + 专业类，均绑定班级）：按班级自动分配
INSERT INTO `sc` (student_id, teaching_class_id, select_time)
SELECT s.id, t.id, '2025-09-10 09:00:00'
FROM `student` s
JOIN `teaching_class` t ON s.class_id = t.class_id
JOIN `course` c ON c.id = t.course_id
WHERE c.course_type IN ('公共必修', '专业类') AND t.class_id IS NOT NULL;

-- 合班公共必修（未绑定班级）：全员自动分配
INSERT INTO `sc` (student_id, teaching_class_id, select_time)
SELECT s.id, t.id, '2025-09-10 09:30:00'
FROM `student` s
CROSS JOIN `teaching_class` t
JOIN `course` c ON c.id = t.course_id
WHERE c.course_type = '公共必修' AND t.class_id IS NULL;

-- 选修类：学生自由选课（手工种子）。
-- 注：选修课教学班 id 23~34 对应 XK01~XK12。
-- 注：选修课最低开课人数为 25，本种子中各选修课选课人数均低于 25，教师可演示「停止开设」（其中 XK05/XK06/XK11/XK12 人数最少）。
-- 注：每名学生在同一学期最多选 2 门选修课，故本种子每生选修课数量 ≤ 2（1 门计算机类 + 1 门人文类，少数学生仅 1 门）。
INSERT INTO `sc` (student_id, teaching_class_id, select_time) VALUES
-- XK01 Python数据分析（9 人）
(1, 23, '2025-09-12 10:00:00'),
(5, 23, '2025-09-12 10:00:00'),
(9, 23, '2025-09-12 10:00:00'),
(13, 23, '2025-09-12 10:00:00'),
(17, 23, '2025-09-12 10:00:00'),
(21, 23, '2025-09-12 10:00:00'),
(25, 23, '2025-09-12 10:00:00'),
(29, 23, '2025-09-12 10:00:00'),
(33, 23, '2025-09-12 10:00:00'),
-- XK02 Web前端开发（9 人）
(2, 24, '2025-09-12 10:00:00'),
(6, 24, '2025-09-12 10:00:00'),
(10, 24, '2025-09-12 10:00:00'),
(14, 24, '2025-09-12 10:00:00'),
(18, 24, '2025-09-12 10:00:00'),
(22, 24, '2025-09-12 10:00:00'),
(26, 24, '2025-09-12 10:00:00'),
(30, 24, '2025-09-12 10:00:00'),
(34, 24, '2025-09-12 10:00:00'),
-- XK03 数据库原理与应用（9 人）
(3, 25, '2025-09-12 10:00:00'),
(7, 25, '2025-09-12 10:00:00'),
(11, 25, '2025-09-12 10:00:00'),
(15, 25, '2025-09-12 10:00:00'),
(19, 25, '2025-09-12 10:00:00'),
(23, 25, '2025-09-12 10:00:00'),
(27, 25, '2025-09-12 10:00:00'),
(31, 25, '2025-09-12 10:00:00'),
(35, 25, '2025-09-12 10:00:00'),
-- XK04 人工智能概论（9 人）
(4, 26, '2025-09-12 10:00:00'),
(8, 26, '2025-09-12 10:00:00'),
(12, 26, '2025-09-12 10:00:00'),
(16, 26, '2025-09-12 10:00:00'),
(20, 26, '2025-09-12 10:00:00'),
(24, 26, '2025-09-12 10:00:00'),
(28, 26, '2025-09-12 10:00:00'),
(32, 26, '2025-09-12 10:00:00'),
(36, 26, '2025-09-12 10:00:00'),
-- XK05 网络安全基础（2 人，低于最低开课人数）
(29, 27, '2025-09-12 11:00:00'),
(33, 27, '2025-09-12 11:00:00'),
-- XK06 云计算与大数据（1 人，低于最低开课人数）
(30, 28, '2025-09-12 11:00:00'),
-- XK07 《论语》精读（7 人）
(1, 29, '2025-09-12 10:00:00'),
(5, 29, '2025-09-12 10:00:00'),
(9, 29, '2025-09-12 10:00:00'),
(13, 29, '2025-09-12 10:00:00'),
(17, 29, '2025-09-12 10:00:00'),
(21, 29, '2025-09-12 10:00:00'),
(25, 29, '2025-09-12 10:00:00'),
-- XK08 中华诗词之美（7 人）
(2, 30, '2025-09-12 10:00:00'),
(6, 30, '2025-09-12 10:00:00'),
(10, 30, '2025-09-12 10:00:00'),
(14, 30, '2025-09-12 10:00:00'),
(18, 30, '2025-09-12 10:00:00'),
(22, 30, '2025-09-12 10:00:00'),
(26, 30, '2025-09-12 10:00:00'),
-- XK09 美的历程：美学导论（7 人）
(3, 31, '2025-09-12 10:00:00'),
(7, 31, '2025-09-12 10:00:00'),
(11, 31, '2025-09-12 10:00:00'),
(15, 31, '2025-09-12 10:00:00'),
(19, 31, '2025-09-12 10:00:00'),
(23, 31, '2025-09-12 10:00:00'),
(27, 31, '2025-09-12 10:00:00'),
-- XK10 心理健康与自我成长（7 人）
(4, 32, '2025-09-12 10:00:00'),
(8, 32, '2025-09-12 10:00:00'),
(12, 32, '2025-09-12 10:00:00'),
(16, 32, '2025-09-12 10:00:00'),
(20, 32, '2025-09-12 10:00:00'),
(24, 32, '2025-09-12 10:00:00'),
(28, 32, '2025-09-12 10:00:00'),
-- XK11 解读中国经济发展的密码（2 人，低于最低开课人数）
(31, 33, '2025-09-12 11:00:00'),
(34, 33, '2025-09-12 11:00:00'),
-- XK12 中国书法史（1 人，低于最低开课人数）
(32, 34, '2025-09-12 11:00:00');

-- 按选课表回填已选人数，保证与 sc 记录一致
UPDATE `teaching_class` t
SET t.selected_count = (SELECT COUNT(*) FROM `sc` WHERE teaching_class_id = t.id);
