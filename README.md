# CPF Generator (Android) v2.0

App offline e monocromático que gera números **fictícios** no formato de CPF para testes de layout/UI. Os números nunca passam na validação oficial (o dígito verificador é sempre ajustado se coincidir com um válido).

## Novidades da v2.0
- Quantidade selecionável: 10 / 50 / 100 / 500
- Escolha do 9º dígito (região) ou aleatório
- Alternar máscara (000.000.000-00 / só números)
- Copiar tudo, compartilhar lista, tocar no item para copiar
- Correção: antes ~1% dos números gerados podiam ser CPFs válidos por acaso
- Sem duplicatas, SecureRandom, lista otimizada (ListView), mantém estado ao girar a tela
- Respeita barras do sistema (Android 15), ícone próprio, release com minify

## Como gerar o APK
**Android Studio:** abra a pasta, aguarde o sync e use *Build > Build APK(s)*.
**Linha de comando:** `gradle assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk`
**GitHub:** envie a pasta a um repositório; a aba *Actions* roda `Build APK` e disponibiliza o APK como artefato.
