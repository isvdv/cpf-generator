# CPF Generator (Android) v3.0

App offline e monocromático que gera CPFs matematicamente válidos para testes de layout/UI. A validade dos dígitos verificadores não confirma existência ou cadastro real.

## Novidades da v3.0
- **Notificação:** tocar nela copia o CPF e já gera o próximo (sem abrir o app); botões "Pular" e "Abrir app"
- Interface nova: cartão de destaque com o CPF atual, chips de origem (9º dígito com estados), controle segmentado de quantidade, lista em cartões que rola junto com o topo
- Economia de bateria: preto puro (OLED), nenhum serviço/alarme/wakelock, canal de notificação silencioso, notificação só é atualizada quando algo muda, sem sincronizar com relógios
- Vibração leve ao tocar; sem toast duplicado no Android 13+ (o sistema já confirma a cópia)

## Novidades da v2.0
- Quantidade selecionável: 10 / 50 / 100 / 500
- Escolha do 9º dígito (região) ou aleatório
- Alternar máscara (000.000.000-00 / só números)
- Copiar tudo, compartilhar lista, tocar no item para copiar
- Geração dos dois dígitos verificadores conforme o algoritmo oficial do CPF
- Notificação com o número, botão Copiar e botão Novo
- Sem duplicatas, SecureRandom, lista otimizada (ListView), mantém estado ao girar a tela
- Respeita barras do sistema (Android 15), ícone próprio, release com minify

## Como gerar o APK
**Android Studio:** abra a pasta, aguarde o sync e use *Build > Build APK(s)*.
**Linha de comando:** `gradle assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk`
**GitHub:** envie a pasta a um repositório; a aba *Actions* roda `Build APK` e disponibiliza o APK como artefato.
