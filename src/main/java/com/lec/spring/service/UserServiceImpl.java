package com.lec.spring.service;

import com.lec.spring.domain.Authority;
import com.lec.spring.domain.User;
import com.lec.spring.repository.AuthorityRepository;
import com.lec.spring.repository.UserRepository;
import org.apache.ibatis.session.SqlSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    private final PasswordEncoder passwordEncoder;

    private final UserRepository userRepository;
    private final AuthorityRepository authorityRepository;

    public UserServiceImpl(PasswordEncoder passwordEncoder, SqlSession sqlSession) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = sqlSession.getMapper(UserRepository.class);
        this.authorityRepository = sqlSession.getMapper(AuthorityRepository.class);
    }


    @Override
    public User findByUsername(String username) {
        return userRepository.findByUsername(username.toUpperCase());
    }

    @Override
    public boolean isExist(String username) {
        return userRepository.findByUsername(username.toUpperCase()) != null;
    }

    // 신규회원 등록
    // User (username, password)
    @Override
    public int register(User user) {
        user.setUsername(user.getUsername().toUpperCase());
        user.setPassword(passwordEncoder.encode(user.getPassword())); // password 는 PasswordEncoder 로 암호화 하여 저장해야 한다
        userRepository.save(user); // 회원 저장, id 값 받아온다.

        Authority auth = authorityRepository.findByName("ROLE_MEMBER");
        Long userId = user.getId();
        Long authId = auth.getId();
        authorityRepository.addAuthority(userId, authId);
        return 1;
    }

    @Override
    public List<Authority> selectAuthorityById(Long id) {
        User user = userRepository.findById(id);
        if (user == null) return null;


        return authorityRepository.findByUser(user);
    }
}
