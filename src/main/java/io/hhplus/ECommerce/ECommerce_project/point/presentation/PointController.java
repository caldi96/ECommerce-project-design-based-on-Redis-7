package io.hhplus.ECommerce.ECommerce_project.point.presentation;

import io.hhplus.ECommerce.ECommerce_project.point.application.ChargePointUseCase;
import io.hhplus.ECommerce.ECommerce_project.point.application.GetPointBalanceUseCase;
import io.hhplus.ECommerce.ECommerce_project.point.application.GetPointHistoryUseCase;
import io.hhplus.ECommerce.ECommerce_project.point.application.command.GetPointBalanceCommand;
import io.hhplus.ECommerce.ECommerce_project.point.application.command.GetPointHistoryCommand;
import io.hhplus.ECommerce.ECommerce_project.point.presentation.request.ChargePointRequest;
import io.hhplus.ECommerce.ECommerce_project.point.presentation.response.GetPointBalanceResponse;
import io.hhplus.ECommerce.ECommerce_project.point.presentation.response.GetPointHistoryResponse;
import io.hhplus.ECommerce.ECommerce_project.point.presentation.response.PointResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/points")
public class PointController {

    private final ChargePointUseCase chargePointUseCase;
    private final GetPointBalanceUseCase getPointBalanceUseCase;
    private final GetPointHistoryUseCase getPointHistoryUseCase;

    /**
     * 포인트 충전
     */
    @PostMapping("/charge")
    public ResponseEntity<PointResponse> chargePoint(@Valid @RequestBody ChargePointRequest request) {
        var chargedPoint = chargePointUseCase.execute(request.toCommand());
        return ResponseEntity.ok(PointResponse.from(chargedPoint));
    }

    /**
     * 포인트 잔액 조회
     */
    @GetMapping("/balance")
    public ResponseEntity<GetPointBalanceResponse> getPointBalance(
            @RequestParam @NotNull(message = "사용자 ID는 필수입니다") Long userId
    ) {
        GetPointBalanceCommand command = new GetPointBalanceCommand(userId);
        GetPointBalanceResponse response = getPointBalanceUseCase.execute(command);
        return ResponseEntity.ok(response);
    }

    /**
     * 포인트 이력 조회
     */
    @GetMapping("/history")
    public ResponseEntity<GetPointHistoryResponse> getPointHistory(
            @RequestParam @NotNull(message = "사용자 ID는 필수입니다") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        GetPointHistoryCommand command = new GetPointHistoryCommand(userId, page, size);
        GetPointHistoryResponse response = getPointHistoryUseCase.execute(command);
        return ResponseEntity.ok(response);
    }

}
