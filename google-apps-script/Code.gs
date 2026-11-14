/**
 * Pixelabs Calculator - Google Sheet Feedback Webhook & Auto-Formatter
 * - Automatic 3-Month (90-Day) Retention & Auto-Purge
 * - Professional Sheet Formatting (Frozen Header, Alternating Rows, Filter, Badges)
 */

function doPost(e) {
  try {
    var ss = SpreadsheetApp.getActiveSpreadsheet();
    var sheet = ss.getActiveSheet() || ss.getSheets()[0];
    var data = JSON.parse(e.postData.contents);
    
    // Receipt timestamp: use client timestamp or current server time
    var timestamp = data.timestamp || Utilities.formatDate(new Date(), Session.getScriptTimeZone() || "GMT", "yyyy-MM-dd HH:mm:ss");
    
    // Ensure header row exists before appending
    if (sheet.getLastRow() === 0) {
      formatFeedbackSheet();
    }

    // Append new feedback row (maintaining original 9-column structure)
    sheet.appendRow([
      timestamp,
      data.category || "Feedback",
      data.title || "User Feedback",
      data.description || "",
      data.email || "",
      data.deviceModel || "N/A",
      data.androidVersion || "N/A",
      data.appVersion || "N/A",
      data.angleMode || "N/A"
    ]);

    // Automatically purge rows older than 3 months (90 days)
    deleteFeedbackOlderThanThreeMonths(sheet);

    return ContentService.createTextOutput(JSON.stringify({
      "status": "success",
      "message": "Feedback recorded successfully"
    })).setMimeType(ContentService.MimeType.JSON);

  } catch (error) {
    return ContentService.createTextOutput(JSON.stringify({
      "status": "error",
      "message": error.toString()
    })).setMimeType(ContentService.MimeType.JSON);
  }
}

/**
 * Webhook GET handler: runs formatFeedbackSheet() and returns status
 */
function doGet(e) {
  try {
    var msg = formatFeedbackSheet();
    return ContentService.createTextOutput(JSON.stringify({
      "status": "success",
      "message": msg
    })).setMimeType(ContentService.MimeType.JSON);
  } catch (err) {
    return ContentService.createTextOutput(JSON.stringify({
      "status": "error",
      "message": err.toString()
    })).setMimeType(ContentService.MimeType.JSON);
  }
}

/**
 * Formats the Calculator Feedback sheet into a clean, modern table interface:
 * 1. Frozen header row (Timestamp, Type, Title, Description, Email, Device, OS Version, App Version, Mode)
 * 2. Elegant dark Material 3 palette for header (#25183E, white bold text)
 * 3. Text wrapping enabled on all data columns
 * 4. Auto-resized columns so nothing is truncated
 * 5. Alternating row banding with clean dark/light friendly palette
 * 6. Filter view enabled across all 9 columns
 * 7. Conditional formatting badges for 'Bug Report', 'Suggestion', 'Feedback', 'Calculation'
 */
function formatFeedbackSheet() {
  var ss = SpreadsheetApp.getActiveSpreadsheet();
  var sheet = ss.getActiveSheet() || ss.getSheets()[0];
  
  var headers = [
    "Timestamp", 
    "Type", 
    "Title", 
    "Description", 
    "Email", 
    "Device", 
    "OS Version", 
    "App Version", 
    "Mode"
  ];
  
  // 1. Ensure header row 1 is populated (insert row before row 1 if data row exists)
  var firstCell = sheet.getRange(1, 1).getValue();
  if (firstCell !== "Timestamp") {
    sheet.insertRowBefore(1);
  }
  sheet.getRange(1, 1, 1, headers.length).setValues([headers]);
  
  // Freeze header row
  sheet.setFrozenRows(1);
  
  // Header row styling: bold, white text, dark palette, vertical centered
  var headerRange = sheet.getRange(1, 1, 1, headers.length);
  headerRange.setFontWeight("bold")
             .setFontColor("#FFFFFF")
             .setBackground("#25183E")
             .setHorizontalAlignment("center")
             .setVerticalAlignment("middle")
             .setFontSize(10.5)
             .setFontFamily("Roboto");
  sheet.setRowHeight(1, 38);
  
  // 2. Enable text wrapping on all data rows
  var lastRow = Math.max(sheet.getLastRow(), 2);
  sheet.getRange(1, 1, lastRow, headers.length)
       .setWrapStrategy(SpreadsheetApp.WrapStrategy.WRAP)
       .setVerticalAlignment("middle");
  
  // 3. Auto-resize all columns, then apply generous minimum widths
  for (var col = 1; col <= headers.length; col++) {
    sheet.autoResizeColumn(col);
  }
  if (sheet.getColumnWidth(1) < 165) sheet.setColumnWidth(1, 165); // Timestamp
  if (sheet.getColumnWidth(2) < 140) sheet.setColumnWidth(2, 140); // Type
  if (sheet.getColumnWidth(3) < 180) sheet.setColumnWidth(3, 200); // Title
  if (sheet.getColumnWidth(4) < 280) sheet.setColumnWidth(4, 320); // Description
  if (sheet.getColumnWidth(5) < 180) sheet.setColumnWidth(5, 190); // Email
  if (sheet.getColumnWidth(6) < 150) sheet.setColumnWidth(6, 160); // Device
  if (sheet.getColumnWidth(7) < 150) sheet.setColumnWidth(7, 160); // OS Version
  if (sheet.getColumnWidth(8) < 110) sheet.setColumnWidth(8, 110); // App Version
  if (sheet.getColumnWidth(9) < 90)  sheet.setColumnWidth(9, 90);   // Mode
  
  // 4. Alternating row banding
  var existingBandings = sheet.getBandings();
  for (var b = 0; b < existingBandings.length; b++) {
    existingBandings[b].remove();
  }
  
  var bandingRange = sheet.getRange(1, 1, Math.max(lastRow, 30), headers.length);
  var banding = bandingRange.applyRowBanding(SpreadsheetApp.BandingTheme.LIGHT_GREY, true, false);
  banding.setHeaderRowColor("#25183E")
         .setFirstRowColor("#FFFFFF")
         .setSecondRowColor("#F6F3FA");
         
  // 5. Add filter view if not already present
  if (sheet.getFilter() === null) {
    sheet.getRange(1, 1, Math.max(lastRow, 1), headers.length).createFilter();
  }
  
  // 6. Conditional formatting badges for Column B (Type)
  sheet.clearConditionalFormatRules();
  var rules = [];
  var typeRange = sheet.getRange(2, 2, Math.max(lastRow - 1, 1), 1);
  
  // Bug Report: Soft Coral / Red badge
  rules.push(SpreadsheetApp.newConditionalFormatRule()
    .whenTextContains("Bug Report")
    .setBackground("#FFEBEE")
    .setFontColor("#C62828")
    .setBold(true)
    .setRanges([typeRange])
    .build());
    
  // Suggestion: Soft Amber / Yellow badge
  rules.push(SpreadsheetApp.newConditionalFormatRule()
    .whenTextContains("Suggestion")
    .setBackground("#FFF8E1")
    .setFontColor("#E65100")
    .setBold(true)
    .setRanges([typeRange])
    .build());
    
  // Feedback: Soft Teal / Blue badge
  rules.push(SpreadsheetApp.newConditionalFormatRule()
    .whenTextContains("Feedback")
    .setBackground("#E0F2F1")
    .setFontColor("#00695C")
    .setBold(true)
    .setRanges([typeRange])
    .build());

  // Calculation: Soft Lavender / Purple badge
  rules.push(SpreadsheetApp.newConditionalFormatRule()
    .whenTextContains("Calculation")
    .setBackground("#EDE7F6")
    .setFontColor("#4527A0")
    .setBold(true)
    .setRanges([typeRange])
    .build());

  sheet.setConditionalFormatRules(rules);
  return "Sheet formatted successfully with frozen header, wrapped text, auto-resized columns, banding, filters, and badges";
}

/**
 * Automatically creates a custom menu in Google Sheets
 */
function onOpen() {
  SpreadsheetApp.getUi()
    .createMenu("Calculator Tools")
    .addItem("Format Feedback Sheet", "formatFeedbackSheet")
    .addItem("Purge Records Older Than 90 Days", "deleteFeedbackOlderThanThreeMonths")
    .addToUi();
}

/**
 * Automatically deletes feedback entries older than 3 months (90 days)
 * calculated from the received date in Column A.
 */
function deleteFeedbackOlderThanThreeMonths(sheet) {
  if (!sheet) {
    var ss = SpreadsheetApp.getActiveSpreadsheet();
    sheet = ss.getActiveSheet() || ss.getSheets()[0];
  }
  
  var lastRow = sheet.getLastRow();
  if (lastRow <= 1) return;

  var now = new Date().getTime();
  var cutoff = now - (90 * 24 * 60 * 60 * 1000); // 90 days in ms

  // Column A timestamps from row 2
  var values = sheet.getRange(2, 1, lastRow - 1, 1).getValues();

  // Iterate backwards from bottom to top
  for (var i = values.length - 1; i >= 0; i--) {
    var raw = values[i][0];
    if (!raw) continue;
    
    var rowDate = new Date(raw);
    if (isNaN(rowDate.getTime()) && typeof raw === "string") {
      rowDate = new Date(raw.replace(/-/g, "/"));
    }

    if (!isNaN(rowDate.getTime()) && rowDate.getTime() < cutoff) {
      sheet.deleteRow(i + 2);
    }
  }
}

/**
 * Daily trigger handler: purges records older than 3 months
 */
function autoDeleteDailyCleanupTrigger() {
  deleteFeedbackOlderThanThreeMonths();
}

/**
 * One-time setup: installs the automated daily trigger (2:00 AM)
 */
function setupDailyAutoDeleteTrigger() {
  var triggers = ScriptApp.getProjectTriggers();
  for (var i = 0; i < triggers.length; i++) {
    if (triggers[i].getHandlerFunction() === "autoDeleteDailyCleanupTrigger") {
      ScriptApp.deleteTrigger(triggers[i]);
    }
  }

  ScriptApp.newTrigger("autoDeleteDailyCleanupTrigger")
    .timeBased()
    .everyDays(1)
    .atHour(2)
    .create();

  Logger.log("Daily auto-delete trigger installed successfully (runs daily at 2:00 AM).");
}
