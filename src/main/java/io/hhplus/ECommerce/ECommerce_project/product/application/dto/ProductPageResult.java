package io.hhplus.ECommerce.ECommerce_project.product.application.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.hhplus.ECommerce.ECommerce_project.product.domain.entity.Product;
import lombok.Getter;

import java.util.List;

@Getter
public class ProductPageResult {
    private final List<Product> products;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;
    private final boolean isFirst;
    private final boolean isLast;

    @JsonCreator
    public ProductPageResult(
            @JsonProperty("products") List<Product> products,
            @JsonProperty("page") int page,
            @JsonProperty("size") int size,
            @JsonProperty("totalElements") long totalElements,
            @JsonProperty("totalPages") int totalPages,
            @JsonProperty("isFirst") boolean isFirst,
            @JsonProperty("isLast") boolean isLast
    ) {
        this.products = products;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.isFirst = isFirst;
        this.isLast = isLast;
    }
}