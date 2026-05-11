package com.smarthome.controller;

import com.smarthome.common.ApiResponse;
import com.smarthome.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;

@RestController
@RequestMapping("/api/v1/reports")
@Tag(name = "Reports", description = "API báo cáo và thống kê")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/export/excel")
    @Operation(summary = "Xuất báo cáo doanh thu ra file Excel (có lọc)")
    public ResponseEntity<Resource> exportExcel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) String status) throws IOException {
        
        byte[] data = reportService.generateOrderExcelReport(from, to, status);
        ByteArrayResource resource = new ByteArrayResource(data);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=bao_cao_doanh_thu.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .contentLength(data.length)
                .body(resource);
    }

    @GetMapping("/preview")
    @Operation(summary = "Xem trước dữ liệu báo cáo (JSON)")
    public ResponseEntity<ApiResponse<List<com.smarthome.dto.ReportPreviewDto>>> preview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) String status) {
        
        List<com.smarthome.dto.ReportPreviewDto> orders = reportService.getPreviewOrdersByRange(from, to, status);
        return ResponseEntity.ok(ApiResponse.ok("Lấy dữ liệu xem trước thành công", orders));
    }
}
