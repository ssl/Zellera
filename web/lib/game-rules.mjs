import { readFileSync, existsSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { clientString } from './client-strings.mjs';

const flag = 'flag';
const number = 'number';
const numbers = 'numbers';
const plain = 'text';
const clock = 'clock';
const SCHEMA = {
  'reward_track.introduction.enabled': flag, 'reward_track.halloween.enabled': flag, 'reward_track.premium_for_everyone': flag,
  'reward_track.premium_pass.credits': number, 'reward_track.premium_pass.premium_price': number,
  'reward_track.premium_pass.boost_percent': number, 'reward_track.premium_pass.instant_points': number,
  'daily_tasks.enabled': flag, 'daily_gift.enabled': flag, 'respect.daily_players': number, 'respect.daily_pets': number,
  'respect.replenish_cost_points': number,
  'new_player.credits': number, 'new_player.points': number, 'new_player.premium': number, 'new_player.look': plain,
  'build_pass.membership_for_everyone': flag, 'build_pass.membership_days': number,
  'build_pass.member_furni_limit': number, 'build_pass.month_furni_bonus': number,
  'build_pass.non_member_furni_limit': number, 'build_pass.non_member_max_furni_limit': number,
  'build_pass.non_member_alone_only': flag, 'build_pass.room_lock': flag, 'build_pass.placement_warning': flag,
  'floor_plan.editor': ['permission', 'build_pass_member', 'owner'], 'floor_plan.requires_floor': flag,
  'floor_plan.max_area': number, 'floor_plan.max_open_tiles': number,
  'snowball.enabled': flag, 'snowball.free_games': ['unlimited', 'none', 'daily'],
  'snowball.daily_free_games': number, 'snowball.daily_free_games_club': number,
  'snowball.tokens_10.credits': number, 'snowball.tokens_10.premium': number,
  'snowball.tokens_100.credits': number, 'snowball.tokens_100.premium': number,
  'snowball.tokens_300.credits': number, 'snowball.tokens_300.premium': number,
  'idle.auto_sleep': flag, 'idle.sleep_seconds': number, 'idle.room_sleep_min_seconds': number, 'idle.room_sleep_max_seconds': number,
  'idle.kick_enabled': flag, 'idle.new_room_kick_seconds': number, 'idle.room_kick_min_seconds': number, 'idle.room_kick_max_seconds': number,
  'room.max_furni': number, 'room.max_notes': number, 'room.max_pets': number, 'room.max_bots': number, 'room.spectator_slots': number, 'room.enforce_category_visitors': number, 'room.max_filter_words': number, 'chat.flood_mute_seconds': number, 'word_filter.replacement': plain,
  'player.max_rooms': number, 'player.max_rooms_club': number, 'player.max_friends': number, 'player.max_friends_club': number,
  'player.max_favourite_rooms': number, 'login.enter_home_room': flag, 'login.default_room': number, 'player.name.min_length': number, 'player.name.max_length': number, 'player.name.punctuation': plain, 'account_level.hosting_divisor': number, 'achievements.resolution_days': number,
  'trading.enabled': flag, 'trading.credit_furni': ['wallet', 'inventory'], 'marketplace.enabled': flag, 'marketplace.tokens_required': flag,
  'marketplace.token_batch_price': number, 'marketplace.token_batch_size': number, 'recycler.enabled': flag, 'groups.require_club': flag, 'groups.max_members': number,
  'groups.max_memberships': number, 'groups.max_memberships_club': number, 'club.payday_enabled': flag,
  'club.monthly_gift_notice': flag, 'seasonal_currency.enabled': flag, 'seasonal_currency.types': numbers,
  'opening_hours.enabled': flag, 'opening_hours.opens_at': clock, 'opening_hours.closes_at': clock,
  'rollers.default_speed': number, 'rollers.height_tolerance_hundredths': number,
  'messenger.offline_messages_max': number,
  'pets.needs.rest_energy_divisor': number, 'pets.needs.thirst_seconds': number,
  'pets.needs.walking_sadness_seconds': number,
  'pets.mood.every_seconds': number, 'pets.mood.gesture_seconds': number, 'pets.mood.weary_energy_divisor': number,
  'pets.mood.thirsty_from': number, 'pets.mood.happy_from_happiness': number,
  'pets.mood.sad_up_to_happiness': number, 'pets.obedience.rested_percent': number,
  'pets.obedience.drained_percent': number, 'pets.obedience.drained_energy_divisor': number,
  'pets.obedience.sulks_below_happiness': number, 'pets.obedience.gap_seconds': number,
  'pets.obedience.repeat_window_seconds': number, 'pets.obedience.repeats_allowed': number,
  'pets.obedience.grumble_one_in': number, 'pets.obedience.stay_seconds': number,
  'pets.plants.rare_parent_from': number, 'pets.plants.one_seed_percent': number,
  'pets.breeding.spread_hundredths': number, 'pets.breeding.narrowing_hundredths': number,
  'pets.breeding.tier_1_up_to_level': number, 'pets.breeding.tier_2_up_to_level': number,
  'pets.breeding.tier_3_up_to_level': number, 'pets.breeding.tier_4_up_to_level': number,
  'catalog.max_items_per_order': number, 'trading.batch_cap': number,
  'rates.build_pass_place_gap_millis': number, 'rates.furni_move_gap_millis': number,
  'rates.share_sync_gap_millis': number, 'rates.wired_give_furni_gap_millis': number,
  'limits.wired_nesting': number, 'limits.wired_steps_per_firing': number, 'limits.wired_signals_per_firing': number,
  'limits.wired_signal_hops': number, 'limits.wired_signal_forwarded': number,
  'limits.wired_call_tiles': number, 'limits.wired_call_stacks': number, 'limits.frame_bytes': number,
  'limits.messages_per_kind_per_second': number, 'limits.messages_per_second': number,
  'limits.moderation_chatlog_lines': number,
  'limits.moderation_room_visits': number, 'limits.reports_open_per_player': number,
};

const typed = (key, text, problems) => {
  const type = SCHEMA[key];
  if (type === flag) {
    if (text === 'true' || text === 'false') return text === 'true';
    problems.push(`${key} must be true or false, not '${text}'`);
  } else if (type === plain) {
    if (text) return text;
    problems.push(`${key} must not be empty`);
  } else if (type === clock) {
    if (/^([01]\d|2[0-3]):[0-5]\d$/.test(text)) return text;
    problems.push(`${key} must be a time of day as HH:MM, not '${text}'`);
  } else if (type === number) {
    if (/^\d+$/.test(text)) return Number(text);
    problems.push(`${key} must be a whole number of 0 or more, not '${text}'`);
  } else if (type === numbers) {
    if (/^\d+(\s*,\s*\d+)*$/.test(text)) return text.split(',').map((part) => Number(part.trim()));
    problems.push(`${key} must list whole numbers of 0 or more, separated by commas, not '${text}'`);
  } else {
    if (type.includes(text)) return text;
    problems.push(`${key} must be one of ${type.join(', ')}, not '${text}'`);
  }
  return undefined;
};

const readLines = (text, label, problems) => {
  const values = new Map();
  text.split('\n').forEach((raw, index) => {
    const line = raw.trim();
    if (!line || line.startsWith('#')) return;
    const split = line.indexOf('=');
    if (split < 1) { problems.push(`${label}line ${index + 1} is not key=value: '${line}'`); return; }
    const key = line.slice(0, split).trim();
    if (!(key in SCHEMA)) problems.push(`${label}${key} is not a known rule`);
    else if (values.has(key)) problems.push(`${label}${key} is set twice`);
    else values.set(key, line.slice(split + 1).trim());
  });
  return values;
};

export const parseGameRules = (text, source = 'config/game-rules.conf', local = null) => {
  const problems = [];
  const values = readLines(text, '', problems);
  if (local != null) for (const [key, value] of readLines(local, 'the local file: ', problems)) values.set(key, value);
  const rules = {};
  for (const [key, value] of values) rules[key] = typed(key, value, problems);
  for (const key of Object.keys(SCHEMA)) if (!(key in rules)) problems.push(`${key} is missing`);
  if (problems.length) throw new Error(`Game rules file ${source}: ${problems.join('; ')}`);
  return Object.freeze(rules);
};

export const gameRulesFile = () => process.env.ZEL_GAME_RULES_FILE
  || fileURLToPath(new URL('../../config/game-rules.conf', import.meta.url));

export const gameRulesLocalFile = (file = gameRulesFile()) => file.replace(/(?:\.conf)?$/, '.local.conf');

let loaded = null;
export const gameRules = () => {
  if (loaded) return loaded;
  const file = gameRulesFile();
  if (!existsSync(file)) throw new Error(`Game rules file ${file} is missing; set ZEL_GAME_RULES_FILE or check it out`);
  const local = gameRulesLocalFile(file);
  loaded = parseGameRules(readFileSync(file, 'utf8'), file, existsSync(local) ? readFileSync(local, 'utf8') : null);
  return loaded;
};

const NEW_FEATURE_TRACKS = {
  rewardtrack_intro: 'reward_track.introduction.enabled',
};
const newFeatureRule = (name) => (name === `rewardtrack_${clientString('track.halloween')}`
  ? 'reward_track.halloween.enabled' : NEW_FEATURE_TRACKS[name]);

export const ruledVariables = (text, rules = gameRules()) => text
  .replace(/^notifications\.new_feature\.active=(.*)$/m, (_, list) => `notifications.new_feature.active=${
    list.split(',').filter((name) => rules[newFeatureRule(name)] !== false).join(',')}`)
  .replace(/^games_icon_enabled=true$/m, rules['snowball.enabled'] ? 'games_icon_enabled=true' : 'games_icon_enabled=false')
  .replace(/^seasonalcurrencyindicator\.enabled=.*$/m, `seasonalcurrencyindicator.enabled=${rules['seasonal_currency.enabled']}`)
  .replace(/^seasonalcurrencyindicator\.active=.*$/m, `seasonalcurrencyindicator.active=${
    rules['seasonal_currency.enabled'] ? rules['seasonal_currency.types'].join(',') : ''}`);
