package com.example.curriculum.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.curriculum.common.exception.BusinessException;
import com.example.curriculum.dto.ChangePasswordDTO;
import com.example.curriculum.dto.ForgotPasswordDTO;
import com.example.curriculum.dto.LoginDTO;
import com.example.curriculum.dto.ResetPasswordDTO;
import com.example.curriculum.entity.Student;
import com.example.curriculum.entity.Teacher;
import com.example.curriculum.mapper.StudentMapper;
import com.example.curriculum.mapper.TeacherMapper;
import com.example.curriculum.utils.JwtUtil;
import com.example.curriculum.vo.LoginVO;
import com.example.curriculum.vo.UserVO;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 认证服务：登录、生成 token、修改密码、忘记密码
 */
@Service
public class AuthService {

    /** 忘记密码时重置成的默认密码 */
    private static final String DEFAULT_PASSWORD = "123456";

    private final StudentMapper studentMapper;
    private final TeacherMapper teacherMapper;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthService(StudentMapper studentMapper, TeacherMapper teacherMapper,
                       JwtUtil jwtUtil, BCryptPasswordEncoder passwordEncoder) {
        this.studentMapper = studentMapper;
        this.teacherMapper = teacherMapper;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    public LoginVO login(LoginDTO dto) {
        String role = dto.getRole();
        UserVO user = new UserVO();
        Long userId;

        if ("student".equals(role)) {
            // 学生：按学号查询并校验 BCrypt 密码
            Student s = studentMapper.selectOne(new LambdaQueryWrapper<Student>()
                    .eq(Student::getSno, dto.getAccount()));
            if (s == null) {
                throw new BusinessException("学号不存在");
            }
            if (!passwordEncoder.matches(dto.getPassword(), s.getPassword())) {
                throw new BusinessException("密码错误");
            }
            userId = s.getId();
            user.setId(s.getId());
            user.setAccount(s.getSno());
            user.setName(s.getName());
        } else if ("teacher".equals(role)) {
            // 教师：按工号查询
            Teacher t = teacherMapper.selectOne(new LambdaQueryWrapper<Teacher>()
                    .eq(Teacher::getTno, dto.getAccount()));
            if (t == null) {
                throw new BusinessException("工号不存在");
            }
            if (!passwordEncoder.matches(dto.getPassword(), t.getPassword())) {
                throw new BusinessException("密码错误");
            }
            userId = t.getId();
            user.setId(t.getId());
            user.setAccount(t.getTno());
            user.setName(t.getName());
        } else if ("admin".equals(role)) {
            // 管理员：硬编码账号密码
            if (!"admin".equals(dto.getAccount()) || !"123456".equals(dto.getPassword())) {
                throw new BusinessException("管理员账号或密码错误");
            }
            userId = 0L; // 管理员无数据库记录
            user.setId(0L);
            user.setAccount("admin");
            user.setName("管理员");
        } else {
            throw new BusinessException("角色不合法");
        }

        user.setRole(role);
        String token = jwtUtil.generateToken(userId, role);

        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setUser(user);
        return vo;
    }

    /**
     * 登录页修改密码（公开接口）：按角色+账号定位，校验旧密码后更新为新密码。
     * 管理员为硬编码账号，不支持此方式修改。
     */
    public void changePasswordByAccount(ResetPasswordDTO dto) {
        String role = dto.getRole();
        String newPwd = passwordEncoder.encode(dto.getNewPassword());
        if ("student".equals(role)) {
            Student s = studentMapper.selectOne(new LambdaQueryWrapper<Student>()
                    .eq(Student::getSno, dto.getAccount()));
            if (s == null) {
                throw new BusinessException("学号不存在");
            }
            if (!passwordEncoder.matches(dto.getOldPassword(), s.getPassword())) {
                throw new BusinessException("旧密码错误");
            }
            s.setPassword(newPwd);
            studentMapper.updateById(s);
        } else if ("teacher".equals(role)) {
            Teacher t = teacherMapper.selectOne(new LambdaQueryWrapper<Teacher>()
                    .eq(Teacher::getTno, dto.getAccount()));
            if (t == null) {
                throw new BusinessException("工号不存在");
            }
            if (!passwordEncoder.matches(dto.getOldPassword(), t.getPassword())) {
                throw new BusinessException("旧密码错误");
            }
            t.setPassword(newPwd);
            teacherMapper.updateById(t);
        } else {
            throw new BusinessException("管理员不支持通过此方式修改密码");
        }
    }

    /**
     * 登录页「忘记密码」（公开接口）：忘记密码时无法提供旧密码，
     * 改用「账号 + 姓名 + 手机号」三项核验身份，通过后重置为默认密码 123456。
     * <p>
     * 为免被用来探测账号是否存在，核验不通过时统一返回同一条提示，不区分是哪一项不匹配。
     */
    @Transactional
    public void resetPasswordByVerification(ForgotPasswordDTO dto) {
        String role = dto.getRole();
        String account = dto.getAccount() == null ? "" : dto.getAccount().trim();
        String name = dto.getName() == null ? "" : dto.getName().trim();
        String phone = dto.getPhone() == null ? "" : dto.getPhone().trim();

        boolean verified;
        if ("student".equals(role)) {
            Student s = studentMapper.selectOne(new LambdaQueryWrapper<Student>()
                    .eq(Student::getSno, account));
            verified = s != null && name.equals(s.getName()) && phone.equals(s.getPhone());
            if (verified) {
                s.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
                studentMapper.updateById(s);
            }
        } else if ("teacher".equals(role)) {
            Teacher t = teacherMapper.selectOne(new LambdaQueryWrapper<Teacher>()
                    .eq(Teacher::getTno, account));
            verified = t != null && name.equals(t.getName()) && phone.equals(t.getPhone());
            if (verified) {
                t.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
                teacherMapper.updateById(t);
            }
        } else {
            throw new BusinessException("管理员不支持通过此方式重置密码");
        }

        if (!verified) {
            throw new BusinessException("账号、姓名或手机号不匹配，无法重置密码");
        }
    }

    /**
     * 个人信息页修改密码（已登录）：根据当前用户 id + 角色定位，校验旧密码后更新。
     */
    public void changeOwnPassword(Long userId, String role, ChangePasswordDTO dto) {
        String newPwd = passwordEncoder.encode(dto.getNewPassword());
        if ("student".equals(role)) {
            Student s = studentMapper.selectById(userId);
            if (s == null) {
                throw new BusinessException("学生不存在");
            }
            if (!passwordEncoder.matches(dto.getOldPassword(), s.getPassword())) {
                throw new BusinessException("旧密码错误");
            }
            s.setPassword(newPwd);
            studentMapper.updateById(s);
        } else if ("teacher".equals(role)) {
            Teacher t = teacherMapper.selectById(userId);
            if (t == null) {
                throw new BusinessException("教师不存在");
            }
            if (!passwordEncoder.matches(dto.getOldPassword(), t.getPassword())) {
                throw new BusinessException("旧密码错误");
            }
            t.setPassword(newPwd);
            teacherMapper.updateById(t);
        } else {
            throw new BusinessException("管理员不支持修改密码");
        }
    }
}
