# Running a standalone test mail server
- https://greenmail-mail-test.github.io/greenmail/#deploy_standalone
```
./gradlew runGreenMail &
```

# Testing the mail client
```bash
java -jar build/libs/mail.cli-app-0.1.0.jar send user1@abc.com user0@abc.com Test 'Test\n000'
java -jar build/libs/mail.cli-app-0.1.0.jar send user2@abc.com user0@abc.com Test 'Test\n000'
java -jar build/libs/mail.cli-app-0.1.0.jar messages INBOX
java -jar build/libs/mail.cli-app-0.1.0.jar delete INBOX 1
java -jar build/libs/mail.cli-app-0.1.0.jar messages INBOX
java -jar build/libs/mail.cli-app-0.1.0.jar delete INBOX 1
java -jar build/libs/mail.cli-app-0.1.0.jar messages INBOX
```
