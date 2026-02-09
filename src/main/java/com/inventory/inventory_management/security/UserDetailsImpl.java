package com.inventory.inventory_management.security;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.inventory.inventory_management.entity.User;

/**
 * Spring SecurityのUserDetailsインターフェース実装。
 * 認証対象ユーザー情報と権限情報を保持する。
 */
public class UserDetailsImpl implements UserDetails {
    /** ユーザーエンティティ */
    private final User user;
    /** 権限のコレクション */
    private final Collection<GrantedAuthority> authorities;

    /**
     * コンストラクタ。
     * @param user ユーザーエンティティ
     * @param authorities 権限のコレクション
     */
    public UserDetailsImpl(User user, Collection<GrantedAuthority> authorities) {
        this.user = user;
        this.authorities = authorities;
    }

    /**
     * ユーザーエンティティを取得する。
     * @return ユーザーエンティティ
     */
    public User getUser() {
        return user;
    }

    // ハッシュ化済みのパスワードを返す
    @Override
    public String getPassword() {
        return user.getPassword();
    }

    /**
     * ログイン時に利用するユーザー名を返す。
     * 本アプリではログインフォームの入力（name="username"）と合わせて username を返す。
     * @return ユーザー名
     */
    @Override
    public String getUsername() {
        return user.getUsername();
    }

    // ロールのコレクションを返す
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    // アカウントが期限切れでなければtrueを返す
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    // ユーザーがロックされていなければtrueを返す
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    // ユーザーのパスワードが期限切れでなければtrueを返す
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    // ユーザーが有効であればtrueを返す
    @Override
    public boolean isEnabled() {
        return user.getIsActive();
    }
}
