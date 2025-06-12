package com.example.demo.service;

import com.example.demo.model.User;
import com.example.demo.model.Dtos.UserDTO;
import com.example.demo.repo.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

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

    public List<UserDTO> searchUsers(String prefix, String currentUsername) {
        List<User> users = repo.findTop10ByUsernameStartingWithAndIsVerifiedTrue(prefix).stream()
                .filter(user -> !user.getUsername().equals(currentUsername))
                .toList();
        if (users.isEmpty()) {
            return List.of(); // Return empty list if no users found
        }
        return users.stream().map(UserDTO::new).toList(); // Convert to UserDTO list
    }
}
