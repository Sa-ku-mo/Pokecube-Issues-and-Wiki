import json
import utils
import os
from utils import get_moves_index, get_move, trim, default_or_latest, url_to_id

MANUAL_RENAMES = {
    "lastout": "lash-out",
    "roud": "round"
}

LEGENDS_ARCEUS = [
    "barb-barrage",
    "bitter-malice",
    "bleakwind-storm",
    "ceaseless-edge",
    "chloroblast",
    "dire-claw",
    "esper-wing",
    "headlong-rush",
    "infernal-parade",
    "lunar-blessing",
    "mountain-gale",
    "mystical-power",
    "power-shift",
    "psyshield-bash",
    "raging-fury",
    "sandsear-storm",
    "shelter",
    "springtide-storm",
    "stone-axe",
    "take-heart",
    "triple-arrows",
    "victory-dance",
    "wave-crash",
    "wildbolt-storm",
]

SPECIAL_CONTACT = {
    "petal-dance",
    "trump-card",
    "wring-out",
    "grass-knot",
    "draining-kiss",
    "infestation",
}

PHYSICAL_RANGED = {
    "attack-order",
    "aura-wheel",
    "barrage",
    "beak-blast",
    "beat-up",
    "bone-club",
    "bone-rush",
    "bonemerang",
    "bulldoze",
    "bullet-seed",
    "diamond-storm",
    "dragon-darts",
    "drum-beating",
    "earthquake",
    "egg-bomb",
    "explosion",
    "feint",
    "fissure",
    "fling",
    "freeze-shock",
    "fusion-bolt",
    "grav-apple",
    "gunk-shot",
    "ice-shard",
    "icicle-crash",
    "icicle-spear",
    "land's-wrath",
    "leafage",
    "magnet-bomb",
    "magnitude",
    "metal-burst",
    "meteor-assault",
    "natural-gift",
    "pay-day",
    "petal-blizzard",
    "pin-missile",
    "poison-sting",
    "poltergeist",
    "precipice-blades",
    "present",
    "psycho-cut",
    "pyro-ball",
    "razor-leaf",
    "rock-blast",
    "rock-slide",
    "rock-throw",
    "rock-tomb",
    "rock-wrecker",
    "sacred-fire",
    "sand-tomb",
    "scale-shot",
    "secret-power",
    "seed-bomb",
    "self-destruct",
    "shadow-bone",
    "sky-attack",
    "smack-down",
    "sinister-arrow-raid",
    "spike-cannon",
    "spirit-shackle",
    "splintered-stormshards",
    "stone-edge",
    "thousand-arrows",
    "thousand-waves",
    "twineedle",
}

index_map = get_moves_index()

def is_english(details):
    return details.language.name == 'en'

class MoveEntry:
    def __init__(self, move) -> None:
        self.name = move.name
        self.id = move.id
        self.power = move.power
        self.pp = move.pp
        self.priority = move.priority
        self.type = move.type.name
        self.accuracy = move.accuracy
        self.target = move.target.name
        self.damage_class = move.damage_class.name
        self.names = move.names

        flavor_text = default_or_latest(move.flavor_text_entries, is_english)
        if flavor_text is not None:
            self.flavor_text = flavor_text.flavor_text

        if move.meta is not None:
            self.move_category = move.meta.category.name
            if move.meta.flinch_chance != 0:
                self.flinch_chance = move.meta.flinch_chance
            if move.meta.crit_rate != 0:
                self.crit_rate = move.meta.crit_rate
            if move.meta.drain != 0:
                self.drain = move.meta.drain
            if move.meta.healing != 0:
                self.healing = move.meta.healing
            if move.meta.stat_chance != 0:
                self.stat_chance = move.meta.stat_chance
            if move.meta.ailment_chance != 0:
                self.ailment_chance = move.meta.ailment_chance
            if move.meta.ailment is not None:
                self.ailment = move.meta.ailment.name

            self.max_hits = move.meta.max_hits
            self.min_hits = move.meta.min_hits
            self.max_turns = move.meta.max_turns
            self.min_turns = move.meta.min_turns
        else:
            print(f'no meta for {move.name}??')

        for entry in move.effect_entries:
            if entry.language.name == 'en':
                self.effect_text_extend = entry.effect
                self.effect_text_simple = entry.short_effect

        if move.effect_chance is not None:
            self.effect_chance = move.effect_chance

        keys = [x for x in self.__dict__.keys()]
        for x in keys:
            if self.__dict__[x] is None:
                del self.__dict__[x]

def convert_old_move_name(old):
    old = trim(old)

    if old in MANUAL_RENAMES:
        return MANUAL_RENAMES[old]

    for name in index_map.keys():
        if name == old:
            return name
        test = name.replace('-', '')
        if test == old:
            return name

    for name in LEGENDS_ARCEUS:
        if name == old:
            return name
        test = name.replace('-', '')
        if test == old:
            return name

    print(f'error converting old name for {old}')
    return None

def convert_moves():

    old_moves = './old/moves/moves.json'
    file = open(old_moves, 'r')
    data = file.read()
    file.close()
    old_moves = json.loads(data)
    moves_dex = {}
    for var in old_moves["moves"]:
        moves_dex[var["name"]] = var

    old_animations = './old/moves/moves_anims.json'
    file = open(old_animations, 'r')
    data = file.read()
    file.close()
    old_animations = json.loads(data)

    anims_dex = {}
    for var in old_animations["moves"]:
        anims_dex[var["name"]] = var

    lang_files = {}

    contact = []
    ranged = []

    move_entries = []

    # Dump each move, and collect langs and tags
    for name, index in index_map.items():
        move = get_move(index)
        entry = MoveEntry(move)

        # These will go in langs
        for __name in entry.names:
            _name = __name.name
            lang = utils.get('language', url_to_id(__name.language))
            key = f'{lang.iso639}_{lang.iso3166}.json'
            items = {}
            if key in lang_files:
                items = lang_files[key]
            items[f"pokemob.move.{entry.name}"] = _name
            lang_files[key] = items
        del entry.names

        # These will go in tags
        if name in SPECIAL_CONTACT:
            contact.append(name)
        elif name in PHYSICAL_RANGED:
            ranged.append(name)
        elif move.damage_class.name == "physical":
            contact.append(name)
        else:
            ranged.append(name)

        move_entries.append(entry)

        # Dump the entry file
        file = f'./new/moves/entries/{name}.json'
        if not os.path.exists(os.path.dirname(file)):
            os.makedirs(os.path.dirname(file))
        file = open(file, 'w', encoding='utf-8')
        json.dump(entry.__dict__, file, indent=2, ensure_ascii=False)
        file.close()


    # Dump the lang files
    for key, dict in lang_files.items():
        file = f'./new/assets/pokecube_moves/lang/{key}'
        if not os.path.exists(os.path.dirname(file)):
            os.makedirs(os.path.dirname(file))
        try:
            file = open(file, 'w', encoding='utf-8')
            json.dump(dict, file, indent=2, ensure_ascii=False)
            file.close()
        except Exception as err:
            print(f'error saving for {key}')
            print(err)

    # Dump animation files
    for name, value in anims_dex.items():
        new_name = convert_old_move_name(name)
        if new_name is not None:
            file = f'./new/moves/animations/{new_name}.json'
            if not os.path.exists(os.path.dirname(file)):
                os.makedirs(os.path.dirname(file))
            file = open(file, 'w', encoding='utf-8')
            json.dump(value, file, indent=2, ensure_ascii=False)
            file.close()
        else:
            print(f'unknown animation: {name}')

    # Dump ranged and contact tags
    file = f'./new/tags/pokemob_moves/contact-moves.json'
    if not os.path.exists(os.path.dirname(file)):
        os.makedirs(os.path.dirname(file))
    tag = {"replace":False,"values":contact}
    file = open(file, 'w', encoding='utf-8')
    json.dump(tag, file, indent=2, ensure_ascii=False)
    file.close()
    file = f'./new/tags/pokemob_moves/ranged-moves.json'
    if not os.path.exists(os.path.dirname(file)):
        os.makedirs(os.path.dirname(file))
    tag = {"replace":False,"values":ranged}
    file = open(file, 'w', encoding='utf-8')
    json.dump(tag, file, indent=2, ensure_ascii=False)
    file.close()

    # Print any that errored
    for name, value in moves_dex.items():
        new_name = convert_old_move_name(name)
        if new_name is None:
            print(f'unknown move: {name}')

if __name__ == "__main__":
    convert_moves()