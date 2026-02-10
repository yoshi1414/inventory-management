package com.inventory.inventory_management;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 在庫管理システムのメインアプリケーションクラス
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
public class InventoryManagementApplication {

	/**
	 * アプリケーションのエントリーポイント
	 * @param args コマンドライン引数
	 */
	public static void main(String[] args) {
		SpringApplication.run(InventoryManagementApplication.class, args);
	}

}
