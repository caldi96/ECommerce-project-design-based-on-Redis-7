package io.hhplus.ECommerce.ECommerce_project.point.application;

import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import io.hhplus.ECommerce.ECommerce_project.common.exception.PointException;
import io.hhplus.ECommerce.ECommerce_project.common.exception.UserException;
import io.hhplus.ECommerce.ECommerce_project.point.application.command.ChargePointCommand;
import io.hhplus.ECommerce.ECommerce_project.point.domain.entity.Point;
import io.hhplus.ECommerce.ECommerce_project.point.domain.repository.PointRepository;
import io.hhplus.ECommerce.ECommerce_project.user.domain.entity.User;
import io.hhplus.ECommerce.ECommerce_project.user.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChargePointUseCaseTest {

    @Mock
    private PointRepository pointRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ChargePointUseCase chargePointUseCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void execute_Success() {
        Long userId = 1L;
        BigDecimal amount = BigDecimal.valueOf(1000);
        String description = "테스트 충전";

        User user = new User(userId, "testUser", "password", BigDecimal.valueOf(500), LocalDateTime.now(), LocalDateTime.now());
        Point point = Point.charge(userId, amount, description);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(pointRepository.save(any(Point.class))).thenAnswer(i -> i.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        ChargePointCommand command = new ChargePointCommand(userId, amount, description);
        Point savedPoint = chargePointUseCase.execute(command);

        assertNotNull(savedPoint);
        assertEquals(amount, savedPoint.getAmount());
        assertEquals(userId, savedPoint.getUserId());
        assertEquals(BigDecimal.valueOf(1500), user.getPointBalance()); // 기존 500 + 충전 1000

        verify(pointRepository, times(1)).save(any(Point.class));
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void execute_UserNotFound_ThrowsException() {
        Long userId = 1L;
        BigDecimal amount = BigDecimal.valueOf(1000);

        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        ChargePointCommand command = new ChargePointCommand(userId, amount, "desc");

        UserException exception = assertThrows(UserException.class,
                () -> chargePointUseCase.execute(command));

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
        verify(pointRepository, never()).save(any());
    }

    @Test
    void execute_InvalidAmount_ThrowsException() {
        Long userId = 1L;
        User user = new User(userId, "testUser", "password", BigDecimal.valueOf(500), LocalDateTime.now(), LocalDateTime.now());

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        ChargePointCommand command = new ChargePointCommand(userId, BigDecimal.valueOf(-10), "desc");

        PointException exception = assertThrows(PointException.class,
                () -> chargePointUseCase.execute(command));

        assertEquals(ErrorCode.POINT_AMOUNT_INVALID, exception.getErrorCode());
        verify(pointRepository, never()).save(any());
    }
}
