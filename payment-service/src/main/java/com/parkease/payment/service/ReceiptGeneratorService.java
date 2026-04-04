package com.parkease.payment.service;

import com.parkease.payment.entity.Payment;
import com.parkease.payment.feign.dto.BookingDetailDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class ReceiptGeneratorService {

    @Value("${receipt.storage.path:receipts/}")
    private String receiptStoragePath;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    public String generateReceipt(Payment payment, BookingDetailDto booking) throws IOException {
        Files.createDirectories(Paths.get(receiptStoragePath));
        String filePath = receiptStoragePath + payment.getPaymentId() + ".pdf";

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float y      = 750f;
                float margin = 50f;
                float gap    = 20f;

                // Title
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 16);
                cs.beginText();
                cs.newLineAtOffset(margin, y);
                cs.showText("ParkEase - Parking Receipt");
                cs.endText();
                y -= gap;

                drawLine(cs, margin, y, 545f);
                y -= gap;

                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11);

                y = line(cs, margin, y, gap, "Receipt #:      ", payment.getPaymentId().toString());
                y = line(cs, margin, y, gap, "Booking ID:     ", payment.getBookingId().toString());
                y = line(cs, margin, y, gap, "Transaction ID: ",
                        payment.getTransactionId() != null ? payment.getTransactionId() : "N/A");
                y = line(cs, margin, y, gap, "Date:           ",
                        payment.getPaidAt() != null ? payment.getPaidAt().format(FMT) : "N/A");

                y -= gap / 2;
                y = line(cs, margin, y, gap, "Driver ID:      ", payment.getUserId().toString());
                y = line(cs, margin, y, gap, "Vehicle:        ",
                        booking.getVehiclePlate() + " (" + booking.getVehicleType() + ")");

                y -= gap / 2;
                y = line(cs, margin, y, gap, "Check-In:       ",
                        booking.getCheckInTime() != null ? booking.getCheckInTime().format(FMT) : "N/A");
                y = line(cs, margin, y, gap, "Check-Out:      ",
                        booking.getCheckOutTime() != null ? booking.getCheckOutTime().format(FMT) : "N/A");

                String duration = "N/A";
                if (booking.getCheckInTime() != null && booking.getCheckOutTime() != null) {
                    Duration d = Duration.between(booking.getCheckInTime(), booking.getCheckOutTime());
                    duration = d.toHours() + " hours " + d.toMinutesPart() + " minutes";
                }
                y = line(cs, margin, y, gap, "Duration:       ", duration);

                y -= gap / 2;

                y = line(cs, margin, y, gap, "Payment Mode:   ",
                        payment.getMode() != null ? payment.getMode().name() : "N/A");

                y = line(cs, margin, y, gap, "Amount Charged: ",
                        "\u20B9" + payment.getAmount());

                y = line(cs, margin, y, gap, "Currency:       ",
                        payment.getCurrency());

                y = line(cs, margin, y, gap, "Status:         ",
                        payment.getStatus() != null ? payment.getStatus().name() : "N/A");
                y -= gap / 2;
                drawLine(cs, margin, y, 545f);
                y -= gap;

                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 11);
                cs.beginText();
                cs.newLineAtOffset(margin, y);
                cs.showText("Thank you for using ParkEase!");
                cs.endText();
            }

            doc.save(filePath);
            log.info("Receipt generated at: {}", filePath);
            return filePath;

        } catch (IOException e) {
            log.error("PDF generation failed for paymentId={}: {}", payment.getPaymentId(), e.getMessage(), e);
            throw new RuntimeException("Receipt generation failed");
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private float line(PDPageContentStream cs, float x, float y, float gap,
                       String label, String value) throws IOException {
        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 11);
        cs.beginText();
        cs.newLineAtOffset(x, y);
        cs.showText(label);
        cs.endText();

        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11);
        cs.beginText();
        cs.newLineAtOffset(x + 130, y);
        cs.showText(value != null ? value : "");
        cs.endText();

        return y - gap;
    }

    private void drawLine(PDPageContentStream cs, float x1, float y, float x2) throws IOException {
        cs.setLineWidth(0.5f);
        cs.moveTo(x1, y);
        cs.lineTo(x2, y);
        cs.stroke();
    }
}