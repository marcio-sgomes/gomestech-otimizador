# GomesTech Otimizador

App Android que deixa o celular mais leve e com mais bateria, **sem anúncios e sem promessas falsas**.
Funciona em todas as marcas (Samsung, Motorola, Xiaomi/Redmi/POCO, Realme, OPPO, OnePlus, Vivo, Asus, LG, Nokia, Pixel…), do Android 7 em diante.

## O que faz

| Tela | Função |
|---|---|
| **Painel** | Nota de saúde (0–100), armazenamento, RAM, bateria, dicas automáticas e checklist semanal com lembrete |
| **Limpeza** | Apps com mais cache; abre direto a tela de limpar cache de cada um, com o caminho certo para cada marca |
| **Apps** | Apps esquecidos (sem uso há 30/60/90 dias) com espaço ocupado e botão de desinstalar |
| **Arquivos** | Fotos, vídeos e áudios acima de 10 MB (filtro do WhatsApp) com visualizar e apagar |
| **Bateria** | Temperatura, saúde, tensão e atalhos para as telas de bateria da marca do aparelho |

O app **não** finge limpar cache de outros apps nem "fechar apps para liberar RAM": o Android não permite isso
sem root, e os apps que prometem fazem mais mal do que bem. Ele faz o que é possível de verdade e guia o usuário no resto.

## Estrutura do código

```
app/src/main/java/br/com/gomestech/otimizador/
├── MainActivity.kt          # navegação, rodapé e botão "Atualizar"
├── data/
│   ├── Aparelho.kt          # armazenamento, RAM, bateria e nota de saúde
│   ├── Marcas.kt            # atalhos por marca (para adicionar marca nova, mexa só aqui)
│   ├── Permissoes.kt        # acesso ao uso, mídia e telas de detalhes
│   ├── ScannerApps.kt       # apps instalados, cache e último uso
│   └── ScannerArquivos.kt   # arquivos grandes via MediaStore
├── ui/
│   ├── BaseListaFragment.kt # TELA MODELO das listas (Limpeza, Apps, Arquivos)
│   ├── LinhaAdapter.kt      # linha genérica das listas
│   └── *Fragment.kt         # cada tela
├── update/Atualizador.kt    # atualização 100% por dentro do app (GitHub Releases)
└── reminder/                # checklist e lembrete semanal
```

Telas de lista usam o mesmo layout (`res/layout/fragment_lista.xml`) e a mesma base
(`BaseListaFragment`): uma mudança ali vale para as três.

## Como o APK é gerado e atualizado

1. Cada envio para a branch `main` dispara o GitHub Actions (`.github/workflows/build.yml`).
2. Ele compila o APK assinado e publica um **Release** `v<número>` com o arquivo `GomesTech-Otimizador.apk`.
3. O app, ao abrir, consulta o último Release; se o número for maior que o dele, mostra o botão **Atualizar**,
   baixa e abre o instalador.

O repositório precisa ser **público** para o download e a atualização funcionarem sem login.
A chave de assinatura fica nos *Secrets* do GitHub — veja [`docs/ASSINATURA.md`](docs/ASSINATURA.md).

## Link de instalação

`site/index.html` detecta o aparelho: Android baixa o APK; iPhone mostra o passo a passo de limpeza do iOS
(a Apple não permite esse tipo de app); PC mostra um QR Code para abrir no celular.

---

© GomesTech — feito por Márcio Gomes
