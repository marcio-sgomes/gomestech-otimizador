#!/usr/bin/env bash
# Roda dentro do emulador: robô de vistoria + teste de estresse (monkey).
set -euo pipefail
PKG=br.com.gomestech.otimizador
mkdir -p resultados

adb wait-for-device
adb shell settings put global window_animation_scale 0 || true
adb shell settings put global transition_animation_scale 0 || true
adb shell settings put global animator_duration_scale 0 || true
# Tela sempre ligada e desbloqueada (se a tela apagar no meio, o Espresso não acha a tela do app).
adb shell svc power stayon true || true
adb shell settings put system screen_off_timeout 2147483647 || true
adb shell input keyevent 82 || true
adb shell wm dismiss-keyguard || true
# Esconde janelas "X não está respondendo" de apps do SISTEMA, comuns em emulador lento.
# Falhas do NOSSO app continuam sendo detectadas (o teste falha e o logcat é conferido).
adb shell settings put global hide_error_dialogs 1 || true
adb logcat -c || true

echo "=== Robô de vistoria (Espresso + UiAutomator) ==="
set +e
gradle :app:connectedDebugAndroidTest --no-daemon --stacktrace
RESULT_E2E=$?
set -e
adb logcat -d > resultados/logcat-vistoria.txt || true
if [ $RESULT_E2E -ne 0 ]; then
  echo "::error::O robô de vistoria encontrou falhas (veja o relatório nos artefatos)."
  exit $RESULT_E2E
fi

echo "=== Teste de estresse: 3000 toques aleatórios ==="
adb install -r -g app/build/outputs/apk/debug/app-debug.apk
adb shell input keyevent 82 || true
adb shell appops set $PKG GET_USAGE_STATS allow || true
adb logcat -c || true
set +e
# --ignore-crashes: se OUTRO app do sistema (teclado, tela inicial...) falhar no emulador,
# o teste continua. Falhas do NOSSO app continuam reprovando (verificação abaixo pelo logcat).
adb shell monkey -p $PKG --pct-syskeys 0 --pct-appswitch 0 --ignore-crashes --ignore-timeouts --throttle 100 -s 2026 -v 3000 > resultados/monkey.txt 2>&1
RESULT_MONKEY=$?
set -e
adb logcat -d > resultados/logcat-estresse.txt || true
tail -n 20 resultados/monkey.txt
echo "--- Falhas registradas durante o estresse (qualquer app) ---"
grep -E "// CRASH|// NOT RESPONDING|ANR in" resultados/monkey.txt resultados/logcat-estresse.txt | head -20 || true

CRASH_LOGCAT=$(grep -A 3 "FATAL EXCEPTION" resultados/logcat-estresse.txt | grep -c "Process: $PKG" || true)
if grep -q "// CRASH: $PKG" resultados/monkey.txt || [ "${CRASH_LOGCAT:-0}" -gt 0 ]; then
  echo "::error::O app fechou sozinho durante o teste de estresse."
  grep -A 25 "FATAL EXCEPTION" resultados/logcat-estresse.txt | head -60 || true
  exit 1
fi
if grep -q "// NOT RESPONDING: $PKG" resultados/monkey.txt || grep -q "ANR in $PKG" resultados/logcat-estresse.txt; then
  echo "::error::O app travou (ANR) durante o teste de estresse."
  exit 1
fi
if ! grep -q "Events injected: 3000" resultados/monkey.txt; then
  echo "::error::O teste de estresse não terminou os 3000 toques (código $RESULT_MONKEY)."
  exit 1
fi
echo "OK: robô de vistoria e teste de estresse passaram."
