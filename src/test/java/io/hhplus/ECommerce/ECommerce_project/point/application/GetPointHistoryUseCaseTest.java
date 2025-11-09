package io.hhplus.ECommerce.ECommerce_project.point.application;

import io.hhplus.ECommerce.ECommerce_project.point.domain.entity.Point;
import io.hhplus.ECommerce.ECommerce_project.point.domain.repository.PointRepository;
import io.hhplus.ECommerce.ECommerce_project.point.presentation.response.GetPointHistoryResponse;
import io.hhplus.ECommerce.ECommerce_project.user.domain.entity.User;
import io.hhplus.ECommerce.ECommerce_project.user.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GetPointHistoryUseCaseTest {

    private PointRepository pointRepository;
    private UserRepository userRepository;
    private GetPointHistoryUseCase getPointHistoryUseCase;

    @BeforeEach
    void setUp() {
        pointRepository = mock(PointRepository.class);
        userRepository = mock(UserRepository.class);
        getPointHistoryUseCase = new GetPointHistoryUseCase(pointRepository, userRepository);
    }

    @Test
    void testGetPointHistory_Success() {
        Long userId = 1L;

        User user = new User(userId, "testUser", "password", BigDecimal.valueOf(1000),
                LocalDateTime.now(), LocalDateTime.now());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        Point point1 = Point.charge(userId, BigDecimal.valueOf(100), "충전1");
        Point point2 = Point.charge(userId, BigDecimal.valueOf(200), "충전2");

        when(pointRepository.findByUserIdWithPaging(userId, 0, 10))
                .thenReturn(List.of(point1, point2));
        when(pointRepository.countByUserId(userId))
                .thenReturn(2L);

        GetPointHistoryResponse response = getPointHistoryUseCase.execute(
                new io.hhplus.ECommerce.ECommerce_project.point.application.command.GetPointHistoryCommand(userId, 0, 10)
        );

        assertNotNull(response);
        assertEquals(0, response.currentPage());      // currentPage() 사용
        assertEquals(10, response.pageSize());        // pageSize() 사용
        assertEquals(2, response.totalElements());    // totalElements() 사용
        assertEquals(2, response.pointHistory().size()); // pointHistory() 사용
    }

    @Test
    void testGetPointHistory_UserNotFound() {
        Long userId = 999L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(io.hhplus.ECommerce.ECommerce_project.common.exception.UserException.class,
                () -> getPointHistoryUseCase.execute(
                        new io.hhplus.ECommerce.ECommerce_project.point.application.command.GetPointHistoryCommand(userId, 0, 10)
                ));
    }
}
