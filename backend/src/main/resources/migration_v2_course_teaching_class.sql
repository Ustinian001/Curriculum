-- =====================================================
-- 迁移脚本 v2：拆分「课程」与「教学班」
-- 目标：一门课只有一个课程编号（course.cno 唯一），
--       但同一门课可以开设多个教学班，供多个学生选择。
--
-- 迁移前结构：course 表同时承担「课程」与「教学班」两个角色，
--             高等数学有 PK0101~PK0106 六个编号（每班一个教学班）。
-- 迁移后结构：course(课程, cno 唯一) + teaching_class(教学班) + sc(选课)
--
-- 重要：本脚本保留 teaching_class.id = 原 course.id，
--       因此 sc.course_id 的取值无需换算，只需改列名。
-- 执行前请先备份数据库。
-- =====================================================

SET NAMES utf8mb4;
USE curriculum;

-- ---------- 0. 备份原表 ----------
DROP TABLE IF EXISTS `course_bak_v1`;
DROP TABLE IF EXISTS `sc_bak_v1`;
CREATE TABLE `course_bak_v1` AS SELECT * FROM `course`;
CREATE TABLE `sc_bak_v1`    AS SELECT * FROM `sc`;

-- ---------- 1. 新建课程表（一门课一个编号） ----------
DROP TABLE IF EXISTS `course_new`;
CREATE TABLE `course_new` (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '课程id',
    cno         VARCHAR(32) NOT NULL COMMENT '课程编号（唯一，一门课一个编号）',
    cname       VARCHAR(64) NOT NULL COMMENT '课程名称',
    credit      INT COMMENT '学分',
    hours       INT COMMENT '学时',
    course_type VARCHAR(16) NOT NULL DEFAULT '选修类' COMMENT '课程类型：公共必修/专业类/选修类',
    category    VARCHAR(32) COMMENT '课程类别（选修类细分小类，如 国学经典/诗词文学 等）',
    UNIQUE KEY uk_cno (cno)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '课程表';

-- 24 门课程：原「分班课」的 PK0101~PK0106 / PK0201~PK0206 归并为一门课 PK01 / PK02
INSERT INTO `course_new` (cno, cname, credit, hours, course_type, category) VALUES
-- 公共必修
('PK01', '高等数学',             4, 64, '公共必修', NULL),
('PK02', '大学英语',             3, 48, '公共必修', NULL),
('PK03', '大学体育',             1, 32, '公共必修', NULL),
('PK04', '思想道德与法治',       2, 32, '公共必修', NULL),
('PK05', '军事理论',             2, 32, '公共必修', NULL),
('PK06', '形势与政策',           1, 16, '公共必修', NULL),
-- 专业类
('ZY01', '程序设计基础',         4, 64, '专业类', NULL),
('ZY02', '面向对象程序设计',     4, 64, '专业类', NULL),
('ZY03', '数据结构',             4, 64, '专业类', NULL),
('ZY04', '计算机组成原理',       3, 48, '专业类', NULL),
('ZY05', '机器学习导论',         3, 48, '专业类', NULL),
('ZY06', '网络协议分析',         3, 48, '专业类', NULL),
-- 选修类
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

-- ---------- 2. 新建教学班表 ----------
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

-- 原 course 的每一行 = 一个教学班；id 保持不变，编号沿用原课程编号
INSERT INTO `teaching_class`
    (id, course_id, tc_no, teacher_id, semester_id, class_id, status,
     min_students, capacity, selected_count, location, schedule)
SELECT o.id,
       n.id,
       o.cno,
       o.teacher_id, o.semester_id, o.class_id, o.status,
       o.min_students, o.capacity, o.selected_count, o.location, o.schedule
FROM `course` o
JOIN `course_new` n
  ON n.cno = CASE WHEN o.cno LIKE 'PK01__' THEN 'PK01'
                  WHEN o.cno LIKE 'PK02__' THEN 'PK02'
                  ELSE o.cno END;

-- ---------- 3. 替换课程表 ----------
DROP TABLE `course`;
RENAME TABLE `course_new` TO `course`;

-- ---------- 4. 选课表改为指向教学班（取值不变，仅改列名） ----------
ALTER TABLE `sc` CHANGE COLUMN `course_id` `teaching_class_id` BIGINT NOT NULL COMMENT '教学班id';
ALTER TABLE `sc` DROP INDEX `uk_student_course`,
              ADD UNIQUE KEY `uk_student_tc` (`student_id`, `teaching_class_id`);

-- ---------- 5. 校验 ----------
SELECT '课程数（应为一门课一条）' AS 检查项, COUNT(*) AS 值 FROM `course`
UNION ALL SELECT '不同课程编号数', COUNT(DISTINCT cno) FROM `course`
UNION ALL SELECT '重复课程编号数', COUNT(*) FROM (SELECT cno FROM `course` GROUP BY cno HAVING COUNT(*) > 1) t
UNION ALL SELECT '教学班数', COUNT(*) FROM `teaching_class`
UNION ALL SELECT '选课记录数', COUNT(*) FROM `sc`
UNION ALL SELECT '人数不一致的教学班数',
       COUNT(*) FROM `teaching_class` t
       WHERE t.selected_count <> (SELECT COUNT(*) FROM `sc` WHERE teaching_class_id = t.id);
