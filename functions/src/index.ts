import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

admin.initializeApp();
const db = admin.firestore();

export const mpesaCallback = functions.https.onRequest(async (req, res) => {
    const callbackData = req.body.Body.stkCallback;
    const resultCode = callbackData.ResultCode;
    const checkoutRequestID = callbackData.CheckoutRequestID;

    console.log(`Received M-Pesa Callback for ID: ${checkoutRequestID} with ResultCode: ${resultCode}`);

    if (resultCode === 0) {
        const metadata = callbackData.CallbackMetadata.Item;
        const amount = metadata.find((item: any) => item.Name === "Amount").Value;
        const mpesaReceipt = metadata.find((item: any) => item.Name === "MpesaReceiptNumber").Value;

        // 1. Find the pending payment we saved in the App
        const paymentRef = db.collection("payments").doc(checkoutRequestID);
        const paymentDoc = await paymentRef.get();

        if (paymentDoc.exists) {
            const uid = paymentDoc.data()?.uid;

            // 2. Update the User's balance (Subtract the paid amount)
            const userRef = db.collection("users").doc(uid);
            await userRef.update({
                rentBalance: admin.firestore.FieldValue.increment(-amount)
            });

            // 3. Mark the payment as Success
            await paymentRef.update({
                status: "Success",
                mpesaReceipt: mpesaReceipt,
                completedAt: admin.firestore.FieldValue.serverTimestamp()
            });

            console.log(`Successfully updated balance for user: ${uid}`);
        }
    } else {
        // Mark as failed if ResultCode is not 0
        await db.collection("payments").doc(checkoutRequestID).update({
            status: "Failed",
            errorCode: resultCode
        });
    }

    res.status(200).send("OK");
});