package com.tdarquier.test.user_management_service.controllers;

import org.springframework.web.bind.annotation.*;

import com.tdarquier.test.user_management_service.entities.User;
import com.tdarquier.test.user_management_service.services.UserService;

@RestController
@RequestMapping("/users")
public class UserController {

  final
  UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

// Endpoints Predeterminados
//

  @GetMapping("/{userId}")
  public User getUserById(@PathVariable String userId){

    return userService.getUserById(Long.valueOf(userId));
  }

  @PostMapping("")
  public User createUser(@RequestBody User user){

    return userService.createUser(user);
  }
  
  @PutMapping("")
  public User updateUser(@RequestBody User modifiedUser){

    return userService.updateUser(modifiedUser);
  }

  @DeleteMapping("")
  public void deleteUser(@RequestBody User user){

    userService.deleteUser(user);
  }
}