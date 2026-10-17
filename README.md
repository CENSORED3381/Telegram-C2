# Serviços do Sistema — C2 Telegram (red team / authorized)

Agente Android controlado via Bot API do Telegram. Uso apenas em laboratórios e
dispositivos **de sua propriedade** ou com **consentimento escrito**.

## Compilando

### Local
```bash
echo "TELEGRAM_TOKEN=123:abc" >> local.properties
echo "TELEGRAM_CHAT_ID=123456789" >> local.properties
gradle wrapper --gradle-version 8.7   # gera gradle-wrapper.jar
./gradlew assembleRelease
