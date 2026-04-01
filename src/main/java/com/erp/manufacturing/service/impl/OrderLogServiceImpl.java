package com.erp.manufacturing.service.impl;

import com.erp.manufacturing.entity.OrderEntity;
import com.erp.manufacturing.entity.OrderLog;
import com.erp.manufacturing.repository.OrderLogRepository;
import com.erp.manufacturing.service.OrderLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class OrderLogServiceImpl implements OrderLogService {

    private final OrderLogRepository orderLogRepository;

    public OrderLogServiceImpl(OrderLogRepository orderLogRepository) {
        this.orderLogRepository = orderLogRepository;
    }

    @Override
    public void logAction(OrderEntity order, String action, String username) {
        OrderLog log = OrderLog.builder()
                .order(order)
                .action(action)
                .performedBy(username)
                .timestamp(LocalDateTime.now())
                .build();
        orderLogRepository.save(log);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderLog> getLogsForOrder(Long orderId) {
        return orderLogRepository.findByOrderIdOrderByTimestampDesc(orderId);
    }
}
