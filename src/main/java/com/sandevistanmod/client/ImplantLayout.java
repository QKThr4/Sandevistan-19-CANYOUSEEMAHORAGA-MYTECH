package com.sandevistanmod.client;

import com.sandevistanmod.data.SandevistanData;

/**
 * Onde cada encaixe de implante fica na silhueta (Ripperdoc e aba J usam a mesma tabela). Os encaixes sao agrupados por parte do
 * corpo e ficam lado a lado numa linha: costas (Sandevistan, Jetpack, Injetor), cabeca (Link Neural, Solar, Olhos),
 * cada braco (Braco de Ferro e Lamina Mantis), cada perna (perna e bota), cortex (XP e Scanner) e coracao (coracao e Reator).
 * Indices: 0..SLOT_COUNT-1 sao os encaixes comuns; depois vem os extras (SLOT_COUNT + EXT_x).
 */
final class ImplantLayout {
    private ImplantLayout() {}

    static final int P = SandevistanData.SLOT_COUNT;
    static final int TOTAL = SandevistanData.SLOT_COUNT + SandevistanData.EXT_COUNT;
    /** Distancia entre os encaixes de um grupo e entre as linhas. */
    static final int STEP = 22;
    static final int[] ROW_Y = {26, 64, 102, 140};

    /** side: -1 coluna da esquerda, 0 centro, 1 coluna da direita. */
    record Group(String key, int side, int row, int[] slots) {}

    static final Group[] GROUPS = {
            new Group("group.back", -1, 0, new int[] {SandevistanData.SLOT_BACK, P + SandevistanData.EXT_JET, P + SandevistanData.EXT_INJECTOR}),
            new Group("group.head", 1, 0, new int[] {SandevistanData.SLOT_NEURAL, P + SandevistanData.EXT_SOLAR, P + SandevistanData.EXT_EYE}),
            new Group("group.armr", -1, 1, new int[] {SandevistanData.SLOT_ARM_R, P + SandevistanData.EXT_MANTIS_R}),
            new Group("group.arml", 1, 1, new int[] {SandevistanData.SLOT_ARM_L, P + SandevistanData.EXT_MANTIS_L}),
            new Group("group.bone", -1, 2, new int[] {SandevistanData.SLOT_BONE}),
            new Group("group.nerve", 1, 2, new int[] {SandevistanData.SLOT_NERVE}),
            new Group("group.legr", -1, 3, new int[] {SandevistanData.SLOT_LEG_R, P + SandevistanData.EXT_BOOT_R}),
            new Group("group.legl", 1, 3, new int[] {SandevistanData.SLOT_LEG_L, P + SandevistanData.EXT_BOOT_L}),
            new Group("group.cortex", 0, 2, new int[] {P + SandevistanData.EXT_XP, P + SandevistanData.EXT_XRAY}),
            new Group("group.heart", 0, 3, new int[] {SandevistanData.SLOT_HEART, P + SandevistanData.EXT_REACTOR}),
    };

    private static final int[] DX = new int[TOTAL];
    private static final int[] DY = new int[TOTAL];
    private static final int[] LABEL_X = new int[GROUPS.length];

    static {
        for (int g = 0; g < GROUPS.length; g++) {
            Group gr = GROUPS[g];
            int n = gr.slots().length;
            int width = n * STEP - 4;
            int first = gr.side() < 0 ? -124 - (n - 1) * STEP : gr.side() > 0 ? 106 : -width / 2;
            for (int k = 0; k < n; k++) {
                DX[gr.slots()[k]] = first + k * STEP;
                DY[gr.slots()[k]] = ROW_Y[gr.row()];
            }
            LABEL_X[g] = first + width / 2;
        }
    }

    static int x(int slot, int cx) {
        return cx + DX[slot];
    }

    static int y(int slot, int top) {
        return top + DY[slot];
    }

    static int labelX(int g, int cx) {
        return cx + LABEL_X[g];
    }

    static int labelY(int g, int top) {
        return top + ROW_Y[GROUPS[g].row()] + 21;
    }

    /** O encaixe esta na posicao do mouse? */
    static int slotAt(double mx, double my, int cx, int top) {
        for (int s = 0; s < TOTAL; s++) {
            int x = x(s, cx);
            int y = y(s, top);
            if (mx >= x && mx < x + 18 && my >= y && my < y + 18) return s;
        }
        return -1;
    }
}
