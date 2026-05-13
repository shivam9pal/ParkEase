package com.parkease.payment.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.format.DateTimeFormatter;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import com.parkease.payment.entity.Payment;
import com.parkease.payment.feign.dto.BookingDetailDto;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ReceiptGeneratorService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    /**
     * Generate receipt PDF and return as byte array (in-memory) Does NOT save
     * to local file system
     */
    public byte[] generateReceipt(Payment payment, BookingDetailDto booking) throws IOException {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float y = 760f;
                float margin = 40f;
                float contentWidth = 515f;

                // ═══════════════════════════════════════════════════════════════════════════════
                // HEADER SECTION
                // ═══════════════════════════════════════════════════════════════════════════════
                // Header background (RGB: 220, 53, 69)
                drawFilledRectangle(cs, margin, y - 50, contentWidth, 60, 0.86f, 0.21f, 0.27f);

                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 22);
                cs.setNonStrokingColor(1f, 1f, 1f); // White text (1.0, 1.0, 1.0)
                cs.beginText();
                cs.newLineAtOffset(margin + 20, y - 30);
                cs.showText("ParkEase Parking Receipt");
                cs.endText();

                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                cs.beginText();
                cs.newLineAtOffset(margin + 20, y - 45);
                cs.showText("Parking Receipt");
                cs.endText();

                cs.setNonStrokingColor(0f, 0f, 0f); // Back to black
                y -= 70;

                // ═══════════════════════════════════════════════════════════════════════════════
                // RECEIPT & BOOKING DETAILS SECTION
                // ═══════════════════════════════════════════════════════════════════════════════
                y = drawSectionHeader(cs, margin, y, "Receipt Information", 0.91f, 0.93f, 0.94f); // Light gray

                // Two-column layout
                float col1 = margin + 10;
                float col2 = margin + 260;
                float rowGap = 18f;

                y = drawFieldPair(cs, col1, y, col2, y,
                        "Receipt #", payment.getPaymentId().toString().substring(0, 8),
                        "Date", payment.getPaidAt() != null ? payment.getPaidAt().format(FMT) : "N/A");

                y = drawFieldPair(cs, col1, y - rowGap, col2, y - rowGap,
                        "Booking ID", payment.getBookingId().toString().substring(0, 8),
                        "Transaction", payment.getTransactionId() != null ? payment.getTransactionId().substring(0, 8) : "N/A");

                y -= 25;

                // ═══════════════════════════════════════════════════════════════════════════════
                // PARKING DETAILS SECTION
                // ═══════════════════════════════════════════════════════════════════════════════
                y = drawSectionHeader(cs, margin, y, "Parking Details", 0.91f, 0.93f, 0.94f); // Light gray

                y = drawFieldPair(cs, col1, y, col2, y,
                        "Driver ID", payment.getUserId().toString().substring(0, 8),
                        "Vehicle", booking.getVehiclePlate() + " (" + booking.getVehicleType() + ")");

                y = drawFieldPair(cs, col1, y - rowGap, col2, y - rowGap,
                        "Check-In", booking.getCheckInTime() != null ? booking.getCheckInTime().format(FMT) : "N/A",
                        "Check-Out", booking.getCheckOutTime() != null ? booking.getCheckOutTime().format(FMT) : "N/A");

                String duration = "N/A";
                if (booking.getCheckInTime() != null && booking.getCheckOutTime() != null) {
                    Duration d = Duration.between(booking.getCheckInTime(), booking.getCheckOutTime());
                    long hours = d.toHours();
                    int minutes = d.toMinutesPart();
                    duration = hours + "h " + minutes + "m";
                }

                y -= rowGap;
                y = drawSingleField(cs, col1, y, "Duration", duration);

                y -= 25;

                // ═══════════════════════════════════════════════════════════════════════════════
                // PAYMENT DETAILS SECTION (Highlighted)
                // ═══════════════════════════════════════════════════════════════════════════════
                y = drawSectionHeader(cs, margin, y, "Payment Details", 0.78f, 0.90f, 0.79f); // Light green

                y = drawFieldPair(cs, col1, y, col2, y,
                        "Payment Mode", payment.getMode() != null ? payment.getMode().name() : "N/A",
                        "Currency", payment.getCurrency());

                y -= rowGap;

                // Amount box highlight (RGB: 240, 248, 245)
                drawFilledRectangle(cs, col1 - 5, y - 35, 470, 40, 0.94f, 0.97f, 0.96f); // Very light cyan

                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 11);
                cs.setNonStrokingColor(0.39f, 0.39f, 0.39f); // Gray (100/255)
                cs.beginText();
                cs.newLineAtOffset(col1, y - 15);
                cs.showText("Amount Charged:");
                cs.endText();

                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 18);
                cs.setNonStrokingColor(0.16f, 0.65f, 0.27f); // Green for amount (40/255, 167/255, 69/255)
                cs.beginText();
                cs.newLineAtOffset(col1 + 250, y - 20);
                cs.showText("Rs. " + payment.getAmount());
                cs.endText();

                cs.setNonStrokingColor(0.39f, 0.39f, 0.39f); // Gray
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                cs.beginText();
                cs.newLineAtOffset(col1 + 250, y - 30);
                cs.showText(payment.getStatus() != null ? payment.getStatus().name() : "N/A");
                cs.endText();

                cs.setNonStrokingColor(0f, 0f, 0f);

                y -= 50;

                // ═══════════════════════════════════════════════════════════════════════════════
                // FOOTER
                // ═══════════════════════════════════════════════════════════════════════════════
                // Divider line (RGB: 200, 200, 200)
                drawLine(cs, margin, y, margin + contentWidth, y, 1f, 0.78f, 0.78f, 0.78f);

                y -= 15;

                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 9);
                cs.setNonStrokingColor(0.59f, 0.59f, 0.59f); // Gray (150/255)
                cs.beginText();
                cs.newLineAtOffset(margin, y);
                cs.showText("Thank you for using ParkEase! Drive safely.");
                cs.endText();

                y -= 12;
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 8);
                cs.beginText();
                cs.newLineAtOffset(margin, y);
                cs.showText("This is an automated receipt. For support, contact support@parkease.com");
                cs.endText();
            }

            doc.save(baos);
            byte[] pdfBytes = baos.toByteArray();
            log.info("Receipt generated in-memory as bytes. Size: {} bytes", pdfBytes.length);
            return pdfBytes;

        } catch (IOException e) {
            log.error("PDF generation failed for paymentId={}: {}", payment.getPaymentId(), e.getMessage(), e);
            throw new RuntimeException("Receipt generation failed");
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────
    /**
     * Draw a section header with background color (RGB values as 0-1 floats)
     */
    private float drawSectionHeader(PDPageContentStream cs, float x, float y, String title,
            float r, float g, float b) throws IOException {
        // Background rectangle
        drawFilledRectangle(cs, x, y - 20, 515, 20, r, g, b);

        // Title text
        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 12);
        cs.setNonStrokingColor(0.20f, 0.20f, 0.20f); // Dark gray
        cs.beginText();
        cs.newLineAtOffset(x + 10, y - 16);
        cs.showText(title);
        cs.endText();

        return y - 30;
    }

    /**
     * Draw two fields side by side (two columns)
     */
    private float drawFieldPair(PDPageContentStream cs, float x1, float y1, float x2, float y2,
            String label1, String value1, String label2, String value2) throws IOException {

        // Left column
        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 10);
        cs.setNonStrokingColor(0.39f, 0.39f, 0.39f); // Gray (100/255)
        cs.beginText();
        cs.newLineAtOffset(x1, y1);
        cs.showText(label1);
        cs.endText();

        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
        cs.setNonStrokingColor(0.16f, 0.16f, 0.16f); // Dark gray (40/255)
        cs.beginText();
        cs.newLineAtOffset(x1, y1 - 12);
        cs.showText(value1 != null ? value1 : "");
        cs.endText();

        // Right column
        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 10);
        cs.setNonStrokingColor(0.39f, 0.39f, 0.39f); // Gray (100/255)
        cs.beginText();
        cs.newLineAtOffset(x2, y2);
        cs.showText(label2);
        cs.endText();

        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
        cs.setNonStrokingColor(0.16f, 0.16f, 0.16f); // Dark gray (40/255)
        cs.beginText();
        cs.newLineAtOffset(x2, y2 - 12);
        cs.showText(value2 != null ? value2 : "");
        cs.endText();

        return y1 - 25;
    }

    /**
     * Draw a single field
     */
    private float drawSingleField(PDPageContentStream cs, float x, float y, String label, String value) throws IOException {
        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 10);
        cs.setNonStrokingColor(0.39f, 0.39f, 0.39f); // Gray (100/255)
        cs.beginText();
        cs.newLineAtOffset(x, y);
        cs.showText(label);
        cs.endText();

        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
        cs.setNonStrokingColor(0.16f, 0.16f, 0.16f); // Dark gray (40/255)
        cs.beginText();
        cs.newLineAtOffset(x, y - 12);
        cs.showText(value != null ? value : "");
        cs.endText();

        return y - 25;
    }

    /**
     * Draw a filled rectangle with background color (RGB values as 0-1 floats)
     */
    private void drawFilledRectangle(PDPageContentStream cs, float x, float y, float width, float height,
            float r, float g, float b) throws IOException {
        cs.setNonStrokingColor(r, g, b);
        cs.addRect(x, y, width, height);
        cs.fill();
        cs.setNonStrokingColor(0f, 0f, 0f); // Reset to black
    }

    /**
     * Draw a line with custom color and width (RGB values as 0-1 floats)
     */
    private void drawLine(PDPageContentStream cs, float x1, float y, float x2, float y2, float width,
            float r, float g, float b) throws IOException {
        cs.setLineWidth(width);
        cs.setStrokingColor(r, g, b);
        cs.moveTo(x1, y);
        cs.lineTo(x2, y2);
        cs.stroke();
        cs.setStrokingColor(0f, 0f, 0f); // Reset to black
    }

    /**
     * Draw a simple line (legacy support)
     */
    private void drawLine(PDPageContentStream cs, float x1, float y, float x2) throws IOException {
        cs.setLineWidth(0.5f);
        cs.setStrokingColor(0, 0, 0);
        cs.moveTo(x1, y);
        cs.lineTo(x2, y);
        cs.stroke();
    }

    /**
     * Old helper method (deprecated - kept for reference)
     */
    @Deprecated
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
}
