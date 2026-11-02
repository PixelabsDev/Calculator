/**
 * Pixelabs Calculator - Google Sheet Feedback Webhook with 90-Day (3 Months) Auto-Deletion
 *
 * Requirements:
 * 1. Collects feedback submissions from the Calculator Android app (bug reports, calculations, suggestions, feedback).
 * 2. Records the exact receipt date in Column A.
 * 3. Automatically deletes any feedback rows older than 90 days (3 months) calculated from the date of receipt.
 * 4. Includes both real-time purge on receipt and scheduled daily background purge trigger.
 */

// Target sheet name inside the spreadsheet
const SHEET_NAME = "Feedback";

// Retention period: 90 days (approx. 3 months)
const RETENTION_DAYS = 90;

/**
 * Webhook handler for POST requests from Calculator app
 */
function doPost(e) {
  try {
    // Acquire a script lock to prevent race conditions during concurrent submissions
    const lock = LockService.getScriptLock();
    lock.waitLock(10000); // Wait up to 10 seconds

    const sheet = getOrCreateFeedbackSheet();
    const payload = JSON.parse(e.postData.contents);
    const receivedAt = new Date(); // Exact server receipt timestamp (used for 3-month calculation)

    // Append new feedback record
    sheet.appendRow([
      receivedAt,                                      // Column A: Received Date (Trigger for 3-Month Auto-Delete)
      payload.timestamp || receivedAt.toISOString(),   // Column B: Client Timestamp
      payload.category || "Feedback",                  // Column C: Category (Bug Report, Calculation, etc.)
      payload.title || "User Feedback",                // Column D: Subject / Summary
      payload.description || "",                       // Column E: Details & Description
      payload.email || "",                             // Column F: User Email (Optional)
      payload.deviceModel || "N/A",                    // Column G: Device Model
      payload.androidVersion || "N/A",                 // Column H: Android Version
      payload.appVersion || "N/A",                     // Column I: App Version
      payload.angleMode || "N/A"                       // Column J: Angle Mode (DEG/RAD)
    ]);

    // Automatically purge entries older than 3 months upon every new submission
    deleteFeedbackOlderThanThreeMonths(sheet);

    lock.releaseLock();

    return ContentService.createTextOutput(JSON.stringify({
      status: "success",
      message: "Feedback recorded successfully."
    })).setMimeType(ContentService.MimeType.JSON);

  } catch (error) {
    return ContentService.createTextOutput(JSON.stringify({
      status: "error",
      message: error.toString()
    })).setMimeType(ContentService.MimeType.JSON);
  }
}

/**
 * Purges any row where (Now - Received Date) > 90 days (3 months).
 * Iterates backwards from bottom to top to preserve valid row indices during deletion.
 */
function deleteFeedbackOlderThanThreeMonths(sheet) {
  if (!sheet) {
    sheet = getOrCreateFeedbackSheet();
  }

  const lastRow = sheet.getLastRow();
  // If sheet only has headers or is empty, nothing to delete
  if (lastRow <= 1) return;

  const now = new Date();
  const cutoffTime = now.getTime() - (RETENTION_DAYS * 24 * 60 * 60 * 1000); // 90 days in milliseconds

  // Read Column A (Received Date) starting from row 2 down to lastRow
  const dateRange = sheet.getRange(2, 1, lastRow - 1, 1);
  const dateValues = dateRange.getValues();

  // Walk backwards from bottom row to row 2
  for (let i = dateValues.length - 1; i >= 0; i--) {
    const rawValue = dateValues[i][0];
    if (!rawValue) continue;

    const rowDate = new Date(rawValue);
    if (!isNaN(rowDate.getTime()) && rowDate.getTime() < cutoffTime) {
      // Row index is 1-based, offset by header (+2)
      sheet.deleteRow(i + 2);
    }
  }
}

/**
 * Scheduled trigger function that runs daily to purge records older than 3 months
 * even if no new submissions occur on that day.
 */
function autoDeleteDailyCleanupTrigger() {
  const sheet = getOrCreateFeedbackSheet();
  deleteFeedbackOlderThanThreeMonths(sheet);
}

/**
 * One-time setup function: Run this function once from the Apps Script Editor
 * to install the automated daily 2:00 AM auto-delete trigger.
 */
function setupDailyAutoDeleteTrigger() {
  // Remove any pre-existing cleanup triggers to avoid duplicate runs
  const triggers = ScriptApp.getProjectTriggers();
  for (let i = 0; i < triggers.length; i++) {
    if (triggers[i].getHandlerFunction() === "autoDeleteDailyCleanupTrigger") {
      ScriptApp.deleteTrigger(triggers[i]);
    }
  }

  // Create a time-driven trigger running daily at 2:00 AM
  ScriptApp.newTrigger("autoDeleteDailyCleanupTrigger")
    .timeBased()
    .everyDays(1)
    .atHour(2)
    .create();

  Logger.log("Daily auto-delete trigger installed successfully (runs daily at 2:00 AM).");
}

/**
 * Helper to ensure the Feedback sheet exists with formatted headers
 */
function getOrCreateFeedbackSheet() {
  const ss = SpreadsheetApp.getActiveSpreadsheet();
  let sheet = ss.getSheetByName(SHEET_NAME);
  if (!sheet) {
    sheet = ss.insertSheet(SHEET_NAME);
  }

  if (sheet.getLastRow() === 0) {
    const headers = [
      "Received Date",
      "Client Timestamp",
      "Feedback Type",
      "Subject / Summary",
      "Details & Description",
      "User Email",
      "Device Model",
      "Android OS Version",
      "App Version",
      "Angle Mode"
    ];
    sheet.appendRow(headers);
    sheet.getRange(1, 1, 1, headers.length).setFontWeight("bold").setBackground("#EDE7F6");
    sheet.setFrozenRows(1);
  }

  return sheet;
}
