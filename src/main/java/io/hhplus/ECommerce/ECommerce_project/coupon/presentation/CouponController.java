package io.hhplus.ECommerce.ECommerce_project.coupon.presentation;

import io.hhplus.ECommerce.ECommerce_project.coupon.application.*;
import io.hhplus.ECommerce.ECommerce_project.coupon.presentation.request.CreateCouponRequest;
import io.hhplus.ECommerce.ECommerce_project.coupon.presentation.request.IssueCouponRequest;
import io.hhplus.ECommerce.ECommerce_project.coupon.presentation.request.UpdateCouponRequest;
import io.hhplus.ECommerce.ECommerce_project.coupon.presentation.response.CouponResponse;
import io.hhplus.ECommerce.ECommerce_project.coupon.presentation.response.UserCouponResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/coupons")
public class CouponController {

    private final CreateCouponUseCase createCouponUseCase;
    private final GetCouponListUseCase getCouponListUseCase;
    private final GetCouponUseCase getCouponUseCase;
    private final UpdateCouponUseCase updateCouponUseCase;
    private final IssueCouponUseCase issueCouponUseCase;
    private final DeactivateCouponUseCase deactivateCouponUseCase;
    private final ActivateCouponUseCase activateCouponUseCase;

    /**
     * 쿠폰 마스터 생성
     */
    @PostMapping
    public ResponseEntity<CouponResponse> createCoupon(@Valid @RequestBody CreateCouponRequest request) {
        var createdCoupon = createCouponUseCase.execute(request.toCommand());
        return ResponseEntity.ok(CouponResponse.from(createdCoupon));
    }

    /**
     * 쿠폰 마스터 목록 조회
     */
    @GetMapping
    public ResponseEntity<List<CouponResponse>> getCouponList() {
        var couponList = getCouponListUseCase.execute();
        return ResponseEntity.ok(CouponResponse.from(couponList));
    }

    /**
     * 쿠폰 마스터 단건 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<CouponResponse> getCoupon(@PathVariable Long id) {
        var coupon = getCouponUseCase.execute(id);
        return ResponseEntity.ok(CouponResponse.from(coupon));
    }

    /**
     * 쿠폰 마스터 수정
     */
    @PutMapping("/{id}")
    public ResponseEntity<CouponResponse> updateCoupon(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCouponRequest request
    ) {
        var command = request.toCommand(id);
        var updatedCoupon = updateCouponUseCase.execute(command);
        return ResponseEntity.ok(CouponResponse.from(updatedCoupon));
    }

    /**
     * 선착순 쿠폰 발급 (사용자당 1개)
     */
    @PostMapping("/issue")
    public ResponseEntity<UserCouponResponse> issueCoupon(@Valid @RequestBody IssueCouponRequest request) {
        var userCoupon = issueCouponUseCase.execute(request.toCommand());
        return ResponseEntity.ok(UserCouponResponse.from(userCoupon));
    }

    /**
     * 쿠폰 마스터 비활성화
     */
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<CouponResponse> deactivateCoupon(@PathVariable Long id) {
        var coupon = deactivateCouponUseCase.execute(id);
        return ResponseEntity.ok(CouponResponse.from(coupon));
    }

    /**
     * 쿠폰 마스터 활성화
     */
    @PatchMapping("/{id}/activate")
    public ResponseEntity<CouponResponse> activateCoupon(@PathVariable Long id) {
        var coupon = activateCouponUseCase.execute(id);
        return ResponseEntity.ok(CouponResponse.from(coupon));
    }
}
