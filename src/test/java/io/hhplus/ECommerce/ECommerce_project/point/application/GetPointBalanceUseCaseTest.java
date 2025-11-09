package io.hhplus.ECommerce.ECommerce_project.point.application;

import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import io.hhplus.ECommerce.ECommerce_project.common.exception.UserException;
import io.hhplus.ECommerce.ECommerce_project.point.application.command.GetPointBalanceCommand;
import io.hhplus.ECommerce.ECommerce_project.point.domain.entity.Point;
import io.hhplus.ECommerce.ECommerce_project.point.domain.repository.PointRepository;
import io.hhplus.ECommerce.ECommerce_project.point.presentation.response.GetPointBalanceResponse;
import io.hhplus.ECommerce.ECommerce_project.user.domain.entity.User;
import io.hhplus.ECommerce.ECommerce_project.user.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GetPointBalanceUseCaseTest {

    @Mock
    private PointRepository pointRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private GetPointBalanceUseCase getPointBalanceUseCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void execute_Success() {
        Long userId = 1L;

        User user = new User(userId, "testUser", "password", BigDecimal.valueOf(500), LocalDateTime.now(), LocalDateTime.now());
        Point point1 = Point.charge(userId, BigDecimal.valueOf(100), "충전1");
        Point point2 = Point.charge(userId, BigDecimal.valueOf(200), "충전2");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(pointRepository.findAvailablePointsByUserId(userId)).thenReturn(List.of(point1, point2));

        GetPointBalanceCommand command = new GetPointBalanceCommand(userId);
        GetPointBalanceResponse response = getPointBalanceUseCase.execute(command);

        assertNotNull(response);
        assertEquals(userId, response.userId());
        assertEquals(BigDecimal.valueOf(300), response.totalBalance());

        verify(userRepository, times(1)).findById(userId);
        verify(pointRepository, times(1)).findAvailablePointsByUserId(userId);
    }

    @Test
    void execute_UserNotFound_ThrowsException() {
        Long userId = 1L;

        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        GetPointBalanceCommand command = new GetPointBalanceCommand(userId);

        UserException exception = assertThrows(UserException.class,
                () -> getPointBalanceUseCase.execute(command));

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
        verify(pointRepository, never()).findAvailablePointsByUserId(any());
    }
}
