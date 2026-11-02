# Google Sheets Feedback Webhook & 3-Month Auto-Delete

This script handles feedback submissions from the **Pixelabs Calculator** Android app and enforces our **90-Day (3 Months)** privacy data retention policy by automatically purging entries older than 3 months.

---

## 🚀 How to Apply to your Google Sheet

1. **Open your Google Sheet** where calculator feedback is collected.
2. In the top menu, click **Extensions** > **Apps Script**.
3. Replace the code in the editor with the contents of **[`Code.gs`](Code.gs)**.
4. Click **Save** (💾 icon).
5. Click **Deploy** > **Manage deployments** > **Edit** > select **New version** > click **Deploy**.

### Enable Automated Daily Auto-Delete Trigger (Recommended)
1. In the Apps Script toolbar, select `setupDailyAutoDeleteTrigger` from the function dropdown.
2. Click **Run**.
3. Grant permissions if prompted.
4. *Result:* Google will now automatically run `autoDeleteDailyCleanupTrigger` every night at 2:00 AM, purging any row where the timestamp in Column A is older than 90 days (3 months).

### Real-Time Auto-Delete on Submission
- Even without the daily trigger, every time a new feedback entry is received by the `doPost(e)` webhook, the script automatically checks and deletes rows older than 90 days.

---

## 📋 Data Columns Recorded
| Column | Name | Purpose |
| :---: | :--- | :--- |
| **A** | **Timestamp / Received Date** | Timestamp when feedback was received/recorded. **Used for 3-Month Auto-Deletion.** |
| **B** | Feedback Type | Bug Report, Calculation, Suggestion, Feedback. |
| **C** | Subject / Summary | User-provided summary. |
| **D** | Details & Description | Detailed user message. |
| **E** | User Email | Optional email for follow-up. |
| **F** | Device Model | Device manufacturer and model (only if opted in). |
| **G** | Android OS Version | OS version & API level (only if opted in). |
| **H** | App Version | Current app build version. |
| **I** | Angle Mode | Active calculation mode (DEG or RAD). |
