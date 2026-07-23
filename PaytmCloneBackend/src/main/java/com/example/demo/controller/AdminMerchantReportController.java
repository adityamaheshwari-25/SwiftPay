package com.example.demo.controller;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import com.example.demo.service.AdminMerchantReportService;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 *produces = XLSX_MIME -> This endpoint produces (returns) responses of type XLSX 
 *
 *Content-Disposition: attachment
 *This is the key header that tells the browser to Download this as a file (don't show inline), and suggesting a file name.
 *Without this header, some browsers may try to display raw binary.
 *
 *Cache-Control: no-store
 *Tells browser don't cache this file, good for the security
 *
 *.contentType(MediaType.parseMediaType(XLSX_MIME))
 *Sets the response type to Excel.
 *
 *.body(new InputStreamResource(in));
 *Spring needs something it can stream to the HTTP response output stream.
 *InputStreamResource wraps your ByteArrayInputStream into a Spring Resource.
 *
 *If you don't set proper header then Browser may try to display file contents as text.
 *"no-store" -> It is part of HTTP protocol rules, its not a java keyword.
 *
 *Cache-Control: no-store
 *Means do not store this response anywhere — not in browser, not in proxy, not in memory.It disables caching completely.
 *
 *Any limitations in this approach?

Yes: using ByteArrayOutputStream means we hold the entire file in memory at the end. 
For very large exports, we should stream directly to HttpServletResponse.getOutputStream() 
or generate asynchronously and store the file (S3/MinIO) and return a link.

What is streaming in file download?
Streaming means writing file content directly to the HTTP response output stream as it is generated, instead of building the entire file in memory first. This reduces memory usage and improves scalability for large exports.

Why not always use ByteArrayOutputStream?
Because it stores the entire file in heap memory before sending, which can cause high memory consumption for large reports.
 *
 *
 *In the updated setup, I am implementing proper streaming only.
 *Even though it's streamed, the client still receives a standard HTTP response. 
 *You can verify response headers like Content-Type and Content-Disposition and ensure file integrity.
 *
 *
 *HttpServletResponse : Spring MVC usually hides it (when you return ResponseEntity or a DTO).
But when you want to stream large files, using HttpServletResponse is the most direct and efficient.



In the controller, I set response headers such as Content-Type and Content-Disposition so the client treats 
it as an Excel download. I then obtain the HttpServletResponse output stream and pass it to the service. 
In the service, Apache POI builds the workbook object and wb.write(outputStream) serializes the workbook 
into XLSX bytes and writes directly to the HTTP response stream. This is still a single HTTP request/response, 
but the response body is sent in chunks at the network level. Using SXSSFWorkbook and direct streaming avoids 
loading the entire dataset or file into memory, making it scalable for large exports. So memory saving happens
in two layers.
 */
@RestController
@RequestMapping("/api/v1/admin/merchants/reports")
@RequiredArgsConstructor
public class AdminMerchantReportController {

  private final AdminMerchantReportService reportService;

  // this is the MIME type, also called a Content-Type, it tells a client that the response is the Excel .xlsx file.
  private static final String XLSX_MIME =
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

//  @GetMapping(value = "/high-value.xlsx", produces = XLSX_MIME)
//  public ResponseEntity<InputStreamResource> downloadHighValueMerchantSummary(
//      @RequestParam BigDecimal minAmount,
//      @RequestParam(required = false) String q
//  ) {
//    ByteArrayInputStream in = reportService.exportHighValueMerchantsSummaryXlsx(minAmount, q);
//
//    // generate a filename
//    String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
//    String filename = "high_value_merchants_" + ts + "_min_" + minAmount + ".xlsx";
//
//    // returning response.
//    return ResponseEntity.ok()
//        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"") 
//        .header(HttpHeaders.CACHE_CONTROL, "no-store")
//        .contentType(MediaType.parseMediaType(XLSX_MIME))
//        .body(new InputStreamResource(in)); // basically InputStreamResponse is the HTTP response stream, so writing bytes from in and sending as http response stream.
//  }
  
  @GetMapping(value = "/high-value.xlsx", produces = XLSX_MIME)
  public void downloadHighValueMerchantSummary(
      @RequestParam BigDecimal minAmount,
      @RequestParam(required = false) String q,
      HttpServletResponse response
  ) throws IOException {

      response.setContentType(XLSX_MIME);
      response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
          "attachment; filename=\"high_value_merchants.xlsx\"");
      response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");

      // Stream directly to response output stream
      // Output Stream is a java concept: a thing you can write bytes into.
      /**
       *In this case, response.getOutputStream() returns a ServletOutputStream, which is connected to the network response body. 
       *It is literally the channel back to the browser/Postman.
       *
       *The controller hands the response output stream to service, So your service now writes directly into the HTTP response body.
       *Controller doesn’t return a Java object at all — it writes the response directly.
       */
      reportService.exportHighValueMerchantsSummaryXlsx(minAmount, q, response.getOutputStream());
  }

//  @GetMapping(value = "/merchant/{merchantId}/high-value-txns.xlsx", produces = XLSX_MIME)
//  public ResponseEntity<InputStreamResource> downloadMerchantHighValueTxns(
//      @PathVariable Long merchantId,
//      @RequestParam BigDecimal minAmount
//  ) {
//    ByteArrayInputStream in = reportService.exportMerchantHighValueTxnsXlsx(merchantId, minAmount);
//
//    String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
//    String filename = "merchant_" + merchantId + "_high_value_txns_" + ts + "_min_" + minAmount + ".xlsx";
//
//    return ResponseEntity.ok()
//        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
//        .header(HttpHeaders.CACHE_CONTROL, "no-store")
//        .contentType(MediaType.parseMediaType(XLSX_MIME))
//        .body(new InputStreamResource(in));
//  }
  
  @GetMapping(
		    value = "/merchant/{merchantId}/high-value-txns.xlsx",
		    produces = XLSX_MIME
		)
	public void downloadMerchantHighValueTxns(
	        @PathVariable Long merchantId,
	        @RequestParam BigDecimal minAmount,
	        HttpServletResponse response
	) throws IOException {

	    // 1️. Set response headers
	    response.setContentType(XLSX_MIME);

	    String ts = LocalDateTime.now()
	            .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

	    String filename = "merchant_" + merchantId
	            + "_high_value_txns_" + ts
	            + "_min_" + minAmount + ".xlsx";

	    response.setHeader(
	            HttpHeaders.CONTENT_DISPOSITION,
	            "attachment; filename=\"" + filename + "\""
	    );

	    response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");

	    // 2️. Stream directly to response output stream
	    reportService.exportMerchantHighValueTxnsXlsx(
	            merchantId,
	            minAmount,
	            response.getOutputStream()
	    );
	}
}