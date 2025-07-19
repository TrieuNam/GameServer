package com.southMillion.report_service.service;

import com.southMillion.report_service.entity.ReportEvent;
import com.southMillion.report_service.repository.ReportEventRepository;
import org.SouthMillion.dto.report.ReportResultDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class ReportEventService {
    @Autowired
    private ReportEventRepository repository;

    public ReportEvent save(ReportEvent event) {
        return repository.save(event);
    }


    public ReportResultDTO processReportToDTO(String data) {
        try {
            // Giải mã base64
            byte[] decodedBytes = Base64.getDecoder().decode(data);
            String decodedString = new String(decodedBytes, StandardCharsets.UTF_8);

            // TODO: Parse decodedString thành các trường nếu muốn

            // Demo trả về trạng thái + dữ liệu đã giải mã
            return new ReportResultDTO("ok", decodedString);
        } catch (Exception e) {
            // Nếu lỗi, trả về object với trạng thái lỗi
            return new ReportResultDTO("error", null);
        }
    }
}
