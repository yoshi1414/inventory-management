package com.inventory.inventory_management.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Securityの設定クラス
 * モック動作のため、一時的に全てのURLへのアクセスを許可
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
       http
           .authorizeHttpRequests((requests) -> requests
           .requestMatchers("/css/**", "/images/**", "/js/**", "/storage/**").permitAll()  // すべてのユーザーにアクセスを許可するURL
           .requestMatchers("/menu", "/admin/menu","/login", "/admin/login").permitAll() // メニュー画面へのアクセスを許可
           .requestMatchers("/admin/inventory","/inventory").permitAll() // 一般ユーザー・管理者用在庫一覧画面へのアクセスを許可
           .requestMatchers("/admin/products","/admin/products/**").permitAll() // 管理者用商品管理画面へのアクセスを許可
           .anyRequest().authenticated()  // 上記以外のURLはログインが必要（どのロールでもOK）
           );

       return http.build();
   }

}
