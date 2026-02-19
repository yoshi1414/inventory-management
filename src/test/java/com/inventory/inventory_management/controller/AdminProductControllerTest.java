package com.inventory.inventory_management.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import com.inventory.inventory_management.dto.request.ProductSearchCriteriaDto;
import com.inventory.inventory_management.entity.Product;
import com.inventory.inventory_management.form.ProductDetailForm;
import com.inventory.inventory_management.form.ProductQuickForm;
import com.inventory.inventory_management.service.AdminProductService;

/**
 * AdminProductControllerのユニットテスト
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AdminProductController ユニットテスト")
class AdminProductControllerTest {

    @Mock
    private AdminProductService adminProductService;

    @InjectMocks
    private AdminProductController adminProductController;

    @Test
    @DisplayName("showAdminProducts: 正常系で一覧画面を返す")
    void showAdminProducts_Success() {
        ProductSearchCriteriaDto criteria = new ProductSearchCriteriaDto();
        criteria.setPage(0);

        Page<Product> page = new PageImpl<>(List.of(new Product()));
        when(adminProductService.validateSearchCriteria(criteria)).thenReturn(criteria);
        when(adminProductService.searchProducts(any(), any(), any(), any(), anyInt()))
                .thenReturn(page);
        when(adminProductService.getAllCategories()).thenReturn(List.of("Books"));
        when(adminProductService.calculatePagingInfo(0, page.getSize(), page.getTotalElements()))
                .thenReturn(new int[]{1, 1});

        Model model = new ExtendedModelMap();

        String view = adminProductController.showAdminProducts(criteria, model);

        assertEquals("admin/products", view);
        assertNotNull(model.getAttribute("products"));
        assertNotNull(model.getAttribute("quickForm"));
    }

    @Test
    @DisplayName("showAdminProducts: 例外時はerror画面")
    void showAdminProducts_Exception_ReturnsError() {
        ProductSearchCriteriaDto criteria = new ProductSearchCriteriaDto();
        Model model = new ExtendedModelMap();
        when(adminProductService.validateSearchCriteria(criteria)).thenThrow(new RuntimeException("DB error"));

        String view = adminProductController.showAdminProducts(criteria, model);

        assertEquals("error", view);
        assertEquals("商品情報の取得に失敗しました。", model.getAttribute("errorMessage"));
    }

    @Test
    @DisplayName("showCreateForm: detailFormが無ければ初期化")
    void showCreateForm_AddsForm() {
        Model model = new ExtendedModelMap();

        String view = adminProductController.showCreateForm(model);

        assertEquals("admin/product-create", view);
        assertNotNull(model.getAttribute("detailForm"));
    }

    @Test
    @DisplayName("showCreateForm: 既存detailFormがある場合は上書きしない")
    void showCreateForm_KeepsExistingForm() {
        ExtendedModelMap model = new ExtendedModelMap();
        ProductDetailForm existing = new ProductDetailForm();
        existing.setProductName("既存");
        model.addAttribute("detailForm", existing);

        String view = adminProductController.showCreateForm(model);

        assertEquals("admin/product-create", view);
        ProductDetailForm form = (ProductDetailForm) model.getAttribute("detailForm");
        assertEquals("既存", form.getProductName());
    }

    @Test
    @DisplayName("createProductQuick: バリデーションエラー時は一覧へ戻る")
    void createProductQuick_WhenValidationError_Redirects() {
        ProductQuickForm form = new ProductQuickForm();
        form.setProductName("");
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "quickForm");
        bindingResult.rejectValue("productName", "NotBlank", "必須");
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        String view = adminProductController.createProductQuick(form, bindingResult, redirectAttributes);

        assertEquals("redirect:/admin/products", view);
        assertEquals("入力内容を確認してください。", redirectAttributes.getFlashAttributes().get("error"));
    }

    @Test
    @DisplayName("createProductQuick: 正常登録で一覧へリダイレクト")
    void createProductQuick_Success() {
        ProductQuickForm form = new ProductQuickForm();
        form.setProductName("商品A");
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "quickForm");
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        Product saved = new Product();
        saved.setId(1);
        saved.setProductName("商品A");
        saved.setProductCode("ABCD1234");
        when(adminProductService.createProductQuick(form)).thenReturn(saved);

        String view = adminProductController.createProductQuick(form, bindingResult, redirectAttributes);

        assertEquals("redirect:/admin/products", view);
        assertTrue(redirectAttributes.getFlashAttributes().get("message").toString().contains("商品A"));
    }

    @Test
    @DisplayName("createProductQuick: IllegalArgumentException時はエラーで一覧へ戻る")
    void createProductQuick_WhenIllegalArgumentException_RedirectsWithError() {
        ProductQuickForm form = new ProductQuickForm();
        form.setProductName("商品A");
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "quickForm");
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        when(adminProductService.createProductQuick(form)).thenThrow(new IllegalArgumentException("重複エラー"));

        String view = adminProductController.createProductQuick(form, bindingResult, redirectAttributes);

        assertEquals("redirect:/admin/products", view);
        assertEquals("重複エラー", redirectAttributes.getFlashAttributes().get("error"));
    }

    @Test
    @DisplayName("createProductQuick: 想定外例外時はエラーで一覧へ戻る")
    void createProductQuick_WhenException_RedirectsWithError() {
        ProductQuickForm form = new ProductQuickForm();
        form.setProductName("商品A");
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "quickForm");
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        when(adminProductService.createProductQuick(form)).thenThrow(new RuntimeException("DB error"));

        String view = adminProductController.createProductQuick(form, bindingResult, redirectAttributes);

        assertEquals("redirect:/admin/products", view);
        assertEquals("商品登録時にエラーが発生しました。", redirectAttributes.getFlashAttributes().get("error"));
    }

    @Test
    @DisplayName("createProductDetail: バリデーションエラー時は登録画面へ戻る")
    void createProductDetail_WhenValidationError_Redirects() {
        ProductDetailForm form = new ProductDetailForm();
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "detailForm");
        bindingResult.rejectValue("productName", "NotBlank", "必須");
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        String view = adminProductController.createProductDetail(form, bindingResult, redirectAttributes);

        assertEquals("redirect:/admin/products/create", view);
        assertEquals("入力内容を確認してください。", redirectAttributes.getFlashAttributes().get("error"));
    }

    @Test
    @DisplayName("createProductDetail: 正常登録で一覧へリダイレクト")
    void createProductDetail_Success() {
        ProductDetailForm form = new ProductDetailForm();
        form.setProductName("詳細商品");
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "detailForm");
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        Product saved = new Product();
        saved.setId(11);
        saved.setProductName("詳細商品");
        saved.setProductCode("ZZZZ1111");
        when(adminProductService.createProductDetail(form)).thenReturn(saved);

        String view = adminProductController.createProductDetail(form, bindingResult, redirectAttributes);

        assertEquals("redirect:/admin/products", view);
        assertTrue(redirectAttributes.getFlashAttributes().get("message").toString().contains("詳細商品"));
    }

    @Test
    @DisplayName("createProductDetail: IllegalArgumentException時は登録画面へ戻る")
    void createProductDetail_WhenIllegalArgumentException_RedirectsWithError() {
        ProductDetailForm form = new ProductDetailForm();
        form.setProductName("詳細商品");
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "detailForm");
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        when(adminProductService.createProductDetail(form)).thenThrow(new IllegalArgumentException("入力不正"));

        String view = adminProductController.createProductDetail(form, bindingResult, redirectAttributes);

        assertEquals("redirect:/admin/products/create", view);
        assertEquals("入力不正", redirectAttributes.getFlashAttributes().get("error"));
    }

    @Test
    @DisplayName("createProductDetail: 想定外例外時は登録画面へ戻る")
    void createProductDetail_WhenException_RedirectsWithError() {
        ProductDetailForm form = new ProductDetailForm();
        form.setProductName("詳細商品");
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "detailForm");
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        when(adminProductService.createProductDetail(form)).thenThrow(new RuntimeException("DB error"));

        String view = adminProductController.createProductDetail(form, bindingResult, redirectAttributes);

        assertEquals("redirect:/admin/products/create", view);
        assertEquals("商品登録時にエラーが発生しました。", redirectAttributes.getFlashAttributes().get("error"));
    }

    @Test
    @DisplayName("showEditForm: 商品未存在時はerror画面")
    void showEditForm_WhenNotFound_ReturnsError() {
        when(adminProductService.getProductById(10)).thenReturn(Optional.empty());
        Model model = new ExtendedModelMap();

        String view = adminProductController.showEditForm(10, model);

        assertEquals("error", view);
        assertEquals("商品が見つかりません。", model.getAttribute("errorMessage"));
    }

    @Test
    @DisplayName("showEditForm: 正常系で編集画面を返す")
    void showEditForm_Success() {
        Product product = new Product();
        product.setId(1);
        product.setProductName("商品A");

        ProductDetailForm detailForm = new ProductDetailForm();
        detailForm.setProductName("商品A");

        when(adminProductService.getProductById(1)).thenReturn(Optional.of(product));
        when(adminProductService.createProductDetailForm(product)).thenReturn(detailForm);
        when(adminProductService.getAllCategories()).thenReturn(List.of("Books"));

        Model model = new ExtendedModelMap();
        String view = adminProductController.showEditForm(1, model);

        assertEquals("admin/product-edit", view);
        assertEquals(product, model.getAttribute("product"));
        assertEquals(detailForm, model.getAttribute("detailForm"));
        assertNotNull(model.getAttribute("categories"));
    }

    @Test
    @DisplayName("showEditForm: 例外時はerror画面")
    void showEditForm_WhenException_ReturnsError() {
        when(adminProductService.getProductById(1)).thenThrow(new RuntimeException("DB error"));
        Model model = new ExtendedModelMap();

        String view = adminProductController.showEditForm(1, model);

        assertEquals("error", view);
        assertEquals("商品情報の取得に失敗しました。", model.getAttribute("errorMessage"));
    }

    @Test
    @DisplayName("updateProduct: バリデーションエラー時は編集画面へ戻る")
    void updateProduct_WhenValidationError_Redirects() {
        ProductDetailForm form = new ProductDetailForm();
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "detailForm");
        bindingResult.rejectValue("productName", "NotBlank", "必須");
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        String view = adminProductController.updateProduct(5, form, bindingResult, redirectAttributes);

        assertEquals("redirect:/admin/products/5/edit", view);
    }

    @Test
    @DisplayName("updateProduct: 正常更新で一覧へ戻る")
    void updateProduct_Success() {
        ProductDetailForm form = new ProductDetailForm();
        form.setProductName("更新商品");
        form.setCategory("Books");
        form.setPrice(new BigDecimal("1000"));
        form.setStockQuantity(1);
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "detailForm");
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        Product saved = new Product();
        saved.setId(5);
        saved.setProductName("更新商品");
        when(adminProductService.updateProductDetail(5, form)).thenReturn(saved);

        String view = adminProductController.updateProduct(5, form, bindingResult, redirectAttributes);

        assertEquals("redirect:/admin/products", view);
        assertTrue(redirectAttributes.getFlashAttributes().get("message").toString().contains("更新商品"));
    }

    @Test
    @DisplayName("updateProduct: IllegalArgumentException時は編集画面へ戻る")
    void updateProduct_WhenIllegalArgumentException_RedirectsEdit() {
        ProductDetailForm form = new ProductDetailForm();
        form.setProductName("更新商品");
        form.setCategory("Books");
        form.setPrice(new BigDecimal("1000"));
        form.setStockQuantity(1);
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "detailForm");
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        when(adminProductService.updateProductDetail(5, form)).thenThrow(new IllegalArgumentException("対象なし"));

        String view = adminProductController.updateProduct(5, form, bindingResult, redirectAttributes);

        assertEquals("redirect:/admin/products/5/edit", view);
        assertEquals("対象なし", redirectAttributes.getFlashAttributes().get("error"));
    }

    @Test
    @DisplayName("updateProduct: 想定外例外時は編集画面へ戻る")
    void updateProduct_WhenException_RedirectsEdit() {
        ProductDetailForm form = new ProductDetailForm();
        form.setProductName("更新商品");
        form.setCategory("Books");
        form.setPrice(new BigDecimal("1000"));
        form.setStockQuantity(1);
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "detailForm");
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        when(adminProductService.updateProductDetail(5, form)).thenThrow(new RuntimeException("DB error"));

        String view = adminProductController.updateProduct(5, form, bindingResult, redirectAttributes);

        assertEquals("redirect:/admin/products/5/edit", view);
        assertEquals("商品更新時にエラーが発生しました。", redirectAttributes.getFlashAttributes().get("error"));
    }

    @Test
    @DisplayName("deleteProduct: 正常削除で一覧へ戻る")
    void deleteProduct_Success() {
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        String view = adminProductController.deleteProduct(2, redirectAttributes);

        assertEquals("redirect:/admin/products", view);
        verify(adminProductService, times(1)).deleteProduct(2);
    }

    @Test
    @DisplayName("deleteProduct: IllegalArgumentException時は一覧へ戻る")
    void deleteProduct_WhenIllegalArgumentException_Redirects() {
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        doThrow(new IllegalArgumentException("削除不可")).when(adminProductService).deleteProduct(2);

        String view = adminProductController.deleteProduct(2, redirectAttributes);

        assertEquals("redirect:/admin/products", view);
        assertEquals("削除不可", redirectAttributes.getFlashAttributes().get("error"));
    }

    @Test
    @DisplayName("deleteProduct: 想定外例外時は一覧へ戻る")
    void deleteProduct_WhenException_Redirects() {
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        doThrow(new RuntimeException("DB error")).when(adminProductService).deleteProduct(2);

        String view = adminProductController.deleteProduct(2, redirectAttributes);

        assertEquals("redirect:/admin/products", view);
        assertEquals("商品削除時にエラーが発生しました。", redirectAttributes.getFlashAttributes().get("error"));
    }

    @Test
    @DisplayName("restoreProduct: 正常復元で一覧へ戻る")
    void restoreProduct_Success() {
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        String view = adminProductController.restoreProduct(3, redirectAttributes);

        assertEquals("redirect:/admin/products", view);
        verify(adminProductService, times(1)).restoreProduct(3);
    }

    @Test
    @DisplayName("restoreProduct: IllegalArgumentException時は一覧へ戻る")
    void restoreProduct_WhenIllegalArgumentException_Redirects() {
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        doThrow(new IllegalArgumentException("復元不可")).when(adminProductService).restoreProduct(3);

        String view = adminProductController.restoreProduct(3, redirectAttributes);

        assertEquals("redirect:/admin/products", view);
        assertEquals("復元不可", redirectAttributes.getFlashAttributes().get("error"));
    }

    @Test
    @DisplayName("restoreProduct: 想定外例外時は一覧へ戻る")
    void restoreProduct_WhenException_Redirects() {
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        doThrow(new RuntimeException("DB error")).when(adminProductService).restoreProduct(3);

        String view = adminProductController.restoreProduct(3, redirectAttributes);

        assertEquals("redirect:/admin/products", view);
        assertEquals("商品復元時にエラーが発生しました。", redirectAttributes.getFlashAttributes().get("error"));
    }

    @Test
    @DisplayName("showImportForm: インポート画面を返す")
    void showImportForm_ReturnsView() {
        Model model = new ExtendedModelMap();

        String view = adminProductController.showImportForm(model);

        assertEquals("admin/product-import", view);
    }

    @Test
    @DisplayName("importProducts: 準備中メッセージで一覧へ戻る")
    void importProducts_ReturnsRedirect() {
        org.springframework.web.multipart.MultipartFile file = new org.springframework.mock.web.MockMultipartFile(
                "file", "products.csv", "text/csv", "a,b\n1,2".getBytes());
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        String view = adminProductController.importProducts(file, redirectAttributes);

        assertEquals("redirect:/admin/products", view);
        assertEquals("CSVインポート機能は準備中です。", redirectAttributes.getFlashAttributes().get("message"));
    }

    @Test
    @DisplayName("exportProducts: 準備中メッセージで一覧へ戻る")
    void exportProducts_ReturnsRedirect() {
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        String view = adminProductController.exportProducts(redirectAttributes);

        assertEquals("redirect:/admin/products", view);
        assertEquals("CSVエクスポート機能は準備中です。", redirectAttributes.getFlashAttributes().get("message"));
    }
}
