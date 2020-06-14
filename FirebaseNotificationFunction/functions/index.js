'use-strict'

const functions = require('firebase-functions');
const admin     = require('firebase-admin');
admin.initializeApp(functions.config().firebase);

exports.sendNotification = functions.database.ref('/notifications/{user_id}/{notification_id}').onWrite((change, context) => {

	const user_id         = context.params.user_id;
	const notification_id = context.params.notification_id;

	console.log('We have a notification to send to : ', context.params.user_id);

	const fromUser = admin.database().ref(`/notifications/${user_id}/${notification_id}/from`).once('value');
	return fromUser.then(fromUserResult => {
		const fromUserId = fromUserResult.val();
		console.log(`fromUserId : ${fromUserId}`);

		const userQuery = admin.database().ref(`users/${fromUserId}/name`).once('value');
		return userQuery.then(userResult => {
			const userName = userResult.val();

			const deviceToken = admin.database().ref(`/users/${user_id}/device_token`).once('value');
			return deviceToken.then(result => {

				const token_id  = result.val();
				const nameQuery = admin.database().ref(`/users/${user_id}/name`).once('value');
				return nameQuery.then(result => {

					const name = result.val();
					const payload = {
						notification: {
							title       : "Friend Request",
							body        : `${userName} sent you a friend request`,
							icon        : "default",
							click_action: "kapilgg99.android.chat_TARGET_NOTIFICATION"
						},
						data : {
							from_user_id : fromUserId
						}
					};

					return admin.messaging().sendToDevice(token_id, payload).then(response => {
						return console.log('This was the notification feature');
					});

				});

			});
		});

	});

});