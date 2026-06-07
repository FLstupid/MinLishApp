const functions = require("firebase-functions");
const admin = require("firebase-admin");
const sgMail = require("@sendgrid/mail");

admin.initializeApp();

function startOfDayUtcMs(ts = Date.now()) {
  const d = new Date(ts);
  d.setUTCHours(0, 0, 0, 0);
  return d.getTime();
}

async function sendEmail({ to, subject, text }) {
  const apiKey = process.env.SENDGRID_API_KEY;
  if (!apiKey) {
    console.warn("SENDGRID_API_KEY not set; skipping email.");
    return;
  }

  sgMail.setApiKey(apiKey);

  await sgMail.send({
    to,
    from: process.env.SENDGRID_FROM_EMAIL || "no-reply@minlish.app",
    subject,
    text,
  });
}

/**
 * Daily digest email (MVP skeleton).
 * Queries Firestore for due review words (repetitions > 0 && nextReviewDate <= now),
 * then emails a per-user summary.
 *
 * Notes:
 * - Requires that Android syncs words to `/users/{uid}/words`.
 * - Requires SendGrid API key via env var `SENDGRID_API_KEY`.
 * - User opt-in flag `emailNotifications` is assumed as boolean in `/users/{uid}`.
 */
exports.dailyDueReviewEmailDigest = functions.pubsub
  .schedule("every day 08:00")
  .timeZone("Asia/Bangkok")
  .onRun(async () => {
    const now = Date.now();

    const usersSnap = await admin.firestore().collection("users").get();

    const perUser = await Promise.all(
      usersSnap.docs.map(async (userDoc) => {
        const uid = userDoc.id;
        const userData = userDoc.data() || {};

        const email = userData.email;
        const optIn = userData.emailNotifications === true;
        if (!email || !optIn) return null;

        // Query due words for this user.
        const dueWordsSnap = await admin
          .firestore()
          .collection("users")
          .doc(uid)
          .collection("words")
          .where("repetitions", ">", 0)
          .where("nextReviewDate", "<=", now)
          .get();

        const dueCount = dueWordsSnap.size;
        if (dueCount <= 0) return null;

        const subject = `MinLish daily review: ${dueCount} word(s) due`;
        const text =
          `Hi! You have ${dueCount} vocabulary word(s) due for review today.\n\n` +
          `Open the app to continue your daily plan.`;

        return sendEmail({ to: email, subject, text });
      })
    );

    // Wait all potential sends.
    await Promise.all(perUser.filter(Boolean));

    return { ok: true };
  });

