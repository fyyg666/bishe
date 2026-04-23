package com.library.system.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.library.system.common.Constants;
import com.library.system.entity.User;
import com.library.system.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * 用户详情服务实现类
 * 用于Spring Security加载用户信息
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 查询用户
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        wrapper.eq(User::getDeleted, 0);
        User user = userMapper.selectOne(wrapper);

        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        // 检查账号状态
        if (Constants.UserStatus.DISABLED.equals(user.getStatus())) {
            throw new UsernameNotFoundException("账号已被禁用: " + username);
        }

        // 构建UserDetails对象
        return new org.springframework.security.core.userdetails.User(
                user.getId().toString(),  // 使用用户ID作为principal
                user.getPassword(),
                Constants.UserStatus.NORMAL.equals(user.getStatus()),  // enabled
                true,                     // accountNonExpired
                true,                     // credentialsNonExpired
                !Constants.UserStatus.LOCKED.equals(user.getStatus()),  // accountNonLocked
                Collections.singletonList(new SimpleGrantedAuthority(Constants.Token.ROLE_PREFIX + user.getRole())) 
        );
    }
}
