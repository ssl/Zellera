package zel.wired.variable

internal object InternalTable {
    const val ROWS = """
        -1000 1 -10 101000000 @current_time
        -1006 1 -10 101000000 @current_time.day_of_month
        -1005 1 -10 101000000 @current_time.day_of_week
        -1007 1 -10 101000000 @current_time.day_of_year
        -1004 1 -10 101000000 @current_time.hour_of_day
        -1001 1 -10 101000000 @current_time.milliseconds_of_seconds
        -1003 1 -10 101000000 @current_time.minute_of_hour
        -1009 1 -10 101000000 @current_time.month_of_year
        -1002 1 -10 101000000 @current_time.seconds_of_minute
        -1008 1 -10 101000000 @current_time.week_of_year
        -1010 1 -10 101000000 @current_time.year
        -300 1 -10 101000000 @furni_count
        -381 1 -10 101000000 @group_id
        -380 1 -10 101000000 @room_id
        -322 1 -10 101100000 @teams.blue.score
        -332 1 -10 101000000 @teams.blue.size
        -321 1 -10 101100000 @teams.green.score
        -331 1 -10 101000000 @teams.green.size
        -320 1 -10 101100000 @teams.red.score
        -330 1 -10 101000000 @teams.red.size
        -323 1 -10 101100000 @teams.yellow.score
        -333 1 -10 101000000 @teams.yellow.size
        -301 1 -10 101000000 @user_count
        -310 1 -10 101000000 @wired_timer
        -211 1 1 101000000 @achievement_score
        -233 1 1 101000000 @altitude
        -272 1 1 001000000 @bot_id
        -260 1 1 001000000 @dance
        -232 1 1 101100000 @direction
        -241 1 1 001000000 @effect
        -250 1 1 001000000 @favourite_group_id
        -206 1 1 101000000 @gender
        -245 1 1 011101011 profile:icon.variable
        -240 1 1 011100000 @handitem
        -221 1 1 000000000 @has_rights
        -200 1 1 101000000 @index
        -242 1 1 000000000 @is_frozen
        -222 1 1 000000000 @is_group_admin
        -220 1 1 000000000 @is_hc
        -262 1 1 000000000 @is_idle
        -243 1 1 000000000 @is_muted
        -223 1 1 000000000 @is_owner
        -244 1 1 000000000 @is_trading
        -210 1 1 101000000 @level
        -271 1 1 001000000 @pet_id
        -280 1 1 001001000 @pet_owner_id
        -234 1 1 101100000 @position
        -230 1 1 101100000 @position.x
        -231 1 1 101100000 @position.y
        -266 1 1 101000000 @room_entry.method
        -267 1 1 001000000 @room_entry.teleport_id
        -261 1 1 001000000 @sign
        -238 1 1 001000000 @team.color
        -237 1 1 001100000 @team.score
        -239 1 1 001000000 @team.type
        -285 1 1 000000000 @transaction.can_accept
        -283 1 1 001000000 @transaction.contract_id
        -286 1 1 001000000 @transaction.current_multiplier
        -281 1 1 000000000 @transaction.in_trade
        -282 1 1 001000000 @transaction.start_time
        -284 1 1 001000000 @transaction.state
        -205 1 1 101000000 @type
        -270 1 1 001000000 @user_id
        ~502 3 1 001001000 ~horse.controller_user_id
        ~500 3 1 000001000 ~horse.has_saddle
        ~501 3 1 000001000 ~horse.is_riding
        ~431 3 1 001001000 ~pet.creation_time
        ~402 3 1 001001000 ~pet.energy
        ~401 3 1 001001000 ~pet.experience
        ~411 3 1 001001000 ~pet.experience_required
        ~403 3 1 001001000 ~pet.happiness
        ~400 3 1 001001000 ~pet.level
        ~412 3 1 001001000 ~pet.max_energy
        ~413 3 1 001001000 ~pet.max_happiness
        ~410 3 1 001001000 ~pet.max_level
        ~430 3 1 001001000 ~pet.owner_id
        ~420 3 1 001001000 ~pet.scratches
        ~610 3 1 000001000 ~plant.can_breed
        ~611 3 1 000001000 ~plant.can_harvest
        ~612 3 1 000001000 ~plant.can_revive
        ~602 3 1 001001000 ~plant.color
        ~613 3 1 000001000 ~plant.is_dead
        ~614 3 1 000001000 ~plant.is_growing
        ~617 3 1 001001000 ~plant.max_wellbeing_seconds
        ~600 3 1 001001000 ~plant.rarity_level
        ~616 3 1 001001000 ~plant.remaining_growing_seconds
        ~615 3 1 001001000 ~plant.remaining_wellbeing_seconds
        ~601 3 1 001001000 ~plant.shape
        -123 1 0 101100000 @altitude
        -133 1 0 000000000 @can_lay_on
        -2000 1 0 010001000 @can_move_freely
        -132 1 0 000000000 @can_sit_on
        -131 1 0 000000000 @can_stand_on
        -101 1 0 101000000 @class_id
        -140 1 0 101000000 @dimensions.x
        -141 1 0 101000000 @dimensions.y
        -102 1 0 101000000 @height
        -100 1 0 101000000 @id
        -124 1 0 000000000 @is_invisible
        -130 1 0 000000000 @is_stackable
        -126 1 0 101100000 @occupation
        -180 1 0 101000000 @owner_id
        -125 1 0 101100000 @position
        -120 1 0 101100000 @position.x
        -121 1 0 101100000 @position.y
        -2102 1 0 001010000 @projectile.animation.furni_collisions
        -2106 1 0 000010000 @projectile.animation.is_travelling
        -2105 1 0 001000000 @projectile.animation.position.altitude
        -2103 1 0 001000000 @projectile.animation.position.x
        -2104 1 0 001000000 @projectile.animation.position.y
        -2100 1 0 001010000 @projectile.animation.tiles_travelled
        -2101 1 0 001010000 @projectile.animation.user_collisions
        -122 1 0 101100000 @rotation
        -110 1 0 101100000 @state
        -129 1 0 101000000 @type
        -190 1 0 001100000 @wallitem_offset
        ~115 3 0 010001000 ~area_hide.hiding_wallitems
        ~116 3 0 010001000 ~area_hide.inverted
        ~114 3 0 010001000 ~area_hide.is_invisible_furni
        ~113 3 0 001101000 ~area_hide.length
        ~110 3 0 001101000 ~area_hide.root_x
        ~111 3 0 001101000 ~area_hide.root_y
        ~112 3 0 001101000 ~area_hide.width
        ~100 3 0 001101000 ~background_color.hue
        ~102 3 0 001101000 ~background_color.lightness
        ~101 3 0 001101000 ~background_color.saturation
        ~130 3 0 001001000 ~chest.available_amount
        ~133 3 0 001001000 ~chest.capacity
        ~132 3 0 000001000 ~chest.is_auto_lock
        ~135 3 0 000001000 ~chest.is_donatable
        ~134 3 0 000001000 ~chest.is_open
        ~131 3 0 000001000 ~chest.locked
        ~122 3 0 000001000 ~clock.is_game_aware
        ~121 3 0 001101000 ~clock.pulse_count
        ~120 3 0 001001000 ~clock.state
        ~146 3 0 001101000 ~recolorable_furni.color.rgb
        ~142 3 0 001001000 ~recolorable_furni.color.rgb.blue
        ~141 3 0 001001000 ~recolorable_furni.color.rgb.green
        ~140 3 0 001001000 ~recolorable_furni.color.rgb.red
        ~103 3 0 001101000 ~teleport.target_id
        ~104 3 0 010001000 ~walk_helper.is_multi_walk
        -451 1 -20 001000000 @event.chat.style
        -450 1 -20 001000000 @event.chat.type
        -452 1 -20 001001000 profile:icon.eventVariable
        -453 1 -20 001000000 @event.link.source_room_id
        -420 1 -20 001000000 @event.signal.antenna_id
        -472 1 -20 001000000 @event.transaction_complete.deposit.coins_count
        -471 1 -20 001000000 @event.transaction_complete.deposit.furni_count
        -470 1 -20 001000000 @event.transaction_complete.multiplier
        -474 1 -20 001000000 @event.transaction_complete.withdrawal.coins_count
        -473 1 -20 001000000 @event.transaction_complete.withdrawal.furni_count
        -478 1 -20 001000000 @event.transaction_failed.reason
        -459 1 -20 001000000 @event.variable_update.box_id
        -464 1 -20 001000000 @event.variable_update.change_origin
        -460 1 -20 001000000 @event.variable_update.change_type
        -463 1 -20 001000000 @event.variable_update.difference
        -462 1 -20 001000000 @event.variable_update.new_value
        -461 1 -20 001000000 @event.variable_update.old_value
        -400 1 -20 101000000 @selector_furni_count
        -401 1 -20 101000000 @selector_user_count
        -410 1 -20 101000000 @signal_furni_count
        -411 1 -20 101000000 @signal_user_count
    """

    const val TEXTS = """
        -1005 1 Monday
        -1005 2 Tuesday
        -1005 3 Wednesday
        -1005 4 Thursday
        -1005 5 Friday
        -1005 6 Saturday
        -1005 7 Sunday
        -1009 1 January
        -1009 2 February
        -1009 3 March
        -1009 4 April
        -1009 5 May
        -1009 6 June
        -1009 7 July
        -1009 8 August
        -1009 9 September
        -1009 10 October
        -1009 11 November
        -1009 12 December
        -260 1 Dance
        -260 2 Pogo Mogo
        -260 3 Duck Funck
        -260 4 The Rollie
        -232 0 N
        -232 1 NE
        -232 2 E
        -232 3 SE
        -232 4 S
        -232 5 SW
        -232 6 W
        -232 7 NW
        -206 -1 Unknown
        -206 0 M
        -206 1 F
        -266 0 Default
        -266 2 Teleport
        -266 3 Room Network
        -261 0 0
        -261 1 1
        -261 2 2
        -261 3 3
        -261 4 4
        -261 5 5
        -261 6 6
        -261 7 7
        -261 8 8
        -261 9 9
        -261 10 10
        -261 11 |
        -261 12 ª
        -261 13 !
        -261 14 Football
        -261 15 :)
        -261 16 Red card
        -261 17 Yellow card
        -238 1 Red
        -238 2 Green
        -238 3 Blue
        -238 4 Yellow
        -239 0 Battle TileClaim
        -239 1 Freeze
        -239 2 Tagging
        -239 3 Swimming
        -239 4 Wired
        -284 1 Adding Items
        -284 2 Confirmed
        -205 1 User
        -205 2 Pet
        -205 3 Old bot
        -205 4 Bot
        -129 0 Normal
        -129 1 BC
        -129 2 Temp
        ~120 0 Initial
        ~120 1 Running
        ~120 2 Paused
        -451 0 Default
        -451 1 Generic
        -451 2 Robot
        -451 130 Team Red
        -451 3 Normal Red
        -451 131 Team Blue
        -451 4 Normal Blue
        -451 132 Team Yellow
        -451 5 Normal Yellow
        -451 133 Team Green
        -451 6 Normal Green
        -451 7 Normal Grey
        -451 8 Fortune Teller
        -451 9 Zombie Hand
        -451 10 Skeleton
        -451 11 Normal Sky Blue
        -451 12 Normal Pink
        -451 13 Normal Purple
        -451 14 Normal Dark Yellow
        -451 15 Normal Dark Turquoise
        -451 16 Hearts
        -451 17 Gothicrose
        -451 18 Stickingplaster
        -451 19 Piglet
        -451 20 Sausagedog
        -451 21 Firingmylazer
        -451 22 Dragon
        -451 23 Staff
        -451 24 Bats
        -451 25 Console
        -451 26 Steampunk Pipe
        -451 27 Storm
        -451 28 Parrot
        -451 29 Pirate
        -451 30 Bot Guide
        -451 31 Bot Rentable
        -451 32 Skeleton Stock
        -451 33 Bot Frank Large
        -451 34 Notification
        -451 35 Goat
        -451 36 Santa
        -451 37 Ambassador
        -451 38 Radio
        -451 200 Notification Red
        -451 201 Notification Green
        -451 202 Notification Blue
        -451 210 Notification Alert
        -451 211 Notification Info
        -451 212 Notification Warning
        -451 220 Notification Wrong
        -451 221 Notification Wrong Circle
        -451 222 Notification Correct
        -451 223 Notification Correct Circle
        -451 224 Notification Question Mark
        -451 225 Notification Question Mark Circle
        -451 226 Notification Arrow Up
        -451 227 Notification Arrow Up Circle
        -451 228 Notification Arrow Down
        -451 229 Notification Arrow Down Circle
        -451 250 Notification Skull
        -451 251 Notification Skull 2
        -451 252 Notification Magnifier
        -451 253 Wired Link Speaker
        -450 0 Talk
        -450 1 Shout
        -450 2 Whisper
        -478 0 Cancelled By User
        -478 1 Invalid Trade
        -478 2 Timeout
        -478 3 Cancelled By Wired
        -478 4 Already Trading
        -478 5 Wired Misconfiguration
        -478 6 No Sufficient Funds
        -478 7 Funds No Longer Available
        -478 8 User Cant Trade
        -478 9 Chest Owner Cant Trade
        -478 10 Empty Transaction
        -478 12 Feature Disabled
        -478 13 Chest Not In Room
        -478 14 Too Many Chests
        -478 15 No Wired Chests Or Locked
        -478 16 Can Not Give All To Multiple Users
        -478 17 Trade Limit Wired
        -478 18 Rate Limit
        -478 19 At Capacity
        -478 20 Misconfig Invalid Multiplier
        -478 21 Misconfig Too Many Or No Contracts
        -478 22 Misconfig No Users
        -478 23 Misconfig Invalid Timeout
        -478 24 Outdated Client Version
        -478 25 User Left Room
        -478 1000 Internal Error
        -478 1001 Internal Error Db
        -478 1002 Internal Error Reload Required
        -464 0 In Room
        -464 1 Another Room
        -464 2 Inspection
        -464 3 External
        -460 0 Created
        -460 1 Value changed
        -460 2 Deleted
    """
}
