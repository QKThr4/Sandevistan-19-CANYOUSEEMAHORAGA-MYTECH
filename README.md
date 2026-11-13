# Sandevistan (Cyberpunk: Edgerunners) — Minecraft Java 1.21.1 / NeoForge 21.1.172

Este mod adiciona **somente** o Sandevistan. / This mod adds **only** the Sandevistan.

## Como compilar (GitHub Actions)
1. Crie um repositório e envie estes arquivos (a pasta `.github/workflows` inclusa já faz o build).
2. Aba **Actions** → rode o workflow → baixe o artefato `sandevistan-mod` (o `.jar` fica dentro).
3. Coloque o `.jar` na pasta `mods` do Minecraft 1.21.1 com NeoForge.

## Como obter
**Crafting** (mesa de trabalho, coluna do meio): **Neuralink** em cima, **Espinha** no meio, **Acelerador** embaixo.
Receitas das peças (ajustáveis em `data/sandevistan/recipe`):
- **Chip de Circuito**: Esmeralda e Redstone em cima; Barra de ferro e Pepita de ouro embaixo (2x2).
- **Neuralink**: Chip, Redstone, Chip em uma linha.
- **Espinha**: 3 Chips em coluna.
- **Acelerador**: Chip no centro, Diamante à esquerda e à direita, Barra de ouro em cima e embaixo.
Tudo também está na aba criativa "Sandevistan".

## Implante
- Clique com o botão direito segurando o item. Aparece um **aviso**: o implante causa **3 corações de dano instantâneo**.
- Clique de novo em até 10 segundos para confirmar. Precisa de mais de 3,5 corações de vida (criativo ignora).
- Depois de implantado, a coluna verde fica **nas suas costas permanentemente** (visível para todos os jogadores).
- Só sai se você remover o implante na aba **J** (botão "Remover implante", com o Sandevistan desligado): o item volta para o inventário. Morrer não remove o implante.

## Uso
| Tecla (configurável em Controles) | Ação |
|---|---|
| **V** | Ligar/desligar o Sandevistan (espera de 2 s entre trocas) |
| **J** | Abrir a aba do Sandevistan |

Enquanto está ligado:
- O jogo inteiro roda a **3 ticks por segundo** (igual a `/tick rate 3`, 85% mais lento).
- Você ganha o **efeito de Velocidade** (nível 55, aparece nos efeitos), pulo alto (~3 blocos), gravidade reduzida, queda rápida, Força IV, Resistência IV, Pressa III, Visão Noturna, bater/quebrar rápido como se o tempo fosse normal e **nenhum dano de queda**.
- A tela fica **levemente esverdeada** e silhuetas (afterimages) do jogador ficam paradas por onde ele passou, **cada uma dura 3 segundos** e muda suavemente de cor entre verde, azul e roxo.
- Ao desligar, o jogador **para no lugar** na hora (sem sair disparado).
- Sons: "Sandevistan Usa" ao ligar, "Sandevistan Para De Usar" ao desligar (os sons acompanham o jogador).

## Mente
- A barra **Mente** (HUD e aba J) é o tempo de uso: cheia dura **30 segundos reais** de uso (a contagem é em tempo real, mesmo com o jogo a 3 ticks).
- Desligado, ela recupera 1 segundo de uso a cada 3 segundos parados (do zero ao cheio em **90 segundos**). Precisa de pelo menos 10 de Mente para ligar.
- Se a Mente chegar a zero o Sandevistan desliga sozinho e o jogador sofre **sobrecarga** (náusea, fraqueza e lentidão).
- A posição do HUD da Mente muda na aba J (4 cantos; fica salva em `config/sandevistan-client.properties`).

## Ajustes de balanceamento
Tudo fica no topo de `SandevistanLogic.java` (velocidade, pulo, gravidade, força, pressa) e em `SandevistanData.java`
(duração da Mente, regeneração, tick rate). Textos em `assets/sandevistan/lang`.

## Idiomas
Português (pt_br e pt_pt) para quem joga em português; inglês para todos os outros idiomas.

## Multiplayer
O tick rate é do servidor inteiro: enquanto qualquer jogador está com o Sandevistan ligado, todos ficam a 3 ticks.

## Ciberpsicose (3.1.0)

- A cada 50% de Mente usada somam-se 5 de ciberpsicose; os sintomas aparecem na hora (com a Mente em 0 usando o Sandevistan, soma 2 de ciberpsicose por segundo).
- A perda de Mente da ciberpsicose é permanente: a Mente máxima cai 1 (100 vira 99) a cada 30/15/7/5/1 s conforme a fase. A parte perdida aparece em vermelho escuro na barra.
- Estabilizador Neural (mel, olho de aranha fermentado, açúcar e frutas silvestres): cura 5 de Mente na hora e dá Mente Boa por 1 minuto, em que cada segundo tem 1% de chance de curar 1 de Mente (recupera Mente perdida de vez, sem passar de 100) e baixa a ciberpsicose; tomar outro refaz o minuto e cura de novo.
- Imunossupressor (garrafa, bagas luminosas, ametista e redstone): +20 de Mente por 1 minuto. Acumula (cada dose soma +20 e 1 minuto, até 100 de Mente no total; com 100 não dá para tomar). Ao acabar some todo o bônus e cada dose tira 5 de Mente para sempre.
- O ataque involuntário tem dano e alcance reduzidos (3/4/6 de dano, 2,5 blocos).

- Mente em 0: ao chegar em 0 o jogador toma 3 corações de dano (uma vez; só de novo depois de recuperar Mente e zerar outra vez) e, enquanto usa o Sandevistan com a Mente em 0, toma 1 coração a cada 5 s. O dano ignora a Resistência do próprio Sandevistan.

## Ciberpsicose: efeitos extras (3.1.0)

- Imunossupressor: também tira até 40 de ciberpsicose; quando o efeito acaba, essa mesma quantidade volta. Cores: Imunossupressor verde, Estabilizador Neural roxo.
- A partir de 100% a ciberpsicose só sobe (+1 a cada 1,5 s); esperar ou o Estabilizador não baixam, só o Imunossupressor alivia.
- Acima de 50%: a visão vira para o lado, o jogador anda ou pula sozinho, e há apagões (tela preta) em que ele caminha até a entidade mais próxima (jogador ou mob) e bate nela. Tudo cresce com a ciberpsicose.
- Sons estranhos só depois de 25%. Ícone de glitch só depois de 50%, pulando de lugar. Feixes de glitch: raros até 25%, cada vez mais até 75%, sempre na tela depois.
- Vultos escuros de jogadores aparecem ao redor (raros até 25%, comuns a partir de 50%) e somem 3 s depois de o jogador olhar para eles.
- Músicas (a partir de 75%, volume sobe até 100% e depois fica estável; só o jogador escuta; botão na aba do Sandevistan para trocar entre as duas ou nenhuma): "I Really Want to Stay at Your House" e "Żurawie", fornecidas pelo autor do mod; os direitos pertencem aos artistas.

## 3.1.1

- O apagão dura 10 segundos logo acima de 50% de ciberpsicose e aumenta com ela até 30 segundos (a partir de 150%).

## 3.1.2

- Tick rate do Sandevistan: 3 (85% mais lento que 20), velocidade e quebra de blocos ajustadas para o novo ritmo.
- Rápido também na água, debaixo d'água, agachado, em alma de areia/mel, em escadas/trepadeiras e em teia, arbusto de frutas e neve em pó.
- Comando de OP: `/sandevistan mente <add|remove|set> <0-100> [jogadores]` muda a Mente permanente.
- A tela não pisca em verde até 15% de ciberpsicose. A partir de 100% o jogador fica sempre em apagão (às vezes enxerga, mas não controla).
- A música da ciberpsicose para a música do jogo e o volume máximo foi reduzido.

## 3.1.3 (crafting mais difícil e Clínica de Ripperdoc)

- Cristal Neural: ametista + lápis-lazúli + redstone (sem forma). Fibra Sináptica: 2 linhas + bola de slime + esmeralda (sem forma).
- Neuralink: chip, Cristal Neural, chip em cima e Fibra Sináptica embaixo no meio.
- Ventoinha: 4 barras de ferro nos cantos + 1 pepita de ferro no meio. Núcleo: 3 barras de cobre (cima, esquerda, direita) + Ventoinha no meio + grade de ferro embaixo.
- Bateria de Energia: 4 ametistas e 4 barras de cobre alternados em volta de 1 bloco de redstone.
- Espinha: chip em cima, Núcleo no meio e Bateria de Energia embaixo.
- Servo-Motor: pistão + redstone + 2 barras de cobre (sem forma). Acelerador: o diamante da esquerda vira Servo-Motor.
- Clínica de Ripperdoc (bloco): 6 barras de ferro nas laterais, painel de vidro em cima, chip no meio e bloco de ferro embaixo. O Sandevistan só é implantado nela: segure o item, clique na clínica, pague 8 esmeraldas e confirme (continua dando 3 corações de dano).
- A gravidade ao cair com o Sandevistan ligado ficou um pouco menor; vultos um pouco mais frequentes.

## 3.2.0

- Dois sons de psicose do mod entram no sorteio dos sons estranhos (os sons do Minecraft continuam). O som "Alert" toca, só para o jogador, nos avisos em vermelho do chat.
- Ripperdoc: aldeão doutor (skin própria) que trabalha na Clínica de Ripperdoc e aparece numa garagem decorada nas vilas (as 5 variantes). Clique nele: ele pergunta se quer implantar o Sandevistan por 8 esmeraldas (botão no chat; gasta o item e causa 3 corações). Remover o implante também só com ele. OP: `/sandevistan clinica` constrói a garagem na sua frente.
- Sons de psicose e Alert agora seguem o jogador. Ligar/desligar sem cooldown; o som de um corta o do outro (sobreposição).
- Apagão: chance minúscula em 50%, bem mais fácil depois de 75%, comum a partir de 80% e bem comum a partir de 90%.
- Dano da ciberpsicose só a partir de 175% (1 coração/s) e 3 corações/s a partir de 225%.
- Tremedeira do modelo (rara, aumenta até 99%) e glitches no modelo visíveis para todos, por faixa: 1-25 fracos na cabeça, 26-50 também no corpo às vezes, 51-75 fracos ou fortes, 76-99 mais frequentes, 100+ vários e aleatórios.
- Comando `/sandevistan mente atual add|remove|set <valor> [jogadores]` muda a Mente que regenera.
- Usar sem Mente: 2 corações a cada 8 s de uso (o tempo acumulado se mantém ao parar). Acima de 100% o implante liga/desliga sozinho (5 a 10 s) e não há mais esse dano.
- Afterimages: só enquanto ligado, sem sumir sozinhas; ao desligar viram um feixe branco que some bem rápido.
- Receitas liberadas ao pegar qualquer ingrediente.
- Nome do item: Sandevistan Militech Apogee.
- Ripperdoc sem chat: clicar nele ou no bloco da clínica abre uma tela com Loja e Implantes. Implantes: encaixe o Sandevistan na silhueta (seta para as costas) e clique em Implantar (8 esmeraldas); com o implante há o botão Remover. Loja: Estabilizador Neural 20 esmeraldas, Imunossupressor 16.
- Tecla J: aba com a silhueta e o implante (só visual); passar o mouse no implante mostra o que ele faz.
- Tremedeira do modelo mais fraca e por partes (braços, depois corpo/cabeça, depois qualquer parte ou tudo).
- Knockback maior em quem o jogador acerta; de 30% a 100% de ciberpsicose o jogador perde aos poucos o knockback recebido e o balanço da tela ao tomar dano.
- Sons de psicose mais baixos; música dos 75% um pouco mais alta no início.
- Ligar gasta 5 de Mente. Os 3 corações ao chegar em 0 têm recarga de 1,5 s.
- 50% de desviar de ataques e projéteis ligado ("Desviou!", sem recarga).
- Comer, beber, arcos, bestas, tridente e recargas de itens 85% mais rápidos; na água dá para subir (pular) e descer (agachar), inclusive em cachoeira.

## Novidades (ainda 3.2.0)

- Imunossupressor: segurando e agachado, bater em outro jogador injeta nele. Em sequência (até 3 minutos entre doses): doses 1 a 4 tiram até 40 de ciberpsicose e 1/3 do que saiu não volta; da 5ª à 8ª tiram metade e tudo volta; da 9ª à 12ª um quarto; da 13ª em diante metade a cada dose até não tirar mais nada.
- De 100% de ciberpsicose em diante o jogador não usa itens, não dropa nem abre inventário (só o menu); pode trocar o item da mão.
- Apagão com metade da chance. Sons de psicose diminuem de 75% a 100% e ficam mudos de 100% em diante (a música continua).
- Tela de implantes: o jogador (modelo do inventário), slot "Costas" e o inventário para arrastar o implante até o slot; informações aparecem no item. Sem seta, sem bloco verde.
- Item Sandevistan com as informações do que faz; a informação de ir à clínica fica na aba J.
- Tecla V sem implante: "Você precisa de um implante primeiro".
- O Ripperdoc volta a andar até o bloco da clínica de tempos em tempos.
- "Desviou!" some mais rápido.

## 4.0.0 (Carga Cibernética, Braço de Ferro, Link Neural e chips)
- **Aba J**: mostra só os implantes (jogador + encaixes), a **Carga Cibernética**, os chips e os botões (posição do HUD da Mente, música, fechar).
- **Carga Cibernética**: Sandevistan +30%, cada Braço de Ferro +10%, cada chip +5%, Link Neural +0%. A partir de 50%, a cada 5 s há chance de ganhar ciberpsicose (50%: 5% de chance de +1; 100%: 50% de chance de +10; valores intermediários são interpolados). Aparece no J e na tela do Ripperdoc (que mostra quanto vai subir antes de implantar).
- **Braço de Ferro** (encaixes braço direito e esquerdo): braço vira um bloco de ferro (dá para ocultar no J) e soma o dano da Força I (+3) em ataques corpo a corpo, sem afetar arcos nem arremessos.
- **Link Neural** + **Chipware**: soquete visível atrás da orelha direita. Até 2 chips diferentes; inserir com botão direito, remover na aba J (sem doutor). Cada chip: +5% de carga, -5 de Mente permanente (azul na barra, volta ao remover), 10 s de "Baixando Chipware..." e 1 de durabilidade por uso (conserto na bigorna com redstone ou ferro).
- Chips: Artes Marciais (Velocidade I + Força I), Supressão de Dor (Resistência I), Mineração (blocos 2x mais rápido), Andarilho (Pulo I, 20% menos dano de queda, Graça do Golfinho).
- Imunossupressor: só com ciberpsicose acima de 0, restaura Mente atual, cada dose tem o seu minuto e o HUD mostra no máximo 2 tempos.
- Apagão só a partir de 70% (raro, incomum em 80%, comum em 90%). Socos involuntários removidos: o jogador só vira a cabeça para a entidade mais próxima por 3 s. No apagão o jogador contorna paredes, abre portas, sobe escadas e água.
- O bloco da clínica não tem mais interação.

### 4.0.0 (2nd batch)
- 4 new Sandevistans: Militech Falcon (70% slow, 25% load, 35 s), Dynalar (50%, 20%, 40 s), Zetatech (30%, 15%, 45 s, double damage and more in the air), QianT Warp Dancer (20%, 10%, 50 s, half damage taken). Each needs its own colored Accelerator, Spine and Neuralink.
- New implants: Reinforced Bone Structure and Enhanced Nervous System (5% load each, no potion effects).
- Effects tab in the J screen, HUD position editor, two Iron Arms stack (+6), psychosis effects only after 15%.
- Sandevistans other than Apogee use the new on/off sounds; first use of the Apogee after each implant plays a full-screen intro video.
- Cyberpsychosis works without any implant; psychosis music from 60%; iron arm +2 each; flat 2D Neural Link on head; green zombie Ripperdoc; Immunosuppressant and Neural Stabilizer are syringes (hold right click).
- Empty Syringe (3 iron nuggets) is the base of Immunosuppressant and Neural Stabilizer and is what is left after injecting.
- New implants: Iron Heart (+5 hearts, +10% healing, 5% load) and Cybernetic Legs (per leg: +5% speed, jump, -15% fall damage, 10% load).
- Cybernetic Legs show as dark iron blocks on the player (same visibility toggle as the iron arms).
- Visibility screen: arms and legs can be shown or hidden independently.
- Colored Sandevistan parts are crafted from a dye, blocks and ingots plus one Circuit Chip (no Apogee parts).

### 4.1.2
- Ripperdoc can install several implants in one surgery (one damage, 8 emeralds each).
- Cyberware Load psychosis from 70% (not at exactly 50%).
- Walker Chip: Jump Boost I and 20% less fall damage (no longer cancels it).
- Cybernetic Legs give +15%% speed each; Cyberware Load psychosis from 70%%.

### 4.2.0
- Low permanent Mind (below 60%, 30% and 10%) now causes light symptoms: shaky aim, neural fatigue, reflex lapses, headaches, failing vision, rare tinnitus with muffled sound, phantom footsteps and whispers, hotbar mix-ups and 1-second freezes.
- Cyberpsychosis slowly creeps up to a low cap (4/8/14%), builds a bit faster, the Immunosuppressant removes less, and the 15% effects start at 10% when Mind is below 30%.
- Villagers and animals avoid the player, hostile mobs notice them from farther away.
- The Ripperdoc now talks to the player depending on the situation.

### 4.2.1
- Removed the shadow figure that flashed in the corner of the screen with low Mind.

## Tiers dos implantes (4.3.0)
Braço de Ferro, Pernas Cibernéticas, Estrutura Óssea, Sistema Nervoso, Coração de Ferro e os chips de Artes Marciais, Supressão de Dor e Andarilho têm tiers I, II e III. Sandevistan, Link Neural e Chip de Mineração não têm.
- Para melhorar: na **Bancada de Montagem**, use o implante do tier anterior mais os componentes do tier (Melhorado para o II, Alta Precisão para o III). Depois remova o implante antigo no Ripperdoc e implante o novo.
- Braço: +2 / +2,5 / +3 de dano; carga 10 / 12 / 14%. Pernas: velocidade +15 / +18 / +20%, pulo +0,04 / +0,05 / +0,06, queda -15 / -18 / -21%; carga 10 / 12 / 14% por perna.
- Osso: armadura +3 / +4 / +5, knockback 25 / 30 / 35%, queda -30 / -35 / -40%. Nervos: velocidade +8 / +10 / +12%, ataque +12 / +15 / +18%. Coração: +5 / +6 / +7 corações, cura +10 / +15 / +20%. Carga dos três: 5 / 6 / 7%.
- Chips: Mente permanente 5 / 4 / 3; durabilidade Artes Marciais 800 / 900 / 1000, Supressão de Dor 500 / 600 / 700, Andarilho 400 / 500 / 600.
- `python3 tools/gen_tiers.py` regenera as texturas dos tiers.
- Removida a linha branca do meio da barra de Carga Cibernética (J e Ripperdoc).
- Corrigido: ao desligar, os efeitos do Sandevistan (Velocidade etc.) agora saem mesmo se outro efeito os fundiu; também são limpos ao entrar no mundo.
- **Chip de RAM** (4.3.0, tiers I/II/III): sobe o limite da Carga Cibernética para 75 / 77 / 80% (a ciberpsicose por carga só começa aí). Não soma Carga, custa 5 de Mente e tem 600 de durabilidade em todos os tiers; só gasta durabilidade (1 a cada 5 s) enquanto a carga passa de 70%.

## 5.0.0 (em andamento)
- Reune tudo da 4.3.0 (tiers, Chip de RAM, correções) e as novidades abaixo.
- **Insanidade** (barra vermelha acima da de ciberpsicose): a Carga Cibernética alta e o uso do Sandevistan sobem a Insanidade em vez da ciberpsicose. A partir de 20% de Insanidade a ciberpsicose sobe de +1 a cada 10 s até +1 a cada 2 s (em 100%). A Insanidade desce parado, com a carga abaixo do limite, no mesmo ritmo da ciberpsicose. Comando de teste: `/sandevistan insanidade <0-100>`.
- **Pílula de Sanidade**: baixa 1 de Insanidade por segundo durante 30 s (o Imunossupressor continua só na ciberpsicose).
- Chip de RAM com durabilidade fixa de 800.
- **Sucata e componentes**: Minério de Sucata (Overworld, y -40 a 72) dá Sucata. O **Reciclador de Sucata** transforma Sucata em Componente Comum, Melhorado, de Alta Precisão, Conjunto de Engrenagens, Fios e Liga Reforçada.
- **Bancada de Montagem** (3x3, só itens do mod, receitas por quantidade, livro de receitas ao lado que preenche a bancada): faz Núcleo Computacional, Módulo de RAM, todos os implantes e tiers, Link Neural, Chipware, todos os chips e tiers, e os Sandevistan (peças de 9 itens). Os blocos de diamante, esmeralda e ouro saíram das receitas.
- **Bancada Medicinal**: cada remédio usa plantas diferentes e raras. Pílula: lírio-do-vale, orquídea azul, açúcar e Resina. Imunossupressor: Seringa, bagas luminosas, ametista, redstone, flor de esporos e alho-silvestre. Estabilizador: Seringa, mel, olho de aranha fermentado, açúcar, frutas silvestres, flor-tocha e centáurea.
- Itens novos: Barra de Solda, Placa de Circuito, Capacitor, Músculo Sintético, Resina Biocompatível, Liga Reforçada, Fluido Refrigerante e 4 Pigmentos (um por modelo colorido; feitos com corante + Fluido).
- `python3 tools/gen_stage2.py` regenera texturas, blocos, minério e receitas comuns da etapa.

### 5.0.0 - Energia (etapa 3)
- Nova aba **Energia** na tela J: encaixe de bateria e barra de Energia (também uma barrinha azul no HUD, sob a Mente). Botão direito numa bateria instala; clicar no encaixe remove.
- 5 baterias (saem vazias da Bancada de Montagem): Célula Básica 2.000, Célula de Íons 6.000, Bateria de Lítio 15.000, Alta Densidade 40.000, Núcleo de Fusão Compacto 100.000 FE. Quanto maior, mais devagar carrega.
- Compatível com FE do NeoForge: outros mods carregam as baterias e puxam energia dos nossos blocos.
- Blocos: Carregador de Baterias (botão direito com bateria insere; sem item tira; agachar mostra a carga), Gerador a Combustível (clique com carvão, lava etc.), Painel Solar (dia, céu aberto), Turbina Eólica (mais alto = mais), Gerador Térmico (15 FE/t por lava vizinha). Geradores empurram energia para blocos encostados neles (sem cabos).
- Os implantes que gastam energia chegam na etapa 5 (cada um aumenta 10% o consumo dos outros).

### 5.0.0 - Robôs (etapa 4)
- Robô Zumbi (não queima no sol), Robô Creeper (só explode), Robô Esqueleto (atira raios elétricos), Cão Mecânico (rápido e fraco), Robô de Segurança (lento, 60 de vida e armadura), Drone Sucateiro (voa e atira raios).
- Spawnam no Overworld em baixa frequência e dropam Sucata, às vezes componentes e baterias vazias. Raios causam o dobro de dano neles. Ovos de geração na aba criativa.
- Imunossupressor agora usa bagas luminosas e o Estabilizador usa frutas silvestres (sem itens do Nether).

### 5.0.0 - Implantes novos (etapa 5)
- Todos passam pelo Ripperdoc (tela de Implantes: fileira "Implantes extras" abaixo do botão), custam esmeraldas, dão 3 corações de dano e somam Carga Cibernética. Todos têm 3 tiers. Quem gasta energia exige bateria e cada um aumenta em 10% o consumo dos outros.
- Bota Voadora (um item por pé): 1 pé amortece 50/60/70% da queda e plana; 2 pés 75/80/85% e voo curto perto do chão (4/5/6 blocos). Preço 10/14/18, carga 4/5/6 cada.
- Jetpack: voo livre (pular duas vezes), mais rápido e eficiente a cada tier; com o Sandevistan ligado voa mais rápido e gasta metade da energia. Preço 16/22/28, carga 8/10/12.
- Implante de XP: +20/30/40% de experiência. Preço 8/12/16, carga 3/4/5.
- Scanner de Minérios (tecla H ou roleta): minérios (tag c:ores) através das paredes por 10 s, raio 8/12/16, recarga 35/30/25 s. Preço 10/14/18, carga 5/6/7.
- Par de olhos (só um par por vez): Zoom (2x até 4/6/8x; preço 8/12/16, carga 3/4/5), Tático (silhueta por tipo, alcance 24/32/48; 12/16/20, carga 6/7/8), Laser (carrega 1,5/1,2/0,9 s e dispara 6/8/10 de dano com fogo, recarga 3 s; 14/18/24, carga 7/8/9), Visão Noturna/Térmica (alcance 16/24/32; 10/14/18, carga 5/6/7).
- Roleta de habilidades: segure Alt e role o mouse para trocar entre Sandevistan, Scanner e Olhos; R usa a escolhida. A posição se ajusta no editor do HUD (arraste "Habilidades").
- O Ripperdoc só fala ao clicar nele, ao implantar e ao remover (sem frases ao chegar perto).
- Cargas reduzidas um pouco: braços e pernas 9/11/13 (eram 10/12/14), osso, nervos e coração 4/5/6 (eram 5/6/7).
- Receitas dos 3 tiers na Bancada de Montagem.

## 5.0.0 — Etapa 6: novos implantes, tier IV e missões do Ripperdoc

**Novos implantes (Ripperdoc, 3 tiers, Carga Cibernética):**
- **Implante Solar** (cabeça): recarrega 4/7/10 FE/s na bateria de dia e ao ar livre (metade na chuva).
- **Reator Arc** (peito): recarrega 20/35/55/**90** FE/s sempre. Receita muito difícil: Núcleo Estelar (Estrela do Nether + Sinalizador + Bobina de Fusão + Matriz de Contenção).
- **Lâminas Mantis** (um implante por braço): +5/6/7/**9** de dano corpo a corpo (como espada de ferro/diamante/netherite) com as mãos livres; as duas juntas somam +1.
- **Injetor Automático** (ombro): aplica sozinho a Pílula (Insanidade ≥ 70/60/50) e o Imunossupressor (ciberpsicose ≥ 40/35/30). Segurança: uma injeção por conferência, Pílula com espera de 40 s e só sem pílula agindo; Imunossupressor com espera de 180 s, só sem dose/sequência em andamento — nunca com ciberpsicose baixa. Liga/desliga na roleta.

**Tier IV:** só Jetpack, Reator Arc e Lâminas Mantis. Crafts extremamente difíceis (Wither, Sinalizador, Núcleo Estelar, Fragmentos de Eco...).

**Licenças (missões do Ripperdoc):** botão *Missões* na conversa com o Ripperdoc. As missões liberam na Bancada os crafts dos tiers II, III e IV (receitas bloqueadas aparecem em vermelho com o tier exigido). Missões longas: robôs, tempo de Sandevistan ligado, chefes (Guardião Ancestral, Warden, Wither) e entrega de peças. Progresso e licença ficam salvos e sobrevivem à morte.

**Energia:** aba J → Energia lista as baterias do inventário (clique para instalar; clique na instalada para tirar). Ao acabar a energia aparece só uma mensagem no chat.

## 5.0.0 — Etapa 7 e correções
- **Modelos 3D no corpo:** jetpack nas costas (chama quando voa), botas, painel solar na cabeça, Reator Arc no peito, Lâminas Mantis nos dois antebraços, injetor no ombro esquerdo. Olhos sem modelo.
- **Som de voo:** "ligando" (cerca de 1 s) e depois o som constante em repetição enquanto voa (botas: mais agudo e mais baixo). Vem do vídeo enviado.
- **Remédios com tiers 1–3:** Estabilizador (recupera 8/12/16 de Mente máxima), Pílula (1/1,5/2 de Insanidade por segundo), Imunossupressor (tira até 40/55/70 de psicose, +20/30/40 de Mente atual). Tier II e III dependem das licenças do Ripperdoc. Preços: Estabilizador 10 e Imunossupressor 8 esmeraldas.
- **Bugs:** a Mente máxima baixa não mexe mais na psicose (era o que fazia ela subir sozinha e não descer). O tick rate agora lê a taxa real do servidor a cada tick, então o Sandevistan reaplica se o Sharingan mudar por cima e a Mente não gasta de uma vez.

## Modelos GLB (jetpack_utrm e Lamina Mantis)
- `tools/gen_models.py <jetpack.glb> <mantis.glb>` converte os modelos para `assets/sandevistan/models/gear/*.bin` + texturas.
- `client/MeshModel` le as malhas; `GearLayer` desenha jetpack (costas) e Mantis (bracos, 3a pessoa).
- `client/MantisHand`: em primeira pessoa, com a mao vazia e Mantis no braco, a mao vira a "mao de Mantis".

## Notas desta rodada (5.0.0)
- Aba de implantes (Ripperdoc e J): encaixes agrupados por parte do corpo, lado a lado; dois encaixes por braço/perna/cabeça/costas.
- Bancada: lista de receitas à esquerda (botão abre/fecha); clicar mostra os ingredientes e "Preencher".
- Carregador e gerador a combustível agora têm GUI; roda de habilidades com liga/desliga da Lâmina Mantis e dos olhos.
- HUD: barra de Energia acima da de Mente.

- Baterias: 2.000 / 6.000 / 10.000 / 15.000 / 30.000 FE; cada uma pede a anterior para ser feita.
- Geradores não guardam energia: entregam direto ao Carregador encostado (máx. 5 FE/t).
- JEI: receitas das bancadas aparecem (dependência compileOnly do JEI em build.gradle).
