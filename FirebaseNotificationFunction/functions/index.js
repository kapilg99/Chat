'use-strict'

const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp(functions.config().firebase);

exports.sendNotification = functions.database.ref('/notifications/{user_id}/{notification_id}').onWrite((change, context) => {

	const user_id         = context.params.user_id;
	const notification_id = context.params.notification_id;

	console.log('We have a notification to send to : ', context.params.user_id);

	const fromUser = admin.database().ref(`/notifications/${user_id}/${notification_id}/from`).once('value');
	return fromUser.then(fromUserResult => {
		const fromUserId = fromUserResult.val();
		console.log(`fromUserId : ${fromUserId}`);

		const userQuery   = admin.database().ref(`users/${fromUserId}/name`).once('value');
		const deviceToken = admin.database().ref(`/users/${user_id}/device_token`).once('value');

		return Promise.all([userQuery, deviceToken]).then(result => {
			const userName = result[0].val();
			const tokenId  = result[1].val();

			const payload = {
				notification: {
					title       : "Friend Request",
					body        : `${userName} sent you a friend request`,
					icon        : "default",
					click_action: "kapilgg99.android.chat_TARGET_NOTIFICATION"
				},
				data: {
					from_user_id: fromUserId
				}
			};
			return admin.messaging().sendToDevice(tokenId, payload).then(response => {
				return console.log('This was the notification feature');
			});
		});

	});
});
