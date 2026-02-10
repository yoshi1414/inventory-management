package com.inventory.inventory_management.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * CSP違反レポートを受け取るコントローラー
 * Content Security Policy (CSP)の違反を監視し、ログに記録します。
 */
@RestController
public class CspReportController {

    private static final Logger logger = LoggerFactory.getLogger(CspReportController.class);

    /**
     * CSP違反レポートを受信して記録する
     * 
     * @param report CSP違反レポートのJSON文字列
     * @return HTTPステータス204 (No Content)
     */
    @PostMapping("/csp-violation-report-endpoint")
    public ResponseEntity<Void> reportCspViolation(@RequestBody String report) {
        logger.warn("CSP Violation detected: {}", report);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
