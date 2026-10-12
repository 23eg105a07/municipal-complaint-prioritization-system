package com.municipal.system.user.controller;

import com.municipal.system.user.dto.CreateOfficerRequest;
import com.municipal.system.user.dto.UserResponse;
import com.municipal.system.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Users", description = "Administrator management of field officers and staff")
@SecurityRequirement(name = "bearerAuth")
public class AdminUserController {

    private final UserService userService;

    @PostMapping("/officers")
    @Operation(summary = "Create field officer", description = "Admin endpoint to create an officer with assigned department and zone.")
    public ResponseEntity<UserResponse> createOfficer(@Valid @RequestBody CreateOfficerRequest request) {
        UserResponse response = userService.createOfficer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/officers")
    @Operation(summary = "List officers", description = "List all field officers, optionally filtered by department.")
    public ResponseEntity<List<UserResponse>> listOfficers(@RequestParam(required = false) Long departmentId) {
        List<UserResponse> officers = userService.listOfficers(departmentId);
        return ResponseEntity.ok(officers);
    }
}
