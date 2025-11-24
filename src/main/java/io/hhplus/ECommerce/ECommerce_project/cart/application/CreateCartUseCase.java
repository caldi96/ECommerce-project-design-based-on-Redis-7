package io.hhplus.ECommerce.ECommerce_project.cart.application;

import io.hhplus.ECommerce.ECommerce_project.cart.application.command.CreateCartCommand;
import io.hhplus.ECommerce.ECommerce_project.cart.application.service.CartFinderService;
import io.hhplus.ECommerce.ECommerce_project.cart.domain.entity.Cart;
import io.hhplus.ECommerce.ECommerce_project.cart.domain.service.CartDomainService;
import io.hhplus.ECommerce.ECommerce_project.cart.infrastructure.CartRepository;
import io.hhplus.ECommerce.ECommerce_project.product.application.service.ProductFinderService;
import io.hhplus.ECommerce.ECommerce_project.product.domain.entity.Product;
import io.hhplus.ECommerce.ECommerce_project.user.application.service.UserFinderService;
import io.hhplus.ECommerce.ECommerce_project.user.domain.entity.User;
import io.hhplus.ECommerce.ECommerce_project.user.domain.service.UserDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CreateCartUseCase {

    private final CartRepository cartRepository;
    private final CartFinderService cartFinderService;
    private final CartDomainService cartDomainService;
    private final UserDomainService userDomainService;
    private final UserFinderService userFinderService;
    private final ProductFinderService productFinderService;

    @Transactional
    public Cart execute(CreateCartCommand command) {

        // 1. USER_ID 값 검증 (Domain Layer)
        userDomainService.validateId(command.userId());
        cartDomainService.validateQuantityPositive(command.quantity());

        // 2. 유저 존재 여부 확인
        User user = userFinderService.getUser(command.userId());

        // 3. 상품 존재 여부 확인
        Product product = productFinderService.getActiveProduct(command.productId());

        // 4. 이미 해당 사용자의 장바구니에 같은 상품 존재 여부 확인
        Optional<Cart> existingCart = cartFinderService.findByUserAndProduct(user, product);

        // 5-1. 이미 같은 상품이 존재하면 수량만 증가
        if (existingCart.isPresent()) {
            Cart cart = existingCart.get();
            // 증가될 총 수량 계산
            int newTotalQuantity = cart.getQuantity() + command.quantity();

            // 증가된 총 수량으로 주문 가능 여부 확인
            cartDomainService.validateCanOrder(product, newTotalQuantity);

            // 기존 수량에 추가
            cart.increaseQuantity(command.quantity());
            // 저장 후 반환
            return cart;
        }

        // 5-2. 새 카트 생성
        // 상품 주문 가능 여부 확인
        cartDomainService.validateCanOrder(product, command.quantity());
        Cart cart = Cart.createCart(
                user,
                product,
                command.quantity()
        );
        // 6. 저장 후 반환
        return cartRepository.save(cart);
    }
}
