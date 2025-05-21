package com.example.demo.service;

import com.example.demo.model.User;
import com.example.demo.model.Dtos.UserDTO;
import com.example.demo.repo.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    UserRepo repo;

    public UserDTO getUserProfile(String username) throws UsernameNotFoundException{
        User user = repo.findByUsernameAndIsVerifiedTrue(username)
                .orElseThrow(() -> new UsernameNotFoundException("USER NOT FOUND !!"));
        return new UserDTO(user);
    }

    public User getUserProfile2(String username) throws UsernameNotFoundException{
        User user = repo.findByUsernameAndIsVerifiedTrue(username)
                .orElseThrow(() -> new UsernameNotFoundException("USER NOT FOUND !!"));
        return user;
    }

}
