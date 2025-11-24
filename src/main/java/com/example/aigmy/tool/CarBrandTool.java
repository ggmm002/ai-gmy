package com.example.aigmy.tool;

import java.util.function.Supplier;

// 销售品牌查询工具
public class CarBrandTool implements Supplier<String> {
    @Override
    public String get() {
        return """
                我们销售的品牌有：奔驰，宝马，奥迪。
                """;
    }
}
