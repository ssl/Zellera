package zel.wired.form

internal object DialogTable {
    const val TRIGGERS = """
        wf_trg_at_given_time 3 i=1
        wf_trg_at_time_long 3 i=0 limit=5 basic ctx=0
        wf_trg_bot_reached_avtr 14 u=100:100/200
        wf_trg_bot_reached_stf 13 f=100:100/200 u=100:100/200
        wf_trg_click_furni 18 f=100:100/200 wall
        wf_trg_click_tile 21 f=100:100/200
        wf_trg_click_user 24 i=1,0
        wf_trg_clock_counter 15 i=0,0,0 f=100:100/200
        wf_trg_collision 11
        wf_trg_enter_room 7
        wf_trg_game_ends 9
        wf_trg_game_starts 8
        wf_trg_leave_room 23
        wf_trg_period_long 12 i=10
        wf_trg_period_short 19 i=4
        wf_trg_periodically 6 i=1
        wf_trg_receive_dataframe 30 i=0,0,0,0,0,0,0,0,0,0,0,0,0 f=100:100/200 v=0,0,0,0,0,0,0,0,0,0,0,0 ctx=0
          text=2048
        wf_trg_receive_message 29 f=100:100/200
        wf_trg_recv_signal 17 f=100:100/200/201
        wf_trg_room_initializes 28
        wf_trg_says_something 0 i=0,0,1
        wf_trg_score_achieved 10 i=100,0
        wf_trg_state_changed 4 f=100:100/200 wall
        wf_trg_stuff_state 20 i=1 f=100:100/200 wall
        wf_trg_transaction_complete 25 f=100:100/200
        wf_trg_transaction_fail 26 f=100:100/200
        wf_trg_user_performs_action 16 i=0
        wf_trg_var_changed 22 i=1,1,0,7,-1,0 v=0 ctx=0
        wf_trg_walks_off_furni 2 f=100:100/200
        wf_trg_walks_on_furni 1 f=100:100/200
"""

    const val EFFECTS = """
        wf_act_adjust_clock 38 i=0,0,0,0 f=100:0/100/200/201
        wf_act_bot_clothes 26 u=100:0/100/200/201
        wf_act_bot_follow_avatar 25 i=1 u=100:0/100/200/201;0:0/200/201
        wf_act_bot_give_handitem 24 i=2 u=100:0/100/200/201;0:0/200/201
        wf_act_bot_move 22 f=100:0/100/200/201 u=100:0/100/200/201
        wf_act_bot_talk 23 i=0,-1,0 u=100:0/100/200/201 ctx=7
        wf_act_bot_talk_to_avatar 27 i=0,-1,0 u=100:0/100/200/201;0:0/200/201 ctx=7
        wf_act_bot_teleport 21 f=100:0/100/200/201 u=100:0/100/200/201
        wf_act_call_stacks 18 f=100:0/100/200/201
        wf_act_cancel_transaction 48 i=0 f=100:0/100/200/201 u=0:0/200/201
        wf_act_change_var_val 41 i=1,0,0,0,0,-10,0 f=0:0/100/101/200/201;200:0/100/101/200/201
          u=0:0/200/201;200:0/200/201 v=0,0 wall ctx=0
        wf_act_chase 11 f=100:0/100/200/201
        wf_act_click_conf 54 i=0,0 u=0:0/200/201
        wf_act_control_clock 28 i=0 f=100:0/100/200/201
        wf_act_flee 12 f=100:0/100/200/201
        wf_act_freeze 31 i=0,0 u=0:0/200/201
        wf_act_furni_to_furni 34 f=0:0/100/101/200/201;100:0/100/101/200/201 wall
        wf_act_furni_to_user 35 f=100:0/100/200/201 u=0:0/200/201
        wf_act_give_currency 45 i=0,1,0,-20,1,11 f=100:0/100/101/200/201;200:0/100/101/200/201
          u=0:0/200/201;200:0/200/201 v=0 wall ctx=0
        wf_act_give_furni 46 i=0,1,0,-20,1,0 f=100:0/100/101/200/201;200:0/100/101/200/201
          u=0:0/200/201;200:0/200/201 v=0 wall ctx=0
        wf_act_give_reward 17 i=0,0,0,0 u=0:0/200/201 limit=0 ctx=0
        wf_act_give_score 6 i=5,0 u=0:0/200/201
        wf_act_give_score_tm 14 i=5,0,1
        wf_act_give_var 39 i=1,0,0,0,0,-10,0 f=0:0/100/101/200/201;200:0/100/101/200/201 u=0:0/200/201;200:0/200/201
          v=0,0 wall ctx=0
        wf_act_init_transaction 47 i=0,10,0,-20,0,300
          f=100:0/100/101/200/201;101:0/100/101/200/201;200:0/100/101/200/201 u=0:0/200/201;200:0/200/201 v=0,0,0
          wall ctx=0
        wf_act_join_team 9 i=1,0 u=0:0/200/201
        wf_act_kick_user 19 u=0:0/200/201
        wf_act_leave_team 10 u=0:0/200/201
        wf_act_log 49 i=0
        wf_act_match_to_sshot 3 i=0,0,0,0,1 f=100:0/100/200/201 wall
        wf_act_move_furni_as_group 57 i=1,0,0 f=100:0/100/101/200/201;0:0/100/101/200/201 u=0:0/200/201 wall
        wf_act_move_furni_to 16 i=0,1 f=100:100/0/200/201 fs=0 ctx=0
        wf_act_move_rotate 4 i=0,0,1 f=100:0/100/200/201 wall
        wf_act_move_rotate_user 42 i=-1,-1 u=0:0/200/201
        wf_act_move_to_dir 13 i=0,0,1 f=100:0/100/200/201
        wf_act_mute_triggerer 20 i=5 u=0:0/200/201 limit=5 ctx=0
        wf_act_neg_call_stacks 36 f=100:0/100/200/201
        wf_act_neg_log 50 i=0
        wf_act_neg_send_signal 37 i=0,0 f=100:0/100/101/200/201;200:0/100/101/200/201 u=200:0/200/201
        wf_act_place_furni 55 i=0,0,0,0,0,0,0,0,0,-20
          f=110:0/100/101/110/200/201;0:0/100/101/200/201;200:0/100/101/200/201 u=0:0/200/201;200:0/200/201 v=0,0
          wall ctx=0
        wf_act_rel_mov 33 i=0,0 f=100:0/100/200/201 wall
        wf_act_remove_furni 56 f=200:0/200/201
        wf_act_remove_var 40 i=1 f=100:0/100/200/201 u=0:0/200/201 v=0 wall ctx=0
        wf_act_reset_timers 1
        wf_act_send_dataframe 64
          i=0,0,0,-20,0,0,-20,0,0,-20,0,0,-20,0,0,-20,0,0,-20,0,0,-20,0,0,-20,0,0,-20,0,0,-20,0,0,-20,0,0,-20
          f=100:0/100/101/200/201;200:0/100/101/200/201;200:0/100/101/200/201;200:0/100/101/200/201
          f=200:0/100/101/200/201;200:0/100/101/200/201;200:0/100/101/200/201;200:0/100/101/200/201
          f=200:0/100/101/200/201;200:0/100/101/200/201;200:0/100/101/200/201;200:0/100/101/200/201
          f=200:0/100/101/200/201
          u=0:0/200/201;0:0/200/201;0:0/200/201;0:0/200/201;0:0/200/201;0:0/200/201;0:0/200/201
          u=0:0/200/201;0:0/200/201;0:0/200/201;0:0/200/201;0:0/200/201 v=0,0,0,0,0,0,0,0,0,0,0,0 wall ctx=0,7
          text=6000
        wf_act_send_message 63 i=0 f=100:0/100/200/201 ctx=7
        wf_act_send_signal 30 i=0,0 f=100:0/100/101/200/201;200:0/100/101/200/201 u=200:0/200/201
        wf_act_set_altitude 29 i=0,0 f=100:0/100/200/201 wall
        wf_act_set_highlight 62 i=0,0,100,16771584,0,4,0 f=100:0/100/200/201 u=0:0/200/201;0:0/200/201 wall
        wf_act_set_visibility 61 i=0,100,0,4,0 f=100:0/100/200/201 u=0:0/200/201;0:0/200/201 wall
        wf_act_show_message 7 i=0,202,-1 u=0:0/200/201 ctx=7
        wf_act_teleport_to 8 i=0 f=100:0/100/200/201 u=0:0/200/201
        wf_act_teleport_to_room 44 f=100:0/100/200/201 u=0:0/200/201
        wf_act_toggle_state 0 i=0 f=100:0/100/200/201 wall
        wf_act_toggle_to_rnd 15 f=100:0/100/200/201 wall
        wf_act_unfreeze 32 u=0:0/200/201
        wf_act_user_to_furni 43 i=0 f=100:0/100/200/201 u=0:0/200/201
"""

    const val CONDITIONS = """
        wf_cnd_actor_dir 37 i=0 u=0:0/200/201 q=2
        wf_cnd_actor_in_group 10 u=0:0/200/201 q=2
        wf_cnd_actor_in_team 6 i=0 u=0:0/200/201 q=2
        wf_cnd_chest_has_item_type 46 i=0,0,-20,2 f=100:0/100/101/200/201;101:0/100/101/200/201;0:0/100/101/200/201
          u=0:0/200/201 v=0 wall q=1 ctx=0
        wf_cnd_chest_has_items 45 i=0,0,-20,2 f=100:0/100/200/201;200:0/100/200/201 u=200:0/200/201 v=0 wall q=1
          ctx=0
        wf_cnd_counter_time_matches 35 i=0,0,0,0 f=100:0/100/200/201 q=1
        wf_cnd_date_rng_active 24 i=0,0
        wf_cnd_furnis_hv_avtrs 1 i=1 f=100:0/100/200/201
        wf_cnd_has_altitude 36 i=0,0 f=100:0/100/200/201 wall q=1
        wf_cnd_has_furni_on 7 i=0 f=100:0/100/200/201
        wf_cnd_has_handitem 25 i=2 u=0:0/200/201 q=2
        wf_cnd_has_var 40 i=1 f=100:0/100/200/201 u=0:0/200/201 v=0 wall q=3 ctx=0
        wf_cnd_match_date 29 i=0,0,127,1,1,4095,0,0
        wf_cnd_match_snapshot 0 i=0,0,0,0 f=100:0/100/200/201 wall q=1
        wf_cnd_match_time 28 i=0,0,0,0,0,0,0,0,0
        wf_cnd_neg_has_var 41 i=1 f=100:0/100/200/201 u=0:0/200/201 v=0 wall q=3 invert ctx=0
        wf_cnd_not_furni_on 18 i=0 f=100:0/100/200/201
        wf_cnd_not_has_handitem 30 i=2 u=0:0/200/201 q=2 invert
        wf_cnd_not_hv_avtrs 14 i=0 f=100:0/100/200/201 invert
        wf_cnd_not_in_group 21 u=0:0/200/201 q=2 invert
        wf_cnd_not_in_team 17 i=0 u=0:0/200/201 q=2 invert
        wf_cnd_not_match_snap 13 i=0,0,0,0 f=100:0/100/200/201 wall q=1 invert
        wf_cnd_not_stuff_is 19 f=0:0/100/101/200/201;100:0/100/101/200/201 wall q=1 invert
        wf_cnd_not_trggrer_on 15 f=100:0/100/200/201 u=0:0/200/201 q=2 invert
        wf_cnd_not_triggerer_match 27 i=1 u=0:0/200/201;101:0/101/200/201 q=2 invert
        wf_cnd_not_user_count 16 i=1,50 invert
        wf_cnd_not_user_performs_action 33 i=0 u=0:0/200/201 q=2 invert
        wf_cnd_not_wearing_b 22 i=1 u=0:0/200/201 limit=5 ctx=0
        wf_cnd_not_wearing_fx 23 i=0,1 u=0:0/200/201 limit=5 ctx=0
        wf_cnd_slc_quantity 38 i=0,5,0 f=200:0/200/201 u=200:0/200/201
        wf_cnd_stuff_is 8 f=0:0/100/101/200/201;100:0/100/101/200/201 wall q=1
        wf_cnd_team_has_rank 31 i=0,0 u=0:0/200/201 q=2
        wf_cnd_team_has_score 34 i=0,0,0 u=0:0/200/201 q=2
        wf_cnd_time_less_than 4 i=20
        wf_cnd_time_more_than 3 i=20
        wf_cnd_trggrer_on_frn 2 f=100:0/100/200/201 u=0:0/200/201 q=2
        wf_cnd_triggerer_match 26 i=1 u=0:0/200/201;101:0/101/200/201 q=2
        wf_cnd_user_count_in 5 i=1,50
        wf_cnd_user_performs_action 32 i=0 u=0:0/200/201 q=2
        wf_cnd_valid_moves 39
        wf_cnd_var_age_match 43 i=1,0,0,0,0,1,0,-10 f=0:0/100/101/200/201;200:0/100/101/200/201
          u=0:0/200/201;200:0/200/201 v=0,0 wall q=3 ctx=0
        wf_cnd_var_val_match 42 i=1,2,0,0,0,-10,0 f=0:0/100/101/200/201;200:0/100/101/200/201
          u=0:0/200/201;200:0/200/201 v=0,0 wall q=3 ctx=0
        wf_cnd_wearing_badge 11 i=1 u=0:0/200/201 limit=5 ctx=0
        wf_cnd_wearing_effect 12 i=0,1 u=0:0/200/201 limit=5 ctx=0
"""

    const val SELECTORS = """
        wf_slc_furni_altitude 16 i=0,0
        wf_slc_furni_area 7 i=0,0,0,0
        wf_slc_furni_bytype 0 i=0 f=100:0/100/201 wall
        wf_slc_furni_neighborhood 6 i=1,0,0,33554431,0,0,0,0,0,0,0,0,0,0,0,0,0,0 f=100:0/100/201 u=0:0/201
        wf_slc_furni_onfurni 4 i=0 f=100:0/100/201
        wf_slc_furni_picks 1 wall
        wf_slc_furni_signal 5
        wf_slc_furni_with_var 17 i=2,0,0,0,-10 f=100:0/100/201 u=0:0/201 v=0,0 wall ctx=0
        wf_slc_remote 19 i=0,0 f=100:0/100/201
        wf_slc_users_area 13 i=0,0,0,0
        wf_slc_users_byaction 9 i=0
        wf_slc_users_byname 11
        wf_slc_users_bytype 2 i=1
        wf_slc_users_group 15
        wf_slc_users_handitem 14 i=2
        wf_slc_users_neighborhood 12 i=1,0,0,33554431,0,0,0,0,0,0,0,0,0,0,0,0,0,0 f=100:0/100/201 u=0:0/201
        wf_slc_users_onfurni 8 f=100:0/100/201
        wf_slc_users_signal 10
        wf_slc_users_team 3 i=0
        wf_slc_users_with_var 18 i=2,0,0,0,-10 f=100:0/100/201 u=0:0/201 v=0,0 wall ctx=0
"""

    const val ADDONS = """
        wf_xtra_achievement_enabler 2001 limit=0 basic ctx=0
        wf_xtra_anim_time 9 i=500
        wf_xtra_custom_contract 20 i=0,0,0,1,-20,0,0,0,1,-20
          f=0:0/100/101/200/201;0:0/100/101/200/201;200:0/100/101/200/201;200:0/100/101/200/201
          u=200:0/200/201;200:0/200/201 v=0,0 wall ctx=0
        wf_xtra_exec_in_order 17
        wf_xtra_execution_limit 5 i=100,1
        wf_xtra_filter_furni 10 i=10,0,1 f=100:0/100/201 u=0:0/201 v=0 ctx=0
        wf_xtra_filter_furni_by_var 12 i=10,0,0,1 f=100:0/100/201 u=0:0/201 v=0,0 ctx=0
        wf_xtra_filter_users 11 i=10,0,1 f=100:0/100/201 u=0:0/201 v=0 ctx=0
        wf_xtra_filter_users_by_var 13 i=10,0,0,1 f=100:0/100/201 u=0:0/201 v=0,0 ctx=0
        wf_xtra_link_speaker 2003 i=1,1
        wf_xtra_mov_carry_users 8 i=0 u=900:0/200/201/900
        wf_xtra_mov_curve 22 i=0,160,-20 f=100:0/100/200/201 u=0:0/200/201 v=0 ctx=0
        wf_xtra_mov_no_animation 6
        wf_xtra_mov_physics 7 i=1,0,0,0 f=900:0/100/101/200/201/900;900:0/100/101/200/201/900 u=900:0/200/201/900
        wf_xtra_or_eval 0 i=1,0,0 f=100:0/100/200/201
        wf_xtra_random 1 i=0,1
        wf_xtra_rotate_to_dir 21 i=1,1,0,0,100,-20,1,1,0,4,0,0,1,1,0,0,1,-20,50
          f=901:0/100/200/201/901;0:0/100/200/201;0:0/100/200/201 u=0:0/200/201;0:0/200/201;0:0/200/201 v=0,0 ctx=0
        wf_xtra_scan_chest_furni_by_type 18 i=0 f=0:0/100/101/200/201;100:0/100/101/200/201 v=0 wall ctx=0
        wf_xtra_text_input_variable 16 i=0 v=0 ctx=0
        wf_xtra_text_output_furni_name 19 i=0 f=0:0/100/200/201 wall
        wf_xtra_text_output_username 14 i=0 u=0:0/200/201
        wf_xtra_text_output_variable 15 i=0,1,0 f=100:0/100/200/201 u=0:0/200/201 v=0 wall ctx=0
        wf_xtra_unseen 2
        wf_xtra_var_import 1004 i=1,1,1
        wf_xtra_var_leaderboard 1005 i=0,2,0,1,-1,-1,0,0,0,0,0,0,0,0,3,0,0,0,0 v=0,0,0,0,0,0,0,0,0,0,0,0 ctx=0,8
        wf_xtra_var_lvlup_system 1001 i=0,1,100,50,50 ctx=5
        wf_xtra_var_share 1003 i=1,1,1
        wf_xtra_var_text_connector 1000
        wf_xtra_var_time_util 1002 i=0,1 ctx=5
        wf_xtra_var_web_api 2002 i=0
        wf_xtra_varfx_boss 1204 i=1,2,0,7,0,3000,0,-1,-1,-1,0,0,0,100,0,0,1,1,0,0,0,0,0 v=0,0,0 ctx=0
        wf_xtra_varfx_hp 1200 i=1,2,0,7,0,3000,0,-1,-1,-1,0,0,0,100,0,0,1,1,0,0,0,0,0 v=0,0,0 ctx=0
        wf_xtra_varfx_levelling 1202 i=1,2,0,7,0,3000,0,-1,-1,-1,0,0,0,100,0,0,1,1,0,0,0,2 v=0,0,0 ctx=0
        wf_xtra_varfx_number 1205 i=1,2,0,7,0,3000,0,-1,-1,-1,0,0,0,100,0,0,1,1,0,0,0,0 v=0,0,0 ctx=0
        wf_xtra_varfx_prog 1201 i=1,2,0,7,0,3000,0,-1,-1,-1,0,0,0,100,0,0,1,1,0,0,0,0,0 v=0,0,0 ctx=0
        wf_xtra_varfx_status 1203 i=1,2,0,7,0,3000,0,-1,-1,-1,0,0,0,100,0,0,1,1,0,0,0,0,0 v=0,0,0 ctx=0
"""

    const val VARIABLES = """
        wf_var_context 3 i=0
        wf_var_echo 7 v=0 ctx=0
        wf_var_furni 0 i=0,1 ctx=1
        wf_var_pointer 9 i=1 v=0 ctx=0
        wf_var_quest 5 i=0 limit=0 basic ctx=0
        wf_var_quest_chain 6 i=0 limit=0 basic ctx=0
        wf_var_reference 4 i=1 v=0 ctx=4
        wf_var_room 2 i=1 ctx=3
        wf_var_user 1 i=0,0 ctx=2
"""
}
