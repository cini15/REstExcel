package com.example.demo;

import com.example.demo.model.TotalMass;
import com.example.demo.model.dto.*;
import com.spire.doc.*;
import com.spire.doc.collections.CellCollection;
import com.spire.doc.documents.HorizontalAlignment;
import com.spire.doc.documents.Paragraph;
import com.spire.doc.documents.TableRowHeightType;
import com.spire.doc.documents.VerticalAlignment;
import com.spire.doc.fields.TextRange;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;


@RestController
@Slf4j
public class ProductController {
    String nameFile = "";
    int rowLast = 0;

    @PostMapping("/import-export")
    public String printData(@RequestBody CountryReport countryRequest) throws Exception {

        log.debug("CountryReport " + countryRequest);
        TotalMass totalMass = new TotalMass(countryRequest);
        log.debug("TotalMass " + totalMass);

        InputStream is = getClass().getClassLoader().getResourceAsStream("Blanc.xlsx");
        XSSFWorkbook xssfWorkbook = new XSSFWorkbook(is);

        XSSFCellStyle cellStyle = xssfWorkbook.createCellStyle();
        XSSFSheet sheet = xssfWorkbook.getSheetAt(0);
        cellStyle.setBorderLeft(XSSFCellStyle.BORDER_THIN);
        cellStyle.setBorderRight(XSSFCellStyle.BORDER_THIN);
        cellStyle.setBorderTop(XSSFCellStyle.BORDER_THIN);
        cellStyle.setBorderBottom(XSSFCellStyle.BORDER_THIN);
        cellStyle.setWrapText(true);

        XSSFFont font = xssfWorkbook.createFont();
        font.setFontHeightInPoints((short) 12);
        font.setFontName("Times New Roman");
        cellStyle.setFont(font);

        XSSFCellStyle cellStyleRow = xssfWorkbook.createCellStyle();
        cellStyleRow.setBorderLeft(XSSFCellStyle.BORDER_THIN);
        cellStyleRow.setBorderRight(XSSFCellStyle.BORDER_THIN);
        cellStyleRow.setBorderTop(XSSFCellStyle.BORDER_THIN);
        cellStyleRow.setBorderBottom(XSSFCellStyle.BORDER_THIN);
        cellStyleRow.setWrapText(true);
        cellStyleRow.setAlignment(CellStyle.ALIGN_CENTER);

        XSSFFont fontRow = xssfWorkbook.createFont();
        fontRow.setFontHeightInPoints((short) 12);
        fontRow.setFontName("Times New Roman");
        cellStyleRow.setFont(font);

        String fromTitle = sheet.getRow(0).getCell(0).toString();
        String fromTitle2 = sheet.getRow(1).getCell(1).toString();

        fromTitle = fromTitle.replace("startDate", countryRequest.getStarDate().toString());
        fromTitle = fromTitle.replace("endDate", countryRequest.getEndDate().toString());

        for (int i = 2; i <= 17; i++) {
            String date2 = sheet.getRow(2).getCell(i).toString();
            date2 = date2.replace("startYearDate", countryRequest.getStarDateYear().toString());
            date2 = date2.replace("endYearDate", countryRequest.getEndDateYear().toString());
            date2 = date2.replace("startDate", countryRequest.getStarDate().toString());
            date2 = date2.replace("endDate", countryRequest.getEndDate().toString());
            if (countryRequest.isImport()) {
                date2 = date2.replace("importexport2", "поступило с");
            } else {
                date2 = date2.replace("importexport2", "вывезено с");
            }
            sheet.getRow(2).getCell(i).setCellValue(date2);
        }

        for (int cellCount = 0; cellCount < countryRequest.getCountryRows().size(); cellCount++) {
            rowLast = sheet.getLastRowNum();
            XSSFRow row = sheet.createRow(rowLast + 1);
            CountryRow countryRow = countryRequest.getCountryRows().get(cellCount);
            XSSFCell number = row.createCell(0);
            number.setCellValue(cellCount + 1);
            number.setCellStyle(cellStyleRow);
            Import.createRowsImport(xssfWorkbook, row, cellStyle, cellStyleRow, countryRow.getRegions(), countryRow.getMassProduct(),
                    countryRow.getResCountryOrProduct(), 5);
        }

        rowLast = sheet.getLastRowNum();
        XSSFRow rowTotal = sheet.createRow(rowLast + 1);

        Import.createRowsImport(xssfWorkbook, rowTotal, cellStyle, cellStyleRow, totalMass.getRegions(), totalMass.getMassProduct(), "ИТОГО, тонн", 5);

        if (countryRequest.isImport()) {
            if (countryRequest.isProduct()) {
                fromTitle = fromTitle.replace("reqCountryOrProduct", countryRequest.getReqCountryOrProduct());
                fromTitle = fromTitle.replace("importexport", "поступлении в Республику Беларусь ");
                fromTitle2 = fromTitle2.replace("resCountryOrProduct", "Страна отправления");
                nameFile = "ImportProduct";
            } else {
                fromTitle = fromTitle.replace("reqCountryOrProduct", countryRequest.getReqCountryOrProduct());
                fromTitle = fromTitle.replace("importexport", "поступлении в Республику Беларусь из");
                fromTitle2 = fromTitle2.replace("resCountryOrProduct", "Наименование подкарантинной продукции");
                nameFile = "ImportCountry";
            }
            sheet.getRow(1).getCell(1).setCellValue(fromTitle2);
            sheet.getRow(0).getCell(0).setCellValue(fromTitle);
        } else {
            if (countryRequest.isProduct()) {
                fromTitle = fromTitle.replace("reqCountryOrProduct", countryRequest.getReqCountryOrProduct());
                fromTitle = fromTitle.replace("importexport", "вывозе из Республики Беларусь ");
                fromTitle2 = fromTitle2.replace("resCountryOrProduct", "Страна получатель");
                nameFile = "ExportProduсt";
            } else {
                fromTitle = fromTitle.replace("reqCountryOrProduct", countryRequest.getReqCountryOrProduct());
                fromTitle = fromTitle.replace("importexport", "вывозе из Республики Беларусь в ");
                fromTitle2 = fromTitle2.replace("resCountryOrProduct", "Наименование подкарантинной продукции");
                nameFile = "ExportCountry";
            }
        }

        sheet.getRow(1).getCell(1).setCellValue(fromTitle2);
        sheet.getRow(0).getCell(0).setCellValue(fromTitle);

        if (countryRequest.isFlowers()) {
            ReExport.createRowsMaterial(xssfWorkbook, countryRequest, cellStyle, cellStyleRow, 2);
        }

        if (countryRequest.getFss() != null) {
            Import.createRowsAllFss(xssfWorkbook, countryRequest, cellStyle, cellStyleRow, 2);
            Import.createRowsNameObl(xssfWorkbook, cellStyleRow);
            Import.createRowsFss2022(xssfWorkbook, countryRequest, cellStyle, cellStyleRow, 2);
        }

        if(!countryRequest.isWeekWeight())
            Import.removeColumn(sheet);

        File tempFile = File.createTempFile(nameFile, null);
        try (OutputStream fileOut = Files.newOutputStream(tempFile.toPath())) {
            xssfWorkbook.write(fileOut);
        }

        return ConBase64.convert(tempFile);
    }

    @PostMapping("/re-export")
    public String printReData(@RequestBody CountryReport countryRequest) throws Exception {

        log.debug("CountryReport " + countryRequest);
        TotalMass totalMass = new TotalMass(countryRequest);
        log.debug("TotalMass " + totalMass);

        InputStream is = getClass().getClassLoader().getResourceAsStream("BlancRe.xlsx");
        XSSFWorkbook xssfWorkbook = new XSSFWorkbook(is);

        XSSFCellStyle cellStyle = xssfWorkbook.createCellStyle();
        XSSFSheet sheet = xssfWorkbook.getSheetAt(0);
        cellStyle.setBorderLeft(XSSFCellStyle.BORDER_THIN);
        cellStyle.setBorderRight(XSSFCellStyle.BORDER_THIN);
        cellStyle.setBorderTop(XSSFCellStyle.BORDER_THIN);
        cellStyle.setBorderBottom(XSSFCellStyle.BORDER_THIN);
        cellStyle.setWrapText(true);

        XSSFFont font = xssfWorkbook.createFont();
        font.setFontHeightInPoints((short) 12);
        font.setFontName("Times New Roman");
        cellStyle.setFont(font);

        XSSFCellStyle cellStyleRow = xssfWorkbook.createCellStyle();
        cellStyleRow.setBorderLeft(XSSFCellStyle.BORDER_THIN);
        cellStyleRow.setBorderRight(XSSFCellStyle.BORDER_THIN);
        cellStyleRow.setBorderTop(XSSFCellStyle.BORDER_THIN);
        cellStyleRow.setBorderBottom(XSSFCellStyle.BORDER_THIN);
        cellStyleRow.setWrapText(true);
        cellStyleRow.setAlignment(CellStyle.ALIGN_CENTER);

        XSSFFont fontRow = xssfWorkbook.createFont();
        fontRow.setFontHeightInPoints((short) 12);
        fontRow.setFontName("Times New Roman");
        cellStyleRow.setFont(font);

        String fromTitle = sheet.getRow(0).getCell(0).toString();

        fromTitle = fromTitle.replace("startDate", countryRequest.getStarDate().toString());
        fromTitle = fromTitle.replace("endDate", countryRequest.getEndDate().toString());

        for (int i = 2; i <= 17; i++) {
            String date2 = sheet.getRow(2).getCell(i).toString();
            date2 = date2.replace("startYearDate", countryRequest.getStarDateYear().toString());
            date2 = date2.replace("endYearDate", countryRequest.getEndDateYear().toString());
            date2 = date2.replace("startDate", countryRequest.getStarDate().toString());
            date2 = date2.replace("endDate", countryRequest.getEndDate().toString());
            sheet.getRow(2).getCell(i).setCellValue(date2);
        }

        for (int cellCount = 0; cellCount < countryRequest.getCountryRows().size(); cellCount++) {
            rowLast = sheet.getLastRowNum();
            XSSFRow row = sheet.createRow(rowLast + 1);
            CountryRow countryRow = countryRequest.getCountryRows().get(cellCount);
            XSSFCell number = row.createCell(0);
            number.setCellValue(cellCount + 1);
            number.setCellStyle(cellStyleRow);
            Import.createRowsImport(xssfWorkbook, row, cellStyle, cellStyleRow, countryRow.getRegions(), countryRow.getMassProduct(), countryRow.getResCountryOrProduct(), 5);
        }

        rowLast = sheet.getLastRowNum();
        XSSFRow rowTotal = sheet.createRow(rowLast + 1);

        Import.createRowsImport(xssfWorkbook, rowTotal, cellStyle, cellStyleRow, totalMass.getRegions(), totalMass.getMassProduct(), "ИТОГО, тонн", 5);

        if (countryRequest.isProduct()) {
            fromTitle = fromTitle.replace("countryExport", countryRequest.getReqCountryOrProduct());
            nameFile = "ReExportInRF";
        } else {
            fromTitle = fromTitle.replace("countryExport", "в " + countryRequest.getReqCountryOrProduct() + "  подкарантинной продукции");
            nameFile = "ReExportAllCountry";
        }

        sheet.getRow(0).getCell(0).setCellValue(fromTitle);

        if (countryRequest.isFlowers()) {
            ReExport.createRowsMaterial(xssfWorkbook, countryRequest, cellStyle, cellStyleRow, 2);
        }

        Import.createRowsAllFss(xssfWorkbook, countryRequest, cellStyle, cellStyleRow, 2);
        Import.createRowsNameObl(xssfWorkbook, cellStyle);
        Import.createRowsFss2022(xssfWorkbook, countryRequest, cellStyle, cellStyleRow, 2);

        if (countryRequest.isProduct()) {
            sheet.getRow(1).getCell(1).setCellValue("Страна получатель");
        } else {
            sheet.getRow(1).getCell(1).setCellValue("Наименование подкарантинной продукции");
        }

        if(!countryRequest.isWeekWeight())
            Import.removeColumn(sheet);

        File tempFile = File.createTempFile(nameFile, null);

        try (OutputStream fileOut = Files.newOutputStream(tempFile.toPath())) {
            xssfWorkbook.write(fileOut);
        }

        return ConBase64.convert(tempFile);
    }

    @PostMapping("/tranzit")
    public String printTranData(@RequestBody CountryReport countryRequest) throws Exception {

        log.debug("CountryReport " + countryRequest);

        InputStream is = getClass().getClassLoader().getResourceAsStream("BlancTranzit.xlsx");
        XSSFWorkbook xssfWorkbook = new XSSFWorkbook(is);

        XSSFCellStyle cellStyle = xssfWorkbook.createCellStyle();
        cellStyle.setBorderLeft(XSSFCellStyle.BORDER_THIN);
        cellStyle.setBorderRight(XSSFCellStyle.BORDER_THIN);
        cellStyle.setBorderTop(XSSFCellStyle.BORDER_THIN);
        cellStyle.setBorderBottom(XSSFCellStyle.BORDER_THIN);
        cellStyle.setWrapText(true);

        XSSFFont font = xssfWorkbook.createFont();
        font.setFontHeightInPoints((short) 12);
        font.setFontName("Times New Roman");
        cellStyle.setFont(font);

        XSSFCellStyle cell_styl_obl = xssfWorkbook.createCellStyle();
        cell_styl_obl.setBorderLeft(XSSFCellStyle.BORDER_THIN);
        cell_styl_obl.setBorderRight(XSSFCellStyle.BORDER_THIN);
        cell_styl_obl.setBorderTop(XSSFCellStyle.BORDER_THIN);
        cell_styl_obl.setBorderBottom(XSSFCellStyle.BORDER_THIN);
        cell_styl_obl.setWrapText(true);//перенос слов

        XSSFFont font2 = xssfWorkbook.createFont();
        font2.setFontHeightInPoints((short) 12);
        font2.setFontName("Times New Roman");
        font2.setBold(true);
        cell_styl_obl.setFont(font2);

        XSSFCellStyle cellStyleRow = xssfWorkbook.createCellStyle();
        cellStyleRow.setBorderLeft(XSSFCellStyle.BORDER_THIN);
        cellStyleRow.setBorderRight(XSSFCellStyle.BORDER_THIN);
        cellStyleRow.setBorderTop(XSSFCellStyle.BORDER_THIN);
        cellStyleRow.setBorderBottom(XSSFCellStyle.BORDER_THIN);
        cellStyleRow.setWrapText(true);
        cellStyleRow.setAlignment(CellStyle.ALIGN_CENTER);

        XSSFFont fontRow = xssfWorkbook.createFont();
        fontRow.setFontHeightInPoints((short) 12);
        fontRow.setFontName("Times New Roman");
        cellStyleRow.setFont(font);

        XSSFSheet sheet = xssfWorkbook.getSheetAt(0);
        String fromTitle = sheet.getRow(0).getCell(0).toString();

        for (int i = 1; i <= 11; i++) {
            String date2 = sheet.getRow(2).getCell(i).toString();
            String date1 = sheet.getRow(1).getCell(0).toString();
            if (countryRequest.isTranzitEAEU()) {
                fromTitle = fromTitle.replace("contents", "ТРАНЗИТ В АДРЕС СТРАН ЕВРАЗИЙСКОГО ЭКОНОМИЧЕСКОГО СОЮЗА И ГОСУДАРСТВ - УЧАСТНИЦ СНГ");
                date1 = date1.replace("name", "Наименование страны-получателя подкарантинной продукции");
                date2 = date2.replace("amount1", "тыс. т");
                date2 = date2.replace("amount2", "тыс. пос. ед.");
                date2 = date2.replace("amount3", "тыс. шт.");
                date2 = date2.replace("amount4", "тыс. парт.");
                date2 = date2.replace("amount5", "тыс. пак.");
                date2 = date2.replace("amount6", "м3");
            } else {
                fromTitle = fromTitle.replace("contents", "ТРАНЗИТ ПОДКАРАНТИННОЙ ПРОДУКЦИИ");
                date1 = date1.replace("name", "Наименование пограничных пунктов");
                date2 = date2.replace("amount1", "тыс. т");
                date2 = date2.replace("amount2", "тыс. пос. ед.");
                date2 = date2.replace("amount3", "тыс. шт.");
                date2 = date2.replace("amount4", "тыс. парт.");
                date2 = date2.replace("amount5", "тыс. м2");
                date2 = date2.replace("amount6", "тыс. м3");
            }

            sheet.getRow(2).getCell(i).setCellValue(date2);
            sheet.getRow(1).getCell(0).setCellValue(date1);
            sheet.getRow(0).getCell(0).setCellValue(fromTitle);
        }

        for (int cellCount = 0; cellCount < countryRequest.getCountryRows().get(0).getRegions().size(); cellCount++) {
            if (countryRequest.isTranzitEAEU()) {
                nameFile = "TranzitEAEUandCIS";
                rowLast = sheet.getLastRowNum();
                XSSFRow row = sheet.createRow(rowLast + 1);
                CountryRow countryRow = countryRequest.getCountryRows().get(0);
                ElementRegion elementRegion = countryRow.getRegions().get(cellCount);
                Tranzit.create_obl(cellCount, xssfWorkbook, row, cellStyle, cell_styl_obl, cellStyleRow, countryRow.getRegions(), elementRegion.getNamePoints());
                //В том числе в страны ЕАЭС
                Tranzit.plus_eaeu(countryRequest, countryRow, xssfWorkbook, sheet, cellStyleRow, cell_styl_obl);
            } else {
                nameFile = "Tranzit";
                rowLast = sheet.getLastRowNum();
                XSSFRow row = sheet.createRow(rowLast + 1);
                CountryRow countryRow = countryRequest.getCountryRows().get(0);
                ElementRegion elementRegion = countryRow.getRegions().get(cellCount);
                Tranzit.create_obl(cellCount, xssfWorkbook, row, cellStyle, cell_styl_obl, cellStyleRow, countryRow.getRegions(), elementRegion.getNamePoints());
            }
        }

        if (!countryRequest.isTranzitEAEU()) {
            Tranzit.plusAll(xssfWorkbook, sheet, cellStyleRow,
                    Tranzit.summa_tonn2, Tranzit.summa_pos_ed2, Tranzit.summa_sht2, Tranzit.summa_part2, Tranzit.summa_m22,
                    Tranzit.summa_m32, Tranzit.summa_wagons2, Tranzit.summa_transport2, Tranzit.summa_container2,
                    Tranzit.summa_baggage2, Tranzit.summa_airplane2);
        }

        Tranzit.nullable();
        File tempFile = File.createTempFile(nameFile, null);

        try (OutputStream fileOut = Files.newOutputStream(tempFile.toPath())) {
            xssfWorkbook.write(fileOut);
        }

        return ConBase64.convert(tempFile);
    }

    @PostMapping("/sticker")
    public String createPdfSticker(@RequestBody Sticker sticker) {

        String nameFile = "ЭТИКЕТКА";
        File tempFile = null;
        String nameFile2 = "ПОДПИСЬ";
        File tempFile2 = null;
        try {
            tempFile = File.createTempFile(nameFile, null);
            tempFile2 = File.createTempFile(nameFile2, null);

            InputStream doc = getClass().getClassLoader().getResourceAsStream("StickerAll.docx");
            Document documentAll = new Document(doc);
            InputStream doc2 = getClass().getClassLoader().getResourceAsStream("StickerSignature.docx");
            Document documentSign = new Document(doc2);
            InputStream doc4 = getClass().getClassLoader().getResourceAsStream("StickerOne.docx");
            Document documentOne = new Document(doc4);


            if (sticker.getStickerProducts().size() > 1) {
                // Replace a specific text
                documentAll.replace("number", String.valueOf(sticker.getNumber()), true, true);
                documentAll.replace("name", "согласно приложению", true, true);
                documentAll.replace("weight", "согласно приложению", true, true);
                documentAll.replace("origin", "согласно приложению", true, true);
//                documentAll.replace("origin", sticker.getStickerProducts().get(0).getOrigin(), true, true);
                documentAll.replace("place", sticker.getPlace(), true, true);
                documentAll.replace("quantity", "согласно приложению", true, true);
                documentAll.replace("unit", "", true, true);
                documentAll.replace("recipient", sticker.getRecipient(), true, true);
                documentAll.replace("appointment", sticker.getAppointment().isEmpty()? "-----" : sticker.getAppointment(), true, true);
                documentAll.replace("area", "согласно приложению", true, true);
                documentAll.replace("external_sings", sticker.getExternal_sings().isEmpty()? "-----": sticker.getExternal_sings(), true, true);
                documentAll.replace("provisional_definition", sticker.getProvisional_definition().isEmpty()? "-----" : sticker.getProvisional_definition(), true, true);
                documentAll.replace("additional_info", "согласно приложению", true, true);
                documentAll.replace("seal_number", "согласно приложению", true, true);
                documentAll.replace("position", sticker.getPosition(), true, true);
                documentAll.replace("date", String.valueOf(sticker.getDate()), true, true);
                documentAll.replace("FIO1", sticker.getFio1(), true, true);
                documentAll.replace("FIO2", sticker.getFio2(), true, true);

                documentSign.replace("position", sticker.getPosition(), true, true);
                documentSign.replace("date", String.valueOf(sticker.getDate()), true, true);
                documentSign.replace("FIO1", sticker.getFio1(), true, true);
                documentSign.replace("FIO2", sticker.getFio2(), true, true);


                Section section = documentAll.addSection();
                String[] header = {"№\nп/п",
                        "Наименование подкарантинной продукции",
                        "Вес партии или площадь",
                        "Фитосанитарный сертификат",
                        "Чистый вес образца",
                        "Номер пломбы (сейф-пакета)",
                };

                String[] header2 = {"кол-во", "ед.изм"};

                Table table = section.addTable(true);
                table.resetCells(sticker.getStickerProducts().size() + 2, header.length);
                table.applyVerticalMerge(0, 0, 1);
                table.applyVerticalMerge(1, 0, 1);
                table.applyVerticalMerge(2, 0, 1);
                table.applyVerticalMerge(3, 0, 1);
                table.applyVerticalMerge(5, 0, 1);
                table.getRows().get(1).getCells().get(4).splitCell(2, 1);

                TableRow row = table.getRows().get(0);
                for (int i = 0; i < header.length; i++) {
                    row.getCells().get(i).getCellFormat().setVerticalAlignment(VerticalAlignment.Middle);
                    Paragraph p = row.getCells().get(i).addParagraph();
                    p.getFormat().setHorizontalAlignment(HorizontalAlignment.Center);
                    TextRange txtRange = p.appendText(header[i]);
                    txtRange.getCharacterFormat().setFontSize(11);
                    txtRange.getCharacterFormat().setFontName("Times New Roman");
                }

                row = table.getRows().get(1);
                Paragraph p = row.getCells().get(4).addParagraph();
                p.getFormat().setHorizontalAlignment(HorizontalAlignment.Center);
                TextRange txtRange = p.appendText(header2[0]);
                p = row.getCells().get(5).addParagraph();
                p.appendText(header2[1]);

                txtRange.getCharacterFormat().setFontSize(11);
                txtRange.getCharacterFormat().setFontName("Times New Roman");

                String text = "происхождением из страны:";
                for (int r = 0; r < sticker.getStickerProducts().size(); r++) {
                    TableRow dataRow = table.getRows().get(r + 2);
                    dataRow.getCells().get(0).addParagraph().appendText(String.valueOf(r + 1));
                    dataRow.getCells().get(0).setCellWidth(5f,CellWidthType.Percentage);

                    dataRow.setHeightType(TableRowHeightType.At_Least);
                    StickerProduct stickerProduct = sticker.getStickerProducts().get(r);
                    dataRow.getCells().get(1).addParagraph().appendText(stickerProduct.getName()+"/").getCharacterFormat().setFontSize(11);
                    dataRow.getCells().get(1).addParagraph().appendText(text);
                    dataRow.getCells().get(1).addParagraph().appendText(stickerProduct.getOrigin()).getCharacterFormat().setFontSize(11);
                    dataRow.getCells().get(1).setCellWidth(20f,CellWidthType.Percentage);

                    dataRow.getCells().get(2).addParagraph().appendText(stickerProduct.getWeight()).getCharacterFormat().setFontSize(11);
                    dataRow.getCells().get(2).setCellWidth(15f,CellWidthType.Percentage);

                    String additional_info = stickerProduct.getAdditional_info();
                    dataRow.getCells().get(3).addParagraph().appendText(additional_info.isEmpty()? "-----": additional_info).getCharacterFormat().setFontSize(11);
                    dataRow.getCells().get(3).setCellWidth(25f,CellWidthType.Percentage);

                    dataRow.getCells().get(4).splitCell(2, 1);
                    dataRow.getCells().get(4).addParagraph().appendText(stickerProduct.getQuantity()).getCharacterFormat().setFontSize(11);
                    dataRow.getCells().get(4).setCellWidth(10f,CellWidthType.Percentage);

                    dataRow.getCells().get(5).addParagraph().appendText(stickerProduct.getUnit()).getCharacterFormat().setFontSize(11);
                    dataRow.getCells().get(5).setCellWidth(10f,CellWidthType.Percentage);

                    dataRow.getCells().get(6).addParagraph().appendText(stickerProduct.getSeal_number()).getCharacterFormat().setFontSize(11);
                    dataRow.getCells().get(6).setCellWidth(20f,CellWidthType.Percentage);

                    table.autoFit(AutoFitBehaviorType.Auto_Fit_To_Contents);
                }

                documentAll.saveToFile(tempFile.getAbsolutePath(), FileFormat.Docx_2013);
                documentSign.saveToFile(tempFile2.getAbsolutePath(), FileFormat.Docx_2013);

                //нумерация страниц
                //get footer object of the first section
                HeaderFooter footer = documentAll.getSections().get(0).getHeadersFooters().getFooter();
                //add a paragraph to footer
                Paragraph footerParagraph = footer.addParagraph();
                footerParagraph.appendText("страница ");
                footerParagraph.appendField("page number", FieldType.Field_Page);
                footerParagraph.appendText(" из ");
                footerParagraph.appendField("number of pages", FieldType.Field_Num_Pages);
                footerParagraph.getFormat().setHorizontalAlignment(HorizontalAlignment.Right);

                if (documentAll.getSections().getCount() > 1) {
                    //loop through the sections except the first one
                    for (int i = 1; i < documentAll.getSections().getCount(); i++) {
                        //restart page numbering of the current section
                        documentAll.getSections().get(i).getPageSetup().setRestartPageNumbering(true);
                        //set the starting number to 1
                        documentAll.getSections().get(i).getPageSetup().setPageStartingNumber(1);
                    }
                }

                //save to file
                documentAll.insertTextFromFile(tempFile2.getAbsolutePath(), FileFormat.Docx_2013);
                documentAll.saveToFile(tempFile.getAbsolutePath(), FileFormat.Docx_2013);//

                Document doc3 = new Document(tempFile.getAbsolutePath(), FileFormat.Docx_2013);
                Section sec = doc3.getSections().get(0);
                int sections = doc3.getSections().getCount() - 1;
                for (int i = 0; i < sections; i++) {
                    Section section2 = doc3.getSections().get(1);
                    for (int j = 0; j < section2.getBody().getChildObjects().getCount(); j++) {
                        sec.getBody().getChildObjects().add(section2.getBody().getChildObjects().get(j).deepClone());
                    }
                    doc3.getSections().remove(section2);
                }
                String date= "";
                if(!sticker.isNew()) {
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
                    date = "СВЕДЕНИЯ ИЗ БАЗЫ ДАННЫХ \n АИС \"БЕЛФИТО\" "
                            + simpleDateFormat.format(new Date());
                }
                doc3.replace("new", date, true, true);

                doc3.saveToFile(tempFile.getAbsolutePath(), FileFormat.PDF);

            } else {

                documentOne.replace("number", String.valueOf(sticker.getNumber()), true, true);
                documentOne.replace("name", sticker.getStickerProducts().get(0).getName(), true, true);
                documentOne.replace("weight", sticker.getStickerProducts().get(0).getWeight(), true, true);
                documentOne.replace("origin", sticker.getStickerProducts().get(0).getOrigin(), true, true);
                documentOne.replace("place", sticker.getPlace(), true, true);
                documentOne.replace("quantity", sticker.getStickerProducts().get(0).getQuantity(), true, true);
                documentOne.replace("unit", sticker.getStickerProducts().get(0).getUnit(), true, true);
                documentOne.replace("recipient", sticker.getRecipient(), true, true);
                documentOne.replace("appointment", sticker.getAppointment().isEmpty()? "-----" : sticker.getAppointment(), true, true);
                documentOne.replace("area", String.valueOf(sticker.getArea()==0? "-----": sticker.getArea()), true, true);
                documentOne.replace("external_sings", sticker.getExternal_sings().isEmpty()? "-----": sticker.getExternal_sings(), true, true);
                documentOne.replace("provisional_definition", sticker.getProvisional_definition().isEmpty()? "-----" : sticker.getProvisional_definition(), true, true);
                documentOne.replace("additional_info", sticker.getStickerProducts().get(0).getAdditional_info().isEmpty()? "-----" : sticker.getStickerProducts().get(0).getAdditional_info(), true, true);
                documentOne.replace("seal_number", sticker.getStickerProducts().get(0).getSeal_number().isEmpty() ? "-----" : sticker.getStickerProducts().get(0).getSeal_number(), true, true);
                documentOne.replace("position", sticker.getPosition(), true, true);
                documentOne.replace("date", String.valueOf(sticker.getDate()), true, true);
                documentOne.replace("FIO1", sticker.getFio1(), true, true);
                documentOne.replace("FIO2", sticker.getFio2(), true, true);

                String date= "";
                if(!sticker.isNew()) {
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
                    date = "СВЕДЕНИЯ ИЗ БАЗЫ ДАННЫХ \n АИС \"БЕЛФИТО\" "
                            + simpleDateFormat.format(new Date());
                }
                documentOne.replace("new", date, true, true);
                documentOne.saveToFile(tempFile.getAbsolutePath(), FileFormat.PDF);
            }



//            documentAll.replace("new", date, true, true);
////            documentOne.saveToFile(tempFile.getAbsolutePath(), FileFormat.PDF);
//            if(sticker.getStickerProducts().size()>1)
//                documentAll.saveToFile(tempFile.getAbsolutePath(), FileFormat.PDF);
//            else
//                documentOne.saveToFile(tempFile.getAbsolutePath(), FileFormat.PDF);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return ConBase64.convert(tempFile);
    }

    @PostMapping("/conclusion")
    public String createPdfConclusion(@RequestBody Conclusion conclusion) {

        String nameFile = "ЗАКЛЮЧЕНИЕ";
        File tempFile = null;
        String nameFile2 = "ПОДПИСЬ";
        File tempFile2 = null;
        try {
            tempFile = File.createTempFile(nameFile, null);
            tempFile2 = File.createTempFile(nameFile2, null);


            InputStream doc = getClass().getClassLoader().getResourceAsStream("ConclusionAll.docx");
            Document documentAll = new Document(doc);
            InputStream doc2 = getClass().getClassLoader().getResourceAsStream("ConclusionSignature.docx");
            Document documentSign = new Document(doc2);
            InputStream doc4;
            if(conclusion.getNumber1().contains("b"))
            doc4 =  getClass().getClassLoader().getResourceAsStream("ConclusionOneBel.docx");
            else doc4 = getClass().getClassLoader().getResourceAsStream("ConclusionOne.docx");
            Document documentOne = new Document(doc4);


            if (conclusion.getConclusionProducts().size() > 1) {
                // Replace a specific text
                ConclusionProduct conclusionProduct = conclusion.getConclusionProducts().get(0);
                documentAll.replace("number1", String.valueOf(conclusion.getNumber1()), true, true);
                documentAll.replace("name_legal", conclusion.getName_legal(), true, true);
                documentAll.replace("date1", String.valueOf(conclusion.getDate1()), true, true);
                documentAll.replace("date2", conclusionProduct.getDate2(), true, true);
                documentAll.replace("date3", String.valueOf(conclusion.getDate3()), true, true);
                documentAll.replace("date4", String.valueOf(conclusion.getDate4()), true, true);
                documentAll.replace("number2", conclusionProduct.getNumber2(), true, true);
                documentAll.replace("number3", String.valueOf(conclusion.getNumber3()), true, true);
                documentAll.replace("issued", conclusion.getIssued(), true, true);
                documentAll.replace("name", "согласно приложению", true, true);
                documentAll.replace("weight", "", true, true);
                documentAll.replace("origin", "согласно приложению", true, true);
                documentAll.replace("place", conclusion.getPlace(), true, true);
                documentAll.replace("from_whos", conclusionProduct.getFrom_whos(), true, true);
                documentAll.replace("recipient", conclusion.getRecipient(), true, true);
                documentAll.replace("result", "согласно приложению", true, true);
                documentAll.replace("events", conclusion.getEvents(), true, true);
                documentAll.replace("FIO", conclusion.getFio(), true, true);

                documentSign.replace("FIO", conclusion.getFio(), true, true);


                Section section = documentAll.addSection();
                String[] header = {"№\nп/п",
                        "Наименование растительного материала",
                        "Фитосанитарный сертификат",
                        "Количество образцов",
                        "Результаты экспертизы",
                };

                Table table = section.addTable(true);
                table.resetCells(conclusion.getConclusionProducts().size() + 1, header.length);
                table.setDefaultRowHeight(20f);
                TableRow row = table.getRows().get(0);
                for (int i = 0; i < header.length; i++) {
                    TableCell tableCell = row.getCells().get(i);
                    tableCell.getCellFormat().setVerticalAlignment(VerticalAlignment.Middle);
                    Paragraph p = tableCell.addParagraph();
                    p.getFormat().setHorizontalAlignment(HorizontalAlignment.Center);
                    TextRange txtRange = p.appendText(header[i]);
                    txtRange.getCharacterFormat().setFontSize(11);
                    txtRange.getCharacterFormat().setFontName("Times New Roman");
                }


                String text = "происхождением из страны: ";
                String text3 = "выданный в стране: ";

                for (int r = 0; r < conclusion.getConclusionProducts().size(); r++) {
                    ConclusionProduct product = conclusion.getConclusionProducts().get(r);
                    TableRow dataRow = table.getRows().get(r + 1);
                    dataRow.setHeightType(TableRowHeightType.At_Least);
                    CellCollection cells = dataRow.getCells();
                    cells.get(0).addParagraph().appendText(String.valueOf(r + 1)).getCharacterFormat().setFontSize(11);
                    cells.get(0).setCellWidth(5f,CellWidthType.Percentage);

                    cells.get(1).addParagraph().appendText(product.getName()+"/ " + text).getCharacterFormat().setFontSize(11);
                    cells.get(1).addParagraph().appendText(product.getOrigin()).getCharacterFormat().setFontSize(11);
                    cells.get(1).setCellWidth(25f,CellWidthType.Percentage);

                    if(product.getFssNum().isEmpty() && product.getFssCountry().isEmpty() ){
                        cells.get(2).addParagraph().appendText("-----").getCharacterFormat().setFontSize(11);
                    }else {
                        cells.get(2).addParagraph().appendText(product.getFssNum() + ", " + text3 + product.getFssCountry()).getCharacterFormat().setFontSize(11);
                        cells.get(2).setCellWidth(30f, CellWidthType.Percentage);
                    }
                    cells.get(3).addParagraph().appendText(product.getWeight()).getCharacterFormat().setFontSize(11);
                    cells.get(3).setCellWidth(20f,CellWidthType.Percentage);
                    cells.get(4).addParagraph().appendText(product.getResult()).getCharacterFormat().setFontSize(11);
                    cells.get(4).setCellWidth(20f,CellWidthType.Percentage);

                    table.autoFit(AutoFitBehaviorType.Auto_Fit_To_Contents);

                }

                documentAll.saveToFile(tempFile.getAbsolutePath(), FileFormat.Docx_2013);
                documentSign.saveToFile(tempFile2.getAbsolutePath(), FileFormat.Docx_2013);

                //нумерация страниц
                //get footer object of the first section
                HeaderFooter footer = documentAll.getSections().get(0).getHeadersFooters().getFooter();
                //add a paragraph to footer
                Paragraph footerParagraph = footer.addParagraph();
                footerParagraph.appendText("страница ");
                footerParagraph.appendField("page number", FieldType.Field_Page);
                footerParagraph.appendText(" из ");
                footerParagraph.appendField("number of pages", FieldType.Field_Num_Pages);
                footerParagraph.getFormat().setHorizontalAlignment(HorizontalAlignment.Right);

                if (documentAll.getSections().getCount() > 1) {
                    //loop through the sections except the first one
                    for (int i = 1; i < documentAll.getSections().getCount(); i++) {
                        //restart page numbering of the current section
                        documentAll.getSections().get(i).getPageSetup().setRestartPageNumbering(true);
                        //set the starting number to 1
                        documentAll.getSections().get(i).getPageSetup().setPageStartingNumber(1);
                    }
                }

                //save to file
                documentAll.insertTextFromFile(tempFile2.getAbsolutePath(), FileFormat.Docx_2013);
                documentAll.saveToFile(tempFile.getAbsolutePath(), FileFormat.Docx_2013);//

                Document doc3 = new Document(tempFile.getAbsolutePath(), FileFormat.Docx_2013);
                Section sec = doc3.getSections().get(0);
                int sections = doc3.getSections().getCount() - 1;
                for (int i = 0; i < sections; i++) {
                    Section section2 = doc3.getSections().get(1);
                    for (int j = 0; j < section2.getBody().getChildObjects().getCount(); j++) {
                        sec.getBody().getChildObjects().add(section2.getBody().getChildObjects().get(j).deepClone());
                    }
                    doc3.getSections().remove(section2);
                }
                doc3.saveToFile(tempFile.getAbsolutePath(), FileFormat.PDF);

            } else {

                // Replace a specific text
                documentOne.replace("number1", String.valueOf(conclusion.getNumber1()), true, true);
                documentOne.replace("name_legal", conclusion.getName_legal(), true, true);
                documentOne.replace("date1", String.valueOf(conclusion.getDate1()), true, true);
                documentOne.replace("date2", "от " + String.valueOf(conclusion.getConclusionProducts().get(0).getDate2())+ "г.", true, true) ;
                documentOne.replace("date3", String.valueOf(conclusion.getDate3()), true, true);
                documentOne.replace("date4", String.valueOf(conclusion.getDate4()), true, true);
                documentOne.replace("number1", String.valueOf(conclusion.getNumber1()), true, true);
                documentOne.replace("number2", "№ " + String.valueOf(conclusion.getConclusionProducts().get(0).getNumber2()), true, true);
                documentOne.replace("number3", String.valueOf(conclusion.getNumber3()), true, true);
                documentOne.replace("issued", conclusion.getIssued(), true, true);
                documentOne.replace("name", conclusion.getConclusionProducts().get(0).getName(), true, true);
                documentOne.replace("weight", String.valueOf(conclusion.getConclusionProducts().get(0).getWeight()), true, true);
                documentOne.replace("origin", conclusion.getConclusionProducts().get(0).getOrigin(), true, true);
                documentOne.replace("place", conclusion.getPlace(), true, true);
                documentOne.replace("from_whos", conclusion.getConclusionProducts().get(0).getFrom_whos(), true, true);
                documentOne.replace("recipient", conclusion.getRecipient(), true, true);
                documentOne.replace("result", conclusion.getConclusionProducts().get(0).getResult(), true, true);
                documentOne.replace("events", conclusion.getEvents(), true, true);
                documentOne.replace("FIO", conclusion.getFio(), true, true);
                if(!conclusion.getNumber1().contains("b")){
                    documentOne.replace("fssNum",conclusion.getConclusionProducts().get(0).getFssNum(),true,true);
                    documentOne.replace("fssCountry",conclusion.getConclusionProducts().get(0).getFssCountry(),true,true);
                }
                //Save the result document
                String date= "";
                if(!conclusion.isNew()) {
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
                    date = "СВЕДЕНИЯ ИЗ БАЗЫ ДАННЫХ \n АИС \"БЕЛФИТО\" "
                            + simpleDateFormat.format(new Date());
                }
                documentOne.replace("new", date, true, true);
                //Save the result document
                documentOne.saveToFile(tempFile.getAbsolutePath(), FileFormat.PDF);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return ConBase64.convert(tempFile);
    }

    @PostMapping("/act-disinfection")
    public String createPdfActDecontamination(@RequestBody Disinfection disinfection) {

        String nameFile = "Акт обеззараживания";
        File tempFile = null;
        try {
            tempFile = File.createTempFile(nameFile, null);
            InputStream doc = getClass().getClassLoader().getResourceAsStream("ActDisinfection.docx");
            Document document = new Document(doc);

            // Replace a specific text

            document.replace("INSPECTION", String.valueOf(disinfection.getInspection()), true, true);
            document.replace("date1", String.valueOf(disinfection.getDate1()), true, true);
            document.replace("date2", String.valueOf(disinfection.getDate2()), true, true);
            document.replace("number", String.valueOf(disinfection.getNumber()), true, true);
            document.replace("name1", disinfection.getName1(), true, true);
            document.replace("name2", disinfection.getName2(), true, true);
//            document.replace("quantity", String.valueOf(disinfection.getQuantity()), true, true);
            document.replace("conclusion1", disinfection.getConclusion1(), true, true);
            document.replace("conclusion2", disinfection.getConclusion2(), true, true);
            document.replace("conclusion3", disinfection.getConclusion3(), true, true);
            document.replace("organization", disinfection.getOrganization(), true, true);
            document.replace("method_disinfection", disinfection.getMethod_disinfection(), true, true);
            document.replace("FIO1", disinfection.getFio1(), true, true);
            document.replace("FIO2", disinfection.getFio2(), true, true);
            document.replace("FIO3", disinfection.getFio3(), true, true);

            String date= "";
            if(!disinfection.isNew()) {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
                date = "СВЕДЕНИЯ ИЗ БАЗЫ ДАННЫХ \n АИС \"БЕЛФИТО\" "
                        + simpleDateFormat.format(new Date());
            }
            document.replace("new", date, true, true);
            document.saveToFile(tempFile.getAbsolutePath(), FileFormat.PDF);


        } catch (Exception e) {
            e.printStackTrace();
        }

        return ConBase64.convert(tempFile);
    }

    @PostMapping("/act-destruction")
    public String createPdfActDestruction(@RequestBody Destruction destruction) {

        String nameFile = "Акт об уничтожении";
        File tempFile = null;
        try {
            tempFile = File.createTempFile(nameFile, null);
            InputStream doc = getClass().getClassLoader().getResourceAsStream("ActDestruction.docx");
            Document document = new Document(doc);

            document.replace("number", String.valueOf(destruction.getNumber()), true, true);
            document.replace("date1", String.valueOf(destruction.getDate1()), true, true);
            document.replace("date2", String.valueOf(destruction.getDate2()), true, true);
            document.replace("method_destruction", destruction.getMethod_destruction(), true, true);
            document.replace("name", destruction.getName(), true, true);
            document.replace("quantity", "", true, true);
//            document.replace("quantity", String.valueOf(destruction.getQuantity()), true, true);
            document.replace("units", String.valueOf(destruction.getUnits()), true, true);
            document.replace("place", destruction.getPlace()==null? "-----" :  destruction.getPlace(), true, true);
            document.replace("position1", destruction.getPosition1(), true, true);
            document.replace("position2", destruction.getPosition2(), true, true);
            document.replace("position3", destruction.getPosition3(), true, true);
            document.replace("FIO1", destruction.getFio1(), true, true);
            document.replace("FIO2", destruction.getFio2(), true, true);
            document.replace("FIO3", destruction.getFio3(), true, true);
            String date= "";
            if(!destruction.isNew()) {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
                date = "СВЕДЕНИЯ ИЗ БАЗЫ ДАННЫХ \n АИС \"БЕЛФИТО\" "
                        + simpleDateFormat.format(new Date());
            }
            document.replace("new", date, true, true);
            //Save the result document
            document.saveToFile(tempFile.getAbsolutePath(), FileFormat.PDF);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return ConBase64.convert(tempFile);
    }

    @PostMapping("/act-refund")
    public String createPdfActReturn(@RequestBody Refund refund) {

        String nameFile = "Акт возврата";
        File tempFile = null;
        try {
            tempFile = File.createTempFile(nameFile, null);
            InputStream doc = getClass().getClassLoader().getResourceAsStream("ActRefund.docx");
            Document document = new Document(doc);

            document.replace("INSPECTION", String.valueOf(refund.getInspection()), true, true);
            document.replace("number", String.valueOf(refund.getNumber()), true, true);
            document.replace("date1", String.valueOf(refund.getDate1()), true, true);
            document.replace("date2", String.valueOf(refund.getDate2()), true, true);
            document.replace("place", refund.getPlace(), true, true);
            document.replace("name", refund.getName(), true, true);
//            document.replace("quantity", String.valueOf(refund.getQuantity()), true, true);
            document.replace("quantity", "", true, true);
            document.replace("units", String.valueOf(refund.getUnits()), true, true);

            document.replace("recipient", refund.getRecipient(), true, true);
            document.replace("place_sender", refund.getPlace_sender(), true, true);
            document.replace("number_TS", refund.getNumber_TS(), true, true);
            document.replace("numberFSS", refund.getNumberFSS(), true, true);
            document.replace("return_reasons", refund.getReturn_reasons(), true, true);
            document.replace("organizationFSS", refund.getOrganizationFSS(), true, true);
            document.replace("FIO1", refund.getFio1(), true, true);
            document.replace("FIO2", refund.getFio2(), true, true);
            document.replace("FIO3", refund.getFio3(), true, true);

            String date= "";
            if(!refund.isNew()) {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
                date = "СВЕДЕНИЯ ИЗ БАЗЫ ДАННЫХ \n АИС \"БЕЛФИТО\" "
                        + simpleDateFormat.format(new Date());
            }
            document.replace("new", date, true, true);
            document.saveToFile(tempFile.getAbsolutePath(), FileFormat.PDF);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return ConBase64.convert(tempFile);
    }


}