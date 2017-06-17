# spencer-translate-bot
Google Translate Bot for Slack, will translate Romanian and Hebrew to English (other lang can be added)

### Building the bot
User gradle or gradlew to build<br />
You can generate a fat jar for deploying using the task: fatJarPlus

### Running the bot

For the Slack API key, add an integration for your team from Slack settings.<br />
For the Google API keys, see https://console.cloud.google.com

#### Required command line arguments:
-googleTranslateApiKey=TRANSLATE_API_KEY
-slackApiKey=TRANSLATE_API_KEY

#### Optional command line arguments
Send message to this slack user when the bot starts successfully
-alertUserName=SLACK_USERNAME<br />


If you want to use google cloud storage api for saving settings, create a service account with the proper rights and set these params:<br />
-googleCloudProjectName=GOOGLE_CLOUD_PROJECT_NAME<br />
-googleCloudServiceAccountJsonFile=JSON_FILE_PATH
