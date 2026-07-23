package com.example.demo.service.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Service;

import com.example.demo.dto.HighValueMerchantSummaryDto;
import com.example.demo.dto.MerchantHighValueTxnRowDto;
import com.example.demo.repository.AdminMerchantSummaryRepository;
import com.example.demo.repository.AdminMerchantTxnRespository;
import com.example.demo.service.AdminMerchantReportService;

import lombok.RequiredArgsConstructor;

/**
 * 
 * Apache POI is a Java library which basically lets java to generate Microsoft Office documents.
 * Without Apache POI, Java cannot create .xlsx files.
 * 
 * SXSSFWorkbook is the streaming version of the XSSFWorkbook.
 * 
 * 
 *It returns the .xlsx file as the ByteArrayInputStream format.
 *
 *I implemented backend Excel export using Apache POI with SXSSF streaming workbook to handle large 
 *datasets without memory issues. The API accepts dynamic parameters like minAmount and merchant name. 
 *I fetch data in chunks using limit/offset pagination, writing each chunk to the Excel sheet row-by-row. 
 *I keep values numeric (counts/amounts) so Excel can filter and sum, and I store createdAt as a real date 
 *type using Timestamp with a date format style. I freeze the header and apply reusable styles. 
 *For autosizing with SXSSF, I explicitly track columns, otherwise POI throws an exception.
 *
 *
 *For optimization:
 *1. remove autosize, set fixed widths
 *2. stream HTTP response directly (don’t hold full byte[] in memory)
 *
 *
 * Remember its still not streaming directly to the client, first getting the ByteArrayInputStream, then
 * converting that to the byte array 0 -> that still keeps the whole file in memory at the end.
 * 
 * For large exports you can directly write to the HttpServletResponse.getOutputStream() (stream to client)
 * 
 * So, basically we are chunk/batch processing here first getting the 5000 records then writing to excel, then
 * writing another 5000 and write that to excel and so on.
 * 
 * limit is how many rows to return
 * offset is how many rows to skip.
 * 
 * 
 * ByteArrayOutputStream -> It’s an in-memory buffer, It writes to memory instead of disk, “Output” means you write into it, Think: a buffer you fill
 * ByteArrayInputStream -> “Input” means you read from it, Think: a buffer you consume, After you have bytes, you convert them into an InputStream:
 * 
 * POI workbook → write bytes to OutputStream → convert to byte[] → read bytes as InputStream → return to HTTP response
 * 
 * 	Build Excel in Java
       ↓
	Convert to bytes
	       ↓
	Store in memory buffer
	       ↓
	Send bytes to browser
 */
@Service
@RequiredArgsConstructor
public class AdminMerchantReportServiceImpl implements AdminMerchantReportService {

  private final AdminMerchantSummaryRepository summaryRepository;
  private final AdminMerchantTxnRespository txnRepository;

  private static final String XLSX_MIME =
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

  private static final int PAGE_SIZE = 5000; // this is how many records you fetch from the DB at one time.

  @Override
  public void exportHighValueMerchantsSummaryXlsx(BigDecimal minAmount, String q, OutputStream outputStream) {
    String safeQ = normalizeQ(q);

    // try with resources thing, Keep 200 rows in memory, rest flush it to the disk.
    try (SXSSFWorkbook wb = new SXSSFWorkbook(200);
         ) {

      Sheet sheet = wb.createSheet("Summary"); // this creates an Excel sheet.
      ((SXSSFSheet) sheet).trackAllColumnsForAutoSizing(); // trackAllColumnsForAutoSizing() is required because SXSSF doesn’t track widths by default.
      sheet.createFreezePane(0, 1); // createFreezePane(0, 1) freezes top row (header) so it stays visible when scrolling.
      // createFreezePane(colSplit, rowSplit) -> freeze 0 columns and 1 row, meaning the freeze the entire first row ie. of index 0.
      
      // Styles
      // I used DataFormat so numeric cells remain numeric for sorting/summing in Excel
      CellStyle headerStyle = headerStyle(wb); // header style
      CellStyle intStyle = intStyle(wb); // format 0 styling
      CellStyle moneyStyle = moneyStyle(wb); // money formatting style

      // Header
      String[] headers = {
          "Merchant ID",
          "Merchant Code",
          "Business Name",
          "Category",
          "High Value Txn Count",
          "Total High Value Amount",
          "Distinct Payers"
      };

      // Creates row 0 and writes column names.
      Row header = sheet.createRow(0);
      for (int i = 0; i < headers.length; i++) {
        Cell c = header.createCell(i);
        c.setCellValue(headers[i]);
        c.setCellStyle(headerStyle);
      }

      /** pagination support, basically fetching the data in chunks otherwise it will be heavy for the 
       * db and memory(ram) as well to process the data, that's why fetching that in chunks.
       */
      int rowNum = 1; // remember the row 0 was for the headers, so starting from 1.
      int offset = 0;

      while (true) {
        List<HighValueMerchantSummaryDto> page =
            summaryRepository.fetchSummary(minAmount, safeQ, PAGE_SIZE, offset); // safeQ is the name of the merchant.

        if (page == null || page.isEmpty()) break;

        for (HighValueMerchantSummaryDto dto : page) {
          Row row = sheet.createRow(rowNum++); // creates a excel per dto

          // Merchant ID
          Cell c0 = row.createCell(0);
          if (dto.getMerchantId() != null) c0.setCellValue(dto.getMerchantId());
          else c0.setCellValue("");

          row.createCell(1).setCellValue(nullSafe(dto.getMerchantCode()));
          row.createCell(2).setCellValue(nullSafe(dto.getBusinessName()));
          row.createCell(3).setCellValue(nullSafe(dto.getCategory()));

          Cell c4 = row.createCell(4);
          c4.setCellValue(dto.getHighValueTxnCount() == null ? 0 : dto.getHighValueTxnCount());
          c4.setCellStyle(intStyle);

          // Converting BigDecimal → double can lose precision for extremely large / fraction values, but for money it’s typically okay at 2 decimals. If interviewer pushes: you can store as string, but numeric is preferred.
          Cell c5 = row.createCell(5);
          c5.setCellValue(dto.getTotalHighValueAmount() == null ? 0 : dto.getTotalHighValueAmount().doubleValue()); // Apache POI writes numeric using double. BigDecimal is converted to doubleValue()
          c5.setCellStyle(moneyStyle);

          Cell c6 = row.createCell(6);
          c6.setCellValue(dto.getDistinctPayers() == null ? 0 : dto.getDistinctPayers());
          c6.setCellStyle(intStyle);
        }

        offset += PAGE_SIZE;
      }

      // Autosize (OK if sheet is not massive; otherwise remove)
      for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);

      // below three lines order matters a lot.
      /**
       * this basically takes the workbook, convert that to the actual .xlsx format and write buffer raw bytes(out thing), the data into it. 
       */
//      wb.write(out); // write bytes into out
      
      /**
       * 
       * wb is the java object representing the Excel structure.
       *Serialize the workbook structure into real .xlsx file format bytes, and write those bytes into the given OutputStream. 
       *Serialize means “convert the Java objects into the real file format”.
       */
      wb.write(outputStream); // stream directly
      wb.dispose(); // this basically cleans up the temporary disk files that are being created because of the flushing, to prevent memory leaks.
//      return new ByteArrayInputStream(out.toByteArray()); // Converts memory buffer to input stream so controller can send it as HTTP response.

    } catch (IOException e) {
    	
    	// if any exception occurs before write, the file will be incomplete/corrupt.
      throw new RuntimeException("Failed to generate high value merchant summary XLSX", e);
    }
  }
  

  @Override
  public void exportMerchantHighValueTxnsXlsx(Long merchantId, BigDecimal minAmount, OutputStream outputStream) {
    try (SXSSFWorkbook wb = new SXSSFWorkbook(200);
         ) {

      Sheet sheet = wb.createSheet("Transactions");
      ((SXSSFSheet) sheet).trackAllColumnsForAutoSizing();
      sheet.createFreezePane(0, 1);

      // Styles
      CellStyle headerStyle = headerStyle(wb);
      CellStyle intStyle = intStyle(wb);
      CellStyle moneyStyle = moneyStyle(wb);
      CellStyle dateTimeStyle = dateTimeStyle(wb);

      String[] headers = {
          "Merchant ID", "Merchant Code", "Business Name", "Category",
          "Payer User ID", "Payer Name", "Payer Email", "Payer Mobile",
          "Transaction DB ID", "Tx ID", "Reference ID",
          "Transaction Type", "Payment Mode",
          "Amount", "Status", "Created At"
      };

      Row header = sheet.createRow(0);
      for (int i = 0; i < headers.length; i++) {  
        Cell c = header.createCell(i);
        c.setCellValue(headers[i]);
        c.setCellStyle(headerStyle);
      }

      int rowNum = 1;
      int offset = 0;

      while (true) {
        List<MerchantHighValueTxnRowDto> page =
            txnRepository.fetchHighValueTxnsByMerchant(merchantId, minAmount, PAGE_SIZE, offset);

        if (page == null || page.isEmpty()) break;

        for (MerchantHighValueTxnRowDto dto : page) {
          Row row = sheet.createRow(rowNum++);

          // Merchant block
          setLong(row.createCell(0), dto.getMerchantId());
          row.createCell(1).setCellValue(nullSafe(dto.getMerchantCode()));
          row.createCell(2).setCellValue(nullSafe(dto.getBusinessName()));
          row.createCell(3).setCellValue(nullSafe(dto.getCategory()));

          // Payer block
          setLong(row.createCell(4), dto.getPayerUserId());
          row.createCell(5).setCellValue(nullSafe(dto.getPayerName()));
          row.createCell(6).setCellValue(nullSafe(dto.getPayerEmail()));
          row.createCell(7).setCellValue(nullSafe(dto.getPayerMobile()));

          // Tx block
          setLong(row.createCell(8), dto.getTransactionDbId());
          row.createCell(9).setCellValue(nullSafe(dto.getTxId()));
          row.createCell(10).setCellValue(nullSafe(dto.getReferenceId()));
          row.createCell(11).setCellValue(nullSafe(dto.getTransactionType()));
          row.createCell(12).setCellValue(nullSafe(dto.getPaymentMode()));

          // Amount numeric
          Cell amt = row.createCell(13);
          amt.setCellValue(dto.getAmount() == null ? 0 : dto.getAmount().doubleValue());
          amt.setCellStyle(moneyStyle);

          row.createCell(14).setCellValue(nullSafe(dto.getStatus()));

          // Created At as real Excel datetime
          /**
           *Excel expects Java Date/Timestamp type for date cells.
           *So we convert LocalDateTime to Timestamp and set date style. 
           *I wrote createdAt as an actual Excel date type rather than string so it supports sorting and filtering.
           */
          Cell created = row.createCell(15);
          if (dto.getCreatedAt() != null) {
            created.setCellValue(Timestamp.valueOf(dto.getCreatedAt()));
            created.setCellStyle(dateTimeStyle);
          } else {
            created.setCellValue("");
          }
        }

        offset += PAGE_SIZE;
      }

      /**
       *This tries to resize columns to fit content.
       * autosizing is expensive for large data. In production for huge exports, fixed widths are often better.
       * Our data is not that big so its fine to autosize it for now.
       * Autosize is convenient but expensive; for large exports we can set fixed widths for performance.
       */
      for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);

//      wb.write(out);
      wb.write(outputStream);
      wb.dispose();
//      return new ByteArrayInputStream(out.toByteArray()); // returning the bytes as an input stream so controller can send it.

    } catch (IOException e) {
      throw new RuntimeException("Failed to generate merchant transactions XLSX", e);
    }
  }

  // ----------------- Helpers & Styles -----------------

  // normalize search param so empty string doesn’t break repository logic.
  private static String normalizeQ(String q) {
    if (q == null) return null;
    String t = q.trim();
    return t.isEmpty() ? null : t;
  }

  // Returns empty string instead of null to avoid null values in Excel, have you seen null values in a report?
  private static String nullSafe(String s) {
    return s == null ? "" : s;
  }

//  Writes a long as numeric if present, otherwise blank.
  private static void setLong(Cell cell, Long v) {
    if (v == null) cell.setCellValue("");
    else cell.setCellValue(v);
  }

  // setting the style of the header.
  private static CellStyle headerStyle(Workbook wb) {
    Font font = wb.createFont();
    font.setBold(true);

    CellStyle style = wb.createCellStyle();
    style.setFont(font);
    style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
    style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    style.setBorderBottom(BorderStyle.THIN);
    style.setBorderTop(BorderStyle.THIN);
    style.setBorderLeft(BorderStyle.THIN);
    style.setBorderRight(BorderStyle.THIN);
    return style;
  }

  // number styling so that grouping, and other math operations like summing can be applied properly.
  private static CellStyle intStyle(Workbook wb) {
    DataFormat df = wb.createDataFormat();
    CellStyle style = wb.createCellStyle();
    style.setDataFormat(df.getFormat("0"));
    return style;
  }

  private static CellStyle moneyStyle(Workbook wb) {
    DataFormat df = wb.createDataFormat();
    CellStyle style = wb.createCellStyle();
    style.setDataFormat(df.getFormat("#,##0.00"));
    return style;
  }

  private static CellStyle dateTimeStyle(Workbook wb) {
    DataFormat df = wb.createDataFormat();
    CellStyle style = wb.createCellStyle();
    style.setDataFormat(df.getFormat("yyyy-mm-dd hh:mm:ss"));
    return style;
  }
}