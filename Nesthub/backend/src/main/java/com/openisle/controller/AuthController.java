package com.openisle.controller;
import com.openisle.dto.LoginRequest;
import com.openisle.dto.RegisterRequest;
import com.openisle.model.User;
import com.openisle.service.JwtService;
import com.openisle.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final JwtService jwtService;

    public AuthController(UserService userService, JwtService jwtService) {
        this.userService = userService;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register user", description = "Register a new user account")
    @ApiResponse(
        responseCode = "200",
        description = "Registration result",
        content = @Content(schema = @Schema(implementation = Map.class))
    )
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        // 检查用户名是否已存在
        if (userService.findByUsername(req.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "用户名已存在"));
        }
        
        // 检查邮箱是否已存在
        if (userService.findByEmail(req.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "邮箱已被注册"));
        }
        
        // 注册用户
        User user = userService.register(
            req.getUsername(),
            req.getEmail(),
            req.getPassword(),
            "",
            com.openisle.model.RegisterMode.DIRECT
        );
        
        // 生成 Token
        String token = jwtService.generateToken(user.getUsername());
        
        return ResponseEntity.ok(Map.of(
            "token", token,
            "message", "注册成功"
        ));
    }

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Authenticate with username/email and password")
    @ApiResponse(
        responseCode = "200",
        description = "Authentication result",
        content = @Content(schema = @Schema(implementation = Map.class))
    )
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        // 按用户名查找
        Optional<User> userOpt = userService.findByUsername(req.getUsername());
        if (userOpt.isEmpty()) {
            // 按邮箱查找
            userOpt = userService.findByEmail(req.getUsername());
        }
        
        // 用户不存在或密码错误
        if (userOpt.isEmpty() || !userService.matchesPassword(userOpt.get(), req.getPassword())) {
            return ResponseEntity.badRequest().body(
                Map.of("error", "用户名或密码错误")
            );
        }
        
        User user = userOpt.get();
  
  
        if (user.isBanned()) {
         return ResponseEntity.status(403).body(Map.of("error", "该账号已被封禁"));
        }
  
        String token = jwtService.generateToken(user.getUsername());
        
        return ResponseEntity.ok(Map.of("token", token));
    }

    @GetMapping("/check")
    @SecurityRequirement(name = "JWT")
    @Operation(summary = "Check token", description = "Validate JWT token")
    @ApiResponse(
        responseCode = "200",
        description = "Token valid",
        content = @Content(schema = @Schema(implementation = Map.class))
    )
    public ResponseEntity<?> checkToken() {
        return ResponseEntity.ok(Map.of("valid", true));
    }
}