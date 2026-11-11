#!/usr/bin/env python3
"""v5.0.0 etapa 2: texturas, modelos, blocos, minerio, receitas comuns dos itens novos e remocao das receitas que foram para a bancada."""
import json, os, glob, random
from PIL import Image, ImageDraw

BASE = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources")
A = os.path.join(BASE, "assets", "sandevistan")
D = os.path.join(BASE, "data", "sandevistan")
random.seed(7)


def dump(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(obj, f, indent=2)


def new():
    return Image.new("RGBA", (16, 16), (0, 0, 0, 0))


def outline(im, col=(18, 18, 26, 255)):
    src = im.copy()
    sp, p = src.load(), im.load()
    for x in range(16):
        for y in range(16):
            if sp[x, y][3] == 0:
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < 16 and 0 <= ny < 16 and sp[nx, ny][3] > 0:
                        p[x, y] = col
                        break
    return im


def chunk(im, pts, col, shade):
    d = ImageDraw.Draw(im)
    for (x, y) in pts:
        d.point((x, y), fill=col)
        d.point((x, y + 1), fill=shade)


# ---------------- icones dos itens ----------------
ICONS = {}


def icon(name):
    def deco(fn):
        ICONS[name] = fn
        return fn
    return deco


@icon("sucata")
def _():
    im = new(); d = ImageDraw.Draw(im)
    d.polygon([(3, 11), (5, 5), (10, 3), (13, 8), (11, 13), (6, 13)], fill=(96, 98, 106, 255))
    d.polygon([(5, 8), (8, 5), (11, 8), (9, 11)], fill=(128, 130, 138, 255))
    for p in ((4, 11), (9, 12), (12, 8), (6, 6)):
        d.point(p, fill=(170, 96, 52, 255))
    d.point((8, 8), fill=(220, 200, 90, 255))
    return outline(im)


def comp(core, edge, glow):
    im = new(); d = ImageDraw.Draw(im)
    d.rectangle([3, 3, 12, 12], fill=edge)
    d.rectangle([5, 5, 10, 10], fill=core)
    d.rectangle([7, 7, 8, 8], fill=glow)
    for k in (4, 7, 10):
        d.point((k, 2), fill=(230, 200, 90, 255)); d.point((k, 13), fill=(230, 200, 90, 255))
        d.point((2, k), fill=(230, 200, 90, 255)); d.point((13, k), fill=(230, 200, 90, 255))
    return outline(im)


ICONS["componente_comum"] = lambda: comp((40, 120, 70, 255), (30, 80, 50, 255), (170, 255, 190, 255))
ICONS["componente_melhorado"] = lambda: comp((40, 90, 170, 255), (28, 58, 110, 255), (150, 210, 255, 255))
ICONS["componente_alta_precisao"] = lambda: comp((110, 50, 170, 255), (60, 30, 100, 255), (120, 255, 240, 255))


@icon("conjunto_engrenagens")
def _():
    im = new(); d = ImageDraw.Draw(im)
    d.ellipse([3, 3, 12, 12], fill=(150, 154, 164, 255))
    for p in ((7, 1), (8, 1), (7, 14), (8, 14), (1, 7), (1, 8), (14, 7), (14, 8), (3, 3), (12, 3), (3, 12), (12, 12)):
        d.point(p, fill=(120, 124, 134, 255))
    d.ellipse([6, 6, 9, 9], fill=(40, 44, 52, 255))
    return outline(im)


@icon("fios_eletronicos")
def _():
    im = new(); d = ImageDraw.Draw(im)
    for col, off in (((220, 50, 50, 255), 0), ((50, 120, 230, 255), 3), ((240, 200, 50, 255), 6)):
        pts = [(2, 4 + off), (5, 6 + off), (8, 4 + off), (11, 6 + off), (13, 4 + off)]
        d.line(pts, fill=col)
    return outline(im)


@icon("nucleo_computacional")
def _():
    im = new(); d = ImageDraw.Draw(im)
    d.rectangle([2, 2, 13, 13], fill=(34, 38, 48, 255))
    d.rectangle([4, 4, 11, 11], fill=(20, 24, 30, 255))
    d.rectangle([6, 6, 9, 9], fill=(40, 220, 255, 255))
    d.rectangle([7, 7, 8, 8], fill=(220, 255, 255, 255))
    for k in (3, 6, 9, 12):
        d.point((k, 1), fill=(190, 160, 70, 255)); d.point((k, 14), fill=(190, 160, 70, 255))
    return outline(im)


@icon("modulo_ram")
def _():
    im = new(); d = ImageDraw.Draw(im)
    d.rectangle([1, 4, 14, 11], fill=(30, 130, 70, 255))
    for x in (3, 7, 11):
        d.rectangle([x, 5, x + 2, 8], fill=(22, 24, 28, 255))
    for x in range(2, 14, 2):
        d.point((x, 11), fill=(230, 200, 90, 255))
    return outline(im)


@icon("barra_solda")
def _():
    im = new(); d = ImageDraw.Draw(im)
    d.polygon([(2, 11), (4, 8), (14, 5), (14, 8), (12, 11)], fill=(196, 200, 208, 255))
    d.line([(4, 8), (14, 5)], fill=(236, 240, 246, 255))
    d.line([(2, 11), (12, 11)], fill=(120, 124, 134, 255))
    return outline(im)


@icon("placa_circuito")
def _():
    im = new(); d = ImageDraw.Draw(im)
    d.rectangle([2, 2, 13, 13], fill=(26, 120, 60, 255))
    for line in ([(4, 4), (4, 9), (8, 9)], [(7, 3), (11, 3), (11, 7)], [(5, 12), (10, 12)]):
        d.line(line, fill=(210, 190, 90, 255))
    d.rectangle([9, 9, 11, 10], fill=(24, 26, 30, 255))
    return outline(im)


@icon("capacitor")
def _():
    im = new(); d = ImageDraw.Draw(im)
    d.rectangle([5, 3, 10, 11], fill=(40, 90, 190, 255))
    d.rectangle([5, 3, 6, 11], fill=(90, 150, 240, 255))
    d.rectangle([5, 3, 10, 4], fill=(170, 176, 186, 255))
    d.line([(6, 12), (6, 14)], fill=(190, 194, 202, 255))
    d.line([(9, 12), (9, 14)], fill=(190, 194, 202, 255))
    return outline(im)


@icon("musculo_sintetico")
def _():
    im = new(); d = ImageDraw.Draw(im)
    for i, x in enumerate(range(3, 13, 2)):
        d.line([(x, 3), (x + (1 if i % 2 else -1), 13)], fill=(190, 40, 56, 255), width=2)
    d.line([(3, 8), (13, 8)], fill=(240, 130, 140, 255))
    return outline(im)


@icon("resina_biocompativel")
def _():
    im = new(); d = ImageDraw.Draw(im)
    d.ellipse([3, 5, 12, 13], fill=(230, 150, 40, 255))
    d.ellipse([5, 3, 9, 8], fill=(240, 180, 70, 255))
    d.point((6, 6), fill=(255, 240, 190, 255))
    return outline(im)


@icon("liga_reforcada")
def _():
    im = new(); d = ImageDraw.Draw(im)
    d.polygon([(2, 11), (5, 5), (14, 5), (11, 11)], fill=(70, 76, 94, 255))
    d.polygon([(5, 5), (14, 5), (13, 7), (4, 7)], fill=(130, 140, 164, 255))
    d.line([(6, 9), (10, 9)], fill=(40, 190, 220, 255))
    return outline(im)


@icon("fluido_refrigerante")
def _():
    im = new(); d = ImageDraw.Draw(im)
    d.rectangle([5, 2, 10, 4], fill=(150, 156, 166, 255))
    d.rectangle([4, 5, 11, 13], fill=(60, 200, 235, 255))
    d.rectangle([5, 6, 6, 12], fill=(190, 245, 255, 255))
    return outline(im)


def pigment(col, light):
    def fn():
        im = new(); d = ImageDraw.Draw(im)
        d.rectangle([4, 3, 11, 5], fill=(200, 204, 212, 255))
        d.rectangle([3, 6, 12, 13], fill=col)
        d.rectangle([4, 7, 5, 12], fill=light)
        return outline(im)
    return fn


ICONS["pigmento_falcon"] = pigment((30, 170, 80, 255), (140, 255, 170, 255))
ICONS["pigmento_dynalar"] = pigment((40, 110, 230, 255), (150, 200, 255, 255))
ICONS["pigmento_zetatech"] = pigment((230, 200, 50, 255), (255, 245, 150, 255))
ICONS["pigmento_qiant"] = pigment((235, 130, 30, 255), (255, 200, 120, 255))

for name, fn in ICONS.items():
    fn().save(os.path.join(A, "textures", "item", name + ".png"))
    dump(os.path.join(A, "models", "item", name + ".json"),
         {"parent": "minecraft:item/generated", "textures": {"layer0": "sandevistan:item/" + name}})


# ---------------- blocos ----------------
def block_img(base, noise=14):
    im = Image.new("RGBA", (16, 16), base)
    p = im.load()
    for x in range(16):
        for y in range(16):
            n = random.randint(-noise, noise)
            p[x, y] = (max(0, min(255, base[0] + n)), max(0, min(255, base[1] + n)), max(0, min(255, base[2] + n)), 255)
    return im


def frame(im, col):
    d = ImageDraw.Draw(im)
    d.rectangle([0, 0, 15, 15], outline=col)
    return im


def save_block(name, top, side, bottom):
    for suffix, im in (("top", top), ("side", side), ("bottom", bottom)):
        im.save(os.path.join(A, "textures", "block", "%s_%s.png" % (name, suffix)))
    dump(os.path.join(A, "models", "block", name + ".json"), {"parent": "minecraft:block/cube_bottom_top", "textures": {
        "top": "sandevistan:block/%s_top" % name, "side": "sandevistan:block/%s_side" % name, "bottom": "sandevistan:block/%s_bottom" % name}})


# Montagem: metal escuro com painel ciano
top = frame(block_img((52, 60, 72)), (26, 30, 38)); d = ImageDraw.Draw(top)
for r in range(3):
    for c in range(3):
        d.rectangle([3 + c * 4, 3 + r * 4, 5 + c * 4, 5 + r * 4], fill=(40, 200, 235, 255))
side = frame(block_img((58, 66, 80)), (26, 30, 38)); d = ImageDraw.Draw(side)
d.rectangle([3, 3, 12, 8], fill=(24, 28, 36, 255)); d.rectangle([4, 4, 11, 7], fill=(40, 200, 235, 255))
for y in (10, 12): d.line([(3, y), (12, y)], fill=(30, 34, 42, 255))
save_block("bancada_montagem", top, side, frame(block_img((36, 40, 48)), (20, 22, 28)))

# Medicinal: madeira com folhas
top = frame(block_img((60, 130, 62), 18), (36, 84, 40)); d = ImageDraw.Draw(top)
for (x, y, col) in ((4, 4, (240, 90, 120, 255)), (10, 5, (250, 220, 70, 255)), (6, 10, (130, 140, 250, 255)), (11, 11, (255, 255, 255, 255))):
    d.point((x, y), fill=col); d.point((x + 1, y), fill=(250, 200, 60, 255))
side = frame(block_img((140, 104, 62), 12), (84, 60, 34)); d = ImageDraw.Draw(side)
d.rectangle([5, 4, 10, 11], fill=(44, 130, 70, 255)); d.line([(7, 5), (8, 10)], fill=(150, 240, 160, 255))
d.point((7, 3), fill=(240, 90, 120, 255))
save_block("bancada_medicinal", top, side, frame(block_img((120, 88, 52), 12), (70, 50, 28)))

# Reciclador: cinza com faixa amarela
top = frame(block_img((44, 48, 56)), (22, 24, 30)); d = ImageDraw.Draw(top)
for x in range(3, 14, 3): d.line([(x, 2), (x, 13)], fill=(18, 20, 24, 255))
side = frame(block_img((88, 92, 100)), (40, 44, 52)); d = ImageDraw.Draw(side)
for k in range(-8, 16, 4): d.line([(k, 2), (k + 6, 7)], fill=(240, 200, 40, 255), width=1)
d.rectangle([5, 9, 10, 13], fill=(30, 32, 38, 255)); d.point((7, 11), fill=(220, 90, 40, 255)); d.point((8, 11), fill=(220, 90, 40, 255))
save_block("reciclador_sucata", top, side, frame(block_img((34, 36, 42)), (18, 20, 24)))

for name in ("bancada_montagem", "bancada_medicinal", "reciclador_sucata"):
    dump(os.path.join(A, "blockstates", name + ".json"), {"variants": {"": {"model": "sandevistan:block/" + name}}})
    dump(os.path.join(A, "models", "item", name + ".json"), {"parent": "sandevistan:block/" + name})
    dump(os.path.join(D, "loot_table", "blocks", name + ".json"), {"type": "minecraft:block", "pools": [{"rolls": 1, "bonus_rolls": 0,
         "entries": [{"type": "minecraft:item", "name": "sandevistan:" + name}], "conditions": [{"condition": "minecraft:survives_explosion"}]}]})

# Minerio de sucata: pedra com fragmentos enferrujados
ore = block_img((124, 124, 128), 10); p = ore.load()
for (x, y, col) in ((3, 3, (172, 96, 52)), (4, 3, (150, 80, 44)), (10, 4, (190, 190, 200)), (11, 5, (110, 114, 124)), (5, 9, (170, 96, 52)), (6, 10, (230, 200, 80)),
                    (11, 11, (150, 80, 44)), (12, 11, (190, 190, 200)), (8, 7, (110, 114, 124)), (2, 12, (230, 200, 80))):
    p[x, y] = col + (255,)
ore.save(os.path.join(A, "textures", "block", "minerio_sucata.png"))
dump(os.path.join(A, "models", "block", "minerio_sucata.json"), {"parent": "minecraft:block/cube_all", "textures": {"all": "sandevistan:block/minerio_sucata"}})
dump(os.path.join(A, "blockstates", "minerio_sucata.json"), {"variants": {"": {"model": "sandevistan:block/minerio_sucata"}}})
dump(os.path.join(A, "models", "item", "minerio_sucata.json"), {"parent": "sandevistan:block/minerio_sucata"})
dump(os.path.join(D, "loot_table", "blocks", "minerio_sucata.json"), {"type": "minecraft:block", "pools": [{"rolls": 1, "bonus_rolls": 0, "entries": [
    {"type": "minecraft:alternatives", "children": [
        {"type": "minecraft:item", "name": "sandevistan:minerio_sucata", "conditions": [{"condition": "minecraft:match_tool", "predicate": {
            "predicates": {"minecraft:enchantments": [{"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}]}}}]},
        {"type": "minecraft:item", "name": "sandevistan:sucata", "functions": [
            {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 3}},
            {"function": "minecraft:apply_bonus", "enchantment": "minecraft:fortune", "formula": "minecraft:ore_drops"},
            {"function": "minecraft:explosion_decay"}]}]}]}]})

# tags de mineracao
blocks = ["sandevistan:clinica_ripperdoc", "sandevistan:bancada_montagem", "sandevistan:bancada_medicinal", "sandevistan:reciclador_sucata", "sandevistan:minerio_sucata"]
dump(os.path.join(BASE, "data", "minecraft", "tags", "block", "mineable", "pickaxe.json"), {"values": blocks})
dump(os.path.join(BASE, "data", "minecraft", "tags", "block", "needs_stone_tool.json"), {"values": ["sandevistan:minerio_sucata"]})

# geracao do minerio no Overworld
dump(os.path.join(D, "worldgen", "configured_feature", "minerio_sucata.json"), {"type": "minecraft:ore", "config": {
    "discard_chance_on_air_exposure": 0.0, "size": 7, "targets": [
        {"state": {"Name": "sandevistan:minerio_sucata"}, "target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:stone_ore_replaceables"}},
        {"state": {"Name": "sandevistan:minerio_sucata"}, "target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:deepslate_ore_replaceables"}}]}})
dump(os.path.join(D, "worldgen", "placed_feature", "minerio_sucata.json"), {"feature": "sandevistan:minerio_sucata", "placement": [
    {"type": "minecraft:count", "count": 10}, {"type": "minecraft:in_square"},
    {"type": "minecraft:height_range", "height": {"type": "minecraft:uniform", "min_inclusive": {"absolute": -40}, "max_inclusive": {"absolute": 72}}},
    {"type": "minecraft:biome_filter"}]})
dump(os.path.join(D, "neoforge", "biome_modifier", "minerio_sucata.json"), {"type": "neoforge:add_features", "biomes": "#minecraft:is_overworld",
    "features": "sandevistan:minerio_sucata", "step": "underground_ores"})

# ---------------- receitas ----------------
RC = os.path.join(D, "recipe")
gone = ["acelerador", "espinha", "neuralink", "sandevistan", "braco_ferro", "pernas_ciberneticas", "estrutura_ossea", "sistema_nervoso", "coracao_ferro",
        "link_neural", "chipware", "chip_artes_marciais", "chip_supressao_dor", "chip_mineracao", "chip_andarilho", "chip_ram",
        "pilula_sanidade", "imunossupressor", "estabilizador_neural"]
for n in list(gone):
    for suf in ("", "_2", "_3", "_dynalar", "_falcon", "_qiant", "_zetatech"):
        f = os.path.join(RC, n + suf + ".json")
        if os.path.exists(f):
            os.remove(f)


def shapeless(name, ings, result, count=1):
    dump(os.path.join(RC, name + ".json"), {"type": "minecraft:crafting_shapeless", "category": "misc",
         "ingredients": [{"item": i} if not i.startswith("#") else {"tag": i[1:]} for i in ings], "result": {"id": "sandevistan:" + result, "count": count}})


def shaped(name, pattern, key, result):
    dump(os.path.join(RC, name + ".json"), {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": pattern,
         "key": {k: ({"item": v} if not v.startswith("#") else {"tag": v[1:]}) for k, v in key.items()}, "result": {"id": "sandevistan:" + result, "count": 1}})


shapeless("barra_solda", ["minecraft:copper_ingot", "minecraft:iron_nugget", "minecraft:redstone"], "barra_solda", 2)
shapeless("placa_circuito", ["minecraft:copper_ingot", "minecraft:redstone", "sandevistan:barra_solda"], "placa_circuito", 2)
shapeless("capacitor", ["minecraft:iron_ingot", "minecraft:redstone", "minecraft:amethyst_shard", "sandevistan:barra_solda"], "capacitor", 2)
shapeless("musculo_sintetico", ["minecraft:leather", "minecraft:string", "minecraft:string", "minecraft:slime_ball"], "musculo_sintetico")
shapeless("resina_biocompativel", ["minecraft:slime_ball", "minecraft:honey_bottle", "minecraft:bone_meal"], "resina_biocompativel", 2)
shapeless("fluido_refrigerante", ["minecraft:glass_bottle", "minecraft:ice", "minecraft:lapis_lazuli"], "fluido_refrigerante")
for v, dye in (("falcon", "green_dye"), ("dynalar", "blue_dye"), ("zetatech", "yellow_dye"), ("qiant", "orange_dye")):
    shapeless("pigmento_" + v, ["minecraft:" + dye, "sandevistan:fluido_refrigerante"], "pigmento_" + v, 2)
shaped("bancada_montagem", ["ICI", "SBS", "III"], {"I": "minecraft:iron_ingot", "C": "sandevistan:chip", "S": "sandevistan:servo_motor", "B": "minecraft:crafting_table"}, "bancada_montagem")
shaped("bancada_medicinal", [" S ", "PCP", "PPP"], {"S": "sandevistan:seringa", "P": "#minecraft:planks", "C": "minecraft:composter"}, "bancada_medicinal")
shaped("reciclador_sucata", ["IVI", "SHS", "III"], {"I": "minecraft:iron_ingot", "V": "sandevistan:ventoinha", "S": "sandevistan:servo_motor", "H": "minecraft:hopper"}, "reciclador_sucata")

# advancements: refaz todos
adv = os.path.join(D, "advancement", "recipes")
for f in glob.glob(os.path.join(adv, "*.json")):
    os.remove(f)
print("ok")
