package com.inventory.inventory_management.security;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * SecurityConfigのテストクラス
 * Beanの生成とPasswordEncoderの動作をテストします
 */
@SpringBootTest
@DisplayName("SecurityConfig単体テスト")
class SecurityConfigTest {

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 正常系：PasswordEncoderがBeanとして生成されている
     */
    @Test
    @DisplayName("正常系：PasswordEncoderがBeanとして生成されている")
    void testPasswordEncoderBean() {
        // then
        assertNotNull(passwordEncoder);
    }

    /**
     * 正常系：PasswordEncoderがBCryptPasswordEncoderである
     */
    @Test
    @DisplayName("正常系：PasswordEncoderがBCryptPasswordEncoderである")
    void testPasswordEncoderIsBCrypt() {
        // given
        String rawPassword = "password";

        // when
        String encodedPassword = passwordEncoder.encode(rawPassword);

        // then
        assertNotNull(encodedPassword);
        assertTrue(encodedPassword.startsWith("$2a$")); // BCryptのプレフィックス
        assertNotEquals(rawPassword, encodedPassword);
    }

    /**
     * 正常系：PasswordEncoderのmatchesメソッドが正しく動作する
     */
    @Test
    @DisplayName("正常系：PasswordEncoderのmatchesメソッドが正しく動作する")
    void testPasswordEncoderMatches() {
        // given
        String rawPassword = "password";
        String encodedPassword = passwordEncoder.encode(rawPassword);

        // when
        boolean matches = passwordEncoder.matches(rawPassword, encodedPassword);

        // then
        assertTrue(matches);
    }

    /**
     * 正常系：異なるパスワードでmatchesがfalseを返す
     */
    @Test
    @DisplayName("正常系：異なるパスワードでmatchesがfalseを返す")
    void testPasswordEncoderNotMatches() {
        // given
        String rawPassword = "password";
        String wrongPassword = "wrongpassword";
        String encodedPassword = passwordEncoder.encode(rawPassword);

        // when
        boolean matches = passwordEncoder.matches(wrongPassword, encodedPassword);

        // then
        assertFalse(matches);
    }

    /**
     * 正常系：同じパスワードでも毎回異なるハッシュが生成される
     */
    @Test
    @DisplayName("正常系：同じパスワードでも毎回異なるハッシュが生成される（ソルト付き）")
    void testPasswordEncoderGeneratesDifferentHashes() {
        // given
        String rawPassword = "password";

        // when
        String encodedPassword1 = passwordEncoder.encode(rawPassword);
        String encodedPassword2 = passwordEncoder.encode(rawPassword);

        // then
        assertNotEquals(encodedPassword1, encodedPassword2);
        assertTrue(passwordEncoder.matches(rawPassword, encodedPassword1));
        assertTrue(passwordEncoder.matches(rawPassword, encodedPassword2));
    }

    /**
     * 正常系：BCrypt強度12で生成されたハッシュが検証できる
     */
    @Test
    @DisplayName("正常系：BCrypt強度12で生成されたハッシュが検証できる")
    void testPasswordEncoderBCryptStrength12() {
        // given
        String rawPassword = "testPassword123";
        String encodedPassword = passwordEncoder.encode(rawPassword);

        // then
        // BCrypt強度12の場合、プレフィックスは$2a$12$となる
        assertTrue(encodedPassword.startsWith("$2a$12$"));
        assertTrue(passwordEncoder.matches(rawPassword, encodedPassword));
    }
}
