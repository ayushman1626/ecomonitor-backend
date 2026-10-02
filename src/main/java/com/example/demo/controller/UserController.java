package com.example.demo.controller;


import com.example.demo.model.Dtos.UserDTO;
import com.example.demo.model.Dtos.common.ApiResponse;
import com.example.demo.model.UserPrinciple;
import com.example.demo.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "User", description = "API endpoints for user profile management and search")
public class UserController {

    @Autowired
    UserService userService;

    @GetMapping("api/users/me")
    @Operation(summary = "Get currently authenticated user profile")
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

    @GetMapping("api/users/search")
    @Operation(summary = "Search users by prefix")
    public ResponseEntity<ApiResponse<List<UserDTO>>> searchUsers(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "query", required = false) String query,
            @AuthenticationPrincipal UserPrinciple userPrinciple
    ) {
        if (userPrinciple == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, "UNAUTHORIZED USER!!", null));

        String prefix = (q != null && !q.isEmpty()) ? q : query;
        if (prefix == null || prefix.trim().isEmpty())
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Search prefix cannot be empty", null));

        List<UserDTO> users = userService.searchUsers(prefix.trim(), userPrinciple.getUsername());

        if (users.isEmpty()) {
            return ResponseEntity.ok(new ApiResponse<>(true, "No users found", users));
        }
        return ResponseEntity.ok(new ApiResponse<>(true, "Users found", users));
    }

    @GetMapping("hello")
    @Operation(summary = "Health check endpoint")
    public ResponseEntity<String> helloWorld(){
        return ResponseEntity.ok("Hello App is running ");
    }
}
