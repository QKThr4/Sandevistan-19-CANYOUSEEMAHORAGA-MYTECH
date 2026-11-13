package com.sandevistanmod.registry;

import com.sandevistanmod.SandevistanMod;
import com.sandevistanmod.data.SandevistanData;
import com.sandevistanmod.bench.BenchBlock;
import com.sandevistanmod.bench.BenchMenu;
import com.sandevistanmod.energy.BatteryItem;
import com.sandevistanmod.energy.EnergyBlock;
import com.sandevistanmod.energy.EnergyBlockEntity;
import com.sandevistanmod.robot.ElectricBolt;
import com.sandevistanmod.robot.RoboCreeper;
import com.sandevistanmod.robot.RoboDog;
import com.sandevistanmod.robot.RoboDrone;
import com.sandevistanmod.robot.RoboSecurity;
import com.sandevistanmod.robot.RoboSkeleton;
import com.sandevistanmod.robot.RoboZombie;
import com.sandevistanmod.logic.ExtItem;
import com.sandevistanmod.logic.ChipItem;
import com.sandevistanmod.logic.ClinicBlock;
import com.sandevistanmod.logic.ImplantItem;
import com.sandevistanmod.logic.PillItem;
import com.sandevistanmod.logic.GoodMindEffect;
import com.sandevistanmod.logic.ImmunoItem;
import com.sandevistanmod.logic.StabilizerItem;
import com.sandevistanmod.logic.SandevistanItem;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModRegistries {
    private ModRegistries() {}

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, SandevistanMod.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, SandevistanMod.MODID);
    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, SandevistanMod.MODID);
    public static final DeferredRegister<PoiType> POIS =
            DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, SandevistanMod.MODID);
    public static final DeferredRegister<VillagerProfession> PROFESSIONS =
            DeferredRegister.create(Registries.VILLAGER_PROFESSION, SandevistanMod.MODID);
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(SandevistanMod.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SandevistanMod.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SandevistanMod.MODID);

    public static final Supplier<AttachmentType<SandevistanData>> SANDEVISTAN =
            ATTACHMENTS.register("sandevistan",
                    () -> AttachmentType.serializable(() -> new SandevistanData()).copyOnDeath().build());

    private static Supplier<SoundEvent> sound(String name) {
        return SOUNDS.register(name,
                () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(SandevistanMod.MODID, name)));
    }

    public static final Supplier<SoundEvent> SND_ON = sound("sandevistan_on");
    public static final Supplier<SoundEvent> SND_OFF = sound("sandevistan_off");
    /** Sons de ligar/desligar de todos os Sandevistans, menos o Apogee. */
    public static final Supplier<SoundEvent> SND_ON_V = sound("sandevistan_on_v");
    public static final Supplier<SoundEvent> SND_OFF_V = sound("sandevistan_off_v");
    public static final Supplier<SoundEvent> SND_JET_START = sound("jetpack_start");
    public static final Supplier<SoundEvent> SND_JET_LOOP = sound("jetpack_loop");
    public static final Supplier<SoundEvent> TINNITUS = sound("tinnitus");
    public static final Supplier<SoundEvent> PSY_SND_1 = sound("psy_snd_1");
    public static final Supplier<SoundEvent> PSY_SND_2 = sound("psy_snd_2");
    public static final Supplier<SoundEvent> ALERT = sound("alert");
    public static final Supplier<SoundEvent> PSY_MUSIC_1 = sound("psy_music_1");
    public static final Supplier<SoundEvent> PSY_MUSIC_2 = sound("psy_music_2");

    private static DeferredItem<SandevistanItem> sandevistan(String name, int variant) {
        return ITEMS.register(name, () -> new SandevistanItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC), variant));
    }

    /** Modelos de Sandevistan na ordem: Apogee, Falcon, Dynalar, Zetatech, QianT. */
    public static final List<DeferredItem<SandevistanItem>> SANDEVISTANS = List.of(
            sandevistan("sandevistan", 0), sandevistan("sandevistan_falcon", 1), sandevistan("sandevistan_dynalar", 2),
            sandevistan("sandevistan_zetatech", 3), sandevistan("sandevistan_qiant", 4));
    public static final DeferredItem<SandevistanItem> SANDEVISTAN_ITEM = SANDEVISTANS.get(0);

    public static final DeferredHolder<MobEffect, GoodMindEffect> GOOD_MIND =
            EFFECTS.register("good_mind", () -> new GoodMindEffect());

    /** Seringa vazia: base do Imunossupressor e do Estabilizador Neural; sobra depois de injetar. */
    public static final DeferredItem<Item> SERINGA = ITEMS.registerSimpleItem("seringa", new Item.Properties().stacksTo(16));
    private static Item.Properties med(int tier) {
        return new Item.Properties().stacksTo(16).rarity(tier == 3 ? Rarity.RARE : tier == 2 ? Rarity.UNCOMMON : Rarity.COMMON);
    }

    public static final DeferredItem<StabilizerItem> ESTABILIZADOR = ITEMS.register("estabilizador_neural", () -> new StabilizerItem(med(1), 1));
    public static final DeferredItem<StabilizerItem> ESTABILIZADOR_2 = ITEMS.register("estabilizador_neural_2", () -> new StabilizerItem(med(2), 2));
    public static final DeferredItem<StabilizerItem> ESTABILIZADOR_3 = ITEMS.register("estabilizador_neural_3", () -> new StabilizerItem(med(3), 3));
    public static final DeferredItem<PillItem> PILULA = ITEMS.register("pilula_sanidade", () -> new PillItem(med(1), 1));
    public static final DeferredItem<PillItem> PILULA_2 = ITEMS.register("pilula_sanidade_2", () -> new PillItem(med(2), 2));
    public static final DeferredItem<PillItem> PILULA_3 = ITEMS.register("pilula_sanidade_3", () -> new PillItem(med(3), 3));
    public static final DeferredItem<ImmunoItem> IMUNOSSUPRESSOR = ITEMS.register("imunossupressor", () -> new ImmunoItem(med(1), 1));
    public static final DeferredItem<ImmunoItem> IMUNOSSUPRESSOR_2 = ITEMS.register("imunossupressor_2", () -> new ImmunoItem(med(2), 2));
    public static final DeferredItem<ImmunoItem> IMUNOSSUPRESSOR_3 = ITEMS.register("imunossupressor_3", () -> new ImmunoItem(med(3), 3));

    public static final DeferredBlock<ClinicBlock> CLINICA = BLOCKS.register("clinica_ripperdoc",
            () -> new ClinicBlock(BlockBehaviour.Properties.of().strength(3.0f, 6.0f).sound(SoundType.METAL)
                    .lightLevel(s -> 7)));
    public static final DeferredItem<BlockItem> CLINICA_ITEM = ITEMS.register("clinica_ripperdoc",
            () -> new BlockItem(CLINICA.get(), new Item.Properties()));

    /** Posto de trabalho do Ripperdoc: o aldeao que pega o bloco da clinica vira o doutor. */
    public static final ResourceKey<PoiType> RIPPERDOC_POI_KEY = ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE,
            ResourceLocation.fromNamespaceAndPath(SandevistanMod.MODID, "ripperdoc"));
    public static final Supplier<PoiType> RIPPERDOC_POI = POIS.register("ripperdoc",
            () -> new PoiType(ImmutableSet.copyOf(CLINICA.get().getStateDefinition().getPossibleStates()), 1, 1));
    public static final Supplier<VillagerProfession> RIPPERDOC = PROFESSIONS.register("ripperdoc",
            () -> new VillagerProfession("ripperdoc",
                    holder -> holder.is(RIPPERDOC_POI_KEY), holder -> holder.is(RIPPERDOC_POI_KEY),
                    ImmutableSet.of(), ImmutableSet.of(), null));

    public static final DeferredItem<Item> CRISTAL = ITEMS.registerSimpleItem("cristal_neural", new Item.Properties());
    public static final DeferredItem<Item> FIBRA = ITEMS.registerSimpleItem("fibra_sinaptica", new Item.Properties());
    public static final DeferredItem<Item> VENTOINHA = ITEMS.registerSimpleItem("ventoinha", new Item.Properties());
    public static final DeferredItem<Item> NUCLEO = ITEMS.registerSimpleItem("nucleo", new Item.Properties());
    public static final DeferredItem<Item> SERVO = ITEMS.registerSimpleItem("servo_motor", new Item.Properties());
    public static final DeferredItem<Item> BATERIA = ITEMS.registerSimpleItem("bateria_energia", new Item.Properties());

    // ---- Sucata, componentes e itens de montagem (v5.0.0) ----
    private static DeferredItem<Item> plain(String name) {
        return ITEMS.registerSimpleItem(name, new Item.Properties());
    }

    public static final DeferredItem<Item> SUCATA = plain("sucata");
    public static final DeferredItem<Item> COMP_COMUM = plain("componente_comum");
    public static final DeferredItem<Item> COMP_MELHORADO = plain("componente_melhorado");
    public static final DeferredItem<Item> COMP_ALTA = plain("componente_alta_precisao");
    public static final DeferredItem<Item> ENGRENAGENS = plain("conjunto_engrenagens");
    public static final DeferredItem<Item> FIOS = plain("fios_eletronicos");
    public static final DeferredItem<Item> NUCLEO_COMP = plain("nucleo_computacional");
    public static final DeferredItem<Item> MODULO_RAM = plain("modulo_ram");
    public static final DeferredItem<Item> SOLDA = plain("barra_solda");
    public static final DeferredItem<Item> PLACA = plain("placa_circuito");
    public static final DeferredItem<Item> CAPACITOR = plain("capacitor");
    public static final DeferredItem<Item> MUSCULO = plain("musculo_sintetico");
    public static final DeferredItem<Item> RESINA = plain("resina_biocompativel");
    public static final DeferredItem<Item> LIGA = plain("liga_reforcada");
    public static final DeferredItem<Item> FLUIDO = plain("fluido_refrigerante");
    /** Pigmentos dos modelos coloridos, na mesma ordem de VARIANT_IDS (Falcon, Dynalar, Zetatech, QianT). */
    public static final List<DeferredItem<Item>> PIGMENTOS = List.of(plain("pigmento_falcon"), plain("pigmento_dynalar"),
            plain("pigmento_zetatech"), plain("pigmento_qiant"));

    // ---- Bancadas e minerio ----
    public static final DeferredRegister<net.minecraft.world.inventory.MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, SandevistanMod.MODID);
    public static final Supplier<net.minecraft.world.inventory.MenuType<BenchMenu>> BENCH_MENU = MENUS.register("bancada",
            () -> net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(BenchMenu::new));

    public static final Supplier<net.minecraft.world.inventory.MenuType<com.sandevistanmod.energy.EnergyMenu>> ENERGY_MENU = MENUS.register("energia",
            () -> net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(com.sandevistanmod.energy.EnergyMenu::new));

    private static DeferredBlock<BenchBlock> bench(String name, int kind) {
        return BLOCKS.register(name, () -> new BenchBlock(BlockBehaviour.Properties.of().strength(3.0f, 6.0f)
                .sound(kind == BenchBlock.MEDICINAL ? SoundType.WOOD : SoundType.METAL).requiresCorrectToolForDrops(), kind));
    }

    public static final DeferredBlock<BenchBlock> BANCADA_MONTAGEM = bench("bancada_montagem", BenchBlock.ASSEMBLY);
    public static final DeferredBlock<BenchBlock> BANCADA_MEDICINAL = bench("bancada_medicinal", BenchBlock.MEDICINAL);
    public static final DeferredBlock<BenchBlock> RECICLADOR = bench("reciclador_sucata", BenchBlock.RECYCLER);
    public static final DeferredItem<BlockItem> BANCADA_MONTAGEM_ITEM = ITEMS.register("bancada_montagem",
            () -> new BlockItem(BANCADA_MONTAGEM.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> BANCADA_MEDICINAL_ITEM = ITEMS.register("bancada_medicinal",
            () -> new BlockItem(BANCADA_MEDICINAL.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> RECICLADOR_ITEM = ITEMS.register("reciclador_sucata",
            () -> new BlockItem(RECICLADOR.get(), new Item.Properties()));
    public static final DeferredBlock<net.minecraft.world.level.block.Block> MINERIO_SUCATA = BLOCKS.register("minerio_sucata",
            () -> new net.minecraft.world.level.block.DropExperienceBlock(
                    net.minecraft.util.valueproviders.UniformInt.of(0, 2),
                    BlockBehaviour.Properties.of().strength(3.0f, 3.0f).sound(SoundType.STONE).requiresCorrectToolForDrops()));
    public static final DeferredItem<BlockItem> MINERIO_SUCATA_ITEM = ITEMS.register("minerio_sucata",
            () -> new BlockItem(MINERIO_SUCATA.get(), new Item.Properties()));

    // ---- Energia ----
    public static final DeferredRegister<net.minecraft.world.level.block.entity.BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, SandevistanMod.MODID);

    private static DeferredItem<BatteryItem> battery(String name, int type) {
        return ITEMS.register(name, () -> new BatteryItem(new Item.Properties().stacksTo(1)
                .durability(SandevistanData.BAT_CAP[type]).rarity(type >= 4 ? Rarity.RARE : Rarity.UNCOMMON), type));
    }

    /** As 5 baterias, do tipo 1 (Celula Basica) ao 5 (Nucleo de Fusao Compacto). */
    public static final List<DeferredItem<BatteryItem>> BATERIAS = List.of(
            battery("celula_basica", 1), battery("celula_ions", 2), battery("bateria_litio", 3),
            battery("bateria_alta_densidade", 4), battery("nucleo_fusao", 5));

    public static Item battery(int type) {
        return BATERIAS.get(Math.max(1, Math.min(SandevistanData.BAT_TYPES, type)) - 1).get();
    }

    private static DeferredBlock<EnergyBlock> energyBlock(String name, int kind) {
        return BLOCKS.register(name, () -> new EnergyBlock(BlockBehaviour.Properties.of().strength(3.0f, 6.0f)
                .sound(SoundType.METAL).requiresCorrectToolForDrops(), kind));
    }

    public static final DeferredBlock<EnergyBlock> CARREGADOR = energyBlock("carregador_baterias", EnergyBlock.CHARGER);
    public static final DeferredBlock<EnergyBlock> GERADOR_COMBUSTIVEL = energyBlock("gerador_combustivel", EnergyBlock.COMBUSTION);
    public static final DeferredBlock<EnergyBlock> PAINEL_SOLAR = energyBlock("painel_solar", EnergyBlock.SOLAR);
    public static final DeferredBlock<EnergyBlock> TURBINA_EOLICA = energyBlock("turbina_eolica", EnergyBlock.WIND);
    public static final DeferredBlock<EnergyBlock> GERADOR_TERMICO = energyBlock("gerador_termico", EnergyBlock.THERMAL);
    public static final List<DeferredBlock<EnergyBlock>> ENERGY_BLOCKS = List.of(CARREGADOR, GERADOR_COMBUSTIVEL, PAINEL_SOLAR,
            TURBINA_EOLICA, GERADOR_TERMICO);
    public static final List<DeferredItem<BlockItem>> ENERGY_ITEMS = List.of(
            blockItem("carregador_baterias", CARREGADOR), blockItem("gerador_combustivel", GERADOR_COMBUSTIVEL),
            blockItem("painel_solar", PAINEL_SOLAR), blockItem("turbina_eolica", TURBINA_EOLICA),
            blockItem("gerador_termico", GERADOR_TERMICO));

    private static DeferredItem<BlockItem> blockItem(String name, DeferredBlock<EnergyBlock> b) {
        return ITEMS.register(name, () -> new BlockItem(b.get(), new Item.Properties()));
    }

    public static final Supplier<net.minecraft.world.level.block.entity.BlockEntityType<EnergyBlockEntity>> ENERGY_BE =
            BLOCK_ENTITIES.register("energia", () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(
                    EnergyBlockEntity::new, CARREGADOR.get(), GERADOR_COMBUSTIVEL.get(), PAINEL_SOLAR.get(), TURBINA_EOLICA.get(),
                    GERADOR_TERMICO.get()).build(null));

    // ---- Implantes extras (3 tiers cada; instalados no Ripperdoc) ----
    private static String tierId(String base, int tier) {
        return tier == 1 ? base : base + "_" + tier;
    }

    private static List<DeferredItem<ExtItem>> extTiers(String base, int slot, int eyeType, String tip, Object[][] args) {
        List<DeferredItem<ExtItem>> out = new java.util.ArrayList<>();
        for (int t = 1; t <= SandevistanData.maxExtTier(slot); t++) {
            final int tier = t;
            final Object[] a = args == null ? new Object[0] : args[t - 1];
            final boolean blade = slot == SandevistanData.EXT_MANTIS_R || slot == SandevistanData.EXT_MANTIS_L;
            out.add(ITEMS.register(tierId(base, t), () -> {
                Item.Properties pr = new Item.Properties();
                if (blade) pr.durability(SandevistanData.MANTIS_MAX); else pr.stacksTo(1);
                pr.rarity(tier == 4 ? Rarity.EPIC : tier == 3 ? Rarity.RARE : Rarity.UNCOMMON);
                if (tier == 4) pr.fireResistant();
                return new ExtItem(pr, slot, tier, eyeType, tip, a);
            }));
        }
        return out;
    }

    public static final List<DeferredItem<ExtItem>> BOTAS = extTiers("bota_voadora", SandevistanData.EXT_BOOT_R, 0, "tooltip.ext.boot",
            new Object[][] {{50, 75}, {60, 80}, {70, 85}});
    public static final List<DeferredItem<ExtItem>> JETPACKS = extTiers("jetpack", SandevistanData.EXT_JET, 0, "tooltip.ext.jet", null);
    public static final List<DeferredItem<ExtItem>> IMPLANTE_XP = extTiers("implante_xp", SandevistanData.EXT_XP, 0, "tooltip.ext.xp",
            new Object[][] {{20}, {30}, {40}});
    public static final DeferredItem<Item> BOBINA_FUSAO = plain("bobina_fusao");
    public static final DeferredItem<Item> MATRIZ_CONTENCAO = plain("matriz_contencao");
    public static final DeferredItem<Item> NUCLEO_ESTELAR = ITEMS.register("nucleo_estelar",
            () -> new Item(new Item.Properties().rarity(Rarity.EPIC).fireResistant()));
    public static final List<DeferredItem<ExtItem>> SOLARES = extTiers("implante_solar", SandevistanData.EXT_SOLAR, 0, "tooltip.ext.solar",
            new Object[][] {{4}, {7}, {10}});
    public static final List<DeferredItem<ExtItem>> REATORES = extTiers("reator_arc", SandevistanData.EXT_REACTOR, 0, "tooltip.ext.reactor",
            new Object[][] {{20}, {35}, {55}, {90}});
    public static final List<DeferredItem<ExtItem>> MANTIS = extTiers("lamina_mantis", SandevistanData.EXT_MANTIS_R, 0, "tooltip.ext.mantis",
            new Object[][] {{5}, {6}, {7}, {9}});
    public static final List<DeferredItem<ExtItem>> INJETORES = extTiers("injetor_automatico", SandevistanData.EXT_INJECTOR, 0, "tooltip.ext.injector",
            new Object[][] {{70, 40}, {60, 35}, {50, 30}});
    public static final List<DeferredItem<ExtItem>> SCANNER = extTiers("scanner_minerios", SandevistanData.EXT_XRAY, 0, "tooltip.ext.xray",
            new Object[][] {{8, 35}, {12, 30}, {16, 25}});
    /** Pares de olhos: indice 0 Zoom, 1 Tatico, 2 Laser, 3 Visao Noturna/Termica. */
    public static final List<List<DeferredItem<ExtItem>>> OLHOS = List.of(
            extTiers("olho_zoom", SandevistanData.EXT_EYE, 1, "tooltip.ext.eye1", new Object[][] {{4}, {6}, {8}}),
            extTiers("olho_tatico", SandevistanData.EXT_EYE, 2, "tooltip.ext.eye2", new Object[][] {{24}, {32}, {48}}),
            extTiers("olho_laser", SandevistanData.EXT_EYE, 3, "tooltip.ext.eye3", new Object[][] {{6, 1.5}, {8, 1.2}, {10, 0.9}}),
            extTiers("olho_visao", SandevistanData.EXT_EYE, 4, "tooltip.ext.eye4", new Object[][] {{16}, {24}, {32}}));

    /** Item do implante extra deste encaixe, tier (1 a 3) e, para os olhos, tipo (1 a 4). */
    public static Item extItem(int slot, int tier, int eyeType) {
        int i = Math.max(1, Math.min(4, tier)) - 1;
        switch (slot) {
            case SandevistanData.EXT_BOOT_R, SandevistanData.EXT_BOOT_L:
                return BOTAS.get(Math.min(i, BOTAS.size() - 1)).get();
            case SandevistanData.EXT_JET:
                return JETPACKS.get(Math.min(i, JETPACKS.size() - 1)).get();
            case SandevistanData.EXT_XP:
                return IMPLANTE_XP.get(Math.min(i, IMPLANTE_XP.size() - 1)).get();
            case SandevistanData.EXT_XRAY:
                return SCANNER.get(Math.min(i, SCANNER.size() - 1)).get();
            case SandevistanData.EXT_SOLAR:
                return SOLARES.get(Math.min(i, SOLARES.size() - 1)).get();
            case SandevistanData.EXT_REACTOR:
                return REATORES.get(Math.min(i, REATORES.size() - 1)).get();
            case SandevistanData.EXT_MANTIS_R, SandevistanData.EXT_MANTIS_L:
                return MANTIS.get(Math.min(i, MANTIS.size() - 1)).get();
            case SandevistanData.EXT_INJECTOR:
                return INJETORES.get(Math.min(i, INJETORES.size() - 1)).get();
            default:
                return OLHOS.get(Math.max(1, Math.min(4, eyeType)) - 1).get(Math.min(i, 2)).get();
        }
    }

    // ---- Robos ----
    public static final DeferredRegister<net.minecraft.world.entity.EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, SandevistanMod.MODID);

    private static <T extends net.minecraft.world.entity.Entity> Supplier<net.minecraft.world.entity.EntityType<T>> entity(
            String name, net.minecraft.world.entity.EntityType.EntityFactory<T> factory,
            net.minecraft.world.entity.MobCategory cat, float w, float h, int range) {
        return ENTITIES.register(name, () -> net.minecraft.world.entity.EntityType.Builder.of(factory, cat).sized(w, h)
                .clientTrackingRange(range).build(name));
    }

    public static final Supplier<net.minecraft.world.entity.EntityType<RoboZombie>> ROBO_ZUMBI =
            entity("robo_zumbi", RoboZombie::new, net.minecraft.world.entity.MobCategory.MONSTER, 0.6f, 1.95f, 8);
    public static final Supplier<net.minecraft.world.entity.EntityType<RoboCreeper>> ROBO_CREEPER =
            entity("robo_creeper", RoboCreeper::new, net.minecraft.world.entity.MobCategory.MONSTER, 0.6f, 1.7f, 8);
    public static final Supplier<net.minecraft.world.entity.EntityType<RoboSkeleton>> ROBO_ESQUELETO =
            entity("robo_esqueleto", RoboSkeleton::new, net.minecraft.world.entity.MobCategory.MONSTER, 0.6f, 1.99f, 8);
    public static final Supplier<net.minecraft.world.entity.EntityType<RoboDog>> ROBO_CAO =
            entity("robo_cao", RoboDog::new, net.minecraft.world.entity.MobCategory.MONSTER, 0.6f, 0.8f, 8);
    public static final Supplier<net.minecraft.world.entity.EntityType<RoboSecurity>> ROBO_SEGURANCA =
            entity("robo_seguranca", RoboSecurity::new, net.minecraft.world.entity.MobCategory.MONSTER, 0.9f, 2.4f, 8);
    public static final Supplier<net.minecraft.world.entity.EntityType<RoboDrone>> ROBO_DRONE =
            entity("robo_drone", RoboDrone::new, net.minecraft.world.entity.MobCategory.MONSTER, 0.7f, 0.6f, 8);
    public static final Supplier<net.minecraft.world.entity.EntityType<ElectricBolt>> ELECTRIC_BOLT =
            entity("electric_bolt", ElectricBolt::new, net.minecraft.world.entity.MobCategory.MISC, 0.25f, 0.25f, 4);

    private static DeferredItem<Item> egg(String name, Supplier<? extends net.minecraft.world.entity.EntityType<? extends net.minecraft.world.entity.Mob>> type,
            int a, int b) {
        return ITEMS.register(name + "_spawn_egg", () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(type, a, b, new Item.Properties()));
    }

    public static final List<DeferredItem<Item>> ROBOT_EGGS = List.of(
            egg("robo_zumbi", ROBO_ZUMBI, 0x5C6B70, 0xE03030), egg("robo_creeper", ROBO_CREEPER, 0x4A5A52, 0x30E0E0),
            egg("robo_esqueleto", ROBO_ESQUELETO, 0xB8BEC6, 0x30A0FF), egg("robo_cao", ROBO_CAO, 0x6A7078, 0xFFB020),
            egg("robo_seguranca", ROBO_SEGURANCA, 0x2A3038, 0xFF4040), egg("robo_drone", ROBO_DRONE, 0x8890A0, 0x40FFB0));

    public static final DeferredItem<Item> CHIP = ITEMS.registerSimpleItem("chip", new Item.Properties());
    public static final DeferredItem<Item> NEURALINK = ITEMS.registerSimpleItem("neuralink", new Item.Properties());
    public static final DeferredItem<Item> ESPINHA = ITEMS.registerSimpleItem("espinha", new Item.Properties());
    public static final DeferredItem<Item> ACELERADOR = ITEMS.registerSimpleItem("acelerador", new Item.Properties());

    /** Pecas coloridas de cada modelo (Falcon, Dynalar, Zetatech, QianT): Neuralink, Espinha e Acelerador. */
    public static final List<String> VARIANT_IDS = List.of("falcon", "dynalar", "zetatech", "qiant");
    public static final List<DeferredItem<Item>> VARIANT_NEURALINK = variantParts("neuralink");
    public static final List<DeferredItem<Item>> VARIANT_ESPINHA = variantParts("espinha");
    public static final List<DeferredItem<Item>> VARIANT_ACELERADOR = variantParts("acelerador");

    private static List<DeferredItem<Item>> variantParts(String part) {
        java.util.ArrayList<DeferredItem<Item>> out = new java.util.ArrayList<>();
        for (String v : VARIANT_IDS) out.add(ITEMS.registerSimpleItem(part + "_" + v, new Item.Properties()));
        return List.copyOf(out);
    }

    private static DeferredItem<ImplantItem> implant(String name, int slot, int tier, String... tips) {
        return ITEMS.register(name, () -> new ImplantItem(new Item.Properties().stacksTo(1).rarity(
                tier == 3 ? Rarity.EPIC : Rarity.RARE), slot, tier, tips));
    }

    public static final DeferredItem<ImplantItem> BRACO_FERRO = implant("braco_ferro", SandevistanData.SLOT_ARM_R, 1,
            "tooltip.arm.1", "tooltip.arm.2", "tooltip.arm.3");
    public static final DeferredItem<ImplantItem> LINK_NEURAL = implant("link_neural", SandevistanData.SLOT_NEURAL, 1,
            "tooltip.neural.1", "tooltip.neural.2");
    public static final DeferredItem<ImplantItem> ESTRUTURA_OSSEA = implant("estrutura_ossea", SandevistanData.SLOT_BONE, 1,
            "tooltip.bone.1", "tooltip.bone.2");
    public static final DeferredItem<ImplantItem> CORACAO_FERRO = implant("coracao_ferro", SandevistanData.SLOT_HEART, 1,
            "tooltip.heart.1", "tooltip.heart.2");
    public static final DeferredItem<ImplantItem> PERNAS = implant("pernas_ciberneticas", SandevistanData.SLOT_LEG_R, 1,
            "tooltip.leg.1", "tooltip.leg.2", "tooltip.leg.3");
    public static final DeferredItem<ImplantItem> SISTEMA_NERVOSO = implant("sistema_nervoso", SandevistanData.SLOT_NERVE, 1,
            "tooltip.nerve.1", "tooltip.nerve.2");

    /** Tiers II e III de cada implante (indice 0 = tier II, 1 = tier III). */
    public static final List<DeferredItem<ImplantItem>> BRACO_FERRO_T = List.of(
            implant("braco_ferro_2", SandevistanData.SLOT_ARM_R, 2, "tooltip.arm.1", "tooltip.arm.2", "tooltip.arm.3"),
            implant("braco_ferro_3", SandevistanData.SLOT_ARM_R, 3, "tooltip.arm.1", "tooltip.arm.2", "tooltip.arm.3"));
    public static final List<DeferredItem<ImplantItem>> ESTRUTURA_OSSEA_T = List.of(
            implant("estrutura_ossea_2", SandevistanData.SLOT_BONE, 2, "tooltip.bone.1", "tooltip.bone.2"),
            implant("estrutura_ossea_3", SandevistanData.SLOT_BONE, 3, "tooltip.bone.1", "tooltip.bone.2"));
    public static final List<DeferredItem<ImplantItem>> CORACAO_FERRO_T = List.of(
            implant("coracao_ferro_2", SandevistanData.SLOT_HEART, 2, "tooltip.heart.1", "tooltip.heart.2"),
            implant("coracao_ferro_3", SandevistanData.SLOT_HEART, 3, "tooltip.heart.1", "tooltip.heart.2"));
    public static final List<DeferredItem<ImplantItem>> PERNAS_T = List.of(
            implant("pernas_ciberneticas_2", SandevistanData.SLOT_LEG_R, 2, "tooltip.leg.1", "tooltip.leg.2", "tooltip.leg.3"),
            implant("pernas_ciberneticas_3", SandevistanData.SLOT_LEG_R, 3, "tooltip.leg.1", "tooltip.leg.2", "tooltip.leg.3"));
    public static final List<DeferredItem<ImplantItem>> SISTEMA_NERVOSO_T = List.of(
            implant("sistema_nervoso_2", SandevistanData.SLOT_NERVE, 2, "tooltip.nerve.1", "tooltip.nerve.2"),
            implant("sistema_nervoso_3", SandevistanData.SLOT_NERVE, 3, "tooltip.nerve.1", "tooltip.nerve.2"));

    public static final DeferredItem<Item> CHIPWARE = ITEMS.registerSimpleItem("chipware", new Item.Properties());

    private static DeferredItem<ChipItem> chip(String name, int type, int tier, String effectKey) {
        return ITEMS.register(name, () -> new ChipItem(new Item.Properties().durability(SandevistanData.chipMax(type, tier))
                .rarity(tier == 3 ? Rarity.RARE : Rarity.UNCOMMON), type, tier, effectKey));
    }

    /** Chips tier I na ordem dos tipos 1 a 4: Artes Marciais, Supressao de Dor, Mineracao, Andarilho. */
    public static final List<DeferredItem<ChipItem>> CHIPS = List.of(
            chip("chip_artes_marciais", SandevistanData.CHIP_MARTIAL, 1, "tooltip.chip.martial"),
            chip("chip_supressao_dor", SandevistanData.CHIP_PAIN, 1, "tooltip.chip.pain"),
            chip("chip_mineracao", SandevistanData.CHIP_MINING, 1, "tooltip.chip.mining"),
            chip("chip_andarilho", SandevistanData.CHIP_WALKER, 1, "tooltip.chip.walker"),
            chip("chip_ram", SandevistanData.CHIP_RAM, 1, "tooltip.chip.ram"));
    public static final List<DeferredItem<ChipItem>> CHIPS_RAM_T = List.of(
            chip("chip_ram_2", SandevistanData.CHIP_RAM, 2, "tooltip.chip.ram"),
            chip("chip_ram_3", SandevistanData.CHIP_RAM, 3, "tooltip.chip.ram"));

    /** Tiers II e III dos chips que tem tiers: Artes Marciais, Supressao de Dor e Andarilho. */
    public static final List<DeferredItem<ChipItem>> CHIPS_MARTIAL_T = List.of(
            chip("chip_artes_marciais_2", SandevistanData.CHIP_MARTIAL, 2, "tooltip.chip.martial"),
            chip("chip_artes_marciais_3", SandevistanData.CHIP_MARTIAL, 3, "tooltip.chip.martial"));
    public static final List<DeferredItem<ChipItem>> CHIPS_PAIN_T = List.of(
            chip("chip_supressao_dor_2", SandevistanData.CHIP_PAIN, 2, "tooltip.chip.pain"),
            chip("chip_supressao_dor_3", SandevistanData.CHIP_PAIN, 3, "tooltip.chip.pain"));
    public static final List<DeferredItem<ChipItem>> CHIPS_WALKER_T = List.of(
            chip("chip_andarilho_2", SandevistanData.CHIP_WALKER, 2, "tooltip.chip.walker"),
            chip("chip_andarilho_3", SandevistanData.CHIP_WALKER, 3, "tooltip.chip.walker"));

    /** Item do chip deste tipo (1 a 4) e tier (1 a 3). */
    public static Item chipItem(int type, int tier) {
        int t = SandevistanData.chipHasTiers(type) ? SandevistanData.clampTier(tier) : 1;
        if (t == 1) return CHIPS.get(type - 1).get();
        List<DeferredItem<ChipItem>> l = type == SandevistanData.CHIP_MARTIAL ? CHIPS_MARTIAL_T
                : type == SandevistanData.CHIP_PAIN ? CHIPS_PAIN_T
                : type == SandevistanData.CHIP_RAM ? CHIPS_RAM_T : CHIPS_WALKER_T;
        return l.get(t - 2).get();
    }

    /** Item do implante dos encaixes com tiers (tier 1 a 3); null se o encaixe nao tem esse item. */
    public static Item tierImplant(int slot, int tier) {
        int t = SandevistanData.clampTier(tier);
        switch (slot) {
            case SandevistanData.SLOT_ARM_R, SandevistanData.SLOT_ARM_L:
                return t == 1 ? BRACO_FERRO.get() : BRACO_FERRO_T.get(t - 2).get();
            case SandevistanData.SLOT_BONE:
                return t == 1 ? ESTRUTURA_OSSEA.get() : ESTRUTURA_OSSEA_T.get(t - 2).get();
            case SandevistanData.SLOT_NERVE:
                return t == 1 ? SISTEMA_NERVOSO.get() : SISTEMA_NERVOSO_T.get(t - 2).get();
            case SandevistanData.SLOT_HEART:
                return t == 1 ? CORACAO_FERRO.get() : CORACAO_FERRO_T.get(t - 2).get();
            case SandevistanData.SLOT_LEG_R, SandevistanData.SLOT_LEG_L:
                return t == 1 ? PERNAS.get() : PERNAS_T.get(t - 2).get();
            default:
                return null;
        }
    }

    public static final Supplier<CreativeModeTab> TAB = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.sandevistan"))
                    .icon(() -> new ItemStack(SANDEVISTAN_ITEM.get()))
                    .displayItems((params, out) -> {
                        out.accept(CHIP.get());
                        out.accept(CRISTAL.get());
                        out.accept(FIBRA.get());
                        out.accept(VENTOINHA.get());
                        out.accept(NUCLEO.get());
                        out.accept(SERVO.get());
                        out.accept(BATERIA.get());
                        out.accept(CLINICA_ITEM.get());
                        out.accept(BANCADA_MONTAGEM_ITEM.get());
                        out.accept(BANCADA_MEDICINAL_ITEM.get());
                        out.accept(RECICLADOR_ITEM.get());
                        for (DeferredItem<BlockItem> it : ENERGY_ITEMS) out.accept(it.get());
                        for (DeferredItem<ExtItem> it : BOTAS) out.accept(it.get());
                        for (DeferredItem<ExtItem> it : JETPACKS) out.accept(it.get());
                        for (DeferredItem<ExtItem> it : IMPLANTE_XP) out.accept(it.get());
                        for (DeferredItem<ExtItem> it : SCANNER) out.accept(it.get());
                        for (DeferredItem<ExtItem> it : SOLARES) out.accept(it.get());
                        for (DeferredItem<ExtItem> it : REATORES) out.accept(it.get());
                        for (DeferredItem<ExtItem> it : MANTIS) out.accept(it.get());
                        for (DeferredItem<ExtItem> it : INJETORES) out.accept(it.get());
                        out.accept(BOBINA_FUSAO.get());
                        out.accept(MATRIZ_CONTENCAO.get());
                        out.accept(NUCLEO_ESTELAR.get());
                        for (List<DeferredItem<ExtItem>> eyes : OLHOS) for (DeferredItem<ExtItem> it : eyes) out.accept(it.get());
                        for (DeferredItem<Item> it : ROBOT_EGGS) out.accept(it.get());
                        for (DeferredItem<BatteryItem> it : BATERIAS) out.accept(BatteryItem.empty(it.get()));
                        out.accept(MINERIO_SUCATA_ITEM.get());
                        for (DeferredItem<Item> it : List.of(SUCATA, COMP_COMUM, COMP_MELHORADO, COMP_ALTA, ENGRENAGENS, FIOS,
                                NUCLEO_COMP, MODULO_RAM, SOLDA, PLACA, CAPACITOR, MUSCULO, RESINA, LIGA, FLUIDO)) out.accept(it.get());
                        for (DeferredItem<Item> it : PIGMENTOS) out.accept(it.get());
                        out.accept(NEURALINK.get());
                        out.accept(ESPINHA.get());
                        out.accept(ACELERADOR.get());
                        for (DeferredItem<Item> it : VARIANT_NEURALINK) out.accept(it.get());
                        for (DeferredItem<Item> it : VARIANT_ESPINHA) out.accept(it.get());
                        for (DeferredItem<Item> it : VARIANT_ACELERADOR) out.accept(it.get());
                        for (DeferredItem<SandevistanItem> it : SANDEVISTANS) out.accept(it.get());
                        out.accept(ESTRUTURA_OSSEA.get());
                        out.accept(SISTEMA_NERVOSO.get());
                        out.accept(CORACAO_FERRO.get());
                        out.accept(PERNAS.get());
                        out.accept(BRACO_FERRO.get());
                        out.accept(LINK_NEURAL.get());
                        out.accept(CHIPWARE.get());
                        for (DeferredItem<ChipItem> c : CHIPS) out.accept(c.get());
                        for (DeferredItem<ImplantItem> it : BRACO_FERRO_T) out.accept(it.get());
                        for (DeferredItem<ImplantItem> it : PERNAS_T) out.accept(it.get());
                        for (DeferredItem<ImplantItem> it : ESTRUTURA_OSSEA_T) out.accept(it.get());
                        for (DeferredItem<ImplantItem> it : SISTEMA_NERVOSO_T) out.accept(it.get());
                        for (DeferredItem<ImplantItem> it : CORACAO_FERRO_T) out.accept(it.get());
                        for (DeferredItem<ChipItem> c : CHIPS_MARTIAL_T) out.accept(c.get());
                        for (DeferredItem<ChipItem> c : CHIPS_PAIN_T) out.accept(c.get());
                        for (DeferredItem<ChipItem> c : CHIPS_WALKER_T) out.accept(c.get());
                        for (DeferredItem<ChipItem> c : CHIPS_RAM_T) out.accept(c.get());
                        out.accept(SERINGA.get());
                        out.accept(ESTABILIZADOR.get());
                        out.accept(ESTABILIZADOR_2.get());
                        out.accept(ESTABILIZADOR_3.get());
                        out.accept(IMUNOSSUPRESSOR.get());
                        out.accept(IMUNOSSUPRESSOR_2.get());
                        out.accept(IMUNOSSUPRESSOR_3.get());
                        out.accept(PILULA.get());
                        out.accept(PILULA_2.get());
                        out.accept(PILULA_3.get());
                    })
                    .build());
}
