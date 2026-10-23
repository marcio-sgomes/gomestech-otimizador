# Testes automáticos

Toda mudança enviada para a branch `main` passa por estas etapas no GitHub Actions
(`.github/workflows/build.yml`). **O APK só é publicado se todas passarem.**

## 1. Testes da lógica — cobertura 100%

Tudo que é regra (nota de saúde, detecção de marca, datas, leitura da atualização, filtros)
fica em `app/src/main/java/br/com/gomestech/otimizador/logica/`, sem depender de tela.

- Testes em `app/src/test/java/.../logica/`
- Cobertura medida com JaCoCo; `scripts/verificar_cobertura.py` **reprova se faltar 1 linha ou 1 caminho (if/else)**.

Rodar no PC (Android Studio ou terminal):

```
gradle :app:testDebugUnitTest :app:createDebugUnitTestCoverageReport
python scripts/verificar_cobertura.py app/build/reports/coverage/test/debug/report.xml
```

## 2. Verificação de segurança

O código é varrido em busca de coisas proibidas: comandos de root (`su`, `exec`),
permissão de acesso total aos arquivos, mudança de configurações do sistema,
encerrar outros apps ou apagar dados de outros apps. Se aparecer, a versão é reprovada.

## 3. Robô de vistoria (ponta a ponta)

`app/src/androidTest/.../RoboDeVistoriaTest.kt` usa o app como uma pessoa,
em emuladores **Android 7, 8, 10, 12 e 15**:

| Teste | O que confere |
|---|---|
| t01 | Painel: nota 0–100, armazenamento, RAM, bateria, dicas, rodapé com o ano atual |
| t02 | Checklist marca/desmarca e salva |
| t03 | Lembrete semanal liga, notifica e desliga |
| t04 | Botão de limpeza oficial abre a tela do sistema |
| t05 | Limpeza: lista do maior cache para o menor, limpa **só o cache do próprio app**, abre a tela certa de cada app, não remove nenhum app |
| t06 | Apps esquecidos: filtros 30/90 dias corretos; "Desinstalar" **só abre a confirmação do Android** — o robô cancela e confere que nada foi removido |
| t07 | Arquivos: cria uma foto de teste de 14 MB, confere que aparece, filtros funcionam, o Android pede confirmação para apagar e **só aquele arquivo** é apagado |
| t08 | Bateria: dados reais e **todos os atalhos** abrem uma tela do sistema |
| t09 | Atualização interna: acha a versão publicada, baixa e confere que é um APK válido; o aviso "Atualizar" aparece sozinho |
| t10 | Girar a tela em todas as telas não fecha o app |
| t11 | Segurança: nenhuma permissão perigosa; analisar tudo 3 vezes não apaga nem desinstala nada |
| t12 | Atalhos da marca existem no aparelho |

## 4. Teste de estresse

Depois do robô, o `monkey` do Android faz **3.000 toques aleatórios** no app.
Se o app fechar sozinho ou travar, a versão é reprovada.

## Relatórios

Em cada execução, na aba **Actions** do GitHub → execução → **Artifacts**:
relatórios dos testes, cobertura, lint, logcat e saída do teste de estresse.

## Limites (honestidade)

Os emuladores rodam o Android "puro". As telas próprias de cada marca
(Manutenção da Samsung, Segurança da Xiaomi etc.) só existem nos celulares reais;
por isso o app sempre tenta várias telas e, se a da marca não existir, abre a do Android —
nunca fecha.
