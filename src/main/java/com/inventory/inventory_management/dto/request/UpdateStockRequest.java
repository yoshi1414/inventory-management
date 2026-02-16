package com.inventory.inventory_management.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 在庫更新リクエストDTO
 * 入庫・出庫・在庫数直接設定のリクエストを表現
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStockRequest {
    
    /**
     * 商品ID（必須）
     */
    @NotNull(message = "商品IDが指定されていません")
    private Integer productId;
    
    /**
     * 取引種別（必須、in/out/setのいずれか）
     */
    @NotNull(message = "取引種別が指定されていません")
    @Pattern(regexp = "^(in|out|set)$", message = "取引種別は in/out/set のみ有効です")
    private String transactionType;
    
    /**
     * 数量（必須、0以上）
     */
    @NotNull(message = "数量が指定されていません")
    @Min(value = 0, message = "数量は0以上である必要があります")
    private Integer quantity;
    
    /**
     * 備考（オプション）
     */
    private String remarks;
}
