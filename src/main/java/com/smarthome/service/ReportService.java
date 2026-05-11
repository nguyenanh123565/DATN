package com.smarthome.service;

import com.smarthome.entity.Order;
import com.smarthome.repository.OrderRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ReportService {

    private final OrderRepository orderRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public ReportService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /**
     * Lấy danh sách đơn hàng trong khoảng thời gian (dùng cho Preview)
     */
    public List<Order> getOrdersByRange(LocalDateTime from, LocalDateTime to, String statusStr) {
        List<Order> orders;
        if (from == null || to == null) {
            orders = orderRepository.findAll();
        } else {
            orders = orderRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(from, to);
        }
        
        if (statusStr != null && !statusStr.trim().isEmpty()) {
            orders = orders.stream()
                    .filter(o -> o.getStatus() != null && o.getStatus().name().equalsIgnoreCase(statusStr))
                    .toList();
        }
        return orders;
    }

    /**
     * Lấy danh sách đơn hàng cho Xem trước (sử dụng DTO để tránh lỗi JSON)
     */
    public List<com.smarthome.dto.ReportPreviewDto> getPreviewOrdersByRange(LocalDateTime from, LocalDateTime to, String status) {
        List<Order> orders = getOrdersByRange(from, to, status);
        return orders.stream().map(order -> new com.smarthome.dto.ReportPreviewDto(
                order.getId(),
                order.getCustomerName(),
                order.getPhone(),
                order.getAddress(),
                order.getFinalAmount(),
                order.getStatus().name(),
                order.getCreatedAt()
        )).toList();
    }

    /**
     * Tạo file Excel chứa danh sách đơn hàng (có lọc theo ngày)
     */
    public byte[] generateOrderExcelReport(LocalDateTime from, LocalDateTime to, String status) throws IOException {
        List<Order> orders = getOrdersByRange(from, to, status);

        try (Workbook workbook = new XSSFWorkbook(); 
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            
            Sheet sheet = workbook.createSheet("Báo cáo Đơn hàng");

            // 1. Tạo Header Style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);

            // 2. Tạo Header Row
            String[] columns = {"ID Đơn hàng", "Khách hàng", "Số điện thoại", "Địa chỉ", "Tổng tiền (Final)", "Trạng thái", "Ngày đặt"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            // 3. Điền dữ liệu
            int rowIdx = 1;
            for (Order order : orders) {
                Row row = sheet.createRow(rowIdx++);

                row.createCell(0).setCellValue(order.getId());
                row.createCell(1).setCellValue(order.getCustomerName());
                row.createCell(2).setCellValue(order.getPhone());
                row.createCell(3).setCellValue(order.getAddress());
                row.createCell(4).setCellValue(order.getFinalAmount().doubleValue());
                row.createCell(5).setCellValue(order.getStatus().name());
                row.createCell(6).setCellValue(order.getCreatedAt() != null ? order.getCreatedAt().format(DATE_FORMATTER) : "");
            }

            // Auto-size columns
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }
}
