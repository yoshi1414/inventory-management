package com.inventory.inventory_management.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.inventory.inventory_management.dto.request.ProductSearchCriteriaDto;
import com.inventory.inventory_management.entity.Product;
import com.inventory.inventory_management.form.ProductDetailForm;
import com.inventory.inventory_management.form.ProductQuickForm;
import com.inventory.inventory_management.service.AdminProductService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 管理者用商品管理コントローラー
 * 商品の一覧表示・登録・編集・論理削除・復元を担当する
 */
@Slf4j
@RequiredArgsConstructor
@Controller
@RequestMapping("/admin/products")
public class AdminProductController {

    private final AdminProductService adminProductService;

    // =========================================================
    // 一覧
    // =========================================================

    /**
     * 管理者用商品一覧画面を表示する
     *
     * @param criteria 検索条件 DTO（クエリパラメータから自動バインド）
     * @param model    モデル
     * @return admin/products.html
     */
    @GetMapping
    public String showAdminProducts(ProductSearchCriteriaDto criteria, Model model) {
        try {
            // 検索条件の補正をサービス層で実行
            criteria = adminProductService.validateSearchCriteria(criteria);

            log.debug("管理者用商品一覧画面を表示: {}", criteria);

            Page<Product> productPage = adminProductService.searchProducts(
                    criteria.getSearch(),
                    criteria.getCategory(),
                    criteria.getStatus(),
                    criteria.getSort(),
                    criteria.getPage(),
                    criteria.isIncludeDeleted());

            List<String> categories = adminProductService.getAllCategories();

            // ページング情報の計算をサービス層で実行
            int[] pagingInfo = adminProductService.calculatePagingInfo(
                    criteria.getPage(), productPage.getSize(), productPage.getTotalElements());

            model.addAttribute("productPage",         productPage);
            model.addAttribute("products",            productPage.getContent());
            model.addAttribute("categories",          categories);
            model.addAttribute("criteria",            criteria);
            model.addAttribute("includeDeleted",      criteria.isIncludeDeleted());
            model.addAttribute("currentPage",         criteria.getPage());
            model.addAttribute("totalPages",          productPage.getTotalPages());
            model.addAttribute("totalElements",       productPage.getTotalElements());
            model.addAttribute("currentPageNumber",   criteria.getPage() + 1);
            model.addAttribute("pageSize",            productPage.getSize());
            model.addAttribute("startItem",           pagingInfo[0]);
            model.addAttribute("endItem",             pagingInfo[1]);

            // クイック登録フォーム用（空オブジェクトで初期化）
            if (!model.containsAttribute("quickForm")) {
                model.addAttribute("quickForm", new ProductQuickForm());
            }

            log.debug("商品一覧取得完了: 全{}件中{}-{}件 (ページ {}/{})",
                    productPage.getTotalElements(), pagingInfo[0], pagingInfo[1],
                    criteria.getPage() + 1, productPage.getTotalPages());

            return "admin/products";
        } catch (Exception e) {
            log.error("管理者用商品一覧画面表示時にエラーが発生: error={}", e.getMessage(), e);
            model.addAttribute("errorMessage", "商品情報の取得に失敗しました。");
            return "error";
        }
    }

    // =========================================================
    // 登録
    // =========================================================

    /**
     * 商品登録フォーム画面を表示する
     *
     * @param model モデル
     * @return admin/product-create.html
     */
    @GetMapping("/create")
    public String showCreateForm(Model model) {
        log.debug("商品登録フォームを表示");
        if (!model.containsAttribute("detailForm")) {
            model.addAttribute("detailForm", new ProductDetailForm());
        }
        model.addAttribute("categories", adminProductService.getAllCategories());
        return "admin/product-create";
    }

    /**
     * 商品をクイック登録する（一覧画面の簡易フォームから）
     *
     * @param form               クイック登録フォーム（バリデーション済み）
     * @param bindingResult      バリデーション結果
     * @param redirectAttributes リダイレクト用フラッシュ属性
     * @return 商品一覧画面へリダイレクト
     */
    @PostMapping("/quick-create")
    public String createProductQuick(
            @ModelAttribute("quickForm") @Valid ProductQuickForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            log.warn("商品クイック登録バリデーションエラー: errors={}", bindingResult.getErrorCount());
            redirectAttributes.addFlashAttribute(
                    "org.springframework.validation.BindingResult.quickForm", bindingResult);
            redirectAttributes.addFlashAttribute("quickForm", form);
            redirectAttributes.addFlashAttribute("error", "入力内容を確認してください。");
            return "redirect:/admin/products";
        }
        try {
            log.info("商品クイック登録開始: productName={}", form.getProductName());

            Product saved = adminProductService.createProductQuick(form);

            log.info("商品クイック登録完了: productId={}, productName={}", saved.getId(), saved.getProductName());
            redirectAttributes.addFlashAttribute("message",
                    "商品「" + saved.getProductName() + "」を登録しました。（商品ID: " + saved.getProductCode() + "）");
            return "redirect:/admin/products";
        } catch (IllegalArgumentException e) {
            log.warn("商品クイック登録エラー: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addFlashAttribute("quickForm", form);
            return "redirect:/admin/products";
        } catch (Exception e) {
            log.error("商品クイック登録時にエラーが発生: productName={}, error={}", form.getProductName(), e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "商品登録時にエラーが発生しました。");
            return "redirect:/admin/products";
        }
    }

    /**
     * 商品を詳細フォームから新規登録する（商品登録画面から）
     *
     * @param form               詳細登録フォーム（バリデーション済み）
     * @param bindingResult      バリデーション結果
     * @param redirectAttributes リダイレクト用フラッシュ属性
     * @return 商品一覧画面へリダイレクト
     */
    @PostMapping("/create")
    public String createProductDetail(
            @ModelAttribute("detailForm") @Valid ProductDetailForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            log.warn("商品詳細登録バリデーションエラー: errors={}", bindingResult.getErrorCount());
            redirectAttributes.addFlashAttribute(
                    "org.springframework.validation.BindingResult.detailForm", bindingResult);
            redirectAttributes.addFlashAttribute("detailForm", form);
            redirectAttributes.addFlashAttribute("error", "入力内容を確認してください。");
            return "redirect:/admin/products/create";
        }
        try {
            log.info("商品詳細登録開始: productName={}", form.getProductName());

            Product saved = adminProductService.createProductDetail(form);

            log.info("商品詳細登録完了: productId={}, productName={}", saved.getId(), saved.getProductName());
            redirectAttributes.addFlashAttribute("message",
                    "商品「" + saved.getProductName() + "」を登録しました。（商品ID: " + saved.getProductCode() + "）");
            return "redirect:/admin/products";
        } catch (IllegalArgumentException e) {
            log.warn("商品詳細登録エラー: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addFlashAttribute("detailForm", form);
            return "redirect:/admin/products/create";
        } catch (Exception e) {
            log.error("商品詳細登録時にエラーが発生: productName={}, error={}", form.getProductName(), e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "商品登録時にエラーが発生しました。");
            return "redirect:/admin/products/create";
        }
    }

    // =========================================================
    // 編集
    // =========================================================

    /**
     * 商品編集フォーム画面を表示する
     *
     * @param id    商品 ID
     * @param model モデル
     * @return admin/product-edit.html
     */
    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Integer id, Model model) {
        try {
            log.debug("商品編集フォームを表示: productId={}", id);

            Optional<Product> productOpt = adminProductService.getProductById(id);
            if (productOpt.isEmpty()) {
                log.warn("商品が見つかりません: productId={}", id);
                model.addAttribute("errorMessage", "商品が見つかりません。");
                return "error";
            }

            Product product = productOpt.get();
            ProductDetailForm form = adminProductService.createProductDetailForm(product);

            model.addAttribute("product", product);
            model.addAttribute("detailForm", form);
            model.addAttribute("categories", adminProductService.getAllCategories());

            return "admin/product-edit";
        } catch (Exception e) {
            log.error("商品編集フォーム表示時にエラーが発生: productId={}, error={}", id, e.getMessage(), e);
            model.addAttribute("errorMessage", "商品情報の取得に失敗しました。");
            return "error";
        }
    }

    /**
     * 商品情報を更新する
     *
     * @param id                 商品 ID
     * @param form               更新内容フォーム（バリデーション済み）
     * @param bindingResult      バリデーション結果
     * @param redirectAttributes リダイレクト用フラッシュ属性
     * @return 商品一覧画面へリダイレクト
     */
    @PostMapping("/{id}/edit")
    public String updateProduct(
            @PathVariable Integer id,
            @ModelAttribute("detailForm") @Valid ProductDetailForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            log.warn("商品更新バリデーションエラー: productId={}, errors={}", id, bindingResult.getErrorCount());
            redirectAttributes.addFlashAttribute(
                    "org.springframework.validation.BindingResult.detailForm", bindingResult);
            redirectAttributes.addFlashAttribute("detailForm", form);
            redirectAttributes.addFlashAttribute("error", "入力内容を確認してください。");
            return "redirect:/admin/products/" + id + "/edit";
        }
        try {
            log.info("商品更新開始: productId={}", id);

            Product saved = adminProductService.updateProductDetail(id, form);

            log.info("商品更新完了: productId={}, productName={}", saved.getId(), saved.getProductName());
            redirectAttributes.addFlashAttribute("message",
                    "商品「" + saved.getProductName() + "」を更新しました。");
            return "redirect:/admin/products";
        } catch (IllegalArgumentException e) {
            log.warn("商品更新エラー: productId={}, {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/products/" + id + "/edit";
        } catch (Exception e) {
            log.error("商品更新時にエラーが発生: productId={}, error={}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "商品更新時にエラーが発生しました。");
            return "redirect:/admin/products/" + id + "/edit";
        }
    }

    // =========================================================
    // 削除・復元
    // =========================================================

    /**
     * 商品を論理削除する
     *
     * @param id                 商品 ID
     * @param redirectAttributes リダイレクト用フラッシュ属性
     * @return 商品一覧画面へリダイレクト
     */
    @PostMapping("/{id}/delete")
    public String deleteProduct(
            @PathVariable Integer id,
            RedirectAttributes redirectAttributes) {
        try {
            log.info("商品削除開始: productId={}", id);

            adminProductService.deleteProduct(id);

            log.info("商品削除完了: productId={}", id);
            redirectAttributes.addFlashAttribute("message", "商品を削除しました。");
            return "redirect:/admin/products";
        } catch (IllegalArgumentException e) {
            log.warn("商品削除エラー: productId={}, {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/products";
        } catch (Exception e) {
            log.error("商品削除時にエラーが発生: productId={}, error={}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "商品削除時にエラーが発生しました。");
            return "redirect:/admin/products";
        }
    }

    /**
     * 論理削除された商品を復元する
     *
     * @param id                 商品 ID
     * @param redirectAttributes リダイレクト用フラッシュ属性
     * @return 商品一覧画面へリダイレクト
     */
    @PostMapping("/{id}/restore")
    public String restoreProduct(
            @PathVariable Integer id,
            RedirectAttributes redirectAttributes) {
        try {
            log.info("商品復元開始: productId={}", id);

            adminProductService.restoreProduct(id);

            log.info("商品復元完了: productId={}", id);
            redirectAttributes.addFlashAttribute("message", "商品を復元しました。");
            return "redirect:/admin/products";
        } catch (IllegalArgumentException e) {
            log.warn("商品復元エラー: productId={}, {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/products";
        } catch (Exception e) {
            log.error("商品復元時にエラーが発生: productId={}, error={}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "商品復元時にエラーが発生しました。");
            return "redirect:/admin/products";
        }
    }

    // =========================================================
    // CSV インポート / エクスポート（将来実装）
    // =========================================================

    /**
     * CSV インポートフォームを表示する
     *
     * @param model モデル
     * @return admin/product-import.html
     */
    @GetMapping("/import")
    public String showImportForm(Model model) {
        log.debug("CSV インポートフォームを表示");
        return "admin/product-import";
    }

    /**
     * CSV ファイルから商品を一括インポートする（将来実装）
     *
     * @param file               アップロードされた CSV ファイル
     * @param redirectAttributes リダイレクト用フラッシュ属性
     * @return 商品一覧画面へリダイレクト
     */
    @PostMapping("/import")
    public String importProducts(
            @org.springframework.web.bind.annotation.RequestParam("file")
            org.springframework.web.multipart.MultipartFile file,
            RedirectAttributes redirectAttributes) {
        log.info("CSV インポート: fileName={}, size={}", file.getOriginalFilename(), file.getSize());
        redirectAttributes.addFlashAttribute("message", "CSVインポート機能は準備中です。");
        return "redirect:/admin/products";
    }

    /**
     * 商品を CSV ファイルとしてエクスポートする（将来実装）
     *
     * @param redirectAttributes リダイレクト用フラッシュ属性
     * @return 商品一覧画面へリダイレクト
     */
    @GetMapping("/export")
    public String exportProducts(RedirectAttributes redirectAttributes) {
        log.info("CSV エクスポート要求");
        redirectAttributes.addFlashAttribute("message", "CSVエクスポート機能は準備中です。");
        return "redirect:/admin/products";
    }
}

