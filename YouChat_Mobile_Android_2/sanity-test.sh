#!/bin/bash

if [ $# -ge 2 ]; then
    echo "Starting..."
    echo "UserA: $1"
    echo "UserB: $2"
else
    echo "Usage: $0 UserA UserB"
    exit 0
fi

USERA=$1
USERB=$2

echo "Initial Setup..."
# start fresh: delete usera account...
curl -X DELETE http://deploy-youchat-nodejs.acesse.com:7000/api/account/$USERA
adb shell am force-stop com.acesse.youchat
sleep 2
adb logcat -c
echo "Create $USERA account, update profile..."
adb shell am start -n com.acesse.youchat/.YouChatLoginActivity -e cmd create.account -e user $USERA -e pass 123456 -e email $USERA@corp.acesse.com -e first $USERA -e last $USERA
sleep 30
#adb logcat -d -v time  Linphone:D YOUC:D *:S
#adb logcat -c
echo "Add contact..."
adb shell am start -n com.acesse.youchat/.YouChatHomeActivity --activity-single-top -e cmd add.contact -e name $USERB
sleep 15
echo "Text chat..."
adb shell am start -n com.acesse.youchat/.YouChatHomeActivity --activity-single-top -e cmd chat.text -e name $USERB -e count 10
sleep 40
echo "Image chat..."
adb shell am start -n com.acesse.youchat/.YouChatHomeActivity --activity-single-top -e cmd chat.image -e name $USERB -e count 10
sleep 40
echo "Photo chat..."
adb shell am start -n com.acesse.youchat/.YouChatHomeActivity --activity-single-top -e cmd chat.photo -e name $USERB -e count 2
sleep 40
echo "Video chat..."
adb shell am start -n com.acesse.youchat/.YouChatHomeActivity --activity-single-top -e cmd chat.video -e name $USERB -e count 1
sleep 60
echo "Delete chat session..."
adb shell am start -n com.acesse.youchat/.YouChatHomeActivity --activity-single-top -e cmd delete.session -e name $USERB
sleep 2
echo "Delete contact..."
adb shell am start -n com.acesse.youchat/.YouChatHomeActivity --activity-single-top -e cmd delete.contact -e name $USERB
sleep 2
echo "Logout..."
adb shell am start -n com.acesse.youchat/.YouChatHomeActivity --activity-single-top -e cmd logout
sleep 2
echo "Login..."
adb shell am start -n com.acesse.youchat/.YouChatLoginActivity --activity-single-top -e cmd login -e name $USERA -e pass 123456
sleep 10
echo "Logout..."
adb shell am start -n com.acesse.youchat/.YouChatHomeActivity --activity-single-top -e cmd logout
sleep 2
echo "Stopping app..."
adb shell am force-stop com.acesse.youchat
echo "Deleting $USERA account..."
curl -X DELETE http://deploy-youchat-nodejs.acesse.com:7000/api/account/$USERA

timestamp=$(date +%s)
adb logcat -v time  Linphone:D YOUC:D *:S > output-$timestamp.txt
