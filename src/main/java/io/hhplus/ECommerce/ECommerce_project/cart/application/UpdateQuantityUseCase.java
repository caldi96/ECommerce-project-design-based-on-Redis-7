package io.hhplus.ECommerce.ECommerce_project.cart.application;

import io.hhplus.ECommerce.ECommerce_project.cart.application.command.UpdateQuantityCommand;
import io.hhplus.ECommerce.ECommerce_project.cart.application.service.CartFinderService;
import io.hhplus.ECommerce.ECommerce_project.cart.domain.entity.Cart;
import io.hhplus.ECommerce.ECommerce_project.cart.domain.service.CartDomainService;
import io.hhplus.ECommerce.ECommerce_project.cart.presentation.response.UpdateQuantityResponse;
import io.hhplus.ECommerce.ECommerce_project.product.application.service.ProductFinderService;
import io.hhplus.ECommerce.ECommerce_project.product.domain.entity.Product;
import io.hhplus.ECommerce.ECommerce_project.user.application.service.UserFinderService;
import io.hhplus.ECommerce.ECommerce_project.user.domain.entity.User;
import io.hhplus.ECommerce.ECommerce_project.user.domain.service.UserDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateQuantityUseCase {

    private final CartDomainService cartDomainService;
    private final UserDomainService userDomainService;
    private final CartFinderService cartFinderService;
    private final ProductFinderService productFinderService;
    private final UserFinderService userFinderService;

    @Transactional
    public UpdateQuantityResponse execute(UpdateQuantityCommand command) {

        // 1. ID 검증
        cartDomainService.validateId(command.cartId());
        userDomainService.validateId(command.userId());

        // 2. 수량값 검증
        cartDomainService.validateQuantityPositive(command.quantity());

        // 3. 유저 존재 유무 확인
        User user = userFinderService.getUser(command.userId());

        // 4. 장바구니 조회
        Cart cart = cartFinderService.getCart(command.cartId());

        // 5. 사용자 검증 (다른 사용자의 장바구니 수정 방지)
        cartDomainService.validateSameUser(cart, user);

        // 6. 상품 정보 조회
        Product product = productFinderService.getActiveProduct(cart.getProduct().getId());

        // 7. 상품 주문 가능 여부 확인 (새로운 수량으로)
        cartDomainService.validateCanOrder(product, command.quantity());

        // 8. 수량 변경
        cart.changeQuantity(command.quantity());

        // 9. Response 생성 (총 가격 자동 계산됨)
        return UpdateQuantityResponse.from(cart, product);
    }
}
