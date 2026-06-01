package com.library.system.sip2;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Sip2Message {

    private static final DateTimeFormatter SIP_DATE = DateTimeFormatter.ofPattern("yyyyMMdd    HHmmss");

    private String code;
    private String institutionId;
    private String patronIdentifier;
    private String itemIdentifier;
    private String terminalPassword;
    private String patronPassword;
    private String feeAcknowledged;
    private String cancel;
    private String transactionDate;
    private String returnDate;
    private String nbDueDate;
    private String screenMessage;
    private String printLine;
    private String ok;
    private String renewOk;
    private String magneticMedia;
    private String desensitize;
    private String feeType;
    private String securityInhibit;
    private String mediaType;
    private String itemProperties;
    private String sequence;
    private String checksum;

    public String getTransactionDate() {
        return transactionDate != null ? transactionDate : LocalDateTime.now().format(SIP_DATE);
    }

    public static Sip2Message parse(String raw) {
        Sip2Message msg = new Sip2Message();
        msg.setCode(raw.substring(0, 2));

        switch (msg.getCode()) {
            case "23" -> parsePatronStatus(msg, raw);
            case "35" -> parseEndSession(msg, raw);
            case "63" -> parsePatronInformation(msg, raw);
            case "09" -> parseCheckin(msg, raw);
            case "29" -> parseCheckout(msg, raw);
            case "17" -> parseItemInformation(msg, raw);
            case "93" -> parseScStatus(msg, raw);
            default -> {}
        }
        return msg;
    }

    private static void parsePatronStatus(Sip2Message msg, String raw) {
        msg.setTransactionDate(extract(raw, 2, 18));
        msg.setInstitutionId(extractVar(raw, "AO"));
        msg.setPatronIdentifier(extractVar(raw, "AA"));
        msg.setTerminalPassword(extractVar(raw, "AC"));
        msg.setPatronPassword(extractVar(raw, "AD"));
    }

    private static void parseEndSession(Sip2Message msg, String raw) {
        msg.setTransactionDate(extract(raw, 2, 18));
        msg.setInstitutionId(extractVar(raw, "AO"));
        msg.setPatronIdentifier(extractVar(raw, "AA"));
        msg.setTerminalPassword(extractVar(raw, "AC"));
        msg.setPatronPassword(extractVar(raw, "AD"));
    }

    private static void parsePatronInformation(Sip2Message msg, String raw) {
        msg.setTransactionDate(extract(raw, 2, 18));
        msg.setInstitutionId(extractVar(raw, "AO"));
        msg.setPatronIdentifier(extractVar(raw, "AA"));
        msg.setTerminalPassword(extractVar(raw, "AC"));
        msg.setPatronPassword(extractVar(raw, "AD"));
    }

    private static void parseCheckin(Sip2Message msg, String raw) {
        msg.setTransactionDate(extract(raw, 2, 18));
        msg.setReturnDate(extract(raw, 20, 18));
        msg.setInstitutionId(extractVar(raw, "AO"));
        msg.setItemIdentifier(extractVar(raw, "AB"));
        msg.setTerminalPassword(extractVar(raw, "AC"));
        msg.setItemProperties(extractVar(raw, "CH"));
        msg.setCancel(extractVar(raw, "BI"));
    }

    private static void parseCheckout(Sip2Message msg, String raw) {
        msg.setTransactionDate(extract(raw, 2, 18));
        msg.setNbDueDate(extract(raw, 20, 18));
        msg.setInstitutionId(extractVar(raw, "AO"));
        msg.setPatronIdentifier(extractVar(raw, "AA"));
        msg.setItemIdentifier(extractVar(raw, "AB"));
        msg.setTerminalPassword(extractVar(raw, "AC"));
        msg.setPatronPassword(extractVar(raw, "AD"));
        msg.setItemProperties(extractVar(raw, "CH"));
        msg.setFeeAcknowledged(extractVar(raw, "BO"));
        msg.setCancel(extractVar(raw, "BI"));
        msg.setMagneticMedia(extractVar(raw, "BV"));
    }

    private static void parseItemInformation(Sip2Message msg, String raw) {
        msg.setTransactionDate(extract(raw, 2, 18));
        msg.setInstitutionId(extractVar(raw, "AO"));
        msg.setItemIdentifier(extractVar(raw, "AB"));
        msg.setTerminalPassword(extractVar(raw, "AC"));
    }

    private static void parseScStatus(Sip2Message msg, String raw) {
        msg.setTransactionDate(extract(raw, 2, 18));
        msg.setInstitutionId(extractVar(raw, "AO"));
    }

    private static String extract(String raw, int start, int length) {
        if (raw.length() >= start + length) {
            return raw.substring(start, start + length);
        }
        return "";
    }

    private static String extractVar(String raw, String tag) {
        int idx = raw.indexOf(tag);
        if (idx < 0) return "";
        int start = idx + tag.length();
        int end = start;
        while (end < raw.length() && raw.charAt(end) != '|' && raw.charAt(end) != '\r') {
            end++;
        }
        return raw.substring(start, end);
    }

    public String buildAcsStatus() {
        return "98" + getTransactionDate() + "1" + "1" + "1" + "1" + "1"
                + "1" + "1" + "1" + "1" + "1" + "1" + "1" + "1"
                + "AO" + (institutionId != null ? institutionId : "LIB001") + "|"
                + "AM" + "Library System|"
                + "BX" + "YYYYMMDDHHMMSS|"
                + "\r";
    }

    public String buildCheckoutResponse(boolean success, String title, String dueDate) {
        return "29" + getTransactionDate()
                + (success ? "1" : "0")
                + (success ? "1" : "0")
                + "N" + "N"
                + dueDate
                + "AA" + (patronIdentifier != null ? patronIdentifier : "") + "|"
                + "AB" + (itemIdentifier != null ? itemIdentifier : "") + "|"
                + "AJ" + (title != null ? title : "") + "|"
                + "AH" + dueDate + "|"
                + (success ? "" : "AF" + "Checkout failed|")
                + "\r";
    }

    public String buildCheckinResponse(boolean success, String title) {
        return "09" + getTransactionDate()
                + getTransactionDate()
                + (success ? "1" : "0")
                + "N" + "N"
                + "AB" + (itemIdentifier != null ? itemIdentifier : "") + "|"
                + "AJ" + (title != null ? title : "") + "|"
                + (success ? "" : "AF" + "Checkin failed|")
                + "\r";
    }

    public String buildPatronStatusResponse(boolean valid, String name, int borrowCount) {
        return "24" + getTransactionDate()
                + (valid ? "000" : "Y00")
                + "AE" + (name != null ? name : "") + "|"
                + "AA" + (patronIdentifier != null ? patronIdentifier : "") + "|"
                + "BL" + (valid ? "Y" : "N") + "|"
                + "CQ" + (valid ? "Y" : "N") + "|"
                + "\r";
    }

    public String buildItemInformationResponse(boolean available, String title, String author) {
        return "18" + getTransactionDate()
                + (available ? "1" : "0")
                + "01" + "003"
                + "AB" + (itemIdentifier != null ? itemIdentifier : "") + "|"
                + "AJ" + (title != null ? title : "") + "|"
                + "BD" + (author != null ? author : "") + "|"
                + "\r";
    }
}
