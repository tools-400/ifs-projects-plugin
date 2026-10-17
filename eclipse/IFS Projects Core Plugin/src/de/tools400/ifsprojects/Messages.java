/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
    private static final String BUNDLE_NAME = "de.tools400.ifsprojects.messages"; //$NON-NLS-1$

    public static String Title_IFS_Projects;
    public static String Title_New_IFS_project;
    public static String Label_IFS_project_dots;

    public static String Label_A_disconnected;
    public static String Label_Connect;
    public static String Tooltip_Connect;
    public static String Label_Disconnect;
    public static String Tooltip_Disconnect;
    public static String Label_Clear;
    public static String Tooltip_Clear;
    public static String Label_New_terminal;
    public static String Tooltip_New_terminal;
    public static String Label_Scroll_lock;
    public static String Tooltip_Scroll_lock;
    public static String Label_Command_input_field;
    public static String Tooltip_Command_input_field;
    public static String Label_No_terminal_open;
    public static String Label_Connect_all;
    public static String Label_Disconnect_all;
    public static String Label_Close_all;
    public static String Label_Close;
    public static String Label_Rename;
    public static String Tooltip_Connect_all;
    public static String Tooltip_Disconnect_all;
    public static String Tooltip_Close_all;
    public static String Tooltip_Connect_selected;
    public static String Tooltip_Disconnect_selected;
    public static String Tooltip_Close_selected;
    public static String Tooltip_Rename_selected;
    public static String Tooltip_Terminal_list;
    public static String Title_Rename_terminal;
    public static String Label_New_name_colon;
    public static String Tooltip_Terminal_name;
    public static String Label_Restore_terminals_after_restart;
    public static String Label_Enter_user_ID_for_A;
    public static String Tooltip_User_ID;

    public static String Description_Preference_page;
    public static String Label_SSH_port_colon;
    public static String Label_Connection_timeout_colon;
    public static String Label_Keep_alive_interval_colon;
    public static String Label_Encoding_colon;
    public static String Label_Convert_user_ID_to_lowercase;
    public static String Label_Use_password_saved_in_RSE;
    public static String Tooltip_Compile_command_placeholders;
    public static String Label_Group_Defaults_of_IFS_projects;
    public static String Label_Preset_colon;
    public static String Label_Preset_custom;
    public static String Tooltip_Preset_A_B;
    public static String Label_Reuse_build_terminal;
    public static String Label_Run_build_and_compile_commands;
    public static String Label_Build_mode_terminal;
    public static String Label_Build_mode_rse;
    public static String Tooltip_Build_mode;
    public static String Tooltip_Build_mode_terminal;
    public static String Tooltip_Build_mode_rse;
    public static String Tooltip_SSH_port;
    public static String Tooltip_Connection_timeout;
    public static String Tooltip_Keep_alive_interval;
    public static String Tooltip_Encoding;
    public static String Tooltip_Convert_user_ID_to_lowercase;
    public static String Tooltip_Use_password_saved_in_RSE;
    public static String Tooltip_Restore_terminals_after_restart;
    public static String Tooltip_Reuse_build_terminal;
    public static String Tooltip_Default_build_command;
    public static String Tooltip_Default_environment_variables;

    public static String Description_New_IFS_project_page;
    public static String Label_Project_name_colon;
    public static String Label_Connection_colon;
    public static String Label_Directory_colon;
    public static String Label_Browse;
    public static String Title_Select_project_directory;
    public static String Label_Select_a_directory_colon;
    public static String Label_Create_directory_if_it_does_not_exist;
    public static String Label_Group_iproj_json;
    public static String Label_Create_iproj_json_if_it_does_not_exist;
    public static String Label_Description_colon;
    public static String Label_Build_tool_colon;
    public static String Label_Build_command_colon;
    public static String Label_Compile_command_colon;
    public static String Tooltip_Compile_command;
    public static String Label_Repository_colon;
    public static String Tooltip_Project_directory;
    public static String Tooltip_Browse_project_directory;
    public static String Tooltip_Project_name;
    public static String Tooltip_Create_directory;
    public static String Tooltip_Create_iproj_json;
    public static String Tooltip_Build_tool;
    public static String Tooltip_Build_command;
    public static String Tooltip_Empty_compile_command_uses_A;
    public static String Tooltip_iproj_description;
    public static String Tooltip_iproj_version;
    public static String Tooltip_iproj_repository;
    public static String Tooltip_iproj_objlib;
    public static String Tooltip_iproj_curlib;
    public static String Tooltip_iproj_includePath;
    public static String Tooltip_iproj_preUsrlibl;
    public static String Tooltip_iproj_postUsrlibl;
    public static String Tooltip_iproj_setIBMiEnvCmd;
    public static String Tooltip_Directory_tree;
    public static String Tooltip_Selected_directory;
    public static String Label_File_A_CCSID_B;
    public static String Label_File_A_does_not_exist;
    public static String Tooltip_Empty_build_command_uses_A;
    public static String Task_Creating_IFS_project_A;
    public static String Task_Reading_A;
    public static String Task_Writing_A;
    public static String Question_File_A_changed_overwrite;
    public static String Info_Existing_iproj_json_is_used;
    public static String Warning_Existing_iproj_json_is_invalid_A;
    public static String Description_Environment_page;
    public static String Label_Environment_variables_colon;
    public static String Tooltip_Environment_variables;
    public static String Label_Variables_used_in_iproj_json_A;
    public static String Label_No_variables_used_in_iproj_json;
    public static String Label_Default_environment_variables_of_preferences_A;
    public static String Info_Variables_of_iproj_json_added_A;
    public static String Warning_Variables_of_iproj_json_not_supported_A;
    public static String Error_Invalid_environment_variable_in_line_A;
    public static String Error_Could_not_save_environment_variables_A;

    public static String Task_Checking_syntax_of_A;
    public static String Question_Save_A_before_syntax_check;
    public static String Error_Syntax_check_of_A_not_supported;
    public static String Error_File_A_is_not_part_of_an_IFS_project;
    public static String Error_Editor_does_not_edit_a_file_of_an_IFS_project;
    public static String Error_Could_not_save_A;
    public static String Error_A_has_not_been_uploaded;
    public static String Error_Command_A_failed_B;
    public static String Error_No_events_file_A_B;
    public static String Error_Could_not_show_errors_A;

    public static String Task_Building_A;
    public static String Task_Compiling_A;
    public static String Task_Waiting_for_command_to_start;
    public static String Task_Waiting_for_command_to_end;
    public static String Log_Running_A_in_B;
    public static String Log_Finished;
    public static String Log_Finished_with_exit_code_A;
    public static String Log_Canceled;
    public static String Log_Canceled_command_may_still_be_running;
    public static String Log_No_events_files_found;
    public static String Error_Could_not_run_A_B;

    public static String Description_Local_project_page;
    public static String Label_Deploy_directory_colon;
    public static String Label_Empty_A;
    public static String Label_Build_on_IBM_i;
    public static String Tooltip_Build_on_IBM_i;
    public static String Tooltip_Local_project_connection;
    public static String Error_Select_IBM_i_connection;
    public static String Title_New_local_IFS_project;
    public static String Description_New_local_IFS_project_page;
    public static String Description_New_local_IFS_project_settings_page;
    public static String Title_Import_local_IFS_project;
    public static String Description_Import_local_IFS_project_page;
    public static String Label_Text_files_in_UTF8;
    public static String Tooltip_Text_files_in_UTF8;
    public static String Error_Could_not_create_project_A;
    public static String Tooltip_Deploy_directory;
    public static String Error_Deploy_directory_invalid;
    public static String Error_Could_not_save_settings_A;
    public static String Error_A_is_not_configured_as_IFS_project;
    public static String Error_Connection_A_of_B_not_found;
    public static String Task_Uploading_A_to_B;
    public static String Info_Upload_to_A_B_uploaded_C_deleted_D_unchanged;
    public static String Error_Could_not_delete_A;
    public static String Description_Local_environment_page;
    public static String Tooltip_Local_environment_variables;
    public static String Error_File_A_is_not_uploaded;
    public static String Question_Files_changed_in_IFS_A;
    public static String Label_And_A_more_files;
    public static String Error_Could_not_upload_A_B;

    public static String Error_No_supported_terminal_implementation_found;
    public static String Error_No_SSH_connector_found;
    public static String Error_No_active_workbench_page;
    public static String Error_Could_not_connect_terminal_A;
    public static String Error_User_ID_must_not_be_empty;
    public static String Error_Unexpected_error_A;
    public static String Error_Directory_must_be_absolute;
    public static String Error_Project_name_must_not_be_empty;
    public static String Error_A_project_named_A_already_exists;
    public static String Error_Workspace_project_A_already_exists;
    public static String Error_Directory_A_does_not_exist;
    public static String Error_Could_not_create_project_filter_A;
    public static String Error_Not_an_IFS_project;
    public static String Error_Invalid_iproj_json_A;
    public static String Error_Could_not_connect_to_A;
    public static String Error_Could_not_read_A_B;
    public static String Error_Could_not_write_A_B;
    public static String Error_File_A_has_been_changed;
    public static String Error_Could_not_create_directory_A;
    public static String Error_Could_not_create_file_A;
    public static String Error_Not_an_IBM_i_connection_A;
    public static String Error_Connection_A_is_not_connected;
    public static String Error_IFS_subsystem_not_found_A;
    public static String Error_IFS_projects_subsystem_not_found_A;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
