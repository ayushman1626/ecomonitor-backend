package com.example.demo.controller;


import com.example.demo.model.Dtos.UserDTO;
import com.example.demo.model.UserPrinciple;
import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    @Autowired
    UserService userService;

    @GetMapping("api/users/me")
    public ResponseEntity<?> getUserProfile(@AuthenticationPrincipal UserPrinciple userPrinciple){

        if(userPrinciple == null){
            return new ResponseEntity<>("UNAUTHORIZED USER!!", HttpStatus.UNAUTHORIZED);
        }

        try{
            UserDTO user = userService.getUserProfile(userPrinciple.getUsername());
            return ResponseEntity.ok(user);
        }catch(UsernameNotFoundException e){
            return new ResponseEntity<>("USER NOT FOUND", HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("hello")
    public ResponseEntity<String> helloWorld(){
        return ResponseEntity.ok("Hello App is running ");
    }
}
