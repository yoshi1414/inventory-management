package com.inventory.inventory_management.security;

import java.util.ArrayList;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.inventory.inventory_management.entity.User;
import com.inventory.inventory_management.repository.UserRepository;

/**
 * Spring SecurityのUserDetailsServiceインターフェースの実装クラス
 * ユーザー認証情報の読み込みを行う
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserDetailsServiceImpl.class);
    
    /** ユーザーリポジトリ */
    private final UserRepository userRepository;
    /** ログイン試行管理サービス */
    private final LoginAttemptService loginAttemptService;

    /**
     * コンストラクタ
     * @param userRepository ユーザーリポジトリ
     * @param loginAttemptService ログイン試行管理サービス
     */
    public UserDetailsServiceImpl(UserRepository userRepository, LoginAttemptService loginAttemptService) {
        this.userRepository = userRepository;
        this.loginAttemptService = loginAttemptService;
    }

    /**
     * ユーザー名からユーザー情報を読み込む
     * @param username ユーザー名
     * @return ユーザー詳細情報
     * @throws UsernameNotFoundException ユーザーが見つからない場合
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        logger.debug("ユーザー認証情報読み込み開始: username={}", username);
        
        // ログイン試行回数チェック（ブルートフォース攻撃対策）
        if (loginAttemptService.isBlocked(username)) {
            logger.warn("ブロック中のユーザーのログイン試行: username={}", username);
            throw new UsernameNotFoundException("ログインに失敗しました。ユーザー名またはパスワードが正しくありません。");
        }
        
        try {
            // LazyInitializationException対策: ロール情報を含めて取得
            User user = userRepository.findByUsernameWithRoles(username)
                .orElseThrow(() -> {
                    logger.warn("ユーザーが見つかりません: username={}", username);
                    return new UsernameNotFoundException("ログインに失敗しました。ユーザー名またはパスワードが正しくありません。");
                });
            
            logger.debug("ユーザー情報取得成功: username={}, active={}", username, user.getIsActive());
            
            Collection<GrantedAuthority> authorities = new ArrayList<>();
            // ユーザーに紐づくロールを取得
            user.getUserRoles().forEach(userRole -> {
                String roleName = userRole.getRole().getRoleName();
                // ROLE_プレフィックスがない場合は追加する
                if (!roleName.startsWith("ROLE_")) {
                    roleName = "ROLE_" + roleName;
                }
                authorities.add(new SimpleGrantedAuthority(roleName));
            });
            
            return new UserDetailsImpl(user, authorities);
        } catch (UsernameNotFoundException e) {
            throw e;
        } catch (Exception e) {
            logger.error("ユーザー認証情報読み込み中にエラー発生: username={}", username, e);
            throw new UsernameNotFoundException("ログインに失敗しました。ユーザー名またはパスワードが正しくありません。");
        }
    }
}
