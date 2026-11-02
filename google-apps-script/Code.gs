/**
 * Pixelabs Calculator - Google Sheet Feedback Webhook
 * Automatic 3-Month (90-Day) Retention & Auto-Purge
 */

function doPost(e) {
  try {
    var ss = SpreadsheetApp.getActiveSpreadsheet();
    var sheet = ss.getActiveSheet() || ss.getSheets()[0];
    var data = JSON.parse(e.postData.contents);
    
    // Receipt timestamp: use client timestamp or current server time
    var timestamp = data.timestamp || Utilities.formatDate(new Date(), Session.getScriptTimeZone() || "GMT", "yyyy-MM-dd HH:mm:ss");
    
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
