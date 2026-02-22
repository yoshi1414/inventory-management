package com.inventory.inventory_management.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;

/**
 * グローバル例外ハンドラー
 * <p>
 * アプリケーション全体の例外を一括で捕捉し、適切なエラーページへ誘導します。
 * すべての例外はログに記録され、ユーザーにはわかりやすいエラーメッセージが表示されます。
 * </p>
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 404エラー（ページが見つからない）を処理
     * <p>
     * 存在しないURLや静的リソースへのアクセス時に発生します。
     * </p>
     *
     * @param ex       発生した例外
     * @param request  HTTPリクエスト
     * @param response HTTPレスポンス
     * @param model    モデル
     * @return エラーページのビュー名
     */
    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public String handleNotFound(Exception ex, HttpServletRequest request, HttpServletResponse response, Model model) {
        log.warn("404エラー: リクエストされたリソースが見つかりません。URL={}, error={}", 
                   request.getRequestURI(), ex.getMessage());

        response.setStatus(HttpStatus.NOT_FOUND.value());
        model.addAttribute("status", HttpStatus.NOT_FOUND.value());
        model.addAttribute("error", "Not Found");
        model.addAttribute("message", "お探しのページは存在しないか、移動した可能性があります。");
        model.addAttribute("path", request.getRequestURI());
        model.addAttribute("timestamp", LocalDateTime.now());
        model.addAttribute("isAdmin", request.isUserInRole("ROLE_ADMIN"));

        return "error";
    }

    /**
     * 403エラー（アクセス拒否）を処理
     * <p>
     * 権限のないリソースへのアクセス時に発生します。
     * </p>
     *
     * @param ex       発生した例外
     * @param request  HTTPリクエスト
     * @param response HTTPレスポンス
     * @param model    モデル
     * @return エラーページのビュー名
     */
    @ExceptionHandler(AccessDeniedException.class)
    public String handleAccessDenied(AccessDeniedException ex, HttpServletRequest request, HttpServletResponse response, Model model) {
        log.warn("403エラー: アクセスが拒否されました。URL={}, user={}, error={}", 
                   request.getRequestURI(), 
                   request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : "anonymous",
                   ex.getMessage());

        response.setStatus(HttpStatus.FORBIDDEN.value());
        model.addAttribute("status", HttpStatus.FORBIDDEN.value());
        model.addAttribute("error", "Forbidden");
        model.addAttribute("message", "このページにアクセスする権限がありません。");
        model.addAttribute("path", request.getRequestURI());
        model.addAttribute("timestamp", LocalDateTime.now());
        model.addAttribute("isAdmin", request.isUserInRole("ROLE_ADMIN"));

        return "error";
    }

    /**
     * IllegalArgumentException（不正な引数）を処理
     * <p>
     * バリデーションエラーや不正なパラメータ時に発生します。
     * </p>
     *
     * @param ex       発生した例外
     * @param request  HTTPリクエスト
     * @param response HTTPレスポンス
     * @param model    モデル
     * @return エラーページのビュー名
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public String handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request, HttpServletResponse response, Model model) {
        log.warn("400エラー: 不正なリクエストです。URL={}, error={}", 
                   request.getRequestURI(), ex.getMessage());

        response.setStatus(HttpStatus.BAD_REQUEST.value());
        model.addAttribute("status", HttpStatus.BAD_REQUEST.value());
        model.addAttribute("error", "Bad Request");
        model.addAttribute("message", ex.getMessage() != null ? ex.getMessage() : "不正なリクエストです。");
        model.addAttribute("path", request.getRequestURI());
        model.addAttribute("timestamp", LocalDateTime.now());
        model.addAttribute("isAdmin", request.isUserInRole("ROLE_ADMIN"));

        return "error";
    }

    /**
     * すべての予期しない例外を処理（500エラー）
     * <p>
     * システムエラーや予期しない例外が発生した場合の最終的なハンドラーです。
     * 詳細なスタックトレースはログに記録されますが、ユーザーには表示されません。
     * </p>
     *
     * @param ex       発生した例外
     * @param request  HTTPリクエスト
     * @param response HTTPレスポンス
     * @param model    モデル
     * @return エラーページのビュー名
     */
    @ExceptionHandler(Exception.class)
    public String handleAllExceptions(Exception ex, HttpServletRequest request, HttpServletResponse response, Model model) {
        log.error("500エラー: 予期しないエラーが発生しました。URL={}, error={}", 
                    request.getRequestURI(), ex.getMessage(), ex);

        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        model.addAttribute("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        model.addAttribute("error", "Internal Server Error");
        model.addAttribute("message", "システムエラーが発生しました。しばらく時間をおいてから再度お試しください。");
        model.addAttribute("path", request.getRequestURI());
        model.addAttribute("timestamp", LocalDateTime.now());
        model.addAttribute("isAdmin", request.isUserInRole("ROLE_ADMIN"));

        // 開発環境のみスタックトレースを表示（本番環境では非表示にする）
        // model.addAttribute("trace", ex.getStackTrace());

        return "error";
    }
}
