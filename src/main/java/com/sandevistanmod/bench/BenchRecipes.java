package com.sandevistanmod.bench;

import com.sandevistanmod.data.SandevistanData;
import com.sandevistanmod.energy.BatteryItem;
import com.sandevistanmod.registry.ModRegistries;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

/** Receitas das bancadas: sem formato (so as quantidades importam) e a bancada precisa estar com exatamente esses itens. */
public final class BenchRecipes {
    private BenchRecipes() {}

    /** Um ingrediente: quantos, como reconhecer o item, o icone mostrado e (opcional) a chave de idioma do nome generico. */
    public record Req(Predicate<ItemStack> test, int count, ItemStack icon, String key) {}

    public record Recipe(List<Req> in, ItemStack out) {}

    private static final Map<Integer, List<Recipe>> CACHE = new ConcurrentHashMap<>();

    public static List<Recipe> of(int kind) {
        return CACHE.computeIfAbsent(kind, BenchRecipes::build);
    }

    /** A bancada deste tipo aceita este item em algum slot? */
    public static boolean accepts(int kind, ItemStack st) {
        for (Recipe r : of(kind)) {
            for (Req q : r.in()) if (q.test().test(st)) return true;
        }
        return false;
    }

    // ---------- construcao ----------
    private static Req i(ItemLike it, int n) {
        Item item = it.asItem();
        return new Req(st -> st.is(item), n, new ItemStack(item), null);
    }

    private static Recipe r(ItemLike out, int n, Req... in) {
        return new Recipe(Arrays.asList(in), new ItemStack(out.asItem(), n));
    }

    private static Req[] join(Req first, Req... rest) {
        Req[] out = new Req[rest.length + 1];
        out[0] = first;
        System.arraycopy(rest, 0, out, 1, rest.length);
        return out;
    }

    private static Req[] cat(Req[] a, Req[] b) {
        Req[] out = new Req[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }

    private static List<Recipe> build(int kind) {
        List<Recipe> out = new ArrayList<>();
        if (kind == BenchBlock.MEDICINAL) {
            // cada remedio usa plantas diferentes (e mais dificeis de achar), alem de ingredientes do mod
            // Pilula de Sanidade: flores calmantes (lirio-do-vale e orquidea azul do pantano)
            out.add(r(ModRegistries.PILULA.get(), 4, i(Items.LILY_OF_THE_VALLEY, 2), i(Items.BLUE_ORCHID, 2),
                    i(Items.SUGAR, 1)));
            // Imunossupressor: flor de esporos (cavernas exuberantes) e alho-silvestre
            out.add(r(ModRegistries.IMUNOSSUPRESSOR.get(), 1, i(ModRegistries.SERINGA, 1), i(Items.GLOW_BERRIES, 1),
                    i(Items.SUGAR, 1), i(Items.ALLIUM, 2)));
            // Estabilizador Neural: flor-tocha (farejador) e centaurea
            out.add(r(ModRegistries.ESTABILIZADOR.get(), 1, i(ModRegistries.SERINGA, 1), i(Items.HONEY_BOTTLE, 1),
                    i(Items.FERMENTED_SPIDER_EYE, 1), i(Items.SUGAR, 1), i(Items.SWEET_BERRIES, 1)));
            // tiers II e III (liberados pelas licencas do Ripperdoc): partem do remedio do tier anterior
            out.add(r(ModRegistries.PILULA_2.get(), 2, i(ModRegistries.PILULA, 2),
                    i(Items.AZURE_BLUET, 1)));
            out.add(r(ModRegistries.PILULA_3.get(), 2, i(ModRegistries.PILULA_2, 2), i(Items.TORCHFLOWER, 1)));
            out.add(r(ModRegistries.IMUNOSSUPRESSOR_2.get(), 1, i(ModRegistries.IMUNOSSUPRESSOR, 1), i(Items.HONEY_BOTTLE, 2),
                    i(Items.SUGAR, 2), i(Items.FERMENTED_SPIDER_EYE, 1)));
            out.add(r(ModRegistries.IMUNOSSUPRESSOR_3.get(), 1, i(ModRegistries.IMUNOSSUPRESSOR_2, 1),
                    i(Items.SPORE_BLOSSOM, 1), i(Items.SUGAR, 3), i(Items.ALLIUM, 1)));
            out.add(r(ModRegistries.ESTABILIZADOR_2.get(), 1, i(ModRegistries.ESTABILIZADOR, 1), i(Items.HONEY_BOTTLE, 1),
                    i(Items.CORNFLOWER, 2)));
            out.add(r(ModRegistries.ESTABILIZADOR_3.get(), 1, i(ModRegistries.ESTABILIZADOR_2, 1), i(Items.TORCHFLOWER, 1),
                    i(Items.SUGAR, 2), i(Items.CORNFLOWER, 1)));
        } else if (kind == BenchBlock.RECYCLER) {
            // metalurgia: carbono do carvao + ferro = aco (sucata: clique direito no Reciclador com ela na mao)
            out.add(r(ModRegistries.CARBONO.get(), 2, i(Items.COAL, 1)));
            out.add(r(ModRegistries.LINGOTE_ACO.get(), 1, i(Items.IRON_INGOT, 1), i(ModRegistries.CARBONO, 2)));
            out.add(r(ModRegistries.COMP_COMUM.get(), 3, i(ModRegistries.LINGOTE_ACO, 2)));
            out.add(r(ModRegistries.COMP_MELHORADO.get(), 2, i(ModRegistries.LINGOTE_ACO, 3), i(ModRegistries.COMP_COMUM, 2), i(ModRegistries.SOLDA, 1)));
            out.add(r(ModRegistries.COMP_ALTA.get(), 2, i(ModRegistries.LINGOTE_ACO, 4), i(ModRegistries.COMP_MELHORADO, 3),
                    i(ModRegistries.CAPACITOR, 1), i(ModRegistries.FLUIDO, 1)));
            out.add(r(ModRegistries.ENGRENAGENS.get(), 2, i(ModRegistries.LINGOTE_ACO, 1), i(ModRegistries.COMP_COMUM, 2)));
            out.add(r(ModRegistries.FIOS.get(), 3, i(ModRegistries.LINGOTE_ACO, 1), i(Items.COPPER_INGOT, 2)));
            out.add(r(ModRegistries.LIGA.get(), 2, i(ModRegistries.LINGOTE_ACO, 2), i(Items.IRON_INGOT, 2)));
        } else {
            assembly(out);
        }
        return List.copyOf(out);
    }

    private static void assembly(List<Recipe> out) {
        Req comum3 = i(ModRegistries.COMP_COMUM, 3);
        Req melh3 = i(ModRegistries.COMP_MELHORADO, 3);
        Req alta3 = i(ModRegistries.COMP_ALTA, 3);
        Req sol = i(ModRegistries.SOLDA, 1);
        Req cap = i(ModRegistries.CAPACITOR, 1);
        Req placa = i(ModRegistries.PLACA, 1);
        Req musc2 = i(ModRegistries.MUSCULO, 2);
        Req servo = i(ModRegistries.SERVO, 1);
        Req bat = i(ModRegistries.BATERIA, 1);
        Req liga = i(ModRegistries.LIGA, 1);
        Req eng = i(ModRegistries.ENGRENAGENS, 1);
        Req eng2 = i(ModRegistries.ENGRENAGENS, 2);
        Req fios2 = i(ModRegistries.FIOS, 2);
        Req fibra = i(ModRegistries.FIBRA, 1);
        Req cristal = i(ModRegistries.CRISTAL, 1);
        Req resina = i(ModRegistries.RESINA, 1);
        Req fluido = i(ModRegistries.FLUIDO, 1);
        Req chip = i(ModRegistries.CHIP, 1);

        // componentes de computacao
        out.add(r(ModRegistries.NUCLEO_COMP.get(), 1, i(ModRegistries.COMP_MELHORADO, 2), i(ModRegistries.PLACA, 2), cap));
        out.add(r(ModRegistries.MODULO_RAM.get(), 1, i(ModRegistries.PLACA, 2), cap, i(ModRegistries.COMP_MELHORADO, 1)));

        // baterias (saem vazias; carregue no Carregador de Baterias)
        Req cap2 = i(ModRegistries.CAPACITOR, 2);
        Req bat2 = i(ModRegistries.BATERIA, 2);
        out.add(new Recipe(Arrays.asList(comum3, bat, placa, sol), BatteryItem.empty(ModRegistries.battery(1))));
        out.add(new Recipe(Arrays.asList(i(ModRegistries.COMP_COMUM, 2), i(ModRegistries.COMP_MELHORADO, 2), bat2, placa, sol, i(ModRegistries.battery(1), 1)),
                BatteryItem.empty(ModRegistries.battery(2))));
        out.add(new Recipe(Arrays.asList(melh3, cap2, bat2, liga, sol, i(ModRegistries.battery(2), 1)), BatteryItem.empty(ModRegistries.battery(3))));
        out.add(new Recipe(Arrays.asList(alta3, cap2, bat2, liga, fluido, i(ModRegistries.battery(3), 1)), BatteryItem.empty(ModRegistries.battery(4))));
        out.add(new Recipe(Arrays.asList(alta3, i(ModRegistries.NUCLEO_COMP, 1), cap2, fluido, liga, bat, i(ModRegistries.battery(4), 1)),
                BatteryItem.empty(ModRegistries.battery(5))));

        // implantes extras (3 tiers; o Ripperdoc instala). II = o anterior + melhorados; III = o anterior + alta precisao
        Req cristal2 = i(ModRegistries.CRISTAL, 2);
        Req nucleo = i(ModRegistries.NUCLEO_COMP, 1);
        Req matriz1 = i(ModRegistries.MATRIZ_CONTENCAO, 1);
        Req olho = i(Items.SPIDER_EYE, 1);
        Req melh2 = i(ModRegistries.COMP_MELHORADO, 2);
        Req alta2 = i(ModRegistries.COMP_ALTA, 2);
        Req placa2 = i(ModRegistries.PLACA, 2);
        out.add(r(ModRegistries.BOTAS.get(0).get(), 1, musc2, servo, i(ModRegistries.LIGA, 2), cap, melh2, i(Items.BLAZE_ROD, 2), i(Items.IRON_BLOCK, 1)));
        out.add(r(ModRegistries.BOTAS.get(1).get(), 1, i(ModRegistries.BOTAS.get(0).get(), 1), musc2, melh3, cap, i(Items.BLAZE_ROD, 2), chip));
        out.add(r(ModRegistries.BOTAS.get(2).get(), 1, i(ModRegistries.BOTAS.get(1).get(), 1), musc2, alta3, cap, i(Items.BLAZE_ROD, 2), chip));
        out.add(r(ModRegistries.JETPACKS.get(0).get(), 1, i(ModRegistries.LIGA, 3), i(ModRegistries.SERVO, 2), fluido, cap2, melh3,
                i(Items.BLAZE_ROD, 4), i(ModRegistries.FIOS, 1), i(ModRegistries.PLACA, 3), i(Items.IRON_BLOCK, 2)));
        out.add(r(ModRegistries.JETPACKS.get(1).get(), 1, i(ModRegistries.JETPACKS.get(0).get(), 1), i(ModRegistries.LIGA, 2), alta2, fluido,
                i(Items.BLAZE_ROD, 4), i(ModRegistries.FIOS, 1), i(ModRegistries.PLACA, 3), i(Items.IRON_BLOCK, 2), nucleo));
        out.add(r(ModRegistries.JETPACKS.get(2).get(), 1, i(ModRegistries.JETPACKS.get(1).get(), 1), alta3, nucleo, cap2,
                i(Items.BLAZE_ROD, 4), i(ModRegistries.FIOS, 1), i(ModRegistries.PLACA, 3), i(Items.IRON_BLOCK, 2)));
        out.add(r(ModRegistries.IMPLANTE_XP.get(0).get(), 1, cristal, placa, resina, comum3, i(Items.EXPERIENCE_BOTTLE, 1)));
        out.add(r(ModRegistries.IMPLANTE_XP.get(1).get(), 1, i(ModRegistries.IMPLANTE_XP.get(0).get(), 1), melh3, cap, i(Items.EXPERIENCE_BOTTLE, 1), chip));
        out.add(r(ModRegistries.IMPLANTE_XP.get(2).get(), 1, i(ModRegistries.IMPLANTE_XP.get(1).get(), 1), alta3, cap, i(Items.EXPERIENCE_BOTTLE, 1), chip));
        out.add(r(ModRegistries.SCANNER.get(0).get(), 1, cristal2, placa2, melh2, cap, matriz1));
        out.add(r(ModRegistries.SCANNER.get(1).get(), 1, i(ModRegistries.SCANNER.get(0).get(), 1), alta2, cap, matriz1));
        out.add(r(ModRegistries.SCANNER.get(2).get(), 1, i(ModRegistries.SCANNER.get(1).get(), 1), alta3, nucleo, matriz1));
        // pares de olhos: tier I
        out.add(r(ModRegistries.OLHOS.get(0).get(0).get(), 1, cristal2, servo, placa, comum3, olho));
        out.add(r(ModRegistries.OLHOS.get(1).get(0).get(), 1, cristal, placa2, melh2, cap, fios2, olho));
        out.add(r(ModRegistries.OLHOS.get(2).get(0).get(), 1, cristal2, cap2, melh3, placa, olho));
        out.add(r(ModRegistries.OLHOS.get(3).get(0).get(), 1, cristal2, fluido, placa, melh2, fios2, olho));
        // olhos: tiers II e III
        Req[][] eyeT2 = {{cristal, melh2, servo}, {placa2, melh2, fios2}, {cap2, melh2, fluido}, {cristal, melh2, fluido}};
        Req[][] eyeT3 = {{cristal2, alta2, placa}, {alta2, cap2, nucleo}, {alta3, cap2, nucleo}, {alta2, placa2, nucleo}};
        for (int e = 0; e < 4; e++) {
            List<Req> two = new java.util.ArrayList<>();
            two.add(i(ModRegistries.OLHOS.get(e).get(0).get(), 1));
            two.addAll(Arrays.asList(eyeT2[e]));
            two.add(olho);
            out.add(r(ModRegistries.OLHOS.get(e).get(1).get(), 1, two.toArray(new Req[0])));
            List<Req> three = new java.util.ArrayList<>();
            three.add(i(ModRegistries.OLHOS.get(e).get(1).get(), 1));
            three.addAll(Arrays.asList(eyeT3[e]));
            three.add(olho);
            three.add(chip);
            out.add(r(ModRegistries.OLHOS.get(e).get(2).get(), 1, three.toArray(new Req[0])));
        }

        // ---- itens dificeis: bobina, matriz e o Nucleo Estelar (Estrela do Nether + Sinalizador) ----
        Req cap2b = i(ModRegistries.CAPACITOR, 2);
        Req bobina = i(ModRegistries.BOBINA_FUSAO, 1);
        Req matriz = i(ModRegistries.MATRIZ_CONTENCAO, 1);
        Req estrela = i(Items.NETHER_STAR, 1);
        Req sinal = i(Items.BEACON, 1);
        Req estelar = i(ModRegistries.NUCLEO_ESTELAR, 1);
        out.add(r(ModRegistries.BOBINA_FUSAO.get(), 1, i(ModRegistries.battery(5), 1), nucleo, i(ModRegistries.LIGA, 3), cap2b, fluido));
        out.add(r(ModRegistries.MATRIZ_CONTENCAO.get(), 1, alta3, nucleo, cristal2, cap2b, placa));
        out.add(r(ModRegistries.NUCLEO_ESTELAR.get(), 1, estrela, sinal, bobina, matriz, alta3));
        // Reator Arc (peito): I = Nucleo Estelar; II/III/IV pedem mais estrelas e pecas dificeis
        out.add(r(ModRegistries.REATORES.get(0).get(), 1, estelar, matriz, i(ModRegistries.LIGA, 3), i(ModRegistries.FLUIDO, 2)));
        out.add(r(ModRegistries.REATORES.get(1).get(), 1, i(ModRegistries.REATORES.get(0).get(), 1), estrela, bobina, matriz));
        out.add(r(ModRegistries.REATORES.get(2).get(), 1, i(ModRegistries.REATORES.get(1).get(), 1), estrela, sinal,
                i(ModRegistries.BOBINA_FUSAO, 2), i(ModRegistries.MATRIZ_CONTENCAO, 2)));
        out.add(r(ModRegistries.REATORES.get(3).get(), 1, i(ModRegistries.REATORES.get(2).get(), 1), i(ModRegistries.NUCLEO_ESTELAR, 2),
                i(ModRegistries.BOBINA_FUSAO, 2), i(ModRegistries.MATRIZ_CONTENCAO, 2), alta3));
        // Jetpack IV
        out.add(r(ModRegistries.JETPACKS.get(3).get(), 1, i(ModRegistries.JETPACKS.get(2).get(), 1), estrela, bobina, matriz, alta3, nucleo));
        // Implante Solar (cabeca)
        out.add(r(ModRegistries.SOLARES.get(0).get(), 1, i(ModRegistries.PAINEL_SOLAR, 1), placa2, cristal, melh2, cap));
        out.add(r(ModRegistries.SOLARES.get(1).get(), 1, i(ModRegistries.SOLARES.get(0).get(), 1), i(ModRegistries.PAINEL_SOLAR, 1), melh3, chip));
        out.add(r(ModRegistries.SOLARES.get(2).get(), 1, i(ModRegistries.SOLARES.get(1).get(), 1), i(ModRegistries.PAINEL_SOLAR, 1), alta3, cap, chip));
        // Laminas Mantis (a mesma receita serve para um braco; faca duas para os dois)
        out.add(r(ModRegistries.MANTIS.get(0).get(), 1, i(Items.DIAMOND_SWORD, 1), i(Items.IRON_INGOT, 5), i(ModRegistries.LIGA, 2), musc2, servo,
                i(ModRegistries.COMP_ALTA, 3), i(ModRegistries.CHIP, 3)));
        out.add(r(ModRegistries.MANTIS.get(1).get(), 1, i(ModRegistries.MANTIS.get(0).get(), 1), alta3, i(ModRegistries.LIGA, 2), servo,
                i(Items.DIAMOND_SWORD, 1), nucleo));
        out.add(r(ModRegistries.MANTIS.get(2).get(), 1, i(ModRegistries.MANTIS.get(1).get(), 1), alta3, i(ModRegistries.LIGA, 3), fluido));
        out.add(r(ModRegistries.MANTIS.get(3).get(), 1, i(ModRegistries.MANTIS.get(2).get(), 1), i(Items.ECHO_SHARD, 3), alta3, matriz, nucleo,
                i(ModRegistries.LIGA, 3)));
        // Injetor automatico (ombro)
        out.add(r(ModRegistries.INJETORES.get(0).get(), 1, i(ModRegistries.SERINGA, 2), servo, placa, resina, cap, melh2));
        out.add(r(ModRegistries.INJETORES.get(1).get(), 1, i(ModRegistries.INJETORES.get(0).get(), 1), melh3, cristal, chip));
        out.add(r(ModRegistries.INJETORES.get(2).get(), 1, i(ModRegistries.INJETORES.get(1).get(), 1), alta3, nucleo, chip));

        // implantes (tier I: componentes comuns; II: o implante anterior + melhorados; III: o anterior + alta precisao)
        int[] slots = {SandevistanData.SLOT_ARM_R, SandevistanData.SLOT_LEG_R, SandevistanData.SLOT_BONE,
                SandevistanData.SLOT_NERVE, SandevistanData.SLOT_HEART};
        Req[][] t1 = {
                {musc2, servo, bat, liga, sol},
                {musc2, i(ModRegistries.ENGRENAGENS, 2), servo, sol},
                {i(ModRegistries.LIGA, 2), i(ModRegistries.RESINA, 2), eng, sol},
                {fios2, i(ModRegistries.FIBRA, 2), resina, sol},
                {musc2, bat, fluido, placa, sol}};
        Req[][] t2 = {
                {musc2, servo, liga, sol},
                {musc2, eng2, sol},
                {i(ModRegistries.LIGA, 2), i(ModRegistries.RESINA, 2), sol},
                {fios2, i(ModRegistries.FIBRA, 2), sol},
                {musc2, bat, fluido, sol}};
        Req[][] t3 = {
                {musc2, liga, cap, sol},
                {musc2, eng, cap, sol},
                {i(ModRegistries.LIGA, 2), resina, cap, sol},
                {fios2, cristal, cap, sol},
                {musc2, cap, placa, sol}};
        for (int k = 0; k < slots.length; k++) {
            boolean leg = slots[k] == SandevistanData.SLOT_LEG_R;
            boolean heart = slots[k] == SandevistanData.SLOT_HEART;
            boolean bone = slots[k] == SandevistanData.SLOT_BONE;
            boolean nerve = slots[k] == SandevistanData.SLOT_NERVE;
            Req[] base1 = bone ? new Req[] {i(ModRegistries.COMP_COMUM, 6), i(Items.BONE, 3)} : nerve ? new Req[] {melh3} : leg ? new Req[] {melh3, i(Items.IRON_INGOT, 1)} : heart ? new Req[] {melh3} : new Req[] {comum3};
            out.add(new Recipe(Arrays.asList(cat(base1, t1[k])), new ItemStack(ModRegistries.tierImplant(slots[k], 1))));
            out.add(new Recipe(Arrays.asList(cat(join(i(ModRegistries.tierImplant(slots[k], 1), 1), melh3), t2[k])),
                    new ItemStack(ModRegistries.tierImplant(slots[k], 2))));
            out.add(new Recipe(Arrays.asList(cat(join(i(ModRegistries.tierImplant(slots[k], 2), 1), alta3, chip), t3[k])),
                    new ItemStack(ModRegistries.tierImplant(slots[k], 3))));
        }
        // Link Neural (sem tiers)
        out.add(r(ModRegistries.LINK_NEURAL.get(), 1, i(ModRegistries.CRISTAL, 2), i(ModRegistries.FIBRA, 2), resina, placa, chip, sol,
                i(ModRegistries.COMP_COMUM, 1)));

        // chipware e chips
        out.add(r(ModRegistries.CHIPWARE.get(), 1, chip, cristal, fibra, i(ModRegistries.PLACA, 2), i(ModRegistries.COMP_COMUM, 2), sol, cap));
        Req chipware = i(ModRegistries.CHIPWARE, 1);
        Req comum2 = i(ModRegistries.COMP_COMUM, 2);
        Req[][] c1 = {
                {i(ModRegistries.MUSCULO, 1), bat, cap, servo, sol, placa},
                {i(ModRegistries.ESTABILIZADOR, 1), cristal, resina, placa, sol, fibra},
                {servo, i(ModRegistries.NUCLEO, 1), eng2, liga, sol},
                {servo, i(ModRegistries.MUSCULO, 1), eng, cap, sol, placa},
                {i(ModRegistries.MODULO_RAM, 2), placa, cap, sol, bat}};
        int[] types = {SandevistanData.CHIP_MARTIAL, SandevistanData.CHIP_PAIN, SandevistanData.CHIP_MINING,
                SandevistanData.CHIP_WALKER, SandevistanData.CHIP_RAM};
        for (int k = 0; k < types.length; k++) {
            int type = types[k];
            out.add(new Recipe(Arrays.asList(cat(join(chipware, comum2), c1[k])), new ItemStack(ModRegistries.chipItem(type, 1))));
            if (!SandevistanData.chipHasTiers(type)) continue;
            boolean ram = type == SandevistanData.CHIP_RAM;
            Req[] e2 = ram ? new Req[] {i(ModRegistries.MODULO_RAM, 2), cap, sol, placa} : new Req[] {i(ModRegistries.PLACA, 2), cap, sol, fibra};
            Req[] e3 = ram ? new Req[] {i(ModRegistries.MODULO_RAM, 2), cap, cristal, sol} : new Req[] {i(ModRegistries.PLACA, 2), cap, cristal, sol};
            out.add(new Recipe(Arrays.asList(cat(join(i(ModRegistries.chipItem(type, 1), 1), melh3), e2)), new ItemStack(ModRegistries.chipItem(type, 2))));
            out.add(new Recipe(Arrays.asList(cat(join(i(ModRegistries.chipItem(type, 2), 1), alta3), e3)), new ItemStack(ModRegistries.chipItem(type, 3))));
        }

        // Ferramenta Multiuso e modulos da Mesa de Upgrades (3 tiers por modulo)
        Req nucleo2 = i(ModRegistries.NUCLEO_COMP, 1);
        out.add(r(ModRegistries.MULTITOOL.get(), 1, i(Items.DIAMOND_PICKAXE, 1), i(Items.DIAMOND_AXE, 1), i(Items.DIAMOND_SHOVEL, 1),
                i(Items.NETHERITE_INGOT, 1), i(ModRegistries.LIGA, 3), i(ModRegistries.LINGOTE_ACO, 4), i(ModRegistries.NUCLEO_COMP, 1)));
        out.add(r(ModRegistries.MOD_VELOCIDADE.get(0).get(), 1, servo, cap, i(ModRegistries.LINGOTE_ACO, 2), chip, i(Items.REDSTONE, 4)));
        out.add(r(ModRegistries.MOD_DANO.get(0).get(), 1, i(ModRegistries.LIGA, 2), fios2, i(ModRegistries.LINGOTE_ACO, 2), chip, i(Items.DIAMOND, 1)));
        out.add(r(ModRegistries.MOD_EFICIENCIA.get(0).get(), 1, servo, fios2, placa, i(ModRegistries.LINGOTE_ACO, 2), chip));
        out.add(r(ModRegistries.MOD_FORTUNA.get(0).get(), 1, i(ModRegistries.NUCLEO_COMP, 1), i(Items.EMERALD, 4), i(Items.DIAMOND, 2), i(ModRegistries.CHIP, 2), i(ModRegistries.LINGOTE_ACO, 3)));
        Req[] up2 = {i(Items.REDSTONE_BLOCK, 1), i(Items.DIAMOND, 2), i(Items.AMETHYST_SHARD, 4), i(Items.EMERALD, 4)};
        Req[] up3 = {i(Items.GLOWSTONE, 2), i(Items.NETHERITE_INGOT, 1), i(Items.DIAMOND, 2), i(Items.EMERALD_BLOCK, 1)};
        for (int t = 0; t < 4; t++) {
            List<net.neoforged.neoforge.registries.DeferredItem<net.minecraft.world.item.Item>> mods = ModRegistries.MODULOS.get(t);
            out.add(r(mods.get(1).get(), 1, i(mods.get(0), 1), melh3, servo, cap, up2[t]));
            out.add(r(mods.get(2).get(), 1, i(mods.get(1), 1), alta3, nucleo2, chip, up3[t]));
        }

        // Sandevistan Apogee: tres pecas (9 itens cada) e a montagem final
        out.add(r(ModRegistries.ACELERADOR.get(), 1, i(ModRegistries.COMP_ALTA, 2), eng2, servo, cap, fluido, sol, chip));
        out.add(r(ModRegistries.ESPINHA.get(), 1, i(ModRegistries.COMP_ALTA, 2), fios2, i(ModRegistries.NUCLEO, 1), bat, liga, sol, chip));
        out.add(r(ModRegistries.NEURALINK.get(), 1, i(ModRegistries.COMP_ALTA, 2), fios2, cristal, fibra, resina, sol, chip));
        out.add(r(ModRegistries.SANDEVISTANS.get(0).get(), 1, i(ModRegistries.NEURALINK, 1), i(ModRegistries.ESPINHA, 1),
                i(ModRegistries.ACELERADOR, 1), alta3, i(ModRegistries.NUCLEO_COMP, 1), liga, cap));

        // modelos coloridos: pigmento + item da assinatura do modelo + melhorados (Falcon, Dynalar, Zetatech, QianT)
        Req[] sig = {placa, cap, fluido, liga};
        for (int v = 0; v < ModRegistries.VARIANT_IDS.size(); v++) {
            Req base = i(ModRegistries.PIGMENTOS.get(v), 1);
            Req[] common = {base, sig[v], melh3, sol, chip};
            out.add(new Recipe(Arrays.asList(cat(common, new Req[] {eng2})), new ItemStack(ModRegistries.VARIANT_ACELERADOR.get(v).get())));
            out.add(new Recipe(Arrays.asList(cat(common, new Req[] {fios2})), new ItemStack(ModRegistries.VARIANT_ESPINHA.get(v).get())));
            out.add(new Recipe(Arrays.asList(cat(common, new Req[] {cristal, fibra})), new ItemStack(ModRegistries.VARIANT_NEURALINK.get(v).get())));
            out.add(r(ModRegistries.SANDEVISTANS.get(v + 1).get(), 1, i(ModRegistries.VARIANT_NEURALINK.get(v), 1),
                    i(ModRegistries.VARIANT_ESPINHA.get(v), 1), i(ModRegistries.VARIANT_ACELERADOR.get(v), 1), melh3,
                    i(ModRegistries.NUCLEO_COMP, 1), sol, fluido));
        }
    }
}
