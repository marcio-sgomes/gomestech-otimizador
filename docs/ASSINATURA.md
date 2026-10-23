# Chave de assinatura do APK

O Android só aceita instalar uma atualização por cima se ela tiver **a mesma assinatura** da versão anterior.
Por isso o app usa uma chave fixa, que fica guardada nos *Secrets* do GitHub (nunca no código).

Isso é feito **uma vez só**.

## 1. Criar a chave (no seu PC)

Abra o **Prompt de Comando** e rode (troque nada, só digite as senhas quando pedir):

```
D:
mkdir D:\Chaves
cd D:\Chaves
keytool -genkeypair -v -keystore otimizador.jks -alias otimizador -keyalg RSA -keysize 2048 -validity 10000
```

- Ele vai pedir uma **senha**: crie uma e anote num lugar seguro.
- As perguntas de nome/cidade podem ser respondidas como quiser.
- **Guarde o arquivo `D:\Chaves\otimizador.jks`**. Se perder, os celulares terão que desinstalar e instalar de novo.

> Se aparecer "keytool não é reconhecido", instale o Java (Temurin 17) em adoptium.net e abra o Prompt de novo.

## 2. Transformar a chave em texto

No mesmo Prompt:

```
certutil -encodehex -f otimizador.jks otimizador.txt 0x40000001
```

Isso cria `otimizador.txt` com a chave em texto (base64).

## 3. Colocar no GitHub

No repositório → **Settings → Secrets and variables → Actions → New repository secret**, crie 4 secrets:

| Nome | Valor |
|---|---|
| `KEYSTORE_BASE64` | todo o conteúdo do `otimizador.txt` |
| `KEYSTORE_PASSWORD` | a senha que você criou |
| `KEY_ALIAS` | `otimizador` |
| `KEY_PASSWORD` | a mesma senha |

Depois, em **Actions → Gerar APK → Run workflow**, o APK sai em alguns minutos na aba **Releases**.
