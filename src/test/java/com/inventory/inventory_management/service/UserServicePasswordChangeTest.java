package com.inventory.inventory_management.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.inventory.inventory_management.entity.User;
import com.inventory.inventory_management.repository.UserRepository;

/**
 * UserServiceのテスト
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService - テスト")
class UserServicePasswordChangeTest {
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @InjectMocks
    private UserService userService;
    
    private User testUser;
    
    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1);
        testUser.setUsername("testuser");
        testUser.setPassword("$2a$12$encodedOldPassword");
        testUser.setEmail("test@example.com");
        testUser.setFullName("Test User");
        testUser.setIsActive(true);
        testUser.setCreatedAt(LocalDateTime.now().minusDays(30));
        testUser.setUpdatedAt(LocalDateTime.now().minusDays(30));
    }
    
    @Test
    @DisplayName("正常系: パスワード変更が成功する")
    void changePassword_Success() {
        // Given
        String username = "testuser";
        String currentPassword = "OldPass123!";
        String newPassword = "NewPass123!";
        
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(currentPassword, testUser.getPassword())).thenReturn(true);
        when(passwordEncoder.matches(newPassword, testUser.getPassword())).thenReturn(false);
        when(passwordEncoder.encode(newPassword)).thenReturn("$2a$12$encodedNewPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        
        // When
        assertDoesNotThrow(() -> userService.changePassword(username, currentPassword, newPassword));
        
        // Then
        verify(userRepository, times(1)).findByUsername(username);
        verify(passwordEncoder, times(2)).matches(anyString(), anyString());
        verify(passwordEncoder, times(1)).encode(newPassword);
        verify(userRepository, times(1)).save(any(User.class));
    }
    
    @Test
    @DisplayName("異常系: ユーザーが見つからない")
    void changePassword_UserNotFound() {
        // Given
        String username = "nonexistent";
        String currentPassword = "OldPass123!";
        String newPassword = "NewPass123!";
        
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());
        
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> userService.changePassword(username, currentPassword, newPassword));
        
        assertEquals("ユーザーが見つかりません", exception.getMessage());
        verify(userRepository, times(1)).findByUsername(username);
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(userRepository, never()).save(any(User.class));
    }
    
    @Test
    @DisplayName("異常系: 現在のパスワードが正しくない")
    void changePassword_CurrentPasswordIncorrect() {
        // Given
        String username = "testuser";
        String currentPassword = "WrongPassword123!";
        String newPassword = "NewPass123!";
        
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(currentPassword, testUser.getPassword())).thenReturn(false);
        
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> userService.changePassword(username, currentPassword, newPassword));
        
        assertEquals("現在のパスワードが正しくありません", exception.getMessage());
        verify(userRepository, times(1)).findByUsername(username);
        verify(passwordEncoder, times(1)).matches(currentPassword, testUser.getPassword());
        verify(userRepository, never()).save(any(User.class));
    }
    
    @Test
    @DisplayName("異常系: 新しいパスワードが現在のパスワードと同じ")
    void changePassword_SamePassword() {
        // Given
        String username = "testuser";
        String currentPassword = "SamePass123!";
        String newPassword = "SamePass123!";
        
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> userService.changePassword(username, currentPassword, newPassword));
        
        assertEquals("新しいパスワードは現在のパスワードと異なる必要があります", exception.getMessage());
        verify(userRepository, times(1)).findByUsername(username);
        verify(userRepository, never()).save(any(User.class));
    }
    
    @Test
    @DisplayName("異常系: 新しいパスワードが8文字未満")
    void changePassword_PasswordTooShort() {
        // Given
        String username = "testuser";
        String currentPassword = "OldPass123!";
        String newPassword = "Short1!";
        
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(currentPassword, testUser.getPassword())).thenReturn(true);
        
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> userService.changePassword(username, currentPassword, newPassword));
        
        assertEquals("パスワードは8文字以上である必要があります", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }
    
    @Test
    @DisplayName("異常系: 新しいパスワードに英大文字が含まれていない")
    void changePassword_NoUppercase() {
        // Given
        String username = "testuser";
        String currentPassword = "OldPass123!";
        String newPassword = "newpass123!";
        
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(currentPassword, testUser.getPassword())).thenReturn(true);
        
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> userService.changePassword(username, currentPassword, newPassword));
        
        assertEquals("パスワードに英大文字（A-Z）を含める必要があります", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }
    
    @Test
    @DisplayName("異常系: 新しいパスワードに英小文字が含まれていない")
    void changePassword_NoLowercase() {
        // Given
        String username = "testuser";
        String currentPassword = "OldPass123!";
        String newPassword = "NEWPASS123!";
        
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(currentPassword, testUser.getPassword())).thenReturn(true);
        
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> userService.changePassword(username, currentPassword, newPassword));
        
        assertEquals("パスワードに英小文字（a-z）を含める必要があります", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }
    
    @Test
    @DisplayName("異常系: 新しいパスワードに数字が含まれていない")
    void changePassword_NoNumber() {
        // Given
        String username = "testuser";
        String currentPassword = "OldPass123!";
        String newPassword = "NewPassword!";
        
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(currentPassword, testUser.getPassword())).thenReturn(true);
        
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> userService.changePassword(username, currentPassword, newPassword));
        
        assertEquals("パスワードに数字（0-9）を含める必要があります", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }
    
    @Test
    @DisplayName("異常系: 新しいパスワードに特殊文字が含まれていない")
    void changePassword_NoSpecialCharacter() {
        // Given
        String username = "testuser";
        String currentPassword = "OldPass123!";
        String newPassword = "NewPass123";
        
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(currentPassword, testUser.getPassword())).thenReturn(true);
        
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> userService.changePassword(username, currentPassword, newPassword));
        
        assertEquals("パスワードに特殊文字（!@#$%^&*）を含める必要があります", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }
    
    @Test
    @DisplayName("正常系: メールアドレスでユーザーを検索")
    void findByEmail_Success() {
        // Given
        String email = "test@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));
        
        // When
        Optional<User> result = userService.findByEmail(email);
        
        // Then
        assertTrue(result.isPresent());
        assertEquals(testUser, result.get());
        verify(userRepository, times(1)).findByEmail(email);
    }
    
    @Test
    @DisplayName("異常系: メールアドレスが見つからない")
    void findByEmail_NotFound() {
        // Given
        String email = "nonexistent@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        
        // When
        Optional<User> result = userService.findByEmail(email);
        
        // Then
        assertFalse(result.isPresent());
        verify(userRepository, times(1)).findByEmail(email);
    }
    
    @Test
    @DisplayName("正常系: ユーザー名でユーザーを検索")
    void findByUsername_Success() {
        // Given
        String username = "testuser";
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(testUser));
        
        // When
        Optional<User> result = userService.findByUsername(username);
        
        // Then
        assertTrue(result.isPresent());
        assertEquals(testUser, result.get());
        verify(userRepository, times(1)).findByUsername(username);
    }
    
    @Test
    @DisplayName("異常系: ユーザー名が見つからない")
    void findByUsername_NotFound() {
        // Given
        String username = "nonexistent";
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());
        
        // When
        Optional<User> result = userService.findByUsername(username);
        
        // Then
        assertFalse(result.isPresent());
        verify(userRepository, times(1)).findByUsername(username);
    }
    
    @Test
    @DisplayName("正常系: ユーザーを保存")
    void save_Success() {
        // Given
        User newUser = new User();
        newUser.setUsername("newuser");
        newUser.setPassword("Password123!");
        newUser.setEmail("new@example.com");
        newUser.setFullName("New User");
        newUser.setIsActive(true);
        newUser.setCreatedAt(LocalDateTime.now());
        newUser.setUpdatedAt(LocalDateTime.now());
        
        when(userRepository.save(any(User.class))).thenReturn(newUser);
        
        // When
        User result = userService.save(newUser);
        
        // Then
        assertNotNull(result);
        assertEquals("newuser", result.getUsername());
        verify(userRepository, times(1)).save(newUser);
    }
}
