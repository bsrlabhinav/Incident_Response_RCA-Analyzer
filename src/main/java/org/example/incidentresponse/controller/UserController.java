package org.example.incidentresponse.controller;

import jakarta.validation.Valid;
import org.example.incidentresponse.dto.CreateUserRequest;
import org.example.incidentresponse.dto.UserResponse;
import org.example.incidentresponse.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request));
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> listUsers() {
        return ResponseEntity.ok(userService.listUsers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUser(@PathVariable String id) {
        return userService.getUser(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/resolve")
    public ResponseEntity<UserResponse> resolveOrCreate(@RequestBody Map<String, String> body) {
        String nameOrId = body.getOrDefault("nameOrId", "");
        return ResponseEntity.ok(userService.resolveOrCreate(nameOrId));
    }
}
