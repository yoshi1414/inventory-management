package com.inventory.inventory_management.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.inventory.inventory_management.dto.request.ProductSearchCriteriaDto;
import com.inventory.inventory_management.entity.Product;
import com.inventory.inventory_management.form.ProductDetailForm;
import com.inventory.inventory_management.form.ProductQuickForm;
import com.inventory.inventory_management.repository.ProductRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 管理者用商品管理サービス
 * 商品の検索・登録・更新・論理削除・復元を提供する
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminProductService {

    private final ProductRepository productRepository;

    @Value("${inventory.page-size}")
    private int pageSize;

    // =========================================================
    // 検索
    // =========================================================

    /**
     * 商品を検索（削除済み除外・ページング対応）
     *
     * @param keyword  商品名キーワード（部分一致）
     * @param category カテゴリフィルター
     * @param status   ステータスフィルター（active / inactive）
     * @param sortBy   ソート順（name / name_desc / price / price_desc / stock / stock_desc / updated）
     * @param page     ページ番号（0始まり）
     * @return 検索結果ページ
     */
    public Page<Product> searchProducts(
            String keyword,
            String category,
            String status,
            String sortBy,
            int page) {

        Sort sort = createSort(sortBy);
        Pageable pageable = PageRequest.of(page, pageSize, sort);

        log.debug("商品検索: keyword={}, category={}, status={}, sort={}, page={}",
                keyword, category, status, sortBy, page);

        return productRepository.findBySearchConditions(keyword, category, status, pageable);
    }

    /**
     * 商品 ID で 1 件取得（削除済み含む）
     *
     * @param id 商品 ID
     * @return 商品エンティティ（存在しない場合は empty）
     */
    public Optional<Product> getProductById(Integer id) {
        return productRepository.findById(id);
    }

    /**
     * 登録済みカテゴリ一覧を取得（削除済み商品除外）
     *
     * @return カテゴリ名リスト（昇順）
     */
    public List<String> getAllCategories() {
        return productRepository.findAllCategories();
    }

    // =========================================================
    // 登録
    // =========================================================

    /**
     * 商品をクイック登録する（一覧画面の簡易フォーム用）
     * 最小限のフィールド（商品名・カテゴリ・価格・在庫数・ステータス）のみ登録する。
     *
     * @param form クイック登録フォーム（バリデーション済み）
     * @return 登録した商品エンティティ
     * @throws IllegalArgumentException 商品コード生成に失敗した場合
     */
    @Transactional
    public Product createProductQuick(ProductQuickForm form) {
        log.info("商品クイック登録開始: productName={}", form.getProductName());

        Product product = new Product();
        product.setProductCode(generateProductCode());
        product.setProductName(form.getProductName().trim());
        product.setCategory(form.getCategory());
        product.setPrice(form.getPrice());
        product.setStock(form.getStockQuantity() != null ? form.getStockQuantity() : 0);
        product.setStatus(form.getStatus() != null ? form.getStatus() : "active");
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());

        Product saved = productRepository.save(product);
        log.info("商品クイック登録完了: productId={}, productCode={}, productName={}",
                saved.getId(), saved.getProductCode(), saved.getProductName());
        return saved;
    }

    /**
     * 商品を詳細フォームから新規登録する（商品登録画面用）
     * 全フィールド（SKU・説明・保証期間・寸法・バリエーション・製造日・有効期限・タグを含む）を登録する。
     *
     * @param form 詳細登録フォーム（バリデーション済み）
     * @return 登録した商品エンティティ
     * @throws IllegalArgumentException 商品コード生成に失敗した場合
     */
    @Transactional
    public Product createProductDetail(ProductDetailForm form) {
        log.info("商品詳細登録開始: productName={}", form.getProductName());

        Product product = new Product();
        product.setProductCode(generateProductCode());
        product.setProductName(form.getProductName().trim());
        product.setCategory(form.getCategory());
        product.setSku(blankToNull(form.getSku()));
        product.setPrice(form.getPrice());
        product.setStock(form.getStockQuantity() != null ? form.getStockQuantity() : 0);
        product.setStatus(form.getStatus() != null ? form.getStatus() : "active");
        product.setDescription(blankToNull(form.getDescription()));
        product.setWarrantyMonths(form.getWarrantyMonths());
        product.setDimensions(blankToNull(form.getDimensions()));
        product.setVariations(blankToNull(form.getVariations()));
        product.setManufacturingDate(parseDate(form.getManufacturingDate()));
        product.setExpirationDate(parseDate(form.getExpirationDate()));
        product.setTags(blankToNull(form.getTags()));
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());

        Product saved = productRepository.save(product);
        log.info("商品詳細登録完了: productId={}, productCode={}, productName={}",
                saved.getId(), saved.getProductCode(), saved.getProductName());
        return saved;
    }

    // =========================================================
    // 更新
    // =========================================================

    /**
     * 商品情報を詳細フォームから更新する（商品編集画面用）
     * 全フィールドを更新する。
     *
     * @param id   商品 ID
     * @param form 更新内容フォーム（バリデーション済み）
     * @return 更新後の商品エンティティ
     * @throws IllegalArgumentException 商品が存在しない場合
     */
    @Transactional
    public Product updateProductDetail(Integer id, ProductDetailForm form) {
        log.info("商品詳細更新開始: productId={}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("商品が見つかりません: id=" + id));

        product.setProductName(form.getProductName().trim());
        product.setCategory(form.getCategory());
        product.setSku(blankToNull(form.getSku()));
        product.setPrice(form.getPrice());
        product.setStock(form.getStockQuantity() != null ? form.getStockQuantity() : 0);
        product.setStatus(form.getStatus() != null ? form.getStatus() : "active");
        product.setDescription(blankToNull(form.getDescription()));
        product.setWarrantyMonths(form.getWarrantyMonths());
        product.setDimensions(blankToNull(form.getDimensions()));
        product.setVariations(blankToNull(form.getVariations()));
        product.setManufacturingDate(parseDate(form.getManufacturingDate()));
        product.setExpirationDate(parseDate(form.getExpirationDate()));
        product.setTags(blankToNull(form.getTags()));
        product.setUpdatedAt(LocalDateTime.now());

        Product saved = productRepository.save(product);
        log.info("商品詳細更新完了: productId={}, productName={}", saved.getId(), saved.getProductName());
        return saved;
    }

    // =========================================================
    // 削除・復元
    // =========================================================

    /**
     * 商品を論理削除する（deletedAt に現在日時をセット）
     *
     * @param id 商品 ID
     * @throws IllegalArgumentException 商品が存在しない場合
     */
    @Transactional
    public void deleteProduct(Integer id) {
        log.info("商品論理削除開始: productId={}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("商品が見つかりません: id=" + id));

        product.setDeletedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
        productRepository.save(product);

        log.info("商品論理削除完了: productId={}, productName={}", id, product.getProductName());
    }

    /**
     * 論理削除された商品を復元する（deletedAt を null にリセット）
     *
     * @param id 商品 ID
     * @throws IllegalArgumentException 商品が存在しない場合
     */
    @Transactional
    public void restoreProduct(Integer id) {
        log.info("商品復元開始: productId={}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("商品が見つかりません: id=" + id));

        product.setDeletedAt(null);
        product.setUpdatedAt(LocalDateTime.now());
        productRepository.save(product);

        log.info("商品復元完了: productId={}, productName={}", id, product.getProductName());
    }

    // =========================================================
    // プライベートメソッド
    // =========================================================

    /**
     * ソート条件を生成する
     *
     * @param sortBy ソート種別文字列
     * @return Sort オブジェクト
     */
    private Sort createSort(String sortBy) {
        if (sortBy == null) {
            return Sort.by(Sort.Direction.ASC, "productName");
        }
        return switch (sortBy) {
            case "name_desc" -> Sort.by(Sort.Direction.DESC, "productName");
            case "price"     -> Sort.by(Sort.Direction.ASC,  "price");
            case "price_desc"-> Sort.by(Sort.Direction.DESC, "price");
            case "stock"     -> Sort.by(Sort.Direction.ASC,  "stock");
            case "stock_desc"-> Sort.by(Sort.Direction.DESC, "stock");
            case "updated"   -> Sort.by(Sort.Direction.DESC, "updatedAt");
            default          -> Sort.by(Sort.Direction.ASC,  "productName");
        };
    }

    /**
     * 一意な 8 桁英数字の商品コードを生成する
     *
     * @return 商品コード（例: A3F7X9Z2）
     */
    private String generateProductCode() {
        for (int i = 0; i < 10; i++) {
            String code = UUID.randomUUID().toString()
                    .replace("-", "")
                    .substring(0, 8)
                    .toUpperCase();
            if (productRepository.findByProductCode(code) == null) {
                return code;
            }
        }
        throw new IllegalStateException("商品コードの生成に失敗しました。再試行してください。");
    }

    /**
     * 空文字または null を null に統一して返す
     *
     * @param value 入力値
     * @return null または trimしたvalue
     */
    private String blankToNull(String value) {
        return (value == null || value.trim().isEmpty()) ? null : value.trim();
    }

    /**
     * 日付文字列（yyyy-MM-dd）を LocalDate に変換する
     *
     * @param dateStr 日付文字列
     * @return LocalDate（null または空文字の場合は null）
     */
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr.trim());
        } catch (Exception e) {
            log.warn("日付解析失敗: value={}", dateStr);
            return null;
        }
    }

    /**
     * 検索条件の補正処理
     *
     * @param criteria 検索条件 DTO
     * @return 補正済みの検索条件 DTO
     */
    public ProductSearchCriteriaDto validateSearchCriteria(ProductSearchCriteriaDto criteria) {
        if (criteria.getPage() < 0) {
            log.warn("無効なページ番号: page={}", criteria.getPage());
            criteria.setPage(0);
        }
        if (criteria.getSearch() != null && !criteria.getSearch().trim().isEmpty()) {
            criteria.setSearch(criteria.getSearch().trim());
        }
        return criteria;
    }

    /**
     * ページング情報の計算
     *
     * @param pageNumber 現在のページ番号
     * @param pageSize   ページサイズ
     * @param totalItems 総アイテム数
     * @return ページング情報（開始アイテム番号、終了アイテム番号）
     */
    public int[] calculatePagingInfo(int pageNumber, int pageSize, long totalItems) {
        int startItem = pageNumber * pageSize + 1;
        int endItem = Math.min(startItem + pageSize - 1, (int) totalItems);
        return new int[]{startItem, endItem};
    }

    /**
     * 商品を ProductDetailForm に変換する
     *
     * @param product 商品エンティティ
     * @return ProductDetailForm
     */
    public ProductDetailForm createProductDetailForm(Product product) {
        ProductDetailForm form = new ProductDetailForm();
        form.setProductName(product.getProductName());
        form.setCategory(product.getCategory());
        form.setSku(product.getSku());
        form.setPrice(product.getPrice());
        form.setStockQuantity(product.getStock());
        form.setStatus(product.getStatus());
        form.setDescription(product.getDescription());
        form.setWarrantyMonths(product.getWarrantyMonths());
        form.setDimensions(product.getDimensions());
        form.setVariations(product.getVariations());
        form.setManufacturingDate(product.getManufacturingDate() != null
                ? product.getManufacturingDate().toString() : null);
        form.setExpirationDate(product.getExpirationDate() != null
                ? product.getExpirationDate().toString() : null);
        form.setTags(product.getTags());
        return form;
    }
}
